package com.mapletct.ftmes.womentry.domain;

public class CreateInstructionResult {

    private final String requestId;
    private final boolean idempotent;
    private final TaskResult task;

    public CreateInstructionResult(String requestId, boolean idempotent, TaskResult task) {
        this.requestId = requestId;
        this.idempotent = idempotent;
        this.task = task;
    }

    public String getRequestId() {
        return requestId;
    }

    public boolean isIdempotent() {
        return idempotent;
    }

    public TaskResult getTask() {
        return task;
    }
}
