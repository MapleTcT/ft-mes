#!/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DEPLOY_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
ENV_FILE=${1:-$DEPLOY_DIR/.env}
REPORT_OVERRIDE=${BPI_SAVEPOINT_RESTORE_REPORT:-}

if [ ! -f "$ENV_FILE" ]; then
    printf 'ERROR: BPI deployment env file not found: %s\n' "$ENV_FILE" >&2
    exit 1
fi

env_value() {
    key=$1
    fallback=$2
    value=$(sed -n "s/^${key}=//p" "$ENV_FILE" | tail -1)
    printf '%s' "${value:-$fallback}"
}

RESTORE_PATH=$(env_value BPI_FLINK_RESTORE_SAVEPOINT_PATH '')
REST_URL="http://$(env_value BPI_BIND_ADDRESS 127.0.0.1):$(env_value BPI_FLINK_REST_PORT 18081)"
TIMEOUT=$(env_value BPI_SMOKE_TIMEOUT_SECONDS 360)
case "$RESTORE_PATH" in
    s3://*/savepoints/*) ;;
    *) printf 'ERROR: BPI_FLINK_RESTORE_SAVEPOINT_PATH must reference an S3 savepoints object\n' >&2; exit 1 ;;
esac
case "$TIMEOUT" in
    ''|*[!0-9]*) printf 'ERROR: BPI_SMOKE_TIMEOUT_SECONDS must be numeric\n' >&2; exit 1 ;;
esac
deadline=$(( $(date +%s) + TIMEOUT ))
JOB_ID=
JOB_START_TIME=
RESTORED_PATH=
CHECKPOINT_ID=
CHECKPOINT_TRIGGER_TIME=

while [ "$(date +%s)" -lt "$deadline" ]; do
    jobs=$(curl -fsS "$REST_URL/jobs/overview" 2>/dev/null || true)
    job_values=$(printf '%s' "$jobs" | python3 -c '
import json, sys
try:
    jobs = [job for job in json.load(sys.stdin).get("jobs", [])
            if job.get("state") == "RUNNING" and job.get("name") == "ft-mes-bpi-batch-boundary-v1"]
except Exception:
    jobs = []
if len(jobs) == 1:
    print(str(jobs[0].get("jid", "")) + "|" + str(jobs[0].get("start-time", "")))
else:
    print("|")
' 2>/dev/null || true)
    JOB_ID=${job_values%%|*}
    JOB_START_TIME=${job_values#*|}
    if [ -n "$JOB_ID" ]; then
        checkpoints=$(curl -fsS "$REST_URL/jobs/$JOB_ID/checkpoints" 2>/dev/null || true)
        values=$(printf '%s' "$checkpoints" | python3 -c '
import json, sys
try:
    data = json.load(sys.stdin)
    restored = data.get("latest", {}).get("restored") or {}
    completed = data.get("latest", {}).get("completed") or {}
    print((restored.get("external_path") or "") + "|" + str(completed.get("id") or "")
          + "|" + str(completed.get("trigger_timestamp") or ""))
except Exception:
    print("||")
' 2>/dev/null || printf '||')
        RESTORED_PATH=${values%%|*}
        checkpoint_values=${values#*|}
        CHECKPOINT_ID=${checkpoint_values%%|*}
        CHECKPOINT_TRIGGER_TIME=${checkpoint_values#*|}
        case "$JOB_START_TIME" in
            ''|*[!0-9]*) checkpoint_is_new=false ;;
            *)
                case "$CHECKPOINT_TRIGGER_TIME" in
                    ''|*[!0-9]*) checkpoint_is_new=false ;;
                    *)
                        if [ "$CHECKPOINT_TRIGGER_TIME" -ge "$JOB_START_TIME" ]; then
                            checkpoint_is_new=true
                        else
                            checkpoint_is_new=false
                        fi
                        ;;
                esac
                ;;
        esac
        if [ "$RESTORED_PATH" = "$RESTORE_PATH" ] \
            && [ -n "$CHECKPOINT_ID" ] \
            && [ "$checkpoint_is_new" = true ]; then
                break
        fi
    fi
    sleep 5
done

if [ "$RESTORED_PATH" != "$RESTORE_PATH" ]; then
    printf 'ERROR: Flink did not report restoration from the configured savepoint\n' >&2
    exit 1
fi
if [ -z "$CHECKPOINT_ID" ] || [ "${checkpoint_is_new:-false}" != true ]; then
    printf 'ERROR: restored Flink job has no completed post-restore checkpoint\n' >&2
    exit 1
fi

JOB_DETAIL=$(curl -fsS "$REST_URL/jobs/$JOB_ID")
printf '%s' "$JOB_DETAIL" | grep -q 'Kafka point-catalog source' || {
    printf 'ERROR: restored job is missing the point-catalog source\n' >&2
    exit 1
}
printf '%s' "$JOB_DETAIL" | grep -q 'runtime-readiness sink' || {
    printf 'ERROR: restored job is missing the runtime-readiness sink\n' >&2
    exit 1
}

sh "$SCRIPT_DIR/smoke-cluster.sh" "$ENV_FILE"

REPORT=${REPORT_OVERRIDE:-$(env_value BPI_REPLAY_EVIDENCE_DIR /tmp/bpi-streaming-evidence)/bpi-savepoint-restore.json}
export REPORT JOB_ID JOB_START_TIME RESTORE_PATH CHECKPOINT_ID CHECKPOINT_TRIGGER_TIME REST_URL
python3 <<'PY'
import datetime
import json
import os
from pathlib import Path

report = {
    "generatedAt": datetime.datetime.now(datetime.timezone.utc).isoformat(),
    "status": "PASS",
    "job": {
        "name": "ft-mes-bpi-batch-boundary-v1",
        "id": os.environ["JOB_ID"],
        "restUrl": os.environ["REST_URL"],
        "startTime": int(os.environ["JOB_START_TIME"]),
        "restoredFrom": os.environ["RESTORE_PATH"],
        "postRestoreCheckpointId": int(os.environ["CHECKPOINT_ID"]),
        "postRestoreCheckpointTriggerTime": int(os.environ["CHECKPOINT_TRIGGER_TIME"]),
    },
    "requiredOperators": {
        "pointCatalogSource": "PRESENT",
        "runtimeReadinessSink": "PRESENT",
    },
    "clusterSmoke": "PASS",
}
path = Path(os.environ["REPORT"])
path.parent.mkdir(parents=True, exist_ok=True)
path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(f"BPI savepoint restore verification: PASS ({path})")
PY
