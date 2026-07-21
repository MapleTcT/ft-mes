#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { chromium, request } = require("playwright");

const action = required("BPI_BROWSER_ACTION");
const marker = required("BPI_ACCEPTANCE_MARKER");
const adpBaseUrl = required("ADP_BASE_URL").replace(/\/+$/, "");
const bpiBaseUrl = required("BPI_BROWSER_BASE_URL").replace(/\/+$/, "");
const username = required("ADP_USERNAME");
const password = required("ADP_PASSWORD");
const ruleCode = process.env.BPI_ACCEPTANCE_RULE_CODE || "";
const goldenSetId = process.env.BPI_ACCEPTANCE_GOLDEN_SET_ID || "";
const boundaryTime = process.env.BPI_ACCEPTANCE_BOUNDARY_TIME || "";
const orderId = process.env.BPI_ACCEPTANCE_ORDER_ID || `MO-${marker}`;
const expectedLineId = process.env.BPI_ACCEPTANCE_LINE_ID || "";
const expectedBatchId = process.env.BPI_ACCEPTANCE_BATCH_ID || "";
const expectedBatchState = (process.env.BPI_ACCEPTANCE_BATCH_STATE || "ACTIVE")
  .trim()
  .toUpperCase();
const expectedBoundaryType = (process.env.BPI_ACCEPTANCE_BOUNDARY_TYPE || "START")
  .trim()
  .toUpperCase();
const expectedRuntimeStatus = (process.env.BPI_ACCEPTANCE_EXPECTED_RUNTIME_STATUS || "")
  .trim()
  .toUpperCase();
const expectedPublishStatus = Number(process.env.BPI_ACCEPTANCE_EXPECTED_PUBLISH_STATUS || 200);
const expectedPublishDetail = process.env.BPI_ACCEPTANCE_EXPECTED_PUBLISH_DETAIL || "";
const expectedPublishToast = process.env.BPI_ACCEPTANCE_EXPECTED_PUBLISH_TOAST || "";
const outputPath = path.resolve(process.env.BPI_BROWSER_REPORT || `/tmp/bpi-joint-browser-${action}.json`);
const screenshotPath = path.resolve(process.env.BPI_BROWSER_SCREENSHOT || `/tmp/bpi-joint-browser-${action}.png`);
const headless = process.env.BPI_HEADLESS !== "false";
const timeoutMs = Number(process.env.BPI_BROWSER_TIMEOUT_MS || 120_000);

if (!new Set(["publish", "confirm", "read", "rule-read", "candidate-read", "candidate-absent", "batch-read"]).has(action)) {
  throw new Error("BPI_BROWSER_ACTION must be publish, confirm, read, rule-read, candidate-read, candidate-absent or batch-read");
}
if (action === "publish" && (!ruleCode || !goldenSetId || !boundaryTime)) {
  throw new Error("publish requires rule code, golden set ID and boundary time");
}
if (expectedRuntimeStatus && !new Set(["READY", "DEGRADED", "INACTIVE"]).has(expectedRuntimeStatus)) {
  throw new Error("BPI_ACCEPTANCE_EXPECTED_RUNTIME_STATUS must be READY, DEGRADED or INACTIVE");
}
if (!Number.isInteger(expectedPublishStatus) || expectedPublishStatus < 200 || expectedPublishStatus > 599) {
  throw new Error("BPI_ACCEPTANCE_EXPECTED_PUBLISH_STATUS must be an HTTP status from 200 to 599");
}
if (expectedPublishStatus !== 200 && expectedRuntimeStatus) {
  throw new Error("a blocked publication cannot assert a runtime status");
}
if (!new Set(["START", "END"]).has(expectedBoundaryType)) {
  throw new Error("BPI_ACCEPTANCE_BOUNDARY_TYPE must be START or END");
}
if (action === "rule-read" && (!ruleCode || !expectedRuntimeStatus)) {
  throw new Error("rule-read requires a rule code and expected runtime status");
}
if (action === "batch-read" && !expectedBatchId) {
  throw new Error("batch-read requires BPI_ACCEPTANCE_BATCH_ID");
}

