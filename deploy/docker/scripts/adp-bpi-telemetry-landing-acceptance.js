#!/usr/bin/env node
"use strict";

const fs = require("node:fs");
const path = require("node:path");
const { execFileSync } = require("node:child_process");
const { chromium, request } = require("playwright");

const marker = required("BPI_ACCEPTANCE_MARKER");
const adpBaseUrl = required("ADP_BASE_URL").replace(/\/+$/, "");
const bpiBaseUrl = `${required("BPI_BROWSER_BASE_URL").replace(/#.*$/, "").replace(/\/+$/, "")}/`;
const username = required("ADP_USERNAME");
const password = required("ADP_PASSWORD");
const plantId = process.env.BPI_PLANT_ID || "PLANT-01";
const lineId = process.env.BPI_LINE_ID || "LINE-S07-01";
const productId = required("BPI_PRODUCT_ID");
const deviceId = required("BPI_DEVICE_ID");
const propertyId = required("BPI_PROPERTY_ID");
const calibrationVersion = required("BPI_CALIBRATION_VERSION");
const mqttWorkdir = path.resolve(required("BPI_MQTT_WORKDIR"));
const mqttScript = path.resolve(required("BPI_MQTT_PUBLISH_SCRIPT"));
const mqttMapping = path.resolve(required("BPI_MQTT_MAPPING_PATH"));
const mqttEpoch = positiveInteger("BPI_MQTT_SOURCE_EPOCH");
const mqttStartSequence = positiveInteger("BPI_MQTT_START_SEQUENCE");
const mqttCount = positiveInteger("BPI_MQTT_COUNT", 2);
const mqttMarker = `${marker}_WINDOW`;
const timeoutMs = Number(process.env.BPI_BROWSER_TIMEOUT_MS || 180_000);
const pollIntervalMs = Number(process.env.BPI_BROWSER_POLL_INTERVAL_MS || 2_000);
const headless = process.env.BPI_HEADLESS !== "false";
const executablePath = process.env.BPI_CHROMIUM_EXECUTABLE
  ? path.resolve(process.env.BPI_CHROMIUM_EXECUTABLE)
  : undefined;
const postgresContainer = process.env.BPI_POSTGRES_CONTAINER || "adp-mes-newbase-postgres-1";
const postgresUser = process.env.BPI_POSTGRES_USER || "adp";
const postgresDatabase = process.env.BPI_POSTGRES_DATABASE || "ft_mes_bpi";
const reportPath = path.resolve(
  process.env.BPI_BROWSER_REPORT || `/tmp/${marker}-telemetry-landing.json`,
);
const mqttReportPath = path.resolve(
  process.env.BPI_MQTT_REPORT || `/tmp/${mqttMarker}-mqtt.json`,
);
const desktopScreenshot = path.resolve(
  process.env.BPI_DESKTOP_SCREENSHOT || `/tmp/${marker}-telemetry-landing-desktop.png`,
);
const mobileScreenshot = path.resolve(
  process.env.BPI_MOBILE_SCREENSHOT || `/tmp/${marker}-telemetry-landing-mobile.png`,
);
const liveOverviewScreenshot = path.resolve(
  process.env.BPI_LIVE_OVERVIEW_SCREENSHOT || `/tmp/${marker}-live-overview.png`,
);
const liveDrawerScreenshot = path.resolve(
  process.env.BPI_LIVE_DRAWER_SCREENSHOT || `/tmp/${marker}-live-evidence-drawer.png`,
);

if (!/^[A-Za-z0-9_-]{8,80}$/.test(marker)) {
  throw new Error("BPI_ACCEPTANCE_MARKER must use 8-80 letters, digits, underscores or hyphens");
}
if (!/^[A-Za-z0-9_.:-]{1,128}$/.test(calibrationVersion)) {
  throw new Error("BPI_CALIBRATION_VERSION contains unsupported characters");
}
if (!Number.isInteger(timeoutMs) || timeoutMs < 10_000) {
  throw new Error("BPI_BROWSER_TIMEOUT_MS must be an integer of at least 10000");
}
if (!Number.isInteger(pollIntervalMs) || pollIntervalMs < 250) {
  throw new Error("BPI_BROWSER_POLL_INTERVAL_MS must be an integer of at least 250");
}
if (!fs.existsSync(mqttScript) || !fs.existsSync(mqttMapping)) {
  throw new Error("controlled MQTT publisher or mapping is missing");
}
if (executablePath) {
  try {
    fs.accessSync(executablePath, fs.constants.X_OK);
  } catch (_error) {
    throw new Error(`BPI_CHROMIUM_EXECUTABLE is not executable: ${executablePath}`);
  }
}

