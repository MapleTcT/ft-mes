package com.mapletct.ftmes.bpi.stream;

import java.util.Objects;

public record KafkaDecodeIssue(
        String code,
        String topic,
        int partition,
        long offset,
        String sourceEventId,
        String detail) {

    public KafkaDecodeIssue {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(topic, "topic");
        Objects.requireNonNull(sourceEventId, "sourceEventId");
        Objects.requireNonNull(detail, "detail");
    }
}
