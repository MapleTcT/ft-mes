package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.rules.BoundaryKind;
import com.mapletct.ftmes.bpi.rules.BoundaryRuleDefinition;
import com.mapletct.ftmes.bpi.rules.BoundaryWindowEvaluator;
import com.mapletct.ftmes.bpi.rules.BoundaryWindowResult;
import com.mapletct.ftmes.bpi.rules.BoundaryWindowState;
import com.mapletct.ftmes.bpi.rules.ConditionOperator;
import com.mapletct.ftmes.bpi.rules.EvidenceClass;
import com.mapletct.ftmes.bpi.rules.EvidenceCondition;
import com.mapletct.ftmes.bpi.rules.SignalObservation;
import com.mapletct.ftmes.bpi.rules.SignalQuality;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BoundaryKeyedBroadcastFunctionTest {

    private static final Instant T0 = Instant.parse("2026-07-12T08:00:00Z");

    @Test
    void nextDeadlineUsesEarliestMaturityAndThenStopsAfterCandidate() {
        BoundaryRuleDefinition rule = rule();
        BoundaryWindowState state = BoundaryWindowState.empty();
        state = observe(rule, state, SignalObservation.bool(
                "ORDER", "order.active", true, SignalQuality.GOOD, T0));
        state = observe(rule, state, SignalObservation.numeric(
                "FLOW", "feed.flow", new BigDecimal("3"), SignalQuality.GOOD, T0.plusSeconds(1)));

        assertEquals(
                T0.plusSeconds(11).toEpochMilli(),
                BoundaryKeyedBroadcastFunction.nextDeadline(rule, state, T0.plusSeconds(1)));

        BoundaryWindowResult eligible = BoundaryWindowEvaluator.advanceEventTime(
                rule, state, T0.plusSeconds(11), 0);
        assertEquals(
                BoundaryOperatorState.NO_TIMER,
                BoundaryKeyedBroadcastFunction.nextDeadline(rule, eligible.state(), T0.plusSeconds(11)));
    }

    @Test
    void nextDeadlineSchedulesUnknownTransitionOneMillisecondAfterMaxSilence() {
        BoundaryRuleDefinition rule = rule();
        BoundaryWindowState state = observe(
                rule,
                BoundaryWindowState.empty(),
                SignalObservation.bool("ORDER", "order.active", true, SignalQuality.GOOD, T0));

        assertEquals(
                T0.plusSeconds(30).plusMillis(1).toEpochMilli(),
                BoundaryKeyedBroadcastFunction.nextDeadline(rule, state, T0));
    }

    @Test
    void nextDeadlineRoundsSubMillisecondMaturityUpToTheNextFlinkTimestamp() {
        BoundaryRuleDefinition rule = rule();
        Instant observedAt = T0.plusSeconds(1).plusNanos(500_000);
        BoundaryWindowState state = observe(
                rule,
                BoundaryWindowState.empty(),
                SignalObservation.numeric(
                        "FLOW", "feed.flow", new BigDecimal("3"), SignalQuality.GOOD, observedAt));

        assertEquals(
                T0.plusSeconds(11).plusMillis(1).toEpochMilli(),
                BoundaryKeyedBroadcastFunction.nextDeadline(rule, state, observedAt));
    }

    private static BoundaryWindowState observe(
            BoundaryRuleDefinition rule,
            BoundaryWindowState state,
            SignalObservation observation) {
        return BoundaryWindowEvaluator.onObservation(rule, state, observation).state();
    }

    private static BoundaryRuleDefinition rule() {
        return new BoundaryRuleDefinition(
                "START-01", "1", BoundaryKind.START, 1, 1.0, 0,
                List.of(
                        new EvidenceCondition(
                                "order.active", ConditionOperator.EQUALS_TRUE, null,
                                Duration.ZERO, Duration.ofSeconds(30), EvidenceClass.REQUIRED, 50),
                        new EvidenceCondition(
                                "feed.flow", ConditionOperator.GREATER_THAN, new BigDecimal("2"),
                                Duration.ofSeconds(10), Duration.ofSeconds(30), EvidenceClass.QUORUM, 50)));
    }
}
