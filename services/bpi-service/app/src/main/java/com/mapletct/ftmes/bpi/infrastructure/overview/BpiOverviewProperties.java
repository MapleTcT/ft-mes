package com.mapletct.ftmes.bpi.infrastructure.overview;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "bpi.overview")
public record BpiOverviewProperties(
        @NotNull Duration telemetryFreshness,
        @Min(1) @Max(1440) int defaultTrendWindowMinutes,
        @Min(2) @Max(500) int defaultTrendLimit) {

    public BpiOverviewProperties {
        if (telemetryFreshness == null || telemetryFreshness.isZero() || telemetryFreshness.isNegative()) {
            throw new IllegalArgumentException("BPI overview telemetryFreshness must be positive.");
        }
    }
}
