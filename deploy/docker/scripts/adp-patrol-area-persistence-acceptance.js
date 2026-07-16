#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { execFileSync } = require("child_process");
const { chromium, request } = require("playwright");

const now = new Date();
const stamp = now.toISOString().replace(/[-:T.Z]/g, "").slice(0, 14);
const marker = process.env.ADP_E2E_MARKER || `ADP_E2E_${stamp}_PATROL_AREA`;
const baseUrl = (process.env.ADP_BASE_URL || "http://10.11.100.17:18080").replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD || "123456";
const routeCode = process.env.ADP_PATROL_ROUTE_CODE || "ADP_E2E_202607162045_PATROL_ROUTE";
const seedAreaCode =
  process.env.ADP_PATROL_SEED_AREA_CODE || "ADP_E2E_202607162112_PATROL_AREA";
const headless = process.env.ADP_HEADLESS !== "false";
const pageTimeoutMs = Number(process.env.ADP_PAGE_TIMEOUT_MS || 180000);
const outputDir = process.env.ADP_OUTPUT_DIR || path.join("/tmp", `adp-patrol-area-${stamp}`);
const outputPath =
  process.env.ADP_PATROL_AREA_PERSISTENCE_OUTPUT ||
  path.join(outputDir, "patrol-area-persistence.json");
const dbSshTarget = process.env.ADP_DB_SSH_TARGET || "v6@10.11.100.17";
const dbContainer = process.env.ADP_DB_CONTAINER || "adp-mes-newbase-postgres-1";
const dbName = process.env.ADP_DB_NAME || "adp";
const dbUser = process.env.ADP_DB_USER || "adp";

const listRoute = "/msService/PATROL/patrolRoute/workGroup/workGroupList";
const savePath = "/msService/PATROL/patrolRoute/workGroup/workAreaPtEdit/submit";
const statePath = "/msService/PATROL/publicItem/publicItem/updateItemState";
const deletePath = "/msService/PATROL/patrolRoute/workArea/deleteWorkAreas";
const routeGrid = "PATROL_1.0.0_patrolRoute_workGroupListdg1575506219664";
const areaGrid = "PATROL_1.0.0_patrolRoute_workGroupListdg1575506226708";
const areaEditGrid = "PATROL_1.0.0_patrolRoute_workAreaPtEditdg1575630363211";
const updatedName = `${marker}_UPDATED`;
const updatedRemark = `${marker}_UPDATED_REMARK`;
const signCode = `SIGN_${stamp}`;
const visibleErrorPattern =
  /(数据库操作异常|系统错误|系统异常|SQLGrammarException|could not extract ResultSet|column .* does not exist|relation .* does not exist|500 INTERNAL|自定义代码错误|\b[\w.]+Exception(?::|\s+at\b))/i;
const rawI18nPattern = /(?:ec\.print\.template\.(?:delete|Stop|import)|PATROL\.custom\.randon\d+)/;

let activeBrowser = null;
let cleanupTicket = null;
const result = {
  generatedAt: now.toISOString(),
  repoCommit: process.env.ADP_REPO_COMMIT || "WORKTREE",
  database: "PostgreSQL",
  marker,
  environment: {
    baseUrl,
    route: listRoute,
    database: "PostgreSQL",
    table: "public.mp_work_areas",
    routeCode,
    seedAreaCode,
    backendEntry:
      "PATROLWorkAreaController/PATROLPublicItemController -> PATROLWorkAreaServiceImpl/PATROLPublicItemServiceImpl -> PATROLWorkAreaDao/JPA",
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
    fallbackCleanup: null,
  },
  error: null,
};

