package com.supcon.supfusion.auditlog.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

public enum AuditLogErrorEnum implements ErrorDefinition {
    AUDIT_DATA_LOG_NOT_EXISTS(100105000, "审计数据日志不存在!"),
    AUDIT_BUSSINESS_LOG_NOT_EXISTS(100105001, "审计业务数据日志不存在!"),
    AUDIT_BUSSINESS_LOG_FIELD(100105002, "查询审计业务数据日志失败!"),
    AUDIT_DATA_MODEL_LOG_FIELD(100105003, "审计数据模型为空!"),
    AUDIT_LOG_FILE_NOT_FOUND(100105004, "审计日志导入导出文件不存在!"),

    EXCEL_EXPORT_IDS_EMPTY(100106000, "请选择导入的数据条目!"),
    EXCEL_FILE_CREATE_ERROR(100106001, "Excel目录创建失败!")
    ;

    AuditLogErrorEnum(Integer code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    /**
     * 异常码
     */
    private Integer code;
    /**
     * 默认异常信息
     */
    private String defaultMessage;

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
