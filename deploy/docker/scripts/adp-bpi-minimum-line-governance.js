#!/usr/bin/env node
"use strict";

const crypto = require("node:crypto");
const fs = require("node:fs");
const path = require("node:path");

const action = (process.env.BPI_MINIMUM_LINE_ACTION || "setup").trim().toLowerCase();
const marker = required("BPI_ACCEPTANCE_MARKER");
const serviceBaseUrl = required("BPI_SERVICE_BASE_URL").replace(/\/+$/, "");
const internalSecret = required("BPI_INTERNAL_JWT_SECRET");
const scenarioPath = path.resolve(required("BPI_MINIMUM_LINE_SCENARIO"));
const reportPath = path.resolve(
  process.env.BPI_MINIMUM_LINE_REPORT || `/tmp/${marker}-minimum-line-governance.json`,
);
const boundaryBaseTime = process.env.BPI_ACCEPTANCE_BOUNDARY_BASE_TIME || "";
const timeoutMs = Number(process.env.BPI_ACCEPTANCE_TIMEOUT_MS || 180000);
const recoveredPreviousFlags = previousFlagsFromEnvironment();

if (!new Set(["setup", "cleanup"]).has(action)) {
  throw new Error("BPI_MINIMUM_LINE_ACTION must be setup or cleanup");
}
if (!/^[A-Za-z0-9_-]{8,64}$/.test(marker)) {
  throw new Error("BPI_ACCEPTANCE_MARKER must use 8-64 letters, digits, underscores or hyphens");
}
if (Buffer.byteLength(internalSecret, "utf8") < 32) {
  throw new Error("BPI_INTERNAL_JWT_SECRET must contain at least 32 UTF-8 bytes");
}
if (!Number.isInteger(timeoutMs) || timeoutMs < 1000) {
  throw new Error("BPI_ACCEPTANCE_TIMEOUT_MS must be an integer of at least 1000");
}
if (action === "setup" && !boundaryBaseTime) {
  throw new Error("BPI_ACCEPTANCE_BOUNDARY_BASE_TIME is required for setup");
}

function required(key) {
  const value = process.env[key];
  if (!value || !value.trim()) throw new Error(`${key} is required`);
  return value.trim();
}

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

function previousFlagsFromEnvironment() {
  const raw = process.env.BPI_MINIMUM_LINE_PREVIOUS_FLAGS_JSON;
  if (!raw?.trim()) return {};
  let parsed;
  try {
    parsed = JSON.parse(raw);
  } catch (error) {
    throw new Error(`BPI_MINIMUM_LINE_PREVIOUS_FLAGS_JSON is invalid JSON: ${error.message}`);
  }
  assert(parsed && typeof parsed === "object" && !Array.isArray(parsed),
    "BPI_MINIMUM_LINE_PREVIOUS_FLAGS_JSON must be an object");
  for (const [key, value] of Object.entries(parsed)) {
    assert(new Set(["bpi.rule-management", "bpi.commands"]).has(key),
      `unsupported recovered feature flag: ${key}`);
    assert(value && typeof value === "object" && !Array.isArray(value),
      `recovered feature flag ${key} must be an object`);
    for (const booleanField of ["overrideExists", "overrideActive"]) {
      assert(typeof value[booleanField] === "boolean",
        `recovered feature flag ${key}.${booleanField} must be boolean`);
    }
    assert(value.overrideEnabled === null || typeof value.overrideEnabled === "boolean",
      `recovered feature flag ${key}.overrideEnabled must be boolean or null`);
    assert(Number.isInteger(value.overrideRevision) && value.overrideRevision >= 0,
      `recovered feature flag ${key}.overrideRevision must be a non-negative integer`);
  }
  return parsed;
}

function base64Url(value) {
  return Buffer.from(value).toString("base64url");
}

function internalToken(subject, roles, scenario) {
  const now = Math.floor(Date.now() / 1000);
  const header = base64Url(JSON.stringify({ alg: "HS256", typ: "JWT" }));
  const payload = base64Url(JSON.stringify({
    iss: "ft-mes-adapter",
    aud: "bpi-service",
    sub: subject,
    iat: now,
    exp: now + 900,
    tenant_id: scenario.scope.tenantId,
    roles,
    plant_ids: [scenario.scope.plantId],
    line_ids: [scenario.scope.lineId],
  }));
  const signature = crypto.createHmac("sha256", internalSecret)
    .update(`${header}.${payload}`)
    .digest("base64url");
  return `${header}.${payload}.${signature}`;
}

