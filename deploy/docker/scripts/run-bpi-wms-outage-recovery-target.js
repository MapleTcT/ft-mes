#!/usr/bin/env node
"use strict";

const fs = require("node:fs");
const path = require("node:path");
const { spawnSync } = require("node:child_process");

const repoRoot = path.resolve(__dirname, "../../..");
const sshTarget = process.env.BPI_TARGET_SSH || "v6@10.11.100.17";
const runtimeRoot = process.env.BPI_TARGET_RUNTIME_ROOT
  || "/home/v6/adp-mes-docker-newbase-20260611-181921";
const composeDir = `${runtimeRoot}/deploy/docker`;
const composeProject = process.env.BPI_TARGET_COMPOSE_PROJECT || "adp-mes-newbase";
const postgresContainer = `${composeProject}-postgres-1`;
const materialContainer = `${composeProject}-material-1`;
const bpiServiceContainer = `${composeProject}-bpi-service-1`;
const wmsAdapterContainer = `${composeProject}-bpi-wms-adapter-1`;
const kafkaContainer = process.env.BPI_TARGET_KAFKA_CONTAINER
  || "ft-mes-bpi-streaming-kafka-1-1";
const kafkaBootstrap = process.env.BPI_TARGET_KAFKA_BOOTSTRAP || "kafka-1:9092";
const commandTopic = "bpi.wms.completion-inbound-command.v1";
const commandDlqTopic = "bpi.wms.completion-inbound-command.dlq.v1";
const receiptTopic = "wms.completion-inbound.receipt.v1";
const wmsGroup = "ft-mes-bpi-wms-adapter-v1";
const receiptGroup = "ft-mes-bpi-phase2-integrations-v1";
const marker = process.env.BPI_ACCEPTANCE_MARKER || defaultMarker();
const fixturePath = path.resolve(
  process.env.BPI_FIXTURE_OUTPUT || `/tmp/${marker}-wms-outage-fixture.json`,
);
const browserReportPath = path.resolve(
  process.env.BPI_BROWSER_REPORT || `/tmp/${marker}-wms-outage-recovery-browser.json`,
);
const screenshotPath = path.resolve(
  process.env.BPI_BROWSER_SCREENSHOT || `/tmp/${marker}-wms-outage-recovery.png`,
);
const targetReportPath = path.resolve(
  process.env.BPI_TARGET_REPORT || `/tmp/${marker}-wms-outage-recovery-target.json`,
);
const envPath = `${composeDir}/.env`;
const envBackup = `${composeDir}/.env.${marker}.backup`;
const waitTimeoutMs = Number(process.env.BPI_TARGET_WAIT_TIMEOUT_MS || 180_000);

const sql = {
  fixture: path.join(__dirname, "bpi-wms-outage-recovery-fixture.sql"),
  verifyBpi: path.join(__dirname, "bpi-wms-outage-recovery-verification.sql"),
  cleanupBpi: path.join(__dirname, "bpi-wms-outage-recovery-cleanup.sql"),
  verifyMaterial: path.join(__dirname, "bpi-wms-outage-recovery-material-verification.sql"),
  cleanupMaterial: path.join(__dirname, "bpi-wms-outage-recovery-material-cleanup.sql"),
};

const safetyKeys = [
  "BPI_PHASE2_INTEGRATION_ENABLED",
  "BPI_PHASE2_PROTOBUF_HTTP_INGRESS_ENABLED",
  "BPI_PHASE2_KAFKA_ENABLED",
  "BPI_WMS_OUTBOX_ENABLED",
  "BPI_WMS_ADAPTER_ENABLED",
];

validateInputs();

const report = {
  generatedAt: new Date().toISOString(),
  status: "FAIL",
  database: "PostgreSQL",
  marker,
  target: {
    host: sshTarget.replace(/^[^@]+@/, ""),
    composeProject,
    runtimeRoot,
  },
  scope: { tenantId: "1000", plantId: "PLANT-01", lineId: "LINE-S07-01" },
  outage: {
    service: "material",
    expectedCommandDlqDelta: 1,
    expectedCommandTopicDelta: 2,
    expectedReceiptTopicDelta: 1,
  },
  fixture: null,
  preflight: null,
  firstDelivery: null,
  browser: null,
  persistence: null,
  kafka: null,
  cleanup: null,
  error: null,
};