function required(key) {
  const value = process.env[key];
  if (!value || !value.trim()) throw new Error(`${key} is required`);
  return value.trim();
}

function findTicket(payload) {
  const candidates = [
    payload && payload.ticket,
    payload && payload.access_token,
    payload && payload.token,
    payload && payload.data && payload.data.ticket,
    payload && payload.data && payload.data.access_token,
    payload && payload.data && payload.data.token,
    payload && payload.result && payload.result.ticket,
    payload && payload.result && payload.result.access_token,
    payload && payload.result && payload.result.token,
  ];
  return candidates.find((value) => typeof value === "string" && value.length > 20);
}

async function readJsonSafe(response) {
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
      headers: {
        Accept: "application/json, text/plain, */*",
        "Content-Type": "application/json;charset=UTF-8",
      },
      timeout: timeoutMs,
    });
    const parsed = await readJsonSafe(response);
    const ticket = response.ok() ? findTicket(parsed.json) : null;
    if (ticket) return { ticket, loginStatus: response.status(), loginPayload: parsed.json };
    failures.push({
      status: response.status(),
      contentType: response.headers()["content-type"] || "",
    });
  }
  throw new Error(`ADP login failed: ${JSON.stringify(failures)}`);
}

function localDateTime(iso) {
  const value = new Date(iso);
  if (Number.isNaN(value.getTime())) throw new Error(`invalid boundary time: ${iso}`);
  return new Date(value.getTime() - value.getTimezoneOffset() * 60_000)
    .toISOString()
    .slice(0, 19);
}

async function publishRule(page, evidence) {
  const boundary = new Date(boundaryTime);
  await page.goto(`${bpiBaseUrl}/#/rules`, { waitUntil: "networkidle", timeout: timeoutMs });
  await page.getByRole("heading", { name: "规则与拓扑" }).waitFor({ timeout: timeoutMs });
  const ruleRow = page.locator("[data-rule-id]").filter({ hasText: ruleCode });
  if (await ruleRow.count() !== 1) throw new Error(`expected one rule row for ${ruleCode}`);
  evidence.ruleId = await ruleRow.getAttribute("data-rule-id");
  await ruleRow.click();
  await page.getByRole("heading", { name: `${ruleCode}@1`, exact: true }).waitFor({ timeout: timeoutMs });
  await page.getByRole("button", { name: "运行历史回放" }).click();
  await page.locator("#simulation-from").fill(localDateTime(new Date(boundary.getTime() - 1_000).toISOString()));
  await page.locator("#simulation-to").fill(localDateTime(new Date(boundary.getTime() + 1_000).toISOString()));
  await page.locator("#simulation-calibration").fill("CAL-1");
  await page.locator("#simulation-golden").fill(goldenSetId);
  await page.locator("#simulation-submit").click();
  await page.getByText("历史回放通过，可提交发布", { exact: true }).waitFor({ timeout: timeoutMs });
  await page.getByRole("button", { name: "发布规则版本" }).click();
  await page.locator("#confirm-reason").fill(`历史 marker ${marker} 已回放通过并完成受控发布复核`);
  const publishResponsePromise = page.waitForResponse(
    (response) => response.url().includes(`/bpi-api/rules/${evidence.ruleId}/publish`)
      && response.request().method() === "POST",
  );
  await page.locator("#confirm-submit").click();
  const publishResponse = await publishResponsePromise;
  const publishBody = (await publishResponse.text()).slice(0, 2_000);
  evidence.publicationHttpStatus = publishResponse.status();
  if (publishResponse.status() !== expectedPublishStatus) {
    throw new Error(`rule publication returned ${publishResponse.status()}: ${publishBody}`);
  }
  if (expectedPublishStatus !== 200) {
    if (expectedPublishDetail && !publishBody.includes(expectedPublishDetail)) {
      throw new Error(`publication rejection did not include expected detail: ${publishBody}`);
    }
    const toast = page.locator("#toast");
    await toast.waitFor({ state: "visible", timeout: timeoutMs });
    evidence.publicationToast = (await toast.textContent() || "").trim();
    if (expectedPublishToast && !evidence.publicationToast.includes(expectedPublishToast)) {
      throw new Error(`publication toast did not include expected business message: ${evidence.publicationToast}`);
    }
    if (expectedPublishToast
      && (/Rule publication requires/.test(evidence.publicationToast)
        || /POINT_[A-Z_]+/.test(evidence.publicationToast))) {
      throw new Error(`publication toast exposed backend implementation details: ${evidence.publicationToast}`);
    }
    evidence.publicationBlocked = true;
    evidence.publicationResponse = publishBody;
    await page.screenshot({ path: screenshotPath, fullPage: true });
    return;
  }
  await page.getByText(new RegExp(`规则 ${escapeRegExp(ruleCode)}@1 已提交发布`)).waitFor({ timeout: timeoutMs });

  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    await page.waitForTimeout(2_000);
    await page.reload({ waitUntil: "networkidle", timeout: timeoutMs });
    const refreshedRow = page.locator("[data-rule-id]").filter({ hasText: ruleCode });
    await refreshedRow.click();
    if (await page.getByText("Flink 已应用", { exact: true }).count()) {
      evidence.applicationStatus = "APPLIED";
      evidence.ruleState = "PUBLISHED";
      if (!expectedRuntimeStatus || await captureExpectedRuntime(page, evidence)) {
        await page.screenshot({ path: screenshotPath, fullPage: true });
        return;
      }
    }
  }
  throw new Error(expectedRuntimeStatus
    ? `Flink application and runtime ${expectedRuntimeStatus} did not become visible before timeout`
    : "Flink application receipt did not become visible before timeout");
}

