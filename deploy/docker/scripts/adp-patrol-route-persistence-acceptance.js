#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { execFileSync } = require("child_process");
const { chromium, request } = require("playwright");

const now = new Date();
const stamp = now.toISOString().replace(/[-:T.Z]/g, "").slice(0, 14);
const marker = process.env.ADP_E2E_MARKER || `ADP_E2E_${stamp}_PATROL_ROUTE`;
const baseUrl = (process.env.ADP_BASE_URL || "http://10.11.100.17:18080").replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD || "123456";
const headless = process.env.ADP_HEADLESS !== "false";
const pageTimeoutMs = Number(process.env.ADP_PAGE_TIMEOUT_MS || 180000);
const outputDir = process.env.ADP_OUTPUT_DIR || path.join("/tmp", `adp-patrol-route-${stamp}`);
const outputPath =
  process.env.ADP_PATROL_ROUTE_PERSISTENCE_OUTPUT || path.join(outputDir, "patrol-route-persistence.json");
const dbSshTarget = process.env.ADP_DB_SSH_TARGET || "v6@10.11.100.17";
const dbContainer = process.env.ADP_DB_CONTAINER || "adp-mes-newbase-postgres-1";
const dbName = process.env.ADP_DB_NAME || "adp";
const dbUser = process.env.ADP_DB_USER || "adp";

const listRoute = "/msService/PATROL/patrolRoute/workGroup/workGroupList";
const submitPath = "/msService/PATROL/patrolRoute/workGroup/workGroupEdit/submit";
const statePath = "/msService/PATROL/publicItem/publicItem/updateItemState";
const relationPath = "/msService/PATROL/patrolRoute/workGroup/checkRelationPlan";
const deletePath = "/msService/PATROL/patrolRoute/workGroup/deleteWorkGroups";
const updatedName = `${marker}_UPDATED`;
const updatedRemark = `${marker}_UPDATED_REMARK`;
const visibleErrorPattern =
  /(数据库操作异常|系统错误|系统异常|SQLGrammarException|could not extract ResultSet|column .* does not exist|relation .* does not exist|500 INTERNAL|自定义代码错误|\b[\w.]+Exception(?::|\s+at\b))/i;
const rawI18nPattern = /ec\.print\.template\.(?:delete|Stop|import)/;

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
    table: "public.mp_work_groups",
    backendEntry:
      "PATROLWorkGroupController/PATROLPublicItemController -> PATROLWorkGroupServiceImpl/PATROLPublicItemServiceImpl -> PATROLWorkGroupDao/JPA",
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
  'isRun', is_run,
  'valid', valid,
  'patrolType', patrol_type,
  'departmentId', dept,
  'remark', remark,
  'cid', cid
) from public.mp_work_groups where code=${sqlLiteral(marker)} order by id desc limit 1;
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

function routeToolbar(page) {
  return page
    .locator(".sup-datagrid-button-wrap")
    .filter({ has: page.locator(".sup-datagrid-title", { hasText: "巡检路线" }) })
    .first();
}

async function openRouteMore(page) {
  const more = routeToolbar(page).locator("#btn-more");
  let menu = more.locator(".restbtn-wrap");
  if (!(await menu.count()) || !(await menu.isVisible())) {
    await more.click();
    menu = more.locator(".restbtn-wrap");
  }
  await menu.waitFor({ state: "visible", timeout: 10000 });
  return menu;
}

async function waitForEditFrame(page) {
  const iframe = page.locator('iframe[src*="workGroupEdit"]').last();
  await iframe.waitFor({ state: "attached", timeout: 30000 });
  const handle = await iframe.elementHandle();
  const frame = handle && (await handle.contentFrame());
  if (!frame) {
    throw new Error("PATROL route edit iframe did not open");
  }
  await frame.waitForLoadState("domcontentloaded", { timeout: 30000 });
  await frame.waitForFunction(() => (document.body.innerText || "").includes("巡检类型"), null, {
    timeout: 30000,
  });
  return frame;
}

async function clickSave(page) {
  const button = page.locator("button:visible").filter({ hasText: /^\s*保\s*存\s*$/ }).last();
  await button.waitFor({ state: "visible", timeout: 10000 });
  await button.click();
}

async function markerRow(page) {
  const cell = page.getByText(marker, { exact: true }).first();
  await cell.waitFor({ state: "visible", timeout: 30000 });
  return cell.locator('xpath=ancestor::div[contains(@class,"sup-datagrid-row")]').first();
}

