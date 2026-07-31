#!/usr/bin/env node
"use strict";

const fs = require("node:fs");
const path = require("node:path");
const { spawnSync } = require("node:child_process");
const { chromium, request } = require("playwright");

const repoRoot = path.resolve(__dirname, "../../..");
const stamp = new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14);
const baseUrl = (process.env.ADP_BASE_URL || "http://10.11.100.17:18080").replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD;
const outputDir =
  process.env.ADP_OUTPUT_DIR || path.join("/tmp", `adp-wts-business-actions-${stamp}`);
const outputPath =
  process.env.ADP_OUTPUT_PATH || path.join(outputDir, "wts-business-actions-results.json");
const headless = process.env.ADP_HEADLESS !== "false";
const verbose = process.env.ADP_VERBOSE === "true";
const systemChrome = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";
const browserExecutable =
  process.env.ADP_CHROME_EXECUTABLE ||
  (process.platform === "darwin" && fs.existsSync(systemChrome) ? systemChrome : undefined);
const visibleErrorPattern =
  /(数据库操作异常|系统发生未知异常|系统错误|服务器异常|SQLGrammarException|could not extract ResultSet|Bad value for type long)/i;

const pages = [
  {
    key: "soilWork",
    label: "动土安全作业",
    route: "/msService/WTS/workTicket/workTicket/soilWork",
    viewCode: "WTS_1.0.0_workTicket_soilWork",
    gridApi: "WTS_1.0.0_workTicket_soilWork_workTicket_sdg",
    expectedButtons: ["新增", "查看详情"],
    addRoute: "/msService/WTS/workTicket/workTicket/soilWorkEdit",
    detailRoute: "/msService/WTS/workTicket/workTicket/soilWorkView",
  },
  {
    key: "limitSpaceWork",
    label: "受限空间安全作业",
    route: "/msService/WTS/workTicket/workTicket/limitSpaceWork",
    viewCode: "WTS_1.0.0_workTicket_limitSpaceWork",
    gridApi: "WTS_1.0.0_workTicket_limitSpaceWork_workTicket_sdg",
    expectedButtons: ["新增", "查看详情"],
    addRoute: "/msService/WTS/workTicket/workTicket/limitSpaceWorkEdit",
    detailRoute: "/msService/WTS/workTicket/workTicket/limitSpaceView",
  },
  {
    key: "liftWork",
    label: "吊装安全作业",
    route: "/msService/WTS/workTicket/workTicket/liftWork",
    viewCode: "WTS_1.0.0_workTicket_liftWork",
    gridApi: "WTS_1.0.0_workTicket_liftWork_workTicket_sdg",
    expectedButtons: ["查看详情"],
    detailRoute: "/msService/WTS/workTicket/workTicket/liftWorkView",
  },
  {
    key: "electricityWork",
    label: "临时用电安全作业",
    route: "/msService/WTS/workTicket/workTicket/electricityWork",
    viewCode: "WTS_1.0.0_workTicket_electricityWork",
    gridApi: "WTS_1.0.0_workTicket_electricityWork_workTicket_sdg",
    expectedButtons: ["新增", "查看详情"],
    addRoute: "/msService/WTS/workTicket/workTicket/electricityEdit",
    detailRoute: "/msService/WTS/workTicket/workTicket/electricityView",
  },
  {
    key: "heightWork",
    label: "高处安全作业",
    route: "/msService/WTS/workTicket/workTicket/heightWork",
    viewCode: "WTS_1.0.0_workTicket_heightWork",
    gridApi: "WTS_1.0.0_workTicket_heightWork_workTicket_sdg",
    expectedButtons: ["新增", "查看详情"],
    addRoute: "/msService/WTS/workTicket/workTicket/heightWorkEdit",
    detailRoute: "/msService/WTS/workTicket/workTicket/heightWorkView",
  },
  {
    key: "firework",
    label: "动火安全作业",
    route: "/msService/WTS/workTicket/workTicket/firework",
    viewCode: "WTS_1.0.0_workTicket_firework",
    gridApi: "WTS_1.0.0_workTicket_firework_workTicket_sdg",
    expectedButtons: ["查看详情", "批量打印"],
    detailRoute: "/msService/WTS/workTicket/workTicket/fireworkView",
    batchPrint: true,
  },
  {
    key: "breakWork",
    label: "断路安全作业",
    route: "/msService/WTS/workTicket/workTicket/breakWork",
    viewCode: "WTS_1.0.0_workTicket_breakWork",
    gridApi: "WTS_1.0.0_workTicket_breakWork_workTicket_sdg",
    expectedButtons: ["新增", "查看详情"],
    addRoute: "/msService/WTS/workTicket/workTicket/breakWorkEdit",
    detailRoute: "/msService/WTS/workTicket/workTicket/breakWorkView",
  },
  {
    key: "blockWork",
    label: "盲板抽堵安全作业",
    route: "/msService/WTS/workTicket/workTicket/blockWork",
    viewCode: "WTS_1.0.0_workTicket_blockWork",
    gridApi: "WTS_1.0.0_workTicket_blockWork_workTicket_sdg",
    expectedButtons: ["查看详情"],
    detailRoute: "/msService/WTS/workTicket/workTicket/blockWorkView",
  },
  {
    key: "workList",
    label: "作业台账",
    route: "/msService/WTS/workTicket/workTicket/workList",
    viewCode: "WTS_1.0.0_workTicket_workList",
    gridApi: "WTS_1.0.0_workTicket_workList_workTicket_sdg",
    expectedButtons: ["查看详情", "导出"],
    dynamicDetail: true,
    exportRoute: "/msService/WTS/workTicket/workTicket/workList-query",
  },
  {
    key: "plateAccountList",
    label: "盲板台账",
    route: "/msService/WTS/blindPlateAccount/plateAccount/plateAccountList",
    viewCode: "WTS_1.0.0_blindPlateAccount_plateAccountList",
    expectedButtons: ["导出"],
    exportRoute: "/msService/WTS/blindPlateAccount/plateAccount/plateAccountList-query",
  },
  {
    key: "workStatistics",
    label: "作业统计",
    route: "/msService/WTS/workTicket/assWorkTickets/workTicket",
    viewCode: "WTS_1.0.0_workTicket_workTicket",
    expectedButtons: ["导出"],
    clientExport: true,
  },
];

