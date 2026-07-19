package com.mapletct.ftmes.bpi.interfaces.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FeatureFlagOverrideCommand(
        @NotBlank String scopeType,
        @NotBlank @Size(max = 64) String plantId,
        @NotBlank @Size(max = 128) String lineId,
        @NotBlank String mode,
        Boolean enabled,
        @NotBlank @Size(min = 8, max = 500) String reason) {
}
