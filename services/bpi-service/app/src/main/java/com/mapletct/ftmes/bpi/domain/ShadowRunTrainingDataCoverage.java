package com.mapletct.ftmes.bpi.domain;

import java.util.List;

public record ShadowRunTrainingDataCoverage(
        String policyVersion,
        int requiredReviewedBatchCount,
        int reviewedBatchCount,
        int requiredProductionDayCount,
        int distinctProductionDayCount,
        int requiredAcceptedStartLabelCount,
        int acceptedStartLabelCount,
        int requiredRejectedStartLabelCount,
        int rejectedStartLabelCount,
        boolean thresholdsMet,
        List<String> blockers) {
}
