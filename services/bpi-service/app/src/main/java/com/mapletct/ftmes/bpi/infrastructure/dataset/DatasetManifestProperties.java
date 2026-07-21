package com.mapletct.ftmes.bpi.infrastructure.dataset;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "bpi.dataset-manifest")
public record DatasetManifestProperties(
        boolean enabled,
        @Min(1) @Max(100) int batchSize,
        @Min(1) @Max(20) int maxAttempts,
        @NotNull Duration pollDelay,
        @NotNull Duration claimTimeout) {

    public DatasetManifestProperties {
        positive(pollDelay, "pollDelay");
        positive(claimTimeout, "claimTimeout");
    }

    private static void positive(Duration value, String field) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException("Dataset manifest " + field + " must be positive.");
        }
    }
}