const supportLayouts = [
  "WTS_1.0.0_workTicket_soilWorkEdit",
  "WTS_1.0.0_workTicket_limitSpaceWorkEdit",
  "WTS_1.0.0_workTicket_electricityEdit",
  "WTS_1.0.0_workTicket_heightWorkEdit",
  "WTS_1.0.0_workTicket_breakWorkEdit",
  "WTS_1.0.0_workTicket_soilWorkView",
  "WTS_1.0.0_workTicket_limitSpaceView",
  "WTS_1.0.0_workTicket_liftWorkView",
  "WTS_1.0.0_workTicket_electricityView",
  "WTS_1.0.0_workTicket_heightWorkView",
  "WTS_1.0.0_workTicket_fireworkView",
  "WTS_1.0.0_workTicket_breakWorkView",
  "WTS_1.0.0_workTicket_blockWorkView",
];

function progress(message) {
  if (verbose) {
    console.error(`[wts-business-actions] ${message}`);
  }
}

function repoCommit() {
  const result = spawnSync("git", ["rev-parse", "HEAD"], {
    cwd: repoRoot,
    encoding: "utf8",
  });
  return result.status === 0 ? result.stdout.trim() : "UNKNOWN";
}

function ticketFrom(payload) {
  const candidates = [
    payload && payload.ticket,
    payload && payload.token,
    payload && payload.access_token,
    payload && payload.data && payload.data.ticket,
    payload && payload.data && payload.data.token,
    payload && payload.data && payload.data.access_token,
  ];
  return candidates.find((value) => typeof value === "string" && value.length > 20);
}

async function responseBody(response) {
  const text = await response.text();
  try {
    return { text, json: JSON.parse(text) };
  } catch (_error) {
    return { text, json: null };
  }
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
        Accept: "application/json",
        "Content-Type": "application/json;charset=UTF-8",
      },
    });
    const parsed = await responseBody(response);
    const ticket = response.ok() ? ticketFrom(parsed.json) : null;
    if (ticket) {
      return { ticket, status: response.status() };
    }
    failures.push({ status: response.status(), body: parsed.text.slice(0, 300) });
  }
  throw new Error(`Login failed: ${JSON.stringify(failures)}`);
}

