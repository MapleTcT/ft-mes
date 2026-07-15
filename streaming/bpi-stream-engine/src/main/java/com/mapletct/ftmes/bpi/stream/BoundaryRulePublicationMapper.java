package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.contract.v1.BoundaryConditionOperatorV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryEvidenceClassV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryEvidenceConditionV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRulePublicationV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundarySignalBindingV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryType;
import com.mapletct.ftmes.bpi.rules.BoundaryKind;
import com.mapletct.ftmes.bpi.rules.BoundaryRuleDefinition;
import com.mapletct.ftmes.bpi.rules.BoundaryTimingPolicy;
import com.mapletct.ftmes.bpi.rules.ConditionOperator;
import com.mapletct.ftmes.bpi.rules.EvidenceClass;
import com.mapletct.ftmes.bpi.rules.EvidenceCondition;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BoundaryRulePublicationMapper {

    private BoundaryRulePublicationMapper() {
    }

    public static PublishedBoundaryPlan map(BoundaryRulePublicationV1 publication) {
        require(publication.getEventId(), "event_id");
        require(publication.getTenantId(), "tenant_id");
        require(publication.getPlantId(), "plant_id");
        require(publication.getLineId(), "line_id");
        require(publication.getLocalityGroup(), "locality_group");
        require(publication.getTopologyCode(), "topology_code");
        require(publication.getTopologyVersion(), "topology_version");
        require(publication.getRuleCode(), "rule_code");
        require(publication.getRuleVersion(), "rule_version");
        require(publication.getChecksum(), "checksum");
        if (publication.getPublishedAtMs() <= 0) {
            throw new IllegalArgumentException("published_at_ms must be positive");
        }
        BoundaryKind boundaryKind = boundaryKind(publication.getBoundaryType());
        List<EvidenceCondition> conditions = publication.getConditionsList().stream()
                .map(BoundaryRulePublicationMapper::condition)
                .toList();
        BoundaryRuleDefinition rule = new BoundaryRuleDefinition(
                publication.getRuleCode(),
                publication.getRuleVersion(),
                boundaryKind,
                publication.getQuorumMinimum(),
                publication.getMinimumConfidence(),
                publication.getMaxCompositePenalty(),
                new BoundaryTimingPolicy(
                        duration(publication.getAllowedLatenessMs(), "allowed_lateness_ms", true),
                        duration(publication.getWatermarkDelayMs(), "watermark_delay_ms", true),
                        duration(publication.getEvaluationTimeoutMs(), "evaluation_timeout_ms", false)),
                conditions);
        Map<String, BoundarySignalBindingV1> bindings = bindings(publication, conditions);
        return new PublishedBoundaryPlan(publication, rule, bindings);
    }

    private static EvidenceCondition condition(BoundaryEvidenceConditionV1 source) {
        require(source.getSignal(), "condition.signal");
        ConditionOperator operator = operator(source.getOperator());
        BigDecimal threshold = switch (operator) {
            case GREATER_THAN, LESS_THAN, RISING -> decimal(source.getThresholdDecimal());
            case EQUALS_TRUE, EQUALS_FALSE -> null;
        };
        return new EvidenceCondition(
                source.getSignal(),
                operator,
                threshold,
                duration(source.getHoldForMs(), "condition.hold_for_ms", true),
                duration(source.getMaxSilenceMs(), "condition.max_silence_ms", false),
                evidenceClass(source.getClassification()),
                source.getWeight());
    }

    private static Map<String, BoundarySignalBindingV1> bindings(
            BoundaryRulePublicationV1 publication,
            List<EvidenceCondition> conditions) {
        Set<String> configuredSignals = new HashSet<>();
        Map<String, EvidenceCondition> conditionsBySignal = new HashMap<>();
        for (EvidenceCondition condition : conditions) {
            configuredSignals.add(condition.signal());
            conditionsBySignal.put(condition.signal(), condition);
        }
        Set<String> boundSignals = new HashSet<>();
        Map<String, BoundarySignalBindingV1> result = new HashMap<>();
        for (BoundarySignalBindingV1 binding : publication.getSignalBindingsList()) {
            require(binding.getProductId(), "binding.product_id");
            require(binding.getDeviceId(), "binding.device_id");
            require(binding.getPropertyId(), "binding.property_id");
            require(binding.getSignal(), "binding.signal");
            require(binding.getCalibrationVersion(), "binding.calibration_version");
            if (!configuredSignals.contains(binding.getSignal())) {
                throw new IllegalArgumentException(
                        "binding references a signal that is absent from conditions: " + binding.getSignal());
            }
            EvidenceCondition condition = conditionsBySignal.get(binding.getSignal());
            if (condition.operator() != ConditionOperator.EQUALS_TRUE
                    && condition.operator() != ConditionOperator.EQUALS_FALSE
                    && binding.getExpectedUnit().isBlank()) {
                throw new IllegalArgumentException(
                        "numeric signal binding requires expected_unit: " + binding.getSignal());
            }
            String key = PublishedBoundaryPlan.bindingKey(
                    binding.getProductId(), binding.getDeviceId(), binding.getPropertyId());
            if (result.put(key, binding) != null) {
                throw new IllegalArgumentException("duplicate product/device/property binding: " + key);
            }
            if (!boundSignals.add(binding.getSignal())) {
                throw new IllegalArgumentException("duplicate signal binding: " + binding.getSignal());
            }
        }
        if (!boundSignals.equals(configuredSignals)) {
            Set<String> missing = new HashSet<>(configuredSignals);
            missing.removeAll(boundSignals);
            throw new IllegalArgumentException("conditions have no signal binding: " + missing);
        }
        return result;
    }

    private static BoundaryKind boundaryKind(BoundaryType type) {
        return switch (type) {
            case START -> BoundaryKind.START;
            case END -> BoundaryKind.END;
            default -> throw new IllegalArgumentException("boundary_type must be START or END");
        };
    }

    private static ConditionOperator operator(BoundaryConditionOperatorV1 operator) {
        return switch (operator) {
            case GREATER_THAN -> ConditionOperator.GREATER_THAN;
            case LESS_THAN -> ConditionOperator.LESS_THAN;
            case EQUALS_TRUE -> ConditionOperator.EQUALS_TRUE;
            case EQUALS_FALSE -> ConditionOperator.EQUALS_FALSE;
            case RISING -> ConditionOperator.RISING;
            default -> throw new IllegalArgumentException("condition operator is unspecified");
        };
    }

    private static EvidenceClass evidenceClass(BoundaryEvidenceClassV1 classification) {
        return switch (classification) {
            case REQUIRED -> EvidenceClass.REQUIRED;
            case QUORUM -> EvidenceClass.QUORUM;
            case OPTIONAL -> EvidenceClass.OPTIONAL;
            default -> throw new IllegalArgumentException("evidence classification is unspecified");
        };
    }

    private static Duration duration(long millis, String field, boolean zeroAllowed) {
        if (millis < 0 || (!zeroAllowed && millis == 0)) {
            throw new IllegalArgumentException(field + (zeroAllowed ? " cannot be negative" : " must be positive"));
        }
        return Duration.ofMillis(millis);
    }

    private static BigDecimal decimal(String value) {
        require(value, "condition.threshold_decimal");
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("condition.threshold_decimal is invalid", error);
        }
    }

    private static void require(String value, String field) {
        if (value == null || value.isBlank() || value.indexOf('|') >= 0) {
            throw new IllegalArgumentException(field + " must be nonblank and cannot contain '|'");
        }
    }
}
