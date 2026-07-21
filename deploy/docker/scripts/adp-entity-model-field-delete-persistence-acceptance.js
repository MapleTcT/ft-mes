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
  process.env.ADP_ENTITY_MODEL_FIELD_DELETE_OUTPUT ||
  path.join("/tmp", `adp-entity-model-field-delete-persistence-${stamp}.json`);
const dbSshTarget = process.env.ADP_DB_SSH_TARGET || "v6@10.11.100.17";
const dbContainer = process.env.ADP_DB_CONTAINER || "adp-mes-newbase-postgres-1";
const dbName = process.env.ADP_DB_NAME || "adp";
const dbUser = process.env.ADP_DB_USER || "adp";
const sshConnectTimeout = process.env.ADP_SSH_CONNECT_TIMEOUT || "20";
const dbQueryTimeoutMs = Number(process.env.ADP_DB_QUERY_TIMEOUT_MS || "30000");
const headless = process.env.ADP_HEADLESS !== "false";

const marker = process.env.ADP_E2E_MARKER || `ADP_E2E_${stamp}_PG_FIELD_DELETE`;
const moduleCode = process.env.ADP_ENTITY_MODEL_MODULE_CODE || "DataSet_1.0.0";
const entityName = `E2eDelEnt${suffix}`;
const modelName = `E2eDelMod${suffix}`;
const entityCode = `${moduleCode}_${entityName}`;
const modelCode = `${entityCode}_${modelName}`;
const tableName = `DS_E2D_${suffix}`;
const probeId = Number(`${Date.now()}`.slice(-14));
const pagePath = "/msService/ec/engine/msManage";
const entityConfigPath = `/msService/ec/entity/config?entity.code=${encodeURIComponent(entityCode)}`;
const modelManagePath = `/msService/ec/model/manage?entity.code=${encodeURIComponent(entityCode)}`;
const deleteWarning = "删除字段会永久删除对应物理列及该字段的全部已有数据，且无法撤销。确认继续？";
const dependencyView = `V_E2D_${suffix}`;
const rollbackFunction = `FN_E2D_${suffix}`;
const rollbackTrigger = `TR_E2D_${suffix}`;

function field(key, name, columnName, type = "TEXT", fieldType = "TEXTFIELD", format = "TEXT") {
  return {
    key,
    name,
    columnName,
    type,
    fieldType,
    format,
    propertyCode: `${modelCode}_${name}`,
  };
}

const fields = {
  softHard: field("softHard", `softHard${suffix}`, `DEL_MAIN_${suffix}`),
  dependency: field("dependency", `dependency${suffix}`, `DEL_DEP_${suffix}`),
  rollback: field("rollback", `rollback${suffix}`, `DEL_RB_${suffix}`),
  drift: field("drift", `drift${suffix}`, `DEL_DRF_${suffix}`),
  attachment: field(
    "attachment",
    `attachment${suffix}`,
    `DEL_ATT_${suffix}`,
    "PROPERTYATTACHMENT",
    "PROPERTYATTACHMENT",
    "SELECTCOMP"
  ),
};
const fieldList = Object.values(fields);
const inherentIdCode = `${modelCode}_id`;
const driftedColumn = `${fields.drift.columnName}_X`;
const visibleErrorPattern =
  /(数据库操作异常|系统错误|系统异常|SQLGrammarException|could not extract ResultSet|column .* does not exist|relation .* does not exist|500 INTERNAL|org\.hibernate\.[\w.]+Exception|org\.springframework\.[\w.]+Exception|java\.lang\.[\w.]+Exception)/i;

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
  if (!result || result.responseStatus >= 400 || visibleErrorPattern.test(result.text || "")) {
    return false;
  }
  if (result.json && Object.prototype.hasOwnProperty.call(result.json, "success")) {
    return result.json.success === true || String(result.json.success) === "true";
  }
  return /"success"\s*:\s*true|success=true/i.test(result.text || "");
}

