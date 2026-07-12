package com.mapletct.ftmes.bpi.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record EvidenceView(
        @NotBlank @Size(max = 256) String eventId,
        @NotBlank @Size(max = 128) String signal,
        @NotBlank @Pattern(regexp = "REQUIRED|QUORUM|OPTIONAL") String classification,
        boolean satisfied,
        @Size(max = 1024) String value,
        @Size(max = 32) String unit,
        @NotBlank @Pattern(regexp = "GOOD|UNCERTAIN|BAD|STALE") String quality,
        @NotNull Instant eventTime,
        @NotBlank @Size(max = 128) String source) {
}
