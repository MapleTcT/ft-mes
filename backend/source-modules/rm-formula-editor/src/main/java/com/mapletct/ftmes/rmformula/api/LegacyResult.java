package com.mapletct.ftmes.rmformula.api;

public final class LegacyResult<T> {
    private final int code;
    private final T data;
    private final String message;
    private final String msg;

    private LegacyResult(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
        this.msg = message;
    }

    @SuppressWarnings("unchecked")
    public static <T> LegacyResult<T> success(T data) {
        T normalized = (T) ApiIdentifierNormalizer.normalize(data);
        return new LegacyResult<T>(200, normalized, "success");
    }

    public static <T> LegacyResult<T> failure(int code, String message) {
        return new LegacyResult<T>(code, null, message);
    }

    public int getCode() { return code; }
    public T getData() { return data; }
    public String getMessage() { return message; }
    public String getMsg() { return msg; }
}
