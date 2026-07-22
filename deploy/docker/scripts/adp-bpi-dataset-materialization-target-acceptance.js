#!/usr/bin/env node
"use strict";

const fs = require("node:fs");
const path = require("node:path");
const { chromium, request } = require("playwright");

const marker = required("BPI_ACCEPTANCE_MARKER");
const action = process.env.BPI_BROWSER_ACTION || "request-failure";
const adpBaseUrl = required("ADP_BASE_URL").replace(/\/+$/, "");
const browserBaseUrl = required("BPI_BROWSER_BASE_URL");
const username = required("ADP_USERNAME");
const password = required("ADP_PASSWORD");
const plantId = process.env.BPI_PLANT_ID || "PLANT-01";
const lineId = process.env.BPI_LINE_ID || "LINE-S07-01";
const expectedFailureCode = process.env.BPI_EXPECTED_FAILURE_CODE || "MATERIALIZATION_ERROR";
const timeoutMs = Number(process.env.BPI_BROWSER_TIMEOUT_MS || 120_000);
const headless = process.env.BPI_HEADLESS !== "false";
const reportPath = path.resolve(
  process.env.BPI_BROWSER_REPORT || `/tmp/${marker}-dataset-materialization-${action}.json`,
);
const desktopScreenshot = path.resolve(
  process.env.BPI_DESKTOP_SCREENSHOT || `/tmp/${marker}-dataset-materialization-${action}.png`,
);
const mobileScreenshot = path.resolve(
  process.env.BPI_MOBILE_SCREENSHOT || `/tmp/${marker}-dataset-materialization-${action}-mobile.png`,
);

if (!/^[A-Za-z0-9_-]{8,80}$/.test(marker)) {
  throw new Error("BPI_ACCEPTANCE_MARKER must use 8-80 letters, digits, underscores or hyphens");
}
if (!["request-failure", "retry-ready", "read-ready"].includes(action)) {
  throw new Error(`Unsupported BPI_BROWSER_ACTION: ${action}`);
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
  return [
    payload?.ticket,
    payload?.access_token,
    payload?.token,
    payload?.data?.ticket,
    payload?.data?.access_token,
    payload?.data?.token,
    payload?.result?.ticket,
    payload?.result?.access_token,
    payload?.result?.token,
  ].find((value) => typeof value === "string" && value.length > 20);
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
  const statuses = [];
  for (const body of [
    { userName: username, password, clientId: "pc_dt" },
    { username, password, clientId: "pc_dt" },
  ]) {
    const response = await api.post(`${adpBaseUrl}/inter-api/auth/login`, {
      data: body,
      headers: { Accept: "application/json", "Content-Type": "application/json;charset=UTF-8" },
      timeout: timeoutMs,
    });
    const parsed = await readJson(response);
    const ticket = response.ok() ? findTicket(parsed.json) : null;
    if (ticket) return { ticket, status: response.status(), payload: parsed.json };
    statuses.push(response.status());
  }
  throw new Error(`ADP login failed with statuses ${statuses.join(",")}`);
}

function sanitizedPayload(requestValue) {
  const value = requestValue.postData();
  if (!value) return null;
  try {
    return JSON.parse(value);
  } catch (_error) {
    return value.slice(0, 2_000);
  }
}

function installEvidenceListeners(page, report) {
  page.on("console", (message) => {
    if (message.type() === "error") report.browser.consoleErrors.push(message.text());
  });
  page.on("pageerror", (error) => report.browser.pageErrors.push(error.message));
  page.on("requestfailed", (failed) => report.browser.requestFailures.push({
    method: failed.method(), url: failed.url(), error: failed.failure()?.errorText || "",
  }));
  page.on("response", async (response) => {
    const url = new URL(response.url());
    if (!url.pathname.includes("/bpi-api/")) return;
    const requestValue = response.request();
    const record = {
      method: requestValue.method(),
      url: `${url.pathname}${url.search}`,
      status: response.status(),
      payload: sanitizedPayload(requestValue),
    };
    if (requestValue.method() !== "GET") {
      const parsed = await readJson(response);
      record.response = parsed.json?.data || parsed.json || parsed.text.slice(0, 2_000);
    }
    report.browser.network.push(record);
  });
}

