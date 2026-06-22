#!/usr/bin/env node
"use strict";

const crypto = require("crypto");
const fs = require("fs");
const path = require("path");
const { spawnSync } = require("child_process");

const repoRoot = path.resolve(__dirname, "../../..");

const sshHost = process.env.ADP_SSH_HOST || "100.99.133.43";
const sshUser = process.env.ADP_SSH_USER || "v6";
const sshTarget = process.env.ADP_SSH_TARGET || `${sshUser}@${sshHost}`;
const sshConnectTimeout = process.env.ADP_SSH_CONNECT_TIMEOUT || "8";
const keycloakContainer = process.env.ADP_KEYCLOAK_CONTAINER || "adp-mes-newbase-keycloak-1";
const nacosContainer = process.env.ADP_NACOS_CONTAINER || "adp-mes-newbase-nacos-1";
const realm = process.env.ADP_KEYCLOAK_REALM || "dt";
const nacosGroup = process.env.ADP_NACOS_GROUP || "prod";
const baseUrl = (process.env.ADP_KEYCLOAK_SMOKE_BASE_URL || `http://${sshHost}:18080`).replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD || "123456";
const adminUser = process.env.ADP_KEYCLOAK_ADMIN_USER || "admin";
const adminPassword = process.env.ADP_KEYCLOAK_ADMIN_PASSWORD || "admin";
const outputPath =
  process.env.ADP_KEYCLOAK_JWT_SMOKE_OUTPUT ||
  path.join("/tmp", `adp-keycloak-jwt-runtime-smoke-${new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14)}.json`);

const expectedClients = (process.env.ADP_KEYCLOAK_EXPECTED_CLIENTS || `pc_${realm},mobile_${realm}`)
  .split(",")
  .map((item) => item.trim())
  .filter(Boolean);

const requiredMapperNames = [
  "userName",
  "userId",
  "departmentCode",
  "companyId",
  "staffName",
];

function ensureSafeToken(value, label) {
  if (!/^[A-Za-z0-9_.:@/-]+$/.test(value)) {
    throw new Error(`${label} contains unsupported characters: ${value}`);
  }
}

function ensureDir(filePath) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
}

function sha256(text) {
  return crypto.createHash("sha256").update(String(text || "")).digest("hex");
}

function fingerprint(text) {
  return text ? sha256(text).slice(0, 16) : "";
}

function shellSingleQuote(value) {
  return `'${String(value).replace(/'/g, "'\\''")}'`;
}

function parseDelimited(stdout) {
  const blocks = new Map();
  let current = null;
  let lines = [];
  for (const line of stdout.split(/\n/)) {
    const begin = line.match(/^__ADP_BEGIN__(.+)$/);
    if (begin) {
      current = begin[1];
      lines = [];
      continue;
    }
    const end = line.match(/^__ADP_END__(.+)$/);
    if (end) {
      blocks.set(end[1], lines.join("\n").trim());
      current = null;
      lines = [];
      continue;
    }
    if (current) {
      lines.push(line);
    }
  }
  return blocks;
}

function parseJson(text, fallback) {
  try {
    return JSON.parse(text);
  } catch (_error) {
    return fallback;
  }
}

function parseCsv(text) {
  const rows = [];
  for (const rawLine of String(text || "").split(/\n/)) {
    const line = rawLine.trim();
    if (!line) {
      continue;
    }
    const values = [];
    let current = "";
    let inQuotes = false;
    for (let index = 0; index < line.length; index += 1) {
      const char = line[index];
      if (char === '"') {
        inQuotes = !inQuotes;
        continue;
      }
      if (char === "," && !inQuotes) {
        values.push(current);
        current = "";
        continue;
      }
      current += char;
    }
    values.push(current);
    rows.push(values.map((value) => value.trim()));
  }
  return rows;
}

function parseProperties(text) {
  const values = {};
  for (const rawLine of String(text || "").split(/\n/)) {
    const line = rawLine.trim();
    if (!line || line.startsWith("#") || line.startsWith("!")) {
      continue;
    }
    const index = line.indexOf("=");
    if (index <= 0) {
      continue;
    }
    values[line.slice(0, index).trim()] = line.slice(index + 1).trim();
  }
  return values;
}

