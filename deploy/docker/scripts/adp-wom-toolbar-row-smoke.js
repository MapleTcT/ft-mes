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
const seedEvidencePath = process.env.ADP_WOM_TOOLBAR_SEED_EVIDENCE || "";
const outputPath = process.env.ADP_WOM_TOOLBAR_ROW_SMOKE_OUTPUT || "/tmp/adp-wom-toolbar-row-smoke.json";
const screenshotPath =
  process.env.ADP_WOM_TOOLBAR_SCREENSHOT ||
  path.join(path.dirname(outputPath), "adp-wom-toolbar-row-smoke.png");
const reportWorkScreenshotPath =
  process.env.ADP_WOM_REPORT_WORK_SCREENSHOT ||
  path.join(path.dirname(outputPath), "adp-wom-toolbar-report-work-render.png");
const dbSshTarget = process.env.ADP_DB_SSH_TARGET || "v6@100.99.133.43";
const dbSshPassword = process.env.ADP_DB_SSH_PASSWORD || "";
const dbContainer = process.env.ADP_DB_CONTAINER || "adp-mes-newbase-postgres-1";
const dbName = process.env.ADP_DB_NAME || "adp";
const dbUser = process.env.ADP_DB_USER || "adp";
const pageTimeoutMs = Number(process.env.ADP_PAGE_TIMEOUT_MS || 180000);
const gridTimeoutMs = Number(process.env.ADP_GRID_TIMEOUT_MS || pageTimeoutMs);
const route = "/msService/WOM/produceTask/produceTask/makeTaskList";
const gridId = "WOM_1.0.0_produceTask_makeTaskList_produceTask_sdg";

function readSeedEvidence() {
  if (!seedEvidencePath) {
    return {};
  }
  return JSON.parse(fs.readFileSync(seedEvidencePath, "utf8"));
}

const seedEvidence = readSeedEvidence();
const marker = process.env.ADP_E2E_MARKER || seedEvidence.marker || "";
const taskId =
  process.env.ADP_WOM_TASK_ID ||
  (seedEvidence.ids && seedEvidence.ids.task) ||
  (seedEvidence.persistence && seedEvidence.persistence.task && seedEvidence.persistence.task[1]) ||
  "";
const tableNo =
  process.env.ADP_WOM_TABLE_NO ||
  seedEvidence.tableNo ||
  (seedEvidence.persistence && seedEvidence.persistence.task && seedEvidence.persistence.task[2]) ||
  "";
const batchNo = process.env.ADP_WOM_BATCH_NO || `${marker}_BATCH`;

if (!marker || !taskId) {
  throw new Error(
    "ADP_WOM_TOOLBAR_SEED_EVIDENCE must point to a hold/restart acceptance JSON, or ADP_E2E_MARKER and ADP_WOM_TASK_ID must be set."
  );
}

function ensureDir(filePath) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
}

async function captureScreenshot(page, filePath, owner, label) {
  ensureDir(filePath);
  try {
    await page.screenshot({
      path: filePath,
      fullPage: true,
      timeout: Math.min(pageTimeoutMs, 30000),
    });
    return { ok: true, path: filePath };
  } catch (error) {
    const item = {
      label,
      path: filePath,
      error: error.message,
    };
    owner.screenshotFailures = owner.screenshotFailures || [];
    owner.screenshotFailures.push(item);
    return { ok: false, ...item };
  }
}

