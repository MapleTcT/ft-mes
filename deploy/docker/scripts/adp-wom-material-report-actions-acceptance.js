#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { execFileSync } = require("child_process");
const { chromium, request } = require("playwright");

const baseUrl = (process.env.ADP_BASE_URL || "http://10.11.100.17:18080").replace(/\/+$/, "");
const browserBaseUrl = (process.env.ADP_BROWSER_BASE_URL || baseUrl).replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD;

if (!password) {
  throw new Error("ADP_PASSWORD is required for the WOM material/report actions acceptance run");
}
const sshTarget = process.env.ADP_DB_SSH_TARGET || "v6@10.11.100.17";
const dbContainer = process.env.ADP_DB_CONTAINER || "adp-mes-newbase-postgres-1";
const dbName = process.env.ADP_DB_NAME || "adp";
const dbUser = process.env.ADP_DB_USER || "adp";
const timeoutMs = Number(process.env.ADP_PAGE_TIMEOUT_MS || 120000);
const popupTimeoutMs = Number(process.env.ADP_POPUP_TIMEOUT_MS || 10000);
const headless = process.env.ADP_HEADLESS !== "false";
const outputPath =
  process.env.ADP_WOM_MATERIAL_REPORT_OUTPUT ||
  "metadata/wom-material-report-actions-acceptance-20260801.json";
const screenshotDir =
  process.env.ADP_WOM_MATERIAL_REPORT_SCREENSHOT_DIR ||
  "/tmp/adp-wom-material-report-actions-20260801";
const selectedCaseKeys = new Set(
  String(process.env.ADP_WOM_MATERIAL_REPORT_CASES || "")
    .split(",")
    .map((value) => value.trim())
    .filter(Boolean)
);
const skipWms = process.env.ADP_WOM_MATERIAL_REPORT_SKIP_WMS === "true";

