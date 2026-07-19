#!/usr/bin/env node
"use strict";

const crypto = require("node:crypto");
const fs = require("node:fs");
const path = require("node:path");
const { execFileSync } = require("node:child_process");
const { chromium, request } = require("playwright");

const marker = required("BPI_ACCEPTANCE_MARKER");
const adpBaseUrl = required("ADP_BASE_URL").replace(/\/+$/, "");
const bpiBaseUrl = required("BPI_BROWSER_BASE_URL");
const serviceBaseUrl = required("BPI_SERVICE_BASE_URL").replace(/\/+$/, "");
const username = required("ADP_USERNAME");
const password = required("ADP_PASSWORD");
const internalSecret = required("BPI_INTERNAL_JWT_SECRET");
const expectedApprover = process.env.BPI_EXPECTED_APPROVER || `legacy-ticket:${username}`;
const plantId = process.env.BPI_PLANT_ID || "PLANT-01";
const lineId = process.env.BPI_LINE_ID || "LINE-S07-01";
const postgresContainer = process.env.BPI_POSTGRES_CONTAINER || "adp-mes-newbase-postgres-1";
const postgresUser = process.env.BPI_POSTGRES_USER || "adp";
const postgresDatabase = process.env.BPI_POSTGRES_DATABASE || "ft_mes_bpi";
const timeCompressionDays = Number(process.env.BPI_TIME_COMPRESSION_DAYS || 8);
const timeoutMs = Number(process.env.BPI_BROWSER_TIMEOUT_MS || 120_000);
const headless = process.env.BPI_HEADLESS !== "false";
const engineer = `${marker}_ENGINEER`;
const reportPath = path.resolve(
  process.env.BPI_BROWSER_REPORT || `/tmp/${marker}-shadow-run.json`,
);
const screenshotPath = path.resolve(
  process.env.BPI_BROWSER_SCREENSHOT || `/tmp/${marker}-shadow-run.png`,
);

if (!/^[A-Za-z0-9_-]{8,80}$/.test(marker)) {
  throw new Error("BPI_ACCEPTANCE_MARKER must use 8-80 letters, digits, underscores or hyphens");
}
if (Buffer.byteLength(internalSecret, "utf8") < 32) {
  throw new Error("BPI_INTERNAL_JWT_SECRET must contain at least 32 UTF-8 bytes");
}
if (!Number.isInteger(timeCompressionDays) || timeCompressionDays < 7 || timeCompressionDays > 14) {
  throw new Error("BPI_TIME_COMPRESSION_DAYS must be an integer between 7 and 14");
}

function required(key) {
  const value = process.env[key];
  if (!value || !value.trim()) throw new Error(`${key} is required`);
  return value.trim();
}

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

function base64Url(value) {
  return Buffer.from(value).toString("base64url");
}

function internalToken(subject, roles) {
  const now = Math.floor(Date.now() / 1_000);
  const header = base64Url(JSON.stringify({ alg: "HS256", typ: "JWT" }));
  const payload = base64Url(JSON.stringify({
    iss: "ft-mes-adapter",
    aud: "bpi-service",
    sub: subject,
    iat: now,
    exp: now + 600,
    tenant_id: "1000",
    roles,
    plant_ids: [plantId],
    line_ids: [lineId],
  }));
  const signature = crypto.createHmac("sha256", internalSecret)
    .update(`${header}.${payload}`)
    .digest("base64url");
  return `${header}.${payload}.${signature}`;
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
    if (ticket) return { ticket, status: response.status(), payload: parsed.json };
    failures.push(response.status());
  }
  throw new Error(`ADP login failed with statuses ${failures.join(",")}`);
}

async function jsonRequest(api, method, url, options = {}) {
  const headers = { Accept: "application/json" };
  if (options.token) headers.Authorization = `Bearer ${options.token}`;
  if (options.key) headers["Idempotency-Key"] = options.key;
  if (options.revision !== undefined) headers["If-Match"] = String(options.revision);
  if (options.body !== undefined) headers["Content-Type"] = "application/json;charset=UTF-8";
  const response = await api.fetch(url, {
    method,
    headers,
    data: options.body,
    timeout: timeoutMs,
  });
  const parsed = await readJson(response);
  const expected = options.expected || [200];
  assert(expected.includes(response.status()),
    `${method} ${url} returned ${response.status()}: ${parsed.text.slice(0, 1000)}`);
  return { status: response.status(), body: parsed.json, text: parsed.text };
}

function adapterUrl(route) {
  return `${adpBaseUrl}/bpi-api${route}`;
}

