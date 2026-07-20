package com.mapletct.ftmes.bpi.infrastructure.integration;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "bpi.phase2-integration", name = "kafka-enabled", havingValue = "true")
public class Phase2IntegrationKafkaListener {
    private final Phase2IntegrationKafkaRecordProcessor processor;

    public Phase2IntegrationKafkaListener(Phase2IntegrationKafkaRecordProcessor processor) {
        this.processor = processor;
    }

    @KafkaListener(
            topics = "${bpi.phase2-integration.qcs-topic}",
            containerFactory = "bpiPhase2IntegrationKafkaListenerContainerFactory")
    public void onQcsQualityGate(ConsumerRecord<byte[], byte[]> record, Acknowledgment acknowledgment) {
        processor.processQcs(record);
        acknowledgment.acknowledge();
    }

    @KafkaListener(
            topics = "${bpi.phase2-integration.wms-receipt-topic}",
            containerFactory = "bpiPhase2IntegrationKafkaListenerContainerFactory")
    public void onWmsReceipt(ConsumerRecord<byte[], byte[]> record, Acknowledgment acknowledgment) {
        processor.processWmsReceipt(record);
        acknowledgment.acknowledge();
    }
}
