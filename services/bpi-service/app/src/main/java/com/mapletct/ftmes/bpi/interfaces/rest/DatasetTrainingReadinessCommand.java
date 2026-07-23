package com.mapletct.ftmes.bpi.interfaces.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DatasetTrainingReadinessCommand(
        @NotBlank
        @Pattern(regexp = "BATCH_START_BOUNDARY_REVIEW_RISK")
        String objectiveCode,
        @NotBlank @Size(min = 3, max = 500)
        String reason) {
}
