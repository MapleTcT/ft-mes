package com.mapletct.ftmes.materialwms.api;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StockDocumentRequest {

    @JsonAlias({"srcID", "srcId"})
    private String sourceDocumentId;
    private String srcTableNo;
    private String directiveNo;
    private String companyCode;
    private String deptCode;
    private String staffCode;
    private String userName;
    private String wareCode;
    private String storageDate;
    private String comeType;
    private String redBlue;
    @JsonAlias({"handRemarks", "memo"})
    private String memo;
    private List<StockDocumentLineRequest> detailList = new ArrayList<StockDocumentLineRequest>();

    public String getSourceDocumentId() {
        return sourceDocumentId;
    }

    public void setSourceDocumentId(String sourceDocumentId) {
        this.sourceDocumentId = sourceDocumentId;
    }

    public String getSrcTableNo() {
        return srcTableNo;
    }

    public void setSrcTableNo(String srcTableNo) {
        this.srcTableNo = srcTableNo;
    }

    public String getDirectiveNo() {
        return directiveNo;
    }

    public void setDirectiveNo(String directiveNo) {
        this.directiveNo = directiveNo;
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public String getDeptCode() {
        return deptCode;
    }

    public void setDeptCode(String deptCode) {
        this.deptCode = deptCode;
    }

    public String getStaffCode() {
        return staffCode;
    }

    public void setStaffCode(String staffCode) {
        this.staffCode = staffCode;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getWareCode() {
        return wareCode;
    }

    public void setWareCode(String wareCode) {
        this.wareCode = wareCode;
    }

    public String getStorageDate() {
        return storageDate;
    }

    public void setStorageDate(String storageDate) {
        this.storageDate = storageDate;
    }

    public String getComeType() {
        return comeType;
    }

    public void setComeType(String comeType) {
        this.comeType = comeType;
    }

    public String getRedBlue() {
        return redBlue;
    }

    public void setRedBlue(String redBlue) {
        this.redBlue = redBlue;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public List<StockDocumentLineRequest> getDetailList() {
        return detailList;
    }

    public void setDetailList(List<StockDocumentLineRequest> detailList) {
        this.detailList = detailList;
    }
}
