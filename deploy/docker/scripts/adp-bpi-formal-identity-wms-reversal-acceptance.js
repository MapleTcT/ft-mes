#!/usr/bin/env node
"use strict";

const crypto = require("node:crypto");
const fs = require("node:fs");
const path = require("node:path");
const { spawn, spawnSync } = require("node:child_process");
const printRemoteScript = process.argv.includes("--print-remote-script");
const { chromium, request } = printRemoteScript ? {} : require("playwright");

const repoRoot = path.resolve(__dirname, "../../..");
const stamp = new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14);
const marker = process.env.BPI_FORMAL_IDENTITY_MARKER || `ADP_BPI_FORMAL_WMS_REVERSAL_${stamp}`;
const sshTarget = process.env.ADP_DB_SSH_TARGET || "v6@10.11.100.17";
const remoteRoot = process.env.ADP_REMOTE_DEPLOY_ROOT
  || "/home/v6/adp-mes-docker-newbase-20260611-181921";
const adpBaseUrl = required("ADP_BASE_URL").replace(/\/+$/, "");
const adminUsername = required("ADP_USERNAME");
const adminPassword = required("ADP_PASSWORD");
const tenantId = process.env.BPI_TENANT_ID || "1000";
const plantId = process.env.BPI_PLANT_ID || "PLANT-01";
const lineId = process.env.BPI_LINE_ID || "LINE-S07-01";
const reviewerUsername = process.env.BPI_FORMAL_APPROVER_USERNAME
  || `bpi_reviewer_${stamp}`.toLowerCase();
const reviewerPassword = process.env.BPI_FORMAL_APPROVER_PASSWORD || `Ft@${stamp}Aa1!`;
const personCode = process.env.BPI_FORMAL_APPROVER_PERSON_CODE || `${marker}_PERSON`;
const personName = process.env.BPI_FORMAL_APPROVER_PERSON_NAME || `${marker}_PERSON`;
const mainPositionId = Number(process.env.BPI_FORMAL_APPROVER_POSITION_ID || 1);
const roleCode = process.env.BPI_FORMAL_APPROVER_ROLE_CODE || "systemRole";
const fullRoundTrip = process.env.BPI_FORMAL_IDENTITY_FULL_ROUNDTRIP === "true";
let originalDocumentId = process.env.BPI_ORIGINAL_DOCUMENT_ID || `${marker}_BLUE_DOC`;
const timeoutSeconds = positiveInteger("BPI_FORMAL_IDENTITY_TIMEOUT_SECONDS", 180, 60, 600);
const watchdogSeconds = positiveInteger("BPI_FORMAL_IDENTITY_WATCHDOG_SECONDS", 900, 300, 3600);
const backupDir = process.env.BPI_FORMAL_IDENTITY_BACKUP_DIR
  || `/data/docker/bpi-upgrade-backups/${marker}`;
const reportStem = fullRoundTrip
  ? "bpi-formal-identity-wms-roundtrip"
  : "bpi-formal-identity-wms-reversal";
const outputPath = path.resolve(process.env.BPI_FORMAL_IDENTITY_OUTPUT
  || path.join(repoRoot, `metadata/${reportStem}-acceptance.json`));
const pendingScreenshot = path.resolve(process.env.BPI_FORMAL_IDENTITY_PENDING_SCREENSHOT
  || path.join(repoRoot, `metadata/${reportStem}-pending.png`));
const approvedScreenshot = path.resolve(process.env.BPI_FORMAL_IDENTITY_APPROVED_SCREENSHOT
  || path.join(repoRoot, `metadata/${reportStem}-approved.png`));
const completedScreenshot = path.resolve(process.env.BPI_FORMAL_IDENTITY_COMPLETED_SCREENSHOT
  || path.join(repoRoot, `metadata/${reportStem}-completed.png`));
const nodePath = process.env.NODE_PATH || path.join(repoRoot, "frontend/apps/bpi/node_modules");
const kafkaSuffix = marker.toLowerCase().replace(/_/g, "-");
const kafka = {
  blueCommand: `bpi.acceptance.${kafkaSuffix}.blue-command`,
  blueCommandDlq: `bpi.acceptance.${kafkaSuffix}.blue-command-dlq`,
  blueReceipt: `bpi.acceptance.${kafkaSuffix}.blue-receipt`,
  blueReceiptDlq: `bpi.acceptance.${kafkaSuffix}.blue-receipt-dlq`,
  redCommand: `bpi.acceptance.${kafkaSuffix}.red-command`,
  redCommandDlq: `bpi.acceptance.${kafkaSuffix}.red-command-dlq`,
  redReceipt: `bpi.acceptance.${kafkaSuffix}.red-receipt`,
  redReceiptDlq: `bpi.acceptance.${kafkaSuffix}.red-receipt-dlq`,
  qcs: `bpi.acceptance.${kafkaSuffix}.qcs`,
  qcsDlq: `bpi.acceptance.${kafkaSuffix}.qcs-dlq`,
  wmsGroup: `bpi-acceptance-${kafkaSuffix}-wms`,
  receiptGroup: `bpi-acceptance-${kafkaSuffix}-receipt`,
};

if (!printRemoteScript && process.env.BPI_FORMAL_IDENTITY_CONFIRM
    !== "CREATE_TEMPORARY_ADP_ADMIN_AND_RESTORE") {
  throw new Error(
    "Set BPI_FORMAL_IDENTITY_CONFIRM=CREATE_TEMPORARY_ADP_ADMIN_AND_RESTORE",
  );
}

for (const [label, value, pattern] of [
  ["marker", marker, /^[A-Za-z0-9_-]{8,100}$/],
  ["SSH target", sshTarget, /^[A-Za-z0-9._-]+@[A-Za-z0-9._-]+$/],
  ["remote root", remoteRoot, /^\/[A-Za-z0-9._/-]+$/],
  ["backup directory", backupDir, /^\/[A-Za-z0-9._/-]+$/],
  ["tenant", tenantId, /^[A-Za-z0-9._-]{1,64}$/],
  ["plant", plantId, /^[A-Za-z0-9._-]{1,128}$/],
  ["line", lineId, /^[A-Za-z0-9._-]{1,128}$/],
  ["reviewer username", reviewerUsername, /^[A-Za-z0-9._-]{3,64}$/],
  ["person code", personCode, /^[A-Za-z0-9._-]{3,128}$/],
  ["role code", roleCode, /^[A-Za-z0-9._-]{1,64}$/],
  ["original document ID", originalDocumentId, /^[A-Za-z0-9._-]{8,256}$/],
  ...Object.entries(kafka).map(([key, value]) => [
    `Kafka ${key}`,
    value,
    /^[A-Za-z0-9._-]{1,249}$/,
  ]),
]) {
  if (!pattern.test(value)) throw new Error(`Unsafe ${label}: ${value}`);
}
if (!Number.isInteger(mainPositionId) || mainPositionId <= 0) {
  throw new Error("BPI_FORMAL_APPROVER_POSITION_ID must be a positive integer");
}
if (reviewerUsername === adminUsername) {
  throw new Error("Formal approver username must differ from the requester username");
}

function required(key) {
  const value = String(process.env[key] || "").trim();
  if (!value) throw new Error(`${key} is required`);
  return value;
}

function positiveInteger(key, fallback, minimum, maximum) {
  const value = Number(process.env[key] || fallback);
  if (!Number.isInteger(value) || value < minimum || value > maximum) {
    throw new Error(`${key} must be an integer from ${minimum} to ${maximum}`);
  }
  return value;
}

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

function gitHead() {
  const result = spawnSync("git", ["rev-parse", "HEAD"], {
    cwd: repoRoot,
    encoding: "utf8",
  });
  return result.status === 0 ? result.stdout.trim() : "unknown";
}

function fileSha256(file) {
  return crypto.createHash("sha256").update(fs.readFileSync(file)).digest("hex");
}

function shellQuote(value) {
  return `'${String(value).replace(/'/g, `'"'"'`)}'`;
}

function sqlLiteral(value) {
  return `'${String(value).replace(/'/g, "''")}'`;
}

function parseLastJson(output, label) {
  for (const line of String(output || "").trim().split(/\r?\n/).reverse()) {
    const candidate = line.trim();
    if (!candidate.startsWith("{") || !candidate.endsWith("}")) continue;
    try {
      return JSON.parse(candidate);
    } catch (_error) {
      // Keep looking for the complete PostgreSQL JSON row.
    }
  }
  throw new Error(`${label} did not return a JSON row: ${String(output).slice(-1000)}`);
}

function parseKeyValues(output) {
  const values = {};
  for (const line of String(output || "").split(/\r?\n/)) {
    const separator = line.indexOf("=");
    if (separator > 0) values[line.slice(0, separator)] = line.slice(separator + 1);
  }
  return values;
}

function psql(database, sql, variables = {}) {
  const variableArgs = Object.entries(variables)
    .map(([key, value]) => `-v ${shellQuote(`${key}=${value}`)}`)
    .join(" ");
  const command = [
    "docker exec -i adp-mes-newbase-postgres-1",
    `psql -X -At -U adp -d ${shellQuote(database)} -v ON_ERROR_STOP=1`,
    variableArgs,
  ].filter(Boolean).join(" ");
  const result = spawnSync(
    "ssh",
    ["-o", "BatchMode=yes", "-o", "ConnectTimeout=10", sshTarget, command],
    { input: sql, encoding: "utf8", maxBuffer: 16 * 1024 * 1024 },
  );
  if (result.status !== 0) {
    throw new Error(`PostgreSQL ${database} command failed: ${result.stderr || result.stdout}`);
  }
  return result.stdout.trim();
}

function psqlJson(database, sql, label) {
  return parseLastJson(psql(database, sql), label);
}

function psqlFile(file, variables) {
  return psql("ft_mes_bpi", fs.readFileSync(file, "utf8"), variables);
}

function psqlFileIn(database, file, variables) {
  return psql(database, fs.readFileSync(file, "utf8"), variables);
}

function sleep(milliseconds) {
  return new Promise((resolve) => setTimeout(resolve, milliseconds));
}

async function waitFor(label, probe, predicate) {
  const deadline = Date.now() + timeoutSeconds * 1000;
  let current;
  let lastError;
  while (Date.now() < deadline) {
    try {
      current = await probe();
      if (predicate(current)) return current;
      lastError = null;
    } catch (error) {
      lastError = error;
    }
    await sleep(1000);
  }
  throw new Error(`${label} timed out: ${lastError?.message || JSON.stringify(current)}`);
}