function responseBusinessRejected(result) {
  if (!result) {
    return false;
  }
  if (result.responseStatus >= 400) {
    return true;
  }
  return Boolean(
    result.json && Object.prototype.hasOwnProperty.call(result.json, "success") &&
      !(result.json.success === true || String(result.json.success) === "true")
  );
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
    engineUrl: `${browserBaseUrl}${pagePath}`,
    engineNavigationStatus: null,
    entityConfigUrl: `${browserBaseUrl}${entityConfigPath}`,
    entityConfigNavigationStatus: null,
    modelManageUrl: `${browserBaseUrl}${modelManagePath}`,
    modelManageNavigationStatus: null,
    deleteWarningVisible: false,
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
    const requestItem = response.request();
    let propertyCode = "";
    try {
      propertyCode = new URLSearchParams(requestItem.postData() || "").get("property.code") || "";
    } catch (_error) {
      propertyCode = "";
    }
    evidence.networkErrors.push({
      status: response.status(),
      method: requestItem.method(),
      url: response.url(),
      propertyCode,
      expected: false,
    });
  });
  const response = await page.goto(evidence.engineUrl, { waitUntil: "domcontentloaded", timeout: 45000 });
  evidence.engineNavigationStatus = response ? response.status() : null;
  await page.waitForTimeout(1000);
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
    "entity.prefix": "E2D",
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

function propertyPayload(item) {
  return {
    "property.version": "0",
    "property.sort": "1",
    "property.model.code": modelCode,
    "model.enableDataAudit": "false",
    "property.entityCode": entityCode,
    "property.moduleCode": moduleCode,
    "property.code": "",
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
    "property.orgColumnName": "",
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
    "property.type": item.type,
    "property.fieldType": item.fieldType,
    "property.format": item.format,
    "property.maxLength": item.type === "TEXT" ? "64" : "",
    "property.fetchMode": "SELECT",
    "property.description": `${marker}_${item.key}`,
  };
}

function deletePayload(propertyCode) {
  return { "property.code": propertyCode, "property.version": "0" };
}

function metadataSql() {
  const codes = [...fieldList.map((item) => item.propertyCode), inherentIdCode];
  return [
    "select code::text, coalesce(valid::text,''), coalesce(is_inherent::text,''),",
    "coalesce(is_pk::text,''), coalesce(type::text,''), coalesce(column_name::text,''),",
    "coalesce(version::text,'0'), coalesce(description::text,'')",
    "from public.ec_property",
    `where code in (${codes.map(sqlLiteral).join(",")})`,
    "order by code;",
  ].join(" ");
}

function queryState(label) {
  const tableExists = Number(
    runSql(
      "select count(*) from information_schema.tables where table_schema='public' " +
        `and lower(table_name)=lower(${sqlLiteral(tableName)});`
    ) || 0
  );
  const columns = tableExists
    ? parseRows(
        runSql(
          "select column_name::text, data_type::text from information_schema.columns " +
            "where table_schema='public' " +
            `and lower(table_name)=lower(${sqlLiteral(tableName)}) order by ordinal_position;`
        ),
        ["columnName", "dataType"]
      )
    : [];
  const rowJsonText = tableExists
    ? runSql(
        `select to_jsonb(t)::text from public.${sqlIdentifier(tableName)} t where id=${probeId};`
      )
    : "";
  return {
    label,
    tableExists,
    metadata: parseRows(runSql(metadataSql()), [
      "code",
      "valid",
      "isInherent",
      "isPk",
      "type",
      "columnName",
      "version",
      "description",
    ]),
    columns,
    rowJson: rowJsonText ? JSON.parse(rowJsonText) : null,
    dependencyViewCount: Number(
      runSql(
        "select count(*) from information_schema.views where table_schema='public' " +
          `and lower(table_name)=lower(${sqlLiteral(dependencyView)});`
      ) || 0
    ),
    rollbackTriggerCount: Number(
      runSql(
        "select count(*) from pg_trigger where not tgisinternal " +
          `and lower(tgname)=lower(${sqlLiteral(rollbackTrigger)});`
      ) || 0
    ),
  };
}