function required(key) {
  const value = process.env[key];
  if (!value || !value.trim()) throw new Error(`${key} is required`);
  return value.trim();
}

function positiveInteger(key, fallback) {
  const source = process.env[key] || (fallback === undefined ? "" : String(fallback));
  const value = Number(source);
  if (!Number.isSafeInteger(value) || value <= 0) {
    throw new Error(`${key} must be a positive safe integer`);
  }
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
    headers: { Accept: "application/json", Authorization: `Bearer ${ticket}` },
    timeout: timeoutMs,
  });
  const parsed = await readJson(response);
  assert(response.status() === 200,
    `GET ${route} returned ${response.status()}: ${parsed.text.slice(0, 1000)}`);
  return parsed.json?.data;
}

function observe(page, report) {
  page.on("console", (message) => {
    if (message.type() === "error") report.consoleErrors.push(message.text());
  });
  page.on("pageerror", (error) => report.pageErrors.push(error.message));
  page.on("requestfailed", (failed) => report.requestFailures.push({
    method: failed.method(),
    url: failed.url(),
    error: failed.failure()?.errorText || "",
  }));
  page.on("response", async (response) => {
    if (!response.url().includes("/bpi-api/")) return;
    const requestValue = response.request();
    let responseBody = "";
    try {
      responseBody = (await response.text()).slice(0, 4000);
    } catch (_error) {
      responseBody = "<unavailable>";
    }
    report.responses.push({
      method: requestValue.method(),
      path: new URL(response.url()).pathname,
      requestBody: (requestValue.postData() || "").slice(0, 4000),
      status: response.status(),
      responseBody,
    });
  });
}

async function newContext(browser, auth, viewport) {
  const context = await browser.newContext({
    ignoreHTTPSErrors: true,
    viewport,
    extraHTTPHeaders: { Authorization: `Bearer ${auth.ticket}` },
  });
  const origin = new URL(bpiBaseUrl).origin;
  await context.addCookies([
    { name: "suposTicket", value: auth.ticket, url: origin },
    { name: "SUPOS_TICKET", value: auth.ticket, url: origin },
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
  }, {
    token: auth.ticket,
    loginPayload: auth.payload,
    targetPlantId: plantId,
    targetLineId: lineId,
  });
  return context;
}

function publishWindowTelemetry() {
  const output = execFileSync("python3", [
    mqttScript,
    "--mapping", mqttMapping,
    "--marker", mqttMarker,
    "--source-epoch", String(mqttEpoch),
    "--start-sequence", String(mqttStartSequence),
    "--count", String(mqttCount),
    "--quality", "GOOD",
    "--value", "12.5",
    "--output", mqttReportPath,
  ], {
    cwd: mqttWorkdir,
    env: process.env,
    encoding: "utf8",
    timeout: timeoutMs,
    stdio: ["ignore", "pipe", "pipe"],
  }).trim();
  const report = JSON.parse(fs.readFileSync(mqttReportPath, "utf8"));
  assert(report.status === "PASS_CONTROLLED_MQTT_PUBLISH", "MQTT publisher did not report PASS");
  assert(report.messages?.length === mqttCount, "MQTT publisher report has an unexpected message count");
  assert(report.messages.every((message) => message.pubAck === true),
    "not every controlled MQTT message received PUBACK");
  return { output, report };
}

