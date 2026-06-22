#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { spawnSync } = require("child_process");
const { chromium, request } = require("playwright");

const repoRoot = path.resolve(__dirname, "../../..");

const baseUrl = (process.env.ADP_BASE_URL || "http://100.99.133.43:18080").replace(/\/+$/, "");
const browserBaseUrl = (process.env.ADP_BROWSER_BASE_URL || baseUrl).replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD || "123456";
const headless = process.env.ADP_HEADLESS !== "false";
const pageTimeoutMs = Number(process.env.ADP_PAGE_TIMEOUT_MS || 45000);
const apiTimeoutMs = Number(process.env.ADP_API_TIMEOUT_MS || 20000);
const outputPath =
  process.env.ADP_PRODUCTION_EXPORT_SMOKE_OUTPUT ||
  path.join("/tmp", `adp-production-export-readiness-${new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14)}.json`);

const visibleErrorPattern =
  /(系统错误|系统异常|发生未知异常|SQLGrammarException|could not extract ResultSet|404_NOT_FOUND|500 INTERNAL|\bHTTP\s*(404|500)\b|\b(404|500)\s+(Not Found|Internal Server Error)\b|Caused by:|\b[\w.]+Exception(?::|\s+at\b)|Invalid bound statement|relation .* does not exist|column .* does not exist)/i;

const targets = [
  {
    id: "wom-make-task",
    label: "WOM make task list",
    route: "/msService/WOM/produceTask/produceTask/makeTaskList",
    viewCode: "WOM_1.0.0_produceTask_makeTaskList",
    downloadPath: "/msService/WOM/produceTask/produceTask/downloadXls",
    queryExportPath: "/msService/WOM/produceTask/produceTask/makeTaskList-query",
    sourceHintPaths: [
      "deploy/docker/assets/module-static/WOM/produceTask/produceTask/makeTaskList.html",
      "deploy/docker/assets/module-static/WOM/produceTask/produceTask/prepareMakeTaskList.html",
      "deploy/docker/postgres/init/065-business-view-runtime-json.sql",
      "deploy/docker/postgres/init/078-wom-list-button-runtime-json.sql",
    ],
  },
  {
    id: "rm-batch-formula",
    label: "RM batch formula list",
    route: "/msService/RM/formula/formula/batchFormulaList",
    viewCode: "RM_1.0.0_formula_batchFormulaList",
    downloadPath: "/msService/RM/formula/formula/downloadXls",
    queryExportPath: "/msService/RM/formula/formula/batchFormulaList-query",
    sourceHintPaths: [
      "deploy/docker/postgres/init/065-business-view-runtime-json.sql",
      "deploy/docker/postgres/init/141-rm-formula-import-template.sql",
      "deploy/docker/postgres/init/160-rm-batch-formula-edit-view-runtime-json.sql",
    ],
  },
  {
    id: "wts-work-permit",
    label: "WTS work permit list",
    route: "/msService/WTS/workPermit/workPermit/workPermitList",
    viewCode: "WTS_1.0.0_workPermit_workPermitList",
    downloadPath: "/msService/WTS/workPermit/workPermit/downloadXls",
    queryExportPath: "/msService/WTS/workPermit/workPermit/workPermitList-query",
    sourceHintPaths: [
      "deploy/docker/postgres/init/065-business-view-runtime-json.sql",
      "deploy/docker/postgres/init/153-wts-firework-runtime-json.sql",
      "deploy/docker/postgres/init/165-wts-workpermit-list-runtime-compat.sql",
      "deploy/docker/postgres/init/171-wts-workpermit-export-action.sql",
    ],
  },
  {
    id: "qcs-inspect-report",
    label: "QCS inspect report list",
    route: "/msService/QCS/inspectReport/inspectReport/manuInspReportList",
    viewCode: "QCS_5.0.0.0_inspectReport_manuInspReportList",
    downloadPath: "/msService/QCS/inspectReport/inspectReport/downloadXls",
    queryExportPath: "/msService/QCS/inspectReport/inspectReport/manuInspReportList-query",
    sourceHintPaths: [
      "deploy/docker/postgres/init/065-business-view-runtime-json.sql",
      "deploy/docker/postgres/init/107-qcs-inspect-detail-tables.sql",
      "deploy/docker/postgres/init/119-qcs-inspect-report-edit-runtime-json.sql",
    ],
  },
  {
    id: "qcs-unqualified-deal",
    label: "QCS unqualified deal list",
    route: "/msService/QCS/unQlfDeal/unQlfDeal/manuUnQlfDealList",
    viewCode: "QCS_5.0.0.0_unQlfDeal_manuUnQlfDealList",
    downloadPath: "/msService/QCS/unQlfDeal/unQlfDeal/downloadXls",
    queryExportPath: "/msService/QCS/unQlfDeal/unQlfDeal/manuUnQlfDealList-query",
    sourceHintPaths: [
      "deploy/docker/postgres/init/065-business-view-runtime-json.sql",
      "deploy/docker/postgres/init/121-qcs-unqualified-deal-workflow-config.sql",
      "deploy/docker/postgres/init/122-qcs-unqualified-deal-runtime-compat.sql",
    ],
  },
  {
    id: "qcs-inspect-release",
    label: "QCS inspect release list",
    route: "/msService/QCS/inspectRelease/inspectRelease/manuInspReleaseList",
    viewCode: "QCS_5.0.0.0_inspectRelease_manuInspReleaseList",
    downloadPath: "/msService/QCS/inspectRelease/inspectRelease/downloadXls",
    queryExportPath: "/msService/QCS/inspectRelease/inspectRelease/manuInspReleaseList-query",
    sourceHintPaths: [
      "deploy/docker/postgres/init/065-business-view-runtime-json.sql",
      "deploy/docker/postgres/init/124-qcs-inspect-release-runtime-json.sql",
      "deploy/docker/postgres/init/125-qcs-inspect-release-action-compat.sql",
    ],
  },
];

const sourceSearchRoots = ["deploy/docker/assets/module-static", "deploy/docker/postgres/init", "backend/modules"];
const genericExportFrameworkPaths = [
  "backend/modules/com/supcon/supfusion/configuration/configuration-services-open-api/1.0.0-SNAPSHOT/templates/cui/cui_exportexcel.ftl",
  "backend/modules/com/supcon/supfusion/configuration/configuration-services-open-api/1.0.0-SNAPSHOT/templates/cui/cui_datagrid.ftl",
  "backend/modules/com/supcon/supfusion/configuration/configuration-services-open-api/1.0.0-SNAPSHOT/templates/datagrid/config-datagrid.ftl",
  "backend/modules/com/supcon/supfusion/configuration/configuration-services-open-api/1.0.0-SNAPSHOT/templates/view/config-datagrid.ftl",
];
const fileTextCache = new Map();

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

