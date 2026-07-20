package com.mapletct.ftmes.bpi.stream;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRulePublicationV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleRuntimeReadinessStatusV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundarySignalBindingV1;
import com.mapletct.ftmes.bpi.contract.v1.PointCalibrationStatusV1;
import com.mapletct.ftmes.bpi.contract.v1.PointCatalogPointV1;
import com.mapletct.ftmes.bpi.contract.v1.PointCatalogSnapshotV1;
import com.mapletct.ftmes.bpi.contract.v1.PointDeviceStateV1;
import com.mapletct.ftmes.bpi.contract.v1.PointValue;
import com.mapletct.ftmes.bpi.contract.v1.SequenceOrigin;
import com.mapletct.ftmes.bpi.contract.v1.TelemetryEnvelopeV1;
import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ReadOnlyBroadcastState;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Routes each contextual point by an indexed product/device/property lookup and current catalog readiness. */
public final class BoundaryRuleRoutingBroadcastFunction extends BroadcastProcessFunction<
        byte[],
        byte[],
        BoundaryStreamInput> {

    public static final MapStateDescriptor<String, byte[]> PUBLICATIONS = new MapStateDescriptor<>(
            "bpi-boundary-publications-v1", String.class, byte[].class);
    public static final MapStateDescriptor<String, byte[]> ROUTES = new MapStateDescriptor<>(
            "bpi-boundary-route-index-v1", String.class, byte[].class);
    public static final MapStateDescriptor<String, byte[]> POINT_CATALOGS = new MapStateDescriptor<>(
            "bpi-point-catalog-scopes-v1", String.class, byte[].class);
    public static final MapStateDescriptor<String, byte[]> POINTS = new MapStateDescriptor<>(
            "bpi-point-catalog-points-v1", String.class, byte[].class);
    public static final MapStateDescriptor<String, byte[]> RUNTIME_RULE_STATUS = new MapStateDescriptor<>(
            "bpi-runtime-rule-readiness-v1", String.class, byte[].class);
    public static final OutputTag<BoundaryRoutingIssue> ISSUES =
            new OutputTag<>("bpi-boundary-routing-issues") {
            };
    public static final OutputTag<byte[]> RULE_UPDATES =
            new OutputTag<>("bpi-runtime-ready-rule-updates") {
            };
    public static final OutputTag<byte[]> RUNTIME_READINESS =
            new OutputTag<>("bpi-rule-runtime-readiness") {
            };

    private final String deploymentId;
    private transient boolean controlPlaneEmitter;

    BoundaryRuleRoutingBroadcastFunction() {
        this("test-deployment");
    }

    public BoundaryRuleRoutingBroadcastFunction(String deploymentId) {
        if (deploymentId == null || deploymentId.isBlank()) {
            throw new IllegalArgumentException("deploymentId is required");
        }
        this.deploymentId = deploymentId;
    }

    @Override
    public void open(OpenContext openContext) {
        controlPlaneEmitter = getRuntimeContext().getTaskInfo().getIndexOfThisSubtask() == 0;
    }

    @Override
    public void processElement(
            byte[] contextualBytes,
            ReadOnlyContext context,
            Collector<BoundaryStreamInput> output) throws Exception {
        ContextualTelemetryPoint contextual;
        try {
            contextual = ContextualTelemetryPointCodec.decode(contextualBytes);
        } catch (IllegalStateException error) {
            context.output(ISSUES, new BoundaryRoutingIssue(
                    "CONTEXTUAL_POINT_DECODE_REJECTED", "", "", error.getMessage()));
            return;
        }
        String routeKey = routeKey(contextual);
        ReadOnlyBroadcastState<String, byte[]> routes = context.getBroadcastState(ROUTES);
        byte[] encodedRuleKeys = routes.get(routeKey);
        if (encodedRuleKeys == null) {
            return;
        }
        List<String> ruleKeys;
        try {
            ruleKeys = BoundaryRouteIndexCodec.decode(encodedRuleKeys);
        } catch (IllegalStateException error) {
            context.output(ISSUES, issue(contextual, "ROUTE_INDEX_DECODE_REJECTED", error.getMessage()));
            return;
        }
        ReadOnlyBroadcastState<String, byte[]> publications = context.getBroadcastState(PUBLICATIONS);
        ReadOnlyBroadcastState<String, byte[]> catalogs = context.getBroadcastState(POINT_CATALOGS);
        ReadOnlyBroadcastState<String, byte[]> points = context.getBroadcastState(POINTS);
        for (String ruleKey : ruleKeys) {
            byte[] publicationBytes = publications.get(ruleKey);
            if (publicationBytes == null) {
                context.output(ISSUES, issue(
                        contextual, "ROUTE_PUBLICATION_MISSING", "route references an unavailable publication"));
                continue;
            }
            try {
                PublishedBoundaryPlan plan = BoundaryRulePublicationMapper.map(
                        BoundaryRulePublicationV1.parseFrom(publicationBytes));
                BoundarySignalBindingV1 binding = plan.bindings().get(bindingKey(contextual));
                BoundaryRoutingIssue readinessIssue = readinessIssue(contextual, binding, catalogs, points);
                if (readinessIssue != null) {
                    context.output(ISSUES, readinessIssue);
                    continue;
                }
                BoundaryRoutingResult result = BoundarySignalRouter.route(plan, contextual);
                for (BoundaryStreamInput input : result.inputs()) {
                    output.collect(input);
                }
                for (BoundaryRoutingIssue issue : result.issues()) {
                    context.output(ISSUES, issue);
                }
            } catch (InvalidProtocolBufferException | IllegalArgumentException | IllegalStateException error) {
                context.output(ISSUES, issue(contextual, "PUBLICATION_DECODE_REJECTED", error.getMessage()));
            }
        }
    }

    @Override
    public void processBroadcastElement(
            byte[] controlBytes,
            Context context,
            Collector<BoundaryStreamInput> output) throws Exception {
        BoundaryRoutingControlCodec.Decoded control;
        try {
            control = BoundaryRoutingControlCodec.decode(controlBytes);
        } catch (IllegalStateException error) {
            emit(context, ISSUES, new BoundaryRoutingIssue(
                    "ROUTING_CONTROL_REJECTED", "", "", error.getMessage()));
            return;
        }
        if (control.kind() == BoundaryRoutingControlCodec.Kind.POINT_CATALOG) {
            processPointCatalog(control.payload(), context);
            return;
        }
        processRulePublication(control.payload(), context);
    }

    private void processRulePublication(byte[] publicationBytes, Context context) throws Exception {
        BoundaryRulePublicationV1 publication;
        PublishedBoundaryPlan incoming;
        try {
            publication = BoundaryRulePublicationV1.parseFrom(publicationBytes);
            incoming = BoundaryRulePublicationMapper.map(publication);
        } catch (InvalidProtocolBufferException | IllegalArgumentException | IllegalStateException error) {
            emit(context, ISSUES, new BoundaryRoutingIssue(
                    "RULE_PUBLICATION_REJECTED", "", "", error.getMessage()));
            return;
        }
        String ruleKey = incoming.ruleRef().key();
        BroadcastState<String, byte[]> publications = context.getBroadcastState(PUBLICATIONS);
        BroadcastState<String, byte[]> routes = context.getBroadcastState(ROUTES);
        BroadcastState<String, byte[]> catalogs = context.getBroadcastState(POINT_CATALOGS);
        BroadcastState<String, byte[]> points = context.getBroadcastState(POINTS);
        BroadcastState<String, byte[]> runtimeStatus = context.getBroadcastState(RUNTIME_RULE_STATUS);
        byte[] existingBytes = publications.get(ruleKey);
        if (existingBytes != null) {
            BoundaryRulePublicationV1 existing = BoundaryRulePublicationV1.parseFrom(existingBytes);
            if (!BoundaryRulePublicationSemantics.equivalent(existing, publication)) {
                emit(context, ISSUES, new BoundaryRoutingIssue(
                        "RULE_VERSION_CONFLICT", publication.getEventId(), "",
                        "published rule versions are immutable; use a new version"));
                return;
            }
            if (!existing.getActive() && publication.getActive()) {
                emit(context, ISSUES, new BoundaryRoutingIssue(
                        "RULE_REACTIVATION_REQUIRES_NEW_VERSION", publication.getEventId(), "",
                        "an inactive rule version cannot be reactivated"));
                return;
            }
            if (publication.getPublishedAtMs() < existing.getPublishedAtMs()) {
                emit(context, ISSUES, new BoundaryRoutingIssue(
                        "RULE_PUBLICATION_OUT_OF_ORDER", publication.getEventId(), "",
                        "publication time is older than the current lifecycle state"));
                return;
            }
            removeRoutes(routes, BoundaryRulePublicationMapper.map(existing));
        }
        publications.put(ruleKey, Arrays.copyOf(publicationBytes, publicationBytes.length));
        reconcileRule(context, incoming, routes, catalogs, points, runtimeStatus);
    }

    private void processPointCatalog(byte[] snapshotBytes, Context context) throws Exception {
        PointCatalogSnapshotV1 incoming;
        try {
            incoming = PointCatalogRuntimeValidator.validate(PointCatalogSnapshotV1.parseFrom(snapshotBytes));
        } catch (InvalidProtocolBufferException | IllegalArgumentException error) {
            emit(context, ISSUES, new BoundaryRoutingIssue(
                    "POINT_CATALOG_RUNTIME_REJECTED", "", "", error.getMessage()));
            return;
        }
        String scopeKey = PointCatalogRuntimeValidator.scopeKey(incoming);
        BroadcastState<String, byte[]> catalogs = context.getBroadcastState(POINT_CATALOGS);
        BroadcastState<String, byte[]> points = context.getBroadcastState(POINTS);
        BroadcastState<String, byte[]> runtimeStatus = context.getBroadcastState(RUNTIME_RULE_STATUS);
        byte[] currentBytes = catalogs.get(scopeKey);
        if (currentBytes != null) {
            PointCatalogSnapshotV1 current = PointCatalogSnapshotV1.parseFrom(currentBytes);
            if (incoming.getObservedAtMs() < current.getObservedAtMs()) {
                emit(context, ISSUES, catalogIssue(
                        incoming, "POINT_CATALOG_OUT_OF_ORDER", "older catalog snapshot was ignored"));
                return;
            }
            if (incoming.getObservedAtMs() == current.getObservedAtMs()) {
                if (java.util.Arrays.equals(currentBytes, snapshotBytes)) {
                    return;
                }
                emit(context, ISSUES, catalogIssue(
                        incoming,
                        "POINT_CATALOG_TIME_CONFLICT",
                        "different catalog content has the same observed_at_ms"));
                return;
            }
            for (PointCatalogPointV1 point : current.getPointsList()) {
                points.remove(PointCatalogRuntimeValidator.pointKey(current, point));
            }
        }
        for (PointCatalogPointV1 point : incoming.getPointsList()) {
            points.put(PointCatalogRuntimeValidator.pointKey(incoming, point), point.toByteArray());
        }
        catalogs.put(scopeKey, java.util.Arrays.copyOf(snapshotBytes, snapshotBytes.length));
        BroadcastState<String, byte[]> publications = context.getBroadcastState(PUBLICATIONS);
        BroadcastState<String, byte[]> routes = context.getBroadcastState(ROUTES);
        for (Map.Entry<String, byte[]> entry : publications.entries()) {
            BoundaryRulePublicationV1 publication = null;
            try {
                publication = BoundaryRulePublicationV1.parseFrom(entry.getValue());
                if (sameScope(publication, incoming)) {
                    reconcileRule(
                            context,
                            BoundaryRulePublicationMapper.map(publication),
                            routes,
                            catalogs,
                            points,
                            runtimeStatus);
                }
            } catch (InvalidProtocolBufferException | IllegalArgumentException | IllegalStateException error) {
                if (publication != null) {
                    failClosedLegacyPublication(
                            context, publication, incoming, runtimeStatus, error.getMessage());
                }
                emit(context, ISSUES, publication == null
                        ? new BoundaryRoutingIssue(
                                "RULE_PUBLICATION_RUNTIME_REJECTED", "", "", error.getMessage())
                        : publicationIssue(
                                publication,
                                null,
                                "RULE_PUBLICATION_RUNTIME_REJECTED",
                                error.getMessage()));
            }
        }
    }

    private void failClosedLegacyPublication(
            Context context,
            BoundaryRulePublicationV1 publication,
            PointCatalogSnapshotV1 catalog,
            BroadcastState<String, byte[]> runtimeStatus,
            String detail) throws Exception {
        if (publication.getEventId().isBlank()
                || publication.getTenantId().isBlank()
                || publication.getPlantId().isBlank()
                || publication.getLineId().isBlank()
                || publication.getRuleCode().isBlank()
                || publication.getRuleVersion().isBlank()
                || publication.getChecksum().isBlank()) {
            return;
        }
        BoundaryRuleUpdate delete = BoundaryRuleUpdate.delete(
                publication.getTenantId(),
                publication.getPlantId(),
                publication.getLineId(),
                publication.getRuleCode(),
                publication.getRuleVersion());
        byte[] previousStatus = runtimeStatus.get(delete.ruleRef().key());
        if (previousStatus == null || RuleRuntimeReadinessProjector.ready(previousStatus)) {
            emit(context, RULE_UPDATES, BoundaryRuleUpdateCodec.encode(delete));
        }
        String safeDetail = detail == null || detail.isBlank()
                ? "stored rule publication cannot be mapped by the current runtime"
                : detail;
        byte[] receipt = RuleRuntimeReadinessProjector.project(
                publication,
                deploymentId,
                BoundaryRuleRuntimeReadinessStatusV1.DEGRADED,
                "RULE_PUBLICATION_RUNTIME_REJECTED",
                safeDetail,
                catalog.getObservedAtMs(),
                catalog);
        if (!RuleRuntimeReadinessProjector.sameObservation(previousStatus, receipt)) {
            emit(context, RUNTIME_READINESS, receipt);
        }
        runtimeStatus.put(delete.ruleRef().key(), receipt);
    }

    private void reconcileRule(
            Context context,
            PublishedBoundaryPlan plan,
            BroadcastState<String, byte[]> routes,
            BroadcastState<String, byte[]> catalogs,
            BroadcastState<String, byte[]> points,
            BroadcastState<String, byte[]> runtimeStatus) throws Exception {
        String ruleKey = plan.ruleRef().key();
        byte[] previousStatus = runtimeStatus.get(ruleKey);
        boolean knownStatus = previousStatus != null;
        boolean wasReady = RuleRuntimeReadinessProjector.ready(previousStatus);
        if (wasReady) {
            removeRoutes(routes, plan);
        }
        BoundaryRoutingIssue readinessIssue = plan.publication().getActive()
                ? catalogBindingsReadinessIssue(plan, catalogs, points)
                : null;
        boolean isReady = plan.publication().getActive() && readinessIssue == null;
        if (isReady) {
            addRoutes(routes, plan);
        }
        PointCatalogSnapshotV1 catalog = currentCatalog(plan.publication(), catalogs);
        BoundaryRuleRuntimeReadinessStatusV1 status = !plan.publication().getActive()
                ? BoundaryRuleRuntimeReadinessStatusV1.INACTIVE
                : isReady
                        ? BoundaryRuleRuntimeReadinessStatusV1.READY
                        : BoundaryRuleRuntimeReadinessStatusV1.DEGRADED;
        String reasonCode = status == BoundaryRuleRuntimeReadinessStatusV1.INACTIVE
                ? "RULE_INACTIVE"
                : readinessIssue == null ? "" : readinessIssue.code();
        String detail = status == BoundaryRuleRuntimeReadinessStatusV1.INACTIVE
                ? "published rule version is inactive"
                : readinessIssue == null ? "" : readinessIssue.message();
        long observedAtMs = catalog == null
                ? plan.publication().getPublishedAtMs()
                : catalog.getObservedAtMs();
        byte[] receipt = RuleRuntimeReadinessProjector.project(
                plan.publication(), deploymentId, status, reasonCode, detail, observedAtMs, catalog);
        boolean readinessChanged =
                !RuleRuntimeReadinessProjector.sameObservation(previousStatus, receipt);
        runtimeStatus.put(ruleKey, receipt);
        if (!knownStatus || isReady != wasReady) {
            BoundaryRuleUpdate update = isReady
                    ? plan.ruleUpdate()
                    : BoundaryRuleUpdate.delete(
                            plan.publication().getTenantId(),
                            plan.publication().getPlantId(),
                            plan.publication().getLineId(),
                            plan.rule().ruleCode(),
                            plan.rule().ruleVersion());
            emit(context, RULE_UPDATES, BoundaryRuleUpdateCodec.encode(update));
        }
        if (readinessChanged) {
            emit(context, RUNTIME_READINESS, receipt);
            if (readinessIssue != null) emit(context, ISSUES, readinessIssue);
        }
    }

    private <T> void emit(Context context, OutputTag<T> tag, T value) {
        if (controlPlaneEmitter) context.output(tag, value);
    }

    private static PointCatalogSnapshotV1 currentCatalog(
            BoundaryRulePublicationV1 publication,
            BroadcastState<String, byte[]> catalogs) throws Exception {
        String scopeKey = String.join(
                "|", publication.getTenantId(), publication.getPlantId(), publication.getLineId());
        byte[] catalogBytes = catalogs.get(scopeKey);
        return catalogBytes == null ? null : PointCatalogSnapshotV1.parseFrom(catalogBytes);
    }

    private static BoundaryRoutingIssue catalogBindingsReadinessIssue(
            PublishedBoundaryPlan plan,
            BroadcastState<String, byte[]> catalogs,
            BroadcastState<String, byte[]> points) throws Exception {
        BoundaryRulePublicationV1 publication = plan.publication();
        String scopeKey = String.join(
                "|", publication.getTenantId(), publication.getPlantId(), publication.getLineId());
        if (catalogs.get(scopeKey) == null) {
            return publicationIssue(
                    publication,
                    null,
                    "POINT_CATALOG_RUNTIME_MISSING",
                    "no point catalog has been observed for this rule scope");
        }
        for (BoundarySignalBindingV1 binding : publication.getSignalBindingsList()) {
            byte[] pointBytes = points.get(PointCatalogRuntimeValidator.pointKey(
                    publication.getTenantId(), publication.getPlantId(), publication.getLineId(),
                    binding.getProductId(), binding.getDeviceId(), binding.getPropertyId()));
            if (pointBytes == null) {
                return publicationIssue(
                        publication,
                        binding,
                        "POINT_CATALOG_BINDING_MISSING",
                        "bound product/device/property is absent from the latest point catalog");
            }
            PointCatalogPointV1 point = PointCatalogPointV1.parseFrom(pointBytes);
            BoundaryRoutingIssue issue = catalogPointReadinessIssue(publication, binding, point);
            if (issue != null) {
                return issue;
            }
        }
        return null;
    }

    private static BoundaryRoutingIssue catalogPointReadinessIssue(
            BoundaryRulePublicationV1 publication,
            BoundarySignalBindingV1 binding,
            PointCatalogPointV1 point) {
        if (!point.getRegistered()) {
            return publicationIssue(
                    publication, binding, "POINT_DEVICE_NOT_REGISTERED", "bound device is not registered");
        }
        if (point.getDeviceState() != PointDeviceStateV1.POINT_DEVICE_ACTIVE) {
            return publicationIssue(
                    publication, binding, "POINT_DEVICE_NOT_ACTIVE", "bound device is not active");
        }
        if (!point.getPropertyPresent()) {
            return publicationIssue(
                    publication,
                    binding,
                    "POINT_PROPERTY_NOT_AVAILABLE",
                    "bound property is absent from product metadata");
        }
        if (point.getUnit().isBlank()
                || (!binding.getExpectedUnit().isBlank()
                    && !UnitSymbolNormalizer.equivalent(point.getUnit(), binding.getExpectedUnit()))) {
            return publicationIssue(
                    publication,
                    binding,
                    "POINT_UNIT_MISMATCH",
                    "catalog unit does not match the published binding");
        }
        if (!binding.getCalibrationEvidenceId().isBlank()
                && binding.getCalibrationValidUntilMs() <= System.currentTimeMillis()) {
            return publicationIssue(
                    publication,
                    binding,
                    "POINT_CALIBRATION_EVIDENCE_EXPIRED",
                    "authoritative MES calibration evidence has expired");
        }
        if (binding.getCalibrationEvidenceId().isBlank()
                && point.getCalibrationStatus() != PointCalibrationStatusV1.POINT_CALIBRATION_VERIFIED) {
            return publicationIssue(
                    publication,
                    binding,
                    "POINT_CALIBRATION_NOT_VERIFIED",
                    "catalog calibration is not verified");
        }
        if (!point.getCalibrationVersion().equals(binding.getCalibrationVersion())) {
            return publicationIssue(
                    publication,
                    binding,
                    "POINT_CALIBRATION_VERSION_MISMATCH",
                    "catalog calibration version differs from the published binding");
        }
        if (!point.getSourceSequenceEnabled()) {
            return publicationIssue(
                    publication,
                    binding,
                    "POINT_SOURCE_SEQUENCE_DISABLED",
                    "catalog source sequence is not ready");
        }
        return null;
    }

    private static boolean sameScope(
            BoundaryRulePublicationV1 publication,
            PointCatalogSnapshotV1 snapshot) {
        return publication.getTenantId().equals(snapshot.getTenantId())
                && publication.getPlantId().equals(snapshot.getPlantId())
                && publication.getLineId().equals(snapshot.getLineId());
    }

    private static void addRoutes(BroadcastState<String, byte[]> routes, PublishedBoundaryPlan plan)
            throws Exception {
        for (BoundarySignalBindingV1 binding : plan.publication().getSignalBindingsList()) {
            String routeKey = routeKey(plan.publication(), binding);
            List<String> ruleKeys = readRoute(routes.get(routeKey));
            if (!ruleKeys.contains(plan.ruleRef().key())) {
                ruleKeys.add(plan.ruleRef().key());
                routes.put(routeKey, BoundaryRouteIndexCodec.encode(ruleKeys));
            }
        }
    }

    private static void removeRoutes(BroadcastState<String, byte[]> routes, PublishedBoundaryPlan plan)
            throws Exception {
        for (BoundarySignalBindingV1 binding : plan.publication().getSignalBindingsList()) {
            String routeKey = routeKey(plan.publication(), binding);
            List<String> ruleKeys = readRoute(routes.get(routeKey));
            if (ruleKeys.remove(plan.ruleRef().key())) {
                if (ruleKeys.isEmpty()) {
                    routes.remove(routeKey);
                } else {
                    routes.put(routeKey, BoundaryRouteIndexCodec.encode(ruleKeys));
                }
            }
        }
    }

    private static List<String> readRoute(byte[] encoded) {
        return encoded == null
                ? new ArrayList<>()
                : new ArrayList<>(BoundaryRouteIndexCodec.decode(encoded));
    }

    private static String routeKey(ContextualTelemetryPoint contextual) {
        TelemetryEnvelopeV1 envelope = contextual.telemetry().envelope();
        return String.join(
                "|",
                envelope.getTenantId(),
                envelope.getPlantId(),
                envelope.getLineId(),
                envelope.getProductId(),
                envelope.getDeviceId(),
                contextual.telemetry().point().getPropertyId());
    }

    private static String routeKey(
            BoundaryRulePublicationV1 publication,
            BoundarySignalBindingV1 binding) {
        return String.join(
                "|",
                publication.getTenantId(),
                publication.getPlantId(),
                publication.getLineId(),
                binding.getProductId(),
                binding.getDeviceId(),
                binding.getPropertyId());
    }

    private static String bindingKey(ContextualTelemetryPoint contextual) {
        TelemetryEnvelopeV1 envelope = contextual.telemetry().envelope();
        return PublishedBoundaryPlan.bindingKey(
                envelope.getProductId(), envelope.getDeviceId(), contextual.telemetry().point().getPropertyId());
    }

    private static BoundaryRoutingIssue readinessIssue(
            ContextualTelemetryPoint contextual,
            BoundarySignalBindingV1 binding,
            ReadOnlyBroadcastState<String, byte[]> catalogs,
            ReadOnlyBroadcastState<String, byte[]> points) throws Exception {
        TelemetryEnvelopeV1 envelope = contextual.telemetry().envelope();
        PointValue telemetryPoint = contextual.telemetry().point();
        if (binding == null) {
            return issue(contextual, "RULE_BINDING_MISSING", "route has no exact product/device/property binding");
        }
        String scopeKey = String.join("|", envelope.getTenantId(), envelope.getPlantId(), envelope.getLineId());
        if (catalogs.get(scopeKey) == null) {
            return issue(contextual, "POINT_CATALOG_RUNTIME_MISSING", "no point catalog has been observed for this scope");
        }
        String pointKey = PointCatalogRuntimeValidator.pointKey(
                envelope.getTenantId(), envelope.getPlantId(), envelope.getLineId(),
                envelope.getProductId(), envelope.getDeviceId(), telemetryPoint.getPropertyId());
        byte[] pointBytes = points.get(pointKey);
        if (pointBytes == null) {
            return issue(contextual, "POINT_CATALOG_BINDING_MISSING", "point is absent from the latest catalog snapshot");
        }
        PointCatalogPointV1 point = PointCatalogPointV1.parseFrom(pointBytes);
        if (!point.getRegistered()) {
            return issue(contextual, "POINT_DEVICE_NOT_REGISTERED", "bound device is not registered");
        }
        if (point.getDeviceState() != PointDeviceStateV1.POINT_DEVICE_ACTIVE) {
            return issue(contextual, "POINT_DEVICE_NOT_ACTIVE", "bound device is not active");
        }
        if (!point.getPropertyPresent()) {
            return issue(contextual, "POINT_PROPERTY_NOT_AVAILABLE", "bound property is absent from product metadata");
        }
        if (point.getUnit().isBlank()
                || (!binding.getExpectedUnit().isBlank()
                    && !UnitSymbolNormalizer.equivalent(point.getUnit(), binding.getExpectedUnit()))) {
            return issue(contextual, "POINT_UNIT_MISMATCH", "catalog unit does not match the published binding");
        }
        if (!binding.getCalibrationEvidenceId().isBlank()
                && binding.getCalibrationValidUntilMs() <= System.currentTimeMillis()) {
            return issue(contextual, "POINT_CALIBRATION_EVIDENCE_EXPIRED",
                    "authoritative MES calibration evidence has expired");
        }
        if (binding.getCalibrationEvidenceId().isBlank()
                && point.getCalibrationStatus() != PointCalibrationStatusV1.POINT_CALIBRATION_VERIFIED) {
            return issue(contextual, "POINT_CALIBRATION_NOT_VERIFIED", "catalog calibration is not verified");
        }
        if (!point.getCalibrationVersion().equals(binding.getCalibrationVersion())) {
            return issue(contextual, "POINT_CALIBRATION_VERSION_MISMATCH",
                    "catalog calibration version differs from the published binding");
        }
        if (!telemetryPoint.getCalibrationVersion().equals(binding.getCalibrationVersion())) {
            return issue(contextual, "TELEMETRY_CALIBRATION_VERSION_MISMATCH",
                    "telemetry calibration version differs from the published binding");
        }
        if (!point.getSourceSequenceEnabled()) {
            return issue(contextual, "POINT_SOURCE_SEQUENCE_DISABLED", "catalog source sequence is not ready");
        }
        if ((envelope.getSequenceOrigin() != SequenceOrigin.DEVICE
                && envelope.getSequenceOrigin() != SequenceOrigin.GATEWAY)
                || envelope.getSourceEpoch() == 0
                || envelope.getSequence() == 0) {
            return issue(contextual, "TELEMETRY_SOURCE_SEQUENCE_NOT_AUTHORITATIVE",
                    "telemetry requires a positive device/gateway source epoch and sequence");
        }
        return null;
    }

    private static BoundaryRoutingIssue catalogIssue(
            PointCatalogSnapshotV1 snapshot,
            String code,
            String message) {
        return new BoundaryRoutingIssue(
                code,
                snapshot.getEventId(),
                "",
                message,
                snapshot.getTenantId(),
                snapshot.getPlantId(),
                snapshot.getLineId());
    }

    private static BoundaryRoutingIssue publicationIssue(
            BoundaryRulePublicationV1 publication,
            BoundarySignalBindingV1 binding,
            String code,
            String message) {
        return new BoundaryRoutingIssue(
                code,
                publication.getEventId(),
                binding == null ? "" : binding.getPropertyId(),
                message,
                publication.getTenantId(),
                publication.getPlantId(),
                publication.getLineId());
    }

    private static BoundaryRoutingIssue issue(
            ContextualTelemetryPoint contextual,
            String code,
            String message) {
        TelemetryEnvelopeV1 envelope = contextual.telemetry().envelope();
        return new BoundaryRoutingIssue(
                code,
                envelope.getEventId(),
                contextual.telemetry().point().getPropertyId(),
                message,
                envelope.getTenantId(),
                envelope.getPlantId(),
                envelope.getLineId());
    }

}
