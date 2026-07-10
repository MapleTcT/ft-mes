package com.mapletct.ftmes.materialwms.domain;

import java.util.LinkedHashMap;
import java.util.Map;

public final class QualityUpdateResult {

    private final String sourceLineId;
    private final QualityStatus qualityStatus;
    private final int appliedLines;
    private final long revision;

    public QualityUpdateResult(String sourceLineId, QualityStatus qualityStatus, int appliedLines, long revision) {
        this.sourceLineId = sourceLineId;
        this.qualityStatus = qualityStatus;
        this.appliedLines = appliedLines;
        this.revision = revision;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("sourceLineId", sourceLineId);
        result.put("qualityStatus", qualityStatus.name());
        result.put("appliedLines", appliedLines);
        result.put("pendingInbound", appliedLines == 0);
        result.put("revision", revision);
        return result;
    }
}
