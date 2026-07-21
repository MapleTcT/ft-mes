#!/usr/bin/env node
"use strict";

const fs = require("node:fs");
const path = require("node:path");
const { spawnSync } = require("node:child_process");

const repoRoot = path.resolve(__dirname, "../../..");
const sshTarget = process.env.ADP_DB_SSH_TARGET || "v6@10.11.100.17";
const remoteRoot = process.env.ADP_REMOTE_DEPLOY_ROOT
  || "/home/v6/adp-mes-docker-newbase-20260611-181921";
const streamRoot = process.env.BPI_STREAM_REMOTE_ROOT || "/home/v6/adp-bpi-stream-v15";
const rollbackServiceImage = required("BPI_ROLLBACK_SERVICE_IMAGE");
const rollbackAdapterImage = required("BPI_ROLLBACK_ADAPTER_IMAGE");
const rollbackJobJar = required("BPI_ROLLBACK_JOB_JAR");
const loadClientJobJar = required("BPI_LOAD_CLIENT_JOB_JAR");
const adpBaseUrl = required("ADP_BASE_URL").replace(/\/+$/, "");
const username = required("ADP_USERNAME");
const password = required("ADP_PASSWORD");
const marker = process.env.BPI_INTEGRATED_ROLLBACK_MARKER
  || `ADP_BPI_INTEGRATED_ROLLBACK_${new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14)}`;
const tenantId = process.env.BPI_ACCEPTANCE_TENANT_ID || "1000";
const plantId = process.env.BPI_ACCEPTANCE_PLANT_ID || "PLANT-01";
const lineId = process.env.BPI_ACCEPTANCE_LINE_ID || `LINE-IRB-${marker.slice(-14)}`;
const topologyCode = `TOPO-${marker}`;
const topologyVersion = "1";
const ruleCode = `RULE-${marker}`;
const ruleVersion = "1";
const orderId = `MO-${marker}`;
const productId = `PRODUCT-${marker}`;
const deviceId = `DEVICE-${marker}`;
const timeoutSeconds = positiveInteger("BPI_INTEGRATED_ROLLBACK_TIMEOUT_SECONDS", 300, 60, 900);
const watchdogSeconds = positiveInteger("BPI_INTEGRATED_ROLLBACK_WATCHDOG_SECONDS", 1200, 300, 3600);
const expectedFlywayVersion = positiveInteger("BPI_EXPECTED_FLYWAY_VERSION", 24, 1, 999);
const backupDir = process.env.BPI_INTEGRATED_ROLLBACK_BACKUP_DIR
  || `/data/docker/bpi-upgrade-backups/${marker}`;
const outputPath = path.resolve(process.env.BPI_INTEGRATED_ROLLBACK_OUTPUT
  || path.join(repoRoot, "metadata/bpi-integrated-rollback-acceptance.json"));
const rollbackScreenshot = path.resolve(process.env.BPI_INTEGRATED_ROLLBACK_OLD_SCREENSHOT
  || path.join(repoRoot, "metadata/bpi-integrated-rollback-old-stack.png"));
const restoredScreenshot = path.resolve(process.env.BPI_INTEGRATED_ROLLBACK_RESTORED_SCREENSHOT
  || path.join(repoRoot, "metadata/bpi-integrated-rollback-restored-stack.png"));
const cleanupScreenshot = path.resolve(process.env.BPI_INTEGRATED_ROLLBACK_CLEANUP_SCREENSHOT
  || path.join(repoRoot, "metadata/bpi-integrated-rollback-cleanup.png"));
const nodePath = process.env.NODE_PATH || path.join(repoRoot, "frontend/apps/bpi/node_modules");
const printRemoteScript = process.argv.includes("--print-remote-script");
const precheckOnly = process.argv.includes("--precheck-only");

if (!printRemoteScript && !precheckOnly && process.env.BPI_INTEGRATED_ROLLBACK_CONFIRM
    !== "ROLLBACK_BPI_SERVICE_ADAPTER_FLINK_AND_RESTORE") {
  throw new Error(
    "Set BPI_INTEGRATED_ROLLBACK_CONFIRM=ROLLBACK_BPI_SERVICE_ADAPTER_FLINK_AND_RESTORE",
  );
}

assertSafe(sshTarget, "SSH target", /^[A-Za-z0-9._-]+@[A-Za-z0-9._-]+$/);
for (const [label, value] of [
  ["remote root", remoteRoot],
  ["stream root", streamRoot],
  ["backup directory", backupDir],
  ["rollback JAR", rollbackJobJar],
  ["load-client JAR", loadClientJobJar],
]) {
  assertSafe(value, label, /^\/[A-Za-z0-9._/-]+$/);
}
for (const [label, value] of [
  ["marker", marker],
  ["tenant", tenantId],
  ["plant", plantId],
  ["line", lineId],
  ["topology code", topologyCode],
  ["rule code", ruleCode],
  ["order", orderId],
  ["product", productId],
  ["device", deviceId],
]) {
  assertSafe(value, label, /^[A-Za-z0-9._-]{1,128}$/);
}
for (const [label, value] of [
  ["service rollback image", rollbackServiceImage],
  ["adapter rollback image", rollbackAdapterImage],
]) {
  assertSafe(value, label, /^[A-Za-z0-9._/-]+:[A-Za-z0-9._-]+$/);
}

