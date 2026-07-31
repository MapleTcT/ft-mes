#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { execFileSync } = require("child_process");
const { chromium, request } = require("playwright");

const baseUrl = (process.env.ADP_BASE_URL || "http://10.11.100.17:18080").replace(/\/+$/, "");
const browserBaseUrl = (process.env.ADP_BROWSER_BASE_URL || baseUrl).replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD || "123456";
const sshTarget = process.env.ADP_DB_SSH_TARGET || "v6@10.11.100.17";
const dbContainer = process.env.ADP_DB_CONTAINER || "adp-mes-newbase-postgres-1";
const dbName = process.env.ADP_DB_NAME || "adp";
const dbUser = process.env.ADP_DB_USER || "adp";
const timeoutMs = Number(process.env.ADP_PAGE_TIMEOUT_MS || 120000);
const headless = process.env.ADP_HEADLESS !== "false";
const taskExecutionId = String(process.env.ADP_WOM_TASK_EXECUTION_ID || "771353614402816");
const activityExecutionId = String(process.env.ADP_WOM_ACTIVITY_EXECUTION_ID || "771353684604160");
const outputActivityExecutionId = String(
  process.env.ADP_WOM_OUTPUT_ACTIVITY_EXECUTION_ID || "9007185760134776"
);
const outputPath =
  process.env.ADP_WOM_EXECUTION_RECORD_ACTIONS_OUTPUT ||
  "metadata/wom-execution-record-actions-acceptance-20260731.json";
const screenshotDir =
  process.env.ADP_WOM_EXECUTION_RECORD_ACTIONS_SCREENSHOT_DIR || "metadata";
const marker = `ADP_E2E_${new Date().toISOString().replace(/\D/g, "").slice(0, 14)}_WOM_RECORD_ACTIONS`;

