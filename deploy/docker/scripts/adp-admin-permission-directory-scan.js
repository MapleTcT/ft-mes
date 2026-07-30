#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { chromium, request } = require("playwright");

const baseUrl = (process.env.ADP_BASE_URL || "http://10.11.100.17:18080").replace(/\/+$/, "");
const browserBaseUrl = (process.env.ADP_BROWSER_BASE_URL || baseUrl).replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD || "123456";
const roleCode = process.env.ADP_RBAC_AUTHORITY_ROLE_CODE || "systemRole";
const userId = process.env.ADP_RBAC_AUTHORITY_USER_ID || "1";
const headless = process.env.ADP_HEADLESS !== "false";
const screenshotMode = process.env.ADP_SCREENSHOTS || "failures";
const pageConcurrency = positiveInteger(process.env.ADP_SCAN_CONCURRENCY, 6);
const permissionConcurrency = positiveInteger(process.env.ADP_PERMISSION_CONCURRENCY, 12);
const pageTimeoutMs = positiveInteger(process.env.ADP_PAGE_TIMEOUT_MS, 45000);
const pageSettleMs = positiveInteger(process.env.ADP_PAGE_SETTLE_MS, 1800);
const networkIdleTimeoutMs = positiveInteger(process.env.ADP_NETWORK_IDLE_TIMEOUT_MS, 8000);
const blankRetryMs = positiveInteger(process.env.ADP_BLANK_RETRY_MS, 2500);
const apiTimeoutMs = positiveInteger(process.env.ADP_API_TIMEOUT_MS, 60000);
const menuLimit = Number.parseInt(process.env.ADP_MENU_LIMIT || "", 10);
const menuPatternText = process.env.ADP_MENU_PATTERN || "";
const scanPages = process.env.ADP_SCAN_PAGES !== "false";
const scanPermissions = process.env.ADP_SCAN_PERMISSIONS !== "false";
const outputDir =
  process.env.ADP_OUTPUT_DIR ||
  path.join("/tmp", `adp-admin-permission-scan-${new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14)}`);

const visibleErrorPattern =
  /(数据库操作异常|系统错误|系统异常|发生未知异常|SQLGrammarException|BadSqlGrammarException|could not extract ResultSet|Invalid bound statement|relation .* does not exist|column .* does not exist|401 Authorization Required|401 Unauthorized|403 Forbidden|404_NOT_FOUND|500 INTERNAL|\bHTTP\s*(401|403|404|500)\b|Caused by:|\b[\w.]+Exception(?::|\s+at\b))/i;
const untranslatedPattern =
  /\b(?:Button\.text\.[\w.]+|ec\.common\.[\w.]+|[\w]+(?:\.[\w]+){1,}\.(?:random|randon)\d+)\b/gi;
const benignConsolePattern = /请将参数放在数组里!?/;

function positiveInteger(value, fallback) {
  const parsed = Number.parseInt(value || "", 10);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true });
}

