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
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.Pagination;
import java.util.Collection;

@JsonPropertyOrder(value={"code", "message", "pagination", "list"})
public class PageResult<T>
extends ListResult<T> {
    private static final long serialVersionUID = -1474022793316774669L;
    @JsonInclude(value=JsonInclude.Include.NON_NULL)
    private Pagination pagination;

    public PageResult() {
    }

    public PageResult(int code, String message) {
        super(code, message);
    }

    public PageResult(Collection<T> list, int total, int pageSize, int pageNo) {
        super(list);
        this.pagination = new Pagination(total, pageSize, pageNo);
    }

    public PageResult(Collection<T> list, long total, long pageSize, long pageNo) {
        super(list);
        this.pagination = new Pagination(Integer.valueOf(Long.toString(total)), Integer.valueOf(Long.toString(pageSize)), Integer.valueOf(Long.toString(pageNo)));
    }

    public void setPagination(Pagination pagination) {
        this.pagination = pagination;
    }

    public Pagination getPagination() {
        return this.pagination;
    }

    @Override
    public String toString() {
        return "PageResult(super=" + super.toString() + ", pagination=" + this.getPagination() + ")";
    }
}

