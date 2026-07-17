#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { execFileSync } = require("child_process");
const { chromium, request } = require("playwright");

const baseUrl = (process.env.ADP_BASE_URL || "http://10.11.100.17:18080").replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD || "123456";
const headless = process.env.ADP_HEADLESS !== "false";
const outputDir =
  process.env.ADP_PATROL_REPORT_OUTPUT_DIR ||
  path.join(
    "/tmp",
    `adp-patrol-report-smoke-${new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14)}`
  );
const reportPath =
  process.env.ADP_PATROL_REPORT_OUTPUT || path.join(outputDir, "patrol-report-smoke.json");
const dbSshTarget = process.env.ADP_DB_SSH_TARGET || "v6@10.11.100.17";
const dbContainer = process.env.ADP_DB_CONTAINER || "adp-mes-newbase-postgres-1";
const dbName = process.env.ADP_DB_NAME || "adp";
const dbUser = process.env.ADP_DB_USER || "adp";
const reportMonth = process.env.ADP_PATROL_REPORT_MONTH || new Date().toISOString().slice(0, 7);

if (!/^\d{4}-\d{2}$/.test(reportMonth)) {
  throw new Error(`ADP_PATROL_REPORT_MONTH must use YYYY-MM format: ${reportMonth}`);
}

const targets = [
  {
    code: "potrol-result-summary",
    label: "巡检结果汇总",
    url: "/msService/PATROL/patrolTask/taskDetail/potrolResultSummary",
    expectedApis: ["/patrolTask/taskDetail/potrolResultSummary-query"],
  },
  {
    code: "abnormal-summary",
    label: "异常巡检结果汇总",
    url: "/msService/PATROL/patrolTask/taskDetail/abnormalSummary",
    expectedApis: ["/patrolTask/taskDetail/abnormalSummary-query"],
  },
  {
    code: "task-overview",
    label: "巡检任务概述",
    url: "/msService/PATROL/patrolTask/potrolTask/taskOverviewList",
    expectedApis: ["/patrolTask/potrolTask/data-dg1586331011160"],
  },
  {
    code: "unusual-summary",
    label: "异常巡检情况概述",
    url: "/msService/PATROL/patrolTask/taskDetail/unusualSummary",
    expectedApis: [
      "/patrolTask/taskDetail/getFaultDealStatus",
      "/patrolTask/taskDetail/getFaultDealCountGroupByCompleteUser",
      "/patrolTask/potrolTask/getabnormalTaskDetailList",
    ],
  },
  {
    code: "task-finish-statistics",
    label: "巡检任务完成情况",
    url: "/msService/PATROL/patrolTask/taskDetail/taskFinishStatistics",
    expectedApis: [
      "/patrolTask/potrolTask/getTaskCompleteStatisticsTableData",
      "/patrolTask/potrolTask/getTaskCompleteStatisticsRC",
      "/patrolTask/potrolTask/getTableData",
    ],
  },
  {
    code: "stability-rate",
    label: "设备平稳率",
    url: "/msService/PATROL/patrolTask/potrolTask/stabilityRAMenu",
    expectedApis: [
      "/patrolTask/taskDetail/eamStabilityRateChart",
      "/patrolTask/taskDetail/eamStabilityRate",
    ],
  },
  {
    code: "gather-error-analysis",
    label: "采集数据误差分析",
    url: "/msService/PATROL/patrolTask/potrolTask/errorAnalysisMenu",
    expectedApis: [
      "/patrolRoute/workGroup/getGroupList",
      "/patrolRoute/workArea/getWorkAreaList",
      "/patrolRoute/workItem/getworkItemList",
    ],
  },
  {
    code: "patrol-monitor-map",
    label: "巡检监控地图",
    url: "/msService/PATROL/patrolMonit/patrolMonit/patrolMonitMap",
    expectedApis: [
      "/patrolRoute/workGroup/selectData",
      "/patrolTask/potrolTask/selectData",
    ],
    queryRequired: false,
  },
];

const visibleErrorPattern =
  /(系统错误|系统异常|数据库操作异常|SQLGrammarException|could not extract ResultSet|InvalidDataAccess|BadSqlGrammar|relation .* does not exist|column .* does not exist|找不到viewCode|404_NOT_FOUND|500 INTERNAL|\bHTTP\s*(404|500)\b|\b(404|500)\s+(Not Found|Internal Server Error)\b|Caused by:|\b[\w.]+Exception(?::|\s+at\b))/i;
const rawI18nPattern = /\b(?:PATROL|ec|foundation)\.[A-Za-z0-9_.-]+\b/g;

