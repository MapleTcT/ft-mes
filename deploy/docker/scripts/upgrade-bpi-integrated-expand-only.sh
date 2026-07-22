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
ALLOW_ALREADY_MIGRATED=${BPI_INTEGRATED_ALLOW_ALREADY_MIGRATED:-false}
ALREADY_MIGRATED_CONFIRM=${BPI_INTEGRATED_ALREADY_MIGRATED_CONFIRM:-}

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
case "$ALLOW_ALREADY_MIGRATED" in
    true|false) ;;
    *) printf 'ERROR: BPI_INTEGRATED_ALLOW_ALREADY_MIGRATED must be true or false\n' >&2; exit 1 ;;
esac

DEPLOY_DIR="$RUNTIME_ROOT/deploy/docker"
ENV_FILE="$DEPLOY_DIR/.env"
COMPOSE_FILE="$DEPLOY_DIR/docker-compose.yml"
RELEASE_COMPOSE_FILE="$RELEASE_ROOT/deploy/docker/docker-compose.yml"
RELEASE_MINIO_DIR="$RELEASE_ROOT/deploy/minio"
RUNTIME_MINIO_DIR="$RUNTIME_ROOT/deploy/minio"
RELEASE_POLARIS_DIR="$RELEASE_ROOT/deploy/polaris"
RUNTIME_POLARIS_DIR="$RUNTIME_ROOT/deploy/polaris"
UI_TARGET="$RUNTIME_ROOT/frontend/apps/bpi/dist"
UI_RELEASE="$RELEASE_ROOT/frontend/apps/bpi/dist"
MIGRATIONS="$RELEASE_ROOT/services/bpi-service/app/src/main/resources/db/migration"
VERIFY_MIGRATIONS="$RELEASE_ROOT/scripts/verify-bpi-release-migrations.py"
MATERIALIZER_ROLE_SCRIPT="$RELEASE_ROOT/deploy/docker/postgres/ensure-bpi-materializer-role.sh"
CATALOG_PUBLISHER_ROLE_SCRIPT="$RELEASE_ROOT/deploy/docker/postgres/ensure-bpi-catalog-publisher-role.sh"

for path in \
    "$ENV_FILE" \
    "$COMPOSE_FILE" \
    "$RELEASE_COMPOSE_FILE" \
    "$RELEASE_MINIO_DIR/bootstrap-bpi-dataset-bucket.sh" \
    "$RELEASE_MINIO_DIR/bpi-dataset-materializer-policy.json" \
    "$RELEASE_MINIO_DIR/bpi-dataset-catalog-source-reader-policy.json" \
    "$RELEASE_MINIO_DIR/bootstrap-bpi-iceberg-warehouse.sh" \
    "$RELEASE_MINIO_DIR/bpi-iceberg-warehouse-policy.json" \
    "$RELEASE_POLARIS_DIR/bootstrap_bpi_catalog.py" \
    "$RELEASE_POLARIS_DIR/check_metastore_bootstrap.sh" \
    "$RELEASE_POLARIS_DIR/bootstrap_metastore_if_required.sh" \
    "$MIGRATIONS" \
    "$VERIFY_MIGRATIONS" \
    "$MATERIALIZER_ROLE_SCRIPT" \
    "$CATALOG_PUBLISHER_ROLE_SCRIPT"; do
    if [ ! -e "$path" ]; then
        printf 'ERROR: required integrated runtime path is missing: %s\n' "$path" >&2
        exit 1
    fi
done
for command_name in curl docker git mktemp npm python3 rsync sha256sum tar; do
    if ! command -v "$command_name" >/dev/null 2>&1; then
        printf 'ERROR: required command is unavailable: %s\n' "$command_name" >&2
        exit 1
    fi
done

if [ -n "$(git -C "$RELEASE_ROOT" status --porcelain --untracked-files=normal)" ]; then
    printf 'ERROR: integrated BPI release checkout must be clean before image build\n' >&2
    exit 1
fi

env_value() {
    key=$1
    fallback=$2
    value=$(sed -n "s/^${key}=//p" "$ENV_FILE" | tail -1)
    printf '%s' "${value:-$fallback}"
}

required_env_value() {
    key=$1
    value=$(sed -n "s/^${key}=//p" "$ENV_FILE" | tail -1)
    test -n "$value" || {
        printf 'ERROR: %s must be explicitly set in %s\n' "$key" "$ENV_FILE" >&2
        exit 1
    }
    printf '%s' "$value"
}

required_secret() {
    key=$1
    value=$(required_env_value "$key")
    case "$value" in
        *change-me*|*dev-only*|*_DISABLED_*)
            printf 'ERROR: %s still contains a placeholder value\n' "$key" >&2
            exit 1
            ;;
    esac
    printf '%s' "$value"
}

