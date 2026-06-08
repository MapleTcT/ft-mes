package com.supcon.supfusion.notification.sms.service.runner;

import com.supcon.supfusion.notification.sms.config.SuposConfiguration;
import com.supcon.supfusion.notification.sms.util.AKSKUtil;
import feign.Logger;
import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

import java.util.*;

import static com.supcon.supfusion.notification.sms.Constants.HTTP_HEADER_AUTHORIZATION;

//@Configuration
@Slf4j
public class ClientConfiguration {


    @Autowired
    SuposConfiguration suposConfiguration;

    @Value("${SUPOS_APP_TENANT_ID:dt}")
    String SUPOS_APP_TENANT_ID;

    @Bean
    @ConditionalOnMissingBean
    Logger.Level feignLoggerLevel() {
        //这里记录所有，根据实际情况选择合适的日志level
        return Logger.Level.FULL;
    }

    @Bean
    public RequestInterceptor headerInterceptor() {
        return template -> {
            String url = template.url();
            String method = template.method();
            String ContentType = template.headers().get("Content-Type").stream().findAny().orElse("");
            Map<String, Collection<String>> queries = template.queries();
            String signature = AKSKUtil.signature(
                    suposConfiguration.getSk(),
                    method,
                    url,
                    ContentType,
                    getCanonicalQueryString(template.queries()),
                    getCanonicalCustomHeaders(template.headers())
                    , ""
            );

            String authorization = "Sign " + suposConfiguration.getAk() + "-" + signature;
            Map<String, Collection<String>> headers = new HashMap<>();
            headers.put(HTTP_HEADER_AUTHORIZATION, Arrays.asList(authorization));
            headers.put("X-Tenant-Id", Arrays.asList(SUPOS_APP_TENANT_ID));

            template.headers(headers);

        };
    }


    private String getCanonicalQueryString(Map<String, Collection<String>> queryParams) {

        final Map<String, String> params = new TreeMap<>();
        for (Map.Entry<String, Collection<String>> entry : queryParams.entrySet()) {
            String key = entry.getKey().toLowerCase();
            entry.getValue().stream().findAny().ifPresent(x -> {
                params.put(key, x);
            });

        }
        StringBuilder sb = new StringBuilder();
        for (String key : params.keySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(key).append("=").append(params.get(key));
        }
        return sb.toString();
    }

    private String getCanonicalCustomHeaders(Map<String, Collection<String>> headers) {

        if (headers.isEmpty()) {
            return "";
        }
        Map<String, String> params = new TreeMap<>();
        for (Map.Entry<String, Collection<String>> entry : headers.entrySet()) {
            String key = entry.getKey().toLowerCase();
            if (key.startsWith("x-mc-")) {
                entry.getValue().stream().findAny().ifPresent(x -> {
                    params.put(key, x);
                });

            }
        }
        StringBuilder sb = new StringBuilder();
        for (String key : params.keySet()) {
            if (sb.length() > 0) {
                sb.append(";");
            }
            sb.append(key).append(":").append(params.get(key));
        }
        return sb.toString();
    }

}
