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
const nowToken = new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14);
const marker = process.env.ADP_E2E_MARKER || `ADP_E2E_${nowToken}_PROCESS_ANALYSIS`;
const outputDir = process.env.ADP_OUTPUT_DIR || path.join("/tmp", `adp-process-analysis-${nowToken}`);
const outputPath = process.env.ADP_PROCESS_ANALYSIS_ACCEPTANCE_OUTPUT || path.join(outputDir, "acceptance.json");
const screenshotPath = process.env.ADP_PROCESS_ANALYSIS_SCREENSHOT || path.join(outputDir, "process-trace.png");
const defaultChromePath = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";
const browserExecutable = process.env.ADP_BROWSER_EXECUTABLE
  || (fs.existsSync(defaultChromePath) ? defaultChromePath : "");
const sourceTaskId = process.env.ADP_PROCESS_ANALYSIS_TASK_ID || "8991075113025740";
const sourceTaskExecutionId = process.env.ADP_PROCESS_ANALYSIS_TASK_EXECUTION_ID || "8991075113025744";
const sourceProcessExecutionId = process.env.ADP_PROCESS_ANALYSIS_PROCESS_EXECUTION_ID || "8991075113025745";
const sourceActivityExecutionId = process.env.ADP_PROCESS_ANALYSIS_ACTIVITY_EXECUTION_ID || "764215029294336";
const sourceTableNo = process.env.ADP_PROCESS_ANALYSIS_TABLE_NO || "ADP_E2E_20260710023850_WOM_CHECKOUTBILL_TASK_TN";
const sourceBatchNo = process.env.ADP_PROCESS_ANALYSIS_BATCH_NO || "ADP_E2E_20260710023850_WOM_CHECKOUTBILL_BATCH";
const sourceProductNo = process.env.ADP_PROCESS_ANALYSIS_PRODUCT_NO || "ADP_E2E_20260710023850_WOM_CHECKOUTBILL_MAT";
const browserTaskId = process.env.ADP_PROCESS_ANALYSIS_BROWSER_TASK_ID || sourceTaskId;
const browserTableNo = process.env.ADP_PROCESS_ANALYSIS_BROWSER_TABLE_NO || sourceTableNo;
const browserBatchNo = process.env.ADP_PROCESS_ANALYSIS_BROWSER_BATCH_NO || sourceBatchNo;
const keepFixture = process.env.ADP_PROCESS_ANALYSIS_KEEP_FIXTURE === "true";
const tenantId = `${marker}_TENANT`;
const gridId = "WOM_1.0.0_produceTask_makeTaskList_produceTask_sdg";

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

async function readBody(response) {
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
    const parsed = await readBody(response);
    const ticket = response.ok() ? findTicket(parsed.json) : null;
    if (ticket) return ticket;
    errors.push({ status: response.status(), body: parsed.text.slice(0, 300) });
  }
  throw new Error(`登录失败: ${JSON.stringify(errors)}`);
}

async function apiGet(api, ticket, route) {
  const response = await api.get(`${baseUrl}${route}`, {
    headers: { Authorization: `Bearer ${ticket}`, "X-Tenant-Id": tenantId, Accept: "application/json" },
  });
  const parsed = await readBody(response);
  return { method: "GET", url: route, status: response.status(), body: parsed.json || parsed.text };
}

async function apiPost(api, ticket, route, payload) {
  const response = await api.post(`${baseUrl}${route}`, {
    data: payload,
    headers: {
      Authorization: `Bearer ${ticket}`,
      "X-Tenant-Id": tenantId,
      Accept: "application/json",
      "Content-Type": "application/json;charset=UTF-8",
    },
  });
  const parsed = await readBody(response);
  return { method: "POST", url: route, payload, status: response.status(), body: parsed.json || parsed.text };
}

