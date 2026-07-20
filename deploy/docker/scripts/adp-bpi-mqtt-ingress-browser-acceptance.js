#!/usr/bin/env node
"use strict";

const fs = require("node:fs");
const path = require("node:path");
const { chromium, request } = require("playwright");

const marker = required("BPI_ACCEPTANCE_MARKER");
const adpBaseUrl = required("ADP_BASE_URL").replace(/\/+$/, "");
const bpiBaseUrl = required("BPI_BROWSER_BASE_URL").replace(/\/+$/, "");
const username = required("ADP_USERNAME");
const password = required("ADP_PASSWORD");
const plantId = required("BPI_PLANT_ID");
const lineId = required("BPI_LINE_ID");
const productId = required("BPI_PRODUCT_ID");
const deviceId = required("BPI_DEVICE_ID");
const propertyId = required("BPI_PROPERTY_ID");
const sourcePropertyId = required("BPI_SOURCE_PROPERTY_ID");
const expectedRevision = required("BPI_EXPECTED_POINT_CATALOG_REVISION");
const expectedEpoch = integer("BPI_EXPECTED_SOURCE_EPOCH");
const expectedFirst = integer("BPI_EXPECTED_SOURCE_SEQUENCE_FIRST");
const expectedLast = integer("BPI_EXPECTED_SOURCE_SEQUENCE_LAST");
const expectedObservationCount = integer("BPI_EXPECTED_SOURCE_OBSERVATION_COUNT");
const expectedSequenceWindows = parseSequenceWindows(required("BPI_EXPECTED_SEQUENCE_WINDOWS"));
const timeoutMs = Number(process.env.BPI_BROWSER_TIMEOUT_MS || 120_000);
const headless = process.env.BPI_HEADLESS !== "false";
const reportPath = path.resolve(
  process.env.BPI_BROWSER_REPORT || `/tmp/${marker}-mqtt-ingress-browser.json`,
);
const pointScreenshotPath = path.resolve(
  process.env.BPI_POINT_SCREENSHOT || `/tmp/${marker}-mqtt-point-catalog.png`,
);
const dataQualityScreenshotPath = path.resolve(
  process.env.BPI_DATA_QUALITY_SCREENSHOT || `/tmp/${marker}-mqtt-data-quality.png`,
);

if (!/^[A-Za-z0-9._-]{8,80}$/.test(marker)) {
  throw new Error("BPI_ACCEPTANCE_MARKER must use 8-80 safe token characters");
}
if (!Number.isFinite(timeoutMs) || timeoutMs <= 0) {
  throw new Error("BPI_BROWSER_TIMEOUT_MS must be positive");
}
if (expectedFirst > expectedLast) {
  throw new Error("expected source sequence first cannot exceed last");
}

function required(key) {
  const value = process.env[key];
  if (!value || !value.trim()) throw new Error(`${key} is required`);
  return value.trim();
}

function integer(key) {
  const value = Number(required(key));
  if (!Number.isSafeInteger(value) || value < 0) {
    throw new Error(`${key} must be a non-negative safe integer`);
  }
  return value;
}

function parseSequenceWindows(value) {
  const windows = value.split(",").map((item) => item.trim()).filter(Boolean).map((item) => {
    const match = /^(\d+):(\d+)-(\d+)$/.exec(item);
    if (!match) throw new Error(`invalid sequence window: ${item}`);
    const [, epochValue, firstValue, lastValue] = match;
    const epoch = Number(epochValue);
    const first = Number(firstValue);
    const last = Number(lastValue);
    if (![epoch, first, last].every(Number.isSafeInteger) || epoch <= 0 || first <= 0 || first > last) {
      throw new Error(`invalid sequence window: ${item}`);
    }
    return { epoch, first, last };
  });
  if (!windows.length) throw new Error("at least one sequence window is required");
  return windows;
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
  assert(
    response.status() === 200,
    `GET ${route} returned ${response.status()}: ${parsed.text.slice(0, 500)}`,
  );
  return parsed.json;
}

function expectedSequenceKeys() {
  const result = [];
  for (const window of expectedSequenceWindows) {
    for (let sequence = window.first; sequence <= window.last; sequence += 1) {
      result.push(`${window.epoch}:${sequence}`);
    }
  }
  return result;
}

