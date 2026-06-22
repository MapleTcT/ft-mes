#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { execFileSync } = require("child_process");
const { request } = require("playwright");

const baseUrl = (process.env.ADP_BASE_URL || "http://100.99.133.43:18080").replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD || "123456";
const dbSshTarget = process.env.ADP_DB_SSH_TARGET || "v6@100.99.133.43";
const dbSshPassword = process.env.ADP_DB_SSH_PASSWORD || "";
const dbContainer = process.env.ADP_DB_CONTAINER || "adp-mes-newbase-postgres-1";
const dbName = process.env.ADP_DB_NAME || "adp";
const dbUser = process.env.ADP_DB_USER || "adp";
const serviceJarPath =
  process.env.ADP_WOM_QRCODE_SERVICE_JAR ||
  "/home/v6/adp-mes-docker-newbase-20260611-181921/runtime/bap-server/module-Server/WOMMs/manual/WOMMs-1.0.0.jar";
const outputPath =
  process.env.ADP_WOM_QRCODE_ROUTE_PROBE_OUTPUT ||
  "metadata/wom-qrcode-route-probe.json";

function ensureDir(filePath) {
  fs.mkdirSync(path.dirname(path.resolve(filePath)), { recursive: true });
}

function shellQuote(value) {
  return `'${String(value).replace(/'/g, "'\\''")}'`;
}

function runRemote(command, input) {
  const commonArgs = [
    "-o",
    "StrictHostKeyChecking=no",
    "-o",
    "UserKnownHostsFile=/dev/null",
  ];
  if (dbSshPassword) {
    return execFileSync("sshpass", ["-e", "ssh", ...commonArgs, dbSshTarget, command], {
      input,
      encoding: "utf8",
      env: { ...process.env, SSHPASS: dbSshPassword },
      stdio: ["pipe", "pipe", "pipe"],
    });
  }
  return execFileSync("ssh", ["-o", "BatchMode=yes", ...commonArgs, dbSshTarget, command], {
    input,
    encoding: "utf8",
    stdio: ["pipe", "pipe", "pipe"],
  });
}

function runSql(sql) {
  const command = [
    "docker",
    "exec",
    "-i",
    shellQuote(dbContainer),
    "psql",
    "-U",
    shellQuote(dbUser),
    "-d",
    shellQuote(dbName),
    "-v",
    "ON_ERROR_STOP=1",
    "-AtF",
    shellQuote("|"),
  ].join(" ");
  return runRemote(command, sql).trim();
}

function parseRows(raw) {
  return raw
    .split(/\r?\n/)
    .filter(Boolean)
    .map((line) => line.split("|"));
}

async function readJsonSafe(response) {
  const text = await response.text();
  try {
    return { json: JSON.parse(text), text };
  } catch (_error) {
    return { json: null, text };
  }
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
      return { ticket, status: response.status() };
    }
    errors.push({ status: response.status(), body: parsed.text.slice(0, 500) });
  }
  throw new Error(`Login failed for ${username}: ${JSON.stringify(errors)}`);
}

function collectDatabaseEvidence() {
  const raw = runSql(`
SELECT 'tableCount', 'public.baseset_printers', count(*)::text FROM public.baseset_printers;
SELECT 'tableCount', 'public.baseset_qr_code_types', count(*)::text FROM public.baseset_qr_code_types;
SELECT 'tableCount', 'public.baseset_qr_detail_infos', count(*)::text FROM public.baseset_qr_detail_infos;
SELECT 'tableCount', 'public.printer_register', count(*)::text FROM public.printer_register;
SELECT 'task', id::text, coalesce(table_no, ''), coalesce(task_run_state, ''), coalesce(status::text, '')
FROM public.wom_produce_tasks
WHERE coalesce(valid, true) IS TRUE
ORDER BY create_time DESC NULLS LAST, id DESC
LIMIT 1;
`);
  const rows = parseRows(raw);
  const tableCounts = {};
  let selectedTask = null;
  for (const row of rows) {
    if (row[0] === "tableCount") {
      tableCounts[row[1]] = Number(row[2] || 0);
    }
    if (row[0] === "task") {
      selectedTask = {
        id: row[1],
        tableNo: row[2],
        taskRunState: row[3],
        status: row[4],
      };
    }
  }
  return {
    sql: [
      "SELECT count(*) FROM public.baseset_printers;",
      "SELECT count(*) FROM public.baseset_qr_code_types;",
      "SELECT count(*) FROM public.baseset_qr_detail_infos;",
      "SELECT count(*) FROM public.printer_register;",
      "SELECT id, table_no, task_run_state, status FROM public.wom_produce_tasks ORDER BY create_time DESC NULLS LAST, id DESC LIMIT 1;",
    ],
    tableCounts,
    selectedTask,
    rawRows: rows,
  };
}

