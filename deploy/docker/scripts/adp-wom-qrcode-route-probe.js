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
  "/home/v6/adp-mes-docker-newbase-20260611-181921/runtime/bap-server/module-Server/WOMPrint/manual/wom-print.jar";
const outputPath =
  process.env.ADP_WOM_QRCODE_ROUTE_PROBE_OUTPUT ||
  "metadata/wom-qrcode-persistence-acceptance.json";
const testManufactureDate = process.env.ADP_WOM_QRCODE_TEST_DATE || "2099-12-30";
const testExpiryDate = process.env.ADP_WOM_QRCODE_TEST_EXPIRY || "2100-12-30";

function ensureDir(filePath) {
  fs.mkdirSync(path.dirname(path.resolve(filePath)), { recursive: true });
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

function parseRows(raw) {
  return String(raw || "")
    .split(/\r?\n/)
    .filter((line) => line && !/^(INSERT|UPDATE|DELETE)\s+\d+/i.test(line))
    .map((line) => line.split("|"));
}

async function readJson(response) {
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
    const parsed = await readJson(response);
    const ticket = response.ok() ? findTicket(parsed.json) : null;
    if (ticket) {
      return { ticket, status: response.status() };
    }
    errors.push({ status: response.status(), body: parsed.text.slice(0, 300) });
  }
  throw new Error(`Login failed for ${username}: ${JSON.stringify(errors)}`);
}

function selectTask() {
  const rows = parseRows(runSql(`
SELECT t.id::text,
       COALESCE(t.table_no, ''),
       COALESCE(t.produce_batch_num, ''),
       COALESCE(m.code, ''),
       COALESCE(m.name, '')
FROM public.wom_produce_tasks t
JOIN public.baseset_materials m ON m.id = t.product_id
WHERE COALESCE(t.valid, TRUE) IS TRUE
  AND COALESCE(t.produce_batch_num, '') <> ''
  AND COALESCE(m.code, '') <> ''
ORDER BY CASE WHEN t.table_no LIKE 'ADP_E2E_%' THEN 0 ELSE 1 END,
         t.create_time DESC NULLS LAST,
         t.id DESC
LIMIT 1;
`));
  if (!rows.length || rows[0].length < 5) {
    throw new Error("No valid WOM production task with batch and material was found");
  }
  return {
    id: rows[0][0],
    tableNo: rows[0][1],
    batchNo: rows[0][2],
    productCode: rows[0][3],
    productName: rows[0][4],
  };
}

function sequenceSnapshot() {
  const rows = parseRows(runSql(`
SELECT last_sequence::text
FROM public.wom_qrcode_daily_sequences
WHERE tenant_id = 'default' AND manufacture_date = ${sqlLiteral(testManufactureDate)}::date;
`));
  return {
    existed: rows.length > 0,
    lastSequence: rows.length ? Number(rows[0][0]) : null,
  };
}

function markerRows(requestId) {
  return parseRows(runSql(`
SELECT request_id,
       request_hash,
       sequence_no::text,
       task_id::text,
       qr_code,
       qr_content,
       detail,
       is_print::text,
       print_count::text,
       COALESCE(printed_at::text, '')
FROM public.wom_package_qrcodes
WHERE tenant_id = 'default' AND request_id = ${sqlLiteral(requestId)}
ORDER BY sequence_no;
`));
}

function cleanup(requestId, snapshot) {
  const sequenceRestore = snapshot.existed
    ? `UPDATE public.wom_qrcode_daily_sequences
       SET last_sequence = ${Number(snapshot.lastSequence)}, updated_at = CURRENT_TIMESTAMP
       WHERE tenant_id = 'default' AND manufacture_date = ${sqlLiteral(testManufactureDate)}::date;`
    : `DELETE FROM public.wom_qrcode_daily_sequences
       WHERE tenant_id = 'default' AND manufacture_date = ${sqlLiteral(testManufactureDate)}::date;`;
  runSql(`
DELETE FROM public.wom_package_qrcodes
WHERE tenant_id = 'default' AND request_id = ${sqlLiteral(requestId)};
${sequenceRestore}
`);
  return markerRows(requestId).length;
}

