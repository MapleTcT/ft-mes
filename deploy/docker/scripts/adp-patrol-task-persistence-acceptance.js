#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { execFileSync } = require("child_process");
const { chromium, request } = require("playwright");

const now = new Date();
const stamp = now.toISOString().replace(/[-:T.Z]/g, "").slice(0, 14);
const action = process.env.ADP_PATROL_TASK_ACTION || "cancel";
if (!new Set(["cancel", "complete", "hidden-danger"]).has(action)) {
  throw new Error(`Unsupported ADP_PATROL_TASK_ACTION: ${action}`);
}
const hiddenDangerMode = action === "hidden-danger";
const marker =
  process.env.ADP_E2E_MARKER ||
  `ADP_E2E_${stamp}_${hiddenDangerMode ? "PATROL_HIDDEN_DANGER" : action === "complete" ? "PATROL_EXECUTION" : "PATROL"}`;
const baseUrl = (process.env.ADP_BASE_URL || "http://10.11.100.17:18080").replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD || "123456";
const headless = process.env.ADP_HEADLESS !== "false";
const pageTimeoutMs = Number(process.env.ADP_PAGE_TIMEOUT_MS || 180000);
const outputDir = process.env.ADP_OUTPUT_DIR || path.join("/tmp", `adp-patrol-persistence-${stamp}`);
const outputPath =
  process.env.ADP_PATROL_PERSISTENCE_OUTPUT || path.join(outputDir, "patrol-persistence-results.json");

const dbSshTarget = process.env.ADP_DB_SSH_TARGET || "v6@10.11.100.17";
const dbContainer = process.env.ADP_DB_CONTAINER || "adp-mes-newbase-postgres-1";
const dbName = process.env.ADP_DB_NAME || "adp";
const dbUser = process.env.ADP_DB_USER || "adp";

const routeId = Number(process.env.ADP_PATROL_ROUTE_ID || 6675485506913104);
const routeCode = process.env.ADP_PATROL_ROUTE_CODE || "ADP_E2E_202607162045_PATROL_ROUTE";
const routeName = process.env.ADP_PATROL_ROUTE_NAME || "ADP_E2E_202607162045_PATROL_ROUTE_UPDATED";
const staffId = Number(process.env.ADP_PATROL_STAFF_ID || 1);
const staffName = process.env.ADP_PATROL_STAFF_NAME || "虚拟人员";

const planCode = `${marker}_PLAN`;
const generationRemark = `${marker}_TASK_GENERATION`;
const planStart = new Date(now.getTime() + 12 * 60 * 60 * 1000);
planStart.setMinutes(0, 0, 0);
const generationStart = new Date(planStart.getTime() - 30 * 60 * 1000);
const generationEnd = new Date(planStart.getTime() + 90 * 60 * 1000);

const planListRoute = "/msService/PATROL/patrolPlan/patrolPlan/potrolPlanList";
const taskListRoute = "/msService/PATROL/patrolTask/potrolTask/potrolTaskList";
const batchChangeRoute = "/msService/PATROL/patrolTask/potrolTask/batchChangeList";
const planSubmitPath = "/msService/PATROL/patrolPlan/patrolPlan/patrolPlan/submit";
const createTaskSubmitPath = "/msService/PATROL/patrolPlan/createTask/createTaskEdit/submit";
const taskQueryPath = "/msService/PATROL/patrolTask/potrolTask/potrolTaskList-query";
const taskStateUpdatePath = "/msService/PATROL/patrolTask/potrolTask/taskStateUpdate";
const cancelledState = "PATROL_taskState/cancelled";
const issuedState = "PATROL_taskState/issued";
const runningState = "PATROL_taskState/running";
const completedState = "PATROL_taskState/completed";
const stateChangeRemark = `${marker}_CANCELLED_BY_UI`;
const executionRemark = `${marker}_RUNNING`;
const completionResult = process.env.ADP_PATROL_COMPLETION_RESULT || (hiddenDangerMode ? "99.99" : "12.34");
const completionConclusion =
  process.env.ADP_PATROL_COMPLETION_CONCLUSION || (hiddenDangerMode ? "异常" : "正常");
const completionConclusionId =
  process.env.ADP_PATROL_COMPLETION_CONCLUSION_ID ||
  (hiddenDangerMode ? "PATROL_realValue/abnormal" : "PATROL_realValue/normal");
const enteringResultListRoute = "/msService/PATROL/patrolTask/potrolTask/enteringResultList";
const enteringResultQueryPath =
  "/msService/PATROL/patrolTask/potrolTask/enteringResultList-query";
const enteringResultSubmitPrefix =
  "/msService/PATROL/patrolTask/potrolTask/enteringResultEdit/";
const resultGridCode = "PATROL_1.0.0_patrolTask_enteringResultEditdg1584600022503";
const abnormalSummaryRoute = "/msService/PATROL/patrolTask/taskDetail/abnormalSummary";
const abnormalSummaryQueryPath = `${abnormalSummaryRoute}-query`;
const createHiddenDangerPath = "/msService/PATROL/patrolTask/taskDetail/createHiddenDanger";
const abnormalGridCode = "PATROL_1.0.0_patrolTask_abnormalSummary_taskDetail_sdg";
const eamRiskRoute = "/msService/EAM/businessConfig/riskHandle/riskRecord";
const eamRiskGridCode = "EAM_1.0.0_businessConfig_riskRecorddg1578550214154";
let activeBrowser = null;

const visibleErrorPattern =
  /(数据库操作异常|系统错误|系统异常|SQLGrammarException|could not extract ResultSet|column .* does not exist|relation .* does not exist|500 INTERNAL|\b[\w.]+Exception(?::|\s+at\b))/i;

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true });
}

function shellQuote(value) {
  return `'${String(value).replace(/'/g, "'\\''")}'`;
}

function sqlLiteral(value) {
  return `'${String(value).replace(/'/g, "''")}'`;
}

function runSql(sql) {
  const remoteCommand = [
    "docker",
    "exec",
    "-i",
    dbContainer,
    "psql",
    "-U",
    dbUser,
    "-d",
    dbName,
    "-v",
    "ON_ERROR_STOP=1",
    "-At",
  ]
    .map(shellQuote)
    .join(" ");
  return execFileSync(
    "ssh",
    [
      "-o",
      "BatchMode=yes",
      "-o",
      "ConnectTimeout=10",
      "-o",
      "ServerAliveInterval=15",
      "-o",
      "ServerAliveCountMax=2",
      dbSshTarget,
      remoteCommand,
    ],
    { input: sql, encoding: "utf8", stdio: ["pipe", "pipe", "pipe"] }
  ).trim();
}

function queryJson(sql) {
  const output = runSql(sql);
  const lines = output.split(/\r?\n/).filter(Boolean);
  return lines.length ? JSON.parse(lines[lines.length - 1]) : null;
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
  for (const data of attempts) {
    const response = await api.post(`${baseUrl}/inter-api/auth/login`, {
      data,
      headers: { Accept: "application/json", "Content-Type": "application/json;charset=UTF-8" },
    });
    const parsed = await readJsonSafe(response);
    const ticket = response.ok() ? findTicket(parsed.json) : null;
    if (ticket) {
      return ticket;
    }
    failures.push({ status: response.status(), body: parsed.text.slice(0, 500) });
  }
  throw new Error(`Login failed: ${JSON.stringify(failures)}`);
}

