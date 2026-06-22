#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { execFileSync } = require("child_process");
const { chromium, request } = require("playwright");

const baseUrl = (process.env.ADP_BASE_URL || "http://100.99.133.43:18080").replace(/\/+$/, "");
const browserBaseUrl = (process.env.ADP_BROWSER_BASE_URL || baseUrl).replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD || "123456";
const headless = process.env.ADP_HEADLESS !== "false";
const dbSshTarget = process.env.ADP_DB_SSH_TARGET || "v6@100.99.133.43";
const dbSshPassword = process.env.ADP_DB_SSH_PASSWORD || "";
const dbContainer = process.env.ADP_DB_CONTAINER || "adp-mes-newbase-postgres-1";
const dbName = process.env.ADP_DB_NAME || "adp";
const dbUser = process.env.ADP_DB_USER || "adp";
const pageTimeoutMs = Number(process.env.ADP_PAGE_TIMEOUT_MS || 180000);
const gridTimeoutMs = Number(process.env.ADP_GRID_TIMEOUT_MS || pageTimeoutMs);
const nowToken = new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14);
const transitionConfigs = {
  start: {
    label: "开始",
    buttonId: "btn-startTask",
    requestState: "start",
    expectedState: "WOM_runState/runing",
    confirm: true,
  },
  hold: {
    label: "保持",
    buttonId: "btn-pauseTask",
    requestState: "hold",
    expectedState: "WOM_runState/iskeep",
    confirm: false,
  },
  restart: {
    label: "重启",
    buttonId: "btn-recoveryTask",
    requestState: "restart",
    expectedState: "WOM_runState/runing",
    confirm: false,
  },
  stop: {
    label: "结束",
    buttonId: "btn-stopTask",
    expectedState: "WOM_runState/finished",
    special: "finishWithoutOutputDetails",
  },
  "stop-output": {
    label: "结束（产出明细）",
    buttonId: "btn-stopTask",
    expectedState: "WOM_runState/finished",
    special: "finishWithOutputDetail",
    expectedFinishNum: 5,
  },
  "advance-release": {
    label: "提前放料",
    buttonId: "btn-earlyPutIn",
    expectedState: "WOM_runState/runing",
    special: "advanceRelease",
  },
};
const transitions = (process.env.ADP_WOM_TRANSITIONS || "start")
  .split(",")
  .map((value) => value.trim())
  .filter(Boolean);
for (const transition of transitions) {
  if (!transitionConfigs[transition]) {
    throw new Error(`Unsupported ADP_WOM_TRANSITIONS item: ${transition}`);
  }
}
const transitionName = transitions.join("-");
const defaultMarkerSuffix = transitionName === "start" ? "WOMSTART" : `WOM${transitionName.replace(/[^a-z0-9]+/gi, "_").toUpperCase()}`;
const marker = process.env.ADP_E2E_MARKER || `ADP_E2E_${nowToken}_${defaultMarkerSuffix}`;
const outputDir =
  process.env.ADP_OUTPUT_DIR ||
  path.join("/tmp", `adp-wom-${transitionName}-persistence-${nowToken}`);
const outputPath =
  process.env.ADP_WOM_STATE_PERSISTENCE_OUTPUT ||
  process.env.ADP_WOM_START_PERSISTENCE_OUTPUT ||
  path.join(outputDir, `wom-${transitionName}-persistence-results.json`);
// Keep marker rows above older seed data but below Number.MAX_SAFE_INTEGER for legacy frontend JSON handling.
const idBase = 9000000000000000n + BigInt(Date.now() % 700000000) * 10n + BigInt(process.pid % 10);
const ids = {
  material: idBase + 1n,
  formula: idBase + 2n,
  task: idBase + 3n,
  wait: idBase + 4n,
  pending: idBase + 5n,
  outputDetail: idBase + 6n,
};
const tableNo = `${marker}_TASK_TN`;
const materialCode = `${marker}_MAT`;
const formulaCode = `${marker}_FORM`;
const batchNo = `${marker}_BATCH`;
const isAdvanceReleaseOnly = transitions.length === 1 && transitions[0] === "advance-release";
const initialTaskRunState = isAdvanceReleaseOnly ? "WOM_runState/runing" : "WOM_runState/waitForRun";
const initialAdvanceChargeSql = isAdvanceReleaseOnly ? "true" : "false";
// The recovered list sorts pending rows by act_start_time; keep marker rows ahead of accumulated E2E history.
const initialActStartTimeSql = isAdvanceReleaseOnly ? "now()" : "now() + interval '30 days'";
const initialFeedConditionSql = isAdvanceReleaseOnly ? sqlLiteral(`${marker} feed condition`) : "NULL";
const route = "/msService/WOM/produceTask/produceTask/makeTaskList";
const gridId = "WOM_1.0.0_produceTask_makeTaskList_produceTask_sdg";

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true });
}

async function captureScreenshot(page, filePath, evidence, label) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  try {
    await page.screenshot({
      path: filePath,
      fullPage: true,
      timeout: Math.min(pageTimeoutMs, 30000),
    });
    return { ok: true, path: filePath };
  } catch (error) {
    const item = {
      label,
      path: filePath,
      error: error.message,
    };
    evidence.screenshotFailures = evidence.screenshotFailures || [];
    evidence.screenshotFailures.push(item);
    return { ok: false, ...item };
  }
}

function shellQuote(value) {
  return `'${String(value).replace(/'/g, "'\\''")}'`;
}

function sqlLiteral(value) {
  return `'${String(value).replace(/'/g, "''")}'`;
}

