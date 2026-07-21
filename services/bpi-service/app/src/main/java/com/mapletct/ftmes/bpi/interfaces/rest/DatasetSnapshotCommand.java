package com.mapletct.ftmes.bpi.interfaces.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DatasetSnapshotCommand(
        @NotNull Instant freezeAt,
        @NotEmpty @Size(max = 100) List<@NotBlank @Size(max = 128) String> lineIds,
        @NotBlank @Size(max = 64) String predictionTimePolicy,
        @Size(max = 100) List<UUID> ruleVersionIds,
        Boolean excludeLowConfidence,
        @NotBlank @Size(max = 500) String reason) {

    public boolean effectiveExcludeLowConfidence() {
        return excludeLowConfidence == null || excludeLowConfidence;
    }
}
