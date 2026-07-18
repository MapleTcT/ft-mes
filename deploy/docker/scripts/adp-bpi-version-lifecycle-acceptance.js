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
const boundaryTime = required("BPI_ACCEPTANCE_BOUNDARY_TIME");
const goldenSetId = `${marker}_GOLDEN`;
const topologyCode = `${marker}_TOPOLOGY`;
const publishRuleCode = `${marker}_RULE`;
const rejectRuleCode = `${marker}_REJECT`;
const approver = `${marker}_APPROVER`;
const reportPath = path.resolve(process.env.BPI_BROWSER_REPORT || `/tmp/${marker}-lifecycle.json`);
const screenshotPrefix = path.resolve(process.env.BPI_BROWSER_SCREENSHOT_PREFIX || `/tmp/${marker}-lifecycle`);
const timeoutMs = Number(process.env.BPI_BROWSER_TIMEOUT_MS || 120_000);

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
    sub: approver,
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

function localDateTime(iso) {
  const value = new Date(iso);
  if (Number.isNaN(value.getTime())) throw new Error(`invalid acceptance time: ${iso}`);
  return new Date(value.getTime() - value.getTimezoneOffset() * 60_000)
    .toISOString()
    .slice(0, 19);
}

async function closeDrawer(page) {
  const button = page.locator("#detail-drawer [data-close-drawer]").first();
  if (await button.isVisible().catch(() => false)) await button.click();
}

async function openRule(page, code, version) {
  await closeDrawer(page);
  const row = page.locator("[data-rule-id]").filter({ hasText: code }).filter({ hasText: version });
  assert(await row.count() === 1, `expected exactly one rule row for ${code}@${version}`);
  const id = await row.getAttribute("data-rule-id");
  await row.click();
  await page.getByRole("heading", { name: `${code}@${version}`, exact: true }).waitFor({ timeout: timeoutMs });
  return id;
}

async function simulateAndSubmit(page, evidence, code, version, keySuffix) {
  const ruleId = await openRule(page, code, version);
  const boundary = new Date(boundaryTime);
  await page.getByRole("button", { name: "运行历史回放" }).click();
  await page.locator("#simulation-from").fill(localDateTime(new Date(boundary.getTime() - 1_000).toISOString()));
  await page.locator("#simulation-to").fill(localDateTime(new Date(boundary.getTime() + 1_000).toISOString()));
  await page.locator("#simulation-calibration").fill("CAL-1");
  await page.locator("#simulation-golden").fill(goldenSetId);
  const simulationResponsePromise = page.waitForResponse((response) =>
    response.request().method() === "POST" && response.url().includes(`/bpi-api/rules/${ruleId}/simulate`));
  await page.locator("#simulation-submit").click();
  const simulationResponse = await simulationResponsePromise;
  const simulationBody = await readJson(simulationResponse);
  assert(simulationResponse.status() === 202,
    `${code} simulation returned ${simulationResponse.status()}: ${simulationBody.text.slice(0, 500)}`);
  assert(simulationBody.json?.data?.state === "PASSED", `${code} simulation did not pass`);
  await page.getByText("历史回放通过，可提交发布", { exact: true }).waitFor({ timeout: timeoutMs });

  await page.getByRole("button", { name: "提交审批" }).click();
  await page.locator("#confirm-reason").fill(`${marker} 历史回放和现场复核通过，提交版本审批`);
  const submitResponsePromise = page.waitForResponse((response) =>
    response.request().method() === "POST" && response.url().includes(`/bpi-api/rules/${ruleId}/submit-approval`));
  await page.getByRole("button", { name: "确认提交审批" }).click();
  const submitResponse = await submitResponsePromise;
  const submitBody = await readJson(submitResponse);
  assert(submitResponse.status() === 200,
    `${code} approval submission returned ${submitResponse.status()}: ${submitBody.text.slice(0, 500)}`);
  assert(submitBody.json?.data?.state === "PENDING_APPROVAL", `${code} did not enter PENDING_APPROVAL`);
  assert(submitBody.json?.data?.approvalStatus === "PENDING", `${code} approval is not PENDING`);
  await page.locator(".batch-state-band").getByText("PENDING_APPROVAL", { exact: true }).waitFor({ timeout: timeoutMs });

  evidence[`${keySuffix}RuleId`] = ruleId;
  evidence[`${keySuffix}SimulationId`] = simulationBody.json.data.id;
  evidence[`${keySuffix}SimulationChecksum`] = simulationBody.json.data.checksum;
  evidence[`${keySuffix}PendingRevision`] = submitBody.json.data.revision;
  evidence[`${keySuffix}Metrics`] = simulationBody.json.data.metrics;
  return { ruleId, simulation: simulationBody.json.data, pending: submitBody.json.data };
}

