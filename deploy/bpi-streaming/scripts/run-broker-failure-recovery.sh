#!/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DEPLOY_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
ENV_FILE=${1:-$DEPLOY_DIR/.env}
BROKER_SERVICE=${BPI_CHAOS_BROKER_SERVICE:-kafka-2}
CHAOS_TOPIC=${BPI_CHAOS_TOPIC:-bpi.acceptance.broker-chaos.v1}
TIMEOUT=${BPI_BROKER_CHAOS_TIMEOUT_SECONDS:-360}
MARKER=${BPI_BROKER_CHAOS_MARKER:-ADP_BPI_BROKER_CHAOS_$(date -u +%Y%m%d_%H%M%S)}
REPORT=${BPI_BROKER_CHAOS_REPORT:-/tmp/$MARKER.json}

case $BROKER_SERVICE in
    kafka-1) SURVIVOR_SERVICE=kafka-2 ;;
    kafka-2|kafka-3) SURVIVOR_SERVICE=kafka-1 ;;
    *) printf 'ERROR: unsupported broker service: %s\n' "$BROKER_SERVICE" >&2; exit 1 ;;
esac

if [ ! -f "$ENV_FILE" ]; then
    printf 'ERROR: BPI deployment env file not found: %s\n' "$ENV_FILE" >&2
    exit 1
fi
case $MARKER in
    *[!A-Za-z0-9_-]*|'') printf 'ERROR: invalid chaos marker\n' >&2; exit 1 ;;
esac

load_env_file() {
    while IFS= read -r line || [ -n "$line" ]; do
        line=$(printf '%s' "$line" | tr -d '\r')
        case $line in
            ""|\#*) continue ;;
            *=*) key=${line%%=*}; value=${line#*=} ;;
            *) printf 'ERROR: invalid env line without equals sign\n' >&2; exit 1 ;;
        esac
        case $key in
            ""|*[!A-Za-z0-9_]*) printf 'ERROR: invalid env key: %s\n' "$key" >&2; exit 1 ;;
        esac
        export "$key=$value"
    done <"$ENV_FILE"
}

load_env_file

compose() {
    docker compose --env-file "$ENV_FILE" -f "$DEPLOY_DIR/docker-compose.yml" "$@"
}

REST_URL="http://${BPI_BIND_ADDRESS:-127.0.0.1}:${BPI_FLINK_REST_PORT:-18081}"
BASELINE_DESCRIBE=$(mktemp)
OUTAGE_DESCRIBE=$(mktemp)
RECOVERED_DESCRIBE=$(mktemp)
CONSUMED=$(mktemp)
BROKER_STOPPED=false

container_id() {
    compose ps -q --all "$1"
}

wait_healthy() {
    service=$1
    deadline=$(( $(date +%s) + TIMEOUT ))
    while [ "$(date +%s)" -lt "$deadline" ]; do
        id=$(container_id "$service")
        if [ -n "$id" ]; then
            health=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$id")
            [ "$health" = healthy ] && return 0
        fi
        sleep 5
    done
    printf 'ERROR: service did not become healthy: %s\n' "$service" >&2
    return 1
}

cleanup() {
    status=$?
    trap - EXIT HUP INT TERM
    if [ "$BROKER_STOPPED" = true ]; then
        printf 'Recovery guard: starting %s before exit\n' "$BROKER_SERVICE" >&2
        compose up -d "$BROKER_SERVICE" >/dev/null 2>&1 || true
        wait_healthy "$BROKER_SERVICE" >/dev/null 2>&1 || true
    fi
    rm -f "$BASELINE_DESCRIBE" "$OUTAGE_DESCRIBE" "$RECOVERED_DESCRIBE" "$CONSUMED"
    exit "$status"
}
trap cleanup EXIT
trap 'exit 129' HUP
trap 'exit 130' INT
trap 'exit 143' TERM

