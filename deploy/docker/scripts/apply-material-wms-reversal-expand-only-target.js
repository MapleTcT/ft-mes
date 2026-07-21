#!/usr/bin/env node
"use strict";

const crypto = require("node:crypto");
const fs = require("node:fs");
const path = require("node:path");
const { spawnSync } = require("node:child_process");

const repoRoot = path.resolve(__dirname, "../../..");
const sshTarget = process.env.BPI_TARGET_SSH || "v6@10.11.100.17";
const postgresContainer = process.env.BPI_TARGET_POSTGRES_CONTAINER
  || "adp-mes-newbase-postgres-1";
const database = process.env.BPI_TARGET_SYSTEM_DATABASE || "adp";
const databaseUser = process.env.BPI_TARGET_POSTGRES_USER || "adp";
const stamp = new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14);
const backupDir = process.env.BPI_MATERIAL_REVERSAL_BACKUP_DIR
  || `/data/docker/bpi-upgrade-backups/material-wms-reversal-${stamp}`;
const migrationPath = path.join(
  repoRoot,
  "deploy/docker/postgres/init/198-material-wms-completion-inbound-reversal.sql",
);
const reportPath = path.resolve(process.env.BPI_MATERIAL_REVERSAL_MIGRATION_REPORT
  || path.join(repoRoot, "metadata/bpi-material-wms-reversal-schema-upgrade.json"));

if (process.env.BPI_MATERIAL_REVERSAL_MIGRATION_CONFIRM
    !== "APPLY_EXPAND_ONLY_TO_TARGET") {
  throw new Error(
    "Set BPI_MATERIAL_REVERSAL_MIGRATION_CONFIRM=APPLY_EXPAND_ONLY_TO_TARGET",
  );
}

for (const [label, value, pattern] of [
  ["SSH target", sshTarget, /^[A-Za-z0-9._-]+@[A-Za-z0-9._-]+$/],
  ["PostgreSQL container", postgresContainer, /^[A-Za-z0-9._-]+$/],
  ["database", database, /^[A-Za-z0-9_-]+$/],
  ["database user", databaseUser, /^[A-Za-z0-9_-]+$/],
  ["backup directory", backupDir, /^\/[A-Za-z0-9._/-]+$/],
]) {
  if (!pattern.test(value)) throw new Error(`Unsafe ${label}: ${value}`);
}
if (!fs.existsSync(migrationPath)) throw new Error(`Migration is missing: ${migrationPath}`);

function shellQuote(value) {
  return `'${String(value).replace(/'/g, `'"'"'`)}'`;
}

function run(command, args, options = {}) {
  const result = spawnSync(command, args, {
    encoding: "utf8",
    maxBuffer: 16 * 1024 * 1024,
    ...options,
  });
  if (result.error) throw result.error;
  if (result.status !== 0) {
    const detail = String(result.stderr || result.stdout || "").trim().slice(-4000);
    throw new Error(`${command} exited ${result.status}: ${detail}`);
  }
  return String(result.stdout || "").trim();
}

function ssh(command, options = {}) {
  return run("ssh", [
    "-o", "BatchMode=yes",
    "-o", "ConnectTimeout=8",
    "-o", "ServerAliveInterval=15",
    "-o", "ServerAliveCountMax=4",
    sshTarget,
    command,
  ], options);
}

function psql(statement) {
  const command = [
    "docker", "exec", "-i", postgresContainer,
    "psql", "-X", "-At", "-U", databaseUser, "-d", database,
    "-v", "ON_ERROR_STOP=1",
  ].map(shellQuote).join(" ");
  return ssh(command, { input: statement });
}

function parseJson(output, label) {
  for (const line of String(output).trim().split(/\r?\n/).reverse()) {
    try {
      return JSON.parse(line);
    } catch (_error) {
      // Continue until the final PostgreSQL JSON row is found.
    }
  }
  throw new Error(`${label} did not return JSON: ${String(output).slice(-1000)}`);
}

