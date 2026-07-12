package com.mapletct.ftmes.bpi.stream;

public record BoundaryProcessingIssue(
        String code,
        String keyedLocality,
        String ruleKey,
        String eventId,
        long eventTimeMs,
        String message) {
}
