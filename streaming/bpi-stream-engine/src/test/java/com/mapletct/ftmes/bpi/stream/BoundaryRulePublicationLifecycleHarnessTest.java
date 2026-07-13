package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.contract.v1.BoundaryRulePublicationV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleApplicationV1;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.runtime.checkpoint.OperatorSubtaskState;
import org.apache.flink.streaming.api.operators.KeyedProcessOperator;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.streaming.util.KeyedOneInputStreamOperatorTestHarness;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BoundaryRulePublicationLifecycleHarnessTest {

    private static final Instant T0 = Instant.parse("2026-07-12T08:00:00Z");

    @Test
    void activeCanBecomeInactiveButSameVersionCannotReactivate() throws Exception {
        try (Harness harness = harness()) {
            harness.open();
            BoundaryRulePublicationV1 active = publication(true, T0, "RULE-ON");
            BoundaryRulePublicationV1 inactive = publication(false, T0.plusSeconds(1), "RULE-OFF");
            BoundaryRulePublicationV1 reactivated = publication(true, T0.plusSeconds(2), "RULE-ON-AGAIN");

            harness.processElement(active.toByteArray(), active.getPublishedAtMs());
            harness.processElement(inactive.toByteArray(), inactive.getPublishedAtMs());
            harness.processElement(reactivated.toByteArray(), reactivated.getPublishedAtMs());

            assertEquals(List.of("RULE-ON", "RULE-OFF"), eventIds(harness));
            assertEquals(
                    "RULE_REACTIVATION_REQUIRES_NEW_VERSION",
                    harness.getSideOutput(BoundaryRulePublicationLifecycleFunction.ISSUES)
                            .peek().getValue().code());
        }
    }

    @Test
    void checkpointRestoreKeepsTerminalInactiveLifecycle() throws Exception {
        OperatorSubtaskState snapshot;
        try (Harness first = harness()) {
            first.open();
            BoundaryRulePublicationV1 active = publication(true, T0, "RULE-ON");
            BoundaryRulePublicationV1 inactive = publication(false, T0.plusSeconds(1), "RULE-OFF");
            first.processElement(active.toByteArray(), active.getPublishedAtMs());
            first.processElement(inactive.toByteArray(), inactive.getPublishedAtMs());
            snapshot = first.snapshot(1, T0.plusSeconds(1).toEpochMilli());
        }

        try (Harness restored = harness()) {
            restored.initializeState(snapshot);
            restored.open();
            BoundaryRulePublicationV1 reactivated = publication(
                    true, T0.plusSeconds(2), "RULE-ON-AGAIN");
            restored.processElement(reactivated.toByteArray(), reactivated.getPublishedAtMs());

            assertEquals(List.of(), eventIds(restored));
            assertEquals(
                    "RULE_REACTIVATION_REQUIRES_NEW_VERSION",
                    restored.getSideOutput(BoundaryRulePublicationLifecycleFunction.ISSUES)
                            .peek().getValue().code());
        }
    }

    @Test
    void changedSemanticsAndOlderLifecycleEventAreRejected() throws Exception {
        try (Harness harness = harness()) {
            harness.open();
            BoundaryRulePublicationV1 active = publication(true, T0.plusSeconds(2), "RULE-ON");
            BoundaryRulePublicationV1 older = publication(false, T0.plusSeconds(1), "RULE-OLD");
            BoundaryRulePublicationV1 conflict = active.toBuilder()
                    .setEventId("RULE-CONFLICT")
                    .setChecksum("sha:changed")
                    .setPublishedAtMs(T0.plusSeconds(3).toEpochMilli())
                    .build();
            harness.processElement(active.toByteArray(), active.getPublishedAtMs());
            harness.processElement(older.toByteArray(), older.getPublishedAtMs());
            harness.processElement(conflict.toByteArray(), conflict.getPublishedAtMs());

            assertEquals(
                    List.of("RULE_PUBLICATION_OUT_OF_ORDER", "RULE_VERSION_CONFLICT"),
                    harness.getSideOutput(BoundaryRulePublicationLifecycleFunction.ISSUES).stream()
                            .map(StreamRecord::getValue)
                            .map(BoundaryRoutingIssue::code)
                            .toList());
        }
    }

    @Test
    void ruleWindowMustFitInsideConfiguredKeyedStateTtl() throws Exception {
        try (Harness harness = harness(Duration.ofMinutes(2))) {
            harness.open();
            BoundaryRulePublicationV1 active = publication(true, T0, "RULE-TOO-LONG");

            harness.processElement(active.toByteArray(), active.getPublishedAtMs());

            assertEquals(List.of(), eventIds(harness));
            assertEquals(
                    "RULE_WINDOW_EXCEEDS_STATE_TTL",
                    harness.getSideOutput(BoundaryRulePublicationLifecycleFunction.ISSUES)
                            .peek().getValue().code());
        }
    }

    @Test
    void acceptedDuplicateAndRejectedConflictProduceCheckpointedApplicationOutcomes() throws Exception {
        try (Harness harness = harness()) {
            harness.open();
            harness.setProcessingTime(T0.toEpochMilli());
            BoundaryRulePublicationV1 active = publication(true, T0, "RULE-ON");
            BoundaryRulePublicationV1 conflict = active.toBuilder()
                    .setEventId("RULE-CONFLICT")
                    .setChecksum("sha:changed")
                    .setPublishedAtMs(T0.plusSeconds(1).toEpochMilli())
                    .build();

            harness.processElement(active.toByteArray(), active.getPublishedAtMs());
            harness.processElement(active.toByteArray(), active.getPublishedAtMs());
            harness.processElement(conflict.toByteArray(), conflict.getPublishedAtMs());

            assertEquals(
                    List.of("APPLIED:RULE-ON", "APPLIED:RULE-ON", "REJECTED:RULE-CONFLICT"),
                    applications(harness));
        }
    }

    private static BoundaryRulePublicationV1 publication(
            boolean active,
            Instant publishedAt,
            String eventId) {
        return BoundaryRuleRoutingBroadcastHarnessTest
                .publication("TENANT-A", "RULE-A", active, "sha:a")
                .toBuilder()
                .setEventId(eventId)
                .setPublishedAtMs(publishedAt.toEpochMilli())
                .build();
    }

    private static Harness harness() throws Exception {
        return harness(Duration.ofDays(30));
    }

    private static Harness harness(Duration stateTtl) throws Exception {
        return new Harness(new KeyedProcessOperator<>(
                new BoundaryRulePublicationLifecycleFunction(stateTtl)));
    }

    private static List<String> eventIds(Harness harness) {
        return harness.getOutput().stream()
                .filter(StreamRecord.class::isInstance)
                .map(StreamRecord.class::cast)
                .map(StreamRecord::getValue)
                .filter(byte[].class::isInstance)
                .map(byte[].class::cast)
                .map(BoundaryRulePublicationLifecycleHarnessTest::eventId)
                .toList();
    }

    private static List<String> applications(Harness harness) {
        return harness.getSideOutput(BoundaryRulePublicationLifecycleFunction.APPLICATIONS).stream()
                .map(StreamRecord::getValue)
                .map(BoundaryRulePublicationLifecycleHarnessTest::application)
                .map(value -> value.getStatus().name() + ":" + value.getPublicationEventId())
                .toList();
    }

    private static BoundaryRuleApplicationV1 application(byte[] bytes) {
        try {
            return BoundaryRuleApplicationV1.parseFrom(bytes);
        } catch (com.google.protobuf.InvalidProtocolBufferException error) {
            throw new IllegalStateException(error);
        }
    }

    private static String eventId(byte[] bytes) {
        try {
            return BoundaryRulePublicationV1.parseFrom(bytes).getEventId();
        } catch (com.google.protobuf.InvalidProtocolBufferException error) {
            throw new IllegalStateException(error);
        }
    }

    private static final class Harness extends KeyedOneInputStreamOperatorTestHarness<
            String, byte[], byte[]> {

        private Harness(KeyedProcessOperator<String, byte[], byte[]> operator) throws Exception {
            super(
                    operator,
                    bytes -> BoundaryRulePublicationSemantics.key(
                            BoundaryRulePublicationV1.parseFrom(bytes)),
                    TypeInformation.of(String.class),
                    1,
                    1,
                    0);
        }
    }
}