function schemaState() {
  return parseJson(psql(`
SELECT json_build_object(
  'reversalColumn', EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = 'public' AND table_name = 'wms_stock_documents'
       AND column_name = 'reversal_of_document_id' AND data_type = 'bigint'
  ),
  'documentTypeConstraint', COALESCE((
    SELECT pg_get_constraintdef(oid) LIKE '%COMPLETION_INBOUND_REVERSAL%'
      FROM pg_constraint
     WHERE conrelid = 'public.wms_stock_documents'::regclass
       AND conname = 'ck_wms_stock_documents_type'
  ), false),
  'transactionTypeConstraint', COALESCE((
    SELECT pg_get_constraintdef(oid) LIKE '%COMPLETION_INBOUND_REVERSAL%'
      FROM pg_constraint
     WHERE conrelid = 'public.wms_inventory_transactions'::regclass
       AND conname = 'ck_wms_inventory_transactions_type'
  ), false),
  'reversalForeignKey', EXISTS (
    SELECT 1 FROM pg_constraint
     WHERE conrelid = 'public.wms_stock_documents'::regclass
       AND conname = 'fk_wms_stock_documents_reversal_original'
       AND contype = 'f'
  ),
  'reversalUniqueIndex', to_regclass(
    'public.uk_wms_stock_documents_reversal_original'
  ) IS NOT NULL,
  'unsupportedDocumentTypes', COALESCE((
    SELECT json_agg(document_type ORDER BY document_type)
      FROM (SELECT DISTINCT document_type FROM public.wms_stock_documents
             WHERE document_type NOT IN (
               'COMPLETION_INBOUND', 'COMPLETION_INBOUND_REVERSAL', 'PRODUCTION_ISSUE'
             )) unsupported
  ), '[]'::json),
  'unsupportedTransactionTypes', COALESCE((
    SELECT json_agg(transaction_type ORDER BY transaction_type)
      FROM (SELECT DISTINCT transaction_type FROM public.wms_inventory_transactions
             WHERE transaction_type NOT IN (
               'COMPLETION_INBOUND', 'COMPLETION_INBOUND_REVERSAL', 'PRODUCTION_ISSUE',
               'QUALITY_RELEASE', 'QUALITY_HOLD',
               'QUALITY_ALLOCATION_HOLD', 'QUALITY_ALLOCATION_RELEASE'
             )) unsupported
  ), '[]'::json)
);`), "material-wms schema state");
}

function schemaReady(state) {
  return state.reversalColumn
    && state.documentTypeConstraint
    && state.transactionTypeConstraint
    && state.reversalForeignKey
    && state.reversalUniqueIndex;
}

function sha256(file) {
  return crypto.createHash("sha256").update(fs.readFileSync(file)).digest("hex");
}

const report = {
  schemaVersion: 1,
  generatedAt: new Date().toISOString(),
  status: "RUNNING",
  strategy: "EXPAND_ONLY",
  target: {
    host: sshTarget.split("@")[1],
    postgresContainer,
    database,
  },
  migration: {
    path: path.relative(repoRoot, migrationPath),
    sha256: sha256(migrationPath),
  },
  backup: null,
  before: null,
  after: null,
  issues: [],
};

try {
  fs.mkdirSync(path.dirname(reportPath), { recursive: true });
  report.before = schemaState();
  if (report.before.unsupportedDocumentTypes.length
      || report.before.unsupportedTransactionTypes.length) {
    throw new Error(`Unsupported current WMS types: ${JSON.stringify({
      documents: report.before.unsupportedDocumentTypes,
      transactions: report.before.unsupportedTransactionTypes,
    })}`);
  }
  if (schemaReady(report.before)) {
    report.after = report.before;
    report.status = "PASS_ALREADY_APPLIED";
  } else {
    const backupFile = `${backupDir}/adp-material-wms-before-reversal-${stamp}.dump`;
    ssh(`test ! -e ${shellQuote(backupDir)} && mkdir -p ${shellQuote(backupDir)} && chmod 700 ${shellQuote(backupDir)}`);
    const dumpCommand = [
      "docker", "exec", postgresContainer,
      "pg_dump", "-Fc", "-U", databaseUser, "-d", database,
      "-t", "public.wms_stock_documents",
      "-t", "public.wms_stock_document_lines",
      "-t", "public.wms_inventory_transactions",
      "-t", "public.wms_batch_stocks",
    ].map(shellQuote).join(" ");
    ssh(`${dumpCommand} > ${shellQuote(backupFile)} && test -s ${shellQuote(backupFile)} && chmod 600 ${shellQuote(backupFile)}`);
    const backupSha256 = ssh(`sha256sum ${shellQuote(backupFile)} | cut -d' ' -f1`);
    report.backup = { path: backupFile, sha256: backupSha256 };
    psql(fs.readFileSync(migrationPath, "utf8"));
    report.after = schemaState();
    if (!schemaReady(report.after)) {
      throw new Error(`Schema verification failed after migration: ${JSON.stringify(report.after)}`);
    }
    report.status = "PASS_APPLIED_EXPAND_ONLY";
  }
} catch (error) {
  report.status = "FAIL";
  report.issues.push(error?.message || String(error));
  process.exitCode = 1;
} finally {
  report.generatedAt = new Date().toISOString();
  fs.writeFileSync(reportPath, `${JSON.stringify(report, null, 2)}\n`, "utf8");
  process.stdout.write(`${reportPath}\n`);
}
