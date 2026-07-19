package com.mapletct.ftmes.bpi.interfaces.rest;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record ShadowRunCreateCommand(
        @NotBlank @Size(max = 128)
        @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._:-]*") String runCode,
        @NotBlank @Size(max = 256) String name,
        @NotBlank @Size(max = 64) String plantId,
        @NotBlank @Size(max = 128) String lineId,
        @NotNull UUID ruleVersionId,
        @Min(7) @Max(14) int minimumDurationDays,
        @Min(10) @Max(10000) int minimumReviewedBatches,
        @Min(0) @Max(3600) int boundaryToleranceSeconds,
        @NotNull @DecimalMin("0.950000") @DecimalMax("1.000000")
        BigDecimal minimumBoundaryAgreement,
        @NotNull @DecimalMin(value = "0.000001") @DecimalMax("100.000000")
        BigDecimal quantityTolerancePercent,
        @NotBlank @Size(max = 500) String reason) {
}
