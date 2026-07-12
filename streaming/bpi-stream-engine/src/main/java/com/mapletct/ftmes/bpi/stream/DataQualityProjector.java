package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.contract.v1.DataQualityEventV1;
import com.mapletct.ftmes.bpi.contract.v1.DataQualitySeverity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

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
        return event(
                issue.code(), issue.sourceEventId(), "", issue.detail(), detectedAtMs,
                new String[0], headers).toByteArray();
    }

    public static byte[] project(ContextJoinIssue issue, long detectedAtMs) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("stage", "context-join");
        headers.put("scope_key", issue.scopeKey());
        headers.put("event_time_ms", Long.toString(issue.eventTimeMs()));
        return event(
                issue.code(), issue.sourceEventId(), issue.propertyId(), issue.message(), detectedAtMs,
                split(issue.scopeKey()), headers).toByteArray();
    }

    public static byte[] project(BoundaryRoutingIssue issue, long detectedAtMs) {
        return event(
                issue.code(), issue.sourceEventId(), issue.propertyId(), issue.message(), detectedAtMs,
                new String[0], Map.of("stage", "rule-routing")).toByteArray();
    }

    public static byte[] project(BoundaryProcessingIssue issue, long detectedAtMs) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("stage", "boundary-evaluation");
        headers.put("keyed_locality", nullToEmpty(issue.keyedLocality()));
        headers.put("rule_key", nullToEmpty(issue.ruleKey()));
        headers.put("event_time_ms", Long.toString(issue.eventTimeMs()));
        return event(
                issue.code(), nullToEmpty(issue.eventId()), "", issue.message(), detectedAtMs,
                split(issue.keyedLocality()), headers).toByteArray();
    }

    private static DataQualityEventV1 event(
            String code,
            String sourceEventId,
            String propertyId,
            String detail,
            long detectedAtMs,
            String[] scope,
            Map<String, String> headers) {
        long timestamp = Math.max(1, detectedAtMs);
        String tenantId = scope.length > 0 ? scope[0] : "";
        String plantId = scope.length > 2 ? scope[1] : "";
        String lineId = scope.length > 2 ? scope[2] : (scope.length > 1 ? scope[1] : "");
        String normalizedDetail = truncate(nullToEmpty(detail));
        String identity = String.join(
                "|",
                code,
                nullToEmpty(sourceEventId),
                nullToEmpty(propertyId),
                tenantId,
                plantId,
                lineId,
                normalizedDetail,
                headers.toString());
        return DataQualityEventV1.newBuilder()
                .setEventId("DQ-" + sha256(identity).substring(0, 32))
                .setSourceEventId(nullToEmpty(sourceEventId))
                .setTenantId(tenantId)
                .setPlantId(plantId)
                .setLineId(lineId)
                .setPropertyId(nullToEmpty(propertyId))
                .setIssueCode(code)
                .setSeverity(DataQualitySeverity.ERROR)
                .setDetail(normalizedDetail.isBlank() ? code : normalizedDetail)
                .setDetectedAtMs(timestamp)
                .putAllHeaders(headers)
                .build();
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
