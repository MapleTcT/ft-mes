package com.supcon.supfusion.signature.base.exception;

import com.supcon.supfusion.framework.cloud.common.exception.BizException;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

/**
 * @author zhang yafei
 */
public class SignatureException   extends BizException {
    public SignatureException(ErrorDefinition definition) {
        super(definition);
    }

    public SignatureException(ErrorDefinition definition, Object... args) {
        super(definition, args);
    }

    public SignatureException(ErrorDefinition definition, Throwable throwable) {
        super(definition, throwable);
    }

    public SignatureException(ErrorDefinition definition, Throwable throwable, Object... args) {
        super(definition, throwable, args);
    }
}
