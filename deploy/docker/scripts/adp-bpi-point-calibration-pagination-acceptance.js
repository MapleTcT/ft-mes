#!/usr/bin/env node
"use strict";

const crypto = require("node:crypto");
const fs = require("node:fs");
const path = require("node:path");
const { chromium, request } = require("playwright");

const marker = required("BPI_ACCEPTANCE_MARKER");
const adpBaseUrl = required("ADP_BASE_URL").replace(/\/+$/, "");
const bpiBaseUrl = required("BPI_BROWSER_BASE_URL");
const username = required("ADP_USERNAME");
const password = required("ADP_PASSWORD");
const plantId = process.env.BPI_PLANT_ID || "PLANT-01";
const lineId = process.env.BPI_LINE_ID || "LINE-S07-01";
const pageSize = Number(process.env.BPI_CALIBRATION_PAGE_SIZE || 2);
const timeoutMs = Number(process.env.BPI_BROWSER_TIMEOUT_MS || 120_000);
const headless = process.env.BPI_HEADLESS !== "false";
const reportPath = path.resolve(
  process.env.BPI_BROWSER_REPORT || `/tmp/${marker}-point-calibration-pagination.json`,
);
const screenshotPath = path.resolve(
  process.env.BPI_BROWSER_SCREENSHOT || `/tmp/${marker}-point-calibration-pagination.png`,
);

if (!/^[A-Za-z0-9_-]{8,80}$/.test(marker)) {
  throw new Error("BPI_ACCEPTANCE_MARKER must use 8-80 letters, digits, underscores or hyphens");
}
if (!Number.isInteger(pageSize) || pageSize < 1 || pageSize > 200) {
  throw new Error("BPI_CALIBRATION_PAGE_SIZE must be between 1 and 200");
}

function required(key) {
  const value = process.env[key];
  if (!value || !value.trim()) throw new Error(`${key} is required`);
  return value.trim();
}

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

function sha256(value) {
  return crypto.createHash("sha256").update(value).digest("hex");
}

function findTicket(payload) {
  return [
    payload?.ticket,
    payload?.access_token,
    payload?.token,
    payload?.data?.ticket,
    payload?.data?.access_token,
    payload?.data?.token,
    payload?.result?.ticket,
    payload?.result?.access_token,
    payload?.result?.token,
  ].find((value) => typeof value === "string" && value.length > 20);
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
  const failures = [];
  for (const body of [
    { userName: username, password, clientId: "pc_dt" },
    { username, password, clientId: "pc_dt" },
  ]) {
    const response = await api.post(`${adpBaseUrl}/inter-api/auth/login`, {
      data: body,
      headers: { Accept: "application/json", "Content-Type": "application/json;charset=UTF-8" },
      timeout: timeoutMs,
    });
    const parsed = await readJson(response);
    const ticket = response.ok() ? findTicket(parsed.json) : null;
    if (ticket) return { ticket, loginStatus: response.status(), loginPayload: parsed.json };
    failures.push(response.status());
  }
  throw new Error(`ADP login failed with statuses ${failures.join(",")}`);
}

async function adapterGet(api, ticket, parameters, expectedStatus = 200) {
  const response = await api.get(`${adpBaseUrl}/bpi-api/point-calibrations?${parameters}`, {
    headers: { Authorization: `Bearer ${ticket}` },
    timeout: timeoutMs,
  });
  const parsed = await readJson(response);
  assert(response.status() === expectedStatus,
    `point calibration GET returned ${response.status()}: ${parsed.text.slice(0, 500)}`);
  return { status: response.status(), ...parsed };
}

