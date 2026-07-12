package com.mapletct.ftmes.bpi.stream;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mapletct.ftmes.bpi.contract.v1.DataQualityEventV1;
import com.mapletct.ftmes.bpi.contract.v1.DataQualitySeverity;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;

import java.nio.charset.StandardCharsets;
import java.util.List;

public final class DataQualityKafkaSerializationSchema implements KafkaRecordSerializationSchema<byte[]> {

    private final String topic;

    public DataQualityKafkaSerializationSchema(String topic) {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("data-quality topic is required");
        }
        this.topic = topic;
    }

    @Override
    public ProducerRecord<byte[], byte[]> serialize(
            byte[] payload,
            KafkaSinkContext context,
            Long timestamp) {
        DataQualityEventV1 event;
        try {
            event = DataQualityEventV1.parseFrom(payload);
        } catch (InvalidProtocolBufferException error) {
            throw new IllegalArgumentException("data-quality payload is not valid Protobuf", error);
        }
        if (event.getEventId().isBlank()
                || event.getIssueCode().isBlank()
                || event.getDetail().isBlank()
                || event.getDetectedAtMs() <= 0
                || event.getSeverity() == DataQualitySeverity.DATA_QUALITY_SEVERITY_UNSPECIFIED
                || event.getSeverity() == DataQualitySeverity.UNRECOGNIZED) {
            throw new IllegalArgumentException("data-quality payload violates the BPI v1 contract");
        }
        String key = String.join(
                "|",
                event.getTenantId(),
                event.getLineId(),
                event.getSourceEventId(),
                event.getPropertyId(),
                event.getIssueCode());
        return new ProducerRecord<>(
                topic,
                null,
                event.getDetectedAtMs(),
                key.getBytes(StandardCharsets.UTF_8),
                payload,
                List.of(
                        header("event_id", event.getEventId()),
                        header("issue_code", event.getIssueCode()),
                        header("tenant_id", event.getTenantId()),
                        header("schema_version", "v1")));
    }

    private static RecordHeader header(String key, String value) {
        return new RecordHeader(key, value.getBytes(StandardCharsets.UTF_8));
    }
}
