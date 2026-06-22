#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { execFileSync } = require("child_process");
const { chromium, request } = require("playwright");

const baseUrl = (process.env.ADP_BASE_URL || "http://100.99.133.43:18080").replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD || "123456";
const headless = process.env.ADP_HEADLESS !== "false";
const dbSshTarget = process.env.ADP_DB_SSH_TARGET || "v6@100.99.133.43";
const dbContainer = process.env.ADP_DB_CONTAINER || "adp-mes-newbase-postgres-1";
const dbName = process.env.ADP_DB_NAME || "adp";
const dbUser = process.env.ADP_DB_USER || "adp";
const nowToken = new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14);
const marker = process.env.ADP_E2E_MARKER || `ADP_E2E_${nowToken}_RMBATCH`;
const productCode = process.env.ADP_RM_PRODUCT_CODE || "ADP_E2E_20260615191905_WOMSTART_MAT";
const origin = process.env.ADP_RM_BATCH_ORIGIN || "formulaEnableFlw";
const batchFormulaId = process.env.ADP_RM_BATCH_FORMULA_ID || `${Date.now()}${process.pid % 1000}`;
const route = `/msService/RM/formula/formula/batchFormulaList?system=${encodeURIComponent(origin)}`;
const syncApi = "/msService/RM/formula/formula/batch/sync";
const deleteApi = `/msService/RM/formula/formula/batch/delete?batchFormulaId=${encodeURIComponent(batchFormulaId)}`;
const outputDir =
  process.env.ADP_OUTPUT_DIR || path.join("/tmp", `adp-rm-batch-sync-persistence-${nowToken}`);
const outputPath =
  process.env.ADP_RM_BATCH_SYNC_PERSISTENCE_OUTPUT ||
  path.join(outputDir, "rm-batch-sync-persistence-results.json");

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true });
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
    ["-o", "BatchMode=yes", "-o", "StrictHostKeyChecking=no", "-o", "UserKnownHostsFile=/dev/null", dbSshTarget, command],
    { input, encoding: "utf8", stdio: ["pipe", "pipe", "pipe"] }
  );
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
  return raw
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
  const response = await api.post(`${baseUrl}/inter-api/auth/login`, {
    data: { userName: username, password, clientId: "pc_dt" },
    headers: {
      Accept: "application/json, text/plain, */*",
      "Content-Type": "application/json;charset=UTF-8",
    },
  });
  const parsed = await readJsonSafe(response);
  const ticket = response.ok() ? findTicket(parsed.json) : null;
  if (!ticket) {
    throw new Error(`Login failed: ${response.status()} ${parsed.text.slice(0, 500)}`);
  }
  return { ticket, status: response.status() };
}

function formulaPayload() {
  return {
    ID: batchFormulaId,
    Name: `${marker}_FORMULA`,
    Version: "1",
    Status: "0",
    Product: productCode,
    ProductID: "-1",
    OrgID: "-1",
    Norsize: "100",
    Remark: marker,
    Units: [],
    Cells: [],
  };
}

function precheckSql() {
  return `
SELECT 'product', id, code, valid, coalesce(status::text, '') FROM public.baseset_materials WHERE code = ${sqlLiteral(productCode)};
SELECT 'other_system', id, code, valid, coalesce(status::text, ''), coalesce(system_type::text, '') FROM public.baseset_other_systems WHERE code = ${sqlLiteral(origin)};
SELECT 'deployment', id, process_key, is_current_version FROM public.wf_deployment WHERE process_key = ${sqlLiteral(origin)} ORDER BY id DESC LIMIT 5;
SELECT 'table', table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_name IN ('rm_formulas', 'rm_process_actives', 'rm_formula_processes') ORDER BY table_name;
`;
}

function syncVerificationSql() {
  return `
SELECT id, batch_formulaid, batch_status, formula_name, formula_edtion, batch_server_id, product_id, valid, status, coalesce(description, '')
FROM public.rm_formulas
WHERE batch_formulaid = ${sqlLiteral(batchFormulaId)}
ORDER BY id DESC;
SELECT count(*) FROM public.rm_process_actives WHERE formula_id IN (SELECT id FROM public.rm_formulas WHERE batch_formulaid = ${sqlLiteral(batchFormulaId)});
`;
}

