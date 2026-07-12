package com.mapletct.ftmes.bpi.infrastructure.postgres;

public record IdempotencyRecord(
        String method,
        String resourcePath,
        String requestChecksum,
        String state,
        Integer responseStatus,
        String responseBody) {
}
