package com.mapletct.ftmes.bpi.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.application.error.BpiConflictException;
import com.mapletct.ftmes.bpi.application.error.BpiForbiddenException;
import com.mapletct.ftmes.bpi.application.error.BpiPreconditionRequiredException;
import com.mapletct.ftmes.bpi.application.error.BpiValidationException;
import com.mapletct.ftmes.bpi.domain.BatchCandidate;
import com.mapletct.ftmes.bpi.domain.BatchInstance;
import com.mapletct.ftmes.bpi.domain.BatchState;
import com.mapletct.ftmes.bpi.domain.BoundaryType;
import com.mapletct.ftmes.bpi.domain.CandidateConfirmation;
import com.mapletct.ftmes.bpi.domain.CandidateState;
import com.mapletct.ftmes.bpi.infrastructure.postgres.BpiPostgresRepository;
import com.mapletct.ftmes.bpi.infrastructure.postgres.IdempotencyRecord;
import com.mapletct.ftmes.bpi.infrastructure.postgres.PersistedCandidate;
import com.mapletct.ftmes.bpi.interfaces.rest.ReasonCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CandidateService {
    private static final UUID BATCH_NAMESPACE = UUID.fromString("af4cbdb2-2f7a-5d40-9e99-6a69e51fd07a");
    private static final Pattern REVISION_HEADER = Pattern.compile("^(?:W/)?\\\"?(\\d+)\\\"?$");
    private static final DateTimeFormatter BATCH_DATE = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);

    private final BpiPostgresRepository repository;
    private final ObjectMapper objectMapper;

    public CandidateService(BpiPostgresRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<BatchCandidate> list(
            ActorContext actor, String plantId, String lineId, CandidateState state, int limit) {
        return repository.listCandidates(actor, plantId, lineId, state, limit);
    }

    @Transactional(readOnly = true)
    public BatchCandidate get(ActorContext actor, UUID candidateId) {
        BatchCandidate candidate = repository.findCandidate(actor, candidateId);
        assertScope(actor, candidate);
        return candidate;
    }

    @Transactional(timeout = 15)
    public CommandResult<CandidateConfirmation> confirm(
            ActorContext actor,
            UUID candidateId,
            String idempotencyKey,
            String ifMatch,
            ReasonCommand command,
            String traceId) {
        validateCommandHeaders(idempotencyKey, ifMatch);
        long expectedRevision = parseRevision(ifMatch);
        BatchCandidate visibleCandidate = repository.findCandidate(actor, candidateId);
        assertScope(actor, visibleCandidate);
        if (!repository.commandsEnabled(actor, visibleCandidate)) {
            throw new BpiForbiddenException("BPI commands are disabled for this scope.");
        }
        String path = "/bpi/v1/candidates/" + candidateId + "/confirm";
        String checksum = Checksums.sha256(candidateId + "|" + expectedRevision + "|" + command.reason() + "|" + command.comment());
        boolean owner = repository.reserveIdempotency(
                UUID.randomUUID(), actor.tenantId(), idempotencyKey, "POST", path, checksum);
        if (!owner) {
            IdempotencyRecord previous = repository.lockIdempotency(actor.tenantId(), idempotencyKey);
            assertIdempotencyReplay(previous, "POST", path, checksum);
            if ("COMPLETED".equals(previous.state()) && previous.responseBody() != null) {
                CandidateConfirmation response = repository.readJson(
                        previous.responseBody(), new TypeReference<CandidateConfirmation>() {});
                return new CommandResult<>(response, true);
            }
            throw new BpiConflictException("The command is still processing.", null);
        }

        PersistedCandidate persisted = repository.lockCandidate(actor, candidateId);
        BatchCandidate candidate = persisted.candidate();
        assertScope(actor, candidate);
        if (candidate.revision() != expectedRevision) {
            throw new BpiConflictException("Candidate revision is stale.", candidate.revision());
        }
        if (candidate.state() != CandidateState.PENDING) {
            throw new BpiConflictException("Candidate is already " + candidate.state() + ".", candidate.revision());
        }
        if (candidate.boundaryType() != BoundaryType.START) {
            throw new BpiValidationException("END candidate confirmation requires an existing active batch.");
        }

        UUID batchId = UuidV5.from(BATCH_NAMESPACE, actor.tenantId() + "|" + candidate.candidateKey());
        String suffix = candidate.candidateKey().toString().substring(0, 8).toUpperCase();
        String normalizedLine = candidate.lineId().replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        String batchNo = "BPI-" + normalizedLine + "-" + BATCH_DATE.format(candidate.boundaryTime()) + "-" + suffix;
        BatchInstance batch = new BatchInstance(
                batchId, batchNo, actor.tenantId(), candidate.plantId(), candidate.lineId(), "UNASSIGNED",
                candidate.orderId(), null, BatchState.ACTIVE, 1, true, candidate.boundaryTime(), null,
                BigDecimal.ZERO, "t", null, "NOT_APPLICABLE", "NOT_REQUESTED",
                candidate.ruleVersion(), candidate.topologyVersion());

        repository.insertBatch(batch, persisted, actor.userId());
        repository.insertEvidence(actor.tenantId(), batchId, candidate.boundaryType(), candidate.evidence());
        repository.insertStateEvent(
                actor.tenantId(), batchId, "SHADOW_BATCH_CREATED", BatchState.ACTIVE.name(),
                command.reason(), actor.userId(), Instant.now(), traceId);
        repository.confirmCandidate(candidateId, expectedRevision, batchId, actor.userId(), command.reason());
        repository.insertAudit(
                actor, candidate, "CANDIDATE_CONFIRMED", expectedRevision, expectedRevision + 1,
                command.reason(), traceId, Map.of("batchId", batchId, "shadow", true));

        CandidateConfirmation response = new CandidateConfirmation(
                repository.findCandidate(actor, candidateId), repository.findBatch(actor, batchId));
        repository.completeIdempotency(actor.tenantId(), idempotencyKey, 200, writeJson(response));
        return new CommandResult<>(response, false);
    }

    @Transactional(timeout = 15)
    public CommandResult<BatchCandidate> reject(
            ActorContext actor,
            UUID candidateId,
            String idempotencyKey,
            String ifMatch,
            ReasonCommand command,
            String traceId) {
        validateCommandHeaders(idempotencyKey, ifMatch);
        long expectedRevision = parseRevision(ifMatch);
        BatchCandidate visibleCandidate = repository.findCandidate(actor, candidateId);
        assertScope(actor, visibleCandidate);
        if (!repository.commandsEnabled(actor, visibleCandidate)) {
            throw new BpiForbiddenException("BPI commands are disabled for this scope.");
        }
        String path = "/bpi/v1/candidates/" + candidateId + "/reject";
        String checksum = Checksums.sha256(candidateId + "|" + expectedRevision + "|" + command.reason() + "|" + command.comment());
        boolean owner = repository.reserveIdempotency(
                UUID.randomUUID(), actor.tenantId(), idempotencyKey, "POST", path, checksum);
        if (!owner) {
            IdempotencyRecord previous = repository.lockIdempotency(actor.tenantId(), idempotencyKey);
            assertIdempotencyReplay(previous, "POST", path, checksum);
            if ("COMPLETED".equals(previous.state()) && previous.responseBody() != null) {
                BatchCandidate response = repository.readJson(
                        previous.responseBody(), new TypeReference<BatchCandidate>() {});
                return new CommandResult<>(response, true);
            }
            throw new BpiConflictException("The command is still processing.", null);
        }

        PersistedCandidate persisted = repository.lockCandidate(actor, candidateId);
        BatchCandidate candidate = persisted.candidate();
        assertScope(actor, candidate);
        if (candidate.revision() != expectedRevision) {
            throw new BpiConflictException("Candidate revision is stale.", candidate.revision());
        }
        if (candidate.state() != CandidateState.PENDING) {
            throw new BpiConflictException("Candidate is already " + candidate.state() + ".", candidate.revision());
        }

        repository.rejectCandidate(candidateId, expectedRevision, actor.userId(), command.reason());
        repository.insertAudit(
                actor, candidate, "CANDIDATE_REJECTED", expectedRevision, expectedRevision + 1,
                command.reason(), traceId,
                Map.of("candidateKey", candidate.candidateKey(), "boundaryType", candidate.boundaryType()));

        BatchCandidate response = repository.findCandidate(actor, candidateId);
        repository.completeIdempotency(actor.tenantId(), idempotencyKey, 200, writeJson(response));
        return new CommandResult<>(response, false);
    }

    private void assertScope(ActorContext actor, BatchCandidate candidate) {
        if (!actor.canAccess(candidate.plantId(), candidate.lineId())) {
            throw new BpiForbiddenException("Token scope does not allow this candidate.");
        }
    }

    private void assertIdempotencyReplay(
            IdempotencyRecord previous, String method, String path, String checksum) {
        if (!method.equals(previous.method())
                || !path.equals(previous.resourcePath())
                || !checksum.equals(previous.requestChecksum())) {
            throw new BpiConflictException("Idempotency-Key was reused with a different request.", null);
        }
    }

    private void validateCommandHeaders(String idempotencyKey, String ifMatch) {
        if (idempotencyKey == null || idempotencyKey.length() < 8 || ifMatch == null) {
            throw new BpiPreconditionRequiredException("Idempotency-Key and If-Match are required.");
        }
        if (idempotencyKey.length() > 128) {
            throw new BpiValidationException("Idempotency-Key must not exceed 128 characters.");
        }
    }

    private long parseRevision(String header) {
        Matcher matcher = REVISION_HEADER.matcher(header);
        if (!matcher.matches()) {
            throw new BpiPreconditionRequiredException("If-Match must contain a numeric entity revision.");
        }
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException exception) {
            throw new BpiPreconditionRequiredException("If-Match revision is outside the supported range.");
        }
    }

    private String writeJson(Object response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not persist idempotent response", exception);
        }
    }
}
