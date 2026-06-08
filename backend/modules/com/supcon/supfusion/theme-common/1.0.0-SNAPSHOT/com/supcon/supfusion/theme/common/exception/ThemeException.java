package com.supcon.supfusion.theme.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.BizException;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

public class ThemeException extends BizException {

	private static final long serialVersionUID = 8865546233119795186L;

	public ThemeException(ErrorDefinition error) {
        super(error);
    }

    public ThemeException(ErrorDefinition error, Throwable throwable) {
        super(error, throwable);
    }

}
