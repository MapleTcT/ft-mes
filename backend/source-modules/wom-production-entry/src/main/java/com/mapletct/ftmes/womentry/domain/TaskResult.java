package com.mapletct.ftmes.womentry.domain;

public class TaskResult {

    private final long taskId;
    private final int version;
    private final String tableNo;
    private final String batchCode;
    private final int status;
    private final boolean valid;
    private final Long pendingId;
    private final String pendingOpenUrl;
    private final String activityName;

    public TaskResult(
            long taskId,
            int version,
            String tableNo,
            String batchCode,
            int status,
            boolean valid,
            Long pendingId,
            String pendingOpenUrl,
            String activityName) {
        this.taskId = taskId;
        this.version = version;
        this.tableNo = tableNo;
        this.batchCode = batchCode;
        this.status = status;
        this.valid = valid;
        this.pendingId = pendingId;
        this.pendingOpenUrl = pendingOpenUrl;
        this.activityName = activityName;
    }

    public long getTaskId() {
        return taskId;
    }

    public int getVersion() {
        return version;
    }

    public String getTableNo() {
        return tableNo;
    }

    public String getBatchCode() {
        return batchCode;
    }

    public int getStatus() {
        return status;
    }

    public boolean isValid() {
        return valid;
    }

    public Long getPendingId() {
        return pendingId;
    }

    public String getPendingOpenUrl() {
        return pendingOpenUrl;
    }

    public String getActivityName() {
        return activityName;
    }
}
