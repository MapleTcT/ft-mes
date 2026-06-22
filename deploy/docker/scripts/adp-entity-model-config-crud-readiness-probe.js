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
  process.env.ENTITY_MODEL_CONFIG_CRUD_READINESS_OUTPUT ||
  path.join("/tmp", `adp-entity-model-config-crud-readiness-${stamp}.json`);
const dbSshTarget = process.env.ADP_DB_SSH_TARGET || "v6@100.99.133.43";
const dbContainer = process.env.ADP_DB_CONTAINER || "adp-mes-newbase-postgres-1";
const dbName = process.env.ADP_DB_NAME || "adp";
const dbUser = process.env.ADP_DB_USER || "adp";
const headless = process.env.ADP_HEADLESS !== "false";

const visibleErrorPattern =
  /(数据库操作异常|系统错误|系统异常|发生未知异常|SQLGrammarException|could not extract ResultSet|column .* does not exist|relation .* does not exist|500 INTERNAL|org\.hibernate\.[\w.]+Exception|org\.springframework\.[\w.]+Exception|java\.lang\.[\w.]+Exception|Invalid bound statement)/i;
const htmlServerErrorPattern =
  /(SQLGrammarException|could not extract ResultSet|column .* does not exist|relation .* does not exist|500 INTERNAL|Invalid bound statement)/i;
const legacyTemplateGlobalPattern = /^(YAHOO|CUI|\$|jQuery|foundation) is not defined$/;

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

function countJsonNodes(value) {
  if (Array.isArray(value)) {
    return value.reduce((total, item) => total + countJsonNodes(item), value.length);
  }
  if (value && typeof value === "object") {
    return Object.values(value).reduce((total, item) => total + countJsonNodes(item), 1);
  }
  return 0;
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

async function readTextAndJson(response) {
  const text = await response.text();
  try {
    return { text, json: JSON.parse(text) };
  } catch (_error) {
    return { text, json: null };
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
    const parsed = await readTextAndJson(response);
    const ticket = response.ok() ? findTicket(parsed.json) : null;
    if (ticket) {
      return ticket;
    }
    errors.push({ status: response.status(), bodySnippet: parsed.text.slice(0, 400) });
  }
  throw new Error(`Login failed for ${username}: ${JSON.stringify(errors)}`);
}

function responseHasError(text) {
  return visibleErrorPattern.test(String(text || ""));
}

function makePath(pathname, params = {}) {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== "") {
      search.set(key, String(value));
    }
  }
  const query = search.toString();
  return query ? `${pathname}?${query}` : pathname;
}

