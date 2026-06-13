#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { request } = require("playwright");

const baseUrl = (process.env.ADP_BASE_URL || "http://10.11.100.17:18080").replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD || "123456";
const outputPath =
  process.env.ADP_BUSINESS_SMOKE_OUTPUT ||
  path.join("/tmp", `adp-business-module-smoke-${new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14)}.json`);

const bodyErrorPattern =
  /(系统错误|系统异常|发生未知异常|Incomplete parameter|非法参数|Bad Request|404_NOT_FOUND|500 INTERNAL|Caused by:|\b[\w.]+Exception(?::|\s+at\b)|Invalid bound statement|relation .* does not exist|column .* does not exist)/i;

const checks = [
  {
    name: "base-cooperate-layout",
    module: "BaseSet",
    path: "/msService/BaseSet/cooperate/cooperate/cmcLayout",
  },
  {
    name: "base-material-layout",
    module: "BaseSet",
    path: "/msService/BaseSet/material/material/materialLayout",
  },
  {
    name: "base-unit-group-list",
    module: "BaseSet",
    path: "/msService/BaseSet/unit/unitGroup/unitGroupList",
  },
  {
    name: "chart-scatter-list",
    module: "ChartReportMap",
    path: "/msService/ChartReportMap/scatterChartSet/scatteChartSet/scatterChartList",
  },
  {
    name: "dataset-category-list",
    module: "DataSet",
    path: "/msService/DataSet/categoryMgt/categoryMgt/categoryList",
  },
  {
    name: "hierarchical-factory-tree",
    module: "HierarchicalMod",
    path: "/msService/HierarchicalMod/factoryModel/factoryModel/factoryTreeList",
  },
  {
    name: "tagmanagement-tag-list",
    module: "TagManagement",
    path: "/msService/TagManagement/tagInfo/tag/tagList",
  },
  {
    name: "tagmanagement-data-convert-list",
    module: "TagManagement",
    path: "/msService/TagManagement/dataConvert/dataConvert/dataConvertList",
  },
  {
    name: "team-team-list",
    module: "TeamInfo",
    path: "/msService/TeamInfo/team/team/teamList",
  },
  {
    name: "team-schedule-dept-layout",
    module: "TeamInfo",
    path: "/msService/TeamInfo/schedual/schedule/schedualDeptLayout",
  },
  {
    name: "qualification-certificate-layout",
    module: "Qualify",
    path: "/msService/Qualify/certificate/certificate/certifcateLayOut",
  },
  {
    name: "doc-document-list",
    module: "DocManage",
    path: "/msService/DocManage/document/docDocument/documentList",
  },
  {
    name: "lims-analy-sample-list",
    module: "LIMSBasic",
    path: "/msService/LIMSBasic/analySample/analySample/analySampleList",
  },
  {
    name: "lims-sample-type-list",
    module: "LIMSBasic",
    path: "/msService/LIMSBasic/sampleType/sampleType/sampleTypeList",
  },
  {
    name: "lims-dc-analysis-file-ref",
    module: "LIMSDC",
    path: "/msService/LIMSDC/analysisFile/analysisFile/analysisFileRef",
  },
  {
    name: "lims-material-info-list",
    module: "LIMSMaterial",
    path: "/msService/LIMSMaterial/mATInfo/matInfo/matInfoList",
  },
  {
    name: "lims-retain-plan-list",
    module: "LIMSRetain",
    path: "/msService/LIMSRetain/retention/retPlan/retPlanList",
  },
  {
    name: "lims-sample-collect-layout",
    module: "LIMSSample",
    path: "/msService/LIMSSample/sample/sampleInfo/collectListLayout",
  },
  {
    name: "lims-stds-aq-prep-record-list",
    module: "LIMSSTDS",
    path: "/msService/LIMSSTDS/aqPrepRecord/aqPrepRecord/aqPrepRecordList",
  },
  {
    name: "lims-steady-env-condition-list",
    module: "LIMSSteady",
    path: "/msService/LIMSSteady/envCondition/envCondition/envConditionList",
  },
  {
    name: "qcs-manu-inspect-list",
    module: "QCS",
    path: "/msService/QCS/inspect/inspect/manuInspectList",
  },
  {
    name: "wom-make-task-list",
    module: "WOM",
    path: "/msService/WOM/produceTask/produceTask/makeTaskList",
  },
  {
    name: "wom-retirement-pda-list",
    module: "WOM",
    path: "/msService/WOM/batchMaterial/batMaterilPart/baRetireMentPDAList",
  },
  {
    name: "rm-batch-formula-list",
    module: "RM",
    path: "/msService/RM/formula/formula/batchFormulaList",
  },
  {
    name: "craft-basic-info-list",
    module: "craftGraph",
    path: "/msService/craftGraph/basicInfo/basicInfo/basicInfoList",
  },
  {
    name: "craft-button-conf-list",
    module: "craftGraph",
    path: "/msService/craftGraph/operationButton/buttonConfig/buttonConfList",
  },
  {
    name: "eam-base-info-layout",
    module: "EAM",
    path: "/msService/EAM/baseInfo/baseInfo/baseInfoLayout",
  },
  {
    name: "eqpt-operation-alarm-history-layout",
    module: "EQPTOperation",
    path: "/msService/EQPTOperation/alarm/alarmRecord/alarmHistoryLayout",
  },
  {
    name: "eqpt-operation-alarm-record-layout",
    module: "EQPTOperation",
    path: "/msService/EQPTOperation/alarm/alarmRecord/alarmRecordLayout",
  },
  {
    name: "eqpt-operation-alarming-layout",
    module: "EQPTOperation",
    path: "/msService/EQPTOperation/alarm/alarmRecord/alarmingLayout",
  },
  {
    name: "measure-type-tree-layout",
    module: "Measure",
    path: "/msService/Measure/eamType/meaType/treeLayout",
  },
  {
    name: "measure-info-base-layout",
    module: "Measure",
    path: "/msService/Measure/meaInfo/baseInfo/baseInfoLayout",
  },
  {
    name: "measure-info-view-layout",
    module: "Measure",
    path: "/msService/Measure/meaInfo/baseInfo/eamViewLayout",
  },
  {
    name: "oee-running-record-layout",
    module: "OverEquipEffect",
    path: "/msService/OverEquipEffect/runRecord/runningRecord/runningRecordLayout",
  },
  {
    name: "overhaul-ticket-maintenance-ticket-list",
    module: "OverhaulTicket",
    path: "/msService/OverhaulTicket/otMaintenanceTicket/otMWorkTicket/otMtcTicketList",
  },
  {
    name: "overhaul-ticket-hazard-library-list",
    module: "OverhaulTicket",
    path: "/msService/OverhaulTicket/otOverhaulHazard/otHazid/otHazidLibList",
  },
  {
    name: "overhaul-ticket-risk-safety-list",
    module: "OverhaulTicket",
    path: "/msService/OverhaulTicket/otRiskSafeMeasure/riskSafeys/otRiskSafeyLisit",
  },
  {
    name: "parti-manage-scenario-list",
    module: "PartiManage",
    path: "/msService/PartiManage/businessScenario/scenarioInfo/scenarioInfoList",
  },
  {
    name: "parti-manage-notice-log-list",
    module: "PartiManage",
    path: "/msService/PartiManage/noticeLog/noticeLog/noticeLogList",
  },
  {
    name: "parti-manage-problem-list",
    module: "PartiManage",
    path: "/msService/PartiManage/problemManage/problemDetail/problemList",
  },
  {
    name: "spare-manage-arrival-list",
    module: "SpareManage",
    path: "/msService/SpareManage/spareArrvial/purArrivalInfo/spareArrvialList",
  },
  {
    name: "spare-manage-back-list",
    module: "SpareManage",
    path: "/msService/SpareManage/spareBack/spWarehousing/spareBackList",
  },
  {
    name: "spare-manage-demand-plan-report-list",
    module: "SpareManage",
    path: "/msService/SpareManage/spareDemand/demandDetail/planReportList",
  },
  {
    name: "tool-tool-layout",
    module: "TOOL",
    path: "/msService/TOOL/toolInfo/toolInfo/toolLayout",
  },
  {
    name: "maintenance-fault-library-layout",
    module: "maintenance",
    path: "/msService/maintenance/faultRepository/eamFaultLib/eamFaultLibLayout",
  },
  {
    name: "maintenance-daily-plan-list",
    module: "maintenance",
    path: "/msService/maintenance/dailyPlan/dailyplanHead/dailyPlanList",
  },
  {
    name: "maintenance-entrust-repair-list",
    module: "maintenance",
    path: "/msService/maintenance/entrustRepair/entrustRepair/entrustRecordList",
  },
  {
    name: "outag-manage-statistic-list",
    module: "outagManage",
    path: "/msService/outagManage/dataStatistic/dataStatistic/statisticList",
  },
  {
    name: "outag-manage-electric-inspect-record-list",
    module: "outagManage",
    path: "/msService/outagManage/eleInspectRecord/eleCheckRecord/eleInspectRecordList",
  },
  {
    name: "outag-manage-electric-delay-apply-list",
    module: "outagManage",
    path: "/msService/outagManage/electricDelay/eleDelayApply/delayApplyList",
  },
  {
    name: "special-finish-check-list",
    module: "Special",
    path: "/msService/Special/semFinishCheck/finishCheck/finishCheckList",
  },
  {
    name: "wts-work-permit-list",
    module: "WTS",
    path: "/msService/WTS/workPermit/workPermit/workPermitList",
  },
  {
    name: "waps-work-plan-list",
    module: "workAppointment",
    path: "/msService/workAppointment/workPlan/workTicketPlan/workPlanList",
  },
];

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

