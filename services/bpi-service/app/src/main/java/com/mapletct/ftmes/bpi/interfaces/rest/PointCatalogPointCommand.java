package com.mapletct.ftmes.bpi.interfaces.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PointCatalogPointCommand(
        @Size(max = 128) String localityGroup,
        @NotBlank @Size(max = 128) String productId,
        @NotBlank @Size(max = 128) String deviceId,
        @NotBlank @Size(max = 128) String propertyId,
        @Size(max = 128) String sourcePropertyId,
        @Size(max = 256) String pointName,
        @Size(max = 32) String unit,
        @Size(max = 64) String dataType,
        @NotBlank @Pattern(regexp = "ACTIVE|INACTIVE|UNKNOWN") String deviceState,
        boolean registered,
        boolean propertyPresent,
        @Size(max = 128) String calibrationVersion,
        @NotBlank @Pattern(regexp = "VERIFIED|UNVERIFIED|MISSING") String calibrationStatus,
        boolean sourceSequenceEnabled) {
}
