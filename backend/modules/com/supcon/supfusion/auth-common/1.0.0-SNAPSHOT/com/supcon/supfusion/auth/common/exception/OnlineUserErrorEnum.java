package com.supcon.supfusion.auth.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

/**
 * 在线用户相关错误码和错误信息的枚举类
 *
 * @author caokele
 */
public enum OnlineUserErrorEnum implements ErrorDefinition {
    LOGOUT(100106501, "您已被注销，请重新登录", "onlineUser.logout");
    /**
     * 异常码
     */
    private Integer code;
    /**
     * 默认异常信息
     */
    private String defaultMessage;

    private String key;


    OnlineUserErrorEnum(Integer code, String defaultMessage, String key) {
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
