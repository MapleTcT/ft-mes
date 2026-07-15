package com.mapletct.ftmes.bpi.stream;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

record BpiKafkaAcceptanceReplayConfig(
        String bootstrapServers,
        String telemetryTopic,
        String pointCatalogTopic,
        String contextTopic,
        String ruleTopic,
        String candidateTopic,
        String dataQualityTopic,
        String marker,
        Duration timeout,
        Duration ruleSettle,
        Duration telemetrySpacing,
        Duration resultGrace,
        Path reportPath) {

    private static final Pattern SAFE_TOPIC = Pattern.compile("[A-Za-z0-9._-]+");
    private static final Pattern SAFE_MARKER = Pattern.compile("[A-Za-z0-9._-]{8,80}");
    private static final DateTimeFormatter MARKER_TIME = DateTimeFormatter
            .ofPattern("yyyyMMdd_HHmmss")
            .withZone(ZoneOffset.UTC);

    BpiKafkaAcceptanceReplayConfig {
        required(bootstrapServers, "bootstrapServers");
        topic(telemetryTopic, "telemetryTopic");
        topic(pointCatalogTopic, "pointCatalogTopic");
        topic(contextTopic, "contextTopic");
        topic(ruleTopic, "ruleTopic");
        topic(candidateTopic, "candidateTopic");
        topic(dataQualityTopic, "dataQualityTopic");
        if (!SAFE_MARKER.matcher(marker).matches()) {
            throw new IllegalArgumentException("marker must be 8-80 safe token characters");
        }
        positive(timeout, "timeout");
        nonNegative(ruleSettle, "ruleSettle");
        nonNegative(telemetrySpacing, "telemetrySpacing");
        nonNegative(resultGrace, "resultGrace");
        if (reportPath == null || !reportPath.isAbsolute()) {
            throw new IllegalArgumentException("reportPath must be absolute");
        }
    }

    static BpiKafkaAcceptanceReplayConfig fromEnvironment(Map<String, String> environment) {
        return new BpiKafkaAcceptanceReplayConfig(
                value(environment, "BPI_KAFKA_BOOTSTRAP_SERVERS", null),
                value(environment, "BPI_TELEMETRY_TOPIC", "iot.telemetry.selected.v1"),
                value(environment, "BPI_POINT_CATALOG_TOPIC", "iot.point-catalog.snapshot.v1"),
                value(environment, "BPI_CONTEXT_TOPIC", "mes.production.context.v1"),
                value(environment, "BPI_RULE_TOPIC", "bpi.boundary.rule-publication.v1"),
                value(environment, "BPI_CANDIDATE_TOPIC", "bpi.batch.candidate.v1"),
                value(environment, "BPI_DATA_QUALITY_TOPIC", "bpi.data-quality.v1"),
                marker(environment.get("BPI_REPLAY_MARKER")),
                seconds(environment, "BPI_REPLAY_TIMEOUT_SECONDS", 180),
                millis(environment, "BPI_REPLAY_RULE_SETTLE_MS", 5_000),
                millis(environment, "BPI_REPLAY_TELEMETRY_SPACING_MS", 2_000),
                millis(environment, "BPI_REPLAY_RESULT_GRACE_MS", 5_000),
                Path.of(value(environment, "BPI_REPLAY_REPORT", "/evidence/bpi-kafka-replay.json")));
    }

    String consumerGroup() {
        return "ft-mes-bpi-acceptance-" + marker;
    }

    private static String marker(String configured) {
        if (configured != null && !configured.isBlank()) {
            return configured.trim();
        }
        return "ADP_E2E_" + MARKER_TIME.format(Instant.now()) + "_"
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