function stateSql() {
  return `SELECT json_build_object(
    'snapshotCount', (SELECT count(*) FROM pa_trace_snapshots WHERE tenant_id=${sqlLiteral(tenantId)}),
    'taskRevision', COALESCE((SELECT revision FROM pa_trace_snapshots WHERE tenant_id=${sqlLiteral(tenantId)} AND source_type='TASK' AND source_id=${sourceTaskExecutionId}), 0),
    'processRevision', COALESCE((SELECT revision FROM pa_trace_snapshots WHERE tenant_id=${sqlLiteral(tenantId)} AND source_type='PROCESS' AND source_id=${sourceProcessExecutionId}), 0),
    'activityRevision', COALESCE((SELECT revision FROM pa_trace_snapshots WHERE tenant_id=${sqlLiteral(tenantId)} AND source_type='ACTIVITY' AND source_id=${sourceActivityExecutionId}), 0),
    'batchRows', (SELECT count(*) FROM pa_trace_snapshots WHERE tenant_id=${sqlLiteral(tenantId)} AND batch_no=${sqlLiteral(sourceBatchNo)}),
    'wmsDocuments', (SELECT count(*) FROM wms_stock_documents WHERE tenant_id=${sqlLiteral(tenantId)}),
    'wmsTransactions', (SELECT count(*) FROM wms_inventory_transactions WHERE tenant_id=${sqlLiteral(tenantId)}),
    'wmsStockRows', (SELECT count(*) FROM wms_batch_stocks WHERE tenant_id=${sqlLiteral(tenantId)}),
    'sourceRows', (
      SELECT count(*) FROM wom_produce_tasks t
      JOIN wom_produce_task_exelog te ON te.task_id=t.id
      WHERE t.id=${sourceTaskId} AND t.produce_batch_num=${sqlLiteral(sourceBatchNo)}
    )
  );`;
}

function queryState(label) {
  return { label, ...JSON.parse(runSql(stateSql())) };
}

function cleanupSql() {
  return `BEGIN;
DELETE FROM pa_trace_snapshots WHERE tenant_id=${sqlLiteral(tenantId)};
DELETE FROM wms_stock_documents WHERE tenant_id=${sqlLiteral(tenantId)};
DELETE FROM wms_quality_results WHERE tenant_id=${sqlLiteral(tenantId)};
DELETE FROM wms_batch_stocks WHERE tenant_id=${sqlLiteral(tenantId)};
COMMIT;`;
}

function assertCondition(condition, message) {
  if (!condition) throw new Error(message);
}

async function selectSourceRow(page) {
  await page.waitForFunction((code) => {
    const factory = window.ReactAPI && window.ReactAPI.getComponentAPI("SupDataGrid");
    const grid = factory && factory.APIs && factory.APIs(code);
    return Boolean(grid && typeof grid.refreshDataByRequst === "function");
  }, gridId, { timeout: 120000 });

  const queryResponse = page.waitForResponse((response) =>
    /\/WOM\/produceTask\/produceTask\/makeTaskList-query(?:\?|$)/.test(response.url())
      && response.request().method() === "POST", { timeout: 120000 });
  const body = await page.evaluate(async () => {
    const response = await fetch("/msService/WOM/produceTask/produceTask/makeTaskList-query", {
      method: "POST",
      credentials: "same-origin",
      headers: { "Content-Type": "application/json;charset=UTF-8", Accept: "application/json" },
      body: JSON.stringify({
        classifyCodes: "", customCondition: {},
        permissionCode: "WOM_1.0.0_produceTask_makeTaskList",
        pageNo: 1, paging: true, pageSize: 65535, crossCompanyFlag: "true",
      }),
    });
    return response.json();
  });
  const response = await queryResponse;
  assertCondition(response.status() === 200 && body.code === 200, `WOM 列表查询失败: ${response.status()}`);
  const result = body && body.data && Array.isArray(body.data.result) ? body.data.result : [];
  const target = result.find((row) => String(row.id) === String(browserTaskId));
  assertCondition(Boolean(target), `WOM query 真实响应中不存在目标指令 ${browserTaskId}`);

  return page.evaluate(({ code, row }) => {
    const grid = window.ReactAPI.getComponentAPI("SupDataGrid").APIs(code);
    grid.setDatagridData([row]);
    grid.setSelecteds("0");
    const selected = grid.getSelecteds()[0] || {};
    return {
      rowIndex: 0,
      id: String(selected.id || ""),
      tableNo: selected.tableNo,
      batchNo: selected.produceBatchNum,
      taskRunState: selected.taskRunState && (selected.taskRunState.id || selected.taskRunState.value),
      selectionSource: "makeTaskList-query response loaded through SupDataGrid.setDatagridData",
    };
  }, { code: gridId, row: target });
}

