package com.mapletct.ftmes.materialwms.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class QualityAllocationRequest {

    private String requestId;
    private String action;
    private String qualityReportId;
    private String taskId;
    private String sourceLineId;
    private BigDecimal totalQuantity;
    private BigDecimal goodQuantity;
    private BigDecimal badQuantity;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getQualityReportId() {
        return qualityReportId;
    }

    public void setQualityReportId(String qualityReportId) {
        this.qualityReportId = qualityReportId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getSourceLineId() {
        return sourceLineId;
    }

    public void setSourceLineId(String sourceLineId) {
        this.sourceLineId = sourceLineId;
    }

    public BigDecimal getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(BigDecimal totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public BigDecimal getGoodQuantity() {
        return goodQuantity;
    }

    public void setGoodQuantity(BigDecimal goodQuantity) {
        this.goodQuantity = goodQuantity;
    }

    public BigDecimal getBadQuantity() {
        return badQuantity;
    }

    public void setBadQuantity(BigDecimal badQuantity) {
        this.badQuantity = badQuantity;
    }
}
