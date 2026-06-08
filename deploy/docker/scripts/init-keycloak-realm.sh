#!/usr/bin/env bash
set -euo pipefail

REALM="${ADP_KEYCLOAK_REALM:-dt}"
KEYCLOAK_ADMIN_USER="${KEYCLOAK_ADMIN_USER:-admin}"
KEYCLOAK_ADMIN_PASSWORD="${KEYCLOAK_ADMIN_PASSWORD:-admin}"

COMPOSE_ARGS=()
if [[ -f .env ]]; then
  COMPOSE_ARGS+=(--env-file .env)
fi

docker compose "${COMPOSE_ARGS[@]}" exec -T keycloak sh -s -- \
  "$REALM" "$KEYCLOAK_ADMIN_USER" "$KEYCLOAK_ADMIN_PASSWORD" <<'KEYCLOAK_SCRIPT'
set -eu

REALM="$1"
KEYCLOAK_ADMIN_USER="$2"
KEYCLOAK_ADMIN_PASSWORD="$3"
KC=/opt/keycloak/bin/kcadm.sh
SERVER=http://127.0.0.1:8080/auth

$KC config credentials \
  --server "$SERVER" \
  --realm master \
  --user "$KEYCLOAK_ADMIN_USER" \
  --password "$KEYCLOAK_ADMIN_PASSWORD" >/dev/null

csv_id_by_name() {
  name="$1"
  awk -F, -v n="\"$name\"" '$2==n {gsub(/\"/, "", $1); print $1; exit}'
}

client_id_by_clientId() {
  client_id="$1"
  $KC get clients -r "$REALM" --fields id,clientId --format csv \
    | awk -F, -v n="\"$client_id\"" '$2==n {gsub(/\"/, "", $1); print $1; exit}'
}

realm_id() {
  $KC get "realms/$REALM" \
    | sed -n 's/^[[:space:]]*"id"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' \
    | head -1
}

if ! $KC get "realms/$REALM" >/dev/null 2>&1; then
  $KC create realms \
    -s "realm=$REALM" \
    -s enabled=true \
    -s sslRequired=none \
    -s ssoSessionIdleTimeout=5184000 \
    -s ssoSessionMaxLifespan=5184000 >/dev/null
fi
$KC update "realms/$REALM" -s sslRequired=none -s enabled=true >/dev/null

SCOPE_ID=$($KC get client-scopes -r "$REALM" --fields id,name --format csv | csv_id_by_name supos || true)
if [ -z "${SCOPE_ID:-}" ]; then
  cat >/tmp/supos-client-scope.json <<'JSON'
{
  "name": "supos",
  "protocol": "openid-connect",
  "attributes": {
    "display.on.consent.screen": "true",
    "include.in.token.scope": "true"
  }
}
JSON
  $KC create client-scopes -r "$REALM" -f /tmp/supos-client-scope.json >/dev/null
  SCOPE_ID=$($KC get client-scopes -r "$REALM" --fields id,name --format csv | csv_id_by_name supos)
fi

mapper_exists() {
  $KC get "client-scopes/$SCOPE_ID/protocol-mappers/models" -r "$REALM" 2>/dev/null \
    | grep -q "\"name\" : \"$1\""
}

add_mapper() {
  name="$1"
  user_attr="$2"
  claim="$3"
  json_type="$4"
  if mapper_exists "$name"; then
    return 0
  fi
  cat >/tmp/supos-mapper.json <<JSON
{
  "name": "$name",
  "protocol": "openid-connect",
  "protocolMapper": "oidc-usermodel-attribute-mapper",
  "config": {
    "userinfo.token.claim": "true",
    "user.attribute": "$user_attr",
    "id.token.claim": "true",
    "access.token.claim": "true",
    "claim.name": "$claim",
    "jsonType.label": "$json_type"
  }
}
JSON
  $KC create "client-scopes/$SCOPE_ID/protocol-mappers/models" -r "$REALM" -f /tmp/supos-mapper.json >/dev/null
}

add_mapper userName userName user_name String
add_mapper userId id user_id long
add_mapper departmentCode departmentCode department_code String
add_mapper departmentId departmentId department_id long
add_mapper positionName positionName position_name String
add_mapper companyType companyType company_type String
add_mapper departmentName departmentName department_name String
add_mapper positionId positionId position_id long
add_mapper staffCode personCode staff_code String
add_mapper positionCompanyId positionCompanyId position_company_id long
add_mapper userType userType user_type int
add_mapper companyName companyName company_name String
add_mapper positionCode positionCode position_code String
add_mapper companyCode companyCode company_code String
add_mapper staffId personId staff_id long
add_mapper staffName personName staff_name String
add_mapper companyId companyId company_id long

