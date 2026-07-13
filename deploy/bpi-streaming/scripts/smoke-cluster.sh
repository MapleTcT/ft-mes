#!/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DEPLOY_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
ENV_FILE=${1:-$DEPLOY_DIR/.env}
REPORT_OVERRIDE=${BPI_SMOKE_REPORT:-}

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

for service in kafka-1 kafka-2 kafka-3 bpi-minio bpi-jobmanager; do
    container_id=$(compose ps -q "$service")
    if [ -z "$container_id" ] || [ "$(docker inspect -f '{{.State.Running}}' "$container_id")" != true ]; then
        printf 'ERROR: required service is not running: %s\n' "$service" >&2
        exit 1
    fi
done

TOPICS="${BPI_TELEMETRY_TOPIC:-iot.telemetry.selected.v1}
${BPI_CONTEXT_TOPIC:-mes.production.context.v1}
${BPI_RULE_TOPIC:-bpi.boundary.rule-publication.v1}
${BPI_RULE_APPLICATION_TOPIC:-bpi.boundary.rule-application.v1}
${BPI_RULE_APPLICATION_DLQ_TOPIC:-bpi.boundary.rule-application.dlq.v1}
${BPI_CANDIDATE_TOPIC:-bpi.batch.candidate.v1}
${BPI_CANDIDATE_DLQ_TOPIC:-bpi.batch.candidate.dlq.v1}
${BPI_DATA_QUALITY_TOPIC:-bpi.data-quality.v1}"

DESCRIBE=/tmp/bpi-streaming-topics.$$.txt
trap 'rm -f "$DESCRIBE"' EXIT HUP INT TERM

printf '%s\n' "$TOPICS" | while IFS= read -r topic; do
    compose exec -T kafka-1 /opt/kafka/bin/kafka-topics.sh \
        --bootstrap-server kafka-1:19092 \
        --describe \
        --topic "$topic"
done >"$DESCRIBE"

if [ "$(grep -c 'ReplicationFactor: 3' "$DESCRIBE")" -ne 8 ]; then
    printf 'ERROR: one or more BPI topics do not have replication factor 3\n' >&2
    exit 1
fi
if [ "$(grep -c 'min.insync.replicas=2' "$DESCRIBE")" -ne 8 ]; then
    printf 'ERROR: one or more BPI topics do not have min.insync.replicas=2\n' >&2
    exit 1
fi
if [ "$(grep -c 'retention.ms=2592000000' "$DESCRIBE")" -lt 2 ]; then
    printf 'ERROR: candidate source and DLQ topics must retain records for 30 days\n' >&2
    exit 1
fi

REST_URL="http://${BPI_BIND_ADDRESS:-127.0.0.1}:${BPI_FLINK_REST_PORT:-18081}"
TIMEOUT=${BPI_SMOKE_TIMEOUT_SECONDS:-360}
deadline=$(( $(date +%s) + TIMEOUT ))
JOB_ID=
CHECKPOINT_ID=

while [ "$(date +%s)" -lt "$deadline" ]; do
    jobs_json=$(curl -fsS "$REST_URL/jobs/overview" 2>/dev/null || true)
    JOB_ID=$(printf '%s' "$jobs_json" | python3 -c '
import json, sys
try:
    jobs = json.load(sys.stdin).get("jobs", [])
except Exception:
    jobs = []
running = [job for job in jobs if job.get("state") == "RUNNING" and job.get("name") == "ft-mes-bpi-batch-boundary-v1"]
print(running[0].get("jid", "") if running else "")
' 2>/dev/null || true)
    if [ -n "$JOB_ID" ]; then
        checkpoint_json=$(curl -fsS "$REST_URL/jobs/$JOB_ID/checkpoints" 2>/dev/null || true)
        CHECKPOINT_ID=$(printf '%s' "$checkpoint_json" | python3 -c '
import json, sys
try:
    latest = json.load(sys.stdin).get("latest", {}).get("completed") or {}
except Exception:
    latest = {}
print(latest.get("id", ""))
' 2>/dev/null || true)
        if [ -n "$CHECKPOINT_ID" ]; then
            break
        fi
    fi
    sleep 10
done

if [ -z "$JOB_ID" ] || [ -z "$CHECKPOINT_ID" ]; then
    printf 'ERROR: Flink job did not reach RUNNING with a completed checkpoint within %s seconds\n' "$TIMEOUT" >&2
    exit 1
fi

TASKMANAGERS=$(compose ps -q bpi-taskmanager | wc -l | tr -d ' ')
REPORT=${REPORT_OVERRIDE:-${BPI_SMOKE_REPORT:-/tmp/bpi-streaming-cluster-smoke.json}}
export REPORT JOB_ID CHECKPOINT_ID TASKMANAGERS REST_URL DESCRIBE
python3 <<'PY'
import datetime
import json
import os
from pathlib import Path

report = {
    "generatedAt": datetime.datetime.now(datetime.timezone.utc).isoformat(),
    "status": "PASS",
    "kafka": {
        "brokers": 3,
        "topics": 8,
        "replicationFactor": 3,
        "minInSyncReplicas": 2,
        "describeEvidence": Path(os.environ["DESCRIBE"]).read_text(encoding="utf-8"),
    },
    "flink": {
        "restUrl": os.environ["REST_URL"],
        "jobName": "ft-mes-bpi-batch-boundary-v1",
        "jobId": os.environ["JOB_ID"],
        "taskManagers": int(os.environ["TASKMANAGERS"]),
        "latestCompletedCheckpointId": int(os.environ["CHECKPOINT_ID"]),
    },
}
path = Path(os.environ["REPORT"])
path.parent.mkdir(parents=True, exist_ok=True)
path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(f"BPI streaming cluster smoke: PASS ({path})")
PY
