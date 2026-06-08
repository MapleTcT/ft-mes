/*
 * Decompiled with CFR 0.152.
 */
package com.supcon.supfusion.framework.cloud.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorEntity;

public class BizException
extends RuntimeException {
    private static final long serialVersionUID = -7240083594328015317L;
    private final ErrorDefinition definition;
    private final ErrorEntity simpleError;

    public BizException(ErrorDefinition definition) {
        super(definition.getSimpleMessage());
        this.definition = definition;
        this.simpleError = ErrorEntity.builder().setCode(definition.getCode()).setMessage(definition.getMessage()).build();
    }

    public BizException(final ErrorDefinition definition, final Object ... args) {
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

    public BizException(ErrorDefinition definition, Throwable throwable) {
        super(definition.getSimpleMessage(), throwable);
        this.definition = definition;
        this.simpleError = ErrorEntity.builder().setCode(definition.getCode()).setMessage(definition.getMessage()).build();
    }

    public BizException(final ErrorDefinition definition, Throwable throwable, final Object ... args) {
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

