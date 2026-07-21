package com.mapletct.ftmes.bpiwmsadapter;

import java.util.List;

public record MaterialWmsReversalDocument(
        long internalId,
        String documentNo,
        String documentType,
        String sourceSystem,
        String sourceDocumentId,
        String idempotencyKey,
        String warehouseCode,
        String status,
        String qualityStatus,
        long originalInternalId,
        OriginalDocument originalDocument,
        List<MaterialWmsDocument.Line> lines) {

    public record OriginalDocument(
            long internalId,
            String documentNo,
            String documentType,
            String sourceSystem,
            String sourceDocumentId,
            String idempotencyKey,
            String warehouseCode,
            String status,
            String qualityStatus) {
    }
}
