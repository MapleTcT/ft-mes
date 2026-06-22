#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { execFileSync, spawnSync } = require("child_process");
const { chromium, request } = require("playwright");

const repoRoot = path.resolve(__dirname, "../../..");
const stamp = new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14);
const baseUrl = (process.env.ADP_BASE_URL || "http://100.99.133.43:18080").replace(/\/+$/, "");
const browserBaseUrl = (process.env.ADP_BROWSER_BASE_URL || baseUrl).replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD || "123456";
const outputPath =
  process.env.ADP_RUNTIME_CONFIG_SMOKE_OUTPUT ||
  path.join("/tmp", `adp-runtime-configuration-readiness-${stamp}.json`);
const dbSshTarget = process.env.ADP_DB_SSH_TARGET || "v6@100.99.133.43";
const dbContainer = process.env.ADP_DB_CONTAINER || "adp-mes-newbase-postgres-1";
const dbName = process.env.ADP_DB_NAME || "adp";
const dbUser = process.env.ADP_DB_USER || "adp";
const headless = process.env.ADP_HEADLESS !== "false";

const apiChecks = [
  {
    name: "custom-property-tree",
    method: "GET",
    path: "/inter-api/customProperty/tree?type=module",
    minNodes: 1,
  },
  {
    name: "service-manager-apps",
    method: "GET",
    path: "/msService/servicemanager/msModule/supos/app/list",
    minNodes: 1,
  },
  {
    name: "ec-engine-datalist",
    method: "GET",
    path: "/msService/ec/engine/datalist",
  },
  {
    name: "ec-engine-msManage",
    method: "GET",
    path: "/msService/ec/engine/msManage",
    html: true,
  },
  {
    name: "ec-module-msManage",
    method: "GET",
    path: "/msService/ec/module/msManage",
    html: true,
  },
];

const browserPages = [
  { name: "ec-engine-msManage", path: "/msService/ec/engine/msManage" },
  { name: "ec-module-msManage", path: "/msService/ec/module/msManage" },
  { name: "supplant-service-config", path: "/supplant/#/serviceConfig" },
  { name: "supplant-app-config", path: "/supplant/#/appConfig" },
];

const visibleErrorPattern =
  /(数据库操作异常|系统错误|系统异常|发生未知异常|SQLGrammarException|could not extract ResultSet|column .* does not exist|relation .* does not exist|500 INTERNAL|\b[\w.]+Exception(?::|\s+at\b)|Invalid bound statement)/i;

function ensureDir(filePath) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
}

function getRepoCommit() {
  const result = spawnSync("git", ["rev-parse", "HEAD"], {
    cwd: repoRoot,
    encoding: "utf8",
  });
  return result.status === 0 ? result.stdout.trim() : "UNKNOWN";
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

async function readJsonSafe(response) {
  const text = await response.text();
  try {
    return { json: JSON.parse(text), text };
  } catch (_error) {
    return { json: null, text };
  }
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
    const parsed = await readJsonSafe(response);
    const ticket = response.ok() ? findTicket(parsed.json) : null;
    if (ticket) {
      return ticket;
    }
    errors.push({ status: response.status(), body: parsed.text.slice(0, 500) });
  }
  throw new Error(`Login failed for ${username}: ${JSON.stringify(errors)}`);
}

function countJsonNodes(value) {
  if (Array.isArray(value)) {
    return value.reduce((total, item) => total + countJsonNodes(item), value.length);
  }
  if (value && typeof value === "object") {
    return Object.values(value).reduce((total, item) => total + countJsonNodes(item), 1);
  }
  return 0;
}

function shellQuote(value) {
  return `'${String(value).replace(/'/g, "'\\''")}'`;
}

function runSql(sql) {
  const remoteCommand = [
    "docker",
    "exec",
    dbContainer,
    "psql",
    "-U",
    dbUser,
    "-d",
    dbName,
    "-v",
    "ON_ERROR_STOP=1",
    "-AtF",
    "|",
    "-c",
    sql,
  ]
    .map(shellQuote)
    .join(" ");
  return execFileSync("ssh", ["-o", "BatchMode=yes", "-o", "ConnectTimeout=8", dbSshTarget, remoteCommand], {
    encoding: "utf8",
    stdio: ["ignore", "pipe", "pipe"],
  }).trim();
}

function parseRows(output, columns) {
  if (!output) {
    return [];
  }
  return output.split(/\r?\n/).map((line) => {
    const values = line.split("|");
    return Object.fromEntries(columns.map((column, index) => [column, values[index] || ""]));
  });
}

