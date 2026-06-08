/*
 * Decompiled with CFR 0.152.
 */
package com.supcon.supfusion.framework.cloud.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

public enum BizErrorEnum implements ErrorDefinition
{
    SYSTEM_OK(100000000, "system.ok", "system success"),
    SYSTEM_ERROR(100000001, "system.error", "system error"),
    SYSTEM_BUSY(100000002, "system.busy", "system busy"),
    ARGUMENT_ILLEGAL(100000003, "system.argument_illegal", "illegal argument"),
    SYSTEM_DATA_NULL(100000004, "system.data_null", "return data is null"),
    INVOCATION_TARGET_ERROR(100000005, "system.invocation_target_error", "invocation target"),
    UN_AUTHORIZED(100000006, "system.unauthorized", "unauthorized"),
    DATABASE_ERROR(100000007, "system.database_error", "database operate failed"),
    ORM_UNKNOW_DATABASE(100001000, "system.unknown_database", "UNKNOW_DATABASE"),
    ORM_SQL_ERROR(100001001, "system.sql_error", "SQL_ERROR"),
    ID_NOT_NULL(100001002, "system.id_not_null", "ID_NOT_NULL"),
    ID_MUST_NULL(100001003, "system.id_must_null", "ID_MUST_NULL"),
    OBJECT_NULL(100001004, "system.object_null", "OBJECT_NULL"),
    OBJECT_HAVE_BEAN_DELETED(100001005, "system.object_have_bean_deleted", "OBJECT_HAVE_BEAN_DELETED"),
    CODE_NOT_NULL(100001006, "system.code_not_null", "CODE_NOT_NULL"),
    URL_NOT_NULL(100001007, "system.url_not_null", "URL_NOT_NULL"),
    URL_FORMAT_ERROR(100001008, "system.url_format_error", "URL_FORMAT_ERROR"),
    URL_NOT_FOUND(100001009, "system.url.not.found", "not found: %s"),
    URL_CALL_FAILED(100001010, "system.url.call.failed", "invoker remote service failed: %s");

    private Integer code;
    private String info;
    private String defaultMessage;

    private BizErrorEnum(Integer code, String info, String defaultMessage) {
        this.code = code;
        this.info = info;
        this.defaultMessage = defaultMessage;
    }

    @Override
    public Integer getCode() {
        return this.code;
    }

    @Override
    public String getMessage() {
        return this.defaultMessage;
    }

    @Override
    public String getInfo() {
        return this.info;
    }
}

