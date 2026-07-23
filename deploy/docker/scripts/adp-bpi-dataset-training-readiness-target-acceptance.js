#!/usr/bin/env node
"use strict";

const fs = require("node:fs");
const path = require("node:path");
const { chromium, request } = require("playwright");

const marker = required("BPI_ACCEPTANCE_MARKER");
const action = process.env.BPI_BROWSER_ACTION || "assess-blocked";
const adpBaseUrl = required("ADP_BASE_URL").replace(/\/+$/, "");
const browserBaseUrl = required("BPI_BROWSER_BASE_URL");
const username = required("ADP_USERNAME");
const password = required("ADP_PASSWORD");
const plantId = process.env.BPI_PLANT_ID || "PLANT-01";
const lineId = process.env.BPI_LINE_ID || "LINE-S07-01";
const timeoutMs = Number(process.env.BPI_BROWSER_TIMEOUT_MS || 180_000);
const headless = process.env.BPI_HEADLESS !== "false";
const reportPath = path.resolve(
  process.env.BPI_BROWSER_REPORT || `/tmp/${marker}-dataset-training-readiness-${action}.json`,
);
const desktopScreenshot = path.resolve(
  process.env.BPI_DESKTOP_SCREENSHOT || `/tmp/${marker}-dataset-training-readiness-${action}.png`,
);
const mobileScreenshot = path.resolve(
  process.env.BPI_MOBILE_SCREENSHOT
    || `/tmp/${marker}-dataset-training-readiness-${action}-mobile.png`,
);

