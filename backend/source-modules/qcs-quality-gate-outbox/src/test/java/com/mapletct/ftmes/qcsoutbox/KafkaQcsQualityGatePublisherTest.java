package com.mapletct.ftmes.qcsoutbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.contract.v1.QcsQualityGateV1;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KafkaQcsQualityGatePublisherTest {

    @SuppressWarnings("unchecked")
    @Test
    public void emitsConsumerRequiredKeyAndExactlyOneIdentityHeader() throws Exception {
        KafkaTemplate<String, byte[]> template = mock(KafkaTemplate.class);
        ListenableFuture<SendResult<String, byte[]>> future = mock(ListenableFuture.class);
        when(template.send(any(ProducerRecord.class))).thenReturn(future);
        when(future.get(30_000L, TimeUnit.MILLISECONDS)).thenReturn(null);
        QcsQualityGateOutboxProperties properties = QcsInternalJwtIssuerTest.properties();
        properties.setSendTimeoutMs(30_000L);
        KafkaQcsQualityGatePublisher publisher = new KafkaQcsQualityGatePublisher(template, properties);
        QcsQualityGateV1 event = new QcsQualityGateProjector(new ObjectMapper()).project(
            QcsQualityGateProjectorTest.record(1, QcsQualityGateProjectorTest.acceptedInspections()),
            QcsQualityGateProjectorTest.batch("CLOSED_RAW"));

        publisher.publish(
            "qcs.batch.quality-gate.v1",
            event.getBatchId() + "|" + event.getQualityGateId(),
            event);

        ArgumentCaptor<ProducerRecord<String, byte[]>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(template).send(captor.capture());
        ProducerRecord<String, byte[]> record = captor.getValue();
        assertEquals("qcs.batch.quality-gate.v1", record.topic());
        assertEquals("c1584e53-2780-4f58-bb34-9c7399a54d01|qcs-inspect:4001", record.key());
        assertEquals(event, QcsQualityGateV1.parseFrom(record.value()));
        assertHeader(record, "event_id", event.getEventId());
        assertHeader(record, "idempotency_key", event.getIdempotencyKey());
        assertHeader(record, "tenant_id", "1000");
        assertHeader(record, "schema_version", "v1");
    }

    private static void assertHeader(ProducerRecord<String, byte[]> record, String name, String expected) {
        Header header = record.headers().lastHeader(name);
        assertEquals(expected, new String(header.value(), StandardCharsets.UTF_8));
        assertEquals(1, count(record, name));
    }

    private static int count(ProducerRecord<String, byte[]> record, String name) {
        int count = 0;
        for (Header ignored : record.headers().headers(name)) count++;
        return count;
    }
}
