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
const pageTimeoutMs = Number(process.env.ADP_PAGE_TIMEOUT_MS || 120000);
const navigationWaitUntil = process.env.ADP_NAV_WAIT_UNTIL || "commit";
const nowToken = new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14);
const marker = process.env.ADP_E2E_MARKER || `ADP_E2E_${nowToken}_WOM_CHECKOUTBILL`;
const outputDir =
  process.env.ADP_OUTPUT_DIR || path.join("/tmp", `adp-wom-checkoutbill-persistence-${nowToken}`);
const outputPath =
  process.env.ADP_WOM_CHECKOUTBILL_PERSISTENCE_OUTPUT ||
  path.join(outputDir, "wom-checkoutbill-persistence-results.json");

// Keep ids below Number.MAX_SAFE_INTEGER so legacy frontend JSON can round-trip them.
const idBase = 8991000000000000n + BigInt(Date.now() % 900000000) * 100n + BigInt(process.pid % 100);
const ids = {
  material: idBase + 1n,
  formula: idBase + 2n,
  qualityStd: idBase + 3n,
  stdVersion: idBase + 4n,
  analyProdStd: idBase + 5n,
  formulaQuality: idBase + 6n,
  batchInfo: idBase + 7n,
  task: idBase + 8n,
  process: idBase + 9n,
  active: idBase + 10n,
  wait: idBase + 11n,
  taskExelog: idBase + 12n,
  processExelog: idBase + 13n,
  workUnit: idBase + 14n,
  testComponent: idBase + 15n,
  stdVerCom: idBase + 16n,
  stdVerGradeUnqualified: idBase + 17n,
  stdVerGradeQualified: idBase + 18n,
  specLimit: idBase + 19n,
};

const tableNo = `${marker}_TASK_TN`;
const materialCode = `${marker}_MAT`;
const formulaCode = `${marker}_FORM`;
const qualityStdCode = `${marker}_STD`;
const batchNo = `${marker}_BATCH`;
const workUnitCode = `${marker}_WU`;
const processName = `${marker} process`;
const activeName = `${marker} quality active`;
const qcsReportItemCode = `${marker}_QCS_REPORT_ITEM`;
const qcsReportItemName = `${marker} QCS report item`;
const route = `/msService/WOM/produceTask/produceTask/makeTaskBatchView?id=${ids.task}`;
const checkoutBillApi = `/msService/WOM/produceTask/produceTask/checkoutBill/generate/${ids.active}`;
const processGridId = "WOM_1.0.0_produceTask_makeTaskBatchViewdg1586416569990";
const activeGridId = "WOM_1.0.0_produceTask_makeTaskBatchViewdg1586416570027";

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true });
}

function shellQuote(value) {
  return `'${String(value).replace(/'/g, "'\\''")}'`;
}

function sqlLiteral(value) {
  return `'${String(value).replace(/'/g, "''")}'`;
}

