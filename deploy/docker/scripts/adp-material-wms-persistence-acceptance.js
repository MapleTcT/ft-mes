#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { execFileSync, spawnSync } = require("child_process");
const { chromium, request } = require("playwright");

const repoRoot = path.resolve(__dirname, "../../..");
const baseUrl = (process.env.ADP_BASE_URL || "http://100.99.133.43:18080").replace(/\/+$/, "");
const browserBaseUrl = (process.env.ADP_BROWSER_BASE_URL || baseUrl).replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD || "123456";
const sshTarget = process.env.ADP_DB_SSH_TARGET || "v6@100.99.133.43";
const dbContainer = process.env.ADP_DB_CONTAINER || "adp-mes-newbase-postgres-1";
const dbName = process.env.ADP_DB_NAME || "adp";
const dbUser = process.env.ADP_DB_USER || "adp";
const headless = process.env.ADP_HEADLESS !== "false";
const qualityMode = process.env.ADP_WMS_QUALITY_MODE || "qualified";
if (!["qualified", "unqualified"].includes(qualityMode)) {
  throw new Error("ADP_WMS_QUALITY_MODE must be qualified or unqualified");
}
const unqualified = qualityMode === "unqualified";
const nowToken = new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14);
const marker = process.env.ADP_E2E_MARKER
  || `ADP_E2E_${nowToken}_MATERIAL_WMS_${unqualified ? "UNQUAL" : "QUAL"}`;
const outputDir = process.env.ADP_OUTPUT_DIR || path.join("/tmp", `adp-material-wms-${nowToken}`);
const outputPath = process.env.ADP_MATERIAL_WMS_ACCEPTANCE_OUTPUT || path.join(outputDir, "acceptance.json");
const screenshotPath = process.env.ADP_MATERIAL_WMS_SCREENSHOT || path.join(outputDir, "completion-inbound.png");
const defaultChromePath = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";
const browserExecutable = process.env.ADP_BROWSER_EXECUTABLE
  || (fs.existsSync(defaultChromePath) ? defaultChromePath : "");
const tenantId = `${marker}_COMP`;
const inboundSource = `${marker}_IN`;
const inboundLine = `${marker}_IN_LINE`;
const issueSource = `${marker}_OUT`;
const warehouseCode = `${marker}_WARE`;
const locationCode = `${marker}_LOC`;
const materialCode = `${marker}_MAT`;
const batchNo = `${marker}_BATCH`;
const productionBatchNo = `${marker}_PROD_BATCH`;

function ensureDir(filePath) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
}

function shellQuote(value) {
  return `'${String(value).replace(/'/g, "'\\''")}'`;
}

function sqlLiteral(value) {
  return `'${String(value).replace(/'/g, "''")}'`;
}

function runSql(sql) {
  const remoteCommand = [
    "docker", "exec", "-i", shellQuote(dbContainer), "psql",
    "-U", shellQuote(dbUser), "-d", shellQuote(dbName), "-v", "ON_ERROR_STOP=1", "-At",
  ].join(" ");
  return execFileSync(
    "ssh",
    ["-o", "BatchMode=yes", "-o", "ConnectTimeout=8", sshTarget, remoteCommand],
    { input: sql, encoding: "utf8", stdio: ["pipe", "pipe", "pipe"] }
  ).trim();
}

function gitCommit() {
  const result = spawnSync("git", ["rev-parse", "HEAD"], { cwd: repoRoot, encoding: "utf8" });
  return result.status === 0 ? result.stdout.trim() : "UNKNOWN";
}

function findTicket(payload) {
  const candidates = [
    payload && payload.ticket,
    payload && payload.access_token,
    payload && payload.token,
    payload && payload.data && payload.data.ticket,
    payload && payload.data && payload.data.access_token,
    payload && payload.data && payload.data.token,
  ];
  return candidates.find((value) => typeof value === "string" && value.length > 20);
}

