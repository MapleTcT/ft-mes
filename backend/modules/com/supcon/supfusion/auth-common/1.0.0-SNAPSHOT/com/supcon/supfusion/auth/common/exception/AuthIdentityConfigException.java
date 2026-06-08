package com.supcon.supfusion.auth.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.BizException;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

public class AuthIdentityConfigException extends BizException {
    public AuthIdentityConfigException(ErrorDefinition definition) {
        super(definition);
    }
}
