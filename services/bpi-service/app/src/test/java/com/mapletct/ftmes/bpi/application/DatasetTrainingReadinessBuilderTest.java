package com.mapletct.ftmes.bpi.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.domain.DatasetTrainingReadinessBuild;
import com.mapletct.ftmes.bpi.domain.DatasetTrainingReadinessEvidence;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DatasetTrainingReadinessBuilderTest {
    private final DatasetTrainingReadinessBuilder builder =
            new DatasetTrainingReadinessBuilder(
                    new CanonicalJson(new ObjectMapper().findAndRegisterModules()));

    @Test
    void sparseIdentifierOnlyDatasetFailsClosedWithoutStartingTraining() {
        DatasetTrainingReadinessBuild result = builder.build(evidence(
                List.of("batch.material_code", "rule.version_id"),
                List.of("review.manual_start_time"),
                1, 2, 0, 0, 1, 1, 1, true));

        assertThat(result.state()).isEqualTo("BLOCKED");
        assertThat(result.blockerCodes())
                .contains("REQUIRED_CONTEXT_FEATURES_MISSING")
                .contains("PROCESS_SIGNAL_WINDOWS_MISSING")
                .contains("BOUNDARY_REVIEW_LABEL_MISSING")
                .contains("INCLUDED_SAMPLE_COUNT_BELOW_MINIMUM")
                .contains("START_LABEL_VALUES_MISSING");
        assertThat(result.phaseBoundary())
                .containsEntry("assessmentOnly", true)
                .containsEntry("trainingStarted", false)
                .containsEntry("modelCreated", false)
                .containsEntry("productionActivationAllowed", false);
        assertThat(result.assessmentChecksum()).hasSize(64);
    }

    @Test
    void governedPointInTimeDatasetCanBecomeEligibleWithoutModelSideEffects() {
        List<String> features = List.of(
                "batch.material_code", "batch.stage_code", "rule.version_id",
                "topology.version_id", "point_catalog.snapshot_id",
                "process.window.feed_flow.mean_60s",
                "process.window.feed_pump.true_ratio_30s");
        DatasetTrainingReadinessEvidence evidence = evidence(
                features, List.of("review.boundary_acceptance"),
                220, 20, 210, 10, 220, 2, 0, true);

        DatasetTrainingReadinessBuild first = builder.build(evidence);
        DatasetTrainingReadinessBuild second = builder.build(evidence);

        assertThat(first.state()).isEqualTo("ELIGIBLE");
        assertThat(first.blockerCodes()).isEmpty();
        assertThat(first.gateResults()).allSatisfy(gate ->
                assertThat(gate).containsEntry("passed", true));
        assertThat(first.assessmentChecksum())
                .isEqualTo(second.assessmentChecksum())
                .hasSize(64);
        assertThat(first.phaseBoundary())
                .containsEntry("trainingStarted", false)
                .containsEntry("modelRegistered", false)
                .containsEntry("onlineInferenceEnabled", false);
    }

    @Test
    void declaredWindowNamesCannotReplacePersistedReadyFacts() {
        List<String> features = List.of(
                "batch.material_code", "batch.stage_code", "rule.version_id",
                "topology.version_id", "point_catalog.snapshot_id",
                "process.window.feed_flow.mean_60s",
                "process.window.feed_pump.true_ratio_30s");
        DatasetTrainingReadinessBuild result = builder.build(evidence(
                features, List.of("review.boundary_acceptance"),
                220, 20, 210, 10, 220, 2, 0, false));

        assertThat(result.state()).isEqualTo("BLOCKED");
        assertThat(result.blockerCodes())
                .contains("PROCESS_SIGNAL_WINDOW_FACTS_INCOMPLETE")
                .doesNotContain("PROCESS_SIGNAL_WINDOWS_MISSING");
    }

    private DatasetTrainingReadinessEvidence evidence(
            List<String> featureRefs,
            List<String> labelRefs,
            int included,
            int excluded,
            int accepted,
            int rejected,
            int distinctBatches,
            int splitGroups,
            int missingLabels,
            boolean windowFactsReady) {
        UUID registrationId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID snapshotId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID datasetId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        int processWindowCount = (int) featureRefs.stream()
                .filter(reference -> reference.startsWith("signal.")
                        || reference.startsWith("telemetry.")
                        || reference.startsWith("process.window.")
                        || reference.startsWith("parameter.window."))
                .count();
        int expectedWindowFacts = included * processWindowCount;
        int readyWindowFacts = windowFactsReady ? expectedWindowFacts : 0;
        return new DatasetTrainingReadinessEvidence(
                registrationId, snapshotId, datasetId,
                "BOUNDARY-RISK", "1.0.0", "TENANT-01", "PLANT-01",
                List.of("LINE-01"), "REGISTERED", 6,
                "a".repeat(64), "b".repeat(16), included,
                true, true, true,
                "AUTOMATIC_BATCH_START", "AT_OR_BEFORE_PREDICTION_TIME",
                "PRODUCTION_TIME", featureRefs, labelRefs,
                included, excluded, included + excluded, included, excluded,
                distinctBatches, 8, splitGroups, 0,
                expectedWindowFacts, readyWindowFacts, 0,
                expectedWindowFacts - readyWindowFacts,
                accepted, rejected, missingLabels,
                1, 1, 0, 8L * 86_400L,
                1, 1, 0);
    }
}
