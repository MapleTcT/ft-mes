#!/usr/bin/env node
"use strict";

const fs = require("node:fs");
const path = require("node:path");
const { spawnSync } = require("node:child_process");
const { chromium, request } = require("playwright");

const repoRoot = path.resolve(__dirname, "../../..");
const sshTarget = process.env.ADP_DB_SSH_TARGET || "v6@100.99.133.43";
const remoteRoot =
  process.env.ADP_REMOTE_DEPLOY_ROOT || "/home/v6/adp-mes-docker-newbase-20260611-181921";
const rollbackServiceImage = required("BPI_ROLLBACK_SERVICE_IMAGE");
const rollbackAdapterImage = required("BPI_ROLLBACK_ADAPTER_IMAGE");
const adpBaseUrl = (process.env.ADP_BASE_URL || "http://100.99.133.43:18080").replace(/\/+$/, "");
const bpiBaseUrl = (process.env.BPI_BROWSER_BASE_URL || `${adpBaseUrl}/bpi`).replace(/\/+$/, "");
const username = required("ADP_USERNAME");
const password = required("ADP_PASSWORD");
const timeoutSeconds = Number(process.env.BPI_RUNTIME_ROLLBACK_TIMEOUT_SECONDS || 240);
const browserTimeoutMs = Number(process.env.BPI_BROWSER_TIMEOUT_MS || 120_000);
const expectedFlywayVersion = Number(process.env.BPI_EXPECTED_FLYWAY_VERSION || 16);
const marker =
  process.env.BPI_RUNTIME_ROLLBACK_MARKER ||
  `ADP_BPI_RUNTIME_ROLLBACK_${new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14)}`;
const backupDir =
  process.env.BPI_RUNTIME_ROLLBACK_BACKUP_DIR ||
  `/data/docker/bpi-upgrade-backups/${marker}`;
const outputPath = path.resolve(
  process.env.BPI_RUNTIME_ROLLBACK_OUTPUT ||
    path.join(repoRoot, "metadata/bpi-runtime-image-rollback-acceptance.json")
);
const screenshotDir = path.resolve(
  process.env.BPI_RUNTIME_ROLLBACK_SCREENSHOT_DIR || "/tmp/bpi-runtime-rollback"
);

if (process.env.BPI_RUNTIME_ROLLBACK_CONFIRM !== "ROLLBACK_BPI_RUNTIME_IMAGES_AND_RESTORE") {
  throw new Error(
    "Set BPI_RUNTIME_ROLLBACK_CONFIRM=ROLLBACK_BPI_RUNTIME_IMAGES_AND_RESTORE"
  );
}
assertSafe(sshTarget, "SSH target", /^[A-Za-z0-9._-]+@[A-Za-z0-9._-]+$/);
assertSafe(remoteRoot, "remote root", /^\/[A-Za-z0-9._/-]+$/);
assertSafe(backupDir, "backup directory", /^\/[A-Za-z0-9._/-]+$/);
assertSafe(marker, "marker", /^[A-Za-z0-9_-]{8,96}$/);
assertSafe(rollbackServiceImage, "service rollback image", /^[A-Za-z0-9._/-]+:[A-Za-z0-9._-]+$/);
assertSafe(rollbackAdapterImage, "adapter rollback image", /^[A-Za-z0-9._/-]+:[A-Za-z0-9._-]+$/);
if (!Number.isInteger(timeoutSeconds) || timeoutSeconds < 30 || timeoutSeconds > 900) {
  throw new Error("BPI_RUNTIME_ROLLBACK_TIMEOUT_SECONDS must be an integer from 30 to 900");
}
if (!Number.isInteger(browserTimeoutMs) || browserTimeoutMs < 10_000 || browserTimeoutMs > 600_000) {
  throw new Error("BPI_BROWSER_TIMEOUT_MS must be an integer from 10000 to 600000");
}
if (!Number.isInteger(expectedFlywayVersion) || expectedFlywayVersion < 1) {
  throw new Error("BPI_EXPECTED_FLYWAY_VERSION must be a positive integer");
}

