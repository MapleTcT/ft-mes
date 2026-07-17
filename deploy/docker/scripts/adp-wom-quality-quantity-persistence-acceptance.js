#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { execFileSync, spawnSync } = require("child_process");
const { chromium, request } = require("playwright");

const repoRoot = path.resolve(__dirname, "../../..");
const baseUrl = (process.env.ADP_BASE_URL || "http://10.11.100.17:18080").replace(/\/+$/, "");
const browserBaseUrl = (process.env.ADP_BROWSER_BASE_URL || baseUrl).replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD || "123456";
const sshTarget = process.env.ADP_DB_SSH_TARGET || "v6@10.11.100.17";
const dbContainer = process.env.ADP_DB_CONTAINER || "adp-mes-newbase-postgres-1";
const dbName = process.env.ADP_DB_NAME || "adp";
const dbUser = process.env.ADP_DB_USER || "adp";
const headless = process.env.ADP_HEADLESS !== "false";
const nowToken = new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14);
const marker = process.env.ADP_E2E_MARKER || `ADP_E2E_${nowToken}_WOM_BAD_QTY`;
const outputDir = process.env.ADP_OUTPUT_DIR || path.join("/tmp", `adp-wom-bad-quantity-${nowToken}`);
const outputPath = process.env.ADP_WOM_QUALITY_ACCEPTANCE_OUTPUT
  || path.join(outputDir, "acceptance.json");
const screenshotPath = path.join(outputDir, "wom-bad-quantity.png");
const qcsScreenshotPath = path.join(outputDir, "qcs-bad-quantity-context.png");
const womEntryScreenshotPath = path.join(outputDir, "wom-bad-quantity-entry.png");
const qcsEntryScreenshotPath = path.join(outputDir, "qcs-bad-quantity-entry.png");
const defaultChromePath = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";
const browserExecutable = process.env.ADP_BROWSER_EXECUTABLE
  || (fs.existsSync(defaultChromePath) ? defaultChromePath : "");

function ensureParent(filePath) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
}

function shellQuote(value) {
  return `'${String(value).replace(/'/g, "'\\''")}'`;
}

function sqlLiteral(value) {
  return `'${String(value).replace(/'/g, "''")}'`;
}

function runSql(sql) {
  const command = [
    "docker", "exec", "-i", shellQuote(dbContainer), "psql",
    "-U", shellQuote(dbUser), "-d", shellQuote(dbName),
    "-v", "ON_ERROR_STOP=1", "-qAt",
  ].join(" ");
  return execFileSync(
    "ssh",
    ["-o", "BatchMode=yes", "-o", "ConnectTimeout=8", sshTarget, command],
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
    errors.push({ status: response.status(), body: parsed.text.slice(0, 400) });
  }
  throw new Error(`登录失败: ${JSON.stringify(errors)}`);
}

async function apiCall(api, ticket, method, route, data) {
  const options = {
    headers: {
      Authorization: `Bearer ${ticket}`,
      "X-Tenant-Id": "default",
      Accept: "application/json",
      "Content-Type": "application/json;charset=UTF-8",
    },
  };
  if (data !== undefined) options.data = data;
  const response = await api.fetch(`${baseUrl}${route}`, { ...options, method });
  const parsed = await readJson(response);
  return {
    method,
    route,
    payload: data,
    status: response.status(),
    body: parsed.json || parsed.text,
  };
}

