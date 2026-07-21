package com.mapletct.ftmes.bpi.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.application.error.BpiValidationException;
import com.mapletct.ftmes.bpi.domain.DatasetManifestBuild;
import com.mapletct.ftmes.bpi.domain.DatasetManifestClaim;
import com.mapletct.ftmes.bpi.domain.DatasetSampleSource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatasetManifestBuilderTest {
    private final DatasetManifestBuilder builder = new DatasetManifestBuilder(
            new CanonicalJson(new ObjectMapper().findAndRegisterModules()));

    @Test
    void sameControlledInputsProduceSameChecksumAndExcludeLeakageAndLowConfidence() {
        Instant freezeAt = Instant.parse("2026-07-20T00:00:00Z");
        UUID datasetId = UUID.randomUUID();
        UUID ruleId = UUID.randomUUID();
        DatasetManifestClaim firstClaim = claim(
                UUID.randomUUID(), UUID.randomUUID(), datasetId, ruleId, freezeAt, true, 24);
        DatasetManifestClaim secondClaim = claim(
                UUID.randomUUID(), UUID.randomUUID(), datasetId, ruleId, freezeAt, true, 24);

        DatasetSampleSource high = source(
                "BATCH-HIGH", ruleId, freezeAt.minusSeconds(7_200),
                freezeAt.minusSeconds(3_600), true, true, true);
        DatasetSampleSource low = source(
                "BATCH-LOW", ruleId, freezeAt.minusSeconds(10_800),
                freezeAt.minusSeconds(7_200), false, true, true);
        DatasetSampleSource delayed = source(
                "BATCH-DELAYED", ruleId, freezeAt.minusSeconds(172_800),
                freezeAt.minusSeconds(3_600), true, true, true);

        DatasetManifestBuild first = builder.build(firstClaim, List.of(low, delayed, high));
        DatasetManifestBuild second = builder.build(secondClaim, List.of(high, low, delayed));

        assertThat(first.manifestChecksum()).isEqualTo(second.manifestChecksum()).hasSize(64);
        assertThat(first.includedCount()).isEqualTo(1);
        assertThat(first.excludedCount()).isEqualTo(2);
        assertThat(first.exclusionSummary()).containsEntry("CONFIDENCE_BELOW_THRESHOLD", 1)
                .containsEntry("START_BOUNDARY_OUTSIDE_TOLERANCE", 1)
                .containsEntry("LABEL_DELAY_EXCEEDED", 1);
        assertThat(first.samples()).extracting(item -> item.batchNo())
                .containsExactly("BATCH-DELAYED", "BATCH-LOW", "BATCH-HIGH");
        assertThat(first.samples().get(2).featureCutoff())
                .isEqualTo(first.samples().get(2).predictionTime());
        assertThat(first.samples().get(2).featurePayload())
                .doesNotContainKeys("review.manual_start_time", "review.reference_quantity");
        assertThat(first.manifest()).extracting("phaseBoundary")
                .asString().contains("MANIFEST_ONLY", "NOT_STARTED", "modelTrained=false");
    }

    @Test
    void unsupportedOrLabelReferenceCannotEnterFeatureSet() {
        assertThatThrownBy(() -> builder.validateDefinition(
                List.of("review.reference_quantity"),
                List.of("review.reference_quantity"),
                DatasetManifestBuilder.PREDICTION_TIME_POLICY,
                DatasetManifestBuilder.FEATURE_CUTOFF_POLICY,
                DatasetManifestBuilder.SPLIT_POLICY))
                .isInstanceOf(BpiValidationException.class)
                .hasMessageContaining("leakage-prone");
    }

    private DatasetManifestClaim claim(
            UUID snapshotId,
            UUID claimToken,
            UUID datasetId,
            UUID ruleId,
            Instant freezeAt,
            boolean excludeLowConfidence,
            int maxLabelDelayHours) {
        return new DatasetManifestClaim(
                snapshotId, claimToken, 1, "TENANT-E2E", datasetId,
                "BATCH-BOUNDARY", "1.0.0", "Batch boundary labels", "PLANT-01",
                freezeAt, List.of("LINE-01"), List.of(ruleId), excludeLowConfidence,
                "a".repeat(64), DatasetManifestBuilder.PREDICTION_TIME_POLICY,
                DatasetManifestBuilder.FEATURE_CUTOFF_POLICY,
                List.of("batch.order_id", "rule.version_id"),
                List.of("review.manual_start_time", "review.reference_quantity"),
                maxLabelDelayHours, BigDecimal.ONE, DatasetManifestBuilder.SPLIT_POLICY);
    }

    private DatasetSampleSource source(
            String batchNo,
            UUID ruleId,
            Instant automaticStart,
            Instant reviewedAt,
            boolean startAccepted,
            boolean endAccepted,
            boolean quantityAccepted) {
        Instant automaticEnd = automaticStart.plusSeconds(1_800);
        return new DatasetSampleSource(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), batchNo,
                "PLANT-01", "LINE-01", "EVAPORATION", "ORDER-01", "SUGAR-JUICE",
                ruleId, UUID.randomUUID(), UUID.randomUUID(), automaticStart, automaticEnd,
                automaticStart.plusSeconds(10), automaticEnd.plusSeconds(10),
                new BigDecimal("100.000000"), new BigDecimal("99.500000"), "t",
                startAccepted, endAccepted, quantityAccepted, reviewedAt);
    }
}
