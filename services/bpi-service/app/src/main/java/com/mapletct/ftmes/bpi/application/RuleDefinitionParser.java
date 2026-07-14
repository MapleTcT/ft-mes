package com.mapletct.ftmes.bpi.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.application.error.BpiValidationException;
import com.mapletct.ftmes.bpi.domain.RuleVersionView;
import com.mapletct.ftmes.bpi.rules.BoundaryKind;
import com.mapletct.ftmes.bpi.rules.BoundaryRuleDefinition;
import com.mapletct.ftmes.bpi.rules.BoundaryTimingPolicy;
import com.mapletct.ftmes.bpi.rules.ConditionOperator;
import com.mapletct.ftmes.bpi.rules.EvidenceClass;
import com.mapletct.ftmes.bpi.rules.EvidenceCondition;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class RuleDefinitionParser {
    private final ObjectMapper objectMapper;

    public RuleDefinitionParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public BoundaryRuleDefinition parse(RuleVersionView rule) {
        return parse(rule.code(), rule.version(), rule.ast());
    }

    public BoundaryRuleDefinition parse(String code, String version, Map<String, Object> definition) {
        try {
            JsonNode ast = objectMapper.valueToTree(definition);
            List<EvidenceCondition> conditions = new ArrayList<>();
            JsonNode conditionNodes = ast.path("conditions");
            if (!conditionNodes.isArray() || conditionNodes.isEmpty()) {
                throw new IllegalArgumentException("conditions must be a non-empty array");
            }
            for (JsonNode item : conditionNodes) {
                ConditionOperator operator = ConditionOperator.valueOf(required(item, "operator"));
                BigDecimal threshold = item.hasNonNull("threshold") ? item.path("threshold").decimalValue() : null;
                conditions.add(new EvidenceCondition(
                        required(item, "signal"),
                        operator,
                        threshold,
                        Duration.ofSeconds(item.path("holdSeconds").asLong(0)),
                        Duration.ofSeconds(item.path("maxSilenceSeconds").asLong(60)),
                        EvidenceClass.valueOf(required(item, "classification")),
                        item.path("weight").asInt()));
            }
            JsonNode timing = ast.path("timing");
            BoundaryTimingPolicy policy = new BoundaryTimingPolicy(
                    Duration.ofSeconds(timing.path("allowedLatenessSeconds").asLong(0)),
                    Duration.ofSeconds(timing.path("watermarkDelaySeconds").asLong(0)),
                    Duration.ofSeconds(timing.path("evaluationTimeoutSeconds").asLong(300)));
            return new BoundaryRuleDefinition(
                    code,
                    version,
                    BoundaryKind.valueOf(required(ast, "boundaryType")),
                    ast.path("quorumMinimum").asInt(),
                    ast.path("minimumConfidence").asDouble(0.60),
                    ast.path("maxCompositePenalty").asDouble(0.80),
                    policy,
                    List.copyOf(conditions));
        } catch (BpiValidationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BpiValidationException("Rule AST is invalid: " + exception.getMessage());
        }
    }

    private String required(JsonNode node, String field) {
        String value = node.path(field).asText();
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value;
    }
}