function normalizeText(value) {
  return String(value || "").replace(/\s+/g, " ").trim();
}

async function visibleExactText(page, text) {
  const matches = page.getByText(text, { exact: true });
  const count = await matches.count();
  for (let index = 0; index < count; index += 1) {
    if (await matches.nth(index).isVisible().catch(() => false)) {
      return matches.nth(index);
    }
  }
  return null;
}

async function checkLayout(context, target) {
  const endpoint = `/msService/baseService/view/layoutJson?viewCode=${encodeURIComponent(
    target.viewCode
  )}&isEs5=true`;
  const response = await context.request.get(endpoint);
  const parsed = await responseBody(response);
  const item = {
    label: target.label,
    viewCode: target.viewCode,
    endpoint,
    httpStatus: response.status(),
    responseBytes: Buffer.byteLength(parsed.text, "utf8"),
    expectedButtons: target.expectedButtons || [],
    status: "PASS",
    issues: [],
  };
  if (response.status() !== 200) {
    item.issues.push(`layoutJson returned HTTP ${response.status()}`);
  }
  if (!parsed.text || visibleErrorPattern.test(parsed.text)) {
    item.issues.push("layoutJson contains an empty or server-error payload");
  }
  for (const button of item.expectedButtons) {
    if (!parsed.text.includes(button)) {
      item.issues.push(`layoutJson 缺少动作：${button}`);
    }
  }
  if (target.key === "heightWork" && !parsed.text.includes("heightWorkEdit")) {
    item.issues.push("高处作业新增未指向 heightWorkEdit");
  }
  if (target.key === "heightWork" && parsed.text.includes('"viewselect":{"title":"","code":"WTS_1.0.0_workTicket_liftWorkEdit"')) {
    item.issues.push("高处作业仍错误指向 liftWorkEdit");
  }
  if (item.issues.length) {
    item.status = "FAIL";
  }
  return item;
}

async function detectInteractionSurface(page, before) {
  const popup = before.popup;
  if (popup) {
    await popup.waitForLoadState("domcontentloaded", { timeout: 30000 }).catch(() => {});
    await popup.waitForTimeout(800);
    return { kind: "popup", surface: popup, url: popup.url() };
  }
  const frames = page.locator("iframe:visible");
  const frameCount = await frames.count();
  if (frameCount > before.frameCount) {
    const frame = frames.last().contentFrame();
    if (frame) {
      await frame.locator("body").waitFor({ state: "visible", timeout: 15000 });
      return { kind: "frame", surface: frame, url: page.url() };
    }
  }
  const dialogs = page.locator(".ant-modal:visible, .layui-layer:visible, [role='dialog']:visible");
  if ((await dialogs.count()) > before.dialogCount) {
    return { kind: "dialog", surface: dialogs.last(), url: page.url() };
  }
  if (page.url() !== before.url) {
    return { kind: "page", surface: page, url: page.url() };
  }
  return null;
}