function seedSql() {
  return `
BEGIN;
DO $seed$
DECLARE
    source_task public.wom_produce_tasks%ROWTYPE;
    source_exelog public.wom_produce_task_exelog%ROWTYPE;
    source_output public.wom_mat_outpt_records%ROWTYPE;
    source_inspect public.qcs_inspects%ROWTYPE;
    task_id_value bigint;
    exelog_id_value bigint;
    output_id_value bigint;
    inspect_id_value bigint;
BEGIN
    PERFORM pg_advisory_xact_lock(hashtext('adp-wom-quality-quantity-acceptance'));
    SELECT t.* INTO source_task
    FROM public.wom_produce_tasks t
    JOIN public.wom_produce_task_exelog e ON e.task_id = t.id
    JOIN public.wom_mat_outpt_records o ON o.task_exelog_id = e.id
    WHERE COALESCE(t.valid, true) AND COALESCE(e.valid, true) AND COALESCE(o.valid, true)
      AND COALESCE(o.report_num, o.output_num, 0) > 0
    ORDER BY t.id LIMIT 1;
    SELECT e.* INTO source_exelog
    FROM public.wom_produce_task_exelog e
    JOIN public.wom_mat_outpt_records o ON o.task_exelog_id = e.id
    WHERE e.task_id = source_task.id AND COALESCE(e.valid, true) AND COALESCE(o.valid, true)
    ORDER BY e.id LIMIT 1;
    SELECT o.* INTO source_output
    FROM public.wom_mat_outpt_records o
    WHERE o.task_exelog_id = source_exelog.id AND COALESCE(o.valid, true)
      AND COALESCE(o.report_num, o.output_num, 0) > 0
    ORDER BY o.id LIMIT 1;
    SELECT i.* INTO source_inspect
    FROM public.qcs_inspects i
    WHERE COALESCE(i.valid, true)
    ORDER BY i.id LIMIT 1;

    IF source_task.id IS NULL OR source_exelog.id IS NULL
       OR source_output.id IS NULL OR source_inspect.id IS NULL THEN
        RAISE EXCEPTION 'WOM/QCS source fixture is incomplete';
    END IF;

    SELECT COALESCE(MAX(id), 8900000000000000) + 1001 INTO task_id_value
    FROM public.wom_produce_tasks;
    SELECT COALESCE(MAX(id), 8900000000000000) + 1001 INTO exelog_id_value
    FROM public.wom_produce_task_exelog;
    SELECT COALESCE(MAX(id), 8900000000000000) + 1001 INTO output_id_value
    FROM public.wom_mat_outpt_records;
    SELECT COALESCE(MAX(id), 8900000000000000) + 1001 INTO inspect_id_value
    FROM public.qcs_inspects;

    source_task.id := task_id_value;
    source_task.table_info_id := task_id_value;
    source_task.table_no := ${sqlLiteral(`${marker}_TASK`)};
    source_task.produce_batch_num := ${sqlLiteral(`${marker}_BATCH`)};
    source_task.plan_num := 10;
    source_task.finish_num := 10;
    source_task.task_run_state := 'WOM_runState/runing';
    source_task.valid := true;
    source_task.status := 99;
    source_task.create_time := CURRENT_TIMESTAMP;
    source_task.modify_time := CURRENT_TIMESTAMP;
    source_task.inpect_deal_id := inspect_id_value;
    INSERT INTO public.wom_produce_tasks SELECT source_task.*;

    source_exelog.id := exelog_id_value;
    source_exelog.table_info_id := exelog_id_value;
    source_exelog.table_no := ${sqlLiteral(`${marker}_EXELOG`)};
    source_exelog.task_id := task_id_value;
    source_exelog.produce_batch_num := ${sqlLiteral(`${marker}_BATCH`)};
    source_exelog.task_run_state := 'WOM_runState/runing';
    source_exelog.valid := true;
    source_exelog.status := 99;
    source_exelog.create_time := CURRENT_TIMESTAMP;
    source_exelog.modify_time := CURRENT_TIMESTAMP;
    source_exelog.inpect_deal_id := inspect_id_value;
    INSERT INTO public.wom_produce_task_exelog SELECT source_exelog.*;

    source_output.id := output_id_value;
    source_output.table_info_id := output_id_value;
    source_output.table_no := ${sqlLiteral(`${marker}_OUTPUT`)};
    source_output.task_exelog_id := exelog_id_value;
    source_output.report_num := 10;
    source_output.output_num := 10;
    source_output.mat_batch_num := ${sqlLiteral(`${marker}_BATCH`)};
    source_output.produce_batch_num := ${sqlLiteral(`${marker}_BATCH`)};
    source_output.valid := true;
    source_output.status := 99;
    source_output.create_time := CURRENT_TIMESTAMP;
    source_output.modify_time := CURRENT_TIMESTAMP;
    INSERT INTO public.wom_mat_outpt_records SELECT source_output.*;

    source_inspect.id := inspect_id_value;
    source_inspect.table_info_id := inspect_id_value;
    source_inspect.table_no := ${sqlLiteral(`${marker}_INSPECT`)};
    source_inspect.source_id := task_id_value;
    source_inspect.source_table_id := task_id_value;
    source_inspect.batch_code := ${sqlLiteral(`${marker}_BATCH`)};
    source_inspect.valid := true;
    source_inspect.status := 99;
    source_inspect.create_time := CURRENT_TIMESTAMP;
    source_inspect.modify_time := CURRENT_TIMESTAMP;
    INSERT INTO public.qcs_inspects SELECT source_inspect.*;
END $seed$;
COMMIT;
SELECT json_build_object(
    'taskId', (SELECT id::text FROM public.wom_produce_tasks WHERE table_no=${sqlLiteral(`${marker}_TASK`)}),
    'outputId', (SELECT id::text FROM public.wom_mat_outpt_records WHERE table_no=${sqlLiteral(`${marker}_OUTPUT`)}),
    'inspectId', (SELECT id::text FROM public.qcs_inspects WHERE table_no=${sqlLiteral(`${marker}_INSPECT`)})
);
`;
}

