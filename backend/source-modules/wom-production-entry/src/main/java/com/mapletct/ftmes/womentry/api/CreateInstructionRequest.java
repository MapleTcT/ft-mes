package com.mapletct.ftmes.womentry.api;

import java.math.BigDecimal;

public class CreateInstructionRequest {

    private String requestId;
    private String productCode;
    private String formulaCode;
    private Long workLineId;
    private BigDecimal planNum;
    private String planStartDate;
    private String planEndDate;
    private String batchCode;
    private Boolean needPack;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getFormulaCode() {
        return formulaCode;
    }

    public void setFormulaCode(String formulaCode) {
        this.formulaCode = formulaCode;
    }

    public Long getWorkLineId() {
        return workLineId;
    }

    public void setWorkLineId(Long workLineId) {
        this.workLineId = workLineId;
    }

    public BigDecimal getPlanNum() {
        return planNum;
    }

    public void setPlanNum(BigDecimal planNum) {
        this.planNum = planNum;
    }

    public String getPlanStartDate() {
        return planStartDate;
    }

    public void setPlanStartDate(String planStartDate) {
        this.planStartDate = planStartDate;
    }

    public String getPlanEndDate() {
        return planEndDate;
    }

    public void setPlanEndDate(String planEndDate) {
        this.planEndDate = planEndDate;
    }

    public String getBatchCode() {
        return batchCode;
    }

    public void setBatchCode(String batchCode) {
        this.batchCode = batchCode;
    }

    public Boolean getNeedPack() {
        return needPack;
    }

    public void setNeedPack(Boolean needPack) {
        this.needPack = needPack;
    }
}
