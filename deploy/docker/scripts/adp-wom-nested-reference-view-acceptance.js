#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { chromium, request } = require("playwright");

const baseUrl = (process.env.ADP_BASE_URL || "http://10.11.100.17:18080").replace(
  /\/+$/,
  ""
);
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD;
const headless = process.env.ADP_HEADLESS !== "false";
const screenshotMode = process.env.ADP_SCREENSHOTS || "failures";
const nestedOnly = process.env.ADP_NESTED_ONLY === "true";
const nowToken = new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14);
const outputDir =
  process.env.ADP_OUTPUT_DIR ||
  path.join("/tmp", `adp-wom-nested-reference-${nowToken}`);

const visibleErrorPattern =
  /(系统错误|系统异常|发生未知异常|数据库操作异常|SQLGrammarException|could not extract ResultSet|Authorization Required|404_NOT_FOUND|500 INTERNAL)/i;
const rawI18nPattern =
  /\b(?:WOM|BaseSet|HierarchicalMod|Button)\.[A-Za-z0-9_.\u4e00-\u9fff]+/g;
const queryNotApplicable = new Set(["batchMatOrderView"]);

if (!password) throw new Error("ADP_PASSWORD is required");

const targets = [
  ["warehouseRefLayout", "/msService/BaseSet/warehouse/warehouse/warehouseRefLayout"],
  ["storeSetRefLayout", "/msService/BaseSet/warehouse/storeSet/storeSetRefLayout"],
  ["factoryNeedRef", "/msService/HierarchicalMod/factoryModel/factoryModel/factoryNeedRef"],
  ["batOrdPartRef", "/msService/WOM/batchMaterialNeed/makeBatOrdPart/batOrdPartRef"],
  ["batchMatOrderView", "/msService/WOM/batchMaterial/batchMateril/batchMatOrderView"],
  ["batchMaterilRefList", "/msService/WOM/batchMaterial/batchMateril/batchMaterilRefList"],
  ["recodRefForReject", "/msService/WOM/batchMaterial/batMaterilPart/recodRefForReject"],
  ["recodRefForReport", "/msService/WOM/batchMaterial/batMaterilPart/recodRefForReport"],
  ["outputRef", "/msService/WOM/outputMaterial/outputMaterial/outputRef"],
  ["preOrderRef", "/msService/WOM/prePraOrder/prePraOrder/preOrderRef"],
  ["prePraNeedPartRef", "/msService/WOM/prepareMaterialNeed/prePraNeedPart/prePraNeedPartRef"],
  ["prepareMaterialRef", "/msService/WOM/prepareMaterialNeed/prePareNeed/prepareMaterialRef"],
  ["instructionReference", "/msService/WOM/prepareMaterial/prepreMaterial/instructionReference"],
  ["procReportOutputRef", "/msService/WOM/procReport/outputDetail/outputRef"],
  ["procReportRef", "/msService/WOM/procReport/procReport/procReportRef"],
  ["procReportPutinRef", "/msService/WOM/procReport/putinDetail/putinRef"],
  ["activeExeLogRef", "/msService/WOM/produceTask/actiExelog/activeExeLogRef"],
  ["makeTaskExecuRef", "/msService/WOM/produceTask/prodTaskExelog/makeTaskExecuRef"],
  ["makeTaskRef", "/msService/WOM/produceTask/produceTask/makeTaskRef"],
  ["processExeLogRefer", "/msService/WOM/produceTask/processExelog/processExeLogRefer"],
  ["processExelogRef", "/msService/WOM/produceTask/processExelog/processExelogRef"],
  ["taskActiveExelogRef", "/msService/WOM/produceTask/taskActive/taskActiveExelogRef"],
  ["taskActiveRef", "/msService/WOM/produceTask/taskActive/taskActiveRef"],
  ["taskProcessExelogRef", "/msService/WOM/produceTask/taskProcess/taskProcessExelogRef"],
  ["taskRefForPrNeed", "/msService/WOM/produceTask/produceTask/taskRefForPrNeed"],
  ["putinRef", "/msService/WOM/putInMaterial/putInMaterial/putinRef"],
  ["batchRejectRef", "/msService/WOM/rejectMaterilal/rejectMaterial/batchRejectRef"],
  ["prePareRejectRef", "/msService/WOM/rejectMaterilal/rejectMaterial/prePareRejectRef"],
  ["remainMaterialRef", "/msService/WOM/remainMaterial/remainMaterial/remainMaterialRef"],
];

