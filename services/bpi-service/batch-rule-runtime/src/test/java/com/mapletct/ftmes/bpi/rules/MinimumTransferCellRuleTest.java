package com.mapletct.ftmes.bpi.rules;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinimumTransferCellRuleTest {

    private static final Instant T0 = Instant.parse("2026-07-27T08:00:00Z");

    @Test
    void fiveSignalsProduceStartOnlyAfterRequiredAndTwoOfThreeQuorumEvidenceMature() {
        BoundaryRuleDefinition rule = startRule();
        BoundaryWindowState state = BoundaryWindowState.empty();
        boolean emitted = false;

        for (int sample = 0; sample < 6; sample++) {
            Instant time = T0.plusSeconds(sample);
            FrameResult result = observeFrame(
                    rule,
                    state,
                    time,
                    true,
                    true,
                    new BigDecimal("13.5"),
                    new BigDecimal("100.0").add(new BigDecimal("0.3").multiply(BigDecimal.valueOf(sample))),
                    new BigDecimal("40.0").add(new BigDecimal("0.08").multiply(BigDecimal.valueOf(sample))));
            state = result.state();
            emitted = emitted || result.newlyEligible();
            if (sample < 5) {
                assertFalse(emitted, "START must wait for the four-second rising evidence window");
            }
        }

        assertTrue(emitted);
    }

    @Test
    void missingRequiredValvePathBlocksStartEvenWhenAllThreeQuorumSignalsPass() {
        BoundaryRuleDefinition rule = startRule();
        BoundaryWindowState state = BoundaryWindowState.empty();
        boolean emitted = false;

        for (int sample = 0; sample < 7; sample++) {
            FrameResult result = observeFrame(
                    rule,
                    state,
                    T0.plusSeconds(sample),
                    true,
                    false,
                    new BigDecimal("18.2"),
                    new BigDecimal("100.0").add(new BigDecimal("0.5").multiply(BigDecimal.valueOf(sample))),
                    new BigDecimal("40.0").add(new BigDecimal("0.12").multiply(BigDecimal.valueOf(sample))));
            state = result.state();
            emitted = emitted || result.newlyEligible();
        }

        assertFalse(emitted);
    }

    @Test
    void lowFlowAndEitherStoppedPathEvidenceProduceEndAfterHoldWindow() {
        BoundaryRuleDefinition rule = endRule();
        BoundaryWindowState state = BoundaryWindowState.empty();
        boolean emitted = false;

        for (int sample = 0; sample < 5; sample++) {
            Instant time = T0.plusSeconds(sample);
            BoundaryWindowResult flow = BoundaryWindowEvaluator.onObservation(
                    rule,
                    state,
                    SignalObservation.numeric(
                            "END-FLOW-" + sample,
                            "flow.instant",
                            new BigDecimal("0.2"),
                            SignalQuality.GOOD,
                            time));
            state = flow.state();
            emitted = emitted || flow.newlyEligible();

            BoundaryWindowResult pump = BoundaryWindowEvaluator.onObservation(
                    rule,
                    state,
                    SignalObservation.bool(
                            "END-PUMP-" + sample,
                            "pump.running",
                            false,
                            SignalQuality.GOOD,
                            time));
            state = pump.state();
            emitted = emitted || pump.newlyEligible();

            BoundaryWindowResult valve = BoundaryWindowEvaluator.onObservation(
                    rule,
                    state,
                    SignalObservation.bool(
                            "END-VALVE-" + sample,
                            "valve.path.ready",
                            true,
                            SignalQuality.GOOD,
                            time));
            state = valve.state();
            emitted = emitted || valve.newlyEligible();
            if (sample < 4) {
                assertFalse(emitted, "END must wait for the four-second stop window");
            }
        }

        assertTrue(emitted);
    }

    private static FrameResult observeFrame(
            BoundaryRuleDefinition rule,
            BoundaryWindowState initial,
            Instant time,
            boolean pump,
            boolean valve,
            BigDecimal flow,
            BigDecimal totalizer,
            BigDecimal level) {
        BoundaryWindowState state = BoundaryWindowEvaluator.onObservation(
                rule,
                initial,
                SignalObservation.bool(
                        "START-PUMP-" + time.toEpochMilli(),
                        "pump.running",
                        pump,
                        SignalQuality.GOOD,
                        time)).state();
        state = BoundaryWindowEvaluator.onObservation(
                rule,
                state,
                SignalObservation.bool(
                        "START-VALVE-" + time.toEpochMilli(),
                        "valve.path.ready",
                        valve,
                        SignalQuality.GOOD,
                        time)).state();
        state = BoundaryWindowEvaluator.onObservation(
                rule,
                state,
                SignalObservation.numeric(
                        "START-FLOW-" + time.toEpochMilli(),
                        "flow.instant",
                        flow,
                        SignalQuality.GOOD,
                        time)).state();
        BoundaryWindowResult totalizerResult = BoundaryWindowEvaluator.onObservation(
                rule,
                state,
                SignalObservation.numeric(
                        "START-TOTAL-" + time.toEpochMilli(),
                        "flow.totalizer",
                        totalizer,
                        SignalQuality.GOOD,
                        time));
        BoundaryWindowResult levelResult = BoundaryWindowEvaluator.onObservation(
                rule,
                totalizerResult.state(),
                SignalObservation.numeric(
                        "START-LEVEL-" + time.toEpochMilli(),
                        "tank.level",
                        level,
                        SignalQuality.GOOD,
                        time));
        return new FrameResult(
                levelResult.state(),
                totalizerResult.newlyEligible() || levelResult.newlyEligible());
    }

    private static BoundaryRuleDefinition startRule() {
        return new BoundaryRuleDefinition(
                "RULE-S07-TRANSFER-START",
                "1.0.0",
                BoundaryKind.START,
                2,
                0.8,
                0.2,
                List.of(
                        condition("pump.running", ConditionOperator.EQUALS_TRUE, null,
                                EvidenceClass.REQUIRED, 25),
                        condition("valve.path.ready", ConditionOperator.EQUALS_TRUE, null,
                                EvidenceClass.REQUIRED, 25),
                        condition("flow.instant", ConditionOperator.GREATER_THAN, "12",
                                EvidenceClass.QUORUM, 20),
                        condition("flow.totalizer", ConditionOperator.RISING, "0.2",
                                EvidenceClass.QUORUM, 15),
                        condition("tank.level", ConditionOperator.RISING, "0.05",
                                EvidenceClass.QUORUM, 15)));
    }

    private static BoundaryRuleDefinition endRule() {
        return new BoundaryRuleDefinition(
                "RULE-S07-TRANSFER-END",
                "1.0.0",
                BoundaryKind.END,
                1,
                0.7,
                0.2,
                List.of(
                        condition("flow.instant", ConditionOperator.LESS_THAN, "0.5",
                                EvidenceClass.REQUIRED, 40),
                        condition("pump.running", ConditionOperator.EQUALS_FALSE, null,
                                EvidenceClass.QUORUM, 30),
                        condition("valve.path.ready", ConditionOperator.EQUALS_FALSE, null,
                                EvidenceClass.QUORUM, 30)));
    }

    private static EvidenceCondition condition(
            String signal,
            ConditionOperator operator,
            String threshold,
            EvidenceClass evidenceClass,
            int weight) {
        return new EvidenceCondition(
                signal,
                operator,
                threshold == null ? null : new BigDecimal(threshold),
                Duration.ofSeconds(4),
                Duration.ofSeconds(5),
                evidenceClass,
                weight);
    }

    private record FrameResult(BoundaryWindowState state, boolean newlyEligible) {
    }
}
