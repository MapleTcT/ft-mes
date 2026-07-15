package com.mapletct.ftmes.bpi.stream;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleRuntimeReadinessStatusV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleRuntimeReadinessV1;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;

import java.nio.charset.StandardCharsets;
import java.util.List;

public final class RuleRuntimeReadinessKafkaSerializationSchema
        implements KafkaRecordSerializationSchema<byte[]> {

    private final String topic;

    public RuleRuntimeReadinessKafkaSerializationSchema(String topic) {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("rule runtime-readiness topic is required");
        }
        this.topic = topic;
    }

    @Override
    public ProducerRecord<byte[], byte[]> serialize(
            byte[] payload,
            KafkaSinkContext context,
            Long timestamp) {
        BoundaryRuleRuntimeReadinessV1 event;
        try {
            event = BoundaryRuleRuntimeReadinessV1.parseFrom(payload);
        } catch (InvalidProtocolBufferException error) {
            throw new IllegalArgumentException("runtime-readiness payload is not valid Protobuf", error);
        }
        BoundaryRuleRuntimeReadinessStatusV1 status = event.getStatus();
        if (event.getEventId().isBlank()
                || event.getPublicationEventId().isBlank()
                || event.getTenantId().isBlank()
                || event.getPlantId().isBlank()
                || event.getLineId().isBlank()
                || event.getRuleCode().isBlank()
                || event.getRuleVersion().isBlank()
                || event.getChecksum().isBlank()
                || event.getDeploymentId().isBlank()
                || event.getObservedAtMs() <= 0
                || status == BoundaryRuleRuntimeReadinessStatusV1
                        .BOUNDARY_RULE_RUNTIME_READINESS_STATUS_UNSPECIFIED
                || status == BoundaryRuleRuntimeReadinessStatusV1.UNRECOGNIZED) {
            throw new IllegalArgumentException("runtime-readiness payload violates the BPI v1 contract");
        }
        if (status != BoundaryRuleRuntimeReadinessStatusV1.READY
                && (event.getReasonCode().isBlank() || event.getDetail().isBlank())) {
            throw new IllegalArgumentException("non-ready runtime status requires a reason and detail");
        }
        byte[] key = event.getPublicationEventId().getBytes(StandardCharsets.UTF_8);
        return new ProducerRecord<>(
                topic,
                null,
                event.getObservedAtMs(),
                key,
                payload,
                List.of(
                        header("event_id", event.getEventId()),
                        header("publication_event_id", event.getPublicationEventId()),
                        header("tenant_id", event.getTenantId()),
                        header("status", status.name()),
                        header("schema_version", "v1")));
    }

    private static RecordHeader header(String key, String value) {
        return new RecordHeader(key, value.getBytes(StandardCharsets.UTF_8));
    }
}