require_integer_range() {
    key=$1
    minimum=$2
    maximum=$3
    value=$(required_env_value "$key")
    case "$value" in
        ''|*[!0-9]*)
            printf 'ERROR: %s must be an integer\n' "$key" >&2
            exit 1
            ;;
    esac
    if [ "$value" -lt "$minimum" ] || [ "$value" -gt "$maximum" ]; then
        printf 'ERROR: %s must be between %s and %s\n' \
            "$key" "$minimum" "$maximum" >&2
        exit 1
    fi
}

compose() {
    docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" --profile bpi "$@"
}

compose_expand() {
    BPI_DATASET_MATERIALIZER_ENABLED=false \
    BPI_DATASET_BUCKET_BOOTSTRAP_ENABLED=false \
    BPI_DATASET_SOURCE_READER_ENABLED=false \
    BPI_POLARIS_ENABLED=false \
    BPI_POLARIS_CATALOG_BOOTSTRAP_ENABLED=false \
    BPI_ICEBERG_WAREHOUSE_BOOTSTRAP_ENABLED=false \
    BPI_DATASET_CATALOG_PUBLISHER_ENABLED=false \
        docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" --profile bpi "$@"
}

validate_release_deployment_manifests() {
    docker compose --env-file "$ENV_FILE" -f "$RELEASE_COMPOSE_FILE" \
        --profile bpi --profile bpi-catalog config --quiet
    release_services=$(docker compose --env-file "$ENV_FILE" \
        -f "$RELEASE_COMPOSE_FILE" --profile bpi --profile bpi-catalog config --services)
    for service in bpi-service bpi-adapter bpi-wms-adapter \
        bpi-dataset-minio-bootstrap bpi-dataset-materializer \
        bpi-polaris-postgres bpi-polaris-bootstrap-check bpi-polaris-bootstrap bpi-polaris \
        bpi-iceberg-warehouse-bootstrap \
        bpi-polaris-catalog-bootstrap bpi-dataset-catalog-publisher; do
        if ! printf '%s\n' "$release_services" | grep -qx "$service"; then
            printf 'ERROR: release Compose is missing required service: %s\n' \
                "$service" >&2
            exit 1
        fi
    done
}

