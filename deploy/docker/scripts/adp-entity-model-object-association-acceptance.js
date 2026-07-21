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
  process.env.ADP_ENTITY_MODEL_OBJECT_ASSOCIATION_OUTPUT ||
  path.join("/tmp", `adp-entity-model-object-association-${stamp}.json`);
const dbSshTarget = process.env.ADP_DB_SSH_TARGET || "v6@10.11.100.17";
const dbContainer = process.env.ADP_DB_CONTAINER || "adp-mes-newbase-postgres-1";
const dbName = process.env.ADP_DB_NAME || "adp";
const dbUser = process.env.ADP_DB_USER || "adp";
const sshConnectTimeout = process.env.ADP_SSH_CONNECT_TIMEOUT || "20";
const dbQueryTimeoutMs = Number(process.env.ADP_DB_QUERY_TIMEOUT_MS || "30000");
const headless = process.env.ADP_HEADLESS !== "false";

const marker = process.env.ADP_E2E_MARKER || `ADP_E2E_${stamp}_PG_OBJECT_ASSOC`;
const moduleCode = process.env.ADP_ENTITY_MODEL_MODULE_CODE || "DataSet_1.0.0";
const targetEntityName = `E2eObjTgt${suffix}`;
const sourceEntityName = `E2eObjSrc${suffix}`;
const targetModelName = `E2eObjTgtM${suffix}`;
const sourceModelName = `E2eObjSrcM${suffix}`;
const targetEntityCode = `${moduleCode}_${targetEntityName}`;
const sourceEntityCode = `${moduleCode}_${sourceEntityName}`;
const targetModelCode = `${targetEntityCode}_${targetModelName}`;
const sourceModelCode = `${sourceEntityCode}_${sourceModelName}`;
const targetTableName = `DS_EOT_${suffix}`;
const sourceTableName = `DS_EOS_${suffix}`;
const targetProbeId = Number(`${Date.now()}`.slice(-14));
const sourceProbeId = targetProbeId + 1;
const targetLongValue = targetProbeId + 100;
const pagePath = "/msService/ec/engine/msManage";
const visibleErrorPattern =
  /(数据库操作异常|系统错误|系统异常|发生未知异常|SQLGrammarException|could not extract ResultSet|column .* does not exist|relation .* does not exist|500 INTERNAL|org\.hibernate\.[\w.]+Exception|org\.springframework\.[\w.]+Exception|java\.lang\.[\w.]+Exception|Invalid bound statement)/i;

const targetProperties = [
  {
    key: "longKey",
    name: `e2eObjLong${suffix}`,
    columnName: `OT_LONG_${suffix}`,
    type: "LONG",
    fieldType: "TEXTFIELD",
    format: "TEXT",
    expected: { dataType: "bigint" },
    sqlValue: String(targetLongValue),
  },
  {
    key: "textKey",
    name: `e2eObjText${suffix}`,
    columnName: `OT_TEXT_${suffix}`,
    type: "TEXT",
    fieldType: "TEXTFIELD",
    format: "TEXT",
    maxLength: "24",
    expected: { dataType: "character varying", characterLength: "48" },
    sqlValue: sqlLiteral(`${marker}_TEXT`),
  },
  {
    key: "bapCodeKey",
    name: `e2eObjCode${suffix}`,
    columnName: `OT_CODE_${suffix}`,
    type: "BAPCODE",
    fieldType: "TEXTFIELD",
    format: "TEXT",
    expected: { dataType: "character varying", characterLength: "4000" },
    sqlValue: sqlLiteral(`${marker}_CODE`),
  },
].map((item, index) => ({
  ...item,
  sort: index + 1,
  propertyCode: `${targetModelCode}_${item.name}`,
}));

