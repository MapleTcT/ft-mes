package com.mapletct.ftmes.qcsoutbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class QcsQualityGateOutboxDispatcherTest {

    private QcsQualityGateOutboxRepository repository;
    private BpiBatchResolver resolver;
    private QcsQualityGatePublisher publisher;
    private QcsQualityGateOutboxProperties properties;
    private QcsQualityGateOutboxDispatcher dispatcher;

    @Before
    public void setUp() {
        repository = mock(QcsQualityGateOutboxRepository.class);
        resolver = mock(BpiBatchResolver.class);
        publisher = mock(QcsQualityGatePublisher.class);
        properties = QcsInternalJwtIssuerTest.properties();
        properties.setBatchSize(10);
        properties.setMaxAttempts(3);
        properties.setBaseRetryMs(1000L);
        properties.setMaxRetryMs(10_000L);
        dispatcher = new QcsQualityGateOutboxDispatcher(
            repository,
            resolver,
            new QcsQualityGateProjector(new ObjectMapper()),
            publisher,
            properties,
            new SimpleMeterRegistry(),
            "test-instance"
        );
    }

    @Test
    public void resolvesPublishesAndMarksCanonicalPayloadSent() throws Exception {
        QcsQualityGateOutboxRecord record = record(1);
        ResolvedBpiBatch batch = QcsQualityGateProjectorTest.batch("CLOSED_RAW");
        when(repository.claim("test-instance", 1, properties.getClaimTimeoutMs()))
            .thenReturn(Collections.singletonList(record), Collections.<QcsQualityGateOutboxRecord>emptyList());
        when(resolver.resolve(record)).thenReturn(batch);

        dispatcher.dispatch();

        verify(publisher).publish(
            eq("qcs.batch.quality-gate.v1"),
            eq("c1584e53-2780-4f58-bb34-9c7399a54d01|qcs-inspect:4001"),
            any()
        );
        verify(repository).markSent(
            eq(42L),
            eq("test-instance"),
            eq(UUID.fromString("c1584e53-2780-4f58-bb34-9c7399a54d01")),
            anyString()
        );
        verify(repository, times(2)).claim("test-instance", 1, properties.getClaimTimeoutMs());
    }

    @Test
    public void databaseRowCannotRedirectPublicationToAnotherTopic() throws Exception {
        QcsQualityGateOutboxRecord record = QcsQualityGateProjectorTest.record(
            1, QcsQualityGateProjectorTest.acceptedInspections(), "unexpected.topic");
        when(resolver.resolve(record)).thenReturn(QcsQualityGateProjectorTest.batch("CLOSED_RAW"));

        dispatcher.dispatch(record);

        verify(publisher).publish(
            eq("qcs.batch.quality-gate.v1"),
            eq("c1584e53-2780-4f58-bb34-9c7399a54d01|qcs-inspect:4001"),
            any()
        );
        verify(publisher, never()).publish(eq("unexpected.topic"), anyString(), any());
    }

    @Test
    public void retriesWhileBpiBatchHasNotClosed() throws Exception {
        QcsQualityGateOutboxRecord record = record(2);
        when(resolver.resolve(record)).thenReturn(QcsQualityGateProjectorTest.batch("ACTIVE"));

        dispatcher.dispatch(record);

        verify(repository).markRetry(42L, "test-instance", 2000L,
            "com.mapletct.ftmes.qcsoutbox.RetryableQcsOutboxException: "
                + "BPI batch is not closed for QCS ingestion; current state=ACTIVE");
        verify(publisher, never()).publish(anyString(), anyString(), any());
    }

    @Test
    public void marksFinalBpiStateAsPermanentDeadLetter() throws Exception {
        QcsQualityGateOutboxRecord record = record(1);
        when(resolver.resolve(record)).thenReturn(QcsQualityGateProjectorTest.batch("RELEASED"));

        dispatcher.dispatch(record);

        verify(repository).markDead(eq(42L), eq("test-instance"), anyString());
        verify(publisher, never()).publish(anyString(), anyString(), any());
    }

    @Test
    public void republishesExactQualityGateReplayFromTerminalBatch() throws Exception {
        QcsQualityGateOutboxRecord record = record(2);
        ResolvedBpiBatch batch = QcsQualityGateProjectorTest.batch("RELEASED");
        batch.setCurrentQualityGateId(record.getQualityGateId());
        batch.setCurrentQualityGateRevision(record.getQualityGateRevision());
        batch.setCurrentQualityGateSourceEventId(record.getEventId());
        when(resolver.resolve(record)).thenReturn(batch);

        dispatcher.dispatch(record);

        verify(publisher).publish(
            eq("qcs.batch.quality-gate.v1"),
            eq("c1584e53-2780-4f58-bb34-9c7399a54d01|qcs-inspect:4001"),
            any()
        );
        verify(repository).markSent(
            eq(42L),
            eq("test-instance"),
            eq(UUID.fromString("c1584e53-2780-4f58-bb34-9c7399a54d01")),
            anyString()
        );
        verify(repository, never()).markDead(eq(42L), eq("test-instance"), anyString());
    }

    @Test
    public void marksKafkaFailureDeadAtAttemptLimit() throws Exception {
        QcsQualityGateOutboxRecord record = record(3);
        when(resolver.resolve(record)).thenReturn(QcsQualityGateProjectorTest.batch("WAIT_QA"));
        doThrow(new IllegalStateException("planned Kafka outage"))
            .when(publisher).publish(anyString(), anyString(), any());

        dispatcher.dispatch(record);

        verify(repository).markDead(eq(42L), eq("test-instance"), anyString());
    }

    private static QcsQualityGateOutboxRecord record(int attemptCount) {
        return QcsQualityGateProjectorTest.record(
            attemptCount, QcsQualityGateProjectorTest.acceptedInspections());
    }
}
