#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { chromium, request } = require("playwright");

const baseUrl = (process.env.ADP_BASE_URL || "http://10.11.100.17:18080").replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD;
const headless = process.env.ADP_HEADLESS !== "false";
const outputDir =
  process.env.ADP_OUTPUT_DIR ||
  path.join("/tmp", `adp-lims-sample-actions-${new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14)}`);

if (!password) throw new Error("ADP_PASSWORD is required");

const pageSpecs = [
  ["LIMSSample_5.0.0.0_sample_sampleRegisterLayout", ["登记", "设置检测项目", "取消"], "登记"],
  ["LIMSBasic_1.0.0_testPlan_planSetSampleList", ["生成样品"]],
  ["LIMSSample_5.0.0.0_sample_batchSampleRegister", []],
  ["LIMSSample_5.0.0.0_sample_sampTaskAlcatLayOutNew", ["任务分配"], "任务分配"],
  ["LIMSSample_5.0.0.0_sample_collectListLayoutNew", ["取样", "取样信息设置"]],
  ["LIMSSample_5.0.0.0_sample_receiveListLayoutNew", ["收样", "设置检测项目"]],
  ["LIMSSample_5.0.0.0_sample_sampleSweepReceive", ["提交", "删行"]],
  ["LIMSSample_5.0.0.0_sample_makeListLayoutNew", ["制样"]],
  ["LIMSSample_5.0.0.0_sample_handoverListLayoutNew", ["领用"]],
  ["LIMSSample_5.0.0.0_sample_batchRecordByTest", []],
  ["LIMSSample_5.0.0.0_sample_recordBySingleSample", ["环境条件记录"]],
  ["LIMSSample_5.0.0.0_sample_recordByTest", []],
  ["LIMSSample_5.0.0.0_sample_recordBySample", ["环境条件记录"]],
  ["LIMSSample_5.0.0.0_sample_recordCheckBySample", []],
  ["LIMSSample_5.0.0.0_sample_recordCheckByTest", ["刷新"]],
  ["LIMSSample_5.0.0.0_sample_sampleCheck", ["样品查看"]],
  ["LIMSSample_5.0.0.0_sample_sampleRefuse", []],
  ["LIMSSample_5.0.0.0_sample_sampleAccept", []],
  [
    "LIMSSample_5.0.0.0_sample_sampleDealListLayout",
    ["样品信息修改", "设置检测项目", "取消", "恢复", "重新取样检验", "激活", "删除"],
  ],
  ["LIMSSample_5.0.0.0_sample_remainSampleLayout", ["归还", "销毁"], "归还"],
  ["LIMSSample_5.0.0.0_sample_retainListLayout", ["留样"]],
  ["LIMSSample_5.0.0.0_sample_sampleTestProgress", []],
  ["LIMSSample_5.0.0.0_sample_sampleInfoLayout", ["样品查看", "处理记录查看"]],
  ["LIMSSample_5.0.0.0_sampleReport_sampleReportList", []],
].map(([code, expectedButtons, safeClick]) => ({ code, expectedButtons, safeClick }));

const visibleErrorPattern =
  /(系统错误|系统异常|发生未知异常|数据库操作异常|404_NOT_FOUND|500 INTERNAL|Invalid bound statement|relation .* does not exist|column .* does not exist)/i;

function pickArray(...values) {
  return values.find((value) => Array.isArray(value)) || [];
}

function roots(payload) {
  return pickArray(
    payload,
    payload && payload.list,
    payload && payload.data,
    payload && payload.data && payload.data.list,
    payload && payload.data && payload.data.menus,
    payload && payload.result,
    payload && payload.result && payload.result.list,
    payload && payload.result && payload.result.menus
  );
}

function children(node) {
  return pickArray(
    node && node.children,
    node && node.childrens,
    node && node.childMenus,
    node && node.subMenus,
    node && node.nodes,
    node && node.menuList
  );
}

function findNode(nodes, code) {
  for (const node of nodes) {
    if (node && (node.code === code || node.menuCode === code)) return node;
    const nested = findNode(children(node), code);
    if (nested) return nested;
  }
  return null;
}

