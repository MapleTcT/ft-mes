#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { execFileSync } = require("child_process");
const { chromium, request } = require("playwright");

const baseUrl = (process.env.ADP_BASE_URL || "http://100.99.133.43:18080").replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD || "123456";
const headless = process.env.ADP_HEADLESS !== "false";
const dbSshTarget = process.env.ADP_DB_SSH_TARGET || "v6@100.99.133.43";
const dbContainer = process.env.ADP_DB_CONTAINER || "adp-mes-newbase-postgres-1";
const dbName = process.env.ADP_DB_NAME || "adp";
const dbUser = process.env.ADP_DB_USER || "adp";
const nowToken = new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14);
const marker = process.env.ADP_E2E_MARKER || `ADP_E2E_${nowToken}_WAPS_WORKPLAN`;
const outputDir =
  process.env.ADP_OUTPUT_DIR ||
  path.join("/tmp", `adp-waps-workplan-persistence-${nowToken}`);
const outputPath =
  process.env.ADP_WAPS_WORKPLAN_PERSISTENCE_OUTPUT ||
  path.join(outputDir, "waps-workplan-persistence-results.json");
const route = "/msService/workAppointment/workPlan/workTicketPlan/workPlanList";
const scenario = process.env.ADP_WAPS_WORKPLAN_SCENARIO || "save-delete";
const saveApi = "/msService/workAppointment/workPlan/workPlan/workPlanUltraSubmit";
const deleteApi = "/msService/workAppointment/workPlan/workTicketPlan/delete";
const deploymentId = process.env.ADP_WAPS_WORKPLAN_DEPLOYMENT_ID || "6579649925021696";
const entityCode = "workAppointment_6.1.6.1_workPlan";
const editViewCode = "workAppointment_6.1.6.1_workPlan_workPlanEdit";
const approveViewCode = "workAppointment_6.1.6.1_workPlan_workPlanApprove";

const approvalSteps = [
  {
    name: "work plan edit submit",
    activityName: "TaskEvent_0tb6d76",
    viewCode: editViewCode,
    viewPath: "workPlanEdit",
    outcome: "SequenceFlow_15k1f4h",
    outcomeDes: "提交",
    outcomeType: "normal",
  },
  {
    name: "work plan worker approval",
    activityName: "TaskEvent_01f7pag",
    viewCode: approveViewCode,
    viewPath: "workPlanApprove",
    outcome: "SequenceFlow_1arr2zz",
    outcomeDes: "提交",
    outcomeType: "normal",
  },
  {
    name: "work plan analyst approval",
    activityName: "TaskEvent_0uatdpl",
    viewCode: approveViewCode,
    viewPath: "workPlanApprove",
    outcome: "SequenceFlow_1wj6nyt",
    outcomeDes: "提交",
    outcomeType: "normal",
  },
  {
    name: "work plan final approval",
    activityName: "TaskEvent_1p2qrpi",
    viewCode: approveViewCode,
    viewPath: "workPlanApprove",
    outcome: "SequenceFlow_1qu55ey",
    outcomeDes: "生效",
    outcomeType: "normal",
    expectedStatus: "99",
  },
];

const cancelStep = {
  name: "work plan cancel from edit",
  activityName: "TaskEvent_0tb6d76",
  viewCode: editViewCode,
  viewPath: "workPlanEdit",
  outcome: "SequenceFlow_1wa5f0c",
  outcomeDes: "作废",
  outcomeType: "cancel",
  expectedStatus: "0",
};

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true });
}

function shellQuote(value) {
  return `'${String(value).replace(/'/g, "'\\''")}'`;
}

function sqlLiteral(value) {
  return `'${String(value).replace(/'/g, "''")}'`;
}

function runRemote(command, input) {
  return execFileSync(
    "ssh",
    [
      "-o",
      "BatchMode=yes",
      "-o",
      "StrictHostKeyChecking=no",
      "-o",
      "UserKnownHostsFile=/dev/null",
      dbSshTarget,
      command,
    ],
    { input, encoding: "utf8", stdio: ["pipe", "pipe", "pipe"] }
  );
}