async function readJson(response) {
  const text = await response.text();
  try {
    return { text, json: JSON.parse(text) };
  } catch (_error) {
    return { text, json: null };
  }
}

async function login(api) {
  const attempts = [
    { userName: username, password, clientId: "pc_dt" },
    { username, password, clientId: "pc_dt" },
  ];
  const errors = [];
  for (const body of attempts) {
    const response = await api.post(`${baseUrl}/inter-api/auth/login`, { data: body });
    const parsed = await readJson(response);
    const ticket = response.ok() ? findTicket(parsed.json) : null;
    if (ticket) return ticket;
    errors.push({ status: response.status(), body: parsed.text.slice(0, 300) });
  }
  throw new Error(`登录失败: ${JSON.stringify(errors)}`);
}

async function postJson(api, ticket, url, data) {
  const response = await api.post(`${baseUrl}${url}`, {
    data,
    headers: {
      Authorization: `Bearer ${ticket}`,
      "X-Tenant-Id": tenantId,
      Accept: "application/json",
      "Content-Type": "application/json;charset=UTF-8",
    },
  });
  const parsed = await readJson(response);
  return { method: "POST", url, payload: data, status: response.status(), body: parsed.json || parsed.text };
}

function stateSql() {
  return `SELECT json_build_object(
    'documents', (SELECT count(*) FROM wms_stock_documents WHERE tenant_id=${sqlLiteral(tenantId)}),
    'lines', (SELECT count(*) FROM wms_stock_document_lines WHERE tenant_id=${sqlLiteral(tenantId)}),
    'transactions', (SELECT count(*) FROM wms_inventory_transactions WHERE tenant_id=${sqlLiteral(tenantId)}),
    'qualityResults', (SELECT count(*) FROM wms_quality_results WHERE tenant_id=${sqlLiteral(tenantId)}),
    'stockRows', (SELECT count(*) FROM wms_batch_stocks WHERE tenant_id=${sqlLiteral(tenantId)}),
    'onHand', COALESCE((SELECT on_hand_quantity FROM wms_batch_stocks WHERE tenant_id=${sqlLiteral(tenantId)} LIMIT 1), 0),
    'available', COALESCE((SELECT available_quantity FROM wms_batch_stocks WHERE tenant_id=${sqlLiteral(tenantId)} LIMIT 1), 0),
    'hold', COALESCE((SELECT hold_quantity FROM wms_batch_stocks WHERE tenant_id=${sqlLiteral(tenantId)} LIMIT 1), 0),
    'inboundQuality', COALESCE((SELECT quality_status FROM wms_stock_documents WHERE tenant_id=${sqlLiteral(tenantId)} AND document_type='COMPLETION_INBOUND' LIMIT 1), '')
);`;
}

function queryState(label) {
  return { label, ...JSON.parse(runSql(stateSql())) };
}

function cleanupSql() {
  return `BEGIN;
DELETE FROM wms_stock_documents WHERE tenant_id=${sqlLiteral(tenantId)};
DELETE FROM wms_quality_results WHERE tenant_id=${sqlLiteral(tenantId)};
DELETE FROM wms_batch_stocks WHERE tenant_id=${sqlLiteral(tenantId)};
COMMIT;`;
}

function assertCondition(condition, message) {
  if (!condition) throw new Error(message);
}

function numeric(value) {
  return Number(value || 0);
}

