#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { execFileSync, spawnSync } = require("child_process");
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
const navigationWaitUntil = process.env.ADP_NAV_WAIT_UNTIL || "commit";
const mode = process.env.ADP_QCS_REPORT_CHAIN_MODE || "qualified";

if (!["qualified", "unqualified"].includes(mode)) {
  throw new Error("ADP_QCS_REPORT_CHAIN_MODE must be qualified or unqualified");
}

const nowToken = new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14);
const marker =
  process.env.ADP_E2E_MARKER ||
  `ADP_E2E_${nowToken}_${mode === "unqualified" ? "QCS_UNQLF" : "QCS_QUAL"}`;
const outputDir =
  process.env.ADP_OUTPUT_DIR || path.join("/tmp", `adp-qcs-report-chain-${mode}-${nowToken}`);
const outputPath =
  process.env.ADP_QCS_REPORT_CHAIN_PERSISTENCE_OUTPUT ||
  path.join(outputDir, "qcs-report-chain-persistence-results.json");
const womOutputPath = path.join(outputDir, "wom-manu-inspect-persistence-results.json");
const womScript = path.join(__dirname, "adp-wom-manu-inspect-persistence-acceptance.js");
const inspectListRoute = "/msService/QCS/inspect/inspect/manuInspectList";
const reportEditRoute = "/msService/QCS/inspectReport/inspectReport/manuInspReportEdit";
const inspectBulkSubmitApi = "/msService/QCS/inspect/inspect/bulkSubmit";
const reportBatchDealApi = "/msService/QCS/inspectReport/inspectReport/batchDealReports";
const expectedResult = mode === "unqualified" ? "不合格" : "合格";
const expectedBatchResult =
  mode === "unqualified" ? "BaseSet_checkResult/unqualified" : "BaseSet_checkResult/qualified";
const expectAutoUnqualifiedDeal = process.env.ADP_QCS_EXPECT_UNQLF_DEAL !== "false";

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