async function captureExpectedRuntime(page, evidence) {
  const statusText = `运行时 ${expectedRuntimeStatus}`;
  if (!await page.getByText(statusText, { exact: true }).count()) return false;
  const trace = page.locator("#detail-drawer .runtime-readiness-trace");
  await trace.waitFor({ timeout: timeoutMs });
  evidence.runtimeReadinessStatus = expectedRuntimeStatus;
  evidence.runtimeReadinessText = (await trace.innerText()).slice(0, 2_000);
  return true;
}

async function readRuleRuntime(page, evidence) {
  await page.goto(`${bpiBaseUrl}/#/rules`, { waitUntil: "networkidle", timeout: timeoutMs });
  await page.getByRole("heading", { name: "规则与拓扑" }).waitFor({ timeout: timeoutMs });
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    const ruleRow = page.locator("[data-rule-id]").filter({ hasText: ruleCode });
    if (await ruleRow.count() !== 1) throw new Error(`expected one rule row for ${ruleCode}`);
    evidence.ruleId = await ruleRow.getAttribute("data-rule-id");
    await ruleRow.click();
    if (await captureExpectedRuntime(page, evidence)) {
      evidence.ruleState = "PUBLISHED";
      await page.screenshot({ path: screenshotPath, fullPage: true });
      return;
    }
    await page.waitForTimeout(2_000);
    await page.reload({ waitUntil: "networkidle", timeout: timeoutMs });
  }
  throw new Error(`runtime ${expectedRuntimeStatus} did not become visible before timeout`);
}