async function clickOpeningAction(page, buttonText, expectedRoute) {
  const button = await visibleExactText(page, buttonText);
  if (!button) {
    throw new Error(`${buttonText}按钮不可见`);
  }
  const before = {
    url: page.url(),
    frameCount: await page.locator("iframe:visible").count(),
    dialogCount: await page
      .locator(".ant-modal:visible, .layui-layer:visible, [role='dialog']:visible")
      .count(),
  };
  const observed = [];
  const listener = (response) => {
    if (response.url().includes(expectedRoute)) {
      observed.push({
        status: response.status(),
        method: response.request().method(),
        url: response.url(),
      });
    }
  };
  page.on("response", listener);
  const popupPromise = page.waitForEvent("popup", { timeout: 5000 }).catch(() => null);
  await button.click();
  before.popup = await popupPromise;
  await page.waitForTimeout(1400);
  page.off("response", listener);
  const opened = await detectInteractionSurface(page, before);
  if (!opened) {
    throw new Error(`${buttonText}未打开目标页面`);
  }
  const bodyText = normalizeText(
    opened.kind === "dialog"
      ? await opened.surface.innerText()
      : await opened.surface.locator("body").innerText()
  );
  const routeObserved =
    opened.url.includes(expectedRoute) || observed.some((entry) => entry.status < 400);
  if (!routeObserved) {
    throw new Error(`${buttonText}未请求目标路由 ${expectedRoute}`);
  }
  if (!bodyText || visibleErrorPattern.test(bodyText)) {
    throw new Error(`${buttonText}打开的页面为空白或显示系统异常`);
  }
  const result = {
    status: "PASS",
    surfaceKind: opened.kind,
    url: opened.url,
    responses: observed,
    bodyTextExcerpt: bodyText.slice(0, 500),
  };
  if (opened.kind === "popup") {
    await opened.surface.close();
  } else if (opened.kind === "page") {
    await page.goBack({ waitUntil: "domcontentloaded" });
    await page.waitForTimeout(900);
  } else {
    const close = page
      .locator(".ant-modal-close:visible, .layui-layer-close:visible, [title='关闭']:visible")
      .last();
    if (await close.isVisible().catch(() => false)) {
      await close.click().catch(() => {});
      await page.waitForTimeout(300);
    }
  }
  return result;
}

async function selectFirstBusinessRow(page, target) {
  if (target.gridApi) {
    const apiSelection = await page.evaluate((gridCode) => {
      try {
        const factory = window.ReactAPI && window.ReactAPI.getComponentAPI("SupDataGrid");
        const grid = factory && typeof factory.APIs === "function" && factory.APIs(gridCode);
        const rows =
          (grid && typeof grid.getRows === "function" && grid.getRows()) ||
          (grid && typeof grid.getDatagridData === "function" && grid.getDatagridData()) ||
          [];
        if (!grid || !rows.length || typeof grid.setSelecteds !== "function") {
          return { selected: false, rowCount: rows.length };
        }
        grid.setSelecteds("0");
        const selecteds = typeof grid.getSelecteds === "function" ? grid.getSelecteds() : [];
        return {
          selected: selecteds.length === 1,
          rowCount: rows.length,
          row: selecteds[0] || rows[0] || null,
        };
      } catch (error) {
        return { selected: false, error: error.message };
      }
    }, target.gridApi);
    if (apiSelection.selected) {
      return {
        selected: true,
        rowText: normalizeText(JSON.stringify(apiSelection.row || {})).slice(0, 500),
      };
    }
  }

  const rows = page.locator(
    ".sup-datagrid-body-row-wrap > *, .ant-table-tbody tr, tbody tr, [role='row']"
  );
  const count = await rows.count();
  for (let index = 0; index < count; index += 1) {
    const row = rows.nth(index);
    if (!(await row.isVisible().catch(() => false))) {
      continue;
    }
    const text = normalizeText(await row.innerText().catch(() => ""));
    if (!text || /暂无数据|没有数据|No data/i.test(text)) {
      continue;
    }
    const checkboxInput = row.locator("input[type='checkbox'], .ant-checkbox-input").first();
    if ((await checkboxInput.count()) > 0) {
      await checkboxInput.check({ force: true }).catch(async () => {
        await checkboxInput.click({ force: true }).catch(() => {});
      });
    } else {
      const checkboxSurface = row
        .locator(".ant-checkbox-wrapper, .ant-checkbox, [role='checkbox'], td:first-child")
        .first();
      if ((await checkboxSurface.count()) > 0) {
        await checkboxSurface.click({ force: true }).catch(() => {});
      } else {
        await row.click({ force: true }).catch(() => {});
      }
    }
    await page.waitForTimeout(250);
    return { selected: true, rowText: text.slice(0, 500) };
  }
  return { selected: false, rowText: "" };
}