function responseHasError(text) {
  return visibleErrorPattern.test(String(text || ""));
}

async function runApiCheck(api, ticket, check) {
  const response = await api.fetch(`${baseUrl}${check.path}`, {
    method: check.method,
    headers: {
      Accept: "application/json, text/plain, */*",
      Authorization: `Bearer ${ticket}`,
      Cookie: `suposTicket=${encodeURIComponent(ticket)}`,
      langu_code: "zh_CN",
    },
  });
  const parsed = await readJsonSafe(response);
  const contentType = response.headers()["content-type"] || "";
  const nodeCount = parsed.json ? countJsonNodes(parsed.json) : 0;
  const htmlOk = check.html ? /text\/html/i.test(contentType) || /<html|<body/i.test(parsed.text) : true;
  const jsonOk = check.minNodes ? nodeCount >= check.minNodes : true;
  const bodyOk = check.html ? true : !responseHasError(parsed.text);
  const ok = response.status() < 400 && htmlOk && jsonOk && bodyOk;
  return {
    name: check.name,
    method: check.method,
    path: check.path,
    status: response.status(),
    contentType,
    nodeCount,
    ok,
    bodySnippet: ok ? undefined : parsed.text.slice(0, 1000),
  };
}

async function openPage(browser, ticket, target) {
  const context = await browser.newContext({ ignoreHTTPSErrors: true });
  const browserHost = new URL(browserBaseUrl).hostname;
  await context.addCookies([
    {
      name: "suposTicket",
      value: ticket,
      domain: browserHost,
      path: "/",
      httpOnly: false,
      secure: browserBaseUrl.startsWith("https://"),
      sameSite: "Lax",
    },
  ]);
  const page = await context.newPage();
  const networkErrors = [];
  const consoleErrors = [];
  const pageErrors = [];
  const requestFailures = [];
  page.on("console", (message) => {
    if (message.type() === "error") {
      consoleErrors.push(message.text());
    }
  });
  page.on("pageerror", (error) => pageErrors.push(error.message));
  page.on("requestfailed", (request) => {
    requestFailures.push({
      url: request.url(),
      method: request.method(),
      failure: request.failure() && request.failure().errorText,
    });
  });
  page.on("response", async (response) => {
    const type = response.request().resourceType();
    if (!["document", "xhr", "fetch", "script", "stylesheet"].includes(type)) {
      return;
    }
    const contentType = response.headers()["content-type"] || "";
    const htmlAssetFallback =
      ["script", "stylesheet"].includes(type) && response.status() < 400 && /text\/html/i.test(contentType);
    if (response.status() < 400 && !htmlAssetFallback) {
      return;
    }
    let body = "";
    try {
      body = (await response.text()).slice(0, 300);
    } catch (_error) {
      body = "";
    }
    networkErrors.push({
      status: response.status(),
      type,
      url: response.url(),
      contentType,
      body,
    });
  });

  let navigationStatus = null;
  let navigationError = null;
  const url = `${browserBaseUrl}${target.path}`;
  try {
    const response = await page.goto(url, { waitUntil: "domcontentloaded", timeout: 45000 });
    navigationStatus = response ? response.status() : null;
    await page.evaluate(
      (token) => {
        window.localStorage.setItem("suposTicket", token);
        window.localStorage.setItem("SUPOS_TICKET", token);
        window.localStorage.setItem("token", token);
      },
      ticket
    );
    await page.waitForTimeout(2000);
  } catch (error) {
    navigationError = error.message;
  }
  const bodyText = navigationError ? "" : (await page.locator("body").innerText({ timeout: 5000 }).catch(() => ""));
  await context.close();
  const visibleError = bodyText
    .split(/\r?\n/)
    .map((line) => line.trim())
    .find((line) => line && visibleErrorPattern.test(line));
  return {
    name: target.name,
    path: target.path,
    url,
    navigationStatus,
    navigationError,
    visibleError: visibleError || null,
    networkErrors,
    consoleErrors,
    pageErrors,
    requestFailures,
    ok:
      navigationStatus !== null &&
      navigationStatus < 400 &&
      !navigationError &&
      !visibleError &&
      networkErrors.length === 0 &&
      consoleErrors.length === 0 &&
      pageErrors.length === 0 &&
      requestFailures.length === 0,
  };
}

