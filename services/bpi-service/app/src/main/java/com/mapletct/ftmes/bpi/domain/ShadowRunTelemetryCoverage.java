package com.mapletct.ftmes.bpi.domain;

import java.time.Instant;
import java.util.List;

public record ShadowRunTelemetryCoverage(
        boolean windowStarted,
        Instant windowStart,
        Instant windowEnd,
        int pinnedPointCount,
        int observedPointCount,
        int authoritativeSequencePointCount,
        int calibratedPointCount,
        int goodQualityPointCount,
        long acceptedEventCount,
        long acceptedObservationCount,
        long rejectedObservationCount,
        long gapEventCount,
        long outOfOrderEventCount,
        Instant firstObservedAt,
        Instant lastObservedAt,
        boolean fullyCovered,
        List<String> blockers) {
}