const pageCases = [
  {
    key: "prepare-return",
    module: "备料退料",
    route: "/msService/WOM/rejectMaterilal/rejectMaterial/prePareRejectList",
    permissionCode: "WOM_1.0.0_rejectMaterilal_prePareRejectList",
    gridId: "WOM_1.0.0_rejectMaterilal_prePareRejectList_rejectMaterial_sdg",
    expectedButtons: [["btn-add", "新增退料申请"], ["btn-viewDetail", "查看详情"]],
    addRoute: "/WOM/rejectMaterilal/rejectMaterial/prePareRejectEdit",
    detailRoutes: ["/WOM/rejectMaterilal/rejectMaterial/prePareRejectView"],
    selectionMessage: "请选择一条备料退料申请",
  },
  {
    key: "batch-return",
    module: "配料退料",
    route: "/msService/WOM/rejectMaterilal/rejectMaterial/batchRejectList",
    permissionCode: "WOM_1.0.0_rejectMaterilal_batchRejectList",
    gridId: "WOM_1.0.0_rejectMaterilal_batchRejectList_rejectMaterial_sdg",
    expectedButtons: [["btn-add", "新增退料申请"], ["btn-viewDetail", "查看详情"]],
    addRoute: "/WOM/rejectMaterilal/rejectMaterial/batchRejectEdit",
    detailRoutes: ["/WOM/rejectMaterilal/rejectMaterial/batchRejectView"],
    selectionMessage: "请选择一条配料退料申请",
  },
  {
    key: "workshop-return",
    module: "车间物料退料",
    route: "/msService/WOM/rejectMaterilal/rejectMaterial/materiaRejectList",
    permissionCode: "WOM_1.0.0_rejectMaterilal_materiaRejectList",
    gridId: "WOM_1.0.0_rejectMaterilal_materiaRejectList_rejectMaterial_sdg",
    expectedButtons: [["btn-add", "新增退料申请"], ["btn-viewDetail", "查看详情"]],
    addRoute: "/WOM/rejectMaterilal/rejectMaterial/materiaRejectEdit",
    detailRoutes: ["/WOM/rejectMaterilal/rejectMaterial/materiaRejectView"],
    selectionMessage: "请选择一条车间物料退料申请",
  },
  {
    key: "return-record",
    module: "退料记录",
    route: "/msService/WOM/rejectMaterilal/rejctMatalPart/batchRejectPrtList",
    permissionCode: "WOM_1.0.0_rejectMaterilal_batchRejectPrtList",
    gridId: "WOM_1.0.0_rejectMaterilal_batchRejectPrtList_rejctMatalPart_sdg",
    expectedButtons: [["btn-viewDetail", "查看退料单"]],
    detailRoutes: [
      "/WOM/rejectMaterilal/rejectMaterial/prePareRejectView",
      "/WOM/rejectMaterilal/rejectMaterial/batchRejectView",
      "/WOM/rejectMaterilal/rejectMaterial/materiaRejectView",
    ],
    selectionMessage: "请选择一条退料记录",
  },
  {
    key: "material-use",
    module: "生产用料单",
    route: "/msService/WOM/putInMaterial/putInMaterial/putinList",
    permissionCode: "WOM_1.0.0_putInMaterial_putinList",
    gridId: "WOM_1.0.0_putInMaterial_putinList_putInMaterial_sdg",
    expectedButtons: [["btn-viewDetail", "查看详情"]],
    detailRoutes: ["/WOM/putInMaterial/putInMaterial/putinView"],
    selectionMessage: "请选择一条生产用料单",
  },
  {
    key: "material-use-detail",
    module: "生产用料明细",
    route: "/msService/WOM/putInMaterial/putMateiDetail/putInDetailList",
    permissionCode: "WOM_1.0.0_putInMaterial_putInDetailList",
    gridId: "WOM_1.0.0_putInMaterial_putInDetailList_putMateiDetail_sdg",
    expectedButtons: [["btn-viewDetail", "查看详情"]],
    detailRoutes: ["/WOM/putInMaterial/putMateiDetail/putInDetailView"],
    selectionMessage: "请选择一条生产用料明细",
  },
  {
    key: "production-report",
    module: "生产报工单",
    route: "/msService/WOM/outputMaterial/outputMaterial/outputList",
    permissionCode: "WOM_1.0.0_outputMaterial_outputList",
    gridId: "WOM_1.0.0_outputMaterial_outputList_outputMaterial_sdg",
    expectedButtons: [["btn-viewDetail", "查看详情"]],
    detailRoutes: ["/WOM/outputMaterial/outputMaterial/outputView"],
    selectionMessage: "请选择一条生产报工单",
  },
  {
    key: "production-report-detail",
    module: "生产报工明细",
    route: "/msService/WOM/outputMaterial/outMateDetail/outputDetailList",
    permissionCode: "WOM_1.0.0_outputMaterial_outputDetailList",
    gridId: "WOM_1.0.0_outputMaterial_outputDetailList_outMateDetail_sdg",
    expectedButtons: [["btn-viewDetail", "查看报工单"]],
    detailRoutes: ["/WOM/outputMaterial/outputMaterial/outputView"],
    selectionMessage: "请选择一条生产报工明细",
  },
  {
    key: "remaining-material",
    module: "尾料记录",
    route: "/msService/WOM/remainMaterial/remainMaterial/remainMaterialList",
    permissionCode: "WOM_1.0.0_remainMaterial_remainMaterialList",
    gridId: "WOM_1.0.0_remainMaterial_remainMaterialList_remainMaterial_sdg",
    expectedButtons: [["btn-insertRow", "新增"], ["btn-update", "修改"], ["btn-deleteRow", "删除"]],
    sourceCrud: true,
    addRoute: "/WOM/remainMaterial/remainMaterial/remainMaterialEdit",
  },
  {
    key: "remaining-input",
    module: "尾料投料",
    route: "/msService/WOM/procReport/putinDetail/putinDetailList",
    permissionCode: "WOM_1.0.0_procReport_putinDetailList",
    gridId: "WOM_1.0.0_procReport_putinDetailList_putinDetail_sdg",
    expectedButtons: [["btn-viewTask", "查看指令"]],
    detailRoutes: ["/WOM/produceTask/produceTask/makeTaskView"],
    selectionMessage: "请选择一条尾料投料记录",
  },
  {
    key: "remaining-output",
    module: "尾料产出",
    route: "/msService/WOM/procReport/outputDetail/outputDetailList",
    permissionCode: "WOM_1.0.0_procReport_outputDetailList",
    gridId: "WOM_1.0.0_procReport_outputDetailList_outputDetail_sdg",
    expectedButtons: [["btn-viewTask", "查看指令"]],
    detailRoutes: ["/WOM/produceTask/produceTask/makeTaskView"],
    selectionMessage: "请选择一条尾料产出记录",
  },
];

