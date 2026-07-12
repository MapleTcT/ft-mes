package com.mapletct.ftmes.bpi.infrastructure.telemetry;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.Set;

@Validated
@ConfigurationProperties(prefix = "bpi.telemetry")
public record BpiTelemetryProperties(
        boolean httpIngressEnabled,
        @Min(1024) @Max(1048576) int maxPayloadBytes,
        @Min(1) @Max(5000) int maxPointsPerEnvelope,
        Duration maxFutureSkew,
        @NotEmpty Set<String> acceptedUnits,
        @NotEmpty Set<String> acceptedQualities) {
}