const remoteScript = String.raw`set -eu
action=$1
root=$2
backup_dir=$3
reviewer=$4
tenant_id=$5
plant_id=$6
line_id=$7
timeout=$8
watchdog_timeout=$9
shift 9
full_roundtrip=$1; shift
blue_command=$1; shift
blue_command_dlq=$1; shift
blue_receipt=$1; shift
blue_receipt_dlq=$1; shift
red_command=$1; shift
red_command_dlq=$1; shift
red_receipt=$1; shift
red_receipt_dlq=$1; shift
qcs_topic=$1; shift
qcs_dlq=$1; shift
wms_group=$1; shift
receipt_group=$1

kafka_container=ft-mes-bpi-streaming-kafka-1-1
kafka_bootstrap=kafka-1:9092

runtime_dir="$root/deploy/docker"
compose_file="$runtime_dir/docker-compose.yml"
base_env="$runtime_dir/.env"
acceptance_env="$backup_dir/formal-identity.env"
acceptance_compose="$backup_dir/formal-identity.compose.yml"
complete_file="$backup_dir/COMPLETE"

kafka_topics() {
  printf '%s\n' \
    "$blue_command" "$blue_command_dlq" "$blue_receipt" "$blue_receipt_dlq" \
    "$red_command" "$red_command_dlq" "$red_receipt" "$red_receipt_dlq" \
    "$qcs_topic" "$qcs_dlq"
}

kafka_topic_exists() {
  docker exec "$kafka_container" /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server "$kafka_bootstrap" --list | grep -Fqx "$1"
}

kafka_topic_offset() {
  docker exec "$kafka_container" /opt/kafka/bin/kafka-get-offsets.sh \
    --bootstrap-server "$kafka_bootstrap" --topic "$1" \
    | awk -F: '{ total += $3 } END { print total + 0 }'
}

kafka_group_lag() {
  docker exec "$kafka_container" /opt/kafka/bin/kafka-consumer-groups.sh \
    --bootstrap-server "$kafka_bootstrap" --describe --group "$1" 2>/dev/null \
    | awk -v group="$1" '$1 == group && $6 ~ /^[0-9]+$/ { total += $6 } END { print total + 0 }'
}

dc() {
  selected_env=$1
  shift
  if [ "$selected_env" = "$acceptance_env" ] && [ -f "$acceptance_compose" ]; then
    docker compose --env-file "$selected_env" -f "$compose_file" -f "$acceptance_compose" --profile bpi "$@"
  else
    docker compose --env-file "$selected_env" -f "$compose_file" --profile bpi "$@"
  fi
}

health() {
  selected_env=$1
  if [ "$#" -ge 2 ]; then service=$2; else service=bpi-adapter; fi
  id=$(dc "$selected_env" ps -q "$service")
  if [ -z "$id" ]; then printf missing; return; fi
  docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$id"
}

wait_healthy() {
  selected_env=$1
  if [ "$#" -ge 2 ]; then service=$2; else service=bpi-adapter; fi
  deadline=$(( $(date +%s) + timeout ))
  while [ "$(date +%s)" -lt "$deadline" ]; do
    state=$(health "$selected_env" "$service")
    case "$state" in
      healthy) return 0 ;;
      exited|dead|missing) dc "$selected_env" logs --tail 120 "$service" >&2 || true; return 1 ;;
    esac
    sleep 2
  done
  dc "$selected_env" logs --tail 120 "$service" >&2 || true
  return 1
}

adapter_state() {
  selected_env=$1
  id=$(dc "$selected_env" ps -q bpi-adapter)
  printf 'adapterImage=%s\n' "$(docker inspect -f '{{.Config.Image}}' "$id")"
  printf 'adapterImageId=%s\n' "$(docker inspect -f '{{.Image}}' "$id")"
  printf 'adapterHealth=%s\n' "$(health "$selected_env")"
  printf 'adapterSubjectScopeRules=%s\n' "$(docker inspect -f '{{range .Config.Env}}{{println .}}{{end}}' "$id" | sed -n 's/^BPI_ADAPTER_SUBJECT_SCOPE_RULES=//p' | tail -1)"
  service_id=$(dc "$selected_env" ps -q bpi-service)
  printf 'serviceImage=%s\n' "$(docker inspect -f '{{.Config.Image}}' "$service_id")"
  printf 'serviceImageId=%s\n' "$(docker inspect -f '{{.Image}}' "$service_id")"
  printf 'serviceHealth=%s\n' "$(health "$selected_env" bpi-service)"
  wms_id=$(dc "$selected_env" ps -q bpi-wms-adapter)
  if [ -n "$wms_id" ]; then
    printf 'wmsAdapterImage=%s\n' "$(docker inspect -f '{{.Config.Image}}' "$wms_id")"
    printf 'wmsAdapterImageId=%s\n' "$(docker inspect -f '{{.Image}}' "$wms_id")"
    printf 'wmsAdapterHealth=%s\n' "$(health "$selected_env" bpi-wms-adapter)"
    api_key=$(docker inspect -f '{{range .Config.Env}}{{println .}}{{end}}' "$wms_id" | sed -n 's/^BPI_WMS_ADAPTER_MATERIAL_API_KEY=//p' | tail -1)
    if [ -n "$api_key" ] && [ "$api_key" != "_DISABLED_" ]; then
      printf 'materialApiKeyConfigured=true\n'
    else
      printf 'materialApiKeyConfigured=false\n'
    fi
  fi
}

phase2_state() {
  service_id=$(dc "$base_env" ps -q bpi-service)
  qcs_id=$(dc "$base_env" ps -q qcs-quality-gate-outbox)
  wms_id=$(dc "$base_env" ps -q bpi-wms-adapter)
  for spec in \
    "$service_id:BPI_PHASE2_INTEGRATION_ENABLED" \
    "$service_id:BPI_PHASE2_PROTOBUF_HTTP_INGRESS_ENABLED" \
    "$service_id:BPI_PHASE2_KAFKA_ENABLED" \
    "$service_id:BPI_WMS_OUTBOX_ENABLED" \
    "$wms_id:BPI_WMS_ADAPTER_ENABLED" \
    "$qcs_id:QCS_BPI_OUTBOX_ENABLED"; do
    container=$(printf '%s' "$spec" | cut -d: -f1)
    key=$(printf '%s' "$spec" | cut -d: -f2-)
    value=false
    if [ -n "$container" ]; then
      configured=$(docker inspect -f '{{range .Config.Env}}{{println .}}{{end}}' "$container" | sed -n "s/^$key=//p" | tail -1)
      if [ -n "$configured" ]; then value=$configured; fi
    fi
    printf '%s:%s,' "$key" "$value"
  done
  printf '\n'
}

write_watchdog() {
  cat > "$backup_dir/watchdog.sh" <<'SH'
#!/bin/sh
set -eu
root=$1
backup_dir=$2
delay=$3
full_roundtrip=$4
sleep "$delay"
if [ -f "$backup_dir/COMPLETE" ]; then exit 0; fi
runtime_dir="$root/deploy/docker"
services="bpi-service bpi-adapter"
if [ "$full_roundtrip" = "true" ]; then services="$services bpi-wms-adapter"; fi
docker compose --env-file "$runtime_dir/.env" -f "$runtime_dir/docker-compose.yml" --profile bpi \
  up -d --no-deps --force-recreate --no-build $services
date -u +%FT%TZ > "$backup_dir/WATCHDOG_RESTORED"
SH
  chmod 700 "$backup_dir/watchdog.sh"
  nohup sh "$backup_dir/watchdog.sh" "$root" "$backup_dir" "$watchdog_timeout" "$full_roundtrip" \
    >"$backup_dir/watchdog.log" 2>&1 </dev/null &
  printf '%s\n' "$!" > "$backup_dir/watchdog.pid"
}

case "$action" in
  precheck)
    test -f "$base_env"
    wait_healthy "$base_env"
    wait_healthy "$base_env" bpi-service
    if [ "$full_roundtrip" = "true" ]; then
      wait_healthy "$base_env" bpi-wms-adapter
      test "$(docker inspect -f '{{.State.Running}}' "$kafka_container")" = "true"
      for topic in $(kafka_topics); do
        if kafka_topic_exists "$topic"; then
          printf 'isolated Kafka topic already exists before activation: %s\n' "$topic" >&2
          exit 5
        fi
      done
    fi
    adapter_state "$base_env"
    printf 'phase2State=%s\n' "$(phase2_state)"
    ;;
  activate)
    test ! -e "$backup_dir"
    mkdir -p "$backup_dir"
    chmod 700 "$backup_dir"
    cp "$base_env" "$backup_dir/runtime.env.before"
    cp "$base_env" "$acceptance_env"
    chmod 600 "$backup_dir/runtime.env.before" "$acceptance_env"
    current_scope=$(dc "$base_env" ps -q bpi-adapter | xargs docker inspect -f '{{range .Config.Env}}{{println .}}{{end}}' | sed -n 's/^BPI_ADAPTER_SUBJECT_SCOPE_RULES=//p' | tail -1)
    test -n "$current_scope"
    case ";$current_scope;" in *";$reviewer="*) printf 'reviewer scope already exists\n' >&2; exit 4 ;; esac
    updated_scope="$current_scope;$reviewer=$tenant_id|$plant_id|$line_id"
    if [ "$full_roundtrip" = "true" ]; then
      test "$(docker inspect -f '{{.State.Running}}' "$kafka_container")" = "true"
      for topic in $(kafka_topics); do
        if kafka_topic_exists "$topic"; then
          printf 'isolated Kafka topic already exists: %s\n' "$topic" >&2
          exit 5
        fi
      done
      for topic in $(kafka_topics); do
        docker exec "$kafka_container" /opt/kafka/bin/kafka-topics.sh \
          --bootstrap-server "$kafka_bootstrap" --create --topic "$topic" \
          --partitions 1 --replication-factor 1 \
          --config cleanup.policy=delete --config retention.ms=3600000 >/dev/null
      done
    fi
    python3 - "$acceptance_compose" "$updated_scope" "$full_roundtrip" \
      "$blue_command" "$blue_command_dlq" "$blue_receipt" "$blue_receipt_dlq" \
      "$red_command" "$red_command_dlq" "$red_receipt" "$red_receipt_dlq" \
      "$qcs_topic" "$qcs_dlq" "$wms_group" "$receipt_group" \
      "$tenant_id" "$plant_id" "$line_id" <<'PY'
import os
import sys
from pathlib import Path

path = Path(sys.argv[1])
value = sys.argv[2]
full_roundtrip = sys.argv[3] == "true"
(
    blue_command,
    blue_command_dlq,
    blue_receipt,
    blue_receipt_dlq,
    red_command,
    red_command_dlq,
    red_receipt,
    red_receipt_dlq,
    qcs_topic,
    qcs_dlq,
    wms_group,
    receipt_group,
    tenant_id,
    plant_id,
    line_id,
) = sys.argv[4:]
escaped = value.replace("\\", "\\\\").replace('"', '\\"')
content = (
    "services:\n"
    "  bpi-service:\n"
    "    environment:\n"
    "      BPI_PHASE2_INTEGRATION_ENABLED: \"true\"\n"
    "      BPI_PHASE2_PROTOBUF_HTTP_INGRESS_ENABLED: \"false\"\n"
    f'      BPI_PHASE2_KAFKA_ENABLED: "{str(full_roundtrip).lower()}"\n'
    f'      BPI_WMS_OUTBOX_ENABLED: "{str(full_roundtrip).lower()}"\n'
    "  bpi-adapter:\n"
    "    environment:\n"
    f'      BPI_ADAPTER_SUBJECT_SCOPE_RULES: "{escaped}"\n'
)
if full_roundtrip:
    content = content.replace(
        "  bpi-adapter:\n",
        (
            f'      BPI_PHASE2_KAFKA_GROUP_ID: "{receipt_group}"\n'
            f'      BPI_PHASE2_QCS_TOPIC: "{qcs_topic}"\n'
            f'      BPI_PHASE2_QCS_DLQ_TOPIC: "{qcs_dlq}"\n'
            f'      BPI_PHASE2_WMS_RECEIPT_TOPIC: "{blue_receipt}"\n'
            f'      BPI_PHASE2_WMS_RECEIPT_DLQ_TOPIC: "{blue_receipt_dlq}"\n'
            f'      BPI_PHASE2_WMS_REVERSAL_RECEIPT_TOPIC: "{red_receipt}"\n'
            f'      BPI_PHASE2_WMS_REVERSAL_RECEIPT_DLQ_TOPIC: "{red_receipt_dlq}"\n'
            f'      BPI_PHASE2_ALLOWED_TENANT_IDS: "{tenant_id}"\n'
            f'      BPI_PHASE2_ALLOWED_PLANT_IDS: "{plant_id}"\n'
            f'      BPI_PHASE2_ALLOWED_LINE_IDS: "{line_id}"\n'
            f'      BPI_WMS_OUTBOX_TOPIC: "{blue_command}"\n'
            f'      BPI_WMS_REVERSAL_OUTBOX_TOPIC: "{red_command}"\n'
            '      BPI_WMS_RECONCILIATION_DELAY: "2s"\n'
            "  bpi-adapter:\n"
        ),
    )
    content += (
        "  bpi-wms-adapter:\n"
        "    environment:\n"
        '      BPI_WMS_ADAPTER_ENABLED: "true"\n'
        f'      BPI_WMS_ADAPTER_COMMAND_TOPIC: "{blue_command}"\n'
        f'      BPI_WMS_ADAPTER_COMMAND_DLQ_TOPIC: "{blue_command_dlq}"\n'
        f'      BPI_WMS_ADAPTER_RECEIPT_TOPIC: "{blue_receipt}"\n'
        f'      BPI_WMS_ADAPTER_REVERSAL_COMMAND_TOPIC: "{red_command}"\n'
        f'      BPI_WMS_ADAPTER_REVERSAL_COMMAND_DLQ_TOPIC: "{red_command_dlq}"\n'
        f'      BPI_WMS_ADAPTER_REVERSAL_RECEIPT_TOPIC: "{red_receipt}"\n'
        f'      BPI_WMS_ADAPTER_GROUP_ID: "{wms_group}"\n'
        f'      BPI_WMS_ADAPTER_CLIENT_ID: "{wms_group}"\n'
        f'      BPI_WMS_ADAPTER_ROUTES: "{tenant_id}|{plant_id}|{line_id}|WARE-E2E|LOC-E2E|COMP-E2E|kg"\n'
        '      BPI_WMS_ADAPTER_MAX_ATTEMPTS: "4"\n'
        '      BPI_WMS_ADAPTER_RETRY_BACKOFF: "1s"\n'
        '      BPI_WMS_ADAPTER_REQUEST_TIMEOUT: "5s"\n'
    )
path.write_text(content, encoding="utf-8")
os.chmod(path, 0o600)
PY
    before_adapter_id=$(dc "$base_env" ps -q bpi-adapter | xargs docker inspect -f '{{.Image}}')
    before_service_id=$(dc "$base_env" ps -q bpi-service | xargs docker inspect -f '{{.Image}}')
    before_wms_id=$(dc "$base_env" ps -q bpi-wms-adapter | xargs docker inspect -f '{{.Image}}')
    printf '%s\n' "$before_adapter_id" > "$backup_dir/adapter-image-id.before"
    printf '%s\n' "$before_service_id" > "$backup_dir/service-image-id.before"
    printf '%s\n' "$before_wms_id" > "$backup_dir/wms-adapter-image-id.before"
    write_watchdog
    services="bpi-service bpi-adapter"
    if [ "$full_roundtrip" = "true" ]; then services="$services bpi-wms-adapter"; fi
    dc "$acceptance_env" up -d --no-deps --force-recreate --no-build $services
    wait_healthy "$acceptance_env"
    wait_healthy "$acceptance_env" bpi-service
    if [ "$full_roundtrip" = "true" ]; then wait_healthy "$acceptance_env" bpi-wms-adapter; fi
    after_adapter_id=$(dc "$acceptance_env" ps -q bpi-adapter | xargs docker inspect -f '{{.Image}}')
    after_service_id=$(dc "$acceptance_env" ps -q bpi-service | xargs docker inspect -f '{{.Image}}')
    after_wms_id=$(dc "$acceptance_env" ps -q bpi-wms-adapter | xargs docker inspect -f '{{.Image}}')
    test "$before_adapter_id" = "$after_adapter_id"
    test "$before_service_id" = "$after_service_id"
    test "$before_wms_id" = "$after_wms_id"
    adapter_state "$acceptance_env"
    printf 'phase2State=%s\n' "$(phase2_state)"
    printf 'watchdogPid=%s\n' "$(cat "$backup_dir/watchdog.pid")"
    ;;
  restore)
    services="bpi-service bpi-adapter"
    if [ "$full_roundtrip" = "true" ]; then services="$services bpi-wms-adapter"; fi
    dc "$base_env" up -d --no-deps --force-recreate --no-build $services
    wait_healthy "$base_env"
    wait_healthy "$base_env" bpi-service
    if [ "$full_roundtrip" = "true" ]; then wait_healthy "$base_env" bpi-wms-adapter; fi
    if [ -f "$backup_dir/adapter-image-id.before" ]; then
      current_id=$(dc "$base_env" ps -q bpi-adapter | xargs docker inspect -f '{{.Image}}')
      test "$current_id" = "$(cat "$backup_dir/adapter-image-id.before")"
    fi
    if [ -f "$backup_dir/service-image-id.before" ]; then
      current_service_id=$(dc "$base_env" ps -q bpi-service | xargs docker inspect -f '{{.Image}}')
      test "$current_service_id" = "$(cat "$backup_dir/service-image-id.before")"
    fi
    if [ -f "$backup_dir/wms-adapter-image-id.before" ]; then
      current_wms_id=$(dc "$base_env" ps -q bpi-wms-adapter | xargs docker inspect -f '{{.Image}}')
      test "$current_wms_id" = "$(cat "$backup_dir/wms-adapter-image-id.before")"
    fi
    adapter_state "$base_env"
    printf 'phase2State=%s\n' "$(phase2_state)"
    ;;
  stop-wms)
    test "$full_roundtrip" = "true"
    dc "$acceptance_env" stop bpi-wms-adapter >/dev/null
    id=$(dc "$acceptance_env" ps -a -q bpi-wms-adapter)
    test -n "$id"
    state=$(docker inspect -f '{{.State.Status}}' "$id")
    test "$state" = "exited"
    printf 'wmsAdapterHealth=%s\n' "$state"
    ;;
  start-wms)
    test "$full_roundtrip" = "true"
    dc "$acceptance_env" start bpi-wms-adapter >/dev/null
    wait_healthy "$acceptance_env" bpi-wms-adapter
    adapter_state "$acceptance_env"
    ;;
  kafka-state)
    test "$full_roundtrip" = "true"
    printf 'blueCommand=%s\n' "$(kafka_topic_offset "$blue_command")"
    printf 'blueCommandDlq=%s\n' "$(kafka_topic_offset "$blue_command_dlq")"
    printf 'blueReceipt=%s\n' "$(kafka_topic_offset "$blue_receipt")"
    printf 'blueReceiptDlq=%s\n' "$(kafka_topic_offset "$blue_receipt_dlq")"
    printf 'redCommand=%s\n' "$(kafka_topic_offset "$red_command")"
    printf 'redCommandDlq=%s\n' "$(kafka_topic_offset "$red_command_dlq")"
    printf 'redReceipt=%s\n' "$(kafka_topic_offset "$red_receipt")"
    printf 'redReceiptDlq=%s\n' "$(kafka_topic_offset "$red_receipt_dlq")"
    printf 'qcs=%s\n' "$(kafka_topic_offset "$qcs_topic")"
    printf 'qcsDlq=%s\n' "$(kafka_topic_offset "$qcs_dlq")"
    printf 'wmsLag=%s\n' "$(kafka_group_lag "$wms_group")"
    printf 'receiptLag=%s\n' "$(kafka_group_lag "$receipt_group")"
    ;;
  complete)
    if [ ! -d "$backup_dir" ]; then
      printf 'watchdogRestored=false\n'
      exit 0
    fi
    touch "$complete_file"
    if [ -f "$backup_dir/watchdog.pid" ]; then
      kill "$(cat "$backup_dir/watchdog.pid")" 2>/dev/null || true
    fi
    if [ "$full_roundtrip" = "true" ]; then
      docker exec "$kafka_container" /opt/kafka/bin/kafka-consumer-groups.sh \
        --bootstrap-server "$kafka_bootstrap" --delete \
        --group "$wms_group" --group "$receipt_group" >/dev/null 2>&1 || true
      for topic in $(kafka_topics); do
        docker exec "$kafka_container" /opt/kafka/bin/kafka-topics.sh \
          --bootstrap-server "$kafka_bootstrap" --delete --topic "$topic" >/dev/null
      done
      deadline=$(( $(date +%s) + 30 ))
      for topic in $(kafka_topics); do
        while kafka_topic_exists "$topic" && [ "$(date +%s)" -lt "$deadline" ]; do sleep 1; done
        if kafka_topic_exists "$topic"; then printf 'Kafka topic cleanup failed: %s\n' "$topic" >&2; exit 6; fi
      done
      printf 'isolatedKafkaCleaned=true\n'
    fi
    printf 'watchdogRestored=%s\n' "$(if [ -f "$backup_dir/WATCHDOG_RESTORED" ]; then printf true; else printf false; fi)"
    ;;
  *) printf 'unsupported action: %s\n' "$action" >&2; exit 2 ;;
esac
`;

