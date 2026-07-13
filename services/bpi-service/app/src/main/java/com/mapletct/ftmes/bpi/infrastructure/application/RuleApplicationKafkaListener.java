package com.mapletct.ftmes.bpi.infrastructure.application;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "bpi.rule-application-kafka", name = "enabled", havingValue = "true")
public class RuleApplicationKafkaListener {
    private final RuleApplicationKafkaRecordProcessor processor;

    public RuleApplicationKafkaListener(RuleApplicationKafkaRecordProcessor processor) {
        this.processor = processor;
    }

    @KafkaListener(
            id = "${bpi.rule-application-kafka.client-id}-listener",
            topics = "${bpi.rule-application-kafka.topic}",
            groupId = "${bpi.rule-application-kafka.group-id}",
            containerFactory = "bpiRuleApplicationKafkaListenerContainerFactory")
    public void receive(ConsumerRecord<byte[], byte[]> record, Acknowledgment acknowledgment) {
        processor.process(record);
        acknowledgment.acknowledge();
    }
}
