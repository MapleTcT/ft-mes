package com.mapletct.ftmes.bpi.domain;

public record ShadowRunSourceCoverage(
        int pinnedPointCount,
        int activeRegisteredPointCount,
        int physicalIdentityPointCount,
        int freshSequenceQualifiedPointCount,
        int approvedCalibrationPointCount,
        int readyPointCount,
        boolean fullyReady) {
}