async function runApiCheck(api, ticket, check) {
  const response = await api.fetch(`${baseUrl}${check.path}`, {
    method: check.method || "GET",
    headers: {
      Accept: check.html ? "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8" : "application/json, text/plain, */*",
      Cookie: `suposTicket=${encodeURIComponent(ticket)}`,
      Authorization: `Bearer ${ticket}`,
      langu_code: "zh_CN",
    },
  });
  const parsed = await readTextAndJson(response);
  const contentType = response.headers()["content-type"] || "";
  const nodeCount = parsed.json ? countJsonNodes(parsed.json) : 0;
  const htmlOk = check.html ? /text\/html/i.test(contentType) || /<html|<body|<!doctype/i.test(parsed.text) : true;
  const jsonOk = check.minNodes ? nodeCount >= check.minNodes : true;
  const expectedTextOk = check.expectText ? parsed.text.includes(check.expectText) : true;
  const bodyOk = check.html ? !htmlServerErrorPattern.test(parsed.text) : !responseHasError(parsed.text);
  const ok = response.status() < 400 && htmlOk && jsonOk && expectedTextOk && bodyOk;
  return {
    name: check.name,
    method: check.method || "GET",
    path: check.path,
    status: response.status(),
    contentType,
    nodeCount,
    ok,
    responseShape: parsed.json
      ? Object.keys(parsed.json)
          .slice(0, 12)
          .join(",")
      : "text",
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
  await context.addInitScript((token) => {
    window.localStorage.setItem("suposTicket", token);
    window.localStorage.setItem("SUPOS_TICKET", token);
    window.localStorage.setItem("token", token);
  }, ticket);

  const page = await context.newPage();
  const consoleErrors = [];
  const pageErrors = [];
  const requestFailures = [];
  const networkErrors = [];
  page.on("console", (message) => {
    if (message.type() === "error") {
      consoleErrors.push(message.text());
    }
  });
  page.on("pageerror", (error) => pageErrors.push(error.message));
  page.on("requestfailed", (req) => {
    requestFailures.push({
      method: req.method(),
      url: req.url(),
      failure: req.failure() && req.failure().errorText,
    });
  });
  page.on("response", async (response) => {
    const resourceType = response.request().resourceType();
    if (!["document", "xhr", "fetch", "script", "stylesheet"].includes(resourceType)) {
      return;
    }
    const contentType = response.headers()["content-type"] || "";
    const htmlAssetFallback =
      ["script", "stylesheet"].includes(resourceType) && response.status() < 400 && /text\/html/i.test(contentType);
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
      resourceType,
      url: response.url(),
      contentType,
      body,
    });
  });

  const url = `${browserBaseUrl}${target.path}`;
  let navigationStatus = null;
  let navigationError = null;
  try {
    const response = await page.goto(url, { waitUntil: "domcontentloaded", timeout: 45000 });
    navigationStatus = response ? response.status() : null;
    await page.waitForTimeout(target.waitMs || 2000);
  } catch (error) {
    navigationError = error.message;
  }
  const bodyText = navigationError ? "" : await page.locator("body").innerText({ timeout: 5000 }).catch(() => "");
  await context.close();

  const visibleError = bodyText
    .split(/\r?\n/)
    .map((line) => line.trim())
    .find((line) => line && visibleErrorPattern.test(line));
  const allowedPageErrors = target.allowLegacyTemplateGlobalErrors
    ? pageErrors.filter((message) => legacyTemplateGlobalPattern.test(message))
    : [];
  const blockingPageErrors = pageErrors.filter((message) => !allowedPageErrors.includes(message));
  return {
    name: target.name,
    path: target.path,
    url,
    navigationStatus,
    navigationError,
    visibleError: visibleError || null,
    consoleErrors,
    pageErrors,
    allowedPageErrors,
    blockingPageErrors,
    warningClassification: allowedPageErrors.length > 0 ? "LEGACY_TEMPLATE_GLOBALS_WHEN_OPENED_DIRECTLY" : null,
    requestFailures,
    networkErrors,
    ok:
      navigationStatus !== null &&
      navigationStatus < 400 &&
      !navigationError &&
      !visibleError &&
      consoleErrors.length === 0 &&
      blockingPageErrors.length === 0 &&
      requestFailures.length === 0 &&
      networkErrors.length === 0,
  };
}

