package com.mapletct.ftmes.bpiwmsadapter;

import java.math.BigDecimal;
import java.util.List;

public record MaterialWmsDocument(
        long internalId,
        String documentNo,
        String sourceSystem,
        String sourceDocumentId,
        String idempotencyKey,
        String warehouseCode,
        String status,
        String qualityStatus,
        List<Line> lines) {

    public record Line(
            String sourceSystem,
            String sourceLineId,
            String materialCode,
            String batchNo,
            String productionBatchNo,
            String warehouseCode,
            String locationCode,
            BigDecimal quantity,
            String unitCode,
            String qualityStatus) {
    }
}
