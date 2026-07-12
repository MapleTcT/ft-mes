package com.mapletct.ftmes.bpi.rules;

import java.util.Objects;

public record RuleEvidence(
        String signal,
        EvidenceClass classification,
        boolean satisfied,
        int weight,
        double qualityPenalty) {

    public RuleEvidence {
        Objects.requireNonNull(signal, "signal");
        Objects.requireNonNull(classification, "classification");
        if (weight < 0) {
            throw new IllegalArgumentException("weight must be non-negative");
        }
        if (qualityPenalty < 0 || qualityPenalty > 1) {
            throw new IllegalArgumentException("qualityPenalty must be between 0 and 1");
        }
    }
}
