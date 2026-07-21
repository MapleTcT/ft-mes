package com.mapletct.ftmes.bpi.application;

import com.mapletct.ftmes.bpi.application.error.BpiValidationException;
import com.mapletct.ftmes.bpi.domain.DatasetManifestBuild;
import com.mapletct.ftmes.bpi.domain.DatasetManifestClaim;
import com.mapletct.ftmes.bpi.domain.DatasetManifestSample;
import com.mapletct.ftmes.bpi.domain.DatasetSampleSource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Component
public class DatasetManifestBuilder {
    public static final String MANIFEST_SCHEMA_VERSION = "bpi.dataset-manifest.v1";
    public static final String PREDICTION_TIME_POLICY = "AUTOMATIC_BATCH_START";
    public static final String FEATURE_CUTOFF_POLICY = "AT_OR_BEFORE_PREDICTION_TIME";
    public static final String SPLIT_POLICY = "PRODUCTION_TIME";
    public static final int MAX_SAMPLES = 10_000;

    public static final Set<String> ALLOWED_FEATURE_REFS = Set.of(
            "batch.order_id",
            "batch.material_code",
            "batch.stage_code",
            "batch.quantity_unit",
            "rule.version_id",
            "topology.version_id",
            "point_catalog.snapshot_id");

    public static final Set<String> ALLOWED_LABEL_REFS = Set.of(
            "review.manual_start_time",
            "review.manual_end_time",
            "review.reference_quantity",
            "review.boundary_acceptance",
            "review.quantity_acceptance",
            "batch.automatic_end_time",
            "batch.automatic_quantity");

    private static final DateTimeFormatter SPLIT_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM").withZone(ZoneOffset.UTC);

    private final CanonicalJson canonicalJson;

    public DatasetManifestBuilder(CanonicalJson canonicalJson) {
        this.canonicalJson = canonicalJson;
    }

    public void validateDefinition(
            List<String> featureRefs,
            List<String> labelRefs,
            String predictionTimePolicy,
            String featureCutoffPolicy,
            String splitPolicy) {
        validateDistinctRefs(featureRefs, "featureRefs", ALLOWED_FEATURE_REFS);
        validateDistinctRefs(labelRefs, "labelRefs", ALLOWED_LABEL_REFS);
        if (!PREDICTION_TIME_POLICY.equals(predictionTimePolicy)) {
            throw new BpiValidationException(
                    "Phase 3A supports only AUTOMATIC_BATCH_START prediction time.");
        }
        if (!FEATURE_CUTOFF_POLICY.equals(featureCutoffPolicy)) {
            throw new BpiValidationException(
                    "Phase 3A requires feature data at or before prediction time.");
        }
        if (!SPLIT_POLICY.equals(splitPolicy)) {
            throw new BpiValidationException(
                    "Phase 3A requires production-time dataset splitting.");
        }
    }

    public DatasetManifestBuild build(
            DatasetManifestClaim claim,
            List<DatasetSampleSource> sourceRows) {
        validateDefinition(claim.featureRefs(), claim.labelRefs(), claim.predictionTimePolicy(),
                claim.featureCutoffPolicy(), claim.splitPolicy());
        if (sourceRows.size() > MAX_SAMPLES) {
            throw new BpiValidationException(
                    "Dataset snapshot exceeds the Phase 3A limit of 10000 reviewed batches.");
        }

        List<DatasetSampleSource> ordered = sourceRows.stream()
                .sorted(Comparator.comparing(DatasetSampleSource::lineId)
                        .thenComparing(DatasetSampleSource::automaticStartTime)
                        .thenComparing(DatasetSampleSource::batchId)
                        .thenComparing(DatasetSampleSource::reviewId))
                .toList();
        List<DatasetManifestSample> samples = new ArrayList<>(ordered.size());
        Map<String, Integer> exclusionSummary = new TreeMap<>();
        int included = 0;

        for (DatasetSampleSource source : ordered) {
            DatasetManifestSample sample = sample(claim, source);
            samples.add(sample);
            if (sample.included()) {
                included++;
            } else {
                sample.exclusionReasons().forEach(reason ->
                        exclusionSummary.merge(reason, 1, Integer::sum));
            }
        }

        Map<String, Object> manifest = manifest(claim, samples, included, exclusionSummary);
        return new DatasetManifestBuild(
                List.copyOf(samples),
                manifest,
                Checksums.sha256(canonicalJson.write(manifest)),
                included,
                samples.size() - included,
                Map.copyOf(exclusionSummary));
    }

