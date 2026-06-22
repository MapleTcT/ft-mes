#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { execFileSync } = require("child_process");
const { chromium, request } = require("playwright");

const stamp = new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14);
const baseUrl = (process.env.ADP_BASE_URL || "http://100.99.133.43:18080").replace(/\/+$/, "");
const browserBaseUrl = (process.env.ADP_BROWSER_BASE_URL || baseUrl).replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD || "123456";
const headless = process.env.ADP_HEADLESS !== "false";
const outputDir = process.env.ADP_OUTPUT_DIR || path.join("/tmp", `adp-systemcode-persistence-${stamp}`);
const outputPath =
  process.env.ADP_SYSTEMCODE_PERSISTENCE_OUTPUT ||
  path.join(outputDir, "systemcode-persistence-results.json");

const dbSshTarget = process.env.ADP_DB_SSH_TARGET || "v6@100.99.133.43";
const dbContainer = process.env.ADP_DB_CONTAINER || "adp-mes-newbase-postgres-1";
const dbName = process.env.ADP_DB_NAME || "adp";
const dbUser = process.env.ADP_DB_USER || "adp";

const moduleId = process.env.ADP_SYSTEMCODE_MODULE_ID || "systemCode";
const marker = process.env.ADP_E2E_MARKER || `ADP_E2E_${stamp}_SYSCODE`;
const entityCode = process.env.ADP_SYSTEMCODE_ENTITY_CODE || `${moduleId}_${marker}_ENTITY`;
const valueCode = process.env.ADP_SYSTEMCODE_VALUE_CODE || `${marker}_VALUE`;
const entityName = `${marker}.entity`;
const entityNameUpdated = `${marker}.entity.updated`;
const valueName = `${marker}.value`;
const valueNameUpdated = `${marker}.value.updated`;
const entityDisplayName = `${marker} entity`;
const entityDisplayNameUpdated = `${marker} entity update`;
const valueDisplayName = `${marker} value`;
const valueDisplayNameUpdated = `${marker} value update`;
const entityMemoCreate = `${marker} create system code entity`;
const entityMemoUpdate = `${marker} update system code entity`;
const valueMemoCreate = `${marker} create system code value`;
const valueMemoUpdate = `${marker} update system code value`;

const visibleErrorPattern =
  /(数据库操作异常|系统错误|系统异常|发生未知异常|SQLGrammarException|could not extract ResultSet|column .* does not exist|relation .* does not exist|500 INTERNAL|\b[\w.]+Exception(?::|\s+at\b))/i;

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true });
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

function parseTableRows(output, columns) {
  if (!output) {
    return [];
  }
  return output.split(/\r?\n/).map((line) => {
    const values = line.split("|");
    return Object.fromEntries(columns.map((column, index) => [column, values[index] || ""]));
  });
}

function queryModule(value) {
  const sql = [
    "select module_id, module_name",
    "from public.mod_module_registry",
    `where module_id = ${sqlLiteral(value)};`,
  ].join(" ");
  return {
    sql,
    rows: parseTableRows(runSql(sql), ["moduleId", "moduleName"]),
  };
}

function queryEntityByCode(code) {
  const sql = [
    "select id, code, name, coalesce(display_name,''), coalesce(type,''),",
    "coalesce(module_id,''), coalesce(cid::text,''), coalesce(valid::text,''),",
    "coalesce(multi_flag::text,''), coalesce(sys_default::text,''), coalesce(memo,''),",
    "coalesce(row_version::text,''), coalesce(modify_time::text,'')",
    "from public.sys_entity",
    `where code = ${sqlLiteral(code)}`,
    "order by create_time desc nulls last, id desc;",
  ].join(" ");
  return {
    sql,
    rows: parseTableRows(runSql(sql), [
      "id",
      "code",
      "name",
      "displayName",
      "type",
      "moduleId",
      "cid",
      "valid",
      "multiFlag",
      "sysDefault",
      "memo",
      "rowVersion",
      "modifyTime",
    ]),
  };
}

