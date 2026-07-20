package com.mapletct.ftmes.bpiwmsadapter;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Validated
@ConfigurationProperties(prefix = "bpi.wms-adapter")
public record BpiWmsAdapterProperties(
        boolean enabled,
        @NotBlank String bootstrapServers,
        @NotBlank String commandTopic,
        @NotBlank String commandDlqTopic,
        @NotBlank String receiptTopic,
        @NotBlank String groupId,
        @NotBlank String clientId,
        @NotBlank String materialBaseUrl,
        @NotBlank String materialApiKey,
        @NotBlank String timeZone,
        @Min(1024) @Max(5_242_880) int maxPayloadBytes,
        @Min(1) @Max(12) int maxAttempts,
        @Min(1) @Max(8) int concurrency,
        @NotNull Duration retryBackoff,
        @NotNull Duration requestTimeout,
        @NotNull Duration publishTimeout,
        @NotNull List<String> routes) {

    private static final String DENY_ALL = "_DENY_ALL_";
    private static final String DISABLED_KEY = "_DISABLED_";

    public BpiWmsAdapterProperties {
        if (commandTopic != null && commandTopic.equals(commandDlqTopic)) {
            throw new IllegalArgumentException("BPI WMS command and DLQ topics must differ.");
        }
        if (commandTopic != null && commandTopic.equals(receiptTopic)) {
            throw new IllegalArgumentException("BPI WMS command and receipt topics must differ.");
        }
        requirePositive(retryBackoff, "retry backoff");
        requirePositive(requestTimeout, "request timeout");
        requirePositive(publishTimeout, "publish timeout");
        ZoneId.of(timeZone);

        List<String> normalized = new ArrayList<>();
        if (routes != null) {
            for (String route : routes) {
                String value = route == null ? "" : route.trim();
                if (value.isEmpty()) {
                    throw new IllegalArgumentException("BPI WMS route cannot be blank.");
                }
                if (!DENY_ALL.equals(value)) {
                    WmsRoute.parse(value);
                }
                normalized.add(value);
            }
        }
        routes = List.copyOf(normalized);
        if (enabled) {
            if (DISABLED_KEY.equals(materialApiKey)) {
                throw new IllegalArgumentException(
                        "BPI WMS material API key must be configured before activation.");
            }
            if (routes.isEmpty() || routes.stream().allMatch(DENY_ALL::equals)) {
                throw new IllegalArgumentException(
                        "BPI WMS adapter requires at least one exact scope route before activation.");
            }
        }
    }

    Optional<WmsRoute> routeFor(String tenantId, String plantId, String lineId) {
        return routes.stream()
                .filter(value -> !DENY_ALL.equals(value))
                .map(WmsRoute::parse)
                .filter(route -> route.matches(tenantId, plantId, lineId))
                .findFirst();
    }

    ZoneId zoneId() {
        return ZoneId.of(timeZone);
    }

    private static void requirePositive(Duration value, String name) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException("BPI WMS " + name + " must be positive.");
        }
    }
}
