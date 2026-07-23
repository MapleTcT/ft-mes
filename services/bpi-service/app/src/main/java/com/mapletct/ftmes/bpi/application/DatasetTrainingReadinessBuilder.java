package com.mapletct.ftmes.bpi.application;

import com.mapletct.ftmes.bpi.domain.DatasetTrainingReadinessBuild;
import com.mapletct.ftmes.bpi.domain.DatasetTrainingReadinessEvidence;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DatasetTrainingReadinessBuilder {
    public static final String OBJECTIVE_CODE = "BATCH_START_BOUNDARY_REVIEW_RISK";
    public static final String POLICY_VERSION =
            "bpi-training-readiness/batch-start-boundary-v1";

    private static final int MINIMUM_INCLUDED_SAMPLES = 200;
    private static final int MINIMUM_DISTINCT_BATCHES = 200;
    private static final int MINIMUM_PRODUCTION_DAYS = 7;
    private static final int MINIMUM_PRODUCTION_SPLIT_GROUPS = 2;
    private static final int MINIMUM_START_ACCEPTED_LABELS = 100;
    private static final int MINIMUM_START_REJECTED_LABELS = 10;
    private static final int MINIMUM_SIGNAL_WINDOW_FEATURES = 2;
    private static final long MINIMUM_CONTINUOUS_SHADOW_SECONDS = 7L * 86_400L;
    private static final BigDecimal MAXIMUM_EXCLUDED_RATIO = new BigDecimal("0.200000");
    private static final List<String> REQUIRED_CONTEXT_FEATURES = List.of(
            "batch.material_code",
            "batch.stage_code",
            "rule.version_id",
            "topology.version_id",
            "point_catalog.snapshot_id");
    private static final String REQUIRED_LABEL = "review.boundary_acceptance";

    private final CanonicalJson canonicalJson;

    public DatasetTrainingReadinessBuilder(CanonicalJson canonicalJson) {
        this.canonicalJson = canonicalJson;
    }

    public DatasetTrainingReadinessBuild build(DatasetTrainingReadinessEvidence evidence) {
        List<String> missingContextFeatures = REQUIRED_CONTEXT_FEATURES.stream()
                .filter(required -> !evidence.featureRefs().contains(required))
                .toList();
        List<String> signalWindowFeatures = evidence.featureRefs().stream()
                .filter(this::isSignalWindowFeature)
                .sorted()
                .toList();
        boolean requiredLabelPresent = evidence.labelRefs().contains(REQUIRED_LABEL);
        BigDecimal excludedRatio = ratio(
                evidence.excludedSampleCount(), evidence.persistedSampleCount());
        BigDecimal maximumContinuousShadowDays = BigDecimal
                .valueOf(evidence.maximumContinuousShadowRunSeconds())
                .divide(BigDecimal.valueOf(86_400L), 3, RoundingMode.DOWN);

        Map<String, Object> thresholds = new LinkedHashMap<>();
        thresholds.put("requiredRegistrationState", "REGISTERED");
        thresholds.put("requiredDatasetInputEvidence", true);
        thresholds.put("requiredPredictionTimePolicy", "AUTOMATIC_BATCH_START");
        thresholds.put("requiredFeatureCutoffPolicy", "AT_OR_BEFORE_PREDICTION_TIME");
        thresholds.put("requiredSplitPolicy", "PRODUCTION_TIME");
        thresholds.put("requiredContextFeatureRefs", REQUIRED_CONTEXT_FEATURES);
        thresholds.put("requiredLabelRef", REQUIRED_LABEL);
        thresholds.put("minimumSignalWindowFeatureRefs", MINIMUM_SIGNAL_WINDOW_FEATURES);
        thresholds.put("minimumIncludedSamples", MINIMUM_INCLUDED_SAMPLES);
        thresholds.put("minimumDistinctBatches", MINIMUM_DISTINCT_BATCHES);
        thresholds.put("minimumProductionDays", MINIMUM_PRODUCTION_DAYS);
        thresholds.put("minimumProductionSplitGroups", MINIMUM_PRODUCTION_SPLIT_GROUPS);
        thresholds.put("maximumExcludedRatio", MAXIMUM_EXCLUDED_RATIO);
        thresholds.put("minimumStartAcceptedLabels", MINIMUM_START_ACCEPTED_LABELS);
        thresholds.put("minimumStartRejectedLabels", MINIMUM_START_REJECTED_LABELS);
        thresholds.put("minimumContinuousShadowRunDays", 7);
        thresholds.put("maximumLeakageRows", 0);
        thresholds.put("maximumUnresolvedCriticalIncidents", 0);

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("registrationState", evidence.registrationState());
        metrics.put("registrationRevision", evidence.registrationRevision());
        metrics.put("datasetInputVerified", evidence.datasetInputVerified());
        metrics.put("lineageVerified", evidence.lineageVerified());
        metrics.put("sourceFactsVerified", evidence.sourceFactsVerified());
        metrics.put("sourceRowCount", evidence.sourceRowCount());
        metrics.put("snapshotIncludedCount", evidence.snapshotIncludedCount());
        metrics.put("snapshotExcludedCount", evidence.snapshotExcludedCount());
        metrics.put("persistedSampleCount", evidence.persistedSampleCount());
        metrics.put("includedSampleCount", evidence.includedSampleCount());
        metrics.put("excludedSampleCount", evidence.excludedSampleCount());
        metrics.put("excludedRatio", excludedRatio);
        metrics.put("distinctBatchCount", evidence.distinctBatchCount());
        metrics.put("distinctProductionDayCount", evidence.distinctProductionDayCount());
        metrics.put("productionSplitGroupCount", evidence.productionSplitGroupCount());
        metrics.put("leakageRowCount", evidence.leakageRowCount());
        metrics.put("featureRefs", evidence.featureRefs());
        metrics.put("labelRefs", evidence.labelRefs());
        metrics.put("missingContextFeatureRefs", missingContextFeatures);
        metrics.put("signalWindowFeatureRefs", signalWindowFeatures);
        metrics.put("startAcceptedLabelCount", evidence.startAcceptedLabelCount());
        metrics.put("startRejectedLabelCount", evidence.startRejectedLabelCount());
        metrics.put("startLabelMissingCount", evidence.startLabelMissingCount());
        metrics.put("distinctShadowRunCount", evidence.distinctShadowRunCount());
        metrics.put("approvedShadowRunCount", evidence.approvedShadowRunCount());
        metrics.put("shadowRunDurationGateFailureCount",
                evidence.shadowRunDurationGateFailureCount());
        metrics.put("maximumContinuousShadowRunDays", maximumContinuousShadowDays);
        metrics.put("pointCatalogSnapshotCount", evidence.pointCatalogSnapshotCount());
        metrics.put("readyPointCatalogSnapshotCount",
                evidence.readyPointCatalogSnapshotCount());
        metrics.put("unresolvedCriticalIncidentCount",
                evidence.unresolvedCriticalIncidentCount());

        List<Map<String, Object>> gates = new ArrayList<>();
        gate(gates, "MLFLOW_DATASET_INPUT_NOT_VERIFIED",
                "REGISTERED with verified input, lineage and source facts",
                evidence.registrationState() + "|" + evidence.datasetInputVerified()
                        + "|" + evidence.lineageVerified() + "|" + evidence.sourceFactsVerified(),
                "MLflow Dataset Input must be immutable and fully reconciled before assessment.",
                "REGISTERED".equals(evidence.registrationState())
                        && evidence.datasetInputVerified()
                        && evidence.lineageVerified()
                        && evidence.sourceFactsVerified());
        gate(gates, "DATASET_POLICY_MISMATCH",
                "AUTOMATIC_BATCH_START|AT_OR_BEFORE_PREDICTION_TIME|PRODUCTION_TIME",
                evidence.predictionTimePolicy() + "|" + evidence.featureCutoffPolicy()
                        + "|" + evidence.splitPolicy(),
                "The first model must use point-in-time safe features and production-time splits.",
                "AUTOMATIC_BATCH_START".equals(evidence.predictionTimePolicy())
                        && "AT_OR_BEFORE_PREDICTION_TIME".equals(evidence.featureCutoffPolicy())
                        && "PRODUCTION_TIME".equals(evidence.splitPolicy()));
        gate(gates, "SOURCE_ROW_RECONCILIATION_FAILED",
                "source rows = included rows; snapshot counts = persisted rows",
                evidence.sourceRowCount() + "|" + evidence.includedSampleCount()
                        + "|" + evidence.snapshotIncludedCount() + "+"
                        + evidence.snapshotExcludedCount() + "|" + evidence.persistedSampleCount(),
                "Training must use the exact rows registered in MLflow.",
                evidence.sourceRowCount() == evidence.includedSampleCount()
                        && evidence.snapshotIncludedCount() == evidence.includedSampleCount()
                        && evidence.snapshotExcludedCount() == evidence.excludedSampleCount()
                        && evidence.persistedSampleCount()
                        == evidence.includedSampleCount() + evidence.excludedSampleCount());
        gate(gates, "POINT_IN_TIME_LEAKAGE_DETECTED", 0,
                evidence.leakageRowCount(),
                "Feature cutoff and label availability must not leak future facts.",
                evidence.leakageRowCount() == 0);
        gate(gates, "REQUIRED_CONTEXT_FEATURES_MISSING", List.of(),
                missingContextFeatures,
                "Material, stage and immutable rule/topology/catalog lineage are required.",
                missingContextFeatures.isEmpty());
        gate(gates, "PROCESS_SIGNAL_WINDOWS_MISSING", MINIMUM_SIGNAL_WINDOW_FEATURES,
                signalWindowFeatures.size(),
                "At least two pre-boundary flow, pump, valve or level windows are required; identifiers alone cannot train a process model.",
                signalWindowFeatures.size() >= MINIMUM_SIGNAL_WINDOW_FEATURES);
        gate(gates, "BOUNDARY_REVIEW_LABEL_MISSING", REQUIRED_LABEL,
                requiredLabelPresent ? REQUIRED_LABEL : evidence.labelRefs(),
                "The objective requires the reviewed start-boundary acceptance label.",
                requiredLabelPresent);
        gate(gates, "INCLUDED_SAMPLE_COUNT_BELOW_MINIMUM", MINIMUM_INCLUDED_SAMPLES,
                evidence.includedSampleCount(),
                "The pilot model needs enough reviewed batches to avoid a demo-only fit.",
                evidence.includedSampleCount() >= MINIMUM_INCLUDED_SAMPLES);
        gate(gates, "DISTINCT_BATCH_COUNT_BELOW_MINIMUM", MINIMUM_DISTINCT_BATCHES,
                evidence.distinctBatchCount(),
                "Repeated rows from the same batch do not increase independent evidence.",
                evidence.distinctBatchCount() >= MINIMUM_DISTINCT_BATCHES);
        gate(gates, "PRODUCTION_DAY_COVERAGE_BELOW_MINIMUM", MINIMUM_PRODUCTION_DAYS,
                evidence.distinctProductionDayCount(),
                "The dataset must cover at least seven production dates.",
                evidence.distinctProductionDayCount() >= MINIMUM_PRODUCTION_DAYS);
        gate(gates, "PRODUCTION_SPLIT_GROUPS_BELOW_MINIMUM",
                MINIMUM_PRODUCTION_SPLIT_GROUPS, evidence.productionSplitGroupCount(),
                "At least two production-time groups are required for train/validation isolation.",
                evidence.productionSplitGroupCount() >= MINIMUM_PRODUCTION_SPLIT_GROUPS);
        gate(gates, "EXCLUDED_RATIO_ABOVE_MAXIMUM", MAXIMUM_EXCLUDED_RATIO,
                excludedRatio,
                "High exclusion rates indicate an unstable label or source-quality process.",
                excludedRatio.compareTo(MAXIMUM_EXCLUDED_RATIO) <= 0);
        gate(gates, "START_ACCEPTED_LABEL_COUNT_BELOW_MINIMUM",
                MINIMUM_START_ACCEPTED_LABELS, evidence.startAcceptedLabelCount(),
                "The accepted class must contain enough reviewed examples.",
                evidence.startAcceptedLabelCount() >= MINIMUM_START_ACCEPTED_LABELS);
        gate(gates, "START_REJECTED_LABEL_COUNT_BELOW_MINIMUM",
                MINIMUM_START_REJECTED_LABELS, evidence.startRejectedLabelCount(),
                "The minority rejection class must be represented before risk training.",
                evidence.startRejectedLabelCount() >= MINIMUM_START_REJECTED_LABELS);
        gate(gates, "START_LABEL_VALUES_MISSING", 0,
                evidence.startLabelMissingCount(),
                "Every included row must have a reviewed start-boundary label.",
                evidence.startLabelMissingCount() == 0);
        gate(gates, "SHADOW_RUN_SOURCE_NOT_APPROVED",
                evidence.distinctShadowRunCount(), evidence.approvedShadowRunCount(),
                "Every included row must originate from an approved shadow run.",
                evidence.distinctShadowRunCount() > 0
                        && evidence.approvedShadowRunCount()
                        == evidence.distinctShadowRunCount());
        gate(gates, "SHADOW_RUN_DURATION_GATE_FAILED", 0,
                evidence.shadowRunDurationGateFailureCount(),
                "Each source run must satisfy its governed 7-14 day field window.",
                evidence.shadowRunDurationGateFailureCount() == 0
                        && evidence.maximumContinuousShadowRunSeconds()
                        >= MINIMUM_CONTINUOUS_SHADOW_SECONDS);
        gate(gates, "POINT_CATALOG_READINESS_UNPROVEN",
                evidence.pointCatalogSnapshotCount(),
                evidence.readyPointCatalogSnapshotCount(),
                "All pinned point catalogs must preserve their source readiness claim.",
                evidence.pointCatalogSnapshotCount() > 0
                        && evidence.readyPointCatalogSnapshotCount()
                        == evidence.pointCatalogSnapshotCount());
        gate(gates, "UNRESOLVED_CRITICAL_DATA_QUALITY", 0,
                evidence.unresolvedCriticalIncidentCount(),
                "Critical incidents overlapping source runs must be resolved before training.",
                evidence.unresolvedCriticalIncidentCount() == 0);

        List<String> blockers = gates.stream()
                .filter(gate -> !Boolean.TRUE.equals(gate.get("passed")))
                .map(gate -> String.valueOf(gate.get("code")))
                .toList();
        Map<String, Object> phaseBoundary = new LinkedHashMap<>();
        phaseBoundary.put("assessmentOnly", true);
        phaseBoundary.put("trainingStarted", false);
        phaseBoundary.put("modelCreated", false);
        phaseBoundary.put("modelRegistered", false);
        phaseBoundary.put("onlineInferenceEnabled", false);
        phaseBoundary.put("productionActivationAllowed", false);

        Map<String, Object> checksumPayload = new LinkedHashMap<>();
        checksumPayload.put("registrationId", evidence.registrationId());
        checksumPayload.put("snapshotId", evidence.snapshotId());
        checksumPayload.put("sourceRegistrationRevision", evidence.registrationRevision());
        checksumPayload.put("objectiveCode", OBJECTIVE_CODE);
        checksumPayload.put("policyVersion", POLICY_VERSION);
        checksumPayload.put("requiredThresholds", thresholds);
        checksumPayload.put("observedMetrics", metrics);
        checksumPayload.put("gateResults", gates);
        checksumPayload.put("phaseBoundary", phaseBoundary);
        return new DatasetTrainingReadinessBuild(
                blockers.isEmpty() ? "ELIGIBLE" : "BLOCKED",
                Map.copyOf(thresholds), Map.copyOf(metrics), List.copyOf(gates),
                List.copyOf(blockers), Map.copyOf(phaseBoundary),
                Checksums.sha256(canonicalJson.write(checksumPayload)));
    }

    private boolean isSignalWindowFeature(String reference) {
        return reference.startsWith("signal.")
                || reference.startsWith("telemetry.")
                || reference.startsWith("process.window.")
                || reference.startsWith("parameter.window.");
    }

    private BigDecimal ratio(int numerator, int denominator) {
        if (denominator <= 0) return BigDecimal.ONE.setScale(6);
        return BigDecimal.valueOf(numerator)
                .divide(BigDecimal.valueOf(denominator), 6, RoundingMode.HALF_UP);
    }

    private void gate(
            List<Map<String, Object>> gates,
            String code,
            Object expected,
            Object observed,
            String detail,
            boolean passed) {
        Map<String, Object> gate = new LinkedHashMap<>();
        gate.put("code", code);
        gate.put("passed", passed);
        gate.put("expected", expected);
        gate.put("observed", observed);
        gate.put("detail", detail);
        gates.add(Map.copyOf(gate));
    }
}
