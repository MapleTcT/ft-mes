package com.supcon.supfusion.auth.common.constants;

/**
 * 常量类
 */

public interface Constants {
    //------------------------表名------------------------
    String AUTH_USER = "auth_user";
    String AUTH_ROLE_NAME = "auth_user_role";
    String AUTH_USER_DIRECTORY = "auth_user_directory";
    String AUTH_ONLINE_USER = "auth_online_user";
    String AUTH_PASSWORD_RULES = "auth_passwd_rules";
    String AUTH_IP_BLACK_WHITE = "auth_ip_black_white";
    String AUTH_IDENTITY_PROVIDER = "auth_identity_provider";
    String AUTH_CENTER = "auth_center";
    String AUTH_OAUTH_CLIENT = "auth_oauth_client";
    String AUTH_PASSWD_RULES = "auth_passwd_rules";
    String AUTH_LOGIN_LOG = "auth_login_log";

    //------------------------黑白名单------------------------
    Integer BLACK_CONTROL_TYPE = 0;
    Integer WHITE_CONTROL_TYPE = 1;
    /**
     * 企业ID与ip列表对应 - SET
     * key: AUTH:IWB:CID:{企业ID}
     * value: ip列表
     */
    String AUTH_IWB_CID = "AUTH:IWB:TENANTID:%s:CID:%d";

    /**
     * 企业ID与管控模式 - HASH
     * key: AUTH:IWB:CID:CONTROL-TYPE
     * value: 0:黑名单,1:白名单
     */
    String AUTH_IWB_CID_CONTROL_TYPE = "AUTH:IWB:CID:CONTROL-TYPE:TENANTID:%s";

    String AUTH_TICKET = "AUTH:TICKET:%s";
    String TENANT_TICKET = "TENANT:TICKET:%s";
    String LAST_UID_CIDS = "%s:LAST:UID:%d:CIDS";
    String ACCESS_TOKEN = "access_token";
    String REFRESH_TOKEN = "refresh_token";
    String TOKEN_TYPE = "token_type";
    String TENANT_ID = "tenantId";
    String EXPIRES_IN = "expires_in";
    String REFRESH_EXPIRES_IN = "refresh_expires_in";
    String USERNAME = "username";
    String USER_NAME = "userName";
    String PASSWORD = "password";
    String CLIENT_ID = "client_id";
    String TYPE = "type";
    String GRANT_TYPE = "grant_type";
    String COMPANY_NAME = "companyName";
    String COMPANY_ID = "companyId";
    String COMPANY_CODE = "companyCode";
    String COMPANY = "company";
    String DATA = "data";
    String USER_ID = "userId";
    String TICKET = "ticket";
    String LOGIN_IP = "loginIp";
    String AUTHORIZATION = "Authorization";
    String IP = "ip";
    String CODE = "code";
    String MESSAGE = "message";
    String DEFAULT_USER = "Supos1304";
    // TODO: 先默认时区，后面从配置项里拿
    String DEFAULT_TIME_ZONE = "CST+08:00";
    String EXCEL_IMPORT = "import";
    /**
     * ticket和code的对应关系
     */
    String BRANCH_AUTH_TICKET_CODE = "BRANCH:AUTH:TICKET:%s:CODE";
    /**
     * ticket和code列表的对应关系
     */
    String HEAD_AUTH_TICKET_CODE_SET = "HEAD:AUTH:TICKET:%s:CODES";

    String MKEY = "mkey";
    String CONFIG_INFO = "configInfo";
    String EXCEL_EXPORT = "export";
    Integer ROLE_USER = 1;
    Integer ROLE_ORG = 2;
    Integer ROLE_REPEATE = 3;

    Integer COMMON_TYPE = 0;

    String BACKEND_LOGOUT_URL = "backendLogoutUrl";

    String OAUTH2_TOKEN = "accessToken";

    Integer ADMIN_TYPE = 1;

    String FEATURE = "auth";

    String TOTAL_ONLINE = "totalOnline";

    String PC = "pc";

    String MOBILE = "mobile";

    String MAX_PC_LOGIN = "MAX_PC_LOGIN";

    String MAX_MOBILE_LOGI = "MAX_MOBILE_LOGIN";

    String CLAIM_KEY_USER_NAME = "user_name";

    String SPACE_CHAR = " ";

    String EMAIL_MSG = "亲爱的用户:\n" + "您好!感谢您使用supOS找回密码服务,您正在进行验证邮箱,本次请求的验证码为%s (为了保障您帐号的安全性,请在15分钟内完成验证).";

    String EMAIL_TITLE = "supOS找回密码";

    Integer COMBINATION_PWD_RULE = 0;

    Integer CUSTOM_PWD_RULE = 1;

    String RULE_ERROR = "密码规则:包含%s的%d-%d位";

    String LOGIN_TYPE = "loginType";

    String PROTOCOL_TYPE = "protocolType";

    String THIRD_TOKEN = "clientAccessToken";

    String THIRD_REFRESH_TOKEN = "clientRefreshToken";

    String ACTIVE_LOGOUT = "0";

    String TIME_OUT_LOGOUT = "1";

    String FORCE_LOGOUT = "2";

    String AUTH_ORIGIN_URL = "AUTH:ORIGIN_URL:%s";

    String SIMULATED_CLIENT_ID = "pc_dt";

    String KC_RESTART = "KC_RESTART";

    String HEAD_BRANCH_OFFICE_CODE = "headBranchOfficeCode";
}
