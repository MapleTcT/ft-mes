package com.mapletct.ftmes.bpi.infrastructure.integration;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "bpi.wms-outbox")
public record BpiWmsOutboxProperties(
        boolean enabled,
        @NotBlank String bootstrapServers,
        @NotBlank String topic,
        @NotBlank String reversalTopic,
        @NotBlank String clientId,
        @Min(1) @Max(500) int batchSize,
        @Min(1) @Max(100) int maxAttempts,
        @NotNull Duration pollDelay,
        @NotNull Duration claimTimeout,
        @NotNull Duration retryBackoff,
        @NotNull Duration reconciliationDelay) {

    public BpiWmsOutboxProperties {
        if (topic != null && topic.equals(reversalTopic)) {
            throw new IllegalArgumentException(
                    "WMS inbound and reversal outbox topics must differ.");
        }
        positive(pollDelay, "pollDelay");
        positive(claimTimeout, "claimTimeout");
        positive(retryBackoff, "retryBackoff");
        positive(reconciliationDelay, "reconciliationDelay");
    }

    private static void positive(Duration value, String field) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException("WMS outbox " + field + " must be positive.");
        }
    }
}