function ensureDir(filePath) {
  fs.mkdirSync(path.dirname(path.resolve(filePath)), { recursive: true });
}

function shellQuote(value) {
  return `'${String(value).replace(/'/g, "'\\''")}'`;
}

function runSql(sql) {
  const command = [
    "docker", "exec", "-i", shellQuote(dbContainer), "psql",
    "-U", shellQuote(dbUser), "-d", shellQuote(dbName),
    "-v", "ON_ERROR_STOP=1", "-AtF", shellQuote("|"),
  ].join(" ");
  return execFileSync(
    "ssh",
    [
      "-o", "BatchMode=yes",
      "-o", "ConnectTimeout=8",
      "-o", "StrictHostKeyChecking=no",
      "-o", "UserKnownHostsFile=/dev/null",
      sshTarget,
      command,
    ],
    { input: sql, encoding: "utf8", stdio: ["pipe", "pipe", "pipe"] }
  ).trim();
}

function parsePipeRows(raw) {
  return raw.split(/\r?\n/).filter(Boolean).map((line) => line.split("|"));
}

function databaseSnapshot() {
  const tables = [
    "wom_reject_materials",
    "wom_rejct_matal_parts",
    "wom_put_in_materials",
    "wom_put_matei_details",
    "wom_output_materials",
    "wom_output_details",
    "wom_remain_materials",
    "wms_stock_documents",
    "wms_stock_document_lines",
    "wms_inventory_transactions",
  ];
  const sql = tables
    .map((table) => `SELECT '${table}', count(*)::text FROM public.${table}`)
    .join(" UNION ALL ") + ";\n";
  return Object.fromEntries(parsePipeRows(runSql(sql)).map(([table, count]) => [table, Number(count)]));
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
      headers: { Accept: "application/json, text/plain, */*", "Content-Type": "application/json;charset=UTF-8" },
    });
    const parsed = await readJsonSafe(response);
    const ticket = response.ok() ? findTicket(parsed.json) : null;
    if (ticket) return { ticket, status: response.status() };
    failures.push({ status: response.status(), body: parsed.text.slice(0, 300) });
  }
  throw new Error(`Login failed: ${JSON.stringify(failures)}`);
}

function messageProbe() {
  window.__adpMaterialReportMessages = [];
  window.setInterval(function patchMessageApi() {
    var api = window.ReactAPI;
    if (!api || api.__materialReportMessageProbe || typeof api.showMessage !== "function") return;
    var original = api.showMessage;
    api.showMessage = function showMessageWithProbe(type, message) {
      window.__adpMaterialReportMessages.push([String(type), String(message)]);
      return original.apply(this, arguments);
    };
    api.__materialReportMessageProbe = true;
  }, 100);
}

function ignoreRequestFailure(url) {
  return /favicon\.ico|sockjs|websocket|\/ws(?:\/|$)/i.test(url);
}

