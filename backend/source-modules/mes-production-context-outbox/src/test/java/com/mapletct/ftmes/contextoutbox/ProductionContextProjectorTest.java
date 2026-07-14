package com.mapletct.ftmes.contextoutbox;

import com.mapletct.ftmes.bpi.contract.v1.ProductionContextEventV1;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProductionContextProjectorTest {

    private final ProductionContextProjector projector = new ProductionContextProjector();

    @Test
    public void projectsReadyOutboxSnapshotWithoutLosingBusinessIdentity() {
        ProductionContextEventV1 event = projector.project(record(true, 1));

        assertEquals("wom-context:1000:PLANT-01:LINE-01:8", event.getEventId());
        assertEquals("1000", event.getTenantId());
        assertEquals("MO-001", event.getOrderId());
        assertEquals("BATCH-001", event.getBatchId());
        assertEquals("MAT-001", event.getMaterialCode());
        assertEquals("FORMULA-01:V2", event.getRecipeVersion());
        assertEquals(8L, event.getContextRevision());
        assertTrue(event.getActive());
        assertEquals("42", event.getAttributesOrThrow("outbox_id"));
        assertEquals("running", event.getAttributesOrThrow("source_state"));
    }

    @Test
    public void inactiveContextMayCloseScopeWithoutMaterialAndRecipe() {
        ProductionContextOutboxRecord inactive = new ProductionContextOutboxRecord(
            43L, "event-inactive", "mes.production.context.v1",
            1000L, 77L, "1000", "PLANT-01", "LINE-01",
            "MO-001", "99", null, null, null, "finished",
            9L, false, 1_725_000_000_100L, null, 1
        );

        ProductionContextEventV1 event = projector.project(inactive);

        assertFalse(event.getActive());
        assertEquals("", event.getMaterialCode());
        assertEquals(9L, event.getContextRevision());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsActiveSnapshotMissingRequiredRecipe() {
        ProductionContextOutboxRecord invalid = new ProductionContextOutboxRecord(
            44L, "event-invalid", "mes.production.context.v1",
            1000L, 77L, "1000", "PLANT-01", "LINE-01",
            "MO-001", "99", "MAT-001", null, "BATCH-001", "running",
            10L, true, 1_725_000_000_200L, null, 1
        );

        projector.project(invalid);
    }

    static ProductionContextOutboxRecord record(boolean active, int attemptCount) {
        return new ProductionContextOutboxRecord(
            42L, "wom-context:1000:PLANT-01:LINE-01:8", "mes.production.context.v1",
            1000L, 77L, "1000", "PLANT-01", "LINE-01",
            "MO-001", "99", "MAT-001", "FORMULA-01:V2", "BATCH-001", "running",
            8L, active, 1_725_000_000_000L, null, attemptCount
        );
    }
}
