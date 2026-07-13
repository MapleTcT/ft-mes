package com.mapletct.ftmes.bpi.stream;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleApplicationStatusV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleApplicationV1;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;

import java.nio.charset.StandardCharsets;
import java.util.List;

public final class RuleApplicationKafkaSerializationSchema
        implements KafkaRecordSerializationSchema<byte[]> {

    private final String topic;

    public RuleApplicationKafkaSerializationSchema(String topic) {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("rule-application topic is required");
        }
        this.topic = topic;
    }

    @Override
    public ProducerRecord<byte[], byte[]> serialize(
            byte[] payload,
            KafkaSinkContext context,
            Long timestamp) {
        BoundaryRuleApplicationV1 application;
        try {
            application = BoundaryRuleApplicationV1.parseFrom(payload);
        } catch (InvalidProtocolBufferException error) {
            throw new IllegalArgumentException("rule-application payload is not valid Protobuf", error);
        }
        if (application.getEventId().isBlank()
                || application.getPublicationEventId().isBlank()
                || application.getTenantId().isBlank()
                || application.getRuleCode().isBlank()
                || application.getRuleVersion().isBlank()
                || application.getChecksum().isBlank()
                || application.getDeploymentId().isBlank()
                || application.getObservedAtMs() <= 0
                || application.getStatus()
                        == BoundaryRuleApplicationStatusV1.BOUNDARY_RULE_APPLICATION_STATUS_UNSPECIFIED
                || application.getStatus() == BoundaryRuleApplicationStatusV1.UNRECOGNIZED) {
            throw new IllegalArgumentException("rule-application payload violates the BPI v1 contract");
        }
        if (application.getStatus() == BoundaryRuleApplicationStatusV1.REJECTED
                && (application.getErrorCode().isBlank() || application.getDetail().isBlank())) {
            throw new IllegalArgumentException("rejected rule application requires error detail");
        }
        byte[] key = application.getPublicationEventId().getBytes(StandardCharsets.UTF_8);
        return new ProducerRecord<>(
                topic,
                null,
                application.getObservedAtMs(),
                key,
                payload,
                List.of(
                        header("event_id", application.getEventId()),
                        header("publication_event_id", application.getPublicationEventId()),
                        header("tenant_id", application.getTenantId()),
                        header("status", application.getStatus().name()),
                        header("schema_version", "v1")));
    }

    private static RecordHeader header(String key, String value) {
        return new RecordHeader(key, value.getBytes(StandardCharsets.UTF_8));
    }
}
