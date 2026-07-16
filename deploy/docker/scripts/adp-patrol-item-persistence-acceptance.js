#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { execFileSync } = require("child_process");
const { chromium, request } = require("playwright");

const now = new Date();
const stamp = now.toISOString().replace(/[-:T.Z]/g, "").slice(0, 14);
const marker = process.env.ADP_E2E_MARKER || `ADP_E2E_${stamp}_PATROL_ITEM`;
const itemNumber = `TAG_${marker}`;
const baseUrl = (process.env.ADP_BASE_URL || "http://10.11.100.17:18080").replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD || "123456";
const routeCode = process.env.ADP_PATROL_ROUTE_CODE || "ADP_E2E_202607162045_PATROL_ROUTE";
const areaCode = process.env.ADP_PATROL_AREA_CODE || "ADP_E2E_202607162112_PATROL_AREA";
const seedItemContent =
  process.env.ADP_PATROL_SEED_ITEM_CONTENT || "ADP_E2E_202607162116_PATROL_ITEM";
const headless = process.env.ADP_HEADLESS !== "false";
const pageTimeoutMs = Number(process.env.ADP_PAGE_TIMEOUT_MS || 180000);
const outputDir = process.env.ADP_OUTPUT_DIR || path.join("/tmp", `adp-patrol-item-${stamp}`);
const outputPath =
  process.env.ADP_PATROL_ITEM_PERSISTENCE_OUTPUT ||
  path.join(outputDir, "patrol-item-persistence.json");
const dbSshTarget = process.env.ADP_DB_SSH_TARGET || "v6@10.11.100.17";
const dbContainer = process.env.ADP_DB_CONTAINER || "adp-mes-newbase-postgres-1";
const dbName = process.env.ADP_DB_NAME || "adp";
const dbUser = process.env.ADP_DB_USER || "adp";

const listRoute = "/msService/PATROL/patrolRoute/workGroup/workGroupList";
const savePath = "/msService/PATROL/patrolRoute/workArea/workItemPtEdit/submit";
const statePath = "/msService/PATROL/publicItem/publicItem/updateItemState";
const deletePath = "/msService/PATROL/patrolRoute/workItem/deleteWorkItems";
const routeGrid = "PATROL_1.0.0_patrolRoute_workGroupListdg1575506219664";
const areaGrid = "PATROL_1.0.0_patrolRoute_workGroupListdg1575506226708";
const mainItemGrid = "PATROL_1.0.0_patrolRoute_workGroupListdg1575507309041";
const editorItemGrid = "PATROL_1.0.0_patrolRoute_workItemPtEditdg1575632686507";
const updatedContent = `${marker}_UPDATED`;
const updatedRemark = `${marker}_UPDATED_REMARK`;
const visibleErrorPattern =
  /(数据库操作异常|系统错误|系统异常|SQLGrammarException|could not extract ResultSet|column .* does not exist|relation .* does not exist|500 INTERNAL|自定义代码错误|\b[\w.]+Exception(?::|\s+at\b))/i;
const rawI18nPattern = /(?:PATROL\.buttonPropertyshowName|PATROL\.custom\.randon|ec\.view\.button)/;

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
    table: "public.mp_work_items",
    routeCode,
    areaCode,
    seedItemContent,
    backendEntry:
      "PATROLWorkAreaController/PATROLWorkItemController/PATROLPublicItemController -> PATROLWorkAreaServiceImpl/PATROLWorkItemServiceImpl/PATROLPublicItemServiceImpl -> PATROLWorkItemDao/JPA",
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

function itemSql() {
  return `
select json_build_object(
  'id', id,
  'version', version,
  'content', content,
  'part', part,
  'claim', claim,
  'defaultVal', default_val,
  'inputStandardId', input_standard_id,
  'isAutoJudge', is_auto_judge,
  'normalRange', normal_range,
  'lowerLimit', lower_limit,
  'upperLimit', upper_limit,
  'isPhone', is_phone,
  'isPass', is_pass,
  'isConclusionModify', is_conclusion_modify,
  'isRun', is_run,
  'valid', valid,
  'sort', sort,
  'itemNumber', item_number,
  'routeId', route_id,
  'workId', work_id,
  'remark', remark,
  'cid', cid
) from public.mp_work_items where item_number=${sqlLiteral(itemNumber)} order by id desc limit 1;
`;
}

