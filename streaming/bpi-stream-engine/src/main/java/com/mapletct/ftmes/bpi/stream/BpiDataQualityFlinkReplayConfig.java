package com.mapletct.ftmes.bpi.stream;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

record BpiDataQualityFlinkReplayConfig(
        String bootstrapServers,
        String telemetryTopic,
        String contextTopic,
        String dataQualityTopic,
        String marker,
        Duration timeout,
        Duration contextSettle,
        Duration telemetrySpacing,
        Duration resultGrace,
        Path reportPath) {

    private static final Pattern SAFE_TOPIC = Pattern.compile("[A-Za-z0-9._-]+");
    private static final Pattern SAFE_MARKER = Pattern.compile("[A-Za-z0-9._-]{8,80}");
    private static final DateTimeFormatter MARKER_TIME = DateTimeFormatter
            .ofPattern("yyyyMMdd_HHmmss")
            .withZone(ZoneOffset.UTC);

    BpiDataQualityFlinkReplayConfig {
        required(bootstrapServers, "bootstrapServers");
        topic(telemetryTopic, "telemetryTopic");
        topic(contextTopic, "contextTopic");
        topic(dataQualityTopic, "dataQualityTopic");
        if (!SAFE_MARKER.matcher(marker).matches()) {
            throw new IllegalArgumentException("marker must be 8-80 safe token characters");
        }
        positive(timeout, "timeout");
        nonNegative(contextSettle, "contextSettle");
        nonNegative(telemetrySpacing, "telemetrySpacing");
        nonNegative(resultGrace, "resultGrace");
        if (reportPath == null || !reportPath.isAbsolute()) {
            throw new IllegalArgumentException("reportPath must be absolute");
        }
    }

    static BpiDataQualityFlinkReplayConfig fromEnvironment(Map<String, String> environment) {
        return new BpiDataQualityFlinkReplayConfig(
                value(environment, "BPI_KAFKA_BOOTSTRAP_SERVERS", null),
                value(environment, "BPI_TELEMETRY_TOPIC", "iot.telemetry.selected.v1"),
                value(environment, "BPI_CONTEXT_TOPIC", "mes.production.context.v1"),
                value(environment, "BPI_DATA_QUALITY_TOPIC", "bpi.data-quality.v1"),
                marker(environment.get("BPI_DQ_REPLAY_MARKER")),
                seconds(environment, "BPI_DQ_REPLAY_TIMEOUT_SECONDS", 180),
                millis(environment, "BPI_DQ_REPLAY_CONTEXT_SETTLE_MS", 2_000),
                millis(environment, "BPI_DQ_REPLAY_TELEMETRY_SPACING_MS", 250),
                millis(environment, "BPI_DQ_REPLAY_RESULT_GRACE_MS", 5_000),
                Path.of(value(environment, "BPI_DQ_REPLAY_REPORT", "/evidence/bpi-data-quality-flink-replay.json")));
    }

    String consumerGroup() {
        return "ft-mes-bpi-dq-acceptance-" + marker;
    }

    private static String marker(String configured) {
        if (configured != null && !configured.isBlank()) {
            return configured.trim();
        }
        return "ADP_E2E_DQ_FLINK_" + MARKER_TIME.format(Instant.now()) + "_"
                + UUID.randomUUID().toString().substring(0, 8);
    }

    private static Duration seconds(Map<String, String> values, String key, long defaultValue) {
        return Duration.ofSeconds(number(values, key, defaultValue));
    }

    private static Duration millis(Map<String, String> values, String key, long defaultValue) {
        return Duration.ofMillis(number(values, key, defaultValue));
    }

    private static long number(Map<String, String> values, String key, long defaultValue) {
        try {
            return Long.parseLong(values.getOrDefault(key, Long.toString(defaultValue)));
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(key + " must be an integer", error);
        }
    }

    private static String value(Map<String, String> values, String key, String defaultValue) {
        String result = values.getOrDefault(key, defaultValue);
        required(result, key);
        return result.trim();
    }

    private static void topic(String value, String field) {
        required(value, field);
        if (!SAFE_TOPIC.matcher(value).matches()) {
            throw new IllegalArgumentException(field + " contains unsupported characters");
        }
    }

    private static void required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    private static void positive(Duration value, String field) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(field + " must be positive");
        }
    }

    private static void nonNegative(Duration value, String field) {
        if (value == null || value.isNegative()) {
            throw new IllegalArgumentException(field + " cannot be negative");
        }
    }
}
