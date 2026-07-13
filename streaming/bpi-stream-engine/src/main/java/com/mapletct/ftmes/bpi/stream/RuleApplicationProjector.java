package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleApplicationStatusV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleApplicationV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRulePublicationV1;

final class RuleApplicationProjector {

    private RuleApplicationProjector() {
    }

    static byte[] project(
            BoundaryRulePublicationV1 publication,
            String deploymentId,
            BoundaryRuleApplicationStatusV1 status,
            String errorCode,
            String detail,
            long observedAtMs) {
        if (publication == null) {
            throw new IllegalArgumentException("publication is required");
        }
        if (deploymentId == null || deploymentId.isBlank()) {
            throw new IllegalArgumentException("deploymentId is required");
        }
        if (status == null
                || status == BoundaryRuleApplicationStatusV1.BOUNDARY_RULE_APPLICATION_STATUS_UNSPECIFIED
                || status == BoundaryRuleApplicationStatusV1.UNRECOGNIZED) {
            throw new IllegalArgumentException("application status is required");
        }
        if (observedAtMs <= 0) {
            throw new IllegalArgumentException("observedAtMs must be positive");
        }
        String safeErrorCode = errorCode == null ? "" : errorCode;
        String safeDetail = detail == null ? "" : detail;
        String eventId = String.join(
                "|", publication.getEventId(), deploymentId, status.name(), safeErrorCode,
                Long.toString(observedAtMs));
        return BoundaryRuleApplicationV1.newBuilder()
                .setEventId(eventId)
                .setPublicationEventId(publication.getEventId())
                .setTenantId(publication.getTenantId())
                .setPlantId(publication.getPlantId())
                .setLineId(publication.getLineId())
                .setRuleCode(publication.getRuleCode())
                .setRuleVersion(publication.getRuleVersion())
                .setChecksum(publication.getChecksum())
                .setDeploymentId(deploymentId)
                .setStatus(status)
                .setErrorCode(safeErrorCode)
                .setDetail(safeDetail)
                .setObservedAtMs(observedAtMs)
                .putHeaders("schema_version", "1")
                .putHeaders("event_type", "BOUNDARY_RULE_APPLICATION")
                .putHeaders("trace_id", publication.getHeadersOrDefault("trace_id", ""))
                .build()
                .toByteArray();
    }
}
