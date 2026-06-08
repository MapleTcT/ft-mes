/*
 * Decompiled with CFR 0.152.
 */
package com.supcon.supfusion.framework.cloud.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.BizHttpStatusException;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

public class Biz500Exception
extends BizHttpStatusException {
    public Biz500Exception(ErrorDefinition definition) {
        super(definition, 500);
    }

    public Biz500Exception(ErrorDefinition definition, Object ... args) {
        super(definition, 500, args);
    }

    public Biz500Exception(ErrorDefinition definition, Throwable throwable) {
        super(definition, 500, throwable);
    }

    public Biz500Exception(ErrorDefinition definition, Throwable throwable, Object ... args) {
        super(definition, 500, throwable, args);
    }
}

