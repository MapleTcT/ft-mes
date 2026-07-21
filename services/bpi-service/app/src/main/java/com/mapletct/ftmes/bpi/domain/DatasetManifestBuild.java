package com.mapletct.ftmes.bpi.domain;

import java.util.List;
import java.util.Map;

public record DatasetManifestBuild(
        List<DatasetManifestSample> samples,
        Map<String, Object> manifest,
        String manifestChecksum,
        int includedCount,
        int excludedCount,
        Map<String, Integer> exclusionSummary) {
}