function rowToObject(row, fields) {
  if (!row) {
    return null;
  }
  return Object.fromEntries(fields.map((field, index) => [field, row[index] || ""]));
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

function runWomManufacturingInspect(evidence) {
  const result = spawnSync(process.execPath, [womScript], {
    encoding: "utf8",
    env: {
      ...process.env,
      ADP_BASE_URL: baseUrl,
      ADP_BROWSER_BASE_URL: browserBaseUrl,
      ADP_USERNAME: username,
      ADP_PASSWORD: password,
      ADP_DB_SSH_TARGET: dbSshTarget,
      ADP_DB_CONTAINER: dbContainer,
      ADP_DB_NAME: dbName,
      ADP_DB_USER: dbUser,
      ADP_E2E_MARKER: marker,
      ADP_OUTPUT_DIR: outputDir,
      ADP_WOM_MANU_INSPECT_PERSISTENCE_OUTPUT: womOutputPath,
      ADP_PAGE_TIMEOUT_MS: String(pageTimeoutMs),
      ADP_NAV_WAIT_UNTIL: navigationWaitUntil,
    },
  });
  evidence.womScript = {
    script: womScript,
    exitCode: result.status,
    stdout: result.stdout.slice(-12000),
    stderr: result.stderr.slice(-12000),
    outputPath: womOutputPath,
  };
  if (result.status !== 0) {
    throw new Error(`WOM manufacturing inspection prerequisite failed: ${result.stderr || result.stdout}`);
  }
  const womEvidence = JSON.parse(fs.readFileSync(womOutputPath, "utf8"));
  evidence.womManuInspect = {
    status: womEvidence.status,
    marker: womEvidence.marker,
    taskId: womEvidence.ids && womEvidence.ids.task,
    batchNo: womEvidence.batchNo,
    inspectId: womEvidence.persistence && womEvidence.persistence.inspect && womEvidence.persistence.inspect[1],
    createManuInspect: womEvidence.createManuInspect,
    frontendClean: womEvidence.frontendClean,
  };
  if (!evidence.womManuInspect.inspectId) {
    throw new Error(`WOM prerequisite did not expose qcs_inspects.id: ${JSON.stringify(evidence.womManuInspect)}`);
  }
  return womEvidence;
}

function queryInspectState(inspectId) {
  const fields = [
    "id",
    "version",
    "status",
    "tableNo",
    "tableInfoId",
    "batchCode",
    "checkState",
    "refable",
    "valid",
    "pendingId",
    "pendingDeploymentId",
    "pendingActivityName",
    "pendingTaskDescription",
    "pendingProcessKey",
  ];
  const raw = runSql(`
SELECT qi.id, qi.version, qi.status, qi.table_no, qi.table_info_id,
       coalesce(qi.batch_code, ''), coalesce(qi.check_state, ''),
       coalesce(qi.refable::text, ''), coalesce(qi.valid::text, ''),
       coalesce(p.id::text, ''), coalesce(p.deployment_id::text, ''),
       coalesce(p.activity_name, ''), coalesce(p.task_description, ''),
       coalesce(p.process_key, '')
FROM public.qcs_inspects qi
LEFT JOIN LATERAL (
  SELECT id, deployment_id, activity_name, task_description, process_key
  FROM public.wfm_task_pending p
  WHERE p.table_info_id = qi.table_info_id
    AND p.task_status IN (1, 88)
  ORDER BY p.id DESC
  LIMIT 1
) p ON true
WHERE qi.id = ${inspectId};
`);
  return rowToObject(parseRows(raw)[0], fields);
}

function queryReportState(inspectId, batchNo) {
  const fields = [
    "id",
    "version",
    "status",
    "tableNo",
    "tableInfoId",
    "batchCode",
    "checkResult",
    "checkResCode",
    "memoField",
    "unQlfDealFlag",
    "actBatStateId",
    "valid",
    "pendingId",
    "pendingDeploymentId",
    "pendingActivityName",
    "pendingTaskDescription",
    "pendingProcessKey",
    "pendingOpenUrl",
  ];
  const raw = runSql(`
SELECT r.id, r.version, r.status, r.table_no, r.table_info_id,
       coalesce(r.batch_code, ''), coalesce(r.check_result, ''),
       coalesce(r.check_res_code, ''), coalesce(r.memo_field, ''),
       coalesce(r.un_qlf_deal_flag::text, ''), coalesce(r.act_bat_state_id::text, ''),
       coalesce(r.valid::text, ''),
       coalesce(p.id::text, ''), coalesce(p.deployment_id::text, ''),
       coalesce(p.activity_name, ''), coalesce(p.task_description, ''),
       coalesce(p.process_key, ''), coalesce(p.open_url, '')
FROM public.qcs_inspect_reports r
LEFT JOIN LATERAL (
  SELECT id, deployment_id, activity_name, task_description, process_key, open_url
  FROM public.wfm_task_pending p
  WHERE p.table_info_id = r.table_info_id
    AND p.task_status IN (1, 88)
  ORDER BY p.id DESC
  LIMIT 1
) p ON true
WHERE r.inspect_id = ${inspectId}
   OR r.batch_code = ${sqlLiteral(batchNo)}
ORDER BY r.id DESC
LIMIT 1;
`);
  return rowToObject(parseRows(raw)[0], fields);
}

function queryReportComs(reportId) {
  const fields = ["id", "version", "reportId", "reportName", "dispValue", "checkResult", "valid"];
  const raw = runSql(`
SELECT id, version, report_id, coalesce(report_name, ''), coalesce(disp_value, ''),
       coalesce(check_result, ''), coalesce(valid::text, '')
FROM public.qcs_report_coms
WHERE report_id = ${reportId}
ORDER BY id;
`);
  return parseRows(raw).map((row) => rowToObject(row, fields));
}

function queryFinalState(womEvidence, reportId, inspectId, batchNo) {
  const taskId = womEvidence.ids.task;
  const raw = runSql(`
SELECT 'report', id, status, version, coalesce(check_result, ''), coalesce(check_res_code, ''),
       coalesce(memo_field, ''), coalesce(un_qlf_deal_flag::text, ''), coalesce(act_bat_state_id::text, '')
FROM public.qcs_inspect_reports
WHERE id = ${reportId};

SELECT 'reportComCount', count(*)::text,
       coalesce(string_agg(coalesce(check_result, '') || ':' || coalesce(disp_value, ''), ',' ORDER BY id), '')
FROM public.qcs_report_coms
WHERE report_id = ${reportId};

SELECT 'reportPendingCount', count(*)::text
FROM public.wfm_task_pending
WHERE table_info_id = (SELECT table_info_id FROM public.qcs_inspect_reports WHERE id = ${reportId})
  AND task_status IN (1, 88);

SELECT 'inspect', id, status, coalesce(check_state, ''), coalesce(refable::text, '')
FROM public.qcs_inspects
WHERE id = ${inspectId};

SELECT 'task', id, coalesce(check_state, ''), coalesce(check_result, ''),
       coalesce(check_result_id::text, ''), coalesce(inpect_deal_id::text, ''), coalesce(rejects_deal_id::text, '')
FROM public.wom_produce_tasks
WHERE id = ${taskId};

SELECT 'wait', id, coalesce(check_state, ''), coalesce(check_result, '')
FROM public.wom_wait_put_records
WHERE task_id = ${taskId}
ORDER BY id;

SELECT 'exelog', id, coalesce(check_state, ''), coalesce(check_result, ''),
       coalesce(inpect_deal_id::text, ''), coalesce(rejects_deal_id::text, '')
FROM public.wom_produce_task_exelog
WHERE task_id = ${taskId}
ORDER BY id;

SELECT 'batch', id, coalesce(batch_num, ''), coalesce(check_state, ''), coalesce(check_result, ''),
       coalesce(active_batch_state_id::text, ''), coalesce(is_available::text, ''), coalesce(rejects_deal_id::text, '')
FROM public.baseset_batch_infos
WHERE batch_num = ${sqlLiteral(batchNo)}
ORDER BY id;

SELECT 'deal', coalesce(id::text, ''), coalesce(status::text, ''), coalesce(table_no, ''),
       coalesce(report_id::text, ''), coalesce(batch_code, ''), coalesce(act_bat_state_id::text, '')
FROM public.qcs_un_qlf_deals
WHERE report_id = ${reportId}
ORDER BY id;
`);
  const rows = parseRows(raw);
  return {
    raw,
    rows,
    report: rows.find((row) => row[0] === "report") || null,
    reportComCount: rows.find((row) => row[0] === "reportComCount") || null,
    reportPendingCount: rows.find((row) => row[0] === "reportPendingCount") || null,
    inspect: rows.find((row) => row[0] === "inspect") || null,
    task: rows.find((row) => row[0] === "task") || null,
    wait: rows.find((row) => row[0] === "wait") || null,
    exelog: rows.find((row) => row[0] === "exelog") || null,
    batch: rows.find((row) => row[0] === "batch") || null,
    deal: rows.find((row) => row[0] === "deal") || null,
  };
}

function buildReportJson(report, reportComs) {
  const reportPayload = {
    id: report.id,
    version: Number(report.version),
    checkResult: expectedResult,
    memoField: `${marker} ${mode} memo`,
    pending: report.pendingId
      ? {
          id: report.pendingId,
          deploymentId: report.pendingDeploymentId,
          activityName: report.pendingActivityName,
        }
      : null,
  };
  const comPayload = reportComs.map((row) => ({
    id: row.id,
    version: Number(row.version),
    checkResult: expectedResult,
    dispValue: expectedResult,
  }));
  return JSON.stringify([
    {
      reportId: JSON.stringify(reportPayload),
      reportComs: JSON.stringify(comPayload),
    },
  ]);
}

async function postForm(page, apiPath, form) {
  return page.evaluate(
    async ({ apiPath: pathValue, formEntries }) => {
      const body = new URLSearchParams(formEntries);
      const response = await fetch(pathValue, {
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
        url: pathValue,
        requestPayload: body.toString(),
        status: response.status,
        body: text.slice(0, 8000),
        json,
      };
    },
    { apiPath, formEntries: Object.entries(form) }
  );
}

async function safeScreenshot(page, evidence, pathValue) {
  try {
    await page.screenshot({ path: pathValue, fullPage: true, timeout: 15000 });
  } catch (error) {
    evidence.screenshotErrors.push({ path: pathValue, error: error.message });
  }
}

async function runBrowserWorkflow(ticket, womEvidence, evidence) {
  const inspectId = evidence.womManuInspect.inspectId;
  const batchNo = evidence.womManuInspect.batchNo;
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
      if (/QCS\/inspect|QCS\/inspectReport|WOM\/quality|layoutJson/.test(url)) {
        evidence.requests.push({ method: requestItem.method(), url, postData: requestItem.postData() });
      }
    });
    page.on("response", async (response) => {
      const url = response.url();
      if (!/QCS\/inspect|QCS\/inspectReport|WOM\/quality|layoutJson/.test(url)) {
        return;
      }
      let body = "";
      try {
        body = (await response.text()).slice(0, 8000);
      } catch (_error) {
        body = "";
      }
      evidence.responses.push({ method: response.request().method(), url, status: response.status(), body });
    });

    const inspectNav = await page.goto(`${inspectListRoute}?ADP_E2E=${encodeURIComponent(marker)}`, {
      waitUntil: navigationWaitUntil,
      timeout: pageTimeoutMs,
    });
    evidence.inspectListNavigation = {
      route: inspectListRoute,
      status: inspectNav && inspectNav.status(),
    };
    evidence.screenshots.inspectList = path.join(outputDir, "qcs-inspect-list-before-submit.png");
    await safeScreenshot(page, evidence, evidence.screenshots.inspectList);

    for (let attempt = 1; attempt <= 3; attempt += 1) {
      const inspectState = queryInspectState(inspectId);
      const existingReport = queryReportState(inspectId, batchNo);
      evidence.inspectBulkSubmitAttempts.push({ attempt, before: inspectState, existingReport });
      if (existingReport && existingReport.id) {
        break;
      }
      if (!inspectState || !inspectState.pendingId) {
        throw new Error(`QCS inspect pending missing before report generation: ${JSON.stringify(inspectState)}`);
      }
      const response = await postForm(page, inspectBulkSubmitApi, {
        ids: inspectState.id,
        deploymentId: inspectState.pendingDeploymentId,
        pendingIds: inspectState.pendingId,
        activityName: inspectState.pendingActivityName,
      });
      evidence.inspectBulkSubmitAttempts[evidence.inspectBulkSubmitAttempts.length - 1].response = response;
      if (response.status !== 200) {
        throw new Error(`QCS inspect bulkSubmit failed: ${JSON.stringify(response)}`);
      }
      await page.waitForTimeout(1200);
      const afterReport = queryReportState(inspectId, batchNo);
      evidence.inspectBulkSubmitAttempts[evidence.inspectBulkSubmitAttempts.length - 1].afterReport = afterReport;
      if (afterReport && afterReport.id) {
        break;
      }
    }

    let report = queryReportState(inspectId, batchNo);
    if (!report || !report.id) {
      throw new Error(`QCS report was not generated for inspect ${inspectId}`);
    }
    evidence.generatedReport = report;

    const reportNav = await page.goto(
      `${reportEditRoute}?id=${encodeURIComponent(report.id)}&pendingId=${encodeURIComponent(report.pendingId || "")}`,
      { waitUntil: navigationWaitUntil, timeout: pageTimeoutMs }
    );
    evidence.reportEditNavigation = {
      route: reportEditRoute,
      status: reportNav && reportNav.status(),
      reportId: report.id,
      pendingId: report.pendingId,
    };
    await page.waitForTimeout(2500);
    evidence.reportEditBodyHasMarker = (await page.locator("body").innerText({ timeout: pageTimeoutMs })).includes(
      batchNo
    );
    evidence.screenshots.reportEdit = path.join(outputDir, "qcs-report-edit-before-save.png");
    await safeScreenshot(page, evidence, evidence.screenshots.reportEdit);

    const reportComsBeforeSave = queryReportComs(report.id);
    if (!reportComsBeforeSave.length) {
      throw new Error(`QCS report has no qcs_report_coms rows: ${JSON.stringify(report)}`);
    }
    const saveJson = buildReportJson(report, reportComsBeforeSave);
    const saveResponse = await postForm(page, reportBatchDealApi, {
      reportJSon: saveJson,
      dealType: "save",
    });
    evidence.reportSave = {
      before: report,
      reportComsBefore: reportComsBeforeSave,
      requestReportJson: saveJson,
      response: saveResponse,
    };
    if (saveResponse.status !== 200) {
      throw new Error(`QCS report save failed: ${JSON.stringify(saveResponse)}`);
    }
    await page.waitForTimeout(1200);
    report = queryReportState(inspectId, batchNo);
    evidence.reportSave.after = report;

    for (let attempt = 1; attempt <= 3; attempt += 1) {
      report = queryReportState(inspectId, batchNo);
      const reportComs = queryReportComs(report.id);
      evidence.reportSubmitAttempts.push({ attempt, before: report, reportComs });
      if (report.status === "99") {
        break;
      }
      if (!report.pendingId) {
        throw new Error(`QCS report pending missing before submit: ${JSON.stringify(report)}`);
      }
      const reportJson = buildReportJson(report, reportComs);
      const response = await postForm(page, reportBatchDealApi, {
        reportJSon: reportJson,
        dealType: "submit",
      });
      evidence.reportSubmitAttempts[evidence.reportSubmitAttempts.length - 1].requestReportJson = reportJson;
      evidence.reportSubmitAttempts[evidence.reportSubmitAttempts.length - 1].response = response;
      if (response.status !== 200) {
        throw new Error(`QCS report submit failed: ${JSON.stringify(response)}`);
      }
      await page.waitForTimeout(1500);
      evidence.reportSubmitAttempts[evidence.reportSubmitAttempts.length - 1].after = queryReportState(
        inspectId,
        batchNo
      );
      if (evidence.reportSubmitAttempts[evidence.reportSubmitAttempts.length - 1].after.status === "99") {
        break;
      }
    }

    evidence.screenshots.reportAfterSubmit = path.join(outputDir, "qcs-report-edit-after-submit.png");
    await safeScreenshot(page, evidence, evidence.screenshots.reportAfterSubmit);
    await context.close();
  } finally {
    await browser.close();
  }
}

