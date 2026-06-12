#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { chromium } = require("playwright");

const baseUrl = (process.env.ADP_BASE_URL || "http://10.11.100.17:18080").replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD || "123456";
const headless = process.env.ADP_HEADLESS !== "false";
const screenshotMode = process.env.ADP_SCREENSHOTS || "failures";
const outputDir =
  process.env.ADP_OUTPUT_DIR ||
  path.join("/tmp", `adp-home-todo-smoke-${new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14)}`);

const visibleErrorPattern =
  /(系统错误|系统异常|发生未知异常|SQLGrammarException|could not extract ResultSet|404_NOT_FOUND|500 INTERNAL|\bHTTP\s*(404|500)\b|\b(404|500)\s+(Not Found|Internal Server Error)\b|Caused by:|\b[\w.]+Exception(?::|\s+at\b)|Invalid bound statement|relation .* does not exist|column .* does not exist)/i;

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true });
}

function screenshotPath(name) {
  return path.join(outputDir, `${name}.png`);
}

async function fillFirst(page, selectors, value) {
  for (const selector of selectors) {
    const locator = page.locator(selector).first();
    if ((await locator.count()) > 0) {
      await locator.fill(value, { timeout: 5000 });
      return selector;
    }
  }
  throw new Error(`No input matched selectors: ${selectors.join(", ")}`);
}

async function clickLogin(page) {
  const candidates = [
    page.getByRole("button", { name: /登\s*录|登录|Login/i }).first(),
    page.locator("button:has-text('登 录')").first(),
    page.locator("button:has-text('登录')").first(),
    page.getByText(/^登\s*录$|^登录$/).first(),
  ];

  for (const locator of candidates) {
    if ((await locator.count()) > 0) {
      await locator.click({ timeout: 5000 });
      return;
    }
  }
  throw new Error("Login button not found");
}

async function clickTopTodo(page) {
  const candidates = [
    page.locator("#v3_head .v3_icon_pending").first(),
    page.locator("#v3_head li").filter({ hasText: /待办/ }).first(),
    page.locator("li.v3_icon_pending").first(),
    page.locator("text=待办").first(),
  ];

  for (const locator of candidates) {
    if ((await locator.count()) > 0) {
      await locator.waitFor({ state: "visible", timeout: 15000 });
      await locator.click({ timeout: 10000 });
      return;
    }
  }
  throw new Error("Workbench Todo tab not found");
}

function findVisibleError(bodyText) {
  return String(bodyText || "")
    .split(/\r?\n/)
    .map((line) => line.trim())
    .find((line) => line && visibleErrorPattern.test(line));
}

async function main() {
  ensureDir(outputDir);

  const browser = await chromium.launch({ headless });
  const context = await browser.newContext({
    baseURL: baseUrl,
    ignoreHTTPSErrors: true,
    viewport: { width: 1440, height: 960 },
  });
  const page = await context.newPage();

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

  let navigationStatus = null;
  let navigationError = null;
  let visibleError = null;
  let screenshot = null;

  try {
    const navigation = await page.goto(`${baseUrl}/`, { waitUntil: "domcontentloaded", timeout: 45000 });
    navigationStatus = navigation ? navigation.status() : null;
    await page.waitForLoadState("networkidle", { timeout: 10000 }).catch(() => {});

    await fillFirst(page, ["#username2", "input[name='username']", "input[type='text']"], username);
    await fillFirst(page, ["#password2", "input[name='password']", "input[type='password']"], password);
    await clickLogin(page);
    await page.locator("#v3_head").waitFor({ state: "visible", timeout: 45000 });
    await page.waitForLoadState("networkidle", { timeout: 10000 }).catch(() => {});

    await clickTopTodo(page);
    await page.waitForLoadState("networkidle", { timeout: 10000 }).catch(() => {});
    await page.waitForTimeout(1500);

    const bodyText = await page.locator("body").innerText({ timeout: 5000 }).catch(() => "");
    visibleError = findVisibleError(bodyText) || null;
  } catch (error) {
    navigationError = error.message;
  }

  const failed =
    Boolean(navigationError) ||
    (navigationStatus !== null && navigationStatus >= 400) ||
    networkErrors.length > 0 ||
    consoleErrors.length > 0 ||
    pageErrors.length > 0 ||
    requestFailures.length > 0 ||
    Boolean(visibleError);

  if (screenshotMode === "all" || (screenshotMode === "failures" && failed)) {
    screenshot = screenshotPath(failed ? "home-todo-failure" : "home-todo");
    await page.screenshot({ path: screenshot, fullPage: true }).catch(() => {
      screenshot = null;
    });
  }

  await browser.close();

  const report = {
    generatedAt: new Date().toISOString(),
    baseUrl,
    username,
    ok: !failed,
    navigationStatus,
    navigationError,
    visibleError,
    networkErrors,
    consoleErrors,
    pageErrors,
    requestFailures,
    screenshot,
  };
  const reportPath = path.join(outputDir, "home-todo-smoke-results.json");
  fs.writeFileSync(reportPath, `${JSON.stringify(report, null, 2)}\n`);

  console.log(`${report.ok ? "OK" : "FAIL"} home-todo`);
  console.log(`REPORT ${reportPath}`);

  if (failed) {
    process.exitCode = 1;
  }
}

main().catch((error) => {
  console.error(error && error.stack ? error.stack : error);
  process.exit(1);
});
