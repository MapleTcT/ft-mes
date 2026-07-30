#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { execFileSync, spawnSync } = require("child_process");
const { chromium, request } = require("playwright");

const repoRoot = path.resolve(__dirname, "../../..");
const baseUrl = (process.env.ADP_BASE_URL || "http://10.11.100.17:18080").replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD || "123456";
const dbSshTarget = process.env.ADP_DB_SSH_TARGET || "v6@10.11.100.17";
const dbContainer = process.env.ADP_DB_CONTAINER || "adp-mes-newbase-postgres-1";
const dbName = process.env.ADP_DB_NAME || "adp";
const dbUser = process.env.ADP_DB_USER || "adp";
const headless = process.env.ADP_HEADLESS !== "false";
const nowToken = new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14);
const marker = process.env.ADP_E2E_MARKER || `ADP_E2E_${nowToken}_WOM_MANUAL_ENTRY`;
const outputDir = process.env.ADP_OUTPUT_DIR || path.join("/tmp", `adp-wom-manual-entry-${nowToken}`);
const outputPath = process.env.ADP_OUTPUT_PATH || path.join(outputDir, "wom-manual-entry-results.json");

function ensureDir(directory) {
  fs.mkdirSync(directory, { recursive: true });
}

function repoCommit() {
  const result = spawnSync("git", ["rev-parse", "HEAD"], {
    cwd: repoRoot,
    encoding: "utf8",
  });
  return result.status === 0 ? result.stdout.trim() : "UNKNOWN";
}

function shellQuote(value) {
  return `'${String(value).replace(/'/g, "'\\''")}'`;
}

function sqlLiteral(value) {
  return `'${String(value).replace(/'/g, "''")}'`;
}

function runRemote(command, input) {
  return execFileSync(
    "ssh",
    [
      "-o", "BatchMode=yes",
      "-o", "ConnectTimeout=8",
      "-o", "StrictHostKeyChecking=no",
      "-o", "UserKnownHostsFile=/dev/null",
      dbSshTarget,
      command,
    ],
    { input, encoding: "utf8", stdio: ["pipe", "pipe", "pipe"] }
  );
}

function runSql(sql) {
  const command = [
    "docker", "exec", "-i", shellQuote(dbContainer),
    "psql", "-U", shellQuote(dbUser), "-d", shellQuote(dbName),
    "-v", "ON_ERROR_STOP=1", "-AtF", shellQuote("|"),
  ].join(" ");
  return runRemote(command, sql).trim();
}

function parseRows(raw) {
  return String(raw || "")
    .split(/\r?\n/)
    .filter(Boolean)
    .map((line) => line.split("|"));
}

async function readJsonSafe(response) {
  const text = await response.text();
  try {
    return { json: JSON.parse(text), text };
  } catch (_error) {
    return { json: null, text };
  }
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
    const parsed = await readJsonSafe(response);
    const ticket = response.ok() ? findTicket(parsed.json) : null;
    if (ticket) return { ticket, status: response.status() };
    errors.push({ status: response.status(), body: parsed.text.slice(0, 400) });
  }
  throw new Error(`Login failed for ${username}: ${JSON.stringify(errors)}`);
}

async function browserFetch(page, method, apiPath, payload, formEncoded) {
  return page.evaluate(
    async ({ requestMethod, pathValue, bodyValue, useForm }) => {
      const headers = { Accept: "application/json, text/plain, */*" };
      const ticket = localStorage.getItem("ticket")
        || localStorage.getItem("suposTicket")
        || localStorage.getItem("SUPOS_TICKET")
        || localStorage.getItem("token");
      if (ticket) {
        headers.Authorization = ticket.startsWith("Bearer ") ? ticket : `Bearer ${ticket}`;
      }
      let body;
      if (bodyValue !== undefined) {
        if (useForm) {
          headers["Content-Type"] = "application/x-www-form-urlencoded;charset=UTF-8";
          body = new URLSearchParams(bodyValue).toString();
        } else {
          headers["Content-Type"] = "application/json;charset=UTF-8";
          body = JSON.stringify(bodyValue);
        }
      }
      const response = await fetch(pathValue, { method: requestMethod, headers, body });
      const text = await response.text();
      let json = null;
      try { json = JSON.parse(text); } catch (_error) { json = null; }
      return { status: response.status, ok: response.ok, body: text, json };
    },
    { requestMethod: method, pathValue: apiPath, bodyValue: payload, useForm: Boolean(formEncoded) }
  );
}

