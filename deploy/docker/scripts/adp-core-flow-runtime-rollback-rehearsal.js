#!/usr/bin/env node
"use strict";

const crypto = require("crypto");
const fs = require("fs");
const path = require("path");
const { spawnSync } = require("child_process");

const repoRoot = path.resolve(__dirname, "../../..");
const sshTarget = process.env.ADP_DB_SSH_TARGET || "v6@100.99.133.43";
const remoteRoot =
  process.env.ADP_REMOTE_DEPLOY_ROOT || "/home/v6/adp-mes-docker-newbase-20260611-181921";
const backupTag = process.env.ADP_CORE_FLOW_BACKUP_TAG || "20260710-coreflow";
const womContainer = process.env.ADP_WOM_CONTAINER || "adp-mes-newbase-WOMMs-1";
const nacosContainer = process.env.ADP_NACOS_CONTAINER || "adp-mes-newbase-nacos-1";
const httpPort = process.env.ADP_HTTP_PORT || "18080";
const timeoutSeconds = Number(process.env.ADP_ROLLBACK_WAIT_SECONDS || 180);
const generatedAt = new Date();
const marker =
  process.env.ADP_E2E_MARKER ||
  `ADP_E2E_${generatedAt.toISOString().replace(/[-:T.Z]/g, "").slice(0, 14)}_CORE_RUNTIME_ROLLBACK`;
const outputPath = path.resolve(
  process.env.ADP_CORE_FLOW_ROLLBACK_OUTPUT ||
    path.join(repoRoot, "metadata/core-flow-runtime-rollback-rehearsal.json")
);

const artifacts = [
  {
    key: "womJar",
    relative: "runtime/bap-server/module-Server/WOMMs/manual/WOMMs-1.0.0.jar",
  },
  {
    key: "bodyJs",
    relative:
      "deploy/docker/assets/module-static/WOM/produceTask/produceTask/makeTaskList/body.js",
    local:
      "deploy/docker/assets/module-static/WOM/produceTask/produceTask/makeTaskList/body.js",
  },
  {
    key: "bodyEs5Js",
    relative:
      "deploy/docker/assets/module-static/WOM/produceTask/produceTask/makeTaskList/body-es5.js",
    local:
      "deploy/docker/assets/module-static/WOM/produceTask/produceTask/makeTaskList/body-es5.js",
  },
  {
    key: "i18nValueJs",
    relative:
      "deploy/docker/assets/module-static/WOM/produceTask/produceTask/makeTaskList/i18n-value.js",
    local:
      "deploy/docker/assets/module-static/WOM/produceTask/produceTask/makeTaskList/i18n-value.js",
  },
];

function assertSafe(value, label, pattern) {
  if (!pattern.test(value)) {
    throw new Error(`Unsafe ${label}: ${value}`);
  }
}

assertSafe(sshTarget, "SSH target", /^[A-Za-z0-9._-]+@[A-Za-z0-9._-]+$/);
assertSafe(remoteRoot, "remote root", /^\/[A-Za-z0-9._/-]+$/);
assertSafe(backupTag, "backup tag", /^[A-Za-z0-9._-]+$/);
assertSafe(marker, "marker", /^[A-Za-z0-9._-]+$/);
assertSafe(womContainer, "WOM container", /^[A-Za-z0-9._-]+$/);
assertSafe(nacosContainer, "Nacos container", /^[A-Za-z0-9._-]+$/);
assertSafe(httpPort, "HTTP port", /^[0-9]+$/);
if (!Number.isInteger(timeoutSeconds) || timeoutSeconds < 30 || timeoutSeconds > 600) {
  throw new Error(`ADP_ROLLBACK_WAIT_SECONDS must be an integer from 30 to 600: ${timeoutSeconds}`);
}

