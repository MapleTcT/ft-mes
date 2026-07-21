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
const headless = process.env.ADP_HEADLESS !== "false";
const dbSshTarget = process.env.ADP_DB_SSH_TARGET || "v6@10.11.100.17";
const dbSshPassword = process.env.ADP_DB_SSH_PASSWORD || "";
const dbContainer = process.env.ADP_DB_CONTAINER || "adp-mes-newbase-postgres-1";
const dbName = process.env.ADP_DB_NAME || "adp";
const dbUser = process.env.ADP_DB_USER || "adp";
const nowToken = new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14);
const marker = process.env.ADP_E2E_MARKER || `ADP_E2E_${nowToken}_PUBLIC_PRODUCE_RETIRED`;
const outputPath =
  process.env.ADP_WOM_PUBLIC_PRODUCE_TASK_CREATED_RETIREMENT_OUTPUT ||
  process.env.ADP_WOM_PUBLIC_PRODUCE_TASK_CREATED_NOOP_OUTPUT ||
  "metadata/wom-public-produce-task-created-retirement-acceptance.json";
const endpoint = "/msService/public/WOM/produceTask/produceTask/produceTaskCreated";

function ensureDir(filePath) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
}

function repoCommit() {
  try {
    return execFileSync("git", ["rev-parse", "HEAD"], { encoding: "utf8" }).trim();
  } catch (_error) {
    return "UNKNOWN";
  }
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

function verificationSql() {
  const likeMarker = `%${marker}%`;
  return `
SELECT count(*)::text
FROM public.wom_produce_tasks
WHERE coalesce(table_no::text, '') LIKE ${sqlLiteral(likeMarker)}
   OR coalesce(produce_batch_num::text, '') LIKE ${sqlLiteral(likeMarker)}
   OR coalesce(day_plan_ids::text, '') LIKE ${sqlLiteral(likeMarker)};
`;
}

function markerCount() {
  const raw = runSql(verificationSql());
  return Number(raw || "0");
}

function buildProbePayload() {
  return [
    {
      userName: "admin",
      companyCode: "default",
      orderInfos: {
        sourceIds: `${marker}_SOURCE`,
        sourceRatio: "1",
        sourceType: "1",
        creatStaffCode: "admin",
        prodCode: `${marker}_PRODUCT`,
        planNum: "1",
        batchcode: `${marker}_BATCH`,
        packageInfos: [],
      },
    },
  ];
}

async function readResponse(response) {
  const text = await response.text();
  let json = null;
  try {
    json = JSON.parse(text);
  } catch (_error) {
    json = null;
  }
  return { text, json };
}

function findTicket(payload) {
  const values = [
    payload && payload.ticket,
    payload && payload.access_token,
    payload && payload.token,
    payload && payload.data && payload.data.ticket,
    payload && payload.data && payload.data.access_token,
    payload && payload.data && payload.data.token,
  ];
  return values.find((value) => typeof value === "string" && value.length > 20);
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
      headers: { Accept: "application/json", "Content-Type": "application/json;charset=UTF-8" },
    });
    const parsed = await readResponse(response);
    const ticket = response.ok() ? findTicket(parsed.json) : null;
    if (ticket) return { ticket, status: response.status() };
    errors.push({ status: response.status(), body: parsed.text.slice(0, 400) });
  }
  throw new Error(`Login failed for ${username}: ${JSON.stringify(errors)}`);
}

function attachBrowserEvidence(page, evidence, label) {
  page.on("console", (message) => {
    if (message.type() === "error") {
      evidence.consoleErrors.push({ page: label, text: message.text() });
    }
  });
  page.on("pageerror", (error) => {
    evidence.pageErrors.push({ page: label, message: error.message });
  });
  page.on("requestfailed", (item) => {
    evidence.requestFailures.push({
      page: label,
      method: item.method(),
      url: item.url(),
      failure: item.failure(),
    });
  });
  page.on("response", (response) => {
    if (response.status() >= 400) {
      evidence.networkErrors.push({
        page: label,
        method: response.request().method(),
        url: response.url(),
        status: response.status(),
      });
    }
  });
}

async function verifyReplacementBrowserPath(api, report) {
  const loginResult = await login(api);
  const browser = await chromium.launch({ headless });
  try {
    const context = await browser.newContext({
      baseURL: browserBaseUrl,
      ignoreHTTPSErrors: true,
      viewport: { width: 1600, height: 1000 },
      extraHTTPHeaders: { Authorization: `Bearer ${loginResult.ticket}` },
    });
    await context.addCookies([
      { name: "suposTicket", value: loginResult.ticket, url: browserBaseUrl },
      { name: "SUPOS_TICKET", value: loginResult.ticket, url: browserBaseUrl },
    ]);

    const listPage = await context.newPage();
    attachBrowserEvidence(listPage, report.browser, "makeTaskList");
    const navigation = await listPage.goto(
      `${browserBaseUrl}/msService/WOM/produceTask/produceTask/makeTaskList`,
      { waitUntil: "domcontentloaded", timeout: 60000 }
    );
    await listPage.waitForTimeout(2500);
    const manualButton = listPage.locator("#btn-manualCreateTask");
    await manualButton.waitFor({ state: "visible", timeout: 30000 });
    report.browser.listNavigationStatus = navigation && navigation.status();
    report.browser.manualEntryButtonVisible = await manualButton.isVisible();

    const popupPromise = listPage.waitForEvent("popup");
    await manualButton.click();
    const entryPage = await popupPromise;
    attachBrowserEvidence(entryPage, report.browser, "manualEntry");
    await entryPage.waitForLoadState("domcontentloaded");
    await entryPage.locator("#production-option option").nth(1).waitFor({ state: "attached", timeout: 30000 });
    report.browser.manualEntryUrl = entryPage.url();
    report.browser.manualEntryTitle = await entryPage.title();
    report.browser.manualEntryOptionCount = await entryPage.locator("#production-option option").count();
    await context.close();
  } finally {
    await browser.close();
  }
}

