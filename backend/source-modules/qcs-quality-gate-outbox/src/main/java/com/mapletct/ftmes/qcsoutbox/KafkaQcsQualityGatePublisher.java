package com.mapletct.ftmes.qcsoutbox;

import com.mapletct.ftmes.bpi.contract.v1.QcsQualityGateV1;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "qcs.bpi.outbox.enabled", havingValue = "true")
public class KafkaQcsQualityGatePublisher implements QcsQualityGatePublisher {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final QcsQualityGateOutboxProperties properties;

    public KafkaQcsQualityGatePublisher(
            KafkaTemplate<String, byte[]> qcsQualityGateKafkaTemplate,
            QcsQualityGateOutboxProperties properties) {
        this.kafkaTemplate = qcsQualityGateKafkaTemplate;
        this.properties = properties;
    }

    @Override
    public void publish(String topic, String key, QcsQualityGateV1 event) throws Exception {
        ProducerRecord<String, byte[]> record = new ProducerRecord<String, byte[]>(
            topic, key, event.toByteArray());
        header(record, "event_id", event.getEventId());
        header(record, "idempotency_key", event.getIdempotencyKey());
        header(record, "tenant_id", event.getTenantId());
        header(record, "schema_version", "v1");
        kafkaTemplate.send(record).get(properties.getSendTimeoutMs(), TimeUnit.MILLISECONDS);
    }

    private static void header(ProducerRecord<String, byte[]> record, String name, String value) {
        record.headers().add(name, value.getBytes(StandardCharsets.UTF_8));
    }
}