function remoteArgs(action) {
  return [
    action,
    remoteRoot,
    backupDir,
    reviewerUsername,
    tenantId,
    plantId,
    lineId,
    String(timeoutSeconds),
    String(watchdogSeconds),
    String(fullRoundTrip),
    kafka.blueCommand,
    kafka.blueCommandDlq,
    kafka.blueReceipt,
    kafka.blueReceiptDlq,
    kafka.redCommand,
    kafka.redCommandDlq,
    kafka.redReceipt,
    kafka.redReceiptDlq,
    kafka.qcs,
    kafka.qcsDlq,
    kafka.wmsGroup,
    kafka.receiptGroup,
  ];
}

function runRemote(action) {
  const result = spawnSync(
    "ssh",
    ["-o", "BatchMode=yes", "-o", "ConnectTimeout=10", sshTarget,
      "sh", "-s", "--", ...remoteArgs(action)],
    { input: remoteScript, encoding: "utf8", maxBuffer: 16 * 1024 * 1024 },
  );
  if (result.status !== 0) {
    throw new Error(`Remote ${action} failed: ${result.stderr || result.stdout}`);
  }
  return parseKeyValues(result.stdout);
}

function findTicket(payload) {
  const candidates = [
    payload?.ticket,
    payload?.access_token,
    payload?.token,
    payload?.data?.ticket,
    payload?.data?.access_token,
    payload?.data?.token,
    payload?.result?.ticket,
    payload?.result?.access_token,
    payload?.result?.token,
  ];
  return candidates.find((value) => typeof value === "string" && value.length > 20);
}

async function readBody(response) {
  const text = await response.text();
  try {
    return { text, json: JSON.parse(text) };
  } catch (_error) {
    return { text, json: null };
  }
}

async function login(api, username, password, label) {
  const failures = [];
  for (const data of [
    { userName: username, password, clientId: "pc_dt" },
    { username, password, clientId: "pc_dt" },
  ]) {
    const response = await api.post(`${adpBaseUrl}/inter-api/auth/login`, {
      data,
      headers: { Accept: "application/json", "Content-Type": "application/json;charset=UTF-8" },
      timeout: timeoutSeconds * 1000,
    });
    const body = await readBody(response);
    const ticket = response.ok() ? findTicket(body.json) : null;
    if (ticket) return { ticket, status: response.status(), payload: body.json };
    failures.push({ status: response.status(), body: body.text.slice(0, 300) });
  }
  throw new Error(`ADP login failed for ${label}: ${JSON.stringify(failures)}`);
}

