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
  const response = await page.goto(evidence.url, { waitUntil: "domcontentloaded", timeout: 45000 });
  evidence.navigationStatus = response ? response.status() : null;
  await page.waitForTimeout(1500);
  const bodyText = await page.locator("body").innerText({ timeout: 5000 }).catch(() => "");
  evidence.visibleError =
    bodyText
      .split(/\r?\n/)
      .map((line) => line.trim())
      .find((line) => line && visibleErrorPattern.test(line)) || null;
  return { browser, context, page, evidence };
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
    "property.isUnique": "false",
    "property.isHidden": "false",
    "property.nullable": "true",
    "property.multable": "false",
    "property.seniorSystemCode": "false",
    "property.sensitive": "false",
    "property.stretch": "false",
    "property.isBussinessKey": "false",
    "property.isUsedMneCode": "false",
    "property.isIndex": "true",
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
    "select i.relname::text, a.attname::text",
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

function probeSql(columnName) {
  return [
    `select id::text, coalesce(${sqlIdentifier(columnName)}::text,'')`,
    `from public.${sqlIdentifier(tableName)}`,
    `where id=${probeId};`,
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
    indexes: parseRows(runSql(indexSql()), ["indexName", "columnName"]),
    probe: probeColumn ? parseRows(runSql(probeSql(probeColumn)), ["id", "value"]) : [],
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
    requests.fieldRenameAndWiden = await pageFormFetch(
      page,
      "/msService/ec/property/save",
      propertyPayload({
        version: created.version || "0",
        columnName: renamedColumn,
        orgColumnName: initialColumn,
        maxLength: "128",
      })
    );
    states.afterRename = queryState("afterRename", renamedColumn);

    const renamed = states.afterRename.metadata[0] || {};
    requests.fieldIdempotentReplay = await pageFormFetch(
      page,
      "/msService/ec/property/save",
      propertyPayload({
        version: renamed.version || "0",
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
    browserEvidence.networkErrors.forEach((item) => {
      if (item.url.includes("/msService/ec/property/save") && item.status >= 400) {
        item.expected = true;
      }
    });
    if (requests.unsafeTypeChange && requests.unsafeTypeChange.responseStatus >= 400) {
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
      browserEvidence.navigationStatus &&
        browserEvidence.navigationStatus < 400 &&
        !browserEvidence.visibleError &&
        unexpectedConsoleErrors.length === 0 &&
        browserEvidence.pageErrors.length === 0 &&
        browserEvidence.requestFailures.length === 0 &&
        unexpectedNetworkErrors.length === 0,
      `status=${browserEvidence.navigationStatus}; visibleError=${browserEvidence.visibleError || "none"}; ` +
        `unexpectedConsole=${unexpectedConsoleErrors.length}; expectedConsole=${expectedConsoleErrors.length}; ` +
        `page=${browserEvidence.pageErrors.length}; ` +
        `request=${browserEvidence.requestFailures.length}; unexpectedNetwork=${unexpectedNetworkErrors.length}`
    ),
    check(
      "create-requests",
      responseBusinessOk(requests.entityCreate) &&
        responseBusinessOk(requests.modelCreate) &&
        responseBusinessOk(requests.fieldCreate),
      `entity=${requests.entityCreate && requests.entityCreate.responseStatus}; ` +
        `model=${requests.modelCreate && requests.modelCreate.responseStatus}; ` +
        `field=${requests.fieldCreate && requests.fieldCreate.responseStatus}`
    ),
    check(
      "field-metadata-created",
      states.afterCreate &&
        states.afterCreate.metadata.length === 1 &&
        states.afterCreate.metadata[0].type === "TEXT" &&
        states.afterCreate.metadata[0].columnName.toLowerCase() === initialColumn.toLowerCase(),
      `rows=${states.afterCreate ? states.afterCreate.metadata.length : 0}; ` +
        `type=${states.afterCreate && states.afterCreate.metadata[0] && states.afterCreate.metadata[0].type}; ` +
        `column=${states.afterCreate && states.afterCreate.metadata[0] && states.afterCreate.metadata[0].columnName}`
    ),
    check(
      "postgres-column-created-and-indexed",
      createdColumn &&
        createdColumn.dataType === "character varying" &&
        createdColumn.characterLength === "64" &&
        states.afterCreate.indexes.length >= 1,
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
      "field-renamed-widened-and-data-preserved",
      responseBusinessOk(requests.fieldRenameAndWiden) &&
        renamedPhysical &&
        renamedPhysical.dataType === "character varying" &&
        renamedPhysical.characterLength === "256" &&
        !columnByName(states.afterRename, initialColumn) &&
        hasProbe(states.afterRename),
      `response=${requests.fieldRenameAndWiden && requests.fieldRenameAndWiden.responseStatus}; ` +
        `column=${JSON.stringify(renamedPhysical || null)}; probe=${JSON.stringify(
          (states.afterRename && states.afterRename.probe) || []
        )}`
    ),
    check(
      "field-idempotent-replay",
      responseBusinessOk(requests.fieldIdempotentReplay) &&
        replayColumns.length === 1 &&
        states.afterReplay.indexes.length >= 1 &&
        hasProbe(states.afterReplay),
      `response=${requests.fieldIdempotentReplay && requests.fieldIdempotentReplay.responseStatus}; ` +
        `columns=${replayColumns.length}; indexes=${states.afterReplay ? states.afterReplay.indexes.length : 0}; ` +
        `probe=${JSON.stringify((states.afterReplay && states.afterReplay.probe) || [])}`
    ),
    check(
      "unsafe-type-change-rolled-back",
      requests.unsafeTypeChange &&
        !responseBusinessOk(requests.unsafeTypeChange) &&
        afterUnsafeMetadata &&
        afterUnsafeMetadata.type === "TEXT" &&
        afterUnsafeMetadata.columnName.toLowerCase() === renamedColumn.toLowerCase() &&
        afterUnsafeColumn &&
        afterUnsafeColumn.dataType === "character varying" &&
        afterUnsafeColumn.characterLength === "256" &&
        hasProbe(states.afterUnsafeChange),
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
  const report = {
    generatedAt: new Date().toISOString(),
    repoCommit: getRepoCommit(),
    database: "PostgreSQL",
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
      probeId,
      probeValue,
    },
    summary: {
      testedChecks: checks.length,
      pass,
      fail,
      blocked: fatalError ? 1 : 0,
      status: fail === 0 && !fatalError ? "PASS" : fatalError ? "BLOCKED" : "FAIL",
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
        "FieldSyncDBUtils.fieldSyncToDb -> PostgresFieldSyncSupport.sync -> ALTER TABLE/COMMENT/CREATE INDEX",
      transactionBoundary:
        "ModelServiceImpl.saveProperty propagates PostgreSQL DDL failures so ec_property and physical DDL roll back together",
      destructivePolicy:
        "No automatic DROP COLUMN. Incompatible type conversion is rejected and rolled back; this acceptance proves TEXT add, rename, widening and idempotent replay.",
    },
    verificationSql: {
      metadata: metadataSql(),
      columns: columnSql(),
      indexes: indexSql(),
      markerRow: probeSql(renamedColumn),
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
