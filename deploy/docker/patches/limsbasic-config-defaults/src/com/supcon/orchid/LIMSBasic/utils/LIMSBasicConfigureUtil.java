package com.supcon.orchid.LIMSBasic.utils;

import com.supcon.supfusion.systemconfig.api.tenantconfig.annotation.ClassSystemConfigAnno;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@ClassSystemConfigAnno
public class LIMSBasicConfigureUtil {
    @Value("${LIMSBasic/LIMSBasic.specialResult:}")
    private String specialResult;

    @Value("${LIMSBasic/LIMSBasic.LIMSTrade:}")
    private String LIMSTrade;

    @Value("${LIMSBasic/LIMSBasic.LIMSWareType:}")
    private String LIMSWareType;

    @Value("${LIMSSample/LIMSSample.dataPermission:false}")
    private Boolean dataPermission;

    @Value("${LIMSBasic/LIMSBasic.dataPermission:false}")
    private Boolean baseDataPermission;

    public String getSpecialResult() {
        return specialResult;
    }

    public void setSpecialResult(String specialResult) {
        this.specialResult = specialResult;
    }

    public String getLIMSTrade() {
        return LIMSTrade;
    }

    public void setLIMSTrade(String LIMSTrade) {
        this.LIMSTrade = LIMSTrade;
    }

    public String getLIMSWareType() {
        return LIMSWareType;
    }

    public void setLIMSWareType(String LIMSWareType) {
        this.LIMSWareType = LIMSWareType;
    }

    public Boolean getDataPermission() {
        return Boolean.TRUE.equals(dataPermission);
    }

    public void setDataPermission(Boolean dataPermission) {
        this.dataPermission = dataPermission;
    }

    public Boolean getBaseDataPermission() {
        return Boolean.TRUE.equals(baseDataPermission);
    }

    public void setBaseDataPermission(Boolean baseDataPermission) {
        this.baseDataPermission = baseDataPermission;
    }
}
