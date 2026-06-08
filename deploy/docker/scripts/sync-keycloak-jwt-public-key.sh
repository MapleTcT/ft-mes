#!/usr/bin/env bash
set -euo pipefail

REALM="${ADP_KEYCLOAK_REALM:-dt}"
CONFIG_FILE="${NACOS_JWT_CONFIG_FILE:-nacos-rendered/supfusion-jwt-common.properties}"

COMPOSE_ARGS=()
if [[ -f .env ]]; then
  COMPOSE_ARGS+=(--env-file .env)
fi

if [[ ! -f "$CONFIG_FILE" ]]; then
  echo "Nacos JWT config not found: $CONFIG_FILE" >&2
  echo "Run scripts/render-nacos-configs.py first." >&2
  exit 1
fi

PUBLIC_KEY=$(docker compose "${COMPOSE_ARGS[@]}" exec -T keycloak sh -s -- "$REALM" <<'KEYCLOAK_SCRIPT'
set -eu
REALM="$1"
curl -fsS "http://127.0.0.1:8080/auth/realms/$REALM" \
  | tr -d '\n' \
  | sed -n 's/.*"public_key"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p'
KEYCLOAK_SCRIPT
)

if [[ -z "$PUBLIC_KEY" ]]; then
  echo "Could not read Keycloak public key for realm: $REALM" >&2
  exit 1
fi

python3 - "$CONFIG_FILE" "$PUBLIC_KEY" <<'PY'
from pathlib import Path
import sys

path = Path(sys.argv[1])
public_key = sys.argv[2]
lines = path.read_text(encoding="utf-8").splitlines()
for index, line in enumerate(lines):
    if line.startswith("supfusion.cloud.jwt.secret="):
        lines[index] = "supfusion.cloud.jwt.secret=" + public_key
        break
else:
    lines.append("supfusion.cloud.jwt.secret=" + public_key)
path.write_text("\n".join(lines) + "\n", encoding="utf-8")
PY

echo "Updated $CONFIG_FILE with Keycloak realm '$REALM' public key."

if [[ "${ADP_SKIP_NACOS_PUBLISH:-false}" != "true" ]]; then
  docker compose "${COMPOSE_ARGS[@]}" run --rm nacos-config
fi

if [[ "$#" -gt 0 ]]; then
  docker compose "${COMPOSE_ARGS[@]}" restart "$@"
fi
