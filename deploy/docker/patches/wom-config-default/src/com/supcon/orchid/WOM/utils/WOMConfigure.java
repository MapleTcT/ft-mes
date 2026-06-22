package com.supcon.orchid.WOM.utils;

import com.supcon.supfusion.systemconfig.api.tenantconfig.annotation.ClassSystemConfigAnno;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@ClassSystemConfigAnno
public class WOMConfigure {
    @Value("${BaseSet/BaseSet.isEnable:}")
    public Boolean batchIsEnable;

    @Value("${WOM/WOM.mesProBatch:}")
    public Boolean mesProBatch;

    @Value("${RM/RM.Batch.username:}")
    public String loginNameForBatch;

    @Value("${RM/RM.Batch.password:}")
    public String password;

    @Value("${WOM/WOM.batchReportFileUrl:}")
    public String batchReportFileUrl;

    @Value("${WOM/WOM.kafkaipAndportWom:}")
    public String kafkaipAndportWom;

    @Value("${WOM/WOM.autoStartup:false}")
    public Boolean autoStartup;

    public Boolean getBatchIsEnable() {
        return batchIsEnable;
    }

    public void setBatchIsEnable(Boolean batchIsEnable) {
        this.batchIsEnable = batchIsEnable;
    }

    public Boolean getMesProBatch() {
        return mesProBatch != null ? mesProBatch : Boolean.FALSE;
    }

    public void setMesProBatch(Boolean mesProBatch) {
        this.mesProBatch = mesProBatch;
    }

    public String getLoginNameForBatch() {
        return loginNameForBatch;
    }

    public void setLoginNameForBatch(String loginNameForBatch) {
        this.loginNameForBatch = loginNameForBatch;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getBatchReportFileUrl() {
        return batchReportFileUrl;
    }

    public void setBatchReportFileUrl(String batchReportFileUrl) {
        this.batchReportFileUrl = batchReportFileUrl;
    }

    public String getKafkaipAndportWom() {
        return kafkaipAndportWom != null ? kafkaipAndportWom : "kafka:9092";
    }

    public void setKafkaipAndportWom(String kafkaipAndportWom) {
        this.kafkaipAndportWom = kafkaipAndportWom;
    }

    public Boolean getAutoStartup() {
        return autoStartup != null ? autoStartup : Boolean.FALSE;
    }

    public void setAutoStartup(Boolean autoStartup) {
        this.autoStartup = autoStartup;
    }
}
