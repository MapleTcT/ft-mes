package com.mapletct.ftmes.bpi.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.application.error.BpiConflictException;
import com.mapletct.ftmes.bpi.application.error.BpiForbiddenException;
import com.mapletct.ftmes.bpi.application.error.BpiPreconditionRequiredException;
import com.mapletct.ftmes.bpi.application.error.BpiValidationException;
import com.mapletct.ftmes.bpi.domain.BatchInstance;
import com.mapletct.ftmes.bpi.domain.BatchState;
import com.mapletct.ftmes.bpi.infrastructure.postgres.BpiPostgresRepository;
import com.mapletct.ftmes.bpi.infrastructure.postgres.IdempotencyRecord;
import com.mapletct.ftmes.bpi.interfaces.rest.ReasonCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BatchCommandService {
    private static final Pattern REVISION_HEADER = Pattern.compile("^(?:W/)?\\\"?(\\d+)\\\"?$");

    private final BpiPostgresRepository repository;
    private final ObjectMapper objectMapper;

    public BatchCommandService(BpiPostgresRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(timeout = 15)
    public CommandResult<BatchInstance> suspend(
            ActorContext actor,
            UUID batchId,
            String idempotencyKey,
            String ifMatch,
            ReasonCommand command,
            String traceId) {
        return transition(
                actor, batchId, idempotencyKey, ifMatch, command, traceId,
                "suspend", "BATCH_SUSPENDED", BatchState.ACTIVE, BatchState.SUSPENDED);
    }

    @Transactional(timeout = 15)
    public CommandResult<BatchInstance> resume(
            ActorContext actor,
            UUID batchId,
            String idempotencyKey,
            String ifMatch,
            ReasonCommand command,
            String traceId) {
        return transition(
                actor, batchId, idempotencyKey, ifMatch, command, traceId,
                "resume", "BATCH_RESUMED", BatchState.SUSPENDED, BatchState.ACTIVE);
    }

    private CommandResult<BatchInstance> transition(
            ActorContext actor,
            UUID batchId,
            String idempotencyKey,
            String ifMatch,
            ReasonCommand command,
            String traceId,
            String commandName,
            String auditAction,
            BatchState fromState,
            BatchState toState) {
        validateCommandHeaders(idempotencyKey, ifMatch);
        long expectedRevision = parseRevision(ifMatch);
        BatchInstance visibleBatch = repository.findBatch(actor, batchId);
        assertScope(actor, visibleBatch);
        if (!repository.commandsEnabled(actor, visibleBatch)) {
            throw new BpiForbiddenException("BPI commands are disabled for this scope.");
        }

        String path = "/bpi/v1/batches/" + batchId + "/" + commandName;
        String checksum = Checksums.sha256(
                batchId + "|" + expectedRevision + "|" + command.reason() + "|" + command.comment());
        boolean owner = repository.reserveIdempotency(
                UUID.randomUUID(), actor.tenantId(), idempotencyKey, "POST", path, checksum);
        if (!owner) {
            IdempotencyRecord previous = repository.lockIdempotency(actor.tenantId(), idempotencyKey);
            assertIdempotencyReplay(previous, "POST", path, checksum);
            if ("COMPLETED".equals(previous.state()) && previous.responseBody() != null) {
                BatchInstance response = repository.readJson(
                        previous.responseBody(), new TypeReference<BatchInstance>() {});
                return new CommandResult<>(response, true);
            }
            throw new BpiConflictException("The command is still processing.", null);
        }

        BatchInstance batch = repository.lockBatch(actor, batchId);
        assertScope(actor, batch);
        if (batch.revision() != expectedRevision) {
            throw new BpiConflictException("Batch revision is stale.", batch.revision());
        }
        if (batch.state() != fromState) {
            throw new BpiConflictException(
                    "Batch must be " + fromState + " before it can " + commandName + ".", batch.revision());
        }

        long nextRevision = expectedRevision + 1;
        Instant eventTime = Instant.now();
        repository.transitionBatch(actor.tenantId(), batchId, expectedRevision, fromState, toState);
        repository.insertStateEvent(
                actor.tenantId(), batchId, nextRevision, auditAction, fromState.name(), toState.name(),
                command.reason(), actor.userId(), eventTime, traceId);
        repository.insertBatchAudit(
                actor, batch, auditAction, expectedRevision, nextRevision, command.reason(), traceId,
                Map.of("fromState", fromState, "toState", toState, "comment", safeComment(command.comment())));

        BatchInstance response = repository.findBatch(actor, batchId);
        repository.completeIdempotency(actor.tenantId(), idempotencyKey, 200, writeJson(response));
        return new CommandResult<>(response, false);
    }

    private void assertScope(ActorContext actor, BatchInstance batch) {
        if (!actor.canAccess(batch.plantId(), batch.lineId())) {
            throw new BpiForbiddenException("Token scope does not allow this batch.");
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

    private String safeComment(String comment) {
        return comment == null ? "" : comment;
    }

    private String writeJson(Object response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not persist idempotent response", exception);
        }
    }
}
