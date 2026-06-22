#!/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BASE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)

ENV_FILE=${ADP_MINIO_MIGRATION_ENV:-"$BASE_DIR/.env"}
if [ -f "$ENV_FILE" ]; then
  # shellcheck disable=SC1090
  set -a
  . "$ENV_FILE"
  set +a
fi

ROLE=${ADP_MINIO_INVENTORY_ROLE:-${MINIO_INVENTORY_ROLE:-source}}
REPORT_DIR=${ADP_MINIO_REPORT_DIR:-${MINIO_REPORT_DIR:-}}
BUCKET_LIST=${MINIO_BUCKET_LIST:-"$BASE_DIR/bucket-list.example.txt"}
MC_GLOBAL_FLAGS=${MINIO_MC_GLOBAL_FLAGS:-}

require_var() {
  name=$1
  eval "value=\${$name:-}"
  if [ -z "$value" ]; then
    echo "Missing required environment variable: $name" >&2
    exit 2
  fi
}

sanitize_bucket_name() {
  bucket=$1
  case "$bucket" in
    *[!A-Za-z0-9._-]*)
      return 1
      ;;
  esac
  return 0
}

configure_alias_if_requested() {
  alias_name=$1
  endpoint=$2
  access_key=$3
  secret_key=$4
  if [ -n "$endpoint" ] && [ -n "$access_key" ] && [ -n "$secret_key" ]; then
    # shellcheck disable=SC2086
    mc $MC_GLOBAL_FLAGS alias set "$alias_name" "$endpoint" "$access_key" "$secret_key" >/dev/null
  fi
}

case "$ROLE" in
  source)
    MINIO_ALIAS=${SOURCE_MINIO_ALIAS:-}
    configure_endpoint=${SOURCE_MINIO_ENDPOINT:-}
    configure_access_key=${SOURCE_MINIO_ACCESS_KEY:-}
    configure_secret_key=${SOURCE_MINIO_SECRET_KEY:-}
    ;;
  target)
    MINIO_ALIAS=${TARGET_MINIO_ALIAS:-}
    configure_endpoint=${TARGET_MINIO_ENDPOINT:-}
    configure_access_key=${TARGET_MINIO_ACCESS_KEY:-}
    configure_secret_key=${TARGET_MINIO_SECRET_KEY:-}
    ;;
  *)
    echo "Unsupported ADP_MINIO_INVENTORY_ROLE: $ROLE" >&2
    echo "Allowed values: source, target" >&2
    exit 2
    ;;
esac

require_var REPORT_DIR

if [ -z "$MINIO_ALIAS" ]; then
  echo "Missing required MinIO alias for role: $ROLE" >&2
  echo "Set SOURCE_MINIO_ALIAS or TARGET_MINIO_ALIAS in the secure env file." >&2
  exit 2
fi

if [ ! -f "$BUCKET_LIST" ]; then
  echo "Bucket list not found: $BUCKET_LIST" >&2
  exit 2
fi

if ! command -v mc >/dev/null 2>&1; then
  echo "MinIO Client 'mc' is required for bucket inventory" >&2
  exit 2
fi

if ! command -v python3 >/dev/null 2>&1; then
  echo "python3 is required for inventory normalization" >&2
  exit 2
fi

configure_alias_if_requested "$MINIO_ALIAS" "$configure_endpoint" "$configure_access_key" "$configure_secret_key"

mkdir -p "$REPORT_DIR/raw/$ROLE"
OBJECT_REPORT="$REPORT_DIR/${ROLE}-object-inventory.tsv"
SUMMARY_REPORT="$REPORT_DIR/${ROLE}-bucket-summary.tsv"
ERROR_FILE="$REPORT_DIR/${ROLE}-inventory-errors.log"
printf 'bucket\tobject_key\tsize\tetag\tlast_modified\tstatus\n' >"$OBJECT_REPORT"
printf 'bucket\tobject_count\ttotal_size\tstatus\n' >"$SUMMARY_REPORT"
: >"$ERROR_FILE"

while IFS= read -r raw_bucket || [ -n "$raw_bucket" ]; do
  bucket=$(printf '%s' "$raw_bucket" | sed 's/[[:space:]]*$//')
  case "$bucket" in
    ''|\#*) continue ;;
  esac

  if ! sanitize_bucket_name "$bucket"; then
    printf '%s\t\t\t\t\tINVALID_BUCKET_NAME\n' "$bucket" >>"$OBJECT_REPORT"
    printf '%s\t0\t0\tINVALID_BUCKET_NAME\n' "$bucket" >>"$SUMMARY_REPORT"
    continue
  fi

  RAW_JSON="$REPORT_DIR/raw/$ROLE/$bucket.jsonl"
  # shellcheck disable=SC2086
  if mc $MC_GLOBAL_FLAGS ls --recursive --json "$MINIO_ALIAS/$bucket" >"$RAW_JSON" 2>>"$ERROR_FILE"; then
    if ! python3 "$SCRIPT_DIR/normalize-mc-ls-json.py" \
      --bucket "$bucket" \
      --input "$RAW_JSON" \
      --object-output "$OBJECT_REPORT" \
      --summary-output "$SUMMARY_REPORT" >>"$ERROR_FILE" 2>&1; then
      echo "Failed to normalize bucket inventory: $bucket" >&2
    fi
  else
    printf '%s\t\t\t\t\tERROR\n' "$bucket" >>"$OBJECT_REPORT"
    printf '%s\t0\t0\tERROR\n' "$bucket" >>"$SUMMARY_REPORT"
  fi
done <"$BUCKET_LIST"

echo "Writing $ROLE MinIO object inventory to $OBJECT_REPORT"
echo "Writing $ROLE MinIO bucket summary to $SUMMARY_REPORT"
if [ -s "$ERROR_FILE" ]; then
  echo "MinIO $ROLE inventory completed with errors. See $ERROR_FILE" >&2
else
  rm -f "$ERROR_FILE"
fi
echo "MinIO $ROLE inventory complete."
