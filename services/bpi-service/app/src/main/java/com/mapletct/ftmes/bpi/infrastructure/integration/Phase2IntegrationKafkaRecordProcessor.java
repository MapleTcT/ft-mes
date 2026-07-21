package com.mapletct.ftmes.bpi.infrastructure.integration;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mapletct.ftmes.bpi.application.ActorContext;
import com.mapletct.ftmes.bpi.application.BatchReleaseService;
import com.mapletct.ftmes.bpi.application.Checksums;
import com.mapletct.ftmes.bpi.application.WmsInboundReversalService;
import com.mapletct.ftmes.bpi.contract.v1.QcsQualityGateV1;
import com.mapletct.ftmes.bpi.contract.v1.WmsCompletionInboundReceiptV1;
import com.mapletct.ftmes.bpi.contract.v1.WmsCompletionInboundReversalReceiptV1;
import com.mapletct.ftmes.bpi.domain.BatchReleaseView;
import com.mapletct.ftmes.bpi.domain.WmsInboundReversalTaskView;
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
public class Phase2IntegrationKafkaRecordProcessor {
    private static final String EVENT_ID = "event_id";
    private static final String IDEMPOTENCY_KEY = "idempotency_key";
    private static final String TENANT_ID = "tenant_id";
    private static final String SCHEMA_VERSION = "schema_version";

    private final BatchReleaseService service;
    private final WmsInboundReversalService reversalService;
    private final BpiPhase2IntegrationProperties properties;

    public Phase2IntegrationKafkaRecordProcessor(
            BatchReleaseService service,
            WmsInboundReversalService reversalService,
            BpiPhase2IntegrationProperties properties) {
        this.service = service;
        this.reversalService = reversalService;
        this.properties = properties;
    }

    public BatchReleaseView processQcs(ConsumerRecord<byte[], byte[]> record) {
        assertTopic(record, properties.qcsTopic());
        byte[] payload = payload(record);
        QcsQualityGateV1 event;
        try {
            event = QcsQualityGateV1.parseFrom(payload);
        } catch (InvalidProtocolBufferException error) {
            throw rejected("QCS Kafka payload is not valid QcsQualityGateV1 Protobuf.", error);
        }
        assertScopeAndHeaders(
                record, event.getTenantId(), event.getPlantId(), event.getLineId(),
                event.getEventId(), event.getIdempotencyKey());
        assertKey(record, event.getBatchId() + "|" + event.getQualityGateId());
        return service.applyQualityGate(
                actor(event.getTenantId(), event.getPlantId(), event.getLineId(), properties.qcsActorId()),
                event, Checksums.sha256(payload));
    }

    public BatchReleaseView processWmsReceipt(ConsumerRecord<byte[], byte[]> record) {
        assertTopic(record, properties.wmsReceiptTopic());
        byte[] payload = payload(record);
        WmsCompletionInboundReceiptV1 event;
        try {
            event = WmsCompletionInboundReceiptV1.parseFrom(payload);
        } catch (InvalidProtocolBufferException error) {
            throw rejected(
                    "WMS Kafka payload is not valid WmsCompletionInboundReceiptV1 Protobuf.", error);
        }
        assertScopeAndHeaders(
                record, event.getTenantId(), event.getPlantId(), event.getLineId(),
                event.getEventId(), event.getIdempotencyKey());
        assertKey(record, event.getCommandEventId());
        return service.applyWmsReceipt(
                actor(event.getTenantId(), event.getPlantId(), event.getLineId(), properties.wmsActorId()),
                event, Checksums.sha256(payload));
    }

    public WmsInboundReversalTaskView processWmsReversalReceipt(
            ConsumerRecord<byte[], byte[]> record) {
        assertTopic(record, properties.wmsReversalReceiptTopic());
        byte[] payload = payload(record);
        WmsCompletionInboundReversalReceiptV1 event;
        try {
            event = WmsCompletionInboundReversalReceiptV1.parseFrom(payload);
        } catch (InvalidProtocolBufferException error) {
            throw rejected(
                    "WMS Kafka payload is not valid WmsCompletionInboundReversalReceiptV1 Protobuf.",
                    error);
        }
        assertScopeAndHeaders(
                record, event.getTenantId(), event.getPlantId(), event.getLineId(),
                event.getEventId(), event.getIdempotencyKey());
        assertKey(record, event.getCommandEventId());
        return reversalService.applyReceipt(
                actor(event.getTenantId(), event.getPlantId(), event.getLineId(),
                        properties.wmsActorId()),
                event, Checksums.sha256(payload));
    }

    private void assertScopeAndHeaders(
            ConsumerRecord<byte[], byte[]> record,
            String tenantId,
            String plantId,
            String lineId,
            String eventId,
            String idempotencyKey) {
        if (!properties.allows(tenantId, plantId, lineId)) {
            throw rejected("Phase 2 Kafka event is outside the configured tenant/plant/line scope.");
        }
        requireHeader(record, EVENT_ID, eventId);
        requireHeader(record, IDEMPOTENCY_KEY, idempotencyKey);
        requireHeader(record, TENANT_ID, tenantId);
        requireHeader(record, SCHEMA_VERSION, "v1");
    }

    private byte[] payload(ConsumerRecord<byte[], byte[]> record) {
        byte[] payload = record.value();
        if (payload == null || payload.length == 0 || payload.length > properties.maxPayloadBytes()) {
            throw rejected("Phase 2 Kafka payload size is invalid.");
        }
        return payload;
    }

    private void assertTopic(ConsumerRecord<byte[], byte[]> record, String expected) {
        if (!expected.equals(record.topic())) {
            throw rejected("Phase 2 record arrived from an untrusted topic.");
        }
    }

    private void assertKey(ConsumerRecord<byte[], byte[]> record, String expected) {
        if (!expected.equals(decode(record.key(), "Kafka record key"))) {
            throw rejected("Phase 2 Kafka record key does not match the payload identity.");
        }
    }

    private void requireHeader(ConsumerRecord<byte[], byte[]> record, String name, String expected) {
        Iterator<Header> headers = record.headers().headers(name).iterator();
        if (!headers.hasNext()) {
            throw rejected("Phase 2 Kafka header " + name + " is required.");
        }
        String actual = decode(headers.next().value(), "Kafka header " + name);
        if (headers.hasNext()) {
            throw rejected("Phase 2 Kafka header " + name + " must appear exactly once.");
        }
        if (!expected.equals(actual)) {
            throw rejected("Phase 2 Kafka header " + name + " does not match the payload.");
        }
    }

    private ActorContext actor(String tenantId, String plantId, String lineId, String actorId) {
        return new ActorContext(
                tenantId, actorId, Set.of("BPI_INTEGRATION_INGEST"), Set.of(plantId), Set.of(lineId));
    }

    private String decode(byte[] value, String field) {
        if (value == null || value.length == 0) throw rejected(field + " is required.");
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

    private Phase2IntegrationKafkaRecordRejectedException rejected(String message) {
        return new Phase2IntegrationKafkaRecordRejectedException(message);
    }

    private Phase2IntegrationKafkaRecordRejectedException rejected(String message, Throwable cause) {
        return new Phase2IntegrationKafkaRecordRejectedException(message, cause);
    }
}
