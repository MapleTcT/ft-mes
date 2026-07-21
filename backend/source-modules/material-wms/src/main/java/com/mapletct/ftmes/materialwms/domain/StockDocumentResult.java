package com.mapletct.ftmes.materialwms.domain;

import java.util.LinkedHashMap;
import java.util.Map;

public final class StockDocumentResult {

    private final long documentId;
    private final String documentNo;
    private final DocumentType documentType;
    private final boolean idempotent;

    public StockDocumentResult(long documentId, String documentNo, DocumentType documentType, boolean idempotent) {
        this.documentId = documentId;
        this.documentNo = documentNo;
        this.documentType = documentType;
        this.idempotent = idempotent;
    }

    public long getDocumentId() {
        return documentId;
    }

    public String getDocumentNo() {
        return documentNo;
    }

    public boolean isIdempotent() {
        return idempotent;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("documentId", documentId);
        result.put("documentNo", documentNo);
        result.put("documentType", documentType.name());
        result.put("idempotent", idempotent);
        return result;
    }
}
