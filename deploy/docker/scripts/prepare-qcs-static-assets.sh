#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
ROOT_DIR="$(cd "$DEPLOY_DIR/../.." && pwd)"
WORKSPACE_DIR="$(cd "$ROOT_DIR/.." && pwd)"

MODULES_ROOT="${MES_MODULES_ROOT:-$WORKSPACE_DIR/mes-modules-source-repo/modules}"
QCS_STATIC_SRC="$MODULES_ROOT/lims/QCS_6.1.3.5/service/src/main/resources/custom/QCS/static"
STATIC_ROOT="${ADP_STATIC_ROOT:-$ROOT_DIR/runtime/bap-server/bap-workspace/bap-static}"
QCS_STATIC_DEST="$STATIC_ROOT/greenDill/static/QCS/static"

if [[ ! -d "$QCS_STATIC_SRC" ]]; then
  echo "QCS static source not found: $QCS_STATIC_SRC" >&2
  exit 1
fi

mkdir -p "$QCS_STATIC_DEST"
cp -R "$QCS_STATIC_SRC"/. "$QCS_STATIC_DEST"/

echo "QCS static assets prepared: $QCS_STATIC_DEST"
