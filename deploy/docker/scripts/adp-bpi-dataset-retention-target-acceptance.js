#!/usr/bin/env node
"use strict";

const fs = require("node:fs");
const path = require("node:path");
const { chromium, request } = require("playwright");

const marker = required("BPI_ACCEPTANCE_MARKER");
const action = process.env.BPI_BROWSER_ACTION || "request-locked";
const adpBaseUrl = required("ADP_BASE_URL").replace(/\/+$/, "");
const browserBaseUrl = required("BPI_BROWSER_BASE_URL");
const username = required("ADP_USERNAME");
const password = required("ADP_PASSWORD");
const plantId = process.env.BPI_PLANT_ID || "PLANT-01";
const lineId = process.env.BPI_LINE_ID || "LINE-S07-01";
const expectedFailureCode = process.env.BPI_EXPECTED_FAILURE_CODE || "RETENTION_ARCHIVE_ERROR";
const timeoutMs = Number(process.env.BPI_BROWSER_TIMEOUT_MS || 180_000);
const headless = process.env.BPI_HEADLESS !== "false";
const reportPath = path.resolve(
  process.env.BPI_BROWSER_REPORT || `/tmp/${marker}-dataset-retention-${action}.json`,
);
const desktopScreenshot = path.resolve(
  process.env.BPI_DESKTOP_SCREENSHOT || `/tmp/${marker}-dataset-retention-${action}.png`,
);
const mobileScreenshot = path.resolve(
  process.env.BPI_MOBILE_SCREENSHOT || `/tmp/${marker}-dataset-retention-${action}-mobile.png`,
);

