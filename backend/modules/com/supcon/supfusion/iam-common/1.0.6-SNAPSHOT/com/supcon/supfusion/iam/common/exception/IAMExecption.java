package com.supcon.supfusion.iam.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.BizException;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

public class IAMExecption extends BizException {
    public IAMExecption(ErrorDefinition definition) {
        super(definition);
    }

    public IAMExecption(ErrorDefinition errorDefinition, Throwable throwable) {
        super(errorDefinition, throwable);
    }

    public IAMExecption(ErrorDefinition errorDefinition, final Object... args) {
        super(errorDefinition, args);
    }
}