function required(key) {
  const value = process.env[key];
  if (!value || !value.trim()) throw new Error(`${key} is required`);
  return value.trim();
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

const remoteScript = String.raw`set -eu
action=$1
root=$2
backup_dir=$3
rollback_service_image=$4
rollback_adapter_image=$5
timeout=$6

deploy_dir="$root/deploy/docker"
compose_file="$deploy_dir/docker-compose.yml"
base_env="$deploy_dir/.env"
rollback_env="$backup_dir/rollback.env"

env_value() {
  key=$1
  fallback=$2
  file=$3
  value=$(sed -n "s/^$key=//p" "$file" | tail -1)
  if [ -n "$value" ]; then
    printf '%s' "$value"
  else
    printf '%s' "$fallback"
  fi
}

compose_with() {
  selected_env=$1
  shift
  docker compose --env-file "$selected_env" -f "$compose_file" --profile bpi "$@"
}

container_id() {
  compose_with "$1" ps -q "$2"
}

health() {
  id=$(container_id "$1" "$2")
  if [ -z "$id" ]; then
    printf 'missing'
    return
  fi
  docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$id"
}

wait_healthy() {
  selected_env=$1
  service=$2
  deadline=$(( $(date +%s) + timeout ))
  while [ "$(date +%s)" -lt "$deadline" ]; do
    current=$(health "$selected_env" "$service")
    case "$current" in
      healthy) return 0 ;;
      exited|dead)
        compose_with "$selected_env" logs --tail 120 "$service" >&2 || true
        return 1
        ;;
    esac
    sleep 3
  done
  compose_with "$selected_env" ps "$service" >&2 || true
  compose_with "$selected_env" logs --tail 120 "$service" >&2 || true
  return 1
}

database_state() {
  postgres_user=$(env_value POSTGRES_USER adp "$base_env")
  database_name=$(env_value BPI_DATABASE_NAME ft_mes_bpi "$base_env")
  compose_with "$base_env" exec -T postgres psql -X -At -U "$postgres_user" -d "$database_name" -c \
    "SELECT concat(
       (SELECT max(version::integer) FROM bpi.flyway_schema_history WHERE success), '|',
       (SELECT count(*) FROM bpi.bpi_point_catalog_snapshots), '|',
       (SELECT count(*) FROM bpi.bpi_point_catalog_entries), '|',
       (SELECT count(*) FROM bpi.bpi_topology_versions), '|',
       (SELECT count(*) FROM bpi.bpi_rule_versions), '|',
       (SELECT count(*) FROM bpi.bpi_rule_approval_requests), '|',
       (SELECT count(*) FROM bpi.bpi_batch_candidates), '|',
       (SELECT count(*) FROM bpi.bpi_batch_instances), '|',
       (SELECT count(*) FROM bpi.bpi_batch_state_events), '|',
       (SELECT count(*) FROM bpi.bpi_audit_events));"
}

page_http() {
  http_port=$(env_value ADP_HTTP_PORT 18080 "$base_env")
  curl -sS -o /dev/null -w '%{http_code}' "http://127.0.0.1:$http_port/bpi/" 2>/dev/null || true
}

emit_state() {
  selected_env=$1
  service_id=$(container_id "$selected_env" bpi-service)
  adapter_id=$(container_id "$selected_env" bpi-adapter)
  printf 'serviceImage=%s\n' "$(docker inspect -f '{{.Config.Image}}' "$service_id")"
  printf 'serviceImageId=%s\n' "$(docker inspect -f '{{.Image}}' "$service_id")"
  printf 'serviceHealth=%s\n' "$(health "$selected_env" bpi-service)"
  printf 'adapterImage=%s\n' "$(docker inspect -f '{{.Config.Image}}' "$adapter_id")"
  printf 'adapterImageId=%s\n' "$(docker inspect -f '{{.Image}}' "$adapter_id")"
  printf 'adapterHealth=%s\n' "$(health "$selected_env" bpi-adapter)"
  printf 'databaseState=%s\n' "$(database_state)"
  printf 'pageHttp=%s\n' "$(page_http)"
}

case "$action" in
  precheck)
    test -f "$base_env"
    test -f "$compose_file"
    docker image inspect "$rollback_service_image" >/dev/null
    docker image inspect "$rollback_adapter_image" >/dev/null
    wait_healthy "$base_env" bpi-service
    wait_healthy "$base_env" bpi-adapter
    emit_state "$base_env"
    ;;
  snapshot)
    test ! -e "$backup_dir"
    mkdir -p "$backup_dir"
    chmod 700 "$backup_dir"
    cp "$base_env" "$backup_dir/base.env"
    chmod 600 "$backup_dir/base.env"
    cp "$base_env" "$rollback_env"
    python3 - "$rollback_env" "$rollback_service_image" "$rollback_adapter_image" <<'PY'
import os
import sys
from pathlib import Path

path = Path(sys.argv[1])
updates = {
    "BPI_SERVICE_IMAGE": sys.argv[2],
    "BPI_ADAPTER_IMAGE": sys.argv[3],
}
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
temporary = path.with_suffix(path.suffix + ".tmp")
temporary.write_text("\n".join(result) + "\n", encoding="utf-8")
os.chmod(temporary, 0o600)
temporary.replace(path)
PY
    chmod 600 "$rollback_env"
    ;;
  rollback)
    test -f "$rollback_env"
    compose_with "$rollback_env" up -d --no-deps --force-recreate --no-build bpi-service
    wait_healthy "$rollback_env" bpi-service
    compose_with "$rollback_env" up -d --no-deps --force-recreate --no-build bpi-adapter
    wait_healthy "$rollback_env" bpi-adapter
    emit_state "$rollback_env"
    ;;
  restore)
    test -f "$backup_dir/base.env"
    compose_with "$base_env" up -d --no-deps --force-recreate --no-build bpi-service
    wait_healthy "$base_env" bpi-service
    compose_with "$base_env" up -d --no-deps --force-recreate --no-build bpi-adapter
    wait_healthy "$base_env" bpi-adapter
    emit_state "$base_env"
    ;;
  *)
    printf 'ERROR: unsupported action: %s\n' "$action" >&2
    exit 2
    ;;
esac
`;

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
      backupDir,
      rollbackServiceImage,
      rollbackAdapterImage,
      String(timeoutSeconds),
    ],
    { input: remoteScript, encoding: "utf8", maxBuffer: 20 * 1024 * 1024 }
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

