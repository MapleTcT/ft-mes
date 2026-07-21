#!/usr/bin/env node
"use strict";

const fs = require("node:fs");
const path = require("node:path");
const { chromium, request } = require("playwright");

const marker = required("BPI_ACCEPTANCE_MARKER");
const batchId = required("BPI_BATCH_ID");
const originalDocumentId = required("BPI_ORIGINAL_DOCUMENT_ID");
const adpBaseUrl = required("ADP_BASE_URL").replace(/\/+$/, "");
const username = required("ADP_USERNAME");
const password = required("ADP_PASSWORD");
const approverUsername = required("BPI_APPROVER_USERNAME");
const approverPassword = required("BPI_APPROVER_PASSWORD");
const plantId = process.env.BPI_PLANT_ID || "PLANT-01";
const lineId = process.env.BPI_LINE_ID || "LINE-S07-01";
const timeoutMs = Number(process.env.BPI_BROWSER_TIMEOUT_MS || 120_000);
const executablePath = process.env.BPI_CHROMIUM_EXECUTABLE || undefined;
const reportPath = path.resolve(
  process.env.BPI_BROWSER_REPORT || `/tmp/${marker}-wms-inbound-reversal.json`,
);
const screenshotPrefix = path.resolve(
  process.env.BPI_BROWSER_SCREENSHOT_PREFIX || `/tmp/${marker}-wms-inbound-reversal`,
);
const pendingReadyPath = process.env.BPI_PENDING_READY_FILE
  ? path.resolve(process.env.BPI_PENDING_READY_FILE)
  : null;
const pendingContinuePath = process.env.BPI_PENDING_CONTINUE_FILE
  ? path.resolve(process.env.BPI_PENDING_CONTINUE_FILE)
  : null;

if (!/^[A-Za-z0-9_-]{8,100}$/.test(marker)) {
  throw new Error("BPI_ACCEPTANCE_MARKER must use 8-100 letters, digits, underscores or hyphens");
}
if (!/^[0-9a-f-]{36}$/i.test(batchId)) throw new Error("BPI_BATCH_ID must be a UUID");
if (approverUsername === username) {
  throw new Error("BPI_APPROVER_USERNAME must differ from ADP_USERNAME");
}
if (Boolean(pendingReadyPath) !== Boolean(pendingContinuePath)) {
  throw new Error("BPI_PENDING_READY_FILE and BPI_PENDING_CONTINUE_FILE must be configured together");
}
if (!Number.isFinite(timeoutMs) || timeoutMs < 30_000) {
  throw new Error("BPI_BROWSER_TIMEOUT_MS must be at least 30000");
}