async function browserAcceptance(ticket) {
  const launchOptions = { headless };
  if (browserExecutable) launchOptions.executablePath = browserExecutable;
  const browser = await chromium.launch(launchOptions);
  try {
    const context = await browser.newContext({
      extraHTTPHeaders: { Authorization: `Bearer ${ticket}`, "X-Tenant-Id": tenantId },
      viewport: { width: 1500, height: 960 },
    });
    const listPage = await context.newPage();
    const listConsoleErrors = [];
    const processResponses = [];
    listPage.on("console", (message) => {
      if (message.type() === "error") listConsoleErrors.push(message.text());
    });
    listPage.on("response", (response) => {
      if (response.url().includes("/ProcessAnalysis/")) {
        processResponses.push({ method: response.request().method(), url: response.url(), status: response.status() });
      }
    });
    const navigation = await listPage.goto(
      `${browserBaseUrl}/msService/WOM/produceTask/produceTask/makeTaskList`,
      { waitUntil: "domcontentloaded", timeout: 180000 }
    );
    const selected = await selectSourceRow(listPage);
    assertCondition(selected.id === browserTaskId, `未选中目标 WOM 指令: ${JSON.stringify(selected)}`);
    const powerCodeProbe = await listPage.evaluate(() => {
      try {
        const value = window.ReactAPI.getPowerCode(
          "ProcessAnalysis_1.0.0_processAnalysis_processBatchViewOut_self",
          function () {}
        );
        return { ok: true, type: typeof value, value: value == null ? null : value };
      } catch (error) {
        return { ok: false, error: error.message };
      }
    });

    listConsoleErrors.length = 0;
    processResponses.length = 0;
    const precheckPromise = listPage.waitForResponse((response) =>
      response.url().includes("/ProcessAnalysis/analysisParam/analysisParam/isProdprocessView"),
      { timeout: 30000 });
    const popupPromise = listPage.waitForEvent("popup", { timeout: 30000 })
      .then((page) => ({ page }))
      .catch((error) => ({ error: error.message }));
    await listPage.locator("#btn-prodprocessView").click({ timeout: 30000 });
    const precheck = await precheckPromise;
    const precheckBody = await precheck.json();
    const popupResult = await popupPromise;
    if (!popupResult.page) {
      return {
        route: "/msService/WOM/produceTask/produceTask/makeTaskList",
        navigationStatus: navigation && navigation.status(),
        selected,
        powerCodeProbe,
        precheck: { status: precheck.status(), body: precheckBody },
        popupError: popupResult.error,
        listConsoleErrors,
        processResponses,
        traceMarkerVisible: false,
        traceTitleVisible: false,
        traceConsoleErrors: [],
        traceBadResponses: [],
        traceRequestFailures: [],
      };
    }
    const tracePage = popupResult.page;
    const traceConsoleErrors = [];
    const traceBadResponses = [];
    const traceRequestFailures = [];
    tracePage.on("console", (message) => {
      if (message.type() === "error") traceConsoleErrors.push(message.text());
    });
    tracePage.on("response", (response) => {
      if (response.status() >= 400) traceBadResponses.push({ url: response.url(), status: response.status() });
    });
    tracePage.on("requestfailed", (requestValue) => traceRequestFailures.push({
      url: requestValue.url(), error: requestValue.failure() && requestValue.failure().errorText,
    }));
    await tracePage.waitForLoadState("domcontentloaded", { timeout: 60000 });
    await tracePage.locator("h1", { hasText: "生产过程追溯" }).waitFor({ timeout: 30000 });
    await tracePage.waitForFunction(() => {
      const value = document.getElementById("state");
      return value && !value.textContent.includes("正在读取");
    }, { timeout: 30000 });
    await tracePage.screenshot({ path: screenshotPath, fullPage: true });
    const traceText = await tracePage.locator("body").innerText();
    return {
      route: "/msService/WOM/produceTask/produceTask/makeTaskList",
      navigationStatus: navigation && navigation.status(),
      selected,
      powerCodeProbe,
      precheck: { status: precheck.status(), body: precheckBody },
      popupUrl: tracePage.url(),
      traceMarkerVisible: traceText.includes(browserBatchNo),
      traceTitleVisible: traceText.includes("生产过程追溯"),
      traceState: await tracePage.locator("#state").innerText(),
      listConsoleErrors,
      traceConsoleErrors,
      traceBadResponses,
      traceRequestFailures,
      processResponses,
      screenshot: screenshotPath,
    };
  } finally {
    await browser.close();
  }
}

