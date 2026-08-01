#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { chromium, request } = require("playwright");

const baseUrl = (process.env.ADP_BASE_URL || "http://10.11.100.17:18080").replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD;
const headless = process.env.ADP_HEADLESS !== "false";
const outputDir =
  process.env.ADP_OUTPUT_DIR ||
  path.join("/tmp", `adp-lims-sample-menu-${new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14)}`);

if (!password) {
  throw new Error("ADP_PASSWORD is required");
}

const expectedGroups = [
  {
    code: "LIMSSample.group.registerCollect",
    name: "登记与取样",
    children: [
      "LIMSSample_5.0.0.0_sample_sampleRegisterLayout",
      "LIMSBasic_1.0.0_testPlan_planSetSampleList",
      "LIMSSample_5.0.0.0_sample_batchSampleRegister",
      "LIMSSample_5.0.0.0_sample_sampTaskAlcatLayOutNew",
      "LIMSSample_5.0.0.0_sample_collectListLayoutNew",
    ],
  },
  {
    code: "LIMSSample.group.receivePrepare",
    name: "收样与制备",
    children: [
      "LIMSSample_5.0.0.0_sample_receiveListLayoutNew",
      "LIMSSample_5.0.0.0_sample_sampleSweepReceive",
      "LIMSSample_5.0.0.0_sample_makeListLayoutNew",
      "LIMSSample_5.0.0.0_sample_handoverListLayoutNew",
    ],
  },
  {
    code: "LIMSSample.group.resultReview",
    name: "结果录入与复核",
    children: [
      "LIMSSample_5.0.0.0_sample_batchRecordByTest",
      "LIMSSample_5.0.0.0_sample_recordBySingleSample",
      "LIMSSample_5.0.0.0_sample_recordByTest",
      "LIMSSample_5.0.0.0_sample_recordBySample",
      "LIMSSample_5.0.0.0_sample_recordCheckBySample",
      "LIMSSample_5.0.0.0_sample_recordCheckByTest",
    ],
  },
  {
    code: "LIMSSample.group.auditDisposition",
    name: "审核与处置",
    children: [
      "LIMSSample_5.0.0.0_sample_sampleCheck",
      "LIMSSample_5.0.0.0_sample_sampleRefuse",
      "LIMSSample_5.0.0.0_sample_sampleAccept",
      "LIMSSample_5.0.0.0_sample_sampleDealListLayout",
      "LIMSSample_5.0.0.0_sample_remainSampleLayout",
      "LIMSSample_5.0.0.0_sample_retainListLayout",
    ],
  },
  {
    code: "LIMSSample.group.ledgerReport",
    name: "台账与报告",
    children: [
      "LIMSSample_5.0.0.0_sample_sampleTestProgress",
      "LIMSSample_5.0.0.0_sample_sampleInfoLayout",
      "LIMSSample_5.0.0.0_sampleReport_sampleReportList",
    ],
  },
];

const visibleErrorPattern =
  /(系统错误|系统异常|发生未知异常|数据库操作异常|404_NOT_FOUND|500 INTERNAL|Invalid bound statement|relation .* does not exist|column .* does not exist)/i;

function pickArray(...values) {
  return values.find((value) => Array.isArray(value)) || [];
}

function roots(payload) {
  return pickArray(
    payload,
    payload && payload.list,
    payload && payload.data,
    payload && payload.data && payload.data.list,
    payload && payload.data && payload.data.menus,
    payload && payload.result,
    payload && payload.result && payload.result.list,
    payload && payload.result && payload.result.menus
  );
}

function children(node) {
  return pickArray(
    node && node.children,
    node && node.childrens,
    node && node.childMenus,
    node && node.subMenus,
    node && node.nodes,
    node && node.menuList
  );
}

function findNode(nodes, code) {
  for (const node of nodes) {
    if (node && (node.code === code || node.menuCode === code)) {
      return node;
    }
    const nested = findNode(children(node), code);
    if (nested) return nested;
  }
  return null;
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

  const failures = [];
  for (const body of attempts) {
    const response = await api.post(`${baseUrl}/inter-api/auth/login`, {
      data: body,
      headers: { "Content-Type": "application/json;charset=UTF-8" },
    });
    const parsed = await readJsonSafe(response);
    const ticket = response.ok() ? findTicket(parsed.json) : null;
    if (ticket) return { ticket, payload: parsed.json, status: response.status() };
    failures.push({ status: response.status(), body: parsed.text.slice(0, 300) });
  }
  throw new Error(`ADP login failed: ${JSON.stringify(failures)}`);
}