function loadScenario() {
  const scenario = JSON.parse(fs.readFileSync(scenarioPath, "utf8"));
  assert(scenario.schemaVersion === 1, "scenario schemaVersion must be 1");
  assert(scenario.safety?.shadowOnly === true, "scenario must remain shadow-only");
  for (const target of ["wom", "qcs", "wms", "plcDcs"]) {
    assert(scenario.safety?.writeback?.[target] === false, `${target} writeback must remain disabled`);
  }
  assert(Array.isArray(scenario.signals) && scenario.signals.length >= 2,
    "scenario must contain at least two signals");
  assert(Array.isArray(scenario.rules) && scenario.rules.length === 2,
    "scenario must contain START and END rules");
  return scenario;
}

function actor(subject, roles, scenario) {
  return {
    subject,
    token: internalToken(subject, roles, scenario),
  };
}

async function readResponse(response) {
  const text = await response.text();
  try {
    return { text, json: JSON.parse(text) };
  } catch (_error) {
    return { text, json: null };
  }
}

async function api(report, actorValue, method, route, options = {}) {
  const headers = {
    Accept: "application/json",
    Authorization: `Bearer ${actorValue.token}`,
    "X-Trace-Id": `${marker}-${options.trace || route}`.replace(/[^A-Za-z0-9_.:-]/g, "-").slice(0, 64),
  };
  if (options.body !== undefined) headers["Content-Type"] = "application/json";
  if (options.key) headers["Idempotency-Key"] = options.key;
  if (options.revision !== undefined) headers["If-Match"] = String(options.revision);
  const response = await fetch(`${serviceBaseUrl}${route}`, {
    method,
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
    signal: AbortSignal.timeout(timeoutMs),
  });
  const parsed = await readResponse(response);
  report.requests.push({
    actor: actorValue.subject,
    method,
    route,
    status: response.status,
  });
  const expected = options.expected || [200];
  if (!expected.includes(response.status)) {
    throw new Error(`${method} ${route} returned ${response.status}: ${parsed.text.slice(0, 1000)}`);
  }
  return parsed.json?.data;
}

async function waitFor(description, operation) {
  const deadline = Date.now() + timeoutMs;
  let last;
  while (Date.now() < deadline) {
    last = await operation();
    if (last?.ready) return last.value;
    await new Promise((resolve) => setTimeout(resolve, 1000));
  }
  throw new Error(`${description} did not become ready: ${JSON.stringify(last?.value || null)}`);
}

async function listFlags(report, admin, scenario) {
  const query = new URLSearchParams({
    plantId: scenario.scope.plantId,
    lineId: scenario.scope.lineId,
    scopeType: "LINE",
  });
  return api(report, admin, "GET", `/bpi/v1/feature-flags?${query}`);
}

async function changeFlag(report, admin, scenario, flag, mode, enabled, revision, suffix) {
  return api(report, admin, "POST", `/bpi/v1/feature-flags/${flag}`, {
    key: `${marker}-${suffix}-${flag}`.slice(0, 128),
    revision,
    trace: `${suffix}-${flag}`,
    body: {
      scopeType: "LINE",
      plantId: scenario.scope.plantId,
      lineId: scenario.scope.lineId,
      mode,
      enabled,
      reason: `${marker} 最小转运单元受控影子验收 ${suffix}`,
    },
  });
}

async function currentCatalog(report, engineer, scenario) {
  const query = new URLSearchParams({
    plantId: scenario.scope.plantId,
    lineId: scenario.scope.lineId,
  });
  return api(report, engineer, "GET", `/bpi/v1/point-catalog/current?${query}`);
}

async function listCalibrations(report, actorValue, scenario, propertyId) {
  const query = new URLSearchParams({
    plantId: scenario.scope.plantId,
    lineId: scenario.scope.lineId,
    productId: scenario.device.productId,
    deviceId: scenario.device.deviceId,
    propertyId,
    limit: "100",
  });
  return api(report, actorValue, "GET", `/bpi/v1/point-calibrations?${query}`);
}

