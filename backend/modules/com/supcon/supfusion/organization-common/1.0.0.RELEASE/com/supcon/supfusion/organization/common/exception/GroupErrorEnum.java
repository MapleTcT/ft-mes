package com.supcon.supfusion.organization.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

public enum GroupErrorEnum implements ErrorDefinition {
    GROUP_THIS_CODE_EXISTS(100104022, "该编码的组已经存在!"),
    GROUP_ID_NOT_EXISTS(100104023, "指定组不存在！");

    /**
     * 异常码
     */
    private Integer code;
    /**
     * 默认异常信息
     */
    private String defaultMessage;

    GroupErrorEnum(Integer code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
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
        return null;
    }
}
