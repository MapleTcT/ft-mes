#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const crypto = require("crypto");
const { execFileSync, spawnSync } = require("child_process");
const { chromium, request } = require("playwright");

const repoRoot = path.resolve(__dirname, "../../..");
const stamp = new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14);
const baseUrl = (process.env.ADP_BASE_URL || "http://100.99.133.43:18080").replace(/\/+$/, "");
const browserBaseUrl = (process.env.ADP_BROWSER_BASE_URL || baseUrl).replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD || "123456";
const outputPath =
  process.env.ADP_SYSTEMCONFIG_BUILTINS_OUTPUT ||
  path.join("/tmp", `adp-systemconfig-builtins-readiness-${stamp}.json`);
const dbSshTarget = process.env.ADP_DB_SSH_TARGET || "v6@100.99.133.43";
const dbContainer = process.env.ADP_DB_CONTAINER || "adp-mes-newbase-postgres-1";
const dbName = process.env.ADP_DB_NAME || "adp";
const dbUser = process.env.ADP_DB_USER || "adp";
const sshConnectTimeout = process.env.ADP_SSH_CONNECT_TIMEOUT || "20";
const parsedSqlRetries = Number(process.env.ADP_SQL_RETRIES || "3");
const sqlRetries = Number.isFinite(parsedSqlRetries) && parsedSqlRetries > 0 ? Math.floor(parsedSqlRetries) : 3;
const headless = process.env.ADP_HEADLESS !== "false";

const expectedCatalogNames = (process.env.ADP_SYSTEMCONFIG_BUILTIN_NAMES ||
  [
    "用户目录",
    "打印服务授权",
    "AK/SK凭证管理",
    "密码配置",
    "质量检验配置",
    "RM.ocd.RM",
    "BaseSet.ocd.BaseSet",
  ].join("|"))
  .split("|")
  .map((value) => value.trim())
  .filter(Boolean);

const catalogPolicies = {
  "用户目录": {
    classification: "identity-directory",
    editPolicy: "read-only-in-smoke",
    risk: "Identity source changes can affect login and user synchronization.",
    mutationAllowedInSmoke: false,
  },
  "打印服务授权": {
    classification: "license-or-entitlement",
    editPolicy: "read-only-in-smoke",
    risk: "License changes can invalidate client-side spreadsheet/report components.",
    mutationAllowedInSmoke: false,
  },
  "AK/SK凭证管理": {
    classification: "secret",
    editPolicy: "read-only-in-smoke",
    risk: "Credential values must not be written to committed reports or mutated by generic smoke.",
    mutationAllowedInSmoke: false,
  },
  "密码配置": {
    classification: "security-policy",
    editPolicy: "read-only-in-smoke",
    risk: "Password policy changes can lock out test and production users.",
    mutationAllowedInSmoke: false,
  },
  "质量检验配置": {
    classification: "business-runtime-config",
    editPolicy: "controlled-marker-required",
    risk: "QCS config changes can affect inspection/report generation behavior already covered by production markers.",
    mutationAllowedInSmoke: false,
  },
  "RM.ocd.RM": {
    classification: "business-runtime-config",
    editPolicy: "controlled-marker-required",
    risk: "RM config changes can affect formula/batch integration behavior.",
    mutationAllowedInSmoke: false,
  },
  "BaseSet.ocd.BaseSet": {
    classification: "business-runtime-config",
    editPolicy: "controlled-marker-required",
    risk: "BaseSet config changes can affect recovered batch/material defaults.",
    mutationAllowedInSmoke: false,
  },
};

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

function digestValue(value) {
  return crypto.createHash("sha256").update(JSON.stringify(value ?? null)).digest("hex").slice(0, 12);
}

function summarizeValue(value) {
  const values = Array.isArray(value) ? value : value === undefined || value === null ? [] : [value];
  return {
    present: values.length > 0,
    itemCount: values.length,
    sha256Prefix: digestValue(values),
  };
}

