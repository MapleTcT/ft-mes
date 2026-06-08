/*
 * Decompiled with CFR 0.152.
 */
package com.supcon.supfusion.framework.cloud.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;
import com.supcon.supfusion.framework.cloud.common.exception.SystemHttpStatusException;

public class System400Exception
extends SystemHttpStatusException {
    public System400Exception(ErrorDefinition definition) {
        super(definition, 400);
    }

    public System400Exception(ErrorDefinition definition, Object ... args) {
        super(definition, 400, args);
    }

    public System400Exception(ErrorDefinition definition, Throwable throwable) {
        super(definition, 400, throwable);
    }

    public System400Exception(ErrorDefinition definition, Throwable throwable, Object ... args) {
        super(definition, 400, throwable, args);
    }
}

