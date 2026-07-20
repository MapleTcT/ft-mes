package com.mapletct.ftmes.bpi.infrastructure.pointcatalog;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mapletct.ftmes.bpi.application.ActorContext;
import com.mapletct.ftmes.bpi.application.PointCatalogService;
import com.mapletct.ftmes.bpi.contract.v1.PointCalibrationStatusV1;
import com.mapletct.ftmes.bpi.contract.v1.PointCatalogPointV1;
import com.mapletct.ftmes.bpi.contract.v1.PointCatalogSnapshotV1;
import com.mapletct.ftmes.bpi.contract.v1.PointDeviceStateV1;
import com.mapletct.ftmes.bpi.contract.v1.SequenceOrigin;
import com.mapletct.ftmes.bpi.domain.PointCatalogView;
import com.mapletct.ftmes.bpi.interfaces.rest.PointCatalogPointCommand;
import com.mapletct.ftmes.bpi.interfaces.rest.PointCatalogSnapshotCommand;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class PointCatalogKafkaRecordProcessor {
    private static final Pattern REVISION = Pattern.compile("sha256:[0-9a-f]{64}");
    private static final Pattern SOURCE = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]*");
    private static final Pattern BINDING_FINGERPRINT = Pattern.compile("sha256:[0-9a-f]{64}");
    private static final String EVENT_ID = "event_id";
    private static final String TENANT_ID = "tenant_id";
    private static final String SOURCE_REVISION = "source_revision";
    private static final String SCHEMA_VERSION = "schema_version";

    private final PointCatalogService service;
    private final BpiPointCatalogKafkaProperties properties;

    public PointCatalogKafkaRecordProcessor(
            PointCatalogService service,
            BpiPointCatalogKafkaProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    public PointCatalogView process(ConsumerRecord<byte[], byte[]> record) {
        PointCatalogSnapshotV1 event = decodeAndValidate(record);
        ActorContext actor = new ActorContext(
                event.getTenantId(),
                properties.actorId(),
                Set.of("BPI_ADMIN"),
                Set.of(event.getPlantId()),
                Set.of(event.getLineId()));
        return service.importSnapshot(
                actor,
                event.getEventId(),
                "0",
                command(event),
                event.getEventId()).data();
    }

    PointCatalogSnapshotV1 decodeAndValidate(ConsumerRecord<byte[], byte[]> record) {
        if (!properties.topic().equals(record.topic())) {
            throw rejected("Point catalog record arrived from an untrusted topic.");
        }
        byte[] payload = record.value();
        if (payload == null || payload.length == 0 || payload.length > properties.maxPayloadBytes()) {
            throw rejected("Point catalog Kafka payload size is invalid.");
        }

        PointCatalogSnapshotV1 event;
        try {
            event = PointCatalogSnapshotV1.parseFrom(payload);
        } catch (InvalidProtocolBufferException error) {
            throw rejected("Point catalog Kafka payload is not valid PointCatalogSnapshotV1 Protobuf.", error);
        }
        validateEvent(event);
        validateContentIdentity(event);
        if (!properties.allows(event.getTenantId(), event.getPlantId(), event.getLineId())) {
            throw rejected("Point catalog Kafka event is outside the configured tenant/plant/line scope.");
        }

        requireHeader(record, EVENT_ID, event.getEventId());
        requireHeader(record, TENANT_ID, event.getTenantId());
        requireHeader(record, SOURCE_REVISION, event.getSourceRevision());
        requireHeader(record, SCHEMA_VERSION, "v1");
        String expectedKey = String.join(
                "|", event.getTenantId(), event.getPlantId(), event.getLineId(), event.getSourceInstance());
        if (!expectedKey.equals(decode(record.key(), "Kafka record key"))) {
            throw rejected("Point catalog Kafka record key does not match tenant|plant|line|source_instance.");
        }
        return event;
    }

    private void validateContentIdentity(PointCatalogSnapshotV1 event) {
        PointCatalogSnapshotV1 content = PointCatalogSnapshotV1.newBuilder()
                .setSource(event.getSource())
                .setSourceInstance(event.getSourceInstance())
                .setTenantId(event.getTenantId())
                .setPlantId(event.getPlantId())
                .setLineId(event.getLineId())
                .addAllPoints(event.getPointsList())
                .build();
        String digest = sha256(content.toByteArray());
        String expectedRevision = "sha256:" + digest;
        if (!expectedRevision.equals(event.getSourceRevision())) {
            throw rejected("Point catalog source_revision does not match the canonical payload SHA-256.");
        }
        String legacyEventId = "point-catalog-" + digest;
        String observedEventId = legacyEventId + "-" + event.getObservedAtMs();
        if (!legacyEventId.equals(event.getEventId()) && !observedEventId.equals(event.getEventId())) {
            throw rejected("Point catalog event_id does not match the canonical payload SHA-256.");
        }
    }

    private String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is not available", error);
        }
    }

    private void validateEvent(PointCatalogSnapshotV1 event) {
        require(event.getEventId(), 8, 128, "event_id");
        require(event.getSource(), 1, 64, "source");
        if (!SOURCE.matcher(event.getSource()).matches()) {
            throw rejected("Point catalog source has an invalid format.");
        }
        require(event.getSourceInstance(), 1, 128, "source_instance");
        require(event.getSourceRevision(), 1, 128, "source_revision");
        if (!REVISION.matcher(event.getSourceRevision()).matches()) {
            throw rejected("Point catalog source_revision must be a lowercase SHA-256 revision.");
        }
        require(event.getTenantId(), 1, 128, "tenant_id");
        require(event.getPlantId(), 1, 64, "plant_id");
        require(event.getLineId(), 1, 128, "line_id");
        require(event.getReason(), 3, 500, "reason");
        if (event.getObservedAtMs() <= 0) {
            throw rejected("Point catalog observed_at_ms must be positive.");
        }
        if (event.getPointsCount() > 10_000) {
            throw rejected("Point catalog cannot contain more than 10000 points.");
        }
        for (PointCatalogPointV1 point : event.getPointsList()) {
            validatePoint(point);
        }
    }

    private void validatePoint(PointCatalogPointV1 point) {
        optional(point.getLocalityGroup(), 128, "point.locality_group");
        require(point.getProductId(), 1, 128, "point.product_id");
        require(point.getDeviceId(), 1, 128, "point.device_id");
        require(point.getPropertyId(), 1, 128, "point.property_id");
        optional(point.getSourcePropertyId(), 128, "point.source_property_id");
        optional(point.getPointName(), 256, "point.point_name");
        optional(point.getUnit(), 32, "point.unit");
        optional(point.getDataType(), 64, "point.data_type");
        optional(point.getCalibrationVersion(), 128, "point.calibration_version");
        if (point.getDeviceState() == PointDeviceStateV1.POINT_DEVICE_STATE_UNSPECIFIED
                || point.getDeviceState() == PointDeviceStateV1.UNRECOGNIZED) {
            throw rejected("Point catalog point.device_state is required.");
        }
        if (point.getCalibrationStatus() == PointCalibrationStatusV1.POINT_CALIBRATION_STATUS_UNSPECIFIED
                || point.getCalibrationStatus() == PointCalibrationStatusV1.UNRECOGNIZED) {
            throw rejected("Point catalog point.calibration_status is required.");
        }
        validateSourceSequenceBinding(point);
    }

    private void validateSourceSequenceBinding(PointCatalogPointV1 point) {
        SequenceOrigin origin = point.getSourceSequenceOrigin();
        boolean hasOrigin = origin == SequenceOrigin.DEVICE || origin == SequenceOrigin.GATEWAY;
        boolean hasFingerprint = !point.getSourceSequenceBindingFingerprint().isBlank();
        if (origin == SequenceOrigin.EXPORTER || origin == SequenceOrigin.UNRECOGNIZED) {
            throw rejected("Point catalog point.source_sequence_origin must be DEVICE or GATEWAY.");
        }
        if (hasOrigin != hasFingerprint) {
            throw rejected("Point catalog source sequence origin and binding fingerprint must be provided together.");
        }
        if (hasFingerprint
                && !BINDING_FINGERPRINT.matcher(point.getSourceSequenceBindingFingerprint()).matches()) {
            throw rejected("Point catalog source sequence binding fingerprint is invalid.");
        }
        if (point.getSourceSequenceRequired() && !hasOrigin) {
            throw rejected("Point catalog required source sequence binding is incomplete.");
        }
        if (point.getSourceSequenceEnabled() && hasOrigin && !point.getSourceSequenceRequired()) {
            throw rejected("Point catalog enabled source sequence must be required by the binding.");
        }
    }

    private PointCatalogSnapshotCommand command(PointCatalogSnapshotV1 event) {
        List<PointCatalogPointCommand> points = event.getPointsList().stream()
                .map(this::command)
                .toList();
        return new PointCatalogSnapshotCommand(
                event.getSource(),
                event.getSourceInstance(),
                event.getSourceRevision(),
                event.getPlantId(),
                event.getLineId(),
                Instant.ofEpochMilli(event.getObservedAtMs()),
                points,
                event.getReason());
    }

    private PointCatalogPointCommand command(PointCatalogPointV1 point) {
        return new PointCatalogPointCommand(
                blankToNull(point.getLocalityGroup()),
                point.getProductId(),
                point.getDeviceId(),
                point.getPropertyId(),
                blankToNull(point.getSourcePropertyId()),
                blankToNull(point.getPointName()),
                blankToNull(point.getUnit()),
                blankToNull(point.getDataType()),
                switch (point.getDeviceState()) {
                    case POINT_DEVICE_ACTIVE -> "ACTIVE";
                    case POINT_DEVICE_INACTIVE -> "INACTIVE";
                    case POINT_DEVICE_UNKNOWN -> "UNKNOWN";
                    default -> throw rejected("Point catalog point.device_state is invalid.");
                },
                point.getRegistered(),
                point.getPropertyPresent(),
                blankToNull(point.getCalibrationVersion()),
                switch (point.getCalibrationStatus()) {
                    case POINT_CALIBRATION_VERIFIED -> "VERIFIED";
                    case POINT_CALIBRATION_UNVERIFIED -> "UNVERIFIED";
                    case POINT_CALIBRATION_MISSING -> "MISSING";
                    default -> throw rejected("Point catalog point.calibration_status is invalid.");
                },
                point.getSourceSequenceEnabled(),
                point.getSourceSequenceRequired(),
                switch (point.getSourceSequenceOrigin()) {
                    case DEVICE -> "DEVICE";
                    case GATEWAY -> "GATEWAY";
                    case SEQUENCE_ORIGIN_UNSPECIFIED -> null;
                    default -> throw rejected("Point catalog point.source_sequence_origin is invalid.");
                },
                blankToNull(point.getSourceSequenceBindingFingerprint()));
    }

    private void requireHeader(ConsumerRecord<byte[], byte[]> record, String name, String expected) {
        Iterator<Header> headers = record.headers().headers(name).iterator();
        if (!headers.hasNext()) {
            throw rejected("Point catalog Kafka header " + name + " is required.");
        }
        String actual = decode(headers.next().value(), "Kafka header " + name);
        if (headers.hasNext()) {
            throw rejected("Point catalog Kafka header " + name + " must appear exactly once.");
        }
        if (!expected.equals(actual)) {
            throw rejected("Point catalog Kafka header " + name + " does not match the payload.");
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

    private void require(String value, int minimum, int maximum, String field) {
        if (value == null || value.length() < minimum || value.length() > maximum || value.isBlank()) {
            throw rejected("Point catalog " + field + " length is invalid.");
        }
    }

    private void optional(String value, int maximum, String field) {
        if (value != null && value.length() > maximum) {
            throw rejected("Point catalog " + field + " length is invalid.");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private PointCatalogKafkaRecordRejectedException rejected(String message) {
        return new PointCatalogKafkaRecordRejectedException(message);
    }

    private PointCatalogKafkaRecordRejectedException rejected(String message, Throwable cause) {
        return new PointCatalogKafkaRecordRejectedException(message, cause);
    }
}
