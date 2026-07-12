package com.mapletct.ftmes.bpi.stream;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KafkaIngressRecordCodecTest {

    @Test
    void roundTripsKafkaMetadataAndPayload() {
        KafkaIngressRecord original = new KafkaIngressRecord(
                "telemetry.v1", 3, 42L, 1_720_000_000_123L,
                new byte[]{1, 2, 3}, new byte[]{4, 5, 6});

        KafkaIngressRecord restored = KafkaIngressRecordCodec.decode(
                KafkaIngressRecordCodec.encode(original));

        assertEquals(original.topic(), restored.topic());
        assertEquals(original.partition(), restored.partition());
        assertEquals(original.offset(), restored.offset());
        assertEquals(original.timestamp(), restored.timestamp());
        assertArrayEquals(original.key(), restored.key());
        assertArrayEquals(original.value(), restored.value());
    }

    @Test
    void roundTripsNullKeyAndTombstoneValue() {
        KafkaIngressRecord restored = KafkaIngressRecordCodec.decode(
                KafkaIngressRecordCodec.encode(new KafkaIngressRecord(
                        "context.v1", 0, 7L, 99L, null, null)));

        assertNull(restored.key());
        assertNull(restored.value());
    }

    @Test
    void rejectsUnknownHeaderTrailingDataAndTruncation() {
        byte[] encoded = KafkaIngressRecordCodec.encode(new KafkaIngressRecord(
                "rules.v1", 1, 2L, 3L, new byte[]{1}, new byte[]{2}));

        byte[] unknownVersion = Arrays.copyOf(encoded, encoded.length);
        unknownVersion[7] = 2;
        byte[] trailingData = Arrays.copyOf(encoded, encoded.length + 1);
        byte[] truncated = Arrays.copyOf(encoded, encoded.length - 1);

        assertDecodeFailure(unknownVersion, "unsupported Kafka ingress record header");
        assertDecodeFailure(trailingData, "Kafka ingress record has trailing data");
        assertDecodeFailure(truncated, "truncated Kafka ingress field");
    }

    @Test
    void enforcesOneMiBFieldLimitDuringEncoding() {
        KafkaIngressRecord maximum = new KafkaIngressRecord(
                "telemetry.v1", 0, 0L, 0L, null, new byte[1_048_576]);
        KafkaIngressRecord oversized = new KafkaIngressRecord(
                "telemetry.v1", 0, 0L, 0L, null, new byte[1_048_577]);

        assertEquals(1_048_576, KafkaIngressRecordCodec.decode(
                KafkaIngressRecordCodec.encode(maximum)).value().length);
        IllegalStateException error = assertThrows(
                IllegalStateException.class, () -> KafkaIngressRecordCodec.encode(oversized));
        assertEquals("cannot encode Kafka ingress record", error.getMessage());
        assertTrue(error.getCause().getMessage().contains("exceeds one MiB"));
    }

    private static void assertDecodeFailure(byte[] bytes, String causeMessage) {
        IllegalStateException error = assertThrows(
                IllegalStateException.class, () -> KafkaIngressRecordCodec.decode(bytes));
        assertEquals("cannot decode Kafka ingress record", error.getMessage());
        assertTrue(error.getCause().getMessage().contains(causeMessage));
    }
}
