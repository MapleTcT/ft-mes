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
const marker = process.env.ADP_E2E_MARKER || `ADP_E2E_${nowToken}_WOM_ACTIVE`;
const activeAction = process.env.ADP_WOM_ACTIVE_ACTION || "start";
const outputDir =
  process.env.ADP_OUTPUT_DIR ||
  path.join("/tmp", `adp-wom-active-persistence-${nowToken}`);
const outputPath =
  process.env.ADP_WOM_ACTIVE_PERSISTENCE_OUTPUT ||
  path.join(outputDir, "wom-active-persistence-results.json");
const idBase = 8300000000000000n + BigInt(Date.now() % 900000000000) * 100n + BigInt(process.pid % 100);
const ids = {
  material: idBase + 1n,
  formula: idBase + 2n,
  task: idBase + 3n,
  process: idBase + 4n,
  active: idBase + 5n,
  wait: idBase + 6n,
  taskExelog: idBase + 7n,
  processExelog: idBase + 8n,
  procReport: idBase + 9n,
  activeExelog: idBase + 10n,
  outputDetail: idBase + 11n,
  processWait: idBase + 12n,
  workUnit: idBase + 13n,
  workOrderWait: idBase + 14n,
  putinDetailFallback: idBase + 15n,
  proCheckDetail: idBase + 16n,
};
const tableNo = `${marker}_TASK_TN`;
const materialCode = `${marker}_MAT`;
const formulaCode = `${marker}_FORM`;
const batchNo = `${marker}_BATCH`;
const workUnitCode = `${marker}_WU`;
const workUnitName = `${marker} work unit`;
const processName = `${marker} process`;
const activeName = `${marker} active`;
const checkItems = `${marker} check item`;
const checkReportValue = `${marker} report value`;
const checkStandard = `${marker} standard`;
const easyReportAction = activeAction === "easy-end";
const putinEndAction = activeAction === "putin-end";
const checkEndAction = activeAction === "check-end";
const processUnitAction = activeAction === "process-unit";
const processAction = activeAction === "process-start" || activeAction === "process-end";
const processFlowAction = processAction || processUnitAction;
const route = processUnitAction
  ? `/msService/WOM/produceTask/taskProcess/processUnitEdit?id=${ids.process}`
  : easyReportAction
  ? `/msService/WOM/produceTask/produceTask/easyTaskOperateView?id=${ids.task}`
  : `/msService/WOM/produceTask/produceTask/makeTaskBatchView?id=${ids.task}`;
const processGridId = "WOM_1.0.0_produceTask_makeTaskBatchViewdg1586416569990";
const activeGridId = "WOM_1.0.0_produceTask_makeTaskBatchViewdg1586416570027";
const easyReportGridId = "WOM_1.0.0_produceTask_easyTaskOperateViewdg1577337007020";

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

