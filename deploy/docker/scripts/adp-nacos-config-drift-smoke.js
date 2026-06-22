#!/usr/bin/env node
"use strict";

const crypto = require("crypto");
const fs = require("fs");
const path = require("path");
const { spawnSync } = require("child_process");

const repoRoot = path.resolve(__dirname, "../../..");
const defaultConfigDir = path.join(repoRoot, "deploy/docker/nacos-rendered");

const sshHost = process.env.ADP_SSH_HOST || "100.99.133.43";
const sshUser = process.env.ADP_SSH_USER || "v6";
const sshTarget = process.env.ADP_SSH_TARGET || `${sshUser}@${sshHost}`;
const sshConnectTimeout = process.env.ADP_SSH_CONNECT_TIMEOUT || "8";
const nacosContainer = process.env.ADP_NACOS_CONTAINER || "adp-mes-newbase-nacos-1";
const nacosGroup = process.env.ADP_NACOS_GROUP || "prod";
const configDir = path.resolve(process.env.ADP_NACOS_RENDERED_DIR || defaultConfigDir);
const outputPath =
  process.env.ADP_NACOS_CONFIG_SMOKE_OUTPUT ||
  path.join("/tmp", `adp-nacos-config-drift-smoke-${new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14)}.json`);

const criticalDataIds = new Set([
  "supfusion-datasource-system.properties",
  "supfusion-mybatis-common.properties",
  "supfusion-jwt-common.properties",
  "supfusion-gateway.properties",
  "supfusion-registry.properties",
  "supfusion-license.properties",
  "supfusion-systemConfig.properties",
  "WOMMs-prod.properties",
]);

const expectedServices = [
  "supos-gateway",
  "FoundationMs",
  "baseService",
  "auth",
  "organization",
  "rbac",
  "systemconfig",
  "keycloak",
  "WOMMs",
  "WOM",
  "QCS",
  "LIMSBasic",
  "WTS",
  "workAppointment",
  "RM",
  "RMMs",
  "TeamInfo",
  "BaseSet",
];

const forbiddenOraclePatterns = [
  /\bjdbc:oracle\b/i,
  /\boracle\.jdbc\b/i,
  /\borg\.hibernate\.dialect\.Oracle/i,
  /\bdb-type\s*=\s*oracle\b/i,
  /\$\{SUPOS_SYSTEM_DB_TYPE:oracle\}/i,
  /\$\{supfusion\.cloud\.datasource\.connect\.system\.db-type:oracle\}/i,
  /(^|[/:])oracle([/:]|$)/i,
  /\b1521\b/,
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
  return crypto.createHash("sha256").update(text).digest("hex");
}

function normalize(text) {
  return text.replace(/\r\n/g, "\n").replace(/\r/g, "\n").trimEnd() + "\n";
}

function stripCommentLines(text) {
  return text
    .split(/\n/)
    .filter((line) => {
      const trimmed = line.trim();
      return trimmed && !trimmed.startsWith("#") && !trimmed.startsWith("!");
    })
    .join("\n");
}

function parseProperties(text) {
  const values = {};
  for (const rawLine of text.split(/\n/)) {
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

function parseJson(text, fallback) {
  try {
    return JSON.parse(text);
  } catch (_error) {
    return fallback;
  }
}

function countOracleResidue(text) {
  const body = stripCommentLines(text);
  return forbiddenOraclePatterns.reduce((count, pattern) => count + (pattern.test(body) ? 1 : 0), 0);
}

function readLocalDataIds() {
  if (!fs.existsSync(configDir)) {
    throw new Error(`Nacos rendered config directory not found: ${configDir}`);
  }
  return fs
    .readdirSync(configDir)
    .filter((name) => name.endsWith(".properties"))
    .sort();
}

function shellSingleQuote(value) {
  return `'${value.replace(/'/g, "'\\''")}'`;
}

function buildRemoteScript(dataIds) {
  const dataIdList = dataIds.map(shellSingleQuote).join(" ");
  return `set -eu
group=${shellSingleQuote(nacosGroup)}
fetch_one() {
  data_id="$1"
  url="http://127.0.0.1:8848/nacos/v1/cs/configs?dataId=\${data_id}&group=\${group}"
  if command -v curl >/dev/null 2>&1; then
    curl -fsS "$url"
  else
    wget -qO- "$url"
  fi
}
for data_id in ${dataIdList}; do
  printf '%s\\n' "__ADP_NACOS_BEGIN__\${data_id}"
  set +e
  fetch_one "$data_id"
  rc=$?
  set -e
  printf '\\n%s\\n' "__ADP_NACOS_END__\${data_id}__RC__\${rc}"
done
`;
}

function fetchRemoteConfigs(dataIds) {
  ensureSafeToken(nacosContainer, "ADP_NACOS_CONTAINER");
  ensureSafeToken(nacosGroup, "ADP_NACOS_GROUP");
  for (const dataId of dataIds) {
    ensureSafeToken(dataId, "dataId");
  }

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
      nacosContainer,
      "sh",
      "-s",
    ],
    {
      input: buildRemoteScript(dataIds),
      encoding: "utf8",
      maxBuffer: 50 * 1024 * 1024,
    }
  );

  if (result.error) {
    throw result.error;
  }
  if (result.status !== 0) {
    throw new Error(`Remote Nacos fetch failed with status ${result.status}: ${result.stderr}`);
  }

  const records = new Map();
  let current = null;
  let currentLines = [];
  for (const line of result.stdout.split(/\n/)) {
    const begin = line.match(/^__ADP_NACOS_BEGIN__(.+)$/);
    if (begin) {
      current = begin[1];
      currentLines = [];
      continue;
    }
    const end = line.match(/^__ADP_NACOS_END__(.+)__RC__(\d+)$/);
    if (end) {
      const dataId = end[1];
      records.set(dataId, {
        dataId,
        rc: Number(end[2]),
        content: normalize(currentLines.join("\n")),
      });
      current = null;
      currentLines = [];
      continue;
    }
    if (current) {
      currentLines.push(line);
    }
  }
  return records;
}