const remoteScript = String.raw`set -eu
action=$1
root=$2
tag=$3
marker=$4
timeout=$5
wom_container=$6
nacos_container=$7
http_port=$8

jar_rel='runtime/bap-server/module-Server/WOMMs/manual/WOMMs-1.0.0.jar'
body_rel='deploy/docker/assets/module-static/WOM/produceTask/produceTask/makeTaskList/body.js'
body_es5_rel='deploy/docker/assets/module-static/WOM/produceTask/produceTask/makeTaskList/body-es5.js'
i18n_rel='deploy/docker/assets/module-static/WOM/produceTask/produceTask/makeTaskList/i18n-value.js'
snapshot_root="$root/.adp-core-flow-rollback-rehearsal/$marker"

sha() {
  sha256sum "$1" | awk '{print $1}'
}

atomic_copy() {
  src=$1
  dst=$2
  tmp="$dst.tmp-$marker"
  cp -p "$src" "$tmp"
  mv "$tmp" "$dst"
}

healthy_count() {
  docker exec "$nacos_container" curl -fsS \
    "http://127.0.0.1:8848/nacos/v1/ns/instance/list?serviceName=WOMMs&groupName=prod" \
    2>/dev/null | grep -o '"healthy":true' | wc -l | tr -d ' '
}

page_http() {
  curl -sS -o /dev/null -w '%{http_code}' \
    "http://127.0.0.1:$http_port/msService/WOM/produceTask/produceTask/makeTaskList" \
    2>/dev/null || true
}

boot_log_ready() {
  started_at=$(docker inspect "$wom_container" --format '{{.State.StartedAt}}' 2>/dev/null || true)
  if [ -n "$started_at" ] && docker logs --since "$started_at" "$wom_container" 2>&1 \
    | grep -q 'Started WOMMsApplication'; then
    printf 'true'
  else
    printf 'false'
  fi
}

wait_runtime() {
  elapsed=0
  while [ "$elapsed" -lt "$timeout" ]; do
    running=$(docker inspect "$wom_container" --format '{{.State.Running}}' 2>/dev/null || true)
    booted=$(boot_log_ready)
    healthy=$(healthy_count 2>/dev/null || printf '0')
    page=$(page_http)
    if [ "$running" = 'true' ] && [ "$booted" = 'true' ] \
      && [ "$healthy" -ge 1 ] && [ "$page" = '200' ]; then
      return 0
    fi
    sleep 3
    elapsed=$((elapsed + 3))
  done
  docker logs --tail 80 "$wom_container" >&2 || true
  return 1
}

check_files() {
  for rel in "$jar_rel" "$body_rel" "$body_es5_rel" "$i18n_rel"; do
    test -f "$root/$rel"
    test -f "$root/$rel.bak-$tag"
  done
}

emit_state() {
  printf 'womJarHash=%s\n' "$(sha "$root/$jar_rel")"
  printf 'womJarBackupHash=%s\n' "$(sha "$root/$jar_rel.bak-$tag")"
  printf 'bodyJsHash=%s\n' "$(sha "$root/$body_rel")"
  printf 'bodyJsBackupHash=%s\n' "$(sha "$root/$body_rel.bak-$tag")"
  printf 'bodyEs5JsHash=%s\n' "$(sha "$root/$body_es5_rel")"
  printf 'bodyEs5JsBackupHash=%s\n' "$(sha "$root/$body_es5_rel.bak-$tag")"
  printf 'i18nValueJsHash=%s\n' "$(sha "$root/$i18n_rel")"
  printf 'i18nValueJsBackupHash=%s\n' "$(sha "$root/$i18n_rel.bak-$tag")"
  printf 'womRunning=%s\n' "$(docker inspect "$wom_container" --format '{{.State.Running}}')"
  printf 'womStatus=%s\n' "$(docker inspect "$wom_container" --format '{{.State.Status}}')"
  printf 'womStartedAt=%s\n' "$(docker inspect "$wom_container" --format '{{.State.StartedAt}}')"
  printf 'bootLogReady=%s\n' "$(boot_log_ready)"
  printf 'nacosHealthyCount=%s\n' "$(healthy_count)"
  printf 'pageHttp=%s\n' "$(page_http)"
}

case "$action" in
  precheck)
    check_files
    wait_runtime
    emit_state
    ;;
  snapshot)
    check_files
    test ! -e "$snapshot_root"
    mkdir -p "$snapshot_root"
    for rel in "$jar_rel" "$body_rel" "$body_es5_rel" "$i18n_rel"; do
      name=$(basename "$rel")
      cp -p "$root/$rel" "$snapshot_root/$name"
    done
    emit_state
    ;;
  rollback)
    test -d "$snapshot_root"
    for rel in "$jar_rel" "$body_rel" "$body_es5_rel" "$i18n_rel"; do
      atomic_copy "$root/$rel.bak-$tag" "$root/$rel"
    done
    docker restart "$wom_container" >/dev/null
    wait_runtime
    emit_state
    ;;
  restore)
    test -d "$snapshot_root"
    for rel in "$jar_rel" "$body_rel" "$body_es5_rel" "$i18n_rel"; do
      name=$(basename "$rel")
      atomic_copy "$snapshot_root/$name" "$root/$rel"
    done
    docker restart "$wom_container" >/dev/null
    wait_runtime
    emit_state
    rm -f \
      "$snapshot_root/WOMMs-1.0.0.jar" \
      "$snapshot_root/body.js" \
      "$snapshot_root/body-es5.js" \
      "$snapshot_root/i18n-value.js"
    rmdir "$snapshot_root"
    rmdir "$root/.adp-core-flow-rollback-rehearsal" 2>/dev/null || true
    ;;
  *)
    printf 'unsupported action: %s\n' "$action" >&2
    exit 2
    ;;
esac
`;

