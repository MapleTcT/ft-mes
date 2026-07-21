package com.mapletct.ftmes.bpi.application;

import com.mapletct.ftmes.bpi.application.error.BpiForbiddenException;
import com.mapletct.ftmes.bpi.application.error.BpiConflictException;
import com.mapletct.ftmes.bpi.application.error.BpiNotFoundException;
import com.mapletct.ftmes.bpi.application.error.BpiValidationException;
import com.mapletct.ftmes.bpi.domain.BatchInstance;
import com.mapletct.ftmes.bpi.domain.BatchStateEvent;
import com.mapletct.ftmes.bpi.domain.BoundaryType;
import com.mapletct.ftmes.bpi.domain.EvidenceView;
import com.mapletct.ftmes.bpi.infrastructure.postgres.BpiPostgresRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BatchQueryService {
    private final BpiPostgresRepository repository;

    public BatchQueryService(BpiPostgresRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<BatchInstance> list(ActorContext actor, String plantId, String lineId, String state, int limit) {
        return repository.listBatches(actor, plantId, lineId, state, limit);
    }

    @Transactional(readOnly = true)
    public BatchInstance get(ActorContext actor, UUID batchId) {
        BatchInstance batch = repository.findBatch(actor, batchId);
        assertScope(actor, batch);
        return batch;
    }

    @Transactional(readOnly = true)
    public BatchInstance resolveIntegrationBatch(
            ActorContext actor, String plantId, String lineId, String orderId) {
        String normalizedPlant = required(plantId, "plantId", 64);
        String normalizedLine = required(lineId, "lineId", 128);
        String normalizedOrder = required(orderId, "orderId", 128);
        if (!actor.canAccess(normalizedPlant, normalizedLine)) {
            throw new BpiForbiddenException("Token scope does not allow this batch scope.");
        }
        if (!repository.featureEnabled(actor, normalizedPlant, normalizedLine, "bpi.qcs-link")) {
            throw new BpiForbiddenException("QCS quality-gate integration is disabled for this scope.");
        }
        List<BatchInstance> matches = repository.findBatchesByOrder(
                actor, normalizedPlant, normalizedLine, normalizedOrder);
        if (matches.isEmpty()) {
            throw new BpiNotFoundException("No BPI batch matches the external production order.");
        }
        if (matches.size() > 1) {
            throw new BpiConflictException(
                    "Multiple BPI batches match the external production order; explicit mapping is required.", null);
        }
        return matches.get(0);
    }

    @Transactional(readOnly = true)
    public Map<String, List<EvidenceView>> evidence(ActorContext actor, UUID batchId) {
        BatchInstance batch = get(actor, batchId);
        return Map.of(
                "start", repository.findEvidence(actor, batch.id(), BoundaryType.START),
                "end", repository.findEvidence(actor, batch.id(), BoundaryType.END));
    }

    @Transactional(readOnly = true)
    public List<BatchStateEvent> timeline(ActorContext actor, UUID batchId) {
        BatchInstance batch = get(actor, batchId);
        return repository.findTimeline(actor, batch.id());
    }

    private void assertScope(ActorContext actor, BatchInstance batch) {
        if (!actor.canAccess(batch.plantId(), batch.lineId())) {
            throw new BpiForbiddenException("Token scope does not allow this batch.");
        }
    }

    private String required(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new BpiValidationException(field + " is required.");
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new BpiValidationException(field + " exceeds the maximum length.");
        }
        return normalized;
    }
}
