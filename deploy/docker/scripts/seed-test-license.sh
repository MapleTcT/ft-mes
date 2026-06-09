#!/bin/sh
set -eu

if [ "${ADP_SEED_TEST_LICENSE:-true}" != "true" ]; then
  echo "test license seed disabled"
  exit 0
fi

redis_host="${ADP_REDIS_HOST:-redis}"
redis_port="${ADP_REDIS_PORT:-6379}"
redis_auth="${ADP_REDIS_PASSWORD:-}"
license_key="LICENSE:INFO"
salt="licenseSalt"
now_ms="$(($(date +%s) * 1000))"

redis_cli() {
  if [ -n "$redis_auth" ]; then
    redis-cli -h "$redis_host" -p "$redis_port" -a "$redis_auth" "$@"
  else
    redis-cli -h "$redis_host" -p "$redis_port" "$@"
  fi
}

b64() {
  printf '%s' "$1" | base64 | tr -d '\n'
}

md5() {
  printf '%s' "$1" | md5sum | awk '{print $1}'
}

seed_license() {
  module_code="$1"
  module_license_key="$2"
  value="$3"

  hash="$(md5 "${module_code}${module_license_key}${value}${now_ms}${salt}")"
  hkey="$(b64 "$module_code")/$(b64 "$module_license_key")"
  hvalue="$(b64 "$value")/$(b64 "$now_ms")/$hash"
  redis_cli HSET "$license_key" "$hkey" "$hvalue" >/dev/null
  echo "seeded test license: ${module_code}=${value}"
}

redis_cli PING >/dev/null

seed_license \
  "supPlant-Dev" \
  "EdrvXM2VSorwfKb4iDrzMMRgDzfLimq73HOkrltncTOK1xXIAa+5nQJum1DguTH2XIxtGQ==" \
  "0"

seed_license \
  "supPlant-Server-S0C" \
  "EdrvXM2VSorwfKb4iDrzMPMOQapgN5//3HOkrltncTPXKqYwEtTtcqxWPTxOx3LDnoI+iw==" \
  "${ADP_TEST_LOGIN_LIMIT:-255}"
