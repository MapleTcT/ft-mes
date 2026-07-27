package com.mapletct.ftmes.bpi.domain;

import java.time.Instant;
import java.util.List;

public record ProcessEvidenceView(
        String tenantId,
        String plantId,
        String lineId,
        String orderId,
        Instant from,
        Instant to,
        boolean contextInferred,
        List<ProcessEvidenceSeries> series) {
}