async function main() {
  ensureDir(outputPath);
  ensureDir(screenshotPath);
  const api = await request.newContext({ timeout: 60000 });
  const evidence = {
    generatedAt: new Date().toISOString(),
    repoCommit: gitCommit(),
    database: "PostgreSQL",
    marker,
    tenantId,
    persistenceSourceFixture: {
      taskId: sourceTaskId,
      taskExecutionId: sourceTaskExecutionId,
      processExecutionId: sourceProcessExecutionId,
      activityExecutionId: sourceActivityExecutionId,
      tableNo: sourceTableNo,
      batchNo: sourceBatchNo,
      productNo: sourceProductNo,
      ownership: "retained WOM/QCS audit fixture; ProcessAnalysis writes only marker-scoped snapshots",
    },
    browserSourceFixture: {
      taskId: browserTaskId,
      tableNo: browserTableNo,
      batchNo: browserBatchNo,
      ownership: "retained WOM/QCS task returned by the real makeTaskList query grid; read-only trace click",
    },
    status: "FAIL",
    requests: [],
    states: [],
    browser: null,
    verificationSql: stateSql(),
    cleanupSql: cleanupSql(),
    issues: [],
  };
  let exitCode = 1;
  try {
    runSql(cleanupSql());
    evidence.states.push(queryState("before"));
    const ticket = await login(api);
    const precheck = await apiGet(api, ticket,
      `/msService/ProcessAnalysis/analysisParam/analysisParam/isProdprocessView?batchNo=${encodeURIComponent(sourceBatchNo)}`);
    evidence.requests.push(precheck);
    assertCondition(precheck.status === 200 && precheck.body && precheck.body.code === 200, "追溯预检接口失败");
    assertCondition(precheck.body.data.dealRes === true, "追溯预检未识别真实 WOM 批次");

    const inboundLineId = `${marker}_WMS_LINE`;
    const inboundPayload = {
      srcID: `${marker}_WMS_IN`,
      srcTableNo: `${marker}_WMS_IN_NO`,
      directiveNo: sourceTableNo,
      companyCode: tenantId,
      deptCode: "ADP_E2E",
      staffCode: "ADP_E2E",
      userName: username,
      wareCode: `${marker}_WARE`,
      storageDate: new Date().toISOString().slice(0, 10),
      comeType: "produceIn",
      redBlue: "blue",
      detailList: [{
        srcPartId: inboundLineId,
        goodCode: `${marker}_MAT`,
        batchText: sourceBatchNo,
        produceBatchNum: sourceBatchNo,
        placeSetCode: `${marker}_LOC`,
        quantity: 1,
      }],
    };
    const inbound = await apiPost(api, ticket,
      "/msService/public/material/produceInSingles/produceInSingl/generateProductInSingle", inboundPayload);
    evidence.requests.push(inbound);
    assertCondition(inbound.status === 200 && inbound.body && inbound.body.code === 200, "追溯前置完工入库失败");
    const qualityRelease = await apiPost(api, ticket,
      `/msService/material/foreign/foreign/checkProdResult?srcId=${encodeURIComponent(inboundLineId)}&checkResult=${encodeURIComponent("BaseSet_checkResult/qualified")}`,
      {});
    evidence.requests.push(qualityRelease);
    assertCondition(qualityRelease.status === 200 && qualityRelease.body && qualityRelease.body.code === 200, "追溯前置质量释放失败");

    const trace = await apiGet(api, ticket,
      `/msService/ProcessAnalysis/processAnalysis/api/trace?batchNo=${encodeURIComponent(sourceBatchNo)}&productNo=${encodeURIComponent(sourceProductNo)}`);
    evidence.requests.push(trace);
    assertCondition(trace.status === 200 && trace.body && trace.body.code === 200, "追溯聚合接口失败");
    assertCondition(trace.body.data.task.table_no === sourceTableNo, "追溯 API 未返回目标制造指令");
    assertCondition(Number(trace.body.data.summary.processCount) > 0, "追溯 API 未聚合工序");
    assertCondition(Number(trace.body.data.summary.activityCount) > 0, "追溯 API 未聚合活动");
    assertCondition(Number(trace.body.data.summary.qualityEventCount) > 0, "追溯 API 未聚合质量记录");
    assertCondition(Number(trace.body.data.summary.inventoryEventCount) >= 2, "追溯 API 未聚合完工入库/质量释放流水");

    const snapshotRoutes = [
      `/msService/ProcessAnalysis/paramDetail/paramDetail/analysisiTask?taskExeLogId=${sourceTaskExecutionId}`,
      `/msService/ProcessAnalysis/paramStatRec/paramStatRec/manualStatProcess?processId=${sourceProcessExecutionId}`,
      `/msService/ProcessAnalysis/paramStatRec/paramStatRec/manualStatActive?activeId=${sourceActivityExecutionId}`,
      `/msService/ProcessAnalysis/paramDetail/paramDetail/analysisiTask?taskExeLogId=${sourceTaskExecutionId}`,
    ];
    for (const route of snapshotRoutes) {
      const result = await apiGet(api, ticket, route);
      evidence.requests.push(result);
      assertCondition(result.status === 200 && result.body && result.body.code === 200, `接口失败: ${route}`);
    }
    evidence.states.push(queryState("afterManualStatistics"));
    const after = evidence.states[evidence.states.length - 1];
    assertCondition(Number(after.snapshotCount) === 3 && Number(after.batchRows) === 3, "三个快照未真实落库");
    assertCondition(Number(after.taskRevision) === 2, "任务统计幂等修订未递增");
    assertCondition(Number(after.processRevision) === 1 && Number(after.activityRevision) === 1, "工序/活动快照修订异常");
    assertCondition(Number(after.sourceRows) > 0, "追溯源 WOM 工单/执行记录不存在");
    assertCondition(Number(after.wmsDocuments) === 1 && Number(after.wmsTransactions) === 2, "追溯 WMS marker 未真实落库");

    evidence.browser = await browserAcceptance(ticket);
    assertCondition(evidence.browser.navigationStatus === 200, "WOM 制造指令页面未返回 200");
    assertCondition(evidence.browser.precheck.status === 200 && evidence.browser.precheck.body.data.dealRes === true, "真实按钮预检失败");
    assertCondition(evidence.browser.traceMarkerVisible && evidence.browser.traceTitleVisible, "追溯弹窗未渲染目标批次");
    assertCondition(evidence.browser.listConsoleErrors.length === 0, "WOM 点击过程出现 console error");
    assertCondition(evidence.browser.traceConsoleErrors.length === 0, "追溯页面出现 console error");
    assertCondition(evidence.browser.traceBadResponses.length === 0 && evidence.browser.traceRequestFailures.length === 0, "追溯页面存在 network error");
    evidence.status = "PASS";
    exitCode = 0;
  } catch (error) {
    evidence.issues.push(error.stack || error.message);
  } finally {
    if (keepFixture && evidence.status === "PASS") {
      evidence.cleanup = {
        status: "DEFERRED",
        reason: "Shared MES flow snapshots and completion inbound rows retained for final cross-module verification.",
      };
    } else {
      try {
        runSql(cleanupSql());
        evidence.states.push(queryState("afterCleanup"));
        const cleaned = evidence.states[evidence.states.length - 1];
        if (Number(cleaned.snapshotCount) !== 0 || Number(cleaned.wmsDocuments) !== 0 || Number(cleaned.wmsStockRows) !== 0) {
          throw new Error("快照或 WMS marker 清理后仍有残留行");
        }
        evidence.cleanup = { status: "PASS" };
      } catch (cleanupError) {
        evidence.issues.push(`cleanup: ${cleanupError.stack || cleanupError.message}`);
        evidence.cleanup = { status: "FAIL", error: cleanupError.stack || cleanupError.message };
        evidence.status = "FAIL";
        exitCode = 1;
      }
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
