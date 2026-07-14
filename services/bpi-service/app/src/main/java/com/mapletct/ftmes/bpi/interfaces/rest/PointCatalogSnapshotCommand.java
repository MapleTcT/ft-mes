package com.mapletct.ftmes.bpi.interfaces.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public record PointCatalogSnapshotCommand(
        @NotBlank @Size(max = 64) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._:-]*") String source,
        @NotBlank @Size(max = 128) String sourceInstance,
        @NotBlank @Size(max = 128) String sourceRevision,
        @NotBlank @Size(max = 64) String plantId,
        @NotBlank @Size(max = 128) String lineId,
        @NotNull Instant observedAt,
        @NotNull @Size(max = 10_000) List<@Valid PointCatalogPointCommand> points,
        @NotBlank @Size(min = 3, max = 500) String reason) {
}
