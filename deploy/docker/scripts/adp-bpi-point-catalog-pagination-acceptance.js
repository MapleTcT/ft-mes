#!/usr/bin/env node
"use strict";

const crypto = require("node:crypto");
const fs = require("node:fs");
const path = require("node:path");
const { chromium, request } = require("playwright");

const marker = required("BPI_ACCEPTANCE_MARKER");
const adpBaseUrl = required("ADP_BASE_URL").replace(/\/+$/, "");
const browserBaseUrl = required("BPI_BROWSER_BASE_URL");
const username = required("ADP_USERNAME");
const password = required("ADP_PASSWORD");
const plantId = process.env.BPI_PLANT_ID || "PLANT-01";
const lineId = process.env.BPI_LINE_ID || "LINE-S07-01";
const pageSize = Number(process.env.BPI_POINT_PAGE_SIZE || 2);
const timeoutMs = Number(process.env.BPI_BROWSER_TIMEOUT_MS || 120_000);
const headless = process.env.BPI_HEADLESS !== "false";
const reportPath = path.resolve(
  process.env.BPI_BROWSER_REPORT || `/tmp/${marker}-point-catalog-pagination.json`,
);
const screenshotPath = path.resolve(
  process.env.BPI_BROWSER_SCREENSHOT || `/tmp/${marker}-point-catalog-pagination.png`,
);

if (!/^[A-Za-z0-9_-]{8,80}$/.test(marker)) {
  throw new Error("BPI_ACCEPTANCE_MARKER must use 8-80 letters, digits, underscores or hyphens");
}
if (!Number.isInteger(pageSize) || pageSize < 1 || pageSize > 200) {
  throw new Error("BPI_POINT_PAGE_SIZE must be between 1 and 200");
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
  const statuses = [];
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
    if (ticket) return { ticket, status: response.status(), payload: parsed.json };
    statuses.push(response.status());
  }
  throw new Error(`ADP login failed with statuses ${statuses.join(",")}`);
}

async function catalogGet(api, ticket, parameters, expectedStatus = 200) {
  const response = await api.get(`${adpBaseUrl}/bpi-api/point-catalog/current?${parameters}`, {
    headers: { Authorization: `Bearer ${ticket}` },
    timeout: timeoutMs,
  });
  const parsed = await readJson(response);
  assert(response.status() === expectedStatus,
    `point catalog GET returned ${response.status()}: ${parsed.text.slice(0, 500)}`);
  return { status: response.status(), ...parsed };
}

function point(index, suffix = "PAGE") {
  const serial = String(index).padStart(3, "0");
  return {
    localityGroup: `${marker}_LOCALITY`,
    productId: `${marker}_PRODUCT_${suffix}`,
    deviceId: `${marker}_DEVICE_${serial}`,
    propertyId: `${marker.toLowerCase()}.flow.${serial}`,
    sourcePropertyId: `${marker}_source_${serial}`,
    pointName: `${marker} 分页点位 ${serial}`,
    unit: "m3/h",
    dataType: "double",
    deviceState: "ACTIVE",
    registered: true,
    propertyPresent: true,
    calibrationVersion: null,
    calibrationStatus: "MISSING",
    sourceSequenceEnabled: true,
  };
}

async function importCatalog(api, ticket, suffix, points, observedAt) {
  const response = await api.post(`${adpBaseUrl}/bpi-api/point-catalog/snapshots`, {
    headers: {
      Authorization: `Bearer ${ticket}`,
      "Idempotency-Key": `${marker}_IMPORT_${suffix}`,
      "If-Match": "0",
      "Content-Type": "application/json",
    },
    data: {
      source: "JETLINKS",
      sourceInstance: `${marker}_INSTANCE`,
      sourceRevision: `${marker}_CATALOG_${suffix}`,
      plantId,
      lineId,
      observedAt,
      points,
      reason: `${marker} 点位目录稳定分页验收`,
    },
    timeout: timeoutMs,
  });
  const parsed = await readJson(response);
  assert(response.status() === 200,
    `point catalog import returned ${response.status()}: ${parsed.text.slice(0, 500)}`);
  assert(parsed.json?.data?.snapshot?.pointCount === points.length, "imported point count is incorrect");
  return parsed.json;
}

