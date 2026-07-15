package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.contract.v1.PointValue;
import com.mapletct.ftmes.bpi.contract.v1.ProductionContextEventV1;
import com.mapletct.ftmes.bpi.contract.v1.TelemetryEnvelopeV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundarySignalBindingV1;
import com.mapletct.ftmes.bpi.rules.BoundaryKind;
import com.mapletct.ftmes.bpi.rules.SignalObservation;
import com.mapletct.ftmes.bpi.rules.SignalQuality;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class BoundarySignalRouter {

    private BoundarySignalRouter() {
    }

    public static BoundaryRoutingResult route(
            PublishedBoundaryPlan plan,
            ProductionContextEventV1 context,
            TelemetryEnvelopeV1 envelope) {
        List<BoundaryRoutingIssue> issues = new ArrayList<>();
        List<BoundaryStreamInput> inputs = new ArrayList<>();
        if (!plan.publication().getActive()) {
            return new BoundaryRoutingResult(inputs, issues);
        }
        if (!sameScope(plan, context, envelope)) {
            issues.add(issue(envelope, "SCOPE_MISMATCH", "", "rule, context and telemetry scope differ"));
            return new BoundaryRoutingResult(inputs, issues);
        }
        for (PointValue point : envelope.getPointsList()) {
            BoundarySignalBindingV1 binding = plan.bindings().get(
                    PublishedBoundaryPlan.bindingKey(
                            envelope.getProductId(), envelope.getDeviceId(), point.getPropertyId()));
            if (binding == null) {
                continue;
            }
            long eventTimeMs = point.getSampleTimeMs() > 0
                    ? point.getSampleTimeMs()
                    : envelope.getEventTimeMs();
            if (!contextApplies(context, eventTimeMs)) {
                issues.add(issue(envelope, "CONTEXT_NOT_EFFECTIVE", point.getPropertyId(),
                        "production context is inactive or outside the signal event time"));
                continue;
            }
            if (!binding.getExpectedUnit().isBlank()
                    && !binding.getExpectedUnit().equals(point.getUnit())) {
                issues.add(issue(envelope, "UNIT_MISMATCH", point.getPropertyId(),
                        "point unit does not match the published binding"));
                continue;
            }
            try {
                inputs.add(input(plan, context, envelope, point, binding, eventTimeMs));
            } catch (IllegalArgumentException error) {
                issues.add(issue(envelope, "POINT_REJECTED", point.getPropertyId(), error.getMessage()));
            }
        }
        return new BoundaryRoutingResult(inputs, issues);
    }

    public static BoundaryRoutingResult route(
            PublishedBoundaryPlan plan,
            ContextualTelemetryPoint contextualPoint) {
        TelemetryPointEvent telemetry = contextualPoint.telemetry();
        TelemetryEnvelopeV1 envelope = telemetry.envelope();
        ProductionContextEventV1 context = contextualPoint.context();
        List<BoundaryRoutingIssue> issues = new ArrayList<>();
        List<BoundaryStreamInput> inputs = new ArrayList<>();
        if (!plan.publication().getActive()) {
            return new BoundaryRoutingResult(inputs, issues);
        }
        if (!sameScope(plan, context, envelope)) {
            issues.add(issue(envelope, "SCOPE_MISMATCH", telemetry.point().getPropertyId(),
                    "rule, context and telemetry scope differ"));
            return new BoundaryRoutingResult(inputs, issues);
        }
        PointValue point = telemetry.point();
        BoundarySignalBindingV1 binding = plan.bindings().get(
                PublishedBoundaryPlan.bindingKey(
                        envelope.getProductId(), envelope.getDeviceId(), point.getPropertyId()));
        if (binding == null) {
            return new BoundaryRoutingResult(inputs, issues);
        }
        long eventTimeMs = telemetry.eventTime().toEpochMilli();
        if (!contextApplies(context, eventTimeMs)) {
            issues.add(issue(envelope, "CONTEXT_NOT_EFFECTIVE", point.getPropertyId(),
                    "production context is inactive or outside the signal event time"));
            return new BoundaryRoutingResult(inputs, issues);
        }
        if (!binding.getExpectedUnit().isBlank()
                && !binding.getExpectedUnit().equals(point.getUnit())) {
            issues.add(issue(envelope, "UNIT_MISMATCH", point.getPropertyId(),
                    "point unit does not match the published binding"));
            return new BoundaryRoutingResult(inputs, issues);
        }
        try {
            inputs.add(input(plan, context, envelope, point, binding, eventTimeMs));
        } catch (IllegalArgumentException error) {
            issues.add(issue(envelope, "POINT_REJECTED", point.getPropertyId(), error.getMessage()));
        }
        return new BoundaryRoutingResult(inputs, issues);
    }

    private static BoundaryStreamInput input(
            PublishedBoundaryPlan plan,
            ProductionContextEventV1 context,
            TelemetryEnvelopeV1 envelope,
            PointValue point,
            BoundarySignalBindingV1 binding,
            long eventTimeMs) {
        SignalQuality quality;
        try {
            quality = SignalQuality.valueOf(point.getQualityCode());
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("unsupported quality_code: " + point.getQualityCode(), error);
        }
        Instant eventTime = Instant.ofEpochMilli(eventTimeMs);
        SignalObservation observation = switch (point.getValueCase()) {
            case DOUBLE_VALUE -> SignalObservation.numeric(
                    envelope.getEventId(), binding.getSignal(),
                    BigDecimal.valueOf(point.getDoubleValue()), quality, eventTime);
            case LONG_VALUE -> SignalObservation.numeric(
                    envelope.getEventId(), binding.getSignal(),
                    BigDecimal.valueOf(point.getLongValue()), quality, eventTime);
            case BOOL_VALUE -> SignalObservation.bool(
                    envelope.getEventId(), binding.getSignal(), point.getBoolValue(), quality, eventTime);
            default -> throw new IllegalArgumentException("point value type cannot drive a boundary signal");
        };
        BoundaryExecutionContext executionContext = new BoundaryExecutionContext(
                envelope.getTenantId(),
                envelope.getPlantId(),
                envelope.getLineId(),
                plan.publication().getLocalityGroup(),
                plan.publication().getTopologyCode(),
                plan.publication().getTopologyVersion(),
                emptyToNull(context.getOrderId()),
                emptyToNull(context.getBatchId()));
        if (plan.rule().boundaryKind() == BoundaryKind.START && executionContext.contextOrderId() == null) {
            throw new IllegalArgumentException("START binding requires an active production order");
        }
        if (plan.rule().boundaryKind() == BoundaryKind.END && executionContext.batchId() == null) {
            throw new IllegalArgumentException("END binding requires an active batch identity");
        }
        return new BoundaryStreamInput(executionContext, plan.ruleRef(), plan.rule().boundaryKind(), observation);
    }

    private static boolean sameScope(
            PublishedBoundaryPlan plan,
            ProductionContextEventV1 context,
            TelemetryEnvelopeV1 envelope) {
        return plan.publication().getTenantId().equals(envelope.getTenantId())
                && plan.publication().getPlantId().equals(envelope.getPlantId())
                && plan.publication().getLineId().equals(envelope.getLineId())
                && context.getTenantId().equals(envelope.getTenantId())
                && context.getPlantId().equals(envelope.getPlantId())
                && context.getLineId().equals(envelope.getLineId());
    }

    private static boolean contextApplies(ProductionContextEventV1 context, long eventTimeMs) {
        return context.getActive()
                && eventTimeMs >= context.getEffectiveFromMs()
                && (context.getEffectiveToMs() == 0 || eventTimeMs < context.getEffectiveToMs());
    }

    private static BoundaryRoutingIssue issue(
            TelemetryEnvelopeV1 envelope,
            String code,
            String propertyId,
            String message) {
        return new BoundaryRoutingIssue(
                code,
                envelope.getEventId(),
                propertyId,
                message,
                envelope.getTenantId(),
                envelope.getPlantId(),
                envelope.getLineId());
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