async function expectSameActorDecisionRejected(page, ruleId, command) {
  const approve = command === "approve";
  await page.getByRole("button", { name: approve ? "管理员批准并发布" : "管理员驳回" }).click();
  await page.locator("#confirm-reason").fill(approve
    ? `${marker} 验证提交人不能批准自己的规则`
    : `${marker} 验证提交人不能驳回自己的规则`);
  const responsePromise = page.waitForResponse((response) => response.request().method() === "POST"
    && response.url().includes(`/bpi-api/rules/${ruleId}/${approve ? "publish" : "reject-approval"}`));
  await page.locator("#confirm-submit").click();
  const response = await responsePromise;
  const body = await readJson(response);
  assert(response.status() === 422,
    `same-actor ${command} returned ${response.status()}: ${body.text.slice(0, 500)}`);
  assert(/different administrator|administrator other than/i.test(body.json?.detail || ""),
    `same-actor ${command} did not return the separation-of-duties reason`);
  await page.locator("#confirm-dialog button[value=cancel]").first().click();
  return { status: response.status(), detail: body.json.detail };
}

async function decideDirectly(api, flow, command) {
  const approve = command === "approve";
  const endpoint = approve ? "publish" : "reject-approval";
  const body = approve ? {
    reason: `${marker} 独立管理员复核后批准发布`,
    simulationId: flow.simulation.id,
    simulationChecksum: flow.simulation.checksum,
  } : { reason: `${marker} 独立管理员发现证据不足，退回修订` };
  const response = await api.post(`${serviceBaseUrl}/bpi/v1/rules/${flow.ruleId}/${endpoint}`, {
    data: body,
    headers: {
      Authorization: `Bearer ${internalToken()}`,
      "Content-Type": "application/json",
      "Idempotency-Key": `${marker}-${command}-decision`,
      "If-Match": String(flow.pending.revision),
      "X-Trace-Id": `${marker}-${command}`.slice(0, 64),
    },
    timeout: timeoutMs,
  });
  const parsed = await readJson(response);
  assert(response.status() === 200,
    `independent ${command} returned ${response.status()}: ${parsed.text.slice(0, 500)}`);
  return parsed.json.data;
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

    const topologyRow = page.locator("[data-topology-id]")
      .filter({ hasText: topologyCode })
      .filter({ hasText: "2.0.0" });
    assert(await topologyRow.count() === 1, "target topology version is not uniquely visible");
    report.evidence.topologyId = await topologyRow.getAttribute("data-topology-id");
    await topologyRow.click();
    await page.getByText(`对比 ${topologyCode}@1.0.0 → ${topologyCode}@2.0.0`, { exact: true })
      .waitFor({ timeout: timeoutMs });
    await page.locator(".version-diff code").getByText("/localityGroup", { exact: true })
      .waitFor({ timeout: timeoutMs });
    report.evidence.topologyComparison = (await page.locator("#detail-drawer").innerText()).slice(0, 3_000);

    const publishFlow = await simulateAndSubmit(page, report.evidence, publishRuleCode, "2.0.0", "publish");
    await page.locator(".version-diff code").getByText("/ast/conditions/0/threshold", { exact: true })
      .waitFor({ timeout: timeoutMs });
    report.evidence.sameSubmitterPublish = await expectSameActorDecisionRejected(page, publishFlow.ruleId, "approve");
    const published = await decideDirectly(api, publishFlow, "approve");
    assert(published.state === "PUBLISHED" && published.approvalStatus === "APPROVED",
      "independent approval did not publish the rule");
    await page.reload({ waitUntil: "networkidle", timeout: timeoutMs });
    await openRule(page, publishRuleCode, "2.0.0");
    await page.locator(".batch-state-band").getByText("PUBLISHED", { exact: true }).waitFor({ timeout: timeoutMs });
    await page.getByText("APPROVED", { exact: true }).last().waitFor({ timeout: timeoutMs });
    await page.getByText(approver, { exact: true }).waitFor({ timeout: timeoutMs });
    report.evidence.publishedRevision = published.revision;
    report.evidence.publishedApprovalStatus = published.approvalStatus;
    report.evidence.publishedDecisionBy = published.approvalDecidedBy;
    await page.screenshot({ path: `${screenshotPrefix}-published.png`, fullPage: true });

    await page.reload({ waitUntil: "networkidle", timeout: timeoutMs });
    const rejectFlow = await simulateAndSubmit(page, report.evidence, rejectRuleCode, "1.0.0", "reject");
    report.evidence.sameSubmitterReject = await expectSameActorDecisionRejected(page, rejectFlow.ruleId, "reject");
    const rejected = await decideDirectly(api, rejectFlow, "reject");
    assert(rejected.state === "DRAFT" && rejected.approvalStatus === "REJECTED",
      "independent rejection did not return the rule to DRAFT");
    await page.reload({ waitUntil: "networkidle", timeout: timeoutMs });
    await openRule(page, rejectRuleCode, "1.0.0");
    await page.locator(".batch-state-band").getByText("DRAFT", { exact: true }).waitFor({ timeout: timeoutMs });
    await page.getByText("REJECTED", { exact: true }).last().waitFor({ timeout: timeoutMs });
    await page.getByText(approver, { exact: true }).waitFor({ timeout: timeoutMs });
    report.evidence.rejectedRevision = rejected.revision;
    report.evidence.rejectedApprovalStatus = rejected.approvalStatus;
    report.evidence.rejectedDecisionBy = rejected.approvalDecidedBy;
    await page.screenshot({ path: `${screenshotPrefix}-rejected.png`, fullPage: true });

    report.expectedConsoleErrors = report.consoleErrors.filter((item) =>
      /Failed to load resource: the server responded with a status of 422/.test(item));
    report.unexpectedConsoleErrors = report.consoleErrors.filter((item) =>
      !/Failed to load resource: the server responded with a status of 422/.test(item));
    assert(report.expectedConsoleErrors.length === 2,
      `expected two separation-of-duties 422 console records, got ${report.expectedConsoleErrors.length}`);
    assert(report.unexpectedConsoleErrors.length === 0,
      `unexpected console errors: ${report.unexpectedConsoleErrors.join(" | ")}`);
    assert(report.pageErrors.length === 0, `unexpected page errors: ${report.pageErrors.join(" | ")}`);
    assert(report.requestFailures.length === 0,
      `unexpected request failures: ${JSON.stringify(report.requestFailures)}`);
    report.status = "PASS";
    report.summary = {
      topologyComparison: "PASS",
      ruleComparison: "PASS",
      simulation: "PASS",
      sameActorSeparation: "PASS",
      independentApproval: "PASS",
      independentRejection: "PASS",
      consoleErrors: report.consoleErrors.length,
      expectedConsoleErrors: report.expectedConsoleErrors.length,
      unexpectedConsoleErrors: report.unexpectedConsoleErrors.length,
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
