package com.supcon.supfusion.i18n.common.until;

import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;

/**
 * ${description}
 *
 * @author
 * @create 2020/6/2 13:43
 */
public class ApiConstants {
    public static final String OPENAPI_PATH = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + ApiConstants.SERVICE_NAME + HttpConstants.URL_SPLITER + "v1";
    public static final String SERVICE_NAME = "i18n";

    private ApiConstants() {
        throw new IllegalStateException("ApiConstants class");
    }
}
