#!/bin/sh
set -eu

MODULE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
ROOT_DIR=$(CDPATH= cd -- "$MODULE_DIR/../../.." && pwd)
CONTAINER="ft-mes-context-outbox-test-$$"
POSTGRES_IMAGE=${POSTGRES_IMAGE:-m.daocloud.io/docker.io/library/postgres:15-alpine}

cleanup() {
    docker stop "$CONTAINER" >/dev/null 2>&1 || true
}
trap cleanup EXIT HUP INT TERM

docker run --rm -d --name "$CONTAINER" \
    -e POSTGRES_DB=adp_test \
    -e POSTGRES_USER=adp_test \
    -e POSTGRES_PASSWORD=adp_test \
    "$POSTGRES_IMAGE" >/dev/null

attempt=0
until [ "$(docker exec "$CONTAINER" psql -Atq -U adp_test -d adp_test -c 'SELECT 1' 2>/dev/null || true)" = "1" ]; do
    attempt=$((attempt + 1))
    if [ "$attempt" -ge 30 ]; then
        printf '%s\n' 'PostgreSQL test container did not become ready' >&2
        exit 1
    fi
    sleep 1
done

docker exec -i "$CONTAINER" psql -v ON_ERROR_STOP=1 -U adp_test -d adp_test \
    < "$MODULE_DIR/src/test/resources/production-context-outbox-test-schema.sql"
docker exec -i "$CONTAINER" psql -v ON_ERROR_STOP=1 -U adp_test -d adp_test \
    < "$ROOT_DIR/deploy/docker/postgres/init/176-wom-bpi-production-context-outbox.sql"
docker exec -i "$CONTAINER" psql -v ON_ERROR_STOP=1 -U adp_test -d adp_test \
    < "$ROOT_DIR/deploy/docker/postgres/init/177-wom-bpi-context-revision-clock-floor.sql"
docker exec -i "$CONTAINER" psql -v ON_ERROR_STOP=1 -U adp_test -d adp_test \
    < "$MODULE_DIR/src/test/resources/production-context-outbox-postgres-acceptance.sql"
