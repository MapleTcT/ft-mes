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
  process.env.ADP_CUSTOM_PROPERTY_ACCEPTANCE_OUTPUT ||
  path.join("/tmp", `adp-custom-property-persistence-${stamp}.json`);
const dbSshTarget = process.env.ADP_DB_SSH_TARGET || "v6@100.99.133.43";
const dbContainer = process.env.ADP_DB_CONTAINER || "adp-mes-newbase-postgres-1";
const dbName = process.env.ADP_DB_NAME || "adp";
const dbUser = process.env.ADP_DB_USER || "adp";
const sshConnectTimeout = process.env.ADP_SSH_CONNECT_TIMEOUT || "20";
const headless = process.env.ADP_HEADLESS !== "false";
const apiTimeoutMs = Number(process.env.ADP_API_TIMEOUT_MS || "30000");
const dbQueryTimeoutMs = Number(process.env.ADP_DB_QUERY_TIMEOUT_MS || "30000");

const marker = process.env.ADP_E2E_MARKER || `ADP_E2E_${stamp}_CUSTOM_PROPERTY`;
const modelCode = process.env.ADP_CUSTOM_PROPERTY_MODEL_CODE || "TeamInfo_1.0.0_schedual_Schedule";
const propertyCode =
  process.env.ADP_CUSTOM_PROPERTY_PROPERTY_CODE || "TeamInfo_1.0.0_schedual_Schedule_bigintparama";
const pagePath = process.env.ADP_CUSTOM_PROPERTY_PAGE_PATH || "/supplant/#/customFieldModelManage";
const editedDisplayName = `${marker}.displayName`;
const editedDisplayNameValue = `${marker} display name`;
const editedDescription = `${marker} description`;

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

function shellQuote(value) {
  return `'${String(value).replace(/'/g, "'\\''")}'`;
}

function sqlLiteral(value) {
  return `'${String(value).replace(/'/g, "''")}'`;
}

function logStage(message) {
  console.error(`[custom-property] ${message}`);
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
  return execFileSync("ssh", ["-o", "BatchMode=yes", "-o", `ConnectTimeout=${sshConnectTimeout}`, dbSshTarget, remoteCommand], {
    encoding: "utf8",
    stdio: ["ignore", "pipe", "pipe"],
    timeout: dbQueryTimeoutMs,
  }).trim();
}

