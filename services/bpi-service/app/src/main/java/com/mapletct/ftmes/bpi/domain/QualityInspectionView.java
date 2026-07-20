package com.mapletct.ftmes.bpi.domain;

import java.time.Instant;

public record QualityInspectionView(
        String inspectionCode,
        String inspectionRecordId,
        boolean required,
        String disposition,
        boolean finalResult,
        Instant observedAt) {
}