    private DatasetManifestSample sample(
            DatasetManifestClaim claim,
            DatasetSampleSource source) {
        int acceptedChecks = (source.startBoundaryAccepted() ? 1 : 0)
                + (source.endBoundaryAccepted() ? 1 : 0)
                + (source.quantityWithinTolerance() ? 1 : 0);
        BigDecimal confidence = BigDecimal.valueOf(acceptedChecks)
                .divide(BigDecimal.valueOf(3), 6, RoundingMode.HALF_UP);
        List<String> exclusionReasons = new ArrayList<>();

        if (source.reviewedAt().isBefore(source.automaticStartTime())) {
            exclusionReasons.add("LABEL_AVAILABLE_BEFORE_PREDICTION_TIME");
        }
        if (source.reviewedAt().isAfter(claim.freezeAt())) {
            exclusionReasons.add("LABEL_AVAILABLE_AFTER_FREEZE_AT");
        }
        Duration labelDelay = Duration.between(source.automaticStartTime(), source.reviewedAt());
        if (!labelDelay.isNegative()
                && labelDelay.compareTo(Duration.ofHours(claim.maxLabelDelayHours())) > 0) {
            exclusionReasons.add("LABEL_DELAY_EXCEEDED");
        }
        if (claim.excludeLowConfidence()
                && confidence.compareTo(claim.minimumConfidence()) < 0) {
            exclusionReasons.add("CONFIDENCE_BELOW_THRESHOLD");
            if (!source.startBoundaryAccepted()) {
                exclusionReasons.add("START_BOUNDARY_OUTSIDE_TOLERANCE");
            }
            if (!source.endBoundaryAccepted()) {
                exclusionReasons.add("END_BOUNDARY_OUTSIDE_TOLERANCE");
            }
            if (!source.quantityWithinTolerance()) {
                exclusionReasons.add("QUANTITY_OUTSIDE_TOLERANCE");
            }
        }

        List<String> stableReasons = exclusionReasons.stream().distinct().sorted().toList();
        return new DatasetManifestSample(
                source.reviewId(), source.shadowRunId(), source.batchId(), source.batchNo(),
                source.lineId(), stableReasons.isEmpty(), stableReasons,
                source.automaticStartTime(), source.automaticStartTime(), source.reviewedAt(),
                confidence, SPLIT_FORMAT.format(source.automaticStartTime()),
                selectedPayload(claim.featureRefs(), featureValues(source)),
                selectedPayload(claim.labelRefs(), labelValues(source)),
                sourcePayload(source));
    }