function shellQuote(value) {
  return `'${String(value).replace(/'/g, "'\\''")}'`;
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

function verificationSql() {
  return `
SELECT 'task', id, table_no, task_run_state, status, version, coalesce(act_start_time::text, ''), coalesce(modify_time::text, '')
FROM public.wom_produce_tasks
WHERE id = ${taskId};

SELECT 'wait', id, task_id, exe_state, coalesce(proc_report_id::text, ''), coalesce(batch_sync_status, ''), coalesce(actual_start_time::text, '')
FROM public.wom_wait_put_records
WHERE task_id = ${taskId};
`;
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

function installMessageProbeScript() {
  window.__adpWomToolbarMessages = [];
  window.__adpWomPatchMessages = function patchMessages() {
    var api = window.ReactAPI;
    if (!api || api.__adpWomToolbarMessagePatched || typeof api.showMessage !== "function") {
      return false;
    }
    var original = api.showMessage;
    api.showMessage = function patchedShowMessage(type, message) {
      try {
        window.__adpWomToolbarMessages.push([String(type), String(message)]);
      } catch (_error) {
        // Ignore probe recording failures; the real UI call must still run.
      }
      return original.apply(this, arguments);
    };
    api.__adpWomToolbarMessagePatched = true;
    return true;
  };
  window.setInterval(function patchMessagesOnInterval() {
    try {
      window.__adpWomPatchMessages();
    } catch (_error) {
      // Best-effort probe only.
    }
  }, 250);
}

async function waitForResponseSafe(page, predicate, timeout = 30000) {
  return page
    .waitForResponse(predicate, { timeout })
    .then((response) => ({ response }))
    .catch((error) => ({ error: error.message }));
}

async function waitForNoBlockingSpin(page, timeout = 60000) {
  return page
    .waitForFunction(
      () => {
        const blockingSpins = Array.from(document.querySelectorAll(".ant-spin-container.ant-spin-blur"));
        return !blockingSpins.some((element) => {
          const rect = element.getBoundingClientRect();
          const style = window.getComputedStyle(element);
          return (
            rect.width > 0 &&
            rect.height > 0 &&
            style.visibility !== "hidden" &&
            style.display !== "none" &&
            Number(style.opacity) !== 0
          );
        });
      },
      null,
      { timeout }
    )
    .then(() => ({ ok: true }))
    .catch((error) => ({ ok: false, error: error.message }));
}

async function parseResponse(response) {
  const body = await response.text();
  let json = null;
  try {
    json = JSON.parse(body);
  } catch (_error) {
    json = null;
  }
  const url = response.url();
  return {
    method: response.request().method(),
    url,
    status: response.status(),
    postData: response.request().postData(),
    body: body.slice(0, 2000),
    json: compactJson(url, json),
  };
}

function compactJson(url, json) {
  if (!json || typeof json !== "object") {
    return json;
  }
  if (!/\/WOM\/produceTask\/produceTask\/makeTaskList-(query|pending)/.test(url)) {
    return json;
  }
  const data = json.data || {};
  const result = Array.isArray(data.result) ? data.result : [];
  return {
    code: json.code,
    message: json.message,
    data: {
      pageNo: data.pageNo,
      pageSize: data.pageSize,
      totalCount: data.totalCount,
      totalPages: data.totalPages,
      resultCount: result.length,
      resultSample: result.slice(0, 5).map((row) => ({
        id: row.id,
        tableNo: row.tableNo,
        taskRunState: row.taskRunState && row.taskRunState.id,
        status: row.status,
        version: row.version,
        produceBatchNum: row.produceBatchNum,
      })),
    },
  };
}

async function waitForGrid(page, timeout = gridTimeoutMs) {
  await page.waitForFunction(
    (gridCode) => {
      const gridFactory = window.ReactAPI && window.ReactAPI.getComponentAPI("SupDataGrid");
      const grid = gridFactory && typeof gridFactory.APIs === "function" && gridFactory.APIs(gridCode);
      return Boolean(grid && typeof grid.getRows === "function");
    },
    gridId,
    { timeout }
  );
}

async function navigateToMakeTaskList(page, report) {
  const attempts = [];
  const gridInitTimeout = Math.min(gridTimeoutMs, 90000);

  for (let attempt = 1; attempt <= 3; attempt += 1) {
    const responseOffset = report.responses.length;
    const nav = await page.goto(route, { waitUntil: "commit", timeout: pageTimeoutMs });
    const domContentLoaded = await page
      .waitForLoadState("domcontentloaded", { timeout: Math.min(pageTimeoutMs, 60000) })
      .then(() => ({ ok: true }))
      .catch((error) => ({ ok: false, error: error.message }));
    const gridReady = await waitForGrid(page, gridInitTimeout)
      .then(() => ({ ok: true }))
      .catch((error) => ({ ok: false, error: error.message }));
    const observedResponses = report.responses.slice(responseOffset).map((response) => ({
      method: response.method,
      url: response.url,
      status: response.status,
    }));
    attempts.push({
      attempt,
      status: nav && nav.status(),
      domContentLoaded,
      gridReady,
      observedResponses,
    });
    if (gridReady.ok) {
      report.navigation = {
        status: nav && nav.status(),
        domContentLoaded,
        attempts,
      };
      return;
    }
    await page.goto("about:blank", { waitUntil: "commit", timeout: 30000 }).catch(() => null);
    await page.waitForTimeout(1000);
  }

  report.navigation = {
    status: attempts.length ? attempts[attempts.length - 1].status : null,
    domContentLoaded: attempts.length ? attempts[attempts.length - 1].domContentLoaded : null,
    attempts,
  };
  throw new Error(`makeTaskList grid did not initialize after ${attempts.length} navigation attempts`);
}

async function refreshMarkerGrid(page) {
  await page.waitForFunction(
    (gridCode) => {
      const gridFactory = window.ReactAPI && window.ReactAPI.getComponentAPI("SupDataGrid");
      const grid = gridFactory && typeof gridFactory.APIs === "function" && gridFactory.APIs(gridCode);
      return Boolean(grid && typeof grid.refreshDataByRequst === "function");
    },
    gridId,
    { timeout: gridTimeoutMs }
  );
  const responsePromise = waitForResponseSafe(
    page,
    (response) =>
      /\/WOM\/produceTask\/produceTask\/makeTaskList-(pending|query)/.test(response.url()) &&
      response.request().method() === "POST",
    gridTimeoutMs
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
    const fallback = await page.evaluate(
      ({ gridCode, expectedMarker }) => {
        const gridFactory = window.ReactAPI && window.ReactAPI.getComponentAPI("SupDataGrid");
        const grid = gridFactory && typeof gridFactory.APIs === "function" && gridFactory.APIs(gridCode);
        const rows =
          (grid && typeof grid.getRows === "function" && grid.getRows()) ||
          (grid && typeof grid.getDatagridData === "function" && grid.getDatagridData()) ||
          [];
        const result = rows.find((row) => String(row.tableNo || "").includes(expectedMarker));
        return {
          ok: Boolean(result),
          rowCount: rows.length,
          markerRow: result
            ? {
                id: result.id,
                tableNo: result.tableNo,
                taskRunState: result.taskRunState && result.taskRunState.id,
                status: result.status,
                version: result.version,
                produceBatchNum: result.produceBatchNum,
              }
            : null,
        };
      },
      { gridCode: gridId, expectedMarker: marker }
    );
    if (fallback.ok) {
      return {
        method: "GRID_STATE_FALLBACK",
        url: "/msService/WOM/produceTask/produceTask/makeTaskList-pending",
        status: 200,
        postData: "response not observed; marker row already present in SupDataGrid",
        body: "",
        json: {
          code: 200,
          message: "marker row present after refresh response timeout",
          data: fallback,
        },
        responseObservationError: error,
      };
    }
    throw new Error(`WOM marker grid refresh response was not observed: ${error}`);
  }
  return parseResponse(response);
}

async function selectMarkerRow(page) {
  const refreshResponse = await refreshMarkerGrid(page);
  await page.waitForFunction(
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
    { timeout: gridTimeoutMs }
  );
  const apiSelection = await page.evaluate(
    ({ gridCode, expectedMarker }) => {
      const gridFactory = window.ReactAPI && window.ReactAPI.getComponentAPI("SupDataGrid");
      const grid = gridFactory && typeof gridFactory.APIs === "function" && gridFactory.APIs(gridCode);
      if (!grid || typeof grid.getRows !== "function") {
        return { ok: false, error: "grid api missing" };
      }
      const rows = grid.getRows ? grid.getRows() : [];
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
        selectedId: String(selected.id || ""),
        tableNo: selected.tableNo,
        taskRunState: selected.taskRunState && selected.taskRunState.id,
        status: selected.status,
        batchContral: selected.batchContral,
        advanceCharge: selected.advanceCharge,
        isAdvanced: selected.isAdvanced,
        feedCondition: selected.feedCondition,
      };
    },
    { gridCode: gridId, expectedMarker: marker }
  );

  const domSelection = { attempted: false, ok: false, error: "" };
  let rowLocator = page
    .locator(".sup-datagrid-body-wrap .sup-datagrid-row, .ant-table-row, .sup-table-row, .datagrid-row, .cui-grid-row, tr")
    .filter({ hasText: marker })
    .first();
  if ((await rowLocator.count()) < 1 && typeof apiSelection.rowIndex === "number" && apiSelection.rowIndex >= 0) {
    await page
      .waitForFunction(
        ({ rowIndex }) =>
          document.querySelectorAll(
            ".sup-datagrid-body-wrap .sup-datagrid-row, .ant-table-tbody tr, tbody tr, .ant-table-row, .sup-table-row, .datagrid-row, .cui-grid-row"
          ).length > rowIndex,
        { rowIndex: apiSelection.rowIndex },
        { timeout: 5000 }
      )
      .catch(() => null);
    rowLocator = page
      .locator(".sup-datagrid-body-wrap .sup-datagrid-row, .ant-table-tbody tr, tbody tr, .ant-table-row, .sup-table-row, .datagrid-row, .cui-grid-row")
      .nth(apiSelection.rowIndex);
  }
  if ((await rowLocator.count()) > 0) {
    domSelection.attempted = true;
    try {
      await rowLocator.click({ timeout: 5000 });
      await page.waitForTimeout(300);
      domSelection.ok = true;
    } catch (error) {
      domSelection.error = error.message;
    }
  }

  let finalSelection = await page.evaluate(
    ({ gridCode }) => {
      const gridFactory = window.ReactAPI && window.ReactAPI.getComponentAPI("SupDataGrid");
      const grid = gridFactory && typeof gridFactory.APIs === "function" && gridFactory.APIs(gridCode);
      const selecteds = grid && typeof grid.getSelecteds === "function" ? grid.getSelecteds() : [];
      const selected = selecteds[0] || {};
      return {
        selectedCount: selecteds.length,
        selectedId: String(selected.id || ""),
        tableNo: selected.tableNo,
        taskRunState: selected.taskRunState && selected.taskRunState.id,
        status: selected.status,
        batchContral: selected.batchContral,
        advanceCharge: selected.advanceCharge,
        isAdvanced: selected.isAdvanced,
        feedCondition: selected.feedCondition,
      };
    },
    { gridCode: gridId }
  );
  let restoredSelection = null;
  if (
    String(finalSelection.selectedId || "") !== String(taskId) &&
    typeof apiSelection.rowIndex === "number" &&
    apiSelection.rowIndex >= 0
  ) {
    restoredSelection = await page.evaluate(
      ({ gridCode, rowIndex }) => {
        const gridFactory = window.ReactAPI && window.ReactAPI.getComponentAPI("SupDataGrid");
        const grid = gridFactory && typeof gridFactory.APIs === "function" && gridFactory.APIs(gridCode);
        if (!grid || typeof grid.setSelecteds !== "function") {
          return { ok: false, error: "grid setSelecteds missing" };
        }
        grid.setSelecteds(String(rowIndex));
        const selecteds = typeof grid.getSelecteds === "function" ? grid.getSelecteds() : [];
        const selected = selecteds[0] || {};
        return {
          ok: selecteds.length === 1,
          selectedCount: selecteds.length,
          selectedId: String(selected.id || ""),
          tableNo: selected.tableNo,
          taskRunState: selected.taskRunState && selected.taskRunState.id,
          status: selected.status,
        };
      },
      { gridCode: gridId, rowIndex: apiSelection.rowIndex }
    );
    if (restoredSelection && restoredSelection.ok) {
      finalSelection = restoredSelection;
    }
  }

  return {
    refreshResponse: {
      status: refreshResponse.status,
      url: refreshResponse.url,
      postData: refreshResponse.postData,
    },
    ...apiSelection,
    ...finalSelection,
    ok: finalSelection.selectedCount === 1 && String(finalSelection.selectedId) === String(taskId),
    apiSelection,
    domSelection,
    restoredSelection,
  };
}

async function clickFirst(page, locators, label) {
  const errors = [];
  for (const locator of locators) {
    try {
      const count = await locator.count();
      if (count > 0) {
        await locator.first().click({ timeout: 10000 });
        return { ok: true, label, selectorIndex: errors.length };
      }
    } catch (error) {
      errors.push(error.message);
    }
  }
  return { ok: false, label, errors };
}

async function clickToolbarText(page, labelPattern, label, responsePattern) {
  const ready = await waitForNoBlockingSpin(page);
  if (!ready.ok) {
    return { clicked: false, error: `blocking loading overlay remained before ${label}: ${ready.error}` };
  }
  const responsePromise = responsePattern
    ? waitForResponseSafe(
        page,
        (response) =>
          response.request().method() === "POST" &&
          responsePattern.test(response.url()),
        30000
      )
    : null;
  const click = await clickFirst(
    page,
    [
      page.getByRole("button", { name: labelPattern }),
      page.locator("button, .ant-btn, [role='button'], .sup-btn, .btn").filter({ hasText: labelPattern }),
      page.locator("text=" + label),
    ],
    label
  );
  if (!click.ok) {
    return { clicked: false, error: click.errors.join("; ") };
  }
  if (!responsePromise) {
    await page.waitForTimeout(700);
    return { clicked: true };
  }
  const responseResult = await responsePromise;
  if (responseResult.error) {
    return { clicked: true, responseError: responseResult.error };
  }
  const parsed = await parseResponse(responseResult.response);
  return { clicked: true, response: parsed };
}

async function clickToolbarId(page, buttonId, responsePredicate, timeout = 30000) {
  const ready = await waitForNoBlockingSpin(page);
  if (!ready.ok) {
    return { clicked: false, error: `blocking loading overlay remained before ${buttonId}: ${ready.error}` };
  }
  const responsePromise = responsePredicate ? waitForResponseSafe(page, responsePredicate, timeout) : null;
  await page.locator(`#${buttonId}`).click({ timeout: 10000 });
  if (!responsePromise) {
    await page.waitForTimeout(700);
    return { clicked: true };
  }
  const responseResult = await responsePromise;
  if (responseResult.error) {
    return { clicked: true, responseError: responseResult.error };
  }
  const response = await parseResponse(responseResult.response);
  return { clicked: true, response };
}

async function clickStopButton(page) {
  const ready = await waitForNoBlockingSpin(page);
  if (!ready.ok) {
    return { clicked: false, error: `blocking loading overlay remained before stop button: ${ready.error}` };
  }
  const findProcPromise = waitForResponseSafe(
    page,
    (response) => response.url().includes("/WOM/produceTask/produceTask/findProcReportIdByTaskId"),
    30000
  );
  const reportWorkPromise = waitForResponseSafe(
    page,
    (response) => response.url().includes("/WOM/procReport/procReport/outPutCommonTaskEdit"),
    60000
  );
  await page.locator("#btn-stopTask").click({ timeout: 10000 });

  const findProcResult = await findProcPromise;
  const reportWorkResult = await reportWorkPromise;
  const operation = { clicked: true };
  if (findProcResult.error) {
    operation.responseError = findProcResult.error;
  } else {
    operation.response = await parseResponse(findProcResult.response);
  }
  if (reportWorkResult.error) {
    operation.reportWorkResponseError = reportWorkResult.error;
  } else {
    operation.reportWorkResponse = await parseResponse(reportWorkResult.response);
  }
  await page.waitForLoadState("networkidle", { timeout: 10000 }).catch(() => null);
  await page.waitForTimeout(1000);
  return operation;
}

async function verifyReportWorkRender(page, operation) {
  const reportWorkUrl = operation && operation.reportWorkResponse && operation.reportWorkResponse.url;
  const render = {
    checked: Boolean(reportWorkUrl),
    url: reportWorkUrl || "",
    navigation: null,
    domContentLoaded: null,
    visibleReady: null,
    responses: [],
    failedResponses: [],
    requestFailures: [],
    console: [],
    pageErrors: [],
    content: {},
    screenshot: reportWorkScreenshotPath,
  };
  if (!reportWorkUrl) {
    render.error = "outPutCommonTaskEdit response URL was not captured";
    return render;
  }

  const renderPage = await page.context().newPage();
  renderPage.setDefaultTimeout(pageTimeoutMs);
  renderPage.setDefaultNavigationTimeout(pageTimeoutMs);
  try {
    renderPage.on("console", (message) => {
      if (["error", "warning"].includes(message.type())) {
        render.console.push({ type: message.type(), text: message.text().slice(0, 1000) });
      }
    });
    renderPage.on("pageerror", (error) => render.pageErrors.push(error.message));
    renderPage.on("requestfailed", (requestItem) => {
      render.requestFailures.push({
        method: requestItem.method(),
        url: requestItem.url(),
        failure: requestItem.failure() && requestItem.failure().errorText,
      });
    });
    renderPage.on("response", (response) => {
      const url = response.url();
      if (!/outPutCommonTaskEdit|layoutJson|editStates|\/procReport\/procReport\/data\/|upload-list|body-es5|i18n-value/.test(url)) {
        return;
      }
      const item = {
        method: response.request().method(),
        url,
        status: response.status(),
      };
      render.responses.push(item);
      if (response.status() >= 400) {
        render.failedResponses.push(item);
      }
    });

    const nav = await renderPage.goto(reportWorkUrl, { waitUntil: "commit", timeout: pageTimeoutMs });
    render.navigation = { status: nav && nav.status() };
    render.domContentLoaded = await renderPage
      .waitForLoadState("domcontentloaded", { timeout: Math.min(pageTimeoutMs, 60000) })
      .then(() => ({ ok: true }))
      .catch((error) => ({ ok: false, error: error.message }));
    render.visibleReady = await renderPage
      .waitForFunction(
        (expectedBatchNo) => {
          const text = document.body ? document.body.innerText || "" : "";
          return (
            /生产批号/.test(text) &&
            /产品编码/.test(text) &&
            /产品名称/.test(text) &&
            (!expectedBatchNo || text.indexOf(expectedBatchNo) !== -1)
          );
        },
        batchNo,
        { timeout: 60000 }
      )
      .then(() => ({ ok: true }))
      .catch((error) => ({ ok: false, error: error.message }));

    render.content = await renderPage.evaluate(
      ({ expectedMarker, expectedBatchNo }) => {
        const bodyText = document.body ? document.body.innerText || "" : "";
        return {
          hasBatchLabel: /生产批号/.test(bodyText),
          hasProductCodeLabel: /产品编码/.test(bodyText),
          hasProductNameLabel: /产品名称/.test(bodyText),
          hasPlanNumLabel: /计划数量/.test(bodyText),
          hasExpectedMarker: expectedMarker ? bodyText.indexOf(expectedMarker) !== -1 : false,
          hasExpectedBatchNo: expectedBatchNo ? bodyText.indexOf(expectedBatchNo) !== -1 : false,
          inputCount: document.querySelectorAll("input,textarea,.ant-input").length,
          snippet: bodyText.replace(/\s+/g, " ").slice(0, 1200),
        };
      },
      { expectedMarker: marker, expectedBatchNo: batchNo }
    );
    render.screenshotCapture = await captureScreenshot(renderPage, reportWorkScreenshotPath, render, "reportWorkRender");
  } finally {
    await renderPage.close();
  }
  return render;
}

async function clickTopFilterDropdown(page) {
  const candidate = await page.evaluate(() => {
    function visibleRect(element) {
      const rect = element.getBoundingClientRect();
      const style = window.getComputedStyle(element);
      if (
        rect.width < 80 ||
        rect.height < 20 ||
        rect.top < 0 ||
        rect.top > 90 ||
        rect.left < 0 ||
        rect.left > 320 ||
        style.visibility === "hidden" ||
        style.display === "none" ||
        Number(style.opacity) === 0
      ) {
        return null;
      }
      return {
        x: rect.left,
        y: rect.top,
        width: rect.width,
        height: rect.height,
        centerX: rect.left + rect.width / 2,
        centerY: rect.top + rect.height / 2,
      };
    }

    const selectors = [
      "[role='combobox']",
      ".ant-select",
      ".sup-select",
      ".Select",
      "[class*='select']",
      "input[readonly]",
      "input"
    ];
    const nodes = [];
    selectors.forEach((selector) => {
      document.querySelectorAll(selector).forEach((element) => {
        if (!nodes.includes(element)) {
          nodes.push(element);
        }
      });
    });
    const matches = nodes
      .map((element) => {
        const rect = visibleRect(element);
        if (!rect) {
          return null;
        }
        return {
          tag: element.tagName,
          id: element.id || "",
          className: String(element.className || "").slice(0, 160),
          text: String(element.innerText || element.value || element.getAttribute("placeholder") || "").slice(0, 120),
          displayText: String(element.innerText || element.value || element.getAttribute("placeholder") || "").replace(/\s+/g, " ").trim().slice(0, 120),
          emptySelectFallbackApplied: Boolean(
            (element.getAttribute && element.getAttribute("data-adp-wom-empty-search-select") === "true") ||
              (element.closest &&
                element.closest("#search_panel_selectField") &&
                element.closest("#search_panel_selectField").getAttribute("data-adp-wom-empty-search-select") === "true")
          ),
          rect,
        };
      })
      .filter(Boolean)
      .sort((left, right) => left.rect.top - right.rect.top || left.rect.left - right.rect.left);
    return matches[0] || null;
  });

  if (!candidate) {
    return { clicked: false, dropdownVisible: false, error: "top filter dropdown candidate not found" };
  }

  await page.mouse.click(candidate.rect.centerX, candidate.rect.centerY);
  await page.waitForTimeout(600);
  const selectStateAfterClick = await page.evaluate(() => {
    const element = document.getElementById("search_panel_selectField");
    const text = element ? String(element.innerText || "").replace(/\s+/g, " ").trim().slice(0, 120) : "";
    return {
      displayText: text,
      emptySelectFallbackApplied: Boolean(
        element && element.getAttribute("data-adp-wom-empty-search-select") === "true"
      ),
    };
  });
  const dropdown = await page.evaluate(() => {
    function isVisible(element) {
      const rect = element.getBoundingClientRect();
      const style = window.getComputedStyle(element);
      return (
        rect.width > 40 &&
        rect.height > 20 &&
        rect.bottom > 0 &&
        rect.right > 0 &&
        style.visibility !== "hidden" &&
        style.display !== "none" &&
        Number(style.opacity) !== 0
      );
    }
    const selectors = [
      ".ant-select-dropdown:not(.ant-select-dropdown-hidden)",
      ".select2-drop",
      ".Select-menu-outer",
      ".sup-select-dropdown",
      "[class*='dropdown']",
      "[class*='menu']"
    ];
    const samples = [];
    selectors.forEach((selector) => {
      document.querySelectorAll(selector).forEach((element) => {
        if (!isVisible(element)) {
          return;
        }
        const rect = element.getBoundingClientRect();
        samples.push({
          selector,
          className: String(element.className || "").slice(0, 160),
          text: String(element.innerText || "").replace(/\s+/g, " ").trim().slice(0, 160),
          rect: {
            x: Math.round(rect.left),
            y: Math.round(rect.top),
            width: Math.round(rect.width),
            height: Math.round(rect.height),
          },
        });
      });
    });
    return { visible: samples.length > 0, samples: samples.slice(0, 5) };
  });
  await page.keyboard.press("Escape").catch(() => null);
  const optionTexts = dropdown.samples
    .map((sample) => sample.text)
    .filter((text) => text && text !== "暂无数据");
  return {
    clicked: true,
    dropdownVisible: dropdown.visible,
    hasOptions: optionTexts.length > 0,
    emptySelectFallbackApplied: candidate.emptySelectFallbackApplied || selectStateAfterClick.emptySelectFallbackApplied,
    displayText: selectStateAfterClick.displayText || candidate.displayText,
    candidate,
    selectStateAfterClick,
    dropdowns: dropdown.samples,
  };
}

async function latestMessages(page) {
  return page.evaluate(() => (window.__adpWomToolbarMessages || []).slice());
}

async function latestMessageCount(page) {
  return page.evaluate(() => (window.__adpWomToolbarMessages || []).length);
}

async function clearGridSelection(page) {
  return page.evaluate((gridCode) => {
    const gridFactory = window.ReactAPI && window.ReactAPI.getComponentAPI("SupDataGrid");
    const grid = gridFactory && typeof gridFactory.APIs === "function" && gridFactory.APIs(gridCode);
    if (!grid || typeof grid.setSelecteds !== "function") {
      return { ok: false, error: "grid setSelecteds missing" };
    }
    grid.setSelecteds("");
    const selecteds = typeof grid.getSelecteds === "function" ? grid.getSelecteds() : [];
    return { ok: selecteds.length === 0, selectedCount: selecteds.length };
  }, gridId);
}

async function clickToolbarWithoutSelection(page, buttonIds) {
  const results = [];
  for (const buttonId of buttonIds) {
    const clearSelection = await clearGridSelection(page);
    const messageOffset = await latestMessageCount(page);
    const operation = await clickToolbarId(page, buttonId, null);
    const messages = await page.evaluate(
      (offset) => (window.__adpWomToolbarMessages || []).slice(offset),
      messageOffset
    );
    results.push({ buttonId, clearSelection, operation, messages });
  }
  return results;
}

function messageTexts(messages) {
  return messages.map((entry) => String(entry[1] || ""));
}

function containsMessage(messages, pattern) {
  return messageTexts(messages).some((message) => pattern.test(message));
}

function hasRawI18nMessage(messages) {
  return messageTexts(messages).some((message) => /WOM\.custom\.|ec\.common\./.test(message));
}

function updateStateFromResponse(operation) {
  const json = operation && operation.response && operation.response.json;
  return json && json.data && json.data.exeState && json.data.exeState.id;
}

function dealSuccess(operation) {
  const json = operation && operation.response && operation.response.json;
  return Boolean(json && json.data && json.data.dealSuccessFlag === true);
}

async function ensureTaskRunningForHold(page) {
  const before = await selectMarkerRow(page);
  const precondition = {
    checked: true,
    expectedState: "WOM_runState/runing",
    before,
    needed: before.taskRunState === "WOM_runState/iskeep",
    operation: null,
    after: before,
    messages: [],
  };

  if (!precondition.needed) {
    return precondition;
  }

  precondition.operation = await clickToolbarId(
    page,
    "btn-recoveryTask",
    (response) => {
      const postData = response.request().postData() || "";
      return response.url().includes("/WOM/produceTask/produceTask/updateTaskState") && postData.includes(`state=restart&taskId=${taskId}`);
    },
    30000
  );
  precondition.messages = await latestMessages(page);
  await page.waitForTimeout(1200);
  precondition.after = await selectMarkerRow(page);
  return precondition;
}

function summarizePersistence(raw) {
  const rows = parseRows(raw);
  const task = rows.find((row) => row[0] === "task");
  const wait = rows.find((row) => row[0] === "wait");
  return { raw, rows, task, wait };
}

function buildStatus(report) {
  const failures = [];
  const operations = report.operations;

  if (report.navigation.status !== 200) {
    failures.push(`navigation status is ${report.navigation.status}`);
  }
  if (report.i18n.hasRawWomCustom || report.i18n.hasTableNoKey) {
    failures.push("raw i18n keys are still visible");
  }
  if (report.i18n.getTextStart !== "只有【待执行】的指令单可以开始！") {
    failures.push(`start warning i18n is unexpected: ${report.i18n.getTextStart}`);
  }
  if (report.i18n.getTextTableNo !== "单据编号") {
    failures.push(`tableNo i18n is unexpected: ${report.i18n.getTextTableNo}`);
  }
  if (report.i18n.bodyTranslateInstalled !== true) {
    failures.push("body.js message translation fallback was not installed");
  }
  if (!operations.query.response || operations.query.response.status !== 200) {
    failures.push("query click did not capture HTTP 200 list response");
  }
  if (!operations.onlyPending.response || operations.onlyPending.response.status !== 200) {
    failures.push("only-pending click did not capture HTTP 200 list response");
  }
  if (!operations.clear.clicked) {
    failures.push("clear button was not clicked");
  }
  const noSelection = operations.noSelectionRowActions || [];
  const expectedNoSelectionButtons = [
    "btn-startTask",
    "btn-pauseTask",
    "btn-recoveryTask",
    "btn-stopTask",
    "btn-earlyPutIn",
    "btn-manuInspect",
    "btn-prodprocessView",
    "btn-generateCode",
  ];
  const noSelectionByButton = Object.fromEntries(noSelection.map((item) => [item.buttonId, item]));
  expectedNoSelectionButtons.forEach((buttonId) => {
    const item = noSelectionByButton[buttonId];
    if (!item || !item.operation || item.operation.clicked !== true) {
      failures.push(`no-selection ${buttonId} was not clicked`);
      return;
    }
    if (!item.clearSelection || item.clearSelection.ok !== true) {
      failures.push(`no-selection ${buttonId} did not start from an empty grid selection: ${JSON.stringify(item && item.clearSelection)}`);
    }
    if (!containsMessage(item.messages || [], /请先选择一条指令单/)) {
      failures.push(`no-selection ${buttonId} did not show the unified selection warning`);
    }
    if (hasRawI18nMessage(item.messages || [])) {
      failures.push(`no-selection ${buttonId} still showed a raw i18n key`);
    }
  });
  if (!operations.filterDropdown || !operations.filterDropdown.clicked) {
    failures.push(`top filter dropdown did not open: ${JSON.stringify(operations.filterDropdown)}`);
  } else if (operations.filterDropdown.emptySelectFallbackApplied) {
    if (operations.filterDropdown.dropdownVisible) {
      failures.push(`empty top filter fallback still opened a dropdown: ${JSON.stringify(operations.filterDropdown)}`);
    }
    if (operations.filterDropdown.displayText !== "全部") {
      failures.push(`empty top filter fallback display is unexpected: ${operations.filterDropdown.displayText}`);
    }
  } else if (!operations.filterDropdown.dropdownVisible || !operations.filterDropdown.hasOptions) {
    failures.push(`top filter dropdown has no real options and no fallback: ${JSON.stringify(operations.filterDropdown)}`);
  }
  if (!containsMessage(report.messagesAfter.startNegative, /只有【待执行】的指令单可以开始/)) {
    failures.push("start negative click did not show translated business warning");
  }
  if (hasRawI18nMessage(report.messagesAfter.startNegative)) {
    failures.push("start negative click still showed a raw i18n key");
  }
  const beforeHoldRunning = report.preconditions && report.preconditions.beforeHoldRunning;
  if (beforeHoldRunning && beforeHoldRunning.checked) {
    const beforeState = beforeHoldRunning.before && beforeHoldRunning.before.taskRunState;
    const afterState = beforeHoldRunning.after && beforeHoldRunning.after.taskRunState;
    if (beforeState !== "WOM_runState/runing" && beforeState !== "WOM_runState/iskeep") {
      failures.push(`hold precondition started from unsupported state: ${beforeState}`);
    }
    if (beforeHoldRunning.needed) {
      if (
        !beforeHoldRunning.operation ||
        !beforeHoldRunning.operation.response ||
        beforeHoldRunning.operation.response.status !== 200 ||
        !dealSuccess(beforeHoldRunning.operation)
      ) {
        failures.push("hold precondition restart did not return HTTP 200/dealSuccessFlag=true");
      }
      if (afterState !== "WOM_runState/runing") {
        failures.push(`hold precondition did not restore running state: ${afterState}`);
      }
    }
  }
  if (!operations.hold.response || operations.hold.response.status !== 200 || !dealSuccess(operations.hold)) {
    failures.push("hold updateTaskState did not return HTTP 200/dealSuccessFlag=true");
  }
  if (updateStateFromResponse(operations.hold) !== "WOM_runState/iskeep") {
    failures.push(`hold exeState is unexpected: ${updateStateFromResponse(operations.hold)}`);
  }
  if (!operations.restart.response || operations.restart.response.status !== 200 || !dealSuccess(operations.restart)) {
    failures.push("restart updateTaskState did not return HTTP 200/dealSuccessFlag=true");
  }
  if (updateStateFromResponse(operations.restart) !== "WOM_runState/runing") {
    failures.push(`restart exeState is unexpected: ${updateStateFromResponse(operations.restart)}`);
  }
  if (!containsMessage(report.messagesAfter.earlyPutIn, /该批次不能提前放料|只有【执行中】的指令单允许提前放料|是否提前放料/)) {
    failures.push("early release click did not show a translated business warning/confirmation");
  }
  if (hasRawI18nMessage(report.messagesAfter.earlyPutIn)) {
    failures.push("early release click still showed a raw i18n key");
  }
  if (!containsMessage(report.messagesAfter.manuInspect, /该指令单产品无需质检|只有【执行中】的指令单允许请检|是否发起检验申请/)) {
    failures.push("manual inspection click did not show a translated business warning/confirmation");
  }
  if (hasRawI18nMessage(report.messagesAfter.manuInspect)) {
    failures.push("manual inspection click still showed a raw i18n key");
  }
  if (!containsMessage(report.messagesAfter.trace, /生产过程追溯服务未部署或暂不可用/)) {
    failures.push("ProcessAnalysis guard message was not shown");
  }
  if (!report.knownBlockers.processAnalysis.guardedWithoutRequest) {
    failures.push("ProcessAnalysis toolbar dependency was not guarded before the missing service request");
  }
  if (!containsMessage(report.messagesAfter.qrcode, /二维码生成页面未部署或暂不可用/)) {
    failures.push("QR-code guard message was not shown");
  }
  if (!report.knownBlockers.qrcode.guardedWithoutRequest) {
    failures.push("WOM printManage QR toolbar dependency was not guarded before the missing endpoint request");
  }
  if (!operations.stop.response || operations.stop.response.status !== 200) {
    failures.push("stop button did not open the report-work entry through findProcReportIdByTaskId HTTP 200");
  }
  if (!operations.stop.reportWorkResponse || operations.stop.reportWorkResponse.status !== 200) {
    failures.push("stop button did not load outPutCommonTaskEdit HTTP 200");
  }
  const reportWorkRender = report.reportWorkRender || {};
  const reportWorkContent = reportWorkRender.content || {};
  if (!reportWorkRender.checked) {
    failures.push("stop button did not capture a report-work render URL");
  }
  if (!reportWorkRender.navigation || reportWorkRender.navigation.status !== 200) {
    failures.push(`report-work render navigation status is ${JSON.stringify(reportWorkRender.navigation)}`);
  }
  if (!reportWorkRender.visibleReady || reportWorkRender.visibleReady.ok !== true) {
    failures.push(`report-work render did not become visible: ${JSON.stringify(reportWorkRender.visibleReady)}`);
  }
  if (
    reportWorkContent.hasBatchLabel !== true ||
    reportWorkContent.hasProductCodeLabel !== true ||
    reportWorkContent.hasProductNameLabel !== true ||
    reportWorkContent.hasExpectedBatchNo !== true
  ) {
    failures.push(`report-work render content is incomplete: ${JSON.stringify(reportWorkContent)}`);
  }
  if ((reportWorkRender.failedResponses || []).length || (reportWorkRender.requestFailures || []).length || (reportWorkRender.pageErrors || []).length) {
    failures.push(
      `report-work render had errors: failedResponses=${JSON.stringify(reportWorkRender.failedResponses || [])} requestFailures=${JSON.stringify(
        reportWorkRender.requestFailures || []
      )} pageErrors=${JSON.stringify(reportWorkRender.pageErrors || [])}`
    );
  }
  if (hasRawI18nMessage(report.messagesAfter.stop)) {
    failures.push("stop click still showed a raw i18n key");
  }

  const task = report.persistence.task;
  const wait = report.persistence.wait;
  if (!task || task[3] !== "WOM_runState/runing") {
    failures.push(`wom_produce_tasks final state is not running: ${JSON.stringify(task)}`);
  }
  if (!wait || wait[3] !== "WOM_runState/runing") {
    failures.push(`wom_wait_put_records final state is not running: ${JSON.stringify(wait)}`);
  }
  if (report.pageErrors.length) {
    failures.push(`page errors observed: ${report.pageErrors.join("; ")}`);
  }

  return {
    status: failures.length ? "FAIL" : "PASS_WITH_KNOWN_BLOCKERS",
    failures,
  };
}

async function runBrowser(ticket, report) {
  const browser = await chromium.launch({ headless });
  try {
    const context = await browser.newContext({
      baseURL: browserBaseUrl,
      ignoreHTTPSErrors: true,
      viewport: { width: 2048, height: 900 },
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
    await context.addInitScript(installMessageProbeScript);

    const page = await context.newPage();
    page.setDefaultTimeout(pageTimeoutMs);
    page.setDefaultNavigationTimeout(pageTimeoutMs);
    page.on("console", (message) => {
      if (["error", "warning"].includes(message.type())) {
        report.console.push({ type: message.type(), text: message.text().slice(0, 1000) });
      }
    });
    page.on("pageerror", (error) => report.pageErrors.push(error.message));
    page.on("requestfailed", (requestItem) => {
      report.requestFailures.push({
        method: requestItem.method(),
        url: requestItem.url(),
        failure: requestItem.failure() && requestItem.failure().errorText,
      });
    });
    page.on("request", (requestItem) => {
      const url = requestItem.url();
      if (/makeTaskList|updateTaskState|setAdvanceTrue|createManuInspect|findProcReportIdByTaskId|outPutCommonTaskEdit|ProcessAnalysis|printManage|layoutJson|i18n-value|body-es5/.test(url)) {
        report.requests.push({
          method: requestItem.method(),
          url,
          postData: requestItem.postData(),
        });
      }
    });
    page.on("response", async (response) => {
      const url = response.url();
      if (!/makeTaskList|updateTaskState|setAdvanceTrue|createManuInspect|findProcReportIdByTaskId|outPutCommonTaskEdit|ProcessAnalysis|printManage|layoutJson|i18n-value|body-es5/.test(url)) {
        return;
      }
      if (response.status() >= 400) {
        report.failedResponses.push({
          method: response.request().method(),
          url,
          status: response.status(),
        });
      }
      report.responses.push({
        method: response.request().method(),
        url,
        status: response.status(),
      });
    });

    await navigateToMakeTaskList(page, report);
    await page.evaluate(() => window.__adpWomPatchMessages && window.__adpWomPatchMessages());

    report.i18n = await page.evaluate(() => {
      var getText = window.ReactAPI && window.ReactAPI.international && window.ReactAPI.international.getText;
      var rawStart = "WOM.custom.randon1575958246066";
      var rawTableNo = "ec.common.tableNo";
      var bodyText = document.body ? document.body.innerText || "" : "";
      return {
        rawStart,
        rawTableNo,
        getTextStart: typeof getText === "function" ? getText(rawStart) : null,
        getTextTableNo: typeof getText === "function" ? getText(rawTableNo) : null,
        guardInstalled: Boolean(window.__ADP_WOM_MAKETASKLIST_GUARD__ || window.__adpWomMakeTaskListToolbarGuards),
        stateSyncPatched: Boolean(
          window.__ADP_WOM_MAKETASKLIST_STATE_SYNC__ ||
            ((window.jQuery || window.$) &&
              (window.jQuery || window.$).ajax &&
              (window.jQuery || window.$).ajax.__adpWomMakeTaskStateSyncPatched)
        ),
        translateInstalled: Boolean(
          window.__ADP_WOM_MAKETASKLIST_TRANSLATE__ ||
            window.__ADP_WOM_MAKETASKLIST_BODY_TRANSLATE__ ||
            window.adpWomMakeTaskListTranslate ||
            (window.ReactAPI &&
              window.ReactAPI.international &&
              window.ReactAPI.international.__adpWomMakeTaskListI18nPatched)
        ),
        bodyTranslateInstalled: Boolean(window.__ADP_WOM_MAKETASKLIST_BODY_TRANSLATE__),
        hasRawWomCustom: bodyText.indexOf("WOM.custom.") !== -1,
        hasTableNoKey: bodyText.indexOf("ec.common.tableNo") !== -1,
      };
    });

    report.operations.filterDropdown = await clickTopFilterDropdown(page);
    report.operations.query = await clickToolbarText(page, /查\s*询/, "查询", /\/WOM\/produceTask\/produceTask\/makeTaskList-query/);
    report.operations.onlyPending = await clickToolbarText(
      page,
      /仅\s*查\s*待\s*办/,
      "仅查待办",
      /\/WOM\/produceTask\/produceTask\/makeTaskList-(query|pending)/
    );
    report.operations.clear = await clickToolbarText(page, /清\s*空/, "清空", null);
    report.operations.noSelectionRowActions = await clickToolbarWithoutSelection(page, [
      "btn-startTask",
      "btn-pauseTask",
      "btn-recoveryTask",
      "btn-stopTask",
      "btn-earlyPutIn",
      "btn-manuInspect",
      "btn-prodprocessView",
      "btn-generateCode",
    ]);

    report.selections.startNegative = await selectMarkerRow(page);
    report.operations.startNegative = await clickToolbarId(page, "btn-startTask", null);
    report.messagesAfter.startNegative = await latestMessages(page);

    report.preconditions.beforeHoldRunning = await ensureTaskRunningForHold(page);
    report.selections.hold = await selectMarkerRow(page);
    report.operations.hold = await clickToolbarId(
      page,
      "btn-pauseTask",
      (response) => {
        const postData = response.request().postData() || "";
        return response.url().includes("/WOM/produceTask/produceTask/updateTaskState") && postData.includes(`state=hold&taskId=${taskId}`);
      },
      30000
    );
    report.messagesAfter.hold = await latestMessages(page);
    await page.waitForTimeout(1200);

    report.selections.restart = await selectMarkerRow(page);
    report.operations.restart = await clickToolbarId(
      page,
      "btn-recoveryTask",
      (response) => {
        const postData = response.request().postData() || "";
        return response.url().includes("/WOM/produceTask/produceTask/updateTaskState") && postData.includes(`state=restart&taskId=${taskId}`);
      },
      30000
    );
    report.messagesAfter.restart = await latestMessages(page);
    await page.waitForTimeout(1200);

    report.selections.earlyPutIn = await selectMarkerRow(page);
    report.operations.earlyPutIn = await clickToolbarId(page, "btn-earlyPutIn", null);
    report.messagesAfter.earlyPutIn = await latestMessages(page);

    report.selections.manuInspect = await selectMarkerRow(page);
    report.operations.manuInspect = await clickToolbarId(page, "btn-manuInspect", null);
    report.messagesAfter.manuInspect = await latestMessages(page);

    report.selections.trace = await selectMarkerRow(page);
    report.operations.trace = await clickToolbarId(page, "btn-prodprocessView", null);
    report.messagesAfter.trace = await latestMessages(page);

    report.selections.qrcode = await selectMarkerRow(page);
    report.operations.qrcode = await clickToolbarId(page, "btn-generateCode", null);
    report.messagesAfter.qrcode = await latestMessages(page);

    report.selections.stop = await selectMarkerRow(page);
    report.operations.stop = await clickStopButton(page);
    report.reportWorkRender = await verifyReportWorkRender(page, report.operations.stop);
    report.messagesAfter.stop = await latestMessages(page);
    report.stopDialog = await page.evaluate(() => {
      const bodyText = document.body ? document.body.innerText || "" : "";
      return {
        hasReportWorkText: /指令单完工报工|完工报工/.test(bodyText),
        snippet: bodyText.replace(/\s+/g, " ").slice(0, 500),
      };
    });

    report.bodyAfter = await page.locator("body").innerText();
    report.screenshotCapture = await captureScreenshot(page, screenshotPath, report, "toolbarRow");
    report.screenshot = screenshotPath;
    report.knownBlockers.processAnalysis = {
      expectedGuard: "front-end dependency guard before missing ProcessAnalysis request",
      observed: containsMessage(report.messagesAfter.trace, /生产过程追溯服务未部署或暂不可用/),
      guardedWithoutRequest: !report.operations.trace.response && !report.operations.trace.responseError,
      response: report.operations.trace.response
        ? {
            status: report.operations.trace.response.status,
            url: report.operations.trace.response.url,
          }
        : null,
      messageObserved: containsMessage(report.messagesAfter.trace, /生产过程追溯服务未部署或暂不可用/),
      message: "生产过程追溯服务未部署或暂不可用！",
      dependencyStatus: "BLOCKED",
      businessBlocker:
        "ProcessAnalysis service/runtime metadata/menu/schema is still missing; the toolbar guard only prevents a broken click path.",
    };
    report.knownBlockers.qrcode = {
      expectedGuard: "front-end dependency guard before missing WOM printManage QR request",
      observed: containsMessage(report.messagesAfter.qrcode, /二维码生成页面未部署或暂不可用/),
      guardedWithoutRequest: !report.operations.qrcode.response && !report.operations.qrcode.responseError,
      response: report.operations.qrcode.response
        ? {
            status: report.operations.qrcode.response.status,
            url: report.operations.qrcode.response.url,
          }
        : null,
      messageObserved: containsMessage(report.messagesAfter.qrcode, /二维码生成页面未部署或暂不可用/),
      message: "二维码生成页面未部署或暂不可用！",
      dependencyStatus: "BLOCKED",
      businessBlocker:
        "WOM printManage QR endpoint/package is still missing; route probe remains the backend blocker evidence.",
    };
  } finally {
    await browser.close();
  }
}

async function main() {
  ensureDir(outputPath);
  const report = {
    schemaVersion: 1,
    reportKind: "wom-toolbar-row-smoke",
    generatedAt: new Date().toISOString(),
    database: "PostgreSQL",
    baseUrl,
    browserBaseUrl,
    route,
    marker,
    taskId: String(taskId),
    tableNo,
    batchNo,
    seedEvidencePath,
    pageTimeoutMs,
    gridTimeoutMs,
    login: null,
    navigation: null,
    i18n: {},
    operations: {},
    messagesAfter: {},
    selections: {},
    preconditions: {},
    knownBlockers: {},
    persistence: {},
    verificationSql: verificationSql().trim(),
    requests: [],
    responses: [],
    failedResponses: [],
    requestFailures: [],
    console: [],
    pageErrors: [],
    status: "RUNNING",
    failures: [],
  };

  const api = await request.newContext({ ignoreHTTPSErrors: true });
  try {
    report.login = await login(api);
    await runBrowser(report.login.ticket, report);
    report.persistence = summarizePersistence(runSql(verificationSql()));
    const status = buildStatus(report);
    report.status = status.status;
    report.failures = status.failures;
  } catch (error) {
    report.status = "FAIL";
    report.failures.push(error && error.stack ? error.stack : String(error));
  } finally {
    await api.dispose();
    fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`);
  }

  if (report.status === "FAIL") {
    console.error(`WOM toolbar row smoke failed. Report: ${outputPath}`);
    console.error(report.failures.join("\n"));
    process.exit(1);
  }

  console.log(`WOM toolbar row smoke ${report.status}: ${outputPath}`);
}

main();
