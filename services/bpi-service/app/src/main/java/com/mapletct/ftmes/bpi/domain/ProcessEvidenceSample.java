package com.mapletct.ftmes.bpi.domain;

import java.time.Instant;

public record ProcessEvidenceSample(
        Instant sampleTime,
        Double numericValue,
        String stringValue,
        Boolean booleanValue,
        String qualityCode) {
}
