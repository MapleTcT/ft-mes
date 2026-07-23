package com.mapletct.ftmes.bpi.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.domain.ProcessSignalWindowDefinition;
import com.mapletct.ftmes.bpi.domain.ProcessSignalWindowEvidence;
import com.mapletct.ftmes.bpi.domain.ProcessSignalWindowFact;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessSignalWindowBuilderTest {
    private static final Instant PREDICTION = Instant.parse("2026-07-23T00:01:00Z");
    private final ProcessSignalWindowBuilder builder = new ProcessSignalWindowBuilder(
            new CanonicalJson(new ObjectMapper().findAndRegisterModules()));

    @Test
    void buildsDeterministicReadyNumericWindow() {
        ProcessSignalWindowEvidence evidence = evidence(
                numericDefinition(), 3, 3, 0, 0, 0, 0, 0,
                new BigDecimal("20.000000"), new BigDecimal("12.500000"),
                new BigDecimal("10.000000"), new BigDecimal("15.000000"),
                new BigDecimal("10.000000"), new BigDecimal("15.000000"),
                new BigDecimal("0.250000"), null);

        ProcessSignalWindowFact first = builder.build(evidence);
        ProcessSignalWindowFact second = builder.build(evidence);

        assertThat(first.state()).isEqualTo("READY");
        assertThat(first.numericValue()).isEqualByComparingTo("12.500000");
        assertThat(first.blockerCodes()).isEmpty();
        assertThat(first.factChecksum()).isEqualTo(second.factChecksum()).hasSize(64);
        assertThat(first.evidencePayload())
                .containsEntry("acceptedSampleCount", 3)
                .containsEntry("sourceFingerprint", "f".repeat(32));
    }

    @Test
    void failsClosedForCoverageUnitTypeAndCalibrationProblems() {
        ProcessSignalWindowFact fact = builder.build(evidence(
                numericDefinition(), 5, 1, 1, 1, 1, 1, 1,
                new BigDecimal("75.000000"), new BigDecimal("12.500000"),
                new BigDecimal("10.000000"), new BigDecimal("15.000000"),
                new BigDecimal("10.000000"), new BigDecimal("15.000000"),
                null, null));

        assertThat(fact.state()).isEqualTo("BLOCKED");
        assertThat(fact.blockerCodes())
                .contains("WINDOW_SAMPLE_COUNT_BELOW_MINIMUM")
                .contains("WINDOW_MAX_GAP_EXCEEDED")
                .contains("WINDOW_UNIT_MISMATCH")
                .contains("WINDOW_VALUE_TYPE_MISMATCH")
                .contains("WINDOW_CALIBRATION_MISMATCH");
        assertThat(fact.factChecksum()).hasSize(64);
    }

    @Test
    void computesBooleanTrueRatioWithoutCalibrationRequirement() {
        ProcessSignalWindowDefinition definition = new ProcessSignalWindowDefinition(
                "process.window.feed_pump.true_ratio_30s", "feed.pump",
                "BOOLEAN", "TRUE_RATIO", -30, 0, 2, 30,
                "state", false, List.of("GOOD"), "b".repeat(64));
        ProcessSignalWindowFact fact = builder.build(evidence(
                definition, 4, 4, 0, 0, 0, 0, 0,
                new BigDecimal("10.000000"), null, null, null,
                null, null, null, new BigDecimal("0.750000")));

        assertThat(fact.state()).isEqualTo("READY");
        assertThat(fact.numericValue()).isEqualByComparingTo("0.750000");
    }

    private ProcessSignalWindowDefinition numericDefinition() {
        return new ProcessSignalWindowDefinition(
                "process.window.feed_flow.mean_60s", "feed.flow",
                "NUMERIC", "MEAN", -60, 0, 2, 30,
                "t/h", true, List.of("GOOD"), "a".repeat(64));
    }

    private ProcessSignalWindowEvidence evidence(
            ProcessSignalWindowDefinition definition,
            int sourcePointCount,
            int acceptedSampleCount,
            int rejectedQualityCount,
            int lateAvailabilityCount,
            int unitMismatchCount,
            int typeMismatchCount,
            int calibrationMismatchCount,
            BigDecimal maximumGap,
            BigDecimal mean,
            BigDecimal minimum,
            BigDecimal maximum,
            BigDecimal first,
            BigDecimal last,
            BigDecimal slope,
            BigDecimal trueRatio) {
        return new ProcessSignalWindowEvidence(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                "BATCH-01", "PLANT-01", "LINE-01",
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                UUID.fromString("55555555-5555-5555-5555-555555555555"),
                UUID.fromString("66666666-6666-6666-6666-666666666666"),
                definition, PREDICTION,
                PREDICTION.plusSeconds(definition.startOffsetSeconds()),
                PREDICTION.plusSeconds(definition.endOffsetSeconds()),
                1, definition.expectedUnit(), "CAL-1",
                "PRODUCT-01", "DEVICE-01", definition.signal(),
                definition.expectedUnit(), "CAL-1", "ACTIVE", true, true,
                definition.requireCalibration() ? "VERIFIED" : "MISSING",
                sourcePointCount, acceptedSampleCount, rejectedQualityCount,
                lateAvailabilityCount, unitMismatchCount, typeMismatchCount,
                calibrationMismatchCount,
                acceptedSampleCount == 0 ? null : PREDICTION.plusSeconds(
                        definition.startOffsetSeconds()),
                acceptedSampleCount == 0 ? null : PREDICTION.plusSeconds(
                        definition.endOffsetSeconds()),
                acceptedSampleCount == 0 ? null : PREDICTION.minusSeconds(1),
                maximumGap, mean, minimum, maximum, first, last, slope, trueRatio,
                "f".repeat(32));
    }
}