function attachPageEvidence(page, report, label) {
  page.on("console", (message) => {
    if (message.type() === "error") report.browser.consoleErrors.push({ page: label, text: message.text().slice(0, 1000) });
  });
  page.on("pageerror", (error) => report.browser.pageErrors.push({ page: label, error: error.message }));
  page.on("requestfailed", (requestItem) => {
    if (!ignoreRequestFailure(requestItem.url())) {
      report.browser.requestFailures.push({
        page: label,
        method: requestItem.method(),
        url: requestItem.url(),
        error: requestItem.failure() && requestItem.failure().errorText,
      });
    }
  });
  page.on("response", (response) => {
    const url = response.url();
    if (/rejectMaterial|batchRejectPrtList|putinList|putInDetailList|outputList|outputDetailList|remainMaterialList|completion-inbounds/.test(url)) {
      report.http.push({ page: label, method: response.request().method(), url, status: response.status() });
    }
    if (response.status() >= 400 && !ignoreRequestFailure(url)) {
      report.browser.failedResponses.push({ page: label, method: response.request().method(), url, status: response.status() });
    }
  });
}

async function waitForGrid(page, gridId) {
  await page.waitForFunction(
    (code) => {
      const factory = window.ReactAPI && window.ReactAPI.getComponentAPI("SupDataGrid");
      if (!factory || typeof factory.APIs !== "function") return false;
      const grid = factory.APIs(code);
      return Boolean(grid && typeof grid.getRows === "function");
    },
    gridId,
    { timeout: timeoutMs }
  );
}

async function readToolbar(page) {
  return page.locator("[id^=btn-]").evaluateAll((elements) =>
    elements.map((element) => {
      const style = window.getComputedStyle(element);
      const rect = element.getBoundingClientRect();
      return {
        id: element.id,
        text: (element.innerText || element.textContent || "").trim(),
        visible: rect.width > 0 && rect.height > 0 && style.display !== "none" && style.visibility !== "hidden",
      };
    })
  );
}

async function fetchRows(page, pageCase) {
  const endpoint = `${pageCase.route}-query`;
  const response = await page.request.post(`${baseUrl}${endpoint}`, {
    data: {
      classifyCodes: "",
      customCondition: {},
      permissionCode: pageCase.permissionCode,
      pageNo: 1,
      paging: true,
      pageSize: 200,
      crossCompanyFlag: "true",
    },
  });
  const parsed = await readJsonSafe(response);
  const rows =
    (parsed.json && parsed.json.data && parsed.json.data.result) ||
    (parsed.json && parsed.json.data && parsed.json.data.rows) ||
    (parsed.json && parsed.json.data && parsed.json.data.records) ||
    [];
  return {
    endpoint,
    status: response.status(),
    rows: Array.isArray(rows) ? rows : [],
    body: parsed.text.slice(0, 500),
  };
}

async function runVisibleQuery(page, pageCase) {
  const queryButton = page.locator('button[data-id="query"]');
  if (!(await queryButton.count())) return { status: "NOT_APPLICABLE" };
  const responsePromise = page.waitForResponse(
    (response) => response.url().includes(`${pageCase.route}-query`),
    { timeout: timeoutMs }
  );
  await queryButton.first().click();
  const response = await responsePromise;
  await page.waitForTimeout(500);
  return {
    status: response.status(),
    renderedRows: Math.max(0, (await page.locator(".sup-datagrid-row:visible").count()) - 1),
  };
}

async function injectAndSelectRow(page, gridId, row) {
  return page.evaluate(
    ({ code, target }) => {
      const grid = ReactAPI.getComponentAPI("SupDataGrid").APIs(code);
      grid.setDatagridData([target]);
      grid.setSelecteds("0");
      const selected = (grid.getSelecteds && grid.getSelecteds()[0]) || {};
      return {
        id: String(selected.id || ""),
        tableNo: selected.tableNo || "",
        headId: selected.headId || null,
        flatHeadId: selected["headId.id"] || null,
        flatRejectType: selected["headId.rejectType.id"] || null,
      };
    },
    { code: gridId, target: row }
  );
}

