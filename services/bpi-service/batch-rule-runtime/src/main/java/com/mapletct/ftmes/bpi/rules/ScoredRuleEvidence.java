package com.mapletct.ftmes.bpi.rules;

import java.util.Objects;

public record ScoredRuleEvidence(
        String signal,
        EvidenceClass classification,
        boolean satisfied,
        double evidenceScore,
        double qualityFactor,
        int weight) {

    public ScoredRuleEvidence {
        Objects.requireNonNull(signal, "signal");
        Objects.requireNonNull(classification, "classification");
        if (evidenceScore < 0 || evidenceScore > 1 || qualityFactor < 0 || qualityFactor > 1) {
            throw new IllegalArgumentException("scores and factors must be between 0 and 1");
        }
        if (weight < 0) throw new IllegalArgumentException("weight must be non-negative");
    }
}
