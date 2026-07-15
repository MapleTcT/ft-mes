package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.contract.v1.BoundaryRulePublicationV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleRuntimeReadinessStatusV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleRuntimeReadinessV1;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RuleRuntimeReadinessKafkaSerializationSchemaTest {

    @Test
    void readyReceiptUsesPublicationIdentityAsCompactedKey() throws Exception {
        BoundaryRulePublicationV1 publication = BoundaryRuleRoutingBroadcastHarnessTest
                .publication("TENANT-A", "RULE-A", true, "sha:a");
        byte[] payload = RuleRuntimeReadinessProjector.project(
                publication,
                "integration-a",
                BoundaryRuleRuntimeReadinessStatusV1.READY,
                "ignored",
                "ignored",
                1234L,
                null);

        ProducerRecord<byte[], byte[]> record = new RuleRuntimeReadinessKafkaSerializationSchema(
                "bpi.boundary.rule-runtime-readiness.v1").serialize(payload, null, null);

        BoundaryRuleRuntimeReadinessV1 event = BoundaryRuleRuntimeReadinessV1.parseFrom(payload);
        assertEquals("bpi.boundary.rule-runtime-readiness.v1", record.topic());
        assertEquals(publication.getEventId(), new String(record.key(), StandardCharsets.UTF_8));
        assertArrayEquals(payload, record.value());
        assertEquals(1234L, record.timestamp());
        assertEquals("", event.getReasonCode());
        assertEquals("", event.getDetail());
        assertEquals(71, event.getEventId().length());
    }

    @Test
    void degradedReceiptRequiresMachineReadableFailureEvidence() {
        BoundaryRuleRuntimeReadinessV1 invalid = BoundaryRuleRuntimeReadinessV1.newBuilder()
                .setEventId("ready-1")
                .setPublicationEventId("publication-1")
                .setTenantId("TENANT-A")
                .setPlantId("PLANT-A")
                .setLineId("LINE-A")
                .setRuleCode("RULE-A")
                .setRuleVersion("1")
                .setChecksum("sha:a")
                .setDeploymentId("integration-a")
                .setStatus(BoundaryRuleRuntimeReadinessStatusV1.DEGRADED)
                .setObservedAtMs(1234L)
                .build();

        assertThrows(IllegalArgumentException.class, () ->
                new RuleRuntimeReadinessKafkaSerializationSchema("runtime-readiness")
                        .serialize(invalid.toByteArray(), null, null));
    }
}
