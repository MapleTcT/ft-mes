package com.mapletct.ftmes.bpi.infrastructure.telemetry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.protobuf.InvalidProtocolBufferException;
import com.mapletct.ftmes.bpi.application.ActorContext;
import com.mapletct.ftmes.bpi.application.Checksums;
import com.mapletct.ftmes.bpi.application.TelemetryIngestionService;
import com.mapletct.ftmes.bpi.contract.validation.ContractViolation;
import com.mapletct.ftmes.bpi.contract.validation.PointRejection;
import com.mapletct.ftmes.bpi.contract.validation.TelemetryEnvelopeValidationResult;
import com.mapletct.ftmes.bpi.contract.validation.BpiContractValidator;
import com.mapletct.ftmes.bpi.contract.v1.PointValue;
import com.mapletct.ftmes.bpi.contract.v1.TelemetryEnvelopeV1;
import com.mapletct.ftmes.bpi.domain.TelemetryIngestResult;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class TelemetryKafkaRecordProcessor {
    private final TelemetryIngestionService service;
    private final BpiTelemetryKafkaProperties properties;
    private final ObjectMapper objectMapper;

    public TelemetryKafkaRecordProcessor(
            TelemetryIngestionService service,
            BpiTelemetryKafkaProperties properties,
            ObjectMapper objectMapper) {
        this.service = service;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public TelemetryIngestResult process(ConsumerRecord<byte[], byte[]> record) {
        TelemetryEnvelopeV1 event = decodeAndValidate(record);
        ActorContext actor = new ActorContext(
                event.getTenantId(),
                properties.actorId(),
                Set.of("BPI_EVENT_INGEST"),
                Set.of(event.getPlantId()),
                Set.of(event.getLineId()));
        String traceSource = record.topic() + "|" + record.partition() + "|" + record.offset();
        String traceId = "kafka-" + Checksums.sha256(traceSource).substring(0, 32);
        return service.ingestKafka(actor, toJson(event), traceId);
    }

    TelemetryEnvelopeV1 decodeAndValidate(ConsumerRecord<byte[], byte[]> record) {
        if (!properties.topic().equals(record.topic())) {
            throw rejected("Telemetry record arrived from an untrusted topic.");
        }
        byte[] payload = record.value();
        if (payload == null || payload.length == 0 || payload.length > properties.maxPayloadBytes()) {
            throw rejected("Telemetry Kafka payload size is invalid.");
        }

        TelemetryEnvelopeV1 event;
        try {
            event = TelemetryEnvelopeV1.parseFrom(payload);
        } catch (InvalidProtocolBufferException error) {
            throw rejected("Telemetry Kafka payload is not valid TelemetryEnvelopeV1 Protobuf.", error);
        }
        TelemetryEnvelopeValidationResult validation = BpiContractValidator.validate(event);
        if (!validation.isEnvelopeAccepted() || !validation.getPointRejections().isEmpty()) {
            throw rejected("Telemetry contract rejected the Kafka record: " + violations(validation));
        }
        if (!properties.allows(event.getTenantId(), event.getPlantId(), event.getLineId())) {
            throw rejected("Telemetry Kafka event is outside the configured tenant/plant/line scope.");
        }
        String expectedKey = event.getPlantId() + "|" + event.getDeviceId();
        if (!expectedKey.equals(decode(record.key(), "Kafka record key"))) {
            throw rejected("Telemetry Kafka record key does not match plant_id|device_id.");
        }
        return event;
    }

    private ObjectNode toJson(TelemetryEnvelopeV1 event) {
        ObjectNode json = objectMapper.createObjectNode();
        json.put("eventId", event.getEventId());
        json.put("messageId", event.getMessageId());
        json.put("tenantId", event.getTenantId());
        json.put("plantId", event.getPlantId());
        json.put("lineId", event.getLineId());
        json.put("gatewayId", event.getGatewayId());
        json.put("productId", event.getProductId());
        json.put("deviceId", event.getDeviceId());
        json.put("eventTimeMs", event.getEventTimeMs());
        json.put("ingestTimeMs", event.getIngestTimeMs());
        json.put("sequence", unsigned(event.getSequence()));
        json.put("sourceEpoch", unsigned(event.getSourceEpoch()));
        json.put("sequenceOrigin", event.getSequenceOrigin().name());
        ObjectNode headers = json.putObject("headers");
        event.getHeadersMap().forEach(headers::put);
        ArrayNode points = json.putArray("points");
        event.getPointsList().forEach(point -> points.add(pointJson(point)));
        return json;
    }

    private ObjectNode pointJson(PointValue point) {
        ObjectNode json = objectMapper.createObjectNode();
        json.put("propertyId", point.getPropertyId());
        switch (point.getValueCase()) {
            case DOUBLE_VALUE -> json.put("doubleValue", point.getDoubleValue());
            case LONG_VALUE -> json.put("longValue", point.getLongValue());
            case STRING_VALUE -> json.put("stringValue", point.getStringValue());
            case BOOL_VALUE -> json.put("boolValue", point.getBoolValue());
            default -> throw rejected("Telemetry point value is not set.");
        }
        json.put("unit", point.getUnit());
        json.put("qualityCode", point.getQualityCode());
        json.put("sampleTimeMs", point.getSampleTimeMs());
        if (!point.getCalibrationVersion().isBlank()) {
            json.put("calibrationVersion", point.getCalibrationVersion());
        }
        return json;
    }

    private String violations(TelemetryEnvelopeValidationResult validation) {
        String envelope = validation.getEnvelopeViolations().stream()
                .map(this::violation)
                .sorted()
                .collect(Collectors.joining(","));
        String points = validation.getPointRejections().stream()
                .map(this::rejection)
                .sorted()
                .collect(Collectors.joining(","));
        if (envelope.isEmpty()) return points;
        if (points.isEmpty()) return envelope;
        return envelope + "," + points;
    }

    private String rejection(PointRejection rejection) {
        return rejection.getViolations().stream()
                .map(this::violation)
                .sorted()
                .collect(Collectors.joining(","));
    }

    private String violation(ContractViolation violation) {
        return violation.getPath() + ":" + violation.getCode();
    }

    private BigInteger unsigned(long value) {
        return new BigInteger(Long.toUnsignedString(value));
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

    private TelemetryKafkaRecordRejectedException rejected(String message) {
        return new TelemetryKafkaRecordRejectedException(message);
    }

    private TelemetryKafkaRecordRejectedException rejected(String message, Throwable cause) {
        return new TelemetryKafkaRecordRejectedException(message, cause);
    }
}
