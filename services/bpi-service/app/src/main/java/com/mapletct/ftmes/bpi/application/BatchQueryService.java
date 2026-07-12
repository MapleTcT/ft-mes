package com.mapletct.ftmes.bpi.application;

import com.mapletct.ftmes.bpi.application.error.BpiForbiddenException;
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
}
