#!/usr/bin/env node
"use strict";

const fs = require("node:fs");
const path = require("node:path");
const { chromium, request } = require("playwright");

const marker = required("BPI_ACCEPTANCE_MARKER");
const action = process.env.BPI_BROWSER_ACTION || "read-registered";
const adpBaseUrl = required("ADP_BASE_URL").replace(/\/+$/, "");
const browserBaseUrl = required("BPI_BROWSER_BASE_URL");
const username = required("ADP_USERNAME");
const password = required("ADP_PASSWORD");
const plantId = process.env.BPI_PLANT_ID || "PLANT-01";
const lineId = process.env.BPI_LINE_ID || "LINE-S07-01";
const expectedFailureCode = process.env.BPI_EXPECTED_FAILURE_CODE
  || "MLFLOW_TRANSPORT_ERROR";
const timeoutMs = Number(process.env.BPI_BROWSER_TIMEOUT_MS || 180_000);
const headless = process.env.BPI_HEADLESS !== "false";
const reportPath = path.resolve(
  process.env.BPI_BROWSER_REPORT || `/tmp/${marker}-dataset-mlflow-${action}.json`,
);
const desktopScreenshot = path.resolve(
  process.env.BPI_DESKTOP_SCREENSHOT || `/tmp/${marker}-dataset-mlflow-${action}.png`,
);
const mobileScreenshot = path.resolve(
  process.env.BPI_MOBILE_SCREENSHOT || `/tmp/${marker}-dataset-mlflow-${action}-mobile.png`,
);

if (!/^[A-Za-z0-9_-]{8,80}$/.test(marker)) {
  throw new Error("BPI_ACCEPTANCE_MARKER must use 8-80 letters, digits, underscores or hyphens");
}
if (!["request-failure", "retry-registered", "read-registered"].includes(action)) {
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
  assert(found, `cannot find current MLflow panel: ${selector}`);
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
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json;charset=UTF-8",
      },
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
    method: failed.method(),
    url: failed.url(),
    error: failed.failure()?.errorText || "",
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
  ))?.data;
  assert(archive?.state === "LOCKED", "dataset recovery archive is not LOCKED");
  assert(archive.archiveMetadata?.objectLockVerified === true
    && archive.archiveMetadata?.recoveryVerified === true,
  "dataset recovery archive lacks Object Lock or recovery verification");
  const registration = (await apiGet(
    api,
    ticket,
    `/bpi-api/dataset-retention-archives/${archive.id}/mlflow-registrations`,
  ))?.data || null;
  return { definition, snapshot, materialization, publication, archive, registration };
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
  await page.locator('[data-retention-state="LOCKED"]').waitFor();
}

async function submitRegistration(page, target, command) {
  const button = command === "retry"
    ? "#retry-dataset-mlflow-registration" : "#open-dataset-mlflow-registration";
  const endpoint = command === "retry"
    ? `/bpi-api/dataset-mlflow-registrations/${target.registration.id}/retry`
    : `/bpi-api/dataset-retention-archives/${target.archive.id}/mlflow-registrations`;
  await page.locator(button).click();
  await page.getByRole("heading", {
    name: command === "retry" ? "重新排队数据输入登记" : "登记数据输入",
  }).waitFor();
  await page.locator("#dataset-mlflow-registration-reason").fill(
    `${marker} ${command === "retry" ? "Tracking Server 恢复后页面重试" : "页面登记不可变训练数据输入"}`,
  );
  const responsePromise = page.waitForResponse((response) => {
    const url = new URL(response.url());
    return response.request().method() === "POST" && url.pathname.endsWith(endpoint);
  });
  await page.locator("#dataset-mlflow-registration-submit").click();
  const response = await responsePromise;
  const parsed = await readJson(response);
  assert(response.status() === 202,
    `MLflow registration returned ${response.status()}: ${parsed.text.slice(0, 500)}`);
  assert(parsed.json?.data?.id, "MLflow registration response has no id");
  return parsed.json.data;
}

function expectedSource(target) {
  return `s3://${target.archive.archiveBucket}/${target.archive.sourceArchiveObjectKey}`
    + `?versionId=${target.archive.sourceArchiveVersionId}`;
}

