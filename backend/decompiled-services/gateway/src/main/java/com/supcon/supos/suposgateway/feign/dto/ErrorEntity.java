/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.annotation.JsonInclude
 *  com.fasterxml.jackson.annotation.JsonInclude$Include
 */
package com.supcon.supos.suposgateway.feign.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;

public class ErrorEntity
implements Serializable {
    private static final long serialVersionUID = -6441130176192844775L;
    @JsonInclude(value=JsonInclude.Include.NON_NULL)
    private Integer code;
    @JsonInclude(value=JsonInclude.Include.NON_NULL)
    private String message;

    public Integer getCode() {
        return this.code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

