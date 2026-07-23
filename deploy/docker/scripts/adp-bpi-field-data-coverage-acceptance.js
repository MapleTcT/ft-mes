#!/usr/bin/env node
"use strict";

const fs = require("node:fs");
const path = require("node:path");
const { chromium, request } = require("playwright");

const marker = required("BPI_ACCEPTANCE_MARKER");
const adpBaseUrl = required("ADP_BASE_URL").replace(/\/+$/, "");
const bpiBaseUrl = `${required("BPI_BROWSER_BASE_URL").replace(/#.*$/, "").replace(/\/+$/, "")}/`;
const username = required("ADP_USERNAME");
const password = required("ADP_PASSWORD");
const plantId = process.env.BPI_PLANT_ID || "PLANT-01";
const lineId = process.env.BPI_LINE_ID || "LINE-FIELD-COVERAGE-01";
const timeoutMs = Number(process.env.BPI_BROWSER_TIMEOUT_MS || 120_000);
const headless = process.env.BPI_HEADLESS !== "false";
const reportPath = path.resolve(
  process.env.BPI_BROWSER_REPORT || `/tmp/${marker}-field-data-coverage.json`,
);
const desktopScreenshot = path.resolve(
  process.env.BPI_DESKTOP_SCREENSHOT || `/tmp/${marker}-field-data-coverage-desktop.png`,
);
const mobileScreenshot = path.resolve(
  process.env.BPI_MOBILE_SCREENSHOT || `/tmp/${marker}-field-data-coverage-mobile.png`,
);

if (!/^[A-Za-z0-9_-]{8,80}$/.test(marker)) {
  throw new Error("BPI_ACCEPTANCE_MARKER must use 8-80 letters, digits, underscores or hyphens");
}
if (!Number.isInteger(timeoutMs) || timeoutMs < 10_000) {
  throw new Error("BPI_BROWSER_TIMEOUT_MS must be an integer of at least 10000");
}

function required(key) {
  const value = process.env[key];
  if (!value || !value.trim()) throw new Error(`${key} is required`);
  return value.trim();
}

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

function findTicket(payload) {
  const candidates = [
    payload?.ticket,
    payload?.access_token,
    payload?.token,
    payload?.data?.ticket,
    payload?.data?.access_token,
    payload?.data?.token,
    payload?.result?.ticket,
    payload?.result?.access_token,
    payload?.result?.token,
  ];
  return candidates.find((value) => typeof value === "string" && value.length > 20);
}

async function readJson(response) {
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
    const response = await api.post(`${adpBaseUrl}/inter-api/auth/login`, {
      data: body,
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json;charset=UTF-8",
      },
      timeout: timeoutMs,
    });
    const parsed = await readJson(response);
    const ticket = response.ok() ? findTicket(parsed.json) : null;
    if (ticket) return { ticket, status: response.status(), payload: parsed.json };
    failures.push(response.status());
  }
  throw new Error(`ADP login failed with statuses ${failures.join(",")}`);
}

async function adapterGet(api, ticket, route) {
  const response = await api.get(`${adpBaseUrl}/bpi-api${route}`, {
    headers: { Accept: "application/json", Authorization: `Bearer ${ticket}` },
    timeout: timeoutMs,
  });
  const parsed = await readJson(response);
  assert(response.status() === 200,
    `GET ${route} returned ${response.status()}: ${parsed.text.slice(0, 1000)}`);
  return parsed.json?.data;
}

function observe(page, report) {
  page.on("console", (message) => {
    if (message.type() === "error") report.consoleErrors.push(message.text());
  });
  page.on("pageerror", (error) => report.pageErrors.push(error.message));
  page.on("requestfailed", (failed) => report.requestFailures.push({
    method: failed.method(),
    url: failed.url(),
    error: failed.failure()?.errorText || "",
  }));
  page.on("response", async (response) => {
    if (!response.url().includes("/bpi-api/")) return;
    const requestValue = response.request();
    let responseBody = "";
    try {
      responseBody = (await response.text()).slice(0, 4000);
    } catch (_error) {
      responseBody = "<unavailable>";
    }
    report.responses.push({
      method: requestValue.method(),
      path: new URL(response.url()).pathname,
      requestBody: (requestValue.postData() || "").slice(0, 4000),
      status: response.status(),
      responseBody,
    });
  });
}

