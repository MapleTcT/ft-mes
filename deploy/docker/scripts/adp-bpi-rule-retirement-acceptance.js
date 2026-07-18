#!/usr/bin/env node
"use strict";

const crypto = require("node:crypto");
const fs = require("node:fs");
const path = require("node:path");
const { chromium, request } = require("playwright");

const marker = required("BPI_ACCEPTANCE_MARKER");
const adpBaseUrl = required("ADP_BASE_URL").replace(/\/+$/, "");
const bpiBaseUrl = required("BPI_BROWSER_BASE_URL").replace(/\/+$/, "");
const serviceBaseUrl = required("BPI_SERVICE_BASE_URL").replace(/\/+$/, "");
const browserOrigin = new URL(bpiBaseUrl).origin;
const username = required("ADP_USERNAME");
const password = required("ADP_PASSWORD");
const internalSecret = required("BPI_INTERNAL_JWT_SECRET");
const ruleCode = `${marker}_RULE`;
const ruleVersion = process.env.BPI_RETIREMENT_RULE_VERSION || "2.0.0";
const rollbackVersion = process.env.BPI_RETIREMENT_ROLLBACK_VERSION || "2.0.1";
const actorId = `${marker}_RETIRE_ADMIN`;
const reportPath = path.resolve(
  process.env.BPI_RETIREMENT_REPORT || `/tmp/${marker}-rule-retirement.json`,
);
const screenshotPrefix = path.resolve(
  process.env.BPI_RETIREMENT_SCREENSHOT_PREFIX || `/tmp/${marker}-rule-retirement`,
);
const timeoutMs = Number(process.env.BPI_RETIREMENT_TIMEOUT_MS || 180_000);

if (!/^[A-Za-z0-9_-]{8,80}$/.test(marker)) {
  throw new Error("BPI_ACCEPTANCE_MARKER must use 8-80 letters, digits, underscores or hyphens");
}
if (Buffer.byteLength(internalSecret, "utf8") < 32) {
  throw new Error("BPI_INTERNAL_JWT_SECRET must contain at least 32 UTF-8 bytes");
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
    if (ticket) return { ticket, status: response.status() };
    failures.push(response.status());
  }
  throw new Error(`ADP login failed with statuses ${failures.join(",")}`);
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
    sub: actorId,
    iat: now,
    exp: now + 600,
    tenant_id: "1000",
    roles: ["BPI_ADMIN"],
    plant_ids: ["PLANT-01"],
    line_ids: ["LINE-S07-01"],
  }));
  const signature = crypto.createHmac("sha256", internalSecret)
    .update(`${header}.${payload}`)
    .digest("base64url");
  return `${header}.${payload}.${signature}`;
}

function lifecycleSnapshot(rule) {
  return {
    id: rule.id,
    code: rule.code,
    version: rule.version,
    state: rule.state,
    revision: rule.revision,
    lifecycleAction: rule.lifecycleAction,
    lifecycleSequence: rule.lifecycleSequence,
    lifecycleActive: rule.lifecycleActive,
    publicationEventId: rule.publicationEventId,
    publicationStatus: rule.publicationStatus,
    publicationAttemptCount: rule.publicationAttemptCount,
    applicationEventId: rule.applicationEventId,
    applicationStatus: rule.applicationStatus,
    applicationDeploymentId: rule.applicationDeploymentId,
    runtimeReadinessEventId: rule.runtimeReadinessEventId,
    runtimeReadinessStatus: rule.runtimeReadinessStatus,
    runtimeReadinessDeploymentId: rule.runtimeReadinessDeploymentId,
    pointCatalogEventId: rule.runtimeReadinessPointCatalogEventId,
    pointCatalogSourceRevision: rule.runtimeReadinessPointCatalogSourceRevision,
  };
}

async function getRule(api, ruleId) {
  const response = await api.get(`${serviceBaseUrl}/bpi/v1/rules/${ruleId}`, {
    headers: { Authorization: `Bearer ${internalToken()}` },
    timeout: timeoutMs,
  });
  const parsed = await readJson(response);
  assert(response.status() === 200,
    `rule lookup returned ${response.status()}: ${parsed.text.slice(0, 500)}`);
  return parsed.json.data;
}

