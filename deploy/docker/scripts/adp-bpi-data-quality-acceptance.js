#!/usr/bin/env node
"use strict";

const fs = require("node:fs");
const path = require("node:path");
const { chromium, request } = require("playwright");

const marker = required("BPI_ACCEPTANCE_MARKER");
const adpBaseUrl = required("ADP_BASE_URL").replace(/\/+$/, "");
const bpiBaseUrl = required("BPI_BROWSER_BASE_URL");
const username = required("ADP_USERNAME");
const password = required("ADP_PASSWORD");
const plantId = process.env.BPI_PLANT_ID || "PLANT-01";
const lineId = process.env.BPI_LINE_ID || "LINE-S07-01";
const timeoutMs = Number(process.env.BPI_BROWSER_TIMEOUT_MS || 120_000);
const headless = process.env.BPI_HEADLESS !== "false";
const assignee = `${marker}_OWNER`;
const reportPath = path.resolve(
  process.env.BPI_BROWSER_REPORT || `/tmp/${marker}-data-quality.json`,
);
const screenshotPath = path.resolve(
  process.env.BPI_BROWSER_SCREENSHOT || `/tmp/${marker}-data-quality.png`,
);

if (!/^[A-Za-z0-9_-]{8,80}$/.test(marker)) {
  throw new Error("BPI_ACCEPTANCE_MARKER must use 8-80 letters, digits, underscores or hyphens");
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
    if (ticket) return { ticket, status: response.status(), payload: parsed.json };
    failures.push(response.status());
  }
  throw new Error(`ADP login failed with statuses ${failures.join(",")}`);
}

async function adapterGet(api, ticket, route) {
  const response = await api.get(`${adpBaseUrl}/bpi-api${route}`, {
    headers: { Authorization: `Bearer ${ticket}` },
    timeout: timeoutMs,
  });
  const parsed = await readJson(response);
  assert(response.status() === 200,
    `GET ${route} returned ${response.status()}: ${parsed.text.slice(0, 500)}`);
  return parsed.json;
}