async function fetchMenuTree(api, ticket) {
  const response = await api.get(`${baseUrl}/inter-api/rbac/v1/menus/currentUser`, {
    headers: {
      Authorization: `Bearer ${ticket}`,
      Cookie: `suposTicket=${encodeURIComponent(ticket)}`,
      langu_code: "zh_CN",
    },
  });
  const parsed = await readJsonSafe(response);
  if (!response.ok()) {
    throw new Error(`Menu API ${response.status()}: ${parsed.text.slice(0, 500)}`);
  }
  return parsed.json;
}

function assertMenuTree(payload) {
  const root = findNode(roots(payload), "LIMSSample");
  if (!root) throw new Error("LIMSSample root is missing from current-user menus");

  const directGroups = children(root);
  const actualGroupCodes = directGroups.map((node) => node.code);
  const expectedGroupCodes = expectedGroups.map((group) => group.code);
  if (JSON.stringify(actualGroupCodes) !== JSON.stringify(expectedGroupCodes)) {
    throw new Error(
      `Unexpected direct groups: ${JSON.stringify(actualGroupCodes)}; expected ${JSON.stringify(expectedGroupCodes)}`
    );
  }

  const pages = [];
  for (const expected of expectedGroups) {
    const group = directGroups.find((node) => node.code === expected.code);
    if (group.nameDisplay !== expected.name) {
      throw new Error(`${expected.code} label is ${group.nameDisplay}; expected ${expected.name}`);
    }
    if (group.url) {
      throw new Error(`${expected.code} must be a navigation folder without a URL`);
    }

    const groupChildren = children(group);
    const actualChildCodes = groupChildren.map((node) => node.code);
    if (JSON.stringify(actualChildCodes) !== JSON.stringify(expected.children)) {
      throw new Error(
        `${expected.code} children are ${JSON.stringify(actualChildCodes)}; expected ${JSON.stringify(expected.children)}`
      );
    }
    for (const page of groupChildren) {
      if (!page.url) throw new Error(`${page.code} lost its page URL`);
      pages.push({ group: expected.name, code: page.code, name: page.nameDisplay, url: page.url });
    }
  }

  if (pages.length !== 24) {
    throw new Error(`Expected 24 retained sample pages, got ${pages.length}`);
  }
  return { root, groups: directGroups, pages };
}

async function createBrowserContext(browser, auth) {
  const context = await browser.newContext({
    baseURL: baseUrl,
    ignoreHTTPSErrors: true,
    viewport: { width: 1600, height: 1000 },
    extraHTTPHeaders: { Authorization: `Bearer ${auth.ticket}` },
  });
  await context.addCookies([
    { name: "suposTicket", value: auth.ticket, url: baseUrl },
    { name: "SUPOS_TICKET", value: auth.ticket, url: baseUrl },
  ]);
  await context.addInitScript(({ token, loginPayload }) => {
    for (const storage of [window.localStorage, window.sessionStorage]) {
      storage.setItem("suposTicket", token);
      storage.setItem("SUPOS_TICKET", token);
      storage.setItem("token", token);
      storage.setItem("ticket", token);
      storage.setItem("language", "zh_CN");
      storage.setItem("langu_code", "zh_CN");
      storage.setItem("locale", "zh-cn");
      storage.setItem("loginMsg", JSON.stringify(loginPayload));
    }
  }, { token: auth.ticket, loginPayload: auth.payload });
  return context;
}

