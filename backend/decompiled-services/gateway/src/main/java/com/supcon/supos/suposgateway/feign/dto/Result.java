/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.annotation.JsonInclude
 *  com.fasterxml.jackson.annotation.JsonInclude$Include
 *  com.fasterxml.jackson.annotation.JsonPropertyOrder
 */
package com.supcon.supos.suposgateway.feign.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.supcon.supos.suposgateway.feign.dto.ErrorEntity;

@JsonPropertyOrder(value={"code", "message", "data"})
public class Result<T>
extends ErrorEntity {
    private static final long serialVersionUID = 2368049747710991182L;
    @JsonInclude(value=JsonInclude.Include.NON_NULL)
    private T data;

    public T getData() {
        return this.data;
    }

    public void setData(T data) {
        this.data = data;
    }
}