require_healthy() {
    service=$1
    id=$(container_id "$service")
    if [ -z "$id" ]; then
        printf 'ERROR: service has no container: %s\n' "$service" >&2
        return 1
    fi
    health=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$id")
    if [ "$health" != healthy ]; then
        printf 'ERROR: service is not healthy: %s (%s)\n' "$service" "$health" >&2
        return 1
    fi
}

job_id() {
    curl -fsS "$REST_URL/jobs/overview" | python3 -c '
import json, sys
jobs = json.load(sys.stdin).get("jobs", [])
running = [j for j in jobs if j.get("name") == "ft-mes-bpi-batch-boundary-v1" and j.get("state") == "RUNNING"]
print(running[0].get("jid", "") if len(running) == 1 else "")
'
}

job_snapshot() {
    current_job=$1
    checkpoints=$(curl -fsS "$REST_URL/jobs/$current_job/checkpoints")
    printf '%s' "$checkpoints" | python3 -c '
import json, sys
payload = json.load(sys.stdin)
latest = payload.get("latest", {}).get("completed") or {}
counts = payload.get("counts", {})
print("%s|%s|%s" % (latest.get("id", ""), counts.get("completed", 0), counts.get("failed", 0)))
'
}

wait_checkpoint_after() {
    expected_job=$1
    previous_checkpoint=$2
    deadline=$(( $(date +%s) + TIMEOUT ))
    while [ "$(date +%s)" -lt "$deadline" ]; do
        jobs=$(curl -fsS "$REST_URL/jobs/overview" 2>/dev/null || true)
        state=$(printf '%s' "$jobs" | python3 -c '
import json, sys
try:
    jobs = json.load(sys.stdin).get("jobs", [])
except Exception:
    jobs = []
match = [j for j in jobs if j.get("jid") == sys.argv[1]]
print(match[0].get("state", "MISSING") if len(match) == 1 else "MISSING")
' "$expected_job" 2>/dev/null || true)
        if [ "$state" != RUNNING ]; then
            printf 'ERROR: Flink job left RUNNING during broker exercise: %s\n' "$state" >&2
            return 1
        fi
        snapshot=$(job_snapshot "$expected_job")
        checkpoint=${snapshot%%|*}
        case $checkpoint in
            ''|*[!0-9]*) ;;
            *) [ "$checkpoint" -gt "$previous_checkpoint" ] && { printf '%s\n' "$snapshot"; return 0; } ;;
        esac
        sleep 5
    done
    printf 'ERROR: Flink checkpoint did not advance after %s within %s seconds\n' \
        "$previous_checkpoint" "$TIMEOUT" >&2
    return 1
}

describe_cluster() {
    output=$1
    compose exec -T "$SURVIVOR_SERVICE" /opt/kafka/bin/kafka-topics.sh \
        --bootstrap-server "$SURVIVOR_SERVICE:19092" --describe </dev/null >"$output"
}

partition_summary() {
    python3 - "$1" <<'PY'
import re
import sys
from pathlib import Path

rows = [line for line in Path(sys.argv[1]).read_text(encoding="utf-8").splitlines() if "Partition:" in line]
unavailable = 0
below_two = 0
below_three = 0
for row in rows:
    leader = re.search(r"Leader:\s*(-?\d+)", row)
    isr = re.search(r"Isr:\s*([0-9,]*)", row)
    members = [value for value in (isr.group(1).split(",") if isr else []) if value]
    unavailable += int(not leader or leader.group(1) == "-1")
    below_two += int(len(members) < 2)
    below_three += int(len(members) < 3)
print(f"{len(rows)}|{unavailable}|{below_two}|{below_three}")
PY
}

topic_offset_sum() {
    compose exec -T "$SURVIVOR_SERVICE" /opt/kafka/bin/kafka-get-offsets.sh \
        --bootstrap-server "$SURVIVOR_SERVICE:19092" --topic "$CHAOS_TOPIC" </dev/null \
        | awk -F: '{sum += $3} END {print sum + 0}'
}

for service in kafka-1 kafka-2 kafka-3; do
    require_healthy "$service"
done

