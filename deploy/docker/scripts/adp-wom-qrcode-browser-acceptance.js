#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { execFileSync } = require("child_process");
const { chromium, request } = require("playwright");

const baseUrl = (process.env.ADP_BASE_URL || "http://100.99.133.43:18080").replace(/\/+$/, "");
const browserBaseUrl = (process.env.ADP_BROWSER_BASE_URL || baseUrl).replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD || "123456";
const dbSshTarget = process.env.ADP_DB_SSH_TARGET || "v6@100.99.133.43";
const dbSshPassword = process.env.ADP_DB_SSH_PASSWORD || "";
const dbContainer = process.env.ADP_DB_CONTAINER || "adp-mes-newbase-postgres-1";
const dbName = process.env.ADP_DB_NAME || "adp";
const dbUser = process.env.ADP_DB_USER || "adp";
const outputPath =
  process.env.ADP_WOM_QRCODE_BROWSER_OUTPUT || "metadata/wom-qrcode-browser-acceptance.json";
const screenshotPath =
  process.env.ADP_WOM_QRCODE_BROWSER_SCREENSHOT || "/tmp/adp-wom-qrcode-browser-acceptance.png";
const manufactureDate = process.env.ADP_WOM_QRCODE_BROWSER_TEST_DATE || "2099-12-29";
const expiryDate = process.env.ADP_WOM_QRCODE_BROWSER_TEST_EXPIRY || "2100-12-29";
const route = "/msService/WOM/produceTask/produceTask/makeTaskList";
const gridId = "WOM_1.0.0_produceTask_makeTaskList_produceTask_sdg";
const timeoutMs = Number(process.env.ADP_PAGE_TIMEOUT_MS || 180000);

