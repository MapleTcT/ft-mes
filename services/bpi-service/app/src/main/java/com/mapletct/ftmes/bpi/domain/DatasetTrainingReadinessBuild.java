package com.mapletct.ftmes.bpi.domain;

import java.util.List;
import java.util.Map;

public record DatasetTrainingReadinessBuild(
        String state,
        Map<String, Object> requiredThresholds,
        Map<String, Object> observedMetrics,
        List<Map<String, Object>> gateResults,
        List<String> blockerCodes,
        Map<String, Object> phaseBoundary,
        String assessmentChecksum) {
}
