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
const marker = process.env.ADP_E2E_MARKER || `ADP_E2E_${nowToken}_WOM_MANU_INSPECT`;
const outputDir =
  process.env.ADP_OUTPUT_DIR ||
  path.join("/tmp", `adp-wom-manu-inspect-persistence-${nowToken}`);
const outputPath =
  process.env.ADP_WOM_MANU_INSPECT_PERSISTENCE_OUTPUT ||
  path.join(outputDir, "wom-manu-inspect-persistence-results.json");
// Keep marker rows above older WOM seed data but below Number.MAX_SAFE_INTEGER for legacy frontend JSON handling.
const idBase = 8990000000000000n + BigInt(Date.now() % 1000000000) * 100n + BigInt(process.pid % 100);
const ids = {
  material: idBase + 1n,
  formula: idBase + 2n,
  qualityStd: idBase + 3n,
  stdVersion: idBase + 4n,
  analyProdStd: idBase + 5n,
  formulaQuality: idBase + 6n,
  batchInfo: idBase + 7n,
  task: idBase + 8n,
  wait: idBase + 9n,
  taskExelog: idBase + 10n,
  pending: idBase + 11n,
  workUnit: idBase + 12n,
  testComponent: idBase + 13n,
  stdVerCom: idBase + 14n,
  stdVerGradeUnqualified: idBase + 15n,
  stdVerGradeQualified: idBase + 16n,
  specLimit: idBase + 17n,
};
const tableNo = `${marker}_TASK_TN`;
const materialCode = `${marker}_MAT`;
const formulaCode = `${marker}_FORM`;
const qualityStdCode = `${marker}_STD`;
const batchNo = `${marker}_BATCH`;
const workUnitCode = `${marker}_WU`;
const workUnitName = `${marker} work unit`;
const qcsReportItemCode = `${marker}_QCS_REPORT_ITEM`;
const qcsReportItemName = `${marker} QCS report item`;
const route = "/msService/WOM/produceTask/produceTask/makeTaskList";
const qcsRoute = "/msService/QCS/inspect/inspect/manuInspectList";
const gridId = "WOM_1.0.0_produceTask_makeTaskList_produceTask_sdg";
const createInspectApi = "/msService/WOM/produceTask/produceTask/createManuInspect";
const findCheckMsgApi = "/msService/WOM/produceTask/produceTask/findCheckMsgByBatch";

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

async function readJsonSafe(response) {
  const text = await response.text();
  try {
    return { json: JSON.parse(text), text };
  } catch (_error) {
    return { json: null, text };
  }
}

async function safeScreenshot(page, evidence, pathValue) {
  try {
    await page.screenshot({ path: pathValue, fullPage: true, timeout: 15000 });
  } catch (error) {
    evidence.screenshotErrors.push({ path: pathValue, error: error.message });
  }
}

function waitForResponseSafe(page, predicate, timeout = pageTimeoutMs) {
  return page
    .waitForResponse(predicate, { timeout })
    .then((response) => ({ response }))
    .catch((error) => ({ error }));
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
  1, '1', ${sqlLiteral(workUnitCode)}, ${sqlLiteral(workUnitName)}, ${sqlLiteral(`${marker}_WU_TN`)}, ${ids.workUnit},
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
  false, true, true, ${sqlLiteral(marker)}
) ON CONFLICT (id) DO UPDATE SET
  valid = true,
  formula_id = EXCLUDED.formula_id,
  formula_active_id = NULL,
  unit_id = EXCLUDED.unit_id,
  material_id = EXCLUDED.material_id,
  quality_std_id = EXCLUDED.quality_std_id,
  apply_check_staff_id = EXCLUDED.apply_check_staff_id,
  apply_check_dep_id = EXCLUDED.apply_check_dep_id,
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
  check_times = 0,
  check_state = NULL,
  check_result = NULL,
  inpect_deal_id = NULL,
  remark = NULL,
  modify_time = now();