function assertFrontendClean(evidence) {
  const failedResponses = evidence.responses.filter((response) => response.status >= 400);
  const consoleErrors = evidence.console.filter((message) => message.type === "error");
  const blockingRequestFailures = evidence.requestFailures.filter(
    (failure) =>
      !(
        failure.method === "GET" &&
        failure.failure === "net::ERR_ABORTED" &&
        /\/greenDill\/static\/.*\/i18n-value\.js/.test(failure.url)
      )
  );
  const failures = [];
  if (failedResponses.length) {
    failures.push(`HTTP ${failedResponses.map((response) => `${response.status} ${response.url}`).join("; ")}`);
  }
  if (blockingRequestFailures.length) {
    failures.push(`request failures ${JSON.stringify(blockingRequestFailures)}`);
  }
  if (consoleErrors.length) {
    failures.push(`console errors ${JSON.stringify(consoleErrors)}`);
  }
  if (evidence.pageErrors.length) {
    failures.push(`page errors ${JSON.stringify(evidence.pageErrors)}`);
  }
  evidence.frontendClean = {
    failedResponses,
    requestFailures: blockingRequestFailures,
    ignoredRequestFailures: evidence.requestFailures.filter(
      (failure) => !blockingRequestFailures.includes(failure)
    ),
    consoleErrors,
    pageErrors: evidence.pageErrors,
    status: failures.length ? "FAIL" : "PASS",
  };
  if (failures.length) {
    throw new Error(`Frontend acceptance had blocking errors: ${failures.join(" | ")}`);
  }
}