async function browserAcceptance(ticket) {
  const launchOptions = { headless };
  if (browserExecutable) launchOptions.executablePath = browserExecutable;
  const browser = await chromium.launch(launchOptions);
  const consoleErrors = [];
  const requestFailures = [];
  const badResponses = [];
  try {
    const context = await browser.newContext({
      extraHTTPHeaders: { Authorization: `Bearer ${ticket}`, "X-Tenant-Id": tenantId },
      viewport: { width: 1440, height: 900 },
    });
    const page = await context.newPage();
    page.on("console", (message) => {
      if (message.type() === "error") consoleErrors.push(message.text());
    });
    page.on("requestfailed", (requestValue) => requestFailures.push({
      url: requestValue.url(), error: requestValue.failure() && requestValue.failure().errorText,
    }));
    page.on("response", (response) => {
      if (response.status() >= 400) badResponses.push({ url: response.url(), status: response.status() });
    });
    const response = await page.goto(`${browserBaseUrl}/msService/material/wms`, {
      waitUntil: "domcontentloaded", timeout: 120000,
    });
    await page.locator("h1", { hasText: "完工入库" }).waitFor({ timeout: 30000 });
    await page.locator("#keyword").fill(marker);
    await page.locator("#search").click();
    await page.locator("tbody tr").first().waitFor({ timeout: 30000 });
    await page.screenshot({ path: screenshotPath, fullPage: true });
    await page.locator("tbody tr").first().click();
    await page.locator("#detail-dialog[open]").waitFor({ timeout: 10000 });
    const pageText = await page.locator("body").innerText();
    return {
      route: "/msService/material/wms",
      httpStatus: response ? response.status() : null,
      markerVisible: pageText.includes(marker),
      detailOpened: await page.locator("#detail-dialog[open]").count() === 1,
      consoleErrors,
      requestFailures,
      badResponses,
      screenshot: screenshotPath,
    };
  } finally {
    await browser.close();
  }
}

