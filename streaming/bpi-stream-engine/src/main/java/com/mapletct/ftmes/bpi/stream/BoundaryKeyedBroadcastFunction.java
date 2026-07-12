package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.rules.BoundaryRuleDefinition;
import com.mapletct.ftmes.bpi.rules.BoundaryKind;
import com.mapletct.ftmes.bpi.rules.BoundaryWindowEvaluator;
import com.mapletct.ftmes.bpi.rules.BoundaryWindowResult;
import com.mapletct.ftmes.bpi.rules.BoundaryWindowState;
import com.mapletct.ftmes.bpi.rules.ConditionStatus;
import com.mapletct.ftmes.bpi.rules.EvidenceCondition;
import com.mapletct.ftmes.bpi.rules.EvidenceSignalState;
import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ReadOnlyBroadcastState;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.streaming.api.functions.co.KeyedBroadcastProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

import java.time.Instant;
import java.util.Arrays;

public final class BoundaryKeyedBroadcastFunction extends KeyedBroadcastProcessFunction<
        String,
        BoundaryStreamInput,
        BoundaryRuleUpdate,
        byte[]> {

    public static final MapStateDescriptor<String, byte[]> RULES = new MapStateDescriptor<>(
            "bpi-boundary-rules-v1", String.class, byte[].class);
    public static final OutputTag<BoundaryProcessingIssue> ISSUES =
            new OutputTag<>("bpi-boundary-processing-issues") {
            };

    private static final ValueStateDescriptor<byte[]> WINDOW_STATE = new ValueStateDescriptor<>(
            "bpi-boundary-window-v1", byte[].class);

    private transient ValueState<byte[]> encodedState;

    @Override
    public void open(OpenContext openContext) {
        encodedState = getRuntimeContext().getState(WINDOW_STATE);
    }

    @Override
    public void processElement(
            BoundaryStreamInput input,
            ReadOnlyContext context,
            Collector<byte[]> output) throws Exception {
        if (!input.keyedLocality().equals(context.getCurrentKey())) {
            issue(context, input, "KEY_MISMATCH", "stream key does not match the input locality");
            return;
        }
        if (input.boundaryKind() == BoundaryKind.START && blank(input.context().contextOrderId())) {
            issue(context, input, "CONTEXT_ID_MISSING", "START input requires a production order identity");
            return;
        }
        if (input.boundaryKind() == BoundaryKind.END && blank(input.context().batchId())) {
            issue(context, input, "CONTEXT_ID_MISSING", "END input requires an active batch identity");
            return;
        }
        Long streamTimestamp = context.timestamp();
        long eventTime = input.observation().eventTime().toEpochMilli();
        if (streamTimestamp == null || streamTimestamp != eventTime) {
            issue(context, input, "TIMESTAMP_MISMATCH", "Flink timestamp must equal observation event time");
            return;
        }
        if (context.currentWatermark() != Long.MIN_VALUE && eventTime <= context.currentWatermark()) {
            issue(context, input, "LATE_EVENT_UNSUPPORTED", "event is at or behind the current watermark");
            return;
        }

        ReadOnlyBroadcastState<String, byte[]> rules = context.getBroadcastState(RULES);
        byte[] encodedRule = rules.get(input.ruleRef().key());
        if (encodedRule == null) {
            issue(context, input, "RULE_NOT_FOUND", "published boundary rule is not available");
            return;
        }
        BoundaryRuleDefinition rule = BoundaryRuleCodec.decode(encodedRule);
        if (rule.boundaryKind() != input.boundaryKind()) {
            issue(context, input, "RULE_KIND_MISMATCH", "rule boundary kind does not match the input");
            return;
        }

        BoundaryOperatorState current = readState(input);
        BoundaryWindowResult evaluated;
        try {
            evaluated = BoundaryWindowEvaluator.onObservation(
                    rule, current.windowState(), input.observation());
        } catch (IllegalArgumentException error) {
            issue(context, input, "EVALUATION_REJECTED", error.getMessage());
            return;
        }
        if (evaluated.newlyEligible()) {
            output.collect(BoundaryCandidateProjector.project(
                    rule, input.context(), evaluated, input.observation().eventTime()).toByteArray());
        }
        storeAndSchedule(
                rule,
                current,
                evaluated.state(),
                input.observation().eventTime(),
                context.timerService());
    }

    @Override
    public void processBroadcastElement(
            BoundaryRuleUpdate update,
            Context context,
            Collector<byte[]> output) throws Exception {
        if (update.operation() == BoundaryRuleUpdate.Operation.DELETE) {
            context.getBroadcastState(RULES).remove(update.ruleRef().key());
        } else {
            byte[] encodedRule = BoundaryRuleCodec.encode(update.rule());
            byte[] existingRule = context.getBroadcastState(RULES).get(update.ruleRef().key());
            if (existingRule != null && !Arrays.equals(existingRule, encodedRule)) {
                context.output(ISSUES, new BoundaryProcessingIssue(
                        "RULE_VERSION_CONFLICT",
                        null,
                        update.ruleRef().key(),
                        null,
                        context.timestamp() == null ? Long.MIN_VALUE : context.timestamp(),
                        "published rule versions are immutable; use a new version"));
                return;
            }
            context.getBroadcastState(RULES).put(update.ruleRef().key(), encodedRule);
        }
    }

    @Override
    public void onTimer(
            long timestamp,
            OnTimerContext context,
            Collector<byte[]> output) throws Exception {
        byte[] bytes = encodedState.value();
        if (bytes == null) {
            return;
        }
        BoundaryOperatorState current = BoundaryOperatorStateCodec.decode(bytes);
        if (current.nextTimerEpochMs() != timestamp) {
            return;
        }
        byte[] encodedRule = context.getBroadcastState(RULES).get(current.ruleRef().key());
        if (encodedRule == null) {
            issue(
                    context,
                    current,
                    "RULE_NOT_FOUND_ON_TIMER",
                    "published boundary rule was removed before its event-time timer fired");
            encodedState.update(BoundaryOperatorStateCodec.encode(
                    current.withWindow(current.windowState(), BoundaryOperatorState.NO_TIMER)));
            return;
        }
        BoundaryRuleDefinition rule = BoundaryRuleCodec.decode(encodedRule);
        BoundaryWindowResult evaluated = BoundaryWindowEvaluator.advanceEventTime(
                rule, current.windowState(), Instant.ofEpochMilli(timestamp), 0);
        if (evaluated.newlyEligible()) {
            output.collect(BoundaryCandidateProjector.project(
                    rule, current.context(), evaluated, Instant.ofEpochMilli(timestamp)).toByteArray());
        }
        storeAndSchedule(
                rule,
                current,
                evaluated.state(),
                Instant.ofEpochMilli(timestamp),
                context.timerService());
    }

    private BoundaryOperatorState readState(BoundaryStreamInput input) throws Exception {
        byte[] bytes = encodedState.value();
        if (bytes == null) {
            return new BoundaryOperatorState(
                    input.context(),
                    input.ruleRef(),
                    BoundaryWindowState.empty(),
                    BoundaryOperatorState.NO_TIMER);
        }
        BoundaryOperatorState current = BoundaryOperatorStateCodec.decode(bytes);
        if (current.context().equals(input.context()) && current.ruleRef().equals(input.ruleRef())) {
            return current;
        }
        return new BoundaryOperatorState(
                input.context(),
                input.ruleRef(),
                BoundaryWindowState.empty(),
                current.nextTimerEpochMs());
    }

    private void storeAndSchedule(
            BoundaryRuleDefinition rule,
            BoundaryOperatorState current,
            BoundaryWindowState nextWindow,
            Instant now,
            org.apache.flink.streaming.api.TimerService timers) throws Exception {
        long nextTimer = nextDeadline(rule, nextWindow, now);
        if (current.nextTimerEpochMs() != BoundaryOperatorState.NO_TIMER
                && current.nextTimerEpochMs() != nextTimer) {
            timers.deleteEventTimeTimer(current.nextTimerEpochMs());
        }
        if (nextTimer != BoundaryOperatorState.NO_TIMER
                && current.nextTimerEpochMs() != nextTimer) {
            timers.registerEventTimeTimer(nextTimer);
        }
        encodedState.update(BoundaryOperatorStateCodec.encode(
                current.withWindow(nextWindow, nextTimer)));
    }

    static long nextDeadline(
            BoundaryRuleDefinition rule,
            BoundaryWindowState state,
            Instant now) {
        if (state.candidateEmitted()) {
            return BoundaryOperatorState.NO_TIMER;
        }
        long earliest = Long.MAX_VALUE;
        for (EvidenceCondition condition : rule.conditions()) {
            EvidenceSignalState signal = state.signals().get(condition.signal());
            if (signal == null || signal.lastEventTime() == null) {
                continue;
            }
            if (signal.status() == ConditionStatus.PENDING && signal.trueSince() != null) {
                Instant matureAt = signal.trueSince().plus(condition.holdFor());
                if (matureAt.isAfter(now)) {
                    earliest = Math.min(earliest, matureAt.toEpochMilli());
                }
            }
            if (signal.status() == ConditionStatus.PENDING || signal.status() == ConditionStatus.TRUE) {
                Instant staleAt = signal.lastEventTime().plus(condition.maxSilence()).plusMillis(1);
                if (staleAt.isAfter(now)) {
                    earliest = Math.min(earliest, staleAt.toEpochMilli());
                }
            }
        }
        return earliest == Long.MAX_VALUE ? BoundaryOperatorState.NO_TIMER : earliest;
    }

    private static void issue(
            ReadOnlyContext context,
            BoundaryStreamInput input,
            String code,
            String message) {
        context.output(ISSUES, new BoundaryProcessingIssue(
                code,
                input.keyedLocality(),
                input.ruleRef().key(),
                input.observation().eventId(),
                input.observation().eventTime().toEpochMilli(),
                message));
    }

    private static void issue(
            OnTimerContext context,
            BoundaryOperatorState state,
            String code,
            String message) {
        context.output(ISSUES, new BoundaryProcessingIssue(
                code,
                context.getCurrentKey(),
                state.ruleRef().key(),
                null,
                context.timestamp() == null ? Long.MIN_VALUE : context.timestamp(),
                message));
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
