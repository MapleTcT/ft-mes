#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { execFileSync, spawnSync } = require("child_process");
const { chromium, request } = require("playwright");
const {
  loadScenario,
  validateProductionModel,
} = require("../../../simulation/bpi/fructose-line-scenario");

const repoRoot = path.resolve(__dirname, "../../..");
const baseUrl = (process.env.ADP_BASE_URL || "http://10.11.100.17:18080").replace(/\/+$/, "");
const browserBaseUrl = (process.env.ADP_BROWSER_BASE_URL || baseUrl).replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD || "123456";
const sshTarget = process.env.ADP_DB_SSH_TARGET || "v6@10.11.100.17";
const dbContainer = process.env.ADP_DB_CONTAINER || "adp-mes-newbase-postgres-1";
const dbName = process.env.ADP_DB_NAME || "adp";
const bpiDbName = process.env.BPI_DB_NAME || "ft_mes_bpi";
const dbUser = process.env.ADP_DB_USER || "adp";
const headless = process.env.ADP_HEADLESS !== "false";
const marker = process.env.ADP_E2E_MARKER || "ADP_E2E_FRUCTOSE_LINE_01";
const tenantId = process.env.ADP_E2E_TENANT_ID || "1000";
const legacyTenantId = `${marker}_TENANT`;
const scenarioPath = path.resolve(
  process.env.ADP_FRUCTOSE_SCENARIO
    || path.join(repoRoot, "simulation/bpi/scenarios/fructose-jet-saccharification-v1.json")
);
const fullFlowEvidencePath = path.resolve(
  process.env.ADP_FRUCTOSE_FULL_FLOW_OUTPUT || "/tmp/adp-fructose-line-full-flow-01.json"
);
const outputPath = path.resolve(
  process.env.ADP_FRUCTOSE_PILOT_OUTPUT || "/tmp/adp-fructose-line-pilot-acceptance.json"
);
const screenshotDir = path.resolve(
  process.env.ADP_FRUCTOSE_PILOT_SCREENSHOT_DIR
    || path.join(path.dirname(outputPath), "adp-fructose-line-pilot-screenshots")
);
const defaultChromePath = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";
const browserExecutable = process.env.ADP_BROWSER_EXECUTABLE
  || (fs.existsSync(defaultChromePath) ? defaultChromePath : "");
const bpiWomLineId = process.env.ADP_FRUCTOSE_BPI_WOM_LINE_ID || "9007190231280105";
const bpiScope = {
  tenantId: "1000",
  plantId: "PLANT-01",
  lineId: "LINE-S07-01",
  from: "2026-07-27T16:00:00+08:00",
  to: "2026-07-27T16:01:32+08:00",
};
const processWindows = {
  jet: {
    start: "2026-07-27 16:00:20",
    end: "2026-07-27 16:00:44",
  },
  saccharification: {
    start: "2026-07-27 16:00:56",
    end: "2026-07-27 16:01:32",
  },
};

function ensureDirectory(targetPath) {
  fs.mkdirSync(targetPath, { recursive: true });
}

function shellQuote(value) {
  return `'${String(value).replace(/'/g, "'\\''")}'`;
}

function sqlLiteral(value) {
  return `'${String(value).replace(/'/g, "''")}'`;
}

function runSql(sql, database = dbName) {
  const remoteCommand = [
    "docker",
    "exec",
    "-i",
    shellQuote(dbContainer),
    "psql",
    "-U",
    shellQuote(dbUser),
    "-d",
    shellQuote(database),
    "-v",
    "ON_ERROR_STOP=1",
    "-At",
  ].join(" ");
  return execFileSync(
    "ssh",
    [
      "-o",
      "BatchMode=yes",
      "-o",
      "ConnectTimeout=8",
      "-o",
      "StrictHostKeyChecking=no",
      sshTarget,
      remoteCommand,
    ],
    {
      input: sql,
      encoding: "utf8",
      stdio: ["pipe", "pipe", "pipe"],
      maxBuffer: 32 * 1024 * 1024,
    }
  ).trim();
}

function gitCommit() {
  const result = spawnSync("git", ["rev-parse", "HEAD"], {
    cwd: repoRoot,
    encoding: "utf8",
  });
  return result.status === 0 ? result.stdout.trim() : "UNKNOWN";
}

function readJson(filePath) {
  return JSON.parse(fs.readFileSync(filePath, "utf8"));
}

function parseLastJsonLine(raw) {
  const lines = String(raw).split(/\r?\n/).map((line) => line.trim()).filter(Boolean);
  assertCondition(lines.length > 0, "Expected PostgreSQL JSON output, received no rows");
  return JSON.parse(lines[lines.length - 1]);
}

function writeEvidence(evidence) {
  ensureDirectory(path.dirname(outputPath));
  fs.writeFileSync(outputPath, `${JSON.stringify(evidence, null, 2)}\n`, "utf8");
}

