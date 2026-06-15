#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { chromium, request } = require("playwright");

const baseUrl = (process.env.ADP_BASE_URL || "http://10.11.100.17:18080").replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD || "123456";
const departmentCode = process.env.ADP_ORG_DEPARTMENT_CODE || "0102";
const departmentName = process.env.ADP_ORG_DEPARTMENT_NAME || "办公室";
const departmentId = process.env.ADP_ORG_DEPARTMENT_ID || "";
const headless = process.env.ADP_HEADLESS !== "false";
const screenshotMode = process.env.ADP_SCREENSHOTS || "failures";
const outputDir =
  process.env.ADP_OUTPUT_DIR ||
  path.join("/tmp", `adp-organization-smoke-${new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14)}`);
const outputPath =
  process.env.ADP_ORGANIZATION_SMOKE_OUTPUT || path.join(outputDir, "organization-smoke-results.json");

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
  return node.menuName || node.name || node.title || node.menuNameZh || node.menuNameEn || node.label || node.code || "";
}

function getTargetUrl(node) {
  return node.url || node.path || node.href || node.menuUrl || node.targetUrl || node.routeUrl || "";
}

function flattenMenus(payload) {
  const targets = [];

  function visit(node, parents) {
    if (!node || typeof node !== "object") {
      return;
    }
    const label = getLabel(node);
    const nextParents = parents.concat(label);
    const url = getTargetUrl(node);
    if (typeof url === "string" && url.trim() && !/^javascript:/i.test(url.trim())) {
      targets.push({
        label: nextParents.join(" > "),
        url: url.trim(),
        code: node.code || node.menuInfoCode || node.menuCode || "",
      });
    }
    getChildren(node).forEach((child) => visit(child, nextParents));
  }

  pickMenuRoots(payload).forEach((root) => visit(root, []));
  return targets;
}

async function fetchOrganizationManageUrl(api, ticket) {
  const response = await api.get(`${baseUrl}/inter-api/rbac/v1/menus/currentUser`, {
    headers: authHeaders(ticket),
  });
  const parsed = await readJsonSafe(response);
  if (!response.ok()) {
    throw new Error(`Menu API failed with ${response.status()}: ${parsed.text.slice(0, 500)}`);
  }
  const targets = flattenMenus(parsed.json);
  const target =
    targets.find((item) => item.code === "organizationmanage") ||
    targets.find((item) => /organizationmanage/i.test(item.url)) ||
    targets.find((item) => /ORGANIZATION_MANAGE|组织管理/.test(item.label));
  return target ? target.url : "/organization/#/organizationmanage";
}

function authHeaders(ticket) {
  return {
    Accept: "application/json, text/plain, */*",
    Authorization: `Bearer ${ticket}`,
    Cookie: `suposTicket=${encodeURIComponent(ticket)}`,
    langu_code: "zh_CN",
  };
}

function collectDepartments(payload) {
  const departments = [];
  const roots = pickArray(payload, payload && payload.data, payload && payload.result, payload && payload.list) || [
    payload && payload.data,
  ];

  function visit(node) {
    if (!node || typeof node !== "object") {
      return;
    }
    if (node.id && (node.code || node.name)) {
      departments.push(node);
    }
    getChildren(node).forEach(visit);
  }

  roots.forEach(visit);
  return departments;
}

async function runApiCheck(api, ticket, name, urlPath) {
  const response = await api.get(`${baseUrl}${urlPath}`, { headers: authHeaders(ticket) });
  const parsed = await readJsonSafe(response);
  const ok = response.status() < 400 && !visibleErrorPattern.test(parsed.text || "");
  return {
    name,
    path: urlPath,
    ok,
    status: response.status(),
    bodySnippet: ok ? undefined : parsed.text.slice(0, 1000),
  };
}

async function selectDepartment(api, ticket) {
  const treePath = "/inter-api/organization/v1/departments?companyId=1000";
  const tree = await runApiCheck(api, ticket, "department-tree", treePath);
  if (!tree.ok) {
    throw new Error(`Department tree API failed: ${tree.bodySnippet || tree.status}`);
  }
  const response = await api.get(`${baseUrl}${treePath}`, { headers: authHeaders(ticket) });
  const parsed = await readJsonSafe(response);
  const departments = collectDepartments(parsed.json).filter((item) => item.companyId);
  const selected =
    (departmentId && departments.find((item) => String(item.id) === String(departmentId))) ||
    departments.find((item) => item.code === departmentCode) ||
    departments.find((item) => item.name === departmentName) ||
    departments.find((item) => item.id && item.companyId === 1000);
  if (!selected) {
    throw new Error(`No department found in tree response: ${parsed.text.slice(0, 500)}`);
  }
  return { selected, tree };
}

function findVisibleError(bodyText) {
  return String(bodyText || "")
    .split(/\r?\n/)
    .map((line) => line.trim())
    .find((line) => line && visibleErrorPattern.test(line));
}

