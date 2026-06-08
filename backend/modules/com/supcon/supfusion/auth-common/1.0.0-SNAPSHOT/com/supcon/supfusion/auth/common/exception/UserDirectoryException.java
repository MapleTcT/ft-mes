package com.supcon.supfusion.auth.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.BizException;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

/**
 * 用户目录异常类
 *
 * @author caokele
 */
public class UserDirectoryException extends BizException {
    public UserDirectoryException(ErrorDefinition definition) {
        super(definition);
    }
}
