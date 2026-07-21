#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { execFileSync, spawnSync } = require("child_process");
const { chromium, request } = require("playwright");

const repoRoot = path.resolve(__dirname, "../../..");
const stamp = new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14);
const suffix = stamp.slice(8, 14);
const baseUrl = (process.env.ADP_BASE_URL || "http://10.11.100.17:18080").replace(/\/+$/, "");
const browserBaseUrl = (process.env.ADP_BROWSER_BASE_URL || baseUrl).replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD || "123456";
const outputPath =
  process.env.ADP_ENTITY_MODEL_FIELD_TYPE_MATRIX_OUTPUT ||
  path.join("/tmp", `adp-entity-model-field-type-matrix-${stamp}.json`);
const dbSshTarget = process.env.ADP_DB_SSH_TARGET || "v6@10.11.100.17";
const dbContainer = process.env.ADP_DB_CONTAINER || "adp-mes-newbase-postgres-1";
const dbName = process.env.ADP_DB_NAME || "adp";
const dbUser = process.env.ADP_DB_USER || "adp";
const sshConnectTimeout = process.env.ADP_SSH_CONNECT_TIMEOUT || "20";
const dbQueryTimeoutMs = Number(process.env.ADP_DB_QUERY_TIMEOUT_MS || "30000");
const headless = process.env.ADP_HEADLESS !== "false";

const marker = process.env.ADP_E2E_MARKER || `ADP_E2E_${stamp}_PG_TYPE_MATRIX`;
const moduleCode = process.env.ADP_ENTITY_MODEL_MODULE_CODE || "DataSet_1.0.0";
const entityName = `E2eTypEnt${suffix}`;
const modelName = `E2eTypMod${suffix}`;
const entityCode = `${moduleCode}_${entityName}`;
const modelCode = `${entityCode}_${modelName}`;
const tableName = `DS_E2TM_${suffix}`;
const probeId = Number(`${Date.now()}`.slice(-14));
const invalidBooleanProbeId = probeId + 1;
const pagePath = "/msService/ec/engine/msManage";
const visibleErrorPattern =
  /(数据库操作异常|系统错误|系统异常|发生未知异常|SQLGrammarException|could not extract ResultSet|column .* does not exist|relation .* does not exist|500 INTERNAL|org\.hibernate\.[\w.]+Exception|org\.springframework\.[\w.]+Exception|java\.lang\.[\w.]+Exception|Invalid bound statement)/i;

function scalarCase(definition, index) {
  const propertyName = `e2e${definition.namePart}${suffix}`;
  return {
    ...definition,
    sort: index + 1,
    propertyName,
    propertyCode: `${modelCode}_${propertyName}`,
    columnName: `${definition.columnPrefix}_${suffix}`,
  };
}

