package com.mapletct.ftmes.bpi.interfaces.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReasonCommand(
        @NotBlank @Size(min = 3, max = 500) String reason,
        @Size(max = 2000) String comment) {
}
