#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { chromium, request } = require("playwright");

const baseUrl = (process.env.ADP_BASE_URL || "http://10.11.100.17:18080").replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD || "123456";
const headless = process.env.ADP_HEADLESS !== "false";
const screenshotMode = process.env.ADP_SCREENSHOTS || "failures";
const outputDir =
  process.env.ADP_OUTPUT_DIR ||
  path.join("/tmp", `adp-business-page-smoke-${new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14)}`);

const visibleErrorPattern =
  /(系统错误|系统异常|发生未知异常|SQLGrammarException|could not extract ResultSet|404_NOT_FOUND|500 INTERNAL|\bHTTP\s*(404|500)\b|\b(404|500)\s+(Not Found|Internal Server Error)\b|Caused by:|\b[\w.]+Exception(?::|\s+at\b)|Invalid bound statement|relation .* does not exist|column .* does not exist)/i;

const targets = [
  { label: "BaseSet material layout", url: "/msService/BaseSet/material/material/materialLayout" },
  { label: "BaseSet unit group list", url: "/msService/BaseSet/unit/unitGroup/unitGroupList" },
  { label: "HierarchicalMod factory tree", url: "/msService/HierarchicalMod/factoryModel/factoryModel/factoryTreeList" },
  { label: "TagManagement tag list", url: "/msService/TagManagement/tagInfo/tag/tagList" },
  { label: "TeamInfo team list", url: "/msService/TeamInfo/team/team/teamList" },
  { label: "Qualify certificate layout", url: "/msService/Qualify/certificate/certificate/certifcateLayOut" },
  { label: "DocManage document list", url: "/msService/DocManage/document/docDocument/documentList" },
  { label: "LIMSBasic analy sample list", url: "/msService/LIMSBasic/analySample/analySample/analySampleList" },
  { label: "LIMSBasic sample type list", url: "/msService/LIMSBasic/sampleType/sampleType/sampleTypeList" },
  { label: "WOM make task list", url: "/msService/WOM/produceTask/produceTask/makeTaskList" },
  { label: "RM batch formula list", url: "/msService/RM/formula/formula/batchFormulaList" },
  { label: "craftGraph basic info list", url: "/msService/craftGraph/basicInfo/basicInfo/basicInfoList" },
  { label: "craftGraph button config list", url: "/msService/craftGraph/operationButton/buttonConfig/buttonConfList" },
  { label: "EAM base info layout", url: "/msService/EAM/baseInfo/baseInfo/baseInfoLayout" },
  { label: "OverEquipEffect running record layout", url: "/msService/OverEquipEffect/runRecord/runningRecord/runningRecordLayout" },
  { label: "TOOL tool layout", url: "/msService/TOOL/toolInfo/toolInfo/toolLayout" },
  { label: "Special finish check list", url: "/msService/Special/semFinishCheck/finishCheck/finishCheckList" },
  { label: "WTS work permit list", url: "/msService/WTS/workPermit/workPermit/workPermitList" },
  { label: "workAppointment work plan list", url: "/msService/workAppointment/workPlan/workTicketPlan/workPlanList" },
];

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true });
}