function collectDatabaseEvidence() {
  const countSql = [
    "select 'ec_entity', count(*) from public.ec_entity",
    "union all select 'ec_model', count(*) from public.ec_model",
    "union all select 'ec_field', count(*) from public.ec_field",
    "union all select 'ec_view', count(*) from public.ec_view",
    "union all select 'ec_module', count(*) from public.ec_module",
    "union all select 'runtime_view', count(*) from public.runtime_view",
    "union all select 'runtime_extra_view', count(*) from public.runtime_extra_view",
    "union all select 'runtime_button', count(*) from public.runtime_button",
    "union all select 'rbac_menuinfo_ec', count(*) from public.rbac_menuinfo where code::text in ('ec_entityConfig','ec_engineManage','ms_ec_projectManage','ec_ManageMs','appManage') or coalesce(url::text,'') like '/msService/ec/%' or coalesce(url::text,'') like '/supplant/#/%Config'",
    "union all select 'runtime_extra_oid', count(*) from information_schema.columns where table_schema='public' and table_name='runtime_extra_view' and column_name='view_json' and udt_name='oid'",
  ].join(" ");
  const menuSql = [
    "select id::text, code::text, coalesce(name::text,''), coalesce(name_display::text,''),",
    "coalesce(url::text,''), coalesce(route::text,'')",
    "from public.rbac_menuinfo",
    "where code::text in ('ec_entityConfig','ec_engineManage','ms_ec_projectManage','ec_ManageMs','appManage')",
    "or coalesce(url::text,'') like '/msService/ec/%'",
    "or coalesce(url::text,'') like '/supplant/#/%Config'",
    "order by code::text",
    "limit 40",
  ].join(" ");
  const runtimeSql = [
    "select code::text, coalesce(title::text,''), coalesce(display_name::text,''),",
    "coalesce(url::text,''), coalesce(module_code::text,''), coalesce(entity_code::text,''), coalesce(extra_view::text,'')",
    "from public.runtime_view",
    "order by code::text",
    "limit 40",
  ].join(" ");
  const ecViewSql = [
    "select code::text, coalesce(title::text,''), coalesce(display_name::text,''),",
    "coalesce(url::text,''), coalesce(module_code::text,''), coalesce(entity_code::text,''), coalesce(extra_view::text,'')",
    "from public.ec_view",
    "order by code::text",
    "limit 40",
  ].join(" ");
  const counts = parseRows(runSql(countSql), ["name", "count"]).map((row) => ({
    name: row.name,
    count: Number(row.count || 0),
  }));
  return {
    countSql,
    menuSql,
    runtimeSql,
    ecViewSql,
    counts,
    menuRows: parseRows(runSql(menuSql), ["id", "code", "name", "nameDisplay", "url", "route"]),
    ecViewRows: parseRows(runSql(ecViewSql), [
      "code",
      "title",
      "displayName",
      "url",
      "moduleCode",
      "entityCode",
      "extraView",
    ]),
    runtimeRows: parseRows(runSql(runtimeSql), [
      "code",
      "title",
      "displayName",
      "url",
      "moduleCode",
      "entityCode",
      "extraView",
    ]),
  };
}

function buildChecks(apiResults, browserResults, databaseEvidence) {
  const countByName = Object.fromEntries(databaseEvidence.counts.map((row) => [row.name, row.count]));
  const requiredCounts = {
    ec_entity: 1,
    ec_model: 1,
    ec_field: 1,
    ec_view: 1,
    ec_module: 1,
    runtime_view: 1,
    runtime_extra_view: 1,
    runtime_button: 1,
    rbac_menuinfo_ec: 3,
    runtime_extra_oid: 1,
  };
  const dbOk = Object.entries(requiredCounts).every(([name, min]) => (countByName[name] || 0) >= min);
  return [
    {
      name: "configuration-api-readiness",
      status: apiResults.every((result) => result.ok) ? "PASS" : "FAIL",
      evidence: `${apiResults.filter((result) => result.ok).length}/${apiResults.length} API/page endpoints passed`,
    },
    {
      name: "configuration-browser-pages",
      status: browserResults.every((result) => result.ok) ? "PASS" : "FAIL",
      evidence: `${browserResults.filter((result) => result.ok).length}/${browserResults.length} browser pages passed`,
    },
    {
      name: "configuration-postgres-metadata",
      status: dbOk ? "PASS" : "FAIL",
      evidence: Object.entries(requiredCounts)
        .map(([name, min]) => `${name}=${countByName[name] || 0}>=${min}`)
        .join("; "),
    },
    {
      name: "configuration-menu-metadata",
      status: databaseEvidence.menuRows.length >= 3 ? "PASS" : "FAIL",
      evidence: `menuRows=${databaseEvidence.menuRows.length}`,
    },
    {
      name: "configuration-ec-view-samples",
      status: databaseEvidence.ecViewRows.length > 0 ? "PASS" : "FAIL",
      evidence: `ecViewRows=${databaseEvidence.ecViewRows.length}`,
    },
    {
      name: "configuration-runtime-view-samples",
      status: databaseEvidence.runtimeRows.length > 0 ? "PASS" : "FAIL",
      evidence: `runtimeRows=${databaseEvidence.runtimeRows.length}`,
    },
  ];
}

