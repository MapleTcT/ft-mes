#!/bin/sh
set -eu

if [ "${BPI_ICEBERG_WAREHOUSE_BOOTSTRAP_ENABLED:-false}" != "true" ]; then
    printf 'BPI Iceberg warehouse bootstrap is disabled\n'
    exit 0
fi

: "${BPI_ICEBERG_MINIO_ENDPOINT:?BPI_ICEBERG_MINIO_ENDPOINT is required}"
: "${MINIO_ROOT_USER:?MINIO_ROOT_USER is required}"
: "${MINIO_ROOT_PASSWORD:?MINIO_ROOT_PASSWORD is required}"
: "${BPI_ICEBERG_MINIO_ACCESS_KEY:?BPI_ICEBERG_MINIO_ACCESS_KEY is required}"
: "${BPI_ICEBERG_MINIO_SECRET_KEY:?BPI_ICEBERG_MINIO_SECRET_KEY is required}"
: "${BPI_ICEBERG_WAREHOUSE_BUCKET:?BPI_ICEBERG_WAREHOUSE_BUCKET is required}"

case "$BPI_ICEBERG_WAREHOUSE_BUCKET" in
    ''|*[!a-z0-9.-]*)
        printf 'ERROR: BPI_ICEBERG_WAREHOUSE_BUCKET contains unsupported characters\n' >&2
        exit 1
        ;;
esac

if [ "$BPI_ICEBERG_MINIO_ACCESS_KEY" = "$MINIO_ROOT_USER" ] \
   || [ "$BPI_ICEBERG_MINIO_SECRET_KEY" = "$MINIO_ROOT_PASSWORD" ]; then
    printf 'ERROR: Iceberg warehouse credentials must differ from MinIO root credentials\n' >&2
    exit 1
fi

attempt=0
until mc alias set bpi-minio "$BPI_ICEBERG_MINIO_ENDPOINT" \
    "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD" >/dev/null 2>&1 \
    && mc ls bpi-minio >/dev/null 2>&1; do
    attempt=$((attempt + 1))
    if [ "$attempt" -ge 30 ]; then
        printf 'ERROR: MinIO did not become ready for Iceberg warehouse bootstrap\n' >&2
        exit 1
    fi
    sleep 2
done

mc mb --ignore-existing "bpi-minio/$BPI_ICEBERG_WAREHOUSE_BUCKET"
mc anonymous set none "bpi-minio/$BPI_ICEBERG_WAREHOUSE_BUCKET"
mc version enable "bpi-minio/$BPI_ICEBERG_WAREHOUSE_BUCKET"

if ! mc admin user info bpi-minio "$BPI_ICEBERG_MINIO_ACCESS_KEY" >/dev/null 2>&1; then
    mc admin user add bpi-minio \
        "$BPI_ICEBERG_MINIO_ACCESS_KEY" "$BPI_ICEBERG_MINIO_SECRET_KEY"
fi

while IFS= read -r policy_line || [ -n "$policy_line" ]; do
    case "$policy_line" in
        *__BUCKET__*)
            policy_prefix=${policy_line%%__BUCKET__*}
            policy_suffix=${policy_line#*__BUCKET__}
            printf '%s%s%s\n' "$policy_prefix" "$BPI_ICEBERG_WAREHOUSE_BUCKET" "$policy_suffix"
            ;;
        *)
            printf '%s\n' "$policy_line"
            ;;
    esac
done </work/policy.json >/tmp/bpi-iceberg-warehouse-policy.json
mc admin policy create bpi-minio bpi-iceberg-warehouse \
    /tmp/bpi-iceberg-warehouse-policy.json
mc admin policy attach bpi-minio bpi-iceberg-warehouse \
    --user "$BPI_ICEBERG_MINIO_ACCESS_KEY"

printf 'BPI private versioned Iceberg warehouse bootstrap: PASS\n'
