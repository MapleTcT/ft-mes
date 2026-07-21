#!/usr/bin/env node
"use strict";

const crypto = require("node:crypto");
const fs = require("node:fs");
const path = require("node:path");
const { chromium, request } = require("playwright");

const marker = required("BPI_ACCEPTANCE_MARKER");
const batchId = required("BPI_BATCH_ID");
const boundaryTime = required("BPI_BOUNDARY_TIME");
const adpBaseUrl = required("ADP_BASE_URL").replace(/\/+$/, "");
const username = required("ADP_USERNAME");
const password = required("ADP_PASSWORD");
const approverUsername = optional("BPI_APPROVER_USERNAME");
const approverPassword = optional("BPI_APPROVER_PASSWORD");
const useAdpApproverSession = Boolean(approverUsername && approverPassword);
const serviceBaseUrl = optional("BPI_SERVICE_BASE_URL").replace(/\/+$/, "");
const internalSecret = optional("BPI_INTERNAL_JWT_SECRET");
const plantId = process.env.BPI_PLANT_ID || "PLANT-01";
const lineId = process.env.BPI_LINE_ID || "LINE-S07-01";
const approver = process.env.BPI_APPROVER_SUBJECT
  || (useAdpApproverSession ? `legacy-ticket:${approverUsername}` : `${marker}_BPI_ADMIN`);
