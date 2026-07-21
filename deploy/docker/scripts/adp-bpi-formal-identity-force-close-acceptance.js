#!/usr/bin/env node
"use strict";

const crypto = require("node:crypto");
const fs = require("node:fs");
const path = require("node:path");
const { spawn, spawnSync } = require("node:child_process");
const { chromium, request } = require("playwright");

const repoRoot = path.resolve(__dirname, "../../..");
const stamp = new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14);
const marker = process.env.BPI_FORMAL_IDENTITY_MARKER || `ADP_BPI_FORMAL_IDENTITY_${stamp}`;
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
const batchId = process.env.BPI_FORMAL_IDENTITY_BATCH_ID || crypto.randomUUID();
const commandsFlagId = process.env.BPI_FORMAL_IDENTITY_FLAG_ID || crypto.randomUUID();
const boundaryTime = process.env.BPI_FORMAL_IDENTITY_BOUNDARY_TIME
  || new Date(Date.now() - 5 * 60_000).toISOString().replace(/\.\d{3}Z$/, "Z");
const timeoutSeconds = positiveInteger("BPI_FORMAL_IDENTITY_TIMEOUT_SECONDS", 180, 60, 600);
const watchdogSeconds = positiveInteger("BPI_FORMAL_IDENTITY_WATCHDOG_SECONDS", 900, 300, 3600);
const backupDir = process.env.BPI_FORMAL_IDENTITY_BACKUP_DIR
  || `/data/docker/bpi-upgrade-backups/${marker}`;
const outputPath = path.resolve(process.env.BPI_FORMAL_IDENTITY_OUTPUT
  || path.join(repoRoot, "metadata/bpi-formal-identity-force-close-acceptance.json"));
const pendingScreenshot = path.resolve(process.env.BPI_FORMAL_IDENTITY_PENDING_SCREENSHOT
  || path.join(repoRoot, "metadata/bpi-formal-identity-force-close-pending.png"));
const completedScreenshot = path.resolve(process.env.BPI_FORMAL_IDENTITY_COMPLETED_SCREENSHOT
  || path.join(repoRoot, "metadata/bpi-formal-identity-force-close-completed.png"));
const nodePath = process.env.NODE_PATH || path.join(repoRoot, "frontend/apps/bpi/node_modules");
const printRemoteScript = process.argv.includes("--print-remote-script");

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
]) {
  if (!pattern.test(value)) throw new Error(`Unsafe ${label}: ${value}`);
}
if (!Number.isInteger(mainPositionId) || mainPositionId <= 0) {
  throw new Error("BPI_FORMAL_APPROVER_POSITION_ID must be a positive integer");
}
if (reviewerUsername === adminUsername) {
  throw new Error("Formal approver username must differ from the requester username");
}
if (!/^[0-9a-f-]{36}$/i.test(batchId) || !/^[0-9a-f-]{36}$/i.test(commandsFlagId)) {
  throw new Error("BPI formal identity batch and flag IDs must be UUIDs");
}
if (Number.isNaN(new Date(boundaryTime).getTime())) {
  throw new Error("BPI_FORMAL_IDENTITY_BOUNDARY_TIME must be an ISO timestamp");
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

runtime_dir="$root/deploy/docker"
compose_file="$runtime_dir/docker-compose.yml"
base_env="$runtime_dir/.env"
acceptance_env="$backup_dir/formal-identity.env"
acceptance_compose="$backup_dir/formal-identity.compose.yml"
complete_file="$backup_dir/COMPLETE"

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
  id=$(dc "$selected_env" ps -q bpi-adapter)
  if [ -z "$id" ]; then printf missing; return; fi
  docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$id"
}

wait_healthy() {
  selected_env=$1
  deadline=$(( $(date +%s) + timeout ))
  while [ "$(date +%s)" -lt "$deadline" ]; do
    state=$(health "$selected_env")
    case "$state" in
      healthy) return 0 ;;
      exited|dead|missing) dc "$selected_env" logs --tail 120 bpi-adapter >&2 || true; return 1 ;;
    esac
    sleep 2
  done
  dc "$selected_env" logs --tail 120 bpi-adapter >&2 || true
  return 1
}

