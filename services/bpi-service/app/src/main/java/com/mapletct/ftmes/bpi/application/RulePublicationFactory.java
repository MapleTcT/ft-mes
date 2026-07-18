package com.mapletct.ftmes.bpi.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.application.error.BpiValidationException;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryConditionOperatorV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryEvidenceClassV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryEvidenceConditionV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRulePublicationV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundarySignalBindingV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryType;
import com.mapletct.ftmes.bpi.domain.RulePublicationEnvelope;
import com.mapletct.ftmes.bpi.domain.RuleVersionView;
import com.mapletct.ftmes.bpi.domain.TopologyVersionView;
import com.mapletct.ftmes.bpi.rules.BoundaryRuleDefinition;
import com.mapletct.ftmes.bpi.rules.ConditionOperator;
import com.mapletct.ftmes.bpi.rules.EvidenceClass;
import com.mapletct.ftmes.bpi.rules.EvidenceCondition;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class RulePublicationFactory {
    private final ObjectMapper objectMapper;

    public RulePublicationFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public RulePublicationEnvelope create(
            ActorContext actor,
            RuleVersionView rule,
            TopologyVersionView topology,
            BoundaryRuleDefinition definition,
            UUID eventId,
            Instant publishedAt,
            String topic,
            String traceId) {
        return create(actor, rule, topology, definition, eventId, publishedAt, topic, traceId, true);
    }

    public RulePublicationEnvelope create(
            ActorContext actor,
            RuleVersionView rule,
            TopologyVersionView topology,
            BoundaryRuleDefinition definition,
            UUID eventId,
            Instant publishedAt,
            String topic,
            String traceId,
            boolean active) {
        try {
            if (!"PUBLISHED".equals(topology.state())) {
                throw new BpiValidationException("Rule publication requires a PUBLISHED topology version.");
            }
            JsonNode topologyDefinition = objectMapper.valueToTree(topology.definition());
            String localityGroup = required(topologyDefinition, "localityGroup");
            Map<String, JsonNode> bindings = bindings(topologyDefinition);
            BoundaryRulePublicationV1.Builder publication = BoundaryRulePublicationV1.newBuilder()
                    .setEventId(eventId.toString())
                    .setTenantId(actor.tenantId())
                    .setPlantId(rule.plantId())
                    .setLineId(rule.lineId())
                    .setLocalityGroup(localityGroup)
                    .setTopologyCode(topology.code())
                    .setTopologyVersion(topology.version())
                    .setRuleCode(rule.code())
                    .setRuleVersion(rule.version())
                    .setBoundaryType(BoundaryType.valueOf(definition.boundaryKind().name()))
                    .setQuorumMinimum(definition.quorumMinimum())
                    .setMinimumConfidence(definition.minimumConfidence())
                    .setMaxCompositePenalty(definition.maxCompositePenalty())
                    .setAllowedLatenessMs(definition.timing().allowedLateness().toMillis())
                    .setWatermarkDelayMs(definition.timing().watermarkDelay().toMillis())
                    .setEvaluationTimeoutMs(definition.timing().evaluationTimeout().toMillis())
                    .setActive(active)
                    .setPublishedAtMs(publishedAt.toEpochMilli())
                    .setChecksum(rule.checksum())
                    .putHeaders("schema_version", "1")
                    .putHeaders("event_type", "BOUNDARY_RULE_PUBLISHED")
                    .putHeaders("lifecycle_action", active ? "ACTIVATE" : "RETIRE")
                    .putHeaders("trace_id", traceId == null ? "" : traceId);
            for (EvidenceCondition condition : definition.conditions()) {
                publication.addConditions(condition(condition));
                publication.addSignalBindings(binding(condition, bindings.get(condition.signal())));
            }
            String partitionKey = String.join(
                    ":", actor.tenantId(), rule.lineId(), rule.code(), rule.version());
            Map<String, String> headers = new LinkedHashMap<>();
            headers.put("schema_version", "1");
            headers.put("event_type", "BOUNDARY_RULE_PUBLISHED");
            headers.put("lifecycle_action", active ? "ACTIVATE" : "RETIRE");
            headers.put("trace_id", traceId == null ? "" : traceId);
            return new RulePublicationEnvelope(
                    eventId, topic, partitionKey, publication.build().toByteArray(), headers);
        } catch (BpiValidationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BpiValidationException("Rule publication payload is invalid: " + exception.getMessage());
        }
    }

    private Map<String, JsonNode> bindings(JsonNode definition) {
        JsonNode nodes = definition.path("bindings");
        if (!nodes.isArray() || nodes.isEmpty()) {
            throw new BpiValidationException("Published topology must contain signal bindings.");
        }
        Map<String, JsonNode> result = new LinkedHashMap<>();
        for (JsonNode item : nodes) {
            String signal = required(item, "signal");
            if (result.put(signal, item) != null) {
                throw new BpiValidationException("Duplicate topology signal binding: " + signal);
            }
        }
        return result;
    }

    private BoundaryEvidenceConditionV1 condition(EvidenceCondition condition) {
        BoundaryEvidenceConditionV1.Builder result = BoundaryEvidenceConditionV1.newBuilder()
                .setSignal(condition.signal())
                .setOperator(operator(condition.operator()))
                .setHoldForMs(condition.holdFor().toMillis())
                .setMaxSilenceMs(condition.maxSilence().toMillis())
                .setClassification(evidenceClass(condition.classification()))
                .setWeight(condition.weight());
        if (condition.threshold() != null) {
            result.setThresholdDecimal(condition.threshold().toPlainString());
        }
        return result.build();
    }

    private BoundarySignalBindingV1 binding(EvidenceCondition condition, JsonNode binding) {
        if (binding == null) {
            throw new BpiValidationException("Rule signal has no topology binding: " + condition.signal());
        }
        String expectedUnit = binding.path("expectedUnit").asText(binding.path("unit").asText(""));
        if (numeric(condition.operator()) && expectedUnit.isBlank()) {
            throw new BpiValidationException("Numeric signal binding requires expectedUnit: " + condition.signal());
        }
        return BoundarySignalBindingV1.newBuilder()
                .setProductId(required(binding, "productId"))
                .setDeviceId(required(binding, "deviceId"))
                .setPropertyId(required(binding, "propertyId"))
                .setSignal(condition.signal())
                .setExpectedUnit(expectedUnit)
                .setCalibrationVersion(required(binding, "calibrationVersion"))
                .build();
    }

    private BoundaryConditionOperatorV1 operator(ConditionOperator operator) {
        return BoundaryConditionOperatorV1.valueOf(operator.name());
    }

    private BoundaryEvidenceClassV1 evidenceClass(EvidenceClass classification) {
        return BoundaryEvidenceClassV1.valueOf(classification.name());
    }

    private boolean numeric(ConditionOperator operator) {
        return operator == ConditionOperator.GREATER_THAN
                || operator == ConditionOperator.LESS_THAN
                || operator == ConditionOperator.RISING;
    }

    private String required(JsonNode node, String field) {
        String value = node.path(field).asText();
        if (value == null || value.isBlank() || value.indexOf('|') >= 0) {
            throw new BpiValidationException(field + " must be nonblank and cannot contain '|'.");
        }
        return value;
    }
}
