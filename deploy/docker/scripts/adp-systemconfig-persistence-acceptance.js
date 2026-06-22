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
const outputDir = process.env.ADP_OUTPUT_DIR || path.join("/tmp", `adp-systemconfig-persistence-${stamp}`);
const outputPath =
  process.env.ADP_SYSTEMCONFIG_PERSISTENCE_OUTPUT ||
  path.join(outputDir, "systemconfig-persistence-results.json");

const dbSshTarget = process.env.ADP_DB_SSH_TARGET || "v6@100.99.133.43";
const dbContainer = process.env.ADP_DB_CONTAINER || "adp-mes-newbase-postgres-1";
const dbName = process.env.ADP_DB_NAME || "adp";
const dbUser = process.env.ADP_DB_USER || "adp";

const marker = process.env.ADP_E2E_MARKER || `ADP_E2E_${stamp}_SCFG`;
const appCode = process.env.ADP_SYSTEMCONFIG_APP_CODE || `${marker}_APP`;
const catalogCode = process.env.ADP_SYSTEMCONFIG_CATALOG_CODE || `${marker}_CAT`;
const configCode = process.env.ADP_SYSTEMCONFIG_CONFIG_CODE || `${marker}_KEY`;
const catalogName = `${marker} catalog`;
const configName = `${marker} config`;
const defaultValue = `${marker}_DEFAULT`;
const updatedValue = `${marker}_UPDATED`;
const tenantId = process.env.ADP_TENANT_ID || "dt";
const tidModuleKey = `${tenantId}/${appCode}/${catalogCode}`;

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

function queryCatalog() {
  const sql = [
    "select id, coalesce(parent_id::text,''), coalesce(sort::text,''),",
    "code, name, app_code, coalesce(catalog_type::text,''), coalesce(has_hide::text,''),",
    "coalesce(modify_time::text,'')",
    "from public.systemconfig_config_catalog",
    `where app_code = ${sqlLiteral(appCode)} and code = ${sqlLiteral(catalogCode)}`,
    "order by create_time desc nulls last, id desc;",
  ].join(" ");
  return {
    sql,
    rows: parseTableRows(runSql(sql), [
      "id",
      "parentId",
      "sort",
      "code",
      "name",
      "appCode",
      "catalogType",
      "hasHide",
      "modifyTime",
    ]),
  };
}

function queryConfig() {
  const sql = [
    "select id, coalesce(catalog_id::text,''), coalesce(sort::text,''), code, name, app_code,",
    "coalesce(module_code,''), coalesce(widget_type::text,''), coalesce(default_value,''),",
    "coalesce(widget_value,''), coalesce(max_value::text,''), coalesce(min_value::text,''),",
    "coalesce(reg_format,''), coalesce(reg_message,''), coalesce(has_require::text,''),",
    "coalesce(custom,''), coalesce(description,''), coalesce(modify_time::text,'')",
    "from public.systemconfig_config_info",
    `where app_code = ${sqlLiteral(appCode)} and code = ${sqlLiteral(configCode)}`,
    "order by create_time desc nulls last, id desc;",
  ].join(" ");
  return {
    sql,
    rows: parseTableRows(runSql(sql), [
      "id",
      "catalogId",
      "sort",
      "code",
      "name",
      "appCode",
      "moduleCode",
      "widgetType",
      "defaultValue",
      "widgetValue",
      "maxValue",
      "minValue",
      "regFormat",
      "regMessage",
      "hasRequire",
      "custom",
      "description",
      "modifyTime",
    ]),
  };
}

function queryVersion() {
  const sql = [
    "select id, tid_module_key, config_version, coalesce(modify_time::text,'')",
    "from public.systemconfig_config_version",
    `where tid_module_key = ${sqlLiteral(tidModuleKey)}`,
    "order by create_time desc nulls last, id desc;",
  ].join(" ");
  return {
    sql,
    rows: parseTableRows(runSql(sql), ["id", "tidModuleKey", "configVersion", "modifyTime"]),
  };
}

function queryOptionCount() {
  const sql = [
    "select count(*)::text",
    "from public.systemconfig_config_option opt",
    "join public.systemconfig_config_info info on info.id = opt.config_id",
    `where info.app_code = ${sqlLiteral(appCode)} and info.code = ${sqlLiteral(configCode)};`,
  ].join(" ");
  return {
    sql,
    rows: parseTableRows(runSql(sql), ["count"]),
  };
}

function requestMatcher(url) {
  return /\/(?:inter-api|open-api)\/systemconfig\/v1\//.test(url) ||
    /\/inter-api\/rbac\/v1\/userPermission\/findUserOperate/.test(url);
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
  if (!/^[0-9A-Za-z_]{1,56}$/.test(value)) {
    throw new Error(`${label} must match ^[0-9A-Za-z_]{1,56}$: ${value}`);
  }
}