async function runBrowserClick(ticket, organizationUrl, department) {
  const browser = await chromium.launch({ headless });
  const context = await browser.newContext({
    baseURL: baseUrl,
    ignoreHTTPSErrors: true,
    viewport: { width: 1440, height: 960 },
    extraHTTPHeaders: {
      Authorization: `Bearer ${ticket}`,
    },
  });
  await context.addCookies([
    { name: "suposTicket", value: ticket, url: baseUrl },
    { name: "SUPOS_TICKET", value: ticket, url: baseUrl },
  ]);
  await context.addInitScript((token) => {
    window.localStorage.setItem("suposTicket", token);
    window.localStorage.setItem("SUPOS_TICKET", token);
    window.localStorage.setItem("token", token);
    window.sessionStorage.setItem("suposTicket", token);
    window.sessionStorage.setItem("SUPOS_TICKET", token);
    window.sessionStorage.setItem("token", token);
  }, ticket);

  const page = await context.newPage();
  const networkErrors = [];
  const pageErrors = [];
  const requestFailures = [];
  const resolvedUrl = new URL(organizationUrl, `${baseUrl}/`).toString();
  let navigationStatus = null;
  let navigationError = null;
  let visibleError = null;
  let screenshot = null;

  page.on("response", async (response) => {
    const type = response.request().resourceType();
    const relevant = /\/(inter-api\/organization|organization\/)/.test(response.url());
    if (!relevant || !["document", "xhr", "fetch"].includes(type) || response.status() < 400) {
      return;
    }
    let body = "";
    try {
      body = (await response.text()).slice(0, 1000);
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

  page.on("pageerror", (error) => {
    pageErrors.push(error.message);
  });

  page.on("requestfailed", (requestItem) => {
    if (/\/(inter-api\/organization|organization\/)/.test(requestItem.url())) {
      requestFailures.push({
        method: requestItem.method(),
        type: requestItem.resourceType(),
        url: requestItem.url(),
        failure: requestItem.failure() && requestItem.failure().errorText,
      });
    }
  });

  try {
    const navigation = await page.goto(resolvedUrl, { waitUntil: "domcontentloaded", timeout: 45000 });
    navigationStatus = navigation ? navigation.status() : null;
    await page.waitForLoadState("networkidle", { timeout: 10000 }).catch(() => {});
    await page.getByText(department.name, { exact: true }).first().waitFor({ state: "visible", timeout: 30000 });
    await page.getByText(department.name, { exact: true }).first().click({ timeout: 10000 });
    await page.waitForLoadState("networkidle", { timeout: 10000 }).catch(() => {});
    await page.waitForTimeout(1200);
    const bodyText = await page.locator("body").innerText({ timeout: 5000 }).catch(() => "");
    visibleError = findVisibleError(bodyText) || null;
  } catch (error) {
    navigationError = error.message;
  }

  const failed =
    Boolean(navigationError) ||
    (navigationStatus !== null && navigationStatus >= 400) ||
    networkErrors.length > 0 ||
    pageErrors.length > 0 ||
    requestFailures.length > 0 ||
    Boolean(visibleError);

  if (screenshotMode === "all" || (screenshotMode === "failures" && failed)) {
    screenshot = path.join(outputDir, failed ? "organization-failure.png" : "organization.png");
    await page.screenshot({ path: screenshot, fullPage: true }).catch(() => {
      screenshot = null;
    });
  }

  await browser.close();

  return {
    ok: !failed,
    url: organizationUrl,
    resolvedUrl,
    navigationStatus,
    navigationError,
    visibleError,
    networkErrors,
    pageErrors,
    requestFailures,
    screenshot,
  };
}

async function main() {
  ensureDir(path.dirname(outputPath));
  ensureDir(outputDir);

  const api = await request.newContext({ ignoreHTTPSErrors: true });
  const ticket = await login(api);
  const organizationUrl = await fetchOrganizationManageUrl(api, ticket);
  const { selected, tree } = await selectDepartment(api, ticket);
  const detail = await runApiCheck(
    api,
    ticket,
    "department-detail",
    `/inter-api/organization/v1/department?id=${encodeURIComponent(selected.id)}`
  );
  const relatedPersons = await runApiCheck(
    api,
    ticket,
    "department-related-persons",
    `/inter-api/organization/v1/department/person?companyId=${encodeURIComponent(
      selected.companyId || 1000
    )}&departmentId=${encodeURIComponent(selected.id)}&current=1&pageSize=20`
  );
  await api.dispose();

  const browser = await runBrowserClick(ticket, organizationUrl, selected);
  const apiResults = [tree, detail, relatedPersons];
  const failedApi = apiResults.filter((result) => !result.ok);
  const ok = failedApi.length === 0 && browser.ok;
  const report = {
    generatedAt: new Date().toISOString(),
    baseUrl,
    username,
    ok,
    selectedDepartment: {
      id: selected.id,
      code: selected.code,
      name: selected.name,
      companyId: selected.companyId,
    },
    organizationUrl,
    apiResults,
    browser,
  };

  fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`);
  console.log(`${ok ? "OK" : "FAIL"} organization-department selected=${selected.code || selected.id}`);
  console.log(`REPORT ${outputPath}`);

  if (!ok) {
    process.exitCode = 1;
  }
}

main().catch((error) => {
  console.error(error && error.stack ? error.stack : error);
  process.exit(1);
});
