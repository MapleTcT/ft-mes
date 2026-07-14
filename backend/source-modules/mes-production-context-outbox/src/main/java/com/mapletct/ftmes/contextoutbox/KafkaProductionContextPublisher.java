package com.mapletct.ftmes.contextoutbox;

import com.mapletct.ftmes.bpi.contract.v1.ProductionContextEventV1;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "mes.production-context.outbox.enabled", havingValue = "true")
public class KafkaProductionContextPublisher implements ProductionContextPublisher {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final ProductionContextOutboxProperties properties;

    public KafkaProductionContextPublisher(
        KafkaTemplate<String, byte[]> kafkaTemplate,
        ProductionContextOutboxProperties properties
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    @Override
    public void publish(String topic, String key, ProductionContextEventV1 event) throws Exception {
        kafkaTemplate.send(topic, key, event.toByteArray())
            .get(properties.getSendTimeoutMs(), TimeUnit.MILLISECONDS);
    }
}