stage_runtime_deployment_manifests() {
    mkdir -p "$BACKUP_DIR"
    chmod 700 "$BACKUP_DIR"

    cp "$COMPOSE_FILE" "$COMPOSE_BACKUP"
    chmod 600 "$COMPOSE_BACKUP"
    if [ -d "$RUNTIME_MINIO_DIR" ]; then
        tar -czf "$MINIO_CONFIG_BACKUP" -C "$RUNTIME_ROOT/deploy" minio
    else
        tar -czf "$MINIO_CONFIG_BACKUP" --files-from /dev/null
    fi
    chmod 600 "$MINIO_CONFIG_BACKUP"
    if [ -d "$RUNTIME_POLARIS_DIR" ]; then
        tar -czf "$POLARIS_CONFIG_BACKUP" -C "$RUNTIME_ROOT/deploy" polaris
    else
        tar -czf "$POLARIS_CONFIG_BACKUP" --files-from /dev/null
    fi
    chmod 600 "$POLARIS_CONFIG_BACKUP"

    temporary_compose=$(mktemp "$DEPLOY_DIR/.docker-compose.v${EXPECTED_VERSION}.XXXXXX")
    cp "$RELEASE_COMPOSE_FILE" "$temporary_compose"
    chmod 644 "$temporary_compose"
    mv "$temporary_compose" "$COMPOSE_FILE"
    mkdir -p "$RUNTIME_MINIO_DIR"
    rsync -a "$RELEASE_MINIO_DIR/bootstrap-bpi-dataset-bucket.sh" \
        "$RUNTIME_MINIO_DIR/bootstrap-bpi-dataset-bucket.sh"
    rsync -a "$RELEASE_MINIO_DIR/bpi-dataset-materializer-policy.json" \
        "$RUNTIME_MINIO_DIR/bpi-dataset-materializer-policy.json"
    rsync -a "$RELEASE_MINIO_DIR/bpi-dataset-catalog-source-reader-policy.json" \
        "$RUNTIME_MINIO_DIR/bpi-dataset-catalog-source-reader-policy.json"
    rsync -a "$RELEASE_MINIO_DIR/bootstrap-bpi-iceberg-warehouse.sh" \
        "$RUNTIME_MINIO_DIR/bootstrap-bpi-iceberg-warehouse.sh"
    rsync -a "$RELEASE_MINIO_DIR/bpi-iceberg-warehouse-policy.json" \
        "$RUNTIME_MINIO_DIR/bpi-iceberg-warehouse-policy.json"
    mkdir -p "$RUNTIME_POLARIS_DIR"
    rsync -a "$RELEASE_POLARIS_DIR/bootstrap_bpi_catalog.py" \
        "$RUNTIME_POLARIS_DIR/bootstrap_bpi_catalog.py"
    rsync -a "$RELEASE_POLARIS_DIR/check_metastore_bootstrap.sh" \
        "$RUNTIME_POLARIS_DIR/check_metastore_bootstrap.sh"
    rsync -a "$RELEASE_POLARIS_DIR/bootstrap_metastore_if_required.sh" \
        "$RUNTIME_POLARIS_DIR/bootstrap_metastore_if_required.sh"

    docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" \
        --profile bpi config --quiet
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
    python3 - \
        "$ENV_FILE" \
        "$SERVICE_IMAGE" \
        "$ADAPTER_IMAGE" \
        "$WMS_ADAPTER_IMAGE" \
        "$MATERIALIZER_IMAGE" \
        "$CATALOG_PUBLISHER_IMAGE" \
        "$EXPECTED_VERSION" <<'PY'
import os
import stat
import sys
import tempfile
from pathlib import Path

path = Path(sys.argv[1])
updates = {
    "BPI_SERVICE_IMAGE": sys.argv[2],
    "BPI_ADAPTER_IMAGE": sys.argv[3],
    "BPI_WMS_ADAPTER_IMAGE": sys.argv[4],
    "BPI_DATASET_MATERIALIZER_IMAGE": sys.argv[5],
    "BPI_DATASET_CATALOG_PUBLISHER_IMAGE": sys.argv[6],
    "BPI_EXPECTED_FLYWAY_VERSION": sys.argv[7],
    "BPI_DATASET_MATERIALIZER_ENABLED": "false",
    "BPI_DATASET_BUCKET_BOOTSTRAP_ENABLED": "false",
    "BPI_DATASET_SOURCE_READER_ENABLED": "false",
    "BPI_POLARIS_ENABLED": "false",
    "BPI_POLARIS_ALLOW_INSECURE_STORAGE_TYPES": "false",
    "BPI_POLARIS_IGNORE_SEVERE_READINESS_ISSUES": "false",
    "BPI_POLARIS_CATALOG_BOOTSTRAP_ENABLED": "false",
    "BPI_POLARIS_PUBLISHER_CREDENTIAL_ROTATION_ENABLED": "false",
    "BPI_ICEBERG_WAREHOUSE_BOOTSTRAP_ENABLED": "false",
    "BPI_DATASET_CATALOG_PUBLISHER_ENABLED": "false",
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
    export RELEASE_COMMIT EXPECTED_VERSION BEFORE_VERSION AFTER_VERSION DATABASE_MIGRATION_MODE
    export SERVICE_IMAGE ADAPTER_IMAGE WMS_ADAPTER_IMAGE MATERIALIZER_IMAGE CATALOG_PUBLISHER_IMAGE
    export BEFORE_SERVICE_IMAGE_ID BEFORE_ADAPTER_IMAGE_ID BEFORE_WMS_ADAPTER_IMAGE_ID
    export AFTER_SERVICE_IMAGE_ID AFTER_ADAPTER_IMAGE_ID AFTER_WMS_ADAPTER_IMAGE_ID
    export ROLLBACK_SERVICE_IMAGE ROLLBACK_ADAPTER_IMAGE ROLLBACK_WMS_ADAPTER_IMAGE
    export DATABASE_BACKUP ENV_BACKUP COMPOSE_BACKUP MINIO_CONFIG_BACKUP
    export POLARIS_CONFIG_BACKUP
    export UI_BACKUP UI_SHA256
    export MATERIALIZER_IMAGE_ID MATERIALIZER_IMAGE_USER MIGRATION_SET_SHA256
    export CATALOG_PUBLISHER_IMAGE_ID CATALOG_PUBLISHER_IMAGE_USER
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
        "migrationMode": value("DATABASE_MIGRATION_MODE"),
        "migrationApplied": value("DATABASE_MIGRATION_MODE") == "APPLY_EXPANSION",
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
        "wmsAdapter": {
            "beforeImageId": value("BEFORE_WMS_ADAPTER_IMAGE_ID"),
            "afterImageId": value("AFTER_WMS_ADAPTER_IMAGE_ID"),
            "releaseImage": value("WMS_ADAPTER_IMAGE"),
            "rollbackImage": value("ROLLBACK_WMS_ADAPTER_IMAGE"),
        },
        "datasetMaterializer": {
            "releaseImage": value("MATERIALIZER_IMAGE"),
            "releaseImageId": value("MATERIALIZER_IMAGE_ID"),
            "runtimeUser": value("MATERIALIZER_IMAGE_USER"),
            "enabledDuringUpgrade": False,
            "bucketBootstrapEnabledDuringUpgrade": False,
            "postUpgradeState": "STOPPED",
        },
        "datasetCatalogPublisher": {
            "releaseImage": value("CATALOG_PUBLISHER_IMAGE"),
            "releaseImageId": value("CATALOG_PUBLISHER_IMAGE_ID"),
            "runtimeUser": value("CATALOG_PUBLISHER_IMAGE_USER"),
            "enabledDuringUpgrade": False,
            "polarisEnabledDuringUpgrade": False,
            "postUpgradeState": "STOPPED",
        },
        "environmentBackup": value("ENV_BACKUP"),
        "composeBackup": value("COMPOSE_BACKUP"),
        "minioConfigBackup": value("MINIO_CONFIG_BACKUP"),
        "polarisConfigBackup": value("POLARIS_CONFIG_BACKUP"),
        "uiBackup": value("UI_BACKUP"),
        "uiIndexSha256": value("UI_SHA256"),
        "migrationSetSha256": value("MIGRATION_SET_SHA256"),
    },
    "safety": {
        "phase2IntegrationDefault": False,
        "wmsOutboxDefault": False,
        "wmsAdapterDefault": False,
        "datasetMaterializerDefault": False,
        "datasetBucketBootstrapDefault": False,
        "datasetCatalogPublisherDefault": False,
        "polarisDefault": False,
        "rollbackMethod": "Restore recorded images, UI, Compose, MinIO and Polaris configuration; stop optional WMS/materializer/catalog services when required; keep the expanded schema.",
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
WMS_ADAPTER_IMAGE=${BPI_INTEGRATED_WMS_ADAPTER_IMAGE:-ft-mes-bpi-wms-adapter:${release_suffix}-${short_commit}}
MATERIALIZER_IMAGE=${BPI_INTEGRATED_MATERIALIZER_IMAGE:-ft-mes-bpi-dataset-materializer:${release_suffix}-${short_commit}}
CATALOG_PUBLISHER_IMAGE=${BPI_INTEGRATED_CATALOG_PUBLISHER_IMAGE:-ft-mes-bpi-dataset-catalog-publisher:${release_suffix}-${short_commit}}
REPORT=${BPI_INTEGRATED_UPGRADE_REPORT:-$BACKUP_DIR/bpi-integrated-upgrade-${timestamp}.json}
COMPOSE_BACKUP="$BACKUP_DIR/docker-compose-before-v${EXPECTED_VERSION}-${timestamp}.yml"
MINIO_CONFIG_BACKUP="$BACKUP_DIR/bpi-minio-runtime-before-v${EXPECTED_VERSION}-${timestamp}.tar.gz"
POLARIS_CONFIG_BACKUP="$BACKUP_DIR/bpi-polaris-runtime-before-v${EXPECTED_VERSION}-${timestamp}.tar.gz"

DATABASE_NAME=$(env_value BPI_DATABASE_NAME ft_mes_bpi)
POSTGRES_USER=$(env_value POSTGRES_USER adp)
POSTGRES_DB=$(env_value POSTGRES_DB adp)
DATABASE_PASSWORD=$(required_secret BPI_DATABASE_PASSWORD)
MIGRATOR_PASSWORD=$(required_secret BPI_MIGRATOR_PASSWORD)
MATERIALIZER_DATABASE_PASSWORD=$(required_secret BPI_MATERIALIZER_DATABASE_PASSWORD)
CATALOG_PUBLISHER_DATABASE_PASSWORD=$(required_secret BPI_CATALOG_PUBLISHER_DATABASE_PASSWORD)
MINIO_ROOT_USER=$(required_secret MINIO_ROOT_USER)
MINIO_ROOT_PASSWORD=$(required_secret MINIO_ROOT_PASSWORD)
MATERIALIZER_MINIO_ACCESS_KEY=$(required_secret BPI_DATASET_MINIO_ACCESS_KEY)
MATERIALIZER_MINIO_SECRET_KEY=$(required_secret BPI_DATASET_MINIO_SECRET_KEY)
FLYWAY_IMAGE=$(env_value BPI_FLYWAY_IMAGE m.daocloud.io/docker.io/flyway/flyway:11-alpine)
SERVICE_MAVEN_IMAGE=${BPI_INTEGRATED_SERVICE_MAVEN_IMAGE:-$(env_value BPI_MAVEN_IMAGE m.daocloud.io/docker.io/library/maven:3.9.9-eclipse-temurin-17)}
SERVICE_JAVA_IMAGE=${BPI_INTEGRATED_SERVICE_JAVA_IMAGE:-$(env_value BPI_JAVA_IMAGE m.daocloud.io/docker.io/library/eclipse-temurin:17-jre-jammy)}
ADAPTER_MAVEN_IMAGE=${BPI_INTEGRATED_ADAPTER_MAVEN_IMAGE:-$(env_value BPI_ADAPTER_MAVEN_IMAGE m.daocloud.io/docker.io/library/maven:3.9.9-eclipse-temurin-8)}
ADAPTER_JAVA_IMAGE=${BPI_INTEGRATED_ADAPTER_JAVA_IMAGE:-$(env_value BPI_ADAPTER_JAVA_IMAGE m.daocloud.io/docker.io/library/eclipse-temurin:8-jre-jammy)}
WMS_ADAPTER_MAVEN_IMAGE=${BPI_INTEGRATED_WMS_ADAPTER_MAVEN_IMAGE:-$(env_value BPI_WMS_ADAPTER_MAVEN_IMAGE m.daocloud.io/docker.io/library/maven:3.9.9-eclipse-temurin-17)}
WMS_ADAPTER_JAVA_IMAGE=${BPI_INTEGRATED_WMS_ADAPTER_JAVA_IMAGE:-$(env_value BPI_WMS_ADAPTER_JAVA_IMAGE m.daocloud.io/docker.io/library/eclipse-temurin:17-jre-jammy)}
MATERIALIZER_PYTHON_IMAGE=${BPI_INTEGRATED_MATERIALIZER_PYTHON_IMAGE:-$(env_value BPI_DATASET_MATERIALIZER_PYTHON_IMAGE m.daocloud.io/docker.io/library/python:3.12.13-slim-bookworm)}
CATALOG_PUBLISHER_PYTHON_IMAGE=${BPI_INTEGRATED_CATALOG_PUBLISHER_PYTHON_IMAGE:-$(env_value BPI_DATASET_CATALOG_PUBLISHER_PYTHON_IMAGE m.daocloud.io/docker.io/library/python:3.12.13-slim-bookworm)}
if [ "$MATERIALIZER_DATABASE_PASSWORD" = "$DATABASE_PASSWORD" ] \
   || [ "$MATERIALIZER_DATABASE_PASSWORD" = "$MIGRATOR_PASSWORD" ] \
   || [ "$CATALOG_PUBLISHER_DATABASE_PASSWORD" = "$DATABASE_PASSWORD" ] \
   || [ "$CATALOG_PUBLISHER_DATABASE_PASSWORD" = "$MIGRATOR_PASSWORD" ] \
   || [ "$CATALOG_PUBLISHER_DATABASE_PASSWORD" = "$MATERIALIZER_DATABASE_PASSWORD" ]; then
    printf 'ERROR: BPI service, migrator and worker database credentials must be distinct\n' >&2
    exit 1
fi
if [ "$MATERIALIZER_MINIO_ACCESS_KEY" = "$MINIO_ROOT_USER" ] \
   || [ "$MATERIALIZER_MINIO_SECRET_KEY" = "$MINIO_ROOT_PASSWORD" ]; then
    printf 'ERROR: BPI dataset MinIO credentials must be distinct from root\n' >&2
    exit 1
fi
test "${#MINIO_ROOT_PASSWORD}" -ge 8 || {
    printf 'ERROR: MINIO_ROOT_PASSWORD must contain at least 8 characters\n' >&2
    exit 1
}
test "${#MATERIALIZER_MINIO_SECRET_KEY}" -ge 8 || {
    printf 'ERROR: BPI_DATASET_MINIO_SECRET_KEY must contain at least 8 characters\n' >&2
    exit 1
}
require_integer_range BPI_DATASET_MATERIALIZER_POLL_SECONDS 1 3600
require_integer_range BPI_DATASET_MATERIALIZER_CLAIM_TIMEOUT_SECONDS 30 86400
require_integer_range BPI_DATASET_MATERIALIZER_MAX_ATTEMPTS 1 20
require_integer_range BPI_DATASET_CATALOG_PUBLISHER_POLL_SECONDS 1 60
require_integer_range BPI_DATASET_CATALOG_PUBLISHER_CLAIM_TIMEOUT_SECONDS 30 86400
require_integer_range BPI_DATASET_CATALOG_PUBLISHER_MAX_ATTEMPTS 1 20
for disabled_key in \
    BPI_PHASE2_INTEGRATION_ENABLED \
    BPI_PHASE2_PROTOBUF_HTTP_INGRESS_ENABLED \
    BPI_PHASE2_KAFKA_ENABLED \
    BPI_WMS_OUTBOX_ENABLED \
    BPI_WMS_ADAPTER_ENABLED \
    BPI_DATASET_MATERIALIZER_ENABLED \
    BPI_DATASET_BUCKET_BOOTSTRAP_ENABLED \
    BPI_DATASET_SOURCE_READER_ENABLED \
    BPI_POLARIS_ENABLED \
    BPI_POLARIS_ALLOW_INSECURE_STORAGE_TYPES \
    BPI_POLARIS_IGNORE_SEVERE_READINESS_ISSUES \
    BPI_POLARIS_CATALOG_BOOTSTRAP_ENABLED \
    BPI_POLARIS_PUBLISHER_CREDENTIAL_ROTATION_ENABLED \
    BPI_ICEBERG_WAREHOUSE_BOOTSTRAP_ENABLED \
    BPI_DATASET_CATALOG_PUBLISHER_ENABLED \
    QCS_BPI_OUTBOX_ENABLED; do
    if [ "$(env_value "$disabled_key" false)" != "false" ]; then
        printf 'ERROR: %s must remain false during the integrated expand-only upgrade\n' \
            "$disabled_key" >&2
        exit 1
    fi
done

validate_release_deployment_manifests
POSTGRES_ID=$(compose ps -q postgres)
SERVICE_ID=$(compose ps -q bpi-service)
ADAPTER_ID=$(compose ps -q bpi-adapter)
WMS_ADAPTER_ID=$(compose ps -q bpi-wms-adapter)
MATERIALIZER_ID=$(compose ps -q bpi-dataset-materializer)
NGINX_ID=$(compose ps -q nginx)
if [ -z "$POSTGRES_ID" ] || [ -z "$SERVICE_ID" ] || [ -z "$ADAPTER_ID" ] || [ -z "$NGINX_ID" ]; then
    printf 'ERROR: postgres, bpi-service, bpi-adapter and nginx must all be running\n' >&2
    exit 1
fi

run_materializer_role_action() {
    action=$1
    docker exec -i \
        -e "POSTGRES_USER=$POSTGRES_USER" \
        -e "POSTGRES_DB=$POSTGRES_DB" \
        -e "BPI_DATABASE_NAME=$DATABASE_NAME" \
        -e "BPI_MATERIALIZER_DATABASE_PASSWORD=$MATERIALIZER_DATABASE_PASSWORD" \
        "$POSTGRES_ID" sh -s -- "$action" <"$MATERIALIZER_ROLE_SCRIPT"
}

run_catalog_publisher_role_action() {
    action=$1
    docker exec -i \
        -e "POSTGRES_USER=$POSTGRES_USER" \
        -e "POSTGRES_DB=$POSTGRES_DB" \
        -e "BPI_DATABASE_NAME=$DATABASE_NAME" \
        -e "BPI_CATALOG_PUBLISHER_DATABASE_PASSWORD=$CATALOG_PUBLISHER_DATABASE_PASSWORD" \
        "$POSTGRES_ID" sh -s -- "$action" <"$CATALOG_PUBLISHER_ROLE_SCRIPT"
}

verify_service_image_migrations() {
    temporary_directory=$(mktemp -d)
    temporary_container=$(docker create "$SERVICE_IMAGE")
    if ! docker cp \
        "$temporary_container:/opt/bpi/bpi-service.jar" \
        "$temporary_directory/bpi-service.jar" >/dev/null; then
        docker rm "$temporary_container" >/dev/null 2>&1 || true
        rm -f "$temporary_directory/bpi-service.jar"
        rmdir "$temporary_directory" 2>/dev/null || true
        return 1
    fi
    docker rm "$temporary_container" >/dev/null
    if ! python3 "$VERIFY_MIGRATIONS" \
        --jar "$temporary_directory/bpi-service.jar" \
        --migrations-dir "$MIGRATIONS" \
        --expected-version "$EXPECTED_VERSION" \
        --digest-only; then
        rm -f "$temporary_directory/bpi-service.jar"
        rmdir "$temporary_directory" 2>/dev/null || true
        return 1
    fi
    rm -f "$temporary_directory/bpi-service.jar"
    rmdir "$temporary_directory"
}

BEFORE_VERSION=$(query_version)
case "$BEFORE_VERSION" in
    ''|*[!0-9]*) printf 'ERROR: current Flyway version is not numeric: %s\n' "$BEFORE_VERSION" >&2; exit 1 ;;
esac
if [ "$BEFORE_VERSION" -gt "$EXPECTED_VERSION" ]; then
    printf 'ERROR: current Flyway version %s is above target %s\n' \
        "$BEFORE_VERSION" "$EXPECTED_VERSION" >&2
    exit 1
fi
DATABASE_MIGRATION_MODE=APPLY_EXPANSION
if [ "$BEFORE_VERSION" -eq "$EXPECTED_VERSION" ]; then
    if [ "$ALLOW_ALREADY_MIGRATED" != true ] \
       || [ "$ALREADY_MIGRATED_CONFIRM" != "REDEPLOY_APPLICATIONS_ON_EXISTING_BPI_SCHEMA" ]; then
        printf '%s\n' \
            "ERROR: target schema is already at Flyway ${EXPECTED_VERSION}; set BPI_INTEGRATED_ALLOW_ALREADY_MIGRATED=true and BPI_INTEGRATED_ALREADY_MIGRATED_CONFIRM=REDEPLOY_APPLICATIONS_ON_EXISTING_BPI_SCHEMA to validate it and redeploy applications" >&2
        exit 1
    fi
    DATABASE_MIGRATION_MODE=VALIDATE_EXISTING_SCHEMA
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
AFTER_WMS_ADAPTER_IMAGE_ID=
MATERIALIZER_IMAGE_ID=
MATERIALIZER_IMAGE_USER=
CATALOG_PUBLISHER_IMAGE_ID=
CATALOG_PUBLISHER_IMAGE_USER=
MIGRATION_SET_SHA256=
DATABASE_BACKUP=
ENV_BACKUP=
UI_BACKUP=
UI_SHA256=
BEFORE_SERVICE_IMAGE_ID=$(docker inspect --format '{{.Image}}' "$SERVICE_ID")
BEFORE_ADAPTER_IMAGE_ID=$(docker inspect --format '{{.Image}}' "$ADAPTER_ID")
BEFORE_WMS_ADAPTER_IMAGE_ID=
if [ -n "$WMS_ADAPTER_ID" ]; then
    BEFORE_WMS_ADAPTER_IMAGE_ID=$(docker inspect --format '{{.Image}}' "$WMS_ADAPTER_ID")
fi
ROLLBACK_SERVICE_IMAGE="ft-mes-bpi-service:rollback-v${BEFORE_VERSION}-${release_suffix}"
ROLLBACK_ADAPTER_IMAGE="ft-mes-bpi-adapter:rollback-v${BEFORE_VERSION}-${release_suffix}"
ROLLBACK_WMS_ADAPTER_IMAGE=
if [ -n "$BEFORE_WMS_ADAPTER_IMAGE_ID" ]; then
    ROLLBACK_WMS_ADAPTER_IMAGE="ft-mes-bpi-wms-adapter:rollback-v${BEFORE_VERSION}-${release_suffix}"
fi
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
    --build-arg "MAVEN_IMAGE=$SERVICE_MAVEN_IMAGE" \
    --build-arg "JAVA_IMAGE=$SERVICE_JAVA_IMAGE" \
    --label "org.opencontainers.image.revision=$RELEASE_COMMIT" \
    -f "$RELEASE_ROOT/services/bpi-service/Dockerfile" \
    -t "$SERVICE_IMAGE" "$RELEASE_ROOT"
docker build \
    --build-arg "MAVEN_IMAGE=$ADAPTER_MAVEN_IMAGE" \
    --build-arg "JAVA_IMAGE=$ADAPTER_JAVA_IMAGE" \
    --label "org.opencontainers.image.revision=$RELEASE_COMMIT" \
    -f "$RELEASE_ROOT/backend/source-modules/batch-intelligence-adapter/Dockerfile" \
    -t "$ADAPTER_IMAGE" "$RELEASE_ROOT"
docker build \
    --build-arg "MAVEN_IMAGE=$WMS_ADAPTER_MAVEN_IMAGE" \
    --build-arg "JAVA_IMAGE=$WMS_ADAPTER_JAVA_IMAGE" \
    --label "org.opencontainers.image.revision=$RELEASE_COMMIT" \
    -f "$RELEASE_ROOT/services/bpi-service/wms-adapter/Dockerfile" \
    -t "$WMS_ADAPTER_IMAGE" "$RELEASE_ROOT"
docker build \
    --build-arg "PYTHON_IMAGE=$MATERIALIZER_PYTHON_IMAGE" \
    --label "org.opencontainers.image.revision=$RELEASE_COMMIT" \
    -f "$RELEASE_ROOT/services/bpi-dataset-materializer/Dockerfile" \
    -t "$MATERIALIZER_IMAGE" "$RELEASE_ROOT"
docker build \
    --build-arg "PYTHON_IMAGE=$CATALOG_PUBLISHER_PYTHON_IMAGE" \
    --label "org.opencontainers.image.revision=$RELEASE_COMMIT" \
    -f "$RELEASE_ROOT/services/bpi-dataset-catalog-publisher/Dockerfile" \
    -t "$CATALOG_PUBLISHER_IMAGE" "$RELEASE_ROOT"

MIGRATION_SET_SHA256=$(verify_service_image_migrations)
MATERIALIZER_IMAGE_ID=$(docker image inspect --format '{{.Id}}' "$MATERIALIZER_IMAGE")
MATERIALIZER_IMAGE_USER=$(docker image inspect --format '{{.Config.User}}' "$MATERIALIZER_IMAGE")
if [ "$MATERIALIZER_IMAGE_USER" != "10001:10001" ]; then
    printf 'ERROR: BPI dataset materializer must run as 10001:10001, found %s\n' \
        "$MATERIALIZER_IMAGE_USER" >&2
    exit 1
fi
CATALOG_PUBLISHER_IMAGE_ID=$(docker image inspect --format '{{.Id}}' "$CATALOG_PUBLISHER_IMAGE")
CATALOG_PUBLISHER_IMAGE_USER=$(docker image inspect --format '{{.Config.User}}' "$CATALOG_PUBLISHER_IMAGE")
if [ "$CATALOG_PUBLISHER_IMAGE_USER" != "10002:10002" ]; then
    printf 'ERROR: BPI catalog publisher must run as 10002:10002, found %s\n' \
        "$CATALOG_PUBLISHER_IMAGE_USER" >&2
    exit 1
fi

if [ -n "$MATERIALIZER_ID" ]; then
    compose_expand stop bpi-dataset-materializer
fi

phase=BACKUP
mkdir -p "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR"
DATABASE_BACKUP="$BACKUP_DIR/ft_mes_bpi-before-v${EXPECTED_VERSION}-${timestamp}.dump"
ENV_BACKUP="$BACKUP_DIR/adp-compose-before-v${EXPECTED_VERSION}-${timestamp}.env"
UI_BACKUP="$BACKUP_DIR/bpi-ui-before-v${EXPECTED_VERSION}-${timestamp}.tar.gz"
docker image tag "$BEFORE_SERVICE_IMAGE_ID" "$ROLLBACK_SERVICE_IMAGE"
docker image tag "$BEFORE_ADAPTER_IMAGE_ID" "$ROLLBACK_ADAPTER_IMAGE"
if [ -n "$BEFORE_WMS_ADAPTER_IMAGE_ID" ]; then
    docker image tag "$BEFORE_WMS_ADAPTER_IMAGE_ID" "$ROLLBACK_WMS_ADAPTER_IMAGE"
fi
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
stage_runtime_deployment_manifests
write_report PREPARED BACKUP_COMPLETE

phase=MIGRATION
run_materializer_role_action provision
run_catalog_publisher_role_action provision
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
run_materializer_role_action grant
run_materializer_role_action verify
run_catalog_publisher_role_action grant
run_catalog_publisher_role_action verify
if [ "$DATABASE_MIGRATION_MODE" = "APPLY_EXPANSION" ]; then
    write_report IN_PROGRESS MIGRATION_APPLIED
else
    write_report IN_PROGRESS EXISTING_MIGRATION_VALIDATED
fi

phase=SERVICE_RECREATE
replace_env_images
compose_expand up -d --no-deps --force-recreate bpi-service
wait_for_health bpi-service
compose_expand up -d --no-deps --force-recreate bpi-adapter
wait_for_health bpi-adapter
compose_expand up -d --no-deps --force-recreate bpi-wms-adapter
wait_for_health bpi-wms-adapter
SERVICE_ID=$(compose ps -q bpi-service)
ADAPTER_ID=$(compose ps -q bpi-adapter)
WMS_ADAPTER_ID=$(compose ps -q bpi-wms-adapter)
AFTER_SERVICE_IMAGE_ID=$(docker inspect --format '{{.Image}}' "$SERVICE_ID")
AFTER_ADAPTER_IMAGE_ID=$(docker inspect --format '{{.Image}}' "$ADAPTER_ID")
AFTER_WMS_ADAPTER_IMAGE_ID=$(docker inspect --format '{{.Image}}' "$WMS_ADAPTER_ID")
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
wms_adapter_health=$(curl -fsS --connect-timeout 5 --max-time 20 \
    "http://127.0.0.1:$(env_value BPI_WMS_ADAPTER_HTTP_PORT 19092)/actuator/health")
printf '%s' "$wms_adapter_health" | grep -q '"status":"UP"'
flag_state=$(docker exec "$POSTGRES_ID" psql -X -At -U "$POSTGRES_USER" -d "$DATABASE_NAME" \
    -c "SELECT string_agg(flag_key || '=' || enabled::text, ',' ORDER BY flag_key) FROM bpi.bpi_feature_flags WHERE tenant_id = '*' AND scope_type = 'GLOBAL' AND flag_key IN ('bpi.auto-confirm','bpi.qcs-link','bpi.shadow-only','bpi.wms-link')")
printf '%s' "$flag_state" | grep -q 'bpi.auto-confirm=false'
printf '%s' "$flag_state" | grep -q 'bpi.qcs-link=false'
printf '%s' "$flag_state" | grep -q 'bpi.shadow-only=true'
printf '%s' "$flag_state" | grep -q 'bpi.wms-link=false'
if [ -n "$(compose ps -q bpi-dataset-materializer)" ]; then
    printf 'ERROR: BPI dataset materializer must remain stopped after expand-only upgrade\n' >&2
    exit 1
fi
for disabled_service in bpi-dataset-catalog-publisher bpi-polaris \
    bpi-polaris-postgres bpi-polaris-catalog-bootstrap; do
    if [ -n "$(docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" \
        --profile bpi-catalog ps -q "$disabled_service")" ]; then
        printf 'ERROR: %s must remain stopped after expand-only upgrade\n' \
            "$disabled_service" >&2
        exit 1
    fi
done

write_report PASS COMPLETE
trap - EXIT HUP INT TERM
printf 'Integrated BPI expand-only upgrade: PASS (Flyway %s, mode %s, release %s)\n' \
    "$AFTER_VERSION" "$DATABASE_MIGRATION_MODE" "$RELEASE_COMMIT"
printf 'Service image: %s\nAdapter image: %s\nWMS adapter image: %s\nReport: %s\n' \
    "$SERVICE_IMAGE" "$ADAPTER_IMAGE" "$WMS_ADAPTER_IMAGE" "$REPORT"
printf 'Dataset materializer image (built, not started): %s\n' "$MATERIALIZER_IMAGE"
printf 'Dataset catalog publisher image (built, not started): %s\n' \
    "$CATALOG_PUBLISHER_IMAGE"
