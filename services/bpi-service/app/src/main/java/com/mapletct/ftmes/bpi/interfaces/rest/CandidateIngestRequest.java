package com.mapletct.ftmes.bpi.interfaces.rest;

import com.mapletct.ftmes.bpi.domain.BoundaryType;
import com.mapletct.ftmes.bpi.domain.EvidenceView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CandidateIngestRequest(
        @NotBlank @Size(max = 256) String eventId,
        @NotNull UUID candidateKey,
        @NotBlank @Size(max = 64) String plantId,
        @NotBlank @Size(max = 128) String lineId,
        @NotNull BoundaryType boundaryType,
        @Size(max = 128) String orderId,
        @NotNull Instant boundaryTime,
        @NotBlank @Size(max = 128) String ruleCode,
        @NotBlank @Size(max = 64) String ruleVersion,
        @NotBlank @Size(max = 128) String topologyCode,
        @NotBlank @Size(max = 64) String topologyVersion,
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal confidence,
        @NotEmpty @Size(max = 100) List<@Valid EvidenceView> evidence,
        @Size(max = 100) List<@NotBlank @Size(max = 128) String> missingSignals) {

    public List<String> normalizedMissingSignals() {
        return missingSignals == null ? List.of() : List.copyOf(missingSignals);
    }
}
