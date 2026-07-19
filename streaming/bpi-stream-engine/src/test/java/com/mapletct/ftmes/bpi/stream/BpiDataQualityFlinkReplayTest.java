package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.contract.v1.DataQualityEventV1;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.api.operators.KeyedProcessOperator;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.streaming.util.KeyedOneInputStreamOperatorTestHarness;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BpiDataQualityFlinkReplayTest {

    @Test
    void defaultConfigIsFailClosedAndUsesDedicatedConsumerIdentity() {
        BpiDataQualityFlinkReplayConfig config = BpiDataQualityFlinkReplayConfig.fromEnvironment(
                Map.of("BPI_KAFKA_BOOTSTRAP_SERVERS", "kafka-1:19092"));

        assertTrue(config.marker().startsWith("ADP_E2E_DQ_FLINK_"));
        assertEquals(Duration.ofMinutes(3), config.timeout());
        assertEquals(Duration.ofSeconds(2), config.contextSettle());
        assertEquals("ft-mes-bpi-dq-acceptance-" + config.marker(), config.consumerGroup());

        Map<String, String> invalid = new HashMap<>();
        invalid.put("BPI_KAFKA_BOOTSTRAP_SERVERS", "kafka-1:19092");
        invalid.put("BPI_DQ_REPLAY_REPORT", "relative.json");
        assertThrows(IllegalArgumentException.class,
                () -> BpiDataQualityFlinkReplayConfig.fromEnvironment(invalid));
    }

    @Test
    void replayScenarioProducesOnlyTheFourExpectedAutomaticEvents() throws Exception {
        String marker = "ADP_E2E_DQ_FLINK_SCENARIO";
        BpiDataQualityFlinkReplay.Scenario scenario = BpiDataQualityFlinkReplay.scenario(
                marker, Instant.parse("2026-07-19T12:00:00Z"));
        try (Harness harness = new Harness(new KeyedProcessOperator<>(
                new TelemetryDataQualityFunction(Duration.ofMinutes(5), Duration.ofDays(7))))) {
            harness.open();
            for (var telemetry : scenario.telemetry()) {
                harness.processElement(telemetry.toByteArray(), telemetry.getEventTimeMs());
            }

            assertEquals(
                    List.of(
                            "SOURCE_SEQUENCE_GAP",
                            "CLOCK_DRIFT",
                            "POINT_QUALITY_BAD",
                            "SOURCE_SEQUENCE_DUPLICATE"),
                    harness.getOutput().stream()
                            .filter(StreamRecord.class::isInstance)
                            .map(StreamRecord.class::cast)
                            .map(StreamRecord::getValue)
                            .filter(byte[].class::isInstance)
                            .map(byte[].class::cast)
                            .map(BpiDataQualityFlinkReplayTest::parse)
                            .map(DataQualityEventV1::getIssueCode)
                            .toList());
            assertEquals(marker + "-CONTEXT-ACTIVE", scenario.activeContext().getEventId());
            assertEquals(marker + "-CONTEXT-INACTIVE", scenario.inactiveContext().getEventId());
        }
    }

    private static DataQualityEventV1 parse(byte[] bytes) {
        try {
            return DataQualityEventV1.parseFrom(bytes);
        } catch (com.google.protobuf.InvalidProtocolBufferException error) {
            throw new IllegalStateException(error);
        }
    }

    private static final class Harness extends KeyedOneInputStreamOperatorTestHarness<
            String, byte[], byte[]> {

        private Harness(KeyedProcessOperator<String, byte[], byte[]> operator) throws Exception {
            super(
                    operator,
                    TelemetryDataQualityFunction::sourceKey,
                    TypeInformation.of(String.class),
                    1,
                    1,
                    0);
        }
    }
}
