#!/bin/sh
set -eu

# Installs the WOM production-context publisher on an existing BPI streaming
# host. The dispatcher is deliberately kept disabled until site mappings pass.

BPI_STREAMING_ROOT=${BPI_STREAMING_ROOT:-/home/v6/ft-mes-bpi-streaming}
STAGE_DIR=${MES_CONTEXT_STAGE_DIR:-/tmp/ft-mes-context-deploy}
ADP_POSTGRES_CONTAINER=${ADP_POSTGRES_CONTAINER:-adp-mes-newbase-postgres-1}
ADP_DATABASE_NAME=${ADP_DATABASE_NAME:-adp}
ADP_RUNTIME_NETWORK_NAME=${ADP_RUNTIME_NETWORK_NAME:-adp-mes-newbase_default}
MES_CONTEXT_IMAGE_TAG=${MES_CONTEXT_IMAGE_TAG:-ft-mes-production-context-outbox:local}
MES_CONTEXT_JAVA_IMAGE=${MES_CONTEXT_JAVA_IMAGE:-m.daocloud.io/docker.io/eclipse-temurin:8-jre}
MES_CONTEXT_HTTP_PORT=${MES_CONTEXT_HTTP_PORT:-19082}

DEPLOY_DIR="$BPI_STREAMING_ROOT/deploy/bpi-streaming"
MODULE_DIR="$BPI_STREAMING_ROOT/backend/source-modules/mes-production-context-outbox"
ENV_FILE="$DEPLOY_DIR/.env"
COMPOSE_FILE="$DEPLOY_DIR/docker-compose.yml"

required_files="
$STAGE_DIR/docker-compose.yml
$STAGE_DIR/Dockerfile.runtime
$STAGE_DIR/mes-production-context-outbox.jar
$STAGE_DIR/176-wom-bpi-production-context-outbox.sql
$STAGE_DIR/177-wom-bpi-context-revision-clock-floor.sql
"

for file in $required_files; do
  if [ ! -f "$file" ]; then
    echo "missing staged deployment file: $file" >&2
    exit 1
  fi
done

if [ ! -f "$ENV_FILE" ] || [ ! -f "$COMPOSE_FILE" ]; then
  echo "existing BPI streaming deployment is incomplete under $DEPLOY_DIR" >&2
  exit 1
fi

read_env_value() {
  key=$1
  awk -v key="$key" 'index($0, key "=") == 1 { print substr($0, length(key) + 2); exit }' "$ENV_FILE"
}

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

stamp=$(date +%Y%m%d%H%M%S)
backup_file="$COMPOSE_FILE.bak.$stamp"
cp "$COMPOSE_FILE" "$backup_file"
mkdir -p "$MODULE_DIR/target"
install -m 0644 "$STAGE_DIR/docker-compose.yml" "$COMPOSE_FILE"
install -m 0644 "$STAGE_DIR/Dockerfile.runtime" "$MODULE_DIR/Dockerfile.runtime"
install -m 0644 "$STAGE_DIR/mes-production-context-outbox.jar" \
  "$MODULE_DIR/target/mes-production-context-outbox-0.1.0-SNAPSHOT.jar"

# The migration is additive and intentionally seeds no line or state semantics.
docker exec -i -e TARGET_DB="$ADP_DATABASE_NAME" "$ADP_POSTGRES_CONTAINER" sh -ceu \
  'psql -U "$POSTGRES_USER" -d "$TARGET_DB" -v ON_ERROR_STOP=1' \
  < "$STAGE_DIR/176-wom-bpi-production-context-outbox.sql" \
  > /tmp/ft-mes-context-migration-176.log
docker exec -i -e TARGET_DB="$ADP_DATABASE_NAME" "$ADP_POSTGRES_CONTAINER" sh -ceu \
  'psql -U "$POSTGRES_USER" -d "$TARGET_DB" -v ON_ERROR_STOP=1' \
  < "$STAGE_DIR/177-wom-bpi-context-revision-clock-floor.sql" \
  > /tmp/ft-mes-context-migration-177.log

role_password=$(read_env_value MES_CONTEXT_DATABASE_PASSWORD)
if [ -z "$role_password" ]; then
  role_password=$(openssl rand -hex 32)
fi

