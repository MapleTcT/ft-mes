package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.rules.BoundaryRuleDefinition;
import com.mapletct.ftmes.bpi.rules.BoundaryKind;
import com.mapletct.ftmes.bpi.rules.BoundaryWindowEvaluator;
import com.mapletct.ftmes.bpi.rules.BoundaryWindowResult;
import com.mapletct.ftmes.bpi.rules.BoundaryWindowState;
import com.mapletct.ftmes.bpi.rules.ConditionStatus;
import com.mapletct.ftmes.bpi.rules.EvidenceCondition;
import com.mapletct.ftmes.bpi.rules.EvidenceSignalState;
import com.mapletct.ftmes.bpi.rules.SignalObservation;
import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ReadOnlyBroadcastState;
import org.apache.flink.api.common.state.StateTtlConfig;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.streaming.api.functions.co.KeyedBroadcastProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public final class BoundaryKeyedBroadcastFunction extends KeyedBroadcastProcessFunction<
        String,
        BoundaryStreamInput,
        byte[],
        byte[]> {

    public static final MapStateDescriptor<String, byte[]> RULES = new MapStateDescriptor<>(
            "bpi-boundary-rules-v1", String.class, byte[].class);
    public static final OutputTag<BoundaryProcessingIssue> ISSUES =
            new OutputTag<>("bpi-boundary-processing-issues") {
            };

    private static final String WINDOW_STATE_NAME = "bpi-boundary-window-v1";
    private static final int MAX_BUFFERED_OBSERVATIONS = 10_000;

    private final java.time.Duration stateTtl;
    private transient ValueState<byte[]> encodedState;

    public BoundaryKeyedBroadcastFunction() {
        this(java.time.Duration.ofDays(30));
    }

    public BoundaryKeyedBroadcastFunction(java.time.Duration stateTtl) {
        if (stateTtl == null || stateTtl.isZero() || stateTtl.isNegative()) {
            throw new IllegalArgumentException("stateTtl must be positive");
        }
        this.stateTtl = stateTtl;
    }

    @Override
    public void open(OpenContext openContext) {
        encodedState = getRuntimeContext().getState(windowStateDescriptor());
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
        ObservationBuffer buffer = appendObservation(
                rule,
                current.observations(),
                input.observation(),
                Instant.ofEpochMilli(Math.max(eventTime, context.currentWatermark())));
        if (buffer.conflict()) {
            issue(context, input, "EVENT_ID_CONFLICT", "same event and signal identity has different content");
            return;
        }
        if (buffer.overflow()) {
            issue(context, input, "OBSERVATION_BUFFER_OVERFLOW", "open rule window exceeded the observation limit");
            return;
        }
        if (buffer.duplicate()) {
            return;
        }
        if (context.currentWatermark() != Long.MIN_VALUE && eventTime <= context.currentWatermark()) {
            long latenessMillis = context.currentWatermark() - eventTime;
            if (latenessMillis > rule.timing().allowedLateness().toMillis()
                    || current.windowState().candidateEmitted()
                    || !current.observationHistoryComplete()) {
                issue(
                        context,
                        input,
                        "LATE_EVENT_REVISION_REQUIRED",
                        "late evidence cannot mutate an emitted, expired, or incomplete-history window");
                return;
            }
            Recalculation recalculated;
            try {
                recalculated = recalculate(
                        rule, buffer.observations(), Instant.ofEpochMilli(context.currentWatermark()));
            } catch (IllegalArgumentException error) {
                issue(context, input, "EVALUATION_REJECTED", error.getMessage());
                return;
            }
            if (recalculated.emission() != null) {
                output.collect(BoundaryCandidateProjector.project(
                        rule,
                        input.context(),
                        recalculated.emission().result(),
                        recalculated.emission().eventTime()).toByteArray());
            }
            storeAndSchedule(
                    rule,
                    current,
                    recalculated.state(),
                    buffer.observations(),
                    Instant.ofEpochMilli(context.currentWatermark()),
                    context.timerService());
            return;
        }
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
                buffer.observations(),
                input.observation().eventTime(),
                context.timerService());
    }

    @Override
    public void processBroadcastElement(
            byte[] updateBytes,
            Context context,
            Collector<byte[]> output) throws Exception {
        BoundaryRuleUpdate update;
        try {
            update = BoundaryRuleUpdateCodec.decode(updateBytes);
        } catch (IllegalStateException error) {
            context.output(ISSUES, new BoundaryProcessingIssue(
                    "RULE_UPDATE_DECODE_REJECTED",
                    null,
                    null,
                    null,
                    context.timestamp() == null ? Long.MIN_VALUE : context.timestamp(),
                    error.getMessage()));
            return;
        }
        if (update.operation() == BoundaryRuleUpdate.Operation.DELETE) {
            context.getBroadcastState(RULES).remove(update.ruleRef().key());
            context.applyToKeyedState(windowStateDescriptor(), (key, state) -> {
                byte[] stateBytes = state.value();
                if (stateBytes != null
                        && BoundaryOperatorStateCodec.decode(stateBytes).ruleRef().equals(update.ruleRef())) {
                    state.clear();
                }
            });
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

    private ValueStateDescriptor<byte[]> windowStateDescriptor() {
        ValueStateDescriptor<byte[]> descriptor = new ValueStateDescriptor<>(
                WINDOW_STATE_NAME, byte[].class);
        descriptor.enableTimeToLive(StateTtlConfig.newBuilder(stateTtl)
                .updateTtlOnCreateAndWrite()
                .neverReturnExpired()
                .cleanupFullSnapshot()
                .cleanupInRocksdbCompactFilter(1_000)
                .build());
        return descriptor;
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
                pruneObservations(rule, current.observations(), Instant.ofEpochMilli(timestamp)),
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
            List<SignalObservation> observations,
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
                current.withWindowAndObservations(nextWindow, observations, nextTimer)));
    }

    private static ObservationBuffer appendObservation(
            BoundaryRuleDefinition rule,
            List<SignalObservation> current,
            SignalObservation incoming,
            Instant referenceTime) {
        if (rule.conditions().stream().noneMatch(condition -> condition.signal().equals(incoming.signal()))) {
            return new ObservationBuffer(current, false, false, false);
        }
        for (SignalObservation existing : current) {
            if (existing.eventId().equals(incoming.eventId())
                    && existing.signal().equals(incoming.signal())) {
                return new ObservationBuffer(current, existing.equals(incoming), !existing.equals(incoming), false);
            }
        }
        List<SignalObservation> next = new ArrayList<>(current);
        next.add(incoming);
        next = new ArrayList<>(pruneObservations(rule, next, referenceTime));
        next.sort(Comparator.comparing(SignalObservation::eventTime)
                .thenComparing(SignalObservation::eventId)
                .thenComparing(SignalObservation::signal));
        return new ObservationBuffer(
                List.copyOf(next), false, false, next.size() > MAX_BUFFERED_OBSERVATIONS);
    }

    private static List<SignalObservation> pruneObservations(
            BoundaryRuleDefinition rule,
            List<SignalObservation> observations,
            Instant referenceTime) {
        Instant cutoff = referenceTime
                .minus(rule.timing().evaluationTimeout())
                .minus(rule.timing().allowedLateness());
        return observations.stream()
                .filter(item -> !item.eventTime().isBefore(cutoff))
                .toList();
    }

    private static Recalculation recalculate(
            BoundaryRuleDefinition rule,
            List<SignalObservation> observations,
            Instant watermark) {
        BoundaryWindowState state = BoundaryWindowState.empty();
        CandidateEmission emission = null;
        Instant cursor = null;
        for (SignalObservation observation : observations) {
            if (observation.eventTime().isAfter(watermark)) {
                continue;
            }
            if (cursor != null) {
                Recalculation advanced = advanceScheduledDeadlines(
                        rule, state, cursor, observation.eventTime(), false);
                state = advanced.state();
                if (emission == null) {
                    emission = advanced.emission();
                }
            }
            BoundaryWindowResult observed = BoundaryWindowEvaluator.onObservation(rule, state, observation);
            state = observed.state();
            if (emission == null && observed.newlyEligible()) {
                emission = new CandidateEmission(observed, observation.eventTime());
            }
            BoundaryWindowResult timed = BoundaryWindowEvaluator.advanceEventTime(
                    rule, state, observation.eventTime(), 0);
            state = timed.state();
            if (emission == null && timed.newlyEligible()) {
                emission = new CandidateEmission(timed, observation.eventTime());
            }
            cursor = observation.eventTime();
        }
        if (cursor != null) {
            Recalculation advanced = advanceScheduledDeadlines(rule, state, cursor, watermark, true);
            state = advanced.state();
            if (emission == null) {
                emission = advanced.emission();
            }
        }
        for (SignalObservation observation : observations) {
            if (!observation.eventTime().isAfter(watermark)) {
                continue;
            }
            BoundaryWindowResult observed = BoundaryWindowEvaluator.onObservation(rule, state, observation);
            state = observed.state();
            if (emission == null && observed.newlyEligible()) {
                emission = new CandidateEmission(observed, observation.eventTime());
            }
        }
        return new Recalculation(state, emission);
    }

    private static Recalculation advanceScheduledDeadlines(
            BoundaryRuleDefinition rule,
            BoundaryWindowState initial,
            Instant cursor,
            Instant limit,
            boolean includeLimit) {
        BoundaryWindowState state = initial;
        CandidateEmission emission = null;
        Instant current = cursor;
        while (true) {
            long deadlineEpochMs = nextDeadline(rule, state, current);
            if (deadlineEpochMs == BoundaryOperatorState.NO_TIMER) {
                break;
            }
            Instant deadline = Instant.ofEpochMilli(deadlineEpochMs);
            if (deadline.isAfter(limit) || (!includeLimit && deadline.equals(limit))) {
                break;
            }
            BoundaryWindowResult advanced = BoundaryWindowEvaluator.advanceEventTime(rule, state, deadline, 0);
            state = advanced.state();
            if (emission == null && advanced.newlyEligible()) {
                emission = new CandidateEmission(advanced, deadline);
            }
            current = deadline;
        }
        return new Recalculation(state, emission);
    }

    private record ObservationBuffer(
            List<SignalObservation> observations,
            boolean duplicate,
            boolean conflict,
            boolean overflow) {
    }

    private record CandidateEmission(BoundaryWindowResult result, Instant eventTime) {
    }

    private record Recalculation(BoundaryWindowState state, CandidateEmission emission) {
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
                    earliest = Math.min(earliest, ceilEpochMilli(matureAt));
                }
            }
            if (signal.status() == ConditionStatus.PENDING || signal.status() == ConditionStatus.TRUE) {
                Instant staleThreshold = signal.lastEventTime().plus(condition.maxSilence());
                long staleAt = Math.addExact(staleThreshold.toEpochMilli(), 1);
                if (Instant.ofEpochMilli(staleAt).isAfter(now)) {
                    earliest = Math.min(earliest, staleAt);
                }
            }
        }
        return earliest == Long.MAX_VALUE ? BoundaryOperatorState.NO_TIMER : earliest;
    }

    private static long ceilEpochMilli(Instant value) {
        long floor = value.toEpochMilli();
        return value.equals(Instant.ofEpochMilli(floor)) ? floor : Math.addExact(floor, 1);
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
