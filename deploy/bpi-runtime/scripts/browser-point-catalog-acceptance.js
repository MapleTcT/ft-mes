#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { chromium, request } = require("playwright");

const marker = required("BPI_ACCEPTANCE_MARKER");
const action = process.env.BPI_BROWSER_ACTION || "write";
const adpBaseUrl = required("ADP_BASE_URL").replace(/\/+$/, "");
const bpiBaseUrl = required("BPI_BROWSER_BASE_URL").replace(/\/+$/, "");
const username = required("ADP_USERNAME");
const password = required("ADP_PASSWORD");
const outputPath = path.resolve(
  process.env.BPI_BROWSER_REPORT || `/tmp/bpi-point-catalog-${marker}.json`,
);
const screenshotPath = path.resolve(
  process.env.BPI_BROWSER_SCREENSHOT || `/tmp/bpi-point-catalog-${marker}.png`,
);
const headless = process.env.BPI_HEADLESS !== "false";
const timeoutMs = Number(process.env.BPI_BROWSER_TIMEOUT_MS || 120_000);
const plantId = process.env.BPI_PLANT_ID || "PLANT-01";
const lineId = process.env.BPI_LINE_ID || "LINE-S07-01";
const productId = process.env.BPI_JETLINKS_PRODUCT_ID || "bpi-pilot-product-01";
const deviceId = process.env.BPI_JETLINKS_DEVICE_ID || "bpi-pilot-device-01";
const sourcePropertyId = process.env.BPI_JETLINKS_SOURCE_PROPERTY_ID || "instantFlow";
const propertyId = process.env.BPI_CANONICAL_PROPERTY_ID || "flow.instant";
const sourceRevision = new Set(["sync-read", "sync-validate"]).has(action)
  ? required("BPI_EXPECTED_POINT_CATALOG_REVISION")
  : `${marker}_JETLINKS_STATUS`;
const topologyCode = `${marker}_BLOCKED_TOPOLOGY`;
const expectedPointIssues = [
  "DEVICE_NOT_REGISTERED",
  "DEVICE_NOT_ACTIVE",
  "PROPERTY_NOT_AVAILABLE",
  "CALIBRATION_NOT_VERIFIED",
  "SOURCE_SEQUENCE_DISABLED",
];
const pointIssueLabels = {
  DEVICE_NOT_REGISTERED: "设备未注册",
  DEVICE_NOT_ACTIVE: "设备未激活",
  PROPERTY_NOT_AVAILABLE: "设备属性不可用",
  UNIT_MISSING: "单位缺失",
  CALIBRATION_NOT_VERIFIED: "校准证据未批准或已失效",
  SOURCE_SEQUENCE_DISABLED: "来源序列未启用",
};
const expectedAutomaticPointIssues = process.env.BPI_EXPECTED_POINT_ISSUES
  ? process.env.BPI_EXPECTED_POINT_ISSUES.split(",").map((value) => value.trim()).filter(Boolean)
  : [...expectedPointIssues.slice(0, 3), "UNIT_MISSING", ...expectedPointIssues.slice(3)];
const expectedAutomaticPointLabels = expectedAutomaticPointIssues.map((issue) => {
  const label = pointIssueLabels[issue];
  if (!label) throw new Error(`BPI_EXPECTED_POINT_ISSUES contains unsupported issue: ${issue}`);
  return label;
});
const expectedPointLabels = [
  "设备未注册",
  "设备未激活",
  "设备属性不可用",
  "校准证据未批准或已失效",
  "来源序列未启用",
];
const expectedTopologyErrors = [
  "POINT_DEVICE_NOT_REGISTERED",
  "POINT_DEVICE_NOT_ACTIVE",
  "POINT_PROPERTY_NOT_AVAILABLE",
  "POINT_CALIBRATION_NOT_VERIFIED",
  "POINT_SOURCE_SEQUENCE_DISABLED",
];
const pointIssueTopologyErrors = {
  DEVICE_NOT_REGISTERED: "POINT_DEVICE_NOT_REGISTERED",
  DEVICE_NOT_ACTIVE: "POINT_DEVICE_NOT_ACTIVE",
  PROPERTY_NOT_AVAILABLE: "POINT_PROPERTY_NOT_AVAILABLE",
  UNIT_MISSING: "POINT_UNIT_MISSING",
  CALIBRATION_NOT_VERIFIED: "POINT_CALIBRATION_NOT_VERIFIED",
  SOURCE_SEQUENCE_DISABLED: "POINT_SOURCE_SEQUENCE_DISABLED",
};
const expectedAutomaticTopologyErrors = expectedAutomaticPointIssues.map((issue) => {
  const error = pointIssueTopologyErrors[issue];
  if (!error) throw new Error(`No topology validation error is mapped for point issue: ${issue}`);
  return error;
});