async function confirmCandidate(page, evidence) {
  await page.goto(`${bpiBaseUrl}/#/candidates`, { waitUntil: "networkidle", timeout: timeoutMs });
  await page.getByRole("heading", { name: "候选批次" }).waitFor({ timeout: timeoutMs });
  let row = page.locator("[data-candidate-id]");
  if (expectedBoundaryType === "START") row = row.filter({ hasText: orderId });
  else {
    if (expectedLineId) row = row.filter({ hasText: expectedLineId });
    row = row.filter({ hasText: "END" });
  }
  if (await row.count() !== 1) {
    throw new Error(`expected one ${expectedBoundaryType} candidate row for ${expectedLineId || orderId}`);
  }
  evidence.candidateId = await row.getAttribute("data-candidate-id");
  await row.click();
  if (expectedBoundaryType === "END") {
    await page.locator("#detail-drawer").getByText("END 候选", { exact: true }).waitFor({ timeout: timeoutMs });
  }
  await page.locator("#open-confirm").click();
  await page.locator("#confirm-reason").fill(
    `现场复核 marker ${marker} 的 ${expectedBoundaryType} 生产上下文和瞬时流量证据`,
  );
  await page.locator("#confirm-submit").click();
  await page.getByRole("heading", { name: "批次档案" }).waitFor({ timeout: timeoutMs });
  await page.getByText("SHADOW", { exact: true }).last().waitFor({ timeout: timeoutMs });
  const expectedBatchState = expectedBoundaryType === "END" ? "CLOSED_RAW" : "ACTIVE";
  await page.getByText(expectedBatchState, { exact: true }).last().waitFor({ timeout: timeoutMs });
  evidence.candidateState = "CONFIRMED";
  evidence.boundaryType = expectedBoundaryType;
  evidence.batchState = expectedBatchState;
  evidence.shadow = true;
  await page.screenshot({ path: screenshotPath, fullPage: true });
}

async function readCandidate(page, evidence) {
  await page.goto(`${bpiBaseUrl}/#/candidates`, { waitUntil: "networkidle", timeout: timeoutMs });
  await page.getByRole("heading", { name: "候选批次" }).waitFor({ timeout: timeoutMs });
  const row = page.locator("[data-candidate-id]").filter({ hasText: orderId });
  if (await row.count() !== 1) throw new Error(`expected one candidate row for ${orderId}`);
  evidence.candidateId = await row.getAttribute("data-candidate-id");
  evidence.candidateRowText = (await row.innerText()).trim();
  await row.click();
  const drawer = page.locator("#detail-drawer");
  await drawer.waitFor({ state: "visible", timeout: timeoutMs });
  if (expectedLineId) {
    await drawer.getByRole("heading", { name: expectedLineId, exact: true }).waitFor({ timeout: timeoutMs });
  }
  if (expectedBoundaryType === "START") {
    await drawer.getByText(orderId, { exact: true }).waitFor({ timeout: timeoutMs });
  } else {
    await drawer.getByText("END 候选", { exact: true }).waitFor({ timeout: timeoutMs });
  }
  await page.waitForFunction(() => {
    const current = document.querySelector("#detail-drawer");
    if (!current) return false;
    const bounds = current.getBoundingClientRect();
    return bounds.left >= -1 && bounds.right <= window.innerWidth + 1;
  }, undefined, { timeout: timeoutMs });
  evidence.candidateDrawerText = (await drawer.innerText()).trim();
  evidence.drawerBounds = await drawer.evaluate((current) => {
    const bounds = current.getBoundingClientRect();
    return {
      left: bounds.left,
      right: bounds.right,
      width: bounds.width,
      viewportWidth: window.innerWidth,
    };
  });
  evidence.pendingStateVisible = await drawer.getByText("PENDING", { exact: true }).count() === 1;
  evidence.confirmEnabled = await drawer.locator("#open-confirm").isEnabled();
  evidence.rejectEnabled = await drawer.locator("#open-reject").isEnabled();
  if (!evidence.pendingStateVisible || !evidence.confirmEnabled || !evidence.rejectEnabled) {
    throw new Error("candidate drawer did not expose one actionable PENDING candidate");
  }
  await page.screenshot({ path: screenshotPath, fullPage: true });
}

