#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { execFileSync } = require("child_process");
const { chromium, request } = require("playwright");

const now = new Date();
const stamp = now.toISOString().replace(/[-:T.Z]/g, "").slice(0, 14);
const marker = process.env.ADP_E2E_MARKER || `ADP_E2E_${stamp}_PATROL`;
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
const stateChangeRemark = `${marker}_CANCELLED_BY_UI`;
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
  const patrolUrlPattern = /\/msService\/PATROL\//;
  page.on("console", (message) => {
    if (["error", "warning"].includes(message.type())) {
      evidence.consoleErrors.push({
        type: message.type(),
        text: message.text().slice(0, 1000),
        location: message.location(),
      });
    }
  });
  page.on("pageerror", (error) => evidence.pageErrors.push(error.message.slice(0, 1000)));
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

  const batchPage = await openBusinessPage(page, batchChangeRoute, evidence, "巡检任务批量变更");
  await page.waitForFunction((expectedTableNo) => (document.body.innerText || "").includes(expectedTableNo), taskTableNo, {
    timeout: 30000,
  });
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
  const taskStateChangeResult = await compactPageResponse(await stateChangeResponsePromise);
  if (!taskStateChangeResult.ok || !taskStateChangeResult.text.includes("SUCCESS")) {
    throw new Error(`Task state change failed: ${JSON.stringify(compactHttp(taskStateChangeResult))}`);
  }

  const statePersistence = queryJson(`
select json_build_object(
  'id', id,
  'tableNo', table_no,
  'taskState', task_state,
  'remark', remark,
  'version', version,
  'valid', valid
)
from public.mp_potrol_tasks
where id=${taskId};
`);

  const cancelledTaskPage = await openBusinessPage(page, taskListRoute, evidence, "巡检任务取消结果");
  const cancelledQueryResponsePromise = page.waitForResponse(
    (response) => response.url().endsWith(taskQueryPath) && response.request().method() === "POST",
    { timeout: 30000 }
  );
  await page.locator('button[data-id="query"]').click();
  const cancelledQueryResult = await compactPageResponse(await cancelledQueryResponsePromise);
  const taskCancelledVisibleInGrid = await page
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
      'patrolPlan', t.patrol_plan, 'patrolPlanId', t.patrol_plan_id, 'workRoute', t.work_route
    ) from public.mp_potrol_tasks t where t.patrol_plan_id=${planId} order by t.id desc limit 1
  ),
  'detail', (
    select json_build_object(
      'id', d.id, 'valid', d.valid, 'version', d.version,
      'taskDetailState', d.task_detail_state, 'patrolTask', d.patrol_task,
      'workItemId', d.work_item_id, 'content', d.content
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
    taskStateChangeResponseSuccess: taskStateChangeResult.ok && taskStateChangeResult.text.includes("SUCCESS"),
    taskCancelledPersisted: Boolean(statePersistence && statePersistence.taskState === cancelledState),
    taskStateRemarkPersisted: Boolean(statePersistence && statePersistence.remark === stateChangeRemark),
    taskCancelledVisibleInGrid,
    detailPersisted: Boolean(detail && Number(detail.patrolTask) === taskId),
    detailDefaultsPersisted: Boolean(
      detail &&
        detail.valid === true &&
        Number(detail.version) === 0 &&
        detail.taskDetailState === "PATROL_taskDetailState/pending"
    ),
    noPlanAssociationMismatch: Number(persistence && persistence.planAssociationMismatchCount) === 0,
    noNullDetailLifecycle: Number(persistence && persistence.detailLifecycleNullCount) === 0,
    planPageNoVisibleError: !planPage.visibleError,
    taskPageNoVisibleError: !taskPage.visibleError,
    batchPageNoVisibleError: !batchPage.visibleError,
    cancelledTaskPageNoVisibleError: !cancelledTaskPage.visibleError,
    noPatrolRequestFailures: evidence.requestFailures.length === 0,
    noLegacyI18nParameterErrors: !evidence.consoleErrors.some((item) => item.text.includes("请将参数放在数组里")),
    noConsoleErrors: !evidence.consoleErrors.some((item) => item.type === "error"),
  };
  const failedAssertions = Object.entries(assertions)
    .filter(([, passed]) => !passed)
    .map(([name]) => name);

  const report = {
    generatedAt: new Date().toISOString(),
    baseUrl,
    database: "PostgreSQL",
    marker,
    planCode,
    generationRemark,
    ids: { routeId, staffId, planId, generationId, taskId, taskDetailId: detail && detail.id },
    endpoints: {
      planSubmit: { method: "POST", path: planSubmitPath, ...compactHttp(planResult) },
      taskGeneration: { method: "POST", path: createTaskSubmitPath, ...compactHttp(generationResult) },
      taskListQuery: { method: "POST", path: taskQueryPath, ...compactHttp(taskQueryResult) },
      taskStateChange: {
        method: "GET",
        path: taskStateUpdatePath,
        ...compactHttp(taskStateChangeResult),
      },
      cancelledTaskQuery: { method: "POST", path: taskQueryPath, ...compactHttp(cancelledQueryResult) },
    },
    payloads: {
      plan: planPayload,
      taskGeneration: createTaskPayload,
      taskStateChange: { changeState: cancelledState, remark: stateChangeRemark, idList: [taskId] },
    },
    statePersistence,
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
