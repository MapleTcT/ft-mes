#!/bin/sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../../.." && pwd)
DEPLOY_DIR="$ROOT_DIR/deploy/bpi-runtime"
ENV_FILE=${1:-"$DEPLOY_DIR/.env"}
REPORT=${BPI_RUNTIME_SMOKE_REPORT:-/tmp/bpi-runtime-smoke.json}

env_value() {
    key=$1
    fallback=$2
    value=$(sed -n "s/^${key}=//p" "$ENV_FILE" | tail -1)
    printf '%s' "${value:-$fallback}"
}

compose() {
    docker compose --env-file "$ENV_FILE" -f "$DEPLOY_DIR/docker-compose.yml" "$@"
}

bind_address=$(env_value BPI_BIND_ADDRESS 127.0.0.1)
http_port=$(env_value BPI_HTTP_PORT 19091)
web_bind_address=$(env_value BPI_WEB_BIND_ADDRESS 127.0.0.1)
web_port=$(env_value BPI_WEB_PORT 18090)
database_name=$(env_value BPI_DATABASE_NAME ft_mes_bpi)
postgres_user=$(env_value POSTGRES_USER bpi_admin)
expected_flyway=$(env_value BPI_EXPECTED_FLYWAY_VERSION 12)

health=$(curl -fsS "http://${bind_address}:${http_port}/actuator/health")
printf '%s' "$health" | grep -q '"status":"UP"' || {
    printf 'ERROR: BPI service health is not UP\n' >&2
    exit 1
}

web_health=$(curl -fsS "http://${web_bind_address}:${web_port}/healthz")
test "$web_health" = "ok" || {
    printf 'ERROR: BPI web health is not OK\n' >&2
    exit 1
}

adapter_health=$(compose exec -T bpi-adapter \
    curl -fsS http://127.0.0.1:19080/actuator/health)
printf '%s' "$adapter_health" | grep -q '"status":"UP"' || {
    printf 'ERROR: BPI adapter health is not UP\n' >&2
    exit 1
}

migration=$(compose exec -T bpi-postgres \
    psql -At -U "$postgres_user" -d "$database_name" \
    -c "SELECT max(version::integer)::text FROM bpi.flyway_schema_history WHERE success")
test "$migration" = "$expected_flyway" || {
    printf 'ERROR: expected Flyway version %s, found %s\n' "$expected_flyway" "$migration" >&2
    exit 1
}

table_count=$(compose exec -T bpi-postgres \
    psql -At -U "$postgres_user" -d "$database_name" \
    -c "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'bpi'")
test "$table_count" -ge 21 || {
    printf 'ERROR: BPI schema table count is incomplete: %s\n' "$table_count" >&2
    exit 1
}

python3 - "$REPORT" "$bind_address" "$http_port" "$web_bind_address" "$web_port" "$database_name" "$migration" "$table_count" "$expected_flyway" <<'PY'
import json
import sys
from datetime import datetime, timezone
from pathlib import Path

path = Path(sys.argv[1])
report = {
    "generatedAt": datetime.now(timezone.utc).isoformat(),
    "status": "PASS",
    "service": {"bindAddress": sys.argv[2], "port": int(sys.argv[3]), "health": "UP"},
    "web": {"bindAddress": sys.argv[4], "port": int(sys.argv[5]), "health": "UP"},
    "adapter": {"health": "UP"},
    "postgres": {
        "database": sys.argv[6],
        "flywayVersion": sys.argv[7],
        "expectedFlywayVersion": sys.argv[9],
        "schemaTableCount": int(sys.argv[8]),
    },
}
path.write_text(json.dumps(report, ensure_ascii=True, indent=2) + "\n", encoding="utf-8")
print(f"BPI runtime smoke: PASS ({path})")
PY
