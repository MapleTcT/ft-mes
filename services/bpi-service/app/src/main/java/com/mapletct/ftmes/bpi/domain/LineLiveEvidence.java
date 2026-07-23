package com.mapletct.ftmes.bpi.domain;

import java.time.Instant;
import java.util.List;

public record LineLiveEvidence(
        LineState line,
        Instant windowStart,
        Instant windowEnd,
        List<LineTelemetrySample> samples,
        List<LineEvidenceCheck> checks,
        List<LineIncidentSnapshot> incidents) {
}
