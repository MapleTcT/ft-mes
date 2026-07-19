package com.mapletct.ftmes.bpi.stream;

import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import com.mapletct.ftmes.bpi.contract.v1.DataQualitySeverity;
import com.mapletct.ftmes.bpi.contract.v1.PointValue;
import com.mapletct.ftmes.bpi.contract.v1.SequenceOrigin;
import com.mapletct.ftmes.bpi.contract.v1.TelemetryEnvelopeV1;
import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.api.common.state.StateTtlConfig;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.metrics.Counter;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TelemetryDataQualityFunction
        extends KeyedProcessFunction<String, byte[], byte[]> {

    private static final String STATE_NAME = "bpi-telemetry-sequence-v1";

    private final Duration maxClockSkew;
    private final Duration stateTtl;
    private transient ValueState<byte[]> encodedState;
    private transient Counter emittedEvents;
    private transient Counter sequenceIssues;
    private transient Counter clockIssues;
    private transient Counter pointQualityIssues;

    public TelemetryDataQualityFunction(Duration maxClockSkew, Duration stateTtl) {
        if (maxClockSkew == null || maxClockSkew.isNegative()) {
            throw new IllegalArgumentException("maxClockSkew cannot be negative");
        }
        if (stateTtl == null || stateTtl.isZero() || stateTtl.isNegative()) {
            throw new IllegalArgumentException("stateTtl must be positive");
        }
        this.maxClockSkew = maxClockSkew;
        this.stateTtl = stateTtl;
    }

    @Override
    public void open(OpenContext openContext) {
        ValueStateDescriptor<byte[]> descriptor = new ValueStateDescriptor<>(STATE_NAME, byte[].class);
        descriptor.enableTimeToLive(StateTtlConfig.newBuilder(stateTtl)
                .updateTtlOnCreateAndWrite()
                .neverReturnExpired()
                .cleanupFullSnapshot()
                .cleanupInRocksdbCompactFilter(1_000)
                .build());
        encodedState = getRuntimeContext().getState(descriptor);
        emittedEvents = getRuntimeContext().getMetricGroup().counter("data_quality_events_total");
        sequenceIssues = getRuntimeContext().getMetricGroup().counter("sequence_issues_total");
        clockIssues = getRuntimeContext().getMetricGroup().counter("clock_issues_total");
        pointQualityIssues = getRuntimeContext().getMetricGroup().counter("point_quality_issues_total");
    }

    @Override
    public void processElement(byte[] bytes, Context context, Collector<byte[]> output) throws Exception {
        TelemetryEnvelopeV1 envelope = decode(bytes);
        if (!sourceKey(envelope).equals(context.getCurrentKey())) {
            throw new IllegalStateException("telemetry data-quality stream key does not match source identity");
        }
        long detectedAtMs = envelope.getIngestTimeMs();
        if (hasAuthoritativeSequence(envelope)) {
            detectSequence(envelope, detectedAtMs, output);
        }
        detectClockSkew(envelope, detectedAtMs, output);
        detectPointQuality(envelope, detectedAtMs, output);
    }

    public static String sourceKey(byte[] bytes) {
        return sourceKey(decode(bytes));
    }

    static String sourceKey(TelemetryEnvelopeV1 envelope) {
        return String.join(
                "|",
                envelope.getTenantId(),
                envelope.getPlantId(),
                envelope.getGatewayId(),
                envelope.getDeviceId());
    }

    private void detectSequence(
            TelemetryEnvelopeV1 envelope,
            long detectedAtMs,
            Collector<byte[]> output) throws Exception {
        String fingerprint = sha256(deterministicBytes(envelope));
        TelemetrySequenceState incoming = new TelemetrySequenceState(
                envelope.getSourceEpoch(), envelope.getSequence(), envelope.getEventId(), fingerprint);
        byte[] currentBytes = encodedState.value();
        if (currentBytes == null) {
            encodedState.update(TelemetrySequenceStateCodec.encode(incoming));
            return;
        }

        TelemetrySequenceState current = TelemetrySequenceStateCodec.decode(currentBytes);
        int epochComparison = Long.compareUnsigned(incoming.sourceEpoch(), current.sourceEpoch());
        if (epochComparison < 0) {
            emit(sequenceIssue(
                    envelope,
                    "SOURCE_EPOCH_REGRESSION",
                    DataQualitySeverity.ERROR,
                    detectedAtMs,
                    "source_epoch regressed from " + unsigned(current.sourceEpoch())
                            + " to " + unsigned(incoming.sourceEpoch()),
                    fingerprint), output, sequenceIssues);
            return;
        }
        if (epochComparison > 0) {
            encodedState.update(TelemetrySequenceStateCodec.encode(incoming));
            return;
        }

        int sequenceComparison = Long.compareUnsigned(incoming.highestSequence(), current.highestSequence());
        if (sequenceComparison == 0) {
            boolean identical = current.eventId().equals(incoming.eventId())
                    && current.payloadSha256().equals(incoming.payloadSha256());
            emit(sequenceIssue(
                    envelope,
                    identical ? "SOURCE_SEQUENCE_DUPLICATE" : "SOURCE_SEQUENCE_CONFLICT",
                    identical ? DataQualitySeverity.INFO : DataQualitySeverity.CRITICAL,
                    detectedAtMs,
                    identical
                            ? "duplicate telemetry envelope for source_epoch=" + unsigned(incoming.sourceEpoch())
                                    + " sequence=" + unsigned(incoming.highestSequence())
                            : "same source_epoch and sequence carry different event identity or payload",
                    fingerprint), output, sequenceIssues);
            return;
        }
        if (sequenceComparison < 0) {
            emit(sequenceIssue(
                    envelope,
                    "SOURCE_SEQUENCE_OUT_OF_ORDER",
                    DataQualitySeverity.WARNING,
                    detectedAtMs,
                    "sequence " + unsigned(incoming.highestSequence()) + " arrived behind high-water mark "
                            + unsigned(current.highestSequence()) + " in source_epoch="
                            + unsigned(incoming.sourceEpoch()),
                    fingerprint), output, sequenceIssues);
            return;
        }
        if (!isConsecutive(current.highestSequence(), incoming.highestSequence())) {
            emit(sequenceIssue(
                    envelope,
                    "SOURCE_SEQUENCE_GAP",
                    DataQualitySeverity.WARNING,
                    detectedAtMs,
                    "sequence advanced from " + unsigned(current.highestSequence()) + " to "
                            + unsigned(incoming.highestSequence()) + " in source_epoch="
                            + unsigned(incoming.sourceEpoch()),
                    fingerprint), output, sequenceIssues);
        }
        encodedState.update(TelemetrySequenceStateCodec.encode(incoming));
    }

    private void detectClockSkew(
            TelemetryEnvelopeV1 envelope,
            long detectedAtMs,
            Collector<byte[]> output) {
        long skew = absoluteDifference(envelope.getEventTimeMs(), envelope.getIngestTimeMs());
        if (skew <= maxClockSkew.toMillis()) {
            return;
        }
        Map<String, String> headers = baseHeaders(envelope);
        headers.put("clock_skew_ms", Long.toString(skew));
        headers.put("max_clock_skew_ms", Long.toString(maxClockSkew.toMillis()));
        emit(new TelemetryDataQualityIssue(
                "CLOCK_DRIFT",
                DataQualitySeverity.ERROR,
                envelope.getEventId(),
                envelope.getTenantId(),
                envelope.getPlantId(),
                envelope.getLineId(),
                envelope.getDeviceId(),
                "",
                "event_time differs from ingest_time by " + skew + " ms; allowed "
                        + maxClockSkew.toMillis() + " ms",
                detectedAtMs,
                headers), output, clockIssues);
    }

    private void detectPointQuality(
            TelemetryEnvelopeV1 envelope,
            long detectedAtMs,
            Collector<byte[]> output) {
        for (PointValue point : envelope.getPointsList()) {
            if ("GOOD".equals(point.getQualityCode())) {
                continue;
            }
            DataQualitySeverity severity = switch (point.getQualityCode()) {
                case "BAD", "STALE" -> DataQualitySeverity.ERROR;
                case "UNCERTAIN", "SUBSTITUTED" -> DataQualitySeverity.WARNING;
                default -> null;
            };
            if (severity == null) {
                continue;
            }
            Map<String, String> headers = baseHeaders(envelope);
            headers.put("quality_code", point.getQualityCode());
            headers.put("sample_time_ms", Long.toString(point.getSampleTimeMs()));
            emit(new TelemetryDataQualityIssue(
                    "POINT_QUALITY_" + point.getQualityCode(),
                    severity,
                    envelope.getEventId(),
                    envelope.getTenantId(),
                    envelope.getPlantId(),
                    envelope.getLineId(),
                    envelope.getDeviceId(),
                    point.getPropertyId(),
                    "point quality_code is " + point.getQualityCode(),
                    detectedAtMs,
                    headers), output, pointQualityIssues);
        }
    }

    private TelemetryDataQualityIssue sequenceIssue(
            TelemetryEnvelopeV1 envelope,
            String code,
            DataQualitySeverity severity,
            long detectedAtMs,
            String detail,
            String fingerprint) {
        Map<String, String> headers = baseHeaders(envelope);
        headers.put("payload_sha256", fingerprint);
        return new TelemetryDataQualityIssue(
                code,
                severity,
                envelope.getEventId(),
                envelope.getTenantId(),
                envelope.getPlantId(),
                envelope.getLineId(),
                envelope.getDeviceId(),
                "",
                detail,
                detectedAtMs,
                headers);
    }

    private static Map<String, String> baseHeaders(TelemetryEnvelopeV1 envelope) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("stage", "telemetry-data-quality");
        headers.put("gateway_id", envelope.getGatewayId());
        headers.put("source_epoch", unsigned(envelope.getSourceEpoch()));
        headers.put("sequence", unsigned(envelope.getSequence()));
        headers.put("sequence_origin", envelope.getSequenceOrigin().name());
        headers.put("event_time_ms", Long.toString(envelope.getEventTimeMs()));
        headers.put("ingest_time_ms", Long.toString(envelope.getIngestTimeMs()));
        return headers;
    }

    private void emit(
            TelemetryDataQualityIssue issue,
            Collector<byte[]> output,
            Counter categoryCounter) {
        output.collect(DataQualityProjector.project(issue));
        emittedEvents.inc();
        categoryCounter.inc();
    }

    private static TelemetryEnvelopeV1 decode(byte[] bytes) {
        try {
            return TelemetryEnvelopeV1.parseFrom(bytes);
        } catch (InvalidProtocolBufferException error) {
            throw new IllegalStateException("validated telemetry envelope cannot be decoded", error);
        }
    }

    private static boolean isConsecutive(long previous, long current) {
        return previous != -1L && current == previous + 1L;
    }

    private static boolean hasAuthoritativeSequence(TelemetryEnvelopeV1 envelope) {
        return (envelope.getSequenceOrigin() == SequenceOrigin.DEVICE
                || envelope.getSequenceOrigin() == SequenceOrigin.GATEWAY)
                && envelope.getSourceEpoch() != 0
                && envelope.getSequence() != 0;
    }

    private static long absoluteDifference(long left, long right) {
        long larger = Math.max(left, right);
        long smaller = Math.min(left, right);
        try {
            return Math.subtractExact(larger, smaller);
        } catch (ArithmeticException error) {
            return Long.MAX_VALUE;
        }
    }

    private static String unsigned(long value) {
        return Long.toUnsignedString(value);
    }

    private static String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is unavailable", error);
        }
    }

    private static byte[] deterministicBytes(TelemetryEnvelopeV1 envelope) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(envelope.getSerializedSize());
            CodedOutputStream output = CodedOutputStream.newInstance(bytes);
            output.useDeterministicSerialization();
            envelope.writeTo(output);
            output.flush();
            return bytes.toByteArray();
        } catch (IOException error) {
            throw new IllegalStateException("cannot deterministically encode telemetry envelope", error);
        }
    }
}
