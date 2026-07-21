package com.mapletct.ftmes.qcsoutbox;

import java.math.BigDecimal;

public class ResolvedBpiBatch {

    private String id;
    private String batchNo;
    private String tenantId;
    private String plantId;
    private String lineId;
    private String orderId;
    private String materialCode;
    private String state;
    private long revision;
    private boolean shadow;
    private BigDecimal quantity;
    private String quantityUnit;
    private String currentQualityGateId;
    private Long currentQualityGateRevision;
    private String currentQualityGateSourceEventId;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getPlantId() { return plantId; }
    public void setPlantId(String plantId) { this.plantId = plantId; }
    public String getLineId() { return lineId; }
    public void setLineId(String lineId) { this.lineId = lineId; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getMaterialCode() { return materialCode; }
    public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public long getRevision() { return revision; }
    public void setRevision(long revision) { this.revision = revision; }
    public boolean isShadow() { return shadow; }
    public void setShadow(boolean shadow) { this.shadow = shadow; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public String getQuantityUnit() { return quantityUnit; }
    public void setQuantityUnit(String quantityUnit) { this.quantityUnit = quantityUnit; }
    public String getCurrentQualityGateId() { return currentQualityGateId; }
    public void setCurrentQualityGateId(String currentQualityGateId) {
        this.currentQualityGateId = currentQualityGateId;
    }
    public Long getCurrentQualityGateRevision() { return currentQualityGateRevision; }
    public void setCurrentQualityGateRevision(Long currentQualityGateRevision) {
        this.currentQualityGateRevision = currentQualityGateRevision;
    }
    public String getCurrentQualityGateSourceEventId() { return currentQualityGateSourceEventId; }
    public void setCurrentQualityGateSourceEventId(String currentQualityGateSourceEventId) {
        this.currentQualityGateSourceEventId = currentQualityGateSourceEventId;
    }
}