function deleteVerificationSql() {
  return `
SELECT id, batch_formulaid, valid, status, coalesce(description, '')
FROM public.rm_formulas
WHERE batch_formulaid = ${sqlLiteral(batchFormulaId)}
ORDER BY id DESC;
SELECT count(*) FROM public.rm_process_actives WHERE formula_id IN (SELECT id FROM public.rm_formulas WHERE batch_formulaid = ${sqlLiteral(batchFormulaId)} AND valid = true);
`;
}

function fetchOptions(method, apiPath, data) {
  return {
    method,
    headers: {
      Accept: "application/json, text/plain, */*",
      "Content-Type": "application/json;charset=UTF-8",
    },
    body: data === undefined ? undefined : JSON.stringify(data),
  };
}

async function readBrowserApiResponse(response) {
  const text = await response.text();
  return { status: response.status(), ok: response.ok(), body: text };
}

async function readResponseTextSample(response, timeoutMs = 3000) {
  let timeoutId;
  try {
    return await Promise.race([
      response.text(),
      new Promise((resolve) => {
        timeoutId = setTimeout(() => resolve(""), timeoutMs);
      }),
    ]);
  } finally {
    if (timeoutId) {
      clearTimeout(timeoutId);
    }
  }
}

async function runBrowser(ticket, evidence) {
  const browser = await chromium.launch({ headless });
  try {
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
      window.localStorage.clear();
      window.sessionStorage.clear();
      ["suposTicket", "SUPOS_TICKET", "token", "ticket"].forEach((key) => {
        window.localStorage.setItem(key, token);
        window.sessionStorage.setItem(key, token);
      });
    }, ticket);

    const page = await context.newPage();
    page.on("console", (message) => {
      if (["error", "warning"].includes(message.type())) {
        evidence.console.push({ type: message.type(), text: message.text() });
      }
    });
    page.on("pageerror", (error) => evidence.pageErrors.push(error.message));
    page.on("requestfailed", (requestItem) => {
      evidence.requestFailures.push({
        method: requestItem.method(),
        url: requestItem.url(),
        failure: requestItem.failure() && requestItem.failure().errorText,
      });
    });
    page.on("request", (requestItem) => {
      const url = requestItem.url();
      if (/batchFormulaList|formula\/batch\/sync|formula\/batch\/delete|layoutJson|systemCodeJson/.test(url)) {
        evidence.requests.push({
          method: requestItem.method(),
          url,
          postData: requestItem.postData(),
        });
      }
    });
    page.on("response", async (response) => {
      const url = response.url();
      if (!/batchFormulaList|formula\/batch\/sync|formula\/batch\/delete|layoutJson|systemCodeJson/.test(url)) {
        return;
      }
      let body = "";
      try {
        body = (await readResponseTextSample(response)).slice(0, 4000);
      } catch (_error) {
        body = "";
      }
      evidence.responses.push({
        method: response.request().method(),
        url,
        status: response.status(),
        body,
      });
    });

    try {
      const nav = await page.goto(route, { waitUntil: "commit", timeout: 15000 });
      evidence.navigation = { route, status: nav && nav.status() };
    } catch (error) {
      evidence.navigation = { route, status: "BLOCKED", error: error.message };
    }
    evidence.screenshots.batchFormulaList = path.join(outputDir, "rm-batch-formula-list.png");
    try {
      await page.screenshot({ path: evidence.screenshots.batchFormulaList, fullPage: true, timeout: 10000 });
    } catch (error) {
      evidence.screenshots.batchFormulaListError = error.message;
    }

    const syncBody = {
      origin,
      mqValue: JSON.stringify(formulaPayload()),
    };
    evidence.sync.request = { method: "POST", api: syncApi, payload: syncBody };
    const syncResponse = await context.request.post(syncApi, {
      data: syncBody,
      headers: fetchOptions("POST", syncApi, syncBody).headers,
      timeout: 90000,
    });
    evidence.sync.response = await readBrowserApiResponse(syncResponse);

    evidence.sync.verificationRaw = runSql(syncVerificationSql());
    evidence.sync.verificationRows = parseRows(evidence.sync.verificationRaw);
    const formulaRows = evidence.sync.verificationRows.filter((row) => row.length >= 10);
    evidence.sync.persistence = {
      formulaRows,
      status:
        evidence.sync.response.ok &&
        /"success"\s*:\s*true/.test(evidence.sync.response.body) &&
        formulaRows.some(
          (row) =>
            row[1] === batchFormulaId &&
            row[7] === "t" &&
            row[8] === "88" &&
            (row[3].includes(marker) || row[9].includes(marker))
        )
          ? "PASS"
          : "FAIL",
    };

    if (formulaRows.length) {
      evidence.delete.request = { method: "POST", api: deleteApi };
      const deleteResponse = await context.request.post(deleteApi, {
        headers: fetchOptions("POST", deleteApi).headers,
        timeout: 90000,
      });
      evidence.delete.response = await readBrowserApiResponse(deleteResponse);
      evidence.delete.verificationRaw = runSql(deleteVerificationSql());
      evidence.delete.verificationRows = parseRows(evidence.delete.verificationRaw);
      const deleteRows = evidence.delete.verificationRows.filter((row) => row.length >= 5);
      evidence.delete.persistence = {
        formulaRows: deleteRows,
        status:
          evidence.delete.response.ok &&
          /"dealSuccessFlag"\s*:\s*true/.test(evidence.delete.response.body) &&
          deleteRows.some((row) => row[1] === batchFormulaId && row[2] === "f" && row[3] === "0")
            ? "PASS"
            : "FAIL",
      };
    } else {
      evidence.delete.persistence = { status: "BLOCKED", reason: "sync did not create rm_formulas row" };
    }

    evidence.screenshots.afterActions = path.join(outputDir, "rm-batch-formula-after-actions.png");
    try {
      await page.screenshot({ path: evidence.screenshots.afterActions, fullPage: true, timeout: 10000 });
    } catch (error) {
      evidence.screenshots.afterActionsError = error.message;
    }
  } finally {
    await Promise.race([
      browser.close(),
      new Promise((resolve) => {
        setTimeout(resolve, 10000);
      }),
    ]);
  }
}

