package com.mapletct.ftmes.bpi.stream;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleApplicationStatusV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRulePublicationV1;
import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

import java.util.Arrays;
import java.time.Duration;

/** Enforces immutable, ordered rule-version lifecycle before publications enter broadcast state. */
public final class BoundaryRulePublicationLifecycleFunction extends KeyedProcessFunction<
        String,
        byte[],
        byte[]> {

    public static final OutputTag<BoundaryRoutingIssue> ISSUES =
            new OutputTag<>("bpi-boundary-rule-lifecycle-issues") {
            };
    public static final OutputTag<byte[]> APPLICATIONS =
            new OutputTag<>("bpi-boundary-rule-applications") {
            };
    private static final ValueStateDescriptor<byte[]> LAST_PUBLICATION = new ValueStateDescriptor<>(
            "bpi-boundary-rule-lifecycle-v1", byte[].class);

    private final Duration boundaryStateTtl;
    private final String deploymentId;
    private transient ValueState<byte[]> lastPublication;

    public BoundaryRulePublicationLifecycleFunction() {
        this(Duration.ofDays(30), "local");
    }

    public BoundaryRulePublicationLifecycleFunction(Duration boundaryStateTtl) {
        this(boundaryStateTtl, "local");
    }

    public BoundaryRulePublicationLifecycleFunction(Duration boundaryStateTtl, String deploymentId) {
        if (boundaryStateTtl == null || boundaryStateTtl.isZero() || boundaryStateTtl.isNegative()) {
            throw new IllegalArgumentException("boundaryStateTtl must be positive");
        }
        if (deploymentId == null || deploymentId.isBlank()) {
            throw new IllegalArgumentException("deploymentId is required");
        }
        this.boundaryStateTtl = boundaryStateTtl;
        this.deploymentId = deploymentId;
    }

    @Override
    public void open(OpenContext openContext) {
        lastPublication = getRuntimeContext().getState(LAST_PUBLICATION);
    }

    @Override
    public void processElement(
            byte[] bytes,
            Context context,
            Collector<byte[]> output) throws Exception {
        BoundaryRulePublicationV1 incoming;
        PublishedBoundaryPlan plan;
        try {
            incoming = BoundaryRulePublicationV1.parseFrom(bytes);
            plan = BoundaryRulePublicationMapper.map(incoming);
        } catch (InvalidProtocolBufferException | IllegalArgumentException | IllegalStateException error) {
            issue(context, "RULE_PUBLICATION_REJECTED", "", error.getMessage());
            rejectIfIdentifiable(context, bytes, "RULE_PUBLICATION_REJECTED", error.getMessage());
            return;
        }
        Duration requiredStateHorizon = plan.rule().timing().evaluationTimeout()
                .plus(plan.rule().timing().allowedLateness());
        if (requiredStateHorizon.compareTo(boundaryStateTtl) >= 0) {
            reject(context, incoming, "RULE_WINDOW_EXCEEDS_STATE_TTL",
                    "evaluation timeout plus allowed lateness must be less than boundary state TTL");
            return;
        }
        String ruleKey = BoundaryRulePublicationSemantics.key(incoming);
        if (!ruleKey.equals(context.getCurrentKey())) {
            reject(context, incoming, "RULE_KEY_MISMATCH",
                    "publication identity does not match its keyed partition");
            return;
        }
        byte[] existingBytes = lastPublication.value();
        if (existingBytes == null) {
            storeAndEmit(context, incoming, bytes, output);
            return;
        }
        if (Arrays.equals(existingBytes, bytes)) {
            apply(context, incoming);
            return;
        }
        BoundaryRulePublicationV1 existing = BoundaryRulePublicationV1.parseFrom(existingBytes);
        if (!BoundaryRulePublicationSemantics.equivalent(existing, incoming)) {
            reject(context, incoming, "RULE_VERSION_CONFLICT",
                    "published rule versions are immutable; use a new version");
            return;
        }
        if (incoming.getPublishedAtMs() < existing.getPublishedAtMs()) {
            reject(context, incoming, "RULE_PUBLICATION_OUT_OF_ORDER",
                    "publication time is older than the current lifecycle state");
            return;
        }
        if (!existing.getActive()) {
            if (incoming.getActive()) {
                reject(context, incoming, "RULE_REACTIVATION_REQUIRES_NEW_VERSION",
                        "an inactive rule version cannot be reactivated");
            } else {
                apply(context, incoming);
            }
            return;
        }
        if (incoming.getActive()) {
            apply(context, incoming);
            return;
        }
        storeAndEmit(context, incoming, bytes, output);
    }

    private void storeAndEmit(
            Context context,
            BoundaryRulePublicationV1 publication,
            byte[] bytes,
            Collector<byte[]> output) throws Exception {
        byte[] copy = Arrays.copyOf(bytes, bytes.length);
        lastPublication.update(copy);
        output.collect(copy);
        apply(context, publication);
    }

    private void apply(Context context, BoundaryRulePublicationV1 publication) {
        context.output(APPLICATIONS, RuleApplicationProjector.project(
                publication,
                deploymentId,
                BoundaryRuleApplicationStatusV1.APPLIED,
                "",
                "",
                processingTime(context)));
    }

    private void reject(
            Context context,
            BoundaryRulePublicationV1 publication,
            String code,
            String message) {
        issue(context, code, publication.getEventId(), message);
        context.output(APPLICATIONS, RuleApplicationProjector.project(
                publication,
                deploymentId,
                BoundaryRuleApplicationStatusV1.REJECTED,
                code,
                message,
                processingTime(context)));
    }

    private void rejectIfIdentifiable(Context context, byte[] bytes, String code, String message) {
        try {
            BoundaryRulePublicationV1 publication = BoundaryRulePublicationV1.parseFrom(bytes);
            if (!publication.getEventId().isBlank()) {
                context.output(APPLICATIONS, RuleApplicationProjector.project(
                        publication,
                        deploymentId,
                        BoundaryRuleApplicationStatusV1.REJECTED,
                        code,
                        message,
                        processingTime(context)));
            }
        } catch (InvalidProtocolBufferException ignored) {
            // A malformed payload has no trustworthy publication identity to acknowledge.
        }
    }

    private static long processingTime(Context context) {
        return Math.max(1L, context.timerService().currentProcessingTime());
    }

    private static void issue(Context context, String code, String eventId, String message) {
        context.output(ISSUES, new BoundaryRoutingIssue(code, eventId, "", message));
    }
}
