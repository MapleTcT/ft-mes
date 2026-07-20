#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { chromium, request } = require("playwright");

const baseUrl = required("ADP_BASE_URL").replace(/\/+$/, "");
const username = required("ADP_USERNAME");
const password = required("ADP_PASSWORD");
const batchId = required("BPI_BATCH_ID");
const expectedBatchNo = process.env.BPI_EXPECTED_BATCH_NO || "";
const timeoutMs = Number(process.env.BPI_BROWSER_TIMEOUT_MS || 120_000);
const headless = process.env.BPI_HEADLESS !== "false";
const reportPath = path.resolve(
  process.env.BPI_BROWSER_REPORT || "/tmp/bpi-quality-release-target-acceptance.json",
);
const screenshotPath = path.resolve(
  process.env.BPI_BROWSER_SCREENSHOT || "/tmp/bpi-quality-release-target-acceptance.png",
);

function required(key) {
  const value = process.env[key];
  if (!value || !value.trim()) throw new Error(`${key} is required`);
  return value.trim();
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

async function body(response) {
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
        Accept: "application/json, text/plain, */*",
        "Content-Type": "application/json;charset=UTF-8",
      },
      timeout: timeoutMs,
    });
    const parsed = await body(response);
    const ticket = response.ok() ? findTicket(parsed.json) : null;
    if (ticket) return { ticket, status: response.status(), payload: parsed.json };
    failures.push({ status: response.status(), response: parsed.text.slice(0, 500) });
  }
  throw new Error(`ADP login failed: ${JSON.stringify(failures)}`);
}

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