const objectProperties = [
  {
    key: "oneToOneLong",
    name: `e2eOneLong${suffix}`,
    columnName: `OS_O1L_${suffix}`,
    associatedType: "1",
    targetKey: "longKey",
    expected: { dataType: "bigint" },
    sqlValue: String(targetLongValue),
    parameterStyle: "dotted",
  },
  {
    key: "manyToOneLong",
    name: `e2eManyLong${suffix}`,
    columnName: `OS_M1L_${suffix}`,
    associatedType: "2",
    targetKey: "longKey",
    expected: { dataType: "bigint" },
    sqlValue: String(targetLongValue),
    parameterStyle: "legacyUnderscore",
  },
  {
    key: "oneToOneText",
    name: `e2eOneText${suffix}`,
    columnName: `OS_O1T_${suffix}`,
    associatedType: "1",
    targetKey: "textKey",
    expected: { dataType: "character varying", characterLength: "48" },
    sqlValue: sqlLiteral(`${marker}_TEXT`),
    parameterStyle: "dotted",
  },
  {
    key: "manyToOneBapCode",
    name: `e2eManyCode${suffix}`,
    columnName: `OS_M1C_${suffix}`,
    associatedType: "2",
    targetKey: "bapCodeKey",
    expected: { dataType: "character varying", characterLength: "4000" },
    sqlValue: sqlLiteral(`${marker}_CODE`),
    parameterStyle: "dotted",
  },
].map((item, index) => ({
  ...item,
  sort: index + 1,
  propertyCode: `${sourceModelCode}_${item.name}`,
}));

const invalidObject = {
  key: "invalidTarget",
  name: `e2eBadObj${suffix}`,
  columnName: `OS_BAD_${suffix}`,
  sort: objectProperties.length + 1,
  propertyCode: `${sourceModelCode}_e2eBadObj${suffix}`,
  missingAssociatedPropertyCode: `${targetModelCode}_missing${suffix}`,
};

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
    expectedConsoleErrors: [],
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

function entityPayload(entityCode, entityName, prefix) {
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
    "entity.prefix": prefix,
    "entity.name": `${marker} ${entityName}`,
    "entity.isBase": "true",
    "entity.groupEnabled": "false",
    "entity.workflowEnabled": "false",
    "entity.description": `${marker}_${entityName}`,
  };
}

function modelPayload(entityCode, modelName, tableName) {
  return {
    "model.version": "0",
    "model.code": "",
    "entity.code": entityCode,
    "model.entity.code": entityCode,
    "model.moduleCode": moduleCode,
    "model.orgTableName": "",
    "model.modelName": modelName,
    "model.tableName": tableName,
    "model.name": `${marker} ${modelName}`,
    "model.dataType": "1",
    "model.isMain": "false",
    "model.isExtraCol": "false",
    "model.isCache": "false",
    "model.enableSync": "false",
    "model.type": "0",
    "model.description": `${marker}_${modelName}`,
  };
}

function basePropertyPayload(item, modelCode, entityCode, isNew, version = "0") {
  return {
    "property.version": version,
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
    "property.orgColumnName": isNew ? "" : item.columnName,
    "property.isGroupObject": "false",
    "property.onlyLeaf": "false",
    "property.isUsedForList": "true",
    "property.isCustom": "false",
    "property.isUsedForSearch": "false",
    "property.isIgnoreAudit": "false",
    "property.noAnalyzer": "false",
    "property.isMainAssociated": "false",
    "property.isMainDisplay": "false",
    "property.name": item.name,
    "property.columnName": item.columnName,
    "property.displayName": `${marker} ${item.key}`,
    "property.fetchMode": "SELECT",
    "property.description": `${marker}_${item.key}`,
  };
}

function targetPropertyPayload(item) {
  return {
    ...basePropertyPayload(item, targetModelCode, targetEntityCode, true),
    "property.type": item.type,
    "property.fieldType": item.fieldType,
    "property.format": item.format,
    "property.maxLength": item.maxLength || "",
    "property.decimalNum": "",
  };
}

