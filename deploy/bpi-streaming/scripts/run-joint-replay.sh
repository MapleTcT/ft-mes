#!/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DEPLOY_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
ENV_FILE=${1:-$DEPLOY_DIR/.env}

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

require_value() {
    key=$1
    eval "value=\${$key:-}"
    if [ -z "$value" ]; then
        printf 'ERROR: %s is required for joint acceptance\n' "$key" >&2
        exit 1
    fi
}

load_env_file

for key in \
    BPI_JOINT_TENANT_ID BPI_JOINT_PLANT_ID BPI_JOINT_LINE_ID \
    BPI_JOINT_TOPOLOGY_CODE BPI_JOINT_TOPOLOGY_VERSION \
    BPI_JOINT_RULE_CODE BPI_JOINT_RULE_VERSION BPI_JOINT_DEVICE_ID
do
    require_value "$key"
done

MARKER=${BPI_JOINT_MARKER:-ADP_E2E_$(date -u +%Y%m%d_%H%M%S)_$$}
EVIDENCE_DIR=${BPI_REPLAY_EVIDENCE_DIR:-/tmp/bpi-streaming-evidence}
CONTAINER_REPORT=${BPI_JOINT_REPORT:-/evidence/bpi-joint-replay.json}
case $EVIDENCE_DIR in
    /*) ;;
    *) printf 'ERROR: BPI_REPLAY_EVIDENCE_DIR must be absolute\n' >&2; exit 1 ;;
esac
case $CONTAINER_REPORT in
    /evidence/*) REPORT_NAME=${CONTAINER_REPORT#/evidence/} ;;
    *) printf 'ERROR: BPI_JOINT_REPORT must be under /evidence\n' >&2; exit 1 ;;
esac
case $REPORT_NAME in
    ""|*/*|*..*) printf 'ERROR: BPI_JOINT_REPORT must use one safe file name\n' >&2; exit 1 ;;
esac
HOST_REPORT=$EVIDENCE_DIR/$REPORT_NAME

mkdir -p "$EVIDENCE_DIR"
export BPI_HOST_UID=$(id -u)
export BPI_HOST_GID=$(id -g)
export BPI_JOINT_MARKER=$MARKER

sh "$SCRIPT_DIR/smoke-cluster.sh" "$ENV_FILE"

docker compose --env-file "$ENV_FILE" -f "$DEPLOY_DIR/docker-compose.yml" \
    --profile acceptance run --rm -T \
    -e "BPI_JOINT_MARKER=$MARKER" \
    bpi-joint-replay

if [ ! -f "$HOST_REPORT" ]; then
    printf 'ERROR: joint replay report was not written: %s\n' "$HOST_REPORT" >&2
    exit 1
fi

SMOKE_REPORT=${BPI_SMOKE_REPORT:-/tmp/bpi-streaming-cluster-smoke.json}
python3 - "$HOST_REPORT" "$SMOKE_REPORT" <<'PY'
import json
import sys
from pathlib import Path

replay_path = Path(sys.argv[1])
smoke_path = Path(sys.argv[2])
replay = json.loads(replay_path.read_text(encoding="utf-8"))
smoke = json.loads(smoke_path.read_text(encoding="utf-8"))
if replay.get("status") != "PASS":
    raise SystemExit("joint replay report is not PASS")
if replay.get("ruleSource") != "BPI_BROWSER_PUBLICATION_OUTBOX":
    raise SystemExit("joint replay did not preserve the browser publication boundary")
if smoke.get("status") != "PASS":
    raise SystemExit("cluster smoke report is not PASS")
replay["clusterSmoke"] = {
    "status": smoke["status"],
    "jobId": smoke["flink"]["jobId"],
    "latestCompletedCheckpointId": smoke["flink"]["latestCompletedCheckpointId"],
    "taskManagers": smoke["flink"]["taskManagers"],
}
replay_path.write_text(json.dumps(replay, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(f"BPI browser-rule joint replay evidence: {replay_path}")
PY
