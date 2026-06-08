package com.supcon.supfusion.auth.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.BizException;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

public class ThirdAuthException extends BizException {

    public ThirdAuthException(ErrorDefinition definition) {
        super(definition);
    }

    public ThirdAuthException(ErrorDefinition definition, Object... args) {
        super(definition, args);
    }
}