INSERT INTO public.wom_wait_put_records (
  id, ${commonCols}, status, table_no, table_info_id, formula_id, plan_num,
  plan_start_time, plan_end_time, produce_batch_num, product_id, record_type,
  exe_state, batch_sync_status, task_id, material_id, material_code, material_name,
  product_code, product_name, formula_code, quality_std_id, check_times,
  check_state, check_result, remark
) VALUES (
  ${ids.wait}, ${commonVals}, 99, ${sqlLiteral(`${marker}_WAIT_TN`)}, ${ids.wait}, ${ids.formula}, 1,
  now() - interval '1 day', now() + interval '1 day', ${sqlLiteral(batchNo)}, ${ids.material}, 'WOM_recordType/workOrder',
  'WOM_runState/runing', 'WOM_BatchSyncStatus/done', ${ids.task}, ${ids.material}, ${sqlLiteral(materialCode)}, ${sqlLiteral(`${marker} material`)},
  ${sqlLiteral(materialCode)}, ${sqlLiteral(`${marker} material`)}, ${sqlLiteral(formulaCode)}, ${ids.qualityStd}, 0,
  NULL, NULL, ${sqlClobLiteral(marker)}
) ON CONFLICT (id) DO UPDATE SET
  valid = true,
  status = 99,
  task_id = EXCLUDED.task_id,
  product_id = EXCLUDED.product_id,
  material_id = EXCLUDED.material_id,
  quality_std_id = EXCLUDED.quality_std_id,
  produce_batch_num = EXCLUDED.produce_batch_num,
  exe_state = EXCLUDED.exe_state,
  check_times = 0,
  check_state = NULL,
  check_result = NULL,
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
  NULL, ${sqlClobLiteral(marker)}
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

INSERT INTO public.wfm_task_pending (
  id, user_id, app_id, task_description, activity_type, activity_name, task_status,
  open_url, process_key, process_version, process_description, process_id,
  table_info_id, entity_code, table_no, deployment_id, cid, start_time,
  create_time, creator, version, row_version
) VALUES (
  ${ids.pending}, 1, 'WOM_1.0.0', '生产请检验收', '1', 'start_manuInspect', 1,
  '/msService/WOM/produceTask/produceTask/makeTaskEdit', 'makeTaskFlow', 1, '生产请检验收',
  ${sqlLiteral(`ADP_E2E_MANU_INSPECT_${marker}`)}, ${ids.task}, 'WOM_1.0.0_produceTask',
  ${sqlLiteral(tableNo)}, 1, 1000, now() - interval '1 day', now(), 'admin', 0, 0
) ON CONFLICT (id) DO NOTHING;

COMMIT;

SELECT 'seed', ${sqlLiteral(marker)}, ${ids.task}, ${ids.qualityStd}, ${ids.stdVersion}, ${ids.formulaQuality}, ${ids.batchInfo};
`;
}

function verificationSql() {
  return `
SELECT 'task', id, table_no, coalesce(quality_std_id::text, ''), coalesce(check_state, ''), coalesce(check_times::text, ''), coalesce(inpect_deal_id::text, ''), coalesce(check_result, '')
FROM public.wom_produce_tasks
WHERE id = ${ids.task};

SELECT 'wait', id, task_id, coalesce(quality_std_id::text, ''), coalesce(check_state, ''), coalesce(check_times::text, ''), coalesce(check_result, '')
FROM public.wom_wait_put_records
WHERE task_id = ${ids.task}
ORDER BY id;

SELECT 'exelog', id, task_id, coalesce(quality_std_id::text, ''), coalesce(check_state, ''), coalesce(check_times::text, ''), coalesce(inpect_deal_id::text, ''), coalesce(check_result, '')
FROM public.wom_produce_task_exelog
WHERE task_id = ${ids.task}
ORDER BY id;

SELECT 'batch', id, batch_num, coalesce(material_id::text, ''), coalesce(check_state, ''), coalesce(check_result, ''), coalesce(check_times::text, '')
FROM public.baseset_batch_infos
WHERE batch_num = ${sqlLiteral(batchNo)}
ORDER BY id;

SELECT 'inspect', id, coalesce(source_id::text, ''), coalesce(source_table_id::text, ''), coalesce(source_type, ''),
       coalesce(prod_id::text, ''), coalesce(batch_code, ''), coalesce(table_type_id::text, ''),
       coalesce(busi_type_id::text, ''), coalesce(apply_staff_id::text, ''),
       coalesce(apply_dept_id::text, ''), coalesce(need_lab::text, ''), coalesce(table_no, ''),
       coalesce(status::text, '')
