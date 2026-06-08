/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.http.HttpMethod
 */
package com.supcon.supos.suposgateway.enums;

import org.springframework.http.HttpMethod;

public enum MethodUrlEnum {
    GET(HttpMethod.GET, "getUrl", "completeMatchUrl_GET", "regMatchUrl_GET", "GET"),
    POST(HttpMethod.POST, "postUrl", "completeMatchUrl_POST", "regMatchUrl_POST", "POST"),
    PUT(HttpMethod.PUT, "putUrl", "completeMatchUrl_PUT", "regMatchUrl_PUT", "PUT"),
    DELETE(HttpMethod.DELETE, "deleteUrl", "completeMatchUrl_DELETE", "regMatchUrl_DELETE", "DELETE");

    private HttpMethod method;
    private String url;
    private String completeMatchUrl;
    private String regMatchUrl;
    private String urlmethod;

    public String getCompleteMatchUrl() {
        return this.completeMatchUrl;
    }

    public void setCompleteMatchUrl(String completeMatchUrl) {
        this.completeMatchUrl = completeMatchUrl;
    }

    public String getRegMatchUrl() {
        return this.regMatchUrl;
    }

    public void setRegMatchUrl(String regMatchUrl) {
        this.regMatchUrl = regMatchUrl;
    }

    public HttpMethod getMethod() {
        return this.method;
    }

    public void setMethod(HttpMethod method) {
        this.method = method;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    private MethodUrlEnum(HttpMethod method, String url, String completeMatchUrl, String regMatchUrl, String urlmethod) {
        this.method = method;
        this.url = url;
        this.completeMatchUrl = completeMatchUrl;
        this.regMatchUrl = regMatchUrl;
        this.urlmethod = urlmethod;
    }

    public static String getUrlByMethod(HttpMethod method) {
        for (MethodUrlEnum methodUrlEnum : MethodUrlEnum.values()) {
            if (!methodUrlEnum.method.equals((Object)method)) continue;
            return methodUrlEnum.url;
        }
        return null;
    }

    public static String getCompleteUrlByMethod(HttpMethod method) {
        for (MethodUrlEnum methodUrlEnum : MethodUrlEnum.values()) {
            if (!methodUrlEnum.method.equals((Object)method)) continue;
            return methodUrlEnum.completeMatchUrl;
        }
        return null;
    }

    public static String getRegMatchUrlByMethod(HttpMethod method) {
        for (MethodUrlEnum methodUrlEnum : MethodUrlEnum.values()) {
            if (!methodUrlEnum.method.equals((Object)method)) continue;
            return methodUrlEnum.regMatchUrl;
        }
        return null;
    }

    public static String getUrlMethodByMethod(HttpMethod method) {
        for (MethodUrlEnum methodUrlEnum : MethodUrlEnum.values()) {
            if (!methodUrlEnum.method.equals((Object)method)) continue;
            return methodUrlEnum.urlmethod;
        }
        return null;
    }
}