async function listRules(api) {
  const response = await api.get(`${serviceBaseUrl}/bpi/v1/rules?plantId=PLANT-01`, {
    headers: { Authorization: `Bearer ${internalToken()}` },
    timeout: timeoutMs,
  });
  const parsed = await readJson(response);
  assert(response.status() === 200,
    `rule list returned ${response.status()}: ${parsed.text.slice(0, 500)}`);
  return parsed.json.data;
}

async function pollRule(api, ruleId, predicate, description) {
  const deadline = Date.now() + timeoutMs;
  let latest;
  while (Date.now() < deadline) {
    latest = await getRule(api, ruleId);
    if (predicate(latest)) return latest;
    await new Promise((resolve) => setTimeout(resolve, 2_000));
  }
  throw new Error(`${description} did not converge: ${JSON.stringify(lifecycleSnapshot(latest || {}))}`);
}

async function openRule(page, code, version) {
  const close = page.locator("#detail-drawer [data-close-drawer]").first();
  if (await close.isVisible().catch(() => false)) await close.click();
  const row = page.locator("[data-rule-id]").filter({ hasText: code }).filter({ hasText: version });
  assert(await row.count() === 1, `expected exactly one rule row for ${code}@${version}`);
  const ruleId = await row.getAttribute("data-rule-id");
  await row.click();
  await page.getByRole("heading", { name: `${code}@${version}`, exact: true })
    .waitFor({ timeout: timeoutMs });
  return ruleId;
}

function requestEvidence(response, parsed) {
  let payload = null;
  try {
    payload = response.request().postDataJSON();
  } catch (_error) {
    payload = response.request().postData() || null;
  }
  return {
    method: response.request().method(),
    url: response.url().replace(browserOrigin, ""),
    payload,
    status: response.status(),
    response: parsed.json,
  };
}