async function openPopup(context, trigger, report, label) {
  const popupPromise = context.waitForEvent("page", { timeout: popupTimeoutMs });
  await trigger();
  const popup = await popupPromise;
  popup.setDefaultTimeout(timeoutMs);
  popup.setDefaultNavigationTimeout(timeoutMs);
  attachPageEvidence(popup, report, label);
  await popup.waitForLoadState("domcontentloaded", { timeout: timeoutMs });
  await popup.waitForFunction(() => document.body && document.body.innerText.trim().length > 10, null, { timeout: timeoutMs });
  await popup.waitForTimeout(1000);
  return popup;
}

async function inspectPopup(popup) {
  const body = (await popup.locator("body").innerText()).slice(0, 12000);
  const navigationStatus = await popup.evaluate(() => {
    const navigation = window.performance.getEntriesByType("navigation")[0];
    return navigation && typeof navigation.responseStatus === "number" ? navigation.responseStatus : 0;
  });
  return {
    status: navigationStatus,
    url: popup.url(),
    title: await popup.title(),
    bodyLength: body.trim().length,
    body: body.slice(0, 600),
    inputCount: await popup.locator("input, textarea, select, [role=combobox]").count(),
    controlCount: await popup.locator("button, input, textarea, select, [role=combobox]").count(),
  };
}

function validPopup(result, routes, requireInput) {
  const forbidden = ["404", "Authorization Required", "数据库操作异常", "系统发生未知异常"];
  return Boolean(
    result &&
    result.status === 200 &&
    routes.some((route) => result.url.includes(route)) &&
    result.bodyLength > 20 &&
    result.controlCount > 0 &&
    (!requireInput || result.inputCount > 0) &&
    !forbidden.some((text) => result.body.includes(text))
  );
}

async function capture(page, name) {
  const filePath = path.join(screenshotDir, name);
  ensureDir(filePath);
  await page.screenshot({ path: filePath, fullPage: true, timeout: Math.min(timeoutMs, 30000) });
  return filePath;
}

async function readMessages(page) {
  return page.evaluate(() => window.__adpMaterialReportMessages || []);
}

