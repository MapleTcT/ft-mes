package com.supcon.orchid.RM.utils;

import com.supcon.supfusion.systemconfig.api.tenantconfig.annotation.ClassSystemConfigAnno;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

@Component
@ClassSystemConfigAnno
@DependsOn("annoFilter")
public class RMConfigure {
    @Value("${BaseSet/BaseSet.isEnable:}")
    public Boolean batchIsEnable;

    @Value("${RM/RM.MQ.brokerUrl:}")
    public String brokerUrl;

    @Value("${RM/RM.Batch.username:}")
    public String loginNameForBatch;

    @Value("${RM/RM.Batch.password:}")
    public String password;

    @Value("${RM/RM.isJiQun:}")
    public Boolean isJiQun;

    public Boolean getBatchIsEnable() {
        return batchIsEnable != null ? batchIsEnable : Boolean.TRUE;
    }

    public void setBatchIsEnable(Boolean batchIsEnable) {
        this.batchIsEnable = batchIsEnable;
    }

    public String getBrokerUrl() {
        return nonBlank(brokerUrl, "localhost");
    }

    public void setBrokerUrl(String brokerUrl) {
        this.brokerUrl = brokerUrl;
    }

    public String getPassword() {
        return nonBlank(password, "123456");
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getLoginNameForBatch() {
        return nonBlank(loginNameForBatch, "admin");
    }

    public void setLoginNameForBatch(String loginNameForBatch) {
        this.loginNameForBatch = loginNameForBatch;
    }

    public Boolean getJiQun() {
        return isJiQun != null ? isJiQun : Boolean.FALSE;
    }

    public void setJiQun(Boolean jiQun) {
        isJiQun = jiQun;
    }

    private static String nonBlank(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}