function ensureDir(filePath) {
  fs.mkdirSync(path.dirname(path.resolve(filePath)), { recursive: true });
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

function parseRows(raw) {
  return String(raw || "")
    .split(/\r?\n/)
    .filter((line) => line && !/^(INSERT|UPDATE|DELETE)\s+\d+/i.test(line))
    .map((line) => line.split("|"));
}

function sequenceSnapshot() {
  const rows = parseRows(runSql(`
SELECT last_sequence::text
FROM public.wom_qrcode_daily_sequences
WHERE tenant_id = 'default' AND manufacture_date = ${sqlLiteral(manufactureDate)}::date;
`));
  return {
    existed: rows.length > 0,
    lastSequence: rows.length ? Number(rows[0][0]) : null,
  };
}

function markerRows(marker) {
  return parseRows(runSql(`
SELECT request_id,
       sequence_no::text,
       task_id::text,
       qr_code,
       detail,
       is_print::text,
       print_count::text,
       COALESCE(created_at::text, '')
FROM public.wom_package_qrcodes
WHERE tenant_id = 'default' AND request_id = ${sqlLiteral(marker)}
ORDER BY sequence_no;
`));
}

function cleanup(marker, snapshot) {
  const sequenceRestore = snapshot.existed
    ? `UPDATE public.wom_qrcode_daily_sequences
       SET last_sequence = ${Number(snapshot.lastSequence)}, updated_at = CURRENT_TIMESTAMP
       WHERE tenant_id = 'default' AND manufacture_date = ${sqlLiteral(manufactureDate)}::date;`
    : `DELETE FROM public.wom_qrcode_daily_sequences
       WHERE tenant_id = 'default' AND manufacture_date = ${sqlLiteral(manufactureDate)}::date;`;
  runSql(`
DELETE FROM public.wom_package_qrcodes
WHERE tenant_id = 'default' AND request_id = ${sqlLiteral(marker)};
${sequenceRestore}
`);
  return markerRows(marker).length;
}

async function readJson(response) {
  const text = await response.text();
  try {
    return { json: JSON.parse(text), text };
  } catch (_error) {
    return { json: null, text };
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
    const parsed = await readJson(response);
    const ticket = response.ok() ? findTicket(parsed.json) : null;
    if (ticket) return { ticket, status: response.status() };
    errors.push({ status: response.status(), body: parsed.text.slice(0, 300) });
  }
  throw new Error(`Login failed for ${username}: ${JSON.stringify(errors)}`);
}

async function selectTaskRow(page) {
  const navigation = await page.goto(route, { waitUntil: "domcontentloaded", timeout: timeoutMs });
  await page.waitForFunction(
    (code) => {
      const factory = window.ReactAPI && window.ReactAPI.getComponentAPI("SupDataGrid");
      const grid = factory && typeof factory.APIs === "function" && factory.APIs(code);
      return Boolean(grid && typeof grid.refreshDataByRequst === "function");
    },
    gridId,
    { timeout: timeoutMs },
  );

  const refreshResponsePromise = page
    .waitForResponse(
      (response) =>
        /\/WOM\/produceTask\/produceTask\/makeTaskList-(pending|query)/.test(response.url()) &&
        response.request().method() === "POST",
      { timeout: timeoutMs },
    )
    .catch(() => null);

  await page.evaluate(
    ({ code }) => {
      const factory = window.ReactAPI && window.ReactAPI.getComponentAPI("SupDataGrid");
      const grid = factory && typeof factory.APIs === "function" && factory.APIs(code);
      grid.refreshDataByRequst({
        type: "POST",
        url: "/msService/WOM/produceTask/produceTask/makeTaskList-pending",
        param: {
          classifyCodes: "",
          customCondition: {},
          permissionCode: "WOM_1.0.0_produceTask_makeTaskList",
          pageNo: 1,
          paging: true,
          pageSize: 65535,
          crossCompanyFlag: "true",
        },
      });
    },
    { code: gridId },
  );

  const refreshResponse = await refreshResponsePromise;
  const selection = await page.evaluate(
    ({ code }) => {
      const factory = window.ReactAPI && window.ReactAPI.getComponentAPI("SupDataGrid");
      const grid = factory && typeof factory.APIs === "function" && factory.APIs(code);
      const rows =
        (grid && typeof grid.getRows === "function" && grid.getRows()) ||
        (grid && typeof grid.getDatagridData === "function" && grid.getDatagridData()) ||
        [];
      const rowIndex = rows.findIndex((row) => {
        const product = row.productId || {};
        return Boolean(row.id && row.tableNo && row.produceBatchNum && product.code);
      });
      if (rowIndex < 0) {
        return {
          ok: false,
          rowCount: rows.length,
          sample: rows.slice(0, 10).map((row) => ({
            id: row.id,
            tableNo: row.tableNo,
            batchNo: row.produceBatchNum,
            productCode: row.productId && row.productId.code,
          })),
        };
      }
      grid.setSelecteds(String(rowIndex));
      const selectedRows = typeof grid.getSelecteds === "function" ? grid.getSelecteds() : [];
      const selected = selectedRows[0] || {};
      const product = selected.productId || {};
      return {
        ok: selectedRows.length === 1,
        rowIndex,
        selectedCount: selectedRows.length,
        selectedId: String(selected.id || ""),
        tableNo: selected.tableNo || "",
        batchNo: selected.produceBatchNum || "",
        task: {
          id: String(selected.id || ""),
          tableNo: selected.tableNo || "",
          batchNo: selected.produceBatchNum || "",
          productCode: product.code || "",
          productName: product.name || "",
          taskRunState: (selected.taskRunState && selected.taskRunState.id) || "",
        },
      };
    },
    { code: gridId },
  );
  if (!selection.ok || !selection.task || !selection.task.id) {
    throw new Error(`No QR-capable task was found in the visible WOM grid: ${JSON.stringify(selection)}`);
  }
  const task = selection.task;

  const rowLocator = page
    .locator(
      ".sup-datagrid-body-wrap .sup-datagrid-row, .ant-table-tbody tr, tbody tr, .ant-table-row, .sup-table-row, .datagrid-row, .cui-grid-row",
    )
    .filter({ hasText: task.tableNo })
    .first();
  const domClick = { attempted: false, ok: false };
  if ((await rowLocator.count()) > 0) {
    domClick.attempted = true;
    try {
      await rowLocator.click({ timeout: 5000 });
      domClick.ok = true;
    } catch (error) {
      domClick.error = error.message;
    }
  }

  await page.evaluate(
    ({ code, rowIndex }) => {
      const factory = window.ReactAPI && window.ReactAPI.getComponentAPI("SupDataGrid");
      const grid = factory && typeof factory.APIs === "function" && factory.APIs(code);
      grid.setSelecteds(String(rowIndex));
    },
    { code: gridId, rowIndex: selection.rowIndex },
  );

  await page.locator("#btn-generateCode[data-adp-wom-qrcode='true']").waitFor({
    state: "visible",
    timeout: timeoutMs,
  });
  return {
    navigationStatus: navigation && navigation.status(),
    refreshStatus: refreshResponse && refreshResponse.status(),
    task,
    selection,
    domClick,
  };
}

async function waitForQrFrame(page) {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    const frame = page.frames().find((candidate) =>
      candidate.url().includes("/msService/WOM/printManage/printDate/generateCode"),
    );
    if (frame) return frame;
    await page.waitForTimeout(200);
  }
  throw new Error("QR dialog iframe did not become available");
}

