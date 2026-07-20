#!/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DEPLOY_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
ROOT_DIR=$(CDPATH= cd -- "$DEPLOY_DIR/../.." && pwd)
ENV_FILE=${1:-$DEPLOY_DIR/.env}

if [ "${BPI_RUNTIME_UPGRADE_CONFIRM:-}" != "UPGRADE_BPI_RUNTIME_EXPAND_ONLY" ]; then
    printf 'ERROR: set BPI_RUNTIME_UPGRADE_CONFIRM=UPGRADE_BPI_RUNTIME_EXPAND_ONLY\n' >&2
    exit 1
fi
if [ ! -f "$ENV_FILE" ]; then
    printf 'ERROR: BPI runtime env file not found: %s\n' "$ENV_FILE" >&2
    exit 1
fi

env_value() {
    key=$1
    fallback=$2
    value=$(sed -n "s/^${key}=//p" "$ENV_FILE" | tail -1)
    printf '%s' "${value:-$fallback}"
}

compose() {
    docker compose --env-file "$ENV_FILE" -f "$DEPLOY_DIR/docker-compose.yml" "$@"
}

DATABASE_NAME=$(env_value BPI_DATABASE_NAME ft_mes_bpi)
POSTGRES_USER=$(env_value POSTGRES_USER bpi_admin)
EXPECTED_VERSION=$(env_value BPI_EXPECTED_FLYWAY_VERSION '')
BACKUP_DIR=${BPI_RUNTIME_UPGRADE_BACKUP_DIR:-}
REPORT=${BPI_RUNTIME_UPGRADE_REPORT:-/tmp/bpi-runtime-expand-upgrade.json}
HEALTH_TIMEOUT_SECONDS=${BPI_RUNTIME_UPGRADE_HEALTH_TIMEOUT_SECONDS:-180}

case "$EXPECTED_VERSION" in
    ''|*[!0-9]*) printf 'ERROR: BPI_EXPECTED_FLYWAY_VERSION must be numeric\n' >&2; exit 1 ;;