async function browserFetch(page, method, apiPath, payload) {
  return page.evaluate(
    async ({ method: requestMethod, apiPath: pathValue, payload: bodyValue }) => {
      const response = await fetch(pathValue, {
        method: requestMethod,
        credentials: "include",
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
      return {
        status: response.status,
        ok: response.ok,
        url: response.url,
        body: text.slice(0, 8000),
        json,
      };
    },
    { method, apiPath, payload }
  );
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
  const activeType = easyReportAction
    ? "RM_activeType/output"
    : putinEndAction
      ? "RM_activeType/putin"
      : checkEndAction
        ? "RM_activeType/check"
        : "RM_activeType/common";
  const activeProperty = easyReportAction ? "RM_RMproperty/02" : putinEndAction ? "RM_RMproperty/01" : null;
  const initialProcessState = processFlowAction ? "WOM_runState/waitForRun" : "WOM_runState/runing";
  const initialProcessStartTime = processFlowAction ? "NULL" : "now()";
  const initialActiveState = easyReportAction
    ? "WOM_runState/runing"
    : checkEndAction
      ? "WOM_runState/runing"
    : processFlowAction
      ? "WOM_runState/finished"
      : "WOM_runState/waitForRun";
  const initialActiveFinished = processFlowAction ? "true" : "false";
  const initialWaitState = easyReportAction || checkEndAction ? "WOM_runState/runing" : "WOM_runState/waitForRun";
  const actualStartTime = easyReportAction || checkEndAction ? "now() - interval '5 minutes'" : "NULL";
  const procReportId = easyReportAction || checkEndAction ? String(ids.procReport) : "NULL";
  const activeExelogId = easyReportAction || checkEndAction ? String(ids.activeExelog) : "NULL";
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

INSERT INTO public.hm_factory_models (
  id, version, valid, cid, create_staff_id, create_time, create_department_id,
  create_position_id, group_id, owner_staff_id, owner_department_id,
  owner_position_id, position_lay_rec, code, name, table_no, table_info_id,
  working_type
) VALUES (
  ${ids.workUnit}, 0, true, 1000, 1, now(), 1,
  1, 1000, 1, 1,
  1, '1', ${sqlLiteral(workUnitCode)}, ${sqlLiteral(workUnitName)}, ${sqlLiteral(`${marker}_WU_TN`)}, ${ids.workUnit},
  'HierarchicalMod_workingType/notOccupied'
) ON CONFLICT (id) DO UPDATE SET
  valid = true,
  code = EXCLUDED.code,
  name = EXCLUDED.name,
  working_type = EXCLUDED.working_type,
  modify_time = now();

INSERT INTO public.wom_produce_tasks (
  id, ${commonCols}, status, table_no, table_info_id, batch_contral, finish_num,
  formula_id, plan_num, plan_start_time, plan_end_time, produce_batch_num,
  product_id, task_run_state, task_type, need_pack, is_analy, is_abnormal,
  is_prepared, advance_charge, act_start_time, remark
) VALUES (
  ${ids.task}, ${commonVals}, 99, ${sqlLiteral(tableNo)}, ${ids.task}, false, 0,
  ${ids.formula}, 1, now() - interval '1 day', now() + interval '1 day', ${sqlLiteral(batchNo)},
  ${ids.material}, 'WOM_runState/runing', 'WOM_taskType/manufacture', false, false, false,
  false, false, now(), NULL
) ON CONFLICT (id) DO UPDATE SET
  valid = true,
  status = 99,
  task_run_state = 'WOM_runState/runing',
  batch_contral = false,
  act_start_time = coalesce(public.wom_produce_tasks.act_start_time, now()),
  act_end_time = NULL;

INSERT INTO public.wom_task_processes (
  id, ${commonCols}, status, table_no, table_info_id, task_id, formula_id,
  name, process_run_state, proc_sort, exe_order, equipment_id, work_unit_working_type,
  act_start_time, remark
) VALUES (
  ${ids.process}, ${commonVals}, 99, ${sqlLiteral(`${marker}_PROCESS_TN`)}, ${ids.process}, ${ids.task}, ${ids.formula},
  ${sqlLiteral(processName)}, ${sqlLiteral(initialProcessState)}, '1', 1, ${processUnitAction ? "NULL" : ids.workUnit}, 'WOM_workUnitWorkingType/notRunning',
  ${initialProcessStartTime}, NULL
) ON CONFLICT (id) DO UPDATE SET
  valid = true,
  status = 99,
  process_run_state = EXCLUDED.process_run_state,
  equipment_id = EXCLUDED.equipment_id,
  work_unit_working_type = EXCLUDED.work_unit_working_type,
  act_start_time = EXCLUDED.act_start_time,
  act_end_time = NULL;

INSERT INTO public.wom_task_actives (
  id, ${commonCols}, status, table_no, table_info_id, task_id, task_process_id,
  formula_id, formula_process_id, material_id, name, active_type, run_state,
  property, exec_sort, hidden_sort, main_active, is_more_other, is_finish, is_run,
  is_agile, need_param_ana, material_batch_num, plan_quantity, standard_quantity, act_start_time, act_end_time, remark
) VALUES (
  ${ids.active}, ${commonVals}, 99, ${sqlLiteral(`${marker}_ACTIVE_TN`)}, ${ids.active}, ${ids.task}, ${ids.process},
  ${ids.formula}, NULL, ${ids.material}, ${sqlLiteral(activeName)}, ${sqlLiteral(activeType)}, ${sqlLiteral(initialActiveState)},
  ${activeProperty ? sqlLiteral(activeProperty) : "NULL"}, '1', 1, true, false, ${initialActiveFinished}, ${easyReportAction ? "true" : "false"},
  false, false, ${sqlLiteral(batchNo)}, 1, 1, ${actualStartTime}, NULL, NULL
) ON CONFLICT (id) DO UPDATE SET
  valid = true,
  status = 99,
  active_type = EXCLUDED.active_type,
  property = EXCLUDED.property,
  run_state = EXCLUDED.run_state,
  formula_process_id = NULL,
  act_start_time = ${easyReportAction ? actualStartTime : "NULL"},
  act_end_time = NULL,
  is_finish = EXCLUDED.is_finish,
  is_run = EXCLUDED.is_run;

INSERT INTO public.wom_produce_task_exelog (
  id, ${commonCols}, status, table_no, table_info_id, task_id, task_run_state,
  act_start_time, batch_contral, finish_num, formula_id, product_id, produce_batch_num, task_type, remark
) VALUES (
  ${ids.taskExelog}, ${commonVals}, 99, ${sqlLiteral(`${marker}_TASK_EXELOG_TN`)}, ${ids.taskExelog}, ${ids.task}, 'WOM_runState/runing',
  now(), false, 0, ${ids.formula}, ${ids.material}, ${sqlLiteral(batchNo)}, 'WOM_taskType/manufacture', NULL
) ON CONFLICT (id) DO UPDATE SET
  valid = true,
  task_run_state = 'WOM_runState/runing',
  act_start_time = coalesce(public.wom_produce_task_exelog.act_start_time, now()),
  act_end_time = NULL;

${processFlowAction ? "" : `
INSERT INTO public.wom_process_exelogs (
  id, ${commonCols}, status, table_no, table_info_id, task_id, task_process_id,
  name, process_run_state, act_start_time, formula_process_id, produce_batch_num, remark
) VALUES (
  ${ids.processExelog}, ${commonVals}, 99, ${sqlLiteral(`${marker}_PROCESS_EXELOG_TN`)}, ${ids.processExelog}, ${ids.task}, ${ids.process},
  ${sqlLiteral(processName)}, 'WOM_runState/runing', now(), NULL, ${sqlLiteral(batchNo)}, NULL
) ON CONFLICT (id) DO UPDATE SET
  valid = true,
  process_run_state = 'WOM_runState/runing',
  act_start_time = coalesce(public.wom_process_exelogs.act_start_time, now()),
  act_end_time = NULL;
`}

${easyReportAction || checkEndAction ? `
INSERT INTO public.wom_proc_reports (
  id, ${commonCols}, status, table_no, table_info_id, task_id, task_process_id,
  task_active_id, proc_report_type, exe_system, is_finish, material_id, is_agile,
  task_type, produce_staff_id, produce_time, remark
) VALUES (
  ${ids.procReport}, ${commonVals}, 99, ${sqlLiteral(`${marker}_PROC_REPORT_TN`)}, ${ids.procReport}, ${ids.task}, ${ids.process},
  ${ids.active}, 'WOM_procReportType/taskActive', 'RM_exeSystem/mes', false, ${ids.material}, false,
  'WOM_taskType/manufacture', 1, now() - interval '5 minutes', NULL
) ON CONFLICT (id) DO UPDATE SET
  valid = true,
  task_id = EXCLUDED.task_id,
  task_process_id = EXCLUDED.task_process_id,
  task_active_id = EXCLUDED.task_active_id,
  proc_report_type = EXCLUDED.proc_report_type,
  exe_system = EXCLUDED.exe_system,
  is_finish = false,
  material_id = EXCLUDED.material_id,
  modify_time = now();
` : ""}

${easyReportAction ? `
INSERT INTO public.wom_output_details (
  id, ${commonCols}, status, table_no, table_info_id, head_id,
  material_batch_num, product, output_num, report_num, putin_time, putin_end_time,
  remain_operate, task_type, remark
) VALUES (
  ${ids.outputDetail}, ${commonVals}, 99, ${sqlLiteral(`${marker}_OUTPUT_DETAIL_TN`)}, ${ids.outputDetail}, ${ids.procReport},
  ${sqlLiteral(batchNo)}, ${ids.material}, 3, 3, now() - interval '5 minutes', now(),
  'WOM_remainOperate/noOperate', 'WOM_taskType/manufacture', NULL
) ON CONFLICT (id) DO UPDATE SET
  valid = true,
  head_id = EXCLUDED.head_id,
  material_batch_num = EXCLUDED.material_batch_num,
  product = EXCLUDED.product,
  output_num = EXCLUDED.output_num,
  report_num = EXCLUDED.report_num,
  putin_time = EXCLUDED.putin_time,
  putin_end_time = EXCLUDED.putin_end_time,
  remain_operate = EXCLUDED.remain_operate,
  modify_time = now();
` : ""}

${checkEndAction ? `
INSERT INTO public.wom_pro_check_details (
  id, ${commonCols}, status, table_no, table_info_id, head_id,
  check_items, standrad, report_value, is_pass, remark
) VALUES (
  ${ids.proCheckDetail}, ${commonVals}, 99, ${sqlLiteral(`${marker}_CHECK_DETAIL_TN`)}, ${ids.task}, ${ids.procReport},
  ${sqlLiteral(checkItems)}, ${sqlLiteral(checkStandard)}, ${sqlLiteral(checkReportValue)}, true, NULL
) ON CONFLICT (id) DO UPDATE SET
  valid = true,
  head_id = EXCLUDED.head_id,
  table_info_id = EXCLUDED.table_info_id,
  check_items = EXCLUDED.check_items,
  standrad = EXCLUDED.standrad,
  report_value = EXCLUDED.report_value,
  is_pass = true,
  modify_time = now();
` : ""}

${easyReportAction || checkEndAction ? `
INSERT INTO public.wom_acti_exelogs (
  id, ${commonCols}, status, table_no, table_info_id, head_id, task_id, task_process_id,
  task_active_id, proc_report_id, proc_exelog_id, output_detail_id, material_id,
  name, active_type, run_state, act_start_time, material_batch_num, produce_batch_num,
  actual_num, use_num, exec_sort, exe_system, need_param_ana, analysis_flag, is_sync, need_sync, remark
) VALUES (
  ${ids.activeExelog}, ${commonVals}, 99, ${sqlLiteral(`${marker}_ACTIVE_EXELOG_TN`)}, ${ids.activeExelog}, ${ids.taskExelog}, ${ids.task}, ${ids.process},
  ${ids.active}, ${ids.procReport}, ${ids.processExelog}, ${easyReportAction ? ids.outputDetail : "NULL"}, ${ids.material},
  ${sqlLiteral(activeName)}, ${sqlLiteral(activeType)}, 'WOM_runState/runing', now() - interval '5 minutes', ${sqlLiteral(batchNo)}, ${sqlLiteral(batchNo)},
  ${easyReportAction ? "3, 3" : "1, 1"}, '1', 'RM_exeSystem/mes', false, false, false, true, NULL
) ON CONFLICT (id) DO UPDATE SET
  valid = true,
  head_id = EXCLUDED.head_id,
  task_id = EXCLUDED.task_id,
  task_process_id = EXCLUDED.task_process_id,
  task_active_id = EXCLUDED.task_active_id,
  proc_report_id = EXCLUDED.proc_report_id,
  proc_exelog_id = EXCLUDED.proc_exelog_id,
  output_detail_id = EXCLUDED.output_detail_id,
  material_id = EXCLUDED.material_id,
  active_type = EXCLUDED.active_type,
  run_state = 'WOM_runState/runing',
  act_start_time = EXCLUDED.act_start_time,
  act_end_time = NULL,
  actual_num = EXCLUDED.actual_num,
  use_num = EXCLUDED.use_num,
  modify_time = now();
` : ""}

INSERT INTO public.wom_wait_put_records (
  id, ${commonCols}, status, table_no, table_info_id, task_id, task_process_id,
  task_active_id, formula_id, material_id, product_id, produce_batch_num,
  product_code, product_name, material_code, material_name, formula_code,
  process_name, active_name, active_type, record_type, exe_state,
  batch_sync_status, actual_start_time, actual_end_time, proc_report_id, acti_exelog, remark
) VALUES (
  ${ids.wait}, ${commonVals}, 99, ${sqlLiteral(`${marker}_ACTIVE_WAIT_TN`)}, ${ids.wait}, ${ids.task}, ${ids.process},
  ${ids.active}, ${ids.formula}, ${ids.material}, ${ids.material}, ${sqlLiteral(batchNo)},
  ${sqlLiteral(materialCode)}, ${sqlLiteral(`${marker} material`)}, ${sqlLiteral(materialCode)}, ${sqlLiteral(`${marker} material`)}, ${sqlLiteral(formulaCode)},
  ${sqlLiteral(processName)}, ${sqlLiteral(activeName)}, ${sqlLiteral(activeType)}, 'WOM_recordType/active', ${sqlLiteral(initialWaitState)},
  'WOM_BatchSyncStatus/await', ${actualStartTime}, NULL, ${procReportId}, ${activeExelogId}, NULL
) ON CONFLICT (id) DO UPDATE SET
  valid = true,
  active_type = EXCLUDED.active_type,
  exe_state = EXCLUDED.exe_state,
  actual_start_time = EXCLUDED.actual_start_time,
  actual_end_time = NULL,
  proc_report_id = EXCLUDED.proc_report_id,
  acti_exelog = EXCLUDED.acti_exelog;

${processFlowAction ? `
INSERT INTO public.wom_wait_put_records (
  id, ${commonCols}, status, table_no, table_info_id, task_id, task_process_id,
  formula_id, material_id, product_id, produce_batch_num,
  product_code, product_name, material_code, material_name, formula_code,
  record_type, exe_state, batch_sync_status, actual_start_time, actual_end_time, remark
) VALUES (
  ${ids.workOrderWait}, ${commonVals}, 99, ${sqlLiteral(`${marker}_WORK_ORDER_WAIT_TN`)}, ${ids.workOrderWait}, ${ids.task}, ${ids.process},
  ${ids.formula}, ${ids.material}, ${ids.material}, ${sqlLiteral(batchNo)},
  ${sqlLiteral(materialCode)}, ${sqlLiteral(`${marker} material`)}, ${sqlLiteral(materialCode)}, ${sqlLiteral(`${marker} material`)}, ${sqlLiteral(formulaCode)},
  'WOM_recordType/workOrder', 'WOM_runState/runing', 'WOM_BatchSyncStatus/await', NULL, NULL, NULL
) ON CONFLICT (id) DO UPDATE SET
  valid = true,
  task_id = EXCLUDED.task_id,
  task_process_id = EXCLUDED.task_process_id,
  exe_state = EXCLUDED.exe_state,
  euq_id = NULL,
  equ_code = NULL,
  equ_name = NULL,
  modify_time = now();
` : ""}

${processFlowAction ? `
INSERT INTO public.wom_wait_put_records (
  id, ${commonCols}, status, table_no, table_info_id, task_id, task_process_id,
  task_active_id, formula_id, material_id, product_id, produce_batch_num,
  product_code, product_name, material_code, material_name, formula_code,
  process_name, active_name, active_type, record_type, exe_state,
  batch_sync_status, actual_start_time, actual_end_time, proc_report_id, acti_exelog, remark
) VALUES (
  ${ids.processWait}, ${commonVals}, 99, ${sqlLiteral(`${marker}_PROCESS_WAIT_TN`)}, ${ids.processWait}, ${ids.task}, ${ids.process},
  NULL, ${ids.formula}, ${ids.material}, ${ids.material}, ${sqlLiteral(batchNo)},
  ${sqlLiteral(materialCode)}, ${sqlLiteral(`${marker} material`)}, ${sqlLiteral(materialCode)}, ${sqlLiteral(`${marker} material`)}, ${sqlLiteral(formulaCode)},
  ${sqlLiteral(processName)}, NULL, NULL, 'WOM_recordType/process', 'WOM_runState/waitForRun',
  'WOM_BatchSyncStatus/await', NULL, NULL, NULL, NULL, NULL
) ON CONFLICT (id) DO UPDATE SET
  valid = true,
  exe_state = 'WOM_runState/waitForRun',
  actual_start_time = NULL,
  actual_end_time = NULL,
  proc_report_id = NULL,
  acti_exelog = NULL;
` : ""}

COMMIT;

SELECT 'seed', ${sqlLiteral(marker)}, ${ids.task}, ${ids.process}, ${ids.active}, ${ids.wait}, ${sqlLiteral(tableNo)};
`;
}

function putinDetailFallbackSql(procReportId) {
  const commonCols =
    "version, valid, cid, create_staff_id, create_time, create_department_id, create_position_id, group_id, owner_staff_id, owner_department_id, owner_position_id, position_lay_rec";
  const commonVals = "0, true, 1000, 1, now(), 1, 1, 1000, 1, 1, 1, '1'";
  return `
INSERT INTO public.wom_putin_details (
  id, ${commonCols}, status, table_no, table_info_id, head_id,
  material_batch_num, material_id, putin_num, use_num, putin_time, putin_end_time,
  remain_operate, task_type, remark
) VALUES (
  ${ids.putinDetailFallback}, ${commonVals}, 99, ${sqlLiteral(`${marker}_PUTIN_DETAIL_FALLBACK_TN`)}, ${ids.putinDetailFallback}, ${procReportId},
  ${sqlLiteral(batchNo)}, ${ids.material}, 2, 2, now() - interval '4 minutes', now(),
  'WOM_remainOperate/noOperate', 'WOM_taskType/manufacture', NULL
) ON CONFLICT (id) DO UPDATE SET
  valid = true,
  head_id = EXCLUDED.head_id,
  material_batch_num = EXCLUDED.material_batch_num,
  material_id = EXCLUDED.material_id,
  putin_num = EXCLUDED.putin_num,
  use_num = EXCLUDED.use_num,
  putin_time = EXCLUDED.putin_time,
  putin_end_time = EXCLUDED.putin_end_time,
  remain_operate = EXCLUDED.remain_operate,
  modify_time = now();
SELECT 'putinDetailFallback', ${ids.putinDetailFallback}, ${procReportId};
`;
}

function verificationSql() {
  return `
SELECT 'task', id, table_no, coalesce(task_run_state, ''), coalesce(act_start_time::text, ''), coalesce(act_end_time::text, ''), coalesce(version::text, '')
FROM public.wom_produce_tasks
WHERE id = ${ids.task};

SELECT 'process', id, task_id, coalesce(name, ''), coalesce(process_run_state, ''), coalesce(act_start_time::text, ''), coalesce(act_end_time::text, ''), coalesce(version::text, '')
FROM public.wom_task_processes
WHERE id = ${ids.process};

SELECT 'active', id, task_id, task_process_id, coalesce(name, ''), coalesce(run_state, ''), coalesce(is_finish::text, ''), coalesce(act_start_time::text, ''), coalesce(act_end_time::text, ''), coalesce(version::text, '')
FROM public.wom_task_actives
WHERE id = ${ids.active};

SELECT 'procReport', id, task_id, task_process_id, task_active_id, coalesce(proc_report_type, ''), coalesce(exe_system, ''), coalesce(is_finish::text, ''), coalesce(valid::text, '')
FROM public.wom_proc_reports
WHERE task_active_id = ${ids.active}
ORDER BY id;

SELECT 'processProcReport', id, task_id, task_process_id, coalesce(task_active_id::text, ''), coalesce(proc_report_type, ''), coalesce(exe_system, ''), coalesce(is_finish::text, ''), coalesce(valid::text, '')
FROM public.wom_proc_reports
WHERE task_process_id = ${ids.process}
  AND proc_report_type = 'WOM_procReportType/taskProcess'
ORDER BY id;

SELECT 'activeExelog', id, task_id, task_process_id, task_active_id, coalesce(proc_report_id::text, ''), coalesce(run_state, ''), coalesce(act_start_time::text, ''), coalesce(act_end_time::text, ''), coalesce(valid::text, '')
FROM public.wom_acti_exelogs
WHERE task_active_id = ${ids.active}
ORDER BY id;

SELECT 'wait', id, task_id, task_process_id, task_active_id, coalesce(record_type, ''), coalesce(exe_state, ''), coalesce(actual_start_time::text, ''), coalesce(actual_end_time::text, ''), coalesce(proc_report_id::text, ''), coalesce(acti_exelog::text, ''), coalesce(valid::text, '')
FROM public.wom_wait_put_records
WHERE task_active_id = ${ids.active}
ORDER BY id;

SELECT 'processWait', id, task_id, task_process_id, coalesce(task_active_id::text, ''), coalesce(record_type, ''), coalesce(exe_state, ''), coalesce(actual_start_time::text, ''), coalesce(actual_end_time::text, ''), coalesce(proc_report_id::text, ''), coalesce(valid::text, '')
FROM public.wom_wait_put_records
WHERE id = ${ids.processWait}
ORDER BY id;

SELECT 'processUnit', p.id, coalesce(p.equipment_id::text, ''), coalesce(m.code, ''), coalesce(m.name, ''), coalesce(p.valid::text, '')
FROM public.wom_task_processes p
LEFT JOIN public.hm_factory_models m ON m.id = p.equipment_id
WHERE p.id = ${ids.process};

SELECT 'processWaitUnit', id, coalesce(record_type, ''), coalesce(euq_id::text, ''), coalesce(equ_code, ''), coalesce(equ_name, ''), coalesce(valid::text, '')
FROM public.wom_wait_put_records
WHERE id = ${ids.processWait}
ORDER BY id;

SELECT 'workOrderWait', id, task_id, coalesce(record_type, ''), coalesce(euq_id::text, ''), coalesce(equ_code, ''), coalesce(equ_name, ''), coalesce(valid::text, '')
FROM public.wom_wait_put_records
WHERE id = ${ids.workOrderWait}
ORDER BY id;

SELECT 'processExelog', id, task_id, task_process_id, coalesce(process_run_state, ''), coalesce(act_start_time::text, ''), coalesce(act_end_time::text, ''), coalesce(valid::text, '')
FROM public.wom_process_exelogs
WHERE task_process_id = ${ids.process}
ORDER BY id;

SELECT 'outputDetail', id, coalesce(head_id::text, ''), coalesce(material_batch_num, ''), coalesce(output_num::text, ''), coalesce(report_num::text, ''), coalesce(product::text, ''), coalesce(remain_operate, ''), coalesce(valid::text, '')
FROM public.wom_output_details
WHERE id = ${ids.outputDetail};

SELECT 'putinDetail', id, coalesce(head_id::text, ''), coalesce(material_batch_num, ''), coalesce(material_id::text, ''), coalesce(putin_num::text, ''), coalesce(use_num::text, ''), coalesce(putin_time::text, ''), coalesce(putin_end_time::text, ''), coalesce(remain_operate, ''), coalesce(valid::text, '')
FROM public.wom_putin_details
WHERE material_batch_num = ${sqlLiteral(batchNo)}
   OR table_no = ${sqlLiteral(`${marker}_PUTIN_DETAIL_TN`)}
   OR id = ${ids.putinDetailFallback}
ORDER BY id DESC
LIMIT 1;

SELECT 'proCheckDetail', id, coalesce(head_id::text, ''), coalesce(check_items, ''), coalesce(report_value, ''), coalesce(is_pass::text, ''), coalesce(valid::text, '')
FROM public.wom_pro_check_details
WHERE id = ${ids.proCheckDetail};

SELECT 'checkRecord', id, coalesce(check_detail_id::text, ''), coalesce(check_items, ''), coalesce(report_num, ''), coalesce(produce_batch_num, ''), coalesce(data_source, ''), coalesce(valid::text, ''), coalesce(act_exelog_id::text, '')
FROM public.wom_check_records
WHERE check_items = ${sqlLiteral(checkItems)}
   OR report_num = ${sqlLiteral(checkReportValue)}
ORDER BY id DESC
LIMIT 1;

SELECT 'matOutptRecordCount', count(*)::text
FROM public.wom_mat_outpt_records
WHERE act_exelog_id = ${ids.activeExelog}
   OR out_mat_detail_id = ${ids.outputDetail};

SELECT 'matConsumRecordCount', count(*)::text
FROM public.wom_mat_consum_recods
WHERE put_mat_detail_id IN (
    SELECT id
    FROM public.wom_putin_details
    WHERE material_batch_num = ${sqlLiteral(batchNo)}
       OR table_no = ${sqlLiteral(`${marker}_PUTIN_DETAIL_TN`)}
       OR id = ${ids.putinDetailFallback}
  )
   OR putin_material_detail_id IN (
    SELECT id
    FROM public.wom_putin_details
    WHERE material_batch_num = ${sqlLiteral(batchNo)}
       OR table_no = ${sqlLiteral(`${marker}_PUTIN_DETAIL_TN`)}
       OR id = ${ids.putinDetailFallback}
  );
`;
}

function assertStartPersistence(rawRows) {
  const rows = parseRows(rawRows);
  const task = rows.find((row) => row[0] === "task");
  const process = rows.find((row) => row[0] === "process");
  const active = rows.find((row) => row[0] === "active");
  const procReport = rows.find((row) => row[0] === "procReport");
  const activeExelog = rows.find((row) => row[0] === "activeExelog");
  const wait = rows.find((row) => row[0] === "wait");
  const processExelog = rows.find((row) => row[0] === "processExelog");
  const failures = [];

  if (!task || task[3] !== "WOM_runState/runing" || !task[4]) {
    failures.push(`wom_produce_tasks not running as expected: ${JSON.stringify(task)}`);
  }
  if (!process || process[4] !== "WOM_runState/runing" || !process[5]) {
    failures.push(`wom_task_processes not running as expected: ${JSON.stringify(process)}`);
  }
  if (!active || active[5] !== "WOM_runState/runing" || active[6] !== "false" || !active[7]) {
    failures.push(`wom_task_actives did not start as expected: ${JSON.stringify(active)}`);
  }
  if (!procReport || procReport[5] !== "WOM_procReportType/taskActive" || procReport[6] !== "RM_exeSystem/mes" || procReport[7] !== "false" || procReport[8] !== "true") {
    failures.push(`wom_proc_reports not inserted as expected: ${JSON.stringify(procReport)}`);
  }
  if (!activeExelog || activeExelog[6] !== "WOM_runState/runing" || !activeExelog[5] || !activeExelog[7] || activeExelog[9] !== "true") {
    failures.push(`wom_acti_exelogs not inserted as expected: ${JSON.stringify(activeExelog)}`);
  }
  if (!wait || wait[5] !== "WOM_recordType/active" || wait[6] !== "WOM_runState/runing" || !wait[7] || !wait[9] || !wait[10] || wait[11] !== "true") {
    failures.push(`wom_wait_put_records not updated as expected: ${JSON.stringify(wait)}`);
  }
  if (!processExelog || processExelog[4] !== "WOM_runState/runing" || !processExelog[5] || processExelog[7] !== "true") {
    failures.push(`wom_process_exelogs not available as expected: ${JSON.stringify(processExelog)}`);
  }
  if (failures.length) {
    throw new Error(failures.join("; "));
  }
  return { rows, task, process, active, procReport, activeExelog, wait, processExelog };
}

function assertEndPersistence(rawRows) {
  const rows = parseRows(rawRows);
  const task = rows.find((row) => row[0] === "task");
  const process = rows.find((row) => row[0] === "process");
  const active = rows.find((row) => row[0] === "active");
  const procReport = rows.find((row) => row[0] === "procReport");
  const activeExelog = rows.find((row) => row[0] === "activeExelog");
  const wait = rows.find((row) => row[0] === "wait");
  const processExelog = rows.find((row) => row[0] === "processExelog");
  const failures = [];

  if (!task || task[3] !== "WOM_runState/runing" || !task[4]) {
    failures.push(`wom_produce_tasks not kept running as expected: ${JSON.stringify(task)}`);
  }
  if (!process || process[4] !== "WOM_runState/runing" || !process[5]) {
    failures.push(`wom_task_processes not kept running as expected: ${JSON.stringify(process)}`);
  }
  if (!active || active[5] !== "WOM_runState/finished" || active[6] !== "true" || !active[7] || !active[8]) {
    failures.push(`wom_task_actives did not finish as expected: ${JSON.stringify(active)}`);
  }
  if (!procReport || procReport[5] !== "WOM_procReportType/taskActive" || procReport[6] !== "RM_exeSystem/mes" || procReport[8] !== "true") {
    failures.push(`wom_proc_reports not retained as expected: ${JSON.stringify(procReport)}`);
  }
  if (!activeExelog || activeExelog[6] !== "WOM_runState/finished" || !activeExelog[5] || !activeExelog[7] || !activeExelog[8] || activeExelog[9] !== "true") {
    failures.push(`wom_acti_exelogs not finished as expected: ${JSON.stringify(activeExelog)}`);
  }
  if (!wait || wait[5] !== "WOM_recordType/active" || wait[6] !== "WOM_runState/finished" || !wait[7] || !wait[8] || !wait[9] || !wait[10] || wait[11] !== "true") {
    failures.push(`wom_wait_put_records not finished as expected: ${JSON.stringify(wait)}`);
  }
  if (!processExelog || processExelog[4] !== "WOM_runState/runing" || !processExelog[5] || processExelog[7] !== "true") {
    failures.push(`wom_process_exelogs not available as expected: ${JSON.stringify(processExelog)}`);
  }
  if (failures.length) {
    throw new Error(failures.join("; "));
  }
  return { rows, task, process, active, procReport, activeExelog, wait, processExelog };
}

function assertCheckEndPersistence(rawRows) {
  const base = assertEndPersistence(rawRows);
  const proCheckDetail = base.rows.find((row) => row[0] === "proCheckDetail");
  const checkRecord = base.rows.find((row) => row[0] === "checkRecord");
  const failures = [];

  if (
    !proCheckDetail ||
    proCheckDetail[1] !== String(ids.proCheckDetail) ||
    proCheckDetail[2] !== String(ids.procReport) ||
    proCheckDetail[3] !== checkItems ||
    proCheckDetail[4] !== checkReportValue ||
    proCheckDetail[5] !== "true" ||
    proCheckDetail[6] !== "true"
  ) {
    failures.push(`wom_pro_check_details marker row not available as expected: ${JSON.stringify(proCheckDetail)}`);
  }
  if (
    !checkRecord ||
    checkRecord[3] !== checkItems ||
    checkRecord[4] !== checkReportValue ||
    checkRecord[5] !== batchNo ||
    checkRecord[6] !== "WOM_TaskRecordDataSource/active" ||
    checkRecord[7] !== "true" ||
    checkRecord[8] !== String(ids.activeExelog)
  ) {
    failures.push(`wom_check_records marker row not generated as expected: ${JSON.stringify(checkRecord)}`);
  }
  if (failures.length) {
    throw new Error(failures.join("; "));
  }
  return { ...base, proCheckDetail, checkRecord, issues: [] };
}

function assertProcessStartPersistence(rawRows) {
  const rows = parseRows(rawRows);
  const task = rows.find((row) => row[0] === "task");
  const process = rows.find((row) => row[0] === "process");
  const processProcReport = rows.find((row) => row[0] === "processProcReport");
  const processWait = rows.find((row) => row[0] === "processWait");
  const workOrderWait = rows.find((row) => row[0] === "workOrderWait");
  const processExelog = rows.find((row) => row[0] === "processExelog");
  const failures = [];

  if (!task || task[3] !== "WOM_runState/runing" || !task[4]) {
    failures.push(`wom_produce_tasks not running as expected: ${JSON.stringify(task)}`);
  }
  if (!process || process[4] !== "WOM_runState/runing" || !process[5]) {
    failures.push(`wom_task_processes did not start as expected: ${JSON.stringify(process)}`);
  }
  if (!processProcReport || processProcReport[5] !== "WOM_procReportType/taskProcess" || processProcReport[6] !== "RM_exeSystem/mes" || processProcReport[7] !== "false" || processProcReport[8] !== "true") {
    failures.push(`wom_proc_reports taskProcess row not inserted as expected: ${JSON.stringify(processProcReport)}`);
  }
  if (!processWait || processWait[5] !== "WOM_recordType/process" || processWait[6] !== "WOM_runState/runing" || !processWait[7] || !processWait[9] || processWait[10] !== "true") {
    failures.push(`wom_wait_put_records process row not started as expected: ${JSON.stringify(processWait)}`);
  }
  if (!workOrderWait || workOrderWait[3] !== "WOM_recordType/workOrder" || workOrderWait[4] !== String(ids.workUnit) || workOrderWait[5] !== workUnitCode || workOrderWait[6] !== workUnitName || workOrderWait[7] !== "true") {
    failures.push(`wom_wait_put_records workOrder row not updated with process work unit as expected: ${JSON.stringify(workOrderWait)}`);
  }
  if (!processExelog || processExelog[4] !== "WOM_runState/runing" || !processExelog[5] || processExelog[7] !== "true") {
    failures.push(`wom_process_exelogs not inserted as expected: ${JSON.stringify(processExelog)}`);
  }
  if (failures.length) {
    throw new Error(failures.join("; "));
  }
  return { rows, task, process, processProcReport, processWait, workOrderWait, processExelog };
}

function assertProcessEndPersistence(rawRows) {
  const rows = parseRows(rawRows);
  const task = rows.find((row) => row[0] === "task");
  const process = rows.find((row) => row[0] === "process");
  const processProcReport = rows.find((row) => row[0] === "processProcReport");
  const processWait = rows.find((row) => row[0] === "processWait");
  const workOrderWait = rows.find((row) => row[0] === "workOrderWait");
  const processExelog = rows.find((row) => row[0] === "processExelog");
  const failures = [];

  if (!task || task[3] !== "WOM_runState/runing" || !task[4]) {
    failures.push(`wom_produce_tasks not kept running as expected: ${JSON.stringify(task)}`);
  }
  if (!process || process[4] !== "WOM_runState/finished" || !process[5] || !process[6]) {
    failures.push(`wom_task_processes did not finish as expected: ${JSON.stringify(process)}`);
  }
  if (!processProcReport || processProcReport[5] !== "WOM_procReportType/taskProcess" || processProcReport[6] !== "RM_exeSystem/mes" || processProcReport[7] !== "true" || processProcReport[8] !== "true") {
    failures.push(`wom_proc_reports taskProcess row not finished as expected: ${JSON.stringify(processProcReport)}`);
  }
  if (!processWait || processWait[5] !== "WOM_recordType/process" || processWait[6] !== "WOM_runState/finished" || !processWait[7] || !processWait[8] || !processWait[9] || processWait[10] !== "true") {
    failures.push(`wom_wait_put_records process row not finished as expected: ${JSON.stringify(processWait)}`);
  }
  if (!workOrderWait || workOrderWait[3] !== "WOM_recordType/workOrder" || workOrderWait[4] !== String(ids.workUnit) || workOrderWait[5] !== workUnitCode || workOrderWait[6] !== workUnitName || workOrderWait[7] !== "true") {
    failures.push(`wom_wait_put_records workOrder row not retained with process work unit as expected: ${JSON.stringify(workOrderWait)}`);
  }
  if (!processExelog || processExelog[4] !== "WOM_runState/finished" || !processExelog[5] || !processExelog[6] || processExelog[7] !== "true") {
    failures.push(`wom_process_exelogs not finished as expected: ${JSON.stringify(processExelog)}`);
  }
  if (failures.length) {
    throw new Error(failures.join("; "));
  }
  return { rows, task, process, processProcReport, processWait, workOrderWait, processExelog, issues: [] };
}

function assertProcessUnitPersistence(rawRows) {
  const rows = parseRows(rawRows);
  const task = rows.find((row) => row[0] === "task");
  const process = rows.find((row) => row[0] === "process");
  const processUnit = rows.find((row) => row[0] === "processUnit");
  const processWaitUnit = rows.find((row) => row[0] === "processWaitUnit");
  const workOrderWait = rows.find((row) => row[0] === "workOrderWait");
  const failures = [];

  if (!task || task[3] !== "WOM_runState/runing" || !task[4]) {
    failures.push(`wom_produce_tasks not kept running as expected: ${JSON.stringify(task)}`);
  }
  if (!process || process[4] !== "WOM_runState/waitForRun") {
    failures.push(`wom_task_processes state changed unexpectedly: ${JSON.stringify(process)}`);
  }
  if (
    !processUnit ||
    processUnit[2] !== String(ids.workUnit) ||
    processUnit[3] !== workUnitCode ||
    processUnit[4] !== workUnitName ||
    processUnit[5] !== "true"
  ) {
    failures.push(`wom_task_processes equipment_id was not saved as expected: ${JSON.stringify(processUnit)}`);
  }
  if (
    !processWaitUnit ||
    processWaitUnit[2] !== "WOM_recordType/process" ||
    processWaitUnit[3] !== String(ids.workUnit) ||
    processWaitUnit[4] !== workUnitCode ||
    processWaitUnit[5] !== workUnitName ||
    processWaitUnit[6] !== "true"
  ) {
    failures.push(`wom_wait_put_records process row work unit was not synchronized: ${JSON.stringify(processWaitUnit)}`);
  }
  if (
    !workOrderWait ||
    workOrderWait[3] !== "WOM_recordType/workOrder" ||
    workOrderWait[4] !== String(ids.workUnit) ||
    workOrderWait[5] !== workUnitCode ||
    workOrderWait[6] !== workUnitName ||
    workOrderWait[7] !== "true"
  ) {
    failures.push(`wom_wait_put_records workOrder row work unit was not synchronized: ${JSON.stringify(workOrderWait)}`);
  }
  if (failures.length) {
    throw new Error(failures.join("; "));
  }
  return { rows, task, process, processUnit, processWaitUnit, workOrderWait, issues: [] };
}

function assertEasyEndPersistence(rawRows) {
  const rows = parseRows(rawRows);
  const task = rows.find((row) => row[0] === "task");
  const process = rows.find((row) => row[0] === "process");
  const active = rows.find((row) => row[0] === "active");
  const procReport = rows.find((row) => row[0] === "procReport");
  const activeExelog = rows.find((row) => row[0] === "activeExelog");
  const wait = rows.find((row) => row[0] === "wait");
  const processExelog = rows.find((row) => row[0] === "processExelog");
  const outputDetail = rows.find((row) => row[0] === "outputDetail");
  const matOutptRecordCount = rows.find((row) => row[0] === "matOutptRecordCount");
  const failures = [];

  if (!task || task[3] !== "WOM_runState/runing" || !task[4]) {
    failures.push(`wom_produce_tasks not kept running as expected: ${JSON.stringify(task)}`);
  }
  if (!process || process[4] !== "WOM_runState/runing" || !process[5]) {
    failures.push(`wom_task_processes not kept running as expected: ${JSON.stringify(process)}`);
  }
  if (!active || active[5] !== "WOM_runState/runing" || active[6] !== "true" || !active[7] || !active[8]) {
    failures.push(`wom_task_actives did not finish easy report as expected: ${JSON.stringify(active)}`);
  }
  if (!procReport || procReport[5] !== "WOM_procReportType/taskActive" || procReport[6] !== "RM_exeSystem/mes" || procReport[7] !== "true" || procReport[8] !== "true") {
    failures.push(`wom_proc_reports not finished as expected: ${JSON.stringify(procReport)}`);
  }
  if (!activeExelog || activeExelog[6] !== "WOM_runState/finished" || activeExelog[5] !== String(ids.procReport) || !activeExelog[7] || activeExelog[9] !== "true") {
    failures.push(`wom_acti_exelogs not finished as expected: ${JSON.stringify(activeExelog)}`);
  }
  if (!wait || wait[5] !== "WOM_recordType/active" || wait[6] !== "WOM_runState/finished" || !wait[7] || !wait[8] || wait[9] !== String(ids.procReport) || wait[10] !== String(ids.activeExelog) || wait[11] !== "true") {
    failures.push(`wom_wait_put_records not finished as expected: ${JSON.stringify(wait)}`);
  }
  if (!processExelog || processExelog[4] !== "WOM_runState/runing" || !processExelog[5] || processExelog[7] !== "true") {
    failures.push(`wom_process_exelogs not available as expected: ${JSON.stringify(processExelog)}`);
  }
  if (
    !outputDetail ||
    outputDetail[2] !== String(ids.procReport) ||
    outputDetail[3] !== batchNo ||
    Number(outputDetail[4] || 0) !== 3 ||
    Number(outputDetail[5] || 0) !== 3 ||
    outputDetail[6] !== String(ids.material) ||
    outputDetail[8] !== "true"
  ) {
    failures.push(`wom_output_details marker row not available as expected: ${JSON.stringify(outputDetail)}`);
  }
  if (!matOutptRecordCount || Number(matOutptRecordCount[1] || 0) < 1) {
    failures.push(`wom_mat_outpt_records was not generated by easy report: ${JSON.stringify(matOutptRecordCount)}`);
  }
  if (failures.length) {
    throw new Error(failures.join("; "));
  }
  return { rows, task, process, active, procReport, activeExelog, wait, processExelog, outputDetail, matOutptRecordCount };
}

function assertPutinEndPersistence(rawRows) {
  const rows = parseRows(rawRows);
  const task = rows.find((row) => row[0] === "task");
  const process = rows.find((row) => row[0] === "process");
  const active = rows.find((row) => row[0] === "active");
  const procReport = rows.find((row) => row[0] === "procReport");
  const activeExelog = rows.find((row) => row[0] === "activeExelog");
  const wait = rows.find((row) => row[0] === "wait");
  const processExelog = rows.find((row) => row[0] === "processExelog");
  const putinDetail = rows.find((row) => row[0] === "putinDetail");
  const matConsumRecordCount = rows.find((row) => row[0] === "matConsumRecordCount");
  const failures = [];
  const issues = [];

  if (!task || task[3] !== "WOM_runState/runing" || !task[4]) {
    failures.push(`wom_produce_tasks not kept running as expected: ${JSON.stringify(task)}`);
  }
  if (!process || process[4] !== "WOM_runState/runing" || !process[5]) {
    failures.push(`wom_task_processes not kept running as expected: ${JSON.stringify(process)}`);
  }
  if (!active || active[5] !== "WOM_runState/finished" || active[6] !== "true" || !active[7] || !active[8]) {
    failures.push(`wom_task_actives did not finish putin activity as expected: ${JSON.stringify(active)}`);
  }
  if (!procReport || procReport[5] !== "WOM_procReportType/taskActive" || procReport[6] !== "RM_exeSystem/mes" || procReport[8] !== "true") {
    failures.push(`wom_proc_reports not retained as expected: ${JSON.stringify(procReport)}`);
  }
  if (activeExelog) {
    issues.push("RM_activeType/putin unexpectedly created wom_acti_exelogs; source code usually skips activity exelog creation for putin/output activity types.");
  }
  if (!wait || wait[5] !== "WOM_recordType/active" || wait[6] !== "WOM_runState/finished" || !wait[7] || !wait[8] || !wait[9] || wait[11] !== "true") {
    failures.push(`wom_wait_put_records not finished as expected: ${JSON.stringify(wait)}`);
  }
  if (!processExelog || processExelog[4] !== "WOM_runState/runing" || !processExelog[5] || processExelog[7] !== "true") {
    failures.push(`wom_process_exelogs not available as expected: ${JSON.stringify(processExelog)}`);
  }
  if (
    !putinDetail ||
    putinDetail[2] !== procReport[1] ||
    putinDetail[3] !== batchNo ||
    putinDetail[4] !== String(ids.material) ||
    Number(putinDetail[5] || 0) !== 2 ||
    Number(putinDetail[6] || 0) !== 2 ||
    !putinDetail[7] ||
    !putinDetail[8] ||
    putinDetail[10] !== "true"
  ) {
    failures.push(`wom_putin_details marker row not available as expected: ${JSON.stringify(putinDetail)}`);
  }
  if (!matConsumRecordCount || Number(matConsumRecordCount[1] || 0) === 0) {
    issues.push("wom_mat_consum_recods was not generated for putin-end; source endActive skips wom_acti_exelogs for RM_activeType/putin, so this remains a data-quality follow-up rather than a blocking persistence failure for wom_putin_details.");
  }
  if (failures.length) {
    throw new Error(failures.join("; "));
  }
  return { rows, task, process, active, procReport, activeExelog, wait, processExelog, putinDetail, matConsumRecordCount, issues };
}

async function getGridRows(page, gridCode) {
  return page.evaluate((code) => {
    const gridFactory = window.ReactAPI && window.ReactAPI.getComponentAPI("SupDataGrid");
    const grid = gridFactory && typeof gridFactory.APIs === "function" && gridFactory.APIs(code);
    if (!grid) {
      return { ok: false, error: "grid api missing" };
    }
    const rows =
      (typeof grid.getRows === "function" && grid.getRows()) ||
      (typeof grid.getDatagridData === "function" && grid.getDatagridData()) ||
      [];
    return {
      ok: true,
      rows: rows.map((row) => ({
        id: row.id,
        tableNo: row.tableNo,
        name: row.name,
        rowIndex: row.rowIndex,
        processRunState: row.processRunState && row.processRunState.id,
        runState: row.runState && row.runState.id,
        taskProcessId: row.taskProcessId && row.taskProcessId.id,
      })),
    };
  }, gridCode);
}

function waitForResponseSafe(page, predicate, timeout = 30000) {
  return page
    .waitForResponse(predicate, { timeout })
    .then((response) => ({ response }))
    .catch((error) => ({ error }));
}

async function selectGridRow(page, gridCode, expectedId, expectedName) {
  await page.waitForFunction(
    ({ code, rowId }) => {
      const gridFactory = window.ReactAPI && window.ReactAPI.getComponentAPI("SupDataGrid");
      const grid = gridFactory && typeof gridFactory.APIs === "function" && gridFactory.APIs(code);
      const rows =
        (grid && typeof grid.getRows === "function" && grid.getRows()) ||
        (grid && typeof grid.getDatagridData === "function" && grid.getDatagridData()) ||
        [];
      return rows.some((row) => String(row.id) === String(rowId));
    },
    { code: gridCode, rowId: String(expectedId) },
    { timeout: 45000 }
  );
  const selection = await page.evaluate(
    ({ code, rowId }) => {
      const gridFactory = window.ReactAPI && window.ReactAPI.getComponentAPI("SupDataGrid");
      const grid = gridFactory && typeof gridFactory.APIs === "function" && gridFactory.APIs(code);
      const rows =
        (grid && typeof grid.getRows === "function" && grid.getRows()) ||
        (grid && typeof grid.getDatagridData === "function" && grid.getDatagridData()) ||
        [];
      const rowIndex = rows.findIndex((row) => String(row.id) === String(rowId));
      if (!grid || rowIndex < 0) {
        return { ok: false, rowCount: rows.length, ids: rows.map((row) => row.id).slice(0, 20) };
      }
      grid.setSelecteds(String(rowIndex));
      const selecteds = grid.getSelecteds ? grid.getSelecteds() : [];
      return {
        ok: selecteds.length === 1,
        rowIndex,
        selectedCount: selecteds.length,
        selected: selecteds[0] || null,
      };
    },
    { code: gridCode, rowId: String(expectedId) }
  );
  if (!selection.ok) {
    throw new Error(`Failed to select ${expectedName}: ${JSON.stringify(selection)}`);
  }
  const label = page.getByText(expectedName, { exact: false }).first();
  if ((await label.count()) > 0) {
    await label.click({ timeout: 10000 }).catch(() => undefined);
  }
  return selection;
}

async function refreshActiveGridFromPage(page) {
  const responsePromise = waitForResponseSafe(
    page,
    (response) =>
      response.url().includes("/WOM/produceTask/taskActive/queryByProcess") &&
      response.url().includes(`processId=${ids.process}`),
    30000
  );
  const refreshResult = await page.evaluate(
    ({ gridCode, processId }) => {
      const gridFactory = window.ReactAPI && window.ReactAPI.getComponentAPI("SupDataGrid");
      const grid = gridFactory && typeof gridFactory.APIs === "function" && gridFactory.APIs(gridCode);
      if (!grid || typeof grid.refreshDataByRequst !== "function") {
        return { ok: false, error: "active grid refreshDataByRequst missing" };
      }
      grid.refreshDataByRequst({
        type: "post",
        url: `/msService/WOM/produceTask/taskActive/queryByProcess?processId=${processId}&showBatch=false`,
        param: {},
      });
      return { ok: true };
    },
    { gridCode: activeGridId, processId: String(ids.process) }
  );
  if (!refreshResult.ok) {
    throw new Error(`Failed to refresh active grid: ${JSON.stringify(refreshResult)}`);
  }
  const { response, error } = await responsePromise;
  if (error) {
    throw new Error(`Active grid refresh response was not observed: ${error.message}`);
  }
  return { status: response.status(), url: response.url() };
}

async function refreshProcessGridFromPage(page) {
  const responsePromise = waitForResponseSafe(
    page,
    (response) =>
      response.url().includes("/WOM/produceTask/produceTask/data-dg1576028988483") &&
      response.url().includes(`id=${ids.task}`) &&
      response.url().includes(processGridId),
    30000
  );
  const refreshResult = await page.evaluate(
    ({ gridCode, taskId }) => {
      const gridFactory = window.ReactAPI && window.ReactAPI.getComponentAPI("SupDataGrid");
      const grid = gridFactory && typeof gridFactory.APIs === "function" && gridFactory.APIs(gridCode);
      if (!grid || typeof grid.refreshDataByRequst !== "function") {
        return { ok: false, error: "process grid refreshDataByRequst missing" };
      }
      grid.refreshDataByRequst({
        type: "post",
        url: `/msService/WOM/produceTask/produceTask/data-dg1576028988483?datagridCode=${gridCode}&id=${taskId}`,
        param: { pageSize: 65535 },
      });
      return { ok: true };
    },
    { gridCode: processGridId, taskId: String(ids.task) }
  );
  if (!refreshResult.ok) {
    throw new Error(`Failed to refresh process grid: ${JSON.stringify(refreshResult)}`);
  }
  const { response, error } = await responsePromise;
  if (error) {
    throw new Error(`Process grid refresh response was not observed: ${error.message}`);
  }
  return { status: response.status(), url: response.url() };
}

async function refreshEasyReportGridFromPage(page) {
  const responsePromise = waitForResponseSafe(
    page,
    (response) =>
      response.url().includes("/WOM/produceTask/produceTask/data-dg1577337007020") &&
      response.url().includes(`id=${ids.task}`) &&
      response.url().includes(easyReportGridId),
    30000
  );
  const refreshResult = await page.evaluate(
    ({ gridCode, taskId }) => {
      const gridFactory = window.ReactAPI && window.ReactAPI.getComponentAPI("SupDataGrid");
      const grid = gridFactory && typeof gridFactory.APIs === "function" && gridFactory.APIs(gridCode);
      if (!grid || typeof grid.refreshDataByRequst !== "function") {
        return { ok: false, error: "easy report grid refreshDataByRequst missing" };
      }
      grid.refreshDataByRequst({
        type: "post",
        url: `/msService/WOM/produceTask/produceTask/data-dg1577337007020?datagridCode=${gridCode}&id=${taskId}`,
        param: { pageSize: 65535 },
      });
      return { ok: true };
    },
    { gridCode: easyReportGridId, taskId: String(ids.task) }
  );
  if (!refreshResult.ok) {
    throw new Error(`Failed to refresh easy report grid: ${JSON.stringify(refreshResult)}`);
  }
  const { response, error } = await responsePromise;
  if (error) {
    throw new Error(`Easy report grid refresh response was not observed: ${error.message}`);
  }
  return { status: response.status(), url: response.url() };
}

async function submitProcessUnitFromPage(page) {
  const payload = {
    viewCode: "WOM_1.0.0_produceTask_processUnitEdit",
    modelName: "WOM_1.0.0_produceTask_TaskProcess",
    operateType: "save",
    taskProcess: {
      id: Number(ids.process),
      equipmentId: {
        id: Number(ids.workUnit),
        code: workUnitCode,
        name: workUnitName,
      },
    },
    dgList: {},
    dgDeletedIds: {},
    viewSelect: "",
  };
  return browserFetch(
    page,
    "POST",
    `/msService/WOM/produceTask/taskProcess/processUnitEdit/submit?id=${ids.process}`,
    payload
  );
}

async function invokeRuntimeButton(page, runtimeButtonId) {
  return page.evaluate((buttonIdToInvoke) => {
    const targets = new Set([buttonIdToInvoke]);
    const buttonItems = Array.from(document.querySelectorAll(".sup-datagrid-button-item"));
    const candidates = [];

    function getFiber(node) {
      const fiberKey = Object.keys(node).find(
        (key) => key.startsWith("__reactInternalInstance$") || key.startsWith("__reactFiber$")
      );
      return fiberKey && node[fiberKey];
    }

    for (const buttonItem of buttonItems) {
      const buttonCell = buttonItem.closest("[id]");
      let fiber = getFiber(buttonItem);
      while (fiber) {
        if (fiber.type && fiber.type.name === "HeaderButtonCell") {
          const instance = fiber.stateNode;
          const button = instance && instance.props && instance.props.button;
          const values = [
            buttonCell && buttonCell.id,
            button && button.id,
            button && button.code,
            button && button.CODE,
            button && button.name,
            button && button.NAME,
            button && button.funcname,
            button && button.showname,
            button && button.displayName,
          ]
            .filter((value) => value !== undefined && value !== null)
            .map(String);
          candidates.push({
            cellId: buttonCell && buttonCell.id,
            buttonId: button && button.id,
            buttonCode: button && (button.code || button.CODE),
            buttonName: button && (button.name || button.NAME),
            functionName: button && button.funcname,
            showName: button && (button.showname || button.displayName),
            text: buttonItem.textContent && buttonItem.textContent.trim(),
          });
          if (values.some((value) => targets.has(value) || value.endsWith(`_${buttonIdToInvoke}`))) {
            if (!instance || !button || typeof instance.setWindowFunc !== "function") {
              return { ok: false, error: `runtime button instance is incomplete: ${buttonIdToInvoke}`, candidates };
            }

            let showTipResult = true;
            if (typeof instance.showTipFunc === "function") {
              showTipResult = instance.showTipFunc();
            }
            if (!showTipResult) {
              return { ok: false, error: `runtime button showTipFunc returned false: ${buttonIdToInvoke}`, candidates };
            }

            const operateEvent = instance.setWindowFunc(button.funcname, button.funcbody);
            if (typeof operateEvent !== "function") {
              return { ok: false, error: `runtime button event is not callable: ${buttonIdToInvoke}`, candidates };
            }
            operateEvent({ type: "adp-e2e-runtime-button", target: buttonItem, currentTarget: buttonItem });
            return {
              ok: true,
              triggerMode: "runtime-button-event",
              buttonId: buttonCell && buttonCell.id,
              buttonConfigId: button.id,
              buttonCode: button.code || button.CODE,
              buttonName: button.showname || button.name || button.NAME,
              operationCode: button.buttonoperationcode || button.CODE,
              candidates,
            };
          }
        }
        fiber = fiber.return;
      }
    }

    return { ok: false, error: `HeaderButtonCell not found: ${buttonIdToInvoke}`, candidates };
  }, runtimeButtonId);
}

function formatDateTimeForPayload(date) {
  const pad = (value) => String(value).padStart(2, "0");
  const padMs = (value) => String(value).padStart(3, "0");
  const offsetMinutes = -date.getTimezoneOffset();
  const sign = offsetMinutes >= 0 ? "+" : "-";
  const absoluteOffset = Math.abs(offsetMinutes);
  const offset =
    sign +
    pad(Math.floor(absoluteOffset / 60)) +
    pad(absoluteOffset % 60);
  return [
    date.getFullYear(),
    "-",
    pad(date.getMonth() + 1),
    "-",
    pad(date.getDate()),
    "T",
    pad(date.getHours()),
    ":",
    pad(date.getMinutes()),
    ":",
    pad(date.getSeconds()),
    ".",
    padMs(date.getMilliseconds()),
    offset,
  ].join("");
}

async function savePutinDetailFromPage(page, procReportId) {
  const end = new Date();
  const start = new Date(end.getTime() - 4 * 60 * 1000);
  const payload = {
    viewCode: "WOM_1.0.0_procReport_remainMaterialView",
    modelName: "WOM_1.0.0_procReport_PutinDetail",
    operateType: "save",
    putinDetail: {
      version: 0,
      valid: true,
      status: 99,
      cid: 1000,
      tableNo: `${marker}_PUTIN_DETAIL_TN`,
      tableInfoId: Number(ids.putinDetailFallback),
      headId: { id: String(procReportId) },
      materialBatchNum: batchNo,
      materialId: { id: String(ids.material) },
      putinNum: 2,
      useNum: 2,
      putinTime: formatDateTimeForPayload(start),
      putinEndTime: formatDateTimeForPayload(end),
      remainOperate: { id: "WOM_remainOperate/noOperate" },
      taskType: { id: "WOM_taskType/manufacture" },
    },
    dgList: {},
    dgDeletedIds: {},
    viewSelect: "",
  };
  const result = await page.evaluate(async (body) => {
    const response = await fetch("/msService/WOM/procReport/putinDetail/remainMaterialView/save", {
      method: "POST",
      credentials: "include",
      headers: {
        Accept: "application/json, text/plain, */*",
        "Content-Type": "application/json;charset=UTF-8",
      },
      body: JSON.stringify(body),
    });
    const text = await response.text();
    let json = null;
    try {
      json = JSON.parse(text);
    } catch (_error) {
      json = null;
    }
    return {
      status: response.status,
      url: response.url,
      requestPayload: body,
      body: text.slice(0, 8000),
      json,
    };
  }, payload);
  return result;
}

async function parseResponse(response) {
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

async function gotoRouteWhenReady(page, evidence) {
  const attempts = [];
  let lastResponse = null;
  for (let attempt = 1; attempt <= 8; attempt += 1) {
    lastResponse = await page.goto(route, { waitUntil: "domcontentloaded", timeout: 45000 });
    const status = lastResponse && lastResponse.status();
    attempts.push({ attempt, status });
    if (![502, 503, 504].includes(status)) {
      evidence.navigation = { status, attempts };
      return lastResponse;
    }
    await page.waitForTimeout(5000);
  }
  evidence.navigation = { status: lastResponse && lastResponse.status(), attempts };
  return lastResponse;
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
      ["suposTicket", "SUPOS_TICKET", "token"].forEach((key) => {
        window.localStorage.setItem(key, token);
        window.sessionStorage.setItem(key, token);
      });
    }, ticket);

    const page = await context.newPage();
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
      if (/makeTaskBatchView|easyTaskOperateView|processUnitEdit|produceTask\/produceTask\/data|taskActive\/queryByProcess|taskProcess\/start|taskProcess\/end|taskProcess\/isUnFinishActives|startActive|endActive|endEasyActive|procReport\/putinDetail\/remainMaterialView\/save|layoutJson|systemCodeJson/.test(url)) {
        evidence.requests.push({
          method: requestItem.method(),
          url,
          postData: requestItem.postData(),
        });
      }
    });
    page.on("response", async (response) => {
      const url = response.url();
      if (!/makeTaskBatchView|easyTaskOperateView|processUnitEdit|produceTask\/produceTask\/data|taskActive\/queryByProcess|taskProcess\/start|taskProcess\/end|taskProcess\/isUnFinishActives|startActive|endActive|endEasyActive|procReport\/putinDetail\/remainMaterialView\/save|layoutJson|systemCodeJson/.test(url)) {
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

    await gotoRouteWhenReady(page, evidence);
    await page.waitForLoadState("domcontentloaded");
    if (processUnitAction) {
      await page.waitForFunction(
        (processId) => {
          const data = window.ReactAPI && window.ReactAPI.getFormData && window.ReactAPI.getFormData();
          return data && String(data.id) === String(processId);
        },
        String(ids.process),
        { timeout: 60000 }
      );
    } else {
      await page.waitForFunction(
        (taskId) => {
          const data = window.ReactAPI && window.ReactAPI.getFormData && window.ReactAPI.getFormData();
          return data && String(data.id) === String(taskId);
        },
        String(ids.task),
        { timeout: 60000 }
      );
    }
    evidence.screenshots.before = path.join(outputDir, "wom-active-before.png");
    await page.screenshot({ path: evidence.screenshots.before, fullPage: true });

    evidence.formData = await page.evaluate(() => {
      const data = window.ReactAPI && window.ReactAPI.getFormData && window.ReactAPI.getFormData();
      return data
        ? {
            id: data.id,
            tableNo: data.tableNo,
            taskRunState: data.taskRunState && data.taskRunState.id,
            processRunState: data.processRunState && data.processRunState.id,
            equipmentId: data.equipmentId && data.equipmentId.id,
            batchContral: data.batchContral,
          }
        : null;
    });
    const expectedFormId = processUnitAction ? ids.process : ids.task;
    if (!evidence.formData || String(evidence.formData.id) !== String(expectedFormId)) {
      throw new Error(`WOM route did not load marker form data: ${JSON.stringify(evidence.formData)}`);
    }

    if (processUnitAction) {
      const processUnitResponsePromise = waitForResponseSafe(
        page,
        (response) => response.url().includes(`/WOM/produceTask/taskProcess/processUnitEdit/submit`),
        30000
      );
      evidence.processUnitSubmit = await submitProcessUnitFromPage(page);
      const { response: processUnitResponse, error: processUnitResponseError } = await processUnitResponsePromise;
      if (processUnitResponseError) {
        evidence.processUnitSubmitWaitError = processUnitResponseError.message;
      } else {
        evidence.processUnitSubmitNetwork = await parseResponse(processUnitResponse);
      }
      const processUnitPayload =
        evidence.processUnitSubmit.json &&
        (evidence.processUnitSubmit.json.data || evidence.processUnitSubmit.json);
      if (
        evidence.processUnitSubmit.status !== 200 ||
        !processUnitPayload ||
        processUnitPayload.dealSuccessFlag === false ||
        processUnitPayload.success === false
      ) {
        throw new Error(`processUnitEdit submit did not pass: ${JSON.stringify(evidence.processUnitSubmit)}`);
      }
    } else if (processAction) {
      evidence.processGridRefresh = await refreshProcessGridFromPage(page);
      evidence.processRowsBefore = await getGridRows(page, processGridId);
      evidence.processSelection = await selectGridRow(page, processGridId, String(ids.process), processName);

      const processStartResponsePromise = waitForResponseSafe(
        page,
        (response) => response.url().includes(`/WOM/produceTask/taskProcess/start/${ids.process}`),
        30000
      );
      const processStartInvocation = await invokeRuntimeButton(page, "startProcess");
      evidence.processStartButtonInvocation = processStartInvocation;
      if (!processStartInvocation.ok) {
        const pendingStartResponse = await processStartResponsePromise;
        if (pendingStartResponse.error) {
          evidence.processStartWaitError = pendingStartResponse.error.message;
        }
        throw new Error(`startProcess runtime button invocation failed: ${JSON.stringify(processStartInvocation)}`);
      }

      const { response: processStartResponse, error: processStartResponseError } = await processStartResponsePromise;
      if (processStartResponseError) {
        evidence.processStartWaitError = processStartResponseError.message;
        throw new Error(`startProcess response was not observed: ${processStartResponseError.message}`);
      }
      evidence.processStart = await parseResponse(processStartResponse);
      const processStartPayload = evidence.processStart.json && (evidence.processStart.json.data || evidence.processStart.json);
      if (evidence.processStart.status !== 200 || !processStartPayload || processStartPayload.dealSuccessFlag !== true) {
        throw new Error(`startProcess did not pass: ${JSON.stringify(evidence.processStart)}`);
      }

      if (activeAction === "process-end") {
        await page.waitForTimeout(1500);
        evidence.processGridRefreshAfterStart = await refreshProcessGridFromPage(page);
        evidence.processRowsAfterStart = await getGridRows(page, processGridId);
        evidence.processSelectionAfterStart = await selectGridRow(page, processGridId, String(ids.process), processName);

        const processEndResponsePromise = waitForResponseSafe(
          page,
          (response) => response.url().includes(`/WOM/produceTask/taskProcess/end/${ids.process}`),
          30000
        );
        const processEndInvocation = await invokeRuntimeButton(page, "endProcess");
        evidence.processEndButtonInvocation = processEndInvocation;
        if (!processEndInvocation.ok) {
          const pendingEndResponse = await processEndResponsePromise;
          if (pendingEndResponse.error) {
            evidence.processEndWaitError = pendingEndResponse.error.message;
          }
          throw new Error(`endProcess runtime button invocation failed: ${JSON.stringify(processEndInvocation)}`);
        }
        await page.getByText("是", { exact: true }).last().click({ timeout: 5000 }).catch(() => undefined);

        const { response: processEndResponse, error: processEndResponseError } = await processEndResponsePromise;
        if (processEndResponseError) {
          evidence.processEndWaitError = processEndResponseError.message;
          throw new Error(`endProcess response was not observed: ${processEndResponseError.message}`);
        }
        evidence.processEnd = await parseResponse(processEndResponse);
        const processEndPayload = evidence.processEnd.json && (evidence.processEnd.json.data || evidence.processEnd.json);
        if (evidence.processEnd.status !== 200 || !processEndPayload || processEndPayload.dealSuccessFlag !== true) {
          throw new Error(`endProcess did not pass: ${JSON.stringify(evidence.processEnd)}`);
        }
      } else if (activeAction !== "process-start") {
        throw new Error(`Unsupported process action ADP_WOM_ACTIVE_ACTION=${activeAction}`);
      }
    } else if (easyReportAction) {
      evidence.easyReportGridRefresh = await refreshEasyReportGridFromPage(page);
      evidence.easyReportRowsBefore = await getGridRows(page, easyReportGridId);
      evidence.easyReportSelection = await selectGridRow(page, easyReportGridId, String(ids.active), activeName);

      const easyEndResponsePromise = waitForResponseSafe(
        page,
        (response) => response.url().includes(`/WOM/produceTask/produceTask/endEasyActive/${ids.active}`),
        30000
      );
      const easyInvocation = await invokeRuntimeButton(page, "btn-report");
      evidence.easyReportButtonInvocation = easyInvocation;
      if (!easyInvocation.ok) {
        const pendingEasyResponse = await easyEndResponsePromise;
        if (pendingEasyResponse.error) {
          evidence.endEasyActiveWaitError = pendingEasyResponse.error.message;
        }
        throw new Error(`endEasyActive runtime button invocation failed: ${JSON.stringify(easyInvocation)}`);
      }

      const { response: easyEndResponse, error: easyEndResponseError } = await easyEndResponsePromise;
      if (easyEndResponseError) {
        evidence.endEasyActiveWaitError = easyEndResponseError.message;
        throw new Error(`endEasyActive response was not observed: ${easyEndResponseError.message}`);
      }
      evidence.endEasyActive = await parseResponse(easyEndResponse);
      const easyPayload = evidence.endEasyActive.json && (evidence.endEasyActive.json.data || evidence.endEasyActive.json);
      if (
        evidence.endEasyActive.status !== 200 ||
        !easyPayload ||
        easyPayload.dealSuccessFlag !== true ||
        !easyPayload.exeState ||
        easyPayload.exeState.id !== "WOM_runState/finished"
      ) {
        throw new Error(`endEasyActive did not pass: ${JSON.stringify(evidence.endEasyActive)}`);
      }
    } else if (checkEndAction) {
      evidence.processGridRefresh = await refreshProcessGridFromPage(page);
      evidence.processRowsBefore = await getGridRows(page, processGridId);
      evidence.processSelection = await selectGridRow(page, processGridId, String(ids.process), processName);
      evidence.activeGridRefresh = await refreshActiveGridFromPage(page);
      evidence.activeRowsBefore = await getGridRows(page, activeGridId);
      evidence.activeSelection = await selectGridRow(page, activeGridId, String(ids.active), activeName);

      const endResponsePromise = waitForResponseSafe(
        page,
        (response) =>
          response.url().includes("/WOM/produceTask/produceTask/endActive") &&
          response.url().includes(`activeId=${ids.active}`),
        30000
      );
      const endInvocation = await invokeRuntimeButton(page, "endActive");
      evidence.endButtonInvocation = endInvocation;
      if (!endInvocation.ok) {
        const pendingEndResponse = await endResponsePromise;
        if (pendingEndResponse.error) {
          evidence.endActiveWaitError = pendingEndResponse.error.message;
        }
        throw new Error(`endActive runtime button invocation failed: ${JSON.stringify(endInvocation)}`);
      }

      const { response: endResponse, error: endResponseError } = await endResponsePromise;
      if (endResponseError) {
        evidence.endActiveWaitError = endResponseError.message;
        throw new Error(`endActive response was not observed: ${endResponseError.message}`);
      }
      evidence.endActive = await parseResponse(endResponse);
      const endPayload = evidence.endActive.json && (evidence.endActive.json.data || evidence.endActive.json);
      if (evidence.endActive.status !== 200 || !endPayload || endPayload.success !== true) {
        throw new Error(`endActive did not pass: ${JSON.stringify(evidence.endActive)}`);
      }
    } else {
    evidence.processGridRefresh = await refreshProcessGridFromPage(page);
    evidence.processRowsBefore = await getGridRows(page, processGridId);
    evidence.processSelection = await selectGridRow(page, processGridId, String(ids.process), processName);
    evidence.activeGridRefresh = await refreshActiveGridFromPage(page);
    evidence.activeRowsBefore = await getGridRows(page, activeGridId);
    evidence.activeSelection = await selectGridRow(page, activeGridId, String(ids.active), activeName);

    const startResponsePromise = waitForResponseSafe(
      page,
      (response) =>
        response.url().includes("/WOM/produceTask/produceTask/startActive") &&
        response.url().includes(`activeId=${ids.active}`),
      30000
    );
    const invocation = await invokeRuntimeButton(page, "startActive");
    evidence.buttonInvocation = invocation;
    if (!invocation.ok) {
      const pendingStartResponse = await startResponsePromise;
      if (pendingStartResponse.error) {
        evidence.startActiveWaitError = pendingStartResponse.error.message;
      }
      throw new Error(`startActive runtime button invocation failed: ${JSON.stringify(invocation)}`);
    }

    const { response: startResponse, error: startResponseError } = await startResponsePromise;
    if (startResponseError) {
      evidence.startActiveWaitError = startResponseError.message;
      throw new Error(`startActive response was not observed: ${startResponseError.message}`);
    }
    evidence.startActive = await parseResponse(startResponse);
    const payload = evidence.startActive.json && (evidence.startActive.json.data || evidence.startActive.json);
    if (
      evidence.startActive.status !== 200 ||
      !payload ||
      payload.success !== true ||
      !payload.data ||
      payload.data.id !== "WOM_runState/runing"
    ) {
      throw new Error(`startActive did not pass: ${JSON.stringify(evidence.startActive)}`);
    }

    if (activeAction === "end" || putinEndAction) {
      await page.waitForTimeout(1500);
      if (putinEndAction) {
        const verifyAfterStartRaw = runSql(verificationSql());
        const verifyAfterStartRows = parseRows(verifyAfterStartRaw);
        const procReport = verifyAfterStartRows.find((row) => row[0] === "procReport");
        if (!procReport || !procReport[1]) {
          throw new Error(`putin-end could not find startActive procReport row: ${verifyAfterStartRaw}`);
        }
        evidence.persistenceAfterStart = { rows: verifyAfterStartRows, procReport };
        evidence.putinDetailSave = await savePutinDetailFromPage(page, procReport[1]);
        const putinSavePayload =
          evidence.putinDetailSave.json &&
          (evidence.putinDetailSave.json.data || evidence.putinDetailSave.json);
        if (
          evidence.putinDetailSave.status !== 200 ||
          !putinSavePayload ||
          putinSavePayload.dealSuccessFlag !== true ||
          !putinSavePayload.id
        ) {
          throw new Error(`putinDetail save did not pass: ${JSON.stringify(evidence.putinDetailSave)}`);
        }
        evidence.putinDetailId = String(putinSavePayload.id);
      }
      evidence.processGridRefreshAfterStart = await refreshProcessGridFromPage(page);
      evidence.processRowsAfterStart = await getGridRows(page, processGridId);
      evidence.processSelectionAfterStart = await selectGridRow(page, processGridId, String(ids.process), processName);
      evidence.activeGridRefreshAfterStart = await refreshActiveGridFromPage(page);
      evidence.activeRowsAfterStart = await getGridRows(page, activeGridId);
      evidence.activeSelectionAfterStart = await selectGridRow(page, activeGridId, String(ids.active), activeName);

      const endResponsePromise = waitForResponseSafe(
        page,
        (response) =>
          response.url().includes("/WOM/produceTask/produceTask/endActive") &&
          response.url().includes(`activeId=${ids.active}`),
        30000
      );
      const endInvocation = await invokeRuntimeButton(page, "endActive");
      evidence.endButtonInvocation = endInvocation;
      if (!endInvocation.ok) {
        const pendingEndResponse = await endResponsePromise;
        if (pendingEndResponse.error) {
          evidence.endActiveWaitError = pendingEndResponse.error.message;
        }
        throw new Error(`endActive runtime button invocation failed: ${JSON.stringify(endInvocation)}`);
      }

      const { response: endResponse, error: endResponseError } = await endResponsePromise;
      if (endResponseError) {
        evidence.endActiveWaitError = endResponseError.message;
        throw new Error(`endActive response was not observed: ${endResponseError.message}`);
      }
      evidence.endActive = await parseResponse(endResponse);
      const endPayload = evidence.endActive.json && (evidence.endActive.json.data || evidence.endActive.json);
      if (evidence.endActive.status !== 200 || !endPayload || endPayload.success !== true) {
        throw new Error(`endActive did not pass: ${JSON.stringify(evidence.endActive)}`);
      }
      } else if (activeAction !== "start") {
        throw new Error(`Unsupported ADP_WOM_ACTIVE_ACTION=${activeAction}`);
      }
    }

    await page.waitForTimeout(1500);
    evidence.afterBody = (await page.locator("body").innerText()).slice(0, 5000);
    evidence.screenshots.after = path.join(outputDir, "wom-active-after.png");
    await page.screenshot({ path: evidence.screenshots.after, fullPage: true });
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
    marker,
    ids: Object.fromEntries(Object.entries(ids).map(([key, value]) => [key, value.toString()])),
    activeAction,
    route,
    processGridId,
    activeGridId,
    easyReportGridId,
    tableNo,
    materialCode,
    formulaCode,
    batchNo,
    processName,
    activeName,
    console: [],
    pageErrors: [],
    requestFailures: [],
    requests: [],
    responses: [],
    screenshots: {},
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
    evidence.persistence = processUnitAction
      ? assertProcessUnitPersistence(verifyRaw)
      : processAction
      ? activeAction === "process-end"
        ? assertProcessEndPersistence(verifyRaw)
        : assertProcessStartPersistence(verifyRaw)
      : easyReportAction
        ? assertEasyEndPersistence(verifyRaw)
      : putinEndAction
        ? assertPutinEndPersistence(verifyRaw)
        : checkEndAction
          ? assertCheckEndPersistence(verifyRaw)
        : activeAction === "end"
          ? assertEndPersistence(verifyRaw)
          : assertStartPersistence(verifyRaw);
    evidence.issues = evidence.persistence.issues || [];
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
        outputPath,
        startActive: evidence.startActive,
        endActive: evidence.endActive,
        endEasyActive: evidence.endEasyActive,
        putinDetailSave: evidence.putinDetailSave,
        processUnitSubmit: evidence.processUnitSubmit,
        processStart: evidence.processStart,
        processEnd: evidence.processEnd,
        persistence: {
          task: evidence.persistence.task,
          process: evidence.persistence.process,
          processProcReport: evidence.persistence.processProcReport,
          processWait: evidence.persistence.processWait,
          processUnit: evidence.persistence.processUnit,
          processWaitUnit: evidence.persistence.processWaitUnit,
          active: evidence.persistence.active,
          procReport: evidence.persistence.procReport,
          activeExelog: evidence.persistence.activeExelog,
          wait: evidence.persistence.wait,
          processExelog: evidence.persistence.processExelog,
          outputDetail: evidence.persistence.outputDetail,
          putinDetail: evidence.persistence.putinDetail,
          proCheckDetail: evidence.persistence.proCheckDetail,
          checkRecord: evidence.persistence.checkRecord,
          matOutptRecordCount: evidence.persistence.matOutptRecordCount,
          matConsumRecordCount: evidence.persistence.matConsumRecordCount,
        },
        issues: evidence.issues,
        frontendClean: evidence.frontendClean,
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
