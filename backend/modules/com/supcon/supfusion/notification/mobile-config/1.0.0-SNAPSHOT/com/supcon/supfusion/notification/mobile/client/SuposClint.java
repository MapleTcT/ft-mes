package com.supcon.supfusion.notification.mobile.client;

import com.supcon.supfusion.notification.mobile.constant.SuposConfiguration;
import com.supcon.supfusion.notification.mobile.util.AKSKUtil;
import com.supcon.supfusion.notification.mobile.util.UrlUtils;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import static com.supcon.supfusion.notification.mobile.constant.Constants.*;


@Service("mobileSuposClint")
public class SuposClint {
    @Qualifier("mobileSuposConfiguration")
    @Autowired
    private SuposConfiguration akskConfiguration;
    public String getSystemConfig(String appId) {
        String signature = AKSKUtil.signature(akskConfiguration.getSk(), "GET", String.format(URL_GET_SYS_CONFIG, appId), "application/json;charset=utf-8", "", "", "");
        String authorization = "Sign " + akskConfiguration.getAk() + "-" + signature;
        Map<String, String> headers = new HashMap<>();
        headers.put(HTTP_HEADER_AUTHORIZATION, authorization);

        return UrlUtils.actGetURLWithoutLog(akskConfiguration.getSuposHost() + String.format(URL_GET_SYS_CONFIG, appId), headers);
    }

    public String getAllPerson() {
        String signature = AKSKUtil.signature(akskConfiguration.getSk(), "GET", URL_GET_PERSON_DETAIL, "application/json;charset=utf-8", "", "", "");
        String authorization = "Sign " + akskConfiguration.getAk() + "-" + signature;
        Map<String, String> headers = new HashMap<>();
        headers.put(HTTP_HEADER_AUTHORIZATION, authorization);

        return UrlUtils.actGetURL(akskConfiguration.getSuposHost() + URL_GET_PERSON_DETAIL, headers);
    }

    public void synchronousMessageStatus(String body) {
//        String signature = AKSKUtil.signature(akskConfiguration.getSk(), "POST", URL_POST_NOTICE_STATUS, "application/json;charset=utf-8", "", "", "");
//        String authorization = "Sign " + akskConfiguration.getAk() + "-" + signature;
        Map<String, String> headers = new HashMap<>();
//        headers.put(HTTP_HEADER_AUTHORIZATION, authorization);

        UrlUtils.actPostURL(akskConfiguration.getSuposHost() + URL_POST_NOTICE_STATUS, headers, body);
    }

    public void addSystemConfig(String body) {
        String signature = AKSKUtil.signature(akskConfiguration.getSk(), "POST", URL_POST_SYS_CONFIG, "application/json;charset=utf-8", "", "", "");
        String authorization = "Sign " + akskConfiguration.getAk() + "-" + signature;
        Map<String, String> headers = new HashMap<>();
        headers.put(HTTP_HEADER_AUTHORIZATION, authorization);

        UrlUtils.actPostURL(akskConfiguration.getSuposHost() + URL_POST_SYS_CONFIG, headers, body);
    }

    public void registerMobile(String body) {
        String signature = AKSKUtil.signature(akskConfiguration.getSk(), "POST", URL_POST_REGISTER_CONFIG, "application/json;charset=utf-8", "", "", "");
        String authorization = "Sign " + akskConfiguration.getAk() + "-" + signature;
        Map<String, String> headers = new HashMap<>();
        headers.put(HTTP_HEADER_AUTHORIZATION, authorization);

        UrlUtils.actPostURL(akskConfiguration.getSuposHost() + URL_POST_REGISTER_CONFIG, headers, body);
    }
}