async function readCandidateAbsence(page, evidence) {
  await page.goto(`${bpiBaseUrl}/#/candidates`, { waitUntil: "networkidle", timeout: timeoutMs });
  await page.getByRole("heading", { name: "候选批次" }).waitFor({ timeout: timeoutMs });
  const rows = page.locator("[data-candidate-id]");
  const matchingRows = rows.filter({ hasText: orderId });
  evidence.pendingCandidateCount = await rows.count();
  evidence.matchingOrderRows = await matchingRows.count();
  evidence.emptyStateVisible = await page.getByText("没有待审核候选", { exact: true }).count() === 1;
  if (evidence.matchingOrderRows !== 0 || (evidence.pendingCandidateCount === 0 && !evidence.emptyStateVisible)) {
    throw new Error(`candidate cleanup is not visible in the browser for ${orderId}`);
  }
  await page.screenshot({ path: screenshotPath, fullPage: true });
}

async function readBatch(page, evidence) {
  await page.goto(`${bpiBaseUrl}/#/batches`, { waitUntil: "networkidle", timeout: timeoutMs });
  await page.getByRole("heading", { name: "批次档案" }).waitFor({ timeout: timeoutMs });
  const row = page.locator(`[data-batch-id="${expectedBatchId}"]`);
  if (await row.count() !== 1) throw new Error(`expected one batch row for ${expectedBatchId}`);
  evidence.batchId = expectedBatchId;
  evidence.batchRowText = (await row.innerText()).trim();
  await row.click();
  const drawer = page.locator("#detail-drawer");
  await drawer.waitFor({ state: "visible", timeout: timeoutMs });
  await drawer.getByText(orderId, { exact: true }).waitFor({ timeout: timeoutMs });
  if (expectedLineId) {
    await drawer.getByText(new RegExp(`^${escapeRegExp(expectedLineId)}\\s*/`)).waitFor({ timeout: timeoutMs });
  }
  await drawer.getByText(expectedBatchState, { exact: true }).waitFor({ timeout: timeoutMs });
  await drawer.getByText("SHADOW", { exact: true }).waitFor({ timeout: timeoutMs });
  evidence.batchState = expectedBatchState;
  evidence.shadow = true;
  evidence.batchDrawerText = (await drawer.innerText()).trim();
  evidence.drawerBounds = await drawer.evaluate((current) => {
    const bounds = current.getBoundingClientRect();
    return {
      left: bounds.left,
      right: bounds.right,
      width: bounds.width,
      viewportWidth: window.innerWidth,
    };
  });
  if (evidence.drawerBounds.left < -1 || evidence.drawerBounds.right > evidence.drawerBounds.viewportWidth + 1) {
    throw new Error("batch drawer extends outside the viewport");
  }
  await page.screenshot({ path: screenshotPath, fullPage: true });
}