function scanServiceJar() {
  const pattern = "printManage|generateQrCode|backfill-printInfo|QrCode|Qrcode|PackageQrcode|PrintManage|womPackageQrcode";
  const command = `
set -eu
outer=${shellQuote(serviceJarPath)}
tmp="/tmp/adp-wom-service-scan-$$.jar"
if [ ! -f "$outer" ]; then
  echo "STATUS|missing-outer-jar|$outer"
  exit 0
fi
unzip -p "$outer" BOOT-INF/lib/com.supcon.greendill.WOM.service-6.1.3.1.jar > "$tmp"
echo "STATUS|service-jar-extracted|$tmp"
echo "CLASS_MATCHES_BEGIN"
unzip -l "$tmp" | grep -Ei ${shellQuote(pattern)} | grep -v '^Archive:' || true
echo "CLASS_MATCHES_END"
echo "STRING_MATCHES_BEGIN"
strings "$tmp" | grep -Ei ${shellQuote(pattern)} || true
echo "STRING_MATCHES_END"
rm -f "$tmp"
`;
  try {
    const raw = runRemote(command);
    const classText = (raw.match(/CLASS_MATCHES_BEGIN\n([\s\S]*?)\nCLASS_MATCHES_END/) || [null, ""])[1].trim();
    const stringText = (raw.match(/STRING_MATCHES_BEGIN\n([\s\S]*?)\nSTRING_MATCHES_END/) || [null, ""])[1].trim();
    const classMatches = classText ? classText.split(/\r?\n/).filter(Boolean) : [];
    const stringMatches = stringText ? stringText.split(/\r?\n/).filter(Boolean) : [];
    return {
      checked: true,
      outerJar: serviceJarPath,
      classMatchCount: classMatches.length,
      stringMatchCount: stringMatches.length,
      classMatches: classMatches.slice(0, 80),
      stringMatches: stringMatches.slice(0, 80),
    };
  } catch (error) {
    return {
      checked: false,
      outerJar: serviceJarPath,
      error: error.message,
    };
  }
}

async function probeEndpoint(api, headers, probe) {
  const options = {
    headers,
  };
  if (probe.data !== undefined) {
    options.data = probe.data;
  }
  const response = await api[probe.method.toLowerCase()](`${baseUrl}${probe.url}`, options);
  const parsed = await readJsonSafe(response);
  return {
    method: probe.method,
    url: probe.url,
    requestPayload: probe.data || null,
    httpStatus: response.status(),
    responseCode: parsed.json && parsed.json.code,
    responseMessage: (parsed.json && parsed.json.message) || "",
    responseBodyExcerpt: parsed.text.slice(0, 800),
  };
}

async function main() {
  const databaseEvidence = collectDatabaseEvidence();
  const taskId = databaseEvidence.selectedTask && databaseEvidence.selectedTask.id;
  const api = await request.newContext({ ignoreHTTPSErrors: true });
  let loginResult;
  const endpointProbes = [];
  try {
    loginResult = await login(api);
    const headers = {
      Accept: "application/json, text/plain, */*",
      "Content-Type": "application/json;charset=UTF-8",
      Authorization: `Bearer ${loginResult.ticket}`,
      ticket: loginResult.ticket,
    };
    const generatedPayload = {
      taskId: taskId && Number.isSafeInteger(Number(taskId)) ? Number(taskId) : taskId,
      manuDate: "2026-06-21",
      approveDate: "2027-06-21",
      printId: 1,
      printCount: 1,
    };
    const probes = [
      { method: "GET", url: "/msService/WOM/printManage/printDate/generateCode" },
      { method: "POST", url: "/msService/WOM/printManage/generateQrCode", data: generatedPayload },
      {
        method: "POST",
        url: "/msService/WOM/printManage/backfill-printInfo",
        data: [{ isPrint: 1, detail: "ADP_E2E_QRCODE_ROUTE_PROBE" }],
      },
    ];
    for (const probe of probes) {
      endpointProbes.push(await probeEndpoint(api, headers, probe));
    }
  } finally {
    await api.dispose();
  }

  const serviceJarScan = scanServiceJar();
  const allEndpoints404 = endpointProbes.length === 3 && endpointProbes.every((probe) => probe.httpStatus === 404);
  const noJarMatches =
    serviceJarScan.checked &&
    serviceJarScan.classMatchCount === 0 &&
    serviceJarScan.stringMatchCount === 0;
  const status = allEndpoints404 && noJarMatches ? "BLOCKED" : "NEEDS_REVIEW";
  const report = {
    schemaVersion: 1,
    reportKind: "wom-qrcode-route-probe",
    generatedAt: new Date().toISOString(),
    database: "PostgreSQL",
    baseUrl,
    dbSshTarget,
    login: {
      username,
      status: loginResult && loginResult.status,
      tokenCaptured: Boolean(loginResult && loginResult.ticket),
    },
    databaseEvidence,
    endpointProbes,
    serviceJarScan,
    summary: {
      status,
      allEndpoints404,
      noServiceJarMatches: noJarMatches,
      missingPrinterConfig: (databaseEvidence.tableCounts["public.baseset_printers"] || 0) === 0,
      qrRowsBeforeProbe: databaseEvidence.tableCounts["public.baseset_qr_detail_infos"] || 0,
    },
    conclusion:
      status === "BLOCKED"
        ? "The WOM toolbar button references printManage QR endpoints, but the current runtime returns 404 and the WOM service jar contains no matching controller/string evidence. This is a missing runtime implementation/package gap, not a proven PostgreSQL persistence path."
        : "The QR route probe did not match the known missing-endpoint signature. Review endpoint responses and service jar scan before changing toolbar coverage.",
  };

  ensureDir(outputPath);
  fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`, "utf8");
  console.log(`WOM QR code route probe wrote ${outputPath} with status=${status}`);
}

main().catch((error) => {
  console.error(error.stack || error);
  process.exit(1);
});
