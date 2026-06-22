#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { chromium, request } = require("playwright");

const baseUrl = (process.env.ADP_BASE_URL || "http://100.99.133.43:18080").replace(/\/+$/, "");
const browserBaseUrl = (process.env.ADP_BROWSER_BASE_URL || baseUrl).replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD || "123456";
const headless = process.env.ADP_HEADLESS !== "false";
const screenshotMode = process.env.ADP_SCREENSHOTS || "failures";
const menuLimit = Number.parseInt(process.env.ADP_MENU_LIMIT || "", 10);
const apiTimeoutMs = Number(process.env.ADP_API_TIMEOUT_MS || 90000);
const apiRetries = Number(process.env.ADP_API_RETRIES || 3);
const pageTimeoutMs = Number(process.env.ADP_PAGE_TIMEOUT_MS || 90000);
const outputDir =
  process.env.ADP_OUTPUT_DIR ||
  path.join("/tmp", `adp-menu-smoke-${new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14)}`);

const visibleErrorPattern =
  /(系统错误|系统异常|发生未知异常|404_NOT_FOUND|500 INTERNAL|\bHTTP\s*(404|500)\b|\b(404|500)\s+(Not Found|Internal Server Error)\b|Caused by:|\b[\w.]+Exception(?::|\s+at\b)|Invalid bound statement|relation .* does not exist)/i;

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true });
}

