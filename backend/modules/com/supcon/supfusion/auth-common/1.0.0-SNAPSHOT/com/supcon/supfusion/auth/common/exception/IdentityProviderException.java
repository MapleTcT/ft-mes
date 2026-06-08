package com.supcon.supfusion.auth.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.BizException;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

/**
 * 身份提供者异常类
 *
 * @author caokele
 */
public class IdentityProviderException extends BizException {
    public IdentityProviderException(ErrorDefinition definition) {
        super(definition);
    }
}