const timeoutMs = Number(process.env.BPI_BROWSER_TIMEOUT_MS || 120_000);
const executablePath = process.env.BPI_CHROMIUM_EXECUTABLE || undefined;
const reportPath = path.resolve(
  process.env.BPI_BROWSER_REPORT || `/tmp/${marker}-force-close.json`,
);
const screenshotPrefix = path.resolve(
  process.env.BPI_BROWSER_SCREENSHOT_PREFIX || `/tmp/${marker}-force-close`,
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
if (Boolean(approverUsername) !== Boolean(approverPassword)) {
  throw new Error("BPI_APPROVER_USERNAME and BPI_APPROVER_PASSWORD must be configured together");
}
if (useAdpApproverSession && approverUsername === username) {
  throw new Error("BPI_APPROVER_USERNAME must differ from ADP_USERNAME");
}
if (!useAdpApproverSession && !serviceBaseUrl) {
  throw new Error("BPI_SERVICE_BASE_URL is required for internal JWT approval mode");
}
if (!useAdpApproverSession && Buffer.byteLength(internalSecret, "utf8") < 32) {
  throw new Error("BPI_INTERNAL_JWT_SECRET must contain at least 32 UTF-8 bytes");
}
if (approver.length > 128) throw new Error("BPI_APPROVER_SUBJECT must not exceed 128 characters");
if (Number.isNaN(new Date(boundaryTime).getTime())) throw new Error("BPI_BOUNDARY_TIME must be an ISO timestamp");
if (Boolean(pendingReadyPath) !== Boolean(pendingContinuePath)) {
  throw new Error("BPI_PENDING_READY_FILE and BPI_PENDING_CONTINUE_FILE must be configured together");
}

function required(key) {
  const value = process.env[key];
  if (!value || !value.trim()) throw new Error(`${key} is required`);
  return value.trim();
}

function optional(key) {
  return String(process.env[key] || "").trim();
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
  const attempts = [
    { userName: loginUsername, password: loginPassword, clientId: "pc_dt" },
    { username: loginUsername, password: loginPassword, clientId: "pc_dt" },
  ];
  const failures = [];
  for (const data of attempts) {
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

function observePage(page, browserReport, session) {
  page.on("console", (message) => {
    if (message.type() === "error") browserReport.consoleErrors.push({
      session,
      text: message.text(),
      url: message.location().url || null,
    });
  });
  page.on("pageerror", (error) => browserReport.pageErrors.push({ session, message: error.message }));
  page.on("requestfailed", (failed) => browserReport.requestFailures.push({
    session,
    method: failed.method(),
    url: failed.url(),
    error: failed.failure()?.errorText || "",
  }));
  page.on("response", (response) => {
    if (response.url().includes("/bpi-api/") && response.status() >= 400) {
      browserReport.bpiHttpErrors.push({
        session,
        method: response.request().method(),
        url: response.url(),
        status: response.status(),
      });
    }
  });
}

function base64Url(value) {
  return Buffer.from(value).toString("base64url");
}

function internalToken() {
  const now = Math.floor(Date.now() / 1_000);
  const header = base64Url(JSON.stringify({ alg: "HS256", typ: "JWT" }));
  const payload = base64Url(JSON.stringify({
    iss: "ft-mes-adapter",
    aud: "bpi-service",
    sub: approver,
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

function localDateTime(iso) {
  const value = new Date(iso);
  if (Number.isNaN(value.getTime())) throw new Error(`invalid timestamp: ${iso}`);
  return new Date(value.getTime() - value.getTimezoneOffset() * 60_000)
    .toISOString()
    .slice(0, 19);
}

function sameInstant(left, right) {
  return typeof left === "string"
    && typeof right === "string"
    && new Date(left).getTime() === new Date(right).getTime();
}

async function waitForPendingEvidenceGate() {
  if (!pendingReadyPath || !pendingContinuePath) return;
  fs.mkdirSync(path.dirname(pendingReadyPath), { recursive: true });
  fs.writeFileSync(pendingReadyPath, `${JSON.stringify({
    marker,
    batchId,
    boundaryTime,
    state: "PENDING_APPROVAL",
    readyAt: new Date().toISOString(),
  }, null, 2)}\n`, "utf8");
  console.log(`BPI force-close pending evidence ready: ${pendingReadyPath}`);
  const deadline = Date.now() + timeoutMs;
  while (!fs.existsSync(pendingContinuePath)) {
    if (Date.now() >= deadline) {
      throw new Error(`Timed out waiting for pending evidence gate: ${pendingContinuePath}`);
    }
    await new Promise((resolve) => setTimeout(resolve, 250));
  }
}

function operation(response, parsed, payload, requestMetadata) {
  const browserRequest = typeof response.request === "function" ? response.request() : null;
  const headers = browserRequest ? browserRequest.headers() : requestMetadata;
  return {
    method: browserRequest ? browserRequest.method() : requestMetadata.method,
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

async function directApprove(api, expectedRevision) {
  const payload = {
    reason: `${marker} independent administrator verified stop evidence and boundary`,
    comment: "Controlled target acceptance approval; no QCS or WMS side effect is allowed",
    boundaryTime,
    approvalMode: "APPROVE",
  };
  const idempotencyKey = `${marker}-approve`;
  const response = await api.post(`${serviceBaseUrl}/bpi/v1/batches/${batchId}/force-close`, {
    data: payload,
    headers: {
      Authorization: `Bearer ${internalToken()}`,
      "Content-Type": "application/json",
      "Idempotency-Key": idempotencyKey,
      "If-Match": String(expectedRevision),
      "X-Trace-Id": `${marker}-approve`.slice(0, 128),
    },
    timeout: timeoutMs,
  });
  const parsed = await readBody(response);
  assert(response.status() === 202,
    `independent approval returned ${response.status()}: ${parsed.text.slice(0, 500)}`);
  assert(parsed.json?.data?.state === "COMPLETED", "independent approval did not complete the task");
  assert(parsed.json?.data?.decidedBy === approver, "independent approval lost the approver identity");
  return operation(response, parsed, payload, {
    method: "POST",
    "idempotency-key": idempotencyKey,
    "if-match": String(expectedRevision),
  });
}

async function main() {
  fs.mkdirSync(path.dirname(reportPath), { recursive: true });
  fs.mkdirSync(path.dirname(screenshotPrefix), { recursive: true });
  const api = await request.newContext({ ignoreHTTPSErrors: true });
  let browser;
  let approverContext;
  const report = {
    schemaVersion: 1,
    generatedAt: new Date().toISOString(),
    status: "FAIL",
    database: "PostgreSQL",
    marker,
    route: "/bpi/#/batches",
    batchId,
    boundaryTime,
    environment: {
      adpBaseUrl,
      serviceBaseUrl: serviceBaseUrl || null,
      tenantId: "1000",
      plantId,
      lineId,
    },
    approvalAuthentication: useAdpApproverSession ? "ADP_SESSION" : "INTERNAL_JWT",
    actors: {
      requesterLogin: username,
      requesterSubject: null,
      approverLogin: approverUsername || null,
      approverSubject: approver,
    },
    loginStatus: null,
    approverLoginStatus: null,
    operations: {},
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
    final: null,
    screenshots: {
      pending: `${screenshotPrefix}-pending.png`,
      completed: `${screenshotPrefix}-completed.png`,
    },
    error: null,
  };

  try {
    const unauthenticated = await api.get(
      `${adpBaseUrl}/bpi-api/batches/${encodeURIComponent(batchId)}/force-close`,
      { timeout: timeoutMs },
    );
    report.operations.unauthenticatedRead = {
      method: "GET",
      url: unauthenticated.url(),
      status: unauthenticated.status(),
    };
    assert([401, 403].includes(unauthenticated.status()),
      `unauthenticated force-close read returned ${unauthenticated.status()}`);

    const auth = await login(api, username, password, "requester");
    report.loginStatus = auth.status;
    browser = await chromium.launch({
      headless: true,
      executablePath,
      args: ["--no-proxy-server"],
    });
    const context = await authenticatedContext(browser, auth);

    const page = await context.newPage();
    page.setDefaultTimeout(timeoutMs);
    observePage(page, report.browser, "requester");

    await page.goto(`${adpBaseUrl}/bpi/#/batches`, { waitUntil: "networkidle", timeout: timeoutMs });
    await page.getByRole("heading", { name: "批次档案" }).waitFor();
    let row = page.locator(`[data-batch-id="${batchId}"]`);
    await row.waitFor();
    await row.getByText(marker, { exact: true }).waitFor();
    await row.click();

    const drawer = page.locator("#detail-drawer");
    await drawer.getByRole("heading", { name: marker }).waitFor();
    await drawer.getByRole("button", { name: "申请强制结束" }).click();
    const dialog = page.locator("#confirm-dialog");
    await dialog.getByRole("heading", { name: "申请强制结束批次" }).waitFor();
    await dialog.locator("#command-boundary-time").fill(localDateTime(boundaryTime));
    const requestReason = `${marker} shift lead verified pump stop, valve path and zero flow`;
    await dialog.locator("#confirm-reason").fill(requestReason);
    const requestResponsePromise = page.waitForResponse((response) => (
      response.request().method() === "POST"
        && response.url().endsWith(`/bpi-api/batches/${batchId}/force-close`)
    ));
    await dialog.getByRole("button", { name: "提交独立审批" }).click();
    const requestResponse = await requestResponsePromise;
    const requestBody = await readBody(requestResponse);
    assert(requestResponse.status() === 202,
      `browser request returned ${requestResponse.status()}: ${requestBody.text.slice(0, 500)}`);
    assert(requestBody.json?.data?.state === "PENDING_APPROVAL", "request did not enter PENDING_APPROVAL");
    assert(sameInstant(requestBody.json?.data?.boundaryTime, boundaryTime),
      "request changed the approved boundary time");
    report.operations.request = operation(requestResponse, requestBody, {
      reason: requestReason,
      boundaryTime,
      approvalMode: "REQUEST",
    });
    report.actors.requesterSubject = requestBody.json.data.requestedBy;
    assert(report.actors.requesterSubject !== approver, "acceptance actors are not independent");

    await page.getByText("强制结束申请已提交，等待独立管理员审批").waitFor();
    await drawer.locator('[data-force-close-state="PENDING_APPROVAL"]').waitFor();
    await drawer.getByText("BATCH_FORCE_CLOSE_REQUESTED", { exact: true }).waitFor();
    assert(await drawer.getByRole("button", { name: "暂停自动处理" }).count() === 0,
      "pending approval did not freeze suspend/resume commands");
    await page.screenshot({ path: report.screenshots.pending, fullPage: true });
    await waitForPendingEvidenceGate();

    await drawer.getByRole("button", { name: "批准并强制结束" }).click();
    await dialog.getByRole("heading", { name: "批准强制结束批次" }).waitFor();
    assert(await dialog.locator("#command-boundary-time").inputValue() === localDateTime(boundaryTime),
      "approval dialog did not recover the stored boundary time");
    const sameActorReason = `${marker} same requester must not approve this task`;
    await dialog.locator("#confirm-reason").fill(sameActorReason);
    const rejectedResponsePromise = page.waitForResponse((response) => (
      response.request().method() === "POST"
        && response.url().endsWith(`/bpi-api/batches/${batchId}/force-close`)
    ));
    await dialog.getByRole("button", { name: "批准并关闭批次" }).click();
    const rejectedResponse = await rejectedResponsePromise;
    const rejectedBody = await readBody(rejectedResponse);
    assert(rejectedResponse.status() === 403,
      `same-actor approval returned ${rejectedResponse.status()}: ${rejectedBody.text.slice(0, 500)}`);
    assert(/different administrator/i.test(rejectedBody.json?.detail || ""),
      "same-actor rejection did not explain separation of duties");
    report.operations.sameActorRejection = operation(rejectedResponse, rejectedBody, {
      reason: sameActorReason,
      boundaryTime,
      approvalMode: "APPROVE",
    });
    await dialog.getByRole("button", { name: "取消" }).click();

    let finalPage = page;
    let finalDrawer = drawer;
    let finalApiBase;
    let finalAuthorization;
    if (useAdpApproverSession) {
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
      await approverDrawer.locator('[data-force-close-state="PENDING_APPROVAL"]').waitFor();
      await approverDrawer.getByRole("button", { name: "批准并强制结束" }).click();
      const approverDialog = approverPage.locator("#confirm-dialog");
      await approverDialog.getByRole("heading", { name: "批准强制结束批次" }).waitFor();
      assert(await approverDialog.locator("#command-boundary-time").inputValue() === localDateTime(boundaryTime),
        "independent approval session did not recover the stored boundary time");
      const approvalReason = `${marker} formal ADP administrator independently verified the boundary`;
      await approverDialog.locator("#confirm-reason").fill(approvalReason);
      const approvalResponsePromise = approverPage.waitForResponse((response) => (
        response.request().method() === "POST"
          && response.url().endsWith(`/bpi-api/batches/${batchId}/force-close`)
      ));
      await approverDialog.getByRole("button", { name: "批准并关闭批次" }).click();
      const approvalResponse = await approvalResponsePromise;
      const approvalBody = await readBody(approvalResponse);
      assert(approvalResponse.status() === 202,
        `formal approver session returned ${approvalResponse.status()}: ${approvalBody.text.slice(0, 500)}`);
      assert(approvalBody.json?.data?.state === "COMPLETED",
        "formal approver session did not complete the task");
      report.operations.approval = operation(approvalResponse, approvalBody, {
        reason: approvalReason,
        boundaryTime,
        approvalMode: "APPROVE",
      });
      finalPage = approverPage;
      finalDrawer = approverDrawer;
      finalApiBase = `${adpBaseUrl}/bpi-api`;
      finalAuthorization = `Bearer ${approverAuth.ticket}`;
    } else {
      report.operations.approval = await directApprove(api, 2);
      await page.reload({ waitUntil: "networkidle", timeout: timeoutMs });
      await page.getByRole("heading", { name: "批次档案" }).waitFor();
      row = page.locator(`[data-batch-id="${batchId}"]`);
      await row.waitFor();
      await row.click();
      finalApiBase = `${serviceBaseUrl}/bpi/v1`;
      finalAuthorization = `Bearer ${internalToken()}`;
    }
    assert(report.operations.approval.response.requestedBy === report.actors.requesterSubject,
      "approval changed the requester identity");
    assert(report.operations.approval.response.decidedBy === approver,
      "approval did not use the independent administrator identity");

    await finalDrawer.locator('[data-force-close-state="COMPLETED"]').waitFor();
    await finalDrawer.locator(".batch-state-band").getByText("CLOSED_RAW", { exact: true }).waitFor();
    await finalDrawer.getByText("BATCH_FORCE_CLOSED", { exact: true }).waitFor();
    assert((await finalDrawer.locator(".batch-state-band").textContent()).includes("revision 3"),
      "completed batch did not advance to revision 3");
    assert(await finalDrawer.getByRole("button", { name: "申请强制结束" }).count() === 0,
      "completed batch still exposes force-close request");
    assert(await finalDrawer.getByRole("button", { name: "批准并强制结束" }).count() === 0,
      "completed batch still exposes force-close approval");

    const finalBatchResponse = await api.get(`${finalApiBase}/batches/${batchId}`, {
      headers: { Authorization: finalAuthorization },
      timeout: timeoutMs,
    });
    const finalBatch = await readBody(finalBatchResponse);
    const finalTaskResponse = await api.get(`${finalApiBase}/batches/${batchId}/force-close`, {
      headers: { Authorization: finalAuthorization },
      timeout: timeoutMs,
    });
    const finalTask = await readBody(finalTaskResponse);
    const finalTimelineResponse = await api.get(`${finalApiBase}/batches/${batchId}/timeline`, {
      headers: { Authorization: finalAuthorization },
      timeout: timeoutMs,
    });
    const finalTimeline = await readBody(finalTimelineResponse);
    assert(finalBatchResponse.status() === 200 && finalBatch.json?.data?.state === "CLOSED_RAW",
      "final API batch is not CLOSED_RAW");
    assert(finalBatch.json?.data?.revision === 3, "final API batch revision is not 3");
    assert(sameInstant(finalBatch.json?.data?.endTime, boundaryTime),
      "final API batch endTime differs from the approved boundary");
    assert(finalTaskResponse.status() === 200 && finalTask.json?.data?.state === "COMPLETED",
      "final API force-close task is not COMPLETED");
    assert(finalTask.json?.data?.requestedBy !== finalTask.json?.data?.decidedBy,
      "final task violates two-person separation");
    assert(finalTimelineResponse.status() === 200, "final timeline read failed");
    const actions = (finalTimeline.json?.data || []).map((item) => item.action);
    assert(actions.includes("BATCH_FORCE_CLOSE_REQUESTED") && actions.includes("BATCH_FORCE_CLOSED"),
      "final timeline is missing force-close lifecycle events");
    report.final = {
      batch: finalBatch.json.data,
      task: finalTask.json.data,
      timeline: finalTimeline.json.data,
    };

    report.browser.geometry = await finalPage.evaluate(() => ({
      viewportWidth: window.innerWidth,
      documentWidth: document.documentElement.scrollWidth,
      viewportHeight: window.innerHeight,
      documentHeight: document.documentElement.scrollHeight,
      drawerWidth: document.querySelector("#detail-drawer")?.getBoundingClientRect().width || 0,
    }));
    assert(report.browser.geometry.documentWidth <= report.browser.geometry.viewportWidth + 1,
      `page has horizontal overflow: ${JSON.stringify(report.browser.geometry)}`);
    await finalPage.screenshot({ path: report.screenshots.completed, fullPage: true });
    report.browser.url = finalPage.url();
    report.browser.title = await finalPage.title();
    report.browser.unexpectedBpiHttpErrors = report.browser.bpiHttpErrors.filter((item) => !(
      item.method === "POST"
        && item.url.endsWith(`/bpi-api/batches/${batchId}/force-close`)
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
    if (approverContext) {
      await approverContext.close();
      approverContext = null;
    }
    await context.close();
    console.log(`BPI force-close browser acceptance: PASS (${marker})`);
  } catch (error) {
    report.error = error?.message || String(error);
    throw error;
  } finally {
    report.generatedAt = new Date().toISOString();
    fs.writeFileSync(reportPath, `${JSON.stringify(report, null, 2)}\n`, "utf8");
    if (approverContext) await approverContext.close().catch(() => {});
    if (browser) await browser.close();
    await api.dispose();
  }
}

main().catch((error) => {
  console.error(error?.stack || error);
  process.exitCode = 1;
});
