package com.mapletct.ftmes.bpi.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record RuleSimulationView(
        UUID id,
        UUID ruleId,
        String state,
        String checksum,
        Map<String, Object> metrics,
        Map<String, Object> inputManifest,
        List<Instant> emittedBoundaries,
        String failureReason) {
}
