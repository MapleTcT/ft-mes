package com.supcon.supfusion.portal.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.BizException;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

/**
 * @Author kk.C
 * @Description 门户模块自定义异常
 * @Date 2020/10/23 14:26
 * @Param
 * @return
 **/
public class PortalException extends BizException {

    public PortalException(ErrorDefinition definition) {
        super(definition);
    }

    public PortalException(ErrorDefinition definition, Throwable throwable) {
        super(definition, throwable);
    }

    public PortalException(ErrorDefinition definition, Throwable throwable, Object... args) {
        super(definition, throwable, args);
    }

    public PortalException(ErrorDefinition definition, Object... args) {
        super(definition, args);
    }
}