async function retryLogin(api, username, password, label) {
  const deadline = Date.now() + timeoutSeconds * 1000;
  let lastError;
  while (Date.now() < deadline) {
    try {
      return await login(api, username, password, label);
    } catch (error) {
      lastError = error;
      await new Promise((resolve) => setTimeout(resolve, 1500));
    }
  }
  throw lastError;
}

async function browserApi(page, method, urlPath, payload) {
  return page.evaluate(async ({ methodArg, urlPathArg, payloadArg }) => {
    const token = window.localStorage.getItem("suposTicket")
      || window.localStorage.getItem("SUPOS_TICKET")
      || window.localStorage.getItem("token")
      || "";
    const response = await window.fetch(urlPathArg, {
      method: methodArg,
      credentials: "include",
      headers: {
        Accept: "application/json, text/plain, */*",
        Authorization: token ? `Bearer ${token}` : "",
        "Content-Type": "application/json;charset=UTF-8",
        langu_code: "zh_CN",
      },
      body: payloadArg === undefined ? undefined : JSON.stringify(payloadArg),
    });
    const text = await response.text();
    let json = null;
    try { json = JSON.parse(text); } catch (_error) { json = null; }
    return { ok: response.ok, status: response.status, text, json };
  }, { methodArg: method, urlPathArg: urlPath, payloadArg: payload });
}

function ensureApiOk(result, label) {
  const visibleFailure = /(数据库操作异常|系统错误|SQLGrammarException|\bException\b)/i.test(result.text);
  if (!result.ok || visibleFailure) {
    throw new Error(`${label} failed (${result.status}): ${result.text.slice(0, 800)}`);
  }
}

async function adminBrowser() {
  const api = await request.newContext({ ignoreHTTPSErrors: true });
  const auth = await login(api, adminUsername, adminPassword, "requester administrator");
  await api.dispose();
  const browser = await chromium.launch({ headless: true, args: ["--no-proxy-server"] });
  const context = await browser.newContext({
    ignoreHTTPSErrors: true,
    viewport: { width: 1600, height: 1000 },
    extraHTTPHeaders: { Authorization: `Bearer ${auth.ticket}` },
  });
  await context.addCookies([
    { name: "suposTicket", value: auth.ticket, url: adpBaseUrl },
    { name: "SUPOS_TICKET", value: auth.ticket, url: adpBaseUrl },
  ]);
  await context.addInitScript((ticket) => {
    window.localStorage.clear();
    window.sessionStorage.clear();
    for (const key of ["suposTicket", "SUPOS_TICKET", "token"]) {
      window.localStorage.setItem(key, ticket);
      window.sessionStorage.setItem(key, ticket);
    }
  }, auth.ticket);
  const page = await context.newPage();
  await page.goto(`${adpBaseUrl}/auth/#/user`, {
    waitUntil: "networkidle",
    timeout: timeoutSeconds * 1000,
  });
  return { browser, context, page, auth };
}

function identityState() {
  const sql = `
SELECT json_build_object(
  'person', (SELECT json_build_object('id', id, 'valid', valid, 'userId', user_id, 'userName', user_name)
               FROM public.org_person WHERE code = ${sqlLiteral(personCode)} ORDER BY id DESC LIMIT 1),
  'user', (SELECT json_build_object('id', id, 'valid', valid, 'userName', user_name,
                                    'personId', person_id, 'personCode', person_code,
                                    'passwordEncoded', password IS NOT NULL AND password <> '')
             FROM public.auth_user WHERE user_name = ${sqlLiteral(reviewerUsername)} ORDER BY id DESC LIMIT 1),
  'roleUser', (SELECT json_build_object('id', ru.id, 'valid', ru.valid, 'roleCode', r.code,
                                        'userId', ru.user_id, 'userName', ru.user_name)
                 FROM public.rbac_roleuser ru JOIN public.rbac_role r ON r.id = ru.role_id
                WHERE ru.user_name = ${sqlLiteral(reviewerUsername)} AND r.code = ${sqlLiteral(roleCode)}
                ORDER BY ru.id DESC LIMIT 1),
  'authRoles', COALESCE((SELECT json_agg(json_build_object(
                                      'roleCode', COALESCE(aur.role_code, r.code),
                                      'roleName', COALESCE(aur.role_name, r.name)))
                           FROM public.auth_user_role aur
                           JOIN public.auth_user u ON u.id = aur.user_id
                           JOIN public.rbac_role r ON r.id = aur.role_id
                          WHERE u.user_name = ${sqlLiteral(reviewerUsername)}), '[]'::json)
);`;
  return psqlJson("adp", sql, "identity state");
}

function identityPrecheck() {
  const sql = `
SELECT json_build_object(
  'roleId', (SELECT id FROM public.rbac_role WHERE code = ${sqlLiteral(roleCode)} AND valid = true),
  'positionId', (SELECT id FROM public.org_position WHERE id = ${mainPositionId} AND valid = 1),
  'activeUsers', (SELECT count(*) FROM public.auth_user WHERE user_name = ${sqlLiteral(reviewerUsername)} AND valid = 1),
  'activePersons', (SELECT count(*) FROM public.org_person WHERE code = ${sqlLiteral(personCode)} AND valid = 1)
);`;
  return psqlJson("adp", sql, "identity precheck");
}

function bpiMarkerState() {
  assert(fixture, "WMS reversal fixture identity is not initialized");
  const ids = fixture.ids;
  const sql = `
SELECT json_build_object(
  'batchRows', (SELECT count(*) FROM bpi.bpi_batch_instances WHERE id = ${sqlLiteral(ids.batchId)}::uuid),
  'taskRows', (SELECT count(*) FROM bpi.bpi_wms_inbound_reversal_tasks WHERE batch_id = ${sqlLiteral(ids.batchId)}::uuid),
  'qualityGateRows', (SELECT count(*) FROM bpi.bpi_quality_gates WHERE id = ${sqlLiteral(ids.qualityGateId)}::uuid),
  'qualityLinkRows', (SELECT count(*) FROM bpi.bpi_quality_links WHERE id = ${sqlLiteral(ids.qualityLinkId)}::uuid),
  'wmsLinkRows', (SELECT count(*) FROM bpi.bpi_wms_inbound_links WHERE id = ${sqlLiteral(ids.wmsLinkId)}::uuid),
  'outboxRows', (SELECT count(*) FROM bpi.bpi_outbox_events WHERE aggregate_id = ${sqlLiteral(ids.batchId)}::uuid),
  'stateEventRows', (SELECT count(*) FROM bpi.bpi_batch_state_events WHERE batch_id = ${sqlLiteral(ids.batchId)}::uuid),
  'auditRows', (SELECT count(*) FROM bpi.bpi_audit_events WHERE object_id = ${sqlLiteral(ids.batchId)}::uuid),
  'idempotencyRows', (SELECT count(*) FROM bpi.bpi_api_idempotency
                       WHERE resource_path = '/bpi/v1/batches/${ids.batchId}/wms/reversal'),
  'inboxRows', (SELECT count(*) FROM bpi.bpi_inbox_events
                 WHERE idempotency_key LIKE 'WMS_COMPLETION_INBOUND_REVERSAL|1000|${ids.batchId}|%'
                    OR (source = 'wms.completion-inbound.receipt.v1'
                        AND idempotency_key = ${sqlLiteral(marker + "|WMS|1")})),
  'commandsFlagRows', (SELECT count(*) FROM bpi.bpi_feature_flags WHERE id = ${sqlLiteral(ids.commandsFlagId)}::uuid),
  'wmsFlagRows', (SELECT count(*) FROM bpi.bpi_feature_flags WHERE id = ${sqlLiteral(ids.wmsFlagId)}::uuid),
  'plantCommandsOverrides', (SELECT count(*) FROM bpi.bpi_feature_flags
                              WHERE tenant_id = ${sqlLiteral(tenantId)} AND scope_type = 'PLANT'
                                AND scope_key = ${sqlLiteral(plantId)} AND flag_key = 'bpi.commands'),
  'plantWmsOverrides', (SELECT count(*) FROM bpi.bpi_feature_flags
                         WHERE tenant_id = ${sqlLiteral(tenantId)} AND scope_type = 'PLANT'
                           AND scope_key = ${sqlLiteral(plantId)} AND flag_key = 'bpi.wms-link')
);`;
  return psqlJson("ft_mes_bpi", sql, "BPI marker state");
}

function materialSchemaState() {
  const sql = `
SELECT json_build_object(
  'reversalColumn', EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = 'public' AND table_name = 'wms_stock_documents'
       AND column_name = 'reversal_of_document_id' AND data_type = 'bigint'
  ),
  'documentConstraint', COALESCE((
    SELECT pg_get_constraintdef(oid) LIKE '%COMPLETION_INBOUND_REVERSAL%'
      FROM pg_constraint WHERE conrelid = 'public.wms_stock_documents'::regclass
       AND conname = 'ck_wms_stock_documents_type'
  ), false),
  'transactionConstraint', COALESCE((
    SELECT pg_get_constraintdef(oid) LIKE '%COMPLETION_INBOUND_REVERSAL%'
      FROM pg_constraint WHERE conrelid = 'public.wms_inventory_transactions'::regclass
       AND conname = 'ck_wms_inventory_transactions_type'
  ), false),
  'foreignKey', EXISTS (
    SELECT 1 FROM pg_constraint
     WHERE conrelid = 'public.wms_stock_documents'::regclass
       AND conname = 'fk_wms_stock_documents_reversal_original' AND contype = 'f'
  ),
  'uniqueIndex', to_regclass('public.uk_wms_stock_documents_reversal_original') IS NOT NULL
);`;
  return psqlJson("adp", sql, "material-wms reversal schema state");
}

function materialMarkerState() {
  const sql = `
SELECT json_build_object(
  'documentRows', (SELECT count(*) FROM wms_stock_documents document
                    WHERE document.tenant_id = ${sqlLiteral(tenantId)}
                      AND document.source_system = 'BPI'
                      AND EXISTS (SELECT 1 FROM wms_stock_document_lines line
                                   WHERE line.document_id = document.id
                                     AND line.batch_no = ${sqlLiteral(marker)})),
  'lineRows', (SELECT count(*) FROM wms_stock_document_lines
                WHERE tenant_id = ${sqlLiteral(tenantId)}
                  AND batch_no = ${sqlLiteral(marker)}),
  'transactionRows', (SELECT count(*) FROM wms_inventory_transactions
                       WHERE tenant_id = ${sqlLiteral(tenantId)}
                         AND batch_no = ${sqlLiteral(marker)}),
  'stockRows', (SELECT count(*) FROM wms_batch_stocks
                 WHERE tenant_id = ${sqlLiteral(tenantId)}
                   AND batch_no = ${sqlLiteral(marker)})
);`;
  return psqlJson("adp", sql, "material-wms marker state");
}