function isBusinessOk(result) {
  if (!result.ok || visibleErrorPattern.test(result.text)) {
    return false;
  }
  if (result.json && Object.prototype.hasOwnProperty.call(result.json, "code")) {
    return ["0", "200", "100000000"].includes(String(result.json.code));
  }
  return true;
}

function assertBusinessOk(result, label) {
  if (!isBusinessOk(result)) {
    throw new Error(`${label} failed with ${result.status}: ${result.text.slice(0, 1200)}`);
  }
}

function extractResultData(result) {
  const json = result.json || {};
  if (json.data && Object.prototype.hasOwnProperty.call(json.data, "data")) {
    return json.data.data;
  }
  if (Object.prototype.hasOwnProperty.call(json, "data")) {
    return json.data;
  }
  return json;
}

async function main() {
  ensureDir(outputDir);
  ensureDir(path.dirname(outputPath));
  assertCode(appCode, "appCode");
  assertCode(catalogCode, "catalogCode");
  assertCode(configCode, "configCode");

  const catalogPrecheck = queryCatalog();
  const configPrecheck = queryConfig();
  if (catalogPrecheck.rows.length || configPrecheck.rows.length) {
    throw new Error(`System config marker already exists: ${JSON.stringify({ catalog: catalogPrecheck.rows, config: configPrecheck.rows })}`);
  }

  const api = await request.newContext({ ignoreHTTPSErrors: true });
  const ticket = await login(api);
  await api.dispose();

  let browser;
  const consoleMessages = [];
  const pageErrors = [];
  const requestFailures = [];
  const capturedRequests = [];
  const capturedResponses = [];

  try {
    browser = await chromium.launch({ headless });
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
    page.on("console", (message) => {
      if (["error", "warning"].includes(message.type())) {
        consoleMessages.push({ type: message.type(), text: message.text() });
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

    const route = "/systemconfig/#/sysconfig";
    const navigation = await page.goto(`${browserBaseUrl}${route}`, { waitUntil: "domcontentloaded", timeout: 45000 });
    await page.waitForLoadState("networkidle", { timeout: 15000 }).catch(() => {});
    await page.waitForTimeout(1500);

    const catalogListBefore = await browserApi(page, "GET", "/inter-api/systemconfig/v1/config/catalog?keyword=&type=2");
    assertBusinessOk(catalogListBefore, "System config catalog list before create");

    const createPayload = {
      type: 2,
      catalogs: [
        {
          appCode,
          code: catalogCode,
          name: catalogName,
          order: 9999,
          hide: false,
          config: [
            {
              code: configCode,
              name: configName,
              order: 1,
              type: 0,
              defaultValue: [defaultValue],
              verify: [{ length: 120, isRequire: true }],
              typeConfig: { remind: `${marker} remind` },
            },
          ],
        },
      ],
    };
    const createResult = await browserApi(page, "POST", "/open-api/systemconfig/v1/config/catalog", createPayload);
    assertBusinessOk(createResult, "System config create");
    await page.waitForTimeout(700);

    const catalogAfterCreate = queryCatalog();
    const configAfterCreate = queryConfig();
    const versionAfterCreate = queryVersion();
    const optionCountAfterCreate = queryOptionCount();
    const createdCatalog = catalogAfterCreate.rows[0];
    const createdConfig = configAfterCreate.rows[0];
    if (
      !createdCatalog ||
      createdCatalog.name !== catalogName ||
      createdCatalog.appCode !== appCode ||
      createdCatalog.catalogType !== "2" ||
      createdCatalog.hasHide !== "false"
    ) {
      throw new Error(`System config catalog create did not persist expected values: ${JSON.stringify(catalogAfterCreate.rows)}`);
    }
    if (
      !createdConfig ||
      createdConfig.catalogId !== createdCatalog.id ||
      createdConfig.name !== configName ||
      createdConfig.appCode !== appCode ||
      createdConfig.moduleCode !== catalogCode ||
      createdConfig.widgetType !== "0" ||
      createdConfig.defaultValue !== defaultValue ||
      createdConfig.maxValue !== "120" ||
      createdConfig.hasRequire !== "true" ||
      createdConfig.description !== `${marker} remind`
    ) {
      throw new Error(`System config item create did not persist expected values: ${JSON.stringify(configAfterCreate.rows)}`);
    }
    if (!versionAfterCreate.rows.some((row) => row.tidModuleKey === tidModuleKey)) {
      throw new Error(`System config create did not persist version for ${tidModuleKey}: ${JSON.stringify(versionAfterCreate.rows)}`);
    }

    const searchAfterCreate = await browserApi(
      page,
      "GET",
      `/inter-api/systemconfig/v1/config/catalog?keyword=${encodeURIComponent(marker)}&type=2`
    );
    assertBusinessOk(searchAfterCreate, "System config search after create");
    const searchData = extractResultData(searchAfterCreate);
    const searchCatalogs = (searchData && searchData.catalogs) || [];
    const foundInSearch = JSON.stringify(searchCatalogs).includes(createdCatalog.id);
    if (!foundInSearch) {
      throw new Error(`System config created catalog was not visible in catalog search: ${JSON.stringify(searchCatalogs).slice(0, 1200)}`);
    }

    const detailAfterCreate = await browserApi(page, "GET", `/inter-api/systemconfig/v1/config/catalog/${createdCatalog.id}`);
    assertBusinessOk(detailAfterCreate, "System config detail after create");
    const detailData = extractResultData(detailAfterCreate);
    const detailConfig = ((detailData && detailData.config) || []).find((item) => String(item.configId) === createdConfig.id);
    if (!detailConfig || detailConfig.code !== configCode || !Array.isArray(detailConfig.value) || detailConfig.value[0] !== defaultValue) {
      throw new Error(`System config detail did not expose created config: ${JSON.stringify(detailData).slice(0, 1200)}`);
    }

    const updatePayload = {
      catalogId: Number(createdCatalog.id),
      config: [
        {
          configId: Number(createdConfig.id),
          value: [updatedValue],
        },
      ],
    };
    const updateResult = await browserApi(page, "PUT", "/inter-api/systemconfig/v1/config/catalog/value", updatePayload);
    assertBusinessOk(updateResult, "System config value update");
    await page.waitForTimeout(700);

    const configAfterUpdate = queryConfig();
    const versionAfterUpdate = queryVersion();
    const updatedConfig = configAfterUpdate.rows.find((row) => row.id === createdConfig.id);
    if (!updatedConfig || updatedConfig.widgetValue !== updatedValue) {
      throw new Error(`System config value update did not persist expected value: ${JSON.stringify(configAfterUpdate.rows)}`);
    }
    if (!versionAfterUpdate.rows.some((row) => row.tidModuleKey === tidModuleKey)) {
      throw new Error(`System config update did not keep version row for ${tidModuleKey}: ${JSON.stringify(versionAfterUpdate.rows)}`);
    }

    const readByModule = await browserApi(
      page,
      "GET",
      `/inter-api/systemconfig/v1/config/catalog/by/module?moduleCode=${encodeURIComponent(catalogCode)}&key=${encodeURIComponent(configCode)}`
    );
    assertBusinessOk(readByModule, "System config module read after update");
    const readData = extractResultData(readByModule);
    if (!readData || readData[configCode] !== updatedValue) {
      throw new Error(`System config module read did not return updated value: ${JSON.stringify(readData)}`);
    }

    const deleteResult = await browserApi(page, "DELETE", `/open-api/systemconfig/v1/config/catalog/${appCode}/${catalogCode}`);
    assertBusinessOk(deleteResult, "System config delete");
    await page.waitForTimeout(700);

    const catalogAfterDelete = queryCatalog();
    const configAfterDelete = queryConfig();
    const versionAfterDelete = queryVersion();
    const optionCountAfterDelete = queryOptionCount();
    if (catalogAfterDelete.rows.length || configAfterDelete.rows.length || versionAfterDelete.rows.length) {
      throw new Error(
        `System config delete did not clean catalog/config/version rows: ${JSON.stringify({
          catalog: catalogAfterDelete.rows,
          config: configAfterDelete.rows,
          version: versionAfterDelete.rows,
        })}`
      );
    }

    const bodyText = await page.locator("body").innerText({ timeout: 5000 }).catch(() => "");
    const visibleError = String(bodyText)
      .split(/\r?\n/)
      .map((line) => line.trim())
      .find((line) => line && visibleErrorPattern.test(line));
    if (visibleError) {
      throw new Error(`System config page displayed visible error: ${visibleError}`);
    }
    if (pageErrors.length || requestFailures.length) {
      throw new Error(`System config browser errors: ${JSON.stringify({ pageErrors, requestFailures })}`);
    }

    const screenshotPath = path.join(outputDir, "systemconfig-persistence.png");
    await page.screenshot({ path: screenshotPath, fullPage: true }).catch(() => {});

    const report = {
      generatedAt: new Date().toISOString(),
      status: "PASS",
      baseUrl,
      browserBaseUrl,
      username,
      marker,
      route,
      appCode,
      catalogCode,
      configCode,
      tenantId,
      tidModuleKey,
      backendTrace: {
        create:
          "SystemConfigController.create -> SystemConfigServiceImpl.batchInsertConfig -> systemconfig_config_catalog/systemconfig_config_info; setConfigInfoCache -> systemconfig_config_version",
        update:
          "WebControllerService.updateConfig -> SystemConfigServiceImpl.updateConfigInfoById -> systemconfig_config_info.widget_value; updateConfigInfo -> systemconfig_config_version",
        read:
          "WebControllerService.getConfigByModuleCodeAndKey -> SystemConfigServiceImpl.selectByCode/selectByCatalogIdAndKey -> systemconfig_config_info",
        delete:
          "SystemConfigController.deleteSystemConfig -> SystemConfigServiceImpl.deleteBatchIds(appCode, code) -> delete catalog/info/options and config_version",
      },
      browser: {
        navigationStatus: navigation ? navigation.status() : null,
        consoleMessages: compactErrors(consoleMessages),
        pageErrors: compactErrors(pageErrors),
        requestFailures: compactErrors(requestFailures),
        visibleError: null,
        screenshot: screenshotPath,
      },
      operations: {
        catalogListBefore: {
          method: "GET",
          api: "/inter-api/systemconfig/v1/config/catalog?keyword=&type=2",
          responseStatus: catalogListBefore.status,
          responseBody: catalogListBefore.json || catalogListBefore.text.slice(0, 1000),
        },
        create: {
          method: "POST",
          api: "/open-api/systemconfig/v1/config/catalog",
          payload: createPayload,
          responseStatus: createResult.status,
          responseBody: createResult.json || createResult.text.slice(0, 1000),
          verificationSql: `${catalogAfterCreate.sql} ${configAfterCreate.sql} ${versionAfterCreate.sql} ${optionCountAfterCreate.sql}`,
          rows: {
            catalog: catalogAfterCreate.rows,
            config: configAfterCreate.rows,
            version: versionAfterCreate.rows,
            optionCount: optionCountAfterCreate.rows,
          },
        },
        searchAfterCreate: {
          method: "GET",
          api: `/inter-api/systemconfig/v1/config/catalog?keyword=${marker}&type=2`,
          responseStatus: searchAfterCreate.status,
          responseBody: searchAfterCreate.json || searchAfterCreate.text.slice(0, 1000),
        },
        detailAfterCreate: {
          method: "GET",
          api: `/inter-api/systemconfig/v1/config/catalog/${createdCatalog.id}`,
          responseStatus: detailAfterCreate.status,
          responseBody: detailAfterCreate.json || detailAfterCreate.text.slice(0, 1000),
        },
        update: {
          method: "PUT",
          api: "/inter-api/systemconfig/v1/config/catalog/value",
          payload: updatePayload,
          responseStatus: updateResult.status,
          responseBody: updateResult.json || updateResult.text.slice(0, 1000),
          verificationSql: `${configAfterUpdate.sql} ${versionAfterUpdate.sql}`,
          rows: {
            config: configAfterUpdate.rows,
            version: versionAfterUpdate.rows,
          },
        },
        readByModule: {
          method: "GET",
          api: `/inter-api/systemconfig/v1/config/catalog/by/module?moduleCode=${catalogCode}&key=${configCode}`,
          responseStatus: readByModule.status,
          responseBody: readByModule.json || readByModule.text.slice(0, 1000),
        },
        delete: {
          method: "DELETE",
          api: `/open-api/systemconfig/v1/config/catalog/${appCode}/${catalogCode}`,
          responseStatus: deleteResult.status,
          responseBody: deleteResult.json || deleteResult.text.slice(0, 1000),
          verificationSql: `${catalogAfterDelete.sql} ${configAfterDelete.sql} ${versionAfterDelete.sql} ${optionCountAfterDelete.sql}`,
          rows: {
            catalog: catalogAfterDelete.rows,
            config: configAfterDelete.rows,
            version: versionAfterDelete.rows,
            optionCount: optionCountAfterDelete.rows,
          },
        },
      },
      capturedRequests,
      capturedResponses,
    };

    fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`);
    console.log(JSON.stringify(report, null, 2));
  } finally {
    if (browser) {
      await browser.close();
    }
  }
}

main().catch((error) => {
  ensureDir(path.dirname(outputPath));
  const report = {
    generatedAt: new Date().toISOString(),
    status: "FAIL",
    baseUrl,
    browserBaseUrl,
    username,
    marker,
    appCode,
    catalogCode,
    configCode,
    tenantId,
    tidModuleKey,
    error: error && error.stack ? error.stack : String(error),
  };
  fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`);
  console.error(error);
  process.exit(1);
});
