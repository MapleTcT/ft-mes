package com.mapletct.ftmes.bpi.stream;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRulePublicationV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundarySignalBindingV1;
import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ReadOnlyBroadcastState;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Routes each contextual point by an indexed device/property lookup instead of scanning all rules. */
public final class BoundaryRuleRoutingBroadcastFunction extends BroadcastProcessFunction<
        byte[],
        byte[],
        BoundaryStreamInput> {

    public static final MapStateDescriptor<String, byte[]> PUBLICATIONS = new MapStateDescriptor<>(
            "bpi-boundary-publications-v1", String.class, byte[].class);
    public static final MapStateDescriptor<String, byte[]> ROUTES = new MapStateDescriptor<>(
            "bpi-boundary-route-index-v1", String.class, byte[].class);
    public static final OutputTag<BoundaryRoutingIssue> ISSUES =
            new OutputTag<>("bpi-boundary-routing-issues") {
            };

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
            byte[] publicationBytes,
            Context context,
            Collector<BoundaryStreamInput> output) throws Exception {
        BoundaryRulePublicationV1 publication;
        PublishedBoundaryPlan incoming;
        try {
            publication = BoundaryRulePublicationV1.parseFrom(publicationBytes);
            incoming = BoundaryRulePublicationMapper.map(publication);
        } catch (InvalidProtocolBufferException | IllegalArgumentException | IllegalStateException error) {
            context.output(ISSUES, new BoundaryRoutingIssue(
                    "RULE_PUBLICATION_REJECTED", "", "", error.getMessage()));
            return;
        }
        String ruleKey = incoming.ruleRef().key();
        BroadcastState<String, byte[]> publications = context.getBroadcastState(PUBLICATIONS);
        BroadcastState<String, byte[]> routes = context.getBroadcastState(ROUTES);
        byte[] existingBytes = publications.get(ruleKey);
        if (existingBytes != null) {
            BoundaryRulePublicationV1 existing = BoundaryRulePublicationV1.parseFrom(existingBytes);
            if (!BoundaryRulePublicationSemantics.equivalent(existing, publication)) {
                context.output(ISSUES, new BoundaryRoutingIssue(
                        "RULE_VERSION_CONFLICT", publication.getEventId(), "",
                        "published rule versions are immutable; use a new version"));
                return;
            }
            if (!existing.getActive() && publication.getActive()) {
                context.output(ISSUES, new BoundaryRoutingIssue(
                        "RULE_REACTIVATION_REQUIRES_NEW_VERSION", publication.getEventId(), "",
                        "an inactive rule version cannot be reactivated"));
                return;
            }
            if (publication.getPublishedAtMs() < existing.getPublishedAtMs()) {
                context.output(ISSUES, new BoundaryRoutingIssue(
                        "RULE_PUBLICATION_OUT_OF_ORDER", publication.getEventId(), "",
                        "publication time is older than the current lifecycle state"));
                return;
            }
            removeRoutes(routes, BoundaryRulePublicationMapper.map(existing));
        }
        publications.put(ruleKey, Arrays.copyOf(publicationBytes, publicationBytes.length));
        if (publication.getActive()) {
            addRoutes(routes, incoming);
        }
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
        return String.join(
                "|",
                contextual.telemetry().envelope().getTenantId(),
                contextual.telemetry().envelope().getPlantId(),
                contextual.telemetry().envelope().getLineId(),
                contextual.telemetry().envelope().getDeviceId(),
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
                binding.getDeviceId(),
                binding.getPropertyId());
    }

    private static BoundaryRoutingIssue issue(
            ContextualTelemetryPoint contextual,
            String code,
            String message) {
        return new BoundaryRoutingIssue(
                code,
                contextual.telemetry().envelope().getEventId(),
                contextual.telemetry().point().getPropertyId(),
                message);
    }
}