function buildRemoteServiceScript(serviceNames) {
  const serviceList = serviceNames.map(shellSingleQuote).join(" ");
  return `set -eu
group=${shellSingleQuote(nacosGroup)}
fetch_url() {
  url="$1"
  if command -v curl >/dev/null 2>&1; then
    curl -fsS "$url"
  else
    wget -qO- "$url"
  fi
}
printf '%s\\n' "__ADP_NACOS_SERVICE_LIST_BEGIN__"
set +e
fetch_url "http://127.0.0.1:8848/nacos/v1/ns/service/list?pageNo=1&pageSize=500&groupName=\${group}"
service_list_rc=$?
set -e
printf '\\n%s\\n' "__ADP_NACOS_SERVICE_LIST_END__RC__\${service_list_rc}"
for service_name in ${serviceList}; do
  printf '%s\\n' "__ADP_NACOS_SERVICE_BEGIN__\${service_name}"
  set +e
  fetch_url "http://127.0.0.1:8848/nacos/v1/ns/instance/list?serviceName=\${service_name}&groupName=\${group}&healthyOnly=false"
  rc=$?
  set -e
  printf '\\n%s\\n' "__ADP_NACOS_SERVICE_END__\${service_name}__RC__\${rc}"
done
`;
}

function fetchRemoteServices(serviceNames) {
  ensureSafeToken(nacosContainer, "ADP_NACOS_CONTAINER");
  ensureSafeToken(nacosGroup, "ADP_NACOS_GROUP");
  for (const serviceName of serviceNames) {
    ensureSafeToken(serviceName, "serviceName");
  }

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
      nacosContainer,
      "sh",
      "-s",
    ],
    {
      input: buildRemoteServiceScript(serviceNames),
      encoding: "utf8",
      maxBuffer: 50 * 1024 * 1024,
    }
  );

  if (result.error) {
    throw result.error;
  }
  if (result.status !== 0) {
    throw new Error(`Remote Nacos service fetch failed with status ${result.status}: ${result.stderr}`);
  }

  const records = new Map();
  let serviceList = { rc: 1, content: "" };
  let current = null;
  let currentLines = [];
  for (const line of result.stdout.split(/\n/)) {
    if (line === "__ADP_NACOS_SERVICE_LIST_BEGIN__") {
      current = "__service_list__";
      currentLines = [];
      continue;
    }
    const serviceListEnd = line.match(/^__ADP_NACOS_SERVICE_LIST_END__RC__(\d+)$/);
    if (serviceListEnd) {
      serviceList = {
        rc: Number(serviceListEnd[1]),
        content: normalize(currentLines.join("\n")),
      };
      current = null;
      currentLines = [];
      continue;
    }
    const begin = line.match(/^__ADP_NACOS_SERVICE_BEGIN__(.+)$/);
    if (begin) {
      current = begin[1];
      currentLines = [];
      continue;
    }
    const end = line.match(/^__ADP_NACOS_SERVICE_END__(.+)__RC__(\d+)$/);
    if (end) {
      records.set(end[1], {
        serviceName: end[1],
        rc: Number(end[2]),
        content: normalize(currentLines.join("\n")),
      });
      current = null;
      currentLines = [];
      continue;
    }
    if (current) {
      currentLines.push(line);
    }
  }
  return { serviceList, records };
}