function seedSql() {
  return `
select json_build_object(
  'id', id,
  'version', version,
  'content', content,
  'inputStandardId', input_standard_id,
  'isRun', is_run,
  'valid', valid,
  'routeId', route_id,
  'workId', work_id,
  'cid', cid
) from public.mp_work_items where content=${sqlLiteral(seedItemContent)} order by id desc limit 1;
`;
}

function areaSql() {
  return `
select json_build_object(
  'id', id,
  'code', code,
  'valid', valid,
  'workGroupId', work_group_id,
  'cid', cid
) from public.mp_work_areas where code=${sqlLiteral(areaCode)} order by id desc limit 1;
`;
}

function routeSql() {
  return `
select json_build_object(
  'id', id,
  'code', code,
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

function toolbar(scope, title) {
  return scope
    .locator(".sup-datagrid-button-wrap")
    .filter({ has: scope.locator(".sup-datagrid-title", { hasText: title }) })
    .first();
}

async function openMore(scope, title) {
  const buttonWrap = title
    ? toolbar(scope, title)
    : scope.locator(".sup-datagrid-button-wrap").filter({ has: scope.locator("#btn-more") }).first();
  const more = buttonWrap.locator("#btn-more");
  let menu = more.locator(".restbtn-wrap");
  if (!(await menu.count()) || !(await menu.isVisible())) {
    await more.click();
    menu = more.locator(".restbtn-wrap");
  }
  await menu.waitFor({ state: "visible", timeout: 10000 });
  return { more, menu };
}

async function loadAndSelectRouteArea(page) {
  await page.reload({ waitUntil: "domcontentloaded", timeout: pageTimeoutMs });
  await page.waitForFunction(() => (document.body.innerText || "").includes("巡检路线编码"), null, {
    timeout: 60000,
  });
  const routeCell = page.getByText(routeCode, { exact: true }).first();
  await routeCell.waitFor({ state: "visible", timeout: 30000 });
  await routeCell.click();
  await page.waitForTimeout(500);
  await page.evaluate(
    ({ gridCode, code }) => {
      const dataGrid = ReactAPI.getComponentAPI("SupDataGrid").APIs(gridCode);
      const row = dataGrid.getDatagridData().find((item) => item.code === code);
      dataGrid.setSelecteds(String(row.rowIndex));
    },
    { gridCode: routeGrid, code: routeCode }
  );
  await page.waitForFunction(
    ({ gridCode, code }) =>
      ReactAPI.getComponentAPI("SupDataGrid")
        .APIs(gridCode)
        .getDatagridData()
        .some((item) => item.code === code),
    { gridCode: areaGrid, code: areaCode },
    { timeout: 30000 }
  );
  await page.evaluate(
    ({ gridCode, code }) => {
      const dataGrid = ReactAPI.getComponentAPI("SupDataGrid").APIs(gridCode);
      const row = dataGrid.getDatagridData().find((item) => item.code === code);
      dataGrid.setSelecteds(String(row.rowIndex));
    },
    { gridCode: areaGrid, code: areaCode }
  );
  await page.waitForFunction(
    ({ gridCode, content }) =>
      ReactAPI.getComponentAPI("SupDataGrid")
        .APIs(gridCode)
        .getDatagridData()
        .some((item) => item.content === content),
    { gridCode: mainItemGrid, content: seedItemContent },
    { timeout: 30000 }
  );
}

async function openItemEditor(page) {
  const controls = await openMore(page, "巡检区域");
  await controls.menu.locator("#btn-workItemSet").click();
  await controls.more.locator(".restbtn-wrap").waitFor({ state: "hidden", timeout: 10000 });
  const iframe = page.locator('iframe[src*="workItemPtEdit"]').last();
  await iframe.waitFor({ state: "attached", timeout: 30000 });
  const handle = await iframe.elementHandle();
  const frame = handle && (await handle.contentFrame());
  if (!frame) {
    throw new Error("PATROL item editor iframe did not open");
  }
  await frame.waitForLoadState("domcontentloaded", { timeout: 30000 });
  await frame.waitForFunction(
    (gridCode) => {
      try {
        return (
          Boolean(ReactAPI.getComponentAPI("SupDataGrid").APIs(gridCode)) &&
          window.__ADP_PATROL_ITEM_EDITOR_ACTIONS_INSTALLED__ === true
        );
      } catch (_error) {
        return false;
      }
    },
    editorItemGrid,
    { timeout: 30000 }
  );
  return { iframe, frame };
}

async function clickEditorButton(frame, buttonId) {
  const direct = frame.locator(buttonId).first();
  if ((await direct.count()) && (await direct.isVisible())) {
    await direct.click();
    return;
  }
  const more = frame.locator("#btn-more");
  await more.click();
  const menu = more.locator(".restbtn-wrap");
  await menu.waitFor({ state: "visible", timeout: 10000 });
  await menu.locator(buttonId).click();
  await menu.waitFor({ state: "hidden", timeout: 10000 });
}

async function exerciseEditorRowActions(frame) {
  const before = await frame.evaluate(
    (gridCode) => ReactAPI.getComponentAPI("SupDataGrid").APIs(gridCode).getDatagridData().length,
    editorItemGrid
  );
  await clickEditorButton(frame, "#btn-add");
  const scratch = await frame.evaluate((gridCode) => {
    const dataGrid = ReactAPI.getComponentAPI("SupDataGrid").APIs(gridCode);
    const rows = dataGrid.getDatagridData();
    const row = rows[rows.length - 1];
    dataGrid.setSelecteds(String(row.rowIndex));
    return { key: row.key, rowIndex: row.rowIndex };
  }, editorItemGrid);

  await clickEditorButton(frame, "#btn-insertLine");
  const afterInsert = await frame.evaluate(
    ({ gridCode, scratchKey }) => {
      const rows = ReactAPI.getComponentAPI("SupDataGrid").APIs(gridCode).getDatagridData();
      const inserted = rows.find((row) => row.key !== scratchKey && !row.id);
      return { count: rows.length, key: inserted && inserted.key, rowIndex: inserted && inserted.rowIndex };
    },
    { gridCode: editorItemGrid, scratchKey: scratch.key }
  );
  await frame.evaluate(
    ({ gridCode, key }) => {
      const dataGrid = ReactAPI.getComponentAPI("SupDataGrid").APIs(gridCode);
      const row = dataGrid.getDatagridData().find((item) => item.key === key);
      dataGrid.setSelecteds(String(row.rowIndex));
    },
    { gridCode: editorItemGrid, key: afterInsert.key }
  );
  await clickEditorButton(frame, "#btn-moveDown");
  const afterDown = await frame.evaluate(
    ({ gridCode, key }) =>
      ReactAPI.getComponentAPI("SupDataGrid")
        .APIs(gridCode)
        .getDatagridData()
        .findIndex((row) => row.key === key),
    { gridCode: editorItemGrid, key: afterInsert.key }
  );
  await clickEditorButton(frame, "#btn-moveUpward");
  const afterUp = await frame.evaluate(
    ({ gridCode, key }) =>
      ReactAPI.getComponentAPI("SupDataGrid")
        .APIs(gridCode)
        .getDatagridData()
        .findIndex((row) => row.key === key),
    { gridCode: editorItemGrid, key: afterInsert.key }
  );
  await clickEditorButton(frame, "#btn-delete");
  await frame.evaluate(
    ({ gridCode, key }) => {
      const dataGrid = ReactAPI.getComponentAPI("SupDataGrid").APIs(gridCode);
      const row = dataGrid.getDatagridData().find((item) => item.key === key);
      dataGrid.setSelecteds(String(row.rowIndex));
    },
    { gridCode: editorItemGrid, key: scratch.key }
  );
  await clickEditorButton(frame, "#btn-delete");
  const afterDelete = await frame.evaluate(
    (gridCode) => ReactAPI.getComponentAPI("SupDataGrid").APIs(gridCode).getDatagridData().length,
    editorItemGrid
  );
  return { before, scratch, afterInsert, afterDown, afterUp, afterDelete };
}

async function populateMarkerRow(frame) {
  await clickEditorButton(frame, "#btn-add");
  return frame.evaluate(
    ({ gridCode, values }) => {
      const dataGrid = ReactAPI.getComponentAPI("SupDataGrid").APIs(gridCode);
      const rows = dataGrid.getDatagridData();
      const seed = rows.find((row) => row.id && row.inputStandardId && row.inputStandardId.id);
      const row = rows[rows.length - 1];
      const inputStandard = seed.inputStandardId;
      Object.keys(values).forEach((key) => dataGrid.setValueByKey(row.rowIndex, key, values[key]));
      dataGrid.setValueByKey(row.rowIndex, "inputStandardId", inputStandard);
      window.setInputStarnder([inputStandard], row.rowIndex);
      dataGrid.setValueByKey(row.rowIndex, "isAutoJudge", true);
      dataGrid.setValueByKey(row.rowIndex, "normalRange", "≥10|≤20");
      dataGrid.setValueByKey(row.rowIndex, "lowerLimit", 0);
      dataGrid.setValueByKey(row.rowIndex, "upperLimit", 100);
      dataGrid.setValueByKey(row.rowIndex, "isPhone", true);
      return dataGrid.getDatagridData().find((item) => item.key === row.key);
    },
    {
      gridCode: editorItemGrid,
      values: {
        part: `${marker}_PART`,
        claim: `${marker}_CLAIM`,
        content: marker,
        defaultVal: "12.34",
        isPass: false,
        isConclusionModify: true,
        itemNumber,
        remark: `${marker}_REMARK`,
      },
    }
  );
}

async function editMarkerRow(frame) {
  await frame.waitForFunction(
    ({ gridCode, value }) =>
      ReactAPI.getComponentAPI("SupDataGrid")
        .APIs(gridCode)
        .getDatagridData()
        .some((item) => item.itemNumber === value),
    { gridCode: editorItemGrid, value: itemNumber },
    { timeout: 30000 }
  );
  return frame.evaluate(
    ({ gridCode, value, content, remark }) => {
      const dataGrid = ReactAPI.getComponentAPI("SupDataGrid").APIs(gridCode);
      const row = dataGrid.getDatagridData().find((item) => item.itemNumber === value);
      dataGrid.setValueByKey(row.rowIndex, "content", content);
      dataGrid.setValueByKey(row.rowIndex, "remark", remark);
      dataGrid.setValueByKey(row.rowIndex, "isPhone", false);
      dataGrid.setSelecteds(String(row.rowIndex));
      return dataGrid.getDatagridData().find((item) => item.itemNumber === value);
    },
    { gridCode: editorItemGrid, value: itemNumber, content: updatedContent, remark: updatedRemark }
  );
}

async function clickDialogSave(page) {
  const button = page.locator("button:visible").filter({ hasText: /^\s*保\s*存\s*$/ }).last();
  await button.waitFor({ state: "visible", timeout: 10000 });
  await button.click();
}

async function responseAfter(page, predicate, action) {
  const [response] = await Promise.all([
    page.waitForResponse(predicate, { timeout: pageTimeoutMs }),
    action(),
  ]);
  return readResponse(response);
}

async function captureScreenshot(page, fileName) {
  const filePath = path.join(outputDir, fileName);
  await page.screenshot({ path: filePath, fullPage: true });
  result.evidence.screenshots.push(filePath);
}

async function waitForMainItem(page) {
  await page.waitForFunction(
    ({ gridCode, value }) =>
      ReactAPI.getComponentAPI("SupDataGrid")
        .APIs(gridCode)
        .getDatagridData()
        .some((item) => item.itemNumber === value),
    { gridCode: mainItemGrid, value: itemNumber },
    { timeout: 30000 }
  );
}

async function selectMainItem(page) {
  await waitForMainItem(page);
  return page.evaluate(
    ({ gridCode, value }) => {
      const dataGrid = ReactAPI.getComponentAPI("SupDataGrid").APIs(gridCode);
      const row = dataGrid.getDatagridData().find((item) => item.itemNumber === value);
      dataGrid.setSelecteds(String(row.rowIndex));
      return { id: row.id, rowIndex: row.rowIndex, isRun: row.isRun, valid: row.valid };
    },
    { gridCode: mainItemGrid, value: itemNumber }
  );
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
  const current = queryJson(itemSql());
  if (!current || current.valid !== true) {
    result.evidence.fallbackCleanup = { required: false, current };
    return;
  }
  const api = await request.newContext({ ignoreHTTPSErrors: true });
  try {
    const ticket = cleanupTicket || (await login(api));
    const response = await api.get(`${baseUrl}${deletePath}?workItemIds=${current.id}`, {
      headers: { Authorization: `Bearer ${ticket}` },
    });
    result.evidence.fallbackCleanup = {
      required: true,
      response: await readResponse(response),
      after: queryJson(itemSql()),
    };
  } finally {
    await api.dispose();
  }
}

async function main() {
  ensureDir(outputDir);
  ensureDir(path.dirname(outputPath));

  const preexisting = queryJson(
    `select json_build_object('count', count(*)) from public.mp_work_items where item_number=${sqlLiteral(
      itemNumber
    )};`
  );
  const seedRouteBefore = queryJson(routeSql());
  const seedAreaBefore = queryJson(areaSql());
  const seedItemBefore = queryJson(seedSql());
  requireAssertion("marker is unique before test", Number(preexisting.count) === 0, preexisting);
  requireAssertion(
    "seed route, area, item, and input standard are available",
    seedRouteBefore &&
      seedRouteBefore.valid === true &&
      seedAreaBefore &&
      seedAreaBefore.valid === true &&
      Number(seedAreaBefore.workGroupId) === Number(seedRouteBefore.id) &&
      seedItemBefore &&
      seedItemBefore.valid === true &&
      Number(seedItemBefore.routeId) === Number(seedRouteBefore.id) &&
      Number(seedItemBefore.workId) === Number(seedAreaBefore.id) &&
      Number(seedItemBefore.inputStandardId) > 0,
    { seedRouteBefore, seedAreaBefore, seedItemBefore }
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
    "PATROL route/area/item page loads",
    navigation && navigation.status() === 200 && !visibleErrorPattern.test(initialBody),
    { status: navigation && navigation.status(), body: initialBody.slice(0, 1000) }
  );
  await loadAndSelectRouteArea(page);

  let editor = await openItemEditor(page);
  const editorBody = await editor.frame.locator("body").innerText();
  const functions = await editor.frame.evaluate(() => ({
    addNewRow: typeof window.addNewRow,
    insertLineClick: typeof window.insertLineClick,
    workItemRef: typeof window.workItemRef,
    onsave: typeof window.PATROL_patrolRoute_workArea_onsave,
  }));
  requireAssertion(
    "item editor loads restored actions without visible errors",
    !visibleErrorPattern.test(editorBody) && Object.values(functions).every((type) => type === "function"),
    { body: editorBody.slice(0, 1200), functions }
  );
  const editorMore = await openMore(editor.frame, "");
  const editorMoreText = await editorMore.menu.innerText();
  await editorMore.more.click();
  requireAssertion(
    "item editor More menu uses business labels",
    editorMoreText.includes("插行") &&
      editorMoreText.includes("上移") &&
      editorMoreText.includes("下移") &&
      !rawI18nPattern.test(editorMoreText),
    editorMoreText
  );
  const rowActions = await exerciseEditorRowActions(editor.frame);
  requireAssertion(
    "item editor add/insert/move/delete controls change rows and leave seed intact",
    rowActions.afterInsert.count === rowActions.before + 2 &&
      rowActions.afterDown !== rowActions.afterUp &&
      rowActions.afterDelete === rowActions.before,
    rowActions
  );

  const populated = await populateMarkerRow(editor.frame);
  requireAssertion(
    "new item row receives route, area, input standard, and marker fields",
    populated.content === marker &&
      populated.itemNumber === itemNumber &&
      populated.routeId &&
      Number(populated.routeId.id) === Number(seedRouteBefore.id) &&
      populated.workId &&
      Number(populated.workId.id) === Number(seedAreaBefore.id) &&
      populated.inputStandardId &&
      Number(populated.inputStandardId.id) === Number(seedItemBefore.inputStandardId),
    populated
  );
  await captureScreenshot(page, "01-item-add-before-save.png");
  const addResponse = await responseAfter(
    page,
    (response) => response.url().includes(savePath) && response.request().method() === "POST",
    () => clickDialogSave(page)
  );
  await editor.iframe.waitFor({ state: "detached", timeout: 30000 });
  const inserted = queryJson(itemSql());
  requireAssertion(
    "item add API succeeds",
    addResponse.status === 200 && addResponse.json && addResponse.json.code === 200,
    addResponse
  );
  requireAssertion(
    "item add persists all critical PostgreSQL fields",
    inserted &&
      inserted.valid === true &&
      inserted.isRun === true &&
      inserted.content === marker &&
      inserted.part === `${marker}_PART` &&
      inserted.claim === `${marker}_CLAIM` &&
      inserted.defaultVal === "12.34" &&
      inserted.isAutoJudge === true &&
      inserted.normalRange === "≥10|≤20" &&
      Number(inserted.lowerLimit) === 0 &&
      Number(inserted.upperLimit) === 100 &&
      inserted.isPhone === true &&
      inserted.isConclusionModify === true &&
      Number(inserted.inputStandardId) === Number(seedItemBefore.inputStandardId) &&
      Number(inserted.routeId) === Number(seedRouteBefore.id) &&
      Number(inserted.workId) === Number(seedAreaBefore.id) &&
      Number(inserted.cid) === Number(seedRouteBefore.cid),
    inserted
  );
  addOperation(
    "新增巡检项",
    savePath,
    "POST",
    requestPayload(savePath, "POST"),
    addResponse,
    itemSql(),
    inserted,
    "PASS"
  );

  await loadAndSelectRouteArea(page);
  editor = await openItemEditor(page);
  const edited = await editMarkerRow(editor.frame);
  requireAssertion(
    "item edit row is loaded and changed in the real editor",
    edited.content === updatedContent && edited.remark === updatedRemark && edited.isPhone === false,
    edited
  );
  const updateResponse = await responseAfter(
    page,
    (response) => response.url().includes(savePath) && response.request().method() === "POST",
    () => clickDialogSave(page)
  );
  await editor.iframe.waitFor({ state: "detached", timeout: 30000 });
  const updated = queryJson(itemSql());
  requireAssertion(
    "item update API succeeds",
    updateResponse.status === 200 && updateResponse.json && updateResponse.json.code === 200,
    updateResponse
  );
  requireAssertion(
    "item update changes PostgreSQL fields and version",
    updated &&
      updated.content === updatedContent &&
      updated.remark === updatedRemark &&
      updated.isPhone === false &&
      Number(updated.version) > Number(inserted.version),
    { before: inserted, after: updated }
  );
  addOperation(
    "修改巡检项",
    savePath,
    "POST",
    requestPayload(savePath, "POST"),
    updateResponse,
    itemSql(),
    updated,
    "PASS"
  );

  await loadAndSelectRouteArea(page);
  const selectedForStop = await selectMainItem(page);
  const stopResponse = await responseAfter(
    page,
    (response) =>
      response.url().includes(statePath) &&
      response.url().includes("tableType=workItem") &&
      response.url().includes("itemState=0"),
    () => toolbar(page, "巡检项").locator("#btn-stop").click()
  );
  const stopped = queryJson(itemSql());
  requireAssertion(
    "item stop action persists is_run=false",
    selectedForStop.id === stopped.id &&
      stopResponse.status === 200 &&
      stopResponse.json &&
      stopResponse.json.code === 200 &&
      stopped.isRun === false,
    { selectedForStop, stopResponse, stopped }
  );
  addOperation(
    "停用巡检项",
    statePath,
    "GET",
    null,
    stopResponse,
    itemSql(),
    stopped,
    "PASS"
  );

  await loadAndSelectRouteArea(page);
  const selectedForRun = await selectMainItem(page);
  const runResponse = await responseAfter(
    page,
    (response) =>
      response.url().includes(statePath) &&
      response.url().includes("tableType=workItem") &&
      response.url().includes("itemState=1"),
    () => toolbar(page, "巡检项").locator("#btn-start").click()
  );
  const running = queryJson(itemSql());
  requireAssertion(
    "item run action persists is_run=true",
    selectedForRun.id === running.id &&
      runResponse.status === 200 &&
      runResponse.json &&
      runResponse.json.code === 200 &&
      running.isRun === true,
    { selectedForRun, runResponse, running }
  );
  addOperation(
    "启用巡检项",
    statePath,
    "GET",
    null,
    runResponse,
    itemSql(),
    running,
    "PASS"
  );

  await loadAndSelectRouteArea(page);
  editor = await openItemEditor(page);
  await editMarkerRow(editor.frame);
  await clickEditorButton(editor.frame, "#btn-delete");
  const markerPresentAfterDelete = await editor.frame.evaluate(
    ({ gridCode, value }) =>
      ReactAPI.getComponentAPI("SupDataGrid")
        .APIs(gridCode)
        .getDatagridData()
        .some((row) => row.itemNumber === value),
    { gridCode: editorItemGrid, value: itemNumber }
  );
  requireAssertion(
    "item delete removes the selected row before save",
    markerPresentAfterDelete === false,
    { markerPresentAfterDelete, itemId: running.id }
  );
  await captureScreenshot(page, "02-item-delete-before-save.png");
  const [deleteResponseRaw, deleteSaveResponseRaw] = await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes(deletePath) && response.request().method() === "GET",
      { timeout: pageTimeoutMs }
    ),
    page.waitForResponse(
      (response) => response.url().includes(savePath) && response.request().method() === "POST",
      { timeout: pageTimeoutMs }
    ),
    clickDialogSave(page),
  ]);
  const deleteResponse = await readResponse(deleteResponseRaw);
  const deleteSaveResponse = await readResponse(deleteSaveResponseRaw);
  await editor.iframe.waitFor({ state: "detached", timeout: 30000 });
  const deleted = queryJson(itemSql());
  requireAssertion(
    "item delete API and parent save succeed",
    deleteResponse.status === 200 &&
      deleteResponse.json &&
      deleteResponse.json.code === 200 &&
      deleteSaveResponse.status === 200 &&
      deleteSaveResponse.json &&
      deleteSaveResponse.json.code === 200,
    { deleteResponse, deleteSaveResponse }
  );
  requireAssertion("item delete persists valid=false", deleted && deleted.valid === false, deleted);
  addOperation(
    "删除巡检项",
    deletePath,
    "GET",
    null,
    deleteResponse,
    itemSql(),
    deleted,
    "PASS"
  );

  const seedRouteAfter = queryJson(routeSql());
  const seedAreaAfter = queryJson(areaSql());
  const seedItemAfter = queryJson(seedSql());
  requireAssertion(
    "seed route, area, and item remain valid",
    seedRouteAfter &&
      seedRouteAfter.valid === true &&
      Number(seedRouteAfter.id) === Number(seedRouteBefore.id) &&
      seedAreaAfter &&
      seedAreaAfter.valid === true &&
      Number(seedAreaAfter.id) === Number(seedAreaBefore.id) &&
      seedItemAfter &&
      seedItemAfter.valid === true &&
      Number(seedItemAfter.id) === Number(seedItemBefore.id),
    { seedRouteBefore, seedRouteAfter, seedAreaBefore, seedAreaAfter, seedItemBefore, seedItemAfter }
  );
  const finalBody = await page.locator("body").innerText();
  requireAssertion(
    "no visible system/database exception remains",
    !visibleErrorPattern.test(finalBody),
    finalBody.slice(0, 1500)
  );
  const badResponses = result.evidence.responses.filter((item) => item.status >= 500);
  requireAssertion("PATROL requests have no HTTP 5xx", badResponses.length === 0, badResponses);
  requireAssertion(
    "browser has no console/page/request failures",
    result.evidence.consoleErrors.length === 0 &&
      result.evidence.pageErrors.length === 0 &&
      result.evidence.requestFailures.length === 0,
    {
      consoleErrors: result.evidence.consoleErrors,
      pageErrors: result.evidence.pageErrors,
      requestFailures: result.evidence.requestFailures,
    }
  );
  await captureScreenshot(page, "03-item-final.png");

  result.summary.status = result.summary.fail === 0 ? "PASS" : "FAIL";
  await browser.close();
  activeBrowser = null;
}

async function finish() {
  if (activeBrowser) {
    await activeBrowser.close().catch(() => {});
    activeBrowser = null;
  }
  ensureDir(path.dirname(outputPath));
  fs.writeFileSync(outputPath, `${JSON.stringify(result, null, 2)}\n`, "utf8");
  process.stdout.write(`${JSON.stringify(result.summary)}\n${outputPath}\n`);
}

main()
  .catch(async (error) => {
    result.error = error && error.stack ? error.stack : String(error);
    result.summary.status = "FAIL";
    result.summary.fail += 1;
    await fallbackCleanup().catch((cleanupError) => {
      result.evidence.fallbackCleanup = {
        required: true,
        error: cleanupError && cleanupError.stack ? cleanupError.stack : String(cleanupError),
      };
    });
    process.exitCode = 1;
  })
  .finally(finish);