const pageCases = [
  {
    key: "task",
    module: "指令执行记录",
    route: "/msService/WOM/produceTask/prodTaskExelog/makeTaskExecuList",
    query: "/msService/WOM/produceTask/prodTaskExelog/makeTaskExecuList-query",
    permissionCode: "WOM_1.0.0_produceTask_makeTaskExecuList",
    gridId: "WOM_1.0.0_produceTask_makeTaskExecuList_prodTaskExelog_sdg",
    preferredId: taskExecutionId,
    expectedButtons: [
      ["btn-outptConsumption", "产耗查看"],
      ["btn-analysisView", "工艺查看"],
      ["btn-batchReportPreview", "批次追溯"],
      ["btn-manualStatistics", "工艺统计"],
    ],
    noSelectionButton: "btn-outptConsumption",
    noSelectionText: "请选择一条指令执行记录",
    detailButton: "btn-outptConsumption",
    detailRoute: "/WOM/produceTask/prodTaskExelog/matConsumEntryView",
    doubleClickRoute: "/WOM/produceTask/prodTaskExelog/matConsumEntryView",
    bodyHints: ["投入记录", "产出记录", "消耗记录"],
  },
  {
    key: "activity",
    module: "活动执行记录",
    route: "/msService/WOM/produceTask/actiExelog/activeExeLogList",
    query: "/msService/WOM/produceTask/actiExelog/activeExeLogList-query",
    permissionCode: "WOM_1.0.0_produceTask_activeExeLogList",
    gridId: "WOM_1.0.0_produceTask_activeExeLogList_actiExelog_sdg",
    preferredId: activityExecutionId,
    expectedButtons: [
      ["btn-viewDetail", "查看详情"],
      ["btn-manualStatistics", "工艺统计"],
    ],
    noSelectionButton: "btn-viewDetail",
    noSelectionText: "请选择一条活动执行记录",
    detailButton: "btn-viewDetail",
    detailRoute: "/WOM/produceTask/actiExelog/matConsuEntryActView",
    doubleClickRoute: "/WOM/produceTask/actiExelog/matConsuEntryActView",
    bodyHints: ["投入"],
  },
  {
    key: "check",
    module: "检查记录",
    route: "/msService/WOM/produceTask/checkRecord/checkRecordList",
    query: "/msService/WOM/produceTask/checkRecord/checkRecordList-query",
    permissionCode: "WOM_1.0.0_produceTask_checkRecordList",
    gridId: "WOM_1.0.0_produceTask_checkRecordList_checkRecord_sdg",
    expectedButtons: [["btn-viewDetail", "查看详情"]],
    noSelectionButton: "btn-viewDetail",
    noSelectionText: "请选择一条有效的检查记录",
    detailButton: "btn-viewDetail",
    detailRoute: "/WOM/produceTask/actiExelog/checkRecord",
    doubleClickRoute: "/WOM/produceTask/actiExelog/checkRecord",
    bodyHints: ["检查"],
  },
  {
    key: "activity-output",
    module: "活动执行记录（产出活动）",
    route: "/msService/WOM/produceTask/actiExelog/activeExeLogList",
    query: "/msService/WOM/produceTask/actiExelog/activeExeLogList-query",
    permissionCode: "WOM_1.0.0_produceTask_activeExeLogList",
    gridId: "WOM_1.0.0_produceTask_activeExeLogList_actiExelog_sdg",
    preferredId: outputActivityExecutionId,
    rowEndpoint: `/msService/WOM/produceTask/actiExelog/data/${outputActivityExecutionId}`,
    expectedButtons: [
      ["btn-viewDetail", "查看详情"],
      ["btn-manualStatistics", "工艺统计"],
    ],
    noSelectionButton: "btn-viewDetail",
    noSelectionText: "请选择一条活动执行记录",
    detailButton: "btn-viewDetail",
    detailRoute: "/WOM/produceTask/actiExelog/matOutputActView",
    doubleClickRoute: "/WOM/produceTask/actiExelog/matOutputActView",
    bodyHints: ["产出"],
  },
  {
    key: "input",
    module: "投料记录",
    route: "/msService/WOM/produceTask/matConsumRecod/matConsumRecodList",
    query: "/msService/WOM/produceTask/matConsumRecod/matConsumRecodList-query",
    permissionCode: "WOM_1.0.0_produceTask_matConsumRecodList",
    gridId: "WOM_1.0.0_produceTask_matConsumRecodList_matConsumRecod_sdg",
    expectedButtons: [["btn-viewDetail", "查看详情"]],
    noSelectionButton: "btn-viewDetail",
    noSelectionText: "请选择一条投料记录",
    detailButton: "btn-viewDetail",
    detailRoute: "/WOM/produceTask/matConsumRecod/matConsumeRecordView",
    doubleClickRoute: "/WOM/produceTask/matConsumRecod/matConsumeRecordView",
    bodyHints: ["投料"],
  },
  {
    key: "output",
    module: "产出记录",
    route: "/msService/WOM/produceTask/matOutptRecord/matOutptRecordList",
    query: "/msService/WOM/produceTask/matOutptRecord/matOutptRecordList-query",
    permissionCode: "WOM_1.0.0_produceTask_matOutptRecordList",
    gridId: "WOM_1.0.0_produceTask_matOutptRecordList_matOutptRecord_sdg",
    expectedButtons: [["btn-viewDetail", "查看详情"]],
    noSelectionButton: "btn-viewDetail",
    noSelectionText: "请选择一条产出记录",
    detailButton: "btn-viewDetail",
    detailRoute: "/WOM/produceTask/matOutptRecord/matOutputRecordView",
    doubleClickRoute: "/WOM/produceTask/matOutptRecord/matOutputRecordView",
    bodyHints: ["产出"],
  },
];

for (const value of [taskExecutionId, activityExecutionId, outputActivityExecutionId]) {
  if (!/^\d+$/.test(value)) {
    throw new Error("Controlled WOM execution IDs must be numeric.");
  }
}

function ensureDir(filePath) {
  fs.mkdirSync(path.dirname(path.resolve(filePath)), { recursive: true });
}

function shellQuote(value) {
  return `'${String(value).replace(/'/g, "'\\''")}'`;
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
  return execFileSync(
    "ssh",
    [
      "-o",
      "BatchMode=yes",
      "-o",
      "ConnectTimeout=8",
      "-o",
      "StrictHostKeyChecking=no",
      "-o",
      "UserKnownHostsFile=/dev/null",
      sshTarget,
      command,
    ],
    { input: sql, encoding: "utf8", stdio: ["pipe", "pipe", "pipe"] }
  ).trim();
}