async function verifyRenderedNavigation(context, report) {
  const page = await context.newPage();
  const consoleErrors = [];
  const pageErrors = [];
  const networkErrors = [];
  page.on("console", (message) => {
    if (message.type() === "error") consoleErrors.push(message.text());
  });
  page.on("pageerror", (error) => pageErrors.push(error.message));
  page.on("response", (response) => {
    if (["document", "xhr", "fetch"].includes(response.request().resourceType()) && response.status() >= 400) {
      networkErrors.push({ status: response.status(), url: response.url() });
    }
  });

  const response = await page.goto(baseUrl, { waitUntil: "domcontentloaded", timeout: 90000 });
  await page.waitForLoadState("networkidle", { timeout: 12000 }).catch(() => {});
  await page.waitForTimeout(2500);

  const sampleRoot = page.getByText("样品管理", { exact: true }).last();
  if (!(await sampleRoot.isVisible().catch(() => false))) {
    throw new Error("Rendered navigation does not contain 样品管理");
  }
  await sampleRoot.click();
  await page.waitForTimeout(800);

  const visibility = {};
  for (const group of expectedGroups) {
    visibility[group.name] = await page.getByText(group.name, { exact: true }).last().isVisible().catch(() => false);
  }
  if (Object.values(visibility).some((visible) => !visible)) {
    throw new Error(`Rendered sample groups are incomplete: ${JSON.stringify(visibility)}`);
  }

  const body = await page.locator("body").innerText();
  const visibleError = body.split(/\r?\n/).find((line) => visibleErrorPattern.test(line)) || null;
  const screenshot = path.join(outputDir, "lims-sample-menu-groups.png");
  await page.screenshot({ path: screenshot, fullPage: true });

  report.renderedNavigation = {
    status: response && response.status(),
    visibility,
    visibleError,
    consoleErrors,
    pageErrors,
    networkErrors,
    screenshot,
  };

  if (visibleError || consoleErrors.length || pageErrors.length || networkErrors.length) {
    throw new Error(`Rendered navigation errors: ${JSON.stringify(report.renderedNavigation)}`);
  }
  await page.close();
}

async function verifyRepresentativePages(context, tree, report) {
  const representativeCodes = expectedGroups.map((group) => group.children[0]);
  const pages = tree.pages.filter((page) => representativeCodes.includes(page.code));
  report.representativePages = [];

  for (const target of pages) {
    const page = await context.newPage();
    const networkErrors = [];
    const pageErrors = [];
    page.on("pageerror", (error) => pageErrors.push(error.message));
    page.on("response", (response) => {
      if (["document", "xhr", "fetch"].includes(response.request().resourceType()) && response.status() >= 400) {
        networkErrors.push({ status: response.status(), url: response.url() });
      }
    });
    const response = await page.goto(new URL(target.url, `${baseUrl}/`).toString(), {
      waitUntil: "domcontentloaded",
      timeout: 90000,
    });
    await page.waitForLoadState("networkidle", { timeout: 8000 }).catch(() => {});
    await page.waitForTimeout(1200);
    const body = await page.locator("body").innerText().catch(() => "");
    const visibleError = body.split(/\r?\n/).find((line) => visibleErrorPattern.test(line)) || null;
    const result = {
      ...target,
      status: response && response.status(),
      visibleError,
      pageErrors,
      networkErrors,
    };
    report.representativePages.push(result);
    await page.close();
    if (!response || response.status() >= 400 || visibleError || pageErrors.length || networkErrors.length) {
      throw new Error(`Representative page failed: ${JSON.stringify(result)}`);
    }
  }
}

async function main() {
  fs.mkdirSync(outputDir, { recursive: true });
  const report = {
    generatedAt: new Date().toISOString(),
    baseUrl,
    username,
    status: "RUNNING",
    issues: [],
  };

  let browser;
  let api;
  try {
    api = await request.newContext({ ignoreHTTPSErrors: true, timeout: 90000 });
    const auth = await login(api);
    report.loginStatus = auth.status;
    const payload = await fetchMenuTree(api, auth.ticket);
    const tree = assertMenuTree(payload);
    report.menuTree = {
      root: tree.root.nameDisplay,
      directGroupCount: tree.groups.length,
      retainedPageCount: tree.pages.length,
      groups: expectedGroups.map((group) => ({
        code: group.code,
        name: group.name,
        pageCount: group.children.length,
      })),
    };

    browser = await chromium.launch({ headless });
    const context = await createBrowserContext(browser, auth);
    await verifyRenderedNavigation(context, report);
    await verifyRepresentativePages(context, tree, report);
    await context.close();
    report.status = "PASS";
  } catch (error) {
    report.status = "FAIL";
    report.issues.push(error.stack || error.message);
    process.exitCode = 1;
  } finally {
    await api?.dispose().catch(() => {});
    await browser?.close().catch(() => {});
    const reportPath = path.join(outputDir, "acceptance.json");
    fs.writeFileSync(reportPath, `${JSON.stringify(report, null, 2)}\n`);
    console.log(JSON.stringify({ status: report.status, reportPath, outputDir }, null, 2));
  }
}

main();
