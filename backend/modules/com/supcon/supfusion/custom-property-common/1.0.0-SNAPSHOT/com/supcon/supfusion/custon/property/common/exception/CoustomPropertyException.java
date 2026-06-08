package com.supcon.supfusion.custon.property.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.BizException;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

/**
 * @author zhang yafei
 */
public class CoustomPropertyException  extends BizException {
    public CoustomPropertyException(ErrorDefinition definition) {
        super(definition);
    }

    public CoustomPropertyException(ErrorDefinition definition, Object... args) {
        super(definition, args);
    }

    public CoustomPropertyException(ErrorDefinition definition, Throwable throwable) {
        super(definition, throwable);
    }

    public CoustomPropertyException(ErrorDefinition definition, Throwable throwable, Object... args) {
        super(definition, throwable, args);
    }
}
