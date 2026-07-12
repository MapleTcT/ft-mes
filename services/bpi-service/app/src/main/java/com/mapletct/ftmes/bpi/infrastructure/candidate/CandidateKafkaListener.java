package com.mapletct.ftmes.bpi.infrastructure.candidate;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "bpi.candidate-kafka", name = "enabled", havingValue = "true")
public class CandidateKafkaListener {
    private final CandidateKafkaRecordProcessor processor;

    public CandidateKafkaListener(CandidateKafkaRecordProcessor processor) {
        this.processor = processor;
    }

    @KafkaListener(
            id = "${bpi.candidate-kafka.client-id}-listener",
            topics = "${bpi.candidate-kafka.topic}",
            groupId = "${bpi.candidate-kafka.group-id}",
            containerFactory = "bpiCandidateKafkaListenerContainerFactory")
    public void receive(ConsumerRecord<byte[], byte[]> record, Acknowledgment acknowledgment) {
        processor.process(record);
        acknowledgment.acknowledge();
    }
}