adapter_state() {
  selected_env=$1
  id=$(dc "$selected_env" ps -q bpi-adapter)
  printf 'adapterImage=%s\n' "$(docker inspect -f '{{.Config.Image}}' "$id")"
  printf 'adapterImageId=%s\n' "$(docker inspect -f '{{.Image}}' "$id")"
  printf 'adapterHealth=%s\n' "$(health "$selected_env")"
  printf 'adapterSubjectScopeRules=%s\n' "$(docker inspect -f '{{range .Config.Env}}{{println .}}{{end}}' "$id" | sed -n 's/^BPI_ADAPTER_SUBJECT_SCOPE_RULES=//p' | tail -1)"
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
sleep "$delay"
if [ -f "$backup_dir/COMPLETE" ]; then exit 0; fi
runtime_dir="$root/deploy/docker"
docker compose --env-file "$runtime_dir/.env" -f "$runtime_dir/docker-compose.yml" --profile bpi \
  up -d --no-deps --force-recreate --no-build bpi-adapter
date -u +%FT%TZ > "$backup_dir/WATCHDOG_RESTORED"
SH
  chmod 700 "$backup_dir/watchdog.sh"
  nohup sh "$backup_dir/watchdog.sh" "$root" "$backup_dir" "$watchdog_timeout" \
    >"$backup_dir/watchdog.log" 2>&1 </dev/null &
  printf '%s\n' "$!" > "$backup_dir/watchdog.pid"
}

case "$action" in
  precheck)
    test -f "$base_env"
    wait_healthy "$base_env"
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
    python3 - "$acceptance_compose" "$updated_scope" <<'PY'
import os
import sys
from pathlib import Path

path = Path(sys.argv[1])
value = sys.argv[2]
escaped = value.replace("\\", "\\\\").replace('"', '\\"')
path.write_text(
    "services:\n"
    "  bpi-adapter:\n"
    "    environment:\n"
    f'      BPI_ADAPTER_SUBJECT_SCOPE_RULES: "{escaped}"\n',
    encoding="utf-8",
)
os.chmod(path, 0o600)
PY
    before_id=$(dc "$base_env" ps -q bpi-adapter | xargs docker inspect -f '{{.Image}}')
    printf '%s\n' "$before_id" > "$backup_dir/adapter-image-id.before"
    write_watchdog
    dc "$acceptance_env" up -d --no-deps --force-recreate --no-build bpi-adapter
    wait_healthy "$acceptance_env"
    after_id=$(dc "$acceptance_env" ps -q bpi-adapter | xargs docker inspect -f '{{.Image}}')
    test "$before_id" = "$after_id"
    adapter_state "$acceptance_env"
    printf 'phase2State=%s\n' "$(phase2_state)"
    printf 'watchdogPid=%s\n' "$(cat "$backup_dir/watchdog.pid")"
    ;;
  restore)
    dc "$base_env" up -d --no-deps --force-recreate --no-build bpi-adapter
    wait_healthy "$base_env"
    if [ -f "$backup_dir/adapter-image-id.before" ]; then
      current_id=$(dc "$base_env" ps -q bpi-adapter | xargs docker inspect -f '{{.Image}}')
      test "$current_id" = "$(cat "$backup_dir/adapter-image-id.before")"
    fi
    adapter_state "$base_env"
    printf 'phase2State=%s\n' "$(phase2_state)"
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
  const sql = `
SELECT json_build_object(
  'batchRows', (SELECT count(*) FROM bpi.bpi_batch_instances WHERE id = ${sqlLiteral(batchId)}::uuid),
  'taskRows', (SELECT count(*) FROM bpi.bpi_batch_force_close_tasks WHERE batch_id = ${sqlLiteral(batchId)}::uuid),
  'commandsFlagRows', (SELECT count(*) FROM bpi.bpi_feature_flags WHERE id = ${sqlLiteral(commandsFlagId)}::uuid),
  'plantCommandsOverrides', (SELECT count(*) FROM bpi.bpi_feature_flags
                              WHERE tenant_id = ${sqlLiteral(tenantId)} AND scope_type = 'PLANT'
                                AND scope_key = ${sqlLiteral(plantId)} AND flag_key = 'bpi.commands')
);`;
  return psqlJson("ft_mes_bpi", sql, "BPI marker state");
}