function queryCodeByEntityAndCode(entityCodeArg, code) {
  const sql = [
    "select id, coalesce(row_version::text,''), entity_code, code, coalesce(name,''),",
    "coalesce(display_name,''), coalesce(cid::text,''), coalesce(valid::text,''),",
    "coalesce(default_flag::text,''), coalesce(parent_id::text,''), coalesce(lay_no::text,''),",
    "coalesce(lay_rec,''), coalesce(sort::text,''), coalesce(full_path,''),",
    "coalesce(full_path_name,''), coalesce(memo,''), coalesce(des_a,''), coalesce(des_b,''),",
    "coalesce(des_c,''), coalesce(modify_time::text,'')",
    "from public.sys_code",
    `where entity_code = ${sqlLiteral(entityCodeArg)} and code = ${sqlLiteral(code)}`,
    "order by create_time desc nulls last, id desc;",
  ].join(" ");
  return {
    sql,
    rows: parseTableRows(runSql(sql), [
      "id",
      "rowVersion",
      "entityCode",
      "code",
      "name",
      "displayName",
      "cid",
      "valid",
      "defaultFlag",
      "parentId",
      "layNo",
      "layRec",
      "sort",
      "fullPath",
      "fullPathName",
      "memo",
      "desA",
      "desB",
      "desC",
      "modifyTime",
    ]),
  };
}

function requestMatcher(url) {
  return /\/inter-api\/systemcode\/v1\//.test(url) ||
    /\/inter-api\/module-registry\/v1\/modules/.test(url);
}

async function browserApi(page, method, urlPath, payload) {
  return page.evaluate(
    async ({ method: methodArg, urlPath: urlPathArg, payload: payloadArg }) => {
      const token =
        window.localStorage.getItem("suposTicket") ||
        window.localStorage.getItem("SUPOS_TICKET") ||
        window.localStorage.getItem("token") ||
        window.sessionStorage.getItem("suposTicket") ||
        window.sessionStorage.getItem("SUPOS_TICKET") ||
        window.sessionStorage.getItem("token") ||
        "";
      const response = await window.fetch(urlPathArg, {
        method: methodArg,
        credentials: "include",
        headers: {
          Accept: "application/json, text/plain, */*",
          Authorization: token ? `Bearer ${token}` : "",
          "Content-Type": "application/json;charset=UTF-8",
          langu_code: "zh_CN",
        },
        body: payloadArg === undefined ? undefined : JSON.stringify(payloadArg),
      });
      const text = await response.text();
      let json = null;
      try {
        json = JSON.parse(text);
      } catch (_error) {
        json = null;
      }
      return { ok: response.ok, status: response.status, text, json };
    },
    { method, urlPath, payload }
  );
}

function compactErrors(items) {
  return items.map((item) => {
    if (typeof item === "string") {
      return item.slice(0, 500);
    }
    return {
      ...item,
      body: item.body ? item.body.slice(0, 800) : item.body,
      text: item.text ? item.text.slice(0, 500) : item.text,
    };
  });
}

function assertCode(value, label) {
  if (!/^[0-9A-Za-z_]{1,100}$/.test(value)) {
    throw new Error(`${label} must match ^[0-9A-Za-z_]{1,100}$: ${value}`);
  }
}

