package com.mapletct.ftmes.bpi.rules;

import java.util.List;

public record BoundaryWindowResult(
        BoundaryWindowState state,
        BoundaryDecision decision,
        boolean newlyEligible,
        boolean ignoredObservation,
        String firstQuorumEvidenceEventId,
        List<BoundaryEvidenceSnapshot> evidence) {

    public BoundaryWindowResult {
        evidence = List.copyOf(evidence);
    }
}
