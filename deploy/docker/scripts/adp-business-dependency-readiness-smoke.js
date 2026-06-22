#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { spawnSync } = require("child_process");
const { request } = require("playwright");

const repoRoot = path.resolve(__dirname, "../../..");

const baseUrl = (process.env.ADP_BASE_URL || "http://100.99.133.43:18080").replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD || "123456";
const sshHost = process.env.ADP_SSH_HOST || "100.99.133.43";
const sshUser = process.env.ADP_SSH_USER || "v6";
const sshTarget = process.env.ADP_SSH_TARGET || `${sshUser}@${sshHost}`;
const sshConnectTimeout = process.env.ADP_SSH_CONNECT_TIMEOUT || "8";
const nacosContainer = process.env.ADP_NACOS_CONTAINER || "adp-mes-newbase-nacos-1";
const postgresContainer = process.env.ADP_POSTGRES_CONTAINER || "adp-mes-newbase-postgres-1";
const nacosGroup = process.env.ADP_NACOS_GROUP || "prod";
const apiTimeoutMs = Number(process.env.ADP_API_TIMEOUT_MS || 20000);
const outputPath =
  process.env.ADP_BUSINESS_DEPENDENCY_SMOKE_OUTPUT ||
  path.join("/tmp", `adp-business-dependency-readiness-${new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14)}.json`);

const dependencies = [
  {
    id: "material-service",
    title: "Material warehouse/inventory service",
    requiredFor: ["PROD-019", "PROD-021", "PROD-022", "WOM/QCS inventory and stock-in write-back"],
    serviceNames: ["material", "MATERIAL", "wms", "WMS", "warehouse", "Warehouse", "inventory", "Inventory"],
    decisiveServiceName: "material",
    endpoints: [
      {
        name: "checkProdResult",
        method: "POST",
        path: "/msService/material/foreign/foreign/checkProdResult?srcId=1&checkResult=1",
        data: {},
      },
      {
        name: "generateProduceOutSing",
        method: "POST",
        path: "/msService/public/material/produceOutSingle/produceOutSing/generateProduceOutSing",
        data: {},
      },
    ],
    readyWhen: "serviceName=material has a healthy Nacos instance and marker inventory endpoints stop returning tenant-service 503.",
    nextAction:
      "Deploy/register the warehouse/material service package, map target stock-in tables, then rerun marker inventory acceptance.",
  },
  {
    id: "process-analysis",
    title: "ProcessAnalysis traceability service",
    requiredFor: ["PROD-020", "WOM production process traceability"],
    serviceNames: ["ProcessAnalysis", "processanalysis", "PROCESSANALYSIS", "Traceability", "traceability"],
    decisiveServiceName: "ProcessAnalysis",
    endpoints: [
      {
        name: "isProdprocessView",
        method: "GET",
        path: "/msService/ProcessAnalysis/analysisParam/analysisParam/isProdprocessView?batchNo=ADP_DEPENDENCY_SMOKE_BATCH",
      },
      {
        name: "processBatchViewOut",
        method: "GET",
        path:
          "/msService/ProcessAnalysis/processAnalysis/exelogSecond/processBatchViewOut?workFlowMenuCode=ProcessAnalysis_1.0.0_processAnalysis_processBatchViewOut&openType=page&batchNo=ADP_DEPENDENCY_SMOKE_BATCH&productNo=ADP_DEPENDENCY_SMOKE_MAT",
      },
      {
        name: "analysisiTask",
        method: "GET",
        path: "/msService/ProcessAnalysis/paramDetail/paramDetail/analysisiTask",
      },
      {
        name: "manualStatActive",
        method: "GET",
        path: "/msService/ProcessAnalysis/paramStatRec/paramStatRec/manualStatActive?activeId=1",
      },
      {
        name: "manualStatProcess",
        method: "GET",
        path: "/msService/ProcessAnalysis/paramStatRec/paramStatRec/manualStatProcess?processId=1",
      },
    ],
    readyWhen:
      "ProcessAnalysis has healthy service registration, runtime/menu metadata, PostgreSQL target tables, and authenticated endpoints no longer return tenant-service 503.",
    nextAction:
      "Deploy/recover the ProcessAnalysis package and schema, then rerun traceability browser/API/DB marker acceptance from the WOM button.",
  },
];

function ensureDir(filePath) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
}

function ensureSafeToken(value, label) {
  if (!/^[A-Za-z0-9_.:@/?=&%-]+$/.test(value)) {
    throw new Error(`${label} contains unsupported characters: ${value}`);
  }
}

