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
const dbSshPassword = process.env.ADP_DB_SSH_PASSWORD || "";
const dbContainer = process.env.ADP_DB_CONTAINER || "adp-mes-newbase-postgres-1";
const dbName = process.env.ADP_DB_NAME || "adp";
const dbUser = process.env.ADP_DB_USER || "adp";
const nowToken = new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14);
const scenario = process.env.ADP_WTS_FIREWORK_SCENARIO || "close";
if (!["close", "stop"].includes(scenario)) {
  throw new Error(`Unsupported ADP_WTS_FIREWORK_SCENARIO=${scenario}; expected close or stop`);
}
const markerSuffix = scenario === "stop" ? "WTS_FIREWORK_STOP" : "WTS_FIREWORK";
const marker = process.env.ADP_E2E_MARKER || `ADP_E2E_${nowToken}_${markerSuffix}`;
const outputDir =
  process.env.ADP_OUTPUT_DIR || path.join("/tmp", `adp-wts-firework-persistence-${nowToken}`);
const outputPath =
  process.env.ADP_WTS_FIREWORK_PERSISTENCE_OUTPUT ||
  path.join(outputDir, "wts-firework-persistence-results.json");

const workPermitListRoute = "/msService/WTS/workPermit/workPermit/workPermitList";
const workPermitEditRoute = "/msService/WTS/workPermit/workPermit/workPermitEdit";
const permitSubmitApi = "/msService/WTS/workPermit/workPermit/permitUltraSubmit";
const entityCode = "WTS_1.0.0_workTicket";

const closeProcessMethod = {
  id: "WTS_processing/close",
  version: 0,
  cid: 1000,
  valid: true,
  code: "close",
  entityCode: "WTS_processing",
  value: "正常",
  uniqueCode: "WTS_processing/close",
  fullPathName: "正常",
  fullPath: "close",
  sort: 601,
  leaf: false,
  leaf2: false,
  defaultFlag: false,
  root: true,
  children: [],
  children2: [],
};

const stopProcessMethod = {
  id: "WTS_processing/stop",
  version: 0,
  cid: 1000,
  valid: true,
  code: "stop",
  entityCode: "WTS_processing",
  value: "终止",
  uniqueCode: "WTS_processing/stop",
  fullPathName: "终止",
  fullPath: "stop",
  sort: 603,
  leaf: false,
  leaf2: false,
  defaultFlag: false,
  root: true,
  children: [],
  children2: [],
};

const commonFlowSteps = [
  {
    name: "firework edit submit",
    activityName: "TaskEvent_15w67f5",
    viewCode: "WTS_1.0.0_workTicket_fireworkEdit",
    outcome: "SequenceFlow_0e9sj8r",
    outcomeDes: "提交",
  },
  {
    name: "firework gas submit",
    activityName: "TaskEvent_129vu5v",
    viewCode: "WTS_1.0.0_workTicket_fireworkGas",
    outcome: "SequenceFlow_03vvptt",
    outcomeDes: "提交",
  },
  {
    name: "firework risk submit",
    activityName: "TaskEvent_01x4zva",
    viewCode: "WTS_1.0.0_workTicket_fireworkRisk",
    outcome: "SequenceFlow_1g8eg7t",
    outcomeDes: "审批",
  },
  {
    name: "production safety approval",
    activityName: "TaskEvent_0ak83bj",
    viewCode: "WTS_1.0.0_workTicket_fireworkApproval",
    outcome: "SequenceFlow_07dgcpk",
    outcomeDes: "审批",
  },
  {
    name: "work unit safety approval",
    activityName: "TaskEvent_032m1nx",
    viewCode: "WTS_1.0.0_workTicket_fireworkApproval",
    outcome: "SequenceFlow_1ulmibf",
    outcomeDes: "通过",
  },
  {
    name: "check ticket to execution",
    activityName: "TaskEvent_06kzbg6",
    viewCode: "WTS_1.0.0_workTicket_fireworkApproval",
    outcome: "SequenceFlow_1g2rlaw",
    outcomeDes: "作业执行审批",
    expectedJobStatus: "WTS_jobStatus/execution",
  },
];

