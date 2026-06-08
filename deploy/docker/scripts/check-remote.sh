#!/bin/sh
set -eu

base_url="${1:-http://127.0.0.1:${ADP_HTTP_PORT:-18080}}"

echo "checking ${base_url}"
curl -fsSI "${base_url}/" | sed -n '1,12p'
curl -fsS "${base_url}/" | sed -n '1,20p' >/dev/null
echo "frontend index is reachable"

echo "gateway health probe"
curl -fsSI "http://127.0.0.1:${ADP_GATEWAY_PORT:-18008}/" | sed -n '1,12p' || true