function objectPropertyPayload(item, options = {}) {
  const target = targetProperties.find((candidate) => candidate.key === item.targetKey);
  const isNew = options.isNew !== false;
  const payload = {
    ...basePropertyPayload(item, sourceModelCode, sourceEntityCode, isNew, options.version || "0"),
    "property.type": "OBJECT",
    "property.fieldType": "SELECTCOMP",
    "property.format": "SELECTCOMP",
    "property.maxLength": "",
    "property.decimalNum": "",
    "property.associatedType": item.associatedType,
  };
  const associatedCode = options.associatedPropertyCode || target.propertyCode;
  if ((options.parameterStyle || item.parameterStyle) === "legacyUnderscore") {
    payload.property_associatedProperty_code = associatedCode;
  } else {
    payload["property.associatedProperty.code"] = associatedCode;
  }
  return payload;
}

function invalidObjectPayload() {
  const item = {
    ...invalidObject,
    associatedType: "2",
    targetKey: "longKey",
    parameterStyle: "dotted",
  };
  return objectPropertyPayload(item, {
    isNew: true,
    associatedPropertyCode: invalidObject.missingAssociatedPropertyCode,
  });
}

function objectMetadataSql() {
  return [
    "select p.code::text, coalesce(p.version::text,'0'), p.name::text, p.column_name::text,",
    "p.associated_type::text, p.associated_property_code::text, ap.type::text,",
    "coalesce(ap.max_length::text,''), p.model_code::text, p.description::text",
    "from public.ec_property p join public.ec_property ap on ap.code=p.associated_property_code",
    `where p.model_code=${sqlLiteral(sourceModelCode)}`,
    `and p.code in (${objectProperties.map((item) => sqlLiteral(item.propertyCode)).join(",")})`,
    "order by p.code;",
  ].join(" ");
}

function columnsSql(tableName) {
  return [
    "select column_name::text, data_type::text, coalesce(character_maximum_length::text,''),",
    "coalesce(numeric_precision::text,''), coalesce(numeric_scale::text,''), is_nullable::text",
    "from information_schema.columns where table_schema='public'",
    `and lower(table_name)=lower(${sqlLiteral(tableName)}) order by ordinal_position;`,
  ].join(" ");
}

function markerInsertSql() {
  const targetColumns = ["id", "valid", ...targetProperties.map((item) => sqlIdentifier(item.columnName))];
  const targetValues = [String(targetProbeId), "1", ...targetProperties.map((item) => item.sqlValue)];
  const sourceColumns = ["id", "valid", ...objectProperties.map((item) => sqlIdentifier(item.columnName))];
  const sourceValues = [String(sourceProbeId), "1", ...objectProperties.map((item) => item.sqlValue)];
  return [
    "begin;",
    `insert into public.${sqlIdentifier(targetTableName)} (${targetColumns.join(",")}) values (${targetValues.join(",")});`,
    `insert into public.${sqlIdentifier(sourceTableName)} (${sourceColumns.join(",")}) values (${sourceValues.join(",")});`,
    "commit;",
  ].join(" ");
}

function sourceProbeSql() {
  return [
    "select id::text,",
    ...objectProperties.map((item, index) =>
      `${index > 0 ? "" : ""}coalesce(${sqlIdentifier(item.columnName)}::text,'<NULL>')${
        index === objectProperties.length - 1 ? "" : ","
      }`
    ),
    `from public.${sqlIdentifier(sourceTableName)} where id=${sourceProbeId};`,
  ].join(" ");
}

function joinCountsSql() {
  const expressions = objectProperties.map((item) => {
    const target = targetProperties.find((candidate) => candidate.key === item.targetKey);
    return `(select count(*) from public.${sqlIdentifier(sourceTableName)} s join public.${sqlIdentifier(
      targetTableName
    )} t on s.${sqlIdentifier(item.columnName)}=t.${sqlIdentifier(target.columnName)} where s.id=${sourceProbeId})`;
  });
  return `select ${expressions.join(",")};`;
}

