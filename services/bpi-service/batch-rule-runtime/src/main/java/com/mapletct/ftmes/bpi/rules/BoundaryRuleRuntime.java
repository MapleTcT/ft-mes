package com.mapletct.ftmes.bpi.rules;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public final class BoundaryRuleRuntime {

    private BoundaryRuleRuntime() {
    }

    public static BoundaryDecision evaluate(List<RuleEvidence> evidence, int quorumMinimum) {
        if (quorumMinimum < 1) {
            throw new IllegalArgumentException("quorumMinimum must be positive");
        }

        List<String> blockers = new ArrayList<>();
        int quorumSatisfied = 0;
        double earned = 0;
        double available = 0;
        double penalty = 0;

        for (RuleEvidence item : evidence) {
            available += item.weight();
            if (item.satisfied()) {
                earned += item.weight();
                penalty += item.qualityPenalty();
                if (item.classification() == EvidenceClass.QUORUM) {
                    quorumSatisfied++;
                }
            } else if (item.classification() == EvidenceClass.REQUIRED) {
                blockers.add(item.signal());
            }
        }

        if (quorumSatisfied < quorumMinimum) {
            blockers.add("quorum:" + quorumSatisfied + "/" + quorumMinimum);
        }
        double raw = available == 0 ? 0 : earned / available;
        double confidence = Math.max(0, raw - penalty);
        confidence = BigDecimal.valueOf(confidence).setScale(4, RoundingMode.HALF_UP).doubleValue();
        return new BoundaryDecision(blockers.isEmpty(), confidence, quorumSatisfied, List.copyOf(blockers));
    }

    public static BoundaryDecision evaluateScored(
            List<ScoredRuleEvidence> evidence, int quorumMinimum, double penaltyFactor, double maxCompositePenalty) {
        if (quorumMinimum < 1) throw new IllegalArgumentException("quorumMinimum must be positive");
        if (penaltyFactor < 0 || penaltyFactor > 1 || maxCompositePenalty < 0 || maxCompositePenalty > 1) {
            throw new IllegalArgumentException("penalty factors must be valid");
        }
        List<String> blockers = new ArrayList<>();
        int quorumSatisfied = 0;
        double earned = 0;
        double available = 0;
        for (ScoredRuleEvidence item : evidence) {
            available += item.weight();
            if (item.satisfied()) {
                earned += item.weight() * item.evidenceScore() * item.qualityFactor();
                if (item.classification() == EvidenceClass.QUORUM) quorumSatisfied++;
            } else if (item.classification() == EvidenceClass.REQUIRED) {
                blockers.add(item.signal());
            }
        }
        if (quorumSatisfied < quorumMinimum) blockers.add("quorum:" + quorumSatisfied + "/" + quorumMinimum);
        double base = available == 0 ? 0 : earned / available;
        double compositePenalty = Math.min(penaltyFactor, maxCompositePenalty);
        double confidence = Math.max(0, Math.min(1, base * (1 - compositePenalty)));
        confidence = BigDecimal.valueOf(confidence).setScale(4, RoundingMode.HALF_UP).doubleValue();
        return new BoundaryDecision(blockers.isEmpty(), confidence, quorumSatisfied, List.copyOf(blockers));
    }
}
