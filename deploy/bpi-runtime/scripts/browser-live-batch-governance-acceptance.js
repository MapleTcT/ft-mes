#!/usr/bin/env node
"use strict";

const crypto = require("node:crypto");
const fs = require("node:fs");
const path = require("node:path");
const { chromium, request } = require("playwright");

const action = required("BPI_BROWSER_ACTION").toLowerCase();
const marker = required("BPI_ACCEPTANCE_MARKER");
const adpBaseUrl = required("ADP_BASE_URL").replace(/\/+$/, "");
const bpiBaseUrl = required("BPI_BROWSER_BASE_URL").replace(/\/+$/, "");
const serviceBaseUrl = required("BPI_SERVICE_BASE_URL").replace(/\/+$/, "");
const username = required("ADP_USERNAME");
const password = required("ADP_PASSWORD");
const internalSecret = required("BPI_INTERNAL_JWT_SECRET");
const tenantId = process.env.BPI_TENANT_ID || "1000";
const plantId = process.env.BPI_PLANT_ID || "PLANT-01";
const lineId = process.env.BPI_LINE_ID || "LINE-S07-01";
const productId = process.env.BPI_PRODUCT_ID || "bpi-mqtt-pilot-product-01";
const deviceId = process.env.BPI_DEVICE_ID || "bpi-mqtt-pilot-device-01";
const propertyId = process.env.BPI_PROPERTY_ID || "flow.instant";
const signal = process.env.BPI_SIGNAL || propertyId;
const unit = process.env.BPI_UNIT || "t/h";
const calibrationVersion = required("BPI_CALIBRATION_VERSION");
const boundaryType = (process.env.BPI_BOUNDARY_TYPE || "START").trim().toUpperCase();
const topologyCode = process.env.BPI_TOPOLOGY_CODE || `${marker}_TOPOLOGY`;
const topologyVersion = process.env.BPI_TOPOLOGY_VERSION || "1.0.0";
const topologyRef = `${topologyCode}@${topologyVersion}`;
const ruleCode = process.env.BPI_RULE_CODE || `${marker}_${boundaryType}`;
const ruleVersion = process.env.BPI_RULE_VERSION || "1.0.0";
const goldenSetId = process.env.BPI_GOLDEN_SET_ID || `${marker}_GOLDEN_${boundaryType}`;
const simulationFrom = process.env.BPI_SIMULATION_FROM || "";
const simulationTo = process.env.BPI_SIMULATION_TO || "";
const timeoutMs = Number(process.env.BPI_BROWSER_TIMEOUT_MS || 180_000);
const headless = process.env.BPI_HEADLESS !== "false";
const outputPath = path.resolve(
  process.env.BPI_BROWSER_REPORT || `/tmp/${marker}-live-batch-${action}.json`,
);
const screenshotPath = path.resolve(
  process.env.BPI_BROWSER_SCREENSHOT || `/tmp/${marker}-live-batch-${action}.png`,
);
const independentActor = `${marker}_INDEPENDENT_ADMIN`;

const actions = new Set([
  "feature-enable",
  "feature-inherit",
  "topology-publish",
  "rule-publish",
  "rules-retire",
]);

if (!actions.has(action)) {
  throw new Error(`BPI_BROWSER_ACTION must be one of: ${Array.from(actions).join(", ")}`);
}
if (!/^[A-Za-z0-9_-]{8,96}$/.test(marker)) {
  throw new Error("BPI_ACCEPTANCE_MARKER must use 8-96 letters, digits, underscores or hyphens");
}
if (!new Set(["START", "END"]).has(boundaryType)) {
  throw new Error("BPI_BOUNDARY_TYPE must be START or END");
}
if (Buffer.byteLength(internalSecret, "utf8") < 32) {
  throw new Error("BPI_INTERNAL_JWT_SECRET must contain at least 32 UTF-8 bytes");
}
if (!Number.isInteger(timeoutMs) || timeoutMs < 10_000) {
  throw new Error("BPI_BROWSER_TIMEOUT_MS must be an integer of at least 10000");
}
if (action === "rule-publish" && (!simulationFrom || !simulationTo)) {
  throw new Error("rule-publish requires BPI_SIMULATION_FROM and BPI_SIMULATION_TO");
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
    failures.push({ status: response.status(), body: parsed.text.slice(0, 300) });
  }
  throw new Error(`ADP login failed: ${JSON.stringify(failures)}`);
}

