package com.mapletct.ftmes.bpi.contract.identity;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class CandidateKeyFactoryTest {

    @Test
    public void startKeyMatchesThePublishedUuidV5GoldenValue() {
        String key = CandidateKeyFactory.startKey(
            "TENANT_A",
            "LINE_01",
            "RULE_1.0.0",
            "ORDER_42",
            "EVENT_START_7"
        );

        assertEquals("407bb849-985a-5d6f-8cd2-65557e1ff3b9", key);
        assertEquals(key, CandidateKeyFactory.startKey(
            "TENANT_A", "LINE_01", "RULE_1.0.0", "ORDER_42", "EVENT_START_7"
        ));
    }

    @Test
    public void endKeyMatchesThePublishedUuidV5GoldenValue() {
        assertEquals(
            "1e7751f6-0620-50ee-9689-28d2c01f495d",
            CandidateKeyFactory.endKey("BATCH_99", "RULE_1.0.0", "EVENT_END_8")
        );
    }

    @Test
    public void aChangedEvidenceEventProducesANewIdentity() {
        String first = CandidateKeyFactory.startKey(
            "TENANT_A", "LINE_01", "RULE_1.0.0", "ORDER_42", "EVENT_START_7"
        );
        String second = CandidateKeyFactory.startKey(
            "TENANT_A", "LINE_01", "RULE_1.0.0", "ORDER_42", "EVENT_START_8"
        );

        assertNotEquals(first, second);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ambiguousSegmentsAreRejected() {
        CandidateKeyFactory.endKey("BATCH|99", "RULE_1.0.0", "EVENT_END_8");
    }
}
