package com.mapletct.ftmes.bpi.stream;

import java.util.Objects;

public record BoundaryExecutionContext(
        String tenantId,
        String plantId,
        String lineId,
        String localityGroup,
        String topologyCode,
        String topologyVersion,
        String contextOrderId,
        String batchId) {

    public BoundaryExecutionContext {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(plantId, "plantId");
        Objects.requireNonNull(lineId, "lineId");
        Objects.requireNonNull(localityGroup, "localityGroup");
        Objects.requireNonNull(topologyCode, "topologyCode");
        Objects.requireNonNull(topologyVersion, "topologyVersion");
        for (String value : new String[]{tenantId, plantId, lineId, localityGroup, topologyCode, topologyVersion}) {
            if (value.isBlank() || value.indexOf('|') >= 0) {
                throw new IllegalArgumentException("execution context identifiers must be nonblank and cannot contain '|'");
            }
        }
    }
}
