package com.supcon.supfusion.organization.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.BizException;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

/**
 * 岗位相关异常
 */
public class PositionException extends BizException {

    public PositionException(ErrorDefinition definition) {
        super(definition);
    }

    public PositionException(ErrorDefinition definition, Object... args) {
        super(definition, args);
    }

    public PositionException(ErrorDefinition definition, Throwable throwable) {
        super(definition, throwable);
    }

    public PositionException(ErrorDefinition definition, Throwable throwable, Object... args) {
        super(definition, throwable, args);
    }
}
