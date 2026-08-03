#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
ROOT_DIR="$(cd "$DEPLOY_DIR/../.." && pwd)"
I18N_DIR="$ROOT_DIR/deploy/docker/assets/module-static/LIMSSample"

python3 "$SCRIPT_DIR/generate-module-i18n-js.py" \
  --properties "$I18N_DIR/LIMSSample_M_zh_CN.properties" \
  --fallback-properties "$I18N_DIR/platform-common-zh_CN.properties" \
  --fallback-properties "$I18N_DIR/LIMSSample_C_zh_CN.properties" \
  --output "$I18N_DIR/i18n-value.js" \
  --module-code LIMSSample

echo "LIMSSample i18n compatibility prepared: $I18N_DIR/i18n-value.js"
