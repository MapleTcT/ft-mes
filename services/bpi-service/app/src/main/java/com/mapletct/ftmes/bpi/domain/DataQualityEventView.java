package com.mapletct.ftmes.bpi.domain;

import java.time.Instant;
import java.util.Map;

public record DataQualityEventView(
        String eventId,
        String sourceEventId,
        String severity,
        String detail,
        Instant detectedAt,
        Instant receivedAt,
        Map<String, String> headers) {

    public DataQualityEventView {
        headers = Map.copyOf(headers);
    }
}
