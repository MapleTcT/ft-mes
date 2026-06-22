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
  process.env.ADP_SYSTEMCONFIG_CONTROLLED_OUTPUT ||
  path.join("/tmp", `adp-systemconfig-controlled-runtime-config-${stamp}.json`);
const dbSshTarget = process.env.ADP_DB_SSH_TARGET || "v6@100.99.133.43";
const dbContainer = process.env.ADP_DB_CONTAINER || "adp-mes-newbase-postgres-1";
const dbName = process.env.ADP_DB_NAME || "adp";
const dbUser = process.env.ADP_DB_USER || "adp";
const sshConnectTimeout = process.env.ADP_SSH_CONNECT_TIMEOUT || "20";
const headless = process.env.ADP_HEADLESS !== "false";
const targetMode = (process.env.ADP_SYSTEMCONFIG_CONTROLLED_TARGET_MODE || "qcs").toLowerCase();
const markerBase = process.env.ADP_E2E_MARKER || `ADP_E2E_${stamp}_SCFG_${targetMode.toUpperCase()}_RUNTIME`;

const targetDefinitions = {
  qcs: [
    {
      actionId: "builtin-qcs-runtime-config",
      catalogName: "质量检验配置",
      catalogId: 117000,
      catalogCode: "QCS",
      configId: 117001,
      configCode: "reportShowIndexRange",
      moduleCode: "QCS",
      marker: markerBase,
      regressionStatus:
        "ROLLBACK_ONLY; QCS production chain regression is required before this action can become PASS.",
    },
  ],
  rm: [
    {
      actionId: "builtin-rm-runtime-config",
      catalogName: "RM.ocd.MQ",
      catalogId: 129020,
      catalogCode: "RM.MQ",
      configId: 129003,
      configCode: "brokerUrl",
      moduleCode: "RM.MQ",
      marker: markerBase,
      regressionStatus:
        "ROLLBACK_ONLY; RM formula sync/delete regression is required before this action can become PASS.",
    },
  ],
  baseset: [
    {
      actionId: "builtin-baseset-runtime-config",
      catalogName: "BaseSet.ocd.BaseSet",
      catalogId: 129030,
      catalogCode: "BaseSet",
      configId: 129004,
      configCode: "isEnable",
      moduleCode: "BaseSet",
      marker: markerBase,
      toggleBoolean: true,
      regressionStatus:
        "ROLLBACK_ONLY; representative BaseSet/WOM/QCS regression is required before this action can become PASS.",
    },
  ],
};

const targets = targetDefinitions[targetMode];
if (!targets) {
  throw new Error(
    `Unsupported ADP_SYSTEMCONFIG_CONTROLLED_TARGET_MODE=${targetMode}; expected one of ${Object.keys(targetDefinitions).join(", ")}`
  );
}

const visibleErrorPattern =
  /(数据库操作异常|系统错误|系统异常|发生未知异常|SQLGrammarException|could not extract ResultSet|column .* does not exist|relation .* does not exist|500 INTERNAL|\b[\w.]+Exception(?::|\s+at\b))/i;

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
      return { ticket, loginPayload: parsed.json };
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

function configRowSql(configId) {
  return [
    "select i.id::text, i.catalog_id::text, c.code, c.name, i.code, i.name,",
    "coalesce(i.widget_type::text,''), coalesce(i.widget_value,''),",
    "coalesce(v.tid_module_key,''), coalesce(v.config_version,''), coalesce(i.modify_time::text,'')",
    "from public.systemconfig_config_info i",
    "join public.systemconfig_config_catalog c on c.id=i.catalog_id",
    "left join public.systemconfig_config_version v on v.tid_module_key like concat('dt/%/', c.code)",
    `where i.id=${sqlLiteral(configId)};`,
  ].join(" ");
}

function queryConfig(configId) {
  const sql = configRowSql(configId);
  const rows = parseRows(runSql(sql), [
    "id",
    "catalogId",
    "catalogCode",
    "catalogName",
    "configCode",
    "configName",
    "widgetType",
    "widgetValue",
    "tidModuleKey",
    "configVersion",
    "modifyTime",
  ]);
  return { sql, rows };
}

function detailConfigValue(detailResult, configId) {
  const configs = detailResult && detailResult.json && detailResult.json.data && detailResult.json.data.config;
  const config = Array.isArray(configs)
    ? configs.find((item) => String(item.configId) === String(configId))
    : null;
  return config && Array.isArray(config.value) ? String(config.value[0]) : "";
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
      return { ok: response.ok, status: response.status, text, json };
    },
    { method, urlPath, payload }
  );
}

function compactResponse(result) {
  if (!result) {
    return null;
  }
  return {
    status: result.status,
    ok: isBusinessOk(result),
    body: result.json || String(result.text || "").slice(0, 600),
  };
}

function updatePayloadFor(target, value) {
  return {
    catalogId: Number(target.catalogId),
    config: [
      {
        configId: Number(target.configId),
        value: [value],
      },
    ],
  };
}