function collectDatabaseEvidence() {
  const countSql = [
    "select 'ec_module', count(*) from public.ec_module",
    "union all select 'ec_entity', count(*) from public.ec_entity",
    "union all select 'ec_model', count(*) from public.ec_model",
    "union all select 'ec_field', count(*) from public.ec_field",
    "union all select 'ec_view', count(*) from public.ec_view",
    "union all select 'runtime_view', count(*) from public.runtime_view",
    "union all select 'runtime_extra_view', count(*) from public.runtime_extra_view",
    "union all select 'runtime_button', count(*) from public.runtime_button",
    "union all select 'marker_ec_entity', count(*) from public.ec_entity where code::text like 'ADP_E2E_%_EC_%' or coalesce(name::text,'') like 'ADP_E2E_%_EC_%' or coalesce(description::text,'') like 'ADP_E2E_%_EC_%'",
    "union all select 'marker_ec_model', count(*) from public.ec_model where code::text like 'ADP_E2E_%_EC_%' or coalesce(name::text,'') like 'ADP_E2E_%_EC_%' or coalesce(description::text,'') like 'ADP_E2E_%_EC_%'",
  ].join(" ");
  const sampleSql = [
    "select m.code::text, coalesce(m.name::text,''),",
    "e.code::text, coalesce(e.name::text,''), coalesce(e.entity_name::text,''), coalesce(e.version::text,'0'),",
    "mo.code::text, coalesce(mo.name::text,''), coalesce(mo.model_name::text,''), coalesce(mo.table_name::text,''), coalesce(mo.version::text,'0')",
    "from public.ec_module m",
    "join public.ec_entity e on e.module_code = m.code",
    "join public.ec_model mo on mo.entity_code = e.code",
    "where coalesce(m.valid,1) <> 0",
    "and coalesce(e.valid,1) <> 0",
    "and coalesce(mo.valid,1) <> 0",
    "and e.code::text not like 'ADP_E2E_%'",
    "and mo.code::text not like 'ADP_E2E_%'",
    "order by m.code::text, e.code::text, mo.code::text",
    "limit 1",
  ].join(" ");
  const columnSql = [
    "select table_name, column_name, data_type",
    "from information_schema.columns",
    "where table_schema = 'public'",
    "and table_name in ('ec_entity','ec_model','ec_field','ec_view','runtime_view','runtime_extra_view','runtime_button')",
    "order by table_name, ordinal_position",
  ].join(" ");
  const counts = parseRows(runSql(countSql), ["name", "count"]).map((row) => ({
    name: row.name,
    count: Number(row.count || 0),
  }));
  const sampleRows = parseRows(runSql(sampleSql), [
    "moduleCode",
    "moduleName",
    "entityCode",
    "entityName",
    "entityTechnicalName",
    "entityVersion",
    "modelCode",
    "modelName",
    "modelTechnicalName",
    "tableName",
    "modelVersion",
  ]);
  const columns = parseRows(runSql(columnSql), ["tableName", "columnName", "dataType"]);
  return {
    verificationSql: {
      counts: countSql,
      sample: sampleSql,
      columns: columnSql,
      futureCreateMarker:
        "select code, name, description from public.ec_entity where code::text like 'ADP_E2E_%_EC_CREATE%' or name::text like 'ADP_E2E_%_EC_CREATE%'",
      futureEditMarker:
        "select code, name, description from public.ec_entity where code::text like 'ADP_E2E_%_EC_EDIT%' or description::text like 'ADP_E2E_%_EC_EDIT%'",
      futureDeleteMarker:
        "select code, valid, delete_time from public.ec_entity where code::text like 'ADP_E2E_%_EC_DELETE%'",
    },
    counts,
    sample: sampleRows[0] || null,
    columns,
  };
}

function backendTrace() {
  return {
    entityCreateEdit: {
      apiEndpoint: "/msService/ec/entity/save",
      expectedMethod: "POST from frontend form; Spring @RequestMapping also accepts method-less requests",
      controller: "configuration-services-open-api/.../controller/EntityController.java save(HttpServletRequest)",
      dto: "DtoUtils.getEntity(HttpServletRequest)",
      service: "EntityService.saveEntity(Entity)",
      persistence: "Hibernate/scaffold DAO layer in configuration-services-service",
      targetTables: ["ec_entity", "ec_model", "ec_field", "ec_view", "runtime_view", "runtime_extra_view"],
    },
    entityDelete: {
      apiEndpoint: "/msService/ec/entity/ordinaryDelete and /msService/ec/entity/delete",
      expectedMethod: "POST from frontend action; Spring @RequestMapping also accepts method-less requests",
      controller: "configuration-services-open-api/.../controller/EntityController.java deleteChoise(...)",
      service: "EntityService.deleteEntity(Entity) or EntityService.deleteEntityPhysical(code, false)",
      persistence: "Hibernate/scaffold DAO layer in configuration-services-service",
      targetTables: ["ec_entity", "ec_model", "ec_field", "ec_view", "runtime_view", "runtime_extra_view", "runtime_button"],
    },
    modelCreateEdit: {
      apiEndpoint: "/msService/ec/model/save",
      expectedMethod: "POST from frontend form; Spring @RequestMapping also accepts method-less requests",
      controller: "configuration-services-open-api/.../controller/ModelController.java save(String,HttpServletRequest)",
      dto: "DtoUtils.getModelVO(HttpServletRequest), then ModelController.prepare(Model)",
      service: "ModelService.saveModel(Model)",
      persistence: "Hibernate/scaffold DAO layer and ModelSyncDBUtils for physical/runtime metadata",
      targetTables: ["ec_model", "ec_field", "ec_view", "runtime_view", "runtime_extra_view", "runtime_button"],
    },
    modelDelete: {
      apiEndpoint: "/msService/ec/model/ordinaryDelete and /msService/ec/model/delete",
      expectedMethod: "POST from frontend action; Spring @RequestMapping also accepts method-less requests",
      controller: "configuration-services-open-api/.../controller/ModelController.java deleteChoise(...)",
      service: "ModelService.deleteModel(Model) or ModelService.deleteModelPhysical(code, false)",
      persistence: "Hibernate/scaffold DAO layer in configuration-services-service",
      targetTables: ["ec_model", "ec_field", "ec_view", "runtime_view", "runtime_extra_view", "runtime_button"],
    },
    modelDerivedReadiness: {
      apiEndpoint: "/msService/ec/model/formatTableName",
      expectedMethod: "GET in this probe",
      controller: "configuration-services-open-api/.../controller/ModelController.java formatTableName(HttpServletRequest)",
      service: "EntityService.getEntity(code), ModelController.prepare(Model)",
      persistence: "read-only lookup plus derived table name formatting; no save/delete invoked by this probe",
      targetTables: ["ec_entity", "ec_model"],
    },
  };
}

