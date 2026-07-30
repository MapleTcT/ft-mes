#!/usr/bin/env node
"use strict";

const fs = require("node:fs");
const path = require("node:path");
const { spawnSync } = require("node:child_process");
const { chromium, request } = require("playwright");

const repoRoot = path.resolve(__dirname, "../../..");
const stamp = new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14);
const baseUrl = (process.env.ADP_BASE_URL || "http://10.11.100.17:18080").replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD || "123456";
const entityCode = "QCS_5.0.0.0_inspectReport";
const route = `/msService/ec/entity/config?entity.code=${encodeURIComponent(entityCode)}`;
const outputDir =
  process.env.ADP_OUTPUT_DIR || path.join("/tmp", `adp-qcs-inspect-report-config-${stamp}`);
const outputPath =
  process.env.ADP_OUTPUT_PATH || path.join(outputDir, "qcs-inspect-report-config-results.json");
const headless = process.env.ADP_HEADLESS !== "false";
const visibleErrorPattern =
  /(数据库操作异常|系统发生未知异常|系统错误|SQLGrammarException|could not extract ResultSet|Bad value for type long)/i;

const tabs = [
  { label: "基本信息" },
  { label: "数据模型" },
  {
    label: "视图信息",
    endpoint: "/msService/ec/view/list",
    screenshot: "01-view-information.png",
  },
  {
    label: "菜单信息",
    endpoint: "/msService/ec/entity/publishMenuFrame",
    screenshot: "02-menu-information.png",
  },
  {
    label: "工作流",
    endpoint: "/msService/ec/entity/wf",
    screenshot: "03-workflow.png",
  },
  { label: "Excel导入模板" },
  { label: "脚本信息" },
  { label: "自定义代码" },
];

function repoCommit() {
  const result = spawnSync("git", ["rev-parse", "HEAD"], {
    cwd: repoRoot,
    encoding: "utf8",
  });
  return result.status === 0 ? result.stdout.trim() : "UNKNOWN";
}

function ticketFrom(payload) {
  const candidates = [
    payload && payload.ticket,
    payload && payload.token,
    payload && payload.access_token,
    payload && payload.data && payload.data.ticket,
    payload && payload.data && payload.data.token,
    payload && payload.data && payload.data.access_token,
  ];
  return candidates.find((value) => typeof value === "string" && value.length > 20);
}

async function responseBody(response) {
  const text = await response.text();
  try {
    return { text, json: JSON.parse(text) };
  } catch (_error) {
    return { text, json: null };
  }
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
        Accept: "application/json",
        "Content-Type": "application/json;charset=UTF-8",
      },
    });
    const parsed = await responseBody(response);
    const ticket = response.ok() ? ticketFrom(parsed.json) : null;
    if (ticket) {
      return { ticket, status: response.status() };
    }
    failures.push({ status: response.status(), body: parsed.text.slice(0, 300) });
  }
  throw new Error(`Login failed: ${JSON.stringify(failures)}`);
}

