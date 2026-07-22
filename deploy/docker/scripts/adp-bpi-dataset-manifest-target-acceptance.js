#!/usr/bin/env node
"use strict";

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
const timeoutMs = Number(process.env.BPI_BROWSER_TIMEOUT_MS || 120_000);
const headless = process.env.BPI_HEADLESS !== "false";
const reportPath = path.resolve(
  process.env.BPI_BROWSER_REPORT || `/tmp/${marker}-dataset-manifest-target.json`,
);
const desktopScreenshot = path.resolve(
  process.env.BPI_DESKTOP_SCREENSHOT || `/tmp/${marker}-dataset-manifest-desktop.png`,
);
const mobileScreenshot = path.resolve(
  process.env.BPI_MOBILE_SCREENSHOT || `/tmp/${marker}-dataset-manifest-mobile.png`,
);

if (!/^[A-Za-z0-9_-]{8,80}$/.test(marker)) {
  throw new Error("BPI_ACCEPTANCE_MARKER must use 8-80 letters, digits, underscores or hyphens");
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

function localDateTime(secondsAgo = 5) {
  const instant = new Date(Date.now() - secondsAgo * 1_000);
  const local = new Date(instant.getTime() - instant.getTimezoneOffset() * 60_000);
  return local.toISOString().slice(0, 19);
}

function sanitizedPayload(requestValue) {
  const value = requestValue.postData();
  if (!value) return null;
  try {
    return JSON.parse(value);
  } catch (_error) {
    return value.slice(0, 2_000);
  }
}

function installEvidenceListeners(page, report) {
  page.on("console", (message) => {
    if (message.type() === "error") report.browser.consoleErrors.push(message.text());
  });
  page.on("pageerror", (error) => report.browser.pageErrors.push(error.message));
  page.on("requestfailed", (failed) => report.browser.requestFailures.push({
    method: failed.method(), url: failed.url(), error: failed.failure()?.errorText || "",
  }));
  page.on("response", async (response) => {
    const url = new URL(response.url());
    if (!url.pathname.includes("/bpi-api/")) return;
    const requestValue = response.request();
    const record = {
      method: requestValue.method(),
      url: `${url.pathname}${url.search}`,
      status: response.status(),
      payload: sanitizedPayload(requestValue),
    };
    if (requestValue.method() !== "GET") {
      const parsed = await readJson(response);
      record.response = parsed.json?.data || parsed.json || parsed.text.slice(0, 2_000);
    }
    report.browser.network.push(record);
  });
}

async function prepareContext(browser, auth, viewport) {
  const context = await browser.newContext({
    ignoreHTTPSErrors: true,
    viewport,
    extraHTTPHeaders: { Authorization: `Bearer ${auth.ticket}` },
  });
  await context.addCookies([
    { name: "suposTicket", value: auth.ticket, url: new URL(browserBaseUrl).origin },
    { name: "SUPOS_TICKET", value: auth.ticket, url: new URL(browserBaseUrl).origin },
  ]);
  await context.addInitScript(({ token, loginPayload, targetPlant, targetLine }) => {
    for (const key of ["suposTicket", "SUPOS_TICKET", "token", "ticket"]) {
      window.localStorage.setItem(key, token);
      window.sessionStorage.setItem(key, token);
    }
    if (loginPayload) window.localStorage.setItem("loginMsg", JSON.stringify(loginPayload));
    window.localStorage.setItem("language", "zh_CN");
    window.localStorage.setItem("langu_code", "zh_CN");
    window.localStorage.setItem("locale", "zh-cn");
    window.localStorage.setItem("bpi.plantId", targetPlant);
    window.localStorage.setItem("bpi.lineId", targetLine);
  }, { token: auth.ticket, loginPayload: auth.payload, targetPlant: plantId, targetLine: lineId });
  return context;
}

async function apiGet(api, ticket, endpoint) {
  const response = await api.get(`${adpBaseUrl}${endpoint}`, {
    headers: { Authorization: `Bearer ${ticket}` },
    timeout: timeoutMs,
  });
  const parsed = await readJson(response);
  assert(response.status() === 200,
    `${endpoint} returned ${response.status()}: ${parsed.text.slice(0, 500)}`);
  return parsed.json;
}

async function main() {
  for (const target of [reportPath, desktopScreenshot, mobileScreenshot]) {
    fs.mkdirSync(path.dirname(target), { recursive: true });
  }
  const api = await request.newContext({ ignoreHTTPSErrors: true });
  let browser;
  const report = {
    generatedAt: new Date().toISOString(),
    status: "FAIL",
    marker,
    scope: { tenantId: "1000", plantId, lineId },
    login: null,
    definition: null,
    snapshot: null,
    api: {},
    browser: {
      route: browserBaseUrl,
      consoleErrors: [],
      pageErrors: [],
      requestFailures: [],
      network: [],
      desktop: null,
      mobile: null,
    },
    screenshots: { desktop: desktopScreenshot, mobile: mobileScreenshot },
    databaseVerificationRequired: true,
    cleanupRequired: false,
    error: null,
  };

  try {
    const auth = await login(api);
    report.login = { status: auth.status, username };
    browser = await chromium.launch({ headless });
    const desktopContext = await prepareContext(browser, auth, { width: 1440, height: 900 });
    const page = await desktopContext.newPage();
    page.setDefaultTimeout(timeoutMs);
    installEvidenceListeners(page, report);

    await page.goto(browserBaseUrl, { waitUntil: "networkidle" });
    await page.getByRole("heading", { name: "数据集清单" }).waitFor();
    await page.locator("#open-dataset-definition").click();
    await page.locator("#dataset-code").fill(marker);
    await page.locator("#dataset-name").fill(`${marker} 目标清单`);
    await page.locator("#dataset-version").fill("1.0.0");
    await page.locator("#dataset-lines").fill(lineId);
    await page.locator("#dataset-label-delay").fill("24");
    await page.locator("#dataset-confidence").fill("1");
    await page.locator("#dataset-reason").fill(`${marker} 真实页面定义创建`);

    const definitionResponsePromise = page.waitForResponse((response) => {
      const url = new URL(response.url());
      return response.request().method() === "POST" && url.pathname.endsWith("/bpi-api/datasets");
    });
    await page.locator("#dataset-definition-submit").click();
    const definitionResponse = await definitionResponsePromise;
    const definitionBody = await readJson(definitionResponse);
    assert(definitionResponse.status() === 200,
      `dataset definition returned ${definitionResponse.status()}: ${definitionBody.text.slice(0, 500)}`);
    const definition = definitionBody.json?.data;
    assert(definition?.datasetCode === marker, "dataset definition response lost the marker");
    assert(definition?.plantId === plantId, "dataset definition response has the wrong plant");
    assert(definition?.lineIds?.length === 1 && definition.lineIds[0] === lineId,
      "dataset definition response has the wrong line scope");
    report.cleanupRequired = true;
    report.definition = {
      id: definition.id,
      revision: definition.revision,
      checksum: definition.checksum,
      state: definition.state,
    };

    const row = page.locator(`[data-dataset-id="${definition.id}"]`);
    await row.waitFor();
    await page.locator("#open-dataset-snapshot").click();
    await page.locator("#dataset-freeze-at").fill(localDateTime());
    await page.locator("#dataset-snapshot-lines").fill(lineId);
    await page.locator("#dataset-rule-versions").fill("");
    await page.locator("#dataset-snapshot-reason").fill(`${marker} 真实页面冻结清单`);

    const snapshotResponsePromise = page.waitForResponse((response) => {
      const url = new URL(response.url());
      return response.request().method() === "POST"
        && url.pathname.endsWith(`/bpi-api/datasets/${definition.id}/snapshots`);
    });
    await page.locator("#dataset-snapshot-submit").click();
    const snapshotResponse = await snapshotResponsePromise;
    const snapshotBody = await readJson(snapshotResponse);
    assert(snapshotResponse.status() === 202,
      `dataset snapshot returned ${snapshotResponse.status()}: ${snapshotBody.text.slice(0, 500)}`);
    const queued = snapshotBody.json?.data;
    assert(queued?.state === "QUEUED", "dataset snapshot did not enter QUEUED");
    report.snapshot = { id: queued.id, queuedRevision: queued.revision, queuedState: queued.state };

    await page.locator("#detail-drawer")
      .getByText("MANIFEST_READY", { exact: true }).first().waitFor({ timeout: timeoutMs });
    await page.locator(".dataset-exclusion-list")
      .getByText("CONFIDENCE_BELOW_THRESHOLD", { exact: true }).waitFor();
    await page.locator(".dataset-exclusion-list")
      .getByText("LABEL_DELAY_EXCEEDED", { exact: true }).waitFor();
    assert(await page.locator(".dataset-sample-frame tbody tr").count() === 3,
      "dataset drawer did not render exactly three in-plant samples");
    assert(await page.getByText("MANIFEST_ONLY", { exact: true }).count() > 0,
      "dataset drawer does not expose the MANIFEST_ONLY boundary");
    assert(await page.getByText("NOT_STARTED", { exact: true }).count() > 0,
      "dataset drawer does not expose the NOT_STARTED materialization state");
    await page.screenshot({ path: desktopScreenshot, fullPage: true });
    report.browser.desktop = {
      sampleRows: await page.locator(".dataset-sample-frame tbody tr").count(),
      exclusionRows: await page.locator(".dataset-exclusion-list li").count(),
    };
    await desktopContext.close();

    const snapshot = await apiGet(api, auth.ticket, `/bpi-api/dataset-snapshots/${queued.id}`);
    const ready = snapshot?.data;
    assert(ready?.state === "MANIFEST_READY", "snapshot API did not reach MANIFEST_READY");
    assert(ready?.includedCount === 1 && ready?.excludedCount === 2,
      "snapshot API returned an unexpected included/excluded projection");
    assert(ready?.materializationState === "NOT_STARTED" && ready?.artifactUri == null,
      "snapshot API crossed the manifest-only materialization boundary");
    assert(ready?.manifest?.phaseBoundary?.deliveryState === "MANIFEST_ONLY",
      "snapshot API lost the MANIFEST_ONLY boundary");
    assert(ready?.manifest?.phaseBoundary?.icebergReady === false
      && ready?.manifest?.phaseBoundary?.mlflowRegistered === false
      && ready?.manifest?.phaseBoundary?.modelTrained === false,
    "snapshot API claims unsupported downstream delivery");
    report.snapshot = {
      ...report.snapshot,
      readyRevision: ready.revision,
      readyState: ready.state,
      manifestChecksum: ready.manifestChecksum,
      includedCount: ready.includedCount,
      excludedCount: ready.excludedCount,
      materializationState: ready.materializationState,
      artifactUri: ready.artifactUri ?? null,
    };
    const definitions = await apiGet(
      api, auth.ticket, `/bpi-api/datasets?plantId=${encodeURIComponent(plantId)}&limit=100`,
    );
    const listed = definitions?.data?.find((item) => item.id === definition.id);
    assert(listed?.latestSnapshot?.id === queued.id, "dataset list did not project the ready snapshot");
    report.api = {
      definitionListed: true,
      snapshotStatus: ready.state,
      phaseBoundary: ready.manifest.phaseBoundary,
      counts: ready.manifest.counts,
    };

    const mobileContext = await prepareContext(browser, auth, { width: 390, height: 844 });
    const mobilePage = await mobileContext.newPage();
    mobilePage.setDefaultTimeout(timeoutMs);
    installEvidenceListeners(mobilePage, report);
    await mobilePage.goto(browserBaseUrl, { waitUntil: "networkidle" });
    await mobilePage.getByRole("heading", { name: "数据集清单" }).waitFor();
    const geometry = await mobilePage.evaluate(() => ({
      viewport: window.innerWidth,
      body: document.body.scrollWidth,
      document: document.documentElement.scrollWidth,
    }));
    assert(geometry.body <= geometry.viewport + 1 && geometry.document <= geometry.viewport + 1,
      `dataset page overflows the mobile viewport: ${JSON.stringify(geometry)}`);
    await mobilePage.locator(`[data-dataset-id="${definition.id}"]`).click();
    await mobilePage.locator("#open-latest-dataset-snapshot").click();
    await mobilePage.locator("#detail-drawer")
      .getByText("数据集快照 v1", { exact: true }).waitFor();
    await mobilePage.locator("#detail-drawer")
      .getByText("MANIFEST_READY", { exact: true }).first().waitFor();
    await mobilePage.locator(".dataset-sample-frame tbody tr").nth(2).waitFor();
    await mobilePage.screenshot({ path: mobileScreenshot, fullPage: true });
    report.browser.mobile = {
      ...geometry,
      readyDrawer: true,
      sampleRows: await mobilePage.locator(".dataset-sample-frame tbody tr").count(),
    };
    await mobileContext.close();
    await browser.close();
    browser = null;

    assert(report.browser.consoleErrors.length === 0, "browser emitted console errors");
    assert(report.browser.pageErrors.length === 0, "browser emitted page errors");
    assert(report.browser.requestFailures.length === 0, "browser emitted request failures");
    const writeResponses = report.browser.network.filter((item) => item.method === "POST");
    assert(writeResponses.some((item) => item.status === 200 && item.url.endsWith("/bpi-api/datasets")),
      "browser evidence is missing the dataset-definition POST");
    assert(writeResponses.some((item) => item.status === 202 && item.url.includes("/snapshots")),
      "browser evidence is missing the dataset-snapshot POST");
    report.status = "PASS_PENDING_DATABASE_VERIFICATION_AND_CLEANUP";
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
      desktopScreenshot,
      mobileScreenshot,
      datasetId: report.definition?.id || null,
      snapshotId: report.snapshot?.id || null,
      cleanupRequired: report.cleanupRequired,
    })}\n`);
  }
}

main().catch((error) => {
  process.stderr.write(`${error instanceof Error ? error.stack || error.message : String(error)}\n`);
  process.exitCode = 1;
});
