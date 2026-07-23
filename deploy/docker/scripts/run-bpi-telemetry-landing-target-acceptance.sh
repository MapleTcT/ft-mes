#!/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
RELEASE_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../../.." && pwd)
RUNTIME_ROOT=${BPI_INTEGRATED_RUNTIME_ROOT:-}
IOT_ROOT=${BPI_IOT_PILOT_ROOT:-}
MARKER=${BPI_ACCEPTANCE_MARKER:-}
CONFIRM=${BPI_TELEMETRY_ACCEPTANCE_CONFIRM:-}
PLANT_ID=${BPI_PLANT_ID:-PLANT-01}
LINE_ID=${BPI_LINE_ID:-LINE-S07-01}
PRODUCT_ID=${BPI_PRODUCT_ID:-bpi-mqtt-pilot-product-01}
DEVICE_ID=${BPI_DEVICE_ID:-bpi-mqtt-pilot-device-01}
GATEWAY_ID=${BPI_GATEWAY_ID:-bpi-pilot-mqtt-gateway-01}
PROPERTY_ID=${BPI_PROPERTY_ID:-flow.instant}
CALIBRATION_VERSION=${BPI_CALIBRATION_VERSION:-pilot-telemetry-${MARKER}}
SOURCE_EPOCH=${BPI_MQTT_SOURCE_EPOCH:-$(date -u +%Y%m%d%H%M%S)}
PREHEAT_COUNT=${BPI_MQTT_PREHEAT_COUNT:-3}
WINDOW_COUNT=${BPI_MQTT_COUNT:-2}
WAIT_SECONDS=${BPI_TELEMETRY_ACCEPTANCE_WAIT_SECONDS:-240}
SOURCE_SEQUENCE_EVIDENCE_INTERVAL=${BPI_SOURCE_SEQUENCE_EVIDENCE_ACCEPTANCE_INTERVAL:-5s}
POINT_CATALOG_INTERVAL=${BPI_POINT_CATALOG_ACCEPTANCE_INTERVAL:-1h}
MQTT_PUBLISH_ATTEMPTS=${BPI_MQTT_PUBLISH_ATTEMPTS:-3}
MQTT_TIMEOUT_SECONDS=${BPI_MQTT_TIMEOUT_SECONDS:-20}
MQTT_SETTLE_SECONDS=${BPI_MQTT_SETTLE_SECONDS:-5}
EVIDENCE_DIR=${BPI_TELEMETRY_ACCEPTANCE_EVIDENCE_DIR:-}
POSTGRES_CONTAINER=${BPI_POSTGRES_CONTAINER:-adp-mes-newbase-postgres-1}
POSTGRES_USER=${BPI_POSTGRES_USER:-adp}
POSTGRES_DATABASE=${BPI_POSTGRES_DATABASE:-ft_mes_bpi}

if [ "$CONFIRM" != "RUN_CONTROLLED_TELEMETRY_LANDING_ACCEPTANCE" ]; then
    printf '%s\n' \
        'ERROR: set BPI_TELEMETRY_ACCEPTANCE_CONFIRM=RUN_CONTROLLED_TELEMETRY_LANDING_ACCEPTANCE' >&2
    exit 1