function blueRoundTripState() {
  const ids = fixture.ids;
  const bpi = psqlJson("ft_mes_bpi", `
SELECT json_build_object(
  'batchState', batch.state, 'batchRevision', batch.revision,
  'wmsStatus', batch.wms_status, 'linkStatus', link.status,
  'linkRevision', link.revision, 'receiptEventId', link.receipt_event_id,
  'documentId', link.document_id, 'outboxStatus', event.status,
  'outboxRevision', event.revision, 'topic', event.topic,
  'payloadSha256', encode(sha256(event.payload), 'hex')
)
  FROM bpi.bpi_batch_instances batch
  JOIN bpi.bpi_wms_inbound_links link
    ON link.tenant_id = batch.tenant_id AND link.batch_id = batch.id
  JOIN bpi.bpi_outbox_events event
    ON event.tenant_id = link.tenant_id AND event.id = link.command_event_id
 WHERE batch.tenant_id = ${sqlLiteral(tenantId)}
   AND batch.id = ${sqlLiteral(ids.batchId)}::uuid;`, "blue BPI roundtrip state");
  const material = psqlJson("adp", `
SELECT json_build_object(
  'documents', count(*),
  'documentNo', max(document.document_no),
  'status', max(document.status),
  'idempotencyKey', max(document.idempotency_key),
  'lineRows', (SELECT count(*) FROM wms_stock_document_lines line
                WHERE line.tenant_id = ${sqlLiteral(tenantId)}
                  AND line.source_system = 'BPI'
                  AND line.source_line_id = ${sqlLiteral(ids.commandEventId + ":1")})
)
  FROM wms_stock_documents document
 WHERE document.tenant_id = ${sqlLiteral(tenantId)}
   AND document.document_type = 'COMPLETION_INBOUND'
   AND document.source_system = 'BPI'
   AND document.source_document_id = ${sqlLiteral(ids.commandEventId)};`,
  "blue material-wms roundtrip state");
  return { bpi, material };
}

function reversalRoundTripState() {
  const ids = fixture.ids;
  return psqlJson("ft_mes_bpi", `
SELECT json_build_object(
  'batchState', batch.state, 'batchRevision', batch.revision,
  'wmsStatus', batch.wms_status, 'taskState', task.state,
  'taskRevision', task.revision,
  'redCommandEventId', task.reversal_command_event_id,
  'redIdempotencyKey', task.reversal_idempotency_key,
  'redReceiptEventId', task.reversal_receipt_event_id,
  'redDocumentId', task.reversal_document_id,
  'errorCode', task.error_code,
  'redOutboxStatus', event.status,
  'redOutboxTopic', event.topic
)
  FROM bpi.bpi_batch_instances batch
  JOIN bpi.bpi_wms_inbound_reversal_tasks task
    ON task.tenant_id = batch.tenant_id AND task.batch_id = batch.id
  JOIN bpi.bpi_outbox_events event
    ON event.tenant_id = task.tenant_id AND event.id = task.reversal_command_event_id
 WHERE batch.tenant_id = ${sqlLiteral(tenantId)}
   AND batch.id = ${sqlLiteral(ids.batchId)}::uuid
 ORDER BY task.requested_at DESC, task.id DESC
 LIMIT 1;`, "red BPI roundtrip state");
}

async function waitForFile(file, child) {
  const deadline = Date.now() + timeoutSeconds * 1000;
  while (Date.now() < deadline) {
    if (fs.existsSync(file)) return;
    if (child.exitCode !== null) throw new Error(`WMS reversal browser exited before pending gate (${child.exitCode})`);
    await new Promise((resolve) => setTimeout(resolve, 250));
  }
  throw new Error(`Timed out waiting for pending evidence gate: ${file}`);
}

function waitForChild(child, stdout, stderr) {
  return new Promise((resolve, reject) => {
    child.on("error", reject);
    child.on("close", (code) => {
      if (code === 0) resolve();
      else reject(new Error(`WMS reversal browser failed (${code}): ${stderr.value || stdout.value}`));
    });
  });
}

async function runWmsReversalBrowser(tempDir) {
  const script = path.join(
    repoRoot,
    "deploy/docker/scripts/adp-bpi-wms-inbound-reversal-target-acceptance.js",
  );
  const browserReportPath = path.join(tempDir, "wms-reversal-browser.json");
  const screenshotPrefix = path.join(tempDir, "wms-reversal");
  const readyFile = path.join(tempDir, "pending-ready.json");
  const continueFile = path.join(tempDir, "pending-continue");
  const stdout = { value: "" };
  const stderr = { value: "" };
  const child = spawn(process.execPath, [script], {
    cwd: repoRoot,
    env: {
      ...process.env,
      NODE_PATH: nodePath,
      BPI_ACCEPTANCE_MARKER: marker,
      BPI_BATCH_ID: fixture.ids.batchId,
      BPI_ORIGINAL_DOCUMENT_ID: originalDocumentId,
      ADP_BASE_URL: adpBaseUrl,
      ADP_USERNAME: adminUsername,
      ADP_PASSWORD: adminPassword,
      BPI_APPROVER_USERNAME: reviewerUsername,
      BPI_APPROVER_PASSWORD: reviewerPassword,
      BPI_APPROVER_SUBJECT: `legacy-ticket:${reviewerUsername}`,
      BPI_PLANT_ID: plantId,
      BPI_LINE_ID: lineId,
      BPI_BROWSER_TIMEOUT_MS: String(timeoutSeconds * 1000),
      BPI_BROWSER_REPORT: browserReportPath,
      BPI_BROWSER_SCREENSHOT_PREFIX: screenshotPrefix,
      BPI_PENDING_READY_FILE: readyFile,
      BPI_PENDING_CONTINUE_FILE: continueFile,
    },
    stdio: ["ignore", "pipe", "pipe"],
  });
  child.stdout.on("data", (chunk) => { stdout.value += chunk.toString(); });
  child.stderr.on("data", (chunk) => { stderr.value += chunk.toString(); });
  const completion = waitForChild(child, stdout, stderr).then(
    () => ({ error: null }),
    (error) => ({ error }),
  );
  try {
    await waitForFile(readyFile, child);
    const pending = parseLastJson(psqlFile(
      path.join(
        repoRoot,
        "deploy/docker/scripts/bpi-wms-inbound-reversal-acceptance-verification.sql",
      ),
      { marker, batch_id: fixture.ids.batchId },
    ), "pending WMS inbound reversal verification");
    fs.writeFileSync(continueFile, `${new Date().toISOString()}\n`, "utf8");
    const result = await completion;
    if (result.error) throw result.error;
    fs.copyFileSync(`${screenshotPrefix}-pending.png`, pendingScreenshot);
    fs.copyFileSync(`${screenshotPrefix}-approved.png`, approvedScreenshot);
    const browserReport = JSON.parse(fs.readFileSync(browserReportPath, "utf8"));
    browserReport.screenshots = {
      pending: {
        path: path.relative(repoRoot, pendingScreenshot),
        sha256: fileSha256(pendingScreenshot),
      },
      approved: {
        path: path.relative(repoRoot, approvedScreenshot),
        sha256: fileSha256(approvedScreenshot),
      },
    };
    return {
      pending,
      browser: browserReport,
    };
  } finally {
    if (child.exitCode === null && !child.killed) {
      child.kill("SIGTERM");
      await Promise.race([
        completion,
        new Promise((resolve) => setTimeout(resolve, 5_000)),
      ]);
    }
  }
}

async function captureCompletedRoundTrip(ticket, expectedRedDocumentId) {
  const browser = await chromium.launch({ headless: true, args: ["--no-proxy-server"] });
  const context = await browser.newContext({
    ignoreHTTPSErrors: true,
    viewport: { width: 1600, height: 1000 },
    extraHTTPHeaders: { Authorization: `Bearer ${ticket}` },
  });
  await context.addCookies([
    { name: "suposTicket", value: ticket, url: adpBaseUrl },
    { name: "SUPOS_TICKET", value: ticket, url: adpBaseUrl },
  ]);
  await context.addInitScript(({ token, selectedPlantId, selectedLineId }) => {
    window.localStorage.clear();
    window.sessionStorage.clear();
    for (const key of ["suposTicket", "SUPOS_TICKET", "token"]) {
      window.localStorage.setItem(key, token);
      window.sessionStorage.setItem(key, token);
    }
    window.localStorage.setItem("language", "zh_CN");
    window.localStorage.setItem("langu_code", "zh_CN");
    window.localStorage.setItem("bpi.plantId", selectedPlantId);
    window.localStorage.setItem("bpi.lineId", selectedLineId);
  }, { token: ticket, selectedPlantId: plantId, selectedLineId: lineId });
  const page = await context.newPage();
  page.setDefaultTimeout(timeoutSeconds * 1000);
  const consoleErrors = [];
  const pageErrors = [];
  const requestFailures = [];
  const httpErrors = [];
  page.on("console", (message) => {
    if (message.type() === "error") consoleErrors.push(message.text());
  });
  page.on("pageerror", (error) => pageErrors.push(error.message));
  page.on("requestfailed", (requestFailure) => requestFailures.push({
    method: requestFailure.method(),
    url: requestFailure.url(),
    error: requestFailure.failure()?.errorText || "",
  }));
  page.on("response", (response) => {
    if (response.url().includes("/bpi-api/") && response.status() >= 400) {
      httpErrors.push({
        method: response.request().method(),
        url: response.url(),
        status: response.status(),
      });
    }
  });
  try {
    await page.goto(`${adpBaseUrl}/bpi/#/batches`, {
      waitUntil: "networkidle",
      timeout: timeoutSeconds * 1000,
    });
    await page.getByRole("heading", { name: "批次档案" }).waitFor();
    const row = page.locator(`[data-batch-id="${fixture.ids.batchId}"]`);
    await row.waitFor();
    await row.getByText(marker, { exact: true }).waitFor();
    await row.click();
    const drawer = page.locator("#detail-drawer");
    const completed = drawer.locator('[data-release-reversal="COMPLETED"]');
    await completed.waitFor();
    await drawer.locator(".batch-state-band")
      .getByText("INBOUND_REVERSED", { exact: true }).waitFor();
    await completed.getByText("完工入库已冲销", { exact: true }).waitFor();
    await completed.getByText(expectedRedDocumentId, { exact: true }).waitFor();
    assert(await completed.locator("[data-original-document]").textContent()
      === originalDocumentId, "completed page changed the original blue document");
    const geometry = await page.evaluate(() => ({
      viewportWidth: window.innerWidth,
      documentWidth: document.documentElement.scrollWidth,
      viewportHeight: window.innerHeight,
      documentHeight: document.documentElement.scrollHeight,
      drawerWidth: document.querySelector("#detail-drawer")?.getBoundingClientRect().width || 0,
    }));
    assert(geometry.documentWidth <= geometry.viewportWidth + 1,
      `completed page has horizontal overflow: ${JSON.stringify(geometry)}`);
    await completed.scrollIntoViewIfNeeded();
    await page.screenshot({ path: completedScreenshot, fullPage: true });
    const api = await request.newContext({
      ignoreHTTPSErrors: true,
      extraHTTPHeaders: { Authorization: `Bearer ${ticket}` },
    });
    const releaseResponse = await api.get(
      `${adpBaseUrl}/bpi-api/batches/${fixture.ids.batchId}/release`,
      { timeout: timeoutSeconds * 1000 },
    );
    const taskResponse = await api.get(
      `${adpBaseUrl}/bpi-api/batches/${fixture.ids.batchId}/wms/reversal`,
      { timeout: timeoutSeconds * 1000 },
    );
    const releaseBody = await readBody(releaseResponse);
    const taskBody = await readBody(taskResponse);
    await api.dispose();
    assert(releaseResponse.status() === 200
      && releaseBody.json?.data?.batch?.state === "INBOUND_REVERSED",
    `completed release API is invalid: ${releaseResponse.status()} ${releaseBody.text.slice(0, 500)}`);
    assert(taskResponse.status() === 200
      && taskBody.json?.data?.state === "COMPLETED"
      && taskBody.json?.data?.reversalDocumentId === expectedRedDocumentId,
    `completed reversal API is invalid: ${taskResponse.status()} ${taskBody.text.slice(0, 500)}`);
    assert(consoleErrors.length === 0,
      `completed page emitted console errors: ${JSON.stringify(consoleErrors)}`);
    assert(pageErrors.length === 0,
      `completed page emitted page errors: ${JSON.stringify(pageErrors)}`);
    assert(requestFailures.length === 0,
      `completed page emitted request failures: ${JSON.stringify(requestFailures)}`);
    assert(httpErrors.length === 0,
      `completed page emitted BPI HTTP errors: ${JSON.stringify(httpErrors)}`);
    return {
      status: "PASS",
      route: "/bpi/#/batches",
      url: page.url(),
      title: await page.title(),
      geometry,
      consoleErrors,
      pageErrors,
      requestFailures,
      bpiHttpErrors: httpErrors,
      release: releaseBody.json.data,
      task: taskBody.json.data,
      screenshot: {
        path: path.relative(repoRoot, completedScreenshot),
        sha256: fileSha256(completedScreenshot),
      },
    };
  } finally {
    await context.close().catch(() => {});
    await browser.close().catch(() => {});
  }
}

