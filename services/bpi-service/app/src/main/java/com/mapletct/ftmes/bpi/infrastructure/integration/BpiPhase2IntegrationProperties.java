package com.mapletct.ftmes.bpi.infrastructure.integration;

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
@ConfigurationProperties(prefix = "bpi.phase2-integration")
public record BpiPhase2IntegrationProperties(
        boolean enabled,
        boolean protobufHttpIngressEnabled,
        boolean kafkaEnabled,
        @Min(1024) @Max(5_242_880) int maxPayloadBytes,
        @NotBlank String bootstrapServers,
        @NotBlank String qcsTopic,
        @NotBlank String qcsDlqTopic,
        @NotBlank String wmsReceiptTopic,
        @NotBlank String wmsReceiptDlqTopic,
        @NotBlank String groupId,
        @NotBlank String clientId,
        @NotBlank String qcsActorId,
        @NotBlank String wmsActorId,
        @NotEmpty Set<String> allowedTenantIds,
        @NotEmpty Set<String> allowedPlantIds,
        @NotEmpty Set<String> allowedLineIds,
        @Min(1) @Max(24) int concurrency,
        @Min(1) @Max(10) int maxAttempts,
        @NotNull Duration retryBackoff) {

    public BpiPhase2IntegrationProperties {
        if (qcsTopic != null && qcsTopic.equals(qcsDlqTopic)) {
            throw new IllegalArgumentException("QCS source and DLQ topics must differ.");
        }
        if (wmsReceiptTopic != null && wmsReceiptTopic.equals(wmsReceiptDlqTopic)) {
            throw new IllegalArgumentException("WMS receipt source and DLQ topics must differ.");
        }
        allowedTenantIds = normalizedScope(allowedTenantIds, "tenant");
        allowedPlantIds = normalizedScope(allowedPlantIds, "plant");
        allowedLineIds = normalizedScope(allowedLineIds, "line");
        if (retryBackoff == null || retryBackoff.isZero() || retryBackoff.isNegative()) {
            throw new IllegalArgumentException("Phase 2 integration retry backoff must be positive.");
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
            throw new IllegalArgumentException("Phase 2 " + scope + " scope is required.");
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Phase 2 " + scope + " scope cannot be blank.");
            }
            normalized.add(value.trim());
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Phase 2 " + scope + " scope is required.");
        }
        return Set.copyOf(normalized);
    }
}