function scanServiceJar() {
  const command = `
set -eu
jar_path=${shellQuote(serviceJarPath)}
if [ ! -f "$jar_path" ]; then
  printf 'missing|%s\n' "$jar_path"
  exit 0
fi
printf 'sha256|'
sha256sum "$jar_path" | awk '{print $1}'
printf 'controller|'
unzip -l "$jar_path" | grep -c 'WomPrintController.class' || true
printf 'service|'
unzip -l "$jar_path" | grep -c 'WomQrCodeService.class' || true
printf 'page|'
unzip -l "$jar_path" | grep -c 'wom-qr-generate.html' || true
`;
  const rows = parseRows(runRemote(command));
  const result = { path: serviceJarPath, exists: true };
  for (const row of rows) {
    if (row[0] === "missing") {
      return { path: serviceJarPath, exists: false };
    }
    if (row[0] === "sha256") result.sha256 = row[1];
    if (row[0] === "controller") result.controllerCount = Number(row[1]);
    if (row[0] === "service") result.serviceCount = Number(row[1]);
    if (row[0] === "page") result.pageCount = Number(row[1]);
  }
  return result;
}

async function callJson(api, headers, method, url, payload) {
  const options = { headers };
  if (payload !== undefined) options.data = payload;
  const response = await api[method.toLowerCase()](`${baseUrl}${url}`, options);
  const parsed = await readJson(response);
  return {
    method,
    url,
    requestPayload: payload === undefined ? null : payload,
    httpStatus: response.status(),
    responseCode: parsed.json && parsed.json.code,
    responseMessage: parsed.json && (parsed.json.message || parsed.json.msg),
    data: parsed.json && parsed.json.data,
    responseBodyExcerpt: parsed.text.slice(0, 600),
  };
}

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

function qrCodeFromDetail(detail) {
  return String(detail || "").split(",")[1] || "";
}

function detailMatchesContract(detail, task) {
  const fields = String(detail || "").split(",");
  return fields.length === 6 &&
    fields[0] === task.batchNo &&
    /^991230\d{5}$/.test(fields[1]) &&
    fields[2] === task.productCode &&
    fields[3] === testManufactureDate &&
    fields[4] === testExpiryDate &&
    fields[5] === "G0001";
}