if (!/^[A-Za-z0-9_-]{8,80}$/.test(marker)) {
  throw new Error("BPI_ACCEPTANCE_MARKER must use 8-80 letters, digits, underscores or hyphens");
}
if (!new Set(["write", "read", "sync-read", "sync-validate"]).has(action)) {
  throw new Error("BPI_BROWSER_ACTION must be write, read, sync-read or sync-validate");
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
    failures.push({ status: response.status(), contentType: response.headers()["content-type"] || "" });
  }
  throw new Error(`ADP login failed: ${JSON.stringify(failures)}`);
}

function pointCommand() {
  return {
    localityGroup: "ft-mes-test/plant-test-01/line-test-01",
    productId,
    deviceId,
    propertyId,
    sourcePropertyId,
    pointName: "BPI影子试点流量计01 / 瞬时流量",
    unit: "m3/h",
    dataType: "double",
    deviceState: "INACTIVE",
    registered: false,
    propertyPresent: false,
    calibrationVersion: "pilot-unverified-20260714",
    calibrationStatus: "UNVERIFIED",
    sourceSequenceEnabled: false,
  };
}

function topologyDefinition() {
  return {
    localityGroup: "ft-mes-test/plant-test-01/line-test-01",
    nodes: [
      { code: "FEED-TANK", type: "TANK", name: "进料罐" },
      { code: "FLOW-METER", type: "METER", name: "瞬时流量计" },
      { code: "RECEIVE-TANK", type: "TANK", name: "接收罐" },
    ],
    edges: [
      { from: "FEED-TANK", to: "FLOW-METER" },
      { from: "FLOW-METER", to: "RECEIVE-TANK" },
    ],
    bindings: [
      {
        signal: "feed.flow",
        productId,
        deviceId,
        propertyId,
        expectedUnit: "m3/h",
        calibrationVersion: "pilot-unverified-20260714",
      },
    ],
    requiredSignals: ["feed.flow"],
  };
}

function assertExactMembers(actual, expected, label) {
  const normalizedActual = [...actual].sort();
  const normalizedExpected = [...expected].sort();
  if (JSON.stringify(normalizedActual) !== JSON.stringify(normalizedExpected)) {
    throw new Error(`${label} mismatch: ${JSON.stringify(normalizedActual)}`);
  }
}

