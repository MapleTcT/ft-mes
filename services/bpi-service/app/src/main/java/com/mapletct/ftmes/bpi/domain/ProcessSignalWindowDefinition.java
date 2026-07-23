package com.mapletct.ftmes.bpi.domain;

import java.util.List;

public record ProcessSignalWindowDefinition(
        String featureRef,
        String signal,
        String valueType,
        String metric,
        int startOffsetSeconds,
        int endOffsetSeconds,
        int minimumSamples,
        int maximumGapSeconds,
        String expectedUnit,
        boolean requireCalibration,
        List<String> acceptedQualityCodes,
        String checksum) {
}
