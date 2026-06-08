/*
 * Decompiled with CFR 0.152.
 */
package com.supcon.supfusion.framework.cloud.common.exception;

public interface ErrorDefinition {
    public Integer getCode();

    public String getMessage();

    public String getInfo();

    default public String getSimpleMessage() {
        return "[" + this.getCode() + "]" + this.getMessage();
    }
}

