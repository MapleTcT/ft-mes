package com.mapletct.ftmes.qcsoutbox;

import com.mapletct.ftmes.bpi.contract.v1.QcsQualityGateV1;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "qcs.bpi.outbox.enabled", havingValue = "true")
public class QcsQualityGateOutboxDispatcher {

    private final QcsQualityGateOutboxRepository repository;
    private final BpiBatchResolver batchResolver;
    private final QcsQualityGateProjector projector;
    private final QcsQualityGatePublisher publisher;
    private final QcsQualityGateOutboxProperties properties;
    private final String instanceId;
    private final Counter sent;
    private final Counter retried;
    private final Counter dead;

    @Autowired
    public QcsQualityGateOutboxDispatcher(
            QcsQualityGateOutboxRepository repository,
            BpiBatchResolver batchResolver,
            QcsQualityGateProjector projector,
            QcsQualityGatePublisher publisher,
            QcsQualityGateOutboxProperties properties,
            MeterRegistry meterRegistry) {
        this(repository, batchResolver, projector, publisher, properties, meterRegistry, defaultInstanceId());
    }

    QcsQualityGateOutboxDispatcher(
            QcsQualityGateOutboxRepository repository,
            BpiBatchResolver batchResolver,
            QcsQualityGateProjector projector,
            QcsQualityGatePublisher publisher,
            QcsQualityGateOutboxProperties properties,
            MeterRegistry meterRegistry,
            String instanceId) {
        this.repository = repository;
        this.batchResolver = batchResolver;
        this.projector = projector;
        this.publisher = publisher;
        this.properties = properties;
        this.instanceId = instanceId;
        this.sent = meterRegistry.counter("qcs.bpi.quality.gate.outbox.sent");
        this.retried = meterRegistry.counter("qcs.bpi.quality.gate.outbox.retried");
        this.dead = meterRegistry.counter("qcs.bpi.quality.gate.outbox.dead");
    }

    @Scheduled(fixedDelayString = "${qcs.bpi.outbox.poll-delay-ms:1000}")
    public void dispatch() {
        for (int index = 0; index < properties.getBatchSize(); index++) {
            List<QcsQualityGateOutboxRecord> records = repository.claim(
                instanceId, 1, properties.getClaimTimeoutMs());
            if (records.isEmpty()) return;
            dispatch(records.get(0));
        }
    }

    void dispatch(QcsQualityGateOutboxRecord record) {
        try {
            ResolvedBpiBatch batch = batchResolver.resolve(record);
            assertBatchCanAcceptQualityGate(record, batch);
            QcsQualityGateV1 event = projector.project(record, batch);
            byte[] payload = event.toByteArray();
            publisher.publish(
                properties.getTopic(), event.getBatchId() + "|" + event.getQualityGateId(), event);
            repository.markSent(record.getId(), instanceId, UUID.fromString(batch.getId()), sha256(payload));
            sent.increment();
        } catch (PermanentQcsOutboxException error) {
            repository.markDead(record.getId(), instanceId, error.toString());
            dead.increment();
        } catch (Exception error) {
            if (record.getAttemptCount() >= properties.getMaxAttempts()) {
                repository.markDead(record.getId(), instanceId, error.toString());
                dead.increment();
            } else {
                repository.markRetry(
                    record.getId(), instanceId, retryDelay(record.getAttemptCount()), error.toString());
                retried.increment();
            }
        }
    }

    private static void assertBatchCanAcceptQualityGate(
            QcsQualityGateOutboxRecord record, ResolvedBpiBatch batch) {
        String state = batch.getState() == null ? "" : batch.getState().trim().toUpperCase(Locale.ROOT);
        if ("CLOSED_RAW".equals(state) || "WAIT_QA".equals(state)) return;
        if ("PENDING".equals(state) || "ACTIVE".equals(state) || "PAUSED".equals(state)) {
            throw new RetryableQcsOutboxException(
                "BPI batch is not closed for QCS ingestion; current state=" + state);
        }
        if (isTerminalQualityState(state) && isExactQualityGateReplay(record, batch)) return;
        throw new PermanentQcsOutboxException(
            "BPI batch cannot accept a new QCS quality gate; current state=" + state);
    }

    private static boolean isTerminalQualityState(String state) {
        return "REJECTED".equals(state)
            || "DISPOSED".equals(state)
            || "REWORK".equals(state)
            || "RELEASED".equals(state)
            || "INBOUNDED".equals(state);
    }

    private static boolean isExactQualityGateReplay(
            QcsQualityGateOutboxRecord record, ResolvedBpiBatch batch) {
        return Objects.equals(record.getQualityGateId(), batch.getCurrentQualityGateId())
            && Objects.equals(
                Long.valueOf(record.getQualityGateRevision()),
                batch.getCurrentQualityGateRevision())
            && Objects.equals(record.getEventId(), batch.getCurrentQualityGateSourceEventId());
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

    private static String sha256(byte[] payload) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(payload);
            StringBuilder value = new StringBuilder(digest.length * 2);
            for (byte item : digest) value.append(String.format("%02x", item & 0xff));
            return value.toString();
        } catch (Exception error) {
            throw new PermanentQcsOutboxException("Unable to checksum the QCS payload", error);
        }
    }

    private static String defaultInstanceId() {
        try {
            return InetAddress.getLocalHost().getHostName() + "-" + ManagementFactory.getRuntimeMXBean().getName();
        } catch (Exception ignored) {
            return "qcs-outbox-unknown";
        }
    }
}
