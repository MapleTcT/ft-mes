package com.mapletct.ftmes.bpi.stream;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRulePublicationV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleRuntimeReadinessStatusV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleRuntimeReadinessV1;
import com.mapletct.ftmes.bpi.contract.v1.PointCatalogSnapshotV1;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

final class RuleRuntimeReadinessProjector {

    private RuleRuntimeReadinessProjector() {
    }

    static byte[] project(
            BoundaryRulePublicationV1 publication,
            String deploymentId,
            BoundaryRuleRuntimeReadinessStatusV1 status,
            String reasonCode,
            String detail,
            long observedAtMs,
            PointCatalogSnapshotV1 catalog) {
        if (publication == null) {
            throw new IllegalArgumentException("publication is required");
        }
        if (deploymentId == null || deploymentId.isBlank()) {
            throw new IllegalArgumentException("deploymentId is required");
        }
        if (status == null
                || status == BoundaryRuleRuntimeReadinessStatusV1
                        .BOUNDARY_RULE_RUNTIME_READINESS_STATUS_UNSPECIFIED
                || status == BoundaryRuleRuntimeReadinessStatusV1.UNRECOGNIZED) {
            throw new IllegalArgumentException("runtime readiness status is required");
        }
        if (observedAtMs <= 0) {
            throw new IllegalArgumentException("observedAtMs must be positive");
        }
        String safeReason = reasonCode == null ? "" : reasonCode;
        String safeDetail = detail == null ? "" : detail;
        if (status == BoundaryRuleRuntimeReadinessStatusV1.READY) {
            safeReason = "";
            safeDetail = "";
        } else if (safeReason.isBlank() || safeDetail.isBlank()) {
            throw new IllegalArgumentException("non-ready runtime status requires a reason and detail");
        }
        String catalogEventId = catalog == null ? "" : catalog.getEventId();
        String catalogRevision = catalog == null ? "" : catalog.getSourceRevision();
        String identity = String.join(
                "|",
                publication.getEventId(),
                deploymentId,
                status.name(),
                safeReason,
                safeDetail,
                Long.toString(observedAtMs),
                catalogEventId,
                catalogRevision);
        return BoundaryRuleRuntimeReadinessV1.newBuilder()
                .setEventId("sha256:" + sha256(identity))
                .setPublicationEventId(publication.getEventId())
                .setTenantId(publication.getTenantId())
                .setPlantId(publication.getPlantId())
                .setLineId(publication.getLineId())
                .setRuleCode(publication.getRuleCode())
                .setRuleVersion(publication.getRuleVersion())
                .setChecksum(publication.getChecksum())
                .setDeploymentId(deploymentId)
                .setStatus(status)
                .setReasonCode(safeReason)
                .setDetail(safeDetail)
                .setObservedAtMs(observedAtMs)
                .setPointCatalogEventId(catalogEventId)
                .setPointCatalogSourceRevision(catalogRevision)
                .putHeaders("schema_version", "1")
                .putHeaders("event_type", "BOUNDARY_RULE_RUNTIME_READINESS")
                .putHeaders("trace_id", publication.getHeadersOrDefault("trace_id", ""))
                .build()
                .toByteArray();
    }

    static boolean ready(byte[] state) {
        if (state == null) {
            return false;
        }
        if (state.length == 1) {
            return state[0] == 1;
        }
        try {
            return BoundaryRuleRuntimeReadinessV1.parseFrom(state).getStatus()
                    == BoundaryRuleRuntimeReadinessStatusV1.READY;
        } catch (InvalidProtocolBufferException error) {
            throw new IllegalStateException("runtime readiness state is not valid Protobuf", error);
        }
    }

    static boolean sameObservation(byte[] previousState, byte[] nextState) {
        if (previousState == null || previousState.length == 1) {
            return false;
        }
        try {
            BoundaryRuleRuntimeReadinessV1 previous =
                    BoundaryRuleRuntimeReadinessV1.parseFrom(previousState);
            BoundaryRuleRuntimeReadinessV1 next = BoundaryRuleRuntimeReadinessV1.parseFrom(nextState);
            return previous.getEventId().equals(next.getEventId())
                    && previous.getPublicationEventId().equals(next.getPublicationEventId())
                    && previous.getDeploymentId().equals(next.getDeploymentId())
                    && previous.getStatus() == next.getStatus()
                    && previous.getReasonCode().equals(next.getReasonCode())
                    && previous.getDetail().equals(next.getDetail())
                    && previous.getObservedAtMs() == next.getObservedAtMs()
                    && previous.getPointCatalogEventId().equals(next.getPointCatalogEventId())
                    && previous.getPointCatalogSourceRevision()
                            .equals(next.getPointCatalogSourceRevision());
        } catch (InvalidProtocolBufferException error) {
            throw new IllegalStateException("runtime readiness state is not valid Protobuf", error);
        }
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is unavailable", error);
        }
    }
}