function runRemote(container, script, label) {
  ensureSafeToken(container, `${label} container`);
  const result = spawnSync(
    "ssh",
    [
      "-o",
      "BatchMode=yes",
      "-o",
      `ConnectTimeout=${sshConnectTimeout}`,
      "-T",
      sshTarget,
      "docker",
      "exec",
      "-i",
      container,
      "sh",
      "-s",
    ],
    {
      input: script,
      encoding: "utf8",
      maxBuffer: 50 * 1024 * 1024,
    }
  );
  if (result.error) {
    throw result.error;
  }
  if (result.status !== 0) {
    throw new Error(`${label} remote command failed with status ${result.status}: ${result.stderr}`);
  }
  return parseDelimited(result.stdout);
}

function buildKeycloakScript() {
  return `set -eu
REALM=${shellSingleQuote(realm)}
ADMIN_USER=${shellSingleQuote(adminUser)}
ADMIN_PASSWORD=${shellSingleQuote(adminPassword)}
KC=/opt/keycloak/bin/kcadm.sh
SERVER=http://127.0.0.1:8080/auth

emit() {
  name="$1"
  printf '%s\\n' "__ADP_BEGIN__\${name}"
  cat
  printf '\\n%s\\n' "__ADP_END__\${name}"
}

curl -fsS "$SERVER/realms/$REALM" | emit realm_public

$KC config credentials --server "$SERVER" --realm master --user "$ADMIN_USER" --password "$ADMIN_PASSWORD" >/dev/null 2>&1

$KC get "realms/$REALM" --fields realm,enabled,sslRequired,directGrantFlow --format json | emit realm_admin
$KC get clients -r "$REALM" --fields clientId,enabled,publicClient,directAccessGrantsEnabled,serviceAccountsEnabled,standardFlowEnabled,implicitFlowEnabled --format csv | emit clients_csv
$KC get client-scopes -r "$REALM" --fields id,name --format csv | emit client_scopes_csv

SCOPE_ID=$($KC get client-scopes -r "$REALM" --fields id,name --format csv | awk -F, '$2=="\\"supos\\"" {gsub(/\\"/, "", $1); print $1; exit}')
if [ -n "\${SCOPE_ID:-}" ]; then
  $KC get "client-scopes/$SCOPE_ID/protocol-mappers/models" -r "$REALM" --fields name,protocolMapper --format csv | emit supos_mappers_csv
else
  printf '' | emit supos_mappers_csv
fi

$KC get components -r "$REALM" --fields name,providerId,providerType --format csv | emit components_csv
$KC get authentication/flows -r "$REALM" --fields alias,builtIn,topLevel --format csv | emit flows_csv
`;
}

function buildNacosScript() {
  return `set -eu
GROUP=${shellSingleQuote(nacosGroup)}
emit() {
  name="$1"
  printf '%s\\n' "__ADP_BEGIN__\${name}"
  cat
  printf '\\n%s\\n' "__ADP_END__\${name}"
}
fetch() {
  url="$1"
  if command -v curl >/dev/null 2>&1; then
    curl -fsS "$url"
  else
    wget -qO- "$url"
  fi
}
fetch "http://127.0.0.1:8848/nacos/v1/cs/configs?dataId=supfusion-jwt-common.properties&group=$GROUP" | emit nacos_jwt
fetch "http://127.0.0.1:8848/nacos/v1/ns/instance/list?serviceName=keycloak&groupName=$GROUP" | emit nacos_keycloak_instances
`;
}

function byClientId(rows) {
  const clients = new Map();
  for (const row of rows) {
    const [clientId, enabled, publicClient, directAccessGrantsEnabled, serviceAccountsEnabled, standardFlowEnabled, implicitFlowEnabled] = row;
    if (!clientId) {
      continue;
    }
    clients.set(clientId, {
      clientId,
      enabled: enabled === "true",
      publicClient: publicClient === "true",
      directAccessGrantsEnabled: directAccessGrantsEnabled === "true",
      serviceAccountsEnabled: serviceAccountsEnabled === "true",
      standardFlowEnabled: standardFlowEnabled === "true",
      implicitFlowEnabled: implicitFlowEnabled === "true",
    });
  }
  return clients;
}

function namesFromRows(rows, column = 0) {
  return rows.map((row) => row[column]).filter(Boolean);
}

async function postJson(url, body, headers = {}) {
  const response = await fetch(url, {
    method: "POST",
    headers: {
      Accept: "application/json, text/plain, */*",
      "Content-Type": "application/json;charset=UTF-8",
      ...headers,
    },
    body: JSON.stringify(body),
  });
  const text = await response.text();
  return {
    status: response.status,
    ok: response.status < 400,
    text,
    json: parseJson(text, null),
  };
}

async function getJson(url, headers = {}) {
  const response = await fetch(url, {
    method: "GET",
    headers: {
      Accept: "application/json, text/plain, */*",
      ...headers,
    },
  });
  const text = await response.text();
  return {
    status: response.status,
    ok: response.status < 400,
    text,
    json: parseJson(text, null),
  };
}