let fixture;
let backupCreated = false;
let fixtureInserted = false;
let runtimeChanged = false;
let cleanupRunning = false;

function defaultMarker() {
  const timestamp = new Date().toISOString().replace(/[-:TZ.]/g, "").slice(0, 14);
  return `ADP_E2E_${timestamp}_WMS_OUTAGE`;
}

function required(key) {
  const value = process.env[key];
  if (!value || !value.trim()) throw new Error(`${key} is required`);
  return value.trim();
}

function validateInputs() {
  if (process.env.BPI_CONFIRM_TARGET_OUTAGE !== "YES") {
    throw new Error("Set BPI_CONFIRM_TARGET_OUTAGE=YES to run the controlled material-wms outage.");
  }
  required("ADP_BASE_URL");
  required("ADP_USERNAME");
  required("ADP_PASSWORD");
  if (!/^[A-Za-z0-9_.@-]+$/.test(sshTarget)) throw new Error("BPI_TARGET_SSH is invalid");
  if (!/^\/[A-Za-z0-9_./-]+$/.test(runtimeRoot)) {
    throw new Error("BPI_TARGET_RUNTIME_ROOT must be an absolute safe path");
  }
  if (!/^[A-Za-z0-9_-]+$/.test(composeProject)) {
    throw new Error("BPI_TARGET_COMPOSE_PROJECT is invalid");
  }
  if (!/^[A-Za-z0-9_-]{8,100}$/.test(marker)) {
    throw new Error("BPI_ACCEPTANCE_MARKER must use 8-100 letters, digits, underscores or hyphens");
  }
  if (!Number.isFinite(waitTimeoutMs) || waitTimeoutMs < 30_000 || waitTimeoutMs > 600_000) {
    throw new Error("BPI_TARGET_WAIT_TIMEOUT_MS must be between 30000 and 600000");
  }
  for (const file of Object.values(sql)) {
    if (!fs.existsSync(file)) throw new Error(`required SQL is missing: ${file}`);
  }
}

function shellQuote(value) {
  return `'${String(value).replace(/'/g, `'"'"'`)}'`;
}

function run(command, args, options = {}) {
  const result = spawnSync(command, args, {
    encoding: "utf8",
    maxBuffer: 16 * 1024 * 1024,
    ...options,
  });
  if (result.error) throw result.error;
  if (result.status !== 0) {
    const detail = String(result.stderr || result.stdout || "").trim().slice(-3000);
    throw new Error(`${command} exited ${result.status}: ${detail}`);
  }
  return String(result.stdout || "").trim();
}

function ssh(command, options = {}) {
  return run("ssh", [
    "-o", "BatchMode=yes",
    "-o", "ConnectTimeout=8",
    "-o", "ServerAliveInterval=15",
    "-o", "ServerAliveCountMax=4",
    sshTarget,
    command,
  ], options);
}

function compose(args) {
  const prefix = [
    "cd", shellQuote(composeDir), "&&",
    "docker", "compose",
    "--project-name", shellQuote(composeProject),
    "--env-file", ".env",
    "-f", "docker-compose.yml",
    "--profile", "bpi",
  ].join(" ");
  return ssh(`${prefix} ${args}`);
}

function remotePsql(database, file, variables = {}) {
  const args = [
    "docker", "exec", "-i", postgresContainer,
    "psql", "-X", "-U", "adp", "-d", database,
    "-v", "ON_ERROR_STOP=1",
  ];
  for (const [key, value] of Object.entries(variables)) {
    if (!/^[a-z_][a-z0-9_]*$/.test(key)) throw new Error(`invalid psql variable ${key}`);
    args.push("-v", `${key}=${value}`);
  }
  return ssh(args.map(shellQuote).join(" "), { input: fs.readFileSync(file, "utf8") });
}

