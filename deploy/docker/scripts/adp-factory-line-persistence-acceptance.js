#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { execFileSync } = require("child_process");
const { chromium } = require("playwright");

const generatedAt = new Date();
const stamp = generatedAt.toISOString().replace(/[-:T.Z]/g, "").slice(0, 14);
const baseUrl = (process.env.ADP_BASE_URL || "http://10.11.100.17:18080").replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD || "123456";
const marker = process.env.ADP_E2E_MARKER || `ADP_E2E_${stamp}_FACTORY_LINE`;
const lineName = process.env.ADP_FACTORY_LINE_NAME || `果糖产线验收_${stamp}`;
const headless = process.env.ADP_HEADLESS !== "false";
const pageTimeoutMs = Number(process.env.ADP_PAGE_TIMEOUT_MS || 90000);
const browserExecutable =
  process.env.ADP_BROWSER_EXECUTABLE || "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";
const outputDir =
  process.env.ADP_OUTPUT_DIR || path.join("/tmp", `adp-factory-line-persistence-${stamp}`);
const outputPath =
  process.env.ADP_FACTORY_LINE_PERSISTENCE_OUTPUT ||
  path.join(outputDir, "factory-line-persistence.json");

const dbSshTarget = process.env.ADP_DB_SSH_TARGET || "v6@10.11.100.17";
const dbContainer = process.env.ADP_DB_CONTAINER || "adp-mes-newbase-postgres-1";
const dbName = process.env.ADP_DB_NAME || "adp";
const dbUser = process.env.ADP_DB_USER || "adp";
const nodeTypeId = process.env.ADP_FACTORY_LINE_NODE_TYPE_ID || "9100000000000000";
const nodeTypeCode = "004";
const nodeTypeName = "生产线";

const listRoute =
  "/msService/HierarchicalMod/factoryModel/factoryModel/factoryTreeList" +
  "?workFlowMenuCode=HierarchicalMod_1.0.0_factoryModel_factoryTreeList&openType=page";
const factoryModelMenuSelector = "#menu_factoryModel";
const factoryArchitectureMenuSelector = "#menu_factoryJG";
const factoryTreeMenuSelector =
  'li[code="HierarchicalMod_1.0.0_factoryModel_factoryTreeList"] .v3_submenu_text';
const submitPath = "/msService/HierarchicalMod/factoryModel/factoryModel/factoryEdit/submit";
const pickerRoute =
  "/msService/HierarchicalMod/factoryModel/factoryModel/factoryLineRef2" +
  "?refKey=factoryModel.name&fromViewCode=WOM_1.0.0_produceTask_makeTaskEdit" +
  "&closePage=true&crossCompanyFlag=false&multiSelect=false&openType=frame";
const visibleErrorPattern =
  /(数据库操作异常|系统错误|系统异常|SQLGrammarException|could not extract ResultSet|column .* does not exist|relation .* does not exist|500 INTERNAL|\b[\w.]+Exception(?::|\s+at\b))/i;

