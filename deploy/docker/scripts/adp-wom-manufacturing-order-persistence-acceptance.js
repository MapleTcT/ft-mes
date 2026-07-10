#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { execFileSync } = require("child_process");
const { chromium, request } = require("playwright");

const baseUrl = (process.env.ADP_BASE_URL || "http://100.99.133.43:18080").replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD || "123456";
const dbSshTarget = process.env.ADP_DB_SSH_TARGET || "v6@100.99.133.43";
const dbContainer = process.env.ADP_DB_CONTAINER || "adp-mes-newbase-postgres-1";
const dbName = process.env.ADP_DB_NAME || "adp";
const dbUser = process.env.ADP_DB_USER || "adp";
const headless = process.env.ADP_HEADLESS !== "false";
const nowToken = new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14);
const marker = process.env.ADP_E2E_MARKER || `ADP_E2E_${nowToken}_WOM_MANUFACTURING_ORDER`;
const outputDir = process.env.ADP_OUTPUT_DIR || path.join("/tmp", `adp-wom-manufacturing-order-${nowToken}`);
const outputPath = process.env.ADP_OUTPUT_PATH || path.join(outputDir, "wom-manufacturing-order-results.json");
const screenshotPath = path.join(outputDir, "manufacturing-order-list.png");

const fixture = {
  prodCode: process.env.ADP_WOM_PROD_CODE || "ADP_E2E_20260618200829_WOM_CHECKOUTBILL_MAT",
  formulaCode: process.env.ADP_WOM_FORMULA_CODE || "ADP_E2E_20260618200829_WOM_CHECKOUTBILL_FORM",
  workLineId: Number(process.env.ADP_WOM_WORK_LINE_ID || 8991071330917351),
  batchCode: `${marker}_BATCH`,
  unitId: Number(process.env.ADP_WOM_FIXTURE_UNIT_ID || Date.now() * 1000 + 731),
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

async function browserFetch(page, method, apiPath, payload) {
  return page.evaluate(
    async ({ method: requestMethod, apiPath: pathValue, payload: bodyValue }) => {
      const response = await fetch(pathValue, {
        method: requestMethod,
        headers: {
          Accept: "application/json, text/plain, */*",
          "Content-Type": "application/json;charset=UTF-8",
        },
        body: bodyValue === undefined ? undefined : JSON.stringify(bodyValue),
      });
      const text = await response.text();
      let json = null;
      try {
        json = JSON.parse(text);
      } catch (_error) {
        json = null;
      }
      return { status: response.status, ok: response.ok, body: text, json };
    },
    { method, apiPath, payload }
  );
}

function taskSql() {
  return `
SELECT id, version, valid, status, table_info_id, table_no, deployment_id,
       process_key, produce_batch_num,
       CASE WHEN remark IS NULL THEN '' ELSE convert_from(lo_get(remark), 'UTF8') END
FROM public.wom_produce_tasks
WHERE produce_batch_num = ${sqlLiteral(fixture.batchCode)} AND valid = true
ORDER BY create_time DESC;
`;
}

function taskByIdSql(id) {
  return `
SELECT id, version, valid, status, table_info_id, table_no, deployment_id,
       process_key, produce_batch_num,
       CASE WHEN remark IS NULL THEN '' ELSE convert_from(lo_get(remark), 'UTF8') END
FROM public.wom_produce_tasks
WHERE id = ${Number(id)};
`;
}

function pendingSql(id) {
  return `
SELECT id, activity_name, activity_type, deployment_id, table_info_id,
       COALESCE(open_url, ''), task_status
FROM public.wfm_task_pending
WHERE model_id = ${Number(id)} AND task_status = 88
ORDER BY create_time DESC;
`;
}

function dealInfoSql(tableInfoId) {
  return `
SELECT activity_name, COALESCE(outcome, ''), COALESCE(outcome_des, ''),
       COALESCE(outcome_des_zh_cn, ''), dealinfo_type
FROM public.wf_deal_info
WHERE table_info_id = ${Number(tableInfoId)}
ORDER BY create_time;
`;
}

function parseTask(raw) {
  const rows = parseRows(raw);
  if (rows.length !== 1) {
    throw new Error(`Expected exactly one task row, got ${rows.length}: ${raw}`);
  }
  const [id, version, valid, status, tableInfoId, tableNo, deploymentId, processKey, batchCode, remark] = rows[0];
  return { id, version, valid, status, tableInfoId, tableNo, deploymentId, processKey, batchCode, remark };
}

function parsePending(raw) {
  const rows = parseRows(raw);
  if (rows.length !== 1) {
    throw new Error(`Expected exactly one active pending, got ${rows.length}: ${raw}`);
  }
  const [id, activityName, activityType, deploymentId, tableInfoId, openUrl, taskStatus] = rows[0];
  return { id, activityName, activityType, deploymentId, tableInfoId, openUrl, taskStatus };
}

function prepareMaterialUnit() {
  const materialSql = `
SELECT id, COALESCE(produce_unit::text, '')
FROM public.baseset_materials
WHERE code = ${sqlLiteral(fixture.prodCode)} AND valid = true;
`;
  const rows = parseRows(runSql(materialSql));
  if (rows.length !== 1) {
    throw new Error(`Expected one fixture material ${fixture.prodCode}, got ${rows.length}`);
  }
  const [materialId, originalUnitId] = rows[0];
  if (originalUnitId) {
    return { changed: false, materialId, originalUnitId, verificationSql: materialSql.trim() };
  }
  const setupSql = `
BEGIN;
INSERT INTO public.baseset_units (
  id, version, valid, cid, create_staff_id, create_time, create_department_id,
  create_position_id, group_id, owner_staff_id, owner_department_id,
  owner_position_id, position_lay_rec, status, table_no, table_info_id,
  code, name, symbol, accuracy
) VALUES (
  ${fixture.unitId}, 0, true, 1000, 1, now(), 1, 1, 1000, 1, 1, 1, '1',
  99, ${sqlLiteral(`${marker}_UNIT_TN`)}, ${fixture.unitId},
  ${sqlLiteral(`${marker}_UNIT`)}, '件', '件', 0
);
UPDATE public.baseset_materials
SET produce_unit = ${fixture.unitId}, modify_time = now()
WHERE id = ${Number(materialId)};
COMMIT;
`;
  runSql(setupSql);
  return {
    changed: true,
    materialId,
    originalUnitId,
    setupSql: setupSql.trim(),
    verificationSql: materialSql.trim(),
    afterSetup: runSql(materialSql),
  };
}

function restoreMaterialUnit(setup) {
  if (!setup || !setup.changed) return { changed: false };
  const originalValue = setup.originalUnitId ? Number(setup.originalUnitId) : "NULL";
  const cleanupSql = `
BEGIN;
UPDATE public.baseset_materials
SET produce_unit = ${originalValue}, modify_time = now()
WHERE id = ${Number(setup.materialId)};
DELETE FROM public.baseset_units WHERE id = ${fixture.unitId};
COMMIT;
`;
  runSql(cleanupSql);
  return {
    changed: true,
    cleanupSql: cleanupSql.trim(),
    materialAfterCleanup: runSql(`SELECT id, COALESCE(produce_unit::text, '') FROM public.baseset_materials WHERE id = ${Number(setup.materialId)};`),
    fixtureUnitCount: runSql(`SELECT count(*) FROM public.baseset_units WHERE id = ${fixture.unitId};`),
  };
}

function workflowEvidence(deploymentId, activityName) {
  const metadataSql = `
SELECT code, name_zh_cn, from_node_code, to_node_code, type
FROM public.wf_transition
WHERE deployment_id = ${Number(deploymentId)} AND from_node_code = ${sqlLiteral(activityName)}
ORDER BY id;
`;
  const xmlSql = `
SELECT COALESCE(process_xml_text_backup, '')
FROM public.wf_deployment
WHERE id = ${Number(deploymentId)};
`;
  const metadataRaw = runSql(metadataSql);
  const processXml = runSql(xmlSql);
  const escapedActivity = activityName.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  const taskMatch = processXml.match(new RegExp(`<task[^>]+name="${escapedActivity}"[\\s\\S]*?<\\/task>`));
  if (!taskMatch) {
    throw new Error(`Runtime process XML has no task node ${activityName}`);
  }
  const transitions = [];
  const transitionPattern = /<transition\s+([^>]+)>/g;
  let transitionMatch;
  while ((transitionMatch = transitionPattern.exec(taskMatch[0]))) {
    const attrs = transitionMatch[1];
    const read = (name) => {
      const match = attrs.match(new RegExp(`${name}="([^"]*)"`));
      return match ? match[1] : "";
    };
    transitions.push({
      name: read("name"),
      description: read("desc"),
      to: read("to"),
      cancel: read("cancel"),
    });
  }
  const selected = transitions.find((item) => item.cancel !== "1" && item.description === "生效") ||
    transitions.find((item) => item.cancel !== "1");
  if (!selected || !selected.name) {
    throw new Error(`Runtime process XML has no non-cancel transition for ${activityName}`);
  }
  return {
    metadataSql: metadataSql.trim(),
    metadataRaw,
    processXmlSql: xmlSql.trim(),
    runtimeTransitions: transitions,
    selected,
    metadataDrift: !metadataRaw.includes(selected.name) ||
      parseRows(metadataRaw).some((row) => !transitions.some((item) => item.name === row[0])),
  };
}

function createPayload() {
  const start = new Date();
  const end = new Date(start.getTime() + 60 * 60 * 1000);
  const format = (value) => value.toISOString().slice(0, 19).replace("T", " ");
  return [
    {
      prodCode: fixture.prodCode,
      formulaCode: fixture.formulaCode,
      workLineId: fixture.workLineId,
      planNum: 1,
      planStartDate: format(start),
      planEndDate: format(end),
      batchCode: fixture.batchCode,
      needPack: false,
    },
  ];
}

function submitPayload(data, pending, transition) {
  return {
    id: Number(data.id),
    produceTask: {
      id: Number(data.id),
      version: Number(data.version),
      remark: `${marker}_SUBMITTED`,
    },
    viewCode: "WOM_1.0.0_produceTask_makeTaskEdit",
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
      outcome: transition.name,
      outcomeType: "normal",
      outcomeDes: transition.description,
      outcomeDesZhCn: transition.description,
      outcomeMap: [
        {
          outcome: transition.name,
          dec: transition.description,
          type: "normal",
          assignUser: "",
        },
      ],
      deploymentId: String(data.deploymentId || pending.deploymentId),
      entityCode: "WOM_1.0.0_produceTask",
    },
  };
}

