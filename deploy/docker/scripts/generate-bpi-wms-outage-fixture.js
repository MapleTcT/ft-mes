#!/usr/bin/env node
"use strict";

const crypto = require("node:crypto");
const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");
const { spawnSync } = require("node:child_process");

const repoRoot = path.resolve(__dirname, "../../..");
const protoPath = path.join(
  repoRoot,
  "contracts/bpi-events/src/main/proto/bpi_events_v1.proto",
);
const messageType = "ftmes.bpi.contract.v1.WmsCompletionInboundCommandV1";
const marker = required("BPI_ACCEPTANCE_MARKER");
const commandTopic = process.env.BPI_WMS_COMMAND_TOPIC?.trim()
  || "bpi.wms.completion-inbound-command.v1";
const outputPath = path.resolve(
  process.env.BPI_FIXTURE_OUTPUT || `/tmp/${marker}-wms-outage-fixture.json`,
);

if (!/^[A-Za-z0-9_-]{8,100}$/.test(marker)) {
  throw new Error("BPI_ACCEPTANCE_MARKER must use 8-100 letters, digits, underscores or hyphens");
}
if (!/^[A-Za-z0-9._-]{1,249}$/.test(commandTopic)) {
  throw new Error("BPI_WMS_COMMAND_TOPIC is not a safe Kafka topic name");
}

function required(key) {
  const value = process.env[key];
  if (!value || !value.trim()) throw new Error(`${key} is required`);
  return value.trim();
}

function executable(file) {
  try {
    fs.accessSync(file, fs.constants.X_OK);
    return true;
  } catch (_error) {
    return false;
  }
}

function commandPath(command) {
  const lookup = spawnSync("sh", ["-c", `command -v ${command}`], { encoding: "utf8" });
  return lookup.status === 0 ? lookup.stdout.trim() : "";
}

function mavenProtocCandidates() {
  const base = path.join(os.homedir(), ".m2/repository/com/google/protobuf/protoc");
  if (!fs.existsSync(base)) return [];
  const platform = process.platform === "darwin" ? "osx" : process.platform;
  const architecture = process.arch === "x64" ? "x86_64" : process.arch === "arm64" ? "aarch_64" : process.arch;
  const preferredSuffix = `-${platform}-${architecture}.exe`;
  const platformToken = `-${platform}-`;
  const candidates = fs.readdirSync(base, { withFileTypes: true })
    .filter((entry) => entry.isDirectory())
    .sort((left, right) => right.name.localeCompare(left.name, undefined, { numeric: true }))
    .flatMap((entry) => {
      const directory = path.join(base, entry.name);
      return fs.readdirSync(directory)
        .filter((name) => name.startsWith("protoc-") && name.includes(platformToken) && name.endsWith(".exe"))
        .map((name) => path.join(directory, name));
    });
  return candidates.sort((left, right) => {
    const leftPreferred = left.endsWith(preferredSuffix) ? 1 : 0;
    const rightPreferred = right.endsWith(preferredSuffix) ? 1 : 0;
    return rightPreferred - leftPreferred;
  });
}

function locateProtoc() {
  const configured = process.env.BPI_PROTOC?.trim();
  const candidates = [configured, commandPath("protoc"), ...mavenProtocCandidates()]
    .filter(Boolean);
  const found = candidates.find(executable);
  if (!found) {
    throw new Error("protoc is required; run the BPI contract build or set BPI_PROTOC");
  }
  return found;
}

function quote(value) {
  return JSON.stringify(String(value));
}

function encode(protoc, text) {
  const result = spawnSync(protoc, [
    `--proto_path=${path.dirname(protoPath)}`,
    `--encode=${messageType}`,
    protoPath,
  ], { input: text, maxBuffer: 2 * 1024 * 1024 });
  if (result.status !== 0) {
    throw new Error(`protoc encode failed: ${String(result.stderr || "").trim()}`);
  }
  if (!result.stdout?.length) throw new Error("protoc returned an empty WMS command");
  return result.stdout;
}

