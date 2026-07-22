#!/bin/sh
set -eu

STATE_DIR=${BPI_POLARIS_BOOTSTRAP_STATE_DIR:-/var/run/bpi-polaris-bootstrap}
STATE_FILE="$STATE_DIR/metastore-state"
TEMP_FILE="$STATE_FILE.tmp.$$"

required() {
    key=$1
    eval "value=\${$key:-}"
    if [ -z "$value" ]; then
        printf 'ERROR: %s is required\n' "$key" >&2
        exit 1
    fi
}

write_state() {
    state=$1
    mkdir -p "$STATE_DIR"
    printf '%s\n' "$state" >"$TEMP_FILE"
    chmod 0644 "$TEMP_FILE"
    mv "$TEMP_FILE" "$STATE_FILE"
}

trap 'rm -f "$TEMP_FILE"' EXIT HUP INT TERM

for key in PGHOST PGPORT PGDATABASE PGUSER PGPASSWORD BPI_POLARIS_REALM BPI_POLARIS_BOOTSTRAP_CLIENT_ID; do
    required "$key"
done

rm -f "$STATE_FILE"
auth_table=$(psql -X -A -t -q \
    -c "SELECT to_regclass('polaris_schema.principal_authentication_data')::text")

if [ -z "$auth_table" ]; then
    write_state BOOTSTRAP_REQUIRED
    printf 'BPI Polaris metastore bootstrap check: REQUIRED\n'
    exit 0
fi

realm_rows=$(psql -X -A -t -q -v realm="$BPI_POLARIS_REALM" <<'SQL'
SELECT count(*)
FROM polaris_schema.principal_authentication_data
WHERE realm_id = :'realm';
SQL
)
matching_rows=$(psql -X -A -t -q \
    -v realm="$BPI_POLARIS_REALM" \
    -v client_id="$BPI_POLARIS_BOOTSTRAP_CLIENT_ID" <<'SQL'
SELECT count(*)
FROM polaris_schema.principal_authentication_data
WHERE realm_id = :'realm'
  AND principal_client_id = :'client_id';
SQL
)

case "$realm_rows:$matching_rows" in
    0:0)
        write_state BOOTSTRAP_REQUIRED
        printf 'BPI Polaris metastore bootstrap check: REQUIRED\n'
        ;;
    *:1)
        write_state BOOTSTRAP_COMPLETE
        printf 'BPI Polaris metastore bootstrap check: COMPLETE\n'
        ;;
    *)
        printf 'ERROR: Polaris realm %s already exists with a different bootstrap client ID\n' \
            "$BPI_POLARIS_REALM" >&2
        exit 1
        ;;
esac
