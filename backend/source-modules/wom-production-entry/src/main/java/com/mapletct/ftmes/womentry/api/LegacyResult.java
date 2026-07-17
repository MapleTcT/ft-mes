package com.mapletct.ftmes.womentry.api;

public class LegacyResult<T> {

    private final int code;
    private final T data;
    private final String message;

    private LegacyResult(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public static <T> LegacyResult<T> success(T data) {
        return new LegacyResult<T>(200, data, "处理成功");
    }

    public static <T> LegacyResult<T> failure(int code, String message) {
        return new LegacyResult<T>(code, null, message);
    }

    public int getCode() {
        return code;
    }

    public T getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }
}
