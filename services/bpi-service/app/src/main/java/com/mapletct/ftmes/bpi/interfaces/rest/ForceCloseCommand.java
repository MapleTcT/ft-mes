package com.mapletct.ftmes.bpi.interfaces.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record ForceCloseCommand(
        @NotBlank @Size(min = 3, max = 500) String reason,
        @Size(max = 2000) String comment,
        @NotNull Instant boundaryTime,
        @NotNull ForceCloseApprovalMode approvalMode) {
}