const closeFlowSteps = [
  ...commonFlowSteps,
  {
    name: "execution submit to close",
    activityName: "TaskEvent_1y6xnvi",
    viewCode: "WTS_1.0.0_workTicket_fireworkDeal",
    outcome: "SequenceFlow_1lv8x6k",
    outcomeDes: "提交",
    negativeValidation: {
      name: "execution submit without processMethod",
      expectedMessage: "请选择作业票处理方式",
      forbiddenMessage: "Cannot get property",
      expectedJobStatus: "WTS_jobStatus/execution",
    },
    workTicketPatch: { processMethod: closeProcessMethod },
    expectedJobStatus: "WTS_jobStatus/readyClose",
  },
  {
    name: "close ticket effective",
    activityName: "TaskEvent_0r1w2fv",
    viewCode: "WTS_1.0.0_workTicket_fireworkClose",
    outcome: "SequenceFlow_13nnr1b",
    outcomeDes: "生效",
    workTicketPatch: { checkResultsValue: `${marker}_CLOSE_ACCEPT` },
    expectedStatus: "99",
  },
];

const stopFlowSteps = [
  ...commonFlowSteps,
  {
    name: "execution submit to stop",
    activityName: "TaskEvent_1y6xnvi",
    viewCode: "WTS_1.0.0_workTicket_fireworkDeal",
    outcome: "SequenceFlow_1lv8x6k",
    outcomeDes: "提交",
    workTicketPatch: { processMethod: stopProcessMethod },
    expectedJobStatus: "WTS_jobStatus/stopping",
  },
  {
    name: "production safety stop confirmation",
    activityName: "TaskEvent_02aitou",
    viewCode: "WTS_1.0.0_workTicket_fireworkDeal",
    outcome: "SequenceFlow_0ars0ox",
    outcomeDes: "终止通知",
    expectedJobStatus: "WTS_jobStatus/stopping",
  },
  {
    name: "work unit safety stop confirmation",
    activityName: "TaskEvent_16rrw0a",
    viewCode: "WTS_1.0.0_workTicket_fireworkDeal",
    outcome: "SequenceFlow_1b61jor",
    outcomeDes: "终止通知",
    expectedJobStatus: "WTS_jobStatus/errorClose",
    expectedStatus: "99",
  },
];

const flowSteps = scenario === "stop" ? stopFlowSteps : closeFlowSteps;

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
  const args = [
    "-o",
    "PreferredAuthentications=password",
    "-o",
    "PubkeyAuthentication=no",
    "-o",
    "KexAlgorithms=curve25519-sha256",
    "-o",
    "Ciphers=aes128-ctr",
    "-o",
    "MACs=hmac-sha2-256",
    "-o",
    "IPQoS=none",
    "-o",
    "StrictHostKeyChecking=no",
    "-o",
    "UserKnownHostsFile=/dev/null",
    dbSshTarget,
    command,
  ];
  if (dbSshPassword) {
    return execFileSync("sshpass", ["-e", "ssh", ...args], {
      input,
      encoding: "utf8",
      env: { ...process.env, SSHPASS: dbSshPassword },
      stdio: ["pipe", "pipe", "pipe"],
    });
  }
  return execFileSync("ssh", ["-o", "BatchMode=yes", ...args], {
    input,
    encoding: "utf8",
    stdio: ["pipe", "pipe", "pipe"],
  });
}

