package com.supcon.supfusion.authkeycloak.constant;

import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.events.Errors;

/**
 * keycloak相关错误码和错误信息的枚举类
 *
 * @author caokele
 */
public enum KeyCloakErrorEnum {
    USER_NOT_FOUND(100106001, "用户不存在", Errors.USER_NOT_FOUND, AuthenticationFlowError.UNKNOWN_USER),
    USER_NOT_IN_COMPANY(100106088, "用户不在该公司", "user not in company", AuthenticationFlowError.UNKNOWN_USER),
    USER_HAS_LOCK(100106008, "用户被锁定", "user_have_lock", AuthenticationFlowError.USER_DISABLED),
    INVALID_USER_CREDENTIALS(100106006, "密码错误", Errors.INVALID_USER_CREDENTIALS, AuthenticationFlowError.INVALID_CREDENTIALS),
    GET_IP_FAILED(100106208, "获取ip失败", "get_ip_failed", AuthenticationFlowError.INTERNAL_ERROR),
    ACCESS_IP_FORBIDDEN(100106207, "ip已被限制拒绝访问", "access_ip_forbidden", AuthenticationFlowError.USER_DISABLED),
    GET_USERNAME_FAILED(100106209, "获取用户名失败", "get_username_failed", AuthenticationFlowError.INTERNAL_ERROR),
    GET_COMPANY_ID_FAILED(100106210, "获取公司ID失败", "get_companyId_failed", AuthenticationFlowError.INTERNAL_ERROR),
    USER_OR_PASSWORD_ERROR(100106211, "用户名或密码错误", Errors.INVALID_USER_CREDENTIALS, AuthenticationFlowError.INVALID_CREDENTIALS);
    private Integer code;
    private String message;
    private String error;
    private AuthenticationFlowError flowError;

    KeyCloakErrorEnum(Integer code, String message, String error, AuthenticationFlowError flowError) {
        this.code = code;
        this.message = message;
        this.error = error;
        this.flowError = flowError;
    }

    public Integer getCode() {
        return code;
    }


    public String getMessage() {
        return message;
    }

    public String getError() {
        return error;
    }

    public AuthenticationFlowError getFlowError() {
        return flowError;
    }

}
