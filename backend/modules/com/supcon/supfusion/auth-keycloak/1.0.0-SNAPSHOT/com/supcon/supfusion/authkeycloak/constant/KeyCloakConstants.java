package com.supcon.supfusion.authkeycloak.constant;

/**
 * @author caokele
 */
public interface KeyCloakConstants {
    String TYPE = "type";
    String USERNAME = "username";
    String ATTEMPTED_USERNAME = "ATTEMPTED_USERNAME";
    String PASSWORD = "password";
    String COMPANY_CODE = "companyCode";
    String X_REAL_IP = "X-Real-IP";
    String X_TENANT_IP = "X-Tenant-Id";
    String AUTH = "auth";
    String VERIFY_IP_URI = "/service-api/auth/v1/ip-black-white/verify";
    String USER_INFO_URI = "/service-api/auth/v1/user";
    String AD_AUTHENTICATE_URI = "/service-api/auth/v1/user-directories/authenticate";
}
