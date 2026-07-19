package com.mapletct.ftmes.bpi.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.application.error.BpiConflictException;
import com.mapletct.ftmes.bpi.application.error.BpiForbiddenException;
import com.mapletct.ftmes.bpi.application.error.BpiPreconditionRequiredException;
import com.mapletct.ftmes.bpi.application.error.BpiValidationException;
import com.mapletct.ftmes.bpi.domain.RuleVersionView;
import com.mapletct.ftmes.bpi.domain.ShadowRunBatchReviewView;
import com.mapletct.ftmes.bpi.domain.ShadowRunBatchSource;
import com.mapletct.ftmes.bpi.domain.ShadowRunReviewResult;
import com.mapletct.ftmes.bpi.domain.ShadowRunView;
import com.mapletct.ftmes.bpi.domain.TopologyVersionView;
import com.mapletct.ftmes.bpi.infrastructure.postgres.BpiPostgresRepository;
import com.mapletct.ftmes.bpi.infrastructure.postgres.IdempotencyRecord;
import com.mapletct.ftmes.bpi.infrastructure.postgres.RulePostgresRepository;
import com.mapletct.ftmes.bpi.infrastructure.postgres.ShadowRunPostgresRepository;
import com.mapletct.ftmes.bpi.interfaces.rest.ReasonCommand;
import com.mapletct.ftmes.bpi.interfaces.rest.ShadowRunBatchReviewCommand;
import com.mapletct.ftmes.bpi.interfaces.rest.ShadowRunCreateCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ShadowRunService {
    private static final Pattern REVISION_HEADER = Pattern.compile("^(?:W/)?\\\"?(\\d+)\\\"?$");
    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 200;

    private final ShadowRunPostgresRepository repository;
    private final RulePostgresRepository ruleRepository;
    private final BpiPostgresRepository sharedRepository;
    private final CanonicalJson canonicalJson;
    private final ObjectMapper objectMapper;

    public ShadowRunService(
            ShadowRunPostgresRepository repository,
            RulePostgresRepository ruleRepository,
            BpiPostgresRepository sharedRepository,
            CanonicalJson canonicalJson,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.ruleRepository = ruleRepository;
        this.sharedRepository = sharedRepository;
        this.canonicalJson = canonicalJson;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<ShadowRunView> list(
            ActorContext actor, String plantId, String lineId, String state, Integer requestedLimit) {
        if ((plantId == null) != (lineId == null)) {
            throw new BpiValidationException("plantId and lineId must be supplied together.");
        }
        if (plantId != null) assertConcreteScope(actor, plantId, lineId);
        if (state != null && !state.isBlank() && !List.of(
                "DRAFT", "RUNNING", "EVALUATING", "APPROVED", "REJECTED", "CANCELLED").contains(state)) {
            throw new BpiValidationException("Unsupported shadow run state.");
        }
        int limit = requestedLimit == null ? DEFAULT_LIMIT : requestedLimit;
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new BpiValidationException("limit must be between 1 and 200.");
        }
        return repository.list(actor, plantId, lineId, state, limit);
    }

    @Transactional(readOnly = true)
    public ShadowRunView get(ActorContext actor, UUID runId) {
        return repository.find(actor, runId);
    }

    @Transactional(readOnly = true)
    public List<ShadowRunBatchReviewView> listReviews(
            ActorContext actor, UUID runId, boolean includeSuperseded) {
        return repository.listReviews(actor, runId, includeSuperseded);
    }

    @Transactional(timeout = 15)
    public CommandResult<ShadowRunView> create(
            ActorContext actor,
            String idempotencyKey,
            String ifMatch,
            ShadowRunCreateCommand command,
            String traceId) {
        validateHeaders(idempotencyKey, ifMatch);
        long expectedRevision = parseRevision(ifMatch);
        if (expectedRevision != 0) {
            throw new BpiConflictException("A new shadow run must use If-Match 0.", 0L);
        }
        assertConcreteScope(actor, command.plantId(), command.lineId());
        String path = "/bpi/v1/shadow-runs";
        String checksum = Checksums.sha256(canonicalJson.write(command));
        CommandResult<ShadowRunView> replay = replay(
                actor, idempotencyKey, path, checksum, new TypeReference<ShadowRunView>() {});
        if (replay != null) return replay;

        RuleVersionView rule = ruleRepository.findRule(actor, command.ruleVersionId());
        if (!command.plantId().equals(rule.plantId()) || !command.lineId().equals(rule.lineId())) {
            throw new BpiValidationException("Shadow run scope must match its rule version scope.");
        }
        if (!"PUBLISHED".equals(rule.state())) {
            throw new BpiValidationException("A shadow run must pin a PUBLISHED rule version.");
        }
        TopologyVersionView topology = ruleRepository.findTopologyForRule(actor, rule.id());
        if (!"PUBLISHED".equals(topology.state())
                || topology.validatedPointCatalogSnapshotId() == null
                || topology.validatedPointCatalogChecksum() == null) {
            throw new BpiValidationException(
                    "A shadow run requires a published topology pinned to a point catalog snapshot.");
        }

        UUID id = UUID.randomUUID();
        repository.insertDraft(actor, id, topology.id(), topology.validatedPointCatalogSnapshotId(), command);
        ShadowRunView created = repository.find(actor, id);
        repository.insertAudit(actor, created, "SHADOW_RUN_CREATED", 0, created.revision(),
                command.reason(), traceId, Map.of(
                        "ruleVersionId", rule.id(),
                        "topologyVersionId", topology.id(),
                        "pointCatalogSnapshotId", topology.validatedPointCatalogSnapshotId(),
                        "minimumDurationDays", command.minimumDurationDays(),
                        "minimumReviewedBatches", command.minimumReviewedBatches(),
                        "minimumBoundaryAgreement", command.minimumBoundaryAgreement(),
                        "quantityTolerancePercent", command.quantityTolerancePercent()));
        complete(actor, idempotencyKey, created);
        return new CommandResult<>(created, false);
    }

    @Transactional(timeout = 15)
    public CommandResult<ShadowRunView> start(
            ActorContext actor,
            UUID runId,
            String idempotencyKey,
            String ifMatch,
            ReasonCommand command,
            String traceId) {
        return lifecycle(actor, runId, idempotencyKey, ifMatch, command, traceId,
                "start", "DRAFT", "SHADOW_RUN_STARTED", true);
    }

    @Transactional(timeout = 15)
    public CommandResult<ShadowRunReviewResult> reviewBatch(
            ActorContext actor,
            UUID runId,
            String idempotencyKey,
            String ifMatch,
            ShadowRunBatchReviewCommand command,
            String traceId) {
        validateHeaders(idempotencyKey, ifMatch);
        long expectedRevision = parseRevision(ifMatch);
        String path = "/bpi/v1/shadow-runs/" + runId + "/batch-reviews";
        String checksum = Checksums.sha256(
                runId + "|" + expectedRevision + "|" + canonicalJson.write(command));
        CommandResult<ShadowRunReviewResult> replay = replay(
                actor, idempotencyKey, path, checksum, new TypeReference<ShadowRunReviewResult>() {});
        if (replay != null) return replay;

        ShadowRunView run = repository.lock(actor, runId);
        assertRevisionAndState(run, expectedRevision, "RUNNING");
        if (command.manualEndTime().isBefore(command.manualStartTime())) {
            throw new BpiValidationException("Manual batch end time must not precede its start time.");
        }
        ShadowRunBatchSource batch = repository.lockBatchSource(actor, command.batchId());
        validateReviewBatch(run, batch, command);

        long startDeviation = absoluteSeconds(batch.startTime(), command.manualStartTime());
        long endDeviation = absoluteSeconds(batch.endTime(), command.manualEndTime());
        BigDecimal quantityDeviation = batch.quantity().subtract(command.referenceQuantity()).abs()
                .divide(command.referenceQuantity(), 12, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(9, RoundingMode.HALF_UP);
        boolean startAccepted = startDeviation <= run.boundaryToleranceSeconds();
        boolean endAccepted = endDeviation <= run.boundaryToleranceSeconds();
        boolean quantityAccepted = quantityDeviation.compareTo(run.quantityTolerancePercent()) <= 0;
        long sequence = repository.nextReviewSequence(actor.tenantId(), runId, batch.id());
        UUID reviewId = UUID.randomUUID();
        repository.supersedeActiveReview(actor.tenantId(), runId, batch.id());
        repository.insertReview(actor, reviewId, runId, batch, sequence,
                command.manualStartTime(), command.manualEndTime(), startDeviation, endDeviation,
                startAccepted, endAccepted, command.referenceQuantity(), quantityDeviation,
                quantityAccepted, command.reason());
        repository.incrementRevision(actor, runId, expectedRevision);
        ShadowRunView updated = repository.find(actor, runId);
        ShadowRunBatchReviewView review = repository.findReview(actor, reviewId);
        repository.insertAudit(actor, updated, "SHADOW_RUN_BATCH_REVIEWED",
                expectedRevision, updated.revision(), command.reason(), traceId, Map.of(
                        "batchId", batch.id(),
                        "reviewId", reviewId,
                        "reviewSequence", sequence,
                        "startDeviationSeconds", startDeviation,
                        "endDeviationSeconds", endDeviation,
                        "quantityDeviationPercent", quantityDeviation,
                        "quantityWithinTolerance", quantityAccepted));
        ShadowRunReviewResult result = new ShadowRunReviewResult(updated, review);
        complete(actor, idempotencyKey, result);
        return new CommandResult<>(result, false);
    }

    @Transactional(timeout = 15)
    public CommandResult<ShadowRunView> completeRun(
            ActorContext actor,
            UUID runId,
            String idempotencyKey,
            String ifMatch,
            ReasonCommand command,
            String traceId) {
        validateHeaders(idempotencyKey, ifMatch);
        long expectedRevision = parseRevision(ifMatch);
        String path = "/bpi/v1/shadow-runs/" + runId + "/complete";
        String checksum = Checksums.sha256(
                runId + "|" + expectedRevision + "|" + canonicalJson.write(command));
        CommandResult<ShadowRunView> replay = replay(
                actor, idempotencyKey, path, checksum, new TypeReference<ShadowRunView>() {});
        if (replay != null) return replay;

        ShadowRunView run = repository.lock(actor, runId);
        assertRevisionAndState(run, expectedRevision, "RUNNING");
        if (!run.metrics().durationGatePassed() || !run.metrics().reviewCountGatePassed()) {
            throw new BpiValidationException(
                    "A shadow run cannot enter evaluation before its minimum duration and batch review count are reached.");
        }
        repository.complete(actor, runId, expectedRevision);
        ShadowRunView updated = repository.find(actor, runId);
        repository.insertAudit(actor, updated, "SHADOW_RUN_COMPLETED",
                expectedRevision, updated.revision(), command.reason(), traceId,
                metricAudit(updated));
        complete(actor, idempotencyKey, updated);
        return new CommandResult<>(updated, false);
    }

    @Transactional(timeout = 15)
    public CommandResult<ShadowRunView> approve(
            ActorContext actor,
            UUID runId,
            String idempotencyKey,
            String ifMatch,
            ReasonCommand command,
            String traceId) {
        return decide(actor, runId, idempotencyKey, ifMatch, command, traceId,
                "APPROVED", "approve", "SHADOW_RUN_APPROVED", true);
    }

    @Transactional(timeout = 15)
    public CommandResult<ShadowRunView> reject(
            ActorContext actor,
            UUID runId,
            String idempotencyKey,
            String ifMatch,
            ReasonCommand command,
            String traceId) {
        return decide(actor, runId, idempotencyKey, ifMatch, command, traceId,
                "REJECTED", "reject", "SHADOW_RUN_REJECTED", false);
    }

    @Transactional(timeout = 15)
    public CommandResult<ShadowRunView> cancel(
            ActorContext actor,
            UUID runId,
            String idempotencyKey,
            String ifMatch,
            ReasonCommand command,
            String traceId) {
        validateHeaders(idempotencyKey, ifMatch);
        long expectedRevision = parseRevision(ifMatch);
        String path = "/bpi/v1/shadow-runs/" + runId + "/cancel";
        String checksum = Checksums.sha256(
                runId + "|" + expectedRevision + "|" + canonicalJson.write(command));
        CommandResult<ShadowRunView> replay = replay(
                actor, idempotencyKey, path, checksum, new TypeReference<ShadowRunView>() {});
        if (replay != null) return replay;

        ShadowRunView run = repository.lock(actor, runId);
        if (run.revision() != expectedRevision) {
            throw new BpiConflictException("Shadow run revision is stale.", run.revision());
        }
        if (!List.of("DRAFT", "RUNNING").contains(run.state())) {
            throw new BpiConflictException("Only a DRAFT or RUNNING shadow run can be cancelled.", run.revision());
        }
        repository.cancel(actor, runId, expectedRevision, command.reason());
        ShadowRunView updated = repository.find(actor, runId);
        repository.insertAudit(actor, updated, "SHADOW_RUN_CANCELLED",
                expectedRevision, updated.revision(), command.reason(), traceId, Map.of());
        complete(actor, idempotencyKey, updated);
        return new CommandResult<>(updated, false);
    }

    private CommandResult<ShadowRunView> lifecycle(
            ActorContext actor,
            UUID runId,
            String idempotencyKey,
            String ifMatch,
            ReasonCommand command,
            String traceId,
            String operation,
            String expectedState,
            String auditAction,
            boolean requireReadiness) {
        validateHeaders(idempotencyKey, ifMatch);
        long expectedRevision = parseRevision(ifMatch);
        String path = "/bpi/v1/shadow-runs/" + runId + "/" + operation;
        String checksum = Checksums.sha256(
                runId + "|" + expectedRevision + "|" + canonicalJson.write(command));
        CommandResult<ShadowRunView> replay = replay(
                actor, idempotencyKey, path, checksum, new TypeReference<ShadowRunView>() {});
        if (replay != null) return replay;

        ShadowRunView run = repository.lock(actor, runId);
        assertRevisionAndState(run, expectedRevision, expectedState);
        if (requireReadiness && !run.readiness().ready()) {
            throw new BpiValidationException(
                    "Shadow run start is blocked by pinned runtime readiness: "
                            + String.join(",", readinessBlockers(run)));
        }
        repository.start(actor, runId, expectedRevision);
        ShadowRunView updated = repository.find(actor, runId);
        repository.insertAudit(actor, updated, auditAction,
                expectedRevision, updated.revision(), command.reason(), traceId,
                Map.of("readiness", updated.readiness()));
        complete(actor, idempotencyKey, updated);
        return new CommandResult<>(updated, false);
    }

    private CommandResult<ShadowRunView> decide(
            ActorContext actor,
            UUID runId,
            String idempotencyKey,
            String ifMatch,
            ReasonCommand command,
            String traceId,
            String targetState,
            String operation,
            String auditAction,
            boolean requireAllGates) {
        validateHeaders(idempotencyKey, ifMatch);
        long expectedRevision = parseRevision(ifMatch);
        String path = "/bpi/v1/shadow-runs/" + runId + "/" + operation;
        String checksum = Checksums.sha256(
                runId + "|" + expectedRevision + "|" + canonicalJson.write(command));
        CommandResult<ShadowRunView> replay = replay(
                actor, idempotencyKey, path, checksum, new TypeReference<ShadowRunView>() {});
        if (replay != null) return replay;

        ShadowRunView run = repository.lock(actor, runId);
        assertRevisionAndState(run, expectedRevision, "EVALUATING");
        if (actor.userId().equals(run.createdBy())) {
            throw new BpiValidationException(
                    "Shadow run decisions require an administrator other than the creator.");
        }
        if (requireAllGates && !run.readyForApproval()) {
            throw new BpiValidationException(
                    "Shadow run approval gates are not satisfied: " + String.join(",", run.blockers()));
        }
        repository.decide(actor, runId, expectedRevision, targetState, command.reason());
        ShadowRunView updated = repository.find(actor, runId);
        repository.insertAudit(actor, updated, auditAction,
                expectedRevision, updated.revision(), command.reason(), traceId, metricAudit(updated));
        complete(actor, idempotencyKey, updated);
        return new CommandResult<>(updated, false);
    }

    private void validateReviewBatch(
            ShadowRunView run,
            ShadowRunBatchSource batch,
            ShadowRunBatchReviewCommand command) {
        if (!batch.shadow() || !"CLOSED_RAW".equals(batch.state()) || batch.endTime() == null) {
            throw new BpiValidationException("Only a CLOSED_RAW shadow batch can be reviewed.");
        }
        if (!run.plantId().equals(batch.plantId()) || !run.lineId().equals(batch.lineId())) {
            throw new BpiValidationException("Batch scope does not match the shadow run.");
        }
        if (!run.ruleVersionId().equals(batch.ruleVersionId())
                || !run.topologyVersionId().equals(batch.topologyVersionId())) {
            throw new BpiValidationException("Batch rule and topology versions do not match the shadow run.");
        }
        if (batch.startTime().isBefore(run.startedAt())) {
            throw new BpiValidationException("Batch started before this shadow run.");
        }
        if (!batch.quantityUnit().equals(command.quantityUnit())) {
            throw new BpiValidationException("Reference quantity unit must match the batch quantity unit.");
        }
        String existingUnit = repository.activeQuantityUnit(run.tenantId(), run.id());
        if (existingUnit != null && !existingUnit.equals(batch.quantityUnit())) {
            throw new BpiValidationException("All reviewed batches in a shadow run must use one quantity unit.");
        }
    }

    private List<String> readinessBlockers(ShadowRunView run) {
        return run.blockers().stream()
                .filter(value -> value.startsWith("RULE_")
                        || value.startsWith("TOPOLOGY_")
                        || value.startsWith("POINT_CATALOG_"))
                .toList();
    }

    private Map<String, Object> metricAudit(ShadowRunView run) {
        return Map.of(
                "observedDurationSeconds", run.metrics().observedDurationSeconds(),
                "reviewedBatchCount", run.metrics().reviewedBatchCount(),
                "boundaryAgreement", nullable(run.metrics().boundaryAgreement()),
                "cumulativeQuantityDeviationPercent",
                    nullable(run.metrics().cumulativeQuantityDeviationPercent()),
                "unresolvedCriticalIncidentCount", run.metrics().unresolvedCriticalIncidentCount(),
                "blockers", run.blockers(),
                "readyForApproval", run.readyForApproval());
    }

    private String nullable(Object value) {
        return value == null ? "" : value.toString();
    }

    private long absoluteSeconds(java.time.Instant left, java.time.Instant right) {
        Duration deviation = Duration.between(left, right).abs();
        return deviation.getSeconds() + (deviation.getNano() == 0 ? 0 : 1);
    }

    private void assertRevisionAndState(ShadowRunView run, long expectedRevision, String expectedState) {
        if (run.revision() != expectedRevision) {
            throw new BpiConflictException("Shadow run revision is stale.", run.revision());
        }
        if (!expectedState.equals(run.state())) {
            throw new BpiConflictException(
                    "Shadow run must be " + expectedState + " for this command.", run.revision());
        }
    }

    private void assertConcreteScope(ActorContext actor, String plantId, String lineId) {
        if (plantId == null || plantId.isBlank() || lineId == null || lineId.isBlank()) {
            throw new BpiValidationException("plantId and lineId are required for shadow-run access.");
        }
        if (!actor.canAccess(plantId, lineId)) {
            throw new BpiForbiddenException("Token scope does not allow the requested shadow-run scope.");
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

    private void complete(ActorContext actor, String idempotencyKey, Object response) {
        sharedRepository.completeIdempotency(
                actor.tenantId(), idempotencyKey, 200, writeJson(response));
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

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize BPI shadow-run command", exception);
        }
    }
}
