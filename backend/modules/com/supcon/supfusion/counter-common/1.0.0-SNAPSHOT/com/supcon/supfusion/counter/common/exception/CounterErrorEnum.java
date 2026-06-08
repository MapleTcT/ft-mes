package com.supcon.supfusion.counter.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

public enum CounterErrorEnum implements ErrorDefinition {

    // 数据操作错误定义
    RULE_NOT_FOUND(100115001, "counter rule not found", "counter.rule.not.found");

    /**
     * 错误码
     */
    private Integer resultCode;

    /**
     * 错误描述
     */
    private String resultMsg;

    /**
     * 国际化编码
     */
    private String i18nCode;

    CounterErrorEnum(Integer resultCode, String resultMsg, String i18nCode) {
        this.resultCode = resultCode;
        this.resultMsg = resultMsg;
        this.i18nCode = i18nCode;
    }

    @Override
    public Integer getCode() {
        return resultCode;
    }

    @Override
    public String getMessage() {
        return resultMsg;
    }

    @Override
    public String getInfo() {
        return i18nCode;
    }

    @Override
    public String getSimpleMessage() {
        return "[" + getCode() + "]" + getMessage();
    }
}
