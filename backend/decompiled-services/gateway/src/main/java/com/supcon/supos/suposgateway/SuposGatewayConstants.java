/*
 * Decompiled with CFR 0.152.
 */
package com.supcon.supos.suposgateway;

public class SuposGatewayConstants {
    public static final String REQUEST_TIME_STARTED = "requestTimeStarted";
    public static final String X_TENANT_ID = "X-Tenant-Id";
    public static final String X_TRACE_ID = "X-Trace-Id";
    public static final String X_TICKET = "X-Ticket";
    public static final String TOKEN = "token";
    public static final String JWT = "jwt";
    public static final String REFERER = "Referer";
    public static final String AUTHORIZATION = "Authorization";
    public static final String BEARER = "Bearer";
    public static final String SIGN = "Sign";
    public static final String COOKIE = "Cookie";
    public static final String CACHED_REQUEST_BODY = SuposGatewayConstants.qualify("cachedRequestBody");
    public static final String USERID = SuposGatewayConstants.qualify("userId");
    public static final String COMPANYID = SuposGatewayConstants.qualify("companyId");
    public static final String CACHED_RESPONSE_BODY = SuposGatewayConstants.qualify("cachedResponseBody");
    public static final String CACHED_FORM_DATA = SuposGatewayConstants.qualify("cachedFormData");
    public static final String UNKNOWN = "unknown";
    public static final String X_FORWARDED_FOR = "x-forwarded-for";
    public static final String PROXY_CLIENT_IP = "Proxy-Client-IP";
    public static final String WL_PROXY_CLIENT_IP = "WL-Proxy-Client-IP";
    public static final String X_REAL_IP = "X-Real-IP";
    public static final String X_USER_NAME = "X-User-Name";
    public static final String UPGRADE = "upgrade";
    public static final String DEFAULT_CACHE = "defaultCache";
    public static final String SECRET_KEY_CACHE = "secretKeyCache";

    private static String qualify(String attr) {
        return SuposGatewayConstants.class.getName() + "." + attr;
    }
}

