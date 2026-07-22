#!/bin/sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../../.." && pwd)
DEPLOY_DIR="$ROOT_DIR/deploy/bpi-runtime"
ENV_FILE=${1:-"$DEPLOY_DIR/.env"}
JAR="$ROOT_DIR/services/bpi-service/app/target/bpi-service-0.1.0-SNAPSHOT-exec.jar"
ADAPTER_JAR="$ROOT_DIR/backend/source-modules/batch-intelligence-adapter/target/batch-intelligence-adapter-0.1.0-SNAPSHOT.jar"
WEB_INDEX="$ROOT_DIR/frontend/apps/bpi/dist/index.html"
MIGRATIONS="$ROOT_DIR/services/bpi-service/app/src/main/resources/db/migration"
VERIFY_MIGRATIONS="$ROOT_DIR/scripts/verify-bpi-release-migrations.py"

env_value() {
    key=$1
    fallback=$2
    value=$(sed -n "s/^${key}=//p" "$ENV_FILE" | tail -1)
    printf '%s' "${value:-$fallback}"
}

require_value() {
    key=$1
    value=$(env_value "$key" '')
    test -n "$value" || {
        printf 'ERROR: %s must be set in %s\n' "$key" "$ENV_FILE" >&2
        exit 1
    }
}

require_secret() {
    key=$1
    require_value "$key"
    value=$(env_value "$key" '')
    case "$value" in
        *change-me*|*dev-only*|*_DISABLED_*)
            printf 'ERROR: %s still contains a placeholder value\n' "$key" >&2
            exit 1
            ;;
    esac
}

require_boolean() {
    key=$1
    value=$(env_value "$key" false)
    case "$value" in
        true|false) ;;
        *) printf 'ERROR: %s must be true or false\n' "$key" >&2; exit 1 ;;
    esac
}

require_integer_range() {
    key=$1
    minimum=$2
    maximum=$3
    value=$(env_value "$key" '')
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

test -f "$ENV_FILE" || {
    printf 'ERROR: missing BPI runtime env file: %s\n' "$ENV_FILE" >&2
    exit 1
}
test -f "$JAR" || {
    printf 'ERROR: package the BPI service first: make bpi-service-package\n' >&2
    exit 1
}
test -f "$ADAPTER_JAR" || {
    printf 'ERROR: package the Java 8 BPI adapter first: make bpi-adapter-package\n' >&2
    exit 1
}
test -f "$WEB_INDEX" || {
    printf 'ERROR: build the BPI console first: make bpi-ui-build\n' >&2
    exit 1
}
test -f "$VERIFY_MIGRATIONS" || {
    printf 'ERROR: BPI release migration verifier is missing: %s\n' \
        "$VERIFY_MIGRATIONS" >&2
    exit 1
}

for command_name in docker python3 sed tail; do
    command -v "$command_name" >/dev/null 2>&1 || {
        printf 'ERROR: required command is unavailable: %s\n' "$command_name" >&2
        exit 1
    }
done

for key in \
    POSTGRES_PASSWORD \
    BPI_DATABASE_PASSWORD \
    BPI_MIGRATOR_PASSWORD \
    BPI_MATERIALIZER_DATABASE_PASSWORD \
    BPI_CATALOG_PUBLISHER_DATABASE_PASSWORD \
    BPI_INTERNAL_JWT_SECRET \
    MINIO_ROOT_USER \
    MINIO_ROOT_PASSWORD \
    BPI_DATASET_MINIO_ACCESS_KEY \
    BPI_DATASET_MINIO_SECRET_KEY; do
    require_secret "$key"
done

for key in ADP_RUNTIME_NETWORK_NAME BPI_ADAPTER_KEYCLOAK_JWK_SET_URI BPI_ADAPTER_KEYCLOAK_ISSUER BPI_ADAPTER_LEGACY_GATEWAY_BASE_URL BPI_ADAPTER_ROLE_RULES BPI_ADAPTER_SUBJECT_SCOPE_RULES; do
    require_value "$key"
done

internal_secret=$(env_value BPI_INTERNAL_JWT_SECRET '')
test "${#internal_secret}" -ge 32 || {
    printf 'ERROR: BPI_INTERNAL_JWT_SECRET must contain at least 32 characters\n' >&2
    exit 1
}

bootstrap=$(env_value BPI_KAFKA_BOOTSTRAP_SERVERS '')
test -n "$bootstrap" || {
    printf 'ERROR: BPI_KAFKA_BOOTSTRAP_SERVERS must be set\n' >&2
    exit 1
}

expected_flyway=$(env_value BPI_EXPECTED_FLYWAY_VERSION '')
case "$expected_flyway" in
    ''|0|0[0-9]*|*[!0-9]*)
        printf 'ERROR: BPI_EXPECTED_FLYWAY_VERSION must be a positive integer\n' >&2
        exit 1
        ;;