function foreignKeyCountSql() {
  return [
    "select count(*) from pg_constraint c",
    "join pg_class t on t.oid=c.conrelid join pg_namespace n on n.oid=t.relnamespace",
    `where n.nspname='public' and lower(t.relname)=lower(${sqlLiteral(sourceTableName)}) and c.contype='f';`,
  ].join(" ");
}

function invalidResidualSql() {
  return [
    "select",
    `(select count(*) from public.ec_property where code=${sqlLiteral(invalidObject.propertyCode)}),`,
    `(select count(*) from information_schema.columns where table_schema='public' and lower(table_name)=lower(${sqlLiteral(
      sourceTableName
    )}) and lower(column_name)=lower(${sqlLiteral(invalidObject.columnName)}));`,
  ].join(" ");
}

function queryState(label, includeProbe = false) {
  return {
    label,
    metadata: parseRows(runSql(objectMetadataSql()), [
      "code",
      "version",
      "name",
      "columnName",
      "associatedType",
      "associatedPropertyCode",
      "associatedPropertyType",
      "associatedMaxLength",
      "modelCode",
      "description",
    ]),
    sourceColumns: parseRows(runSql(columnsSql(sourceTableName)), [
      "columnName",
      "dataType",
      "characterLength",
      "numericPrecision",
      "numericScale",
      "nullable",
    ]),
    targetColumns: parseRows(runSql(columnsSql(targetTableName)), [
      "columnName",
      "dataType",
      "characterLength",
      "numericPrecision",
      "numericScale",
      "nullable",
    ]),
    sourceProbe: includeProbe
      ? parseRows(runSql(sourceProbeSql()), ["id", ...objectProperties.map((item) => item.key)])
      : [],
    joinCounts: includeProbe ? runSql(joinCountsSql()).split("|").map((value) => Number(value || 0)) : [],
    foreignKeyCount: Number(runSql(foreignKeyCountSql()) || 0),
  };
}

function cleanupSql() {
  return [
    "begin;",
    `delete from public.ec_property where model_code=${sqlLiteral(sourceModelCode)} or entity_code=${sqlLiteral(
      sourceEntityCode
    )};`,
    `delete from public.ec_property where model_code=${sqlLiteral(targetModelCode)} or entity_code=${sqlLiteral(
      targetEntityCode
    )};`,
    `delete from public.ec_model where code=${sqlLiteral(sourceModelCode)} or entity_code=${sqlLiteral(sourceEntityCode)};`,
    `delete from public.ec_model where code=${sqlLiteral(targetModelCode)} or entity_code=${sqlLiteral(targetEntityCode)};`,
    `delete from public.ec_entity where code in (${sqlLiteral(sourceEntityCode)},${sqlLiteral(targetEntityCode)});`,
    `drop table if exists public.${sqlIdentifier(sourceTableName)} cascade;`,
    `drop table if exists public.${sqlIdentifier(targetTableName)} cascade;`,
    "commit;",
  ].join(" ");
}

