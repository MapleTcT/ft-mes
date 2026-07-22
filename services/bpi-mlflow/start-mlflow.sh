#!/bin/sh
set -eu

: "${BPI_MLFLOW_DATABASE_PASSWORD:?BPI_MLFLOW_DATABASE_PASSWORD is required}"
: "${BPI_MLFLOW_ARTIFACT_BUCKET:?BPI_MLFLOW_ARTIFACT_BUCKET is required}"

encoded_password=$(python -c \
    'import os,urllib.parse; print(urllib.parse.quote(os.environ["BPI_MLFLOW_DATABASE_PASSWORD"], safe=""))')

exec mlflow server \
    --host 0.0.0.0 \
    --port 5000 \
    --backend-store-uri \
    "postgresql+psycopg2://mlflow:${encoded_password}@bpi-mlflow-postgres:5432/mlflow" \
    --artifacts-destination "s3://${BPI_MLFLOW_ARTIFACT_BUCKET}" \
    --allowed-hosts "bpi-mlflow:5000,localhost:*,127.0.0.1:*"
