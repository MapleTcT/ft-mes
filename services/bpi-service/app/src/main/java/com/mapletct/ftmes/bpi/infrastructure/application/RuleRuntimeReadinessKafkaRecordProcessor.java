package com.mapletct.ftmes.bpi.infrastructure.application;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mapletct.ftmes.bpi.application.Checksums;
import com.mapletct.ftmes.bpi.application.RuleRuntimeReadinessReceiptService;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleRuntimeReadinessStatusV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleRuntimeReadinessV1;
import com.mapletct.ftmes.bpi.domain.RuleVersionView;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

@Component
public class RuleRuntimeReadinessKafkaRecordProcessor {
    private final RuleRuntimeReadinessReceiptService receiptService;
    private final BpiRuleApplicationKafkaProperties properties;

    public RuleRuntimeReadinessKafkaRecordProcessor(
            RuleRuntimeReadinessReceiptService receiptService,
            BpiRuleApplicationKafkaProperties properties) {
        this.receiptService = receiptService;
        this.properties = properties;
    }

    public RuleVersionView process(ConsumerRecord<byte[], byte[]> record) {
        BoundaryRuleRuntimeReadinessV1 event = decodeAndValidate(record);
        return receiptService.apply(event, Checksums.sha256(record.value()));
    }

    private BoundaryRuleRuntimeReadinessV1 decodeAndValidate(ConsumerRecord<byte[], byte[]> record) {
        if (!properties.runtimeReadinessTopic().equals(record.topic())) {
            throw rejected("Runtime-readiness event arrived from an untrusted topic.");
        }
        byte[] payload = record.value();
        if (payload == null || payload.length == 0 || payload.length > properties.maxPayloadBytes()) {
            throw rejected("Runtime-readiness payload size is invalid.");
        }
        BoundaryRuleRuntimeReadinessV1 event;
        try {
            event = BoundaryRuleRuntimeReadinessV1.parseFrom(payload);
        } catch (InvalidProtocolBufferException error) {
            throw rejected("Runtime-readiness payload is not valid Protobuf.", error);
        }
        BoundaryRuleRuntimeReadinessStatusV1 status = event.getStatus();
        if (!properties.allows(event.getTenantId(), event.getPlantId(), event.getLineId())) {
            throw rejected("Runtime-readiness event is outside the configured tenant/plant/line scope.");
        }
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
            throw rejected("Runtime-readiness event violates the BPI v1 contract.");
        }
        requireLength(event.getEventId(), 512, "event_id");
        requireLength(event.getPublicationEventId(), 36, "publication_event_id");
        requireLength(event.getTenantId(), 64, "tenant_id");
        requireLength(event.getPlantId(), 64, "plant_id");
        requireLength(event.getLineId(), 128, "line_id");
        requireLength(event.getRuleCode(), 128, "rule_code");
        requireLength(event.getRuleVersion(), 64, "rule_version");
        requireLength(event.getChecksum(), 128, "checksum");
        requireLength(event.getDeploymentId(), 128, "deployment_id");
        requireLength(event.getReasonCode(), 128, "reason_code");
        requireLength(event.getDetail(), 1000, "detail");
        requireLength(event.getPointCatalogEventId(), 512, "point_catalog_event_id");
        requireLength(event.getPointCatalogSourceRevision(), 128, "point_catalog_source_revision");
        if (status != BoundaryRuleRuntimeReadinessStatusV1.READY
                && (event.getReasonCode().isBlank() || event.getDetail().isBlank())) {
            throw rejected("Non-ready runtime status requires a reason and detail.");
        }
        if (status == BoundaryRuleRuntimeReadinessStatusV1.READY
                && (!event.getReasonCode().isBlank() || !event.getDetail().isBlank())) {
            throw rejected("READY runtime status cannot carry a degradation reason.");
        }

        requireHeader(record, "event_id", event.getEventId());
        requireHeader(record, "publication_event_id", event.getPublicationEventId());
        requireHeader(record, "tenant_id", event.getTenantId());
        requireHeader(record, "status", status.name());
        requireHeader(record, "schema_version", "v1");
        if (!event.getPublicationEventId().equals(decode(record.key(), "Kafka record key"))) {
            throw rejected("Runtime-readiness Kafka key does not match publication_event_id.");
        }
        return event;
    }

    private void requireLength(String value, int maximum, String field) {
        if (value != null && value.length() > maximum) {
            throw rejected("Runtime-readiness " + field + " exceeds " + maximum + " characters.");
        }
    }

    private void requireHeader(ConsumerRecord<byte[], byte[]> record, String name, String expected) {
        Iterator<Header> headers = record.headers().headers(name).iterator();
        if (!headers.hasNext()) throw rejected("Runtime-readiness Kafka header " + name + " is required.");
        String actual = decode(headers.next().value(), "Kafka header " + name);
        if (headers.hasNext()) {
            throw rejected("Runtime-readiness Kafka header " + name + " must appear exactly once.");
        }
        if (!expected.equals(actual)) {
            throw rejected("Runtime-readiness Kafka header " + name + " does not match the payload.");
        }
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

    private RuleRuntimeReadinessKafkaRecordRejectedException rejected(String message) {
        return new RuleRuntimeReadinessKafkaRecordRejectedException(message);
    }

    private RuleRuntimeReadinessKafkaRecordRejectedException rejected(String message, Throwable cause) {
        return new RuleRuntimeReadinessKafkaRecordRejectedException(message, cause);
    }
}
