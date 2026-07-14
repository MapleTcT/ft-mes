#!/bin/sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../../.." && pwd)
DEPLOY_DIR="$ROOT_DIR/deploy/bpi-runtime"
ENV_FILE=${1:-"$DEPLOY_DIR/.env"}
JAR="$ROOT_DIR/services/bpi-service/app/target/bpi-service-0.1.0-SNAPSHOT-exec.jar"
ADAPTER_JAR="$ROOT_DIR/backend/source-modules/batch-intelligence-adapter/target/batch-intelligence-adapter-0.1.0-SNAPSHOT.jar"
WEB_INDEX="$ROOT_DIR/frontend/apps/bpi/dist/index.html"

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

for key in POSTGRES_PASSWORD BPI_DATABASE_PASSWORD BPI_MIGRATOR_PASSWORD BPI_INTERNAL_JWT_SECRET; do
    value=$(sed -n "s/^${key}=//p" "$ENV_FILE" | tail -1)
    test -n "$value" || {
        printf 'ERROR: %s must be set in %s\n' "$key" "$ENV_FILE" >&2
        exit 1
    }
    case "$value" in
        *change-me*|*dev-only*)
            printf 'ERROR: %s still contains a placeholder value\n' "$key" >&2
            exit 1
            ;;
    esac
done

for key in ADP_RUNTIME_NETWORK_NAME BPI_ADAPTER_KEYCLOAK_JWK_SET_URI BPI_ADAPTER_KEYCLOAK_ISSUER BPI_ADAPTER_LEGACY_GATEWAY_BASE_URL BPI_ADAPTER_ROLE_RULES BPI_ADAPTER_SUBJECT_SCOPE_RULES; do
    value=$(sed -n "s/^${key}=//p" "$ENV_FILE" | tail -1)
    test -n "$value" || {
        printf 'ERROR: %s must be set in %s\n' "$key" "$ENV_FILE" >&2
        exit 1
    }
done

internal_secret=$(sed -n 's/^BPI_INTERNAL_JWT_SECRET=//p' "$ENV_FILE" | tail -1)
test "${#internal_secret}" -ge 32 || {
    printf 'ERROR: BPI_INTERNAL_JWT_SECRET must contain at least 32 characters\n' >&2
    exit 1
}

bootstrap=$(sed -n 's/^BPI_KAFKA_BOOTSTRAP_SERVERS=//p' "$ENV_FILE" | tail -1)
test -n "$bootstrap" || {
    printf 'ERROR: BPI_KAFKA_BOOTSTRAP_SERVERS must be set\n' >&2
    exit 1
}

docker compose --env-file "$ENV_FILE" -f "$DEPLOY_DIR/docker-compose.yml" config >/dev/null
printf 'BPI runtime preflight: PASS\n'
