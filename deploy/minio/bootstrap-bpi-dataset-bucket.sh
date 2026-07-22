#!/bin/sh
set -eu

if [ "${BPI_DATASET_BUCKET_BOOTSTRAP_ENABLED:-false}" != "true" ]; then
    printf 'BPI dataset bucket bootstrap is disabled\n'
    exit 0
fi

: "${BPI_DATASET_MINIO_ENDPOINT:?BPI_DATASET_MINIO_ENDPOINT is required}"
: "${MINIO_ROOT_USER:?MINIO_ROOT_USER is required}"
: "${MINIO_ROOT_PASSWORD:?MINIO_ROOT_PASSWORD is required}"
: "${BPI_DATASET_MINIO_ACCESS_KEY:?BPI_DATASET_MINIO_ACCESS_KEY is required}"
: "${BPI_DATASET_MINIO_SECRET_KEY:?BPI_DATASET_MINIO_SECRET_KEY is required}"
: "${BPI_DATASET_MINIO_BUCKET:?BPI_DATASET_MINIO_BUCKET is required}"

case "$BPI_DATASET_MINIO_BUCKET" in
    ''|*[!a-z0-9.-]*)
        printf 'ERROR: BPI_DATASET_MINIO_BUCKET contains unsupported characters\n' >&2
        exit 1
        ;;
esac

attempt=0
until mc alias set bpi-minio "$BPI_DATASET_MINIO_ENDPOINT" \
    "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD" >/dev/null 2>&1 \
    && mc ls bpi-minio >/dev/null 2>&1; do
    attempt=$((attempt + 1))
    if [ "$attempt" -ge 30 ]; then
        printf 'ERROR: MinIO did not become ready for BPI bucket bootstrap\n' >&2
        exit 1
    fi
    sleep 2
done

mc mb --ignore-existing "bpi-minio/$BPI_DATASET_MINIO_BUCKET"
mc anonymous set none "bpi-minio/$BPI_DATASET_MINIO_BUCKET"
mc version enable "bpi-minio/$BPI_DATASET_MINIO_BUCKET"

if ! mc admin user info bpi-minio "$BPI_DATASET_MINIO_ACCESS_KEY" >/dev/null 2>&1; then
    mc admin user add bpi-minio \
        "$BPI_DATASET_MINIO_ACCESS_KEY" "$BPI_DATASET_MINIO_SECRET_KEY"
fi

while IFS= read -r policy_line || [ -n "$policy_line" ]; do
    case "$policy_line" in
        *__BUCKET__*)
            policy_prefix=${policy_line%%__BUCKET__*}
            policy_suffix=${policy_line#*__BUCKET__}
            printf '%s%s%s\n' "$policy_prefix" "$BPI_DATASET_MINIO_BUCKET" "$policy_suffix"
            ;;
        *)
            printf '%s\n' "$policy_line"
            ;;
    esac
done </work/policy.json >/tmp/bpi-dataset-materializer-policy.json
mc admin policy create bpi-minio bpi-dataset-materializer \
    /tmp/bpi-dataset-materializer-policy.json
mc admin policy attach bpi-minio bpi-dataset-materializer \
    --user "$BPI_DATASET_MINIO_ACCESS_KEY"

printf 'BPI private dataset bucket bootstrap: PASS\n'
