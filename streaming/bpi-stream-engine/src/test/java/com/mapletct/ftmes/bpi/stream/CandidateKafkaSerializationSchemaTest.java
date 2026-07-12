package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.contract.identity.CandidateKeyFactory;
import com.mapletct.ftmes.bpi.contract.v1.BatchCandidateV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryType;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CandidateKafkaSerializationSchemaTest {

    private static final long BOUNDARY_TIME_MS = 1_720_000_100_000L;

    @Test
    void serializesValidatedCandidateWithStableKafkaIdentityAndHeaders() {
        BatchCandidateV1 candidate = validCandidate();
        byte[] payload = candidate.toByteArray();

        ProducerRecord<byte[], byte[]> record = new CandidateKafkaSerializationSchema("candidates.v1")
                .serialize(payload, null, 123L);

        assertEquals("candidates.v1", record.topic());
        assertNull(record.partition());
        assertEquals(BOUNDARY_TIME_MS, record.timestamp());
        assertArrayEquals("LINE-01|SUGAR-BATCH-END".getBytes(StandardCharsets.UTF_8), record.key());
        assertArrayEquals(payload, record.value());
        assertEquals(4, record.headers().toArray().length);
        assertHeader(record, "event_id", "CANDIDATE-EVENT-END");
        assertHeader(record, "candidate_key", candidate.getCandidateKey());
        assertHeader(record, "tenant_id", "TENANT-A");
        assertHeader(record, "schema_version", "v1");
    }

    @Test
    void rejectsMissingTopicMalformedProtobufAndContractViolation() {
        assertThrows(IllegalArgumentException.class, () -> new CandidateKafkaSerializationSchema(null));
        assertThrows(IllegalArgumentException.class, () -> new CandidateKafkaSerializationSchema("  "));

        CandidateKafkaSerializationSchema schema = new CandidateKafkaSerializationSchema("candidates.v1");
        assertThrows(IllegalArgumentException.class, () -> schema.serialize(new byte[]{0x0A}, null, null));
        assertThrows(IllegalArgumentException.class, () -> schema.serialize(
                validCandidate().toBuilder().clearCandidateKey().build().toByteArray(), null, null));
    }

    private static void assertHeader(
            ProducerRecord<byte[], byte[]> record,
            String name,
            String expected) {
        Header header = record.headers().lastHeader(name);
        assertEquals(expected, new String(header.value(), StandardCharsets.UTF_8));
    }

    private static BatchCandidateV1 validCandidate() {
        String firstEvidence = "EVENT-END-8";
        String ruleVersion = "RULE-1.0.0";
        return BatchCandidateV1.newBuilder()
                .setEventId("CANDIDATE-EVENT-END")
                .setCandidateKey(CandidateKeyFactory.endKey("BATCH-99", ruleVersion, firstEvidence))
                .setTenantId("TENANT-A")
                .setPlantId("PLANT-01")
                .setLineId("LINE-01")
                .setBoundaryType(BoundaryType.END)
                .setRuleCode("SUGAR-BATCH-END")
                .setRuleVersion(ruleVersion)
                .setTopologyVersion("TOPOLOGY-1")
                .setBatchId("BATCH-99")
                .setFirstQuorumEvidenceEventId(firstEvidence)
                .setBoundaryEventTimeMs(BOUNDARY_TIME_MS)
                .setConfidence(0.9d)
                .addEvidenceEventIds(firstEvidence)
                .setEmittedAtMs(BOUNDARY_TIME_MS + 500)
                .build();
    }
}
