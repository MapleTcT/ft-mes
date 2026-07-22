#!/bin/sh
set -eu

if [ "${BPI_DATASET_RECOVERY_BUCKET_BOOTSTRAP_ENABLED:-false}" != "true" ]; then
    printf 'BPI dataset recovery bucket bootstrap is disabled\n'
    exit 0
fi

: "${BPI_DATASET_RETENTION_MINIO_ENDPOINT:?BPI_DATASET_RETENTION_MINIO_ENDPOINT is required}"
: "${MINIO_ROOT_USER:?MINIO_ROOT_USER is required}"
: "${MINIO_ROOT_PASSWORD:?MINIO_ROOT_PASSWORD is required}"
: "${BPI_DATASET_RETENTION_MINIO_ACCESS_KEY:?BPI_DATASET_RETENTION_MINIO_ACCESS_KEY is required}"
: "${BPI_DATASET_RETENTION_MINIO_SECRET_KEY:?BPI_DATASET_RETENTION_MINIO_SECRET_KEY is required}"
: "${BPI_DATASET_RECOVERY_MINIO_ACCESS_KEY:?BPI_DATASET_RECOVERY_MINIO_ACCESS_KEY is required}"
: "${BPI_DATASET_RECOVERY_MINIO_SECRET_KEY:?BPI_DATASET_RECOVERY_MINIO_SECRET_KEY is required}"
: "${BPI_DATASET_MINIO_BUCKET:?BPI_DATASET_MINIO_BUCKET is required}"
: "${BPI_DATASET_RECOVERY_BUCKET:?BPI_DATASET_RECOVERY_BUCKET is required}"
: "${BPI_ICEBERG_WAREHOUSE_BUCKET:?BPI_ICEBERG_WAREHOUSE_BUCKET is required}"
: "${BPI_DATASET_RETENTION_MODE:?BPI_DATASET_RETENTION_MODE is required}"
: "${BPI_DATASET_RETENTION_DAYS:?BPI_DATASET_RETENTION_DAYS is required}"

for bucket in \
    "$BPI_DATASET_MINIO_BUCKET" \
    "$BPI_DATASET_RECOVERY_BUCKET" \
    "$BPI_ICEBERG_WAREHOUSE_BUCKET"; do
    case "$bucket" in
        ''|*[!a-z0-9.-]*)
            printf 'ERROR: BPI dataset bucket contains unsupported characters\n' >&2
            exit 1
            ;;
    esac
done

case "$BPI_DATASET_RETENTION_MODE" in
    GOVERNANCE|COMPLIANCE) ;;
    *)
        printf 'ERROR: retention mode must be GOVERNANCE or COMPLIANCE\n' >&2
        exit 1
        ;;
esac
case "$BPI_DATASET_RETENTION_DAYS" in
    ''|*[!0-9]*)
        printf 'ERROR: retention days must be an integer\n' >&2
        exit 1
        ;;
esac
if [ "$BPI_DATASET_RETENTION_DAYS" -lt 1 ] \
   || [ "$BPI_DATASET_RETENTION_DAYS" -gt 36500 ]; then
    printf 'ERROR: retention days must be between 1 and 36500\n' >&2
    exit 1
fi
if [ "$BPI_DATASET_MINIO_BUCKET" = "$BPI_DATASET_RECOVERY_BUCKET" ]; then
    printf 'ERROR: recovery bucket must be separate from the active dataset bucket\n' >&2
    exit 1
fi
if [ "$BPI_ICEBERG_WAREHOUSE_BUCKET" = "$BPI_DATASET_RECOVERY_BUCKET" ] \
   || [ "$BPI_ICEBERG_WAREHOUSE_BUCKET" = "$BPI_DATASET_MINIO_BUCKET" ]; then
    printf 'ERROR: Iceberg warehouse bucket must be separate from dataset buckets\n' >&2
    exit 1
fi
if [ "$BPI_DATASET_RETENTION_MINIO_ACCESS_KEY" = "$MINIO_ROOT_USER" ] \
   || [ "$BPI_DATASET_RETENTION_MINIO_SECRET_KEY" = "$MINIO_ROOT_PASSWORD" ]; then
    printf 'ERROR: retention archiver credentials must differ from MinIO root\n' >&2
    exit 1
fi
if [ "$BPI_DATASET_RECOVERY_MINIO_ACCESS_KEY" = "$MINIO_ROOT_USER" ] \
   || [ "$BPI_DATASET_RECOVERY_MINIO_SECRET_KEY" = "$MINIO_ROOT_PASSWORD" ] \
   || [ "$BPI_DATASET_RECOVERY_MINIO_ACCESS_KEY" = "$BPI_DATASET_RETENTION_MINIO_ACCESS_KEY" ] \
   || [ "$BPI_DATASET_RECOVERY_MINIO_SECRET_KEY" = "$BPI_DATASET_RETENTION_MINIO_SECRET_KEY" ]; then
    printf 'ERROR: recovery operator credentials must be distinct\n' >&2
    exit 1
fi

attempt=0
until mc alias set bpi-minio "$BPI_DATASET_RETENTION_MINIO_ENDPOINT" \
    "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD" >/dev/null 2>&1 \
    && mc ls bpi-minio >/dev/null 2>&1; do
    attempt=$((attempt + 1))
    if [ "$attempt" -ge 30 ]; then
        printf 'ERROR: MinIO did not become ready for recovery bucket bootstrap\n' >&2
        exit 1
    fi
    sleep 2
