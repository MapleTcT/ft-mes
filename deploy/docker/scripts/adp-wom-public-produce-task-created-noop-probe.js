#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { execFileSync } = require("child_process");
const { request } = require("playwright");

const baseUrl = (process.env.ADP_BASE_URL || "http://100.99.133.43:18080").replace(/\/+$/, "");
const dbSshTarget = process.env.ADP_DB_SSH_TARGET || "v6@100.99.133.43";
const dbSshPassword = process.env.ADP_DB_SSH_PASSWORD || "";
const dbContainer = process.env.ADP_DB_CONTAINER || "adp-mes-newbase-postgres-1";
const dbName = process.env.ADP_DB_NAME || "adp";
const dbUser = process.env.ADP_DB_USER || "adp";
const nowToken = new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14);
const marker = process.env.ADP_E2E_MARKER || `ADP_E2E_${nowToken}_PUBLIC_PRODUCE_NOOP`;
const outputPath =
  process.env.ADP_WOM_PUBLIC_PRODUCE_TASK_CREATED_NOOP_OUTPUT ||
  "metadata/wom-public-produce-task-created-noop-probe.json";
const endpoint = "/msService/public/WOM/produceTask/produceTask/produceTaskCreated";

function ensureDir(filePath) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
}

function shellQuote(value) {
  return `'${String(value).replace(/'/g, "'\\''")}'`;
}

function sqlLiteral(value) {
  return `'${String(value).replace(/'/g, "''")}'`;
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

function verificationSql() {
  const likeMarker = `%${marker}%`;
  return `
SELECT count(*)::text
FROM public.wom_produce_tasks
WHERE coalesce(table_no::text, '') LIKE ${sqlLiteral(likeMarker)}
   OR coalesce(produce_batch_num::text, '') LIKE ${sqlLiteral(likeMarker)}
   OR coalesce(day_plan_ids::text, '') LIKE ${sqlLiteral(likeMarker)};
`;
}

function markerCount() {
  const raw = runSql(verificationSql());
  return Number(raw || "0");
}

function buildProbePayload() {
  return [
    {
      userName: "admin",
      companyCode: "default",
      orderInfos: {
        sourceIds: `${marker}_SOURCE`,
        sourceRatio: "1",
        sourceType: "1",
        creatStaffCode: "admin",
        prodCode: `${marker}_PRODUCT`,
        planNum: "1",
        batchcode: `${marker}_BATCH`,
        packageInfos: [],
      },
    },
  ];
}

async function readResponse(response) {
  const text = await response.text();
  let json = null;
  try {
    json = JSON.parse(text);
  } catch (_error) {
    json = null;
  }
  return { text, json };
}

function classify(report) {
  const responseCode = report.responseJson && report.responseJson.code;
  const responseMessage = report.responseJson && report.responseJson.message;
  const countUnchanged = report.beforeCount === report.afterCount;
  const falseSuccess = report.httpStatus === 200 && responseCode === 200 && countUnchanged;
  if (falseSuccess) {
    return {
      status: "FAIL_BACKLOG_CONFIRMED",
      conclusion:
        "Endpoint still returns HTTP 200/code=200 but wom_produce_tasks marker count remains unchanged. Keep PROD-ACTION-007 open.",
      exitCode: 0,
    };
  }
  if (report.afterCount > report.beforeCount) {
    return {
      status: "UNEXPECTED_PERSISTENCE_REQUIRES_RECLASSIFICATION",
      conclusion:
        "Endpoint inserted marker data. Reclassify the public produceTaskCreated contract with product-approved payload evidence before marking PASS.",
      exitCode: 2,
    };
  }
  if (report.httpStatus !== 200 || responseCode !== 200) {
    return {
      status: "EXPLICIT_REJECTION_NO_PERSISTENCE",
      conclusion: `Endpoint no longer returns false success; HTTP ${report.httpStatus}, code=${responseCode}, message=${responseMessage || ""}. Update backlog classification after product review.`,
      exitCode: 0,
    };
  }
  return {
    status: "UNKNOWN_NO_PERSISTENCE",
    conclusion:
      "Endpoint response shape changed without marker persistence. Review response body and update the acceptance ledger.",
    exitCode: 1,
  };
}

async function main() {
  ensureDir(outputPath);
  const api = await request.newContext({ ignoreHTTPSErrors: true });
  const report = {
    schemaVersion: 1,
    reportKind: "wom-public-produce-task-created-noop-probe",
    generatedAt: new Date().toISOString(),
    database: "PostgreSQL",
    baseUrl,
    endpoint: `${baseUrl}${endpoint}`,
    marker,
    method: "POST",
    requestShape: "minimal daily-plan-like marker payload; not product-accepted",
    payload: buildProbePayload(),
    verificationSql: verificationSql().trim(),
    beforeCount: null,
    httpStatus: null,
    responseText: "",
    responseJson: null,
    afterCount: null,
    status: "RUNNING",
    conclusion: "",
  };

  try {
    report.beforeCount = markerCount();
    const response = await api.post(report.endpoint, {
      data: report.payload,
      headers: {
        Accept: "application/json, text/plain, */*",
        "Content-Type": "application/json;charset=UTF-8",
      },
      timeout: Number(process.env.ADP_API_TIMEOUT_MS || 30000),
    });
    report.httpStatus = response.status();
    const parsed = await readResponse(response);
    report.responseText = parsed.text.slice(0, 2000);
    report.responseJson = parsed.json;
    report.afterCount = markerCount();
    const classification = classify(report);
    report.status = classification.status;
    report.conclusion = classification.conclusion;
    fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`);
    if (classification.exitCode !== 0) {
      console.error(`${report.status}: ${report.conclusion}`);
      console.error(`Report: ${outputPath}`);
      process.exit(classification.exitCode);
    }
    console.log(`${report.status}: ${outputPath}`);
  } catch (error) {
    report.status = "ERROR";
    report.conclusion = error && error.stack ? error.stack : String(error);
    fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`);
    throw error;
  } finally {
    await api.dispose();
  }
}

main();