async function main() {
  ensureDir(outputPath);
  ensureDir(screenshotPath);
  const api = await request.newContext({ timeout: 30000 });
  const evidence = {
    generatedAt: new Date().toISOString(), repoCommit: gitCommit(), database: "PostgreSQL",
    marker, tenantId, qualityMode, status: "FAIL", requests: [], states: [], browser: null,
    verificationSql: stateSql(), cleanupSql: cleanupSql(), issues: [],
  };
  let exitCode = 1;
  try {
    runSql(cleanupSql());
    evidence.states.push(queryState("before"));
    const ticket = await login(api);
    const inboundPayload = {
      srcID: inboundSource,
      srcTableNo: `${marker}_IN_NO`,
      directiveNo: `${marker}_MO`,
      companyCode: tenantId,
      userName: username,
      staffCode: "ADP_E2E",
      deptCode: "ADP_E2E",
      redBlue: "blue",
      comeType: "produceIn",
      workflowStateSetCode: "productInSingleFlw",
      storageDate: new Date().toISOString().slice(0, 10),
      wareCode: warehouseCode,
      detailList: [{
        srcPartId: inboundLine,
        goodCode: materialCode,
        batchText: batchNo,
        produceBatchNum: productionBatchNo,
        placeSetCode: locationCode,
        quantity: 10,
      }],
    };
    const inbound = await postJson(api, ticket,
      "/msService/public/material/produceInSingles/produceInSingl/generateProductInSingle", inboundPayload);
    evidence.requests.push(inbound);
    assertCondition(inbound.status === 200 && inbound.body && inbound.body.code === 200, "完工入库接口失败");
    evidence.states.push(queryState("afterInbound"));

    const retry = await postJson(api, ticket,
      "/msService/public/material/produceInSingles/produceInSingl/generateProductInSingle", inboundPayload);
    evidence.requests.push(retry);
    assertCondition(retry.body && retry.body.code === 200 && retry.body.data.idempotent === true, "重复入库未命中幂等");
    evidence.states.push(queryState("afterInboundRetry"));

    const quality = await postJson(api, ticket,
      `/msService/material/foreign/foreign/checkProdResult?srcId=${encodeURIComponent(inboundLine)}&checkResult=${encodeURIComponent(
        unqualified ? "BaseSet_checkResult/unqualified" : "BaseSet_checkResult/qualified"
      )}`,
      {});
    evidence.requests.push(quality);
    assertCondition(quality.body && quality.body.code === 200, "质检结果回写失败");
    evidence.states.push(queryState("afterQuality"));

    const issuePayload = {
      srcId: issueSource,
      srcTableNo: `${marker}_OUT_NO`,
      companyCode: tenantId,
      redBlue: "blue",
      comeType: "produceOut",
      storageDate: new Date().toISOString().slice(0, 10),
      wareCode: warehouseCode,
      detailList: [{
        goodCode: materialCode,
        batchText: batchNo,
        productBatch: productionBatchNo,
        placeSetCode: locationCode,
        quantity: 3,
      }],
    };
    const issue = await postJson(api, ticket,
      "/msService/public/material/produceOutSingle/produceOutSing/generateProduceOutSing", issuePayload);
    evidence.requests.push(issue);
    if (unqualified) {
      assertCondition(
        issue.status >= 400 || !issue.body || issue.body.code !== 200,
        "不合格冻结库存仍然允许领料出库"
      );
    } else {
      assertCondition(issue.body && issue.body.code === 200, "生产领料出库失败");
    }
    evidence.states.push(queryState("afterIssue"));

    evidence.browser = await browserAcceptance(ticket);
    const afterInbound = evidence.states.find((state) => state.label === "afterInbound");
    const afterRetry = evidence.states.find((state) => state.label === "afterInboundRetry");
    const afterQuality = evidence.states.find((state) => state.label === "afterQuality");
    const afterIssue = evidence.states.find((state) => state.label === "afterIssue");
    assertCondition(numeric(afterInbound.documents) === 1 && numeric(afterInbound.lines) === 1, "入库单或明细未落库");
    assertCondition(numeric(afterInbound.onHand) === 10 && numeric(afterInbound.hold) === 10, "待检入库库存不正确");
    assertCondition(JSON.stringify(afterInbound) === JSON.stringify({ ...afterRetry, label: "afterInbound" }), "幂等重试改变了数据库");
    if (unqualified) {
      assertCondition(
        numeric(afterQuality.onHand) === 10
          && numeric(afterQuality.available) === 0
          && numeric(afterQuality.hold) === 10
          && afterQuality.inboundQuality === "UNQUALIFIED",
        "不合格回写未保持库存冻结"
      );
      assertCondition(
        numeric(afterIssue.documents) === 1
          && numeric(afterIssue.onHand) === 10
          && numeric(afterIssue.available) === 0
          && numeric(afterIssue.hold) === 10,
        "不合格领料拦截后库存发生变化"
      );
    } else {
      assertCondition(
        numeric(afterQuality.available) === 10 && numeric(afterQuality.hold) === 0,
        "合格回写未释放库存"
      );
      assertCondition(
        numeric(afterIssue.documents) === 2
          && numeric(afterIssue.onHand) === 7
          && numeric(afterIssue.available) === 7,
        "领料扣减不正确"
      );
    }
    assertCondition(evidence.browser.httpStatus === 200 && evidence.browser.markerVisible && evidence.browser.detailOpened, "运行视图验收失败");
    assertCondition(evidence.browser.consoleErrors.length === 0 && evidence.browser.requestFailures.length === 0 && evidence.browser.badResponses.length === 0, "运行视图存在 console/network 错误");
    evidence.status = "PASS";
    exitCode = 0;
  } catch (error) {
    evidence.issues.push(error.stack || error.message);
  } finally {
    try {
      runSql(cleanupSql());
      evidence.states.push(queryState("afterCleanup"));
    } catch (cleanupError) {
      evidence.issues.push(`cleanup: ${cleanupError.message}`);
      evidence.status = "FAIL";
      exitCode = 1;
    }
    fs.writeFileSync(outputPath, `${JSON.stringify(evidence, null, 2)}\n`, "utf8");
    await api.dispose();
  }
  console.log(`${evidence.status}: ${outputPath}`);
  process.exitCode = exitCode;
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
