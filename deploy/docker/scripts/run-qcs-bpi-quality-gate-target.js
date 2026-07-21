#!/usr/bin/env node
"use strict";

const crypto = require("node:crypto");
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
const bpiServiceContainer = `${composeProject}-bpi-service-1`;
const qcsOutboxContainer = `${composeProject}-qcs-quality-gate-outbox-1`;
const kafkaContainer = process.env.BPI_TARGET_KAFKA_CONTAINER
  || "ft-mes-bpi-streaming-kafka-1-1";
const kafkaBootstrap = process.env.BPI_TARGET_KAFKA_BOOTSTRAP || "kafka-1:9092";
const topic = "qcs.batch.quality-gate.v1";
const dlqTopic = "qcs.batch.quality-gate.dlq.v1";
const consumerGroup = "ft-mes-bpi-phase2-integrations-v1";
const marker = process.env.BPI_ACCEPTANCE_MARKER || defaultMarker();
const womLineId = process.env.ADP_WOM_LINE_ID || "8388157374858567";
const orderId = `${marker}_TASK_TN`;
const materialCode = `${marker}_MAT`;
const batchId = crypto.randomUUID();
const qcsFlagId = crypto.randomUUID();
const baseUrl = String(process.env.ADP_BASE_URL || "http://10.11.100.17:18080").replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD || "123456";
const outputDir = path.resolve(process.env.QCS_BPI_OUTPUT_DIR || `/tmp/${marker}-qcs-bpi`);
const qcsEvidencePath = path.join(outputDir, "qcs-report-chain.json");
const womEvidencePath = path.join(outputDir, "wom-manu-inspect.json");
const targetReportPath = path.resolve(
  process.env.QCS_BPI_TARGET_REPORT || path.join(outputDir, "qcs-bpi-target-acceptance.json"),
);
const envPath = `${composeDir}/.env`;
const envBackup = `${composeDir}/.env.${marker}.backup`;
const waitTimeoutMs = Number(process.env.BPI_TARGET_WAIT_TIMEOUT_MS || 240_000);

const sql = {
  fixture: path.join(__dirname, "bpi-qcs-quality-gate-target-fixture.sql"),
  verify: path.join(__dirname, "bpi-qcs-quality-gate-target-verification.sql"),
  cleanup: path.join(__dirname, "bpi-qcs-quality-gate-target-cleanup.sql"),
};

const safetyKeys = [
  "BPI_PHASE2_INTEGRATION_ENABLED",
  "BPI_PHASE2_PROTOBUF_HTTP_INGRESS_ENABLED",
  "BPI_PHASE2_KAFKA_ENABLED",
  "QCS_BPI_OUTBOX_ENABLED",
  "BPI_WMS_OUTBOX_ENABLED",
  "BPI_WMS_ADAPTER_ENABLED",
];

validateInputs();
fs.mkdirSync(outputDir, { recursive: true });

const report = {
  generatedAt: new Date().toISOString(),
  status: "FAIL",
  database: "PostgreSQL",
  marker,
  target: {
    host: sshTarget.replace(/^[^@]+@/, ""),
    composeProject,
    runtimeRoot,
    baseUrl,
  },
  scope: {
    tenantId: "1000",
    plantId: "PLANT-01",
    lineId: "LINE-S07-01",
    womLineId,
  },
  fixture: { batchId, qcsFlagId, orderId, materialCode },
  preflight: null,
  activation: null,
  qcs: null,
  firstDelivery: null,
  replay: null,
  kafka: null,
  cleanup: null,
  error: null,
};

let backupCreated = false;
let fixtureAttempted = false;
let runtimeChanged = false;
let cleanupRunning = false;
let lastOutbox = null;

function defaultMarker() {
  const timestamp = new Date().toISOString().replace(/[-:TZ.]/g, "").slice(0, 14);
  return `ADP_E2E_${timestamp}_QCS_BPI`;
}