function taskSql() {
  return `
SELECT id, version, valid, status, table_info_id, table_no, deployment_id,
       process_key, produce_batch_num, product_id, formula_id, line_id,
       plan_num, plan_start_time, plan_end_time
FROM public.wom_produce_tasks
WHERE produce_batch_num = ${sqlLiteral(marker)}
ORDER BY create_time DESC NULLS LAST, id DESC;
`;
}

function taskByIdSql(id) {
  return `
SELECT id, version, valid, status, table_info_id, table_no, deployment_id,
       process_key, produce_batch_num, product_id, formula_id, line_id,
       plan_num, plan_start_time, plan_end_time
FROM public.wom_produce_tasks WHERE id = ${Number(id)};
`;
}

function pendingSql(taskId) {
  return `
SELECT id, activity_name, activity_type, deployment_id, table_info_id,
       COALESCE(open_url, ''), task_status
FROM public.wfm_task_pending
WHERE model_id = ${Number(taskId)} AND task_status = 88
ORDER BY create_time DESC NULLS LAST, id DESC;
`;
}

function requestSql() {
  return `
SELECT tenant_id, request_id, request_hash, batch_code, status,
       COALESCE(task_id::text, ''), payload_json->>'productCode',
       payload_json->>'formulaCode', payload_json->>'workLineId'
FROM public.wom_manual_task_requests
WHERE batch_code = ${sqlLiteral(marker)}
ORDER BY created_at DESC;
`;
}

function workflowSql(deploymentId, activityName) {
  return `
SELECT code, COALESCE(name_zh_cn, ''), from_node_code, to_node_code, type
FROM public.wf_transition
WHERE deployment_id = ${Number(deploymentId)}
  AND from_node_code = ${sqlLiteral(activityName)}
  AND type = 1
ORDER BY CASE WHEN name_zh_cn = '生效' THEN 0 ELSE 1 END, id
LIMIT 1;
`;
}

function parseTask(raw) {
  const rows = parseRows(raw);
  if (rows.length !== 1) throw new Error(`Expected one task row, got ${rows.length}: ${raw}`);
  const values = rows[0];
  return {
    id: values[0], version: values[1], valid: values[2], status: values[3],
    tableInfoId: values[4], tableNo: values[5], deploymentId: values[6],
    processKey: values[7], batchCode: values[8], productId: values[9],
    formulaId: values[10], lineId: values[11], planNum: values[12],
    planStartTime: values[13], planEndTime: values[14],
  };
}

function parsePending(raw) {
  const rows = parseRows(raw);
  if (rows.length !== 1) throw new Error(`Expected one active pending, got ${rows.length}: ${raw}`);
  const values = rows[0];
  return {
    id: values[0], activityName: values[1], activityType: values[2],
    deploymentId: values[3], tableInfoId: values[4], openUrl: values[5], taskStatus: values[6],
  };
}

function parseRequest(raw) {
  const rows = parseRows(raw);
  if (rows.length !== 1) throw new Error(`Expected one idempotency row, got ${rows.length}: ${raw}`);
  const values = rows[0];
  return {
    tenantId: values[0], requestId: values[1], requestHash: values[2], batchCode: values[3],
    status: values[4], taskId: values[5], productCode: values[6], formulaCode: values[7], lineId: values[8],
  };
}

function submitPayload(data, pending, transition) {
  return {
    id: Number(data.id),
    produceTask: { id: Number(data.id), version: Number(data.version), remark: `${marker}_SUBMITTED` },
    viewCode: "WOM_1.0.0_produceTask_makeTaskEdit",
    operateType: "submit",
    deploymentId: String(data.deploymentId || pending.deploymentId),
    linkId: String(data.tableInfoId),
    pendingId: Number(pending.id),
    activityName: pending.activityName,
    pendingActivityType: "4",
    webSignetFlag: false,
    superEdit: false,
    files_staffId: "1",
    uploadFileFormMap: [],
    dgList: {},
    dgDeletedIds: {},
    workFlowVar: {
      outcome: transition.code,
      outcomeType: "normal",
      outcomeDes: transition.name,
      outcomeDesZhCn: transition.name,
      outcomeMap: [{ outcome: transition.code, dec: transition.name, type: "normal", assignUser: "" }],
      deploymentId: String(data.deploymentId || pending.deploymentId),
      entityCode: "WOM_1.0.0_produceTask",
    },
  };
}

