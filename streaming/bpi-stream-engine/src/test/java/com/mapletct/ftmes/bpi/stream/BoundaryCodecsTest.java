package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.rules.BoundaryKind;
import com.mapletct.ftmes.bpi.rules.BoundaryRuleDefinition;
import com.mapletct.ftmes.bpi.rules.BoundaryWindowState;
import com.mapletct.ftmes.bpi.rules.ConditionOperator;
import com.mapletct.ftmes.bpi.rules.ConditionStatus;
import com.mapletct.ftmes.bpi.rules.EvidenceClass;
import com.mapletct.ftmes.bpi.rules.EvidenceCondition;
import com.mapletct.ftmes.bpi.rules.EvidenceSignalState;
import com.mapletct.ftmes.bpi.rules.SignalQuality;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BoundaryCodecsTest {

    private static final Instant T0 = Instant.parse("2026-07-12T08:00:00.123456789Z");

    @Test
    void ruleCodecRoundTripsWithoutLosingDecimalOrDurationSemantics() {
        BoundaryRuleDefinition rule = rule();

        BoundaryRuleDefinition restored = BoundaryRuleCodec.decode(BoundaryRuleCodec.encode(rule));

        assertEquals(rule, restored);
    }

    @Test
    void operatorStateCodecRoundTripsAllNullableSignalFields() {
        BoundaryExecutionContext context = new BoundaryExecutionContext(
                "TENANT-A", "PLANT-01", "LINE-01", "FEED", "TOPO-1", "7", "MO-1", null);
        EvidenceSignalState numeric = new EvidenceSignalState(
                "feed.flow", ConditionStatus.PENDING, T0, "EVT-1", "EVT-2", T0.plusSeconds(1),
                new BigDecimal("1.2300"), new BigDecimal("2.3400"), null, SignalQuality.UNCERTAIN);
        EvidenceSignalState bool = new EvidenceSignalState(
                "feed.pump", ConditionStatus.TRUE, T0, "EVT-3", "EVT-3", T0,
                null, null, true, SignalQuality.GOOD);
        BoundaryOperatorState state = new BoundaryOperatorState(
                context,
                new BoundaryRuleRef("START-01", "1.2.3"),
                new BoundaryWindowState(Map.of(numeric.signal(), numeric, bool.signal(), bool), false, null),
                T0.plusSeconds(10).toEpochMilli());

        BoundaryOperatorState restored = BoundaryOperatorStateCodec.decode(
                BoundaryOperatorStateCodec.encode(state));

        assertEquals(state, restored);
    }

    @Test
    void codecsRejectUnknownVersionInsteadOfSilentlyResettingState() {
        byte[] encoded = BoundaryRuleCodec.encode(rule());
        encoded[7] = 2;

        assertThrows(IllegalStateException.class, () -> BoundaryRuleCodec.decode(encoded));
    }

    @Test
    void stateEncodingIsDeterministicAcrossMapInsertionOrder() {
        EvidenceSignalState first = new EvidenceSignalState(
                "a", ConditionStatus.TRUE, T0, "A", "A", T0,
                null, null, true, SignalQuality.GOOD);
        EvidenceSignalState second = new EvidenceSignalState(
                "b", ConditionStatus.TRUE, T0, "B", "B", T0,
                null, null, false, SignalQuality.GOOD);
        BoundaryExecutionContext context = new BoundaryExecutionContext(
                "T", "P", "L", "G", "TOPO", "1", "MO", null);
        BoundaryRuleRef ruleRef = new BoundaryRuleRef("R", "1");
        BoundaryOperatorState left = new BoundaryOperatorState(
                context, ruleRef, new BoundaryWindowState(Map.of("a", first, "b", second), false, null), 10);
        BoundaryOperatorState right = new BoundaryOperatorState(
                context, ruleRef, new BoundaryWindowState(Map.of("b", second, "a", first), false, null), 10);

        assertEquals(
                Arrays.toString(BoundaryOperatorStateCodec.encode(left)),
                Arrays.toString(BoundaryOperatorStateCodec.encode(right)));
    }

    private static BoundaryRuleDefinition rule() {
        return new BoundaryRuleDefinition(
                "START-01", "1.2.3", BoundaryKind.START, 1, 0.75, 0.2,
                List.of(
                        new EvidenceCondition(
                                "feed.flow", ConditionOperator.GREATER_THAN, new BigDecimal("2.5000"),
                                Duration.ofMillis(1500), Duration.ofSeconds(30), EvidenceClass.QUORUM, 60),
                        new EvidenceCondition(
                                "order.active", ConditionOperator.EQUALS_TRUE, null,
                                Duration.ZERO, Duration.ofSeconds(60), EvidenceClass.REQUIRED, 40)));
    }
}