esac

for key in \
    BPI_DATASET_BUCKET_BOOTSTRAP_ENABLED \
    BPI_DATASET_MATERIALIZER_ENABLED \
    BPI_DATASET_SOURCE_READER_ENABLED \
    BPI_POLARIS_ENABLED \
    BPI_POLARIS_ALLOW_INSECURE_STORAGE_TYPES \
    BPI_POLARIS_IGNORE_SEVERE_READINESS_ISSUES \
    BPI_POLARIS_CATALOG_BOOTSTRAP_ENABLED \
    BPI_POLARIS_PUBLISHER_CREDENTIAL_ROTATION_ENABLED \
    BPI_ICEBERG_WAREHOUSE_BOOTSTRAP_ENABLED \
    BPI_DATASET_CATALOG_PUBLISHER_ENABLED; do
    require_boolean "$key"
done
require_integer_range BPI_DATASET_MATERIALIZER_POLL_SECONDS 1 3600
require_integer_range BPI_DATASET_MATERIALIZER_CLAIM_TIMEOUT_SECONDS 30 86400
require_integer_range BPI_DATASET_MATERIALIZER_MAX_ATTEMPTS 1 20
require_integer_range BPI_DATASET_CATALOG_PUBLISHER_POLL_SECONDS 1 60
require_integer_range BPI_DATASET_CATALOG_PUBLISHER_CLAIM_TIMEOUT_SECONDS 30 86400
require_integer_range BPI_DATASET_CATALOG_PUBLISHER_MAX_ATTEMPTS 1 20

database_password=$(env_value BPI_DATABASE_PASSWORD '')
migrator_password=$(env_value BPI_MIGRATOR_PASSWORD '')
materializer_password=$(env_value BPI_MATERIALIZER_DATABASE_PASSWORD '')
catalog_publisher_password=$(env_value BPI_CATALOG_PUBLISHER_DATABASE_PASSWORD '')
if [ "$materializer_password" = "$database_password" ] \
   || [ "$materializer_password" = "$migrator_password" ] \
   || [ "$catalog_publisher_password" = "$database_password" ] \
   || [ "$catalog_publisher_password" = "$migrator_password" ] \
   || [ "$catalog_publisher_password" = "$materializer_password" ]; then
    printf 'ERROR: BPI service, migrator and worker database credentials must be distinct\n' >&2
    exit 1
fi

minio_root_user=$(env_value MINIO_ROOT_USER '')
minio_root_password=$(env_value MINIO_ROOT_PASSWORD '')
minio_access_key=$(env_value BPI_DATASET_MINIO_ACCESS_KEY '')
minio_secret_key=$(env_value BPI_DATASET_MINIO_SECRET_KEY '')
if [ "$minio_access_key" = "$minio_root_user" ] \
   || [ "$minio_secret_key" = "$minio_root_password" ]; then
    printf 'ERROR: BPI dataset MinIO credentials must be distinct from root\n' >&2
    exit 1
fi
test "${#minio_root_password}" -ge 8 || {
    printf 'ERROR: MINIO_ROOT_PASSWORD must contain at least 8 characters\n' >&2
    exit 1
}
test "${#minio_secret_key}" -ge 8 || {
    printf 'ERROR: BPI_DATASET_MINIO_SECRET_KEY must contain at least 8 characters\n' >&2
    exit 1
}

source_reader_enabled=$(env_value BPI_DATASET_SOURCE_READER_ENABLED false)
polaris_enabled=$(env_value BPI_POLARIS_ENABLED false)
allow_insecure_storage=$(env_value BPI_POLARIS_ALLOW_INSECURE_STORAGE_TYPES false)
ignore_severe_readiness=$(env_value BPI_POLARIS_IGNORE_SEVERE_READINESS_ISSUES false)
catalog_bootstrap_enabled=$(env_value BPI_POLARIS_CATALOG_BOOTSTRAP_ENABLED false)
warehouse_bootstrap_enabled=$(env_value BPI_ICEBERG_WAREHOUSE_BOOTSTRAP_ENABLED false)
publisher_enabled=$(env_value BPI_DATASET_CATALOG_PUBLISHER_ENABLED false)
source_reader_access=''
source_reader_secret=''
if [ "$source_reader_enabled" = true ]; then
    require_secret BPI_DATASET_SOURCE_READER_ACCESS_KEY
    require_secret BPI_DATASET_SOURCE_READER_SECRET_KEY
    source_reader_access=$(env_value BPI_DATASET_SOURCE_READER_ACCESS_KEY '')
    source_reader_secret=$(env_value BPI_DATASET_SOURCE_READER_SECRET_KEY '')
    if [ "$source_reader_access" = "$minio_root_user" ] \
       || [ "$source_reader_access" = "$minio_access_key" ] \
       || [ "$source_reader_secret" = "$minio_root_password" ] \
       || [ "$source_reader_secret" = "$minio_secret_key" ]; then
        printf 'ERROR: BPI catalog source-reader MinIO credentials must be distinct\n' >&2
        exit 1
    fi
