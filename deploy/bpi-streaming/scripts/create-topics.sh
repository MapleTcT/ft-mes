#!/bin/sh
set -eu

BOOTSTRAP=${BPI_KAFKA_BOOTSTRAP_SERVERS:-kafka-1:19092,kafka-2:19092,kafka-3:19092}
KAFKA_TOPICS=${KAFKA_TOPICS_COMMAND:-/opt/kafka/bin/kafka-topics.sh}
KAFKA_CONFIGS=${KAFKA_CONFIGS_COMMAND:-/opt/kafka/bin/kafka-configs.sh}
DATA_PARTITIONS=${BPI_DATA_PARTITIONS:-6}
CONTROL_PARTITIONS=${BPI_CONTROL_PARTITIONS:-3}
POINT_CATALOG_MAX_MESSAGE_BYTES=${BPI_POINT_CATALOG_MAX_MESSAGE_BYTES:-6291456}

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

alter_topic_config() {
    topic=$1
    config=$2
    "$KAFKA_CONFIGS" \
        --bootstrap-server "$BOOTSTRAP" \
        --entity-type topics \
        --entity-name "$topic" \
        --alter \
        --add-config "$config"
}

create_topic "${BPI_TELEMETRY_TOPIC:-iot.telemetry.selected.v1}" "$DATA_PARTITIONS"
create_topic "${BPI_TELEMETRY_DLQ_TOPIC:-iot.telemetry.selected.dlq.v1}" "$DATA_PARTITIONS" \
    --config retention.ms=2592000000
create_topic "${BPI_POINT_CATALOG_TOPIC:-iot.point-catalog.snapshot.v1}" "$CONTROL_PARTITIONS" \
    --config cleanup.policy=compact \
    --config "max.message.bytes=$POINT_CATALOG_MAX_MESSAGE_BYTES" \
    --config delete.retention.ms=86400000
create_topic "${BPI_POINT_CATALOG_DLQ_TOPIC:-iot.point-catalog.snapshot.dlq.v1}" "$CONTROL_PARTITIONS" \
    --config "max.message.bytes=$POINT_CATALOG_MAX_MESSAGE_BYTES" \
    --config retention.ms=2592000000
create_topic "${BPI_SOURCE_SEQUENCE_TOPIC:-iot.source-sequence.evidence.v1}" "$CONTROL_PARTITIONS" \
    --config cleanup.policy=compact \
    --config delete.retention.ms=86400000
create_topic "${BPI_SOURCE_SEQUENCE_DLQ_TOPIC:-iot.source-sequence.evidence.dlq.v1}" "$CONTROL_PARTITIONS" \
    --config retention.ms=2592000000
create_topic "${BPI_CONTEXT_TOPIC:-mes.production.context.v1}" "$CONTROL_PARTITIONS"
create_topic "${BPI_RULE_TOPIC:-bpi.boundary.rule-publication.v1}" "$CONTROL_PARTITIONS" \
    --config cleanup.policy=compact \
    --config delete.retention.ms=86400000
create_topic "${BPI_RULE_APPLICATION_TOPIC:-bpi.boundary.rule-application.v1}" "$CONTROL_PARTITIONS" \
    --config cleanup.policy=compact \
    --config delete.retention.ms=86400000
create_topic "${BPI_RULE_APPLICATION_DLQ_TOPIC:-bpi.boundary.rule-application.dlq.v1}" "$CONTROL_PARTITIONS" \
    --config retention.ms=2592000000
create_topic "${BPI_RULE_RUNTIME_READINESS_TOPIC:-bpi.boundary.rule-runtime-readiness.v1}" "$CONTROL_PARTITIONS" \
    --config cleanup.policy=compact \
    --config delete.retention.ms=86400000
create_topic "${BPI_RULE_RUNTIME_READINESS_DLQ_TOPIC:-bpi.boundary.rule-runtime-readiness.dlq.v1}" "$CONTROL_PARTITIONS" \
    --config retention.ms=2592000000
create_topic "${BPI_CANDIDATE_TOPIC:-bpi.batch.candidate.v1}" "$DATA_PARTITIONS" \
    --config retention.ms=2592000000
create_topic "${BPI_CANDIDATE_DLQ_TOPIC:-bpi.batch.candidate.dlq.v1}" "$DATA_PARTITIONS" \
    --config retention.ms=2592000000
create_topic "${BPI_DATA_QUALITY_TOPIC:-bpi.data-quality.v1}" "$DATA_PARTITIONS"
create_topic "${BPI_QCS_TOPIC:-qcs.batch.quality-gate.v1}" "$CONTROL_PARTITIONS" \
    --config retention.ms=2592000000
create_topic "${BPI_QCS_DLQ_TOPIC:-qcs.batch.quality-gate.dlq.v1}" "$CONTROL_PARTITIONS" \
    --config retention.ms=2592000000
create_topic "${BPI_WMS_COMMAND_TOPIC:-bpi.wms.completion-inbound-command.v1}" "$CONTROL_PARTITIONS" \
    --config retention.ms=2592000000
create_topic "${BPI_WMS_COMMAND_DLQ_TOPIC:-bpi.wms.completion-inbound-command.dlq.v1}" "$CONTROL_PARTITIONS" \
    --config retention.ms=2592000000
create_topic "${BPI_WMS_RECEIPT_TOPIC:-wms.completion-inbound.receipt.v1}" "$CONTROL_PARTITIONS" \
    --config retention.ms=2592000000
create_topic "${BPI_WMS_RECEIPT_DLQ_TOPIC:-wms.completion-inbound.receipt.dlq.v1}" "$CONTROL_PARTITIONS" \
    --config retention.ms=2592000000
create_topic "${BPI_WMS_REVERSAL_COMMAND_TOPIC:-bpi.wms.completion-inbound-reversal-command.v1}" "$CONTROL_PARTITIONS" \
    --config retention.ms=2592000000
create_topic "${BPI_WMS_REVERSAL_COMMAND_DLQ_TOPIC:-bpi.wms.completion-inbound-reversal-command.dlq.v1}" "$CONTROL_PARTITIONS" \
    --config retention.ms=2592000000
create_topic "${BPI_WMS_REVERSAL_RECEIPT_TOPIC:-wms.completion-inbound-reversal.receipt.v1}" "$CONTROL_PARTITIONS" \
    --config retention.ms=2592000000
create_topic "${BPI_WMS_REVERSAL_RECEIPT_DLQ_TOPIC:-wms.completion-inbound-reversal.receipt.dlq.v1}" "$CONTROL_PARTITIONS" \
    --config retention.ms=2592000000

# kafka-topics --if-not-exists does not reconcile configuration on existing topics.
alter_topic_config \
    "${BPI_POINT_CATALOG_TOPIC:-iot.point-catalog.snapshot.v1}" \
    "max.message.bytes=$POINT_CATALOG_MAX_MESSAGE_BYTES"
alter_topic_config \
    "${BPI_POINT_CATALOG_DLQ_TOPIC:-iot.point-catalog.snapshot.dlq.v1}" \
    "max.message.bytes=$POINT_CATALOG_MAX_MESSAGE_BYTES"

"$KAFKA_TOPICS" --bootstrap-server "$BOOTSTRAP" --list