function classify(report) {
  const responseCode = report.responseJson && report.responseJson.code;
  const responseMessage = report.responseJson && report.responseJson.message;
  const countUnchanged = report.beforeCount === report.afterCount;
  const browserPassed =
    report.browser &&
    report.browser.listNavigationStatus === 200 &&
    report.browser.manualEntryButtonVisible === true &&
    String(report.browser.manualEntryUrl || "").includes("/manual-entry/page") &&
    report.browser.manualEntryOptionCount > 1 &&
    report.browser.consoleErrors.length === 0 &&
    report.browser.pageErrors.length === 0 &&
    report.browser.requestFailures.length === 0 &&
    report.browser.networkErrors.length === 0;
  const falseSuccess = report.httpStatus === 200 && responseCode === 200 && countUnchanged;
  if (falseSuccess) {
    return {
      status: "FAIL_FALSE_SUCCESS_REGRESSION",
      conclusion:
        "Deprecated endpoint regressed to HTTP 200/code=200 without persistence.",
      exitCode: 2,
    };
  }
  if (report.afterCount > report.beforeCount) {
    return {
      status: "UNEXPECTED_PERSISTENCE_REQUIRES_RECLASSIFICATION",
      conclusion:
        "Deprecated endpoint inserted marker data and violated the retirement contract.",
      exitCode: 2,
    };
  }
  if (
    report.httpStatus === 200 &&
    responseCode === 400 &&
    countUnchanged &&
    browserPassed &&
    String(responseMessage || "").includes("已废弃")
  ) {
    return {
      status: "PASS_DEPRECATED_EXPLICIT_REJECTION_NO_PERSISTENCE",
      conclusion:
        "Deprecated endpoint returned the documented business rejection and PostgreSQL marker count remained unchanged.",
      exitCode: 0,
    };
  }
  return {
    status: "FAIL_RETIREMENT_CONTRACT_MISMATCH",
    conclusion:
      `Expected HTTP 200/code=400 with an explicit 已废弃 message, no persistence and a clean replacement-page smoke; got HTTP ${report.httpStatus}, code=${responseCode}, message=${responseMessage || ""}.`,
    exitCode: 1,
  };
}

async function main() {
  ensureDir(outputPath);
  const api = await request.newContext({ ignoreHTTPSErrors: true });
  const report = {
    schemaVersion: 2,
    reportKind: "wom-public-produce-task-created-retirement-acceptance",
    generatedAt: new Date().toISOString(),
    repoCommit: repoCommit(),
    database: "PostgreSQL",
    baseUrl,
    endpoint: `${baseUrl}${endpoint}`,
    marker,
    method: "POST",
    contractDecision: "DEPRECATED",
    requiresPersistence: false,
    replacementPaths: [
      "POST /msService/WOM/produceTask/produceTask/produceTaskCreated2 (authenticated daily-plan integration)",
      "/msService/WOM/produceTask/produceTask/makeTaskList -> 新建指令单 (authenticated manual entry)",
    ],
    frontendApplicability:
      "The retired integration endpoint has no direct UI; the authenticated makeTaskList -> 新建指令单 replacement is browser-smoked without creating data.",
    browser: {
      baseUrl: browserBaseUrl,
      listNavigationStatus: null,
      manualEntryButtonVisible: false,
      manualEntryUrl: "",
      manualEntryTitle: "",
      manualEntryOptionCount: null,
      consoleErrors: [],
      pageErrors: [],
      requestFailures: [],
      networkErrors: [],
    },
    requestShape: "legacy daily-plan-like marker payload used only to prove explicit rejection",
    payload: buildProbePayload(),
    verificationSql: verificationSql().trim(),
    beforeCount: null,
    httpStatus: null,
    responseText: "",
    responseJson: null,
    afterCount: null,
    status: "RUNNING",
    conclusion: "",
  };

  try {
    report.beforeCount = markerCount();
    const response = await api.post(report.endpoint, {
      data: report.payload,
      headers: {
        Accept: "application/json, text/plain, */*",
        "Content-Type": "application/json;charset=UTF-8",
      },
      timeout: Number(process.env.ADP_API_TIMEOUT_MS || 30000),
    });
    report.httpStatus = response.status();
    const parsed = await readResponse(response);
    report.responseText = parsed.text.slice(0, 2000);
    report.responseJson = parsed.json;
    report.afterCount = markerCount();
    await verifyReplacementBrowserPath(api, report);
    const classification = classify(report);
    report.status = classification.status;
    report.conclusion = classification.conclusion;
    fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`);
    if (classification.exitCode !== 0) {
      console.error(`${report.status}: ${report.conclusion}`);
      console.error(`Report: ${outputPath}`);
      process.exit(classification.exitCode);
    }
    console.log(`${report.status}: ${outputPath}`);
  } catch (error) {
    report.status = "ERROR";
    report.conclusion = error && error.stack ? error.stack : String(error);
    fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`);
    throw error;
  } finally {
    await api.dispose();
  }
}

main();