function stateSql() {
  return `SELECT json_build_object(
    'reports', (SELECT count(*) FROM wom_quality_quantity_reports q JOIN wom_produce_tasks t ON t.id=q.task_id WHERE t.table_no=${sqlLiteral(`${marker}_TASK`)}),
    'reportEvents', (SELECT count(*) FROM wom_quality_quantity_events e JOIN wom_quality_quantity_reports q ON q.id=e.report_id JOIN wom_produce_tasks t ON t.id=q.task_id WHERE t.table_no=${sqlLiteral(`${marker}_TASK`)}),
    'reportStatus', COALESCE((SELECT q.status FROM wom_quality_quantity_reports q JOIN wom_produce_tasks t ON t.id=q.task_id WHERE t.table_no=${sqlLiteral(`${marker}_TASK`)} LIMIT 1), ''),
    'syncState', COALESCE((SELECT q.wms_sync_state FROM wom_quality_quantity_reports q JOIN wom_produce_tasks t ON t.id=q.task_id WHERE t.table_no=${sqlLiteral(`${marker}_TASK`)} LIMIT 1), ''),
    'reported', COALESCE((SELECT q.reported_quantity FROM wom_quality_quantity_reports q JOIN wom_produce_tasks t ON t.id=q.task_id WHERE t.table_no=${sqlLiteral(`${marker}_TASK`)} LIMIT 1), 0),
    'good', COALESCE((SELECT q.good_quantity FROM wom_quality_quantity_reports q JOIN wom_produce_tasks t ON t.id=q.task_id WHERE t.table_no=${sqlLiteral(`${marker}_TASK`)} LIMIT 1), 0),
    'bad', COALESCE((SELECT q.bad_quantity FROM wom_quality_quantity_reports q JOIN wom_produce_tasks t ON t.id=q.task_id WHERE t.table_no=${sqlLiteral(`${marker}_TASK`)} LIMIT 1), 0),
    'allocations', (SELECT count(*) FROM wms_quality_allocations a WHERE a.source_line_id=(SELECT id::text FROM wom_mat_outpt_records WHERE table_no=${sqlLiteral(`${marker}_OUTPUT`)})),
    'allocationEvents', (SELECT count(*) FROM wms_quality_allocation_events e JOIN wms_quality_allocations a ON a.id=e.allocation_id WHERE a.source_line_id=(SELECT id::text FROM wom_mat_outpt_records WHERE table_no=${sqlLiteral(`${marker}_OUTPUT`)})),
    'allocationStatus', COALESCE((SELECT status FROM wms_quality_allocations a WHERE a.source_line_id=(SELECT id::text FROM wom_mat_outpt_records WHERE table_no=${sqlLiteral(`${marker}_OUTPUT`)}) LIMIT 1), ''),
    'documents', (SELECT count(*) FROM wms_stock_documents WHERE tenant_id='default' AND source_document_id=${sqlLiteral(`${marker}_IN`)}),
    'lineStatus', COALESCE((SELECT l.quality_status FROM wms_stock_document_lines l JOIN wms_stock_documents d ON d.id=l.document_id WHERE d.source_document_id=${sqlLiteral(`${marker}_IN`)} LIMIT 1), ''),
    'lineGood', COALESCE((SELECT l.good_quantity FROM wms_stock_document_lines l JOIN wms_stock_documents d ON d.id=l.document_id WHERE d.source_document_id=${sqlLiteral(`${marker}_IN`)} LIMIT 1), 0),
    'lineBad', COALESCE((SELECT l.bad_quantity FROM wms_stock_document_lines l JOIN wms_stock_documents d ON d.id=l.document_id WHERE d.source_document_id=${sqlLiteral(`${marker}_IN`)} LIMIT 1), 0),
    'onHand', COALESCE((SELECT on_hand_quantity FROM wms_batch_stocks WHERE tenant_id='default' AND material_code=${sqlLiteral(`${marker}_MAT`)} LIMIT 1), 0),
    'available', COALESCE((SELECT available_quantity FROM wms_batch_stocks WHERE tenant_id='default' AND material_code=${sqlLiteral(`${marker}_MAT`)} LIMIT 1), 0),
    'hold', COALESCE((SELECT hold_quantity FROM wms_batch_stocks WHERE tenant_id='default' AND material_code=${sqlLiteral(`${marker}_MAT`)} LIMIT 1), 0)
);`;
}

