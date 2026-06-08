package com.supcon.supfusion.organization.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.BizException;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

/**
 * 组织架构异常类
 */
public class DepartmentException extends BizException {

    public DepartmentException(ErrorDefinition definition) {
        super(definition);
    }

    public DepartmentException(ErrorDefinition definition, Object... args) {
        super(definition, args);
    }

    public DepartmentException(ErrorDefinition definition, Throwable throwable) {
        super(definition, throwable);
    }

    public DepartmentException(ErrorDefinition definition, Throwable throwable, Object... args) {
        super(definition, throwable, args);
    }
}
