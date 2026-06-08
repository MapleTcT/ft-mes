/*
 * Decompiled with CFR 0.152.
 */
package com.supcon.supos.suposgateway.enums;

public interface Constants {
    public static final String AUTHENTICATION_URI = "/inter-api/auth/login";
    public static final String CHANGE_COMPANY_URI = "/inter-api/auth/company/change";
    public static final String LOGOUT_URI = "/inter-api/auth/logout";
    public static final String REFRESH_TOKEN_URI = "/inter-api/auth/token/refresh";
    public static final String ACCESS_TOKEN = "access_token";
    public static final String REFRESH_TOKEN = "refresh_token";
    public static final String EXPIRES_IN = "expires_in";
    public static final String REFRESH_EXPIRES_IN = "refresh_expires_in";
    public static final String TOKEN_TYPE = "token_type";
    public static final String AUTH_TICKET = "AUTH:TICKET:%s";
    public static final String LAST_UID_CIDS = "LAST:UID:%d:CIDS";
    public static final String USERNAME = "username";
    public static final String USER_NAME = "userName";
    public static final String PASSWORD = "password";
    public static final String CLIENT_ID = "client_id";
    public static final String TYPE = "type";
    public static final String GRANT_TYPE = "grant_type";
    public static final String SCOPE = "scope";
    public static final String OFFLINE_ACCESS = "offline_access";
    public static final String COMPANY_NAME = "companyName";
    public static final String STATUS = "status";
    public static final String COMPANY_ID = "companyId";
    public static final String COMPANY_CODE = "companyCode";
    public static final String COMPANY = "company";
    public static final String DATA = "data";
    public static final String USER_ID = "userId";
    public static final String USER_TYPE = "userType";
    public static final String TICKET = "ticket";
    public static final String LOGIN_IP = "loginIp";
    public static final String AUTHORIZATION = "Authorization";
    public static final String JWT = "jwt";
    public static final String COOKIE_AUTHORIZATION = "suposTicket";
    public static final String IP = "ip";
    public static final String CLAIM_KEY_USER_ID = "user_id";
    public static final String CLAIM_KEY_USER_NAME = "user_name";
    public static final String CLAIM_KEY_COMPANY_ID = "company_id";
    public static final String SPACE_CHAR = " ";
    public static final String BAP_API_PREFIX = "/msService";
    public static final String FILE_PDF_PREFIX = "/inter-api/file-server/web/";
    public static final String FILE_PDF_BUILD_PREFIX = "/inter-api/file-server/build/";
    public static final String FILE_IMAGE_PREFIX = "/inter-api/file-server/v1/file/pdfStreamHandeler";
    public static final String FILE_IMAGE_OVERVIEW_PREFIX = "/inter-api/file-server/v1/file/auth/overview/image";
    public static final String HTML = "text/html";
    public static final String FORWARD_URL = "/error/401.html";
    public static final String AUTH_IWB_CID_CONTROL_TYPE = "AUTH:IWB:CID:CONTROL-TYPE:TENANTID:%s";
    public static final String AUTH_IWB_CID = "AUTH:IWB:TENANTID:%s:CID:%d";
    public static final Long ecEntityTime = 43200000L;
    public static final Long moduleTime = 21600000L;
    public static final String licenseRedisKey = "LICENSE:INFO";
    public static final String Salt = "licenseSalt";
    public static final String ec_module = "supPlant-Dev";
    public static final String NO_LICENSE_DES = "\u5f53\u524d\u6a21\u5757\u65e0\u8f6f\u4ef6\u72d7\u6388\u6743";
}