function remoteQuery(database, statement) {
  const command = [
    "docker", "exec", postgresContainer,
    "psql", "-X", "-U", "adp", "-d", database,
    "-At", "-v", "ON_ERROR_STOP=1", "-c", statement,
  ].map(shellQuote).join(" ");
  return ssh(command);
}

function remoteEnv(keys) {
  const program = [
    "import json,pathlib,sys",
    "path=pathlib.Path(sys.argv[1])",
    "wanted=set(sys.argv[2:])",
    "values={}",
    "for raw in path.read_text().splitlines():",
    "    if '=' not in raw or raw.lstrip().startswith('#'): continue",
    "    key,value=raw.split('=',1)",
    "    if key in wanted: values[key]=value",
    "print(json.dumps(values,sort_keys=True))",
  ].join("\n");
  return JSON.parse(ssh([
    "python3", "-c", shellQuote(program), shellQuote(envPath),
    ...keys.map(shellQuote),
  ].join(" ")));
}

function updateRemoteEnv(updates) {
  const encoded = Buffer.from(JSON.stringify(updates), "utf8").toString("base64");
  const program = [
    "import base64,json,pathlib,sys",
    "path=pathlib.Path(sys.argv[1])",
    "updates=json.loads(base64.b64decode(sys.argv[2]).decode())",
    "seen=set()",
    "result=[]",
    "for raw in path.read_text().splitlines():",
    "    if '=' in raw and not raw.lstrip().startswith('#'):",
    "        key=raw.split('=',1)[0]",
    "        if key in updates:",
    "            if key not in seen: result.append(key+'='+str(updates[key])); seen.add(key)",
    "            continue",
    "    result.append(raw)",
    "for key,value in updates.items():",
    "    if key not in seen: result.append(key+'='+str(value))",
    "temporary=path.with_suffix(path.suffix+'.tmp')",
    "temporary.write_text('\\n'.join(result)+'\\n')",
    "temporary.replace(path)",
  ].join("\n");
  ssh(["python3", "-c", shellQuote(program), shellQuote(envPath), shellQuote(encoded)].join(" "));
}

function kafkaOffsets(topic) {
  const output = ssh([
    "docker", "exec", kafkaContainer,
    "/opt/kafka/bin/kafka-get-offsets.sh",
    "--bootstrap-server", kafkaBootstrap,
    "--topic", topic,
  ].map(shellQuote).join(" "));
  const offsets = {};
  for (const line of output.split(/\r?\n/)) {
    const match = line.trim().match(/^(.+):(\d+):(\d+)$/);
    if (match && match[1] === topic) offsets[match[2]] = Number(match[3]);
  }
  if (Object.keys(offsets).length === 0) throw new Error(`no Kafka offsets found for ${topic}`);
  return offsets;
}

function offsetTotal(offsets) {
  return Object.values(offsets).reduce((sum, value) => sum + value, 0);
}

function offsetDelta(before, after) {
  const partitions = new Set([...Object.keys(before), ...Object.keys(after)]);
  const result = {};
  for (const partition of partitions) {
    const delta = (after[partition] || 0) - (before[partition] || 0);
    if (delta !== 0) result[partition] = delta;
  }
  return result;
}

function consumerLag(group) {
  const command = [
    "docker", "exec", kafkaContainer,
    "/opt/kafka/bin/kafka-consumer-groups.sh",
    "--bootstrap-server", kafkaBootstrap,
    "--describe", "--group", group,
  ].map(shellQuote).join(" ");
  const output = ssh(command);
  let lag = 0;
  for (const line of output.split(/\r?\n/)) {
    const columns = line.trim().split(/\s+/);
    if (columns[0] === group && /^\d+$/.test(columns[5] || "")) lag += Number(columns[5]);
  }
  return { lag, output };
}

