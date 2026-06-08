/*
 * Decompiled with CFR 0.152.
 */
package com.supcon.supfusion.framework.cloud.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.BizException;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

public class BizHttpStatusException
extends BizException {
    private static final long serialVersionUID = -7722460133258596100L;
    private final int httpStatus;

    public BizHttpStatusException(ErrorDefinition definition, int httpStatus) {
        super(definition);
        this.httpStatus = httpStatus;
    }

    public BizHttpStatusException(ErrorDefinition definition, int httpStatus, Object ... args) {
        super(definition, args);
        this.httpStatus = httpStatus;
    }

    public BizHttpStatusException(ErrorDefinition definition, int httpStatus, Throwable throwable) {
        super(definition, throwable);
        this.httpStatus = httpStatus;
    }

    public BizHttpStatusException(ErrorDefinition definition, int httpStatus, Throwable throwable, Object ... args) {
        super(definition, throwable, args);
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() {
        return this.httpStatus;
    }
}