docker exec -i \
  -e ROLE_PASSWORD="$role_password" \
  -e TARGET_DB="$ADP_DATABASE_NAME" \
  "$ADP_POSTGRES_CONTAINER" sh -ceu \
  'psql -U "$POSTGRES_USER" -d "$TARGET_DB" -v ON_ERROR_STOP=1 -v role_password="$ROLE_PASSWORD" -v target_db="$TARGET_DB"' \
  <<'SQL' > /tmp/ft-mes-context-role.log
SELECT 'CREATE ROLE mes_context_outbox LOGIN' WHERE NOT EXISTS (
  SELECT 1 FROM pg_roles WHERE rolname = 'mes_context_outbox'
)\gexec
ALTER ROLE mes_context_outbox WITH LOGIN PASSWORD :'role_password';
GRANT CONNECT ON DATABASE :"target_db" TO mes_context_outbox;
GRANT USAGE ON SCHEMA public TO mes_context_outbox;
GRANT SELECT, UPDATE ON public.wom_bpi_production_context_outbox TO mes_context_outbox;
SQL

upsert_env ADP_RUNTIME_NETWORK_NAME "$ADP_RUNTIME_NETWORK_NAME"
upsert_env MES_CONTEXT_JAVA_IMAGE "$MES_CONTEXT_JAVA_IMAGE"
upsert_env MES_CONTEXT_OUTBOX_IMAGE "$MES_CONTEXT_IMAGE_TAG"
upsert_env MES_CONTEXT_DATABASE_URL "jdbc:postgresql://postgres:5432/$ADP_DATABASE_NAME"
upsert_env MES_CONTEXT_DATABASE_USERNAME mes_context_outbox
upsert_env MES_CONTEXT_DATABASE_PASSWORD "$role_password"
upsert_env MES_CONTEXT_KAFKA_CLIENT_ID ft-mes-context-outbox
upsert_env MES_CONTEXT_OUTBOX_ENABLED false
upsert_env MES_CONTEXT_HTTP_PORT "$MES_CONTEXT_HTTP_PORT"
upsert_env MES_CONTEXT_BATCH_SIZE 100
upsert_env MES_CONTEXT_MAX_ATTEMPTS 20
upsert_env MES_CONTEXT_CLAIM_TIMEOUT_MS 120000
unset role_password

cd "$DEPLOY_DIR"
docker compose --env-file .env -f docker-compose.yml --profile mes-context config \
  > /tmp/ft-mes-context-compose-resolved.yml
docker compose --env-file .env -f docker-compose.yml --profile mes-context \
  build mes-production-context-outbox
docker compose --env-file .env -f docker-compose.yml --profile mes-context \
  up -d --no-deps mes-production-context-outbox

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

enabled=$(docker inspect -f '{{range .Config.Env}}{{println .}}{{end}}' "$container_id" \
  | awk -F= '$1 == "MES_CONTEXT_OUTBOX_ENABLED" { print $2 }')
if [ "$enabled" != false ]; then
  echo "safe installation requires MES_CONTEXT_OUTBOX_ENABLED=false" >&2
  exit 1
fi

printf 'BACKUP=%s\n' "$backup_file"
printf 'CONTEXT_CONTAINER=running|healthy\n'
printf 'CONTEXT_ENABLED=false\n'
printf 'MIGRATION_OBJECTS\n'
docker exec -i -e TARGET_DB="$ADP_DATABASE_NAME" "$ADP_POSTGRES_CONTAINER" sh -ceu \
  'psql -U "$POSTGRES_USER" -d "$TARGET_DB" -AtF"|"' <<'SQL'
SELECT to_regclass('public.wom_bpi_production_context_outbox'),
       to_regclass('public.wom_bpi_production_context_bindings'),
       EXISTS (
         SELECT 1 FROM pg_trigger
         WHERE tgname = 'trg_wom_bpi_production_context' AND NOT tgisinternal
       ),
       EXISTS (
         SELECT 1 FROM pg_trigger
         WHERE tgname = 'trg_wom_bpi_context_revision_clock_floor' AND NOT tgisinternal
       ),
       EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'mes_context_outbox');
SELECT publication_state, count(*)
FROM public.wom_bpi_production_context_outbox
GROUP BY publication_state
ORDER BY publication_state;
SQL

printf 'CONTEXT_OFFSETS_AFTER_DISABLED\n'
docker exec ft-mes-bpi-streaming-kafka-1-1 \
  /opt/kafka/bin/kafka-get-offsets.sh \
  --bootstrap-server kafka-1:19092 \
  --topic mes.production.context.v1
