package com.mapletct.ftmes.bpi.interfaces.rest;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record DatasetDefinitionCreateCommand(
        @NotBlank @Size(max = 128)
        @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._:-]*") String datasetCode,
        @NotBlank @Size(max = 64)
        @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]*") String version,
        @NotBlank @Size(max = 256) String name,
        @NotBlank @Size(max = 64) String plantId,
        @NotEmpty @Size(max = 100) List<@NotBlank @Size(max = 128) String> lineIds,
        @NotBlank @Size(max = 64) String predictionTimePolicy,
        @NotBlank @Size(max = 64) String featureCutoffPolicy,
        @NotEmpty @Size(max = 50) List<@NotBlank @Size(max = 128) String> featureRefs,
        @NotEmpty @Size(max = 50) List<@NotBlank @Size(max = 128) String> labelRefs,
        @Min(1) @Max(2160) int maxLabelDelayHours,
        @NotNull @DecimalMin("0.000000") @DecimalMax("1.000000") BigDecimal minimumConfidence,
        @NotBlank @Size(max = 32) String splitPolicy,
        @NotBlank @Size(max = 500) String reason) {
}
