package com.supcon.supfusion.iam.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

/**
 * @author tomcat - <huangjianbo@supos.com>
 * @date 20-5-11 下午4:26
 */
public enum IAMErrorEnum implements ErrorDefinition {
    /**
     * 账号已经申请过了
     */
    ACCOUNT_IS_EXISTS(100109001, "iam.account.has.exists", "account has exists"),
    /**
     * 账号不存在
     */
    ACCOUNT_NOT_EXISTS(100109002, "iam.account.not.exists", "account not exists"),
    /**
     * 非法账号
     */
    ACCOUNT_IS_ILLEGAL(100109003, "iam.account.illegal", "illegal account"),
    /**
     * APP_ID已存在
     */
    APPID_IS_EXIST(100109004, "iam.appid_exist", "APP_ID已存在，请重新输入"),
    /**
     * APP_ID已被下载
     */
    APPID_HAS_BEEN_DOWNLOADED(100109005, "iam.appid_has_been_downloaded", "该凭证已被下载，无法再次下载");

    /**
     * 錯誤碼
     */
    private Integer code;
    /**
     * 錯誤信息
     */
    private String message;
    /**
     * 國際化編碼
     */
    private String i18nCode;

    IAMErrorEnum(Integer code, String i18nCode, String message) {
        this.code = code;
        this.i18nCode = i18nCode;
        this.message = message;
    }

    @Override
    public Integer getCode() {
        return this.code;
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    @Override
    public String getInfo() {
        return this.i18nCode;
    }
}
