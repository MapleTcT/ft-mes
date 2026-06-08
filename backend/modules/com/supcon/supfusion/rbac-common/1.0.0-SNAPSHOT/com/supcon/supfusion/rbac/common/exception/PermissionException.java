package com.supcon.supfusion.rbac.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.BizException;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

public class PermissionException extends BizException {

	private static final long serialVersionUID = 8865546233119795186L;

	public PermissionException(ErrorDefinition error) {
        super(error);
    }

    public PermissionException(ErrorDefinition error, Throwable throwable) {
        super(error, throwable);
    }

}