function findTicket(payload) {
  const candidates = [
    payload && payload.ticket,
    payload && payload.access_token,
    payload && payload.token,
    payload && payload.data && payload.data.ticket,
    payload && payload.data && payload.data.access_token,
    payload && payload.data && payload.data.token,
    payload && payload.result && payload.result.ticket,
    payload && payload.result && payload.result.access_token,
    payload && payload.result && payload.result.token,
  ];
  return candidates.find((value) => typeof value === "string" && value.length > 20);
}

function countMenuTargets(payload) {
  let count = 0;
  const pickArray = (...values) => values.find((value) => Array.isArray(value));
  const roots =
    pickArray(
      payload,
      payload && payload.list,
      payload && payload.data,
      payload && payload.data && payload.data.list,
      payload && payload.data && payload.data.menus,
      payload && payload.result,
      payload && payload.result && payload.result.list,
      payload && payload.result && payload.result.menus
    ) || [];

  function visit(node) {
    if (!node || typeof node !== "object") {
      return;
    }
    const url = node.url || node.path || node.href || node.menuUrl || node.targetUrl || node.routeUrl;
    if (typeof url === "string" && url.trim() && !/^javascript:/i.test(url.trim())) {
      count += 1;
    }
    const children =
      pickArray(node.children, node.childrens, node.childMenus, node.subMenus, node.nodes, node.menuList) || [];
    children.forEach(visit);
  }
  roots.forEach(visit);
  return count;
}

async function runGatewaySmoke() {
  const login = await postJson(`${baseUrl}/inter-api/auth/login`, { userName: username, password, clientId: `pc_${realm}` });
  const ticket = login.ok ? findTicket(login.json) : null;
  const headers = ticket
    ? {
        Authorization: `Bearer ${ticket}`,
        Cookie: `suposTicket=${encodeURIComponent(ticket)}`,
        langu_code: "zh_CN",
      }
    : {};
  const menus = ticket ? await getJson(`${baseUrl}/inter-api/rbac/v1/menus/currentUser`, headers) : null;
  return {
    loginStatus: login.status,
    loginOk: Boolean(ticket),
    ticketFingerprint: ticket ? fingerprint(ticket) : "",
    menuStatus: menus ? menus.status : 0,
    menuTargetCount: menus && menus.ok ? countMenuTargets(menus.json) : 0,
  };
}

function addCheck(checks, name, passed, evidence) {
  checks.push({ name, status: passed ? "PASS" : "FAIL", evidence });
}

