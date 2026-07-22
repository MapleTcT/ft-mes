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

for key in BPI_DATASET_BUCKET_BOOTSTRAP_ENABLED BPI_DATASET_MATERIALIZER_ENABLED; do
    require_boolean "$key"
done
require_integer_range BPI_DATASET_MATERIALIZER_POLL_SECONDS 1 3600
require_integer_range BPI_DATASET_MATERIALIZER_CLAIM_TIMEOUT_SECONDS 30 86400
require_integer_range BPI_DATASET_MATERIALIZER_MAX_ATTEMPTS 1 20

database_password=$(env_value BPI_DATABASE_PASSWORD '')
migrator_password=$(env_value BPI_MIGRATOR_PASSWORD '')
materializer_password=$(env_value BPI_MATERIALIZER_DATABASE_PASSWORD '')
if [ "$materializer_password" = "$database_password" ] \
   || [ "$materializer_password" = "$migrator_password" ]; then
    printf 'ERROR: BPI materializer database credentials must be distinct\n' >&2
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

python3 "$VERIFY_MIGRATIONS" \
    --jar "$JAR" \
    --migrations-dir "$MIGRATIONS" \
    --expected-version "$expected_flyway"

docker compose --env-file "$ENV_FILE" -f "$DEPLOY_DIR/docker-compose.yml" config >/dev/null
printf 'BPI runtime preflight: PASS\n'
