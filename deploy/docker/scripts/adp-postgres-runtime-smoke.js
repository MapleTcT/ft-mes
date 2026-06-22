#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { spawnSync } = require("child_process");

const repoRoot = path.resolve(__dirname, "../../..");

const sshHost = process.env.ADP_SSH_HOST || "100.99.133.43";
const sshUser = process.env.ADP_SSH_USER || "v6";
const sshTarget = process.env.ADP_SSH_TARGET || `${sshUser}@${sshHost}`;
const sshConnectTimeout = process.env.ADP_SSH_CONNECT_TIMEOUT || "8";
const postgresContainer = process.env.ADP_POSTGRES_CONTAINER || "adp-mes-newbase-postgres-1";
const outputPath =
  process.env.ADP_POSTGRES_RUNTIME_SMOKE_OUTPUT ||
  path.join("/tmp", `adp-postgres-runtime-smoke-${new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14)}.json`);

const expectedTables = [
  "auth_user",
  "org_person",
  "org_department",
  "rbac_role",
  "rbac_roleuser",
  "rbac_rolepermission",
  "rbac_userpermission",
  "rbac_menuinfo",
  "wfm_task_pending",
  "sys_entity",
  "sys_code",
  "ec_module",
  "ec_entity",
  "ec_model",
  "ec_field",
  "ec_view",
  "runtime_view",
  "runtime_extra_view",
  "wom_produce_tasks",
  "wom_wait_put_records",
  "wom_produce_task_exelog",
  "wom_proc_reports",
  "rm_formulas",
  "rm_process_actives",
  "qcs_inspects",
  "qcs_inspect_reports",
  "qcs_inspect_reports_sv",
  "qcs_un_qlf_deals",
  "wts_work_permits",
  "wts_work_tickets",
  "waps_work_ticket_plans",
  "baseset_batch_infos",
];

const expectedColumns = [
  ["auth_user", "error_count"],
  ["auth_user", "lock_time"],
  ["rbac_roleuser", "valid"],
  ["wfm_task_pending", "pending_source"],
  ["wom_wait_put_records", "actual_end_time"],
  ["wom_wait_put_records", "task_id"],
  ["wom_wait_put_records", "task_process_id"],
  ["wom_wait_put_records", "task_active_id"],
  ["rm_process_actives", "table_info_id"],
  ["rm_process_actives", "formula_id"],
  ["rm_process_actives", "process_id"],
  ["qcs_inspect_reports_sv", "main_obj"],
  ["qcs_inspect_stds", "inspect_id"],
  ["qcs_inspect_coms", "inspect_id"],
  ["qcs_un_qlf_deals_di", "main_obj"],
];

const expectedIndexes = [
  "idx_wfm_task_pending_user_source",
  "idx_wom_wait_put_records_task",
  "idx_wom_wait_put_records_type_state",
  "idx_wom_wait_put_records_process",
  "idx_rm_process_actives_table_info",
  "idx_rm_process_actives_process",
  "idx_qcs_inspect_stds_inspect_id",
  "idx_qcs_inspect_coms_inspect_id",
];

function ensureDir(filePath) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
}

function ensureSafeToken(value, label) {
  if (!/^[A-Za-z0-9_.:@/-]+$/.test(value)) {
    throw new Error(`${label} contains unsupported characters: ${value}`);
  }
}

