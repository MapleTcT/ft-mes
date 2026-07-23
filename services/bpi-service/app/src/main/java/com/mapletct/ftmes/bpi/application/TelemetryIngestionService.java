package com.mapletct.ftmes.bpi.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mapletct.ftmes.bpi.application.error.BpiConflictException;
import com.mapletct.ftmes.bpi.application.error.BpiForbiddenException;
import com.mapletct.ftmes.bpi.domain.TelemetryIngestResult;
import com.mapletct.ftmes.bpi.domain.TelemetryValue;
import com.mapletct.ftmes.bpi.infrastructure.telemetry.BpiTelemetryProperties;
import com.mapletct.ftmes.bpi.infrastructure.telemetry.TelemetryPostgresRepository;
import com.mapletct.ftmes.bpi.infrastructure.telemetry.TelemetryPostgresRepository.SourceState;
import com.mapletct.ftmes.bpi.infrastructure.telemetry.TelemetryPostgresRepository.TelemetryEvent;
import com.mapletct.ftmes.bpi.infrastructure.telemetry.TelemetryPostgresRepository.TelemetryEventSnapshot;
import com.mapletct.ftmes.bpi.interfaces.rest.TelemetryEnvelopeRequest;
import com.mapletct.ftmes.bpi.interfaces.rest.TelemetryPointRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class TelemetryIngestionService {

    private static final UUID EVENT_NAMESPACE = UUID.fromString("45fd2bdc-124c-5fe6-a487-44ee471491ab");
    private static final UUID POINT_NAMESPACE = UUID.fromString("77fdaeea-ea15-5d68-b094-721c5542c3cc");
    private static final UUID QUARANTINE_NAMESPACE = UUID.fromString("fc12b640-cf48-5362-8c14-9ff26c497fa0");
    private static final Set<String> SEQUENCE_ORIGINS = Set.of("DEVICE", "GATEWAY", "EXPORTER");
    private static final BigInteger UINT64_MAX = new BigInteger("18446744073709551615");

    private final TelemetryPostgresRepository repository;
    private final BpiTelemetryProperties properties;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final Clock clock;

    @Autowired
    public TelemetryIngestionService(TelemetryPostgresRepository repository,
                                     BpiTelemetryProperties properties,
                                     ObjectMapper objectMapper,
                                     Validator validator) {
        this(repository, properties, objectMapper, validator, Clock.systemUTC());
    }

    TelemetryIngestionService(TelemetryPostgresRepository repository,
                              BpiTelemetryProperties properties,
                              ObjectMapper objectMapper,
                              Validator validator,
                              Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.clock = clock;
    }

    @Transactional(timeout = 15)
    public TelemetryIngestResult ingest(ActorContext actor, JsonNode payload, String traceId) {
        requireIngestRole(actor);
        if (!properties.httpIngressEnabled()) {
            throw new BpiForbiddenException(
                    "Telemetry HTTP ingress is disabled; production telemetry must use the approved Kafka path.");
        }
        return ingestPayload(actor, payload, traceId);
    }

    @Transactional(timeout = 15)
    public TelemetryIngestResult ingestKafka(ActorContext actor, JsonNode payload, String traceId) {
        requireIngestRole(actor);
        return ingestPayload(actor, payload, traceId);
    }

    private TelemetryIngestResult ingestPayload(ActorContext actor, JsonNode payload, String traceId) {
        byte[] raw = writeBytes(payload);
        String checksum = Checksums.sha256(writeCanonical(payload));
        String candidateEventId = text(payload, "eventId");
        if (raw.length > properties.maxPayloadBytes()) {
            return quarantine(actor.tenantId(), candidateEventId, checksum,
                    List.of("PAYLOAD_TOO_LARGE"), oversizedPayload(raw.length, checksum), traceId);
        }

        TelemetryEnvelopeRequest envelope;
        try {
            envelope = objectMapper.treeToValue(payload, TelemetryEnvelopeRequest.class);
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            return quarantine(actor.tenantId(), candidateEventId, checksum,
                    List.of("ENVELOPE_DECODE_ERROR"), payload, traceId);
        }

        List<String> envelopeErrors = validateEnvelope(envelope);
        if (!envelopeErrors.isEmpty()) {
            return quarantine(actor.tenantId(), envelope == null ? candidateEventId : envelope.eventId(),
                    checksum, envelopeErrors, payload, traceId);
        }
        if (!actor.tenantId().equals(envelope.tenantId())) {
            throw new BpiForbiddenException("Telemetry tenant does not match the trusted token.");
        }
        if (!actor.canAccess(envelope.plantId(), envelope.lineId())) {
            throw new BpiForbiddenException("Token scope does not allow this telemetry plant/line.");
        }

        repository.lockEventIdentity(actor.tenantId(), envelope.eventId());
        TelemetryEventSnapshot existing = repository.findEvent(actor.tenantId(), envelope.eventId());
        if (existing != null) {
            if (!existing.payloadChecksum().equals(checksum)) {
                throw new BpiConflictException("Telemetry eventId was reused with a different payload.", null);
            }
            return result(existing, true, traceId);
        }
        Instant eventTime = Instant.ofEpochMilli(envelope.eventTimeMs());
        SequenceDecision sequence = decideSequence(actor.tenantId(), envelope, eventTime);
        if (sequence.quarantineReason() != null) {
            return quarantine(actor.tenantId(), envelope.eventId(), checksum,
                    List.of(sequence.quarantineReason()), payload, traceId);
        }
        TelemetryEventSnapshot sourceIdentity = repository.findSourceIdentity(
                actor.tenantId(), envelope.gatewayId(), envelope.deviceId(), envelope.sourceEpoch(), envelope.sequence());
        if (sourceIdentity != null) {
            throw new BpiConflictException("Telemetry source epoch/sequence is already owned by another event.", null);
        }

        List<AcceptedPoint> accepted = new ArrayList<>();
        List<RejectedPoint> rejected = new ArrayList<>();
        Set<String> propertiesSeen = new HashSet<>();
        for (int index = 0; index < envelope.points().size(); index++) {
            JsonNode rawPoint = envelope.points().get(index);
            PointDecision point = validatePoint(rawPoint, propertiesSeen);
            if (point.value() != null) {
                accepted.add(new AcceptedPoint(index, point.value()));
            } else {
                rejected.add(new RejectedPoint(index, text(rawPoint, "propertyId"), point.reasons(), rawPoint));
            }
        }

        String status = status(accepted.size(), rejected.size());
        UUID eventUuid = UuidV5.from(EVENT_NAMESPACE, actor.tenantId() + "|" + envelope.eventId());
        repository.insertEvent(new TelemetryEvent(
                eventUuid, actor.tenantId(), envelope.plantId(), envelope.lineId(), envelope.gatewayId(),
                envelope.productId(), envelope.deviceId(), envelope.eventId(), envelope.messageId(),
                eventTime, Instant.ofEpochMilli(envelope.ingestTimeMs()), envelope.sourceEpoch(), envelope.sequence(),
                envelope.sequenceOrigin(), sequence.disposition(), checksum, writeJson(envelope.headers()),
                envelope.points().size(), accepted.size(), rejected.size(), status));

        for (AcceptedPoint item : accepted) {
            repository.insertPoint(UuidV5.from(POINT_NAMESPACE, eventUuid + "|accepted|" + item.index()),
                    actor.tenantId(), eventUuid, envelope.eventId(), item.value());
            repository.upsertLatestPoint(
                    eventUuid,
                    actor.tenantId(),
                    envelope.plantId(),
                    envelope.lineId(),
                    envelope.gatewayId(),
                    envelope.productId(),
                    envelope.deviceId(),
                    envelope.eventId(),
                    envelope.sourceEpoch(),
                    envelope.sequence(),
                    envelope.sequenceOrigin(),
                    sequence.disposition(),
                    item.value());
        }
        for (RejectedPoint item : rejected) {
            repository.insertPointReject(UuidV5.from(POINT_NAMESPACE, eventUuid + "|rejected|" + item.index()),
                    actor.tenantId(), eventUuid, envelope.eventId(), item.index(), item.propertyId(),
                    writeJson(item.reasons()), writeJson(item.rawPoint()));
        }
        sequence.apply().run();
        return new TelemetryIngestResult(
                envelope.eventId(), status, sequence.disposition(), accepted.size(), rejected.size(), false, traceId);
    }

    private SequenceDecision decideSequence(String tenantId, TelemetryEnvelopeRequest envelope, Instant eventTime) {
        repository.insertSourceIfAbsent(tenantId, envelope.gatewayId(), envelope.deviceId(),
                envelope.sourceEpoch(), envelope.sequence(), envelope.eventId(), eventTime);
        SourceState state = repository.lockSource(tenantId, envelope.gatewayId(), envelope.deviceId());
        if (state == null) throw new IllegalStateException("Telemetry source state was not created");

        int epochComparison = envelope.sourceEpoch().compareTo(state.sourceEpoch());
        if (state.lastEventId().equals(envelope.eventId())) {
            return new SequenceDecision("FIRST", null, () -> { });
        }
        if (epochComparison < 0) {
            return new SequenceDecision("STALE_EPOCH", "SOURCE_EPOCH_REGRESSION", () -> { });
        }
        if (epochComparison > 0) {
            return new SequenceDecision("EPOCH_ADVANCED", null,
                    () -> repository.advanceSource(tenantId, envelope.gatewayId(), envelope.deviceId(),
                            envelope.sourceEpoch(), envelope.sequence(), envelope.eventId(), eventTime, state.revision()));
        }

        int sequenceComparison = envelope.sequence().compareTo(state.lastSequence());
        if (sequenceComparison == 0) {
            throw new BpiConflictException(
                    "Telemetry source epoch/sequence is already owned by another event.", null);
        }
        if (sequenceComparison < 0) {
            return new SequenceDecision("OUT_OF_ORDER", null, () -> { });
        }
        BigInteger delta = envelope.sequence().subtract(state.lastSequence());
        String disposition = delta.equals(BigInteger.ONE) ? "IN_ORDER" : "GAP";
        return new SequenceDecision(disposition, null,
                () -> repository.advanceSource(tenantId, envelope.gatewayId(), envelope.deviceId(),
                        envelope.sourceEpoch(), envelope.sequence(), envelope.eventId(), eventTime, state.revision()));
    }

    private List<String> validateEnvelope(TelemetryEnvelopeRequest envelope) {
        List<String> errors = new ArrayList<>();
        if (envelope == null) {
            return List.of("ENVELOPE_REQUIRED");
        }
        for (ConstraintViolation<TelemetryEnvelopeRequest> violation : validator.validate(envelope)) {
            errors.add("INVALID_" + violation.getPropertyPath().toString().toUpperCase());
        }
        if (envelope.points() != null && envelope.points().size() > properties.maxPointsPerEnvelope()) {
            errors.add("TOO_MANY_POINTS");
        }
        if (!SEQUENCE_ORIGINS.contains(envelope.sequenceOrigin())) errors.add("INVALID_SEQUENCE_ORIGIN");
        if (envelope.sourceEpoch() != null && envelope.sourceEpoch().compareTo(UINT64_MAX) > 0) {
            errors.add("SOURCE_EPOCH_OUT_OF_RANGE");
        }
        if (envelope.sequence() != null && envelope.sequence().compareTo(UINT64_MAX) > 0) {
            errors.add("SEQUENCE_OUT_OF_RANGE");
        }
        Instant latest = clock.instant().plus(properties.maxFutureSkew());
        if (envelope.eventTimeMs() > 0 && Instant.ofEpochMilli(envelope.eventTimeMs()).isAfter(latest)) {
            errors.add("EVENT_TIME_IN_FUTURE");
        }
        if (envelope.ingestTimeMs() > 0 && Instant.ofEpochMilli(envelope.ingestTimeMs()).isAfter(latest)) {
            errors.add("INGEST_TIME_IN_FUTURE");
        }
        if (envelope.eventTimeMs() > 0 && envelope.ingestTimeMs() > 0
                && envelope.eventTimeMs() > envelope.ingestTimeMs() + properties.maxFutureSkew().toMillis()) {
            errors.add("EVENT_TIME_AFTER_INGEST_TIME");
        }
        Collections.sort(errors);
        return errors;
    }

    private PointDecision validatePoint(JsonNode rawPoint, Set<String> propertiesSeen) {
        List<String> errors = new ArrayList<>();
        TelemetryPointRequest point;
        try {
            point = objectMapper.treeToValue(rawPoint, TelemetryPointRequest.class);
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            return new PointDecision(null, List.of("POINT_DECODE_ERROR"));
        }
        if (point.propertyId() == null || !point.propertyId().matches("[A-Za-z0-9_.:-]{1,128}")) {
            errors.add("INVALID_PROPERTY_ID");
        } else if (!propertiesSeen.add(point.propertyId())) {
            errors.add("DUPLICATE_PROPERTY_ID");
        }
        if (point.unit() == null || !properties.acceptedUnits().contains(point.unit())) errors.add("UNKNOWN_UNIT");
        if (point.qualityCode() == null || !properties.acceptedQualities().contains(point.qualityCode())) {
            errors.add("INVALID_QUALITY");
        }
        if (point.sampleTimeMs() <= 0) {
            errors.add("INVALID_SAMPLE_TIME");
        } else if (Instant.ofEpochMilli(point.sampleTimeMs()).isAfter(clock.instant().plus(properties.maxFutureSkew()))) {
            errors.add("SAMPLE_TIME_IN_FUTURE");
        }
        int values = (point.doubleValue() == null ? 0 : 1) + (point.longValue() == null ? 0 : 1)
                + (point.stringValue() == null ? 0 : 1) + (point.boolValue() == null ? 0 : 1);
        if (values != 1) errors.add("VALUE_ONEOF_VIOLATION");
        if (point.doubleValue() != null && !Double.isFinite(point.doubleValue())) errors.add("NON_FINITE_VALUE");
        if (point.stringValue() != null && point.stringValue().length() > 2048) errors.add("STRING_VALUE_TOO_LONG");
        if (point.calibrationVersion() != null && point.calibrationVersion().length() > 64) {
            errors.add("CALIBRATION_VERSION_TOO_LONG");
        }
        if (!errors.isEmpty()) {
            Collections.sort(errors);
            return new PointDecision(null, errors);
        }
        TelemetryValue value;
        Instant sampleTime = Instant.ofEpochMilli(point.sampleTimeMs());
        if (point.doubleValue() != null) {
            value = new TelemetryValue(point.propertyId(), "DOUBLE", BigDecimal.valueOf(point.doubleValue()),
                    null, null, point.unit(), point.qualityCode(), sampleTime, point.calibrationVersion());
        } else if (point.longValue() != null) {
            value = new TelemetryValue(point.propertyId(), "LONG", BigDecimal.valueOf(point.longValue()),
                    null, null, point.unit(), point.qualityCode(), sampleTime, point.calibrationVersion());
        } else if (point.stringValue() != null) {
            value = new TelemetryValue(point.propertyId(), "STRING", null,
                    point.stringValue(), null, point.unit(), point.qualityCode(), sampleTime, point.calibrationVersion());
        } else {
            value = new TelemetryValue(point.propertyId(), "BOOLEAN", null,
                    null, point.boolValue(), point.unit(), point.qualityCode(), sampleTime, point.calibrationVersion());
        }
        return new PointDecision(value, List.of());
    }

    private TelemetryIngestResult quarantine(String tenantId, String eventId, String checksum,
                                             List<String> reasons, JsonNode rawPayload, String traceId) {
        repository.insertQuarantine(UuidV5.from(QUARANTINE_NAMESPACE, tenantId + "|" + checksum),
                tenantId, eventId, checksum, writeJson(reasons), writeJson(rawPayload), traceId);
        return new TelemetryIngestResult(eventId, "QUARANTINED", "NOT_APPLICABLE", 0, 0, false, traceId);
    }

    private TelemetryIngestResult result(TelemetryEventSnapshot event, boolean replay, String traceId) {
        return new TelemetryIngestResult(event.eventId(), event.status(), event.sequenceDisposition(),
                event.acceptedPointCount(), event.rejectedPointCount(), replay, traceId);
    }

    private void requireIngestRole(ActorContext actor) {
        if (!actor.roles().contains("BPI_EVENT_INGEST")) {
            throw new BpiForbiddenException("BPI_EVENT_INGEST role is required for telemetry ingestion.");
        }
    }

    private String status(int accepted, int rejected) {
        if (accepted == 0 && rejected == 0) return "EMPTY";
        if (accepted == 0) return "REJECTED_POINTS_ONLY";
        if (rejected > 0) return "PARTIAL";
        return "ACCEPTED";
    }

    private JsonNode oversizedPayload(int sizeBytes, String checksum) {
        ObjectNode summary = objectMapper.createObjectNode();
        summary.put("truncated", true);
        summary.put("sizeBytes", sizeBytes);
        summary.put("sha256", checksum);
        return summary;
    }

    private String writeCanonical(JsonNode node) {
        return writeJson(canonical(node));
    }

    private JsonNode canonical(JsonNode node) {
        if (node == null || node.isValueNode()) return node;
        if (node.isArray()) {
            ArrayNode array = objectMapper.createArrayNode();
            node.forEach(item -> array.add(canonical(item)));
            return array;
        }
        ObjectNode object = objectMapper.createObjectNode();
        List<Map.Entry<String, JsonNode>> fields = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();
        iterator.forEachRemaining(fields::add);
        fields.sort(Comparator.comparing(Map.Entry::getKey));
        fields.forEach(entry -> object.set(entry.getKey(), canonical(entry.getValue())));
        return object;
    }

    private byte[] writeBytes(JsonNode payload) {
        try {
            return objectMapper.writeValueAsBytes(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Telemetry payload cannot be serialized", exception);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Telemetry audit JSON cannot be serialized", exception);
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private record AcceptedPoint(int index, TelemetryValue value) {
    }

    private record RejectedPoint(int index, String propertyId, List<String> reasons, JsonNode rawPoint) {
    }

    private record PointDecision(TelemetryValue value, List<String> reasons) {
    }

    private record SequenceDecision(String disposition, String quarantineReason, Runnable apply) {
    }
}
