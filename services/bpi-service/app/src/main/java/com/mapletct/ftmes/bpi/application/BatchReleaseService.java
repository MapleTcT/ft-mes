package com.mapletct.ftmes.bpi.application;

import com.mapletct.ftmes.bpi.application.error.BpiConflictException;
import com.mapletct.ftmes.bpi.application.error.BpiForbiddenException;
import com.mapletct.ftmes.bpi.application.error.BpiValidationException;
import com.mapletct.ftmes.bpi.contract.v1.QcsInspectionDispositionV1;
import com.mapletct.ftmes.bpi.contract.v1.QcsInspectionResultV1;
import com.mapletct.ftmes.bpi.contract.v1.QcsQualityGateV1;
import com.mapletct.ftmes.bpi.contract.v1.WmsCompletionInboundCommandV1;
import com.mapletct.ftmes.bpi.contract.v1.WmsCompletionInboundReceiptV1;
import com.mapletct.ftmes.bpi.contract.v1.WmsCompletionInboundStatusV1;
import com.mapletct.ftmes.bpi.domain.BatchInstance;
import com.mapletct.ftmes.bpi.domain.BatchReleaseView;
import com.mapletct.ftmes.bpi.domain.BatchState;
import com.mapletct.ftmes.bpi.domain.QualityGateState;
import com.mapletct.ftmes.bpi.domain.QualityGateView;
import com.mapletct.ftmes.bpi.domain.QualityInspectionView;
import com.mapletct.ftmes.bpi.domain.WmsInboundTarget;
import com.mapletct.ftmes.bpi.infrastructure.integration.BpiPhase2IntegrationProperties;
import com.mapletct.ftmes.bpi.infrastructure.integration.BpiWmsOutboxProperties;
import com.mapletct.ftmes.bpi.infrastructure.postgres.BatchReleasePostgresRepository;
import com.mapletct.ftmes.bpi.infrastructure.postgres.BpiPostgresRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class BatchReleaseService {
    private static final UUID QCS_INBOX_NAMESPACE =
            UUID.fromString("bb11eddd-a04b-517e-9883-e98468f5cd3b");
    private static final UUID QUALITY_GATE_NAMESPACE =
            UUID.fromString("81350371-ece8-554b-8588-b5123281a374");
    private static final UUID WMS_COMMAND_NAMESPACE =
            UUID.fromString("961cfd8a-aa6e-5fd4-9e30-05c292f69bcd");
    private static final UUID WMS_LINK_NAMESPACE =
            UUID.fromString("c128f153-62f7-562f-a1a9-894a0c4bd1d7");
    private static final UUID WMS_INBOX_NAMESPACE =
            UUID.fromString("d97cb14e-5f3f-5a51-bd7c-5d849b3e5c50");
    private static final String QCS_SOURCE = "qcs.batch.quality-gate.v1";
    private static final String WMS_SOURCE = "wms.completion-inbound.receipt.v1";

    private final BpiPostgresRepository repository;
    private final BatchReleasePostgresRepository releaseRepository;
    private final BpiPhase2IntegrationProperties integrationProperties;
    private final BpiWmsOutboxProperties wmsOutboxProperties;

    public BatchReleaseService(
            BpiPostgresRepository repository,
            BatchReleasePostgresRepository releaseRepository,
            BpiPhase2IntegrationProperties integrationProperties,
            BpiWmsOutboxProperties wmsOutboxProperties) {
        this.repository = repository;
        this.releaseRepository = releaseRepository;
        this.integrationProperties = integrationProperties;
        this.wmsOutboxProperties = wmsOutboxProperties;
    }

    @Transactional(readOnly = true)
    public BatchReleaseView get(ActorContext actor, UUID batchId) {
        BatchInstance batch = repository.findBatch(actor, batchId);
        assertScope(actor, batch);
        return view(actor, batch);
    }

    @Transactional(timeout = 15)
    public BatchReleaseView applyQualityGate(
            ActorContext actor,
            QcsQualityGateV1 event,
            String payloadChecksum) {
        assertIntegrationActor(actor);
        assertPhase2Enabled();
        UUID batchId = parseUuid(event.getBatchId(), "batch_id");
        validateEventIdentity(actor, event.getTenantId(), event.getPlantId(), event.getLineId());
        BatchInstance batch = repository.lockBatch(actor, batchId);
        assertScope(actor, batch);
        assertBatchIdentity(batch, event.getPlantId(), event.getLineId());

        boolean firstDelivery = repository.recordInbox(
                UuidV5.from(QCS_INBOX_NAMESPACE, actor.tenantId() + "|" + QCS_SOURCE + "|" + event.getEventId()),
                actor.tenantId(), QCS_SOURCE, requireText(event.getIdempotencyKey(), "idempotency_key", 256),
                requireText(event.getEventId(), "event_id", 256), payloadChecksum);
        if (!firstDelivery) return view(actor, repository.findBatch(actor, batchId));

        if (!repository.featureEnabled(actor, batch.plantId(), batch.lineId(), "bpi.qcs-link")) {
            throw new BpiForbiddenException("QCS quality-gate integration is disabled for this scope.");
        }
        if (!Set.of(BatchState.CLOSED_RAW, BatchState.WAIT_QA).contains(batch.state())) {
            throw new BpiConflictException(
                    "Batch must be CLOSED_RAW or WAIT_QA before QCS quality-gate ingestion.", batch.revision());
        }

        QualityGateView previous = releaseRepository.lockQualityGate(actor.tenantId(), batchId);
        validateQualityGateRevision(previous, event);
        List<QualityInspectionView> inspections = validateInspections(event);
        QualityGateState gateState = gateState(inspections);
        BigDecimal releaseQuantity = releaseQuantity(event, gateState);
        String quantityUnit = normalizedOptional(event.getQuantityUnit(), 32);
        String materialCode = normalizedOptional(event.getMaterialCode(), 128);
        if (gateState == QualityGateState.ACCEPTED) {
            if (quantityUnit == null || materialCode == null) {
                throw new BpiValidationException(
                        "Accepted quality gates require quantity_unit and material_code.");
            }
            if (batch.materialCode() != null && !batch.materialCode().isBlank()
                    && !batch.materialCode().equals(materialCode)) {
                throw new BpiValidationException(
                        "QCS material_code does not match the batch material identity.");
            }
        }
        Instant observedAt = positiveInstant(event.getObservedAtMs(), "observed_at_ms");
        String externalGateId = requireText(event.getQualityGateId(), "quality_gate_id", 256);
        UUID gateId = previous == null
                ? UuidV5.from(QUALITY_GATE_NAMESPACE, actor.tenantId() + "|" + batchId + "|" + externalGateId)
                : previous.id();
        releaseRepository.saveQualityGate(
                actor.tenantId(), batchId, gateId, externalGateId, event.getQualityGateRevision(),
                event.getEventId(), payloadChecksum, gateState, releaseQuantity, quantityUnit,
                materialCode, observedAt, previous, inspections);

        String traceId = traceId(event.getHeadersMap(), event.getEventId());
        long revision = batch.revision();
        BatchState state = batch.state();
        if (state == BatchState.CLOSED_RAW) {
            revision = transition(
                    actor, batch, revision, BatchState.CLOSED_RAW, BatchState.WAIT_QA,
                    "QUALITY_GATE_OPENED", "WAITING", null, materialCode,
                    "QCS quality gate opened", traceId, gateId, event.getQualityGateRevision(), null);
            state = BatchState.WAIT_QA;
        }

        UUID commandEventId = null;
        if (gateState == QualityGateState.WAITING) {
            if (batch.state() == BatchState.WAIT_QA) {
                transition(
                        actor, batch, revision, BatchState.WAIT_QA, BatchState.WAIT_QA,
                        "QUALITY_GATE_UPDATED", "WAITING", null, materialCode,
                        "QCS quality gate snapshot updated", traceId, gateId,
                        event.getQualityGateRevision(), null);
            }
        } else {
            BatchState targetState = gateState == QualityGateState.ACCEPTED
                    ? BatchState.RELEASED : BatchState.REJECTED;
            boolean createWmsCommand = gateState == QualityGateState.ACCEPTED
                    && !batch.shadow()
                    && repository.featureEnabled(actor, batch.plantId(), batch.lineId(), "bpi.wms-link");
            if (createWmsCommand) {
                commandEventId = wmsCommandEventId(actor.tenantId(), batchId, gateId, event.getQualityGateRevision());
            }
            String action = gateState == QualityGateState.ACCEPTED
                    ? "QUALITY_GATE_ACCEPTED" : "QUALITY_GATE_REJECTED";
            String wmsStatus = createWmsCommand ? "PENDING" : null;
            revision = transition(
                    actor, batch, revision, state, targetState, action, gateState.name(),
                    wmsStatus, materialCode,
                    gateState == QualityGateState.ACCEPTED
                            ? "All required final inspections accepted"
                            : "A required final inspection was rejected",
                    traceId, gateId, event.getQualityGateRevision(), commandEventId);
            if (createWmsCommand) {
                WmsCompletionInboundCommandV1 command = wmsCommand(
                        actor, batch, event, gateId, commandEventId, releaseQuantity,
                        quantityUnit, materialCode, observedAt, traceId);
                releaseRepository.insertWmsCommand(
                        actor.tenantId(), batch.plantId(), batch.lineId(), batchId,
                        commandEventId, wmsOutboxProperties.topic(),
                        actor.tenantId() + "|" + batch.plantId() + "|" + batchId,
                        command.toByteArray(), command.getHeadersMap(),
                        UuidV5.from(WMS_LINK_NAMESPACE, commandEventId.toString()),
                        command.getIdempotencyKey());
            }
        }
        return view(actor, repository.findBatch(actor, batchId));
    }

    @Transactional(timeout = 15)
    public BatchReleaseView applyWmsReceipt(
            ActorContext actor,
            WmsCompletionInboundReceiptV1 event,
            String payloadChecksum) {
        assertIntegrationActor(actor);
        assertPhase2Enabled();
        UUID batchId = parseUuid(event.getBatchId(), "batch_id");
        UUID commandEventId = parseUuid(event.getCommandEventId(), "command_event_id");
        validateEventIdentity(actor, event.getTenantId(), event.getPlantId(), event.getLineId());
        BatchInstance batch = repository.lockBatch(actor, batchId);
        assertScope(actor, batch);
        assertBatchIdentity(batch, event.getPlantId(), event.getLineId());
        WmsInboundTarget target = releaseRepository.lockWmsInbound(
                actor.tenantId(), batchId, commandEventId);

        boolean firstDelivery = repository.recordInbox(
                UuidV5.from(WMS_INBOX_NAMESPACE, actor.tenantId() + "|" + WMS_SOURCE + "|" + event.getEventId()),
                actor.tenantId(), WMS_SOURCE, requireText(event.getIdempotencyKey(), "idempotency_key", 256),
                requireText(event.getEventId(), "event_id", 256), payloadChecksum);
        if (!firstDelivery) return view(actor, repository.findBatch(actor, batchId));

        if (!"PUBLISHED".equals(target.outboxStatus())) {
            throw new BpiConflictException(
                    "WMS receipt cannot precede durable command publication.", target.linkRevision());
        }
        if (!"PENDING".equals(target.status())) {
            throw new BpiConflictException(
                    "WMS completion-inbound command already has a terminal receipt.", target.linkRevision());
        }
        if (batch.state() != BatchState.RELEASED) {
            throw new BpiConflictException(
                    "Batch must be RELEASED before a WMS receipt can be applied.", batch.revision());
        }
        if (!Set.of(
                WmsCompletionInboundStatusV1.WMS_COMPLETION_INBOUND_ACCEPTED,
                WmsCompletionInboundStatusV1.WMS_COMPLETION_INBOUND_REJECTED).contains(event.getStatus())) {
            throw new BpiValidationException("WMS receipt status must be ACCEPTED or REJECTED.");
        }
        boolean accepted = event.getStatus()
                == WmsCompletionInboundStatusV1.WMS_COMPLETION_INBOUND_ACCEPTED;
        String documentId = normalizedOptional(event.getDocumentId(), 256);
        String errorCode = normalizedOptional(event.getErrorCode(), 128);
        if (accepted && documentId == null) {
            throw new BpiValidationException("Accepted WMS receipts require document_id.");
        }
        if (!accepted && errorCode == null) {
            throw new BpiValidationException("Rejected WMS receipts require error_code.");
        }
        Instant observedAt = positiveInstant(event.getObservedAtMs(), "observed_at_ms");
        releaseRepository.updateWmsReceipt(
                actor.tenantId(), target, accepted ? "ACCEPTED" : "REJECTED",
                event.getEventId(), documentId, errorCode, event.getDetail(), observedAt);

        String traceId = traceId(event.getHeadersMap(), event.getEventId());
        transition(
                actor, batch, batch.revision(), BatchState.RELEASED,
                accepted ? BatchState.INBOUNDED : BatchState.RELEASED,
                accepted ? "WMS_INBOUND_ACCEPTED" : "WMS_INBOUND_REJECTED",
                "ACCEPTED", accepted ? "INBOUNDED" : "FAILED", batch.materialCode(),
                accepted ? "WMS durable inbound document received" : "WMS rejected completion inbound",
                traceId, null, null, commandEventId);
        return view(actor, repository.findBatch(actor, batchId));
    }

    private long transition(
            ActorContext actor,
            BatchInstance batch,
            long revision,
            BatchState from,
            BatchState to,
            String action,
            String qualityGate,
            String wmsStatus,
            String materialCode,
            String reason,
            String traceId,
            UUID qualityGateId,
            Long qualityGateRevision,
            UUID commandEventId) {
        long nextRevision = releaseRepository.transitionBatch(
                actor.tenantId(), batch.id(), revision, from, to, qualityGate, wmsStatus, materialCode);
        repository.insertStateEvent(
                actor.tenantId(), batch.id(), nextRevision, action, from.name(), to.name(),
                reason, actor.userId(), Instant.now(), traceId);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("fromState", from.name());
        detail.put("toState", to.name());
        detail.put("qualityGate", qualityGate);
        if (qualityGateId != null) detail.put("qualityGateId", qualityGateId);
        if (qualityGateRevision != null) detail.put("qualityGateRevision", qualityGateRevision);
        if (commandEventId != null) detail.put("wmsCommandEventId", commandEventId);
        if (wmsStatus != null) detail.put("wmsStatus", wmsStatus);
        repository.insertBatchAudit(
                actor, batch, action, revision, nextRevision, reason, traceId, detail);
        return nextRevision;
    }

    private WmsCompletionInboundCommandV1 wmsCommand(
            ActorContext actor,
            BatchInstance batch,
            QcsQualityGateV1 qualityGate,
            UUID qualityGateId,
            UUID eventId,
            BigDecimal releaseQuantity,
            String quantityUnit,
            String materialCode,
            Instant requestedAt,
            String traceId) {
        String idempotencyKey = "WMS_COMPLETION_INBOUND|" + actor.tenantId() + "|" + batch.id()
                + "|" + qualityGateId + "|" + qualityGate.getQualityGateRevision();
        return WmsCompletionInboundCommandV1.newBuilder()
                .setEventId(eventId.toString())
                .setIdempotencyKey(idempotencyKey)
                .setTenantId(actor.tenantId())
                .setPlantId(batch.plantId())
                .setLineId(batch.lineId())
                .setBatchId(batch.id().toString())
                .setBatchNo(batch.batchNo())
                .setOrderId(nullToEmpty(batch.orderId()))
                .setMaterialCode(materialCode)
                .setQuantityDecimal(releaseQuantity.toPlainString())
                .setQuantityUnit(quantityUnit)
                .setQualityGateId(qualityGate.getQualityGateId())
                .setQualityGateRevision(qualityGate.getQualityGateRevision())
                .setRequestedAtMs(requestedAt.toEpochMilli())
                .putHeaders("event_id", eventId.toString())
                .putHeaders("idempotency_key", idempotencyKey)
                .putHeaders("tenant_id", actor.tenantId())
                .putHeaders("schema_version", "v1")
                .putHeaders("trace_id", traceId)
                .build();
    }

    private List<QualityInspectionView> validateInspections(QcsQualityGateV1 event) {
        if (event.getInspectionsCount() == 0 || event.getInspectionsCount() > 200) {
            throw new BpiValidationException("QCS quality gate must contain 1 to 200 inspections.");
        }
        Set<String> codes = new HashSet<>();
        List<QualityInspectionView> inspections = event.getInspectionsList().stream().map(item -> {
            String code = requireText(item.getInspectionCode(), "inspection_code", 128);
            if (!codes.add(code)) {
                throw new BpiValidationException("QCS inspection_code values must be unique.");
            }
            if (item.getDisposition() == QcsInspectionDispositionV1.QCS_INSPECTION_DISPOSITION_UNSPECIFIED) {
                throw new BpiValidationException("QCS inspection disposition is required.");
            }
            if (item.getFinalResult()
                    && item.getDisposition() == QcsInspectionDispositionV1.QCS_INSPECTION_PENDING) {
                throw new BpiValidationException("A final QCS inspection cannot remain pending.");
            }
            return new QualityInspectionView(
                    code,
                    requireText(item.getInspectionRecordId(), "inspection_record_id", 256),
                    item.getRequired(),
                    disposition(item),
                    item.getFinalResult(),
                    positiveInstant(item.getObservedAtMs(), "inspection observed_at_ms"));
        }).toList();
        if (inspections.stream().noneMatch(QualityInspectionView::required)) {
            throw new BpiValidationException("QCS quality gate must contain at least one required inspection.");
        }
        return inspections;
    }

    private QualityGateState gateState(List<QualityInspectionView> inspections) {
        List<QualityInspectionView> required = inspections.stream()
                .filter(QualityInspectionView::required)
                .toList();
        if (required.stream().anyMatch(item -> item.finalResult()
                && "REJECTED".equals(item.disposition()))) {
            return QualityGateState.REJECTED;
        }
        if (required.stream().allMatch(item -> item.finalResult()
                && "ACCEPTED".equals(item.disposition()))) {
            return QualityGateState.ACCEPTED;
        }
        return QualityGateState.WAITING;
    }

    private BigDecimal releaseQuantity(QcsQualityGateV1 event, QualityGateState state) {
        String value = normalizedOptional(event.getReleaseQuantityDecimal(), 64);
        if (value == null) {
            if (state == QualityGateState.ACCEPTED) {
                throw new BpiValidationException("Accepted quality gates require release_quantity_decimal.");
            }
            return null;
        }
        try {
            BigDecimal quantity = new BigDecimal(value);
            if (quantity.signum() <= 0 || quantity.precision() > 24 || quantity.scale() > 6) {
                throw new BpiValidationException(
                        "release_quantity_decimal must be positive with precision 24 and scale 6 or less.");
            }
            return quantity;
        } catch (NumberFormatException error) {
            throw new BpiValidationException("release_quantity_decimal must be a decimal number.");
        }
    }

    private void validateQualityGateRevision(QualityGateView previous, QcsQualityGateV1 event) {
        requireText(event.getQualityGateId(), "quality_gate_id", 256);
        if (event.getQualityGateRevision() <= 0) {
            throw new BpiValidationException("quality_gate_revision must be positive.");
        }
        if (previous == null) return;
        if (!previous.externalGateId().equals(event.getQualityGateId())) {
            throw new BpiConflictException(
                    "A batch cannot switch to a different QCS quality gate.", previous.externalRevision());
        }
        if (event.getQualityGateRevision() <= previous.externalRevision()) {
            throw new BpiConflictException(
                    "QCS quality_gate_revision must increase monotonically.", previous.externalRevision());
        }
    }

    private String disposition(QcsInspectionResultV1 item) {
        return switch (item.getDisposition()) {
            case QCS_INSPECTION_PENDING -> "PENDING";
            case QCS_INSPECTION_ACCEPTED -> "ACCEPTED";
            case QCS_INSPECTION_REJECTED -> "REJECTED";
            default -> throw new BpiValidationException("Unsupported QCS inspection disposition.");
        };
    }

    private UUID wmsCommandEventId(
            String tenantId,
            UUID batchId,
            UUID qualityGateId,
            long qualityGateRevision) {
        return UuidV5.from(
                WMS_COMMAND_NAMESPACE,
                tenantId + "|" + batchId + "|" + qualityGateId + "|" + qualityGateRevision);
    }

    private BatchReleaseView view(ActorContext actor, BatchInstance batch) {
        return new BatchReleaseView(
                batch,
                releaseRepository.findQualityGate(actor, batch.id()),
                releaseRepository.findWmsInbound(actor, batch.id()));
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
            ActorContext actor,
            String tenantId,
            String plantId,
            String lineId) {
        if (!actor.tenantId().equals(tenantId)) {
            throw new BpiForbiddenException("Integration event tenant does not match the token.");
        }
        if (!actor.canAccess(plantId, lineId)) {
            throw new BpiForbiddenException("Integration event is outside the token scope.");
        }
    }

    private void assertScope(ActorContext actor, BatchInstance batch) {
        if (!actor.canAccess(batch.plantId(), batch.lineId())) {
            throw new BpiForbiddenException("Token scope does not allow this batch.");
        }
    }

    private void assertBatchIdentity(BatchInstance batch, String plantId, String lineId) {
        if (!batch.plantId().equals(plantId) || !batch.lineId().equals(lineId)) {
            throw new BpiValidationException("Integration event plant/line does not match the batch.");
        }
    }

    private static UUID parseUuid(String value, String field) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException error) {
            throw new BpiValidationException(field + " must be a UUID.");
        }
    }

    private static Instant positiveInstant(long epochMillis, String field) {
        if (epochMillis <= 0) {
            throw new BpiValidationException(field + " must be a positive epoch millisecond value.");
        }
        return Instant.ofEpochMilli(epochMillis);
    }

    private static String requireText(String value, String field, int maximum) {
        String normalized = normalizedOptional(value, maximum);
        if (normalized == null) {
            throw new BpiValidationException(field + " is required.");
        }
        return normalized;
    }

    private static String normalizedOptional(String value, int maximum) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        if (normalized.length() > maximum) {
            throw new BpiValidationException("Integration field exceeds maximum length " + maximum + ".");
        }
        return normalized;
    }

    private static String traceId(Map<String, String> headers, String fallback) {
        String value = headers.getOrDefault("trace_id", fallback);
        return value.length() <= 128 ? value : value.substring(0, 128);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
