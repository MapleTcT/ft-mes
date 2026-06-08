package com.supcon.supfusion.auth.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

/**
 * 认证客户端相关错误码和错误信息的枚举类
 *
 * @author caokele
 */
public enum OAuthClientErrorEnum implements ErrorDefinition {
    NOT_EXIST(100106401, "认证客户端不存在", "oAuthClient.notExist"),
    NAME_IS_EXIST(100106402, "该名称已存在，无法创建", "oAuthClient.nameIsExist"),
    CLIENT_ID_EXIST(100106402, "该client_id已存在，无法创建", "oAuthClient.clientIdIsExist"),
    OAUTH2_CODE_NOT_EXIST(100106403, "错误的responseType", "userManagement.oauth2CodeIsExist");
    /**
     * 异常码
     */
    private Integer code;
    /**
     * 默认异常信息
     */
    private String defaultMessage;

    private String key;


    OAuthClientErrorEnum(Integer code, String defaultMessage, String key) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.key = key;
    }

    @Override
    public Integer getCode() {

        return code;
    }

    @Override
    public String getMessage() {

        return defaultMessage;
    }

    @Override
    public String getInfo() {
        return key;
    }

}