function ensureDir(directory) {
  fs.mkdirSync(directory, { recursive: true });
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

function areaSql(code = marker) {
  return `
select json_build_object(
  'id', id,
  'version', version,
  'code', code,
  'name', name,
  'isRun', is_run,
  'valid', valid,
  'isDevice', is_device,
  'deviceId', device,
  'isSign', is_sign,
  'signCode', sign_code,
  'sort', sort,
  'workGroupId', work_group_id,
  'remark', remark,
  'cid', cid
) from public.mp_work_areas where code=${sqlLiteral(code)} order by id desc limit 1;
`;
}

function routeSql() {
  return `
select json_build_object(
  'id', id,
  'version', version,
  'code', code,
  'name', name,
  'valid', valid,
  'cid', cid
) from public.mp_work_groups where code=${sqlLiteral(routeCode)} order by id desc limit 1;
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

function toolbar(page, title) {
  return page
    .locator(".sup-datagrid-button-wrap")
    .filter({ has: page.locator(".sup-datagrid-title", { hasText: title }) })
    .first();
}

async function openMore(scope, title) {
  const more = toolbar(scope, title).locator("#btn-more");
  let menu = more.locator(".restbtn-wrap");
  if (!(await menu.count()) || !(await menu.isVisible())) {
    await more.click();
    menu = more.locator(".restbtn-wrap");
  }
  await menu.waitFor({ state: "visible", timeout: 10000 });
  return { more, menu };
}

async function loadAndSelectRoute(page) {
  await page.reload({ waitUntil: "domcontentloaded", timeout: pageTimeoutMs });
  await page.waitForFunction(() => (document.body.innerText || "").includes("巡检路线编码"), null, {
    timeout: 60000,
  });
  const cell = page.getByText(routeCode, { exact: true }).first();
  await cell.waitFor({ state: "visible", timeout: 30000 });
  await cell.click();
  await page.waitForTimeout(500);
  const row = cell.locator('xpath=ancestor::div[contains(@class,"sup-datagrid-row")]').first();
  const checkbox = row.locator('input[type="checkbox"]').first();
  if (await checkbox.count()) {
    if (!(await checkbox.isChecked())) {
      await checkbox.click();
    }
  } else {
    await row.click();
  }
  await page.waitForFunction(
    ({ code, gridCode }) =>
      ReactAPI.getComponentAPI("SupDataGrid")
        .APIs(gridCode)
        .getSelecteds()
        .some((item) => item.code === code),
    { code: routeCode, gridCode: routeGrid },
    { timeout: 10000 }
  );
  await page.waitForTimeout(600);
}

async function openAreaEditor(page) {
  const { more, menu } = await openMore(page, "巡检路线");
  await menu.locator("#btn-workAreaSet").click();
  await more.locator(".restbtn-wrap").waitFor({ state: "hidden", timeout: 10000 });
  const iframe = page.locator('iframe[src*="workAreaPtEdit"]').last();
  await iframe.waitFor({ state: "attached", timeout: 30000 });
  const handle = await iframe.elementHandle();
  const frame = handle && (await handle.contentFrame());
  if (!frame) {
    throw new Error("PATROL area editor iframe did not open");
  }
  await frame.waitForLoadState("domcontentloaded", { timeout: 30000 });
  await frame.waitForFunction(
    (gridCode) => {
      try {
        return Boolean(ReactAPI.getComponentAPI("SupDataGrid").APIs(gridCode));
      } catch (_error) {
        return false;
      }
    },
    areaEditGrid,
    { timeout: 30000 }
  );
  return frame;
}

async function openEditorMore(frame) {
  const more = frame.locator("#btn-more");
  let menu = more.locator(".restbtn-wrap");
  if (!(await menu.count()) || !(await menu.isVisible())) {
    await more.click();
    menu = more.locator(".restbtn-wrap");
  }
  await menu.waitFor({ state: "visible", timeout: 10000 });
  return { more, menu };
}

async function addEditorRow(frame) {
  const before = await frame.evaluate(
    (gridCode) => ReactAPI.getComponentAPI("SupDataGrid").APIs(gridCode).getDatagridData().length,
    areaEditGrid
  );
  const { more, menu } = await openEditorMore(frame);
  await menu.locator("#btn-add").click();
  await more.locator(".restbtn-wrap").waitFor({ state: "hidden", timeout: 10000 });
  const after = await frame.evaluate(
    (gridCode) => ReactAPI.getComponentAPI("SupDataGrid").APIs(gridCode).getDatagridData().length,
    areaEditGrid
  );
  return { before, after };
}

async function populateLastAreaRow(frame) {
  return frame.evaluate(
    ({ gridCode, values }) => {
      const dataGrid = ReactAPI.getComponentAPI("SupDataGrid").APIs(gridCode);
      const rows = dataGrid.getDatagridData();
      const row = rows[rows.length - 1];
      Object.keys(values).forEach((key) => dataGrid.setValueByKey(row.rowIndex, key, values[key]));
      return dataGrid.getDatagridData()[dataGrid.getDatagridData().length - 1];
    },
    {
      gridCode: areaEditGrid,
      values: {
        code: marker,
        name: `${marker}_NAME`,
        signCode,
        isSign: false,
        isDevice: false,
        isRun: true,
        remark: `${marker}_REMARK`,
      },
    }
  );
}

async function exerciseEditorRowActions(frame) {
  const addResult = await addEditorRow(frame);
  const scratch = await frame.evaluate((gridCode) => {
    const dataGrid = ReactAPI.getComponentAPI("SupDataGrid").APIs(gridCode);
    const rows = dataGrid.getDatagridData();
    const row = rows[rows.length - 1];
    dataGrid.setSelecteds(String(row.rowIndex));
    return { rowIndex: row.rowIndex, key: row.key, index: rows.length - 1 };
  }, areaEditGrid);

  let controls = await openEditorMore(frame);
  await controls.menu.locator("#btn-moveUp").click();
  await controls.more.locator(".restbtn-wrap").waitFor({ state: "hidden", timeout: 10000 });
  const afterUp = await frame.evaluate(
    ({ gridCode, key }) =>
      ReactAPI.getComponentAPI("SupDataGrid")
        .APIs(gridCode)
        .getDatagridData()
        .findIndex((row) => row.key === key),
    { gridCode: areaEditGrid, key: scratch.key }
  );

  controls = await openEditorMore(frame);
  await controls.menu.locator("#btn-moveDown").click();
  await controls.more.locator(".restbtn-wrap").waitFor({ state: "hidden", timeout: 10000 });
  const afterDown = await frame.evaluate(
    ({ gridCode, key }) =>
      ReactAPI.getComponentAPI("SupDataGrid")
        .APIs(gridCode)
        .getDatagridData()
        .findIndex((row) => row.key === key),
    { gridCode: areaEditGrid, key: scratch.key }
  );

  controls = await openEditorMore(frame);
  await controls.menu.locator("#btn-delete").click();
  await controls.more.locator(".restbtn-wrap").waitFor({ state: "hidden", timeout: 10000 });
  const afterDelete = await frame.evaluate(
    ({ gridCode, key }) => {
      const rows = ReactAPI.getComponentAPI("SupDataGrid").APIs(gridCode).getDatagridData();
      return { count: rows.length, scratchPresent: rows.some((row) => row.key === key) };
    },
    { gridCode: areaEditGrid, key: scratch.key }
  );
  return { addResult, scratch, afterUp, afterDown, afterDelete };
}

async function clickDialogSave(page) {
  const button = page.locator("button:visible").filter({ hasText: /^\s*保\s*存\s*$/ }).last();
  await button.waitFor({ state: "visible", timeout: 10000 });
  await button.click();
}

async function waitForMainArea(page, code) {
  await page.waitForFunction(
    ({ gridCode, value }) =>
      ReactAPI.getComponentAPI("SupDataGrid")
        .APIs(gridCode)
        .getDatagridData()
        .some((row) => row.code === value),
    { gridCode: areaGrid, value: code },
    { timeout: 30000 }
  );
}

async function selectMainArea(page, code) {
  await waitForMainArea(page, code);
  return page.evaluate(
    ({ gridCode, value }) => {
      const dataGrid = ReactAPI.getComponentAPI("SupDataGrid").APIs(gridCode);
      const row = dataGrid.getDatagridData().find((item) => item.code === value);
      dataGrid.setSelecteds(String(row.rowIndex));
      return { id: row.id, rowIndex: row.rowIndex, isRun: row.isRun, valid: row.valid };
    },
    { gridCode: areaGrid, value: code }
  );
}

async function editMarkerRow(frame) {
  await frame.waitForFunction(
    ({ gridCode, code }) =>
      ReactAPI.getComponentAPI("SupDataGrid")
        .APIs(gridCode)
        .getDatagridData()
        .some((item) => item.code === code),
    { gridCode: areaEditGrid, code: marker },
    { timeout: 30000 }
  );
  return frame.evaluate(
    ({ gridCode, code, name, remark }) => {
      const dataGrid = ReactAPI.getComponentAPI("SupDataGrid").APIs(gridCode);
      const row = dataGrid.getDatagridData().find((item) => item.code === code);
      if (!row) {
        throw new Error(`Area row not found: ${code}`);
      }
      dataGrid.setValueByKey(row.rowIndex, "name", name);
      dataGrid.setValueByKey(row.rowIndex, "remark", remark);
      return dataGrid.getDatagridData().find((item) => item.code === code);
    },
    { gridCode: areaEditGrid, code: marker, name: updatedName, remark: updatedRemark }
  );
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
  throw new Error("PATROL area delete confirmation button was not found");
}

async function captureScreenshot(page, fileName) {
  const filePath = path.join(outputDir, fileName);
  await page.screenshot({ path: filePath, fullPage: true });
  result.evidence.screenshots.push(filePath);
}

function requestPayload(url, method) {
  const matching = result.evidence.requests.filter(
    (item) => item.url.includes(url) && item.method === method
  );
  return matching.length ? matching[matching.length - 1].postData : null;
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

async function fallbackCleanup() {
  const current = queryJson(areaSql());
  if (!current || current.valid !== true) {
    result.evidence.fallbackCleanup = { required: false, current };
    return;
  }
  const api = await request.newContext({ ignoreHTTPSErrors: true });
  try {
    const ticket = cleanupTicket || (await login(api));
    const response = await api.get(`${baseUrl}${deletePath}?workAreaIds=${current.id}`, {
      headers: { Authorization: `Bearer ${ticket}` },
    });
    const parsed = await readResponse(response);
    result.evidence.fallbackCleanup = {
      required: true,
      response: parsed,
      after: queryJson(areaSql()),
    };
  } finally {
    await api.dispose();
  }
}

async function main() {
  ensureDir(outputDir);
  ensureDir(path.dirname(outputPath));

  const preexisting = queryJson(
    `select json_build_object('count', count(*)) from public.mp_work_areas where code=${sqlLiteral(marker)};`
  );
  requireAssertion("marker is unique before test", Number(preexisting.count) === 0, preexisting);
  const seedRouteBefore = queryJson(routeSql());
  const seedAreaBefore = queryJson(areaSql(seedAreaCode));
  requireAssertion(
    "seed route and seed area are available",
    seedRouteBefore &&
      seedRouteBefore.valid === true &&
      seedAreaBefore &&
      seedAreaBefore.valid === true &&
      Number(seedAreaBefore.workGroupId) === Number(seedRouteBefore.id),
    { seedRouteBefore, seedAreaBefore }
  );

  const api = await request.newContext({ ignoreHTTPSErrors: true });
  const ticket = await login(api);
  cleanupTicket = ticket;
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
        postData: requestItem.postData() ? requestItem.postData().slice(0, 8000) : null,
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
    "patrol route and area page loads",
    navigation &&
      navigation.status() === 200 &&
      initialBody.includes("巡检区域") &&
      initialBody.includes("巡检项") &&
      !visibleErrorPattern.test(initialBody),
    { status: navigation && navigation.status(), body: initialBody.slice(0, 1200) }
  );

  await loadAndSelectRoute(page);
  let frame = await openAreaEditor(page);
  let editorMore = await openEditorMore(frame);
  const editorMenuText = await editorMore.menu.innerText();
  requireAssertion(
    "area editor menu uses Chinese business labels",
    editorMenuText.includes("增行") &&
      editorMenuText.includes("删行") &&
      editorMenuText.includes("上移") &&
      editorMenuText.includes("下移") &&
      !rawI18nPattern.test(editorMenuText),
    editorMenuText
  );
  await editorMore.more.click();

  const addBehavior = await addEditorRow(frame);
  requireAssertion(
    "area editor add-row click adds exactly one row",
    addBehavior.after === addBehavior.before + 1,
    addBehavior
  );
  const populated = await populateLastAreaRow(frame);
  requireAssertion(
    "new area row accepts required business fields",
    populated.code === marker &&
      populated.name === `${marker}_NAME` &&
      populated.isRun === true &&
      populated.isDevice === false,
    populated
  );
  const actionBehavior = await exerciseEditorRowActions(frame);
  requireAssertion(
    "area editor move and delete row actions work",
    actionBehavior.addResult.after === actionBehavior.addResult.before + 1 &&
      actionBehavior.afterUp === actionBehavior.scratch.index - 1 &&
      actionBehavior.afterDown === actionBehavior.scratch.index &&
      actionBehavior.afterDelete.count === actionBehavior.addResult.before &&
      actionBehavior.afterDelete.scratchPresent === false,
    actionBehavior
  );

  await captureScreenshot(page, "01-patrol-area-add.png");
  const addResponsePromise = page.waitForResponse(
    (response) => response.url().includes(savePath) && response.request().method() === "POST",
    { timeout: pageTimeoutMs }
  );
  await clickDialogSave(page);
  const addResponse = await readResponse(await addResponsePromise);
  requireAssertion(
    "add area API succeeds",
    addResponse.status === 200 &&
      addResponse.json &&
      addResponse.json.code === 200 &&
      !visibleErrorPattern.test(addResponse.text),
    addResponse
  );
  await waitForMainArea(page, marker);
  const insertSql = areaSql();
  const inserted = queryJson(insertSql);
  const insertPass =
    inserted &&
    inserted.code === marker &&
    inserted.name === `${marker}_NAME` &&
    inserted.isRun === true &&
    inserted.valid === true &&
    inserted.isDevice === false &&
    inserted.signCode === signCode &&
    Number(inserted.workGroupId) === Number(seedRouteBefore.id) &&
    Number(inserted.sort) > Number(seedAreaBefore.sort) &&
    Number(inserted.cid) === Number(seedRouteBefore.cid);
  addOperation(
    "新增巡检区域",
    savePath,
    "POST",
    requestPayload(savePath, "POST"),
    addResponse,
    insertSql,
    inserted,
    insertPass ? "PASS" : "FAIL"
  );
  requireAssertion("add area is persisted in PostgreSQL", insertPass, inserted);

  await loadAndSelectRoute(page);
  frame = await openAreaEditor(page);
  const edited = await editMarkerRow(frame);
  requireAssertion(
    "existing area row accepts modifications",
    edited.name === updatedName && edited.remark === updatedRemark,
    edited
  );
  await captureScreenshot(page, "02-patrol-area-modify.png");
  const modifyResponsePromise = page.waitForResponse(
    (response) => response.url().includes(savePath) && response.request().method() === "POST",
    { timeout: pageTimeoutMs }
  );
  await clickDialogSave(page);
  const modifyResponse = await readResponse(await modifyResponsePromise);
  requireAssertion(
    "modify area API succeeds",
    modifyResponse.status === 200 &&
      modifyResponse.json &&
      modifyResponse.json.code === 200 &&
      !visibleErrorPattern.test(modifyResponse.text),
    modifyResponse
  );
  const updateSql = areaSql();
  const updated = queryJson(updateSql);
  const updatePass =
    updated &&
    updated.name === updatedName &&
    updated.remark === updatedRemark &&
    Number(updated.version) > Number(inserted.version);
  addOperation(
    "修改巡检区域",
    savePath,
    "POST",
    requestPayload(savePath, "POST"),
    modifyResponse,
    updateSql,
    updated,
    updatePass ? "PASS" : "FAIL"
  );
  requireAssertion("modify area is persisted in PostgreSQL", updatePass, updated);

  await loadAndSelectRoute(page);
  await selectMainArea(page, marker);
  let areaMore = await openMore(page, "巡检区域");
  const areaMenuText = await areaMore.menu.innerText();
  requireAssertion(
    "area more-menu uses Chinese business labels",
    areaMenuText.includes("删除") &&
      areaMenuText.includes("巡检项设置") &&
      areaMenuText.includes("启用") &&
      areaMenuText.includes("停用") &&
      !rawI18nPattern.test(areaMenuText),
    areaMenuText
  );
  const stopResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes(statePath) &&
      response.url().includes(`itemIds=${inserted.id}`) &&
      response.url().includes("itemState=0") &&
      response.url().includes("tableType=workArea"),
    { timeout: pageTimeoutMs }
  );
  await areaMore.menu.locator("#btn-stop").click();
  const stopResponse = await readResponse(await stopResponsePromise);
  const stopped = queryJson(areaSql());
  const stopPass =
    stopResponse.status === 200 &&
    stopResponse.json &&
    stopResponse.json.code === 200 &&
    stopped &&
    stopped.isRun === false;
  addOperation(
    "停用巡检区域",
    `${statePath}?itemIds=${inserted.id}&itemState=0&tableType=workArea`,
    "GET",
    null,
    stopResponse,
    areaSql(),
    stopped,
    stopPass ? "PASS" : "FAIL"
  );
  requireAssertion("stop area is persisted in PostgreSQL", stopPass, stopped);

  await loadAndSelectRoute(page);
  await selectMainArea(page, marker);
  areaMore = await openMore(page, "巡检区域");
  const runResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes(statePath) &&
      response.url().includes(`itemIds=${inserted.id}`) &&
      response.url().includes("itemState=1") &&
      response.url().includes("tableType=workArea"),
    { timeout: pageTimeoutMs }
  );
  await areaMore.menu.locator("#btn-run").click();
  const runResponse = await readResponse(await runResponsePromise);
  const running = queryJson(areaSql());
  const runPass =
    runResponse.status === 200 &&
    runResponse.json &&
    runResponse.json.code === 200 &&
    running &&
    running.isRun === true;
  addOperation(
    "启用巡检区域",
    `${statePath}?itemIds=${inserted.id}&itemState=1&tableType=workArea`,
    "GET",
    null,
    runResponse,
    areaSql(),
    running,
    runPass ? "PASS" : "FAIL"
  );
  requireAssertion("run area is persisted in PostgreSQL", runPass, running);

  await loadAndSelectRoute(page);
  await selectMainArea(page, marker);
  areaMore = await openMore(page, "巡检区域");
  const deleteResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes(deletePath) && response.url().includes(`workAreaIds=${inserted.id}`),
    { timeout: pageTimeoutMs }
  );
  await areaMore.menu.locator("#btn-delete").click();
  await waitForConfirmAndAccept(page);
  const deleteResponse = await readResponse(await deleteResponsePromise);
  await loadAndSelectRoute(page);
  const deleted = queryJson(areaSql());
  const markerVisibleAfterDelete = await page.evaluate(
    ({ gridCode, code }) =>
      ReactAPI.getComponentAPI("SupDataGrid")
        .APIs(gridCode)
        .getDatagridData()
        .some((row) => row.code === code),
    { gridCode: areaGrid, code: marker }
  );
  const deletePass =
    deleteResponse.status === 200 &&
    deleteResponse.json &&
    deleteResponse.json.code === 200 &&
    deleted &&
    deleted.valid === false &&
    markerVisibleAfterDelete === false;
  addOperation(
    "删除巡检区域",
    `${deletePath}?workAreaIds=${inserted.id}`,
    "GET",
    null,
    deleteResponse,
    areaSql(),
    { deleted, markerVisibleAfterDelete },
    deletePass ? "PASS" : "FAIL"
  );
  requireAssertion(
    "delete area is persisted and removed from list",
    deletePass,
    { deleted, markerVisibleAfterDelete }
  );

  const seedRouteAfter = queryJson(routeSql());
  const seedAreaAfter = queryJson(areaSql(seedAreaCode));
  requireAssertion(
    "seed route and seed area remain intact",
    seedRouteAfter &&
      seedRouteAfter.valid === true &&
      Number(seedRouteAfter.id) === Number(seedRouteBefore.id) &&
      seedAreaAfter &&
      seedAreaAfter.valid === true &&
      Number(seedAreaAfter.id) === Number(seedAreaBefore.id) &&
      seedAreaAfter.code === seedAreaBefore.code &&
      Number(seedAreaAfter.workGroupId) === Number(seedRouteAfter.id),
    { seedRouteBefore, seedRouteAfter, seedAreaBefore, seedAreaAfter }
  );

  await captureScreenshot(page, "03-patrol-area-after-delete.png");
  const finalBody = await page.locator("body").innerText();
  requireAssertion(
    "no visible runtime or raw i18n error",
    !visibleErrorPattern.test(finalBody) && !rawI18nPattern.test(finalBody),
    finalBody.slice(-1600)
  );
  requireAssertion(
    "no browser console error",
    result.evidence.consoleErrors.length === 0,
    result.evidence.consoleErrors
  );
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
    await fallbackCleanup().catch((cleanupError) => {
      result.evidence.fallbackCleanup = {
        required: true,
        error: cleanupError && cleanupError.stack ? cleanupError.stack : String(cleanupError),
      };
    });
  }
  ensureDir(path.dirname(outputPath));
  fs.writeFileSync(outputPath, `${JSON.stringify(result, null, 2)}\n`, "utf8");
  console.log(JSON.stringify({ outputPath, marker, summary: result.summary, error: result.error }, null, 2));
  if (result.summary.status !== "PASS") {
    process.exitCode = 1;
  }
}

finish();