async function runBrowser(ticket, marker, report) {
  const browser = await chromium.launch({ headless: process.env.ADP_HEADLESS !== "false" });
  try {
    const context = await browser.newContext({
      baseURL: browserBaseUrl,
      ignoreHTTPSErrors: true,
      viewport: { width: 1920, height: 1080 },
      extraHTTPHeaders: {
        Authorization: `Bearer ${ticket}`,
        "Accept-Language": "zh-CN",
        langu_code: "zh_CN",
      },
    });
    await context.addCookies([
      { name: "suposTicket", value: ticket, url: browserBaseUrl },
      { name: "SUPOS_TICKET", value: ticket, url: browserBaseUrl },
    ]);
    await context.addInitScript((token) => {
      window.localStorage.clear();
      window.sessionStorage.clear();
      ["suposTicket", "SUPOS_TICKET", "token"].forEach((key) => {
        window.localStorage.setItem(key, token);
        window.sessionStorage.setItem(key, token);
      });
    }, ticket);

    const page = await context.newPage();
    page.setDefaultTimeout(timeoutMs);
    page.setDefaultNavigationTimeout(timeoutMs);
    page.on("console", (message) => {
      if (["error", "warning"].includes(message.type())) {
        report.console.push({ type: message.type(), text: message.text().slice(0, 1000) });
      }
    });
    page.on("pageerror", (error) => report.pageErrors.push(error.message));
    page.on("requestfailed", (requestItem) => {
      report.requestFailures.push({
        method: requestItem.method(),
        url: requestItem.url(),
        failure: requestItem.failure() && requestItem.failure().errorText,
      });
    });
    page.on("response", (response) => {
      if (response.status() >= 400) {
        report.failedResponses.push({
          method: response.request().method(),
          url: response.url(),
          status: response.status(),
        });
      }
      if (/makeTaskList|printManage/.test(response.url())) {
        report.network.push({
          method: response.request().method(),
          url: response.url(),
          status: response.status(),
        });
      }
    });

    let interceptedPayload = null;
    await page.route("**/msService/WOM/printManage/generateQrCode", async (routeItem) => {
      const requestItem = routeItem.request();
      if (requestItem.method() !== "POST") {
        await routeItem.continue();
        return;
      }
      const payload = JSON.parse(requestItem.postData() || "{}");
      payload.requestId = marker;
      interceptedPayload = payload;
      await routeItem.continue({
        postData: JSON.stringify(payload),
        headers: { ...requestItem.headers(), "content-type": "application/json" },
      });
    });

    report.taskList = await selectTaskRow(page);
    const task = report.taskList.task;
    report.task = task;
    const dialogResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes("/msService/WOM/printManage/printDate/generateCode") &&
        response.request().method() === "GET",
      { timeout: timeoutMs },
    );
    await page.locator("#btn-generateCode[data-adp-wom-qrcode='true']").click();
    const dialogResponse = await dialogResponsePromise;
    const qrFrame = await waitForQrFrame(page);
    await qrFrame.locator("#generateForm").waitFor({ state: "visible", timeout: timeoutMs });
    await qrFrame.waitForFunction(() => Boolean(document.getElementById("manufactureDate").value), null, {
      timeout: timeoutMs,
    });

    report.dialog = {
      status: dialogResponse.status(),
      url: qrFrame.url(),
      title: await qrFrame.title(),
      context: {
        taskNo: await qrFrame.locator("#taskNo").innerText(),
        batchNo: await qrFrame.locator("#batchNo").innerText(),
        productCode: await qrFrame.locator("#productCode").innerText(),
        productName: await qrFrame.locator("#productName").innerText(),
      },
    };

    await qrFrame.locator("#manufactureDate").fill(manufactureDate);
    await qrFrame.locator("#manufactureDate").press("Tab");
    await page.waitForTimeout(300);
    await qrFrame.locator("#expiryDate").fill(expiryDate);
    await qrFrame.locator("#printCount").fill("2");

    const generateResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes("/msService/WOM/printManage/generateQrCode") &&
        response.request().method() === "POST",
      { timeout: timeoutMs },
    );
    await qrFrame.locator("#generateButton").click();
    const generateResponse = await generateResponsePromise;
    const parsedResponse = await readJson(generateResponse);
    await qrFrame.locator("#results.visible").waitFor({ state: "visible", timeout: timeoutMs });
    await qrFrame.waitForFunction(
      () => {
        const images = Array.from(document.querySelectorAll(".qr-item img"));
        return images.length === 2 && images.every((image) => image.complete && image.naturalWidth > 0);
      },
      null,
      { timeout: timeoutMs },
    );

    report.submission = {
      payload: interceptedPayload,
      httpStatus: generateResponse.status(),
      responseCode: parsedResponse.json && parsedResponse.json.code,
      responseData: parsedResponse.json && parsedResponse.json.data,
      statusText: await qrFrame.locator("#status").innerText(),
      resultCount: await qrFrame.locator("#resultCount").innerText(),
      qrCards: await qrFrame.locator(".qr-item").count(),
      images: await qrFrame.locator(".qr-item img").evaluateAll((images) =>
        images.map((image) => ({
          src: image.src,
          complete: image.complete,
          naturalWidth: image.naturalWidth,
          naturalHeight: image.naturalHeight,
        })),
      ),
    };
    await qrFrame.locator(".qr-item").first().scrollIntoViewIfNeeded();
    await page.waitForTimeout(200);
    ensureDir(screenshotPath);
    await page.screenshot({ path: screenshotPath, fullPage: true });
    report.screenshot = screenshotPath;
    await context.close();
  } finally {
    await browser.close();
  }
}

