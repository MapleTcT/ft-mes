#!/usr/bin/env node
"use strict";

const fs = require("node:fs");
const path = require("node:path");
const { chromium, request } = require("playwright");

const marker = required("BPI_ACCEPTANCE_MARKER");
const action = process.env.BPI_BROWSER_ACTION || "request-ready";
const adpBaseUrl = required("ADP_BASE_URL").replace(/\/+$/, "");
const browserBaseUrl = required("BPI_BROWSER_BASE_URL");
const username = required("ADP_USERNAME");
const password = required("ADP_PASSWORD");
const plantId = process.env.BPI_PLANT_ID || "PLANT-01";
const lineId = process.env.BPI_LINE_ID || "LINE-S07-01";
const expectedFailureCode = process.env.BPI_EXPECTED_FAILURE_CODE || "SOURCE_OBJECT_ERROR";
const timeoutMs = Number(process.env.BPI_BROWSER_TIMEOUT_MS || 180_000);
const headless = process.env.BPI_HEADLESS !== "false";
const reportPath = path.resolve(
  process.env.BPI_BROWSER_REPORT || `/tmp/${marker}-dataset-catalog-${action}.json`,
);
const desktopScreenshot = path.resolve(
  process.env.BPI_DESKTOP_SCREENSHOT || `/tmp/${marker}-dataset-catalog-${action}.png`,
);
const mobileScreenshot = path.resolve(
  process.env.BPI_MOBILE_SCREENSHOT || `/tmp/${marker}-dataset-catalog-${action}-mobile.png`,
);

