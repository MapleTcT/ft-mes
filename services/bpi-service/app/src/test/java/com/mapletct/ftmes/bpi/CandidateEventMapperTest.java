package com.mapletct.ftmes.bpi;

import com.mapletct.ftmes.bpi.application.ActorContext;
import com.mapletct.ftmes.bpi.application.CandidateEventMapper;
import com.mapletct.ftmes.bpi.application.error.BpiForbiddenException;
import com.mapletct.ftmes.bpi.application.error.BpiValidationException;
import com.mapletct.ftmes.bpi.contract.identity.CandidateKeyFactory;
import com.mapletct.ftmes.bpi.contract.v1.BatchCandidateV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryType;
import com.mapletct.ftmes.bpi.contract.v1.CandidateEvidenceV1;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CandidateEventMapperTest {

    private final CandidateEventMapper mapper = new CandidateEventMapper();
    private final ActorContext actor = new ActorContext(
            "TENANT-A", "user-1", Set.of("BPI_EVENT_INGEST"), Set.of("PLANT-01"), Set.of("LINE-01"));

    @Test
    void mapsRichProtobufEvidenceIntoPersistenceRequest() {
        var request = mapper.toRequest(actor, event("TENANT-A"));

        assertThat(request.candidateKey().toString()).isEqualTo(event("TENANT-A").getCandidateKey());
        assertThat(request.evidence()).hasSize(1);
        assertThat(request.evidence().get(0).signal()).isEqualTo("feed.flow");
        assertThat(request.evidence().get(0).unit()).isEqualTo("t/h");
        assertThat(request.normalizedMissingSignals()).containsExactly("column.level");
    }

    @Test
    void rejectsCrossTenantAndEvidenceFreeEvents() {
        assertThatThrownBy(() -> mapper.toRequest(actor, event("OTHER")))
                .isInstanceOf(BpiForbiddenException.class);

        assertThatThrownBy(() -> mapper.toRequest(actor, event("TENANT-A").toBuilder().clearEvidence().build()))
                .isInstanceOf(BpiValidationException.class)
                .hasMessageContaining("detailed evidence");
    }

    private static BatchCandidateV1 event(String tenant) {
        String evidenceEvent = "EVT-FLOW-1";
        String key = CandidateKeyFactory.startKey(tenant, "LINE-01", "1.2.0", "MO-1", evidenceEvent);
        return BatchCandidateV1.newBuilder()
                .setEventId("CANDIDATE-" + key)
                .setCandidateKey(key)
                .setTenantId(tenant)
                .setPlantId("PLANT-01")
                .setLineId("LINE-01")
                .setBoundaryType(BoundaryType.START)
                .setRuleCode("RULE-START")
                .setRuleVersion("1.2.0")
                .setTopologyVersion("3")
                .setContextOrderId("MO-1")
                .setFirstQuorumEvidenceEventId(evidenceEvent)
                .setBoundaryEventTimeMs(1_783_843_180_000L)
                .setConfidence(0.94)
                .addEvidenceEventIds(evidenceEvent)
                .setEmittedAtMs(1_783_843_180_000L)
                .putHeaders("topology_code", "TOPO-S07")
                .addEvidence(CandidateEvidenceV1.newBuilder()
                        .setEventId(evidenceEvent)
                        .setSignal("feed.flow")
                        .setClassification("QUORUM")
                        .setSatisfied(true)
                        .setValue("18.6")
                        .setUnit("t/h")
                        .setQualityCode("GOOD")
                        .setEventTimeMs(1_783_843_180_000L)
                        .setSource("bpi-stream-engine"))
                .addMissingSignals("column.level")
                .build();
    }
}