function shellSingleQuote(value) {
  return `'${String(value).replace(/'/g, "'\\''")}'`;
}

function getRepoCommit() {
  const result = spawnSync("git", ["rev-parse", "HEAD"], {
    cwd: repoRoot,
    encoding: "utf8",
  });
  return result.status === 0 ? result.stdout.trim() : "UNKNOWN";
}

function parseJson(text, fallback) {
  try {
    return JSON.parse(text);
  } catch (_error) {
    return fallback;
  }
}

function parseDelimited(stdout, beginPrefix, endPattern) {
  const records = new Map();
  let current = null;
  let lines = [];
  for (const line of stdout.split(/\n/)) {
    if (line.startsWith(beginPrefix)) {
      current = line.slice(beginPrefix.length);
      lines = [];
      continue;
    }
    const end = line.match(endPattern);
    if (end) {
      records.set(end[1], {
        rc: Number(end[2]),
        content: lines.join("\n").trim(),
      });
      current = null;
      lines = [];
      continue;
    }
    if (current) {
      lines.push(line);
    }
  }
  return records;
}

function runSshDocker(container, script, label) {
  ensureSafeToken(sshHost, "ADP_SSH_HOST");
  ensureSafeToken(sshUser, "ADP_SSH_USER");
  ensureSafeToken(sshConnectTimeout, "ADP_SSH_CONNECT_TIMEOUT");
  ensureSafeToken(container, label);
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
    throw new Error(`${label} probe failed with status ${result.status}: ${result.stderr}`);
  }
  return result.stdout;
}

function fetchNacosServices() {
  const serviceNames = [...new Set(dependencies.flatMap((dependency) => dependency.serviceNames))];
  const serviceList = serviceNames.map(shellSingleQuote).join(" ");
  const script = `set -eu
group=${shellSingleQuote(nacosGroup)}
fetch_url() {
  url="$1"
  if command -v curl >/dev/null 2>&1; then
    curl -fsS "$url"
  else
    wget -qO- "$url"
  fi
}
for service_name in ${serviceList}; do
  printf '%s\\n' "__ADP_SERVICE_BEGIN__\${service_name}"
  set +e
  fetch_url "http://127.0.0.1:8848/nacos/v1/ns/instance/list?serviceName=\${service_name}&groupName=\${group}&healthyOnly=false"
  rc=$?
  set -e
  printf '\\n%s\\n' "__ADP_SERVICE_END__\${service_name}__RC__\${rc}"
done
`;
  const records = parseDelimited(
    runSshDocker(nacosContainer, script, "ADP_NACOS_CONTAINER"),
    "__ADP_SERVICE_BEGIN__",
    /^__ADP_SERVICE_END__(.+)__RC__(\d+)$/
  );

  const services = {};
  for (const serviceName of serviceNames) {
    const record = records.get(serviceName);
    const payload = record && record.rc === 0 ? parseJson(record.content, {}) : {};
    const hosts = Array.isArray(payload.hosts) ? payload.hosts : [];
    const healthyHosts = hosts.filter((host) => host && host.healthy === true && host.enabled !== false);
    services[serviceName] = {
      serviceName,
      fetched: Boolean(record && record.rc === 0),
      hostCount: hosts.length,
      healthyHostCount: healthyHosts.length,
      ports: [...new Set(hosts.map((host) => host.port).filter((port) => port !== undefined))].sort(
        (left, right) => Number(left) - Number(right)
      ),
      instanceIds: hosts.map((host) => host.instanceId).filter(Boolean),
    };
  }
  return services;
}