function cleanupSql() {
  return [
    "begin;",
    `drop view if exists public.${sqlIdentifier(dependencyView)};`,
    `drop function if exists public.${sqlIdentifier(rollbackFunction)}() cascade;`,
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

function cleanupCounts() {
  const output = runSql(
    [
      "select",
      `(select count(*) from public.ec_property where model_code=${sqlLiteral(modelCode)}),`,
      `(select count(*) from public.ec_model where code=${sqlLiteral(modelCode)}),`,
      `(select count(*) from public.ec_entity where code=${sqlLiteral(entityCode)}),`,
      "(select count(*) from information_schema.tables where table_schema='public'",
      `and lower(table_name)=lower(${sqlLiteral(tableName)})),`,
      "(select count(*) from information_schema.views where table_schema='public'",
      `and lower(table_name)=lower(${sqlLiteral(dependencyView)})),`,
      "(select count(*) from pg_proc p join pg_namespace n on n.oid=p.pronamespace",
      `where n.nspname='public' and lower(p.proname)=lower(${sqlLiteral(rollbackFunction)}));`,
    ].join(" ")
  );
  const values = output.split("|");
  return {
    property: Number(values[0] || 0),
    model: Number(values[1] || 0),
    entity: Number(values[2] || 0),
    physicalTable: Number(values[3] || 0),
    dependencyView: Number(values[4] || 0),
    rollbackFunction: Number(values[5] || 0),
  };
}

function propertyState(state, propertyCode) {
  return state.metadata.find((row) => row.code === propertyCode) || null;
}

function hasColumn(state, columnName) {
  return state.columns.some((row) => row.columnName.toLowerCase() === columnName.toLowerCase());
}

function storedTrue(value) {
  return ["1", "true", "t", "yes", "y"].includes(String(value).trim().toLowerCase());
}

function rowValue(state, columnName) {
  if (!state.rowJson) {
    return undefined;
  }
  return state.rowJson[columnName.toLowerCase()];
}

function check(name, passed, evidence) {
  return { name, status: passed ? "PASS" : "FAIL", evidence };
}

async function main() {
  ensureDir(outputPath);
  runSql(cleanupSql());
  const before = cleanupCounts();
  const api = await request.newContext({ ignoreHTTPSErrors: true });
  const ticket = await login(api);
  await api.dispose();
  const { browser, context, page, evidence: browserEvidence } = await openPage(ticket);
  const requests = {};
  const states = { before };
  let cleanup = null;
  let fatalError = null;
  try {
    requests.entityCreate = await pageFormFetch(page, "/msService/ec/entity/save", entityPayload());
    requests.modelCreate = await pageFormFetch(page, "/msService/ec/model/save", modelPayload());
    requests.propertyCreate = {};
    for (const item of fieldList) {
      requests.propertyCreate[item.key] = await pageFormFetch(
        page,
        "/msService/ec/property/save",
        propertyPayload(item)
      );
    }

    const entityConfigResponse = await page.goto(`${browserBaseUrl}${entityConfigPath}`, {
      waitUntil: "domcontentloaded",
      timeout: 45000,
    });
    browserEvidence.entityConfigNavigationStatus = entityConfigResponse ? entityConfigResponse.status() : null;
    await page.waitForTimeout(1000);
    const modelMenu = page.locator("#mainMenu .menu_item").nth(1);
    const [modelManageResponse] = await Promise.all([
      page.waitForResponse(
        (response) => response.url().includes("/msService/ec/model/manage"),
        { timeout: 30000 }
      ),
      modelMenu.click(),
    ]);
    browserEvidence.modelManageNavigationStatus = modelManageResponse.status();
    await page.locator("#ec_property_manage_del_div").waitFor({ state: "attached", timeout: 30000 });
    const modelManageText = await page
      .locator("#ec_property_manage_del_div")
      .textContent({ timeout: 5000 })
      .catch(() => "");
    browserEvidence.deleteWarningVisible = modelManageText.replace(/\s+/g, "").includes(
      deleteWarning.replace(/\s+/g, "")
    );

    runSql(
      [
        `insert into public.${sqlIdentifier(tableName)} (id, valid,`,
        fieldList
          .filter((item) => item.type !== "PROPERTYATTACHMENT")
          .map((item) => sqlIdentifier(item.columnName))
          .join(","),
        ") values (",
        `${probeId}, 1,`,
        fieldList
          .filter((item) => item.type !== "PROPERTYATTACHMENT")
          .map((item) => sqlLiteral(`${marker}_${item.key}`))
          .join(","),
        ");",
      ].join(" ")
    );
    states.afterCreate = queryState("afterCreate");

    requests.softDelete = await pageFormFetch(
      page,
      "/msService/ec/property/ordinaryDelete",
      deletePayload(fields.softHard.propertyCode)
    );
    states.afterSoftDelete = queryState("afterSoftDelete");

    requests.hardDeleteAfterSoft = await pageFormFetch(
      page,
      "/msService/ec/property/delete",
      deletePayload(fields.softHard.propertyCode)
    );
    states.afterHardDelete = queryState("afterHardDelete");

    runSql(
      `create view public.${sqlIdentifier(dependencyView)} as select id, ` +
        `${sqlIdentifier(fields.dependency.columnName)} from public.${sqlIdentifier(tableName)};`
    );
    requests.dependencyBlockedDelete = await pageFormFetch(
      page,
      "/msService/ec/property/delete",
      deletePayload(fields.dependency.propertyCode)
    );
    states.afterDependencyBlocked = queryState("afterDependencyBlocked");
    runSql(`drop view public.${sqlIdentifier(dependencyView)};`);
    requests.dependencyRetryDelete = await pageFormFetch(
      page,
      "/msService/ec/property/delete",
      deletePayload(fields.dependency.propertyCode)
    );
    states.afterDependencyRetry = queryState("afterDependencyRetry");

    runSql(
      [
        `create function public.${sqlIdentifier(rollbackFunction)}() returns trigger language plpgsql as $$`,
        "begin",
        `if old.code=${sqlLiteral(fields.rollback.propertyCode)} then`,
        `raise exception 'marker metadata delete blocked: %', old.code;`,
        "end if;",
        "return old;",
        "end;",
        "$$;",
        `create trigger ${sqlIdentifier(rollbackTrigger)} before delete on public.ec_property`,
        `for each row execute function public.${sqlIdentifier(rollbackFunction)}();`,
      ].join(" ")
    );
    requests.transactionRollbackDelete = await pageFormFetch(
      page,
      "/msService/ec/property/delete",
      deletePayload(fields.rollback.propertyCode)
    );
    states.afterTransactionRollback = queryState("afterTransactionRollback");
    runSql(
      `drop trigger ${sqlIdentifier(rollbackTrigger)} on public.ec_property; ` +
        `drop function public.${sqlIdentifier(rollbackFunction)}();`
    );
    requests.transactionRetryDelete = await pageFormFetch(
      page,
      "/msService/ec/property/delete",
      deletePayload(fields.rollback.propertyCode)
    );
    states.afterTransactionRetry = queryState("afterTransactionRetry");

    runSql(
      `alter table public.${sqlIdentifier(tableName)} rename column ` +
        `${sqlIdentifier(fields.drift.columnName)} to ${sqlIdentifier(driftedColumn)};`
    );
    requests.driftBlockedDelete = await pageFormFetch(
      page,
      "/msService/ec/property/delete",
      deletePayload(fields.drift.propertyCode)
    );
    states.afterDriftBlocked = queryState("afterDriftBlocked");
    runSql(
      `alter table public.${sqlIdentifier(tableName)} rename column ` +
        `${sqlIdentifier(driftedColumn)} to ${sqlIdentifier(fields.drift.columnName)};`
    );
    requests.driftRetryDelete = await pageFormFetch(
      page,
      "/msService/ec/property/delete",
      deletePayload(fields.drift.propertyCode)
    );
    states.afterDriftRetry = queryState("afterDriftRetry");

    requests.attachmentDelete = await pageFormFetch(
      page,
      "/msService/ec/property/delete",
      deletePayload(fields.attachment.propertyCode)
    );
    requests.inherentDelete = await pageFormFetch(
      page,
      "/msService/ec/property/delete",
      deletePayload(inherentIdCode)
    );
    states.final = queryState("final");
  } catch (error) {
    fatalError = error && error.stack ? error.stack : String(error);
  } finally {
    await context.close().catch(() => {});
    await browser.close().catch(() => {});
    try {
      runSql(cleanupSql());
      cleanup = cleanupCounts();
    } catch (error) {
      fatalError = fatalError || (error && error.stack ? error.stack : String(error));
    }
  }

  const afterCreate = states.afterCreate || { metadata: [], columns: [], rowJson: null };
  const afterSoft = states.afterSoftDelete || { metadata: [], columns: [], rowJson: null };
  const afterHard = states.afterHardDelete || { metadata: [], columns: [], rowJson: null };
  const afterDependencyBlocked = states.afterDependencyBlocked || { metadata: [], columns: [], rowJson: null };
  const afterDependencyRetry = states.afterDependencyRetry || { metadata: [], columns: [], rowJson: null };
  const afterRollback = states.afterTransactionRollback || { metadata: [], columns: [], rowJson: null };
  const afterRollbackRetry = states.afterTransactionRetry || { metadata: [], columns: [], rowJson: null };
  const afterDriftBlocked = states.afterDriftBlocked || { metadata: [], columns: [], rowJson: null };
  const afterDriftRetry = states.afterDriftRetry || { metadata: [], columns: [], rowJson: null };
  const finalState = states.final || { metadata: [], columns: [], rowJson: null };
  const unexpectedNetworkErrors = browserEvidence.networkErrors.filter((item) => !item.expected);
  const checks = [
    check(
      "marker-clean-before-test",
      Object.values(before).every((value) => value === 0),
      `before=${JSON.stringify(before)}`
    ),
    check(
      "real-entity-model-page-loaded",
      browserEvidence.engineNavigationStatus === 200 && browserEvidence.entityConfigNavigationStatus === 200 &&
        browserEvidence.modelManageNavigationStatus === 200 &&
        !browserEvidence.visibleError,
      `engine=${browserEvidence.engineNavigationStatus}; entityConfig=${browserEvidence.entityConfigNavigationStatus}; ` +
        `modelManage=${browserEvidence.modelManageNavigationStatus}; ` +
        `visibleError=${browserEvidence.visibleError}`
    ),
    check(
      "irreversible-delete-warning-visible",
      browserEvidence.deleteWarningVisible,
      `warning=${deleteWarning}`
    ),
    check(
      "entity-model-and-five-properties-created",
      responseBusinessOk(requests.entityCreate) && responseBusinessOk(requests.modelCreate) &&
        fieldList.every((item) => responseBusinessOk(requests.propertyCreate && requests.propertyCreate[item.key])) &&
        fieldList.every((item) => propertyState(afterCreate, item.propertyCode)) &&
        fieldList.filter((item) => item.type !== "PROPERTYATTACHMENT").every((item) => hasColumn(afterCreate, item.columnName)) &&
        !hasColumn(afterCreate, fields.attachment.columnName),
      `metadata=${JSON.stringify(afterCreate.metadata)}; columns=${JSON.stringify(afterCreate.columns)}`
    ),
    check(
      "marker-row-round-trip",
      fieldList.filter((item) => item.type !== "PROPERTYATTACHMENT").every(
        (item) => rowValue(afterCreate, item.columnName) === `${marker}_${item.key}`
      ),
      `row=${JSON.stringify(afterCreate.rowJson)}`
    ),
    check(
      "ordinary-delete-succeeds",
      responseBusinessOk(requests.softDelete),
      `response=${requests.softDelete && requests.softDelete.responseStatus}; body=${requests.softDelete && requests.softDelete.text}`
    ),
    check(
      "ordinary-delete-preserves-column-and-data",
      propertyState(afterSoft, fields.softHard.propertyCode) &&
        !storedTrue(propertyState(afterSoft, fields.softHard.propertyCode).valid) &&
        hasColumn(afterSoft, fields.softHard.columnName) &&
        rowValue(afterSoft, fields.softHard.columnName) === `${marker}_softHard`,
      `metadata=${JSON.stringify(propertyState(afterSoft, fields.softHard.propertyCode))}; ` +
        `row=${JSON.stringify(afterSoft.rowJson)}`
    ),
    check(
      "explicit-delete-removes-metadata-column-and-data",
      responseBusinessOk(requests.hardDeleteAfterSoft) &&
        !propertyState(afterHard, fields.softHard.propertyCode) &&
        !hasColumn(afterHard, fields.softHard.columnName) &&
        !Object.prototype.hasOwnProperty.call(afterHard.rowJson || {}, fields.softHard.columnName.toLowerCase()),
      `response=${requests.hardDeleteAfterSoft && requests.hardDeleteAfterSoft.responseStatus}; ` +
        `metadata=${JSON.stringify(afterHard.metadata)}; row=${JSON.stringify(afterHard.rowJson)}`
    ),
    check(
      "database-dependency-blocks-restrict-delete",
      requests.dependencyBlockedDelete && requests.dependencyBlockedDelete.responseStatus === 200 &&
        responseBusinessRejected(requests.dependencyBlockedDelete) &&
        /数据库视图或其他对象引用/.test(requests.dependencyBlockedDelete.text || "") &&
        propertyState(afterDependencyBlocked, fields.dependency.propertyCode) &&
        hasColumn(afterDependencyBlocked, fields.dependency.columnName) &&
        rowValue(afterDependencyBlocked, fields.dependency.columnName) === `${marker}_dependency` &&
        afterDependencyBlocked.dependencyViewCount === 1,
      `response=${requests.dependencyBlockedDelete && requests.dependencyBlockedDelete.responseStatus}; ` +
        `state=${JSON.stringify(afterDependencyBlocked)}`
    ),
    check(
      "database-dependency-retry-deletes-after-explicit-release",
      responseBusinessOk(requests.dependencyRetryDelete) &&
        !propertyState(afterDependencyRetry, fields.dependency.propertyCode) &&
        !hasColumn(afterDependencyRetry, fields.dependency.columnName) &&
        afterDependencyRetry.dependencyViewCount === 0,
      `response=${requests.dependencyRetryDelete && requests.dependencyRetryDelete.responseStatus}; ` +
        `state=${JSON.stringify(afterDependencyRetry)}`
    ),
    check(
      "metadata-delete-failure-rolls-back-ddl-and-data",
      requests.transactionRollbackDelete && requests.transactionRollbackDelete.responseStatus === 200 &&
        responseBusinessRejected(requests.transactionRollbackDelete) &&
        /数据库变更已回滚/.test(requests.transactionRollbackDelete.text || "") &&
        propertyState(afterRollback, fields.rollback.propertyCode) &&
        hasColumn(afterRollback, fields.rollback.columnName) &&
        rowValue(afterRollback, fields.rollback.columnName) === `${marker}_rollback` &&
        afterRollback.rollbackTriggerCount === 1,
      `response=${requests.transactionRollbackDelete && requests.transactionRollbackDelete.responseStatus}; ` +
        `state=${JSON.stringify(afterRollback)}`
    ),
    check(
      "transaction-retry-deletes-after-blocker-release",
      responseBusinessOk(requests.transactionRetryDelete) &&
        !propertyState(afterRollbackRetry, fields.rollback.propertyCode) &&
        !hasColumn(afterRollbackRetry, fields.rollback.columnName) &&
        afterRollbackRetry.rollbackTriggerCount === 0,
      `response=${requests.transactionRetryDelete && requests.transactionRetryDelete.responseStatus}; ` +
        `state=${JSON.stringify(afterRollbackRetry)}`
    ),
    check(
      "physical-column-drift-fails-closed",
      requests.driftBlockedDelete && requests.driftBlockedDelete.responseStatus === 200 &&
        responseBusinessRejected(requests.driftBlockedDelete) &&
        /物理结构不一致/.test(requests.driftBlockedDelete.text || "") &&
        propertyState(afterDriftBlocked, fields.drift.propertyCode) &&
        !hasColumn(afterDriftBlocked, fields.drift.columnName) && hasColumn(afterDriftBlocked, driftedColumn) &&
        rowValue(afterDriftBlocked, driftedColumn) === `${marker}_drift`,
      `response=${requests.driftBlockedDelete && requests.driftBlockedDelete.responseStatus}; ` +
        `state=${JSON.stringify(afterDriftBlocked)}`
    ),
    check(
      "physical-column-drift-repair-allows-delete",
      responseBusinessOk(requests.driftRetryDelete) &&
        !propertyState(afterDriftRetry, fields.drift.propertyCode) &&
        !hasColumn(afterDriftRetry, fields.drift.columnName) && !hasColumn(afterDriftRetry, driftedColumn),
      `response=${requests.driftRetryDelete && requests.driftRetryDelete.responseStatus}; ` +
        `state=${JSON.stringify(afterDriftRetry)}`
    ),
    check(
      "attachment-metadata-delete-without-physical-column",
      responseBusinessOk(requests.attachmentDelete) &&
        !propertyState(finalState, fields.attachment.propertyCode) &&
        !hasColumn(finalState, fields.attachment.columnName),
      `response=${requests.attachmentDelete && requests.attachmentDelete.responseStatus}; ` +
        `metadata=${JSON.stringify(finalState.metadata)}`
    ),
    check(
      "inherent-primary-key-delete-rejected",
      responseBusinessRejected(requests.inherentDelete) && propertyState(finalState, inherentIdCode) &&
        storedTrue(propertyState(finalState, inherentIdCode).isInherent) &&
        storedTrue(propertyState(finalState, inherentIdCode).isPk) && hasColumn(finalState, "id") &&
        finalState.rowJson && String(finalState.rowJson.id) === String(probeId),
      `response=${requests.inherentDelete && requests.inherentDelete.responseStatus}; ` +
        `metadata=${JSON.stringify(propertyState(finalState, inherentIdCode))}; row=${JSON.stringify(finalState.rowJson)}`
    ),
    check(
      "browser-has-no-unexpected-errors",
      unexpectedNetworkErrors.length === 0 && browserEvidence.pageErrors.length === 0 &&
        browserEvidence.requestFailures.length === 0 && browserEvidence.consoleErrors.length === 0,
      `network=${JSON.stringify(browserEvidence.networkErrors)}; pageErrors=${JSON.stringify(
        browserEvidence.pageErrors
      )}; requestFailures=${JSON.stringify(browserEvidence.requestFailures)}`
    ),
    check(
      "controlled-cleanup",
      cleanup && Object.values(cleanup).every((value) => value === 0),
      `cleanup=${JSON.stringify(cleanup)}`
    ),
  ];
  const pass = checks.filter((item) => item.status === "PASS").length;
  const fail = checks.filter((item) => item.status === "FAIL").length;
  const status = fail === 0 && !fatalError ? "PASS" : fatalError ? "BLOCKED" : "FAIL";
  const report = {
    schemaVersion: 1,
    generatedAt: new Date().toISOString(),
    repoCommit: getRepoCommit(),
    database: "PostgreSQL",
    module: "basic-config",
    actionId: "entity-model-postgres-field-delete",
    areaId: "configuration-physical-model-field",
    goalId: "G-012",
    status,
    marker,
    route: pagePath,
    apiEndpoints: [
      "POST /msService/ec/entity/save",
      "POST /msService/ec/model/save",
      "POST /msService/ec/property/save",
      "POST /msService/ec/property/ordinaryDelete",
      "POST /msService/ec/property/delete",
    ],
    summary: { testedChecks: checks.length, pass, fail, blocked: fatalError ? 1 : 0, status },
    identifiers: {
      entityCode,
      modelCode,
      tableName,
      probeId,
      inherentIdCode,
      dependencyView,
      rollbackFunction,
      rollbackTrigger,
      fields,
    },
    checks,
    browser: browserEvidence,
    requests,
    states,
    cleanup,
    backendTrace: {
      softDelete:
        "PropertyController.ordinaryDelete -> ModelServiceImpl.deleteProperty -> persisted ec_property valid=false",
      explicitDelete:
        "PropertyController.delete -> ModelServiceImpl.deletePropertyPhysical(deleteType=false, ignoreCheck=false) -> FieldSyncDBUtils.deleteFieldFromDb -> PostgresFieldSyncSupport.delete -> ALTER TABLE DROP COLUMN RESTRICT -> propertyDao.deletePhysical",
      transactionBoundary:
        "PostgreSQL DROP COLUMN and ec_property physical delete share ModelServiceImpl's Spring transaction; a marker BEFORE DELETE trigger proves DDL and row data are restored on metadata failure",
      destructivePolicy:
        "No automatic column deletion on property save, ordinaryDelete, model bulk delete or SQL-model reconciliation. Only the explicit property delete endpoint drops a PostgreSQL column, never uses CASCADE, rejects inherent/primary-key fields and fails closed on missing tables, missing columns or external dependencies.",
    },
    verificationSql: {
      metadata: metadataSql(),
      markerRow: `select to_jsonb(t)::text from public.${sqlIdentifier(tableName)} t where id=${probeId};`,
      dependencyFixture:
        `create view public.${sqlIdentifier(dependencyView)} as select id, ` +
        `${sqlIdentifier(fields.dependency.columnName)} from public.${sqlIdentifier(tableName)};`,
      transactionRollbackFixture:
        `create trigger ${sqlIdentifier(rollbackTrigger)} before delete on public.ec_property for each row ` +
        `execute function public.${sqlIdentifier(rollbackFunction)}();`,
      driftFixture:
        `alter table public.${sqlIdentifier(tableName)} rename column ` +
        `${sqlIdentifier(fields.drift.columnName)} to ${sqlIdentifier(driftedColumn)};`,
      cleanup: cleanupSql(),
    },
    fatalError,
  };
  fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`, "utf8");
  console.log(JSON.stringify(report.summary));
  console.log(`report=${outputPath}`);
  if (status !== "PASS") {
    process.exitCode = 1;
  }
}

main().catch((error) => {
  console.error(error && error.stack ? error.stack : error);
  process.exitCode = 1;
});
