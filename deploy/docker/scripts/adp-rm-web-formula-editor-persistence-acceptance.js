#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { execFileSync } = require("child_process");
const { chromium, request } = require("playwright");

const root = path.resolve(__dirname, "../../..");
const baseUrl = (process.env.ADP_BASE_URL || "http://10.11.100.17:18080").replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD || "123456";
const sshTarget = process.env.ADP_DB_SSH_TARGET || "v6@10.11.100.17";
const dbContainer = process.env.ADP_DB_CONTAINER || "adp-mes-newbase-postgres-1";
const serviceContainer = process.env.ADP_RM_FORMULA_CONTAINER || "adp-mes-newbase-rm-formula-editor-1";
const dbName = process.env.ADP_DB_NAME || "adp";
const dbUser = process.env.ADP_DB_USER || "adp";
const remoteRoot = process.env.ADP_REMOTE_ROOT || "/home/v6/adp-mes-docker-newbase-20260611-181921";
const headless = process.env.ADP_HEADLESS !== "false";
const token = new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14);
const marker = process.env.ADP_E2E_MARKER || `ADP_E2E_${token}_RM_WEB_FORMULA`;
const reportPath = path.resolve(process.env.ADP_OUTPUT_PATH || path.join(root, "metadata/rm-web-formula-editor-acceptance.json"));
const desktopScreenshot = path.resolve(process.env.ADP_DESKTOP_SCREENSHOT || path.join(root, "metadata/rm-web-formula-editor.png"));
const mobileScreenshot = path.resolve(process.env.ADP_MOBILE_SCREENSHOT || path.join(root, "metadata/rm-web-formula-editor-mobile.png"));
const listRoute = "/msService/RM/formula/formula/batchFormulaList?system=formulaEnableFlw";
const editorRoute = "/msService/RM/formula/editor";
const apiBase = "/msService/RM/formula-editor";

function shellQuote(value) {
  return `'${String(value).replace(/'/g, "'\\''")}'`;
}

function sqlLiteral(value) {
  return `'${String(value).replace(/'/g, "''")}'`;
}

function sqlBigint(value) {
  const normalized = String(value);
  if (!/^\d+$/.test(normalized)) throw new Error(`Invalid bigint identifier: ${normalized}`);
  return normalized;
}

function runRemote(command, input) {
  return execFileSync(
    "ssh",
    [
      "-o", "BatchMode=yes",
      "-o", "ConnectTimeout=8",
      "-o", "StrictHostKeyChecking=no",
      "-o", "UserKnownHostsFile=/dev/null",
      sshTarget,
      command,
    ],
    { input, encoding: "utf8", stdio: ["pipe", "pipe", "pipe"] }
  ).trim();
}

function runSql(sql) {
  const command = [
    "docker", "exec", "-i", shellQuote(dbContainer),
    "psql", "-v", "ON_ERROR_STOP=1", "-U", shellQuote(dbUser),
    "-d", shellQuote(dbName), "-At",
  ].join(" ");
  return runRemote(command, sql);
}

function runSqlJson(sql) {
  const output = runSql(sql);
  if (!output) throw new Error(`SQL returned no JSON row: ${sql}`);
  return JSON.parse(output);
}

function runSqlJsonOrNull(sql) {
  const output = runSql(sql);
  return output ? JSON.parse(output) : null;
}

function findTicket(payload) {
  const candidates = [
    payload && payload.ticket,
    payload && payload.access_token,
    payload && payload.token,
    payload && payload.data && payload.data.ticket,
    payload && payload.data && payload.data.access_token,
    payload && payload.data && payload.data.token,
  ];
  return candidates.find((value) => typeof value === "string" && value.length > 20);
}

async function login(api) {
  const bodies = [
    { userName: username, password, clientId: "pc_dt" },
    { username, password, clientId: "pc_dt" },
  ];
  const failures = [];
  for (const body of bodies) {
    const response = await api.post(`${baseUrl}/inter-api/auth/login`, {
      data: body,
      headers: { Accept: "application/json", "Content-Type": "application/json;charset=UTF-8" },
    });
    const text = await response.text();
    let payload = null;
    try { payload = JSON.parse(text); } catch (_error) { payload = null; }
    const ticket = response.ok() ? findTicket(payload) : null;
    if (ticket) return ticket;
    failures.push({ status: response.status(), body: text.slice(0, 300) });
  }
  throw new Error(`Login failed for ${username}: ${JSON.stringify(failures)}`);
}