async function runLegacyPage(context, pageCase, report) {
  const item = {
    module: pageCase.module,
    route: pageCase.route,
    operation: pageCase.sourceCrud ? "新增/修改/删除入口" : pageCase.addRoute ? "新增与查看" : "查看与导出",
    api: `${pageCase.route}-query`,
    method: "POST",
    requiresPersistence: false,
    tables: [],
    status: "RUNNING",
    toolbar: [],
    query: {},
    add: null,
    detail: null,
    doubleClick: null,
    issues: [],
  };
  report.items.push(item);
  const page = await context.newPage();
  page.setDefaultTimeout(timeoutMs);
  page.setDefaultNavigationTimeout(timeoutMs);
  attachPageEvidence(page, report, `${pageCase.key}-list`);

  try {
    const navigation = await page.goto(pageCase.route, { waitUntil: "domcontentloaded", timeout: timeoutMs });
    await waitForGrid(page, pageCase.gridId);
    await page.waitForTimeout(1000);
    item.navigation = { status: navigation && navigation.status(), url: page.url(), title: await page.title() };
    item.toolbar = await readToolbar(page);

    for (const [id, label] of pageCase.expectedButtons) {
      const button = item.toolbar.find((candidate) => candidate.id === id);
      if (!button || !button.visible || !button.text.includes(label)) {
        item.issues.push(`缺少按钮 ${id}/${label}: ${JSON.stringify(item.toolbar)}`);
      }
    }

    const query = await fetchRows(page, pageCase);
    const visibleQuery = await runVisibleQuery(page, pageCase);
    item.query = {
      endpoint: query.endpoint,
      status: query.status,
      rowCount: query.rows.length,
      visibleStatus: visibleQuery.status,
      renderedRows: visibleQuery.renderedRows,
    };
    item.screenshot = await capture(page, `${pageCase.key}.png`);
    if (query.status !== 200) item.issues.push(`列表接口 HTTP ${query.status}: ${query.body}`);
    if (typeof visibleQuery.status === "number" && visibleQuery.status !== 200) {
      item.issues.push(`页面查询操作 HTTP ${visibleQuery.status}`);
    }

    const missingButton = pageCase.expectedButtons.some(([id]) => {
      const button = item.toolbar.find((candidate) => candidate.id === id);
      return !button || !button.visible;
    });
    if (missingButton) throw new Error("页面动作未发布，跳过后续点击以避免等待超时");

    if (pageCase.sourceCrud) {
      const popup = await openPopup(
        context,
        () => page.locator("#btn-insertRow").click(),
        report,
        `${pageCase.key}-add`
      );
      item.add = await inspectPopup(popup);
      if (!validPopup(item.add, [pageCase.addRoute], true)) {
        item.issues.push(`新增尾料表单未正确渲染: ${JSON.stringify(item.add)}`);
      }
      await popup.close();
    } else {
      if (query.rows.length) {
        const visibleRows = page.locator(".sup-datagrid-row:visible");
        const visibleRowCount = await visibleRows.count();
        if (visibleRowCount < 2) {
          throw new Error(`列表接口有 ${query.rows.length} 条数据，但页面没有渲染可双击的真实业务行`);
        }
        const doublePopup = await openPopup(
          context,
          () => visibleRows.nth(1).dblclick({ position: { x: 20, y: 10 } }),
          report,
          `${pageCase.key}-double-click`
        );
        item.doubleClick = await inspectPopup(doublePopup);
        if (!validPopup(item.doubleClick, pageCase.detailRoutes, false)) item.issues.push(`双击详情未正确渲染: ${JSON.stringify(item.doubleClick)}`);
        await doublePopup.close();

        // The legacy toast raised by the no-selection action can detach the
        // datagrid double-click listener, so isolate the interaction checks.
        await page.reload({ waitUntil: "domcontentloaded", timeout: timeoutMs });
        await waitForGrid(page, pageCase.gridId);
        await page.waitForTimeout(500);
      } else {
        item.doubleClick = { status: "NOT_APPLICABLE", reason: "列表当前无业务数据" };
      }

      await page.evaluate(() => { window.__adpMaterialReportMessages = []; });
      const detailButtonId = pageCase.expectedButtons.find(([id]) => id === "btn-viewDetail" || id === "btn-viewTask")[0];
      await page.locator(`#${detailButtonId}`).click();
      await page.waitForTimeout(300);
      const messages = await readMessages(page);
      item.noSelection = { messages };
      if (!messages.some((entry) => entry.join(" ").includes(pageCase.selectionMessage))) {
        item.issues.push(`未显示无选择提示: ${JSON.stringify(messages)}`);
      }

      if (pageCase.addRoute) {
        const popup = await openPopup(context, () => page.locator("#btn-add").click(), report, `${pageCase.key}-add`);
        item.add = await inspectPopup(popup);
        if (!validPopup(item.add, [pageCase.addRoute], true)) item.issues.push(`新增表单未正确渲染: ${JSON.stringify(item.add)}`);
        await popup.close();
      }

      if (query.rows.length) {
        item.selection = await injectAndSelectRow(page, pageCase.gridId, query.rows[0]);
        const detailPopup = await openPopup(context, () => page.locator(`#${detailButtonId}`).click(), report, `${pageCase.key}-detail`);
        item.detail = await inspectPopup(detailPopup);
        if (!validPopup(item.detail, pageCase.detailRoutes, false)) item.issues.push(`查看详情未正确渲染: ${JSON.stringify(item.detail)}`);
        await detailPopup.close();
      } else {
        item.detail = { status: "NOT_APPLICABLE", reason: "列表当前无业务数据，已验证按钮及无选择提示" };
      }
    }
  } catch (error) {
    item.issues.push(error.stack || error.message);
  } finally {
    await page.close();
  }
  item.status = item.issues.length ? "FAIL" : "PASS";
}

