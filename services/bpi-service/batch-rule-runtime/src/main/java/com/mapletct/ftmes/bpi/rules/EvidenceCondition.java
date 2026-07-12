package com.mapletct.ftmes.bpi.rules;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Objects;

public record EvidenceCondition(
        String signal,
        ConditionOperator operator,
        BigDecimal threshold,
        Duration holdFor,
        Duration maxSilence,
        EvidenceClass classification,
        int weight) {

    public EvidenceCondition {
        Objects.requireNonNull(signal, "signal");
        Objects.requireNonNull(operator, "operator");
        Objects.requireNonNull(holdFor, "holdFor");
        Objects.requireNonNull(maxSilence, "maxSilence");
        Objects.requireNonNull(classification, "classification");
        if (signal.isBlank()) throw new IllegalArgumentException("signal is required");
        if (holdFor.isNegative()) throw new IllegalArgumentException("holdFor must be non-negative");
        if (maxSilence.isZero() || maxSilence.isNegative()) {
            throw new IllegalArgumentException("maxSilence must be positive");
        }
        if (weight < 0) throw new IllegalArgumentException("weight must be non-negative");
        boolean numericOperator = operator == ConditionOperator.GREATER_THAN
                || operator == ConditionOperator.LESS_THAN || operator == ConditionOperator.RISING;
        if (numericOperator && threshold == null) {
            throw new IllegalArgumentException("numeric operators require a threshold");
        }
        if (!numericOperator && threshold != null) {
            throw new IllegalArgumentException("boolean operators cannot declare a threshold");
        }
    }
}
