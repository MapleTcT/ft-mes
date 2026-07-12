package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.contract.identity.CandidateKeyFactory;
import com.mapletct.ftmes.bpi.contract.v1.BatchCandidateV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryType;
import com.mapletct.ftmes.bpi.rules.BoundaryEvidenceSnapshot;
import com.mapletct.ftmes.bpi.rules.BoundaryKind;
import com.mapletct.ftmes.bpi.rules.BoundaryRuleDefinition;
import com.mapletct.ftmes.bpi.rules.BoundaryWindowResult;
import com.mapletct.ftmes.bpi.rules.ConditionStatus;

import java.time.Instant;
import java.util.Comparator;

public final class BoundaryCandidateProjector {

    private BoundaryCandidateProjector() {
    }

    public static BatchCandidateV1 project(
            BoundaryRuleDefinition rule,
            BoundaryExecutionContext context,
            BoundaryWindowResult result,
            Instant boundaryEventTime) {
        if (!result.newlyEligible() || result.firstQuorumEvidenceEventId() == null) {
            throw new IllegalArgumentException("only a newly eligible boundary can be projected");
        }
        String candidateKey = rule.boundaryKind() == BoundaryKind.START
                ? CandidateKeyFactory.startKey(
                    context.tenantId(), context.lineId(), rule.ruleVersion(),
                    required(context.contextOrderId(), "contextOrderId"), result.firstQuorumEvidenceEventId())
                : CandidateKeyFactory.endKey(
                    required(context.batchId(), "batchId"), rule.ruleVersion(),
                    result.firstQuorumEvidenceEventId());
        BatchCandidateV1.Builder candidate = BatchCandidateV1.newBuilder()
                .setEventId("CANDIDATE-" + candidateKey)
                .setCandidateKey(candidateKey)
                .setTenantId(context.tenantId())
                .setPlantId(context.plantId())
                .setLineId(context.lineId())
                .setBoundaryType(rule.boundaryKind() == BoundaryKind.START ? BoundaryType.START : BoundaryType.END)
                .setRuleCode(rule.ruleCode())
                .setRuleVersion(rule.ruleVersion())
                .setTopologyVersion(context.topologyVersion())
                .setFirstQuorumEvidenceEventId(result.firstQuorumEvidenceEventId())
                .setBoundaryEventTimeMs(boundaryEventTime.toEpochMilli())
                .setConfidence(result.decision().confidence())
                .setEmittedAtMs(boundaryEventTime.toEpochMilli())
                .putHeaders("topology_code", context.topologyCode())
                .putHeaders("locality_group", context.localityGroup());
        if (rule.boundaryKind() == BoundaryKind.START) {
            candidate.setContextOrderId(context.contextOrderId());
        } else {
            candidate.setBatchId(context.batchId());
        }
        result.evidence().stream()
                .filter(item -> item.status() == ConditionStatus.TRUE && item.eventId() != null)
                .sorted(Comparator.comparing(BoundaryEvidenceSnapshot::eventTime)
                        .thenComparing(BoundaryEvidenceSnapshot::signal))
                .map(BoundaryEvidenceSnapshot::eventId)
                .distinct()
                .forEach(candidate::addEvidenceEventIds);
        return candidate.build();
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value;
    }
}
