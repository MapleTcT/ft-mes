package com.mapletct.ftmes.bpi.infrastructure.application;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "bpi.rule-application-kafka", name = "enabled", havingValue = "true")
public class RuleApplicationKafkaListener {
    private final RuleApplicationKafkaRecordProcessor applicationProcessor;
    private final RuleRuntimeReadinessKafkaRecordProcessor readinessProcessor;
    private final BpiRuleApplicationKafkaProperties properties;

    public RuleApplicationKafkaListener(
            RuleApplicationKafkaRecordProcessor applicationProcessor,
            RuleRuntimeReadinessKafkaRecordProcessor readinessProcessor,
            BpiRuleApplicationKafkaProperties properties) {
        this.applicationProcessor = applicationProcessor;
        this.readinessProcessor = readinessProcessor;
        this.properties = properties;
    }

    @KafkaListener(
            id = "${bpi.rule-application-kafka.client-id}-listener",
            topics = {
                "${bpi.rule-application-kafka.topic}",
                "${bpi.rule-application-kafka.runtime-readiness-topic}"
            },
            groupId = "${bpi.rule-application-kafka.group-id}",
            containerFactory = "bpiRuleApplicationKafkaListenerContainerFactory")
    public void receive(ConsumerRecord<byte[], byte[]> record, Acknowledgment acknowledgment) {
        if (properties.runtimeReadinessTopic().equals(record.topic())) {
            readinessProcessor.process(record);
        } else {
            applicationProcessor.process(record);
        }
        acknowledgment.acknowledge();
    }
}