function crudAcceptanceItems(databaseEvidence) {
  const commonIssues = [
    "This probe intentionally did not invoke save, ordinaryDelete, delete, or physical delete endpoints.",
    "No ADP_E2E entity/model marker was created in this run.",
    "A PASS result still requires real frontend mutation, HTTP payload capture, PostgreSQL before/after SQL, cleanup, and runtime/menu regression.",
  ];
  return [
    {
      module: "basic-config",
      route: "configuration/entity runtime pages",
      operation: "entity/model create",
      api: "/msService/ec/entity/save and /msService/ec/model/save",
      method: "POST",
      requiresPersistence: true,
      mutationAttempted: false,
      backendEntry: "EntityController.save; ModelController.save",
      tables: ["ec_entity", "ec_model", "ec_field", "ec_view"],
      markerPattern: "ADP_E2E_*_EC_CREATE",
      verificationSql: databaseEvidence.verificationSql.futureCreateMarker,
      status: "NOT_VERIFIED",
      evidence: "Readiness-only route, browser page and metadata probe completed; create mutation not executed.",
      issues: commonIssues,
    },
    {
      module: "basic-config",
      route: "configuration/entity runtime pages",
      operation: "entity/model edit",
      api: "/msService/ec/entity/save and /msService/ec/model/save",
      method: "POST",
      requiresPersistence: true,
      mutationAttempted: false,
      backendEntry: "EntityController.save; ModelController.save",
      tables: ["ec_entity", "ec_model", "ec_field", "ec_view", "runtime_view", "runtime_extra_view"],
      markerPattern: "ADP_E2E_*_EC_EDIT",
      verificationSql: databaseEvidence.verificationSql.futureEditMarker,
      status: "NOT_VERIFIED",
      evidence: "Readiness-only route, browser page and metadata probe completed; edit mutation not executed.",
      issues: commonIssues,
    },
    {
      module: "basic-config",
      route: "configuration/entity runtime pages",
      operation: "entity/model delete or disable",
      api: "/msService/ec/entity/ordinaryDelete, /msService/ec/entity/delete, /msService/ec/model/ordinaryDelete, /msService/ec/model/delete",
      method: "POST",
      requiresPersistence: true,
      mutationAttempted: false,
      backendEntry: "EntityController.deleteChoise; ModelController.deleteChoise",
      tables: ["ec_entity", "ec_model", "ec_field", "ec_view", "runtime_view", "runtime_extra_view", "runtime_button"],
      markerPattern: "ADP_E2E_*_EC_DELETE",
      verificationSql: databaseEvidence.verificationSql.futureDeleteMarker,
      status: "NOT_VERIFIED",
      evidence: "Readiness-only route, browser page and metadata probe completed; delete/disable mutation not executed.",
      issues: commonIssues,
    },
  ];
}

