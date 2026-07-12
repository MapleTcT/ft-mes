#!/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DEPLOY_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
REPO_ROOT=$(CDPATH= cd -- "$DEPLOY_DIR/../.." && pwd)
ENV_FILE=${1:-$DEPLOY_DIR/.env}
REPORT_OVERRIDE=${BPI_PREFLIGHT_REPORT:-}

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

REPORT=${REPORT_OVERRIDE:-${BPI_PREFLIGHT_REPORT:-/tmp/bpi-streaming-preflight.json}}
MIN_FREE_GB=${BPI_MIN_FREE_DISK_GB:-25}
FAILURES=

record_failure() {
    message=$1
    if [ -n "$FAILURES" ]; then
        FAILURES="$FAILURES
$message"
    else
        FAILURES=$message
    fi
}

for command_name in docker python3; do
    if ! command -v "$command_name" >/dev/null 2>&1; then
        record_failure "required command is missing: $command_name"
    fi
done

SERVER_VERSION=unknown
DOCKER_ROOT=/
AVAILABLE_KB=0
AVAILABLE_GB=0
if command -v docker >/dev/null 2>&1; then
    SERVER_VERSION=$(docker version --format '{{.Server.Version}}' 2>/dev/null || printf unknown)
    if [ "$SERVER_VERSION" = unknown ]; then
        record_failure "Docker daemon is not reachable"
    else
        docker_major=$(printf '%s' "$SERVER_VERSION" | cut -d. -f1)
        docker_minor=$(printf '%s' "$SERVER_VERSION" | cut -d. -f2)
        docker_patch=$(printf '%s' "$SERVER_VERSION" | cut -d. -f3 | sed 's/[^0-9].*$//')
        docker_patch=${docker_patch:-0}
        if [ "$docker_major" -lt 20 ] \
            || { [ "$docker_major" -eq 20 ] && [ "$docker_minor" -lt 10 ]; } \
            || { [ "$docker_major" -eq 20 ] && [ "$docker_minor" -eq 10 ] && [ "$docker_patch" -lt 4 ]; }
        then
            record_failure "Docker 20.10.4 or newer is required, found $SERVER_VERSION"
        fi
    fi
    if ! docker compose version >/dev/null 2>&1; then
        record_failure "Docker Compose v2 is required"
    fi
    DOCKER_ROOT=$(docker info --format '{{.DockerRootDir}}' 2>/dev/null || printf /)
    if [ ! -e "$DOCKER_ROOT" ]; then
        DOCKER_ROOT=/
    fi
    AVAILABLE_KB=$(df -Pk "$DOCKER_ROOT" 2>/dev/null | awk 'NR == 2 {print $4}')
    AVAILABLE_KB=${AVAILABLE_KB:-0}
    AVAILABLE_GB=$((AVAILABLE_KB / 1024 / 1024))
    if [ "$AVAILABLE_GB" -lt "$MIN_FREE_GB" ]; then
        record_failure "Docker storage has ${AVAILABLE_GB} GiB free; ${MIN_FREE_GB} GiB is required"
    fi
fi

case ${BPI_MINIO_ROOT_PASSWORD:-} in
    ""|*change-me*) record_failure "BPI_MINIO_ROOT_PASSWORD must be replaced before deployment" ;;
esac

JOB_JAR=${BPI_JOB_JAR:-../../streaming/bpi-stream-engine/target/bpi-stream-engine-0.1.0-SNAPSHOT-job.jar}
case $JOB_JAR in
    /*) JOB_JAR_ABS=$JOB_JAR ;;
    *) JOB_JAR_ABS=$DEPLOY_DIR/$JOB_JAR ;;
esac

if [ ! -f "$JOB_JAR_ABS" ]; then
    record_failure "BPI job JAR is missing: $JOB_JAR_ABS"
elif command -v python3 >/dev/null 2>&1; then
    if ! python3 - "$JOB_JAR_ABS" <<'PY'
import sys
import zipfile

jar = sys.argv[1]
with zipfile.ZipFile(jar) as archive:
    manifest = archive.read("META-INF/MANIFEST.MF").decode("utf-8", "replace")
    expected_class = "com/mapletct/ftmes/bpi/stream/BpiKafkaJob.class"
    if "Main-Class: com.mapletct.ftmes.bpi.stream.BpiKafkaJob" not in manifest:
        raise SystemExit("job JAR manifest has no BpiKafkaJob Main-Class")
    if expected_class not in archive.namelist():
        raise SystemExit("job JAR does not contain BpiKafkaJob.class")
PY
    then
        record_failure "BPI job JAR manifest or contents are invalid"
    fi
fi

if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
    if ! docker compose --env-file "$ENV_FILE" -f "$DEPLOY_DIR/docker-compose.yml" config --quiet; then
        record_failure "Docker Compose configuration does not render"
    fi
fi

if command -v ss >/dev/null 2>&1; then
    own_ports=$(docker ps \
        --filter "label=com.docker.compose.project=${COMPOSE_PROJECT_NAME:-ft-mes-bpi-streaming}" \
        --format '{{.Ports}}' 2>/dev/null || true)
    for port in \
        "${BPI_KAFKA_1_PORT:-29092}" \
        "${BPI_KAFKA_2_PORT:-39092}" \
        "${BPI_KAFKA_3_PORT:-49092}" \
        "${BPI_FLINK_REST_PORT:-18081}"
    do
        if ss -H -ltn 2>/dev/null | awk '{print $4}' | grep -Eq "[:.]${port}$"; then
            if ! printf '%s\n' "$own_ports" | grep -Eq "[:.]${port}->"; then
                record_failure "host port is already in use: $port"
            fi
        fi
    done
fi

STATUS=READY
if [ -n "$FAILURES" ]; then
    STATUS=BLOCKED
fi

export REPORT STATUS FAILURES SERVER_VERSION DOCKER_ROOT AVAILABLE_KB AVAILABLE_GB MIN_FREE_GB JOB_JAR_ABS ENV_FILE REPO_ROOT
python3 <<'PY'
import datetime
import json
import os
from pathlib import Path

report = {
    "generatedAt": datetime.datetime.now(datetime.timezone.utc).isoformat(),
    "status": os.environ["STATUS"],
    "deploymentStarted": False,
    "destructiveActionsPerformed": False,
    "environmentFile": os.environ["ENV_FILE"],
    "docker": {
        "serverVersion": os.environ["SERVER_VERSION"],
        "root": os.environ["DOCKER_ROOT"],
        "availableKiB": int(os.environ["AVAILABLE_KB"]),
        "availableGiB": int(os.environ["AVAILABLE_GB"]),
        "minimumFreeGiB": int(os.environ["MIN_FREE_GB"]),
    },
    "jobJar": os.environ["JOB_JAR_ABS"],
    "failures": [line for line in os.environ.get("FAILURES", "").splitlines() if line],
}
path = Path(os.environ["REPORT"])
path.parent.mkdir(parents=True, exist_ok=True)
path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(f"BPI streaming preflight: {report['status']} ({path})")
PY

if [ "$STATUS" != READY ]; then
    printf '%s\n' "$FAILURES" | sed 's/^/ERROR: /' >&2
    exit 1
fi
