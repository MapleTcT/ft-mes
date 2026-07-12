package com.mapletct.ftmes.bpi.rules;

import java.util.List;

public record BoundaryDecision(
        boolean eligible,
        double confidence,
        int quorumSatisfied,
        List<String> blockers) {
}
