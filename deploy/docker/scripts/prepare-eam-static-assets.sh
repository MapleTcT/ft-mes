#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
ROOT_DIR="$(cd "$DEPLOY_DIR/../.." && pwd)"
WORKSPACE_DIR="$(cd "$ROOT_DIR/.." && pwd)"

MODULES_ROOT="${MES_MODULES_ROOT:-$WORKSPACE_DIR/mes-modules-source-repo/modules}"
STATIC_ROOT="${ADP_STATIC_ROOT:-$ROOT_DIR/runtime/bap-server/bap-workspace/bap-static}"

copy_asset() {
  local source="$1"
  local destination="$2"

  if [[ ! -f "$source" ]]; then
    echo "EAM static source not found: $source" >&2
    exit 1
  fi

  mkdir -p "$(dirname "$destination")"
  cp "$source" "$destination"
  echo "prepared: $destination"
}

copy_asset \
  "$MODULES_ROOT/eam/EQPTOperation_6.1.9.3/service/src/main/resources/custom/EQPTOperation/js/common.js" \
  "$STATIC_ROOT/greenDill/static/EQPTOperation/js/common.js"

copy_asset \
  "$MODULES_ROOT/eam/Measure_6.1.9.3/service/src/main/resources/static/Measure/js/checkPower.js" \
  "$STATIC_ROOT/greenDill/static/Measure/js/checkPower.js"

copy_asset \
  "$MODULES_ROOT/eam/Measure_6.1.9.3/service/src/main/resources/static/Measure/js/qrcode.min.js" \
  "$STATIC_ROOT/greenDill/static/Measure/js/qrcode.min.js"

copy_asset \
  "$MODULES_ROOT/eam/Measure_6.1.9.3/service/src/main/resources/static/Measure/js/MEM.js" \
  "$STATIC_ROOT/greenDill/static/Measure/js/MEM.js"

copy_asset \
  "$MODULES_ROOT/eam/OverhaulTicket_6.1.9.3/service/src/main/resources/custom/OverhaulTicket/js/common.js" \
  "$STATIC_ROOT/greenDill/static/OverhaulTicket/js/common.js"

copy_asset \
  "$MODULES_ROOT/eam/PartiManage_6.1.9.3/service/src/main/resources/custom/PartiManage/customJs/MessageNotice.js" \
  "$STATIC_ROOT/greenDill/static/PartiManage/customJs/MessageNotice.js"

copy_asset \
  "$MODULES_ROOT/eam/SpareManage_6.1.9.3/service/src/main/resources/static/SpareManage/js/spareManage.js" \
  "$STATIC_ROOT/greenDill/static/SpareManage/js/spareManage.js"

copy_asset \
  "$MODULES_ROOT/eam/maintenance_6.1.9.3/service/src/main/resources/static/maintenance/js/maintenance.js" \
  "$STATIC_ROOT/greenDill/static/maintenance/js/maintenance.js"

copy_asset \
  "$MODULES_ROOT/eam/maintenance_6.1.9.3/service/src/main/resources/static/maintenance/js/commonTool.js" \
  "$STATIC_ROOT/greenDill/static/maintenance/js/commonTool.js"

echo "EAM static assets prepared under: $STATIC_ROOT/greenDill/static"
