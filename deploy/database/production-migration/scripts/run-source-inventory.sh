#!/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BASE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)

ENV_FILE=${ADP_PROD_MIGRATION_ENV:-"$BASE_DIR/.env"}
if [ -f "$ENV_FILE" ]; then
  # shellcheck disable=SC1090
  set -a
  . "$ENV_FILE"
  set +a
fi

REPORT_DIR=${ADP_MIGRATION_REPORT_DIR:-}
TABLE_LIST=${ADP_MIGRATION_TABLE_LIST:-"$BASE_DIR/table-list.example.txt"}
SOURCE_DB_TYPE=${SOURCE_DB_TYPE:-postgresql}

require_var() {
  name=$1
  eval "value=\${$name:-}"
  if [ -z "$value" ]; then
    echo "Missing required environment variable: $name" >&2
    exit 2
  fi
}

sanitize_table_name() {
  table=$1
  case "$table" in
    *[!A-Za-z0-9_\".]*)
      return 1
      ;;
  esac
  return 0
}

run_postgres_count() {
  table=$1
  PGHOST=$SOURCE_PGHOST \
    PGPORT=$SOURCE_PGPORT \
    PGDATABASE=$SOURCE_PGDATABASE \
    PGUSER=$SOURCE_PGUSER \
    PGPASSWORD=${SOURCE_PGPASSWORD:-} \
    PGSSLMODE=${SOURCE_PGSSLMODE:-prefer} \
    psql -v ON_ERROR_STOP=1 -Atqc "select count(*) from $table"
}

run_oracle_count() {
  table=$1
  sqlplus -s "$SOURCE_ORACLE_CONNECT" <<SQL
set heading off feedback off pagesize 0 verify off echo off
whenever sqlerror exit failure
select count(*) from $table;
exit
SQL
}

require_var REPORT_DIR
require_var SOURCE_DB_TYPE

if [ ! -f "$TABLE_LIST" ]; then
  echo "Table list not found: $TABLE_LIST" >&2
  exit 2
fi

case "$SOURCE_DB_TYPE" in
  postgresql|postgres)
    require_var SOURCE_PGHOST
    require_var SOURCE_PGPORT
    require_var SOURCE_PGDATABASE
    require_var SOURCE_PGUSER
    if ! command -v psql >/dev/null 2>&1; then
      echo "psql is required for PostgreSQL source inventory" >&2
      exit 2
    fi
    ;;
  oracle-legacy)
    require_var SOURCE_ORACLE_CONNECT
    if ! command -v sqlplus >/dev/null 2>&1; then
      echo "sqlplus is required for oracle-legacy source inventory" >&2
      exit 2
    fi
    ;;
  *)
    echo "Unsupported SOURCE_DB_TYPE: $SOURCE_DB_TYPE" >&2
    echo "Allowed values: postgresql, postgres, oracle-legacy" >&2
    exit 2
    ;;
esac

mkdir -p "$REPORT_DIR"

SOURCE_REPORT="$REPORT_DIR/source-row-counts.tsv"
ERROR_FILE="$REPORT_DIR/source-inventory-errors.log"
: >"$ERROR_FILE"

printf 'table_name\trow_count\tstatus\n' >"$SOURCE_REPORT"
while IFS= read -r raw_table || [ -n "$raw_table" ]; do
  table=$(printf '%s' "$raw_table" | sed 's/[[:space:]]*$//')
  case "$table" in
    ''|\#*) continue ;;
  esac

  if ! sanitize_table_name "$table"; then
    printf '%s\t\tINVALID_TABLE_NAME\n' "$table" >>"$SOURCE_REPORT"
    continue
  fi

  if [ "$SOURCE_DB_TYPE" = "oracle-legacy" ]; then
    if count=$(run_oracle_count "$table" 2>>"$ERROR_FILE" | tr -d '[:space:]'); then
      printf '%s\t%s\tOK\n' "$table" "$count" >>"$SOURCE_REPORT"
    else
      printf '%s\t\tERROR\n' "$table" >>"$SOURCE_REPORT"
    fi
  else
    if count=$(run_postgres_count "$table" 2>>"$ERROR_FILE"); then
      printf '%s\t%s\tOK\n' "$table" "$count" >>"$SOURCE_REPORT"
    else
      printf '%s\t\tERROR\n' "$table" >>"$SOURCE_REPORT"
    fi
  fi
done <"$TABLE_LIST"

echo "Writing source row counts to $SOURCE_REPORT"
if [ -s "$ERROR_FILE" ]; then
  echo "Source inventory completed with errors. See $ERROR_FILE" >&2
else
  rm -f "$ERROR_FILE"
fi
echo "Source inventory complete."
