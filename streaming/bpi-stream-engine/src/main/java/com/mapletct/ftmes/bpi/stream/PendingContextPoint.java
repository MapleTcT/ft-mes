package com.mapletct.ftmes.bpi.stream;

public record PendingContextPoint(TelemetryPointEvent telemetry, long deadlineEpochMs) {
}