async function waitForCoverage(api, ticket, runId) {
  const deadline = Date.now() + timeoutMs;
  let latest;
  while (Date.now() < deadline) {
    latest = await adapterGet(api, ticket, `/shadow-runs/${runId}`);
    const coverage = latest?.telemetryCoverage;
    if (coverage?.fullyCovered
      && coverage.acceptedEventCount >= mqttCount
      && coverage.acceptedObservationCount >= mqttCount) {
      return latest;
    }
    await new Promise((resolve) => setTimeout(resolve, pollIntervalMs));
  }
  throw new Error(`telemetry coverage did not become ready: ${JSON.stringify(latest?.telemetryCoverage)}`);
}

function postgresEvidence(runId) {
  assert(/^[0-9a-f-]{36}$/i.test(runId), "runId is not a UUID");
  const sql = `
    WITH target_run AS (
      SELECT * FROM bpi.bpi_shadow_runs
       WHERE tenant_id = '1000' AND id = '${runId}'::uuid
    ), events AS (
      SELECT * FROM bpi.bpi_telemetry_events
       WHERE tenant_id = '1000' AND message_id LIKE '${mqttMarker}:%'
    ), points AS (
      SELECT point.*
        FROM bpi.bpi_telemetry_points point
        JOIN events event
          ON event.tenant_id = point.tenant_id
         AND event.id = point.telemetry_event_id
    ), latest AS (
      SELECT latest.*
        FROM bpi.bpi_telemetry_point_latest latest
       WHERE latest.tenant_id = '1000'
         AND latest.plant_id = '${plantId}'
         AND latest.line_id = '${lineId}'
         AND latest.product_id = '${productId}'
         AND latest.device_id = '${deviceId}'
         AND latest.property_id = '${propertyId}'
    )
    SELECT jsonb_build_object(
      'eventRows', (SELECT count(*) FROM events),
      'pointRows', (SELECT count(*) FROM points),
      'latestRows', (SELECT count(*) FROM latest),
      'latestValue', (SELECT numeric_value FROM latest),
      'latestUnit', (SELECT unit FROM latest),
      'latestQuality', (SELECT quality_code FROM latest),
      'latestSequenceDisposition', (SELECT sequence_disposition FROM latest),
      'latestCalibrationVersion', (SELECT calibration_version FROM latest),
      'latestEventIsControlled', (
        SELECT EXISTS (
          SELECT 1
            FROM latest
            JOIN events
              ON events.tenant_id = latest.tenant_id
             AND events.id = latest.telemetry_event_id
        )
      ),
      'rejectedRows', (
        SELECT count(*) FROM bpi.bpi_telemetry_point_rejects reject
         WHERE reject.tenant_id = '1000'
           AND reject.telemetry_event_id IN (SELECT id FROM events)
      ),
      'allPersistedInsideWindow', (
        SELECT bool_and(event.created_at >= run.started_at
                        AND event.event_time >= run.started_at
                        AND point.created_at >= run.started_at
                        AND point.sample_time >= run.started_at)
          FROM events event
          JOIN points point ON point.telemetry_event_id = event.id
          CROSS JOIN target_run run
      ),
      'sequences', (SELECT jsonb_agg(sequence ORDER BY sequence) FROM events),
      'dispositions', (SELECT jsonb_agg(sequence_disposition ORDER BY sequence) FROM events),
      'qualities', (SELECT jsonb_agg(quality_code ORDER BY sample_time) FROM points),
      'calibrationVersions', (
        SELECT jsonb_agg(calibration_version ORDER BY sample_time) FROM points
      ),
      'values', (SELECT jsonb_agg(numeric_value ORDER BY sample_time) FROM points)
    );
  `;
  const output = execFileSync("docker", [
    "exec", postgresContainer, "psql",
    "-U", postgresUser, "-d", postgresDatabase,
    "-v", "ON_ERROR_STOP=1", "-Atc", sql,
  ], { encoding: "utf8", timeout: timeoutMs }).trim();
  const evidence = JSON.parse(output.split("\n").at(-1));
  assert(Number(evidence.eventRows) === mqttCount, `PostgreSQL event rows are ${evidence.eventRows}`);
  assert(Number(evidence.pointRows) === mqttCount, `PostgreSQL point rows are ${evidence.pointRows}`);
  assert(Number(evidence.latestRows) === 1, `PostgreSQL latest rows are ${evidence.latestRows}`);
  assert(Number(evidence.latestValue) === 12.5,
    `PostgreSQL latest value is ${evidence.latestValue}`);
  assert(Boolean(evidence.latestUnit), "PostgreSQL latest unit is empty");
  assert(evidence.latestQuality === "GOOD",
    `PostgreSQL latest quality is ${evidence.latestQuality}`);
  assert(evidence.latestSequenceDisposition === "IN_ORDER",
    `PostgreSQL latest sequence disposition is ${evidence.latestSequenceDisposition}`);
  assert(evidence.latestCalibrationVersion === calibrationVersion,
    `PostgreSQL latest calibration is ${evidence.latestCalibrationVersion}`);
  assert(evidence.latestEventIsControlled === true,
    "PostgreSQL latest projection does not reference the controlled window");
  assert(Number(evidence.rejectedRows) === 0, `PostgreSQL rejected rows are ${evidence.rejectedRows}`);
  assert(evidence.allPersistedInsideWindow === true, "telemetry escaped the Shadow Run window");
  assert(evidence.qualities.every((quality) => quality === "GOOD"),
    `unexpected PostgreSQL qualities: ${JSON.stringify(evidence.qualities)}`);
  assert(evidence.calibrationVersions.every((version) => version === calibrationVersion),
    `unexpected calibration versions: ${JSON.stringify(evidence.calibrationVersions)}`);
  assert(evidence.dispositions.every((value) => value === "IN_ORDER"),
    `unexpected sequence dispositions: ${JSON.stringify(evidence.dispositions)}`);
  return evidence;
}