async function main() {
  ensureDir(outputDir);
  ensureDir(path.dirname(outputPath));
  assertCode(entityCode, "entityCode");
  assertCode(valueCode, "valueCode");

  const modulePrecheck = queryModule(moduleId);
  if (!modulePrecheck.rows.length) {
    throw new Error(`Module ${moduleId} was not found in public.mod_module_registry`);
  }

  const entityPrecheck = queryEntityByCode(entityCode);
  if (entityPrecheck.rows.some((row) => row.valid === "1")) {
    throw new Error(`Active sys_entity already exists for code ${entityCode}`);
  }
  const codePrecheck = queryCodeByEntityAndCode(entityCode, valueCode);
  if (codePrecheck.rows.some((row) => row.valid === "1")) {
    throw new Error(`Active sys_code already exists for ${entityCode}/${valueCode}`);
  }

  const api = await request.newContext({ ignoreHTTPSErrors: true });
  const ticket = await login(api);
  await api.dispose();

  const browser = await chromium.launch({ headless });
  const context = await browser.newContext({
    baseURL: browserBaseUrl,
    ignoreHTTPSErrors: true,
    viewport: { width: 1600, height: 1000 },
    extraHTTPHeaders: { Authorization: `Bearer ${ticket}` },
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
    window.localStorage.setItem("language", "zh_CN");
  }, ticket);

  const page = await context.newPage();
  const consoleErrors = [];
  const pageErrors = [];
  const requestFailures = [];
  const capturedRequests = [];
  const capturedResponses = [];

  page.on("console", (message) => {
    if (["error", "warning"].includes(message.type())) {
      consoleErrors.push({ type: message.type(), text: message.text() });
    }
  });
  page.on("pageerror", (error) => pageErrors.push(error.message));
  page.on("requestfailed", (requestItem) => {
    if (requestMatcher(requestItem.url())) {
      requestFailures.push({
        method: requestItem.method(),
        url: requestItem.url(),
        failure: requestItem.failure() && requestItem.failure().errorText,
      });
    }
  });
  page.on("request", (requestItem) => {
    if (requestMatcher(requestItem.url())) {
      capturedRequests.push({
        method: requestItem.method(),
        url: requestItem.url(),
        postData: requestItem.postData() || null,
      });
    }
  });
  page.on("response", async (response) => {
    if (!requestMatcher(response.url())) {
      return;
    }
    let body = "";
    try {
      body = (await response.text()).slice(0, 1200);
    } catch (_error) {
      body = "";
    }
    capturedResponses.push({
      method: response.request().method(),
      url: response.url(),
      status: response.status(),
      body,
    });
  });

  const route = "/systemcode/#/";
  const navigation = await page.goto(`${browserBaseUrl}${route}`, { waitUntil: "domcontentloaded", timeout: 45000 });
  await page.waitForLoadState("networkidle", { timeout: 15000 }).catch(() => {});
  await page.waitForTimeout(1500);

  const modulesResult = await browserApi(page, "GET", "/inter-api/module-registry/v1/modules");
  if (!modulesResult.ok || visibleErrorPattern.test(modulesResult.text)) {
    throw new Error(`Module list failed with ${modulesResult.status}: ${modulesResult.text.slice(0, 800)}`);
  }

  const createEntityPayload = {
    code: entityCode,
    name: entityName,
    displayName: entityDisplayName,
    type: "list",
    cid: 1000,
    moduleId,
    valid: 1,
    multiFlag: 0,
    sysDefault: 0,
    memo: entityMemoCreate,
  };
  const createEntityResult = await browserApi(page, "POST", "/inter-api/systemcode/v1/entity", createEntityPayload);
  if (!createEntityResult.ok || visibleErrorPattern.test(createEntityResult.text)) {
    throw new Error(`System code entity create failed with ${createEntityResult.status}: ${createEntityResult.text.slice(0, 1200)}`);
  }
  await page.waitForTimeout(700);
  const entityAfterCreate = queryEntityByCode(entityCode);
  const createdEntity = entityAfterCreate.rows.find((row) => row.valid === "1");
  if (
    !createdEntity ||
    createdEntity.name !== entityName ||
    createdEntity.displayName !== entityDisplayName ||
    createdEntity.type !== "list" ||
    createdEntity.moduleId !== moduleId ||
    createdEntity.cid !== "1000" ||
    createdEntity.memo !== entityMemoCreate
  ) {
    throw new Error(`System code entity create did not persist expected values: ${JSON.stringify(entityAfterCreate.rows)}`);
  }

  const updateEntityPayload = {
    code: entityCode,
    name: entityNameUpdated,
    displayName: entityDisplayNameUpdated,
    type: "list",
    memo: entityMemoUpdate,
  };
  const updateEntityResult = await browserApi(page, "PUT", "/inter-api/systemcode/v1/entity", updateEntityPayload);
  if (!updateEntityResult.ok || visibleErrorPattern.test(updateEntityResult.text)) {
    throw new Error(`System code entity update failed with ${updateEntityResult.status}: ${updateEntityResult.text.slice(0, 1200)}`);
  }
  await page.waitForTimeout(700);
  const entityAfterUpdate = queryEntityByCode(entityCode);
  const updatedEntity = entityAfterUpdate.rows.find((row) => row.id === createdEntity.id && row.valid === "1");
  if (
    !updatedEntity ||
    updatedEntity.name !== entityNameUpdated ||
    updatedEntity.displayName !== entityDisplayNameUpdated ||
    updatedEntity.memo !== entityMemoUpdate
  ) {
    throw new Error(`System code entity update did not persist expected values: ${JSON.stringify(entityAfterUpdate.rows)}`);
  }

  const createValuePayload = {
    entityCode,
    code: valueCode,
    name: valueName,
    displayName: valueDisplayName,
    cid: 1000,
    memo: valueMemoCreate,
    desA: `${marker} desA create`,
    desB: `${marker} desB create`,
    desC: `${marker} desC create`,
    type: "list",
    defaultFlag: 1,
  };
  const createValueResult = await browserApi(page, "POST", "/inter-api/systemcode/v1/value", createValuePayload);
  if (!createValueResult.ok || visibleErrorPattern.test(createValueResult.text)) {
    throw new Error(`System code value create failed with ${createValueResult.status}: ${createValueResult.text.slice(0, 1200)}`);
  }
  await page.waitForTimeout(700);
  const codeAfterCreate = queryCodeByEntityAndCode(entityCode, valueCode);
  const createdValue = codeAfterCreate.rows.find((row) => row.valid === "1");
  if (
    !createdValue ||
    createdValue.name !== valueName ||
    createdValue.displayName !== valueDisplayName ||
    createdValue.cid !== "1000" ||
    createdValue.defaultFlag !== "1" ||
    createdValue.layNo !== "1" ||
    createdValue.fullPath !== valueCode ||
    createdValue.memo !== valueMemoCreate ||
    createdValue.desA !== createValuePayload.desA ||
    createdValue.desB !== createValuePayload.desB ||
    createdValue.desC !== createValuePayload.desC
  ) {
    throw new Error(`System code value create did not persist expected values: ${JSON.stringify(codeAfterCreate.rows)}`);
  }

  const updateValuePayload = {
    entityCode,
    code: valueCode,
    name: valueNameUpdated,
    displayName: valueDisplayNameUpdated,
    cid: 1000,
    memo: valueMemoUpdate,
    desA: `${marker} desA update`,
    desB: `${marker} desB update`,
    desC: `${marker} desC update`,
    type: "list",
    defaultFlag: 0,
  };
  const updateValueResult = await browserApi(page, "PUT", "/inter-api/systemcode/v1/value", updateValuePayload);
  if (!updateValueResult.ok || visibleErrorPattern.test(updateValueResult.text)) {
    throw new Error(`System code value update failed with ${updateValueResult.status}: ${updateValueResult.text.slice(0, 1200)}`);
  }
  await page.waitForTimeout(700);
  const codeAfterUpdate = queryCodeByEntityAndCode(entityCode, valueCode);
  const updatedValue = codeAfterUpdate.rows.find((row) => row.id === createdValue.id && row.valid === "1");
  if (
    !updatedValue ||
    updatedValue.name !== valueNameUpdated ||
    updatedValue.displayName !== valueDisplayNameUpdated ||
    updatedValue.defaultFlag !== "0" ||
    updatedValue.memo !== valueMemoUpdate ||
    updatedValue.desA !== updateValuePayload.desA ||
    updatedValue.desB !== updateValuePayload.desB ||
    updatedValue.desC !== updateValuePayload.desC
  ) {
    throw new Error(`System code value update did not persist expected values: ${JSON.stringify(codeAfterUpdate.rows)}`);
  }

  const deleteValueResult = await browserApi(page, "DELETE", `/inter-api/systemcode/v1/${entityCode}/values/${valueCode}`);
  if (!deleteValueResult.ok || visibleErrorPattern.test(deleteValueResult.text)) {
    throw new Error(`System code value delete failed with ${deleteValueResult.status}: ${deleteValueResult.text.slice(0, 1200)}`);
  }
  await page.waitForTimeout(700);
  const codeAfterDelete = queryCodeByEntityAndCode(entityCode, valueCode);
  const deletedValue = codeAfterDelete.rows.find((row) => row.id === createdValue.id && row.valid === "0");
  if (!deletedValue) {
    throw new Error(`System code value delete did not soft-delete row: ${JSON.stringify(codeAfterDelete.rows)}`);
  }

  const deleteEntityResult = await browserApi(page, "DELETE", `/inter-api/systemcode/v1/entities/${entityCode}`);
  if (!deleteEntityResult.ok || visibleErrorPattern.test(deleteEntityResult.text)) {
    throw new Error(`System code entity delete failed with ${deleteEntityResult.status}: ${deleteEntityResult.text.slice(0, 1200)}`);
  }
  await page.waitForTimeout(700);
  const entityAfterDelete = queryEntityByCode(entityCode);
  const deletedEntity = entityAfterDelete.rows.find((row) => row.id === createdEntity.id && row.valid === "0");
  const codeAfterEntityDelete = queryCodeByEntityAndCode(entityCode, valueCode);
  const deletedValueAfterEntityDelete = codeAfterEntityDelete.rows.find((row) => row.id === createdValue.id && row.valid === "0");
  if (!deletedEntity || !deletedValueAfterEntityDelete) {
    throw new Error(
      `System code entity delete did not soft-delete entity and child value: ${JSON.stringify({
        entities: entityAfterDelete.rows,
        values: codeAfterEntityDelete.rows,
      })}`
    );
  }

  const bodyText = await page.locator("body").innerText({ timeout: 5000 }).catch(() => "");
  const visibleError = String(bodyText)
    .split(/\r?\n/)
    .map((line) => line.trim())
    .find((line) => line && visibleErrorPattern.test(line));
  const screenshotPath = path.join(outputDir, "systemcode-persistence.png");
  await page.screenshot({ path: screenshotPath, fullPage: true }).catch(() => {});
  await browser.close();

  const report = {
    generatedAt: new Date().toISOString(),
    baseUrl,
    browserBaseUrl,
    username,
    marker,
    route,
    entityCode,
    valueCode,
    dependencies: {
      moduleId,
      modulePrecheckSql: modulePrecheck.sql,
      modulePrecheckRows: modulePrecheck.rows,
    },
    backendTrace: {
      entityCreate:
        "SystemEntityController.addEntity -> SystemEntityServiceImpl.addEntity -> MyBatis Plus save -> public.sys_entity",
      entityUpdate:
        "SystemEntityController.updateEntity -> SystemEntityServiceImpl.updateEntity -> MyBatis Plus update -> public.sys_entity",
      valueCreate:
        "SystemCodeController.addValue -> SystemCodeServiceImpl.addValue -> insertValue -> MyBatis Plus save -> public.sys_code",
      valueUpdate:
        "SystemCodeController.updateValue -> SystemCodeServiceImpl.updateValue -> MyBatis Plus update -> public.sys_code",
      valueDelete:
        "SystemCodeController.batchDeleteValues -> SystemCodeServiceImpl.batchDeleteValues -> public.sys_code.valid=0",
      entityDelete:
        "SystemEntityController.batchDeleteEntities -> SystemEntityServiceImpl.batchDeleteEntities -> public.sys_entity.valid=0 and public.sys_code.valid=0",
    },
    browser: {
      navigationStatus: navigation ? navigation.status() : null,
      consoleErrors: compactErrors(consoleErrors),
      pageErrors: compactErrors(pageErrors),
      requestFailures: compactErrors(requestFailures),
      visibleError: visibleError || null,
      screenshot: screenshotPath,
    },
    operations: {
      moduleList: {
        method: "GET",
        api: "/inter-api/module-registry/v1/modules",
        responseStatus: modulesResult.status,
        responseBody: modulesResult.json || modulesResult.text.slice(0, 1000),
      },
      entityCreate: {
        method: "POST",
        api: "/inter-api/systemcode/v1/entity",
        payload: createEntityPayload,
        responseStatus: createEntityResult.status,
        responseBody: createEntityResult.json || createEntityResult.text.slice(0, 1000),
        verificationSql: entityAfterCreate.sql,
        rows: entityAfterCreate.rows,
      },
      entityUpdate: {
        method: "PUT",
        api: "/inter-api/systemcode/v1/entity",
        payload: updateEntityPayload,
        responseStatus: updateEntityResult.status,
        responseBody: updateEntityResult.json || updateEntityResult.text.slice(0, 1000),
        verificationSql: entityAfterUpdate.sql,
        rows: entityAfterUpdate.rows,
      },
      valueCreate: {
        method: "POST",
        api: "/inter-api/systemcode/v1/value",
        payload: createValuePayload,
        responseStatus: createValueResult.status,
        responseBody: createValueResult.json || createValueResult.text.slice(0, 1000),
        verificationSql: codeAfterCreate.sql,
        rows: codeAfterCreate.rows,
      },
      valueUpdate: {
        method: "PUT",
        api: "/inter-api/systemcode/v1/value",
        payload: updateValuePayload,
        responseStatus: updateValueResult.status,
        responseBody: updateValueResult.json || updateValueResult.text.slice(0, 1000),
        verificationSql: codeAfterUpdate.sql,
        rows: codeAfterUpdate.rows,
      },
      valueDelete: {
        method: "DELETE",
        api: `/inter-api/systemcode/v1/${entityCode}/values/${valueCode}`,
        payload: null,
        responseStatus: deleteValueResult.status,
        responseBody: deleteValueResult.json || deleteValueResult.text.slice(0, 1000),
        verificationSql: codeAfterDelete.sql,
        rows: codeAfterDelete.rows,
        deleteMode: "soft-delete sys_code.valid=0",
      },
      entityDelete: {
        method: "DELETE",
        api: `/inter-api/systemcode/v1/entities/${entityCode}`,
        payload: null,
        responseStatus: deleteEntityResult.status,
        responseBody: deleteEntityResult.json || deleteEntityResult.text.slice(0, 1000),
        verificationSql: [entityAfterDelete.sql, codeAfterEntityDelete.sql].join("\n"),
        rows: {
          sysEntity: entityAfterDelete.rows,
          sysCode: codeAfterEntityDelete.rows,
        },
        deleteMode: "soft-delete sys_entity.valid=0 and child sys_code.valid=0",
      },
    },
    capturedRequests,
    capturedResponses,
    ok:
      !visibleError &&
      consoleErrors.length === 0 &&
      pageErrors.length === 0 &&
      requestFailures.length === 0 &&
      Boolean(createdEntity) &&
      Boolean(updatedEntity) &&
      Boolean(createdValue) &&
      Boolean(updatedValue) &&
      Boolean(deletedValue) &&
      Boolean(deletedEntity) &&
      Boolean(deletedValueAfterEntityDelete),
  };

  fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`);
  console.log(
    `${report.ok ? "OK" : "FAIL"} systemcode-persistence marker=${marker} entityCode=${entityCode} valueCode=${valueCode}`
  );
  console.log(`REPORT ${outputPath}`);
  if (!report.ok) {
    process.exitCode = 1;
  }
}

main().catch((error) => {
  console.error(error && error.stack ? error.stack : error);
  process.exit(1);
});
