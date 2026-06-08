package com.supcon.supfusion.rbac.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

public enum ExcelErrorEnum implements ErrorDefinition {

    EMPTY_DATA(100105150, "EXCEL中无数据！","rbac.EMPTY_DATA"),
    FILE_NOT_EXIST(100105151, "文件已过时！","rbac.FILE_NOT_EXIST");

    /**
     * 异常码
     */
    private Integer code;
    /**
     * 默认异常信息
     */
    private String defaultMessage;

    private String key;

    ExcelErrorEnum(Integer code, String defaultMessage,String key) {
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

    @Override
    public String getSimpleMessage() {
        return defaultMessage;
    }
}
