package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleApplicationStatusV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleApplicationV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRulePublicationV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleRuntimeReadinessStatusV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleRuntimeReadinessV1;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BpiRuleLifecycleEvidenceTest {

    private static final BpiRuleLifecycleEvidence.Config CONFIG =
            new BpiRuleLifecycleEvidence.Config(
                    "kafka:19092",
                    "bpi.boundary.rule-publication.v1",
                    "bpi.boundary.rule-application.v1",
                    "bpi.boundary.rule-runtime-readiness.v1",
                    "ADP_E2E_LIFECYCLE",
                    "1000",
                    "PLANT-01",
                    "LINE-S07-01",
                    "RULE-S07-START",
                    "2.0.0",
                    Duration.ofSeconds(30),
                    Path.of("/tmp/bpi-rule-lifecycle-evidence.json"));

    @Test
    void provesActivationRetirementApplicationAndRuntimeReadiness() {
        BoundaryRulePublicationV1 activation = publication("activate-event", true, "ACTIVATE", 1000L);
        BoundaryRulePublicationV1 retirement = publication("retire-event", false, "RETIRE", 2000L);

        BpiRuleLifecycleEvidence.Result result = BpiRuleLifecycleEvidence.verify(
                CONFIG,
                List.of(
                        located(activation, 4L, "ACTIVATE"),
                        located(retirement, 5L, "RETIRE")),
                List.of(
                        application("active-application", "activate-event", 6L),
                        application("retire-application", "retire-event", 7L)),
                List.of(
                        readiness("active-readiness", "activate-event",
                                BoundaryRuleRuntimeReadinessStatusV1.READY, 8L),
                        readiness("retire-readiness", "retire-event",
                                BoundaryRuleRuntimeReadinessStatusV1.INACTIVE, 9L)));

        assertEquals("activate-event", result.activePublication().event().getEventId());
        assertFalse(result.retirementPublication().event().getActive());
        assertEquals(BoundaryRuleRuntimeReadinessStatusV1.INACTIVE,
                result.retirementReadiness().event().getStatus());
        assertEquals(1, result.activeReadinessCount());
        assertEquals(1, result.activeReadinessDistinctEventIdCount());
    }

    @Test
    void rejectsDuplicateRetirementPublicationEvidence() {
        BoundaryRulePublicationV1 activation = publication("activate-event", true, "ACTIVATE", 1000L);
        BoundaryRulePublicationV1 retirement = publication("retire-event", false, "RETIRE", 2000L);

        IllegalStateException error = assertThrows(IllegalStateException.class, () ->
                BpiRuleLifecycleEvidence.verify(
                        CONFIG,
                        List.of(
                                located(activation, 4L, "ACTIVATE"),
                                located(retirement, 5L, "RETIRE"),
                                located(retirement, 6L, "RETIRE")),
                        List.of(),
                        List.of()));

        assertEquals("expected exactly one retirement publication, found 2", error.getMessage());
    }

    private static BoundaryRulePublicationV1 publication(
            String eventId,
            boolean active,
            String action,
            long publishedAtMs) {
        return BoundaryRulePublicationV1.newBuilder()
                .setEventId(eventId)
                .setTenantId(CONFIG.tenantId())
                .setPlantId(CONFIG.plantId())
                .setLineId(CONFIG.lineId())
                .setRuleCode(CONFIG.ruleCode())
                .setRuleVersion(CONFIG.ruleVersion())
                .setChecksum("checksum-v1")
                .setActive(active)
                .setPublishedAtMs(publishedAtMs)
                .putHeaders("lifecycle_action", action)
                .build();
    }

    private static BpiRuleLifecycleEvidence.LocatedPublication located(
            BoundaryRulePublicationV1 event,
            long offset,
            String action) {
        return new BpiRuleLifecycleEvidence.LocatedPublication(event, 1, offset, 10_000L + offset, action);
    }

    private static BpiRuleLifecycleEvidence.LocatedApplication application(
            String eventId,
            String publicationEventId,
            long offset) {
        BoundaryRuleApplicationV1 event = BoundaryRuleApplicationV1.newBuilder()
                .setEventId(eventId)
                .setPublicationEventId(publicationEventId)
                .setTenantId(CONFIG.tenantId())
                .setPlantId(CONFIG.plantId())
                .setLineId(CONFIG.lineId())
                .setRuleCode(CONFIG.ruleCode())
                .setRuleVersion(CONFIG.ruleVersion())
                .setDeploymentId("ubuntu-test-v15")
                .setStatus(BoundaryRuleApplicationStatusV1.APPLIED)
                .build();
        return new BpiRuleLifecycleEvidence.LocatedApplication(event, 1, offset, 20_000L + offset);
    }

    private static BpiRuleLifecycleEvidence.LocatedReadiness readiness(
            String eventId,
            String publicationEventId,
            BoundaryRuleRuntimeReadinessStatusV1 status,
            long offset) {
        BoundaryRuleRuntimeReadinessV1 event = BoundaryRuleRuntimeReadinessV1.newBuilder()
                .setEventId(eventId)
                .setPublicationEventId(publicationEventId)
                .setTenantId(CONFIG.tenantId())
                .setPlantId(CONFIG.plantId())
                .setLineId(CONFIG.lineId())
                .setRuleCode(CONFIG.ruleCode())
                .setRuleVersion(CONFIG.ruleVersion())
                .setDeploymentId("ubuntu-test-v15")
                .setStatus(status)
                .build();
        return new BpiRuleLifecycleEvidence.LocatedReadiness(event, 1, offset, 30_000L + offset);
    }
}