function waitFor(label, probe, predicate) {
  const deadline = Date.now() + waitTimeoutMs;
  let current;
  let lastError;
  while (Date.now() < deadline) {
    try {
      current = probe();
      if (predicate(current)) return current;
      lastError = null;
    } catch (error) {
      lastError = error;
    }
    sleep(1000);
  }
  throw new Error(`${label} timed out: ${lastError?.message || JSON.stringify(current)}`);
}

function sleep(milliseconds) {
  Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, milliseconds);
}

function containerState(container) {
  return ssh(`docker inspect --format ${shellQuote("{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}")} ${shellQuote(container)}`);
}

function waitHealthy(container) {
  return waitFor(`${container} healthy`, () => containerState(container), (value) => value === "healthy");
}

function waitMaterialReady() {
  return waitFor("material service port", () => ssh(
    `docker exec ${shellQuote(materialContainer)} bash -c ${shellQuote("exec 3<>/dev/tcp/127.0.0.1/8080")}`,
  ), () => true);
}

function generateFixture() {
  run(process.execPath, [path.join(__dirname, "generate-bpi-wms-outage-fixture.js")], {
    env: { ...process.env, BPI_ACCEPTANCE_MARKER: marker, BPI_FIXTURE_OUTPUT: fixturePath },
  });
  const value = JSON.parse(fs.readFileSync(fixturePath, "utf8"));
  if (value.marker !== marker || value.command.idempotencyKey !== `${marker}|WMS|1`) {
    throw new Error("generated fixture identity is invalid");
  }
  return value;
}

function preflight() {
  const env = remoteEnv([...safetyKeys, "BPI_WMS_ADAPTER_ROUTES", "MATERIAL_WMS_BPI_API_KEY"]);
  for (const key of safetyKeys) {
    if (env[key] !== "false") throw new Error(`${key} must be false before the outage rehearsal`);
  }
  if (env.BPI_WMS_ADAPTER_ROUTES !== "_DENY_ALL_") {
    throw new Error("BPI_WMS_ADAPTER_ROUTES must be _DENY_ALL_ before the outage rehearsal");
  }
  if (!env.MATERIAL_WMS_BPI_API_KEY || env.MATERIAL_WMS_BPI_API_KEY === "_DISABLED_") {
    throw new Error("MATERIAL_WMS_BPI_API_KEY is not configured on the target");
  }
  for (const container of [postgresContainer, materialContainer, bpiServiceContainer, wmsAdapterContainer]) {
    const state = containerState(container);
    if (!new Set(["healthy", "running"]).has(state)) {
      throw new Error(`${container} is not ready: ${state}`);
    }
  }
  const pending = Number(remoteQuery("ft_mes_bpi", [
    "SELECT count(*) FROM bpi.bpi_outbox_events",
    "WHERE event_type='WMS_COMPLETION_INBOUND_COMMAND'",
    "AND status IN ('PENDING','DISPATCHING')",
  ].join(" ")));
  if (pending !== 0) throw new Error(`target has ${pending} active WMS outbox rows`);
  const conflictingFlags = Number(remoteQuery("ft_mes_bpi", [
    "SELECT count(*) FROM bpi.bpi_feature_flags",
    "WHERE tenant_id='1000' AND scope_type='PLANT' AND scope_key='PLANT-01'",
    "AND flag_key IN ('bpi.commands','bpi.wms-link')",
  ].join(" ")));
  if (conflictingFlags !== 0) throw new Error(`target has ${conflictingFlags} conflicting feature flags`);
  const markerBpiRows = Number(remoteQuery("ft_mes_bpi",
    `SELECT count(*) FROM bpi.bpi_batch_instances WHERE tenant_id='1000' AND batch_no='${marker}'`));
  const markerMaterialRows = Number(remoteQuery("adp",
    `SELECT count(*) FROM wms_batch_stocks WHERE tenant_id='1000' AND batch_no='${marker}'`));
  if (markerBpiRows + markerMaterialRows !== 0) throw new Error("target already contains the marker");
  const wmsLag = consumerLag(wmsGroup);
  const receiptLag = consumerLag(receiptGroup);
  if (wmsLag.lag !== 0 || receiptLag.lag !== 0) {
    throw new Error(`Kafka consumer lag must be zero: wms=${wmsLag.lag}, receipt=${receiptLag.lag}`);
  }
  return {
    switches: Object.fromEntries(safetyKeys.map((key) => [key, env[key]])),
    route: env.BPI_WMS_ADAPTER_ROUTES,
    materialApiKeyConfigured: true,
    activeWmsOutboxRows: pending,
    conflictingFeatureFlags: conflictingFlags,
    markerRows: markerBpiRows + markerMaterialRows,
    consumerLag: { wms: wmsLag.lag, receipt: receiptLag.lag },
    offsets: {
      command: kafkaOffsets(commandTopic),
      commandDlq: kafkaOffsets(commandDlqTopic),
      receipt: kafkaOffsets(receiptTopic),
    },
  };
}