function ensureDir(targetPath) {
  fs.mkdirSync(targetPath, { recursive: true });
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
  const failures = [];

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
    failures.push({ status: response.status(), body: parsed.text.slice(0, 300) });
  }

  throw new Error(`PATROL report login failed: ${JSON.stringify(failures)}`);
}

function unique(values) {
  return [...new Set(values.filter(Boolean))];
}

function summarizePatrolPayload(url, payload) {
  if (!payload || typeof payload !== "object") {
    return null;
  }

  const data = payload.data;
  const summary = { appCode: payload.code == null ? null : payload.code };
  if (Array.isArray(data)) {
    summary.dataCount = data.length;
  } else if (data && typeof data === "object") {
    if (Array.isArray(data.result)) {
      summary.resultCount = data.result.length;
    }
    if (data.totalCount != null) {
      summary.totalCount = Number(data.totalCount);
    }
  }

  if (url.includes("getFaultDealStatus")) {
    summary.values = data;
  } else if (
    url.includes("getTaskCompleteStatisticsTableData") ||
    url.includes("getTaskCompleteStatisticsRC")
  ) {
    summary.rows = Array.isArray(data) ? data : [];
  } else if (url.includes("getTableData")) {
    summary.rows = data && Array.isArray(data.result) ? data.result : [];
  } else if (url.includes("eamStabilityRateChart") || url.includes("eamGatherErrorChart")) {
    const rows = Array.isArray(data) ? data : [];
    summary.valueCounts = rows.reduce((counts, row) => {
      const key = String(row && row.concluse);
      counts[key] = (counts[key] || 0) + 1;
      return counts;
    }, {});
  } else if (
    url.includes("eamStabilityRate") ||
    url.includes("eamGatherErrorTable") ||
    url.includes("getabnormalTaskDetailList")
  ) {
    summary.totalCount = Number((data && data.totalCount) || 0);
  } else if (url.includes("/selectData")) {
    const rows = data && Array.isArray(data.result) ? data.result : [];
    summary.resultCount = rows.length;
    summary.stateCounts = rows.reduce((counts, row) => {
      const rawState = row && row.taskState;
      const state = rawState && typeof rawState === "object" ? rawState.id : rawState;
      if (state) {
        counts[state] = (counts[state] || 0) + 1;
      }
      return counts;
    }, {});
  }
  return summary;
}

async function selectFirstBusinessOption(frame, combo) {
  await combo.click({ timeout: 5000 });
  await frame.waitForTimeout(300);
  const options = frame.locator(
    ".ant-select-dropdown-menu-item:visible:not(.ant-select-dropdown-menu-item-disabled)"
  );
  const texts = await options.allTextContents();
  const index = texts.findIndex((text) => {
    const value = text.trim();
    return value && value !== "暂无数据" && value !== "No data";
  });
  if (index < 0) {
    await frame.locator("body").press("Escape").catch(() => {});
    return { selected: false, options: texts.map((text) => text.trim()) };
  }
  const option = options.nth(index);
  const text = (await option.innerText()).trim();
  await option.click({ timeout: 5000 });
  return { selected: true, text, options: texts.map((value) => value.trim()) };
}

async function prepareGatherErrorQuery(page) {
  const frame = page.frames().find((candidate) => /\/errorAnalysis(?:\?|$)/.test(candidate.url()));
  if (!frame) {
    return { ready: false, reason: "error-analysis-frame-not-found" };
  }
  const combos = frame.locator("[role='combobox']");
  if ((await combos.count()) < 3) {
    return { ready: false, reason: "error-analysis-filters-not-found", frameUrl: frame.url() };
  }

  const route = await selectFirstBusinessOption(frame, combos.nth(0));
  if (!route.selected) {
    return { ready: false, reason: "no-patrol-route", route, frameUrl: frame.url() };
  }
  await page.waitForTimeout(700);
  const area = await selectFirstBusinessOption(frame, combos.nth(1));
  if (!area.selected) {
    return { ready: false, reason: "no-patrol-area", route, area, frameUrl: frame.url() };
  }
  await page.waitForTimeout(700);
  const item = await selectFirstBusinessOption(frame, combos.nth(2));
  await page.waitForTimeout(400);
  return {
    ready: item.selected,
    reason: item.selected ? null : "no-error-benchmark-item",
    route,
    area,
    item,
    frameUrl: frame.url(),
  };
}

async function collectNotifications(page) {
  const values = [];
  for (const frame of page.frames()) {
    const texts = await frame
      .locator(".ant-notification-notice:visible")
      .allTextContents()
      .catch(() => []);
    values.push(...texts.map((text) => text.trim()).filter(Boolean));
  }
  return unique(values);
}

