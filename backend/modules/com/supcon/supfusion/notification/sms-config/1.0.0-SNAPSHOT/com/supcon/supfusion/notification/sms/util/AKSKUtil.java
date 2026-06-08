package com.supcon.supfusion.notification.sms.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;

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

        log.info("sk: {}, requestString: {}", sk, requestString);
        HmacUtils hmacSha256 = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, sk);
        String genSign = hmacSha256.hmacHex(requestString);
        return genSign;
    }
}
