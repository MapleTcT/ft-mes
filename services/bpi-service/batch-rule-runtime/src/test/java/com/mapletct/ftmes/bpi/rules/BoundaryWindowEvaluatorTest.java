package com.mapletct.ftmes.bpi.rules;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundaryWindowEvaluatorTest {

    private static final Instant T0 = Instant.parse("2026-07-12T08:00:00Z");

    @Test
    void eventTimeHoldAndQuorumEmitOneDeterministicStartCandidate() {
        BoundaryRuleDefinition rule = startRule();
        BoundaryWindowState state = BoundaryWindowState.empty();

        state = observe(rule, state, SignalObservation.bool(
                "EVT-ORDER", "order.active", true, SignalQuality.GOOD, T0)).state();
        state = observe(rule, state, SignalObservation.bool(
                "EVT-PUMP", "feed.pump", true, SignalQuality.GOOD, T0.plusSeconds(1))).state();
        state = observe(rule, state, SignalObservation.numeric(
                "EVT-FLOW", "feed.flow", new BigDecimal("2.5"), SignalQuality.GOOD,
                T0.plusSeconds(1))).state();

        BoundaryWindowResult beforeHold = BoundaryWindowEvaluator.advanceEventTime(
                rule, state, T0.plusSeconds(9), 0);
        assertFalse(beforeHold.decision().eligible());
        assertTrue(beforeHold.decision().blockers().contains("quorum:1/2"));

        BoundaryWindowResult eligible = BoundaryWindowEvaluator.advanceEventTime(
                rule, beforeHold.state(), T0.plusSeconds(11), 0);
        assertTrue(eligible.decision().eligible());
        assertTrue(eligible.newlyEligible());
        assertEquals(0.9, eligible.decision().confidence());
        assertEquals("EVT-FLOW", eligible.firstQuorumEvidenceEventId());

        BoundaryWindowResult replay = BoundaryWindowEvaluator.advanceEventTime(
                rule, eligible.state(), T0.plusSeconds(12), 0);
        assertTrue(replay.decision().eligible());
        assertFalse(replay.newlyEligible());
        assertEquals("EVT-FLOW", replay.firstQuorumEvidenceEventId());
    }

    @Test
    void maxSilenceTurnsEvidenceUnknownInsteadOfHoldingLastValueForever() {
        BoundaryRuleDefinition rule = startRule();
        BoundaryWindowState state = BoundaryWindowState.empty();
        state = observe(rule, state, SignalObservation.bool(
                "EVT-ORDER", "order.active", true, SignalQuality.GOOD, T0)).state();
        state = observe(rule, state, SignalObservation.bool(
                "EVT-PUMP", "feed.pump", true, SignalQuality.GOOD, T0)).state();
        state = observe(rule, state, SignalObservation.numeric(
                "EVT-FLOW", "feed.flow", new BigDecimal("3.0"), SignalQuality.GOOD, T0)).state();

        BoundaryWindowResult expired = BoundaryWindowEvaluator.advanceEventTime(
                rule, state, T0.plusSeconds(31), 0);

        assertFalse(expired.decision().eligible());
        assertTrue(expired.evidence().stream().allMatch(item -> item.status() == ConditionStatus.UNKNOWN));
    }

    @Test
    void qualityFactorAndCompositePenaltyUseConfiguredDenominator() {
        BoundaryRuleDefinition rule = new BoundaryRuleDefinition(
                "QUALITY", "1", BoundaryKind.START, 1, 0.6, 0.8,
                List.of(condition("flow", ConditionOperator.GREATER_THAN, "2.0", 0, 30,
                        EvidenceClass.QUORUM, 100)));
        BoundaryWindowResult uncertain = observe(rule, BoundaryWindowState.empty(), SignalObservation.numeric(
                "EVT-U", "flow", new BigDecimal("3.0"), SignalQuality.UNCERTAIN, T0));
        assertEquals(0.5, uncertain.decision().confidence());
        assertFalse(uncertain.decision().eligible());

        BoundaryWindowResult good = observe(rule, BoundaryWindowState.empty(), SignalObservation.numeric(
                "EVT-G", "flow", new BigDecimal("3.0"), SignalQuality.GOOD, T0));
        BoundaryWindowResult penalized = BoundaryWindowEvaluator.advanceEventTime(
                rule, good.state(), T0.plusSeconds(1), 0.2);
        assertEquals(0.8, penalized.decision().confidence());
        assertTrue(penalized.decision().eligible());
    }

    @Test
    void outOfOrderObservationIsIgnoredWithoutRewindingSignalState() {
        BoundaryRuleDefinition rule = new BoundaryRuleDefinition(
                "ORDERING", "1", BoundaryKind.START, 1, 0.5, 0.8,
                List.of(condition("flow", ConditionOperator.GREATER_THAN, "2.0", 0, 30,
                        EvidenceClass.QUORUM, 100)));
        BoundaryWindowResult current = observe(rule, BoundaryWindowState.empty(), SignalObservation.numeric(
                "EVT-NEW", "flow", new BigDecimal("3.0"), SignalQuality.GOOD, T0.plusSeconds(2)));
        BoundaryWindowResult stale = observe(rule, current.state(), SignalObservation.numeric(
                "EVT-OLD", "flow", new BigDecimal("0.0"), SignalQuality.GOOD, T0));

        assertTrue(stale.ignoredObservation());
        assertEquals("EVT-NEW", stale.state().signals().get("flow").lastEventId());
        assertTrue(stale.decision().eligible());
        assertFalse(stale.newlyEligible());
    }

    @Test
    void risingConditionRequiresTwoNumericSamples() {
        BoundaryRuleDefinition rule = new BoundaryRuleDefinition(
                "TANK-RISING", "1", BoundaryKind.START, 1, 0.8, 0.8,
                List.of(condition("tank.level", ConditionOperator.RISING, "0.2", 0, 30,
                        EvidenceClass.QUORUM, 100)));
        BoundaryWindowResult first = observe(rule, BoundaryWindowState.empty(), SignalObservation.numeric(
                "EVT-L1", "tank.level", new BigDecimal("10.0"), SignalQuality.GOOD, T0));
        assertFalse(first.decision().eligible());

        BoundaryWindowResult second = observe(rule, first.state(), SignalObservation.numeric(
                "EVT-L2", "tank.level", new BigDecimal("10.3"), SignalQuality.GOOD, T0.plusSeconds(1)));
        assertTrue(second.decision().eligible());
        assertTrue(second.newlyEligible());
        assertEquals("EVT-L2", second.firstQuorumEvidenceEventId());
    }

    @Test
    void impossibleQuorumIsRejectedWhenTheRuleVersionIsBuilt() {
        assertThrows(IllegalArgumentException.class, () -> new BoundaryRuleDefinition(
                "INVALID", "1", BoundaryKind.START, 2, 0.8, 0.8,
                List.of(condition("flow", ConditionOperator.GREATER_THAN, "2.0", 0, 30,
                        EvidenceClass.QUORUM, 100))));
    }

    private static BoundaryWindowResult observe(
            BoundaryRuleDefinition rule, BoundaryWindowState state, SignalObservation observation) {
        return BoundaryWindowEvaluator.onObservation(rule, state, observation);
    }

    private static BoundaryRuleDefinition startRule() {
        return new BoundaryRuleDefinition(
                "S05-FEED-START", "1.0.0", BoundaryKind.START, 2, 0.85, 0.8,
                List.of(
                        condition("order.active", ConditionOperator.EQUALS_TRUE, null, 0, 30,
                                EvidenceClass.REQUIRED, 40),
                        condition("feed.pump", ConditionOperator.EQUALS_TRUE, null, 3, 30,
                                EvidenceClass.QUORUM, 20),
                        condition("feed.flow", ConditionOperator.GREATER_THAN, "2.0", 10, 30,
                                EvidenceClass.QUORUM, 30),
                        condition("column.level", ConditionOperator.RISING, "0.1", 15, 30,
                                EvidenceClass.OPTIONAL, 10)));
    }

    private static EvidenceCondition condition(
            String signal, ConditionOperator operator, String threshold, long holdSeconds, long silenceSeconds,
            EvidenceClass classification, int weight) {
        return new EvidenceCondition(
                signal, operator, threshold == null ? null : new BigDecimal(threshold),
                Duration.ofSeconds(holdSeconds), Duration.ofSeconds(silenceSeconds), classification, weight);
    }
}
