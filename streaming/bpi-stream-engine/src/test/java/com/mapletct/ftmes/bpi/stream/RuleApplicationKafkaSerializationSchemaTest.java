package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleApplicationStatusV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleApplicationV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRulePublicationV1;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RuleApplicationKafkaSerializationSchemaTest {

    @Test
    void appliedReceiptUsesPublicationIdentityAsCompactedKey() throws Exception {
        BoundaryRulePublicationV1 publication = BoundaryRuleRoutingBroadcastHarnessTest
                .publication("TENANT-A", "RULE-A", true, "sha:a");
        byte[] payload = RuleApplicationProjector.project(
                publication,
                "integration-a",
                BoundaryRuleApplicationStatusV1.APPLIED,
                "",
                "",
                1234L);

        ProducerRecord<byte[], byte[]> record = new RuleApplicationKafkaSerializationSchema(
                "bpi.boundary.rule-application.v1").serialize(payload, null, null);

        assertEquals("bpi.boundary.rule-application.v1", record.topic());
        assertEquals(publication.getEventId(), new String(record.key(), StandardCharsets.UTF_8));
        assertArrayEquals(payload, record.value());
        assertEquals(1234L, record.timestamp());
    }

    @Test
    void rejectedReceiptRequiresMachineReadableFailureEvidence() {
        BoundaryRuleApplicationV1 invalid = BoundaryRuleApplicationV1.newBuilder()
                .setEventId("ack-1")
                .setPublicationEventId("publication-1")
                .setTenantId("TENANT-A")
                .setPlantId("PLANT-A")
                .setLineId("LINE-A")
                .setRuleCode("RULE-A")
                .setRuleVersion("1")
                .setChecksum("sha:a")
                .setDeploymentId("integration-a")
                .setStatus(BoundaryRuleApplicationStatusV1.REJECTED)
                .setObservedAtMs(1234L)
                .build();

        assertThrows(IllegalArgumentException.class, () ->
                new RuleApplicationKafkaSerializationSchema("rule-application")
                        .serialize(invalid.toByteArray(), null, null));
    }
}
