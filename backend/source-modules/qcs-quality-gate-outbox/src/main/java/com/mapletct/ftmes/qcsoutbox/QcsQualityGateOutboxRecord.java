package com.mapletct.ftmes.qcsoutbox;

public class QcsQualityGateOutboxRecord {

    private final long id;
    private final String eventId;
    private final String idempotencyKey;
    private final String topic;
    private final long qcsReportId;
    private final int qcsReportVersion;
    private final Long qcsInspectId;
    private final Long womTaskId;
    private final String tenantId;
    private final String plantId;
    private final String lineId;
    private final String sourceOrderId;
    private final String sourceBatchCode;
    private final String qualityGateId;
    private final long qualityGateRevision;
    private final long observedAtMs;
    private final String inspectionsJson;
    private final int attemptCount;

    public QcsQualityGateOutboxRecord(
            long id,
            String eventId,
            String idempotencyKey,
            String topic,
            long qcsReportId,
            int qcsReportVersion,
            Long qcsInspectId,
            Long womTaskId,
            String tenantId,
            String plantId,
            String lineId,
            String sourceOrderId,
            String sourceBatchCode,
            String qualityGateId,
            long qualityGateRevision,
            long observedAtMs,
            String inspectionsJson,
            int attemptCount) {
        this.id = id;
        this.eventId = eventId;
        this.idempotencyKey = idempotencyKey;
        this.topic = topic;
        this.qcsReportId = qcsReportId;
        this.qcsReportVersion = qcsReportVersion;
        this.qcsInspectId = qcsInspectId;
        this.womTaskId = womTaskId;
        this.tenantId = tenantId;
        this.plantId = plantId;
        this.lineId = lineId;
        this.sourceOrderId = sourceOrderId;
        this.sourceBatchCode = sourceBatchCode;
        this.qualityGateId = qualityGateId;
        this.qualityGateRevision = qualityGateRevision;
        this.observedAtMs = observedAtMs;
        this.inspectionsJson = inspectionsJson;
        this.attemptCount = attemptCount;
    }

    public long getId() { return id; }
    public String getEventId() { return eventId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getTopic() { return topic; }
    public long getQcsReportId() { return qcsReportId; }
    public int getQcsReportVersion() { return qcsReportVersion; }
    public Long getQcsInspectId() { return qcsInspectId; }
    public Long getWomTaskId() { return womTaskId; }
    public String getTenantId() { return tenantId; }
    public String getPlantId() { return plantId; }
    public String getLineId() { return lineId; }
    public String getSourceOrderId() { return sourceOrderId; }
    public String getSourceBatchCode() { return sourceBatchCode; }
    public String getQualityGateId() { return qualityGateId; }
    public long getQualityGateRevision() { return qualityGateRevision; }
    public long getObservedAtMs() { return observedAtMs; }
    public String getInspectionsJson() { return inspectionsJson; }
    public int getAttemptCount() { return attemptCount; }
}