async function checkDetailInteraction(page, target) {
  const selection = await selectFirstBusinessRow(page, target);
  if (!selection.selected) {
    const button = await visibleExactText(page, "查看详情");
    if (!button) {
      throw new Error("查看详情按钮不可见");
    }
    await button.click();
    await page.waitForTimeout(500);
    const bodyText = normalizeText(await page.locator("body").innerText());
    if (!/请选择一条/.test(bodyText)) {
      throw new Error("无数据时查看详情未给出选择提示");
    }
    return {
      status: "BLOCKED",
      reason: "当前列表无业务记录，已验证无选择提示",
    };
  }

  if (target.dynamicDetail) {
    const button = await visibleExactText(page, "查看详情");
    const popupPromise = page.waitForEvent("popup", { timeout: 5000 }).catch(() => null);
    await button.click();
    const popup = await popupPromise;
    if (!popup) {
      throw new Error("作业台账查看详情未打开票种详情页");
    }
    await popup.waitForLoadState("domcontentloaded", { timeout: 30000 }).catch(() => {});
    await popup.waitForTimeout(800);
    const popupText = normalizeText(await popup.locator("body").innerText());
    const knownDetailRoute = /\/(firework|limitSpaceView|blockWorkView|heightWorkView|liftWorkView|electricityView|soilWorkView|breakWorkView)/.test(
      popup.url()
    );
    const result = {
      status: knownDetailRoute && !visibleErrorPattern.test(popupText) ? "PASS" : "FAIL",
      url: popup.url(),
      rowText: selection.rowText,
      bodyTextExcerpt: popupText.slice(0, 500),
    };
    await popup.close();
    if (result.status !== "PASS") {
      throw new Error("作业台账未按作业类型打开正确详情页");
    }
    return result;
  }

  return {
    ...(await clickOpeningAction(page, "查看详情", target.detailRoute)),
    rowText: selection.rowText,
  };
}

async function checkExportInteraction(page, target) {
  const button = await visibleExactText(page, "导出");
  if (!button) {
    throw new Error("导出按钮不可见");
  }
  const responsePromise = target.clientExport
    ? Promise.resolve(null)
    : page
        .waitForResponse(
          (response) =>
            response.url().includes(target.exportRoute) && response.request().method() === "POST",
          { timeout: 20000 }
        )
        .catch(() => null);
  const downloadPromise = page.waitForEvent("download", { timeout: 20000 }).catch(() => null);
  await button.click();
  const [response, download] = await Promise.all([responsePromise, downloadPromise]);
  const result = {
    status: "PASS",
    responseStatus: response ? response.status() : null,
    responseUrl: response ? response.url() : null,
    downloadSuggestedFilename: download ? download.suggestedFilename() : null,
    issues: [],
  };
  if (!target.clientExport && !response) {
    result.issues.push("未观察到导出 POST 请求");
  } else if (response && response.status() >= 400) {
    result.issues.push(`导出接口返回 HTTP ${response.status()}`);
  }
  if (!download) {
    result.issues.push("未触发浏览器下载");
  } else if (target.clientExport && !/\.csv$/i.test(download.suggestedFilename())) {
    result.issues.push(`统计导出文件类型不正确：${download.suggestedFilename()}`);
  }
  if (result.issues.length) {
    result.status = "FAIL";
    throw new Error(result.issues.join("；"));
  }
  return result;
}

async function clearGridSelection(page, gridApi) {
  if (!gridApi) {
    return;
  }
  await page.evaluate((gridCode) => {
    const factory = window.ReactAPI && window.ReactAPI.getComponentAPI("SupDataGrid");
    const grid = factory && typeof factory.APIs === "function" && factory.APIs(gridCode);
    if (grid && typeof grid.setSelecteds === "function") {
      grid.setSelecteds("");
    }
  }, gridApi);
}

async function checkBatchPrintInteraction(page, target) {
  const button = await visibleExactText(page, "批量打印");
  if (!button) {
    throw new Error("批量打印按钮不可见");
  }
  await clearGridSelection(page, target.gridApi);
  await button.click();
  await page.waitForTimeout(600);
  let bodyText = normalizeText(await page.locator("body").innerText());
  const noSelectionFeedback = /请选择|选择一条|选择数据/.test(bodyText);
  if (!noSelectionFeedback) {
    throw new Error("批量打印在未选择数据时没有给出提示");
  }

  return {
    status: "BLOCKED",
    reason: "已验证未选择数据提示；测试环境未安装并授权标准打印控件，未调用本机打印进程",
    noSelectionFeedback,
  };
}

