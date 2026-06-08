package com.supcon.supfusion.auth.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

public enum AuthIdentityConfigErrorEnum implements ErrorDefinition {

    OAUTH_NAME_CONFIG_EXIST(1001060023, "应用名称已存在", "identityProviders.authNameExist"),
    UNSUPPORT_INTERNEL_OAUTH_NAME(100106024, "不支持的内置客户端类型", "identityProviders.unsupportInternelName"),
    UNSUPPORT_OPENAPI_EXTERNAL(100106025, "openapi不支持添加外置oauth", "identityProviders.openapiUnsupportAddExternelProtocol")
    ;


    /**
     * 异常码
     */
    private Integer code;

    /**
     * 默认异常信息
     */
    private String defaultMessage;

    private String key;


    AuthIdentityConfigErrorEnum(Integer code, String defaultMessage, String key) {
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
