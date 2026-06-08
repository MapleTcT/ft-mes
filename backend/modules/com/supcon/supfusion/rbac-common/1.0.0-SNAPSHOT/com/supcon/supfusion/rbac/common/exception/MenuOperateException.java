package com.supcon.supfusion.rbac.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.BizException;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

public class MenuOperateException extends BizException {

    private static final long serialVersionUID = 5432584922566093645L;

    public MenuOperateException(ErrorDefinition definition) { super(definition); }

    public MenuOperateException(ErrorDefinition error, Throwable throwable) {
        super(error, throwable);
    }
}