    private Map<String, Object> manifest(
            DatasetManifestClaim claim,
            List<DatasetManifestSample> samples,
            int included,
            Map<String, Integer> exclusionSummary) {
        Map<String, Object> definition = new LinkedHashMap<>();
        definition.put("datasetId", claim.datasetId());
        definition.put("datasetCode", claim.datasetCode());
        definition.put("datasetVersion", claim.datasetVersion());
        definition.put("definitionChecksum", claim.definitionChecksum());
        definition.put("predictionTimePolicy", claim.predictionTimePolicy());
        definition.put("featureCutoffPolicy", claim.featureCutoffPolicy());
        definition.put("featureRefs", claim.featureRefs());
        definition.put("labelRefs", claim.labelRefs());
        definition.put("maxLabelDelayHours", claim.maxLabelDelayHours());
        definition.put("minimumConfidence", claim.minimumConfidence());
        definition.put("splitPolicy", claim.splitPolicy());

        Map<String, Object> selection = new LinkedHashMap<>();
        selection.put("freezeAt", claim.freezeAt());
        selection.put("plantId", claim.plantId());
        selection.put("lineIds", claim.lineIds());
        selection.put("ruleVersionIds", claim.ruleVersionIds());
        selection.put("approvedShadowRunsOnly", true);
        selection.put("activeReviewAtFreezeOnly", true);
        selection.put("excludeLowConfidence", claim.excludeLowConfidence());

        Map<String, Object> boundary = new LinkedHashMap<>();
        boundary.put("deliveryState", "MANIFEST_ONLY");
        boundary.put("materializationState", "NOT_STARTED");
        boundary.put("artifactUri", null);
        boundary.put("icebergReady", false);
        boundary.put("mlflowRegistered", false);
        boundary.put("modelTrained", false);

        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("total", samples.size());
        counts.put("included", included);
        counts.put("excluded", samples.size() - included);
        counts.put("exclusionSummary", exclusionSummary);

        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("schemaVersion", MANIFEST_SCHEMA_VERSION);
        manifest.put("definition", definition);
        manifest.put("selection", selection);
        manifest.put("phaseBoundary", boundary);
        manifest.put("counts", counts);
        manifest.put("samples", samples);
        return manifest;
    }

    private Map<String, Object> featureValues(DatasetSampleSource source) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("batch.order_id", source.orderId());
        values.put("batch.material_code", source.materialCode());
        values.put("batch.stage_code", source.stageCode());
        values.put("batch.quantity_unit", source.quantityUnit());
        values.put("rule.version_id", source.ruleVersionId());
        values.put("topology.version_id", source.topologyVersionId());
        values.put("point_catalog.snapshot_id", source.pointCatalogSnapshotId());
        return values;
    }

    private Map<String, Object> labelValues(DatasetSampleSource source) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("review.manual_start_time", source.manualStartTime());
        values.put("review.manual_end_time", source.manualEndTime());
        values.put("review.reference_quantity", source.referenceQuantity());
        values.put("review.boundary_acceptance", Map.of(
                "start", source.startBoundaryAccepted(),
                "end", source.endBoundaryAccepted()));
        values.put("review.quantity_acceptance", source.quantityWithinTolerance());
        values.put("batch.automatic_end_time", source.automaticEndTime());
        values.put("batch.automatic_quantity", source.automaticQuantity());
        return values;
    }

    private Map<String, Object> sourcePayload(DatasetSampleSource source) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reviewId", source.reviewId());
        payload.put("shadowRunId", source.shadowRunId());
        payload.put("batchId", source.batchId());
        payload.put("batchNo", source.batchNo());
        payload.put("automaticStartTime", source.automaticStartTime());
        payload.put("automaticEndTime", source.automaticEndTime());
        payload.put("reviewedAt", source.reviewedAt());
        payload.put("startBoundaryAccepted", source.startBoundaryAccepted());
        payload.put("endBoundaryAccepted", source.endBoundaryAccepted());
        payload.put("quantityWithinTolerance", source.quantityWithinTolerance());
        return payload;
    }

    private Map<String, Object> selectedPayload(
            List<String> refs,
            Map<String, Object> available) {
        Map<String, Object> selected = new LinkedHashMap<>();
        refs.forEach(ref -> selected.put(ref, available.get(ref)));
        return selected;
    }

    private void validateDistinctRefs(
            List<String> refs,
            String field,
            Set<String> allowed) {
        if (refs == null || refs.isEmpty()) {
            throw new BpiValidationException(field + " must contain at least one reference.");
        }
        LinkedHashSet<String> distinct = new LinkedHashSet<>(refs);
        if (distinct.size() != refs.size()) {
            throw new BpiValidationException(field + " must not contain duplicates.");
        }
        List<String> unsupported = refs.stream().filter(ref -> !allowed.contains(ref)).sorted().toList();
        if (!unsupported.isEmpty()) {
            throw new BpiValidationException(
                    field + " contains unsupported or leakage-prone references: "
                            + String.join(",", unsupported));
        }
    }
}