async function importBlockedPointCatalog(page, evidence) {
  await page.goto(`${bpiBaseUrl}/#/points`, { waitUntil: "networkidle", timeout: timeoutMs });
  await page.getByRole("heading", { name: "点位目录" }).waitFor({ timeout: timeoutMs });
  await page.getByRole("button", { name: "导入快照" }).click();
  await page.getByRole("heading", { name: "导入点位目录快照" }).waitFor();
  await page.locator("#point-source").fill("JETLINKS");
  await page.locator("#point-source-instance").fill("ft-mes-iot-pilot");
  await page.locator("#point-source-revision").fill(sourceRevision);
  await page.locator("#point-import-line").fill(lineId);
  await page.locator("#point-import-json").fill(JSON.stringify([pointCommand()], null, 2));
  await page.locator("#point-import-reason").fill(`目标环境真实 JetLinks 状态快照 ${marker}`);

  const importResponsePromise = page.waitForResponse(
    (response) => response.url().includes("/bpi-api/point-catalog/snapshots")
      && response.request().method() === "POST",
  );
  await page.locator("#point-catalog-submit").click();
  const importResponse = await importResponsePromise;
  const importPayload = await importResponse.json();
  if (importResponse.status() !== 200) {
    throw new Error(`point catalog import returned ${importResponse.status()}`);
  }
  const imported = importPayload.data;
  if (imported.snapshot.pointCount !== 1 || imported.snapshot.readyPointCount !== 0) {
    throw new Error("target JetLinks point must remain 0/1 ready");
  }
  assertExactMembers(imported.points[0].readinessIssues, expectedPointIssues, "point readiness issues");
  evidence.snapshotId = imported.snapshot.id;
  evidence.snapshotChecksum = imported.snapshot.checksum;
  evidence.pointId = imported.points[0].id;
  evidence.pointCount = imported.snapshot.pointCount;
  evidence.readyPointCount = imported.snapshot.readyPointCount;
  evidence.readiness = "BLOCKED";
  evidence.pointIssues = imported.points[0].readinessIssues;
  evidence.sourcePropertyId = imported.points[0].sourcePropertyId;
  evidence.propertyId = imported.points[0].propertyId;
  evidence.importIdempotencyKey = await importResponse.request().headerValue("idempotency-key");
  if (!evidence.importIdempotencyKey) throw new Error("point catalog request did not carry Idempotency-Key");

  await page.getByText("点位快照已导入：0/1 就绪").waitFor();
  const pointRow = page.locator(`[data-point-id="${evidence.pointId}"]`);
  await pointRow.getByText(`${sourcePropertyId} → ${propertyId}`).waitFor();
  for (const label of expectedPointLabels) {
    await pointRow.getByText(new RegExp(label)).waitFor();
  }

  const replay = await page.evaluate(async ({ command, idempotencyKey }) => {
    const response = await fetch("/bpi-api/point-catalog/snapshots", {
      method: "POST",
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json",
        "Idempotency-Key": idempotencyKey,
        "If-Match": "0",
      },
      body: JSON.stringify(command),
    });
    return {
      status: response.status,
      replay: response.headers.get("Idempotent-Replay"),
      payload: await response.json(),
    };
  }, {
    command: {
      source: "JETLINKS",
      sourceInstance: "ft-mes-iot-pilot",
      sourceRevision,
      plantId,
      lineId,
      observedAt: imported.snapshot.observedAt,
      points: [pointCommand()],
      reason: `目标环境真实 JetLinks 状态快照 ${marker}`,
    },
    idempotencyKey: evidence.importIdempotencyKey,
  });
  if (replay.status !== 200 || replay.replay !== "true" || replay.payload.data.snapshot.id !== evidence.snapshotId) {
    throw new Error("point catalog idempotent replay did not return the original snapshot");
  }
  evidence.replayStatus = replay.status;
  evidence.idempotentReplay = replay.replay;
}

async function validateBlockedTopology(page, evidence, expectedErrors = expectedTopologyErrors) {
  await page.getByRole("button", { name: "规则与拓扑" }).click();
  await page.getByRole("heading", { name: "规则与拓扑" }).waitFor({ timeout: timeoutMs });
  await page.getByRole("button", { name: "新建拓扑" }).click();
  await page.getByRole("heading", { name: "新建拓扑版本" }).waitFor();
  await page.locator("#topology-code").fill(topologyCode);
  await page.locator("#topology-version").fill("1.0.0");
  await page.locator("#topology-line").fill(lineId);
  await page.locator("#topology-definition").fill(JSON.stringify(topologyDefinition(), null, 2));
  await page.locator("#topology-reason").fill(`验证未准入点位不能驱动批次规则 ${marker}`);
  const createResponsePromise = page.waitForResponse(
    (response) => response.url().includes("/bpi-api/topologies/drafts")
      && response.request().method() === "POST",
  );
  await page.getByRole("button", { name: "创建草稿" }).click();
  const createResponse = await createResponsePromise;
  const created = (await createResponse.json()).data;
  if (createResponse.status() !== 200) throw new Error(`topology creation returned ${createResponse.status()}`);
  evidence.topologyId = created.id;
  evidence.topologyCreatedRevision = created.revision;

  await page.getByRole("button", { name: "校验拓扑" }).click();
  await page.locator("#confirm-reason").fill("确认设备、属性、标定和来源序列未就绪时必须拒绝发布");
  const validateResponsePromise = page.waitForResponse(
    (response) => response.url().includes(`/bpi-api/topologies/${created.id}/validate`),
  );
  await page.getByRole("button", { name: "开始校验" }).click();
  const validateResponse = await validateResponsePromise;
  const validated = (await validateResponse.json()).data;
  if (validateResponse.status() !== 200 || validated.validationStatus !== "FAILED") {
    throw new Error(`blocked topology validation returned ${validateResponse.status()}/${validated.validationStatus}`);
  }
  const errorCodes = validated.validationErrors.map((issue) => issue.code);
  const warningCodes = validated.validationWarnings.map((issue) => issue.code);
  assertExactMembers(errorCodes, expectedErrors, "topology validation errors");
  assertExactMembers(warningCodes, [], "topology validation warnings");
  if (validated.validatedPointCatalogSnapshotId !== evidence.snapshotId) {
    throw new Error("topology validation did not pin the imported point catalog snapshot");
  }
  evidence.topologyValidationStatus = validated.validationStatus;
  evidence.topologyValidatedRevision = validated.revision;
  evidence.topologyErrors = errorCodes;
  evidence.topologyWarnings = warningCodes;
  evidence.validatedPointCatalogSnapshotId = validated.validatedPointCatalogSnapshotId;
  evidence.publishAllowed = false;
  await page.getByText(`拓扑校验失败：${expectedErrors.length} 项错误`).waitFor();
  for (const code of expectedErrors) {
    await page.getByText(code, { exact: true }).waitFor();
  }
  if (await page.getByRole("button", { name: "发布拓扑" }).count()) {
    throw new Error("failed topology unexpectedly exposes publication action");
  }
  await page.screenshot({ path: screenshotPath, fullPage: true });
}