if (!/^[A-Za-z0-9_-]{8,80}$/.test(marker)) {
  throw new Error("BPI_ACCEPTANCE_MARKER must use 8-80 letters, digits, underscores or hyphens");
}
if (!["assess-blocked", "read-blocked"].includes(action)) {
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
    const requestHeaders = requestValue.headers();
    const record = {
      method: requestValue.method(),
      url: `${url.pathname}${url.search}`,
      status: response.status(),
      payload: sanitizedPayload(requestValue),
    };
    if (requestValue.method() !== "GET") {
      record.headers = {
        idempotencyKey: requestHeaders["idempotency-key"] || null,
        ifMatch: requestHeaders["if-match"] || null,
        traceId: requestHeaders["x-trace-id"] || null,
      };
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

async function apiAssess(api, ticket, registration, key, reason) {
  const endpoint = `/bpi-api/dataset-mlflow-registrations/${registration.id}`
    + "/training-readiness-assessments";
  const response = await api.post(`${adpBaseUrl}${endpoint}`, {
    headers: {
      Authorization: `Bearer ${ticket}`,
      "Content-Type": "application/json;charset=UTF-8",
      "Idempotency-Key": key,
      "If-Match": String(registration.revision),
    },
    data: {
      objectiveCode: "BATCH_START_BOUNDARY_REVIEW_RISK",
      reason,
    },
    timeout: timeoutMs,
  });
  const parsed = await readJson(response);
  assert(response.status() === 200,
    `${endpoint} returned ${response.status()}: ${parsed.text.slice(0, 500)}`);
  return {
    assessment: parsed.json?.data,
    replay: response.headers()["idempotent-replay"] === "true",
    status: response.status(),
    endpoint,
  };
}

async function resolveTarget(api, ticket) {
  const definitions = await apiGet(
    api, ticket, `/bpi-api/datasets?plantId=${encodeURIComponent(plantId)}&limit=100`,
  );
  const definition = definitions?.data?.find((item) => item.datasetCode === marker);
  assert(definition?.latestSnapshot?.id, `dataset definition ${marker} has no snapshot`);
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
  const registration = (await apiGet(
    api,
    ticket,
    `/bpi-api/dataset-retention-archives/${archive.id}/mlflow-registrations`,
  ))?.data;
  assert(registration?.state === "REGISTERED", "MLflow Dataset Input is not REGISTERED");
  const assessment = (await apiGet(
    api,
    ticket,
    `/bpi-api/dataset-mlflow-registrations/${registration.id}/training-readiness-assessments`,
  ))?.data || null;
  return { definition, snapshot, materialization, publication, archive, registration, assessment };
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
  await page.locator('[data-mlflow-state="REGISTERED"]').waitFor({ timeout: timeoutMs });
}

function assertAssessment(assessment, registration, expectedSequence) {
  assert(assessment?.state === "BLOCKED", "training readiness did not fail closed");
  assert(assessment.assessmentSequence === expectedSequence,
    `assessment sequence ${assessment.assessmentSequence} differs from ${expectedSequence}`);
  assert(assessment.revision === 1, "immutable assessment revision is not 1");
  assert(assessment.mlflowRegistrationId === registration.id,
    "assessment points to another MLflow registration");
  assert(assessment.sourceRegistrationRevision === registration.revision,
    "assessment did not pin the source registration revision");
  assert(assessment.objectiveCode === "BATCH_START_BOUNDARY_REVIEW_RISK",
    "assessment objective is invalid");
  assert(assessment.policyVersion === "bpi-training-readiness/batch-start-boundary-v1",
    "assessment policy is invalid");
  assert(Array.isArray(assessment.gateResults) && assessment.gateResults.length === 19,
    "assessment does not contain exactly 19 governed gates");
  assert(Array.isArray(assessment.blockerCodes) && assessment.blockerCodes.length === 8,
    "assessment does not contain exactly 8 readiness blockers");
  for (const blocker of [
    "PROCESS_SIGNAL_WINDOWS_MISSING",
    "INCLUDED_SAMPLE_COUNT_BELOW_MINIMUM",
    "DISTINCT_BATCH_COUNT_BELOW_MINIMUM",
    "PRODUCTION_DAY_COVERAGE_BELOW_MINIMUM",
    "PRODUCTION_SPLIT_GROUPS_BELOW_MINIMUM",
    "EXCLUDED_RATIO_ABOVE_MAXIMUM",
    "START_ACCEPTED_LABEL_COUNT_BELOW_MINIMUM",
    "START_REJECTED_LABEL_COUNT_BELOW_MINIMUM",
  ]) {
    assert(assessment.blockerCodes.includes(blocker), `assessment is missing blocker ${blocker}`);
  }
  assert(Number(assessment.observedMetrics?.includedSampleCount) === 1,
    "assessment did not reconcile the one included target sample");
  assert(Array.isArray(assessment.observedMetrics?.signalWindowFeatureRefs)
    && assessment.observedMetrics.signalWindowFeatureRefs.length === 0,
  "target fixture unexpectedly contains process signal windows");
  assert(assessment.phaseBoundary?.assessmentOnly === true
    && assessment.phaseBoundary?.trainingStarted === false
    && assessment.phaseBoundary?.modelCreated === false
    && assessment.phaseBoundary?.modelRegistered === false
    && assessment.phaseBoundary?.onlineInferenceEnabled === false
    && assessment.phaseBoundary?.productionActivationAllowed === false,
  "assessment crossed the model training or production activation boundary");
  assert(/^[a-f0-9]{64}$/.test(assessment.assessmentChecksum || ""),
    "assessment checksum is invalid");
}

async function assertAssessmentPanel(page, assessment) {
  const panel = page.locator('[data-training-readiness-state="BLOCKED"]');
  await panel.waitFor({ timeout: timeoutMs });
  await panel.getByText("PROCESS_SIGNAL_WINDOWS_MISSING", { exact: true }).waitFor();
  await panel.getByText("INCLUDED_SAMPLE_COUNT_BELOW_MINIMUM", { exact: true }).waitFor();
  assert((await panel.locator(".dataset-training-readiness-checksum").textContent())
    === assessment.assessmentChecksum, "page assessment checksum differs from the API");
  const states = await page.locator(".dataset-delivery-grid .status").allTextContents();
  assert(JSON.stringify(states) === JSON.stringify([
    "MANIFEST_READY", "READY", "READY", "LOCKED", "REGISTERED", "BLOCKED", "NOT_STARTED",
  ]), `page delivery chain is inconsistent: ${JSON.stringify(states)}`);
}

async function submitAssessment(page, registration) {
  await page.locator("#open-dataset-training-readiness").click();
  await page.getByRole("heading", { name: "评估训练就绪" }).waitFor();
  const reason = `${marker} 真实页面核对离线训练资格，不启动模型`;
  await page.locator("#dataset-training-readiness-reason").fill(reason);
  const responsePromise = page.waitForResponse((response) => {
    const url = new URL(response.url());
    return response.request().method() === "POST"
      && url.pathname.endsWith(
        `/bpi-api/dataset-mlflow-registrations/${registration.id}/training-readiness-assessments`,
      );
  });
  await page.locator("#dataset-training-readiness-submit").click();
  const response = await responsePromise;
  const parsed = await readJson(response);
  assert(response.status() === 200,
    `training readiness returned ${response.status()}: ${parsed.text.slice(0, 500)}`);
  const headers = response.request().headers();
  assert(headers["idempotency-key"], "browser request did not carry Idempotency-Key");
  assert(headers["if-match"] === String(registration.revision),
    "browser request did not pin the registration revision");
  return {
    assessment: parsed.json?.data,
    idempotencyKey: headers["idempotency-key"],
    reason,
  };
}

async function runDesktop(page, api, auth, target, report) {
  await openSnapshotDrawer(page, target.definition);
  let assessment = target.assessment;
  if (action === "assess-blocked") {
    assert(assessment == null, "assess-blocked requires no existing assessment");
    await page.locator('[data-training-readiness-state="NOT_STARTED"]').waitFor();
    const submitted = await submitAssessment(page, target.registration);
    assessment = submitted.assessment;
    assertAssessment(assessment, target.registration, 1);
    await assertAssessmentPanel(page, assessment);

    const replay = await apiAssess(
      api, auth.ticket, target.registration, submitted.idempotencyKey, submitted.reason,
    );
    assert(replay.replay === true, "same Idempotency-Key was not replayed");
    assert(replay.assessment?.id === assessment.id,
      "idempotent replay created another assessment");

    const second = await apiAssess(
      api,
      auth.ticket,
      target.registration,
      `${marker}-training-readiness-second`,
      `${marker} 重复相同冻结事实，验证评估序号与 checksum`,
    );
    assert(second.replay === false, "distinct assessment command was replayed");
    assertAssessment(second.assessment, target.registration, 2);
    assert(second.assessment.id !== assessment.id,
      "distinct command did not create an immutable assessment row");
    assert(second.assessment.assessmentChecksum === assessment.assessmentChecksum,
      "same frozen evidence produced another assessment checksum");
    report.api = {
      idempotentReplay: { status: replay.status, sameAssessmentId: true },
      secondAssessment: {
        status: second.status,
        id: second.assessment.id,
        sequence: second.assessment.assessmentSequence,
        sameChecksum: true,
      },
    };
    assessment = second.assessment;
    await openSnapshotDrawer(page, target.definition);
    await assertAssessmentPanel(page, assessment);
  } else {
    assertAssessment(assessment, target.registration, 2);
    await assertAssessmentPanel(page, assessment);
  }
  await page.locator('[data-training-readiness-state="BLOCKED"]')
    .scrollIntoViewIfNeeded();
  await page.screenshot({ path: desktopScreenshot, fullPage: true });
  report.assessment = assessment;
}

async function runMobile(browser, auth, target, assessment, report) {
  const context = await prepareContext(browser, auth, { width: 390, height: 844 });
  const page = await context.newPage();
  page.setDefaultTimeout(timeoutMs);
  installEvidenceListeners(page, report);
  await openSnapshotDrawer(page, target.definition);
  await assertAssessmentPanel(page, assessment);
  const geometry = await page.evaluate(() => ({
    viewport: window.innerWidth,
    body: document.body.scrollWidth,
    document: document.documentElement.scrollWidth,
    drawerClientWidth: document.querySelector("#detail-drawer")?.clientWidth ?? -1,
    drawerScrollWidth: document.querySelector("#detail-drawer")?.scrollWidth ?? -1,
  }));
  assert(geometry.body <= geometry.viewport + 1 && geometry.document <= geometry.viewport + 1,
    `training readiness page overflows mobile viewport: ${JSON.stringify(geometry)}`);
  assert(geometry.drawerScrollWidth <= geometry.drawerClientWidth + 1,
    `training readiness drawer overflows mobile viewport: ${JSON.stringify(geometry)}`);
  await page.locator('[data-training-readiness-state="BLOCKED"]')
    .scrollIntoViewIfNeeded();
  await page.screenshot({ path: mobileScreenshot, fullPage: true });
  report.browser.mobile = geometry;
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
    source: null,
    assessment: null,
    api: {},
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
    mlflowNoSideEffectVerificationRequired: true,
    restartVerificationRequired: action === "assess-blocked",
    cleanupRequired: true,
    error: null,
  };

  try {
    const auth = await login(api);
    report.login = { status: auth.status, username };
    const target = await resolveTarget(api, auth.ticket);
    report.source = {
      datasetId: target.definition.id,
      snapshotId: target.snapshot.id,
      materializationId: target.materialization.id,
      publicationId: target.publication.id,
      archiveId: target.archive.id,
      registrationId: target.registration.id,
      registrationRevision: target.registration.revision,
      mlflowRunId: target.registration.mlflowRunId,
      datasetDigest: target.registration.datasetDigest,
    };

    browser = await chromium.launch({ headless });
    const context = await prepareContext(browser, auth, { width: 1440, height: 900 });
    const page = await context.newPage();
    page.setDefaultTimeout(timeoutMs);
    installEvidenceListeners(page, report);
    await runDesktop(page, api, auth, target, report);
    await context.close();
    await runMobile(browser, auth, target, report.assessment, report);
    await browser.close();
    browser = null;

    assert(report.browser.consoleErrors.length === 0, "browser emitted console errors");
    assert(report.browser.pageErrors.length === 0, "browser emitted page errors");
    assert(report.browser.requestFailures.length === 0, "browser emitted request failures");
    const writes = report.browser.network.filter((item) => item.method === "POST");
    if (action === "assess-blocked") {
      assert(writes.some((item) => item.status === 200
        && item.url.endsWith("/training-readiness-assessments")
        && item.payload?.objectiveCode === "BATCH_START_BOUNDARY_REVIEW_RISK"),
      "browser evidence is missing the training-readiness POST");
    } else {
      assert(writes.length === 0, "read-blocked unexpectedly wrote through the browser");
    }
    report.status = action === "assess-blocked"
      ? "PASS_PENDING_DATABASE_MLFLOW_RESTART_VERIFICATION_AND_CLEANUP"
      : "PASS_BLOCKED_STATE_REDISCOVERED_AFTER_RESTART";
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
      registrationId: report.source?.registrationId || null,
      assessmentId: report.assessment?.id || null,
      assessmentSequence: report.assessment?.assessmentSequence || null,
      blockerCount: report.assessment?.blockerCodes?.length ?? null,
      cleanupRequired: report.cleanupRequired,
    })}\n`);
  }
}

main().catch((error) => {
  process.stderr.write(`${error instanceof Error ? error.stack || error.message : String(error)}\n`);
  process.exitCode = 1;
});
