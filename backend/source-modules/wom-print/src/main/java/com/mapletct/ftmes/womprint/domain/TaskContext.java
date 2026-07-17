package com.mapletct.ftmes.womprint.domain;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TaskContext {

    private final long id;
    private final String tableNo;
    private final String produceBatchNum;
    private final Long productId;
    private final Long lineId;
    private final String productCode;
    private final String productName;
    private final LocalDateTime planStartTime;
    private final Boolean validityManaged;
    private final Integer validPeriod;
    private final String validUnit;

    public TaskContext(
            long id,
            String tableNo,
            String produceBatchNum,
            Long productId,
            Long lineId,
            String productCode,
            String productName,
            LocalDateTime planStartTime,
            Boolean validityManaged,
            Integer validPeriod,
            String validUnit) {
        this.id = id;
        this.tableNo = tableNo;
        this.produceBatchNum = produceBatchNum;
        this.productId = productId;
        this.lineId = lineId;
        this.productCode = productCode;
        this.productName = productName;
        this.planStartTime = planStartTime;
        this.validityManaged = validityManaged;
        this.validPeriod = validPeriod;
        this.validUnit = validUnit;
    }

    public long getId() {
        return id;
    }

    public String getTableNo() {
        return tableNo;
    }

    public String getProduceBatchNum() {
        return produceBatchNum;
    }

    public Long getProductId() {
        return productId;
    }

    public Long getLineId() {
        return lineId;
    }

    public String getProductCode() {
        return productCode;
    }

    public String getProductName() {
        return productName;
    }

    public LocalDateTime getPlanStartTime() {
        return planStartTime;
    }

    public Boolean getValidityManaged() {
        return validityManaged;
    }

    public Integer getValidPeriod() {
        return validPeriod;
    }

    public String getValidUnit() {
        return validUnit;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("taskId", id);
        result.put("taskNo", tableNo);
        result.put("produceBatchNum", produceBatchNum);
        result.put("productId", productId);
        result.put("productCode", productCode);
        result.put("productName", productName);
        result.put("lineId", lineId);
        return result;
    }
}
