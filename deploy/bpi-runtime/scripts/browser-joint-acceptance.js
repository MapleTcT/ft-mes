#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { chromium, request } = require("playwright");

const action = required("BPI_BROWSER_ACTION");
const marker = required("BPI_ACCEPTANCE_MARKER");
const adpBaseUrl = required("ADP_BASE_URL").replace(/\/+$/, "");
const bpiBaseUrl = required("BPI_BROWSER_BASE_URL").replace(/\/+$/, "");
const username = required("ADP_USERNAME");
const password = required("ADP_PASSWORD");
const ruleCode = process.env.BPI_ACCEPTANCE_RULE_CODE || "";
const goldenSetId = process.env.BPI_ACCEPTANCE_GOLDEN_SET_ID || "";
const boundaryTime = process.env.BPI_ACCEPTANCE_BOUNDARY_TIME || "";
const orderId = process.env.BPI_ACCEPTANCE_ORDER_ID || `MO-${marker}`;
const outputPath = path.resolve(process.env.BPI_BROWSER_REPORT || `/tmp/bpi-joint-browser-${action}.json`);
const screenshotPath = path.resolve(process.env.BPI_BROWSER_SCREENSHOT || `/tmp/bpi-joint-browser-${action}.png`);
const headless = process.env.BPI_HEADLESS !== "false";
const timeoutMs = Number(process.env.BPI_BROWSER_TIMEOUT_MS || 120_000);

if (!new Set(["publish", "confirm", "read"]).has(action)) {
  throw new Error("BPI_BROWSER_ACTION must be publish, confirm or read");
}
if (action === "publish" && (!ruleCode || !goldenSetId || !boundaryTime)) {
  throw new Error("publish requires rule code, golden set ID and boundary time");
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
    failures.push({
      status: response.status(),
      contentType: response.headers()["content-type"] || "",
    });
  }
  throw new Error(`ADP login failed: ${JSON.stringify(failures)}`);
}

function localDateTime(iso) {
  const value = new Date(iso);
  if (Number.isNaN(value.getTime())) throw new Error(`invalid boundary time: ${iso}`);
  return new Date(value.getTime() - value.getTimezoneOffset() * 60_000)
    .toISOString()
    .slice(0, 19);
}

async function publishRule(page, evidence) {
  const boundary = new Date(boundaryTime);
  await page.goto(`${bpiBaseUrl}/#/rules`, { waitUntil: "networkidle", timeout: timeoutMs });
  await page.getByRole("heading", { name: "规则与拓扑" }).waitFor({ timeout: timeoutMs });
  const ruleRow = page.locator("[data-rule-id]").filter({ hasText: ruleCode });
  if (await ruleRow.count() !== 1) throw new Error(`expected one rule row for ${ruleCode}`);
  evidence.ruleId = await ruleRow.getAttribute("data-rule-id");
  await ruleRow.click();
  await page.getByRole("heading", { name: `${ruleCode}@1`, exact: true }).waitFor({ timeout: timeoutMs });
  await page.getByRole("button", { name: "运行历史回放" }).click();
  await page.locator("#simulation-from").fill(localDateTime(new Date(boundary.getTime() - 1_000).toISOString()));
  await page.locator("#simulation-to").fill(localDateTime(new Date(boundary.getTime() + 1_000).toISOString()));
  await page.locator("#simulation-calibration").fill("CAL-1");
  await page.locator("#simulation-golden").fill(goldenSetId);
  await page.locator("#simulation-submit").click();
  await page.getByText("历史回放通过，可提交发布", { exact: true }).waitFor({ timeout: timeoutMs });
  await page.getByRole("button", { name: "发布规则版本" }).click();
  await page.locator("#confirm-reason").fill(`历史 marker ${marker} 已回放通过并完成受控发布复核`);
  await page.locator("#confirm-submit").click();
  await page.getByText(new RegExp(`规则 ${escapeRegExp(ruleCode)}@1 已提交发布`)).waitFor({ timeout: timeoutMs });

  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    await page.waitForTimeout(2_000);
    await page.reload({ waitUntil: "networkidle", timeout: timeoutMs });
    const refreshedRow = page.locator("[data-rule-id]").filter({ hasText: ruleCode });
    await refreshedRow.click();
    if (await page.getByText("Flink 已应用", { exact: true }).count()) {
      evidence.applicationStatus = "APPLIED";
      evidence.ruleState = "PUBLISHED";
      await page.screenshot({ path: screenshotPath, fullPage: true });
      return;
    }
  }
  throw new Error("Flink application receipt did not become visible before timeout");
}