function assertCondition(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

function addId(taskId, offset) {
  const result = BigInt(taskId) + BigInt(offset);
  assertCondition(
    result <= BigInt(Number.MAX_SAFE_INTEGER),
    `Generated fixture id exceeds JavaScript safe integer range: ${result}`
  );
  return result.toString();
}

function rangeDisplay(item) {
  return `[${item.minimum}, ${item.maximum}] ${item.unit}`;
}

function modelIndex(productionModel) {
  const materials = new Map(productionModel.materials.map((item) => [item.code, item]));
  const qualityStandards = new Map(
    productionModel.qualityStandards.map((item) => [item.code, item])
  );
  return { materials, qualityStandards };
}

function loadInputs() {
  assertCondition(
    fs.existsSync(fullFlowEvidencePath),
    `Full-flow evidence does not exist: ${fullFlowEvidencePath}`
  );
  const scenario = loadScenario(scenarioPath);
  validateProductionModel(scenario.productionModel);
  const fullFlow = readJson(fullFlowEvidencePath);
  assertCondition(fullFlow.status === "PASS", "Full MES flow evidence must be PASS");
  assertCondition(fullFlow.marker === marker, "Full-flow marker does not match the pilot marker");
  assertCondition(
    fullFlow.retainedFixture && fullFlow.retainedFixture.retained === true,
    "Full MES flow fixture must be retained before pilot enrichment"
  );
  return { scenario, fullFlow };
}

function preflightSql(taskId, batchNo) {
  return `
SELECT json_build_object(
  'task', (
    SELECT json_build_object(
      'id', id,
      'tableNo', table_no,
      'batchNo', produce_batch_num,
      'productId', product_id,
      'formulaId', formula_id,
      'taskExecutionId', (
        SELECT id FROM public.wom_produce_task_exelog
        WHERE task_id=task.id AND coalesce(valid, true)
        ORDER BY id LIMIT 1
      )
    )
    FROM public.wom_produce_tasks task
    WHERE id=${taskId}
      AND table_no=${sqlLiteral(`${marker}_TASK_TN`)}
      AND produce_batch_num=${sqlLiteral(batchNo)}
  ),
  'firstProcess', (
    SELECT json_build_object('id', id, 'reportId', (
      SELECT id FROM public.wom_proc_reports
      WHERE task_id=${taskId}
        AND task_process_id=process.id
        AND proc_report_type='WOM_procReportType/taskProcess'
      ORDER BY id LIMIT 1
    ))
    FROM public.wom_task_processes process
    WHERE task_id=${taskId} AND coalesce(valid, true)
    ORDER BY exe_order, id LIMIT 1
  ),
  'firstProcessExecution', (
    SELECT json_build_object('id', id)
    FROM public.wom_process_exelogs
    WHERE task_id=${taskId} AND coalesce(valid, true)
    ORDER BY exe_order, id LIMIT 1
  ),
  'inputActivity', (
    SELECT json_build_object(
      'id', active.id,
      'executionId', execution.id,
      'reportId', execution.proc_report_id,
      'detailId', execution.putin_detail_id,
      'recordId', (
        SELECT id FROM public.wom_mat_consum_recods
        WHERE put_mat_detail_id=execution.putin_detail_id
        ORDER BY id LIMIT 1
      )
    )
    FROM public.wom_task_actives active
    JOIN public.wom_acti_exelogs execution ON execution.task_active_id=active.id
    WHERE active.task_id=${taskId}
      AND active.active_type='RM_activeType/putin'
      AND coalesce(active.valid, true)
    ORDER BY active.id LIMIT 1
  ),
  'outputActivity', (
    SELECT json_build_object(
      'id', active.id,
      'executionId', execution.id,
      'reportId', execution.proc_report_id,
      'detailId', execution.output_detail_id,
      'recordId', (
        SELECT id FROM public.wom_mat_outpt_records
        WHERE out_mat_detail_id=execution.output_detail_id
        ORDER BY id LIMIT 1
      )
    )
    FROM public.wom_task_actives active
    JOIN public.wom_acti_exelogs execution ON execution.task_active_id=active.id
    WHERE active.task_id=${taskId}
      AND active.active_type='RM_activeType/output'
      AND coalesce(active.valid, true)
    ORDER BY active.id LIMIT 1
  ),
  'completionOutput', (
    SELECT json_build_object(
      'reportId', report.id,
      'detailId', detail.id,
      'recordId', (
        SELECT id FROM public.wom_mat_outpt_records
        WHERE out_mat_detail_id=detail.id
        ORDER BY id LIMIT 1
      )
    )
    FROM public.wom_proc_reports report
    JOIN public.wom_output_details detail ON detail.head_id=report.id
    WHERE report.task_id=${taskId}
      AND report.proc_report_type='WOM_procReportType/produceTask'
      AND coalesce(detail.valid, true)
    ORDER BY detail.id LIMIT 1
  ),
  'finalBatchId', (
    SELECT id FROM public.baseset_batch_infos
    WHERE batch_num=${sqlLiteral(batchNo)}
    ORDER BY id DESC LIMIT 1
  ),
  'finalQuality', (
    SELECT json_build_object(
      'standardId', association.std_id,
      'versionId', (
        SELECT id FROM public.limsba_std_versions
        WHERE std_id=association.std_id AND coalesce(valid, true)
        ORDER BY active DESC, id LIMIT 1
      ),
      'associationId', association.id
    )
    FROM public.limsba_analy_prod_stds association
    WHERE association.product_id=(
      SELECT product_id FROM public.wom_produce_tasks WHERE id=${taskId}
    )
      AND association.available_std
      AND coalesce(association.valid, true)
    ORDER BY association.id DESC LIMIT 1
  ),
  'qcs', (
    SELECT json_build_object(
      'inspectId', inspect.id,
      'reportId', (
        SELECT id FROM public.qcs_inspect_reports
        WHERE inspect_id=inspect.id AND coalesce(valid, true)
        ORDER BY id DESC LIMIT 1
      )
    )
    FROM public.qcs_inspects inspect
    WHERE inspect.source_table_id=${taskId}
      AND inspect.batch_code=${sqlLiteral(batchNo)}
      AND coalesce(inspect.valid, true)
    ORDER BY inspect.id DESC LIMIT 1
  ),
  'unitTemplate', (
    SELECT json_build_object('id', id)
    FROM public.baseset_units
    WHERE valid=1
    ORDER BY CASE WHEN name='件' THEN 0 ELSE 1 END, id
    LIMIT 1
  ),
  'bpiLine', (
    SELECT json_build_object(
      'womLineId', wom_line_id,
      'tenantId', tenant_id,
      'plantId', plant_id,
      'lineId', line_id
    )
    FROM public.wom_bpi_production_context_bindings
    WHERE wom_line_id=${bpiWomLineId}
      AND tenant_id=${sqlLiteral(bpiScope.tenantId)}
      AND plant_id=${sqlLiteral(bpiScope.plantId)}
      AND line_id=${sqlLiteral(bpiScope.lineId)}
      AND enabled
  )
);`;
}

function assertPreflight(preflight, fullFlow) {
  const required = [
    "task",
    "firstProcess",
    "firstProcessExecution",
    "inputActivity",
    "outputActivity",
    "completionOutput",
    "finalBatchId",
    "finalQuality",
    "qcs",
    "unitTemplate",
    "bpiLine",
  ];
  const missing = required.filter((field) => !preflight[field]);
  assertCondition(
    missing.length === 0,
    `Pilot preflight is missing retained full-flow rows: ${missing.join(", ")}`
  );
  assertCondition(
    String(preflight.task.id) === String(fullFlow.identity.taskId),
    "Retained full-flow task identity changed"
  );
  assertCondition(
    String(preflight.task.productId) === String(fullFlow.identity.materialId),
    "Retained full-flow product identity changed"
  );
  for (const [name, value] of Object.entries({
    firstProcessReport: preflight.firstProcess.reportId,
    inputExecution: preflight.inputActivity.executionId,
    inputReport: preflight.inputActivity.reportId,
    inputDetail: preflight.inputActivity.detailId,
    inputRecord: preflight.inputActivity.recordId,
    outputExecution: preflight.outputActivity.executionId,
    outputReport: preflight.outputActivity.reportId,
    outputDetail: preflight.outputActivity.detailId,
    outputRecord: preflight.outputActivity.recordId,
    completionReport: preflight.completionOutput.reportId,
    completionDetail: preflight.completionOutput.detailId,
    completionRecord: preflight.completionOutput.recordId,
    finalStandard: preflight.finalQuality.standardId,
    finalStandardVersion: preflight.finalQuality.versionId,
    qcsReport: preflight.qcs.reportId,
  })) {
    assertCondition(Boolean(value), `Pilot preflight is missing ${name}`);
  }
}

function fixtureIds(taskId) {
  return {
    rawMaterial: addId(taskId, 10001),
    liquidMaterial: addId(taskId, 10002),
    rawBatch: addId(taskId, 10003),
    liquidBatch: addId(taskId, 10004),
    sacchProcess: addId(taskId, 10005),
    sacchExecution: addId(taskId, 10006),
    sacchProcessReport: addId(taskId, 10007),
    sacchInputActivity: addId(taskId, 10101),
    sacchOutputActivity: addId(taskId, 10102),
    pumpActivity: addId(taskId, 10103),
    heatActivity: addId(taskId, 10104),
    flashActivity: addId(taskId, 10105),
    holdActivity: addId(taskId, 10106),
    sacchInputReport: addId(taskId, 10201),
    sacchOutputReport: addId(taskId, 10202),
    pumpReport: addId(taskId, 10203),
    heatReport: addId(taskId, 10204),
    flashReport: addId(taskId, 10205),
    holdReport: addId(taskId, 10206),
    sacchInputExecution: addId(taskId, 10301),
    sacchOutputExecution: addId(taskId, 10302),
    pumpExecution: addId(taskId, 10303),
    heatExecution: addId(taskId, 10304),
    flashExecution: addId(taskId, 10305),
    holdExecution: addId(taskId, 10306),
    sacchInputDetail: addId(taskId, 10401),
    sacchConsumption: addId(taskId, 10402),
    sacchOutputDetail: addId(taskId, 10403),
    sacchOutputRecord: addId(taskId, 10404),
    rawCheckBase: addId(taskId, 10500),
    liquidCheckBase: addId(taskId, 10600),
    rawQualityBase: addId(taskId, 11000),
    liquidQualityBase: addId(taskId, 12000),
    tonneUnit: addId(taskId, 13000),
  };
}

function qualityIds(baseValue, itemCount) {
  const base = BigInt(baseValue);
  return {
    standard: base.toString(),
    version: (base + 1n).toString(),
    association: (base + 2n).toString(),
    gradeQualified: (base + 3n).toString(),
    gradeUnqualified: (base + 4n).toString(),
    items: Array.from({ length: itemCount }, (_value, index) => {
      const itemBase = base + 10n + BigInt(index * 3);
      return {
        component: itemBase.toString(),
        versionComponent: (itemBase + 1n).toString(),
        specLimit: (itemBase + 2n).toString(),
      };
    }),
  };
}

function qualityStandardSql(standard, materialId, baseValue) {
  const ids = qualityIds(baseValue, standard.items.length);
  const standardCode = `${marker}_${standard.code}`;
  const components = standard.items.map((item, index) => {
    const itemIds = ids.items[index];
    const code = `${marker}_${standard.materialCode}_${item.code}`;
    const reportName = item.name;
    return `
INSERT INTO public.limsba_test_components (
  id, version, valid, cid, create_staff_id, create_time, sort, table_info_id,
  code, name, report_name, unit_name, is_report, is_necessary, parallel_times,
  min_value, max_value, default_value, disp_value, memo_field
) VALUES (
  ${itemIds.component}, 0, true, 1000, 1, now(), ${index + 1}, ${itemIds.component},
  ${sqlLiteral(code)}, ${sqlLiteral(reportName)}, ${sqlLiteral(reportName)},
  ${sqlLiteral(item.unit)}, true, true, 1,
  ${sqlLiteral(item.minimum)}, ${sqlLiteral(item.maximum)},
  ${sqlLiteral(item.result)}, ${sqlLiteral(item.result)},
  ${sqlLiteral(`${marker};source=${item.source};TEST_ONLY_DRAFT=true`)}
) ON CONFLICT (id) DO UPDATE SET
  valid=true, sort=EXCLUDED.sort, code=EXCLUDED.code, name=EXCLUDED.name,
  report_name=EXCLUDED.report_name, unit_name=EXCLUDED.unit_name,
  is_report=true, is_necessary=true, parallel_times=1,
  min_value=EXCLUDED.min_value, max_value=EXCLUDED.max_value,
  default_value=EXCLUDED.default_value, disp_value=EXCLUDED.disp_value,
  memo_field=EXCLUDED.memo_field, modify_time=now();

INSERT INTO public.limsba_std_ver_coms (
  id, version, valid, cid, create_staff_id, create_time, sort, table_info_id,
  std_id, std_ver_id, com_id, code, is_report, report_name, report_sort,
  unit_name, parallel_times, valuen, sampling_plan, default_value, ref_value,
  memo_field
) VALUES (
  ${itemIds.versionComponent}, 0, true, 1000, 1, now(), ${index + 1},
  ${itemIds.versionComponent}, ${ids.standard}, ${ids.version},
  ${itemIds.component}, ${sqlLiteral(`${code}_STD_VER`)}, true,
  ${sqlLiteral(reportName)}, ${index + 1}, ${sqlLiteral(item.unit)}, 1, 1,
  'LIMSBasic_samplingPlan/level3', ${sqlLiteral(item.result)},
  ${sqlLiteral(rangeDisplay(item))}, ${sqlLiteral(`${marker};TEST_ONLY_DRAFT=true`)}
) ON CONFLICT (id) DO UPDATE SET
  valid=true, sort=EXCLUDED.sort, std_id=EXCLUDED.std_id,
  std_ver_id=EXCLUDED.std_ver_id, com_id=EXCLUDED.com_id,
  code=EXCLUDED.code, is_report=true, report_name=EXCLUDED.report_name,
  report_sort=EXCLUDED.report_sort, unit_name=EXCLUDED.unit_name,
  parallel_times=1, valuen=1, sampling_plan=EXCLUDED.sampling_plan,
  default_value=EXCLUDED.default_value, ref_value=EXCLUDED.ref_value,
  memo_field=EXCLUDED.memo_field, modify_time=now();

INSERT INTO public.limsba_spec_limits (
  id, version, valid, cid, create_staff_id, create_time, sort, table_info_id,
  code, disp_value, judge_cond, judge_option, judge_values,
  min_val_include, min_value, max_val_include, max_value,
  standard_grade, std_grade_name, std_id, std_ver_com_id,
  sampling_plan, valuen, result_value
) VALUES (
  ${itemIds.specLimit}, 0, true, 1000, 1, now(), ${index + 1},
  ${itemIds.specLimit}, ${sqlLiteral(`${code}_SPEC_QUALIFIED`)},
  ${sqlLiteral(rangeDisplay(item))}, 'BETWEEN', 'AND',
  ${sqlLiteral(`${item.minimum},${item.maximum}`)}, true,
  ${sqlLiteral(item.minimum)}, true, ${sqlLiteral(item.maximum)},
  'LIMSBasic_standardGrade/Qualified', '合格', ${ids.standard},
  ${itemIds.versionComponent}, 'LIMSBasic_samplingPlan/level3', 1,
  ${sqlLiteral(item.result)}
) ON CONFLICT (id) DO UPDATE SET
  valid=true, sort=EXCLUDED.sort, code=EXCLUDED.code,
  disp_value=EXCLUDED.disp_value, judge_cond=EXCLUDED.judge_cond,
  judge_option=EXCLUDED.judge_option, judge_values=EXCLUDED.judge_values,
  min_val_include=true, min_value=EXCLUDED.min_value,
  max_val_include=true, max_value=EXCLUDED.max_value,
  standard_grade=EXCLUDED.standard_grade, std_grade_name=EXCLUDED.std_grade_name,
  std_id=EXCLUDED.std_id, std_ver_com_id=EXCLUDED.std_ver_com_id,
  sampling_plan=EXCLUDED.sampling_plan, valuen=1,
  result_value=EXCLUDED.result_value, modify_time=now();`;
  }).join("\n");

  return `
INSERT INTO public.limsba_quality_stds (
  id, version, valid, cid, create_staff_id, create_time,
  create_department_id, create_position_id, group_id, owner_staff_id,
  owner_department_id, owner_position_id, position_lay_rec,
  status, table_no, table_info_id, code, name, standard, is_default, leaf
) VALUES (
  ${ids.standard}, 0, true, 1000, 1, now(),
  1, 1, 1000, 1, 1, 1, '1',
  99, ${sqlLiteral(`${standardCode}_TN`)}, ${ids.standard},
  ${sqlLiteral(standardCode)}, ${sqlLiteral(standard.name)},
  ${sqlLiteral(`${standard.code};${standard.gate};TEST_ONLY_DRAFT=true`)},
  true, true
) ON CONFLICT (id) DO UPDATE SET
  valid=true, status=99, code=EXCLUDED.code, name=EXCLUDED.name,
  standard=EXCLUDED.standard, is_default=true, modify_time=now();

INSERT INTO public.limsba_std_versions (
  id, version, valid, cid, create_staff_id, create_time,
  create_department_id, create_position_id, group_id, owner_staff_id,
  owner_department_id, owner_position_id, position_lay_rec,
  status, table_no, table_info_id, code, name, busi_version,
  std_id, active, start_date, end_date, leaf
) VALUES (
  ${ids.version}, 0, true, 1000, 1, now(),
  1, 1, 1000, 1, 1, 1, '1',
  99, ${sqlLiteral(`${standardCode}_V1_TN`)}, ${ids.version},
  ${sqlLiteral(`${standardCode}_V1`)}, ${sqlLiteral(`${standard.name} V1`)},
  'V1', ${ids.standard}, true, now() - interval '1 day',
  now() + interval '365 day', true
) ON CONFLICT (id) DO UPDATE SET
  valid=true, status=99, code=EXCLUDED.code, name=EXCLUDED.name,
  std_id=EXCLUDED.std_id, active=true, start_date=EXCLUDED.start_date,
  end_date=EXCLUDED.end_date, modify_time=now();

INSERT INTO public.limsba_std_ver_grades (
  id, version, valid, cid, create_staff_id, create_time, sort, table_info_id,
  std_grade, std_id, std_ver_id, code, name, memo_field
) VALUES
  (
    ${ids.gradeQualified}, 0, true, 1000, 1, now(), 10,
    ${ids.gradeQualified}, 'LIMSBasic_standardGrade/Qualified',
    ${ids.standard}, ${ids.version},
    ${sqlLiteral(`${standardCode}_GRADE_QUALIFIED`)}, '合格',
    ${sqlLiteral(`${marker};TEST_ONLY_DRAFT=true`)}
  ),
  (
    ${ids.gradeUnqualified}, 0, true, 1000, 1, now(), 20,
    ${ids.gradeUnqualified}, 'LIMSBasic_standardGrade/Unqualified',
    ${ids.standard}, ${ids.version},
    ${sqlLiteral(`${standardCode}_GRADE_UNQUALIFIED`)}, '不合格',
    ${sqlLiteral(`${marker};TEST_ONLY_DRAFT=true`)}
  )
ON CONFLICT (id) DO UPDATE SET
  valid=true, sort=EXCLUDED.sort, std_grade=EXCLUDED.std_grade,
  std_id=EXCLUDED.std_id, std_ver_id=EXCLUDED.std_ver_id,
  code=EXCLUDED.code, name=EXCLUDED.name,
  memo_field=EXCLUDED.memo_field, modify_time=now();

${components}

INSERT INTO public.limsba_analy_prod_stds (
  id, version, valid, cid, create_staff_id, create_time,
  table_info_id, code, memo_field, product_id, std_id, available_std
) VALUES (
  ${ids.association}, 0, true, 1000, 1, now(),
  ${ids.association}, ${sqlLiteral(`${standardCode}_PRODUCT`)},
  ${sqlLiteral(`${marker};${standard.gate};TEST_ONLY_DRAFT=true`)},
  ${materialId}, ${ids.standard}, true
) ON CONFLICT (id) DO UPDATE SET
  valid=true, code=EXCLUDED.code, memo_field=EXCLUDED.memo_field,
  product_id=EXCLUDED.product_id, std_id=EXCLUDED.std_id,
  available_std=true, modify_time=now();`;
}

function activityCloneSql({
  activeId,
  reportId,
  executionId,
  name,
  activeType,
  processId,
  processExecutionId,
  processReportId,
  materialId,
  batchNo,
  quantity,
  start,
  end,
  templateActiveId,
  templateReportId,
  templateExecutionId,
  remark,
}) {
  const materialSql = materialId ? materialId : "NULL";
  const batchSql = batchNo ? sqlLiteral(batchNo) : "NULL";
  return `
INSERT INTO public.wom_task_actives
SELECT (
  jsonb_populate_record(
    NULL::public.wom_task_actives,
    to_jsonb(template) || jsonb_build_object(
      'id', ${activeId},
      'table_info_id', ${activeId},
      'table_no', ${sqlLiteral(`${marker}_${name}_ACTIVE_TN`)},
      'task_process_id', ${processId},
      'name', ${sqlLiteral(name)},
      'active_type', ${sqlLiteral(activeType)},
      'material_id', ${materialSql},
      'material_batch_num', ${batchSql},
      'plan_quantity', ${quantity === null ? "NULL" : quantity},
      'standard_quantity', ${quantity === null ? "NULL" : quantity},
      'exec_sort', ${sqlLiteral(String(activeId))},
      'run_state', 'WOM_runState/finished',
      'act_start_time', ${sqlLiteral(start)},
      'act_end_time', ${sqlLiteral(end)},
      'is_finish', true,
      'is_run', false,
      'need_param_ana', false,
      'remark', NULL,
      'create_time', now(),
      'modify_time', now(),
      'valid', true
    )
  )
).*
FROM public.wom_task_actives template
WHERE template.id=${templateActiveId}
ON CONFLICT (id) DO UPDATE SET
  table_info_id=EXCLUDED.table_info_id, table_no=EXCLUDED.table_no,
  task_id=EXCLUDED.task_id, task_process_id=EXCLUDED.task_process_id,
  name=EXCLUDED.name, active_type=EXCLUDED.active_type,
  material_id=EXCLUDED.material_id, material_batch_num=EXCLUDED.material_batch_num,
  plan_quantity=EXCLUDED.plan_quantity, standard_quantity=EXCLUDED.standard_quantity,
  run_state=EXCLUDED.run_state, act_start_time=EXCLUDED.act_start_time,
  act_end_time=EXCLUDED.act_end_time, is_finish=true, is_run=false,
  need_param_ana=false, remark=NULL, valid=true, modify_time=now();

INSERT INTO public.wom_proc_reports
SELECT (
  jsonb_populate_record(
    NULL::public.wom_proc_reports,
    to_jsonb(template) || jsonb_build_object(
      'id', ${reportId},
      'table_info_id', ${reportId},
      'table_no', ${sqlLiteral(`${marker}_${name}_REPORT_TN`)},
      'task_process_id', ${processId},
      'task_active_id', ${activeId},
      'material_id', ${materialSql},
      'proc_report_type', 'WOM_procReportType/taskActive',
      'produce_time', ${sqlLiteral(end)},
      'is_finish', true,
      'remark', NULL,
      'create_time', now(),
      'modify_time', now(),
      'valid', true
    )
  )
).*
FROM public.wom_proc_reports template
WHERE template.id=${templateReportId}
ON CONFLICT (id) DO UPDATE SET
  table_info_id=EXCLUDED.table_info_id, table_no=EXCLUDED.table_no,
  task_id=EXCLUDED.task_id, task_process_id=EXCLUDED.task_process_id,
  task_active_id=EXCLUDED.task_active_id, material_id=EXCLUDED.material_id,
  proc_report_type=EXCLUDED.proc_report_type, produce_time=EXCLUDED.produce_time,
  is_finish=true, remark=NULL, valid=true, modify_time=now();

INSERT INTO public.wom_acti_exelogs
SELECT (
  jsonb_populate_record(
    NULL::public.wom_acti_exelogs,
    to_jsonb(template) || jsonb_build_object(
      'id', ${executionId},
      'table_info_id', ${executionId},
      'table_no', ${sqlLiteral(`${marker}_${name}_EXELOG_TN`)},
      'task_process_id', ${processId},
      'task_active_id', ${activeId},
      'proc_exelog_id', ${processExecutionId},
      'proc_report_id', ${reportId},
      'name', ${sqlLiteral(name)},
      'active_type', ${sqlLiteral(activeType)},
      'material_id', ${materialSql},
      'material_batch_num', ${batchSql},
      'produce_batch_num', ${batchSql},
      'actual_num', ${quantity === null ? "NULL" : quantity},
      'use_num', ${quantity === null ? "NULL" : quantity},
      'run_state', 'WOM_runState/finished',
      'act_start_time', ${sqlLiteral(start)},
      'act_end_time', ${sqlLiteral(end)},
      'actlong_time', extract(epoch from (${sqlLiteral(end)}::timestamp - ${sqlLiteral(start)}::timestamp)),
      'need_param_ana', false,
      'analysis_flag', false,
      'remark', NULL,
      'output_detail_id', NULL,
      'putin_detail_id', NULL,
      'create_time', now(),
      'modify_time', now(),
      'valid', true
    )
  )
).*
FROM public.wom_acti_exelogs template
WHERE template.id=${templateExecutionId}
ON CONFLICT (id) DO UPDATE SET
  table_info_id=EXCLUDED.table_info_id, table_no=EXCLUDED.table_no,
  task_id=EXCLUDED.task_id, task_process_id=EXCLUDED.task_process_id,
  task_active_id=EXCLUDED.task_active_id, proc_exelog_id=EXCLUDED.proc_exelog_id,
  proc_report_id=EXCLUDED.proc_report_id, name=EXCLUDED.name,
  active_type=EXCLUDED.active_type, material_id=EXCLUDED.material_id,
  material_batch_num=EXCLUDED.material_batch_num,
  produce_batch_num=EXCLUDED.produce_batch_num,
  actual_num=EXCLUDED.actual_num, use_num=EXCLUDED.use_num,
  run_state=EXCLUDED.run_state, act_start_time=EXCLUDED.act_start_time,
  act_end_time=EXCLUDED.act_end_time, actlong_time=EXCLUDED.actlong_time,
  need_param_ana=false, analysis_flag=false, remark=NULL,
  valid=true, modify_time=now();`;
}

function handoverCheckSql(standard, reportId, baseId) {
  return standard.items.map((item, index) => {
    const id = (BigInt(baseId) + BigInt(index)).toString();
    return `
INSERT INTO public.wom_pro_check_details (
  id, version, valid, cid, create_staff_id, create_time,
  create_department_id, create_position_id, group_id, owner_staff_id,
  owner_department_id, owner_position_id, position_lay_rec,
  status, table_no, table_info_id, head_id, check_items,
  standrad, report_value, report_num, is_pass, remark
) VALUES (
  ${id}, 0, true, 1000, 1, now(),
  1, 1, 1000, 1, 1, 1, '1',
  99, ${sqlLiteral(`${marker}_${standard.materialCode}_${item.code}_CHECK_TN`)},
  ${id}, ${reportId}, ${sqlLiteral(item.name)},
  ${sqlLiteral(rangeDisplay(item))}, ${sqlLiteral(item.result)}, 1, true, NULL
) ON CONFLICT (id) DO UPDATE SET
  valid=true, status=99, table_no=EXCLUDED.table_no,
  table_info_id=EXCLUDED.table_info_id, head_id=EXCLUDED.head_id,
  check_items=EXCLUDED.check_items, standrad=EXCLUDED.standrad,
  report_value=EXCLUDED.report_value, report_num=1, is_pass=true,
  remark=NULL, modify_time=now();`;
  }).join("\n");
}

function seedSql({ productionModel, preflight, ids, taskId, finalMaterialId, formulaId, batchNo }) {
  const index = modelIndex(productionModel);
  const raw = index.materials.get("STARCH_SLURRY");
  const liquid = index.materials.get("LIQUEFIED_SYRUP");
  const finalMaterial = index.materials.get("SACCHARIFIED_LIQUOR");
  const rawStandard = index.qualityStandards.get(raw.qualityStandard);
  const liquidStandard = index.qualityStandards.get(liquid.qualityStandard);
  const finalStandard = index.qualityStandards.get(finalMaterial.qualityStandard);
  const jet = productionModel.processes.find((item) => item.code === "JET");
  const sacch = productionModel.processes.find((item) => item.code === "SACCHARIFICATION");
  const rawBatchNo = `${marker}_STARCH_SLURRY_BATCH`;
  const liquidBatchNo = `${marker}_LIQUEFIED_SYRUP_BATCH`;
  const finalMaterialCode = `${marker}_SACCHARIFIED_LIQUOR`;
  const rawMaterialCode = `${marker}_STARCH_SLURRY`;
  const liquidMaterialCode = `${marker}_LIQUEFIED_SYRUP`;
  const pilotRemark = `${marker};FRU-JET-SACCH-V1;TEST_ONLY_DRAFT=true`;
  const firstProcessId = String(preflight.firstProcess.id);
  const firstProcessExecutionId = String(preflight.firstProcessExecution.id);
  const firstProcessReportId = String(preflight.firstProcess.reportId);
  const taskExecutionId = String(preflight.task.taskExecutionId);

  const equipmentActivities = [
    {
      activeId: ids.pumpActivity,
      reportId: ids.pumpReport,
      executionId: ids.pumpExecution,
      name: "泵送",
      processId: firstProcessId,
      processExecutionId: firstProcessExecutionId,
      processReportId: firstProcessReportId,
      start: "2026-07-27 16:00:20",
      end: "2026-07-27 16:00:28",
      remark: `${pilotRemark};activity=泵送;no_material=true`,
    },
    {
      activeId: ids.heatActivity,
      reportId: ids.heatReport,
      executionId: ids.heatExecution,
      name: "换热",
      processId: firstProcessId,
      processExecutionId: firstProcessExecutionId,
      processReportId: firstProcessReportId,
      start: "2026-07-27 16:00:24",
      end: "2026-07-27 16:00:36",
      remark: `${pilotRemark};activity=换热;no_material=true`,
    },
    {
      activeId: ids.flashActivity,
      reportId: ids.flashReport,
      executionId: ids.flashExecution,
      name: "闪蒸",
      processId: firstProcessId,
      processExecutionId: firstProcessExecutionId,
      processReportId: firstProcessReportId,
      start: "2026-07-27 16:00:34",
      end: "2026-07-27 16:00:44",
      remark: `${pilotRemark};activity=闪蒸;no_material=true`,
    },
    {
      activeId: ids.holdActivity,
      reportId: ids.holdReport,
      executionId: ids.holdExecution,
      name: "保温",
      processId: ids.sacchProcess,
      processExecutionId: ids.sacchExecution,
      processReportId: ids.sacchProcessReport,
      start: "2026-07-27 16:01:04",
      end: "2026-07-27 16:01:28",
      remark: `${pilotRemark};activity=保温;no_material=true`,
    },
  ];
  const equipmentActivitySql = equipmentActivities.map((activity) => activityCloneSql({
    ...activity,
    activeType: "RM_activeType/common",
    materialId: null,
    batchNo: null,
    quantity: null,
    templateActiveId: preflight.outputActivity.id,
    templateReportId: preflight.outputActivity.reportId,
    templateExecutionId: preflight.outputActivity.executionId,
  })).join("\n");

  const sacchInputSql = activityCloneSql({
    activeId: ids.sacchInputActivity,
    reportId: ids.sacchInputReport,
    executionId: ids.sacchInputExecution,
    name: "液化液投入",
    activeType: "RM_activeType/putin",
    processId: ids.sacchProcess,
    processExecutionId: ids.sacchExecution,
    processReportId: ids.sacchProcessReport,
    materialId: ids.liquidMaterial,
    batchNo: liquidBatchNo,
    quantity: sacch.inputQuantity,
    start: "2026-07-27 16:00:56",
    end: "2026-07-27 16:01:00",
    templateActiveId: preflight.inputActivity.id,
    templateReportId: preflight.inputActivity.reportId,
    templateExecutionId: preflight.inputActivity.executionId,
    remark: `${pilotRemark};material=LIQUEFIED_SYRUP;direction=input`,
  });
  const sacchOutputSql = activityCloneSql({
    activeId: ids.sacchOutputActivity,
    reportId: ids.sacchOutputReport,
    executionId: ids.sacchOutputExecution,
    name: "糖化液产出",
    activeType: "RM_activeType/output",
    processId: ids.sacchProcess,
    processExecutionId: ids.sacchExecution,
    processReportId: ids.sacchProcessReport,
    materialId: finalMaterialId,
    batchNo,
    quantity: sacch.outputQuantity,
    start: "2026-07-27 16:01:28",
    end: "2026-07-27 16:01:32",
    templateActiveId: preflight.outputActivity.id,
    templateReportId: preflight.outputActivity.reportId,
    templateExecutionId: preflight.outputActivity.executionId,
    remark: `${pilotRemark};material=SACCHARIFIED_LIQUOR;direction=output`,
  });

  return `
BEGIN;

DO $guard$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM public.wom_produce_tasks
    WHERE id=${taskId}
      AND table_no=${sqlLiteral(`${marker}_TASK_TN`)}
      AND produce_batch_num=${sqlLiteral(batchNo)}
  ) THEN
    RAISE EXCEPTION 'Retained fructose pilot task is missing; refusing to seed';
  END IF;
  IF EXISTS (
    SELECT 1 FROM public.baseset_materials
    WHERE id IN (${ids.rawMaterial}, ${ids.liquidMaterial})
      AND code NOT LIKE ${sqlLiteral(`${marker}_%`)}
  ) THEN
    RAISE EXCEPTION 'A reserved fructose pilot material id belongs to another record';
  END IF;
  IF EXISTS (
    SELECT 1 FROM public.baseset_units
    WHERE id=${ids.tonneUnit}
      AND code<>${sqlLiteral(`${marker}_TONNE`)}
  ) THEN
    RAISE EXCEPTION 'The reserved fructose pilot unit id belongs to another record';
  END IF;
  IF EXISTS (
    SELECT 1 FROM public.wom_task_processes
    WHERE id=${ids.sacchProcess} AND task_id<>${taskId}
  ) THEN
    RAISE EXCEPTION 'The reserved saccharification process id belongs to another task';
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM public.wom_bpi_production_context_bindings
    WHERE wom_line_id=${bpiWomLineId}
      AND tenant_id=${sqlLiteral(bpiScope.tenantId)}
      AND plant_id=${sqlLiteral(bpiScope.plantId)}
      AND line_id=${sqlLiteral(bpiScope.lineId)}
      AND enabled
  ) THEN
    RAISE EXCEPTION 'The required BPI-bound WOM production line is missing';
  END IF;
END
$guard$;

INSERT INTO public.baseset_units
SELECT (
  jsonb_populate_record(
    NULL::public.baseset_units,
    to_jsonb(template) || jsonb_build_object(
      'id', ${ids.tonneUnit},
      'table_info_id', ${ids.tonneUnit},
      'table_no', ${sqlLiteral(`${marker}_TONNE_UNIT_TN`)},
      'code', ${sqlLiteral(`${marker}_TONNE`)},
      'name', '吨',
      'symbol', 't',
      'memo_field', NULL,
      'create_time', now(),
      'modify_time', now(),
      'valid', 1
    )
  )
).*
FROM public.baseset_units template
WHERE template.id=${preflight.unitTemplate.id}
ON CONFLICT (id) DO UPDATE SET
  table_info_id=EXCLUDED.table_info_id, table_no=EXCLUDED.table_no,
  code=EXCLUDED.code, name='吨', symbol='t', memo_field=NULL,
  valid=1, modify_time=now();

UPDATE public.baseset_materials
SET code=${sqlLiteral(finalMaterialCode)},
    name='糖化液（试运行）',
    is_batch='BaseSet_isBatch/batch',
    is_check=true,
    main_unit=${ids.tonneUnit},
    produce_unit=${ids.tonneUnit},
    memo_field=NULL,
    valid=true,
    modify_time=now()
WHERE id=${finalMaterialId};

INSERT INTO public.baseset_materials
SELECT (
  jsonb_populate_record(
    NULL::public.baseset_materials,
    to_jsonb(template) || jsonb_build_object(
      'id', ${ids.rawMaterial},
      'table_info_id', ${ids.rawMaterial},
      'table_no', ${sqlLiteral(`${rawMaterialCode}_TN`)},
      'code', ${sqlLiteral(rawMaterialCode)},
      'name', '淀粉浆（试运行）',
      'is_batch', 'BaseSet_isBatch/batch',
      'is_check', true,
      'main_unit', ${ids.tonneUnit},
      'produce_unit', ${ids.tonneUnit},
      'memo_field', NULL,
      'create_time', now(),
      'modify_time', now(),
      'valid', true
    )
  )
).*
FROM public.baseset_materials template
WHERE template.id=${finalMaterialId}
ON CONFLICT (id) DO UPDATE SET
  table_info_id=EXCLUDED.table_info_id, table_no=EXCLUDED.table_no,
  code=EXCLUDED.code, name=EXCLUDED.name, is_batch=EXCLUDED.is_batch,
  is_check=true, main_unit=${ids.tonneUnit}, produce_unit=${ids.tonneUnit},
  memo_field=NULL, valid=true, modify_time=now();

INSERT INTO public.baseset_materials
SELECT (
  jsonb_populate_record(
    NULL::public.baseset_materials,
    to_jsonb(template) || jsonb_build_object(
      'id', ${ids.liquidMaterial},
      'table_info_id', ${ids.liquidMaterial},
      'table_no', ${sqlLiteral(`${liquidMaterialCode}_TN`)},
      'code', ${sqlLiteral(liquidMaterialCode)},
      'name', '液化液（试运行）',
      'is_batch', 'BaseSet_isBatch/batch',
      'is_check', true,
      'main_unit', ${ids.tonneUnit},
      'produce_unit', ${ids.tonneUnit},
      'memo_field', NULL,
      'create_time', now(),
      'modify_time', now(),
      'valid', true
    )
  )
).*
FROM public.baseset_materials template
WHERE template.id=${finalMaterialId}
ON CONFLICT (id) DO UPDATE SET
  table_info_id=EXCLUDED.table_info_id, table_no=EXCLUDED.table_no,
  code=EXCLUDED.code, name=EXCLUDED.name, is_batch=EXCLUDED.is_batch,
  is_check=true, main_unit=${ids.tonneUnit}, produce_unit=${ids.tonneUnit},
  memo_field=NULL, valid=true, modify_time=now();

UPDATE public.rm_formulas
SET product_id=${finalMaterialId},
    formula_name='果糖喷射液化至糖化试运行配方',
    quality_std_id=${preflight.finalQuality.standardId},
    modify_time=now(),
    valid=true
WHERE id=${formulaId};

UPDATE public.wom_produce_tasks
SET product_id=${finalMaterialId},
    line_id=${bpiWomLineId},
    plan_num=${sacch.outputQuantity},
    finish_num=${sacch.outputQuantity},
    act_start_time=${sqlLiteral(processWindows.jet.start)},
    act_end_time=${sqlLiteral(processWindows.saccharification.end)},
    remark=NULL,
    modify_time=now()
WHERE id=${taskId};

UPDATE public.wom_produce_task_exelog
SET product_id=${finalMaterialId},
    finish_num=${sacch.outputQuantity},
    act_start_time=${sqlLiteral(processWindows.jet.start)},
    act_end_time=${sqlLiteral(processWindows.saccharification.end)},
    remark=NULL,
    modify_time=now()
WHERE id=${taskExecutionId};

UPDATE public.baseset_batch_infos
SET material_id=${finalMaterialId},
    batch_num=${sqlLiteral(batchNo)},
    head_batch_num=${sqlLiteral(liquidBatchNo)},
    production_date=${sqlLiteral(processWindows.saccharification.end)},
    check_state='BaseSet_checkState/haveChecked',
    check_result='BaseSet_checkResult/qualified',
    is_available=true,
    remark=NULL,
    modify_time=now(),
    valid=true
WHERE id=${preflight.finalBatchId};

INSERT INTO public.baseset_batch_infos
SELECT (
  jsonb_populate_record(
    NULL::public.baseset_batch_infos,
    to_jsonb(template) || jsonb_build_object(
      'id', ${ids.rawBatch},
      'table_no', ${sqlLiteral(`${rawBatchNo}_TN`)},
      'batch_num', ${sqlLiteral(rawBatchNo)},
      'head_batch_num', NULL,
      'material_id', ${ids.rawMaterial},
      'production_date', ${sqlLiteral(processWindows.jet.start)},
      'check_state', 'BaseSet_checkState/haveChecked',
      'check_result', 'BaseSet_checkResult/qualified',
      'is_available', true,
      'remark', NULL,
      'create_time', now(),
      'modify_time', now(),
      'valid', true
    )
  )
).*
FROM public.baseset_batch_infos template
WHERE template.id=${preflight.finalBatchId}
ON CONFLICT (id) DO UPDATE SET
  table_no=EXCLUDED.table_no, batch_num=EXCLUDED.batch_num,
  head_batch_num=NULL, material_id=EXCLUDED.material_id,
  production_date=EXCLUDED.production_date, check_state=EXCLUDED.check_state,
  check_result=EXCLUDED.check_result, is_available=true,
  remark=NULL, valid=true, modify_time=now();

INSERT INTO public.baseset_batch_infos
SELECT (
  jsonb_populate_record(
    NULL::public.baseset_batch_infos,
    to_jsonb(template) || jsonb_build_object(
      'id', ${ids.liquidBatch},
      'table_no', ${sqlLiteral(`${liquidBatchNo}_TN`)},
      'batch_num', ${sqlLiteral(liquidBatchNo)},
      'head_batch_num', ${sqlLiteral(rawBatchNo)},
      'material_id', ${ids.liquidMaterial},
      'production_date', ${sqlLiteral(processWindows.jet.end)},
      'check_state', 'BaseSet_checkState/haveChecked',
      'check_result', 'BaseSet_checkResult/qualified',
      'is_available', true,
      'remark', NULL,
      'create_time', now(),
      'modify_time', now(),
      'valid', true
    )
  )
).*
FROM public.baseset_batch_infos template
WHERE template.id=${preflight.finalBatchId}
ON CONFLICT (id) DO UPDATE SET
  table_no=EXCLUDED.table_no, batch_num=EXCLUDED.batch_num,
  head_batch_num=EXCLUDED.head_batch_num, material_id=EXCLUDED.material_id,
  production_date=EXCLUDED.production_date, check_state=EXCLUDED.check_state,
  check_result=EXCLUDED.check_result, is_available=true,
  remark=NULL, valid=true, modify_time=now();

UPDATE public.wom_task_processes
SET name='喷射液化',
    table_no=${sqlLiteral(`${marker}_JET_PROCESS_TN`)},
    exe_order=1,
    proc_sort='1',
    act_start_time=${sqlLiteral(processWindows.jet.start)},
    act_end_time=${sqlLiteral(processWindows.jet.end)},
    plan_end_time=${sqlLiteral(processWindows.jet.end)},
    process_run_state='WOM_runState/finished',
    remark=NULL,
    valid=true,
    modify_time=now()
WHERE id=${firstProcessId} AND task_id=${taskId};

INSERT INTO public.wom_task_processes
SELECT (
  jsonb_populate_record(
    NULL::public.wom_task_processes,
    to_jsonb(template) || jsonb_build_object(
      'id', ${ids.sacchProcess},
      'table_info_id', ${ids.sacchProcess},
      'table_no', ${sqlLiteral(`${marker}_SACCHARIFICATION_PROCESS_TN`)},
      'name', '糖化',
      'exe_order', 2,
      'proc_sort', '2',
      'act_start_time', ${sqlLiteral(processWindows.saccharification.start)},
      'act_end_time', ${sqlLiteral(processWindows.saccharification.end)},
      'plan_end_time', ${sqlLiteral(processWindows.saccharification.end)},
      'process_run_state', 'WOM_runState/finished',
      'remark', NULL,
      'create_time', now(),
      'modify_time', now(),
      'valid', true
    )
  )
).*
FROM public.wom_task_processes template
WHERE template.id=${firstProcessId}
ON CONFLICT (id) DO UPDATE SET
  table_info_id=EXCLUDED.table_info_id, table_no=EXCLUDED.table_no,
  task_id=EXCLUDED.task_id, name=EXCLUDED.name, exe_order=2, proc_sort='2',
  act_start_time=EXCLUDED.act_start_time, act_end_time=EXCLUDED.act_end_time,
  plan_end_time=EXCLUDED.plan_end_time,
  process_run_state='WOM_runState/finished',
  remark=NULL, valid=true, modify_time=now();

UPDATE public.wom_proc_reports
SET task_process_id=${firstProcessId},
    is_finish=true,
    produce_time=${sqlLiteral(processWindows.jet.end)},
    remark=NULL,
    valid=true,
    modify_time=now()
WHERE id=${firstProcessReportId};

INSERT INTO public.wom_proc_reports
SELECT (
  jsonb_populate_record(
    NULL::public.wom_proc_reports,
    to_jsonb(template) || jsonb_build_object(
      'id', ${ids.sacchProcessReport},
      'table_info_id', ${ids.sacchProcessReport},
      'table_no', ${sqlLiteral(`${marker}_SACCHARIFICATION_REPORT_TN`)},
      'task_process_id', ${ids.sacchProcess},
      'produce_time', ${sqlLiteral(processWindows.saccharification.end)},
      'is_finish', true,
      'remark', NULL,
      'create_time', now(),
      'modify_time', now(),
      'valid', true
    )
  )
).*
FROM public.wom_proc_reports template
WHERE template.id=${firstProcessReportId}
ON CONFLICT (id) DO UPDATE SET
  table_info_id=EXCLUDED.table_info_id, table_no=EXCLUDED.table_no,
  task_id=EXCLUDED.task_id, task_process_id=EXCLUDED.task_process_id,
  proc_report_type='WOM_procReportType/taskProcess',
  produce_time=EXCLUDED.produce_time, is_finish=true,
  remark=NULL, valid=true, modify_time=now();

UPDATE public.wom_process_exelogs
SET name='喷射液化',
    task_process_id=${firstProcessId},
    proc_report_id=${firstProcessReportId},
    exe_order=1,
    act_start_time=${sqlLiteral(processWindows.jet.start)},
    act_end_time=${sqlLiteral(processWindows.jet.end)},
    long_time=24,
    process_run_state='WOM_runState/finished',
    produce_batch_num=${sqlLiteral(batchNo)},
    remark=NULL,
    valid=true,
    modify_time=now()
WHERE id=${firstProcessExecutionId} AND task_id=${taskId};

INSERT INTO public.wom_process_exelogs
SELECT (
  jsonb_populate_record(
    NULL::public.wom_process_exelogs,
    to_jsonb(template) || jsonb_build_object(
      'id', ${ids.sacchExecution},
      'table_info_id', ${taskId},
      'table_no', NULL,
      'task_process_id', ${ids.sacchProcess},
      'proc_report_id', ${ids.sacchProcessReport},
      'name', '糖化',
      'exe_order', 2,
      'act_start_time', ${sqlLiteral(processWindows.saccharification.start)},
      'act_end_time', ${sqlLiteral(processWindows.saccharification.end)},
      'long_time', 36,
      'process_run_state', 'WOM_runState/finished',
      'produce_batch_num', ${sqlLiteral(batchNo)},
      'remark', NULL,
      'create_time', now(),
      'modify_time', now(),
      'valid', true
    )
  )
).*
FROM public.wom_process_exelogs template
WHERE template.id=${firstProcessExecutionId}
ON CONFLICT (id) DO UPDATE SET
  table_info_id=EXCLUDED.table_info_id, task_id=EXCLUDED.task_id,
  task_process_id=EXCLUDED.task_process_id, proc_report_id=EXCLUDED.proc_report_id,
  name=EXCLUDED.name, exe_order=2,
  act_start_time=EXCLUDED.act_start_time, act_end_time=EXCLUDED.act_end_time,
  long_time=EXCLUDED.long_time, process_run_state=EXCLUDED.process_run_state,
  produce_batch_num=EXCLUDED.produce_batch_num,
  remark=NULL, valid=true, modify_time=now();

UPDATE public.wom_task_actives
SET name='淀粉浆投料',
    task_process_id=${firstProcessId},
    material_id=${ids.rawMaterial},
    material_batch_num=${sqlLiteral(rawBatchNo)},
    plan_quantity=${jet.inputQuantity},
    standard_quantity=${jet.inputQuantity},
    act_start_time=${sqlLiteral(processWindows.jet.start)},
    act_end_time='2026-07-27 16:00:24',
    remark=NULL,
    valid=true,
    modify_time=now()
WHERE id=${preflight.inputActivity.id};

UPDATE public.wom_proc_reports
SET task_process_id=${firstProcessId},
    task_active_id=${preflight.inputActivity.id},
    material_id=${ids.rawMaterial},
    produce_time='2026-07-27 16:00:24',
    remark=NULL,
    valid=true,
    modify_time=now()
WHERE id=${preflight.inputActivity.reportId};

UPDATE public.wom_acti_exelogs
SET name='淀粉浆投料',
    task_process_id=${firstProcessId},
    proc_exelog_id=${firstProcessExecutionId},
    material_id=${ids.rawMaterial},
    material_batch_num=${sqlLiteral(rawBatchNo)},
    produce_batch_num=${sqlLiteral(batchNo)},
    actual_num=${jet.inputQuantity},
    use_num=${jet.inputQuantity},
    act_start_time=${sqlLiteral(processWindows.jet.start)},
    act_end_time='2026-07-27 16:00:24',
    actlong_time=4,
    remark=NULL,
    valid=true,
    modify_time=now()
WHERE id=${preflight.inputActivity.executionId};

UPDATE public.wom_putin_details
SET material_id=${ids.rawMaterial},
    material_batch_num=${sqlLiteral(rawBatchNo)},
    putin_num=${jet.inputQuantity},
    use_num=${jet.inputQuantity},
    putin_time=${sqlLiteral(processWindows.jet.start)},
    putin_end_time='2026-07-27 16:00:24',
    remark=NULL,
    valid=true,
    modify_time=now()
WHERE id=${preflight.inputActivity.detailId};

UPDATE public.wom_mat_consum_recods
SET material_id=${ids.rawMaterial},
    mat_batch_num=${sqlLiteral(rawBatchNo)},
    produce_batch_num=${sqlLiteral(batchNo)},
    proc_exelog_id=${firstProcessExecutionId},
    putin_num=${jet.inputQuantity},
    report_num=${jet.inputQuantity},
    putin_time=${sqlLiteral(processWindows.jet.start)},
    putin_end_time='2026-07-27 16:00:24',
    duriton=4,
    remark=NULL,
    valid=true,
    modify_time=now()
WHERE id=${preflight.inputActivity.recordId};

UPDATE public.wom_task_actives
SET name='液化液产出',
    task_process_id=${firstProcessId},
    material_id=${ids.liquidMaterial},
    material_batch_num=${sqlLiteral(liquidBatchNo)},
    plan_quantity=${jet.outputQuantity},
    standard_quantity=${jet.outputQuantity},
    act_start_time='2026-07-27 16:00:40',
    act_end_time=${sqlLiteral(processWindows.jet.end)},
    remark=NULL,
    valid=true,
    modify_time=now()
WHERE id=${preflight.outputActivity.id};

UPDATE public.wom_proc_reports
SET task_process_id=${firstProcessId},
    task_active_id=${preflight.outputActivity.id},
    material_id=${ids.liquidMaterial},
    produce_time=${sqlLiteral(processWindows.jet.end)},
    remark=NULL,
    valid=true,
    modify_time=now()
WHERE id=${preflight.outputActivity.reportId};

UPDATE public.wom_acti_exelogs
SET name='液化液产出',
    task_process_id=${firstProcessId},
    proc_exelog_id=${firstProcessExecutionId},
    material_id=${ids.liquidMaterial},
    material_batch_num=${sqlLiteral(liquidBatchNo)},
    produce_batch_num=${sqlLiteral(batchNo)},
    actual_num=${jet.outputQuantity},
    use_num=${jet.outputQuantity},
    act_start_time='2026-07-27 16:00:40',
    act_end_time=${sqlLiteral(processWindows.jet.end)},
    actlong_time=4,
    remark=NULL,
    valid=true,
    modify_time=now()
WHERE id=${preflight.outputActivity.executionId};

UPDATE public.wom_output_details
SET product=${ids.liquidMaterial},
    material_batch_num=${sqlLiteral(liquidBatchNo)},
    output_num=${jet.outputQuantity},
    report_num=${jet.outputQuantity},
    putin_time='2026-07-27 16:00:40',
    putin_end_time=${sqlLiteral(processWindows.jet.end)},
    remark=NULL,
    valid=true,
    modify_time=now()
WHERE id=${preflight.outputActivity.detailId};

UPDATE public.wom_mat_outpt_records
SET material_id=${ids.liquidMaterial},
    mat_batch_num=${sqlLiteral(liquidBatchNo)},
    produce_batch_num=${sqlLiteral(batchNo)},
    proc_exelog_id=${firstProcessExecutionId},
    output_num=${jet.outputQuantity},
    report_num=${jet.outputQuantity},
    output_time='2026-07-27 16:00:40',
    output_end_time=${sqlLiteral(processWindows.jet.end)},
    duriton=4,
    remark=NULL,
    valid=true,
    modify_time=now()
WHERE id=${preflight.outputActivity.recordId};

${sacchInputSql}
${sacchOutputSql}
${equipmentActivitySql}

INSERT INTO public.wom_putin_details
SELECT (
  jsonb_populate_record(
    NULL::public.wom_putin_details,
    to_jsonb(template) || jsonb_build_object(
      'id', ${ids.sacchInputDetail},
      'table_info_id', ${ids.sacchInputDetail},
      'table_no', ${sqlLiteral(`${marker}_SACCH_INPUT_DETAIL_TN`)},
      'head_id', ${ids.sacchInputReport},
      'material_id', ${ids.liquidMaterial},
      'material_batch_num', ${sqlLiteral(liquidBatchNo)},
      'putin_num', ${sacch.inputQuantity},
      'use_num', ${sacch.inputQuantity},
      'putin_time', ${sqlLiteral(processWindows.saccharification.start)},
      'putin_end_time', '2026-07-27 16:01:00',
      'remark', NULL,
      'create_time', now(),
      'modify_time', now(),
      'valid', true
    )
  )
).*
FROM public.wom_putin_details template
WHERE template.id=${preflight.inputActivity.detailId}
ON CONFLICT (id) DO UPDATE SET
  table_info_id=EXCLUDED.table_info_id, table_no=EXCLUDED.table_no,
  head_id=EXCLUDED.head_id, material_id=EXCLUDED.material_id,
  material_batch_num=EXCLUDED.material_batch_num,
  putin_num=EXCLUDED.putin_num, use_num=EXCLUDED.use_num,
  putin_time=EXCLUDED.putin_time,
  putin_end_time=EXCLUDED.putin_end_time, remark=NULL,
  valid=true, modify_time=now();

INSERT INTO public.wom_mat_consum_recods
SELECT (
  jsonb_populate_record(
    NULL::public.wom_mat_consum_recods,
    to_jsonb(template) || jsonb_build_object(
      'id', ${ids.sacchConsumption},
      'table_info_id', ${taskId},
      'act_exelog_id', ${ids.sacchInputExecution},
      'proc_exelog_id', ${ids.sacchExecution},
      'put_mat_detail_id', ${ids.sacchInputDetail},
      'material_id', ${ids.liquidMaterial},
      'mat_batch_num', ${sqlLiteral(liquidBatchNo)},
      'produce_batch_num', ${sqlLiteral(batchNo)},
      'putin_num', ${sacch.inputQuantity},
      'report_num', ${sacch.inputQuantity},
      'putin_time', ${sqlLiteral(processWindows.saccharification.start)},
      'putin_end_time', '2026-07-27 16:01:00',
      'duriton', 4,
      'remark', NULL,
      'create_time', now(),
      'modify_time', now(),
      'valid', true
    )
  )
).*
FROM public.wom_mat_consum_recods template
WHERE template.id=${preflight.inputActivity.recordId}
ON CONFLICT (id) DO UPDATE SET
  table_info_id=EXCLUDED.table_info_id, act_exelog_id=EXCLUDED.act_exelog_id,
  proc_exelog_id=EXCLUDED.proc_exelog_id,
  put_mat_detail_id=EXCLUDED.put_mat_detail_id,
  material_id=EXCLUDED.material_id, mat_batch_num=EXCLUDED.mat_batch_num,
  produce_batch_num=EXCLUDED.produce_batch_num,
  putin_num=EXCLUDED.putin_num, report_num=EXCLUDED.report_num,
  putin_time=EXCLUDED.putin_time, putin_end_time=EXCLUDED.putin_end_time,
  duriton=EXCLUDED.duriton, remark=NULL,
  valid=true, modify_time=now();

UPDATE public.wom_acti_exelogs
SET putin_detail_id=${ids.sacchInputDetail}
WHERE id=${ids.sacchInputExecution};

INSERT INTO public.wom_output_details
SELECT (
  jsonb_populate_record(
    NULL::public.wom_output_details,
    to_jsonb(template) || jsonb_build_object(
      'id', ${ids.sacchOutputDetail},
      'table_info_id', ${ids.sacchOutputDetail},
      'table_no', ${sqlLiteral(`${marker}_SACCH_OUTPUT_DETAIL_TN`)},
      'head_id', ${ids.sacchOutputReport},
      'product', ${finalMaterialId},
      'material_batch_num', ${sqlLiteral(batchNo)},
      'output_num', ${sacch.outputQuantity},
      'report_num', ${sacch.outputQuantity},
      'putin_time', '2026-07-27 16:01:28',
      'putin_end_time', ${sqlLiteral(processWindows.saccharification.end)},
      'remark', NULL,
      'create_time', now(),
      'modify_time', now(),
      'valid', true
    )
  )
).*
FROM public.wom_output_details template
WHERE template.id=${preflight.outputActivity.detailId}
ON CONFLICT (id) DO UPDATE SET
  table_info_id=EXCLUDED.table_info_id, table_no=EXCLUDED.table_no,
  head_id=EXCLUDED.head_id, product=EXCLUDED.product,
  material_batch_num=EXCLUDED.material_batch_num,
  output_num=EXCLUDED.output_num, report_num=EXCLUDED.report_num,
  putin_time=EXCLUDED.putin_time, putin_end_time=EXCLUDED.putin_end_time,
  remark=NULL, valid=true, modify_time=now();

INSERT INTO public.wom_mat_outpt_records
SELECT (
  jsonb_populate_record(
    NULL::public.wom_mat_outpt_records,
    to_jsonb(template) || jsonb_build_object(
      'id', ${ids.sacchOutputRecord},
      'table_info_id', ${taskId},
      'act_exelog_id', ${ids.sacchOutputExecution},
      'proc_exelog_id', ${ids.sacchExecution},
      'out_mat_detail_id', ${ids.sacchOutputDetail},
      'material_id', ${finalMaterialId},
      'mat_batch_num', ${sqlLiteral(batchNo)},
      'produce_batch_num', ${sqlLiteral(batchNo)},
      'output_num', ${sacch.outputQuantity},
      'report_num', ${sacch.outputQuantity},
      'output_time', '2026-07-27 16:01:28',
      'output_end_time', ${sqlLiteral(processWindows.saccharification.end)},
      'duriton', 4,
      'remark', NULL,
      'create_time', now(),
      'modify_time', now(),
      'valid', true
    )
  )
).*
FROM public.wom_mat_outpt_records template
WHERE template.id=${preflight.outputActivity.recordId}
ON CONFLICT (id) DO UPDATE SET
  table_info_id=EXCLUDED.table_info_id, act_exelog_id=EXCLUDED.act_exelog_id,
  proc_exelog_id=EXCLUDED.proc_exelog_id,
  out_mat_detail_id=EXCLUDED.out_mat_detail_id,
  material_id=EXCLUDED.material_id, mat_batch_num=EXCLUDED.mat_batch_num,
  produce_batch_num=EXCLUDED.produce_batch_num,
  output_num=EXCLUDED.output_num, report_num=EXCLUDED.report_num,
  output_time=EXCLUDED.output_time, output_end_time=EXCLUDED.output_end_time,
  duriton=EXCLUDED.duriton, remark=NULL,
  valid=true, modify_time=now();

UPDATE public.wom_acti_exelogs
SET output_detail_id=${ids.sacchOutputDetail}
WHERE id=${ids.sacchOutputExecution};

UPDATE public.wom_output_details
SET product=${finalMaterialId},
    material_batch_num=${sqlLiteral(batchNo)},
    output_num=${sacch.outputQuantity},
    report_num=${sacch.outputQuantity},
    putin_time='2026-07-27 16:01:28',
    putin_end_time=${sqlLiteral(processWindows.saccharification.end)},
    remark=NULL,
    valid=true,
    modify_time=now()
WHERE id=${preflight.completionOutput.detailId};

UPDATE public.wom_mat_outpt_records
SET material_id=${finalMaterialId},
    mat_batch_num=${sqlLiteral(batchNo)},
    produce_batch_num=${sqlLiteral(batchNo)},
    output_num=${sacch.outputQuantity},
    report_num=${sacch.outputQuantity},
    output_time='2026-07-27 16:01:28',
    output_end_time=${sqlLiteral(processWindows.saccharification.end)},
    remark=NULL,
    valid=true,
    modify_time=now()
WHERE id=${preflight.completionOutput.recordId};

${qualityStandardSql(rawStandard, ids.rawMaterial, ids.rawQualityBase)}
${qualityStandardSql(liquidStandard, ids.liquidMaterial, ids.liquidQualityBase)}

UPDATE public.limsba_quality_stds
SET code=${sqlLiteral(`${marker}_${finalStandard.code}`)},
    name=${sqlLiteral(finalStandard.name)},
    standard=${sqlLiteral(`${finalStandard.code};${finalStandard.gate};TEST_ONLY_DRAFT=true`)},
    modify_time=now(),
    valid=true
WHERE id=${preflight.finalQuality.standardId};

UPDATE public.limsba_analy_prod_stds
SET code=${sqlLiteral(`${marker}_${finalStandard.code}_PRODUCT`)},
    memo_field=${sqlLiteral(`${marker};${finalStandard.gate};TEST_ONLY_DRAFT=true`)},
    product_id=${finalMaterialId},
    available_std=true,
    modify_time=now(),
    valid=true
WHERE id=${preflight.finalQuality.associationId};

${handoverCheckSql(rawStandard, preflight.inputActivity.reportId, ids.rawCheckBase)}
${handoverCheckSql(liquidStandard, preflight.outputActivity.reportId, ids.liquidCheckBase)}

COMMIT;
`;
}

function resetMarkerWmsSql(taskId, batchNo, finalMaterialCode) {
  const sourceDocumentId = `${marker}_WMS_IN`;
  const sourceLineId = `${marker}_WMS_LINE`;
  const tenantScope = [
    sqlLiteral(tenantId),
    sqlLiteral(legacyTenantId),
  ].join(", ");
  return `
BEGIN;
DELETE FROM public.pa_trace_snapshots
WHERE tenant_id IN (${tenantScope})
  AND (task_id=${taskId} OR batch_no=${sqlLiteral(batchNo)});
DELETE FROM public.wms_quality_results
WHERE tenant_id IN (${tenantScope})
  AND source_line_id=${sqlLiteral(sourceLineId)};
DELETE FROM public.wms_inventory_transactions
WHERE tenant_id IN (${tenantScope})
  AND source_document_id=${sqlLiteral(sourceDocumentId)};
DELETE FROM public.wms_stock_documents
WHERE tenant_id IN (${tenantScope})
  AND source_document_id=${sqlLiteral(sourceDocumentId)};
DELETE FROM public.wms_batch_stocks
WHERE tenant_id IN (${tenantScope})
  AND material_code=${sqlLiteral(finalMaterialCode)}
  AND batch_no=${sqlLiteral(batchNo)};
COMMIT;

SELECT json_build_object(
  'documents', (
    SELECT count(*) FROM public.wms_stock_documents
    WHERE tenant_id IN (${tenantScope})
      AND source_document_id=${sqlLiteral(sourceDocumentId)}
  ),
  'transactions', (
    SELECT count(*) FROM public.wms_inventory_transactions
    WHERE tenant_id IN (${tenantScope})
      AND source_document_id=${sqlLiteral(sourceDocumentId)}
  ),
  'stocks', (
    SELECT count(*) FROM public.wms_batch_stocks
    WHERE tenant_id IN (${tenantScope})
      AND material_code=${sqlLiteral(finalMaterialCode)}
      AND batch_no=${sqlLiteral(batchNo)}
  ),
  'snapshots', (
    SELECT count(*) FROM public.pa_trace_snapshots
    WHERE tenant_id IN (${tenantScope})
      AND (task_id=${taskId} OR batch_no=${sqlLiteral(batchNo)})
  )
);`;
}

function verificationSql({ taskId, batchNo, ids, preflight, productionModel }) {
  const rawBatchNo = `${marker}_STARCH_SLURRY_BATCH`;
  const liquidBatchNo = `${marker}_LIQUEFIED_SYRUP_BATCH`;
  const sourceDocumentId = `${marker}_WMS_IN`;
  const finalQuantity = productionModel.processes.find(
    (item) => item.code === "SACCHARIFICATION"
  ).outputQuantity;
  return `
WITH process_rows AS (
  SELECT id, name, exe_order, act_start_time, act_end_time, process_run_state
  FROM public.wom_process_exelogs
  WHERE task_id=${taskId} AND coalesce(valid, true)
), quality_items AS (
  SELECT standard.id AS standard_id, count(component.id) AS item_count
  FROM public.limsba_quality_stds standard
  JOIN public.limsba_std_ver_coms component ON component.std_id=standard.id
  WHERE standard.id IN (
    ${ids.rawQualityBase}, ${ids.liquidQualityBase},
    ${preflight.finalQuality.standardId}
  )
    AND coalesce(standard.valid, true)
    AND coalesce(component.valid, true)
  GROUP BY standard.id
)
SELECT json_build_object(
  'task', (
    SELECT json_build_object(
      'id', id, 'tableNo', table_no, 'batchNo', produce_batch_num,
      'lineId', line_id, 'productId', product_id, 'planNum', plan_num,
      'finishNum', finish_num, 'state', task_run_state,
      'checkState', check_state, 'checkResult', check_result
    )
    FROM public.wom_produce_tasks WHERE id=${taskId}
  ),
  'processes', COALESCE((
    SELECT json_agg(row_to_json(process_rows) ORDER BY exe_order)
    FROM process_rows
  ), '[]'::json),
  'handoverSeconds', (
    SELECT extract(epoch FROM (
      (SELECT act_start_time FROM process_rows WHERE exe_order=2)
      -
      (SELECT act_end_time FROM process_rows WHERE exe_order=1)
    ))::int
  ),
  'materials', COALESCE((
    SELECT json_agg(json_build_object(
      'id', material.id, 'code', material.code, 'name', material.name,
      'isBatch', material.is_batch, 'isCheck', material.is_check,
      'mainUnitId', material.main_unit, 'mainUnitName', main_unit.name,
      'produceUnitId', material.produce_unit, 'produceUnitName', produce_unit.name
    ) ORDER BY material.id)
    FROM public.baseset_materials material
    LEFT JOIN public.baseset_units main_unit ON main_unit.id=material.main_unit
    LEFT JOIN public.baseset_units produce_unit ON produce_unit.id=material.produce_unit
    WHERE material.id IN (
      ${ids.rawMaterial}, ${ids.liquidMaterial}, ${preflight.task.productId}
    )
  ), '[]'::json),
  'unit', (
    SELECT json_build_object(
      'id', id, 'code', code, 'name', name, 'symbol', symbol
    )
    FROM public.baseset_units
    WHERE id=${ids.tonneUnit}
  ),
  'batches', COALESCE((
    SELECT json_agg(json_build_object(
      'id', id, 'batchNo', batch_num, 'headBatchNo', head_batch_num,
      'materialId', material_id, 'checkResult', check_result,
      'available', is_available
    ) ORDER BY id)
    FROM public.baseset_batch_infos
    WHERE id IN (${ids.rawBatch}, ${ids.liquidBatch}, ${preflight.finalBatchId})
  ), '[]'::json),
  'lineage', json_build_array(
    json_build_object(
      'process', '喷射液化',
      'inputBatch', ${sqlLiteral(rawBatchNo)},
      'outputBatch', ${sqlLiteral(liquidBatchNo)},
      'inputQuantity', (
        SELECT putin_num FROM public.wom_putin_details
        WHERE id=${preflight.inputActivity.detailId}
      ),
      'outputQuantity', (
        SELECT output_num FROM public.wom_output_details
        WHERE id=${preflight.outputActivity.detailId}
      )
    ),
    json_build_object(
      'process', '糖化',
      'inputBatch', ${sqlLiteral(liquidBatchNo)},
      'outputBatch', ${sqlLiteral(batchNo)},
      'inputQuantity', (
        SELECT putin_num FROM public.wom_putin_details
        WHERE id=${ids.sacchInputDetail}
      ),
      'outputQuantity', (
        SELECT output_num FROM public.wom_output_details
        WHERE id=${ids.sacchOutputDetail}
      )
    )
  ),
  'equipmentActivities', COALESCE((
    SELECT json_agg(json_build_object(
      'name', name, 'processId', task_process_id,
      'materialId', material_id, 'batchNo', material_batch_num,
      'state', run_state
    ) ORDER BY id)
    FROM public.wom_task_actives
    WHERE id IN (
      ${ids.pumpActivity}, ${ids.heatActivity},
      ${ids.flashActivity}, ${ids.holdActivity}
    )
  ), '[]'::json),
  'qualityStandards', COALESCE((
    SELECT json_agg(json_build_object(
      'id', standard.id, 'code', standard.code, 'name', standard.name,
      'definition', standard.standard, 'itemCount', quality_items.item_count
    ) ORDER BY standard.id)
    FROM public.limsba_quality_stds standard
    JOIN quality_items ON quality_items.standard_id=standard.id
  ), '[]'::json),
  'handoverCheckCount', (
    SELECT count(*) FROM public.wom_pro_check_details
    WHERE id >= ${ids.rawCheckBase}
      AND id < (${ids.liquidCheckBase}::bigint + 100)
      AND coalesce(valid, true)
      AND is_pass
  ),
  'finalQcsItems', COALESCE((
    SELECT json_agg(json_build_object(
      'name', component.report_name,
      'value', component.disp_value,
      'result', component.check_result
    ) ORDER BY component.id)
    FROM public.qcs_report_coms component
    WHERE component.report_id=${preflight.qcs.reportId}
      AND coalesce(component.valid, true)
  ), '[]'::json),
  'wms', json_build_object(
    'documentCount', (
      SELECT count(*) FROM public.wms_stock_documents
      WHERE tenant_id=${sqlLiteral(tenantId)}
        AND source_document_id=${sqlLiteral(sourceDocumentId)}
    ),
    'transactionCount', (
      SELECT count(*) FROM public.wms_inventory_transactions
      WHERE tenant_id=${sqlLiteral(tenantId)}
        AND source_document_id=${sqlLiteral(sourceDocumentId)}
    ),
    'stock', (
      SELECT json_build_object(
        'materialCode', material_code, 'batchNo', batch_no,
        'onHand', on_hand_quantity, 'available', available_quantity,
        'hold', hold_quantity
      )
      FROM public.wms_batch_stocks
      WHERE tenant_id=${sqlLiteral(tenantId)}
        AND batch_no=${sqlLiteral(batchNo)}
      ORDER BY id DESC LIMIT 1
    )
  ),
  'traceSnapshotCount', (
    SELECT count(*) FROM public.pa_trace_snapshots
    WHERE tenant_id=${sqlLiteral(tenantId)}
      AND (task_id=${taskId} OR batch_no=${sqlLiteral(batchNo)})
  ),
  'expectedFinalQuantity', ${finalQuantity}
);`;
}

function bpiVerificationSql() {
  return `
WITH scoped_events AS (
  SELECT *
  FROM bpi.bpi_telemetry_events
  WHERE tenant_id=${sqlLiteral(bpiScope.tenantId)}
    AND plant_id=${sqlLiteral(bpiScope.plantId)}
    AND line_id=${sqlLiteral(bpiScope.lineId)}
    AND event_time BETWEEN
      timestamptz ${sqlLiteral(bpiScope.from)}
      AND timestamptz ${sqlLiteral(bpiScope.to)}
), scoped_points AS (
  SELECT point.*
  FROM bpi.bpi_telemetry_points point
  JOIN scoped_events event
    ON event.id=point.telemetry_event_id
   AND event.tenant_id=point.tenant_id
)
SELECT json_build_object(
  'events', (SELECT count(*) FROM scoped_events),
  'acceptedEvents', (
    SELECT count(*) FROM scoped_events WHERE status='ACCEPTED'
  ),
  'acceptedPoints', (
    SELECT coalesce(sum(accepted_point_count), 0) FROM scoped_events
  ),
  'storedPoints', (SELECT count(*) FROM scoped_points),
  'properties', (SELECT count(DISTINCT property_id) FROM scoped_points),
  'rejects', (
    SELECT count(*) FROM bpi.bpi_telemetry_point_rejects reject
    WHERE reject.tenant_id=${sqlLiteral(bpiScope.tenantId)}
      AND reject.created_at BETWEEN
        timestamptz ${sqlLiteral(bpiScope.from)}
        AND timestamptz ${sqlLiteral(bpiScope.to)}
  ),
  'quarantine', (
    SELECT count(*) FROM bpi.bpi_telemetry_quarantine quarantine
    WHERE quarantine.tenant_id=${sqlLiteral(bpiScope.tenantId)}
      AND quarantine.created_at BETWEEN
        timestamptz ${sqlLiteral(bpiScope.from)}
        AND timestamptz ${sqlLiteral(bpiScope.to)}
  ),
  'series', COALESCE((
    SELECT json_agg(json_build_object(
      'propertyId', property_id, 'samples', samples,
      'minimum', minimum, 'average', average, 'maximum', maximum
    ) ORDER BY property_id)
    FROM (
      SELECT property_id, count(*) AS samples,
             round(min(numeric_value)::numeric, 3) AS minimum,
             round(avg(numeric_value)::numeric, 3) AS average,
             round(max(numeric_value)::numeric, 3) AS maximum
      FROM scoped_points
      WHERE property_id IN (
        'jet.flow.instant', 'jet.feed.baume',
        'saccharification.inlet.flow.instant',
        'saccharification.liquor.baume'
      )
      GROUP BY property_id
    ) statistics
  ), '[]'::json)
);`;
}

function assertDatabaseState(state, bpiState, productionModel) {
  const failures = [];
  const task = state.task || {};
  const finalQuantity = productionModel.processes.find(
    (item) => item.code === "SACCHARIFICATION"
  ).outputQuantity;
  if (
    String(task.lineId) !== bpiWomLineId
    || Number(task.planNum) !== finalQuantity
    || Number(task.finishNum) !== finalQuantity
    || task.state !== "WOM_runState/finished"
  ) {
    failures.push(`WOM task final state mismatch: ${JSON.stringify(task)}`);
  }
  if (
    state.processes.length !== 2
    || state.processes[0].name !== "喷射液化"
    || state.processes[1].name !== "糖化"
    || state.processes.some((item) => item.process_run_state !== "WOM_runState/finished")
    || Number(state.handoverSeconds) !== 12
  ) {
    failures.push(`Two-process boundary mismatch: ${JSON.stringify(state.processes)}`);
  }
  if (
    state.materials.length !== 3
    || state.batches.length !== 3
    || !state.unit
    || state.unit.name !== "吨"
    || state.unit.symbol !== "t"
    || state.materials.some(
      (item) => item.mainUnitName !== "吨" || item.produceUnitName !== "吨"
    )
  ) {
    failures.push(`Material or batch materialization mismatch`);
  }
  if (
    state.lineage.length !== 2
    || Number(state.lineage[0].inputQuantity) !== 34.4
    || Number(state.lineage[0].outputQuantity) !== 34.8
    || Number(state.lineage[1].inputQuantity) !== 34.8
    || Number(state.lineage[1].outputQuantity) !== 35.1
  ) {
    failures.push(`Batch lineage quantity mismatch: ${JSON.stringify(state.lineage)}`);
  }
  if (
    state.equipmentActivities.length !== 4
    || state.equipmentActivities.some(
      (item) => item.materialId !== null || item.batchNo !== null
    )
  ) {
    failures.push(`Equipment activities were materialized as materials`);
  }
  const itemCounts = state.qualityStandards.map((item) => Number(item.itemCount)).sort();
  if (
    state.qualityStandards.length !== 3
    || JSON.stringify(itemCounts) !== JSON.stringify([2, 3, 4])
    || state.qualityStandards.some((item) => !item.definition.includes("TEST_ONLY_DRAFT=true"))
    || Number(state.handoverCheckCount) !== 5
    || state.finalQcsItems.length !== 4
    || state.finalQcsItems.some((item) => item.result !== "合格")
  ) {
    failures.push(`Quality definition or execution mismatch`);
  }
  const stock = state.wms && state.wms.stock;
  if (
    Number(state.wms && state.wms.documentCount) !== 1
    || Number(state.wms && state.wms.transactionCount) !== 2
    || !stock
    || Number(stock.onHand) !== finalQuantity
    || Number(stock.available) !== finalQuantity
    || Number(stock.hold) !== 0
  ) {
    failures.push(`WMS completion stock mismatch: ${JSON.stringify(state.wms)}`);
  }
  if (
    Number(bpiState.events) !== 24
    || Number(bpiState.acceptedEvents) !== 24
    || Number(bpiState.acceptedPoints) !== 288
    || Number(bpiState.storedPoints) !== 288
    || Number(bpiState.properties) !== 12
    || Number(bpiState.rejects) !== 0
    || Number(bpiState.quarantine) !== 0
  ) {
    failures.push(`BPI evidence mismatch: ${JSON.stringify(bpiState)}`);
  }
  if (failures.length) {
    throw new Error(failures.join("; "));
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
  ];
  return candidates.find((value) => typeof value === "string" && value.length > 20);
}

async function readBody(response) {
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
    const response = await api.post(`${baseUrl}/inter-api/auth/login`, {
      data: body,
      headers: {
        Accept: "application/json, text/plain, */*",
        "Content-Type": "application/json;charset=UTF-8",
      },
    });
    const parsed = await readBody(response);
    const ticket = response.ok() ? findTicket(parsed.json) : null;
    if (ticket) {
      return { ticket, status: response.status() };
    }
    errors.push({ status: response.status(), body: parsed.text.slice(0, 400) });
  }
  throw new Error(`Login failed: ${JSON.stringify(errors)}`);
}

