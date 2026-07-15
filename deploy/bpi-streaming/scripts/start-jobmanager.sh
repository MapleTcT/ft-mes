#!/bin/sh
set -eu

RESTORE_PATH=${BPI_FLINK_RESTORE_SAVEPOINT_PATH:-}
ALLOW_NON_RESTORED=${BPI_FLINK_ALLOW_NON_RESTORED_STATE:-false}
JOB_CLASS=${BPI_FLINK_JOB_CLASS:-com.mapletct.ftmes.bpi.stream.BpiKafkaJob}
FLINK_ENTRYPOINT=${BPI_FLINK_DOCKER_ENTRYPOINT:-/docker-entrypoint.sh}

case "$ALLOW_NON_RESTORED" in
    true|false) ;;
    *)
        printf 'ERROR: BPI_FLINK_ALLOW_NON_RESTORED_STATE must be true or false\n' >&2
        exit 1
        ;;
esac

set -- standalone-job --job-classname "$JOB_CLASS"
if [ -n "$RESTORE_PATH" ]; then
    case "$RESTORE_PATH" in
        s3://*/savepoints/*) ;;
        *)
            printf 'ERROR: BPI_FLINK_RESTORE_SAVEPOINT_PATH must reference an S3 savepoints object\n' >&2
            exit 1
            ;;
    esac
    set -- "$@" --fromSavepoint "$RESTORE_PATH"
    if [ "$ALLOW_NON_RESTORED" = true ]; then
        set -- "$@" --allowNonRestoredState
    fi
elif [ "$ALLOW_NON_RESTORED" = true ]; then
    printf 'ERROR: non-restored state cannot be allowed without a savepoint path\n' >&2
    exit 1
fi

exec "$FLINK_ENTRYPOINT" "$@"