async function newContext(browser, auth, viewport) {
  const context = await browser.newContext({
    ignoreHTTPSErrors: true,
    viewport,
    extraHTTPHeaders: { Authorization: `Bearer ${auth.ticket}` },
  });
  const origin = new URL(bpiBaseUrl).origin;
  await context.addCookies([
    { name: "suposTicket", value: auth.ticket, url: origin },
    { name: "SUPOS_TICKET", value: auth.ticket, url: origin },
  ]);
  await context.addInitScript(({ token, loginPayload, targetPlantId, targetLineId }) => {
    for (const key of ["suposTicket", "SUPOS_TICKET", "token", "ticket"]) {
      window.localStorage.setItem(key, token);
      window.sessionStorage.setItem(key, token);
    }
    if (loginPayload) window.localStorage.setItem("loginMsg", JSON.stringify(loginPayload));
    window.localStorage.setItem("language", "zh_CN");
    window.localStorage.setItem("langu_code", "zh_CN");
    window.localStorage.setItem("locale", "zh-cn");
    window.localStorage.setItem("bpi.plantId", targetPlantId);
    window.localStorage.setItem("bpi.lineId", targetLineId);
    window.localStorage.setItem("bpi.shadowRunLineId", targetLineId);
    window.localStorage.setItem("bpi.shadowRunState", "");
  }, {
    token: auth.ticket,
    loginPayload: auth.payload,
    targetPlantId: plantId,
    targetLineId: lineId,
  });
  return context;
}

async function assertCoverage(page) {
  const source = page.locator("[data-source-coverage]");
  await source.getByRole("heading", { name: "固定来源可信度" }).waitFor();
  assert(await source.getByText("2 / 2", { exact: true }).count() === 6,
    "source coverage must show six independent 2 / 2 checks");
  await source.getByText("按本次运行固定的点位目录快照统计，后续目录变化不会改写本次验收来源。", {
    exact: true,
  }).waitFor();

  const training = page.locator("[data-training-data-coverage]");
  await training.getByRole("heading", { name: "现场数据覆盖" }).waitFor();
  await training.getByText("0 / 200", { exact: true }).waitFor();
  await training.getByText("0 / 7", { exact: true }).waitFor();
  await training.getByText("0 / 100", { exact: true }).waitFor();
  await training.getByText("0 / 10", { exact: true }).waitFor();
  for (const code of [
    "TRAINING_REVIEWED_BATCHES_BELOW_MINIMUM",
    "TRAINING_PRODUCTION_DAYS_BELOW_MINIMUM",
    "TRAINING_ACCEPTED_START_LABELS_BELOW_MINIMUM",
    "TRAINING_REJECTED_START_LABELS_BELOW_MINIMUM",
  ]) {
    await training.getByText(code, { exact: true }).waitFor();
  }
  await training.getByText("仅表示现场数据覆盖进度，不代表允许训练", {
    exact: true,
  }).waitFor();
}

async function assertDrawerSettled(page) {
  const drawer = page.locator("#detail-drawer");
  await drawer.waitFor({ state: "visible" });
  await page.waitForFunction(() => {
    const element = document.querySelector("#detail-drawer");
    if (!element) return false;
    const box = element.getBoundingClientRect();
    return box.left >= -1
      && box.right <= window.innerWidth + 1
      && Math.abs(box.right - window.innerWidth) <= 1;
  });
  const geometry = await drawer.evaluate((element) => {
    const drawerBox = element.getBoundingClientRect();
    const actions = [...element.querySelectorAll(".drawer-actions button")].map((button) => {
      const box = button.getBoundingClientRect();
      return {
        label: button.textContent?.trim() || "",
        left: box.left,
        right: box.right,
      };
    });
    return {
      viewport: window.innerWidth,
      left: drawerBox.left,
      right: drawerBox.right,
      width: drawerBox.width,
      actions,
    };
  });
  assert(geometry.actions.every((action) =>
    action.left >= geometry.left - 1 && action.right <= geometry.right + 1),
  `drawer actions exceed viewport: ${JSON.stringify(geometry)}`);
  return geometry;
}

