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
const outputPath = path.resolve(
  process.env.BPI_BROWSER_REPORT || `/tmp/bpi-topology-rule-${action}.json`,
);
const screenshotPath = path.resolve(
  process.env.BPI_BROWSER_SCREENSHOT || `/tmp/bpi-topology-rule-${action}.png`,
);
const headless = process.env.BPI_HEADLESS !== "false";
const timeoutMs = Number(process.env.BPI_BROWSER_TIMEOUT_MS || 120_000);
const topologyCode = `${marker}_TOPOLOGY`;
const ruleCode = `${marker}_START`;
const topologyRef = `${topologyCode}@1.0.0`;

if (!new Set(["author", "finalize", "read"]).has(action)) {
  throw new Error("BPI_BROWSER_ACTION must be author, finalize or read");
}
if (!/^[A-Za-z0-9_-]{8,96}$/.test(marker)) {
  throw new Error("BPI_ACCEPTANCE_MARKER must use 8-96 letters, digits, underscores or hyphens");
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

function topologyDefinition() {
  return {
    localityGroup: "LINE-S07-01-E2E",
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
        productId: "PRODUCT-SUGAR",
        deviceId: `${marker}_DEVICE`,
        propertyId: "flow.instant",
        expectedUnit: "t/h",
        calibrationVersion: "CAL-1",
      },
    ],
    requiredSignals: ["feed.flow"],
  };
}

async function authorTopology(page, evidence) {
  await page.goto(`${bpiBaseUrl}/#/rules`, { waitUntil: "networkidle", timeout: timeoutMs });
  await page.getByRole("heading", { name: "规则与拓扑" }).waitFor({ timeout: timeoutMs });
  await page.getByRole("button", { name: "新建拓扑" }).click();
  await page.getByRole("heading", { name: "新建拓扑版本" }).waitFor();
  await page.locator("#topology-code").fill(topologyCode);
  await page.locator("#topology-version").fill("1.0.0");
  await page.locator("#topology-line").fill("LINE-S07-01");
  await page.locator("#topology-definition").fill(JSON.stringify(topologyDefinition(), null, 2));
  await page.locator("#topology-reason").fill(`目标环境 marker ${marker} 创建拓扑草稿`);
  const createResponsePromise = page.waitForResponse(
    (response) => response.url().includes("/bpi-api/topologies/drafts") && response.request().method() === "POST",
  );
  await page.getByRole("button", { name: "创建草稿" }).click();
  const createResponse = await createResponsePromise;
  const createPayload = await createResponse.json();
  if (createResponse.status() !== 200) throw new Error(`topology creation returned ${createResponse.status()}`);
  evidence.topologyId = createPayload.data.id;
  evidence.createdRevision = createPayload.data.revision;
  await page.getByText(`拓扑草稿 ${topologyRef} 已创建`).waitFor();
  await page.getByRole("heading", { name: topologyRef }).waitFor();

  await page.getByRole("button", { name: "校验拓扑" }).click();
  await page.locator("#confirm-reason").fill("核对路径、JetLinks 点位、单位和校准版本");
  const validateResponsePromise = page.waitForResponse(
    (response) => response.url().includes(`/bpi-api/topologies/${evidence.topologyId}/validate`),
  );
  await page.getByRole("button", { name: "开始校验" }).click();
  const validateResponse = await validateResponsePromise;
  const validatePayload = await validateResponse.json();
  if (validateResponse.status() !== 200 || validatePayload.data.validationStatus !== "PASSED") {
    throw new Error(`topology validation returned ${validateResponse.status()}`);
  }
  evidence.validationStatus = validatePayload.data.validationStatus;
  evidence.validatedRevision = validatePayload.data.revision;
  evidence.validationErrors = validatePayload.data.validationErrors.length;
  await page.getByText("拓扑校验通过，可提交独立管理员发布").waitFor();

  await page.getByRole("button", { name: "发布拓扑" }).click();
  await page.locator("#confirm-reason").fill("创建人尝试发布，用于验证职责分离门禁");
  const publishResponsePromise = page.waitForResponse(
    (response) => response.url().includes(`/bpi-api/topologies/${evidence.topologyId}/publish`),
  );
  await page.getByRole("button", { name: "确认发布" }).click();
  const publishResponse = await publishResponsePromise;
  const publishPayload = await publishResponse.json();
  if (publishResponse.status() !== 422) {
    throw new Error(`creator publication must return 422, got ${publishResponse.status()}`);
  }
  evidence.creatorPublishStatus = publishResponse.status();
  evidence.creatorPublishRejected = /other than the creator/.test(publishPayload.detail || "");
  if (!evidence.creatorPublishRejected) throw new Error("creator publication rejection detail is missing");
  await page.locator("#confirm-dialog").getByRole("button", { name: "取消" }).click();
  await page.screenshot({ path: screenshotPath, fullPage: true });
}