function sqlClobLiteral(value) {
  if (value == null) {
    return "NULL";
  }
  return `lo_from_bytea(0, convert_to(${sqlLiteral(value)}, 'UTF8'))`;
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

function isWaitForCheckState(value) {
  return value === "BaseSet_checkState/waitForCheck" || value === "待检";
}

function isFinishedState(value) {
  return value === "WOM_runState/finished";
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
  id, ${commonCols}, status, table_no, table_info_id, code, name, is_batch
) VALUES (
  ${ids.material}, ${commonVals}, 99, ${sqlLiteral(`${materialCode}_TN`)}, ${ids.material}, ${sqlLiteral(materialCode)}, ${sqlLiteral(`${marker} material`)}, 'BaseSet_isBatch/batch'
) ON CONFLICT (id) DO UPDATE SET
  valid = true,
  status = 99,
  code = EXCLUDED.code,
  name = EXCLUDED.name,
  is_batch = EXCLUDED.is_batch,
  modify_time = now();

INSERT INTO public.limsba_quality_stds (
  id, ${commonCols}, status, table_no, table_info_id, code, name, standard, is_default, leaf
) VALUES (
  ${ids.qualityStd}, ${commonVals}, 99, ${sqlLiteral(`${marker}_QUALITY_STD_TN`)}, ${ids.qualityStd}, ${sqlLiteral(qualityStdCode)}, ${sqlLiteral(`${marker} quality standard`)}, ${sqlLiteral(`${marker} standard`)}, true, true
) ON CONFLICT (id) DO UPDATE SET
  valid = true,
  status = 99,
  code = EXCLUDED.code,
  name = EXCLUDED.name,
  standard = EXCLUDED.standard,
  is_default = EXCLUDED.is_default,
  modify_time = now();

INSERT INTO public.limsba_std_versions (
  id, ${commonCols}, status, table_no, table_info_id, code, name, busi_version,
  std_id, active, start_date, end_date, leaf
) VALUES (
  ${ids.stdVersion}, ${commonVals}, 99, ${sqlLiteral(`${marker}_STD_VERSION_TN`)}, ${ids.stdVersion}, ${sqlLiteral(`${qualityStdCode}_V1`)}, ${sqlLiteral(`${marker} std version`)}, 'V1',
  ${ids.qualityStd}, true, now() - interval '1 day', now() + interval '365 day', true
) ON CONFLICT (id) DO UPDATE SET
  valid = true,
  status = 99,
  code = EXCLUDED.code,
  name = EXCLUDED.name,
  std_id = EXCLUDED.std_id,
  active = EXCLUDED.active,
  start_date = EXCLUDED.start_date,
  end_date = EXCLUDED.end_date,
  modify_time = now();

INSERT INTO public.limsba_test_components (
  id, version, valid, cid, create_staff_id, create_time, sort, table_info_id,
  code, name, report_name, unit_name, is_report, is_necessary, parallel_times,
  memo_field
) VALUES (
  ${ids.testComponent}, 0, true, 1000, 1, now(), 1, ${ids.testComponent},
  ${sqlLiteral(qcsReportItemCode)}, ${sqlLiteral(qcsReportItemName)}, ${sqlLiteral(qcsReportItemName)}, 'EA', true, true, 1,
  ${sqlLiteral(marker)}
) ON CONFLICT (id) DO UPDATE SET
  valid = true,
  code = EXCLUDED.code,
  name = EXCLUDED.name,
  report_name = EXCLUDED.report_name,
  unit_name = EXCLUDED.unit_name,
  is_report = EXCLUDED.is_report,
  is_necessary = EXCLUDED.is_necessary,
  parallel_times = EXCLUDED.parallel_times,
  memo_field = EXCLUDED.memo_field,
  modify_time = now();

INSERT INTO public.limsba_std_ver_coms (
  id, version, valid, cid, create_staff_id, create_time, sort, table_info_id,
  std_id, std_ver_id, com_id, code, is_report, report_name, report_sort,
  unit_name, parallel_times, valuen, sampling_plan, memo_field
) VALUES (
  ${ids.stdVerCom}, 0, true, 1000, 1, now(), 1, ${ids.stdVerCom},
  ${ids.qualityStd}, ${ids.stdVersion}, ${ids.testComponent}, ${sqlLiteral(`${qcsReportItemCode}_STD_VER`)}, true, ${sqlLiteral(qcsReportItemName)}, 1,
  'EA', 1, 1, 'LIMSBasic_samplingPlan/level3', ${sqlLiteral(marker)}
) ON CONFLICT (id) DO UPDATE SET
  valid = true,
  std_id = EXCLUDED.std_id,
  std_ver_id = EXCLUDED.std_ver_id,
  com_id = EXCLUDED.com_id,
  code = EXCLUDED.code,
  is_report = EXCLUDED.is_report,
  report_name = EXCLUDED.report_name,
  report_sort = EXCLUDED.report_sort,
  unit_name = EXCLUDED.unit_name,
  parallel_times = EXCLUDED.parallel_times,
  valuen = EXCLUDED.valuen,
  sampling_plan = EXCLUDED.sampling_plan,
  memo_field = EXCLUDED.memo_field,
  modify_time = now();

INSERT INTO public.limsba_std_ver_grades (
  id, version, valid, cid, create_staff_id, create_time, sort, table_info_id,
  std_grade, std_id, std_ver_id, code, name, memo_field
) VALUES
  (
    ${ids.stdVerGradeUnqualified}, 0, true, 1000, 1, now(), 20, ${ids.stdVerGradeUnqualified},
    'LIMSBasic_standardGrade/Unqualified', ${ids.qualityStd}, ${ids.stdVersion}, ${sqlLiteral(`${marker}_GRADE_UNQUALIFIED`)}, '不合格', ${sqlLiteral(marker)}
  ),
  (
    ${ids.stdVerGradeQualified}, 0, true, 1000, 1, now(), 10, ${ids.stdVerGradeQualified},
    'LIMSBasic_standardGrade/Qualified', ${ids.qualityStd}, ${ids.stdVersion}, ${sqlLiteral(`${marker}_GRADE_QUALIFIED`)}, '合格', ${sqlLiteral(marker)}
  )
ON CONFLICT (id) DO UPDATE SET
  valid = true,
  sort = EXCLUDED.sort,
  std_grade = EXCLUDED.std_grade,
  std_id = EXCLUDED.std_id,
  std_ver_id = EXCLUDED.std_ver_id,
  code = EXCLUDED.code,
  name = EXCLUDED.name,
  memo_field = EXCLUDED.memo_field,
  modify_time = now();

INSERT INTO public.limsba_spec_limits (
  id, version, valid, cid, create_staff_id, create_time, sort, table_info_id,
  code, disp_value, standard_grade, std_grade_name, std_id, std_ver_com_id,
  sampling_plan, valuen, result_value
) VALUES (
  ${ids.specLimit}, 0, true, 1000, 1, now(), 10, ${ids.specLimit},
  ${sqlLiteral(`${marker}_SPEC_LIMIT_QUALIFIED`)}, '合格', 'LIMSBasic_standardGrade/Qualified', '合格',
  ${ids.qualityStd}, ${ids.stdVerCom}, 'LIMSBasic_samplingPlan/level3', 1, '合格'
) ON CONFLICT (id) DO UPDATE SET
  valid = true,
  sort = EXCLUDED.sort,
  code = EXCLUDED.code,
  disp_value = EXCLUDED.disp_value,
  standard_grade = EXCLUDED.standard_grade,
  std_grade_name = EXCLUDED.std_grade_name,
  std_id = EXCLUDED.std_id,
  std_ver_com_id = EXCLUDED.std_ver_com_id,
  sampling_plan = EXCLUDED.sampling_plan,
  valuen = EXCLUDED.valuen,
  result_value = EXCLUDED.result_value,
  modify_time = now();

INSERT INTO public.limsba_analy_prod_stds (
  id, version, valid, cid, create_staff_id, create_time, table_info_id,
  code, memo_field, product_id, std_id, available_std
) VALUES (
  ${ids.analyProdStd}, 0, true, 1000, 1, now(), ${ids.analyProdStd},
  ${sqlLiteral(`${marker}_ANALY_PROD_STD`)}, ${sqlLiteral(marker)}, ${ids.material}, ${ids.qualityStd}, true
) ON CONFLICT (id) DO UPDATE SET
  valid = true,
  product_id = EXCLUDED.product_id,
  std_id = EXCLUDED.std_id,
  available_std = EXCLUDED.available_std,
  modify_time = now();

INSERT INTO public.rm_formulas (
  id, ${commonCols}, status, table_no, table_info_id, formual_code, formula_name,
  set_process, product_id, quality_std_id
) VALUES (
  ${ids.formula}, ${commonVals}, 99, ${sqlLiteral(`${formulaCode}_TN`)}, ${ids.formula}, ${sqlLiteral(formulaCode)}, ${sqlLiteral(`${marker} formula`)},
  'RM_formulaType/simpleFormula', ${ids.material}, ${ids.qualityStd}
) ON CONFLICT (id) DO UPDATE SET
  valid = true,
  status = 99,
  formual_code = EXCLUDED.formual_code,
  formula_name = EXCLUDED.formula_name,
  set_process = EXCLUDED.set_process,
  product_id = EXCLUDED.product_id,
  quality_std_id = EXCLUDED.quality_std_id,
  modify_time = now();

INSERT INTO public.hm_factory_models (
  id, version, valid, cid, create_staff_id, create_time, create_department_id,
  create_position_id, group_id, owner_staff_id, owner_department_id,
  owner_position_id, position_lay_rec, code, name, table_no, table_info_id,
  working_type
) VALUES (
  ${ids.workUnit}, 0, true, 1000, 1, now(), 1,
  1, 1000, 1, 1,
  1, '1', ${sqlLiteral(workUnitCode)}, ${sqlLiteral(`${marker} work unit`)}, ${sqlLiteral(`${marker}_WU_TN`)}, ${ids.workUnit},
  'HierarchicalMod_workingType/notOccupied'
) ON CONFLICT (id) DO UPDATE SET
  valid = true,
  code = EXCLUDED.code,
  name = EXCLUDED.name,
  working_type = EXCLUDED.working_type,
  modify_time = now();

INSERT INTO public.rm_formula_qualities (
  id, version, valid, cid, create_staff_id, create_time, table_info_id,
  formula_id, formula_active_id, unit_id, material_id, quality_std_id,
  quality_code, apply_check_staff_id, apply_check_dep_id, check_staff_id, check_dep_id,
  is_on_site_check, is_pass_check, final_inspection, remark
) VALUES (
  ${ids.formulaQuality}, 0, true, 1000, 1, now(), ${ids.formulaQuality},
  ${ids.formula}, NULL, ${ids.workUnit}, ${ids.material}, ${ids.qualityStd},
  ${sqlLiteral(`${marker}_QUALITY`)}, 1, 1, 1, 1,
  false, false, true, ${sqlClobLiteral(marker)}
) ON CONFLICT (id) DO UPDATE SET
  valid = true,
  formula_id = EXCLUDED.formula_id,
  formula_active_id = NULL,
  unit_id = EXCLUDED.unit_id,
  material_id = EXCLUDED.material_id,
  quality_std_id = EXCLUDED.quality_std_id,
  quality_code = EXCLUDED.quality_code,
  apply_check_staff_id = EXCLUDED.apply_check_staff_id,
  apply_check_dep_id = EXCLUDED.apply_check_dep_id,
  is_pass_check = EXCLUDED.is_pass_check,
  final_inspection = EXCLUDED.final_inspection,
  remark = EXCLUDED.remark,
  modify_time = now();

INSERT INTO public.baseset_batch_infos (
  id, ${commonCols}, status, table_no, batch_num,
  material_id, source_type, check_times, check_state, check_result, remark
) VALUES (
  ${ids.batchInfo}, ${commonVals}, 99, ${sqlLiteral(`${marker}_BATCH_INFO_TN`)}, ${sqlLiteral(batchNo)},
  ${ids.material}, NULL, 0, NULL, NULL, NULL
) ON CONFLICT (id) DO UPDATE SET
  valid = true,
  status = 99,
  batch_num = EXCLUDED.batch_num,
  material_id = EXCLUDED.material_id,
  source_type = NULL,
  check_times = 0,
  check_state = NULL,
  check_result = NULL,
  remark = NULL,
  modify_time = now();

INSERT INTO public.wom_produce_tasks (
  id, ${commonCols}, status, table_no, table_info_id, batch_contral, finish_num,
  formula_id, plan_num, plan_start_time, plan_end_time, produce_batch_num,
  product_id, quality_std_id, task_run_state, task_type, need_pack, is_analy,
  is_abnormal, is_prepared, advance_charge, act_start_time, work_area_id,
  check_times, check_state, check_result, inpect_deal_id, remark
) VALUES (
  ${ids.task}, ${commonVals}, 99, ${sqlLiteral(tableNo)}, ${ids.task}, false, 0,
  ${ids.formula}, 1, now() - interval '1 day', now() + interval '1 day', ${sqlLiteral(batchNo)},
  ${ids.material}, ${ids.qualityStd}, 'WOM_runState/runing', 'WOM_taskType/manufacture', false, false,
  false, false, false, now(), ${ids.workUnit},
  0, NULL, NULL, NULL, NULL
) ON CONFLICT (id) DO UPDATE SET
  valid = true,
  status = 99,
  formula_id = EXCLUDED.formula_id,
  product_id = EXCLUDED.product_id,
  quality_std_id = EXCLUDED.quality_std_id,
  produce_batch_num = EXCLUDED.produce_batch_num,
  task_run_state = EXCLUDED.task_run_state,
  work_area_id = EXCLUDED.work_area_id,
  check_times = 0,
  check_state = NULL,
  check_result = NULL,
  inpect_deal_id = NULL,
  remark = NULL,
  modify_time = now();

INSERT INTO public.wom_task_processes (
  id, ${commonCols}, status, table_no, table_info_id, task_id, formula_id,
  name, process_run_state, proc_sort, exe_order, equipment_id, work_unit_working_type,
  act_start_time, remark
) VALUES (
  ${ids.process}, ${commonVals}, 99, ${sqlLiteral(`${marker}_PROCESS_TN`)}, ${ids.process}, ${ids.task}, ${ids.formula},
  ${sqlLiteral(processName)}, 'WOM_runState/runing', '1', 1, ${ids.workUnit}, 'WOM_workUnitWorkingType/notRunning',
  now(), NULL
) ON CONFLICT (id) DO UPDATE SET
  valid = true,
  status = 99,
  process_run_state = EXCLUDED.process_run_state,
  equipment_id = EXCLUDED.equipment_id,
  act_start_time = EXCLUDED.act_start_time,
  act_end_time = NULL,
  modify_time = now();

INSERT INTO public.wom_task_actives (
  id, ${commonCols}, status, table_no, table_info_id, task_id, task_process_id,
  formula_id, formula_process_id, formula_quality, material_id, quality_std_id,
  name, active_type, run_state, property, exec_sort, hidden_sort, main_active,
  is_more_other, is_finish, is_run, is_agile, need_param_ana, material_batch_num,
  plan_quantity, standard_quantity, is_pass_check, final_inspection, check_times,
  check_state, inpect_deal_id, remark
) VALUES (
  ${ids.active}, ${commonVals}, 99, ${sqlLiteral(`${marker}_ACTIVE_TN`)}, ${ids.active}, ${ids.task}, ${ids.process},
  ${ids.formula}, NULL, ${ids.formulaQuality}, ${ids.material}, ${ids.qualityStd},
  ${sqlLiteral(activeName)}, 'RM_activeType/quality', 'WOM_runState/waitForRun', NULL, '1', 1, true,
  false, false, false, false, false, ${sqlLiteral(batchNo)},
  1, 1, false, true, 0,
  NULL, NULL, NULL
) ON CONFLICT (id) DO UPDATE SET
  valid = true,
  status = 99,
  active_type = EXCLUDED.active_type,
  run_state = 'WOM_runState/waitForRun',
  formula_quality = EXCLUDED.formula_quality,
  material_id = EXCLUDED.material_id,
  quality_std_id = EXCLUDED.quality_std_id,
  is_finish = false,
  is_run = false,
  is_pass_check = EXCLUDED.is_pass_check,
  final_inspection = EXCLUDED.final_inspection,
  check_times = 0,
  check_state = NULL,
  inpect_deal_id = NULL,
  act_start_time = NULL,
  act_end_time = NULL,
  modify_time = now();

INSERT INTO public.wom_produce_task_exelog (
  id, ${commonCols}, status, table_no, table_info_id, task_id, formula_id,
  product_id, quality_std_id, produce_batch_num, task_run_state, task_type,
  batch_contral, act_start_time, check_times, check_state, check_result,
  inpect_deal_id, remark
) VALUES (
  ${ids.taskExelog}, ${commonVals}, 99, ${sqlLiteral(`${marker}_TASK_EXELOG_TN`)}, ${ids.taskExelog}, ${ids.task}, ${ids.formula},
  ${ids.material}, ${ids.qualityStd}, ${sqlLiteral(batchNo)}, 'WOM_runState/runing', 'WOM_taskType/manufacture',
  false, now(), 0, NULL, NULL,
  NULL, NULL
) ON CONFLICT (id) DO UPDATE SET
  valid = true,
  task_id = EXCLUDED.task_id,
  formula_id = EXCLUDED.formula_id,
  product_id = EXCLUDED.product_id,
  quality_std_id = EXCLUDED.quality_std_id,
  produce_batch_num = EXCLUDED.produce_batch_num,
  task_run_state = EXCLUDED.task_run_state,
  check_times = 0,
  check_state = NULL,
  check_result = NULL,
  inpect_deal_id = NULL,
  modify_time = now();

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
  act_end_time = NULL,
  modify_time = now();

INSERT INTO public.wom_wait_put_records (
  id, ${commonCols}, status, table_no, table_info_id, task_id, task_process_id,
  task_active_id, formula_id, material_id, product_id, quality_std_id,
  produce_batch_num, product_code, product_name, material_code, material_name,
  formula_code, process_name, active_name, active_type, record_type, exe_state,
  batch_sync_status, check_times, check_state, check_result, is_pass_check,
  final_inspection, actual_start_time, actual_end_time, proc_report_id, acti_exelog, remark
) VALUES (
  ${ids.wait}, ${commonVals}, 99, ${sqlLiteral(`${marker}_ACTIVE_WAIT_TN`)}, ${ids.wait}, ${ids.task}, ${ids.process},
  ${ids.active}, ${ids.formula}, ${ids.material}, ${ids.material}, ${ids.qualityStd},
  ${sqlLiteral(batchNo)}, ${sqlLiteral(materialCode)}, ${sqlLiteral(`${marker} material`)}, ${sqlLiteral(materialCode)}, ${sqlLiteral(`${marker} material`)},
  ${sqlLiteral(formulaCode)}, ${sqlLiteral(processName)}, ${sqlLiteral(activeName)}, 'RM_activeType/quality', 'WOM_recordType/active', 'WOM_runState/waitForRun',
  'WOM_BatchSyncStatus/await', 0, NULL, NULL, false,
  true, NULL, NULL, NULL, NULL, NULL
) ON CONFLICT (id) DO UPDATE SET
  valid = true,
  status = 99,
  active_type = EXCLUDED.active_type,
  exe_state = EXCLUDED.exe_state,
  quality_std_id = EXCLUDED.quality_std_id,
  check_times = 0,
  check_state = NULL,
  check_result = NULL,
  is_pass_check = EXCLUDED.is_pass_check,
  final_inspection = EXCLUDED.final_inspection,
  actual_start_time = NULL,
  actual_end_time = NULL,
  proc_report_id = NULL,
  acti_exelog = NULL,
  modify_time = now();

COMMIT;

SELECT 'seed', ${sqlLiteral(marker)}, ${ids.task}, ${ids.process}, ${ids.active}, ${ids.wait}, ${ids.qualityStd}, ${ids.formulaQuality};
`;
}

function verificationSql() {
  return `
SELECT 'active', id, task_id, task_process_id, coalesce(active_type, ''), coalesce(run_state, ''),
       coalesce(is_finish::text, ''), coalesce(act_start_time::text, ''), coalesce(act_end_time::text, ''),
       coalesce(check_state, ''), coalesce(check_times::text, ''), coalesce(inpect_deal_id::text, ''),
       coalesce(quality_std_id::text, ''), coalesce(final_inspection::text, '')
FROM public.wom_task_actives
WHERE id = ${ids.active};

SELECT 'wait', id, task_id, task_process_id, task_active_id, coalesce(record_type, ''),
       coalesce(exe_state, ''), coalesce(actual_start_time::text, ''), coalesce(actual_end_time::text, ''),
       coalesce(proc_report_id::text, ''), coalesce(check_state, ''), coalesce(check_times::text, ''),
       coalesce(quality_std_id::text, '')
FROM public.wom_wait_put_records
WHERE task_active_id = ${ids.active}
ORDER BY id;

SELECT 'procReport', id, task_id, task_process_id, task_active_id, coalesce(proc_report_type, ''),
       coalesce(exe_system, ''), coalesce(is_finish::text, ''), coalesce(valid::text, '')
FROM public.wom_proc_reports
WHERE task_active_id = ${ids.active}
ORDER BY id;

SELECT 'activeExelog', id, task_id, task_process_id, task_active_id, coalesce(proc_report_id::text, ''),
       coalesce(active_type, ''), coalesce(run_state, ''), coalesce(check_state, ''),
       coalesce(check_times::text, ''), coalesce(inpect_deal_id::text, ''),
       coalesce(is_sync::text, ''), coalesce(need_sync::text, ''), coalesce(valid::text, '')
FROM public.wom_acti_exelogs
WHERE task_active_id = ${ids.active}
ORDER BY id;

SELECT 'inspect', id, coalesce(source_id::text, ''), coalesce(source_table_id::text, ''),
       coalesce(source_type, ''), coalesce(prod_id::text, ''), coalesce(batch_code, ''),
       coalesce(table_type_id::text, ''), coalesce(busi_type_id::text, ''),
       coalesce(apply_staff_id::text, ''), coalesce(apply_dept_id::text, ''),
       coalesce(need_lab::text, ''), coalesce(table_no, ''), coalesce(status::text, '')
FROM public.qcs_inspects
WHERE batch_code = ${sqlLiteral(batchNo)}
ORDER BY id;

SELECT 'inspectStdCount', coalesce(inspect_id::text, ''), count(*)::text
FROM public.qcs_inspect_stds
WHERE inspect_id IN (SELECT id FROM public.qcs_inspects WHERE batch_code = ${sqlLiteral(batchNo)})
GROUP BY inspect_id
ORDER BY inspect_id;

SELECT 'inspectComCount', coalesce(inspect_id::text, ''), count(*)::text
FROM public.qcs_inspect_coms
WHERE inspect_id IN (SELECT id FROM public.qcs_inspects WHERE batch_code = ${sqlLiteral(batchNo)})
GROUP BY inspect_id
ORDER BY inspect_id;

SELECT 'batch', id, batch_num, coalesce(material_id::text, ''), coalesce(check_state, ''),
       coalesce(check_result, ''), coalesce(check_times::text, '')
FROM public.baseset_batch_infos
WHERE batch_num = ${sqlLiteral(batchNo)}
ORDER BY id;

SELECT 'formulaQuality', id, coalesce(formula_id::text, ''), coalesce(unit_id::text, ''),
       coalesce(apply_check_staff_id::text, ''), coalesce(apply_check_dep_id::text, ''),
       coalesce(quality_std_id::text, ''), coalesce(final_inspection::text, '')
FROM public.rm_formula_qualities
WHERE id = ${ids.formulaQuality};

SELECT 'stdVersion', id, coalesce(std_id::text, ''), coalesce(active::text, ''), coalesce(valid::text, '')
FROM public.limsba_std_versions
WHERE id = ${ids.stdVersion};

SELECT 'stdVerReportCount', coalesce(std_ver_id::text, ''), count(*)::text
FROM public.limsba_std_ver_coms
WHERE std_ver_id = ${ids.stdVersion}
  AND valid = true
  AND is_report = true
GROUP BY std_ver_id;
`;
}

function assertPersistence(rawRows) {
  const rows = parseRows(rawRows);
  const active = rows.find((row) => row[0] === "active");
  const wait = rows.find((row) => row[0] === "wait");
  const procReport = rows.find((row) => row[0] === "procReport");
  const activeExelog = rows.find((row) => row[0] === "activeExelog");
  const inspect = rows.find((row) => row[0] === "inspect");
  const inspectStdCount = rows.find((row) => row[0] === "inspectStdCount");
  const inspectComCount = rows.find((row) => row[0] === "inspectComCount");
  const batch = rows.find((row) => row[0] === "batch");
  const formulaQuality = rows.find((row) => row[0] === "formulaQuality");
  const stdVersion = rows.find((row) => row[0] === "stdVersion");
  const stdVerReportCount = rows.find((row) => row[0] === "stdVerReportCount");
  const failures = [];

  if (!formulaQuality || formulaQuality[2] !== String(ids.formula) || formulaQuality[3] !== String(ids.workUnit) || formulaQuality[4] !== "1" || formulaQuality[5] !== "1" || formulaQuality[7] !== "true") {
    failures.push(`rm_formula_qualities prerequisite invalid: ${JSON.stringify(formulaQuality)}`);
  }
  if (!stdVersion || stdVersion[2] !== String(ids.qualityStd) || stdVersion[3] !== "true" || stdVersion[4] !== "true") {
    failures.push(`limsba_std_versions prerequisite invalid: ${JSON.stringify(stdVersion)}`);
  }
  if (!stdVerReportCount || Number(stdVerReportCount[2] || 0) < 1) {
    failures.push(`limsba_std_ver_coms report item count is zero: ${JSON.stringify(stdVerReportCount)}`);
  }
  if (!active || active[4] !== "RM_activeType/quality" || active[6] !== "true" || !active[7] || !active[8]) {
    failures.push(`wom_task_actives did not finish as expected: ${JSON.stringify(active)}`);
  }
  if (active && (!isWaitForCheckState(active[9]) || Number(active[10] || 0) < 1 || !active[11] || active[12] !== String(ids.qualityStd) || active[13] !== "true")) {
    failures.push(`wom_task_actives inspection fields invalid: ${JSON.stringify(active)}`);
  }
  if (!wait || wait[5] !== "WOM_recordType/active" || !isFinishedState(wait[6]) || !wait[7] || !wait[8] || !wait[9]) {
    failures.push(`wom_wait_put_records did not finish as expected: ${JSON.stringify(wait)}`);
  }
  if (wait && (!isWaitForCheckState(wait[10]) || Number(wait[11] || 0) < 1 || wait[12] !== String(ids.qualityStd))) {
    failures.push(`wom_wait_put_records inspection fields invalid: ${JSON.stringify(wait)}`);
  }
  if (!procReport || procReport[5] !== "WOM_procReportType/taskActive" || procReport[6] !== "RM_exeSystem/mes" || procReport[7] !== "true" || procReport[8] !== "true") {
    failures.push(`wom_proc_reports invalid: ${JSON.stringify(procReport)}`);
  }
  if (!activeExelog || activeExelog[6] !== "RM_activeType/quality" || !isFinishedState(activeExelog[7]) || !isWaitForCheckState(activeExelog[8]) || Number(activeExelog[9] || 0) < 1 || !activeExelog[10] || activeExelog[11] !== "true" || activeExelog[12] !== "false" || activeExelog[13] !== "true") {
    failures.push(`wom_acti_exelogs invalid: ${JSON.stringify(activeExelog)}`);
  }
  if (!inspect || inspect[4] !== "QCS_sourceType/manuComplete" || inspect[5] !== String(ids.material) || inspect[6] !== batchNo || inspect[7] !== "1" || inspect[8] !== "1" || inspect[9] !== "1" || inspect[10] !== "1" || inspect[11] !== "false") {
    failures.push(`qcs_inspects invalid: ${JSON.stringify(inspect)}`);
  }
  if (inspect && activeExelog && inspect[3] !== activeExelog[1]) {
    failures.push(`qcs_inspects.source_table_id does not point to actiExelog: inspect=${JSON.stringify(inspect)}, activeExelog=${JSON.stringify(activeExelog)}`);
  }
  if (inspect && active && active[11] !== inspect[1]) {
    failures.push(`wom_task_actives.inpect_deal_id does not match qcs_inspects.id: active=${active[11]}, inspect=${inspect[1]}`);
  }
  if (inspect && activeExelog && activeExelog[10] !== inspect[1]) {
    failures.push(`wom_acti_exelogs.inpect_deal_id does not match qcs_inspects.id: activeExelog=${activeExelog[10]}, inspect=${inspect[1]}`);
  }
  if (!inspectStdCount || Number(inspectStdCount[2] || 0) < 1) {
    failures.push(`qcs_inspect_stds marker rows not inserted: ${JSON.stringify(inspectStdCount)}`);
  }
  if (!batch || !isWaitForCheckState(batch[4]) || batch[5]) {
    failures.push(`baseset_batch_infos inspection fields invalid: ${JSON.stringify(batch)}`);
  }

  if (failures.length) {
    throw new Error(failures.join("; "));
  }

  return {
    rows,
    active,
    wait,
    procReport,
    activeExelog,
    inspect,
    inspectStdCount,
    inspectComCount: inspectComCount || null,
    batch,
    formulaQuality,
    stdVersion,
    stdVerReportCount,
  };
}

function waitForResponseSafe(page, predicate, timeout = pageTimeoutMs) {
  return page
    .waitForResponse(predicate, { timeout })
    .then((response) => ({ response }))
    .catch((error) => ({ error }));
}

async function parseResponse(response) {
  const text = await response.text();
  let json = null;
  try {
    json = JSON.parse(text);
  } catch (_error) {
    json = null;
  }
  return {
    method: response.request().method(),
    url: response.url(),
    status: response.status(),
    body: text.slice(0, 8000),
    json,
  };
}

async function safeScreenshot(page, evidence, pathValue) {
  try {
    await page.screenshot({ path: pathValue, fullPage: true, timeout: 15000 });
  } catch (error) {
    evidence.screenshotErrors.push({ path: pathValue, error: error.message });
  }
}

async function refreshGrid(page, gridCode, url, waitPattern) {
  const responsePromise = waitForResponseSafe(page, (response) => response.url().includes(waitPattern));
  const refreshResult = await page.evaluate(
    ({ code, requestUrl }) => {
      const gridFactory = window.ReactAPI && window.ReactAPI.getComponentAPI("SupDataGrid");
      const grid = gridFactory && typeof gridFactory.APIs === "function" && gridFactory.APIs(code);
      if (!grid || typeof grid.refreshDataByRequst !== "function") {
        return { ok: false, error: "grid refreshDataByRequst missing" };
      }
      grid.refreshDataByRequst({ type: "post", url: requestUrl, param: { pageSize: 65535 } });
      return { ok: true };
    },
    { code: gridCode, requestUrl: url }
  );
  if (!refreshResult.ok) {
    throw new Error(`Failed to refresh ${gridCode}: ${JSON.stringify(refreshResult)}`);
  }
  const { response, error } = await responsePromise;
  if (error) {
    throw new Error(`Grid refresh response not observed for ${gridCode}: ${error.message}`);
  }
  return { status: response.status(), url: response.url() };
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
    { timeout: pageTimeoutMs }
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
  return selection;
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
      if (/makeTaskBatchView|produceTask\/produceTask\/data|taskActive\/queryByProcess|checkoutBill\/generate|layoutJson|systemCodeJson/.test(url)) {
        evidence.requests.push({
          method: requestItem.method(),
          url,
          postData: requestItem.postData(),
        });
      }
    });
    page.on("response", async (response) => {
      const url = response.url();
      if (!/makeTaskBatchView|produceTask\/produceTask\/data|taskActive\/queryByProcess|checkoutBill\/generate|layoutJson|systemCodeJson/.test(url)) {
        return;
      }
      evidence.responses.push(await parseResponse(response));
    });

    const nav = await page.goto(route, { waitUntil: navigationWaitUntil, timeout: pageTimeoutMs });
    evidence.navigation = { route, status: nav && nav.status() };
    await page.waitForFunction(
      (taskId) => {
        const data = window.ReactAPI && window.ReactAPI.getFormData && window.ReactAPI.getFormData();
        return data && String(data.id) === String(taskId);
      },
      String(ids.task),
      { timeout: pageTimeoutMs }
    );
    evidence.screenshots.before = path.join(outputDir, "wom-checkoutbill-before.png");
    await safeScreenshot(page, evidence, evidence.screenshots.before);

    evidence.formData = await page.evaluate(() => {
      const data = window.ReactAPI && window.ReactAPI.getFormData && window.ReactAPI.getFormData();
      return data
        ? {
            id: data.id,
            tableNo: data.tableNo,
            taskRunState: data.taskRunState && data.taskRunState.id,
            batchContral: data.batchContral,
          }
        : null;
    });
    if (!evidence.formData || String(evidence.formData.id) !== String(ids.task)) {
      throw new Error(`WOM route did not load marker task form data: ${JSON.stringify(evidence.formData)}`);
    }

    evidence.processGridRefresh = await refreshGrid(
      page,
      processGridId,
      `/msService/WOM/produceTask/produceTask/data-dg1576028988483?datagridCode=${processGridId}&id=${ids.task}`,
      `/WOM/produceTask/produceTask/data-dg1576028988483`
    );
    evidence.processSelection = await selectGridRow(page, processGridId, String(ids.process), processName);
    evidence.activeGridRefresh = await refreshGrid(
      page,
      activeGridId,
      `/msService/WOM/produceTask/taskActive/queryByProcess?processId=${ids.process}&showBatch=false`,
      `/WOM/produceTask/taskActive/queryByProcess?processId=${ids.process}`
    );
    evidence.activeSelection = await selectGridRow(page, activeGridId, String(ids.active), activeName);

    const checkoutResponsePromise = waitForResponseSafe(
      page,
      (response) => response.url().includes(`/WOM/produceTask/produceTask/checkoutBill/generate/${ids.active}`),
      60000
    );
    evidence.checkoutBill = await page.evaluate(async (apiPath) => {
      const response = await fetch(apiPath, {
        method: "GET",
        headers: { Accept: "application/json, text/plain, */*" },
        credentials: "include",
      });
      const text = await response.text();
      let json = null;
      try {
        json = JSON.parse(text);
      } catch (_error) {
        json = null;
      }
      return {
        method: "GET",
        url: apiPath,
        status: response.status,
        body: text.slice(0, 8000),
        json,
      };
    }, checkoutBillApi);

    const { response, error } = await checkoutResponsePromise;
    if (error) {
      evidence.checkoutBillWaitError = error.message;
    } else {
      evidence.checkoutBillObservedResponse = await parseResponse(response);
    }
    const payload = evidence.checkoutBill.json && (evidence.checkoutBill.json.data || evidence.checkoutBill.json);
    const checkoutOk =
      evidence.checkoutBill.status === 200 &&
      (payload && (payload.success === true || payload.code === 200));
    if (!checkoutOk) {
      throw new Error(`checkoutBill/generate did not pass: ${JSON.stringify(evidence.checkoutBill)}`);
    }

    await page.waitForTimeout(1500);
    evidence.screenshots.after = path.join(outputDir, "wom-checkoutbill-after.png");
    await safeScreenshot(page, evidence, evidence.screenshots.after);

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
    navigationWaitUntil,
    marker,
    ids: Object.fromEntries(Object.entries(ids).map(([key, value]) => [key, value.toString()])),
    route,
    checkoutBillApi,
    tableNo,
    materialCode,
    formulaCode,
    qualityStdCode,
    batchNo,
    operation: "WOM checkoutBill quality active inspection generation",
    backendEntry:
      "WOMProduceTaskController.generateCheckoutBill -> WOMProduceTaskServiceImpl.generateCheckoutBill -> WOMProcReportServiceImpl.getByEasyActive -> WOMQCSServiceImpl.createInspect -> QCSInspectServiceImpl.createInspect -> WOMProduceTaskServiceImpl.endEasyActive",
    targetTables: [
      "wom_task_actives",
      "wom_wait_put_records",
      "wom_proc_reports",
      "wom_acti_exelogs",
      "qcs_inspects",
      "qcs_inspect_stds",
      "qcs_inspect_coms",
      "baseset_batch_infos",
      "rm_formula_qualities",
      "limsba_quality_stds",
      "limsba_std_versions",
      "limsba_std_ver_coms",
    ],
    console: [],
    pageErrors: [],
    requestFailures: [],
    requests: [],
    responses: [],
    screenshots: {},
    screenshotErrors: [],
  };

  try {
    evidence.seed = { sqlResult: runSql(seedSql()) };
    evidence.beforeSql = runSql(verificationSql());
    const api = await request.newContext({ ignoreHTTPSErrors: true });
    const loginResult = await login(api);
    await api.dispose();
    evidence.login = { status: loginResult.status, ticket: Boolean(loginResult.ticket) };

    await runBrowser(loginResult.ticket, evidence);
    assertFrontendClean(evidence);
    const verifyRaw = runSql(verificationSql());
    evidence.verificationSql = verificationSql().trim();
    evidence.persistence = assertPersistence(verifyRaw);
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
        api: evidence.checkoutBill,
        persistence: {
          active: evidence.persistence.active,
          wait: evidence.persistence.wait,
          procReport: evidence.persistence.procReport,
          activeExelog: evidence.persistence.activeExelog,
          inspect: evidence.persistence.inspect,
          inspectStdCount: evidence.persistence.inspectStdCount,
          inspectComCount: evidence.persistence.inspectComCount,
          batch: evidence.persistence.batch,
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