async function apiGet(api, ticket, route, tenant = tenantId) {
  const response = await api.get(`${baseUrl}${route}`, {
    headers: {
      Authorization: `Bearer ${ticket}`,
      "X-Tenant-Id": tenant,
      Accept: "application/json",
    },
  });
  const parsed = await readBody(response);
  return {
    method: "GET",
    url: route,
    tenant,
    status: response.status(),
    body: parsed.json || parsed.text,
  };
}

async function apiPost(api, ticket, route, payload, tenant = tenantId) {
  const response = await api.post(`${baseUrl}${route}`, {
    data: payload,
    headers: {
      Authorization: `Bearer ${ticket}`,
      "X-Tenant-Id": tenant,
      Accept: "application/json",
      "Content-Type": "application/json;charset=UTF-8",
    },
  });
  const parsed = await readBody(response);
  return {
    method: "POST",
    url: route,
    tenant,
    requestPayload: payload,
    status: response.status(),
    body: parsed.json || parsed.text,
  };
}

function responseSucceeded(result) {
  return result.status === 200
    && result.body
    && (result.body.code === 200 || result.body.success === true);
}

async function rebuildWmsAndTrace({
  api,
  ticket,
  taskId,
  tableNo,
  batchNo,
  finalMaterialCode,
  finalQuantity,
  preflight,
  ids,
}) {
  const requests = [];
  const sourceDocumentId = `${marker}_WMS_IN`;
  const sourceLineId = `${marker}_WMS_LINE`;
  const inboundPayload = {
    srcID: sourceDocumentId,
    srcTableNo: `${marker}_WMS_IN_NO`,
    directiveNo: tableNo,
    companyCode: tenantId,
    deptCode: "ADP_E2E",
    staffCode: "ADP_E2E",
    userName: username,
    wareCode: `${marker}_WARE`,
    storageDate: "2026-07-27",
    comeType: "produceIn",
    redBlue: "blue",
    detailList: [{
      srcPartId: sourceLineId,
      goodCode: finalMaterialCode,
      batchText: batchNo,
      produceBatchNum: batchNo,
      placeSetCode: `${marker}_LOC`,
      quantity: finalQuantity,
      unitCode: "t",
      productionDate: "2026-07-27",
      memo: `${marker};TEST_ONLY_DRAFT=true`,
    }],
  };
  const inbound = await apiPost(
    api,
    ticket,
    "/msService/public/material/produceInSingles/produceInSingl/generateProductInSingle",
    inboundPayload
  );
  requests.push(inbound);
  assertCondition(responseSucceeded(inbound), "Fructose pilot completion inbound API failed");

  const qualityRelease = await apiPost(
    api,
    ticket,
    `/msService/material/foreign/foreign/checkProdResult?srcId=${encodeURIComponent(sourceLineId)}&checkResult=${encodeURIComponent("BaseSet_checkResult/qualified")}`,
    {}
  );
  requests.push(qualityRelease);
  assertCondition(responseSucceeded(qualityRelease), "Fructose pilot WMS quality release API failed");

  const trace = await apiGet(
    api,
    ticket,
    `/msService/ProcessAnalysis/processAnalysis/api/trace?batchNo=${encodeURIComponent(batchNo)}&productNo=${encodeURIComponent(finalMaterialCode)}`
  );
  requests.push(trace);
  assertCondition(responseSucceeded(trace), "Fructose pilot trace API failed");
  assertCondition(
    trace.body.data
      && trace.body.data.task
      && String(trace.body.data.task.id) === String(taskId)
      && Number(trace.body.data.summary.processCount) >= 2
      && Number(trace.body.data.summary.qualityEventCount) >= 1
      && Number(trace.body.data.summary.inventoryEventCount) >= 2,
    `Fructose pilot trace aggregation is incomplete: ${JSON.stringify(trace.body).slice(0, 3000)}`
  );
  const traceLineage = trace.body.data.materials && trace.body.data.materials.lineage;
  assertCondition(
    Array.isArray(traceLineage)
      && traceLineage.length === 2
      && traceLineage[0].fromBatch === `${marker}_STARCH_SLURRY_BATCH`
      && traceLineage[0].toBatch === `${marker}_LIQUEFIED_SYRUP_BATCH`
      && traceLineage[0].processName === "喷射液化"
      && Number(traceLineage[0].inputQuantity) === 34.4
      && Number(traceLineage[0].outputQuantity) === 34.8
      && traceLineage[1].fromBatch === `${marker}_LIQUEFIED_SYRUP_BATCH`
      && traceLineage[1].toBatch === batchNo
      && traceLineage[1].processName === "糖化"
      && Number(traceLineage[1].inputQuantity) === 34.8
      && Number(traceLineage[1].outputQuantity) === 35.1,
    `Fructose pilot process lineage is incorrect: ${JSON.stringify(traceLineage)}`
  );

  const snapshotRoutes = [
    `/msService/ProcessAnalysis/paramDetail/paramDetail/analysisiTask?taskExeLogId=${preflight.task.taskExecutionId}`,
    `/msService/ProcessAnalysis/paramStatRec/paramStatRec/manualStatProcess?processId=${preflight.firstProcessExecution.id}`,
    `/msService/ProcessAnalysis/paramStatRec/paramStatRec/manualStatProcess?processId=${ids.sacchExecution}`,
    `/msService/ProcessAnalysis/paramStatRec/paramStatRec/manualStatActive?activeId=${preflight.inputActivity.executionId}`,
    `/msService/ProcessAnalysis/paramStatRec/paramStatRec/manualStatActive?activeId=${ids.holdExecution}`,
  ];
  for (const route of snapshotRoutes) {
    const snapshot = await apiGet(api, ticket, route);
    requests.push(snapshot);
    assertCondition(responseSucceeded(snapshot), `Trace snapshot API failed: ${route}`);
  }

  const firstProcess = await apiGet(
    api,
    ticket,
    `/msService/ProcessAnalysis/processAnalysis/api/process-executions/${preflight.firstProcessExecution.id}`,
    bpiScope.tenantId
  );
  requests.push(firstProcess);
  assertCondition(responseSucceeded(firstProcess), "Jet process detail API failed");
  assertCondition(
    firstProcess.body.data.processExecution.name === "喷射液化"
      && firstProcess.body.data.nextProcess
      && firstProcess.body.data.nextProcess.name === "糖化"
      && Number(firstProcess.body.data.handover.currentToNext.gapSeconds) === 12,
    `Jet-to-saccharification boundary API mismatch: ${JSON.stringify(firstProcess.body).slice(0, 2500)}`
  );

  const secondProcess = await apiGet(
    api,
    ticket,
    `/msService/ProcessAnalysis/processAnalysis/api/process-executions/${ids.sacchExecution}`,
    bpiScope.tenantId
  );
  requests.push(secondProcess);
  assertCondition(responseSucceeded(secondProcess), "Saccharification process detail API failed");
  assertCondition(
    secondProcess.body.data.processExecution.name === "糖化"
      && secondProcess.body.data.previousProcess
      && secondProcess.body.data.previousProcess.name === "喷射液化",
    `Saccharification previous-process API mismatch: ${JSON.stringify(secondProcess.body).slice(0, 2500)}`
  );

  return {
    requests,
    trace: trace.body.data,
    processDetails: {
      jet: firstProcess.body.data,
      saccharification: secondProcess.body.data,
    },
  };
}

