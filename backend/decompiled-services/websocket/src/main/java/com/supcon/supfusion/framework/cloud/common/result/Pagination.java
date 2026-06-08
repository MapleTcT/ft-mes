/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.annotation.JsonPropertyOrder
 */
package com.supcon.supfusion.framework.cloud.common.result;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.io.Serializable;

@JsonPropertyOrder(value={"total", "pageSize", "current"})
public class Pagination
implements Serializable {
    private static final long serialVersionUID = -6986191490122868740L;
    private int total = 0;
    private int pageSize = 0;
    private int current = 1;

    public static PaginationBuilder builder() {
        return new PaginationBuilder();
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public void setCurrent(int current) {
        this.current = current;
    }

    public int getTotal() {
        return this.total;
    }

    public int getPageSize() {
        return this.pageSize;
    }

    public int getCurrent() {
        return this.current;
    }

    public String toString() {
        return "Pagination(total=" + this.getTotal() + ", pageSize=" + this.getPageSize() + ", current=" + this.getCurrent() + ")";
    }

    public Pagination() {
    }

    public Pagination(int total, int pageSize, int current) {
        this.total = total;
        this.pageSize = pageSize;
        this.current = current;
    }

    public static class PaginationBuilder {
        private int total;
        private int pageSize;
        private int current;

        PaginationBuilder() {
        }

        public PaginationBuilder total(int total) {
            this.total = total;
            return this;
        }

        public PaginationBuilder pageSize(int pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        public PaginationBuilder current(int current) {
            this.current = current;
            return this;
        }

        public Pagination build() {
            return new Pagination(this.total, this.pageSize, this.current);
        }

        public String toString() {
            return "Pagination.PaginationBuilder(total=" + this.total + ", pageSize=" + this.pageSize + ", current=" + this.current + ")";
        }
    }
}

