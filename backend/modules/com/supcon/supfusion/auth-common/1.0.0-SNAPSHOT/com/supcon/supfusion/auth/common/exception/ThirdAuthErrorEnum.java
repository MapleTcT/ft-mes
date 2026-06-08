package com.supcon.supfusion.auth.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

public enum ThirdAuthErrorEnum implements ErrorDefinition {

    GET_TOKEN_BY_ZHUYU_FAILED(100106701, "调用接口获取token失败!","thirdAuth.getTokenFailed"),
    GET_USER_INFO_BY_ZHUYU_FAILED(100106702, "调用接口获取用户信息失败!","thirdAuth.getUserInfoFailed"),
    GET_USER_INFO_IS_NOT_EXISTED(100106703, "用户不存在,认证失败!","thirdAuth.getUserNotExisted"),
    GET_IDENTITY_CONFIG_INFO_FAILED(100106704, "获取第三方配置信息失败!","thirdAuth.getIdentityConfigInfoFailed"),
    REFRESH_TOKEN_BY_ZHUYU_FAILED(100106705, "调用接口刷新token失败!","thirdAuth.refreshTokenFailed"),



    GET_TOKEN_BY_JINDIEYUN_FAILED(100106701, "调用金蝶云接口获取token失败!","thirdAuth.getTokenFailed"),
    GET_USER_INFO_BY_JINDIEYUN_FAILED(100106702, "调用金蝶云接口获取用户信息失败!","thirdAuth.getUserInfoFailed"),
    USER_ALREADY_BIND(100106706, "用户已绑定!","thirdAuth.user_binded_error" ),

    GET_TOKEN_BY_BLUETROON_FAILED(100106701, "调用蓝卓云接口获取token失败!","thirdAuth.getTokenFailed");


    /**
     * 异常码
     */
    private Integer code;

    /**
     * 默认异常信息
     */
    private String defaultMessage;

    private String key;


    ThirdAuthErrorEnum(Integer code, String defaultMessage, String key) {
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