async function readAllPages(api, ticket) {
  const pages = [];
  const ids = [];
  let cursor = null;
  let firstCursor = null;
  let snapshotAt = null;
  for (let index = 0; index < 50; index += 1) {
    const parameters = new URLSearchParams({ plantId, lineId, limit: String(pageSize) });
    if (cursor) parameters.set("cursor", cursor);
    const response = await adapterGet(api, ticket, parameters.toString());
    assert(Array.isArray(response.json?.data), `page ${index + 1} has no data array`);
    assert(response.json.data.length <= pageSize, `page ${index + 1} exceeds requested limit`);
    const pageSnapshot = response.json.meta?.snapshotAt;
    assert(pageSnapshot, `page ${index + 1} has no snapshotAt`);
    if (snapshotAt === null) snapshotAt = pageSnapshot;
    assert(pageSnapshot === snapshotAt, `page ${index + 1} changed snapshotAt`);
    const pageIds = response.json.data.map((item) => item.id);
    assert(pageIds.every(Boolean), `page ${index + 1} contains a record without id`);
    ids.push(...pageIds);
    const nextCursor = response.json.meta?.nextCursor || null;
    pages.push({
      index: index + 1,
      status: response.status,
      itemCount: pageIds.length,
      ids: pageIds,
      snapshotAt,
      requestCursorHash: cursor ? sha256(cursor) : null,
      nextCursorHash: nextCursor ? sha256(nextCursor) : null,
    });
    if (index === 0) firstCursor = nextCursor;
    if (!nextCursor) break;
    cursor = nextCursor;
  }
  assert(pages.length >= 2, `target needs at least two pages at limit ${pageSize}`);
  assert(pages.at(-1).nextCursorHash === null, "pagination did not terminate within 50 pages");
  assert(new Set(ids).size === ids.length, "API cursor pages contain duplicate calibration ids");
  return { pages, ids, snapshotAt, firstCursor };
}

