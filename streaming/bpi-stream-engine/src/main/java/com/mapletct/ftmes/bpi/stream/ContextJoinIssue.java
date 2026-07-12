package com.mapletct.ftmes.bpi.stream;

import java.util.Objects;

public record ContextJoinIssue(
        String code,
        String scopeKey,
        String sourceEventId,
        String propertyId,
        long eventTimeMs,
        String message) {

    public ContextJoinIssue {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(scopeKey, "scopeKey");
        Objects.requireNonNull(sourceEventId, "sourceEventId");
        Objects.requireNonNull(propertyId, "propertyId");
        Objects.requireNonNull(message, "message");
    }
}
