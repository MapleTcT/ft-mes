package com.mapletct.ftmes.bpi.domain;

import java.util.Map;
import java.util.UUID;

public record RulePublicationEnvelope(
        UUID eventId,
        String topic,
        String partitionKey,
        byte[] payload,
        Map<String, String> headers) {

    public RulePublicationEnvelope {
        payload = payload.clone();
        headers = Map.copyOf(headers);
    }

    @Override
    public byte[] payload() {
        return payload.clone();
    }
}
