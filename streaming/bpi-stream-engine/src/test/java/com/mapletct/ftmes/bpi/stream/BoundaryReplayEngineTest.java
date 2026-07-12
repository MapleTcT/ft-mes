package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.contract.validation.BpiContractValidator;
import com.mapletct.ftmes.bpi.contract.v1.BatchCandidateV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryType;
import com.mapletct.ftmes.bpi.rules.BoundaryKind;
import com.mapletct.ftmes.bpi.rules.BoundaryRuleDefinition;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundaryReplayEngineTest {

    private static final Instant T0 = Instant.parse("2026-07-12T08:00:00Z");

    @Test
    void startReplayProducesOneContractValidAndReplayStableCandidate() {
        BoundaryRuleDefinition rule = startRule();
        BoundaryExecutionContext context = new BoundaryExecutionContext(
                "TENANT-A", "PLANT-01", "LINE-S07-01", "S07-FEED",
                "TOPO-S07", "3", "MO-20260712-001", null);
        List<BoundaryEngineInput> inputs = List.of(
                input(context, SignalObservation.numeric(
                        "EVT-FLOW", "feed.flow", new BigDecimal("2.6"), SignalQuality.GOOD, T0.plusSeconds(1))),
                input(context, SignalObservation.bool(
                        "EVT-ORDER", "order.active", true, SignalQuality.GOOD, T0)),
                input(context, SignalObservation.bool(
                        "EVT-PUMP", "feed.pump", true, SignalQuality.GOOD, T0.plusSeconds(1))));

        List<BatchCandidateV1> first = BoundaryReplayEngine.replay(rule, inputs, T0.plusSeconds(11));
        List<BatchCandidateV1> replay = BoundaryReplayEngine.replay(rule, inputs, T0.plusSeconds(11));

        assertEquals(1, first.size());
        BatchCandidateV1 candidate = first.get(0);
        assertEquals(candidate, replay.get(0));
        assertEquals(BoundaryType.START, candidate.getBoundaryType());
        assertEquals("EVT-FLOW", candidate.getFirstQuorumEvidenceEventId());
        assertEquals("MO-20260712-001", candidate.getContextOrderId());
        assertEquals(0.9, candidate.getConfidence());
        assertEquals(List.of("EVT-ORDER", "EVT-FLOW", "EVT-PUMP"), candidate.getEvidenceEventIdsList());
        assertEquals(3, candidate.getEvidenceCount());
        assertEquals("feed.flow", candidate.getEvidence(1).getSignal());
        assertEquals(List.of("column.level"), candidate.getMissingSignalsList());
        assertTrue(BpiContractValidator.validate(candidate).isEmpty());
    }

    @Test
    void endReplayUsesBatchIdentityAndDoesNotDependOnProcessingTime() {
        BoundaryRuleDefinition rule = new BoundaryRuleDefinition(
                "S07-FEED-END", "1.0.0", BoundaryKind.END, 2, 0.9, 0.8,
                List.of(
                        condition("feed.flow", ConditionOperator.LESS_THAN, "0.5", 20,
                                EvidenceClass.QUORUM, 60),
                        condition("feed.pump", ConditionOperator.EQUALS_FALSE, null, 0,
                                EvidenceClass.QUORUM, 40)));
        BoundaryExecutionContext context = new BoundaryExecutionContext(
                "TENANT-A", "PLANT-01", "LINE-S07-01", "S07-FEED",
                "TOPO-S07", "3", null, "BATCH-S07-0001");
        List<BoundaryEngineInput> inputs = List.of(
                input(context, SignalObservation.numeric(
                        "EVT-END-FLOW", "feed.flow", new BigDecimal("0.1"), SignalQuality.GOOD, T0)),
                input(context, SignalObservation.bool(
                        "EVT-END-PUMP", "feed.pump", false, SignalQuality.GOOD, T0.plusSeconds(1))));

        BatchCandidateV1 candidate = BoundaryReplayEngine.replay(
                rule, inputs, T0.plusSeconds(21)).get(0);

        assertEquals(BoundaryType.END, candidate.getBoundaryType());
        assertEquals("BATCH-S07-0001", candidate.getBatchId());
        assertEquals("EVT-END-FLOW", candidate.getFirstQuorumEvidenceEventId());
        assertEquals(T0.plusSeconds(21).toEpochMilli(), candidate.getEmittedAtMs());
        assertTrue(BpiContractValidator.validate(candidate).isEmpty());
    }

    @Test
    void rejectsFinalWatermarkBeforeLatestObservation() {
        BoundaryExecutionContext context = new BoundaryExecutionContext(
                "TENANT-A", "PLANT-01", "LINE-S07-01", "S07-FEED",
                "TOPO-S07", "3", "MO-20260712-001", null);
        List<BoundaryEngineInput> inputs = List.of(input(
                context,
                SignalObservation.bool(
                        "EVT-ORDER", "order.active", true, SignalQuality.GOOD, T0.plusSeconds(10))));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> BoundaryReplayEngine.replay(startRule(), inputs, T0.plusSeconds(9)));

        assertEquals("finalWatermark cannot precede the latest event time", error.getMessage());
    }

    private static BoundaryEngineInput input(
            BoundaryExecutionContext context, SignalObservation observation) {
        return new BoundaryEngineInput(context, observation);
    }

    private static BoundaryRuleDefinition startRule() {
        return new BoundaryRuleDefinition(
                "S07-FEED-START", "1.0.0", BoundaryKind.START, 2, 0.85, 0.8,
                List.of(
                        condition("order.active", ConditionOperator.EQUALS_TRUE, null, 0,
                                EvidenceClass.REQUIRED, 40),
                        condition("feed.pump", ConditionOperator.EQUALS_TRUE, null, 3,
                                EvidenceClass.QUORUM, 20),
                        condition("feed.flow", ConditionOperator.GREATER_THAN, "2.0", 10,
                                EvidenceClass.QUORUM, 30),
                        condition("column.level", ConditionOperator.RISING, "0.1", 15,
                                EvidenceClass.OPTIONAL, 10)));
    }

    private static EvidenceCondition condition(
            String signal, ConditionOperator operator, String threshold, long holdSeconds,
            EvidenceClass classification, int weight) {
        return new EvidenceCondition(
                signal, operator, threshold == null ? null : new BigDecimal(threshold),
                Duration.ofSeconds(holdSeconds), Duration.ofSeconds(30), classification, weight);
    }
}
