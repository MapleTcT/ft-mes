#!/bin/sh
set -eu

STATE_FILE=${BPI_POLARIS_BOOTSTRAP_STATE_FILE:-/var/run/bpi-polaris-bootstrap/metastore-state}

test -r "$STATE_FILE" || {
    printf 'ERROR: Polaris metastore bootstrap state is unavailable: %s\n' "$STATE_FILE" >&2
    exit 1
}

state=$(cat "$STATE_FILE")
case "$state" in
    BOOTSTRAP_COMPLETE)
        printf 'BPI Polaris metastore is already bootstrapped; admin bootstrap skipped\n'
        ;;
    BOOTSTRAP_REQUIRED)
        realm=${BPI_POLARIS_REALM:-}
        client_id=${BPI_POLARIS_BOOTSTRAP_CLIENT_ID:-}
        client_secret=${BPI_POLARIS_BOOTSTRAP_CLIENT_SECRET:-}
        for required_value in "$realm" "$client_id" "$client_secret"; do
            test -n "$required_value" || {
                printf 'ERROR: Polaris bootstrap credentials are incomplete\n' >&2
                exit 1
            }
            case "$required_value" in
                *','*)
                    printf 'ERROR: Polaris bootstrap credentials contain an unsupported delimiter\n' >&2
                    exit 1
                    ;;
            esac
        done
        exec java -jar /deployments/polaris-admin-tool.jar "$@" \
            "--realm=$realm" "--credential=$realm,$client_id,$client_secret"
        ;;
    *)
        printf 'ERROR: unsupported Polaris metastore bootstrap state: %s\n' "$state" >&2
        exit 1
        ;;
esac