function parseJson(text, fallback = null) {
  try {
    return JSON.parse(text);
  } catch (_error) {
    return fallback;
  }
}

function sanitizeBody(text) {
  return String(text || "")
    .replace(/\u001b\[[0-9;]*m/g, "")
    .replace(/\s+/g, " ")
    .replace(/Authorization:\s*Bearer\s+[A-Za-z0-9._-]+/gi, "Authorization: <redacted>")
    .replace(/Bearer\s+[A-Za-z0-9._-]+/g, "Bearer <redacted>")
    .replace(/(suposTicket|SUPOS_TICKET|token)=([A-Za-z0-9._-]+)/g, "$1=<redacted>")
    .slice(0, 500);
}

function normalizeUrl(base, targetUrl) {
  return new URL(targetUrl, `${base}/`).toString();
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

async function readTextSafe(response) {
  const text = await response.text();
  return { text, json: parseJson(text) };
}

async function withTimeout(promise, timeoutMs, label) {
  let timeoutId = null;
  try {
    return await Promise.race([
      promise,
      new Promise((_resolve, reject) => {
        timeoutId = setTimeout(() => reject(new Error(`${label} timed out after ${timeoutMs}ms`)), timeoutMs);
      }),
    ]);
  } finally {
    if (timeoutId !== null) {
      clearTimeout(timeoutId);
    }
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
      timeout: apiTimeoutMs,
      headers: {
        Accept: "application/json, text/plain, */*",
        "Content-Type": "application/json;charset=UTF-8",
      },
    });
    const parsed = await readTextSafe(response);
    const ticket = response.ok() ? findTicket(parsed.json) : null;
    if (ticket) {
      return ticket;
    }
    errors.push({ status: response.status(), body: sanitizeBody(parsed.text) });
  }
  throw new Error(`Login failed for ${username}: ${JSON.stringify(errors)}`);
}

function extractPayloadJson(layoutJson) {
  const data = layoutJson && layoutJson.data;
  const candidates = [
    data && data.layoutJson,
    data && data.viewJson,
    data && data.view_json,
    data && data.json,
    data,
    layoutJson && layoutJson.layoutJson,
    layoutJson && layoutJson.viewJson,
  ];
  for (const candidate of candidates) {
    if (typeof candidate === "string") {
      const parsed = parseJson(candidate);
      if (parsed) {
        return parsed;
      }
    }
    if (candidate && typeof candidate === "object") {
      return candidate;
    }
  }
  return null;
}

function collectLayoutEvidence(root) {
  const exportButtonCandidates = [];
  const downloadXlsFields = [];
  const buttonTexts = [];
  const seen = new Set();

  function remember(collection, value) {
    const normalized = String(value || "").trim();
    if (!normalized || seen.has(normalized)) {
      return;
    }
    seen.add(normalized);
    collection.push(normalized);
  }

  function visit(value, parentKey = "") {
    if (!value || typeof value !== "object") {
      return;
    }
    if (Array.isArray(value)) {
      for (const item of value) {
        visit(item, parentKey);
      }
      return;
    }

    const isButtonObject = parentKey.toLowerCase().includes("button") || value.type === "button";
    const searchableParts = [];
    for (const [key, child] of Object.entries(value)) {
      if (typeof child === "string") {
        if (/downloadXls/i.test(key)) {
          remember(downloadXlsFields, child);
        }
        if (/^(name|text|title|label|namekey|caption|func|function|event|url|code|buttonCode|operateCode)$/i.test(key)) {
          searchableParts.push(child);
        }
      }
    }

    const searchable = searchableParts.join(" ");
    const hasRuntimeExportFlag =
      value.exportExcel === true ||
      value.isExportExcel === true ||
      value.exportExcel === "true" ||
      value.isExportExcel === "true" ||
      value.listProperty?.exportExcel === true ||
      value.listProperty?.isExportExcel === true ||
      value.listProperty?.exportExcel === "true" ||
      value.listProperty?.isExportExcel === "true";
    if (hasRuntimeExportFlag) {
      remember(
        exportButtonCandidates,
        `${value.DataGridCode || value.dataGridName || value.datagridName || value.code || parentKey || "datagrid"} exportExcel=true`
      );
    }
    if (isButtonObject && searchableParts.length) {
      remember(buttonTexts, searchable);
      if (/(导出|export)/i.test(searchable)) {
        remember(exportButtonCandidates, searchable);
      }
    }

    for (const [key, child] of Object.entries(value)) {
      visit(child, key);
    }
  }

  visit(root);
  return {
    exportButtonCandidates,
    downloadXlsFields,
    buttonTexts: buttonTexts.slice(0, 80),
  };
}

