package com.mapletct.ftmes.bpi.interfaces.rest;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ProcessSignalWindowDefinitionCommand(
        @NotBlank @Size(max = 128)
        @Pattern(regexp = "process\\.window\\.[a-z0-9][a-z0-9._-]*")
        String featureRef,
        @NotBlank @Size(max = 128)
        @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._:-]*")
        String signal,
        @NotBlank @Pattern(regexp = "NUMERIC|BOOLEAN")
        String valueType,
        @NotBlank @Pattern(regexp = "MEAN|MIN|MAX|LAST|DELTA|SLOPE|TRUE_RATIO")
        String metric,
        @Min(-3600) @Max(-1)
        int startOffsetSeconds,
        @Min(-3599) @Max(0)
        int endOffsetSeconds,
        @Min(2) @Max(900)
        int minimumSamples,
        @Min(1) @Max(600)
        int maximumGapSeconds,
        @NotBlank @Size(max = 32)
        String expectedUnit,
        boolean requireCalibration,
        @NotEmpty @Size(max = 2)
        List<@Pattern(regexp = "GOOD|SUBSTITUTED") String> acceptedQualityCodes) {
}