function assertPointCatalog(payload, report) {
  const data = payload?.data;
  assert(data?.snapshot, "current point catalog response has no snapshot");
  assert(data.snapshot.sourceRevision === expectedRevision, "point catalog revision does not match");
  assert(data.snapshot.pointCount === 1, `point catalog expected 1 point, got ${data.snapshot.pointCount}`);
  assert(data.snapshot.readyPointCount === 0, "unverified calibration unexpectedly became READY");
  const point = data.points?.find((item) => item.deviceId === deviceId);
  assert(point, `point catalog does not contain ${deviceId}`);
  assert(point.productId === productId, "point product identity does not match");
  assert(point.propertyId === propertyId, "canonical property identity does not match");
  assert(point.sourcePropertyId === sourcePropertyId, "source property identity does not match");
  assert(point.registered === true && point.propertyPresent === true, "point registration is incomplete");
  assert(point.deviceState === "ACTIVE", `point device state is ${point.deviceState}`);
  assert(point.sourceSequenceEnabled === true, "point source sequence declaration is disabled");
  assert(point.sourceSequenceRequired === true, "point does not require an authoritative source sequence");
  assert(point.sourceSequenceOrigin === "DEVICE", `point sequence origin is ${point.sourceSequenceOrigin}`);
  assert(point.sourceSequenceQualified === true, "point source sequence evidence is not qualified");
  assert(point.sourceSequenceEvidenceStatus === "QUALIFIED", "point evidence status is not QUALIFIED");
  assert(Number(point.sourceSequenceEpoch) === expectedEpoch, "point source epoch does not match");
  assert(Number(point.sourceSequenceFirst) === expectedFirst, "point source first sequence does not match");
  assert(Number(point.sourceSequenceLast) === expectedLast, "point source last sequence does not match");
  assert(
    Number(point.sourceSequenceObservationCount) === expectedObservationCount,
    "point source observation count does not match",
  );
  assert(point.calibrationStatus === "UNVERIFIED", "controlled pilot calibration must remain UNVERIFIED");
  assert(point.ready === false, "controlled pilot point unexpectedly became READY");
  assert(
    JSON.stringify([...point.readinessIssues].sort()) === JSON.stringify(["CALIBRATION_NOT_VERIFIED"]),
    `unexpected point readiness issues: ${JSON.stringify(point.readinessIssues)}`,
  );
  assert(Date.parse(point.sourceSequenceValidUntil) > Date.now(), "source sequence evidence is already expired");
  report.api.pointCatalog = {
    snapshotId: data.snapshot.id,
    sourceRevision: data.snapshot.sourceRevision,
    pointCount: data.snapshot.pointCount,
    readyPointCount: data.snapshot.readyPointCount,
    pointId: point.id,
    deviceState: point.deviceState,
    calibrationStatus: point.calibrationStatus,
    readinessIssues: point.readinessIssues,
    sourceSequenceEvidenceStatus: point.sourceSequenceEvidenceStatus,
    sourceSequenceQualified: point.sourceSequenceQualified,
    sourceSequenceOrigin: point.sourceSequenceOrigin,
    sourceSequenceEpoch: point.sourceSequenceEpoch,
    sourceSequenceFirst: point.sourceSequenceFirst,
    sourceSequenceLast: point.sourceSequenceLast,
    sourceSequenceObservationCount: point.sourceSequenceObservationCount,
    sourceSequenceValidUntil: point.sourceSequenceValidUntil,
    sourceSequenceEvidenceEventId: point.sourceSequenceEvidenceEventId,
    sourceSequenceEvidenceRevision: point.sourceSequenceEvidenceRevision,
  };
  return point;
}

