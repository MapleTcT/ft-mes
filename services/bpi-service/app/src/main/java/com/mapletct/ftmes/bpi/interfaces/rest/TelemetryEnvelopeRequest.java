package com.mapletct.ftmes.bpi.interfaces.rest;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public record TelemetryEnvelopeRequest(
        @NotBlank @Size(max = 128) String eventId,
        @NotBlank @Size(max = 128) String messageId,
        @NotBlank @Size(max = 64) String tenantId,
        @NotBlank @Size(max = 64) String plantId,
        @NotBlank @Size(max = 128) String lineId,
        @NotBlank @Size(max = 128) String gatewayId,
        @NotBlank @Size(max = 128) String productId,
        @NotBlank @Size(max = 128) String deviceId,
        @Positive long eventTimeMs,
        @Positive long ingestTimeMs,
        @NotNull @DecimalMin("0") BigInteger sequence,
        @NotNull List<JsonNode> points,
        @Size(max = 32) Map<@Size(max = 64) String, @Size(max = 256) String> headers,
        @NotNull @DecimalMin("0") BigInteger sourceEpoch,
        @NotBlank String sequenceOrigin) {
}