async function inspectBrowserPage(page, name, route, expectedTexts) {
  const consoleErrors = [];
  const pageErrors = [];
  const badResponses = [];
  const requestFailures = [];
  page.on("console", (message) => {
    if (message.type() === "error") consoleErrors.push(message.text());
  });
  page.on("pageerror", (error) => pageErrors.push(error.message));
  page.on("response", (response) => {
    if (response.status() >= 400) {
      badResponses.push({
        url: response.url(),
        status: response.status(),
        method: response.request().method(),
      });
    }
  });
  page.on("requestfailed", (requestValue) => {
    requestFailures.push({
      url: requestValue.url(),
      error: requestValue.failure() && requestValue.failure().errorText,
    });
  });
  const navigation = await page.goto(`${browserBaseUrl}${route}`, {
    waitUntil: "domcontentloaded",
    timeout: 180000,
  });
  await page.waitForTimeout(name === "wom-task" ? 8000 : 3000);
  const bodyText = await page.locator("body").innerText();
  const expected = expectedTexts.map((text) => ({
    text,
    visible: bodyText.includes(text),
  }));
  const screenshot = path.join(screenshotDir, `${name}.png`);
  await page.screenshot({ path: screenshot, fullPage: true });
  return {
    name,
    route,
    navigationStatus: navigation && navigation.status(),
    expected,
    bodyTextExcerpt: bodyText.slice(0, 2500),
    consoleErrors,
    pageErrors,
    badResponses,
    requestFailures,
    screenshot,
  };
}