function runRemote(command, input) {
  const commonArgs = [
    "-o",
    "StrictHostKeyChecking=no",
    "-o",
    "UserKnownHostsFile=/dev/null",
  ];
  if (dbSshPassword) {
    return execFileSync("sshpass", ["-e", "ssh", ...commonArgs, dbSshTarget, command], {
      input,
      encoding: "utf8",
      env: { ...process.env, SSHPASS: dbSshPassword },
      stdio: ["pipe", "pipe", "pipe"],
    });
  }
  return execFileSync("ssh", ["-o", "BatchMode=yes", ...commonArgs, dbSshTarget, command], {
    input,
    encoding: "utf8",
    stdio: ["pipe", "pipe", "pipe"],
  });
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

function seedSql() {
  const commonCols =
    "version, valid, cid, create_staff_id, create_time, create_department_id, create_position_id, group_id, owner_staff_id, owner_department_id, owner_position_id, position_lay_rec";
  const commonVals = "0, true, 1000, 1, now(), 1, 1, 1000, 1, 1, 1, '1'";
  return `
BEGIN;

INSERT INTO public.baseset_materials (
  id, ${commonCols}, code, name, table_no
) VALUES (
  ${ids.material}, ${commonVals}, ${sqlLiteral(materialCode)}, ${sqlLiteral(`${marker} material`)}, ${sqlLiteral(`${materialCode}_TN`)}
) ON CONFLICT (id) DO NOTHING;

INSERT INTO public.rm_formulas (
  id, ${commonCols}, formual_code, formula_name, set_process, product_id, table_no
) VALUES (
  ${ids.formula}, ${commonVals}, ${sqlLiteral(formulaCode)}, ${sqlLiteral(`${marker} formula`)}, 'RM_formulaType/simpleFormula', ${ids.material}, ${sqlLiteral(`${formulaCode}_TN`)}
) ON CONFLICT (id) DO NOTHING;

INSERT INTO public.wom_produce_tasks (
  id, ${commonCols}, status, table_no, table_info_id, batch_contral, finish_num,
  formula_id, plan_num, plan_start_time, plan_end_time, produce_batch_num,
  product_id, task_run_state, task_type, need_pack, is_analy, is_abnormal,
  is_prepared, advance_charge, is_advanced, feed_condition, act_start_time, remark
) VALUES (
  ${ids.task}, ${commonVals}, 99, ${sqlLiteral(tableNo)}, ${ids.task}, false, 0,
  ${ids.formula}, 1, now() - interval '1 day', now() - interval '23 hours', ${sqlLiteral(batchNo)},
  ${ids.material}, ${sqlLiteral(initialTaskRunState)}, 'WOM_taskType/manufacture', false, false, false,
  false, ${initialAdvanceChargeSql}, false, ${initialFeedConditionSql}, ${initialActStartTimeSql}, NULL
) ON CONFLICT (id) DO NOTHING;

INSERT INTO public.wom_wait_put_records (
  id, ${commonCols}, status, table_no, table_info_id, formula_id, plan_num,
  plan_start_time, plan_end_time, produce_batch_num, product_id, record_type,
  exe_state, batch_sync_status, task_id, material_id, material_code, material_name,
  product_code, product_name, formula_code, remark
) VALUES (
  ${ids.wait}, ${commonVals}, 99, ${sqlLiteral(`${marker}_WAIT_TN`)}, ${ids.wait}, ${ids.formula}, 1,
	  now() - interval '1 day', now() - interval '23 hours', ${sqlLiteral(batchNo)}, ${ids.material}, 'WOM_recordType/workOrder',
  'WOM_runState/waitForRun', 'WOM_BatchSyncStatus/await', ${ids.task}, ${ids.material},
  ${sqlLiteral(materialCode)}, ${sqlLiteral(`${marker} material`)},
  ${sqlLiteral(materialCode)}, ${sqlLiteral(`${marker} material`)}, ${sqlLiteral(formulaCode)}, NULL
) ON CONFLICT (id) DO NOTHING;

INSERT INTO public.wfm_task_pending (
  id, user_id, app_id, task_description, activity_type, activity_name, task_status,
  open_url, process_key, process_version, process_description, process_id,
  table_info_id, entity_code, table_no, deployment_id, cid, start_time,
  create_time, creator, version, row_version
) VALUES (
  ${ids.pending}, 1, 'WOM_1.0.0', '开始活动(ec.flowActive.dai)', '1', 'start_dhbmq3g', 1,
  '/msService/WOM/produceTask/produceTask/makeTaskEdit', 'makeTaskFlow', 1, '开始活动',
  ${sqlLiteral(`ADP_E2E_PROCESS_${marker}`)}, ${ids.task}, 'WOM_1.0.0_produceTask',
	  ${sqlLiteral(tableNo)}, 1, 1000, now() - interval '1 day', now(), 'admin', 0, 0
) ON CONFLICT (id) DO NOTHING;

COMMIT;

SELECT 'seed', ${sqlLiteral(marker)}, ${ids.task}, ${ids.wait}, ${ids.pending}, ${sqlLiteral(tableNo)};
`;
}

function verificationSql() {
  return `
SELECT 'task', id, table_no, task_run_state, coalesce(act_start_time::text, ''), coalesce(act_end_time::text, ''), coalesce(finish_num::text, ''), coalesce(version::text, ''), coalesce(advance_charge::text, ''), coalesce(is_advanced::text, ''), coalesce(feed_condition, '')
FROM public.wom_produce_tasks
WHERE id = ${ids.task};

SELECT 'wait', id, task_id, exe_state, coalesce(actual_start_time::text, ''), coalesce(actual_end_time::text, ''), coalesce(proc_report_id::text, ''), coalesce(batch_sync_status, '')
FROM public.wom_wait_put_records
WHERE task_id = ${ids.task};

SELECT 'proc', id, task_id, coalesce(proc_report_type, ''), coalesce(exe_system, ''), coalesce(produce_time::text, ''), coalesce(is_finish::text, ''), coalesce(valid::text, '')
FROM public.wom_proc_reports
WHERE task_id = ${ids.task}
ORDER BY id;

SELECT 'exelog', id, task_id, coalesce(task_run_state, ''), coalesce(act_start_time::text, ''), coalesce(act_end_time::text, ''), coalesce(finish_num::text, ''), coalesce(produce_batch_num, ''), coalesce(valid::text, '')
FROM public.wom_produce_task_exelog
WHERE task_id = ${ids.task}
ORDER BY id;

SELECT 'outputDetail', id, coalesce(head_id::text, ''), coalesce(material_batch_num, ''), coalesce(output_num::text, ''), coalesce(report_num::text, ''), coalesce(product::text, ''), coalesce(remain_operate, ''), coalesce(valid::text, '')
FROM public.wom_output_details
WHERE id = ${ids.outputDetail};

SELECT 'outputDetailCount', count(*)::text
FROM public.wom_output_details
WHERE head_id IN (
  SELECT id
  FROM public.wom_proc_reports
  WHERE task_id = ${ids.task}
    AND proc_report_type = 'WOM_procReportType/produceTask'
    AND coalesce(valid, true) IS TRUE
);

SELECT 'taskActItemsCount', count(*)::text
FROM public.wom_task_act_itemss
WHERE produce_batch_num = ${sqlLiteral(batchNo)};

SELECT 'matOutptRecordCount', count(*)::text
FROM public.wom_mat_outpt_records
WHERE out_mat_detail_id = ${ids.outputDetail};
`;
}

function seedOutputDetailSql(procReportId) {
  const commonCols =
    "version, valid, cid, create_staff_id, create_time, create_department_id, create_position_id, group_id, owner_staff_id, owner_department_id, owner_position_id, position_lay_rec";
  const commonVals = "0, true, 1000, 1, now(), 1, 1, 1000, 1, 1, 1, '1'";
  const outputNum = transitionConfigs["stop-output"].expectedFinishNum;
  return `
INSERT INTO public.wom_output_details (
  id, ${commonCols}, status, table_no, table_info_id, head_id,
  material_batch_num, product, output_num, report_num, putin_time, putin_end_time,
  remain_operate, task_type, remark
) VALUES (
  ${ids.outputDetail}, ${commonVals}, 99, ${sqlLiteral(`${marker}_OUTPUT_DETAIL_TN`)}, ${ids.outputDetail}, ${procReportId},
  ${sqlLiteral(batchNo)}, ${ids.material}, ${outputNum}, ${outputNum}, now() - interval '5 minutes', now(),
  'WOM_remainOperate/noOperate', 'WOM_taskType/manufacture', NULL
) ON CONFLICT (id) DO UPDATE SET
  valid = EXCLUDED.valid,
  head_id = EXCLUDED.head_id,
  material_batch_num = EXCLUDED.material_batch_num,
  product = EXCLUDED.product,
  output_num = EXCLUDED.output_num,
  report_num = EXCLUDED.report_num,
  putin_time = EXCLUDED.putin_time,
  putin_end_time = EXCLUDED.putin_end_time,
  remain_operate = EXCLUDED.remain_operate,
  modify_time = now();

SELECT 'outputDetailSeed', ${ids.outputDetail}, ${procReportId}, ${sqlLiteral(batchNo)}, ${outputNum};
`;
}

function assertPersistence(rawRows, expectedState, options = {}) {
  const rows = parseRows(rawRows);
  const task = rows.find((row) => row[0] === "task");
  const wait = rows.find((row) => row[0] === "wait");
  const proc = rows.find((row) => row[0] === "proc");
  const exelog = rows.find((row) => row[0] === "exelog");
  const outputDetail = rows.find((row) => row[0] === "outputDetail");
  const outputDetailCount = rows.find((row) => row[0] === "outputDetailCount");
  const taskActItemsCount = rows.find((row) => row[0] === "taskActItemsCount");
  const matOutptRecordCount = rows.find((row) => row[0] === "matOutptRecordCount");
  const failures = [];
  const isFinished = expectedState === "WOM_runState/finished";
  const expectedFinishNum = Number(options.expectedFinishNum || 0);

  if (!task || task[3] !== expectedState || !task[4] || Number(task[7]) < 1) {
    failures.push(`wom_produce_tasks not in expected state ${expectedState}: ${JSON.stringify(task)}`);
  }
  if (isFinished && (!task[5] || Number(task[6] || 0) !== expectedFinishNum)) {
    failures.push(`wom_produce_tasks finish fields are not set for stop path: ${JSON.stringify(task)}`);
  }
  if (!wait || wait[3] !== expectedState || !wait[4] || !wait[6] || wait[7] !== "WOM_BatchSyncStatus/done") {
    failures.push(`wom_wait_put_records not in expected state ${expectedState}: ${JSON.stringify(wait)}`);
  }
  if (isFinished && !wait[5]) {
    failures.push(`wom_wait_put_records actual_end_time is not set for stop path: ${JSON.stringify(wait)}`);
  }
  if (!proc || proc[3] !== "WOM_procReportType/produceTask" || proc[4] !== "RM_exeSystem/mes" || proc[6] !== "false" || proc[7] !== "true") {
    failures.push(`wom_proc_reports not inserted as expected: ${JSON.stringify(proc)}`);
  }
  if (!exelog || exelog[3] !== (isFinished ? "WOM_runState/finished" : "WOM_runState/runing") || !exelog[4] || exelog[8] !== "true") {
    failures.push(`wom_produce_task_exelog not inserted as expected: ${JSON.stringify(exelog)}`);
  }
  if (isFinished && (!exelog[5] || Number(exelog[6] || 0) !== expectedFinishNum)) {
    failures.push(`wom_produce_task_exelog finish fields are not set for stop path: ${JSON.stringify(exelog)}`);
  }
  if (options.expectOutputDetail) {
    if (
      !outputDetail ||
      outputDetail[2] !== proc[1] ||
      outputDetail[3] !== batchNo ||
      Number(outputDetail[4] || 0) !== expectedFinishNum ||
      Number(outputDetail[5] || 0) !== expectedFinishNum ||
      outputDetail[6] !== String(ids.material) ||
      outputDetail[7] !== "WOM_remainOperate/noOperate" ||
      outputDetail[8] !== "true"
    ) {
      failures.push(`wom_output_details marker row not persisted as expected: ${JSON.stringify(outputDetail)}`);
    }
    if (!outputDetailCount || Number(outputDetailCount[1] || 0) < 1) {
      failures.push(`wom_output_details count not linked to proc report: ${JSON.stringify(outputDetailCount)}`);
    }
    if (!matOutptRecordCount || Number(matOutptRecordCount[1] || 0) < 1) {
      failures.push(`wom_mat_outpt_records not generated for output detail: ${JSON.stringify(matOutptRecordCount)}`);
    }
  }

  if (failures.length) {
    throw new Error(failures.join("; "));
  }
  return { rows, task, wait, proc, exelog, outputDetail, outputDetailCount, taskActItemsCount, matOutptRecordCount };
}

function assertAdvanceReleasePersistence(rawRows) {
  const rows = parseRows(rawRows);
  const task = rows.find((row) => row[0] === "task");
  const wait = rows.find((row) => row[0] === "wait");
  const failures = [];

  if (!task || task[3] !== "WOM_runState/runing" || !task[4]) {
    failures.push(`wom_produce_tasks is not running for advance release: ${JSON.stringify(task)}`);
  }
  if (!task || task[8] !== "true" || task[9] !== "true" || !String(task[10] || "").includes(marker)) {
    failures.push(`wom_produce_tasks advance fields not persisted: ${JSON.stringify(task)}`);
  }
  if (!wait || wait[2] !== String(ids.task)) {
    failures.push(`wom_wait_put_records seed row missing for advance release task: ${JSON.stringify(wait)}`);
  }

  if (failures.length) {
    throw new Error(failures.join("; "));
  }
  return { rows, task, wait };
}

function assertTransitionPersistence(rawRows, config) {
  if (config.special === "advanceRelease") {
    return assertAdvanceReleasePersistence(rawRows);
  }
  return assertPersistence(rawRows, config.expectedState, {
    expectOutputDetail: config.special === "finishWithOutputDetail",
    expectedFinishNum: config.expectedFinishNum,
  });
}

async function waitForResponseSafe(page, predicate, timeout = 45000) {
  return page
    .waitForResponse(predicate, { timeout })
    .then((response) => ({ response }))
    .catch((error) => ({ error }));
}

async function refreshMarkerGrid(page) {
  await page.waitForFunction(
    (gridCode) => {
      const gridFactory = window.ReactAPI && window.ReactAPI.getComponentAPI("SupDataGrid");
      const grid = gridFactory && typeof gridFactory.APIs === "function" && gridFactory.APIs(gridCode);
      return Boolean(grid && typeof grid.refreshDataByRequst === "function");
    },
    gridId,
    { timeout: gridTimeoutMs }
  );
  const responsePromise = waitForResponseSafe(
    page,
    (response) =>
      /\/WOM\/produceTask\/produceTask\/makeTaskList-(pending|query)/.test(response.url()) &&
      response.request().method() === "POST",
    gridTimeoutMs
  );
  const refreshResult = await page.evaluate(
    ({ gridCode }) => {
      const gridFactory = window.ReactAPI && window.ReactAPI.getComponentAPI("SupDataGrid");
      const grid = gridFactory && typeof gridFactory.APIs === "function" && gridFactory.APIs(gridCode);
      if (!grid || typeof grid.refreshDataByRequst !== "function") {
        return { ok: false, error: "grid refreshDataByRequst missing" };
      }
      grid.refreshDataByRequst({
        type: "POST",
        url: "/msService/WOM/produceTask/produceTask/makeTaskList-pending",
        param: {
          classifyCodes: "",
          customCondition: {},
          permissionCode: "WOM_1.0.0_produceTask_makeTaskList",
          pageNo: 1,
          paging: true,
          pageSize: 65535,
          crossCompanyFlag: "true",
        },
      });
      return { ok: true };
    },
    { gridCode: gridId }
  );
  if (!refreshResult.ok) {
    throw new Error(`Failed to refresh WOM marker grid: ${JSON.stringify(refreshResult)}`);
  }
  const { response, error } = await responsePromise;
  if (error) {
    throw new Error(`WOM marker grid refresh response was not observed: ${error.message}`);
  }
  return { status: response.status(), url: response.url() };
}

async function selectMarkerRow(page) {
  await refreshMarkerGrid(page);
  await page.waitForFunction(
    ({ gridCode, expectedMarker }) => {
      const gridFactory = window.ReactAPI && window.ReactAPI.getComponentAPI("SupDataGrid");
      const grid = gridFactory && typeof gridFactory.APIs === "function" && gridFactory.APIs(gridCode);
      const rows =
        (grid && typeof grid.getRows === "function" && grid.getRows()) ||
        (grid && typeof grid.getDatagridData === "function" && grid.getDatagridData()) ||
        [];
      return rows.some((row) => String(row.tableNo || "").includes(expectedMarker));
    },
    { gridCode: gridId, expectedMarker: marker },
    { timeout: gridTimeoutMs }
  );
  const apiSelection = await page.evaluate(
    ({ gridCode, expectedMarker }) => {
      const gridFactory = window.ReactAPI && window.ReactAPI.getComponentAPI("SupDataGrid");
      const grid = gridFactory && typeof gridFactory.APIs === "function" && gridFactory.APIs(gridCode);
      if (!grid || typeof grid.getRows !== "function") {
        return { ok: false, error: "grid api missing" };
      }
      const rows = grid.getRows ? grid.getRows() : [];
      const rowIndex = rows.findIndex((row) => String(row.tableNo || "").includes(expectedMarker));
      if (rowIndex < 0) {
        return { ok: false, rowCount: rows.length, tableNos: rows.map((row) => row.tableNo).slice(0, 10) };
      }
      grid.setSelecteds(String(rowIndex));
      const selecteds = grid.getSelecteds ? grid.getSelecteds() : [];
      const selected = selecteds[0] || {};
      return {
        ok: selecteds.length === 1,
        rowIndex,
        selectedCount: selecteds.length,
        selectedId: selected.id,
        tableNo: selected.tableNo,
        taskRunState: selected.taskRunState && selected.taskRunState.id,
        status: selected.status,
        batchContral: selected.batchContral,
        advanceCharge: selected.advanceCharge,
        isAdvanced: selected.isAdvanced,
        feedCondition: selected.feedCondition,
      };
    },
    { gridCode: gridId, expectedMarker: marker }
  );

  const domSelection = {
    attempted: false,
    ok: false,
    error: "",
  };
  let rowLocator = page
    .locator(".sup-datagrid-body-wrap .sup-datagrid-row, .ant-table-row, .sup-table-row, .datagrid-row, .cui-grid-row, tr")
    .filter({ hasText: marker })
    .first();
  if ((await rowLocator.count()) < 1 && typeof apiSelection.rowIndex === "number" && apiSelection.rowIndex >= 0) {
    await page
      .waitForFunction(
        ({ rowIndex }) =>
          document.querySelectorAll(
            ".sup-datagrid-body-wrap .sup-datagrid-row, .ant-table-tbody tr, tbody tr, .ant-table-row, .sup-table-row, .datagrid-row, .cui-grid-row"
          ).length > rowIndex,
        { rowIndex: apiSelection.rowIndex },
        { timeout: 5000 }
      )
      .catch(() => null);
    rowLocator = page
      .locator(".sup-datagrid-body-wrap .sup-datagrid-row, .ant-table-tbody tr, tbody tr, .ant-table-row, .sup-table-row, .datagrid-row, .cui-grid-row")
      .nth(apiSelection.rowIndex);
  }
  if ((await rowLocator.count()) > 0) {
    domSelection.attempted = true;
    try {
      await rowLocator.click({ timeout: 5000 });
      await page.waitForTimeout(300);
      domSelection.ok = true;
    } catch (error) {
      domSelection.error = error.message;
    }
  }

  let finalSelection = await page.evaluate(
    ({ gridCode }) => {
      const gridFactory = window.ReactAPI && window.ReactAPI.getComponentAPI("SupDataGrid");
      const grid = gridFactory && typeof gridFactory.APIs === "function" && gridFactory.APIs(gridCode);
      const selecteds = grid && typeof grid.getSelecteds === "function" ? grid.getSelecteds() : [];
      const selected = selecteds[0] || {};
      return {
        selectedCount: selecteds.length,
        selectedId: selected.id,
        tableNo: selected.tableNo,
        taskRunState: selected.taskRunState && selected.taskRunState.id,
        status: selected.status,
        batchContral: selected.batchContral,
        advanceCharge: selected.advanceCharge,
        isAdvanced: selected.isAdvanced,
        feedCondition: selected.feedCondition,
      };
    },
    { gridCode: gridId }
  );
  let restoredSelection = null;
  if (
    String(finalSelection.selectedId || "") !== String(ids.task) &&
    typeof apiSelection.rowIndex === "number" &&
    apiSelection.rowIndex >= 0
  ) {
    restoredSelection = await page.evaluate(
      ({ gridCode, rowIndex }) => {
        const gridFactory = window.ReactAPI && window.ReactAPI.getComponentAPI("SupDataGrid");
        const grid = gridFactory && typeof gridFactory.APIs === "function" && gridFactory.APIs(gridCode);
        if (!grid || typeof grid.setSelecteds !== "function") {
          return { ok: false, error: "grid setSelecteds missing" };
        }
        grid.setSelecteds(String(rowIndex));
        const selecteds = typeof grid.getSelecteds === "function" ? grid.getSelecteds() : [];
        const selected = selecteds[0] || {};
        return {
          ok: selecteds.length === 1,
          selectedCount: selecteds.length,
          selectedId: selected.id,
          tableNo: selected.tableNo,
          taskRunState: selected.taskRunState && selected.taskRunState.id,
          status: selected.status,
        };
      },
      { gridCode: gridId, rowIndex: apiSelection.rowIndex }
    );
    if (restoredSelection && restoredSelection.ok) {
      finalSelection = restoredSelection;
    }
  }

  return {
    ...apiSelection,
    ...finalSelection,
    ok: finalSelection.selectedCount === 1 && String(finalSelection.selectedId || "") === String(ids.task),
    apiSelection,
    domSelection,
    restoredSelection,
  };
}

async function parseUpdateResponse(response) {
  const body = await response.text();
  let json = null;
  try {
    json = JSON.parse(body);
  } catch (_error) {
    json = null;
  }
  return {
    status: response.status(),
    body: body.slice(0, 8000),
    json,
  };
}

async function invokeRuntimeButton(page, buttonId) {
  return page.evaluate((runtimeButtonId) => {
    const buttonCell = document.getElementById(runtimeButtonId);
    const buttonItem = buttonCell && buttonCell.querySelector(".sup-datagrid-button-item");
    if (!buttonItem) {
      return { ok: false, error: `button item not found: ${runtimeButtonId}` };
    }

    const fiberKey = Object.keys(buttonItem).find((key) => key.startsWith("__reactInternalInstance$"));
    let fiber = fiberKey && buttonItem[fiberKey];
    while (fiber) {
      if (fiber.type && fiber.type.name === "HeaderButtonCell") {
        const instance = fiber.stateNode;
        const button = instance && instance.props && instance.props.button;
        if (!instance || !button || typeof instance.setWindowFunc !== "function") {
          return { ok: false, error: `runtime button instance is incomplete: ${runtimeButtonId}` };
        }

        let showTipResult = true;
        if (typeof instance.showTipFunc === "function") {
          showTipResult = instance.showTipFunc();
        }
        if (!showTipResult) {
          return { ok: false, error: `runtime button showTipFunc returned false: ${runtimeButtonId}` };
        }

        const operateEvent = instance.setWindowFunc(button.funcname, button.funcbody);
        if (typeof operateEvent !== "function") {
          return { ok: false, error: `runtime button event is not callable: ${runtimeButtonId}` };
        }
        operateEvent({ type: "adp-e2e-runtime-button", target: buttonItem, currentTarget: buttonItem });
        return {
          ok: true,
          triggerMode: "runtime-button-event",
          buttonId: runtimeButtonId,
          buttonConfigId: button.id,
          buttonName: button.showname || button.name || button.NAME,
          operationCode: button.buttonoperationcode || button.CODE,
        };
      }
      fiber = fiber.return;
    }

    return { ok: false, error: `HeaderButtonCell not found: ${runtimeButtonId}` };
  }, buttonId);
}

async function runAdvanceReleaseTransition(page, config, selection, evidence) {
  const invocation = await invokeRuntimeButton(page, config.buttonId);
  evidence.buttonInvocations.push({ transition: "advance-release", ...invocation });
  if (!invocation.ok) {
    throw new Error(`advance-release runtime button invocation failed: ${JSON.stringify(invocation)}`);
  }

  await page.waitForTimeout(800);
  evidence.screenshots.advanceReleaseConfirm = path.join(outputDir, `wom-${transitionName}-advance-release-confirm.png`);
  await captureScreenshot(page, evidence.screenshots.advanceReleaseConfirm, evidence, "advanceReleaseConfirm");

  const confirmButton = page.locator(
    ".ant-modal:visible .ant-btn-primary, .ant-modal:visible button, .ant-modal-confirm:visible button, .sup-modal:visible button"
  );
  const confirmButtonCount = await confirmButton.count();
  evidence.confirmButtonCounts["advance-release"] = confirmButtonCount;
  if (confirmButtonCount < 1) {
    throw new Error("advance-release confirmation button was not found");
  }
  const confirmButtonMetas = await confirmButton.evaluateAll((buttons) =>
    buttons.map((button, index) => ({
      index,
      text: (button.innerText || button.textContent || "").trim(),
      className: String(button.className || ""),
      ariaLabel: button.getAttribute("aria-label") || "",
      title: button.getAttribute("title") || "",
    }))
  );
  const confirmButtonMeta =
    confirmButtonMetas.find(
      (button) =>
        button.text &&
        !/取消|关闭|Cancel|Close|WOM\.custom\.randon1596434330969/i.test(button.text) &&
        !/close/i.test(button.className) &&
        !/close/i.test(button.ariaLabel)
    ) || confirmButtonMetas.find((button) => button.text);
  if (!confirmButtonMeta) {
    throw new Error(`advance-release confirmation business button was not found: ${JSON.stringify(confirmButtonMetas)}`);
  }
  const confirmButtonText = confirmButtonMeta.text;
  evidence.confirmButtonTexts = evidence.confirmButtonTexts || {};
  evidence.confirmButtonTexts["advance-release"] = confirmButtonText;
  evidence.confirmButtonMetas = evidence.confirmButtonMetas || {};
  evidence.confirmButtonMetas["advance-release"] = confirmButtonMetas;

  const advanceResponsePromise = page
    .waitForResponse(
      (response) => response.url().includes(`/WOM/produceTask/produceTask/setAdvanceTrue/${selection.selectedId}`),
      { timeout: 30000 }
    )
    .catch((error) => ({ error: error.message }));
  await confirmButton.nth(confirmButtonMeta.index).click({ timeout: 10000 });

  const advanceResponse = await advanceResponsePromise;
  if (advanceResponse && advanceResponse.error) {
    throw new Error(`advance-release setAdvanceTrue response was not captured: ${advanceResponse.error}`);
  }
  const setAdvanceTrue = await parseUpdateResponse(advanceResponse);
  evidence.advanceReleaseByTransition.push({
    transition: "advance-release",
    expectedState: config.expectedState,
    response: setAdvanceTrue,
  });

  const payload = setAdvanceTrue.json && (setAdvanceTrue.json.data || setAdvanceTrue.json);
  if (setAdvanceTrue.status !== 200 || !payload || payload.success !== true) {
    throw new Error(`advance-release setAdvanceTrue did not pass: ${JSON.stringify(setAdvanceTrue)}`);
  }

  return {
    status: setAdvanceTrue.status,
    body: setAdvanceTrue.body,
    json: setAdvanceTrue.json,
    submitMode: "runtime-button-event-confirm",
  };
}

async function runStopTransition(page, config, selection, evidence, transition = "stop") {
  const findProcResponsePromise = page.waitForResponse(
    (response) => response.url().includes("/WOM/produceTask/produceTask/findProcReportIdByTaskId"),
    { timeout: 30000 }
  );
  const dialogRequestPromise = page
    .waitForRequest(
      (requestItem) => requestItem.url().includes("/WOM/procReport/procReport/outPutCommonTaskEdit"),
      { timeout: 15000 }
    )
    .catch((error) => ({ error: error.message }));

  const invocation = await invokeRuntimeButton(page, config.buttonId);
  evidence.buttonInvocations.push({ transition, ...invocation });
  if (!invocation.ok) {
    throw new Error(`stop runtime button invocation failed: ${JSON.stringify(invocation)}`);
  }

  const findProcResponse = await findProcResponsePromise;
  const findProc = await parseUpdateResponse(findProcResponse);
  const procReportId =
    findProc.json &&
    (findProc.json.result ||
      (findProc.json.data && findProc.json.data.result) ||
      (findProc.json.data && findProc.json.data.id));
  evidence.findProcReportByTransition.push({
    transition,
    status: findProc.status,
    response: findProc,
    procReportId,
  });
  if (findProc.status !== 200 || !procReportId) {
    throw new Error(`stop findProcReportIdByTaskId did not return a procReportId: ${JSON.stringify(findProc)}`);
  }
  if (config.special === "finishWithOutputDetail") {
    evidence.outputDetailSeedByTransition.push({
      transition,
      procReportId,
      sqlResult: runSql(seedOutputDetailSql(procReportId)),
    });
  }

  const dialogRequest = await dialogRequestPromise;
  evidence.stopDialogRequests.push(
    dialogRequest && dialogRequest.url
      ? { transition, method: dialogRequest.method(), url: dialogRequest.url() }
      : { transition, ...dialogRequest }
  );
  await page.waitForTimeout(800);
  evidence.screenshots.stopDialog = path.join(outputDir, `wom-${transitionName}-stop-dialog.png`);
  await captureScreenshot(page, evidence.screenshots.stopDialog, evidence, "stopDialog");

  const addOutput = await page.evaluate(
    async ({ procReportId: reportId, taskId }) => {
      const response = await fetch("/msService/WOM/produceTask/produceTask/addOutputByOutPutDetails", {
        method: "POST",
        credentials: "include",
        headers: {
          Accept: "application/json, text/plain, */*",
          "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
          "X-Requested-With": "XMLHttpRequest",
        },
        body: new URLSearchParams({ procReportId: reportId, taskId }).toString(),
      });
      const body = await response.text();
      let json = null;
      try {
        json = JSON.parse(body);
      } catch (_error) {
        json = null;
      }
      return {
        status: response.status,
        body: body.slice(0, 8000),
        json,
        request: {
          method: "POST",
          url: "/msService/WOM/produceTask/produceTask/addOutputByOutPutDetails",
          payload: { procReportId: reportId, taskId },
        },
      };
    },
    { procReportId, taskId: String(selection.selectedId) }
  );
  evidence.addOutputByTransition.push({ transition, response: addOutput });

  const payload = addOutput.json && (addOutput.json.data || addOutput.json);
  if (
    addOutput.status !== 200 ||
    !payload ||
    payload.dealSuccessFlag !== true ||
    !payload.exeState ||
    payload.exeState.id !== config.expectedState
  ) {
    throw new Error(`stop addOutputByOutPutDetails did not pass: ${JSON.stringify(addOutput)}`);
  }

  return {
    status: addOutput.status,
    body: addOutput.body,
    json: addOutput.json,
    procReportId,
    submitMode: "browser-page-fetch-after-stop-dialog",
  };
}

async function runBrowser(ticket, evidence) {
  const browser = await chromium.launch({ headless });
  try {
    const context = await browser.newContext({
      baseURL: browserBaseUrl,
      ignoreHTTPSErrors: true,
      viewport: { width: 1600, height: 1000 },
      extraHTTPHeaders: { Authorization: `Bearer ${ticket}` },
    });
    await context.addCookies([
      { name: "suposTicket", value: ticket, url: browserBaseUrl },
      { name: "SUPOS_TICKET", value: ticket, url: browserBaseUrl },
    ]);
    await context.addInitScript((token) => {
      window.localStorage.clear();
      window.sessionStorage.clear();
      ["suposTicket", "SUPOS_TICKET", "token"].forEach((key) => {
        window.localStorage.setItem(key, token);
        window.sessionStorage.setItem(key, token);
      });
    }, ticket);

    const page = await context.newPage();
    page.setDefaultTimeout(pageTimeoutMs);
    page.setDefaultNavigationTimeout(pageTimeoutMs);
    page.on("console", (message) => {
      if (["error", "warning"].includes(message.type())) {
        evidence.console.push({ type: message.type(), text: message.text() });
      }
    });
    page.on("pageerror", (error) => evidence.pageErrors.push(error.message));
    page.on("requestfailed", (requestItem) => {
      evidence.requestFailures.push({
        method: requestItem.method(),
        url: requestItem.url(),
        failure: requestItem.failure() && requestItem.failure().errorText,
      });
    });
    page.on("request", (requestItem) => {
      const url = requestItem.url();
      if (/makeTaskList|updateTaskState|setAdvanceTrue|findProcReportIdByTaskId|addOutputByOutPutDetails|outPutCommonTaskEdit|layoutJson|systemCodeJson/.test(url)) {
        evidence.requests.push({
          method: requestItem.method(),
          url,
          postData: requestItem.postData(),
        });
      }
    });
    page.on("response", async (response) => {
      const url = response.url();
      if (!/makeTaskList|updateTaskState|setAdvanceTrue|findProcReportIdByTaskId|addOutputByOutPutDetails|outPutCommonTaskEdit|layoutJson|systemCodeJson/.test(url)) {
        return;
      }
      let body = "";
      try {
        body = (await response.text()).slice(0, 8000);
      } catch (_error) {
        body = "";
      }
      evidence.responses.push({
        method: response.request().method(),
        url,
        status: response.status(),
        body,
      });
    });

	    const nav = await page.goto(route, { waitUntil: "commit", timeout: pageTimeoutMs });
	    const domContentLoaded = await page
	      .waitForLoadState("domcontentloaded", { timeout: Math.min(pageTimeoutMs, 60000) })
	      .then(() => ({ ok: true }))
	      .catch((error) => ({ ok: false, error: error.message }));
	    evidence.navigation = { status: nav && nav.status(), domContentLoaded };
	    evidence.beforeBodyHasMarker = (await page.locator("body").innerText()).includes(marker);
	    evidence.screenshots.before = path.join(outputDir, `wom-${transitionName}-before.png`);
	    await captureScreenshot(page, evidence.screenshots.before, evidence, "before");

	    for (const transition of transitions) {
	      const config = transitionConfigs[transition];
	      const selection = await selectMarkerRow(page);
	      evidence.selections.push({ transition, selection });
	      if (!selection.ok) {
	        throw new Error(`Failed to select marker row for ${transition}: ${JSON.stringify(selection)}`);
	      }

      if (config.special === "finishWithoutOutputDetails" || config.special === "finishWithOutputDetail") {
        const stopResponse = await runStopTransition(page, config, selection, evidence, transition);
        evidence.updateTaskStateByTransition.push({
          transition,
          expectedState: config.expectedState,
          response: stopResponse,
        });
	        await page.waitForTimeout(1500);
	        const snapshotRaw = runSql(verificationSql());
        evidence.persistenceByTransition.push({
          transition,
          expectedState: config.expectedState,
          persistence: assertTransitionPersistence(snapshotRaw, config),
        });
        continue;
      }

	      if (config.special === "advanceRelease") {
	        const advanceResponse = await runAdvanceReleaseTransition(page, config, selection, evidence);
	        evidence.updateTaskStateByTransition.push({
	          transition,
	          expectedState: config.expectedState,
	          response: advanceResponse,
	        });
	        await page.waitForTimeout(1500);
	        const snapshotRaw = runSql(verificationSql());
	        evidence.persistenceByTransition.push({
	          transition,
	          expectedState: config.expectedState,
	          persistence: assertAdvanceReleasePersistence(snapshotRaw),
	        });
	        continue;
	      }

	      const updateResponsePromise = page.waitForResponse(
	        (response) => {
	          const postData = response.request().postData() || "";
	          return (
	            response.url().includes("/WOM/produceTask/produceTask/updateTaskState") &&
	            postData.includes(`state=${config.requestState}`)
	          );
	        },
	        { timeout: 30000 }
	      ).catch((error) => ({ error: error.message }));
	      if (config.confirm) {
	        await page.locator(`#${config.buttonId}`).click({ timeout: 10000 });
	        evidence.buttonInvocations.push({
	          transition,
	          triggerMode: "browser-click",
	          buttonId: config.buttonId,
	        });
	        await page.waitForTimeout(800);
	        const screenshotKey = `${transition}Confirm`;
	        evidence.screenshots[screenshotKey] = path.join(outputDir, `wom-${transitionName}-${transition}-confirm.png`);
	        await captureScreenshot(page, evidence.screenshots[screenshotKey], evidence, screenshotKey);

	        const confirmButton = page
	          .locator(".ant-modal-confirm-btns button, .ant-modal button, button")
	          .filter({ hasText: /确\s*定|确认/ });
        const confirmButtonCount = await confirmButton.count();
        evidence.confirmButtonCounts[transition] = confirmButtonCount;
        if (confirmButtonCount >= 1) {
          await confirmButton.last().click({ timeout: 10000 });
        } else {
          evidence.buttonInvocations.push({
            transition,
            triggerMode: "browser-click-no-confirm-dialog",
            buttonId: config.buttonId,
            note: "No confirmation dialog was shown; waiting for the direct updateTaskState request.",
          });
        }
      } else {
        try {
          await page.locator(`#${config.buttonId}`).click({ timeout: 10000 });
          evidence.buttonInvocations.push({
            transition,
            triggerMode: "browser-click",
            buttonId: config.buttonId,
          });
        } catch (clickError) {
          const invocation = await invokeRuntimeButton(page, config.buttonId);
          evidence.buttonInvocations.push({
            transition,
            browserClickError: clickError.message,
            ...invocation,
          });
          if (!invocation.ok) {
            throw new Error(`${transition} button invocation failed: ${JSON.stringify(invocation)}`);
          }
        }
      }

	      const updateResponse = await updateResponsePromise;
	      if (updateResponse && updateResponse.error) {
	        throw new Error(`${transition} updateTaskState response was not captured: ${updateResponse.error}`);
	      }
	      const updateTaskState = await parseUpdateResponse(updateResponse);
	      evidence.updateTaskStateByTransition.push({ transition, expectedState: config.expectedState, response: updateTaskState });
	      if (
	        updateTaskState.status !== 200 ||
	        !updateTaskState.json ||
	        !updateTaskState.json.data ||
	        updateTaskState.json.data.dealSuccessFlag !== true ||
	        !updateTaskState.json.data.exeState ||
	        updateTaskState.json.data.exeState.id !== config.expectedState
	      ) {
	        throw new Error(`${transition} updateTaskState did not pass: ${JSON.stringify(updateTaskState)}`);
	      }

	      await page.waitForTimeout(1500);
	      const snapshotRaw = runSql(verificationSql());
	      evidence.persistenceByTransition.push({
	        transition,
	        expectedState: config.expectedState,
	        persistence: assertPersistence(snapshotRaw, config.expectedState),
	      });
	    }

	    evidence.updateTaskState = evidence.updateTaskStateByTransition[0] && evidence.updateTaskStateByTransition[0].response;
	    evidence.afterBody = (await page.locator("body").innerText()).slice(0, 5000);
	    evidence.screenshots.after = path.join(outputDir, `wom-${transitionName}-after.png`);
	    await captureScreenshot(page, evidence.screenshots.after, evidence, "after");
    await context.close();
  } finally {
    await browser.close();
  }
}

function assertFrontendClean(evidence) {
  const failedResponses = evidence.responses.filter((response) => response.status >= 400);
  const consoleErrors = evidence.console.filter((message) => message.type === "error");
  const failures = [];

  if (failedResponses.length) {
    failures.push(`HTTP ${failedResponses.map((response) => `${response.status} ${response.url}`).join("; ")}`);
  }
  if (evidence.requestFailures.length) {
    failures.push(`request failures ${JSON.stringify(evidence.requestFailures)}`);
  }
  if (consoleErrors.length) {
    failures.push(`console errors ${JSON.stringify(consoleErrors)}`);
  }
  if (evidence.pageErrors.length) {
    failures.push(`page errors ${JSON.stringify(evidence.pageErrors)}`);
  }

  evidence.frontendClean = {
    failedResponses,
    requestFailures: evidence.requestFailures,
    consoleErrors,
    pageErrors: evidence.pageErrors,
    status: failures.length ? "FAIL" : "PASS",
  };

  if (failures.length) {
    throw new Error(`Frontend acceptance had blocking errors: ${failures.join(" | ")}`);
  }
}

async function main() {
  ensureDir(outputDir);
  const evidence = {
    generatedAt: new Date().toISOString(),
    baseUrl,
    browserBaseUrl,
	    marker,
	    transitions,
	    ids: Object.fromEntries(Object.entries(ids).map(([key, value]) => [key, value.toString()])),
    route,
    tableNo,
    materialCode,
    formulaCode,
    console: [],
    pageErrors: [],
	    requestFailures: [],
	    requests: [],
	    responses: [],
	    screenshots: {},
	    selections: [],
	    buttonInvocations: [],
	    confirmButtonCounts: {},
	    updateTaskStateByTransition: [],
	    advanceReleaseByTransition: [],
	    findProcReportByTransition: [],
	    outputDetailSeedByTransition: [],
	    addOutputByTransition: [],
	    stopDialogRequests: [],
	    persistenceByTransition: [],
	  };

  try {
    evidence.seed = { sqlResult: runSql(seedSql()) };
    const api = await request.newContext({ ignoreHTTPSErrors: true });
    const loginResult = await login(api);
    await api.dispose();
    evidence.login = { status: loginResult.status, ticket: Boolean(loginResult.ticket) };

    await runBrowser(loginResult.ticket, evidence);
	    assertFrontendClean(evidence);
	    const verifyRaw = runSql(verificationSql());
	    const finalTransition = transitions[transitions.length - 1];
	    const finalConfig = transitionConfigs[finalTransition];
	    const expectedFinalState = finalConfig.expectedState;
	    evidence.persistence = assertTransitionPersistence(verifyRaw, finalConfig);
	    evidence.expectedFinalState = expectedFinalState;
	    evidence.verificationSql = verificationSql().trim();
    evidence.status = "PASS";
  } catch (error) {
    evidence.status = "FAIL";
    evidence.error = error.stack || error.message;
    fs.writeFileSync(outputPath, JSON.stringify(evidence, null, 2));
    throw error;
  }

  fs.writeFileSync(outputPath, JSON.stringify(evidence, null, 2));
  console.log(
    JSON.stringify(
      {
	        status: evidence.status,
	        marker,
	        transitions,
	        outputPath,
	        updateTaskStateByTransition: evidence.updateTaskStateByTransition,
	        advanceReleaseByTransition: evidence.advanceReleaseByTransition,
	        outputDetailSeedByTransition: evidence.outputDetailSeedByTransition,
	        persistence: {
	          task: evidence.persistence.task,
          wait: evidence.persistence.wait,
          proc: evidence.persistence.proc,
          exelog: evidence.persistence.exelog,
          outputDetail: evidence.persistence.outputDetail,
          outputDetailCount: evidence.persistence.outputDetailCount,
          taskActItemsCount: evidence.persistence.taskActItemsCount,
          matOutptRecordCount: evidence.persistence.matOutptRecordCount,
        },
      },
      null,
      2
    )
  );
}

main().catch((error) => {
  console.error(error.stack || error.message);
  process.exit(1);
});