function assertDataQuality(listPayload, detailPayload, report) {
  assert(Array.isArray(listPayload?.data), "data-quality response has no data array");
  const matches = listPayload.data.filter((incident) => (
    incident.deviceId === deviceId
      && incident.propertyId === propertyId
      && incident.issueCode === "POINT_QUALITY_UNCERTAIN"
  ));
  assert(matches.length === 1, `expected one controlled pilot incident, got ${matches.length}`);
  const incident = matches[0];
  assert(incident.state === "OPEN", `controlled pilot incident is ${incident.state}`);
  assert(incident.severity === "WARNING", `controlled pilot severity is ${incident.severity}`);

  const detail = detailPayload?.data;
  assert(detail?.incident?.id === incident.id, "data-quality detail identity does not match");
  assert(Array.isArray(detail.events), "data-quality detail has no raw events");
  const expectedKeys = expectedSequenceKeys();
  const actualEvents = detail.events.filter((event) => (
    event.headers?.stage === "telemetry-data-quality"
      && event.headers?.sequence_origin === "DEVICE"
      && event.headers?.quality_code === "UNCERTAIN"
  ));
  const actualKeys = new Set(actualEvents.map((event) => (
    `${event.headers.source_epoch}:${event.headers.sequence}`
  )));
  const missing = expectedKeys.filter((key) => !actualKeys.has(key));
  assert(!missing.length, `data-quality detail is missing source sequences: ${missing.join(",")}`);
  assert(incident.eventCount >= expectedKeys.length, "incident event count is below the accepted MQTT sample count");
  report.api.dataQuality = {
    incidentId: incident.id,
    issueCode: incident.issueCode,
    severity: incident.severity,
    state: incident.state,
    revision: incident.revision,
    eventCount: incident.eventCount,
    expectedSequenceKeys: expectedKeys,
    verifiedEvents: actualEvents
      .filter((event) => expectedKeys.includes(`${event.headers.source_epoch}:${event.headers.sequence}`))
      .map((event) => ({
        eventId: event.eventId,
        sourceEventId: event.sourceEventId,
        detectedAt: event.detectedAt,
        sourceEpoch: event.headers.source_epoch,
        sequence: event.headers.sequence,
        sequenceOrigin: event.headers.sequence_origin,
        qualityCode: event.headers.quality_code,
      }))
      .sort((left, right) => (
        Number(left.sourceEpoch) - Number(right.sourceEpoch)
          || Number(left.sequence) - Number(right.sequence)
      )),
  };
  return incident;
}