function queryState(label) {
  return { label, ...JSON.parse(runSql(stateSql())) };
}

function cleanupSql() {
  return `BEGIN;
DELETE FROM wms_stock_documents WHERE tenant_id='default' AND source_document_id=${sqlLiteral(`${marker}_IN`)};
DELETE FROM wms_quality_results WHERE tenant_id='default' AND source_line_id IN (SELECT id::text FROM wom_mat_outpt_records WHERE table_no=${sqlLiteral(`${marker}_OUTPUT`)});
DELETE FROM wms_batch_stocks WHERE tenant_id='default' AND material_code=${sqlLiteral(`${marker}_MAT`)};
DELETE FROM wms_quality_allocations WHERE tenant_id='default' AND source_line_id IN (SELECT id::text FROM wom_mat_outpt_records WHERE table_no=${sqlLiteral(`${marker}_OUTPUT`)});
DELETE FROM wom_quality_quantity_reports WHERE task_id IN (SELECT id FROM wom_produce_tasks WHERE table_no=${sqlLiteral(`${marker}_TASK`)});
DELETE FROM qcs_inspects WHERE table_no=${sqlLiteral(`${marker}_INSPECT`)};
DELETE FROM wom_mat_outpt_records WHERE table_no=${sqlLiteral(`${marker}_OUTPUT`)};
DELETE FROM wom_produce_task_exelog WHERE table_no=${sqlLiteral(`${marker}_EXELOG`)};
DELETE FROM wom_produce_tasks WHERE table_no=${sqlLiteral(`${marker}_TASK`)};
COMMIT;`;
}

function assertCondition(condition, message) {
  if (!condition) throw new Error(message);
}

function numeric(value) {
  return Number(value || 0);
}

