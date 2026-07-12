package com.mapletct.ftmes.bpi.rules;

import java.time.Duration;
import java.util.Objects;

public record BoundaryTimingPolicy(
        Duration allowedLateness,
        Duration watermarkDelay,
        Duration evaluationTimeout) {

    public static final BoundaryTimingPolicy LEGACY_DEFAULTS = new BoundaryTimingPolicy(
            Duration.ZERO,
            Duration.ZERO,
            Duration.ofMinutes(5));

    public BoundaryTimingPolicy {
        Objects.requireNonNull(allowedLateness, "allowedLateness");
        Objects.requireNonNull(watermarkDelay, "watermarkDelay");
        Objects.requireNonNull(evaluationTimeout, "evaluationTimeout");
        if (allowedLateness.isNegative()) {
            throw new IllegalArgumentException("allowedLateness cannot be negative");
        }
        if (watermarkDelay.isNegative()) {
            throw new IllegalArgumentException("watermarkDelay cannot be negative");
        }
        if (evaluationTimeout.isZero() || evaluationTimeout.isNegative()) {
            throw new IllegalArgumentException("evaluationTimeout must be positive");
        }
        if (allowedLateness.compareTo(evaluationTimeout) > 0) {
            throw new IllegalArgumentException("allowedLateness cannot exceed evaluationTimeout");
        }
    }
}