function safeName(value) {
  return String(value || "page")
    .replace(/[^a-zA-Z0-9._-]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .slice(0, 120);
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function readJsonSafe(response) {
  const text = await response.text();
  try {
    return { json: JSON.parse(text), text };
  } catch (_error) {
    return { json: null, text };
  }
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

async function login(api) {
  const attempts = [
    { userName: username, password, clientId: "pc_dt" },
    { username, password, clientId: "pc_dt" },
  ];
  const failures = [];

  for (const data of attempts) {
    const response = await api.post(`${baseUrl}/inter-api/auth/login`, {
      data,
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
    failures.push({ status: response.status(), body: parsed.text.slice(0, 300) });
  }

  throw new Error(`Login failed for ${username}: ${JSON.stringify(failures)}`);
}

function authHeaders(ticket) {
  return {
    Accept: "application/json, text/plain, */*",
    Authorization: `Bearer ${ticket}`,
    Cookie: `suposTicket=${encodeURIComponent(ticket)}`,
    langu_code: "zh_CN",
  };
}

async function fetchJson(api, ticket, pathname) {
  const response = await api.get(`${baseUrl}${pathname}`, {
    headers: authHeaders(ticket),
  });
  const parsed = await readJsonSafe(response);
  return {
    path: pathname,
    status: response.status(),
    ok: response.status() < 400 && !visibleErrorPattern.test(parsed.text || ""),
    json: parsed.json,
    bodySnippet: parsed.text.slice(0, 600),
  };
}

function pickArray(...values) {
  return values.find((value) => Array.isArray(value));
}

function pickRoots(payload) {
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

function getCode(node) {
  return node.code || node.menuInfoCode || node.menuCode || "";
}

function getId(node) {
  return node.id || node.menuInfoId || null;
}

function getTargetUrl(node) {
  return node.url || node.path || node.href || node.menuUrl || node.targetUrl || node.routeUrl || "";
}

function isNavigableUrl(targetUrl) {
  const value = String(targetUrl || "").trim();
  return Boolean(value) && value !== "#" && value !== "/" && !/^javascript:/i.test(value);
}

function flattenMenus(payload, source) {
  const rows = [];

  function visit(node, parents, parentCodes, depth) {
    if (!node || typeof node !== "object") {
      return;
    }
    const label = String(getLabel(node));
    const code = String(getCode(node) || "");
    const targetUrl = String(getTargetUrl(node) || "").trim();
    const labelPath = parents.concat(label);
    const codePath = parentCodes.concat(code).filter(Boolean);
    rows.push({
      source,
      id: getId(node),
      code,
      label,
      labelPath: labelPath.join(" > "),
      codePath: codePath.join(" > "),
      depth,
      parentId: node.parentId || node.parentMenuId || null,
      moduleCode: node.moduleCode || node.appCode || node.systemCode || "",
      menuType: node.menuType || node.type || node.nodeType || "",
      sort: node.sort || node.sortNo || node.orderNo || null,
      url: targetUrl,
      navigable: isNavigableUrl(targetUrl),
    });
    for (const child of getChildren(node)) {
      visit(child, labelPath, codePath, depth + 1);
    }
  }

  for (const root of pickRoots(payload)) {
    visit(root, [], [], 0);
  }
  return rows;
}

function deduplicateNavigableMenus(rows) {
  const seen = new Set();
  return rows.filter((row) => {
    if (!row.navigable) {
      return false;
    }
    const key = `${row.labelPath}\n${row.url}`;
    if (seen.has(key)) {
      return false;
    }
    seen.add(key);
    return true;
  });
}

function permissionCounts(payload) {
  const data = (payload && payload.data) || payload || {};
  const nested = (data && data.data) || {};
  const assign = pickArray(data.assign, nested.assign) || [];
  const unassign = pickArray(data.unassign, nested.unassign) || [];
  const list = pickArray(data.list, nested.list) || [];
  return {
    assigned: assign.length,
    unassigned: unassign.length,
    listed: list.length,
  };
}

function permissionDetails(payload, key) {
  const data = payload && payload.data ? payload.data : payload;
  const nested = data && data.data ? data.data : {};
  const entries = pickArray(data && data[key], nested && nested[key]) || [];
  return entries.map((entry) => {
    const operation = entry && entry.op ? entry.op : entry;
    return {
      id: operation && operation.id ? operation.id : null,
      code: operation && operation.code ? operation.code : null,
      name:
        (operation && (operation.nameDisplay || operation.name)) ||
        (entry && (entry.nameDisplay || entry.name)) ||
        null,
      url: operation && operation.url ? operation.url : null,
    };
  });
}

async function mapWithConcurrency(items, concurrency, mapper) {
  const results = new Array(items.length);
  let nextIndex = 0;

  async function worker() {
    while (true) {
      const index = nextIndex;
      nextIndex += 1;
      if (index >= items.length) {
        return;
      }
      results[index] = await mapper(items[index], index);
    }
  }

  await Promise.all(Array.from({ length: Math.min(concurrency, items.length || 1) }, worker));
  return results;
}

async function scanPermissionNode(api, ticket, roleId, menu, index, total) {
  const emptyResult = {
    menuId: menu.id,
    menuCode: menu.code,
    menuPath: menu.labelPath,
    menuUrl: menu.url,
    navigable: menu.navigable,
    role: null,
    user: null,
    effectiveAssigned: 0,
    effectiveActionGap: 0,
    ok: true,
  };
  if (!menu.id) {
    return emptyResult;
  }

  const rolePath = `/inter-api/rbac/v1/rolePermissions?menuId=${encodeURIComponent(
    menu.id
  )}&roleId=${encodeURIComponent(roleId)}`;
  const userPath = `/inter-api/rbac/v1/userPermissions?menuId=${encodeURIComponent(
    menu.id
  )}&userId=${encodeURIComponent(userId)}`;

  const [roleResult, userResult] = await Promise.all([
    fetchJson(api, ticket, rolePath).catch((error) => ({
      path: rolePath,
      status: null,
      ok: false,
      bodySnippet: error.message,
    })),
    fetchJson(api, ticket, userPath).catch((error) => ({
      path: userPath,
      status: null,
      ok: false,
      bodySnippet: error.message,
    })),
  ]);

  const result = {
    ...emptyResult,
    role: {
      status: roleResult.status,
      ok: roleResult.ok,
      counts: roleResult.json ? permissionCounts(roleResult.json) : null,
      assigned: roleResult.json ? permissionDetails(roleResult.json, "assign") : [],
      unassigned: roleResult.json ? permissionDetails(roleResult.json, "unassign") : [],
      error: roleResult.ok ? null : roleResult.bodySnippet,
    },
    user: {
      status: userResult.status,
      ok: userResult.ok,
      counts: userResult.json ? permissionCounts(userResult.json) : null,
      assigned: userResult.json ? permissionDetails(userResult.json, "assign") : [],
      unassigned: userResult.json ? permissionDetails(userResult.json, "unassign") : [],
      error: userResult.ok ? null : userResult.bodySnippet,
    },
    ok: roleResult.ok && userResult.ok,
  };
  if (result.ok) {
    result.effectiveAssigned =
      ((result.role.counts && result.role.counts.assigned) || 0) +
      ((result.user.counts && result.user.counts.assigned) || 0);
    result.effectiveActionGap = Math.max(
      ((result.role.counts && result.role.counts.unassigned) || 0) -
        ((result.user.counts && result.user.counts.assigned) || 0),
      0
    );
  }
  console.log(`${result.ok ? "PERM_OK" : "PERM_FAIL"} ${index + 1}/${total} ${menu.labelPath}`);
  return result;
}

function rewriteToBrowserOrigin(targetUrl) {
  const resolved = new URL(targetUrl, `${browserBaseUrl}/`);
  const apiOrigin = new URL(baseUrl).origin;
  if (resolved.origin === apiOrigin) {
    return `${browserBaseUrl}${resolved.pathname}${resolved.search}${resolved.hash}`;
  }
  return resolved.toString();
}

function findVisibleError(bodyText) {
  return String(bodyText || "")
    .split(/\r?\n/)
    .map((line) => line.trim())
    .find((line) => line && visibleErrorPattern.test(line));
}

function findUntranslatedKeys(bodyText) {
  const matches = String(bodyText || "").match(untranslatedPattern) || [];
  return [...new Set(matches)].slice(0, 30);
}

async function visibleControls(page) {
  return page
    .locator("button, a, [role=button], [onclick], input[type=button], input[type=submit]")
    .evaluateAll((elements) => {
      const values = [];
      for (const element of elements) {
        const style = window.getComputedStyle(element);
        const rect = element.getBoundingClientRect();
        if (
          style.display === "none" ||
          style.visibility === "hidden" ||
          Number(style.opacity) === 0 ||
          rect.width <= 0 ||
          rect.height <= 0
        ) {
          continue;
        }
        const text =
          element.innerText ||
          element.value ||
          element.getAttribute("title") ||
          element.getAttribute("aria-label") ||
          "";
        const normalized = String(text).replace(/\s+/g, " ").trim();
        if (normalized) {
          values.push(normalized.slice(0, 120));
        }
      }
      return [...new Set(values)].slice(0, 80);
    })
    .catch(() => []);
}

async function smokePage(context, menu, index, total) {
  const page = await context.newPage();
  page.setDefaultTimeout(pageTimeoutMs);
  page.setDefaultNavigationTimeout(pageTimeoutMs);
  const networkErrors = [];
  const consoleErrors = [];
  const consoleWarnings = [];
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
      body = (await response.text()).slice(0, 500);
    } catch (_error) {
      body = "";
    }
    networkErrors.push({
      status: response.status(),
      method: response.request().method(),
      type,
      url: response.url(),
      contentType,
      body,
    });
  });

  page.on("console", (message) => {
    if (message.type() === "error") {
      const value = message.text();
      if (benignConsolePattern.test(value)) {
        consoleWarnings.push(value);
      } else {
        consoleErrors.push(value);
      }
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

  const url = rewriteToBrowserOrigin(menu.url);
  let navigation = null;
  let navigationError = null;
  try {
    navigation = await page.goto(url, { waitUntil: "commit", timeout: pageTimeoutMs });
    await page.waitForLoadState("domcontentloaded", { timeout: 30000 }).catch(() => {});
    await page.waitForLoadState("networkidle", { timeout: networkIdleTimeoutMs }).catch(() => {});
    await page.waitForTimeout(pageSettleMs);
  } catch (error) {
    navigationError = error.message;
  }

  let bodyText = await page.locator("body").innerText({ timeout: 5000 }).catch(() => "");
  let structureCount = await page
    .locator("table, iframe, canvas, img, form, .sup-datagrid, .el-table")
    .count()
    .catch(() => 0);
  if (bodyText.replace(/\s+/g, "").length < 8 && structureCount === 0 && !navigationError) {
    await page.waitForTimeout(blankRetryMs);
    bodyText = await page.locator("body").innerText({ timeout: 5000 }).catch(() => "");
    structureCount = await page
      .locator("table, iframe, canvas, img, form, .sup-datagrid, .el-table")
      .count()
      .catch(() => 0);
  }
  const title = await page.title().catch(() => "");
  const visibleError = findVisibleError(bodyText);
  const untranslatedKeys = findUntranslatedKeys(bodyText);
  const controls = await visibleControls(page);
  const meaningfulTextLength = bodyText.replace(/\s+/g, "").length;
  const blank = meaningfulTextLength < 8 && structureCount === 0;
  const navigationStatus = navigation ? navigation.status() : null;
  const hardFailure =
    Boolean(navigationError) ||
    (navigationStatus !== null && navigationStatus >= 400) ||
    networkErrors.length > 0 ||
    consoleErrors.length > 0 ||
    pageErrors.length > 0 ||
    requestFailures.length > 0 ||
    Boolean(visibleError) ||
    blank;
  const warning = !hardFailure && (untranslatedKeys.length > 0 || consoleWarnings.length > 0);
  const status = hardFailure ? "FAIL" : warning ? "WARN" : "PASS";

  let screenshot = null;
  if (screenshotMode === "all" || (screenshotMode === "failures" && hardFailure)) {
    screenshot = path.join(
      outputDir,
      "screenshots",
      `${String(index + 1).padStart(3, "0")}-${safeName(menu.code || menu.label)}.png`
    );
    await page.screenshot({ path: screenshot, fullPage: true, timeout: 15000 }).catch(() => {
      screenshot = null;
    });
  }

  const result = {
    menuId: menu.id,
    menuCode: menu.code,
    menuPath: menu.labelPath,
    sourceUrl: menu.url,
    resolvedUrl: url,
    finalUrl: page.url(),
    title,
    status,
    navigationStatus,
    navigationError,
    visibleError: visibleError || null,
    blank,
    meaningfulTextLength,
    visibleControls: controls,
    untranslatedKeys,
    networkErrors,
    consoleErrors,
    consoleWarnings,
    pageErrors,
    requestFailures,
    screenshot,
  };
  await page.close();
  console.log(`${status} ${index + 1}/${total} ${menu.labelPath} ${menu.url}`);
  return result;
}

async function main() {
  ensureDir(outputDir);
  ensureDir(path.join(outputDir, "screenshots"));
  const startedAt = new Date().toISOString();
  const api = await request.newContext({
    ignoreHTTPSErrors: true,
    timeout: apiTimeoutMs,
  });
  let browser;

  try {
    const { ticket, loginPayload } = await login(api);
    const currentMenuResult = await fetchJson(api, ticket, "/inter-api/rbac/v1/menus/currentUser");
    const referenceMenuResult = await fetchJson(api, ticket, "/inter-api/rbac/v1/menus/ref?restrict=false");
    if (!currentMenuResult.ok) {
      throw new Error(`Current-user menu API failed: ${currentMenuResult.status} ${currentMenuResult.bodySnippet}`);
    }
    if (!referenceMenuResult.ok) {
      throw new Error(`Reference menu API failed: ${referenceMenuResult.status} ${referenceMenuResult.bodySnippet}`);
    }

    const currentMenus = flattenMenus(currentMenuResult.json, "currentUser");
    const referenceMenus = flattenMenus(referenceMenuResult.json, "reference");
    const currentKeys = new Set(currentMenus.map((menu) => String(menu.id || menu.code || "")));
    const directory = referenceMenus.map((menu) => ({
      ...menu,
      visibleToAdmin: currentKeys.has(String(menu.id || menu.code || "")),
    }));
    let navigableMenus = deduplicateNavigableMenus(currentMenus);
    let menuPattern = null;
    if (menuPatternText) {
      try {
        menuPattern = new RegExp(menuPatternText, "i");
      } catch (error) {
        throw new Error(`Invalid ADP_MENU_PATTERN=${menuPatternText}: ${error.message}`);
      }
      navigableMenus = navigableMenus.filter((menu) =>
        menuPattern.test(`${menu.labelPath}\n${menu.code}\n${menu.url}`)
      );
    }
    if (Number.isFinite(menuLimit) && menuLimit > 0) {
      navigableMenus = navigableMenus.slice(0, menuLimit);
    }

    const roleResult = await fetchJson(
      api,
      ticket,
      `/inter-api/rbac/v1/role/findOne?code=${encodeURIComponent(roleCode)}`
    );
    const roleData = roleResult.json && (roleResult.json.data || roleResult.json);
    const roleId = roleData && (roleData.id || (roleData.data && roleData.data.id));
    if (!roleResult.ok || !roleId) {
      throw new Error(`Role lookup failed for ${roleCode}: ${roleResult.status} ${roleResult.bodySnippet}`);
    }

    let permissionResults = [];
    if (scanPermissions) {
      const permissionMenus = currentMenus.filter((menu) => menu.id);
      permissionResults = await mapWithConcurrency(
        permissionMenus,
        permissionConcurrency,
        (menu, index) => scanPermissionNode(api, ticket, roleId, menu, index, permissionMenus.length)
      );
    }

    let pageResults = [];
    if (scanPages) {
      browser = await chromium.launch({ headless });
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
        for (const storage of [window.localStorage, window.sessionStorage]) {
          storage.setItem("suposTicket", token);
          storage.setItem("SUPOS_TICKET", token);
          storage.setItem("token", token);
          storage.setItem("ticket", token);
          storage.setItem("language", "zh_CN");
          storage.setItem("langu_code", "zh_CN");
          storage.setItem("locale", "zh-cn");
          if (loginPayloadValue) {
            storage.setItem("loginMsg", JSON.stringify(loginPayloadValue));
          }
        }
      }, { token: ticket, loginPayload });

      pageResults = await mapWithConcurrency(
        navigableMenus,
        pageConcurrency,
        (menu, index) => smokePage(context, menu, index, navigableMenus.length)
      );
      await context.close();
    }

    const pageSummary = {
      total: pageResults.length,
      pass: pageResults.filter((item) => item.status === "PASS").length,
      warn: pageResults.filter((item) => item.status === "WARN").length,
      fail: pageResults.filter((item) => item.status === "FAIL").length,
    };
    const roleAssignedDetails = permissionResults.flatMap(
      (item) => (item.role && item.role.assigned) || []
    );
    const userAssignedDetails = permissionResults.flatMap(
      (item) => (item.user && item.user.assigned) || []
    );
    const uniqueActionCount = (items) => new Set(
      items.map((item) => String(item.id || item.code || `${item.name || ""}|${item.url || ""}`))
    ).size;
    const uniqueActionUrlCount = (items) => new Set(
      items.map((item) => item.url).filter(Boolean)
    ).size;
    const permissionSummary = {
      total: permissionResults.length,
      pass: permissionResults.filter((item) => item.ok).length,
      fail: permissionResults.filter((item) => !item.ok).length,
      navigableWithActionGap: permissionResults.filter(
        (item) => item.navigable && item.effectiveActionGap > 0
      ).length,
      roleAssignedActions: roleAssignedDetails.length,
      roleAssignedUniqueActions: uniqueActionCount(roleAssignedDetails),
      roleAssignedUniqueActionUrls: uniqueActionUrlCount(roleAssignedDetails),
      userAssignedActions: userAssignedDetails.length,
      userAssignedUniqueActions: uniqueActionCount(userAssignedDetails),
      userAssignedUniqueActionUrls: uniqueActionUrlCount(userAssignedDetails),
    };
    const report = {
      generatedAt: new Date().toISOString(),
      startedAt,
      environment: {
        apiBaseUrl: baseUrl,
        browserBaseUrl,
        username,
        roleCode,
        roleId: String(roleId),
        userId,
      },
      scanConfig: {
        headless,
        screenshotMode,
        pageConcurrency,
        permissionConcurrency,
        pageTimeoutMs,
        pageSettleMs,
        networkIdleTimeoutMs,
        blankRetryMs,
        menuLimit: Number.isFinite(menuLimit) && menuLimit > 0 ? menuLimit : null,
        menuPattern: menuPatternText || null,
        scanPages,
        scanPermissions,
      },
      directorySummary: {
        referenceNodes: directory.length,
        adminVisibleNodes: currentMenus.length,
        adminNavigableRoutes: deduplicateNavigableMenus(currentMenus).length,
        scannedNavigableRoutes: navigableMenus.length,
      },
      permissionSummary,
      pageSummary,
      directory,
      adminMenus: currentMenus,
      permissionResults,
      pageResults,
    };
    const reportPath = path.join(outputDir, "admin-permission-directory-scan.json");
    fs.writeFileSync(reportPath, `${JSON.stringify(report, null, 2)}\n`);
    console.log(
      `SUMMARY directory=${directory.length} adminNodes=${currentMenus.length} routes=${navigableMenus.length} ` +
        `permissionPass=${permissionSummary.pass} permissionFail=${permissionSummary.fail} ` +
        `pagePass=${pageSummary.pass} pageWarn=${pageSummary.warn} pageFail=${pageSummary.fail}`
    );
    console.log(`REPORT ${reportPath}`);
    if (permissionSummary.fail > 0 || pageSummary.fail > 0) {
      process.exitCode = 1;
    }
  } finally {
    if (browser) {
      await browser.close().catch(() => {});
    }
    await api.dispose();
  }
}

main().catch((error) => {
  ensureDir(outputDir);
  fs.writeFileSync(
    path.join(outputDir, "admin-permission-directory-scan-error.json"),
    `${JSON.stringify(
      {
        generatedAt: new Date().toISOString(),
        environment: { apiBaseUrl: baseUrl, browserBaseUrl, username, roleCode, userId },
        error: error && error.stack ? error.stack : String(error),
      },
      null,
      2
    )}\n`
  );
  console.error(error && error.stack ? error.stack : error);
  process.exit(1);
});