async function browserAcceptance(ticket, ids) {
  const launchOptions = { headless };
  if (browserExecutable) launchOptions.executablePath = browserExecutable;
  const browser = await chromium.launch(launchOptions);
  const consoleErrors = [];
  const pageErrors = [];
  const requestFailures = [];
  const badResponses = [];
  try {
    const context = await browser.newContext({
      baseURL: browserBaseUrl,
      ignoreHTTPSErrors: true,
      viewport: { width: 1440, height: 900 },
      extraHTTPHeaders: { Authorization: `Bearer ${ticket}`, "X-Tenant-Id": "default" },
    });
    await context.addCookies([
      { name: "suposTicket", value: ticket, url: browserBaseUrl },
      { name: "SUPOS_TICKET", value: ticket, url: browserBaseUrl },
    ]);
    await context.addInitScript((token) => {
      ["suposTicket", "SUPOS_TICKET", "token"].forEach((key) => {
        window.localStorage.setItem(key, token);
        window.sessionStorage.setItem(key, token);
      });
    }, ticket);

    const page = await context.newPage();
    page.on("console", (message) => {
      if (message.type() === "error") consoleErrors.push(message.text());
    });
    page.on("pageerror", (error) => pageErrors.push(error.message));
    page.on("requestfailed", (requestValue) => requestFailures.push({
      url: requestValue.url(), error: requestValue.failure() && requestValue.failure().errorText,
    }));
    page.on("response", (response) => {
      if (response.status() >= 400) badResponses.push({ url: response.url(), status: response.status() });
    });

    const response = await page.goto(
      `${browserBaseUrl}/msService/WOM/quality-quantity/page?taskId=${encodeURIComponent(ids.taskId)}`,
      { waitUntil: "domcontentloaded", timeout: 120000 }
    );
    await page.locator("h1", { hasText: "不良数量登记" }).waitFor({ timeout: 30000 });
    await page.locator("#taskSelect").waitFor({ timeout: 30000 });
    await page.waitForFunction((taskId) => document.querySelector("#taskSelect").value === taskId, ids.taskId);
    await page.locator("#reportRows tr").first().waitFor({ timeout: 30000 });
    const selectedTaskId = await page.locator("#taskSelect").inputValue();
    const pageText = await page.locator("body").innerText();
    await page.screenshot({ path: screenshotPath, fullPage: true });

    const qcsPage = await context.newPage();
    const qcsResponse = await qcsPage.goto(
      `${browserBaseUrl}/msService/WOM/quality-quantity/page?inspectId=${encodeURIComponent(ids.inspectId)}`,
      { waitUntil: "domcontentloaded", timeout: 120000 }
    );
    await qcsPage.locator("h1", { hasText: "不良数量登记" }).waitFor({ timeout: 30000 });
    await qcsPage.waitForFunction((taskId) => document.querySelector("#taskSelect").value === taskId, ids.taskId);
    const qcsSelectedTaskId = await qcsPage.locator("#taskSelect").inputValue();
    await qcsPage.screenshot({ path: qcsScreenshotPath, fullPage: true });

    const entryPage = await context.newPage();
    const womEntryResponse = await entryPage.goto(
      `${browserBaseUrl}/msService/WOM/produceTask/produceTask/makeTaskList?ADP_ENTRY_CHECK=1`,
      { waitUntil: "commit", timeout: 180000 }
    );
    await entryPage.locator("#btn-badQuantityReport").waitFor({ state: "visible", timeout: 120000 });
    const womEntryText = (await entryPage.locator("#btn-badQuantityReport").innerText()).trim();
    await entryPage.screenshot({ path: womEntryScreenshotPath, fullPage: true });

    const qcsEntryResponse = await entryPage.goto(
      `${browserBaseUrl}/msService/QCS/inspectReport/inspectReport/manuInspReportEdit?ADP_ENTRY_CHECK=1`,
      { waitUntil: "commit", timeout: 180000 }
    );
    await entryPage.locator("#btn-badQuantityReportQcs").waitFor({ state: "visible", timeout: 120000 });
    const qcsEntryText = (await entryPage.locator("#btn-badQuantityReportQcs").innerText()).trim();
    await entryPage.screenshot({ path: qcsEntryScreenshotPath, fullPage: true });

    return {
      route: "/msService/WOM/quality-quantity/page",
      httpStatus: response ? response.status() : null,
      qcsContextStatus: qcsResponse ? qcsResponse.status() : null,
      selectedTaskId,
      qcsSelectedTaskId,
      markerVisible: pageText.includes(marker),
      legacyEntries: {
        wom: { status: womEntryResponse ? womEntryResponse.status() : null, text: womEntryText },
        qcs: { status: qcsEntryResponse ? qcsEntryResponse.status() : null, text: qcsEntryText },
      },
      consoleErrors,
      pageErrors,
      requestFailures,
      badResponses,
      screenshots: [
        screenshotPath,
        qcsScreenshotPath,
        womEntryScreenshotPath,
        qcsEntryScreenshotPath,
      ],
    };
  } finally {
    await browser.close();
  }
}

