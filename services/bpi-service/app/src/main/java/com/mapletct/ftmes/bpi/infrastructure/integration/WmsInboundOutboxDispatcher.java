package com.mapletct.ftmes.bpi.infrastructure.integration;

import com.mapletct.ftmes.bpi.domain.OutboxEventClaim;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(prefix = "bpi.wms-outbox", name = "enabled", havingValue = "true")
public class WmsInboundOutboxDispatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(WmsInboundOutboxDispatcher.class);

    private final WmsInboundOutboxRepository repository;
    private final BpiWmsOutboxProperties properties;
    private final KafkaTemplate<byte[], byte[]> kafkaTemplate;

    public WmsInboundOutboxDispatcher(
            WmsInboundOutboxRepository repository,
            BpiWmsOutboxProperties properties,
            @Qualifier("bpiWmsInboundKafkaTemplate") KafkaTemplate<byte[], byte[]> kafkaTemplate) {
        this.repository = repository;
        this.properties = properties;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${bpi.wms-outbox.poll-delay:1s}")
    public void dispatchPending() {
        List<OutboxEventClaim> claims = repository.claimPending(
                properties.batchSize(), properties.claimTimeout());
        for (OutboxEventClaim claim : claims) dispatch(claim);
    }

    private void dispatch(OutboxEventClaim claim) {
        try {
            ProducerRecord<byte[], byte[]> record = new ProducerRecord<>(
                    claim.topic(), null, null,
                    claim.partitionKey().getBytes(StandardCharsets.UTF_8),
                    claim.payload(), headers(claim));
            kafkaTemplate.send(record).get(30, TimeUnit.SECONDS);
            if (!repository.markPublished(claim.id(), claim.claimToken())) {
                LOGGER.warn("BPI WMS outbox claim was lost after Kafka send: {}", claim.id());
            }
        } catch (Exception error) {
            String message = error.getMessage() == null
                    ? error.getClass().getSimpleName() : error.getMessage();
            repository.markFailed(
                    claim.id(), claim.claimToken(), claim.attemptCount(), properties.maxAttempts(),
                    properties.retryBackoff(), message);
            LOGGER.warn("BPI WMS outbox dispatch failed for {}: {}", claim.id(), message);
        }
    }

    private Iterable<Header> headers(OutboxEventClaim claim) {
        List<Header> values = new ArrayList<>();
        claim.headers().forEach((key, value) -> values.add(new RecordHeader(
                key, value.getBytes(StandardCharsets.UTF_8))));
        values.add(new RecordHeader(
                "outbox_event_id", claim.id().toString().getBytes(StandardCharsets.UTF_8)));
        return values;
    }
}
