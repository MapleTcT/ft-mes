package com.mapletct.ftmes.bpi.stream;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.connector.kafka.source.reader.deserializer.KafkaRecordDeserializationSchema;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.ConsumerRecord;

public final class KafkaIngressDeserializationSchema implements KafkaRecordDeserializationSchema<byte[]> {

    @Override
    public void deserialize(ConsumerRecord<byte[], byte[]> record, Collector<byte[]> output) {
        output.collect(KafkaIngressRecordCodec.encode(new KafkaIngressRecord(
                record.topic(),
                record.partition(),
                record.offset(),
                record.timestamp(),
                record.key(),
                record.value())));
    }

    @Override
    public TypeInformation<byte[]> getProducedType() {
        return TypeInformation.of(byte[].class);
    }
}