async function readJsonSafe(response) {
  const text = await response.text();
  try {
    return { json: JSON.parse(text), text };
  } catch (_error) {
    return { json: null, text };
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
    const parsed = await readJsonSafe(response);
    const ticket = response.ok() ? findTicket(parsed.json) : null;
    if (ticket) {
      return ticket;
    }
    errors.push({ status: response.status(), body: parsed.text.slice(0, 500) });
  }

  throw new Error(`Login failed for ${username}: ${JSON.stringify(errors)}`);
}

function responseHasError(text) {
  return bodyErrorPattern.test(String(text || ""));
}

async function runCheck(api, ticket, check) {
  const response = await api.fetch(`${baseUrl}${check.path}`, {
    method: check.method || "GET",
    data: check.data,
    headers: {
      Accept: "application/json, text/plain, */*",
      Authorization: `Bearer ${ticket}`,
      Cookie: `suposTicket=${encodeURIComponent(ticket)}`,
    },
  });
  const parsed = await readJsonSafe(response);
  const snippet = parsed.text.replace(/\s+/g, " ").slice(0, 600);
  const okStatus = response.status() >= 200 && response.status() < 400;
  const bodyOk = !responseHasError(parsed.text);
  const passed = okStatus && bodyOk;

  return {
    ...check,
    status: response.status(),
    contentType: response.headers()["content-type"] || "",
    bytes: parsed.text.length,
    passed,
    error: passed ? null : snippet,
  };
}