async function liveProjectionEvidence(api, ticket) {
  const overview = await adapterGet(
    api,
    ticket,
    `/overview?plantId=${encodeURIComponent(plantId)}&onlyAbnormal=false`,
  );
  assert(Array.isArray(overview), "overview response is not an array");
  const line = overview.find((item) => item.lineId === lineId);
  assert(line, `overview does not contain ${lineId}`);
  assert(Number(line.telemetry?.value) === 12.5,
    `overview latest value is ${line.telemetry?.value}`);
  assert(Boolean(line.telemetry?.unit), "overview latest unit is empty");
  assert(line.telemetry?.qualityCode === "GOOD",
    `overview quality is ${line.telemetry?.qualityCode}`);
  assert(line.telemetry?.productId === productId
    && line.telemetry?.deviceId === deviceId
    && line.telemetry?.propertyId === propertyId,
  `overview physical point is unexpected: ${JSON.stringify(line.telemetry)}`);
  assert(line.telemetry?.calibrationVersion === calibrationVersion,
    `overview calibration is ${line.telemetry?.calibrationVersion}`);

  const liveEvidence = await adapterGet(
    api,
    ticket,
    `/lines/${encodeURIComponent(lineId)}/live-evidence`
      + `?plantId=${encodeURIComponent(plantId)}&windowMinutes=15&limit=120`,
  );
  assert(liveEvidence?.line?.lineId === lineId, "live evidence line scope is unexpected");
  assert(Number(liveEvidence.line.telemetry?.value) === 12.5,
    `live evidence latest value is ${liveEvidence.line.telemetry?.value}`);
  assert(Array.isArray(liveEvidence.samples) && liveEvidence.samples.length >= mqttCount,
    `live evidence sample count is ${liveEvidence.samples?.length}`);
  assert(liveEvidence.samples.some((sample) =>
    Number(sample.numericValue) === 12.5
      && sample.qualityCode === "GOOD"
      && sample.calibrationVersion === calibrationVersion),
  "live evidence does not contain the controlled calibrated sample");
  assert(Array.isArray(liveEvidence.checks)
    && liveEvidence.checks.some((check) =>
      check.code === "TOPOLOGY_BOUND" && check.status === "PASS"),
  "live evidence does not prove the published topology binding");
  return { overview, line, liveEvidence };
}