if (!/^[A-Za-z0-9_-]{8,80}$/.test(marker)) {
  throw new Error("BPI_ACCEPTANCE_MARKER must use 8-80 letters, digits, underscores or hyphens");
}
if (!["request-failure", "request-ready", "retry-ready", "read-ready"].includes(action)) {
  throw new Error(`Unsupported BPI_BROWSER_ACTION: ${action}`);
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

async function resolveTarget(api, ticket) {
  const definitions = await apiGet(
    api, ticket, `/bpi-api/datasets?plantId=${encodeURIComponent(plantId)}&limit=100`,
  );
  const definition = definitions?.data?.find((item) => item.datasetCode === marker);
  assert(definition, `dataset definition ${marker} was not found`);
  assert(definition.plantId === plantId && definition.lineIds?.includes(lineId),
    "dataset definition scope does not match the acceptance target");
  assert(definition.latestSnapshot?.id, "dataset definition has no latest snapshot");
  const snapshotPayload = await apiGet(
    api, ticket, `/bpi-api/dataset-snapshots/${definition.latestSnapshot.id}`,
  );
  const snapshot = snapshotPayload?.data;
  assert(snapshot?.state === "MANIFEST_READY", "dataset snapshot is not MANIFEST_READY");
  const materialization = snapshot.latestMaterialization;
  assert(materialization?.state === "READY", "dataset materialization is not READY");
  const publicationPayload = await apiGet(
    api,
    ticket,
    `/bpi-api/dataset-materializations/${materialization.id}/catalog-publications`,
  );
  return { definition, snapshot, materialization, publication: publicationPayload?.data || null };
}

function assertReadyPublication(publication, materialization) {
  assert(publication?.state === "READY", "catalog publication did not reach READY");
  assert(/^\d+$/.test(publication.icebergSnapshotId || ""), "Iceberg snapshot id is invalid");
  assert(/^ft_mes_bpi\.bpi_training\.tenant_[a-f0-9]{16}\.dataset_[a-f0-9]{32}$/
    .test(publication.tableIdentifier || ""), "Iceberg table identifier is invalid");
  assert(/^s3:\/\/bpi-iceberg-warehouse\/.*\/metadata\/.*\.metadata\.json$/
    .test(publication.icebergMetadataLocation || ""), "Iceberg metadata location is invalid");
  assert(publication.icebergSchemaId === 0 && publication.icebergPartitionSpecId === 1,
    "Iceberg schema or partition spec id is invalid");
  assert(publication.sourceRowCount > 0
    && publication.verifiedRowCount === publication.sourceRowCount,
  "catalog publication row counts do not reconcile");
  assert(/^[a-f0-9]{64}$/.test(publication.semanticChecksum || ""),
    "catalog publication semantic checksum is invalid");
  assert(publication.sourceContentSha256 === materialization.contentSha256,
    "catalog publication source SHA differs from the materialization");
  assert(publication.sourceObjectVersionId === materialization.artifactMetadata?.objectVersionId,
    "catalog publication source version differs from the materialization");
  assert(publication.catalogMetadata?.catalogSnapshotVerified === true,
    "catalog publication does not record an exact snapshot verification");
  assert(publication.catalogMetadata?.icebergReady === true
    && publication.catalogMetadata?.mlflowRegistered === false
    && publication.catalogMetadata?.modelTrained === false,
  "catalog publication crossed an unsupported delivery boundary");
}

async function openSnapshotDrawer(page, definition) {
  await page.goto(browserBaseUrl, { waitUntil: "networkidle" });
  await page.getByRole("heading", { name: "数据集清单" }).waitFor();
  const row = page.locator(`[data-dataset-id="${definition.id}"]`);
  await row.waitFor();
  await row.click();
  await page.locator("#open-latest-dataset-snapshot").click();
  await page.locator("#detail-drawer")
    .getByText("MANIFEST_READY", { exact: true }).first().waitFor();
  await page.locator('[data-materialization-state="READY"]').waitFor();
}

async function submitPublication(page, target, command) {
  const button = command === "retry"
    ? "#retry-dataset-catalog-publication" : "#open-dataset-catalog-publication";
  const endpoint = command === "retry"
    ? `/bpi-api/dataset-catalog-publications/${target.publication.id}/retry`
    : `/bpi-api/dataset-materializations/${target.materialization.id}/catalog-publications`;
  await page.locator(button).click();
  await page.getByRole("heading", {
    name: command === "retry" ? "重新排队 Iceberg 发布" : "发布版本锁定对象",
  }).waitFor();
  await page.locator("#dataset-catalog-publication-reason")
    .fill(`${marker} ${command === "retry" ? "页面重试" : "真实页面发布"}`);
  const responsePromise = page.waitForResponse((response) => {
    const url = new URL(response.url());
    return response.request().method() === "POST" && url.pathname.endsWith(endpoint);
  });
  await page.locator("#dataset-catalog-publication-submit").click();
  const response = await responsePromise;
  const parsed = await readJson(response);
  assert(response.status() === 202,
    `catalog publication returned ${response.status()}: ${parsed.text.slice(0, 500)}`);
  assert(parsed.json?.data?.id, "catalog publication response has no id");
  return parsed.json.data;
}

async function assertReadyPanel(page, publication) {
  const panel = page.locator('[data-catalog-state="READY"]');
  await panel.waitFor({ timeout: timeoutMs });
  await panel.getByText("VERIFIED", { exact: true }).waitFor();
  assert((await panel.locator(".dataset-iceberg-snapshot").textContent()) === publication.icebergSnapshotId,
    "page Iceberg snapshot differs from the API");
  assert((await panel.locator(".dataset-table-identifier").textContent()) === publication.tableIdentifier,
    "page table identifier differs from the API");
  assert((await panel.locator(".dataset-semantic-sha").textContent()) === publication.semanticChecksum,
    "page semantic checksum differs from the API");
}

async function runDesktop(page, api, auth, target, report) {
  await openSnapshotDrawer(page, target.definition);
  let publication = target.publication;
  if (action === "request-failure") {
    assert(publication == null, "request-failure requires no existing publication");
    publication = await submitPublication(page, target, "request");
    const panel = page.locator('[data-catalog-state="FAILED"]');
    await panel.waitFor({ timeout: timeoutMs });
    await panel.getByText(expectedFailureCode, { exact: true }).waitFor();
    const payload = await apiGet(
      api, auth.ticket, `/bpi-api/dataset-catalog-publications/${publication.id}`,
    );
    publication = payload.data;
    assert(publication.state === "FAILED", "catalog failure was not persisted");
    assert(publication.attemptCount === 1, "failed publication did not record one attempt");
    assert(publication.failureCode === expectedFailureCode,
      `unexpected failure code: ${publication.failureCode}`);
    assert(publication.icebergSnapshotId == null,
      "source-object failure unexpectedly persisted an Iceberg snapshot");
  } else if (action === "request-ready") {
    assert(publication == null, "request-ready requires no existing publication");
    publication = await submitPublication(page, target, "request");
    await page.locator('[data-catalog-state="READY"]').waitFor({ timeout: timeoutMs });
    publication = (await apiGet(
      api, auth.ticket, `/bpi-api/dataset-catalog-publications/${publication.id}`,
    )).data;
    assertReadyPublication(publication, target.materialization);
    await assertReadyPanel(page, publication);
  } else if (action === "retry-ready") {
    assert(publication?.state === "FAILED", "retry-ready requires a FAILED publication");
    const queued = await submitPublication(page, target, "retry");
    assert(queued.id === publication.id, "retry created another publication id");
    await page.locator('[data-catalog-state="READY"]').waitFor({ timeout: timeoutMs });
    publication = (await apiGet(
      api, auth.ticket, `/bpi-api/dataset-catalog-publications/${publication.id}`,
    )).data;
    assertReadyPublication(publication, target.materialization);
    assert(publication.attemptCount === 2, "retried publication did not record two attempts");
    await assertReadyPanel(page, publication);
  } else {
    assertReadyPublication(publication, target.materialization);
    await assertReadyPanel(page, publication);
  }
  await page.screenshot({ path: desktopScreenshot, fullPage: true });
  report.publication = publication;
}

async function runRediscovery(page, target, publication) {
  await page.reload({ waitUntil: "networkidle" });
  await openSnapshotDrawer(page, target.definition);
  await assertReadyPanel(page, publication);
}

async function runMobile(browser, auth, target, publication, report) {
  const context = await prepareContext(browser, auth, { width: 390, height: 844 });
  const page = await context.newPage();
  page.setDefaultTimeout(timeoutMs);
  installEvidenceListeners(page, report);
  await openSnapshotDrawer(page, target.definition);
  await assertReadyPanel(page, publication);
  const geometry = await page.evaluate(() => ({
    viewport: window.innerWidth,
    body: document.body.scrollWidth,
    document: document.documentElement.scrollWidth,
    drawerScrollTop: document.querySelector("#detail-drawer")?.scrollTop ?? -1,
    drawerClientWidth: document.querySelector("#detail-drawer")?.clientWidth ?? -1,
    drawerScrollWidth: document.querySelector("#detail-drawer")?.scrollWidth ?? -1,
  }));
  assert(geometry.body <= geometry.viewport + 1 && geometry.document <= geometry.viewport + 1,
    `dataset catalog page overflows mobile viewport: ${JSON.stringify(geometry)}`);
  assert(geometry.drawerScrollTop === 0, "opening the catalog drawer did not reset mobile scroll");
  assert(geometry.drawerScrollWidth <= geometry.drawerClientWidth + 1,
    `dataset catalog drawer overflows mobile viewport: ${JSON.stringify(geometry)}`);
  await page.screenshot({ path: mobileScreenshot, fullPage: true });
  report.browser.mobile = { ...geometry, readyPanel: true };
  await context.close();
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
    action,
    scope: { tenantId: "1000", plantId, lineId },
    login: null,
    definition: null,
    snapshot: null,
    materialization: null,
    publication: null,
    browser: {
      route: browserBaseUrl,
      consoleErrors: [],
      pageErrors: [],
      requestFailures: [],
      network: [],
      mobile: null,
    },
    screenshots: { desktop: desktopScreenshot, mobile: mobileScreenshot },
    databaseVerificationRequired: true,
    catalogVerificationRequired: action !== "request-failure",
    cleanupRequired: true,
    error: null,
  };

  try {
    const auth = await login(api);
    report.login = { status: auth.status, username };
    const target = await resolveTarget(api, auth.ticket);
    report.definition = { id: target.definition.id, datasetCode: target.definition.datasetCode };
    report.snapshot = {
      id: target.snapshot.id,
      state: target.snapshot.state,
      revision: target.snapshot.revision,
      manifestChecksum: target.snapshot.manifestChecksum,
    };
    report.materialization = target.materialization;

    browser = await chromium.launch({ headless });
    const context = await prepareContext(browser, auth, { width: 1440, height: 900 });
    const page = await context.newPage();
    page.setDefaultTimeout(timeoutMs);
    installEvidenceListeners(page, report);
    await runDesktop(page, api, auth, target, report);
    if (report.publication?.state === "READY") {
      await runRediscovery(page, target, report.publication);
      await runMobile(browser, auth, target, report.publication, report);
    }
    await context.close();
    await browser.close();
    browser = null;

    assert(report.browser.consoleErrors.length === 0, "browser emitted console errors");
    assert(report.browser.pageErrors.length === 0, "browser emitted page errors");
    assert(report.browser.requestFailures.length === 0, "browser emitted request failures");
    const writeResponses = report.browser.network.filter((item) => item.method === "POST");
    if (action.startsWith("request")) {
      assert(writeResponses.some((item) => item.status === 202
        && item.url.endsWith("/catalog-publications")),
      "browser evidence is missing the catalog publication POST");
    }
    if (action === "retry-ready") {
      assert(writeResponses.some((item) => item.status === 202 && item.url.endsWith("/retry")),
        "browser evidence is missing the catalog publication retry POST");
    }
    report.status = action === "request-failure"
      ? "PASS_FAILED_STATE_PERSISTED"
      : "PASS_PENDING_DATABASE_CATALOG_VERIFICATION_AND_CLEANUP";
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
      action,
      reportPath,
      desktopScreenshot,
      mobileScreenshot,
      datasetId: report.definition?.id || null,
      snapshotId: report.snapshot?.id || null,
      materializationId: report.materialization?.id || null,
      publicationId: report.publication?.id || null,
      cleanupRequired: report.cleanupRequired,
    })}\n`);
  }
}

main().catch((error) => {
  process.stderr.write(`${error instanceof Error ? error.stack || error.message : String(error)}\n`);
  process.exitCode = 1;
});
