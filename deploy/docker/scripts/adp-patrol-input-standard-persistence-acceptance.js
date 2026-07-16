#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { execFileSync } = require("child_process");
const { chromium, request } = require("playwright");

const now = new Date();
const stamp = now.toISOString().replace(/[-:T.Z]/g, "").slice(0, 14);
const marker = process.env.ADP_E2E_MARKER || `ADP_E2E_${stamp}_PATROL_INPUT`;
const baseUrl = (process.env.ADP_BASE_URL || "http://10.11.100.17:18080").replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD || "123456";
const headless = process.env.ADP_HEADLESS !== "false";
const pageTimeoutMs = Number(process.env.ADP_PAGE_TIMEOUT_MS || 180000);
const outputDir = process.env.ADP_OUTPUT_DIR || path.join("/tmp", `adp-patrol-input-standard-${stamp}`);
const outputPath =
  process.env.ADP_PATROL_INPUT_STANDARD_PERSISTENCE_OUTPUT ||
  path.join(outputDir, "patrol-input-standard-persistence.json");
const dbSshTarget = process.env.ADP_DB_SSH_TARGET || "v6@10.11.100.17";
const dbContainer = process.env.ADP_DB_CONTAINER || "adp-mes-newbase-postgres-1";
const dbName = process.env.ADP_DB_NAME || "adp";
const dbUser = process.env.ADP_DB_USER || "adp";

const listRoute = "/msService/PATROL/inputStandard/inputStandard/inputStanList";
const submitPath = "/msService/PATROL/inputStandard/inputStandard/inputStanEdit/submit";
const statePath = "/msService/PATROL/publicItem/publicItem/updateItemState";
const relationPath = "/msService/PATROL/inputStandard/inputStandard/checkRelationWorkItem";
const deletePath = "/msService/PATROL/inputStandard/inputStandard/deleteInputStandard";
const bodyAssetPath =
  "/greenDill/static/PATROL/inputStandard/inputStandard/inputStanEdit/body-es5.js";
const updatedName = `${marker}_UPDATED`;
const updatedRemark = `${marker}_UPDATED_REMARK`;
const visibleErrorPattern =
  /(数据库操作异常|系统错误|系统异常|SQLGrammarException|could not extract ResultSet|column .* does not exist|relation .* does not exist|500 INTERNAL|自定义代码错误|\b[\w.]+Exception(?::|\s+at\b))/i;

let activeBrowser = null;
const result = {
  generatedAt: now.toISOString(),
  repoCommit: process.env.ADP_REPO_COMMIT || "WORKTREE",
  database: "PostgreSQL",
  marker,
  environment: {
    baseUrl,
    route: listRoute,
    database: "PostgreSQL",
    table: "public.mp_input_standards",
    backendEntry:
      "PATROLInputStandardController/PATROLPublicItemController -> PATROLInputStandardServiceImpl/PATROLPublicItemServiceImpl -> JPA/native SQL",
  },
  summary: { status: "RUNNING", assertions: 0, pass: 0, fail: 0 },
  assertions: [],
  operations: [],
  evidence: {
    requests: [],
    responses: [],
    consoleErrors: [],
    consoleWarnings: [],
    pageErrors: [],
    requestFailures: [],
    screenshots: [],
  },
  error: null,
};

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true });
}

function shellQuote(value) {
  return `'${String(value).replace(/'/g, "'\\''")}'`;
}

function sqlLiteral(value) {
  return `'${String(value).replace(/'/g, "''")}'`;
}

function runSql(sql) {
  const remoteCommand = [
    "docker",
    "exec",
    "-i",
    dbContainer,
    "psql",
    "-U",
    dbUser,
    "-d",
    dbName,
    "-v",
    "ON_ERROR_STOP=1",
    "-At",
  ]
    .map(shellQuote)
    .join(" ");
  return execFileSync(
    "ssh",
    [
      "-o",
      "BatchMode=yes",
      "-o",
      "ConnectTimeout=10",
      "-o",
      "ServerAliveInterval=15",
      "-o",
      "ServerAliveCountMax=2",
      dbSshTarget,
      remoteCommand,
    ],
    { input: sql, encoding: "utf8", stdio: ["pipe", "pipe", "pipe"] }
  ).trim();
}