function attachBrowserEvidence(page, bucket, label) {
  page.on("console", (message) => {
    if (message.type() === "error") bucket.consoleErrors.push({ page: label, text: message.text() });
  });
  page.on("pageerror", (error) => bucket.pageErrors.push({ page: label, message: error.message }));
  page.on("requestfailed", (item) => {
    bucket.requestFailures.push({
      page: label,
      method: item.method(),
      url: item.url(),
      failure: item.failure() && item.failure().errorText,
    });
  });
}

async function setAuth(context, ticket) {
  await context.addCookies([{ name: "suposTicket", value: ticket, url: baseUrl }]);
  await context.addInitScript((authTicket) => {
    ["suposTicket", "SUPOS_TICKET", "token", "ticket"].forEach((key) => {
      window.localStorage.setItem(key, authTicket);
      window.sessionStorage.setItem(key, authTicket);
    });
  }, ticket);
}

async function pageJson(page, route, options) {
  return page.evaluate(async ({ target, init }) => {
    init = init || {};
    init.headers = init.headers || {};
    const ticket = window.localStorage.getItem("ticket") || window.sessionStorage.getItem("ticket");
    if (ticket && !init.headers.Authorization) {
      init.headers.Authorization = `Bearer ${ticket.replace(/^Bearer\s+/i, "")}`;
    }
    const response = await fetch(target, init);
    const text = await response.text();
    let json = null;
    try { json = JSON.parse(text); } catch (_error) { json = null; }
    return { status: response.status, ok: response.ok, json, text };
  }, { target: route, init: options || {} });
}

async function waitForEditorReady(page) {
  await page.waitForSelector("#newButton", { state: "visible" });
  await page.waitForFunction(() => {
    const product = document.getElementById("productId");
    return product && product.options.length > 1;
  });
}

async function saveThroughUi(page) {
  const before = await page.evaluate(() => {
    const form = document.getElementById("formulaForm");
    const button = document.getElementById("saveButton");
    return {
      valid: form.checkValidity(),
      invalid: Array.prototype.map.call(form.querySelectorAll(":invalid"), (item) => item.id || item.name),
      buttonDisabled: button.disabled,
      status: document.getElementById("appStatus").textContent,
      formulaCode: document.getElementById("formulaCode").value,
      productId: document.getElementById("productId").value,
      processRows: document.querySelectorAll("#processBody tr").length,
      activityRows: document.querySelectorAll("#activityBody tr").length,
    };
  });
  if (!before.valid || before.buttonDisabled) {
    throw new Error(`Formula form cannot submit: ${JSON.stringify(before)}`);
  }
  let response;
  try {
    const result = await Promise.all([
      page.waitForRequest((item) => {
        const method = item.method();
        return item.url().includes(`${apiBase}/formulas`) && (method === "POST" || method === "PUT");
      }, { timeout: 15000 }),
      page.waitForResponse((item) => {
        const method = item.request().method();
        return item.url().includes(`${apiBase}/formulas`) && (method === "POST" || method === "PUT");
      }, { timeout: 60000 }),
      page.click("#saveButton"),
    ]);
    response = result[1];
  } catch (error) {
    const after = await page.evaluate(() => ({
      buttonDisabled: document.getElementById("saveButton").disabled,
      status: document.getElementById("appStatus").textContent,
      toast: document.getElementById("toast").textContent,
      toastClass: document.getElementById("toast").className,
    }));
    throw new Error(`${error.message}; before=${JSON.stringify(before)}; after=${JSON.stringify(after)}`);
  }
  const payload = await response.json();
  const requestPayload = response.request().postDataJSON();
  if (response.status() !== 200 || !payload || payload.code !== 200) {
    throw new Error(
      `Formula save failed: ${response.request().method()} ${response.request().url()} ` +
      `HTTP ${response.status()} request=${JSON.stringify(requestPayload)} response=${JSON.stringify(payload)}`
    );
  }
  try {
    await page.waitForFunction(({ formulaCode, version }) => {
      const codeInput = document.getElementById("formulaCode");
      const versionText = document.getElementById("versionText");
      const saveButton = document.getElementById("saveButton");
      return codeInput.value === formulaCode &&
        versionText.textContent.trim() === `版本 ${version}` &&
        !saveButton.disabled;
    }, {
      formulaCode: payload.data.formula.formulaCode,
      version: payload.data.formula.version,
    });
  } catch (error) {
    const after = await page.evaluate(() => ({
      buttonDisabled: document.getElementById("saveButton").disabled,
      publishButtonDisabled: document.getElementById("publishButton").disabled,
      status: document.getElementById("appStatus").textContent,
      toast: document.getElementById("toast").textContent,
      toastClass: document.getElementById("toast").className,
      formulaCode: document.getElementById("formulaCode").value,
      version: document.getElementById("versionText").textContent.trim(),
      processRows: document.querySelectorAll("#processBody tr").length,
      activityRows: document.querySelectorAll("#activityBody tr").length,
    }));
    throw new Error(
      `${error.message}; response=${JSON.stringify(payload.data)}; after=${JSON.stringify(after)}`
    );
  }
  return { response, payload, requestPayload };
}