function mutationValueFor(target, originalValue) {
  if (target.toggleBoolean) {
    return String(originalValue).toLowerCase() === "true" ? "false" : "true";
  }
  return target.mutationValue || target.marker;
}

async function acceptTarget(page, target) {
  const result = {
    actionId: target.actionId,
    catalogName: target.catalogName,
    catalogId: String(target.catalogId),
    configId: String(target.configId),
    configCode: target.configCode,
    marker: target.marker,
    status: "FAIL",
    regressionStatus: target.regressionStatus,
    issues: [],
  };
  let originalValue = "";
  let rollbackAttempted = false;

  try {
    const before = queryConfig(target.configId);
    originalValue = before.rows[0] ? before.rows[0].widgetValue : "";
    if (!originalValue) {
      throw new Error(`No original value found for config ${target.configId}`);
    }
    const mutationValue = mutationValueFor(target, originalValue);
    result.mutationValue = mutationValue;

    const detailBefore = await browserApi(
      page,
      "GET",
      `/inter-api/systemconfig/v1/config/catalog/${encodeURIComponent(target.catalogId)}`
    );
    if (!isBusinessOk(detailBefore)) {
      throw new Error(`Detail before update failed with ${detailBefore.status}`);
    }

    const markerPayload = updatePayloadFor(target, mutationValue);
    const update = await browserApi(page, "PUT", "/inter-api/systemconfig/v1/config/catalog/value", markerPayload);
    if (!isBusinessOk(update)) {
      throw new Error(`Marker update failed with ${update.status}: ${String(update.text || "").slice(0, 300)}`);
    }
    await page.waitForTimeout(700);

    const afterMarker = queryConfig(target.configId);
    const markerDbValue = afterMarker.rows[0] ? afterMarker.rows[0].widgetValue : "";
    const detailAfterMarker = await browserApi(
      page,
      "GET",
      `/inter-api/systemconfig/v1/config/catalog/${encodeURIComponent(target.catalogId)}`
    );
    const detailMarkerValue = detailConfigValue(detailAfterMarker, target.configId);
    const readByModule = await browserApi(
      page,
      "GET",
      `/inter-api/systemconfig/v1/config/catalog/by/module?moduleCode=${encodeURIComponent(target.moduleCode)}&key=${encodeURIComponent(
        target.configCode
      )}`
    );
    const readByModuleValue =
      readByModule && readByModule.json && readByModule.json.data
        ? String(readByModule.json.data[target.configCode] || "")
        : "";

    const rollbackPayload = updatePayloadFor(target, originalValue);
    rollbackAttempted = true;
    const rollback = await browserApi(page, "PUT", "/inter-api/systemconfig/v1/config/catalog/value", rollbackPayload);
    if (!isBusinessOk(rollback)) {
      throw new Error(`Rollback failed with ${rollback.status}: ${String(rollback.text || "").slice(0, 300)}`);
    }
    await page.waitForTimeout(700);

    const afterRollback = queryConfig(target.configId);
    const rollbackDbValue = afterRollback.rows[0] ? afterRollback.rows[0].widgetValue : "";
    const detailAfterRollback = await browserApi(
      page,
      "GET",
      `/inter-api/systemconfig/v1/config/catalog/${encodeURIComponent(target.catalogId)}`
    );
    const detailRollbackValue = detailConfigValue(detailAfterRollback, target.configId);

    result.before = {
      verificationSql: before.sql,
      rows: before.rows,
      detail: compactResponse(detailBefore),
      detailValue: detailConfigValue(detailBefore, target.configId),
    };
    result.markerUpdate = {
      method: "PUT",
      api: "/inter-api/systemconfig/v1/config/catalog/value",
      payload: markerPayload,
      response: compactResponse(update),
      verificationSql: afterMarker.sql,
      rows: afterMarker.rows,
      detail: compactResponse(detailAfterMarker),
      detailValue: detailMarkerValue,
      moduleRead: compactResponse(readByModule),
      moduleReadValue: readByModuleValue,
    };
    result.rollback = {
      method: "PUT",
      api: "/inter-api/systemconfig/v1/config/catalog/value",
      payload: rollbackPayload,
      response: compactResponse(rollback),
      verificationSql: afterRollback.sql,
      rows: afterRollback.rows,
      detail: compactResponse(detailAfterRollback),
      detailValue: detailRollbackValue,
    };

    const checks = [
      ["mutation persisted in PostgreSQL", markerDbValue === mutationValue],
      ["mutation visible in detail API", detailMarkerValue === mutationValue],
      ["mutation visible in module/key read", readByModuleValue === mutationValue],
      ["rollback restored PostgreSQL value", rollbackDbValue === originalValue],
      ["rollback visible in detail API", detailRollbackValue === originalValue],
    ];
    result.issues = checks.filter(([, ok]) => !ok).map(([name]) => name);
    result.status = result.issues.length ? "FAIL" : "PASS";
  } catch (error) {
    result.issues.push(error && error.message ? error.message : String(error));
    if (originalValue && !rollbackAttempted) {
      try {
        const rollbackPayload = updatePayloadFor(target, originalValue);
        const rollback = await browserApi(page, "PUT", "/inter-api/systemconfig/v1/config/catalog/value", rollbackPayload);
        await page.waitForTimeout(700);
        const afterRollback = queryConfig(target.configId);
        result.emergencyRollback = {
          response: compactResponse(rollback),
          verificationSql: afterRollback.sql,
          rows: afterRollback.rows,
        };
      } catch (rollbackError) {
        result.issues.push(`Emergency rollback failed: ${rollbackError.message || rollbackError}`);
      }
    }
  }
  return result;
}