function required(key) {
  const value = String(process.env[key] || "").trim();
  if (!value) throw new Error(`${key} is required`);
  return value;
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

async function readBody(response) {
  const text = await response.text();
  try {
    return { text, json: JSON.parse(text) };
  } catch (_error) {
    return { text, json: null };
  }
}

async function login(api, loginUsername, loginPassword, label) {
  const failures = [];
  for (const data of [
    { userName: loginUsername, password: loginPassword, clientId: "pc_dt" },
    { username: loginUsername, password: loginPassword, clientId: "pc_dt" },
  ]) {
    const response = await api.post(`${adpBaseUrl}/inter-api/auth/login`, {
      data,
      headers: { Accept: "application/json", "Content-Type": "application/json;charset=UTF-8" },
      timeout: timeoutMs,
    });
    const parsed = await readBody(response);
    const ticket = response.ok() ? findTicket(parsed.json) : null;
    if (ticket) return { ticket, status: response.status(), payload: parsed.json };
    failures.push({ status: response.status(), response: parsed.text.slice(0, 300) });
  }
  throw new Error(`ADP login failed for ${label}: ${JSON.stringify(failures)}`);
}

async function authenticatedContext(browser, auth) {
  const context = await browser.newContext({
    ignoreHTTPSErrors: true,
    viewport: { width: 1600, height: 1000 },
    extraHTTPHeaders: { Authorization: `Bearer ${auth.ticket}` },
  });
  await context.addCookies([
    { name: "suposTicket", value: auth.ticket, url: adpBaseUrl },
    { name: "SUPOS_TICKET", value: auth.ticket, url: adpBaseUrl },
  ]);
  await context.addInitScript(({ token, loginPayload, selectedPlantId, selectedLineId }) => {
    for (const key of ["suposTicket", "SUPOS_TICKET", "token", "ticket"]) {
      window.localStorage.setItem(key, token);
      window.sessionStorage.setItem(key, token);
    }
    window.localStorage.setItem("loginMsg", JSON.stringify(loginPayload));
    window.localStorage.setItem("language", "zh_CN");
    window.localStorage.setItem("langu_code", "zh_CN");
    window.localStorage.setItem("locale", "zh-cn");
    window.localStorage.setItem("bpi.plantId", selectedPlantId);
    window.localStorage.setItem("bpi.lineId", selectedLineId);
  }, { token: auth.ticket, loginPayload: auth.payload, selectedPlantId: plantId, selectedLineId: lineId });
  return context;
}

function observePage(page, report, session) {
  page.on("console", (message) => {
    if (message.type() === "error") report.consoleErrors.push({
      session,
      text: message.text(),
      url: message.location().url || null,
    });
  });
  page.on("pageerror", (error) => report.pageErrors.push({ session, message: error.message }));
  page.on("requestfailed", (failed) => report.requestFailures.push({
    session,
    method: failed.method(),
    url: failed.url(),
    error: failed.failure()?.errorText || "",
  }));
  page.on("response", (response) => {
    if (response.url().includes("/bpi-api/") && response.status() >= 400) {
      report.bpiHttpErrors.push({
        session,
        method: response.request().method(),
        url: response.url(),
        status: response.status(),
      });
    }
  });
}

function operation(response, parsed, payload) {
  const requestValue = response.request();
  const headers = requestValue.headers();
  return {
    method: requestValue.method(),
    url: response.url(),
    request: {
      payload,
      idempotencyKey: headers["idempotency-key"] || null,
      ifMatch: headers["if-match"] || null,
    },
    status: response.status(),
    response: parsed.json?.data || parsed.json || parsed.text.slice(0, 500),
  };
}

async function waitForPendingEvidenceGate() {
  if (!pendingReadyPath || !pendingContinuePath) return;
  fs.mkdirSync(path.dirname(pendingReadyPath), { recursive: true });
  fs.writeFileSync(pendingReadyPath, `${JSON.stringify({
    marker,
    batchId,
    state: "PENDING_APPROVAL",
    readyAt: new Date().toISOString(),
  }, null, 2)}\n`, "utf8");
  const deadline = Date.now() + timeoutMs;
  while (!fs.existsSync(pendingContinuePath)) {
    if (Date.now() >= deadline) {
      throw new Error(`Timed out waiting for pending evidence gate: ${pendingContinuePath}`);
    }
    await new Promise((resolve) => setTimeout(resolve, 250));
  }
}

async function main() {
  fs.mkdirSync(path.dirname(reportPath), { recursive: true });
  fs.mkdirSync(path.dirname(screenshotPrefix), { recursive: true });
  const api = await request.newContext({ ignoreHTTPSErrors: true });
  let browser;
  let requesterContext;
  let approverContext;
  const report = {
    schemaVersion: 1,
    generatedAt: new Date().toISOString(),
    status: "FAIL",
    database: "PostgreSQL",
    marker,
    route: "/bpi/#/batches",
    batchId,
    originalDocumentId,
    actors: {
      requesterLogin: username,
      requesterSubject: null,
      approverLogin: approverUsername,
      approverSubject: `legacy-ticket:${approverUsername}`,
    },
    loginStatus: null,
    approverLoginStatus: null,
    operations: {},
    final: null,
    browser: {
      url: null,
      title: null,
      consoleErrors: [],
      pageErrors: [],
      requestFailures: [],
      bpiHttpErrors: [],
      unexpectedBpiHttpErrors: [],
      geometry: null,
    },
    screenshots: {
      pending: `${screenshotPrefix}-pending.png`,
      approved: `${screenshotPrefix}-approved.png`,
    },
    error: null,
  };

  try {
    const unauthenticated = await api.get(
      `${adpBaseUrl}/bpi-api/batches/${encodeURIComponent(batchId)}/wms/reversal`,
      { timeout: timeoutMs },
    );
    report.operations.unauthenticatedRead = {
      method: "GET",
      url: unauthenticated.url(),
      status: unauthenticated.status(),
    };
    assert([401, 403].includes(unauthenticated.status()),
      `unauthenticated reversal read returned ${unauthenticated.status()}`);

    const auth = await login(api, username, password, "requester");
    report.loginStatus = auth.status;
    browser = await chromium.launch({
      headless: true,
      executablePath,
      args: ["--no-proxy-server"],
    });
    requesterContext = await authenticatedContext(browser, auth);
    const page = await requesterContext.newPage();
    page.setDefaultTimeout(timeoutMs);
    observePage(page, report.browser, "requester");

    await page.goto(`${adpBaseUrl}/bpi/#/batches`, { waitUntil: "networkidle", timeout: timeoutMs });
    await page.getByRole("heading", { name: "批次档案" }).waitFor();
    const row = page.locator(`[data-batch-id="${batchId}"]`);
    await row.waitFor();
    await row.getByText(marker, { exact: true }).waitFor();
    await row.click();
    const drawer = page.locator("#detail-drawer");
    const availableReversal = drawer.locator('[data-release-reversal="AVAILABLE"]');
    await availableReversal.waitFor();
    await availableReversal.getByText(originalDocumentId, { exact: false }).waitFor();

    await drawer.getByRole("button", { name: "申请入库冲销" }).click();
    const dialog = page.locator("#confirm-dialog");
    await dialog.getByRole("heading", { name: "申请完工入库冲销" }).waitFor();
    const requestReason = `${marker} original completion inbound document requires controlled reversal`;
    await dialog.locator("#confirm-reason").fill(requestReason);
    const requestResponsePromise = page.waitForResponse((response) => (
      response.request().method() === "POST"
        && response.url().endsWith(`/bpi-api/batches/${batchId}/wms/reversal`)
    ));
    await dialog.getByRole("button", { name: "提交独立审批" }).click();
    const requestResponse = await requestResponsePromise;
    const requestBody = await readBody(requestResponse);
    assert(requestResponse.status() === 202,
      `reversal request returned ${requestResponse.status()}: ${requestBody.text.slice(0, 500)}`);
    assert(requestBody.json?.data?.state === "PENDING_APPROVAL",
      "reversal request did not enter PENDING_APPROVAL");
    report.operations.request = operation(requestResponse, requestBody, {
      reason: requestReason,
      approvalMode: "REQUEST",
    });
    report.actors.requesterSubject = requestBody.json.data.requestedBy;
    assert(report.actors.requesterSubject !== report.actors.approverSubject,
      "requester and approver identities are not independent");

    await page.locator("#toast").getByText("入库冲销申请已提交，等待独立管理员审批", { exact: true }).waitFor();
    const pendingReversal = drawer.locator('[data-release-reversal="PENDING_APPROVAL"]');
    await pendingReversal.waitFor();
    assert(await drawer.locator("[data-original-document]").textContent() === originalDocumentId,
      "pending request changed the original document projection");
    await pendingReversal.scrollIntoViewIfNeeded();
    await page.screenshot({ path: report.screenshots.pending, fullPage: true });
    await waitForPendingEvidenceGate();

    await drawer.getByRole("button", { name: "独立审批冲销" }).click();
    await dialog.getByRole("heading", { name: "批准完工入库冲销" }).waitFor();
    const sameActorReason = `${marker} requester self approval must fail`;
    await dialog.locator("#confirm-reason").fill(sameActorReason);
    const rejectedResponsePromise = page.waitForResponse((response) => (
      response.request().method() === "POST"
        && response.url().endsWith(`/bpi-api/batches/${batchId}/wms/reversal`)
    ));
    await dialog.getByRole("button", { name: "批准并生成红单" }).click();
    const rejectedResponse = await rejectedResponsePromise;
    const rejectedBody = await readBody(rejectedResponse);
    assert(rejectedResponse.status() === 403,
      `same-requester approval returned ${rejectedResponse.status()}: ${rejectedBody.text.slice(0, 500)}`);
    assert(/different administrator/i.test(rejectedBody.json?.detail || ""),
      "same-requester rejection did not explain separation of duties");
    report.operations.sameActorRejection = operation(rejectedResponse, rejectedBody, {
      reason: sameActorReason,
      approvalMode: "APPROVE",
    });
    await dialog.getByRole("button", { name: "取消" }).click();

    const approverAuth = await login(api, approverUsername, approverPassword, "approver");
    report.approverLoginStatus = approverAuth.status;
    approverContext = await authenticatedContext(browser, approverAuth);
    const approverPage = await approverContext.newPage();
    approverPage.setDefaultTimeout(timeoutMs);
    observePage(approverPage, report.browser, "approver");
    await approverPage.goto(`${adpBaseUrl}/bpi/#/batches`, {
      waitUntil: "networkidle",
      timeout: timeoutMs,
    });
    await approverPage.getByRole("heading", { name: "批次档案" }).waitFor();
    const approverRow = approverPage.locator(`[data-batch-id="${batchId}"]`);
    await approverRow.waitFor();
    await approverRow.getByText(marker, { exact: true }).waitFor();
    await approverRow.click();
    const approverDrawer = approverPage.locator("#detail-drawer");
    await approverDrawer.locator('[data-release-reversal="PENDING_APPROVAL"]').waitFor();
    await approverDrawer.getByRole("button", { name: "独立审批冲销" }).click();
    const approverDialog = approverPage.locator("#confirm-dialog");
    await approverDialog.getByRole("heading", { name: "批准完工入库冲销" }).waitFor();
    await approverDialog.getByText(report.actors.requesterSubject, { exact: true }).waitFor();
    const approvalReason = `${marker} independent administrator verified blue document and quantity`;
    await approverDialog.locator("#confirm-reason").fill(approvalReason);
    const approvalResponsePromise = approverPage.waitForResponse((response) => (
      response.request().method() === "POST"
        && response.url().endsWith(`/bpi-api/batches/${batchId}/wms/reversal`)
    ));
    await approverDialog.getByRole("button", { name: "批准并生成红单" }).click();
    const approvalResponse = await approvalResponsePromise;
    const approvalBody = await readBody(approvalResponse);
    assert(approvalResponse.status() === 202,
      `independent approval returned ${approvalResponse.status()}: ${approvalBody.text.slice(0, 500)}`);
    assert(approvalBody.json?.data?.state === "PENDING_WMS",
      "independent approval did not enter PENDING_WMS");
    assert(approvalBody.json?.data?.decidedBy === report.actors.approverSubject,
      "independent approval lost the approver identity");
    assert(approvalBody.json?.data?.requestedBy === report.actors.requesterSubject,
      "independent approval changed the requester identity");
    report.operations.approval = operation(approvalResponse, approvalBody, {
      reason: approvalReason,
      approvalMode: "APPROVE",
    });

    await approverPage.locator("#toast").getByText("冲销已批准，红单命令已进入 WMS 队列", { exact: true }).waitFor();
    const approvedReversal = approverDrawer.locator('[data-release-reversal="PENDING_WMS"]');
    await approvedReversal.waitFor();
    await approverDrawer.locator(".batch-state-band").getByText("INBOUND_REVERSING", { exact: true }).waitFor();
    assert(await approverDrawer.locator("[data-original-document]").textContent() === originalDocumentId,
      "approval changed the original document projection");

    const headers = { Authorization: `Bearer ${approverAuth.ticket}` };
    const releaseResponse = await api.get(`${adpBaseUrl}/bpi-api/batches/${batchId}/release`, {
      headers,
      timeout: timeoutMs,
    });
    const releaseBody = await readBody(releaseResponse);
    const taskResponse = await api.get(`${adpBaseUrl}/bpi-api/batches/${batchId}/wms/reversal`, {
      headers,
      timeout: timeoutMs,
    });
    const taskBody = await readBody(taskResponse);
    const timelineResponse = await api.get(`${adpBaseUrl}/bpi-api/batches/${batchId}/timeline`, {
      headers,
      timeout: timeoutMs,
    });
    const timelineBody = await readBody(timelineResponse);
    assert(releaseResponse.status() === 200 && releaseBody.json?.data?.batch?.state === "INBOUND_REVERSING",
      "release projection is not INBOUND_REVERSING");
    assert(releaseBody.json?.data?.batch?.revision === 6,
      "release projection did not advance to batch revision 6");
    assert(releaseBody.json?.data?.wmsInbound?.documentId === originalDocumentId,
      "release projection changed the original blue document");
    assert(releaseBody.json?.data?.wmsInboundReversal?.state === "PENDING_WMS",
      "release projection lost the reversal task");
    assert(releaseBody.json?.data?.wmsInboundReversal?.reversalCommandEventId
      !== releaseBody.json?.data?.wmsInbound?.commandEventId,
    "red and blue command event identities must differ");
    assert(taskResponse.status() === 200 && taskBody.json?.data?.state === "PENDING_WMS",
      "reversal task API is not PENDING_WMS");
    assert(timelineResponse.status() === 200, "batch timeline read failed");
    const actions = (timelineBody.json?.data || []).map((item) => item.action);
    assert(actions.includes("WMS_INBOUND_REVERSAL_REQUESTED")
      && actions.includes("WMS_INBOUND_REVERSAL_APPROVED"),
    "batch timeline is missing reversal lifecycle events");
    report.final = {
      release: releaseBody.json.data,
      task: taskBody.json.data,
      timeline: timelineBody.json.data,
    };

    report.browser.geometry = await approverPage.evaluate(() => ({
      viewportWidth: window.innerWidth,
      documentWidth: document.documentElement.scrollWidth,
      viewportHeight: window.innerHeight,
      documentHeight: document.documentElement.scrollHeight,
      drawerWidth: document.querySelector("#detail-drawer")?.getBoundingClientRect().width || 0,
    }));
    assert(report.browser.geometry.documentWidth <= report.browser.geometry.viewportWidth + 1,
      `page has horizontal overflow: ${JSON.stringify(report.browser.geometry)}`);
    await approvedReversal.scrollIntoViewIfNeeded();
    await approverPage.screenshot({ path: report.screenshots.approved, fullPage: true });
    report.browser.url = approverPage.url();
    report.browser.title = await approverPage.title();
    report.browser.unexpectedBpiHttpErrors = report.browser.bpiHttpErrors.filter((item) => !(
      item.method === "POST"
        && item.url.endsWith(`/bpi-api/batches/${batchId}/wms/reversal`)
        && item.status === 403
    ));
    const unexpectedConsoleErrors = report.browser.consoleErrors.filter((item) => !(
      /403 \(Forbidden\)/i.test(item.text) || /status of 403/i.test(item.text)
    ));
    assert(unexpectedConsoleErrors.length === 0,
      `browser emitted unexpected console errors: ${JSON.stringify(unexpectedConsoleErrors)}`);
    assert(report.browser.pageErrors.length === 0,
      `browser emitted page errors: ${JSON.stringify(report.browser.pageErrors)}`);
    assert(report.browser.requestFailures.length === 0,
      `browser emitted failed requests: ${JSON.stringify(report.browser.requestFailures)}`);
    assert(report.browser.unexpectedBpiHttpErrors.length === 0,
      `browser emitted unexpected BPI HTTP errors: ${JSON.stringify(report.browser.unexpectedBpiHttpErrors)}`);
    report.status = "PASS";
    console.log(`BPI WMS inbound reversal browser acceptance: PASS (${marker})`);
  } catch (error) {
    report.error = error?.message || String(error);
    throw error;
  } finally {
    report.generatedAt = new Date().toISOString();
    fs.writeFileSync(reportPath, `${JSON.stringify(report, null, 2)}\n`, "utf8");
    if (approverContext) await approverContext.close().catch(() => {});
    if (requesterContext) await requesterContext.close().catch(() => {});
    if (browser) await browser.close();
    await api.dispose();
  }
}

main().catch((error) => {
  console.error(error?.stack || error);
  process.exitCode = 1;
});
