#!/bin/sh
set -eu

if [ "${BPI_MLFLOW_ARTIFACT_BOOTSTRAP_ENABLED:-false}" != "true" ]; then
    printf 'BPI MLflow artifact bucket bootstrap is disabled\n'
    exit 0
fi

: "${BPI_MLFLOW_MINIO_ENDPOINT:?BPI_MLFLOW_MINIO_ENDPOINT is required}"
: "${MINIO_ROOT_USER:?MINIO_ROOT_USER is required}"
: "${MINIO_ROOT_PASSWORD:?MINIO_ROOT_PASSWORD is required}"
: "${BPI_MLFLOW_MINIO_ACCESS_KEY:?BPI_MLFLOW_MINIO_ACCESS_KEY is required}"
: "${BPI_MLFLOW_MINIO_SECRET_KEY:?BPI_MLFLOW_MINIO_SECRET_KEY is required}"
: "${BPI_MLFLOW_ARTIFACT_BUCKET:?BPI_MLFLOW_ARTIFACT_BUCKET is required}"

case "$BPI_MLFLOW_ARTIFACT_BUCKET" in
    ''|*[!a-z0-9.-]*)
        printf 'ERROR: MLflow artifact bucket contains unsupported characters\n' >&2
        exit 1
        ;;
esac
if [ "$BPI_MLFLOW_MINIO_ACCESS_KEY" = "$MINIO_ROOT_USER" ] \
   || [ "$BPI_MLFLOW_MINIO_SECRET_KEY" = "$MINIO_ROOT_PASSWORD" ]; then
    printf 'ERROR: MLflow artifact credentials must differ from MinIO root\n' >&2
    exit 1
fi

attempt=0
until mc alias set bpi-minio "$BPI_MLFLOW_MINIO_ENDPOINT" \
    "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD" >/dev/null 2>&1 \
    && mc ls bpi-minio >/dev/null 2>&1; do
    attempt=$((attempt + 1))
    if [ "$attempt" -ge 30 ]; then
        printf 'ERROR: MinIO did not become ready for MLflow artifact bootstrap\n' >&2
        exit 1
    fi
    sleep 2
done

mc mb --ignore-existing "bpi-minio/$BPI_MLFLOW_ARTIFACT_BUCKET"
mc anonymous set none "bpi-minio/$BPI_MLFLOW_ARTIFACT_BUCKET"

if ! mc admin user info bpi-minio \
    "$BPI_MLFLOW_MINIO_ACCESS_KEY" >/dev/null 2>&1; then
    mc admin user add bpi-minio \
        "$BPI_MLFLOW_MINIO_ACCESS_KEY" \
        "$BPI_MLFLOW_MINIO_SECRET_KEY"
fi

sed "s/__MLFLOW_ARTIFACT_BUCKET__/$BPI_MLFLOW_ARTIFACT_BUCKET/g" \
    /work/policy.json >/tmp/bpi-mlflow-artifact-policy.json
case "$(cat /tmp/bpi-mlflow-artifact-policy.json)" in
    *__MLFLOW_ARTIFACT_BUCKET__*)
        printf 'ERROR: rendered MLflow artifact policy is incomplete\n' >&2
        exit 1
        ;;
esac
mc admin policy create bpi-minio bpi-mlflow-artifact \
    /tmp/bpi-mlflow-artifact-policy.json
mc admin policy attach bpi-minio bpi-mlflow-artifact \
    --user "$BPI_MLFLOW_MINIO_ACCESS_KEY"

printf 'BPI private MLflow artifact bucket and service account: PASS\n'
