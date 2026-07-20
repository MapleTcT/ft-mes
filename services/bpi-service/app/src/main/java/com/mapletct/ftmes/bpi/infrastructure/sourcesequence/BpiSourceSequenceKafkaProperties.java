package com.mapletct.ftmes.bpi.infrastructure.sourcesequence;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;

@Validated
@ConfigurationProperties(prefix = "bpi.source-sequence-kafka")
public record BpiSourceSequenceKafkaProperties(
        boolean enabled,
        @NotBlank String bootstrapServers,
        @NotBlank String topic,
        @NotBlank String dlqTopic,
        @NotBlank String groupId,
        @NotBlank String clientId,
        @NotBlank String actorId,
        @NotEmpty Set<String> allowedTenantIds,
        @NotEmpty Set<String> allowedPlantIds,
        @NotEmpty Set<String> allowedLineIds,
        @Min(1) @Max(8) int concurrency,
        @Min(1) @Max(10) int maxAttempts,
        @NotNull Duration retryBackoff,
        @Min(1024) @Max(1048576) int maxPayloadBytes) {

    public BpiSourceSequenceKafkaProperties {
        if (topic != null && topic.equals(dlqTopic)) {
            throw new IllegalArgumentException("Source sequence evidence source and DLQ topics must differ.");
        }
        allowedTenantIds = normalizedScope(allowedTenantIds, "tenant");
        allowedPlantIds = normalizedScope(allowedPlantIds, "plant");
        allowedLineIds = normalizedScope(allowedLineIds, "line");
        if (retryBackoff == null || retryBackoff.isNegative() || retryBackoff.isZero()) {
            throw new IllegalArgumentException("Source sequence evidence Kafka retry backoff must be positive.");
        }
    }

    public boolean allows(String tenantId, String plantId, String lineId) {
        return allows(allowedTenantIds, tenantId)
                && allows(allowedPlantIds, plantId)
                && allows(allowedLineIds, lineId);
    }

    private static boolean allows(Set<String> configured, String value) {
        return configured.contains("*") || configured.contains(value);
    }

    private static Set<String> normalizedScope(Set<String> values, String scope) {
        if (values == null) {
            throw new IllegalArgumentException("Source sequence evidence Kafka " + scope + " scope is required.");
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(
                        "Source sequence evidence Kafka " + scope + " scope cannot be blank.");
            }
            normalized.add(value.trim());
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Source sequence evidence Kafka " + scope + " scope is required.");
        }
        return Set.copyOf(normalized);
    }
}
