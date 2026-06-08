package com.supcon.supfusion.license.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

/**
 * 身份提供者相关错误码和错误信息的枚举类
 *
 * @author caokele
 */
public enum IdentityProviderErrorEnum implements ErrorDefinition {
    IDP_NOT_EXIST(100106301, "身份提供者不存在", "identityProvider.notExist"),
    IDP_NAME_IS_EXIST(100106302, "该名称已存在，无法创建", "identityProvider.nameIsExist"),
    MODULE_NOT_EXIST(100106303, "当前moduleCode查询不到授权信息", "moduleCode.notExist"),
    LICENSE_KEY_NOT_EXIST(100106304, "当前licenseKey查询不到授权信息", "licenseKey.notExist");
    /**
     * 异常码
     */
    private Integer code;
    /**
     * 默认异常信息
     */
    private String defaultMessage;

    private String key;


    IdentityProviderErrorEnum(Integer code, String defaultMessage, String key) {
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
