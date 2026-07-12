package com.mapletct.ftmes.bpi.application;

import com.mapletct.ftmes.bpi.application.error.BpiNotFoundException;
import com.mapletct.ftmes.bpi.domain.BatchCandidate;
import com.mapletct.ftmes.bpi.domain.BatchInstance;
import com.mapletct.ftmes.bpi.domain.BatchState;
import com.mapletct.ftmes.bpi.domain.CandidateState;
import com.mapletct.ftmes.bpi.domain.LineState;
import com.mapletct.ftmes.bpi.infrastructure.postgres.BpiPostgresRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class OverviewService {
    private final BpiPostgresRepository repository;

    public OverviewService(BpiPostgresRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<LineState> overview(ActorContext actor, String plantId, boolean onlyAbnormal) {
        List<BatchCandidate> candidates = repository.listCandidates(actor, plantId, null, null, 200);
        List<BatchInstance> batches = repository.listBatches(actor, plantId, null, null, 200);
        Set<String> lineIds = new LinkedHashSet<>();
        candidates.forEach(candidate -> lineIds.add(candidate.lineId()));
        batches.forEach(batch -> lineIds.add(batch.lineId()));
        List<LineState> states = new ArrayList<>();
        for (String lineId : lineIds) {
            LineState state = stateFor(lineId, candidates, batches);
            if (!onlyAbnormal
                    || state.pendingCandidates() > 0
                    || !"GOOD".equals(state.dataHealth())
                    || "BLOCKED".equals(state.status())) {
                states.add(state);
            }
        }
        return states;
    }

    @Transactional(readOnly = true)
    public LineState current(ActorContext actor, String lineId) {
        List<BatchCandidate> candidates = repository.listCandidates(actor, null, lineId, null, 200);
        List<BatchInstance> batches = repository.listBatches(actor, null, lineId, null, 200);
        if (candidates.isEmpty() && batches.isEmpty()) {
            throw new BpiNotFoundException("Line has no BPI context.");
        }
        return stateFor(lineId, candidates, batches);
    }

    private LineState stateFor(
            String lineId, List<BatchCandidate> candidates, List<BatchInstance> batches) {
        BatchInstance active = batches.stream()
                .filter(batch -> batch.lineId().equals(lineId) && !"CLOSED".equals(batch.state().name()))
                .findFirst().orElse(null);
        List<BatchCandidate> lineCandidates = candidates.stream()
                .filter(candidate -> candidate.lineId().equals(lineId)).toList();
        int pending = (int) lineCandidates.stream().filter(candidate -> candidate.state() == CandidateState.PENDING).count();
        BatchCandidate latest = lineCandidates.stream().findFirst().orElse(null);
        Instant last = active != null ? active.startTime()
                : latest != null ? latest.boundaryTime() : Instant.EPOCH;
        return new LineState(
                lineId, lineId,
                active != null ? active.orderId() : latest != null ? latest.orderId() : null,
                active == null ? null : active.id(),
                active == null ? "UNASSIGNED" : active.stageCode(),
                active == null ? "IDLE" : lineStatus(active),
                latest == null ? null : latest.confidence(),
                null,
                active == null ? BigDecimal.ZERO : active.quantity(),
                "GOOD",
                pending,
                pending > 0 ? 1 : 0,
                last);
    }

    private String lineStatus(BatchInstance batch) {
        return batch.state() == BatchState.SUSPENDED ? "BLOCKED" : "RUNNING";
    }
}
