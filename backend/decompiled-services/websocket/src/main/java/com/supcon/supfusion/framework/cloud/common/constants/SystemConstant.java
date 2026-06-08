/*
 * Decompiled with CFR 0.152.
 */
package com.supcon.supfusion.framework.cloud.common.constants;

public interface SystemConstant {
    public static final String DEV_MODE = "dev";
    public static final String PROD_MODE = "prod";
    public static final String TEST_MODE = "test";
    public static final String IDENTITY_ID = "identity:";
    public static final Integer DB_NOT_DELETED = 1;
    public static final Integer DB_DELETED = 0;
    public static final String CONFIGURATION_PROPERTIES_PREFIX = "supfusion.cloud";
    public static final String CONFIGURATION_USE_SYSTEM_DB_KEY = "supfusion.cloud.datasource.connect.use-system";
    public static final String MDC_TRACE_ID = "traceId";
    public static final String MDC_FROM_SERVICE_NAME = "fromServiceName";
    public static final String SYSTEM_TENANT_ID = "system001";
}