async function selectMarkerRow(page) {
  await page.reload({ waitUntil: "domcontentloaded", timeout: pageTimeoutMs });
  await page.waitForFunction(() => (document.body.innerText || "").includes("巡检路线编码"), null, {
    timeout: 60000,
  });
  const cell = page.getByText(marker, { exact: true }).first();
  await cell.waitFor({ state: "visible", timeout: 30000 });
  await cell.click();
  await page.waitForTimeout(750);
  const row = await markerRow(page);
  const checkbox = row.locator('input[type="checkbox"]').first();
  if (await checkbox.count()) {
    if (!(await checkbox.isChecked())) {
      await checkbox.click();
    }
  } else {
    await row.click();
  }
  await page.waitForFunction(
    (code) =>
      ReactAPI.getComponentAPI("SupDataGrid")
        .APIs("PATROL_1.0.0_patrolRoute_workGroupListdg1575506219664")
        .getSelecteds()
        .some((item) => item.code === code),
    marker,
    { timeout: 10000 }
  );
  await page.waitForTimeout(250);
  return row;
}

async function captureScreenshot(page, fileName) {
  const filePath = path.join(outputDir, fileName);
  await page.screenshot({ path: filePath, fullPage: true });
  result.evidence.screenshots.push(filePath);
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
  throw new Error("PATROL route delete confirmation button was not found");
}