async function assertCoverageUi(page, expectedEventCount) {
  const telemetry = page.locator("[data-telemetry-coverage]");
  await telemetry.getByRole("heading", { name: "现场遥测落表" }).waitFor();
  assert(await telemetry.getByText("1 / 1", { exact: true }).count() === 4,
    "telemetry coverage must show four independent 1 / 1 checks");
  await telemetry.getByText(`${expectedEventCount} / ${expectedEventCount}`, { exact: true }).waitFor();
  await telemetry.getByText("0 / 0", { exact: true }).waitFor();
  await telemetry.getByText(
    "固定点位已在本次窗口内形成可核验的 PostgreSQL 遥测证据。",
    { exact: true },
  ).waitFor();
  await telemetry.getByText("落表覆盖不等于现场验收完成", { exact: true }).waitFor();
}

async function assertDrawerSettled(page) {
  const drawer = page.locator("#detail-drawer");
  await drawer.waitFor({ state: "visible" });
  await page.waitForFunction(() => {
    const element = document.querySelector("#detail-drawer");
    if (!element) return false;
    const box = element.getBoundingClientRect();
    return box.left >= -1 && box.right <= window.innerWidth + 1;
  });
  return drawer.evaluate((element) => {
    const box = element.getBoundingClientRect();
    return { viewport: window.innerWidth, left: box.left, right: box.right, width: box.width };
  });
}

