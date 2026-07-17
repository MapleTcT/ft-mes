package com.mapletct.ftmes.womentry.support;

public class RequestAuthContext {

    private final String authorization;
    private final String cookie;
    private final String tenantId;
    private final String acceptLanguage;

    public RequestAuthContext(String authorization, String cookie, String tenantId, String acceptLanguage) {
        this.authorization = authorization;
        this.cookie = cookie;
        this.tenantId = tenantId;
        this.acceptLanguage = acceptLanguage;
    }

    public String getAuthorization() {
        return authorization;
    }

    public String getCookie() {
        return cookie;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getAcceptLanguage() {
        return acceptLanguage;
    }
}
