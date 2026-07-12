package com.mapletct.ftmes.bpi.stream;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.record.TimestampType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class KafkaIngressDeserializationSchemaTest {

    @Test
    void capturesConsumerRecordMetadataAndBytes() throws Exception {
        ConsumerRecord<byte[], byte[]> source = new ConsumerRecord<>(
                "telemetry.v1",
                4,
                19L,
                1_720_000_000_321L,
                TimestampType.CREATE_TIME,
                2,
                3,
                new byte[]{1, 2},
                new byte[]{3, 4, 5},
                new org.apache.kafka.common.header.internals.RecordHeaders(),
                Optional.of(8));
        List<byte[]> output = new ArrayList<>();

        new KafkaIngressDeserializationSchema().deserialize(source, collector(output));

        assertEquals(1, output.size());
        KafkaIngressRecord restored = KafkaIngressRecordCodec.decode(output.get(0));
        assertEquals("telemetry.v1", restored.topic());
        assertEquals(4, restored.partition());
        assertEquals(19L, restored.offset());
        assertEquals(1_720_000_000_321L, restored.timestamp());
        assertArrayEquals(new byte[]{1, 2}, restored.key());
        assertArrayEquals(new byte[]{3, 4, 5}, restored.value());
    }

    @Test
    void preservesKafkaTombstonesAndDeclaresByteArrayOutput() throws Exception {
        ConsumerRecord<byte[], byte[]> tombstone = new ConsumerRecord<>(
                "context.v1", 0, 5L, null, null);
        List<byte[]> output = new ArrayList<>();
        KafkaIngressDeserializationSchema schema = new KafkaIngressDeserializationSchema();

        schema.deserialize(tombstone, collector(output));

        KafkaIngressRecord restored = KafkaIngressRecordCodec.decode(output.get(0));
        assertNull(restored.key());
        assertNull(restored.value());
        assertEquals(TypeInformation.of(byte[].class), schema.getProducedType());
    }

    private static Collector<byte[]> collector(List<byte[]> output) {
        return new Collector<>() {
            @Override
            public void collect(byte[] value) {
                output.add(value);
            }

            @Override
            public void close() {
            }
        };
    }
}