function stateCounts() {
  const output = runSql(
    [
      "select",
      `(select count(*) from public.ec_property where model_code in (${sqlLiteral(sourceModelCode)},${sqlLiteral(
        targetModelCode
      )}) or entity_code in (${sqlLiteral(sourceEntityCode)},${sqlLiteral(targetEntityCode)})),`,
      `(select count(*) from public.ec_model where code in (${sqlLiteral(sourceModelCode)},${sqlLiteral(
        targetModelCode
      )}) or entity_code in (${sqlLiteral(sourceEntityCode)},${sqlLiteral(targetEntityCode)})),`,
      `(select count(*) from public.ec_entity where code in (${sqlLiteral(sourceEntityCode)},${sqlLiteral(
        targetEntityCode
      )})),`,
      `(select count(*) from information_schema.tables where table_schema='public' and lower(table_name) in (lower(${sqlLiteral(
        sourceTableName
      )}),lower(${sqlLiteral(targetTableName)})));`,
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

function metadataFor(state, item) {
  return state.metadata.find((row) => row.code === item.propertyCode);
}

function sourceColumnFor(state, item) {
  return state.sourceColumns.find((row) => row.columnName.toLowerCase() === item.columnName.toLowerCase());
}

function columnMatches(column, expected) {
  if (!column || column.dataType !== expected.dataType || column.nullable !== "YES") {
    return false;
  }
  if (expected.characterLength && column.characterLength !== expected.characterLength) {
    return false;
  }
  return true;
}

function check(name, passed, evidence) {
  return { name, status: passed ? "PASS" : "FAIL", evidence };
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
  let invalidResidual = null;
  let cleanup = null;
  let fatalError = null;
  try {
    requests.targetEntityCreate = await pageFormFetch(
      page,
      "/msService/ec/entity/save",
      entityPayload(targetEntityCode, targetEntityName, "EOT")
    );
    requests.targetModelCreate = await pageFormFetch(
      page,
      "/msService/ec/model/save",
      modelPayload(targetEntityCode, targetModelName, targetTableName)
    );
    for (const item of targetProperties) {
      requests[`createTarget_${item.key}`] = await pageFormFetch(
        page,
        "/msService/ec/property/save",
        targetPropertyPayload(item)
      );
    }

    requests.sourceEntityCreate = await pageFormFetch(
      page,
      "/msService/ec/entity/save",
      entityPayload(sourceEntityCode, sourceEntityName, "EOS")
    );
    requests.sourceModelCreate = await pageFormFetch(
      page,
      "/msService/ec/model/save",
      modelPayload(sourceEntityCode, sourceModelName, sourceTableName)
    );
    for (const item of objectProperties) {
      requests[`createObject_${item.key}`] = await pageFormFetch(
        page,
        "/msService/ec/property/save",
        objectPropertyPayload(item, { isNew: true })
      );
    }
    states.afterCreate = queryState("afterCreate");

    runSql(markerInsertSql());
    states.afterMarkerInsert = queryState("afterMarkerInsert", true);

    const replayItem = objectProperties[1];
    const replayMetadata = metadataFor(states.afterMarkerInsert, replayItem);
    requests.objectReplay = await pageFormFetch(
      page,
      "/msService/ec/property/save",
      objectPropertyPayload(replayItem, {
        isNew: false,
        version: replayMetadata ? replayMetadata.version : "0",
      })
    );
    states.afterReplay = queryState("afterReplay", true);

    requests.invalidTarget = await pageFormFetch(
      page,
      "/msService/ec/property/save",
      invalidObjectPayload()
    );
    invalidResidual = runSql(invalidResidualSql()).split("|").map((value) => Number(value || 0));
    if (requests.invalidTarget && requests.invalidTarget.responseStatus >= 400) {
      const expectedNetwork = browserEvidence.networkErrors.find(
        (item) =>
          !item.expected && item.url.includes("/msService/ec/property/save") &&
          item.status === requests.invalidTarget.responseStatus
      );
      if (expectedNetwork) {
        expectedNetwork.expected = true;
      }
      browserEvidence.expectedConsoleErrors = browserEvidence.consoleErrors.filter((message) =>
        /Failed to load resource: the server responded with a status of [45]\d\d/i.test(message)
      );
    }
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

  const expectedConsoleErrors = browserEvidence.expectedConsoleErrors || [];
  const unexpectedConsoleErrors = browserEvidence.consoleErrors.filter(
    (message) => !expectedConsoleErrors.includes(message)
  );
  const unexpectedNetworkErrors = browserEvidence.networkErrors.filter((item) => !item.expected);
  const validRequestKeys = [
    "targetEntityCreate",
    "targetModelCreate",
    ...targetProperties.map((item) => `createTarget_${item.key}`),
    "sourceEntityCreate",
    "sourceModelCreate",
    ...objectProperties.map((item) => `createObject_${item.key}`),
  ];
  const checks = [
    check(
      "browser-page-context",
      browserEvidence.navigationStatus && browserEvidence.navigationStatus < 400 && !browserEvidence.visibleError &&
        unexpectedConsoleErrors.length === 0 && browserEvidence.pageErrors.length === 0 &&
        browserEvidence.requestFailures.length === 0 && unexpectedNetworkErrors.length === 0,
      `status=${browserEvidence.navigationStatus}; visibleError=${browserEvidence.visibleError || "none"}; ` +
        `unexpectedConsole=${unexpectedConsoleErrors.length}; expectedConsole=${expectedConsoleErrors.length}; ` +
        `page=${browserEvidence.pageErrors.length}; request=${browserEvidence.requestFailures.length}; ` +
        `unexpectedNetwork=${unexpectedNetworkErrors.length}`
    ),
    check(
      "entity-model-target-and-object-create-requests",
      validRequestKeys.every((key) => responseBusinessOk(requests[key])),
      JSON.stringify(Object.fromEntries(validRequestKeys.map((key) => [key, requests[key] && requests[key].responseStatus])))
    ),
    check(
      "object-association-metadata-persistence",
      states.afterCreate && objectProperties.every((item) => {
        const metadata = metadataFor(states.afterCreate, item);
        const target = targetProperties.find((candidate) => candidate.key === item.targetKey);
        return metadata && metadata.associatedType === item.associatedType &&
          metadata.associatedPropertyCode === target.propertyCode &&
          metadata.associatedPropertyType === target.type && metadata.modelCode === sourceModelCode;
      }),
      JSON.stringify((states.afterCreate && states.afterCreate.metadata) || [])
    ),
    check(
      "postgres-object-physical-column-types",
      states.afterCreate && objectProperties.every((item) =>
        columnMatches(sourceColumnFor(states.afterCreate, item), item.expected)
      ),
      JSON.stringify(
        objectProperties.map((item) => ({ key: item.key, column: states.afterCreate && sourceColumnFor(states.afterCreate, item) }))
      )
    ),
    check(
      "legacy-underscore-association-parameter",
      responseBusinessOk(requests.createObject_manyToOneLong) &&
        Object.prototype.hasOwnProperty.call(
          requests.createObject_manyToOneLong.requestPayload,
          "property_associatedProperty_code"
        ) && metadataFor(states.afterCreate, objectProperties[1]).associatedPropertyCode === targetProperties[0].propertyCode,
      JSON.stringify(requests.createObject_manyToOneLong && requests.createObject_manyToOneLong.requestPayload)
    ),
    check(
      "marker-row-round-trip-and-logical-association-joins",
      states.afterMarkerInsert && states.afterMarkerInsert.sourceProbe.length === 1 &&
        states.afterMarkerInsert.sourceProbe[0].id === String(sourceProbeId) &&
        objectProperties.every((item) => states.afterMarkerInsert.sourceProbe[0][item.key] ===
          String(item.sqlValue).replace(/^'|'$/g, "").replace(/''/g, "'")) &&
        states.afterMarkerInsert.joinCounts.length === objectProperties.length &&
        states.afterMarkerInsert.joinCounts.every((count) => count === 1),
      `probe=${JSON.stringify(states.afterMarkerInsert && states.afterMarkerInsert.sourceProbe)}; ` +
        `joins=${JSON.stringify(states.afterMarkerInsert && states.afterMarkerInsert.joinCounts)}`
    ),
    check(
      "object-property-replay-is-idempotent",
      responseBusinessOk(requests.objectReplay) && states.afterReplay &&
        states.afterReplay.metadata.length === objectProperties.length &&
        states.afterReplay.sourceProbe.length === 1 && states.afterReplay.joinCounts.every((count) => count === 1),
      `response=${requests.objectReplay && requests.objectReplay.responseStatus}; ` +
        `metadata=${states.afterReplay && states.afterReplay.metadata.length}; joins=${JSON.stringify(
          states.afterReplay && states.afterReplay.joinCounts
        )}`
    ),
    check(
      "invalid-associated-property-rolls-back",
      requests.invalidTarget && requests.invalidTarget.responseStatus === 500 && invalidResidual &&
        invalidResidual[0] === 0 && invalidResidual[1] === 0,
      `response=${requests.invalidTarget && requests.invalidTarget.responseStatus}; ` +
        `body=${requests.invalidTarget && requests.invalidTarget.text}; residual=${JSON.stringify(invalidResidual)}`
    ),
    check(
      "legacy-object-storage-does-not-invent-physical-foreign-keys",
      states.afterReplay && states.afterReplay.foreignKeyCount === 0,
      `foreignKeyCount=${states.afterReplay && states.afterReplay.foreignKeyCount}`
    ),
    check(
      "controlled-cleanup",
      cleanup && cleanup.property === 0 && cleanup.model === 0 && cleanup.entity === 0 && cleanup.physicalTable === 0,
      `cleanup=${JSON.stringify(cleanup)}`
    ),
  ];

  const pass = checks.filter((item) => item.status === "PASS").length;
  const fail = checks.filter((item) => item.status === "FAIL").length;
  const acceptanceStatus = fail === 0 && !fatalError ? "PASS" : fatalError ? "BLOCKED" : "FAIL";
  const report = {
    schemaVersion: 1,
    generatedAt: new Date().toISOString(),
    repoCommit: getRepoCommit(),
    database: "PostgreSQL",
    module: "basic-config",
    actionId: "entity-model-postgres-object-association",
    areaId: "configuration-physical-model-field",
    status: acceptanceStatus,
    marker,
    route: pagePath,
    operation: "Create target key fields and OBJECT association fields, replay, reject missing target, verify and clean",
    expectedResult:
      "One-to-one and many-to-one metadata point to real target properties; PostgreSQL columns mirror legacy key storage and all marker joins resolve",
    actualResult: acceptanceStatus,
    apiEndpoints: [
      "POST /msService/ec/entity/save",
      "POST /msService/ec/model/save",
      "POST /msService/ec/property/save",
    ],
    identifiers: {
      targetEntityCode,
      sourceEntityCode,
      targetModelCode,
      sourceModelCode,
      targetTableName,
      sourceTableName,
      targetProbeId,
      sourceProbeId,
      targetProperties,
      objectProperties,
      invalidObject,
    },
    coverage: {
      associationTypes: ["ONE_TO_ONE", "MANY_TO_ONE"],
      targetKeyTypes: ["LONG", "TEXT", "BAPCODE"],
      requestParameterStyles: ["property.associatedProperty.code", "property_associatedProperty_code"],
      persistenceLayers: ["ec_property", "information_schema.columns", targetTableName, sourceTableName],
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
    invalidResidual,
    cleanup,
    backendTrace: {
      endpoint: "PropertyController.save -> DtoUtils.getPropertyVO -> ModelServiceImpl.saveProperty",
      targetResolution:
        "ModelServiceImpl.getProperty resolves the submitted associated property code and rejects missing targets inside the transaction",
      metadataPersistence: "propertyDao.merge -> public.ec_property.associated_property_code / associated_type",
      physicalPersistence:
        "FieldSyncDBUtils.fieldSyncToDb -> PostgresFieldSyncSupport.objectColumnSpec -> ALTER TABLE",
      storagePolicy:
        "LONG target keys use bigint; BAPCODE uses varchar(4000); legacy character-key associations use bounded varchar. No synthetic PostgreSQL FK is added because legacy runtime associations are metadata-driven.",
      transactionBoundary:
        "Invalid associated-property resolution returns HTTP 500 and leaves neither ec_property metadata nor a physical column",
    },
    verificationSql: {
      metadata: objectMetadataSql(),
      sourceColumns: columnsSql(sourceTableName),
      targetColumns: columnsSql(targetTableName),
      markerInsert: markerInsertSql(),
      markerReadback: sourceProbeSql(),
      logicalJoins: joinCountsSql(),
      invalidResidual: invalidResidualSql(),
      foreignKeyCount: foreignKeyCountSql(),
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