FROM public.qcs_inspects
WHERE source_table_id = ${ids.task}
   OR batch_code = ${sqlLiteral(batchNo)}
ORDER BY id;

SELECT 'inspectStdCount', coalesce(inspect_id::text, ''), count(*)::text
FROM public.qcs_inspect_stds
WHERE inspect_id IN (
  SELECT id FROM public.qcs_inspects WHERE source_table_id = ${ids.task} OR batch_code = ${sqlLiteral(batchNo)}
)
GROUP BY inspect_id
ORDER BY inspect_id;

SELECT 'inspectComCount', coalesce(inspect_id::text, ''), count(*)::text
FROM public.qcs_inspect_coms
WHERE inspect_id IN (
  SELECT id FROM public.qcs_inspects WHERE source_table_id = ${ids.task} OR batch_code = ${sqlLiteral(batchNo)}
)
GROUP BY inspect_id
ORDER BY inspect_id;

SELECT 'formulaQuality', id, coalesce(formula_id::text, ''), coalesce(unit_id::text, ''), coalesce(apply_check_staff_id::text, ''), coalesce(apply_check_dep_id::text, ''), coalesce(quality_std_id::text, '')
FROM public.rm_formula_qualities
WHERE id = ${ids.formulaQuality};

SELECT 'stdVersion', id, coalesce(std_id::text, ''), coalesce(active::text, ''), coalesce(valid::text, '')
FROM public.limsba_std_versions
WHERE id = ${ids.stdVersion};

SELECT 'testComponent', id, code, coalesce(name, ''), coalesce(report_name, ''), coalesce(is_report::text, ''), coalesce(valid::text, '')
FROM public.limsba_test_components
WHERE id = ${ids.testComponent};

SELECT 'stdVerCom', id, coalesce(std_id::text, ''), coalesce(std_ver_id::text, ''), coalesce(com_id::text, ''), coalesce(is_report::text, ''), coalesce(report_name, ''), coalesce(valid::text, '')
FROM public.limsba_std_ver_coms
WHERE id = ${ids.stdVerCom};

SELECT 'stdVerReportCount', coalesce(std_ver_id::text, ''), count(*)::text
FROM public.limsba_std_ver_coms
WHERE std_ver_id = ${ids.stdVersion}
  AND valid = true
  AND is_report = true
GROUP BY std_ver_id;

SELECT 'stdVerGradeCount', coalesce(std_ver_id::text, ''), count(*)::text,
       coalesce(string_agg(coalesce(std_grade, '') || ':' || coalesce(name, ''), ',' ORDER BY sort DESC, id), '')
FROM public.limsba_std_ver_grades
WHERE std_ver_id = ${ids.stdVersion}
  AND valid = true
GROUP BY std_ver_id;

SELECT 'specLimit', id, coalesce(std_ver_com_id::text, ''), coalesce(standard_grade, ''), coalesce(disp_value, ''), coalesce(valid::text, '')
FROM public.limsba_spec_limits
WHERE id = ${ids.specLimit};

SELECT 'analyProdStd', id, coalesce(product_id::text, ''), coalesce(std_id::text, ''), coalesce(available_std::text, ''), coalesce(valid::text, '')
FROM public.limsba_analy_prod_stds
WHERE id = ${ids.analyProdStd};

SELECT 'wfCustom', id, code, coalesce(deployment_key, ''), coalesce(flow_config_type, ''), coalesce(valid::text, '')
FROM public.baseset_wf_custom_configs
WHERE code = 'manuInspect';

SELECT 'wfCustomReport', id, code, coalesce(deployment_key, ''), coalesce(flow_config_type, ''), coalesce(valid::text, '')
FROM public.baseset_wf_custom_configs
WHERE code = 'manuReport';

SELECT 'wfDeployment', id, process_key, coalesce(is_current_version::text, ''), coalesce(valid::text, '')
FROM public.wf_deployment
WHERE process_key = 'manuInspectWorkFlow'
ORDER BY id;

