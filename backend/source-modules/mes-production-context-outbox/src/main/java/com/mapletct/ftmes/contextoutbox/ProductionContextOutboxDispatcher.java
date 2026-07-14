package com.mapletct.ftmes.contextoutbox;

import com.mapletct.ftmes.bpi.contract.v1.ProductionContextEventV1;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.util.List;

@Component
@ConditionalOnProperty(name = "mes.production-context.outbox.enabled", havingValue = "true")
public class ProductionContextOutboxDispatcher {

    private final ProductionContextOutboxRepository repository;
    private final ProductionContextProjector projector;
    private final ProductionContextPublisher publisher;
    private final ProductionContextOutboxProperties properties;
    private final String instanceId;
    private final Counter sent;
    private final Counter retried;
    private final Counter dead;

    @Autowired
    public ProductionContextOutboxDispatcher(
        ProductionContextOutboxRepository repository,
        ProductionContextProjector projector,
        ProductionContextPublisher publisher,
        ProductionContextOutboxProperties properties,
        MeterRegistry meterRegistry
    ) {
        this(repository, projector, publisher, properties, meterRegistry, defaultInstanceId());
    }

    ProductionContextOutboxDispatcher(
        ProductionContextOutboxRepository repository,
        ProductionContextProjector projector,
        ProductionContextPublisher publisher,
        ProductionContextOutboxProperties properties,
        MeterRegistry meterRegistry,
        String instanceId
    ) {
        this.repository = repository;
        this.projector = projector;
        this.publisher = publisher;
        this.properties = properties;
        this.instanceId = instanceId;
        this.sent = meterRegistry.counter("mes.production.context.outbox.sent");
        this.retried = meterRegistry.counter("mes.production.context.outbox.retried");
        this.dead = meterRegistry.counter("mes.production.context.outbox.dead");
    }

    @Scheduled(fixedDelayString = "${mes.production-context.outbox.poll-delay-ms:1000}")
    public void dispatch() {
        List<ProductionContextOutboxRecord> records = repository.claim(
            instanceId,
            properties.getBatchSize(),
            properties.getClaimTimeoutMs()
        );
        for (ProductionContextOutboxRecord record : records) {
            dispatch(record);
        }
    }

    void dispatch(ProductionContextOutboxRecord record) {
        try {
            ProductionContextEventV1 event = projector.project(record);
            String topic = blank(record.getTopic()) ? properties.getTopic() : record.getTopic();
            publisher.publish(topic, scopeKey(event), event);
            repository.markSent(record.getId(), instanceId);
            sent.increment();
        } catch (Exception error) {
            if (record.getAttemptCount() >= properties.getMaxAttempts()) {
                repository.markDead(record.getId(), instanceId, error.toString());
                dead.increment();
            } else {
                repository.markRetry(
                    record.getId(),
                    instanceId,
                    retryDelay(record.getAttemptCount()),
                    error.toString()
                );
                retried.increment();
            }
        }
    }

    private long retryDelay(int attemptCount) {
        long delay = properties.getBaseRetryMs();
        int doublings = Math.max(0, Math.min(attemptCount - 1, 20));
        for (int index = 0; index < doublings && delay < properties.getMaxRetryMs(); index++) {
            if (delay > properties.getMaxRetryMs() / 2L) {
                delay = properties.getMaxRetryMs();
            } else {
                delay = Math.min(properties.getMaxRetryMs(), delay * 2L);
            }
        }
        return delay;
    }

    private static String scopeKey(ProductionContextEventV1 event) {
        return event.getTenantId() + "|" + event.getPlantId() + "|" + event.getLineId();
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String defaultInstanceId() {
        try {
            return InetAddress.getLocalHost().getHostName() + "-" + ManagementFactory.getRuntimeMXBean().getName();
        } catch (Exception ignored) {
            return "context-outbox-unknown";
        }
    }
}