async function readPersistedAcceptance(page, evidence) {
  await page.goto(`${bpiBaseUrl}/#/points`, { waitUntil: "networkidle", timeout: timeoutMs });
  await page.getByRole("heading", { name: "点位目录" }).waitFor({ timeout: timeoutMs });
  await page.getByText(sourceRevision, { exact: true }).waitFor();
  const pointRow = page.locator("[data-point-id]").filter({ hasText: deviceId });
  if (await pointRow.count() !== 1) throw new Error("persisted point is not uniquely visible after restart");
  await pointRow.getByText(`${sourcePropertyId} → ${propertyId}`).waitFor();
  for (const label of expectedPointLabels) {
    await pointRow.getByText(new RegExp(label)).waitFor();
  }
  evidence.readiness = "BLOCKED";
  evidence.pointVisibleAfterRestart = true;

  await page.getByRole("button", { name: "规则与拓扑" }).click();
  await page.getByRole("heading", { name: "规则与拓扑" }).waitFor({ timeout: timeoutMs });
  const topologyRow = page.locator("[data-topology-id]").filter({ hasText: topologyCode });
  if (await topologyRow.count() !== 1) throw new Error("persisted blocked topology is not uniquely visible after restart");
  await topologyRow.click();
  await page.getByRole("heading", { name: `${topologyCode}@1.0.0` }).waitFor();
  for (const code of expectedTopologyErrors) {
    await page.getByText(code, { exact: true }).waitFor();
  }
  if (await page.getByRole("button", { name: "发布拓扑" }).count()) {
    throw new Error("persisted failed topology unexpectedly exposes publication action");
  }
  evidence.topologyValidationStatus = "FAILED";
  evidence.topologyVisibleAfterRestart = true;
  evidence.publishAllowed = false;
  await page.screenshot({ path: screenshotPath, fullPage: true });
}