function valueIncludes(row, expected) {
  return row && row.some((value) => String(value || "").includes(expected));
}

function isExpectedWomResult(value) {
  const normalized = String(value || "");
  if (mode === "unqualified") {
    return ["不合格", "Unqualified", "BaseSet_checkResult/unqualified"].includes(normalized);
  }
  return ["合格", "Qualified", "BaseSet_checkResult/qualified"].includes(normalized);
}

function assertPersistence(evidence, womEvidence) {
  const inspectId = evidence.womManuInspect.inspectId;
  const batchNo = evidence.womManuInspect.batchNo;
  const report = queryReportState(inspectId, batchNo);
  if (!report || !report.id) {
    throw new Error(`QCS report missing in final assertion for inspect ${inspectId}`);
  }
  const finalState = queryFinalState(womEvidence, report.id, inspectId, batchNo);
  evidence.finalVerificationSql = finalState.raw;
  evidence.finalState = finalState;

  const failures = [];
  if (!finalState.report || finalState.report[2] !== "99") {
    failures.push(`qcs_inspect_reports not effective: ${JSON.stringify(finalState.report)}`);
  }
  if (!finalState.report || finalState.report[4] !== expectedResult) {
    failures.push(`qcs_inspect_reports check_result mismatch: ${JSON.stringify(finalState.report)}`);
  }
  if (!valueIncludes(finalState.report, marker)) {
    failures.push(`qcs_inspect_reports memo marker missing: ${JSON.stringify(finalState.report)}`);
  }
  if (!finalState.reportComCount || Number(finalState.reportComCount[1] || 0) < 1) {
    failures.push(`qcs_report_coms were not persisted: ${JSON.stringify(finalState.reportComCount)}`);
  }
  if (!finalState.reportPendingCount || finalState.reportPendingCount[1] !== "0") {
    failures.push(`report pending rows were not cleared: ${JSON.stringify(finalState.reportPendingCount)}`);
  }
  if (!finalState.inspect || finalState.inspect[3] !== "QCS_checkState/reported") {
    failures.push(`qcs_inspects not reported: ${JSON.stringify(finalState.inspect)}`);
  }
  if (!finalState.task || !isExpectedWomResult(finalState.task[3])) {
    failures.push(`wom_produce_tasks check_result mismatch: ${JSON.stringify(finalState.task)}`);
  }
  if (!finalState.wait || !isExpectedWomResult(finalState.wait[3])) {
    failures.push(`wom_wait_put_records check_result mismatch: ${JSON.stringify(finalState.wait)}`);
  }
  if (!finalState.exelog || !isExpectedWomResult(finalState.exelog[3])) {
    failures.push(`wom_produce_task_exelog check_result mismatch: ${JSON.stringify(finalState.exelog)}`);
  }
  if (!finalState.batch || finalState.batch[4] !== expectedBatchResult) {
    failures.push(`baseset_batch_infos check_result mismatch: ${JSON.stringify(finalState.batch)}`);
  }
  if (mode === "unqualified" && expectAutoUnqualifiedDeal && !finalState.deal) {
    failures.push("qcs_un_qlf_deals auto treatment document was not generated for unqualified report");
  }
  if (failures.length) {
    throw new Error(failures.join("; "));
  }
  return { report, finalState };
}