fi
case "$RUNTIME_ROOT" in
    /*) ;;
    *) printf 'ERROR: BPI_INTEGRATED_RUNTIME_ROOT must be absolute\n' >&2; exit 1 ;;
esac
case "$IOT_ROOT" in
    /*) ;;
    *) printf 'ERROR: BPI_IOT_PILOT_ROOT must be absolute\n' >&2; exit 1 ;;
esac
case "$MARKER" in
    ''|*[!A-Za-z0-9_-]*)
        printf 'ERROR: BPI_ACCEPTANCE_MARKER must use letters, digits, underscores or hyphens\n' >&2
        exit 1
        ;;
esac
if [ "${#MARKER}" -lt 8 ] || [ "${#MARKER}" -gt 80 ]; then
    printf 'ERROR: BPI_ACCEPTANCE_MARKER must contain 8-80 characters\n' >&2
    exit 1
fi
case "$CALIBRATION_VERSION" in
    ''|*[!A-Za-z0-9_.:-]*)
        printf 'ERROR: BPI_CALIBRATION_VERSION contains unsupported characters\n' >&2
        exit 1
        ;;
esac
for identifier in \
    "$PLANT_ID" "$LINE_ID" "$PRODUCT_ID" "$DEVICE_ID" "$GATEWAY_ID" "$PROPERTY_ID"; do
    case "$identifier" in
        ''|*[!A-Za-z0-9_.:-]*)
            printf 'ERROR: scope and point identifiers contain unsupported characters\n' >&2
            exit 1
            ;;
    esac
done
for value_name in \
    SOURCE_EPOCH PREHEAT_COUNT WINDOW_COUNT WAIT_SECONDS \
    MQTT_PUBLISH_ATTEMPTS MQTT_TIMEOUT_SECONDS MQTT_SETTLE_SECONDS; do
    eval "value=\${$value_name}"
    case "$value" in
        ''|*[!0-9]*|0)
            printf 'ERROR: %s must be a positive integer\n' "$value_name" >&2
            exit 1
            ;;
    esac
done
for duration_name in SOURCE_SEQUENCE_EVIDENCE_INTERVAL POINT_CATALOG_INTERVAL; do
    eval "duration_value=\${$duration_name}"
    if ! printf '%s\n' "$duration_value" \
        | grep -Eq '^[1-9][0-9]*(ms|s|m|h|d)$'; then
        printf 'ERROR: %s must be a positive duration\n' "$duration_name" >&2
        exit 1
    fi
done
test -n "${ADP_USERNAME:-}" || {
    printf 'ERROR: ADP_USERNAME is required\n' >&2
    exit 1
}
test -n "${ADP_PASSWORD:-}" || {
    printf 'ERROR: ADP_PASSWORD is required\n' >&2
    exit 1
}

RUNTIME_ENV="$RUNTIME_ROOT/deploy/docker/.env"
IOT_ENV="$IOT_ROOT/.env"
IOT_COMPOSE="$IOT_ROOT/docker-compose.yml"
IOT_APPLICATION_CONFIG="$IOT_ROOT/application-bpi-pilot.yml"
MAPPING="$IOT_ROOT/runtime/pilot-mapping.json"
MAPPING_PROPERTIES="$IOT_ROOT/runtime/application-bpi-mapping.properties"
RENDERER="$IOT_ROOT/scripts/render-pilot-mapping.py"
PUBLISHER="$IOT_ROOT/scripts/publish-pilot-mqtt-telemetry.py"
FIXTURE_SQL="$SCRIPT_DIR/bpi-telemetry-landing-acceptance-fixture.sql"
VERIFY_SQL="$SCRIPT_DIR/bpi-telemetry-landing-acceptance-verification.sql"
CLEANUP_SQL="$SCRIPT_DIR/bpi-telemetry-landing-acceptance-cleanup.sql"
BROWSER_SCRIPT="$SCRIPT_DIR/adp-bpi-telemetry-landing-acceptance.js"
PREHEAT_MARKER="${MARKER}_PREHEAT"
WINDOW_MARKER="${MARKER}_WINDOW"

for path in \
    "$RUNTIME_ENV" "$IOT_ENV" "$IOT_COMPOSE" "$IOT_APPLICATION_CONFIG" \
    "$MAPPING" "$MAPPING_PROPERTIES" \
    "$RENDERER" "$PUBLISHER" "$FIXTURE_SQL" "$VERIFY_SQL" "$CLEANUP_SQL" \
    "$BROWSER_SCRIPT"; do
    test -f "$path" || {
        printf 'ERROR: required acceptance path is missing: %s\n' "$path" >&2
        exit 1
    }
done
if ! grep -Fq \
    'source-sequence-evidence-publish-interval: ${BPI_SOURCE_SEQUENCE_EVIDENCE_PUBLISH_INTERVAL:10m}' \
    "$IOT_APPLICATION_CONFIG"; then
    printf '%s\n' \
        'ERROR: pilot application config does not bind BPI_SOURCE_SEQUENCE_EVIDENCE_PUBLISH_INTERVAL' >&2
    exit 1
fi
for command_name in docker node python3 sha256sum; do
    command -v "$command_name" >/dev/null 2>&1 || {
        printf 'ERROR: required command is unavailable: %s\n' "$command_name" >&2
        exit 1
    }
done

if [ -z "$EVIDENCE_DIR" ]; then
    EVIDENCE_DIR="$RUNTIME_ROOT/backups/bpi-telemetry-landing-$MARKER"
fi
case "$EVIDENCE_DIR" in
    /*) ;;
    *) printf 'ERROR: BPI_TELEMETRY_ACCEPTANCE_EVIDENCE_DIR must be absolute\n' >&2; exit 1 ;;
esac
mkdir -p "$EVIDENCE_DIR"
chmod 700 "$EVIDENCE_DIR"
MAPPING_BACKUP="$EVIDENCE_DIR/pilot-mapping-before.json"
PROPERTIES_BACKUP="$EVIDENCE_DIR/application-bpi-mapping-before.properties"
IOT_ENV_BACKUP="$EVIDENCE_DIR/.iot-env-before"

env_value() {
    key=$1
    file=$2
    awk -v key="$key" '
        index($0, key "=") == 1 {
            value = substr($0, length(key) + 2)
        }
        END {
            print value
        }
    ' "$file"
}

ORIGINAL_SOURCE_SEQUENCE_EVIDENCE_INTERVAL=$(
    env_value BPI_SOURCE_SEQUENCE_EVIDENCE_PUBLISH_INTERVAL "$IOT_ENV"
)
ORIGINAL_POINT_CATALOG_INTERVAL=$(
    env_value BPI_POINT_CATALOG_INTERVAL "$IOT_ENV"
)
test -n "$ORIGINAL_SOURCE_SEQUENCE_EVIDENCE_INTERVAL" || {
    printf 'ERROR: original source-sequence evidence interval is missing\n' >&2
    exit 1
}
test -n "$ORIGINAL_POINT_CATALOG_INTERVAL" || {
    printf 'ERROR: original point-catalog interval is missing\n' >&2
    exit 1
}

cp "$MAPPING" "$MAPPING_BACKUP"
cp "$MAPPING_PROPERTIES" "$PROPERTIES_BACKUP"
cp -p "$IOT_ENV" "$IOT_ENV_BACKUP"
chmod 600 "$MAPPING_BACKUP" "$PROPERTIES_BACKUP" "$IOT_ENV_BACKUP"

ORIGINAL_CALIBRATION_VERSION=$(python3 - "$MAPPING_BACKUP" "$DEVICE_ID" "$PROPERTY_ID" <<'PY'
import json
import sys

mapping = json.load(open(sys.argv[1], encoding="utf-8"))
matches = [
    point
    for device in mapping["devices"]
    if device["deviceId"] == sys.argv[2]
    for point in device["points"]
    if point["targetPropertyId"] == sys.argv[3]
]
if len(matches) != 1:
    raise SystemExit("controlled mapping does not contain exactly one target point")
print(matches[0]["calibrationVersion"])
PY
)

iot_compose() {
    docker compose --env-file "$IOT_ENV" -f "$IOT_COMPOSE" "$@"
}

postgres() {
    docker exec -i "$POSTGRES_CONTAINER" \
        psql -X -U "$POSTGRES_USER" -d "$POSTGRES_DATABASE" "$@"
}

wait_for_jetlinks() {
    deadline=$(( $(date +%s) + WAIT_SECONDS ))
    while [ "$(date +%s)" -lt "$deadline" ]; do
        container_id=$(iot_compose ps -q jetlinks)
        if [ -n "$container_id" ]; then
            state=$(docker inspect --format \
                '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' \
                "$container_id" 2>/dev/null || true)
            if [ "$state" = healthy ]; then
                return 0
            fi
        fi
        sleep 2
    done
    printf 'ERROR: JetLinks did not become healthy within %s seconds\n' "$WAIT_SECONDS" >&2
    iot_compose logs --tail 160 jetlinks >&2 || true
    return 1
}

capture_runtime_intervals() {
    output_path=$1
    expected_source_sequence_interval=$2
    expected_point_catalog_interval=$3
    container_id=$(iot_compose ps -q jetlinks)
    test -n "$container_id" || {
        printf 'ERROR: JetLinks container is missing while verifying runtime intervals\n' >&2
        return 1
    }
    docker inspect --format '{{range .Config.Env}}{{println .}}{{end}}' "$container_id" \
        | grep -E '^BPI_(SOURCE_SEQUENCE_EVIDENCE_PUBLISH_INTERVAL|POINT_CATALOG_INTERVAL)=' \
        >"$output_path"
    if ! grep -Fxq \
        "BPI_SOURCE_SEQUENCE_EVIDENCE_PUBLISH_INTERVAL=$expected_source_sequence_interval" \
        "$output_path"; then
        printf 'ERROR: JetLinks runtime source-sequence interval is not %s\n' \
            "$expected_source_sequence_interval" >&2
        return 1
    fi
    if ! grep -Fxq \
        "BPI_POINT_CATALOG_INTERVAL=$expected_point_catalog_interval" \
        "$output_path"; then
        printf 'ERROR: JetLinks runtime point-catalog interval is not %s\n' \
            "$expected_point_catalog_interval" >&2
        return 1
    fi
}

wait_for_catalog_version() {
    expected=$1
    deadline=$(( $(date +%s) + WAIT_SECONDS ))
    while [ "$(date +%s)" -lt "$deadline" ]; do
        current=$(postgres -Atc "
            WITH snapshot AS (
                SELECT *
                  FROM bpi.bpi_point_catalog_snapshots
                 WHERE tenant_id = '1000'
                   AND plant_id = '$PLANT_ID'
                   AND line_id = '$LINE_ID'
                 ORDER BY observed_at DESC, imported_at DESC, id
                 LIMIT 1
            )
            SELECT COALESCE(entry.calibration_version, '')
                   || '|' ||
                   (
                       entry.registered
                       AND entry.property_present
                       AND entry.device_state = 'ACTIVE'
                       AND entry.source_sequence_enabled
                       AND entry.source_sequence_required
                       AND entry.source_sequence_origin IN ('DEVICE', 'GATEWAY')
                       AND entry.source_sequence_binding_fingerprint IS NOT NULL
                   )::text
              FROM snapshot
              JOIN bpi.bpi_point_catalog_entries entry
                ON entry.tenant_id = snapshot.tenant_id
               AND entry.snapshot_id = snapshot.id
             WHERE entry.product_id = '$PRODUCT_ID'
               AND entry.device_id = '$DEVICE_ID'
               AND entry.property_id = '$PROPERTY_ID';
        " 2>/dev/null || true)
        if [ "$current" = "$expected|true" ]; then
            return 0
        fi
        sleep 2
    done
    printf 'ERROR: latest point catalog did not reach calibration/identity readiness %s|true (last=%s)\n' \
        "$expected" "${current:-missing}" >&2
    return 1
}

wait_for_source_sequence() {
    deadline=$(( $(date +%s) + WAIT_SECONDS ))
    while [ "$(date +%s)" -lt "$deadline" ]; do
        qualified=$(postgres -Atc "
            WITH snapshot AS (
                SELECT *
                  FROM bpi.bpi_point_catalog_snapshots
                 WHERE tenant_id = '1000'
                   AND plant_id = '$PLANT_ID'
                   AND line_id = '$LINE_ID'
                 ORDER BY observed_at DESC, imported_at DESC, id
                 LIMIT 1
            ), point AS (
                SELECT entry.*
                  FROM snapshot
                  JOIN bpi.bpi_point_catalog_entries entry
                    ON entry.tenant_id = snapshot.tenant_id
                   AND entry.snapshot_id = snapshot.id
                 WHERE entry.product_id = '$PRODUCT_ID'
                   AND entry.device_id = '$DEVICE_ID'
                   AND entry.property_id = '$PROPERTY_ID'
            )
            SELECT EXISTS (
                SELECT 1
                  FROM snapshot
                  JOIN point ON true
                  JOIN bpi.bpi_source_sequence_evidence_current evidence
                    ON evidence.tenant_id = point.tenant_id
                   AND evidence.source = snapshot.source
                   AND evidence.source_instance = snapshot.source_instance
                   AND evidence.plant_id = point.plant_id
                   AND evidence.line_id = point.line_id
                   AND evidence.product_id = point.product_id
                   AND evidence.device_id = point.device_id
                   AND evidence.binding_fingerprint = point.source_sequence_binding_fingerprint
                   AND evidence.status = 'QUALIFIED'
                   AND evidence.sequence_origin = point.source_sequence_origin
                   AND evidence.source_epoch = $SOURCE_EPOCH
                   AND evidence.last_sequence >= $PREHEAT_COUNT
                   AND evidence.observation_count >= $PREHEAT_COUNT
                   AND evidence.observed_at >= snapshot.observed_at
                   AND evidence.valid_until > now()
            );
        " 2>/dev/null || true)
        if [ "$qualified" = t ]; then
            return 0
        fi
        sleep 2
    done
    printf 'ERROR: source sequence evidence did not become QUALIFIED\n' >&2
    return 1
}

restore_iot_state() {
    if [ "$mapping_modified" = true ]; then
        cp "$MAPPING_BACKUP" "$MAPPING"
        cp "$PROPERTIES_BACKUP" "$MAPPING_PROPERTIES"
        chmod 600 "$MAPPING_PROPERTIES"
    fi
    if [ "$iot_env_modified" = true ]; then
        cp -p "$IOT_ENV_BACKUP" "$IOT_ENV"
    fi
    unset BPI_SOURCE_SEQUENCE_EVIDENCE_PUBLISH_INTERVAL
    unset BPI_POINT_CATALOG_INTERVAL
    iot_compose up -d --no-deps --force-recreate jetlinks >/dev/null
    wait_for_jetlinks
    capture_runtime_intervals \
        "$EVIDENCE_DIR/restored-runtime-env.txt" \
        "$ORIGINAL_SOURCE_SEQUENCE_EVIDENCE_INTERVAL" \
        "$ORIGINAL_POINT_CATALOG_INTERVAL"
    if [ "$mapping_modified" = true ]; then
        wait_for_catalog_version "$ORIGINAL_CALIBRATION_VERSION"
    fi
}

fixture_applied=false
mapping_modified=false
iot_env_modified=false
cleanup_fixture() {
    output_path=$1
    postgres \
        -v "marker=$MARKER" \
        -v "plant_id=$PLANT_ID" \
        -v "line_id=$LINE_ID" \
        -v "product_id=$PRODUCT_ID" \
        -v "device_id=$DEVICE_ID" \
        -v "gateway_id=$GATEWAY_ID" \
        -v "property_id=$PROPERTY_ID" \
        -v "calibration_version=$CALIBRATION_VERSION" \
        -v "preheat_marker=$PREHEAT_MARKER" \
        -v "window_marker=$WINDOW_MARKER" \
        <"$CLEANUP_SQL" >"$output_path"
}

cleanup() {
    exit_code=$?
    trap - EXIT HUP INT TERM
    if [ "$fixture_applied" = true ]; then
        cleanup_fixture "$EVIDENCE_DIR/cleanup-pre-restore.json.txt" || exit_code=1
    fi
    if [ "$mapping_modified" = true ] || [ "$iot_env_modified" = true ]; then
        restore_iot_state || exit_code=1
    fi
    if [ "$fixture_applied" = true ]; then
        cleanup_fixture "$EVIDENCE_DIR/cleanup.json.txt" || exit_code=1
    fi
    rm -f "$IOT_ENV_BACKUP"
    exit "$exit_code"
}
trap cleanup EXIT HUP INT TERM

iot_env_modified=true
python3 - \
    "$IOT_ENV" \
    "$SOURCE_SEQUENCE_EVIDENCE_INTERVAL" \
    "$POINT_CATALOG_INTERVAL" <<'PY'
import os
import sys
import tempfile
from pathlib import Path

path = Path(sys.argv[1])
overrides = {
    "BPI_SOURCE_SEQUENCE_EVIDENCE_PUBLISH_INTERVAL": sys.argv[2],
    "BPI_POINT_CATALOG_INTERVAL": sys.argv[3],
}
lines = path.read_text(encoding="utf-8").splitlines()
rendered = []
replaced = set()
for line in lines:
    key = line.partition("=")[0]
    if key in overrides:
        rendered.append(f"{key}={overrides[key]}")
        replaced.add(key)
    else:
        rendered.append(line)
for key, value in overrides.items():
    if key not in replaced:
        rendered.append(f"{key}={value}")
mode = path.stat().st_mode & 0o777
with tempfile.NamedTemporaryFile(
    "w", encoding="utf-8", dir=path.parent, delete=False
) as handle:
    handle.write("\n".join(rendered))
    handle.write("\n")
    temporary = Path(handle.name)
os.chmod(temporary, mode)
os.replace(temporary, path)
PY
printf 'BPI_SOURCE_SEQUENCE_EVIDENCE_PUBLISH_INTERVAL=%s\n' \
    "$SOURCE_SEQUENCE_EVIDENCE_INTERVAL" \
    >"$EVIDENCE_DIR/acceptance-runtime-overrides.txt"
printf 'BPI_POINT_CATALOG_INTERVAL=%s\n' \
    "$POINT_CATALOG_INTERVAL" \
    >>"$EVIDENCE_DIR/acceptance-runtime-overrides.txt"

mapping_modified=true
python3 - "$MAPPING" "$DEVICE_ID" "$PROPERTY_ID" "$CALIBRATION_VERSION" <<'PY'
import json
import os
import sys
import tempfile
from pathlib import Path

path = Path(sys.argv[1])
mapping = json.loads(path.read_text(encoding="utf-8"))
matches = [
    point
    for device in mapping["devices"]
    if device["deviceId"] == sys.argv[2]
    for point in device["points"]
    if point["targetPropertyId"] == sys.argv[3]
]
if len(matches) != 1:
    raise SystemExit("controlled mapping does not contain exactly one target point")
matches[0]["calibrationVersion"] = sys.argv[4]
matches[0]["calibrationVerified"] = True
matches[0]["defaultQuality"] = "GOOD"
with tempfile.NamedTemporaryFile(
    "w", encoding="utf-8", dir=path.parent, delete=False
) as handle:
    json.dump(mapping, handle, ensure_ascii=False, indent=2)
    handle.write("\n")
    temporary = Path(handle.name)
os.chmod(temporary, 0o600)
os.replace(temporary, path)
PY
python3 "$RENDERER" "$MAPPING" "$MAPPING_PROPERTIES"
iot_compose up -d --no-deps --force-recreate jetlinks
wait_for_jetlinks
capture_runtime_intervals \
    "$EVIDENCE_DIR/active-runtime-env.txt" \
    "$SOURCE_SEQUENCE_EVIDENCE_INTERVAL" \
    "$POINT_CATALOG_INTERVAL"
wait_for_catalog_version "$CALIBRATION_VERSION"

postgres \
    -v "marker=$MARKER" \
    -v "plant_id=$PLANT_ID" \
    -v "line_id=$LINE_ID" \
    -v "product_id=$PRODUCT_ID" \
    -v "device_id=$DEVICE_ID" \
    -v "property_id=$PROPERTY_ID" \
    -v "calibration_version=$CALIBRATION_VERSION" \
    <"$FIXTURE_SQL" >"$EVIDENCE_DIR/fixture.json.txt"
fixture_applied=true

set -a
. "$IOT_ENV"
set +a
sleep "$MQTT_SETTLE_SECONDS"
PREHEAT_ATTEMPT_SUMMARY="$EVIDENCE_DIR/preheat-publish-attempts.txt"
: >"$PREHEAT_ATTEMPT_SUMMARY"
publish_attempt=1
publish_success=false
while [ "$publish_attempt" -le "$MQTT_PUBLISH_ATTEMPTS" ]; do
    attempt_source_epoch=$(( SOURCE_EPOCH + publish_attempt - 1 ))
    attempt_log="$EVIDENCE_DIR/preheat-mqtt-attempt-$publish_attempt.log"
    if python3 "$PUBLISHER" \
        --mapping "$MAPPING" \
        --marker "$PREHEAT_MARKER" \
        --source-epoch "$attempt_source_epoch" \
        --start-sequence 1 \
        --count "$PREHEAT_COUNT" \
        --quality GOOD \
        --value 12.5 \
        --timeout-seconds "$MQTT_TIMEOUT_SECONDS" \
        --output "$EVIDENCE_DIR/preheat-mqtt.json" \
        >"$attempt_log" 2>&1; then
        SOURCE_EPOCH=$attempt_source_epoch
        publish_success=true
        printf 'attempt=%s sourceEpoch=%s result=PASS\n' \
            "$publish_attempt" "$SOURCE_EPOCH" >>"$PREHEAT_ATTEMPT_SUMMARY"
        cat "$attempt_log"
        break
    fi
    printf 'attempt=%s sourceEpoch=%s result=RETRY\n' \
        "$publish_attempt" "$attempt_source_epoch" >>"$PREHEAT_ATTEMPT_SUMMARY"
    cat "$attempt_log" >&2
    publish_attempt=$(( publish_attempt + 1 ))
    if [ "$publish_attempt" -le "$MQTT_PUBLISH_ATTEMPTS" ]; then
        sleep "$MQTT_SETTLE_SECONDS"
    fi
done
if [ "$publish_success" != true ]; then
    printf 'ERROR: controlled MQTT preheat failed after %s attempts\n' \
        "$MQTT_PUBLISH_ATTEMPTS" >&2
    exit 1
fi
wait_for_source_sequence

export BPI_ACCEPTANCE_MARKER="$MARKER"
export BPI_PLANT_ID="$PLANT_ID"
export BPI_LINE_ID="$LINE_ID"
export BPI_PRODUCT_ID="$PRODUCT_ID"
export BPI_DEVICE_ID="$DEVICE_ID"
export BPI_PROPERTY_ID="$PROPERTY_ID"
export BPI_CALIBRATION_VERSION="$CALIBRATION_VERSION"
export BPI_MQTT_WORKDIR="$IOT_ROOT"
export BPI_MQTT_PUBLISH_SCRIPT="$PUBLISHER"
export BPI_MQTT_MAPPING_PATH="$MAPPING"
export BPI_MQTT_SOURCE_EPOCH="$SOURCE_EPOCH"
export BPI_MQTT_START_SEQUENCE=$(( PREHEAT_COUNT + 1 ))
export BPI_MQTT_COUNT="$WINDOW_COUNT"
export BPI_POSTGRES_CONTAINER="$POSTGRES_CONTAINER"
export BPI_POSTGRES_USER="$POSTGRES_USER"
export BPI_POSTGRES_DATABASE="$POSTGRES_DATABASE"
export ADP_BASE_URL=${ADP_BASE_URL:-http://10.11.100.17:18080}
export BPI_BROWSER_BASE_URL=${BPI_BROWSER_BASE_URL:-http://10.11.100.17:18080/bpi/}
export BPI_BROWSER_TIMEOUT_MS=$(( WAIT_SECONDS * 1000 ))
export BPI_BROWSER_REPORT="$EVIDENCE_DIR/browser-acceptance.json"
export BPI_MQTT_REPORT="$EVIDENCE_DIR/window-mqtt.json"
export BPI_DESKTOP_SCREENSHOT="$EVIDENCE_DIR/telemetry-landing-desktop.png"
export BPI_MOBILE_SCREENSHOT="$EVIDENCE_DIR/telemetry-landing-mobile.png"
export BPI_LIVE_OVERVIEW_SCREENSHOT="$EVIDENCE_DIR/live-operations-overview.png"
export BPI_LIVE_DRAWER_SCREENSHOT="$EVIDENCE_DIR/live-operations-drawer.png"
export NODE_PATH=${NODE_PATH:-$RELEASE_ROOT/frontend/apps/bpi/node_modules}
node "$BROWSER_SCRIPT"

postgres \
    -v "marker=$MARKER" \
    -v "preheat_marker=$PREHEAT_MARKER" \
    -v "window_marker=$WINDOW_MARKER" \
    <"$VERIFY_SQL" >"$EVIDENCE_DIR/postgres-verification.json.txt"

sha256sum \
    "$EVIDENCE_DIR/browser-acceptance.json" \
    "$EVIDENCE_DIR/preheat-mqtt.json" \
    "$EVIDENCE_DIR/window-mqtt.json" \
    "$EVIDENCE_DIR/postgres-verification.json.txt" \
    "$EVIDENCE_DIR/acceptance-runtime-overrides.txt" \
    "$EVIDENCE_DIR/active-runtime-env.txt" \
    "$EVIDENCE_DIR/preheat-publish-attempts.txt" \
    "$EVIDENCE_DIR/telemetry-landing-desktop.png" \
    "$EVIDENCE_DIR/telemetry-landing-mobile.png" \
    "$EVIDENCE_DIR/live-operations-overview.png" \
    "$EVIDENCE_DIR/live-operations-drawer.png" \
    >"$EVIDENCE_DIR/SHA256SUMS"

printf 'BPI controlled telemetry landing acceptance PASS: %s\n' "$EVIDENCE_DIR"