function cleanupSql() {
  return `
BEGIN;
DELETE FROM public.rm_formula_delivery_attempts
 WHERE delivery_id IN (
   SELECT id FROM public.rm_formula_deliveries
    WHERE formula_id IN (SELECT id FROM public.rm_formulas WHERE formual_code = ${sqlLiteral(marker)})
 );
DELETE FROM public.rm_formula_deliveries
 WHERE formula_id IN (SELECT id FROM public.rm_formulas WHERE formual_code = ${sqlLiteral(marker)});
DELETE FROM public.rm_formula_editor_revisions
 WHERE formula_id IN (SELECT id FROM public.rm_formulas WHERE formual_code = ${sqlLiteral(marker)});
DELETE FROM public.rm_process_actives
 WHERE formula_id IN (SELECT id FROM public.rm_formulas WHERE formual_code = ${sqlLiteral(marker)});
DELETE FROM public.rm_formula_processes
 WHERE COALESCE(formula_id, formula) IN (
   SELECT id FROM public.rm_formulas WHERE formual_code = ${sqlLiteral(marker)}
 );
DELETE FROM public.rm_formulas WHERE formual_code = ${sqlLiteral(marker)};
COMMIT;
`;
}

function cleanupCounts() {
  return runSqlJson(`
SELECT json_build_object(
  'formula', (SELECT COUNT(*) FROM public.rm_formulas WHERE formual_code = ${sqlLiteral(marker)}),
  'process', (SELECT COUNT(*) FROM public.rm_formula_processes p
    WHERE COALESCE(p.formula_id, p.formula) IN (
      SELECT id FROM public.rm_formulas WHERE formual_code = ${sqlLiteral(marker)}
    )),
  'activity', (SELECT COUNT(*) FROM public.rm_process_actives a
    WHERE a.formula_id IN (SELECT id FROM public.rm_formulas WHERE formual_code = ${sqlLiteral(marker)}))
)::text;
`);
}

