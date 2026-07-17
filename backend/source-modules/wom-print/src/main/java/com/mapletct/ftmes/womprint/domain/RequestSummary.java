package com.mapletct.ftmes.womprint.domain;

public final class RequestSummary {

    private final String requestHash;
    private final int recordCount;

    public RequestSummary(String requestHash, int recordCount) {
        this.requestHash = requestHash;
        this.recordCount = recordCount;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public int getRecordCount() {
        return recordCount;
    }
}
