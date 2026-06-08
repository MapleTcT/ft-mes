package com.supcon.supfusion.notification.app.config.util;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AKSKUtil {
    public static String signature(String sk, String method, String uri, String contentType, String queryString, String customHeaders, String requestBodyPayload) {
        String requestString = new StringBuilder()
                .append(method).append("\n")
                .append(uri).append("\n")
                .append(contentType).append("\n")
                .append(queryString).append("\n")
                .append(customHeaders).append("\n")
                .append(requestBodyPayload).toString();

        log.debug("sk: {}, requestString: {}", sk, requestString);
        return HmacUtil.hmacSha256(sk, requestString);
    }
}
