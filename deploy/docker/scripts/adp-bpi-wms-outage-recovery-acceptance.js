#!/usr/bin/env node
"use strict";

const fs = require("node:fs");
const path = require("node:path");
const { chromium, request } = require("playwright");

const baseUrl = required("ADP_BASE_URL").replace(/\/+$/, "");
const username = required("ADP_USERNAME");
const password = required("ADP_PASSWORD");
const marker = required("BPI_ACCEPTANCE_MARKER");
const batchId = required("BPI_BATCH_ID");
const expectedEventId = required("BPI_COMMAND_EVENT_ID");
const expectedWmsKey = required("BPI_WMS_IDEMPOTENCY_KEY");
const plantId = process.env.BPI_PLANT_ID || "PLANT-01";
const lineId = process.env.BPI_LINE_ID || "LINE-S07-01";
const timeoutMs = Number(process.env.BPI_BROWSER_TIMEOUT_MS || 180_000);
const headless = process.env.BPI_HEADLESS !== "false";
const reportPath = path.resolve(
  process.env.BPI_BROWSER_REPORT || `/tmp/${marker}-wms-outage-recovery.json`,
);
const screenshotPath = path.resolve(
  process.env.BPI_BROWSER_SCREENSHOT || `/tmp/${marker}-wms-outage-recovery.png`,
);

