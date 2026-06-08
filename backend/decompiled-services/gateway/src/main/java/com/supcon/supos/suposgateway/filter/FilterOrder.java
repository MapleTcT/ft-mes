/*
 * Decompiled with CFR 0.152.
 */
package com.supcon.supos.suposgateway.filter;

public enum FilterOrder {
    INIT(Integer.MIN_VALUE),
    URL_ANALYSIS(-2147483647),
    WS_RESOLVER(-2147483646),
    CACHE_REQUEST_BODY(-2147483645),
    REQUEST_LOG(-2147483644),
    LOGIN_NUM(-2147483643),
    LOGIN(-2147483641),
    REFRESH_TOKEN(-2147483640),
    COMPANY_CHANGE(-2147483639),
    LOGOUT(-2147483638),
    BRANCH_OFFICE(-2147483637),
    IP_VERIFY(-2147483636),
    COOKIE(-2147483635),
    AUTH(-2147483549),
    LICENSE(-2147483548),
    RBAC(-2147483547),
    STORE_IP(-2147483546),
    CACHE_RESPONSE_BODY(-2),
    DYNAMIC_REDIRECT(9998),
    ADD_TENANT_APP_URI_HEADER(9999),
    FORWARD_APP_URL(10101),
    SET_PORT_AFTER_LOAD_BALANCER(10102);

    private int order;

    private FilterOrder(int order) {
        this.order = order;
    }

    public int getOrder() {
        return this.order;
    }
}

