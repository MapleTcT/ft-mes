package com.supcon.supfusion.auditlog.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.BizException;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

/**
 * 审计日志相关异常类
 */
public class AuditLogException extends BizException {

    public AuditLogException(ErrorDefinition definition) {
        super(definition);
    }

    public AuditLogException(ErrorDefinition definition, Object... args) {
        super(definition, args);
    }

    public AuditLogException(ErrorDefinition definition, Throwable throwable) {
        super(definition, throwable);
    }

    public AuditLogException(ErrorDefinition definition, Throwable throwable, Object... args) {
        super(definition, throwable, args);
    }
}