function consumeDlqHeaders(partition, offset) {
  const command = [
    "docker", "exec", kafkaContainer,
    "/opt/kafka/bin/kafka-console-consumer.sh",
    "--bootstrap-server", kafkaBootstrap,
    "--topic", commandDlqTopic,
    "--partition", String(partition),
    "--offset", String(offset),
    "--max-messages", "1",
    "--property", "print.value=false",
    "--property", "print.headers=true",
  ].map(shellQuote).join(" ");
  return ssh(command);
}

function runBrowser() {
  const nodePath = process.env.NODE_PATH || path.join(repoRoot, "frontend/apps/bpi/node_modules");
  run(process.execPath, [path.join(__dirname, "adp-bpi-wms-outage-recovery-acceptance.js")], {
    env: {
      ...process.env,
      NODE_PATH: nodePath,
      BPI_ACCEPTANCE_MARKER: marker,
      BPI_BATCH_ID: fixture.ids.batchId,
      BPI_COMMAND_EVENT_ID: fixture.ids.commandEventId,
      BPI_WMS_IDEMPOTENCY_KEY: fixture.command.idempotencyKey,
      BPI_BROWSER_REPORT: browserReportPath,
      BPI_BROWSER_SCREENSHOT: screenshotPath,
    },
  });
  return JSON.parse(fs.readFileSync(browserReportPath, "utf8"));
}

function cleanup() {
  if (cleanupRunning) return report.cleanup;
  cleanupRunning = true;
  const result = { environmentRestored: false, servicesRestored: false, residualRows: null, errors: [] };
  try {
    if (backupCreated) {
      ssh(`cp ${shellQuote(envBackup)} ${shellQuote(envPath)}`);
      runtimeChanged = false;
      result.environmentRestored = true;
    }
  } catch (error) {
    result.errors.push(`restore env: ${error.message}`);
  }
  try {
    compose("up -d material");
    waitMaterialReady();
    if (backupCreated) {
      compose("up -d --no-deps --force-recreate bpi-service bpi-wms-adapter");
      waitHealthy(bpiServiceContainer);
      waitHealthy(wmsAdapterContainer);
    }
    result.servicesRestored = true;
  } catch (error) {
    result.errors.push(`restore services: ${error.message}`);
  }
  if (fixtureInserted && fixture) {
    try {
      remotePsql("adp", sql.cleanupMaterial, {
        marker,
        outbox_id: fixture.ids.commandEventId,
      });
    } catch (error) {
      result.errors.push(`material cleanup: ${error.message}`);
    }
    try {
      remotePsql("ft_mes_bpi", sql.cleanupBpi, fixture.sqlVariables);
    } catch (error) {
      result.errors.push(`BPI cleanup: ${error.message}`);
    }
  }
  try {
    const restored = remoteEnv([...safetyKeys, "BPI_WMS_ADAPTER_ROUTES"]);
    const unsafe = safetyKeys.filter((key) => restored[key] !== "false");
    if (unsafe.length || restored.BPI_WMS_ADAPTER_ROUTES !== "_DENY_ALL_") {
      throw new Error(`unsafe restored environment: ${unsafe.join(",") || restored.BPI_WMS_ADAPTER_ROUTES}`);
    }
    const bpiRows = fixture ? Number(remoteQuery("ft_mes_bpi",
      `SELECT count(*) FROM bpi.bpi_batch_instances WHERE id='${fixture.ids.batchId}'::uuid`)) : 0;
    const materialRows = fixture ? Number(remoteQuery("adp",
      `SELECT count(*) FROM wms_batch_stocks WHERE tenant_id='1000' AND batch_no='${marker}'`)) : 0;
    result.residualRows = bpiRows + materialRows;
    if (result.residualRows !== 0) throw new Error(`cleanup left ${result.residualRows} marker rows`);
  } catch (error) {
    result.errors.push(`post-cleanup verification: ${error.message}`);
  }
  try {
    if (backupCreated && result.environmentRestored && result.servicesRestored && result.errors.length === 0) {
      ssh(`rm -f ${shellQuote(envBackup)}`);
      backupCreated = false;
    }
  } catch (error) {
    result.errors.push(`remove backup: ${error.message}`);
  }
  report.cleanup = result;
  return result;
}