async function browserAcceptance({ ticket, taskId, batchNo, preflight, ids }) {
  ensureDirectory(screenshotDir);
  const launchOptions = { headless };
  if (browserExecutable) launchOptions.executablePath = browserExecutable;
  const browser = await chromium.launch(launchOptions);
  try {
    const context = await browser.newContext({
      viewport: { width: 1600, height: 1000 },
      extraHTTPHeaders: {
        Authorization: `Bearer ${ticket}`,
        "X-Tenant-Id": bpiScope.tenantId,
      },
    });
    const pages = [];
    pages.push(await inspectBrowserPage(
      await context.newPage(),
      "wom-task",
      `/msService/WOM/produceTask/produceTask/makeTaskEdit?id=${taskId}`,
      ["产品编码", "生产批号", "工序活动", "用料汇总", "检验清单", "吨"]
    ));
    pages.push(await inspectBrowserPage(
      await context.newPage(),
      "jet-process-detail",
      `/msService/ProcessAnalysis/processAnalysis/processExecution/detail?processExecutionId=${preflight.firstProcessExecution.id}`,
      ["喷射液化", "糖化", "12 秒", "瞬时流量", "波美值"]
    ));
    pages.push(await inspectBrowserPage(
      await context.newPage(),
      "saccharification-process-detail",
      `/msService/ProcessAnalysis/processAnalysis/processExecution/detail?processExecutionId=${ids.sacchExecution}`,
      ["喷射液化", "糖化", "瞬时流量", "波美值"]
    ));
    pages.push(await inspectBrowserPage(
      await context.newPage(),
      "qcs-report",
      `/msService/QCS/inspectReport/inspectReport/manuInspReportView?id=${preflight.qcs.reportId}`,
      ["糖化液波美值", "糖化液 pH", "糖化 DE 值", "糖化液干物含量", "吨"]
    ));
    pages.push(await inspectBrowserPage(
      await context.newPage(),
      "batch-trace",
      `/msService/ProcessAnalysis/processAnalysis/exelogSecond/processBatchViewOut?batchNo=${encodeURIComponent(batchNo)}&productNo=${encodeURIComponent(`${marker}_SACCHARIFIED_LIQUOR`)}`,
      [
        "生产过程追溯",
        batchNo,
        `${marker}_STARCH_SLURRY_BATCH`,
        `${marker}_LIQUEFIED_SYRUP_BATCH`,
        "喷射液化",
        "糖化",
        "COMPLETION_INBOUND",
        "QUALITY_RELEASE",
      ]
    ));

    const ignoredFailures = [
      /favicon/i,
      /sockjs/i,
      /websocket/i,
      /hot-update/i,
    ];
    const failures = [];
    for (const page of pages) {
      if (page.navigationStatus !== 200) {
        failures.push(`${page.name} navigation=${page.navigationStatus}`);
      }
      const missingTexts = page.expected.filter((item) => !item.visible).map((item) => item.text);
      if (missingTexts.length) {
        failures.push(`${page.name} missing=${missingTexts.join(",")}`);
      }
      const meaningfulBadResponses = page.badResponses.filter(
        (entry) => !ignoredFailures.some((pattern) => pattern.test(entry.url))
      );
      const meaningfulRequestFailures = page.requestFailures.filter(
        (entry) => !ignoredFailures.some((pattern) => pattern.test(entry.url))
      );
      if (
        page.consoleErrors.length
        || page.pageErrors.length
        || meaningfulBadResponses.length
        || meaningfulRequestFailures.length
      ) {
        failures.push(
          `${page.name} frontend errors=${JSON.stringify({
            consoleErrors: page.consoleErrors,
            pageErrors: page.pageErrors,
            badResponses: meaningfulBadResponses,
            requestFailures: meaningfulRequestFailures,
          }).slice(0, 3000)}`
        );
      }
    }
    assertCondition(failures.length === 0, failures.join("; "));
    return {
      status: "PASS",
      pages,
      summary: {
        pageCount: pages.length,
        consoleErrors: pages.reduce((sum, page) => sum + page.consoleErrors.length, 0),
        pageErrors: pages.reduce((sum, page) => sum + page.pageErrors.length, 0),
        badResponses: pages.reduce((sum, page) => sum + page.badResponses.length, 0),
        requestFailures: pages.reduce((sum, page) => sum + page.requestFailures.length, 0),
      },
    };
  } finally {
    await browser.close();
  }
}

