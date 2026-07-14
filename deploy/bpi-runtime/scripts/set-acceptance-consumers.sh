#!/bin/sh
set -eu

if [ "$#" -lt 1 ] || { [ "$1" != enable ] && [ "$1" != disable ]; }; then
  echo "usage: $0 enable TENANT_ID PLANT_ID LINE_ID | disable" >&2
  exit 2
fi

mode=$1
if [ "$mode" = enable ] && [ "$#" -ne 4 ]; then
  echo "enable requires exact tenant, plant and line IDs" >&2
  exit 2
fi
if [ "$mode" = disable ] && [ "$#" -ne 1 ]; then
  echo "disable does not accept a scope" >&2
  exit 2
fi

BPI_RUNTIME_ROOT=${BPI_RUNTIME_ROOT:-/home/v6/ft-mes-bpi-runtime}
DEPLOY_DIR="$BPI_RUNTIME_ROOT/deploy/bpi-runtime"
ENV_FILE="$DEPLOY_DIR/.env"
COMPOSE_FILE="$DEPLOY_DIR/docker-compose.yml"

if [ ! -f "$ENV_FILE" ] || [ ! -f "$COMPOSE_FILE" ]; then
  echo "BPI runtime deployment is incomplete under $DEPLOY_DIR" >&2
  exit 1
fi

upsert_env() {
  key=$1
  value=$2
  tmp=$(mktemp)
  awk -v key="$key" -v value="$value" '
    BEGIN { found = 0 }
    index($0, key "=") == 1 { print key "=" value; found = 1; next }
    { print }
    END { if (!found) print key "=" value }
  ' "$ENV_FILE" > "$tmp"
  chmod --reference="$ENV_FILE" "$tmp"
  mv "$tmp" "$ENV_FILE"
}

if [ "$mode" = enable ]; then
  tenant_id=$2
  plant_id=$3
  line_id=$4
  for scope_value in "$tenant_id" "$plant_id" "$line_id"; do
    case "$scope_value" in
      ""|*[!A-Za-z0-9._-]*) echo "scope contains unsupported characters" >&2; exit 2 ;;
    esac
  done
  enabled=true
else
  tenant_id=_DENY_ALL_
  plant_id=_DENY_ALL_
  line_id=_DENY_ALL_
  enabled=false
fi

upsert_env BPI_CANDIDATE_KAFKA_ENABLED "$enabled"
upsert_env BPI_CANDIDATE_KAFKA_ALLOWED_TENANT_IDS "$tenant_id"
upsert_env BPI_CANDIDATE_KAFKA_ALLOWED_PLANT_IDS "$plant_id"
upsert_env BPI_CANDIDATE_KAFKA_ALLOWED_LINE_IDS "$line_id"
upsert_env BPI_RULE_PUBLICATION_OUTBOX_ENABLED "$enabled"
upsert_env BPI_RULE_APPLICATION_KAFKA_ENABLED "$enabled"
upsert_env BPI_RULE_APPLICATION_KAFKA_ALLOWED_TENANT_IDS "$tenant_id"
upsert_env BPI_RULE_APPLICATION_KAFKA_ALLOWED_PLANT_IDS "$plant_id"
upsert_env BPI_RULE_APPLICATION_KAFKA_ALLOWED_LINE_IDS "$line_id"

cd "$DEPLOY_DIR"
docker compose --env-file .env -f docker-compose.yml config \
  > /tmp/ft-mes-bpi-runtime-compose-resolved.yml
docker compose --env-file .env -f docker-compose.yml \
  up -d --no-deps --force-recreate bpi-service

container_id=$(docker compose --env-file .env -f docker-compose.yml ps -q bpi-service)
if [ -z "$container_id" ]; then
  echo "BPI service container was not created" >&2
  exit 1
fi

health=starting
attempt=0
while [ "$attempt" -lt 36 ]; do
  health=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' \
    "$container_id")
  [ "$health" = healthy ] && break
  attempt=$((attempt + 1))
  sleep 5
done

if [ "$health" != healthy ]; then
  docker logs --tail 100 "$container_id" >&2
  echo "BPI service did not become healthy" >&2
  exit 1
fi

printf 'BPI_SERVICE=running|healthy\n'
printf 'ACCEPTANCE_CONSUMERS=%s\n' "$enabled"
printf 'ALLOWLIST=%s|%s|%s\n' "$tenant_id" "$plant_id" "$line_id"