function writeReport(report) {
  fs.writeFileSync(reportPath, `${JSON.stringify(report, null, 2)}\n`);
}

function selectedPoints(catalog, scenario) {
  return (catalog?.points || []).filter((point) =>
    point.productId === scenario.device.productId
    && point.deviceId === scenario.device.deviceId
    && scenario.signals.some((signal) => signal.targetPropertyId === point.propertyId));
}

async function setup() {
  const scenario = loadScenario();
  const engineer = actor(`${marker}_ENGINEER`, ["BPI_ENGINEER"], scenario);
  const admin = actor(`${marker}_ADMIN`, ["BPI_ADMIN"], scenario);
  let previousRun = null;
  if (fs.existsSync(reportPath)) {
    const candidate = JSON.parse(fs.readFileSync(reportPath, "utf8"));
    if (candidate.marker === marker && candidate.action === "setup") {
      previousRun = candidate;
    }
  }
  const report = {
    schemaVersion: 1,
    generatedAt: new Date().toISOString(),
    status: "RUNNING",
    action,
    marker,
    scenarioCode: scenario.scenarioCode,
    scenarioPath,
    scenarioChecksum: crypto.createHash("sha256")
      .update(fs.readFileSync(scenarioPath))
      .digest("hex"),
    scope: scenario.scope,
    safety: scenario.safety,
    serviceBaseUrl,
    requests: [],
    previousFlags: {
      ...recoveredPreviousFlags,
      ...(previousRun?.previousFlags || {}),
    },
    previousFlagsSource: {
      reportCheckpoint: Boolean(previousRun),
      recoveredFromAudit: Object.keys(recoveredPreviousFlags),
    },
    resources: {
      calibrations: [],
      topology: null,
      rules: [],
    },
  };
  fs.mkdirSync(path.dirname(reportPath), { recursive: true });
  try {
    const flags = await listFlags(report, admin, scenario);
    for (const key of ["bpi.rule-management", "bpi.commands"]) {
      const flag = flags.find((item) => item.flagKey === key);
      assert(flag, `feature flag ${key} is missing`);
      if (!report.previousFlags[key]) {
        report.previousFlags[key] = {
          overrideExists: flag.overrideExists,
          overrideActive: flag.overrideActive,
          overrideEnabled: flag.overrideEnabled,
          overrideRevision: flag.overrideRevision,
        };
      }
    }
    writeReport(report);
    for (const key of ["bpi.rule-management", "bpi.commands"]) {
      const flag = flags.find((item) => item.flagKey === key);
      if (flag.overrideActive && flag.overrideEnabled === true) {
        report.requests.push({
          actor: admin.subject,
          method: "NOOP",
          route: `/bpi/v1/feature-flags/${key}`,
          status: "ALREADY_ENABLED",
        });
      } else {
        await changeFlag(
          report,
          admin,
          scenario,
          key,
          "SET",
          true,
          flag.overrideRevision,
          "enable",
        );
      }
    }

    const catalogBefore = await waitFor("five source-sequence-qualified pilot points", async () => {
      const catalog = await currentCatalog(report, engineer, scenario);
      const points = selectedPoints(catalog, scenario);
      return {
        ready: points.length === scenario.signals.length
          && points.every((point) =>
            point.sourceSequenceQualified
            && point.calibrationVersion === scenario.signals.find(
              (signal) => signal.targetPropertyId === point.propertyId,
            ).calibrationVersion),
        value: catalog,
      };
    });
    report.catalogBefore = {
      snapshotId: catalogBefore.snapshot.id,
      sourceRevision: catalogBefore.snapshot.sourceRevision,
      points: selectedPoints(catalogBefore, scenario).map((point) => ({
        propertyId: point.propertyId,
        dataType: point.dataType,
        unit: point.unit,
        sourceSequenceQualified: point.sourceSequenceQualified,
        sourceSequenceObservationCount: point.sourceSequenceObservationCount,
        calibrationStatus: point.calibrationStatus,
        ready: point.ready,
      })),
    };

    const validFrom = new Date(Date.now() - 86400000).toISOString();
    const validUntil = new Date(Date.now() + 30 * 86400000).toISOString();
    for (const signal of scenario.signals) {
      const certificateReference =
        `urn:adp:controlled-simulator:${marker}:${signal.targetPropertyId}`;
      const existing = await listCalibrations(
        report,
        engineer,
        scenario,
        signal.targetPropertyId,
      );
      let submitted = existing.find((candidate) =>
        candidate.calibrationVersion === signal.calibrationVersion
        && candidate.certificateReference === certificateReference
        && candidate.state !== "REVOKED");
      const reused = Boolean(submitted);
      if (!submitted) {
        submitted = await api(report, engineer, "POST", "/bpi/v1/point-calibrations", {
          key: `${marker}-cal-submit-${signal.targetPropertyId}`.slice(0, 128),
          revision: 0,
          trace: `cal-submit-${signal.targetPropertyId}`,
          body: {
            plantId: scenario.scope.plantId,
            lineId: scenario.scope.lineId,
            productId: scenario.device.productId,
            deviceId: scenario.device.deviceId,
            propertyId: signal.targetPropertyId,
            calibrationVersion: signal.calibrationVersion,
            certificateReference,
            certificateChecksum: crypto.createHash("sha256")
              .update(`${marker}:${signal.targetPropertyId}:controlled-simulator`)
              .digest("hex"),
            validFrom,
            validUntil,
            reason: `${marker} 受控模拟点确定性量程与类型校验`,
          },
        });
      }
      const approved = submitted.state === "APPROVED"
        ? submitted
        : await api(
          report,
          admin,
          "POST",
          `/bpi/v1/point-calibrations/${submitted.id}/approve`,
          {
            key: `${marker}-cal-approve-${signal.targetPropertyId}`.slice(0, 128),
            revision: submitted.revision,
            trace: `cal-approve-${signal.targetPropertyId}`,
            body: {
              reason: `${marker} 独立复核受控模拟器配置与校验和`,
            },
          },
        );
      report.resources.calibrations.push({
        id: approved.id,
        propertyId: signal.targetPropertyId,
        state: approved.state,
        revision: approved.revision,
        reused,
      });
      writeReport(report);
    }

    const readyCatalog = await waitFor("five READY pilot points", async () => {
      const catalog = await currentCatalog(report, engineer, scenario);
      const points = selectedPoints(catalog, scenario);
      return {
        ready: points.length === scenario.signals.length && points.every((point) => point.ready),
        value: catalog,
      };
    });
    report.catalogReady = {
      snapshotId: readyCatalog.snapshot.id,
      readyPointCount: selectedPoints(readyCatalog, scenario).filter((point) => point.ready).length,
      points: selectedPoints(readyCatalog, scenario).map((point) => ({
        propertyId: point.propertyId,
        calibrationStatus: point.calibrationStatus,
        sourceSequenceEvidenceStatus: point.sourceSequenceEvidenceStatus,
        ready: point.ready,
      })),
    };

    const topologyCode = `${marker}_${scenario.topology.code}`.slice(0, 128);
    let topology = await api(report, engineer, "POST", "/bpi/v1/topologies/drafts", {
      key: `${marker}-topology-draft`,
      revision: 0,
      trace: "topology-draft",
      body: {
        code: topologyCode,
        version: scenario.topology.version,
        plantId: scenario.scope.plantId,
        lineId: scenario.scope.lineId,
        baseVersionId: null,
        definition: scenario.topology.definition,
        reason: `${marker} 最小转运单元拓扑草稿`,
      },
    });
    topology = await api(
      report,
      engineer,
      "POST",
      `/bpi/v1/topologies/${topology.id}/validate`,
      {
        key: `${marker}-topology-validate`,
        revision: topology.revision,
        trace: "topology-validate",
        body: { reason: `${marker} 校验五点拓扑和校准绑定` },
      },
    );
    topology = await api(
      report,
      admin,
      "POST",
      `/bpi/v1/topologies/${topology.id}/publish`,
      {
        key: `${marker}-topology-publish`,
        revision: topology.revision,
        trace: "topology-publish",
        body: { reason: `${marker} 独立管理员发布受控影子拓扑` },
      },
    );
    report.resources.topology = {
      id: topology.id,
      code: topology.code,
      version: topology.version,
      state: topology.state,
      revision: topology.revision,
    };

    const base = new Date(boundaryBaseTime);
    assert(!Number.isNaN(base.getTime()), "BPI_ACCEPTANCE_BOUNDARY_BASE_TIME is invalid");
    const simulationBody = {
      lineId: scenario.scope.lineId,
      from: new Date(base.getTime() - 1000).toISOString(),
      to: new Date(base.getTime() + 30000).toISOString(),
      topologyVersion: `${topology.code}@${topology.version}`,
      calibrationVersion: scenario.signals[0].calibrationVersion,
      goldenSetId: `${marker}_GOLDEN`,
    };

    for (const scenarioRule of scenario.rules) {
      const ruleCode = `${marker}_${scenarioRule.code}`.slice(0, 128);
      let rule = await api(report, engineer, "POST", "/bpi/v1/rules/drafts", {
        key: `${marker}-${scenarioRule.ast.boundaryType}-rule-draft`,
        revision: 0,
        trace: `${scenarioRule.ast.boundaryType}-rule-draft`,
        body: {
          code: ruleCode,
          version: scenarioRule.version,
          lineId: scenario.scope.lineId,
          topologyVersion: `${topology.code}@${topology.version}`,
          baseVersionId: null,
          ast: scenarioRule.ast,
          reason: `${marker} ${scenarioRule.ast.boundaryType} 边界规则草稿`,
        },
      });
      const simulation = await api(
        report,
        engineer,
        "POST",
        `/bpi/v1/rules/${rule.id}/simulate`,
        {
          key: `${marker}-${scenarioRule.ast.boundaryType}-simulate`,
          revision: rule.revision,
          trace: `${scenarioRule.ast.boundaryType}-simulate`,
          body: simulationBody,
          expected: [202],
        },
      );
      assert(simulation.state === "PASSED",
        `${scenarioRule.ast.boundaryType} simulation did not pass`);
      rule = await api(report, engineer, "GET", `/bpi/v1/rules/${rule.id}`);
      rule = await api(
        report,
        engineer,
        "POST",
        `/bpi/v1/rules/${rule.id}/submit-approval`,
        {
          key: `${marker}-${scenarioRule.ast.boundaryType}-submit`,
          revision: rule.revision,
          trace: `${scenarioRule.ast.boundaryType}-submit`,
          body: {
            reason: `${marker} 提交确定性历史回放结果`,
            simulationId: simulation.id,
            simulationChecksum: simulation.checksum,
          },
        },
      );
      rule = await api(
        report,
        admin,
        "POST",
        `/bpi/v1/rules/${rule.id}/publish`,
        {
          key: `${marker}-${scenarioRule.ast.boundaryType}-publish`,
          revision: rule.revision,
          trace: `${scenarioRule.ast.boundaryType}-publish`,
          body: {
            reason: `${marker} 独立管理员批准影子规则`,
            simulationId: simulation.id,
            simulationChecksum: simulation.checksum,
          },
        },
      );
      const readyRule = await waitFor(`${scenarioRule.ast.boundaryType} rule runtime`, async () => {
        const current = await api(report, admin, "GET", `/bpi/v1/rules/${rule.id}`);
        return {
          ready: current.state === "PUBLISHED"
            && current.applicationStatus === "APPLIED"
            && current.runtimeReadinessStatus === "READY",
          value: current,
        };
      });
      report.resources.rules.push({
        id: readyRule.id,
        code: readyRule.code,
        version: readyRule.version,
        boundaryType: scenarioRule.ast.boundaryType,
        state: readyRule.state,
        revision: readyRule.revision,
        simulationId: simulation.id,
        simulationChecksum: simulation.checksum,
        simulationMetrics: simulation.metrics,
        applicationStatus: readyRule.applicationStatus,
        runtimeReadinessStatus: readyRule.runtimeReadinessStatus,
      });
    }

    report.status = "PASS_MINIMUM_LINE_GOVERNANCE_READY";
    report.completedAt = new Date().toISOString();
    writeReport(report);
    process.stdout.write(`${JSON.stringify({
      status: report.status,
      reportPath,
      topology: report.resources.topology,
      rules: report.resources.rules,
      readyPointCount: report.catalogReady.readyPointCount,
    }, null, 2)}\n`);
  } catch (error) {
    report.status = "FAIL";
    report.error = error.stack || error.message;
    writeReport(report);
    throw error;
  }
}

