package com.mapletct.ftmes.womquality.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BadQuantityCreateRequest {

    private String requestId;
    private String taskId;
    private String sourceOutputId;
    private BigDecimal badQuantity;
    private String unitCode;
    private String reasonCode;
    private String reasonText;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getSourceOutputId() {
        return sourceOutputId;
    }

    public void setSourceOutputId(String sourceOutputId) {
        this.sourceOutputId = sourceOutputId;
    }

    public BigDecimal getBadQuantity() {
        return badQuantity;
    }

    public void setBadQuantity(BigDecimal badQuantity) {
        this.badQuantity = badQuantity;
    }

    public String getUnitCode() {
        return unitCode;
    }

    public void setUnitCode(String unitCode) {
        this.unitCode = unitCode;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getReasonText() {
        return reasonText;
    }

    public void setReasonText(String reasonText) {
        this.reasonText = reasonText;
    }
}
