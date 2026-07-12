package com.mapletct.ftmes.bpi.infrastructure.candidate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "bpi.candidate-event")
public record BpiCandidateEventProperties(
        boolean protobufHttpIngressEnabled,
        @Min(1024) @Max(1048576) int maxPayloadBytes) {
}
