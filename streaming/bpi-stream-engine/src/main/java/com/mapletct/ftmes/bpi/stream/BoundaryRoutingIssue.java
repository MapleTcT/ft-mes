package com.mapletct.ftmes.bpi.stream;

import java.util.Objects;

public record BoundaryRoutingIssue(
        String code,
        String sourceEventId,
        String propertyId,
        String message,
        String tenantId,
        String plantId,
        String lineId) {

    public BoundaryRoutingIssue(String code, String sourceEventId, String propertyId, String message) {
        this(code, sourceEventId, propertyId, message, "", "", "");
    }

    public BoundaryRoutingIssue {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(sourceEventId, "sourceEventId");
        Objects.requireNonNull(propertyId, "propertyId");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(plantId, "plantId");
        Objects.requireNonNull(lineId, "lineId");
    }
}