async function readAutomaticSnapshot(page, evidence) {
  await page.goto(`${bpiBaseUrl}/#/points`, { waitUntil: "networkidle", timeout: timeoutMs });
  await page.getByRole("heading", { name: "点位目录" }).waitFor({ timeout: timeoutMs });
  await page.getByText(sourceRevision, { exact: true }).waitFor();
  const current = await page.evaluate(async ({ currentPlantId, currentLineId }) => {
    const query = new URLSearchParams({ plantId: currentPlantId, lineId: currentLineId });
    const response = await fetch(`/bpi-api/point-catalog/current?${query}`);
    return { status: response.status, payload: await response.json() };
  }, { currentPlantId: plantId, currentLineId: lineId });
  if (current.status !== 200) {
    throw new Error(`current point catalog returned ${current.status}`);
  }
  const currentData = current.payload.data;
  if (!currentData || currentData.snapshot.sourceRevision !== sourceRevision) {
    throw new Error("current point catalog revision does not match the expected automatic snapshot");
  }
  const currentPoint = currentData.points.find((point) => point.deviceId === deviceId);
  if (!currentPoint) throw new Error("automatic point is missing from current point catalog API");
  assertExactMembers(
    currentPoint.readinessIssues,
    expectedAutomaticPointIssues,
    "automatic point readiness issues",
  );
  const pointRow = page.locator("[data-point-id]").filter({ hasText: deviceId });
  if (await pointRow.count() !== 1) {
    throw new Error("automatically synchronized point is not uniquely visible");
  }
  await pointRow.getByText(`${sourcePropertyId} → ${propertyId}`).waitFor();
  for (const label of expectedAutomaticPointLabels) {
    await pointRow.getByText(new RegExp(label)).waitFor();
  }
  evidence.readiness = "BLOCKED";
  evidence.automaticSnapshotVisible = true;
  evidence.sourceRevision = sourceRevision;
  evidence.snapshotId = currentData.snapshot.id;
  evidence.pointCount = currentData.snapshot.pointCount;
  evidence.readyPointCount = currentData.snapshot.readyPointCount;
  evidence.pointIssues = currentPoint.readinessIssues;
  await page.screenshot({ path: screenshotPath, fullPage: true });
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
    sourceRevision,
    topologyCode,
    scope: { plantId, lineId },
    source: { productId, deviceId, sourcePropertyId, propertyId },
    adpBaseUrl,
    bpiBaseUrl,
    loginStatus: null,
    page: {},
    requests: [],
    consoleErrors: [],
    pageErrors: [],
    requestFailures: [],
    evidence: {},
    screenshot: screenshotPath,
    error: null,
  };
  try {
    const auth = await login(api);
    report.loginStatus = auth.loginStatus;
    browser = await chromium.launch({ headless });
    const context = await browser.newContext({
      ignoreHTTPSErrors: true,
      viewport: { width: 1440, height: 900 },
      extraHTTPHeaders: { Authorization: `Bearer ${auth.ticket}` },
    });
    await context.addCookies([
      { name: "suposTicket", value: auth.ticket, url: bpiBaseUrl },
      { name: "SUPOS_TICKET", value: auth.ticket, url: bpiBaseUrl },
    ]);
    await context.addInitScript(({ token, loginPayload, pointLineId, pointPlantId }) => {
      for (const key of ["suposTicket", "SUPOS_TICKET", "token", "ticket"]) {
        window.localStorage.setItem(key, token);
        window.sessionStorage.setItem(key, token);
      }
      if (loginPayload) window.localStorage.setItem("loginMsg", JSON.stringify(loginPayload));
      window.localStorage.setItem("language", "zh_CN");
      window.localStorage.setItem("langu_code", "zh_CN");
      window.localStorage.setItem("locale", "zh-cn");
      window.localStorage.setItem("bpi.lineId", pointLineId);
      window.localStorage.setItem("bpi.plantId", pointPlantId);
    }, { token: auth.ticket, loginPayload: auth.loginPayload, pointLineId: lineId, pointPlantId: plantId });
    const page = await context.newPage();
    page.setDefaultTimeout(timeoutMs);
    page.on("console", (message) => {
      if (message.type() !== "error") return;
      const location = message.location();
      report.consoleErrors.push({ text: message.text(), url: location.url });
    });
    page.on("pageerror", (error) => report.pageErrors.push(error.message));
    page.on("requestfailed", (failed) => {
      report.requestFailures.push({
        method: failed.method(),
        url: failed.url(),
        error: failed.failure()?.errorText || "",
      });
    });
    page.on("response", async (response) => {
      if (!response.url().includes("/bpi-api/")) return;
      const requestValue = response.request();
      let responseBody = "";
      try {
        responseBody = (await response.text()).slice(0, 4_000);
      } catch (_error) {
        responseBody = "<unavailable>";
      }
      report.requests.push({
        method: requestValue.method(),
        url: response.url(),
        requestBody: (requestValue.postData() || "").slice(0, 4_000),
        status: response.status(),
        responseBody,
      });
    });

    if (action === "write") {
      await importBlockedPointCatalog(page, report.evidence);
      await validateBlockedTopology(page, report.evidence);
    } else if (action === "sync-read") {
      await readAutomaticSnapshot(page, report.evidence);
    } else if (action === "sync-validate") {
      await readAutomaticSnapshot(page, report.evidence);
      await validateBlockedTopology(page, report.evidence, expectedAutomaticTopologyErrors);
    } else {
      await readPersistedAcceptance(page, report.evidence);
    }
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
