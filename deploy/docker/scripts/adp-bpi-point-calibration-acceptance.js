#!/usr/bin/env node
"use strict";

const crypto = require("node:crypto");
const fs = require("node:fs");
const path = require("node:path");
const { chromium, request } = require("playwright");

const marker = required("BPI_ACCEPTANCE_MARKER");
const adpBaseUrl = required("ADP_BASE_URL").replace(/\/+$/, "");
const bpiBaseUrl = required("BPI_BROWSER_BASE_URL");
const serviceBaseUrl = required("BPI_SERVICE_BASE_URL").replace(/\/+$/, "");
const username = required("ADP_USERNAME");
const password = required("ADP_PASSWORD");
const internalSecret = required("BPI_INTERNAL_JWT_SECRET");
const plantId = process.env.BPI_PLANT_ID || "PLANT-01";
const lineId = process.env.BPI_LINE_ID || "LINE-S07-01";
const calibrationVersion = `${marker}_NON_MATCHING`;
const reviewer = `${marker}_REVIEWER`;
const timeoutMs = Number(process.env.BPI_BROWSER_TIMEOUT_MS || 120_000);
const headless = process.env.BPI_HEADLESS !== "false";
const reportPath = path.resolve(
  process.env.BPI_BROWSER_REPORT || `/tmp/${marker}-point-calibration.json`,
);
const screenshotPath = path.resolve(
  process.env.BPI_BROWSER_SCREENSHOT || `/tmp/${marker}-point-calibration.png`,
);

if (!/^[A-Za-z0-9_-]{8,80}$/.test(marker)) {
  throw new Error("BPI_ACCEPTANCE_MARKER must use 8-80 letters, digits, underscores or hyphens");
}
if (Buffer.byteLength(internalSecret, "utf8") < 32) {
  throw new Error("BPI_INTERNAL_JWT_SECRET must contain at least 32 UTF-8 bytes");
}

function required(key) {
  const value = process.env[key];
  if (!value || !value.trim()) throw new Error(`${key} is required`);
  return value.trim();
}

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

function findTicket(payload) {
  const candidates = [
    payload?.ticket,
    payload?.access_token,
    payload?.token,
    payload?.data?.ticket,
    payload?.data?.access_token,
    payload?.data?.token,
    payload?.result?.ticket,
    payload?.result?.access_token,
    payload?.result?.token,
  ];
  return candidates.find((value) => typeof value === "string" && value.length > 20);
}

async function readJson(response) {
  const text = await response.text();
  try {
    return { json: JSON.parse(text), text };
  } catch (_error) {
    return { json: null, text };
  }
}

async function login(api) {
  const attempts = [
    { userName: username, password, clientId: "pc_dt" },
    { username, password, clientId: "pc_dt" },
  ];
  const failures = [];
  for (const body of attempts) {
    const response = await api.post(`${adpBaseUrl}/inter-api/auth/login`, {
      data: body,
      headers: { Accept: "application/json", "Content-Type": "application/json;charset=UTF-8" },
      timeout: timeoutMs,
    });
    const parsed = await readJson(response);
    const ticket = response.ok() ? findTicket(parsed.json) : null;
    if (ticket) return { ticket, loginStatus: response.status(), loginPayload: parsed.json };
    failures.push(response.status());
  }
  throw new Error(`ADP login failed with statuses ${failures.join(",")}`);
}

function base64Url(value) {
  return Buffer.from(value).toString("base64url");
}

function reviewerToken() {
  const now = Math.floor(Date.now() / 1_000);
  const header = base64Url(JSON.stringify({ alg: "HS256", typ: "JWT" }));
  const payload = base64Url(JSON.stringify({
    iss: "ft-mes-adapter",
    aud: "bpi-service",
    sub: reviewer,
    iat: now,
    exp: now + 600,
    tenant_id: "1000",
    roles: ["BPI_ADMIN"],
    plant_ids: [plantId],
    line_ids: [lineId],
  }));
  const signature = crypto.createHmac("sha256", internalSecret)
    .update(`${header}.${payload}`)
    .digest("base64url");
  return `${header}.${payload}.${signature}`;
}

