#!/bin/sh
set -eu

BOOTSTRAP=${BPI_KAFKA_BOOTSTRAP_SERVERS:-kafka-1:19092,kafka-2:19092,kafka-3:19092}
KAFKA_TOPICS=${KAFKA_TOPICS_COMMAND:-/opt/kafka/bin/kafka-topics.sh}
DATA_PARTITIONS=${BPI_DATA_PARTITIONS:-6}
CONTROL_PARTITIONS=${BPI_CONTROL_PARTITIONS:-3}

create_topic() {
    topic=$1
    partitions=$2
    shift 2
    "$KAFKA_TOPICS" \
        --bootstrap-server "$BOOTSTRAP" \
        --create \
        --if-not-exists \
        --topic "$topic" \
        --partitions "$partitions" \
        --replication-factor 3 \
        --config min.insync.replicas=2 \
        "$@"
}

create_topic "${BPI_TELEMETRY_TOPIC:-iot.telemetry.selected.v1}" "$DATA_PARTITIONS"
create_topic "${BPI_CONTEXT_TOPIC:-mes.production.context.v1}" "$CONTROL_PARTITIONS"
create_topic "${BPI_RULE_TOPIC:-bpi.boundary.rule-publication.v1}" "$CONTROL_PARTITIONS" \
    --config cleanup.policy=compact \
    --config delete.retention.ms=86400000
create_topic "${BPI_CANDIDATE_TOPIC:-bpi.batch.candidate.v1}" "$DATA_PARTITIONS" \
    --config retention.ms=2592000000
create_topic "${BPI_CANDIDATE_DLQ_TOPIC:-bpi.batch.candidate.dlq.v1}" "$DATA_PARTITIONS" \
    --config retention.ms=2592000000
create_topic "${BPI_DATA_QUALITY_TOPIC:-bpi.data-quality.v1}" "$DATA_PARTITIONS"

"$KAFKA_TOPICS" --bootstrap-server "$BOOTSTRAP" --list
