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

load_env_file

MARKER=${BPI_DQ_REPLAY_MARKER:-ADP_E2E_DQ_FLINK_$(date -u +%Y%m%d_%H%M%S)_$$}
TENANT_ID=${BPI_DQ_REPLAY_TENANT_ID:-TENANT-E2E}
PLANT_ID=${BPI_DQ_REPLAY_PLANT_ID:-PLANT-E2E}
LINE_ID=${BPI_DQ_REPLAY_LINE_ID:-LINE-$MARKER}
EVIDENCE_DIR=${BPI_REPLAY_EVIDENCE_DIR:-/tmp/bpi-streaming-evidence}
CONTAINER_REPORT=${BPI_DQ_REPLAY_REPORT:-/evidence/bpi-data-quality-flink-replay.json}
for scope_value in "$TENANT_ID" "$PLANT_ID" "$LINE_ID"; do
    case $scope_value in
        ""|"*") printf 'ERROR: data-quality replay scope must be explicit and cannot use *\n' >&2; exit 1 ;;
    esac
done
case $EVIDENCE_DIR in
    /*) ;;
    *) printf 'ERROR: BPI_REPLAY_EVIDENCE_DIR must be absolute\n' >&2; exit 1 ;;
esac
case $CONTAINER_REPORT in
    /evidence/*) REPORT_NAME=${CONTAINER_REPORT#/evidence/} ;;
    *) printf 'ERROR: BPI_DQ_REPLAY_REPORT must be under /evidence\n' >&2; exit 1 ;;
esac
case $REPORT_NAME in
    ""|*/*|*..*) printf 'ERROR: BPI_DQ_REPLAY_REPORT must use one safe file name\n' >&2; exit 1 ;;
esac
HOST_REPORT=$EVIDENCE_DIR/$REPORT_NAME

mkdir -p "$EVIDENCE_DIR"
export BPI_HOST_UID=$(id -u)
export BPI_HOST_GID=$(id -g)
export BPI_REPLAY_EVIDENCE_DIR=$EVIDENCE_DIR

SMOKE_BEFORE=/tmp/bpi-data-quality-flink-smoke-before.$$.json
SMOKE_AFTER=/tmp/bpi-data-quality-flink-smoke-after.$$.json
trap 'rm -f "$SMOKE_BEFORE" "$SMOKE_AFTER"' EXIT HUP INT TERM

BPI_SMOKE_REPORT=$SMOKE_BEFORE sh "$SCRIPT_DIR/smoke-cluster.sh" "$ENV_FILE"

docker compose --env-file "$ENV_FILE" -f "$DEPLOY_DIR/docker-compose.yml" \
    --profile acceptance run --rm -T --no-deps \
    --entrypoint java \
    -e "BPI_DQ_REPLAY_MARKER=$MARKER" \
    -e "BPI_DQ_REPLAY_TENANT_ID=$TENANT_ID" \
    -e "BPI_DQ_REPLAY_PLANT_ID=$PLANT_ID" \
    -e "BPI_DQ_REPLAY_LINE_ID=$LINE_ID" \
    -e "BPI_DQ_REPLAY_REPORT=$CONTAINER_REPORT" \
    bpi-cluster-replay \
    -cp /opt/bpi/bpi-stream-engine-job.jar \
    com.mapletct.ftmes.bpi.stream.BpiDataQualityFlinkReplay

if [ ! -f "$HOST_REPORT" ]; then
    printf 'ERROR: Flink data-quality replay report was not written: %s\n' "$HOST_REPORT" >&2
    exit 1
fi

BPI_SMOKE_REPORT=$SMOKE_AFTER sh "$SCRIPT_DIR/smoke-cluster.sh" "$ENV_FILE"

python3 - "$HOST_REPORT" "$SMOKE_BEFORE" "$SMOKE_AFTER" \
    "$TENANT_ID" "$PLANT_ID" "$LINE_ID" <<'PY'
import json
import sys
from pathlib import Path

replay_path = Path(sys.argv[1])
smoke_before_path = Path(sys.argv[2])
smoke_after_path = Path(sys.argv[3])
expected_scope = {
    "tenantId": sys.argv[4],
    "plantId": sys.argv[5],
    "lineId": sys.argv[6],
}
replay = json.loads(replay_path.read_text(encoding="utf-8"))
smoke_before = json.loads(smoke_before_path.read_text(encoding="utf-8"))
smoke_after = json.loads(smoke_after_path.read_text(encoding="utf-8"))
if replay.get("status") != "PASS":
    raise SystemExit("Flink data-quality replay report is not PASS")
if replay.get("scope") != expected_scope:
    raise SystemExit("Flink data-quality replay did not preserve the requested scope")
if smoke_before.get("status") != "PASS" or smoke_after.get("status") != "PASS":
    raise SystemExit("cluster smoke report is not PASS before and after replay")
before_flink = smoke_before["flink"]
after_flink = smoke_after["flink"]
if before_flink["jobId"] != after_flink["jobId"]:
    raise SystemExit("Flink job ID changed during data-quality replay")
if after_flink["latestCompletedCheckpointId"] < before_flink["latestCompletedCheckpointId"]:
    raise SystemExit("Flink checkpoint ID regressed during data-quality replay")
if replay.get("producer") != "Flink telemetry-data-quality operator":
    raise SystemExit("data-quality evidence does not identify the Flink detector")
if len(replay.get("outputs", [])) != 4:
    raise SystemExit("data-quality replay did not produce exactly four committed outputs")
replay["clusterSmoke"] = {
    "status": smoke_after["status"],
    "jobId": after_flink["jobId"],
    "latestCompletedCheckpointId": after_flink["latestCompletedCheckpointId"],
    "taskManagers": after_flink["taskManagers"],
    "jobIdUnchangedDuringReplay": True,
    "checkpointBeforeReplay": before_flink["latestCompletedCheckpointId"],
}
replay_path.write_text(json.dumps(replay, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(f"BPI Flink data-quality replay evidence: {replay_path}")
PY
