package com.mapletct.ftmes.contextoutbox;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProductionContextOutboxDispatcherTest {

    private ProductionContextOutboxRepository repository;
    private ProductionContextPublisher publisher;
    private ProductionContextOutboxProperties properties;
    private ProductionContextOutboxDispatcher dispatcher;

    @Before
    public void setUp() {
        repository = mock(ProductionContextOutboxRepository.class);
        publisher = mock(ProductionContextPublisher.class);
        properties = new ProductionContextOutboxProperties();
        properties.setBatchSize(10);
        properties.setMaxAttempts(3);
        properties.setBaseRetryMs(1000L);
        properties.setMaxRetryMs(10_000L);
        dispatcher = new ProductionContextOutboxDispatcher(
            repository,
            new ProductionContextProjector(),
            publisher,
            properties,
            new SimpleMeterRegistry(),
            "test-instance"
        );
    }

    @Test
    public void claimsPublishesAndMarksSnapshotSent() throws Exception {
        ProductionContextOutboxRecord record = ProductionContextProjectorTest.record(true, 1);
        when(repository.claim("test-instance", 10, properties.getClaimTimeoutMs()))
            .thenReturn(Collections.singletonList(record));

        dispatcher.dispatch();

        verify(publisher).publish(
            eq("mes.production.context.v1"),
            eq("1000|PLANT-01|LINE-01"),
            any()
        );
        verify(repository).markSent(42L, "test-instance");
    }

    @Test
    public void schedulesRetryWithExponentialDelayAfterKafkaFailure() throws Exception {
        ProductionContextOutboxRecord record = ProductionContextProjectorTest.record(true, 2);
        doThrow(new IllegalStateException("planned Kafka outage"))
            .when(publisher).publish(any(), any(), any());

        dispatcher.dispatch(record);

        verify(repository).markRetry(
            eq(42L),
            eq("test-instance"),
            eq(2000L),
            any()
        );
    }

    @Test
    public void marksPoisonSnapshotDeadAtAttemptLimit() throws Exception {
        ProductionContextOutboxRecord record = ProductionContextProjectorTest.record(true, 3);
        doThrow(new IllegalArgumentException("invalid payload"))
            .when(publisher).publish(any(), any(), any());

        dispatcher.dispatch(record);

        verify(repository).markDead(eq(42L), eq("test-instance"), any());
    }
}