async function browserApi(page, method, urlPath, payload) {
  return page.evaluate(
    async ({ methodArg, urlPathArg, payloadArg }) => {
      const token =
        window.localStorage.getItem("suposTicket") ||
        window.localStorage.getItem("SUPOS_TICKET") ||
        window.localStorage.getItem("token") ||
        window.sessionStorage.getItem("suposTicket") ||
        window.sessionStorage.getItem("SUPOS_TICKET") ||
        window.sessionStorage.getItem("token") ||
        "";
      const response = await window.fetch(urlPathArg, {
        method: methodArg,
        credentials: "include",
        headers: {
          Accept: "application/json, text/plain, */*",
          Authorization: token ? `Bearer ${token}` : "",
          "Content-Type": "application/json;charset=UTF-8",
          langu_code: "zh_CN",
          "X-Requested-With": "XMLHttpRequest",
        },
        body: payloadArg === undefined ? undefined : JSON.stringify(payloadArg),
      });
      const text = await response.text();
      let json = null;
      try {
        json = JSON.parse(text);
      } catch (_error) {
        json = null;
      }
      return { ok: response.ok, status: response.status, text, json };
    },
    { methodArg: method, urlPathArg: urlPath, payloadArg: payload }
  );
}

async function browserFormApi(page, method, urlPath, payload) {
  return page.evaluate(
    async ({ methodArg, urlPathArg, payloadArg }) => {
      const token =
        window.localStorage.getItem("suposTicket") ||
        window.localStorage.getItem("SUPOS_TICKET") ||
        window.localStorage.getItem("token") ||
        window.sessionStorage.getItem("suposTicket") ||
        window.sessionStorage.getItem("SUPOS_TICKET") ||
        window.sessionStorage.getItem("token") ||
        "";
      const response = await window.fetch(urlPathArg, {
        method: methodArg,
        credentials: "include",
        headers: {
          Accept: "application/json, text/plain, */*",
          Authorization: token ? `Bearer ${token}` : "",
          "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8",
          langu_code: "zh_CN",
          "X-Requested-With": "XMLHttpRequest",
        },
        body: new URLSearchParams(payloadArg).toString(),
      });
      const text = await response.text();
      let json = null;
      try {
        json = JSON.parse(text);
      } catch (_error) {
        json = null;
      }
      return { ok: response.ok, status: response.status, text, json };
    },
    { methodArg: method, urlPathArg: urlPath, payloadArg: payload }
  );
}

function responseData(result) {
  if (!result || !result.json) {
    return null;
  }
  return result.json.data || result.json.result || result.json;
}

async function openBusinessPage(page, route, evidence, label) {
  let response = null;
  let error = null;
  try {
    response = await page.goto(`${baseUrl}${route}`, { waitUntil: "commit", timeout: pageTimeoutMs });
  } catch (navigationError) {
    error = navigationError.message;
  }
  await page.waitForLoadState("domcontentloaded", { timeout: 30000 }).catch(() => {});
  await page.waitForFunction(() => document.body && (document.body.innerText || "").length > 20, null, {
    timeout: 30000,
  }).catch(() => {});
  const body = await page.locator("body").innerText().catch(() => "");
  const item = {
    label,
    route,
    status: response ? response.status() : null,
    navigationError: error,
    visibleError: body.match(visibleErrorPattern)?.[0] || null,
    unresolvedI18nKeys: Array.from(new Set(body.match(/\b(?:ec|PATROL|mobileEAM)\.[\w.]+/g) || [])),
  };
  evidence.navigations.push(item);
  return item;
}

async function captureScreenshot(page, fileName, evidence) {
  const filePath = path.join(outputDir, fileName);
  try {
    await page.screenshot({ path: filePath, fullPage: true, timeout: 30000 });
    evidence.screenshots.push(filePath);
  } catch (error) {
    evidence.screenshotFailures.push({ filePath, error: error.message });
  }
}

function compactHttp(result) {
  return {
    status: result.status,
    ok: result.ok,
    body: result.text.slice(0, 1800),
  };
}

async function compactPageResponse(response) {
  const parsed = await readJsonSafe(response);
  return {
    status: response.status(),
    ok: response.ok(),
    text: parsed.text,
    json: parsed.json,
  };
}

function trackPage(page, evidence, patrolUrlPattern) {
  page.on("console", (message) => {
    if (["error", "warning"].includes(message.type())) {
      evidence.consoleErrors.push({
        type: message.type(),
        text: message.text().slice(0, 1000),
        location: message.location(),
        page: page.url(),
      });
    }
  });
  page.on("pageerror", (error) => {
    evidence.pageErrors.push({
      page: page.url(),
      text: error.message.slice(0, 1000),
      stack: String(error.stack || "").slice(0, 3000),
    });
  });
  page.on("requestfailed", (requestItem) => {
    if (patrolUrlPattern.test(requestItem.url())) {
      evidence.requestFailures.push({
        method: requestItem.method(),
        url: requestItem.url(),
        failure: requestItem.failure() && requestItem.failure().errorText,
      });
    }
  });
  page.on("request", (requestItem) => {
    if (patrolUrlPattern.test(requestItem.url())) {
      evidence.requests.push({
        method: requestItem.method(),
        url: requestItem.url(),
        postData: requestItem.postData() ? requestItem.postData().slice(0, 4000) : null,
      });
    }
  });
  page.on("response", async (response) => {
    if (!patrolUrlPattern.test(response.url())) {
      return;
    }
    let body = "";
    try {
      body = (await response.text()).slice(0, 1800);
    } catch (_error) {
      body = "";
    }
    evidence.responses.push({
      method: response.request().method(),
      url: response.url(),
      status: response.status(),
      body,
    });
  });
}

async function changeTaskState(page, taskId, changeState, remark) {
  const params = new URLSearchParams({
    changeState,
    remark,
    idList: String(taskId),
  });
  return browserApi(page, "GET", `${taskStateUpdatePath}?${params.toString()}`);
}

async function waitForFrame(page, urlPart) {
  const iframe = page.locator(`iframe[src*="${urlPart}"]`).last();
  await iframe.waitFor({ state: "attached", timeout: 30000 });
  const handle = await iframe.elementHandle();
  const frame = handle && (await handle.contentFrame());
  if (!frame) {
    throw new Error(`Expected iframe was not opened: ${urlPart}`);
  }
  await frame.waitForURL((url) => url.toString().includes(urlPart), { timeout: 30000 });
  await frame.waitForLoadState("domcontentloaded", { timeout: 30000 });
  return frame;
}

