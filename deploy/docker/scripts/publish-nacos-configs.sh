#!/bin/sh
set -eu

nacos_url="http://${SUPOS_NACOS_ADDRESS:-nacos:8848}"
group="${SUPOS_NACOS_REGISTRY_GROUP:-prod}"
config_dir="${NACOS_CONFIG_DIR:-/work/nacos-rendered}"

echo "waiting for Nacos at ${nacos_url}"
for _ in $(seq 1 90); do
  if curl -fsS "${nacos_url}/nacos/" >/dev/null 2>&1; then
    break
  fi
  sleep 2
done

if ! curl -fsS "${nacos_url}/nacos/" >/dev/null 2>&1; then
  echo "Nacos did not become reachable: ${nacos_url}" >&2
  exit 1
fi

published=0
for file in "${config_dir}"/*.properties; do
  [ -f "$file" ] || continue
  data_id="$(basename "$file")"
  curl -fsS -X POST "${nacos_url}/nacos/v1/cs/configs" \
    --data-urlencode "dataId=${data_id}" \
    --data-urlencode "group=${group}" \
    --data-urlencode "type=properties" \
    --data-urlencode "content@${file}" >/dev/null
  published=$((published + 1))
  echo "published ${data_id} to group ${group}"
done

echo "published ${published} Nacos config files"
