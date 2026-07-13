package com.mapletct.ftmes.bpi.domain;

import java.util.Map;
import java.util.UUID;

public record OutboxEventClaim(
        UUID id,
        UUID claimToken,
        String topic,
        String partitionKey,
        byte[] payload,
        Map<String, String> headers,
        int attemptCount) {

    public OutboxEventClaim {
        payload = payload.clone();
        headers = Map.copyOf(headers);
    }

    @Override
    public byte[] payload() {
        return payload.clone();
    }
}