compose exec -T "$SURVIVOR_SERVICE" /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server "$SURVIVOR_SERVICE:19092" \
    --create --if-not-exists --topic "$CHAOS_TOPIC" --partitions 3 --replication-factor 3 \
    --config min.insync.replicas=2 --config unclean.leader.election.enable=false \
    --config retention.ms=86400000 </dev/null >/dev/null

describe_cluster "$BASELINE_DESCRIBE"
BASELINE_PARTITIONS=$(partition_summary "$BASELINE_DESCRIBE")
IFS='|' read -r BASELINE_COUNT BASELINE_UNAVAILABLE BASELINE_BELOW_TWO BASELINE_BELOW_THREE <<EOF
$BASELINE_PARTITIONS
EOF
if [ "$BASELINE_COUNT" -lt 1 ] || [ "$BASELINE_UNAVAILABLE" -ne 0 ] \
    || [ "$BASELINE_BELOW_TWO" -ne 0 ] || [ "$BASELINE_BELOW_THREE" -ne 0 ]; then
    printf 'ERROR: Kafka topics are not fully replicated before exercise\n' >&2
    exit 1
fi

JOB_ID=$(job_id)
if [ -z "$JOB_ID" ]; then
    printf 'ERROR: expected Flink job is not uniquely RUNNING\n' >&2
    exit 1
