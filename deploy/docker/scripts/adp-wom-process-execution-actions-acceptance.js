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
const processExecutionId = String(process.env.ADP_WOM_PROCESS_EXECUTION_ID || "9007190231282109");
const timeoutMs = Number(process.env.ADP_PAGE_TIMEOUT_MS || 120000);
const headless = process.env.ADP_HEADLESS !== "false";
const outputPath =
  process.env.ADP_WOM_PROCESS_ACTIONS_OUTPUT ||
  "metadata/wom-process-execution-actions-acceptance-20260731.json";
const screenshotDir = process.env.ADP_WOM_PROCESS_ACTIONS_SCREENSHOT_DIR || "metadata";
const route = "/msService/WOM/produceTask/processExelog/processExeLogList";
const gridId = "WOM_1.0.0_produceTask_processExeLogList_processExelog_sdg";
const marker = `ADP_E2E_${new Date().toISOString().replace(/\D/g, "").slice(0, 14)}_WOM_PROCESS_ACTIONS`;

if (!/^\d+$/.test(processExecutionId)) {
  throw new Error("ADP_WOM_PROCESS_EXECUTION_ID must be a numeric WOM process execution ID.");
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

function originalFlagLiteral(value) {
  if (value === "t" || value === "true") {
    return "true";
  }
  if (value === "f" || value === "false") {
    return "false";
  }
  if (value === "") {
    return "null";
  }
  throw new Error(`Unexpected need_param_ana value: ${value}`);
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
  const failures = [];
  for (const data of attempts) {
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
  window.__adpProcessActionMessages = [];
  window.setInterval(function patchMessageApi() {
    var api = window.ReactAPI;
    if (!api || api.__processActionMessageProbe || typeof api.showMessage !== "function") {
      return;
    }
    var original = api.showMessage;
    api.showMessage = function showMessageWithProbe(type, message) {
      window.__adpProcessActionMessages.push([String(type), String(message)]);
      return original.apply(this, arguments);
    };
    api.__processActionMessageProbe = true;
  }, 100);
}

function attachPageEvidence(page, report, label) {
  page.on("console", (message) => {
    if (["error", "warning"].includes(message.type())) {
      report.browser.console.push({ page: label, type: message.type(), text: message.text().slice(0, 1000) });
    }
  });
  page.on("pageerror", (error) => report.browser.pageErrors.push({ page: label, error: error.message }));
  page.on("requestfailed", (requestItem) => {
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
      /processExeLogList|processExecution|matConsuEntryProView|manualStatProcess|layoutJson/.test(url)
    ) {
      report.http.push({
        page: label,
        method: response.request().method(),
        url,
        status: response.status(),
      });
    }
    if (response.status() >= 400) {
      report.browser.failedResponses.push({
        page: label,
        method: response.request().method(),
        url,
        status: response.status(),
      });
    }
  });
}

async function waitForGrid(page) {
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

async function waitForLoadingToFinish(page) {
  await page.waitForFunction(
    () =>
      !Array.from(document.querySelectorAll(".ant-spin-spinning, .ant-spin-blur")).some(
        (element) => {
          const style = window.getComputedStyle(element);
          const rect = element.getBoundingClientRect();
          return (
            rect.width > 0 &&
            rect.height > 0 &&
            style.display !== "none" &&
            style.visibility !== "hidden" &&
            Number(style.opacity) !== 0
          );
        }
      ),
    null,
    { timeout: timeoutMs }
  );
}

async function listTargetRow(page) {
  const response = await page.request.post(
    `${baseUrl}/msService/WOM/produceTask/processExelog/processExeLogList-query`,
    {
      data: {
        classifyCodes: "",
        customCondition: {},
        permissionCode: "WOM_1.0.0_produceTask_processExeLogList",
        pageNo: 1,
        paging: true,
        pageSize: 65535,
        crossCompanyFlag: "true",
      },
    }
  );
  const parsed = await readJsonSafe(response);
  const rows =
    (parsed.json && parsed.json.data && parsed.json.data.result) ||
    (parsed.json && parsed.json.data && parsed.json.data.rows) ||
    [];
  const row = rows.find((candidate) => String(candidate.id) === processExecutionId);
  if (!response.ok() || !row) {
    throw new Error(
      `Controlled process execution ${processExecutionId} was not returned: HTTP ${response.status()}`
    );
  }
  return row;
}

async function selectTargetRow(page, row) {
  return page.evaluate(
    ({ code, target }) => {
      const grid = ReactAPI.getComponentAPI("SupDataGrid").APIs(code);
      grid.setDatagridData([target]);
      grid.setSelecteds("0");
      const selected = (grid.getSelecteds && grid.getSelecteds()[0]) || {};
      return {
        id: String(selected.id || ""),
        name: selected.name,
        produceBatchNum: selected.produceBatchNum,
        needParamAna: selected.needParamAna,
        processRunState:
          selected.processRunState && typeof selected.processRunState === "object"
            ? selected.processRunState.id
            : selected.processRunState,
        actStartTime: selected.actStartTime,
        actEndTime: selected.actEndTime,
      };
    },
    { code: gridId, target: row }
  );
}

async function capture(page, fileName) {
  const filePath = path.join(screenshotDir, fileName);
  ensureDir(filePath);
  await page.screenshot({ path: filePath, fullPage: true, timeout: Math.min(timeoutMs, 30000) });
  return filePath;
}

async function openActionPage(context, page, buttonId, report, label) {
  const popupPromise = context.waitForEvent("page", { timeout: timeoutMs });
  await page.locator(`#${buttonId}`).click();
  const popup = await popupPromise;
  attachPageEvidence(popup, report, label);
  await popup.waitForLoadState("domcontentloaded", { timeout: timeoutMs });
  await popup.waitForTimeout(1000);
  return popup;
}

async function navigationStatus(page) {
  return page.evaluate(() => {
    const navigation = window.performance.getEntriesByType("navigation")[0];
    return navigation && typeof navigation.responseStatus === "number"
      ? navigation.responseStatus
      : 0;
  });
}

function evaluateStatus(report) {
  const failures = [];
  const expectedButtons = ["btn-viewDetail", "btn-outptConsumption", "btn-manualStatistics"];
  if (report.navigation.status !== 200) {
    failures.push(`list navigation returned ${report.navigation.status}`);
  }
  expectedButtons.forEach((id) => {
    const button = report.toolbar.find((item) => item.id === id);
    if (!button || button.visible !== true) {
      failures.push(`${id} is not visible`);
    }
  });
  if (!report.noSelection.messages.some((item) => item[1] === "请选择一条工序执行记录")) {
    failures.push("no-selection warning was not shown");
  }
  if (report.selection.id !== processExecutionId || report.selection.needParamAna !== true) {
    failures.push(`controlled row selection failed: ${JSON.stringify(report.selection)}`);
  }
  if (
    report.detail.status !== 200 ||
    !report.detail.url.includes(`processExecutionId=${processExecutionId}`) ||
    !report.detail.body.includes("工序边界") ||
    !report.detail.body.includes("流量与波美值证据")
  ) {
    failures.push("process execution detail did not render the process boundary and telemetry evidence");
  }
  if (
    report.consumption.status !== 200 ||
    !report.consumption.url.includes(`id=${processExecutionId}`) ||
    !report.consumption.body.includes("投入记录") ||
    !report.consumption.body.includes("产出记录") ||
    !report.consumption.body.includes("消耗记录")
  ) {
    failures.push("process consumption page did not render its three business tabs");
  }
  if (
    report.statistics.httpStatus !== 200 ||
    report.statistics.responseCode !== 200 ||
    !report.statistics.messages.some((item) => item[1] === "工艺参数统计完成")
  ) {
    failures.push(`manual statistics request failed: ${JSON.stringify(report.statistics)}`);
  }
  const before = report.persistence.before || {};
  const after = report.persistence.after || {};
  if (
    typeof after.maxRevision !== "number" ||
    typeof after.rowCount !== "number" ||
    (after.maxRevision <= before.maxRevision && after.rowCount <= before.rowCount)
  ) {
    failures.push("manual statistics returned success without inserting or revising pa_trace_snapshots");
  }
  if (report.persistence.restoredNeedParamAna !== report.persistence.originalNeedParamAna) {
    failures.push("controlled need_param_ana fixture was not restored");
  }
  if (
    report.browser.console.length ||
    report.browser.pageErrors.length ||
    report.browser.requestFailures.length ||
    report.browser.failedResponses.length
  ) {
    failures.push(`browser errors were observed: ${JSON.stringify(report.browser)}`);
  }
  return { status: failures.length ? "FAIL" : "PASS", failures };
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

    const page = await context.newPage();
    page.setDefaultTimeout(timeoutMs);
    page.setDefaultNavigationTimeout(timeoutMs);
    attachPageEvidence(page, report, "list");

    const navigation = await page.goto(route, { waitUntil: "domcontentloaded", timeout: timeoutMs });
    report.navigation = { status: navigation && navigation.status(), url: page.url(), title: await page.title() };
    await waitForGrid(page);
    await page.waitForTimeout(1000);
    report.navigation.title = await page.title();
    report.toolbar = await page.evaluate(() =>
      ["btn-viewDetail", "btn-outptConsumption", "btn-manualStatistics", "btn-matConsumEntry"].map(
        (id) => {
          const element = document.getElementById(id);
          if (!element) {
            return { id, present: false, visible: false, text: "" };
          }
          const style = window.getComputedStyle(element);
          const rect = element.getBoundingClientRect();
          return {
            id,
            present: true,
            visible:
              rect.width > 0 &&
              rect.height > 0 &&
              style.display !== "none" &&
              style.visibility !== "hidden",
            text: (element.innerText || element.textContent || "").trim(),
          };
        }
      )
    );

    await page.locator("#btn-viewDetail").click();
    await page.waitForTimeout(300);
    report.noSelection = {
      messages: await page.evaluate(() => window.__adpProcessActionMessages || []),
      popupCount: context.pages().length - 1,
    };

    const targetRow = await listTargetRow(page);
    report.selection = await selectTargetRow(page, targetRow);
    report.screenshots.list = await capture(
      page,
      "wom-process-execution-actions-list-20260731.png"
    );

    const detailPage = await openActionPage(context, page, "btn-viewDetail", report, "detail");
    report.detail = {
      status: await navigationStatus(detailPage),
      url: detailPage.url(),
      title: await detailPage.title(),
      body: (await detailPage.locator("body").innerText()).slice(0, 8000),
    };
    report.screenshots.detail = await capture(
      detailPage,
      "wom-process-execution-actions-detail-20260731.png"
    );
    await detailPage.close();

    const consumptionPage = await openActionPage(
      context,
      page,
      "btn-outptConsumption",
      report,
      "consumption"
    );
    await consumptionPage.waitForFunction(
      () => {
        const text = document.body && document.body.innerText;
        return (
          text &&
          text.includes("投入记录") &&
          text.includes("产出记录") &&
          text.includes("消耗记录")
        );
      },
      null,
      { timeout: timeoutMs }
    );
    await consumptionPage.waitForTimeout(3000);
    await waitForLoadingToFinish(consumptionPage);
    report.consumption = {
      status: await navigationStatus(consumptionPage),
      url: consumptionPage.url(),
      title: await consumptionPage.title(),
      body: (await consumptionPage.locator("body").innerText()).slice(0, 8000),
    };
    report.screenshots.consumption = await capture(
      consumptionPage,
      "wom-process-execution-actions-consumption-20260731.png"
    );
    await consumptionPage.close();

    const responsePromise = page.waitForResponse(
      (response) =>
        response.url().includes("/ProcessAnalysis/paramStatRec/paramStatRec/manualStatProcess") &&
        response.url().includes(`processId=${processExecutionId}`),
      { timeout: timeoutMs }
    );
    await page.locator("#btn-manualStatistics").click();
    const statisticsResponse = await responsePromise;
    const statisticsBody = await readJsonSafe(statisticsResponse);
    await page.waitForTimeout(500);
    report.statistics = {
      method: statisticsResponse.request().method(),
      endpoint: statisticsResponse.url().replace(baseUrl, ""),
      httpStatus: statisticsResponse.status(),
      responseCode: statisticsBody.json && statisticsBody.json.code,
      response: statisticsBody.json,
      messages: await page.evaluate(() => window.__adpProcessActionMessages || []),
    };
    report.screenshots.statistics = await capture(
      page,
      "wom-process-execution-actions-statistics-20260731.png"
    );
  } finally {
    await browser.close();
  }
}

async function main() {
  ensureDir(outputPath);
  const originalRaw = runSql(
    `SELECT coalesce(need_param_ana::text, '') FROM public.wom_process_exelogs WHERE id = ${processExecutionId};\n`
  );
  const originalNeedParamAna = originalRaw.split(/\r?\n/)[0];
  const restoreLiteral = originalFlagLiteral(originalNeedParamAna);
  const beforeRows = parsePipeRows(
    runSql(
      `SELECT coalesce(max(revision), 0), count(*) FROM public.pa_trace_snapshots WHERE tenant_id = 'dt' AND source_type = 'PROCESS' AND source_id = ${processExecutionId};\n`
    )
  );
  const report = {
    generatedAt: new Date().toISOString(),
    repoCommit: execFileSync("git", ["rev-parse", "HEAD"], { encoding: "utf8" }).trim() + "+working-tree",
    database: "PostgreSQL",
    environment: baseUrl,
    marker,
    module: "WOM 工序执行记录",
    route,
    processExecutionId,
    status: "RUNNING",
    navigation: {},
    toolbar: [],
    noSelection: {},
    selection: {},
    detail: {},
    consumption: {},
    statistics: {},
    http: [],
    browser: { console: [], pageErrors: [], requestFailures: [], failedResponses: [] },
    screenshots: {},
    persistence: {
      table: "public.pa_trace_snapshots",
      originalNeedParamAna,
      before: {
        maxRevision: Number((beforeRows[0] && beforeRows[0][0]) || 0),
        rowCount: Number((beforeRows[0] && beforeRows[0][1]) || 0),
      },
      verificationSql:
        `SELECT id,tenant_id,source_type,source_id,task_id,batch_no,source_state,revision,updated_at ` +
        `FROM public.pa_trace_snapshots WHERE tenant_id='dt' AND source_type='PROCESS' ` +
        `AND source_id=${processExecutionId} ORDER BY revision;`,
    },
    issues: [],
  };

  let api;
  try {
    runSql(
      `UPDATE public.wom_process_exelogs SET need_param_ana = true WHERE id = ${processExecutionId};\n`
    );
    api = await request.newContext({ ignoreHTTPSErrors: true });
    const auth = await login(api);
    report.login = { status: auth.status };
    await runBrowser(auth.ticket, report);

    const afterRows = parsePipeRows(
      runSql(
        `SELECT coalesce(max(revision), 0), count(*) FROM public.pa_trace_snapshots WHERE tenant_id = 'dt' AND source_type = 'PROCESS' AND source_id = ${processExecutionId};\n`
      )
    );
    report.persistence.after = {
      maxRevision: Number((afterRows[0] && afterRows[0][0]) || 0),
      rowCount: Number((afterRows[0] && afterRows[0][1]) || 0),
    };
    report.persistence.rows = parsePipeRows(runSql(report.persistence.verificationSql + "\n"));
  } catch (error) {
    report.issues.push(error.stack || error.message);
  } finally {
    if (api) {
      await api.dispose();
    }
    try {
      runSql(
        `UPDATE public.wom_process_exelogs SET need_param_ana = ${restoreLiteral} WHERE id = ${processExecutionId};\n`
      );
      report.persistence.restoredNeedParamAna = runSql(
        `SELECT coalesce(need_param_ana::text, '') FROM public.wom_process_exelogs WHERE id = ${processExecutionId};\n`
      );
    } catch (error) {
      report.issues.push(`Fixture restore failed: ${error.stack || error.message}`);
    }
  }

  const status = evaluateStatus(report);
  report.status = report.issues.length ? "FAIL" : status.status;
  report.issues.push(...status.failures);
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