async function checkPage(page, target, evidence) {
  const item = {
    module: target.label,
    route: target.route,
    viewCode: target.viewCode,
    expectedButtons: target.expectedButtons,
    visibleButtons: [],
    httpStatus: null,
    status: "PASS",
    issues: [],
  };
  const firstServerError = evidence.frontend.serverErrors.length;
  const firstClientError = evidence.frontend.clientErrors.length;
  try {
    progress(`opening ${target.label}`);
    const navigation = await page.goto(target.route, {
      waitUntil: "domcontentloaded",
      timeout: 60000,
    });
    item.httpStatus = navigation && navigation.status();
    await page.waitForTimeout(2200);
    const bodyText = normalizeText(await page.locator("body").innerText());
    item.bodyTextExcerpt = bodyText.slice(0, 1200);
    for (const buttonText of target.expectedButtons) {
      if (await visibleExactText(page, buttonText)) {
        item.visibleButtons.push(buttonText);
      }
    }
    const missing = target.expectedButtons.filter((text) => !item.visibleButtons.includes(text));
    if (missing.length) {
      item.issues.push(`缺少按钮：${missing.join("、")}`);
    }
    const visibleError = bodyText.match(visibleErrorPattern);
    if (visibleError) {
      item.issues.push(`页面显示错误：${visibleError[0]}`);
    }
    if (target.addRoute && !missing.includes("新增")) {
      try {
        item.addInteraction = await clickOpeningAction(page, "新增", target.addRoute);
      } catch (error) {
        item.issues.push(error.message);
      }
    }
    // Check the print guard before detail navigation selects a business row.
    // The recovered SupDataGrid keeps that selection after its popup closes.
    if (target.batchPrint && !missing.includes("批量打印")) {
      try {
        item.batchPrintInteraction = await checkBatchPrintInteraction(page, target);
      } catch (error) {
        item.issues.push(error.message);
      }
    }
    if (target.detailRoute || target.dynamicDetail) {
      try {
        item.detailInteraction = await checkDetailInteraction(page, target);
      } catch (error) {
        item.issues.push(error.message);
      }
    }
    if ((target.exportRoute || target.clientExport) && !missing.includes("导出")) {
      try {
        item.exportInteraction = await checkExportInteraction(page, target);
      } catch (error) {
        item.issues.push(error.message);
      }
    }
    item.serverErrors = evidence.frontend.serverErrors.slice(firstServerError);
    item.clientErrors = evidence.frontend.clientErrors.slice(firstClientError);
    if (item.serverErrors.length) {
      item.issues.push(`出现 ${item.serverErrors.length} 个 HTTP 5xx`);
    }
    const screenshot = path.join(outputDir, `${target.key}.png`);
    await page.screenshot({ path: screenshot, fullPage: true });
    item.screenshot = screenshot;
  } catch (error) {
    item.issues.push(error.message);
  }
  if (item.issues.length) {
    item.status = "FAIL";
  }
  return item;
}

