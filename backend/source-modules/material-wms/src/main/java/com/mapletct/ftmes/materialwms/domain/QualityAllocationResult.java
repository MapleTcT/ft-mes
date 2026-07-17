package com.mapletct.ftmes.materialwms.domain;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public final class QualityAllocationResult {

    private final String sourceLineId;
    private final String qualityReportId;
    private final String status;
    private final BigDecimal totalQuantity;
    private final BigDecimal goodQuantity;
    private final BigDecimal badQuantity;
    private final int appliedLines;
    private final boolean idempotent;

    public QualityAllocationResult(
            String sourceLineId,
            String qualityReportId,
            String status,
            BigDecimal totalQuantity,
            BigDecimal goodQuantity,
            BigDecimal badQuantity,
            int appliedLines,
            boolean idempotent) {
        this.sourceLineId = sourceLineId;
        this.qualityReportId = qualityReportId;
        this.status = status;
        this.totalQuantity = totalQuantity;
        this.goodQuantity = goodQuantity;
        this.badQuantity = badQuantity;
        this.appliedLines = appliedLines;
        this.idempotent = idempotent;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("sourceLineId", sourceLineId);
        result.put("qualityReportId", qualityReportId);
        result.put("status", status);
        result.put("totalQuantity", totalQuantity);
        result.put("goodQuantity", goodQuantity);
        result.put("badQuantity", badQuantity);
        result.put("appliedLines", appliedLines);
        result.put("pendingInbound", appliedLines == 0);
        result.put("idempotent", idempotent);
        return result;
    }
}