function summarizeConfig(config) {
  return {
    configId: String(config.configId || ""),
    code: String(config.code || ""),
    name: String(config.name || ""),
    type: config.type === undefined || config.type === null ? "" : String(config.type),
    required: Array.isArray(config.verify) ? config.verify.some((rule) => rule && rule.isRequire === true) : false,
    valueSummary: summarizeValue(config.value),
  };
}

function sanitizePayloadForEvidence(payload) {
  if (!payload || typeof payload !== "object") {
    return payload;
  }
  const cloned = JSON.parse(JSON.stringify(payload));
  const configs = cloned && cloned.data && cloned.data.config;
  if (Array.isArray(configs)) {
    cloned.data.config = configs.map((config) => summarizeConfig(config));
  }
  return cloned;
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
  let lastError = null;
  for (let attempt = 1; attempt <= Math.max(1, sqlRetries); attempt += 1) {
    try {
      return execFileSync("ssh", ["-o", "BatchMode=yes", "-o", `ConnectTimeout=${sshConnectTimeout}`, dbSshTarget, remoteCommand], {
        encoding: "utf8",
        stdio: ["ignore", "pipe", "pipe"],
      }).trim();
    } catch (error) {
      lastError = error;
      if (attempt < sqlRetries) {
        Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 1000 * attempt);
      }
    }
  }
  throw lastError;
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

function parseCatalogTree(payload) {
  const roots =
    (payload && payload.data && payload.data.catalogs) ||
    (payload && payload.result && payload.result.catalogs) ||
    (payload && payload.catalogs) ||
    [];
  const rows = [];
  for (const root of Array.isArray(roots) ? roots : []) {
    const children = Array.isArray(root.catalog) ? root.catalog : [];
    for (const child of children) {
      rows.push({
        catalogId: String(child.catalogId || ""),
        name: String(child.name || ""),
        moduleCode: child.moduleCode || "",
        parentId: String(root.catalogId || ""),
        parentName: String(root.name || ""),
      });
    }
  }
  return rows.filter((row) => row.catalogId && row.name);
}

function requestMatcher(url) {
  return /\/inter-api\/systemconfig\/v1\/config\/catalog/.test(url);
}

async function browserApi(page, method, urlPath) {
  return page.evaluate(
    async ({ method: methodArg, urlPath: urlPathArg }) => {
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
          langu_code: "zh_CN",
        },
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
    { method, urlPath }
  );
}

