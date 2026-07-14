package com.mapletct.ftmes.contextoutbox;

public final class ProductionContextOutboxRecord {

    private final long id;
    private final String eventId;
    private final String topic;
    private final Long womCid;
    private final Long womLineId;
    private final String tenantId;
    private final String plantId;
    private final String lineId;
    private final String orderId;
    private final String taskId;
    private final String materialCode;
    private final String recipeVersion;
    private final String batchId;
    private final String sourceState;
    private final long contextRevision;
    private final boolean active;
    private final long effectiveFromMs;
    private final Long effectiveToMs;
    private final int attemptCount;

    public ProductionContextOutboxRecord(
        long id,
        String eventId,
        String topic,
        Long womCid,
        Long womLineId,
        String tenantId,
        String plantId,
        String lineId,
        String orderId,
        String taskId,
        String materialCode,
        String recipeVersion,
        String batchId,
        String sourceState,
        long contextRevision,
        boolean active,
        long effectiveFromMs,
        Long effectiveToMs,
        int attemptCount
    ) {
        this.id = id;
        this.eventId = eventId;
        this.topic = topic;
        this.womCid = womCid;
        this.womLineId = womLineId;
        this.tenantId = tenantId;
        this.plantId = plantId;
        this.lineId = lineId;
        this.orderId = orderId;
        this.taskId = taskId;
        this.materialCode = materialCode;
        this.recipeVersion = recipeVersion;
        this.batchId = batchId;
        this.sourceState = sourceState;
        this.contextRevision = contextRevision;
        this.active = active;
        this.effectiveFromMs = effectiveFromMs;
        this.effectiveToMs = effectiveToMs;
        this.attemptCount = attemptCount;
    }

    public long getId() { return id; }
    public String getEventId() { return eventId; }
    public String getTopic() { return topic; }
    public Long getWomCid() { return womCid; }
    public Long getWomLineId() { return womLineId; }
    public String getTenantId() { return tenantId; }
    public String getPlantId() { return plantId; }
    public String getLineId() { return lineId; }
    public String getOrderId() { return orderId; }
    public String getTaskId() { return taskId; }
    public String getMaterialCode() { return materialCode; }
    public String getRecipeVersion() { return recipeVersion; }
    public String getBatchId() { return batchId; }
    public String getSourceState() { return sourceState; }
    public long getContextRevision() { return contextRevision; }
    public boolean isActive() { return active; }
    public long getEffectiveFromMs() { return effectiveFromMs; }
    public Long getEffectiveToMs() { return effectiveToMs; }
    public int getAttemptCount() { return attemptCount; }
}