const report = {
  schemaVersion: 1,
  generatedAt: new Date().toISOString(),
  repoCommit: gitHead(),
  database: "PostgreSQL",
  marker,
  mode: fullRoundTrip ? "TARGET_INTERNAL_WMS_ROUNDTRIP" : "FORMAL_IDENTITY_APPROVAL",
  status: "RUNNING",
  target: {
    sshHost: sshTarget.split("@")[1],
    adpBaseUrl,
    remoteRoot,
    backupDir,
  },
  scope: { tenantId, plantId, lineId },
  identity: {
    requesterLogin: adminUsername,
    requesterSubject: `legacy-ticket:${adminUsername}`,
    approverLogin: reviewerUsername,
    approverSubject: `legacy-ticket:${reviewerUsername}`,
    roleCode,
    personCode,
    password: "REDACTED",
  },
  safety: {
    temporaryFormalIdentity: true,
    adapterScopeUsesIsolatedComposeOverride: true,
    baseRuntimeEnvEdited: false,
    adapterImageMustRemainExact: true,
    watchdogSeconds,
    phase2BaseExpectedEnabled: false,
    phase2CommandGateTemporarilyEnabled: true,
    kafkaIngressExpectedEnabled: fullRoundTrip,
    wmsOutboxExpectedEnabled: fullRoundTrip,
    internalMaterialWmsRoundTripExpected: fullRoundTrip,
    externalWmsReceiptExpected: false,
    isolatedKafkaResources: fullRoundTrip,
    cleanupByMarkerAndIdentityOnly: true,
  },
  fixture: null,
  stages: {},
  operations: {},
  browser: null,
  completedBrowser: null,
  kafka: fullRoundTrip ? { topics: kafka } : null,
  postgres: {},
  cleanup: {},
  checks: [],
  issues: [],
};

function check(name, passed, detail) {
  report.checks.push({ name, passed, detail });
  if (!passed) report.issues.push(`${name}: ${JSON.stringify(detail)}`);
}

let admin;
let identityCreated = false;
let roleUserId = null;
let userId = null;
let personId = null;
let reviewerAuth = null;
let scopeActivated = false;
let fixtureSeeded = false;
let fixture = null;

function generateFixture(tempDir) {
  const fixturePath = path.join(tempDir, "wms-reversal-fixture.json");
  const generator = path.join(repoRoot, "deploy/docker/scripts/generate-bpi-wms-outage-fixture.js");
  const result = spawnSync(process.execPath, [generator], {
    cwd: repoRoot,
    encoding: "utf8",
    maxBuffer: 4 * 1024 * 1024,
    env: {
      ...process.env,
      BPI_ACCEPTANCE_MARKER: marker,
      BPI_FIXTURE_OUTPUT: fixturePath,
      BPI_TENANT_ID: tenantId,
      BPI_PLANT_ID: plantId,
      BPI_LINE_ID: lineId,
      BPI_WMS_COMMAND_TOPIC: fullRoundTrip ? kafka.blueCommand : undefined,
    },
  });
  if (result.status !== 0) {
    throw new Error(`WMS command fixture generation failed: ${result.stderr || result.stdout}`);
  }
  const generated = JSON.parse(fs.readFileSync(fixturePath, "utf8"));
  const uuidFields = Object.entries(generated.ids || {});
  assert(uuidFields.length === 7
    && uuidFields.every(([, value]) => /^[0-9a-f-]{36}$/i.test(value)),
  `Generated WMS fixture IDs are invalid: ${JSON.stringify(generated.ids)}`);
  assert(generated.scope?.tenantId === tenantId
    && generated.scope?.plantId === plantId
    && generated.scope?.lineId === lineId,
  `Generated WMS fixture scope is invalid: ${JSON.stringify(generated.scope)}`);
  assert(generated.command?.topic === (fullRoundTrip
    ? kafka.blueCommand : "bpi.wms.completion-inbound-command.v1"),
  `Generated WMS fixture topic is invalid: ${generated.command?.topic}`);
  return generated;
}

async function cleanupIdentity() {
  if (!admin?.page) return;
  const errors = [];
  const state = identityState();
  if (!roleUserId && state.roleUser?.valid === true) roleUserId = Number(state.roleUser.id);
  if (!userId && state.user?.valid === 1) userId = Number(state.user.id);
  if (!personId && state.person?.valid === 1) personId = Number(state.person.id);
  if (roleUserId) {
    const result = await browserApi(admin.page, "DELETE", `/inter-api/rbac/v1/roleUser/${roleUserId}`)
      .catch((error) => ({ ok: false, status: 0, text: error.message }));
    if (!result.ok) errors.push(`role user cleanup: ${result.text}`);
    else roleUserId = null;
  }
  if (userId) {
    const result = await browserApi(admin.page, "DELETE", "/inter-api/auth/v1/user", { ids: [userId] })
      .catch((error) => ({ ok: false, status: 0, text: error.message }));
    if (!result.ok) errors.push(`auth user cleanup: ${result.text}`);
    else userId = null;
  }
  if (personId) {
    const result = await browserApi(admin.page, "DELETE", `/inter-api/organization/v1/person/${personId}`)
      .catch((error) => ({ ok: false, status: 0, text: error.message }));
    if (!result.ok) errors.push(`person cleanup: ${result.text}`);
    else personId = null;
  }
  if (errors.length) throw new Error(errors.join("; "));
}