function queryJson(sql) {
  const output = runSql(sql);
  const lines = output.split(/\r?\n/).filter(Boolean);
  return lines.length ? JSON.parse(lines[lines.length - 1]) : null;
}

function markerSql() {
  return `
select json_build_object(
  'id', id,
  'version', version,
  'code', code,
  'name', name,
  'valType', val_type,
  'editType', edit_type,
  'state', state,
  'valid', valid,
  'remark', remark,
  'cid', cid
) from public.mp_input_standards where code=${sqlLiteral(marker)} order by id desc limit 1;
`;
}

function addAssertion(name, pass, detail) {
  const item = { name, pass: Boolean(pass), detail };
  result.assertions.push(item);
  result.summary.assertions += 1;
  if (item.pass) {
    result.summary.pass += 1;
  } else {
    result.summary.fail += 1;
  }
  return item.pass;
}

function requireAssertion(name, pass, detail) {
  if (!addAssertion(name, pass, detail)) {
    throw new Error(`${name}: ${typeof detail === "string" ? detail : JSON.stringify(detail)}`);
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

async function readResponse(response) {
  const text = await response.text();
  let json = null;
  try {
    json = JSON.parse(text);
  } catch (_error) {
    json = null;
  }
  return { status: response.status(), url: response.url(), text: text.slice(0, 4000), json };
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
      headers: { Accept: "application/json", "Content-Type": "application/json;charset=UTF-8" },
    });
    const parsed = await readResponse(response);
    const ticket = response.ok() ? findTicket(parsed.json) : null;
    if (ticket) {
      return ticket;
    }
    failures.push({ status: parsed.status, body: parsed.text.slice(0, 500) });
  }
  throw new Error(`Login failed: ${JSON.stringify(failures)}`);
}

async function waitForEditFrame(page) {
  const iframe = page.locator('iframe[src*="inputStanEdit"]').last();
  await iframe.waitFor({ state: "attached", timeout: 30000 });
  const handle = await iframe.elementHandle();
  const frame = handle && (await handle.contentFrame());
  if (!frame) {
    throw new Error("Input-standard edit iframe did not open");
  }
  await frame.waitForLoadState("domcontentloaded", { timeout: 30000 });
  await frame.waitForFunction(() => (document.body.innerText || "").includes("值类型"), null, {
    timeout: 30000,
  });
  return frame;
}

async function selectOption(frame, index, label) {
  const select = frame.locator(".ant-select").nth(index);
  await select.click();
  const option = frame.locator(".ant-select-dropdown-menu-item", { hasText: label }).last();
  await option.waitFor({ state: "visible", timeout: 10000 });
  await option.click();
}

async function clickSave(page) {
  const button = page.locator("button:visible").filter({ hasText: /保\s*存/ }).last();
  await button.waitFor({ state: "visible", timeout: 10000 });
  await button.click();
}

async function markerRow(page) {
  const cell = page.getByText(marker, { exact: true }).first();
  await cell.waitFor({ state: "visible", timeout: 30000 });
  return cell.locator('xpath=ancestor::div[contains(@class,"sup-datagrid-row")]').first();
}

async function captureScreenshot(page, fileName) {
  const filePath = path.join(outputDir, fileName);
  await page.screenshot({ path: filePath, fullPage: true });
  result.evidence.screenshots.push(filePath);
}

function addOperation(operation, api, method, payload, response, verificationSql, dbResult, status) {
  result.operations.push({
    operation,
    ui: `${listRoute} -> ${operation}`,
    api,
    method,
    payload,
    response,
    verificationSql: verificationSql.trim(),
    dbResult,
    status,
  });
}

async function waitForConfirmAndAccept(page) {
  const candidates = [/^\s*确\s*定\s*$/, /^\s*确\s*认\s*$/, /^\s*是\s*$/];
  for (const pattern of candidates) {
    const button = page.locator("button:visible").filter({ hasText: pattern }).last();
    if (await button.count()) {
      await button.click();
      return;
    }
  }
  throw new Error("Delete confirmation button was not found");
}