function requestPayload(url, method) {
  return result.evidence.requests
    .filter((item) => item.url.includes(url) && item.method === method)
    .at(-1)?.postData;
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

async function main() {
  ensureDir(outputDir);
  ensureDir(path.dirname(outputPath));

  const preexisting = queryJson(
    `select json_build_object('count', count(*)) from public.mp_work_groups where code=${sqlLiteral(marker)};`
  );
  requireAssertion("marker is unique before test", Number(preexisting.count) === 0, preexisting);

  const api = await request.newContext({ ignoreHTTPSErrors: true });
  const ticket = await login(api);
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
  await page.waitForFunction(() => (document.body.innerText || "").includes("巡检路线编码"), null, {
    timeout: 60000,
  });
  const initialBody = await page.locator("body").innerText();
  requireAssertion(
    "route list page loads",
    navigation &&
      navigation.status() === 200 &&
      initialBody.includes("巡检区域") &&
      initialBody.includes("巡检项") &&
      !visibleErrorPattern.test(initialBody),
    { status: navigation && navigation.status(), body: initialBody.slice(0, 1000) }
  );

  const routeMore = await openRouteMore(page);
  await routeMore.getByText("删除", { exact: true }).waitFor({ state: "visible", timeout: 10000 });
  const routeMoreText = await routeMore.innerText();
  requireAssertion(
    "route more-menu uses Chinese business labels",
    routeMoreText.includes("删除") &&
      routeMoreText.includes("启用") &&
      routeMoreText.includes("停用") &&
      routeMoreText.includes("导入") &&
      !rawI18nPattern.test(routeMoreText),
    routeMoreText
  );
  await routeToolbar(page).locator("#btn-more").click();

  const addResponsePromise = page.waitForResponse(
    (response) => response.url().includes(submitPath) && response.request().method() === "POST",
    { timeout: pageTimeoutMs }
  );
  await routeToolbar(page).locator("#btn-add").click();
  const addFrame = await waitForEditFrame(page);
  await addFrame.locator('[id="edit_form_workGroup.code"]').fill(marker);
  await addFrame.locator('[id="edit_form_workGroup.name"]').fill(`${marker}_NAME`);
  const addState = addFrame.locator('input[type="checkbox"]:not([disabled])').first();
  if (!(await addState.isChecked())) {
    await addState.check();
  }
  await addFrame.locator("textarea").first().fill(`${marker}_REMARK`);
  await captureScreenshot(page, "01-patrol-route-add.png");
  await clickSave(page);
  const addResponse = await readResponse(await addResponsePromise);
  const routeId = Number(addResponse.json && addResponse.json.data && addResponse.json.data.id);
  requireAssertion(
    "add route API succeeds",
    addResponse.status === 200 && routeId > 0 && !visibleErrorPattern.test(addResponse.text),
    addResponse
  );
  await markerRow(page);
  const insertSql = markerSql();
  const inserted = queryJson(insertSql);
  const insertPass =
    inserted &&
    Number(inserted.id) === routeId &&
    inserted.code === marker &&
    inserted.name === `${marker}_NAME` &&
    inserted.isRun === true &&
    inserted.valid === true &&
    inserted.patrolType === "PATROL_routeType/eam" &&
    Number(inserted.departmentId) > 0;
  addOperation(
    "新增巡检路线",
    submitPath,
    "POST",
    requestPayload(submitPath, "POST"),
    addResponse,
    insertSql,
    inserted,
    insertPass ? "PASS" : "FAIL"
  );
  requireAssertion("add route is persisted in PostgreSQL", insertPass, inserted);

  await selectMarkerRow(page);
  await routeToolbar(page).locator("#btn-modify").click();
  const modifyFrame = await waitForEditFrame(page);
  await modifyFrame.locator('[id="edit_form_workGroup.name"]').fill(updatedName);
  await modifyFrame.locator("textarea").first().fill(updatedRemark);
  await captureScreenshot(page, "02-patrol-route-modify.png");
  const modifyResponsePromise = page.waitForResponse(
    (response) => response.url().includes(submitPath) && response.request().method() === "POST",
    { timeout: pageTimeoutMs }
  );
  await clickSave(page);
  const modifyResponse = await readResponse(await modifyResponsePromise);
  requireAssertion(
    "modify route API succeeds",
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
    "修改巡检路线",
    submitPath,
    "POST",
    requestPayload(submitPath, "POST"),
    modifyResponse,
    updateSql,
    updated,
    updatePass ? "PASS" : "FAIL"
  );
  requireAssertion("modify route is persisted in PostgreSQL", updatePass, updated);

  await selectMarkerRow(page);
  let menu = await openRouteMore(page);
  const stopResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes(statePath) &&
      response.url().includes("itemState=0") &&
      response.url().includes("tableType=workRoute"),
    { timeout: pageTimeoutMs }
  );
  await menu.locator("#btn-stop").click();
  const stopResponse = await readResponse(await stopResponsePromise);
  const stopped = queryJson(markerSql());
  const stopPass = stopResponse.status === 200 && stopped && stopped.isRun === false;
  addOperation(
    "停用巡检路线",
    `${statePath}?itemIds=${routeId}&itemState=0&tableType=workRoute`,
    "GET",
    null,
    stopResponse,
    markerSql(),
    stopped,
    stopPass ? "PASS" : "FAIL"
  );
  requireAssertion("stop route is persisted in PostgreSQL", stopPass, stopped);

  await selectMarkerRow(page);
  menu = await openRouteMore(page);
  const runResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes(statePath) &&
      response.url().includes("itemState=1") &&
      response.url().includes("tableType=workRoute"),
    { timeout: pageTimeoutMs }
  );
  await menu.locator("#btn-run").click();
  const runResponse = await readResponse(await runResponsePromise);
  const running = queryJson(markerSql());
  const runPass = runResponse.status === 200 && running && running.isRun === true;
  addOperation(
    "启用巡检路线",
    `${statePath}?itemIds=${routeId}&itemState=1&tableType=workRoute`,
    "GET",
    null,
    runResponse,
    markerSql(),
    running,
    runPass ? "PASS" : "FAIL"
  );
  requireAssertion("run route is persisted in PostgreSQL", runPass, running);

  await selectMarkerRow(page);
  menu = await openRouteMore(page);
  const relationResponsePromise = page.waitForResponse(
    (response) => response.url().includes(relationPath) && response.request().method() === "POST",
    { timeout: pageTimeoutMs }
  );
  await menu.locator("#btn-delete").click();
  const relationResponse = await readResponse(await relationResponsePromise);
  requireAssertion(
    "delete route relation check succeeds",
    relationResponse.status === 200 && !visibleErrorPattern.test(relationResponse.text),
    relationResponse
  );
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
    deleteResponse.status === 200 && deleted && deleted.valid === false && markerVisibleAfterDelete === 0;
  addOperation(
    "删除巡检路线",
    `${deletePath}?routeIds=${routeId}`,
    "GET",
    null,
    deleteResponse,
    markerSql(),
    { deleted, markerVisibleAfterDelete },
    deletePass ? "PASS" : "FAIL"
  );
  requireAssertion(
    "delete route is persisted and removed from list",
    deletePass,
    { deleted, markerVisibleAfterDelete }
  );

  await captureScreenshot(page, "03-patrol-route-after-delete.png");
  const finalBody = await page.locator("body").innerText();
  requireAssertion(
    "no visible runtime or raw i18n error",
    !visibleErrorPattern.test(finalBody) && !rawI18nPattern.test(finalBody),
    finalBody.slice(-1200)
  );
  requireAssertion("no browser console error", result.evidence.consoleErrors.length === 0, result.evidence.consoleErrors);
  requireAssertion("no page error", result.evidence.pageErrors.length === 0, result.evidence.pageErrors);
  requireAssertion(
    "no PATROL request failure",
    result.evidence.requestFailures.length === 0,
    result.evidence.requestFailures
  );

  result.summary.status = result.summary.fail === 0 ? "PASS" : "FAIL";
  await browser.close();
  activeBrowser = null;
}

async function finish() {
  try {
    await main();
  } catch (error) {
    result.summary.status = "FAIL";
    result.error = error && error.stack ? error.stack : String(error);
    if (activeBrowser) {
      await activeBrowser.close().catch(() => {});
      activeBrowser = null;
    }
  }
  ensureDir(path.dirname(outputPath));
  fs.writeFileSync(outputPath, `${JSON.stringify(result, null, 2)}\n`, "utf8");
  console.log(JSON.stringify({ outputPath, marker, summary: result.summary, error: result.error }, null, 2));
  if (result.summary.status !== "PASS") {
    process.exitCode = 1;
  }
}

finish();
