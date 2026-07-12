package com.mapletct.ftmes.bpi.rules;

import java.util.Map;

public record BoundaryWindowState(
        Map<String, EvidenceSignalState> signals,
        boolean candidateEmitted,
        String firstQuorumEvidenceEventId) {

    public BoundaryWindowState {
        signals = signals == null ? Map.of() : Map.copyOf(signals);
    }

    public static BoundaryWindowState empty() {
        return new BoundaryWindowState(Map.of(), false, null);
    }
}
