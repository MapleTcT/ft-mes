package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.contract.v1.ProductionContextEventV1;
import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.state.StateTtlConfig;
import org.apache.flink.streaming.api.functions.co.KeyedCoProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class ProductionContextJoinFunction extends KeyedCoProcessFunction<
        String,
        byte[],
        byte[],
        byte[]> {

    public static final OutputTag<ContextJoinIssue> ISSUES =
            new OutputTag<>("bpi-production-context-join-issues") {
            };
    private static final String JOIN_STATE_NAME = "bpi-production-context-join-v1";
    private static final int MAX_PENDING = 10_000;

    private final Duration contextWait;
    private final Duration contextRetention;
    private final Duration stateTtl;
    private transient ValueState<byte[]> encodedState;

    public ProductionContextJoinFunction(Duration contextWait, Duration contextRetention) {
        if (contextWait == null || contextWait.isNegative() || contextWait.isZero()) {
            throw new IllegalArgumentException("contextWait must be positive");
        }
        if (contextRetention == null || contextRetention.isNegative() || contextRetention.isZero()) {
            throw new IllegalArgumentException("contextRetention must be positive");
        }
        if (contextWait.compareTo(contextRetention) > 0) {
            throw new IllegalArgumentException("contextWait cannot exceed contextRetention");
        }
        this.contextWait = contextWait;
        this.contextRetention = contextRetention;
        this.stateTtl = contextRetention.plus(contextWait);
    }

    @Override
    public void open(OpenContext openContext) {
        ValueStateDescriptor<byte[]> descriptor = new ValueStateDescriptor<>(
                JOIN_STATE_NAME, byte[].class);
        descriptor.enableTimeToLive(StateTtlConfig.newBuilder(stateTtl)
                .updateTtlOnCreateAndWrite()
                .neverReturnExpired()
                .cleanupFullSnapshot()
                .cleanupInRocksdbCompactFilter(1_000)
                .build());
        encodedState = getRuntimeContext().getState(descriptor);
    }

    @Override
    public void processElement1(
            byte[] telemetryBytes,
            Context context,
            Collector<byte[]> output) throws Exception {
        TelemetryPointEvent telemetry;
        try {
            telemetry = TelemetryPointEventCodec.decode(telemetryBytes);
        } catch (IllegalStateException error) {
            context.output(ISSUES, new ContextJoinIssue(
                    "TELEMETRY_DECODE_REJECTED", context.getCurrentKey(), "", "",
                    context.timestamp() == null ? Long.MIN_VALUE : context.timestamp(), error.getMessage()));
            return;
        }
        if (!telemetry.scopeKey().equals(context.getCurrentKey())) {
            issue(context, telemetry, "KEY_MISMATCH", "telemetry scope does not match the keyed partition");
            return;
        }
        Long timestamp = context.timestamp();
        if (timestamp == null || timestamp != telemetry.eventTime().toEpochMilli()) {
            issue(context, telemetry, "TIMESTAMP_MISMATCH", "Flink timestamp must equal point event time");
            return;
        }
        ProductionContextJoinState state = readState();
        ProductionContextTimeline timeline = timeline(state.contexts());
        ProductionContextEventV1 resolved = timeline.resolve(
                telemetry.envelope().getTenantId(),
                telemetry.envelope().getPlantId(),
                telemetry.envelope().getLineId(),
                telemetry.eventTime()).orElse(null);
        if (resolved != null) {
            output.collect(ContextualTelemetryPointCodec.encode(
                    new ContextualTelemetryPoint(telemetry, resolved)));
            return;
        }
        for (PendingContextPoint existing : state.pending()) {
            if (existing.telemetry().identity().equals(telemetry.identity())) {
                if (existing.telemetry().equals(telemetry)) {
                    return;
                }
                issue(context, telemetry, "EVENT_ID_CONFLICT", "pending telemetry identity has different content");
                return;
            }
        }
        if (state.pending().size() >= MAX_PENDING) {
            issue(context, telemetry, "PENDING_BUFFER_OVERFLOW", "context wait buffer reached its per-key limit");
            return;
        }
        long deadline = Math.addExact(telemetry.eventTime().toEpochMilli(), contextWait.toMillis());
        if (context.timerService().currentWatermark() >= deadline) {
            issue(context, telemetry, "CONTEXT_WAIT_EXPIRED", "production context did not arrive before its deadline");
            return;
        }
        List<PendingContextPoint> pending = new ArrayList<>(state.pending());
        pending.add(new PendingContextPoint(telemetry, deadline));
        context.timerService().registerEventTimeTimer(deadline);
        writeState(new ProductionContextJoinState(state.contexts(), pending));
    }

    @Override
    public void processElement2(
            byte[] contextBytes,
            Context context,
            Collector<byte[]> output) throws Exception {
        ProductionContextEventV1 incoming;
        try {
            incoming = ProductionContextWire.decode(contextBytes);
        } catch (IllegalStateException error) {
            context.output(ISSUES, new ContextJoinIssue(
                    "CONTEXT_DECODE_REJECTED", context.getCurrentKey(), "", "",
                    context.timestamp() == null ? Long.MIN_VALUE : context.timestamp(), error.getMessage()));
            return;
        }
        if (!TelemetryPointEvent.contextScopeKey(incoming).equals(context.getCurrentKey())) {
            context.output(ISSUES, new ContextJoinIssue(
                    "KEY_MISMATCH", context.getCurrentKey(), incoming.getEventId(), "",
                    incoming.getEffectiveFromMs(), "production context scope does not match the keyed partition"));
            return;
        }
        Long timestamp = context.timestamp();
        if (timestamp == null || timestamp != incoming.getEffectiveFromMs()) {
            context.output(ISSUES, new ContextJoinIssue(
                    "TIMESTAMP_MISMATCH", context.getCurrentKey(), incoming.getEventId(), "",
                    incoming.getEffectiveFromMs(), "Flink timestamp must equal context effective_from_ms"));
            return;
        }
        ProductionContextJoinState state = readState();
        ProductionContextTimeline timeline;
        try {
            timeline = timeline(state.contexts()).apply(incoming);
        } catch (IllegalArgumentException | IllegalStateException error) {
            context.output(ISSUES, new ContextJoinIssue(
                    "CONTEXT_REJECTED", context.getCurrentKey(), incoming.getEventId(), "",
                    incoming.getEffectiveFromMs(), error.getMessage()));
            return;
        }
        List<PendingContextPoint> remaining = new ArrayList<>();
        for (PendingContextPoint pending : state.pending()) {
            ProductionContextEventV1 resolved = timeline.resolve(
                    pending.telemetry().envelope().getTenantId(),
                    pending.telemetry().envelope().getPlantId(),
                    pending.telemetry().envelope().getLineId(),
                    pending.telemetry().eventTime()).orElse(null);
            if (resolved == null) {
                remaining.add(pending);
            } else {
                output.collect(ContextualTelemetryPointCodec.encode(
                        new ContextualTelemetryPoint(pending.telemetry(), resolved)));
            }
        }
        writeState(new ProductionContextJoinState(
                prune(timeline, context.timerService().currentWatermark()).events(), remaining));
    }

    @Override
    public void onTimer(
            long timestamp,
            OnTimerContext context,
            Collector<byte[]> output) throws Exception {
        ProductionContextJoinState state = readState();
        List<PendingContextPoint> remaining = new ArrayList<>();
        for (PendingContextPoint pending : state.pending()) {
            if (pending.deadlineEpochMs() <= timestamp) {
                issue(context, pending.telemetry(), "CONTEXT_WAIT_EXPIRED",
                        "production context did not arrive before its deadline");
            } else {
                remaining.add(pending);
            }
        }
        ProductionContextTimeline timeline = prune(timeline(state.contexts()), timestamp);
        writeState(new ProductionContextJoinState(timeline.events(), remaining));
    }

    private ProductionContextJoinState readState() throws Exception {
        byte[] bytes = encodedState.value();
        return bytes == null ? ProductionContextJoinState.empty() : ProductionContextJoinStateCodec.decode(bytes);
    }

    private void writeState(ProductionContextJoinState state) throws Exception {
        encodedState.update(ProductionContextJoinStateCodec.encode(state));
    }

    private static ProductionContextTimeline timeline(List<ProductionContextEventV1> contexts) {
        ProductionContextTimeline timeline = new ProductionContextTimeline();
        for (ProductionContextEventV1 context : contexts) {
            timeline = timeline.apply(context);
        }
        return timeline;
    }

    private ProductionContextTimeline prune(ProductionContextTimeline timeline, long watermark) {
        if (watermark == Long.MIN_VALUE) {
            return timeline;
        }
        return timeline.pruneBefore(Instant.ofEpochMilli(watermark).minus(contextRetention));
    }

    private static void issue(
            Context context,
            TelemetryPointEvent telemetry,
            String code,
            String message) {
        context.output(ISSUES, new ContextJoinIssue(
                code,
                context.getCurrentKey(),
                telemetry.envelope().getEventId(),
                telemetry.point().getPropertyId(),
                telemetry.eventTime().toEpochMilli(),
                message));
    }
}
