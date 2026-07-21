#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { execFileSync, spawnSync } = require("child_process");
const { chromium, request } = require("playwright");

const repoRoot = path.resolve(__dirname, "../../..");
const stamp = new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14);
const baseUrl = (process.env.ADP_BASE_URL || "http://10.11.100.17:18080").replace(/\/+$/, "");
const browserBaseUrl = (process.env.ADP_BROWSER_BASE_URL || baseUrl).replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD || "123456";
const outputPath =
  process.env.ADP_ENTITY_MODEL_FIELD_ACCEPTANCE_OUTPUT ||
  path.join("/tmp", `adp-entity-model-field-persistence-${stamp}.json`);
const dbSshTarget = process.env.ADP_DB_SSH_TARGET || "v6@10.11.100.17";
const dbContainer = process.env.ADP_DB_CONTAINER || "adp-mes-newbase-postgres-1";
const dbName = process.env.ADP_DB_NAME || "adp";
const dbUser = process.env.ADP_DB_USER || "adp";
const sshConnectTimeout = process.env.ADP_SSH_CONNECT_TIMEOUT || "20";
const dbQueryTimeoutMs = Number(process.env.ADP_DB_QUERY_TIMEOUT_MS || "30000");
const headless = process.env.ADP_HEADLESS !== "false";

const marker = process.env.ADP_E2E_MARKER || `ADP_E2E_${stamp}_PG_FIELD`;
const moduleCode = process.env.ADP_ENTITY_MODEL_MODULE_CODE || "DataSet_1.0.0";
const suffix = stamp.slice(8, 14);
const entityName = process.env.ADP_ENTITY_MODEL_ENTITY_NAME || `E2eFldEnt${suffix}`;
const modelName = process.env.ADP_ENTITY_MODEL_MODEL_NAME || `E2eFldMod${suffix}`;
const entityCode = `${moduleCode}_${entityName}`;
const modelCode = `${entityCode}_${modelName}`;
const propertyName = process.env.ADP_ENTITY_MODEL_PROPERTY_NAME || `e2eText${suffix}`;
const propertyCode = `${modelCode}_${propertyName}`;
const tableName = process.env.ADP_ENTITY_MODEL_TABLE_NAME || `DS_E2EF_${suffix}`;
const initialColumn = process.env.ADP_ENTITY_MODEL_FIELD_COLUMN || `E2E_TXT_${suffix}`;
const renamedColumn = process.env.ADP_ENTITY_MODEL_RENAMED_FIELD_COLUMN || `E2E_TEXT_${suffix}`;
const probeId = Number(`${Date.now()}`.slice(-14));
const duplicateProbeId = probeId + 1;
const nullProbeId = probeId + 2;
const probeValue = `${marker}_ROW`;
const pagePath = "/msService/ec/engine/msManage";
const visibleErrorPattern =
  /(数据库操作异常|系统错误|系统异常|发生未知异常|SQLGrammarException|could not extract ResultSet|column .* does not exist|relation .* does not exist|500 INTERNAL|org\.hibernate\.[\w.]+Exception|org\.springframework\.[\w.]+Exception|java\.lang\.[\w.]+Exception|Invalid bound statement)/i;

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