async function readAllPages(api, ticket) {
  const pages = [];
  const ids = [];
  let cursor = null;
  let firstCursor = null;
  let snapshotId = null;
  let snapshotAt = null;
  for (let index = 0; index < 20; index += 1) {
    const parameters = new URLSearchParams({ plantId, lineId, search: marker, limit: String(pageSize) });
    if (cursor) parameters.set("cursor", cursor);
    const response = await catalogGet(api, ticket, parameters.toString());
    assert(response.json?.data?.snapshot, `page ${index + 1} has no snapshot`);
    assert(Array.isArray(response.json.data.points), `page ${index + 1} has no point array`);
    if (snapshotId === null) snapshotId = response.json.data.snapshot.id;
    if (snapshotAt === null) snapshotAt = response.json.meta?.snapshotAt;
    assert(response.json.data.snapshot.id === snapshotId, `page ${index + 1} changed snapshot id`);
    assert(response.json.meta?.snapshotAt === snapshotAt, `page ${index + 1} changed snapshotAt`);
    const pageIds = response.json.data.points.map((item) => item.id);
    ids.push(...pageIds);
    const nextCursor = response.json.meta?.nextCursor || null;
    pages.push({
      index: index + 1,
      itemCount: pageIds.length,
      ids: pageIds,
      requestCursorHash: cursor ? sha256(cursor) : null,
      nextCursorHash: nextCursor ? sha256(nextCursor) : null,
    });
    if (index === 0) firstCursor = nextCursor;
    if (!nextCursor) break;
    cursor = nextCursor;
  }
  assert(pages.length >= 2, `target needs at least two pages at limit ${pageSize}`);
  assert(pages.at(-1).nextCursorHash === null, "point catalog pagination did not terminate");
  assert(new Set(ids).size === ids.length, "point catalog pages contain duplicate ids");
  return { pages, ids, snapshotId, snapshotAt, firstCursor };
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
    baseline: null,
    importedSnapshots: [],
    api: {},
    browser: {
      requests: [], consoleErrors: [], pageErrors: [], requestFailures: [], loadedIds: [],
    },
    cleanupRequired: false,
    screenshot: screenshotPath,
    error: null,
  };

  try {
    const auth = await login(api);
    const legacyParameters = new URLSearchParams({ plantId, lineId });
    const baseline = await catalogGet(api, auth.ticket, legacyParameters.toString());
    assert(baseline.json?.data?.snapshot?.id, "target has no baseline point catalog snapshot");
    report.baseline = {
      snapshotId: baseline.json.data.snapshot.id,
      sourceRevision: baseline.json.data.snapshot.sourceRevision,
      pointCount: baseline.json.data.points.length,
    };

    const first = await importCatalog(
      api,
      auth.ticket,
      "A",
      Array.from({ length: 5 }, (_, index) => point(index)),
      new Date().toISOString(),
    );
    report.cleanupRequired = true;
    report.importedSnapshots.push(first.data.snapshot.id);
    const paged = await readAllPages(api, auth.ticket);
    assert(paged.snapshotId === first.data.snapshot.id, "paged API did not select the first marker snapshot");
    assert(paged.ids.length === 5, "paged API did not return all five marker points");
    assert(paged.firstCursor, "first page did not return a cursor");

    const tamperedParameters = new URLSearchParams({
      plantId, lineId, search: marker, limit: String(pageSize), cursor: `${paged.firstCursor}a`,
    });
    const tampered = await catalogGet(api, auth.ticket, tamperedParameters.toString(), 422);
    const wrongSearchParameters = new URLSearchParams({
      plantId, lineId, search: `${marker}_OTHER`, limit: String(pageSize), cursor: paged.firstCursor,
    });
    const wrongSearch = await catalogGet(api, auth.ticket, wrongSearchParameters.toString(), 422);
    report.api = {
      pages: paged.pages,
      totalItems: paged.ids.length,
      uniqueItems: new Set(paged.ids).size,
      snapshotId: paged.snapshotId,
      snapshotAt: paged.snapshotAt,
      tamperedCursorStatus: tampered.status,
      changedSearchStatus: wrongSearch.status,
    };

    browser = await chromium.launch({ headless });
    const context = await browser.newContext({
      ignoreHTTPSErrors: true,
      viewport: { width: 1440, height: 900 },
      extraHTTPHeaders: { Authorization: `Bearer ${auth.ticket}` },
    });
    await context.addCookies([
      { name: "suposTicket", value: auth.ticket, url: new URL(browserBaseUrl).origin },
      { name: "SUPOS_TICKET", value: auth.ticket, url: new URL(browserBaseUrl).origin },
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
    }, { token: auth.ticket, loginPayload: auth.payload, pointPlantId: plantId, pointLineId: lineId });

    const page = await context.newPage();
    page.setDefaultTimeout(timeoutMs);
    await page.route("**/bpi-api/point-catalog/current?**", async (route) => {
      const url = new URL(route.request().url());
      url.searchParams.set("limit", String(pageSize));
      await route.continue({ url: url.toString() });
    });
    page.on("console", (message) => {
      if (message.type() === "error") report.browser.consoleErrors.push(message.text());
    });
    page.on("pageerror", (error) => report.browser.pageErrors.push(error.message));
    page.on("requestfailed", (failed) => report.browser.requestFailures.push({
      method: failed.method(), url: failed.url(), error: failed.failure()?.errorText || "",
    }));
    page.on("response", (response) => {
      const url = new URL(response.url());
      if (!url.pathname.endsWith("/bpi-api/point-catalog/current")) return;
      report.browser.requests.push({
        status: response.status(),
        limit: url.searchParams.get("limit"),
        search: url.searchParams.get("search"),
        hasCursor: url.searchParams.has("cursor"),
        cursorHash: url.searchParams.has("cursor") ? sha256(url.searchParams.get("cursor")) : null,
      });
    });

    await page.goto(browserBaseUrl, { waitUntil: "networkidle" });
    await page.getByRole("heading", { name: "点位目录" }).waitFor();
    const searchResponse = page.waitForResponse((response) => {
      const url = new URL(response.url());
      return url.pathname.endsWith("/bpi-api/point-catalog/current")
        && url.searchParams.get("search") === marker;
    });
    await page.locator("#point-search").fill(marker);
    await searchResponse;
    assert(await page.locator("[data-point-id]").count() === pageSize,
      "browser initial point page did not honor the forced acceptance page size");
    for (let index = 0; index < 10; index += 1) {
      const button = page.locator("#load-more-points");
      if (await button.count() === 0) break;
      const previous = await page.locator("[data-point-id]").count();
      await button.click();
      await page.waitForFunction((count) => document.querySelectorAll("[data-point-id]").length > count, previous);
    }
    report.browser.loadedIds = await page.locator("[data-point-id]").evaluateAll(
      (rows) => rows.map((row) => row.getAttribute("data-point-id")),
    );
    assert(report.browser.loadedIds.length === 5, "browser did not incrementally load five points");
    assert(new Set(report.browser.loadedIds).size === 5, "browser rendered duplicate point ids");
    assert(await page.locator("#point-search").inputValue() === marker, "browser lost the server search value");
    assert(await page.locator("#load-more-points").count() === 0, "browser still offers a page after the last row");
    assert(report.browser.requests.some((item) => item.hasCursor), "browser did not issue a cursor request");
    await page.screenshot({ path: screenshotPath, fullPage: true });
    await context.close();
    await browser.close();
    browser = null;

    const pinnedFirstParameters = new URLSearchParams({
      plantId, lineId, search: marker, limit: String(pageSize),
    });
    const pinnedFirst = await catalogGet(api, auth.ticket, pinnedFirstParameters.toString());
    const pinnedCursor = pinnedFirst.json?.meta?.nextCursor;
    assert(pinnedCursor, "snapshot pinning probe did not receive a cursor");
    const second = await importCatalog(
      api,
      auth.ticket,
      "B",
      [point(900, "REPLACEMENT")],
      new Date(Date.now() + 1_000).toISOString(),
    );
    report.importedSnapshots.push(second.data.snapshot.id);
    const continuationParameters = new URLSearchParams({
      plantId, lineId, search: marker, limit: String(pageSize), cursor: pinnedCursor,
    });
    const continuation = await catalogGet(api, auth.ticket, continuationParameters.toString());
    assert(continuation.json.data.snapshot.id === first.data.snapshot.id,
      "cursor did not remain pinned after a newer snapshot was imported");
    const fresh = await catalogGet(api, auth.ticket, pinnedFirstParameters.toString());
    assert(fresh.json.data.snapshot.id === second.data.snapshot.id,
      "fresh request did not select the newer snapshot");
    const legacy = await catalogGet(api, auth.ticket, legacyParameters.toString());
    assert(legacy.json.data.snapshot.id === second.data.snapshot.id && legacy.json.data.points.length === 1,
      "legacy full response compatibility changed");
    report.api.pinnedContinuationSnapshotId = continuation.json.data.snapshot.id;
    report.api.freshSnapshotId = fresh.json.data.snapshot.id;
    report.api.legacyPointCount = legacy.json.data.points.length;

    assert(report.browser.consoleErrors.length === 0, "browser emitted console errors");
    assert(report.browser.pageErrors.length === 0, "browser emitted page errors");
    assert(report.browser.requestFailures.length === 0, "browser emitted request failures");
    report.status = "PASS_PENDING_CLEANUP";
  } catch (error) {
    report.error = error instanceof Error ? error.stack || error.message : String(error);
    throw error;
  } finally {
    if (browser) await browser.close();
    await api.dispose();
    fs.writeFileSync(reportPath, `${JSON.stringify(report, null, 2)}\n`);
    process.stdout.write(`${JSON.stringify({
      status: report.status,
      marker,
      reportPath,
      screenshotPath,
      baselineSnapshotId: report.baseline?.snapshotId || null,
      importedSnapshots: report.importedSnapshots,
      cleanupRequired: report.cleanupRequired,
    })}\n`);
  }
}

main().catch((error) => {
  process.stderr.write(`${error instanceof Error ? error.stack || error.message : String(error)}\n`);
  process.exitCode = 1;
});
