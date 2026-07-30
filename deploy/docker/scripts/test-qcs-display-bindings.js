#!/usr/bin/env node
"use strict";

const assert = require("assert");
const fs = require("fs");
const path = require("path");

const repoRoot = path.resolve(__dirname, "..", "..", "..");

function read(relativePath) {
  return fs.readFileSync(path.join(repoRoot, relativePath), "utf8");
}

const nginx = read("deploy/docker/nginx/adp.conf");
const listI18n = read(
  "deploy/docker/assets/module-static/QCS/inspect/inspect/manuInspectList/i18n-value.js"
);
const editI18n = read(
  "deploy/docker/assets/module-static/QCS/inspect/inspect/manuInspectEdit/i18n-value.js"
);
const planListI18n = read(
  "deploy/docker/assets/module-static/QCS/testPlan/inspectPlan/manuInspPlanList/i18n-value.js"
);
const reportViewI18n = read(
  "deploy/docker/assets/module-static/QCS/inspectReport/inspectReport/manuInspReportView/i18n-value.js"
);
const reportBody = read(
  "deploy/docker/assets/module-static/QCS/inspectReport/inspectReport/manuInspReportEdit/body.js"
);
const reportBodyEs5 = read(
  "deploy/docker/assets/module-static/QCS/inspectReport/inspectReport/manuInspReportEdit/body-es5.js"
);
const reportViewSql = read(
  "deploy/docker/postgres/init/194-qcs-inspect-report-view-runtime-json.sql"
);
const womSourceSql = read("deploy/docker/postgres/init/195-wom-source-compat.sql");
const inspectListSql = read(
  "deploy/docker/postgres/init/196-qcs-manu-inspect-list-empty-column.sql"
);
const limsPatchScript = read(
  "deploy/docker/scripts/patch-lims-qcs-inspect-report-service.sh"
);
const limsResponseSource = read(
  "deploy/docker/patches/limsbasic-wom-source-response/src/com/supcon/orchid/LIMSBasic/utils/ServiceClientUtils.java"
);
const acceptance = read(
  "deploy/docker/scripts/adp-qcs-report-chain-persistence-acceptance.js"
);

assert(
  nginx.includes(
    "location = /greenDill/static/QCS/inspect/inspect/manuInspectList/i18n-value.js"
  ),
  "QCS list i18n asset must have an exact no-cache nginx route"
);
assert(
  listI18n.includes('window.InternationalResource["ec.common.tableNo"] = "单据编号";'),
  "QCS list i18n asset must translate the document-number header"
);
assert(
  listI18n.includes(
    'window.InternationalResource["SupDatagrid.button.error"] = "请选择一条记录进行操作！";'
  ) &&
    listI18n.includes(
      'window.InternationalResource["ec.list.taskDescription"] = "任务描述";'
    ),
  "QCS list i18n asset must translate action feedback and workflow headers"
);
assert(
  listI18n.includes("installQCS_MANU_INSPECT_LISTI18nCompatibility"),
  "QCS list i18n asset must install the cross-frame compatibility fallback"
);
assert(
  nginx.includes(
    "location = /greenDill/static/QCS/inspect/inspect/manuInspectEdit/i18n-value.js"
  ),
  "QCS edit i18n asset must have an exact no-cache nginx route"
);
assert(
  editI18n.includes(
    'window.InternationalResource["LIMSBasic.viewtitle.randon1584520303249"] = "质量标准参照";'
  ) &&
    editI18n.includes(
      'window.InternationalResource["Button.text.select"] = "选择";'
    ) &&
    editI18n.includes(
      'window.InternationalResource["Button.text.close"] = "关闭";'
    ) &&
    editI18n.includes(
      'window.InternationalResource["Reference.confirm.tip.message"] = "请至少选中一行！";'
    ),
  "QCS edit quality-standard reference must translate its title, actions, and empty-selection feedback"
);
assert(
  editI18n.includes("installQCS_MANU_INSPECT_EDITI18nCompatibility"),
  "QCS edit i18n asset must install the cross-frame compatibility fallback"
);
assert(
  nginx.includes(
    "location = /greenDill/static/QCS/testPlan/inspectPlan/manuInspPlanList/i18n-value.js"
  ),
  "QCS inspection-plan i18n asset must have an exact no-cache nginx route"
);
assert(
  planListI18n.includes(
    'window.InternationalResource["SupDatagrid.button.error"] = "请选择一条记录进行操作！";'
  ) &&
    planListI18n.includes("installQCS_MANU_INSP_PLAN_LISTI18nCompatibility"),
  "QCS inspection-plan actions must install translated feedback resources"
);
assert(
  nginx.includes(
    "location = /greenDill/static/QCS/inspectReport/inspectReport/manuInspReportView/i18n-value.js"
  ),
  "QCS report view i18n asset must have an exact no-cache nginx route"
);
assert(
  reportViewI18n.includes(
    'window.InternationalResource["LIMSBasic.qualityStd.stdGrade.range"] = "范围";'
  ),
  "QCS report view must translate the cross-module range header"
);