if (!/^[A-Za-z0-9_-]{8,100}$/.test(marker)) {
  throw new Error("BPI_ACCEPTANCE_MARKER must use 8-100 letters, digits, underscores or hyphens");
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

async function readBody(response) {
  const text = await response.text();
  try {
    return { text, json: JSON.parse(text) };
  } catch (_error) {
    return { text, json: null };
  }
}

async function login(api) {
  const attempts = [
    { userName: username, password, clientId: "pc_dt" },
    { username, password, clientId: "pc_dt" },
  ];
  const failures = [];
  for (const data of attempts) {
    const response = await api.post(`${baseUrl}/inter-api/auth/login`, {
      data,
      headers: { Accept: "application/json", "Content-Type": "application/json;charset=UTF-8" },
      timeout: timeoutMs,
    });
    const parsed = await readBody(response);
    const ticket = response.ok() ? findTicket(parsed.json) : null;
    if (ticket) return { ticket, status: response.status(), payload: parsed.json };
    failures.push({ status: response.status(), response: parsed.text.slice(0, 300) });
  }
  throw new Error(`ADP login failed: ${JSON.stringify(failures)}`);
}

async function release(api, token) {
  const response = await api.get(
    `${baseUrl}/bpi-api/batches/${encodeURIComponent(batchId)}/release`,
    { headers: { Authorization: `Bearer ${token}` }, timeout: timeoutMs },
  );
  const parsed = await readBody(response);
  assert(response.status() === 200,
    `batch release returned ${response.status()}: ${parsed.text.slice(0, 500)}`);
  return parsed.json?.data;
}

async function waitForInbound(api, token) {
  const deadline = Date.now() + timeoutMs;
  let current = null;
  while (Date.now() < deadline) {
    current = await release(api, token);
    if (current?.batch?.state === "INBOUNDED"
        && current?.wmsInbound?.status === "ACCEPTED"
        && current?.wmsInbound?.documentId) return current;
    if (current?.wmsInbound?.status === "REJECTED") {
      throw new Error(`WMS recovery returned REJECTED: ${JSON.stringify(current.wmsInbound)}`);
    }
    await new Promise((resolve) => setTimeout(resolve, 1000));
  }
  throw new Error(`WMS recovery did not reach INBOUNDED: ${JSON.stringify(current)}`);
}

async function main() {
  fs.mkdirSync(path.dirname(reportPath), { recursive: true });
  fs.mkdirSync(path.dirname(screenshotPath), { recursive: true });
  const api = await request.newContext({ ignoreHTTPSErrors: true });
  let browser;
  const report = {
    generatedAt: new Date().toISOString(),
    status: "FAIL",
    database: "PostgreSQL",
    marker,
    scope: { tenantId: "1000", plantId, lineId },
    route: "/bpi/#/batches",
    batchId,
    commandEventId: expectedEventId,
    wmsIdempotencyKey: expectedWmsKey,
    loginStatus: null,
    before: null,
    reconciliation: null,
    after: null,
    browser: { consoleErrors: [], pageErrors: [], requestFailures: [], bpiHttpErrors: [] },
    screenshot: screenshotPath,
    error: null,
  };

  try {
    const unauthenticated = await api.get(
      `${baseUrl}/bpi-api/batches/${encodeURIComponent(batchId)}/release`,
      { timeout: timeoutMs },
    );
    assert([401, 403].includes(unauthenticated.status()),
      `unauthenticated release read returned ${unauthenticated.status()}`);
    const auth = await login(api);
    report.loginStatus = auth.status;
    const initial = await release(api, auth.ticket);
    assert(initial?.batch?.id === batchId, "initial release returned a different batch");
    assert(initial.batch.batchNo === marker, "initial release lost the marker");
    assert(initial.batch.state === "RELEASED", "batch is not RELEASED before recovery");
    assert(initial.wmsInbound?.status === "PENDING", "WMS status is not PENDING before recovery");
    assert(initial.wmsInbound.outboxStatus === "PUBLISHED", "outbox is not PUBLISHED before recovery");
    assert(initial.wmsInbound.commandEventId === expectedEventId, "command event changed before recovery");
    assert(initial.wmsInbound.idempotencyKey === expectedWmsKey, "WMS key changed before recovery");
    assert(initial.wmsInbound.revision === 1, "initial WMS link revision is not 1");
    assert(initial.wmsInbound.reconciliationAllowed === true,
      `reconciliation is blocked by ${initial.wmsInbound.reconciliationBlockedReason}`);
    report.before = initial;

    browser = await chromium.launch({ headless, args: ["--no-proxy-server"] });
    const context = await browser.newContext({
      ignoreHTTPSErrors: true,
      viewport: { width: 1600, height: 1000 },
      extraHTTPHeaders: { Authorization: `Bearer ${auth.ticket}` },
    });
    await context.addCookies([
      { name: "suposTicket", value: auth.ticket, url: baseUrl },
      { name: "SUPOS_TICKET", value: auth.ticket, url: baseUrl },
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

    const page = await context.newPage();
    page.setDefaultTimeout(timeoutMs);
    page.on("console", (message) => {
      if (message.type() === "error") report.browser.consoleErrors.push(message.text());
    });
    page.on("pageerror", (error) => report.browser.pageErrors.push(error.message));
    page.on("requestfailed", (failed) => report.browser.requestFailures.push({
      method: failed.method(),
      url: failed.url(),
      error: failed.failure()?.errorText || "",
    }));
    page.on("response", (response) => {
      if (response.url().includes("/bpi-api/") && response.status() >= 400) {
        report.browser.bpiHttpErrors.push({
          method: response.request().method(),
          url: response.url(),
          status: response.status(),
        });
      }
    });

    await page.goto(`${baseUrl}/bpi/#/batches`, { waitUntil: "networkidle", timeout: timeoutMs });
    await page.getByRole("heading", { name: "批次档案" }).waitFor();
    let row = page.locator(`[data-batch-id="${batchId}"]`);
    await row.waitFor();
    await row.getByText(marker, { exact: true }).waitFor();
    await row.click();
    let drawer = page.locator("#detail-drawer");
    await drawer.getByText("重新核对原单", { exact: true }).waitFor();
    await drawer.getByText(expectedEventId, { exact: true }).waitFor();
    await drawer.locator("#open-wms-reconcile").click();
    const dialog = page.locator("#confirm-dialog");
    await dialog.getByText("重新核对原 WMS 单据", { exact: true }).waitFor();
    const reason = `${marker} material-wms outage recovered; query original command`;
    await dialog.locator("#confirm-reason").fill(reason);
    const commandResponsePromise = page.waitForResponse((response) => (
      response.request().method() === "POST"
        && response.url().endsWith(`/bpi-api/batches/${batchId}/wms/reconcile`)
    ));
    await dialog.locator("#confirm-submit").click();
    const commandResponse = await commandResponsePromise;
    const commandBody = await readBody(commandResponse);
    const commandHeaders = commandResponse.request().headers();
    assert(commandResponse.status() === 200,
      `reconciliation returned ${commandResponse.status()}: ${commandBody.text.slice(0, 500)}`);
    assert(commandHeaders["if-match"] === "1", "reconciliation did not use WMS revision 1");
    assert((commandHeaders["idempotency-key"] || "").length >= 8,
      "reconciliation has no API idempotency key");
    assert(commandBody.json?.data?.wmsInbound?.commandEventId === expectedEventId,
      "reconciliation created a different command event");
    assert(commandBody.json?.data?.wmsInbound?.idempotencyKey === expectedWmsKey,
      "reconciliation changed the WMS idempotency key");
    report.reconciliation = {
      status: commandResponse.status(),
      request: {
        idempotencyKey: commandHeaders["idempotency-key"],
        ifMatch: commandHeaders["if-match"],
        reason,
      },
      response: commandBody.json?.data || null,
    };

    const accepted = await waitForInbound(api, auth.ticket);
    assert(accepted.wmsInbound.commandEventId === expectedEventId,
      "accepted receipt references a different command event");
    assert(accepted.wmsInbound.idempotencyKey === expectedWmsKey,
      "accepted receipt references a different WMS key");
    assert(accepted.wmsInbound.reconciliationCount === 1,
      "recovery did not persist exactly one manual reconciliation");
    report.after = accepted;

    await page.reload({ waitUntil: "networkidle", timeout: timeoutMs });
    await page.getByRole("heading", { name: "批次档案" }).waitFor();
    row = page.locator(`[data-batch-id="${batchId}"]`);
    await row.waitFor();
    await row.click();
    drawer = page.locator("#detail-drawer");
    await drawer.getByText("已入库", { exact: true }).waitFor();
    await drawer.getByText(accepted.wmsInbound.documentId, { exact: true }).first().waitFor();
    await drawer.getByText(expectedEventId, { exact: true }).waitFor();
    await page.screenshot({ path: screenshotPath, fullPage: true });

    assert(report.browser.consoleErrors.length === 0, "browser emitted console errors");
    assert(report.browser.pageErrors.length === 0, "browser emitted page errors");
    assert(report.browser.requestFailures.length === 0, "browser emitted failed requests");
    assert(report.browser.bpiHttpErrors.length === 0, "browser emitted BPI HTTP errors");
    report.status = "PASS_BROWSER_API_DURABLE_RECEIPT";
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
