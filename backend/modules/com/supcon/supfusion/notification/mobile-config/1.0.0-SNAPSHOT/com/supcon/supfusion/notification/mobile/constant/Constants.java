package com.supcon.supfusion.notification.mobile.constant;

/**
 * @author zhang yafei
 */
public class Constants {

    public final static String HTTP_HEADER_AUTHORIZATION = "Authorization";

    public final static String VENDOR_NAME = "supos";
    public final static String APP_NAME = "mobile";
    public final static String APP_SHOW_NAME = "移动端通知";

    public static final String URL_GET_SYS_CONFIG = "/open-api/systemconfig/v1/config/catalog/%s/" + VENDOR_NAME + APP_NAME;
    public static final String URL_GET_PERSON_DETAIL = "/open-api/organization/v1/person/all";
    public static final String URL_POST_NOTICE_STATUS = "/open-api/notification-apiserver/v2/notice/status";
    public static final String URL_POST_SYS_CONFIG = "/open-api/systemconfig/v1/config/catalog";
    public static final String URL_POST_REGISTER_CONFIG = "/open-api/notification-admin/v2/register";

    public static final String URL_GET_USERID_BYPHONE = "https://oapi.dingtalk.com/user/get_by_mobile";
    public static final String URL_POST_USERID_MESSAGE = "https://oapi.dingtalk.com/topapi/message/corpconversation/getsendresult";
    public final static String TOKEN_URL = "https://oapi.dingtalk.com/gettoken?appkey=%s&appsecret=%s";
    public final static String MESSAGE_URL = "https://oapi.dingtalk.com/topapi/message/corpconversation/asyncsend_v2?access_token=%s";

    public final static long DEFAULT_PERIOD = 30;

}