async function main() {
  fs.mkdirSync(path.dirname(outputPath), { recursive: true });
  fs.mkdirSync(path.dirname(pendingScreenshot), { recursive: true });
  fs.mkdirSync(path.dirname(approvedScreenshot), { recursive: true });
  if (fullRoundTrip) fs.mkdirSync(path.dirname(completedScreenshot), { recursive: true });
  const tempDir = fs.mkdtempSync("/tmp/bpi-formal-wms-reversal-");
  fixture = generateFixture(tempDir);
  report.fixture = {
    batchId: fixture.ids.batchId,
    qualityGateId: fixture.ids.qualityGateId,
    qualityLinkId: fixture.ids.qualityLinkId,
    originalCommandEventId: fixture.ids.commandEventId,
    wmsInboundLinkId: fixture.ids.wmsLinkId,
    originalDocumentId,
    originalPayloadSha256: fixture.command.payloadSha256,
    commandTopic: fixture.command.topic,
  };

  report.stages.adapterPrecheck = runRemote("precheck");
  report.postgres.identityPrecheck = identityPrecheck();
  report.postgres.bpiPrecheck = bpiMarkerState();
  if (fullRoundTrip) {
    report.postgres.materialSchemaPrecheck = materialSchemaState();
    report.postgres.materialPrecheck = materialMarkerState();
  }
  check("formal role and position exist",
    Number(report.postgres.identityPrecheck.roleId) > 0
      && Number(report.postgres.identityPrecheck.positionId) === mainPositionId,
    report.postgres.identityPrecheck);
  check("formal identity marker starts clean",
    Number(report.postgres.identityPrecheck.activeUsers) === 0
      && Number(report.postgres.identityPrecheck.activePersons) === 0,
    report.postgres.identityPrecheck);
  check("BPI marker and plant command/WMS overrides start clean",
    Object.values(report.postgres.bpiPrecheck).every((value) => Number(value) === 0),
    report.postgres.bpiPrecheck);
  check("Phase 2 and write-back switches start disabled",
    !String(report.stages.adapterPrecheck.phase2State).includes(":true"),
    report.stages.adapterPrecheck.phase2State);
  if (fullRoundTrip) {
    check("material-wms reversal expand-only schema is present",
      Object.values(report.postgres.materialSchemaPrecheck).every(Boolean),
      report.postgres.materialSchemaPrecheck);
    check("material-wms marker starts clean",
      Object.values(report.postgres.materialPrecheck).every((value) => Number(value) === 0),
      report.postgres.materialPrecheck);
    check("target material-wms API key is configured without disclosure",
      report.stages.adapterPrecheck.materialApiKeyConfigured === "true",
      { configured: report.stages.adapterPrecheck.materialApiKeyConfigured });
  }
  assert(report.issues.length === 0, `Precheck failed: ${report.issues.join("; ")}`);

  admin = await adminBrowser();
  const personPayload = {
    code: personCode,
    name: personName,
    gender: "sys_gender/male",
    mainPosition: mainPositionId,
    status: "sys_person_status/onWork",
    description: `${marker} formal BPI approver`,
    createUser: false,
    roles: [],
    roleNames: [],
  };
  const personResult = await browserApi(admin.page, "POST", "/inter-api/organization/v1/person", personPayload);
  ensureApiOk(personResult, "Formal approver person create");
  const personState = identityState();
  personId = Number(personState.person?.id || 0);
  assert(personId > 0 && personState.person.valid === 1, "Formal approver person did not persist");

  const userPayload = {
    userName: reviewerUsername,
    password: reviewerPassword,
    personId,
    role: [],
    timeZone: "CST+08:00",
    description: `${marker} formal BPI approver`,
    userType: 0,
  };
  const userResult = await browserApi(admin.page, "POST", "/inter-api/auth/v1/user", userPayload);
  ensureApiOk(userResult, "Formal approver auth user create");
  const userState = identityState();
  userId = Number(userState.user?.id || 0);
  assert(userId > 0 && userState.user.valid === 1 && userState.user.passwordEncoded,
    "Formal approver auth user did not persist with an encoded credential");

  const roleId = Number(report.postgres.identityPrecheck.roleId);
  const roleUserPayload = {
    roleId,
    users: [{ id: userId, userName: reviewerUsername, personName, personCode }],
  };
  const roleUserResult = await browserApi(admin.page, "POST", "/inter-api/rbac/v1/roleUser", roleUserPayload);
  ensureApiOk(roleUserResult, "Formal approver role binding");
  await new Promise((resolve) => setTimeout(resolve, 1000));
  report.postgres.identityProvisioned = identityState();
  roleUserId = Number(report.postgres.identityProvisioned.roleUser?.id || 0);
  identityCreated = true;
  const identityPersisted = roleUserId > 0
      && report.postgres.identityProvisioned.roleUser.valid === true
      && report.postgres.identityProvisioned.roleUser.roleCode === roleCode
      && report.postgres.identityProvisioned.authRoles.some((item) => item.roleCode === roleCode);
  check("formal identity persisted through organization, auth, and RBAC APIs",
    identityPersisted,
    report.postgres.identityProvisioned);
  assert(identityPersisted, "Formal identity RBAC persistence verification failed");

  report.operations.identityProvisioning = {
    person: { method: "POST", endpoint: "/inter-api/organization/v1/person", status: personResult.status },
    user: { method: "POST", endpoint: "/inter-api/auth/v1/user", status: userResult.status, password: "REDACTED" },
    role: { method: "POST", endpoint: "/inter-api/rbac/v1/roleUser", status: roleUserResult.status },
  };

  const reviewerApi = await request.newContext({ ignoreHTTPSErrors: true });
  reviewerAuth = await retryLogin(reviewerApi, reviewerUsername, reviewerPassword, "formal approver");
  const currentUserResponse = await reviewerApi.get(`${adpBaseUrl}/inter-api/auth/v1/currentuser`, {
    headers: { Authorization: `Bearer ${reviewerAuth.ticket}` },
    timeout: timeoutSeconds * 1000,
  });
  const currentUserBody = await readBody(currentUserResponse);
  await reviewerApi.dispose();
  const currentUser = currentUserBody.json?.userInfo || currentUserBody.json?.data?.userInfo;
  const currentRoles = (currentUser?.userRoleList || []).flatMap((item) => [item.code, item.name, item.showName])
    .filter(Boolean);
  report.identity.currentUser = {
    loginStatus: reviewerAuth.status,
    currentUserStatus: currentUserResponse.status(),
    username: currentUser?.username || null,
    cid: currentUser?.cid || null,
    roles: currentRoles,
    ticketMode: /^[0-9a-f-]{36}$/i.test(reviewerAuth.ticket) ? "LEGACY_UUID" : "JWT",
  };
  check("second administrator has a real ADP identity session",
    currentUserResponse.status() === 200
      && currentUser?.username === reviewerUsername
      && String(currentUser?.cid) === tenantId
      && currentRoles.includes(roleCode),
    report.identity.currentUser);

  scopeActivated = true;
  report.stages.adapterActivated = runRemote("activate");
  const expectedReviewerScope = `${reviewerUsername}=${tenantId}|${plantId}|${lineId}`;
  const reviewerScopeActive = String(report.stages.adapterActivated.adapterSubjectScopeRules)
    .split(";")
    .includes(expectedReviewerScope);
  check("adapter exposes the explicit formal reviewer scope",
    reviewerScopeActive,
    report.stages.adapterActivated.adapterSubjectScopeRules);
  assert(reviewerScopeActive, "Formal reviewer adapter scope was not activated");
  check("adapter image is unchanged while scope is active",
    report.stages.adapterActivated.adapterImageId === report.stages.adapterPrecheck.adapterImageId
      && report.stages.adapterActivated.serviceImageId
        === report.stages.adapterPrecheck.serviceImageId
      && report.stages.adapterActivated.wmsAdapterImageId
        === report.stages.adapterPrecheck.wmsAdapterImageId,
    {
      adapterBefore: report.stages.adapterPrecheck.adapterImageId,
      adapterActive: report.stages.adapterActivated.adapterImageId,
      serviceBefore: report.stages.adapterPrecheck.serviceImageId,
      serviceActive: report.stages.adapterActivated.serviceImageId,
      wmsAdapterBefore: report.stages.adapterPrecheck.wmsAdapterImageId,
      wmsAdapterActive: report.stages.adapterActivated.wmsAdapterImageId,
    });
  const temporarilyEnabledKeys = String(report.stages.adapterActivated.phase2State)
    .split(",")
    .filter((item) => item.endsWith(":true"))
    .map((item) => item.split(":")[0]);
  const expectedEnabledKeys = fullRoundTrip
    ? [
      "BPI_PHASE2_INTEGRATION_ENABLED",
      "BPI_PHASE2_KAFKA_ENABLED",
      "BPI_WMS_OUTBOX_ENABLED",
      "BPI_WMS_ADAPTER_ENABLED",
    ]
    : ["BPI_PHASE2_INTEGRATION_ENABLED"];
  check(fullRoundTrip
    ? "only the guarded internal WMS roundtrip switches are temporarily enabled"
    : "only the Phase 2 command gate is temporarily enabled",
  temporarilyEnabledKeys.length === expectedEnabledKeys.length
      && expectedEnabledKeys.every((key) => temporarilyEnabledKeys.includes(key)),
    report.stages.adapterActivated.phase2State);
  assert(report.issues.length === 0,
    `Controlled runtime activation failed: ${report.issues.join("; ")}`);

  if (fullRoundTrip) {
    const fixtureOutput = psqlFile(
      path.join(repoRoot, "deploy/docker/scripts/bpi-wms-outage-recovery-fixture.sql"),
      fixture.sqlVariables,
    );
    fixtureSeeded = true;
    report.postgres.fixture = parseLastJson(fixtureOutput, "WMS roundtrip initial fixture");
    check("controlled released batch and blue command were seeded",
      Number(report.postgres.fixture.batchRows) === 1
        && Number(report.postgres.fixture.outboxRows) === 1
        && Number(report.postgres.fixture.wmsLinkRows) === 1
        && report.postgres.fixture.commandEventId === fixture.ids.commandEventId
        && report.postgres.fixture.wmsIdempotencyKey === `${marker}|WMS|1`
        && Number(report.postgres.fixture.payloadBytes) > 0,
      report.postgres.fixture);
    report.postgres.blueRoundTrip = await waitFor(
      "blue completion-inbound Kafka and material-wms roundtrip",
      () => blueRoundTripState(),
      (state) => state.bpi.batchState === "INBOUNDED"
        && Number(state.bpi.batchRevision) === 4
        && state.bpi.wmsStatus === "INBOUNDED"
        && state.bpi.linkStatus === "ACCEPTED"
        && state.bpi.outboxStatus === "PUBLISHED"
        && state.bpi.topic === kafka.blueCommand
        && state.bpi.payloadSha256 === fixture.command.payloadSha256
        && Number(state.material.documents) === 1
        && state.material.status === "POSTED"
        && state.material.idempotencyKey === `${marker}|WMS|1`
        && Number(state.material.lineRows) === 1,
    );
    originalDocumentId = report.postgres.blueRoundTrip.bpi.documentId;
    report.fixture.originalDocumentId = originalDocumentId;
    check("blue document completed through isolated Kafka and real material-wms",
      Boolean(originalDocumentId)
        && originalDocumentId === report.postgres.blueRoundTrip.material.documentNo,
      report.postgres.blueRoundTrip);
    report.kafka.afterBlue = runRemote("kafka-state");
    check("isolated blue command and receipt each have one durable record and zero DLQ",
      Number(report.kafka.afterBlue.blueCommand) === 1
        && Number(report.kafka.afterBlue.blueReceipt) === 1
        && Number(report.kafka.afterBlue.blueCommandDlq) === 0
        && Number(report.kafka.afterBlue.blueReceiptDlq) === 0
        && Number(report.kafka.afterBlue.redCommand) === 0
        && Number(report.kafka.afterBlue.redReceipt) === 0
        && Number(report.kafka.afterBlue.wmsLag) === 0
        && Number(report.kafka.afterBlue.receiptLag) === 0,
      report.kafka.afterBlue);
    report.stages.wmsPaused = runRemote("stop-wms");
  } else {
    const fixtureOutput = psqlFile(
      path.join(
        repoRoot,
        "deploy/docker/scripts/bpi-wms-inbound-reversal-acceptance-fixture.sql",
      ),
      { ...fixture.sqlVariables, original_document_id: originalDocumentId },
    );
    fixtureSeeded = true;
    report.postgres.fixture = parseLastJson(fixtureOutput, "WMS inbound reversal fixture");
    check("controlled accepted WMS inbound fixture is immutable and active",
      Number(report.postgres.fixture.batchRows) === 1
        && report.postgres.fixture.commandsEnabled === true
        && report.postgres.fixture.wmsLinkEnabled === true
        && report.postgres.fixture.batchState === "INBOUNDED"
        && Number(report.postgres.fixture.batchRevision) === 4
        && report.postgres.fixture.originalCommandEventId === fixture.ids.commandEventId
        && report.postgres.fixture.originalDocumentId === originalDocumentId
        && report.postgres.fixture.originalPayloadSha256 === fixture.command.payloadSha256,
      report.postgres.fixture);
  }
  assert(report.issues.length === 0,
    `Initial WMS acceptance fixture failed: ${report.issues.join("; ")}`);

  const browserEvidence = await runWmsReversalBrowser(tempDir);
  report.postgres.pending = browserEvidence.pending;
  report.browser = browserEvidence.browser;
  report.operations.reversal = report.browser.operations;
  check("requester persisted a pending reversal without creating a red command",
    report.postgres.pending.batch?.state === "INBOUNDED"
      && Number(report.postgres.pending.batch?.revision) === 5
      && report.postgres.pending.batch?.wmsStatus === "INBOUNDED"
      && report.postgres.pending.reversalTask?.state === "PENDING_APPROVAL"
      && Number(report.postgres.pending.reversalTask?.revision) === 1
      && report.postgres.pending.reversalTask?.requestedBy === `legacy-ticket:${adminUsername}`
      && !report.postgres.pending.reversalTask?.decidedBy
      && Number(report.postgres.pending.reversalOutboxRows) === 0
      && Number(report.postgres.pending.idempotencyRows) === 1,
    report.postgres.pending);
  check("pending state preserves the accepted blue document byte-for-byte",
    report.postgres.pending.originalInbound?.commandEventId === fixture.ids.commandEventId
      && report.postgres.pending.originalInbound?.documentId === originalDocumentId
      && report.postgres.pending.originalInbound?.payloadSha256 === fixture.command.payloadSha256
      && report.postgres.pending.originalInbound?.status === "ACCEPTED"
      && report.postgres.pending.originalInbound?.outboxStatus === "PUBLISHED",
    report.postgres.pending.originalInbound);
  check("formal approver used a separate ADP browser session through the adapter",
    report.browser.status === "PASS"
      && report.browser.loginStatus === 200
      && report.browser.approverLoginStatus === 200
      && report.browser.operations?.approval?.url?.includes("/bpi-api/")
      && report.browser.operations?.approval?.response?.decidedBy === `legacy-ticket:${reviewerUsername}`
      && report.browser.operations?.sameActorRejection?.status === 403,
    {
      status: report.browser.status,
      loginStatus: report.browser.loginStatus,
      approverLoginStatus: report.browser.approverLoginStatus,
      approval: report.browser.operations?.approval,
      sameActorRejection: report.browser.operations?.sameActorRejection,
    });

  if (fullRoundTrip) {
    report.kafka.afterApproval = await waitFor(
      "red command publication while WMS adapter is paused",
      () => runRemote("kafka-state"),
      (state) => Number(state.redCommand) === 1
        && Number(state.redReceipt) === 0
        && Number(state.redCommandDlq) === 0
        && Number(state.redReceiptDlq) === 0,
    );
  }

  report.postgres.approved = parseLastJson(psqlFile(
    path.join(
      repoRoot,
      "deploy/docker/scripts/bpi-wms-inbound-reversal-acceptance-verification.sql",
    ),
    { marker, batch_id: fixture.ids.batchId },
  ), "approved WMS inbound reversal verification");
  check("PostgreSQL records distinct requester and approver identities",
    report.postgres.approved.batch?.state === "INBOUND_REVERSING"
      && Number(report.postgres.approved.batch?.revision) === 6
      && report.postgres.approved.batch?.wmsStatus === "REVERSAL_PENDING"
      && report.postgres.approved.reversalTask?.state === "PENDING_WMS"
      && Number(report.postgres.approved.reversalTask?.revision) === 2
      && report.postgres.approved.reversalTask?.requestedBy === `legacy-ticket:${adminUsername}`
      && report.postgres.approved.reversalTask?.decidedBy === `legacy-ticket:${reviewerUsername}`
      && report.postgres.approved.reversalTask?.requestedBy
        !== report.postgres.approved.reversalTask?.decidedBy,
    report.postgres.approved);
  check("approval appends one durable red command without mutating the blue document",
    report.postgres.approved.originalInbound?.commandEventId === fixture.ids.commandEventId
      && report.postgres.approved.originalInbound?.documentId === originalDocumentId
      && report.postgres.approved.originalInbound?.payloadSha256 === fixture.command.payloadSha256
      && report.postgres.approved.originalInbound?.status === "ACCEPTED"
      && report.postgres.approved.originalInbound?.outboxStatus === "PUBLISHED"
      && Number(report.postgres.approved.reversalOutboxRows) === 1
      && report.postgres.approved.reversalOutbox?.eventType
        === "WMS_COMPLETION_INBOUND_REVERSAL_COMMAND"
      && report.postgres.approved.reversalOutbox?.topic
        === (fullRoundTrip
          ? kafka.redCommand : "bpi.wms.completion-inbound-reversal-command.v1")
      && report.postgres.approved.reversalOutbox?.status
        === (fullRoundTrip ? "PUBLISHED" : "PENDING")
      && report.postgres.approved.reversalOutbox?.id !== fixture.ids.commandEventId
      && Number(report.postgres.approved.reversalOutbox?.payloadBytes) > 0,
    {
      blue: report.postgres.approved.originalInbound,
      red: report.postgres.approved.reversalOutbox,
    });
  const approvedActions = report.postgres.approved.stateEvents.map((item) => item.action);
  check("reversal lifecycle has durable state, audit, and idempotency evidence",
    approvedActions.includes("WMS_INBOUND_REVERSAL_REQUESTED")
      && approvedActions.includes("WMS_INBOUND_REVERSAL_APPROVED")
      && report.postgres.approved.auditEvents.length === 2
      && Number(report.postgres.approved.idempotencyRows) === 2
      && Number(report.postgres.approved.reversalInboxRows) === 0,
    {
      stateEvents: report.postgres.approved.stateEvents,
      auditEvents: report.postgres.approved.auditEvents,
      idempotencyRows: report.postgres.approved.idempotencyRows,
      reversalInboxRows: report.postgres.approved.reversalInboxRows,
    });

  assert(report.issues.length === 0, `Formal identity acceptance failed: ${report.issues.join("; ")}`);
  if (fullRoundTrip) {
    report.stages.wmsResumed = runRemote("start-wms");
    report.postgres.redRoundTrip = await waitFor(
      "red reversal Kafka, material-wms and BPI receipt roundtrip",
      () => reversalRoundTripState(),
      (state) => state.batchState === "INBOUND_REVERSED"
        && Number(state.batchRevision) === 7
        && state.wmsStatus === "REVERSED"
        && state.taskState === "COMPLETED"
        && Number(state.taskRevision) === 3
        && state.redOutboxStatus === "PUBLISHED"
        && state.redOutboxTopic === kafka.redCommand
        && Boolean(state.redReceiptEventId)
        && Boolean(state.redDocumentId)
        && !state.errorCode,
    );
    const finalVariables = {
      ...fixture.sqlVariables,
      blue_command_topic: kafka.blueCommand,
      red_command_topic: kafka.redCommand,
    };
    report.postgres.roundTrip = parseLastJson(psqlFile(
      path.join(
        repoRoot,
        "deploy/docker/scripts/bpi-wms-formal-roundtrip-verification.sql",
      ),
      finalVariables,
    ), "formal WMS BPI roundtrip verification");
    const red = report.postgres.roundTrip.red;
    report.postgres.materialRoundTrip = parseLastJson(psqlFileIn(
      "adp",
      path.join(
        repoRoot,
        "deploy/docker/scripts/bpi-wms-formal-roundtrip-material-verification.sql",
      ),
      {
        marker,
        blue_outbox_id: fixture.ids.commandEventId,
        red_outbox_id: red.commandEventId,
        blue_document_no: report.postgres.roundTrip.blue.documentId,
        red_document_no: red.documentId,
        red_idempotency_key: red.idempotencyKey,
      },
    ), "formal WMS material roundtrip verification");
    report.kafka.final = runRemote("kafka-state");
    check("isolated Kafka chain has one blue and one red command/receipt with zero DLQ and lag",
      Number(report.kafka.final.blueCommand) === 1
        && Number(report.kafka.final.blueReceipt) === 1
        && Number(report.kafka.final.redCommand) === 1
        && Number(report.kafka.final.redReceipt) === 1
        && Number(report.kafka.final.blueCommandDlq) === 0
        && Number(report.kafka.final.blueReceiptDlq) === 0
        && Number(report.kafka.final.redCommandDlq) === 0
        && Number(report.kafka.final.redReceiptDlq) === 0
        && Number(report.kafka.final.qcs) === 0
        && Number(report.kafka.final.qcsDlq) === 0
        && Number(report.kafka.final.wmsLag) === 0
        && Number(report.kafka.final.receiptLag) === 0,
      report.kafka.final);
    report.completedBrowser = await captureCompletedRoundTrip(
      reviewerAuth.ticket,
      red.documentId,
    );
    check("completed INBOUND_REVERSED state is visible in a real formal ADP browser session",
      report.completedBrowser.status === "PASS"
        && report.completedBrowser.release?.batch?.state === "INBOUND_REVERSED"
        && report.completedBrowser.task?.state === "COMPLETED"
        && report.completedBrowser.task?.reversalDocumentId === red.documentId,
      report.completedBrowser);
    assert(report.issues.length === 0,
      `Formal internal WMS roundtrip failed: ${report.issues.join("; ")}`);
    report.status = "PASS_TARGET_FORMAL_IDENTITY_INTERNAL_WMS_ROUNDTRIP";
  } else {
    report.status = "PASS_TARGET_FORMAL_IDENTITY_WMS_REVERSAL_TWO_BROWSER_SESSIONS";
  }
}