function required(key) {
  const value = process.env[key];
  if (!value || !value.trim()) throw new Error(`${key} is required`);
  return value.trim();
}

function positiveInteger(key, fallback, minimum, maximum) {
  const value = Number(process.env[key] || fallback);
  if (!Number.isInteger(value) || value < minimum || value > maximum) {
    throw new Error(`${key} must be an integer from ${minimum} to ${maximum}`);
  }
  return value;
}

function assertSafe(value, label, pattern) {
  if (!pattern.test(value)) throw new Error(`Unsafe ${label}: ${value}`);
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

function shellQuote(value) {
  return `'${String(value).replace(/'/g, `'"'"'`)}'`;
}

function parseKeyValues(output) {
  const values = {};
  for (const line of output.split(/\r?\n/)) {
    const separator = line.indexOf("=");
    if (separator <= 0) continue;
    values[line.slice(0, separator)] = line.slice(separator + 1);
  }
  return values;
}

function parseLastJson(output, label) {
  for (const line of output.trim().split(/\r?\n/).reverse()) {
    const candidate = line.trim();
    if (!candidate.startsWith("{") || !candidate.endsWith("}")) continue;
    try {
      return JSON.parse(candidate);
    } catch (_error) {
      // Continue until a complete JSON row is found.
    }
  }
  throw new Error(`${label} did not return a JSON row: ${output.slice(-1000)}`);
}

const remoteScript = String.raw`set -eu
action=$1
root=$2
stream_root=$3
backup_dir=$4
rollback_service_image=$5
rollback_adapter_image=$6
rollback_jar=$7
load_client_jar=$8
marker=$9
shift 9
tenant_id=$1
plant_id=$2
line_id=$3
topology_code=$4
topology_version=$5
rule_code=$6
rule_version=$7
order_id=$8
product_id=$9
shift 9
device_id=$1
timeout=$2
watchdog_timeout=$3
expected_flyway=$4

runtime_dir="$root/deploy/docker"
runtime_compose="$runtime_dir/docker-compose.yml"
base_env="$runtime_dir/.env"
stream_dir="$stream_root/deploy/bpi-streaming"
stream_compose="$stream_dir/docker-compose.yml"
stream_env="$stream_dir/.env"
current_env="$backup_dir/current-acceptance.env"
rollback_env="$backup_dir/rollback.env"
recovery_env="$backup_dir/recovery.env"
complete_file="$backup_dir/COMPLETE"

env_value() {
  key=$1
  fallback=$2
  file=$3
  value=$(sed -n "s/^$key=//p" "$file" | tail -1)
  if [ -n "$value" ]; then printf '%s' "$value"; else printf '%s' "$fallback"; fi
}

update_env() {
  file=$1
  shift
  python3 - "$file" "$@" <<'PY'
import os
import stat
import sys
from pathlib import Path

path = Path(sys.argv[1])
updates = dict(item.split("=", 1) for item in sys.argv[2:])
mode = stat.S_IMODE(path.stat().st_mode)
lines = path.read_text(encoding="utf-8").splitlines()
seen = set()
result = []
for line in lines:
    key = line.split("=", 1)[0] if "=" in line else ""
    if key in updates:
        result.append(f"{key}={updates[key]}")
        seen.add(key)
    else:
        result.append(line)
for key, value in updates.items():
    if key not in seen:
        result.append(f"{key}={value}")
temporary = path.with_name(path.name + ".integrated-rollback-tmp")
temporary.write_text("\n".join(result) + "\n", encoding="utf-8")
os.chmod(temporary, mode)
temporary.replace(path)
PY
}

runtime_dc() {
  selected_env=$1
  shift
  docker compose --env-file "$selected_env" -f "$runtime_compose" --profile bpi "$@"
}

stream_dc() {
  docker compose --env-file "$stream_env" -f "$stream_compose" "$@"
}

runtime_health() {
  selected_env=$1
  service=$2
  id=$(runtime_dc "$selected_env" ps -q "$service")
  if [ -z "$id" ]; then printf 'missing'; return; fi
  docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$id"
}

wait_runtime_healthy() {
  selected_env=$1
  service=$2
  deadline=$(( $(date +%s) + timeout ))
  while [ "$(date +%s)" -lt "$deadline" ]; do
    state=$(runtime_health "$selected_env" "$service")
    case "$state" in
      healthy) return 0 ;;
      exited|dead|missing) runtime_dc "$selected_env" logs --tail 120 "$service" >&2 || true; return 1 ;;
    esac
    sleep 3
  done
  runtime_dc "$selected_env" logs --tail 120 "$service" >&2 || true
  return 1
}

restart_runtime() {
  selected_env=$1
  runtime_dc "$selected_env" up -d --no-deps --force-recreate --no-build bpi-service
  wait_runtime_healthy "$selected_env" bpi-service
  runtime_dc "$selected_env" up -d --no-deps --force-recreate --no-build bpi-adapter
  wait_runtime_healthy "$selected_env" bpi-adapter
}

stream_rest_url() {
  bind=$(env_value BPI_BIND_ADDRESS 127.0.0.1 "$stream_env")
  port=$(env_value BPI_FLINK_REST_PORT 18081 "$stream_env")
  printf 'http://%s:%s' "$bind" "$port"
}

stream_job() {
  rest=$(stream_rest_url)
  curl -fsS "$rest/jobs/overview" | python3 -c '
import json, sys
jobs = [job for job in json.load(sys.stdin).get("jobs", [])
        if job.get("name") == "ft-mes-bpi-batch-boundary-v1" and job.get("state") == "RUNNING"]
if len(jobs) != 1:
    raise SystemExit("expected exactly one RUNNING BPI job")
job = jobs[0]
print("{}|{}|{}".format(
    job.get("jid"),
    job.get("tasks", {}).get("running", 0),
    job.get("tasks", {}).get("total", 0),
))
'
}

stream_checkpoint() {
  rest=$(stream_rest_url)
  job_id=$(stream_job | cut -d'|' -f1)
  curl -fsS "$rest/jobs/$job_id/checkpoints" | python3 -c '
import json, sys
latest = json.load(sys.stdin).get("latest", {}).get("completed") or {}
print(latest.get("id", ""))
'
}

mounted_jar() {
  id=$(stream_dc ps -q bpi-jobmanager)
  docker inspect "$id" | python3 -c '
import json, sys
container = json.load(sys.stdin)[0]
matches = [mount.get("Source", "") for mount in container.get("Mounts", [])
           if mount.get("Destination") == "/opt/flink/usrlib/bpi-stream-engine-job.jar"]
print(matches[0] if len(matches) == 1 else "")
'
}

update_stream() {
  selected_jar=$1
  selected_savepoint=$2
  update_env "$stream_env" \
    "BPI_JOB_JAR=$selected_jar" \
    "BPI_FLINK_RESTORE_SAVEPOINT_PATH=$selected_savepoint" \
    "BPI_FLINK_ALLOW_NON_RESTORED_STATE=false"
}

capture_savepoint() {
  report_path=$1
  BPI_SAVEPOINT_CAPTURE_REPORT="$report_path" \
    sh "$stream_dir/scripts/capture-upgrade-savepoint.sh" "$stream_env" >/dev/null
  python3 - "$report_path" <<'PY'
import json, sys
print(json.load(open(sys.argv[1], encoding="utf-8"))["savepoint"]["path"])
PY
}

restore_stream() {
  BPI_STREAM_RESTORE_CONFIRM=RESTORE_BPI_FLINK_FROM_SAVEPOINT \
    sh "$stream_dir/scripts/restore-from-savepoint.sh" "$stream_env" >/dev/null
}

database_state() {
  docker exec adp-mes-newbase-postgres-1 psql -X -At -U adp -d ft_mes_bpi \
    -c "
SELECT json_build_object(
  'flywayVersion', (SELECT max(version::integer) FROM bpi.flyway_schema_history WHERE success),
  'topologies', (SELECT count(*) FROM bpi.bpi_topology_versions WHERE tenant_id = '$tenant_id' AND created_by = '$marker'),
  'rules', (SELECT count(*) FROM bpi.bpi_rule_versions WHERE tenant_id = '$tenant_id' AND created_by = '$marker'),
  'candidates', (SELECT count(*) FROM bpi.bpi_batch_candidates WHERE tenant_id = '$tenant_id' AND order_id = '$order_id'),
  'batches', (SELECT count(*) FROM bpi.bpi_batch_instances WHERE tenant_id = '$tenant_id' AND order_id = '$order_id'),
  'pointCatalogSnapshots', (SELECT count(*) FROM bpi.bpi_point_catalog_snapshots WHERE tenant_id = '$tenant_id' AND plant_id = '$plant_id' AND line_id = '$line_id' AND source_instance = 'BPI-JOINT-$marker')
);"
}

phase2_state() {
  for key in BPI_PHASE2_INTEGRATION_ENABLED BPI_PHASE2_PROTOBUF_HTTP_INGRESS_ENABLED BPI_PHASE2_KAFKA_ENABLED BPI_WMS_OUTBOX_ENABLED BPI_WMS_ADAPTER_ENABLED QCS_BPI_OUTBOX_ENABLED; do
    value=$(env_value "$key" false "$base_env")
    printf '%s:%s,' "$key" "$value"
  done
}

emit_state() {
  selected_env=$1
  service_id=$(runtime_dc "$selected_env" ps -q bpi-service)
  adapter_id=$(runtime_dc "$selected_env" ps -q bpi-adapter)
  jar=$(mounted_jar)
  printf 'serviceImage=%s\n' "$(docker inspect -f '{{.Config.Image}}' "$service_id")"
  printf 'serviceImageId=%s\n' "$(docker inspect -f '{{.Image}}' "$service_id")"
  printf 'serviceHealth=%s\n' "$(runtime_health "$selected_env" bpi-service)"
  printf 'adapterImage=%s\n' "$(docker inspect -f '{{.Config.Image}}' "$adapter_id")"
  printf 'adapterImageId=%s\n' "$(docker inspect -f '{{.Image}}' "$adapter_id")"
  printf 'adapterHealth=%s\n' "$(runtime_health "$selected_env" bpi-adapter)"
  printf 'jobJar=%s\n' "$jar"
  printf 'jobJarSha256=%s\n' "$(sha256sum "$jar" | awk '{print $1}')"
  printf 'flinkJob=%s\n' "$(stream_job)"
  printf 'flinkCheckpoint=%s\n' "$(stream_checkpoint)"
  printf 'databaseState=%s\n' "$(database_state)"
  printf 'phase2State=%s\n' "$(phase2_state)"
}

write_watchdog() {
  cat > "$backup_dir/watchdog.sh" <<'SH'
#!/bin/sh
set -eu
root=$1
stream_root=$2
backup_dir=$3
wait_seconds=$4
sleep "$wait_seconds"
[ ! -f "$backup_dir/COMPLETE" ] || exit 0
. "$backup_dir/recovery.env"
stream_dir="$stream_root/deploy/bpi-streaming"
stream_env="$stream_dir/.env"
python3 - "$stream_env" "$CURRENT_JAR" "$CURRENT_SAVEPOINT" <<'PY'
import os, stat, sys
from pathlib import Path
path = Path(sys.argv[1])
updates = {
    "BPI_JOB_JAR": sys.argv[2],
    "BPI_FLINK_RESTORE_SAVEPOINT_PATH": sys.argv[3],
    "BPI_FLINK_ALLOW_NON_RESTORED_STATE": "false",
}
mode = stat.S_IMODE(path.stat().st_mode)
lines = path.read_text(encoding="utf-8").splitlines()
seen = set()
result = []
for line in lines:
    key = line.split("=", 1)[0] if "=" in line else ""
    if key in updates:
        result.append(f"{key}={updates[key]}")
        seen.add(key)
    else:
        result.append(line)
for key, value in updates.items():
    if key not in seen:
        result.append(f"{key}={value}")
tmp = path.with_name(path.name + ".watchdog-tmp")
tmp.write_text("\n".join(result) + "\n", encoding="utf-8")
os.chmod(tmp, mode)
tmp.replace(path)
PY
BPI_STREAM_RESTORE_CONFIRM=RESTORE_BPI_FLINK_FROM_SAVEPOINT \
  sh "$stream_dir/scripts/restore-from-savepoint.sh" "$stream_env"
runtime_dir="$root/deploy/docker"
docker compose --env-file "$runtime_dir/.env" -f "$runtime_dir/docker-compose.yml" --profile bpi \
  up -d --no-deps --force-recreate --no-build bpi-service
docker compose --env-file "$runtime_dir/.env" -f "$runtime_dir/docker-compose.yml" --profile bpi \
  up -d --no-deps --force-recreate --no-build bpi-adapter
date -u +%FT%TZ > "$backup_dir/WATCHDOG_RESTORED"
SH
  chmod 700 "$backup_dir/watchdog.sh"
  nohup sh "$backup_dir/watchdog.sh" "$root" "$stream_root" "$backup_dir" "$watchdog_timeout" \
    >"$backup_dir/watchdog.log" 2>&1 </dev/null &
  printf '%s\n' "$!" > "$backup_dir/watchdog.pid"
}

case "$action" in
  precheck)
    test -f "$base_env"
    test -f "$stream_env"
    test -s "$rollback_jar"
    test -s "$load_client_jar"
    docker image inspect "$rollback_service_image" >/dev/null
    docker image inspect "$rollback_adapter_image" >/dev/null
    wait_runtime_healthy "$base_env" bpi-service
    wait_runtime_healthy "$base_env" bpi-adapter
    current_jar=$(env_value BPI_JOB_JAR '' "$stream_env")
    test -s "$current_jar"
    test "$(sha256sum "$current_jar" | awk '{print $1}')" != "$(sha256sum "$rollback_jar" | awk '{print $1}')"
    test "$(sha256sum "$load_client_jar" | awk '{print $1}')" != "$(sha256sum "$rollback_jar" | awk '{print $1}')"
    [ "$(phase2_state | grep -o ':true' | wc -l)" -eq 0 ]
    [ "$(database_state | python3 -c 'import json,sys; print(json.load(sys.stdin)["flywayVersion"])')" -eq "$expected_flyway" ]
    emit_state "$base_env"
    ;;
  snapshot)
    test ! -e "$backup_dir"
    mkdir -p "$backup_dir/evidence"
    chmod 700 "$backup_dir"
    cp "$base_env" "$backup_dir/runtime.env.before"
    cp "$stream_env" "$backup_dir/stream.env.before"
    chmod 600 "$backup_dir/runtime.env.before" "$backup_dir/stream.env.before"
    cp "$base_env" "$current_env"
    cp "$base_env" "$rollback_env"
    chmod 600 "$current_env" "$rollback_env"
    allowlist_updates="BPI_POINT_CATALOG_KAFKA_ENABLED=true BPI_POINT_CATALOG_KAFKA_ALLOWED_TENANT_IDS=$tenant_id BPI_POINT_CATALOG_KAFKA_ALLOWED_PLANT_IDS=$plant_id BPI_POINT_CATALOG_KAFKA_ALLOWED_LINE_IDS=$line_id BPI_RULE_PUBLICATION_OUTBOX_ENABLED=false BPI_RULE_APPLICATION_KAFKA_ENABLED=false BPI_CANDIDATE_KAFKA_ENABLED=true BPI_CANDIDATE_KAFKA_ALLOWED_TENANT_IDS=$tenant_id BPI_CANDIDATE_KAFKA_ALLOWED_PLANT_IDS=$plant_id BPI_CANDIDATE_KAFKA_ALLOWED_LINE_IDS=$line_id BPI_DATA_QUALITY_KAFKA_ENABLED=true BPI_DATA_QUALITY_KAFKA_ALLOWED_TENANT_IDS=$tenant_id BPI_DATA_QUALITY_KAFKA_ALLOWED_PLANT_IDS=$plant_id BPI_DATA_QUALITY_KAFKA_ALLOWED_LINE_IDS=$line_id BPI_PHASE2_INTEGRATION_ENABLED=false BPI_PHASE2_PROTOBUF_HTTP_INGRESS_ENABLED=false BPI_PHASE2_KAFKA_ENABLED=false BPI_WMS_OUTBOX_ENABLED=false BPI_WMS_ADAPTER_ENABLED=false QCS_BPI_OUTBOX_ENABLED=false"
    update_env "$current_env" $allowlist_updates
    update_env "$rollback_env" $allowlist_updates "BPI_SERVICE_IMAGE=$rollback_service_image" "BPI_ADAPTER_IMAGE=$rollback_adapter_image"
    current_jar=$(env_value BPI_JOB_JAR '' "$stream_env")
    printf 'CURRENT_JAR=%s\n' "$current_jar" > "$recovery_env"
    chmod 600 "$recovery_env"
    printf 'currentJar=%s\n' "$current_jar"
    printf 'currentJarSha256=%s\n' "$(sha256sum "$current_jar" | awk '{print $1}')"
    printf 'rollbackJarSha256=%s\n' "$(sha256sum "$rollback_jar" | awk '{print $1}')"
    printf 'loadClientJarSha256=%s\n' "$(sha256sum "$load_client_jar" | awk '{print $1}')"
    ;;
  capture-current)
    current_savepoint=$(capture_savepoint "$backup_dir/current-savepoint.json")
    printf 'CURRENT_SAVEPOINT=%s\n' "$current_savepoint" >> "$recovery_env"
    write_watchdog
    printf 'currentSavepoint=%s\n' "$current_savepoint"
    printf 'watchdogPid=%s\n' "$(cat "$backup_dir/watchdog.pid")"
    ;;
  rollback-flink)
    . "$recovery_env"
    update_stream "$rollback_jar" "$CURRENT_SAVEPOINT"
    restore_stream
    emit_state "$current_env"
    ;;
  rollback-runtime)
    restart_runtime "$rollback_env"
    emit_state "$rollback_env"
    ;;
  replay)
    export BPI_HOST_UID=$(id -u)
    export BPI_HOST_GID=$(id -g)
    export BPI_JOB_JAR="$load_client_jar"
    export BPI_REPLAY_EVIDENCE_DIR="$backup_dir/evidence"
    docker compose --env-file "$stream_env" -f "$stream_compose" --profile acceptance run --rm -T --no-deps \
      -e "BPI_REPLAY_MARKER=$marker" \
      -e "BPI_REPLAY_TENANT_ID=$tenant_id" \
      -e "BPI_REPLAY_PLANT_ID=$plant_id" \
      -e "BPI_REPLAY_LINE_ID=$line_id" \
      -e "BPI_REPLAY_TOPOLOGY_CODE=$topology_code" \
      -e "BPI_REPLAY_TOPOLOGY_VERSION=$topology_version" \
      -e "BPI_REPLAY_RULE_CODE=$rule_code" \
      -e "BPI_REPLAY_RULE_VERSION=$rule_version" \
      -e "BPI_REPLAY_ORDER_ID=$order_id" \
      -e "BPI_REPLAY_PRODUCT_ID=$product_id" \
      -e "BPI_REPLAY_DEVICE_ID=$device_id" \
      -e "BPI_REPLAY_POINT_CATALOG_SOURCE_INSTANCE=BPI-JOINT-$marker" \
      -e "BPI_REPLAY_TIMEOUT_SECONDS=$timeout" \
      -e "BPI_REPLAY_REPORT=/evidence/integrated-replay.json" \
      bpi-cluster-replay >/dev/null
    test "$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["status"])' "$backup_dir/evidence/integrated-replay.json")" = PASS
    printf 'replayReport=%s\n' "$backup_dir/evidence/integrated-replay.json"
    ;;
  capture-rollback)
    rollback_savepoint=$(capture_savepoint "$backup_dir/rollback-savepoint.json")
    printf 'ROLLBACK_SAVEPOINT=%s\n' "$rollback_savepoint" >> "$recovery_env"
    printf 'rollbackSavepoint=%s\n' "$rollback_savepoint"
    ;;
  restore-flink)
    . "$recovery_env"
    update_stream "$CURRENT_JAR" "$ROLLBACK_SAVEPOINT"
    restore_stream
    emit_state "$rollback_env"
    ;;
  restore-runtime)
    restart_runtime "$base_env"
    emit_state "$base_env"
    ;;
  restore-all)
    restart_runtime "$base_env"
    if [ -f "$recovery_env" ]; then
      current_jar=$(sed -n 's/^CURRENT_JAR=//p' "$recovery_env" | tail -1)
      current_savepoint=$(sed -n 's/^CURRENT_SAVEPOINT=//p' "$recovery_env" | tail -1)
      rollback_savepoint=$(sed -n 's/^ROLLBACK_SAVEPOINT=//p' "$recovery_env" | tail -1)
      if [ -n "$current_jar" ] && [ -n "$current_savepoint" ]; then
        restore_point=$current_savepoint
        if [ -n "$rollback_savepoint" ]; then restore_point=$rollback_savepoint; fi
        update_stream "$current_jar" "$restore_point"
        restore_stream
      else
        cp "$backup_dir/stream.env.before" "$stream_env"
      fi
    else
      cp "$backup_dir/stream.env.before" "$stream_env" 2>/dev/null || true
    fi
    emit_state "$base_env"
    ;;
  complete)
    touch "$complete_file"
    if [ -f "$backup_dir/watchdog.pid" ]; then
      kill "$(cat "$backup_dir/watchdog.pid")" 2>/dev/null || true
    fi
    printf 'watchdogRestored=%s\n' "$(if [ -f "$backup_dir/WATCHDOG_RESTORED" ]; then printf true; else printf false; fi)"
    ;;
  state-base)
    emit_state "$base_env"
    ;;
  *) printf 'ERROR: unsupported action: %s\n' "$action" >&2; exit 2 ;;
esac
`;

function remoteArgs(action) {
  return [
    action,
    remoteRoot,
    streamRoot,
    backupDir,
    rollbackServiceImage,
    rollbackAdapterImage,
    rollbackJobJar,
    loadClientJobJar,
    marker,
    tenantId,
    plantId,
    lineId,
    topologyCode,
    topologyVersion,
    ruleCode,
    ruleVersion,
    orderId,
    productId,
    deviceId,
    String(timeoutSeconds),
    String(watchdogSeconds),
    String(expectedFlywayVersion),
  ];
}

function runRemote(action) {
  const result = spawnSync(
    "ssh",
    [
      "-o", "BatchMode=yes",
      "-o", "ConnectTimeout=10",
      sshTarget,
      "sh", "-s", "--",
      ...remoteArgs(action),
    ],
    { input: remoteScript, encoding: "utf8", maxBuffer: 32 * 1024 * 1024 },
  );
  if (result.status !== 0) {
    throw new Error(
      `Remote ${action} failed (${result.status}): ${result.stderr || result.stdout}`.trim(),
    );
  }
  return parseKeyValues(result.stdout);
}

function readRemoteJson(remotePath) {
  assertSafe(remotePath, "remote evidence path", /^\/[A-Za-z0-9._/-]+$/);
  const result = spawnSync(
    "ssh",
    ["-o", "BatchMode=yes", "-o", "ConnectTimeout=10", sshTarget, "cat", remotePath],
    { encoding: "utf8", maxBuffer: 16 * 1024 * 1024 },
  );
  if (result.status !== 0) {
    throw new Error(`Could not read remote evidence ${remotePath}: ${result.stderr}`);
  }
  return JSON.parse(result.stdout);
}

function psqlFile(file, variables) {
  const sql = fs.readFileSync(file, "utf8");
  const variableArgs = Object.entries(variables)
    .map(([key, value]) => `-v ${shellQuote(`${key}=${value}`)}`)
    .join(" ");
  const command = `docker exec -i adp-mes-newbase-postgres-1 psql -X -At -U adp -d ft_mes_bpi -v ON_ERROR_STOP=1 ${variableArgs}`;
  const result = spawnSync(
    "ssh",
    ["-o", "BatchMode=yes", "-o", "ConnectTimeout=10", sshTarget, command],
    { input: sql, encoding: "utf8", maxBuffer: 16 * 1024 * 1024 },
  );
  if (result.status !== 0) {
    throw new Error(`PostgreSQL script ${path.basename(file)} failed: ${result.stderr || result.stdout}`);
  }
  return parseLastJson(result.stdout, path.basename(file));
}

function browserAction(action, output, screenshot, extra = {}) {
  const script = path.join(repoRoot, "deploy/bpi-runtime/scripts/browser-joint-acceptance.js");
  const result = spawnSync(process.execPath, [script], {
    cwd: repoRoot,
    encoding: "utf8",
    maxBuffer: 32 * 1024 * 1024,
    env: {
      ...process.env,
      NODE_PATH: nodePath,
      BPI_BROWSER_ACTION: action,
      BPI_ACCEPTANCE_MARKER: marker,
      BPI_ACCEPTANCE_ORDER_ID: orderId,
      BPI_ACCEPTANCE_LINE_ID: lineId,
      ADP_BASE_URL: adpBaseUrl,
      BPI_BROWSER_BASE_URL: `${adpBaseUrl}/bpi`,
      ADP_USERNAME: username,
      ADP_PASSWORD: password,
      BPI_BROWSER_REPORT: output,
      BPI_BROWSER_SCREENSHOT: screenshot,
      BPI_BROWSER_TIMEOUT_MS: String(timeoutSeconds * 1000),
      ...extra,
    },
  });
  if (result.status !== 0) {
    throw new Error(`Browser ${action} failed: ${result.stderr || result.stdout}`);
  }
  const evidence = JSON.parse(fs.readFileSync(output, "utf8"));
  assert(evidence.status === "PASS", `Browser ${action} evidence is not PASS`);
  return evidence;
}

function waitForPersistence() {
  const verifyFile = path.join(repoRoot, "deploy/bpi-runtime/sql/joint-acceptance-verify.sql");
  const deadline = Date.now() + timeoutSeconds * 1000;
  let latest = null;
  while (Date.now() < deadline) {
    latest = psqlFile(verifyFile, {
      marker,
      tenant_id: tenantId,
      order_id: orderId,
    });
    if (latest?.candidate && latest.counts?.candidates === 1) return latest;
    spawnSync("sleep", ["2"]);
  }
  throw new Error(`Candidate did not persist before timeout: ${JSON.stringify(latest)}`);
}

function cleanMarker() {
  return psqlFile(path.join(repoRoot, "deploy/bpi-runtime/sql/joint-acceptance-cleanup.sql"), {
    marker,
    tenant_id: tenantId,
    plant_id: plantId,
    line_id: lineId,
    order_id: orderId,
  });
}

function parseDatabaseState(stage) {
  return JSON.parse(stage.databaseState || "{}");
}

function stageReady(stage, serviceImage, adapterImage, jarSha256) {
  const job = String(stage.flinkJob || "").split("|");
  const database = parseDatabaseState(stage);
  return stage.serviceHealth === "healthy"
    && stage.adapterHealth === "healthy"
    && stage.serviceImage === serviceImage
    && stage.adapterImage === adapterImage
    && stage.jobJarSha256 === jarSha256
    && job.length === 3
    && Number(job[1]) > 0
    && job[1] === job[2]
    && /^\d+$/.test(String(stage.flinkCheckpoint || ""))
    && database.flywayVersion === expectedFlywayVersion
    && !String(stage.phase2State).includes(":true");
}

const report = {
  schemaVersion: 1,
  generatedAt: new Date().toISOString(),
  repoCommit: gitHead(),
  database: "PostgreSQL",
  marker,
  status: "RUNNING",
  loadClassification: "CONTROLLED_PRODUCTION_EQUIVALENT_NOT_PHYSICAL",
  scope: { tenantId, plantId, lineId, orderId },
  target: {
    sshHost: sshTarget.split("@")[1],
    adpBaseUrl,
    remoteRoot,
    streamRoot,
    backupDir,
  },
  rollbackArtifacts: {
    serviceImage: rollbackServiceImage,
    adapterImage: rollbackAdapterImage,
    flinkJar: rollbackJobJar,
    loadClientJar: loadClientJobJar,
  },
  safety: {
    databaseDowngrade: false,
    destructiveSchemaChange: false,
    phase2ExpectedEnabled: false,
    watchdogSeconds,
    cleanupByMarkerOnly: true,
    publishedRuleIsControlledFixture: true,
    businessMutationStillRequiresBrowserConfirmation: true,
  },
  stages: {},
  browser: {},
  postgres: {},
  kafkaFlink: {},
  checks: [],
  recovery: {},
  issues: [],
};

function check(name, passed, detail) {
  report.checks.push({ name, passed, detail });
  if (!passed) report.issues.push(`${name}: ${JSON.stringify(detail)}`);
}

let snapshotCreated = false;
let markerSeeded = false;
let restored = false;
let cleaned = false;

async function main() {
  fs.mkdirSync(path.dirname(outputPath), { recursive: true });
  for (const screenshot of [rollbackScreenshot, restoredScreenshot, cleanupScreenshot]) {
    fs.mkdirSync(path.dirname(screenshot), { recursive: true });
  }
  const tempDir = fs.mkdtempSync("/tmp/bpi-integrated-rollback-");
  const browserConfirm = path.join(tempDir, "confirm.json");
  const browserBatch = path.join(tempDir, "batch.json");
  const browserCleanup = path.join(tempDir, "cleanup.json");
  const seedFile = path.join(repoRoot, "deploy/bpi-runtime/sql/integrated-rollback-seed.sql");
  const verifyFile = path.join(repoRoot, "deploy/bpi-runtime/sql/joint-acceptance-verify.sql");

  try {
    report.stages.precheck = runRemote("precheck");
    const currentService = report.stages.precheck.serviceImage;
    const currentAdapter = report.stages.precheck.adapterImage;
    const currentJarSha = report.stages.precheck.jobJarSha256;
    check(
      "current base stack ready before rehearsal",
      stageReady(report.stages.precheck, currentService, currentAdapter, currentJarSha),
      report.stages.precheck,
    );
    const preDatabase = parseDatabaseState(report.stages.precheck);
    check("unique marker starts clean", Object.entries(preDatabase)
      .filter(([key]) => key !== "flywayVersion").every(([, value]) => value === 0), preDatabase);

    report.stages.snapshot = runRemote("snapshot");
    snapshotCreated = true;
    report.stages.currentSavepoint = runRemote("capture-current");
    report.stages.rollbackFlink = runRemote("rollback-flink");
    report.stages.rollback = runRemote("rollback-runtime");
    check(
      "rollback stack ready together",
      stageReady(
        report.stages.rollback,
        rollbackServiceImage,
        rollbackAdapterImage,
        report.stages.snapshot.rollbackJarSha256,
      ),
      report.stages.rollback,
    );

    report.postgres.seed = psqlFile(seedFile, {
      marker,
      tenant_id: tenantId,
      plant_id: plantId,
      line_id: lineId,
      topology_code: topologyCode,
      topology_version: topologyVersion,
      rule_code: ruleCode,
      rule_version: ruleVersion,
      order_id: orderId,
      product_id: productId,
      device_id: deviceId,
    });
    markerSeeded = true;
    check(
      "published rule fixture is explicitly scoped to rollback rehearsal",
      report.postgres.seed.status === "SEEDED_PUBLISHED_FIXTURE"
        && report.postgres.seed.fixturePurpose === "CONTROLLED_INTEGRATED_ROLLBACK_ONLY"
        && report.postgres.seed.ruleState === "PUBLISHED",
      report.postgres.seed,
    );

    const replayLocation = runRemote("replay");
    report.kafkaFlink.replay = readRemoteJson(replayLocation.replayReport);
    check(
      "old Flink produced exactly one candidate",
      report.kafkaFlink.replay.status === "PASS"
        && report.kafkaFlink.replay.candidate?.matchingRecordCount === 1
        && report.kafkaFlink.replay.matchingDataQualityIssues === 0
        && report.kafkaFlink.replay.cleanup?.application?.status === "APPLIED",
      report.kafkaFlink.replay,
    );

    report.postgres.rollbackBeforeConfirm = waitForPersistence();
    report.browser.rollbackConfirm = browserAction(
      "confirm",
      browserConfirm,
      rollbackScreenshot,
    );
    report.postgres.rollback = psqlFile(verifyFile, {
      marker,
      tenant_id: tenantId,
      order_id: orderId,
    });
    const batchId = report.postgres.rollback.batch?.id;
    check(
      "old service persisted confirmed shadow batch",
      report.postgres.rollback.candidate?.state === "CONFIRMED"
        && report.postgres.rollback.batch?.state === "ACTIVE"
        && report.postgres.rollback.batch?.is_shadow === true
        && report.postgres.rollback.counts?.candidates === 1
        && report.postgres.rollback.counts?.batches === 1
        && report.postgres.rollback.counts?.boundaryEvidence
          === report.kafkaFlink.replay.candidate?.evidenceEventIds?.length,
      report.postgres.rollback,
    );
    assert(batchId, "Rollback PostgreSQL evidence did not return a batch ID");

    report.stages.rollbackSavepoint = runRemote("capture-rollback");
    report.stages.restoredFlink = runRemote("restore-flink");
    report.stages.restored = runRemote("restore-runtime");
    restored = true;
    check(
      "current stack restored exactly",
      stageReady(report.stages.restored, currentService, currentAdapter, currentJarSha)
        && report.stages.restored.serviceImageId === report.stages.precheck.serviceImageId
        && report.stages.restored.adapterImageId === report.stages.precheck.adapterImageId,
      report.stages.restored,
    );

    report.browser.restoredBatch = browserAction(
      "batch-read",
      browserBatch,
      restoredScreenshot,
      {
        BPI_ACCEPTANCE_BATCH_ID: batchId,
        BPI_ACCEPTANCE_BATCH_STATE: "ACTIVE",
      },
    );
    report.postgres.restored = psqlFile(verifyFile, {
      marker,
      tenant_id: tenantId,
      order_id: orderId,
    });
    const stableProjection = (value) => ({
      candidateId: value.candidate?.id,
      candidateKey: value.candidate?.candidate_key,
      candidateState: value.candidate?.state,
      candidateRevision: value.candidate?.revision,
      batchId: value.batch?.id,
      batchState: value.batch?.state,
      batchRevision: value.batch?.revision,
      counts: value.counts,
    });
    check(
      "marker persistence unchanged across restore",
      JSON.stringify(stableProjection(report.postgres.rollback))
        === JSON.stringify(stableProjection(report.postgres.restored)),
      {
        rollback: stableProjection(report.postgres.rollback),
        restored: stableProjection(report.postgres.restored),
      },
    );

    report.postgres.cleanup = cleanMarker();
    cleaned = true;
    check(
      "marker cleanup has zero residue",
      report.postgres.cleanup.status === "CLEANED"
        && Object.values(report.postgres.cleanup.remaining || {}).every((value) => value === 0),
      report.postgres.cleanup,
    );
    report.browser.cleanup = browserAction(
      "candidate-absent",
      browserCleanup,
      cleanupScreenshot,
    );
    report.stages.final = runRemote("state-base");
    check(
      "final current stack remains ready",
      stageReady(report.stages.final, currentService, currentAdapter, currentJarSha),
      report.stages.final,
    );
    report.recovery = runRemote("complete");
  } catch (error) {
    report.issues.push(error?.stack || error?.message || String(error));
  } finally {
    if (snapshotCreated && !restored) {
      try {
        report.recovery.restoreAll = runRemote("restore-all");
        restored = true;
      } catch (restoreError) {
        report.issues.push(`RESTORE FAILED: ${restoreError?.stack || restoreError}`);
      }
    }
    if (markerSeeded && !cleaned) {
      try {
        report.postgres.recoveryCleanup = cleanMarker();
        cleaned = Object.values(report.postgres.recoveryCleanup.remaining || {})
          .every((value) => value === 0);
      } catch (cleanupError) {
        report.issues.push(`CLEANUP FAILED: ${cleanupError?.stack || cleanupError}`);
      }
    }
    if (snapshotCreated) {
      try {
        report.recovery.watchdog = runRemote("complete");
      } catch (watchdogError) {
        report.issues.push(`WATCHDOG CANCEL FAILED: ${watchdogError?.stack || watchdogError}`);
      }
    }
    report.generatedAt = new Date().toISOString();
    report.status = report.issues.length === 0 && report.checks.every((item) => item.passed)
      ? "PASS_INTEGRATED_ROLLBACK_CONTROLLED_MARKER"
      : "FAIL";
    fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`, "utf8");
    console.log(`BPI integrated rollback rehearsal ${report.status}: ${outputPath}`);
    if (report.status === "FAIL") process.exitCode = 1;
  }
}

if (printRemoteScript) {
  process.stdout.write(remoteScript);
} else if (precheckOnly) {
  process.stdout.write(`${JSON.stringify(runRemote("precheck"), null, 2)}\n`);
} else {
  main().catch((error) => {
    console.error(error?.stack || error);
    process.exitCode = 1;
  });
}
