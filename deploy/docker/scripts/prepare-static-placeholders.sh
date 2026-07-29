#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../../.." && pwd)
STATIC_ROOT="${ADP_STATIC_ROOT:-$ROOT_DIR/runtime/bap-server/bap-workspace/bap-static}"
BRANDING_ROOT="$ROOT_DIR/deploy/docker/assets/branding"

install_branding_file() {
  source_file="$1"
  target_file="$2"
  if [ ! -f "$source_file" ]; then
    echo "Branding source not found: $source_file" >&2
    exit 1
  fi
  mkdir -p "$(dirname "$target_file")"
  cp "$source_file" "$target_file"
}

install_branding_file \
  "$BRANDING_ROOT/login.css" \
  "$STATIC_ROOT/bap/static/adp-custom/login/style/index.css"

install_branding_file \
  "$BRANDING_ROOT/login.js" \
  "$STATIC_ROOT/bap/static/adp-custom/login/script/index.js"

install_branding_file \
  "$BRANDING_ROOT/homepage.css" \
  "$STATIC_ROOT/bap/static/adp-custom/homepage/style/index.css"

install_branding_file \
  "$BRANDING_ROOT/homepage.js" \
  "$STATIC_ROOT/bap/static/adp-custom/homepage/script/index.js"

install_branding_file \
  "$BRANDING_ROOT/feitian-logo-login.png" \
  "$STATIC_ROOT/bap/static/adp-custom/branding/feitian-logo-login.png"

install_branding_file \
  "$BRANDING_ROOT/feitian-logo-title.png" \
  "$STATIC_ROOT/bap/static/adp-custom/branding/feitian-logo-title.png"
