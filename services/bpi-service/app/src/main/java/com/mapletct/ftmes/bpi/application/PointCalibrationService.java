package com.mapletct.ftmes.bpi.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.application.error.BpiConflictException;
import com.mapletct.ftmes.bpi.application.error.BpiForbiddenException;
import com.mapletct.ftmes.bpi.application.error.BpiPreconditionRequiredException;
import com.mapletct.ftmes.bpi.application.error.BpiValidationException;
import com.mapletct.ftmes.bpi.domain.PointCalibrationView;
import com.mapletct.ftmes.bpi.infrastructure.postgres.BpiPostgresRepository;
import com.mapletct.ftmes.bpi.infrastructure.postgres.IdempotencyRecord;
import com.mapletct.ftmes.bpi.infrastructure.postgres.PointCalibrationPostgresRepository;
import com.mapletct.ftmes.bpi.interfaces.rest.PointCalibrationSubmitCommand;
import com.mapletct.ftmes.bpi.interfaces.rest.ReasonCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PointCalibrationService {
    private static final Pattern REVISION_HEADER = Pattern.compile("^(?:W/)?\\\"?(\\d+)\\\"?$");
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 200;

    private final PointCalibrationPostgresRepository repository;
    private final BpiPostgresRepository sharedRepository;
    private final CanonicalJson canonicalJson;
    private final ObjectMapper objectMapper;
    private final PointCalibrationCursorCodec cursorCodec;

    public PointCalibrationService(
            PointCalibrationPostgresRepository repository,
            BpiPostgresRepository sharedRepository,
            CanonicalJson canonicalJson,
            ObjectMapper objectMapper,
            PointCalibrationCursorCodec cursorCodec) {
        this.repository = repository;
        this.sharedRepository = sharedRepository;
        this.canonicalJson = canonicalJson;
        this.objectMapper = objectMapper;
        this.cursorCodec = cursorCodec;
    }

    @Transactional(readOnly = true)
    public PointCalibrationPage list(
            ActorContext actor,
            String plantId,
            String lineId,
            String productId,
            String deviceId,
            String propertyId,
            String encodedCursor,
            Integer requestedLimit) {
        assertConcreteScope(actor, plantId, lineId);
        int limit = pageSize(requestedLimit);
        String scopeFingerprint = scopeFingerprint(
                actor, plantId, lineId, productId, deviceId, propertyId);
        PointCalibrationCursorCodec.Cursor cursor = encodedCursor == null || encodedCursor.isBlank()
                ? null
                : cursorCodec.decode(encodedCursor, scopeFingerprint);
        Instant snapshotAt = cursor == null
                ? repository.currentTransactionTime()
                : cursor.snapshotAt();
        List<PointCalibrationView> values = repository.list(
                actor,
                plantId,
                lineId,
                productId,
                deviceId,
                propertyId,
                snapshotAt,
                cursor == null ? null : cursor.submittedAt(),
                cursor == null ? null : cursor.id(),
                limit + 1);
        boolean hasMore = values.size() > limit;
        List<PointCalibrationView> items = hasMore ? values.subList(0, limit) : values;
        String nextCursor = null;
        if (hasMore) {
            PointCalibrationView last = items.get(items.size() - 1);
            nextCursor = cursorCodec.encode(
                    new PointCalibrationCursorCodec.Cursor(snapshotAt, last.submittedAt(), last.id()),
                    scopeFingerprint);
        }
        return new PointCalibrationPage(items, snapshotAt, nextCursor);
    }

    @Transactional(timeout = 15)
    public CommandResult<PointCalibrationView> submit(
            ActorContext actor,
            String idempotencyKey,
            String ifMatch,
            PointCalibrationSubmitCommand command,
            String traceId) {
        validateHeaders(idempotencyKey, ifMatch);
        long expectedRevision = parseRevision(ifMatch);
        if (expectedRevision != 0) {
            throw new BpiConflictException("A new point calibration must use If-Match 0.", 0L);
        }
        assertConcreteScope(actor, command.plantId(), command.lineId());
        if (!command.validUntil().isAfter(command.validFrom())) {
            throw new BpiValidationException("Calibration validUntil must be after validFrom.");
        }
        if (!command.validUntil().isAfter(Instant.now())) {
            throw new BpiValidationException("Expired calibration evidence cannot be submitted.");
        }

        String path = "/bpi/v1/point-calibrations";
        String checksum = Checksums.sha256(canonicalJson.write(command));
        CommandResult<PointCalibrationView> replay = replay(
                actor, idempotencyKey, path, checksum, new TypeReference<PointCalibrationView>() {});
        if (replay != null) return replay;

        UUID id = UUID.randomUUID();
        repository.insertPending(actor, id, command);
        PointCalibrationView submitted = repository.find(actor, id);
        repository.insertAudit(actor, submitted, "POINT_CALIBRATION_SUBMITTED", 0, 1,
                command.reason(), traceId);
        sharedRepository.completeIdempotency(actor.tenantId(), idempotencyKey, 200, writeJson(submitted));
        return new CommandResult<>(submitted, false);
    }

    @Transactional(timeout = 15)
    public CommandResult<PointCalibrationView> approve(
            ActorContext actor,
            UUID calibrationId,
            String idempotencyKey,
            String ifMatch,
            ReasonCommand command,
            String traceId) {
        return decide(actor, calibrationId, idempotencyKey, ifMatch, command,
                traceId, "APPROVED", "approve", "POINT_CALIBRATION_APPROVED");
    }

    @Transactional(timeout = 15)
    public CommandResult<PointCalibrationView> reject(
            ActorContext actor,
            UUID calibrationId,
            String idempotencyKey,
            String ifMatch,
            ReasonCommand command,
            String traceId) {
        return decide(actor, calibrationId, idempotencyKey, ifMatch, command,
                traceId, "REJECTED", "reject", "POINT_CALIBRATION_REJECTED");
    }

    @Transactional(timeout = 15)
    public CommandResult<PointCalibrationView> revoke(
            ActorContext actor,
            UUID calibrationId,
            String idempotencyKey,
            String ifMatch,
            ReasonCommand command,
            String traceId) {
        validateHeaders(idempotencyKey, ifMatch);
        long expectedRevision = parseRevision(ifMatch);
        String path = "/bpi/v1/point-calibrations/" + calibrationId + "/revoke";
        String checksum = Checksums.sha256(
                calibrationId + "|" + expectedRevision + "|" + canonicalJson.write(command));
        CommandResult<PointCalibrationView> replay = replay(
                actor, idempotencyKey, path, checksum, new TypeReference<PointCalibrationView>() {});
        if (replay != null) return replay;

        PointCalibrationView locked = repository.lock(actor, calibrationId);
        assertRevisionAndState(locked, expectedRevision, "APPROVED");
        repository.revoke(actor, calibrationId, expectedRevision, command.reason());
        PointCalibrationView revoked = repository.find(actor, calibrationId);
        repository.insertAudit(actor, revoked, "POINT_CALIBRATION_REVOKED",
                expectedRevision, revoked.revision(), command.reason(), traceId);
        sharedRepository.completeIdempotency(actor.tenantId(), idempotencyKey, 200, writeJson(revoked));
        return new CommandResult<>(revoked, false);
    }

    private CommandResult<PointCalibrationView> decide(
            ActorContext actor,
            UUID calibrationId,
            String idempotencyKey,
            String ifMatch,
            ReasonCommand command,
            String traceId,
            String targetState,
            String operation,
            String auditAction) {
        validateHeaders(idempotencyKey, ifMatch);
        long expectedRevision = parseRevision(ifMatch);
        String path = "/bpi/v1/point-calibrations/" + calibrationId + "/" + operation;
        String checksum = Checksums.sha256(
                calibrationId + "|" + expectedRevision + "|" + canonicalJson.write(command));
        CommandResult<PointCalibrationView> replay = replay(
                actor, idempotencyKey, path, checksum, new TypeReference<PointCalibrationView>() {});
        if (replay != null) return replay;

        PointCalibrationView locked = repository.lock(actor, calibrationId);
        assertRevisionAndState(locked, expectedRevision, "PENDING");
        if (actor.userId().equals(locked.submittedBy())) {
            throw new BpiValidationException(
                    "Point calibration decisions require a reviewer other than the submitter.");
        }
        if ("APPROVED".equals(targetState) && !locked.validUntil().isAfter(Instant.now())) {
            throw new BpiValidationException("Expired calibration evidence cannot be approved.");
        }
        repository.decide(actor, calibrationId, targetState, expectedRevision, command.reason());
        PointCalibrationView decided = repository.find(actor, calibrationId);
        repository.insertAudit(actor, decided, auditAction,
                expectedRevision, decided.revision(), command.reason(), traceId);
        sharedRepository.completeIdempotency(actor.tenantId(), idempotencyKey, 200, writeJson(decided));
        return new CommandResult<>(decided, false);
    }

    private void assertRevisionAndState(
            PointCalibrationView calibration,
            long expectedRevision,
            String expectedState) {
        if (calibration.revision() != expectedRevision) {
            throw new BpiConflictException("Point calibration revision is stale.", calibration.revision());
        }
        if (!expectedState.equals(calibration.state())) {
            throw new BpiConflictException(
                    "Point calibration must be " + expectedState + " for this command.", calibration.revision());
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
        parseRevision(ifMatch);
    }

    private long parseRevision(String ifMatch) {
        Matcher matcher = REVISION_HEADER.matcher(ifMatch == null ? "" : ifMatch.trim());
        if (!matcher.matches()) {
            throw new BpiPreconditionRequiredException("If-Match must contain a numeric entity revision.");
        }
        return Long.parseLong(matcher.group(1));
    }

    private void assertConcreteScope(ActorContext actor, String plantId, String lineId) {
        if (plantId == null || plantId.isBlank() || lineId == null || lineId.isBlank()) {
            throw new BpiValidationException("plantId and lineId are required for calibration access.");
        }
        if (!actor.canAccess(plantId, lineId)) {
            throw new BpiForbiddenException("Token scope does not allow the requested calibration scope.");
        }
    }

    private int pageSize(Integer requestedLimit) {
        int limit = requestedLimit == null ? DEFAULT_PAGE_SIZE : requestedLimit;
        if (limit < 1 || limit > MAX_PAGE_SIZE) {
            throw new BpiValidationException("limit must be between 1 and 200.");
        }
        return limit;
    }

    private String scopeFingerprint(
            ActorContext actor,
            String plantId,
            String lineId,
            String productId,
            String deviceId,
            String propertyId) {
        return Checksums.sha256(String.join("|",
                fingerprintPart(actor.tenantId()),
                fingerprintPart(plantId),
                fingerprintPart(lineId),
                fingerprintPart(normalizedFilter(productId)),
                fingerprintPart(normalizedFilter(deviceId)),
                fingerprintPart(normalizedFilter(propertyId))));
    }

    private String normalizedFilter(String value) {
        return value == null || value.isBlank() ? "" : value;
    }

    private String fingerprintPart(String value) {
        return value.length() + ":" + value;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize BPI point calibration command", exception);
        }
    }
}