function parseRows(output, columns) {
  if (!output) {
    return [];
  }
  return output.split(/\r?\n/).filter(Boolean).map((line) => {
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

async function readJsonSafe(response) {
  const text = await response.text();
  try {
    return { json: JSON.parse(text), text, status: response.status(), ok: response.ok() };
  } catch (_error) {
    return { json: null, text, status: response.status(), ok: response.ok() };
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

function isBusinessOk(result) {
  if (!result || !result.ok || visibleErrorPattern.test(result.text || "")) {
    return false;
  }
  if (result.json && Object.prototype.hasOwnProperty.call(result.json, "code")) {
    return ["0", "200", "100000000"].includes(String(result.json.code));
  }
  return true;
}

function mappingSql() {
  return [
    "select coalesce(id::text,''), coalesce(model_code,''), coalesce(property_code,''),",
    "coalesce(enable_custom::text,''), coalesce(nullable::text,''), coalesce(field_type,''),",
    "coalesce(format,''), coalesce(display_name,''), coalesce(description,''), coalesce(sort::text,'')",
    "from public.base_cp_model_mapping",
    `where property_code=${sqlLiteral(propertyCode)} and model_code=${sqlLiteral(modelCode)}`,
    "order by id desc;",
  ].join(" ");
}

function propertySql(tableName) {
  return [
    "select coalesce(code,''), coalesce(model_code,''), coalesce(proj_custom_in_use::text,''),",
    "coalesce(is_used_for_list::text,''), coalesce(is_used_for_search::text,''), coalesce(proj_flag::text,''),",
    "coalesce(nullable::text,''), coalesce(field_type,''), coalesce(format,''),",
    "coalesce(display_name,''), coalesce(description,'')",
    `from public.${tableName}`,
    `where code=${sqlLiteral(propertyCode)};`,
  ].join(" ");
}

function viewMappingSql() {
  return [
    "select coalesce(id::text,''), coalesce(property_code,''), coalesce(associated_code,''),",
    "coalesce(property_layrec,''), coalesce(show_custom::text,''), coalesce(nullable::text,''),",
    "coalesce(field_type,''), coalesce(format,''), coalesce(display_name,''), coalesce(sort::text,'')",
    "from public.base_cp_view_mapping",
    `where property_code=${sqlLiteral(propertyCode)}`,
    "order by id desc;",
  ].join(" ");
}

function cleanupSql(originalDisplayName, originalDescription) {
  return [
    "begin;",
    "delete from public.base_cp_model_mapping",
    `where property_code=${sqlLiteral(propertyCode)} and model_code=${sqlLiteral(modelCode)};`,
    "update public.runtime_property",
    "set proj_custom_in_use=false, proj_flag=false, is_used_for_list=false, is_used_for_search=false,",
    `sort=null, display_name=${sqlLiteral(originalDisplayName)}, description=${
      originalDescription ? sqlLiteral(originalDescription) : "null"
    },`,
    "modify_time=current_timestamp",
    `where code=${sqlLiteral(propertyCode)};`,
    "commit;",
  ].join(" ");
}

function queryState() {
  const modelMapping = parseRows(runSql(mappingSql()), [
    "id",
    "modelCode",
    "propertyCode",
    "enableCustom",
    "nullable",
    "fieldType",
    "format",
    "displayName",
    "description",
    "sort",
  ]);
  const runtimeProperty = parseRows(runSql(propertySql("runtime_property")), [
    "code",
    "modelCode",
    "projCustomInUse",
    "isUsedForList",
    "isUsedForSearch",
    "projFlag",
    "nullable",
    "fieldType",
    "format",
    "displayName",
    "description",
  ]);
  const projectProperty = parseRows(runSql(propertySql("project_property")), [
    "code",
    "modelCode",
    "projCustomInUse",
    "isUsedForList",
    "isUsedForSearch",
    "projFlag",
    "nullable",
    "fieldType",
    "format",
    "displayName",
    "description",
  ]);
  const viewMapping = parseRows(runSql(viewMappingSql()), [
    "id",
    "propertyCode",
    "associatedCode",
    "propertyLayRec",
    "showCustom",
    "nullable",
    "fieldType",
    "format",
    "displayName",
    "sort",
  ]);
  return {
    sql: {
      modelMapping: mappingSql(),
      runtimeProperty: propertySql("runtime_property"),
      projectProperty: propertySql("project_property"),
      viewMapping: viewMappingSql(),
    },
    modelMapping,
    runtimeProperty,
    projectProperty,
    viewMapping,
  };
}

async function browserApi(page, method, urlPath, payload) {
  return page.evaluate(
    async ({ method: methodArg, urlPath: urlPathArg, payload: payloadArg, timeoutMsArg }) => {
      const token =
        window.localStorage.getItem("suposTicket") ||
        window.localStorage.getItem("SUPOS_TICKET") ||
        window.localStorage.getItem("token") ||
        window.sessionStorage.getItem("suposTicket") ||
        window.sessionStorage.getItem("SUPOS_TICKET") ||
        window.sessionStorage.getItem("token") ||
        "";
      const controller = new AbortController();
      const timer = window.setTimeout(() => controller.abort(), timeoutMsArg);
      try {
        const response = await window.fetch(urlPathArg, {
          method: methodArg,
          credentials: "include",
          signal: controller.signal,
          headers: {
            Accept: "application/json, text/plain, */*",
            "Content-Type": "application/json;charset=UTF-8",
            Authorization: token ? `Bearer ${token}` : "",
            langu_code: "zh_CN",
          },
          body: payloadArg ? JSON.stringify(payloadArg) : undefined,
        });
        const text = await response.text();
        let json = null;
        try {
          json = JSON.parse(text);
        } catch (_error) {
          json = null;
        }
        return {
          ok: response.ok,
          status: response.status,
          url: response.url,
          method: methodArg,
          requestPayload: payloadArg || null,
          text,
          bodySnippet: text.slice(0, 1000),
          json,
        };
      } catch (error) {
        const text = `${error && error.name ? error.name : "Error"}: ${error && error.message ? error.message : String(error)}`;
        return {
          ok: false,
          status: 0,
          url: urlPathArg,
          method: methodArg,
          requestPayload: payloadArg || null,
          text,
          bodySnippet: text.slice(0, 1000),
          json: null,
          error: text,
        };
      } finally {
        window.clearTimeout(timer);
      }
    },
    { method, urlPath, payload, timeoutMsArg: apiTimeoutMs }
  );
}

function modelList(result) {
  return (result && result.json && result.json.list) || [];
}

function cloneForSave(row, overrides) {
  return {
    associatedPropertyCode: row.associatedPropertyCode || null,
    associatedType: row.associatedType || null,
    relatedKey: row.relatedKey || null,
    precision: row.precision == null ? row.precision1 || null : row.precision,
    multable: Boolean(row.multable),
    sort: row.sort == null || row.sort === "" ? null : row.sort,
    description: row.description || null,
    displayName: row.displayName,
    enableCustom: Boolean(row.enableCustom),
    fieldType: row.fieldType,
    format: row.format,
    fillContent: row.fillContent || null,
    nullable: Boolean(row.nullable),
    refViewCode: row.refViewCode || null,
    property: row.property,
    ...overrides,
  };
}

function textOrEmpty(value) {
  return value == null ? "" : String(value);
}

function rowHasMarker(row) {
  return Object.values(row || {}).some((value) => textOrEmpty(value).includes(marker));
}

async function openRuntimePage(ticket) {
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
    ["suposTicket", "SUPOS_TICKET", "token", "ticket"].forEach((key) => {
      window.localStorage.setItem(key, token);
      window.sessionStorage.setItem(key, token);
    });
    window.localStorage.setItem("language", "zh_CN");
    window.localStorage.setItem("langu_code", "zh_CN");
    window.localStorage.setItem("locale", "zh-cn");
  }, ticket);
  const page = await context.newPage();
  const networkErrors = [];
  const consoleErrors = [];
  const pageErrors = [];
  const requestFailures = [];
  const apiCalls = [];
  page.on("console", (message) => {
    if (message.type() === "error") {
      consoleErrors.push(message.text());
    }
  });
  page.on("pageerror", (error) => pageErrors.push(error.message));
  page.on("requestfailed", (requestObject) => {
    requestFailures.push({
      url: requestObject.url(),
      method: requestObject.method(),
      failure: requestObject.failure() && requestObject.failure().errorText,
    });
  });
  page.on("response", async (response) => {
    const type = response.request().resourceType();
    if (!["document", "xhr", "fetch", "script", "stylesheet"].includes(type)) {
      return;
    }
    if (/\/inter-api\/customProperty\//.test(response.url())) {
      apiCalls.push({
        method: response.request().method(),
        url: response.url(),
        status: response.status(),
      });
    }
    const contentType = response.headers()["content-type"] || "";
    const htmlAssetFallback =
      ["script", "stylesheet"].includes(type) && response.status() < 400 && /text\/html/i.test(contentType);
    if (response.status() < 400 && !htmlAssetFallback) {
      return;
    }
    let body = "";
    try {
      body = (await response.text()).slice(0, 500);
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

  const url = `${browserBaseUrl}${pagePath}`;
  const response = await page.goto(url, { waitUntil: "domcontentloaded", timeout: 90000 });
  await page.evaluate((token) => {
    window.localStorage.setItem("suposTicket", token);
    window.localStorage.setItem("SUPOS_TICKET", token);
    window.localStorage.setItem("token", token);
  }, ticket);
  await page.waitForTimeout(2000);
  const bodyText = await page.locator("body").innerText({ timeout: 10000 }).catch(() => "");
  const visibleError = bodyText
    .split(/\r?\n/)
    .map((line) => line.trim())
    .find((line) => line && visibleErrorPattern.test(line));
  return {
    browser,
    context,
    page,
    pageEvidence: {
      url,
      navigationStatus: response ? response.status() : null,
      visibleError: visibleError || null,
      networkErrors,
      consoleErrors,
      pageErrors,
      requestFailures,
      apiCalls,
    },
  };
}

async function run() {
  ensureDir(outputPath);
  logStage("login");
  const api = await request.newContext({ ignoreHTTPSErrors: true });
  const ticket = await login(api);
  await api.dispose();

  logStage("query before state");
  const beforeState = queryState();
  logStage("open runtime page");
  const { browser, context, page, pageEvidence } = await openRuntimePage(ticket);
  const operations = {};

  try {
    logStage("modelManage/list before");
    const listPath = `/inter-api/customProperty/modelManage/list?modelCode=${encodeURIComponent(modelCode)}`;
    const listBefore = await browserApi(page, "GET", listPath, null);
    operations.listBefore = listBefore;
    if (!isBusinessOk(listBefore)) {
      throw new Error(`modelManage/list before failed: ${listBefore.status} ${listBefore.bodySnippet}`);
    }
    const sourceRow = modelList(listBefore).find((row) => row.property && row.property.code === propertyCode);
    if (!sourceRow) {
      throw new Error(`property ${propertyCode} not found in modelManage/list for ${modelCode}`);
    }
    const originalDisplayName = textOrEmpty(sourceRow.displayName);
    const originalDisplayNameInternational = textOrEmpty(sourceRow.displayNameInternational || sourceRow.displayName);
    const originalDescription = textOrEmpty(sourceRow.description);

    logStage("enable mapping");
    const enablePayload = {
      codes: sourceRow.id ? [] : [propertyCode],
      ids: sourceRow.id ? [sourceRow.id] : [],
      enabled: true,
    };
    operations.enable = await browserApi(
      page,
      "POST",
      "/inter-api/customProperty/modelManage/batcheUpdateEnabledStatus",
      enablePayload
    );
    if (!isBusinessOk(operations.enable)) {
      throw new Error(`enable custom property failed: ${operations.enable.status} ${operations.enable.bodySnippet}`);
    }

    logStage("query after enable");
    const afterEnableState = queryState();
    logStage("modelManage/list after enable");
    const listAfterEnable = await browserApi(page, "GET", listPath, null);
    operations.listAfterEnable = listAfterEnable;
    if (!isBusinessOk(listAfterEnable)) {
      throw new Error(`modelManage/list after enable failed: ${listAfterEnable.status} ${listAfterEnable.bodySnippet}`);
    }
    const enabledRow = modelList(listAfterEnable).find((row) => row.property && row.property.code === propertyCode);
    if (!enabledRow || !enabledRow.enableCustom) {
      throw new Error(`property ${propertyCode} did not become enableCustom=true in list response`);
    }

    logStage("edit marker display metadata");
    const editPayload = cloneForSave(enabledRow, {
      displayName: editedDisplayName,
      displayNameInternational: editedDisplayNameValue,
      description: editedDescription,
      enableCustom: true,
      nullable: true,
      fieldType: enabledRow.fieldType || "TEXTFIELD",
      format: enabledRow.format || "TEXT",
    });
    operations.edit = await browserApi(page, "POST", "/inter-api/customProperty/modelManage/save", editPayload);
    if (!isBusinessOk(operations.edit)) {
      throw new Error(`modelManage/save failed: ${operations.edit.status} ${operations.edit.bodySnippet}`);
    }
    logStage("query after marker edit");
    const afterEditState = queryState();

    logStage("modelManage/list after marker edit");
    const listAfterEdit = await browserApi(page, "GET", listPath, null);
    operations.listAfterEdit = listAfterEdit;
    if (!isBusinessOk(listAfterEdit)) {
      throw new Error(`modelManage/list after edit failed: ${listAfterEdit.status} ${listAfterEdit.bodySnippet}`);
    }
    const editedRow = modelList(listAfterEdit).find((row) => row.property && row.property.code === propertyCode);
    if (!editedRow || textOrEmpty(editedRow.displayName) !== editedDisplayName) {
      throw new Error(`property ${propertyCode} did not expose the edited displayName in list response`);
    }

    logStage("restore original display metadata");
    const restorePayload = cloneForSave(editedRow, {
      displayName: originalDisplayName,
      displayNameInternational: originalDisplayNameInternational,
      description: originalDescription,
      enableCustom: true,
      nullable: Boolean(sourceRow.nullable),
      fieldType: sourceRow.fieldType || enabledRow.fieldType || "TEXTFIELD",
      format: sourceRow.format || enabledRow.format || "TEXT",
    });
    operations.restoreOriginalDisplay = await browserApi(
      page,
      "POST",
      "/inter-api/customProperty/modelManage/save",
      restorePayload
    );
    if (!isBusinessOk(operations.restoreOriginalDisplay)) {
      throw new Error(
        `modelManage/save restore failed: ${operations.restoreOriginalDisplay.status} ${operations.restoreOriginalDisplay.bodySnippet}`
      );
    }
    logStage("query after display restore");
    const afterRestoreState = queryState();

    logStage("disable mapping rollback");
    const disablePayload = {
      codes: [],
      ids: afterRestoreState.modelMapping[0] && afterRestoreState.modelMapping[0].id ? [Number(afterRestoreState.modelMapping[0].id)] : [],
      enabled: false,
    };
    operations.disable = await browserApi(
      page,
      "POST",
      "/inter-api/customProperty/modelManage/batcheUpdateEnabledStatus",
      disablePayload
    );
    if (!isBusinessOk(operations.disable)) {
      throw new Error(`disable custom property failed: ${operations.disable.status} ${operations.disable.bodySnippet}`);
    }
    logStage("query after disable");
    const afterDisableState = queryState();
    let cleanupPerformed = false;
    let afterCleanupState = afterDisableState;
    const cleanupStatement = cleanupSql(originalDisplayName, originalDescription);
    if (beforeState.modelMapping.length === 0) {
      logStage("cleanup test-created mapping");
      runSql(cleanupStatement);
      cleanupPerformed = true;
      afterCleanupState = queryState();
    }

    logStage("customProperty tree final readiness");
    const finalReadiness = await browserApi(page, "GET", "/inter-api/customProperty/tree?type=module", null);
    operations.finalReadiness = finalReadiness;
    if (!isBusinessOk(finalReadiness)) {
      throw new Error(`customProperty tree final readiness failed: ${finalReadiness.status} ${finalReadiness.bodySnippet}`);
    }

    const modelCreated =
      beforeState.modelMapping.length === 0 &&
      afterEnableState.modelMapping.some((row) => row.propertyCode === propertyCode && row.enableCustom === "true");
    const modelEdited = afterEditState.modelMapping.some(
      (row) => row.propertyCode === propertyCode && row.displayName === editedDisplayName && row.description === editedDescription
    );
    const runtimeSynced = afterEditState.runtimeProperty.some(
      (row) =>
        row.code === propertyCode &&
        row.projCustomInUse === "true" &&
        row.isUsedForList === "true" &&
        row.isUsedForSearch === "true" &&
        row.displayName === editedDisplayName &&
        row.description === editedDescription
    );
    const projectSynced = afterEditState.projectProperty.some(
      (row) =>
        row.code === propertyCode &&
        row.projCustomInUse === "true" &&
        row.isUsedForList === "true" &&
        row.isUsedForSearch === "true" &&
        row.displayName === editedDisplayName &&
        row.description === editedDescription
    );
    const modelRestored = afterRestoreState.modelMapping.some(
      (row) =>
        row.propertyCode === propertyCode &&
        row.displayName === originalDisplayName &&
        row.description === originalDescription &&
        !rowHasMarker(row)
    );
    const runtimeRestored = afterRestoreState.runtimeProperty.some(
      (row) =>
        row.code === propertyCode &&
        row.displayName === originalDisplayName &&
        row.description === originalDescription &&
        !rowHasMarker(row)
    );
    const projectRestored = afterRestoreState.projectProperty.some(
      (row) =>
        row.code === propertyCode &&
        row.displayName === originalDisplayName &&
        row.description === originalDescription &&
        !rowHasMarker(row)
    );
    const disabled = afterDisableState.modelMapping.some(
      (row) => row.propertyCode === propertyCode && row.enableCustom === "false" && row.sort === ""
    );
    const runtimeDisabled = afterDisableState.runtimeProperty.some(
      (row) =>
        row.code === propertyCode &&
        row.projCustomInUse === "false" &&
        row.isUsedForList === "false" &&
        row.isUsedForSearch === "false" &&
        row.projFlag === "false"
    );
    const projectDisabled = afterDisableState.projectProperty.some(
      (row) =>
        row.code === propertyCode &&
        row.projCustomInUse === "false" &&
        row.isUsedForList === "false" &&
        row.isUsedForSearch === "false" &&
        row.projFlag === "false"
    );
    const cleanupClean =
      !cleanupPerformed ||
      (afterCleanupState.modelMapping.length === 0 &&
        afterCleanupState.runtimeProperty.some(
          (row) =>
            row.code === propertyCode &&
            row.projCustomInUse === "false" &&
            row.isUsedForList === "false" &&
            row.isUsedForSearch === "false" &&
            row.displayName === originalDisplayName &&
            row.description === originalDescription
        ) &&
        afterCleanupState.projectProperty.some(
          (row) =>
            row.code === propertyCode &&
            row.projCustomInUse === "false" &&
            row.isUsedForList === "false" &&
            row.isUsedForSearch === "false" &&
            row.displayName === originalDisplayName &&
            row.description === originalDescription
        ));

    const issues = [];
    if (!modelCreated) issues.push("base_cp_model_mapping row was not created/enabled from the browser API operation");
    if (!modelEdited) issues.push("base_cp_model_mapping did not persist the ADP_E2E displayName/description edit");
    if (!runtimeSynced) issues.push("runtime_property did not persist the enabled/edit marker state");
    if (!projectSynced) issues.push("project_property did not persist the enabled/edit marker state");
    if (!modelRestored) issues.push("base_cp_model_mapping did not restore the original display metadata before rollback");
    if (!runtimeRestored) issues.push("runtime_property did not restore the original display metadata before rollback");
    if (!projectRestored) issues.push("project_property did not restore the original display metadata before rollback");
    if (!disabled) issues.push("base_cp_model_mapping was not disabled during rollback");
    if (!runtimeDisabled) issues.push("runtime_property was not disabled during rollback");
    if (!projectDisabled) issues.push("project_property was not disabled during rollback");
    if (!cleanupClean) issues.push("controlled SQL cleanup did not restore the pre-test custom-property state");
    if (pageEvidence.visibleError) issues.push(`visible page error: ${pageEvidence.visibleError}`);
    if (pageEvidence.networkErrors.length > 0) issues.push("browser network errors were observed");
    if (pageEvidence.consoleErrors.length > 0) issues.push("browser console errors were observed");
    if (pageEvidence.pageErrors.length > 0) issues.push("browser page errors were observed");
    if (pageEvidence.requestFailures.length > 0) issues.push("browser request failures were observed");

    const report = {
      schemaVersion: 1,
      generatedAt: new Date().toISOString(),
      repoCommit: getRepoCommit(),
      database: "PostgreSQL",
      module: "基础配置-低代码自定义字段配置",
      route: pagePath,
      status: issues.length === 0 ? "PASS" : "FAIL",
      baseUrl,
      browserBaseUrl,
      marker,
      target: {
        modelCode,
        propertyCode,
        pagePath,
      },
      operations,
      backendTrace: {
        modelList: {
          controller:
            "custom-property-webapi ModelManageController.modelManageList GET /inter-api/customProperty/modelManage/list",
          service: "custom-property-service ModelServiceFoundationImpl.findCustomPropertyModelMappings",
          mapper: "PropertyMapper + CustomPropertyModelMappingMapper",
          sqlOrOrm: "MyBatis-Plus selectList on runtime_property and base_cp_model_mapping",
        },
        enableDisable: {
          controller:
            "custom-property-webapi ModelManageController.batcheUpdateEnabledStatus POST /inter-api/customProperty/modelManage/batcheUpdateEnabledStatus",
          service: "custom-property-service ModelServiceFoundationImpl.enableProperty",
          mapper:
            "CustomPropertyModelMappingMapper + PropertyMapper + PropertyProjectMapper + CustomPropertyViewMappingMapper",
          sqlOrOrm:
            "MyBatis-Plus saveOrUpdate on base_cp_model_mapping, runtime_property and project_property; optional base_cp_view_mapping sync for non-nullable/sysbase fields",
        },
        edit: {
          controller:
            "custom-property-webapi ModelManageController.modelManageSave POST /inter-api/customProperty/modelManage/save",
          service: "custom-property-service ModelServiceFoundationImpl.saveCustomPropertyModelMapping",
          mapper:
            "CustomPropertyModelMappingMapper + PropertyMapper + PropertyProjectMapper + CustomPropertyViewMappingMapper",
          sqlOrOrm:
            "MyBatis-Plus saveOrUpdate on base_cp_model_mapping, runtime_property and project_property; updateViewData for dependent view mapping",
        },
      },
      databaseEvidence: {
        before: beforeState,
        afterEnable: afterEnableState,
        afterEdit: afterEditState,
        afterRestore: afterRestoreState,
        afterDisable: afterDisableState,
        afterCleanup: afterCleanupState,
        verificationSql: {
          modelMapping: mappingSql(),
          runtimeProperty: propertySql("runtime_property"),
          projectProperty: propertySql("project_property"),
          viewMapping: viewMappingSql(),
          cleanup: cleanupStatement,
        },
      },
      checks: [
        { name: "browser-page-open", status: pageEvidence.navigationStatus === 200 && !pageEvidence.visibleError ? "PASS" : "FAIL" },
        { name: "model-mapping-created", status: modelCreated ? "PASS" : "FAIL" },
        { name: "model-mapping-edited", status: modelEdited ? "PASS" : "FAIL" },
        { name: "runtime-property-synced", status: runtimeSynced ? "PASS" : "FAIL" },
        { name: "project-property-synced", status: projectSynced ? "PASS" : "FAIL" },
        { name: "model-mapping-display-restored", status: modelRestored ? "PASS" : "FAIL" },
        { name: "runtime-property-display-restored", status: runtimeRestored ? "PASS" : "FAIL" },
        { name: "project-property-display-restored", status: projectRestored ? "PASS" : "FAIL" },
        { name: "model-mapping-disabled", status: disabled ? "PASS" : "FAIL" },
        { name: "runtime-property-disabled", status: runtimeDisabled ? "PASS" : "FAIL" },
        { name: "project-property-disabled", status: projectDisabled ? "PASS" : "FAIL" },
        { name: "controlled-cleanup", status: cleanupClean ? "PASS" : "FAIL" },
        { name: "final-readiness", status: isBusinessOk(finalReadiness) ? "PASS" : "FAIL" },
      ],
      browser: pageEvidence,
      issues,
      evidence:
        "Browser-context marker acceptance for low-code custom property mapping. It creates/enables one existing custom field mapping, edits display metadata with an ADP_E2E marker, verifies PostgreSQL before/after rows, restores the original display metadata, then disables the marker mapping again.",
      limitations: [
        "This validates custom-property model mapping and runtime_property/project_property synchronization, not full ec_entity/ec_model creation.",
        "A controlled SQL cleanup removes the test-created mapping when the pre-test state had no base_cp_model_mapping row; the business persistence evidence remains in afterEnable/afterEdit/afterDisable.",
        "Entity/model create/edit/delete actions remain PLANNED until real configuration pages create or remove dedicated ADP_E2E entity/model metadata.",
      ],
    };

    logStage("write report");
    fs.writeFileSync(outputPath, JSON.stringify(report, null, 2) + "\n");
    if (issues.length > 0) {
      console.error(`FAIL custom-property acceptance issues=${issues.length}`);
      console.error(`REPORT ${outputPath}`);
      process.exitCode = 1;
      return;
    }
    console.log(`PASS custom-property acceptance marker=${marker}`);
    console.log(`REPORT ${outputPath}`);
  } finally {
    await context.close().catch((error) => logStage(`context close warning: ${error.message}`));
    await browser.close().catch((error) => logStage(`browser close warning: ${error.message}`));
  }
}

run().catch((error) => {
  console.error(error && error.stack ? error.stack : String(error));
  process.exitCode = 1;
});