async function runWmsPage(context, report) {
  const item = {
    module: "完工入库台账",
    route: "/msService/material/wms",
    operation: "查询/查看详情/双击详情/导出",
    api: "/msService/material/wms/completion-inbounds",
    method: "GET",
    requiresPersistence: false,
    tables: ["wms_stock_documents", "wms_stock_document_lines", "wms_inventory_transactions"],
    status: "RUNNING",
    issues: [],
  };
  report.items.push(item);
  const page = await context.newPage();
  page.setDefaultTimeout(timeoutMs);
  page.setDefaultNavigationTimeout(timeoutMs);
  attachPageEvidence(page, report, "completion-inbound-ledger");
  try {
    const responsePromise = page.waitForResponse((response) => response.url().includes("/material/wms/completion-inbounds?"), { timeout: timeoutMs });
    const navigation = await page.goto(item.route, { waitUntil: "domcontentloaded", timeout: timeoutMs });
    const response = await responsePromise;
    await page.waitForFunction(() => !document.getElementById("search").disabled, null, { timeout: timeoutMs });
    item.navigation = { status: navigation && navigation.status(), url: page.url(), title: await page.title() };
    item.query = { status: response.status(), rowCount: await page.locator("#rows tr").count() };
    item.toolbar = {
      viewDetail: await page.locator("#view-detail").innerText(),
      exportCurrent: await page.locator("#export-current").innerText(),
    };
    item.screenshot = await capture(page, "completion-inbound-ledger.png");
    if (response.status() !== 200) item.issues.push(`完工入库列表接口 HTTP ${response.status()}`);
    if (!item.query.rowCount) {
      item.issues.push("完工入库台账没有可用于详情与导出的真实记录");
    } else {
      await page.locator("#rows tr").first().click();
      if (await page.locator("#view-detail").isDisabled()) item.issues.push("选择记录后查看详情按钮仍不可用");
      await page.locator("#view-detail").click();
      await page.locator("#detail-dialog[open]").waitFor({ state: "visible", timeout: timeoutMs });
      item.detail = {
        hasInboundLines: await page.locator("#detail").getByText("入库明细", { exact: true }).count(),
        hasTransactions: await page.locator("#detail").getByText("库存流水", { exact: true }).count(),
      };
      if (!item.detail.hasInboundLines || !item.detail.hasTransactions) item.issues.push(`详情内容不完整: ${JSON.stringify(item.detail)}`);
      await page.locator("#close-detail").click();

      await page.locator("#rows tr").first().dblclick();
      await page.locator("#detail-dialog[open]").waitFor({ state: "visible", timeout: timeoutMs });
      item.doubleClick = { opened: true };
      await page.locator("#close-detail").click();

      const downloadPromise = page.waitForEvent("download", { timeout: timeoutMs });
      await page.locator("#export-current").click();
      const download = await downloadPromise;
      const downloadPath = path.join(screenshotDir, await download.suggestedFilename());
      await download.saveAs(downloadPath);
      item.export = { fileName: path.basename(downloadPath), bytes: fs.statSync(downloadPath).size };
      if (item.export.bytes < 20) item.issues.push(`导出文件为空: ${JSON.stringify(item.export)}`);
    }
  } catch (error) {
    item.issues.push(error.stack || error.message);
  } finally {
    await page.close();
  }
  item.status = item.issues.length ? "FAIL" : "PASS";
}