function checkCriticalDataId(dataId, remoteText) {
  const properties = parseProperties(remoteText);
  const checks = [];
  const add = (name, passed, evidence) => {
    checks.push({ name, status: passed ? "PASS" : "FAIL", evidence });
  };

  if (dataId === "supfusion-datasource-system.properties") {
    add(
      "datasource-db-type-postgresql",
      properties["supfusion.cloud.datasource.connect.system.db-type"] === "postgresql",
      `db-type=${properties["supfusion.cloud.datasource.connect.system.db-type"] || ""}`
    );
    add(
      "datasource-host-postgres",
      properties["supfusion.cloud.datasource.connect.system.host"] === "postgres",
      `host=${properties["supfusion.cloud.datasource.connect.system.host"] || ""}`
    );
    add(
      "datasource-port-5432",
      properties["supfusion.cloud.datasource.connect.system.port"] === "5432",
      `port=${properties["supfusion.cloud.datasource.connect.system.port"] || ""}`
    );
    add(
      "datasource-validation-select-1",
      /^SELECT\s+1$/i.test(properties["supfusion.cloud.datasource.druid.validationQuerySql"] || ""),
      "validationQuerySql is SELECT 1"
    );
  }

  if (dataId === "supfusion-mybatis-common.properties") {
    const mapperValues = Object.entries(properties)
      .filter(([key]) => key.startsWith("mybatis-plus.mapper-locations"))
      .map(([, value]) => value);
    add(
      "mybatis-uses-runtime-db-type",
      mapperValues.some((value) => value.includes("${supfusion.cloud.datasource.connect.system.db-type}")),
      `mapperLocations=${mapperValues.length}`
    );
  }

  if (dataId === "supfusion-jwt-common.properties") {
    const jwtSecret = properties["supfusion.cloud.jwt.secret"] || "";
    const fingerprint = jwtSecret ? sha256(jwtSecret).slice(0, 16) : "";
    add("jwt-secret-present", jwtSecret.length > 20, `sha256Prefix=${fingerprint}; length=${jwtSecret.length}`);
    add("jwt-token-head-bearer", properties["supfusion.cloud.jwt.tokenHead"] === "Bearer", "tokenHead=Bearer");
  }

  if (dataId === "supfusion-gateway.properties") {
    add("gateway-keycloak-client-present", Boolean(properties["keycloak.client"]), "keycloak.client is present");
    add("gateway-jwt-secret-present", Boolean(properties["jwt.secret"]), "jwt.secret is present");
  }

  if (dataId === "supfusion-registry.properties") {
    add(
      "registry-group-prod",
      properties["supfusion.cloud.registry.group"] === nacosGroup,
      `registry.group=${properties["supfusion.cloud.registry.group"] || ""}`
    );
    add(
      "discovery-group-prod",
      properties["supfusion.cloud.discovery.group"] === nacosGroup,
      `discovery.group=${properties["supfusion.cloud.discovery.group"] || ""}`
    );
  }

  if (dataId === "WOMMs-prod.properties") {
    add("wom-kafka-service-name", /kafka:9092/.test(remoteText), "WOM kafka endpoint uses kafka:9092");
  }

  if (criticalDataIds.has(dataId)) {
    add("no-oracle-residue", countOracleResidue(remoteText) === 0, "non-comment Oracle residue count is zero");
  }

  return checks;
}

function buildServiceItems(serviceResult) {
  const listJson = serviceResult.serviceList.rc === 0 ? parseJson(serviceResult.serviceList.content, {}) : {};
  const serviceNames = new Set(Array.isArray(listJson.doms) ? listJson.doms : []);
  const items = expectedServices.map((serviceName) => {
    const record = serviceResult.records.get(serviceName);
    const fetched = Boolean(record && record.rc === 0);
    const payload = fetched ? parseJson(record.content, {}) : {};
    const hosts = Array.isArray(payload.hosts) ? payload.hosts : [];
    const healthyEnabledHosts = hosts.filter((host) => host && host.healthy === true && host.enabled !== false);
    const unhealthyHosts = hosts.filter((host) => host && host.healthy !== true);
    const issues = [];
    if (!serviceNames.has(serviceName)) {
      issues.push("service is missing from Nacos service list");
    }
    if (!fetched) {
      issues.push(`instance fetch failed rc=${record ? record.rc : "missing-delimiter"}`);
    }
    if (healthyEnabledHosts.length === 0) {
      issues.push("no healthy enabled instance");
    }

    return {
      serviceName,
      listed: serviceNames.has(serviceName),
      fetched,
      totalHosts: hosts.length,
      healthyHosts: healthyEnabledHosts.length,
      unhealthyHosts: unhealthyHosts.length,
      ports: [...new Set(hosts.map((host) => host.port).filter((port) => port !== undefined))].sort((a, b) => Number(a) - Number(b)),
      instanceIds: hosts.map((host) => host.instanceId).filter(Boolean),
      status: serviceNames.has(serviceName) && fetched && healthyEnabledHosts.length > 0 ? "PASS" : "FAIL",
      issues,
    };
  });
  return {
    group: nacosGroup,
    listFetchStatus: serviceResult.serviceList.rc === 0 ? "PASS" : "FAIL",
    serviceCount: Number(listJson.count || serviceNames.size || 0),
    expectedServiceCount: expectedServices.length,
    items,
  };
}

