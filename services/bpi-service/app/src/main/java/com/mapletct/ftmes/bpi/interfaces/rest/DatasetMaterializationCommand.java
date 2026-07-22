package com.mapletct.ftmes.bpi.interfaces.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DatasetMaterializationCommand(
        @NotBlank
        @Pattern(regexp = "PARQUET", message = "artifactFormat must be PARQUET")
        String artifactFormat,
        @NotBlank @Size(min = 3, max = 500) String reason) {
}
