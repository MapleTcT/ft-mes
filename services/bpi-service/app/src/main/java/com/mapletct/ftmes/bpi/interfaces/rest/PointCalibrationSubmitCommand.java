package com.mapletct.ftmes.bpi.interfaces.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record PointCalibrationSubmitCommand(
        @NotBlank @Size(max = 64) String plantId,
        @NotBlank @Size(max = 128) String lineId,
        @NotBlank @Size(max = 128) String productId,
        @NotBlank @Size(max = 128) String deviceId,
        @NotBlank @Size(max = 128) String propertyId,
        @NotBlank @Size(max = 128) String calibrationVersion,
        @NotBlank @Size(max = 512) String certificateReference,
        @NotBlank @Pattern(regexp = "^[a-f0-9]{64}$") String certificateChecksum,
        @NotNull Instant validFrom,
        @NotNull Instant validUntil,
        @NotBlank @Size(min = 3, max = 500) String reason) {
}
