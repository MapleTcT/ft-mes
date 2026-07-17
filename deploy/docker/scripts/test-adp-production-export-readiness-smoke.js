#!/usr/bin/env node
"use strict";

const assert = require("assert");
const {
  browserQueryExportEvidence,
  buildQueryExportPayload,
  detectMagic,
  redactRequestPayload,
  selectQueryExportPayload,
} = require("./adp-production-export-readiness-smoke");

const target = {
  viewCode: "TEST_view",
  queryExportPath: "/msService/TEST/list-query",
};

const capturedPayload = {
  exportFlag: true,
  properties: [{ code: "tableNo", name: "单据编号" }],
  token: "must-not-leak",
};
const exportClick = {
  verifiedDataExport: true,
  file: {
    backendRequest: {
      method: "POST",
      url: `http://127.0.0.1${target.queryExportPath}`,
      payload: redactRequestPayload(capturedPayload),
    },
    backendResponse: {
      status: 200,
      contentType: "application/vnd.ms-excel",
    },
  },
};

assert.strictEqual(detectMagic(Buffer.from("d0cf11e000000000", "hex")), "OLE_XLS");
assert.strictEqual(detectMagic(Buffer.from("504b030400000000", "hex")), "ZIP_XLSX");
assert.strictEqual(redactRequestPayload(capturedPayload).token, "<redacted>");
assert.strictEqual(browserQueryExportEvidence(target, exportClick).verified, true);

const selected = selectQueryExportPayload(target, exportClick);
assert.strictEqual(selected.source, "browser-export-request");
assert.deepStrictEqual(selected.payload.properties, capturedPayload.properties);
assert.notStrictEqual(selected.payload, exportClick.file.backendRequest.payload);

const fallback = selectQueryExportPayload(target, null);
assert.strictEqual(fallback.source, "synthetic-fallback");
assert.deepStrictEqual(fallback.payload, buildQueryExportPayload(target));

console.log("production export readiness helpers: PASS");