async function main() {
  fs.mkdirSync(outputDir, { recursive: true });
  const evidence = {
    schemaVersion: 1,
    generatedAt: new Date().toISOString(),
    repoCommit: repoCommit(),
    database: "PostgreSQL",
    baseUrl,
    route,
    entityCode,
    status: "RUNNING",
    summary: { testedFeatures: tabs.length, pass: 0, fail: 0, blocked: 0, notApplicable: 0 },
    frontend: {
      console: [],
      pageErrors: [],
      requestFailures: [],
      serverErrors: [],
      tabs: [],
      screenshots: {},
    },
    persistence: {
      required: false,
      status: "NOT_APPLICABLE",
      reason: "Read-only low-code configuration-page regression; no business data is changed.",
    },
    issues: [],
  };

  let browser;
  try {
    const api = await request.newContext({ ignoreHTTPSErrors: true });
    const loginResult = await login(api);
    await api.dispose();
    evidence.login = { status: loginResult.status, authenticated: true };

    browser = await chromium.launch({ headless });
    const context = await browser.newContext({
      baseURL: baseUrl,
      ignoreHTTPSErrors: true,
      viewport: { width: 1600, height: 1000 },
    });
    await context.addCookies([
      { name: "suposTicket", value: loginResult.ticket, url: baseUrl },
      { name: "SUPOS_TICKET", value: loginResult.ticket, url: baseUrl },
    ]);
    await context.addInitScript((ticket) => {
      localStorage.setItem("ticket", ticket);
      localStorage.setItem("suposTicket", ticket);
      localStorage.setItem("SUPOS_TICKET", ticket);
      localStorage.setItem("token", ticket);
    }, loginResult.ticket);

    const page = await context.newPage();
    page.on("console", (message) => {
      if (["error", "warning"].includes(message.type())) {
        evidence.frontend.console.push({ type: message.type(), text: message.text() });
      }
    });
    page.on("pageerror", (error) => {
      evidence.frontend.pageErrors.push({ message: error.message });
    });
    page.on("requestfailed", (item) => {
      evidence.frontend.requestFailures.push({
        method: item.method(),
        url: item.url(),
        failure: item.failure(),
      });
    });
    page.on("response", async (response) => {
      if (response.status() >= 500 && response.url().includes("/msService/ec/")) {
        evidence.frontend.serverErrors.push({
          status: response.status(),
          url: response.url(),
        });
      }
    });

    const navigation = await page.goto(route, {
      waitUntil: "domcontentloaded",
      timeout: 60000,
    });
    await page.waitForTimeout(1200);
    evidence.frontend.navigation = {
      status: navigation && navigation.status(),
      title: await page.title(),
    };

    for (const tab of tabs) {
      const item = {
        label: tab.label,
        expectedEndpoint: tab.endpoint || null,
        responseStatus: null,
        visibleError: null,
        status: "PASS",
      };
      try {
        const locator = page.getByText(tab.label, { exact: true }).first();
        await locator.waitFor({ state: "visible", timeout: 15000 });
        let endpointResponse = null;
        if (tab.endpoint) {
          const responsePromise = page.waitForResponse(
            (response) => response.url().includes(tab.endpoint),
            { timeout: 15000 }
          );
          await locator.click();
          endpointResponse = await responsePromise;
        } else {
          await locator.click();
        }
        await page.waitForTimeout(1000);
        if (endpointResponse) {
          item.responseStatus = endpointResponse.status();
          if (endpointResponse.status() >= 400) {
            throw new Error(`${tab.endpoint} returned HTTP ${endpointResponse.status()}`);
          }
        }

        const visibleText = await page.locator("body").innerText();
        const errorMatch = visibleText.match(visibleErrorPattern);
        if (errorMatch) {
          item.visibleError = errorMatch[0];
          throw new Error(`Visible error: ${errorMatch[0]}`);
        }
        if (tab.screenshot) {
          const screenshotPath = path.join(outputDir, tab.screenshot);
          await page.screenshot({ path: screenshotPath, fullPage: true });
          evidence.frontend.screenshots[tab.label] = screenshotPath;
        }
        evidence.summary.pass += 1;
      } catch (error) {
        item.status = "FAIL";
        item.error = error.message;
        evidence.summary.fail += 1;
      }
      evidence.frontend.tabs.push(item);
    }

    if (
      evidence.summary.fail ||
      evidence.frontend.pageErrors.length ||
      evidence.frontend.serverErrors.length
    ) {
      throw new Error("QCS inspection-report configuration regression failed");
    }
    evidence.status = "PASS";
  } catch (error) {
    evidence.status = "FAIL";
    evidence.issues.push(error.message);
  } finally {
    if (browser) {
      await browser.close();
    }
    fs.writeFileSync(outputPath, `${JSON.stringify(evidence, null, 2)}\n`, "utf8");
  }

  console.log(JSON.stringify({
    status: evidence.status,
    outputPath,
    summary: evidence.summary,
  }, null, 2));
  if (evidence.status !== "PASS") {
    process.exitCode = 1;
  }
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