async function main() {
  ensureParent(outputPath);
  ensureParent(screenshotPath);
  const api = await request.newContext({ timeout: 30000 });
  const evidence = {
    generatedAt: new Date().toISOString(),
    repoCommit: gitCommit(),
    database: "PostgreSQL",
    marker,
    status: "FAIL",
    ids: null,
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
    evidence.ids = JSON.parse(runSql(seedSql()));
    const ticket = await login(api);

    const contextResult = await apiCall(
      api, ticket, "GET",
      `/msService/WOM/quality-quantity/quality-context/${encodeURIComponent(evidence.ids.inspectId)}`
    );
    evidence.requests.push(contextResult);
    assertCondition(contextResult.status === 200 && contextResult.body.code === 200, "QCS 制造任务上下文解析失败");
    assertCondition(contextResult.body.data.task_id === evidence.ids.taskId, "QCS 上下文返回了错误的制造任务");

    const createPayload = {
      requestId: `${marker}_REQUEST`,
      taskId: evidence.ids.taskId,
      sourceOutputId: evidence.ids.outputId,
      badQuantity: 2,
      unitCode: "kg",
      reasonCode: "PROCESS_DEFECT",
      reasonText: `${marker} process deviation`,
    };
    const created = await apiCall(
      api, ticket, "POST", "/msService/WOM/quality-quantity/reports", createPayload
    );
    evidence.requests.push(created);
    assertCondition(created.status === 200 && created.body.code === 200, "不良数量登记接口失败");
    assertCondition(created.body.data.qcs_inspect_id === evidence.ids.inspectId, "登记未关联 QCS 请检记录");
    assertCondition(created.body.data.wms_sync_state === "APPLIED", "登记未同步到 WMS 数量分配");
    evidence.states.push(queryState("afterReport"));

    const inboundPayload = {
      srcID: `${marker}_IN`,
      srcTableNo: `${marker}_IN_NO`,
      directiveNo: `${marker}_TASK`,
      companyCode: "default",
      userName: username,
      staffCode: "ADP_E2E",
      deptCode: "ADP_E2E",
      redBlue: "blue",
      comeType: "produceIn",
      workflowStateSetCode: "productInSingleFlw",
      storageDate: new Date().toISOString().slice(0, 10),
      wareCode: `${marker}_WARE`,
      detailList: [{
        srcPartId: evidence.ids.outputId,
        goodCode: `${marker}_MAT`,
        batchText: `${marker}_BATCH`,
        produceBatchNum: `${marker}_BATCH`,
        placeSetCode: `${marker}_LOC`,
        quantity: 10,
      }],
    };
    const inbound = await apiCall(
      api, ticket, "POST",
      "/msService/public/material/produceInSingles/produceInSingl/generateProductInSingle",
      inboundPayload
    );
    evidence.requests.push(inbound);
    assertCondition(inbound.status === 200 && inbound.body.code === 200, "完工入库接口失败");
    evidence.states.push(queryState("afterInbound"));

    const quality = await apiCall(
      api, ticket, "POST",
      `/msService/material/foreign/foreign/checkProdResult?srcId=${encodeURIComponent(evidence.ids.outputId)}`
        + `&checkResult=${encodeURIComponent("BaseSet_checkResult/qualified")}`,
      {}
    );
    evidence.requests.push(quality);
    assertCondition(quality.status === 200 && quality.body.code === 200, "质检合格回写失败");
    evidence.states.push(queryState("afterQuality"));

    const retry = await apiCall(
      api, ticket, "POST", "/msService/WOM/quality-quantity/reports", createPayload
    );
    evidence.requests.push(retry);
    assertCondition(retry.status === 200 && retry.body.code === 200, "登记幂等重放失败");
    evidence.states.push(queryState("afterRetry"));

    evidence.browser = await browserAcceptance(ticket, evidence.ids);

    const reverse = await apiCall(
      api, ticket, "POST",
      `/msService/WOM/quality-quantity/reports/${encodeURIComponent(created.body.data.id)}/reverse`,
      { version: retry.body.data.version, reason: `${marker} rollback rehearsal` }
    );
    evidence.requests.push(reverse);
    assertCondition(reverse.status === 200 && reverse.body.code === 200, "不良数量冲销失败");
    evidence.states.push(queryState("afterReverse"));

    const afterReport = evidence.states.find((item) => item.label === "afterReport");
    const afterInbound = evidence.states.find((item) => item.label === "afterInbound");
    const afterQuality = evidence.states.find((item) => item.label === "afterQuality");
    const afterRetry = evidence.states.find((item) => item.label === "afterRetry");
    const afterReverse = evidence.states.find((item) => item.label === "afterReverse");
    assertCondition(numeric(afterReport.reports) === 1 && numeric(afterReport.reportEvents) === 2, "登记或事件账本未落库");
    assertCondition(numeric(afterReport.allocations) === 1 && numeric(afterReport.allocationEvents) === 1, "WMS 数量分配未落库");
    assertCondition(numeric(afterInbound.onHand) === 10 && numeric(afterInbound.available) === 0 && numeric(afterInbound.hold) === 10, "待检入库库存分桶错误");
    assertCondition(afterQuality.lineStatus === "PARTIAL" && numeric(afterQuality.lineGood) === 8 && numeric(afterQuality.lineBad) === 2, "良品/不良品行数量错误");
    assertCondition(numeric(afterQuality.available) === 8 && numeric(afterQuality.hold) === 2, "合格回写释放了不良数量");
    assertCondition(numeric(afterRetry.reports) === 1 && numeric(afterRetry.reportEvents) === 2 && numeric(afterRetry.allocationEvents) === 1, "幂等重放产生重复数据");
    assertCondition(afterReverse.reportStatus === "REVERSED" && afterReverse.allocationStatus === "REVERSED", "冲销状态未闭合");
    assertCondition(numeric(afterReverse.available) === 10 && numeric(afterReverse.hold) === 0, "冲销未恢复库存分桶");
    assertCondition(numeric(afterReverse.reportEvents) === 4 && numeric(afterReverse.allocationEvents) === 2, "冲销事件账本不完整");
    assertCondition(evidence.browser.httpStatus === 200 && evidence.browser.qcsContextStatus === 200, "运行页面不可访问");
    assertCondition(evidence.browser.selectedTaskId === evidence.ids.taskId
      && evidence.browser.qcsSelectedTaskId === evidence.ids.taskId
      && evidence.browser.markerVisible, "WOM/QCS 页面上下文未准确回填");
    assertCondition(evidence.browser.legacyEntries.wom.status === 200
      && evidence.browser.legacyEntries.wom.text === "不良数量"
      && evidence.browser.legacyEntries.qcs.status === 200
      && evidence.browser.legacyEntries.qcs.text === "不良数量", "WOM/QCS 旧页面入口未渲染");
    assertCondition(evidence.browser.consoleErrors.length === 0
      && evidence.browser.pageErrors.length === 0
      && evidence.browser.requestFailures.length === 0
      && evidence.browser.badResponses.length === 0, "运行页面存在 console/network 错误");
    evidence.status = "PASS";
    exitCode = 0;
  } catch (error) {
    evidence.issues.push(error.stack || error.message);
  } finally {
    try {
      runSql(cleanupSql());
      evidence.states.push({ label: "afterCleanup", residual: JSON.parse(runSql(`SELECT json_build_object(
        'tasks', (SELECT count(*) FROM wom_produce_tasks WHERE table_no=${sqlLiteral(`${marker}_TASK`)}),
        'reports', (SELECT count(*) FROM wom_quality_quantity_reports WHERE reason_text LIKE ${sqlLiteral(`${marker}%`)}),
        'documents', (SELECT count(*) FROM wms_stock_documents WHERE source_document_id=${sqlLiteral(`${marker}_IN`)})
      );`)) });
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
