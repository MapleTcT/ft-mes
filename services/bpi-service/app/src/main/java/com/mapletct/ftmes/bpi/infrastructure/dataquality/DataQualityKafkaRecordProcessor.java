package com.mapletct.ftmes.bpi.infrastructure.dataquality;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mapletct.ftmes.bpi.application.DataQualityIngestResult;
import com.mapletct.ftmes.bpi.application.DataQualityIngestionService;
import com.mapletct.ftmes.bpi.contract.v1.DataQualityEventV1;
import com.mapletct.ftmes.bpi.contract.v1.DataQualitySeverity;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class DataQualityKafkaRecordProcessor {
    private static final long MAX_FUTURE_SKEW_MS = 5 * 60 * 1_000L;
    private static final Pattern CODE = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]*");
    private static final String EVENT_ID = "event_id";
    private static final String ISSUE_CODE = "issue_code";
    private static final String TENANT_ID = "tenant_id";
    private static final String SCHEMA_VERSION = "schema_version";

    private final DataQualityIngestionService service;
    private final BpiDataQualityKafkaProperties properties;

    public DataQualityKafkaRecordProcessor(
            DataQualityIngestionService service,
            BpiDataQualityKafkaProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    public DataQualityIngestResult process(ConsumerRecord<byte[], byte[]> record) {
        DataQualityEventV1 event = decodeAndValidate(record);
        return service.ingest(event, properties.actorId());
    }

    DataQualityEventV1 decodeAndValidate(ConsumerRecord<byte[], byte[]> record) {
        if (!properties.topic().equals(record.topic())) {
            throw rejected("Data-quality record arrived from an untrusted topic.");
        }
        byte[] payload = record.value();
        if (payload == null || payload.length == 0 || payload.length > properties.maxPayloadBytes()) {
            throw rejected("Data-quality Kafka payload size is invalid.");
        }
        DataQualityEventV1 event;
        try {
            event = DataQualityEventV1.parseFrom(payload);
        } catch (InvalidProtocolBufferException error) {
            throw rejected("Data-quality Kafka payload is not valid DataQualityEventV1 Protobuf.", error);
        }
        validateEvent(event);
        if (!properties.allows(event.getTenantId(), event.getPlantId(), event.getLineId())) {
            throw rejected("Data-quality Kafka event is outside the configured tenant/plant/line scope.");
        }
        requireHeader(record, EVENT_ID, event.getEventId());
        requireHeader(record, ISSUE_CODE, event.getIssueCode());
        requireHeader(record, TENANT_ID, event.getTenantId());
        requireHeader(record, SCHEMA_VERSION, "v1");
        String expectedKey = String.join("|", event.getTenantId(), event.getLineId(),
                event.getSourceEventId(), event.getPropertyId(), event.getIssueCode());
        if (!expectedKey.equals(decode(record.key(), "Kafka record key"))) {
            throw rejected("Data-quality Kafka record key does not match tenant|line|source_event|property|issue.");
        }
        return event;
    }

    private void validateEvent(DataQualityEventV1 event) {
        require(event.getEventId(), 8, 128, "event_id");
        optional(event.getSourceEventId(), 256, "source_event_id");
        require(event.getTenantId(), 1, 64, "tenant_id");
        require(event.getPlantId(), 1, 64, "plant_id");
        require(event.getLineId(), 1, 128, "line_id");
        optional(event.getDeviceId(), 128, "device_id");
        optional(event.getPropertyId(), 128, "property_id");
        require(event.getIssueCode(), 1, 128, "issue_code");
        if (!CODE.matcher(event.getIssueCode()).matches()) {
            throw rejected("Data-quality issue_code has an invalid format.");
        }
        if (event.getSeverity() == DataQualitySeverity.DATA_QUALITY_SEVERITY_UNSPECIFIED
                || event.getSeverity() == DataQualitySeverity.UNRECOGNIZED) {
            throw rejected("Data-quality severity is required.");
        }
        require(event.getDetail(), 1, 4_096, "detail");
        if (event.getDetectedAtMs() <= 0) {
            throw rejected("Data-quality detected_at_ms must be positive.");
        }
        if (event.getDetectedAtMs() > System.currentTimeMillis() + MAX_FUTURE_SKEW_MS) {
            throw rejected("Data-quality detected_at_ms is too far in the future.");
        }
        if (event.getHeadersCount() > 32) {
            throw rejected("Data-quality headers cannot contain more than 32 entries.");
        }
        for (Map.Entry<String, String> header : event.getHeadersMap().entrySet()) {
            require(header.getKey(), 1, 128, "headers.key");
            optional(header.getValue(), 1_024, "headers.value");
        }
        require(event.getHeadersOrDefault("stage", ""), 1, 128, "headers.stage");
        rejectSeparator(event.getTenantId(), "tenant_id");
        rejectSeparator(event.getPlantId(), "plant_id");
        rejectSeparator(event.getLineId(), "line_id");
        rejectSeparator(event.getSourceEventId(), "source_event_id");
        rejectSeparator(event.getDeviceId(), "device_id");
        rejectSeparator(event.getPropertyId(), "property_id");
        rejectSeparator(event.getHeadersOrDefault("stage", ""), "headers.stage");
    }

    private void requireHeader(ConsumerRecord<byte[], byte[]> record, String name, String expected) {
        Iterator<Header> headers = record.headers().headers(name).iterator();
        if (!headers.hasNext()) throw rejected("Data-quality Kafka header " + name + " is required.");
        String actual = decode(headers.next().value(), "Kafka header " + name);
        if (headers.hasNext()) {
            throw rejected("Data-quality Kafka header " + name + " must appear exactly once.");
        }
        if (!expected.equals(actual)) {
            throw rejected("Data-quality Kafka header " + name + " does not match the payload.");
        }
    }

    private String decode(byte[] value, String field) {
        if (value == null || value.length == 0) throw rejected(field + " is required.");
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(value)).toString();
        } catch (CharacterCodingException error) {
            throw rejected(field + " is not valid UTF-8.", error);
        }
    }

    private void require(String value, int minimum, int maximum, String field) {
        if (value == null || value.length() < minimum || value.length() > maximum || value.isBlank()) {
            throw rejected("Data-quality " + field + " length is invalid.");
        }
    }

    private void optional(String value, int maximum, String field) {
        if (value != null && value.length() > maximum) {
            throw rejected("Data-quality " + field + " length is invalid.");
        }
    }

    private void rejectSeparator(String value, String field) {
        if (value != null && value.indexOf('|') >= 0) {
            throw rejected("Data-quality " + field + " cannot contain '|'.");
        }
    }

    private DataQualityKafkaRecordRejectedException rejected(String message) {
        return new DataQualityKafkaRecordRejectedException(message);
    }

    private DataQualityKafkaRecordRejectedException rejected(String message, Throwable cause) {
        return new DataQualityKafkaRecordRejectedException(message, cause);
    }
}