async function main() {
  fs.mkdirSync(path.dirname(reportPath), { recursive: true });
  fs.mkdirSync(path.dirname(screenshotPath), { recursive: true });
  const api = await request.newContext({ ignoreHTTPSErrors: true });
  let browser;
  const report = {
    generatedAt: new Date().toISOString(),
    status: "FAIL",
    marker,
    scope: { tenantId: "1000", plantId, lineId },
    pageSize,
    loginStatus: null,
    api: {},
    browser: {
      page: {},
      listRequests: [],
      consoleErrors: [],
      pageErrors: [],
      requestFailures: [],
      loadedIds: [],
    },
    screenshot: screenshotPath,
    error: null,
  };

  try {
    const auth = await login(api);
    report.loginStatus = auth.loginStatus;
    const paged = await readAllPages(api, auth.ticket);
    const firstCursor = paged.firstCursor;
    assert(firstCursor, "first API page did not return nextCursor");
    const tamperedCursor = `${firstCursor.startsWith("A") ? "B" : "A"}${firstCursor.slice(1)}`;
    const invalidParameters = new URLSearchParams({
      plantId,
      lineId,
      limit: String(pageSize),
      cursor: tamperedCursor,
    });
    const invalid = await adapterGet(api, auth.ticket, invalidParameters.toString(), 422);
    assert(/cursor is invalid/.test(invalid.json?.detail || ""), "tampered cursor was not rejected precisely");
    const changedScopeParameters = new URLSearchParams({
      plantId,
      lineId,
      productId: `${marker}_OTHER`,
      limit: String(pageSize),
      cursor: firstCursor,
    });
    const changedScope = await adapterGet(api, auth.ticket, changedScopeParameters.toString(), 422);
    assert(/does not match/.test(changedScope.json?.detail || ""), "scope-bound cursor was reused across filters");
    report.api = {
      snapshotAt: paged.snapshotAt,
      pages: paged.pages,
      totalItems: paged.ids.length,
      uniqueItems: new Set(paged.ids).size,
      tamperedCursorStatus: invalid.status,
      changedScopeStatus: changedScope.status,
    };

    browser = await chromium.launch({ headless });
    const context = await browser.newContext({
      ignoreHTTPSErrors: true,
      viewport: { width: 1440, height: 900 },
      extraHTTPHeaders: { Authorization: `Bearer ${auth.ticket}` },
    });
    await context.addCookies([
      { name: "suposTicket", value: auth.ticket, url: new URL(bpiBaseUrl).origin },
      { name: "SUPOS_TICKET", value: auth.ticket, url: new URL(bpiBaseUrl).origin },
    ]);
    await context.addInitScript(({ token, loginPayload, pointPlantId, pointLineId }) => {
      for (const key of ["suposTicket", "SUPOS_TICKET", "token", "ticket"]) {
        window.localStorage.setItem(key, token);
        window.sessionStorage.setItem(key, token);
      }
      if (loginPayload) window.localStorage.setItem("loginMsg", JSON.stringify(loginPayload));
      window.localStorage.setItem("language", "zh_CN");
      window.localStorage.setItem("langu_code", "zh_CN");
      window.localStorage.setItem("locale", "zh-cn");
      window.localStorage.setItem("bpi.plantId", pointPlantId);
      window.localStorage.setItem("bpi.lineId", pointLineId);
    }, {
      token: auth.ticket,
      loginPayload: auth.loginPayload,
      pointPlantId: plantId,
      pointLineId: lineId,
    });

    const page = await context.newPage();
    page.setDefaultTimeout(timeoutMs);
    await page.route("**/bpi-api/point-calibrations?**", async (route) => {
      const url = new URL(route.request().url());
      url.searchParams.set("limit", String(pageSize));
      await route.continue({ url: url.toString() });
    });
    page.on("console", (message) => {
      if (message.type() === "error") report.browser.consoleErrors.push(message.text());
    });
    page.on("pageerror", (error) => report.browser.pageErrors.push(error.message));
    page.on("requestfailed", (failed) => report.browser.requestFailures.push({
      method: failed.method(),
      url: failed.url(),
      error: failed.failure()?.errorText || "",
    }));
    page.on("response", (response) => {
      const url = new URL(response.url());
      if (!url.pathname.endsWith("/bpi-api/point-calibrations")) return;
      report.browser.listRequests.push({
        status: response.status(),
        limit: url.searchParams.get("limit"),
        hasCursor: url.searchParams.has("cursor"),
        cursorHash: url.searchParams.has("cursor")
          ? sha256(url.searchParams.get("cursor"))
          : null,
      });
    });

    await page.goto(bpiBaseUrl, { waitUntil: "networkidle" });
    await page.getByRole("heading", { name: "点位目录" }).waitFor();
    assert(await page.locator("[data-calibration-row]").count() === pageSize,
      "browser initial calibration page did not honor the requested page size");
    await page.locator("#point-search").fill("ADP_E2E_CAL");
    for (let index = 0; index < 50; index += 1) {
      const button = page.getByRole("button", { name: "加载更多" });
      if (await button.count() === 0) break;
      const before = await page.locator("[data-calibration-row]").count();
      await button.click();
      await page.waitForFunction(
        (count) => document.querySelectorAll("[data-calibration-row]").length > count,
        before,
      );
    }
    const browserIds = await page.locator("[data-calibration-row]").evaluateAll(
      (rows) => rows.map((row) => row.getAttribute("data-calibration-row")),
    );
    assert(browserIds.length === paged.ids.length,
      `browser loaded ${browserIds.length}, API snapshot has ${paged.ids.length}`);
    assert(new Set(browserIds).size === browserIds.length, "browser rendered duplicate calibration ids");
    assert(await page.locator("#point-search").inputValue() === "ADP_E2E_CAL",
      "browser lost the point search value while appending a page");
    assert(await page.getByRole("button", { name: "加载更多" }).count() === 0,
      "browser still exposes a load-more action after the final page");
    assert(report.browser.listRequests.length >= 2, "browser did not issue a cursor follow-up request");
    assert(report.browser.listRequests[0].hasCursor === false, "browser first request unexpectedly had a cursor");
    assert(report.browser.listRequests.slice(1).every((entry) => entry.hasCursor),
      "browser follow-up request omitted the cursor");
    assert(report.browser.consoleErrors.length === 0, "browser emitted console errors");
    assert(report.browser.pageErrors.length === 0, "browser emitted page errors");
    assert(report.browser.requestFailures.length === 0, "browser emitted request failures");
    await page.screenshot({ path: screenshotPath, fullPage: true });
    report.browser.loadedIds = browserIds;
    report.browser.page = { url: page.url(), title: await page.title() };
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
