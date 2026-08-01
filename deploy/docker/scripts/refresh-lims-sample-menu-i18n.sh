#!/usr/bin/env bash
set -euo pipefail

postgres_container="${ADP_POSTGRES_CONTAINER:-adp-mes-newbase-postgres-1}"
redis_container="${ADP_REDIS_CONTAINER:-adp-mes-newbase-redis-1}"
postgres_user="${ADP_POSTGRES_USER:-adp}"
postgres_database="${ADP_POSTGRES_DATABASE:-adp}"

query="
SELECT langu_code, i18n_key, i18n_value
  FROM public.supfusion_i18n_resource
 WHERE module_code = 'LIMSSample'
   AND i18n_key LIKE 'LIMSSample.menu.group.%'
   AND langu_code IN ('zh_CN', 'en_US', 'zh_HK')
   AND valid = '1'
   AND coalesce(tenant_id, 'dt') = 'dt'
 ORDER BY langu_code, i18n_key;
"

row_count=0
while IFS=$'\t' read -r language resource_key resource_value; do
  if [[ -z "${language}" || -z "${resource_key}" ]]; then
    continue
  fi
  docker exec "${redis_container}" redis-cli HSET \
    "tenant_dt_LIMSSample_${language}" "${resource_key}" "${resource_value}" >/dev/null
  row_count=$((row_count + 1))
done < <(
  docker exec "${postgres_container}" psql \
    -U "${postgres_user}" -d "${postgres_database}" -At -F $'\t' -c "${query}"
)

if [[ "${row_count}" -ne 15 ]]; then
  echo "Expected 15 LIMSSample group translations, refreshed ${row_count}" >&2
  exit 1
fi

actual_value="$(
  docker exec "${redis_container}" redis-cli --raw HGET \
    tenant_dt_LIMSSample_zh_CN LIMSSample.menu.group.registerCollect
)"

if [[ "${actual_value}" != "登记与取样" ]]; then
  echo "LIMSSample zh_CN cache verification failed: ${actual_value}" >&2
  exit 1
fi

echo "Refreshed ${row_count} LIMSSample menu translations in Redis"
