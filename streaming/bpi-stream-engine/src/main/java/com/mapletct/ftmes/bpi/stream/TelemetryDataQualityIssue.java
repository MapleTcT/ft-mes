package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.contract.v1.DataQualitySeverity;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record TelemetryDataQualityIssue(
        String code,
        DataQualitySeverity severity,
        String sourceEventId,
        String tenantId,
        String plantId,
        String lineId,
        String deviceId,
        String propertyId,
        String detail,
        long detectedAtMs,
        Map<String, String> headers) {

    public TelemetryDataQualityIssue {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(sourceEventId, "sourceEventId");
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(plantId, "plantId");
        Objects.requireNonNull(lineId, "lineId");
        Objects.requireNonNull(deviceId, "deviceId");
        Objects.requireNonNull(propertyId, "propertyId");
        Objects.requireNonNull(detail, "detail");
        Objects.requireNonNull(headers, "headers");
        headers = Collections.unmodifiableMap(new LinkedHashMap<>(headers));
        if (code.isBlank() || tenantId.isBlank() || plantId.isBlank() || lineId.isBlank()
                || deviceId.isBlank() || detail.isBlank() || detectedAtMs <= 0) {
            throw new IllegalArgumentException("telemetry data-quality issue is incomplete");
        }
        if (severity == DataQualitySeverity.DATA_QUALITY_SEVERITY_UNSPECIFIED
                || severity == DataQualitySeverity.UNRECOGNIZED) {
            throw new IllegalArgumentException("telemetry data-quality severity is required");
        }
    }
}
