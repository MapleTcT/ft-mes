#!/bin/sh
set -eu

compose_project="${COMPOSE_PROJECT_NAME:-adp-mes-newbase}"
service_name="${KEYCLOAK_SERVICE_NAME:-keycloak}"
group="${SUPOS_NACOS_REGISTRY_GROUP:-prod}"
nacos_url="${NACOS_URL:-http://127.0.0.1:18848/nacos}"

container="${compose_project}-${service_name}-1"
if ! docker inspect "$container" >/dev/null 2>&1; then
  container="$(docker compose ps -q "$service_name")"
fi

if [ -z "$container" ]; then
  echo "Keycloak container not found for service: ${service_name}" >&2
  exit 1
fi

keycloak_ip="$(docker inspect "$container" --format '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}')"
if [ -z "$keycloak_ip" ]; then
  echo "Keycloak container has no Docker network IP: ${container}" >&2
  exit 1
fi

curl -fsS -X POST "${nacos_url}/v1/ns/instance" \
  --data-urlencode "serviceName=keycloak" \
  --data-urlencode "groupName=${group}" \
  --data-urlencode "ip=${keycloak_ip}" \
  --data-urlencode "port=8080" \
  --data-urlencode "healthy=true" \
  --data-urlencode "enabled=true" \
  --data-urlencode "weight=1.0" \
  --data-urlencode "ephemeral=false" >/dev/null

echo "registered keycloak ${keycloak_ip}:8080 to Nacos group ${group}"