async function main() {
  ensureDir(outputPath);
  const api = await request.newContext({ ignoreHTTPSErrors: true });
  const { ticket, loginPayload } = await login(api);
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
  await context.addInitScript(({ token, loginPayload: loginPayloadValue }) => {
    window.localStorage.clear();
    window.sessionStorage.clear();
    ["suposTicket", "SUPOS_TICKET", "token"].forEach((key) => {
      window.localStorage.setItem(key, token);
      window.sessionStorage.setItem(key, token);
    });
    if (loginPayloadValue) {
      window.localStorage.setItem("loginMsg", JSON.stringify(loginPayloadValue));
    }
    window.localStorage.setItem("language", "zh_CN");
    window.sessionStorage.setItem("language", "zh_CN");
  }, { token: ticket, loginPayload });

  const page = await context.newPage();
  const consoleMessages = [];
  const pageErrors = [];
  const requestFailures = [];
  page.on("console", (message) => {
    if (["error", "warning"].includes(message.type())) {
      consoleMessages.push({ type: message.type(), text: message.text().slice(0, 500) });
    }
  });
  page.on("pageerror", (error) => pageErrors.push(error.message.slice(0, 500)));
  page.on("requestfailed", (requestItem) => {
    if (/\/systemconfig\//.test(requestItem.url())) {
      requestFailures.push({
        method: requestItem.method(),
        url: requestItem.url(),
        failure: requestItem.failure() && requestItem.failure().errorText,
      });
    }
  });

  let navigationStatus = null;
  let navigationError = null;
  let visibleError = null;
  const route = "/systemconfig/#/sysconfig";
  try {
    const navigation = await page.goto(`${browserBaseUrl}${route}`, { waitUntil: "domcontentloaded", timeout: 120000 });
    navigationStatus = navigation ? navigation.status() : null;
    await page.waitForLoadState("networkidle", { timeout: 30000 }).catch(() => {});
    await page.waitForTimeout(1000);
  } catch (error) {
    navigationError = error.message;
  }

  const targetResults = [];
  if (!navigationError) {
    for (const target of targets) {
      targetResults.push(await acceptTarget(page, target));
    }
  }

  const bodyText = await page.locator("body").innerText({ timeout: 5000 }).catch(() => "");
  visibleError =
    String(bodyText || "")
      .split(/\r?\n/)
      .map((line) => line.trim())
      .find((line) => line && visibleErrorPattern.test(line)) || null;

  await browser.close();

  const failedTargets = targetResults.filter((target) => target.status !== "PASS");
  const ok =
    navigationStatus === 200 &&
    !navigationError &&
    !visibleError &&
    consoleMessages.length === 0 &&
    pageErrors.length === 0 &&
    requestFailures.length === 0 &&
    failedTargets.length === 0;

  const report = {
    schemaVersion: 1,
    generatedAt: new Date().toISOString(),
    repoCommit: getRepoCommit(),
    database: "PostgreSQL",
    module: "basic-config",
    areaId: "systemconfig-controlled-runtime-config",
    targetMode,
    status: ok ? "PASS" : "FAIL",
    baseUrl,
    browserBaseUrl,
    route,
    username,
    summary: {
      status: ok ? "PASS" : "FAIL",
      targetCount: targetResults.length,
      pass: targetResults.filter((target) => target.status === "PASS").length,
      fail: failedTargets.length,
      browserErrors: (visibleError ? 1 : 0) + consoleMessages.length + pageErrors.length + requestFailures.length,
      rollbackVerified: targetResults.every((target) => target.rollback && target.rollback.rows && target.rollback.rows.length > 0),
    },
    targets: targetResults,
    browser: {
      navigationStatus,
      navigationError,
      visibleError,
      consoleMessages,
      pageErrors,
      requestFailures,
    },
    evidence: {
      method:
        "Authenticated real browser route plus in-browser systemconfig PUT/GET calls, direct PostgreSQL before/after verification, and immediate rollback.",
      boundary:
        "This report proves controlled save/read/rollback for the listed runtime config only. It does not promote the whole built-in runtime config action to PASS without production-chain regression evidence.",
      secretHandling: "Login token and password are used only in memory and are not written to this report.",
    },
  };

  fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`);
  console.log(`${report.status} controlled-systemconfig targets=${report.summary.pass}/${report.summary.targetCount}`);
  console.log(`REPORT ${outputPath}`);
  if (!ok) {
    process.exitCode = 1;
  }
}

main().catch((error) => {
  console.error(error && error.stack ? error.stack : error);
  process.exit(1);
});
