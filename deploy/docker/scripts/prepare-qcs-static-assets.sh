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
QCS_LIST_I18N_DIR="$ROOT_DIR/deploy/docker/assets/module-static/QCS/inspect/inspect/manuInspectList"
QCS_LIST_I18N_PROPERTIES="$QCS_LIST_I18N_DIR/platform-common-zh_CN.properties"
QCS_LIST_I18N_OUTPUT="$QCS_LIST_I18N_DIR/i18n-value.js"
QCS_EDIT_I18N_DIR="$ROOT_DIR/deploy/docker/assets/module-static/QCS/inspect/inspect/manuInspectEdit"
QCS_EDIT_I18N_PROPERTIES="$QCS_EDIT_I18N_DIR/platform-common-zh_CN.properties"
QCS_EDIT_I18N_OUTPUT="$QCS_EDIT_I18N_DIR/i18n-value.js"
QCS_PLAN_LIST_I18N_DIR="$ROOT_DIR/deploy/docker/assets/module-static/QCS/testPlan/inspectPlan/manuInspPlanList"
QCS_PLAN_LIST_I18N_PROPERTIES="$QCS_PLAN_LIST_I18N_DIR/platform-common-zh_CN.properties"
QCS_PLAN_LIST_I18N_OUTPUT="$QCS_PLAN_LIST_I18N_DIR/i18n-value.js"
QCS_REPORT_VIEW_I18N_DIR="$ROOT_DIR/deploy/docker/assets/module-static/QCS/inspectReport/inspectReport/manuInspReportView"
QCS_REPORT_VIEW_I18N_PROPERTIES="$QCS_REPORT_VIEW_I18N_DIR/platform-common-zh_CN.properties"
QCS_REPORT_VIEW_I18N_OUTPUT="$QCS_REPORT_VIEW_I18N_DIR/i18n-value.js"

if [[ ! -d "$QCS_STATIC_SRC" ]]; then
  echo "QCS static source not found: $QCS_STATIC_SRC" >&2
  exit 1
fi

mkdir -p "$QCS_STATIC_DEST"
cp -R "$QCS_STATIC_SRC"/. "$QCS_STATIC_DEST"/

python3 "$SCRIPT_DIR/generate-module-i18n-js.py" \
  --properties "$QCS_LIST_I18N_PROPERTIES" \
  --output "$QCS_LIST_I18N_OUTPUT" \
  --module-code QCS_MANU_INSPECT_LIST

python3 "$SCRIPT_DIR/generate-module-i18n-js.py" \
  --properties "$QCS_EDIT_I18N_PROPERTIES" \
  --output "$QCS_EDIT_I18N_OUTPUT" \
  --module-code QCS_MANU_INSPECT_EDIT

python3 "$SCRIPT_DIR/generate-module-i18n-js.py" \
  --properties "$QCS_PLAN_LIST_I18N_PROPERTIES" \
  --output "$QCS_PLAN_LIST_I18N_OUTPUT" \
  --module-code QCS_MANU_INSP_PLAN_LIST

python3 "$SCRIPT_DIR/generate-module-i18n-js.py" \
  --properties "$QCS_REPORT_VIEW_I18N_PROPERTIES" \
  --output "$QCS_REPORT_VIEW_I18N_OUTPUT" \
  --module-code QCS_MANU_INSP_REPORT_VIEW

echo "QCS static assets prepared: $QCS_STATIC_DEST"
echo "QCS list i18n compatibility prepared: $QCS_LIST_I18N_OUTPUT"
echo "QCS edit i18n compatibility prepared: $QCS_EDIT_I18N_OUTPUT"
echo "QCS inspection-plan i18n compatibility prepared: $QCS_PLAN_LIST_I18N_OUTPUT"
echo "QCS report view i18n compatibility prepared: $QCS_REPORT_VIEW_I18N_OUTPUT"
