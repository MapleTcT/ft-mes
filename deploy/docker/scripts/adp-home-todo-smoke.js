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
const apiTimeoutMs = Number(process.env.ADP_API_TIMEOUT_MS || 90000);
const apiRetries = Number(process.env.ADP_API_RETRIES || 3);
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

async function findFillTarget(page, selectors, timeoutMs = 15000) {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    for (const selector of selectors) {
      const locator = page.locator(selector);
      const count = await locator.count();
      for (let index = 0; index < count; index += 1) {
        const candidate = locator.nth(index);
        const usable = await candidate
          .evaluate((element) => {
            const rect = element.getBoundingClientRect();
            const style = window.getComputedStyle(element);
            return {
              enabled: !element.disabled,
              visible: style.visibility !== "hidden" && style.display !== "none" && rect.width > 0 && rect.height > 0,
              hasBox: style.display !== "none" && rect.width > 0 && rect.height > 0,
            };
          })
          .catch(() => null);
        if (usable && usable.enabled && (usable.visible || usable.hasBox)) {
          return { locator: candidate, selector: `${selector}[${index}]`, visible: usable.visible };
        }
      }
    }
    await page.waitForTimeout(250);
  }
  throw new Error(`No enabled input matched selectors: ${selectors.join(", ")}`);
}

async function fillFirst(page, selectors, value) {
  const match = await findFillTarget(page, selectors);
  if (match.visible) {
    await match.locator.fill(value, { timeout: 5000 });
  } else {
    await match.locator.evaluate((element, nextValue) => {
      element.value = nextValue;
      element.dispatchEvent(new Event("input", { bubbles: true }));
      element.dispatchEvent(new Event("change", { bubbles: true }));
    }, value);
  }
  return match.selector;
}

async function clickLogin(page) {
  const candidates = [
    page.getByRole("button", { name: /登\s*录|登录|Login/i }).first(),
    page.locator("button:has-text('登 录')").first(),
    page.locator("button:has-text('登录')").first(),
    page.getByText(/^登\s*录$|^登录$/).first(),
    page.locator("button.login-submit").first(),
    page.locator("input[type='submit']").first(),
    page.locator("input[type='button']").first(),
  ];

  for (const locator of candidates) {
    const visible = (await locator.count()) > 0 && (await locator.isVisible().catch(() => false));
    if (visible) {
      await locator.click({ timeout: 5000 });
      return;
    }
  }
  await page.keyboard.press("Enter");
  await page.waitForTimeout(1000);
  const hasHomeShell = (await page.locator("#v3_head").count()) > 0;
  if (!hasHomeShell) {
    const hasLegacySubmit = await page.evaluate(() => typeof window.validata === "function").catch(() => false);
    if (hasLegacySubmit) {
      await page.evaluate(() => {
        window.validata();
        return true;
      });
    }
  }
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

  const api = await request.newContext({
    baseURL: baseUrl,
    ignoreHTTPSErrors: true,
    timeout: apiTimeoutMs,
  });
  const { ticket, loginPayload } = await login(api);
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
    ["suposTicket", "SUPOS_TICKET", "token", "ticket"].forEach((key) => {
      window.localStorage.setItem(key, token);
      window.sessionStorage.setItem(key, token);
    });
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
    const navigation = await page.goto(`${browserBaseUrl}/`, { waitUntil: "domcontentloaded", timeout: 45000 });
    navigationStatus = navigation ? navigation.status() : null;
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
  const capturedNetworkErrors = networkErrors.slice();
  const capturedConsoleErrors = consoleErrors.slice();
  const capturedPageErrors = pageErrors.slice();
  const capturedRequestFailures = requestFailures.slice();

  if (screenshotMode === "all" || (screenshotMode === "failures" && failed)) {
    screenshot = screenshotPath(failed ? "home-todo-failure" : "home-todo");
    await page.screenshot({ path: screenshot, fullPage: true, timeout: 10000 }).catch(() => {
      screenshot = null;
    });
  }

  await browser.close();

  const report = {
    generatedAt: new Date().toISOString(),
    baseUrl,
    browserBaseUrl,
    username,
    authMode: "api-login-ticket-cookie-browser",
    ok: !failed,
    navigationStatus,
    navigationError,
    visibleError,
    networkErrors: capturedNetworkErrors,
    consoleErrors: capturedConsoleErrors,
    pageErrors: capturedPageErrors,
    requestFailures: capturedRequestFailures,
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