async function readJson(response) {
  const text = await response.text();
  try {
    return { json: JSON.parse(text), text };
  } catch (_error) {
    return { json: null, text };
  }
}

async function login(api) {
  const attempts = [
    { userName: username, password, clientId: "pc_dt" },
    { username, password, clientId: "pc_dt" },
  ];
  const failures = [];
  for (const body of attempts) {
    const response = await api.post(`${adpBaseUrl}/inter-api/auth/login`, {
      data: body,
      headers: { Accept: "application/json", "Content-Type": "application/json;charset=UTF-8" },
      timeout: browserTimeoutMs,
    });
    const parsed = await readJson(response);
    const ticket = response.ok() ? findTicket(parsed.json) : null;
    if (ticket) return { ticket, status: response.status() };
    failures.push(response.status());
  }
  throw new Error(`ADP login failed with statuses ${failures.join(",")}`);
}

async function browserSmoke(phase) {
  const evidence = {
    phase,
    pageUrl: `${bpiBaseUrl}/#/points`,
    loginStatus: null,
    heading: null,
    apiResponses: [],
    consoleErrors: [],
    pageErrors: [],
    requestFailures: [],
    screenshot: path.join(screenshotDir, `${marker}-${phase}.png`),
  };
  fs.mkdirSync(screenshotDir, { recursive: true });
  const api = await request.newContext({ ignoreHTTPSErrors: true });
  let browser;
  try {
    const auth = await login(api);
    evidence.loginStatus = auth.status;
    browser = await chromium.launch({ headless: process.env.BPI_HEADLESS !== "false" });
    const context = await browser.newContext({ viewport: { width: 1600, height: 1000 } });
    const origin = new URL(bpiBaseUrl);
    await context.addCookies([
      {
        name: "suposTicket",
        value: auth.ticket,
        url: origin.origin,
      },
    ]);
    const page = await context.newPage();
    page.on("console", (message) => {
      if (message.type() === "error") evidence.consoleErrors.push(message.text());
    });
    page.on("pageerror", (error) => evidence.pageErrors.push(error.message));
    page.on("requestfailed", (requestValue) => {
      evidence.requestFailures.push({
        method: requestValue.method(),
        url: requestValue.url(),
        error: requestValue.failure()?.errorText || "unknown",
      });
    });
    page.on("response", (response) => {
      if (response.url().includes("/bpi-api/")) {
        evidence.apiResponses.push({
          method: response.request().method(),
          url: response.url().replace(/\?.*$/, ""),
          status: response.status(),
        });
      }
    });
    await page.goto(evidence.pageUrl, { waitUntil: "networkidle", timeout: browserTimeoutMs });
    const heading = page.getByRole("heading", { name: "Point Catalog" });
    const chineseHeading = page.getByRole("heading", { name: "点位目录" });
    if (await chineseHeading.isVisible().catch(() => false)) {
      evidence.heading = "点位目录";
    } else {
      await heading.waitFor({ timeout: browserTimeoutMs });
      evidence.heading = "Point Catalog";
    }
    await page.screenshot({ path: evidence.screenshot, fullPage: true });
    assert(evidence.apiResponses.length > 0, `${phase}: no BPI API response was observed`);
    assert(
      evidence.apiResponses.every((item) => item.status >= 200 && item.status < 300),
      `${phase}: BPI API returned non-2xx status`
    );
    assert(evidence.consoleErrors.length === 0, `${phase}: browser console errors were observed`);
    assert(evidence.pageErrors.length === 0, `${phase}: browser page errors were observed`);
    assert(evidence.requestFailures.length === 0, `${phase}: browser request failures were observed`);
    return evidence;
  } finally {
    if (browser) await browser.close();
    await api.dispose();
  }
}