async function prepareContext(browser, auth, viewport) {
  const context = await browser.newContext({
    ignoreHTTPSErrors: true,
    viewport,
    extraHTTPHeaders: { Authorization: `Bearer ${auth.ticket}` },
  });
  await context.addCookies([
    { name: "suposTicket", value: auth.ticket, url: new URL(browserBaseUrl).origin },
    { name: "SUPOS_TICKET", value: auth.ticket, url: new URL(browserBaseUrl).origin },
  ]);
  await context.addInitScript(({ token, loginPayload, targetPlant, targetLine }) => {
    for (const key of ["suposTicket", "SUPOS_TICKET", "token", "ticket"]) {
      window.localStorage.setItem(key, token);
      window.sessionStorage.setItem(key, token);
    }
    if (loginPayload) window.localStorage.setItem("loginMsg", JSON.stringify(loginPayload));
    window.localStorage.setItem("language", "zh_CN");
    window.localStorage.setItem("langu_code", "zh_CN");
    window.localStorage.setItem("locale", "zh-cn");
    window.localStorage.setItem("bpi.plantId", targetPlant);
    window.localStorage.setItem("bpi.lineId", targetLine);
  }, { token: auth.ticket, loginPayload: auth.payload, targetPlant: plantId, targetLine: lineId });
  return context;
}

async function apiGet(api, ticket, endpoint) {
  const response = await api.get(`${adpBaseUrl}${endpoint}`, {
    headers: { Authorization: `Bearer ${ticket}` },
    timeout: timeoutMs,
  });
  const parsed = await readJson(response);
  assert(response.status() === 200,
    `${endpoint} returned ${response.status()}: ${parsed.text.slice(0, 500)}`);
  return parsed.json;
}

async function resolveTarget(api, ticket) {
  const definitions = await apiGet(
    api, ticket, `/bpi-api/datasets?plantId=${encodeURIComponent(plantId)}&limit=100`,
  );
  const definition = definitions?.data?.find((item) => item.datasetCode === marker);
  assert(definition, `dataset definition ${marker} was not found`);
  assert(definition.plantId === plantId && definition.lineIds?.includes(lineId),
    "dataset definition scope does not match the acceptance target");
  assert(definition.latestSnapshot?.id, "dataset definition has no latest snapshot");
  const snapshotPayload = await apiGet(
    api, ticket, `/bpi-api/dataset-snapshots/${definition.latestSnapshot.id}`,
  );
  const snapshot = snapshotPayload?.data;
  assert(snapshot?.state === "MANIFEST_READY", "dataset snapshot is not MANIFEST_READY");
  return { definition, snapshot };
}

function assertManifestBoundary(snapshot) {
  const boundary = snapshot?.manifest?.phaseBoundary;
  assert(boundary?.deliveryState === "MANIFEST_ONLY", "manifest delivery boundary changed");
  assert(boundary?.materializationState === "NOT_STARTED", "manifest materialization state changed");
  assert(boundary?.artifactUri == null, "manifest unexpectedly stores an artifact URI");
  assert(boundary?.icebergReady === false
    && boundary?.mlflowRegistered === false
    && boundary?.modelTrained === false,
  "snapshot claims an unsupported downstream delivery state");
}

function assertReadyMaterialization(materialization) {
  assert(materialization?.state === "READY", "materialization did not reach READY");
  assert(materialization.attemptCount === 2,
    `materialization attempt count is ${materialization.attemptCount}, expected 2`);
  assert(/^s3:\/\/bpi-datasets\/datasets\/.*\.parquet\?versionId=.+$/.test(materialization.artifactUri || ""),
    "materialization URI is not version locked");
  assert(/^[a-f0-9]{64}$/.test(materialization.contentSha256 || ""),
    "materialization SHA-256 is invalid");
  assert(materialization.byteSize > 0 && materialization.rowCount === 1,
    "materialization byte or row count is invalid");
  assert(Array.isArray(materialization.schema?.fields) && materialization.schema.fields.length > 0,
    "materialization schema is missing");
  assert(materialization.artifactMetadata?.objectContentVerified === true,
    "materialization object was not content verified");
  assert(materialization.artifactMetadata?.simulationOnly !== true,
    "target materialization is incorrectly marked as simulated");
  assert(typeof materialization.artifactMetadata?.objectVersionId === "string"
    && materialization.artifactMetadata.objectVersionId.length > 0,
  "materialization object version is missing");
}

async function openSnapshotDrawer(page, definition) {
  await page.goto(browserBaseUrl, { waitUntil: "networkidle" });
  await page.getByRole("heading", { name: "数据集清单" }).waitFor();
  const row = page.locator(`[data-dataset-id="${definition.id}"]`);
  await row.waitFor();
  await row.click();
  await page.locator("#open-latest-dataset-snapshot").click();
  await page.locator("#detail-drawer").getByText("MANIFEST_READY", { exact: true }).waitFor();
}