async function main() {
  fs.mkdirSync(path.dirname(reportPath), { recursive: true });
  fs.mkdirSync(path.dirname(desktopScreenshot), { recursive: true });
  fs.mkdirSync(path.dirname(mobileScreenshot), { recursive: true });
  const api = await request.newContext({ ignoreHTTPSErrors: true });
  let browser;
  const report = {
    generatedAt: new Date().toISOString(),
    status: "FAIL",
    marker,
    scope: { tenantId: "1000", plantId, lineId },
    loginStatus: null,
    runId: null,
    api: {},
    browser: {
      desktop: null,
      mobile: null,
      responses: [],
      consoleErrors: [],
      pageErrors: [],
      requestFailures: [],
    },
    boundaries: {
      syntheticReviewedBatchesCreated: 0,
      syntheticProductionDaysCreated: 0,
      modelTrainingStarted: false,
      modelRegistered: false,
      onlineInferenceEnabled: false,
      productionActivationAllowed: false,
    },
    error: null,
  };

  try {
    const auth = await login(api);
    report.loginStatus = auth.status;
    browser = await chromium.launch({ headless });

    const desktopContext = await newContext(
      browser,
      auth,
      { width: 1440, height: 900 },
    );
    const desktopPage = await desktopContext.newPage();
    desktopPage.setDefaultTimeout(timeoutMs);
    observe(desktopPage, report.browser);
    await desktopPage.goto(`${bpiBaseUrl}#/shadowRuns`, { waitUntil: "networkidle" });
    await desktopPage.getByRole("heading", { name: "影子运行验收" }).waitFor();
    await desktopPage.getByRole("button", { name: "新建影子运行" }).click();
    await desktopPage.getByRole("heading", { name: "新建影子运行" }).waitFor();
    await desktopPage.locator("#shadow-run-code").fill(marker);
    await desktopPage.locator("#shadow-run-name").fill(`${marker} 现场数据覆盖验收`);
    await desktopPage.locator("#shadow-run-line").fill(lineId);
    const ruleId = await desktopPage.locator("#shadow-run-rule").inputValue();
    assert(/^[0-9a-f-]{36}$/i.test(ruleId), "published marker rule was not selected");
    await desktopPage.locator("#shadow-run-reason").fill(
      `${marker} verify pinned source and honest training-data deficits`,
    );
    const createResponsePromise = desktopPage.waitForResponse((response) =>
      response.request().method() === "POST"
      && new URL(response.url()).pathname === "/bpi-api/shadow-runs");
    await desktopPage.getByRole("button", { name: "创建验收任务" }).click();
    const createResponse = await createResponsePromise;
    const createParsed = await readJson(createResponse);
    assert(createResponse.status() === 200,
      `create shadow run returned ${createResponse.status()}: ${createParsed.text}`);
    const run = createParsed.json?.data;
    assert(run?.state === "DRAFT" && run?.revision === 1,
      `created run is not DRAFT/r1: ${JSON.stringify(run)}`);
    report.runId = run.id;
    report.api.create = {
      method: "POST",
      path: "/bpi-api/shadow-runs",
      status: createResponse.status(),
      request: JSON.parse(createResponse.request().postData() || "{}"),
      response: { id: run.id, state: run.state, revision: run.revision },
    };

    await desktopPage.getByRole("heading", {
      name: `${marker} 现场数据覆盖验收`,
    }).waitFor();
    await assertCoverage(desktopPage);
    const desktopDrawer = await assertDrawerSettled(desktopPage);
    assert(await desktopPage.getByRole("button", { name: "启动影子运行" }).isEnabled(),
      "pinned source readiness should permit starting the shadow run");

    const apiRun = await adapterGet(api, auth.ticket, `/shadow-runs/${run.id}`);
    assert(apiRun.sourceCoverage.fullyReady === true,
      `API source coverage is not ready: ${JSON.stringify(apiRun.sourceCoverage)}`);
    for (const key of [
      "pinnedPointCount",
      "activeRegisteredPointCount",
      "physicalIdentityPointCount",
      "freshSequenceQualifiedPointCount",
      "approvedCalibrationPointCount",
      "readyPointCount",
    ]) {
      assert(apiRun.sourceCoverage[key] === 2,
        `API source coverage ${key} is ${apiRun.sourceCoverage[key]}`);
    }
    assert(apiRun.trainingDataCoverage.reviewedBatchCount === 0,
      "target acceptance must not fabricate reviewed batches");
    assert(apiRun.trainingDataCoverage.distinctProductionDayCount === 0,
      "target acceptance must not fabricate production days");
    assert(apiRun.trainingDataCoverage.acceptedStartLabelCount === 0,
      "target acceptance must not fabricate accepted START labels");
    assert(apiRun.trainingDataCoverage.rejectedStartLabelCount === 0,
      "target acceptance must not fabricate rejected START labels");
    assert(apiRun.trainingDataCoverage.thresholdsMet === false,
      "training coverage must remain below threshold");
    assert(apiRun.readyForApproval === false,
      "new coverage projection must not change shadow approval semantics");
    report.api.read = {
      method: "GET",
      path: `/bpi-api/shadow-runs/${run.id}`,
      status: 200,
      sourceCoverage: apiRun.sourceCoverage,
      trainingDataCoverage: apiRun.trainingDataCoverage,
      readyForApproval: apiRun.readyForApproval,
    };

    await desktopPage.screenshot({ path: desktopScreenshot, fullPage: true });
    report.browser.desktop = {
      viewport: "1440x900",
      drawer: desktopDrawer,
      screenshot: desktopScreenshot,
    };

    const mobileContext = await newContext(
      browser,
      auth,
      { width: 390, height: 844 },
    );
    const mobilePage = await mobileContext.newPage();
    mobilePage.setDefaultTimeout(timeoutMs);
    observe(mobilePage, report.browser);
    await mobilePage.goto(`${bpiBaseUrl}#/shadowRuns`, { waitUntil: "networkidle" });
    await mobilePage.locator(`[data-shadow-run-id="${run.id}"]`).click();
    await assertCoverage(mobilePage);
    const mobileDrawer = await assertDrawerSettled(mobilePage);
    const dimensions = await mobilePage.evaluate(() => ({
      viewport: document.documentElement.clientWidth,
      body: document.body.scrollWidth,
      document: document.documentElement.scrollWidth,
    }));
    assert(dimensions.body <= dimensions.viewport && dimensions.document <= dimensions.viewport,
      `mobile page overflow: ${JSON.stringify(dimensions)}`);
    await mobilePage.screenshot({ path: mobileScreenshot, fullPage: true });
    report.browser.mobile = {
      ...dimensions,
      drawer: mobileDrawer,
      screenshot: mobileScreenshot,
    };
    await mobileContext.close();

    await desktopPage.getByRole("button", { name: "取消任务" }).click();
    await desktopPage.getByRole("heading", { name: "取消影子运行" }).waitFor();
    await desktopPage.locator("#confirm-reason").fill(
      `${marker} target evidence captured without creating training history`,
    );
    const cancelResponsePromise = desktopPage.waitForResponse((response) =>
      response.request().method() === "POST"
      && response.url().includes(`/bpi-api/shadow-runs/${run.id}/cancel`));
    await desktopPage.getByRole("button", { name: "确认取消" }).click();
    const cancelResponse = await cancelResponsePromise;
    const cancelParsed = await readJson(cancelResponse);
    assert(cancelResponse.status() === 200,
      `cancel shadow run returned ${cancelResponse.status()}: ${cancelParsed.text}`);
    await desktopPage.getByText("影子运行已取消").waitFor();
    await desktopPage.locator(".batch-state-band").getByText("CANCELLED", {
      exact: true,
    }).waitFor();
    const cancelled = await adapterGet(api, auth.ticket, `/shadow-runs/${run.id}`);
    assert(cancelled.state === "CANCELLED" && cancelled.revision === 2,
      `cancelled run is ${cancelled.state}/r${cancelled.revision}`);
    report.api.cancel = {
      method: "POST",
      path: `/bpi-api/shadow-runs/${run.id}/cancel`,
      status: cancelResponse.status(),
      response: { state: cancelled.state, revision: cancelled.revision },
    };
    await desktopContext.close();

    assert(report.browser.consoleErrors.length === 0,
      `browser console errors: ${report.browser.consoleErrors.join(" | ")}`);
    assert(report.browser.pageErrors.length === 0,
      `browser page errors: ${report.browser.pageErrors.join(" | ")}`);
    assert(report.browser.requestFailures.length === 0,
      `browser request failures: ${JSON.stringify(report.browser.requestFailures)}`);
    const unexpectedResponses = report.browser.responses.filter((item) =>
      item.status < 200 || item.status >= 300);
    assert(unexpectedResponses.length === 0,
      `real page emitted non-2xx BPI responses: ${JSON.stringify(unexpectedResponses)}`);
    report.status = "PASS";
  } catch (error) {
    report.error = error instanceof Error ? error.stack || error.message : String(error);
    throw error;
  } finally {
    report.generatedAt = new Date().toISOString();
    fs.writeFileSync(reportPath, `${JSON.stringify(report, null, 2)}\n`, {
      mode: 0o600,
    });
    await browser?.close();
    await api.dispose();
  }
}

main()
  .then(() => console.log(`BPI field-data coverage target acceptance PASS: ${reportPath}`))
  .catch((error) => {
    console.error(`BPI field-data coverage target acceptance FAIL: ${error.message}`);
    process.exitCode = 1;
  });
