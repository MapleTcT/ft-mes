#!/usr/bin/env node
"use strict";

const crypto = require("node:crypto");
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
const timeoutMs = Number(process.env.BPI_BROWSER_TIMEOUT_MS || 120_000);
const headless = process.env.BPI_HEADLESS !== "false";
const reportPath = path.resolve(
  process.env.BPI_BROWSER_REPORT || `/tmp/${marker}-wms-reconciliation.json`,
);
const screenshotPath = path.resolve(
  process.env.BPI_BROWSER_SCREENSHOT || `/tmp/${marker}-wms-reconciliation.png`,
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
    loginStatus: null,
    before: null,
    command: null,
    replay: null,
    staleRevision: null,
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
    const initialResponse = await api.get(
      `${baseUrl}/bpi-api/batches/${encodeURIComponent(batchId)}/release`,
      { headers: { Authorization: `Bearer ${auth.ticket}` }, timeout: timeoutMs },
    );
    const initial = await readBody(initialResponse);
    assert(initialResponse.status() === 200,
      `initial release read returned ${initialResponse.status()}: ${initial.text.slice(0, 500)}`);
    const initialRelease = initial.json?.data;
    assert(initialRelease?.batch?.id === batchId, "initial release returned a different batch");
    assert(initialRelease.batch.batchNo === marker, "initial release lost the acceptance marker");
    assert(initialRelease.batch.state === "RELEASED", "acceptance batch is not RELEASED");
    assert(initialRelease.wmsInbound?.status === "PENDING", "WMS projection is not PENDING");
    assert(initialRelease.wmsInbound.outboxStatus === "PUBLISHED", "outbox is not PUBLISHED");
    assert(initialRelease.wmsInbound.commandEventId === expectedEventId,
      "initial command event identity changed");
    assert(initialRelease.wmsInbound.idempotencyKey === expectedWmsKey,
      "initial WMS idempotency key changed");
    assert(initialRelease.wmsInbound.revision === 1, "initial WMS revision is not 1");
    assert(initialRelease.wmsInbound.reconciliationAllowed === true,
      `reconciliation is blocked by ${initialRelease.wmsInbound.reconciliationBlockedReason}`);
    report.before = initialRelease;

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
    const row = page.locator(`[data-batch-id="${batchId}"]`);
    await row.waitFor();
    await row.getByText(marker, { exact: true }).waitFor();
    await row.click();
    const drawer = page.locator("#detail-drawer");
    await drawer.getByText("重新核对原单", { exact: true }).waitFor();
    await drawer.getByText(expectedEventId, { exact: true }).waitFor();
    await drawer.locator("#open-wms-reconcile").click();
    const dialog = page.locator("#confirm-dialog");
    await dialog.getByText("重新核对原 WMS 单据", { exact: true }).waitFor();
    await dialog.getByText("先查原单 · 同一幂等键", { exact: true }).waitFor();
    const reason = `${marker} receipt timeout query-first reconciliation`;
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
      `browser reconciliation returned ${commandResponse.status()}: ${commandBody.text.slice(0, 500)}`);
    assert(commandHeaders["if-match"] === "1", "browser command did not use WMS revision 1");
    assert((commandHeaders["idempotency-key"] || "").length >= 8,
      "browser command has no idempotency key");
    assert(commandBody.json?.data?.wmsInbound?.commandEventId === expectedEventId,
      "browser command created a different event");
    assert(commandBody.json?.data?.wmsInbound?.idempotencyKey === expectedWmsKey,
      "browser command changed the WMS idempotency key");
    assert(commandBody.json?.data?.wmsInbound?.reconciliationCount === 1,
      "browser command did not persist exactly one reconciliation");
    assert(commandBody.json?.data?.wmsInbound?.revision === 2,
      "browser command did not advance WMS revision to 2");
    report.command = {
      method: commandResponse.request().method(),
      url: commandResponse.url(),
      status: commandResponse.status(),
      request: { idempotencyKey: commandHeaders["idempotency-key"], ifMatch: commandHeaders["if-match"], reason },
      response: commandBody.json?.data || null,
    };

    const replayResponse = await api.post(
      `${baseUrl}/bpi-api/batches/${encodeURIComponent(batchId)}/wms/reconcile`,
      {
        headers: {
          Authorization: `Bearer ${auth.ticket}`,
          "Content-Type": "application/json",
          "Idempotency-Key": commandHeaders["idempotency-key"],
          "If-Match": "1",
        },
        data: { reason },
        timeout: timeoutMs,
      },
    );
    const replayBody = await readBody(replayResponse);
    assert(replayResponse.status() === 200, `idempotent replay returned ${replayResponse.status()}`);
    assert(replayResponse.headers()["idempotent-replay"] === "true",
      "idempotent replay header is missing");
    assert(replayBody.json?.data?.wmsInbound?.reconciliationCount === 1,
      "idempotent replay created another reconciliation");
    report.replay = {
      status: replayResponse.status(),
      idempotentReplay: replayResponse.headers()["idempotent-replay"] || null,
      reconciliationCount: replayBody.json?.data?.wmsInbound?.reconciliationCount,
    };

    const staleResponse = await api.post(
      `${baseUrl}/bpi-api/batches/${encodeURIComponent(batchId)}/wms/reconcile`,
      {
        headers: {
          Authorization: `Bearer ${auth.ticket}`,
          "Content-Type": "application/json",
          "Idempotency-Key": crypto.randomUUID(),
          "If-Match": "1",
        },
        data: { reason: `${marker} stale revision must fail` },
        timeout: timeoutMs,
      },
    );
    const staleBody = await readBody(staleResponse);
    assert(staleResponse.status() === 409, `stale revision returned ${staleResponse.status()}`);
    assert(staleBody.json?.currentRevision === 2, "stale revision response lost currentRevision=2");
    report.staleRevision = { status: staleResponse.status(), response: staleBody.json };

    const finalResponse = await api.get(
      `${baseUrl}/bpi-api/batches/${encodeURIComponent(batchId)}/release`,
      { headers: { Authorization: `Bearer ${auth.ticket}` }, timeout: timeoutMs },
    );
    const finalBody = await readBody(finalResponse);
    assert(finalResponse.status() === 200, `final release read returned ${finalResponse.status()}`);
    const finalInbound = finalBody.json?.data?.wmsInbound;
    assert(finalInbound?.commandEventId === expectedEventId, "final event identity changed");
    assert(finalInbound?.idempotencyKey === expectedWmsKey, "final WMS key changed");
    assert(finalInbound?.outboxStatus === "PENDING", "original outbox was not requeued");
    assert(finalInbound?.reconciliationCount === 1, "final reconciliation count is not 1");
    assert(finalInbound?.revision === 2, "final WMS revision is not 2");
    assert(finalBody.json?.data?.batch?.state === "RELEASED", "batch was falsely marked inbounded");
    report.after = finalBody.json.data;

    await page.screenshot({ path: screenshotPath, fullPage: true });
    assert(report.browser.consoleErrors.length === 0,
      `browser console errors: ${report.browser.consoleErrors.join(" | ")}`);
    assert(report.browser.pageErrors.length === 0,
      `browser page errors: ${report.browser.pageErrors.join(" | ")}`);
    assert(report.browser.requestFailures.length === 0,
      `browser request failures: ${JSON.stringify(report.browser.requestFailures)}`);
    assert(report.browser.bpiHttpErrors.length === 0,
      `browser BPI HTTP errors: ${JSON.stringify(report.browser.bpiHttpErrors)}`);
    report.status = "PASS";
    await context.close();
  } catch (error) {
    report.error = error instanceof Error ? error.stack || error.message : String(error);
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
