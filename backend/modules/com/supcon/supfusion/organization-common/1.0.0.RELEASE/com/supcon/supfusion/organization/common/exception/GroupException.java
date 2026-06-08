package com.supcon.supfusion.organization.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.BizException;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

/**
 * @author lifangyuan
 */
public class GroupException extends BizException {
    public GroupException(ErrorDefinition definition) {
        super(definition);
    }

    public GroupException(ErrorDefinition definition, Object... args) {
        super(definition, args);
    }

    public GroupException(ErrorDefinition definition, Throwable throwable) {
        super(definition, throwable);
    }

    public GroupException(ErrorDefinition definition, Throwable throwable, Object... args) {
        super(definition, throwable, args);
    }
}
