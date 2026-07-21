#!/usr/bin/env node
"use strict";

const assert = require("node:assert/strict");
const crypto = require("node:crypto");
const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");
const { execFileSync } = require("node:child_process");
const test = require("node:test");

test("generates a round-tripped WMS command fixture without secrets", () => {
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), "bpi-wms-outage-fixture-"));
  const output = path.join(directory, "fixture.json");
  try {
    execFileSync(process.execPath, [
      path.join(__dirname, "generate-bpi-wms-outage-fixture.js"),
    ], {
      env: {
        ...process.env,
        BPI_ACCEPTANCE_MARKER: "ADP_E2E_WMS_OUTAGE_FIXTURE",
        BPI_FIXTURE_OUTPUT: output,
        BPI_WMS_COMMAND_TOPIC: "bpi.acceptance.fixture.blue-command",
      },
      stdio: "pipe",
    });
    const fixture = JSON.parse(fs.readFileSync(output, "utf8"));
    const payload = Buffer.from(fixture.command.payloadBase64, "base64");
    assert.equal(fixture.schemaVersion, 1);
    assert.equal(fixture.marker, "ADP_E2E_WMS_OUTAGE_FIXTURE");
    assert.match(fixture.ids.batchId, /^[0-9a-f-]{36}$/);
    assert.equal(fixture.command.headers.event_id, fixture.ids.commandEventId);
    assert.equal(fixture.command.headers.idempotency_key, fixture.command.idempotencyKey);
    assert.equal(fixture.command.partitionKey,
      `1000|PLANT-01|${fixture.ids.batchId}`);
    assert.equal(fixture.command.topic, "bpi.acceptance.fixture.blue-command");
    assert.equal(fixture.sqlVariables.command_topic, fixture.command.topic);
    assert.equal(crypto.createHash("sha256").update(payload).digest("hex"),
      fixture.command.payloadSha256);
    for (const key of Object.keys(fixture.sqlVariables)) {
      assert.match(key, /^[a-z_][a-z0-9_]*$/);
    }
    assert.ok(payload.length > 100);
    assert.equal(JSON.stringify(fixture).includes("password"), false);
    assert.equal(JSON.stringify(fixture).includes("secret"), false);
  } finally {
    fs.rmSync(directory, { recursive: true, force: true });
  }
});
