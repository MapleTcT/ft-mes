package com.mapletct.ftmes.bpi.interfaces.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record RulePublishCommand(
        @NotBlank @Size(min = 3, max = 500) String reason,
        @NotNull UUID simulationId,
        @NotBlank String simulationChecksum) {
}
