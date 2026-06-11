#!/bin/sh
set -eu

service_name="${SYSTEMCONFIG_PROVIDER_SERVICE:-sysmanagement}"
systemconfig_name="${SYSTEMCONFIG_SERVICE_NAME:-systemconfig}"
port="${SYSTEMCONFIG_PORT:-30220}"
groups="${SYSTEMCONFIG_NACOS_GROUPS:-prod DEFAULT_GROUP}"
nacos_url="${NACOS_URL:-http://127.0.0.1:18848/nacos}"

container="$(docker compose ps -q "$service_name")"
if [ -z "$container" ]; then
  echo "Provider container not found for service: ${service_name}" >&2
  exit 1
fi

provider_ip="$(docker inspect "$container" --format '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}')"
if [ -z "$provider_ip" ]; then
  echo "Provider container has no Docker network IP: ${container}" >&2
  exit 1
fi

for group in $groups; do
  curl -fsS -X POST "${nacos_url}/v1/ns/instance" \
    --data-urlencode "serviceName=${systemconfig_name}" \
    --data-urlencode "groupName=${group}" \
    --data-urlencode "ip=${provider_ip}" \
    --data-urlencode "port=${port}" \
    --data-urlencode "healthy=true" \
    --data-urlencode "enabled=true" \
    --data-urlencode "weight=1.0" \
    --data-urlencode "ephemeral=false" >/dev/null
  echo "registered ${systemconfig_name} ${provider_ip}:${port} to Nacos group ${group}"
done
