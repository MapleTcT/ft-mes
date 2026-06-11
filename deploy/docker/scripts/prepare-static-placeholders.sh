#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../../.." && pwd)
STATIC_ROOT="${ADP_STATIC_ROOT:-$ROOT_DIR/runtime/bap-server/bap-workspace/bap-static}"

write_placeholder() {
  target="$1"
  message="$2"
  mkdir -p "$(dirname "$target")"
  if [ ! -f "$target" ]; then
    printf '%s\n' "$message" > "$target"
  fi
}

write_placeholder \
  "$STATIC_ROOT/bap/static/adp-custom/login/script/index.js" \
  "// Docker test placeholder for optional login customization."

write_placeholder \
  "$STATIC_ROOT/bap/static/adp-custom/login/style/index.css" \
  "/* Docker test placeholder for optional login styling. */"

write_placeholder \
  "$STATIC_ROOT/bap/static/adp-custom/homepage/script/index.js" \
  "// Docker test placeholder for optional homepage customization."

write_placeholder \
  "$STATIC_ROOT/bap/static/adp-custom/homepage/style/index.css" \
  "/* Docker test placeholder for optional homepage styling. */"
