package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.contract.v1.DataQualityEventV1;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DataQualityKafkaSerializationSchemaTest {

    @Test
    void contextIssueProjectsTypedScopeAndKafkaEvidence() throws Exception {
        ContextJoinIssue issue = new ContextJoinIssue(
                "CONTEXT_WAIT_EXPIRED",
                "TENANT-A|PLANT-01|LINE-01",
                "TEL-1",
                "flow",
                1_720_000_000_000L,
                "production context did not arrive");
        byte[] payload = DataQualityProjector.project(issue, 1_720_000_000_500L);

        ProducerRecord<byte[], byte[]> record = new DataQualityKafkaSerializationSchema(
                "bpi.data-quality.v1").serialize(payload, null, null);
        DataQualityEventV1 event = DataQualityEventV1.parseFrom(record.value());

        assertEquals("bpi.data-quality.v1", record.topic());
        assertEquals(1_720_000_000_500L, record.timestamp());
        assertEquals("TENANT-A", event.getTenantId());
        assertEquals("PLANT-01", event.getPlantId());
        assertEquals("LINE-01", event.getLineId());
        assertEquals("CONTEXT_WAIT_EXPIRED", event.getIssueCode());
        assertEquals("DQ-120b34e6198417e17db8935c78120acb", event.getEventId());
        assertEquals(
                "TENANT-A|LINE-01|TEL-1|flow|CONTEXT_WAIT_EXPIRED",
                new String(record.key(), StandardCharsets.UTF_8));
        assertEquals("v1", new String(
                record.headers().lastHeader("schema_version").value(), StandardCharsets.UTF_8));
    }

    @Test
    void invalidDataQualityPayloadFailsBeforeKafkaPublish() {
        DataQualityKafkaSerializationSchema schema = new DataQualityKafkaSerializationSchema(
                "bpi.data-quality.v1");

        assertThrows(IllegalArgumentException.class, () -> schema.serialize(new byte[]{1}, null, null));
        assertThrows(IllegalArgumentException.class, () -> schema.serialize(
                DataQualityEventV1.getDefaultInstance().toByteArray(), null, null));
    }
}
