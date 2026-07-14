package com.mapletct.ftmes.bpi.infrastructure.pointcatalog;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "bpi.point-catalog-kafka", name = "enabled", havingValue = "true")
public class PointCatalogKafkaListener {
    private final PointCatalogKafkaRecordProcessor processor;

    public PointCatalogKafkaListener(PointCatalogKafkaRecordProcessor processor) {
        this.processor = processor;
    }

    @KafkaListener(
            id = "${bpi.point-catalog-kafka.client-id}-listener",
            topics = "${bpi.point-catalog-kafka.topic}",
            groupId = "${bpi.point-catalog-kafka.group-id}",
            containerFactory = "bpiPointCatalogKafkaListenerContainerFactory")
    public void receive(ConsumerRecord<byte[], byte[]> record, Acknowledgment acknowledgment) {
        processor.process(record);
        acknowledgment.acknowledge();
    }
}
