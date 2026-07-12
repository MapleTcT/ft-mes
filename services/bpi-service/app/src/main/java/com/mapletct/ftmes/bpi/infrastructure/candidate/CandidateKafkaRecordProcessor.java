package com.mapletct.ftmes.bpi.infrastructure.candidate;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mapletct.ftmes.bpi.application.ActorContext;
import com.mapletct.ftmes.bpi.application.CandidateEventMapper;
import com.mapletct.ftmes.bpi.application.CandidateIngestionService;
import com.mapletct.ftmes.bpi.application.error.BpiForbiddenException;
import com.mapletct.ftmes.bpi.application.error.BpiValidationException;
import com.mapletct.ftmes.bpi.contract.v1.BatchCandidateV1;
import com.mapletct.ftmes.bpi.domain.BatchCandidate;
import com.mapletct.ftmes.bpi.interfaces.rest.CandidateIngestRequest;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

@Component
public class CandidateKafkaRecordProcessor {
    private static final String EVENT_ID = "event_id";
    private static final String CANDIDATE_KEY = "candidate_key";
    private static final String TENANT_ID = "tenant_id";
    private static final String SCHEMA_VERSION = "schema_version";

    private final CandidateEventMapper eventMapper;
    private final CandidateIngestionService ingestionService;
    private final BpiCandidateEventProperties eventProperties;
    private final BpiCandidateKafkaProperties kafkaProperties;

    public CandidateKafkaRecordProcessor(
            CandidateEventMapper eventMapper,
            CandidateIngestionService ingestionService,
            BpiCandidateEventProperties eventProperties,
            BpiCandidateKafkaProperties kafkaProperties) {
        this.eventMapper = eventMapper;
        this.ingestionService = ingestionService;
        this.eventProperties = eventProperties;
        this.kafkaProperties = kafkaProperties;
    }

    public BatchCandidate process(ConsumerRecord<byte[], byte[]> record) {
        BatchCandidateV1 event = decodeAndValidateEnvelope(record);
        ActorContext actor = new ActorContext(
                event.getTenantId(),
                kafkaProperties.actorId(),
                Set.of("BPI_EVENT_INGEST"),
                Set.of(event.getPlantId()),
                Set.of(event.getLineId()));
        CandidateIngestRequest request;
        try {
            request = eventMapper.toRequest(actor, event);
        } catch (BpiValidationException | BpiForbiddenException error) {
            throw rejected(error.getMessage(), error);
        }
        return ingestionService.ingest(actor, request);
    }

    private BatchCandidateV1 decodeAndValidateEnvelope(ConsumerRecord<byte[], byte[]> record) {
        if (!kafkaProperties.topic().equals(record.topic())) {
            throw rejected("Candidate record arrived from an untrusted topic.");
        }
        byte[] payload = record.value();
        if (payload == null || payload.length == 0 || payload.length > eventProperties.maxPayloadBytes()) {
            throw rejected("Candidate Kafka payload size is invalid.");
        }

        BatchCandidateV1 event;
        try {
            event = BatchCandidateV1.parseFrom(payload);
        } catch (InvalidProtocolBufferException error) {
            throw rejected("Candidate Kafka payload is not valid BatchCandidateV1 Protobuf.", error);
        }
        if (!kafkaProperties.allows(event.getTenantId(), event.getPlantId(), event.getLineId())) {
            throw rejected("Candidate Kafka event is outside the configured tenant/plant/line scope.");
        }

        requireHeader(record, EVENT_ID, event.getEventId());
        requireHeader(record, CANDIDATE_KEY, event.getCandidateKey());
        requireHeader(record, TENANT_ID, event.getTenantId());
        requireHeader(record, SCHEMA_VERSION, "v1");
        String expectedKey = event.getLineId() + "|" + event.getRuleCode();
        if (!expectedKey.equals(decode(record.key(), "Kafka record key"))) {
            throw rejected("Candidate Kafka record key does not match line_id|rule_code.");
        }

        return event;
    }

    private void requireHeader(ConsumerRecord<byte[], byte[]> record, String name, String expected) {
        Iterator<Header> headers = record.headers().headers(name).iterator();
        if (!headers.hasNext()) {
            throw rejected("Candidate Kafka header " + name + " is required.");
        }
        String actual = decode(headers.next().value(), "Kafka header " + name);
        if (headers.hasNext()) {
            throw rejected("Candidate Kafka header " + name + " must appear exactly once.");
        }
        if (!expected.equals(actual)) {
            throw rejected("Candidate Kafka header " + name + " does not match the payload.");
        }
    }

    private String decode(byte[] value, String field) {
        if (value == null || value.length == 0) {
            throw rejected(field + " is required.");
        }
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(value))
                    .toString();
        } catch (CharacterCodingException error) {
            throw rejected(field + " is not valid UTF-8.", error);
        }
    }

    private CandidateKafkaRecordRejectedException rejected(String message) {
        return new CandidateKafkaRecordRejectedException(message);
    }

    private CandidateKafkaRecordRejectedException rejected(String message, Throwable cause) {
        return new CandidateKafkaRecordRejectedException(message, cause);
    }
}