async function main() {
  ensureDir(outputDir);
  const evidence = {
    generatedAt: new Date().toISOString(),
    baseUrl,
    browserBaseUrl,
    navigationWaitUntil,
    mode,
    marker,
    operation: `QCS manufacturing report save and ${mode} effective persistence`,
    api: `${inspectBulkSubmitApi}; ${reportBatchDealApi}`,
    backendEntry:
      "QCSInspectController.bulkSubmit -> QCSInspectServiceImpl.bulkSubmit -> QCSInspectReportServiceImpl.batchDealReports -> QCSInspectReportServiceImpl.bulkSubmit -> sendReportResultToWOM -> WOMQCSServiceImpl.checkReportBackfillWom",
    targetTables: [
      "qcs_inspects",
      "qcs_inspects_di",
      "qcs_inspect_reports",
      "qcs_report_coms",
      "wfm_task_pending",
      "wf_deal_info",
      "wom_produce_tasks",
      "wom_wait_put_records",
      "wom_produce_task_exelog",
      "baseset_batch_infos",
      "qcs_un_qlf_deals",
    ],
    console: [],
    pageErrors: [],
    requestFailures: [],
    requests: [],
    responses: [],
    screenshots: {},
    screenshotErrors: [],
    inspectBulkSubmitAttempts: [],
    reportSubmitAttempts: [],
  };

  try {
    const womEvidence = runWomManufacturingInspect(evidence);
    const api = await request.newContext({ ignoreHTTPSErrors: true });
    const loginResult = await login(api);
    await api.dispose();
    evidence.login = { status: loginResult.status, ticket: Boolean(loginResult.ticket) };

    await runBrowserWorkflow(loginResult.ticket, womEvidence, evidence);
    assertFrontendClean(evidence);
    const persistence = assertPersistence(evidence, womEvidence);
    evidence.persistence = {
      report: persistence.report,
      finalState: persistence.finalState,
    };
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
        mode,
        marker,
        outputPath,
        womManuInspect: evidence.womManuInspect,
        reportSave: evidence.reportSave && {
          status: evidence.reportSave.response.status,
          after: evidence.reportSave.after,
        },
        finalState: evidence.finalState && {
          report: evidence.finalState.report,
          task: evidence.finalState.task,
          wait: evidence.finalState.wait,
          exelog: evidence.finalState.exelog,
          batch: evidence.finalState.batch,
          deal: evidence.finalState.deal,
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
