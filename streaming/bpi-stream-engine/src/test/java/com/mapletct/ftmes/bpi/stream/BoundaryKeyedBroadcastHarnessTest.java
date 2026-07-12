package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.contract.validation.BpiContractValidator;
import com.mapletct.ftmes.bpi.contract.v1.BatchCandidateV1;
import com.mapletct.ftmes.bpi.rules.BoundaryKind;
import com.mapletct.ftmes.bpi.rules.BoundaryRuleDefinition;
import com.mapletct.ftmes.bpi.rules.BoundaryTimingPolicy;
import com.mapletct.ftmes.bpi.rules.ConditionOperator;
import com.mapletct.ftmes.bpi.rules.EvidenceClass;
import com.mapletct.ftmes.bpi.rules.EvidenceCondition;
import com.mapletct.ftmes.bpi.rules.SignalObservation;
import com.mapletct.ftmes.bpi.rules.SignalQuality;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.runtime.checkpoint.OperatorSubtaskState;
import org.apache.flink.streaming.api.operators.co.CoBroadcastWithKeyedOperator;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.streaming.util.KeyedBroadcastOperatorTestHarness;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundaryKeyedBroadcastHarnessTest {

    private static final Instant T0 = Instant.parse("2026-07-12T08:00:00Z");

    @Test
    void broadcastRuleAndEventTimeTimerProduceOneCandidate() throws Exception {
        try (KeyedBroadcastOperatorTestHarness<String, BoundaryStreamInput, BoundaryRuleUpdate, byte[]>
                     harness = harness()) {
            harness.open();
            harness.processBroadcastElement(BoundaryRuleUpdate.upsert(rule()), T0.toEpochMilli());
            BoundaryExecutionContext context = context();
            harness.processElement(input(
                    context,
                    SignalObservation.bool("ORDER", "order.active", true, SignalQuality.GOOD, T0)),
                    T0.toEpochMilli());
            harness.processElement(input(
                    context,
                    SignalObservation.numeric(
                            "FLOW", "feed.flow", new BigDecimal("3"), SignalQuality.GOOD, T0.plusSeconds(1))),
                    T0.plusSeconds(1).toEpochMilli());

            harness.watermark(T0.plusSeconds(11).toEpochMilli());
            List<BatchCandidateV1> candidates = candidates(harness.getOutput());

            assertEquals(1, candidates.size());
            assertEquals("MO-1", candidates.get(0).getContextOrderId());
            assertTrue(BpiContractValidator.validate(candidates.get(0)).isEmpty());

            harness.watermark(T0.plusSeconds(20).toEpochMilli());
            assertEquals(1, candidates(harness.getOutput()).size());
        }
    }

    @Test
    void checkpointRestorePreservesWindowBroadcastRuleAndTimer() throws Exception {
        OperatorSubtaskState snapshot;
        BoundaryExecutionContext context = context();
        try (KeyedBroadcastOperatorTestHarness<String, BoundaryStreamInput, BoundaryRuleUpdate, byte[]>
                     first = harness()) {
            first.open();
            first.processBroadcastElement(BoundaryRuleUpdate.upsert(rule()), T0.toEpochMilli());
            first.processElement(input(
                    context,
                    SignalObservation.bool("ORDER", "order.active", true, SignalQuality.GOOD, T0)),
                    T0.toEpochMilli());
            first.processElement(input(
                    context,
                    SignalObservation.numeric(
                            "FLOW", "feed.flow", new BigDecimal("3"), SignalQuality.GOOD, T0.plusSeconds(1))),
                    T0.plusSeconds(1).toEpochMilli());
            snapshot = first.snapshot(1, T0.plusSeconds(2).toEpochMilli());
        }

        try (KeyedBroadcastOperatorTestHarness<String, BoundaryStreamInput, BoundaryRuleUpdate, byte[]>
                     restored = harness()) {
            restored.initializeState(snapshot);
            restored.open();
            restored.watermark(T0.plusSeconds(11).toEpochMilli());

            assertEquals(1, candidates(restored.getOutput()).size());
        }
    }

    @Test
    void missingRuleGoesToIssueSideOutputWithoutCandidate() throws Exception {
        try (KeyedBroadcastOperatorTestHarness<String, BoundaryStreamInput, BoundaryRuleUpdate, byte[]>
                     harness = harness()) {
            harness.open();
            BoundaryStreamInput input = input(
                    context(),
                    SignalObservation.bool("ORDER", "order.active", true, SignalQuality.GOOD, T0));

            harness.processElement(input, T0.toEpochMilli());

            assertTrue(candidates(harness.getOutput()).isEmpty());
            ConcurrentLinkedQueue<StreamRecord<BoundaryProcessingIssue>> issues =
                    harness.getSideOutput(BoundaryKeyedBroadcastFunction.ISSUES);
            assertEquals(1, issues.size());
            assertEquals("RULE_NOT_FOUND", issues.peek().getValue().code());
        }
    }

    @Test
    void missingStartOrderIdentityGoesToIssueStreamBeforeEvaluation() throws Exception {
        try (KeyedBroadcastOperatorTestHarness<String, BoundaryStreamInput, BoundaryRuleUpdate, byte[]>
                     harness = harness()) {
            harness.open();
            harness.processBroadcastElement(BoundaryRuleUpdate.upsert(rule()), T0.toEpochMilli());
            BoundaryExecutionContext context = new BoundaryExecutionContext(
                    "TENANT-A", "PLANT-01", "LINE-01", "FEED", "TOPO-1", "7", null, null);
            BoundaryStreamInput input = input(
                    context,
                    SignalObservation.bool("ORDER", "order.active", true, SignalQuality.GOOD, T0));

            harness.processElement(input, T0.toEpochMilli());

            assertTrue(candidates(harness.getOutput()).isEmpty());
            assertEquals(
                    "CONTEXT_ID_MISSING",
                    harness.getSideOutput(BoundaryKeyedBroadcastFunction.ISSUES).peek().getValue().code());
        }
    }

    @Test
    void sameRuleVersionCannotBeOverwrittenWithDifferentSemantics() throws Exception {
        try (KeyedBroadcastOperatorTestHarness<String, BoundaryStreamInput, BoundaryRuleUpdate, byte[]>
                     harness = harness()) {
            harness.open();
            harness.processBroadcastElement(BoundaryRuleUpdate.upsert(rule()), T0.toEpochMilli());
            BoundaryRuleDefinition conflicting = new BoundaryRuleDefinition(
                    "START-01", "1", BoundaryKind.START, 1, 1.0, 0,
                    List.of(
                            new EvidenceCondition(
                                    "order.active", ConditionOperator.EQUALS_TRUE, null,
                                    Duration.ZERO, Duration.ofSeconds(30), EvidenceClass.REQUIRED, 50),
                            new EvidenceCondition(
                                    "feed.flow", ConditionOperator.GREATER_THAN, new BigDecimal("99"),
                                    Duration.ofSeconds(10), Duration.ofSeconds(30), EvidenceClass.QUORUM, 50)));

            harness.processBroadcastElement(
                    BoundaryRuleUpdate.upsert(conflicting), T0.plusSeconds(1).toEpochMilli());

            assertEquals(
                    "RULE_VERSION_CONFLICT",
                    harness.getSideOutput(BoundaryKeyedBroadcastFunction.ISSUES).peek().getValue().code());
            assertEquals(
                    rule(),
                    BoundaryRuleCodec.decode(harness.getBroadcastState(
                            BoundaryKeyedBroadcastFunction.RULES).get("START-01|1")));
        }
    }

    @Test
    void lateEventWithinAllowedLatenessRecomputesTheOpenWindowInEventTimeOrder() throws Exception {
        try (KeyedBroadcastOperatorTestHarness<String, BoundaryStreamInput, BoundaryRuleUpdate, byte[]>
                     harness = harness()) {
            harness.open();
            BoundaryRuleDefinition timedRule = timedRule();
            harness.processBroadcastElement(BoundaryRuleUpdate.upsert(timedRule), T0.toEpochMilli());
            harness.processElement(
                    input(timedRule, SignalObservation.bool(
                            "ORDER", "order.active", true, SignalQuality.GOOD, T0)),
                    T0.toEpochMilli());
            harness.processElement(
                    input(timedRule, SignalObservation.numeric(
                            "FLOW-STOP", "feed.flow", BigDecimal.ZERO,
                            SignalQuality.GOOD, T0.plusSeconds(20))),
                    T0.plusSeconds(20).toEpochMilli());
            harness.watermark(T0.plusSeconds(25).toEpochMilli());

            harness.processElement(
                    input(timedRule, SignalObservation.numeric(
                            "FLOW-LATE", "feed.flow", new BigDecimal("3"),
                            SignalQuality.GOOD, T0.plusSeconds(1))),
                    T0.plusSeconds(1).toEpochMilli());

            List<BatchCandidateV1> candidates = candidates(harness.getOutput());
            assertEquals(1, candidates.size());
            assertEquals(T0.plusSeconds(11).toEpochMilli(), candidates.get(0).getBoundaryEventTimeMs());
            assertEquals(List.of("ORDER", "FLOW-LATE"), candidates.get(0).getEvidenceEventIdsList());
            assertTrue(issueCodes(harness).isEmpty());

            harness.processElement(
                    input(timedRule, SignalObservation.numeric(
                            "FLOW-LATE", "feed.flow", new BigDecimal("3"),
                            SignalQuality.GOOD, T0.plusSeconds(1))),
                    T0.plusSeconds(1).toEpochMilli());
            assertEquals(1, candidates(harness.getOutput()).size());
            assertTrue(issueCodes(harness).isEmpty());
        }
    }

    @Test
    void lateEventBeyondAllowedLatenessRequiresRevision() throws Exception {
        try (KeyedBroadcastOperatorTestHarness<String, BoundaryStreamInput, BoundaryRuleUpdate, byte[]>
                     harness = harness()) {
            harness.open();
            BoundaryRuleDefinition timedRule = timedRule();
            harness.processBroadcastElement(BoundaryRuleUpdate.upsert(timedRule), T0.toEpochMilli());
            harness.watermark(T0.plusSeconds(60).toEpochMilli());

            harness.processElement(
                    input(timedRule, SignalObservation.bool(
                            "LATE-BEYOND", "order.active", true, SignalQuality.GOOD, T0.plusSeconds(20))),
                    T0.plusSeconds(20).toEpochMilli());

            assertEquals(List.of("LATE_EVENT_REVISION_REQUIRED"), issueCodes(harness));
            assertTrue(candidates(harness.getOutput()).isEmpty());
        }
    }

    @Test
    void malformedLateObservationGoesToIssueStreamWithoutFailingTheOperator() throws Exception {
        try (KeyedBroadcastOperatorTestHarness<String, BoundaryStreamInput, BoundaryRuleUpdate, byte[]>
                     harness = harness()) {
            harness.open();
            BoundaryRuleDefinition timedRule = timedRule();
            harness.processBroadcastElement(BoundaryRuleUpdate.upsert(timedRule), T0.toEpochMilli());
            harness.watermark(T0.plusSeconds(5).toEpochMilli());

            harness.processElement(
                    input(timedRule, SignalObservation.bool(
                            "BAD-LATE", "feed.flow", true, SignalQuality.GOOD, T0.plusSeconds(1))),
                    T0.plusSeconds(1).toEpochMilli());

            assertTrue(candidates(harness.getOutput()).isEmpty());
            assertEquals(List.of("EVALUATION_REJECTED"), issueCodes(harness));
        }
    }

    @Test
    void emittedCandidateIsImmutableWhenMoreLateEvidenceArrives() throws Exception {
        try (KeyedBroadcastOperatorTestHarness<String, BoundaryStreamInput, BoundaryRuleUpdate, byte[]>
                     harness = harness()) {
            harness.open();
            BoundaryRuleDefinition timedRule = timedRule();
            harness.processBroadcastElement(BoundaryRuleUpdate.upsert(timedRule), T0.toEpochMilli());
            harness.processElement(
                    input(timedRule, SignalObservation.bool(
                            "ORDER", "order.active", true, SignalQuality.GOOD, T0)),
                    T0.toEpochMilli());
            harness.processElement(
                    input(timedRule, SignalObservation.numeric(
                            "FLOW", "feed.flow", new BigDecimal("3"),
                            SignalQuality.GOOD, T0.plusSeconds(1))),
                    T0.plusSeconds(1).toEpochMilli());
            harness.watermark(T0.plusSeconds(11).toEpochMilli());
            assertEquals(1, candidates(harness.getOutput()).size());

            harness.processElement(
                    input(timedRule, SignalObservation.numeric(
                            "FLOW-CORRECTION", "feed.flow", new BigDecimal("4"),
                            SignalQuality.GOOD, T0.plusSeconds(5))),
                    T0.plusSeconds(5).toEpochMilli());

            assertEquals(1, candidates(harness.getOutput()).size());
            assertEquals(List.of("LATE_EVENT_REVISION_REQUIRED"), issueCodes(harness));
        }
    }

    @Test
    void checkpointRestorePreservesObservationHistoryForLateRecomputation() throws Exception {
        OperatorSubtaskState snapshot;
        BoundaryRuleDefinition timedRule = timedRule();
        try (KeyedBroadcastOperatorTestHarness<String, BoundaryStreamInput, BoundaryRuleUpdate, byte[]>
                     first = harness()) {
            first.open();
            first.processBroadcastElement(BoundaryRuleUpdate.upsert(timedRule), T0.toEpochMilli());
            first.processElement(
                    input(timedRule, SignalObservation.bool(
                            "ORDER", "order.active", true, SignalQuality.GOOD, T0)),
                    T0.toEpochMilli());
            snapshot = first.snapshot(2, T0.plusSeconds(1).toEpochMilli());
        }

        try (KeyedBroadcastOperatorTestHarness<String, BoundaryStreamInput, BoundaryRuleUpdate, byte[]>
                     restored = harness()) {
            restored.initializeState(snapshot);
            restored.open();
            restored.watermark(T0.plusSeconds(15).toEpochMilli());
            restored.processElement(
                    input(timedRule, SignalObservation.numeric(
                            "FLOW-LATE", "feed.flow", new BigDecimal("3"),
                            SignalQuality.GOOD, T0.plusSeconds(1))),
                    T0.plusSeconds(1).toEpochMilli());

            assertEquals(1, candidates(restored.getOutput()).size());
            assertTrue(issueCodes(restored).isEmpty());
        }
    }

    private static KeyedBroadcastOperatorTestHarness<
            String,
            BoundaryStreamInput,
            BoundaryRuleUpdate,
            byte[]> harness() throws Exception {
        CoBroadcastWithKeyedOperator<String, BoundaryStreamInput, BoundaryRuleUpdate, byte[]> operator =
                new CoBroadcastWithKeyedOperator<>(
                        new BoundaryKeyedBroadcastFunction(),
                        List.of(BoundaryKeyedBroadcastFunction.RULES));
        return new KeyedBroadcastOperatorTestHarness<>(
                operator,
                BoundaryStreamInput::keyedLocality,
                TypeInformation.of(String.class),
                1,
                1,
                0);
    }

    private static BoundaryExecutionContext context() {
        return new BoundaryExecutionContext(
                "TENANT-A", "PLANT-01", "LINE-01", "FEED", "TOPO-1", "7", "MO-1", null);
    }

    private static BoundaryStreamInput input(
            BoundaryExecutionContext context,
            SignalObservation observation) {
        return new BoundaryStreamInput(
                context,
                new BoundaryRuleRef("START-01", "1"),
                BoundaryKind.START,
                observation);
    }

    private static BoundaryStreamInput input(
            BoundaryRuleDefinition rule,
            SignalObservation observation) {
        return new BoundaryStreamInput(
                context(),
                new BoundaryRuleRef(rule.ruleCode(), rule.ruleVersion()),
                rule.boundaryKind(),
                observation);
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

    private static BoundaryRuleDefinition timedRule() {
        return new BoundaryRuleDefinition(
                "START-01", "2", BoundaryKind.START, 1, 1.0, 0,
                new BoundaryTimingPolicy(
                        Duration.ofSeconds(30), Duration.ofSeconds(5), Duration.ofMinutes(2)),
                rule().conditions());
    }

    private static List<String> issueCodes(KeyedBroadcastOperatorTestHarness<
            String, BoundaryStreamInput, BoundaryRuleUpdate, byte[]> harness) {
        ConcurrentLinkedQueue<StreamRecord<BoundaryProcessingIssue>> issues =
                harness.getSideOutput(BoundaryKeyedBroadcastFunction.ISSUES);
        if (issues == null) {
            return List.of();
        }
        return issues.stream()
                .map(StreamRecord::getValue)
                .map(BoundaryProcessingIssue::code)
                .toList();
    }

    private static List<BatchCandidateV1> candidates(ConcurrentLinkedQueue<Object> output) {
        return output.stream()
                .filter(StreamRecord.class::isInstance)
                .map(StreamRecord.class::cast)
                .map(StreamRecord::getValue)
                .filter(byte[].class::isInstance)
                .map(byte[].class::cast)
                .map(BoundaryKeyedBroadcastHarnessTest::parseCandidate)
                .toList();
    }

    private static BatchCandidateV1 parseCandidate(byte[] payload) {
        try {
            return BatchCandidateV1.parseFrom(payload);
        } catch (com.google.protobuf.InvalidProtocolBufferException error) {
            throw new IllegalStateException("operator emitted invalid BatchCandidateV1 bytes", error);
        }
    }
}
