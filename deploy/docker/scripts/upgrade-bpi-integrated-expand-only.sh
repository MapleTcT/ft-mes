#!/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
RELEASE_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../../.." && pwd)
RUNTIME_ROOT=${BPI_INTEGRATED_RUNTIME_ROOT:-}
BACKUP_DIR=${BPI_INTEGRATED_BACKUP_DIR:-}
EXPECTED_VERSION=${BPI_EXPECTED_FLYWAY_VERSION:-}
CONFIRM=${BPI_INTEGRATED_UPGRADE_CONFIRM:-}
HEALTH_TIMEOUT_SECONDS=${BPI_INTEGRATED_HEALTH_TIMEOUT_SECONDS:-240}
BUILD_UI=${BPI_INTEGRATED_BUILD_UI:-true}

if [ "$CONFIRM" != "UPGRADE_INTEGRATED_BPI_EXPAND_ONLY" ]; then
    printf 'ERROR: set BPI_INTEGRATED_UPGRADE_CONFIRM=UPGRADE_INTEGRATED_BPI_EXPAND_ONLY\n' >&2
    exit 1
fi
case "$RUNTIME_ROOT" in
    /) printf 'ERROR: BPI_INTEGRATED_RUNTIME_ROOT cannot be /\n' >&2; exit 1 ;;
    /*) ;;
    *) printf 'ERROR: BPI_INTEGRATED_RUNTIME_ROOT must be absolute\n' >&2; exit 1 ;;
esac
case "$BACKUP_DIR" in
    /) printf 'ERROR: BPI_INTEGRATED_BACKUP_DIR cannot be /\n' >&2; exit 1 ;;
    /*) ;;
    *) printf 'ERROR: BPI_INTEGRATED_BACKUP_DIR must be absolute\n' >&2; exit 1 ;;
esac
case "$EXPECTED_VERSION" in
    ''|*[!0-9]*) printf 'ERROR: BPI_EXPECTED_FLYWAY_VERSION must be numeric\n' >&2; exit 1 ;;
esac
case "$HEALTH_TIMEOUT_SECONDS" in
    ''|*[!0-9]*|0) printf 'ERROR: BPI_INTEGRATED_HEALTH_TIMEOUT_SECONDS must be positive\n' >&2; exit 1 ;;
esac
case "$BUILD_UI" in
    true|false) ;;
    *) printf 'ERROR: BPI_INTEGRATED_BUILD_UI must be true or false\n' >&2; exit 1 ;;
esac

DEPLOY_DIR="$RUNTIME_ROOT/deploy/docker"
ENV_FILE="$DEPLOY_DIR/.env"
COMPOSE_FILE="$DEPLOY_DIR/docker-compose.yml"
UI_TARGET="$RUNTIME_ROOT/frontend/apps/bpi/dist"
UI_RELEASE="$RELEASE_ROOT/frontend/apps/bpi/dist"
MIGRATIONS="$RELEASE_ROOT/services/bpi-service/app/src/main/resources/db/migration"

for path in "$ENV_FILE" "$COMPOSE_FILE" "$MIGRATIONS"; do
    if [ ! -e "$path" ]; then
        printf 'ERROR: required integrated runtime path is missing: %s\n' "$path" >&2
        exit 1
    fi
done
for command_name in docker git npm python3 rsync sha256sum tar; do
    if ! command -v "$command_name" >/dev/null 2>&1; then
        printf 'ERROR: required command is unavailable: %s\n' "$command_name" >&2
        exit 1
    fi
done

env_value() {
    key=$1
    fallback=$2
    value=$(sed -n "s/^${key}=//p" "$ENV_FILE" | tail -1)
    printf '%s' "${value:-$fallback}"
}

container_env_value() {
    container_id=$1
    key=$2
    docker inspect --format '{{range .Config.Env}}{{println .}}{{end}}' "$container_id" \
        | sed -n "s/^${key}=//p" \
        | tail -1
}

compose() {
    docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" --profile bpi "$@"
}

query_version() {
    docker exec "$POSTGRES_ID" psql -X -At -U "$POSTGRES_USER" -d "$DATABASE_NAME" \
        -c "SELECT max(version::integer)::text FROM bpi.flyway_schema_history WHERE success"
}

wait_for_health() {
    service=$1
    deadline=$(( $(date +%s) + HEALTH_TIMEOUT_SECONDS ))
    while [ "$(date +%s)" -lt "$deadline" ]; do
        container_id=$(compose ps -q "$service")
        if [ -n "$container_id" ]; then
            state=$(docker inspect --format \
                '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' \
                "$container_id" 2>/dev/null || true)
            case "$state" in
                healthy) return 0 ;;
                exited|dead)
                    printf 'ERROR: %s stopped during readiness wait\n' "$service" >&2
                    compose logs --tail 160 "$service" >&2 || true
                    return 1
                    ;;
            esac
        fi
        sleep 2
    done
    printf 'ERROR: %s did not become healthy within %s seconds\n' \
        "$service" "$HEALTH_TIMEOUT_SECONDS" >&2
    compose logs --tail 160 "$service" >&2 || true
    return 1
}

replace_env_images() {
    python3 - "$ENV_FILE" "$SERVICE_IMAGE" "$ADAPTER_IMAGE" "$EXPECTED_VERSION" <<'PY'
import os
import stat
import sys
import tempfile
from pathlib import Path

path = Path(sys.argv[1])
updates = {
    "BPI_SERVICE_IMAGE": sys.argv[2],
    "BPI_ADAPTER_IMAGE": sys.argv[3],
    "BPI_EXPECTED_FLYWAY_VERSION": sys.argv[4],
}
lines = path.read_text(encoding="utf-8").splitlines()
seen = set()
output = []
for line in lines:
    key = line.split("=", 1)[0] if "=" in line else ""
    if key in updates:
        output.append(f"{key}={updates[key]}")
        seen.add(key)
    else:
        output.append(line)
for key, value in updates.items():
    if key not in seen:
        output.append(f"{key}={value}")

mode = stat.S_IMODE(path.stat().st_mode)
with tempfile.NamedTemporaryFile("w", encoding="utf-8", dir=path.parent, delete=False) as handle:
    handle.write("\n".join(output) + "\n")
    temporary = Path(handle.name)
os.chmod(temporary, mode)
os.replace(temporary, path)
PY
}

write_report() {
    report_status=$1
    report_phase=$2
    export REPORT report_status report_phase
    export RELEASE_COMMIT EXPECTED_VERSION BEFORE_VERSION AFTER_VERSION
    export SERVICE_IMAGE ADAPTER_IMAGE BEFORE_SERVICE_IMAGE_ID BEFORE_ADAPTER_IMAGE_ID
    export AFTER_SERVICE_IMAGE_ID AFTER_ADAPTER_IMAGE_ID ROLLBACK_SERVICE_IMAGE ROLLBACK_ADAPTER_IMAGE
    export DATABASE_BACKUP ENV_BACKUP UI_BACKUP UI_SHA256
    python3 <<'PY'
import datetime
import json
import os
from pathlib import Path

def value(name):
    return os.environ.get(name) or None

def number(name):
    current = value(name)
    return int(current) if current else None

report = {
    "generatedAt": datetime.datetime.now(datetime.timezone.utc).isoformat(),
    "status": os.environ["report_status"],
    "phase": os.environ["report_phase"],
    "strategy": "INTEGRATED_EXPAND_ONLY",
    "releaseCommit": value("RELEASE_COMMIT"),
    "database": {
        "engine": "PostgreSQL",
        "beforeFlywayVersion": number("BEFORE_VERSION"),
        "afterFlywayVersion": number("AFTER_VERSION"),
        "expectedFlywayVersion": number("EXPECTED_VERSION"),
        "backup": value("DATABASE_BACKUP"),
        "schemaDowngradeAllowed": False,
    },
    "runtime": {
        "service": {
            "beforeImageId": value("BEFORE_SERVICE_IMAGE_ID"),
            "afterImageId": value("AFTER_SERVICE_IMAGE_ID"),
            "releaseImage": value("SERVICE_IMAGE"),
            "rollbackImage": value("ROLLBACK_SERVICE_IMAGE"),
        },
        "adapter": {
            "beforeImageId": value("BEFORE_ADAPTER_IMAGE_ID"),
            "afterImageId": value("AFTER_ADAPTER_IMAGE_ID"),
            "releaseImage": value("ADAPTER_IMAGE"),
            "rollbackImage": value("ROLLBACK_ADAPTER_IMAGE"),
        },
        "environmentBackup": value("ENV_BACKUP"),
        "uiBackup": value("UI_BACKUP"),
        "uiIndexSha256": value("UI_SHA256"),
    },
    "safety": {
        "phase2IntegrationDefault": False,
        "wmsOutboxDefault": False,
        "rollbackMethod": "Restore the recorded service/adapter images and UI backup; keep the expanded schema.",
    },
}
path = Path(os.environ["REPORT"])
path.parent.mkdir(parents=True, exist_ok=True)
path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(f"Integrated BPI upgrade report: {report['status']} / {report['phase']} ({path})")
PY
}

timestamp=$(date -u +%Y%m%dT%H%M%SZ)
release_suffix=$(printf '%s' "$timestamp" | tr '[:upper:]' '[:lower:]')
RELEASE_COMMIT=$(git -C "$RELEASE_ROOT" rev-parse HEAD)
short_commit=$(printf '%s' "$RELEASE_COMMIT" | cut -c1-12)
SERVICE_IMAGE=${BPI_INTEGRATED_SERVICE_IMAGE:-ft-mes-bpi-service:${release_suffix}-${short_commit}}
ADAPTER_IMAGE=${BPI_INTEGRATED_ADAPTER_IMAGE:-ft-mes-bpi-adapter:${release_suffix}-${short_commit}}
REPORT=${BPI_INTEGRATED_UPGRADE_REPORT:-$BACKUP_DIR/bpi-integrated-upgrade-${timestamp}.json}

POSTGRES_ID=$(compose ps -q postgres)
SERVICE_ID=$(compose ps -q bpi-service)
ADAPTER_ID=$(compose ps -q bpi-adapter)
NGINX_ID=$(compose ps -q nginx)
if [ -z "$POSTGRES_ID" ] || [ -z "$SERVICE_ID" ] || [ -z "$ADAPTER_ID" ] || [ -z "$NGINX_ID" ]; then
    printf 'ERROR: postgres, bpi-service, bpi-adapter and nginx must all be running\n' >&2
    exit 1
fi

DATABASE_NAME=$(env_value BPI_DATABASE_NAME ft_mes_bpi)
POSTGRES_USER=$(env_value POSTGRES_USER adp)
MIGRATOR_PASSWORD=$(env_value BPI_MIGRATOR_PASSWORD '')
if [ -z "$MIGRATOR_PASSWORD" ]; then
    MIGRATOR_PASSWORD=$(container_env_value "$POSTGRES_ID" BPI_MIGRATOR_PASSWORD)
fi
FLYWAY_IMAGE=$(env_value BPI_FLYWAY_IMAGE m.daocloud.io/docker.io/flyway/flyway:11-alpine)
if [ -z "$MIGRATOR_PASSWORD" ]; then
    printf 'ERROR: BPI_MIGRATOR_PASSWORD is missing\n' >&2
    exit 1
fi
for disabled_key in \
    BPI_PHASE2_INTEGRATION_ENABLED \
    BPI_PHASE2_PROTOBUF_HTTP_INGRESS_ENABLED \
    BPI_PHASE2_KAFKA_ENABLED \
    BPI_WMS_OUTBOX_ENABLED; do
    if [ "$(env_value "$disabled_key" false)" != "false" ]; then
        printf 'ERROR: %s must remain false during the integrated expand-only upgrade\n' \
            "$disabled_key" >&2
        exit 1
    fi
done

BEFORE_VERSION=$(query_version)
case "$BEFORE_VERSION" in
    ''|*[!0-9]*) printf 'ERROR: current Flyway version is not numeric: %s\n' "$BEFORE_VERSION" >&2; exit 1 ;;
esac
if [ "$BEFORE_VERSION" -ge "$EXPECTED_VERSION" ]; then
    printf 'ERROR: current Flyway version %s is not below target %s\n' \
        "$BEFORE_VERSION" "$EXPECTED_VERSION" >&2
    exit 1
fi
set -- "$MIGRATIONS/V${EXPECTED_VERSION}__"*.sql
if [ ! -f "$1" ]; then
    printf 'ERROR: expected migration V%s is absent from release %s\n' \
        "$EXPECTED_VERSION" "$RELEASE_COMMIT" >&2
    exit 1
fi

phase=BUILDING
AFTER_VERSION=
AFTER_SERVICE_IMAGE_ID=
AFTER_ADAPTER_IMAGE_ID=
DATABASE_BACKUP=
ENV_BACKUP=
UI_BACKUP=
UI_SHA256=
BEFORE_SERVICE_IMAGE_ID=$(docker inspect --format '{{.Image}}' "$SERVICE_ID")
BEFORE_ADAPTER_IMAGE_ID=$(docker inspect --format '{{.Image}}' "$ADAPTER_ID")
ROLLBACK_SERVICE_IMAGE="ft-mes-bpi-service:rollback-v${BEFORE_VERSION}-${release_suffix}"
ROLLBACK_ADAPTER_IMAGE="ft-mes-bpi-adapter:rollback-v${BEFORE_VERSION}-${release_suffix}"
trap 'exit_code=$?; if [ "$exit_code" -ne 0 ]; then write_report FAIL "$phase" || true; fi' EXIT HUP INT TERM

if [ "$BUILD_UI" = "true" ]; then
    (
        cd "$RELEASE_ROOT/frontend/apps/bpi"
        npm ci --no-audit --no-fund
        npm run build
    )
fi
test -s "$UI_RELEASE/index.html" || {
    printf 'ERROR: BPI UI build output is missing: %s\n' "$UI_RELEASE/index.html" >&2
    exit 1
}

docker build \
    --build-arg "MAVEN_IMAGE=$(env_value BPI_MAVEN_IMAGE maven:3.9.9-eclipse-temurin-17)" \
    --build-arg "JAVA_IMAGE=$(env_value BPI_JAVA_IMAGE eclipse-temurin:17-jre-jammy)" \
    --label "org.opencontainers.image.revision=$RELEASE_COMMIT" \
    -f "$RELEASE_ROOT/services/bpi-service/Dockerfile" \
    -t "$SERVICE_IMAGE" "$RELEASE_ROOT"
docker build \
    --build-arg "MAVEN_IMAGE=$(env_value BPI_ADAPTER_MAVEN_IMAGE maven:3.9.9-eclipse-temurin-8)" \
    --build-arg "JAVA_IMAGE=$(env_value BPI_ADAPTER_JAVA_IMAGE eclipse-temurin:8-jre-jammy)" \
    --label "org.opencontainers.image.revision=$RELEASE_COMMIT" \
    -f "$RELEASE_ROOT/backend/source-modules/batch-intelligence-adapter/Dockerfile" \
    -t "$ADAPTER_IMAGE" "$RELEASE_ROOT"

phase=BACKUP
mkdir -p "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR"
DATABASE_BACKUP="$BACKUP_DIR/ft_mes_bpi-before-v${EXPECTED_VERSION}-${timestamp}.dump"
ENV_BACKUP="$BACKUP_DIR/adp-compose-before-v${EXPECTED_VERSION}-${timestamp}.env"
UI_BACKUP="$BACKUP_DIR/bpi-ui-before-v${EXPECTED_VERSION}-${timestamp}.tar.gz"
docker image tag "$BEFORE_SERVICE_IMAGE_ID" "$ROLLBACK_SERVICE_IMAGE"
docker image tag "$BEFORE_ADAPTER_IMAGE_ID" "$ROLLBACK_ADAPTER_IMAGE"
cp "$ENV_FILE" "$ENV_BACKUP"
chmod 600 "$ENV_BACKUP"
docker exec "$POSTGRES_ID" pg_dump -Fc -U "$POSTGRES_USER" -d "$DATABASE_NAME" >"$DATABASE_BACKUP"
chmod 600 "$DATABASE_BACKUP"
test -s "$DATABASE_BACKUP" || {
    printf 'ERROR: PostgreSQL backup is empty: %s\n' "$DATABASE_BACKUP" >&2
    exit 1
}
if [ -d "$UI_TARGET" ]; then
    tar -czf "$UI_BACKUP" -C "$UI_TARGET" .
else
    mkdir -p "$UI_TARGET"
    tar -czf "$UI_BACKUP" --files-from /dev/null
fi
chmod 600 "$UI_BACKUP"
write_report PREPARED BACKUP_COMPLETE

phase=MIGRATION
runtime_network=$(docker inspect --format \
    '{{range $name, $_ := .NetworkSettings.Networks}}{{println $name}}{{end}}' \
    "$POSTGRES_ID" | head -1)
if [ -z "$runtime_network" ]; then
    printf 'ERROR: cannot resolve the integrated PostgreSQL network\n' >&2
    exit 1
fi
docker run --rm \
    --network "$runtime_network" \
    -e "FLYWAY_URL=jdbc:postgresql://postgres:5432/$DATABASE_NAME" \
    -e FLYWAY_USER=bpi_migrator \
    -e "FLYWAY_PASSWORD=$MIGRATOR_PASSWORD" \
    -e FLYWAY_SCHEMAS=bpi \
    -e FLYWAY_DEFAULT_SCHEMA=bpi \
    -e FLYWAY_CONNECT_RETRIES=20 \
    -v "$MIGRATIONS:/flyway/sql:ro" \
    "$FLYWAY_IMAGE" migrate
AFTER_VERSION=$(query_version)
if [ "$AFTER_VERSION" != "$EXPECTED_VERSION" ]; then
    printf 'ERROR: expected Flyway %s, found %s after migration\n' \
        "$EXPECTED_VERSION" "$AFTER_VERSION" >&2
    exit 1
fi
write_report IN_PROGRESS MIGRATION_APPLIED

phase=SERVICE_RECREATE
replace_env_images
compose up -d --no-deps --force-recreate bpi-service
wait_for_health bpi-service
compose up -d --no-deps --force-recreate bpi-adapter
wait_for_health bpi-adapter
SERVICE_ID=$(compose ps -q bpi-service)
ADAPTER_ID=$(compose ps -q bpi-adapter)
AFTER_SERVICE_IMAGE_ID=$(docker inspect --format '{{.Image}}' "$SERVICE_ID")
AFTER_ADAPTER_IMAGE_ID=$(docker inspect --format '{{.Image}}' "$ADAPTER_ID")
write_report IN_PROGRESS APPLICATIONS_HEALTHY

phase=UI_DEPLOY
rsync -a --delete "$UI_RELEASE/" "$UI_TARGET/"
UI_SHA256=$(sha256sum "$UI_TARGET/index.html" | awk '{print $1}')
docker exec "$NGINX_ID" test -s /usr/share/nginx/bpi/index.html

phase=FINAL_SMOKE
service_health=$(curl -fsS --connect-timeout 5 --max-time 20 \
    "http://127.0.0.1:$(env_value BPI_HTTP_PORT 19091)/actuator/health")
printf '%s' "$service_health" | grep -q '"status":"UP"'
adapter_health=$(curl -fsS --connect-timeout 5 --max-time 20 \
    "http://127.0.0.1:$(env_value BPI_ADAPTER_HTTP_PORT 19080)/actuator/health")
printf '%s' "$adapter_health" | grep -q '"status":"UP"'
flag_state=$(docker exec "$POSTGRES_ID" psql -X -At -U "$POSTGRES_USER" -d "$DATABASE_NAME" \
    -c "SELECT string_agg(flag_key || '=' || enabled::text, ',' ORDER BY flag_key) FROM bpi.bpi_feature_flags WHERE tenant_id = '*' AND scope_type = 'GLOBAL' AND flag_key IN ('bpi.auto-confirm','bpi.qcs-link','bpi.shadow-only','bpi.wms-link')")
printf '%s' "$flag_state" | grep -q 'bpi.auto-confirm=false'
printf '%s' "$flag_state" | grep -q 'bpi.qcs-link=false'
printf '%s' "$flag_state" | grep -q 'bpi.shadow-only=true'
printf '%s' "$flag_state" | grep -q 'bpi.wms-link=false'

write_report PASS COMPLETE
trap - EXIT HUP INT TERM
printf 'Integrated BPI expand-only upgrade: PASS (Flyway %s, release %s)\n' \
    "$AFTER_VERSION" "$RELEASE_COMMIT"
printf 'Service image: %s\nAdapter image: %s\nReport: %s\n' \
    "$SERVICE_IMAGE" "$ADAPTER_IMAGE" "$REPORT"