function dataQualityRoute(state = "") {
  const parameters = new URLSearchParams({ plantId, lineId, search: marker, limit: "100" });
  if (state) parameters.set("state", state);
  return `/data-quality/incidents?${parameters}`;
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
    loginStatus: null,
    incidentId: null,
    api: {},
    browser: {
      dataQualityResponses: [],
      consoleErrors: [],
      pageErrors: [],
      requestFailures: [],
    },
    screenshot: screenshotPath,
    error: null,
  };

  try {
    const auth = await login(api);
    report.loginStatus = auth.status;
    const initial = await adapterGet(api, auth.ticket, dataQualityRoute());
    assert(Array.isArray(initial?.data), "initial incident response has no data array");
    assert(initial.data.length === 1,
      `expected one marker incident, got ${initial.data.length}`);
    const incident = initial.data[0];
    assert(incident.lastDetail?.includes(marker), "marker is missing from incident detail");
    assert(["OPEN", "ACKNOWLEDGED", "RESOLVED"].includes(incident.state),
      `marker incident has unsupported state ${incident.state}`);
    report.incidentId = incident.id;
    report.api.before = {
      state: incident.state,
      revision: incident.revision,
      eventCount: incident.eventCount,
      affectedBatchCount: incident.affectedBatchCount,
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
    await context.addInitScript(({ token, loginPayload, pointPlantId, pointLineId, incidentState }) => {
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
      window.localStorage.setItem("bpi.dataQualityLineId", pointLineId);
      window.localStorage.setItem("bpi.dataQualityState", incidentState);
    }, {
      token: auth.ticket,
      loginPayload: auth.payload,
      pointPlantId: plantId,
      pointLineId: lineId,
      incidentState: incident.state,
    });

    const page = await context.newPage();
    page.setDefaultTimeout(timeoutMs);
    page.on("console", (message) => {
      if (message.type() === "error") report.browser.consoleErrors.push(message.text());
    });
    page.on("pageerror", (error) => report.browser.pageErrors.push(error.message));
    page.on("requestfailed", (failed) => report.browser.requestFailures.push({
      method: failed.method(),
      url: new URL(failed.url()).pathname,
      error: failed.failure()?.errorText || "",
    }));
    page.on("response", (response) => {
      const url = new URL(response.url());
      if (!url.pathname.includes("/bpi-api/data-quality/")) return;
      report.browser.dataQualityResponses.push({
        method: response.request().method(),
        path: url.pathname,
        status: response.status(),
      });
    });

    await page.goto(bpiBaseUrl, { waitUntil: "networkidle" });
    await page.locator('button[data-view="dataQuality"]').click();
    await page.locator("#data-quality-search").fill(marker);
    await page.locator("#apply-data-quality-filter").click();
    const row = page.locator(`[data-data-quality-id="${incident.id}"]`);
    await row.waitFor();
    assert((await row.textContent())?.includes(`${marker}_DEVICE`),
      "browser row does not contain the marker device");
    await row.click();
    const drawer = page.locator("#detail-drawer");
    await drawer.getByText(marker, { exact: false }).first().waitFor();
    if (incident.state === "OPEN") {
      await drawer.locator("#open-data-quality-acknowledge").click();
      await page.locator("#command-assignee").fill(assignee);
      await page.locator("#confirm-reason").fill(`${marker} acknowledge and assign after source review`);
      const acknowledgeResponse = page.waitForResponse((response) => (
        response.request().method() === "POST"
        && response.url().includes(`/bpi-api/data-quality/incidents/${incident.id}/acknowledge`)
      ));
      await page.locator("#confirm-submit").click();
      assert((await acknowledgeResponse).status() === 200, "acknowledge command did not return 200");
      await drawer.locator("#open-data-quality-resolve").waitFor();
      await drawer.locator(".batch-state-band .status", { hasText: "ACKNOWLEDGED" }).waitFor();
    }

    if (incident.state !== "RESOLVED") {
      await drawer.locator("#open-data-quality-resolve").click();
      await page.locator("#confirm-reason").fill(`${marker} root cause corrected and evidence rechecked`);
      const resolveResponse = page.waitForResponse((response) => (
        response.request().method() === "POST"
        && response.url().includes(`/bpi-api/data-quality/incidents/${incident.id}/resolve`)
      ));
      await page.locator("#confirm-submit").click();
      assert((await resolveResponse).status() === 200, "resolve command did not return 200");
      await drawer.locator(".batch-state-band .status", { hasText: "RESOLVED" }).waitFor();
    }
    await page.screenshot({ path: screenshotPath, fullPage: true });

    const detail = await adapterGet(api, auth.ticket, `/data-quality/incidents/${incident.id}`);
    assert(detail?.data?.incident?.state === "RESOLVED", "target incident is not RESOLVED");
    assert(detail.data.incident.assignee === assignee, "target incident assignee changed unexpectedly");
    assert(detail.data.events.length === 1, "raw marker event was not preserved exactly once");
    const actions = detail.data.lifecycle.map((item) => item.action);
    for (const expected of ["CREATED", "ACKNOWLEDGED", "RESOLVED"]) {
      assert(actions.includes(expected), `lifecycle is missing ${expected}`);
    }
    report.api.after = {
      state: detail.data.incident.state,
      revision: detail.data.incident.revision,
      eventCount: detail.data.incident.eventCount,
      rawEventCount: detail.data.events.length,
      actions,
    };
    assert(report.browser.consoleErrors.length === 0,
      `browser console errors: ${report.browser.consoleErrors.join(" | ")}`);
    assert(report.browser.pageErrors.length === 0,
      `browser page errors: ${report.browser.pageErrors.join(" | ")}`);
    assert(report.browser.requestFailures.length === 0,
      `browser request failures: ${JSON.stringify(report.browser.requestFailures)}`);
    assert(report.browser.dataQualityResponses.every((item) => item.status >= 200 && item.status < 300),
      `data-quality API returned non-2xx: ${JSON.stringify(report.browser.dataQualityResponses)}`);
    report.status = "PASS";
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
  .then(() => console.log(`BPI data-quality target acceptance PASS: ${reportPath}`))
  .catch((error) => {
    console.error(`BPI data-quality target acceptance FAIL: ${error.message}`);
    process.exitCode = 1;
  });