esac
case "$BACKUP_DIR" in
    /) printf 'ERROR: BPI_RUNTIME_UPGRADE_BACKUP_DIR cannot be the filesystem root\n' >&2; exit 1 ;;
    /*) ;;
    *) printf 'ERROR: BPI_RUNTIME_UPGRADE_BACKUP_DIR must be an absolute path\n' >&2; exit 1 ;;
esac
case "$HEALTH_TIMEOUT_SECONDS" in
    ''|*[!0-9]*|0) printf 'ERROR: BPI_RUNTIME_UPGRADE_HEALTH_TIMEOUT_SECONDS must be a positive integer\n' >&2; exit 1 ;;
esac

postgres_id=$(compose ps -q bpi-postgres)
service_id=$(compose ps -q bpi-service)
wms_adapter_id=$(compose ps -q bpi-wms-adapter)
if [ -z "$postgres_id" ] || [ -z "$service_id" ]; then
    printf 'ERROR: BPI PostgreSQL and service must be running before an expand-only upgrade\n' >&2
    exit 1
fi
if [ "$(env_value BPI_WMS_ADAPTER_ENABLED false)" != "false" ]; then
    printf 'ERROR: BPI_WMS_ADAPTER_ENABLED must remain false during expand-only upgrade\n' >&2
    exit 1
fi

query_version() {
    compose exec -T bpi-postgres psql -X -At -U "$POSTGRES_USER" -d "$DATABASE_NAME" \
        -c "SELECT max(version::integer)::text FROM bpi.flyway_schema_history WHERE success"
}

wait_for_service_health() {
    service_name=${1:-bpi-service}
    deadline=$(( $(date +%s) + HEALTH_TIMEOUT_SECONDS ))
    while [ "$(date +%s)" -lt "$deadline" ]; do
        current_id=$(compose ps -q "$service_name")
        if [ -n "$current_id" ]; then
            health_status=$(docker inspect --format \
                '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' \
                "$current_id" 2>/dev/null || true)
            case "$health_status" in
                healthy) return 0 ;;
                exited|dead)
                    printf 'ERROR: %s stopped while waiting for readiness\n' "$service_name" >&2
                    compose logs --tail 120 "$service_name" >&2 || true
                    return 1
                    ;;
            esac
        fi
        sleep 2
    done

    printf 'ERROR: %s did not become healthy within %s seconds\n' \
        "$service_name" "$HEALTH_TIMEOUT_SECONDS" >&2
    compose ps "$service_name" >&2 || true
    compose logs --tail 120 "$service_name" >&2 || true
    return 1
}

write_report() {
    UPGRADE_STATUS=$1
    UPGRADE_PHASE=$2
    SMOKE_STATUS=$3
    AFTER_IMAGE_ID=${4:-}
    AFTER_WMS_ADAPTER_IMAGE_ID=${5:-}
    export REPORT UPGRADE_STATUS UPGRADE_PHASE SMOKE_STATUS
    export BEFORE_VERSION AFTER_VERSION EXPECTED_VERSION BACKUP_FILE ENV_BACKUP
    export ROLLBACK_IMAGE BEFORE_IMAGE_ID AFTER_IMAGE_ID JAR JAR_SHA256
    export WMS_ADAPTER_JAR WMS_ADAPTER_JAR_SHA256
    export BEFORE_WMS_ADAPTER_IMAGE_ID AFTER_WMS_ADAPTER_IMAGE_ID ROLLBACK_WMS_ADAPTER_IMAGE
    python3 <<'PY'
import datetime
import json
import os
from pathlib import Path


def optional_int(name):
    value = os.environ.get(name, "")
    return int(value) if value else None


report = {
    "generatedAt": datetime.datetime.now(datetime.timezone.utc).isoformat(),
    "status": os.environ["UPGRADE_STATUS"],
    "phase": os.environ["UPGRADE_PHASE"],
    "strategy": "EXPAND_ONLY",
    "recoveryRequired": os.environ["UPGRADE_STATUS"] != "PASS",
    "database": {
        "engine": "PostgreSQL",
        "beforeFlywayVersion": int(os.environ["BEFORE_VERSION"]),
        "afterFlywayVersion": optional_int("AFTER_VERSION"),
        "expectedFlywayVersion": int(os.environ["EXPECTED_VERSION"]),
        "backup": os.environ["BACKUP_FILE"],
    },
    "runtime": {
        "jar": os.environ["JAR"],
        "jarSha256": os.environ["JAR_SHA256"],
        "beforeImageId": os.environ["BEFORE_IMAGE_ID"],
        "afterImageId": os.environ.get("AFTER_IMAGE_ID") or None,
        "rollbackImage": os.environ["ROLLBACK_IMAGE"],
        "environmentBackup": os.environ["ENV_BACKUP"],
        "smoke": os.environ["SMOKE_STATUS"],
    },
    "wmsAdapter": {
        "jar": os.environ["WMS_ADAPTER_JAR"],
        "jarSha256": os.environ["WMS_ADAPTER_JAR_SHA256"],
        "beforeImageId": os.environ.get("BEFORE_WMS_ADAPTER_IMAGE_ID") or None,
        "afterImageId": os.environ.get("AFTER_WMS_ADAPTER_IMAGE_ID") or None,
        "rollbackImage": os.environ.get("ROLLBACK_WMS_ADAPTER_IMAGE") or None,
        "enabledDuringUpgrade": False,
    },
    "rollbackBoundary": {
        "schemaDowngrade": False,
        "method": "Restore the tagged application image and keep the expanded schema.",
    },
}
path = Path(os.environ["REPORT"])
path.parent.mkdir(parents=True, exist_ok=True)
path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(f"BPI runtime upgrade state: {report['status']} / {report['phase']} ({path})")
PY
}

BEFORE_VERSION=$(query_version)
case "$BEFORE_VERSION" in
    ''|*[!0-9]*) printf 'ERROR: current Flyway version is not numeric: %s\n' "$BEFORE_VERSION" >&2; exit 1 ;;
esac
if [ "$BEFORE_VERSION" -ge "$EXPECTED_VERSION" ]; then
    printf 'ERROR: current Flyway version %s is not below target %s\n' \
        "$BEFORE_VERSION" "$EXPECTED_VERSION" >&2
    exit 1
fi

timestamp=$(date -u +%Y%m%dT%H%M%SZ)
mkdir -p "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR"
BACKUP_FILE="$BACKUP_DIR/ft_mes_bpi-before-v${EXPECTED_VERSION}-${timestamp}.dump"
ENV_BACKUP="$BACKUP_DIR/bpi-runtime-before-v${EXPECTED_VERSION}-${timestamp}.env"
ROLLBACK_IMAGE="ft-mes-bpi-service:rollback-v${BEFORE_VERSION}-${timestamp}"
BEFORE_IMAGE_ID=$(docker inspect --format '{{.Image}}' "$service_id")
BEFORE_WMS_ADAPTER_IMAGE_ID=
ROLLBACK_WMS_ADAPTER_IMAGE=
if [ -n "$wms_adapter_id" ]; then
    BEFORE_WMS_ADAPTER_IMAGE_ID=$(docker inspect --format '{{.Image}}' "$wms_adapter_id")
    ROLLBACK_WMS_ADAPTER_IMAGE="ft-mes-bpi-wms-adapter:rollback-v${BEFORE_VERSION}-${timestamp}"
fi
docker image tag "$BEFORE_IMAGE_ID" "$ROLLBACK_IMAGE"
if [ -n "$BEFORE_WMS_ADAPTER_IMAGE_ID" ]; then
    docker image tag "$BEFORE_WMS_ADAPTER_IMAGE_ID" "$ROLLBACK_WMS_ADAPTER_IMAGE"
fi
cp "$ENV_FILE" "$ENV_BACKUP"
chmod 600 "$ENV_BACKUP"
compose exec -T bpi-postgres pg_dump -Fc -U "$POSTGRES_USER" -d "$DATABASE_NAME" >"$BACKUP_FILE"
chmod 600 "$BACKUP_FILE"
test -s "$BACKUP_FILE" || {
    printf 'ERROR: PostgreSQL backup is empty: %s\n' "$BACKUP_FILE" >&2
    exit 1
}

JAR="$ROOT_DIR/services/bpi-service/app/target/bpi-service-0.1.0-SNAPSHOT-exec.jar"
test -f "$JAR" || {
    printf 'ERROR: package BPI service before upgrade: %s\n' "$JAR" >&2
    exit 1
}
JAR_SHA256=$(sha256sum "$JAR" | awk '{print $1}')
WMS_ADAPTER_JAR="$ROOT_DIR/services/bpi-service/wms-adapter/target/bpi-wms-adapter-0.1.0-SNAPSHOT-exec.jar"
test -f "$WMS_ADAPTER_JAR" || {
    printf 'ERROR: package BPI WMS adapter before upgrade: %s\n' "$WMS_ADAPTER_JAR" >&2
    exit 1
}
WMS_ADAPTER_JAR_SHA256=$(sha256sum "$WMS_ADAPTER_JAR" | awk '{print $1}')
AFTER_VERSION=
write_report PREPARED BACKUP_COMPLETE NOT_RUN

compose build bpi-service bpi-wms-adapter
compose up --no-deps --force-recreate --abort-on-container-exit \
    --exit-code-from bpi-migrate bpi-migrate

AFTER_VERSION=$(query_version)
if [ "$AFTER_VERSION" != "$EXPECTED_VERSION" ]; then
    printf 'ERROR: expected Flyway %s after migration, found %s\n' \
        "$EXPECTED_VERSION" "$AFTER_VERSION" >&2
    exit 1
fi
write_report IN_PROGRESS MIGRATION_APPLIED NOT_RUN

compose up -d --no-deps --force-recreate bpi-service
AFTER_SERVICE_ID=$(compose ps -q bpi-service)
AFTER_IMAGE_ID=$(docker inspect --format '{{.Image}}' "$AFTER_SERVICE_ID")
compose up -d --no-deps --force-recreate bpi-wms-adapter
AFTER_WMS_ADAPTER_ID=$(compose ps -q bpi-wms-adapter)
AFTER_WMS_ADAPTER_IMAGE_ID=$(docker inspect --format '{{.Image}}' "$AFTER_WMS_ADAPTER_ID")
write_report IN_PROGRESS SERVICES_RECREATED NOT_RUN "$AFTER_IMAGE_ID" "$AFTER_WMS_ADAPTER_IMAGE_ID"
wait_for_service_health bpi-service
wait_for_service_health bpi-wms-adapter
sh "$SCRIPT_DIR/smoke.sh" "$ENV_FILE"
write_report PASS COMPLETE PASS "$AFTER_IMAGE_ID" "$AFTER_WMS_ADAPTER_IMAGE_ID"
printf 'BPI runtime expand-only upgrade: PASS (%s)\n' "$REPORT"
printf 'Rollback image: %s\n' "$ROLLBACK_IMAGE"