function ensureDir(directory) {
  fs.mkdirSync(directory, { recursive: true });
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

function monitorPage(page, getPhase = () => null) {
  const evidence = {
    networkErrors: [],
    consoleErrors: [],
    consoleErrorDetails: [],
    pageErrors: [],
    requestFailures: [],
  };
  const monitoredTypes = ["document", "xhr", "fetch", "script", "stylesheet"];

  page.on("response", async (response) => {
    const type = response.request().resourceType();
    if (!monitoredTypes.includes(type) || response.status() < 400) {
      return;
    }
    evidence.networkErrors.push({
      status: response.status(),
      method: response.request().method(),
      type,
      url: response.url(),
    });
  });
  page.on("console", (message) => {
    if (message.type() === "error") {
      evidence.consoleErrors.push(message.text());
      evidence.consoleErrorDetails.push({
        text: message.text(),
        location: message.location(),
        phase: getPhase(),
      });
    }
  });
  page.on("pageerror", (error) => evidence.pageErrors.push(error.message));
  page.on("requestfailed", (requestItem) => {
    if (!monitoredTypes.includes(requestItem.resourceType())) {
      return;
    }
    evidence.requestFailures.push({
      method: requestItem.method(),
      type: requestItem.resourceType(),
      url: requestItem.url(),
      failure: requestItem.failure() && requestItem.failure().errorText,
    });
  });
  return evidence;
}

async function collectRawI18nContexts(page) {
  const contexts = [];
  for (const frame of page.frames()) {
    const frameContexts = await frame
      .evaluate(() => {
        const prefixes = ["WOM.", "BaseSet.", "HierarchicalMod.", "Button."];
        const walker = document.createTreeWalker(
          document.body,
          NodeFilter.SHOW_TEXT,
          null,
          false
        );
        const matches = [];
        let node;
        while ((node = walker.nextNode()) && matches.length < 20) {
          const text = (node.nodeValue || "").trim();
          if (!prefixes.some((prefix) => text.includes(prefix))) continue;
          const international = window.ReactAPI && window.ReactAPI.international;
          matches.push({
            text,
            parent: node.parentElement ? node.parentElement.outerHTML.slice(0, 500) : "",
            resourceValue:
              window.InternationalResource && window.InternationalResource[text],
            resolvedValue:
              international && typeof international.getText === "function"
                ? international.getText(text)
                : null,
          });
        }
        return matches;
      })
      .catch(() => []);
    frameContexts.forEach((item) =>
      contexts.push({ frameUrl: frame.url(), ...item })
    );
  }
  return contexts;
}

async function exerciseQueryButton(page) {
  const queryButtons = page.getByRole("button", { name: /^\s*查\s*询\s*$/ });
  const buttonCount = await queryButtons.count();
  if (buttonCount === 0) {
    return { buttonCount, ok: false, error: "查询按钮缺失" };
  }

  const responsePromise = page
    .waitForResponse(
      (response) => {
        const requestItem = response.request();
        return (
          ["xhr", "fetch"].includes(requestItem.resourceType()) &&
          requestItem.method() !== "OPTIONS" &&
          /-query(?:\?|$)/.test(response.url())
        );
      },
      { timeout: 15000 }
    )
    .catch(() => null);
  await queryButtons.first().click();
  const response = await responsePromise;
  await page.waitForTimeout(600);
  if (!response) {
    return { buttonCount, ok: false, error: "点击查询后未捕获到查询请求" };
  }

  const responseText = await response.text().catch(() => "");
  let responseJson = null;
  try {
    responseJson = JSON.parse(responseText);
  } catch (_error) {}
  const businessFailure =
    responseJson &&
    (responseJson.success === false || Number(responseJson.code) >= 400);
  return {
    buttonCount,
    ok: response.status() === 200 && !businessFailure,
    method: response.request().method(),
    url: response.url(),
    status: response.status(),
    businessCode: responseJson && responseJson.code,
    businessSuccess: responseJson && responseJson.success,
    responseSnippet: businessFailure ? responseText.slice(0, 500) : "",
  };
}

async function smokeReferencePage(context, target, index) {
  const [label, route] = target;
  const page = await context.newPage();
  const evidence = monitorPage(page);
  let navigationStatus = null;
  let navigationError = null;
  try {
    const response = await page.goto(route, {
      waitUntil: "domcontentloaded",
      timeout: 45000,
    });
    navigationStatus = response ? response.status() : null;
    await page.waitForLoadState("networkidle", { timeout: 8000 }).catch(() => {});
    await page.waitForTimeout(900);
  } catch (error) {
    navigationError = error.message;
  }

  const query = queryNotApplicable.has(label)
    ? { buttonCount: 0, ok: true, status: "NOT_APPLICABLE" }
    : navigationStatus === 200 && !navigationError
      ? await exerciseQueryButton(page)
      : { buttonCount: 0, ok: false, error: "页面导航失败，未执行查询" };
  const bodyText = await page.locator("body").innerText({ timeout: 5000 }).catch(() => "");
  const rawI18nKeys = [...new Set(bodyText.match(rawI18nPattern) || [])];
  const rawI18nContexts = rawI18nKeys.length
    ? await collectRawI18nContexts(page)
    : [];
  const visibleError = bodyText.match(visibleErrorPattern);
  const pageTitle = await page.title().catch(() => "");
  const layoutSignals = {
    inputCount: await page.locator("input").count(),
    buttonCount: await page.locator("button").count(),
    bodyLength: bodyText.trim().length,
  };
  const ok =
    navigationStatus === 200 &&
    !navigationError &&
    query.ok &&
    layoutSignals.bodyLength > 20 &&
    !visibleError &&
    rawI18nKeys.length === 0 &&
    evidence.networkErrors.length === 0 &&
    evidence.consoleErrors.length === 0 &&
    evidence.pageErrors.length === 0 &&
    evidence.requestFailures.length === 0;

  let screenshot = null;
  if (screenshotMode === "all" || (screenshotMode === "failures" && !ok)) {
    screenshot = path.join(outputDir, `${String(index + 1).padStart(2, "0")}-${label}.png`);
    await page.screenshot({ path: screenshot, fullPage: true }).catch(() => {
      screenshot = null;
    });
  }
  await page.close();

  return {
    label,
    route,
    ok,
    navigationStatus,
    navigationError,
    title: pageTitle,
    query,
    visibleError: visibleError ? visibleError[0] : null,
    rawI18nKeys,
    rawI18nContexts,
    layoutSignals,
    screenshot,
    ...evidence,
  };
}

async function verifyNestedDialog(context) {
  const route = "/msService/WOM/rejectMaterilal/rejectMaterial/batchRejectEdit";
  const nestedRoute = "/msService/WOM/batchMaterial/batMaterilPart/recodRefForReject";
  const queryPath = `${nestedRoute}-query`;
  const page = await context.newPage();
  let phase = "navigation";
  const evidence = monitorPage(page, () => phase);
  const response = await page.goto(route, {
    waitUntil: "domcontentloaded",
    timeout: 45000,
  });
  await page.waitForLoadState("networkidle", { timeout: 8000 }).catch(() => {});
  await page.waitForTimeout(1200);

  const trigger = page.getByText("参照配料记录", { exact: true });
  const triggerCount = await trigger.count();
  if (triggerCount !== 1) {
    throw new Error(`Expected one 参照配料记录 button, found ${triggerCount}`);
  }
  phase = "open-dialog";
  await trigger.click();
  await page.waitForTimeout(1600);

  const nestedFrame = page.frames().find((frame) => frame.url().includes(nestedRoute));
  if (!nestedFrame) {
    throw new Error("Nested 配料记录参照 frame did not open");
  }
  await nestedFrame.waitForLoadState("networkidle", { timeout: 10000 }).catch(() => {});
  const nestedBody = await nestedFrame.locator("body").innerText();
  const selectCount = await page.getByText("选择", { exact: true }).count();
  const cancelCount = await page.getByText("取消", { exact: true }).count();
  const rawButtonKeys = await page.getByText(/Button\.text\./).count();
  const rawTitleKeys = await page.getByText(/WOM\.viewtitle\./).count();
  const queryButton = nestedFrame.getByText(/查\s*询/, { exact: true });
  await queryButton.waitFor({ state: "visible", timeout: 10000 });
  if ((await queryButton.count()) !== 1) {
    throw new Error("Nested 配料记录参照 query button is missing");
  }
  const queryResponsePromise = page.waitForResponse(
    (item) => item.url().includes(queryPath),
    { timeout: 15000 }
  );
  phase = "query-dialog";
  await queryButton.click();
  const queryResponse = await queryResponsePromise;
  await nestedFrame.waitForLoadState("networkidle", { timeout: 10000 }).catch(() => {});
  await page.waitForTimeout(600);

  const screenshot = path.join(outputDir, "nested-batch-reject-reference-open.png");
  await page.screenshot({ path: screenshot, fullPage: true });

  phase = "cancel-dialog";
  await page.getByText("取消", { exact: true }).click();
  await page.waitForTimeout(400);
  const titleAfterCancel = await page.getByText("配料记录参照", { exact: true }).count();

  phase = "reopen-dialog";
  await trigger.click();
  await page.waitForTimeout(900);
  const reopenedFrame = page
    .frames()
    .find((frame) => frame.url().includes(nestedRoute));
  if (!reopenedFrame) {
    throw new Error("Nested 配料记录参照 frame did not reopen");
  }
  await reopenedFrame.waitForLoadState("networkidle", { timeout: 10000 }).catch(() => {});
  const repeatedOpenCount = await page.getByText("配料记录参照", { exact: true }).count();
  phase = "cancel-reopened-dialog";
  await page.getByText("取消", { exact: true }).click();

  const rawNestedKeys = [...new Set(nestedBody.match(rawI18nPattern) || [])];
  const visibleNestedError = nestedBody.match(visibleErrorPattern);
  const ok =
    response &&
    response.status() === 200 &&
    nestedBody.trim().length > 40 &&
    nestedBody.includes("配料指令单") &&
    selectCount === 1 &&
    cancelCount === 1 &&
    rawButtonKeys === 0 &&
    rawTitleKeys === 0 &&
    rawNestedKeys.length === 0 &&
    !visibleNestedError &&
    queryResponse.status() === 200 &&
    titleAfterCancel === 0 &&
    repeatedOpenCount === 1 &&
    evidence.networkErrors.length === 0 &&
    evidence.consoleErrors.length === 0 &&
    evidence.pageErrors.length === 0 &&
    evidence.requestFailures.length === 0;

  await page.close();
  return {
    label: "batchRejectEdit -> 参照配料记录",
    route,
    nestedRoute,
    ok,
    navigationStatus: response ? response.status() : null,
    query: { method: queryResponse.request().method(), url: queryResponse.url(), status: queryResponse.status() },
    triggerCount,
    selectCount,
    cancelCount,
    rawButtonKeys,
    rawTitleKeys,
    rawNestedKeys,
    visibleNestedError: visibleNestedError ? visibleNestedError[0] : null,
    titleAfterCancel,
    repeatedOpenCount,
    screenshot,
    ...evidence,
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
    viewport: { width: 1600, height: 1000 },
    extraHTTPHeaders: { Authorization: `Bearer ${ticket}` },
  });
  await context.addCookies([
    { name: "suposTicket", value: ticket, url: baseUrl },
    { name: "SUPOS_TICKET", value: ticket, url: baseUrl },
  ]);
  await context.addInitScript((token) => {
    ["suposTicket", "SUPOS_TICKET", "token", "ticket"].forEach((key) => {
      window.localStorage.setItem(key, token);
      window.sessionStorage.setItem(key, token);
    });
  }, ticket);

  const directPages = [];
  if (!nestedOnly) {
    for (const [index, target] of targets.entries()) {
      const result = await smokeReferencePage(context, target, index);
      directPages.push(result);
      console.log(
        `${result.ok ? "OK" : "FAIL"} ${index + 1}/${targets.length} ${result.label}`
      );
    }
  }

  let nestedDialog;
  try {
    nestedDialog = await verifyNestedDialog(context);
    console.log(`${nestedDialog.ok ? "OK" : "FAIL"} nested dialog interaction`);
  } catch (error) {
    nestedDialog = {
      label: "batchRejectEdit -> 参照配料记录",
      ok: false,
      error: error.stack || error.message,
    };
    console.log("FAIL nested dialog interaction");
  }

  await browser.close();

  const failedDirectPages = directPages.filter((item) => !item.ok);
  const report = {
    generatedAt: new Date().toISOString(),
    baseUrl,
    username,
    summary: {
      directPages: directPages.length,
      directPass: directPages.length - failedDirectPages.length,
      directFail: failedDirectPages.length,
      nestedDialog: nestedDialog.ok ? "PASS" : "FAIL",
    },
    directPages,
    nestedDialog,
  };
  const outputPath = path.join(outputDir, "wom-nested-reference-results.json");
  fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`);
  console.log(`REPORT ${outputPath}`);

  if (failedDirectPages.length || !nestedDialog.ok) {
    process.exitCode = 1;
  }
}

main().catch((error) => {
  console.error(error && error.stack ? error.stack : error);
  process.exit(1);
});
