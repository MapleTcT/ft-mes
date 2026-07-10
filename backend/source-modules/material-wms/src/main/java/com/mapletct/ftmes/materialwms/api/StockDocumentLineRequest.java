package com.mapletct.ftmes.materialwms.api;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StockDocumentLineRequest {

    private String srcPartId;
    private String goodCode;
    private String batchText;
    @JsonAlias({"produceBatchNum", "productBatch"})
    private String productionBatchNo;
    private String placeSetCode;
    private BigDecimal quantity;
    @JsonAlias({"pruductDate", "productDate"})
    private String productionDate;
    private String checkResult;
    @JsonAlias({"detailMemo", "memo"})
    private String memo;

    public String getSrcPartId() {
        return srcPartId;
    }

    public void setSrcPartId(String srcPartId) {
        this.srcPartId = srcPartId;
    }

    public String getGoodCode() {
        return goodCode;
    }

    public void setGoodCode(String goodCode) {
        this.goodCode = goodCode;
    }

    public String getBatchText() {
        return batchText;
    }

    public void setBatchText(String batchText) {
        this.batchText = batchText;
    }

    public String getProductionBatchNo() {
        return productionBatchNo;
    }

    public void setProductionBatchNo(String productionBatchNo) {
        this.productionBatchNo = productionBatchNo;
    }

    public String getPlaceSetCode() {
        return placeSetCode;
    }

    public void setPlaceSetCode(String placeSetCode) {
        this.placeSetCode = placeSetCode;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getProductionDate() {
        return productionDate;
    }

    public void setProductionDate(String productionDate) {
        this.productionDate = productionDate;
    }

    public String getCheckResult() {
        return checkResult;
    }

    public void setCheckResult(String checkResult) {
        this.checkResult = checkResult;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }
}
