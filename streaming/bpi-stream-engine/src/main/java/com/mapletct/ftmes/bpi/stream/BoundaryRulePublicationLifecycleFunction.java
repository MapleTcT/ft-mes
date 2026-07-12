package com.mapletct.ftmes.bpi.stream;

import com.google.protobuf.InvalidProtocolBufferException;
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
    private static final ValueStateDescriptor<byte[]> LAST_PUBLICATION = new ValueStateDescriptor<>(
            "bpi-boundary-rule-lifecycle-v1", byte[].class);

    private final Duration boundaryStateTtl;
    private transient ValueState<byte[]> lastPublication;

    public BoundaryRulePublicationLifecycleFunction() {
        this(Duration.ofDays(30));
    }

    public BoundaryRulePublicationLifecycleFunction(Duration boundaryStateTtl) {
        if (boundaryStateTtl == null || boundaryStateTtl.isZero() || boundaryStateTtl.isNegative()) {
            throw new IllegalArgumentException("boundaryStateTtl must be positive");
        }
        this.boundaryStateTtl = boundaryStateTtl;
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
            return;
        }
        Duration requiredStateHorizon = plan.rule().timing().evaluationTimeout()
                .plus(plan.rule().timing().allowedLateness());
        if (requiredStateHorizon.compareTo(boundaryStateTtl) >= 0) {
            issue(context, "RULE_WINDOW_EXCEEDS_STATE_TTL", incoming.getEventId(),
                    "evaluation timeout plus allowed lateness must be less than boundary state TTL");
            return;
        }
        String ruleKey = BoundaryRulePublicationSemantics.key(incoming);
        if (!ruleKey.equals(context.getCurrentKey())) {
            issue(context, "RULE_KEY_MISMATCH", incoming.getEventId(),
                    "publication identity does not match its keyed partition");
            return;
        }
        byte[] existingBytes = lastPublication.value();
        if (existingBytes == null) {
            storeAndEmit(bytes, output);
            return;
        }
        if (Arrays.equals(existingBytes, bytes)) {
            return;
        }
        BoundaryRulePublicationV1 existing = BoundaryRulePublicationV1.parseFrom(existingBytes);
        if (!BoundaryRulePublicationSemantics.equivalent(existing, incoming)) {
            issue(context, "RULE_VERSION_CONFLICT", incoming.getEventId(),
                    "published rule versions are immutable; use a new version");
            return;
        }
        if (incoming.getPublishedAtMs() < existing.getPublishedAtMs()) {
            issue(context, "RULE_PUBLICATION_OUT_OF_ORDER", incoming.getEventId(),
                    "publication time is older than the current lifecycle state");
            return;
        }
        if (!existing.getActive()) {
            if (incoming.getActive()) {
                issue(context, "RULE_REACTIVATION_REQUIRES_NEW_VERSION", incoming.getEventId(),
                        "an inactive rule version cannot be reactivated");
            }
            return;
        }
        if (incoming.getActive()) {
            return;
        }
        storeAndEmit(bytes, output);
    }

    private void storeAndEmit(byte[] bytes, Collector<byte[]> output) throws Exception {
        byte[] copy = Arrays.copyOf(bytes, bytes.length);
        lastPublication.update(copy);
        output.collect(copy);
    }

    private static void issue(Context context, String code, String eventId, String message) {
        context.output(ISSUES, new BoundaryRoutingIssue(code, eventId, "", message));
    }
}
