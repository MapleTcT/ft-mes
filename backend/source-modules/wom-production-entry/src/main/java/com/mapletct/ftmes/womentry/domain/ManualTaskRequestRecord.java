package com.mapletct.ftmes.womentry.domain;

import java.time.LocalDateTime;

public class ManualTaskRequestRecord {

    private final String tenantId;
    private final String requestId;
    private final String requestHash;
    private final String batchCode;
    private final String status;
    private final Long taskId;
    private final LocalDateTime updatedAt;

    public ManualTaskRequestRecord(
            String tenantId,
            String requestId,
            String requestHash,
            String batchCode,
            String status,
            Long taskId,
            LocalDateTime updatedAt) {
        this.tenantId = tenantId;
        this.requestId = requestId;
        this.requestHash = requestHash;
        this.batchCode = batchCode;
        this.status = status;
        this.taskId = taskId;
        this.updatedAt = updatedAt;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public String getBatchCode() {
        return batchCode;
    }

    public String getStatus() {
        return status;
    }

    public Long getTaskId() {
        return taskId;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