fi
BASELINE_JOB=$(job_snapshot "$JOB_ID")
BASELINE_CHECKPOINT=${BASELINE_JOB%%|*}
BASELINE_REMAINDER=${BASELINE_JOB#*|}
BASELINE_COMPLETED=${BASELINE_REMAINDER%%|*}
BASELINE_FAILED=${BASELINE_REMAINDER#*|}
case $BASELINE_CHECKPOINT in
    ''|*[!0-9]*) printf 'ERROR: baseline checkpoint is missing\n' >&2; exit 1 ;;
esac

OFFSET_BEFORE=$(topic_offset_sum)
STARTED_AT=$(date -u +%Y-%m-%dT%H:%M:%SZ)
START_EPOCH=$(date +%s)
compose stop "$BROKER_SERVICE" >/dev/null
BROKER_STOPPED=true
stopped_id=$(container_id "$BROKER_SERVICE")
if [ -z "$stopped_id" ] || [ "$(docker inspect -f '{{.State.Running}}' "$stopped_id")" != false ]; then
    printf 'ERROR: broker did not stop: %s\n' "$BROKER_SERVICE" >&2
    exit 1
fi
require_healthy "$SURVIVOR_SERVICE"

deadline=$(( $(date +%s) + TIMEOUT ))
OUTAGE_COUNT=0
OUTAGE_UNAVAILABLE=0
OUTAGE_BELOW_TWO=0
OUTAGE_BELOW_THREE=0
while [ "$(date +%s)" -lt "$deadline" ]; do
    if describe_cluster "$OUTAGE_DESCRIBE"; then
        OUTAGE_PARTITIONS=$(partition_summary "$OUTAGE_DESCRIBE")
        IFS='|' read -r OUTAGE_COUNT OUTAGE_UNAVAILABLE OUTAGE_BELOW_TWO OUTAGE_BELOW_THREE <<EOF
$OUTAGE_PARTITIONS
EOF
        if [ "$OUTAGE_UNAVAILABLE" -ne 0 ] || [ "$OUTAGE_BELOW_TWO" -ne 0 ]; then
            printf 'ERROR: partition availability or minISR was lost during broker outage\n' >&2
            exit 1
        fi
        [ "$OUTAGE_COUNT" -eq "$BASELINE_COUNT" ] \
            && [ "$OUTAGE_BELOW_THREE" -eq "$OUTAGE_COUNT" ] \
            && break
    fi
    sleep 5
done
if [ "$OUTAGE_COUNT" -ne "$BASELINE_COUNT" ] || [ "$OUTAGE_UNAVAILABLE" -ne 0 ] \
    || [ "$OUTAGE_BELOW_TWO" -ne 0 ] || [ "$OUTAGE_BELOW_THREE" -ne "$OUTAGE_COUNT" ]; then
    printf 'ERROR: one-broker outage did not retain exactly two ISR replicas for every partition\n' >&2
    exit 1
fi

printf '%s|{"marker":"%s","phase":"single-broker-down"}\n' "$MARKER" "$MARKER" \
    | compose exec -T "$SURVIVOR_SERVICE" /opt/kafka/bin/kafka-console-producer.sh \
        --bootstrap-server "$SURVIVOR_SERVICE:19092" --topic "$CHAOS_TOPIC" \
        --property parse.key=true --property key.separator='|' --producer-property acks=all \
        >/dev/null
OFFSET_DURING=$(topic_offset_sum)
if [ $((OFFSET_DURING - OFFSET_BEFORE)) -ne 1 ]; then
    printf 'ERROR: chaos marker did not advance topic offsets exactly once\n' >&2
    exit 1
fi

compose exec -T "$SURVIVOR_SERVICE" /opt/kafka/bin/kafka-console-consumer.sh \
    --bootstrap-server "$SURVIVOR_SERVICE:19092" --topic "$CHAOS_TOPIC" \
    --from-beginning --timeout-ms 10000 --property print.key=true --property key.separator='|' \
    </dev/null >"$CONSUMED" 2>/dev/null || true
MARKER_COUNT=$(grep -c "^$MARKER|" "$CONSUMED" || true)
if [ "$MARKER_COUNT" -ne 1 ]; then
    printf 'ERROR: chaos marker was not consumed exactly once during outage\n' >&2
    exit 1
fi

OUTAGE_JOB=$(wait_checkpoint_after "$JOB_ID" "$BASELINE_CHECKPOINT")
OUTAGE_CHECKPOINT=${OUTAGE_JOB%%|*}
OUTAGE_REMAINDER=${OUTAGE_JOB#*|}
OUTAGE_COMPLETED=${OUTAGE_REMAINDER%%|*}
OUTAGE_FAILED=${OUTAGE_REMAINDER#*|}
if [ "$OUTAGE_FAILED" -ne "$BASELINE_FAILED" ]; then
    printf 'ERROR: failed checkpoint count increased during broker outage\n' >&2
    exit 1
fi

compose up -d "$BROKER_SERVICE" >/dev/null
wait_healthy "$BROKER_SERVICE"
BROKER_STOPPED=false

deadline=$(( $(date +%s) + TIMEOUT ))
RECOVERED_COUNT=0
RECOVERED_UNAVAILABLE=1
RECOVERED_BELOW_TWO=1
RECOVERED_BELOW_THREE=1
while [ "$(date +%s)" -lt "$deadline" ]; do
    if describe_cluster "$RECOVERED_DESCRIBE"; then
        RECOVERED_PARTITIONS=$(partition_summary "$RECOVERED_DESCRIBE")
        IFS='|' read -r RECOVERED_COUNT RECOVERED_UNAVAILABLE RECOVERED_BELOW_TWO RECOVERED_BELOW_THREE <<EOF
$RECOVERED_PARTITIONS
EOF
        if [ "$RECOVERED_COUNT" -eq "$BASELINE_COUNT" ] && [ "$RECOVERED_UNAVAILABLE" -eq 0 ] \
            && [ "$RECOVERED_BELOW_TWO" -eq 0 ] && [ "$RECOVERED_BELOW_THREE" -eq 0 ]; then
            break
        fi
    fi
    sleep 5
done
if [ "$RECOVERED_COUNT" -ne "$BASELINE_COUNT" ] || [ "$RECOVERED_UNAVAILABLE" -ne 0 ] \
    || [ "$RECOVERED_BELOW_TWO" -ne 0 ] || [ "$RECOVERED_BELOW_THREE" -ne 0 ]; then
    printf 'ERROR: broker recovery did not restore all ISR replicas\n' >&2
    exit 1
fi

RECOVERED_JOB=$(wait_checkpoint_after "$JOB_ID" "$OUTAGE_CHECKPOINT")
RECOVERED_CHECKPOINT=${RECOVERED_JOB%%|*}
RECOVERED_REMAINDER=${RECOVERED_JOB#*|}
RECOVERED_COMPLETED=${RECOVERED_REMAINDER%%|*}
RECOVERED_FAILED=${RECOVERED_REMAINDER#*|}
if [ "$RECOVERED_FAILED" -ne "$BASELINE_FAILED" ]; then
    printf 'ERROR: failed checkpoint count increased after broker recovery\n' >&2
    exit 1
fi

RECOVERED_AT=$(date -u +%Y-%m-%dT%H:%M:%SZ)
RECOVERY_SECONDS=$(( $(date +%s) - START_EPOCH ))
export REPORT MARKER STARTED_AT RECOVERED_AT RECOVERY_SECONDS BROKER_SERVICE SURVIVOR_SERVICE
export CHAOS_TOPIC JOB_ID BASELINE_CHECKPOINT OUTAGE_CHECKPOINT RECOVERED_CHECKPOINT
export BASELINE_COMPLETED OUTAGE_COMPLETED RECOVERED_COMPLETED BASELINE_FAILED
export OFFSET_BEFORE OFFSET_DURING MARKER_COUNT
export OUTAGE_COUNT OUTAGE_UNAVAILABLE OUTAGE_BELOW_TWO OUTAGE_BELOW_THREE
export RECOVERED_COUNT RECOVERED_UNAVAILABLE RECOVERED_BELOW_TWO RECOVERED_BELOW_THREE
python3 <<'PY'
import datetime
import json
import os
from pathlib import Path

def number(name):
    return int(os.environ[name])

report = {
    "generatedAt": datetime.datetime.now(datetime.timezone.utc).isoformat(),
    "status": "PASS",
    "marker": os.environ["MARKER"],
    "exercise": {
        "startedAt": os.environ["STARTED_AT"],
        "recoveredAt": os.environ["RECOVERED_AT"],
        "recoverySeconds": number("RECOVERY_SECONDS"),
        "stoppedBroker": os.environ["BROKER_SERVICE"],
        "survivingBootstrap": os.environ["SURVIVOR_SERVICE"],
    },
    "kafka": {
        "topic": os.environ["CHAOS_TOPIC"],
        "replicationFactor": 3,
        "minInSyncReplicas": 2,
        "outage": {
            "partitions": number("OUTAGE_COUNT"),
            "unavailablePartitions": number("OUTAGE_UNAVAILABLE"),
            "partitionsBelowMinIsr": number("OUTAGE_BELOW_TWO"),
            "underReplicatedPartitions": number("OUTAGE_BELOW_THREE"),
            "offsetBefore": number("OFFSET_BEFORE"),
            "offsetAfter": number("OFFSET_DURING"),
            "markerCount": number("MARKER_COUNT"),
        },
        "recovered": {
            "partitions": number("RECOVERED_COUNT"),
            "unavailablePartitions": number("RECOVERED_UNAVAILABLE"),
            "partitionsBelowMinIsr": number("RECOVERED_BELOW_TWO"),
            "underReplicatedPartitions": number("RECOVERED_BELOW_THREE"),
        },
    },
    "flink": {
        "jobId": os.environ["JOB_ID"],
        "stateThroughout": "RUNNING",
        "checkpoint": {
            "baseline": number("BASELINE_CHECKPOINT"),
            "duringOutage": number("OUTAGE_CHECKPOINT"),
            "afterRecovery": number("RECOVERED_CHECKPOINT"),
            "completedBaseline": number("BASELINE_COMPLETED"),
            "completedDuringOutage": number("OUTAGE_COMPLETED"),
            "completedAfterRecovery": number("RECOVERED_COMPLETED"),
            "failed": number("BASELINE_FAILED"),
        },
    },
    "cleanup": {
        "brokerRestored": True,
        "temporaryBrokerOutageActive": False,
    },
}
path = Path(os.environ["REPORT"])
path.parent.mkdir(parents=True, exist_ok=True)
path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(f"BPI broker failure recovery: PASS ({path})")
PY