async function runDesktop(page, api, auth, target, report) {
  await openSnapshotDrawer(page, target.definition);
  if (action === "request-failure") {
    assert(!target.snapshot.latestMaterialization,
      "request-failure requires a snapshot without a materialization");
    await page.locator('[data-materialization-state="NOT_STARTED"]').waitFor();
    await page.getByRole("button", { name: "生成 Parquet" }).click();
    await page.getByRole("heading", { name: "生成版本锁定对象" }).waitFor();
    await page.locator("#dataset-materialization-reason")
      .fill(`${marker} 目标对象失败恢复验收`);
    const responsePromise = page.waitForResponse((response) => {
      const url = new URL(response.url());
      return response.request().method() === "POST"
        && url.pathname.endsWith(`/bpi-api/dataset-snapshots/${target.snapshot.id}/materializations`);
    });
    await page.locator("#dataset-materialization-submit").click();
    const response = await responsePromise;
    const body = await readJson(response);
    assert(response.status() === 202,
      `materialization request returned ${response.status()}: ${body.text.slice(0, 500)}`);
    const materializationId = body.json?.data?.id;
    assert(materializationId, "materialization request did not return an id");
    const panel = page.locator('[data-materialization-state="FAILED"]');
    await panel.waitFor({ timeout: timeoutMs });
    await panel.getByText(expectedFailureCode, { exact: true }).waitFor();
    const materializationPayload = await apiGet(
      api, auth.ticket, `/bpi-api/dataset-materializations/${materializationId}`,
    );
    const materialization = materializationPayload?.data;
    assert(materialization?.state === "FAILED", "injected worker failure was not persisted");
    assert(materialization.attemptCount === 1, "failed materialization did not record one attempt");
    assert(materialization.failureCode === expectedFailureCode,
      `unexpected failure code: ${materialization.failureCode}`);
    assert(materialization.artifactUri == null && materialization.contentSha256 == null,
      "failed materialization persisted artifact fields");
    const snapshotPayload = await apiGet(
      api, auth.ticket, `/bpi-api/dataset-snapshots/${target.snapshot.id}`,
    );
    assertManifestBoundary(snapshotPayload?.data);
    report.materialization = materialization;
  } else if (action === "retry-ready") {
    const before = target.snapshot.latestMaterialization;
    assert(before?.state === "FAILED", "retry-ready requires a FAILED materialization");
    await page.locator('[data-materialization-state="FAILED"]').waitFor();
    await page.getByRole("button", { name: "重新排队" }).click();
    await page.getByRole("heading", { name: "重新排队 Parquet" }).waitFor();
    await page.locator("#dataset-materialization-reason")
      .fill(`${marker} 对象存储恢复后页面重试`);
    const responsePromise = page.waitForResponse((response) => {
      const url = new URL(response.url());
      return response.request().method() === "POST"
        && url.pathname.endsWith(`/bpi-api/dataset-materializations/${before.id}/retry`);
    });
    await page.locator("#dataset-materialization-submit").click();
    const response = await responsePromise;
    const body = await readJson(response);
    assert(response.status() === 202,
      `materialization retry returned ${response.status()}: ${body.text.slice(0, 500)}`);
    const panel = page.locator('[data-materialization-state="READY"]');
    await panel.waitFor({ timeout: timeoutMs });
    await panel.getByText("VERIFIED", { exact: true }).waitFor();
    assert(!await panel.getByText("SIMULATED", { exact: true }).count(),
      "target page rendered simulated evidence");
    assert(/^s3:\/\/bpi-datasets\/datasets\/.*\.parquet\?versionId=.+$/
      .test((await panel.locator(".dataset-artifact-uri").textContent()) || ""),
    "target page does not show a version-locked URI");
    assert(/^[a-f0-9]{64}$/
      .test((await panel.locator(".dataset-artifact-sha").textContent()) || ""),
    "target page does not show a valid SHA-256");
    assert(JSON.stringify(await panel.locator(".dataset-downstream-grid .status").allTextContents())
      === JSON.stringify(["NOT_STARTED", "NOT_STARTED", "NOT_STARTED"]),
    "target page claims unsupported downstream delivery");
    const materializationPayload = await apiGet(
      api, auth.ticket, `/bpi-api/dataset-materializations/${before.id}`,
    );
    assertReadyMaterialization(materializationPayload?.data);
    const snapshotPayload = await apiGet(
      api, auth.ticket, `/bpi-api/dataset-snapshots/${target.snapshot.id}`,
    );
    assertManifestBoundary(snapshotPayload?.data);
    assert(snapshotPayload?.data?.materializationState === "READY",
      "snapshot projection did not expose READY");
    assert(snapshotPayload?.data?.latestMaterialization?.id === before.id,
      "snapshot projection points to the wrong materialization");
    report.materialization = materializationPayload.data;
  } else {
    const materialization = target.snapshot.latestMaterialization;
    assertReadyMaterialization(materialization);
    assertManifestBoundary(target.snapshot);
    const panel = page.locator('[data-materialization-state="READY"]');
    await panel.waitFor();
    await panel.getByText("VERIFIED", { exact: true }).waitFor();
    report.materialization = materialization;
  }
  await page.screenshot({ path: desktopScreenshot, fullPage: true });
}

