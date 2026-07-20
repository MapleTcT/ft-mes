package com.mapletct.ftmes.bpi.infrastructure.sourcesequence;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "bpi.source-sequence-kafka", name = "enabled", havingValue = "true")
public class SourceSequenceEvidenceKafkaListener {
    private final SourceSequenceEvidenceKafkaRecordProcessor processor;

    public SourceSequenceEvidenceKafkaListener(SourceSequenceEvidenceKafkaRecordProcessor processor) {
        this.processor = processor;
    }

    @KafkaListener(
            id = "${bpi.source-sequence-kafka.client-id}-listener",
            topics = "${bpi.source-sequence-kafka.topic}",
            groupId = "${bpi.source-sequence-kafka.group-id}",
            containerFactory = "bpiSourceSequenceKafkaListenerContainerFactory")
    public void receive(ConsumerRecord<byte[], byte[]> record, Acknowledgment acknowledgment) {
        processor.process(record);
        acknowledgment.acknowledge();
    }
}
