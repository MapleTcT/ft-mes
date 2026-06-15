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

require_var() {
  name=$1
  eval "value=\${$name:-}"
  if [ -z "$value" ]; then
    echo "Missing required environment variable: $name" >&2
    exit 2
  fi
}

require_var REPORT_DIR
require_var PGHOST
require_var PGPORT
require_var PGDATABASE
require_var PGUSER

if ! command -v psql >/dev/null 2>&1; then
  echo "psql is required for target PostgreSQL preflight" >&2
  exit 2
fi

if [ ! -f "$TABLE_LIST" ]; then
  echo "Table list not found: $TABLE_LIST" >&2
  exit 2
fi

mkdir -p "$REPORT_DIR"

SCHEMA_REPORT="$REPORT_DIR/target-schema-inventory.txt"
ROWCOUNT_REPORT="$REPORT_DIR/target-row-counts.tsv"

echo "Writing target schema inventory to $SCHEMA_REPORT"
psql -v ON_ERROR_STOP=1 \
  -f "$BASE_DIR/sql/target-schema-inventory.sql" \
  >"$SCHEMA_REPORT"

printf 'table_name\trow_count\tstatus\n' >"$ROWCOUNT_REPORT"
while IFS= read -r raw_table || [ -n "$raw_table" ]; do
  table=$(printf '%s' "$raw_table" | sed 's/[[:space:]]*$//')
  case "$table" in
    ''|\#*) continue ;;
  esac

  case "$table" in
    *[!A-Za-z0-9_\".]*)
      printf '%s\t\tINVALID_TABLE_NAME\n' "$table" >>"$ROWCOUNT_REPORT"
      continue
      ;;
  esac

  if count=$(psql -v ON_ERROR_STOP=1 -Atqc "select count(*) from $table" 2>/tmp/adp-prod-migration-preflight.err); then
    printf '%s\t%s\tOK\n' "$table" "$count" >>"$ROWCOUNT_REPORT"
  else
    error=$(tr '\n' ' ' </tmp/adp-prod-migration-preflight.err)
    printf '%s\t\tERROR: %s\n' "$table" "$error" >>"$ROWCOUNT_REPORT"
  fi
done <"$TABLE_LIST"

rm -f /tmp/adp-prod-migration-preflight.err
echo "Writing target row counts to $ROWCOUNT_REPORT"
echo "Target preflight complete."
