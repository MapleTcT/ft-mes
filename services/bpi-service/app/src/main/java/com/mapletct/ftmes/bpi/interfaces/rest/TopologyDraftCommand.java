package com.mapletct.ftmes.bpi.interfaces.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.UUID;

public record TopologyDraftCommand(
        @NotBlank @Size(max = 128)
        @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._:-]*") String code,
        @NotBlank @Size(max = 64)
        @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]*") String version,
        @NotBlank @Size(max = 64) String plantId,
        @NotBlank @Size(max = 128) String lineId,
        UUID baseVersionId,
        @NotEmpty Map<String, Object> definition,
        @NotBlank @Size(min = 3, max = 500) String reason) {
}
