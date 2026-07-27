#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { execFileSync, spawnSync } = require("child_process");
const { createHash, randomBytes } = require("crypto");

const baseUrl = (process.env.ADP_BASE_URL || "http://10.11.100.17:18080").replace(/\/+$/, "");
const browserBaseUrl = (process.env.ADP_BROWSER_BASE_URL || baseUrl).replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD || "123456";
const dbSshTarget = process.env.ADP_DB_SSH_TARGET || "v6@10.11.100.17";
const dbSshPassword = process.env.ADP_DB_SSH_PASSWORD || "";
const dbContainer = process.env.ADP_DB_CONTAINER || "adp-mes-newbase-postgres-1";
const dbName = process.env.ADP_DB_NAME || "adp";
const dbUser = process.env.ADP_DB_USER || "adp";
const bpiDbName = process.env.BPI_DB_NAME || "ft_mes_bpi";
const nowToken = new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 17);
const resume = process.env.ADP_MES_FULL_FLOW_RESUME === "true";
const configuredOutputPath = String(process.env.ADP_MES_FULL_FLOW_OUTPUT || "").trim();
if (resume && !configuredOutputPath) {
  throw new Error("ADP_MES_FULL_FLOW_RESUME=true requires ADP_MES_FULL_FLOW_OUTPUT");
}
const resumeEvidence =
  resume && configuredOutputPath && fs.existsSync(configuredOutputPath)
    ? JSON.parse(fs.readFileSync(configuredOutputPath, "utf8"))
    : null;
if (resume && !resumeEvidence) {
  throw new Error(`Resume evidence does not exist: ${configuredOutputPath}`);
}
const marker =
  process.env.ADP_E2E_MARKER ||
  (resumeEvidence && resumeEvidence.marker) ||
  `MES_FULL_${nowToken}_${randomBytes(3).toString("hex")}`;
const previousStepOutput =
  resumeEvidence &&
  Array.isArray(resumeEvidence.steps) &&
  resumeEvidence.steps.find((step) => path.isAbsolute(String(step.outputFile || "")));
const resumeOutputDir = previousStepOutput
  ? path.dirname(path.dirname(previousStepOutput.outputFile))
  : configuredOutputPath
    ? path.dirname(configuredOutputPath)
    : "";
const outputDir =
  process.env.ADP_MES_FULL_FLOW_OUTPUT_DIR ||
  resumeOutputDir ||
  path.join("/tmp", `adp-mes-full-production-flow-${nowToken}`);
const outputPath =
  configuredOutputPath ||
  path.join(outputDir, "mes-full-production-flow-acceptance.json");
const bpiBatchId = String(process.env.ADP_MES_FULL_FLOW_BPI_BATCH_ID || "").trim();

function configuredId(name, fallback) {
  const value = String(process.env[name] || "").trim();
  if (value && !/^[1-9][0-9]{0,15}$/.test(value)) {
    throw new Error(`${name} must be a positive integer with at most 16 digits`);
  }
  const id = value ? BigInt(value) : fallback;
  if (id > BigInt(Number.MAX_SAFE_INTEGER)) {
    throw new Error(`${name} must stay within JavaScript's safe integer range`);
  }
  return id;
}

function deterministicOffset(value, modulus) {
  const hashPrefix = createHash("sha256").update(value).digest("hex").slice(0, 16);
  return BigInt(`0x${hashPrefix}`) % modulus;
}

const generatedBase =
  9007180000000000n +
  deterministicOffset(`${marker}:wom`, 9000000000n);
const taskId = configuredId("ADP_MES_FULL_FLOW_TASK_ID", generatedBase + 3n);
const materialId = configuredId("ADP_MES_FULL_FLOW_MATERIAL_ID", taskId - 2n);
const formulaId = configuredId("ADP_MES_FULL_FLOW_FORMULA_ID", taskId - 1n);
const startIdBase = taskId - 3n;
const lineId = configuredId("ADP_MES_FULL_FLOW_LINE_ID", startIdBase + 7n);
const putinIdBase = configuredId("ADP_MES_FULL_FLOW_PUTIN_ID_BASE", taskId + 1000n);
const workUnitId = configuredId("ADP_MES_FULL_FLOW_WORK_UNIT_ID", putinIdBase + 13n);
const processWaitId = configuredId("ADP_MES_FULL_FLOW_PROCESS_WAIT_ID", putinIdBase + 12n);
const outputIdBase = configuredId("ADP_MES_FULL_FLOW_OUTPUT_ID_BASE", taskId + 2000n);
const processEndIdBase = configuredId("ADP_MES_FULL_FLOW_PROCESS_END_ID_BASE", taskId + 2500n);
const qcsIdBase = configuredId("ADP_MES_FULL_FLOW_QCS_ID_BASE", taskId + 3000n);
const finishIdBase = configuredId(
  "ADP_MES_FULL_FLOW_FINISH_ID_BASE",
  8988000000000000n + deterministicOffset(`${marker}:finish`, 1000000000000n)
);
function idRange(base, count) {
  return Array.from({ length: count }, (_value, index) => base + BigInt(index));
}
const reservedIds = Array.from(
  new Set(
    [
      ...idRange(startIdBase, 24),
      ...idRange(putinIdBase, 32),
      ...idRange(outputIdBase, 32),
      ...idRange(processEndIdBase, 32),
      ...idRange(qcsIdBase, 48),
      ...idRange(finishIdBase, 24),
      lineId,
      workUnitId,
      processWaitId,
    ].map((value) => value.toString())
  )
);
const reservedIdSql = reservedIds.join(", ");
const tableNo = process.env.ADP_WOM_TABLE_NO || `${marker}_TASK_TN`;
const batchNo = process.env.ADP_WOM_BATCH_NO || `${marker}_BATCH`;
const materialCode = process.env.ADP_WOM_MATERIAL_CODE || `${marker}_MAT`;
const formulaCode = process.env.ADP_WOM_FORMULA_CODE || `${marker}_FORM`;
const tenantId = `${marker}_TENANT`;
const repoRoot = path.resolve(__dirname, "../../..");
const workspaceNodePath = path.join(repoRoot, "frontend", "apps", "bpi", "node_modules");

