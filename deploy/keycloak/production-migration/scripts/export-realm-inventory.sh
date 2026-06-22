#!/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BASE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)

ENV_FILE=${ADP_KEYCLOAK_MIGRATION_ENV:-"$BASE_DIR/.env"}
if [ -f "$ENV_FILE" ]; then
  # shellcheck disable=SC1090
  set -a
  . "$ENV_FILE"
  set +a
fi

ROLE=${ADP_KEYCLOAK_INVENTORY_ROLE:-${KEYCLOAK_INVENTORY_ROLE:-source}}
REPORT_DIR=${ADP_KEYCLOAK_REPORT_DIR:-${KEYCLOAK_REPORT_DIR:-}}
REALM=${KEYCLOAK_REALM:-dt}
USER_SAMPLE_SIZE=${KEYCLOAK_USER_SAMPLE_SIZE:-50}
CURL_FLAGS=${KEYCLOAK_CURL_FLAGS:-}

require_var() {
  name=$1
  eval "value=\${$name:-}"
  if [ -z "$value" ]; then
    echo "Missing required environment variable: $name" >&2
    exit 2
  fi
}

json_get_access_token() {
  python3 -c 'import json,sys; print(json.load(sys.stdin).get("access_token",""))'
}

case "$ROLE" in
  source)
    BASE_URL=${SOURCE_KEYCLOAK_BASE_URL:-}
    ADMIN_REALM=${SOURCE_KEYCLOAK_ADMIN_REALM:-master}
    ADMIN_USER=${SOURCE_KEYCLOAK_ADMIN_USER:-}
    ADMIN_PASSWORD=${SOURCE_KEYCLOAK_ADMIN_PASSWORD:-}
    ;;
  target)
    BASE_URL=${TARGET_KEYCLOAK_BASE_URL:-}
    ADMIN_REALM=${TARGET_KEYCLOAK_ADMIN_REALM:-master}
    ADMIN_USER=${TARGET_KEYCLOAK_ADMIN_USER:-}
    ADMIN_PASSWORD=${TARGET_KEYCLOAK_ADMIN_PASSWORD:-}
    ;;
  *)
    echo "Unsupported ADP_KEYCLOAK_INVENTORY_ROLE: $ROLE" >&2
    echo "Allowed values: source, target" >&2
    exit 2
    ;;
esac

require_var REPORT_DIR

if [ -z "$BASE_URL" ] || [ -z "$ADMIN_USER" ] || [ -z "$ADMIN_PASSWORD" ]; then
  echo "Missing Keycloak connection settings for role: $ROLE" >&2
  echo "Set ${ROLE}_KEYCLOAK_BASE_URL, ${ROLE}_KEYCLOAK_ADMIN_USER and ${ROLE}_KEYCLOAK_ADMIN_PASSWORD in the secure env file." >&2
  exit 2
fi

if ! command -v curl >/dev/null 2>&1; then
  echo "curl is required for Keycloak inventory" >&2
  exit 2
fi
if ! command -v python3 >/dev/null 2>&1; then
  echo "python3 is required for Keycloak inventory normalization" >&2
  exit 2
fi

BASE_URL=$(printf '%s' "$BASE_URL" | sed 's#/*$##')
RAW_DIR="$REPORT_DIR/raw/$ROLE"
mkdir -p "$RAW_DIR"

# shellcheck disable=SC2086
TOKEN_RESPONSE=$(curl -fsS $CURL_FLAGS \
  -X POST "$BASE_URL/realms/$ADMIN_REALM/protocol/openid-connect/token" \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode 'grant_type=password' \
  --data-urlencode 'client_id=admin-cli' \
  --data-urlencode "username=$ADMIN_USER" \
  --data-urlencode "password=$ADMIN_PASSWORD")

ACCESS_TOKEN=$(printf '%s' "$TOKEN_RESPONSE" | json_get_access_token)
if [ -z "$ACCESS_TOKEN" ]; then
  echo "Could not obtain Keycloak admin access token for role: $ROLE" >&2
  exit 1
fi

api_get() {
  path=$1
  output=$2
  # shellcheck disable=SC2086
  curl -fsS $CURL_FLAGS \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -H 'Accept: application/json' \
    "$BASE_URL/admin/realms/$REALM/$path" >"$output"
}

api_get "" "$RAW_DIR/realm.json"
api_get "clients" "$RAW_DIR/clients.json"
api_get "roles" "$RAW_DIR/roles.json"
api_get "client-scopes" "$RAW_DIR/client-scopes.json"
api_get "components" "$RAW_DIR/components.json"
api_get "users/count" "$RAW_DIR/users-count.json"
api_get "users?first=0&max=$USER_SAMPLE_SIZE" "$RAW_DIR/users-sample.json"

python3 "$SCRIPT_DIR/normalize-realm-inventory.py" \
  --role "$ROLE" \
  --realm "$REALM" \
  --raw-dir "$RAW_DIR" \
  --output-dir "$REPORT_DIR"

echo "Keycloak $ROLE realm inventory complete: $REPORT_DIR/${ROLE}-realm-inventory.json"
