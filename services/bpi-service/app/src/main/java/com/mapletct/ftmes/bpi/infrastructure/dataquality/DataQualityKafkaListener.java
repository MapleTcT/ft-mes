package com.mapletct.ftmes.bpi.infrastructure.dataquality;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "bpi.data-quality-kafka", name = "enabled", havingValue = "true")
public class DataQualityKafkaListener {
    private final DataQualityKafkaRecordProcessor processor;

    public DataQualityKafkaListener(DataQualityKafkaRecordProcessor processor) {
        this.processor = processor;
    }

    @KafkaListener(
            id = "${bpi.data-quality-kafka.client-id}-listener",
            topics = "${bpi.data-quality-kafka.topic}",
            groupId = "${bpi.data-quality-kafka.group-id}",
            containerFactory = "bpiDataQualityKafkaListenerContainerFactory")
    public void receive(ConsumerRecord<byte[], byte[]> record, Acknowledgment acknowledgment) {
        processor.process(record);
        acknowledgment.acknowledge();
    }
}