async function main() {
  ensureSafeToken(realm, "ADP_KEYCLOAK_REALM");
  ensureSafeToken(nacosGroup, "ADP_NACOS_GROUP");
  for (const client of expectedClients) {
    ensureSafeToken(client, "ADP_KEYCLOAK_EXPECTED_CLIENTS");
  }

  const keycloakBlocks = runRemote(keycloakContainer, buildKeycloakScript(), "Keycloak");
  const nacosBlocks = runRemote(nacosContainer, buildNacosScript(), "Nacos");
  const gateway = await runGatewaySmoke();

  const realmPublic = parseJson(keycloakBlocks.get("realm_public"), {});
  const realmAdmin = parseJson(keycloakBlocks.get("realm_admin"), {});
  const publicKey = realmPublic.public_key || "";
  const clients = byClientId(parseCsv(keycloakBlocks.get("clients_csv")));
  const clientScopeNames = namesFromRows(parseCsv(keycloakBlocks.get("client_scopes_csv")), 1);
  const mapperNames = namesFromRows(parseCsv(keycloakBlocks.get("supos_mappers_csv")), 0);
  const components = parseCsv(keycloakBlocks.get("components_csv")).map((row) => ({
    name: row[0] || "",
    providerId: row[1] || "",
    providerType: row[2] || "",
  }));
  const flowAliases = namesFromRows(parseCsv(keycloakBlocks.get("flows_csv")), 0);

  const nacosJwt = parseProperties(nacosBlocks.get("nacos_jwt"));
  const nacosJwtSecret = nacosJwt["supfusion.cloud.jwt.secret"] || "";
  const nacosInstances = parseJson(nacosBlocks.get("nacos_keycloak_instances"), {});
  const healthyNacosHosts = Array.isArray(nacosInstances.hosts)
    ? nacosInstances.hosts.filter((host) => host && host.healthy && host.enabled)
    : [];

  const checks = [];
  addCheck(checks, "realm-public-key-present", publicKey.length > 100, `publicKeySha256Prefix=${fingerprint(publicKey)}`);
  addCheck(checks, "realm-enabled", realmAdmin.enabled === true, `enabled=${realmAdmin.enabled}`);
  addCheck(checks, "realm-direct-grant-flow", realmAdmin.directGrantFlow === "lfy grant", `directGrantFlow=${realmAdmin.directGrantFlow || ""}`);
  addCheck(
    checks,
    "nacos-jwt-public-key-synced",
    Boolean(publicKey) && publicKey === nacosJwtSecret,
    `keycloakSha256Prefix=${fingerprint(publicKey)}; nacosSha256Prefix=${fingerprint(nacosJwtSecret)}`
  );
  addCheck(checks, "nacos-keycloak-healthy-instance", healthyNacosHosts.length >= 1, `healthyHosts=${healthyNacosHosts.length}`);
  addCheck(checks, "client-scope-supos-present", clientScopeNames.includes("supos"), `clientScopes=${clientScopeNames.length}`);
  addCheck(
    checks,
    "readonly-property-file-component-present",
    components.some((component) => component.name === "readonly-property-file" && component.providerId === "readonly-property-file"),
    `components=${components.length}`
  );
  addCheck(checks, "lfy-grant-flow-present", flowAliases.includes("lfy grant"), `flows=${flowAliases.length}`);
  for (const clientId of expectedClients) {
    const client = clients.get(clientId);
    addCheck(checks, `client-${clientId}-enabled`, Boolean(client && client.enabled), client ? "client is enabled" : "client missing");
    addCheck(
      checks,
      `client-${clientId}-direct-grant`,
      Boolean(client && client.directAccessGrantsEnabled && client.publicClient),
      client ? `publicClient=${client.publicClient}; directAccessGrantsEnabled=${client.directAccessGrantsEnabled}` : "client missing"
    );
  }
  for (const mapperName of requiredMapperNames) {
    addCheck(checks, `supos-mapper-${mapperName}`, mapperNames.includes(mapperName), `mappers=${mapperNames.length}`);
  }
  addCheck(checks, "gateway-login-admin", gateway.loginOk, `status=${gateway.loginStatus}; ticketSha256Prefix=${gateway.ticketFingerprint}`);
  addCheck(checks, "gateway-menu-current-user", gateway.menuTargetCount >= 30, `status=${gateway.menuStatus}; menuTargets=${gateway.menuTargetCount}`);

  const statuses = checks.map((check) => check.status);
  const report = {
    schemaVersion: "1.0",
    generatedAt: new Date().toISOString(),
    repoCommit: spawnSync("git", ["rev-parse", "HEAD"], { cwd: repoRoot, encoding: "utf8" }).stdout.trim(),
    database: "PostgreSQL",
    target: {
      sshHost,
      sshUser,
      keycloakContainer,
      nacosContainer,
      realm,
      nacosGroup,
      baseUrl,
    },
    summary: {
      checks: checks.length,
      pass: statuses.filter((status) => status === "PASS").length,
      fail: statuses.filter((status) => status === "FAIL").length,
      expectedClients: expectedClients.length,
      clientScopes: clientScopeNames.length,
      suposMappers: mapperNames.length,
      keycloakPublicKeySha256Prefix: fingerprint(publicKey),
      nacosJwtSha256Prefix: fingerprint(nacosJwtSecret),
      nacosHealthyKeycloakHosts: healthyNacosHosts.length,
      gatewayMenuTargets: gateway.menuTargetCount,
      status: statuses.includes("FAIL") ? "FAIL" : "PASS",
    },
    checks,
    evidence: {
      clients: expectedClients.map((clientId) => clients.get(clientId) || { clientId, missing: true }),
      requiredMappers: requiredMapperNames,
      presentRequiredMappers: requiredMapperNames.filter((mapperName) => mapperNames.includes(mapperName)),
      nacosKeycloakHosts: healthyNacosHosts.map((host) => ({
        ip: host.ip || "",
        port: host.port || 0,
        healthy: Boolean(host.healthy),
        enabled: Boolean(host.enabled),
        ephemeral: Boolean(host.ephemeral),
      })),
      gateway,
    },
  };

  ensureDir(outputPath);
  fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`, "utf8");
  console.log(`Keycloak/JWT runtime smoke wrote ${outputPath}`);
  console.log(JSON.stringify(report.summary, null, 2));

  if (report.summary.status !== "PASS") {
    process.exitCode = 1;
  }
}

main().catch((error) => {
  console.error(error && error.stack ? error.stack : String(error));
  process.exitCode = 1;
});
