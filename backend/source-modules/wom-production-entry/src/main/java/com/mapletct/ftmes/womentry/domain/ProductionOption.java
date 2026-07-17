package com.mapletct.ftmes.womentry.domain;

public class ProductionOption {

    private final long productId;
    private final String productCode;
    private final String productName;
    private final long formulaId;
    private final String formulaCode;
    private final String formulaName;
    private final long lineId;
    private final String lineCode;
    private final String lineName;
    private final Long unitId;
    private final String unitName;
    private final String unitSymbol;

    public ProductionOption(
            long productId,
            String productCode,
            String productName,
            long formulaId,
            String formulaCode,
            String formulaName,
            long lineId,
            String lineCode,
            String lineName,
            Long unitId,
            String unitName,
            String unitSymbol) {
        this.productId = productId;
        this.productCode = productCode;
        this.productName = productName;
        this.formulaId = formulaId;
        this.formulaCode = formulaCode;
        this.formulaName = formulaName;
        this.lineId = lineId;
        this.lineCode = lineCode;
        this.lineName = lineName;
        this.unitId = unitId;
        this.unitName = unitName;
        this.unitSymbol = unitSymbol;
    }

    public long getProductId() {
        return productId;
    }

    public String getProductCode() {
        return productCode;
    }

    public String getProductName() {
        return productName;
    }

    public long getFormulaId() {
        return formulaId;
    }

    public String getFormulaCode() {
        return formulaCode;
    }

    public String getFormulaName() {
        return formulaName;
    }

    public long getLineId() {
        return lineId;
    }

    public String getLineCode() {
        return lineCode;
    }

    public String getLineName() {
        return lineName;
    }

    public Long getUnitId() {
        return unitId;
    }

    public String getUnitName() {
        return unitName;
    }

    public String getUnitSymbol() {
        return unitSymbol;
    }

    public boolean isEligible() {
        return unitId != null;
    }

    public String getIssue() {
        return isEligible() ? "" : "产品未配置生产单位";
    }
}