async function runMobile(browser, auth, definition, report) {
  const context = await prepareContext(browser, auth, { width: 390, height: 844 });
  const page = await context.newPage();
  page.setDefaultTimeout(timeoutMs);
  installEvidenceListeners(page, report);
  await openSnapshotDrawer(page, definition);
  const panel = page.locator('[data-materialization-state="READY"]');
  await panel.waitFor();
  await panel.getByText("VERIFIED", { exact: true }).waitFor();
  const geometry = await page.evaluate(() => ({
    viewport: window.innerWidth,
    body: document.body.scrollWidth,
    document: document.documentElement.scrollWidth,
    drawerScrollTop: document.querySelector("#detail-drawer")?.scrollTop ?? -1,
  }));
  assert(geometry.body <= geometry.viewport + 1 && geometry.document <= geometry.viewport + 1,
    `dataset materialization page overflows mobile viewport: ${JSON.stringify(geometry)}`);
  assert(geometry.drawerScrollTop === 0, "opening the snapshot did not reset mobile drawer scroll");
  await page.screenshot({ path: mobileScreenshot, fullPage: true });
  report.browser.mobile = { ...geometry, verifiedPanel: true };
  await context.close();
}

async function main() {
  for (const target of [reportPath, desktopScreenshot, mobileScreenshot]) {
    fs.mkdirSync(path.dirname(target), { recursive: true });
  }
  const api = await request.newContext({ ignoreHTTPSErrors: true });
  let browser;
  const report = {
    generatedAt: new Date().toISOString(),
    status: "FAIL",
    marker,
    action,
    scope: { tenantId: "1000", plantId, lineId },
    login: null,
    definition: null,
    snapshot: null,
    materialization: null,
    browser: {
      route: browserBaseUrl,
      consoleErrors: [],
      pageErrors: [],
      requestFailures: [],
      network: [],
      mobile: null,
    },
    screenshots: { desktop: desktopScreenshot, mobile: mobileScreenshot },
    databaseVerificationRequired: true,
    exactObjectVerificationRequired: action !== "request-failure",
    cleanupRequired: true,
    error: null,
  };

  try {
    const auth = await login(api);
    report.login = { status: auth.status, username };
    const target = await resolveTarget(api, auth.ticket);
    report.definition = {
      id: target.definition.id,
      datasetCode: target.definition.datasetCode,
      latestSnapshotId: target.definition.latestSnapshot.id,
    };
    report.snapshot = {
      id: target.snapshot.id,
      state: target.snapshot.state,
      revision: target.snapshot.revision,
      manifestChecksum: target.snapshot.manifestChecksum,
    };
    assertManifestBoundary(target.snapshot);

    browser = await chromium.launch({ headless });
    const desktopContext = await prepareContext(browser, auth, { width: 1440, height: 900 });
    const desktopPage = await desktopContext.newPage();
    desktopPage.setDefaultTimeout(timeoutMs);
    installEvidenceListeners(desktopPage, report);
    await runDesktop(desktopPage, api, auth, target, report);
    await desktopContext.close();
    if (action !== "request-failure") await runMobile(browser, auth, target.definition, report);
    await browser.close();
    browser = null;

    assert(report.browser.consoleErrors.length === 0, "browser emitted console errors");
    assert(report.browser.pageErrors.length === 0, "browser emitted page errors");
    assert(report.browser.requestFailures.length === 0, "browser emitted request failures");
    if (action === "request-failure") {
      assert(report.browser.network.some((item) => item.method === "POST"
        && item.status === 202 && item.url.endsWith("/materializations")),
      "browser evidence is missing the materialization request POST");
    }
    if (action === "retry-ready") {
      assert(report.browser.network.some((item) => item.method === "POST"
        && item.status === 202 && item.url.endsWith("/retry")),
      "browser evidence is missing the materialization retry POST");
    }
    report.status = action === "request-failure"
      ? "PASS_FAILED_STATE_PERSISTED" : "PASS_PENDING_DATABASE_OBJECT_VERIFICATION_AND_CLEANUP";
  } catch (error) {
    report.error = error instanceof Error ? error.stack || error.message : String(error);
    throw error;
  } finally {
    if (browser) await browser.close();
    await api.dispose();
    fs.writeFileSync(reportPath, `${JSON.stringify(report, null, 2)}\n`);
    process.stdout.write(`${JSON.stringify({
      status: report.status,
      marker,
      action,
      reportPath,
      desktopScreenshot,
      mobileScreenshot: action === "request-failure" ? null : mobileScreenshot,
      datasetId: report.definition?.id || null,
      snapshotId: report.snapshot?.id || null,
      materializationId: report.materialization?.id || null,
      cleanupRequired: report.cleanupRequired,
    })}\n`);
  }
}

main().catch((error) => {
  process.stderr.write(`${error instanceof Error ? error.stack || error.message : String(error)}\n`);
  process.exitCode = 1;
});
