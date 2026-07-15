#!/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DEPLOY_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
ENV_FILE=${1:-$DEPLOY_DIR/.env}
REPORT_OVERRIDE=${BPI_SAVEPOINT_CAPTURE_REPORT:-}

if [ ! -f "$ENV_FILE" ]; then
    printf 'ERROR: BPI deployment env file not found: %s\n' "$ENV_FILE" >&2
    exit 1
fi

load_env_file() {
    while IFS= read -r line || [ -n "$line" ]; do
        line=$(printf '%s' "$line" | tr -d '\r')
        case $line in
            ""|\#*) continue ;;
            *=*) key=${line%%=*}; value=${line#*=} ;;
            *) printf 'ERROR: invalid env line without equals sign\n' >&2; exit 1 ;;
        esac
        case $key in
            ""|*[!A-Za-z0-9_]*) printf 'ERROR: invalid env key: %s\n' "$key" >&2; exit 1 ;;
        esac
        export "$key=$value"
    done <"$ENV_FILE"
}

load_env_file

compose() {
    docker compose --env-file "$ENV_FILE" -f "$DEPLOY_DIR/docker-compose.yml" "$@"
}

REST_URL="http://${BPI_BIND_ADDRESS:-127.0.0.1}:${BPI_FLINK_REST_PORT:-18081}"
JOBS_JSON=$(curl -fsS "$REST_URL/jobs/overview")
JOB_ID=$(printf '%s' "$JOBS_JSON" | python3 -c '
import json, sys
jobs = [job for job in json.load(sys.stdin).get("jobs", [])
        if job.get("state") == "RUNNING" and job.get("name") == "ft-mes-bpi-batch-boundary-v1"]
print(jobs[0]["jid"] if len(jobs) == 1 else "")
')
if [ -z "$JOB_ID" ]; then
    printf 'ERROR: exactly one RUNNING BPI Flink job is required before savepoint capture\n' >&2
    exit 1
fi

CHECKPOINTS_JSON=$(curl -fsS "$REST_URL/jobs/$JOB_ID/checkpoints")
CHECKPOINT_ID=$(printf '%s' "$CHECKPOINTS_JSON" | python3 -c '
import json, sys
latest = json.load(sys.stdin).get("latest", {}).get("completed") or {}
print(latest.get("id", ""))
')
if [ -z "$CHECKPOINT_ID" ]; then
    printf 'ERROR: a completed checkpoint is required before savepoint capture\n' >&2
    exit 1
fi

UNDER_REPLICATED=$(compose exec -T kafka-1 /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server kafka-1:19092 --describe --under-replicated-partitions)
if [ -n "$UNDER_REPLICATED" ]; then
    printf 'ERROR: Kafka has under-replicated partitions; savepoint capture is blocked\n' >&2
    exit 1
fi

TOPIC_COUNT=$(compose exec -T kafka-1 /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server kafka-1:19092 --list | grep -vc '^__')
SAVEPOINT_ROOT="s3://${BPI_CHECKPOINT_BUCKET:-ft-mes-bpi-checkpoints}/savepoints"
SAVEPOINT_OUTPUT=$(compose exec -T bpi-jobmanager /opt/flink/bin/flink savepoint \
    -type canonical "$JOB_ID" "$SAVEPOINT_ROOT")
SAVEPOINT_PATH=$(printf '%s\n' "$SAVEPOINT_OUTPUT" \
    | sed -n 's/^.*Path: \(s3:\/\/[^[:space:]]*\).*$/\1/p' | tail -1)
case "$SAVEPOINT_PATH" in
    "$SAVEPOINT_ROOT"/*) ;;
    *)
        printf 'ERROR: Flink did not return a savepoint under %s\n' "$SAVEPOINT_ROOT" >&2
        exit 1
        ;;
esac

case ${BPI_JOB_JAR:-} in
    /*) JOB_JAR=${BPI_JOB_JAR} ;;
    *) JOB_JAR=$DEPLOY_DIR/${BPI_JOB_JAR:-../../streaming/bpi-stream-engine/target/bpi-stream-engine-0.1.0-SNAPSHOT-job.jar} ;;
esac
JOB_JAR_SHA256=$(sha256sum "$JOB_JAR" | awk '{print $1}')
REPORT=${REPORT_OVERRIDE:-${BPI_REPLAY_EVIDENCE_DIR:-/tmp/bpi-streaming-evidence}/bpi-savepoint-capture.json}

export REPORT JOB_ID CHECKPOINT_ID SAVEPOINT_PATH JOB_JAR JOB_JAR_SHA256 TOPIC_COUNT REST_URL
python3 <<'PY'
import datetime
import json
import os
from pathlib import Path

report = {
    "generatedAt": datetime.datetime.now(datetime.timezone.utc).isoformat(),
    "status": "PASS",
    "operation": "CAPTURE_ONLY",
    "job": {
        "name": "ft-mes-bpi-batch-boundary-v1",
        "id": os.environ["JOB_ID"],
        "restUrl": os.environ["REST_URL"],
        "latestCompletedCheckpointId": int(os.environ["CHECKPOINT_ID"]),
        "jar": os.environ["JOB_JAR"],
        "jarSha256": os.environ["JOB_JAR_SHA256"],
    },
    "savepoint": {
        "path": os.environ["SAVEPOINT_PATH"],
        "format": "CANONICAL",
        "jobCancelled": False,
    },
    "kafka": {
        "businessTopicCount": int(os.environ["TOPIC_COUNT"]),
        "underReplicatedPartitions": 0,
    },
    "destructiveActionsPerformed": False,
}
path = Path(os.environ["REPORT"])
path.parent.mkdir(parents=True, exist_ok=True)
path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(f"BPI upgrade savepoint capture: PASS ({path})")
print(f"BPI_FLINK_RESTORE_SAVEPOINT_PATH={report['savepoint']['path']}")
PY
