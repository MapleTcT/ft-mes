/*
 * Decompiled with CFR 0.152.
 */
package com.supcon.supfusion.framework.cloud.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;
import com.supcon.supfusion.framework.cloud.common.exception.SystemException;

public class SystemHttpStatusException
extends SystemException {
    private static final long serialVersionUID = 8907690234815966707L;
    private final int httpStatus;

    public SystemHttpStatusException(ErrorDefinition definition, int httpStatus) {
        super(definition);
        this.httpStatus = httpStatus;
    }

    public SystemHttpStatusException(ErrorDefinition definition, int httpStatus, Object ... args) {
        super(definition, args);
        this.httpStatus = httpStatus;
    }

    public SystemHttpStatusException(ErrorDefinition definition, int httpStatus, Throwable throwable) {
        super(definition, throwable);
        this.httpStatus = httpStatus;
    }

    public SystemHttpStatusException(ErrorDefinition definition, int httpStatus, Throwable throwable, Object ... args) {
        super(definition, throwable, args);
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() {
        return this.httpStatus;
    }
}