function attachBrowserEvidence(page, bucket, label) {
  page.on("console", (message) => {
    if (["error", "warning"].includes(message.type())) {
      bucket.console.push({ page: label, type: message.type(), text: message.text() });
    }
  });
  page.on("pageerror", (error) => bucket.pageErrors.push({ page: label, message: error.message }));
  page.on("requestfailed", (item) => {
    bucket.requestFailures.push({ page: label, method: item.method(), url: item.url(), failure: item.failure() });
  });
}

async function main() {
  ensureDir(outputDir);
  const evidence = {
    schemaVersion: 1,
    generatedAt: new Date().toISOString(),
    repoCommit: repoCommit(),
    database: "PostgreSQL",
    baseUrl,
    dbSshTarget,
    marker,
    route: "/msService/WOM/produceTask/produceTask/makeTaskList",
    entryRoute: "/msService/WOM/produceTask/manual-entry/page",
    status: "RUNNING",
    frontend: { console: [], pageErrors: [], requestFailures: [], screenshots: {} },
    operations: {},
    persistence: {},
    issues: [],
  };

  let browser;
  let listPage;
  let entryPage;
  let task;
  let requestRow;
  let rollbackComplete = false;
  try {
    const preexisting = parseRows(runSql(taskSql()));
    if (preexisting.length) throw new Error(`Marker already exists: ${marker}`);

    const concurrencyGuardSql = `
SELECT indexdef
FROM pg_indexes
WHERE schemaname = 'public'
  AND tablename = 'wom_manual_task_requests'
  AND indexname = 'uq_wom_manual_task_requests_active_batch';
`;
    const concurrencyGuardRaw = runSql(concurrencyGuardSql);
    evidence.persistence.batchConcurrencyGuard = {
      sql: concurrencyGuardSql.trim(),
      raw: concurrencyGuardRaw,
    };
    if (
      !concurrencyGuardRaw.includes("CREATE UNIQUE INDEX") ||
      !concurrencyGuardRaw.includes("lower(TRIM") ||
      !concurrencyGuardRaw.includes("PROCESSING") ||
      !concurrencyGuardRaw.includes("SUCCESS")
    ) {
      throw new Error(`Batch concurrency guard is missing: ${concurrencyGuardRaw}`);
    }

    const api = await request.newContext({ ignoreHTTPSErrors: true });
    const loginResult = await login(api);
    await api.dispose();
    evidence.login = { status: loginResult.status, authenticated: true };

    browser = await chromium.launch({ headless });
    const context = await browser.newContext({
      baseURL: baseUrl,
      ignoreHTTPSErrors: true,
      viewport: { width: 1600, height: 1000 },
    });
    await context.addInitScript((ticket) => {
      localStorage.setItem("ticket", ticket);
      localStorage.setItem("suposTicket", ticket);
      localStorage.setItem("SUPOS_TICKET", ticket);
      localStorage.setItem("token", ticket);
    }, loginResult.ticket);

    listPage = await context.newPage();
    attachBrowserEvidence(listPage, evidence.frontend, "list");
    const listNavigation = await listPage.goto(evidence.route, { waitUntil: "domcontentloaded", timeout: 60000 });
    await listPage.waitForTimeout(2500);
    evidence.frontend.list = {
      status: listNavigation && listNavigation.status(),
      title: await listPage.title(),
      buttonText: (await listPage.locator("#btn-manualCreateTask").innerText()).trim(),
      patched: await listPage.locator("#btn-manualCreateTask").getAttribute("data-adp-wom-manual-create"),
    };
    evidence.frontend.screenshots.list = path.join(outputDir, "01-manufacturing-instruction-list.png");
    await listPage.screenshot({ path: evidence.frontend.screenshots.list, fullPage: true });

    let entryDocumentRequest = null;
    let optionsRequest = null;
    let resolveOptionsResponse;
    const optionsResponsePromise = new Promise((resolve) => { resolveOptionsResponse = resolve; });
    context.on("request", (item) => {
      if (item.isNavigationRequest() && item.url().endsWith(evidence.entryRoute)) {
        entryDocumentRequest = {
          method: item.method(),
          hasAuthorization: Boolean(item.headers().authorization),
        };
      }
      if (item.url().includes("/manual-entry/options")) {
        optionsRequest = {
          method: item.method(),
          hasAuthorization: Boolean(item.headers().authorization),
        };
      }
    });
    context.on("response", async (response) => {
      if (response.url().includes("/manual-entry/options")) {
        const parsed = await readJsonSafe(response);
        resolveOptionsResponse({
          status: response.status(),
          body: parsed.text,
          json: parsed.json,
        });
      }
    });

    const popupPromise = listPage.waitForEvent("popup");
    await listPage.locator("#btn-manualCreateTask").click();
    entryPage = await popupPromise;
    attachBrowserEvidence(entryPage, evidence.frontend, "entry");
    await entryPage.waitForLoadState("domcontentloaded");
    await entryPage.locator("#production-option option").nth(1).waitFor({ state: "attached", timeout: 30000 });
    const optionsResponse = await Promise.race([
      optionsResponsePromise,
      new Promise((_resolve, reject) => setTimeout(
        () => reject(new Error("Options response capture timed out")),
        5000
      )),
    ]);
    evidence.frontend.entry = {
      url: entryPage.url(),
      title: await entryPage.title(),
      optionCount: await entryPage.locator("#production-option option").count(),
      selectedOption: await entryPage.locator("#production-option").inputValue(),
      documentRequest: entryDocumentRequest,
      optionsRequest,
      optionsResponseStatus: optionsResponse.status,
    };
    if (
      !entryDocumentRequest || entryDocumentRequest.hasAuthorization ||
      !optionsRequest || !optionsRequest.hasAuthorization ||
      optionsResponse.status !== 200 || !optionsResponse.json ||
      Number(optionsResponse.json.code) !== 200
    ) {
      throw new Error(`Manual-entry auth boundary mismatch: ${JSON.stringify(evidence.frontend.entry)}`);
    }
    await entryPage.setViewportSize({ width: 390, height: 844 });
    evidence.frontend.mobile = await entryPage.evaluate(() => ({
      viewportWidth: window.innerWidth,
      documentWidth: document.documentElement.scrollWidth,
      horizontalOverflow: document.documentElement.scrollWidth > window.innerWidth,
    }));
    evidence.frontend.screenshots.mobile = path.join(outputDir, "02-mobile-manual-entry-form.png");
    await entryPage.screenshot({ path: evidence.frontend.screenshots.mobile, fullPage: true });
    if (evidence.frontend.mobile.horizontalOverflow) {
      throw new Error(`Manual entry mobile layout overflows: ${JSON.stringify(evidence.frontend.mobile)}`);
    }
    await entryPage.setViewportSize({ width: 1600, height: 1000 });

    let capturedCreatePayload = null;
    let capturedCreateResponse = null;
    let resolveCreateResponse;
    const createResponsePromise = new Promise((resolve) => { resolveCreateResponse = resolve; });
    entryPage.on("request", (item) => {
      if (item.method() === "POST" && item.url().endsWith("/manual-entry/create")) {
        try { capturedCreatePayload = JSON.parse(item.postData() || "null"); } catch (_error) { capturedCreatePayload = null; }
      }
    });
    entryPage.on("response", async (response) => {
      if (response.url().endsWith("/manual-entry/create")) {
        const parsed = await readJsonSafe(response);
        capturedCreateResponse = { status: response.status(), body: parsed.text, json: parsed.json };
        resolveCreateResponse(capturedCreateResponse);
      }
    });

    await entryPage.locator("#batch-code").fill(marker);
    await entryPage.locator("#plan-num").fill("2.5");
    evidence.frontend.screenshots.form = path.join(outputDir, "02-manual-entry-form.png");
    await entryPage.screenshot({ path: evidence.frontend.screenshots.form, fullPage: true });
    await entryPage.locator("#submit-create").click();
    const submissionOutcome = await Promise.race([
      entryPage.locator("#result-section:not(.hidden)").waitFor({ timeout: 60000 })
        .then(() => ({ status: "success" })),
      entryPage.locator("#status.status.visible.error").waitFor({ timeout: 60000 })
        .then(async () => ({
          status: "error",
          message: (await entryPage.locator("#status-text").innerText()).trim(),
        })),
    ]);
    if (submissionOutcome.status === "error") {
      throw new Error(`Manual-entry create failed in page: ${submissionOutcome.message}`);
    }
    capturedCreateResponse = await Promise.race([
      createResponsePromise,
      new Promise((_resolve, reject) => setTimeout(() => reject(new Error("Create response capture timed out")), 5000)),
    ]);
    if (!capturedCreatePayload || !capturedCreateResponse) throw new Error("Create request/response was not captured");
    evidence.operations.create = {
      method: "POST",
      api: "/msService/WOM/produceTask/manual-entry/create",
      payload: capturedCreatePayload,
      response: capturedCreateResponse,
    };
    if (capturedCreateResponse.status !== 200 || !capturedCreateResponse.json || capturedCreateResponse.json.code !== 200) {
      throw new Error(`Create failed: ${capturedCreateResponse.body}`);
    }

    evidence.frontend.result = {
      message: (await entryPage.locator("#status-text").innerText()).trim(),
      taskId: (await entryPage.locator("#result-task-id").innerText()).trim(),
      tableNo: (await entryPage.locator("#result-table-no").innerText()).trim(),
      batchCode: (await entryPage.locator("#result-batch").innerText()).trim(),
    };
    evidence.frontend.screenshots.result = path.join(outputDir, "03-manual-entry-result.png");
    await entryPage.screenshot({ path: evidence.frontend.screenshots.result, fullPage: true });

    const taskRaw = runSql(taskSql());
    task = parseTask(taskRaw);
    const pendingRaw = runSql(pendingSql(task.id));
    const pending = parsePending(pendingRaw);
    const requestRaw = runSql(requestSql());
    requestRow = parseRequest(requestRaw);
    evidence.persistence.afterCreate = {
      taskSql: taskSql().trim(), taskRaw, task,
      pendingSql: pendingSql(task.id).trim(), pendingRaw, pending,
      requestSql: requestSql().trim(), requestRaw, request: requestRow,
    };
    if (
      task.valid !== "t" || task.status !== "88" || task.batchCode !== marker ||
      requestRow.status !== "SUCCESS" || requestRow.taskId !== task.id ||
      String(capturedCreatePayload.workLineId) !== task.lineId
    ) {
      throw new Error(`Create persistence mismatch: ${JSON.stringify(evidence.persistence.afterCreate)}`);
    }

    const idempotentResponse = await browserFetch(
      listPage,
      "POST",
      "/msService/WOM/produceTask/manual-entry/create",
      capturedCreatePayload
    );
    const taskCountRaw = runSql(`SELECT count(*) FROM public.wom_produce_tasks WHERE produce_batch_num = ${sqlLiteral(marker)};`);
    evidence.operations.idempotentReplay = {
      method: "POST",
      api: "/msService/WOM/produceTask/manual-entry/create",
      payload: capturedCreatePayload,
      response: idempotentResponse,
      taskCountSql: `SELECT count(*) FROM public.wom_produce_tasks WHERE produce_batch_num = ${sqlLiteral(marker)};`,
      taskCountRaw,
    };
    if (
      idempotentResponse.status !== 200 || !idempotentResponse.json ||
      idempotentResponse.json.code !== 200 ||
      !idempotentResponse.json.data.idempotent ||
      String(idempotentResponse.json.data.task.taskId) !== task.id ||
      taskCountRaw !== "1"
    ) {
      throw new Error(`Idempotency mismatch: ${JSON.stringify(evidence.operations.idempotentReplay)}`);
    }

    const conflictingPayload = {
      ...capturedCreatePayload,
      requestId: `${capturedCreatePayload.requestId}-BATCH-CONFLICT`.slice(0, 80),
    };
    const conflictingResponse = await browserFetch(
      listPage,
      "POST",
      "/msService/WOM/produceTask/manual-entry/create",
      conflictingPayload
    );
    const conflictingTaskCountRaw = runSql(
      `SELECT count(*) FROM public.wom_produce_tasks WHERE produce_batch_num = ${sqlLiteral(marker)};`
    );
    const conflictingRequestCountSql = `
SELECT count(*) FROM public.wom_manual_task_requests
WHERE request_id = ${sqlLiteral(conflictingPayload.requestId)};
`;
    const conflictingRequestCountRaw = runSql(conflictingRequestCountSql);
    evidence.operations.conflictingBatchRequest = {
      method: "POST",
      api: "/msService/WOM/produceTask/manual-entry/create",
      payload: conflictingPayload,
      response: conflictingResponse,
      taskCountRaw: conflictingTaskCountRaw,
      requestCountSql: conflictingRequestCountSql.trim(),
      requestCountRaw: conflictingRequestCountRaw,
    };
    if (
      conflictingResponse.status !== 200 || !conflictingResponse.json ||
      conflictingResponse.json.code !== 409 ||
      conflictingTaskCountRaw !== "1" || conflictingRequestCountRaw !== "0"
    ) {
      throw new Error(`Conflicting batch claim mismatch: ${JSON.stringify(evidence.operations.conflictingBatchRequest)}`);
    }

    await Promise.all([
      entryPage.waitForNavigation({ waitUntil: "domcontentloaded", timeout: 60000 }),
      entryPage.locator("#open-pending").click(),
    ]);
    await entryPage.waitForTimeout(2500);
    const formValues = await entryPage.locator("input").evaluateAll((inputs) => inputs.map((input) => input.value).filter(Boolean));
    evidence.frontend.pending = {
      url: entryPage.url(),
      title: await entryPage.title(),
      markerVisibleInForm: formValues.some((value) => value === marker),
      visibleInputCount: await entryPage.locator("input:visible").count(),
    };
    evidence.frontend.screenshots.pending = path.join(outputDir, "04-created-instruction-pending.png");
    await entryPage.screenshot({ path: evidence.frontend.screenshots.pending, fullPage: true });
    if (!evidence.frontend.pending.markerVisibleInForm) {
      throw new Error("Created marker is not visible in the generated edit pending page");
    }

    const dataApi = `/msService/WOM/produceTask/produceTask/data/${task.id}?pendingId=${pending.id}`;
    const dataResponse = await browserFetch(listPage, "GET", dataApi);
    const data = dataResponse.json && Object.prototype.hasOwnProperty.call(dataResponse.json, "data")
      ? dataResponse.json.data : dataResponse.json;
    if (dataResponse.status !== 200 || !data || !data.id) {
      throw new Error(`Generated task data lookup failed: ${dataResponse.body}`);
    }

    const transitionRaw = runSql(workflowSql(task.deploymentId, pending.activityName));
    const transitionRows = parseRows(transitionRaw);
    if (transitionRows.length !== 1) throw new Error(`Workflow transition missing: ${transitionRaw}`);
    const transition = {
      code: transitionRows[0][0], name: transitionRows[0][1],
      from: transitionRows[0][2], to: transitionRows[0][3], type: transitionRows[0][4],
    };
    const submitApi = `/msService/WOM/produceTask/produceTask/makeTaskEdit/submit?id=${task.id}`;
    const submission = submitPayload(data, pending, transition);
    const submitResponse = await browserFetch(listPage, "POST", submitApi, submission);
    evidence.operations.submit = { method: "POST", api: submitApi, payload: submission, response: submitResponse };
    if (submitResponse.status !== 200 || !submitResponse.json || submitResponse.json.code !== 200) {
      throw new Error(`Submit failed: ${submitResponse.body}`);
    }

    const submittedRaw = runSql(taskByIdSql(task.id));
    task = parseTask(submittedRaw);
    const pendingAfterSubmit = runSql(pendingSql(task.id));
    const dealRaw = runSql(`
SELECT COALESCE(outcome, ''), COALESCE(outcome_des, ''), dealinfo_type
FROM public.wf_deal_info
WHERE table_info_id = ${Number(task.tableInfoId)}
ORDER BY create_time DESC NULLS LAST, id DESC LIMIT 1;
`);
    evidence.persistence.afterSubmit = {
      taskSql: taskByIdSql(task.id).trim(), taskRaw: submittedRaw, task,
      pendingSql: pendingSql(task.id).trim(), pendingRaw: pendingAfterSubmit,
      dealRaw,
    };
    if (task.status !== "99" || pendingAfterSubmit !== "" || !dealRaw.includes(transition.code)) {
      throw new Error(`Submit persistence mismatch: ${JSON.stringify(evidence.persistence.afterSubmit)}`);
    }

    const deletePayload = { ids: `${task.id}@${task.version}` };
    const deleteResponse = await browserFetch(
      listPage,
      "POST",
      "/msService/WOM/produceTask/produceTask/delete",
      deletePayload,
      true
    );
    evidence.operations.rollback = {
      method: "POST",
      api: "/msService/WOM/produceTask/produceTask/delete",
      payload: deletePayload,
      response: deleteResponse,
    };
    if (deleteResponse.status !== 200 || !deleteResponse.json || deleteResponse.json.code !== 200) {
      throw new Error(`Rollback failed: ${deleteResponse.body}`);
    }

    const rolledBackRaw = runSql(taskByIdSql(task.id));
    task = parseTask(rolledBackRaw);
    evidence.persistence.afterRollback = { sql: taskByIdSql(task.id).trim(), raw: rolledBackRaw, task };
    if (task.valid !== "f") throw new Error(`Rollback did not soft-delete task: ${rolledBackRaw}`);
    rollbackComplete = true;

    const requestCleanupSql = `
DELETE FROM public.wom_manual_task_requests
WHERE tenant_id = ${sqlLiteral(requestRow.tenantId)} AND request_id = ${sqlLiteral(requestRow.requestId)};
SELECT count(*) FROM public.wom_manual_task_requests
WHERE tenant_id = ${sqlLiteral(requestRow.tenantId)} AND request_id = ${sqlLiteral(requestRow.requestId)};
`;
    const requestCleanupRaw = runSql(requestCleanupSql);
    evidence.persistence.requestCleanup = { sql: requestCleanupSql.trim(), raw: requestCleanupRaw };
    if (!requestCleanupRaw.endsWith("0")) throw new Error(`Idempotency cleanup failed: ${requestCleanupRaw}`);

    const hardFrontendErrors = evidence.frontend.console.filter((item) => item.type === "error");
    if (hardFrontendErrors.length || evidence.frontend.pageErrors.length || evidence.frontend.requestFailures.length) {
      throw new Error(`Frontend errors detected: ${JSON.stringify(evidence.frontend)}`);
    }

    evidence.summary = {
      testedFeatures: 9,
      pass: 9,
      fail: 0,
      blocked: 0,
      notApplicable: 0,
    };
    evidence.status = "PASS";
  } catch (error) {
    evidence.status = "FAIL";
    evidence.summary = { testedFeatures: 9, pass: 0, fail: 1, blocked: 0, notApplicable: 0 };
    evidence.issues.push(error && error.stack ? error.stack : String(error));
    process.exitCode = 1;
  } finally {
    if (listPage && task && !rollbackComplete) {
      try {
        const current = parseTask(runSql(taskByIdSql(task.id)));
        if (current.valid === "t") {
          evidence.operations.emergencyRollback = await browserFetch(
            listPage,
            "POST",
            "/msService/WOM/produceTask/produceTask/delete",
            { ids: `${current.id}@${current.version}` },
            true
          );
        }
      } catch (cleanupError) {
        evidence.issues.push(`Emergency rollback failed: ${cleanupError.message}`);
      }
    }
    if (requestRow) {
      try {
        runSql(`DELETE FROM public.wom_manual_task_requests WHERE tenant_id = ${sqlLiteral(requestRow.tenantId)} AND request_id = ${sqlLiteral(requestRow.requestId)};`);
      } catch (cleanupError) {
        evidence.issues.push(`Idempotency cleanup failed: ${cleanupError.message}`);
      }
    }
    if (browser) await browser.close();
    fs.writeFileSync(outputPath, `${JSON.stringify(evidence, null, 2)}\n`, "utf8");
    process.stdout.write(`${JSON.stringify({ status: evidence.status, marker, outputPath, summary: evidence.summary }, null, 2)}\n`);
  }
}

main().catch((error) => {
  process.stderr.write(`${error && error.stack ? error.stack : error}\n`);
  process.exit(1);
});
