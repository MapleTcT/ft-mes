#!/bin/sh
set -eu

if [ "$#" -ne 1 ] || { [ "$1" != true ] && [ "$1" != false ]; }; then
  echo "usage: $0 true|false" >&2
  exit 2
fi

desired=$1
BPI_STREAMING_ROOT=${BPI_STREAMING_ROOT:-/home/v6/ft-mes-bpi-streaming}
ADP_POSTGRES_CONTAINER=${ADP_POSTGRES_CONTAINER:-adp-mes-newbase-postgres-1}
ADP_DATABASE_NAME=${ADP_DATABASE_NAME:-adp}
DEPLOY_DIR="$BPI_STREAMING_ROOT/deploy/bpi-streaming"
ENV_FILE="$DEPLOY_DIR/.env"
COMPOSE_FILE="$DEPLOY_DIR/docker-compose.yml"

if [ ! -f "$ENV_FILE" ] || [ ! -f "$COMPOSE_FILE" ]; then
  echo "BPI streaming deployment is incomplete under $DEPLOY_DIR" >&2
  exit 1
fi

if [ "$desired" = true ]; then
  readiness=$(docker exec -i -e TARGET_DB="$ADP_DATABASE_NAME" "$ADP_POSTGRES_CONTAINER" sh -ceu \
    'psql -U "$POSTGRES_USER" -d "$TARGET_DB" -AtF"|"' <<'SQL'
SELECT
  (SELECT count(*) FROM public.wom_bpi_production_context_bindings WHERE enabled IS TRUE),
  (SELECT count(*) FROM public.wom_bpi_task_state_mappings WHERE enabled IS TRUE),
  (SELECT count(*) FROM public.wom_bpi_production_context_outbox WHERE publication_state = 'READY');
SQL
  )
  bindings=$(printf '%s\n' "$readiness" | cut -d'|' -f1)
  states=$(printf '%s\n' "$readiness" | cut -d'|' -f2)
  if [ "${bindings:-0}" -lt 1 ] || [ "${states:-0}" -lt 1 ]; then
    echo "cannot enable: at least one explicit scope binding and state mapping are required" >&2
    exit 1
  fi
fi

tmp=$(mktemp)
awk -v desired="$desired" '
  BEGIN { found = 0 }
  index($0, "MES_CONTEXT_OUTBOX_ENABLED=") == 1 {
    print "MES_CONTEXT_OUTBOX_ENABLED=" desired
    found = 1
    next
  }
  { print }
  END { if (!found) print "MES_CONTEXT_OUTBOX_ENABLED=" desired }
' "$ENV_FILE" > "$tmp"
chmod --reference="$ENV_FILE" "$tmp"
mv "$tmp" "$ENV_FILE"

cd "$DEPLOY_DIR"
docker compose --env-file .env -f docker-compose.yml --profile mes-context config \
  > /tmp/ft-mes-context-compose-resolved.yml
docker compose --env-file .env -f docker-compose.yml --profile mes-context \
  up -d --no-deps --force-recreate mes-production-context-outbox

container_id=$(docker compose --env-file .env -f docker-compose.yml --profile mes-context \
  ps -q mes-production-context-outbox)
if [ -z "$container_id" ]; then
  echo "MES context outbox container was not created" >&2
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
  echo "MES context outbox container did not become healthy" >&2
  exit 1
fi

actual=$(docker inspect -f '{{range .Config.Env}}{{println .}}{{end}}' "$container_id" \
  | awk -F= '$1 == "MES_CONTEXT_OUTBOX_ENABLED" { print $2 }')
if [ "$actual" != "$desired" ]; then
  echo "publisher state mismatch: expected $desired, got $actual" >&2
  exit 1
fi

printf 'CONTEXT_CONTAINER=running|healthy\n'
printf 'CONTEXT_ENABLED=%s\n' "$actual"
printf 'CONTEXT_OFFSETS\n'
docker exec ft-mes-bpi-streaming-kafka-1-1 \
  /opt/kafka/bin/kafka-get-offsets.sh \
  --bootstrap-server kafka-1:19092 \
  --topic mes.production.context.v1
