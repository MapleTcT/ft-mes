package com.mapletct.ftmes.bpi.infrastructure.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "bpi.security")
public record BpiSecurityProperties(
        @NotBlank @Size(min = 32) String internalJwtSecret,
        @NotBlank String issuer,
        @NotBlank String audience,
        @jakarta.validation.constraints.NotNull Duration maxTokenTtl) {
}