async function main() {
  for (const outputPath of [
    reportPath,
    mqttReportPath,
    desktopScreenshot,
    mobileScreenshot,
    liveOverviewScreenshot,
    liveDrawerScreenshot,
  ]) {
    fs.mkdirSync(path.dirname(outputPath), { recursive: true });
  }
  const api = await request.newContext({ ignoreHTTPSErrors: true });
  let browser;
  const report = {
    generatedAt: new Date().toISOString(),
    status: "FAIL",
    marker,
    scope: { tenantId: "1000", plantId, lineId },
    source: { productId, deviceId, propertyId, calibrationVersion },
    controlledInput: {
      mqttMarker,
      sourceEpoch: mqttEpoch,
      firstSequence: mqttStartSequence,
      lastSequence: mqttStartSequence + mqttCount - 1,
      count: mqttCount,
      quality: "GOOD",
      value: 12.5,
      qos: 1,
    },
    loginStatus: null,
    runId: null,
    api: {},
    mqtt: null,
    postgres: null,
    browser: {
      executable: executablePath || "playwright-managed",
      desktop: null,
      mobile: null,
      liveOperations: null,
      responses: [],
      consoleErrors: [],
      pageErrors: [],
      requestFailures: [],
    },
    boundaries: {
      controlledSimulator: true,
      fieldDeviceClaimed: false,
      modelTrainingStarted: false,
      modelRegistered: false,
      onlineInferenceEnabled: false,
      productionActivationAllowed: false,
    },
    error: null,
  };

  try {
    const auth = await login(api);
    report.loginStatus = auth.status;
    browser = await chromium.launch({
      headless,
      executablePath,
      args: ["--no-proxy-server"],
    });
    const desktopContext = await newContext(browser, auth, { width: 1440, height: 900 });
    const page = await desktopContext.newPage();
    page.setDefaultTimeout(timeoutMs);
    observe(page, report.browser);
    await page.goto(`${bpiBaseUrl}#/shadowRuns`, { waitUntil: "networkidle" });
    await page.getByRole("heading", { name: "影子运行验收" }).waitFor();
    await page.getByRole("button", { name: "新建影子运行" }).click();
    await page.getByRole("heading", { name: "新建影子运行" }).waitFor();
    await page.locator("#shadow-run-code").fill(marker);
    await page.locator("#shadow-run-name").fill(`${marker} 现场遥测落表验收`);
    await page.locator("#shadow-run-line").fill(lineId);
    const ruleSelect = page.locator("#shadow-run-rule");
    const ruleOptions = await ruleSelect.locator("option").evaluateAll((options) =>
      options.map((option) => ({ value: option.value, text: option.textContent || "" })));
    const markerRule = ruleOptions.find((option) => option.text.includes(`${marker}_RULE@`));
    assert(markerRule, `published marker rule is absent: ${JSON.stringify(ruleOptions)}`);
    await ruleSelect.selectOption(markerRule.value);
    await page.locator("#shadow-run-reason").fill(
      `${marker} prove MQTT Kafka PostgreSQL landing inside the started window`,
    );
    const createResponsePromise = page.waitForResponse((response) =>
      response.request().method() === "POST"
      && new URL(response.url()).pathname === "/bpi-api/shadow-runs");
    await page.getByRole("button", { name: "创建验收任务" }).click();
    const createResponse = await createResponsePromise;
    const createParsed = await readJson(createResponse);
    assert(createResponse.status() === 200,
      `create shadow run returned ${createResponse.status()}: ${createParsed.text}`);
    let run = createParsed.json?.data;
    assert(run?.state === "DRAFT" && run?.revision === 1,
      `created run is not DRAFT/r1: ${JSON.stringify(run)}`);
    assert(run.sourceCoverage?.fullyReady === true,
      `controlled source is not ready: ${JSON.stringify(run.sourceCoverage)}`);
    assert(run.telemetryCoverage?.windowStarted === false,
      "DRAFT run unexpectedly started its telemetry window");
    assert(run.telemetryCoverage?.acceptedEventCount === 0,
      "preheat telemetry leaked into the DRAFT run");
    assert(run.telemetryCoverage?.blockers?.includes("TELEMETRY_WINDOW_NOT_STARTED"),
      "DRAFT run does not expose TELEMETRY_WINDOW_NOT_STARTED");
    report.runId = run.id;
    report.api.create = {
      method: "POST",
      path: "/bpi-api/shadow-runs",
      status: createResponse.status(),
      state: run.state,
      revision: run.revision,
      sourceCoverage: run.sourceCoverage,
      telemetryCoverage: run.telemetryCoverage,
    };

    await page.getByRole("button", { name: "启动影子运行" }).click();
    await page.locator("#confirm-reason").fill(
      `${marker} source readiness checked; start a fresh PostgreSQL acceptance window`,
    );
    const startResponsePromise = page.waitForResponse((response) =>
      response.request().method() === "POST"
      && response.url().includes(`/bpi-api/shadow-runs/${run.id}/start`));
    await page.getByRole("button", { name: "确认启动" }).click();
    const startResponse = await startResponsePromise;
    assert(startResponse.status() === 200,
      `start shadow run returned ${startResponse.status()}`);
    await page.getByText("影子运行已启动").waitFor();
    await page.locator(".batch-state-band").getByText("RUNNING", { exact: true }).waitFor();
    run = await adapterGet(api, auth.ticket, `/shadow-runs/${run.id}`);
    assert(run.state === "RUNNING" && run.revision === 2,
      `started run is ${run.state}/r${run.revision}`);
    assert(run.telemetryCoverage.windowStarted === true,
      "RUNNING run did not open its telemetry window");
    assert(run.telemetryCoverage.acceptedEventCount === 0,
      "preheat telemetry leaked into the started window");
    report.api.start = {
      method: "POST",
      path: `/bpi-api/shadow-runs/${run.id}/start`,
      status: startResponse.status(),
      state: run.state,
      revision: run.revision,
      telemetryCoverage: run.telemetryCoverage,
    };

    report.mqtt = publishWindowTelemetry();
    run = await waitForCoverage(api, auth.ticket, run.id);
    const coverage = run.telemetryCoverage;
    assert(coverage.pinnedPointCount === 1 && coverage.observedPointCount === 1,
      `observed point coverage is incomplete: ${JSON.stringify(coverage)}`);
    assert(coverage.authoritativeSequencePointCount === 1,
      "authoritative source sequence coverage is incomplete");
    assert(coverage.calibratedPointCount === 1, "calibration coverage is incomplete");
    assert(coverage.goodQualityPointCount === 1, "GOOD quality coverage is incomplete");
    assert(coverage.acceptedEventCount === mqttCount
      && coverage.acceptedObservationCount === mqttCount,
    `accepted telemetry counts are unexpected: ${JSON.stringify(coverage)}`);
    assert(coverage.rejectedObservationCount === 0
      && coverage.gapEventCount === 0
      && coverage.outOfOrderEventCount === 0,
    `telemetry quality blockers are present: ${JSON.stringify(coverage)}`);
    assert(coverage.fullyCovered === true && coverage.blockers.length === 0,
      `telemetry coverage is blocked: ${JSON.stringify(coverage)}`);
    report.api.coverage = {
      method: "GET",
      path: `/bpi-api/shadow-runs/${run.id}`,
      status: 200,
      telemetryCoverage: coverage,
      readyForApproval: run.readyForApproval,
      blockers: run.blockers,
    };
    report.postgres = postgresEvidence(run.id);
    const liveProjection = await liveProjectionEvidence(api, auth.ticket);
    report.api.overview = {
      method: "GET",
      path: `/bpi-api/overview?plantId=${plantId}&onlyAbnormal=false`,
      status: 200,
      line: liveProjection.line,
    };
    report.api.liveEvidence = {
      method: "GET",
      path: `/bpi-api/lines/${lineId}/live-evidence?plantId=${plantId}&windowMinutes=15&limit=120`,
      status: 200,
      sampleCount: liveProjection.liveEvidence.samples.length,
      checks: liveProjection.liveEvidence.checks,
      incidentCount: liveProjection.liveEvidence.incidents.length,
    };

    await page.locator("#detail-drawer [data-close-drawer]").first().click();
    await page.getByRole("button", { name: "刷新" }).click();
    await page.locator(`[data-shadow-run-id="${run.id}"]`).click();
    await assertCoverageUi(page, mqttCount);
    const desktopDrawer = await assertDrawerSettled(page);
    await page.screenshot({ path: desktopScreenshot, fullPage: true });
    report.browser.desktop = {
      viewport: "1440x900",
      drawer: desktopDrawer,
      screenshot: desktopScreenshot,
    };
    await page.locator("#detail-drawer [data-close-drawer]").first().click();
    await page.goto(`${bpiBaseUrl}#/overview`, { waitUntil: "networkidle" });
    await page.getByRole("heading", { name: "实时生产态势" }).waitFor();
    const liveRow = page.locator(`[data-line-id="${lineId}"]`);
    await liveRow.waitFor();
    const liveRowText = await liveRow.textContent();
    assert(liveRowText?.includes("12.5"),
      `overview row does not show the controlled value: ${liveRowText}`);
    assert(liveRowText?.includes(liveProjection.line.telemetry.unit),
      `overview row does not show the controlled unit: ${liveRowText}`);
    await page.screenshot({ path: liveOverviewScreenshot, fullPage: true });
    await liveRow.click();
    const liveDrawer = page.locator("#detail-drawer");
    await liveDrawer.getByRole("heading", { name: "点位事实" }).waitFor();
    await liveDrawer.getByText(
      `${productId}/${deviceId}/${propertyId}`,
      { exact: true },
    ).waitFor();
    await liveDrawer.getByText("最近 15 分钟真实遥测", { exact: true }).waitFor();
    await liveDrawer.getByText("服务端运行判据", { exact: true }).waitFor();
    assert(await liveDrawer.locator("[data-trend-sample]").count() >= mqttCount,
      "live evidence drawer does not render the controlled samples");
    const liveDrawerGeometry = await assertDrawerSettled(page);
    await liveDrawer.screenshot({ path: liveDrawerScreenshot });
    report.browser.liveOperations = {
      route: "#/overview",
      rowText: liveRowText,
      drawer: liveDrawerGeometry,
      overviewScreenshot: liveOverviewScreenshot,
      drawerScreenshot: liveDrawerScreenshot,
    };
    await liveDrawer.locator("[data-close-drawer]").first().click();
    await page.goto(`${bpiBaseUrl}#/shadowRuns`, { waitUntil: "networkidle" });
    await page.locator(`[data-shadow-run-id="${run.id}"]`).click();

    const mobileContext = await newContext(browser, auth, { width: 390, height: 844 });
    const mobilePage = await mobileContext.newPage();
    mobilePage.setDefaultTimeout(timeoutMs);
    observe(mobilePage, report.browser);
    await mobilePage.goto(`${bpiBaseUrl}#/shadowRuns`, { waitUntil: "networkidle" });
    await mobilePage.locator(`[data-shadow-run-id="${run.id}"]`).click();
    await assertCoverageUi(mobilePage, mqttCount);
    const mobileTelemetry = mobilePage.locator("[data-telemetry-coverage]");
    await mobileTelemetry.scrollIntoViewIfNeeded();
    await mobilePage.waitForTimeout(200);
    const mobileTelemetryGeometry = await mobileTelemetry.evaluate((element) => {
      const box = element.getBoundingClientRect();
      return {
        viewportHeight: window.innerHeight,
        top: box.top,
        bottom: box.bottom,
        visible: box.top < window.innerHeight && box.bottom > 0,
      };
    });
    assert(mobileTelemetryGeometry.visible,
      `mobile telemetry coverage is outside the viewport: ${JSON.stringify(mobileTelemetryGeometry)}`);
    const mobileDrawer = await assertDrawerSettled(mobilePage);
    const mobileGeometry = await mobilePage.evaluate(() => ({
      viewport: document.documentElement.clientWidth,
      body: document.body.scrollWidth,
      document: document.documentElement.scrollWidth,
    }));
    assert(mobileGeometry.body <= mobileGeometry.viewport
      && mobileGeometry.document <= mobileGeometry.viewport,
    `mobile page overflow: ${JSON.stringify(mobileGeometry)}`);
    await mobilePage.screenshot({ path: mobileScreenshot, fullPage: true });
    report.browser.mobile = {
      ...mobileGeometry,
      drawer: mobileDrawer,
      telemetry: mobileTelemetryGeometry,
      screenshot: mobileScreenshot,
    };
    await mobileContext.close();

    await page.getByRole("button", { name: "取消任务" }).click();
    await page.locator("#confirm-reason").fill(
      `${marker} controlled landing evidence captured; no production activation`,
    );
    const cancelResponsePromise = page.waitForResponse((response) =>
      response.request().method() === "POST"
      && response.url().includes(`/bpi-api/shadow-runs/${run.id}/cancel`));
    await page.getByRole("button", { name: "确认取消" }).click();
    const cancelResponse = await cancelResponsePromise;
    assert(cancelResponse.status() === 200,
      `cancel shadow run returned ${cancelResponse.status()}`);
    await page.getByText("影子运行已取消").waitFor();
    const cancelled = await adapterGet(api, auth.ticket, `/shadow-runs/${run.id}`);
    assert(cancelled.state === "CANCELLED" && cancelled.revision === 3,
      `cancelled run is ${cancelled.state}/r${cancelled.revision}`);
    report.api.cancel = {
      method: "POST",
      path: `/bpi-api/shadow-runs/${run.id}/cancel`,
      status: cancelResponse.status(),
      state: cancelled.state,
      revision: cancelled.revision,
    };
    await desktopContext.close();

    assert(report.browser.consoleErrors.length === 0,
      `browser console errors: ${report.browser.consoleErrors.join(" | ")}`);
    assert(report.browser.pageErrors.length === 0,
      `browser page errors: ${report.browser.pageErrors.join(" | ")}`);
    assert(report.browser.requestFailures.length === 0,
      `browser request failures: ${JSON.stringify(report.browser.requestFailures)}`);
    const unexpectedResponses = report.browser.responses.filter((item) =>
      item.status < 200 || item.status >= 300);
    assert(unexpectedResponses.length === 0,
      `real page emitted non-2xx BPI responses: ${JSON.stringify(unexpectedResponses)}`);
    report.status = "PASS_CONTROLLED_MQTT_KAFKA_POSTGRES_BROWSER";
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
  .then(() => console.log(`BPI telemetry landing target acceptance PASS: ${reportPath}`))
  .catch((error) => {
    console.error(`BPI telemetry landing target acceptance FAIL: ${error.message}`);
    process.exitCode = 1;
  });
