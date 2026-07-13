package com.mapletct.ftmes.bpi.domain;

import java.util.UUID;

public record RuleApplicationTarget(
        UUID publicationId,
        UUID ruleId,
        String tenantId,
        String plantId,
        String lineId,
        String ruleCode,
        String ruleVersion,
        String ruleChecksum,
        String publicationStatus,
        long publicationRevision,
        String applicationStatus) {
}