function decode(protoc, payload) {
  const result = spawnSync(protoc, [
    `--proto_path=${path.dirname(protoPath)}`,
    `--decode=${messageType}`,
    protoPath,
  ], { input: payload, encoding: "utf8", maxBuffer: 2 * 1024 * 1024 });
  if (result.status !== 0) {
    throw new Error(`protoc decode failed: ${String(result.stderr || "").trim()}`);
  }
  return result.stdout;
}

function headerText(headers) {
  return Object.entries(headers)
    .map(([key, value]) => `headers { key: ${quote(key)} value: ${quote(value)} }`)
    .join("\n");
}

function main() {
  if (!fs.existsSync(protoPath)) throw new Error(`BPI proto is missing: ${protoPath}`);
  const tenantId = process.env.BPI_TENANT_ID?.trim() || "1000";
  const plantId = process.env.BPI_PLANT_ID?.trim() || "PLANT-01";
  const lineId = process.env.BPI_LINE_ID?.trim() || "LINE-S07-01";
  const quantity = process.env.BPI_QUANTITY?.trim() || "12.345000";
  const unit = process.env.BPI_QUANTITY_UNIT?.trim() || "kg";
  const ids = {
    batchId: crypto.randomUUID(),
    qualityGateId: crypto.randomUUID(),
    qualityLinkId: crypto.randomUUID(),
    commandEventId: crypto.randomUUID(),
    wmsLinkId: crypto.randomUUID(),
    commandsFlagId: crypto.randomUUID(),
    wmsFlagId: crypto.randomUUID(),
  };
  const traceId = crypto.randomUUID();
  const idempotencyKey = `${marker}|WMS|1`;
  const headers = {
    event_id: ids.commandEventId,
    idempotency_key: idempotencyKey,
    tenant_id: tenantId,
    schema_version: "v1",
    trace_id: traceId,
  };
  const requestedAtMs = Date.now();
  const text = [
    `event_id: ${quote(ids.commandEventId)}`,
    `idempotency_key: ${quote(idempotencyKey)}`,
    `tenant_id: ${quote(tenantId)}`,
    `plant_id: ${quote(plantId)}`,
    `line_id: ${quote(lineId)}`,
    `batch_id: ${quote(ids.batchId)}`,
    `batch_no: ${quote(marker)}`,
    `order_id: ${quote(`${marker}_ORDER`)}`,
    `material_code: ${quote(`${marker}_MATERIAL`)}`,
    `quantity_decimal: ${quote(quantity)}`,
    `quantity_unit: ${quote(unit)}`,
    `quality_gate_id: ${quote(`${marker}_GATE`)}`,
    "quality_gate_revision: 1",
    `requested_at_ms: ${requestedAtMs}`,
    headerText(headers),
  ].join("\n");
  const protoc = locateProtoc();
  const payload = encode(protoc, text);
  const decoded = decode(protoc, payload);
  if (!decoded.includes(ids.commandEventId) || !decoded.includes(idempotencyKey)) {
    throw new Error("WMS command failed its Protobuf round-trip identity check");
  }
  const fixture = {
    schemaVersion: 1,
    generatedAt: new Date().toISOString(),
    marker,
    scope: { tenantId, plantId, lineId },
    ids,
    command: {
      topic: commandTopic,
      partitionKey: `${tenantId}|${plantId}|${ids.batchId}`,
      idempotencyKey,
      traceId,
      quantity,
      unit,
      requestedAtMs,
      headers,
      payloadBase64: payload.toString("base64"),
      payloadSha256: crypto.createHash("sha256").update(payload).digest("hex"),
    },
    sqlVariables: {
      marker,
      batch_id: ids.batchId,
      quality_gate_id: ids.qualityGateId,
      quality_link_id: ids.qualityLinkId,
      outbox_id: ids.commandEventId,
      wms_link_id: ids.wmsLinkId,
      commands_flag_id: ids.commandsFlagId,
      wms_flag_id: ids.wmsFlagId,
      partition_key: `${tenantId}|${plantId}|${ids.batchId}`,
      payload_base64: payload.toString("base64"),
      headers_json: JSON.stringify(headers),
      command_topic: commandTopic,
    },
  };
  fs.mkdirSync(path.dirname(outputPath), { recursive: true });
  fs.writeFileSync(outputPath, `${JSON.stringify(fixture, null, 2)}\n`, "utf8");
  process.stdout.write(`${outputPath}\n`);
}

main();