function buildApiChecks(sample) {
  const formatProbeName = `AdpE2EProbe${stamp.slice(8)}`;
  return [
    { name: "ec-engine-msManage-html", path: "/msService/ec/engine/msManage", html: true },
    { name: "ec-module-msManage-html", path: "/msService/ec/module/msManage", html: true },
    { name: "ec-engine-datalist", path: "/msService/ec/engine/datalist" },
    {
      name: "entity-list-existing-module",
      path: makePath("/msService/ec/entity/list", {
        pageNo: 1,
        pageSize: 20,
        "module.code": sample.moduleCode,
      }),
      minNodes: 1,
    },
    {
      name: "entity-edit-existing-entity-html",
      path: makePath("/msService/ec/entity/edit", {
        moduleCode: sample.moduleCode,
        "entity.code": sample.entityCode,
      }),
      html: true,
    },
    {
      name: "model-list-existing-entity",
      path: makePath("/msService/ec/model/list", {
        "entity.code": sample.entityCode,
      }),
      minNodes: 1,
    },
    {
      name: "model-manage-existing-entity-html",
      path: makePath("/msService/ec/model/manage", {
        "entity.code": sample.entityCode,
      }),
      html: true,
    },
    {
      name: "model-edit-existing-model-html",
      path: makePath("/msService/ec/model/edit", {
        "entity.code": sample.entityCode,
        "model.code": sample.modelCode,
      }),
      html: true,
    },
    {
      name: "model-format-table-name-derived",
      path: makePath("/msService/ec/model/formatTableName", {
        "entity.code": sample.entityCode,
        "model.modelName": formatProbeName,
      }),
      expectText: "tableName",
    },
  ];
}

function buildBrowserPages(sample) {
  return [
    { name: "ec-engine-msManage", path: "/msService/ec/engine/msManage" },
    { name: "ec-module-msManage", path: "/msService/ec/module/msManage" },
    {
      name: "entity-edit-existing-entity",
      path: makePath("/msService/ec/entity/edit", {
        moduleCode: sample.moduleCode,
        "entity.code": sample.entityCode,
      }),
      allowLegacyTemplateGlobalErrors: true,
    },
    {
      name: "model-manage-existing-entity",
      path: makePath("/msService/ec/model/manage", {
        "entity.code": sample.entityCode,
      }),
      allowLegacyTemplateGlobalErrors: true,
    },
    {
      name: "model-edit-existing-model",
      path: makePath("/msService/ec/model/edit", {
        "entity.code": sample.entityCode,
        "model.code": sample.modelCode,
      }),
      allowLegacyTemplateGlobalErrors: true,
    },
  ];
}

function buildChecks(apiResults, browserResults, databaseEvidence) {
  const countByName = Object.fromEntries(databaseEvidence.counts.map((row) => [row.name, row.count]));
  const requiredCounts = {
    ec_module: 1,
    ec_entity: 1,
    ec_model: 1,
    ec_field: 1,
    ec_view: 1,
    runtime_view: 1,
    runtime_extra_view: 1,
    runtime_button: 1,
  };
  const requiredCountOk = Object.entries(requiredCounts).every(([name, minimum]) => (countByName[name] || 0) >= minimum);
  return [
    {
      name: "entity-model-api-readiness",
      status: apiResults.every((result) => result.ok) ? "PASS" : "FAIL",
      evidence: `${apiResults.filter((result) => result.ok).length}/${apiResults.length} API/read-only endpoints passed`,
    },
    {
      name: "entity-model-browser-readiness",
      status: browserResults.every((result) => result.ok) ? "PASS" : "FAIL",
      evidence: `${browserResults.filter((result) => result.ok).length}/${browserResults.length} browser pages passed`,
    },
    {
      name: "entity-model-postgres-metadata-readiness",
      status: requiredCountOk ? "PASS" : "FAIL",
      evidence: Object.entries(requiredCounts)
        .map(([name, minimum]) => `${name}=${countByName[name] || 0}>=${minimum}`)
        .join("; "),
    },
    {
      name: "entity-model-marker-safety",
      status: (countByName.marker_ec_entity || 0) === 0 && (countByName.marker_ec_model || 0) === 0 ? "PASS" : "FAIL",
      evidence: `marker_ec_entity=${countByName.marker_ec_entity || 0}; marker_ec_model=${countByName.marker_ec_model || 0}`,
    },
  ];
}

