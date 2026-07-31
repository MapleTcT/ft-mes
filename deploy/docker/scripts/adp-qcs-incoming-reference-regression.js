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
const route = "/msService/QCS/inspect/inspect/purchInspectEdit";
const outputDir =
  process.env.ADP_OUTPUT_DIR || path.join("/tmp", `adp-qcs-incoming-reference-${stamp}`);
const outputPath =
  process.env.ADP_OUTPUT_PATH || path.join(outputDir, "qcs-incoming-reference-results.json");
const headless = process.env.ADP_HEADLESS !== "false";
const verbose = process.env.ADP_VERBOSE === "true";
const visibleErrorPattern =
  /(数据库操作异常|系统发生未知异常|系统错误|服务器异常|SQLGrammarException|could not extract ResultSet|Bad value for type long)/i;

function progress(message) {
  if (verbose) {
    console.error(`[qcs-incoming-reference] ${message}`);
  }
}

const checks = [
  {
    key: "materialReference",
    label: "物料参照",
    inputIndex: 2,
    endpointPattern: /\/BaseSet\/material\//i,
    expectedEndpointFragments: ["materialRef-query"],
    expectedText: ["物料编码", "物料名称"],
    screenshot: "02-material-reference.png",
  },
  {
    key: "pickSiteReference",
    label: "采样点参照",
    inputIndex: 3,
    endpointPattern: /\/LIMSBasic\/pickSite\//i,
    expectedEndpointFragments: ["pickSiteTreeRefTreeDataCustom", "pickSiteRefPart-query"],
    expectedText: ["采样点", "样品模板", "质量标准"],
    screenshot: "03-pick-site-reference.png",
  },
  {
    key: "supplierReference",
    label: "客商档案参照",
    inputIndex: 4,
    endpointPattern: /\/BaseSet\/cooperate\//i,
    expectedEndpointFragments: ["cmcPartRef-query"],
    expectedText: ["客商"],
    screenshot: "04-supplier-reference.png",
  },
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

async function visiblePopupFrame(page) {
  const frameLocator = page.locator("iframe:visible").last();
  await frameLocator.waitFor({ state: "visible", timeout: 20000 });
  const frame = frameLocator.contentFrame();
  if (!frame) {
    throw new Error("Visible reference iframe has no content frame");
  }
  await frame.locator("body").waitFor({ state: "visible", timeout: 20000 });
  await page.waitForTimeout(1200);
  return { frame, url: (await frameLocator.getAttribute("src")) || "UNKNOWN" };
}

async function closePopup(page, popupFrame) {
  const frameCloseButton = popupFrame && popupFrame.getByText("关闭", { exact: true }).last();
  if (frameCloseButton && (await frameCloseButton.isVisible().catch(() => false))) {
    await frameCloseButton.click({ timeout: 5000 });
    await page.waitForTimeout(300);
    return;
  }
  const closeButton = page.getByText("关闭", { exact: true }).last();
  if (await closeButton.isVisible().catch(() => false)) {
    await closeButton.click({ timeout: 5000 });
  } else {
    const closeIcon = page
      .locator(".ant-modal-close:visible, .layui-layer-close:visible, [title='关闭']:visible")
      .last();
    await closeIcon.click({ timeout: 5000 });
  }
  await page.waitForTimeout(300);
}

async function waitForExpectedResponses(page, item, fragments) {
  const deadline = Date.now() + 8000;
  while (Date.now() < deadline) {
    const complete = fragments.every((fragment) =>
      item.endpointResponses.some(
        (response) => response.url.includes(fragment) && response.status < 400
      )
    );
    if (complete) {
      return;
    }
    await page.waitForTimeout(100);
  }
  const missing = fragments.filter(
    (fragment) =>
      !item.endpointResponses.some(
        (response) => response.url.includes(fragment) && response.status < 400
      )
  );
  throw new Error(`Missing successful reference response(s): ${missing.join(", ")}`);
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
    status: "RUNNING",
    summary: { testedFeatures: 4, pass: 0, fail: 0, blocked: 0, notApplicable: 0 },
    frontend: {
      console: [],
      pageErrors: [],
      requestFailures: [],
      clientErrors: [],
      serverErrors: [],
      checks: [],
      screenshots: {},
    },
    persistence: {
      required: false,
      status: "NOT_APPLICABLE",
      reason:
        "Required-field validation and read-only reference dialogs are exercised without saving a business document.",
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
      viewport: { width: 1920, height: 1080 },
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
    page.setDefaultTimeout(15000);
    let activeCheck = "initialization";
    page.on("console", (message) => {
      if (["error", "warning"].includes(message.type())) {
        evidence.frontend.console.push({ type: message.type(), text: message.text() });
      }
    });
    page.on("pageerror", (error) => {
      evidence.frontend.pageErrors.push({
        stage: activeCheck,
        name: error.name,
        message: error.message,
        stack: error.stack,
      });
    });
    page.on("requestfailed", (item) => {
      evidence.frontend.requestFailures.push({
        method: item.method(),
        url: item.url(),
        failure: item.failure(),
      });
    });
    page.on("response", (response) => {
      if (response.status() >= 400 && response.status() < 500) {
        evidence.frontend.clientErrors.push({
          status: response.status(),
          method: response.request().method(),
          url: response.url(),
        });
      }
      if (response.status() >= 500) {
        evidence.frontend.serverErrors.push({
          status: response.status(),
          method: response.request().method(),
          url: response.url(),
        });
      }
    });

    progress("opening incoming-inspection form");
    const navigation = await page.goto(route, {
      waitUntil: "domcontentloaded",
      timeout: 60000,
    });
    await page.waitForTimeout(1800);
    evidence.frontend.navigation = {
      status: navigation && navigation.status(),
      title: await page.title(),
    };
    const searchInputs = page.locator("input.btn-search");
    await searchInputs.nth(4).waitFor({ state: "visible", timeout: 20000 });
    evidence.frontend.searchInputCount = await searchInputs.count();
    if (evidence.frontend.searchInputCount < 5) {
      throw new Error(`Expected at least 5 reference inputs, got ${evidence.frontend.searchInputCount}`);
    }
    const formScreenshot = path.join(outputDir, "01-incoming-inspection-form.png");
    await page.screenshot({ path: formScreenshot, fullPage: true });
    evidence.frontend.screenshots.form = formScreenshot;
    await page.waitForTimeout(3000);

    for (const check of checks) {
      activeCheck = check.label;
      progress(`opening ${check.label}`);
      const item = {
        key: check.key,
        label: check.label,
        status: "PASS",
        endpointResponses: [],
      };
      const initialServerErrorCount = evidence.frontend.serverErrors.length;
      let popupFrame = null;
      const responseListener = (response) => {
        if (check.endpointPattern.test(response.url())) {
          item.endpointResponses.push({
            method: response.request().method(),
            status: response.status(),
            url: response.url(),
          });
        }
      };
      page.on("response", responseListener);
      try {
        await searchInputs.nth(check.inputIndex).click();
        const popup = await visiblePopupFrame(page);
        const frame = popup.frame;
        popupFrame = frame;
        progress(`${check.label} loaded ${popup.url}`);
        const frameText = await frame.locator("body").innerText();
        item.frameUrl = popup.url;
        item.frameTextExcerpt = frameText.replace(/\s+/g, " ").slice(0, 600);
        await waitForExpectedResponses(page, item, check.expectedEndpointFragments);
        item.expectedText = check.expectedText;
        item.expectedTextVisible = check.expectedText.every((text) => frameText.includes(text));
        if (!item.expectedTextVisible) {
          throw new Error(`Missing expected text in ${check.label}: ${check.expectedText.join(", ")}`);
        }
        const visibleText = `${await page.locator("body").innerText()}\n${frameText}`;
        const visibleError = visibleText.match(visibleErrorPattern);
        if (visibleError) {
          item.visibleError = visibleError[0];
          throw new Error(`Visible error in ${check.label}: ${visibleError[0]}`);
        }
        const newServerErrors = evidence.frontend.serverErrors
          .slice(initialServerErrorCount)
          .filter((entry) => check.endpointPattern.test(entry.url));
        item.serverErrors = newServerErrors;
        if (newServerErrors.length) {
          throw new Error(`${check.label} returned ${newServerErrors.length} server error(s)`);
        }
        const screenshotPath = path.join(outputDir, check.screenshot);
        await page.screenshot({ path: screenshotPath, fullPage: true });
        evidence.frontend.screenshots[check.key] = screenshotPath;
        evidence.summary.pass += 1;
      } catch (error) {
        item.status = "FAIL";
        item.error = error.message;
        evidence.summary.fail += 1;
      } finally {
        page.off("response", responseListener);
        await closePopup(page, popupFrame).catch((error) => {
          item.closeError = error.message;
        });
        progress(`${check.label} completed with ${item.status}`);
      }
      evidence.frontend.checks.push(item);
    }

    const validation = {
      key: "requiredMaterialValidation",
      label: "未选物料点击质量标准参考校验",
      status: "PASS",
    };
    try {
      activeCheck = validation.label;
      progress("checking localized required-material warning");
      const initialServerErrorCount = evidence.frontend.serverErrors.length;
      await page.getByText("参照", { exact: true }).first().click();
      await page.waitForTimeout(900);
      const bodyText = await page.locator("body").innerText();
      validation.expectedMessage = "请先选择物料";
      validation.rawKeyVisible = bodyText.includes("QCS.Inspect.operate.warn.selectProduct");
      validation.localizedMessageVisible = bodyText.includes("请先选择物料");
      validation.bodyTextExcerpt = bodyText.replace(/\s+/g, " ").slice(-500);
      validation.submitServerErrors = evidence.frontend.serverErrors
        .slice(initialServerErrorCount)
        .filter((entry) => entry.url.includes("/purchInspectEdit/submit"));
      if (
        validation.rawKeyVisible ||
        !validation.localizedMessageVisible ||
        validation.submitServerErrors.length
      ) {
        throw new Error("Required-material warning is not localized or opened an invalid request");
      }
      const warningScreenshot = path.join(outputDir, "05-localized-material-warning.png");
      await page.screenshot({ path: warningScreenshot, fullPage: true });
      evidence.frontend.screenshots.requiredMaterialValidation = warningScreenshot;
      evidence.summary.pass += 1;
    } catch (error) {
      validation.status = "FAIL";
      validation.error = error.message;
      evidence.summary.fail += 1;
    }
    evidence.frontend.checks.push(validation);

    if (
      evidence.summary.fail ||
      evidence.frontend.pageErrors.length ||
      evidence.frontend.serverErrors.length
    ) {
      throw new Error("QCS incoming-inspection reference regression failed");
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

  console.log(
    JSON.stringify(
      {
        status: evidence.status,
        outputPath,
        summary: evidence.summary,
        checks: evidence.frontend.checks.map((item) => ({
          label: item.label,
          status: item.status,
          error: item.error,
          frameUrl: item.frameUrl,
          endpointResponses: item.endpointResponses,
        })),
        issues: evidence.issues,
      },
      null,
      2
    )
  );
  if (evidence.status !== "PASS") {
    process.exitCode = 1;
  }
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