function main() {
  fs.mkdirSync(path.dirname(targetReportPath), { recursive: true });
  fixture = generateFixture();
  report.fixture = {
    batchId: fixture.ids.batchId,
    commandEventId: fixture.ids.commandEventId,
    wmsIdempotencyKey: fixture.command.idempotencyKey,
    payloadSha256: fixture.command.payloadSha256,
  };
  report.preflight = preflight();
  ssh(`test ! -e ${shellQuote(envBackup)} && cp ${shellQuote(envPath)} ${shellQuote(envBackup)}`);
  backupCreated = true;
  remotePsql("ft_mes_bpi", sql.fixture, fixture.sqlVariables);
  fixtureInserted = true;

  compose("stop material");
  const stopped = containerState(materialContainer);
  if (stopped !== "exited") throw new Error(`material did not stop: ${stopped}`);

  updateRemoteEnv({
    BPI_PHASE2_INTEGRATION_ENABLED: "true",
    BPI_PHASE2_PROTOBUF_HTTP_INGRESS_ENABLED: "false",
    BPI_PHASE2_KAFKA_ENABLED: "true",
    BPI_PHASE2_ALLOWED_TENANT_IDS: "1000",
    BPI_PHASE2_ALLOWED_PLANT_IDS: "PLANT-01",
    BPI_PHASE2_ALLOWED_LINE_IDS: "LINE-S07-01",
    BPI_WMS_OUTBOX_ENABLED: "true",
    BPI_WMS_RECONCILIATION_DELAY: "2s",
    BPI_WMS_ADAPTER_ENABLED: "true",
    BPI_WMS_ADAPTER_ROUTES: "1000|PLANT-01|LINE-S07-01|WARE-E2E|LOC-E2E|COMP-E2E|kg",
    BPI_WMS_ADAPTER_MAX_ATTEMPTS: "3",
    BPI_WMS_ADAPTER_RETRY_BACKOFF: "1s",
    BPI_WMS_ADAPTER_REQUEST_TIMEOUT: "2s",
  });
  runtimeChanged = true;
  compose("up -d --no-deps --force-recreate bpi-service bpi-wms-adapter");
  waitHealthy(bpiServiceContainer);
  waitHealthy(wmsAdapterContainer);
  if (containerState(materialContainer) !== "exited") {
    throw new Error("material was restarted before the outage command completed");
  }

  const firstOutbox = waitFor("first WMS command publication", () => remoteQuery("ft_mes_bpi", [
    "SELECT status || '|' || revision || '|' || total_attempt_count || '|' || manual_retry_count",
    `FROM bpi.bpi_outbox_events WHERE id='${fixture.ids.commandEventId}'::uuid`,
  ].join(" ")), (value) => value === "PUBLISHED|3|1|0");
  const firstDlqOffsets = waitFor("WMS command DLQ", () => kafkaOffsets(commandDlqTopic),
    (value) => offsetTotal(value) === offsetTotal(report.preflight.offsets.commandDlq) + 1);
  const dlqDelta = offsetDelta(report.preflight.offsets.commandDlq, firstDlqOffsets);
  const changedPartitions = Object.entries(dlqDelta).filter(([, delta]) => delta === 1);
  if (changedPartitions.length !== 1) throw new Error(`unexpected command DLQ delta ${JSON.stringify(dlqDelta)}`);
  const [dlqPartition] = changedPartitions[0];
  const dlqHeaders = consumeDlqHeaders(
    dlqPartition,
    report.preflight.offsets.commandDlq[dlqPartition] || 0,
  );
  if (!dlqHeaders.includes(fixture.ids.commandEventId)) {
    throw new Error("command DLQ record does not carry the original event_id header");
  }
  const materialBeforeRecovery = Number(remoteQuery("adp",
    `SELECT count(*) FROM wms_stock_documents WHERE tenant_id='1000' AND source_system='BPI' AND source_document_id='${fixture.ids.commandEventId}'`));
  if (materialBeforeRecovery !== 0) throw new Error("material document exists while material-wms was stopped");
  report.firstDelivery = {
    materialServiceState: "exited",
    outbox: firstOutbox,
    commandDlqDelta: dlqDelta,
    commandEventHeaderVerified: true,
    materialDocumentRows: materialBeforeRecovery,
  };

  compose("up -d material");
  waitMaterialReady();
  report.browser = runBrowser();
  if (report.browser.status !== "PASS_BROWSER_API_DURABLE_RECEIPT") {
    throw new Error(`browser recovery failed: ${report.browser.status}`);
  }

  const bpiEvidence = remotePsql("ft_mes_bpi", sql.verifyBpi, fixture.sqlVariables);
  const materialEvidence = remotePsql("adp", sql.verifyMaterial, {
    marker,
    outbox_id: fixture.ids.commandEventId,
  });
  report.persistence = { bpi: bpiEvidence, material: materialEvidence };

  const finalOffsets = {
    command: kafkaOffsets(commandTopic),
    commandDlq: kafkaOffsets(commandDlqTopic),
    receipt: kafkaOffsets(receiptTopic),
  };
  const deltas = {
    command: offsetTotal(finalOffsets.command) - offsetTotal(report.preflight.offsets.command),
    commandDlq: offsetTotal(finalOffsets.commandDlq) - offsetTotal(report.preflight.offsets.commandDlq),
    receipt: offsetTotal(finalOffsets.receipt) - offsetTotal(report.preflight.offsets.receipt),
  };
  if (deltas.command !== 2 || deltas.commandDlq !== 1 || deltas.receipt !== 1) {
    throw new Error(`unexpected Kafka deltas ${JSON.stringify(deltas)}`);
  }
  const finalWmsLag = consumerLag(wmsGroup).lag;
  const finalReceiptLag = consumerLag(receiptGroup).lag;
  if (finalWmsLag !== 0 || finalReceiptLag !== 0) {
    throw new Error(`final Kafka lag is not zero: wms=${finalWmsLag}, receipt=${finalReceiptLag}`);
  }
  report.kafka = { offsets: finalOffsets, deltas, consumerLag: { wms: finalWmsLag, receipt: finalReceiptLag } };
  report.status = "PASS_TARGET_OUTAGE_RECOVERY_BEFORE_CLEANUP";
}

try {
  main();
} catch (error) {
  report.error = error?.message || String(error);
  process.exitCode = 1;
} finally {
  const cleanupResult = cleanup();
  if (cleanupResult.errors.length > 0 || cleanupResult.residualRows !== 0) {
    report.status = "FAIL_CLEANUP";
    process.exitCode = 1;
  } else if (report.status === "PASS_TARGET_OUTAGE_RECOVERY_BEFORE_CLEANUP") {
    report.status = "PASS_TARGET_OUTAGE_RECOVERY_CLEANED";
  }
  report.generatedAt = new Date().toISOString();
  fs.writeFileSync(targetReportPath, `${JSON.stringify(report, null, 2)}\n`, "utf8");
  process.stdout.write(`${targetReportPath}\n`);
}
