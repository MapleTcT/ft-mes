package com.mapletct.ftmes.bpi.stream;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

record BpiJointAcceptanceReplayConfig(
        String bootstrapServers,
        String telemetryTopic,
        String contextTopic,
        String candidateTopic,
        String dataQualityTopic,
        String marker,
        String tenantId,
        String plantId,
        String lineId,
        String topologyCode,
        String topologyVersion,
        String ruleCode,
        String ruleVersion,
        String deviceId,
        String flowPropertyId,
        String flowUnit,
        String pumpPropertyId,
        String pumpUnit,
        String calibrationVersion,
        Duration timeout,
        Duration telemetrySpacing,
        Duration resultGrace,
        Path reportPath) {

    private static final Pattern SAFE_TOPIC = Pattern.compile("[A-Za-z0-9._-]+");
    private static final Pattern SAFE_TOKEN = Pattern.compile("[A-Za-z0-9._-]{1,128}");
    private static final Pattern SAFE_UNIT = Pattern.compile("[A-Za-z0-9._/%-]{1,32}");
    private static final Pattern SAFE_MARKER = Pattern.compile("[A-Za-z0-9._-]{8,80}");
    private static final DateTimeFormatter MARKER_TIME = DateTimeFormatter
            .ofPattern("yyyyMMdd_HHmmss")
            .withZone(ZoneOffset.UTC);

    BpiJointAcceptanceReplayConfig {
        required(bootstrapServers, "bootstrapServers");
        topic(telemetryTopic, "telemetryTopic");
        topic(contextTopic, "contextTopic");
        topic(candidateTopic, "candidateTopic");
        topic(dataQualityTopic, "dataQualityTopic");
        if (!SAFE_MARKER.matcher(marker).matches()) {
            throw new IllegalArgumentException("marker must be 8-80 safe token characters");
        }
        token(tenantId, "tenantId");
        token(plantId, "plantId");
        token(lineId, "lineId");
        token(topologyCode, "topologyCode");
        token(topologyVersion, "topologyVersion");
        token(ruleCode, "ruleCode");
        token(ruleVersion, "ruleVersion");
        token(deviceId, "deviceId");
        token(flowPropertyId, "flowPropertyId");
        unit(flowUnit, "flowUnit");
        token(pumpPropertyId, "pumpPropertyId");
        unit(pumpUnit, "pumpUnit");
        token(calibrationVersion, "calibrationVersion");
        positive(timeout, "timeout");
        nonNegative(telemetrySpacing, "telemetrySpacing");
        nonNegative(resultGrace, "resultGrace");
        if (reportPath == null || !reportPath.isAbsolute()) {
            throw new IllegalArgumentException("reportPath must be absolute");
        }
    }

    static BpiJointAcceptanceReplayConfig fromEnvironment(Map<String, String> environment) {
        return new BpiJointAcceptanceReplayConfig(
                value(environment, "BPI_KAFKA_BOOTSTRAP_SERVERS", null),
                value(environment, "BPI_TELEMETRY_TOPIC", "iot.telemetry.selected.v1"),
                value(environment, "BPI_CONTEXT_TOPIC", "mes.production.context.v1"),
                value(environment, "BPI_CANDIDATE_TOPIC", "bpi.batch.candidate.v1"),
                value(environment, "BPI_DATA_QUALITY_TOPIC", "bpi.data-quality.v1"),
                marker(environment.get("BPI_JOINT_MARKER")),
                value(environment, "BPI_JOINT_TENANT_ID", null),
                value(environment, "BPI_JOINT_PLANT_ID", null),
                value(environment, "BPI_JOINT_LINE_ID", null),
                value(environment, "BPI_JOINT_TOPOLOGY_CODE", null),
                value(environment, "BPI_JOINT_TOPOLOGY_VERSION", null),
                value(environment, "BPI_JOINT_RULE_CODE", null),
                value(environment, "BPI_JOINT_RULE_VERSION", null),
                value(environment, "BPI_JOINT_DEVICE_ID", null),
                value(environment, "BPI_JOINT_FLOW_PROPERTY_ID", "flow.instant"),
                value(environment, "BPI_JOINT_FLOW_UNIT", "t/h"),
                value(environment, "BPI_JOINT_PUMP_PROPERTY_ID", "pump.running"),
                value(environment, "BPI_JOINT_PUMP_UNIT", "bool"),
                value(environment, "BPI_JOINT_CALIBRATION_VERSION", "CAL-1"),
                seconds(environment, "BPI_JOINT_TIMEOUT_SECONDS", 180),
                millis(environment, "BPI_JOINT_TELEMETRY_SPACING_MS", 2_000),
                millis(environment, "BPI_JOINT_RESULT_GRACE_MS", 5_000),
                Path.of(value(environment, "BPI_JOINT_REPORT", "/evidence/bpi-joint-replay.json")));
    }

    String consumerGroup() {
        return "ft-mes-bpi-joint-acceptance-" + marker;
    }

    String orderId() {
        return "MO-" + marker;
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

    private static void token(String value, String field) {
        required(value, field);
        if (!SAFE_TOKEN.matcher(value).matches() || value.indexOf('|') >= 0) {
            throw new IllegalArgumentException(field + " contains unsupported characters");
        }
    }

    private static void unit(String value, String field) {
        required(value, field);
        if (!SAFE_UNIT.matcher(value).matches()) {
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
