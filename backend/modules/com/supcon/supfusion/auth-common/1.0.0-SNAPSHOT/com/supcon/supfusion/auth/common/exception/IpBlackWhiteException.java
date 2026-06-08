package com.supcon.supfusion.auth.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.BizException;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

/**
 * IP黑白名单异常类
 *
 * @author caokele
 */
public class IpBlackWhiteException extends BizException {
    public IpBlackWhiteException(ErrorDefinition definition) {
        super(definition);
    }
}
