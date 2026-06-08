package com.supcon.supfusion.rbac.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.BizException;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

public class RoleUserException extends BizException {

	private static final long serialVersionUID = 8865546233119795186L;

	public RoleUserException(ErrorDefinition error) {
        super(error);
    }

    public RoleUserException(ErrorDefinition error, Throwable throwable) {
        super(error, throwable);
    }

}
