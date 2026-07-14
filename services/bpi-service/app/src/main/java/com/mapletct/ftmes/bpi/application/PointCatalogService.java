package com.mapletct.ftmes.bpi.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.application.error.BpiConflictException;
import com.mapletct.ftmes.bpi.application.error.BpiForbiddenException;
import com.mapletct.ftmes.bpi.application.error.BpiPreconditionRequiredException;
import com.mapletct.ftmes.bpi.application.error.BpiValidationException;
import com.mapletct.ftmes.bpi.domain.PointCatalogPointView;
import com.mapletct.ftmes.bpi.domain.PointCatalogSnapshotView;
import com.mapletct.ftmes.bpi.domain.PointCatalogView;
import com.mapletct.ftmes.bpi.domain.TopologyValidationIssue;
import com.mapletct.ftmes.bpi.infrastructure.postgres.BpiPostgresRepository;
import com.mapletct.ftmes.bpi.infrastructure.postgres.IdempotencyRecord;
import com.mapletct.ftmes.bpi.infrastructure.postgres.PointCatalogPostgresRepository;
import com.mapletct.ftmes.bpi.interfaces.rest.PointCatalogPointCommand;
import com.mapletct.ftmes.bpi.interfaces.rest.PointCatalogSnapshotCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class PointCatalogService {
    private final PointCatalogPostgresRepository repository;
    private final BpiPostgresRepository sharedRepository;
    private final CanonicalJson canonicalJson;
    private final ObjectMapper objectMapper;

    public PointCatalogService(
            PointCatalogPostgresRepository repository,
            BpiPostgresRepository sharedRepository,
            CanonicalJson canonicalJson,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.sharedRepository = sharedRepository;
        this.canonicalJson = canonicalJson;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<PointCatalogSnapshotView> listSnapshots(
            ActorContext actor, String plantId, String lineId) {
        assertRequestedScope(actor, plantId, lineId);
        return repository.listSnapshots(actor, plantId, lineId);
    }

    @Transactional(readOnly = true)
    public Optional<PointCatalogView> current(
            ActorContext actor, String plantId, String lineId) {
        assertConcreteScope(actor, plantId, lineId);
        return repository.findCurrentSnapshot(actor, plantId, lineId)
                .map(snapshot -> new PointCatalogView(snapshot, repository.listPoints(actor, snapshot)));
    }

    @Transactional(timeout = 30)
    public CommandResult<PointCatalogView> importSnapshot(
            ActorContext actor,
            String idempotencyKey,
            String ifMatch,
            PointCatalogSnapshotCommand command,
            String traceId) {
        validateHeaders(idempotencyKey, ifMatch);
        if (!"0".equals(normalizeRevision(ifMatch))) {
            throw new BpiConflictException("A point catalog snapshot must use If-Match 0.", 0L);
        }
        assertConcreteScope(actor, command.plantId(), command.lineId());
        if (command.observedAt().isAfter(Instant.now().plus(5, ChronoUnit.MINUTES))) {
            throw new BpiValidationException("Point catalog observedAt must not be in the future.");
        }
        assertUniquePoints(command.points());

        String path = "/bpi/v1/point-catalog/snapshots";
        String requestChecksum = Checksums.sha256(canonicalJson.write(command));
        CommandResult<PointCatalogView> replay = replay(
                actor, idempotencyKey, path, requestChecksum, new TypeReference<PointCatalogView>() {});
        if (replay != null) return replay;

        UUID snapshotId = UUID.randomUUID();
        String catalogChecksum = Checksums.sha256(canonicalJson.write(catalogPayload(command)));
        int readyPointCount = (int) command.points().stream()
                .filter(PointCatalogPostgresRepository::isReady)
                .count();
        repository.insertSnapshot(
                actor, snapshotId, command.source(), command.sourceInstance(), command.sourceRevision(),
                command.plantId(), command.lineId(), catalogChecksum, command.observedAt(),
                command.points().size(), readyPointCount, command.points());
        PointCatalogSnapshotView snapshot = repository.findSnapshot(actor, snapshotId);
        PointCatalogView result = new PointCatalogView(snapshot, repository.listPoints(actor, snapshot));
        repository.insertAudit(actor, snapshot, command.reason(), traceId);
        sharedRepository.completeIdempotency(actor.tenantId(), idempotencyKey, 200, writeJson(result));
        return new CommandResult<>(result, false);
    }

    @Transactional(readOnly = true)
    public BindingValidationResult validateBindings(
            ActorContext actor,
            String plantId,
            String lineId,
            Map<String, Object> definition) {
        Optional<PointCatalogSnapshotView> current = repository.findCurrentSnapshot(actor, plantId, lineId);
        if (current.isEmpty()) {
            return new BindingValidationResult(
                    null, null,
                    List.of(issue("POINT_CATALOG_SNAPSHOT_MISSING", "/bindings", "ERROR",
                            "No point catalog snapshot exists for the topology scope.")),
                    List.of());
        }

        PointCatalogSnapshotView snapshot = current.get();
        Map<String, PointCatalogPointView> points = new LinkedHashMap<>();
        for (PointCatalogPointView point : repository.listPoints(actor, snapshot)) {
            points.put(key(point.productId(), point.deviceId(), point.propertyId()), point);
        }

        JsonNode bindings = objectMapper.valueToTree(definition).path("bindings");
        List<TopologyValidationIssue> errors = new ArrayList<>();
        List<TopologyValidationIssue> warnings = new ArrayList<>();
        if (bindings.isArray()) {
            for (int index = 0; index < bindings.size(); index++) {
                JsonNode binding = bindings.get(index);
                String path = "/bindings/" + index;
                String productId = text(binding, "productId");
                String deviceId = text(binding, "deviceId");
                String propertyId = text(binding, "propertyId");
                if (productId.isBlank() || deviceId.isBlank() || propertyId.isBlank()) continue;
                PointCatalogPointView point = points.get(key(productId, deviceId, propertyId));
                if (point == null) {
                    errors.add(issue("POINT_CATALOG_BINDING_NOT_FOUND", path, "ERROR",
                            "The product/device/property binding does not exist in point catalog snapshot "
                                    + snapshot.id() + "."));
                    continue;
                }
                if (!point.registered()) {
                    errors.add(issue("POINT_DEVICE_NOT_REGISTERED", path + "/deviceId", "ERROR",
                            "The bound device is not registered."));
                }
                if (!"ACTIVE".equals(point.deviceState())) {
                    errors.add(issue("POINT_DEVICE_NOT_ACTIVE", path + "/deviceId", "ERROR",
                            "The bound device state is " + point.deviceState() + "."));
                }
                if (!point.propertyPresent()) {
                    errors.add(issue("POINT_PROPERTY_NOT_AVAILABLE", path + "/propertyId", "ERROR",
                            "The bound property is absent from the source product metadata."));
                }
                String expectedUnit = text(binding, "expectedUnit");
                if (expectedUnit.isBlank()) expectedUnit = text(binding, "unit");
                if (point.unit() == null || point.unit().isBlank()) {
                    errors.add(issue("POINT_UNIT_MISSING", path + "/expectedUnit", "ERROR",
                            "The catalog point has no source unit."));
                } else if (!expectedUnit.isBlank() && !point.unit().equalsIgnoreCase(expectedUnit)) {
                    errors.add(issue("POINT_UNIT_MISMATCH", path + "/expectedUnit", "ERROR",
                            "Expected unit " + expectedUnit + " does not match source unit " + point.unit() + "."));
                }
                String calibrationVersion = text(binding, "calibrationVersion");
                if (!"VERIFIED".equals(point.calibrationStatus())
                        || point.calibrationVersion() == null
                        || !point.calibrationVersion().equals(calibrationVersion)) {
                    errors.add(issue("POINT_CALIBRATION_NOT_VERIFIED", path + "/calibrationVersion", "ERROR",
                            "The requested calibration version is not verified in the point catalog."));
                }
                if (!point.sourceSequenceEnabled()) {
                    errors.add(issue("POINT_SOURCE_SEQUENCE_DISABLED", path, "ERROR",
                            "A device or gateway source epoch and sequence are required for replay-safe topology binding."));
                }
            }
        }
        return new BindingValidationResult(
                snapshot.id(), snapshot.checksum(), List.copyOf(errors), List.copyOf(warnings));
    }

    private Map<String, Object> catalogPayload(PointCatalogSnapshotCommand command) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("source", command.source());
        payload.put("sourceInstance", command.sourceInstance());
        payload.put("sourceRevision", command.sourceRevision());
        payload.put("plantId", command.plantId());
        payload.put("lineId", command.lineId());
        payload.put("observedAt", command.observedAt());
        payload.put("points", command.points());
        return payload;
    }

    private void assertUniquePoints(List<PointCatalogPointCommand> points) {
        Set<String> identities = new LinkedHashSet<>();
        for (PointCatalogPointCommand point : points) {
            String identity = key(point.productId(), point.deviceId(), point.propertyId());
            if (!identities.add(identity)) {
                throw new BpiValidationException("Point catalog contains duplicate identity: " + identity);
            }
        }
    }

    private <T> CommandResult<T> replay(
            ActorContext actor,
            String idempotencyKey,
            String path,
            String checksum,
            TypeReference<T> type) {
        boolean owner = sharedRepository.reserveIdempotency(
                UUID.randomUUID(), actor.tenantId(), idempotencyKey, "POST", path, checksum);
        if (owner) return null;
        IdempotencyRecord previous = sharedRepository.lockIdempotency(actor.tenantId(), idempotencyKey);
        if (!"POST".equals(previous.method()) || !path.equals(previous.resourcePath())
                || !checksum.equals(previous.requestChecksum())) {
            throw new BpiConflictException("Idempotency-Key was reused with a different request.", null);
        }
        if ("COMPLETED".equals(previous.state()) && previous.responseBody() != null) {
            return new CommandResult<>(sharedRepository.readJson(previous.responseBody(), type), true);
        }
        throw new BpiConflictException("The command is still processing.", null);
    }

    private void validateHeaders(String idempotencyKey, String ifMatch) {
        if (idempotencyKey == null || idempotencyKey.length() < 8 || ifMatch == null) {
            throw new BpiPreconditionRequiredException("Idempotency-Key and If-Match are required.");
        }
        if (idempotencyKey.length() > 128) {
            throw new BpiValidationException("Idempotency-Key must not exceed 128 characters.");
        }
        normalizeRevision(ifMatch);
    }

    private String normalizeRevision(String ifMatch) {
        String value = ifMatch == null ? "" : ifMatch.trim();
        if (value.startsWith("W/")) value = value.substring(2);
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
            value = value.substring(1, value.length() - 1);
        }
        if (!value.matches("\\d+")) {
            throw new BpiPreconditionRequiredException("If-Match must contain a numeric entity revision.");
        }
        return value;
    }

    private void assertRequestedScope(ActorContext actor, String plantId, String lineId) {
        if (plantId != null && lineId != null && !actor.canAccess(plantId, lineId)) {
            throw new BpiForbiddenException("Token scope does not allow the requested point catalog scope.");
        }
    }

    private void assertConcreteScope(ActorContext actor, String plantId, String lineId) {
        if (plantId == null || plantId.isBlank() || lineId == null || lineId.isBlank()) {
            throw new BpiValidationException("plantId and lineId are required for point catalog access.");
        }
        if (!actor.canAccess(plantId, lineId)) {
            throw new BpiForbiddenException("Token scope does not allow the requested point catalog scope.");
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize BPI point catalog command", exception);
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isTextual() ? value.asText().trim() : "";
    }

    private String key(String productId, String deviceId, String propertyId) {
        return productId + "\u0000" + deviceId + "\u0000" + propertyId;
    }

    private TopologyValidationIssue issue(String code, String path, String severity, String message) {
        return new TopologyValidationIssue(code, path, severity, message);
    }

    public record BindingValidationResult(
            UUID snapshotId,
            String snapshotChecksum,
            List<TopologyValidationIssue> errors,
            List<TopologyValidationIssue> warnings) {
    }
}
