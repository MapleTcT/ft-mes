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
const composeProject = process.env.BPI_TARGET_COMPOSE_PROJECT || "adp-mes-newbase";
const stamp = new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14);
const localJar = path.resolve(process.env.BPI_MATERIAL_WMS_JAR
  || path.join(
    repoRoot,
    "backend/source-modules/material-wms/target/material-wms-0.1.0-SNAPSHOT.jar",
  ));
const targetJar = `${runtimeRoot}/runtime/bap-server/module-Server/material/manual/material-wms.jar`;
const backupDir = process.env.BPI_MATERIAL_WMS_DEPLOY_BACKUP_DIR
  || `/data/docker/bpi-upgrade-backups/material-wms-runtime-${stamp}`;
const reportPath = path.resolve(process.env.BPI_MATERIAL_WMS_DEPLOY_REPORT
  || path.join(repoRoot, "metadata/bpi-material-wms-target-deployment.json"));
const printRemoteScript = process.argv.includes("--print-remote-script");

if (!printRemoteScript && process.env.BPI_MATERIAL_WMS_DEPLOY_CONFIRM
    !== "DEPLOY_BACKUP_RESTART_AND_VERIFY") {
  throw new Error(
    "Set BPI_MATERIAL_WMS_DEPLOY_CONFIRM=DEPLOY_BACKUP_RESTART_AND_VERIFY",
  );
}
for (const [label, value, pattern] of [
  ["SSH target", sshTarget, /^[A-Za-z0-9._-]+@[A-Za-z0-9._-]+$/],
  ["runtime root", runtimeRoot, /^\/[A-Za-z0-9._/-]+$/],
  ["compose project", composeProject, /^[A-Za-z0-9_-]+$/],
  ["target JAR", targetJar, /^\/[A-Za-z0-9._/-]+$/],
  ["backup directory", backupDir, /^\/[A-Za-z0-9._/-]+$/],
]) {
  if (!pattern.test(value)) throw new Error(`Unsafe ${label}: ${value}`);
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

function sha256(file) {
  return crypto.createHash("sha256").update(fs.readFileSync(file)).digest("hex");
}

const stagedJar = `${backupDir}/material-wms.new.jar`;
const previousJar = `${backupDir}/material-wms.previous.jar`;
const remoteScript = String.raw`set -eu
runtime_root=$1
compose_project=$2
target_jar=$3
backup_dir=$4
expected_sha=$5
staged_jar="$backup_dir/material-wms.new.jar"
previous_jar="$backup_dir/material-wms.previous.jar"
compose_dir="$runtime_root/deploy/docker"
env_file="$compose_dir/.env"
compose_file="$compose_dir/docker-compose.yml"
material_container="$compose_project-material-1"
wms_adapter_container="$compose_project-bpi-wms-adapter-1"
restored=false

dc() {
  docker compose --project-name "$compose_project" --env-file "$env_file" \
    -f "$compose_file" --profile bpi "$@"
}

wait_material() {
  deadline=$(( $(date +%s) + 180 ))
  while [ "$(date +%s)" -lt "$deadline" ]; do
    if docker exec "$material_container" bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080' \
        >/dev/null 2>&1; then return 0; fi
    sleep 2
  done
  dc logs --tail 160 material >&2 || true
  return 1
}

restore_previous() {
  if [ -f "$previous_jar" ]; then
    cp -p "$previous_jar" "$target_jar.restore"
    mv -f "$target_jar.restore" "$target_jar"
    dc restart material >/dev/null || true
    wait_material || true
    restored=true
  fi
}

on_error() {
  code=$?
  trap - 0 HUP INT TERM
  set +e
  restore_previous
  printf 'rollbackRestored=%s\n' "$restored" >&2
  exit "$code"
}
trap on_error 0 HUP INT TERM

test -f "$env_file"
test -f "$compose_file"
test -f "$target_jar"
test -s "$staged_jar"
test "$(sha256sum "$staged_jar" | cut -d' ' -f1)" = "$expected_sha"
for key in BPI_PHASE2_INTEGRATION_ENABLED BPI_PHASE2_KAFKA_ENABLED BPI_WMS_OUTBOX_ENABLED BPI_WMS_ADAPTER_ENABLED; do
  value=$(sed -n "s/^$key=//p" "$env_file" | tail -1)
  if [ -z "$value" ]; then value=false; fi
  test "$value" = "false"
done
cp -p "$target_jar" "$previous_jar"
chmod 600 "$previous_jar"
printf 'previousSha256=%s\n' "$(sha256sum "$previous_jar" | cut -d' ' -f1)"
cp -p "$staged_jar" "$target_jar.next"
mv -f "$target_jar.next" "$target_jar"
test "$(sha256sum "$target_jar" | cut -d' ' -f1)" = "$expected_sha"
dc restart material >/dev/null
wait_material
probe=$(docker exec "$wms_adapter_container" wget -qO- --timeout=10 \
  'http://material:8080/material/wms/completion-inbound-reversals/by-idempotency?sourceSystem=BPI&idempotencyKey=deployment-probe')
printf '%s' "$probe" | grep -q 'BPI 完工入库接口认证失败'
printf 'deployedSha256=%s\n' "$(sha256sum "$target_jar" | cut -d' ' -f1)"
printf 'routeProbe=PASS_AUTH_GATE\n'
trap - 0 HUP INT TERM
`;

if (printRemoteScript) {
  process.stdout.write(remoteScript);
  process.exit(0);
}

if (!fs.existsSync(localJar) || fs.statSync(localJar).size < 1024) {
  throw new Error(`Packaged material-wms JAR is missing or empty: ${localJar}`);
}
const localSha256 = sha256(localJar);

const report = {
  schemaVersion: 1,
  generatedAt: new Date().toISOString(),
  status: "RUNNING",
  target: {
    host: sshTarget.split("@")[1],
    runtimeRoot,
    composeProject,
    jar: targetJar,
  },
  artifact: {
    localPath: path.relative(repoRoot, localJar),
    sha256: localSha256,
    sizeBytes: fs.statSync(localJar).size,
  },
  backup: { directory: backupDir, previousJar, previousSha256: null },
  deployedSha256: null,
  routeProbe: null,
  rollbackRestored: false,
  issues: [],
};

function parseKeyValues(output) {
  const values = {};
  for (const line of String(output || "").split(/\r?\n/)) {
    const separator = line.indexOf("=");
    if (separator > 0) values[line.slice(0, separator)] = line.slice(separator + 1);
  }
  return values;
}

try {
  fs.mkdirSync(path.dirname(reportPath), { recursive: true });
  ssh(`test ! -e ${shellQuote(backupDir)} && mkdir -p ${shellQuote(backupDir)} && chmod 700 ${shellQuote(backupDir)}`);
  run("scp", [
    "-q", "-o", "BatchMode=yes", "-o", "ConnectTimeout=8",
    localJar, `${sshTarget}:${stagedJar}`,
  ]);
  ssh(`chmod 600 ${shellQuote(stagedJar)}`);
  const result = run("ssh", [
    "-o", "BatchMode=yes", "-o", "ConnectTimeout=8",
    "-o", "ServerAliveInterval=15", "-o", "ServerAliveCountMax=4",
    sshTarget, "sh", "-s", "--",
    runtimeRoot, composeProject, targetJar, backupDir, localSha256,
  ], { input: remoteScript });
  const values = parseKeyValues(result);
  report.backup.previousSha256 = values.previousSha256 || null;
  report.deployedSha256 = values.deployedSha256 || null;
  report.routeProbe = values.routeProbe || null;
  if (report.deployedSha256 !== localSha256 || report.routeProbe !== "PASS_AUTH_GATE") {
    throw new Error(`Target verification mismatch: ${JSON.stringify(values)}`);
  }
  report.status = "PASS_BACKUP_RESTART_VERIFIED";
} catch (error) {
  report.status = "FAIL";
  report.rollbackRestored = /rollbackRestored=true/.test(error?.message || "");
  report.issues.push(error?.message || String(error));
  process.exitCode = 1;
} finally {
  report.generatedAt = new Date().toISOString();
  fs.writeFileSync(reportPath, `${JSON.stringify(report, null, 2)}\n`, "utf8");
  process.stdout.write(`${reportPath}\n`);
}