function serviceUrl(route) {
  return `${serviceBaseUrl}/bpi/v1${route}`;
}

function timeCompress(runId) {
  assert(/^[0-9a-f-]{36}$/i.test(runId), "runId is not a UUID");
  const sql = `
    UPDATE bpi.bpi_shadow_runs
       SET created_at = now() - interval '${timeCompressionDays} days',
           started_at = now() - interval '${timeCompressionDays} days',
           updated_at = now()
     WHERE tenant_id = '1000' AND id = '${runId}'::uuid
       AND run_code = '${marker}' AND state = 'RUNNING'
    RETURNING id::text || '|' || state || '|' || revision || '|' ||
              floor(extract(epoch FROM (now() - started_at)))::bigint;
  `;
  const output = execFileSync("docker", [
    "exec", postgresContainer, "psql", "-U", postgresUser, "-d", postgresDatabase,
    "-v", "ON_ERROR_STOP=1", "-Atc", sql,
  ], { encoding: "utf8" }).trim();
  assert(output.includes(`${runId}|RUNNING|2|`), `time compression did not update exactly the target run: ${output}`);
  return output.split("\n")[0];
}

async function main() {
  fs.mkdirSync(path.dirname(reportPath), { recursive: true });
  fs.mkdirSync(path.dirname(screenshotPath), { recursive: true });
  const api = await request.newContext({ ignoreHTTPSErrors: true });
  let browser;
  const report = {
    generatedAt: new Date().toISOString(),
    status: "FAIL",
    marker,
    scope: { tenantId: "1000", plantId, lineId },
    actors: { creator: engineer, loginUser: username, approver: expectedApprover },
    timeCompression: {
      applied: false,
      days: timeCompressionDays,
      purpose: "Deterministic target verification of the 7-day gate; not field-duration evidence.",
    },
    loginStatus: null,
    runId: null,
    incidentId: null,
    api: {},
    browser: { responses: [], consoleErrors: [], pageErrors: [], requestFailures: [] },
    externalWrites: null,
    screenshot: screenshotPath,
    error: null,
  };

  try {
    const auth = await login(api);
    report.loginStatus = auth.status;
    const engineerJwt = internalToken(engineer, ["BPI_ENGINEER"]);

    const rules = await jsonRequest(api, "GET",
      adapterUrl(`/rules?plantId=${encodeURIComponent(plantId)}&lineId=${encodeURIComponent(lineId)}`),
      { token: auth.ticket });
    const rule = rules.body?.data?.find((item) => item.code === `${marker}_RULE`);
    assert(rule?.state === "PUBLISHED", "controlled published marker rule is not visible through the adapter");

    const created = await jsonRequest(api, "POST", serviceUrl("/shadow-runs"), {
      token: engineerJwt,
      key: `${marker}_CREATE`,
      revision: 0,
      body: {
        runCode: marker,
        name: `${marker} 影子运行落库验收`,
        plantId,
        lineId,
        ruleVersionId: rule.id,
        minimumDurationDays: 7,
        minimumReviewedBatches: 10,
        boundaryToleranceSeconds: 60,
        minimumBoundaryAgreement: 0.95,
        quantityTolerancePercent: 2,
        reason: `${marker} pin rule topology and catalog for controlled target acceptance`,
      },
    });
    let run = created.body?.data;
    assert(run?.state === "DRAFT" && run?.revision === 1, "createShadowRun did not persist DRAFT/r1");
    assert(run.createdBy === engineer, "createShadowRun did not preserve the independent creator");
    assert(run.readiness?.ready === true, `run start readiness is blocked: ${JSON.stringify(run.blockers)}`);
    report.runId = run.id;
    report.api.create = { method: "POST", path: "/bpi/v1/shadow-runs", status: created.status,
      state: run.state, revision: run.revision, readiness: run.readiness };

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
    await context.addInitScript(({ token, loginPayload, targetPlantId, targetLineId }) => {
      for (const key of ["suposTicket", "SUPOS_TICKET", "token", "ticket"]) {
        window.localStorage.setItem(key, token);
        window.sessionStorage.setItem(key, token);
      }
      if (loginPayload) window.localStorage.setItem("loginMsg", JSON.stringify(loginPayload));
      window.localStorage.setItem("language", "zh_CN");
      window.localStorage.setItem("langu_code", "zh_CN");
      window.localStorage.setItem("locale", "zh-cn");
      window.localStorage.setItem("bpi.plantId", targetPlantId);
      window.localStorage.setItem("bpi.lineId", targetLineId);
      window.localStorage.setItem("bpi.shadowRunLineId", targetLineId);
      window.localStorage.setItem("bpi.shadowRunState", "");
      window.localStorage.setItem("bpi.dataQualityLineId", targetLineId);
    }, { token: auth.ticket, loginPayload: auth.payload, targetPlantId: plantId, targetLineId: lineId });

    const page = await context.newPage();
    page.setDefaultTimeout(timeoutMs);
    page.on("console", (message) => {
      if (message.type() === "error") report.browser.consoleErrors.push(message.text());
    });
    page.on("pageerror", (error) => report.browser.pageErrors.push(error.message));
    page.on("requestfailed", (failed) => report.browser.requestFailures.push({
      method: failed.method(), url: failed.url(), error: failed.failure()?.errorText || "",
    }));
    page.on("response", async (response) => {
      if (!response.url().includes("/bpi-api/")) return;
      const requestValue = response.request();
      let responseBody = "";
      try { responseBody = (await response.text()).slice(0, 4000); } catch (_error) { responseBody = "<unavailable>"; }
      report.browser.responses.push({
        method: requestValue.method(),
        path: new URL(response.url()).pathname,
        requestBody: (requestValue.postData() || "").slice(0, 4000),
        status: response.status(),
        responseBody,
      });
    });

    await page.goto(`${bpiBaseUrl.replace(/#.*$/, "")}#/shadowRuns`, { waitUntil: "networkidle" });
    await page.getByRole("heading", { name: "影子运行验收" }).waitFor();
    const runRow = page.locator(`[data-shadow-run-id="${run.id}"]`);
    await runRow.waitFor();
    await runRow.click();
    await page.getByRole("heading", { name: `${marker} 影子运行落库验收` }).waitFor();
    await page.getByRole("button", { name: "启动影子运行" }).click();
    await page.locator("#confirm-reason").fill(`${marker} target readiness independently checked before start`);
    const startResponse = page.waitForResponse((response) => response.request().method() === "POST"
      && response.url().includes(`/bpi-api/shadow-runs/${run.id}/start`));
    await page.getByRole("button", { name: "确认启动" }).click();
    assert((await startResponse).status() === 200, "startShadowRun did not return 200 through the real page");
    await page.getByText("影子运行已启动").waitFor();
    await page.locator(".batch-state-band").getByText("RUNNING", { exact: true }).waitFor();

    run = (await jsonRequest(api, "GET", adapterUrl(`/shadow-runs/${run.id}`), { token: auth.ticket })).body?.data;
    assert(run.state === "RUNNING" && run.revision === 2, "startShadowRun did not persist RUNNING/r2");
    report.api.start = { method: "POST", path: `/bpi/v1/shadow-runs/${run.id}/start`,
      status: 200, state: run.state, revision: run.revision, actor: run.startedBy };
    report.timeCompression.databaseResult = timeCompress(run.id);
    report.timeCompression.applied = true;

    await page.locator("#detail-drawer [data-close-drawer]").first().click();
    await page.getByRole("button", { name: "刷新" }).click();
    await runRow.click();
    await page.getByRole("button", { name: "复核批次" }).click();
    await page.getByRole("heading", { name: "复核影子批次" }).waitFor();
    const firstBatchId = await page.locator("#shadow-review-batch").inputValue();
    await page.locator("#shadow-review-end").evaluate((element) => {
      const parsed = new Date(element.value);
      parsed.setSeconds(parsed.getSeconds() + 61);
      element.value = new Date(parsed.getTime() - parsed.getTimezoneOffset() * 60_000).toISOString().slice(0, 19);
      element.dispatchEvent(new Event("input", { bubbles: true }));
    });
    await page.locator("#shadow-review-reason").fill(`${marker} first batch deliberate 61 second end deviation`);
    const firstReviewResponse = page.waitForResponse((response) => response.request().method() === "POST"
      && response.url().includes(`/bpi-api/shadow-runs/${run.id}/batch-reviews`));
    await page.getByRole("button", { name: "提交批次复核" }).click();
    assert((await firstReviewResponse).status() === 200, "first batch review did not return 200 through the real page");
    await page.getByText(/边界一致率 50\.00%/).waitFor();

    run = (await jsonRequest(api, "GET", adapterUrl(`/shadow-runs/${run.id}`), { token: auth.ticket })).body?.data;
    const batchesResponse = await jsonRequest(api, "GET",
      adapterUrl(`/batches?plantId=${encodeURIComponent(plantId)}&lineId=${encodeURIComponent(lineId)}&state=CLOSED_RAW&limit=50`),
      { token: auth.ticket });
    const batches = batchesResponse.body?.data?.filter((batch) => batch.batchNo.startsWith(`${marker}_BATCH_`));
    assert(batches?.length === 10, `expected ten controlled batches, got ${batches?.length || 0}`);
    let reviewNumber = 2;
    for (const batch of batches.filter((item) => item.id !== firstBatchId)) {
      const reviewed = await jsonRequest(api, "POST", adapterUrl(`/shadow-runs/${run.id}/batch-reviews`), {
        token: auth.ticket,
        key: `${marker}_REVIEW_${String(reviewNumber).padStart(2, "0")}`,
        revision: run.revision,
        body: {
          batchId: batch.id,
          manualStartTime: batch.startTime,
          manualEndTime: batch.endTime,
          referenceQuantity: batch.quantity,
          quantityUnit: batch.quantityUnit,
          reason: `${marker} independent batch review ${reviewNumber}`,
        },
      });
      run = reviewed.body?.data?.run;
      reviewNumber += 1;
    }
    assert(run.revision === 12, `ten reviews must finish at revision 12, got ${run.revision}`);
    assert(run.metrics.reviewedBatchCount === 10, "review count gate did not reach ten");
    assert(Number(run.metrics.boundaryAgreement) === 0.95, `boundary agreement is ${run.metrics.boundaryAgreement}`);
    assert(run.metrics.quantityGatePassed === true, "cumulative quantity gate did not pass");

    await page.locator("#detail-drawer [data-close-drawer]").first().click();
    await page.getByRole("button", { name: "刷新" }).click();
    await page.locator(`[data-shadow-run-id="${run.id}"]`).click();
    await page.locator(".shadow-metric-grid").getByText("10 / 10", { exact: true }).waitFor();
    await page.locator(".shadow-metric-grid").getByText("95.00% / 95.00%", { exact: true }).waitFor();
    await page.getByRole("button", { name: "结束观察并评估" }).click();
    await page.locator("#confirm-reason").fill(`${marker} seven-day gate and ten human reviews completed`);
    const completeResponse = page.waitForResponse((response) => response.request().method() === "POST"
      && response.url().includes(`/bpi-api/shadow-runs/${run.id}/complete`));
    await page.getByRole("button", { name: "确认进入评估" }).click();
    assert((await completeResponse).status() === 200, "completeShadowRun did not return 200 through the real page");
    await page.locator(".batch-state-band").getByText("EVALUATING", { exact: true }).waitFor();
    await page.getByText("UNRESOLVED_CRITICAL_DATA_QUALITY", { exact: true }).waitFor();
    assert(await page.getByRole("button", { name: "独立批准验收" }).isDisabled(),
      "approval button must remain disabled while CRITICAL data quality is unresolved");

    run = (await jsonRequest(api, "GET", adapterUrl(`/shadow-runs/${run.id}`), { token: auth.ticket })).body?.data;
    const blockedApproval = await jsonRequest(api, "POST", adapterUrl(`/shadow-runs/${run.id}/approve`), {
      token: auth.ticket,
      key: `${marker}_APPROVE_BLOCKED`,
      revision: run.revision,
      body: { reason: `${marker} prove CRITICAL data quality blocks approval` },
      expected: [422],
    });
    assert(blockedApproval.text.includes("UNRESOLVED_CRITICAL_DATA_QUALITY"),
      "blocked approval response did not expose the CRITICAL blocker");
    report.api.blockedApproval = { method: "POST", path: `/bpi/v1/shadow-runs/${run.id}/approve`,
      status: blockedApproval.status, blocker: "UNRESOLVED_CRITICAL_DATA_QUALITY" };

    const incidentList = await jsonRequest(api, "GET", adapterUrl(
      `/data-quality/incidents?plantId=${encodeURIComponent(plantId)}&lineId=${encodeURIComponent(lineId)}&search=${encodeURIComponent(marker)}&limit=100`),
    { token: auth.ticket });
    const incident = incidentList.body?.data?.find((item) => item.lastDetail?.includes(marker));
    assert(incident?.state === "OPEN", "controlled CRITICAL incident is not OPEN through the adapter");
    report.incidentId = incident.id;

    await page.locator('[data-view="dataQuality"]').click();
    await page.getByRole("heading", { name: "数据质量事件" }).waitFor();
    await page.locator("#data-quality-search").fill(marker);
    await page.locator("#apply-data-quality-filter").click();
    const incidentRow = page.locator(`[data-data-quality-id="${incident.id}"]`);
    await incidentRow.waitFor();
    await incidentRow.click();
    await page.locator("#open-data-quality-acknowledge").click();
    await page.locator("#command-assignee").fill(`${marker}_OWNER`);
    await page.locator("#confirm-reason").fill(`${marker} source clock inspected and assigned`);
    await page.locator("#confirm-submit").click();
    await page.locator("#open-data-quality-resolve").waitFor();
    await page.locator("#open-data-quality-resolve").click();
    await page.locator("#confirm-reason").fill(`${marker} clock source corrected and evidence rechecked`);
    await page.locator("#confirm-submit").click();
    await page.getByText("事件已解决，原始数据和处置审计已保留").waitFor();

    await page.locator('[data-view="shadowRuns"]').click();
    await page.locator(`[data-shadow-run-id="${run.id}"]`).click();
    const approveButton = page.getByRole("button", { name: "独立批准验收" });
    await approveButton.waitFor();
    assert(await approveButton.isEnabled(), "approval did not reopen after CRITICAL incident resolution");
    await approveButton.click();
    await page.locator("#confirm-reason").fill(`${marker} independent admin verified versions boundaries quantity and data quality`);
    const approvalResponse = page.waitForResponse((response) => response.request().method() === "POST"
      && response.url().includes(`/bpi-api/shadow-runs/${run.id}/approve`));
    await page.getByRole("button", { name: "批准验收", exact: true }).click();
    assert((await approvalResponse).status() === 200, "approveShadowRun did not return 200 through the real page");
    await page.getByText("影子验收已批准").waitFor();
    await page.locator(".batch-state-band").getByText("APPROVED", { exact: true }).waitFor();

    run = (await jsonRequest(api, "GET", adapterUrl(`/shadow-runs/${run.id}`), { token: auth.ticket })).body?.data;
    const reviews = (await jsonRequest(api, "GET", adapterUrl(`/shadow-runs/${run.id}/batch-reviews`),
      { token: auth.ticket })).body?.data;
    assert(run.state === "APPROVED" && run.revision === 14, `final run is ${run.state}/r${run.revision}`);
    assert(run.createdBy === engineer && run.decidedBy === expectedApprover,
      `independent actors are ${run.createdBy}/${run.decidedBy}`);
    assert(reviews.length === 10, `final active reviews are ${reviews.length}`);
    const finalBatches = (await jsonRequest(api, "GET",
      adapterUrl(`/batches?plantId=${encodeURIComponent(plantId)}&lineId=${encodeURIComponent(lineId)}&state=CLOSED_RAW&limit=50`),
      { token: auth.ticket })).body?.data?.filter((batch) => batch.batchNo.startsWith(`${marker}_BATCH_`));
    assert(finalBatches.length === 10 && finalBatches.every((batch) => batch.state === "CLOSED_RAW"
      && batch.qualityGate === "NOT_APPLICABLE" && batch.wmsStatus === "NOT_REQUESTED"),
    "shadow acceptance mutated batch QA/WMS state");
    report.api.final = { state: run.state, revision: run.revision, createdBy: run.createdBy,
      decidedBy: run.decidedBy, metrics: run.metrics, blockers: run.blockers, reviews: reviews.length };
    report.externalWrites = {
      observed: false,
      evidence: "All ten batches remain CLOSED_RAW, qualityGate=NOT_APPLICABLE and wmsStatus=NOT_REQUESTED; browser requests stayed under /bpi-api.",
    };

    await page.screenshot({ path: screenshotPath, fullPage: true });
    assert(report.browser.consoleErrors.length === 0,
      `browser console errors: ${report.browser.consoleErrors.join(" | ")}`);
    assert(report.browser.pageErrors.length === 0,
      `browser page errors: ${report.browser.pageErrors.join(" | ")}`);
    assert(report.browser.requestFailures.length === 0,
      `browser request failures: ${JSON.stringify(report.browser.requestFailures)}`);
    const unexpectedResponses = report.browser.responses.filter((item) => item.status < 200 || item.status >= 300);
    assert(unexpectedResponses.length === 0,
      `real page emitted non-2xx BPI responses: ${JSON.stringify(unexpectedResponses)}`);
    report.status = "PASS";
    await context.close();
  } catch (error) {
    report.error = error instanceof Error ? error.stack || error.message : String(error);
    throw error;
  } finally {
    report.generatedAt = new Date().toISOString();
    fs.writeFileSync(reportPath, `${JSON.stringify(report, null, 2)}\n`, { mode: 0o600 });
    await browser?.close();
    await api.dispose();
  }
}

main()
  .then(() => console.log(`BPI shadow-run target acceptance PASS: ${reportPath}`))
  .catch((error) => {
    console.error(`BPI shadow-run target acceptance FAIL: ${error.message}`);
    process.exitCode = 1;
  });
