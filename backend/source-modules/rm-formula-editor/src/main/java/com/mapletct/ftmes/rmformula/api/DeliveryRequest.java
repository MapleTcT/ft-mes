package com.mapletct.ftmes.rmformula.api;

public class DeliveryRequest {
    private String requestId;
    private Long revisionId;

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public Long getRevisionId() { return revisionId; }
    public void setRevisionId(Long revisionId) { this.revisionId = revisionId; }
}
