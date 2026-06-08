/*
 * Decompiled with CFR 0.152.
 */
package com.supcon.supfusion.framework.cloud.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorEntity;

public class SystemException
extends RuntimeException {
    private static final long serialVersionUID = -1591481063433037064L;
    private final ErrorDefinition definition;
    private final ErrorEntity simpleError;

    public SystemException(ErrorDefinition definition) {
        super(definition.getSimpleMessage());
        this.definition = definition;
        this.simpleError = ErrorEntity.builder().setCode(definition.getCode()).setMessage(definition.getMessage()).build();
    }

    public SystemException(final ErrorDefinition definition, final Object ... args) {
        super(definition.getSimpleMessage());
        this.definition = new ErrorDefinition(){

            @Override
            public Integer getCode() {
                return definition.getCode();
            }

            @Override
            public String getMessage() {
                return String.format(definition.getMessage(), args);
            }

            @Override
            public String getInfo() {
                return definition.getInfo();
            }
        };
        this.simpleError = ErrorEntity.builder().setCode(this.definition.getCode()).setMessage(this.definition.getMessage()).build();
    }

    public SystemException(ErrorDefinition definition, Throwable throwable) {
        super(definition.getSimpleMessage(), throwable);
        this.definition = definition;
        this.simpleError = ErrorEntity.builder().setCode(definition.getCode()).setMessage(definition.getMessage()).build();
    }

    public SystemException(final ErrorDefinition definition, Throwable throwable, final Object ... args) {
        super(definition.getSimpleMessage(), throwable);
        this.definition = new ErrorDefinition(){

            @Override
            public Integer getCode() {
                return definition.getCode();
            }

            @Override
            public String getMessage() {
                return String.format(definition.getMessage(), args);
            }

            @Override
            public String getInfo() {
                return definition.getInfo();
            }
        };
        this.simpleError = ErrorEntity.builder().setCode(this.definition.getCode()).setMessage(this.definition.getMessage()).build();
    }

    public ErrorDefinition getErrorDefinition() {
        return this.definition;
    }

    public ErrorEntity getSimpleError() {
        return this.simpleError;
    }
}

