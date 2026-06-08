package com.supcon.supfusion.auth.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.BizException;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

/**
 * 认证客户端异常类
 *
 * @author caokele
 */
public class OAuthClientException extends BizException {
    public OAuthClientException(ErrorDefinition definition) {
        super(definition);
    }
}