function safeName(value) {
  return String(value || "page")
    .replace(/[^a-zA-Z0-9._-]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .slice(0, 120);
}

function normalizeUrl(targetUrl) {
  return new URL(targetUrl, `${browserBaseUrl}/`).toString();
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

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function isTransientApiError(error) {
  return /Timeout|ETIMEDOUT|ECONNRESET|ECONNREFUSED|EHOSTUNREACH|ENETUNREACH|socket hang up|network/i.test(
    error && error.message ? error.message : String(error)
  );
}

async function withApiRetries(label, operation) {
  const errors = [];
  for (let attempt = 1; attempt <= apiRetries; attempt += 1) {
    try {
      return await operation();
    } catch (error) {
      errors.push(error && error.message ? error.message : String(error));
      if (attempt >= apiRetries || !isTransientApiError(error)) {
        throw error;
      }
      await sleep(Math.min(1000 * attempt, 5000));
    }
  }
  throw new Error(`${label} failed after ${apiRetries} attempts: ${errors.join(" | ")}`);
}

async function login(api) {
  const attempts = [
    { userName: username, password, clientId: "pc_dt" },
    { username, password, clientId: "pc_dt" },
  ];

  const errors = [];
  for (const body of attempts) {
    const response = await withApiRetries("auth login", () =>
      api.post(`${baseUrl}/inter-api/auth/login`, {
        data: body,
        headers: {
          Accept: "application/json, text/plain, */*",
          "Content-Type": "application/json;charset=UTF-8",
        },
      })
    );
    const parsed = await readJsonSafe(response);
    const ticket = response.ok() ? findTicket(parsed.json) : null;
    if (ticket) {
      return { ticket, loginPayload: parsed.json };
    }
    errors.push({
      status: response.status(),
      body: parsed.text.slice(0, 500),
    });
  }

  throw new Error(`Login failed for ${username}: ${JSON.stringify(errors)}`);
}

function pickArray(...values) {
  return values.find((value) => Array.isArray(value));
}

function pickMenuRoots(payload) {
  return (
    pickArray(
      payload,
      payload && payload.list,
      payload && payload.data,
      payload && payload.data && payload.data.list,
      payload && payload.data && payload.data.menus,
      payload && payload.result,
      payload && payload.result && payload.result.list,
      payload && payload.result && payload.result.menus
    ) || []
  );
}

function getChildren(node) {
  return (
    pickArray(
      node && node.children,
      node && node.childrens,
      node && node.childMenus,
      node && node.subMenus,
      node && node.nodes,
      node && node.menuList
    ) || []
  );
}

function getLabel(node) {
  return (
    node.menuName ||
    node.name ||
    node.title ||
    node.menuNameZh ||
    node.menuNameEn ||
    node.label ||
    node.code ||
    node.id ||
    "unnamed"
  );
}

function getTargetUrl(node) {
  return node.url || node.path || node.href || node.menuUrl || node.targetUrl || node.routeUrl || "";
}

function flattenMenus(payload) {
  const roots = pickMenuRoots(payload);
  const targets = [];

  function visit(node, parents) {
    if (!node || typeof node !== "object") {
      return;
    }
    const label = getLabel(node);
    const nextParents = parents.concat(label);
    const targetUrl = getTargetUrl(node);
    if (typeof targetUrl === "string" && targetUrl.trim() && !/^javascript:/i.test(targetUrl.trim())) {
      targets.push({
        label: nextParents.join(" > "),
        url: targetUrl.trim(),
      });
    }
    for (const child of getChildren(node)) {
      visit(child, nextParents);
    }
  }

  for (const root of roots) {
    visit(root, []);
  }

  const seen = new Set();
  return targets.filter((target) => {
    const key = `${target.label}\n${target.url}`;
    if (seen.has(key)) {
      return false;
    }
    seen.add(key);
    return true;
  });
}

function applyMenuLimit(targets) {
  if (!Number.isFinite(menuLimit) || menuLimit <= 0) {
    return targets;
  }
  return targets.slice(0, menuLimit);
}

async function fetchMenus(api, ticket) {
  const response = await withApiRetries("menus currentUser", () =>
    api.get(`${baseUrl}/inter-api/rbac/v1/menus/currentUser`, {
      headers: {
        Accept: "application/json, text/plain, */*",
        Authorization: `Bearer ${ticket}`,
        Cookie: `suposTicket=${encodeURIComponent(ticket)}`,
        langu_code: "zh_CN",
      },
    })
  );
  const parsed = await readJsonSafe(response);
  if (!response.ok()) {
    throw new Error(`Menu API failed with ${response.status()}: ${parsed.text.slice(0, 500)}`);
  }
  const targets = flattenMenus(parsed.json);
  if (!targets.length) {
    throw new Error(`Menu API returned no navigable menu targets: ${parsed.text.slice(0, 500)}`);
  }
  return targets;
}

function findVisibleError(bodyText) {
  return String(bodyText || "")
    .split(/\r?\n/)
    .map((line) => line.trim())
    .find((line) => line && visibleErrorPattern.test(line));
}

async function smokePage(context, target, index) {
  const page = await context.newPage();
  page.setDefaultTimeout(pageTimeoutMs);
  page.setDefaultNavigationTimeout(pageTimeoutMs);
  const networkErrors = [];
  const consoleErrors = [];
  const pageErrors = [];
  const requestFailures = [];

  page.on("response", async (response) => {
    const type = response.request().resourceType();
    if (!["document", "xhr", "fetch"].includes(type) || response.status() < 400) {
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
      body,
    });
  });

  page.on("console", (message) => {
    if (message.type() === "error") {
      consoleErrors.push(message.text());
    }
  });

  page.on("pageerror", (error) => {
    pageErrors.push(error.message);
  });

  page.on("requestfailed", (requestItem) => {
    if (["document", "xhr", "fetch"].includes(requestItem.resourceType())) {
      requestFailures.push({
        method: requestItem.method(),
        type: requestItem.resourceType(),
        url: requestItem.url(),
        failure: requestItem.failure() && requestItem.failure().errorText,
      });
    }
  });

  const url = normalizeUrl(target.url);
  let navigation = null;
  let navigationError = null;
  try {
    navigation = await page.goto(url, { waitUntil: "commit", timeout: pageTimeoutMs });
    await page.waitForLoadState("domcontentloaded", { timeout: 30000 }).catch(() => {});
    await page.waitForLoadState("networkidle", { timeout: 8000 }).catch(() => {});
    await page.waitForTimeout(1500);
  } catch (error) {
    navigationError = error.message;
  }

  const bodyText = await page.locator("body").innerText({ timeout: 5000 }).catch(() => "");
  const visibleError = findVisibleError(bodyText);
  const navigationStatus = navigation ? navigation.status() : null;
  const failed =
    Boolean(navigationError) ||
    (navigationStatus !== null && navigationStatus >= 400) ||
    networkErrors.length > 0 ||
    consoleErrors.length > 0 ||
    pageErrors.length > 0 ||
    requestFailures.length > 0 ||
    Boolean(visibleError);
  const capturedNetworkErrors = networkErrors.slice();
  const capturedConsoleErrors = consoleErrors.slice();
  const capturedPageErrors = pageErrors.slice();
  const capturedRequestFailures = requestFailures.slice();

  let screenshot = null;
  if (screenshotMode === "all" || (screenshotMode === "failures" && failed)) {
    screenshot = path.join(outputDir, `${String(index + 1).padStart(2, "0")}-${safeName(target.label)}.png`);
    await page.screenshot({ path: screenshot, fullPage: true, timeout: 10000 }).catch(() => {
      screenshot = null;
    });
  }

  await page.close();

  return {
    label: target.label,
    url: target.url,
    resolvedUrl: url,
    ok: !failed,
    navigationStatus,
    navigationError,
    visibleError: visibleError || null,
    networkErrors: capturedNetworkErrors,
    consoleErrors: capturedConsoleErrors,
    pageErrors: capturedPageErrors,
    requestFailures: capturedRequestFailures,
    screenshot,
  };
}

async function main() {
  ensureDir(outputDir);

  const api = await request.newContext({ ignoreHTTPSErrors: true, timeout: apiTimeoutMs });
  const { ticket, loginPayload } = await login(api);
  const discoveredTargets = await fetchMenus(api, ticket);
  const targets = applyMenuLimit(discoveredTargets);
  await api.dispose();

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
  await context.addInitScript(({ token, loginPayload: loginPayloadValue }) => {
    window.localStorage.setItem("suposTicket", token);
    window.localStorage.setItem("SUPOS_TICKET", token);
    window.localStorage.setItem("token", token);
    window.localStorage.setItem("ticket", token);
    window.sessionStorage.setItem("suposTicket", token);
    window.sessionStorage.setItem("SUPOS_TICKET", token);
    window.sessionStorage.setItem("token", token);
    window.sessionStorage.setItem("ticket", token);
    if (loginPayloadValue) {
      window.localStorage.setItem("loginMsg", JSON.stringify(loginPayloadValue));
      window.sessionStorage.setItem("loginMsg", JSON.stringify(loginPayloadValue));
    }
    window.localStorage.setItem("language", "zh_CN");
    window.localStorage.setItem("langu_code", "zh_CN");
    window.localStorage.setItem("locale", "zh-cn");
    window.sessionStorage.setItem("language", "zh_CN");
    window.sessionStorage.setItem("langu_code", "zh_CN");
    window.sessionStorage.setItem("locale", "zh-cn");
  }, { token: ticket, loginPayload });

  const results = [];
  for (const [index, target] of targets.entries()) {
    const result = await smokePage(context, target, index);
    results.push(result);
    console.log(`${result.ok ? "OK" : "FAIL"} ${index + 1}/${targets.length} ${target.label} ${target.url}`);
  }

  await browser.close();

  const failed = results.filter((result) => !result.ok);
  const report = {
    generatedAt: new Date().toISOString(),
    baseUrl,
    browserBaseUrl,
    username,
    discoveredTotal: discoveredTargets.length,
    menuLimit: Number.isFinite(menuLimit) && menuLimit > 0 ? menuLimit : null,
    total: results.length,
    passed: results.length - failed.length,
    failed: failed.length,
    results,
  };
  const reportPath = path.join(outputDir, "menu-smoke-results.json");
  fs.writeFileSync(reportPath, `${JSON.stringify(report, null, 2)}\n`);

  console.log(`SUMMARY total=${report.total} passed=${report.passed} failed=${report.failed}`);
  console.log(`REPORT ${reportPath}`);

  if (failed.length) {
    process.exitCode = 1;
  }
}

main().catch((error) => {
  console.error(error && error.stack ? error.stack : error);
  process.exit(1);
});