async function main() {
  ensureDir(outputPath);
  const api = await request.newContext({ ignoreHTTPSErrors: true });
  const ticket = await login(api);
  const apiResults = [];
  for (const check of apiChecks) {
    const result = await runApiCheck(api, ticket, check);
    apiResults.push(result);
    console.log(`${result.ok ? "OK" : "FAIL"} ${result.name} status=${result.status} nodes=${result.nodeCount}`);
  }
  await api.dispose();

  const browser = await chromium.launch({ headless });
  const browserResults = [];
  for (const target of browserPages) {
    const result = await openPage(browser, ticket, target);
    browserResults.push(result);
    console.log(`${result.ok ? "OK" : "FAIL"} browser ${result.name} status=${result.navigationStatus}`);
  }
  await browser.close();

  const databaseEvidence = collectDatabaseEvidence();
  const checks = buildChecks(apiResults, browserResults, databaseEvidence);
  const failedChecks = checks.filter((check) => check.status !== "PASS");
  const report = {
    schemaVersion: 1,
    generatedAt: new Date().toISOString(),
    repoCommit: getRepoCommit(),
    database: "PostgreSQL",
    module: "basic-config",
    areaId: "configuration-entity-runtime",
    status: failedChecks.length === 0 ? "PASS" : "FAIL",
    baseUrl,
    browserBaseUrl,
    route: "configuration/entity runtime pages",
    summary: {
      status: failedChecks.length === 0 ? "PASS" : "FAIL",
      apiChecks: apiResults.length,
      apiPassed: apiResults.filter((result) => result.ok).length,
      browserPages: browserResults.length,
      browserPassed: browserResults.filter((result) => result.ok).length,
      browserErrors: browserResults.reduce(
        (total, result) =>
          total +
          result.networkErrors.length +
          result.consoleErrors.length +
          result.pageErrors.length +
          result.requestFailures.length +
          (result.visibleError ? 1 : 0) +
          (result.navigationError ? 1 : 0),
        0
      ),
      ecEntityRows: databaseEvidence.counts.find((row) => row.name === "ec_entity")?.count || 0,
      ecModelRows: databaseEvidence.counts.find((row) => row.name === "ec_model")?.count || 0,
      ecFieldRows: databaseEvidence.counts.find((row) => row.name === "ec_field")?.count || 0,
      ecViewRows: databaseEvidence.counts.find((row) => row.name === "ec_view")?.count || 0,
      runtimeViewRows: databaseEvidence.counts.find((row) => row.name === "runtime_view")?.count || 0,
      runtimeExtraViewRows: databaseEvidence.counts.find((row) => row.name === "runtime_extra_view")?.count || 0,
      runtimeButtonRows: databaseEvidence.counts.find((row) => row.name === "runtime_button")?.count || 0,
    },
    checks,
    apiResults,
    browserResults,
    databaseEvidence,
    evidence:
      "Read-only smoke. It validates current configuration/entity runtime API, page, menu and PostgreSQL metadata availability only; it does not create, edit or delete configuration models.",
  };
  fs.writeFileSync(outputPath, JSON.stringify(report, null, 2) + "\n");
  if (failedChecks.length > 0) {
    console.error(`FAIL runtime configuration readiness checks=${failedChecks.map((check) => check.name).join(", ")}`);
    console.error(`REPORT ${outputPath}`);
    process.exitCode = 1;
    return;
  }
  console.log(`PASS runtime configuration readiness api=${report.summary.apiPassed}/${report.summary.apiChecks} browser=${report.summary.browserPassed}/${report.summary.browserPages}`);
  console.log(`REPORT ${outputPath}`);
}

main().catch((error) => {
  console.error(error && error.stack ? error.stack : String(error));
  process.exitCode = 1;
});
