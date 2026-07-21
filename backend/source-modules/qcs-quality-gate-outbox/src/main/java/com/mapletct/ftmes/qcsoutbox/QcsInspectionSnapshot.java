package com.mapletct.ftmes.qcsoutbox;

public class QcsInspectionSnapshot {

    private String inspectionCode;
    private String inspectionRecordId;
    private boolean required;
    private String disposition;
    private boolean finalResult;
    private long observedAtMs;
    private String sourceResult;

    public String getInspectionCode() { return inspectionCode; }
    public void setInspectionCode(String inspectionCode) { this.inspectionCode = inspectionCode; }
    public String getInspectionRecordId() { return inspectionRecordId; }
    public void setInspectionRecordId(String inspectionRecordId) { this.inspectionRecordId = inspectionRecordId; }
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
    public String getDisposition() { return disposition; }
    public void setDisposition(String disposition) { this.disposition = disposition; }
    public boolean isFinalResult() { return finalResult; }
    public void setFinalResult(boolean finalResult) { this.finalResult = finalResult; }
    public long getObservedAtMs() { return observedAtMs; }
    public void setObservedAtMs(long observedAtMs) { this.observedAtMs = observedAtMs; }
    public String getSourceResult() { return sourceResult; }
    public void setSourceResult(String sourceResult) { this.sourceResult = sourceResult; }
}
