package com.mapletct.ftmes.bpi.stream;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mapletct.ftmes.bpi.contract.validation.BpiContractValidator;
import com.mapletct.ftmes.bpi.contract.v1.BatchCandidateV1;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;

import java.nio.charset.StandardCharsets;
import java.util.List;

public final class CandidateKafkaSerializationSchema implements KafkaRecordSerializationSchema<byte[]> {

    private final String topic;

    public CandidateKafkaSerializationSchema(String topic) {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("candidate topic is required");
        }
        this.topic = topic;
    }

    @Override
    public ProducerRecord<byte[], byte[]> serialize(
            byte[] payload,
            KafkaSinkContext context,
            Long timestamp) {
        BatchCandidateV1 candidate;
        try {
            candidate = BatchCandidateV1.parseFrom(payload);
        } catch (InvalidProtocolBufferException error) {
            throw new IllegalArgumentException("candidate payload is not valid Protobuf", error);
        }
        if (!BpiContractValidator.validate(candidate).isEmpty()) {
            throw new IllegalArgumentException("candidate payload violates the BPI v1 contract");
        }
        byte[] key = (candidate.getLineId() + "|" + candidate.getRuleCode())
                .getBytes(StandardCharsets.UTF_8);
        return new ProducerRecord<>(
                topic,
                null,
                candidate.getBoundaryEventTimeMs(),
                key,
                payload,
                List.of(
                        header("event_id", candidate.getEventId()),
                        header("candidate_key", candidate.getCandidateKey()),
                        header("tenant_id", candidate.getTenantId()),
                        header("schema_version", "v1")));
    }

    private static RecordHeader header(String key, String value) {
        return new RecordHeader(key, value.getBytes(StandardCharsets.UTF_8));
    }
}
