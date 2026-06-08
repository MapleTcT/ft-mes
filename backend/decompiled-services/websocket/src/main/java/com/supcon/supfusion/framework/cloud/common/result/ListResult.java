/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.annotation.JsonInclude
 *  com.fasterxml.jackson.annotation.JsonInclude$Include
 *  com.fasterxml.jackson.annotation.JsonPropertyOrder
 */
package com.supcon.supfusion.framework.cloud.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorEntity;
import java.util.Collection;

@JsonPropertyOrder(value={"code", "message", "list"})
public class ListResult<T>
extends ErrorEntity {
    private static final long serialVersionUID = 9192615619472236578L;
    @JsonInclude(value=JsonInclude.Include.NON_NULL)
    private Collection<T> list;

    public ListResult() {
    }

    public ListResult(int code, String message) {
        super(code, message);
    }

    public ListResult(Collection<T> list) {
        this();
        this.list = list;
    }

    public void setList(Collection<T> list) {
        this.list = list;
    }

    public Collection<T> getList() {
        return this.list;
    }

    @Override
    public String toString() {
        return "ListResult(super=" + super.toString() + ", list=" + this.getList() + ")";
    }
}

