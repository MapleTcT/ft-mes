package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.contract.v1.ProductionContextEventV1;

import java.util.Objects;

public record ContextualTelemetryPoint(
        TelemetryPointEvent telemetry,
        ProductionContextEventV1 context) {

    public ContextualTelemetryPoint {
        Objects.requireNonNull(telemetry, "telemetry");
        Objects.requireNonNull(context, "context");
    }
}