function validateInputs() {
  if (process.env.QCS_BPI_CONFIRM_TARGET !== "YES") {
    throw new Error("Set QCS_BPI_CONFIRM_TARGET=YES to run the controlled QCS/BPI target acceptance.");
  }
  if (!/^[A-Za-z0-9_.@-]+$/.test(sshTarget)) throw new Error("BPI_TARGET_SSH is invalid");
  if (!/^\/[A-Za-z0-9_./-]+$/.test(runtimeRoot)) {
    throw new Error("BPI_TARGET_RUNTIME_ROOT must be an absolute safe path");
  }
  if (!/^[A-Za-z0-9_-]+$/.test(composeProject)) {
    throw new Error("BPI_TARGET_COMPOSE_PROJECT is invalid");
  }
  if (!/^[A-Za-z0-9_-]{8,90}$/.test(marker)) {
    throw new Error("BPI_ACCEPTANCE_MARKER must use 8-90 letters, digits, underscores or hyphens");
  }
  if (!/^\d+$/.test(womLineId)) throw new Error("ADP_WOM_LINE_ID must be an unsigned integer");
  if (orderId.length > 128) throw new Error("The generated WOM table_no exceeds the BPI order_id limit");
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
    maxBuffer: 32 * 1024 * 1024,
    ...options,
  });
  if (result.error) throw result.error;
  if (result.status !== 0) {
    const detail = String(result.stderr || result.stdout || "").trim().slice(-5000);
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

function parseLastJson(output, label) {
  const lines = String(output || "").split(/\r?\n/).map((line) => line.trim()).filter(Boolean);
  for (let index = lines.length - 1; index >= 0; index -= 1) {
    if (!lines[index].startsWith("{")) continue;
    try {
      return JSON.parse(lines[index]);
    } catch (_error) {
      // Keep searching because psql may emit non-JSON notices before the result.
    }
  }
  throw new Error(`${label} did not return a JSON object: ${String(output || "").slice(-2000)}`);
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

function containerState(container) {
  return ssh(`docker inspect --format ${shellQuote("{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}")} ${shellQuote(container)}`);
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

function waitHealthy(container) {
  return waitFor(`${container} healthy`, () => containerState(container), (value) => value === "healthy");
}

function kafkaOffsets(topicName) {
  const output = ssh([
    "docker", "exec", kafkaContainer,
    "/opt/kafka/bin/kafka-get-offsets.sh",
    "--bootstrap-server", kafkaBootstrap,
    "--topic", topicName,
  ].map(shellQuote).join(" "));
  const offsets = {};
  for (const line of output.split(/\r?\n/)) {
    const match = line.trim().match(/^(.+):(\d+):(\d+)$/);
    if (match && match[1] === topicName) offsets[match[2]] = Number(match[3]);
  }
  if (Object.keys(offsets).length === 0) throw new Error(`no Kafka offsets found for ${topicName}`);
  return offsets;
}

function offsetTotal(offsets) {
  return Object.values(offsets).reduce((sum, value) => sum + value, 0);
}

function offsetDelta(before, after) {
  const partitions = new Set([...Object.keys(before), ...Object.keys(after)]);
  return Object.fromEntries([...partitions].map((partition) => [
    partition,
    (after[partition] || 0) - (before[partition] || 0),
  ]));
}

function consumerLag(topicName) {
  const output = ssh([
    "docker", "exec", kafkaContainer,
    "/opt/kafka/bin/kafka-consumer-groups.sh",
    "--bootstrap-server", kafkaBootstrap,
    "--describe", "--group", consumerGroup,
  ].map(shellQuote).join(" "));
  let lag = 0;
  let partitions = 0;
  let uncommittedPartitions = 0;
  for (const line of output.split(/\r?\n/)) {
    const columns = line.trim().split(/\s+/);
    if (columns[0] !== consumerGroup || columns[1] !== topicName) continue;
    partitions += 1;
    if (/^\d+$/.test(columns[5] || "")) {
      lag += Number(columns[5]);
    } else if (columns[3] === "-" && /^\d+$/.test(columns[4] || "")) {
      lag += Number(columns[4]);
      uncommittedPartitions += 1;
    } else {
      throw new Error(`could not parse Kafka lag row: ${line}`);
    }
  }
  return { lag, partitions, uncommittedPartitions, output };
}

function outboxState() {
  const output = remoteQuery("adp", `
SELECT json_build_object(
  'id', id,
  'eventId', event_id,
  'idempotencyKey', idempotency_key,
  'qcsReportId', qcs_report_id,
  'qcsReportVersion', qcs_report_version,
  'qcsInspectId', qcs_inspect_id,
  'womTaskId', wom_task_id,
  'tenantId', tenant_id,
  'plantId', plant_id,
  'lineId', line_id,
  'sourceOrderId', source_order_id,
  'qualityGateId', quality_gate_id,
  'qualityGateRevision', quality_gate_revision,
  'publicationState', publication_state,
  'blockReason', block_reason,
  'resolvedBatchId', resolved_batch_id,
  'payloadSha256', payload_sha256,
  'attemptCount', attempt_count,
  'lastError', last_error,
  'inspections', inspections,
  'sentAt', sent_at
)
FROM public.qcs_bpi_quality_gate_outbox
WHERE source_order_id = '${orderId}'
ORDER BY id DESC
LIMIT 1;
  `);
  return output ? parseLastJson(output, "QCS outbox query") : null;
}

function bpiState() {
  return parseLastJson(remotePsql("ft_mes_bpi", sql.verify, { batch_id: batchId }), "BPI verification");
}

function assertFirstDelivery(outbox, state) {
  if (!outbox || outbox.publicationState !== "SENT" || outbox.attemptCount !== 1) {
    throw new Error(`QCS outbox did not publish once: ${JSON.stringify(outbox)}`);
  }
  if (outbox.resolvedBatchId !== batchId || !/^[0-9a-f]{64}$/.test(outbox.payloadSha256 || "")) {
    throw new Error(`QCS outbox canonical identity is invalid: ${JSON.stringify(outbox)}`);
  }
  if (outbox.tenantId !== "1000" || outbox.plantId !== "PLANT-01"
      || outbox.lineId !== "LINE-S07-01" || outbox.sourceOrderId !== orderId) {
    throw new Error(`QCS outbox scope/order identity is invalid: ${JSON.stringify(outbox)}`);
  }
  if (!state.batch || state.batch.state !== "RELEASED" || Number(state.batch.revision) !== 3
      || state.batch.qualityGate !== "ACCEPTED" || state.batch.wmsStatus !== "NOT_REQUESTED"
      || state.batch.shadow !== true) {
    throw new Error(`BPI batch projection is invalid: ${JSON.stringify(state.batch)}`);
  }
  if (Number(state.qualityGateCount) !== 1 || state.qualityGate?.state !== "ACCEPTED"
      || Number(state.qualityLinkCount) < 1 || Number(state.inboxCount) !== 1
      || Number(state.stateEventCount) !== 2 || Number(state.auditCount) !== 2
      || Number(state.wmsOutboxCount) !== 0) {
    throw new Error(`BPI quality persistence is incomplete: ${JSON.stringify(state)}`);
  }
  const actions = state.stateActions || [];
  if (actions.join("|") !== "QUALITY_GATE_OPENED|QUALITY_GATE_ACCEPTED") {
    throw new Error(`BPI quality state actions are invalid: ${JSON.stringify(actions)}`);
  }
}

function stableProjection(state) {
  return {
    batchState: state.batch?.state,
    batchRevision: Number(state.batch?.revision),
    qualityGate: state.batch?.qualityGate,
    wmsStatus: state.batch?.wmsStatus,
    gateCount: Number(state.qualityGateCount),
    gateChecksum: state.qualityGate?.payloadChecksum,
    linkCount: Number(state.qualityLinkCount),
    inboxCount: Number(state.inboxCount),
    stateEventCount: Number(state.stateEventCount),
    auditCount: Number(state.auditCount),
    wmsOutboxCount: Number(state.wmsOutboxCount),
  };
}

function runQcsBrowserChain() {
  const script = path.join(__dirname, "adp-qcs-report-chain-persistence-acceptance.js");
  const nodePath = process.env.NODE_PATH || path.join(repoRoot, "frontend/apps/bpi/node_modules");
  const result = spawnSync(process.execPath, [script], {
    encoding: "utf8",
    maxBuffer: 32 * 1024 * 1024,
    env: {
      ...process.env,
      NODE_PATH: nodePath,
      ADP_BASE_URL: baseUrl,
      ADP_BROWSER_BASE_URL: baseUrl,
      ADP_USERNAME: username,
      ADP_PASSWORD: password,
      ADP_DB_SSH_TARGET: sshTarget,
      ADP_DB_CONTAINER: postgresContainer,
      ADP_DB_NAME: "adp",
      ADP_DB_USER: "adp",
      ADP_E2E_MARKER: marker,
      ADP_WOM_LINE_ID: womLineId,
      ADP_QCS_REPORT_CHAIN_MODE: "qualified",
      ADP_OUTPUT_DIR: outputDir,
      ADP_QCS_REPORT_CHAIN_PERSISTENCE_OUTPUT: qcsEvidencePath,
      ADP_WOM_MANU_INSPECT_PERSISTENCE_OUTPUT: womEvidencePath,
      ADP_PAGE_TIMEOUT_MS: "240000",
      ADP_NAV_WAIT_UNTIL: "commit",
    },
  });
  const evidence = fs.existsSync(qcsEvidencePath)
    ? JSON.parse(fs.readFileSync(qcsEvidencePath, "utf8"))
    : null;
  report.qcs = {
    status: evidence?.status || "FAIL",
    evidencePath: qcsEvidencePath,
    evidence,
    stdout: String(result.stdout || "").slice(-12000),
    stderr: String(result.stderr || "").slice(-12000),
  };
  if (result.error) throw result.error;
  if (result.status !== 0 || evidence?.status !== "PASS") {
    throw new Error(`QCS browser chain failed with exit ${result.status}: ${String(result.stderr || result.stdout || "").slice(-3000)}`);
  }
  return evidence;
}

function preflight() {
  const env = remoteEnv(safetyKeys);
  for (const key of safetyKeys) {
    if (env[key] !== "false") throw new Error(`${key} must be false before QCS/BPI acceptance`);
  }
  for (const container of [postgresContainer, bpiServiceContainer, qcsOutboxContainer]) {
    const state = containerState(container);
    if (state !== "healthy") throw new Error(`${container} is not healthy: ${state}`);
  }
  const activeOutbox = Number(remoteQuery("adp", `
SELECT count(*) FROM public.qcs_bpi_quality_gate_outbox
WHERE publication_state IN ('READY','SENDING','RETRY');
  `));
  if (activeOutbox !== 0) throw new Error(`target has ${activeOutbox} active QCS outbox rows`);
  const conflicts = Number(remoteQuery("ft_mes_bpi", `
SELECT count(*) FROM bpi.bpi_feature_flags
WHERE tenant_id='1000' AND scope_type='LINE' AND scope_key='LINE-S07-01'
  AND flag_key='bpi.qcs-link';
  `));
  if (conflicts !== 0) throw new Error(`target has ${conflicts} conflicting bpi.qcs-link overrides`);
  const markerRows = Number(remoteQuery("ft_mes_bpi", `
SELECT count(*) FROM bpi.bpi_batch_instances
WHERE tenant_id='1000' AND (id='${batchId}'::uuid OR order_id='${orderId}');
  `));
  if (markerRows !== 0) throw new Error("target already contains the QCS/BPI marker");
  const topicDescription = ssh([
    "docker", "exec", kafkaContainer,
    "/opt/kafka/bin/kafka-topics.sh",
    "--bootstrap-server", kafkaBootstrap,
    "--describe", "--topic", topic,
  ].map(shellQuote).join(" "));
  const dlqDescription = ssh([
    "docker", "exec", kafkaContainer,
    "/opt/kafka/bin/kafka-topics.sh",
    "--bootstrap-server", kafkaBootstrap,
    "--describe", "--topic", dlqTopic,
  ].map(shellQuote).join(" "));
  if (!topicDescription.includes("PartitionCount: 3") || !topicDescription.includes("ReplicationFactor: 3")
      || !dlqDescription.includes("PartitionCount: 3") || !dlqDescription.includes("ReplicationFactor: 3")) {
    throw new Error("QCS Kafka topics are not the expected 3-partition/3-replica production shape");
  }
  return {
    switches: env,
    activeOutboxRows: activeOutbox,
    conflictingFlags: conflicts,
    markerRows,
    topicDescription,
    dlqDescription,
    offsets: { topic: kafkaOffsets(topic), dlq: kafkaOffsets(dlqTopic) },
    consumer: consumerLag(topic),
  };
}

function cleanup() {
  if (cleanupRunning) return report.cleanup;
  cleanupRunning = true;
  const result = {
    environmentRestored: false,
    servicesRestored: false,
    adpResidualRows: null,
    bpiResidualRows: null,
    switches: null,
    consumerLag: null,
    errors: [],
  };
  try {
    if (backupCreated) {
      ssh(`cp ${shellQuote(envBackup)} ${shellQuote(envPath)}`);
      runtimeChanged = false;
      result.environmentRestored = true;
      compose("up -d --no-deps --force-recreate bpi-service qcs-quality-gate-outbox");
      waitHealthy(bpiServiceContainer);
      waitHealthy(qcsOutboxContainer);
      result.servicesRestored = true;
    }
  } catch (error) {
    result.errors.push(`restore runtime: ${error.message}`);
  }
  try {
    const existing = outboxState();
    const eventId = lastOutbox?.eventId || existing?.eventId || "_NONE_";
    remoteQuery("adp", `
DELETE FROM public.qcs_bpi_quality_gate_outbox WHERE source_order_id='${orderId}';
    `);
    if (fixtureAttempted) {
      const cleanupOutput = remotePsql("ft_mes_bpi", sql.cleanup, {
        batch_id: batchId,
        qcs_flag_id: qcsFlagId,
        event_id: eventId,
      });
      const lines = cleanupOutput.trim().split(/\r?\n/).map((line) => line.trim());
      const residual = [...lines].reverse().find((line) => /^\d+$/.test(line));
      result.bpiResidualRows = residual == null ? null : Number(residual);
      if (result.bpiResidualRows !== 0) {
        throw new Error(`BPI cleanup left ${result.bpiResidualRows} rows`);
      }
    } else {
      result.bpiResidualRows = 0;
    }
    result.adpResidualRows = Number(remoteQuery("adp", `
SELECT
  (SELECT count(*) FROM public.qcs_bpi_quality_gate_outbox WHERE source_order_id='${orderId}')
  + (SELECT count(*) FROM public.wom_produce_tasks WHERE table_no='${orderId}')
  + (SELECT count(*) FROM public.baseset_materials WHERE code='${materialCode}')
  + (SELECT count(*) FROM public.qcs_inspect_reports WHERE memo_field LIKE '${marker}%');
    `));
    if (result.adpResidualRows !== 0) {
      throw new Error(`ADP cleanup left ${result.adpResidualRows} rows`);
    }
  } catch (error) {
    result.errors.push(`marker cleanup: ${error.message}`);
  }
  try {
    result.switches = remoteEnv(safetyKeys);
    const unsafe = safetyKeys.filter((key) => result.switches[key] !== "false");
    if (unsafe.length) throw new Error(`unsafe restored switches: ${unsafe.join(",")}`);
    result.consumerLag = consumerLag(topic);
    if (result.consumerLag.lag !== 0) {
      throw new Error(`QCS consumer lag after restore is ${result.consumerLag.lag}`);
    }
  } catch (error) {
    result.errors.push(`post-cleanup gate: ${error.message}`);
  }
  try {
    if (backupCreated && result.environmentRestored && result.servicesRestored
        && result.adpResidualRows === 0 && result.bpiResidualRows === 0
        && result.errors.length === 0) {
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
  report.preflight = preflight();
  ssh(`test ! -e ${shellQuote(envBackup)} && cp ${shellQuote(envPath)} ${shellQuote(envBackup)}`);
  backupCreated = true;
  fixtureAttempted = true;
  report.fixture.seed = parseLastJson(remotePsql("ft_mes_bpi", sql.fixture, {
    marker,
    batch_id: batchId,
    qcs_flag_id: qcsFlagId,
    order_id: orderId,
    material_code: materialCode,
  }), "BPI QCS fixture");

  updateRemoteEnv({
    BPI_PHASE2_INTEGRATION_ENABLED: "true",
    BPI_PHASE2_PROTOBUF_HTTP_INGRESS_ENABLED: "false",
    BPI_PHASE2_KAFKA_ENABLED: "true",
    BPI_PHASE2_ALLOWED_TENANT_IDS: "1000",
    BPI_PHASE2_ALLOWED_PLANT_IDS: "PLANT-01",
    BPI_PHASE2_ALLOWED_LINE_IDS: "LINE-S07-01",
    QCS_BPI_OUTBOX_ENABLED: "true",
  });
  runtimeChanged = true;
  compose("up -d --no-deps --force-recreate bpi-service qcs-quality-gate-outbox");
  waitHealthy(bpiServiceContainer);
  waitHealthy(qcsOutboxContainer);
  const activeConsumer = waitFor(
    "QCS Kafka consumer assignment",
    () => consumerLag(topic),
    (value) => value.partitions === 3 && value.lag === 0,
  );
  report.activation = {
    switches: remoteEnv(safetyKeys),
    bpiService: containerState(bpiServiceContainer),
    qcsOutbox: containerState(qcsOutboxContainer),
    consumer: activeConsumer,
  };

  runQcsBrowserChain();
  const firstOutbox = waitFor(
    "QCS outbox first publication",
    () => outboxState(),
    (value) => {
      if (value && ["DEAD", "BLOCKED_MAPPING", "BLOCKED_STATE", "BLOCKED_DATA"].includes(value.publicationState)) {
        throw new Error(`QCS outbox entered ${value.publicationState}: ${value.blockReason || value.lastError}`);
      }
      return value?.publicationState === "SENT" && value.attemptCount === 1;
    },
  );
  lastOutbox = firstOutbox;
  const firstBpi = waitFor(
    "BPI quality-gate projection",
    () => bpiState(),
    (value) => value.batch?.state === "RELEASED" && Number(value.inboxCount) === 1,
  );
  assertFirstDelivery(firstOutbox, firstBpi);
  const firstOffsets = {
    topic: waitFor(
      "QCS topic first offset",
      () => kafkaOffsets(topic),
      (value) => offsetTotal(value) === offsetTotal(report.preflight.offsets.topic) + 1,
    ),
    dlq: kafkaOffsets(dlqTopic),
  };
  if (offsetTotal(firstOffsets.dlq) !== offsetTotal(report.preflight.offsets.dlq)) {
    throw new Error("QCS first delivery unexpectedly wrote to the DLQ");
  }
  const firstLag = consumerLag(topic);
  if (firstLag.lag !== 0) throw new Error(`QCS consumer lag after first delivery is ${firstLag.lag}`);
  report.firstDelivery = {
    outbox: firstOutbox,
    bpi: firstBpi,
    kafka: {
      offsets: firstOffsets,
      topicDelta: offsetDelta(report.preflight.offsets.topic, firstOffsets.topic),
      dlqDelta: offsetDelta(report.preflight.offsets.dlq, firstOffsets.dlq),
      consumer: firstLag,
    },
  };

  const replayId = remoteQuery("adp", `
UPDATE public.qcs_bpi_quality_gate_outbox
   SET publication_state='READY', next_attempt_at=now(), claimed_at=NULL, claimed_by=NULL,
       last_error=NULL, resolved_batch_id=NULL, payload_sha256=NULL, sent_at=NULL, updated_at=now()
 WHERE source_order_id='${orderId}' AND publication_state='SENT'
 RETURNING id;
  `);
  const replayIds = String(replayId).split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => /^\d+$/.test(line));
  if (replayIds.length !== 1 || replayIds[0] !== String(firstOutbox.id)) {
    throw new Error(`QCS replay did not requeue the expected outbox row: ${replayId}`);
  }
  const replayOutbox = waitFor(
    "QCS outbox replay publication",
    () => outboxState(),
    (value) => value?.publicationState === "SENT" && value.attemptCount === 2,
  );
  lastOutbox = replayOutbox;
  if (replayOutbox.payloadSha256 !== firstOutbox.payloadSha256
      || replayOutbox.eventId !== firstOutbox.eventId) {
    throw new Error("QCS replay changed the canonical event identity or payload hash");
  }
  const replayOffsets = waitFor(
    "QCS topic replay offset",
    () => kafkaOffsets(topic),
    (value) => offsetTotal(value) === offsetTotal(report.preflight.offsets.topic) + 2,
  );
  const replayBpi = waitFor(
    "BPI replay idempotency",
    () => bpiState(),
    (value) => Number(value.inboxCount) === 1 && value.batch?.state === "RELEASED",
  );
  const beforeProjection = stableProjection(firstBpi);
  const afterProjection = stableProjection(replayBpi);
  if (JSON.stringify(beforeProjection) !== JSON.stringify(afterProjection)) {
    throw new Error(`BPI replay mutated durable state: ${JSON.stringify({ beforeProjection, afterProjection })}`);
  }
  const replayDlq = kafkaOffsets(dlqTopic);
  const replayLag = consumerLag(topic);
  if (offsetTotal(replayDlq) !== offsetTotal(report.preflight.offsets.dlq) || replayLag.lag !== 0) {
    throw new Error(`QCS replay safety failed: dlq=${offsetTotal(replayDlq)}, lag=${replayLag.lag}`);
  }
  report.replay = {
    outbox: replayOutbox,
    beforeProjection,
    afterProjection,
    idempotent: true,
    kafka: {
      offsets: replayOffsets,
      topicDelta: offsetDelta(report.preflight.offsets.topic, replayOffsets),
      dlq: replayDlq,
      consumer: replayLag,
    },
  };
  report.kafka = {
    topic,
    dlqTopic,
    firstDeliveryDelta: 1,
    replayDelta: 1,
    totalDelta: offsetTotal(replayOffsets) - offsetTotal(report.preflight.offsets.topic),
    dlqDelta: offsetTotal(replayDlq) - offsetTotal(report.preflight.offsets.dlq),
    finalLag: replayLag.lag,
  };
  report.status = "PASS_TARGET_QCS_BPI_REPLAY_BEFORE_CLEANUP";
}

try {
  main();
} catch (error) {
  report.error = error?.stack || error?.message || String(error);
  process.exitCode = 1;
} finally {
  const cleanupResult = cleanup();
  if (cleanupResult.errors.length > 0 || cleanupResult.adpResidualRows !== 0
      || cleanupResult.bpiResidualRows !== 0) {
    report.status = "FAIL_CLEANUP";
    process.exitCode = 1;
  } else if (report.status === "PASS_TARGET_QCS_BPI_REPLAY_BEFORE_CLEANUP") {
    report.status = "PASS_TARGET_QCS_BPI_REPLAY_CLEANED";
  }
  report.generatedAt = new Date().toISOString();
  report.runtimeChanged = runtimeChanged;
  fs.writeFileSync(targetReportPath, `${JSON.stringify(report, null, 2)}\n`, "utf8");
  process.stdout.write(`${targetReportPath}\n`);
}