done

mc mb --with-lock --ignore-existing "bpi-minio/$BPI_DATASET_RECOVERY_BUCKET"
mc anonymous set none "bpi-minio/$BPI_DATASET_RECOVERY_BUCKET"
mc version enable "bpi-minio/$BPI_DATASET_RECOVERY_BUCKET"
mc retention set --default \
    "$(printf '%s' "$BPI_DATASET_RETENTION_MODE" | tr '[:upper:]' '[:lower:]')" \
    "${BPI_DATASET_RETENTION_DAYS}d" \
    "bpi-minio/$BPI_DATASET_RECOVERY_BUCKET/"

retention_info=$(mc retention info \
    "bpi-minio/$BPI_DATASET_RECOVERY_BUCKET/" --default)
case "$retention_info" in
    *"$BPI_DATASET_RETENTION_MODE"*) ;;
    *)
        printf 'ERROR: recovery bucket retention mode verification failed\n' >&2
        exit 1
        ;;
esac
case "$retention_info" in
    *"$BPI_DATASET_RETENTION_DAYS"*) ;;
    *)
        printf 'ERROR: recovery bucket retention duration verification failed\n' >&2
        exit 1
        ;;
esac

if ! mc admin user info bpi-minio \
    "$BPI_DATASET_RETENTION_MINIO_ACCESS_KEY" >/dev/null 2>&1; then
    mc admin user add bpi-minio \
        "$BPI_DATASET_RETENTION_MINIO_ACCESS_KEY" \
        "$BPI_DATASET_RETENTION_MINIO_SECRET_KEY"
fi

while IFS= read -r policy_line || [ -n "$policy_line" ]; do
    case "$policy_line" in
        *__SOURCE_BUCKET__*)
            policy_prefix=${policy_line%%__SOURCE_BUCKET__*}
            policy_suffix=${policy_line#*__SOURCE_BUCKET__}
            printf '%s%s%s\n' "$policy_prefix" "$BPI_DATASET_MINIO_BUCKET" "$policy_suffix"
            ;;
        *__RECOVERY_BUCKET__*)
            policy_prefix=${policy_line%%__RECOVERY_BUCKET__*}
            policy_suffix=${policy_line#*__RECOVERY_BUCKET__}
            printf '%s%s%s\n' "$policy_prefix" "$BPI_DATASET_RECOVERY_BUCKET" "$policy_suffix"
            ;;
        *)
            printf '%s\n' "$policy_line"
            ;;
    esac
done </work/policy.json >/tmp/bpi-dataset-retention-archiver-policy.json

unsafe_policy=false
while IFS= read -r policy_line || [ -n "$policy_line" ]; do
    case "$policy_line" in
        *DeleteObject*|*BypassGovernanceRetention*|*__SOURCE_BUCKET__*|*__RECOVERY_BUCKET__*)
            unsafe_policy=true
            ;;
    esac
done </tmp/bpi-dataset-retention-archiver-policy.json
if [ "$unsafe_policy" = "true" ]; then
    printf 'ERROR: rendered retention policy is unsafe or incomplete\n' >&2
    exit 1
fi
mc admin policy create bpi-minio bpi-dataset-retention-archiver \
    /tmp/bpi-dataset-retention-archiver-policy.json
mc admin policy attach bpi-minio bpi-dataset-retention-archiver \
    --user "$BPI_DATASET_RETENTION_MINIO_ACCESS_KEY"

if ! mc admin user info bpi-minio \
    "$BPI_DATASET_RECOVERY_MINIO_ACCESS_KEY" >/dev/null 2>&1; then
    mc admin user add bpi-minio \
        "$BPI_DATASET_RECOVERY_MINIO_ACCESS_KEY" \
        "$BPI_DATASET_RECOVERY_MINIO_SECRET_KEY"
fi

while IFS= read -r policy_line || [ -n "$policy_line" ]; do
    case "$policy_line" in
        *__RECOVERY_BUCKET__*)
            policy_prefix=${policy_line%%__RECOVERY_BUCKET__*}
            policy_suffix=${policy_line#*__RECOVERY_BUCKET__}
            printf '%s%s%s\n' "$policy_prefix" "$BPI_DATASET_RECOVERY_BUCKET" "$policy_suffix"
            ;;
        *__WAREHOUSE_BUCKET__*)
            policy_prefix=${policy_line%%__WAREHOUSE_BUCKET__*}
            policy_suffix=${policy_line#*__WAREHOUSE_BUCKET__}
            printf '%s%s%s\n' "$policy_prefix" "$BPI_ICEBERG_WAREHOUSE_BUCKET" "$policy_suffix"
            ;;
        *)
            printf '%s\n' "$policy_line"
            ;;
    esac
done </work/recovery-policy.json >/tmp/bpi-dataset-recovery-operator-policy.json

case "$(cat /tmp/bpi-dataset-recovery-operator-policy.json)" in
    *__RECOVERY_BUCKET__*|*__WAREHOUSE_BUCKET__*)
        printf 'ERROR: rendered recovery operator policy is incomplete\n' >&2
        exit 1
        ;;
esac
mc admin policy create bpi-minio bpi-dataset-recovery-operator \
    /tmp/bpi-dataset-recovery-operator-policy.json
mc admin policy attach bpi-minio bpi-dataset-recovery-operator \
    --user "$BPI_DATASET_RECOVERY_MINIO_ACCESS_KEY"

printf 'BPI private Object Lock bucket and isolated recovery operator: PASS\n'