async function finalizeRule(page, evidence) {
  await page.goto(`${bpiBaseUrl}/#/rules`, { waitUntil: "networkidle", timeout: timeoutMs });
  await page.getByRole("heading", { name: "规则与拓扑" }).waitFor({ timeout: timeoutMs });
  const topologyRow = page.locator("[data-topology-id]").filter({ hasText: topologyCode });
  if (await topologyRow.count() !== 1) throw new Error(`expected one topology row for ${topologyCode}`);
  evidence.topologyId = await topologyRow.getAttribute("data-topology-id");
  await topologyRow.click();
  await page.getByRole("heading", { name: topologyRef }).waitFor();
  const topologyDrawer = page.locator("#detail-drawer");
  await topologyDrawer.getByText("PUBLISHED", { exact: true }).waitFor();
  await topologyDrawer.getByText("PASSED", { exact: true }).waitFor();
  evidence.topologyState = "PUBLISHED";
  evidence.validationStatus = "PASSED";
  evidence.topologyRevision = 3;
  await topologyDrawer.locator("[data-close-drawer]").first().click();

  await page.getByRole("button", { name: "新建规则" }).click();
  await page.getByRole("heading", { name: "新建规则版本" }).waitFor();
  await page.locator("#rule-code").fill(ruleCode);
  await page.locator("#rule-version").fill("1.0.0");
  await page.locator("#rule-topology").selectOption(topologyRef);
  if ((await page.locator("#rule-line").inputValue()) !== "LINE-S07-01") {
    throw new Error("rule line did not follow the selected topology");
  }
  const ast = JSON.parse(await page.locator("#rule-ast").inputValue());
  if (ast.conditions?.[0]?.signal !== "feed.flow") {
    throw new Error("rule AST did not inherit the selected topology signal");
  }
  await page.locator("#rule-reason").fill(`目标环境 marker ${marker} 创建启动边界规则`);
  const createRuleResponsePromise = page.waitForResponse(
    (response) => response.url().includes("/bpi-api/rules/drafts") && response.request().method() === "POST",
  );
  await page.getByRole("button", { name: "创建草稿" }).click();
  const createRuleResponse = await createRuleResponsePromise;
  const createRulePayload = await createRuleResponse.json();
  if (createRuleResponse.status() !== 200) {
    throw new Error(`rule creation returned ${createRuleResponse.status()}`);
  }
  evidence.ruleId = createRulePayload.data.id;
  evidence.ruleState = createRulePayload.data.state;
  evidence.ruleRevision = createRulePayload.data.revision;
  evidence.ruleTopologyVersion = createRulePayload.data.topologyVersion;
  await page.getByText(`规则草稿 ${ruleCode}@1.0.0 已创建`).waitFor();
  await page.getByRole("heading", { name: `${ruleCode}@1.0.0` }).waitFor();
  await page.screenshot({ path: screenshotPath, fullPage: true });
}

async function readProductization(page, evidence) {
  await page.goto(`${bpiBaseUrl}/#/rules`, { waitUntil: "networkidle", timeout: timeoutMs });
  await page.getByRole("heading", { name: "规则与拓扑" }).waitFor({ timeout: timeoutMs });
  const topologyRow = page.locator("[data-topology-id]").filter({ hasText: topologyCode });
  const ruleRow = page.locator("[data-rule-id]").filter({ hasText: ruleCode });
  if (await topologyRow.count() !== 1 || await ruleRow.count() !== 1) {
    throw new Error("published topology or rule draft is not uniquely visible");
  }
  evidence.topologyVisible = true;
  evidence.ruleVisible = true;
  evidence.topologyText = (await topologyRow.innerText()).slice(0, 500);
  evidence.ruleText = (await ruleRow.innerText()).slice(0, 500);
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
    topologyCode,
    ruleCode,
    adpBaseUrl,
    bpiBaseUrl,
    loginStatus: null,
    page: {},
    requests: [],
    consoleErrors: [],
    expectedConsoleErrors: [],
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
    await context.addInitScript(({ token, loginPayload }) => {
      for (const key of ["suposTicket", "SUPOS_TICKET", "token", "ticket"]) {
        window.localStorage.setItem(key, token);
        window.sessionStorage.setItem(key, token);
      }
      if (loginPayload) window.localStorage.setItem("loginMsg", JSON.stringify(loginPayload));
      window.localStorage.setItem("language", "zh_CN");
      window.localStorage.setItem("langu_code", "zh_CN");
      window.localStorage.setItem("locale", "zh-cn");
    }, { token: auth.ticket, loginPayload: auth.loginPayload });
    const page = await context.newPage();
    page.setDefaultTimeout(timeoutMs);
    page.on("console", (message) => {
      if (message.type() !== "error") return;
      const location = message.location();
      const expectedCreatorRejection =
        action === "author" &&
        message.text().includes("status of 422") &&
        location.url.includes(`/bpi-api/topologies/${report.evidence.topologyId || "pending"}/publish`);
      if (expectedCreatorRejection) {
        report.expectedConsoleErrors.push({ text: message.text(), url: location.url });
        return;
      }
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

    if (action === "author") await authorTopology(page, report.evidence);
    else if (action === "finalize") await finalizeRule(page, report.evidence);
    else await readProductization(page, report.evidence);
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