async function main() {
  ensureDir(outputDir);
  ensureDir(path.dirname(outputPath));

  const preexisting = queryJson(
    `select json_build_object('count', count(*)) from public.mp_input_standards where code=${sqlLiteral(marker)};`
  );
  requireAssertion("marker is unique before test", Number(preexisting.count) === 0, preexisting);

  const api = await request.newContext({ ignoreHTTPSErrors: true });
  const ticket = await login(api);
  const assetResponse = await api.get(`${baseUrl}${bodyAssetPath}`);
  const asset = await readResponse(assetResponse);
  requireAssertion(
    "input-standard body asset is restored",
    asset.status === 200 && asset.text.includes("function editOrValueChange") && !/^void 0;?\s*$/.test(asset.text),
    { status: asset.status, bytes: asset.text.length, head: asset.text.slice(0, 80) }
  );
  await api.dispose();

  const browser = await chromium.launch({ headless });
  activeBrowser = browser;
  const context = await browser.newContext({
    baseURL: baseUrl,
    ignoreHTTPSErrors: true,
    viewport: { width: 1920, height: 1080 },
    extraHTTPHeaders: { Authorization: `Bearer ${ticket}` },
  });
  await context.addCookies([
    { name: "suposTicket", value: ticket, url: baseUrl },
    { name: "SUPOS_TICKET", value: ticket, url: baseUrl },
  ]);
  await context.addInitScript((token) => {
    ["suposTicket", "SUPOS_TICKET", "token"].forEach((key) => {
      window.localStorage.setItem(key, token);
      window.sessionStorage.setItem(key, token);
    });
  }, ticket);

  const page = await context.newPage();
  page.setDefaultTimeout(pageTimeoutMs);
  page.setDefaultNavigationTimeout(pageTimeoutMs);
  page.on("console", (message) => {
    const item = { text: message.text().slice(0, 1000), location: message.location() };
    if (message.type() === "error") {
      result.evidence.consoleErrors.push(item);
    } else if (message.type() === "warning") {
      result.evidence.consoleWarnings.push(item);
    }
  });
  page.on("pageerror", (error) => result.evidence.pageErrors.push(error.message.slice(0, 1000)));
  page.on("requestfailed", (requestItem) => {
    if (requestItem.url().includes("/PATROL/")) {
      result.evidence.requestFailures.push({
        method: requestItem.method(),
        url: requestItem.url(),
        failure: requestItem.failure() && requestItem.failure().errorText,
      });
    }
  });
  page.on("request", (requestItem) => {
    if (requestItem.url().includes("/PATROL/")) {
      result.evidence.requests.push({
        method: requestItem.method(),
        url: requestItem.url(),
        postData: requestItem.postData() ? requestItem.postData().slice(0, 4000) : null,
      });
    }
  });
  page.on("response", (response) => {
    if (response.url().includes("/PATROL/")) {
      result.evidence.responses.push({
        method: response.request().method(),
        url: response.url(),
        status: response.status(),
      });
    }
  });

  const navigation = await page.goto(`${baseUrl}${listRoute}`, {
    waitUntil: "domcontentloaded",
    timeout: pageTimeoutMs,
  });
  await page.waitForFunction(() => (document.body.innerText || "").includes("录入标准"), null, {
    timeout: 60000,
  }).catch(() => {});
  const initialBody = await page.locator("body").innerText();
  requireAssertion(
    "input-standard list page loads",
    navigation && navigation.status() === 200 && initialBody.includes("标准编码") && !visibleErrorPattern.test(initialBody),
    { status: navigation && navigation.status(), body: initialBody.slice(0, 500) }
  );

  const addResponsePromise = page.waitForResponse(
    (response) => response.url().includes(submitPath) && response.request().method() === "POST",
    { timeout: pageTimeoutMs }
  );
  await page.locator("#btn-add").click();
  const addFrame = await waitForEditFrame(page);
  requireAssertion(
    "editOrValueChange is callable in add dialog",
    (await addFrame.evaluate(() => typeof window.editOrValueChange)) === "function",
    await addFrame.evaluate(() => typeof window.editOrValueChange)
  );
  await addFrame.locator('[id="edit_form_inputStandard.code"]').fill(marker);
  await addFrame.locator('[id="edit_form_inputStandard.name"]').fill(`${marker}_NAME`);
  await selectOption(addFrame, 0, "字符");
  await selectOption(addFrame, 1, "录入");
  await addFrame.locator("textarea").first().fill(`${marker}_REMARK`);
  const stateCheckbox = addFrame.locator('input[type="checkbox"]:not([disabled])').first();
  if (!(await stateCheckbox.isChecked())) {
    await stateCheckbox.check();
  }
  await captureScreenshot(page, "01-input-standard-add.png");
  await clickSave(page);
  const addResponse = await readResponse(await addResponsePromise);
  const inputStandardId = Number(addResponse.json && addResponse.json.data && addResponse.json.data.id);
  requireAssertion(
    "add API succeeds",
    addResponse.status === 200 && inputStandardId > 0 && !visibleErrorPattern.test(addResponse.text),
    addResponse
  );
  await markerRow(page);
  const insertSql = markerSql();
  const inserted = queryJson(insertSql);
  const insertPass =
    inserted &&
    Number(inserted.id) === inputStandardId &&
    inserted.code === marker &&
    inserted.name === `${marker}_NAME` &&
    inserted.valType === "PATROL_valueType/char" &&
    inserted.editType === "PATROL_editType/input" &&
    inserted.state === true &&
    inserted.valid === true;
  addOperation(
    "新增录入标准",
    submitPath,
    "POST",
    result.evidence.requests.find((item) => item.url.includes(submitPath) && item.method === "POST")?.postData,
    addResponse,
    insertSql,
    inserted,
    insertPass ? "PASS" : "FAIL"
  );
  requireAssertion("add is persisted in PostgreSQL", insertPass, inserted);

  let row = await markerRow(page);
  await row.click();
  await page.locator("#btn-modify").click();
  const modifyFrame = await waitForEditFrame(page);
  await modifyFrame.locator('[id="edit_form_inputStandard.name"]').fill(updatedName);
  await modifyFrame.locator("textarea").first().fill(updatedRemark);
  await captureScreenshot(page, "02-input-standard-modify.png");
  const modifyResponsePromise = page.waitForResponse(
    (response) => response.url().includes(submitPath) && response.request().method() === "POST",
    { timeout: pageTimeoutMs }
  );
  await clickSave(page);
  const modifyResponse = await readResponse(await modifyResponsePromise);
  requireAssertion(
    "modify API succeeds",
    modifyResponse.status === 200 && !visibleErrorPattern.test(modifyResponse.text),
    modifyResponse
  );
  await page.waitForFunction(
    ({ code, name }) => (document.body.innerText || "").includes(code) && (document.body.innerText || "").includes(name),
    { code: marker, name: updatedName },
    { timeout: 30000 }
  );
  const updateSql = markerSql();
  const updated = queryJson(updateSql);
  const updatePass =
    updated &&
    updated.name === updatedName &&
    updated.remark === updatedRemark &&
    Number(updated.version) > Number(inserted.version);
  addOperation(
    "修改录入标准",
    submitPath,
    "POST",
    result.evidence.requests.filter((item) => item.url.includes(submitPath) && item.method === "POST").at(-1)
      ?.postData,
    modifyResponse,
    updateSql,
    updated,
    updatePass ? "PASS" : "FAIL"
  );
  requireAssertion("modify is persisted in PostgreSQL", updatePass, updated);

  row = await markerRow(page);
  await row.click();
  const stopResponsePromise = page.waitForResponse(
    (response) => response.url().includes(statePath) && response.url().includes("itemState=0"),
    { timeout: pageTimeoutMs }
  );
  await page.locator("#btn-stop").click();
  const stopResponse = await readResponse(await stopResponsePromise);
  const stopped = queryJson(markerSql());
  const stopPass = stopResponse.status === 200 && stopped && stopped.state === false;
  addOperation(
    "停用录入标准",
    `${statePath}?itemIds=${inputStandardId}&itemState=0&tableType=inputStand`,
    "GET",
    null,
    stopResponse,
    markerSql(),
    stopped,
    stopPass ? "PASS" : "FAIL"
  );
  requireAssertion("stop is persisted in PostgreSQL", stopPass, stopped);

  row = await markerRow(page);
  await row.click();
  const runResponsePromise = page.waitForResponse(
    (response) => response.url().includes(statePath) && response.url().includes("itemState=1"),
    { timeout: pageTimeoutMs }
  );
  await page.locator("#btn-run").click();
  const runResponse = await readResponse(await runResponsePromise);
  const running = queryJson(markerSql());
  const runPass = runResponse.status === 200 && running && running.state === true;
  addOperation(
    "启用录入标准",
    `${statePath}?itemIds=${inputStandardId}&itemState=1&tableType=inputStand`,
    "GET",
    null,
    runResponse,
    markerSql(),
    running,
    runPass ? "PASS" : "FAIL"
  );
  requireAssertion("run is persisted in PostgreSQL", runPass, running);

  row = await markerRow(page);
  await row.click();
  const relationResponsePromise = page.waitForResponse(
    (response) => response.url().includes(relationPath),
    { timeout: pageTimeoutMs }
  );
  await page.locator("#btn-delete").click();
  const relationResponse = await readResponse(await relationResponsePromise);
  requireAssertion("delete relation check succeeds", relationResponse.status === 200, relationResponse);
  const deleteResponsePromise = page.waitForResponse(
    (response) => response.url().includes(deletePath),
    { timeout: pageTimeoutMs }
  );
  await waitForConfirmAndAccept(page);
  const deleteResponse = await readResponse(await deleteResponsePromise);
  await page.waitForTimeout(800);
  const deleted = queryJson(markerSql());
  const markerVisibleAfterDelete = await page.getByText(marker, { exact: true }).count();
  const deletePass =
    deleteResponse.status === 200 &&
    deleted &&
    deleted.valid === false &&
    markerVisibleAfterDelete === 0;
  addOperation(
    "删除录入标准",
    `${deletePath}?standerdIds=${inputStandardId}`,
    "GET",
    null,
    deleteResponse,
    markerSql(),
    { ...deleted, markerVisibleAfterDelete },
    deletePass ? "PASS" : "FAIL"
  );
  requireAssertion("delete is persisted and removed from list", deletePass, {
    deleted,
    markerVisibleAfterDelete,
  });
  await captureScreenshot(page, "03-input-standard-after-delete.png");

  const bodyAfter = await page.locator("body").innerText();
  addAssertion("no visible runtime error", !visibleErrorPattern.test(bodyAfter), bodyAfter.slice(-800));
  addAssertion("no browser console error", result.evidence.consoleErrors.length === 0, result.evidence.consoleErrors);
  addAssertion("no page error", result.evidence.pageErrors.length === 0, result.evidence.pageErrors);
  addAssertion(
    "no PATROL request failure",
    result.evidence.requestFailures.length === 0,
    result.evidence.requestFailures
  );

  result.summary.status = result.summary.fail === 0 ? "PASS" : "FAIL";
  await browser.close();
  activeBrowser = null;
}

main()
  .catch(async (error) => {
    result.error = { message: error.message, stack: error.stack };
    result.summary.status = "FAIL";
    if (activeBrowser) {
      await activeBrowser.close().catch(() => {});
      activeBrowser = null;
    }
  })
  .finally(() => {
    ensureDir(path.dirname(outputPath));
    fs.writeFileSync(outputPath, `${JSON.stringify(result, null, 2)}\n`, "utf8");
    console.log(`PATROL input-standard persistence acceptance: ${result.summary.status}`);
    console.log(`marker: ${marker}`);
    console.log(`assertions: ${result.summary.pass}/${result.summary.assertions} passed`);
    console.log(`output: ${outputPath}`);
    if (result.error) {
      console.error(result.error.message);
      process.exitCode = 1;
    } else if (result.summary.status !== "PASS") {
      process.exitCode = 1;
    }
  });
