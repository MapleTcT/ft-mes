package com.mapletct.ftmes.bpi.stream;

import java.util.Arrays;
import java.util.Objects;

public record KafkaIngressRecord(
        String topic,
        int partition,
        long offset,
        long timestamp,
        byte[] key,
        byte[] value) {

    public KafkaIngressRecord {
        Objects.requireNonNull(topic, "topic");
        key = key == null ? null : Arrays.copyOf(key, key.length);
        value = value == null ? null : Arrays.copyOf(value, value.length);
    }

    @Override
    public byte[] key() {
        return key == null ? null : Arrays.copyOf(key, key.length);
    }

    @Override
    public byte[] value() {
        return value == null ? null : Arrays.copyOf(value, value.length);
    }
}