function runSqlExpectFailure(sql) {
  try {
    const output = runSql(sql);
    return { rejected: false, output, error: "" };
  } catch (error) {
    const stderr = error && error.stderr ? String(error.stderr) : "";
    return {
      rejected: true,
      output: error && error.stdout ? String(error.stdout).trim() : "",
      error: (stderr || (error && error.message) || String(error)).trim().slice(0, 2000),
    };
  }
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
  page.on("response", async (response) => {
    if (response.status() < 400) {
      return;
    }
    evidence.networkErrors.push({
      status: response.status(),
      url: response.url(),
      expected: false,
    });
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
    "entity.prefix": "E2F",
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

function propertyPayload({
  version = "0",
  columnName,
  orgColumnName = "",
  type = "TEXT",
  maxLength = "32",
  isIndex = true,
  isUnique = false,
  nullable = true,
  isNew = false,
}) {
  return {
    "property.version": version,
    "property.sort": "1",
    "property.model.code": modelCode,
    "model.enableDataAudit": "false",
    "property.entityCode": entityCode,
    "property.moduleCode": moduleCode,
    "property.code": isNew ? "" : propertyCode,
    "property.defaultValue": "",
    "property.fillcontent": "",
    "property.attributes": "",
    "property.isControl": "false",
    "property.isUnique": String(isUnique),
    "property.isHidden": "false",
    "property.nullable": String(nullable),
    "property.multable": "false",
    "property.seniorSystemCode": "false",
    "property.sensitive": "false",
    "property.stretch": "false",
    "property.isBussinessKey": "false",
    "property.isUsedMneCode": "false",
    "property.isIndex": String(isIndex),
    "property.orgColumnName": orgColumnName,
    "property.isGroupObject": "false",
    "property.onlyLeaf": "false",
    "property.isUsedForList": "true",
    "property.isCustom": "false",
    "property.isUsedForSearch": "false",
    "property.isIgnoreAudit": "false",
    "property.noAnalyzer": "false",
    "property.isMainAssociated": "false",
    "property.isMainDisplay": "false",
    "property.name": propertyName,
    "property.columnName": columnName,
    "property.displayName": `${marker} field`,
    "property.type": type,
    "property.fieldType": "TEXTFIELD",
    "property.format": "TEXT",
    "property.maxLength": maxLength,
    "property.fetchMode": "SELECT",
    "property.description": `${marker}_PROPERTY`,
  };
}

function metadataSql() {
  return [
    "select code::text, coalesce(version::text,'0'), coalesce(valid::text,''),",
    "coalesce(name::text,''), coalesce(display_name::text,''), coalesce(type::text,''),",
    "coalesce(column_name::text,''), coalesce(max_length::text,''), coalesce(is_index::text,''),",
    "coalesce(is_unique::text,''), coalesce(nullable::text,''),",
    "coalesce(model_code::text,''), coalesce(description::text,'')",
    "from public.ec_property",
    `where code=${sqlLiteral(propertyCode)}`,
    "order by modify_time desc nulls last, create_time desc nulls last;",
  ].join(" ");
}

function columnSql() {
  return [
    "select column_name::text, data_type::text, coalesce(character_maximum_length::text,''),",
    "coalesce(numeric_precision::text,''), coalesce(numeric_scale::text,''), is_nullable::text",
    "from information_schema.columns",
    "where table_schema='public'",
    `and lower(table_name)=lower(${sqlLiteral(tableName)})`,
    `and lower(column_name) in (lower(${sqlLiteral(initialColumn)}), lower(${sqlLiteral(renamedColumn)}))`,
    "order by ordinal_position;",
  ].join(" ");
}

function indexSql() {
  return [
    "select i.relname::text, a.attname::text, x.indisunique::text",
    "from pg_index x",
    "join pg_class t on t.oid=x.indrelid",
    "join pg_namespace n on n.oid=t.relnamespace",
    "join pg_class i on i.oid=x.indexrelid",
    "join pg_attribute a on a.attrelid=t.oid and a.attnum=any(x.indkey)",
    "where n.nspname='public' and not x.indisprimary",
    `and lower(t.relname)=lower(${sqlLiteral(tableName)})`,
    `and lower(a.attname) in (lower(${sqlLiteral(initialColumn)}), lower(${sqlLiteral(renamedColumn)}))`,
    "order by i.relname;",
  ].join(" ");
}

function constraintSql() {
  return [
    "select c.conname::text, i.relname::text, a.attname::text,",
    "c.convalidated::text, c.condeferrable::text",
    "from pg_constraint c",
    "join pg_class t on t.oid=c.conrelid",
    "join pg_namespace n on n.oid=t.relnamespace",
    "join pg_class i on i.oid=c.conindid",
    "join pg_attribute a on a.attrelid=t.oid and a.attnum=any(c.conkey)",
    "where n.nspname='public' and c.contype='u' and cardinality(c.conkey)=1",
    `and lower(t.relname)=lower(${sqlLiteral(tableName)})`,
    `and lower(a.attname) in (lower(${sqlLiteral(initialColumn)}), lower(${sqlLiteral(renamedColumn)}))`,
    "order by c.conname;",
  ].join(" ");
}

function probeSql(columnName) {
  return [
    `select id::text, coalesce(${sqlIdentifier(columnName)}::text,'')`,
    `from public.${sqlIdentifier(tableName)}`,
    `where id=${probeId};`,
  ].join(" ");
}

function nullProbeSql(columnName) {
  return [
    "select id::text",
    `from public.${sqlIdentifier(tableName)}`,
    `where id=${nullProbeId} and ${sqlIdentifier(columnName)} is null;`,
  ].join(" ");
}

function duplicateProbeSql(columnName) {
  return [
    `select id::text, coalesce(${sqlIdentifier(columnName)}::text,'')`,
    `from public.${sqlIdentifier(tableName)}`,
    `where id=${duplicateProbeId};`,
  ].join(" ");
}

function queryState(label, probeColumn) {
  return {
    label,
    metadata: parseRows(runSql(metadataSql()), [
      "code",
      "version",
      "valid",
      "name",
      "displayName",
      "type",
      "columnName",
      "maxLength",
      "isIndex",
      "isUnique",
      "nullable",
      "modelCode",
      "description",
    ]),
    columns: parseRows(runSql(columnSql()), [
      "columnName",
      "dataType",
      "characterLength",
      "numericPrecision",
      "numericScale",
      "nullable",
    ]),
    indexes: parseRows(runSql(indexSql()), ["indexName", "columnName", "isUnique"]),
    constraints: parseRows(runSql(constraintSql()), [
      "constraintName",
      "indexName",
      "columnName",
      "validated",
      "deferrable",
    ]),
    probe: probeColumn ? parseRows(runSql(probeSql(probeColumn)), ["id", "value"]) : [],
    duplicateProbe: probeColumn
      ? parseRows(runSql(duplicateProbeSql(probeColumn)), ["id", "value"])
      : [],
    nullProbe: probeColumn ? parseRows(runSql(nullProbeSql(probeColumn)), ["id"]) : [],
  };
}

function cleanupSql() {
  return [
    "begin;",
    "delete from public.ec_property",
    `where code=${sqlLiteral(propertyCode)} or model_code=${sqlLiteral(modelCode)} or entity_code=${sqlLiteral(entityCode)};`,
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
      `(select count(*) from public.ec_property where code=${sqlLiteral(propertyCode)}),`,
      `(select count(*) from public.ec_model where code=${sqlLiteral(modelCode)}),`,
      `(select count(*) from public.ec_entity where code=${sqlLiteral(entityCode)}),`,
      `(select count(*) from information_schema.tables where table_schema='public' and lower(table_name)=lower(${sqlLiteral(tableName)}));`,
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

function columnByName(state, name) {
  return state.columns.find((row) => row.columnName.toLowerCase() === name.toLowerCase());
}

function hasProbe(state) {
  return state.probe.length === 1 && state.probe[0].value === probeValue;
}

function hasIndex(state, indexName) {
  return state.indexes.some((row) => row.indexName.toLowerCase() === indexName.toLowerCase());
}

function hasConstraint(state, constraintName) {
  return state.constraints.some(
    (row) => row.constraintName.toLowerCase() === constraintName.toLowerCase()
  );
}

function metadataValue(state, key) {
  return state && state.metadata && state.metadata[0] ? state.metadata[0][key] : "";
}

function columnNullable(state, columnName) {
  const column = state && state.columns ? columnByName(state, columnName) : null;
  return column ? column.nullable : "";
}

function storedBooleanEquals(value, expected) {
  const normalized = String(value).trim().toLowerCase();
  const trueValues = new Set(["1", "true", "t", "yes", "y"]);
  const falseValues = new Set(["0", "false", "f", "no", "n"]);
  return expected ? trueValues.has(normalized) : falseValues.has(normalized);
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
  const dbActions = {};
  const states = { before };
  let cleanup = null;
  let fatalError = null;
  try {
    requests.entityCreate = await pageFormFetch(page, "/msService/ec/entity/save", entityPayload());
    requests.modelCreate = await pageFormFetch(page, "/msService/ec/model/save", modelPayload());
    requests.fieldCreate = await pageFormFetch(
      page,
      "/msService/ec/property/save",
      propertyPayload({ columnName: initialColumn, isNew: true })
    );
    states.afterCreate = queryState("afterCreate", null);

    runSql(
      `insert into public.${sqlIdentifier(tableName)} (id, ${sqlIdentifier(initialColumn)}, valid) values (` +
        `${probeId}, ${sqlLiteral(probeValue)}, 1);`
    );
    states.afterProbeInsert = queryState("afterProbeInsert", initialColumn);

    const created = states.afterProbeInsert.metadata[0] || {};
    requests.fieldIndexDisable = await pageFormFetch(
      page,
      "/msService/ec/property/save",
      propertyPayload({
        version: created.version || "0",
        columnName: initialColumn,
        orgColumnName: initialColumn,
        isIndex: false,
      })
    );
    states.afterIndexDisable = queryState("afterIndexDisable", initialColumn);

    const indexDisabled = states.afterIndexDisable.metadata[0] || {};
    requests.fieldIndexDisableReplay = await pageFormFetch(
      page,
      "/msService/ec/property/save",
      propertyPayload({
        version: indexDisabled.version || "0",
        columnName: initialColumn,
        orgColumnName: initialColumn,
        isIndex: false,
      })
    );
    states.afterIndexDisableReplay = queryState("afterIndexDisableReplay", initialColumn);

    const indexDisableReplayed = states.afterIndexDisableReplay.metadata[0] || {};
    requests.fieldIndexEnable = await pageFormFetch(
      page,
      "/msService/ec/property/save",
      propertyPayload({
        version: indexDisableReplayed.version || "0",
        columnName: initialColumn,
        orgColumnName: initialColumn,
        isIndex: true,
      })
    );
    states.afterIndexEnable = queryState("afterIndexEnable", initialColumn);

    const indexEnabled = states.afterIndexEnable.metadata[0] || {};
    requests.fieldUniqueEnable = await pageFormFetch(
      page,
      "/msService/ec/property/save",
      propertyPayload({
        version: indexEnabled.version || "0",
        columnName: initialColumn,
        orgColumnName: initialColumn,
        isIndex: true,
        isUnique: true,
      })
    );
    states.afterUniqueEnable = queryState("afterUniqueEnable", initialColumn);

    const uniqueEnabled = states.afterUniqueEnable.metadata[0] || {};
    requests.fieldUniqueEnableReplay = await pageFormFetch(
      page,
      "/msService/ec/property/save",
      propertyPayload({
        version: uniqueEnabled.version || "0",
        columnName: initialColumn,
        orgColumnName: initialColumn,
        isIndex: true,
        isUnique: true,
      })
    );
    states.afterUniqueEnableReplay = queryState("afterUniqueEnableReplay", initialColumn);

    dbActions.duplicateInsert = runSqlExpectFailure(
      `insert into public.${sqlIdentifier(tableName)} (id, ${sqlIdentifier(initialColumn)}, valid) values (` +
        `${duplicateProbeId}, ${sqlLiteral(probeValue)}, 1);`
    );
    states.afterDuplicateReject = queryState("afterDuplicateReject", initialColumn);

    const uniqueReplayed = states.afterDuplicateReject.metadata[0] || {};
    requests.fieldRenameAndWiden = await pageFormFetch(
      page,
      "/msService/ec/property/save",
      propertyPayload({
        version: uniqueReplayed.version || "0",
        columnName: renamedColumn,
        orgColumnName: initialColumn,
        maxLength: "128",
        isUnique: true,
      })
    );
    states.afterRename = queryState("afterRename", renamedColumn);

    const renamedUnique = states.afterRename.metadata[0] || {};
    requests.fieldUniqueDisable = await pageFormFetch(
      page,
      "/msService/ec/property/save",
      propertyPayload({
        version: renamedUnique.version || "0",
        columnName: renamedColumn,
        orgColumnName: renamedColumn,
        maxLength: "128",
        isIndex: true,
        isUnique: false,
      })
    );
    states.afterUniqueDisable = queryState("afterUniqueDisable", renamedColumn);

    const uniqueDisabled = states.afterUniqueDisable.metadata[0] || {};
    requests.fieldUniqueDisableReplay = await pageFormFetch(
      page,
      "/msService/ec/property/save",
      propertyPayload({
        version: uniqueDisabled.version || "0",
        columnName: renamedColumn,
        orgColumnName: renamedColumn,
        maxLength: "128",
        isIndex: true,
        isUnique: false,
      })
    );
    states.afterUniqueDisableReplay = queryState("afterUniqueDisableReplay", renamedColumn);

    const uniqueDisableReplayed = states.afterUniqueDisableReplay.metadata[0] || {};
    requests.fieldOrdinaryIndexRenameToInitial = await pageFormFetch(
      page,
      "/msService/ec/property/save",
      propertyPayload({
        version: uniqueDisableReplayed.version || "0",
        columnName: initialColumn,
        orgColumnName: renamedColumn,
        maxLength: "128",
        isIndex: true,
        isUnique: false,
      })
    );
    states.afterOrdinaryIndexRenameToInitial = queryState("afterOrdinaryIndexRenameToInitial", initialColumn);

    const ordinaryIndexRenamedToInitial = states.afterOrdinaryIndexRenameToInitial.metadata[0] || {};
    requests.fieldOrdinaryIndexRenameToFinal = await pageFormFetch(
      page,
      "/msService/ec/property/save",
      propertyPayload({
        version: ordinaryIndexRenamedToInitial.version || "0",
        columnName: renamedColumn,
        orgColumnName: initialColumn,
        maxLength: "128",
        isIndex: true,
        isUnique: false,
      })
    );
    states.afterOrdinaryIndexRenameToFinal = queryState("afterOrdinaryIndexRenameToFinal", renamedColumn);

    const externalIndexName = `EXT_${tableName}_${renamedColumn}`;
    runSql(
      `create unique index ${sqlIdentifier(externalIndexName)} on public.${sqlIdentifier(tableName)} ` +
        `(${sqlIdentifier(renamedColumn)});`
    );
    states.afterExternalIndexCreate = queryState("afterExternalIndexCreate", renamedColumn);

    const renamed = states.afterExternalIndexCreate.metadata[0] || {};
    requests.fieldIndexDisableWithExternal = await pageFormFetch(
      page,
      "/msService/ec/property/save",
      propertyPayload({
        version: renamed.version || "0",
        columnName: renamedColumn,
        orgColumnName: renamedColumn,
        maxLength: "128",
        isIndex: false,
      })
    );
    states.afterExternalIndexDisable = queryState("afterExternalIndexDisable", renamedColumn);

    const externalIndexDisabled = states.afterExternalIndexDisable.metadata[0] || {};
    requests.fieldIndexEnableWithExternal = await pageFormFetch(
      page,
      "/msService/ec/property/save",
      propertyPayload({
        version: externalIndexDisabled.version || "0",
        columnName: renamedColumn,
        orgColumnName: renamedColumn,
        maxLength: "128",
        isIndex: true,
      })
    );
    states.afterExternalIndexEnable = queryState("afterExternalIndexEnable", renamedColumn);

    const externalIndexEnabled = states.afterExternalIndexEnable.metadata[0] || {};
    requests.fieldUniqueEnableWithExternal = await pageFormFetch(
      page,
      "/msService/ec/property/save",
      propertyPayload({
        version: externalIndexEnabled.version || "0",
        columnName: renamedColumn,
        orgColumnName: renamedColumn,
        maxLength: "128",
        isIndex: true,
        isUnique: true,
      })
    );
    states.afterExternalUniqueEnable = queryState("afterExternalUniqueEnable", renamedColumn);

    const externalUniqueEnabled = states.afterExternalUniqueEnable.metadata[0] || {};
    requests.fieldUniqueEnableWithExternalReplay = await pageFormFetch(
      page,
      "/msService/ec/property/save",
      propertyPayload({
        version: externalUniqueEnabled.version || "0",
        columnName: renamedColumn,
        orgColumnName: renamedColumn,
        maxLength: "128",
        isIndex: true,
        isUnique: true,
      })
    );
    states.afterExternalUniqueEnableReplay = queryState("afterExternalUniqueEnableReplay", renamedColumn);

    const externalUniqueReplayed = states.afterExternalUniqueEnableReplay.metadata[0] || {};
    requests.fieldUniqueDisableWithExternal = await pageFormFetch(
      page,
      "/msService/ec/property/save",
      propertyPayload({
        version: externalUniqueReplayed.version || "0",
        columnName: renamedColumn,
        orgColumnName: renamedColumn,
        maxLength: "128",
        isIndex: true,
        isUnique: false,
      })
    );
    states.afterExternalUniqueDisable = queryState("afterExternalUniqueDisable", renamedColumn);

    const externalUniqueDisabled = states.afterExternalUniqueDisable.metadata[0] || {};
    requests.fieldNotNullEnable = await pageFormFetch(
      page,
      "/msService/ec/property/save",
      propertyPayload({
        version: externalUniqueDisabled.version || "0",
        columnName: renamedColumn,
        orgColumnName: renamedColumn,
        maxLength: "128",
        nullable: false,
      })
    );
    states.afterNotNullEnable = queryState("afterNotNullEnable", renamedColumn);

    const notNullEnabled = states.afterNotNullEnable.metadata[0] || {};
    requests.fieldNotNullEnableReplay = await pageFormFetch(
      page,
      "/msService/ec/property/save",
      propertyPayload({
        version: notNullEnabled.version || "0",
        columnName: renamedColumn,
        orgColumnName: renamedColumn,
        maxLength: "128",
        nullable: false,
      })
    );
    states.afterNotNullEnableReplay = queryState("afterNotNullEnableReplay", renamedColumn);

    dbActions.nullInsertWhileNotNull = runSqlExpectFailure(
      `insert into public.${sqlIdentifier(tableName)} (id, valid) values (${nullProbeId}, 1);`
    );
    states.afterNullReject = queryState("afterNullReject", renamedColumn);

    const notNullReplayed = states.afterNullReject.metadata[0] || {};
    requests.fieldNullableEnable = await pageFormFetch(
      page,
      "/msService/ec/property/save",
      propertyPayload({
        version: notNullReplayed.version || "0",
        columnName: renamedColumn,
        orgColumnName: renamedColumn,
        maxLength: "128",
        nullable: true,
      })
    );
    states.afterNullableEnable = queryState("afterNullableEnable", renamedColumn);

    const nullableEnabled = states.afterNullableEnable.metadata[0] || {};
    requests.fieldNullableEnableReplay = await pageFormFetch(
      page,
      "/msService/ec/property/save",
      propertyPayload({
        version: nullableEnabled.version || "0",
        columnName: renamedColumn,
        orgColumnName: renamedColumn,
        maxLength: "128",
        nullable: true,
      })
    );
    states.afterNullableEnableReplay = queryState("afterNullableEnableReplay", renamedColumn);

    runSql(
      `insert into public.${sqlIdentifier(tableName)} (id, valid) values (${nullProbeId}, 1);`
    );
    states.afterNullFixtureInsert = queryState("afterNullFixtureInsert", renamedColumn);

    const nullableReplayed = states.afterNullFixtureInsert.metadata[0] || {};
    requests.unsafeNotNull = await pageFormFetch(
      page,
      "/msService/ec/property/save",
      propertyPayload({
        version: nullableReplayed.version || "0",
        columnName: renamedColumn,
        orgColumnName: renamedColumn,
        maxLength: "128",
        nullable: false,
      })
    );
    states.afterUnsafeNotNull = queryState("afterUnsafeNotNull", renamedColumn);

    runSql(`delete from public.${sqlIdentifier(tableName)} where id=${nullProbeId};`);
    states.afterNullFixtureDelete = queryState("afterNullFixtureDelete", renamedColumn);

    const afterNullCleanup = states.afterNullFixtureDelete.metadata[0] || {};
    requests.fieldIdempotentReplay = await pageFormFetch(
      page,
      "/msService/ec/property/save",
      propertyPayload({
        version: afterNullCleanup.version || "0",
        columnName: renamedColumn,
        orgColumnName: renamedColumn,
        maxLength: "128",
      })
    );
    states.afterReplay = queryState("afterReplay", renamedColumn);

    const replayed = states.afterReplay.metadata[0] || {};
    requests.unsafeTypeChange = await pageFormFetch(
      page,
      "/msService/ec/property/save",
      propertyPayload({
        version: replayed.version || "0",
        columnName: renamedColumn,
        orgColumnName: renamedColumn,
        type: "INTEGER",
        maxLength: "",
      })
    );
    const expectedFailureRequests = [requests.unsafeNotNull, requests.unsafeTypeChange].filter(
      (item) => item && item.responseStatus >= 400
    );
    if (expectedFailureRequests.length > 0) {
      browserEvidence.networkErrors.forEach((item) => {
        if (item.url.includes("/msService/ec/property/save") && item.status >= 400) {
          item.expected = true;
        }
      });
      browserEvidence.expectedConsoleErrors = browserEvidence.consoleErrors.filter((message) =>
        /Failed to load resource: the server responded with a status of [45]\d\d/i.test(message)
      );
    }
    states.afterUnsafeChange = queryState("afterUnsafeChange", renamedColumn);
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

  const createdColumn = states.afterCreate && columnByName(states.afterCreate, initialColumn);
  const renamedPhysical = states.afterRename && columnByName(states.afterRename, renamedColumn);
  const replayColumns = states.afterReplay ? states.afterReplay.columns : [];
  const initialManagedIndex = `IDX_${tableName}_${initialColumn}`;
  const renamedManagedIndex = `IDX_${tableName}_${renamedColumn}`;
  const initialManagedUnique = `UQ_${tableName}_${initialColumn}`;
  const renamedManagedUnique = `UQ_${tableName}_${renamedColumn}`;
  const externalIndexName = `EXT_${tableName}_${renamedColumn}`;
  const beforeUnsafeNotNullMetadata =
    states.afterNullFixtureInsert && states.afterNullFixtureInsert.metadata[0];
  const afterUnsafeNotNullMetadata = states.afterUnsafeNotNull && states.afterUnsafeNotNull.metadata[0];
  const afterUnsafeNotNullColumn =
    states.afterUnsafeNotNull && columnByName(states.afterUnsafeNotNull, renamedColumn);
  const afterUnsafeMetadata = states.afterUnsafeChange && states.afterUnsafeChange.metadata[0];
  const afterUnsafeColumn = states.afterUnsafeChange && columnByName(states.afterUnsafeChange, renamedColumn);
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
        `navigationAttempts=${browserEvidence.navigationAttempts}; ` +
        `transientNavigation=${browserEvidence.navigationTransientErrors.length}; ` +
        `unexpectedConsole=${unexpectedConsoleErrors.length}; expectedConsole=${expectedConsoleErrors.length}; ` +
        `page=${browserEvidence.pageErrors.length}; request=${browserEvidence.requestFailures.length}; ` +
        `unexpectedNetwork=${unexpectedNetworkErrors.length}`
    ),
    check(
      "create-requests",
      responseBusinessOk(requests.entityCreate) && responseBusinessOk(requests.modelCreate) &&
        responseBusinessOk(requests.fieldCreate),
      `entity=${requests.entityCreate && requests.entityCreate.responseStatus}; ` +
        `model=${requests.modelCreate && requests.modelCreate.responseStatus}; ` +
        `field=${requests.fieldCreate && requests.fieldCreate.responseStatus}`
    ),
    check(
      "field-metadata-created",
      states.afterCreate && states.afterCreate.metadata.length === 1 &&
        states.afterCreate.metadata[0].type === "TEXT" &&
        states.afterCreate.metadata[0].columnName.toLowerCase() === initialColumn.toLowerCase() &&
        storedBooleanEquals(metadataValue(states.afterCreate, "isIndex"), true) &&
        storedBooleanEquals(metadataValue(states.afterCreate, "isUnique"), false) &&
        storedBooleanEquals(metadataValue(states.afterCreate, "nullable"), true),
      `metadata=${JSON.stringify(states.afterCreate && states.afterCreate.metadata[0])}`
    ),
    check(
      "postgres-column-created-and-indexed",
      createdColumn && createdColumn.dataType === "character varying" && createdColumn.characterLength === "64" &&
        createdColumn.nullable === "YES" && states.afterCreate.indexes.length === 1 &&
        hasIndex(states.afterCreate, initialManagedIndex) && states.afterCreate.constraints.length === 0,
      `column=${JSON.stringify(createdColumn || null)}; indexes=${JSON.stringify(
        (states.afterCreate && states.afterCreate.indexes) || []
      )}`
    ),
    check(
      "marker-row-written",
      states.afterProbeInsert && hasProbe(states.afterProbeInsert),
      `probe=${JSON.stringify((states.afterProbeInsert && states.afterProbeInsert.probe) || [])}`
    ),
    check(
      "managed-index-disabled-without-column-loss",
      responseBusinessOk(requests.fieldIndexDisable) && states.afterIndexDisable &&
        storedBooleanEquals(metadataValue(states.afterIndexDisable, "isIndex"), false) &&
        states.afterIndexDisable.indexes.length === 0 && hasProbe(states.afterIndexDisable),
      `response=${requests.fieldIndexDisable && requests.fieldIndexDisable.responseStatus}; ` +
        `indexes=${JSON.stringify((states.afterIndexDisable && states.afterIndexDisable.indexes) || [])}`
    ),
    check(
      "managed-index-disable-idempotent",
      responseBusinessOk(requests.fieldIndexDisableReplay) && states.afterIndexDisableReplay &&
        storedBooleanEquals(metadataValue(states.afterIndexDisableReplay, "isIndex"), false) &&
        states.afterIndexDisableReplay.indexes.length === 0 && hasProbe(states.afterIndexDisableReplay),
      `response=${requests.fieldIndexDisableReplay && requests.fieldIndexDisableReplay.responseStatus}; ` +
        `indexes=${JSON.stringify((states.afterIndexDisableReplay && states.afterIndexDisableReplay.indexes) || [])}`
    ),
    check(
      "managed-index-reenabled",
      responseBusinessOk(requests.fieldIndexEnable) && states.afterIndexEnable &&
        storedBooleanEquals(metadataValue(states.afterIndexEnable, "isIndex"), true) &&
        states.afterIndexEnable.indexes.length === 1 && hasIndex(states.afterIndexEnable, initialManagedIndex) &&
        hasProbe(states.afterIndexEnable),
      `response=${requests.fieldIndexEnable && requests.fieldIndexEnable.responseStatus}; ` +
        `indexes=${JSON.stringify((states.afterIndexEnable && states.afterIndexEnable.indexes) || [])}`
    ),
    check(
      "managed-unique-enabled-and-replaces-ordinary-index",
      responseBusinessOk(requests.fieldUniqueEnable) && states.afterUniqueEnable &&
        storedBooleanEquals(metadataValue(states.afterUniqueEnable, "isUnique"), true) &&
        states.afterUniqueEnable.constraints.length === 1 && hasConstraint(states.afterUniqueEnable, initialManagedUnique) &&
        states.afterUniqueEnable.indexes.length === 1 && hasIndex(states.afterUniqueEnable, initialManagedUnique) &&
        !hasIndex(states.afterUniqueEnable, initialManagedIndex) && hasProbe(states.afterUniqueEnable),
      `response=${requests.fieldUniqueEnable && requests.fieldUniqueEnable.responseStatus}; ` +
        `constraints=${JSON.stringify((states.afterUniqueEnable && states.afterUniqueEnable.constraints) || [])}; ` +
        `indexes=${JSON.stringify((states.afterUniqueEnable && states.afterUniqueEnable.indexes) || [])}`
    ),
    check(
      "managed-unique-enable-idempotent",
      responseBusinessOk(requests.fieldUniqueEnableReplay) && states.afterUniqueEnableReplay &&
        states.afterUniqueEnableReplay.constraints.length === 1 &&
        hasConstraint(states.afterUniqueEnableReplay, initialManagedUnique) &&
        states.afterUniqueEnableReplay.indexes.length === 1 &&
        hasIndex(states.afterUniqueEnableReplay, initialManagedUnique),
      `response=${requests.fieldUniqueEnableReplay && requests.fieldUniqueEnableReplay.responseStatus}; ` +
        `constraints=${JSON.stringify((states.afterUniqueEnableReplay && states.afterUniqueEnableReplay.constraints) || [])}`
    ),
    check(
      "managed-unique-rejects-duplicate-row",
      dbActions.duplicateInsert && dbActions.duplicateInsert.rejected &&
        /duplicate key|unique constraint/i.test(dbActions.duplicateInsert.error) &&
        states.afterDuplicateReject && states.afterDuplicateReject.duplicateProbe.length === 0 &&
        hasProbe(states.afterDuplicateReject),
      `dbAction=${JSON.stringify(dbActions.duplicateInsert || null)}; ` +
        `duplicateProbe=${JSON.stringify((states.afterDuplicateReject && states.afterDuplicateReject.duplicateProbe) || [])}`
    ),
    check(
      "field-renamed-widened-and-data-preserved",
      responseBusinessOk(requests.fieldRenameAndWiden) && renamedPhysical &&
        renamedPhysical.dataType === "character varying" && renamedPhysical.characterLength === "256" &&
        renamedPhysical.nullable === "YES" && !columnByName(states.afterRename, initialColumn) &&
        hasProbe(states.afterRename),
      `response=${requests.fieldRenameAndWiden && requests.fieldRenameAndWiden.responseStatus}; ` +
        `column=${JSON.stringify(renamedPhysical || null)}; probe=${JSON.stringify(
          (states.afterRename && states.afterRename.probe) || []
        )}`
    ),
    check(
      "managed-unique-renamed-with-column",
      states.afterRename && states.afterRename.constraints.length === 1 &&
        hasConstraint(states.afterRename, renamedManagedUnique) && !hasConstraint(states.afterRename, initialManagedUnique) &&
        states.afterRename.indexes.length === 1 && hasIndex(states.afterRename, renamedManagedUnique) &&
        !hasIndex(states.afterRename, initialManagedUnique),
      `constraints=${JSON.stringify((states.afterRename && states.afterRename.constraints) || [])}; ` +
        `indexes=${JSON.stringify((states.afterRename && states.afterRename.indexes) || [])}`
    ),
    check(
      "managed-unique-disabled-and-ordinary-index-restored",
      responseBusinessOk(requests.fieldUniqueDisable) && states.afterUniqueDisable &&
        storedBooleanEquals(metadataValue(states.afterUniqueDisable, "isUnique"), false) &&
        states.afterUniqueDisable.constraints.length === 0 && states.afterUniqueDisable.indexes.length === 1 &&
        hasIndex(states.afterUniqueDisable, renamedManagedIndex) && !hasIndex(states.afterUniqueDisable, renamedManagedUnique),
      `response=${requests.fieldUniqueDisable && requests.fieldUniqueDisable.responseStatus}; ` +
        `constraints=${JSON.stringify((states.afterUniqueDisable && states.afterUniqueDisable.constraints) || [])}; ` +
        `indexes=${JSON.stringify((states.afterUniqueDisable && states.afterUniqueDisable.indexes) || [])}`
    ),
    check(
      "managed-unique-disable-idempotent",
      responseBusinessOk(requests.fieldUniqueDisableReplay) && states.afterUniqueDisableReplay &&
        states.afterUniqueDisableReplay.constraints.length === 0 &&
        states.afterUniqueDisableReplay.indexes.length === 1 &&
        hasIndex(states.afterUniqueDisableReplay, renamedManagedIndex),
      `response=${requests.fieldUniqueDisableReplay && requests.fieldUniqueDisableReplay.responseStatus}; ` +
        `indexes=${JSON.stringify((states.afterUniqueDisableReplay && states.afterUniqueDisableReplay.indexes) || [])}`
    ),
    check(
      "managed-index-renamed-with-column",
      responseBusinessOk(requests.fieldOrdinaryIndexRenameToInitial) &&
        responseBusinessOk(requests.fieldOrdinaryIndexRenameToFinal) &&
        states.afterOrdinaryIndexRenameToInitial && states.afterOrdinaryIndexRenameToFinal &&
        states.afterOrdinaryIndexRenameToInitial.indexes.length === 1 &&
        hasIndex(states.afterOrdinaryIndexRenameToInitial, initialManagedIndex) &&
        !hasIndex(states.afterOrdinaryIndexRenameToInitial, renamedManagedIndex) &&
        states.afterOrdinaryIndexRenameToFinal.indexes.length === 1 &&
        hasIndex(states.afterOrdinaryIndexRenameToFinal, renamedManagedIndex) &&
        !hasIndex(states.afterOrdinaryIndexRenameToFinal, initialManagedIndex) &&
        hasProbe(states.afterOrdinaryIndexRenameToInitial) && hasProbe(states.afterOrdinaryIndexRenameToFinal),
      `toInitial=${requests.fieldOrdinaryIndexRenameToInitial && requests.fieldOrdinaryIndexRenameToInitial.responseStatus}; ` +
        `toFinal=${requests.fieldOrdinaryIndexRenameToFinal && requests.fieldOrdinaryIndexRenameToFinal.responseStatus}; ` +
        `initialIndexes=${JSON.stringify((states.afterOrdinaryIndexRenameToInitial && states.afterOrdinaryIndexRenameToInitial.indexes) || [])}; ` +
        `finalIndexes=${JSON.stringify((states.afterOrdinaryIndexRenameToFinal && states.afterOrdinaryIndexRenameToFinal.indexes) || [])}`
    ),
    check(
      "external-unique-index-fixture-created",
      states.afterExternalIndexCreate && states.afterExternalIndexCreate.indexes.length === 2 &&
        hasIndex(states.afterExternalIndexCreate, renamedManagedIndex) &&
        hasIndex(states.afterExternalIndexCreate, externalIndexName) &&
        states.afterExternalIndexCreate.indexes.some(
          (row) => row.indexName.toLowerCase() === externalIndexName.toLowerCase() && row.isUnique === "true"
        ),
      `indexes=${JSON.stringify((states.afterExternalIndexCreate && states.afterExternalIndexCreate.indexes) || [])}`
    ),
    check(
      "external-unique-index-protected-on-managed-disable",
      responseBusinessOk(requests.fieldIndexDisableWithExternal) && states.afterExternalIndexDisable &&
        storedBooleanEquals(metadataValue(states.afterExternalIndexDisable, "isIndex"), false) &&
        states.afterExternalIndexDisable.indexes.length === 1 &&
        hasIndex(states.afterExternalIndexDisable, externalIndexName) &&
        !hasIndex(states.afterExternalIndexDisable, renamedManagedIndex) && hasProbe(states.afterExternalIndexDisable),
      `response=${requests.fieldIndexDisableWithExternal && requests.fieldIndexDisableWithExternal.responseStatus}; ` +
        `indexes=${JSON.stringify((states.afterExternalIndexDisable && states.afterExternalIndexDisable.indexes) || [])}`
    ),
    check(
      "external-index-satisfies-reenable-without-duplicate",
      responseBusinessOk(requests.fieldIndexEnableWithExternal) && states.afterExternalIndexEnable &&
        storedBooleanEquals(metadataValue(states.afterExternalIndexEnable, "isIndex"), true) &&
        states.afterExternalIndexEnable.indexes.length === 1 && hasIndex(states.afterExternalIndexEnable, externalIndexName) &&
        !hasIndex(states.afterExternalIndexEnable, renamedManagedIndex) && hasProbe(states.afterExternalIndexEnable),
      `response=${requests.fieldIndexEnableWithExternal && requests.fieldIndexEnableWithExternal.responseStatus}; ` +
        `indexes=${JSON.stringify((states.afterExternalIndexEnable && states.afterExternalIndexEnable.indexes) || [])}`
    ),
    check(
      "external-unique-index-satisfies-property-unique",
      responseBusinessOk(requests.fieldUniqueEnableWithExternal) && states.afterExternalUniqueEnable &&
        storedBooleanEquals(metadataValue(states.afterExternalUniqueEnable, "isUnique"), true) &&
        states.afterExternalUniqueEnable.constraints.length === 0 &&
        states.afterExternalUniqueEnable.indexes.length === 1 &&
        hasIndex(states.afterExternalUniqueEnable, externalIndexName) &&
        !hasIndex(states.afterExternalUniqueEnable, renamedManagedUnique),
      `response=${requests.fieldUniqueEnableWithExternal && requests.fieldUniqueEnableWithExternal.responseStatus}; ` +
        `constraints=${JSON.stringify((states.afterExternalUniqueEnable && states.afterExternalUniqueEnable.constraints) || [])}; ` +
        `indexes=${JSON.stringify((states.afterExternalUniqueEnable && states.afterExternalUniqueEnable.indexes) || [])}`
    ),
    check(
      "external-unique-enable-idempotent",
      responseBusinessOk(requests.fieldUniqueEnableWithExternalReplay) && states.afterExternalUniqueEnableReplay &&
        states.afterExternalUniqueEnableReplay.constraints.length === 0 &&
        states.afterExternalUniqueEnableReplay.indexes.length === 1 &&
        hasIndex(states.afterExternalUniqueEnableReplay, externalIndexName),
      `response=${requests.fieldUniqueEnableWithExternalReplay && requests.fieldUniqueEnableWithExternalReplay.responseStatus}; ` +
        `indexes=${JSON.stringify((states.afterExternalUniqueEnableReplay && states.afterExternalUniqueEnableReplay.indexes) || [])}`
    ),
    check(
      "external-unique-index-protected-on-property-disable",
      responseBusinessOk(requests.fieldUniqueDisableWithExternal) && states.afterExternalUniqueDisable &&
        storedBooleanEquals(metadataValue(states.afterExternalUniqueDisable, "isUnique"), false) &&
        states.afterExternalUniqueDisable.constraints.length === 0 &&
        states.afterExternalUniqueDisable.indexes.length === 1 &&
        hasIndex(states.afterExternalUniqueDisable, externalIndexName),
      `response=${requests.fieldUniqueDisableWithExternal && requests.fieldUniqueDisableWithExternal.responseStatus}; ` +
        `indexes=${JSON.stringify((states.afterExternalUniqueDisable && states.afterExternalUniqueDisable.indexes) || [])}`
    ),
    check(
      "not-null-enabled",
      responseBusinessOk(requests.fieldNotNullEnable) && states.afterNotNullEnable &&
        storedBooleanEquals(metadataValue(states.afterNotNullEnable, "nullable"), false) &&
        columnNullable(states.afterNotNullEnable, renamedColumn) === "NO" && hasProbe(states.afterNotNullEnable),
      `response=${requests.fieldNotNullEnable && requests.fieldNotNullEnable.responseStatus}; ` +
        `metadata=${JSON.stringify(states.afterNotNullEnable && states.afterNotNullEnable.metadata[0])}; ` +
        `column=${JSON.stringify(states.afterNotNullEnable && columnByName(states.afterNotNullEnable, renamedColumn))}`
    ),
    check(
      "not-null-enable-idempotent",
      responseBusinessOk(requests.fieldNotNullEnableReplay) && states.afterNotNullEnableReplay &&
        storedBooleanEquals(metadataValue(states.afterNotNullEnableReplay, "nullable"), false) &&
        columnNullable(states.afterNotNullEnableReplay, renamedColumn) === "NO",
      `response=${requests.fieldNotNullEnableReplay && requests.fieldNotNullEnableReplay.responseStatus}; ` +
        `column=${JSON.stringify(states.afterNotNullEnableReplay && columnByName(states.afterNotNullEnableReplay, renamedColumn))}`
    ),
    check(
      "not-null-rejects-null-row",
      dbActions.nullInsertWhileNotNull && dbActions.nullInsertWhileNotNull.rejected &&
        /null value|not-null constraint/i.test(dbActions.nullInsertWhileNotNull.error) &&
        states.afterNullReject && states.afterNullReject.nullProbe.length === 0 && hasProbe(states.afterNullReject),
      `dbAction=${JSON.stringify(dbActions.nullInsertWhileNotNull || null)}; ` +
        `nullProbe=${JSON.stringify((states.afterNullReject && states.afterNullReject.nullProbe) || [])}`
    ),
    check(
      "nullable-restored",
      responseBusinessOk(requests.fieldNullableEnable) && states.afterNullableEnable &&
        storedBooleanEquals(metadataValue(states.afterNullableEnable, "nullable"), true) &&
        columnNullable(states.afterNullableEnable, renamedColumn) === "YES",
      `response=${requests.fieldNullableEnable && requests.fieldNullableEnable.responseStatus}; ` +
        `column=${JSON.stringify(states.afterNullableEnable && columnByName(states.afterNullableEnable, renamedColumn))}`
    ),
    check(
      "nullable-restore-idempotent",
      responseBusinessOk(requests.fieldNullableEnableReplay) && states.afterNullableEnableReplay &&
        storedBooleanEquals(metadataValue(states.afterNullableEnableReplay, "nullable"), true) &&
        columnNullable(states.afterNullableEnableReplay, renamedColumn) === "YES",
      `response=${requests.fieldNullableEnableReplay && requests.fieldNullableEnableReplay.responseStatus}; ` +
        `column=${JSON.stringify(states.afterNullableEnableReplay && columnByName(states.afterNullableEnableReplay, renamedColumn))}`
    ),
    check(
      "null-fixture-written",
      states.afterNullFixtureInsert && states.afterNullFixtureInsert.nullProbe.length === 1,
      `nullProbe=${JSON.stringify((states.afterNullFixtureInsert && states.afterNullFixtureInsert.nullProbe) || [])}`
    ),
    check(
      "unsafe-not-null-change-rolled-back",
      requests.unsafeNotNull && !responseBusinessOk(requests.unsafeNotNull) && beforeUnsafeNotNullMetadata &&
        afterUnsafeNotNullMetadata && afterUnsafeNotNullColumn &&
        beforeUnsafeNotNullMetadata.version === afterUnsafeNotNullMetadata.version &&
        storedBooleanEquals(afterUnsafeNotNullMetadata.nullable, true) &&
        afterUnsafeNotNullColumn.nullable === "YES" && states.afterUnsafeNotNull.nullProbe.length === 1 &&
        hasProbe(states.afterUnsafeNotNull),
      `response=${requests.unsafeNotNull && requests.unsafeNotNull.responseStatus}; ` +
        `before=${JSON.stringify(beforeUnsafeNotNullMetadata || null)}; ` +
        `after=${JSON.stringify(afterUnsafeNotNullMetadata || null)}; ` +
        `column=${JSON.stringify(afterUnsafeNotNullColumn || null)}; ` +
        `nullProbe=${JSON.stringify((states.afterUnsafeNotNull && states.afterUnsafeNotNull.nullProbe) || [])}`
    ),
    check(
      "null-fixture-cleaned",
      states.afterNullFixtureDelete && states.afterNullFixtureDelete.nullProbe.length === 0 &&
        hasProbe(states.afterNullFixtureDelete),
      `nullProbe=${JSON.stringify((states.afterNullFixtureDelete && states.afterNullFixtureDelete.nullProbe) || [])}`
    ),
    check(
      "field-idempotent-replay",
      responseBusinessOk(requests.fieldIdempotentReplay) && states.afterReplay && replayColumns.length === 1 &&
        storedBooleanEquals(metadataValue(states.afterReplay, "isUnique"), false) &&
        storedBooleanEquals(metadataValue(states.afterReplay, "nullable"), true) &&
        states.afterReplay.indexes.length === 1 && hasIndex(states.afterReplay, externalIndexName) &&
        !hasIndex(states.afterReplay, renamedManagedIndex) && states.afterReplay.constraints.length === 0 &&
        hasProbe(states.afterReplay),
      `response=${requests.fieldIdempotentReplay && requests.fieldIdempotentReplay.responseStatus}; ` +
        `columns=${replayColumns.length}; indexes=${states.afterReplay ? states.afterReplay.indexes.length : 0}; ` +
        `probe=${JSON.stringify((states.afterReplay && states.afterReplay.probe) || [])}`
    ),
    check(
      "unsafe-type-change-rolled-back",
      requests.unsafeTypeChange && !responseBusinessOk(requests.unsafeTypeChange) && afterUnsafeMetadata &&
        afterUnsafeMetadata.type === "TEXT" &&
        afterUnsafeMetadata.columnName.toLowerCase() === renamedColumn.toLowerCase() && afterUnsafeColumn &&
        afterUnsafeColumn.dataType === "character varying" && afterUnsafeColumn.characterLength === "256" &&
        afterUnsafeColumn.nullable === "YES" && hasProbe(states.afterUnsafeChange),
      `response=${requests.unsafeTypeChange && requests.unsafeTypeChange.responseStatus}; ` +
        `metadata=${JSON.stringify(afterUnsafeMetadata || null)}; column=${JSON.stringify(afterUnsafeColumn || null)}; ` +
        `probe=${JSON.stringify((states.afterUnsafeChange && states.afterUnsafeChange.probe) || [])}`
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
    actionId: "entity-model-postgres-field-sync",
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
      propertyCode,
      tableName,
      initialColumn,
      renamedColumn,
      initialManagedIndex,
      renamedManagedIndex,
      initialManagedUnique,
      renamedManagedUnique,
      externalIndexName,
      probeId,
      duplicateProbeId,
      nullProbeId,
      probeValue,
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
    dbActions,
    states,
    cleanup,
    backendTrace: {
      endpoint: "PropertyController.save -> DtoUtils.getPropertyVO -> ModelServiceImpl.saveProperty",
      metadataPersistence: "propertyDao.merge -> public.ec_property",
      physicalPersistence:
        "FieldSyncDBUtils.fieldSyncToDb -> PostgresFieldSyncSupport.sync -> ALTER TABLE/COMMENT/CREATE INDEX/ALTER INDEX/DROP INDEX/ADD CONSTRAINT/RENAME CONSTRAINT/DROP CONSTRAINT/SET NOT NULL/DROP NOT NULL",
      transactionBoundary:
        "ModelServiceImpl.saveProperty propagates PostgreSQL DDL failures so ec_property and physical DDL roll back together",
      destructivePolicy:
        "No automatic DROP COLUMN. Only deterministic single-column managed indexes and unique constraints may be renamed or dropped; external equivalent indexes are preserved. Incompatible type and unsafe NOT NULL changes are rejected and rolled back.",
    },
    verificationSql: {
      metadata: metadataSql(),
      columns: columnSql(),
      indexes: indexSql(),
      constraints: constraintSql(),
      externalIndexFixture:
        `create unique index ${sqlIdentifier(externalIndexName)} on public.${sqlIdentifier(tableName)} ` +
        `(${sqlIdentifier(renamedColumn)});`,
      duplicateRejection:
        `insert into public.${sqlIdentifier(tableName)} (id, ${sqlIdentifier(initialColumn)}, valid) values (` +
        `${duplicateProbeId}, ${sqlLiteral(probeValue)}, 1);`,
      nullRejection:
        `insert into public.${sqlIdentifier(tableName)} (id, valid) values (${nullProbeId}, 1);`,
      markerRow: probeSql(renamedColumn),
      nullMarkerRow: nullProbeSql(renamedColumn),
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