async function confirmCandidate(page, evidence) {
  await page.goto(`${bpiBaseUrl}/#/candidates`, { waitUntil: "networkidle", timeout: timeoutMs });
  await page.getByRole("heading", { name: "候选批次" }).waitFor({ timeout: timeoutMs });
  const row = page.locator("[data-candidate-id]").filter({ hasText: orderId });
  if (await row.count() !== 1) throw new Error(`expected one candidate row for ${orderId}`);
  evidence.candidateId = await row.getAttribute("data-candidate-id");
  await row.click();
  await page.locator("#open-confirm").click();
  await page.locator("#confirm-reason").fill(`现场复核 marker ${marker} 的指令、泵状态和瞬时流量证据`);
  await page.locator("#confirm-submit").click();
  await page.getByRole("heading", { name: "批次档案" }).waitFor({ timeout: timeoutMs });
  await page.getByText("SHADOW", { exact: true }).last().waitFor({ timeout: timeoutMs });
  evidence.candidateState = "CONFIRMED";
  evidence.batchState = "ACTIVE";
  evidence.shadow = true;
  await page.screenshot({ path: screenshotPath, fullPage: true });
}

async function readOverview(page, evidence) {
  await page.goto(`${bpiBaseUrl}/#/overview`, { waitUntil: "networkidle", timeout: timeoutMs });
  await page.getByRole("heading", { name: "实时生产态势" }).waitFor({ timeout: timeoutMs });
  evidence.brand = await page.title();
  evidence.overviewVisible = true;
  evidence.shadowModeVisible = await page.getByText("SHADOW", { exact: true }).count() > 0;
  await page.screenshot({ path: screenshotPath, fullPage: true });
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
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
    orderId,
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
      extraHTTPHeaders: { Authorization: `Bearer ${auth.ticket}` },
    });
    await context.addCookies([
      { name: "suposTicket", value: auth.ticket, url: bpiBaseUrl },
      { name: "SUPOS_TICKET", value: auth.ticket, url: bpiBaseUrl },
    ]);
    await context.addInitScript(({ token, loginPayload }) => {
      window.localStorage.setItem("suposTicket", token);
      window.localStorage.setItem("SUPOS_TICKET", token);
      window.localStorage.setItem("token", token);
      window.localStorage.setItem("ticket", token);
      window.sessionStorage.setItem("suposTicket", token);
      window.sessionStorage.setItem("SUPOS_TICKET", token);
      window.sessionStorage.setItem("token", token);
      window.sessionStorage.setItem("ticket", token);
      if (loginPayload) window.localStorage.setItem("loginMsg", JSON.stringify(loginPayload));
      window.localStorage.setItem("language", "zh_CN");
      window.localStorage.setItem("langu_code", "zh_CN");
      window.localStorage.setItem("locale", "zh-cn");
    }, { token: auth.ticket, loginPayload: auth.loginPayload });
    const page = await context.newPage();
    page.setDefaultTimeout(timeoutMs);
    page.on("console", (message) => {
      if (message.type() === "error") report.consoleErrors.push(message.text());
    });
    page.on("pageerror", (error) => report.pageErrors.push(error.message));
    page.on("requestfailed", (failed) => {
      report.requestFailures.push({ method: failed.method(), url: failed.url(), error: failed.failure()?.errorText || "" });
    });
    page.on("response", async (response) => {
      if (!response.url().includes("/bpi-api/")) return;
      const requestValue = response.request();
      let responseBody = "";
      try {
        responseBody = (await response.text()).slice(0, 2_000);
      } catch (_error) {
        responseBody = "<unavailable>";
      }
      report.requests.push({
        method: requestValue.method(),
        url: response.url(),
        requestBody: (requestValue.postData() || "").slice(0, 2_000),
        status: response.status(),
        responseBody,
      });
    });

    if (action === "publish") await publishRule(page, report.evidence);
    else if (action === "confirm") await confirmCandidate(page, report.evidence);
    else await readOverview(page, report.evidence);
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