async function main() {
  ensureDir(outputPath);
  const databaseEvidence = collectDatabaseEvidence();
  if (!databaseEvidence.sample) {
    throw new Error("No existing ec_module/ec_entity/ec_model sample was found for read-only route probing.");
  }

  const api = await request.newContext({ ignoreHTTPSErrors: true });
  const ticket = await login(api);
  const apiResults = [];
  for (const check of buildApiChecks(databaseEvidence.sample)) {
    const result = await runApiCheck(api, ticket, check);
    apiResults.push(result);
    console.log(`${result.ok ? "OK" : "FAIL"} api ${result.name} status=${result.status} nodes=${result.nodeCount}`);
  }
  await api.dispose();

  const browser = await chromium.launch({ headless });
  const browserResults = [];
  for (const target of buildBrowserPages(databaseEvidence.sample)) {
    const result = await openPage(browser, ticket, target);
    browserResults.push(result);
    console.log(`${result.ok ? "OK" : "FAIL"} browser ${result.name} status=${result.navigationStatus}`);
  }
  await browser.close();

  const checks = buildChecks(apiResults, browserResults, databaseEvidence);
  const failedChecks = checks.filter((check) => check.status !== "PASS");
  const items = crudAcceptanceItems(databaseEvidence);
  const report = {
    schemaVersion: 1,
    generatedAt: new Date().toISOString(),
    repoCommit: getRepoCommit(),
    database: "PostgreSQL",
    module: "basic-config",
    goalId: "G-012",
    areaId: "configuration-entity-runtime",
    status: failedChecks.length === 0 ? "READINESS_ONLY" : "READINESS_FAILED",
    acceptanceConclusion: "NOT_VERIFIED",
    mutationAttempted: false,
    baseUrl,
    browserBaseUrl,
    summary: {
      readinessChecks: checks.length,
      readinessPassed: checks.filter((check) => check.status === "PASS").length,
      apiChecks: apiResults.length,
      apiPassed: apiResults.filter((result) => result.ok).length,
      browserPages: browserResults.length,
      browserPassed: browserResults.filter((result) => result.ok).length,
      browserWarnings: browserResults.reduce((total, result) => total + result.allowedPageErrors.length, 0),
      crudActions: items.length,
      pass: 0,
      fail: failedChecks.length,
      blocked: 0,
      notVerified: items.length,
      mutationAttempted: false,
    },
    sample: databaseEvidence.sample,
    checks,
    apiResults,
    browserResults,
    databaseEvidence,
    backendTrace: backendTrace(),
    items,
    safeguards: [
      "Only GET/read-only page and API checks are executed.",
      "No /entity/save, /model/save, /ordinaryDelete, /delete, or physical delete endpoint is invoked.",
      "The report must not be used as PASS evidence for entity/model CRUD.",
    ],
    nextAcceptanceSteps: [
      "Create a dedicated ADP_E2E_*_EC_CREATE marker through the real frontend page.",
      "Capture HTTP method, URL, request payload, response status and response body key fields.",
      "Trace controller, service, DAO/ORM and target tables.",
      "Run PostgreSQL before/after SQL for ec_entity, ec_model, ec_field, ec_view and related runtime tables.",
      "Clean up or roll back marker-created metadata and rerun runtime/menu smoke.",
    ],
  };

  fs.writeFileSync(outputPath, JSON.stringify(report, null, 2) + "\n");
  if (failedChecks.length > 0) {
    console.error(`FAIL entity/model readiness checks=${failedChecks.map((check) => check.name).join(", ")}`);
    console.error(`REPORT ${outputPath}`);
    process.exitCode = 1;
    return;
  }
  console.log(
    `PASS_READINESS_ONLY entity/model api=${report.summary.apiPassed}/${report.summary.apiChecks} browser=${report.summary.browserPassed}/${report.summary.browserPages}`
  );
  console.log(`REPORT ${outputPath}`);
}

main().catch((error) => {
  console.error(error && error.stack ? error.stack : String(error));
  process.exitCode = 1;
});