function assertRegistered(registration, target) {
  assert(registration?.state === "REGISTERED", "MLflow dataset input did not reach REGISTERED");
  assert(registration.attemptCount === 2, "retried registration did not record two attempts");
  assert(/^\d+$/.test(registration.mlflowExperimentId || ""), "MLflow experiment id is invalid");
  assert(/^[a-f0-9]{32}$/.test(registration.mlflowRunId || ""), "MLflow run id is invalid");
  assert(registration.datasetDigest === target.archive.catalogSemanticChecksum.slice(0, 16),
    "MLflow dataset digest differs from the frozen semantic checksum");
  assert(registration.mlflowDatasetSource === expectedSource(target),
    "MLflow dataset source differs from the exact Object Lock version");
  assert(registration.tableIdentifier === target.publication.tableIdentifier
    && registration.icebergSnapshotId === target.publication.icebergSnapshotId,
  "MLflow registration differs from the Iceberg publication identity");
  assert(registration.registrationMetadata?.sourceFactsVerified === true
    && registration.registrationMetadata?.datasetInputVerified === true
    && registration.registrationMetadata?.lineageVerified === true,
  "source facts, MLflow Dataset Input, or lineage was not verified");
  assert(registration.registrationMetadata?.modelTrained === false
    && registration.registrationMetadata?.modelRegistered === false
    && registration.registrationMetadata?.onlineInferenceEnabled === false
    && registration.registrationMetadata?.productionActivationAllowed === false,
  "MLflow registration crossed the model training or activation boundary");
}

async function assertRegisteredPanel(page, registration) {
  const panel = page.locator('[data-mlflow-state="REGISTERED"]');
  await panel.waitFor({ timeout: timeoutMs });
  await panel.getByText("VERIFIED", { exact: true }).waitFor();
  await panel.getByText("NOT_STARTED", { exact: true }).waitFor();
  assert((await panel.locator(".dataset-mlflow-digest").textContent())
    === registration.datasetDigest, "page dataset digest differs from the API");
  assert((await panel.locator(".dataset-mlflow-experiment").textContent())
    === registration.mlflowExperimentId, "page experiment differs from the API");
  assert((await panel.locator(".dataset-mlflow-run").textContent())
    === registration.mlflowRunId, "page run differs from the API");
  assert((await panel.locator(".dataset-mlflow-source").textContent())
    === registration.mlflowDatasetSource, "page dataset source differs from the API");
  const states = await page.locator(".dataset-delivery-grid .status").allTextContents();
  assert(JSON.stringify(states) === JSON.stringify([
    "MANIFEST_READY", "READY", "READY", "LOCKED", "REGISTERED", "NOT_STARTED",
    "NOT_STARTED",
  ]), `page delivery chain is inconsistent: ${JSON.stringify(states)}`);
}

async function runDesktop(page, api, auth, target, report) {
  await openSnapshotDrawer(page, target.definition);
  let registration = target.registration;
  if (action === "request-failure") {
    assert(registration == null, "request-failure requires no existing registration");
    registration = await submitRegistration(page, target, "request");
    const panel = page.locator('[data-mlflow-state="FAILED"]');
    await panel.waitFor({ timeout: timeoutMs });
    await panel.getByText(expectedFailureCode, { exact: true }).waitFor();
    registration = (await apiGet(
      api, auth.ticket, `/bpi-api/dataset-mlflow-registrations/${registration.id}`,
    )).data;
    assert(registration.state === "FAILED", "MLflow outage was not persisted as FAILED");
    assert(registration.revision === 3 && registration.attemptCount === 1,
      "failed MLflow registration revision or attempt count is invalid");
    assert(registration.failureCode === expectedFailureCode,
      `unexpected MLflow failure code: ${registration.failureCode}`);
    assert(registration.mlflowExperimentId == null && registration.mlflowRunId == null,
      "failed MLflow registration persisted an external identity");
  } else if (action === "retry-registered") {
    assert(registration?.state === "FAILED", "retry-registered requires a FAILED registration");
    const failedId = registration.id;
    const queued = await submitRegistration(page, target, "retry");
    assert(queued.id === failedId, "retry created another MLflow registration id");
    await page.locator('[data-mlflow-state="REGISTERED"]').waitFor({ timeout: timeoutMs });
    registration = (await apiGet(
      api, auth.ticket, `/bpi-api/dataset-mlflow-registrations/${failedId}`,
    )).data;
    assertRegistered(registration, target);
    await assertRegisteredPanel(page, registration);
  } else {
    assertRegistered(registration, target);
    await assertRegisteredPanel(page, registration);
  }
  await scrollCurrentPanel(page, `[data-mlflow-state="${registration.state}"]`);
  await page.screenshot({ path: desktopScreenshot, fullPage: true });
  report.registration = registration;
}

