package com.mapletct.ftmes.bpi.infrastructure.application;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mapletct.ftmes.bpi.application.Checksums;
import com.mapletct.ftmes.bpi.application.RuleApplicationReceiptService;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleApplicationStatusV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleApplicationV1;
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
public class RuleApplicationKafkaRecordProcessor {
    private final RuleApplicationReceiptService receiptService;
    private final BpiRuleApplicationKafkaProperties properties;

    public RuleApplicationKafkaRecordProcessor(
            RuleApplicationReceiptService receiptService,
            BpiRuleApplicationKafkaProperties properties) {
        this.receiptService = receiptService;
        this.properties = properties;
    }

    public RuleVersionView process(ConsumerRecord<byte[], byte[]> record) {
        BoundaryRuleApplicationV1 event = decodeAndValidate(record);
        return receiptService.apply(event, Checksums.sha256(record.value()));
    }

    private BoundaryRuleApplicationV1 decodeAndValidate(ConsumerRecord<byte[], byte[]> record) {
        if (!properties.topic().equals(record.topic())) {
            throw rejected("Rule application arrived from an untrusted topic.");
        }
        byte[] payload = record.value();
        if (payload == null || payload.length == 0 || payload.length > properties.maxPayloadBytes()) {
            throw rejected("Rule application payload size is invalid.");
        }
        BoundaryRuleApplicationV1 event;
        try {
            event = BoundaryRuleApplicationV1.parseFrom(payload);
        } catch (InvalidProtocolBufferException error) {
            throw rejected("Rule application payload is not valid Protobuf.", error);
        }
        if (!properties.allows(event.getTenantId(), event.getPlantId(), event.getLineId())) {
            throw rejected("Rule application is outside the configured tenant/plant/line scope.");
        }
        if (event.getEventId().isBlank()
                || event.getPublicationEventId().isBlank()
                || event.getRuleCode().isBlank()
                || event.getRuleVersion().isBlank()
                || event.getChecksum().isBlank()
                || event.getDeploymentId().isBlank()
                || event.getObservedAtMs() <= 0
                || event.getStatus()
                        == BoundaryRuleApplicationStatusV1.BOUNDARY_RULE_APPLICATION_STATUS_UNSPECIFIED
                || event.getStatus() == BoundaryRuleApplicationStatusV1.UNRECOGNIZED) {
            throw rejected("Rule application violates the BPI v1 contract.");
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
        requireLength(event.getErrorCode(), 128, "error_code");
        requireLength(event.getDetail(), 1000, "detail");
        if (event.getStatus() == BoundaryRuleApplicationStatusV1.REJECTED
                && (event.getErrorCode().isBlank() || event.getDetail().isBlank())) {
            throw rejected("Rejected rule application requires error detail.");
        }

        requireHeader(record, "event_id", event.getEventId());
        requireHeader(record, "publication_event_id", event.getPublicationEventId());
        requireHeader(record, "tenant_id", event.getTenantId());
        requireHeader(record, "status", event.getStatus().name());
        requireHeader(record, "schema_version", "v1");
        if (!event.getPublicationEventId().equals(decode(record.key(), "Kafka record key"))) {
            throw rejected("Rule application Kafka key does not match publication_event_id.");
        }
        return event;
    }

    private void requireLength(String value, int maximum, String field) {
        if (value != null && value.length() > maximum) {
            throw rejected("Rule application " + field + " exceeds " + maximum + " characters.");
        }
    }

    private void requireHeader(ConsumerRecord<byte[], byte[]> record, String name, String expected) {
        Iterator<Header> headers = record.headers().headers(name).iterator();
        if (!headers.hasNext()) {
            throw rejected("Rule application Kafka header " + name + " is required.");
        }
        String actual = decode(headers.next().value(), "Kafka header " + name);
        if (headers.hasNext()) {
            throw rejected("Rule application Kafka header " + name + " must appear exactly once.");
        }
        if (!expected.equals(actual)) {
            throw rejected("Rule application Kafka header " + name + " does not match the payload.");
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

    private RuleApplicationKafkaRecordRejectedException rejected(String message) {
        return new RuleApplicationKafkaRecordRejectedException(message);
    }

    private RuleApplicationKafkaRecordRejectedException rejected(String message, Throwable cause) {
        return new RuleApplicationKafkaRecordRejectedException(message, cause);
    }
}
