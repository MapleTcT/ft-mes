#!/usr/bin/env node
"use strict";

const fs = require("node:fs");
const path = require("node:path");
const { chromium, request } = require("playwright");

const EXPECTED = new Map([
  ["CLOCK_DRIFT", "ERROR"],
  ["POINT_QUALITY_BAD", "ERROR"],
  ["SOURCE_SEQUENCE_DUPLICATE", "INFO"],
  ["SOURCE_SEQUENCE_GAP", "WARNING"],
]);

const marker = required("BPI_ACCEPTANCE_MARKER");
const adpBaseUrl = required("ADP_BASE_URL").replace(/\/+$/, "");
const bpiBaseUrl = required("BPI_BROWSER_BASE_URL");
const username = required("ADP_USERNAME");
const password = required("ADP_PASSWORD");
const plantId = required("BPI_PLANT_ID");
const lineId = required("BPI_LINE_ID");
const timeoutMs = Number(process.env.BPI_BROWSER_TIMEOUT_MS || 120_000);
const headless = process.env.BPI_HEADLESS !== "false";
const reportPath = path.resolve(
  process.env.BPI_BROWSER_REPORT || `/tmp/${marker}-flink-data-quality.json`,
);
const screenshotPath = path.resolve(
  process.env.BPI_BROWSER_SCREENSHOT || `/tmp/${marker}-flink-data-quality.png`,
);

if (!/^[A-Za-z0-9._-]{8,80}$/.test(marker)) {
  throw new Error("BPI_ACCEPTANCE_MARKER must use 8-80 safe token characters");
}
if (!Number.isFinite(timeoutMs) || timeoutMs <= 0) {
  throw new Error("BPI_BROWSER_TIMEOUT_MS must be positive");
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

function incidentRoute() {
  const parameters = new URLSearchParams({
    plantId,
    lineId,
    state: "OPEN",
    search: marker,
    limit: "100",
  });
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
    scope: { plantId, lineId },
    loginStatus: null,
    api: { incidents: [], details: [] },
    browser: {
      rows: 0,
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
    const list = await adapterGet(api, auth.ticket, incidentRoute());
    assert(Array.isArray(list?.data), "incident response has no data array");
    assert(list.data.length === EXPECTED.size,
      `expected ${EXPECTED.size} marker incidents, got ${list.data.length}`);
    const byCode = new Map(list.data.map((incident) => [incident.issueCode, incident]));
    assert(byCode.size === EXPECTED.size, "marker incident issue codes are not unique");
    for (const [code, severity] of EXPECTED) {
      const incident = byCode.get(code);
      assert(incident, `marker incident is missing ${code}`);
      assert(incident.severity === severity,
        `${code} severity is ${incident.severity}, expected ${severity}`);
      assert(incident.state === "OPEN", `${code} is not OPEN`);
      assert(incident.eventCount === 1, `${code} eventCount is not exactly one`);
      assert(incident.deviceId === `DEVICE-${marker}`, `${code} device identity is incorrect`);
      report.api.incidents.push({
        id: incident.id,
        issueCode: code,
        severity,
        state: incident.state,
        revision: incident.revision,
        eventCount: incident.eventCount,
      });

      const detail = await adapterGet(api, auth.ticket, `/data-quality/incidents/${incident.id}`);
      assert(detail?.data?.events?.length === 1, `${code} raw event count is not exactly one`);
      const event = detail.data.events[0];
      assert(event.headers?.stage === "telemetry-data-quality",
        `${code} raw event did not originate from the Flink telemetry detector`);
      assert(event.sourceEventId?.includes(marker), `${code} source event lost its marker`);
      report.api.details.push({
        id: incident.id,
        issueCode: code,
        rawEventCount: detail.data.events.length,
        producerStage: event.headers.stage,
        lifecycle: detail.data.lifecycle.map((item) => item.action),
      });
    }

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
      window.localStorage.setItem("bpi.dataQualityLineId", pointLineId);
      window.localStorage.setItem("bpi.dataQualityState", "OPEN");
    }, {
      token: auth.ticket,
      loginPayload: auth.payload,
      pointPlantId: plantId,
      pointLineId: lineId,
    });

    const page = await context.newPage();
    page.setDefaultTimeout(timeoutMs);
    page.on("console", (message) => {
      if (message.type() === "error") report.browser.consoleErrors.push(message.text());
    });
    page.on("pageerror", (error) => report.browser.pageErrors.push(error.message));
    page.on("requestfailed", (failed) => report.browser.requestFailures.push({
      method: failed.method(),
      path: new URL(failed.url()).pathname,
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
    const rows = page.locator("[data-data-quality-id]");
    await rows.first().waitFor();
    report.browser.rows = await rows.count();
    assert(report.browser.rows === EXPECTED.size,
      `browser expected ${EXPECTED.size} marker rows, got ${report.browser.rows}`);
    for (const incident of byCode.values()) {
      assert(await page.locator(`[data-data-quality-id="${incident.id}"]`).count() === 1,
        `browser table is missing incident ${incident.issueCode}`);
    }
    const pointRow = page.locator(
      `[data-data-quality-id="${byCode.get("POINT_QUALITY_BAD").id}"]`,
    );
    await pointRow.click();
    const drawer = page.locator("#detail-drawer");
    await drawer.getByText(`DEVICE-${marker}`, { exact: false }).waitFor();
    await drawer.getByText(`${marker}-FAULT`, { exact: false }).waitFor();
    await page.screenshot({ path: screenshotPath, fullPage: true });

    assert(report.browser.consoleErrors.length === 0,
      `browser console errors: ${report.browser.consoleErrors.join(" | ")}`);
    assert(report.browser.pageErrors.length === 0,
      `browser page errors: ${report.browser.pageErrors.join(" | ")}`);
    assert(report.browser.requestFailures.length === 0,
      `browser request failures: ${JSON.stringify(report.browser.requestFailures)}`);
    assert(report.browser.dataQualityResponses.length > 0, "browser made no data-quality requests");
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
  .then(() => console.log(`BPI Flink data-quality browser acceptance PASS: ${reportPath}`))
  .catch((error) => {
    console.error(`BPI Flink data-quality browser acceptance FAIL: ${error.message}`);
    process.exitCode = 1;
  });
