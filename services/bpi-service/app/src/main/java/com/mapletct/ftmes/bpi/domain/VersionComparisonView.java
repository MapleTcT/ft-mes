package com.mapletct.ftmes.bpi.domain;

import java.util.List;
import java.util.UUID;

public record VersionComparisonView(
        String objectType,
        VersionReference base,
        VersionReference target,
        boolean identical,
        int changeCount,
        boolean truncated,
        List<VersionChangeView> changes) {

    public record VersionReference(
            UUID id,
            String code,
            String version,
            String state,
            String checksum) {
    }
}
