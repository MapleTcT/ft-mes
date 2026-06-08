/*
 * Decompiled with CFR 0.152.
 */
package com.supcon.supfusion.framework.cloud.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.BizHttpStatusException;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

public class Biz400Exception
extends BizHttpStatusException {
    public Biz400Exception(ErrorDefinition definition) {
        super(definition, 400);
    }

    public Biz400Exception(ErrorDefinition definition, Object ... args) {
        super(definition, 400, args);
    }

    public Biz400Exception(ErrorDefinition definition, Throwable throwable) {
        super(definition, 400, throwable);
    }

    public Biz400Exception(ErrorDefinition definition, Throwable throwable, Object ... args) {
        super(definition, 400, throwable, args);
    }
}

