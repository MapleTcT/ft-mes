package com.mapletct.ftmes.bpi.domain;

import java.time.Instant;

public record FeatureFlagView(
        String flagKey,
        String displayName,
        String description,
        String riskLevel,
        boolean effectiveEnabled,
        String effectiveScopeType,
        String effectiveScopeKey,
        Long effectiveRevision,
        String selectedScopeType,
        String selectedScopeKey,
        boolean overrideExists,
        boolean overrideActive,
        Boolean overrideEnabled,
        long overrideRevision,
        String updatedBy,
        Instant updatedAt,
        String lastReason,
        String enforcementStatus,
        boolean editable,
        String blockedReason) {
}