async function main() {
  ensureDir(outputDir);
  const evidence = {
    schemaVersion: 1,
    generatedAt: new Date().toISOString(),
    database: "PostgreSQL",
    baseUrl,
    dbSshTarget,
    marker,
    fixture,
    route: "/msService/WOM/produceTask/produceTask/makeTaskList",
    status: "RUNNING",
    frontend: { console: [], pageErrors: [], requestFailures: [], requests: [], responses: [] },
    operations: {},
    persistence: {},
    issues: [],
  };
  let browser;
  let page;
  let trackedTask;
  let rollbackComplete = false;
  let materialUnitSetup;
  try {
    const api = await request.newContext({ ignoreHTTPSErrors: true });
    const loginResult = await login(api);
    await api.dispose();
    evidence.login = { status: loginResult.status, authenticated: true };
    materialUnitSetup = prepareMaterialUnit();
    evidence.testSetup = { materialUnit: materialUnitSetup };

    browser = await chromium.launch({ headless });
    const context = await browser.newContext({
      baseURL: baseUrl,
      ignoreHTTPSErrors: true,
      viewport: { width: 1600, height: 1000 },
      extraHTTPHeaders: { Authorization: `Bearer ${loginResult.ticket}` },
    });
    await context.addCookies([
      { name: "suposTicket", value: loginResult.ticket, url: baseUrl },
      { name: "SUPOS_TICKET", value: loginResult.ticket, url: baseUrl },
    ]);
    await context.addInitScript((token) => {
      window.localStorage.clear();
      window.sessionStorage.clear();
      ["suposTicket", "SUPOS_TICKET", "token", "ticket"].forEach((key) => {
        window.localStorage.setItem(key, token);
        window.sessionStorage.setItem(key, token);
      });
    }, loginResult.ticket);

    page = await context.newPage();
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
      if (/WOM\/produceTask\/produceTask/.test(requestItem.url())) {
        evidence.frontend.requests.push({
          method: requestItem.method(),
          url: requestItem.url(),
          postData: requestItem.postData(),
        });
      }
    });
    page.on("response", async (response) => {
      if (!/WOM\/produceTask\/produceTask/.test(response.url())) return;
      evidence.frontend.responses.push({ url: response.url(), status: response.status() });
    });

    const navigation = await page.goto(evidence.route, { waitUntil: "domcontentloaded", timeout: 60000 });
    evidence.frontend.navigation = { status: navigation ? navigation.status() : null, title: await page.title() };
    await page.waitForTimeout(2500);
    await page.screenshot({ path: screenshotPath, fullPage: true });
    evidence.frontend.screenshot = screenshotPath;

    const preexistingRaw = runSql(taskSql());
    if (preexistingRaw) {
      const preexisting = parseTask(preexistingRaw);
      if (preexisting.valid === "t") {
        const cleanupPayload = { ids: `${preexisting.id}@${preexisting.version}` };
        evidence.operations.preflightCleanup = {
          method: "POST",
          api: "/msService/WOM/produceTask/produceTask/delete",
          payload: cleanupPayload,
        };
        evidence.operations.preflightCleanup.response = await browserFetch(
          page,
          "POST",
          evidence.operations.preflightCleanup.api,
          cleanupPayload
        );
        const cleaned = parseTask(runSql(taskByIdSql(preexisting.id)));
        evidence.persistence.preflightCleanup = { before: preexisting, after: cleaned };
        if (evidence.operations.preflightCleanup.response.status !== 200 || cleaned.valid !== "f") {
          throw new Error(`Preflight cleanup failed: ${JSON.stringify(evidence.operations.preflightCleanup)}`);
        }
      }
    }

    const createApi = "/msService/WOM/produceTask/produceTask/produceTaskCreated2";
    const creationPayload = createPayload();
    evidence.operations.create = { method: "POST", api: createApi, payload: creationPayload };
    evidence.operations.create.response = await browserFetch(page, "POST", createApi, creationPayload);
    if (evidence.operations.create.response.status !== 200) {
      throw new Error(`Create failed: ${JSON.stringify(evidence.operations.create.response)}`);
    }

    const createdRaw = runSql(taskSql());
    let task = parseTask(createdRaw);
    trackedTask = task;
    evidence.persistence.afterCreate = { sql: taskSql().trim(), raw: createdRaw, task };
    if (task.valid !== "t" || task.status !== "88") {
      throw new Error(`Created task did not persist as valid/status=88: ${createdRaw}`);
    }

    const pendingRaw = runSql(pendingSql(task.id));
    const pending = parsePending(pendingRaw);
    evidence.persistence.pending = { sql: pendingSql(task.id).trim(), raw: pendingRaw, pending };
    const workflow = workflowEvidence(task.deploymentId, pending.activityName);
    evidence.persistence.workflow = workflow;

    const dataApi = `/msService/WOM/produceTask/produceTask/data/${task.id}?pendingId=${pending.id}`;
    evidence.operations.getData = { method: "GET", api: dataApi };
    const getDataResponse = await browserFetch(page, "GET", dataApi);
    if (getDataResponse.status !== 200) {
      throw new Error(`Get data failed: ${JSON.stringify(getDataResponse)}`);
    }
    const data = getDataResponse.json && Object.prototype.hasOwnProperty.call(getDataResponse.json, "data")
      ? getDataResponse.json.data
      : getDataResponse.json;
    if (!data || !data.id) {
      throw new Error(`Get data did not return a task: ${getDataResponse.body.slice(0, 500)}`);
    }
    evidence.operations.getData.response = {
      status: getDataResponse.status,
      ok: getDataResponse.ok,
      data: {
        id: data.id,
        version: data.version,
        tableInfoId: data.tableInfoId,
        deploymentId: data.deploymentId,
        pendingId: data.pending && data.pending.id,
      },
    };

    const submitApi = `/msService/WOM/produceTask/produceTask/makeTaskEdit/submit?id=${task.id}`;
    const submissionPayload = submitPayload(data, pending, workflow.selected);
    evidence.operations.submit = { method: "POST", api: submitApi, payload: submissionPayload };
    evidence.operations.submit.response = await browserFetch(page, "POST", submitApi, submissionPayload);
    if (evidence.operations.submit.response.status !== 200) {
      throw new Error(`Submit failed: ${JSON.stringify(evidence.operations.submit.response)}`);
    }

    const submittedRaw = runSql(taskByIdSql(task.id));
    task = parseTask(submittedRaw);
    const submittedPendingRaw = runSql(pendingSql(task.id));
    const dealRaw = runSql(dealInfoSql(task.tableInfoId));
    evidence.persistence.afterSubmit = {
      sql: taskByIdSql(task.id).trim(),
      raw: submittedRaw,
      task,
      pendingSql: pendingSql(task.id).trim(),
      pendingRaw: submittedPendingRaw,
      dealInfoSql: dealInfoSql(task.tableInfoId).trim(),
      dealInfoRaw: dealRaw,
    };
    if (task.status !== "99" || submittedPendingRaw !== "" || !dealRaw.includes(workflow.selected.name)) {
      throw new Error(`Submit persistence mismatch: task=${submittedRaw} pending=${submittedPendingRaw} deal=${dealRaw}`);
    }

    const deleteApi = "/msService/WOM/produceTask/produceTask/delete";
    const deletePayload = { ids: `${task.id}@${task.version}` };
    evidence.operations.rollback = { method: "POST", api: deleteApi, payload: deletePayload };
    evidence.operations.rollback.response = await browserFetch(page, "POST", deleteApi, deletePayload);
    if (evidence.operations.rollback.response.status !== 200) {
      throw new Error(`Rollback delete failed: ${JSON.stringify(evidence.operations.rollback.response)}`);
    }

    const rolledBackRaw = runSql(taskByIdSql(task.id));
    const rolledBack = parseTask(rolledBackRaw);
    evidence.persistence.afterRollback = { sql: taskByIdSql(task.id).trim(), raw: rolledBackRaw, task: rolledBack };
    if (rolledBack.valid !== "f") {
      throw new Error(`Rollback did not soft-delete the task: ${rolledBackRaw}`);
    }
    rollbackComplete = true;

    evidence.status = "PASS";
    evidence.summary = {
      createPersisted: true,
      submitPersisted: true,
      rollbackVerified: true,
      selectedRuntimeTransition: workflow.selected.name,
      workflowMetadataDrift: workflow.metadataDrift,
    };
  } catch (error) {
    evidence.status = "FAIL";
    evidence.issues.push(error && error.stack ? error.stack : String(error));
    if (page && trackedTask && !rollbackComplete) {
      try {
        const current = parseTask(runSql(taskByIdSql(trackedTask.id)));
        if (current.valid === "t") {
          const cleanupPayload = { ids: `${current.id}@${current.version}` };
          const cleanupResponse = await browserFetch(
            page,
            "POST",
            "/msService/WOM/produceTask/produceTask/delete",
            cleanupPayload
          );
          const afterCleanup = parseTask(runSql(taskByIdSql(current.id)));
          evidence.emergencyRollback = { cleanupPayload, cleanupResponse, afterCleanup };
          rollbackComplete = afterCleanup.valid === "f";
        }
      } catch (rollbackError) {
        evidence.issues.push(`Emergency rollback failed: ${rollbackError.stack || rollbackError}`);
      }
    }
    process.exitCode = 1;
  } finally {
    if (browser) await browser.close();
    try {
      evidence.testCleanup = { materialUnit: restoreMaterialUnit(materialUnitSetup) };
      if (evidence.testCleanup.materialUnit.changed && evidence.testCleanup.materialUnit.fixtureUnitCount !== "0") {
        throw new Error("Fixture unit cleanup did not remove the temporary unit");
      }
    } catch (cleanupError) {
      evidence.status = "FAIL";
      evidence.issues.push(`Fixture cleanup failed: ${cleanupError.stack || cleanupError}`);
      process.exitCode = 1;
    }
    fs.writeFileSync(outputPath, `${JSON.stringify(evidence, null, 2)}\n`);
    console.log(JSON.stringify({ status: evidence.status, marker, outputPath }, null, 2));
  }
}

main();