const scalarCases = [
  {
    key: "text",
    namePart: "Text",
    columnPrefix: "TM_TEXT",
    type: "TEXT",
    fieldType: "TEXTFIELD",
    format: "TEXT",
    maxLength: "32",
    expected: { dataType: "character varying", characterLength: "64" },
    valueSql: () => sqlLiteral(`${marker}_TEXT`),
    probeText: `${marker}_TEXT`,
  },
  {
    key: "bapCode",
    namePart: "BapCode",
    columnPrefix: "TM_BAP",
    type: "BAPCODE",
    fieldType: "TEXTFIELD",
    format: "TEXT",
    expected: { dataType: "character varying", characterLength: "4000" },
    valueSql: () => sqlLiteral(`${marker}_BAP`),
    probeText: `${marker}_BAP`,
  },
  {
    key: "summary",
    namePart: "Summary",
    columnPrefix: "TM_SUM",
    type: "SUMMARY",
    fieldType: "TEXTFIELD",
    format: "TEXT",
    expected: { dataType: "character varying", characterLength: "4000" },
    valueSql: () => sqlLiteral(`${marker}_SUMMARY`),
    probeText: `${marker}_SUMMARY`,
  },
  {
    key: "integerLong",
    namePart: "IntegerLong",
    columnPrefix: "TM_INTL",
    type: "INTEGER",
    fieldType: "TEXTFIELD",
    format: "TEXT",
    expected: { dataType: "integer" },
    valueSql: () => "42",
    probeText: "42",
  },
  {
    key: "integerNumeric",
    namePart: "IntegerNumeric",
    columnPrefix: "TM_INTN",
    type: "INTEGER",
    fieldType: "TEXTFIELD",
    format: "TEXT",
    expected: { dataType: "integer" },
    valueSql: () => "314",
    probeText: "314",
  },
  {
    key: "decimal",
    namePart: "Decimal",
    columnPrefix: "TM_DEC",
    type: "DECIMAL",
    fieldType: "TEXTFIELD",
    format: "TEXT",
    decimalNum: "2",
    expected: { dataType: "numeric", numericPrecision: "19", numericScale: "2" },
    valueSql: () => "12345.67",
    probeText: "12345.67",
  },
  {
    key: "date",
    namePart: "Date",
    columnPrefix: "TM_DATE",
    type: "DATE",
    fieldType: "DATE",
    format: "YMD",
    expected: { dataType: "date" },
    valueSql: () => "date '2026-07-21'",
    probeText: "2026-07-21",
  },
  {
    key: "time",
    namePart: "Time",
    columnPrefix: "TM_TIME",
    type: "TIME",
    fieldType: "TIME",
    format: "HMS",
    expected: { dataType: "time without time zone" },
    valueSql: () => "time '08:15:30'",
    probeText: "08:15:30",
  },
  {
    key: "dateTime",
    namePart: "DateTime",
    columnPrefix: "TM_DTIME",
    type: "DATETIME",
    fieldType: "DATETIME",
    format: "YMD_HMS",
    expected: { dataType: "timestamp without time zone" },
    valueSql: () => "timestamp '2026-07-21 08:15:30'",
    probeText: "2026-07-21 08:15:30",
  },
  {
    key: "binary",
    namePart: "Binary",
    columnPrefix: "TM_BIN",
    type: "BINARY",
    fieldType: "TEXTFIELD",
    format: "TEXT",
    expected: { dataType: "bytea" },
    valueSql: () => "decode('414450','hex')",
    probeText: "414450",
    probeExpression: (column) => `encode(${sqlIdentifier(column)}, 'hex')`,
  },
  {
    key: "boolean",
    namePart: "Boolean",
    columnPrefix: "TM_BOOL",
    type: "BOOLEAN",
    fieldType: "SELECT",
    format: "SELECT",
    expected: { dataType: "boolean" },
    valueSql: () => "true",
    probeText: "true",
  },
  {
    key: "longText",
    namePart: "LongText",
    columnPrefix: "TM_LTEXT",
    type: "LONGTEXT",
    fieldType: "TEXTAREA",
    format: "TEXT",
    expected: { dataType: "text" },
    valueSql: () => sqlLiteral(`${marker}_LONGTEXT`),
    probeText: `${marker}_LONGTEXT`,
  },
  {
    key: "long",
    namePart: "Long",
    columnPrefix: "TM_LONG",
    type: "LONG",
    fieldType: "TEXTFIELD",
    format: "TEXT",
    expected: { dataType: "bigint" },
    valueSql: () => "9876543210",
    probeText: "9876543210",
  },
  {
    key: "systemCode",
    namePart: "SystemCode",
    columnPrefix: "TM_SYSC",
    type: "SYSTEMCODE",
    fieldType: "SELECTCOMP",
    format: "SYSTEMCODE",
    expected: { dataType: "character varying", characterLength: "4000" },
    valueSql: () => sqlLiteral(`${marker}_SYSTEM`),
    probeText: `${marker}_SYSTEM`,
  },
  {
    key: "enumerate",
    namePart: "Enumerate",
    columnPrefix: "TM_ENUM",
    type: "ENUMERATE",
    fieldType: "SELECT",
    format: "ENUMERATE",
    expected: { dataType: "character varying", characterLength: "510" },
    valueSql: () => sqlLiteral(`${marker}_ENUM`),
    probeText: `${marker}_ENUM`,
  },
  {
    key: "money",
    namePart: "Money",
    columnPrefix: "TM_MONEY",
    type: "MONEY",
    fieldType: "TEXTFIELD",
    format: "THOUSAND",
    decimalNum: "6",
    expected: { dataType: "numeric", numericPrecision: "19", numericScale: "6" },
    valueSql: () => "7654.321000",
    probeText: "7654.321000",
  },
  {
    key: "password",
    namePart: "Password",
    columnPrefix: "TM_PASS",
    type: "PASSWORD",
    fieldType: "PASSWORDFIELD",
    format: "TEXT",
    expected: { dataType: "character varying", characterLength: "510" },
    valueSql: () => sqlLiteral(`${marker}_PASSWORD`),
    probeText: `${marker}_PASSWORD`,
  },
  {
    key: "picture",
    namePart: "Picture",
    columnPrefix: "TM_PIC",
    type: "PICTURE",
    fieldType: "PICTURE",
    format: "PICTURE",
    expected: { dataType: "character varying", characterLength: "510" },
    valueSql: () => sqlLiteral(`${marker}_PICTURE`),
    probeText: `${marker}_PICTURE`,
  },
  {
    key: "propertyAttachment",
    namePart: "PropertyAttachment",
    columnPrefix: "TM_ATTACH",
    type: "PROPERTYATTACHMENT",
    fieldType: "PROPERTYATTACHMENT",
    format: "SELECTCOMP",
    expected: { noPhysicalColumn: true },
  },
  {
    key: "office",
    namePart: "Office",
    columnPrefix: "TM_OFFICE",
    type: "OFFICE",
    fieldType: "OFFICE",
    format: "OFFICE",
    expected: { dataType: "text" },
    valueSql: () => sqlLiteral(`${marker}_OFFICE`),
    probeText: `${marker}_OFFICE`,
  },
  {
    key: "tagNumber",
    namePart: "TagNumber",
    columnPrefix: "TM_TAG",
    type: "TAGNUMBER",
    fieldType: "SELECTTAGNUMBER",
    format: "SELECTTAGNUMBER",
    expected: { dataType: "character varying", characterLength: "510" },
    valueSql: () => sqlLiteral(`${marker}_TAG`),
    probeText: `${marker}_TAG`,
  },
  {
    key: "itemIndex",
    namePart: "ItemIndex",
    columnPrefix: "TM_ITEM",
    type: "ITEMINDEX",
    fieldType: "TEXTFIELD",
    format: "TEXT",
    expected: { dataType: "character varying", characterLength: "510" },
    valueSql: () => sqlLiteral(`${marker}_ITEM`),
    probeText: `${marker}_ITEM`,
  },
  {
    key: "color",
    namePart: "Color",
    columnPrefix: "TM_COLOR",
    type: "COLOR",
    fieldType: "COLOR",
    format: "COLOR",
    expected: { dataType: "character varying", characterLength: "510" },
    valueSql: () => sqlLiteral("#123456"),
    probeText: "#123456",
  },
  {
    key: "layer",
    namePart: "Layer",
    columnPrefix: "TM_LAYER",
    type: "LAYER",
    fieldType: "LAYER",
    format: "LAYER",
    expected: { dataType: "character varying", characterLength: "510" },
    valueSql: () => sqlLiteral(`${marker}_LAYER`),
    probeText: `${marker}_LAYER`,
  },
].map(scalarCase);

