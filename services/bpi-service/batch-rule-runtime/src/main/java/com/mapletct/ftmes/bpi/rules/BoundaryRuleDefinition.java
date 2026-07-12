package com.mapletct.ftmes.bpi.rules;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record BoundaryRuleDefinition(
        String ruleCode,
        String ruleVersion,
        BoundaryKind boundaryKind,
        int quorumMinimum,
        double minimumConfidence,
        double maxCompositePenalty,
        List<EvidenceCondition> conditions) {

    public BoundaryRuleDefinition {
        Objects.requireNonNull(ruleCode, "ruleCode");
        Objects.requireNonNull(ruleVersion, "ruleVersion");
        Objects.requireNonNull(boundaryKind, "boundaryKind");
        Objects.requireNonNull(conditions, "conditions");
        conditions = List.copyOf(conditions);
        if (ruleCode.isBlank() || ruleVersion.isBlank()) {
            throw new IllegalArgumentException("rule code and version are required");
        }
        if (quorumMinimum < 1) throw new IllegalArgumentException("quorumMinimum must be positive");
        if (minimumConfidence < 0 || minimumConfidence > 1) {
            throw new IllegalArgumentException("minimumConfidence must be between 0 and 1");
        }
        if (maxCompositePenalty < 0 || maxCompositePenalty > 1) {
            throw new IllegalArgumentException("maxCompositePenalty must be between 0 and 1");
        }
        Set<String> signals = new HashSet<>();
        int quorumConditions = 0;
        int totalWeight = 0;
        for (EvidenceCondition condition : conditions) {
            if (!signals.add(condition.signal())) {
                throw new IllegalArgumentException("duplicate evidence signal: " + condition.signal());
            }
            if (condition.classification() == EvidenceClass.QUORUM) quorumConditions++;
            totalWeight += condition.weight();
        }
        if (quorumConditions < quorumMinimum) {
            throw new IllegalArgumentException("quorumMinimum exceeds configured quorum evidence");
        }
        if (totalWeight == 0) throw new IllegalArgumentException("at least one evidence weight must be positive");
    }
}