async function main() {
  const api = await request.newContext({ ignoreHTTPSErrors: true });
  const results = [];
  try {
    const ticket = await login(api);
    for (const check of checks) {
      try {
        const result = await runCheck(api, ticket, check);
        results.push(result);
        const marker = result.passed ? "OK" : "FAIL";
        console.log(`${marker} ${result.name} module=${result.module} status=${result.status} bytes=${result.bytes}`);
        if (!result.passed && result.error) {
          console.log(`  ${result.error}`);
        }
      } catch (error) {
        const result = {
          ...check,
          status: null,
          contentType: "",
          bytes: 0,
          passed: false,
          error: error && error.stack ? error.stack : String(error),
        };
        results.push(result);
        console.log(`FAIL ${result.name} module=${result.module} ${result.error}`);
      }
    }
  } finally {
    await api.dispose();
  }

  const summary = {
    baseUrl,
    username,
    generatedAt: new Date().toISOString(),
    total: results.length,
    passed: results.filter((result) => result.passed).length,
    failed: results.filter((result) => !result.passed).length,
    results,
  };
  fs.writeFileSync(outputPath, JSON.stringify(summary, null, 2));
  console.log(`SUMMARY total=${summary.total} passed=${summary.passed} failed=${summary.failed}`);
  console.log(`REPORT ${outputPath}`);

  if (summary.failed) {
    process.exitCode = 1;
  }
}

main().catch((error) => {
  console.error(error && error.stack ? error.stack : error);
  process.exit(1);
});
