package com.mapletct.ftmes.bpi.domain;

public record TelemetryIngestResult(
        String eventId,
        String status,
        String sequenceDisposition,
        int acceptedPoints,
        int rejectedPoints,
        boolean replay,
        String traceId) {
}