function fetchDatabaseEvidence() {
  const script = `set -eu
DB=\${POSTGRES_DB:-adp}
USER=\${POSTGRES_USER:-adp}
PSQL="psql -v ON_ERROR_STOP=1 -U $USER -d $DB"
$PSQL -Atqc "select json_build_object(
  'materialLikeTableCount', (
    select count(*) from information_schema.tables
    where table_schema='public'
      and (
        lower(table_name) like '%material%'
        or lower(table_name) like '%stock%'
        or lower(table_name) like '%warehouse%'
        or lower(table_name) like '%inventory%'
        or lower(table_name) like '%produce_out%'
        or lower(table_name) like '%out_single%'
      )
  ),
  'materialLikeTables', (
    select coalesce(json_agg(table_name order by table_name), '[]'::json)
    from (
      select table_name from information_schema.tables
      where table_schema='public'
        and (
          lower(table_name) like '%material%'
          or lower(table_name) like '%stock%'
          or lower(table_name) like '%warehouse%'
          or lower(table_name) like '%inventory%'
          or lower(table_name) like '%produce_out%'
          or lower(table_name) like '%out_single%'
        )
      order by table_name
      limit 80
    ) t
  ),
  'processAnalysisTableCount', (
    select count(*) from information_schema.tables
    where table_schema='public'
      and (
        table_name ~* '^pa_'
        or table_name ~* 'process.*analysis'
        or table_name ~* 'trace'
        or table_name ~* 'process_batch'
      )
  ),
  'processAnalysisTables', (
    select coalesce(json_agg(table_name order by table_name), '[]'::json)
    from (
      select table_name from information_schema.tables
      where table_schema='public'
        and (
          table_name ~* '^pa_'
          or table_name ~* 'process.*analysis'
          or table_name ~* 'trace'
          or table_name ~* 'process_batch'
        )
      order by table_name
      limit 80
    ) t
  ),
  'processAnalysisRuntimeViewCount', (
    select count(*) from public.runtime_view
    where code ilike '%ProcessAnalysis%'
       or url ilike '%ProcessAnalysis%'
       or code ilike '%trace%'
       or url ilike '%trace%'
  ),
  'processAnalysisMenuCount', (
    select count(*) from public.rbac_menuinfo
    where code ilike '%ProcessAnalysis%'
       or url ilike '%ProcessAnalysis%'
       or code ilike '%trace%'
       or url ilike '%trace%'
  )
)"
`;
  return parseJson(runSshDocker(postgresContainer, script, "ADP_POSTGRES_CONTAINER"), {});
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

async function readJsonSafe(response) {
  const text = await response.text();
  return { json: parseJson(text, null), text };
}

async function login(api) {
  const attempts = [
    { userName: username, password, clientId: "pc_dt" },
    { username, password, clientId: "pc_dt" },
  ];
  const errors = [];
  for (const body of attempts) {
    const response = await api.post(`${baseUrl}/inter-api/auth/login`, {
      data: body,
      headers: {
        Accept: "application/json, text/plain, */*",
        "Content-Type": "application/json;charset=UTF-8",
      },
    });
    const parsed = await readJsonSafe(response);
    const ticket = response.ok() ? findTicket(parsed.json) : null;
    if (ticket) {
      return ticket;
    }
    errors.push({ status: response.status(), body: sanitizeBody(parsed.text) });
  }
  throw new Error(`Login failed for ${username}: ${JSON.stringify(errors)}`);
}

function sanitizeBody(text) {
  return String(text || "")
    .replace(/\s+/g, " ")
    .replace(/Bearer\s+[A-Za-z0-9._-]+/g, "Bearer <redacted>")
    .slice(0, 500);
}

async function probeEndpoint(api, ticket, endpoint) {
  try {
    const response = await api.fetch(`${baseUrl}${endpoint.path}`, {
      method: endpoint.method,
      data: endpoint.data,
      timeout: apiTimeoutMs,
      headers: {
        Accept: "application/json, text/plain, */*",
        Authorization: `Bearer ${ticket}`,
      },
    });
    const text = await response.text();
    const body = sanitizeBody(text);
    return {
      name: endpoint.name,
      method: endpoint.method,
      path: endpoint.path,
      status: response.status(),
      error: null,
      timeout: false,
      body,
      tenantServiceMissing:
        response.status() === 503 && (/can not find any tenant app service/i.test(body) || /Service Unavailable/i.test(body)),
    };
  } catch (error) {
    const message = sanitizeBody(error && error.message ? error.message : String(error));
    return {
      name: endpoint.name,
      method: endpoint.method,
      path: endpoint.path,
      status: 0,
      error: message,
      timeout: /timeout|timed out/i.test(message),
      body: "",
      tenantServiceMissing: false,
    };
  }
}

function decideStatus(dependency, serviceEvidence, endpointEvidence, databaseEvidence) {
  const decisiveService = serviceEvidence[dependency.decisiveServiceName] || { healthyHostCount: 0 };
  const anyHealthyAlias = dependency.serviceNames.some((serviceName) => {
    const service = serviceEvidence[serviceName];
    return service && service.healthyHostCount > 0;
  });
  const tenantMissingEndpoints = endpointEvidence.filter((endpoint) => endpoint.tenantServiceMissing).length;

  if (dependency.id === "material-service") {
    if (decisiveService.healthyHostCount > 0 && tenantMissingEndpoints === 0) {
      return "ACTION_REQUIRED";
    }
    return "BLOCKED";
  }

  if (dependency.id === "process-analysis") {
    const metadataPresent =
      Number(databaseEvidence.processAnalysisTableCount || 0) > 0 ||
      Number(databaseEvidence.processAnalysisRuntimeViewCount || 0) > 0 ||
      Number(databaseEvidence.processAnalysisMenuCount || 0) > 0;
    if (anyHealthyAlias && metadataPresent && tenantMissingEndpoints === 0) {
      return "ACTION_REQUIRED";
    }
    return "BLOCKED";
  }

  return "BLOCKED";
}

async function buildReport() {
  const serviceEvidence = fetchNacosServices();
  const databaseEvidence = fetchDatabaseEvidence();
  const api = await request.newContext({ ignoreHTTPSErrors: true });
  const ticket = await login(api);
  try {
    const items = [];
    for (const dependency of dependencies) {
      const endpoints = [];
      for (const endpoint of dependency.endpoints) {
        endpoints.push(await probeEndpoint(api, ticket, endpoint));
      }
      const services = dependency.serviceNames.map((serviceName) => serviceEvidence[serviceName]);
      const status = decideStatus(dependency, serviceEvidence, endpoints, databaseEvidence);
      const issues = [];
      if (status === "BLOCKED") {
        if (!services.some((service) => service && service.healthyHostCount > 0)) {
          issues.push("No healthy Nacos service instance was found for the expected dependency aliases.");
        }
        if (endpoints.some((endpoint) => endpoint.tenantServiceMissing)) {
          issues.push("Authenticated endpoint probe still returns tenant-service 503.");
        }
        const failedEndpointNames = endpoints
          .filter((endpoint) => endpoint.status === 0 || endpoint.error)
          .map((endpoint) => `${endpoint.name}${endpoint.timeout ? " timeout" : ""}`);
        if (failedEndpointNames.length > 0) {
          issues.push(`Authenticated endpoint probe failed before HTTP response: ${failedEndpointNames.join(", ")}.`);
        }
        if (dependency.id === "process-analysis" && Number(databaseEvidence.processAnalysisRuntimeViewCount || 0) === 0) {
          issues.push("No ProcessAnalysis runtime_view metadata was found.");
        }
      }
      if (status === "ACTION_REQUIRED") {
        issues.push("Dependency appears available now; rerun the corresponding marker persistence/browser acceptance before changing product cases to PASS.");
      }
      items.push({
        id: dependency.id,
        title: dependency.title,
        status,
        requiredFor: dependency.requiredFor,
        readyWhen: dependency.readyWhen,
        services,
        endpoints,
        database:
          dependency.id === "material-service"
            ? {
                materialLikeTableCount: databaseEvidence.materialLikeTableCount || 0,
                materialLikeTables: databaseEvidence.materialLikeTables || [],
              }
            : {
                processAnalysisTableCount: databaseEvidence.processAnalysisTableCount || 0,
                processAnalysisTables: databaseEvidence.processAnalysisTables || [],
                processAnalysisRuntimeViewCount: databaseEvidence.processAnalysisRuntimeViewCount || 0,
                processAnalysisMenuCount: databaseEvidence.processAnalysisMenuCount || 0,
              },
        issues,
        nextAction: dependency.nextAction,
      });
    }

    const statusCounts = {
      ready: items.filter((item) => item.status === "READY").length,
      actionRequired: items.filter((item) => item.status === "ACTION_REQUIRED").length,
      blocked: items.filter((item) => item.status === "BLOCKED").length,
    };
    return {
      schemaVersion: 1,
      generatedAt: new Date().toISOString(),
      repoCommit: getRepoCommit(),
      database: "PostgreSQL",
      target: {
        baseUrl,
        sshHost,
        sshUser,
        nacosContainer,
        postgresContainer,
        nacosGroup,
        apiTimeoutMs,
      },
      summary: {
        status: statusCounts.blocked > 0 ? "BLOCKED" : statusCounts.actionRequired > 0 ? "ACTION_REQUIRED" : "READY",
        dependencies: items.length,
        ...statusCounts,
      },
      items,
      evidence: {
        method:
          "Read-only Nacos service instance lookup, read-only PostgreSQL metadata scan, and authenticated bounded-time HTTP dependency probes.",
        secretHandling: "Login token and credentials are not written to this report.",
      },
    };
  } finally {
    await api.dispose();
  }
}

async function main() {
  const report = await buildReport();
  ensureDir(outputPath);
  fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`, "utf8");
  console.log(JSON.stringify(report.summary, null, 2));
  console.log(`Business dependency readiness smoke report: ${outputPath}`);
}

main().catch((error) => {
  console.error(error && error.stack ? error.stack : String(error));
  process.exitCode = 1;
});