function shellQuote(value) {
  return `'${String(value).replace(/'/g, "'\\''")}'`;
}

function runPostgresSql(sql) {
  const remoteCommand = [
    "docker",
    "exec",
    "-i",
    dbContainer,
    "psql",
    "-U",
    dbUser,
    "-d",
    dbName,
    "-v",
    "ON_ERROR_STOP=1",
    "-At",
  ]
    .map(shellQuote)
    .join(" ");
  return execFileSync(
    "ssh",
    [
      "-o",
      "BatchMode=yes",
      "-o",
      "ConnectTimeout=10",
      "-o",
      "ServerAliveInterval=15",
      "-o",
      "ServerAliveCountMax=2",
      dbSshTarget,
      remoteCommand,
    ],
    { input: sql, encoding: "utf8", stdio: ["pipe", "pipe", "pipe"] }
  ).trim();
}

function reportWindow() {
  const [year, month] = reportMonth.split("-").map(Number);
  const next = new Date(Date.UTC(year, month, 1));
  return {
    start: `${reportMonth}-01 00:00:00`,
    endExclusive: `${next.getUTCFullYear()}-${String(next.getUTCMonth() + 1).padStart(2, "0")}-01 00:00:00`,
  };
}

function queryPostgresAggregates() {
  const window = reportWindow();
  const sql = `
WITH task_area_signs AS (
  SELECT patrol_task AS task_id,
         SUM(CASE WHEN pay_card_type='PATROL_payCardType/card' THEN 1 ELSE 0 END) AS card_sign_num,
         SUM(CASE WHEN pay_card_type='PATROL_payCardType/signIn' THEN 1 ELSE 0 END) AS hand_sign_num
  FROM public.mp_patrol_task_areas
  WHERE valid=true
  GROUP BY patrol_task
), task_month AS (
  SELECT pt.*, wg.dept
  FROM public.mp_potrol_tasks pt
  JOIN public.mp_work_groups wg ON pt.work_route=wg.id
  WHERE pt.valid=true AND pt.status=99 AND pt.cid=1000 AND wg.cid=1000 AND wg.dept=1
    AND pt.task_state<>'PATROL_taskState/cancelled'
    AND pt.start_time >= TIMESTAMP '${window.start}'
    AND pt.start_time < TIMESTAMP '${window.endExclusive}'
), task_aggregate AS (
  SELECT COUNT(*) AS total_num,
         SUM(CASE WHEN tm.task_state='PATROL_taskState/completed' THEN 1 ELSE 0 END) AS complete_num,
         SUM(CASE WHEN tm.task_state='PATROL_taskState/completed' THEN 0 ELSE 1 END) AS uncomplete_num,
         SUM(CASE WHEN tm.actual_end_time <= tm.end_time THEN 1 ELSE 0 END) AS on_time_num,
         COALESCE(SUM(tas.card_sign_num),0) AS card_sign_num,
         COALESCE(SUM(tas.hand_sign_num),0) AS hand_sign_num
  FROM task_month tm
  LEFT JOIN task_area_signs tas ON tm.id=tas.task_id
), detail_month AS (
  SELECT td.*
  FROM public.mp_task_details td
  JOIN public.mp_potrol_tasks pt ON td.patrol_task=pt.id
  JOIN public.mp_work_groups wg ON pt.work_route=wg.id
  WHERE td.valid=true AND td.cid=1000 AND pt.valid=true AND pt.status=99 AND pt.cid=1000
    AND wg.cid=1000 AND wg.dept=1 AND td.complete_user IS NOT NULL
    AND pt.task_state='PATROL_taskState/completed'
    AND pt.start_time >= TIMESTAMP '${window.start}'
    AND pt.start_time < TIMESTAMP '${window.endExclusive}'
), stability_aggregate AS (
  SELECT COUNT(*) AS total_num,
         COUNT(*) FILTER (WHERE td.concluse='12.34') AS normal_sample_num,
         COUNT(*) FILTER (WHERE td.concluse='99.99') AS abnormal_sample_num
  FROM public.mp_task_details td
  JOIN public.mp_potrol_tasks pt ON td.patrol_task=pt.id
  JOIN public.mp_work_groups wg ON pt.work_route=wg.id
  JOIN public.mp_input_standards ins ON td.input_standard_id=ins.id
  WHERE td.valid=true AND td.cid=1000 AND td.concluse IS NOT NULL
    AND ins.val_type='PATROL_valueType/number' AND ins.edit_type='PATROL_editType/input'
    AND wg.dept=1
    AND td.complete_date >= TIMESTAMP '${window.start}'
    AND td.complete_date < TIMESTAMP '${window.endExclusive}'
), gather_aggregate AS (
  SELECT COUNT(*) AS eligible_detail_num
  FROM public.mp_task_details td
  JOIN public.mp_work_items wi ON td.work_item_id=wi.id
  JOIN public.mp_input_standards ins ON wi.input_standard_id=ins.id
  JOIN public.mp_potrol_tasks pt ON td.patrol_task=pt.id
  JOIN public.mp_work_groups wg ON pt.work_route=wg.id
  WHERE td.valid=true AND td.cid=1000 AND wi.is_error_bench=true
    AND td.gather_data IS NOT NULL AND td.concluse IS NOT NULL
    AND ins.val_type='PATROL_valueType/number' AND ins.edit_type='PATROL_editType/input'
    AND wg.dept=1
    AND td.complete_date >= TIMESTAMP '${window.start}'
    AND td.complete_date < TIMESTAMP '${window.endExclusive}'
)
SELECT json_build_object(
  'window', json_build_object('start','${window.start}','endExclusive','${window.endExclusive}'),
  'resultSummaryCount', (
    SELECT COUNT(*) FROM public.mp_task_details td
    JOIN public.mp_potrol_tasks pt ON td.patrol_task=pt.id
    WHERE td.valid=true AND td.cid=1000 AND pt.valid=true AND pt.status=99 AND pt.cid=1000
      AND pt.task_state='PATROL_taskState/completed'
  ),
  'abnormalSummaryCount', (
    SELECT COUNT(*) FROM public.mp_task_details td
    JOIN public.mp_potrol_tasks pt ON td.patrol_task=pt.id
    WHERE td.valid=true AND td.cid=1000 AND pt.valid=true AND pt.status=99 AND pt.cid=1000
      AND pt.task_state='PATROL_taskState/completed'
      AND td.real_value='PATROL_realValue/abnormal'
  ),
  'taskOverviewCount', (
    SELECT COUNT(*) FROM public.mp_potrol_tasks pt
    JOIN public.mp_work_groups wg ON pt.work_route=wg.id
    WHERE pt.valid=true AND pt.status=99 AND pt.cid=1000 AND wg.cid=1000
      AND wg.dept IS NOT NULL AND pt.task_state<>'PATROL_taskState/cancelled'
  ),
  'hiddenDanger', (
    SELECT json_build_object(
      'linkedCount', COUNT(*),
      'pendingCount', COUNT(*) FILTER (WHERE hr.risk_mode='PATROL_COMPATIBILITY_PENDING'),
      'closedCount', COUNT(*) FILTER (WHERE hr.status IN (99,-99))
    )
    FROM public.mp_task_details td
    JOIN public.mp_potrol_tasks pt ON td.patrol_task=pt.id
    JOIN public.mp_work_groups wg ON pt.work_route=wg.id
    JOIN public.ses_hrm_riskhandles hr ON td.fault_id=hr.id
    WHERE td.valid=true AND td.cid=1000 AND pt.valid=true AND pt.status=99 AND pt.cid=1000
      AND wg.dept=1
  ),
  'taskAggregate', (
    SELECT json_build_object(
      'totalNum',total_num,'completeNum',complete_num,'unCompleteNum',uncomplete_num,
      'onTimeNum',on_time_num,'cardSignNum',card_sign_num,'handSignNum',hand_sign_num,
      'completeRate',COALESCE(ROUND(complete_num::numeric/NULLIF(total_num,0),4),0),
      'onTimeRate',COALESCE(ROUND(on_time_num::numeric/NULLIF(complete_num,0),4),0)
    ) FROM task_aggregate
  ),
  'detailAggregate', (
    SELECT json_build_object(
      'totalNum',COUNT(*),
      'faultNum',COUNT(*) FILTER (WHERE real_value='PATROL_realValue/abnormal'),
      'faultRate',COALESCE(ROUND(
        (COUNT(*) FILTER (WHERE real_value='PATROL_realValue/abnormal'))::numeric/NULLIF(COUNT(*),0),4
      ),0)
    ) FROM detail_month
  ),
  'stability', (
    SELECT json_build_object(
      'totalNum',total_num,'normalSampleNum',normal_sample_num,'abnormalSampleNum',abnormal_sample_num
    ) FROM stability_aggregate
  ),
  'gatherError', json_build_object(
    'configuredItemCount', (
      SELECT COUNT(*) FROM public.mp_work_items wi
      JOIN public.mp_work_groups wg ON wi.route_id=wg.id
      JOIN public.mp_input_standards ins ON wi.input_standard_id=ins.id
      WHERE wi.valid=true AND wi.is_run=true AND wi.is_error_bench=true AND wi.cid=1000 AND wg.dept=1
        AND ins.val_type='PATROL_valueType/number' AND ins.edit_type='PATROL_editType/input'
    ),
    'eligibleDetailCount', (SELECT eligible_detail_num FROM gather_aggregate)
  ),
  'monitor', json_build_object(
    'routeCount',(SELECT COUNT(*) FROM public.mp_work_groups WHERE valid=true AND cid=1000),
    'taskCount',(SELECT COUNT(*) FROM public.mp_potrol_tasks WHERE valid=true AND cid=1000),
    'runningCount',(SELECT COUNT(*) FROM public.mp_potrol_tasks WHERE valid=true AND cid=1000 AND task_state='PATROL_taskState/running'),
    'overdueCount',(SELECT COUNT(*) FROM public.mp_potrol_tasks WHERE valid=true AND cid=1000 AND task_state='PATROL_taskState/overdue')
  )
);
`;
  const output = runPostgresSql(sql);
  const lines = output.split(/\r?\n/).filter(Boolean);
  if (!lines.length) {
    throw new Error("PATROL report PostgreSQL verification returned no data");
  }
  return { verificationSql: sql.trim(), values: JSON.parse(lines[lines.length - 1]) };
}