async function finish() {
  const cleanupErrors = [];
  if (fullRoundTrip && fixtureSeeded) {
    try {
      const output = psqlFileIn(
        "adp",
        path.join(
          repoRoot,
          "deploy/docker/scripts/bpi-wms-formal-roundtrip-material-cleanup.sql",
        ),
        { marker },
      );
      report.cleanup.material = output.trim().split(/\r?\n/).slice(-3);
    } catch (error) {
      cleanupErrors.push(`material-wms cleanup: ${error.message}`);
    }
  }
  if (fixtureSeeded) {
    try {
      const output = psqlFile(
        path.join(
          repoRoot,
          "deploy/docker/scripts/bpi-wms-inbound-reversal-acceptance-cleanup.sql",
        ),
        fixture.sqlVariables,
      );
      report.cleanup.bpi = output.trim().split(/\r?\n/).slice(-3);
      fixtureSeeded = false;
    } catch (error) {
      cleanupErrors.push(`BPI cleanup: ${error.message}`);
    }
  }
  if (scopeActivated) {
    try {
      report.stages.adapterRestored = runRemote("restore");
      report.cleanup.adapter = runRemote("complete");
      scopeActivated = false;
    } catch (error) {
      cleanupErrors.push(`Adapter restore: ${error.message}`);
    }
  }
  if (identityCreated || roleUserId || userId || personId) {
    try {
      await cleanupIdentity();
      identityCreated = false;
    } catch (error) {
      cleanupErrors.push(`Identity cleanup: ${error.message}`);
    }
  }
  if (admin) {
    await admin.context.close().catch(() => {});
    await admin.browser.close().catch(() => {});
  }
  try {
    report.postgres.identityFinal = identityState();
    report.postgres.bpiFinal = fixture ? bpiMarkerState() : {};
    if (fullRoundTrip) report.postgres.materialFinal = materialMarkerState();
    const activeIdentityRows = [
      report.postgres.identityFinal.person?.valid === 1,
      report.postgres.identityFinal.user?.valid === 1,
      report.postgres.identityFinal.roleUser?.valid === true,
      report.postgres.identityFinal.authRoles?.length > 0,
    ].filter(Boolean).length;
    check("temporary formal identity has no active residual binding", activeIdentityRows === 0,
      report.postgres.identityFinal);
    check("BPI marker cleanup has zero residual rows",
      Object.values(report.postgres.bpiFinal).every((value) => Number(value) === 0),
      report.postgres.bpiFinal);
    if (fullRoundTrip) {
      check("material-wms marker cleanup has zero residual rows",
        Object.values(report.postgres.materialFinal).every((value) => Number(value) === 0),
        report.postgres.materialFinal);
    }
    if (report.stages.adapterRestored) {
      check("adapter scope and image restored exactly",
        report.stages.adapterRestored.adapterImageId === report.stages.adapterPrecheck.adapterImageId
          && report.stages.adapterRestored.serviceImageId
            === report.stages.adapterPrecheck.serviceImageId
          && report.stages.adapterRestored.wmsAdapterImageId
            === report.stages.adapterPrecheck.wmsAdapterImageId
          && report.stages.adapterRestored.adapterSubjectScopeRules
            === report.stages.adapterPrecheck.adapterSubjectScopeRules,
        {
          before: report.stages.adapterPrecheck,
          restored: report.stages.adapterRestored,
        });
      check("Phase 2 and write-back switches remain disabled",
        !String(report.stages.adapterRestored.phase2State).includes(":true"),
        report.stages.adapterRestored.phase2State);
      if (fullRoundTrip) {
        check("isolated Kafka topics and consumer groups were removed",
          report.cleanup.adapter.isolatedKafkaCleaned === "true",
          report.cleanup.adapter);
      }
    }
  } catch (error) {
    cleanupErrors.push(`Final verification: ${error.message}`);
  }
  if (cleanupErrors.length) report.issues.push(...cleanupErrors);
  if (report.issues.length || cleanupErrors.length) report.status = "FAIL";
  else if (report.status.startsWith("PASS_")) report.status += "_CLEANED";
  report.generatedAt = new Date().toISOString();
  fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`, "utf8");
  if (cleanupErrors.length) throw new Error(cleanupErrors.join("; "));
}

async function run() {
  try {
    await main();
  } catch (error) {
    report.issues.push(error?.message || String(error));
    report.status = "FAIL";
    throw error;
  } finally {
    await finish();
  }
}

if (printRemoteScript) {
  process.stdout.write(remoteScript);
} else {
  run().then(() => {
    console.log(`BPI formal identity WMS reversal acceptance: ${report.status} (${marker})`);
  }).catch((error) => {
    console.error(error?.stack || error);
    process.exitCode = 1;
  });
}
