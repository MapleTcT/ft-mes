package com.mapletct.ftmes.bpi.domain;

import java.util.Map;

public record DataQualitySummary(
        long open,
        long acknowledged,
        long resolved,
        long critical,
        long affectedBatches,
        Map<String, Long> issueCounts) {

    public DataQualitySummary {
        issueCounts = Map.copyOf(issueCounts);
    }
}
