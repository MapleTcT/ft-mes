package com.supcon.supfusion.notification.app.config.constant;

/**
 * @author zhang yafei
 */
public class Constants {

    public final static String HTTP_HEADER_AUTHORIZATION = "Authorization";

    public final static String VENDOR_NAME = "supos";
    public final static String APP_NAME = "supplant";
    public final static String APP_SHOW_NAME = "应用通知";

    public static final String URL_GET_SYS_CONFIG = "/open-api/systemconfig/v1/config/catalog/%s/" + VENDOR_NAME + APP_NAME;
    public static final String URL_GET_PERSON_DETAIL = "/open-api/organization/v1/person/all";
    public static final String URL_POST_NOTICE_STATUS = "/open-api/notification-apiserver/v2/notice/status";
    public static final String URL_POST_SYS_CONFIG = "/open-api/systemconfig/v1/config/catalog";
    public static final String URL_POST_REGISTER_CONFIG = "/open-api/notification-admin/v2/register";


}