async function browserApiWithRetry(page, method, urlPath, attempts = 3) {
  const results = [];
  for (let index = 0; index < attempts; index += 1) {
    const result = await browserApi(page, method, urlPath);
    results.push({ status: result.status, ok: isBusinessOk(result) });
    if (isBusinessOk(result)) {
      return { ...result, attempts: results };
    }
    await page.waitForTimeout(1000 * (index + 1));
  }
  const last = results.length ? results[results.length - 1] : { status: "", ok: false };
  return { ok: false, status: last.status, text: "", json: null, attempts: results };
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

function buildCatalogQueries(catalogIds) {
  const idList = catalogIds.map(sqlLiteral).join(", ");
  const catalogSql = [
    "select id::text, coalesce(parent_id::text,''), code, name, coalesce(app_code,''),",
    "coalesce(catalog_type::text,''), coalesce(has_hide::text,''),",
    "coalesce(modify_time::text,'')",
    "from public.systemconfig_config_catalog",
    `where id::text in (${idList})`,
    "order by id::text;",
  ].join(" ");
  const configSql = [
    "select catalog_id::text, count(*)::text",
    "from public.systemconfig_config_info",
    `where catalog_id::text in (${idList})`,
    "group by catalog_id::text",
    "order by catalog_id::text;",
  ].join(" ");
  return { catalogSql, configSql };
}

function addCheck(checks, name, passed, evidence) {
  checks.push({ name, status: passed ? "PASS" : "FAIL", evidence });
}

async function main() {
  ensureDir(outputPath);
  const api = await request.newContext({ ignoreHTTPSErrors: true });
  const { ticket, loginPayload } = await login(api);
  await api.dispose();

  let browser;
  const route = "/systemconfig/#/sysconfig";
  const consoleMessages = [];
  const pageErrors = [];
  const requestFailures = [];
  const capturedResponses = [];
  const checks = [];
  let navigationStatus = null;
  let navigationError = null;
  let visibleError = null;
  let catalogList = null;
  const details = [];
  const db = {
    catalogSql: "",
    configSql: "",
    catalogRows: [],
    configCounts: [],
  };

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
    page.on("console", (message) => {
      if (["error", "warning"].includes(message.type())) {
        consoleMessages.push({ type: message.type(), text: message.text().slice(0, 500) });
      }
    });
    page.on("pageerror", (error) => pageErrors.push(error.message.slice(0, 500)));
    page.on("requestfailed", (requestItem) => {
      if (requestMatcher(requestItem.url())) {
        requestFailures.push({
          method: requestItem.method(),
          url: requestItem.url(),
          failure: requestItem.failure() && requestItem.failure().errorText,
        });
      }
    });
    page.on("response", async (response) => {
      if (!requestMatcher(response.url())) {
        return;
      }
      let body = "";
      try {
        const rawBody = await response.text();
        try {
          body = JSON.stringify(sanitizePayloadForEvidence(JSON.parse(rawBody))).slice(0, 1200);
        } catch (_jsonError) {
          body = rawBody.slice(0, 800);
        }
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

    const navigation = await page.goto(`${browserBaseUrl}${route}`, { waitUntil: "domcontentloaded", timeout: 45000 });
    navigationStatus = navigation ? navigation.status() : null;
    await page.waitForLoadState("networkidle", { timeout: 15000 }).catch(() => {});
    await page.waitForTimeout(1000);

    catalogList = await browserApiWithRetry(page, "GET", "/inter-api/systemconfig/v1/config/catalog?keyword=&type=2");
    const observedCatalogs = parseCatalogTree(catalogList.json);
    const byName = new Map(observedCatalogs.map((catalog) => [catalog.name, catalog]));
    const expectedCatalogs = expectedCatalogNames.map((name) => ({
      name,
      ...(catalogPolicies[name] || {
        classification: "unclassified",
        editPolicy: "manual-review-required",
        risk: "No built-in catalog edit policy has been recorded for this catalog.",
        mutationAllowedInSmoke: false,
      }),
      observed: byName.has(name),
      ...(byName.get(name) || {}),
    }));
    const catalogIds = expectedCatalogs.filter((item) => item.observed).map((item) => item.catalogId);

    for (const catalog of expectedCatalogs.filter((item) => item.observed)) {
      const detail = await browserApiWithRetry(
        page,
        "GET",
        `/inter-api/systemconfig/v1/config/catalog/${encodeURIComponent(catalog.catalogId)}`
      );
      details.push({
        catalogId: catalog.catalogId,
        name: catalog.name,
        classification: catalog.classification,
        editPolicy: catalog.editPolicy,
        mutationAllowedInSmoke: catalog.mutationAllowedInSmoke,
        status: detail.status,
        attempts: detail.attempts || [{ status: detail.status, ok: isBusinessOk(detail) }],
        ok: isBusinessOk(detail),
        configCount: Array.isArray(detail.json && detail.json.data && detail.json.data.config)
          ? detail.json.data.config.length
          : null,
        configSummaries: Array.isArray(detail.json && detail.json.data && detail.json.data.config)
          ? detail.json.data.config.map(summarizeConfig)
          : [],
      });
    }

    if (catalogIds.length) {
      const queries = buildCatalogQueries(catalogIds);
      db.catalogSql = queries.catalogSql;
      db.configSql = queries.configSql;
      db.catalogRows = parseTableRows(runSql(queries.catalogSql), [
        "id",
        "parentId",
        "code",
        "name",
        "appCode",
        "catalogType",
        "hasHide",
        "modifyTime",
      ]);
      db.configCounts = parseTableRows(runSql(queries.configSql), ["catalogId", "configCount"]);
    }

    const bodyText = await page.locator("body").innerText({ timeout: 5000 }).catch(() => "");
    visibleError = String(bodyText || "")
      .split(/\r?\n/)
      .map((line) => line.trim())
      .find((line) => line && visibleErrorPattern.test(line)) || null;

    const dbIds = new Set(db.catalogRows.map((row) => row.id));
    const missingCatalogs = expectedCatalogs.filter((catalog) => !catalog.observed).map((catalog) => catalog.name);
    const detailFailures = details.filter((detail) => !detail.ok).map((detail) => detail.name);
    const dbMissing = expectedCatalogs
      .filter((catalog) => catalog.observed && !dbIds.has(catalog.catalogId))
      .map((catalog) => catalog.name);
    const missingPolicies = expectedCatalogs
      .filter((catalog) => catalog.classification === "unclassified" || catalog.editPolicy === "manual-review-required")
      .map((catalog) => catalog.name);
    const mutationAllowed = expectedCatalogs
      .filter((catalog) => catalog.mutationAllowedInSmoke)
      .map((catalog) => catalog.name);
    const transientApiFailures = [
      ...(catalogList.attempts || []),
      ...details.flatMap((detail) => detail.attempts || []),
    ].filter((attempt) => attempt.ok !== true).length;
    const ignoredTransientConsoleMessages = consoleMessages.filter((message) =>
      /Failed to load resource: the server responded with a status of 503/i.test(message.text)
    );
    const blockingConsoleMessages = consoleMessages.filter(
      (message) => !ignoredTransientConsoleMessages.includes(message)
    );
    addCheck(checks, "browser-route-systemconfig", navigationStatus === 200 && !navigationError, `navigationStatus=${navigationStatus || ""}`);
    addCheck(
      checks,
      "catalog-list-api",
      isBusinessOk(catalogList),
      `status=${catalogList.status}; attempts=${(catalogList.attempts || []).map((attempt) => attempt.status).join("/")}`
    );
    addCheck(checks, "expected-built-in-catalogs-present", missingCatalogs.length === 0, `missing=${missingCatalogs.join(",") || "none"}`);
    addCheck(checks, "catalog-detail-read", detailFailures.length === 0, `failed=${detailFailures.join(",") || "none"}`);
    addCheck(checks, "catalog-postgres-rows", dbMissing.length === 0, `missing=${dbMissing.join(",") || "none"}`);
    addCheck(
      checks,
      "built-in-edit-policy-boundary",
      missingPolicies.length === 0 && mutationAllowed.length === 0,
      `unclassified=${missingPolicies.join(",") || "none"}; mutationAllowed=${mutationAllowed.join(",") || "none"}`
    );
    addCheck(
      checks,
      "browser-error-free",
      !visibleError && blockingConsoleMessages.length === 0 && pageErrors.length === 0 && requestFailures.length === 0,
      `visible=${visibleError || "none"}; console=${blockingConsoleMessages.length}; ignoredTransientConsole=${ignoredTransientConsoleMessages.length}; page=${pageErrors.length}; request=${requestFailures.length}`
    );

    const failedChecks = checks.filter((check) => check.status !== "PASS");
    const report = {
      schemaVersion: 1,
      generatedAt: new Date().toISOString(),
      repoCommit: getRepoCommit(),
      database: "PostgreSQL",
      module: "basic-config",
      areaId: "systemconfig-built-in-catalogs",
      status: failedChecks.length ? "FAIL" : "PASS",
      baseUrl,
      browserBaseUrl,
      route,
      username,
      summary: {
        status: failedChecks.length ? "FAIL" : "PASS",
        expectedCatalogs: expectedCatalogs.length,
        observedCatalogs: expectedCatalogs.filter((catalog) => catalog.observed).length,
        missingCatalogs: missingCatalogs.length,
        detailReads: details.length,
        detailFailures: detailFailures.length,
        dbCatalogRows: db.catalogRows.length,
        dbMissing: dbMissing.length,
        sensitiveCatalogs: expectedCatalogs.filter((catalog) =>
          ["identity-directory", "license-or-entitlement", "secret", "security-policy"].includes(catalog.classification)
        ).length,
        mutationAllowedInSmoke: mutationAllowed.length,
        configItems: details.reduce((sum, detail) => sum + (Number(detail.configCount) || 0), 0),
        transientApiFailures,
        browserErrors: (visibleError ? 1 : 0) + blockingConsoleMessages.length + pageErrors.length + requestFailures.length,
      },
      checks,
      expectedCatalogs,
      details,
      databaseEvidence: db,
      browser: {
        navigationStatus,
        navigationError,
        visibleError,
        consoleMessages,
        pageErrors,
        requestFailures,
      },
      capturedResponses,
      evidence: {
        method: "Authenticated real browser route plus in-browser systemconfig catalog/detail API reads and read-only PostgreSQL metadata queries.",
        editPolicy: "This smoke classifies built-in catalog risk and intentionally performs no edit/save action. Editing requires a separate marker acceptance per catalog.",
        valueRedaction: "Config values in details and capturedResponses are represented by count/hash summaries only.",
        secretHandling: "Login token and password are used only in memory and are not written to this report.",
      },
    };
    fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`);
    console.log(`${report.status} systemconfig built-in catalogs observed=${report.summary.observedCatalogs}/${report.summary.expectedCatalogs}`);
    console.log(`REPORT ${outputPath}`);
    if (failedChecks.length) {
      process.exitCode = 1;
    }
  } catch (error) {
    const report = {
      schemaVersion: 1,
      generatedAt: new Date().toISOString(),
      repoCommit: getRepoCommit(),
      database: "PostgreSQL",
      module: "basic-config",
      areaId: "systemconfig-built-in-catalogs",
      status: "FAIL",
      baseUrl,
      browserBaseUrl,
      route,
      username,
      summary: {
        status: "FAIL",
        expectedCatalogs: expectedCatalogNames.length,
        observedCatalogs: 0,
        missingCatalogs: expectedCatalogNames.length,
        detailReads: details.length,
        detailFailures: details.filter((detail) => !detail.ok).length,
        dbCatalogRows: db.catalogRows.length,
        dbMissing: 0,
        sensitiveCatalogs: 0,
        mutationAllowedInSmoke: 0,
        configItems: 0,
        browserErrors: 1,
      },
      checks,
      details,
      databaseEvidence: db,
      browser: {
        navigationStatus,
        navigationError: navigationError || (error && error.message ? error.message : String(error)),
        visibleError,
        consoleMessages,
        pageErrors,
        requestFailures,
      },
      capturedResponses,
      evidence: {
        method: "Authenticated real browser route plus in-browser systemconfig catalog/detail API reads and read-only PostgreSQL metadata queries.",
        editPolicy: "This smoke classifies built-in catalog risk and intentionally performs no edit/save action. Editing requires a separate marker acceptance per catalog.",
        valueRedaction: "Config values in details and capturedResponses are represented by count/hash summaries only.",
        secretHandling: "Login token and password are used only in memory and are not written to this report.",
      },
    };
    fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`);
    console.error(error && error.stack ? error.stack : error);
    process.exit(1);
  } finally {
    if (browser) {
      await browser.close();
    }
  }
}

main().catch((error) => {
  console.error(error && error.stack ? error.stack : error);
  process.exit(1);
});