async function run() {
  const report = {
    schemaVersion: 1,
    generatedAt: new Date().toISOString(),
    status: "RUNNING",
    marker,
    environment: {
      adpBaseUrl,
      bpiBaseUrl,
      serviceBaseUrl,
      database: "PostgreSQL",
      tenantId: "1000",
      plantId: "PLANT-01",
      lineId: "LINE-S07-01",
    },
    target: { ruleCode, ruleVersion, rollbackVersion },
    evidence: {},
    network: [],
    consoleErrors: [],
    pageErrors: [],
    requestFailures: [],
  };
  fs.mkdirSync(path.dirname(reportPath), { recursive: true });
  fs.mkdirSync(path.dirname(screenshotPrefix), { recursive: true });

  const api = await request.newContext({ ignoreHTTPSErrors: true });
  let browser;
  try {
    const auth = await login(api);
    report.evidence.loginStatus = auth.status;
    report.evidence.ticketKind = /^[0-9a-fA-F-]{36}$/.test(auth.ticket) ? "UUID" : "OPAQUE";

    const matching = (await listRules(api))
      .filter((rule) => rule.code === ruleCode && rule.version === ruleVersion);
    assert(matching.length === 1, `expected one target rule before retirement, found ${matching.length}`);
    const ruleId = matching[0].id;
    const active = await pollRule(
      api,
      ruleId,
      (rule) => rule.state === "PUBLISHED"
        && rule.lifecycleAction === "ACTIVATE"
        && rule.lifecycleSequence === 1
        && rule.lifecycleActive === true
        && rule.publicationStatus === "PUBLISHED"
        && rule.applicationStatus === "APPLIED"
        && rule.runtimeReadinessStatus === "READY",
      "active lifecycle evidence",
    );
    report.evidence.beforeRetirement = lifecycleSnapshot(active);

    browser = await chromium.launch({ headless: process.env.BPI_HEADLESS !== "false" });
    const context = await browser.newContext({ viewport: { width: 1600, height: 1000 } });
    await context.addCookies([{ name: "suposTicket", value: auth.ticket, url: bpiBaseUrl }]);
    await context.addInitScript((ticket) => {
      for (const key of ["ticket", "suposTicket", "SUPOS_TICKET", "token"]) {
        window.localStorage.setItem(key, ticket);
        window.sessionStorage.setItem(key, ticket);
      }
    }, auth.ticket);
    const page = await context.newPage();
    page.on("console", (message) => {
      if (message.type() === "error") report.consoleErrors.push(message.text());
    });
    page.on("pageerror", (error) => report.pageErrors.push(error.message));
    page.on("requestfailed", (failed) => report.requestFailures.push({
      method: failed.method(),
      url: failed.url().replace(browserOrigin, ""),
      error: failed.failure()?.errorText || "unknown",
    }));
    page.on("response", (response) => {
      if (!response.url().includes("/bpi-api/")) return;
      report.network.push({
        method: response.request().method(),
        url: response.url().replace(browserOrigin, ""),
        status: response.status(),
      });
    });

    await page.goto(`${bpiBaseUrl}/#/rules`, { waitUntil: "networkidle", timeout: timeoutMs });
    await page.getByRole("heading", { name: "规则与拓扑" }).waitFor({ timeout: timeoutMs });
    const pageRuleId = await openRule(page, ruleCode, ruleVersion);
    assert(pageRuleId === ruleId, "browser row and service API resolved different rule IDs");
    await page.getByText("Kafka 已确认", { exact: true }).last().waitFor({ timeout: timeoutMs });
    await page.getByText("控制面 APPLIED", { exact: true }).last().waitFor({ timeout: timeoutMs });
    await page.getByText("运行时 READY", { exact: true }).last().waitFor({ timeout: timeoutMs });
    await page.screenshot({ path: `${screenshotPrefix}-before.png`, fullPage: true });

    await page.getByRole("button", { name: "管理员退役" }).click();
    await page.getByRole("heading", { name: "退役边界规则" }).waitFor({ timeout: timeoutMs });
    await page.locator("#confirm-reason")
      .fill(`${marker} 真实 Kafka/Flink 生命周期验收后受控停用`);
    const retireResponsePromise = page.waitForResponse((response) =>
      response.request().method() === "POST"
      && response.url().includes(`/bpi-api/rules/${ruleId}/retire`));
    await page.getByRole("button", { name: "确认退役并停用" }).click();
    const retireResponse = await retireResponsePromise;
    const retireBody = await readJson(retireResponse);
    assert(retireResponse.status() === 200,
      `retirement returned ${retireResponse.status()}: ${retireBody.text.slice(0, 500)}`);
    report.evidence.retirementRequest = requestEvidence(retireResponse, retireBody);
    await page.getByText(`规则 ${ruleCode}@${ruleVersion} 已退役，等待 Kafka 与 Flink 确认 INACTIVE`)
      .waitFor({ timeout: timeoutMs });
    await page.locator(".batch-state-band").getByText("RETIRED", { exact: true })
      .waitFor({ timeout: timeoutMs });
    await page.locator(".lifecycle-summary").getByText("RETIRE", { exact: true })
      .waitFor({ timeout: timeoutMs });
    await page.locator(".lifecycle-summary").getByText("#2", { exact: true })
      .waitFor({ timeout: timeoutMs });

    const inactive = await pollRule(
      api,
      ruleId,
      (rule) => rule.state === "RETIRED"
        && rule.lifecycleAction === "RETIRE"
        && rule.lifecycleSequence === 2
        && rule.lifecycleActive === false
        && rule.publicationStatus === "PUBLISHED"
        && rule.applicationStatus === "APPLIED"
        && rule.runtimeReadinessStatus === "INACTIVE",
      "retirement lifecycle evidence",
    );
    report.evidence.afterRetirement = lifecycleSnapshot(inactive);

    await page.reload({ waitUntil: "networkidle", timeout: timeoutMs });
    await openRule(page, ruleCode, ruleVersion);
    await page.getByText("Kafka 已确认", { exact: true }).last().waitFor({ timeout: timeoutMs });
    await page.getByText("控制面 APPLIED", { exact: true }).last().waitFor({ timeout: timeoutMs });
    await page.getByText("运行时 INACTIVE", { exact: true }).last().waitFor({ timeout: timeoutMs });
    await page.getByText("该精确规则版本已从评估器停用，不会参与新的边界计算。")
      .waitFor({ timeout: timeoutMs });
    await page.screenshot({ path: `${screenshotPrefix}-inactive.png`, fullPage: true });

    await page.getByRole("button", { name: "创建回滚草稿" }).click();
    await page.getByRole("heading", { name: "创建回滚规则草稿" }).waitFor({ timeout: timeoutMs });
    assert(await page.locator("#rule-base").inputValue() === ruleId,
      "rollback editor did not preserve the retired rule as baseVersionId");
    assert(await page.locator("#rule-code").inputValue() === ruleCode,
      "rollback editor changed the rule code");
    await page.locator("#rule-version").fill(rollbackVersion);
    await page.locator("#rule-reason")
      .fill(`${marker} 从已确认 INACTIVE 的退役版本创建受控回滚草稿`);
    const rollbackResponsePromise = page.waitForResponse((response) =>
      response.request().method() === "POST" && response.url().includes("/bpi-api/rules/drafts"));
    await page.getByRole("button", { name: "创建草稿" }).click();
    const rollbackResponse = await rollbackResponsePromise;
    const rollbackBody = await readJson(rollbackResponse);
    assert([200, 201].includes(rollbackResponse.status()),
      `rollback draft returned ${rollbackResponse.status()}: ${rollbackBody.text.slice(0, 500)}`);
    report.evidence.rollbackRequest = requestEvidence(rollbackResponse, rollbackBody);
    await page.getByText(`规则草稿 ${ruleCode}@${rollbackVersion} 已创建`)
      .waitFor({ timeout: timeoutMs });
    await page.getByRole("heading", { name: `${ruleCode}@${rollbackVersion}`, exact: true })
      .waitFor({ timeout: timeoutMs });
    await page.locator(".batch-state-band").getByText("DRAFT", { exact: true })
      .waitFor({ timeout: timeoutMs });

    const rulesAfterRollback = await listRules(api);
    const rollbackRules = rulesAfterRollback
      .filter((rule) => rule.code === ruleCode && rule.version === rollbackVersion);
    assert(rollbackRules.length === 1, `expected one rollback draft, found ${rollbackRules.length}`);
    const rollback = rollbackRules[0];
    assert(rollback.id !== ruleId, "rollback draft reused the retired rule UUID");
    assert(rollback.state === "DRAFT", "rollback version is not DRAFT");
    const originalAfterRollback = await getRule(api, ruleId);
    assert(originalAfterRollback.state === "RETIRED", "rollback draft reactivated the retired UUID");
    report.evidence.rollbackDraft = lifecycleSnapshot(rollback);
    report.evidence.retiredRuleAfterRollback = lifecycleSnapshot(originalAfterRollback);
    await page.screenshot({ path: `${screenshotPrefix}-rollback-draft.png`, fullPage: true });

    assert(report.consoleErrors.length === 0,
      `unexpected console errors: ${report.consoleErrors.join(" | ")}`);
    assert(report.pageErrors.length === 0, `unexpected page errors: ${report.pageErrors.join(" | ")}`);
    assert(report.requestFailures.length === 0,
      `unexpected request failures: ${JSON.stringify(report.requestFailures)}`);
    report.status = "PASS";
    report.summary = {
      realBrowserRetirement: "PASS",
      kafkaPublication: "PASS",
      flinkApplication: "PASS",
      runtimeInactive: "PASS",
      rollbackDraft: "PASS",
      retiredUuidRemainedInactive: "PASS",
      consoleErrors: 0,
      pageErrors: 0,
      requestFailures: 0,
    };
    await context.close();
  } catch (error) {
    report.status = "FAIL";
    report.error = error instanceof Error ? error.message : String(error);
    throw error;
  } finally {
    report.generatedAt = new Date().toISOString();
    fs.writeFileSync(reportPath, `${JSON.stringify(report, null, 2)}\n`);
    if (browser) await browser.close();
    await api.dispose();
  }
}

run().catch((error) => {
  console.error(error.stack || error.message || String(error));
  process.exitCode = 1;
});
