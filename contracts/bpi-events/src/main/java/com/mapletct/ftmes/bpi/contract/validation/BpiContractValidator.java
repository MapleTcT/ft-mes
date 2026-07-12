package com.mapletct.ftmes.bpi.contract.validation;

import com.mapletct.ftmes.bpi.contract.identity.CandidateKeyFactory;
import com.mapletct.ftmes.bpi.contract.v1.BatchCandidateV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryType;
import com.mapletct.ftmes.bpi.contract.v1.PointValue;
import com.mapletct.ftmes.bpi.contract.v1.SequenceOrigin;
import com.mapletct.ftmes.bpi.contract.v1.TelemetryEnvelopeV1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class BpiContractValidator {

    private static final Set<String> QUALITY_CODES = Collections.unmodifiableSet(
        new HashSet<String>(Arrays.asList("GOOD", "UNCERTAIN", "BAD", "STALE", "SUBSTITUTED"))
    );

    private BpiContractValidator() {
    }

    public static TelemetryEnvelopeValidationResult validate(TelemetryEnvelopeV1 envelope) {
        List<ContractViolation> envelopeViolations = new ArrayList<ContractViolation>();
        if (envelope == null) {
            envelopeViolations.add(violation("envelope", "REQUIRED", "telemetry envelope is required"));
            return new TelemetryEnvelopeValidationResult(
                envelopeViolations,
                Collections.<Integer>emptyList(),
                Collections.<PointRejection>emptyList()
            );
        }

        required(envelope.getEventId(), "event_id", envelopeViolations);
        required(envelope.getTenantId(), "tenant_id", envelopeViolations);
        required(envelope.getPlantId(), "plant_id", envelopeViolations);
        required(envelope.getLineId(), "line_id", envelopeViolations);
        required(envelope.getDeviceId(), "device_id", envelopeViolations);
        positive(envelope.getEventTimeMs(), "event_time_ms", envelopeViolations);
        positive(envelope.getIngestTimeMs(), "ingest_time_ms", envelopeViolations);
        positive(envelope.getSequence(), "sequence", envelopeViolations);
        positive(envelope.getSourceEpoch(), "source_epoch", envelopeViolations);
        if (envelope.getSequenceOrigin() == SequenceOrigin.SEQUENCE_ORIGIN_UNSPECIFIED
            || envelope.getSequenceOrigin() == SequenceOrigin.UNRECOGNIZED) {
            envelopeViolations.add(violation(
                "sequence_origin",
                "UNSPECIFIED",
                "sequence origin must identify device, gateway or exporter"
            ));
        }
        if (envelope.getPointsCount() == 0) {
            envelopeViolations.add(violation("points", "EMPTY", "at least one point is required"));
        }

        if (!envelopeViolations.isEmpty()) {
            return new TelemetryEnvelopeValidationResult(
                envelopeViolations,
                Collections.<Integer>emptyList(),
                Collections.<PointRejection>emptyList()
            );
        }

        List<Integer> accepted = new ArrayList<Integer>();
        List<PointRejection> rejected = new ArrayList<PointRejection>();
        for (int index = 0; index < envelope.getPointsCount(); index++) {
            List<ContractViolation> pointViolations = validatePoint(envelope.getPoints(index), index);
            if (pointViolations.isEmpty()) {
                accepted.add(index);
            } else {
                rejected.add(new PointRejection(index, pointViolations));
            }
        }
        return new TelemetryEnvelopeValidationResult(envelopeViolations, accepted, rejected);
    }

    public static List<ContractViolation> validate(BatchCandidateV1 candidate) {
        List<ContractViolation> violations = new ArrayList<ContractViolation>();
        if (candidate == null) {
            violations.add(violation("candidate", "REQUIRED", "batch candidate is required"));
            return Collections.unmodifiableList(violations);
        }

        required(candidate.getEventId(), "event_id", violations);
        required(candidate.getCandidateKey(), "candidate_key", violations);
        required(candidate.getTenantId(), "tenant_id", violations);
        required(candidate.getPlantId(), "plant_id", violations);
        required(candidate.getLineId(), "line_id", violations);
        required(candidate.getRuleCode(), "rule_code", violations);
        required(candidate.getRuleVersion(), "rule_version", violations);
        required(candidate.getTopologyVersion(), "topology_version", violations);
        required(candidate.getFirstQuorumEvidenceEventId(), "first_quorum_evidence_event_id", violations);
        positive(candidate.getBoundaryEventTimeMs(), "boundary_event_time_ms", violations);
        positive(candidate.getEmittedAtMs(), "emitted_at_ms", violations);
        if (candidate.getConfidence() < 0.0d || candidate.getConfidence() > 1.0d) {
            violations.add(violation("confidence", "OUT_OF_RANGE", "confidence must be between 0 and 1"));
        }
        if (!candidate.getEvidenceEventIdsList().contains(candidate.getFirstQuorumEvidenceEventId())) {
            violations.add(violation(
                "evidence_event_ids",
                "QUORUM_EVIDENCE_MISSING",
                "evidence must include the first quorum event"
            ));
        }

        validateCandidateIdentity(candidate, violations);
        return Collections.unmodifiableList(violations);
    }

    private static void validateCandidateIdentity(
        BatchCandidateV1 candidate,
        List<ContractViolation> violations
    ) {
        try {
            String expectedKey;
            if (candidate.getBoundaryType() == BoundaryType.START) {
                required(candidate.getContextOrderId(), "context_order_id", violations);
                if (isBlank(candidate.getContextOrderId())) {
                    return;
                }
                expectedKey = CandidateKeyFactory.startKey(
                    candidate.getTenantId(),
                    candidate.getLineId(),
                    candidate.getRuleVersion(),
                    candidate.getContextOrderId(),
                    candidate.getFirstQuorumEvidenceEventId()
                );
            } else if (candidate.getBoundaryType() == BoundaryType.END) {
                required(candidate.getBatchId(), "batch_id", violations);
                if (isBlank(candidate.getBatchId())) {
                    return;
                }
                expectedKey = CandidateKeyFactory.endKey(
                    candidate.getBatchId(),
                    candidate.getRuleVersion(),
                    candidate.getFirstQuorumEvidenceEventId()
                );
            } else {
                violations.add(violation("boundary_type", "UNSPECIFIED", "boundary type must be START or END"));
                return;
            }

            if (!expectedKey.equals(candidate.getCandidateKey())) {
                violations.add(violation(
                    "candidate_key",
                    "NON_DETERMINISTIC_ID",
                    "candidate key does not match the canonical UUIDv5 identity"
                ));
            }
        } catch (IllegalArgumentException exception) {
            violations.add(violation("candidate_key", "INVALID_ID_INPUT", exception.getMessage()));
        }
    }

    private static List<ContractViolation> validatePoint(PointValue point, int index) {
        String prefix = "points[" + index + "].";
        List<ContractViolation> violations = new ArrayList<ContractViolation>();
        required(point.getPropertyId(), prefix + "property_id", violations);
        if (point.getValueCase() == PointValue.ValueCase.VALUE_NOT_SET) {
            violations.add(violation(prefix + "value", "REQUIRED", "point value is required"));
        }
        required(point.getQualityCode(), prefix + "quality_code", violations);
        if (!isBlank(point.getQualityCode()) && !QUALITY_CODES.contains(point.getQualityCode())) {
            violations.add(violation(
                prefix + "quality_code",
                "UNKNOWN_QUALITY",
                "quality code must come from the controlled dictionary"
            ));
        }
        positive(point.getSampleTimeMs(), prefix + "sample_time_ms", violations);
        return violations;
    }

    private static void required(String value, String path, List<ContractViolation> violations) {
        if (isBlank(value)) {
            violations.add(violation(path, "REQUIRED", path + " is required"));
        }
    }

    private static void positive(long value, String path, List<ContractViolation> violations) {
        if (value <= 0L) {
            violations.add(violation(path, "NOT_POSITIVE", path + " must be positive"));
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static ContractViolation violation(String path, String code, String message) {
        return new ContractViolation(path, code, message);
    }
}
