package com.supcon.supfusion.portal.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

public enum PortalErrorEnum implements ErrorDefinition {

    // 查不到门户编码错误定义
    CODE_NOT_FOUND(100119000, "查询不到门户编码！", "portal.CODE.NOT.FOUND"),
    // 重复门户编码错误定义
    CODE_FOUND_REPEAT(100119001, "门户编码重复！", "portal.CODE.FOUND.REPEAT"),
    // 添加国际化错误
    ADD_I18N_ERROR(100119002, "添加国际化信息错误！", "portal.ADD.I18N.ERROR");

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

    PortalErrorEnum(Integer resultCode, String resultMsg, String i18nCode) {
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