const result = {
  generatedAt: generatedAt.toISOString(),
  repoCommit: process.env.ADP_REPO_COMMIT || "WORKTREE",
  database: "PostgreSQL",
  marker,
  lineName,
  environment: {
    baseUrl,
    route: listRoute,
    submitPath,
    pickerRoute,
    dbSshTarget,
    dbContainer,
    table: "public.hm_factory_models",
    retainedFixture: true,
  },
  summary: { status: "RUNNING", assertions: 0, pass: 0, fail: 0 },
  assertions: [],
  operation: null,
  persistence: null,
  evidence: {
    requests: [],
    responses: [],
    consoleErrors: [],
    pageErrors: [],
    requestFailures: [],
    screenshots: [],
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
  const rows = output.split(/\r?\n/).filter(Boolean);
  return rows.length ? JSON.parse(rows[rows.length - 1]) : null;
}

function rootSql() {
  return `
select json_build_object(
  'id', id::text,
  'code', code,
  'name', name,
  'parentId', parent_id::text,
  'parentid', parentid::text,
  'companyId', cid::text,
  'valid', valid,
  'leaf', leaf
) from public.hm_factory_models
where valid is true and parent_id = -1
order by sort nulls last, id
limit 1;
`;
}

function lineSql() {
  return `
select json_build_object(
  'id', id::text,
  'version', version,
  'code', code,
  'name', name,
  'parentId', parent_id::text,
  'parentid', parentid::text,
  'nodeTypeId', node_type_id::text,
  'departmentId', department_id::text,
  'layNo', lay_no,
  'layRec', lay_rec,
  'fullPathName', full_path_name,
  'valid', valid,
  'leaf', leaf,
  'companyId', cid::text
) from public.hm_factory_models
where code = ${sqlLiteral(marker)}
order by id desc
limit 1;
`;
}

function recordAssertion(name, pass, evidence) {
  result.summary.assertions += 1;
  if (pass) {
    result.summary.pass += 1;
  } else {
    result.summary.fail += 1;
  }
  result.assertions.push({ name, status: pass ? "PASS" : "FAIL", evidence });
  if (!pass) {
    throw new Error(`${name}: ${JSON.stringify(evidence)}`);
  }
}

async function readResponse(response) {
  const text = await response.text();
  let json = null;
  try {
    json = JSON.parse(text);
  } catch (_error) {
    // Keep the raw body as evidence.
  }
  return {
    method: response.request().method(),
    url: response.url(),
    status: response.status(),
    json,
    text: text.slice(0, 4000),
  };
}

async function capture(page, filename) {
  const target = path.join(outputDir, filename);
  await page.screenshot({ path: target, fullPage: true });
  result.evidence.screenshots.push(target);
}

function attachEvidence(page) {
  page.on("console", (message) => {
    if (message.type() === "error") {
      result.evidence.consoleErrors.push({
        text: message.text().slice(0, 1000),
        location: message.location(),
      });
    }
  });
  page.on("pageerror", (error) => result.evidence.pageErrors.push(error.message.slice(0, 1000)));
  page.on("requestfailed", (request) => {
    if (/HierarchicalMod|baseService\/view|WOM/.test(request.url())) {
      result.evidence.requestFailures.push({
        method: request.method(),
        url: request.url(),
        failure: request.failure() && request.failure().errorText,
      });
    }
  });
  page.on("request", (request) => {
    if (/HierarchicalMod|baseService\/view|WOM/.test(request.url())) {
      result.evidence.requests.push({
        method: request.method(),
        url: request.url(),
        postData: request.postData() ? request.postData().slice(0, 12000) : null,
      });
    }
  });
  page.on("response", (response) => {
    if (/HierarchicalMod|baseService\/view|WOM/.test(response.url())) {
      result.evidence.responses.push({
        method: response.request().method(),
        url: response.url(),
        status: response.status(),
      });
    }
  });
}

async function login(page) {
  await page.goto(`${baseUrl}/login.html`, {
    waitUntil: "domcontentloaded",
    timeout: pageTimeoutMs,
  });
  await page.locator("#username").fill(username);
  await page.locator("#password2").fill(password);
  await page.getByRole("button", { name: "登录", exact: true }).click({ noWaitAfter: true });
  await page.waitForFunction(() => !window.location.pathname.includes("login"), null, {
    timeout: pageTimeoutMs,
  });
  await page.waitForTimeout(500);
}

async function waitForRouteReady(requestContext, url, timeoutMs) {
  const deadline = Date.now() + timeoutMs;
  let lastStatus = null;
  let lastError = null;
  while (Date.now() < deadline) {
    try {
      const response = await requestContext.get(url, {
        failOnStatusCode: false,
        timeout: Math.min(15000, Math.max(1000, deadline - Date.now())),
      });
      lastStatus = response.status();
      await response.dispose();
      if (lastStatus >= 200 && lastStatus < 400) {
        return lastStatus;
      }
    } catch (error) {
      lastError = error && error.message ? error.message : String(error);
    }
    await new Promise((resolve) => setTimeout(resolve, 3000));
  }
  throw new Error(
    `route did not become ready: ${url}; lastStatus=${lastStatus}; lastError=${lastError}`
  );
}

async function main() {
  ensureDir(outputDir);
  const root = queryJson(rootSql());
  recordAssertion(
    "factory tree has one usable top-level root",
    Boolean(root && root.valid === true && root.parentId === "-1" && root.id),
    root
  );

  const launchOptions = { headless };
  if (browserExecutable && fs.existsSync(browserExecutable)) {
    launchOptions.executablePath = browserExecutable;
  }
  const browser = await chromium.launch(launchOptions);
  try {
    const context = await browser.newContext({
      viewport: { width: 1680, height: 1050 },
      ignoreHTTPSErrors: true,
    });
    context.on("page", (openedPage) => attachEvidence(openedPage));
    const page = await context.newPage();
    page.setDefaultTimeout(pageTimeoutMs);
    page.setDefaultNavigationTimeout(pageTimeoutMs);
    await login(page);
    await waitForRouteReady(context.request, `${baseUrl}${listRoute}`, pageTimeoutMs);

    await page.locator(factoryModelMenuSelector).waitFor({ state: "visible" });
    await page.locator(factoryModelMenuSelector).click();
    const factoryTreeMenu = page.locator(factoryTreeMenuSelector);
    if (!(await factoryTreeMenu.isVisible())) {
      await page.locator(`${factoryArchitectureMenuSelector} .v3_submenu_btn_circle`).click();
    }
    await factoryTreeMenu.waitFor({ state: "visible" });
    const navigationPromise = page.waitForResponse(
      (response) =>
        response.url().includes("/factoryTreeList") &&
        response.request().method().toUpperCase() === "GET",
      { timeout: pageTimeoutMs }
    );
    await factoryTreeMenu.click();
    const navigation = await navigationPromise;
    const listIframe = page.locator('iframe[src*="/factoryTreeList"]').last();
    await listIframe.waitFor({ state: "visible" });
    const listFrame = page.frames().find((frame) => frame.url().includes("/factoryTreeList"));
    if (!listFrame) {
      throw new Error("factory tree workbench frame was not attached");
    }
    await listFrame.locator(".ant-tree-title").last().waitFor({ state: "visible" });
    const listBody = await listFrame.locator("body").innerText();
    recordAssertion(
      "factory architecture workbench menu renders tree and CRUD toolbar",
      Boolean(
        navigation &&
          navigation.status() === 200 &&
          listBody.includes("新增") &&
          listBody.includes("修改") &&
          listBody.includes("删除") &&
          !visibleErrorPattern.test(listBody)
      ),
      { status: navigation && navigation.status(), body: listBody.slice(0, 1800) }
    );

    await page.waitForTimeout(1000);
    if ((await listFrame.locator(".ant-tree-title").count()) === 1) {
      const switcher = listFrame.locator(".ant-tree-switcher").first();
      const switcherClass = (await switcher.getAttribute("class")) || "";
      if (switcherClass.includes("close")) {
        await switcher.click();
      }
      await listFrame.waitForFunction(
        () => document.querySelectorAll(".ant-tree-title").length > 1,
        null,
        { timeout: pageTimeoutMs }
      );
    }
    const treeTitleCount = await listFrame.locator(".ant-tree-title").count();
    const treeTitleTexts = (
      await listFrame.locator(".ant-tree-title").allTextContents()
    ).map((value) => value.trim());
    recordAssertion(
      "factory tree renders configured root and PostgreSQL model names",
      treeTitleTexts.includes("默认集团") &&
        treeTitleTexts.includes(root.name) &&
        !treeTitleTexts.includes("---"),
      { treeTitleCount, treeTitleTexts, expectedModelName: root.name }
    );
    let selectedTreeNode = null;
    for (let index = 0; index < treeTitleCount; index += 1) {
      await listFrame.locator(".ant-tree-title").nth(index).click();
      await page.waitForTimeout(250);
      selectedTreeNode = await listFrame.evaluate(() =>
        window.ReactAPI.getComponentAPI("NavTree")
          .APIs("HierarchicalMod_1.0.0_factoryModel_factoryTreeList_factoryModel_nt")
          .getSelectedTreeNode()
      );
      const selectedId =
        selectedTreeNode &&
        (selectedTreeNode.id || selectedTreeNode.key || selectedTreeNode.originId);
      if (String(selectedId) === root.id) {
        break;
      }
    }
    recordAssertion(
      "real PostgreSQL root is selected instead of the synthetic tree wrapper",
      Boolean(
        selectedTreeNode &&
          String(selectedTreeNode.id || selectedTreeNode.key || selectedTreeNode.originId) === root.id
      ),
      { treeTitleCount, selectedTreeNode, root }
    );
    await listFrame.waitForFunction(
      (rootCode) => (document.body.innerText || "").includes(rootCode),
      root.code,
      { timeout: pageTimeoutMs }
    );
    const selectedRootBody = await listFrame.locator("body").innerText();
    recordAssertion(
      "selecting the real tree root loads its PostgreSQL-backed row",
      selectedRootBody.includes(root.code) &&
        selectedRootBody.includes(root.name) &&
        !visibleErrorPattern.test(selectedRootBody),
      selectedRootBody.slice(0, 1800)
    );
    await page.waitForTimeout(1000);
    let addButton = listFrame.locator("#btn-addNode .sup-datagrid-button-item");
    if (!(await addButton.count())) {
      addButton = listFrame.locator("#btn-addNode");
    }
    const editorPagePromise = context.waitForEvent("page", { timeout: pageTimeoutMs });
    await addButton.click({ force: true, noWaitAfter: true });
    const editor = await editorPagePromise;
    editor.setDefaultTimeout(pageTimeoutMs);
    editor.setDefaultNavigationTimeout(pageTimeoutMs);
    await editor.waitForLoadState("domcontentloaded");
    recordAssertion(
      "factory tree add action opens the real factory edit page",
      editor.url().includes("/factoryEdit?") &&
        new URL(editor.url()).searchParams.get("parentId") === root.id,
      editor.url()
    );
    await editor.locator("#edit_form_factoryModel\\.code").waitFor({ state: "visible" });
    const editorBody = await editor.locator("body").innerText();
    recordAssertion(
      "factory edit runtime form renders required fields",
      editorBody.includes("节点编码") &&
      editorBody.includes("节点名称") &&
        editorBody.includes("节点类型") &&
        editorBody.includes("装置") &&
        !editorBody.includes("HierarchicalMod.tabname.randon1618564480544.flag") &&
        editorBody.includes("保存") &&
        !visibleErrorPattern.test(editorBody),
      editorBody.slice(0, 1800)
    );

    await editor.locator("#edit_form_factoryModel\\.code").fill(marker);
    await editor.locator("#edit_form_factoryModel\\.name").fill(lineName);
    await editor.locator("#factoryModel_nodeTypeId_name .btn-search").click({ noWaitAfter: true });
    const nodeTypeIframe = editor.locator('iframe[src*="/nodeTypeRef?"]').last();
    await nodeTypeIframe.waitFor({ state: "visible" });
    const nodeTypeFrame = nodeTypeIframe.contentFrame();
    await nodeTypeFrame.getByText(nodeTypeName, { exact: true }).waitFor({ state: "visible" });
    const nodeTypeBody = await nodeTypeFrame.locator("body").innerText();
    recordAssertion(
      "production-line node-type picker renders its business row",
      nodeTypeBody.includes(nodeTypeCode) &&
        nodeTypeBody.includes(nodeTypeName) &&
        !visibleErrorPattern.test(nodeTypeBody),
      nodeTypeBody.slice(0, 1200)
    );
    await nodeTypeFrame.getByText(nodeTypeName, { exact: true }).click();
    await editor.getByRole("button", { name: "选 择", exact: true }).click();
    await editor.waitForFunction(
      ({ typeId, typeCode }) => {
        const values = window.ReactAPI.getComponentAPI("Reference")
          .APIs("factoryModel.nodeTypeId.name")
          .getValue();
        return (
          Array.isArray(values) &&
          values.length === 1 &&
          String(values[0].id) === typeId &&
          values[0].code === typeCode
        );
      },
      { typeId: nodeTypeId, typeCode: nodeTypeCode },
      { timeout: pageTimeoutMs }
    );

    const selectedValues = await editor.evaluate(() => ({
      nodeType: window.ReactAPI.getComponentAPI("Reference")
        .APIs("factoryModel.nodeTypeId.name")
        .getValue(),
      department: window.ReactAPI.getComponentAPI("Reference")
        .APIs("factoryModel.departmentId.name")
        .getValue(),
      formData: window.ReactAPI.getFormData(),
    }));
    recordAssertion(
      "visible reference selection binds production-line type into the real edit form",
      Array.isArray(selectedValues.nodeType) &&
        selectedValues.nodeType.length === 1 &&
        String(selectedValues.nodeType[0].id) === nodeTypeId &&
        selectedValues.nodeType[0].code === nodeTypeCode &&
        Array.isArray(selectedValues.department) &&
        selectedValues.department.length === 0,
      selectedValues
    );

    await capture(editor, "01-factory-line-before-save.png");

    const saveButton = editor.locator("#operateBtn-save");
    await saveButton.waitFor({ state: "visible" });
    const [rawSubmitResponse] = await Promise.all([
      editor.waitForResponse(
        (response) =>
          response.url().includes(submitPath) &&
          response.request().method().toUpperCase() === "POST",
        { timeout: pageTimeoutMs }
      ),
      saveButton.click({ noWaitAfter: true }),
    ]);
    const submitResponse = await readResponse(rawSubmitResponse);
    const submitRequest = result.evidence.requests
      .filter((item) => item.url.includes(submitPath) && item.method === "POST")
      .slice(-1)[0];
    result.operation = { request: submitRequest || null, response: submitResponse };
    recordAssertion(
      "factory-line create API returns success",
      submitResponse.status === 200 &&
        submitResponse.json &&
        submitResponse.json.code === 200 &&
        submitResponse.json.data &&
        submitResponse.json.data.dealSuccessFlag !== false,
      submitResponse
    );

    const persisted = queryJson(lineSql());
    result.persistence = { verificationSql: lineSql().trim(), row: persisted };
    recordAssertion(
      "factory line persists under the selected root in PostgreSQL",
      Boolean(
        persisted &&
          persisted.valid === true &&
          persisted.code === marker &&
          persisted.name === lineName &&
          persisted.parentId === root.id &&
          persisted.nodeTypeId === nodeTypeId &&
          persisted.departmentId === null &&
          persisted.companyId === root.companyId &&
          submitResponse.json &&
          submitResponse.json.data &&
          String(submitResponse.json.data.id) === persisted.id
      ),
      {
        root,
        persisted,
        responseId:
          submitResponse.json && submitResponse.json.data
            ? String(submitResponse.json.data.id)
            : null,
      }
    );

    const pickerPage = await context.newPage();
    pickerPage.setDefaultTimeout(pageTimeoutMs);
    const pickerNavigation = await pickerPage.goto(`${baseUrl}${pickerRoute}`, {
      waitUntil: "domcontentloaded",
      timeout: pageTimeoutMs,
    });
    await pickerPage.waitForFunction(
      (expected) => (document.body.innerText || "").includes(expected),
      marker,
      { timeout: pageTimeoutMs }
    );
    const pickerBody = await pickerPage.locator("body").innerText();
    recordAssertion(
      "WOM production-line picker lists the newly persisted line",
      Boolean(
        pickerNavigation &&
          pickerNavigation.status() === 200 &&
          pickerBody.includes(marker) &&
          pickerBody.includes(lineName) &&
          pickerBody.includes(nodeTypeName) &&
          !visibleErrorPattern.test(pickerBody)
      ),
      { status: pickerNavigation && pickerNavigation.status(), body: pickerBody.slice(0, 3000) }
    );
    await capture(pickerPage, "02-wom-production-line-picker.png");
    // Saving refreshes the parent factory tree asynchronously. Let those
    // PostgreSQL-backed responses finish before closing Chromium so the test
    // client does not create misleading Broken pipe errors in FoundationMs.
    await page.waitForTimeout(3000);

    const badResponses = result.evidence.responses.filter((item) => item.status >= 400);
    recordAssertion(
      "tested factory-line flow has no console, page, request, or HTTP errors",
      result.evidence.consoleErrors.length === 0 &&
        result.evidence.pageErrors.length === 0 &&
        result.evidence.requestFailures.length === 0 &&
        badResponses.length === 0,
      {
        consoleErrors: result.evidence.consoleErrors,
        pageErrors: result.evidence.pageErrors,
        requestFailures: result.evidence.requestFailures,
        badResponses,
      }
    );
  } finally {
    await browser.close();
  }
}

async function finish() {
  try {
    await main();
    result.summary.status = "PASS";
  } catch (error) {
    result.summary.status = "FAIL";
    if (result.summary.fail === 0) {
      result.summary.assertions += 1;
      result.summary.fail += 1;
      result.assertions.push({
        name: "acceptance execution completes",
        status: "FAIL",
        evidence: error && error.message ? error.message : String(error),
      });
    }
    result.error = {
      message: error && error.message ? error.message : String(error),
      stack: error && error.stack ? error.stack.split("\n").slice(0, 20) : [],
    };
  }
  ensureDir(outputDir);
  fs.writeFileSync(outputPath, `${JSON.stringify(result, null, 2)}\n`, "utf8");
  process.stdout.write(`${JSON.stringify({ outputPath, summary: result.summary, marker }, null, 2)}\n`);
  if (result.summary.status !== "PASS") {
    process.exitCode = 1;
  }
}

finish();