function detailMatchesContract(detail, task) {
  const fields = String(detail || "").split(",");
  return (
    fields.length === 6 &&
    fields[0] === task.batchNo &&
    /^991229\d{5}$/.test(fields[1]) &&
    fields[2] === task.productCode &&
    fields[3] === manufactureDate &&
    fields[4] === expiryDate &&
    fields[5] === "G0001"
  );
}

async function main() {
  const generatedAt = new Date();
  const marker = `ADP-WOM-UI-E2E-${generatedAt.toISOString().replace(/[-:.TZ]/g, "").slice(0, 14)}-${process.pid}`;
  const beforeSequence = sequenceSnapshot();
  const report = {
    schemaVersion: 1,
    reportKind: "wom-qrcode-browser-persistence-acceptance",
    generatedAt: generatedAt.toISOString(),
    database: "PostgreSQL",
    baseUrl,
    browserBaseUrl,
    route,
    marker,
    task: null,
    manufactureDate,
    expiryDate,
    beforeSequence,
    login: null,
    taskList: null,
    dialog: null,
    submission: null,
    databaseRows: [],
    rowsAfterCleanup: null,
    checks: [],
    network: [],
    failedResponses: [],
    requestFailures: [],
    console: [],
    pageErrors: [],
    screenshot: screenshotPath,
    status: "RUNNING",
    issues: [],
  };
  const api = await request.newContext({ ignoreHTTPSErrors: true });
  let failure = null;

  function check(name, condition, evidence) {
    report.checks.push({ name, status: condition ? "PASS" : "FAIL", evidence });
    if (!condition) throw new Error(`${name}: ${evidence}`);
  }

  try {
    const loginResult = await login(api);
    report.login = { username, status: loginResult.status, tokenCaptured: true };
    await runBrowser(loginResult.ticket, marker, report);
    const task = report.task;
    report.databaseRows = markerRows(marker);

    check(
      "WOM task selected",
      report.taskList && report.taskList.selection && report.taskList.selection.ok,
      JSON.stringify(report.taskList && report.taskList.selection),
    );
    check(
      "QR dialog opened from toolbar",
      report.dialog && report.dialog.status === 200 && /二维码生成/.test(report.dialog.title || ""),
      `HTTP ${report.dialog && report.dialog.status}, title=${report.dialog && report.dialog.title}`,
    );
    check(
      "Task context carried into dialog",
      report.dialog.context.taskNo === task.tableNo &&
        report.dialog.context.batchNo === task.batchNo &&
        report.dialog.context.productCode === task.productCode,
      JSON.stringify(report.dialog.context),
    );
    check(
      "Browser submit contract",
      report.submission.payload &&
        report.submission.payload.requestId === marker &&
        String(report.submission.payload.taskId) === String(task.id) &&
        report.submission.payload.manuDate === manufactureDate &&
        report.submission.payload.approveDate === expiryDate &&
        Number(report.submission.payload.printCount) === 2,
      JSON.stringify(report.submission.payload),
    );
    check(
      "Browser generate response",
      report.submission.httpStatus === 200 &&
        report.submission.responseCode === 200 &&
        Array.isArray(report.submission.responseData) &&
        report.submission.responseData.length === 2,
      `HTTP ${report.submission.httpStatus}, code=${report.submission.responseCode}`,
    );
    check(
      "QR visual render",
      report.submission.qrCards === 2 &&
        report.submission.images.length === 2 &&
        report.submission.images.every((image) => image.complete && image.naturalWidth > 0),
      JSON.stringify(report.submission.images),
    );
    check(
      "PostgreSQL browser persistence",
      report.databaseRows.length === 2 &&
        report.databaseRows.every((row) =>
          String(row[2]) === String(task.id) && detailMatchesContract(row[4], task),
        ),
      `wom_package_qrcodes rows=${report.databaseRows.length}`,
    );
    const targetFailures = report.failedResponses.filter((item) => /makeTaskList|printManage/.test(item.url));
    const targetRequestFailures = report.requestFailures.filter((item) => /makeTaskList|printManage/.test(item.url));
    check(
      "Frontend target network clean",
      targetFailures.length === 0 && targetRequestFailures.length === 0,
      `failedResponses=${JSON.stringify(targetFailures)}, requestFailures=${JSON.stringify(targetRequestFailures)}`,
    );
    check(
      "Frontend runtime clean",
      report.pageErrors.length === 0,
      `pageErrors=${JSON.stringify(report.pageErrors)}, consoleWarningsOrErrors=${report.console.length}`,
    );
  } catch (error) {
    failure = error;
    report.issues.push(error.stack || error.message);
  } finally {
    try {
      report.rowsAfterCleanup = cleanup(marker, beforeSequence);
      report.checks.push({
        name: "Marker cleanup and sequence restore",
        status: report.rowsAfterCleanup === 0 ? "PASS" : "FAIL",
        evidence: `remaining rows=${report.rowsAfterCleanup}, prior sequence=${JSON.stringify(beforeSequence)}`,
      });
      if (report.rowsAfterCleanup !== 0 && !failure) {
        failure = new Error(`Marker cleanup failed: ${report.rowsAfterCleanup} rows remain`);
      }
    } catch (cleanupError) {
      report.checks.push({
        name: "Marker cleanup and sequence restore",
        status: "FAIL",
        evidence: cleanupError.message,
      });
      if (!failure) failure = cleanupError;
      report.issues.push(cleanupError.stack || cleanupError.message);
    }
    await api.dispose();
  }

  report.status = failure ? "FAIL" : "PASS";
  report.summary = {
    status: report.status,
    pass: report.checks.filter((item) => item.status === "PASS").length,
    fail: report.checks.filter((item) => item.status === "FAIL").length,
    generatedRows: report.databaseRows.length,
    markerRowsAfterCleanup: report.rowsAfterCleanup,
  };
  report.verificationSql =
    "SELECT request_id, sequence_no, task_id, qr_code, detail, is_print, print_count, created_at " +
    "FROM public.wom_package_qrcodes WHERE tenant_id = 'default' AND request_id = :marker ORDER BY sequence_no;";

  ensureDir(outputPath);
  fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`, "utf8");
  console.log(`WOM QR browser persistence acceptance wrote ${outputPath} with status=${report.status}`);
  if (failure) throw failure;
}

main().catch((error) => {
  console.error(error.stack || error.message);
  process.exit(1);
});