async function main() {
  if (!password) {
    throw new Error("ADP_PASSWORD is required");
  }
  fs.mkdirSync(outputDir, { recursive: true });
  const evidence = {
    schemaVersion: 1,
    generatedAt: new Date().toISOString(),
    repoCommit: repoCommit(),
    database: "PostgreSQL",
    baseUrl,
    status: "RUNNING",
    summary: {
      testedFeatures: pages.length + pages.length + supportLayouts.length,
      pass: 0,
      fail: 0,
      blocked: 0,
      notApplicable: 0,
      blockedInteractions: 0,
      pages: pages.length,
      layouts: pages.length + supportLayouts.length,
    },
    frontend: {
      pages: [],
      layouts: [],
      console: [],
      pageErrors: [],
      requestFailures: [],
      clientErrors: [],
      serverErrors: [],
    },
    persistence: {
      required: false,
      status: "NOT_APPLICABLE",
      reason: "本轮验证动作入口、详情、打印提示和只读导出，不提交作业票业务单据。",
    },
    issues: [],
  };

  let browser;
  try {
    const api = await request.newContext({ ignoreHTTPSErrors: true });
    const loginResult = await login(api);
    await api.dispose();
    evidence.login = { status: loginResult.status, authenticated: true };

    browser = await chromium.launch({
      headless,
      ...(browserExecutable ? { executablePath: browserExecutable } : {}),
    });
    const context = await browser.newContext({
      baseURL: baseUrl,
      ignoreHTTPSErrors: true,
      acceptDownloads: true,
      viewport: { width: 1920, height: 1080 },
    });
    await context.addCookies([
      { name: "suposTicket", value: loginResult.ticket, url: baseUrl },
      { name: "SUPOS_TICKET", value: loginResult.ticket, url: baseUrl },
    ]);
    await context.addInitScript((ticket) => {
      localStorage.setItem("ticket", ticket);
      localStorage.setItem("suposTicket", ticket);
      localStorage.setItem("SUPOS_TICKET", ticket);
      localStorage.setItem("token", ticket);
    }, loginResult.ticket);

    for (const target of pages) {
      const item = await checkLayout(context, target);
      evidence.frontend.layouts.push(item);
      evidence.summary[item.status === "PASS" ? "pass" : "fail"] += 1;
    }
    for (const viewCode of supportLayouts) {
      const item = await checkLayout(context, {
        label: viewCode,
        viewCode,
        expectedButtons: [],
      });
      evidence.frontend.layouts.push(item);
      evidence.summary[item.status === "PASS" ? "pass" : "fail"] += 1;
    }

    const page = await context.newPage();
    page.setDefaultTimeout(15000);
    let activeStage = "initialization";
    page.on("console", (message) => {
      if (["error", "warning"].includes(message.type())) {
        evidence.frontend.console.push({
          stage: activeStage,
          type: message.type(),
          text: message.text(),
        });
      }
    });
    page.on("pageerror", (error) => {
      evidence.frontend.pageErrors.push({
        stage: activeStage,
        name: error.name,
        message: error.message,
      });
    });
    page.on("requestfailed", (requestItem) => {
      evidence.frontend.requestFailures.push({
        stage: activeStage,
        method: requestItem.method(),
        url: requestItem.url(),
        failure: requestItem.failure(),
      });
    });
    page.on("response", (response) => {
      const entry = {
        stage: activeStage,
        status: response.status(),
        method: response.request().method(),
        url: response.url(),
      };
      if (response.status() >= 400 && response.status() < 500) {
        evidence.frontend.clientErrors.push(entry);
      }
      if (response.status() >= 500) {
        evidence.frontend.serverErrors.push(entry);
      }
    });

    for (const target of pages) {
      activeStage = target.label;
      const item = await checkPage(page, target, evidence);
      evidence.frontend.pages.push(item);
      evidence.summary[item.status === "PASS" ? "pass" : "fail"] += 1;
      if (item.detailInteraction && item.detailInteraction.status === "BLOCKED") {
        evidence.summary.blockedInteractions += 1;
      }
      if (item.batchPrintInteraction && item.batchPrintInteraction.status === "BLOCKED") {
        evidence.summary.blockedInteractions += 1;
      }
    }

    const blockingConsole = evidence.frontend.console.filter(
      (entry) => entry.type === "error" && !/favicon|ResizeObserver|Failed to load resource.*404/i.test(entry.text)
    );
    if (evidence.summary.fail || evidence.frontend.pageErrors.length || blockingConsole.length) {
      throw new Error("WTS 业务管理/统计分析页面回归未通过");
    }
    evidence.status = "PASS";
  } catch (error) {
    evidence.status = "FAIL";
    evidence.issues.push(error.message);
  } finally {
    if (browser) {
      await browser.close();
    }
    fs.writeFileSync(outputPath, `${JSON.stringify(evidence, null, 2)}\n`, "utf8");
  }

  console.log(
    JSON.stringify(
      {
        status: evidence.status,
        outputPath,
        summary: evidence.summary,
        pages: evidence.frontend.pages.map((item) => ({
          label: item.module,
          status: item.status,
          visibleButtons: item.visibleButtons,
          addStatus: item.addInteraction && item.addInteraction.status,
          detailStatus: item.detailInteraction && item.detailInteraction.status,
          batchPrintStatus: item.batchPrintInteraction && item.batchPrintInteraction.status,
          exportStatus: item.exportInteraction && item.exportInteraction.status,
          issues: item.issues,
        })),
        issues: evidence.issues,
      },
      null,
      2
    )
  );
  if (evidence.status !== "PASS") {
    process.exitCode = 1;
  }
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