function parsePipeRows(raw) {
  return raw
    .split(/\r?\n/)
    .filter(Boolean)
    .map((line) => line.split("|"));
}

function sqlBoolean(value) {
  if (value === "t" || value === "true") {
    return "true";
  }
  if (value === "f" || value === "false") {
    return "false";
  }
  if (value === "") {
    return "null";
  }
  throw new Error(`Unexpected PostgreSQL boolean value: ${value}`);
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
  const failures = [];
  for (const data of [
    { userName: username, password, clientId: "pc_dt" },
    { username, password, clientId: "pc_dt" },
  ]) {
    const response = await api.post(`${baseUrl}/inter-api/auth/login`, {
      data,
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
    failures.push({ status: response.status(), body: parsed.text.slice(0, 300) });
  }
  throw new Error(`Login failed: ${JSON.stringify(failures)}`);
}

function messageProbe() {
  window.__adpWomRecordMessages = [];
  window.setInterval(function patchMessageApi() {
    var api = window.ReactAPI;
    if (!api || api.__womRecordMessageProbe || typeof api.showMessage !== "function") {
      return;
    }
    var original = api.showMessage;
    api.showMessage = function showMessageWithProbe(type, message) {
      window.__adpWomRecordMessages.push([String(type), String(message)]);
      return original.apply(this, arguments);
    };
    api.__womRecordMessageProbe = true;
  }, 100);
}

function ignoreRequestFailure(url) {
  return /favicon\.ico|sockjs|websocket|\/ws(?:\/|$)/i.test(url);
}

function attachPageEvidence(page, report, label) {
  page.on("console", (message) => {
    if (message.type() === "error") {
      report.browser.consoleErrors.push({
        page: label,
        text: message.text().slice(0, 1000),
      });
    } else if (message.type() === "warning") {
      report.browser.consoleWarnings.push({
        page: label,
        text: message.text().slice(0, 1000),
      });
    }
  });
  page.on("pageerror", (error) => {
    report.browser.pageErrors.push({ page: label, error: error.message });
  });
  page.on("requestfailed", (requestItem) => {
    if (ignoreRequestFailure(requestItem.url())) {
      return;
    }
    report.browser.requestFailures.push({
      page: label,
      method: requestItem.method(),
      url: requestItem.url(),
      error: requestItem.failure() && requestItem.failure().errorText,
    });
  });
  page.on("response", (response) => {
    const url = response.url();
    if (
      /layoutJson|\/data\/\d+|makeTaskExecuList|activeExeLogList|checkRecordList|matConsumRecodList|matOutptRecordList|analysisiTask|manualStatActive|processBatchViewOut/.test(
        url
      )
    ) {
      report.http.push({
        page: label,
        method: response.request().method(),
        url,
        status: response.status(),
      });
    }
    if (response.status() >= 400 && !ignoreRequestFailure(url)) {
      report.browser.failedResponses.push({
        page: label,
        method: response.request().method(),
        url,
        status: response.status(),
      });
    }
  });
}

async function waitForGrid(page, gridId) {
  await page.waitForFunction(
    (code) => {
      const factory = window.ReactAPI && window.ReactAPI.getComponentAPI("SupDataGrid");
      if (!factory || typeof factory.APIs !== "function") {
        return false;
      }
      const grid = factory.APIs(code);
      return Boolean(grid && typeof grid.getRows === "function");
    },
    gridId,
    { timeout: timeoutMs }
  );
}

async function fetchRows(page, pageCase) {
  const response = await page.request.post(`${baseUrl}${pageCase.query}`, {
    data: {
      classifyCodes: "",
      customCondition: {},
      permissionCode: pageCase.permissionCode,
      pageNo: 1,
      paging: true,
      pageSize: 65535,
      crossCompanyFlag: "true",
    },
  });
  const parsed = await readJsonSafe(response);
  const rows =
    (parsed.json && parsed.json.data && parsed.json.data.result) ||
    (parsed.json && parsed.json.data && parsed.json.data.rows) ||
    (parsed.json && parsed.json.data && parsed.json.data.records) ||
    [];
  if (!response.ok() || !Array.isArray(rows) || !rows.length) {
    throw new Error(
      `${pageCase.module} list query returned no rows: HTTP ${response.status()} ${parsed.text.slice(
        0,
        300
      )}`
    );
  }
  return rows;
}

async function fetchRealRow(page, pageCase) {
  const response = await page.request.get(`${baseUrl}${pageCase.rowEndpoint}`);
  const parsed = await readJsonSafe(response);
  const row = parsed.json && parsed.json.data;
  if (!response.ok() || !row || String(row.id) !== String(pageCase.preferredId)) {
    throw new Error(
      `${pageCase.module} real row API failed: HTTP ${response.status()} ${parsed.text.slice(
        0,
        300
      )}`
    );
  }
  return {
    row,
    evidence: {
      source: "detail-api",
      endpoint: pageCase.rowEndpoint,
      status: response.status(),
      id: String(row.id),
    },
  };
}

function chooseRow(rows, pageCase) {
  if (pageCase.preferredId) {
    const preferred = rows.find((row) => String(row.id) === String(pageCase.preferredId));
    if (!preferred) {
      throw new Error(
        `${pageCase.module} controlled row ${pageCase.preferredId} was not returned by the list API`
      );
    }
    return preferred;
  }
  return rows[0];
}

async function injectAndSelectRow(page, pageCase, row) {
  const selection = await page.evaluate(
    ({ code, target }) => {
      const grid = ReactAPI.getComponentAPI("SupDataGrid").APIs(code);
      grid.setDatagridData([target]);
      grid.setSelecteds("0");
      const selected = (grid.getSelecteds && grid.getSelecteds()[0]) || {};
      return {
        id: String(selected.id || ""),
        activeType:
          selected.activeType && typeof selected.activeType === "object"
            ? selected.activeType.id
            : selected.activeType,
        batchNo: selected.produceBatchNum || selected.matBatchNum,
        needParamAna: selected.needParamAna,
        analysisFlag: selected.analysisFlag,
      };
    },
    { code: pageCase.gridId, target: row }
  );
  await page.waitForTimeout(200);
  return selection;
}

async function readToolbar(page) {
  return page.locator("[id^=btn-]").evaluateAll((elements) =>
    elements.map((element) => {
      const style = window.getComputedStyle(element);
      const rect = element.getBoundingClientRect();
      return {
        id: element.id,
        text: (element.innerText || element.textContent || "").trim(),
        visible:
          rect.width > 0 &&
          rect.height > 0 &&
          style.display !== "none" &&
          style.visibility !== "hidden",
      };
    })
  );
}

async function resetMessages(page) {
  await page.evaluate(() => {
    window.__adpWomRecordMessages = [];
  });
}

async function readMessages(page) {
  return page.evaluate(() => window.__adpWomRecordMessages || []);
}

async function navigationStatus(page) {
  return page.evaluate(() => {
    const navigation = window.performance.getEntriesByType("navigation")[0];
    return navigation && typeof navigation.responseStatus === "number"
      ? navigation.responseStatus
      : 0;
  });
}

async function capture(page, fileName) {
  const filePath = path.join(screenshotDir, fileName);
  ensureDir(filePath);
  await page.screenshot({
    path: filePath,
    fullPage: true,
    timeout: Math.min(timeoutMs, 30000),
  });
  return filePath;
}

async function openActionPage(context, page, trigger, report, label) {
  const popupPromise = context.waitForEvent("page", { timeout: timeoutMs });
  await trigger();
  const popup = await popupPromise;
  popup.setDefaultTimeout(timeoutMs);
  popup.setDefaultNavigationTimeout(timeoutMs);
  attachPageEvidence(popup, report, label);
  await popup.waitForLoadState("domcontentloaded", { timeout: timeoutMs });
  await popup.waitForFunction(
    () => document.body && document.body.innerText.trim().length > 20,
    null,
    { timeout: timeoutMs }
  );
  await popup.waitForTimeout(300);
  return popup;
}

async function inspectPopup(popup) {
  const body = (await popup.locator("body").innerText()).slice(0, 12000);
  return {
    status: await navigationStatus(popup),
    url: popup.url(),
    title: await popup.title(),
    body,
    bodyLength: body.trim().length,
  };
}

function validPopup(result, route, bodyHints) {
  const forbidden = ["404", "Authorization Required", "数据库操作异常", "系统发生未知异常"];
  return (
    result &&
    result.status === 200 &&
    typeof result.url === "string" &&
    result.url.includes(route) &&
    result.bodyLength > 20 &&
    typeof result.body === "string" &&
    !forbidden.some((text) => result.body.includes(text)) &&
    (!bodyHints.length || bodyHints.some((text) => result.body.includes(text)))
  );
}

async function runStandardPage(context, pageCase, report) {
  const result = {
    module: pageCase.module,
    route: pageCase.route,
    status: "RUNNING",
    navigation: {},
    toolbar: [],
    noSelection: {},
    selection: {},
    detail: {},
    doubleClick: {},
    screenshots: {},
    observations: [],
    issues: [],
  };
  report.items.push(result);
  const page = await context.newPage();
  page.setDefaultTimeout(timeoutMs);
  page.setDefaultNavigationTimeout(timeoutMs);
  attachPageEvidence(page, report, `${pageCase.key}-list`);

  try {
    const navigation = await page.goto(pageCase.route, {
      waitUntil: "domcontentloaded",
      timeout: timeoutMs,
    });
    await waitForGrid(page, pageCase.gridId);
    await page.waitForTimeout(1000);
    result.navigation = {
      status: navigation && navigation.status(),
      url: page.url(),
      title: await page.title(),
    };
    result.toolbar = await readToolbar(page);

    await resetMessages(page);
    await page.locator(`#${pageCase.noSelectionButton}`).click();
    await page.waitForTimeout(250);
    result.noSelection = {
      messages: await readMessages(page),
      popupCount: context.pages().filter((candidate) => !candidate.isClosed()).length - 1,
    };

    let rows = await fetchRows(page, pageCase);
    let row;
    const preferred = pageCase.preferredId
      ? rows.find((candidate) => String(candidate.id) === String(pageCase.preferredId))
      : null;
    if (pageCase.rowEndpoint && !preferred) {
      const realRow = await fetchRealRow(page, pageCase);
      row = realRow.row;
      result.selectionSource = realRow.evidence;
      result.observations.push(
        `The list API did not return controlled row ${pageCase.preferredId}; ` +
          `the interaction used the same row fetched from its real detail API.`
      );
    } else {
      row = chooseRow(rows, pageCase);
      result.selectionSource = {
        source: "list-api",
        endpoint: pageCase.query,
        id: String(row.id),
      };
    }
    result.selection = await injectAndSelectRow(page, pageCase, row);
    result.screenshots.list = await capture(
      page,
      `wom-${pageCase.key}-record-actions-list-20260731.png`
    );

    const detailPage = await openActionPage(
      context,
      page,
      () => page.locator(`#${pageCase.detailButton}`).click(),
      report,
      `${pageCase.key}-detail`
    );
    result.detail = await inspectPopup(detailPage);
    result.screenshots.detail = await capture(
      detailPage,
      `wom-${pageCase.key}-record-actions-detail-20260731.png`
    );
    await detailPage.close();

    result.selection = await injectAndSelectRow(page, pageCase, row);
    const doubleClickPage = await openActionPage(
      context,
      page,
      () => page.locator(".sup-datagrid-row").nth(1).dblclick(),
      report,
      `${pageCase.key}-double-click`
    );
    result.doubleClick = await inspectPopup(doubleClickPage);
    await doubleClickPage.close();

    if (pageCase.key === "task") {
      result.trace = {};
      result.statistics = {};
      result.analysis = {};

      result.selection = await injectAndSelectRow(page, pageCase, row);
      const tracePage = await openActionPage(
        context,
        page,
        () => page.locator("#btn-batchReportPreview").click(),
        report,
        "task-trace"
      );
      result.trace = await inspectPopup(tracePage);
      result.screenshots.trace = await capture(
        tracePage,
        "wom-task-record-actions-trace-20260731.png"
      );
      await tracePage.close();

      result.selection = await injectAndSelectRow(page, pageCase, row);
      const statisticsButton = page.locator("#btn-manualStatistics");
      await statisticsButton.waitFor({ state: "visible", timeout: timeoutMs });
      const [statisticsResponse] = await Promise.all([
        page.waitForResponse(
          (response) =>
            response
              .url()
              .includes("/ProcessAnalysis/paramDetail/paramDetail/analysisiTask") &&
            response.url().includes(`taskExeLogId=${taskExecutionId}`),
          { timeout: timeoutMs }
        ),
        statisticsButton.click(),
      ]);
      const statisticsPayload = await readJsonSafe(statisticsResponse);
      await page.waitForTimeout(800);
      result.statistics = {
        endpoint: statisticsResponse.url().replace(baseUrl, ""),
        httpStatus: statisticsResponse.status(),
        response: statisticsPayload.json,
        messages: await readMessages(page),
      };

      runSql(
        `UPDATE public.wom_produce_task_exelog SET analysis_flag = true WHERE id = ${taskExecutionId};\n`
      );
      rows = await fetchRows(page, pageCase);
      row = chooseRow(rows, pageCase);
      result.selectionAfterStatistics = await injectAndSelectRow(page, pageCase, row);
      const analysisPage = await openActionPage(
        context,
        page,
        () => page.locator("#btn-analysisView").click(),
        report,
        "task-analysis"
      );
      result.analysis = await inspectPopup(analysisPage);
      result.screenshots.analysis = await capture(
        analysisPage,
        "wom-task-record-actions-analysis-20260731.png"
      );
      await analysisPage.close();
    }

    if (pageCase.key === "activity") {
      result.statistics = {};
      result.selection = await injectAndSelectRow(page, pageCase, row);
      const statisticsButton = page.locator("#btn-manualStatistics");
      await statisticsButton.waitFor({ state: "visible", timeout: timeoutMs });
      const [statisticsResponse] = await Promise.all([
        page.waitForResponse(
          (response) =>
            response
              .url()
              .includes("/ProcessAnalysis/paramStatRec/paramStatRec/manualStatActive") &&
            response.url().includes(`activeId=${activityExecutionId}`),
          { timeout: timeoutMs }
        ),
        statisticsButton.click(),
      ]);
      const statisticsPayload = await readJsonSafe(statisticsResponse);
      await page.waitForTimeout(800);
      result.statistics = {
        endpoint: statisticsResponse.url().replace(baseUrl, ""),
        httpStatus: statisticsResponse.status(),
        response: statisticsPayload.json,
        messages: await readMessages(page),
      };
    }
  } catch (error) {
    result.issues.push(error.stack || error.message);
  } finally {
    await page.close();
  }

  const expectedButtonsVisible = pageCase.expectedButtons.every(([id, text]) =>
    result.toolbar.some((button) => button.id === id && button.text === text && button.visible)
  );
  const noSelectionPassed = (result.noSelection.messages || []).some(
    (message) => message[1] === pageCase.noSelectionText
  );
  const failures = [];
  if (result.navigation.status !== 200) {
    failures.push(`list navigation returned ${result.navigation.status}`);
  }
  if (!expectedButtonsVisible) {
    failures.push(`expected toolbar buttons are not visible: ${JSON.stringify(result.toolbar)}`);
  }
  if (!noSelectionPassed) {
    failures.push(`no-selection warning was not shown: ${JSON.stringify(result.noSelection)}`);
  }
  if (!validPopup(result.detail, pageCase.detailRoute, pageCase.bodyHints)) {
    failures.push(`toolbar detail did not render correctly: ${JSON.stringify(result.detail)}`);
  }
  if (!validPopup(result.doubleClick, pageCase.doubleClickRoute, pageCase.bodyHints)) {
    failures.push(`row double-click did not render correctly: ${JSON.stringify(result.doubleClick)}`);
  }
  if (pageCase.key === "task") {
    if (
      !validPopup(
        result.trace,
        "/ProcessAnalysis/processAnalysis/exelogSecond/processBatchViewOut",
        ["批次", "追溯"]
      )
    ) {
      failures.push(`batch trace did not render correctly: ${JSON.stringify(result.trace)}`);
    }
    if (
      !result.statistics ||
      result.statistics.httpStatus !== 200 ||
      !result.statistics.response ||
      !result.statistics.response.data ||
      result.statistics.response.data.success !== true
    ) {
      failures.push(`task statistics failed: ${JSON.stringify(result.statistics)}`);
    }
    if (
      !validPopup(
        result.analysis,
        "/ProcessAnalysis/processAnalysis/exelogSecond/processBatchViewOut",
        ["工序执行", "活动执行"]
      )
    ) {
      failures.push(`task process analysis did not render correctly: ${JSON.stringify(result.analysis)}`);
    }
  }
  if (pageCase.key === "activity") {
    if (
      !result.statistics ||
      result.statistics.httpStatus !== 200 ||
      !result.statistics.response ||
      result.statistics.response.code !== 200
    ) {
      failures.push(`activity statistics failed: ${JSON.stringify(result.statistics)}`);
    }
  }
  result.issues.push(...failures);
  result.status = result.issues.length ? "FAIL" : "PASS";
}

function snapshotState(sourceType, sourceId) {
  const rows = parsePipeRows(
    runSql(
      `SELECT coalesce(max(revision), 0), count(*) FROM public.pa_trace_snapshots ` +
        `WHERE tenant_id = 'dt' AND source_type = '${sourceType}' AND source_id = ${sourceId};\n`
    )
  );
  return {
    maxRevision: Number((rows[0] && rows[0][0]) || 0),
    rowCount: Number((rows[0] && rows[0][1]) || 0),
  };
}

function evaluatePersistence(report) {
  for (const entry of Object.values(report.persistence.snapshots)) {
    if (!entry.before || !entry.after) {
      report.issues.push(
        `${entry.sourceType} statistics persistence verification did not complete`
      );
      continue;
    }
    if (
      entry.after.maxRevision <= entry.before.maxRevision &&
      entry.after.rowCount <= entry.before.rowCount
    ) {
      report.issues.push(
        `${entry.sourceType} statistics returned success without inserting or revising pa_trace_snapshots`
      );
    }
  }
  if (
    report.persistence.restored.taskNeedParamAna !==
      report.persistence.original.taskNeedParamAna ||
    report.persistence.restored.taskAnalysisFlag !==
      report.persistence.original.taskAnalysisFlag ||
    report.persistence.restored.activityNeedParamAna !==
      report.persistence.original.activityNeedParamAna
  ) {
    report.issues.push("Controlled WOM statistics flags were not restored.");
  }
}

async function runBrowser(ticket, report) {
  const browser = await chromium.launch({ headless });
  try {
    const context = await browser.newContext({
      baseURL: browserBaseUrl,
      viewport: { width: 2048, height: 1080 },
      ignoreHTTPSErrors: true,
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
    await context.addInitScript(messageProbe);

    for (const pageCase of pageCases) {
      await runStandardPage(context, pageCase, report);
    }
  } finally {
    await browser.close();
  }
}

async function main() {
  ensureDir(outputPath);
  const taskOriginal = parsePipeRows(
    runSql(
      `SELECT coalesce(need_param_ana::text, ''), coalesce(analysis_flag::text, '') ` +
        `FROM public.wom_produce_task_exelog WHERE id = ${taskExecutionId};\n`
    )
  )[0];
  const activityOriginal = parsePipeRows(
    runSql(
      `SELECT coalesce(need_param_ana::text, '') FROM public.wom_acti_exelogs ` +
        `WHERE id = ${activityExecutionId};\n`
    )
  )[0];
  if (!taskOriginal || !activityOriginal) {
    throw new Error("Controlled WOM task/activity fixture rows do not exist.");
  }

  const report = {
    generatedAt: new Date().toISOString(),
    repoCommit:
      execFileSync("git", ["rev-parse", "HEAD"], { encoding: "utf8" }).trim() +
      "+working-tree",
    database: "PostgreSQL",
    environment: baseUrl,
    marker,
    module: "WOM 执行记录交互",
    status: "RUNNING",
    summary: {
      testedPages: pageCases.length,
      pass: 0,
      fail: 0,
    },
    items: [],
    http: [],
    browser: {
      consoleErrors: [],
      consoleWarnings: [],
      pageErrors: [],
      requestFailures: [],
      failedResponses: [],
    },
    persistence: {
      table: "public.pa_trace_snapshots",
      original: {
        taskNeedParamAna: taskOriginal[0],
        taskAnalysisFlag: taskOriginal[1],
        activityNeedParamAna: activityOriginal[0],
      },
      snapshots: {
        task: {
          sourceType: "TASK",
          sourceId: Number(taskExecutionId),
          before: snapshotState("TASK", taskExecutionId),
        },
        activity: {
          sourceType: "ACTIVITY",
          sourceId: Number(activityExecutionId),
          before: snapshotState("ACTIVITY", activityExecutionId),
        },
      },
      verificationSql:
        `SELECT source_type,source_id,revision,source_state,updated_at FROM public.pa_trace_snapshots ` +
        `WHERE tenant_id='dt' AND ((source_type='TASK' AND source_id=${taskExecutionId}) ` +
        `OR (source_type='ACTIVITY' AND source_id=${activityExecutionId})) ` +
        `ORDER BY source_type,revision;`,
      restored: {},
    },
    issues: [],
  };

  let api;
  try {
    runSql(
      `UPDATE public.wom_produce_task_exelog SET need_param_ana = true, analysis_flag = false ` +
        `WHERE id = ${taskExecutionId};\n` +
        `UPDATE public.wom_acti_exelogs SET need_param_ana = true ` +
        `WHERE id = ${activityExecutionId};\n`
    );
    api = await request.newContext({ ignoreHTTPSErrors: true });
    const auth = await login(api);
    report.login = { status: auth.status };
    await runBrowser(auth.ticket, report);
    report.persistence.snapshots.task.after = snapshotState("TASK", taskExecutionId);
    report.persistence.snapshots.activity.after = snapshotState(
      "ACTIVITY",
      activityExecutionId
    );
    report.persistence.rows = parsePipeRows(runSql(report.persistence.verificationSql + "\n"));
  } catch (error) {
    report.issues.push(error.stack || error.message);
  } finally {
    if (api) {
      await api.dispose();
    }
    try {
      runSql(
        `UPDATE public.wom_produce_task_exelog ` +
          `SET need_param_ana = ${sqlBoolean(taskOriginal[0])}, ` +
          `analysis_flag = ${sqlBoolean(taskOriginal[1])} ` +
          `WHERE id = ${taskExecutionId};\n` +
          `UPDATE public.wom_acti_exelogs ` +
          `SET need_param_ana = ${sqlBoolean(activityOriginal[0])} ` +
          `WHERE id = ${activityExecutionId};\n`
      );
      const taskRestored = parsePipeRows(
        runSql(
          `SELECT coalesce(need_param_ana::text, ''), coalesce(analysis_flag::text, '') ` +
            `FROM public.wom_produce_task_exelog WHERE id = ${taskExecutionId};\n`
        )
      )[0];
      const activityRestored = parsePipeRows(
        runSql(
          `SELECT coalesce(need_param_ana::text, '') FROM public.wom_acti_exelogs ` +
            `WHERE id = ${activityExecutionId};\n`
        )
      )[0];
      report.persistence.restored = {
        taskNeedParamAna: taskRestored && taskRestored[0],
        taskAnalysisFlag: taskRestored && taskRestored[1],
        activityNeedParamAna: activityRestored && activityRestored[0],
      };
    } catch (error) {
      report.issues.push(`Fixture restore failed: ${error.stack || error.message}`);
    }
  }

  evaluatePersistence(report);
  if (
    report.browser.consoleErrors.length ||
    report.browser.pageErrors.length ||
    report.browser.requestFailures.length ||
    report.browser.failedResponses.length
  ) {
    report.issues.push(`Browser errors were observed: ${JSON.stringify(report.browser)}`);
  }
  report.summary.pass = report.items.filter((item) => item.status === "PASS").length;
  report.summary.fail = report.items.filter((item) => item.status === "FAIL").length;
  report.status =
    report.issues.length || report.summary.fail || report.summary.pass !== pageCases.length
      ? "FAIL"
      : "PASS";

  fs.writeFileSync(outputPath, JSON.stringify(report, null, 2) + "\n");
  process.stdout.write(`${report.status}: ${outputPath}\n`);
  if (report.status !== "PASS") {
    process.exitCode = 1;
  }
}

main().catch((error) => {
  process.stderr.write(`${error.stack || error.message}\n`);
  process.exitCode = 1;
});
