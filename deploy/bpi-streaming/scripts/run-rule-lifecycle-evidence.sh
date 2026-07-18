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
        printf 'ERROR: %s is required for read-only lifecycle evidence\n' "$key" >&2
        exit 1
    fi
}

load_env_file
for key in \
    BPI_LIFECYCLE_EVIDENCE_MARKER BPI_LIFECYCLE_EVIDENCE_TENANT_ID \
    BPI_LIFECYCLE_EVIDENCE_PLANT_ID BPI_LIFECYCLE_EVIDENCE_LINE_ID \
    BPI_LIFECYCLE_EVIDENCE_RULE_CODE BPI_LIFECYCLE_EVIDENCE_RULE_VERSION
do
    require_value "$key"
done

EVIDENCE_DIR=${BPI_REPLAY_EVIDENCE_DIR:-/tmp/bpi-streaming-evidence}
CONTAINER_REPORT=${BPI_LIFECYCLE_EVIDENCE_REPORT:-/evidence/bpi-rule-lifecycle-evidence.json}
case $EVIDENCE_DIR in
    /*) ;;
    *) printf 'ERROR: BPI_REPLAY_EVIDENCE_DIR must be absolute\n' >&2; exit 1 ;;
esac
case $CONTAINER_REPORT in
    /evidence/*) REPORT_NAME=${CONTAINER_REPORT#/evidence/} ;;
    *) printf 'ERROR: BPI_LIFECYCLE_EVIDENCE_REPORT must be under /evidence\n' >&2; exit 1 ;;
esac
case $REPORT_NAME in
    ""|*/*|*..*) printf 'ERROR: BPI_LIFECYCLE_EVIDENCE_REPORT must use one safe file name\n' >&2; exit 1 ;;
esac
HOST_REPORT=$EVIDENCE_DIR/$REPORT_NAME

mkdir -p "$EVIDENCE_DIR"
export BPI_HOST_UID=$(id -u)
export BPI_HOST_GID=$(id -g)

docker compose --env-file "$ENV_FILE" -f "$DEPLOY_DIR/docker-compose.yml" \
    --profile acceptance run --rm -T --no-deps \
    -e "BPI_LIFECYCLE_EVIDENCE_MARKER=$BPI_LIFECYCLE_EVIDENCE_MARKER" \
    -e "BPI_LIFECYCLE_EVIDENCE_TENANT_ID=$BPI_LIFECYCLE_EVIDENCE_TENANT_ID" \
    -e "BPI_LIFECYCLE_EVIDENCE_PLANT_ID=$BPI_LIFECYCLE_EVIDENCE_PLANT_ID" \
    -e "BPI_LIFECYCLE_EVIDENCE_LINE_ID=$BPI_LIFECYCLE_EVIDENCE_LINE_ID" \
    -e "BPI_LIFECYCLE_EVIDENCE_RULE_CODE=$BPI_LIFECYCLE_EVIDENCE_RULE_CODE" \
    -e "BPI_LIFECYCLE_EVIDENCE_RULE_VERSION=$BPI_LIFECYCLE_EVIDENCE_RULE_VERSION" \
    -e "BPI_LIFECYCLE_EVIDENCE_TIMEOUT_SECONDS=${BPI_LIFECYCLE_EVIDENCE_TIMEOUT_SECONDS:-60}" \
    -e "BPI_LIFECYCLE_EVIDENCE_REPORT=$CONTAINER_REPORT" \
    bpi-joint-replay \
    -cp /opt/bpi/bpi-stream-engine-job.jar \
    com.mapletct.ftmes.bpi.stream.BpiRuleLifecycleEvidence

if [ ! -f "$HOST_REPORT" ]; then
    printf 'ERROR: lifecycle evidence report was not written: %s\n' "$HOST_REPORT" >&2
    exit 1
fi

python3 - "$HOST_REPORT" <<'PY'
import json
import sys
from pathlib import Path

path = Path(sys.argv[1])
report = json.loads(path.read_text(encoding="utf-8"))
if report.get("status") != "PASS":
    raise SystemExit("rule lifecycle evidence report is not PASS")
summary = report.get("summary", {})
required = ("activationPublished", "retirementPublished", "flinkAppliedBoth", "runtimeReadyThenInactive")
if not all(summary.get(key) is True for key in required):
    raise SystemExit("rule lifecycle evidence is incomplete")
print(f"BPI read-only rule lifecycle evidence: {path}")
PY
