package com.mapletct.ftmes.bpi.infrastructure.telemetry;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "bpi.telemetry-kafka", name = "enabled", havingValue = "true")
public class TelemetryKafkaListener {
    private final TelemetryKafkaRecordProcessor processor;

    public TelemetryKafkaListener(TelemetryKafkaRecordProcessor processor) {
        this.processor = processor;
    }

    @KafkaListener(
            id = "${bpi.telemetry-kafka.client-id}-listener",
            topics = "${bpi.telemetry-kafka.topic}",
            groupId = "${bpi.telemetry-kafka.group-id}",
            containerFactory = "bpiTelemetryKafkaListenerContainerFactory")
    public void receive(ConsumerRecord<byte[], byte[]> record, Acknowledgment acknowledgment) {
        processor.process(record);
        acknowledgment.acknowledge();
    }
}