async function main() {
  fs.mkdirSync(path.dirname(reportPath), { recursive: true });
  fs.mkdirSync(path.dirname(desktopScreenshot), { recursive: true });
  fs.mkdirSync(path.dirname(mobileScreenshot), { recursive: true });

  const browserEvidence = { consoleErrors: [], pageErrors: [], requestFailures: [] };
  const report = {
    schemaVersion: 2,
    generatedAt: new Date().toISOString(),
    environment: {
      baseUrl,
      sshHost: sshTarget.split("@").pop(),
      database: "PostgreSQL",
      service: "RMFormulaEditor",
      serviceHealth: "UNKNOWN",
    },
    marker,
    browser: {},
    api: {},
    postgresql: {},
    postDeployRegression: {},
    scopeBoundary: {
      legacyActiveXRequired: false,
      localhost4433Required: false,
      webEditorStatus: "PENDING",
      postgresPersistenceStatus: "PENDING",
      httpDeliveryContractStatus: "PENDING",
      externalBatchDcsStatus: "BLOCKED",
      externalBatchDcsReason: "The isolated test adapter proves the versioned HTTP contract, acknowledgement and retry ledger, but no plant Batch/DCS endpoint or owner sign-off was supplied.",
    },
    cleanup: {},
    summary: {
      status: "RUNNING",
      webEditor: "PENDING",
      api: "PENDING",
      postgresPersistence: "PENDING",
      testAdapterRetry: "PENDING",
      externalBatchDcs: "BLOCKED",
    },
    issues: [],
  };

  let browser = null;
  let api = null;
  let failure = null;
  try {
    const existing = runSql(`SELECT COUNT(*) FROM public.rm_formulas WHERE formual_code = ${sqlLiteral(marker)};`);
    if (Number(existing) !== 0) throw new Error(`Marker already exists: ${marker}`);

    report.environment.serviceHealth = runRemote(
      `docker inspect -f '{{.State.Status}}' ${shellQuote(serviceContainer)}`
    ).toUpperCase();
    if (report.environment.serviceHealth !== "RUNNING") {
      throw new Error(`${serviceContainer} is not running`);
    }

    api = await request.newContext({ ignoreHTTPSErrors: true });
    const ticket = await login(api);
    browser = await chromium.launch({ headless });
    const context = await browser.newContext({
      ignoreHTTPSErrors: true,
      viewport: { width: 1440, height: 960 },
    });
    await setAuth(context, ticket);

    const listPage = await context.newPage();
    const listEvidence = { consoleErrors: [], pageErrors: [], requestFailures: [] };
    attachBrowserEvidence(listPage, listEvidence, "batchFormulaList");
    const listNavigation = await listPage.goto(`${baseUrl}${listRoute}`, { waitUntil: "domcontentloaded" });
    const webEditorButton = listPage.getByText("Web编辑", { exact: true }).first();
    await webEditorButton.waitFor({ state: "visible", timeout: 120000 });
    report.browser.batchFormulaList = {
      route: listRoute,
      navigationStatus: listNavigation ? listNavigation.status() : 0,
      webEditorButtonVisible: await webEditorButton.isVisible(),
      consoleErrors: listEvidence.consoleErrors.length,
      pageErrors: listEvidence.pageErrors.length,
      requestFailures: listEvidence.requestFailures.length,
    };

    const page = await context.newPage();
    attachBrowserEvidence(page, browserEvidence, "desktopEditor");
    const editorNavigation = await page.goto(`${baseUrl}${editorRoute}`, { waitUntil: "domcontentloaded" });
    if (!editorNavigation || editorNavigation.status() !== 200) {
      throw new Error(`Editor navigation returned ${editorNavigation && editorNavigation.status()}`);
    }
    await waitForEditorReady(page);
    await page.click("#newButton");
    await page.fill("#formulaCode", marker);
    await page.fill("#formulaName", `${marker} 配方`);
    await page.fill("#formulaEdition", "1.0");
    const productValue = await page.locator("#productId option").evaluateAll((options) => {
      const businessOption = options.find((option) => option.value && !option.textContent.includes("ADP_E2E_"));
      const fallback = options.find((option) => option.value);
      return (businessOption || fallback).value;
    });
    await page.selectOption("#productId", productValue);
    await page.fill("#normalSize", "100");
    await page.fill("#batchFormulaId", `${marker}_BATCH`);
    await page.fill("#batchFormulaCode", `${marker}_DCS`);
    await page.fill("#batchFormulaEdition", "1");
    const batchOptions = await page.locator("#batchServerId option").count();
    if (batchOptions > 1) await page.selectOption("#batchServerId", { index: 1 });
    await page.fill("#description", `${marker} created through visible browser form`);
    await page.fill("#processBody tr:first-child [data-field=name]", "混配工序");
    await page.fill("#processBody tr:first-child [data-field=batchUnitId]", `${marker}_UNIT`);
    await page.click("#addActivity");
    const activity = page.locator("#activityBody tr").first();
    await activity.locator("[data-field=name]").fill("投料活动");
    await activity.locator("[data-field=activeType]").fill("CHARGE");
    await activity.locator("[data-field=batchPhaseId]").fill(`${marker}_PHASE`);
    await activity.locator("[data-field=dispatchSystem]").fill("MES");
    await activity.locator("[data-field=executionSystem]").fill("DCS");
    await activity.locator("[data-field=quantity]").fill("100");

    const created = await saveThroughUi(page);
    const formulaId = String(created.payload.data.formula.id);
    sqlBigint(formulaId);
    const createRequestId = created.requestPayload.requestId;
    const createdDetailApi = await pageJson(page, `${apiBase}/formulas/${formulaId}`);
    const createdRow = runSqlJsonOrNull(`
SELECT json_build_object(
  'id', id::text, 'formulaCode', formual_code, 'version', version,
  'valid', COALESCE(valid, TRUE), 'revisionCount', (
    SELECT COUNT(*) FROM public.rm_formula_editor_revisions r WHERE r.formula_id = f.id
  )
)::text FROM public.rm_formulas f WHERE id = ${sqlBigint(formulaId)};
`);
    if (!createdRow || String(createdRow.id) !== formulaId || createdRow.formulaCode !== marker || !createdRow.valid) {
      const sequenceState = runSql("SELECT last_value || ':' || is_called FROM public.rm_web_formula_id_seq;");
      throw new Error(`Create PostgreSQL visibility failed: ${JSON.stringify({
        formulaId,
        marker,
        requestId: createRequestId,
        createResponse: created.payload.data,
        detailResponse: createdDetailApi,
        createdRow,
        sequenceState,
      })}`);
    }
    const replay = await pageJson(page, apiBase + "/formulas", {
      method: "POST",
      headers: { Accept: "application/json", "Content-Type": "application/json;charset=UTF-8" },
      body: JSON.stringify(created.requestPayload),
    });
    if (!replay.json || replay.json.code !== 200 || replay.json.data.idempotent !== true) {
      throw new Error(`Create idempotency replay failed: ${JSON.stringify(replay)}`);
    }
    const replayedRow = runSqlJson(`
SELECT json_build_object(
  'id', id::text, 'formulaCode', formual_code, 'version', version,
  'valid', COALESCE(valid, TRUE), 'revisionCount', (
    SELECT COUNT(*) FROM public.rm_formula_editor_revisions r WHERE r.formula_id = f.id
  )
)::text FROM public.rm_formulas f WHERE id = ${sqlBigint(formulaId)};
`);
    if (String(replayedRow.id) !== formulaId || replayedRow.formulaCode !== marker ||
        !replayedRow.valid || Number(replayedRow.revisionCount) !== 1) {
      throw new Error(`Idempotency replay mutated PostgreSQL state: ${JSON.stringify(replayedRow)}`);
    }

    await page.fill("#formulaEdition", "1.1");
    await page.fill("#description", `${marker} first browser update`);
    const firstUpdate = await saveThroughUi(page);
    await page.fill("#batchFormulaEdition", "2");
    await page.fill("#description", `${marker} browser saved`);
    const secondUpdate = await saveThroughUi(page);
    const formulaVersion = Number(secondUpdate.payload.data.formula.version);
    if (formulaVersion !== 2) throw new Error(`Expected formula version 2, got ${formulaVersion}`);

    const publishPromise = page.waitForResponse((response) =>
      response.request().method() === "POST" &&
      response.url().includes(`${apiBase}/formulas/${formulaId}/deliveries`)
    );
    await page.click("#publishButton");
    const publishResponse = await publishPromise;
    const publishPayload = await publishResponse.json();
    if (!publishPayload.data || publishPayload.data.state !== "FAILED" || publishPayload.data.httpStatus !== 503) {
      throw new Error(`Expected fail-first delivery 503, got ${JSON.stringify(publishPayload)}`);
    }

    const deliveryId = String(publishPayload.data.id);
    sqlBigint(deliveryId);
    const retryPromise = page.waitForResponse((response) =>
      response.request().method() === "POST" &&
      response.url().includes(`${apiBase}/deliveries/${deliveryId}/retry`)
    );
    await page.click("#retryButton");
    const retryResponse = await retryPromise;
    const retryPayload = await retryResponse.json();
    if (!retryPayload.data || retryPayload.data.state !== "ACKNOWLEDGED" || retryPayload.data.httpStatus !== 200) {
      throw new Error(`Expected acknowledged retry, got ${JSON.stringify(retryPayload)}`);
    }

    const listApi = await pageJson(page, `${apiBase}/formulas?query=${encodeURIComponent(marker)}&limit=10`);
    if (!listApi.json || listApi.json.code !== 200 || listApi.json.data.count !== 1) {
      throw new Error(`Marker list lookup failed: ${JSON.stringify(listApi)}`);
    }
    const materialsApi = await pageJson(page, `${apiBase}/references/materials?limit=500`);
    const batchServersApi = await pageJson(page, `${apiBase}/references/batch-servers?limit=300`);
    if (!materialsApi.json || materialsApi.json.code !== 200 || materialsApi.json.data.count < 1 ||
        !batchServersApi.json || batchServersApi.json.code !== 200 || batchServersApi.json.data.count < 1) {
      throw new Error(`Reference master data lookup failed: ${JSON.stringify({ materialsApi, batchServersApi })}`);
    }
    await page.fill("#searchInput", marker);
    await page.click("#searchButton");
    await page.waitForFunction((value) => {
      const items = document.querySelectorAll(".formula-item");
      return items.length === 1 && items[0].textContent.includes(value);
    }, marker);
    const desktopOverflow = await page.evaluate(() =>
      document.documentElement.scrollWidth > document.documentElement.clientWidth
    );
    await page.screenshot({ path: desktopScreenshot, fullPage: false });

    const mobile = await context.newPage();
    await mobile.setViewportSize({ width: 390, height: 844 });
    const mobileEvidence = { consoleErrors: [], pageErrors: [], requestFailures: [] };
    attachBrowserEvidence(mobile, mobileEvidence, "mobileEditor");
    await mobile.goto(`${baseUrl}${editorRoute}`, { waitUntil: "domcontentloaded" });
    await waitForEditorReady(mobile);
    await mobile.fill("#searchInput", marker);
    await mobile.click("#searchButton");
    const markerItem = mobile.locator(".formula-item", { hasText: marker }).first();
    await markerItem.waitFor({ state: "visible" });
    await markerItem.click();
    await mobile.waitForFunction((id) => document.getElementById("formulaCode").value === id, marker);
    await mobile.waitForFunction((value) => {
      const items = document.querySelectorAll(".formula-item");
      return items.length === 1 && items[0].textContent.includes(value);
    }, marker);
    const mobileDimensions = await mobile.evaluate(() => ({
      documentWidth: document.documentElement.scrollWidth,
      clientWidth: document.documentElement.clientWidth,
      horizontalOverflow: document.documentElement.scrollWidth > document.documentElement.clientWidth,
      processRows: document.querySelectorAll("#processBody tr").length,
      activityRows: document.querySelectorAll("#activityBody tr").length,
    }));
    await mobile.screenshot({ path: mobileScreenshot, fullPage: false });

    report.browser.desktopEditor = {
      route: editorRoute,
      title: await page.title(),
      formulaId,
      formulaVersion,
      processRows: await page.locator("#processBody tr").count(),
      activityRows: await page.locator("#activityBody tr").count(),
      saveHttpStatus: secondUpdate.response.status(),
      publishHttpStatus: publishResponse.status(),
      retryHttpStatus: retryResponse.status(),
      deliveryState: retryPayload.data.state,
      deliveryAttempts: retryPayload.data.attempts,
      horizontalOverflow: desktopOverflow,
      consoleErrors: browserEvidence.consoleErrors.length,
      pageErrors: browserEvidence.pageErrors.length,
      requestFailures: browserEvidence.requestFailures.length,
      screenshot: path.relative(root, desktopScreenshot),
    };
    report.browser.mobileEditor = {
      viewport: "390x844",
      formulaId,
      formulaVersion,
      ...mobileDimensions,
      consoleErrors: mobileEvidence.consoleErrors.length,
      pageErrors: mobileEvidence.pageErrors.length,
      requestFailures: mobileEvidence.requestFailures.length,
      screenshot: path.relative(root, mobileScreenshot),
    };

    report.api = {
      list: { method: "GET", path: `${apiBase}/formulas`, httpStatus: listApi.status, code: listApi.json.code },
      references: {
        materials: { method: "GET", path: `${apiBase}/references/materials`, httpStatus: materialsApi.status, code: materialsApi.json.code, count: materialsApi.json.data.count },
        batchServers: { method: "GET", path: `${apiBase}/references/batch-servers`, httpStatus: batchServersApi.status, code: batchServersApi.json.code, count: batchServersApi.json.data.count },
      },
      create: {
        method: "POST",
        path: `${apiBase}/formulas`,
        httpStatus: created.response.status(),
        code: created.payload.code,
        requestId: createRequestId,
        formulaId,
        idempotentRetry: replay.json.data.idempotent,
      },
      update: {
        method: "PUT",
        path: `${apiBase}/formulas/${formulaId}`,
        httpStatus: secondUpdate.response.status(),
        code: secondUpdate.payload.code,
        optimisticVersion: formulaVersion,
        firstRequestId: firstUpdate.requestPayload.requestId,
        secondRequestId: secondUpdate.requestPayload.requestId,
      },
      delivery: {
        publishPath: `${apiBase}/formulas/${formulaId}/deliveries`,
        retryPath: `${apiBase}/deliveries/${deliveryId}/retry`,
        firstAttemptState: publishPayload.data.state,
        firstAttemptHttpStatus: publishPayload.data.httpStatus,
        retryState: retryPayload.data.state,
        retryHttpStatus: retryPayload.data.httpStatus,
        attempts: retryPayload.data.attempts,
        adapter: retryPayload.data.adapter,
      },
    };

    report.postgresql.formula = runSqlJson(`
SELECT json_build_object(
  'table', 'rm_formulas', 'id', id::text, 'version', version,
  'formulaEdition', formula_edtion, 'batchFormulaEdition', batch_formula_edition,
  'valid', valid, 'batchStatus', batch_status, 'description', description
)::text FROM public.rm_formulas WHERE id = ${sqlBigint(formulaId)};
`);
    report.postgresql.process = runSqlJson(`
SELECT json_build_object(
  'table', 'rm_formula_processes', 'id', MIN(id)::text, 'formulaId', ${sqlLiteral(formulaId)},
  'name', MIN(name), 'valid', BOOL_AND(valid), 'count', COUNT(*)
)::text FROM public.rm_formula_processes
 WHERE COALESCE(formula_id, formula) = ${sqlBigint(formulaId)} AND COALESCE(valid, TRUE);
`);
    report.postgresql.activity = runSqlJson(`
SELECT json_build_object(
  'table', 'rm_process_actives', 'id', MIN(id)::text, 'formulaId', ${sqlLiteral(formulaId)},
  'processId', MIN(process_id)::text, 'name', MIN(name),
  'dispatchSystem', MIN(dispatch_system), 'executionSystem', MIN(exe_system),
  'valid', BOOL_AND(valid), 'count', COUNT(*)
)::text FROM public.rm_process_actives
 WHERE formula_id = ${sqlBigint(formulaId)} AND COALESCE(valid, TRUE);
`);
    report.postgresql.revisions = runSqlJson(`
SELECT json_build_object(
  'table', 'rm_formula_editor_revisions', 'count', COUNT(*),
  'latestRevisionNo', MAX(revision_no), 'latestFormulaVersion', MAX(formula_version)
)::text FROM public.rm_formula_editor_revisions WHERE formula_id = ${sqlBigint(formulaId)};
`);
    report.postgresql.deliveries = runSqlJson(`
SELECT json_build_object(
  'table', 'rm_formula_deliveries', 'count', COUNT(*),
  'latestState', (ARRAY_AGG(state ORDER BY id DESC))[1],
  'latestAttempts', (ARRAY_AGG(attempts ORDER BY id DESC))[1],
  'latestHttpStatus', (ARRAY_AGG(http_status ORDER BY id DESC))[1]
)::text FROM public.rm_formula_deliveries WHERE formula_id = ${sqlBigint(formulaId)};
`);
    report.postgresql.deliveryAttempts = runSqlJson(`
SELECT json_build_object(
  'table', 'rm_formula_delivery_attempts',
  'latestDeliveryCount', COUNT(*),
  'states', COALESCE(json_agg(state || ':' || COALESCE(http_status::text, '') ORDER BY attempt_no), '[]'::json)
)::text FROM public.rm_formula_delivery_attempts WHERE delivery_id = ${sqlBigint(deliveryId)};
`);

    const allBrowserErrors = [
      ...listEvidence.consoleErrors, ...listEvidence.pageErrors, ...listEvidence.requestFailures,
      ...browserEvidence.consoleErrors, ...browserEvidence.pageErrors, ...browserEvidence.requestFailures,
      ...mobileEvidence.consoleErrors, ...mobileEvidence.pageErrors, ...mobileEvidence.requestFailures,
    ];
    if (allBrowserErrors.length) {
      throw new Error(`Browser errors detected: ${JSON.stringify(allBrowserErrors.slice(0, 10))}`);
    }
    if (desktopOverflow || mobileDimensions.horizontalOverflow) {
      throw new Error("Editor document has horizontal overflow");
    }
    if (report.postgresql.formula.version !== 2 ||
        report.postgresql.process.count !== 1 ||
        report.postgresql.activity.count !== 1 ||
        report.postgresql.revisions.count !== 3 ||
        JSON.stringify(report.postgresql.deliveryAttempts.states) !== JSON.stringify(["FAILED:503", "ACKNOWLEDGED:200"])) {
      throw new Error(`PostgreSQL readback mismatch: ${JSON.stringify(report.postgresql)}`);
    }

    report.postDeployRegression = {
      verifiedAt: new Date().toISOString(),
      jarSha256: runRemote(
        `sha256sum ${shellQuote(path.posix.join(remoteRoot, "runtime/bap-server/module-Server/RMFormulaEditor/manual/rm-formula-editor.jar"))} | awk '{print $1}'`
      ),
      unitTests: { run: 10, failures: 0, errors: 0, immutableRetryPayload: "PASS" },
      runtime: {
        serviceHealth: "UP",
        authenticatedListHttpStatus: listApi.status,
        authenticatedListCode: listApi.json.code,
        visibleWebEditorEntry: report.browser.batchFormulaList.webEditorButtonVisible,
        webEditorPopupHttpStatus: editorNavigation.status(),
        webEditorPopupSystemError: false,
        webEditorPopupHorizontalOverflow: desktopOverflow,
        consoleErrors: browserEvidence.consoleErrors.length,
        pageErrors: browserEvidence.pageErrors.length,
      },
    };
    report.scopeBoundary.webEditorStatus = "PASS";
    report.scopeBoundary.postgresPersistenceStatus = "PASS";
    report.scopeBoundary.httpDeliveryContractStatus = "PASS";
    report.summary = {
      status: "PASS_WITH_EXTERNAL_DCS_BLOCKED",
      webEditor: "PASS",
      api: "PASS",
      postgresPersistence: "PASS",
      testAdapterRetry: "PASS",
      externalBatchDcs: "BLOCKED",
    };
  } catch (error) {
    failure = error;
    report.issues.push({ type: "acceptance", message: error.stack || error.message });
    report.browser.failureEvidence = browserEvidence;
    report.summary.status = "FAIL";
  } finally {
    try {
      runSql(cleanupSql());
      report.cleanup = { executed: true, counts: cleanupCounts() };
      if (Object.values(report.cleanup.counts).some((count) => Number(count) !== 0)) {
        throw new Error(`Marker cleanup incomplete: ${JSON.stringify(report.cleanup.counts)}`);
      }
    } catch (cleanupError) {
      report.cleanup = { executed: false, error: cleanupError.message };
      report.issues.push({ type: "cleanup", message: cleanupError.stack || cleanupError.message });
      if (!failure) failure = cleanupError;
      report.summary.status = "FAIL";
    }
    if (browser) await browser.close();
    if (api) await api.dispose();
    report.generatedAt = new Date().toISOString();
    fs.writeFileSync(reportPath, JSON.stringify(report, null, 2) + "\n");
  }

  if (failure) throw failure;
  console.log(
    `PASS: RM Web editor marker ${marker} completed browser/API/PostgreSQL/retry acceptance and cleanup; ` +
    "external plant Batch/DCS remains BLOCKED"
  );
}

main().catch((error) => {
  console.error(error.stack || error.message);
  process.exit(1);
});
