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
import com.supcon.supfusion.framework.cloud.common.exception.BizErrorEnum;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorEntity;

@JsonPropertyOrder(value={"code", "message", "data"})
public class Result<T>
extends ErrorEntity {
    private static final long serialVersionUID = 2368049747710991182L;
    @JsonInclude(value=JsonInclude.Include.NON_NULL)
    private T data;

    public Result() {
    }

    public Result(int code, String message) {
        super(code, message);
    }

    public Result(int code, String message, T data) {
        super(code, message);
        this.data = data;
    }

    public Result(T data) {
        this();
        this.data = data;
    }

    public static <R> ResultBuilder<R> custom() {
        return new ResultBuilder();
    }

    public static <T> Result data(T data) {
        return Result.custom().data(data).build();
    }

    public static <T> Result data(String msg, T data) {
        return Result.custom().data(data).message(msg).build();
    }

    public static <T> Result data(int code, String msg, T data) {
        return Result.custom().data(data).code(code).message(msg).build();
    }

    public static Result success() {
        return Result.custom().code(BizErrorEnum.SYSTEM_OK.getCode()).message(BizErrorEnum.SYSTEM_OK.getMessage()).build();
    }

    public static Result success(String msg) {
        return Result.custom().code(BizErrorEnum.SYSTEM_OK.getCode()).message(msg).build();
    }

    public static Result success(int code, String msg) {
        return Result.custom().code(code).message(msg).build();
    }

    public static Result fail() {
        return Result.custom().code(BizErrorEnum.SYSTEM_ERROR.getCode()).message(BizErrorEnum.SYSTEM_ERROR.getMessage()).build();
    }

    public static Result fail(int code, String msg) {
        return Result.custom().code(code).message(msg).build();
    }

    public static Result fail(String msg) {
        return Result.custom().code(BizErrorEnum.SYSTEM_ERROR.getCode()).message(msg).build();
    }

    public static Result status(boolean flag) {
        return flag ? Result.success() : Result.fail();
    }

    public void setData(T data) {
        this.data = data;
    }

    public T getData() {
        return this.data;
    }

    @Override
    public String toString() {
        return "Result(super=" + super.toString() + ", data=" + this.getData() + ")";
    }

    public static final class ResultBuilder<T> {
        private Integer code;
        private String message;
        private T data;

        public ResultBuilder code(Integer code) {
            this.code = code;
            return this;
        }

        public ResultBuilder message(String message) {
            this.message = message;
            return this;
        }

        public ResultBuilder data(T data) {
            this.data = data;
            return this;
        }

        public Result<T> build() {
            Result<String> result = new Result<String>();
            result.setCode(this.code);
            result.setMessage(this.message);
            result.setData((String)(null != this.data ? this.data : ""));
            return result;
        }
    }
}