async function main() {
  fs.mkdirSync(path.dirname(reportPath), { recursive: true });
  fs.mkdirSync(path.dirname(pointScreenshotPath), { recursive: true });
  fs.mkdirSync(path.dirname(dataQualityScreenshotPath), { recursive: true });
  const api = await request.newContext({ ignoreHTTPSErrors: true });
  let browser;
  const report = {
    generatedAt: new Date().toISOString(),
    status: "FAIL",
    marker,
    scope: { plantId, lineId },
    source: { productId, deviceId, sourcePropertyId, propertyId },
    expected: {
      pointCatalogRevision: expectedRevision,
      sourceEpoch: expectedEpoch,
      sourceSequenceFirst: expectedFirst,
      sourceSequenceLast: expectedLast,
      sourceObservationCount: expectedObservationCount,
      sequenceWindows: expectedSequenceWindows,
    },
    loginStatus: null,
    api: {},
    browser: {
      pointRows: 0,
      dataQualityRows: 0,
      responses: [],
      consoleErrors: [],
      pageErrors: [],
      requestFailures: [],
    },
    screenshots: {
      pointCatalog: pointScreenshotPath,
      dataQuality: dataQualityScreenshotPath,
    },
    error: null,
  };

  try {
    const auth = await login(api);
    report.loginStatus = auth.status;
    const pointQuery = new URLSearchParams({ plantId, lineId, limit: "100", search: deviceId });
    const pointPayload = await adapterGet(api, auth.ticket, `/point-catalog/current?${pointQuery}`);
    const point = assertPointCatalog(pointPayload, report);
    const incidentQuery = new URLSearchParams({
      plantId,
      lineId,
      state: "OPEN",
      search: deviceId,
      limit: "100",
    });
    const incidentPayload = await adapterGet(api, auth.ticket, `/data-quality/incidents?${incidentQuery}`);
    const matchingIncident = incidentPayload.data.find((incident) => (
      incident.deviceId === deviceId
        && incident.propertyId === propertyId
        && incident.issueCode === "POINT_QUALITY_UNCERTAIN"
    ));
    assert(matchingIncident, "controlled pilot data-quality incident is missing");
    const detailPayload = await adapterGet(
      api,
      auth.ticket,
      `/data-quality/incidents/${encodeURIComponent(matchingIncident.id)}`,
    );
    const incident = assertDataQuality(incidentPayload, detailPayload, report);

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
    await context.addInitScript(({ token, loginPayload, currentPlantId, currentLineId }) => {
      for (const key of ["suposTicket", "SUPOS_TICKET", "token", "ticket"]) {
        window.localStorage.setItem(key, token);
        window.sessionStorage.setItem(key, token);
      }
      if (loginPayload) window.localStorage.setItem("loginMsg", JSON.stringify(loginPayload));
      window.localStorage.setItem("language", "zh_CN");
      window.localStorage.setItem("langu_code", "zh_CN");
      window.localStorage.setItem("locale", "zh-cn");
      window.localStorage.setItem("bpi.plantId", currentPlantId);
      window.localStorage.setItem("bpi.lineId", currentLineId);
      window.localStorage.setItem("bpi.dataQualityLineId", currentLineId);
      window.localStorage.setItem("bpi.dataQualityState", "OPEN");
    }, {
      token: auth.ticket,
      loginPayload: auth.payload,
      currentPlantId: plantId,
      currentLineId: lineId,
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
      if (!url.pathname.includes("/bpi-api/")) return;
      report.browser.responses.push({
        method: response.request().method(),
        path: url.pathname,
        status: response.status(),
      });
    });

    await page.goto(`${bpiBaseUrl}/#/points`, { waitUntil: "networkidle", timeout: timeoutMs });
    await page.getByRole("heading", { name: "点位目录" }).waitFor();
    const pointRow = page.locator("[data-point-id]").filter({ hasText: deviceId });
    report.browser.pointRows = await pointRow.count();
    assert(report.browser.pointRows === 1, `browser expected one point row, got ${report.browser.pointRows}`);
    await pointRow.getByText("QUALIFIED", { exact: true }).waitFor();
    await pointRow.getByText("设备原生序列", { exact: true }).waitFor();
    await pointRow.getByText("校准证据未批准或已失效", { exact: true }).waitFor();
    await pointRow.getByRole("button", { name: "查看来源序列证据" }).click();
    const pointDrawer = page.locator("#detail-drawer");
    await pointDrawer.getByText(String(expectedEpoch), { exact: true }).waitFor();
    await pointDrawer.getByText(`${expectedFirst} - ${expectedLast}`, { exact: true }).waitFor();
    await pointDrawer.getByText(String(expectedObservationCount), { exact: true }).waitFor();
    await pointDrawer.getByText("当前证据与点位绑定指纹一致", { exact: false }).waitFor();
    await page.screenshot({ path: pointScreenshotPath, fullPage: true });
    await pointDrawer.locator("footer button[data-close-drawer]").click();

    await page.locator('button[data-view="dataQuality"]').click();
    await page.locator("#data-quality-search").fill(deviceId);
    await page.locator("#apply-data-quality-filter").click();
    const incidentRow = page.locator(`[data-data-quality-id="${incident.id}"]`);
    await incidentRow.waitFor();
    report.browser.dataQualityRows = await page.locator("[data-data-quality-id]").count();
    assert(
      report.browser.dataQualityRows === 1,
      `browser expected one controlled pilot incident row, got ${report.browser.dataQualityRows}`,
    );
    await incidentRow.locator("small").getByText("POINT_QUALITY_UNCERTAIN", { exact: true }).waitFor();
    await incidentRow.getByText(deviceId, { exact: true }).waitFor();
    await incidentRow.click();
    const incidentDrawer = page.locator("#detail-drawer");
    await incidentDrawer.getByText(`${deviceId} / ${propertyId}`, { exact: true }).waitFor();
    await incidentDrawer.getByText(`${incident.eventCount} 条，最多显示 100 条`, { exact: false }).waitFor();
    await page.screenshot({ path: dataQualityScreenshotPath, fullPage: true });

    assert(report.browser.consoleErrors.length === 0,
      `browser console errors: ${report.browser.consoleErrors.join(" | ")}`);
    assert(report.browser.pageErrors.length === 0,
      `browser page errors: ${report.browser.pageErrors.join(" | ")}`);
    assert(report.browser.requestFailures.length === 0,
      `browser request failures: ${JSON.stringify(report.browser.requestFailures)}`);
    assert(report.browser.responses.length > 0, "browser made no BPI API requests");
    assert(report.browser.responses.every((item) => item.status >= 200 && item.status < 300),
      `BPI API returned non-2xx: ${JSON.stringify(report.browser.responses)}`);
    assert(point.sourceSequenceQualified === true, "point qualification changed during browser test");
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
  .then(() => console.log(`BPI MQTT ingress browser acceptance PASS: ${reportPath}`))
  .catch((error) => {
    console.error(`BPI MQTT ingress browser acceptance FAIL: ${error.message}`);
    process.exitCode = 1;
  });