create_client() {
  client_id="$1"
  access_lifespan="$2"
  max_life="$3"
  idle="$4"
  implicit="$5"
  cid=$(client_id_by_clientId "$client_id" || true)
  if [ -z "${cid:-}" ]; then
    cat >/tmp/supos-client.json <<JSON
{
  "clientId": "$client_id",
  "enabled": true,
  "directAccessGrantsEnabled": true,
  "serviceAccountsEnabled": true,
  "standardFlowEnabled": false,
  "implicitFlowEnabled": $implicit,
  "publicClient": true,
  "bearerOnly": false,
  "attributes": {
    "access.token.lifespan": "$access_lifespan",
    "client.session.max.lifespan": "$max_life",
    "client.session.idle.timeout": "$idle"
  },
  "defaultClientScopes": ["supos"]
}
JSON
    $KC create clients -r "$REALM" -f /tmp/supos-client.json >/dev/null
    cid=$(client_id_by_clientId "$client_id")
  else
    $KC update "clients/$cid" -r "$REALM" \
      -s enabled=true \
      -s directAccessGrantsEnabled=true \
      -s serviceAccountsEnabled=true \
      -s standardFlowEnabled=false \
      -s "implicitFlowEnabled=$implicit" \
      -s publicClient=true \
      -s bearerOnly=false >/dev/null
  fi

  if ! $KC get "clients/$cid/default-client-scopes" -r "$REALM" --fields id,name --format csv \
    | awk -F, '$2=="\"supos\"" {found=1} END{exit found?0:1}'; then
    $KC create "clients/$cid/default-client-scopes/$SCOPE_ID" -r "$REALM" >/dev/null || true
  fi
}

create_client "pc_$REALM" 1800 43200 43200 false
create_client "mobile_$REALM" 2592000 5184000 5184000 true

REALM_ID=$(realm_id)
if [ -z "$REALM_ID" ]; then
  echo "Could not resolve internal Keycloak realm id for $REALM" >&2
  exit 1
fi

COMPONENT_ID=$($KC get components -r "$REALM" --fields id,name --format csv \
  | awk -F, '$2=="\"readonly-property-file\"" {gsub(/\"/, "", $1); print $1; exit}')
if [ -z "${COMPONENT_ID:-}" ]; then
  cat >/tmp/supos-user-storage.json <<JSON
{
  "name": "readonly-property-file",
  "providerId": "readonly-property-file",
  "providerType": "org.keycloak.storage.UserStorageProvider",
  "parentId": "$REALM_ID",
  "config": {
    "cachePolicy": ["NO_CACHE"],
    "enabled": ["true"],
    "priority": ["0"]
  }
}
JSON
  $KC create components -r "$REALM" -f /tmp/supos-user-storage.json >/dev/null
else
  $KC update "components/$COMPONENT_ID" -r "$REALM" \
    -s "id=$COMPONENT_ID" \
    -s name=readonly-property-file \
    -s providerId=readonly-property-file \
    -s providerType=org.keycloak.storage.UserStorageProvider \
    -s "parentId=$REALM_ID" \
    -s 'config.cachePolicy=["NO_CACHE"]' \
    -s 'config.enabled=["true"]' \
    -s 'config.priority=["0"]' >/dev/null
fi

if ! $KC get authentication/flows -r "$REALM" | grep -q '"alias" : "lfy grant"'; then
  cat >/tmp/lfy-grant-flow.json <<'JSON'
{
  "alias": "lfy grant",
  "description": "OpenID Connect Resource Owner Grant",
  "providerId": "basic-flow",
  "topLevel": true,
  "builtIn": false
}
JSON
  $KC create authentication/flows -r "$REALM" -f /tmp/lfy-grant-flow.json >/dev/null
  $KC create authentication/flows/lfy%20grant/executions/execution \
    -r "$REALM" \
    -s provider=lfy-grant-validate-username >/dev/null
fi
$KC update "realms/$REALM" -s directGrantFlow='lfy grant' >/dev/null

echo "Keycloak realm '$REALM' is ready."
KEYCLOAK_SCRIPT