for (const [name, source] of [
  ["body.js", reportBody],
  ["body-es5.js", reportBodyEs5],
]) {
  assert(
    source.includes("installQcsPersistedConclusionRestore"),
    `${name} must restore the persisted conclusion selector`
  );
  assert(
    source.includes("__ADP_QCS_CONCLUSION_RESTORED__"),
    `${name} must expose browser-verifiable restoration evidence`
  );
  assert(
    source.includes("pendingId") && source.includes("maxAttempts = 300"),
    `${name} must restrict and retain the persisted-value guard through late initialization`
  );
  assert(
    !source.includes("restorePersistedConclusion() || attempts"),
    `${name} must not stop after the first transient restore`
  );
  assert(
    source.includes("ensureConclusionOption") &&
      source.includes("selector.addOption(gradeId, result)"),
    `${name} must restore the persisted option label before selecting it`
  );
}

assert(
  reportViewSql.includes("QCS_5.0.0.0_inspectReport_manuInspReportView"),
  "PostgreSQL migration must restore the manufacturing report read-only view"
);
assert(
  reportViewSql.includes('"key":"inspectReport.checkResult"'),
  "read-only report layout must bind the persisted inspection conclusion"
);
assert(
  womSourceSql.includes("CREATE TABLE IF NOT EXISTS public.wom_source") &&
    womSourceSql.includes("'chemical industry'"),
  "PostgreSQL must provide the WOM industry source used by QCS report rendering"
);
assert(
  inspectListSql.includes("3aaefcb6_6d96_4d9d_b2c6_bde20b55ed9d") &&
    inspectListSql.includes("target ->> 'key'") &&
    inspectListSql.includes("jsonb_build_object('isHidden', true, 'hide', true)"),
  "QCS list runtime patch must hide the unnamed vendor custom column"
);
assert(
  limsPatchScript.includes("com.supcon.greendill.LIMSBasic.service-") &&
    limsPatchScript.includes("ServiceClientUtils.class"),
  "LIMS boot patcher must replace the LIMSBasic WOM response client class"
);
assert(
  limsPatchScript.includes("ADP_JAVA8_JDK_IMAGE") &&
    limsPatchScript.includes("docker run --rm") &&
    !limsPatchScript.includes('jar tf "$lims_jar"'),
  "LIMS boot patcher must compile with the local Java 8 JDK image when the host has no JDK"
);
assert(
  limsResponseSource.includes('"200".equals(String.valueOf(responseCode))') &&
    limsResponseSource.includes("JSONObject.parseObject((String) rawData)"),
  "LIMSBasic WOM response client must accept the current code/data-string envelope"
);
assert(
  acceptance.includes("effectiveReportDisplay") &&
    acceptance.includes("expectedResultOption") &&
    acceptance.includes("reportViewDisplay") &&
    acceptance.includes("rawEmptyCustomKeyVisible") &&
    acceptance.includes("rawRangeKeyVisible"),
  "QCS persistence acceptance must assert list, edit, and read-only report display truth"
);

console.log("QCS display binding regression checks passed");