async function main() {
  const generatedAt = new Date();
  const marker = `ADP-WOM-E2E-${generatedAt.toISOString().replace(/[-:.TZ]/g, "").slice(0, 14)}-${process.pid}`;
  const task = selectTask();
  const beforeSequence = sequenceSnapshot();
  const endpointEvidence = [];
  const checks = [];
  const api = await request.newContext({ ignoreHTTPSErrors: true });
  let loginResult = null;
  let rowsAfterGenerate = [];
  let rowsAfterBackfill = [];
  let rowsAfterCleanup = null;
  let failure = null;

  function check(name, condition, evidence) {
    checks.push({ name, status: condition ? "PASS" : "FAIL", evidence });
    assert(condition, `${name}: ${evidence}`);
  }

  try {
    loginResult = await login(api);
    const headers = {
      Accept: "application/json, text/plain, */*",
      "Content-Type": "application/json;charset=UTF-8",
      Authorization: `Bearer ${loginResult.ticket}`,
      Cookie: `suposTicket=${encodeURIComponent(loginResult.ticket)}`,
      langu_code: "zh_CN",
      "Accept-Language": "zh-CN",
      ticket: loginResult.ticket,
    };

    const pageResponse = await api.get(
      `${baseUrl}/msService/WOM/printManage/printDate/generateCode?taskId=${encodeURIComponent(task.id)}`,
      { headers: { ...headers, Accept: "text/html" } },
    );
    const pageText = await pageResponse.text();
    endpointEvidence.push({
      method: "GET",
      url: "/msService/WOM/printManage/printDate/generateCode",
      httpStatus: pageResponse.status(),
      contentType: pageResponse.headers()["content-type"] || "",
      responseBodyExcerpt: pageText.slice(0, 300),
    });
    check(
      "QR generation page",
      pageResponse.status() === 200 && /二维码生成|生成二维码/.test(pageText),
      `HTTP ${pageResponse.status()}, content-type=${pageResponse.headers()["content-type"] || ""}`,
    );

    const payload = {
      taskId: task.id,
      manuDate: testManufactureDate,
      approveDate: testExpiryDate,
      printCount: 2,
      requestId: marker,
    };
    const generated = await callJson(
      api,
      headers,
      "POST",
      "/msService/WOM/printManage/generateQrCode",
      payload,
    );
    endpointEvidence.push(generated);
    check(
      "Generate API response",
      generated.httpStatus === 200 && generated.responseCode === 200 && Array.isArray(generated.data) && generated.data.length === 2,
      `HTTP ${generated.httpStatus}, code=${generated.responseCode}, rows=${Array.isArray(generated.data) ? generated.data.length : 0}`,
    );
    check(
      "Legacy QR detail contract",
      generated.data.every((detail) => detailMatchesContract(detail, task)),
      generated.data.join(" ; "),
    );

    rowsAfterGenerate = markerRows(marker);
    check(
      "PostgreSQL insert",
      rowsAfterGenerate.length === 2 && rowsAfterGenerate.every((row) => row[5] === row[6]),
      `wom_package_qrcodes rows=${rowsAfterGenerate.length}, qr_content equals detail`,
    );

    const repeated = await callJson(
      api,
      headers,
      "POST",
      "/msService/WOM/printManage/generateQrCode",
      payload,
    );
    endpointEvidence.push(repeated);
    const rowsAfterRepeat = markerRows(marker);
    check(
      "Request idempotency",
      repeated.responseCode === 200 &&
        JSON.stringify(repeated.data) === JSON.stringify(generated.data) &&
        rowsAfterRepeat.length === 2,
      `repeat code=${repeated.responseCode}, rows remain ${rowsAfterRepeat.length}`,
    );

    const conflicting = await callJson(
      api,
      headers,
      "POST",
      "/msService/WOM/printManage/generateQrCode",
      { ...payload, printCount: 1 },
    );
    endpointEvidence.push(conflicting);
    check(
      "Idempotency conflict",
      conflicting.httpStatus === 200 && conflicting.responseCode === 409 && markerRows(marker).length === 2,
      `HTTP ${conflicting.httpStatus}, code=${conflicting.responseCode}, persisted rows=2`,
    );

    const firstDetail = generated.data[0];
    const firstQrCode = qrCodeFromDetail(firstDetail);
    const imageResponse = await api.get(
      `${baseUrl}/msService/WOM/printManage/qrcode/${encodeURIComponent(firstQrCode)}.png?size=256`,
      { headers: { ...headers, Accept: "image/png" } },
    );
    const imageBody = await imageResponse.body();
    endpointEvidence.push({
      method: "GET",
      url: `/msService/WOM/printManage/qrcode/${firstQrCode}.png?size=256`,
      httpStatus: imageResponse.status(),
      contentType: imageResponse.headers()["content-type"] || "",
      byteLength: imageBody.length,
      pngSignature: imageBody.slice(0, 8).toString("hex"),
    });
    check(
      "Server-side QR PNG",
      imageResponse.status() === 200 &&
        (imageResponse.headers()["content-type"] || "").startsWith("image/png") &&
        imageBody.slice(0, 8).toString("hex") === "89504e470d0a1a0a",
      `HTTP ${imageResponse.status()}, bytes=${imageBody.length}, signature=${imageBody.slice(0, 8).toString("hex")}`,
    );

    const backfill = await callJson(
      api,
      headers,
      "POST",
      "/msService/WOM/printManage/backfill-printInfo",
      [{ isPrint: 1, detail: firstDetail }],
    );
    endpointEvidence.push(backfill);
    const repeatedBackfill = await callJson(
      api,
      headers,
      "POST",
      "/msService/WOM/printManage/backfill-printInfo",
      [{ isPrint: 1, detail: firstDetail }],
    );
    endpointEvidence.push(repeatedBackfill);
    rowsAfterBackfill = markerRows(marker);
    const printedRow = rowsAfterBackfill.find((row) => row[6] === firstDetail);
    check(
      "Print-state backfill",
      backfill.responseCode === 200 &&
        backfill.data && backfill.data.updated === 1 &&
        repeatedBackfill.responseCode === 200 &&
        repeatedBackfill.data && repeatedBackfill.data.updated === 1 &&
        printedRow && printedRow[7] === "true" && printedRow[8] === "1" && printedRow[9],
      `first code=${backfill.responseCode}, replay code=${repeatedBackfill.responseCode}, is_print=${printedRow && printedRow[7]}, print_count after replay=${printedRow && printedRow[8]}`,
    );

    const records = await callJson(
      api,
      headers,
      "GET",
      `/msService/WOM/printManage/records?taskId=${encodeURIComponent(task.id)}&limit=100`,
    );
    endpointEvidence.push(records);
    check(
      "Task record query",
      records.responseCode === 200 &&
        Array.isArray(records.data) &&
        records.data.filter((row) => row.requestId === marker).length === 2,
      `code=${records.responseCode}, marker rows=${Array.isArray(records.data) ? records.data.filter((row) => row.requestId === marker).length : 0}`,
    );
  } catch (error) {
    failure = error;
  } finally {
    try {
      rowsAfterCleanup = cleanup(marker, beforeSequence);
      checks.push({
        name: "Marker cleanup and sequence restore",
        status: rowsAfterCleanup === 0 ? "PASS" : "FAIL",
        evidence: `remaining marker rows=${rowsAfterCleanup}, prior sequence=${JSON.stringify(beforeSequence)}`,
      });
      if (rowsAfterCleanup !== 0 && !failure) {
        failure = new Error(`Marker cleanup failed: ${rowsAfterCleanup} rows remain`);
      }
    } catch (cleanupError) {
      checks.push({
        name: "Marker cleanup and sequence restore",
        status: "FAIL",
        evidence: cleanupError.message,
      });
      if (!failure) failure = cleanupError;
    }
    await api.dispose();
  }

  const serviceJar = scanServiceJar();
  const report = {
    schemaVersion: 2,
    reportKind: "wom-qrcode-persistence-acceptance",
    generatedAt: generatedAt.toISOString(),
    database: "PostgreSQL",
    baseUrl,
    dbSshTarget,
    marker,
    task,
    login: {
      username,
      status: loginResult && loginResult.status,
      tokenCaptured: Boolean(loginResult && loginResult.ticket),
    },
    requestContract: {
      method: "POST",
      endpoint: "/msService/WOM/printManage/generateQrCode",
      uniqueCodeRule: "yyMMdd + five-digit PostgreSQL transaction-locked daily sequence",
      detailRule: "batch,uniqueCode,materialCode,manufactureDate,expiryDate,G0001",
      targetTables: ["public.wom_qrcode_daily_sequences", "public.wom_package_qrcodes"],
    },
    verificationSql: [
      "SELECT request_id, request_hash, sequence_no, task_id, qr_code, qr_content, detail, is_print, print_count, printed_at FROM public.wom_package_qrcodes WHERE tenant_id = 'default' AND request_id = :marker ORDER BY sequence_no;",
      "SELECT last_sequence FROM public.wom_qrcode_daily_sequences WHERE tenant_id = 'default' AND manufacture_date = :manufactureDate;",
    ],
    beforeSequence,
    rowsAfterGenerate,
    rowsAfterBackfill,
    rowsAfterCleanup,
    endpointEvidence,
    serviceJar,
    checks,
    summary: {
      status: failure ? "FAIL" : "PASS",
      pass: checks.filter((item) => item.status === "PASS").length,
      fail: checks.filter((item) => item.status === "FAIL").length,
      generatedRows: rowsAfterGenerate.length,
      markerRowsAfterCleanup: rowsAfterCleanup,
    },
    issues: failure ? [failure.message] : [],
    conclusion: failure
      ? "WOM QR page/API/PostgreSQL persistence acceptance failed; inspect the failing check and endpoint evidence."
      : "WOM QR generation, legacy payload, idempotency, conflict handling, PNG rendering, replay-safe print-state backfill, task query, marker cleanup, and sequence restore all passed against PostgreSQL.",
  };

  ensureDir(outputPath);
  fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`, "utf8");
  console.log(`WOM QR persistence acceptance wrote ${outputPath} with status=${report.summary.status}`);
  if (failure) {
    throw failure;
  }
}

main().catch((error) => {
  console.error(error.stack || error);
  process.exit(1);
});
