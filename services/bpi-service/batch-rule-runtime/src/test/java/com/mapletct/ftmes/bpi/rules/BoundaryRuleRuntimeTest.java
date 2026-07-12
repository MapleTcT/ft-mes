package com.mapletct.ftmes.bpi.rules;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundaryRuleRuntimeTest {

    @Test
    void requiredFailureCannotBeOffsetByOtherSignals() {
        BoundaryDecision decision = BoundaryRuleRuntime.evaluate(List.of(
                new RuleEvidence("orderReleased", EvidenceClass.REQUIRED, false, 40, 0),
                new RuleEvidence("pumpRunning", EvidenceClass.QUORUM, true, 20, 0),
                new RuleEvidence("flowStarted", EvidenceClass.QUORUM, true, 40, 0)
        ), 2);

        assertFalse(decision.eligible());
        assertTrue(decision.blockers().contains("orderReleased"));
        assertEquals(0.6, decision.confidence());
    }

    @Test
    void qualityPenaltyIsDeterministicAndQuorumIsExplicit() {
        List<RuleEvidence> evidence = List.of(
                new RuleEvidence("orderReleased", EvidenceClass.REQUIRED, true, 40, 0),
                new RuleEvidence("pumpRunning", EvidenceClass.QUORUM, true, 20, 0.05),
                new RuleEvidence("flowStarted", EvidenceClass.QUORUM, true, 30, 0),
                new RuleEvidence("tankRising", EvidenceClass.OPTIONAL, false, 10, 0)
        );

        BoundaryDecision first = BoundaryRuleRuntime.evaluate(evidence, 2);
        BoundaryDecision replay = BoundaryRuleRuntime.evaluate(evidence, 2);

        assertTrue(first.eligible());
        assertEquals(0.85, first.confidence());
        assertEquals(first, replay);
    }
}