function sqlString(value) {
  return `'${String(value).replace(/'/g, "''")}'`;
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

function addCheck(checks, name, passed, evidence) {
  checks.push({
    name,
    status: passed ? "PASS" : "FAIL",
    evidence,
  });
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

function parseTsv(text, columns) {
  return String(text || "")
    .split(/\n/)
    .map((line) => line.trimEnd())
    .filter(Boolean)
    .map((line) => {
      const values = line.split(/\t/);
      const row = {};
      for (let index = 0; index < columns.length; index += 1) {
        row[columns[index]] = values[index] || "";
      }
      return row;
    });
}

function valuesSql(rows) {
  return rows.map((row) => `(${row.map(sqlString).join(", ")})`).join(",\n");
}

function buildRemoteScript() {
  const tableNames = expectedTables.map(shellSingleQuote).join(" ");
  const columnValues = valuesSql(expectedColumns);
  const indexValues = valuesSql(expectedIndexes.map((indexName) => [indexName]));

  return `set -eu
DB=\${POSTGRES_DB:-adp}
USER=\${POSTGRES_USER:-adp}
PSQL="psql -v ON_ERROR_STOP=1 -U $USER -d $DB"

emit() {
  name="$1"
  printf '%s\\n' "__ADP_BEGIN__\${name}"
  cat
  printf '\\n%s\\n' "__ADP_END__\${name}"
}

$PSQL -Atqc "select json_build_object(
  'database', current_database(),
  'user', current_user,
  'serverVersion', version(),
  'serverVersionNum', current_setting('server_version_num'),
  'publicTableCount', (select count(*) from information_schema.tables where table_schema='public'),
  'publicViewCount', (select count(*) from information_schema.views where table_schema='public'),
  'databaseSizeBytes', pg_database_size(current_database()),
  'schemaCount', (select count(*) from information_schema.schemata)
)" | emit overview_json

printf 'table\\texists\\trelkind\\trow_count\\tstatus\\n' | emit table_counts_header
for table in ${tableNames}; do
  exists=$($PSQL -Atqc "select to_regclass('public.$table') is not null")
  if [ "$exists" = "t" ]; then
    relkind=$($PSQL -Atqc "select relkind from pg_class where oid=to_regclass('public.$table')")
    if count=$($PSQL -Atqc "select count(*) from public.$table" 2>/tmp/adp-postgres-runtime-smoke.err); then
      printf '%s\\ttrue\\t%s\\t%s\\tOK\\n' "$table" "$relkind" "$count"
    else
      error=$(tr '\\n' ' ' </tmp/adp-postgres-runtime-smoke.err | sed 's/\\t/ /g')
      printf '%s\\ttrue\\t%s\\t\\tERROR: %s\\n' "$table" "$relkind" "$error"
    fi
  else
    printf '%s\\tfalse\\t\\t\\tMISSING\\n' "$table"
  fi
done | emit table_counts_tsv
rm -f /tmp/adp-postgres-runtime-smoke.err

$PSQL -AtF "$(printf '\\t')" -c "with expected(table_name, column_name) as (
  values
${columnValues}
)
select e.table_name,
       e.column_name,
       case when c.column_name is null then 'false' else 'true' end as exists,
       coalesce(c.data_type, '') as data_type,
       coalesce(c.udt_name, '') as udt_name,
       coalesce(c.is_nullable, '') as is_nullable
from expected e
left join information_schema.columns c
  on c.table_schema = 'public'
 and c.table_name = e.table_name
 and c.column_name = e.column_name
order by e.table_name, e.column_name" | emit columns_tsv

$PSQL -AtF "$(printf '\\t')" -c "with expected(index_name) as (
  values
${indexValues}
)
select e.index_name,
       case when i.indexname is null then 'false' else 'true' end as exists,
       coalesce(i.tablename, '') as table_name
from expected e
left join pg_indexes i
  on i.schemaname = 'public'
 and i.indexname = e.index_name
order by e.index_name" | emit indexes_tsv

$PSQL -AtF "$(printf '\\t')" -c "select extname, extversion from pg_extension order by extname" | emit extensions_tsv
`;
}

function runRemote(script) {
  ensureSafeToken(sshConnectTimeout, "ADP_SSH_CONNECT_TIMEOUT");
  ensureSafeToken(postgresContainer, "ADP_POSTGRES_CONTAINER");
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
      postgresContainer,
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
    throw new Error(`Remote PostgreSQL smoke failed with status ${result.status}: ${result.stderr}`);
  }
  return parseDelimited(result.stdout);
}

function summarizeTableCounts(rows) {
  return {
    expected: rows.length,
    present: rows.filter((row) => row.exists === "true" && row.status === "OK").length,
    missing: rows.filter((row) => row.exists !== "true").length,
    errors: rows.filter((row) => row.status && row.status !== "OK" && row.status !== "MISSING").length,
    totalRows: rows.reduce((sum, row) => sum + Number(row.rowCount || 0), 0),
  };
}

function main() {
  ensureSafeToken(sshHost, "ADP_SSH_HOST");
  ensureSafeToken(sshUser, "ADP_SSH_USER");

  const checks = [];
  let report;
  try {
    const blocks = runRemote(buildRemoteScript());
    const overview = parseJson(blocks.get("overview_json") || "{}", {});
    const tableCounts = parseTsv(blocks.get("table_counts_tsv") || "", ["table", "exists", "relkind", "rowCount", "status"]);
    const columns = parseTsv(blocks.get("columns_tsv") || "", ["table", "column", "exists", "dataType", "udtName", "isNullable"]);
    const indexes = parseTsv(blocks.get("indexes_tsv") || "", ["index", "exists", "table"]);
    const extensions = parseTsv(blocks.get("extensions_tsv") || "", ["name", "version"]);
    const tableSummary = summarizeTableCounts(tableCounts);

    addCheck(checks, "ssh-target-reachable", true, `sshTarget=${sshTarget}`);
    addCheck(checks, "postgres-container-reachable", true, `container=${postgresContainer}`);
    addCheck(checks, "postgres-version-detected", /^PostgreSQL\b/.test(overview.serverVersion || ""), `serverVersion=${overview.serverVersion || ""}`);
    addCheck(checks, "postgres-public-table-volume", Number(overview.publicTableCount || 0) >= 1000, `publicTableCount=${overview.publicTableCount || 0}`);
    addCheck(checks, "expected-tables-present", tableSummary.missing === 0 && tableSummary.errors === 0, `present=${tableSummary.present}/${tableSummary.expected}; missing=${tableSummary.missing}; errors=${tableSummary.errors}`);
    const missingColumns = columns.filter((row) => row.exists !== "true");
    addCheck(checks, "compatibility-columns-present", missingColumns.length === 0, `present=${columns.length - missingColumns.length}/${columns.length}`);
    const missingIndexes = indexes.filter((row) => row.exists !== "true");
    addCheck(checks, "compatibility-indexes-present", missingIndexes.length === 0, `present=${indexes.length - missingIndexes.length}/${indexes.length}`);
    addCheck(checks, "postgres-extension-baseline", extensions.some((row) => row.name === "plpgsql"), `extensions=${extensions.map((row) => row.name).join(",")}`);

    const failedChecks = checks.filter((check) => check.status !== "PASS");
    report = {
      schemaVersion: 1,
      generatedAt: new Date().toISOString(),
      repoCommit: getRepoCommit(),
      database: "PostgreSQL",
      environment: {
        sshHost,
        sshUser,
        postgresContainer,
      },
      summary: {
        status: failedChecks.length === 0 ? "PASS" : "FAIL",
        totalChecks: checks.length,
        pass: checks.length - failedChecks.length,
        fail: failedChecks.length,
        publicTableCount: Number(overview.publicTableCount || 0),
        publicViewCount: Number(overview.publicViewCount || 0),
        databaseSizeBytes: Number(overview.databaseSizeBytes || 0),
        expectedTables: tableSummary.expected,
        presentExpectedTables: tableSummary.present,
        missingExpectedTables: tableSummary.missing,
        expectedColumns: columns.length,
        presentExpectedColumns: columns.length - missingColumns.length,
        expectedIndexes: indexes.length,
        presentExpectedIndexes: indexes.length - missingIndexes.length,
      },
      checks,
      overview,
      tableCounts,
      columns,
      indexes,
      extensions,
      evidence: {
        method: "SSH docker exec into PostgreSQL container and read-only psql queries against pg_catalog, information_schema and selected public tables.",
        secretHandling: "Database credentials are read from the running PostgreSQL container environment and are not printed or written to this report.",
      },
    };
  } catch (error) {
    addCheck(checks, "postgres-runtime-smoke", false, error.message);
    report = {
      schemaVersion: 1,
      generatedAt: new Date().toISOString(),
      repoCommit: getRepoCommit(),
      database: "PostgreSQL",
      environment: {
        sshHost,
        sshUser,
        postgresContainer,
      },
      summary: {
        status: "FAIL",
        totalChecks: checks.length,
        pass: checks.filter((check) => check.status === "PASS").length,
        fail: checks.filter((check) => check.status !== "PASS").length,
        publicTableCount: 0,
        publicViewCount: 0,
        databaseSizeBytes: 0,
        expectedTables: expectedTables.length,
        presentExpectedTables: 0,
        missingExpectedTables: expectedTables.length,
        expectedColumns: expectedColumns.length,
        presentExpectedColumns: 0,
        expectedIndexes: expectedIndexes.length,
        presentExpectedIndexes: 0,
      },
      checks,
      overview: {},
      tableCounts: [],
      columns: [],
      indexes: [],
      extensions: [],
      evidence: {
        method: "SSH docker exec into PostgreSQL container and read-only psql queries against pg_catalog, information_schema and selected public tables.",
        secretHandling: "Database credentials are read from the running PostgreSQL container environment and are not printed or written to this report.",
        error: error.stack || error.message,
      },
    };
  }

  ensureDir(outputPath);
  fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`);
  console.log(JSON.stringify(report.summary, null, 2));
  console.log(`PostgreSQL runtime smoke report: ${outputPath}`);
  if (report.summary.status !== "PASS") {
    process.exitCode = 1;
  }
}

main();
