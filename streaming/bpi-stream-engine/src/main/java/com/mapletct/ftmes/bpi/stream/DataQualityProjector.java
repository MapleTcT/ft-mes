package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.contract.v1.DataQualityEventV1;
import com.mapletct.ftmes.bpi.contract.v1.DataQualitySeverity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public final class DataQualityProjector {

    private static final int MAX_DETAIL_LENGTH = 4_096;

    private DataQualityProjector() {
    }

    public static byte[] project(KafkaDecodeIssue issue, long detectedAtMs) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("stage", "kafka-decode");
        headers.put("topic", issue.topic());
        headers.put("partition", Integer.toString(issue.partition()));
        headers.put("offset", Long.toString(issue.offset()));
        return legacyEvent(
                issue.code(), issue.sourceEventId(), "", issue.detail(), detectedAtMs,
                new String[0], headers).toByteArray();
    }

    public static byte[] project(ContextJoinIssue issue, long detectedAtMs) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("stage", "context-join");
        headers.put("scope_key", issue.scopeKey());
        headers.put("event_time_ms", Long.toString(issue.eventTimeMs()));
        return legacyEvent(
                issue.code(), issue.sourceEventId(), issue.propertyId(), issue.message(), detectedAtMs,
                split(issue.scopeKey()), headers).toByteArray();
    }

    public static byte[] project(BoundaryRoutingIssue issue, long detectedAtMs) {
        return legacyEvent(
                issue.code(), issue.sourceEventId(), issue.propertyId(), issue.message(), detectedAtMs,
                new String[]{issue.tenantId(), issue.plantId(), issue.lineId()},
                Map.of("stage", "rule-routing")).toByteArray();
    }

    public static byte[] project(BoundaryProcessingIssue issue, long detectedAtMs) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("stage", "boundary-evaluation");
        headers.put("keyed_locality", nullToEmpty(issue.keyedLocality()));
        headers.put("rule_key", nullToEmpty(issue.ruleKey()));
        headers.put("event_time_ms", Long.toString(issue.eventTimeMs()));
        if (!isSignalSilence(issue.code())) {
            return legacyEvent(
                    issue.code(), nullToEmpty(issue.eventId()), "", issue.message(), detectedAtMs,
                    split(issue.keyedLocality()), headers).toByteArray();
        }
        return typedEvent(
                issue.code(), nullToEmpty(issue.eventId()), nullToEmpty(issue.propertyId()),
                issue.message(), detectedAtMs, "", split(issue.keyedLocality()), headers,
                processingSeverity(issue.code())).toByteArray();
    }

    public static byte[] project(TelemetryDataQualityIssue issue) {
        return typedEvent(
                issue.code(),
                issue.sourceEventId(),
                issue.propertyId(),
                issue.detail(),
                issue.detectedAtMs(),
                issue.deviceId(),
                new String[]{issue.tenantId(), issue.plantId(), issue.lineId()},
                issue.headers(),
                issue.severity()).toByteArray();
    }

    private static DataQualityEventV1 legacyEvent(
            String code,
            String sourceEventId,
            String propertyId,
            String detail,
            long detectedAtMs,
            String[] scope,
            Map<String, String> headers) {
        return event(
                code, sourceEventId, propertyId, detail, detectedAtMs, "", scope, headers,
                DataQualitySeverity.ERROR, true);
    }

    private static DataQualityEventV1 typedEvent(
            String code,
            String sourceEventId,
            String propertyId,
            String detail,
            long detectedAtMs,
            String deviceId,
            String[] scope,
            Map<String, String> headers,
            DataQualitySeverity severity) {
        return event(
                code, sourceEventId, propertyId, detail, detectedAtMs, deviceId, scope, headers,
                severity, false);
    }

    private static DataQualityEventV1 event(
            String code,
            String sourceEventId,
            String propertyId,
            String detail,
            long detectedAtMs,
            String deviceId,
            String[] scope,
            Map<String, String> headers,
            DataQualitySeverity severity,
            boolean preserveLegacyIdentity) {
        long timestamp = Math.max(1, detectedAtMs);
        String tenantId = scope.length > 0 ? scope[0] : "";
        String plantId = scope.length > 2 ? scope[1] : "";
        String lineId = scope.length > 2 ? scope[2] : (scope.length > 1 ? scope[1] : "");
        String normalizedDetail = truncate(nullToEmpty(detail));
        Map<String, String> normalizedHeaders = preserveLegacyIdentity ? headers : new TreeMap<>(headers);
        String legacyIdentity = String.join(
                "|",
                code,
                nullToEmpty(sourceEventId),
                nullToEmpty(propertyId),
                tenantId,
                plantId,
                lineId,
                normalizedDetail,
                normalizedHeaders.toString());
        String identity = preserveLegacyIdentity
                ? legacyIdentity
                : String.join("|", legacyIdentity, nullToEmpty(deviceId), severity.name());
        return DataQualityEventV1.newBuilder()
                .setEventId("DQ-" + sha256(identity).substring(0, 32))
                .setSourceEventId(nullToEmpty(sourceEventId))
                .setTenantId(tenantId)
                .setPlantId(plantId)
                .setLineId(lineId)
                .setDeviceId(nullToEmpty(deviceId))
                .setPropertyId(nullToEmpty(propertyId))
                .setIssueCode(code)
                .setSeverity(severity)
                .setDetail(normalizedDetail.isBlank() ? code : normalizedDetail)
                .setDetectedAtMs(timestamp)
                .putAllHeaders(normalizedHeaders)
                .build();
    }

    private static DataQualitySeverity processingSeverity(String code) {
        return code != null && code.startsWith("OPTIONAL_SIGNAL_")
                ? DataQualitySeverity.WARNING
                : DataQualitySeverity.ERROR;
    }

    private static boolean isSignalSilence(String code) {
        return code != null && code.endsWith("_SIGNAL_SILENCE");
    }

    private static String[] split(String value) {
        return value == null || value.isBlank() ? new String[0] : value.split("\\|", -1);
    }

    private static String truncate(String value) {
        return value.length() <= MAX_DETAIL_LENGTH ? value : value.substring(0, MAX_DETAIL_LENGTH);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is unavailable", error);
        }
    }
}
