package com.mapletct.ftmes.bpi.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.InvalidProtocolBufferException;
import com.mapletct.ftmes.bpi.application.error.BpiConflictException;
import com.mapletct.ftmes.bpi.application.error.BpiForbiddenException;
import com.mapletct.ftmes.bpi.application.error.BpiPreconditionRequiredException;
import com.mapletct.ftmes.bpi.application.error.BpiValidationException;
import com.mapletct.ftmes.bpi.contract.v1.WmsCompletionInboundCommandV1;
import com.mapletct.ftmes.bpi.contract.v1.WmsCompletionInboundReversalCommandV1;
import com.mapletct.ftmes.bpi.contract.v1.WmsCompletionInboundReversalReceiptV1;
import com.mapletct.ftmes.bpi.contract.v1.WmsCompletionInboundReversalStatusV1;
import com.mapletct.ftmes.bpi.domain.BatchInstance;
import com.mapletct.ftmes.bpi.domain.BatchState;
import com.mapletct.ftmes.bpi.domain.WmsInboundReversalOriginalTarget;
import com.mapletct.ftmes.bpi.domain.WmsInboundReversalTaskView;
import com.mapletct.ftmes.bpi.infrastructure.integration.BpiPhase2IntegrationProperties;
import com.mapletct.ftmes.bpi.infrastructure.integration.BpiWmsOutboxProperties;
import com.mapletct.ftmes.bpi.infrastructure.postgres.BatchReleasePostgresRepository;
import com.mapletct.ftmes.bpi.infrastructure.postgres.BpiPostgresRepository;
import com.mapletct.ftmes.bpi.infrastructure.postgres.IdempotencyRecord;
import com.mapletct.ftmes.bpi.infrastructure.postgres.WmsInboundReversalPostgresRepository;
import com.mapletct.ftmes.bpi.interfaces.rest.WmsInboundReversalApprovalMode;
import com.mapletct.ftmes.bpi.interfaces.rest.WmsInboundReversalCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class WmsInboundReversalService {

    private static final Pattern REVISION_HEADER = Pattern.compile("^(?:W/)?\\\"?(\\d+)\\\"?$");
    private static final UUID COMMAND_NAMESPACE =
            UUID.fromString("5e30c4f2-a329-5609-824c-298d619c105b");
    private static final UUID INBOX_NAMESPACE =
            UUID.fromString("6ebcf75c-3df0-5c23-a5f4-56b2d0959ac4");
    private static final String RECEIPT_SOURCE =
            "wms.completion-inbound-reversal.receipt.v1";

    private final BpiPostgresRepository repository;
    private final BatchReleasePostgresRepository releaseRepository;
    private final WmsInboundReversalPostgresRepository reversalRepository;
    private final BpiPhase2IntegrationProperties integrationProperties;
    private final BpiWmsOutboxProperties outboxProperties;
    private final ObjectMapper objectMapper;

    public WmsInboundReversalService(
            BpiPostgresRepository repository,
            BatchReleasePostgresRepository releaseRepository,
            WmsInboundReversalPostgresRepository reversalRepository,
            BpiPhase2IntegrationProperties integrationProperties,
            BpiWmsOutboxProperties outboxProperties,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.releaseRepository = releaseRepository;
        this.reversalRepository = reversalRepository;
        this.integrationProperties = integrationProperties;
        this.outboxProperties = outboxProperties;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public WmsInboundReversalTaskView latest(ActorContext actor, UUID batchId) {
        BatchInstance batch = repository.findBatch(actor, batchId);
        assertScope(actor, batch);
        return reversalRepository.findLatestTask(actor, batchId);
    }

    @Transactional(timeout = 15)
    public CommandResult<WmsInboundReversalTaskView> command(
            ActorContext actor,
            UUID batchId,
            String idempotencyKey,
            String ifMatch,
            WmsInboundReversalCommand command,
            String traceId) {
        validateCommandHeaders(idempotencyKey, ifMatch);
        assertRole(actor, command.approvalMode());
        assertPhase2Enabled();
        long expectedRevision = parseRevision(ifMatch);
        BatchInstance visibleBatch = repository.findBatch(actor, batchId);
        assertScope(actor, visibleBatch);
        assertSwitches(actor, visibleBatch);

        String path = "/bpi/v1/batches/" + batchId + "/wms/reversal";
        String checksum = Checksums.sha256(
                batchId + "|" + expectedRevision + "|" + command.reason() + "|"
                        + safeComment(command.comment()) + "|" + command.approvalMode());
        boolean owner = repository.reserveIdempotency(
                UUID.randomUUID(), actor.tenantId(), idempotencyKey, "POST", path, checksum);
        if (!owner) {
            IdempotencyRecord previous = repository.lockIdempotency(
                    actor.tenantId(), idempotencyKey);
            assertIdempotencyReplay(previous, "POST", path, checksum);
            if ("COMPLETED".equals(previous.state()) && previous.responseBody() != null) {
                WmsInboundReversalTaskView response = repository.readJson(
                        previous.responseBody(), new TypeReference<WmsInboundReversalTaskView>() {});
                return new CommandResult<>(response, true);
            }
            throw new BpiConflictException("The command is still processing.", null);
        }

        BatchInstance batch = repository.lockBatch(actor, batchId);
        assertScope(actor, batch);
        assertSwitches(actor, batch);
        if (batch.revision() != expectedRevision) {
            throw new BpiConflictException("Batch revision is stale.", batch.revision());
        }
        if (batch.state() != BatchState.INBOUNDED) {
            throw new BpiConflictException(
                    "Only an INBOUNDED batch can request completion-inbound reversal.",
                    batch.revision());
        }
        if (batch.shadow()) {
            throw new BpiConflictException(
                    "Shadow batches cannot reverse WMS completion inbound.", batch.revision());
        }

        WmsInboundReversalTaskView response =
                command.approvalMode() == WmsInboundReversalApprovalMode.REQUEST
                        ? request(actor, batch, command, traceId)
                        : approve(actor, batch, command, traceId);
        repository.completeIdempotency(
                actor.tenantId(), idempotencyKey, 202, writeJson(response));
        return new CommandResult<>(response, false);
    }

    @Transactional(timeout = 15)
    public WmsInboundReversalTaskView applyReceipt(
            ActorContext actor,
            WmsCompletionInboundReversalReceiptV1 event,
            String payloadChecksum) {
        assertIntegrationActor(actor);
        assertPhase2Enabled();
        UUID batchId = parseUuid(event.getBatchId(), "batch_id");
        UUID commandEventId = parseUuid(
                event.getCommandEventId(), "command_event_id");
        validateEventIdentity(
                actor, event.getTenantId(), event.getPlantId(), event.getLineId());
        BatchInstance batch = repository.lockBatch(actor, batchId);
        assertScope(actor, batch);
        assertBatchIdentity(batch, event.getPlantId(), event.getLineId());
        WmsInboundReversalTaskView task = reversalRepository.lockByCommand(
                actor, batchId, commandEventId);
        String receiptIdempotencyKey = requireText(
                event.getIdempotencyKey(), "idempotency_key", 256);
        if (!receiptIdempotencyKey.equals(task.reversalIdempotencyKey())) {
            throw new BpiConflictException(
                    "WMS reversal receipt idempotency_key does not match the durable command.",
                    task.revision());
        }

        boolean firstDelivery = repository.recordInbox(
                UuidV5.from(
                        INBOX_NAMESPACE,
                        actor.tenantId() + "|" + RECEIPT_SOURCE + "|" + event.getEventId()),
                actor.tenantId(), RECEIPT_SOURCE, receiptIdempotencyKey,
                requireText(event.getEventId(), "event_id", 256), payloadChecksum);
        if (!firstDelivery) {
            return reversalRepository.findTask(actor, task.taskId());
        }
        if (!"PUBLISHED".equals(task.outboxStatus())) {
            throw new BpiConflictException(
                    "WMS reversal receipt cannot precede durable command publication.",
                    task.revision());
        }
        if (!"PENDING_WMS".equals(task.state())) {
            throw new BpiConflictException(
                    "WMS reversal command already has a terminal receipt.", task.revision());
        }
        if (batch.state() != BatchState.INBOUND_REVERSING) {
            throw new BpiConflictException(
                    "Batch must be INBOUND_REVERSING before a reversal receipt can be applied.",
                    batch.revision());
        }
        if (!Set.of(
                WmsCompletionInboundReversalStatusV1
                        .WMS_COMPLETION_INBOUND_REVERSAL_ACCEPTED,
                WmsCompletionInboundReversalStatusV1
                        .WMS_COMPLETION_INBOUND_REVERSAL_REJECTED).contains(event.getStatus())) {
            throw new BpiValidationException(
                    "WMS reversal receipt status must be ACCEPTED or REJECTED.");
        }
        boolean accepted = event.getStatus()
                == WmsCompletionInboundReversalStatusV1
                        .WMS_COMPLETION_INBOUND_REVERSAL_ACCEPTED;
        String reversalDocumentId = normalizedOptional(
                event.getReversalDocumentId(), 256);
        String originalDocumentId = requireText(
                event.getOriginalDocumentId(), "original_document_id", 256);
        String errorCode = normalizedOptional(event.getErrorCode(), 128);
        if (!task.originalDocumentId().equals(originalDocumentId)) {
            throw new BpiConflictException(
                    "WMS reversal receipt references a different original document.",
                    task.revision());
        }
        if (accepted && reversalDocumentId == null) {
            throw new BpiValidationException(
                    "Accepted WMS reversal receipts require reversal_document_id.");
        }
        if (!accepted && errorCode == null) {
            throw new BpiValidationException(
                    "Rejected WMS reversal receipts require error_code.");
        }
        Instant observedAt = positiveInstant(
                event.getObservedAtMs(), "observed_at_ms");
        reversalRepository.updateReceipt(
                actor, task, accepted, event.getEventId(), reversalDocumentId,
                errorCode, event.getDetail(), observedAt);

        BatchState targetState = accepted
                ? BatchState.INBOUND_REVERSED : BatchState.INBOUNDED;
        String targetWmsStatus = accepted ? "REVERSED" : "REVERSAL_FAILED";
        long nextRevision = releaseRepository.transitionBatch(
                actor.tenantId(), batch.id(), batch.revision(),
                BatchState.INBOUND_REVERSING, targetState,
                batch.qualityGate(), targetWmsStatus, batch.materialCode());
        String action = accepted
                ? "WMS_INBOUND_REVERSAL_ACCEPTED" : "WMS_INBOUND_REVERSAL_REJECTED";
        String reason = accepted
                ? "WMS durable red document received"
                : "WMS rejected completion-inbound reversal";
        String traceId = traceId(event.getHeadersMap(), event.getEventId());
        repository.insertStateEvent(
                actor.tenantId(), batch.id(), nextRevision, action,
                batch.state().name(), targetState.name(), reason,
                actor.userId(), Instant.now(), traceId);
        repository.insertBatchAudit(
                actor, batch, action, batch.revision(), nextRevision, reason, traceId,
                Map.of(
                        "taskId", task.taskId(),
                        "originalDocumentId", task.originalDocumentId(),
                        "reversalCommandEventId", commandEventId,
                        "receiptEventId", event.getEventId(),
                        "targetState", targetState,
                        "wmsStatus", targetWmsStatus));
        return reversalRepository.findTask(actor, task.taskId());
    }

    private WmsInboundReversalTaskView request(
            ActorContext actor,
            BatchInstance batch,
            WmsInboundReversalCommand command,
            String traceId) {
        if (reversalRepository.hasActiveTask(actor.tenantId(), batch.id())) {
            throw new BpiConflictException(
                    "Batch already has an active WMS reversal task.", batch.revision());
        }
        WmsInboundReversalOriginalTarget original = reversalRepository.lockOriginalInbound(
                actor.tenantId(), batch.id());
        originalCommand(batch, original);
        Instant requestedAt = Instant.now();
        UUID taskId = UUID.randomUUID();
        reversalRepository.touchBatchForRequest(batch);
        reversalRepository.insertTask(
                actor, batch, original, taskId, command.reason(), command.comment(), requestedAt);
        long nextRevision = batch.revision() + 1;
        repository.insertStateEvent(
                actor.tenantId(), batch.id(), nextRevision,
                "WMS_INBOUND_REVERSAL_REQUESTED", batch.state().name(), batch.state().name(),
                command.reason(), actor.userId(), requestedAt, traceId);
        repository.insertBatchAudit(
                actor, batch, "WMS_INBOUND_REVERSAL_REQUESTED",
                batch.revision(), nextRevision, command.reason(), traceId,
                Map.of(
                        "taskId", taskId,
                        "originalInboundLinkId", original.inboundLinkId(),
                        "originalCommandEventId", original.originalCommandEventId(),
                        "originalDocumentId", original.originalDocumentId(),
                        "approvalMode", command.approvalMode(),
                        "comment", safeComment(command.comment())));
        return reversalRepository.findTask(actor, taskId);
    }

    private WmsInboundReversalTaskView approve(
            ActorContext actor,
            BatchInstance batch,
            WmsInboundReversalCommand command,
            String traceId) {
        WmsInboundReversalTaskView task = reversalRepository.lockPendingApproval(
                actor, batch.id());
        if (actor.userId().equals(task.requestedBy())) {
            throw new BpiForbiddenException(
                    "WMS reversal approval must be completed by a different administrator.");
        }
        WmsInboundReversalOriginalTarget original = reversalRepository.lockOriginalInbound(
                actor.tenantId(), batch.id());
        assertOriginalSnapshot(task, original);
        WmsCompletionInboundCommandV1 originalCommand = originalCommand(batch, original);
        Instant decidedAt = Instant.now();
        UUID commandEventId = UuidV5.from(
                COMMAND_NAMESPACE, actor.tenantId() + "|" + batch.id() + "|" + task.taskId());
        String reversalIdempotencyKey =
                "WMS_COMPLETION_INBOUND_REVERSAL|" + actor.tenantId() + "|"
                        + batch.id() + "|" + task.taskId() + "|1";
        WmsCompletionInboundReversalCommandV1 reversalCommand =
                WmsCompletionInboundReversalCommandV1.newBuilder()
                        .setEventId(commandEventId.toString())
                        .setIdempotencyKey(reversalIdempotencyKey)
                        .setTenantId(actor.tenantId())
                        .setPlantId(batch.plantId())
                        .setLineId(batch.lineId())
                        .setBatchId(batch.id().toString())
                        .setOriginalCommandEventId(original.originalCommandEventId().toString())
                        .setOriginalIdempotencyKey(original.originalIdempotencyKey())
                        .setOriginalDocumentId(original.originalDocumentId())
                        .setBatchNo(originalCommand.getBatchNo())
                        .setOrderId(originalCommand.getOrderId())
                        .setMaterialCode(originalCommand.getMaterialCode())
                        .setQuantityDecimal(originalCommand.getQuantityDecimal())
                        .setQuantityUnit(originalCommand.getQuantityUnit())
                        .setReason(task.requestReason())
                        .setRequestedBy(task.requestedBy())
                        .setApprovedBy(actor.userId())
                        .setApprovedAtMs(decidedAt.toEpochMilli())
                        .setRequestedAtMs(task.requestedAt().toEpochMilli())
                        .putHeaders("event_id", commandEventId.toString())
                        .putHeaders("idempotency_key", reversalIdempotencyKey)
                        .putHeaders("tenant_id", actor.tenantId())
                        .putHeaders("schema_version", "v1")
                        .putHeaders("trace_id", traceId)
                        .build();
        reversalRepository.approveAndInsertCommand(
                actor, batch, task, commandEventId, reversalIdempotencyKey,
                outboxProperties.reversalTopic(),
                actor.tenantId() + "|" + batch.plantId() + "|" + batch.id(),
                reversalCommand.toByteArray(), reversalCommand.getHeadersMap(),
                command.reason(), command.comment(), decidedAt);
        long nextRevision = releaseRepository.transitionBatch(
                actor.tenantId(), batch.id(), batch.revision(),
                BatchState.INBOUNDED, BatchState.INBOUND_REVERSING,
                batch.qualityGate(), "REVERSAL_PENDING", batch.materialCode());
        repository.insertStateEvent(
                actor.tenantId(), batch.id(), nextRevision,
                "WMS_INBOUND_REVERSAL_APPROVED", batch.state().name(),
                BatchState.INBOUND_REVERSING.name(), command.reason(),
                actor.userId(), decidedAt, traceId);
        Map<String, Object> audit = new LinkedHashMap<>();
        audit.put("taskId", task.taskId());
        audit.put("originalDocumentId", task.originalDocumentId());
        audit.put("reversalCommandEventId", commandEventId);
        audit.put("reversalIdempotencyKey", reversalIdempotencyKey);
        audit.put("requestedBy", task.requestedBy());
        audit.put("approvedBy", actor.userId());
        audit.put("toState", BatchState.INBOUND_REVERSING);
        audit.put("comment", safeComment(command.comment()));
        repository.insertBatchAudit(
                actor, batch, "WMS_INBOUND_REVERSAL_APPROVED",
                batch.revision(), nextRevision, command.reason(), traceId, audit);
        return reversalRepository.findTask(actor, task.taskId());
    }

    private WmsCompletionInboundCommandV1 originalCommand(
            BatchInstance batch, WmsInboundReversalOriginalTarget original) {
        if (!"ACCEPTED".equals(original.inboundStatus())
                || !"PUBLISHED".equals(original.outboxStatus())
                || original.originalDocumentId() == null
                || original.originalDocumentId().isBlank()) {
            throw new BpiConflictException(
                    "WMS completion inbound must have a published accepted durable document before reversal.",
                    original.inboundRevision());
        }
        WmsCompletionInboundCommandV1 command;
        try {
            command = WmsCompletionInboundCommandV1.parseFrom(
                    original.originalCommandPayload());
        } catch (InvalidProtocolBufferException error) {
            throw new BpiConflictException(
                    "Original WMS completion-inbound command payload is invalid.",
                    original.inboundRevision());
        }
        if (!original.originalCommandEventId().toString().equals(command.getEventId())
                || !original.originalIdempotencyKey().equals(command.getIdempotencyKey())
                || !batch.tenantId().equals(command.getTenantId())
                || !batch.plantId().equals(command.getPlantId())
                || !batch.lineId().equals(command.getLineId())
                || !batch.id().toString().equals(command.getBatchId())
                || !batch.batchNo().equals(command.getBatchNo())
                || command.getMaterialCode().isBlank()
                || command.getQuantityUnit().isBlank()) {
            throw new BpiConflictException(
                    "Original WMS command does not match the accepted batch and inbound link.",
                    original.inboundRevision());
        }
        try {
            if (new BigDecimal(command.getQuantityDecimal()).signum() <= 0) {
                throw new NumberFormatException("not positive");
            }
        } catch (NumberFormatException error) {
            throw new BpiConflictException(
                    "Original WMS command quantity is invalid.", original.inboundRevision());
        }
        return command;
    }

    private void assertOriginalSnapshot(
            WmsInboundReversalTaskView task,
            WmsInboundReversalOriginalTarget original) {
        if (!task.originalInboundLinkId().equals(original.inboundLinkId())
                || !task.originalCommandEventId().equals(original.originalCommandEventId())
                || !task.originalIdempotencyKey().equals(original.originalIdempotencyKey())
                || !task.originalDocumentId().equals(original.originalDocumentId())) {
            throw new BpiConflictException(
                    "Accepted WMS inbound facts changed after reversal submission.", task.revision());
        }
    }

    private void assertRole(
            ActorContext actor, WmsInboundReversalApprovalMode approvalMode) {
        if (approvalMode == WmsInboundReversalApprovalMode.APPROVE) {
            if (!actor.roles().contains("BPI_ADMIN")) {
                throw new BpiForbiddenException(
                        "BPI_ADMIN role is required to approve WMS reversal requests.");
            }
            return;
        }
        if (!actor.roles().contains("BPI_SHIFT_LEAD")
                && !actor.roles().contains("BPI_ADMIN")) {
            throw new BpiForbiddenException(
                    "BPI_SHIFT_LEAD or BPI_ADMIN role is required to request WMS reversal.");
        }
    }

    private void assertSwitches(ActorContext actor, BatchInstance batch) {
        if (!repository.commandsEnabled(actor, batch)) {
            throw new BpiForbiddenException("BPI commands are disabled for this scope.");
        }
        if (!repository.featureEnabled(
                actor, batch.plantId(), batch.lineId(), "bpi.wms-link")) {
            throw new BpiForbiddenException(
                    "WMS completion-inbound integration is disabled for this scope.");
        }
    }

    private void assertPhase2Enabled() {
        if (!integrationProperties.enabled()) {
            throw new BpiForbiddenException("BPI Phase 2 integrations are disabled.");
        }
    }

    private void assertIntegrationActor(ActorContext actor) {
        if (!actor.roles().contains("BPI_INTEGRATION_INGEST")
                && !actor.roles().contains("BPI_ADMIN")) {
            throw new BpiForbiddenException("BPI_INTEGRATION_INGEST role is required.");
        }
    }

    private void validateEventIdentity(
            ActorContext actor, String tenantId, String plantId, String lineId) {
        if (!actor.tenantId().equals(tenantId)) {
            throw new BpiForbiddenException("WMS reversal receipt tenant does not match actor tenant.");
        }
        if (!integrationProperties.allows(tenantId, plantId, lineId)) {
            throw new BpiForbiddenException(
                    "WMS reversal receipt is outside the configured Phase 2 scope.");
        }
    }

    private void assertBatchIdentity(BatchInstance batch, String plantId, String lineId) {
        if (!batch.plantId().equals(plantId) || !batch.lineId().equals(lineId)) {
            throw new BpiConflictException(
                    "WMS reversal receipt plant or line does not match the batch.", batch.revision());
        }
    }

    private void assertScope(ActorContext actor, BatchInstance batch) {
        if (!actor.tenantId().equals(batch.tenantId())
                || !actor.canAccess(batch.plantId(), batch.lineId())) {
            throw new BpiForbiddenException("Batch is outside the actor scope.");
        }
    }

    private void assertIdempotencyReplay(
            IdempotencyRecord previous,
            String method,
            String path,
            String checksum) {
        if (!method.equals(previous.method())
                || !path.equals(previous.resourcePath())
                || !checksum.equals(previous.requestChecksum())) {
            throw new BpiConflictException(
                    "Idempotency-Key was reused with a different request.", null);
        }
    }

    private void validateCommandHeaders(String idempotencyKey, String ifMatch) {
        if (idempotencyKey == null || idempotencyKey.isBlank()
                || idempotencyKey.length() < 8 || ifMatch == null) {
            throw new BpiPreconditionRequiredException(
                    "Idempotency-Key and If-Match are required.");
        }
        if (idempotencyKey.length() > 128) {
            throw new BpiValidationException(
                    "Idempotency-Key must not exceed 128 characters.");
        }
    }

    private long parseRevision(String value) {
        Matcher matcher = REVISION_HEADER.matcher(value.trim());
        if (!matcher.matches()) {
            throw new BpiPreconditionRequiredException(
                    "If-Match must contain a numeric revision.");
        }
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException error) {
            throw new BpiPreconditionRequiredException(
                    "If-Match revision is outside the supported range.");
        }
    }

    private UUID parseUuid(String value, String field) {
        try {
            return UUID.fromString(requireText(value, field, 64));
        } catch (IllegalArgumentException error) {
            throw new BpiValidationException(field + " must be a UUID.");
        }
    }

    private String requireText(String value, String field, int maximum) {
        if (value == null || value.isBlank() || value.length() > maximum) {
            throw new BpiValidationException(
                    field + " is required and must not exceed " + maximum + " characters.");
        }
        return value;
    }

    private String normalizedOptional(String value, int maximum) {
        if (value == null || value.isBlank()) return null;
        if (value.length() > maximum) {
            throw new BpiValidationException(
                    "Optional WMS reversal field exceeds " + maximum + " characters.");
        }
        return value;
    }

    private Instant positiveInstant(long value, String field) {
        if (value <= 0) throw new BpiValidationException(field + " must be positive.");
        try {
            return Instant.ofEpochMilli(value);
        } catch (Exception error) {
            throw new BpiValidationException(field + " is outside the supported range.");
        }
    }

    private String traceId(Map<String, String> headers, String fallback) {
        String traceId = headers.get("trace_id");
        return traceId == null || traceId.isBlank() ? fallback : traceId;
    }

    private String safeComment(String value) {
        return value == null ? "" : value;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception error) {
            throw new IllegalStateException("Could not serialize WMS reversal response", error);
        }
    }
}