async function main() {
  if (process.env.ADP_FRUCTOSE_PILOT_CONFIRM !== "YES") {
    throw new Error(
      "Set ADP_FRUCTOSE_PILOT_CONFIRM=YES before enriching the retained test-only fructose fixture"
    );
  }
  const { scenario, fullFlow } = loadInputs();
  const productionModel = scenario.productionModel;
  const taskId = String(fullFlow.identity.taskId);
  const finalMaterialId = String(fullFlow.identity.materialId);
  const formulaId = String(fullFlow.identity.formulaId);
  const batchNo = fullFlow.identity.batchNo;
  const tableNo = fullFlow.identity.tableNo;
  const ids = fixtureIds(taskId);
  const finalProcess = productionModel.processes.find(
    (item) => item.code === "SACCHARIFICATION"
  );
  const finalMaterialCode = `${marker}_SACCHARIFIED_LIQUOR`;
  const evidence = {
    generatedAt: new Date().toISOString(),
    repoCommit: gitCommit(),
    database: "PostgreSQL",
    marker,
    status: "FAIL",
    testOnlyDraft: true,
    scenario: {
      path: path.relative(repoRoot, scenarioPath),
      code: scenario.scenarioCode,
      routeCode: productionModel.routeCode,
      routeName: productionModel.routeName,
      modelStatus: productionModel.status,
    },
    sourceEvidence: {
      path: fullFlowEvidencePath,
      status: fullFlow.status,
      retained: fullFlow.retainedFixture.retained,
    },
    identity: {
      taskId,
      tableNo,
      batchNo,
      finalMaterialId,
      finalMaterialCode,
      ids,
    },
    preflight: null,
    api: null,
    databaseState: null,
    bpiState: null,
    browser: null,
    verificationSql: null,
    bpiVerificationSql: bpiVerificationSql(),
    issues: [],
  };
  writeEvidence(evidence);

  const api = await request.newContext({ timeout: 90000 });
  try {
    const preflight = JSON.parse(runSql(preflightSql(taskId, batchNo)));
    assertPreflight(preflight, fullFlow);
    evidence.preflight = preflight;
    writeEvidence(evidence);

    runSql(seedSql({
      productionModel,
      preflight,
      ids,
      taskId,
      finalMaterialId,
      formulaId,
      batchNo,
    }));

    const resetState = parseLastJsonLine(runSql(
      resetMarkerWmsSql(taskId, batchNo, finalMaterialCode)
    ));
    assertCondition(
      Object.values(resetState).every((value) => Number(value) === 0),
      `Marker WMS reset was incomplete: ${JSON.stringify(resetState)}`
    );

    const loginResult = await login(api);
    evidence.login = { status: loginResult.status };
    evidence.api = await rebuildWmsAndTrace({
      api,
      ticket: loginResult.ticket,
      taskId,
      tableNo,
      batchNo,
      finalMaterialCode,
      finalQuantity: finalProcess.outputQuantity,
      preflight,
      ids,
    });

    const sql = verificationSql({
      taskId,
      batchNo,
      ids,
      preflight,
      productionModel,
    });
    evidence.verificationSql = sql;
    evidence.databaseState = JSON.parse(runSql(sql));
    evidence.bpiState = JSON.parse(runSql(bpiVerificationSql(), bpiDbName));
    assertDatabaseState(evidence.databaseState, evidence.bpiState, productionModel);

    evidence.browser = await browserAcceptance({
      ticket: loginResult.ticket,
      taskId,
      batchNo,
      preflight,
      ids,
    });
    assertCondition(
      Number(evidence.databaseState.traceSnapshotCount) >= 5,
      "ProcessAnalysis trace snapshots were not persisted"
    );

    evidence.summary = {
      processes: evidence.databaseState.processes.length,
      materials: evidence.databaseState.materials.length,
      batches: evidence.databaseState.batches.length,
      lineageEdges: evidence.databaseState.lineage.length,
      equipmentActivitiesWithoutMaterial:
        evidence.databaseState.equipmentActivities.length,
      qualityStandards: evidence.databaseState.qualityStandards.length,
      qualityItems: evidence.databaseState.qualityStandards.reduce(
        (sum, item) => sum + Number(item.itemCount),
        0
      ),
      handoverChecks: Number(evidence.databaseState.handoverCheckCount),
      finalQcsItems: evidence.databaseState.finalQcsItems.length,
      bpiEvents: Number(evidence.bpiState.events),
      bpiPoints: Number(evidence.bpiState.storedPoints),
      browserPages: evidence.browser.summary.pageCount,
    };
    evidence.status = "PASS";
  } catch (error) {
    evidence.issues.push(error.stack || error.message);
  } finally {
    writeEvidence(evidence);
    await api.dispose();
  }
  console.log(`${evidence.status}: ${outputPath}`);
  if (evidence.status !== "PASS") {
    process.exitCode = 1;
  }
}

main().catch((error) => {
  ensureDirectory(path.dirname(outputPath));
  fs.writeFileSync(
    outputPath,
    `${JSON.stringify({
      generatedAt: new Date().toISOString(),
      marker,
      status: "FAIL",
      issues: [error.stack || error.message],
    }, null, 2)}\n`,
    "utf8"
  );
  console.error(error);
  process.exitCode = 1;
});
