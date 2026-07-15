#!/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DEPLOY_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
ENV_FILE=${1:-$DEPLOY_DIR/.env}

if [ "${BPI_STREAM_RESTORE_CONFIRM:-}" != "RESTORE_BPI_FLINK_FROM_SAVEPOINT" ]; then
    printf 'ERROR: set BPI_STREAM_RESTORE_CONFIRM=RESTORE_BPI_FLINK_FROM_SAVEPOINT\n' >&2
    exit 1
fi

if [ ! -f "$ENV_FILE" ]; then
    printf 'ERROR: BPI deployment env file not found: %s\n' "$ENV_FILE" >&2
    exit 1
fi

restore_path=$(sed -n 's/^BPI_FLINK_RESTORE_SAVEPOINT_PATH=//p' "$ENV_FILE" | tail -1)
case "$restore_path" in
    s3://*/savepoints/*) ;;
    *)
        printf 'ERROR: persist a valid BPI_FLINK_RESTORE_SAVEPOINT_PATH in %s first\n' "$ENV_FILE" >&2
        exit 1
        ;;
esac

sh "$SCRIPT_DIR/preflight.sh" "$ENV_FILE"

docker compose --env-file "$ENV_FILE" -f "$DEPLOY_DIR/docker-compose.yml" \
    up -d --no-deps --force-recreate bpi-jobmanager bpi-taskmanager

sh "$SCRIPT_DIR/verify-savepoint-restore.sh" "$ENV_FILE"
