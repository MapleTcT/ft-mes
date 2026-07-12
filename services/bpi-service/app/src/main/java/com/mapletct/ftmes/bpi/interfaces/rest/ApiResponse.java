package com.mapletct.ftmes.bpi.interfaces.rest;

import jakarta.servlet.http.HttpServletRequest;

import java.time.Instant;

public record ApiResponse<T>(T data, ResponseMeta meta) {

    public static <T> ApiResponse<T> of(T data, HttpServletRequest request) {
        String traceId = String.valueOf(request.getAttribute(TraceIdFilter.ATTRIBUTE));
        Instant now = Instant.now();
        return new ApiResponse<>(data, new ResponseMeta(traceId, now, now, null));
    }
}