function latestResponseSummary(items, itemCode, urlPart) {
  const item = items.find((candidate) => candidate.code === itemCode);
  if (!item) {
    return null;
  }
  const matching = item.responses.filter(
    (response) => response.url.includes(urlPart) && response.responseSummary
  );
  return matching.length ? matching[matching.length - 1].responseSummary : null;
}

function equivalent(actual, expected) {
  if (actual == null || expected == null) {
    return false;
  }
  const left = Number(actual);
  const right = Number(expected);
  return Number.isFinite(left) && Number.isFinite(right)
    ? Math.abs(left - right) < 0.000001
    : actual === expected;
}

function buildAggregateChecks(items, databaseValues) {
  const resultSummary = latestResponseSummary(items, "potrol-result-summary", "potrolResultSummary-query");
  const abnormalSummary = latestResponseSummary(items, "abnormal-summary", "abnormalSummary-query");
  const taskOverview = latestResponseSummary(items, "task-overview", "data-dg1586331011160");
  const faultStatus = latestResponseSummary(items, "unusual-summary", "getFaultDealStatus");
  const faultDetails = latestResponseSummary(items, "unusual-summary", "getabnormalTaskDetailList");
  const taskStatistics = latestResponseSummary(
    items,
    "task-finish-statistics",
    "getTaskCompleteStatisticsTableData"
  );
  const detailStatistics = latestResponseSummary(
    items,
    "task-finish-statistics",
    "getTaskCompleteStatisticsRC"
  );
  const stabilityChart = latestResponseSummary(items, "stability-rate", "eamStabilityRateChart");
  const stabilityTable = latestResponseSummary(items, "stability-rate", "eamStabilityRate");
  const monitorRoutes = latestResponseSummary(items, "patrol-monitor-map", "/workGroup/selectData");
  const monitorTasks = latestResponseSummary(items, "patrol-monitor-map", "/potrolTask/selectData");
  const taskRow = taskStatistics && taskStatistics.rows && taskStatistics.rows[0];
  const detailRow = detailStatistics && detailStatistics.rows && detailStatistics.rows[0];
  const stateCounts = (monitorTasks && monitorTasks.stateCounts) || {};
  const checks = [];
  const add = (name, api, apiValue, dbValue) => {
    checks.push({
      name,
      api,
      apiValue,
      databaseValue: dbValue,
      status: equivalent(apiValue, dbValue) ? "PASS" : "FAIL",
    });
  };

  add("巡检结果汇总总数", "potrolResultSummary-query", resultSummary && resultSummary.totalCount, databaseValues.resultSummaryCount);
  add("异常巡检结果总数", "abnormalSummary-query", abnormalSummary && abnormalSummary.totalCount, databaseValues.abnormalSummaryCount);
  add("巡检任务概述总数", "data-dg1586331011160", taskOverview && taskOverview.totalCount, databaseValues.taskOverviewCount);
  add("已移交隐患总数", "getabnormalTaskDetailList", faultDetails && faultDetails.totalCount, databaseValues.hiddenDanger.linkedCount);
  add("待处理隐患总数", "getFaultDealStatus", faultStatus && faultStatus.values && faultStatus.values.pending, databaseValues.hiddenDanger.pendingCount);
  add("任务统计总数", "getTaskCompleteStatisticsTableData", taskRow && taskRow.totalNum, databaseValues.taskAggregate.totalNum);
  add("任务统计完成数", "getTaskCompleteStatisticsTableData", taskRow && taskRow.completeNum, databaseValues.taskAggregate.completeNum);
  add("任务统计未完成数", "getTaskCompleteStatisticsTableData", taskRow && taskRow.unCompleteNum, databaseValues.taskAggregate.unCompleteNum);
  add("任务统计按时完成数", "getTaskCompleteStatisticsTableData", taskRow && taskRow.onTimeNum, databaseValues.taskAggregate.onTimeNum);
  add("任务统计完成率", "getTaskCompleteStatisticsTableData", taskRow && taskRow.completeRate, databaseValues.taskAggregate.completeRate);
  add("任务统计按时完成率", "getTaskCompleteStatisticsTableData", taskRow && taskRow.onTimeRate, databaseValues.taskAggregate.onTimeRate);
  add("结果统计总数", "getTaskCompleteStatisticsRC", detailRow && detailRow.totalNum, databaseValues.detailAggregate.totalNum);
  add("结果统计异常数", "getTaskCompleteStatisticsRC", detailRow && detailRow.faultNum, databaseValues.detailAggregate.faultNum);
  add("结果统计异常率", "getTaskCompleteStatisticsRC", detailRow && detailRow.faultRate, databaseValues.detailAggregate.faultRate);
  add("平稳率图表样本数", "eamStabilityRateChart", stabilityChart && stabilityChart.dataCount, databaseValues.stability.totalNum);
  add("平稳率表格样本数", "eamStabilityRate", stabilityTable && stabilityTable.totalCount, databaseValues.stability.totalNum);
  add("平稳率正常样本数", "eamStabilityRateChart", stabilityChart && stabilityChart.valueCounts && stabilityChart.valueCounts["12.34"], databaseValues.stability.normalSampleNum);
  add("平稳率异常样本数", "eamStabilityRateChart", stabilityChart && stabilityChart.valueCounts && stabilityChart.valueCounts["99.99"], databaseValues.stability.abnormalSampleNum);
  add("监控路线数", "workGroup/selectData", monitorRoutes && monitorRoutes.resultCount, databaseValues.monitor.routeCount);
  add("监控任务数", "potrolTask/selectData", monitorTasks && monitorTasks.resultCount, databaseValues.monitor.taskCount);
  add("监控执行中任务数", "potrolTask/selectData", stateCounts["PATROL_taskState/running"] || 0, databaseValues.monitor.runningCount);
  add("监控已超期任务数", "potrolTask/selectData", stateCounts["PATROL_taskState/overdue"] || 0, databaseValues.monitor.overdueCount);

  if (Number(databaseValues.gatherError.configuredItemCount) === 0) {
    checks.push({
      name: "采集误差分析业务前置",
      api: "getworkItemList?type=error",
      apiValue: "NO_ELIGIBLE_ITEM",
      databaseValue: databaseValues.gatherError,
      status: "NOT_APPLICABLE",
    });
  } else {
    const gatherChart = latestResponseSummary(items, "gather-error-analysis", "eamGatherErrorChart");
    add("采集误差分析样本数", "eamGatherErrorChart", gatherChart && gatherChart.dataCount, databaseValues.gatherError.eligibleDetailCount);
  }
  return checks;
}

