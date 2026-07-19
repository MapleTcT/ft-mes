package com.mapletct.ftmes.bpi.interfaces.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DataQualityAcknowledgeCommand(
        @NotBlank @Size(min = 1, max = 128) String assignee,
        @NotBlank @Size(min = 3, max = 500) String reason) {
}
