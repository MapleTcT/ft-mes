package com.supcon.supfusion.auth.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.BizException;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

/**
 * 组织架构异常类
 *
 * @author lifangyuan
 */
public class UserException extends BizException {
    public UserException(ErrorDefinition definition) {
        super(definition);
    }

    public UserException(ErrorDefinition definition, Object... args) {
        super(definition, args);
    }
}
