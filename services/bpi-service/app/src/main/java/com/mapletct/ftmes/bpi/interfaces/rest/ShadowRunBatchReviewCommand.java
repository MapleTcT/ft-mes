package com.mapletct.ftmes.bpi.interfaces.rest;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ShadowRunBatchReviewCommand(
        @NotNull UUID batchId,
        @NotNull Instant manualStartTime,
        @NotNull Instant manualEndTime,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal referenceQuantity,
        @NotBlank @Size(max = 32) String quantityUnit,
        @NotBlank @Size(max = 500) String reason) {
}
