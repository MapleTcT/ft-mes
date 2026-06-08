package com.supcon.supfusion.systemcode.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.BizException;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

public class SystemCodeException extends BizException {

	private static final long serialVersionUID = 8865546233119795186L;

	public SystemCodeException(ErrorDefinition error) {
        super(error);
    }

    public SystemCodeException(ErrorDefinition error, Throwable throwable) {
        super(error, throwable);
    }

}