function findTicket(payload) {
  return [
    payload && payload.ticket,
    payload && payload.access_token,
    payload && payload.token,
    payload && payload.data && payload.data.ticket,
    payload && payload.data && payload.data.access_token,
    payload && payload.data && payload.data.token,
    payload && payload.result && payload.result.ticket,
  ].find((value) => typeof value === "string" && value.length > 20);
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
      headers: { "Content-Type": "application/json;charset=UTF-8" },
    });
    const parsed = await readJsonSafe(response);
    const ticket = response.ok() ? findTicket(parsed.json) : null;
    if (ticket) return { ticket, payload: parsed.json, status: response.status() };
    failures.push({ status: response.status(), body: parsed.text.slice(0, 300) });
  }
  throw new Error(`ADP login failed: ${JSON.stringify(failures)}`);
}

async function fetchMenuTree(api, ticket) {
  const response = await api.get(`${baseUrl}/inter-api/rbac/v1/menus/currentUser`, {
    headers: {
      Authorization: `Bearer ${ticket}`,
      Cookie: `suposTicket=${encodeURIComponent(ticket)}`,
      langu_code: "zh_CN",
    },
  });
  const parsed = await readJsonSafe(response);
  if (!response.ok()) throw new Error(`Menu API ${response.status()}: ${parsed.text.slice(0, 500)}`);
  return parsed.json;
}

async function createBrowserContext(browser, auth) {
  const context = await browser.newContext({
    baseURL: baseUrl,
    ignoreHTTPSErrors: true,
    viewport: { width: 1920, height: 1080 },
    extraHTTPHeaders: { Authorization: `Bearer ${auth.ticket}` },
  });
  await context.addCookies([
    { name: "suposTicket", value: auth.ticket, url: baseUrl },
    { name: "SUPOS_TICKET", value: auth.ticket, url: baseUrl },
  ]);
  await context.addInitScript(({ token, loginPayload }) => {
    for (const storage of [window.localStorage, window.sessionStorage]) {
      storage.setItem("suposTicket", token);
      storage.setItem("SUPOS_TICKET", token);
      storage.setItem("token", token);
      storage.setItem("ticket", token);
      storage.setItem("language", "zh_CN");
      storage.setItem("langu_code", "zh_CN");
      storage.setItem("locale", "zh-cn");
      storage.setItem("loginMsg", JSON.stringify(loginPayload));
    }
  }, { token: auth.ticket, loginPayload: auth.payload });
  return context;
}

async function visibleExactText(page, label) {
  const matches = page.getByText(label, { exact: true });
  const count = await matches.count();
  for (let index = 0; index < count; index += 1) {
    if (await matches.nth(index).isVisible().catch(() => false)) return matches.nth(index);
  }
  return null;
}