function gitHead() {
  const result = spawnSync("git", ["rev-parse", "HEAD"], {
    cwd: repoRoot,
    encoding: "utf8",
  });
  return result.status === 0 ? result.stdout.trim() : "unknown";
}

function localSha(relativePath) {
  const content = fs.readFileSync(path.join(repoRoot, relativePath));
  return crypto.createHash("sha256").update(content).digest("hex");
}

function runRemote(action) {
  const result = spawnSync(
    "ssh",
    [
      "-o",
      "BatchMode=yes",
      "-o",
      "ConnectTimeout=10",
      sshTarget,
      "sh",
      "-s",
      "--",
      action,
      remoteRoot,
      backupTag,
      marker,
      String(timeoutSeconds),
      womContainer,
      nacosContainer,
      httpPort,
    ],
    { input: remoteScript, encoding: "utf8", maxBuffer: 10 * 1024 * 1024 }
  );
  if (result.status !== 0) {
    throw new Error(
      `Remote ${action} failed (${result.status}): ${result.stderr || result.stdout}`.trim()
    );
  }
  const values = {};
  for (const line of result.stdout.split(/\r?\n/)) {
    const separator = line.indexOf("=");
    if (separator <= 0) continue;
    values[line.slice(0, separator)] = line.slice(separator + 1);
  }
  return values;
}

function stageReady(stage) {
  return (
    stage.womRunning === "true" &&
    stage.womStatus === "running" &&
    stage.bootLogReady === "true" &&
    Number(stage.nacosHealthyCount) >= 1 &&
    stage.pageHttp === "200"
  );
}

function hashesMatch(stage, suffix) {
  return artifacts.every((artifact) => stage[`${artifact.key}Hash`] === stage[`${artifact.key}${suffix}`]);
}

const report = {
  schemaVersion: 1,
  generatedAt: generatedAt.toISOString(),
  repoCommit: gitHead(),
  database: "PostgreSQL",
  target: {
    sshHost: sshTarget.split("@")[1],
    remoteRoot,
    womContainer,
    nacosContainer,
    httpPort: Number(httpPort),
  },
  marker,
  backupTag,
  status: "RUNNING",
  localStaticHashes: Object.fromEntries(
    artifacts.filter((artifact) => artifact.local).map((artifact) => [artifact.key, localSha(artifact.local)])
  ),
  stages: {},
  checks: [],
  issues: [],
};

function check(name, passed, detail) {
  report.checks.push({ name, passed, detail });
  if (!passed) report.issues.push(`${name}: ${detail}`);
}

let snapshotCreated = false;
try {
  report.stages.precheck = runRemote("precheck");
  check("precheck runtime ready", stageReady(report.stages.precheck), report.stages.precheck);
  for (const artifact of artifacts.filter((item) => item.local)) {
    check(
      `${artifact.key} matches repository`,
      report.stages.precheck[`${artifact.key}Hash`] === report.localStaticHashes[artifact.key],
      report.stages.precheck[`${artifact.key}Hash`]
    );
  }
  check(
    "patched runtime differs from rollback backup",
    artifacts.every(
      (artifact) =>
        report.stages.precheck[`${artifact.key}Hash`] !==
        report.stages.precheck[`${artifact.key}BackupHash`]
    ),
    "Every patched artifact must differ from its pre-coreflow backup."
  );

  report.stages.snapshot = runRemote("snapshot");
  snapshotCreated = true;
  report.stages.rollback = runRemote("rollback");
  check("rollback runtime ready", stageReady(report.stages.rollback), report.stages.rollback);
  check(
    "rollback hashes match backups",
    hashesMatch(report.stages.rollback, "BackupHash"),
    report.stages.rollback
  );
} catch (error) {
  report.issues.push(error.stack || error.message);
} finally {
  if (snapshotCreated) {
    try {
      report.stages.restore = runRemote("restore");
      check("restored runtime ready", stageReady(report.stages.restore), report.stages.restore);
      check(
        "restored hashes match precheck",
        artifacts.every(
          (artifact) =>
            report.stages.restore[`${artifact.key}Hash`] ===
            report.stages.precheck[`${artifact.key}Hash`]
        ),
        report.stages.restore
      );
    } catch (restoreError) {
      report.issues.push(`RESTORE FAILED: ${restoreError.stack || restoreError.message}`);
    }
  }
}

report.status = report.issues.length === 0 && report.checks.every((item) => item.passed) ? "PASS" : "FAIL";
fs.mkdirSync(path.dirname(outputPath), { recursive: true });
fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`);
console.log(`Core-flow runtime rollback rehearsal ${report.status}: ${outputPath}`);
if (report.status !== "PASS") process.exitCode = 1;