function ensureDir(filePath) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
}

function getRepoCommit() {
  const result = spawnSync("git", ["rev-parse", "HEAD"], { cwd: repoRoot, encoding: "utf8" });
  return result.status === 0 ? result.stdout.trim() : "UNKNOWN";
}

function shellQuote(value) {
  return `'${String(value).replace(/'/g, "'\\''")}'`;
}

function sqlLiteral(value) {
  return `'${String(value).replace(/'/g, "''")}'`;
}

function sqlIdentifier(value) {
  const text = String(value);
  if (!/^[A-Za-z][A-Za-z0-9_]{0,62}$/.test(text)) {
    throw new Error(`Unsafe SQL identifier: ${text}`);
  }
  return `"${text.toLowerCase()}"`;
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
  return execFileSync(
    "ssh",
    ["-o", "BatchMode=yes", "-o", `ConnectTimeout=${sshConnectTimeout}`, dbSshTarget, remoteCommand],
    { encoding: "utf8", stdio: ["ignore", "pipe", "pipe"], timeout: dbQueryTimeoutMs }
  ).trim();
}

function parseRows(output, columns) {
  if (!output) {
    return [];
  }
  return output
    .split(/\r?\n/)
    .filter(Boolean)
    .map((line) => {
      const values = line.split("|");
      return Object.fromEntries(columns.map((column, index) => [column, values[index] || ""]));
    });
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

async function readTextJson(response) {
  const text = await response.text();
  try {
    return { text, json: JSON.parse(text), status: response.status(), ok: response.ok() };
  } catch (_error) {
    return { text, json: null, status: response.status(), ok: response.ok() };
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
    const parsed = await readTextJson(response);
    const ticket = response.ok() ? findTicket(parsed.json) : null;
    if (ticket) {
      return ticket;
    }
    errors.push({ status: parsed.status, bodySnippet: parsed.text.slice(0, 400) });
  }
  throw new Error(`Login failed for ${username}: ${JSON.stringify(errors)}`);
}

function responseBusinessOk(result) {
  const status = result && (result.responseStatus || result.status || 0);
  if (!result || status >= 400 || visibleErrorPattern.test(result.text || "")) {
    return false;
  }
  if (!result.json) {
    return /"success"\s*:\s*true|success=true/i.test(result.text || "");
  }
  if (Object.prototype.hasOwnProperty.call(result.json, "success")) {
    return result.json.success === true || String(result.json.success) === "true";
  }
  if (Object.prototype.hasOwnProperty.call(result.json, "code")) {
    return ["0", "200", "100000000"].includes(String(result.json.code));
  }
  return true;
}

async function openPage(ticket) {
  const browser = await chromium.launch({ headless });
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
  const evidence = {
    url: `${browserBaseUrl}${pagePath}`,
    navigationStatus: null,
    navigationAttempts: 0,
    navigationTransientErrors: [],
    consoleErrors: [],
    pageErrors: [],
    requestFailures: [],
    networkErrors: [],
    visibleError: null,
  };
  page.on("console", (message) => {
    if (message.type() === "error") {
      evidence.consoleErrors.push(message.text());
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
  page.on("response", (response) => {
    if (response.status() >= 400) {
      evidence.networkErrors.push({ status: response.status(), url: response.url(), expected: false });
    }
  });
  try {
    let response = null;
    for (let attempt = 1; attempt <= 3; attempt += 1) {
      evidence.navigationAttempts = attempt;
      try {
        response = await page.goto(evidence.url, { waitUntil: "domcontentloaded", timeout: 45000 });
        break;
      } catch (error) {
        evidence.navigationTransientErrors.push(error && error.message ? error.message : String(error));
        if (attempt === 3) {
          throw error;
        }
        await page.waitForTimeout(1500 * attempt);
      }
    }
    evidence.navigationStatus = response ? response.status() : null;
    await page.waitForTimeout(1500);
    const bodyText = await page.locator("body").innerText({ timeout: 5000 }).catch(() => "");
    evidence.visibleError =
      bodyText
        .split(/\r?\n/)
        .map((line) => line.trim())
        .find((line) => line && visibleErrorPattern.test(line)) || null;
    return { browser, context, page, evidence };
  } catch (error) {
    await context.close().catch(() => {});
    await browser.close().catch(() => {});
    throw error;
  }
}

async function pageFormFetch(page, endpoint, payload) {
  return page.evaluate(
    async ({ endpoint, payload }) => {
      const response = await fetch(endpoint, {
        method: "POST",
        credentials: "include",
        headers: {
          Accept: "application/json, text/plain, */*",
          "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
          langu_code: "zh_CN",
          "X-Requested-With": "XMLHttpRequest",
        },
        body: new URLSearchParams(payload),
      });
      const text = await response.text();
      let json = null;
      try {
        json = JSON.parse(text);
      } catch (_error) {
        json = null;
      }
      return {
        method: "POST",
        url: endpoint,
        requestPayload: payload,
        responseStatus: response.status,
        ok: response.ok,
        text,
        json,
      };
    },
    { endpoint, payload }
  );
}

function entityPayload() {
  return {
    "entity.version": "0",
    "entity.code": entityCode,
    "entity.module.code": moduleCode,
    "entity.isControl": "false",
    "entity.payCloseAttention": "false",
    "entity.crossCompanyFlag": "false",
    "entity.mobile": "false",
    "entity.enableRest": "false",
    "entity.enableWs": "false",
    "entity.enableFieldsPermissionConf": "false",
    "entity.entityName": entityName,
    "entity.prefix": "E2T",
    "entity.name": `${marker} entity`,
    "entity.isBase": "true",
    "entity.groupEnabled": "false",
    "entity.workflowEnabled": "false",
    "entity.description": `${marker}_ENTITY`,
  };
}

function modelPayload() {
  return {
    "model.version": "0",
    "model.code": "",
    "entity.code": entityCode,
    "model.entity.code": entityCode,
    "model.moduleCode": moduleCode,
    "model.orgTableName": "",
    "model.modelName": modelName,
    "model.tableName": tableName,
    "model.name": `${marker} model`,
    "model.dataType": "1",
    "model.isMain": "false",
    "model.isExtraCol": "false",
    "model.isCache": "false",
    "model.enableSync": "false",
    "model.type": "0",
    "model.description": `${marker}_MODEL`,
  };
}

function propertyPayload(item, overrides = {}) {
  const type = overrides.type || item.type;
  const fieldType = overrides.fieldType || item.fieldType;
  const format = overrides.format || item.format;
  const isNew = overrides.isNew === true;
  return {
    "property.version": overrides.version || "0",
    "property.sort": String(item.sort),
    "property.model.code": modelCode,
    "model.enableDataAudit": "false",
    "property.entityCode": entityCode,
    "property.moduleCode": moduleCode,
    "property.code": isNew ? "" : item.propertyCode,
    "property.defaultValue": "",
    "property.fillcontent": "",
    "property.attributes": "",
    "property.isControl": "false",
    "property.isUnique": "false",
    "property.isHidden": "false",
    "property.nullable": "true",
    "property.multable": "false",
    "property.seniorSystemCode": "false",
    "property.sensitive": "false",
    "property.stretch": "false",
    "property.isBussinessKey": "false",
    "property.isUsedMneCode": "false",
    "property.isIndex": "false",
    "property.orgColumnName": overrides.orgColumnName || "",
    "property.isGroupObject": "false",
    "property.onlyLeaf": "false",
    "property.isUsedForList": "true",
    "property.isCustom": "false",
    "property.isUsedForSearch": "false",
    "property.isIgnoreAudit": "false",
    "property.noAnalyzer": "false",
    "property.isMainAssociated": "false",
    "property.isMainDisplay": "false",
    "property.name": item.propertyName,
    "property.columnName": item.columnName,
    "property.displayName": `${marker} ${item.key}`,
    "property.type": type,
    "property.fieldType": fieldType,
    "property.format": format,
    "property.maxLength": Object.prototype.hasOwnProperty.call(overrides, "maxLength")
      ? overrides.maxLength
      : item.maxLength || "",
    "property.decimalNum": Object.prototype.hasOwnProperty.call(overrides, "decimalNum")
      ? overrides.decimalNum
      : item.decimalNum || "",
    "property.fetchMode": "SELECT",
    "property.description": `${marker}_${item.key}`,
  };
}

function metadataSql() {
  return [
    "select code::text, coalesce(version::text,'0'), coalesce(valid::text,''),",
    "coalesce(name::text,''), coalesce(type::text,''), coalesce(column_name::text,''),",
    "coalesce(max_length::text,''), coalesce(decimal_num::text,''), coalesce(nullable::text,''),",
    "coalesce(model_code::text,''), coalesce(description::text,'')",
    "from public.ec_property",
    `where model_code=${sqlLiteral(modelCode)} and code in (${scalarCases
      .map((item) => sqlLiteral(item.propertyCode))
      .join(",")})`,
    "order by code;",
  ].join(" ");
}

function columnsSql() {
  return [
    "select column_name::text, data_type::text, coalesce(character_maximum_length::text,''),",
    "coalesce(numeric_precision::text,''), coalesce(numeric_scale::text,''), is_nullable::text",
    "from information_schema.columns",
    "where table_schema='public'",
    `and lower(table_name)=lower(${sqlLiteral(tableName)})`,
    "order by ordinal_position;",
  ].join(" ");
}

function probeSql() {
  const expressions = scalarCases
    .filter((item) => !item.expected.noPhysicalColumn)
    .map((item) => {
      const expression = item.probeExpression
        ? item.probeExpression(item.columnName)
        : `${sqlIdentifier(item.columnName)}::text`;
      return `coalesce(${expression},'<NULL>')`;
    });
  return `select ${expressions.join(",")} from public.${sqlIdentifier(tableName)} where id=${probeId};`;
}

function invalidBooleanSql() {
  const booleanCase = caseByKey("boolean");
  return `select id::text, ${sqlIdentifier(booleanCase.columnName)}::text from public.${sqlIdentifier(
    tableName
  )} where id=${invalidBooleanProbeId};`;
}

function queryState(label, includeProbe = true) {
  const probeColumns = scalarCases
    .filter((item) => !item.expected.noPhysicalColumn)
    .map((item) => item.key);
  return {
    label,
    metadata: parseRows(runSql(metadataSql()), [
      "code",
      "version",
      "valid",
      "name",
      "type",
      "columnName",
      "maxLength",
      "decimalNum",
      "nullable",
      "modelCode",
      "description",
    ]),
    columns: parseRows(runSql(columnsSql()), [
      "columnName",
      "dataType",
      "characterLength",
      "numericPrecision",
      "numericScale",
      "nullable",
    ]),
    probe: includeProbe ? parseRows(runSql(probeSql()), probeColumns) : [],
    invalidBooleanRows: parseRows(runSql(invalidBooleanSql()), ["id", "value"]),
  };
}

function markerInsertSql() {
  const physicalCases = scalarCases.filter((item) => !item.expected.noPhysicalColumn);
  const columns = ["id", "valid", ...physicalCases.map((item) => sqlIdentifier(item.columnName))];
  const values = [String(probeId), "1", ...physicalCases.map((item) => item.valueSql())];
  return `insert into public.${sqlIdentifier(tableName)} (${columns.join(",")}) values (${values.join(",")});`;
}

function cleanupSql() {
  return [
    "begin;",
    "delete from public.ec_property",
    `where model_code=${sqlLiteral(modelCode)} or entity_code=${sqlLiteral(entityCode)};`,
    "delete from public.ec_model",
    `where code=${sqlLiteral(modelCode)} or entity_code=${sqlLiteral(entityCode)};`,
    "delete from public.ec_entity",
    `where code=${sqlLiteral(entityCode)};`,
    `drop table if exists public.${sqlIdentifier(tableName)} cascade;`,
    "commit;",
  ].join(" ");
}

function stateCounts() {
  const output = runSql(
    [
      "select",
      `(select count(*) from public.ec_property where model_code=${sqlLiteral(modelCode)} or entity_code=${sqlLiteral(
        entityCode
      )}),`,
      `(select count(*) from public.ec_model where code=${sqlLiteral(modelCode)} or entity_code=${sqlLiteral(
        entityCode
      )}),`,
      `(select count(*) from public.ec_entity where code=${sqlLiteral(entityCode)}),`,
      `(select count(*) from information_schema.tables where table_schema='public' and lower(table_name)=lower(${sqlLiteral(
        tableName
      )}));`,
    ].join(" ")
  );
  const values = output.split("|");
  return {
    property: Number(values[0] || 0),
    model: Number(values[1] || 0),
    entity: Number(values[2] || 0),
    physicalTable: Number(values[3] || 0),
  };
}

function caseByKey(key) {
  const item = scalarCases.find((candidate) => candidate.key === key);
  if (!item) {
    throw new Error(`Unknown scalar case: ${key}`);
  }
  return item;
}

function metadataFor(state, item) {
  return state.metadata.find((row) => row.code === item.propertyCode);
}

function columnFor(state, item) {
  return state.columns.find((row) => row.columnName.toLowerCase() === item.columnName.toLowerCase());
}

function columnMatches(column, expected) {
  if (!column || column.dataType !== expected.dataType || column.nullable !== "YES") {
    return false;
  }
  for (const key of ["characterLength", "numericPrecision", "numericScale"]) {
    if (Object.prototype.hasOwnProperty.call(expected, key) && column[key] !== expected[key]) {
      return false;
    }
  }
  return true;
}

function check(name, passed, evidence) {
  return { name, status: passed ? "PASS" : "FAIL", evidence };
}

async function updateProperty(page, item, state, overrides) {
  const metadata = metadataFor(state, item) || {};
  return pageFormFetch(
    page,
    "/msService/ec/property/save",
    propertyPayload(item, {
      version: metadata.version || "0",
      orgColumnName: item.columnName,
      ...overrides,
    })
  );
}

async function main() {
  ensureDir(outputPath);
  const api = await request.newContext({ ignoreHTTPSErrors: true });
  const ticket = await login(api);
  await api.dispose();

  runSql(cleanupSql());
  const before = stateCounts();
  const { browser, context, page, evidence: browserEvidence } = await openPage(ticket);
  const requests = {};
  const states = { before };
  let cleanup = null;
  let fatalError = null;
  try {
    requests.entityCreate = await pageFormFetch(page, "/msService/ec/entity/save", entityPayload());
    requests.modelCreate = await pageFormFetch(page, "/msService/ec/model/save", modelPayload());
    for (const item of scalarCases) {
      requests[`create_${item.key}`] = await pageFormFetch(
        page,
        "/msService/ec/property/save",
        propertyPayload(item, { isNew: true })
      );
    }
    states.afterCreate = queryState("afterCreate", false);

    runSql(markerInsertSql());
    states.afterProbeInsert = queryState("afterProbeInsert");

    const booleanCase = caseByKey("boolean");
    requests.booleanToInteger = await updateProperty(page, booleanCase, states.afterProbeInsert, {
      type: "INTEGER",
      fieldType: "TEXTFIELD",
      format: "TEXT",
    });
    states.afterBooleanToInteger = queryState("afterBooleanToInteger");

    runSql(
      `insert into public.${sqlIdentifier(tableName)} (id, valid, ${sqlIdentifier(
        booleanCase.columnName
      )}) values (${invalidBooleanProbeId}, 1, 2);`
    );
    states.afterInvalidBooleanFixture = queryState("afterInvalidBooleanFixture");
    requests.invalidIntegerToBoolean = await updateProperty(page, booleanCase, states.afterInvalidBooleanFixture, {
      type: "BOOLEAN",
      fieldType: "SELECT",
      format: "SELECT",
    });
    states.afterInvalidBooleanReject = queryState("afterInvalidBooleanReject");

    runSql(`delete from public.${sqlIdentifier(tableName)} where id=${invalidBooleanProbeId};`);
    states.afterInvalidBooleanCleanup = queryState("afterInvalidBooleanCleanup");
    requests.integerToBoolean = await updateProperty(page, booleanCase, states.afterInvalidBooleanCleanup, {
      type: "BOOLEAN",
      fieldType: "SELECT",
      format: "SELECT",
    });
    states.afterIntegerToBoolean = queryState("afterIntegerToBoolean");
    requests.booleanReplay = await updateProperty(page, booleanCase, states.afterIntegerToBoolean, {
      type: "BOOLEAN",
      fieldType: "SELECT",
      format: "SELECT",
    });
    states.afterBooleanReplay = queryState("afterBooleanReplay");

    const integerLongCase = caseByKey("integerLong");
    requests.integerToLong = await updateProperty(page, integerLongCase, states.afterBooleanReplay, {
      type: "LONG",
      fieldType: "TEXTFIELD",
      format: "TEXT",
    });
    states.afterIntegerToLong = queryState("afterIntegerToLong");

    const integerNumericCase = caseByKey("integerNumeric");
    requests.integerToNumeric = await updateProperty(page, integerNumericCase, states.afterIntegerToLong, {
      type: "DECIMAL",
      fieldType: "TEXTFIELD",
      format: "TEXT",
      decimalNum: "2",
    });
    states.afterIntegerToNumeric = queryState("afterIntegerToNumeric");

    const dateCase = caseByKey("date");
    requests.dateToDateTime = await updateProperty(page, dateCase, states.afterIntegerToNumeric, {
      type: "DATETIME",
      fieldType: "DATETIME",
      format: "YMD_HMS",
    });
    states.afterDateToDateTime = queryState("afterDateToDateTime");

    const decimalCase = caseByKey("decimal");
    requests.unsafeDecimalScale = await updateProperty(page, decimalCase, states.afterDateToDateTime, {
      type: "DECIMAL",
      fieldType: "TEXTFIELD",
      format: "TEXT",
      decimalNum: "6",
    });
    states.afterUnsafeDecimalScale = queryState("afterUnsafeDecimalScale");

    for (const expectedFailure of [requests.invalidIntegerToBoolean, requests.unsafeDecimalScale]) {
      if (expectedFailure && expectedFailure.responseStatus >= 400) {
        const target = browserEvidence.networkErrors.find(
          (item) => !item.expected && item.url.includes("/msService/ec/property/save") && item.status === expectedFailure.responseStatus
        );
        if (target) {
          target.expected = true;
        }
      }
    }
    browserEvidence.expectedConsoleErrors = browserEvidence.consoleErrors.filter((message) =>
      /Failed to load resource: the server responded with a status of [45]\d\d/i.test(message)
    );
  } catch (error) {
    fatalError = error && error.stack ? error.stack : String(error);
  } finally {
    await context.close().catch(() => {});
    await browser.close().catch(() => {});
    try {
      runSql(cleanupSql());
      cleanup = stateCounts();
    } catch (error) {
      fatalError = fatalError || (error && error.stack ? error.stack : String(error));
    }
  }

  const unexpectedNetworkErrors = browserEvidence.networkErrors.filter((item) => !item.expected);
  const expectedConsoleErrors = browserEvidence.expectedConsoleErrors || [];
  const unexpectedConsoleErrors = browserEvidence.consoleErrors.filter(
    (message) => !expectedConsoleErrors.includes(message)
  );
  const checks = [
    check(
      "browser-page-context",
      browserEvidence.navigationStatus && browserEvidence.navigationStatus < 400 && !browserEvidence.visibleError &&
        unexpectedConsoleErrors.length === 0 && browserEvidence.pageErrors.length === 0 &&
        browserEvidence.requestFailures.length === 0 && unexpectedNetworkErrors.length === 0,
      `status=${browserEvidence.navigationStatus}; visibleError=${browserEvidence.visibleError || "none"}; ` +
        `navigationAttempts=${browserEvidence.navigationAttempts}; transientNavigation=${browserEvidence.navigationTransientErrors.length}; ` +
        `unexpectedConsole=${unexpectedConsoleErrors.length}; expectedConsole=${expectedConsoleErrors.length}; ` +
        `page=${browserEvidence.pageErrors.length}; request=${browserEvidence.requestFailures.length}; ` +
        `unexpectedNetwork=${unexpectedNetworkErrors.length}`
    ),
    check(
      "entity-model-and-scalar-create-requests",
      responseBusinessOk(requests.entityCreate) && responseBusinessOk(requests.modelCreate) &&
        scalarCases.every((item) => responseBusinessOk(requests[`create_${item.key}`])),
      JSON.stringify(
        Object.fromEntries(
          ["entityCreate", "modelCreate", ...scalarCases.map((item) => `create_${item.key}`)].map((key) => [
            key,
            requests[key] && requests[key].responseStatus,
          ])
        )
      )
    ),
  ];

  for (const item of scalarCases) {
    const metadata = states.afterCreate ? metadataFor(states.afterCreate, item) : null;
    const column = states.afterCreate ? columnFor(states.afterCreate, item) : null;
    const physicalOk = item.expected.noPhysicalColumn ? !column : columnMatches(column, item.expected);
    checks.push(
      check(
        `scalar-type-${item.key}`,
        metadata && metadata.type === item.type &&
          metadata.columnName.toLowerCase() === item.columnName.toLowerCase() && physicalOk,
        `metadata=${JSON.stringify(metadata || null)}; column=${JSON.stringify(column || null)}`
      )
    );
  }

  const probe = states.afterProbeInsert && states.afterProbeInsert.probe[0];
  checks.push(
    check(
      "scalar-marker-row-round-trip",
      probe && scalarCases
        .filter((item) => !item.expected.noPhysicalColumn)
        .every((item) => probe[item.key] === item.probeText),
      `probe=${JSON.stringify(probe || null)}`
    )
  );

  const booleanCase = caseByKey("boolean");
  const booleanIntegerMetadata = states.afterBooleanToInteger && metadataFor(states.afterBooleanToInteger, booleanCase);
  const booleanIntegerColumn = states.afterBooleanToInteger && columnFor(states.afterBooleanToInteger, booleanCase);
  checks.push(
    check(
      "boolean-to-integer-preserves-values",
      responseBusinessOk(requests.booleanToInteger) && booleanIntegerMetadata &&
        booleanIntegerMetadata.type === "INTEGER" && booleanIntegerColumn &&
        booleanIntegerColumn.dataType === "integer" && states.afterBooleanToInteger.probe[0].boolean === "1",
      `response=${requests.booleanToInteger && requests.booleanToInteger.responseStatus}; ` +
        `metadata=${JSON.stringify(booleanIntegerMetadata || null)}; column=${JSON.stringify(booleanIntegerColumn || null)}; ` +
        `probe=${JSON.stringify((states.afterBooleanToInteger && states.afterBooleanToInteger.probe[0]) || null)}`
    )
  );

  const rejectedBooleanMetadata = states.afterInvalidBooleanReject && metadataFor(states.afterInvalidBooleanReject, booleanCase);
  const rejectedBooleanColumn = states.afterInvalidBooleanReject && columnFor(states.afterInvalidBooleanReject, booleanCase);
  checks.push(
    check(
      "invalid-integer-to-boolean-rolls-back",
      requests.invalidIntegerToBoolean && requests.invalidIntegerToBoolean.responseStatus === 500 &&
        rejectedBooleanMetadata && rejectedBooleanMetadata.type === "INTEGER" &&
        rejectedBooleanColumn && rejectedBooleanColumn.dataType === "integer" &&
        states.afterInvalidBooleanReject.invalidBooleanRows.length === 1 &&
        states.afterInvalidBooleanReject.invalidBooleanRows[0].value === "2" &&
        states.afterInvalidBooleanReject.probe[0].boolean === "1",
      `response=${requests.invalidIntegerToBoolean && requests.invalidIntegerToBoolean.responseStatus}; ` +
        `metadata=${JSON.stringify(rejectedBooleanMetadata || null)}; column=${JSON.stringify(rejectedBooleanColumn || null)}; ` +
        `invalidRows=${JSON.stringify((states.afterInvalidBooleanReject && states.afterInvalidBooleanReject.invalidBooleanRows) || [])}`
    )
  );

  const restoredBooleanMetadata = states.afterIntegerToBoolean && metadataFor(states.afterIntegerToBoolean, booleanCase);
  const restoredBooleanColumn = states.afterIntegerToBoolean && columnFor(states.afterIntegerToBoolean, booleanCase);
  checks.push(
    check(
      "zero-one-integer-to-boolean-preserves-values",
      responseBusinessOk(requests.integerToBoolean) && restoredBooleanMetadata &&
        restoredBooleanMetadata.type === "BOOLEAN" && restoredBooleanColumn &&
        restoredBooleanColumn.dataType === "boolean" && states.afterIntegerToBoolean.probe[0].boolean === "true" &&
        states.afterIntegerToBoolean.invalidBooleanRows.length === 0,
      `response=${requests.integerToBoolean && requests.integerToBoolean.responseStatus}; ` +
        `metadata=${JSON.stringify(restoredBooleanMetadata || null)}; column=${JSON.stringify(restoredBooleanColumn || null)}; ` +
        `probe=${JSON.stringify((states.afterIntegerToBoolean && states.afterIntegerToBoolean.probe[0]) || null)}`
    ),
    check(
      "boolean-idempotent-replay",
      responseBusinessOk(requests.booleanReplay) && states.afterBooleanReplay &&
        columnFor(states.afterBooleanReplay, booleanCase).dataType === "boolean" &&
        states.afterBooleanReplay.probe[0].boolean === "true",
      `response=${requests.booleanReplay && requests.booleanReplay.responseStatus}; ` +
        `column=${JSON.stringify(states.afterBooleanReplay && columnFor(states.afterBooleanReplay, booleanCase))}`
    )
  );

  const integerLongCase = caseByKey("integerLong");
  checks.push(
    check(
      "integer-to-bigint-safe-widening",
      responseBusinessOk(requests.integerToLong) && states.afterIntegerToLong &&
        metadataFor(states.afterIntegerToLong, integerLongCase).type === "LONG" &&
        columnFor(states.afterIntegerToLong, integerLongCase).dataType === "bigint" &&
        states.afterIntegerToLong.probe[0].integerLong === "42",
      `response=${requests.integerToLong && requests.integerToLong.responseStatus}; ` +
        `column=${JSON.stringify(states.afterIntegerToLong && columnFor(states.afterIntegerToLong, integerLongCase))}`
    )
  );

  const integerNumericCase = caseByKey("integerNumeric");
  const integerNumericColumn = states.afterIntegerToNumeric && columnFor(states.afterIntegerToNumeric, integerNumericCase);
  checks.push(
    check(
      "integer-to-numeric-safe-widening",
      responseBusinessOk(requests.integerToNumeric) && states.afterIntegerToNumeric &&
        metadataFor(states.afterIntegerToNumeric, integerNumericCase).type === "DECIMAL" && integerNumericColumn &&
        integerNumericColumn.dataType === "numeric" && integerNumericColumn.numericPrecision === "19" &&
        integerNumericColumn.numericScale === "2" && states.afterIntegerToNumeric.probe[0].integerNumeric === "314.00",
      `response=${requests.integerToNumeric && requests.integerToNumeric.responseStatus}; ` +
        `column=${JSON.stringify(integerNumericColumn || null)}; ` +
        `probe=${JSON.stringify((states.afterIntegerToNumeric && states.afterIntegerToNumeric.probe[0]) || null)}`
    )
  );

  const dateCase = caseByKey("date");
  checks.push(
    check(
      "date-to-timestamp-safe-widening",
      responseBusinessOk(requests.dateToDateTime) && states.afterDateToDateTime &&
        metadataFor(states.afterDateToDateTime, dateCase).type === "DATETIME" &&
        columnFor(states.afterDateToDateTime, dateCase).dataType === "timestamp without time zone" &&
        states.afterDateToDateTime.probe[0].date === "2026-07-21 00:00:00",
      `response=${requests.dateToDateTime && requests.dateToDateTime.responseStatus}; ` +
        `column=${JSON.stringify(states.afterDateToDateTime && columnFor(states.afterDateToDateTime, dateCase))}; ` +
        `probe=${JSON.stringify((states.afterDateToDateTime && states.afterDateToDateTime.probe[0]) || null)}`
    )
  );

  const decimalCase = caseByKey("decimal");
  const rejectedDecimalMetadata = states.afterUnsafeDecimalScale && metadataFor(states.afterUnsafeDecimalScale, decimalCase);
  const rejectedDecimalColumn = states.afterUnsafeDecimalScale && columnFor(states.afterUnsafeDecimalScale, decimalCase);
  checks.push(
    check(
      "numeric-capacity-reduction-rolls-back",
      requests.unsafeDecimalScale && requests.unsafeDecimalScale.responseStatus === 500 &&
        rejectedDecimalMetadata && rejectedDecimalMetadata.type === "DECIMAL" &&
        rejectedDecimalMetadata.decimalNum === "2" && rejectedDecimalColumn &&
        rejectedDecimalColumn.dataType === "numeric" && rejectedDecimalColumn.numericPrecision === "19" &&
        rejectedDecimalColumn.numericScale === "2" && states.afterUnsafeDecimalScale.probe[0].decimal === "12345.67",
      `response=${requests.unsafeDecimalScale && requests.unsafeDecimalScale.responseStatus}; ` +
        `metadata=${JSON.stringify(rejectedDecimalMetadata || null)}; column=${JSON.stringify(rejectedDecimalColumn || null)}; ` +
        `probe=${JSON.stringify((states.afterUnsafeDecimalScale && states.afterUnsafeDecimalScale.probe[0]) || null)}`
    ),
    check(
      "controlled-cleanup",
      cleanup && cleanup.property === 0 && cleanup.model === 0 && cleanup.entity === 0 && cleanup.physicalTable === 0,
      `cleanup=${JSON.stringify(cleanup)}`
    )
  );

  const pass = checks.filter((item) => item.status === "PASS").length;
  const fail = checks.filter((item) => item.status === "FAIL").length;
  const acceptanceStatus = fail === 0 && !fatalError ? "PASS" : fatalError ? "BLOCKED" : "FAIL";
  const report = {
    schemaVersion: 1,
    generatedAt: new Date().toISOString(),
    repoCommit: getRepoCommit(),
    database: "PostgreSQL",
    module: "basic-config",
    actionId: "entity-model-postgres-field-type-matrix",
    areaId: "configuration-physical-model-field",
    status: acceptanceStatus,
    marker,
    route: pagePath,
    apiEndpoints: [
      "POST /msService/ec/entity/save",
      "POST /msService/ec/model/save",
      "POST /msService/ec/property/save",
    ],
    identifiers: {
      entityCode,
      modelCode,
      tableName,
      probeId,
      invalidBooleanProbeId,
      fields: scalarCases.map((item) => ({
        key: item.key,
        type: item.type,
        propertyCode: item.propertyCode,
        columnName: item.columnName,
      })),
    },
    coverage: {
      scalarFieldInstances: scalarCases.length,
      distinctDbColumnTypes: [...new Set(scalarCases.map((item) => item.type))],
      excludedTypes: [
        {
          type: "OBJECT",
          reason: "Association fields require a separate target-model fixture and are not scalar type claims.",
        },
      ],
    },
    summary: {
      testedChecks: checks.length,
      pass,
      fail,
      blocked: fatalError ? 1 : 0,
      status: acceptanceStatus,
    },
    checks,
    browser: browserEvidence,
    requests,
    states,
    cleanup,
    backendTrace: {
      endpoint: "PropertyController.save -> DtoUtils.getPropertyVO -> ModelServiceImpl.saveProperty",
      metadataPersistence: "propertyDao.merge -> public.ec_property",
      physicalPersistence:
        "FieldSyncDBUtils.fieldSyncToDb -> PostgresFieldSyncSupport.sync -> PostgreSQL native scalar DDL and guarded USING conversions",
      transactionBoundary:
        "ModelServiceImpl.saveProperty propagates PostgreSQL DDL or domain validation failures so ec_property and physical DDL roll back together",
      conversionPolicy:
        "BOOLEAN uses PostgreSQL boolean; integer 0/1 conversion is explicit and rejects any other value. DATE, TIME and DATETIME use native PostgreSQL date, time without time zone and timestamp without time zone. Numeric changes must preserve both integer and fractional capacity.",
      destructivePolicy:
        "No automatic DROP COLUMN. OBJECT association fields remain outside this scalar acceptance and require a dedicated fixture.",
    },
    verificationSql: {
      metadata: metadataSql(),
      columns: columnsSql(),
      markerInsert: markerInsertSql(),
      markerReadback: probeSql(),
      invalidBooleanReadback: invalidBooleanSql(),
      cleanup: cleanupSql(),
    },
    fatalError,
  };
  fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`, "utf8");
  console.log(JSON.stringify(report.summary));
  console.log(`report=${outputPath}`);
  if (report.summary.status !== "PASS") {
    process.exitCode = 1;
  }
}

main().catch((error) => {
  console.error(error && error.stack ? error.stack : error);
  process.exitCode = 1;
});