function detectMagic(buffer) {
  if (!buffer || buffer.length === 0) {
    return "EMPTY";
  }
  const first4 = buffer.subarray(0, 4).toString("hex").toLowerCase();
  if (first4 === "d0cf11e0") {
    return "OLE_XLS";
  }
  if (first4 === "504b0304") {
    return "ZIP_XLSX";
  }
  const textPrefix = buffer.subarray(0, 32).toString("utf8");
  if (/^\s*</.test(textPrefix)) {
    return "HTML";
  }
  if (/^\s*[{[]/.test(textPrefix)) {
    return "JSON";
  }
  return "UNKNOWN_BINARY";
}

function buildFileEvidence(source, buffer, headers = {}, extra = {}) {
  const magic = detectMagic(buffer);
  return {
    source,
    status: extra.status === undefined ? null : extra.status,
    method: extra.method || null,
    url: extra.url || null,
    suggestedFilename: extra.suggestedFilename || null,
    contentType: headers["content-type"] || headers["Content-Type"] || null,
    contentDisposition: headers["content-disposition"] || headers["Content-Disposition"] || null,
    bodySize: buffer ? buffer.length : 0,
    first16Hex: buffer && buffer.length > 0 ? buffer.subarray(0, 16).toString("hex") : "",
    magic,
    verifiedDataExport: Boolean(buffer && buffer.length > 0 && ["OLE_XLS", "ZIP_XLSX"].includes(magic)),
  };
}

function emptyExportClickEvidence(status, issue) {
  return {
    attempted: false,
    clickedSelector: null,
    clickedLabel: null,
    status,
    error: null,
    file: null,
    bodySize: 0,
    magic: "EMPTY",
    verifiedDataExport: false,
    issue,
  };
}

async function captureExportFile(page, target, trigger) {
  const timeoutMs = Math.min(apiTimeoutMs, 15000);
  const responseMatcher = (response) => {
    const url = response.url();
    const method = response.request().method();
    if (method !== "GET" && method !== "POST") {
      return false;
    }
    const targetMatch =
      url.includes(target.queryExportPath) ||
      url.includes(target.downloadPath) ||
      (/export|downloadXls|excel/i.test(url) && url.includes("/msService/"));
    if (!targetMatch) {
      return false;
    }
    const contentType = response.headers()["content-type"] || "";
    const disposition = response.headers()["content-disposition"] || "";
    return (
      response.status() >= 200 &&
      response.status() < 500 &&
      (/excel|spreadsheet|octet-stream|ms-excel|officedocument|application\/zip/i.test(contentType) ||
        /attachment|xls|xlsx/i.test(disposition) ||
        url.includes(target.queryExportPath) ||
        url.includes(target.downloadPath))
    );
  };

  const downloadPromise = page
    .waitForEvent("download", { timeout: timeoutMs })
    .then(async (download) => {
      const filePath = await download.path();
      const buffer = filePath ? fs.readFileSync(filePath) : Buffer.alloc(0);
      return buildFileEvidence(
        "browser-download",
        buffer,
        {},
        {
          suggestedFilename: download.suggestedFilename(),
        }
      );
    })
    .catch((error) => ({ source: "browser-download", error }));

  const responsePromise = page
    .waitForResponse(responseMatcher, { timeout: timeoutMs })
    .then(async (response) => {
      const body = await response.body();
      return buildFileEvidence("browser-response", body, response.headers(), {
        status: response.status(),
        method: response.request().method(),
        url: response.url(),
      });
    })
    .catch((error) => ({ source: "browser-response", error }));

  await trigger();
  const results = await Promise.all([downloadPromise, responsePromise]);
  const verified = results.find((result) => result && result.verifiedDataExport === true);
  if (verified) {
    return verified;
  }
  const nonEmptyFile = results.find((result) => result && !result.error && result.bodySize > 0);
  if (nonEmptyFile) {
    return nonEmptyFile;
  }
  const errors = results
    .filter((result) => result && result.error)
    .map((result) => `${result.source}: ${result.error.message || String(result.error)}`);
  throw new Error(errors.length ? errors.join("; ") : "No browser export download or file response was captured.");
}

async function firstVisibleLocator(page, selectors) {
  for (const selector of selectors) {
    const locator = page.locator(selector);
    const count = await locator.count().catch(() => 0);
    for (let index = 0; index < Math.min(count, 8); index += 1) {
      const candidate = locator.nth(index);
      if (await candidate.isVisible().catch(() => false)) {
        return { selector: `${selector} >> nth=${index}`, locator: candidate };
      }
    }
  }
  return null;
}

async function probeBrowserExportClick(page, target) {
  const candidates = [
    'button:has-text("导出")',
    'a:has-text("导出")',
    '[role="button"]:has-text("导出")',
    '.sup-btn:has-text("导出")',
    '.el-button:has-text("导出")',
    '.ant-btn:has-text("导出")',
    'span:has-text("导出")',
    '[title*="导出"]',
    '[aria-label*="导出"]',
    '[onclick*="ptExportExcel"]',
    '[onclick*="exportExcel"]',
    '[class*="excel"]',
    '[class*="export"]',
  ];
  const found = await firstVisibleLocator(page, candidates);
  if (!found) {
    return emptyExportClickEvidence("NOT_ATTEMPTED", "No visible export trigger could be clicked in the browser page.");
  }

  const clickedLabel = await found.locator.evaluate((element) => {
    const text = (element.innerText || element.textContent || "").trim();
    const title = (element.getAttribute("title") || "").trim();
    const aria = (element.getAttribute("aria-label") || "").trim();
    const className =
      typeof element.className === "string"
        ? element.className
        : element.className && typeof element.className.baseVal === "string"
          ? element.className.baseVal
          : "";
    return [text, title, aria, className].filter(Boolean).join(" ").replace(/\s+/g, " ").trim();
  }).catch(() => "");

  const result = {
    attempted: true,
    clickedSelector: found.selector,
    clickedLabel: clickedLabel.slice(0, 160),
    status: "NO_FILE_RESPONSE",
    error: null,
    file: null,
    bodySize: 0,
    magic: "EMPTY",
    verifiedDataExport: false,
    issue: "Click did not produce an accepted XLS/XLSX file response.",
  };

  try {
    const file = await captureExportFile(page, target, async () => {
      await found.locator.click({ timeout: 5000 });
      await page.waitForTimeout(800).catch(() => {});
      const confirm = await firstVisibleLocator(page, [
        'button:has-text("导出全部")',
        'button:has-text("导出")',
        'button:has-text("确定")',
        'button:has-text("确认")',
        '[role="button"]:has-text("导出全部")',
        '[role="button"]:has-text("确定")',
      ]);
      if (confirm && confirm.selector !== found.selector) {
        await confirm.locator.click({ timeout: 5000 }).catch(() => {});
      }
    });
    result.file = file;
    result.bodySize = file.bodySize;
    result.magic = file.magic;
    result.verifiedDataExport = file.verifiedDataExport;
    result.status = file.verifiedDataExport ? "FILE_VERIFIED" : "FILE_REJECTED";
    result.issue = file.verifiedDataExport
      ? null
      : `Browser export click returned non-accepted file content; magic=${file.magic}, bodySize=${file.bodySize}.`;
  } catch (error) {
    result.error = sanitizeBody(error && error.message ? error.message : String(error));
  }
  return result;
}

function findVisibleError(bodyText) {
  return String(bodyText || "")
    .split(/\r?\n/)
    .map((line) => line.trim())
    .find((line) => line && visibleErrorPattern.test(line));
}

function readSourceFile(relativePath) {
  const cached = fileTextCache.get(relativePath);
  if (cached !== undefined) {
    return cached;
  }
  const absolutePath = path.join(repoRoot, relativePath);
  if (!fs.existsSync(absolutePath)) {
    fileTextCache.set(relativePath, null);
    return null;
  }
  const text = fs.readFileSync(absolutePath, "utf8");
  fileTextCache.set(relativePath, text);
  return text;
}

function compactSnippet(line, needles) {
  const normalized = String(line || "").replace(/\s+/g, " ").trim();
  const indexCandidates = needles
    .map((needle) => normalized.indexOf(needle))
    .filter((index) => index >= 0);
  const index = indexCandidates.length > 0 ? Math.min(...indexCandidates) : 0;
  const start = Math.max(0, index - 110);
  const end = Math.min(normalized.length, index + 220);
  const prefix = start > 0 ? "..." : "";
  const suffix = end < normalized.length ? "..." : "";
  return `${prefix}${normalized.slice(start, end)}${suffix}`.slice(0, 360);
}

function scanSourceFile(relativePath, matcher, maxMatches = 8) {
  const text = readSourceFile(relativePath);
  if (text === null) {
    return { path: relativePath, exists: false, matches: [] };
  }
  const matches = [];
  const lines = text.split(/\r?\n/);
  for (let index = 0; index < lines.length; index += 1) {
    const line = lines[index];
    const matchedNeedles = matcher(line);
    if (!matchedNeedles || matchedNeedles.length === 0) {
      continue;
    }
    matches.push({
      path: relativePath,
      line: index + 1,
      matched: matchedNeedles.slice(0, 8),
      snippet: compactSnippet(line, matchedNeedles),
    });
    if (matches.length >= maxMatches) {
      break;
    }
  }
  return { path: relativePath, exists: true, matches };
}

function containsAny(line, needles) {
  return needles.filter((needle) => needle && line.includes(needle));
}

function buildSourceAudit(target, genericExportFramework) {
  const sourceHintPaths = target.sourceHintPaths || [];
  const targetNeedles = [target.viewCode, target.route, target.downloadPath].filter(Boolean);
  const downloadTemplateNeedles = [
    target.downloadPath,
    "downloadXls",
    "importMainXls",
    "下载模板",
    "下载导入模板",
    "导入Excel",
  ];
  const exportNeedles = ["exportExcel", "excelExport", "导出"];

  const sourceFiles = sourceHintPaths.map((relativePath) => ({
    path: relativePath,
    exists: readSourceFile(relativePath) !== null,
  }));
  const downloadTemplateMatches = [];
  const targetExportMatches = [];

  for (const relativePath of sourceHintPaths) {
    const downloadScan = scanSourceFile(
      relativePath,
      (line) => {
        const targetMatches = containsAny(line, targetNeedles);
        if (targetMatches.length === 0) {
          return [];
        }
        const templateMatches = containsAny(line, downloadTemplateNeedles);
        if (templateMatches.length === 0) {
          return [];
        }
        return [...new Set([...targetMatches, ...templateMatches])];
      },
      10
    );
    downloadTemplateMatches.push(...downloadScan.matches);

    const exportScan = scanSourceFile(
      relativePath,
      (line) => {
        const targetMatches = containsAny(line, targetNeedles);
        const exportMatches = containsAny(line, exportNeedles);
        if (targetMatches.length === 0 || exportMatches.length === 0) {
          return [];
        }
        return [...new Set([...targetMatches, ...exportMatches])];
      },
      10
    );
    targetExportMatches.push(...exportScan.matches);
  }

  let classification = "NO_TARGET_EXPORT_SOURCE_FOUND";
  if (targetExportMatches.length > 0) {
    classification = "TARGET_EXPORT_HOOK_FOUND_NEEDS_BROWSER_FILE_PROOF";
  } else if (downloadTemplateMatches.length > 0) {
    classification = "DOWNLOADXLS_IMPORT_TEMPLATE_OR_EMPTY_ENDPOINT_ONLY";
  }

  const issues = [];
  if (targetExportMatches.length === 0) {
    issues.push("No target-specific exportExcel/excelExport/导出 hook was found in static or runtime source hints.");
  }
  if (downloadTemplateMatches.length > 0) {
    issues.push("Target source/runtime hints expose downloadXls/importMainXls/import-template style entries, not an accepted list-data export action.");
  }
  if (genericExportFramework.available && targetExportMatches.length === 0) {
    issues.push("Generic platform export framework exists, but this production view does not enable a target-specific export hook.");
  }

  return {
    searchedRoots: sourceSearchRoots,
    sourceFiles,
    downloadTemplateMatches: downloadTemplateMatches.slice(0, 12),
    targetExportMatches: targetExportMatches.slice(0, 12),
    genericExportFrameworkAvailable: genericExportFramework.available,
    genericExportFrameworkMatches: genericExportFramework.matches.slice(0, 8),
    classification,
    issues,
  };
}

function scanGenericExportFramework() {
  const matches = [];
  for (const relativePath of genericExportFrameworkPaths) {
    const scan = scanSourceFile(
      relativePath,
      (line) => containsAny(line, ["exportExcel", "cui_exportexcel", "CUI.ptExportExcel", "导出"]),
      3
    );
    matches.push(...scan.matches);
  }
  return {
    available: matches.length > 0,
    paths: genericExportFrameworkPaths,
    matches: matches.slice(0, 12),
  };
}

async function probePage(context, target) {
  const page = await context.newPage();
  page.setDefaultTimeout(Math.min(pageTimeoutMs, 15000));
  const networkErrors = [];
  const consoleErrors = [];
  const pageErrors = [];
  const requestFailures = [];

  page.on("response", async (response) => {
    const type = response.request().resourceType();
    if (!["document", "xhr", "fetch", "script", "stylesheet"].includes(type)) {
      return;
    }
    const contentType = response.headers()["content-type"] || "";
    const htmlAssetFallback =
      ["script", "stylesheet"].includes(type) && response.status() < 400 && /text\/html/i.test(contentType);
    if (response.status() < 400 && !htmlAssetFallback) {
      return;
    }
    let body = "";
    try {
      body = (await response.text()).slice(0, 300);
    } catch (_error) {
      body = "";
    }
    networkErrors.push({
      status: response.status(),
      method: response.request().method(),
      type,
      url: response.url(),
      contentType,
      body: sanitizeBody(body),
    });
  });
  page.on("console", (message) => {
    if (message.type() === "error") {
      consoleErrors.push(message.text());
    }
  });
  page.on("pageerror", (error) => pageErrors.push(error.message));
  page.on("requestfailed", (requestItem) => {
    if (["document", "xhr", "fetch", "script", "stylesheet"].includes(requestItem.resourceType())) {
      requestFailures.push({
        method: requestItem.method(),
        type: requestItem.resourceType(),
        url: requestItem.url(),
        failure: requestItem.failure() && requestItem.failure().errorText,
      });
    }
  });

  try {
    let navigation = null;
    let navigationError = null;
    try {
      navigation = await page.goto(normalizeUrl(browserBaseUrl, target.route), {
        waitUntil: "domcontentloaded",
        timeout: pageTimeoutMs,
      });
      await page.waitForLoadState("networkidle", { timeout: Math.min(pageTimeoutMs, 12000) }).catch(() => {});
      await page.waitForTimeout(1200);
    } catch (error) {
      navigationError = sanitizeBody(error && error.message ? error.message : String(error));
    }

    const bodyText = await page.locator("body").innerText({ timeout: 5000 }).catch(() => "");
    const visibleError = findVisibleError(bodyText);
    const visibleTexts = await page
      .locator("button, a, [role=button], .sup-btn, .el-button, .ant-btn, span, i, [title], [aria-label]")
      .evaluateAll((elements) =>
        elements
          .map((element) => {
            const text = (element.innerText || element.textContent || "").trim();
            const title = (element.getAttribute("title") || "").trim();
            const aria = (element.getAttribute("aria-label") || "").trim();
            const value = (element.getAttribute("value") || "").trim();
            const onclick = (element.getAttribute("onclick") || "").trim();
            const className =
              typeof element.className === "string"
                ? element.className
                : element.className && typeof element.className.baseVal === "string"
                  ? element.className.baseVal
                  : "";
            const labels = [text, title, aria, value].filter(Boolean);
            if (!labels.length && /(excel|export)/i.test(`${className} ${onclick}`)) {
              labels.push(`class:${className || "export"}`.slice(0, 80));
            }
            return labels.join(" ").replace(/\s+/g, " ").trim();
          })
          .filter(Boolean)
          .filter((text, index, array) => array.indexOf(text) === index)
          .slice(0, 250)
      )
      .catch(() => []);

    const exportLabels = visibleTexts.filter((text) => /(导出|export)/i.test(text));
    const downloadLikeLabels = visibleTexts.filter((text) => /(下载|download|Excel|导入|import)/i.test(text));
    const exportClick = await probeBrowserExportClick(page, target);
    return {
      url: normalizeUrl(browserBaseUrl, target.route),
      navigationStatus: navigation ? navigation.status() : null,
      navigationError,
      visibleError: visibleError || null,
      consoleErrors,
      pageErrors,
      requestFailures,
      networkErrors,
      exportLabels,
      downloadLikeLabels: downloadLikeLabels.slice(0, 40),
      exportClick,
      ok:
        !navigationError &&
        (!navigation || navigation.status() < 400) &&
        !visibleError &&
        consoleErrors.length === 0 &&
        pageErrors.length === 0 &&
        requestFailures.length === 0 &&
        networkErrors.length === 0,
    };
  } finally {
    await page.close().catch(() => {});
  }
}

async function probeLayout(api, ticket, target) {
  const path = `/msService/baseService/view/layoutJson?viewCode=${encodeURIComponent(target.viewCode)}&isEs5=true`;
  try {
    const response = await api.get(`${baseUrl}${path}`, {
      timeout: apiTimeoutMs,
      headers: {
        Accept: "application/json, text/plain, */*",
        Authorization: `Bearer ${ticket}`,
      },
    });
    const parsed = await readTextSafe(response);
    const payload = extractPayloadJson(parsed.json);
    const layoutEvidence = collectLayoutEvidence(payload);
    return {
      method: "GET",
      path,
      status: response.status(),
      error: null,
      code: parsed.json && parsed.json.code !== undefined ? parsed.json.code : null,
      message: parsed.json && parsed.json.message ? String(parsed.json.message).slice(0, 200) : null,
      hasPayload: Boolean(payload),
      ...layoutEvidence,
    };
  } catch (error) {
    return {
      method: "GET",
      path,
      status: 0,
      error: sanitizeBody(error && error.message ? error.message : String(error)),
      code: null,
      message: null,
      hasPayload: false,
      exportButtonCandidates: [],
      downloadXlsFields: [],
      buttonTexts: [],
    };
  }
}

async function probeDownload(api, ticket, target) {
  const path = `${target.downloadPath}?viewCode=${encodeURIComponent(target.viewCode)}`;
  try {
    const response = await api.get(`${baseUrl}${path}`, {
      timeout: apiTimeoutMs,
      headers: {
        Accept: "*/*",
        Authorization: `Bearer ${ticket}`,
      },
    });
    const body = await withTimeout(response.body(), apiTimeoutMs, `${target.id} download body`);
    const magic = detectMagic(body);
    return {
      method: "GET",
      path,
      status: response.status(),
      error: null,
      contentType: response.headers()["content-type"] || null,
      contentDisposition: response.headers()["content-disposition"] || null,
      bodySize: body.length,
      first16Hex: body.subarray(0, 16).toString("hex"),
      magic,
      verification: null,
      verifiedDataExport: false,
      issue:
        body.length === 0
          ? "HTTP 200 returned an empty body, so this is not accepted as list-data export."
          : "Non-empty download was captured, but no visible/runtime export action proves this is list-data export rather than an import template.",
    };
  } catch (error) {
    return {
      method: "GET",
      path,
      status: 0,
      error: sanitizeBody(error && error.message ? error.message : String(error)),
      contentType: null,
      contentDisposition: null,
      bodySize: 0,
      first16Hex: "",
      magic: "EMPTY",
      verification: null,
      verifiedDataExport: false,
      issue: "downloadXls probe failed before a file response was received.",
    };
  }
}

function buildQueryExportPayload(target) {
  return {
    classifyCodes: "",
    customCondition: {},
    permissionCode: target.viewCode,
    pageNo: 1,
    paging: true,
    pageSize: 20,
    crossCompanyFlag: "true",
    exportFlag: true,
    exportAuxiliaryModelFlag: false,
    useForImportFlag: false,
    properties: [],
    datagridCode: target.viewCode,
    viewCode: target.viewCode,
  };
}

async function probeQueryExport(api, ticket, target) {
  const path = target.queryExportPath;
  const requestPayload = buildQueryExportPayload(target);
  try {
    const response = await api.post(`${baseUrl}${path}`, {
      data: requestPayload,
      timeout: apiTimeoutMs,
      headers: {
        Accept: "*/*",
        "Content-Type": "application/json;charset=UTF-8",
        Authorization: `Bearer ${ticket}`,
      },
    });
    const body = await withTimeout(response.body(), apiTimeoutMs, `${target.id} query export body`);
    const magic = detectMagic(body);
    const textSnippet = ["HTML", "JSON"].includes(magic) ? sanitizeBody(body.toString("utf8")) : "";
    const parsedJson = magic === "JSON" ? parseJson(body.toString("utf8")) : null;
    const isWorkbook = body.length > 0 && ["OLE_XLS", "ZIP_XLSX"].includes(magic);
    const verifiedDataExport = response.status() >= 200 && response.status() < 300 && isWorkbook;
    let issue = null;
    if (!verifiedDataExport) {
      if (response.status() >= 500) {
        issue = "query export returned a server error instead of a workbook.";
      } else if (magic === "JSON") {
        issue = "query export returned JSON, not a workbook file.";
      } else if (body.length === 0) {
        issue = "query export returned an empty response body.";
      } else {
        issue = `query export did not return an accepted workbook; magic=${magic}.`;
      }
    }
    return {
      method: "POST",
      path,
      requestPayload,
      status: response.status(),
      error: null,
      contentType: response.headers()["content-type"] || null,
      contentDisposition: response.headers()["content-disposition"] || null,
      bodySize: body.length,
      first16Hex: body.subarray(0, 16).toString("hex"),
      magic,
      jsonCode: parsedJson && parsedJson.code !== undefined ? parsedJson.code : null,
      jsonMessage: parsedJson && parsedJson.message ? String(parsedJson.message).slice(0, 200) : null,
      textSnippet,
      verifiedDataExport,
      issue,
    };
  } catch (error) {
    return {
      method: "POST",
      path,
      requestPayload,
      status: 0,
      error: sanitizeBody(error && error.message ? error.message : String(error)),
      contentType: null,
      contentDisposition: null,
      bodySize: 0,
      first16Hex: "",
      magic: "EMPTY",
      jsonCode: null,
      jsonMessage: null,
      textSnippet: "",
      verifiedDataExport: false,
      issue: "query export probe failed before a response body was received.",
    };
  }
}

function evaluateDownloadAcceptance(page, layout, download, queryExport, sourceAudit) {
  const exportClick = page.exportClick || emptyExportClickEvidence("NOT_ATTEMPTED", "No browser export click evidence was recorded.");
  const hasAcceptedWorkbook =
    (download.status >= 200 &&
      download.status < 300 &&
      download.bodySize > 0 &&
      ["OLE_XLS", "ZIP_XLSX"].includes(download.magic)) ||
    (queryExport.status >= 200 &&
      queryExport.status < 300 &&
      queryExport.bodySize > 0 &&
      ["OLE_XLS", "ZIP_XLSX"].includes(queryExport.magic)) ||
    exportClick.verifiedDataExport === true;
  const checks = [
    {
      name: "visibleExportAction",
      passed: page.exportLabels.length > 0,
      evidence: page.exportLabels.slice(0, 8),
    },
    {
      name: "runtimeExportAction",
      passed: layout.exportButtonCandidates.length > 0,
      evidence: layout.exportButtonCandidates.slice(0, 8),
    },
    {
      name: "targetExportSourceHook",
      passed: sourceAudit.classification === "TARGET_EXPORT_HOOK_FOUND_NEEDS_BROWSER_FILE_PROOF",
      evidence: sourceAudit.targetExportMatches.slice(0, 6).map((match) => `${match.path}:${match.line}`),
    },
    {
      name: "browserExportClick",
      passed: exportClick.verifiedDataExport === true,
      evidence: [
        `status=${exportClick.status}`,
        `selector=${exportClick.clickedSelector || ""}`,
        `label=${exportClick.clickedLabel || ""}`,
        `bodySize=${exportClick.bodySize || 0}`,
        `magic=${exportClick.magic || "EMPTY"}`,
      ],
    },
    {
      name: "successfulFileResponse",
      passed: hasAcceptedWorkbook,
      evidence: [
        `downloadXls.status=${download.status}`,
        `downloadXls.magic=${download.magic}`,
        `queryExport.status=${queryExport.status}`,
        `queryExport.magic=${queryExport.magic}`,
        `browserClick.status=${exportClick.status}`,
        `browserClick.magic=${exportClick.magic || "EMPTY"}`,
      ],
    },
    {
      name: "nonEmptyWorkbook",
      passed: hasAcceptedWorkbook,
      evidence: [
        `downloadXls.bodySize=${download.bodySize}`,
        `downloadXls.magic=${download.magic}`,
        `queryExport.bodySize=${queryExport.bodySize}`,
        `queryExport.magic=${queryExport.magic}`,
        `browserClick.bodySize=${exportClick.bodySize || 0}`,
        `browserClick.magic=${exportClick.magic || "EMPTY"}`,
      ],
    },
    {
      name: "backendQueryExport",
      passed: queryExport.verifiedDataExport === true,
      evidence: [
        `path=${queryExport.path}`,
        `status=${queryExport.status}`,
        `contentType=${queryExport.contentType || ""}`,
        `bodySize=${queryExport.bodySize}`,
        `magic=${queryExport.magic}`,
      ],
    },
  ];
  const failureReasons = checks
    .filter((check) => !check.passed)
    .map((check) => `${check.name} is not proven`);
  return {
    verifiedDataExport: failureReasons.length === 0,
    checks,
    failureReasons,
  };
}

function buildAcceptanceContract(target, item) {
  const currentGaps = [];
  const exportClick = item.page.exportClick || emptyExportClickEvidence("NOT_ATTEMPTED", "No browser export click evidence was recorded.");
  const hasAcceptedBackendWorkbook = item.queryExport.verifiedDataExport === true ||
    (item.download.status >= 200 &&
      item.download.status < 300 &&
      item.download.bodySize > 0 &&
      ["OLE_XLS", "ZIP_XLSX"].includes(item.download.magic));
  if (item.page.exportLabels.length === 0) {
    currentGaps.push("visibleExportAction missing from the real browser page.");
  }
  if (item.layout.exportButtonCandidates.length === 0) {
    currentGaps.push("runtimeExportAction missing from layoutJson metadata.");
  }
  if (item.sourceAudit.classification !== "TARGET_EXPORT_HOOK_FOUND_NEEDS_BROWSER_FILE_PROOF") {
    currentGaps.push(`targetExportSourceHook missing; current sourceAudit.classification=${item.sourceAudit.classification}.`);
  }
  if (exportClick.verifiedDataExport !== true) {
    currentGaps.push(`browserExportClick did not produce an accepted workbook; status=${exportClick.status}, magic=${exportClick.magic}.`);
  }
  if (item.download.bodySize > 0 && !["OLE_XLS", "ZIP_XLSX"].includes(item.download.magic)) {
    currentGaps.push(`downloadXls returned non-workbook content magic=${item.download.magic}.`);
  }
  if (!hasAcceptedBackendWorkbook) {
    if (item.download.bodySize === 0) {
      currentGaps.push("downloadXls returned an empty response body.");
    }
    if (item.queryExport.status < 200 || item.queryExport.status >= 300) {
      currentGaps.push(`query export endpoint ${target.queryExportPath} did not return 2xx; status=${item.queryExport.status}.`);
    } else if (!["OLE_XLS", "ZIP_XLSX"].includes(item.queryExport.magic)) {
      currentGaps.push(`query export endpoint ${target.queryExportPath} did not return a workbook; magic=${item.queryExport.magic}.`);
    }
  }
  if (item.download.bodySize > 0 && item.download.verifiedDataExport !== true && exportClick.verifiedDataExport !== true) {
    currentGaps.push("downloadXls returned a non-empty file, but browser/runtime/source evidence does not prove list-data export.");
  }

  return {
    targetId: target.id,
    persistence: "NOT_APPLICABLE: list export must not create or mutate business rows.",
    requiredFrontendRuntime: "Expose a visible 导出/export action on the target list page and in layoutJson runtime button metadata.",
    requiredBackend: `Return a non-empty XLS/XLSX list-data file from ${target.downloadPath} or ${target.queryExportPath}; import templates and JSON list responses are not accepted.`,
    requiredSourceEvidence: "Target source/runtime hints must contain exportExcel/excelExport/导出 hooks tied to this view or route.",
    minimumPassConditions: [
      "page.exportLabels contains a visible 导出/export action.",
      "page.exportClick.verifiedDataExport is true after a normal browser click.",
      "layout.exportButtonCandidates contains a runtime export action.",
      "sourceAudit.classification is TARGET_EXPORT_HOOK_FOUND_NEEDS_BROWSER_FILE_PROOF.",
      "download or queryExport returns a 2xx file response.",
      "download/queryExport/browser response bodySize is greater than 0.",
      "download/queryExport/browser response magic is OLE_XLS or ZIP_XLSX.",
      "queryExport.verifiedDataExport is true when the list query exportFlag path is the intended backend export path.",
      "download.verifiedDataExport is true.",
    ],
    currentGaps,
    recheckCommand:
      `make smoke-production-export-readiness ADP_BASE_URL=${baseUrl} ADP_BROWSER_BASE_URL=${browserBaseUrl} PRODUCTION_EXPORT_SMOKE_OUTPUT=metadata/production-export-readiness-smoke.json ADP_PAGE_TIMEOUT_MS=${pageTimeoutMs} ADP_API_TIMEOUT_MS=${apiTimeoutMs}`,
  };
}

function decideItemStatus(item) {
  if (item.download.verifiedDataExport === true) {
    return "READY";
  }
  if (!item.page.ok) {
    return "BLOCKED";
  }
  if (
    item.page.exportLabels.length > 0 ||
    item.layout.exportButtonCandidates.length > 0 ||
    item.sourceAudit.targetExportMatches.length > 0 ||
    item.queryExport.verifiedDataExport === true
  ) {
    return "ACTION_REQUIRED";
  }
  return "BLOCKED";
}

function buildProbeFailureEvidence(target, errorMessage, genericExportFramework) {
  const layoutPath = `/msService/baseService/view/layoutJson?viewCode=${encodeURIComponent(target.viewCode)}&isEs5=true`;
  const downloadPath = `${target.downloadPath}?viewCode=${encodeURIComponent(target.viewCode)}`;
  return {
    page: {
      url: normalizeUrl(browserBaseUrl, target.route),
      navigationStatus: null,
      navigationError: errorMessage,
      visibleError: null,
      consoleErrors: [],
      pageErrors: [errorMessage],
      requestFailures: [],
      networkErrors: [],
      exportLabels: [],
      downloadLikeLabels: [],
      exportClick: emptyExportClickEvidence("NOT_ATTEMPTED", "Target probe failed before browser export click evidence was collected."),
      ok: false,
    },
    layout: {
      method: "GET",
      path: layoutPath,
      status: 0,
      error: errorMessage,
      code: null,
      message: null,
      hasPayload: false,
      exportButtonCandidates: [],
      downloadXlsFields: [],
      buttonTexts: [],
    },
    download: {
      method: "GET",
      path: downloadPath,
      status: 0,
      error: errorMessage,
      contentType: null,
      contentDisposition: null,
      bodySize: 0,
      first16Hex: "",
      magic: "EMPTY",
      verification: null,
      verifiedDataExport: false,
      issue: "Target probe failed before downloadXls evidence was collected.",
    },
    queryExport: {
      method: "POST",
      path: target.queryExportPath,
      requestPayload: buildQueryExportPayload(target),
      status: 0,
      error: errorMessage,
      contentType: null,
      contentDisposition: null,
      bodySize: 0,
      first16Hex: "",
      magic: "EMPTY",
      jsonCode: null,
      jsonMessage: null,
      textSnippet: "",
      verifiedDataExport: false,
      issue: "Target probe failed before query export evidence was collected.",
    },
    sourceAudit: buildSourceAudit(target, genericExportFramework),
  };
}

async function main() {
  ensureDir(outputPath);

  const api = await request.newContext({ ignoreHTTPSErrors: true });
  const ticket = await login(api);
  const browser = await chromium.launch({ headless });
  const context = await browser.newContext({
    baseURL: browserBaseUrl,
    ignoreHTTPSErrors: true,
    viewport: { width: 1440, height: 960 },
    extraHTTPHeaders: {
      Authorization: `Bearer ${ticket}`,
    },
  });
  await context.addCookies([
    { name: "suposTicket", value: ticket, url: browserBaseUrl },
    { name: "SUPOS_TICKET", value: ticket, url: browserBaseUrl },
  ]);
  await context.addInitScript((token) => {
    window.localStorage.setItem("suposTicket", token);
    window.localStorage.setItem("SUPOS_TICKET", token);
    window.localStorage.setItem("token", token);
    window.sessionStorage.setItem("suposTicket", token);
    window.sessionStorage.setItem("SUPOS_TICKET", token);
    window.sessionStorage.setItem("token", token);
  }, ticket);

  try {
    const items = [];
    const genericExportFramework = scanGenericExportFramework();
    for (const target of targets) {
      let targetProbeError = null;
      let page = null;
      let layout = null;
      let download = null;
      let queryExport = null;
      let sourceAudit = null;
      try {
        page = await withTimeout(probePage(context, target), pageTimeoutMs + 15000, `${target.id} page probe`);
        layout = await withTimeout(probeLayout(api, ticket, target), apiTimeoutMs + 5000, `${target.id} layout probe`);
        download = await withTimeout(probeDownload(api, ticket, target), apiTimeoutMs + 5000, `${target.id} download probe`);
        queryExport = await withTimeout(probeQueryExport(api, ticket, target), apiTimeoutMs + 5000, `${target.id} query export probe`);
        sourceAudit = buildSourceAudit(target, genericExportFramework);
      } catch (error) {
        targetProbeError = sanitizeBody(error && error.message ? error.message : String(error));
        ({ page, layout, download, queryExport, sourceAudit } = buildProbeFailureEvidence(target, targetProbeError, genericExportFramework));
      }
      const downloadVerification = evaluateDownloadAcceptance(page, layout, download, queryExport, sourceAudit);
      download.verification = downloadVerification;
      download.verifiedDataExport = downloadVerification.verifiedDataExport;
      if (download.verifiedDataExport) {
        download.issue = null;
      }
      const item = {
        id: target.id,
        label: target.label,
        route: target.route,
        viewCode: target.viewCode,
        page,
        layout,
        download,
        queryExport,
        sourceAudit,
        acceptanceContract: null,
        status: "BLOCKED",
        conclusion:
          "No accepted list-data export was proven. Persistence is NOT_APPLICABLE for file export; acceptance requires a real browser file response with non-empty list data.",
        issues: [],
      };
      item.status = decideItemStatus(item);
      item.acceptanceContract = buildAcceptanceContract(target, item);
      if (targetProbeError) {
        item.issues.push("Target probe failed before full evidence collection: " + targetProbeError);
      }
      if (!page.ok) {
        item.issues.push("The production list page is not cleanly reachable, so export cannot be accepted.");
      }
      if (page.exportLabels.length === 0 && download.verifiedDataExport !== true) {
        item.issues.push("No visible export action label was found on the page.");
      }
      if (layout.exportButtonCandidates.length === 0) {
        item.issues.push("No runtime layout button candidate for export was found.");
      }
      if (layout.error) {
        item.issues.push("layoutJson probe failed: " + layout.error);
      }
      if (download.error) {
        item.issues.push("downloadXls probe failed: " + download.error);
      }
      if (download.bodySize === 0 && download.verifiedDataExport !== true) {
        item.issues.push("Direct downloadXls probe returned an empty body.");
      }
      if (download.bodySize > 0 && page.exportLabels.length === 0 && download.verifiedDataExport !== true) {
        item.issues.push("A non-empty download exists, but no visible export action proves it is list-data export.");
      }
      if (queryExport.error) {
        item.issues.push("query export probe failed: " + queryExport.error);
      }
      if (queryExport.issue) {
        item.issues.push(queryExport.issue);
      }
      for (const reason of downloadVerification.failureReasons) {
        item.issues.push("Export acceptance check failed: " + reason);
      }
      item.issues.push(...sourceAudit.issues);
      items.push(item);
      console.log(
        `${item.status} ${target.id} page=${page.ok ? "OK" : "FAIL"} downloadSize=${download.bodySize} queryExport=${queryExport.status}/${queryExport.magic}/${queryExport.bodySize}`
      );
    }

    const summary = {
      status: items.every((item) => item.status === "READY") ? "READY" : "BLOCKED",
      targets: items.length,
      pagePass: items.filter((item) => item.page.ok).length,
      visibleExportActions: items.filter((item) => item.page.exportLabels.length > 0).length,
      runtimeExportActions: items.filter((item) => item.layout.exportButtonCandidates.length > 0).length,
      nonEmptyDownloads: items.filter((item) => item.download.bodySize > 0).length,
      backendQueryExportWorkbooks: items.filter((item) => item.queryExport.verifiedDataExport === true).length,
      backendQueryExportErrors: items.filter((item) => item.queryExport.status >= 500 || item.queryExport.status === 0).length,
      verifiedDataExports: items.filter((item) => item.download.verifiedDataExport === true).length,
      ready: items.filter((item) => item.status === "READY").length,
      actionRequired: items.filter((item) => item.status === "ACTION_REQUIRED").length,
      blocked: items.filter((item) => item.status === "BLOCKED").length,
    };

    const report = {
      schemaVersion: 1,
      generatedAt: new Date().toISOString(),
      repoCommit: getRepoCommit(),
      database: "PostgreSQL",
      module: "production",
      caseId: "PROD-023",
      status: summary.status,
      apiBaseUrl: baseUrl,
      browserBaseUrl,
      target: {
        baseUrl,
        browserBaseUrl,
        username,
        pageTimeoutMs,
        apiTimeoutMs,
      },
      summary,
      items,
      evidence: {
        method:
          "Authenticated real browser page probe, normal browser export-click/download probe, authenticated layoutJson runtime metadata probe, authenticated downloadXls/query-export file-response probe, and target source/runtime configuration audit.",
        persistence: "NOT_APPLICABLE: file export does not write business data; acceptance checks browser file response instead.",
        sourceAudit:
          "Generic export framework files are scanned separately from target production views; a generic export template is not accepted unless the target page/runtime enables an export hook and the browser file response proves list data.",
        genericExportFramework,
        secretHandling: "Login token and credentials are not written to this report.",
      },
    };
    fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`, "utf8");
    console.log(JSON.stringify(summary, null, 2));
    console.log(`Production export readiness smoke report: ${outputPath}`);
  } finally {
    await browser.close();
    await api.dispose();
  }
}

main().catch((error) => {
  console.error(error && error.stack ? error.stack : String(error));
  process.exitCode = 1;
});