async function clickQuery(page) {
  for (const frame of page.frames()) {
    const candidates = [
      frame.locator("button[data-id='query']:visible"),
      frame.getByRole("button", { name: /^查询$/ }),
      frame.locator("button:visible", { hasText: /^查询$/ }),
    ];

    for (const candidate of candidates) {
      if ((await candidate.count()) === 0) {
        continue;
      }
      const button = candidate.first();
      if (!(await button.isVisible().catch(() => false))) {
        continue;
      }
      await button.click({ timeout: 5000 });
      await page.waitForLoadState("networkidle", { timeout: 5000 }).catch(() => {});
      await page.waitForTimeout(1800);
      return { attempted: true, clicked: true, error: null, frameUrl: frame.url() };
    }
  }

  return { attempted: false, clicked: false, error: null, frameUrl: null };
}

async function collectFrameText(page) {
  const parts = [];
  for (const frame of page.frames()) {
    const text = await frame.locator("body").innerText({ timeout: 5000 }).catch(() => "");
    if (text.trim()) {
      parts.push(`[frame ${frame.url()}]\n${text}`);
    }
  }
  return parts.join("\n");
}

async function inspectPage(context, target, index) {
  const page = await context.newPage();
  const responses = [];
  const networkErrors = [];
  const consoleErrors = [];
  const pageErrors = [];
  const requestFailures = [];
  const responseReaders = [];

  page.on("response", (response) => {
    const requestItem = response.request();
    const type = requestItem.resourceType();
    if (
      !["document", "xhr", "fetch", "script", "stylesheet"].includes(type) &&
      response.status() < 400
    ) {
      return;
    }

    const reader = (async () => {
      const contentType = response.headers()["content-type"] || "";
      let body = "";
      if (["document", "xhr", "fetch"].includes(type) || response.status() >= 400) {
        body = await response.text().catch(() => "");
      }
      const errorMatch = body.match(visibleErrorPattern);
      let parsedBody = null;
      if (["xhr", "fetch"].includes(type) && body) {
        try {
          parsedBody = JSON.parse(body);
        } catch (_error) {
          parsedBody = null;
        }
      }
      const record = {
        method: requestItem.method(),
        status: response.status(),
        type,
        url: response.url(),
        contentType,
        requestBody: ["xhr", "fetch"].includes(type)
          ? (requestItem.postData() || "").slice(0, 8000)
          : null,
        responseBody:
          ["xhr", "fetch"].includes(type) && /\/PATROL\//i.test(response.url())
            ? body.slice(0, 30000)
            : null,
        responseSummary:
          ["xhr", "fetch"].includes(type) && /\/PATROL\//i.test(response.url())
            ? summarizePatrolPayload(response.url(), parsedBody)
            : null,
        errorSnippet: errorMatch ? body.slice(Math.max(0, errorMatch.index - 80), errorMatch.index + 420) : null,
      };
      if (/\/PATROL\//i.test(record.url) || type === "document" || record.status >= 400 || errorMatch) {
        responses.push(record);
      }
      const htmlAssetFallback =
        ["script", "stylesheet"].includes(type) && response.status() < 400 && /text\/html/i.test(contentType);
      const applicationError =
        record.responseSummary &&
        record.responseSummary.appCode != null &&
        Number(record.responseSummary.appCode) !== 200;
      if (response.status() >= 400 || htmlAssetFallback || errorMatch || applicationError) {
        networkErrors.push(record);
      }
    })();
    responseReaders.push(reader);
  });

  page.on("console", (message) => {
    if (message.type() === "error") {
      const location = message.location();
      const suffix = location.url ? ` @ ${location.url}:${location.lineNumber || 0}` : "";
      consoleErrors.push(`${message.text()}${suffix}`);
    }
  });
  page.on("pageerror", (error) => pageErrors.push(error.message));
  page.on("requestfailed", (requestItem) => {
    if (["document", "xhr", "fetch", "script", "stylesheet"].includes(requestItem.resourceType())) {
      requestFailures.push({
        method: requestItem.method(),
        type: requestItem.resourceType(),
        url: requestItem.url(),
        failure: requestItem.failure() && requestItem.failure().errorText,
      });
    }
  });

  let navigationStatus = null;
  let navigationError = null;
  let query = { attempted: false, clicked: false, error: null, frameUrl: null };
  let preparation = null;
  try {
    const navigation = await page.goto(new URL(target.url, `${baseUrl}/`).toString(), {
      waitUntil: "domcontentloaded",
      timeout: 45000,
    });
    navigationStatus = navigation ? navigation.status() : null;
    await page.waitForLoadState("networkidle", { timeout: 8000 }).catch(() => {});
    await page.waitForTimeout(1800);
    try {
      if (target.code === "gather-error-analysis") {
        preparation = await prepareGatherErrorQuery(page);
      }
      query = await clickQuery(page);
    } catch (error) {
      query = { attempted: true, clicked: false, error: error.message, frameUrl: null };
    }
  } catch (error) {
    navigationError = error.message;
  }

  await Promise.allSettled(responseReaders);
  const bodyText = await collectFrameText(page);
  const title = await page.title().catch(() => "");
  const inspectedText = `${title}\n${bodyText}`;
  const visibleErrorMatch = inspectedText.match(visibleErrorPattern);
  const rawI18nKeys = unique(inspectedText.match(rawI18nPattern) || []);
  const notifications = await collectNotifications(page);
  const loginRedirect = /用户登录|user login/i.test(bodyText) || /\/login(?:\?|$)/i.test(page.url());
  const missingExpectedApis = (target.expectedApis || []).filter(
    (expected) => !responses.some((response) => response.url.includes(expected))
  );
  const screenshot = path.join(outputDir, `${String(index + 1).padStart(2, "0")}-${target.code}.png`);
  await page.screenshot({ path: screenshot, fullPage: true }).catch(() => {});

  const result = {
    ...target,
    resolvedUrl: page.url(),
    navigationStatus,
    navigationError,
    preparation,
    query,
    title,
    bodyPreview: bodyText.slice(0, 1200),
    visibleError: visibleErrorMatch ? visibleErrorMatch[0] : null,
    rawI18nKeys,
    notifications,
    missingExpectedApis,
    loginRedirect,
    responses,
    networkErrors,
    consoleErrors: unique(consoleErrors),
    pageErrors: unique(pageErrors),
    requestFailures,
    screenshot,
  };
  const querySatisfied = target.queryRequired === false || (query.clicked && !query.error);
  const basePass =
    !navigationError &&
    navigationStatus !== null &&
    navigationStatus < 400 &&
    !loginRedirect &&
    !result.visibleError &&
    rawI18nKeys.length === 0 &&
    networkErrors.length === 0 &&
    result.consoleErrors.length === 0 &&
    result.pageErrors.length === 0 &&
    requestFailures.length === 0 &&
    querySatisfied &&
    missingExpectedApis.length === 0;
  const noErrorBenchmarkItem =
    target.code === "gather-error-analysis" &&
    preparation &&
    preparation.reason === "no-error-benchmark-item" &&
    responses.some(
      (response) =>
        response.url.includes("/patrolRoute/workItem/getworkItemList") &&
        response.responseSummary &&
        Number(response.responseSummary.dataCount) === 0
    );
  result.status = basePass ? (noErrorBenchmarkItem ? "NOT_APPLICABLE" : "PASS") : "FAIL";
  result.statusReason = noErrorBenchmarkItem ? "NO_ERROR_BENCHMARK_ITEM_CONFIGURED" : null;

  await page.close();
  return result;
}