async function runRediscovery(page, target, registration) {
  await page.reload({ waitUntil: "networkidle" });
  await openSnapshotDrawer(page, target.definition);
  await assertRegisteredPanel(page, registration);
}

async function runMobile(browser, auth, target, registration, report) {
  const context = await prepareContext(browser, auth, { width: 390, height: 844 });
  const page = await context.newPage();
  page.setDefaultTimeout(timeoutMs);
  installEvidenceListeners(page, report);
  await openSnapshotDrawer(page, target.definition);
  await assertRegisteredPanel(page, registration);
  const geometry = await page.evaluate(() => ({
    viewport: window.innerWidth,
    body: document.body.scrollWidth,
    document: document.documentElement.scrollWidth,
    drawerScrollTop: document.querySelector("#detail-drawer")?.scrollTop ?? -1,
    drawerClientWidth: document.querySelector("#detail-drawer")?.clientWidth ?? -1,
    drawerScrollWidth: document.querySelector("#detail-drawer")?.scrollWidth ?? -1,
  }));
  assert(geometry.body <= geometry.viewport + 1 && geometry.document <= geometry.viewport + 1,
    `dataset MLflow page overflows mobile viewport: ${JSON.stringify(geometry)}`);
  assert(geometry.drawerScrollTop === 0, "opening the MLflow drawer did not reset mobile scroll");
  assert(geometry.drawerScrollWidth <= geometry.drawerClientWidth + 1,
    `dataset MLflow drawer overflows mobile viewport: ${JSON.stringify(geometry)}`);
  await scrollCurrentPanel(page, '[data-mlflow-state="REGISTERED"]');
  await page.screenshot({ path: mobileScreenshot, fullPage: true });
  report.browser.mobile = { ...geometry, registeredPanel: true };
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
    registration: null,
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
    mlflowBackendVerificationRequired: action !== "request-failure",
    minioIsolationVerificationRequired: action !== "request-failure",
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
    report.archive = target.archive;

    browser = await chromium.launch({ headless });
    const context = await prepareContext(browser, auth, { width: 1440, height: 900 });
    const page = await context.newPage();
    page.setDefaultTimeout(timeoutMs);
    installEvidenceListeners(page, report);
    await runDesktop(page, api, auth, target, report);
    if (report.registration?.state === "REGISTERED") {
      await runRediscovery(page, target, report.registration);
      await runMobile(browser, auth, target, report.registration, report);
    }
    await context.close();
    await browser.close();
    browser = null;

    assert(report.browser.consoleErrors.length === 0, "browser emitted console errors");
    assert(report.browser.pageErrors.length === 0, "browser emitted page errors");
    assert(report.browser.requestFailures.length === 0, "browser emitted request failures");
    const writes = report.browser.network.filter((item) => item.method === "POST");
    if (action === "request-failure") {
      assert(writes.some((item) => item.status === 202
        && item.url.endsWith("/mlflow-registrations")),
      "browser evidence is missing the MLflow registration POST");
    }
    if (action === "retry-registered") {
      assert(writes.some((item) => item.status === 202 && item.url.endsWith("/retry")),
        "browser evidence is missing the MLflow registration retry POST");
    }
    report.status = action === "request-failure"
      ? "PASS_FAILED_STATE_PERSISTED"
      : "PASS_PENDING_DATABASE_MLFLOW_MINIO_VERIFICATION_AND_CLEANUP";
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
      mobileScreenshot: report.registration?.state === "REGISTERED" ? mobileScreenshot : null,
      datasetId: report.definition?.id || null,
      snapshotId: report.snapshot?.id || null,
      materializationId: report.materialization?.id || null,
      publicationId: report.publication?.id || null,
      archiveId: report.archive?.id || null,
      registrationId: report.registration?.id || null,
      mlflowRunId: report.registration?.mlflowRunId || null,
      cleanupRequired: report.cleanupRequired,
    })}\n`);
  }
}

main().catch((error) => {
  process.stderr.write(`${error instanceof Error ? error.stack || error.message : String(error)}\n`);
  process.exitCode = 1;
});
