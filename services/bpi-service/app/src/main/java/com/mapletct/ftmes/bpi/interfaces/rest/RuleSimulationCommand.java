package com.mapletct.ftmes.bpi.interfaces.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record RuleSimulationCommand(
        @NotBlank String lineId,
        @NotNull Instant from,
        @NotNull Instant to,
        @NotBlank String topologyVersion,
        @NotBlank String calibrationVersion,
        @NotBlank String goldenSetId) {
}
