package com.supcon.supfusion.notification.sms.client;


import com.supcon.supfusion.notification.sms.config.SuposConfiguration;
import com.supcon.supfusion.notification.sms.util.AKSKUtil;
import com.supcon.supfusion.notification.sms.util.UrlUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import static com.supcon.supfusion.notification.sms.Constants.*;

@Slf4j
@Service("smsJincangSuposClint")
public class SuposClint {
    @Autowired
    private SuposConfiguration akskConfiguration;


    @Value("${SUPOS_APP_TENANT_ID:dt}")
    String SUPOS_APP_TENANT_ID;

    public String getSystemConfig(String appId) {
        String signature = AKSKUtil.signature(akskConfiguration.getSk(), "GET", String.format(URL_GET_SYS_CONFIG, appId), "application/json;charset=utf-8", "", "", "");
        String authorization = "Sign " + akskConfiguration.getAk() + "-" + signature;
        Map<String, String> headers = new HashMap<>();
        headers.put(HTTP_HEADER_AUTHORIZATION, authorization);
        headers.put("X-Tenant-Id", SUPOS_APP_TENANT_ID);
        return UrlUtils.actGetURLWithoutLog(akskConfiguration.getSuposHost() + String.format(URL_GET_SYS_CONFIG, appId), headers);
    }

    public void addSystemConfig(String body) {
        String signature = AKSKUtil.signature(akskConfiguration.getSk(), "POST", URL_POST_SYS_CONFIG, "application/json;charset=utf-8", "", "", "");
        String authorization = "Sign " + akskConfiguration.getAk() + "-" + signature;
        Map<String, String> headers = new HashMap<>();
        headers.put(HTTP_HEADER_AUTHORIZATION, authorization);
        headers.put("X-Tenant-Id", SUPOS_APP_TENANT_ID);
        UrlUtils.actPostURL(akskConfiguration.getSuposHost() + URL_POST_SYS_CONFIG, headers, body);
    }
}