async function main() {
  ensureDir(outputDir);
  ensureDir(path.dirname(reportPath));

  const api = await request.newContext({ ignoreHTTPSErrors: true });
  const ticket = await login(api);
  await api.dispose();

  const browser = await chromium.launch({ headless });
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
    for (const key of ["suposTicket", "SUPOS_TICKET", "token"]) {
      window.localStorage.setItem(key, token);
      window.sessionStorage.setItem(key, token);
    }
  }, ticket);

  const items = [];
  for (const [index, target] of targets.entries()) {
    const item = await inspectPage(context, target, index);
    items.push(item);
    console.log(`${item.status} ${index + 1}/${targets.length} ${target.label}`);
  }
  await browser.close();

  let databaseVerification = null;
  let aggregateChecks = [];
  let databaseError = null;
  try {
    databaseVerification = queryPostgresAggregates();
    aggregateChecks = buildAggregateChecks(items, databaseVerification.values);
    const gatherItem = items.find((item) => item.code === "gather-error-analysis");
    if (
      gatherItem &&
      gatherItem.status === "NOT_APPLICABLE" &&
      Number(databaseVerification.values.gatherError.configuredItemCount) > 0
    ) {
      gatherItem.status = "FAIL";
      gatherItem.statusReason = "DATABASE_HAS_ERROR_BENCHMARK_ITEM_BUT_UI_DID_NOT_RETURN_IT";
    }
  } catch (error) {
    databaseError = error.message;
  }

  const summary = {
    total: items.length,
    pass: items.filter((item) => item.status === "PASS").length,
    fail: items.filter((item) => item.status === "FAIL").length,
    blocked: items.filter((item) => item.status === "BLOCKED").length,
    notApplicable: items.filter((item) => item.status === "NOT_APPLICABLE").length,
    aggregateChecks: aggregateChecks.length,
    aggregatePass: aggregateChecks.filter((item) => item.status === "PASS").length,
    aggregateFail: aggregateChecks.filter((item) => item.status === "FAIL").length,
    aggregateNotApplicable: aggregateChecks.filter((item) => item.status === "NOT_APPLICABLE").length,
  };
  const failed = summary.fail > 0 || summary.aggregateFail > 0 || Boolean(databaseError);
  const report = {
    generatedAt: new Date().toISOString(),
    repoCommit: execFileSync("git", ["rev-parse", "HEAD"], { encoding: "utf8" }).trim(),
    reportKind: "patrol-report-browser-api-postgres-acceptance",
    baseUrl,
    username,
    database: "PostgreSQL",
    databaseTarget: dbSshTarget,
    reportMonth,
    outputDir,
    summary,
    items,
    databaseVerification,
    databaseError,
    aggregateChecks,
    status: failed ? "FAIL" : "PASS",
  };
  fs.writeFileSync(reportPath, `${JSON.stringify(report, null, 2)}\n`);
  console.log(`SUMMARY ${JSON.stringify(summary)}`);
  console.log(`REPORT ${reportPath}`);
  if (failed) {
    process.exitCode = 1;
  }
}

main().catch((error) => {
  console.error(error && error.stack ? error.stack : error);
  process.exit(1);
});