function stageReady(stage) {
  return (
    stage.serviceHealth === "healthy" &&
    stage.adapterHealth === "healthy" &&
    stage.pageHttp === "200" &&
    (stage.databaseState || "").startsWith(`${expectedFlywayVersion}|`)
  );
}

const report = {
  schemaVersion: 1,
  generatedAt: new Date().toISOString(),
  repoCommit: gitHead(),
  database: "PostgreSQL",
  marker,
  status: "RUNNING",
  target: {
    sshHost: sshTarget.split("@")[1],
    remoteRoot,
    adpBaseUrl,
    bpiBaseUrl,
    backupDir,
    expectedFlywayVersion,
  },
  rollbackImages: {
    service: rollbackServiceImage,
    adapter: rollbackAdapterImage,
  },
  stages: {},
  browser: {},
  checks: [],
  issues: [],
};

function check(name, passed, detail) {
  report.checks.push({ name, passed, detail });
  if (!passed) report.issues.push(`${name}: ${JSON.stringify(detail)}`);
}

let snapshotCreated = false;

async function main() {
  try {
    report.stages.precheck = runRemote("precheck");
    check("precheck runtime ready", stageReady(report.stages.precheck), report.stages.precheck);
    check(
      "rollback service image differs from current",
      report.stages.precheck.serviceImage !== rollbackServiceImage,
      report.stages.precheck.serviceImage
    );
    check(
      "rollback adapter image differs from current",
      report.stages.precheck.adapterImage !== rollbackAdapterImage,
      report.stages.precheck.adapterImage
    );
    if (report.issues.length) throw new Error("BPI runtime rollback precheck failed");

    runRemote("snapshot");
    snapshotCreated = true;
    report.stages.rollback = runRemote("rollback");
    check("rollback runtime ready", stageReady(report.stages.rollback), report.stages.rollback);
    check(
      "rollback service image selected",
      report.stages.rollback.serviceImage === rollbackServiceImage,
      report.stages.rollback.serviceImage
    );
    check(
      "rollback adapter image selected",
      report.stages.rollback.adapterImage === rollbackAdapterImage,
      report.stages.rollback.adapterImage
    );
    check(
      "rollback database state unchanged",
      report.stages.rollback.databaseState === report.stages.precheck.databaseState,
      { before: report.stages.precheck.databaseState, rollback: report.stages.rollback.databaseState }
    );
    if (report.issues.length) throw new Error("BPI rollback image or database verification failed");
    report.browser.rollback = await browserSmoke("rollback");
  } catch (error) {
    report.issues.push(error.stack || error.message);
  } finally {
    if (snapshotCreated) {
      try {
        report.stages.restored = runRemote("restore");
        check("restored runtime ready", stageReady(report.stages.restored), report.stages.restored);
        check(
          "current service image restored",
          report.stages.restored.serviceImage === report.stages.precheck.serviceImage &&
            report.stages.restored.serviceImageId === report.stages.precheck.serviceImageId,
          report.stages.restored.serviceImage
        );
        check(
          "current adapter image restored",
          report.stages.restored.adapterImage === report.stages.precheck.adapterImage &&
            report.stages.restored.adapterImageId === report.stages.precheck.adapterImageId,
          report.stages.restored.adapterImage
        );
        check(
          "restored database state unchanged",
          report.stages.restored.databaseState === report.stages.precheck.databaseState,
          { before: report.stages.precheck.databaseState, restored: report.stages.restored.databaseState }
        );
        report.browser.restored = await browserSmoke("restored");
      } catch (restoreError) {
        report.issues.push(`RESTORE FAILED: ${restoreError.stack || restoreError.message}`);
      }
    }
  }

  report.status =
    report.issues.length === 0 && report.checks.every((item) => item.passed) ? "PASS" : "FAIL";
  fs.mkdirSync(path.dirname(outputPath), { recursive: true });
  fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`);
  console.log(`BPI runtime image rollback rehearsal ${report.status}: ${outputPath}`);
  if (report.status !== "PASS") process.exitCode = 1;
}

main().catch((error) => {
  console.error(error.stack || error.message);
  process.exitCode = 1;
});
