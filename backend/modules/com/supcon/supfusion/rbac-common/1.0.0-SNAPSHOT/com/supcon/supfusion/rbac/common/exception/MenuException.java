package com.supcon.supfusion.rbac.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.BizException;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

public class MenuException extends BizException {

    private static final long serialVersionUID = -7360531282211417172L;

    public MenuException(ErrorDefinition definition) { super(definition); }

    public MenuException(ErrorDefinition error, Throwable throwable) {
        super(error, throwable);
    }
}
