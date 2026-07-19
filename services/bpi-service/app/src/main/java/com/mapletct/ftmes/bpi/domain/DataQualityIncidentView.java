package com.mapletct.ftmes.bpi.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DataQualityIncidentView(
        UUID id,
        String issueCode,
        String severity,
        String state,
        long revision,
        String plantId,
        String lineId,
        String source,
        String deviceId,
        String propertyId,
        List<String> affectedLines,
        List<String> affectedRules,
        List<String> affectedBatches,
        long affectedBatchCount,
        long eventCount,
        Instant firstSeen,
        Instant lastSeen,
        String lastDetail,
        String assignee,
        String acknowledgedBy,
        Instant acknowledgedAt,
        String acknowledgmentReason,
        String resolvedBy,
        Instant resolvedAt,
        String resolutionReason) {

    public DataQualityIncidentView {
        affectedLines = List.copyOf(affectedLines);
        affectedRules = List.copyOf(affectedRules);
        affectedBatches = List.copyOf(affectedBatches);
    }
}