function localDateTime(value) {
  const date = new Date(value);
  date.setMinutes(date.getMinutes() - date.getTimezoneOffset());
  return date.toISOString().slice(0, 19);
}

async function adapterGet(api, ticket, route) {
  const response = await api.get(`${adpBaseUrl}/bpi-api${route}`, {
    headers: { Authorization: `Bearer ${ticket}` },
    timeout: timeoutMs,
  });
  const parsed = await readJson(response);
  assert(response.status() === 200, `GET ${route} returned ${response.status()}: ${parsed.text.slice(0, 500)}`);
  return parsed.json;
}

async function main() {
  fs.mkdirSync(path.dirname(reportPath), { recursive: true });
  fs.mkdirSync(path.dirname(screenshotPath), { recursive: true });
  const api = await request.newContext({ ignoreHTTPSErrors: true });
  let browser;
  const expectedConsoleErrorIndexes = new Set();
  const report = {
    generatedAt: new Date().toISOString(),
    status: "FAIL",
    marker,
    calibrationVersion,
    scope: { tenantId: "1000", plantId, lineId },
    urls: { adpBaseUrl, bpiBaseUrl, serviceBaseUrl },
    loginStatus: null,
    page: {},
    requests: [],
    consoleErrors: [],
    expectedConsoleErrors: [],
    unexpectedConsoleErrors: [],
    pageErrors: [],
    requestFailures: [],
    evidence: {},
    screenshot: screenshotPath,
    error: null,
  };

  try {
    const auth = await login(api);
    report.loginStatus = auth.loginStatus;
    const initialCatalog = await adapterGet(
      api,
      auth.ticket,
      `/point-catalog/current?plantId=${encodeURIComponent(plantId)}&lineId=${encodeURIComponent(lineId)}`,
    );
    assert(initialCatalog?.data?.snapshot, "target point catalog is missing");
    assert(initialCatalog.data.points.length > 0, "target point catalog contains no points");
    assert(initialCatalog.data.snapshot.readyPointCount === 0,
      `target pilot must remain blocked before acceptance, got ${initialCatalog.data.snapshot.readyPointCount} READY`);
    const initialPoint = initialCatalog.data.points[0];
    report.evidence.before = {
      snapshotId: initialCatalog.data.snapshot.id,
      sourceRevision: initialCatalog.data.snapshot.sourceRevision,
      readyPointCount: initialCatalog.data.snapshot.readyPointCount,
      pointId: initialPoint.id,
      productId: initialPoint.productId,
      deviceId: initialPoint.deviceId,
      propertyId: initialPoint.propertyId,
      catalogCalibrationVersion: initialPoint.calibrationVersion,
      sourceCalibrationStatus: initialPoint.sourceCalibrationStatus,
      calibrationStatus: initialPoint.calibrationStatus,
      readinessIssues: initialPoint.readinessIssues,
    };

    browser = await chromium.launch({ headless });
    const context = await browser.newContext({
      ignoreHTTPSErrors: true,
      viewport: { width: 1440, height: 900 },
      extraHTTPHeaders: { Authorization: `Bearer ${auth.ticket}` },
    });
    await context.addCookies([
      { name: "suposTicket", value: auth.ticket, url: new URL(bpiBaseUrl).origin },
      { name: "SUPOS_TICKET", value: auth.ticket, url: new URL(bpiBaseUrl).origin },
    ]);
    await context.addInitScript(({ token, loginPayload, pointPlantId, pointLineId }) => {
      for (const key of ["suposTicket", "SUPOS_TICKET", "token", "ticket"]) {
        window.localStorage.setItem(key, token);
        window.sessionStorage.setItem(key, token);
      }
      if (loginPayload) window.localStorage.setItem("loginMsg", JSON.stringify(loginPayload));
      window.localStorage.setItem("language", "zh_CN");
      window.localStorage.setItem("langu_code", "zh_CN");
      window.localStorage.setItem("locale", "zh-cn");
      window.localStorage.setItem("bpi.plantId", pointPlantId);
      window.localStorage.setItem("bpi.lineId", pointLineId);
    }, {
      token: auth.ticket,
      loginPayload: auth.loginPayload,
      pointPlantId: plantId,
      pointLineId: lineId,
    });

    const page = await context.newPage();
    page.setDefaultTimeout(timeoutMs);
    page.on("console", (message) => {
      if (message.type() !== "error") return;
      const location = message.location();
      report.consoleErrors.push({
        text: message.text(),
        url: location.url || "",
        lineNumber: location.lineNumber,
        columnNumber: location.columnNumber,
      });
    });
    page.on("pageerror", (error) => report.pageErrors.push(error.message));
    page.on("requestfailed", (failed) => report.requestFailures.push({
      method: failed.method(),
      url: failed.url(),
      error: failed.failure()?.errorText || "",
    }));
    page.on("response", async (response) => {
      if (!response.url().includes("/bpi-api/")) return;
      const requestValue = response.request();
      let responseBody = "";
      try {
        responseBody = (await response.text()).slice(0, 4_000);
      } catch (_error) {
        responseBody = "<unavailable>";
      }
      report.requests.push({
        method: requestValue.method(),
        url: response.url(),
        requestBody: (requestValue.postData() || "").slice(0, 4_000),
        status: response.status(),
        responseBody,
      });
    });

    await page.goto(bpiBaseUrl, { waitUntil: "networkidle" });
    const pointRow = page.locator(`[data-point-id="${initialPoint.id}"]`);
    await pointRow.waitFor();
    await pointRow.getByText("BLOCKED", { exact: true }).waitFor();
    await pointRow.getByRole("button", { name: "提交证据" }).click();
    await page.getByRole("heading", { name: "提交点位校准证据" }).waitFor();
    assert(await page.locator("#calibration-device").inputValue() === initialPoint.deviceId,
      "calibration dialog did not preserve the selected device");

    const checksum = crypto.createHash("sha256").update(`${marker}|certificate`).digest("hex");
    await page.locator("#calibration-version").fill(calibrationVersion);
    await page.locator("#calibration-certificate").fill(`urn:adp:e2e:point-calibration:${marker}`);
    await page.locator("#calibration-checksum").fill(checksum);
    await page.locator("#calibration-valid-from").fill(localDateTime(Date.now() - 86_400_000));
    await page.locator("#calibration-valid-until").fill(localDateTime(Date.now() + 30 * 86_400_000));
    await page.locator("#calibration-reason").fill(`${marker} 提交非匹配版本证据验证四眼复核，不放行真实点位`);
    const submitResponsePromise = page.waitForResponse((response) =>
      response.request().method() === "POST" && /\/bpi-api\/point-calibrations$/.test(response.url()));
    await page.getByRole("button", { name: "提交复核" }).click();
    const submitResponse = await submitResponsePromise;
    const submitted = await readJson(submitResponse);
    assert(submitResponse.status() === 200,
      `calibration submit returned ${submitResponse.status()}: ${submitted.text.slice(0, 500)}`);
    assert(submitted.json?.data?.state === "PENDING", "submitted calibration is not PENDING");
    const calibrationId = submitted.json.data.id;
    report.evidence.submitted = {
      id: calibrationId,
      state: submitted.json.data.state,
      revision: submitted.json.data.revision,
      submittedBy: submitted.json.data.submittedBy,
      effectivenessStatus: submitted.json.data.effectivenessStatus,
    };

    let calibrationRow = page.locator("[data-calibration-row]").filter({ hasText: calibrationVersion });
    await calibrationRow.getByText("PENDING", { exact: true }).first().waitFor();
    await calibrationRow.getByRole("button", { name: "批准", exact: true }).click();
    await page.locator("#confirm-reason").fill(`${marker} 同一提交人尝试批准，应由后端拒绝`);
    const selfApprovalConsoleStart = report.consoleErrors.length;
    const selfApprovalPromise = page.waitForResponse((response) =>
      response.request().method() === "POST"
        && response.url().includes(`/bpi-api/point-calibrations/${calibrationId}/approve`));
    await page.getByRole("button", { name: "批准证据" }).click();
    const selfApprovalResponse = await selfApprovalPromise;
    const selfApproval = await readJson(selfApprovalResponse);
    assert(selfApprovalResponse.status() === 422,
      `same-actor approval must return 422, got ${selfApprovalResponse.status()}`);
    assert(/reviewer other than the submitter/.test(selfApproval.json?.detail || ""),
      `same-actor rejection detail is missing: ${selfApproval.text.slice(0, 500)}`);
    assert(await page.locator("#confirm-dialog").getAttribute("open") === "",
      "same-actor rejection unexpectedly closed the review dialog");
    await page.getByRole("button", { name: "批准证据", exact: true }).waitFor();
    assert(await page.getByRole("button", { name: "批准证据", exact: true }).isEnabled(),
      "same-actor rejection did not restore the approval button");
    await page.waitForTimeout(250);
    const expectedApprovalConsoleErrors = [];
    for (let index = selfApprovalConsoleStart; index < report.consoleErrors.length; index += 1) {
      const entry = report.consoleErrors[index];
      const isExpectedStatus = /^Failed to load resource: the server responded with a status of 422(?: \(.*\))?$/.test(entry.text);
      const isExpectedUrl = !entry.url || entry.url === selfApprovalResponse.url();
      if (isExpectedStatus && isExpectedUrl) {
        expectedConsoleErrorIndexes.add(index);
        expectedApprovalConsoleErrors.push(entry);
      }
    }
    assert(expectedApprovalConsoleErrors.length <= 1,
      `same-actor approval emitted ${expectedApprovalConsoleErrors.length} matching console errors`);
    report.expectedConsoleErrors = expectedApprovalConsoleErrors;
    report.evidence.sameActorApproval = {
      status: selfApprovalResponse.status(),
      detail: selfApproval.json.detail,
      browserConsole: expectedApprovalConsoleErrors,
    };
    await page.locator("#confirm-dialog button[value=cancel]").last().click();

    const reviewerResponse = await api.post(
      `${serviceBaseUrl}/bpi/v1/point-calibrations/${calibrationId}/approve`,
      {
        data: { reason: `${marker} 独立计量管理员复核 marker 证据` },
        headers: {
          Authorization: `Bearer ${reviewerToken()}`,
          "Idempotency-Key": `${marker}-approve-reviewer`,
          "If-Match": "1",
        },
        timeout: timeoutMs,
      },
    );
    const reviewed = await readJson(reviewerResponse);
    assert(reviewerResponse.status() === 200,
      `independent approval returned ${reviewerResponse.status()}: ${reviewed.text.slice(0, 500)}`);
    assert(reviewed.json?.data?.state === "APPROVED", "independent reviewer did not approve evidence");
    assert(reviewed.json?.data?.decidedBy === reviewer, "approval actor does not match independent reviewer");
    report.evidence.independentApproval = {
      status: reviewerResponse.status(),
      state: reviewed.json.data.state,
      revision: reviewed.json.data.revision,
      decidedBy: reviewed.json.data.decidedBy,
      effectivenessStatus: reviewed.json.data.effectivenessStatus,
    };

    await page.reload({ waitUntil: "networkidle" });
    calibrationRow = page.locator("[data-calibration-row]").filter({ hasText: calibrationVersion });
    await calibrationRow.getByText("APPROVED", { exact: true }).waitFor();
    await calibrationRow.getByText("EFFECTIVE", { exact: true }).waitFor();
    await pointRow.getByText("BLOCKED", { exact: true }).waitFor();

    const approvedCatalog = await adapterGet(
      api,
      auth.ticket,
      `/point-catalog/current?plantId=${encodeURIComponent(plantId)}&lineId=${encodeURIComponent(lineId)}`,
    );
    assert(approvedCatalog.data.snapshot.readyPointCount === 0,
      "non-matching calibration version unexpectedly made a real point READY");
    assert(approvedCatalog.data.points[0].calibrationEvidenceId == null,
      "non-matching calibration evidence unexpectedly attached to the real point");

    await calibrationRow.getByRole("button", { name: "撤销", exact: true }).click();
    await page.locator("#confirm-reason").fill(`${marker} 验收完成后撤销 marker 证据`);
    const revokeResponsePromise = page.waitForResponse((response) =>
      response.request().method() === "POST"
        && response.url().includes(`/bpi-api/point-calibrations/${calibrationId}/revoke`));
    await page.getByRole("button", { name: "撤销证据" }).click();
    const revokeResponse = await revokeResponsePromise;
    const revoked = await readJson(revokeResponse);
    assert(revokeResponse.status() === 200,
      `calibration revoke returned ${revokeResponse.status()}: ${revoked.text.slice(0, 500)}`);
    assert(revoked.json?.data?.state === "REVOKED", "calibration evidence is not REVOKED");
    await pointRow.getByText("BLOCKED", { exact: true }).waitFor();
    report.evidence.revoked = {
      status: revokeResponse.status(),
      state: revoked.json.data.state,
      revision: revoked.json.data.revision,
      revokedBy: revoked.json.data.revokedBy,
      effectivenessStatus: revoked.json.data.effectivenessStatus,
    };

    const finalCatalog = await adapterGet(
      api,
      auth.ticket,
      `/point-catalog/current?plantId=${encodeURIComponent(plantId)}&lineId=${encodeURIComponent(lineId)}`,
    );
    const finalCalibrations = await adapterGet(
      api,
      auth.ticket,
      `/point-calibrations?plantId=${encodeURIComponent(plantId)}&lineId=${encodeURIComponent(lineId)}`,
    );
    const persisted = finalCalibrations.data.find((item) => item.id === calibrationId);
    assert(persisted?.state === "REVOKED", "revoked marker is not visible through the adapter list API");
    assert(finalCatalog.data.snapshot.readyPointCount === 0,
      "target pilot readiness changed after marker revocation");
    report.evidence.after = {
      readyPointCount: finalCatalog.data.snapshot.readyPointCount,
      sourceCalibrationStatus: finalCatalog.data.points[0].sourceCalibrationStatus,
      calibrationStatus: finalCatalog.data.points[0].calibrationStatus,
      calibrationEvidenceId: finalCatalog.data.points[0].calibrationEvidenceId,
      markerState: persisted.state,
      markerRevision: persisted.revision,
    };

    await page.screenshot({ path: screenshotPath, fullPage: true });
    report.page = { url: page.url(), title: await page.title() };
    report.unexpectedConsoleErrors = report.consoleErrors.filter(
      (_entry, index) => !expectedConsoleErrorIndexes.has(index),
    );
    assert(report.unexpectedConsoleErrors.length === 0, "browser emitted unexpected console errors");
    assert(report.pageErrors.length === 0, "browser emitted page errors");
    assert(report.requestFailures.length === 0, "browser emitted request failures");
    report.status = "PASS";
    await context.close();
  } catch (error) {
    report.error = error?.message || String(error);
    throw error;
  } finally {
    report.generatedAt = new Date().toISOString();
    fs.writeFileSync(reportPath, `${JSON.stringify(report, null, 2)}\n`, "utf8");
    if (browser) await browser.close();
    await api.dispose();
  }
}

main().catch((error) => {
  console.error(error?.stack || error);
  process.exitCode = 1;
});