function runSql(sql) {
  const command = [
    "docker",
    "exec",
    "-i",
    shellQuote(dbContainer),
    "psql",
    "-U",
    shellQuote(dbUser),
    "-d",
    shellQuote(dbName),
    "-v",
    "ON_ERROR_STOP=1",
    "-AtF",
    shellQuote("|"),
  ].join(" ");
  return runRemote(command, sql).trim();
}

function parseRows(raw) {
  return raw
    .split(/\r?\n/)
    .filter(Boolean)
    .map((line) => line.split("|"));
}

async function readJsonSafe(response) {
  const text = await response.text();
  try {
    return { json: JSON.parse(text), text };
  } catch (_error) {
    return { json: null, text };
  }
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

async function login(api) {
  const attempts = [
    { userName: username, password, clientId: "pc_dt" },
    { username, password, clientId: "pc_dt" },
  ];
  const errors = [];
  for (const body of attempts) {
    const response = await api.post(`${baseUrl}/inter-api/auth/login`, {
      data: body,
      headers: {
        Accept: "application/json, text/plain, */*",
        "Content-Type": "application/json;charset=UTF-8",
      },
    });
    const parsed = await readJsonSafe(response);
    const ticket = response.ok() ? findTicket(parsed.json) : null;
    if (ticket) {
      return { ticket, status: response.status() };
    }
    errors.push({ status: response.status(), body: parsed.text.slice(0, 500) });
  }
  throw new Error(`Login failed for ${username}: ${JSON.stringify(errors)}`);
}

function formatDate(offsetMinutes) {
  const date = new Date(Date.now() + offsetMinutes * 60 * 1000);
  const pad = (value) => String(value).padStart(2, "0");
  return [
    date.getFullYear(),
    "-",
    pad(date.getMonth() + 1),
    "-",
    pad(date.getDate()),
    " ",
    pad(date.getHours()),
    ":",
    pad(date.getMinutes()),
    ":",
    pad(date.getSeconds()),
  ].join("");
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function savePayload(description = marker, options = {}) {
  const workTicketPlan = {
    applyDep: { id: 1 },
    applyStaff: { id: 1 },
    applyTime: formatDate(5),
    workTime: formatDate(65),
    planDescription: description,
    ...(options.workTicketPlan || {}),
  };
  if (options.id) {
    workTicketPlan.id = Number(options.id);
  }
  if (options.version) {
    workTicketPlan.version = Number(options.version);
  }
  if (options.tableInfoId) {
    workTicketPlan.tableInfoId = Number(options.tableInfoId);
  }
  if (options.tableNo) {
    workTicketPlan.tableNo = options.tableNo;
  }

  return {
    workPlan: {
      id: options.id ? Number(options.id) : undefined,
      viewCode: editViewCode,
      operateType: "save",
      deploymentId,
      pendingId: options.pendingId ? Number(options.pendingId) : undefined,
      activityName: options.activityName || "start_glvw15o",
      pendingActivityType: options.pendingActivityType || undefined,
      files_staffId: "1",
      uploadFileFormMap: [],
      workFlowVar: {
        outcome: "save",
        outcomeType: "save",
        deploymentId,
        entityCode,
      },
      workTicketPlan,
    },
    workActions: [],
  };
}

function planSql(description = marker) {
  return `
SELECT id, version, valid, status, table_info_id, table_no, deployment_id, process_key, process_version, plan_description
FROM public.waps_work_ticket_plans
WHERE plan_description = ${sqlLiteral(description)}
ORDER BY id DESC;
`;
}

function planByIdSql(id) {
  return `
SELECT id, version, valid, status, table_info_id, table_no, deployment_id, process_key, process_version, plan_description
FROM public.waps_work_ticket_plans
WHERE id = ${id}
ORDER BY id DESC;
`;
}

function sideEffectSql(row) {
  return `
SELECT 'pending_count', count(*)
FROM public.wfm_task_pending
WHERE table_info_id = ${row.tableInfoId}
   OR model_id = ${row.id}
   OR table_no = ${sqlLiteral(row.tableNo)};
SELECT 'di_count', count(*)
FROM public.waps_work_ticket_plans_di
WHERE table_info_id = ${row.tableInfoId}
   OR main_obj = ${row.id};
SELECT 'sv_count', count(*)
FROM public.waps_work_ticket_plans_sv
WHERE table_info_id = ${row.tableInfoId};
`;
}

function detailStateSql(row) {
  return `
${planByIdSql(row.id)}
SELECT 'pending', id::text, activity_name, coalesce(task_status::text, ''), coalesce(open_url, ''), coalesce(task_description, '')
FROM public.wfm_task_pending
WHERE table_info_id = ${row.tableInfoId}
ORDER BY id;
SELECT 'deal_info', id::text, activity_name, coalesce(outcome, ''), coalesce(outcome_des, ''), coalesce(dealinfo_type, '')
FROM public.waps_work_ticket_plans_di
WHERE table_info_id = ${row.tableInfoId}
   OR main_obj = ${row.id}
ORDER BY id;
SELECT 'sv_count', count(*)::text
FROM public.waps_work_ticket_plans_sv
WHERE table_info_id = ${row.tableInfoId};
`;
}

function activePendingSql(row, activityName) {
  return `
SELECT id, activity_name, coalesce(activity_type, ''), coalesce(task_status::text, ''), coalesce(open_url, ''), table_info_id, deployment_id, coalesce(task_description, '')
FROM public.wfm_task_pending
WHERE table_info_id = ${row.tableInfoId}
  AND activity_name = ${sqlLiteral(activityName)}
  AND coalesce(task_status, 88) = 88
ORDER BY id DESC
LIMIT 1;
`;
}

function firstPlanRow(raw) {
  const rows = parseRows(raw);
  if (rows.length !== 1) {
    throw new Error(`Expected exactly one plan for marker ${marker}, got ${rows.length}: ${raw}`);
  }
  const [id, version, valid, status, tableInfoId, tableNo, deployment, processKey, processVersion, description] = rows[0];
  return {
    id,
    version,
    valid,
    status,
    tableInfoId,
    tableNo,
    deploymentId: deployment,
    processKey,
    processVersion,
    description,
  };
}

function firstPlanRowById(raw, id) {
  const rows = parseRows(raw);
  if (rows.length !== 1) {
    throw new Error(`Expected exactly one plan for id ${id}, got ${rows.length}: ${raw}`);
  }
  const [planId, version, valid, status, tableInfoId, tableNo, deployment, processKey, processVersion, description] = rows[0];
  return {
    id: planId,
    version,
    valid,
    status,
    tableInfoId,
    tableNo,
    deploymentId: deployment,
    processKey,
    processVersion,
    description,
  };
}

function currentPlanRow(id) {
  return firstPlanRowById(runSql(planByIdSql(id)), id);
}

function parsePending(raw, activityName) {
  const rows = parseRows(raw);
  if (rows.length !== 1) {
    throw new Error(`Expected active pending ${activityName}, got ${rows.length}: ${raw}`);
  }
  const [id, name, activityType, taskStatus, openUrl, tableInfoId, pendingDeploymentId, taskDescription] = rows[0];
  return {
    id,
    activityName: name,
    activityType,
    taskStatus,
    openUrl,
    tableInfoId,
    deploymentId: pendingDeploymentId,
    taskDescription,
  };
}

async function waitForPending(row, activityName) {
  let lastRaw = "";
  for (let attempt = 0; attempt < 20; attempt += 1) {
    lastRaw = runSql(activePendingSql(row, activityName));
    if (lastRaw) {
      return parsePending(lastRaw, activityName);
    }
    await sleep(1000);
  }
  throw new Error(`Timed out waiting for pending ${activityName}. Last query: ${lastRaw}`);
}

function parseWrappedData(response) {
  let data = response && response.json && Object.prototype.hasOwnProperty.call(response.json, "data")
    ? response.json.data
    : response && response.json;
  if (typeof data === "string") {
    const text = data.trim();
    if (text.startsWith("{") || text.startsWith("[")) {
      data = JSON.parse(text);
    }
  }
  return data;
}

function buildRoute(row, pending, step) {
  const pathValue = step.viewPath || "workPlanEdit";
  return `/msService/workAppointment/workPlan/workTicketPlan/${pathValue}?pendingId=${pending.id}&tableInfoId=${row.tableInfoId}&entityCode=${entityCode}&id=${row.id}&deploymentId=${row.deploymentId}`;
}

async function openRoute(page, routeValue) {
  const result = { route: routeValue };
  try {
    const nav = await page.goto(routeValue, { waitUntil: "commit", timeout: 60000 });
    result.status = nav ? nav.status() : null;
    await page.waitForLoadState("domcontentloaded", { timeout: 15000 }).catch((error) => {
      result.domcontentloadedWarning = error.message;
    });
  } catch (error) {
    result.error = error.message;
    await page.evaluate(() => window.stop()).catch(() => {});
  }
  result.title = await page.title().catch(() => "");
  return result;
}

function buildSubmitPayload(data, pending, step) {
  const plan = {
    id: Number(data.id),
    version: data.version == null ? undefined : Number(data.version),
    ...(step.workTicketPlanPatch || {}),
  };
  return {
    id: Number(data.id),
    workTicketPlan: plan,
    viewCode: step.viewCode,
    operateType: "submit",
    deploymentId: String(data.deploymentId || pending.deploymentId || deploymentId),
    linkId: String(data.tableInfoId),
    pendingId: Number(pending.id),
    activityName: pending.activityName,
    pendingActivityType: pending.activityType || "4",
    webSignetFlag: false,
    superEdit: false,
    files_staffId: "1",
    uploadFileFormMap: [],
    dgList: {},
    dgDeletedIds: {},
    workFlowVar: {
      outcome: step.outcome,
      outcomeType: step.outcomeType || "normal",
      outcomeDes: step.outcomeDes,
      outcomeDesZhCn: step.outcomeDes,
      outcomeMap: [
        {
          outcome: step.outcome,
          dec: step.outcomeDes,
          type: step.outcomeType || "normal",
          assignUser: "",
        },
      ],
      deploymentId: String(data.deploymentId || pending.deploymentId || deploymentId),
      entityCode,
    },
  };
}

async function browserFetch(page, method, apiPath, payload, timeoutMs = 45000) {
  const apiContext = page.__adpApiContext;
  if (apiContext && process.env.ADP_BROWSER_FETCH_MODE !== "page") {
    try {
      const response = await apiContext.fetch(apiPath, {
        method,
        data: payload,
        headers: {
          Accept: "application/json, text/plain, */*",
          "Content-Type": "application/json;charset=UTF-8",
        },
        timeout: timeoutMs,
      });
      const parsed = await readJsonSafe(response);
      return { status: response.status(), ok: response.ok(), body: parsed.text, json: parsed.json };
    } catch (error) {
      return {
        status: 0,
        ok: false,
        body: error && error.message ? error.message : String(error),
        json: null,
        errorName: error && error.name ? error.name : "Error",
      };
    }
  }

  const fetchInPage = page.evaluate(
    async ({ method: requestMethod, apiPath: pathValue, payload: bodyValue, timeoutMs: requestTimeoutMs }) => {
      const controller = new AbortController();
      const timer = setTimeout(() => controller.abort(), requestTimeoutMs);
      try {
        const response = await fetch(pathValue, {
          method: requestMethod,
          headers: {
            Accept: "application/json, text/plain, */*",
            "Content-Type": "application/json;charset=UTF-8",
          },
          body: bodyValue === undefined ? undefined : JSON.stringify(bodyValue),
          signal: controller.signal,
        });
        const text = await response.text();
        let json = null;
        try {
          json = JSON.parse(text);
        } catch (_error) {
          json = null;
        }
        return { status: response.status, ok: response.ok, body: text, json };
      } catch (error) {
        return {
          status: 0,
          ok: false,
          body: error && error.message ? error.message : String(error),
          json: null,
          errorName: error && error.name ? error.name : "Error",
        };
      } finally {
        clearTimeout(timer);
      }
    },
    { method, apiPath, payload, timeoutMs }
  );
  const hardTimeout = sleep(timeoutMs + 5000).then(() => ({
    status: 0,
    ok: false,
    body: `Timed out waiting for page fetch after ${timeoutMs + 5000}ms`,
    json: null,
    errorName: "TimeoutError",
  }));
  return Promise.race([fetchInPage, hardTimeout]);
}

function captureState(row) {
  return {
    planSql: planByIdSql(row.id).trim(),
    sideEffectSql: sideEffectSql(row).trim(),
    detailStateSql: detailStateSql(row).trim(),
    planRaw: runSql(planByIdSql(row.id)),
    sideEffectsRaw: runSql(sideEffectSql(row)),
    detailStateRaw: runSql(detailStateSql(row)),
  };
}

async function createPlan(page, evidence, key, description) {
  const payload = savePayload(description);
  evidence.operations[key] = { method: "POST", api: saveApi, payload };
  evidence.operations[key].response = await browserFetch(page, "POST", saveApi, payload);
  if (evidence.operations[key].response.status !== 200) {
    throw new Error(`${key} failed: ${JSON.stringify(evidence.operations[key].response)}`);
  }

  const raw = runSql(planSql(description));
  const created = firstPlanRow(raw);
  evidence.database[key] = {
    marker: description,
    planSql: planSql(description).trim(),
    planRaw: raw,
    plan: created,
    sideEffectSql: sideEffectSql(created).trim(),
    sideEffectsRaw: runSql(sideEffectSql(created)),
  };
  if (created.valid !== "t") {
    throw new Error(`${key} saved plan is not valid=true: ${raw}`);
  }
  return created;
}

async function adjustPlan(page, evidence, row, description) {
  const pending = await waitForPending(row, "TaskEvent_0tb6d76");
  const payload = savePayload(description, {
    id: row.id,
    version: row.version,
    tableInfoId: row.tableInfoId,
    tableNo: row.tableNo,
    pendingId: pending.id,
    activityName: pending.activityName,
    pendingActivityType: pending.activityType || "4",
  });
  evidence.operations.adjust = {
    method: "POST",
    api: saveApi,
    pending,
    payload,
  };
  evidence.operations.adjust.response = await browserFetch(page, "POST", saveApi, payload);
  if (evidence.operations.adjust.response.status !== 200) {
    throw new Error(`Adjust failed: ${JSON.stringify(evidence.operations.adjust.response)}`);
  }

  const adjusted = currentPlanRow(row.id);
  evidence.database.afterAdjust = {
    marker: description,
    plan: adjusted,
    state: captureState(adjusted),
  };
  if (adjusted.description !== description) {
    throw new Error(`Adjusted description did not persist: ${JSON.stringify(adjusted)}`);
  }
  return adjusted;
}

async function runSubmitStep(page, evidence, row, step) {
  const pending = await waitForPending(row, step.activityName);
  const stepEvidence = {
    name: step.name,
    pending,
    route: buildRoute(row, pending, step),
    api: `/msService/workAppointment/workPlan/workTicketPlan/${step.viewPath}/submit?id=${row.id}`,
    viewCode: step.viewCode,
    expected: {
      outcome: step.outcome,
      outcomeDes: step.outcomeDes,
      outcomeType: step.outcomeType || "normal",
      status: step.expectedStatus || null,
    },
  };
  stepEvidence.navigation = await openRoute(page, stepEvidence.route);
  stepEvidence.dataResponse = await browserFetch(
    page,
    "GET",
    `/msService/workAppointment/workPlan/workTicketPlan/data/${row.id}?pendingId=${pending.id}`
  );
  if (stepEvidence.dataResponse.status !== 200) {
    throw new Error(`${step.name} data failed: ${JSON.stringify(stepEvidence.dataResponse).slice(0, 1000)}`);
  }

  const data = parseWrappedData(stepEvidence.dataResponse);
  if (!data || !data.id) {
    throw new Error(`${step.name} data payload did not include id: ${JSON.stringify(stepEvidence.dataResponse).slice(0, 1000)}`);
  }
  const payload = buildSubmitPayload(data, pending, step);
  stepEvidence.payloadSummary = {
    id: payload.id,
    pendingId: payload.pendingId,
    activityName: payload.activityName,
    viewCode: payload.viewCode,
    deploymentId: payload.deploymentId,
    outcome: payload.workFlowVar.outcome,
    outcomeType: payload.workFlowVar.outcomeType,
    outcomeDes: payload.workFlowVar.outcomeDes,
    planVersion: payload.workTicketPlan.version,
  };
  stepEvidence.response = await browserFetch(page, "POST", stepEvidence.api, payload);
  if (stepEvidence.response.status !== 200) {
    throw new Error(`${step.name} submit failed: ${JSON.stringify(stepEvidence.response).slice(0, 1000)}`);
  }

  const after = currentPlanRow(row.id);
  stepEvidence.after = {
    plan: after,
    state: captureState(after),
  };
  if (step.expectedStatus && after.status !== step.expectedStatus) {
    throw new Error(`${step.name} expected status ${step.expectedStatus}, got ${after.status}`);
  }
  return { row: after, evidence: stepEvidence };
}

async function runApprovalScenario(page, evidence) {
  const created = await createPlan(page, evidence, "approvalCreate", `${marker}_APPROVE`);
  let row = created;
  evidence.operations.approvalSteps = [];
  for (const step of approvalSteps) {
    const result = await runSubmitStep(page, evidence, row, step);
    row = result.row;
    evidence.operations.approvalSteps.push(result.evidence);
  }
  const finalState = captureState(row);
  evidence.database.afterApproval = {
    plan: row,
    state: finalState,
  };
  if (!finalState.sideEffectsRaw.includes("pending_count|0")) {
    throw new Error(`Approval did not clear pending tasks: ${finalState.sideEffectsRaw}`);
  }
  if (row.status !== "99") {
    throw new Error(`Approval did not make plan effective, status=${row.status}`);
  }
}

async function runAdjustCancelScenario(page, evidence) {
  const created = await createPlan(page, evidence, "adjustCancelCreate", `${marker}_ADJUST_CANCEL`);
  const adjusted = await adjustPlan(page, evidence, created, `${marker}_ADJUSTED`);
  const result = await runSubmitStep(page, evidence, adjusted, cancelStep);
  evidence.operations.cancel = result.evidence;
  evidence.database.afterCancel = {
    plan: result.row,
    state: captureState(result.row),
  };
  if (!evidence.database.afterCancel.state.sideEffectsRaw.includes("pending_count|0")) {
    throw new Error(`Cancel did not clear pending tasks: ${evidence.database.afterCancel.state.sideEffectsRaw}`);
  }
}

async function runBrowser(ticket, evidence) {
  const browser = await chromium.launch({ headless });
  let apiContext = null;
  try {
    const context = await browser.newContext({
      baseURL: baseUrl,
      ignoreHTTPSErrors: true,
      viewport: { width: 1600, height: 1000 },
      extraHTTPHeaders: { Authorization: `Bearer ${ticket}` },
    });
    await context.addCookies([
      { name: "suposTicket", value: ticket, url: baseUrl },
      { name: "SUPOS_TICKET", value: ticket, url: baseUrl },
    ]);
    await context.addInitScript((token) => {
      window.localStorage.clear();
      window.sessionStorage.clear();
      ["suposTicket", "SUPOS_TICKET", "token", "ticket"].forEach((key) => {
        window.localStorage.setItem(key, token);
        window.sessionStorage.setItem(key, token);
      });
    }, ticket);

    apiContext = await request.newContext({
      baseURL: baseUrl,
      ignoreHTTPSErrors: true,
      extraHTTPHeaders: {
        Authorization: `Bearer ${ticket}`,
        ticket,
        suposTicket: ticket,
        SUPOS_TICKET: ticket,
        Accept: "application/json, text/plain, */*",
      },
    });
    const page = await context.newPage();
    page.__adpApiContext = apiContext;
    page.on("console", (message) => {
      if (["error", "warning"].includes(message.type())) {
        evidence.frontend.console.push({ type: message.type(), text: message.text() });
      }
    });
    page.on("pageerror", (error) => evidence.frontend.pageErrors.push(error.message));
    page.on("requestfailed", (requestItem) => {
      evidence.frontend.requestFailures.push({
        method: requestItem.method(),
        url: requestItem.url(),
        failure: requestItem.failure() && requestItem.failure().errorText,
      });
    });
    page.on("request", (requestItem) => {
      const url = requestItem.url();
      if (/workPlanList|workPlanUltraSubmit|workTicketPlan\/delete|workPlanEdit|workPlanApprove|workTicketPlan\/data|layoutJson|upload-list/.test(url)) {
        evidence.frontend.requests.push({
          method: requestItem.method(),
          url,
          postData: requestItem.postData(),
        });
      }
    });
    page.on("response", async (response) => {
      const url = response.url();
      if (!/workPlanList|workPlanUltraSubmit|workTicketPlan\/delete|workPlanEdit|workPlanApprove|workTicketPlan\/data|layoutJson|upload-list/.test(url)) {
        return;
      }
      let body = "";
      try {
        body = (await response.text()).slice(0, 4000);
      } catch (_error) {
        body = "";
      }
      evidence.frontend.responses.push({
        method: response.request().method(),
        url,
        status: response.status(),
        body,
      });
    });

    evidence.frontend.route = await openRoute(page, route);

    if (scenario === "approval") {
      await runApprovalScenario(page, evidence);
      evidence.status = "PASS";
      return;
    }
    if (scenario === "adjust-cancel") {
      await runAdjustCancelScenario(page, evidence);
      evidence.status = "PASS";
      return;
    }
    if (scenario === "workflow") {
      await runAdjustCancelScenario(page, evidence);
      await runApprovalScenario(page, evidence);
      evidence.status = "PASS";
      return;
    }
    if (scenario !== "save-delete") {
      throw new Error(`Unsupported ADP_WAPS_WORKPLAN_SCENARIO=${scenario}`);
    }

    evidence.operations.save.payload = savePayload();
    evidence.operations.save.response = await browserFetch(page, "POST", saveApi, evidence.operations.save.payload);
    if (evidence.operations.save.response.status !== 200) {
      throw new Error(`Save failed: ${JSON.stringify(evidence.operations.save.response)}`);
    }

    const createdRaw = runSql(planSql());
    const created = firstPlanRow(createdRaw);
    evidence.database.afterSave = {
      planSql: planSql().trim(),
      planRaw: createdRaw,
      plan: created,
      sideEffectSql: sideEffectSql(created).trim(),
      sideEffectsRaw: runSql(sideEffectSql(created)),
    };
    if (created.valid !== "t") {
      throw new Error(`Saved plan is not valid=true: ${createdRaw}`);
    }

    const deletePayload = { ids: `${created.id}@${created.version}` };
    evidence.operations.delete.payload = deletePayload;
    evidence.operations.delete.response = await browserFetch(page, "POST", deleteApi, deletePayload);
    if (evidence.operations.delete.response.status !== 200) {
      throw new Error(`Delete failed: ${JSON.stringify(evidence.operations.delete.response)}`);
    }

    const deletedRaw = runSql(planSql());
    const deleted = firstPlanRow(deletedRaw);
    evidence.database.afterDelete = {
      planSql: planSql().trim(),
      planRaw: deletedRaw,
      plan: deleted,
      sideEffectSql: sideEffectSql(deleted).trim(),
      sideEffectsRaw: runSql(sideEffectSql(deleted)),
    };
    if (deleted.valid !== "f") {
      throw new Error(`Deleted plan is not valid=false: ${deletedRaw}`);
    }
    if (!evidence.database.afterDelete.sideEffectsRaw.includes("pending_count|0")) {
      throw new Error(`Pending tasks were not cleaned after delete: ${evidence.database.afterDelete.sideEffectsRaw}`);
    }

    evidence.status = "PASS";
  } finally {
    if (apiContext) {
      await apiContext.dispose().catch(() => {});
    }
    await browser.close();
  }
}

async function main() {
  ensureDir(outputDir);
  const evidence = {
    generatedAt: new Date().toISOString(),
    baseUrl,
    dbSshTarget,
    scenario,
    marker,
    frontend: {
      console: [],
      pageErrors: [],
      requestFailures: [],
      requests: [],
      responses: [],
    },
    operations: {
      save: { method: "POST", api: saveApi },
      delete: { method: "POST", api: deleteApi },
    },
    database: {},
    status: "RUNNING",
    issues: [],
  };

  try {
    const api = await request.newContext({ ignoreHTTPSErrors: true });
    const loginResult = await login(api);
    await api.dispose();
    evidence.login = { status: loginResult.status, ticket: Boolean(loginResult.ticket) };
    await runBrowser(loginResult.ticket, evidence);
  } catch (error) {
    evidence.status = "FAIL";
    evidence.issues.push(error.stack || error.message);
    process.exitCode = 1;
  } finally {
    fs.writeFileSync(outputPath, JSON.stringify(evidence, null, 2));
    console.log(JSON.stringify({ status: evidence.status, marker, outputPath }, null, 2));
  }
}

main();
