/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.annotation.JsonInclude
 *  com.fasterxml.jackson.annotation.JsonInclude$Include
 */
package com.supcon.supfusion.framework.cloud.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supcon.supfusion.framework.cloud.common.pojo.POJO;

public class ErrorEntity
implements POJO {
    private static final long serialVersionUID = -6441130176192844775L;
    @JsonInclude(value=JsonInclude.Include.NON_NULL)
    private Integer code;
    @JsonInclude(value=JsonInclude.Include.NON_NULL)
    private String message;

    public static ErrorEntityBuilder builder() {
        return new ErrorEntityBuilder();
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getCode() {
        return this.code;
    }

    public String getMessage() {
        return this.message;
    }

    public String toString() {
        return "ErrorEntity(code=" + this.getCode() + ", message=" + this.getMessage() + ")";
    }

    public ErrorEntity() {
    }

    public ErrorEntity(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public static class ErrorEntityBuilder {
        private Integer code;
        private String message;

        public ErrorEntityBuilder setCode(Integer code) {
            this.code = code;
            return this;
        }

        public ErrorEntityBuilder setMessage(String message) {
            this.message = message;
            return this;
        }

        public ErrorEntity build() {
            return new ErrorEntity(this.code, this.message);
        }
    }
}