SELECT 'wfDeploymentReport', id, process_key, coalesce(is_current_version::text, ''), coalesce(valid::text, '')
FROM public.wf_deployment
WHERE process_key = 'manuInspectReportWorkFlow'
ORDER BY id;
`;
}

function assertPersistence(rawRows) {
  const rows = parseRows(rawRows);
  const task = rows.find((row) => row[0] === "task");
  const wait = rows.find((row) => row[0] === "wait");
  const exelog = rows.find((row) => row[0] === "exelog");
  const batch = rows.find((row) => row[0] === "batch");
  const inspect = rows.find((row) => row[0] === "inspect");
  const inspectStdCount = rows.find((row) => row[0] === "inspectStdCount");
  const inspectComCount = rows.find((row) => row[0] === "inspectComCount");
  const formulaQuality = rows.find((row) => row[0] === "formulaQuality");
  const stdVersion = rows.find((row) => row[0] === "stdVersion");
  const testComponent = rows.find((row) => row[0] === "testComponent");
  const stdVerCom = rows.find((row) => row[0] === "stdVerCom");
  const stdVerReportCount = rows.find((row) => row[0] === "stdVerReportCount");
  const stdVerGradeCount = rows.find((row) => row[0] === "stdVerGradeCount");
  const specLimit = rows.find((row) => row[0] === "specLimit");
  const analyProdStd = rows.find((row) => row[0] === "analyProdStd");
  const wfCustom = rows.find((row) => row[0] === "wfCustom");
  const wfCustomReport = rows.find((row) => row[0] === "wfCustomReport");
  const wfDeployment = rows.find((row) => row[0] === "wfDeployment");
  const wfDeploymentReport = rows.find((row) => row[0] === "wfDeploymentReport");
  const failures = [];

  if (!wfCustom || wfCustom[2] !== "manuInspect" || wfCustom[3] !== "manuInspectWorkFlow" || wfCustom[5] !== "true") {
    failures.push(`baseset_wf_custom_configs manuInspect missing/invalid: ${JSON.stringify(wfCustom)}`);
  }
  if (!wfDeployment || wfDeployment[3] !== "1") {
    failures.push(`wf_deployment manuInspectWorkFlow is not current: ${JSON.stringify(wfDeployment)}`);
  }
  if (!wfCustomReport || wfCustomReport[2] !== "manuReport" || wfCustomReport[3] !== "manuInspectReportWorkFlow" || wfCustomReport[5] !== "true") {
    failures.push(`baseset_wf_custom_configs manuReport missing/invalid: ${JSON.stringify(wfCustomReport)}`);
  }
  if (!wfDeploymentReport || wfDeploymentReport[3] !== "1") {
    failures.push(`wf_deployment manuInspectReportWorkFlow is not current: ${JSON.stringify(wfDeploymentReport)}`);
  }
  if (!formulaQuality || formulaQuality[2] !== String(ids.formula) || formulaQuality[3] !== String(ids.workUnit) || formulaQuality[4] !== "1" || formulaQuality[5] !== "1") {
    failures.push(`rm_formula_qualities prerequisite missing/invalid: ${JSON.stringify(formulaQuality)}`);
  }
  if (!stdVersion || stdVersion[2] !== String(ids.qualityStd) || stdVersion[3] !== "true" || stdVersion[4] !== "true") {
    failures.push(`limsba_std_versions prerequisite missing/invalid: ${JSON.stringify(stdVersion)}`);
  }
  if (!testComponent || testComponent[2] !== qcsReportItemCode || testComponent[4] !== qcsReportItemName || testComponent[5] !== "true" || testComponent[6] !== "true") {
    failures.push(`limsba_test_components report prerequisite missing/invalid: ${JSON.stringify(testComponent)}`);
  }
  if (!stdVerCom || stdVerCom[2] !== String(ids.qualityStd) || stdVerCom[3] !== String(ids.stdVersion) || stdVerCom[4] !== String(ids.testComponent) || stdVerCom[5] !== "true" || stdVerCom[7] !== "true") {
    failures.push(`limsba_std_ver_coms report prerequisite missing/invalid: ${JSON.stringify(stdVerCom)}`);
  }
  if (!stdVerReportCount || Number(stdVerReportCount[2] || 0) < 1) {
    failures.push(`limsba_std_ver_coms report item count is zero: ${JSON.stringify(stdVerReportCount)}`);
  }
  if (!stdVerGradeCount || Number(stdVerGradeCount[2] || 0) < 2) {
    failures.push(`limsba_std_ver_grades report grade prerequisites missing: ${JSON.stringify(stdVerGradeCount)}`);
  }
  if (!specLimit || specLimit[2] !== String(ids.stdVerCom) || specLimit[3] !== "LIMSBasic_standardGrade/Qualified" || specLimit[5] !== "true") {
    failures.push(`limsba_spec_limits report prerequisite missing/invalid: ${JSON.stringify(specLimit)}`);
  }
  if (!analyProdStd || analyProdStd[2] !== String(ids.material) || analyProdStd[3] !== String(ids.qualityStd) || analyProdStd[4] !== "true" || analyProdStd[5] !== "true") {
    failures.push(`limsba_analy_prod_stds prerequisite missing/invalid: ${JSON.stringify(analyProdStd)}`);
  }
  if (!inspect) {
    failures.push("qcs_inspects marker row was not inserted");
  }
  if (inspect) {
    if (inspect[2] !== String(ids.task) && inspect[3] !== String(ids.task)) {
      failures.push(`qcs_inspects source does not point to WOM task: ${JSON.stringify(inspect)}`);
    }
    if (inspect[5] !== String(ids.material) || inspect[6] !== batchNo || inspect[7] !== "1" || inspect[8] !== "1") {
      failures.push(`qcs_inspects core fields not persisted as expected: ${JSON.stringify(inspect)}`);
    }
    if (inspect[9] !== "1" || inspect[10] !== "1" || inspect[11] !== "false") {
      failures.push(`qcs_inspects apply staff/dept or need_lab invalid: ${JSON.stringify(inspect)}`);
    }
  }
  if (!inspectStdCount || Number(inspectStdCount[2] || 0) < 1) {
    failures.push(`qcs_inspect_stds marker rows not inserted: ${JSON.stringify(inspectStdCount)}`);
  }
  if (!task || task[3] !== String(ids.qualityStd) || !isWaitForCheckState(task[4]) || Number(task[5] || 0) < 1 || !task[6]) {
    failures.push(`wom_produce_tasks inspection fields not persisted: ${JSON.stringify(task)}`);
  }
  if (!wait || wait[3] !== String(ids.qualityStd) || !isWaitForCheckState(wait[4]) || Number(wait[5] || 0) < 1) {
    failures.push(`wom_wait_put_records inspection fields not persisted: ${JSON.stringify(wait)}`);
  }
  if (!exelog || exelog[3] !== String(ids.qualityStd) || !isWaitForCheckState(exelog[4]) || Number(exelog[5] || 0) < 1 || !exelog[6]) {
    failures.push(`wom_produce_task_exelog inspection fields not persisted: ${JSON.stringify(exelog)}`);
  }
  if (task && exelog && task[6] && exelog[6] && task[6] !== exelog[6]) {
    failures.push(`WOM task and exelog inpect_deal_id mismatch: task=${task[6]}, exelog=${exelog[6]}`);
  }
  if (inspect && task && task[6] && task[6] !== inspect[1]) {
    failures.push(`WOM task inpect_deal_id does not match qcs_inspects.id: task=${task[6]}, inspect=${inspect[1]}`);
  }
  if (!batch || !isWaitForCheckState(batch[4]) || batch[5]) {
    failures.push(`baseset_batch_infos inspection fields not persisted: ${JSON.stringify(batch)}`);
  }

  if (failures.length) {
    throw new Error(failures.join("; "));
  }

  return {
    rows,
    task,
    wait,
    exelog,
    batch,
    inspect,
    inspectStdCount,
    inspectComCount: inspectComCount || null,
    formulaQuality,
    stdVersion,
    testComponent,
      stdVerCom,
      stdVerReportCount,
      stdVerGradeCount,
      specLimit,
      analyProdStd,
    wfCustom,
    wfCustomReport,
    wfDeployment,
    wfDeploymentReport,
  };
}

async function refreshMarkerGrid(page) {
  await page.waitForFunction(
    (gridCode) => {
      const gridFactory = window.ReactAPI && window.ReactAPI.getComponentAPI("SupDataGrid");
      const grid = gridFactory && typeof gridFactory.APIs === "function" && gridFactory.APIs(gridCode);
      return Boolean(grid && typeof grid.refreshDataByRequst === "function");
    },
    gridId,
    { timeout: pageTimeoutMs }
  );
  const responsePromise = waitForResponseSafe(
    page,
    (response) =>
      /\/WOM\/produceTask\/produceTask\/makeTaskList-(pending|query)/.test(response.url()) &&
      response.request().method() === "POST"
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

async function selectMarkerRow(page, evidence) {
  evidence.markerGridRefresh = await refreshMarkerGrid(page);
  const gridVisible = await page
    .waitForFunction(
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
      { timeout: Math.min(pageTimeoutMs, 15000) }
    )
    .then(() => true)
    .catch((error) => {
      evidence.markerGridVisibleTimeout = error.message;
      return false;
    });
  if (!gridVisible) {
    return page.evaluate(async ({ expectedMarker }) => {
      const payload = {
        classifyCodes: "",
        customCondition: {},
        permissionCode: "WOM_1.0.0_produceTask_makeTaskList",
        pageNo: 1,
        paging: true,
        pageSize: 65535,
        crossCompanyFlag: "true",
      };
      const response = await fetch("/msService/WOM/produceTask/produceTask/makeTaskList-pending", {
        method: "POST",
        headers: {
          Accept: "application/json, text/plain, */*",
          "Content-Type": "application/json;charset=UTF-8",
        },
        body: JSON.stringify(payload),
        credentials: "include",
      });
      const text = await response.text();
      let json = null;
      try {
        json = JSON.parse(text);
      } catch (_error) {
        json = null;
      }
      const rows = (json && json.data && Array.isArray(json.data.result) && json.data.result) || [];
      const row = rows.find(
        (item) =>
          String(item.tableNo || "").includes(expectedMarker) ||
          String(item.produceBatchNum || "").includes(expectedMarker)
      );
      if (!row) {
        return {
          ok: false,
          fallback: "makeTaskList-pending",
          status: response.status,
          rowCount: rows.length,
          sampleTableNos: rows.map((item) => item.tableNo).slice(0, 10),
          body: text.slice(0, 1000),
        };
      }
      return {
        ok: true,
        fallback: "makeTaskList-pending",
        status: response.status,
        rowCount: rows.length,
        selectedId: row.id,
        tableNo: row.tableNo,
        produceBatchNum: row.produceBatchNum,
        taskRunState: row.taskRunState && row.taskRunState.id,
        statusValue: row.status,
        qualityStdId: row.qualityStdId && row.qualityStdId.id,
        checkState: row.checkState,
        checkTimes: row.checkTimes,
        pending: row.pending && {
          id: row.pending.id,
          activityName: row.pending.activityName,
          taskDescription: row.pending.taskDescription,
        },
      };
    }, { expectedMarker: marker });
  }
  return page.evaluate(
    ({ gridCode, expectedMarker }) => {
      const gridFactory = window.ReactAPI && window.ReactAPI.getComponentAPI("SupDataGrid");
      const grid = gridFactory && typeof gridFactory.APIs === "function" && gridFactory.APIs(gridCode);
      if (!grid) {
        return { ok: false, error: "grid api missing" };
      }
      const rows =
        (typeof grid.getRows === "function" && grid.getRows()) ||
        (typeof grid.getDatagridData === "function" && grid.getDatagridData()) ||
        [];
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
        produceBatchNum: selected.produceBatchNum,
        taskRunState: selected.taskRunState && selected.taskRunState.id,
        status: selected.status,
        qualityStdId: selected.qualityStdId && selected.qualityStdId.id,
        checkState: selected.checkState,
        checkTimes: selected.checkTimes,
      };
    },
    { gridCode: gridId, expectedMarker: marker }
  );
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
      if (/makeTaskList|createManuInspect|findCheckMsgByBatch|manuInspectList|layoutJson|systemCodeJson/.test(url)) {
        evidence.requests.push({
          method: requestItem.method(),
          url,
          postData: requestItem.postData(),
        });
      }
    });
    page.on("response", async (response) => {
      const url = response.url();
      if (!/makeTaskList|createManuInspect|findCheckMsgByBatch|manuInspectList|layoutJson|systemCodeJson/.test(url)) {
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

    const nav = await page.goto(route, { waitUntil: navigationWaitUntil, timeout: pageTimeoutMs });
    evidence.navigation = { route, status: nav && nav.status() };
    evidence.beforeBodyHasMarker = (await page.locator("body").innerText({ timeout: pageTimeoutMs })).includes(marker);
    evidence.screenshots.before = path.join(outputDir, "wom-manu-inspect-before.png");
    await safeScreenshot(page, evidence, evidence.screenshots.before);

    const selection = await selectMarkerRow(page, evidence);
    evidence.selection = selection;
    if (!selection.ok) {
      throw new Error(`Failed to select marker row: ${JSON.stringify(selection)}`);
    }

    const findCheckResponse = await page.request.get(`${findCheckMsgApi}?taskId=${selection.selectedId}`, {
      headers: { Authorization: `Bearer ${ticket}` },
    });
    const findCheckParsed = await readJsonSafe(findCheckResponse);
    evidence.findCheckMsgByBatch = {
      method: "GET",
      url: `${findCheckMsgApi}?taskId=${selection.selectedId}`,
      status: findCheckResponse.status(),
      body: findCheckParsed.text.slice(0, 8000),
      json: findCheckParsed.json,
    };

    const createResponse = await page.evaluate(
      async ({ apiPath, taskId }) => {
        const body = new URLSearchParams({ taskId: String(taskId) });
        const response = await fetch(apiPath, {
          method: "POST",
          headers: {
            Accept: "application/json, text/plain, */*",
            "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
          },
          body,
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
          method: "POST",
          url: apiPath,
          requestPayload: body.toString(),
          status: response.status,
          body: text.slice(0, 8000),
          json,
        };
      },
      { apiPath: createInspectApi, taskId: selection.selectedId }
    );
    evidence.createManuInspect = createResponse;
    const payload = createResponse.json && (createResponse.json.data || createResponse.json);
    if (createResponse.status !== 200 || !payload || payload.success !== true) {
      throw new Error(`createManuInspect did not pass: ${JSON.stringify(createResponse)}`);
    }

    await page.waitForTimeout(1500);
    evidence.screenshots.afterCreate = path.join(outputDir, "wom-manu-inspect-after-create.png");
    await safeScreenshot(page, evidence, evidence.screenshots.afterCreate);

    const qcsNav = await page.goto(`${qcsRoute}?ADP_E2E=${encodeURIComponent(marker)}`, {
      waitUntil: navigationWaitUntil,
      timeout: pageTimeoutMs,
    });
    evidence.qcsNavigation = { route: qcsRoute, status: qcsNav && qcsNav.status() };
    await page.waitForTimeout(1500);
    evidence.qcsBodyHasMarker = (await page.locator("body").innerText({ timeout: pageTimeoutMs })).includes(batchNo);
    evidence.screenshots.qcsList = path.join(outputDir, "qcs-manu-inspect-list-after-create.png");
    await safeScreenshot(page, evidence, evidence.screenshots.qcsList);

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
    qcsRoute,
    tableNo,
    materialCode,
    formulaCode,
    qualityStdCode,
    batchNo,
    operation: "WOM makeTaskList manufacturing inspection creation",
    api: createInspectApi,
    backendEntry:
      "WOMProduceTaskController.createManuInspect -> WOMProduceTaskServiceImpl.changeTaskStateAndInitiateCheck -> WOMQCSServiceImpl.createInspect -> QCSInspectServiceImpl.createInspect",
    targetTables: [
      "wom_produce_tasks",
      "wom_wait_put_records",
      "wom_produce_task_exelog",
      "baseset_batch_infos",
      "qcs_inspects",
      "qcs_inspect_stds",
      "qcs_inspect_coms",
      "rm_formula_qualities",
      "limsba_quality_stds",
      "limsba_std_versions",
      "limsba_test_components",
      "limsba_std_ver_coms",
      "limsba_std_ver_grades",
      "limsba_spec_limits",
      "limsba_analy_prod_stds",
      "baseset_wf_custom_configs",
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
        api: evidence.createManuInspect,
        persistence: {
          task: evidence.persistence.task,
          wait: evidence.persistence.wait,
          exelog: evidence.persistence.exelog,
          batch: evidence.persistence.batch,
          inspect: evidence.persistence.inspect,
          inspectStdCount: evidence.persistence.inspectStdCount,
          inspectComCount: evidence.persistence.inspectComCount,
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
