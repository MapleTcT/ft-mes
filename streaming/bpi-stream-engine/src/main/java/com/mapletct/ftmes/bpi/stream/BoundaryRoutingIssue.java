package com.mapletct.ftmes.bpi.stream;

import java.util.Objects;

public record BoundaryRoutingIssue(
        String code,
        String sourceEventId,
        String propertyId,
        String message) {

    public BoundaryRoutingIssue {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(sourceEventId, "sourceEventId");
        Objects.requireNonNull(propertyId, "propertyId");
        Objects.requireNonNull(message, "message");
    }
}