const scripts = {
  start: path.join(__dirname, "adp-wom-start-persistence-acceptance.js"),
  active: path.join(__dirname, "adp-wom-active-persistence-acceptance.js"),
  qcs: path.join(__dirname, "adp-qcs-report-chain-persistence-acceptance.js"),
  finish: path.join(__dirname, "adp-wom-start-persistence-acceptance.js"),
  trace: path.join(__dirname, "adp-process-analysis-persistence-acceptance.js"),
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

function runSql(sql, database = dbName) {
  const command = [
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
  return runRemote(command, sql).trim();
}

function gitCommit() {
  return execFileSync("git", ["rev-parse", "HEAD"], {
    cwd: repoRoot,
    encoding: "utf8",
  }).trim();
}

function writeEvidence(evidence) {
  ensureDir(path.dirname(outputPath));
  fs.writeFileSync(outputPath, `${JSON.stringify(evidence, null, 2)}\n`, "utf8");
}

function readJson(filePath) {
  return JSON.parse(fs.readFileSync(filePath, "utf8"));
}

function childEnvironment(stepDir, overrides = {}) {
  return {
    ...process.env,
    ADP_BASE_URL: baseUrl,
    ADP_BROWSER_BASE_URL: browserBaseUrl,
    ADP_USERNAME: username,
    ADP_PASSWORD: password,
    ADP_DB_SSH_TARGET: dbSshTarget,
    ADP_DB_SSH_PASSWORD: dbSshPassword,
    ADP_DB_CONTAINER: dbContainer,
    ADP_DB_NAME: dbName,
    ADP_DB_USER: dbUser,
    NODE_PATH: process.env.NODE_PATH || workspaceNodePath,
    ADP_E2E_MARKER: marker,
    ADP_OUTPUT_DIR: stepDir,
    ADP_WOM_TASK_ID: taskId.toString(),
    ADP_WOM_MATERIAL_ID: materialId.toString(),
    ADP_WOM_FORMULA_ID: formulaId.toString(),
    ADP_WOM_LINE_ID: lineId.toString(),
    ADP_WOM_WORK_UNIT_ID: workUnitId.toString(),
    ADP_WOM_TABLE_NO: tableNo,
    ADP_WOM_BATCH_NO: batchNo,
    ADP_WOM_MATERIAL_CODE: materialCode,
    ADP_WOM_FORMULA_CODE: formulaCode,
    ADP_PAGE_TIMEOUT_MS: process.env.ADP_PAGE_TIMEOUT_MS || "180000",
    ...overrides,
  };
}

function runStep(evidence, name, script, outputFile, overrides) {
  const stepDir = path.join(outputDir, name);
  ensureDir(stepDir);
  const startedAt = new Date().toISOString();
  const result = spawnSync(process.execPath, [script], {
    encoding: "utf8",
    env: childEnvironment(stepDir, overrides),
    maxBuffer: 50 * 1024 * 1024,
    timeout: Number(process.env.ADP_MES_FULL_FLOW_STEP_TIMEOUT_MS || 900000),
  });
  const step = {
    name,
    script: path.relative(repoRoot, script),
    startedAt,
    completedAt: new Date().toISOString(),
    exitCode: result.status,
    signal: result.signal || null,
    outputFile,
    stdoutTail: String(result.stdout || "").slice(-12000),
    stderrTail: String(result.stderr || "").slice(-12000),
  };
  evidence.steps.push(step);
  writeEvidence(evidence);
  if (result.error) {
    throw result.error;
  }
  if (result.status !== 0) {
    throw new Error(`${name} failed: ${step.stderrTail || step.stdoutTail}`);
  }
  const detail = readJson(outputFile);
  if (detail.status !== "PASS") {
    throw new Error(`${name} did not produce PASS evidence: ${JSON.stringify(detail).slice(0, 4000)}`);
  }
  step.status = "PASS";
  step.detail = {
    marker: detail.marker,
    operation: detail.operation || null,
    route: detail.route || null,
    api: detail.api || null,
    frontendClean: detail.frontendClean || null,
    persistence: detail.persistence || null,
    cleanup: detail.cleanup || null,
  };
  writeEvidence(evidence);
  return detail;
}

function preflightSql() {
  return `
SELECT json_build_object(
  'womTasks', (SELECT count(*) FROM public.wom_produce_tasks WHERE id=${taskId} OR table_no=${sqlLiteral(tableNo)}),
  'womTaskIds', COALESCE((
    SELECT json_agg(id ORDER BY id)
    FROM public.wom_produce_tasks
    WHERE id=${taskId} OR table_no=${sqlLiteral(tableNo)}
  ), '[]'::json),
  'reservedIdentityRows', (
    SELECT count(*)
    FROM (
      SELECT id FROM public.baseset_materials WHERE id IN (${reservedIdSql})
      UNION ALL SELECT id FROM public.rm_formulas WHERE id IN (${reservedIdSql})
      UNION ALL SELECT id FROM public.hm_factory_models WHERE id IN (${reservedIdSql})
      UNION ALL SELECT id FROM public.wom_produce_tasks WHERE id IN (${reservedIdSql})
      UNION ALL SELECT id FROM public.wom_task_processes WHERE id IN (${reservedIdSql})
      UNION ALL SELECT id FROM public.wom_task_actives WHERE id IN (${reservedIdSql})
      UNION ALL SELECT id FROM public.wom_produce_task_exelog WHERE id IN (${reservedIdSql})
      UNION ALL SELECT id FROM public.wom_process_exelogs WHERE id IN (${reservedIdSql})
      UNION ALL SELECT id FROM public.wom_proc_reports WHERE id IN (${reservedIdSql})
      UNION ALL SELECT id FROM public.wom_acti_exelogs WHERE id IN (${reservedIdSql})
      UNION ALL SELECT id FROM public.wom_wait_put_records WHERE id IN (${reservedIdSql})
      UNION ALL SELECT id FROM public.wom_putin_details WHERE id IN (${reservedIdSql})
      UNION ALL SELECT id FROM public.wom_output_details WHERE id IN (${reservedIdSql})
      UNION ALL SELECT id FROM public.wom_pro_check_details WHERE id IN (${reservedIdSql})
      UNION ALL SELECT id FROM public.qcs_inspects WHERE id IN (${reservedIdSql})
      UNION ALL SELECT id FROM public.qcs_inspect_reports WHERE id IN (${reservedIdSql})
      UNION ALL SELECT id FROM public.qcs_report_coms WHERE id IN (${reservedIdSql})
      UNION ALL SELECT id FROM public.baseset_batch_infos WHERE id IN (${reservedIdSql})
      UNION ALL SELECT id FROM public.wfm_task_pending WHERE id IN (${reservedIdSql})
      UNION ALL SELECT id FROM public.wms_stock_documents WHERE id IN (${reservedIdSql})
      UNION ALL SELECT id FROM public.wms_stock_document_lines WHERE id IN (${reservedIdSql})
      UNION ALL SELECT id FROM public.wms_inventory_transactions WHERE id IN (${reservedIdSql})
      UNION ALL SELECT id FROM public.wms_batch_stocks WHERE id IN (${reservedIdSql})
      UNION ALL SELECT id FROM public.pa_trace_snapshots WHERE id IN (${reservedIdSql})
    ) occupied
  ),
  'womRelatedRows', (
    SELECT
      (SELECT count(*) FROM public.wom_task_processes WHERE task_id=${taskId}) +
      (SELECT count(*) FROM public.wom_task_actives WHERE task_id=${taskId}) +
      (SELECT count(*) FROM public.wom_produce_task_exelog WHERE task_id=${taskId}) +
      (SELECT count(*) FROM public.wom_process_exelogs WHERE task_id=${taskId}) +
      (SELECT count(*) FROM public.wom_proc_reports WHERE task_id=${taskId}) +
      (SELECT count(*) FROM public.wom_acti_exelogs WHERE task_id=${taskId}) +
      (SELECT count(*) FROM public.wom_wait_put_records WHERE task_id=${taskId}) +
      (SELECT count(*) FROM public.wfm_task_pending WHERE table_info_id=${taskId})
  ),
  'masterIdentityRows', (
    (SELECT count(*) FROM public.baseset_materials WHERE id=${materialId} OR code=${sqlLiteral(materialCode)}) +
    (SELECT count(*) FROM public.rm_formulas WHERE id=${formulaId} OR formual_code=${sqlLiteral(formulaCode)}) +
    (SELECT count(*) FROM public.hm_factory_models
      WHERE id IN (${lineId}, ${workUnitId})
         OR code IN (${sqlLiteral(`${marker}_LINE`)}, ${sqlLiteral(`${marker}_WU`)}))
  ),
  'qcsInspects', (SELECT count(*) FROM public.qcs_inspects WHERE batch_code=${sqlLiteral(batchNo)}),
  'wmsDocuments', (SELECT count(*) FROM public.wms_stock_documents WHERE tenant_id=${sqlLiteral(tenantId)}),
  'wmsTransactions', (SELECT count(*) FROM public.wms_inventory_transactions WHERE tenant_id=${sqlLiteral(tenantId)}),
  'wmsStocks', (SELECT count(*) FROM public.wms_batch_stocks WHERE tenant_id=${sqlLiteral(tenantId)}),
  'wmsQualityResults', (SELECT count(*) FROM public.wms_quality_results WHERE tenant_id=${sqlLiteral(tenantId)}),
  'traceSnapshots', (SELECT count(*) FROM public.pa_trace_snapshots WHERE tenant_id=${sqlLiteral(tenantId)})
);`;
}

function bpiStateSql() {
  if (!bpiBatchId) {
    return "SELECT '{}'::json;";
  }
  return `
SELECT COALESCE((
  SELECT row_to_json(batch)
  FROM (
    SELECT id, batch_no, order_id, state, revision, is_shadow, start_time, end_time,
           quality_gate, wms_status
    FROM bpi.bpi_batch_instances
    WHERE id=${sqlLiteral(bpiBatchId)}::uuid
  ) batch
), '{}'::json);`;
}

function verificationSql() {
  return `
SELECT json_build_object(
  'task', (
    SELECT json_build_object(
      'id', id,
      'tableNo', table_no,
      'batchNo', produce_batch_num,
      'state', task_run_state,
      'finishNum', finish_num,
      'checkState', check_state,
      'checkResult', check_result,
      'actStartTime', act_start_time,
      'actEndTime', act_end_time
    )
    FROM public.wom_produce_tasks
    WHERE id=${taskId}
  ),
  'processCount', (SELECT count(*) FROM public.wom_task_processes WHERE task_id=${taskId} AND coalesce(valid,true)),
  'finishedProcessCount', (
    SELECT count(*) FROM public.wom_task_processes
    WHERE task_id=${taskId}
      AND coalesce(valid,true)
      AND process_run_state='WOM_runState/finished'
      AND act_start_time IS NOT NULL
      AND act_end_time IS NOT NULL
  ),
  'processExecutionCount', (
    SELECT count(*) FROM public.wom_process_exelogs
    WHERE task_id=${taskId} AND coalesce(valid,true)
  ),
  'finishedProcessExecutionCount', (
    SELECT count(*) FROM public.wom_process_exelogs
    WHERE task_id=${taskId}
      AND coalesce(valid,true)
      AND process_run_state='WOM_runState/finished'
      AND act_start_time IS NOT NULL
      AND act_end_time IS NOT NULL
  ),
  'activityCount', (SELECT count(*) FROM public.wom_task_actives WHERE task_id=${taskId} AND coalesce(valid,true)),
  'finishedActivityCount', (
    SELECT count(*) FROM public.wom_task_actives
    WHERE task_id=${taskId}
      AND coalesce(valid,true)
      AND is_finish=true
      AND run_state='WOM_runState/finished'
      AND act_end_time IS NOT NULL
  ),
  'activityExecutionCount', (
    SELECT count(*) FROM public.wom_acti_exelogs
    WHERE task_id=${taskId} AND coalesce(valid,true)
  ),
  'finishedActivityExecutionCount', (
    SELECT count(*) FROM public.wom_acti_exelogs
    WHERE task_id=${taskId}
      AND coalesce(valid,true)
      AND run_state='WOM_runState/finished'
      AND act_start_time IS NOT NULL
      AND act_end_time IS NOT NULL
  ),
  'unfinishedWaitCount', (
    SELECT count(*) FROM public.wom_wait_put_records
    WHERE task_id=${taskId}
      AND coalesce(valid,true)
      AND coalesce(exe_state, '') <> 'WOM_runState/finished'
  ),
  'inputDetailCount', (
    SELECT count(*)
    FROM public.wom_putin_details d
    JOIN public.wom_proc_reports r ON r.id=d.head_id
    WHERE r.task_id=${taskId} AND d.material_batch_num=${sqlLiteral(batchNo)} AND coalesce(d.valid,true)
  ),
  'materialConsumptionCount', (
    SELECT count(*)
    FROM public.wom_mat_consum_recods c
    JOIN public.wom_putin_details d ON d.id=c.put_mat_detail_id
    JOIN public.wom_proc_reports r ON r.id=d.head_id
    WHERE r.task_id=${taskId}
      AND d.material_batch_num=${sqlLiteral(batchNo)}
      AND coalesce(c.valid,true)
      AND c.putin_num IS NOT NULL
  ),
  'outputDetailCount', (
    SELECT count(*)
    FROM public.wom_output_details d
    JOIN public.wom_proc_reports r ON r.id=d.head_id
    WHERE r.task_id=${taskId} AND d.material_batch_num=${sqlLiteral(batchNo)} AND coalesce(d.valid,true)
  ),
  'materialOutputRecordCount', (
    SELECT count(*)
    FROM public.wom_mat_outpt_records o
    JOIN public.wom_output_details d ON d.id=o.out_mat_detail_id
    JOIN public.wom_proc_reports r ON r.id=d.head_id
    WHERE r.task_id=${taskId}
      AND d.material_batch_num=${sqlLiteral(batchNo)}
      AND coalesce(o.valid,true)
      AND coalesce(d.valid,true)
      AND o.output_num IS NOT NULL
  ),
  'inspectionCount', (
    SELECT count(*) FROM public.qcs_inspects
    WHERE source_table_id=${taskId} AND batch_code=${sqlLiteral(batchNo)}
  ),
  'effectiveQualifiedReportCount', (
    SELECT count(*)
    FROM public.qcs_inspect_reports r
    JOIN public.qcs_inspects i ON i.id=r.inspect_id
    WHERE i.source_table_id=${taskId}
      AND i.batch_code=${sqlLiteral(batchNo)}
      AND r.status=99
      AND r.check_result IN ('合格', 'Qualified', 'BaseSet_checkResult/qualified')
  ),
  'qualifiedBatchCount', (
    SELECT count(*) FROM public.baseset_batch_infos
    WHERE batch_num=${sqlLiteral(batchNo)}
      AND check_result='BaseSet_checkResult/qualified'
      AND is_available=true
  ),
  'activePendingCount', (
    SELECT count(*) FROM public.wfm_task_pending
    WHERE table_info_id IN (
      SELECT table_info_id FROM public.qcs_inspects WHERE source_table_id=${taskId}
      UNION
      SELECT r.table_info_id
      FROM public.qcs_inspect_reports r
      JOIN public.qcs_inspects i ON i.id=r.inspect_id
      WHERE i.source_table_id=${taskId}
    )
    AND task_status IN (1,88)
  ),
  'wmsDocumentCount', (SELECT count(*) FROM public.wms_stock_documents WHERE tenant_id=${sqlLiteral(tenantId)}),
  'wmsTransactionCount', (SELECT count(*) FROM public.wms_inventory_transactions WHERE tenant_id=${sqlLiteral(tenantId)}),
  'wmsStockCount', (SELECT count(*) FROM public.wms_batch_stocks WHERE tenant_id=${sqlLiteral(tenantId)}),
  'traceSnapshotCount', (SELECT count(*) FROM public.pa_trace_snapshots WHERE tenant_id=${sqlLiteral(tenantId)}),
  'traceBatchSnapshotCount', (
    SELECT count(*) FROM public.pa_trace_snapshots
    WHERE tenant_id=${sqlLiteral(tenantId)} AND batch_no=${sqlLiteral(batchNo)}
  )
);`;
}

function assertPreflight(preflight, bpiBefore) {
  if (resume) {
    if (![0, 1].includes(Number(preflight.womTasks || 0))) {
      throw new Error(`Resume requires zero or one shared WOM task: ${JSON.stringify(preflight)}`);
    }
    const existingTaskIds = Array.isArray(preflight.womTaskIds)
      ? preflight.womTaskIds.map((id) => String(id))
      : [];
    if (existingTaskIds.length === 1 && existingTaskIds[0] !== taskId.toString()) {
      throw new Error(
        `Resume identity mismatch: expected task ${taskId}, found ${existingTaskIds[0]} for ${tableNo}`
      );
    }
  } else {
    const residuals = [
      "womTasks",
      "reservedIdentityRows",
      "womRelatedRows",
      "masterIdentityRows",
      "qcsInspects",
      "wmsDocuments",
      "wmsTransactions",
      "wmsStocks",
      "wmsQualityResults",
      "traceSnapshots",
    ]
      .filter((key) => Number(preflight[key] || 0) !== 0);
    if (residuals.length) {
      throw new Error(`Target identity is not clean before acceptance: ${JSON.stringify(preflight)}`);
    }
  }
  if (bpiBatchId) {
    if (!bpiBefore.id) {
      throw new Error(`BPI source batch ${bpiBatchId} does not exist`);
    }
    if (
      bpiBefore.order_id !== tableNo ||
      bpiBefore.batch_no !== batchNo ||
      bpiBefore.state !== "CLOSED_RAW" ||
      bpiBefore.is_shadow !== true
    ) {
      throw new Error(`BPI source identity does not match the protected shadow boundary: ${JSON.stringify(bpiBefore)}`);
    }
  }
}

function assertFinal(finalState, bpiBefore, bpiAfter) {
  const failures = [];
  const task = finalState.task || {};
  if (
    String(task.id || "") !== taskId.toString() ||
    task.tableNo !== tableNo ||
    task.batchNo !== batchNo ||
    task.state !== "WOM_runState/finished" ||
    !task.actStartTime ||
    !task.actEndTime
  ) {
    failures.push(`WOM task is not the expected finished identity: ${JSON.stringify(task)}`);
  }
  if (!["合格", "Qualified", "BaseSet_checkResult/qualified"].includes(String(task.checkResult || ""))) {
    failures.push(`WOM task quality result is not qualified: ${JSON.stringify(task)}`);
  }
  if (!["已检", "Inspected", "WOM_checkState/inspected"].includes(String(task.checkState || ""))) {
    failures.push(`WOM task check state is not inspected: ${JSON.stringify(task)}`);
  }
  if (Number(task.finishNum) !== 5) {
    failures.push(`WOM task finish quantity expected 5, got ${task.finishNum}`);
  }
  const expectedCounts = {
    processCount: 1,
    finishedProcessCount: 1,
    processExecutionCount: 1,
    finishedProcessExecutionCount: 1,
    activityCount: 2,
    finishedActivityCount: 2,
    activityExecutionCount: 2,
    finishedActivityExecutionCount: 2,
    inputDetailCount: 1,
    materialConsumptionCount: 1,
    outputDetailCount: 2,
    materialOutputRecordCount: 2,
    inspectionCount: 1,
    effectiveQualifiedReportCount: 1,
    qualifiedBatchCount: 1,
    wmsDocumentCount: 1,
    wmsTransactionCount: 2,
    wmsStockCount: 1,
    traceSnapshotCount: 3,
    traceBatchSnapshotCount: 3,
  };
  for (const [field, expected] of Object.entries(expectedCounts)) {
    if (Number(finalState[field] || 0) !== expected) {
      failures.push(`${field} expected ${expected}, got ${finalState[field]}`);
    }
  }
  if (Number(finalState.activePendingCount || 0) !== 0) {
    failures.push(`QCS pending workflow rows remain active: ${finalState.activePendingCount}`);
  }
  if (Number(finalState.finishedProcessCount || 0) !== Number(finalState.processCount || 0)) {
    failures.push(
      `Not every WOM process is finished: ${finalState.finishedProcessCount}/${finalState.processCount}`
    );
  }
  if (
    Number(finalState.finishedProcessExecutionCount || 0) !==
    Number(finalState.processExecutionCount || 0)
  ) {
    failures.push(
      `Not every WOM process execution is finished: ${finalState.finishedProcessExecutionCount}/${finalState.processExecutionCount}`
    );
  }
  if (Number(finalState.finishedActivityCount || 0) !== Number(finalState.activityCount || 0)) {
    failures.push(
      `Not every WOM activity is finished: ${finalState.finishedActivityCount}/${finalState.activityCount}`
    );
  }
  if (
    Number(finalState.finishedActivityExecutionCount || 0) !==
    Number(finalState.activityExecutionCount || 0)
  ) {
    failures.push(
      `Not every WOM activity execution is finished: ${finalState.finishedActivityExecutionCount}/${finalState.activityExecutionCount}`
    );
  }
  if (Number(finalState.unfinishedWaitCount || 0) !== 0) {
    failures.push(`WOM unfinished wait records remain: ${finalState.unfinishedWaitCount}`);
  }
  if (bpiBatchId && JSON.stringify(bpiBefore) !== JSON.stringify(bpiAfter)) {
    failures.push(`Protected BPI shadow batch changed during MES completion: before=${JSON.stringify(bpiBefore)} after=${JSON.stringify(bpiAfter)}`);
  }
  if (failures.length) {
    throw new Error(failures.join("; "));
  }
}

function assertResumeIdentity(previousEvidence) {
  if (!previousEvidence) return;
  const expected = {
    marker,
    taskId: taskId.toString(),
    tableNo,
    batchNo,
    materialId: materialId.toString(),
    formulaId: formulaId.toString(),
    lineId: lineId.toString(),
    workUnitId: workUnitId.toString(),
  };
  const actual = {
    marker: previousEvidence.marker,
    taskId: previousEvidence.identity && previousEvidence.identity.taskId,
    tableNo: previousEvidence.identity && previousEvidence.identity.tableNo,
    batchNo: previousEvidence.identity && previousEvidence.identity.batchNo,
    materialId: previousEvidence.identity && previousEvidence.identity.materialId,
    formulaId: previousEvidence.identity && previousEvidence.identity.formulaId,
    lineId: previousEvidence.identity && previousEvidence.identity.lineId,
    workUnitId: previousEvidence.identity && previousEvidence.identity.workUnitId,
  };
  if (JSON.stringify(actual) !== JSON.stringify(expected)) {
    throw new Error(
      `Resume evidence identity does not match the requested flow: expected=${JSON.stringify(expected)} actual=${JSON.stringify(actual)}`
    );
  }
}

async function main() {
  if (process.env.ADP_MES_FULL_FLOW_CONFIRM !== "YES") {
    throw new Error("Set ADP_MES_FULL_FLOW_CONFIRM=YES to run the live MES production flow acceptance");
  }
  ensureDir(outputDir);
  const previousEvidence = resumeEvidence;
  assertResumeIdentity(previousEvidence);
  const evidence = {
    generatedAt: new Date().toISOString(),
    repoCommit: gitCommit(),
    database: {
      mes: `PostgreSQL/${dbName}`,
      bpi: `PostgreSQL/${bpiDbName}`,
    },
    baseUrl,
    marker,
    status: "FAIL",
    identity: {
      taskId: taskId.toString(),
      tableNo,
      batchNo,
      materialId: materialId.toString(),
      materialCode,
      formulaId: formulaId.toString(),
      formulaCode,
      lineId: lineId.toString(),
      workUnitId: workUnitId.toString(),
      bpiBatchId: bpiBatchId || null,
      bpiBoundaryOwnership: bpiBatchId
        ? "Protected CLOSED_RAW shadow evidence; MES/QCS/WMS rows use the same order and batch identity without promoting the shadow batch."
        : "NOT_APPLICABLE",
    },
    resumedFrom: previousEvidence
      ? {
          generatedAt: previousEvidence.generatedAt,
          previousStatus: previousEvidence.status,
          resumedAt: new Date().toISOString(),
        }
      : null,
    steps: previousEvidence
      ? (previousEvidence.steps || []).filter((step) => step.status === "PASS")
      : [],
    verificationSql: verificationSql().trim(),
    issues: [],
  };
  writeEvidence(evidence);

  try {
    evidence.preflight = JSON.parse(runSql(preflightSql()));
    if (resume && Number(evidence.preflight.womTasks || 0) === 0) {
      evidence.issues.push({
        code: "WOM_SHARED_FIXTURE_REHYDRATION",
        status: "RECOVERING",
        detail:
          "The shared WOM task is missing while resuming; stale PASS steps are discarded and the same task identity will be rebuilt before QCS backfill.",
      });
      evidence.steps = [];
    }
    const currentBpi = JSON.parse(runSql(bpiStateSql(), bpiDbName));
    evidence.bpiBefore = previousEvidence && previousEvidence.bpiBefore
      ? previousEvidence.bpiBefore
      : currentBpi;
    assertPreflight(evidence.preflight, currentBpi);
    if (bpiBatchId && JSON.stringify(evidence.bpiBefore) !== JSON.stringify(currentBpi)) {
      throw new Error(`Protected BPI batch changed before resume: before=${JSON.stringify(evidence.bpiBefore)} current=${JSON.stringify(currentBpi)}`);
    }
    writeEvidence(evidence);

    const startOutput = path.join(outputDir, "start", "start.json");
    if (!evidence.steps.some((step) => step.name === "start" && step.status === "PASS")) {
      runStep(evidence, "start", scripts.start, startOutput, {
        ADP_WOM_ID_BASE: startIdBase.toString(),
        ADP_WOM_TRANSITIONS: "start",
        ADP_WOM_KEEP_FIXTURE: "true",
        ADP_WOM_STATE_PERSISTENCE_OUTPUT: startOutput,
      });
    }
    const start = readJson(startOutput);

    const processStartOutput = path.join(outputDir, "process-start", "process-start.json");
    const processStart = evidence.steps.some(
      (step) => step.name === "process-start" && step.status === "PASS"
    )
      ? readJson(processStartOutput)
      : runStep(evidence, "process-start", scripts.active, processStartOutput, {
          ADP_WOM_ID_BASE: putinIdBase.toString(),
          ADP_WOM_TASK_EXELOG_ID: start.persistence.exelog[1],
          ADP_WOM_PROCESS_WAIT_ID: processWaitId.toString(),
          ADP_WOM_WORK_ORDER_WAIT_ID: start.ids.wait,
          ADP_WOM_REUSE_WORK_ORDER_WAIT: "true",
          ADP_WOM_ACTIVE_ACTION: "process-start",
          ADP_WOM_ACTIVE_PERSISTENCE_OUTPUT: processStartOutput,
        });

    const putinOutput = path.join(outputDir, "putin", "putin.json");
    const putin = evidence.steps.some((step) => step.name === "putin" && step.status === "PASS")
      ? readJson(putinOutput)
      : runStep(evidence, "putin", scripts.active, putinOutput, {
          ADP_WOM_ID_BASE: putinIdBase.toString(),
          ADP_WOM_PROCESS_ID: processStart.ids.process,
          ADP_WOM_PROCESS_EXELOG_ID: processStart.persistence.processExelog[1],
          ADP_WOM_WAIT_ID: processStart.persistence.activeWait[1],
          ADP_WOM_TASK_EXELOG_ID: start.persistence.exelog[1],
          ADP_WOM_ACTIVE_ACTION: "putin-end",
          ADP_WOM_ACTIVE_PERSISTENCE_OUTPUT: putinOutput,
        });

    const outputOutput = path.join(outputDir, "output", "output.json");
    const output = evidence.steps.some((step) => step.name === "output" && step.status === "PASS")
      ? readJson(outputOutput)
      : runStep(evidence, "output", scripts.active, outputOutput, {
          ADP_WOM_ID_BASE: outputIdBase.toString(),
          ADP_WOM_PROCESS_ID: putin.ids.process,
          ADP_WOM_PROCESS_EXELOG_ID: processStart.persistence.processExelog[1],
          ADP_WOM_TASK_EXELOG_ID: start.persistence.exelog[1],
          ADP_WOM_ACTIVE_ACTION: "easy-end",
          ADP_WOM_ACTIVE_PERSISTENCE_OUTPUT: outputOutput,
        });

    const qcsOutput = path.join(outputDir, "quality", "quality.json");
    if (!evidence.steps.some((step) => step.name === "quality" && step.status === "PASS")) {
      const existingInspectCount = Number(runSql(`
SELECT count(*)
FROM public.qcs_inspects
WHERE source_table_id=${taskId}
  AND batch_code=${sqlLiteral(batchNo)}
  AND coalesce(valid, true);
`));
      runStep(evidence, "quality", scripts.qcs, qcsOutput, {
        ADP_QCS_REPORT_CHAIN_MODE: "qualified",
        ADP_QCS_KEEP_FIXTURE: "true",
        ADP_WOM_MANU_INSPECT_ID_BASE: qcsIdBase.toString(),
        ADP_WOM_MANU_INSPECT_RESUME: existingInspectCount > 0 ? "true" : "false",
        ADP_QCS_REDRIVE_WOM_BACKFILL: "true",
        ADP_WOM_WAIT_ID: start.ids.wait,
        ADP_WOM_TASK_EXELOG_ID: start.persistence.exelog[1],
        ADP_QCS_REPORT_CHAIN_PERSISTENCE_OUTPUT: qcsOutput,
      });
    }

    const processEndOutput = path.join(outputDir, "process-end", "process-end.json");
    if (!evidence.steps.some((step) => step.name === "process-end" && step.status === "PASS")) {
      runStep(evidence, "process-end", scripts.active, processEndOutput, {
        ADP_WOM_ID_BASE: processEndIdBase.toString(),
        ADP_WOM_PROCESS_ID: putin.ids.process,
        ADP_WOM_PROCESS_EXELOG_ID: processStart.persistence.processExelog[1],
        ADP_WOM_PROCESS_WAIT_ID: processWaitId.toString(),
        ADP_WOM_WORK_ORDER_WAIT_ID: start.ids.wait,
        ADP_WOM_REUSE_WORK_ORDER_WAIT: "true",
        ADP_WOM_TASK_EXELOG_ID: start.persistence.exelog[1],
        ADP_WOM_ACTIVE_ACTION: "process-end",
        ADP_WOM_ACTIVE_PERSISTENCE_OUTPUT: processEndOutput,
      });
    }

    const finishOutput = path.join(outputDir, "finish", "finish.json");
    if (!evidence.steps.some((step) => step.name === "finish" && step.status === "PASS")) {
      const currentTaskState = runSql(`
SELECT coalesce(task_run_state, '')
FROM public.wom_produce_tasks
WHERE id=${taskId};
`).trim();
      const verifyExistingFinish =
        resume &&
        currentTaskState === "WOM_runState/finished" &&
        fs.existsSync(finishOutput);
      const previousFinish = verifyExistingFinish ? readJson(finishOutput) : null;
      const finishStepIdBase =
        previousFinish && previousFinish.ids && previousFinish.ids.outputDetail
          ? BigInt(previousFinish.ids.outputDetail) - 6n
          : finishIdBase;
      runStep(evidence, "finish", scripts.finish, finishOutput, {
        ADP_WOM_ID_BASE: finishStepIdBase.toString(),
        ADP_WOM_WAIT_ID: start.ids.wait,
        ADP_WOM_LINE_ID: start.ids.line,
        ADP_WOM_TRANSITIONS: "stop-output",
        ADP_WOM_KEEP_FIXTURE: "true",
        ADP_WOM_VERIFY_EXISTING_TRANSITION: verifyExistingFinish ? "true" : "false",
        ADP_WOM_STATE_PERSISTENCE_OUTPUT: finishOutput,
      });
    }

    const traceOutput = path.join(outputDir, "trace", "trace.json");
    if (!evidence.steps.some((step) => step.name === "completion-inbound-trace" && step.status === "PASS")) {
      runStep(evidence, "completion-inbound-trace", scripts.trace, traceOutput, {
        ADP_PROCESS_ANALYSIS_ACCEPTANCE_OUTPUT: traceOutput,
        ADP_PROCESS_ANALYSIS_SCREENSHOT: path.join(outputDir, "trace", "process-trace.png"),
        ADP_PROCESS_ANALYSIS_TASK_ID: taskId.toString(),
        ADP_PROCESS_ANALYSIS_TASK_EXECUTION_ID: start.persistence.exelog[1],
        ADP_PROCESS_ANALYSIS_PROCESS_EXECUTION_ID: output.ids.processExelog,
        ADP_PROCESS_ANALYSIS_ACTIVITY_EXECUTION_ID: output.ids.activeExelog,
        ADP_PROCESS_ANALYSIS_TABLE_NO: tableNo,
        ADP_PROCESS_ANALYSIS_BATCH_NO: batchNo,
        ADP_PROCESS_ANALYSIS_PRODUCT_NO: materialCode,
        ADP_PROCESS_ANALYSIS_BROWSER_TASK_ID: taskId.toString(),
        ADP_PROCESS_ANALYSIS_BROWSER_TABLE_NO: tableNo,
        ADP_PROCESS_ANALYSIS_BROWSER_BATCH_NO: batchNo,
        ADP_PROCESS_ANALYSIS_KEEP_FIXTURE: "true",
      });
    }

    evidence.finalState = JSON.parse(runSql(verificationSql()));
    evidence.bpiAfter = JSON.parse(runSql(bpiStateSql(), bpiDbName));
    assertFinal(evidence.finalState, evidence.bpiBefore, evidence.bpiAfter);
    evidence.status = "PASS";
    evidence.retainedFixture = {
      retained: true,
      reason: "The same-document final state is retained so the user can inspect WOM, QCS, WMS and trace pages.",
      tenantId,
    };
  } catch (error) {
    evidence.issues.push(error.stack || error.message);
  }

  writeEvidence(evidence);
  console.log(`${evidence.status}: ${outputPath}`);
  if (evidence.status !== "PASS") {
    process.exitCode = 1;
  }
}

main().catch((error) => {
  console.error(error.stack || error.message);
  process.exitCode = 1;
});
