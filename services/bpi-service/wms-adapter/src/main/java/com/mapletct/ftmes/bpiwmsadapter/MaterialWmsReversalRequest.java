package com.mapletct.ftmes.bpiwmsadapter;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MaterialWmsReversalRequest(
        String tenantId,
        String sourceDocumentId,
        String idempotencyKey,
        String originalDocumentNo,
        String sourceDocumentNo,
        String directiveNo,
        String companyCode,
        String warehouseCode,
        LocalDate storageDate,
        String sourceLineId,
        String materialCode,
        String batchNo,
        String productionBatchNo,
        String locationCode,
        BigDecimal quantity,
        String unitCode,
        String memo) {
}