if (!/^[A-Za-z0-9_-]{8,80}$/.test(marker)) {
  throw new Error("BPI_ACCEPTANCE_MARKER must use 8-80 letters, digits, underscores or hyphens");
}
if (!["request-failure", "request-locked", "retry-locked", "read-locked"].includes(action)) {
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

async function scrollCurrentPanel(page, selector) {
  const found = await page.evaluate((targetSelector) => {
    const panel = document.querySelector(targetSelector);
    if (!panel) return false;
    panel.scrollIntoView({ block: "center", inline: "nearest" });
    return true;
  }, selector);
  assert(found, `cannot find current retention panel: ${selector}`);
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
  const snapshot = (await apiGet(
    api, ticket, `/bpi-api/dataset-snapshots/${definition.latestSnapshot.id}`,
  ))?.data;
  assert(snapshot?.state === "MANIFEST_READY", "dataset snapshot is not MANIFEST_READY");
  const materialization = snapshot.latestMaterialization;
  assert(materialization?.state === "READY", "dataset materialization is not READY");
  const publication = (await apiGet(
    api,
    ticket,
    `/bpi-api/dataset-materializations/${materialization.id}/catalog-publications`,
  ))?.data;
  assert(publication?.state === "READY", "Iceberg catalog publication is not READY");
  const archive = (await apiGet(
    api,
    ticket,
    `/bpi-api/dataset-catalog-publications/${publication.id}/retention-archives`,
  ))?.data || null;
  return { definition, snapshot, materialization, publication, archive };
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
  await page.locator('[data-catalog-state="READY"]').waitFor();
}

async function submitArchive(page, target, command) {
  const button = command === "retry"
    ? "#retry-dataset-retention-archive" : "#open-dataset-retention-archive";
  const endpoint = command === "retry"
    ? `/bpi-api/dataset-retention-archives/${target.archive.id}/retry`
    : `/bpi-api/dataset-catalog-publications/${target.publication.id}/retention-archives`;
  await page.locator(button).click();
  await page.getByRole("heading", {
    name: command === "retry" ? "重新排队恢复包" : "创建不可变恢复包",
  }).waitFor();
  await page.locator("#dataset-retention-archive-reason")
    .fill(`${marker} ${command === "retry" ? "对象存储恢复后页面重试" : "真实页面 Object Lock 归档"}`);
  const responsePromise = page.waitForResponse((response) => {
    const url = new URL(response.url());
    return response.request().method() === "POST" && url.pathname.endsWith(endpoint);
  });
  await page.locator("#dataset-retention-archive-submit").click();
  const response = await responsePromise;
  const parsed = await readJson(response);
  assert(response.status() === 202,
    `retention archive returned ${response.status()}: ${parsed.text.slice(0, 500)}`);
  assert(parsed.json?.data?.id, "retention archive response has no id");
  return parsed.json.data;
}

function assertLockedArchive(archive, target) {
  assert(archive?.state === "LOCKED", "retention archive did not reach LOCKED");
  assert(archive.attemptCount >= 1, "retention archive has no execution attempt");
  assert(archive.retentionMode === "GOVERNANCE", "retention mode is not GOVERNANCE");
  assert(new Date(archive.retainUntil).getTime() > Date.now(), "retention deadline is not in the future");
  assert(archive.legalHoldEnabled === false, "unexpected legal hold state");
  assert(archive.archiveBucket === "bpi-dataset-recovery", "unexpected recovery bucket");
  assert(new RegExp(`^archives/tenant_[a-f0-9]{16}/${target.publication.id}/${archive.id}$`)
    .test(archive.archivePrefix || ""), "retention archive prefix is not identity scoped");
  assert(/^[A-Za-z0-9._-]+$/.test(archive.sourceArchiveVersionId || ""),
    "retained source version is missing");
  assert(/^[A-Za-z0-9._-]+$/.test(archive.archiveManifestVersionId || ""),
    "retained manifest version is missing");
  assert(/^[a-f0-9]{64}$/.test(archive.archiveManifestSha256 || ""),
    "recovery manifest SHA-256 is invalid");
  assert(archive.archiveObjectCount === 2 && archive.archiveTotalBytes > 0,
    "retention archive object count or size is invalid");
  assert(archive.catalogVerifiedRowCount === target.publication.verifiedRowCount
    && archive.verifiedRowCount === target.publication.verifiedRowCount,
  "retention archive row counts do not reconcile");
  assert(archive.sourceContentSha256 === target.publication.sourceContentSha256,
    "retention archive source SHA differs from the catalog publication");
  assert(archive.catalogSemanticChecksum === target.publication.semanticChecksum
    && archive.verifiedSemanticChecksum === target.publication.semanticChecksum,
  "retention archive semantic checksum does not reconcile");
  assert(archive.archiveMetadata?.objectLockVerified === true
    && archive.archiveMetadata?.recoveryVerified === true,
  "retention archive did not persist Object Lock and recovery verification");
  assert(archive.archiveMetadata?.mlflowRegistered === false
    && archive.archiveMetadata?.modelTrained === false,
  "retention archive crossed an unsupported ML delivery boundary");
}

async function assertLockedPanel(page, archive) {
  const panel = page.locator('[data-retention-state="LOCKED"]');
  await panel.waitFor({ timeout: timeoutMs });
  assert(await panel.getByText("VERIFIED", { exact: true }).count() >= 2,
    "page does not show both Object Lock and recovery verification");
  assert((await panel.locator(".dataset-retention-prefix").textContent())
    === `s3://${archive.archiveBucket}/${archive.archivePrefix}`,
  "page recovery prefix differs from the API");
  assert((await panel.locator(".dataset-retained-source-version").textContent())
    === archive.sourceArchiveVersionId, "page source version differs from the API");
  assert((await panel.locator(".dataset-retained-manifest-version").textContent())
    === archive.archiveManifestVersionId, "page manifest version differs from the API");
  assert((await panel.locator(".dataset-retention-manifest-sha").textContent())
    === archive.archiveManifestSha256, "page manifest SHA differs from the API");
  assert((await panel.locator(".dataset-retention-semantic-sha").textContent())
    === archive.verifiedSemanticChecksum, "page semantic checksum differs from the API");
  const deliveryStates = await page.locator(".dataset-delivery-grid .status").allTextContents();
  assert(JSON.stringify(deliveryStates)
    === JSON.stringify([
      "MANIFEST_READY", "READY", "READY", "LOCKED", "NOT_STARTED", "NOT_STARTED",
    ]),
  `page delivery chain is inconsistent: ${JSON.stringify(deliveryStates)}`);
}

async function runDesktop(page, api, auth, target, report) {
  await openSnapshotDrawer(page, target.definition);
  let archive = target.archive;
  if (action === "request-failure") {
    assert(archive == null, "request-failure requires no existing archive");
    archive = await submitArchive(page, target, "request");
    const panel = page.locator('[data-retention-state="FAILED"]');
    await panel.waitFor({ timeout: timeoutMs });
    await panel.getByText(expectedFailureCode, { exact: true }).waitFor();
    archive = (await apiGet(
      api, auth.ticket, `/bpi-api/dataset-retention-archives/${archive.id}`,
    )).data;
    assert(archive.state === "FAILED", "retention archive failure was not persisted");
    assert(archive.attemptCount === 1, "failed retention archive did not record one attempt");
    assert(archive.failureCode === expectedFailureCode,
      `unexpected retention failure code: ${archive.failureCode}`);
    assert(archive.sourceArchiveVersionId == null && archive.archiveManifestVersionId == null,
      "failed retention archive persisted locked object versions");
  } else if (action === "request-locked") {
    assert(archive == null, "request-locked requires no existing archive");
    archive = await submitArchive(page, target, "request");
    await page.locator('[data-retention-state="LOCKED"]').waitFor({ timeout: timeoutMs });
    archive = (await apiGet(
      api, auth.ticket, `/bpi-api/dataset-retention-archives/${archive.id}`,
    )).data;
    assertLockedArchive(archive, target);
    await assertLockedPanel(page, archive);
  } else if (action === "retry-locked") {
    assert(archive?.state === "FAILED", "retry-locked requires a FAILED archive");
    const queued = await submitArchive(page, target, "retry");
    assert(queued.id === archive.id, "retry created another retention archive id");
    await page.locator('[data-retention-state="LOCKED"]').waitFor({ timeout: timeoutMs });
    archive = (await apiGet(
      api, auth.ticket, `/bpi-api/dataset-retention-archives/${archive.id}`,
    )).data;
    assertLockedArchive(archive, target);
    assert(archive.attemptCount === 2, "retried retention archive did not record two attempts");
    await assertLockedPanel(page, archive);
  } else {
    assertLockedArchive(archive, target);
    await assertLockedPanel(page, archive);
  }
  await scrollCurrentPanel(page, `[data-retention-state="${archive.state}"]`);
  await page.screenshot({ path: desktopScreenshot, fullPage: true });
  report.archive = archive;
}

async function runRediscovery(page, target, archive) {
  await page.reload({ waitUntil: "networkidle" });
  await openSnapshotDrawer(page, target.definition);
  await assertLockedPanel(page, archive);
}

async function runMobile(browser, auth, target, archive, report) {
  const context = await prepareContext(browser, auth, { width: 390, height: 844 });
  const page = await context.newPage();
  page.setDefaultTimeout(timeoutMs);
  installEvidenceListeners(page, report);
  await openSnapshotDrawer(page, target.definition);
  await assertLockedPanel(page, archive);
  const geometry = await page.evaluate(() => ({
    viewport: window.innerWidth,
    body: document.body.scrollWidth,
    document: document.documentElement.scrollWidth,
    drawerScrollTop: document.querySelector("#detail-drawer")?.scrollTop ?? -1,
    drawerClientWidth: document.querySelector("#detail-drawer")?.clientWidth ?? -1,
    drawerScrollWidth: document.querySelector("#detail-drawer")?.scrollWidth ?? -1,
  }));
  assert(geometry.body <= geometry.viewport + 1 && geometry.document <= geometry.viewport + 1,
    `dataset retention page overflows mobile viewport: ${JSON.stringify(geometry)}`);
  assert(geometry.drawerScrollTop === 0, "opening the retention drawer did not reset mobile scroll");
  assert(geometry.drawerScrollWidth <= geometry.drawerClientWidth + 1,
    `dataset retention drawer overflows mobile viewport: ${JSON.stringify(geometry)}`);
  await scrollCurrentPanel(page, '[data-retention-state="LOCKED"]');
  await page.screenshot({ path: mobileScreenshot, fullPage: true });
  report.browser.mobile = { ...geometry, lockedPanel: true };
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
    archive: null,
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
    objectLockVerificationRequired: action !== "request-failure",
    recoveryRehearsalRequired: action !== "request-failure",
    cleanupRequired: true,
    error: null,
  };

  try {
    const auth = await login(api);
    report.login = { status: auth.status, username };
    const target = await resolveTarget(api, auth.ticket);
    report.definition = { id: target.definition.id, datasetCode: target.definition.datasetCode };
    report.snapshot = { id: target.snapshot.id, state: target.snapshot.state };
    report.materialization = target.materialization;
    report.publication = target.publication;

    browser = await chromium.launch({ headless });
    const context = await prepareContext(browser, auth, { width: 1440, height: 900 });
    const page = await context.newPage();
    page.setDefaultTimeout(timeoutMs);
    installEvidenceListeners(page, report);
    await runDesktop(page, api, auth, target, report);
    if (report.archive?.state === "LOCKED") {
      await runRediscovery(page, target, report.archive);
      await runMobile(browser, auth, target, report.archive, report);
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
        && item.url.endsWith("/retention-archives")),
      "browser evidence is missing the retention archive POST");
    }
    if (action === "retry-locked") {
      assert(writeResponses.some((item) => item.status === 202 && item.url.endsWith("/retry")),
        "browser evidence is missing the retention archive retry POST");
    }
    report.status = action === "request-failure"
      ? "PASS_FAILED_STATE_PERSISTED"
      : "PASS_PENDING_DATABASE_OBJECT_LOCK_RECOVERY_VERIFICATION_AND_CLEANUP";
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
      mobileScreenshot: report.archive?.state === "LOCKED" ? mobileScreenshot : null,
      datasetId: report.definition?.id || null,
      snapshotId: report.snapshot?.id || null,
      materializationId: report.materialization?.id || null,
      publicationId: report.publication?.id || null,
      archiveId: report.archive?.id || null,
      cleanupRequired: report.cleanupRequired,
    })}\n`);
  }
}

main().catch((error) => {
  process.stderr.write(`${error instanceof Error ? error.stack || error.message : String(error)}\n`);
  process.exitCode = 1;
});