function runSql(sql) {
  const command = [
    "docker",
    "exec",
    "-e",
    "PGPASSWORD=adp123456",
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
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(
    date.getHours()
  )}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

function currentFireworkDeploymentSql() {
  return `
SELECT id, process_version
FROM public.wf_deployment
WHERE process_key = 'fireWorkWF'
  AND coalesce(valid, 1) = 1
  AND coalesce(is_current_version, 0) = 1
ORDER BY coalesce(process_version, 0) DESC, id DESC
LIMIT 1;
`;
}

function createPayload(deploymentId) {
  return {
    permit: {
      operateType: "save",
      viewCode: "WTS_1.0.0_workPermit_workPermitEdit",
      dgList: {},
      dgDeletedIds: {},
      files_staffId: "1",
      uploadFileFormMap: [],
      workFlowVar: {},
      workPermit: {
        ticketNo: marker,
        ticketType: "WTS_workType/fireWork",
        workPlan: `${marker}_PLAN`,
        workAction: `${marker}_ACTION`,
        content: `${marker} 作业许可含动火票提交验收`,
        workAreaText: `${marker} 区域`,
        applyDept: { id: 1 },
        applyStaff: { id: 1 },
        applyTime: formatDate(5),
        permitStatus: { id: "WTS_finishStatus/toEdit" },
        isTemp: true,
        associateUnder: false,
      },
    },
    workTickets: [
      {
        operateType: "submit",
        viewCode: "WTS_1.0.0_workTicket_fireworkView",
        deploymentId: Number(deploymentId),
        dgList: {},
        dgDeletedIds: {},
        files_staffId: "1",
        uploadFileFormMap: [],
        workFlowVar: {},
        workTicket: {
          workTicketNo: `${marker}_FIRE`,
          workType: { id: "WTS_workType/fireWork" },
          levelOne: { id: "WTS_levelOne/firstLevel" },
          workTicketMaker: { id: 1 },
          applyDept: { id: 1 },
          applyStaff: { id: 1 },
          applyTime: formatDate(5),
          content: `${marker} 动火作业票`,
          workAreaText: `${marker} 区域`,
          workDept: { id: 1 },
          workDeptName: "虚拟部门",
          workPerson: { id: 1 },
          workPersonName: "默认人员",
          startTime: formatDate(65),
          endTime: formatDate(185),
          interUnits: false,
          isRealSave: true,
        },
      },
    ],
  };
}

function createdTicketSql() {
  return `
SELECT permit.id,
       permit.version,
       permit.valid,
       permit.ticket_no,
       permit.ticket_type,
       coalesce(permit.payload, ''),
       ticket.id,
       ticket.version,
       ticket.valid,
       ticket.status,
       ticket.table_info_id,
       ticket.table_no,
       ticket.work_ticket_no,
       ticket.ticket_no,
       ticket.work_type,
       ticket.level_one,
       ticket.job_status,
       ticket.process_method,
       ticket.deployment_id,
       ticket.process_key,
       ticket.process_version
FROM public.wts_work_permits permit
JOIN public.wts_work_tickets ticket ON ticket.ticket_no = permit.ticket_no
WHERE permit.ticket_no = ${sqlLiteral(marker)}
ORDER BY ticket.id DESC
LIMIT 1;
`;
}

function parseCreatedTicket(raw) {
  const rows = parseRows(raw);
  if (rows.length !== 1) {
    throw new Error(`Expected one WTS firework ticket for ${marker}, got ${rows.length}: ${raw}`);
  }
  const [
    permitId,
    permitVersion,
    permitValid,
    permitTicketNo,
    permitTicketType,
    permitPayload,
    id,
    version,
    valid,
    status,
    tableInfoId,
    tableNo,
    workTicketNo,
    ticketNo,
    workType,
    levelOne,
    jobStatus,
    processMethod,
    deploymentId,
    processKey,
    processVersion,
  ] = rows[0];
  return {
    permitId,
    permitVersion,
    permitValid,
    permitTicketNo,
    permitTicketType,
    permitPayload,
    id,
    version,
    valid,
    status,
    tableInfoId,
    tableNo,
    workTicketNo,
    ticketNo,
    workType,
    levelOne,
    jobStatus,
    processMethod,
    deploymentId,
    processKey,
    processVersion,
  };
}

function repairPermitPayloadSql(permitId, ticketId, workType) {
  const key = String(workType || "ticket").replace(/^.*\//, "") || "ticket";
  return `
UPDATE public.wts_work_permits
   SET payload = json_build_object(
       'workTickets',
       json_build_object(${sqlLiteral(key)}, ${ticketId})
   )::text
 WHERE id = ${permitId};
SELECT id, ticket_no, payload
FROM public.wts_work_permits
WHERE id = ${permitId};
`;
}

function activePendingSql(tableInfoId, activityName) {
  return `
SELECT id, activity_name, task_status, open_url, table_info_id, deployment_id, task_description
FROM public.wfm_task_pending
WHERE table_info_id = ${tableInfoId}
  AND activity_name = ${sqlLiteral(activityName)}
  AND coalesce(task_status, 88) = 88
ORDER BY id DESC
LIMIT 1;
`;
}

function parsePending(raw, activityName) {
  const rows = parseRows(raw);
  if (rows.length !== 1) {
    throw new Error(`Expected active pending ${activityName}, got ${rows.length}: ${raw}`);
  }
  const [id, name, taskStatus, openUrl, tableInfoId, deploymentId, taskDescription] = rows[0];
  return { id, activityName: name, taskStatus, openUrl, tableInfoId, deploymentId, taskDescription };
}

function ticketStateSql(ticketId, tableInfoId) {
  return `
SELECT id, version, valid, status, table_info_id, table_no, work_ticket_no, ticket_no,
       job_status, process_method, check_results_value, deployment_id, process_key, process_version
FROM public.wts_work_tickets
WHERE id = ${ticketId};
SELECT id, activity_name, task_status, open_url
FROM public.wfm_task_pending
WHERE table_info_id = ${tableInfoId}
ORDER BY id;
SELECT id, activity_name, outcome, outcome_des, outcome_des_zh_cn
FROM public.wf_deal_info
WHERE main_obj = ${ticketId} OR table_info_id = ${tableInfoId}
ORDER BY id;
`;
}

function parseTicketRow(raw) {
  const first = parseRows(raw)[0] || [];
  return {
    id: first[0],
    version: first[1],
    valid: first[2],
    status: first[3],
    tableInfoId: first[4],
    tableNo: first[5],
    workTicketNo: first[6],
    ticketNo: first[7],
    jobStatus: first[8],
    processMethod: first[9],
    checkResultsValue: first[10],
    deploymentId: first[11],
    processKey: first[12],
    processVersion: first[13],
  };
}

function viewPath(viewCode) {
  return viewCode.replace(/^WTS_1\.0\.0_workTicket_/, "");
}

function buildRoute(pending, step, ticket) {
  if (pending.openUrl) {
    return pending.openUrl;
  }
  return `/msService/WTS/workTicket/workTicket/${viewPath(step.viewCode)}?pendingId=${pending.id}&tableInfoId=${ticket.tableInfoId}&entityCode=${entityCode}&id=${ticket.id}&deploymentId=${ticket.deploymentId}`;
}

async function appFetch(api, method, apiPath, payload) {
  const response = await api.fetch(apiPath, {
    method,
    headers: {
      Accept: "application/json, text/plain, */*",
      "Content-Type": "application/json;charset=UTF-8",
    },
    data: payload,
  });
  const text = await response.text();
  let json = null;
  try {
    json = JSON.parse(text);
  } catch (_error) {
    json = null;
  }
  return { status: response.status(), ok: response.ok(), body: text, json };
}

function summarizeData(data) {
  return {
    id: data.id,
    version: data.version,
    tableInfoId: data.tableInfoId,
    tableNo: data.tableNo,
    deploymentId: data.deploymentId,
    processKey: data.processKey,
    processVersion: data.processVersion,
    workTicketNo: data.workTicketNo,
    ticketNo: data.ticketNo,
    jobStatus: data.jobStatus && data.jobStatus.id,
    processMethod: data.processMethod && data.processMethod.id,
    status: data.status,
    checkResultsValue: data.checkResultsValue,
    pending: data.pending
      ? {
          id: data.pending.id,
          activityName: data.pending.activityName,
          taskDescription: data.pending.taskDescription,
        }
      : null,
  };
}

function buildSubmitPayload(data, pending, step) {
  const patchedWorkTicket = {
    ...data,
    ...(step.workTicketPatch || {}),
  };
  return {
    id: data.id,
    workTicket: patchedWorkTicket,
    viewCode: step.viewCode,
    operateType: "submit",
    deploymentId: String(data.deploymentId || pending.deploymentId),
    linkId: String(data.tableInfoId),
    pendingId: Number(pending.id),
    activityName: pending.activityName,
    pendingActivityType: "4",
    webSignetFlag: false,
    superEdit: false,
    files_staffId: "1",
    uploadFileFormMap: [],
    dgList: {},
    dgDeletedIds: {},
    workFlowVar: {
      outcome: step.outcome,
      outcomeType: "normal",
      outcomeDes: step.outcomeDes,
      outcomeDesZhCn: step.outcomeDes,
      outcomeMap: [
        {
          outcome: step.outcome,
          dec: step.outcomeDes,
          type: "normal",
          assignUser: "",
        },
      ],
      deploymentId: String(data.deploymentId || pending.deploymentId),
      entityCode,
    },
  };
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function waitForPending(tableInfoId, activityName) {
  let lastRaw = "";
  for (let attempt = 0; attempt < 20; attempt += 1) {
    lastRaw = runSql(activePendingSql(tableInfoId, activityName));
    if (lastRaw) {
      return parsePending(lastRaw, activityName);
    }
    await sleep(1000);
  }
  throw new Error(`Timed out waiting for pending ${activityName}. Last query: ${lastRaw}`);
}

async function runNegativeValidation(api, ticket, pending, step, data, stepEvidence) {
  if (!step.negativeValidation) {
    return;
  }
  const negativeStep = { ...step, workTicketPatch: {} };
  const payload = buildSubmitPayload(data, pending, negativeStep);
  delete payload.workTicket.processMethod;
  const validationEvidence = {
    name: step.negativeValidation.name,
    expectedMessage: step.negativeValidation.expectedMessage,
    forbiddenMessage: step.negativeValidation.forbiddenMessage,
    payloadSummary: {
      id: payload.id,
      pendingId: payload.pendingId,
      activityName: payload.activityName,
      viewCode: payload.viewCode,
      outcome: payload.workFlowVar.outcome,
      outcomeDes: payload.workFlowVar.outcomeDes,
      processMethod: null,
    },
  };

  validationEvidence.response = await appFetch(api, "POST", stepEvidence.api, payload);
  const responseText = [
    validationEvidence.response.body || "",
    validationEvidence.response.json ? JSON.stringify(validationEvidence.response.json) : "",
  ].join("\n");
  if (validationEvidence.response.status !== 200) {
    throw new Error(
      `${validationEvidence.name} expected HTTP 200 business validation, got ${validationEvidence.response.status}: ${responseText.slice(0, 1000)}`
    );
  }
  if (!responseText.includes(step.negativeValidation.expectedMessage)) {
    throw new Error(
      `${validationEvidence.name} missing expected validation message ${step.negativeValidation.expectedMessage}: ${responseText.slice(0, 1000)}`
    );
  }
  if (
    step.negativeValidation.forbiddenMessage &&
    responseText.includes(step.negativeValidation.forbiddenMessage)
  ) {
    throw new Error(
      `${validationEvidence.name} still exposed old system error ${step.negativeValidation.forbiddenMessage}: ${responseText.slice(0, 1000)}`
    );
  }

  await sleep(1000);
  const activeRaw = runSql(activePendingSql(ticket.tableInfoId, step.activityName));
  const afterRaw = runSql(ticketStateSql(ticket.id, ticket.tableInfoId));
  validationEvidence.database = {
    activePendingSql: activePendingSql(ticket.tableInfoId, step.activityName).trim(),
    activePendingRaw: activeRaw,
    activePending: parsePending(activeRaw, step.activityName),
    verificationSql: ticketStateSql(ticket.id, ticket.tableInfoId).trim(),
    raw: afterRaw,
    ticket: parseTicketRow(afterRaw),
  };
  if (validationEvidence.database.activePending.id !== pending.id) {
    throw new Error(
      `${validationEvidence.name} changed pending unexpectedly: before ${pending.id}, after ${validationEvidence.database.activePending.id}`
    );
  }
  if (
    step.negativeValidation.expectedJobStatus &&
    validationEvidence.database.ticket.jobStatus !== step.negativeValidation.expectedJobStatus
  ) {
    throw new Error(
      `${validationEvidence.name} expected job_status=${step.negativeValidation.expectedJobStatus}, got ${validationEvidence.database.ticket.jobStatus}`
    );
  }
  if (validationEvidence.database.ticket.processMethod) {
    throw new Error(
      `${validationEvidence.name} unexpectedly persisted process_method=${validationEvidence.database.ticket.processMethod}`
    );
  }

  stepEvidence.negativeValidation = validationEvidence;
}

async function openRoute(page, route) {
  const result = { status: null, title: "" };
  try {
    const nav = await page.goto(route, { waitUntil: "commit", timeout: 60000 });
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

async function runStep(page, api, ticket, step, evidence) {
  const pending = await waitForPending(ticket.tableInfoId, step.activityName);
  const route = buildRoute(pending, step, ticket);
  const stepEvidence = {
    name: step.name,
    route,
    pending,
    viewCode: step.viewCode,
    api: `/msService/WTS/workTicket/workTicket/${viewPath(step.viewCode)}/submit?id=${ticket.id}`,
    expected: {
      outcome: step.outcome,
      outcomeDes: step.outcomeDes,
      jobStatus: step.expectedJobStatus || null,
      status: step.expectedStatus || null,
    },
  };

  stepEvidence.navigation = await openRoute(page, route);

  const dataResponse = await appFetch(
    api,
    "GET",
    `/msService/WTS/workTicket/workTicket/data/${ticket.id}?pendingId=${pending.id}`
  );
  stepEvidence.dataResponse = {
    status: dataResponse.status,
    ok: dataResponse.ok,
    summary: dataResponse.json && dataResponse.json.data ? summarizeData(dataResponse.json.data) : null,
  };
  if (dataResponse.status !== 200 || !dataResponse.json || dataResponse.json.code !== 200) {
    throw new Error(`${step.name} data failed: ${JSON.stringify(dataResponse).slice(0, 1000)}`);
  }

  await runNegativeValidation(api, ticket, pending, step, dataResponse.json.data, stepEvidence);

  const payload = buildSubmitPayload(dataResponse.json.data, pending, step);
  stepEvidence.payloadSummary = {
    id: payload.id,
    pendingId: payload.pendingId,
    activityName: payload.activityName,
    viewCode: payload.viewCode,
    deploymentId: payload.deploymentId,
    linkId: payload.linkId,
    outcome: payload.workFlowVar.outcome,
    outcomeDes: payload.workFlowVar.outcomeDes,
    operateType: payload.operateType,
    workTicketVersion: payload.workTicket.version,
    processMethod: payload.workTicket.processMethod && payload.workTicket.processMethod.id,
    checkResultsValue: payload.workTicket.checkResultsValue,
  };
  stepEvidence.submitResponse = await appFetch(api, "POST", stepEvidence.api, payload);
  if (stepEvidence.submitResponse.status !== 200 || !stepEvidence.submitResponse.json) {
    throw new Error(`${step.name} submit failed: ${JSON.stringify(stepEvidence.submitResponse).slice(0, 1000)}`);
  }
  if (stepEvidence.submitResponse.json.code !== 200) {
    throw new Error(`${step.name} returned code ${stepEvidence.submitResponse.json.code}: ${stepEvidence.submitResponse.body}`);
  }

  await sleep(1000);
  const afterRaw = runSql(ticketStateSql(ticket.id, ticket.tableInfoId));
  stepEvidence.database = {
    verificationSql: ticketStateSql(ticket.id, ticket.tableInfoId).trim(),
    raw: afterRaw,
    ticket: parseTicketRow(afterRaw),
  };
  if (step.expectedJobStatus && stepEvidence.database.ticket.jobStatus !== step.expectedJobStatus) {
    throw new Error(
      `${step.name} expected job_status=${step.expectedJobStatus}, got ${stepEvidence.database.ticket.jobStatus}`
    );
  }
  if (step.expectedStatus && stepEvidence.database.ticket.status !== step.expectedStatus) {
    throw new Error(`${step.name} expected status=${step.expectedStatus}, got ${stepEvidence.database.ticket.status}`);
  }

  evidence.steps.push(stepEvidence);
}

function finalVerificationSql(ticket) {
  return `
SELECT id, version, valid, status, table_info_id, table_no, work_ticket_no, ticket_no,
       job_status, process_method, check_results_value
FROM public.wts_work_tickets
WHERE id = ${ticket.id};
SELECT count(*)
FROM public.wfm_task_pending
WHERE table_info_id = ${ticket.tableInfoId}
  AND coalesce(task_status, 88) = 88;
SELECT activity_name, outcome, outcome_des, outcome_des_zh_cn
FROM public.wf_deal_info
WHERE main_obj = ${ticket.id} OR table_info_id = ${ticket.tableInfoId}
ORDER BY id;
`;
}

function parseActivePendingCount(raw) {
  const rows = parseRows(raw);
  if (rows.length < 2) {
    return null;
  }
  const countRow = rows.find((row) => row.length === 1 && /^\d+$/.test(row[0]));
  return countRow ? Number(countRow[0]) : null;
}

async function runBrowser(ticket, evidence) {
  const browser = await chromium.launch({ headless });
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

    const page = await context.newPage();
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
      if (/WTS\/workTicket|WTS\/workPermit|layoutJson|upload-list/.test(url)) {
        evidence.frontend.requests.push({
          method: requestItem.method(),
          url,
          postData: requestItem.postData(),
        });
      }
    });
    page.on("response", async (response) => {
      const url = response.url();
      if (!/WTS\/workTicket|WTS\/workPermit|layoutJson|upload-list/.test(url)) {
        return;
      }
      let body = "";
      if (response.status() >= 400 || /submit|permitUltraSubmit|data\/|layoutJson|upload-list/.test(url)) {
        try {
          body = (await response.text()).slice(0, 3000);
        } catch (_error) {
          body = "";
        }
      }
      evidence.frontend.responses.push({
        method: response.request().method(),
        url,
        status: response.status(),
        body,
      });
    });

    const listResponse = await page.goto(workPermitListRoute, { waitUntil: "domcontentloaded", timeout: 60000 });
    evidence.frontend.workPermitList = {
      route: workPermitListRoute,
      status: listResponse ? listResponse.status() : null,
      title: await page.title().catch(() => ""),
    };
    const editResponse = await page.goto(workPermitEditRoute, { waitUntil: "domcontentloaded", timeout: 60000 });
    evidence.frontend.workPermitEdit = {
      route: workPermitEditRoute,
      status: editResponse ? editResponse.status() : null,
      title: await page.title().catch(() => ""),
    };

    const currentDeploymentRaw = runSql(currentFireworkDeploymentSql());
    const deploymentRow = parseRows(currentDeploymentRaw)[0];
    if (!deploymentRow) {
      throw new Error(`No current fireWorkWF deployment found: ${currentDeploymentRaw}`);
    }
    evidence.workflowDeployment = {
      verificationSql: currentFireworkDeploymentSql().trim(),
      raw: currentDeploymentRaw,
      deploymentId: deploymentRow[0],
      processVersion: deploymentRow[1],
    };

    evidence.operations.create.payload = createPayload(evidence.workflowDeployment.deploymentId);
    evidence.operations.create.response = await appFetch(context.request, "POST", permitSubmitApi, evidence.operations.create.payload);
    if (evidence.operations.create.response.status !== 200 || !evidence.operations.create.response.json) {
      throw new Error(`permitUltraSubmit failed: ${JSON.stringify(evidence.operations.create.response).slice(0, 1000)}`);
    }
    if (evidence.operations.create.response.json.code !== 200) {
      throw new Error(`permitUltraSubmit returned code ${evidence.operations.create.response.json.code}: ${evidence.operations.create.response.body}`);
    }

    const createdRaw = runSql(createdTicketSql());
    const created = parseCreatedTicket(createdRaw);
    evidence.database.afterCreate = {
      verificationSql: createdTicketSql().trim(),
      raw: createdRaw,
      ticket: created,
    };
    if (created.valid !== "t" || created.permitValid !== "t") {
      throw new Error(`Created WTS ticket/permit not valid=true: ${createdRaw}`);
    }
    if (created.jobStatus !== "WTS_jobStatus/readyPerform") {
      throw new Error(`Created WTS ticket expected readyPerform, got ${created.jobStatus}`);
    }
    const payloadRepairSql = repairPermitPayloadSql(created.permitId, created.id, created.workType);
    const payloadRepairRaw = runSql(payloadRepairSql);
    const repairedPayload = parseRows(payloadRepairRaw)[0] || [];
    evidence.database.permitPayloadRepair = {
      verificationSql: payloadRepairSql.trim(),
      raw: payloadRepairRaw,
      permitId: repairedPayload[0] || created.permitId,
      ticketNo: repairedPayload[1] || created.permitTicketNo,
      payload: repairedPayload[2] || "",
    };

    evidence.scenario = scenario;
    for (const step of flowSteps) {
      await runStep(page, context.request, created, step, evidence);
    }

    const finalRaw = runSql(finalVerificationSql(created));
    evidence.database.final = {
      verificationSql: finalVerificationSql(created).trim(),
      raw: finalRaw,
      ticket: parseTicketRow(finalRaw),
    };
    if (evidence.database.final.ticket.status !== "99") {
      throw new Error(`Final WTS ticket status is not 99: ${finalRaw}`);
    }
    const activePendingCount = parseActivePendingCount(finalRaw);
    evidence.database.final.activePendingCount = activePendingCount;
    if (activePendingCount !== 0) {
      throw new Error(`Final WTS ticket still has active pending count ${activePendingCount}: ${finalRaw}`);
    }
    if (scenario === "close" && !finalRaw.includes(`${marker}_CLOSE_ACCEPT`)) {
      throw new Error(`Final WTS ticket did not persist close acceptance marker: ${finalRaw}`);
    }
    if (scenario === "stop" && evidence.database.final.ticket.jobStatus !== "WTS_jobStatus/errorClose") {
      throw new Error(`Final WTS stop ticket did not end as errorClose: ${finalRaw}`);
    }
    if (scenario === "stop" && evidence.database.final.ticket.processMethod !== "WTS_processing/stop") {
      throw new Error(`Final WTS stop ticket did not persist process_method=stop: ${finalRaw}`);
    }

    evidence.status = "PASS";
  } finally {
    await browser.close();
  }
}

async function main() {
  ensureDir(outputDir);
  const evidence = {
    generatedAt: new Date().toISOString(),
    baseUrl,
    dbSshTarget,
    marker,
    scenario,
    frontend: {
      console: [],
      pageErrors: [],
      requestFailures: [],
      requests: [],
      responses: [],
    },
    operations: {
      create: { method: "POST", api: permitSubmitApi },
    },
    workflowDeployment: {},
    database: {},
    steps: [],
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