async function waitForFile(file, child) {
  const deadline = Date.now() + timeoutSeconds * 1000;
  while (Date.now() < deadline) {
    if (fs.existsSync(file)) return;
    if (child.exitCode !== null) throw new Error(`Force-close browser exited before pending gate (${child.exitCode})`);
    await new Promise((resolve) => setTimeout(resolve, 250));
  }
  throw new Error(`Timed out waiting for pending evidence gate: ${file}`);
}

function waitForChild(child, stdout, stderr) {
  return new Promise((resolve, reject) => {
    child.on("error", reject);
    child.on("close", (code) => {
      if (code === 0) resolve();
      else reject(new Error(`Force-close browser failed (${code}): ${stderr.value || stdout.value}`));
    });
  });
}

async function runForceCloseBrowser(tempDir) {
  const script = path.join(repoRoot, "deploy/docker/scripts/adp-bpi-force-close-acceptance.js");
  const browserReportPath = path.join(tempDir, "force-close-browser.json");
  const screenshotPrefix = path.join(tempDir, "force-close");
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
      BPI_BATCH_ID: batchId,
      BPI_BOUNDARY_TIME: boundaryTime,
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
      path.join(repoRoot, "deploy/docker/scripts/bpi-force-close-acceptance-verification.sql"),
      { marker, batch_id: batchId },
    ), "pending force-close verification");
    fs.writeFileSync(continueFile, `${new Date().toISOString()}\n`, "utf8");
    const result = await completion;
    if (result.error) throw result.error;
    fs.copyFileSync(`${screenshotPrefix}-pending.png`, pendingScreenshot);
    fs.copyFileSync(`${screenshotPrefix}-completed.png`, completedScreenshot);
    return {
      pending,
      browser: JSON.parse(fs.readFileSync(browserReportPath, "utf8")),
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

const report = {
  schemaVersion: 1,
  generatedAt: new Date().toISOString(),
  repoCommit: gitHead(),
  database: "PostgreSQL",
  marker,
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
    phase2ExpectedEnabled: false,
    cleanupByMarkerAndIdentityOnly: true,
  },
  stages: {},
  operations: {},
  browser: null,
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
let scopeActivated = false;
let fixtureSeeded = false;

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
  fs.mkdirSync(path.dirname(completedScreenshot), { recursive: true });
  const tempDir = fs.mkdtempSync("/tmp/bpi-formal-identity-");

  report.stages.adapterPrecheck = runRemote("precheck");
  report.postgres.identityPrecheck = identityPrecheck();
  report.postgres.bpiPrecheck = bpiMarkerState();
  check("formal role and position exist",
    Number(report.postgres.identityPrecheck.roleId) > 0
      && Number(report.postgres.identityPrecheck.positionId) === mainPositionId,
    report.postgres.identityPrecheck);
  check("formal identity marker starts clean",
    Number(report.postgres.identityPrecheck.activeUsers) === 0
      && Number(report.postgres.identityPrecheck.activePersons) === 0,
    report.postgres.identityPrecheck);
  check("BPI marker and plant command override start clean",
    Object.values(report.postgres.bpiPrecheck).every((value) => Number(value) === 0),
    report.postgres.bpiPrecheck);
  check("Phase 2 and write-back switches start disabled",
    !String(report.stages.adapterPrecheck.phase2State).includes(":true"),
    report.stages.adapterPrecheck.phase2State);
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
  const reviewerAuth = await retryLogin(reviewerApi, reviewerUsername, reviewerPassword, "formal approver");
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
    report.stages.adapterActivated.adapterImageId === report.stages.adapterPrecheck.adapterImageId,
    {
      before: report.stages.adapterPrecheck.adapterImageId,
      active: report.stages.adapterActivated.adapterImageId,
    });

  report.postgres.fixture = parseLastJson(psqlFile(
    path.join(repoRoot, "deploy/docker/scripts/bpi-force-close-acceptance-fixture.sql"),
    { marker, batch_id: batchId, commands_flag_id: commandsFlagId },
  ), "force-close fixture");
  fixtureSeeded = true;
  check("controlled shadow batch fixture is active",
    Number(report.postgres.fixture.batchRows) === 1
      && report.postgres.fixture.commandsEnabled === true
      && report.postgres.fixture.initialState === "ACTIVE"
      && Number(report.postgres.fixture.initialRevision) === 1,
    report.postgres.fixture);

  const browserEvidence = await runForceCloseBrowser(tempDir);
  report.postgres.pending = browserEvidence.pending;
  report.browser = browserEvidence.browser;
  check("requester session persisted a pending approval without closing the batch",
    report.postgres.pending.batch?.state === "ACTIVE"
      && Number(report.postgres.pending.batch?.revision) === 2
      && report.postgres.pending.forceCloseTask?.state === "PENDING_APPROVAL"
      && report.postgres.pending.forceCloseTask?.requestedBy === `legacy-ticket:${adminUsername}`
      && !report.postgres.pending.forceCloseTask?.decidedBy,
    report.postgres.pending);
  check("formal approver used a separate ADP browser session through the adapter",
    report.browser.status === "PASS"
      && report.browser.approvalAuthentication === "ADP_SESSION"
      && report.browser.approverLoginStatus === 200
      && report.browser.operations?.approval?.url?.includes("/bpi-api/")
      && report.browser.operations?.approval?.response?.decidedBy === `legacy-ticket:${reviewerUsername}`,
    {
      status: report.browser.status,
      approvalAuthentication: report.browser.approvalAuthentication,
      approverLoginStatus: report.browser.approverLoginStatus,
      approval: report.browser.operations?.approval,
    });

  report.postgres.completed = parseLastJson(psqlFile(
    path.join(repoRoot, "deploy/docker/scripts/bpi-force-close-acceptance-verification.sql"),
    { marker, batch_id: batchId },
  ), "completed force-close verification");
  check("PostgreSQL records distinct formal requester and approver identities",
    report.postgres.completed.batch?.state === "CLOSED_RAW"
      && Number(report.postgres.completed.batch?.revision) === 3
      && report.postgres.completed.forceCloseTask?.state === "COMPLETED"
      && report.postgres.completed.forceCloseTask?.requestedBy === `legacy-ticket:${adminUsername}`
      && report.postgres.completed.forceCloseTask?.decidedBy === `legacy-ticket:${reviewerUsername}`
      && report.postgres.completed.forceCloseTask?.requestedBy
        !== report.postgres.completed.forceCloseTask?.decidedBy,
    report.postgres.completed);

  assert(report.issues.length === 0, `Formal identity acceptance failed: ${report.issues.join("; ")}`);
  report.status = "PASS_TARGET_FORMAL_IDENTITY_TWO_BROWSER_SESSIONS";
}

async function finish() {
  const cleanupErrors = [];
  if (fixtureSeeded) {
    try {
      const output = psqlFile(
        path.join(repoRoot, "deploy/docker/scripts/bpi-force-close-acceptance-cleanup.sql"),
        { marker, batch_id: batchId, commands_flag_id: commandsFlagId },
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
    report.postgres.bpiFinal = bpiMarkerState();
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
    if (report.stages.adapterRestored) {
      check("adapter scope and image restored exactly",
        report.stages.adapterRestored.adapterImageId === report.stages.adapterPrecheck.adapterImageId
          && report.stages.adapterRestored.adapterSubjectScopeRules
            === report.stages.adapterPrecheck.adapterSubjectScopeRules,
        {
          before: report.stages.adapterPrecheck,
          restored: report.stages.adapterRestored,
        });
      check("Phase 2 and write-back switches remain disabled",
        !String(report.stages.adapterRestored.phase2State).includes(":true"),
        report.stages.adapterRestored.phase2State);
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
    console.log(`BPI formal identity force-close acceptance: ${report.status} (${marker})`);
  }).catch((error) => {
    console.error(error?.stack || error);
    process.exitCode = 1;
  });
}