function base64Url(value) {
  return Buffer.from(value).toString("base64url");
}

function independentToken() {
  const now = Math.floor(Date.now() / 1000);
  const header = base64Url(JSON.stringify({ alg: "HS256", typ: "JWT" }));
  const payload = base64Url(JSON.stringify({
    iss: "ft-mes-adapter",
    aud: "bpi-service",
    sub: independentActor,
    iat: now,
    exp: now + 900,
    tenant_id: tenantId,
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
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) throw new Error(`invalid ISO date: ${iso}`);
  date.setMinutes(date.getMinutes() - date.getTimezoneOffset());
  return date.toISOString().slice(0, 16);
}

function topologyDefinition() {
  return {
    localityGroup: `${lineId}-TEST-ONLY`,
    nodes: [
      { code: "FEED-TANK", type: "TANK", name: "测试进料罐" },
      { code: "FLOW-METER", type: "METER", name: "测试瞬时流量计" },
      { code: "RECEIVE-TANK", type: "TANK", name: "测试接收罐" },
    ],
    edges: [
      { from: "FEED-TANK", to: "FLOW-METER" },
      { from: "FLOW-METER", to: "RECEIVE-TANK" },
    ],
    bindings: [{
      signal,
      productId,
      deviceId,
      propertyId,
      expectedUnit: unit,
      calibrationVersion,
    }],
    requiredSignals: [signal],
  };
}

function ruleAst() {
  const isStart = boundaryType === "START";
  return {
    boundaryType,
    quorumMinimum: 1,
    minimumConfidence: 0.8,
    maxCompositePenalty: 0.8,
    timing: {
      allowedLatenessSeconds: 0,
      watermarkDelaySeconds: 0,
      evaluationTimeoutSeconds: 300,
    },
    conditions: [{
      signal,
      operator: isStart ? "GREATER_THAN" : "LESS_THAN",
      threshold: isStart ? 10 : 1,
      holdSeconds: 2,
      maxSilenceSeconds: 60,
      classification: "QUORUM",
      weight: 100,
    }],
  };
}

async function directPost(api, route, revision, data, idempotencyKey, evidence) {
  const response = await api.post(`${serviceBaseUrl}${route}`, {
    data,
    headers: {
      Authorization: `Bearer ${independentToken()}`,
      "Content-Type": "application/json",
      "Idempotency-Key": idempotencyKey,
      "If-Match": String(revision),
    },
    timeout: timeoutMs,
  });
  const parsed = await readJson(response);
  const item = {
    actor: independentActor,
    method: "POST",
    url: `${serviceBaseUrl}${route}`,
    requestBody: data,
    status: response.status(),
    responseBody: parsed.json || parsed.text.slice(0, 2_000),
  };
  evidence.independentCalls.push(item);
  assert(response.status() === 200,
    `independent POST ${route} returned ${response.status()}: ${parsed.text.slice(0, 800)}`);
  return parsed.json;
}

async function featureFlag(page, evidence, mode) {
  await page.goto(`${bpiBaseUrl}/#/featureFlags`, { waitUntil: "networkidle", timeout: timeoutMs });
  await page.getByRole("heading", { name: "运行开关" }).waitFor({ timeout: timeoutMs });
  const row = page.locator('[data-feature-flag-row="bpi.commands"]');
  await row.waitFor({ timeout: timeoutMs });
  evidence.beforeText = (await row.innerText()).trim();
  const actionName = mode === "SET" ? "enable" : "inherit";
  const button = row.locator(`[data-feature-flag-action="${actionName}"]`);
  assert(await button.count() === 1, `bpi.commands ${actionName} action is missing`);
  assert(await button.isEnabled(), `bpi.commands ${actionName} action is not enabled`);
  await button.click();
  await page.locator("#feature-flag-reason").fill(
    mode === "SET"
      ? `${marker} TEST-ONLY 联合批次验收期间临时启用命令`
      : `${marker} 联合批次验收结束，恢复上级继承`,
  );
  const responsePromise = page.waitForResponse((response) =>
    response.request().method() === "POST"
      && response.url().includes("/bpi-api/feature-flags/bpi.commands"));
  await page.locator("#feature-flag-submit").click();
  const response = await responsePromise;
  const parsed = await readJson(response);
  assert(response.status() === 200,
    `feature flag change returned ${response.status()}: ${parsed.text.slice(0, 800)}`);
  const flag = parsed.json?.data;
  if (mode === "SET") {
    assert(flag?.effectiveEnabled === true && flag?.overrideActive === true && flag?.overrideEnabled === true,
      `bpi.commands was not explicitly enabled: ${parsed.text.slice(0, 800)}`);
  } else {
    assert(flag?.overrideActive === false,
      `bpi.commands did not return to inheritance: ${parsed.text.slice(0, 800)}`);
  }
  evidence.command = {
    mode,
    status: response.status(),
    effectiveEnabled: flag.effectiveEnabled,
    overrideActive: flag.overrideActive,
    overrideEnabled: flag.overrideEnabled,
    overrideRevision: flag.overrideRevision,
  };
  if (mode === "SET") {
    await row.getByText("已启用", { exact: true }).waitFor({ timeout: timeoutMs });
    await row.getByText("显式启用", { exact: true }).waitFor({ timeout: timeoutMs });
  } else {
    await row.getByText("继承上级", { exact: true }).waitFor({ timeout: timeoutMs });
  }
  evidence.afterText = (await row.innerText()).trim();
  await page.screenshot({ path: screenshotPath, fullPage: true });
}

async function publishTopology(page, api, evidence) {
  await page.goto(`${bpiBaseUrl}/#/rules`, { waitUntil: "networkidle", timeout: timeoutMs });
  await page.getByRole("heading", { name: "规则与拓扑" }).waitFor({ timeout: timeoutMs });
  const existingRow = page.locator("[data-topology-id]")
    .filter({ hasText: topologyCode })
    .filter({ hasText: topologyVersion });
  const existingCount = await existingRow.count();
  assert(existingCount <= 1,
    `topology ${topologyRef} is not uniquely visible before publication`);
  if (existingCount === 1) {
    const existingId = await existingRow.getAttribute("data-topology-id");
    await existingRow.click();
    const existingDrawer = page.locator("#detail-drawer");
    await existingDrawer.getByText("PUBLISHED", { exact: true }).waitFor({ timeout: timeoutMs });
    await existingDrawer.getByText("PASSED", { exact: true }).waitFor({ timeout: timeoutMs });
    const existingText = await existingDrawer.innerText();
    assert(existingText.includes("PUBLISHED") && existingText.includes("PASSED"),
      `existing topology ${topologyRef} is not PUBLISHED/PASSED`);
    evidence.topology = {
      id: existingId,
      code: topologyCode,
      version: topologyVersion,
      state: "PUBLISHED",
      resumedVerification: true,
    };
    evidence.drawerText = existingText.slice(0, 4_000);
    await page.screenshot({ path: screenshotPath, fullPage: true });
    return;
  }
  await page.getByRole("button", { name: "新建拓扑" }).click();
  await page.locator("#topology-code").fill(topologyCode);
  await page.locator("#topology-version").fill(topologyVersion);
  await page.locator("#topology-line").fill(lineId);
  await page.locator("#topology-definition").fill(JSON.stringify(topologyDefinition(), null, 2));
  await page.locator("#topology-reason").fill(`${marker} TEST-ONLY 联合批次验收拓扑草稿`);
  const createPromise = page.waitForResponse((response) =>
    response.request().method() === "POST" && /\/bpi-api\/topologies\/drafts$/.test(response.url()));
  await page.locator("#topology-editor-submit").click();
  const createdResponse = await createPromise;
  const created = await readJson(createdResponse);
  assert(createdResponse.status() === 200,
    `topology draft returned ${createdResponse.status()}: ${created.text.slice(0, 800)}`);
  const topology = created.json.data;
  evidence.topology = { id: topology.id, state: topology.state, revision: topology.revision };

  await page.locator("#open-topology-validate").click();
  await page.locator("#confirm-reason").fill(`${marker} 核对真实点位、单位、校准和来源序列准入`);
  const validatePromise = page.waitForResponse((response) =>
    response.request().method() === "POST"
      && response.url().includes(`/bpi-api/topologies/${topology.id}/validate`));
  await page.locator("#confirm-submit").click();
  const validatedResponse = await validatePromise;
  const validated = await readJson(validatedResponse);
  assert(validatedResponse.status() === 200 && validated.json?.data?.validationStatus === "PASSED",
    `topology validation failed: ${validated.text.slice(0, 1_200)}`);
  evidence.validation = {
    status: validatedResponse.status(),
    validationStatus: validated.json.data.validationStatus,
    revision: validated.json.data.revision,
    pointCatalogSnapshotId: validated.json.data.validatedPointCatalogSnapshotId,
  };

  await page.locator("#open-topology-publish").click();
  await page.locator("#confirm-reason").fill(`${marker} 创建人尝试发布以验证职责分离`);
  const creatorPublishPromise = page.waitForResponse((response) =>
    response.request().method() === "POST"
      && response.url().includes(`/bpi-api/topologies/${topology.id}/publish`));
  await page.locator("#confirm-submit").click();
  const creatorPublishResponse = await creatorPublishPromise;
  const creatorPublish = await readJson(creatorPublishResponse);
  assert(creatorPublishResponse.status() === 422
      && /other than the creator/.test(creatorPublish.json?.detail || ""),
  `creator topology publication was not rejected: ${creatorPublish.text.slice(0, 800)}`);
  evidence.creatorPublication = {
    status: creatorPublishResponse.status(),
    detail: creatorPublish.json.detail,
  };
  await page.getByRole("button", { name: "取消", exact: true }).click();

  const published = await directPost(
    api,
    `/bpi/v1/topologies/${topology.id}/publish`,
    validated.json.data.revision,
    { reason: `${marker} 独立管理员发布 TEST-ONLY 联合验收拓扑` },
    `${marker}-independent-topology-publish`,
    evidence,
  );
  assert(published?.data?.state === "PUBLISHED", "independent topology publication did not publish");
  evidence.topology.state = published.data.state;
  evidence.topology.revision = published.data.revision;

  await page.reload({ waitUntil: "networkidle", timeout: timeoutMs });
  const row = page.locator("[data-topology-id]")
    .filter({ hasText: topologyCode })
    .filter({ hasText: topologyVersion });
  assert(await row.count() === 1, `published topology ${topologyRef} is not uniquely visible`);
  await row.click();
  const drawer = page.locator("#detail-drawer");
  await drawer.getByText("PUBLISHED", { exact: true }).waitFor({ timeout: timeoutMs });
  await drawer.getByText("PASSED", { exact: true }).waitFor({ timeout: timeoutMs });
  evidence.drawerText = (await drawer.innerText()).slice(0, 4_000);
  await page.screenshot({ path: screenshotPath, fullPage: true });
}

async function publishRule(page, api, evidence) {
  await page.goto(`${bpiBaseUrl}/#/rules`, { waitUntil: "networkidle", timeout: timeoutMs });
  await page.getByRole("heading", { name: "规则与拓扑" }).waitFor({ timeout: timeoutMs });
  const existingRow = page.locator("[data-rule-id]")
    .filter({ hasText: ruleCode })
    .filter({ hasText: ruleVersion });
  const existingCount = await existingRow.count();
  assert(existingCount <= 1, `rule ${ruleCode}@${ruleVersion} is not uniquely visible`);
  let rule;
  if (existingCount === 1) {
    const existingId = await existingRow.getAttribute("data-rule-id");
    const detailPromise = page.waitForResponse((response) =>
      response.request().method() === "GET"
        && response.url().includes(`/bpi-api/rules/${existingId}`));
    await existingRow.click();
    const detailResponse = await detailPromise;
    const detail = await readJson(detailResponse);
    assert(detailResponse.status() === 200 && detail.json?.data,
      `existing rule detail returned ${detailResponse.status()}: ${detail.text.slice(0, 1_000)}`);
    rule = detail.json.data;
    assert(rule.state === "DRAFT",
      `existing rule ${ruleCode}@${ruleVersion} cannot resume from ${rule.state}`);
    evidence.rule = {
      id: rule.id,
      code: rule.code,
      state: rule.state,
      revision: rule.revision,
      resumedDraft: true,
    };
  } else {
    await page.getByRole("button", { name: "新建规则" }).click();
    await page.locator("#rule-code").fill(ruleCode);
    await page.locator("#rule-version").fill(ruleVersion);
    await page.locator("#rule-topology").selectOption(topologyRef);
    await page.locator("#rule-line").fill(lineId);
    await page.locator("#rule-ast").fill(JSON.stringify(ruleAst(), null, 2));
    await page.locator("#rule-reason").fill(`${marker} TEST-ONLY ${boundaryType} 边界规则草稿`);
    const createPromise = page.waitForResponse((response) =>
      response.request().method() === "POST" && /\/bpi-api\/rules\/drafts$/.test(response.url()));
    await page.locator("#rule-editor-submit").click();
    const createdResponse = await createPromise;
    const created = await readJson(createdResponse);
    assert(createdResponse.status() === 200,
      `rule draft returned ${createdResponse.status()}: ${created.text.slice(0, 1_000)}`);
    rule = created.json.data;
    evidence.rule = { id: rule.id, code: rule.code, state: rule.state, revision: rule.revision };
  }

  await page.locator("#open-simulation").click();
  await page.locator("#simulation-from").fill(localDateTime(simulationFrom));
  await page.locator("#simulation-to").fill(localDateTime(simulationTo));
  await page.locator("#simulation-calibration").fill(calibrationVersion);
  await page.locator("#simulation-golden").fill(goldenSetId);
  const simulationPromise = page.waitForResponse((response) =>
    response.request().method() === "POST"
      && response.url().includes(`/bpi-api/rules/${rule.id}/simulate`));
  await page.locator("#simulation-submit").click();
  const simulationResponse = await simulationPromise;
  const simulation = await readJson(simulationResponse);
  assert(simulationResponse.status() === 202 && simulation.json?.data?.state === "PASSED",
    `rule simulation failed: ${simulation.text.slice(0, 1_500)}`);
  evidence.simulation = {
    id: simulation.json.data.id,
    checksum: simulation.json.data.checksum,
    state: simulation.json.data.state,
    metrics: simulation.json.data.metrics,
    emittedBoundaries: simulation.json.data.emittedBoundaries,
  };
  await page.getByText("历史回放通过，可提交发布", { exact: true }).waitFor({ timeout: timeoutMs });

  await page.locator("#open-rule-submit").click();
  await page.locator("#confirm-reason").fill(`${marker} 历史回放与 TEST-ONLY 金标准一致，提交独立审批`);
  const submitPromise = page.waitForResponse((response) =>
    response.request().method() === "POST"
      && response.url().includes(`/bpi-api/rules/${rule.id}/submit-approval`));
  await page.locator("#confirm-submit").click();
  const submittedResponse = await submitPromise;
  const submitted = await readJson(submittedResponse);
  assert(submittedResponse.status() === 200 && submitted.json?.data?.state === "PENDING_APPROVAL",
    `rule approval submission failed: ${submitted.text.slice(0, 1_200)}`);
  evidence.approvalSubmission = {
    status: submittedResponse.status(),
    state: submitted.json.data.state,
    revision: submitted.json.data.revision,
    submittedBy: submitted.json.data.approvalSubmittedBy,
  };

  const published = await directPost(
    api,
    `/bpi/v1/rules/${rule.id}/publish`,
    submitted.json.data.revision,
    {
      reason: `${marker} 独立管理员批准并发布 TEST-ONLY ${boundaryType} 规则`,
      simulationId: simulation.json.data.id,
      simulationChecksum: simulation.json.data.checksum,
    },
    `${marker}-independent-rule-publish-${boundaryType.toLowerCase()}`,
    evidence,
  );
  assert(published?.data?.state === "PUBLISHED", "independent rule publication did not publish");
  evidence.rule.state = published.data.state;
  evidence.rule.revision = published.data.revision;
  evidence.rule.publicationStatus = published.data.publicationStatus;

  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    await page.goto(`${bpiBaseUrl}/#/rules`, { waitUntil: "networkidle", timeout: timeoutMs });
    const row = page.locator("[data-rule-id]")
      .filter({ hasText: ruleCode })
      .filter({ hasText: ruleVersion });
    assert(await row.count() === 1,
      `published rule ${ruleCode}@${ruleVersion} is not uniquely visible`);
    await row.click();
    const drawer = page.locator("#detail-drawer");
    const text = await drawer.innerText();
    evidence.runtimeDrawerText = text.slice(0, 6_000);
    if (text.includes("PUBLISHED") && text.includes("APPLIED") && text.includes("运行时 READY")) {
      evidence.runtime = { publicationStatus: "PUBLISHED", applicationStatus: "APPLIED", readiness: "READY" };
      await page.screenshot({ path: screenshotPath, fullPage: true });
      return;
    }
    await page.waitForTimeout(2_000);
  }
  throw new Error(`rule ${ruleCode} did not reach PUBLISHED/APPLIED/READY before timeout`);
}

async function retireRules(page, evidence) {
  for (const code of [`${marker}_START`, `${marker}_END`]) {
    await page.goto(`${bpiBaseUrl}/#/rules`, { waitUntil: "networkidle", timeout: timeoutMs });
    await page.getByRole("heading", { name: "规则与拓扑" }).waitFor({ timeout: timeoutMs });
    const row = page.locator("[data-rule-id]")
      .filter({ hasText: code })
      .filter({ hasText: ruleVersion });
    const rowCount = await row.count();
    assert(rowCount <= 1, `rule ${code}@${ruleVersion} is not uniquely visible for retirement`);
    if (rowCount === 0) {
      evidence.retired.push({
        code,
        version: ruleVersion,
        state: "NOT_FOUND",
        runtimeReadinessStatus: "NOT_APPLICABLE",
      });
      continue;
    }
    const ruleId = await row.getAttribute("data-rule-id");
    await row.click();
    const retireButton = page.locator("#open-rule-retire");
    await retireButton.waitFor({ timeout: timeoutMs });
    await retireButton.click();
    await page.locator("#confirm-reason").fill(`${marker} TEST-ONLY 联合验收结束，退役规则`);
    const responsePromise = page.waitForResponse((response) =>
      response.request().method() === "POST"
        && response.url().includes(`/bpi-api/rules/${ruleId}/retire`));
    await page.locator("#confirm-submit").click();
    const response = await responsePromise;
    const parsed = await readJson(response);
    assert(response.status() === 200 && parsed.json?.data?.state === "RETIRED",
      `rule ${code} retirement failed: ${parsed.text.slice(0, 1_000)}`);

    const deadline = Date.now() + timeoutMs;
    let runtimeText = "";
    while (Date.now() < deadline) {
      await page.goto(`${bpiBaseUrl}/#/rules`, { waitUntil: "networkidle", timeout: timeoutMs });
      const refreshed = page.locator("[data-rule-id]")
        .filter({ hasText: code })
        .filter({ hasText: ruleVersion });
      await refreshed.click();
      runtimeText = await page.locator("#detail-drawer").innerText();
      if (runtimeText.includes("RETIRED") && runtimeText.includes("运行时 INACTIVE")) break;
      await page.waitForTimeout(2_000);
    }
    assert(runtimeText.includes("运行时 INACTIVE"), `rule ${code} did not become INACTIVE`);
    evidence.retired.push({
      code,
      id: ruleId,
      state: "RETIRED",
      runtimeReadinessStatus: "INACTIVE",
      responseStatus: response.status(),
    });
  }
  assert(evidence.retired.some((item) => item.state === "RETIRED"),
    `no published ${ruleVersion} rule was available for retirement`);
  await page.screenshot({ path: screenshotPath, fullPage: true });
}

async function main() {
  fs.mkdirSync(path.dirname(outputPath), { recursive: true });
  fs.mkdirSync(path.dirname(screenshotPath), { recursive: true });
  const api = await request.newContext({ ignoreHTTPSErrors: true });
  let browser;
  const expectedConsoleErrorIndexes = new Set();
  const report = {
    generatedAt: new Date().toISOString(),
    status: "FAIL",
    action,
    marker,
    scope: { tenantId, plantId, lineId },
    point: { productId, deviceId, propertyId, signal, unit, calibrationVersion },
    topology: { code: topologyCode, version: topologyVersion },
    rule: action === "rule-publish" ? { code: ruleCode, version: ruleVersion, boundaryType } : null,
    urls: { adpBaseUrl, bpiBaseUrl, serviceBaseUrl },
    loginStatus: null,
    page: {},
    requests: [],
    consoleErrors: [],
    expectedConsoleErrors: [],
    pageErrors: [],
    requestFailures: [],
    evidence: { independentCalls: [], retired: [] },
    screenshot: screenshotPath,
    error: null,
  };
  try {
    const auth = await login(api);
    report.loginStatus = auth.status;
    browser = await chromium.launch({ headless, args: ["--no-proxy-server"] });
    const context = await browser.newContext({
      ignoreHTTPSErrors: true,
      viewport: { width: 1600, height: 1000 },
      extraHTTPHeaders: { Authorization: `Bearer ${auth.ticket}` },
    });
    const origin = new URL(bpiBaseUrl).origin;
    await context.addCookies([
      { name: "suposTicket", value: auth.ticket, url: origin },
      { name: "SUPOS_TICKET", value: auth.ticket, url: origin },
    ]);
    await context.addInitScript(({ token, loginPayload, selectedPlant, selectedLine }) => {
      for (const key of ["suposTicket", "SUPOS_TICKET", "token", "ticket"]) {
        window.localStorage.setItem(key, token);
        window.sessionStorage.setItem(key, token);
      }
      if (loginPayload) window.localStorage.setItem("loginMsg", JSON.stringify(loginPayload));
      window.localStorage.setItem("language", "zh_CN");
      window.localStorage.setItem("langu_code", "zh_CN");
      window.localStorage.setItem("locale", "zh-cn");
      window.localStorage.setItem("bpi.plantId", selectedPlant);
      window.localStorage.setItem("bpi.lineId", selectedLine);
      window.localStorage.setItem("bpi.featureFlagLineId", selectedLine);
      window.localStorage.setItem("bpi.featureFlagScopeType", "LINE");
    }, { token: auth.ticket, loginPayload: auth.payload, selectedPlant: plantId, selectedLine: lineId });

    const page = await context.newPage();
    page.setDefaultTimeout(timeoutMs);
    page.on("console", (message) => {
      if (message.type() !== "error") return;
      const location = message.location();
      const entry = { text: message.text(), url: location.url || "" };
      report.consoleErrors.push(entry);
      const index = report.consoleErrors.length - 1;
      if (action === "topology-publish" && /status of 422/.test(entry.text)) {
        expectedConsoleErrorIndexes.add(index);
        report.expectedConsoleErrors.push(entry);
      }
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
        responseBody = (await response.text()).slice(0, 3_000);
      } catch (_error) {
        responseBody = "<unavailable>";
      }
      report.requests.push({
        method: requestValue.method(),
        url: response.url(),
        requestBody: (requestValue.postData() || "").slice(0, 3_000),
        status: response.status(),
        responseBody,
      });
    });

    if (action === "feature-enable") await featureFlag(page, report.evidence, "SET");
    else if (action === "feature-inherit") await featureFlag(page, report.evidence, "INHERIT");
    else if (action === "topology-publish") await publishTopology(page, api, report.evidence);
    else if (action === "rule-publish") await publishRule(page, api, report.evidence);
    else await retireRules(page, report.evidence);

    report.page = { url: page.url(), title: await page.title() };
    const unexpectedConsoleErrors = report.consoleErrors.filter(
      (_entry, index) => !expectedConsoleErrorIndexes.has(index),
    );
    assert(unexpectedConsoleErrors.length === 0,
      `browser emitted unexpected console errors: ${JSON.stringify(unexpectedConsoleErrors)}`);
    assert(report.pageErrors.length === 0,
      `browser emitted page errors: ${JSON.stringify(report.pageErrors)}`);
    assert(report.requestFailures.length === 0,
      `browser emitted request failures: ${JSON.stringify(report.requestFailures)}`);
    report.status = "PASS";
    await context.close();
  } catch (error) {
    report.error = error?.stack || error?.message || String(error);
    throw error;
  } finally {
    report.generatedAt = new Date().toISOString();
    fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`, "utf8");
    if (browser) await browser.close();
    await api.dispose();
  }
}

main().catch((error) => {
  console.error(error?.stack || error);
  process.exitCode = 1;
});