async function readOverview(page, evidence) {
  await page.goto(`${bpiBaseUrl}/#/overview`, { waitUntil: "networkidle", timeout: timeoutMs });
  await page.getByRole("heading", { name: "实时生产态势" }).waitFor({ timeout: timeoutMs });
  evidence.brand = await page.title();
  evidence.overviewVisible = true;
  evidence.shadowModeVisible = await page.getByText("SHADOW", { exact: true }).count() > 0;
  await page.screenshot({ path: screenshotPath, fullPage: true });
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

async function main() {
  fs.mkdirSync(path.dirname(outputPath), { recursive: true });
  fs.mkdirSync(path.dirname(screenshotPath), { recursive: true });
  const api = await request.newContext({ ignoreHTTPSErrors: true });
  let browser;
  const report = {
    generatedAt: new Date().toISOString(),
    status: "FAIL",
    action,
    marker,
    orderId,
    expectedBatchId: expectedBatchId || null,
    expectedBatchState,
    expectedBoundaryType,
    expectedRuntimeStatus: expectedRuntimeStatus || null,
    expectedPublishStatus,
    expectedPublishDetail: expectedPublishDetail || null,
    expectedPublishToast: expectedPublishToast || null,
    adpBaseUrl,
    bpiBaseUrl,
    loginStatus: null,
    page: {},
    requests: [],
    consoleErrors: [],
    expectedConsoleErrors: [],
    pageErrors: [],
    requestFailures: [],
    evidence: {},
    screenshot: screenshotPath,
    error: null,
  };
  try {
    const auth = await login(api);
    report.loginStatus = auth.loginStatus;
    browser = await chromium.launch({ headless, args: ["--no-proxy-server"] });
    const context = await browser.newContext({
      ignoreHTTPSErrors: true,
      extraHTTPHeaders: { Authorization: `Bearer ${auth.ticket}` },
    });
    await context.addCookies([
      { name: "suposTicket", value: auth.ticket, url: bpiBaseUrl },
      { name: "SUPOS_TICKET", value: auth.ticket, url: bpiBaseUrl },
    ]);
    await context.addInitScript(({ token, loginPayload }) => {
      window.localStorage.setItem("suposTicket", token);
      window.localStorage.setItem("SUPOS_TICKET", token);
      window.localStorage.setItem("token", token);
      window.localStorage.setItem("ticket", token);
      window.sessionStorage.setItem("suposTicket", token);
      window.sessionStorage.setItem("SUPOS_TICKET", token);
      window.sessionStorage.setItem("token", token);
      window.sessionStorage.setItem("ticket", token);
      if (loginPayload) window.localStorage.setItem("loginMsg", JSON.stringify(loginPayload));
      window.localStorage.setItem("language", "zh_CN");
      window.localStorage.setItem("langu_code", "zh_CN");
      window.localStorage.setItem("locale", "zh-cn");
    }, { token: auth.ticket, loginPayload: auth.loginPayload });
    const page = await context.newPage();
    page.setDefaultTimeout(timeoutMs);
    page.on("console", (message) => {
      if (message.type() !== "error") return;
      const expectedPublicationRejection = action === "publish"
        && expectedPublishStatus >= 400
        && message.text().includes(`status of ${expectedPublishStatus}`);
      if (expectedPublicationRejection) {
        report.expectedConsoleErrors.push(message.text());
        return;
      }
      report.consoleErrors.push(message.text());
    });
    page.on("pageerror", (error) => report.pageErrors.push(error.message));
    page.on("requestfailed", (failed) => {
      report.requestFailures.push({ method: failed.method(), url: failed.url(), error: failed.failure()?.errorText || "" });
    });
    page.on("response", async (response) => {
      if (!response.url().includes("/bpi-api/")) return;
      const requestValue = response.request();
      let responseBody = "";
      try {
        responseBody = (await response.text()).slice(0, 2_000);
      } catch (_error) {
        responseBody = "<unavailable>";
      }
      report.requests.push({
        method: requestValue.method(),
        url: response.url(),
        requestBody: (requestValue.postData() || "").slice(0, 2_000),
        status: response.status(),
        responseBody,
      });
    });

    if (action === "publish") await publishRule(page, report.evidence);
    else if (action === "confirm") await confirmCandidate(page, report.evidence);
    else if (action === "candidate-read") await readCandidate(page, report.evidence);
    else if (action === "candidate-absent") await readCandidateAbsence(page, report.evidence);
    else if (action === "batch-read") await readBatch(page, report.evidence);
    else if (action === "rule-read") await readRuleRuntime(page, report.evidence);
    else await readOverview(page, report.evidence);
    report.page = { url: page.url(), title: await page.title() };
    if (report.consoleErrors.length || report.pageErrors.length || report.requestFailures.length) {
      throw new Error("browser emitted console, page or request errors");
    }
    report.status = "PASS";
    await context.close();
  } catch (error) {
    report.error = error && error.message ? error.message : String(error);
    throw error;
  } finally {
    report.generatedAt = new Date().toISOString();
    fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`, "utf8");
    if (browser) await browser.close();
    await api.dispose();
  }
}

main().catch((error) => {
  console.error(error && error.stack ? error.stack : error);
  process.exitCode = 1;
});