async function cleanup() {
  const scenario = loadScenario();
  const admin = actor(`${marker}_ADMIN`, ["BPI_ADMIN"], scenario);
  const report = JSON.parse(fs.readFileSync(reportPath, "utf8"));
  report.cleanup = {
    startedAt: new Date().toISOString(),
    rules: [],
    calibrations: [],
    flags: [],
  };
  try {
    for (const resource of report.resources?.rules || []) {
      let rule = await api(report, admin, "GET", `/bpi/v1/rules/${resource.id}`);
      if (rule.state === "PUBLISHED") {
        rule = await api(report, admin, "POST", `/bpi/v1/rules/${resource.id}/retire`, {
          key: `${marker}-${resource.boundaryType}-retire`,
          revision: rule.revision,
          trace: `${resource.boundaryType}-retire`,
          body: { reason: `${marker} 影子验收结束，退役临时规则` },
        });
      }
      const inactive = await waitFor(`${resource.boundaryType} rule retirement`, async () => {
        const current = await api(report, admin, "GET", `/bpi/v1/rules/${resource.id}`);
        return {
          ready: current.state === "RETIRED"
            && current.runtimeReadinessStatus === "INACTIVE",
          value: current,
        };
      });
      report.cleanup.rules.push({
        id: inactive.id,
        state: inactive.state,
        runtimeReadinessStatus: inactive.runtimeReadinessStatus,
      });
    }

    for (const resource of report.resources?.calibrations || []) {
      const calibrations = await listCalibrations(
        report,
        admin,
        scenario,
        resource.propertyId,
      );
      let calibration = calibrations.find((item) => item.id === resource.id);
      if (calibration?.state === "APPROVED") {
        calibration = await api(
          report,
          admin,
          "POST",
          `/bpi/v1/point-calibrations/${resource.id}/revoke`,
          {
            key: `${marker}-cal-revoke-${resource.propertyId}`.slice(0, 128),
            revision: calibration.revision,
            trace: `cal-revoke-${resource.propertyId}`,
            body: { reason: `${marker} 影子验收结束，撤销测试校准` },
          },
        );
      }
      report.cleanup.calibrations.push({
        id: resource.id,
        propertyId: resource.propertyId,
        state: calibration?.state || "NOT_FOUND",
      });
    }

    let flags = await listFlags(report, admin, scenario);
    for (const key of ["bpi.commands", "bpi.rule-management"]) {
      const current = flags.find((item) => item.flagKey === key);
      const previous = report.previousFlags?.[key];
      if (!current || !previous) continue;
      const restoreSet = previous.overrideExists && previous.overrideActive;
      const alreadyRestored = restoreSet
        ? current.overrideActive && current.overrideEnabled === previous.overrideEnabled
        : !current.overrideActive;
      if (alreadyRestored) {
        report.requests.push({
          actor: admin.subject,
          method: "NOOP",
          route: `/bpi/v1/feature-flags/${key}`,
          status: "ALREADY_RESTORED",
        });
        report.cleanup.flags.push({
          flagKey: key,
          effectiveEnabled: current.effectiveEnabled,
          overrideActive: current.overrideActive,
          overrideRevision: current.overrideRevision,
        });
        continue;
      }
      const restored = await changeFlag(
        report,
        admin,
        scenario,
        key,
        restoreSet ? "SET" : "INHERIT",
        restoreSet ? previous.overrideEnabled : null,
        current.overrideRevision,
        "restore",
      );
      report.cleanup.flags.push({
        flagKey: key,
        effectiveEnabled: restored.effectiveEnabled,
        overrideActive: restored.overrideActive,
        overrideRevision: restored.overrideRevision,
      });
      flags = await listFlags(report, admin, scenario);
    }

    report.cleanup.status = "PASS";
    report.cleanup.completedAt = new Date().toISOString();
    writeReport(report);
    process.stdout.write(`${JSON.stringify(report.cleanup, null, 2)}\n`);
  } catch (error) {
    report.cleanup.status = "FAIL";
    report.cleanup.error = error.stack || error.message;
    writeReport(report);
    throw error;
  }
}

(action === "setup" ? setup() : cleanup()).catch((error) => {
  process.stderr.write(`${error.stack || error.message}\n`);
  process.exit(1);
});