async function main() {
  ensureDir(outputDir);
  const preexisting = queryJson(
    `select json_build_object('count', count(*)) from public.mp_patrol_plans where code=${sqlLiteral(planCode)};`
  );
  if (Number(preexisting.count) !== 0) {
    throw new Error(`Marker plan already exists: ${planCode}`);
  }

  const api = await request.newContext({ ignoreHTTPSErrors: true });
  const ticket = await login(api);
  await api.dispose();

  const browser = await chromium.launch({ headless });
  activeBrowser = browser;
  const context = await browser.newContext({
    baseURL: baseUrl,
    ignoreHTTPSErrors: true,
    viewport: { width: 1920, height: 1080 },
    extraHTTPHeaders: { Authorization: `Bearer ${ticket}` },
  });
  await context.addCookies([
    { name: "suposTicket", value: ticket, url: baseUrl },
    { name: "SUPOS_TICKET", value: ticket, url: baseUrl },
  ]);
  await context.addInitScript((token) => {
    ["suposTicket", "SUPOS_TICKET", "token"].forEach((key) => {
      window.localStorage.setItem(key, token);
      window.sessionStorage.setItem(key, token);
    });
  }, ticket);

  const page = await context.newPage();
  page.setDefaultTimeout(pageTimeoutMs);
  page.setDefaultNavigationTimeout(pageTimeoutMs);
  const evidence = {
    navigations: [],
    requests: [],
    responses: [],
    consoleErrors: [],
    pageErrors: [],
    requestFailures: [],
    screenshots: [],
    screenshotFailures: [],
  };
  const businessUrlPattern = hiddenDangerMode
    ? /\/msService\/(?:PATROL|EAM)\//
    : /\/msService\/PATROL\//;
  trackPage(page, evidence, businessUrlPattern);

  const planPage = await openBusinessPage(page, planListRoute, evidence, "巡检计划列表");
  await page
    .waitForFunction((expectedRouteName) => (document.body.innerText || "").includes(expectedRouteName), routeName, {
      timeout: 30000,
    })
    .catch(() => {});
  await captureScreenshot(page, "01-patrol-plan-list.png", evidence);

  const planPayload = {
    patrolPlan: {
      code: planCode,
      startTime: planStart.getTime(),
      endTime: planStart.getTime(),
      useTimes: 1,
      potrolType: { id: "PATROL_planType/single" },
      finishTimeType: { id: "PATROL_finishType/none" },
      assignType: { id: "PATROL_assignType/effect" },
      staffAssignType: { id: "PATROL_staffAssign/staff" },
      state: true,
      finishPlan: false,
      planCount: 0,
      workRouteId: {
        id: routeId,
        code: routeCode,
        name: routeName,
        patrolType: { id: "PATROL_routeType/eam" },
      },
      patrolPlanStaffIdmultiselectIDs: String(staffId),
      patrolPlanStaffIdmultiselectNames: staffName,
      patrolPlanStaffIdAddIds: `${staffId},`,
      patrolPlanStaffIdDeleteIds: "",
      patrolPlanRoleIdmultiselectIDs: "",
      patrolPlanRoleIdmultiselectNames: "",
    },
    viewCode: "PATROL_1.0.0_patrolPlan_patrolPlan",
    modelName: "PATROLPatrolPlan",
    operateType: "save",
    dgList: {},
    dgDeletedIds: {},
  };
  const planResult = await browserApi(page, "POST", planSubmitPath, planPayload);
  const planData = responseData(planResult);
  const planId = planData && Number(planData.id);
  if (!planResult.ok || !planId || visibleErrorPattern.test(planResult.text)) {
    throw new Error(`Plan submit failed: ${JSON.stringify(compactHttp(planResult))}`);
  }

  const createTaskPayload = {
    createTask: {
      startTime: generationStart.getTime(),
      endTime: generationEnd.getTime(),
      remark: generationRemark,
      createPlanIdPatrolPlanmultiselectIDs: String(planId),
      createPlanIdPatrolPlanmultiselectNames: planCode,
      createPlanIdPatrolPlanAddIds: `${planId},`,
      createPlanIdPatrolPlanDeleteIds: "",
    },
    viewCode: "PATROL_1.0.0_patrolPlan_createTaskEdit",
    modelName: "PATROLCreateTask",
    operateType: "save",
    dgList: {},
    dgDeletedIds: {},
  };
  const generationResult = await browserApi(page, "POST", createTaskSubmitPath, createTaskPayload);
  const generationData = responseData(generationResult);
  const generationId = generationData && Number(generationData.id);
  if (!generationResult.ok || !generationId || visibleErrorPattern.test(generationResult.text)) {
    throw new Error(`Task generation failed: ${JSON.stringify(compactHttp(generationResult))}`);
  }

  const taskPage = await openBusinessPage(page, taskListRoute, evidence, "巡检任务列表");
  const taskQueryResponsePromise = page.waitForResponse(
    (response) => response.url().endsWith(taskQueryPath) && response.request().method() === "POST",
    { timeout: 30000 }
  );
  await page.locator('button[data-id="query"]').click();
  const taskQueryResult = await compactPageResponse(await taskQueryResponsePromise);
  const taskMarkerVisibleInGrid = await page
    .waitForFunction((expectedPlanCode) => (document.body.innerText || "").includes(expectedPlanCode), planCode, {
      timeout: 30000,
    })
    .then(() => true)
    .catch(() => false);
  await captureScreenshot(page, "02-patrol-task-list.png", evidence);
  if (!taskQueryResult.ok || visibleErrorPattern.test(taskQueryResult.text)) {
    throw new Error(`Task list query failed: ${JSON.stringify(compactHttp(taskQueryResult))}`);
  }

  const taskIdentity = queryJson(`
select json_build_object('id', t.id, 'tableNo', t.table_no)
from public.mp_potrol_tasks t
where t.patrol_plan_id=${planId}
order by t.id desc
limit 1;
`);
  const taskId = taskIdentity && Number(taskIdentity.id);
  const taskTableNo = taskIdentity && taskIdentity.tableNo;
  if (!taskId || !taskTableNo) {
    throw new Error(`Generated task identity was not persisted for plan ${planId}`);
  }

  let batchPage = null;
  let cancelledTaskPage = null;
  let cancelledQueryResult = null;
  let taskStateChangeResult = null;
  let taskCancelledVisibleInGrid = false;
  let enteringResultPage = null;
  let enteringResultQueryResult = null;
  let completionResultResponse = null;
  let parentRefreshResult = null;
  let completedTaskPage = null;
  let completedQueryResult = null;
  let taskCompletedVisibleInGrid = false;
  let executionEditor = null;
  let executionEditorState = null;
  let abnormalSummaryPage = null;
  let abnormalSummaryQueryResult = null;
  let abnormalSelection = null;
  let hiddenDangerConfirmText = null;
  let hiddenDangerClientState = null;
  let hiddenDangerResult = null;
  let hiddenDangerRequest = null;
  let hiddenDangerPersistence = null;
  let hiddenDangerReuseResult = null;
  let hiddenDangerReusePersistence = null;
  let eamRiskPage = null;
  let eamRiskVisible = false;
  let eamRiskRow = null;
  let eamRiskScriptOrder = null;
  const stateTransitions = [];

  if (action === "cancel") {
    batchPage = await openBusinessPage(page, batchChangeRoute, evidence, "巡检任务批量变更");
    await page.waitForFunction(
      (expectedTableNo) => (document.body.innerText || "").includes(expectedTableNo),
      taskTableNo,
      { timeout: 30000 }
    );
    await captureScreenshot(page, "03-patrol-batch-change-before.png", evidence);

    const taskRow = page
      .getByText(taskTableNo, { exact: true })
      .first()
      .locator("xpath=ancestor::div[contains(@class, 'sup-datagrid-row')]");
    await taskRow.locator("label.ant-checkbox-wrapper").click();
    await page.getByText("状态变更", { exact: true }).click();
    const stateFrame = await waitForFrame(page, "patrolStateEdit");
    await stateFrame.locator(".ant-select").first().click();
    await page.getByText("已取消", { exact: true }).last().click();
    await stateFrame.locator("textarea").fill(stateChangeRemark);
    await captureScreenshot(page, "04-patrol-state-change-cancel.png", evidence);

    const stateChangeResponsePromise = page.waitForResponse(
      (response) => response.url().includes(taskStateUpdatePath) && response.request().method() === "GET",
      { timeout: 30000 }
    );
    await page.getByRole("button", { name: "保存", exact: true }).last().click();
    taskStateChangeResult = await compactPageResponse(await stateChangeResponsePromise);
    if (!taskStateChangeResult.ok || !taskStateChangeResult.text.includes("SUCCESS")) {
      throw new Error(`Task state change failed: ${JSON.stringify(compactHttp(taskStateChangeResult))}`);
    }
    stateTransitions.push({ state: cancelledState, remark: stateChangeRemark, ...compactHttp(taskStateChangeResult) });

    cancelledTaskPage = await openBusinessPage(page, taskListRoute, evidence, "巡检任务取消结果");
    const cancelledQueryResponsePromise = page.waitForResponse(
      (response) => response.url().endsWith(taskQueryPath) && response.request().method() === "POST",
      { timeout: 30000 }
    );
    await page.locator('button[data-id="query"]').click();
    cancelledQueryResult = await compactPageResponse(await cancelledQueryResponsePromise);
    taskCancelledVisibleInGrid = await page
      .waitForFunction(
        ({ expectedTableNo, expectedStateText }) => {
          const rows = Array.from(document.querySelectorAll(".sup-datagrid-row"));
          return rows.some((row) => {
            const text = row.innerText || "";
            return text.includes(expectedTableNo) && text.includes(expectedStateText);
          });
        },
        { expectedTableNo: taskTableNo, expectedStateText: "已取消" },
        { timeout: 30000 }
      )
      .then(() => true)
      .catch(() => false);
    await captureScreenshot(page, "05-patrol-task-cancelled.png", evidence);
    if (!cancelledQueryResult.ok || visibleErrorPattern.test(cancelledQueryResult.text)) {
      throw new Error(`Cancelled task query failed: ${JSON.stringify(compactHttp(cancelledQueryResult))}`);
    }
  } else {
    for (const transition of [
      { state: issuedState, remark: `${marker}_ISSUED` },
      { state: runningState, remark: executionRemark },
    ]) {
      const result = await changeTaskState(page, taskId, transition.state, transition.remark);
      if (!result.ok || !result.text.includes("SUCCESS")) {
        throw new Error(`Task transition failed: ${JSON.stringify(compactHttp(result))}`);
      }
      stateTransitions.push({ ...transition, ...compactHttp(result) });
    }

    enteringResultPage = await openBusinessPage(page, enteringResultListRoute, evidence, "巡检结果录入列表");
    const enteringQueryResponsePromise = page.waitForResponse(
      (response) => response.url().endsWith(enteringResultQueryPath) && response.request().method() === "POST",
      { timeout: 30000 }
    );
    await page.locator('button[data-id="query"]').click();
    enteringResultQueryResult = await compactPageResponse(await enteringQueryResponsePromise);
    if (!enteringResultQueryResult.ok || visibleErrorPattern.test(enteringResultQueryResult.text)) {
      throw new Error(`Entering-result query failed: ${JSON.stringify(compactHttp(enteringResultQueryResult))}`);
    }
    await page.waitForFunction(
      (expectedTableNo) => (document.body.innerText || "").includes(expectedTableNo),
      taskTableNo,
      { timeout: 30000 }
    );
    await captureScreenshot(page, "03-patrol-entering-result-list.png", evidence);

    const selectedTask = await page.evaluate(
      ({ gridCode, expectedTaskId }) => {
        const api = window.ReactAPI.getComponentAPI("SupDataGrid").APIs(gridCode);
        const rows = api.getDatagridData();
        const rowIndex = rows.findIndex((row) => Number(row.id) === expectedTaskId);
        const selected = rowIndex >= 0 ? api.setSelecteds(String(rowIndex)) : [];
        return selected.map((row) => ({ id: Number(row.id), tableNo: row.tableNo }));
      },
      {
        gridCode: "PATROL_1.0.0_patrolTask_enteringResultList_potrolTask_sdg",
        expectedTaskId: taskId,
      }
    );
    if (selectedTask.length !== 1 || selectedTask[0].id !== taskId) {
      throw new Error(`PATROL execution task selection failed: ${JSON.stringify(selectedTask)}`);
    }
    const executionEditorPromise = context.waitForEvent("page", { timeout: 30000 });
    await page.locator("#btn-entr").click();
    executionEditor = await executionEditorPromise;
    executionEditor.setDefaultTimeout(pageTimeoutMs);
    executionEditor.setDefaultNavigationTimeout(pageTimeoutMs);
    trackPage(executionEditor, evidence, businessUrlPattern);
    await executionEditor.waitForLoadState("domcontentloaded", { timeout: 30000 });
    await executionEditor.waitForFunction(
      ({ expectedTableNo, gridCode }) => {
        const bodyReady = (document.body.innerText || "").includes(expectedTableNo);
        const group = window.ReactAPI && window.ReactAPI.getComponentAPI("SupDataGrid");
        const api = group && typeof group.APIs === "function" ? group.APIs(gridCode) : null;
        return bodyReady && api && api.getDatagridData().length > 0;
      },
      { expectedTableNo: taskTableNo, gridCode: resultGridCode },
      { timeout: 30000 }
    );

    executionEditorState = await executionEditor.evaluate((gridCode) => {
      const api = window.ReactAPI.getComponentAPI("SupDataGrid").APIs(gridCode);
      return {
        judgeType: typeof window.judge,
        judgeInstalled: window.__ADP_PATROL_RESULT_JUDGE_INSTALLED__ === true,
        detailCount: api.getDatagridData().length,
      };
    }, resultGridCode);
    if (executionEditorState.judgeType !== "function" || !executionEditorState.judgeInstalled) {
      throw new Error(`PATROL result judge did not load: ${JSON.stringify(executionEditorState)}`);
    }

    for (let rowIndex = 0; rowIndex < executionEditorState.detailCount; rowIndex += 1) {
      const row = executionEditor.locator(".sup-datagrid-row").nth(rowIndex + 1);
      const resultCell = row.locator('[data-key="concluse"]');
      await resultCell.click();
      const resultInput = resultCell.locator('input[type="text"]');
      await resultInput.fill(completionResult);
      await resultInput.press("Tab");

      const conclusionCell = row.locator('[data-key="realValue.value"]');
      await conclusionCell.click();
      await conclusionCell.locator(".ant-select").click();
      // Legacy SupSelect positions its popup after 500 ms; selecting earlier unmounts its anchor.
      await executionEditor.waitForTimeout(650);
      await executionEditor
        .locator("li.ant-select-dropdown-menu-item")
        .filter({ hasText: completionConclusion })
        .last()
        .click();
    }

    executionEditorState.rows = await executionEditor.evaluate((gridCode) => {
      const api = window.ReactAPI.getComponentAPI("SupDataGrid").APIs(gridCode);
      return api.getDatagridData().map((row) => ({
        id: row.id,
        concluse: row.concluse,
        realValueId: row.realValue && row.realValue.id,
      }));
    }, resultGridCode);
    await captureScreenshot(executionEditor, "04-patrol-entering-result-before-save.png", evidence);

    const parentRefreshResponsePromise = page.waitForResponse(
      (response) => response.url().endsWith(enteringResultQueryPath) && response.request().method() === "POST",
      { timeout: 30000 }
    );
    const completionResponsePromise = executionEditor.waitForResponse(
      (response) => {
        const url = new URL(response.url());
        return (
          response.request().method() === "POST" &&
          url.pathname.startsWith(enteringResultSubmitPrefix) &&
          (url.pathname.endsWith("/save") || url.pathname.endsWith("/submit"))
        );
      },
      { timeout: 30000 }
    );
    await executionEditor.getByRole("button", { name: "保存", exact: true }).click();
    completionResultResponse = await compactPageResponse(await completionResponsePromise);
    if (!completionResultResponse.ok || visibleErrorPattern.test(completionResultResponse.text)) {
      throw new Error(`PATROL completion save failed: ${JSON.stringify(compactHttp(completionResultResponse))}`);
    }
    parentRefreshResult = await compactPageResponse(await parentRefreshResponsePromise);
    if (!parentRefreshResult.ok || visibleErrorPattern.test(parentRefreshResult.text)) {
      throw new Error(`PATROL parent refresh failed: ${JSON.stringify(compactHttp(parentRefreshResult))}`);
    }
    await page.waitForTimeout(500);
    await executionEditor.waitForTimeout(1500).catch(() => {});

    completedTaskPage = await openBusinessPage(page, taskListRoute, evidence, "巡检任务完成结果");
    const completedQueryResponsePromise = page.waitForResponse(
      (response) => response.url().endsWith(taskQueryPath) && response.request().method() === "POST",
      { timeout: 30000 }
    );
    await page.locator('button[data-id="query"]').click();
    completedQueryResult = await compactPageResponse(await completedQueryResponsePromise);
    taskCompletedVisibleInGrid = await page
      .waitForFunction(
        ({ expectedTableNo, expectedStateText }) => {
          const rows = Array.from(document.querySelectorAll(".sup-datagrid-row"));
          return rows.some((row) => {
            const text = row.innerText || "";
            return text.includes(expectedTableNo) && text.includes(expectedStateText);
          });
        },
        { expectedTableNo: taskTableNo, expectedStateText: "已完成" },
        { timeout: 30000 }
      )
      .then(() => true)
      .catch(() => false);
    await captureScreenshot(page, "05-patrol-task-completed.png", evidence);
    if (!completedQueryResult.ok || visibleErrorPattern.test(completedQueryResult.text)) {
      throw new Error(`Completed task query failed: ${JSON.stringify(compactHttp(completedQueryResult))}`);
    }

    if (hiddenDangerMode) {
      const taskDetailIdentity = queryJson(`
select json_build_object('id', id)
from public.mp_task_details
where patrol_task=${taskId}
order by id
limit 1;
`);
      const taskDetailId = taskDetailIdentity && Number(taskDetailIdentity.id);
      if (!taskDetailId) {
        throw new Error(`Completed PATROL task has no detail row: ${taskId}`);
      }

      abnormalSummaryPage = await openBusinessPage(
        page,
        abnormalSummaryRoute,
        evidence,
        "巡检异常汇总"
      );
      const abnormalQueryResponsePromise = page.waitForResponse(
        (response) =>
          response.url().endsWith(abnormalSummaryQueryPath) &&
          response.request().method() === "POST",
        { timeout: 30000 }
      );
      await page.locator('button[data-id="query"]').click();
      abnormalSummaryQueryResult = await compactPageResponse(await abnormalQueryResponsePromise);
      if (!abnormalSummaryQueryResult.ok || visibleErrorPattern.test(abnormalSummaryQueryResult.text)) {
        throw new Error(
          `Abnormal-summary query failed: ${JSON.stringify(compactHttp(abnormalSummaryQueryResult))}`
        );
      }
      await page.waitForFunction(
        ({ expectedTableNo, gridCode }) => {
          const group = window.ReactAPI && window.ReactAPI.getComponentAPI("SupDataGrid");
          const api = group && typeof group.APIs === "function" ? group.APIs(gridCode) : null;
          const rows = api ? api.getDatagridData() : [];
          return rows.some((row) => row.patrolTask && row.patrolTask.tableNo === expectedTableNo);
        },
        { expectedTableNo: taskTableNo, gridCode: abnormalGridCode },
        { timeout: 30000 }
      );

      const abnormalRow = page
        .locator("div.sup-datagrid-row")
        .filter({ hasText: taskTableNo })
        .first();
      await abnormalRow.locator("label.ant-checkbox-wrapper").click();
      await page.waitForFunction(
        ({ gridCode, expectedDetailId }) => {
          const api = window.ReactAPI.getComponentAPI("SupDataGrid").APIs(gridCode);
          return api.getSelecteds().some((row) => Number(row.id) === expectedDetailId);
        },
        { gridCode: abnormalGridCode, expectedDetailId: taskDetailId },
        { timeout: 10000 }
      );
      abnormalSelection = await page.evaluate((gridCode) => {
        const api = window.ReactAPI.getComponentAPI("SupDataGrid").APIs(gridCode);
        return api.getSelecteds().map((row) => ({
          id: Number(row.id),
          isFault: row.isFault === true,
          tableNo: row.patrolTask && row.patrolTask.tableNo,
          cid: Number(row.cid),
        }));
      }, abnormalGridCode);
      if (
        abnormalSelection.length !== 1 ||
        abnormalSelection[0].id !== taskDetailId ||
        abnormalSelection[0].isFault
      ) {
        throw new Error(`PATROL abnormal-detail selection failed: ${JSON.stringify(abnormalSelection)}`);
      }
      await captureScreenshot(page, "06-patrol-abnormal-before-hidden-danger.png", evidence);

      await page.getByText("生成隐患单", { exact: true }).click();
      const confirmModal = page
        .locator(".ant-modal.api-confirm")
        .filter({ hasText: "是否生成隐患单" })
        .last();
      await confirmModal.waitFor({ state: "visible", timeout: 30000 });
      hiddenDangerConfirmText = (await confirmModal.innerText()).replace(/\s+/g, " ").trim();
      const confirmButton = confirmModal
        .getByRole("button", { name: /确\s*认/ })
        .last();
      hiddenDangerClientState = await page.evaluate((gridCode) => {
        const group = window.ReactAPI && window.ReactAPI.getComponentAPI("SupDataGrid");
        const api = group && typeof group.APIs === "function" ? group.APIs(gridCode) : null;
        return {
          ajaxHelperType: typeof window._postAsyncWithLoading,
          jqueryType: typeof window.$,
          userCompanyId: Number(window.ReactAPI && window.ReactAPI.getUserInfo().company.id),
          selected: api
            ? api.getSelecteds().map((row) => ({
                id: Number(row.id),
                cid: Number(row.cid),
                isFault: row.isFault === true,
              }))
            : [],
        };
      }, abnormalGridCode);
      hiddenDangerClientState.confirmButton = await confirmButton.evaluate((button) => ({
        text: button.innerText,
        className: button.className,
        disabled: button.disabled,
      }));
      await captureScreenshot(page, "06a-patrol-hidden-danger-confirm.png", evidence);
      const hiddenDangerNetworkOutcomePromise = Promise.allSettled([
        page.waitForRequest(
          (requestItem) =>
            requestItem.url().endsWith(createHiddenDangerPath) && requestItem.method() === "POST",
          { timeout: 30000 }
        ),
        page.waitForResponse(
          (response) =>
            response.url().endsWith(createHiddenDangerPath) && response.request().method() === "POST",
          { timeout: 30000 }
        ),
      ]);
      await confirmButton.click();
      const [requestOutcome, responseOutcome] = await hiddenDangerNetworkOutcomePromise;
      if (requestOutcome.status === "rejected" || responseOutcome.status === "rejected") {
        hiddenDangerClientState.afterConfirm = await page.evaluate(() => ({
          body: (document.body.innerText || "").replace(/\s+/g, " ").slice(0, 1800),
          visibleConfirmCount: Array.from(document.querySelectorAll(".ant-modal.api-confirm")).filter(
            (item) => item.offsetParent !== null
          ).length,
          loadingCount: document.querySelectorAll(".ant-spin-spinning").length,
        }));
        hiddenDangerClientState.consoleErrors = evidence.consoleErrors.slice(-10);
        hiddenDangerClientState.pageErrors = evidence.pageErrors.slice(-10);
        throw new Error(
          `Hidden-danger UI did not issue a request: ${JSON.stringify(hiddenDangerClientState)}`
        );
      }
      const hiddenDangerRawRequest = requestOutcome.value;
      hiddenDangerRequest = {
        method: hiddenDangerRawRequest.method(),
        url: hiddenDangerRawRequest.url(),
        postData: hiddenDangerRawRequest.postData(),
      };
      hiddenDangerResult = await compactPageResponse(responseOutcome.value);
      const hiddenDangerData = responseData(hiddenDangerResult);
      if (
        !hiddenDangerResult.ok ||
        !hiddenDangerResult.json ||
        hiddenDangerResult.json.code !== 200 ||
        !hiddenDangerData ||
        Number(hiddenDangerData.createdCount) !== 1 ||
        visibleErrorPattern.test(hiddenDangerResult.text)
      ) {
        throw new Error(`Hidden-danger creation failed: ${JSON.stringify(compactHttp(hiddenDangerResult))}`);
      }

      hiddenDangerPersistence = queryJson(`
select json_build_object(
  'detail', (
    select json_build_object(
      'id', d.id,
      'isFault', d.is_fault,
      'faultId', d.fault_id,
      'faultTableNo', d.fault_table_no
    )
    from public.mp_task_details d
    where d.id=${taskDetailId}
  ),
  'risk', (
    select json_build_object(
      'id', r.id,
      'tableNo', r.table_no,
      'status', r.status,
      'valid', r.valid,
      'version', r.version,
      'riskMode', r.risk_mode,
      'riskSource', r.risk_source,
      'riskContent', r.risk_content,
      'finder', r.finder,
      'findTime', r.find_time,
      'createTime', r.create_time
    )
    from public.ses_hrm_riskhandles r
    where r.id=(select d.fault_id from public.mp_task_details d where d.id=${taskDetailId})
  ),
  'riskCount', (
    select count(*)
    from public.ses_hrm_riskhandles r
    where r.id=(select d.fault_id from public.mp_task_details d where d.id=${taskDetailId})
  )
);
`);
      const riskId = hiddenDangerPersistence && hiddenDangerPersistence.risk
        ? Number(hiddenDangerPersistence.risk.id)
        : null;
      const riskTableNo = hiddenDangerPersistence && hiddenDangerPersistence.risk
        ? hiddenDangerPersistence.risk.tableNo
        : null;
      if (!riskId || !riskTableNo) {
        throw new Error(`Hidden-danger risk row was not persisted: ${JSON.stringify(hiddenDangerPersistence)}`);
      }

      hiddenDangerReuseResult = await browserFormApi(page, "POST", createHiddenDangerPath, {
        ids: `${taskDetailId},`,
      });
      hiddenDangerReusePersistence = queryJson(`
select json_build_object(
  'riskCount', (select count(*) from public.ses_hrm_riskhandles where id=${riskId}),
  'faultId', (select fault_id from public.mp_task_details where id=${taskDetailId}),
  'faultTableNo', (select fault_table_no from public.mp_task_details where id=${taskDetailId})
);
`);

      eamRiskPage = await openBusinessPage(page, eamRiskRoute, evidence, "EAM 风险台账");
      eamRiskVisible = await page
        .waitForFunction(
          ({ expectedRiskId, gridCode }) => {
            const group = window.ReactAPI && window.ReactAPI.getComponentAPI("SupDataGrid");
            const api = group && typeof group.APIs === "function" ? group.APIs(gridCode) : null;
            const rows = api ? api.getDatagridData() : [];
            return rows.some((row) => Number(row.id) === expectedRiskId);
          },
          { expectedRiskId: riskId, gridCode: eamRiskGridCode },
          { timeout: 30000 }
        )
        .then(() => true)
        .catch(() => false);
      eamRiskRow = await page.evaluate(
        ({ expectedRiskId, gridCode }) => {
          const group = window.ReactAPI && window.ReactAPI.getComponentAPI("SupDataGrid");
          const api = group && typeof group.APIs === "function" ? group.APIs(gridCode) : null;
          const rows = api ? api.getDatagridData() : [];
          const row = rows.find((candidate) => Number(candidate.id) === expectedRiskId);
          return row
            ? {
                id: row.id,
                riskTableNo: row.riskTableNo,
                riskSource: row.riskSource,
                riskContent: row.riskContent,
              }
            : null;
        },
        { expectedRiskId: riskId, gridCode: eamRiskGridCode }
      );
      eamRiskScriptOrder = await page.evaluate(() => {
        const sources = Array.from(document.scripts).map((script) => script.src || "");
        const sesgisIndex = sources.findIndex((source) => source.includes("vendors.sesgis.js"));
        const editIndex = sources.findIndex((source) => /\/scripts\/edit\.js(?:\?|$)/.test(source));
        return { sesgisIndex, editIndex, sources };
      });
      await captureScreenshot(page, "07-eam-risk-ledger.png", evidence);
    }
  }

  const statePersistence = queryJson(`
select json_build_object(
  'id', id,
  'tableNo', table_no,
  'taskState', task_state,
  'remark', remark,
  'version', version,
  'valid', valid,
  'actualStartTime', actual_start_time,
  'actualEndTime', actual_end_time,
  'completeStaff', complete_staff
)
from public.mp_potrol_tasks
where id=${taskId};
`);

  const persistenceSql = `
select json_build_object(
  'plan', (
    select json_build_object(
      'id', p.id, 'code', p.code, 'valid', p.valid, 'workRouteId', p.work_route_id,
      'state', p.state, 'finishPlan', p.finish_plan, 'planCount', p.plan_count,
      'startTime', p.start_time, 'nextTime', p.next_time
    ) from public.mp_patrol_plans p where p.id=${planId}
  ),
  'planStaffCount', (
    select count(*) from public.mp_plan_staffs s
    where s.patrol_plan=${planId} and s.staff_id=${staffId} and s.valid=true
  ),
  'generation', (
    select json_build_object('id', c.id, 'valid', c.valid, 'remark', c.remark, 'startTime', c.start_time, 'endTime', c.end_time)
    from public.mp_create_tasks c where c.id=${generationId}
  ),
  'task', (
    select json_build_object(
      'id', t.id, 'valid', t.valid, 'tableNo', t.table_no, 'taskState', t.task_state, 'remark', t.remark,
      'patrolPlan', t.patrol_plan, 'patrolPlanId', t.patrol_plan_id, 'workRoute', t.work_route,
      'actualStartTime', t.actual_start_time, 'actualEndTime', t.actual_end_time,
      'completeStaff', t.complete_staff
    ) from public.mp_potrol_tasks t where t.patrol_plan_id=${planId} order by t.id desc limit 1
  ),
  'detail', (
    select json_build_object(
      'id', d.id, 'valid', d.valid, 'version', d.version,
      'taskDetailState', d.task_detail_state, 'patrolTask', d.patrol_task,
      'workItemId', d.work_item_id, 'content', d.content,
      'concluse', d.concluse, 'realValue', d.real_value,
      'completeUser', d.complete_user, 'completeDate', d.complete_date,
      'isFault', d.is_fault, 'faultId', d.fault_id, 'faultTableNo', d.fault_table_no
    )
    from public.mp_task_details d
    where d.patrol_task=(select t.id from public.mp_potrol_tasks t where t.patrol_plan_id=${planId} order by t.id desc limit 1)
    order by d.id limit 1
  ),
  'planAssociationMismatchCount', (
    select count(*) from public.mp_potrol_tasks t
    where t.patrol_plan_id=${planId} and t.patrol_plan is distinct from t.patrol_plan_id
  ),
  'detailLifecycleNullCount', (
    select count(*) from public.mp_task_details d
    where d.patrol_task=(select t.id from public.mp_potrol_tasks t where t.patrol_plan_id=${planId} order by t.id desc limit 1)
      and (d.valid is null or d.version is null or d.task_detail_state is null)
  ),
  'detailCompletionNullCount', (
    select count(*) from public.mp_task_details d
    where d.patrol_task=(select t.id from public.mp_potrol_tasks t where t.patrol_plan_id=${planId} order by t.id desc limit 1)
      and (d.concluse is null or d.real_value is null or d.complete_user is null or d.complete_date is null)
  )
);
`;
  const persistence = queryJson(persistenceSql);
  const detail = persistence && persistence.detail;
  const assertions = {
    planPersisted: Boolean(persistence && persistence.plan && persistence.plan.code === planCode),
    planStaffPersisted: Number(persistence && persistence.planStaffCount) === 1,
    generationPersisted: Boolean(
      persistence && persistence.generation && persistence.generation.remark === generationRemark
    ),
    taskPersisted: Boolean(taskId),
    taskPlanAssociationPersisted: Boolean(
      taskId &&
        Number(persistence.task.patrolPlan) === planId &&
        Number(persistence.task.patrolPlanId) === planId &&
        Number(persistence.task.workRoute) === routeId
    ),
    taskListContainsMarker: taskQueryResult.text.includes(planCode),
    taskMarkerVisibleInGrid,
    detailPersisted: Boolean(detail && Number(detail.patrolTask) === taskId),
    noPlanAssociationMismatch: Number(persistence && persistence.planAssociationMismatchCount) === 0,
    noNullDetailLifecycle: Number(persistence && persistence.detailLifecycleNullCount) === 0,
    planPageNoVisibleError: !planPage.visibleError,
    taskPageNoVisibleError: !taskPage.visibleError,
    noPatrolRequestFailures: evidence.requestFailures.length === 0,
    noLegacyI18nParameterErrors: !evidence.consoleErrors.some((item) => item.text.includes("请将参数放在数组里")),
    noConsoleErrors: !evidence.consoleErrors.some((item) => item.type === "error"),
    noPageErrors: evidence.pageErrors.length === 0,
  };
  if (action === "cancel") {
    Object.assign(assertions, {
      taskStateChangeResponseSuccess:
        taskStateChangeResult.ok && taskStateChangeResult.text.includes("SUCCESS"),
      taskCancelledPersisted: Boolean(statePersistence && statePersistence.taskState === cancelledState),
      taskStateRemarkPersisted: Boolean(statePersistence && statePersistence.remark === stateChangeRemark),
      taskCancelledVisibleInGrid,
      detailDefaultsPersisted: Boolean(
        detail &&
          detail.valid === true &&
          Number(detail.version) === 0 &&
          detail.taskDetailState === "PATROL_taskDetailState/pending"
      ),
      batchPageNoVisibleError: !batchPage.visibleError,
      cancelledTaskPageNoVisibleError: !cancelledTaskPage.visibleError,
    });
  } else {
    Object.assign(assertions, {
      issuedAndRunningTransitionsSucceeded:
        stateTransitions.length === 2 &&
        stateTransitions.every((transition) => transition.ok && transition.body.includes("SUCCESS")),
      resultEditorLoadedJudge: Boolean(
        executionEditorState &&
          executionEditorState.judgeType === "function" &&
          executionEditorState.judgeInstalled
      ),
      resultEditorRowsCompleted: Boolean(
        executionEditorState &&
          executionEditorState.rows &&
          executionEditorState.rows.length > 0 &&
          executionEditorState.rows.every(
            (row) => row.concluse === completionResult && row.realValueId === completionConclusionId
          )
      ),
      completionSaveResponseSuccess: Boolean(
        completionResultResponse &&
          completionResultResponse.ok &&
          !visibleErrorPattern.test(completionResultResponse.text)
      ),
      enteringResultParentRefreshSucceeded: Boolean(
        parentRefreshResult && parentRefreshResult.ok && !visibleErrorPattern.test(parentRefreshResult.text)
      ),
      taskCompletedPersisted: Boolean(statePersistence && statePersistence.taskState === completedState),
      taskExecutionTimesPersisted: Boolean(
        statePersistence && statePersistence.actualStartTime && statePersistence.actualEndTime
      ),
      taskCompleteStaffPersisted: Number(statePersistence && statePersistence.completeStaff) === staffId,
      taskExecutionRemarkPersisted: Boolean(
        statePersistence && statePersistence.remark === executionRemark
      ),
      detailResultPersisted: Boolean(detail && String(detail.concluse) === completionResult),
      detailConclusionPersisted: Boolean(detail && detail.realValue === completionConclusionId),
      detailCompletionAuditPersisted: Boolean(
        detail && Number(detail.completeUser) === staffId && detail.completeDate
      ),
      allDetailsCompleted: Number(persistence && persistence.detailCompletionNullCount) === 0,
      taskCompletedVisibleInGrid,
      enteringResultPageNoVisibleError: !enteringResultPage.visibleError,
      completedTaskPageNoVisibleError: !completedTaskPage.visibleError,
    });
    if (hiddenDangerMode) {
      const createdData = responseData(hiddenDangerResult);
      const reusedData = responseData(hiddenDangerReuseResult);
      const risk = hiddenDangerPersistence && hiddenDangerPersistence.risk;
      const linkedDetail = hiddenDangerPersistence && hiddenDangerPersistence.detail;
      Object.assign(assertions, {
        abnormalSummaryPageNoVisibleError: Boolean(abnormalSummaryPage && !abnormalSummaryPage.visibleError),
        abnormalSummaryQuerySucceeded: Boolean(
          abnormalSummaryQueryResult &&
            abnormalSummaryQueryResult.ok &&
            !visibleErrorPattern.test(abnormalSummaryQueryResult.text)
        ),
        abnormalDetailSelectedThroughGrid: Boolean(
          abnormalSelection &&
            abnormalSelection.length === 1 &&
            Number(abnormalSelection[0].id) === Number(detail.id)
        ),
        hiddenDangerConfirmationShown: Boolean(
          hiddenDangerConfirmText && hiddenDangerConfirmText.includes("是否生成隐患单")
        ),
        hiddenDangerUiRequestCaptured: Boolean(
          hiddenDangerRequest &&
            hiddenDangerRequest.method === "POST" &&
            hiddenDangerRequest.url.endsWith(createHiddenDangerPath) &&
            String(hiddenDangerRequest.postData || "").includes(`ids=${detail.id}`)
        ),
        hiddenDangerUiResponseSucceeded: Boolean(
          hiddenDangerResult &&
            hiddenDangerResult.ok &&
            hiddenDangerResult.json &&
            hiddenDangerResult.json.code === 200 &&
            createdData &&
            Number(createdData.createdCount) === 1 &&
            Number(createdData.reusedCount) === 0 &&
            createdData.compatibilityMode === true &&
            createdData.compatibilityStatus === "PATROL_COMPATIBILITY_PENDING"
        ),
        taskDetailLinkedToRisk: Boolean(
          linkedDetail &&
            linkedDetail.isFault === true &&
            Number(linkedDetail.faultId) === Number(risk && risk.id) &&
            linkedDetail.faultTableNo === (risk && risk.tableNo)
        ),
        compatibilityRiskPersisted: Boolean(
          risk &&
            Number(hiddenDangerPersistence.riskCount) === 1 &&
            Number(risk.status) === 1 &&
            Number(risk.valid) === 1 &&
            Number(risk.version) === 0 &&
            risk.riskMode === "PATROL_COMPATIBILITY_PENDING" &&
            risk.riskSource &&
            risk.riskContent &&
            Number(risk.finder) === staffId &&
            risk.findTime &&
            risk.createTime
        ),
        hiddenDangerRetryReusedExistingRisk: Boolean(
          hiddenDangerReuseResult &&
            hiddenDangerReuseResult.ok &&
            hiddenDangerReuseResult.json &&
            hiddenDangerReuseResult.json.code === 200 &&
            reusedData &&
            Number(reusedData.createdCount) === 0 &&
            Number(reusedData.reusedCount) === 1 &&
            Number(hiddenDangerReusePersistence && hiddenDangerReusePersistence.riskCount) === 1 &&
            Number(hiddenDangerReusePersistence && hiddenDangerReusePersistence.faultId) ===
              Number(risk && risk.id) &&
            (hiddenDangerReusePersistence && hiddenDangerReusePersistence.faultTableNo) ===
              (risk && risk.tableNo)
        ),
        eamRiskPageNoVisibleError: Boolean(eamRiskPage && !eamRiskPage.visibleError),
        eamRiskVisibleInGrid: eamRiskVisible,
        eamRiskSourceTranslated: Boolean(eamRiskRow && eamRiskRow.riskSource === "巡检"),
        eamRiskSesgisLoadsBeforeEdit: Boolean(
          eamRiskScriptOrder &&
            eamRiskScriptOrder.sesgisIndex >= 0 &&
            eamRiskScriptOrder.editIndex > eamRiskScriptOrder.sesgisIndex
        ),
      });
    }
  }
  const failedAssertions = Object.entries(assertions)
    .filter(([, passed]) => !passed)
    .map(([name]) => name);

  const endpoints = {
    planSubmit: { method: "POST", path: planSubmitPath, ...compactHttp(planResult) },
    taskGeneration: { method: "POST", path: createTaskSubmitPath, ...compactHttp(generationResult) },
    taskListQuery: { method: "POST", path: taskQueryPath, ...compactHttp(taskQueryResult) },
  };
  const payloads = {
    plan: planPayload,
    taskGeneration: createTaskPayload,
    stateTransitions,
  };
  if (action === "cancel") {
    endpoints.taskStateChange = {
      method: "GET",
      path: taskStateUpdatePath,
      ...compactHttp(taskStateChangeResult),
    };
    endpoints.cancelledTaskQuery = {
      method: "POST",
      path: taskQueryPath,
      ...compactHttp(cancelledQueryResult),
    };
    payloads.taskStateChange = {
      changeState: cancelledState,
      remark: stateChangeRemark,
      idList: [taskId],
    };
  } else {
    endpoints.enteringResultQuery = {
      method: "POST",
      path: enteringResultQueryPath,
      ...compactHttp(enteringResultQueryResult),
    };
    endpoints.completionSave = {
      method: "POST",
      path: `${enteringResultSubmitPrefix}{save|submit}`,
      ...compactHttp(completionResultResponse),
    };
    endpoints.enteringResultParentRefresh = {
      method: "POST",
      path: enteringResultQueryPath,
      ...compactHttp(parentRefreshResult),
    };
    endpoints.completedTaskQuery = {
      method: "POST",
      path: taskQueryPath,
      ...compactHttp(completedQueryResult),
    };
    payloads.completion = {
      result: completionResult,
      conclusion: completionConclusion,
      conclusionId: completionConclusionId,
      detailCount: executionEditorState && executionEditorState.detailCount,
    };
    if (hiddenDangerMode) {
      endpoints.abnormalSummaryQuery = {
        method: "POST",
        path: abnormalSummaryQueryPath,
        ...compactHttp(abnormalSummaryQueryResult),
      };
      endpoints.hiddenDangerCreate = {
        method: "POST",
        path: createHiddenDangerPath,
        ...compactHttp(hiddenDangerResult),
      };
      endpoints.hiddenDangerRetry = {
        method: "POST",
        path: createHiddenDangerPath,
        ...compactHttp(hiddenDangerReuseResult),
      };
      endpoints.eamRiskLedger = {
        method: "GET",
        path: eamRiskRoute,
        status: eamRiskPage && eamRiskPage.status,
        ok: Boolean(eamRiskPage && eamRiskPage.status && eamRiskPage.status < 400),
        body: "",
      };
      payloads.hiddenDanger = {
        ids: `${detail.id},`,
        uiRequest: hiddenDangerRequest,
      };
    }
  }

  const report = {
    generatedAt: new Date().toISOString(),
    baseUrl,
    database: "PostgreSQL",
    action,
    marker,
    planCode,
    generationRemark,
    ids: {
      routeId,
      staffId,
      planId,
      generationId,
      taskId,
      taskDetailId: detail && detail.id,
      riskId:
        hiddenDangerPersistence && hiddenDangerPersistence.risk
          ? hiddenDangerPersistence.risk.id
          : null,
    },
    endpoints,
    payloads,
    executionEditorState,
    statePersistence,
    hiddenDanger: hiddenDangerMode
      ? {
          abnormalSelection,
          confirmText: hiddenDangerConfirmText,
          clientState: hiddenDangerClientState,
          creationPersistence: hiddenDangerPersistence,
          retryPersistence: hiddenDangerReusePersistence,
          eamRiskVisible,
          eamRiskRow,
          eamRiskScriptOrder,
        }
      : null,
    persistenceSql: persistenceSql.trim(),
    persistence,
    assertions,
    failedAssertions,
    evidence,
    status: failedAssertions.length === 0 ? "PASS" : "FAIL",
  };
  fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`, "utf8");
  await context.close();
  await browser.close();
  activeBrowser = null;

  console.log(JSON.stringify({ outputPath, marker, ids: report.ids, status: report.status }, null, 2));
  if (failedAssertions.length > 0) {
    throw new Error(`PATROL acceptance failed: ${failedAssertions.join(", ")}`);
  }
}

main().catch(async (error) => {
  console.error(error.stack || error.message || String(error));
  if (activeBrowser) {
    await activeBrowser.close().catch(() => {});
  }
  process.exitCode = 1;
});