function summarize(items, criticalChecks, services) {
  const statuses = criticalChecks.map((check) => check.status);
  const missingRemote = items.filter((item) => !item.remoteFetched).length;
  const missingLocal = items.filter((item) => !item.localExists).length;
  const oracleResidueFiles = items.filter((item) => item.oracleResidueCount > 0).length;
  const serviceItems = services ? services.items : [];
  const failedServiceCount = serviceItems.filter((item) => item.status !== "PASS").length;
  return {
    dataIds: items.length,
    remoteFetched: items.filter((item) => item.remoteFetched).length,
    missingRemote,
    missingLocal,
    exactMatches: items.filter((item) => item.exactMatch).length,
    drifted: items.filter((item) => item.remoteFetched && item.localExists && !item.exactMatch).length,
    oracleResidueFiles,
    criticalChecks: criticalChecks.length,
    criticalPass: statuses.filter((status) => status === "PASS").length,
    criticalFail: statuses.filter((status) => status === "FAIL").length,
    expectedServices: serviceItems.length,
    healthyServices: serviceItems.filter((item) => item.status === "PASS").length,
    failedServices: failedServiceCount,
    nacosServiceCount: services ? services.serviceCount : 0,
    status:
      missingRemote === 0 &&
      missingLocal === 0 &&
      oracleResidueFiles === 0 &&
      !statuses.includes("FAIL") &&
      services &&
      services.listFetchStatus === "PASS" &&
      failedServiceCount === 0
        ? "PASS"
        : "FAIL",
  };
}

function main() {
  const dataIds = readLocalDataIds();
  const remoteRecords = fetchRemoteConfigs(dataIds);
  const services = buildServiceItems(fetchRemoteServices(expectedServices));
  const items = [];
  const criticalChecks = [];

  for (const dataId of dataIds) {
    const localPath = path.join(configDir, dataId);
    const localText = normalize(fs.readFileSync(localPath, "utf8"));
    const remoteRecord = remoteRecords.get(dataId);
    const remoteFetched = Boolean(remoteRecord && remoteRecord.rc === 0);
    const remoteText = remoteFetched ? remoteRecord.content : "";
    const itemIssues = [];
    if (!remoteFetched) {
      itemIssues.push(`remote fetch failed rc=${remoteRecord ? remoteRecord.rc : "missing-delimiter"}`);
    }
    const oracleResidueCount = remoteFetched ? countOracleResidue(remoteText) : 0;
    if (oracleResidueCount > 0) {
      itemIssues.push("Oracle residue detected in remote non-comment config lines");
    }

    const item = {
      dataId,
      critical: criticalDataIds.has(dataId),
      localExists: true,
      remoteFetched,
      localBytes: Buffer.byteLength(localText, "utf8"),
      remoteBytes: Buffer.byteLength(remoteText, "utf8"),
      localSha256: sha256(localText),
      remoteSha256: remoteFetched ? sha256(remoteText) : "",
      exactMatch: remoteFetched && localText === remoteText,
      oracleResidueCount,
      issues: itemIssues,
    };
    items.push(item);

    if (remoteFetched && criticalDataIds.has(dataId)) {
      for (const check of checkCriticalDataId(dataId, remoteText)) {
        criticalChecks.push({ dataId, ...check });
      }
    }
  }

  const report = {
    schemaVersion: "1.0",
    generatedAt: new Date().toISOString(),
    repoCommit: spawnSync("git", ["rev-parse", "HEAD"], { cwd: repoRoot, encoding: "utf8" }).stdout.trim(),
    database: "PostgreSQL",
    target: {
      sshHost,
      sshUser,
      nacosContainer,
      nacosGroup,
    },
    local: {
      configDir: path.relative(repoRoot, configDir),
    },
    summary: summarize(items, criticalChecks, services),
    criticalChecks,
    services,
    items,
  };

  ensureDir(outputPath);
  fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`, "utf8");
  console.log(`Nacos config drift smoke wrote ${outputPath}`);
  console.log(JSON.stringify(report.summary, null, 2));

  if (report.summary.status !== "PASS") {
    process.exitCode = 1;
  }
}

try {
  main();
} catch (error) {
  console.error(error && error.stack ? error.stack : String(error));
  process.exitCode = 1;
}