async function main() {
  ensureDir(outputDir);
  const evidence = {
    generatedAt: new Date().toISOString(),
    baseUrl,
    route,
    marker,
    origin,
    batchFormulaId,
    productCode,
    console: [],
    pageErrors: [],
    requestFailures: [],
    requests: [],
    responses: [],
    screenshots: {},
    sync: {},
    delete: {},
  };

  try {
    evidence.precheck = {
      sql: precheckSql().trim(),
      result: runSql(precheckSql()),
    };
    if (!evidence.precheck.result.includes(`product|`) || !evidence.precheck.result.includes(`other_system|`)) {
      throw new Error(`RM batch precheck failed: ${evidence.precheck.result}`);
    }

    const api = await request.newContext({ ignoreHTTPSErrors: true });
    const loginResult = await login(api);
    await api.dispose();
    evidence.login = { status: loginResult.status, ticket: Boolean(loginResult.ticket) };

    await runBrowser(loginResult.ticket, evidence);

    evidence.summary = {
      sync: evidence.sync.persistence && evidence.sync.persistence.status,
      delete: evidence.delete.persistence && evidence.delete.persistence.status,
      frontendConsoleErrors: evidence.console.filter((item) => item.type === "error").length,
      pageErrors: evidence.pageErrors.length,
      requestFailures: evidence.requestFailures.length,
      status:
        evidence.sync.persistence &&
        evidence.sync.persistence.status === "PASS" &&
        evidence.delete.persistence &&
        evidence.delete.persistence.status === "PASS"
          ? "PASS"
          : "FAIL",
    };
  } catch (error) {
    evidence.summary = { status: "FAIL", error: error.stack || error.message };
  } finally {
    fs.writeFileSync(outputPath, JSON.stringify(evidence, null, 2));
    console.log(JSON.stringify({ outputPath, summary: evidence.summary }, null, 2));
  }

  if (!evidence.summary || evidence.summary.status !== "PASS") {
    process.exitCode = 1;
  }
}

main().catch((error) => {
  console.error(error.stack || error.message);
  process.exitCode = 1;
});
