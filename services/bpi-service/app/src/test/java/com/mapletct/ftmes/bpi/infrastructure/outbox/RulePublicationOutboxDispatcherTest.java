package com.mapletct.ftmes.bpi.infrastructure.outbox;

import com.mapletct.ftmes.bpi.domain.OutboxEventClaim;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RulePublicationOutboxDispatcherTest {

    @Test
    void sendsStableKafkaRecordBeforeMarkingTheClaimPublished() throws Exception {
        RulePublicationOutboxRepository repository = mock(RulePublicationOutboxRepository.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<byte[], byte[]> kafkaTemplate = mock(KafkaTemplate.class);
        RulePublicationOutboxProperties properties = properties();
        OutboxEventClaim claim = claim(1);
        when(repository.claimPending(properties.batchSize(), properties.claimTimeout()))
                .thenReturn(List.of(claim));
        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(repository.markPublished(claim.id(), claim.claimToken())).thenReturn(true);

        new RulePublicationOutboxDispatcher(repository, properties, kafkaTemplate).dispatchPending();

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<ProducerRecord> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(captor.capture());
        ProducerRecord<?, ?> record = captor.getValue();
        assertThat(record.topic()).isEqualTo(properties.topic());
        assertThat(new String((byte[]) record.key(), StandardCharsets.UTF_8))
                .isEqualTo(claim.partitionKey());
        assertThat((byte[]) record.value()).containsExactly(claim.payload());
        assertThat(new String(record.headers().lastHeader("schema_version").value(), StandardCharsets.UTF_8))
                .isEqualTo("1");
        assertThat(new String(record.headers().lastHeader("outbox_event_id").value(), StandardCharsets.UTF_8))
                .isEqualTo(claim.id().toString());
        verify(repository).markPublished(claim.id(), claim.claimToken());
        verify(repository, never()).markFailed(any(), any(), any(Integer.class), any(Integer.class), any(), any());
    }

    @Test
    void leavesFailedSendForBoundedRetryInsteadOfDroppingTheEvent() {
        RulePublicationOutboxRepository repository = mock(RulePublicationOutboxRepository.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<byte[], byte[]> kafkaTemplate = mock(KafkaTemplate.class);
        RulePublicationOutboxProperties properties = properties();
        OutboxEventClaim claim = claim(2);
        when(repository.claimPending(properties.batchSize(), properties.claimTimeout()))
                .thenReturn(List.of(claim));
        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker unavailable")));

        new RulePublicationOutboxDispatcher(repository, properties, kafkaTemplate).dispatchPending();

        verify(repository, never()).markPublished(any(), any());
        verify(repository).markFailed(
                claim.id(), claim.claimToken(), claim.attemptCount(), properties.maxAttempts(),
                properties.retryBackoff(), "java.lang.IllegalStateException: broker unavailable");
    }

    private RulePublicationOutboxProperties properties() {
        return new RulePublicationOutboxProperties(
                true,
                "127.0.0.1:29092",
                "bpi.boundary.rule-publication.v1",
                "rule-outbox-test",
                50,
                20,
                Duration.ofSeconds(1),
                Duration.ofMinutes(2),
                Duration.ofSeconds(2));
    }

    private OutboxEventClaim claim(int attempt) {
        return new OutboxEventClaim(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "bpi.boundary.rule-publication.v1",
                "TENANT-A:LINE-01:RULE-START:1",
                new byte[] {1, 2, 3},
                Map.of("schema_version", "1", "event_type", "BOUNDARY_RULE_PUBLISHED"),
                attempt);
    }
}