async function main() {
  fs.mkdirSync(path.dirname(reportPath), { recursive: true });
  fs.mkdirSync(path.dirname(screenshotPath), { recursive: true });
  const api = await request.newContext({ ignoreHTTPSErrors: true });
  let browser;
  const report = {
    generatedAt: new Date().toISOString(),
    status: "FAIL",
    database: "PostgreSQL",
    baseUrl,
    route: "/bpi/#/batches",
    batchId,
    expectedBatchNo: expectedBatchNo || null,
    loginStatus: null,
    releaseRequest: null,
    negativeChecks: {},
    page: {},
    consoleErrors: [],
    pageErrors: [],
    requestFailures: [],
    bpiHttpErrors: [],
    screenshot: screenshotPath,
    error: null,
  };

  try {
    const unauthenticated = await api.get(
      `${baseUrl}/bpi-api/batches/${encodeURIComponent(batchId)}/release`,
      { timeout: timeoutMs },
    );
    report.negativeChecks.unauthenticatedStatus = unauthenticated.status();
    assert([401, 403].includes(unauthenticated.status()),
      `unauthenticated release read returned ${unauthenticated.status()}`);

    const auth = await login(api);
    report.loginStatus = auth.status;

    const nested = await api.get(
      `${baseUrl}/bpi-api/batches/${encodeURIComponent(batchId)}/release/export`,
      {
        headers: { Authorization: `Bearer ${auth.ticket}` },
        timeout: timeoutMs,
      },
    );
    report.negativeChecks.nestedRouteStatus = nested.status();
    assert([403, 404, 405].includes(nested.status()),
      `nested release path returned ${nested.status()}`);

    browser = await chromium.launch({ headless });
    const context = await browser.newContext({
      ignoreHTTPSErrors: true,
      viewport: { width: 1600, height: 1000 },
      extraHTTPHeaders: { Authorization: `Bearer ${auth.ticket}` },
    });
    await context.addCookies([
      { name: "suposTicket", value: auth.ticket, url: baseUrl },
      { name: "SUPOS_TICKET", value: auth.ticket, url: baseUrl },
    ]);
    await context.addInitScript(({ token, loginPayload }) => {
      for (const key of ["suposTicket", "SUPOS_TICKET", "token", "ticket"]) {
        window.localStorage.setItem(key, token);
        window.sessionStorage.setItem(key, token);
      }
      window.localStorage.setItem("loginMsg", JSON.stringify(loginPayload));
      window.localStorage.setItem("language", "zh_CN");
      window.localStorage.setItem("langu_code", "zh_CN");
      window.localStorage.setItem("locale", "zh-cn");
      window.localStorage.setItem("bpi.plantId", "PLANT-01");
      window.localStorage.setItem("bpi.lineId", "LINE-S07-01");
    }, { token: auth.ticket, loginPayload: auth.payload });

    const page = await context.newPage();
    page.setDefaultTimeout(timeoutMs);
    page.on("console", (message) => {
      if (message.type() === "error") {
        report.consoleErrors.push({ text: message.text(), url: message.location().url });
      }
    });
    page.on("pageerror", (error) => report.pageErrors.push(error.message));
    page.on("requestfailed", (failed) => {
      report.requestFailures.push({
        method: failed.method(),
        url: failed.url(),
        error: failed.failure()?.errorText || "",
      });
    });
    page.on("response", (response) => {
      if (response.url().includes("/bpi-api/") && response.status() >= 400) {
        report.bpiHttpErrors.push({
          method: response.request().method(),
          url: response.url(),
          status: response.status(),
        });
      }
    });

    await page.goto(`${baseUrl}/bpi/#/batches`, {
      waitUntil: "networkidle",
      timeout: timeoutMs,
    });
    await page.getByRole("heading", { name: "批次档案" }).waitFor();
    const row = page.locator(`[data-batch-id="${batchId}"]`);
    await row.waitFor();
    if (expectedBatchNo) await row.getByText(expectedBatchNo, { exact: true }).waitFor();

    const releaseResponsePromise = page.waitForResponse((response) =>
      response.url().endsWith(`/bpi-api/batches/${batchId}/release`)
        && response.request().method() === "GET");
    await row.click();
    const releaseResponse = await releaseResponsePromise;
    const releasePayload = await body(releaseResponse);
    report.releaseRequest = {
      method: releaseResponse.request().method(),
      url: releaseResponse.url(),
      status: releaseResponse.status(),
      operationId: releaseResponse.headers()["x-bpi-operation-id"] || null,
      data: releasePayload.json?.data || null,
    };
    assert(releaseResponse.status() === 200,
      `batch release returned ${releaseResponse.status()}: ${releasePayload.text.slice(0, 500)}`);
    assert(releasePayload.json?.data?.batch?.id === batchId,
      "batch release response did not carry the selected batch");
    assert(releasePayload.json?.data?.qualityGate == null,
      "target shadow batch unexpectedly has a quality gate projection");
    assert(releasePayload.json?.data?.wmsInbound == null,
      "target shadow batch unexpectedly has a WMS inbound projection");

    const drawer = page.locator("#detail-drawer");
    await drawer.getByText("尚未进入质量放行", { exact: true }).waitFor();
    await drawer.getByText("尚未生成入库命令", { exact: true }).waitFor();
    assert(await drawer.locator('[data-release-quality="NONE"]').count() === 1,
      "quality empty state is missing");
    assert(await drawer.locator('[data-release-wms="NONE"]').count() === 1,
      "WMS empty state is missing");
    assert(await drawer.getByText("正在读取质量门和入库回执", { exact: true }).count() === 0,
      "release projection remained in the loading state");

    const geometry = await page.evaluate(() => ({
      viewportWidth: window.innerWidth,
      documentWidth: document.documentElement.scrollWidth,
      drawerWidth: document.querySelector("#detail-drawer")?.getBoundingClientRect().width || 0,
    }));
    assert(geometry.documentWidth <= geometry.viewportWidth + 1,
      `page has horizontal overflow: ${JSON.stringify(geometry)}`);
    assert(geometry.drawerWidth > 0 && geometry.drawerWidth <= geometry.viewportWidth,
      `drawer is outside viewport: ${JSON.stringify(geometry)}`);
    await page.screenshot({ path: screenshotPath, fullPage: true });

    report.page = {
      url: page.url(),
      title: await page.title(),
      batchNo: (await row.locator("strong").textContent())?.trim() || null,
      geometry,
      qualityText: "尚未进入质量放行",
      wmsText: "尚未生成入库命令",
    };
    assert(report.consoleErrors.length === 0, "browser emitted console errors");
    assert(report.pageErrors.length === 0, "browser emitted page errors");
    assert(report.requestFailures.length === 0, "browser emitted failed requests");
    assert(report.bpiHttpErrors.length === 0, "browser emitted BPI HTTP errors");
    report.status = "PASS";
    await context.close();
  } catch (error) {
    report.error = error?.message || String(error);
    throw error;
  } finally {
    report.generatedAt = new Date().toISOString();
    fs.writeFileSync(reportPath, `${JSON.stringify(report, null, 2)}\n`, "utf8");
    if (browser) await browser.close();
    await api.dispose();
  }
}

main().catch((error) => {
  console.error(error?.stack || error);
  process.exitCode = 1;
});
