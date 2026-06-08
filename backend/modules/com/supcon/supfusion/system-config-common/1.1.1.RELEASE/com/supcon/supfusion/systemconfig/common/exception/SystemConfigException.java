package com.supcon.supfusion.systemconfig.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.BizException;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

/**
 * @author lifangyuan
 */
public class SystemConfigException extends BizException {
    public SystemConfigException(ErrorDefinition definition) {
        super(definition);
    }

    public SystemConfigException(ErrorDefinition definition, Throwable throwable) {
        super(definition, throwable);
    }
}