function safeName(value) {
  return String(value || "page")
    .replace(/[^a-zA-Z0-9._-]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .slice(0, 120);
}

function normalizeUrl(targetUrl) {
  return new URL(targetUrl, `${baseUrl}/`).toString();
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

function findVisibleError(bodyText) {
  return String(bodyText || "")
    .split(/\r?\n/)
    .map((line) => line.trim())
    .find((line) => line && visibleErrorPattern.test(line));
}

async function smokePage(context, target, index) {
  const page = await context.newPage();
  const networkErrors = [];
  const consoleErrors = [];
  const pageErrors = [];
  const requestFailures = [];

  page.on("response", async (response) => {
    const type = response.request().resourceType();
    const monitoredTypes = ["document", "xhr", "fetch", "script", "stylesheet"];
    if (!monitoredTypes.includes(type)) {
      return;
    }
    const contentType = response.headers()["content-type"] || "";
    const htmlAssetFallback =
      ["script", "stylesheet"].includes(type) && response.status() < 400 && /text\/html/i.test(contentType);
    if (response.status() < 400 && !htmlAssetFallback) {
      return;
    }
    let body = "";
    try {
      body = (await response.text()).slice(0, 300);
    } catch (_error) {
      body = "";
    }
    networkErrors.push({
      status: response.status(),
      method: response.request().method(),
      type,
      url: response.url(),
      contentType,
      body,
    });
  });

  page.on("console", (message) => {
    if (message.type() === "error") {
      consoleErrors.push(message.text());
    }
  });

  page.on("pageerror", (error) => {
    pageErrors.push(error.message);
  });

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

  const url = normalizeUrl(target.url);
  let navigation = null;
  let navigationError = null;
  try {
    navigation = await page.goto(url, { waitUntil: "domcontentloaded", timeout: 45000 });
    await page.waitForLoadState("networkidle", { timeout: 8000 }).catch(() => {});
    await page.waitForTimeout(1500);
  } catch (error) {
    navigationError = error.message;
  }

  const bodyText = await page.locator("body").innerText({ timeout: 5000 }).catch(() => "");
  const visibleError = findVisibleError(bodyText);
  const navigationStatus = navigation ? navigation.status() : null;
  const failed =
    Boolean(navigationError) ||
    (navigationStatus !== null && navigationStatus >= 400) ||
    networkErrors.length > 0 ||
    consoleErrors.length > 0 ||
    pageErrors.length > 0 ||
    requestFailures.length > 0 ||
    Boolean(visibleError);

  let screenshot = null;
  if (screenshotMode === "all" || (screenshotMode === "failures" && failed)) {
    screenshot = path.join(outputDir, `${String(index + 1).padStart(2, "0")}-${safeName(target.label)}.png`);
    await page.screenshot({ path: screenshot, fullPage: true }).catch(() => {
      screenshot = null;
    });
  }

  await page.close();

  return {
    label: target.label,
    url: target.url,
    resolvedUrl: url,
    ok: !failed,
    navigationStatus,
    navigationError,
    visibleError: visibleError || null,
    networkErrors,
    consoleErrors,
    pageErrors,
    requestFailures,
    screenshot,
  };
}

async function main() {
  ensureDir(outputDir);

  const api = await request.newContext({ ignoreHTTPSErrors: true });
  const ticket = await login(api);
  await api.dispose();

  const browser = await chromium.launch({ headless });
  const context = await browser.newContext({
    baseURL: baseUrl,
    ignoreHTTPSErrors: true,
    viewport: { width: 1440, height: 960 },
    extraHTTPHeaders: {
      Authorization: `Bearer ${ticket}`,
    },
  });
  await context.addCookies([
    { name: "suposTicket", value: ticket, url: baseUrl },
    { name: "SUPOS_TICKET", value: ticket, url: baseUrl },
  ]);
  await context.addInitScript((token) => {
    window.localStorage.setItem("suposTicket", token);
    window.localStorage.setItem("SUPOS_TICKET", token);
    window.localStorage.setItem("token", token);
    window.sessionStorage.setItem("suposTicket", token);
    window.sessionStorage.setItem("SUPOS_TICKET", token);
    window.sessionStorage.setItem("token", token);
  }, ticket);

  const results = [];
  for (const [index, target] of targets.entries()) {
    const result = await smokePage(context, target, index);
    results.push(result);
    console.log(`${result.ok ? "OK" : "FAIL"} ${index + 1}/${targets.length} ${target.label} ${target.url}`);
  }

  await browser.close();

  const failed = results.filter((result) => !result.ok);
  const report = {
    generatedAt: new Date().toISOString(),
    baseUrl,
    username,
    total: results.length,
    passed: results.length - failed.length,
    failed: failed.length,
    results,
  };
  const reportPath = path.join(outputDir, "business-page-smoke-results.json");
  fs.writeFileSync(reportPath, `${JSON.stringify(report, null, 2)}\n`);

  console.log(`SUMMARY total=${report.total} passed=${report.passed} failed=${report.failed}`);
  console.log(`REPORT ${reportPath}`);

  if (failed.length) {
    process.exitCode = 1;
  }
}

main().catch((error) => {
  console.error(error && error.stack ? error.stack : error);
  process.exit(1);
});
