/*
 * Decompiled with CFR 0.152.
 */
package com.supcon.supfusion.framework.cloud.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;
import com.supcon.supfusion.framework.cloud.common.exception.SystemHttpStatusException;

public class System500Exception
extends SystemHttpStatusException {
    public System500Exception(ErrorDefinition definition) {
        super(definition, 500);
    }

    public System500Exception(ErrorDefinition definition, Object ... args) {
        super(definition, 500, args);
    }

    public System500Exception(ErrorDefinition definition, Throwable throwable) {
        super(definition, 500, throwable);
    }

    public System500Exception(ErrorDefinition definition, Throwable throwable, Object ... args) {
        super(definition, 500, throwable, args);
    }
}