async function runBrowser(ticket, report) {
  const browser = await chromium.launch({ headless });
  try {
    const context = await browser.newContext({
      baseURL: browserBaseUrl,
      viewport: { width: 2048, height: 1080 },
      ignoreHTTPSErrors: true,
      acceptDownloads: true,
      extraHTTPHeaders: { Authorization: `Bearer ${ticket}` },
    });
    await context.addCookies([
      { name: "suposTicket", value: ticket, url: browserBaseUrl },
      { name: "SUPOS_TICKET", value: ticket, url: browserBaseUrl },
    ]);
    await context.addInitScript((token) => {
      try {
        window.localStorage.clear();
        window.sessionStorage.clear();
        ["suposTicket", "SUPOS_TICKET", "token"].forEach((key) => {
          window.localStorage.setItem(key, token);
          window.sessionStorage.setItem(key, token);
        });
      } catch (_error) {
        // Some initial empty documents block storage; the same script runs again after navigation.
      }
    }, ticket);
    await context.addInitScript(messageProbe);

    const selectedCases = selectedCaseKeys.size
      ? pageCases.filter((pageCase) => selectedCaseKeys.has(pageCase.key))
      : pageCases;
    for (const pageCase of selectedCases) await runLegacyPage(context, pageCase, report);
    if (!skipWms) await runWmsPage(context, report);
  } finally {
    await browser.close();
  }
}

async function main() {
  ensureDir(outputPath);
  fs.mkdirSync(screenshotDir, { recursive: true });
  const report = {
    generatedAt: new Date().toISOString(),
    repoCommit: execFileSync("git", ["rev-parse", "HEAD"], { encoding: "utf8" }).trim() + "+working-tree",
    database: "PostgreSQL",
    environment: baseUrl,
    module: "WOM 退料/用料/报工/尾料与 WMS 完工入库交互",
    status: "RUNNING",
    summary: {
      testedPages: (selectedCaseKeys.size ? pageCases.filter((pageCase) => selectedCaseKeys.has(pageCase.key)).length : pageCases.length) + (skipWms ? 0 : 1),
      pass: 0,
      fail: 0,
    },
    items: [],
    http: [],
    browser: { consoleErrors: [], pageErrors: [], requestFailures: [], failedResponses: [] },
    persistence: {
      reason: "本轮仅查看、新增空表单和本地网格插行，不提交业务数据",
      before: databaseSnapshot(),
      after: null,
      unchanged: false,
    },
    issues: [],
  };

  let api;
  try {
    api = await request.newContext({ ignoreHTTPSErrors: true });
    const auth = await login(api);
    report.login = { status: auth.status };
    await runBrowser(auth.ticket, report);
  } catch (error) {
    report.issues.push(error.stack || error.message);
  } finally {
    if (api) await api.dispose();
    report.persistence.after = databaseSnapshot();
    report.persistence.unchanged = JSON.stringify(report.persistence.before) === JSON.stringify(report.persistence.after);
  }

  if (!report.persistence.unchanged) report.issues.push(`只读验收改变了业务表行数: ${JSON.stringify(report.persistence)}`);
  if (report.browser.consoleErrors.length || report.browser.pageErrors.length || report.browser.requestFailures.length || report.browser.failedResponses.length) {
    report.issues.push(`浏览器观察到错误: ${JSON.stringify(report.browser)}`);
  }
  report.summary.pass = report.items.filter((item) => item.status === "PASS").length;
  report.summary.fail = report.items.filter((item) => item.status === "FAIL").length;
  report.status = report.issues.length || report.summary.fail || report.summary.pass !== report.summary.testedPages ? "FAIL" : "PASS";
  fs.writeFileSync(outputPath, JSON.stringify(report, null, 2) + "\n");
  process.stdout.write(`${report.status}: ${outputPath}\n`);
  if (report.status !== "PASS") process.exitCode = 1;
}

main().catch((error) => {
  process.stderr.write(`${error.stack || error.message}\n`);
  process.exitCode = 1;
});