async function verifyPage(context, menuPayload, spec, report) {
  const menu = findNode(roots(menuPayload), spec.code);
  if (!menu || !menu.url) throw new Error(`${spec.code} is missing its menu URL`);

  const page = await context.newPage();
  const consoleErrors = [];
  const pageErrors = [];
  const networkErrors = [];
  page.on("console", (message) => {
    if (message.type() === "error") consoleErrors.push(message.text());
  });
  page.on("pageerror", (error) => pageErrors.push(error.message));
  page.on("response", (response) => {
    if (["document", "xhr", "fetch"].includes(response.request().resourceType()) && response.status() >= 400) {
      networkErrors.push({ status: response.status(), url: response.url() });
    }
  });

  const response = await page.goto(new URL(menu.url, `${baseUrl}/`).toString(), {
    waitUntil: "domcontentloaded",
    timeout: 90000,
  });
  await page.waitForLoadState("networkidle", { timeout: 10000 }).catch(() => {});
  await page.waitForTimeout(1200);

  const missingButtons = [];
  const buttonLocators = new Map();
  for (const label of spec.expectedButtons) {
    const locator = await visibleExactText(page, label);
    if (!locator) missingButtons.push(label);
    else buttonLocators.set(label, locator);
  }

  let safeClickResult = null;
  let safeClickError = null;
  if (spec.safeClick && buttonLocators.has(spec.safeClick)) {
    const nonGetRequests = [];
    const beforePageCount = context.pages().length;
    const recordNonGetRequest = (request) => {
      const method = request.method().toUpperCase();
      if (method !== "GET" && method !== "HEAD" && method !== "OPTIONS") {
        nonGetRequests.push({ method, url: request.url() });
      }
    };
    page.on("request", recordNonGetRequest);
    await buttonLocators.get(spec.safeClick).click({ timeout: 10000 });
    await page.waitForLoadState("networkidle", { timeout: 15000 }).catch(() => {});
    await page.waitForTimeout(2500);
    page.off("request", recordNonGetRequest);
    const safeClickBody = await page.locator("body").innerText().catch(() => "");
    const loadingVisible = /(?:加载中|正在处理，请稍候)/.test(safeClickBody);
    const rawI18nKeys = [
      ...new Set(
        safeClickBody.match(/(?:LIMSSample|LIMSBasic|Button|EditView)\.[A-Za-z0-9_.]+/g) || []
      ),
    ];
    const openedPages = context.pages().slice(beforePageCount);
    safeClickResult = {
      label: spec.safeClick,
      nonGetRequests,
      openedPageCount: openedPages.length,
      loadingVisible,
      rawI18nKeys,
    };
    if (loadingVisible) {
      safeClickError = "safe click remained in a loading state";
    } else if (rawI18nKeys.length) {
      safeClickError = `safe click exposed raw i18n keys: ${rawI18nKeys.join(", ")}`;
    }
    for (const openedPage of openedPages) {
      await openedPage.close().catch(() => {});
    }
  }

  const body = await page.locator("body").innerText().catch(() => "");
  const visibleError = body.split(/\r?\n/).find((line) => visibleErrorPattern.test(line)) || null;
  const result = {
    code: spec.code,
    name: menu.nameDisplay,
    url: menu.url,
    status: response && response.status(),
    expectedButtons: spec.expectedButtons,
    missingButtons,
    actionStatus: spec.expectedButtons.length ? "TESTED" : "NOT_APPLICABLE",
    safeClickResult,
    visibleError,
    consoleErrors,
    pageErrors,
    networkErrors,
    safeClickError,
  };
  report.pages.push(result);

  if (spec.expectedButtons.length) {
    const screenshot = path.join(outputDir, `${spec.code.replace(/[^A-Za-z0-9_.-]/g, "_")}.png`);
    await page.screenshot({ path: screenshot, fullPage: true });
    result.screenshot = screenshot;
  }
  await page.close();

  if (
    !response ||
    response.status() >= 400 ||
    missingButtons.length ||
    visibleError ||
    consoleErrors.length ||
    pageErrors.length ||
    networkErrors.length ||
    safeClickError
  ) {
    throw new Error(`Sample page failed: ${JSON.stringify(result)}`);
  }
}

async function main() {
  fs.mkdirSync(outputDir, { recursive: true });
  const report = {
    generatedAt: new Date().toISOString(),
    baseUrl,
    username,
    status: "RUNNING",
    summary: { totalPages: pageSpecs.length, actionPages: 16, queryOnlyPages: 8, expectedButtons: 29 },
    pages: [],
    issues: [],
  };

  let browser;
  let api;
  try {
    api = await request.newContext({ ignoreHTTPSErrors: true, timeout: 90000 });
    const auth = await login(api);
    report.loginStatus = auth.status;
    const menuPayload = await fetchMenuTree(api, auth.ticket);
    browser = await chromium.launch({ headless });
    const context = await createBrowserContext(browser, auth);
    for (const spec of pageSpecs) {
      try {
        await verifyPage(context, menuPayload, spec, report);
      } catch (error) {
        report.issues.push(error.stack || error.message);
      }
    }
    await context.close();
    report.status = report.issues.length ? "FAIL" : "PASS";
  } catch (error) {
    report.status = "FAIL";
    report.issues.push(error.stack || error.message);
    process.exitCode = 1;
  } finally {
    await api?.dispose().catch(() => {});
    await browser?.close().catch(() => {});
    const reportPath = path.join(outputDir, "acceptance.json");
    fs.writeFileSync(reportPath, `${JSON.stringify(report, null, 2)}\n`);
    console.log(JSON.stringify({ status: report.status, reportPath, outputDir }, null, 2));
  }
}

main();