fi
if [ "$polaris_enabled" = true ] || [ "$catalog_bootstrap_enabled" = true ] \
   || [ "$warehouse_bootstrap_enabled" = true ] || [ "$publisher_enabled" = true ]; then
    for key in \
        BPI_POLARIS_DATABASE_PASSWORD \
        BPI_POLARIS_BOOTSTRAP_CLIENT_ID \
        BPI_POLARIS_BOOTSTRAP_CLIENT_SECRET \
        BPI_POLARIS_TOKEN_BROKER_SECRET \
        BPI_ICEBERG_MINIO_ACCESS_KEY \
        BPI_ICEBERG_MINIO_SECRET_KEY; do
        require_secret "$key"
    done
    require_value BPI_POLARIS_REALM
    require_value BPI_ICEBERG_WAREHOUSE_BUCKET
    require_value BPI_ICEBERG_S3_ENDPOINT_EXTERNAL
    token_broker_secret=$(env_value BPI_POLARIS_TOKEN_BROKER_SECRET '')
    test "${#token_broker_secret}" -ge 32 || {
        printf 'ERROR: BPI_POLARIS_TOKEN_BROKER_SECRET must contain at least 32 characters\n' >&2
        exit 1
    }
    warehouse_access=$(env_value BPI_ICEBERG_MINIO_ACCESS_KEY '')
    warehouse_secret=$(env_value BPI_ICEBERG_MINIO_SECRET_KEY '')
    if [ "$warehouse_access" = "$minio_root_user" ] \
       || [ "$warehouse_access" = "$minio_access_key" ] \
       || { [ -n "$source_reader_access" ] && [ "$warehouse_access" = "$source_reader_access" ]; } \
       || [ "$warehouse_secret" = "$minio_root_password" ] \
       || [ "$warehouse_secret" = "$minio_secret_key" ] \
       || { [ -n "$source_reader_secret" ] && [ "$warehouse_secret" = "$source_reader_secret" ]; }; then
        printf 'ERROR: BPI Iceberg warehouse MinIO credentials must be distinct\n' >&2
        exit 1
    fi
    test "${#warehouse_secret}" -ge 8 || {
        printf 'ERROR: BPI_ICEBERG_MINIO_SECRET_KEY must contain at least 8 characters\n' >&2
        exit 1
    }
    if [ "$allow_insecure_storage" != true ]; then
        printf 'ERROR: internal HTTP MinIO requires BPI_POLARIS_ALLOW_INSECURE_STORAGE_TYPES=true\n' >&2
        exit 1
    fi
    if [ "$ignore_severe_readiness" != true ]; then
        printf 'ERROR: insecure test storage requires explicit BPI_POLARIS_IGNORE_SEVERE_READINESS_ISSUES=true\n' >&2
        exit 1
    fi
fi
if [ "$catalog_bootstrap_enabled" = true ] && [ "$polaris_enabled" != true ]; then
    printf 'ERROR: catalog bootstrap requires BPI_POLARIS_ENABLED=true\n' >&2
    exit 1
fi
if [ "$catalog_bootstrap_enabled" = true ] \
   && [ "$warehouse_bootstrap_enabled" != true ]; then
    printf 'ERROR: catalog bootstrap requires BPI_ICEBERG_WAREHOUSE_BOOTSTRAP_ENABLED=true\n' >&2
    exit 1
fi
if [ "$publisher_enabled" = true ] \
   && { [ "$polaris_enabled" != true ] \
        || [ "$catalog_bootstrap_enabled" != true ] \
        || [ "$warehouse_bootstrap_enabled" != true ] \
        || [ "$source_reader_enabled" != true ]; }; then
    printf 'ERROR: catalog publisher requires Polaris, warehouse/catalog bootstrap and source reader\n' >&2
    exit 1
fi

python3 "$VERIFY_MIGRATIONS" \
    --jar "$JAR" \
    --migrations-dir "$MIGRATIONS" \
    --expected-version "$expected_flyway"

docker compose --env-file "$ENV_FILE" -f "$DEPLOY_DIR/docker-compose.yml" config >/dev/null
printf 'BPI runtime preflight: PASS\n'
