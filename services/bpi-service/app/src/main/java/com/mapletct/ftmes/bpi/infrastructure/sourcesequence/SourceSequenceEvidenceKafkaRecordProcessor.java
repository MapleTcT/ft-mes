package com.mapletct.ftmes.bpi.infrastructure.sourcesequence;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mapletct.ftmes.bpi.application.Checksums;
import com.mapletct.ftmes.bpi.application.SourceSequenceEvidenceIngestResult;
import com.mapletct.ftmes.bpi.application.SourceSequenceEvidenceIngestionService;
import com.mapletct.ftmes.bpi.contract.v1.SequenceOrigin;
import com.mapletct.ftmes.bpi.contract.v1.SourceSequenceEvidenceStatusV1;
import com.mapletct.ftmes.bpi.contract.v1.SourceSequenceEvidenceV1;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.regex.Pattern;

@Component
public class SourceSequenceEvidenceKafkaRecordProcessor {
    private static final Pattern SOURCE = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]*");
    private static final Pattern FINGERPRINT = Pattern.compile("sha256:[0-9a-f]{64}");
    private static final String EVENT_ID = "event_id";
    private static final String TENANT_ID = "tenant_id";
    private static final String BINDING_FINGERPRINT = "binding_fingerprint";
    private static final String SCHEMA_VERSION = "schema_version";

    private final SourceSequenceEvidenceIngestionService service;
    private final BpiSourceSequenceKafkaProperties properties;

    public SourceSequenceEvidenceKafkaRecordProcessor(
            SourceSequenceEvidenceIngestionService service,
            BpiSourceSequenceKafkaProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    public SourceSequenceEvidenceIngestResult process(ConsumerRecord<byte[], byte[]> record) {
        byte[] payload = record.value();
        SourceSequenceEvidenceV1 event = decodeAndValidate(record);
        return service.ingest(event, Checksums.sha256(payload), properties.actorId());
    }

    SourceSequenceEvidenceV1 decodeAndValidate(ConsumerRecord<byte[], byte[]> record) {
        if (!properties.topic().equals(record.topic())) {
            throw rejected("Source sequence evidence arrived from an untrusted topic.");
        }
        byte[] payload = record.value();
        if (payload == null || payload.length == 0 || payload.length > properties.maxPayloadBytes()) {
            throw rejected("Source sequence evidence Kafka payload size is invalid.");
        }

        SourceSequenceEvidenceV1 event;
        try {
            event = SourceSequenceEvidenceV1.parseFrom(payload);
        } catch (InvalidProtocolBufferException error) {
            throw rejected(
                    "Source sequence evidence payload is not valid SourceSequenceEvidenceV1 Protobuf.",
                    error
            );
        }
        validateEvent(event);
        validateContentIdentity(event);
        if (!properties.allows(event.getTenantId(), event.getPlantId(), event.getLineId())) {
            throw rejected("Source sequence evidence is outside the configured tenant/plant/line scope.");
        }

        requireHeader(record, EVENT_ID, event.getEventId());
        requireHeader(record, TENANT_ID, event.getTenantId());
        requireHeader(record, BINDING_FINGERPRINT, event.getBindingFingerprint());
        requireHeader(record, SCHEMA_VERSION, "v1");
        if (!key(event).equals(decode(record.key(), "Kafka record key"))) {
            throw rejected("Source sequence evidence record key does not match its stable binding identity.");
        }
        return event;
    }

    private void validateEvent(SourceSequenceEvidenceV1 event) {
        require(event.getEventId(), 8, 128, "event_id");
        require(event.getSource(), 1, 64, "source");
        if (!SOURCE.matcher(event.getSource()).matches()) {
            throw rejected("Source sequence evidence source has an invalid format.");
        }
        keyPart(event.getSourceInstance(), 1, 128, "source_instance");
        keyPart(event.getTenantId(), 1, 64, "tenant_id");
        keyPart(event.getPlantId(), 1, 64, "plant_id");
        keyPart(event.getLineId(), 1, 128, "line_id");
        keyPart(event.getProductId(), 1, 128, "product_id");
        keyPart(event.getDeviceId(), 1, 128, "device_id");
        if (!FINGERPRINT.matcher(event.getBindingFingerprint()).matches()) {
            throw rejected("Source sequence evidence binding_fingerprint is invalid.");
        }
        require(event.getReason(), 3, 500, "reason");
        if (event.getObservedAtMs() <= 0L) {
            throw rejected("Source sequence evidence observed_at_ms must be positive.");
        }
        if (Instant.ofEpochMilli(event.getObservedAtMs())
                .isAfter(Instant.now().plus(5, ChronoUnit.MINUTES))) {
            throw rejected("Source sequence evidence observed_at_ms must not be in the future.");
        }
        validateShape(event);
    }

    private void validateShape(SourceSequenceEvidenceV1 event) {
        SourceSequenceEvidenceStatusV1 status = event.getStatus();
        if (status == SourceSequenceEvidenceStatusV1.SOURCE_SEQUENCE_EVIDENCE_STATUS_UNSPECIFIED
                || status == SourceSequenceEvidenceStatusV1.UNRECOGNIZED) {
            throw rejected("Source sequence evidence status is required.");
        }
        boolean empty = status == SourceSequenceEvidenceStatusV1.SOURCE_SEQUENCE_EVIDENCE_DISABLED
                || status == SourceSequenceEvidenceStatusV1.SOURCE_SEQUENCE_EVIDENCE_MISSING;
        if (empty) {
            if (event.getSequenceOrigin() != SequenceOrigin.SEQUENCE_ORIGIN_UNSPECIFIED
                    || event.getSourceEpoch() != 0L
                    || event.getFirstSequence() != 0L
                    || event.getLastSequence() != 0L
                    || event.getObservationCount() != 0
                    || event.getFirstObservedAtMs() != 0L
                    || event.getLastObservedAtMs() != 0L
                    || event.getValidUntilMs() != 0L) {
                throw rejected("Disabled or missing source sequence evidence must not carry sequence values.");
            }
            return;
        }

        if (event.getSequenceOrigin() != SequenceOrigin.DEVICE
                && event.getSequenceOrigin() != SequenceOrigin.GATEWAY) {
            throw rejected("Source sequence evidence origin must be DEVICE or GATEWAY.");
        }
        if (event.getSourceEpoch() <= 0L
                || event.getFirstSequence() <= 0L
                || event.getLastSequence() < event.getFirstSequence()
                || event.getObservationCount() <= 0
                || event.getFirstObservedAtMs() <= 0L
                || event.getLastObservedAtMs() < event.getFirstObservedAtMs()
                || event.getValidUntilMs() <= event.getLastObservedAtMs()) {
            throw rejected("Source sequence evidence sequence range or observation time is invalid.");
        }
        if (status == SourceSequenceEvidenceStatusV1.SOURCE_SEQUENCE_EVIDENCE_PENDING
                && event.getValidUntilMs() <= event.getObservedAtMs()) {
            throw rejected("Pending source sequence evidence must still be fresh.");
        }
        if (status == SourceSequenceEvidenceStatusV1.SOURCE_SEQUENCE_EVIDENCE_QUALIFIED
                && (event.getObservationCount() < 2
                    || event.getLastSequence() <= event.getFirstSequence()
                    || event.getValidUntilMs() <= event.getObservedAtMs())) {
            throw rejected("Qualified source sequence evidence must be monotonic and fresh.");
        }
        if (status == SourceSequenceEvidenceStatusV1.SOURCE_SEQUENCE_EVIDENCE_EXPIRED
                && (event.getObservationCount() < 2
                    || event.getLastSequence() <= event.getFirstSequence()
                    || event.getValidUntilMs() > event.getObservedAtMs())) {
            throw rejected("Expired source sequence evidence must contain an expired qualified range.");
        }
    }

    private void validateContentIdentity(SourceSequenceEvidenceV1 event) {
        String expected = "source-sequence-evidence-" + Checksums.sha256(
                event.toBuilder().clearEventId().build().toByteArray());
        if (!expected.equals(event.getEventId())) {
            throw rejected("Source sequence evidence event_id does not match the canonical payload SHA-256.");
        }
    }

    private static String key(SourceSequenceEvidenceV1 event) {
        return String.join("|",
                event.getTenantId(),
                event.getPlantId(),
                event.getLineId(),
                event.getProductId(),
                event.getDeviceId(),
                event.getBindingFingerprint());
    }

    private void requireHeader(
            ConsumerRecord<byte[], byte[]> record,
            String name,
            String expected) {
        Iterator<Header> headers = record.headers().headers(name).iterator();
        if (!headers.hasNext()) {
            throw rejected("Source sequence evidence Kafka header " + name + " is required.");
        }
        String actual = decode(headers.next().value(), "Kafka header " + name);
        if (headers.hasNext()) {
            throw rejected("Source sequence evidence Kafka header " + name + " must appear exactly once.");
        }
        if (!expected.equals(actual)) {
            throw rejected("Source sequence evidence Kafka header " + name + " does not match the payload.");
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

    private void keyPart(String value, int minimum, int maximum, String field) {
        require(value, minimum, maximum, field);
        if (value.indexOf('|') >= 0) {
            throw rejected("Source sequence evidence " + field + " cannot contain '|'.");
        }
    }

    private void require(String value, int minimum, int maximum, String field) {
        if (value == null || value.length() < minimum || value.length() > maximum || value.isBlank()) {
            throw rejected("Source sequence evidence " + field + " length is invalid.");
        }
    }

    private SourceSequenceEvidenceKafkaRecordRejectedException rejected(String message) {
        return new SourceSequenceEvidenceKafkaRecordRejectedException(message);
    }

    private SourceSequenceEvidenceKafkaRecordRejectedException rejected(
            String message,
            Throwable cause) {
        return new SourceSequenceEvidenceKafkaRecordRejectedException(message, cause);
    }
}
