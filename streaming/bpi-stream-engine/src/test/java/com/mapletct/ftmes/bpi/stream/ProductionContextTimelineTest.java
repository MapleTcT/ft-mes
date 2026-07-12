package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.contract.v1.ProductionContextEventV1;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionContextTimelineTest {

    private static final Instant T0 = Instant.parse("2026-07-12T08:00:00Z");

    @Test
    void resolveUsesEventTimeWithoutLeakingAFutureRevision() {
        ProductionContextEventV1 first = context("CTX-1", 1, "MO-1", true, T0, T0.plusSeconds(100));
        ProductionContextEventV1 future = context("CTX-2", 2, "MO-2", true, T0.plusSeconds(100), null);
        ProductionContextTimeline timeline = new ProductionContextTimeline().apply(future).apply(first);

        assertEquals("MO-1", timeline.resolve(
                "TENANT-A", "PLANT-01", "LINE-01", T0.plusSeconds(50)).orElseThrow().getOrderId());
        assertEquals("MO-2", timeline.resolve(
                "TENANT-A", "PLANT-01", "LINE-01", T0.plusSeconds(150)).orElseThrow().getOrderId());
    }

    @Test
    void inactiveHigherRevisionClosesTheTimelineFromItsEffectiveTime() {
        ProductionContextTimeline timeline = new ProductionContextTimeline()
                .apply(context("CTX-1", 1, "MO-1", true, T0, null))
                .apply(context("CTX-2", 2, "MO-1", false, T0.plusSeconds(100), null));

        assertTrue(timeline.resolve(
                "TENANT-A", "PLANT-01", "LINE-01", T0.plusSeconds(99)).isPresent());
        assertTrue(timeline.resolve(
                "TENANT-A", "PLANT-01", "LINE-01", T0.plusSeconds(100)).isEmpty());
    }

    @Test
    void identicalReplayIsIdempotentAndRevisionConflictFailsClosed() {
        ProductionContextEventV1 first = context("CTX-1", 1, "MO-1", true, T0, null);
        ProductionContextTimeline timeline = new ProductionContextTimeline().apply(first);

        assertSame(timeline, timeline.apply(first));
        assertThrows(IllegalArgumentException.class, () -> timeline.apply(
                context("CTX-OTHER", 1, "MO-2", true, T0.plusSeconds(1), null)));
        assertThrows(IllegalArgumentException.class, () -> timeline.apply(
                first.toBuilder().setOrderId("MO-CHANGED").build()));
    }

    @Test
    void pruningRemovesOnlyClosedHistoryBeforeTheCutoff() {
        ProductionContextTimeline timeline = new ProductionContextTimeline()
                .apply(context("CTX-1", 1, "MO-1", true, T0, T0.plusSeconds(10)))
                .apply(context("CTX-2", 2, "MO-2", true, T0.plusSeconds(10), null));

        ProductionContextTimeline pruned = timeline.pruneBefore(T0.plusSeconds(11));

        assertEquals(1, pruned.events().size());
        assertEquals("CTX-2", pruned.events().get(0).getEventId());
    }

    private static ProductionContextEventV1 context(
            String eventId,
            long revision,
            String orderId,
            boolean active,
            Instant from,
            Instant to) {
        ProductionContextEventV1.Builder builder = ProductionContextEventV1.newBuilder()
                .setEventId(eventId)
                .setTenantId("TENANT-A")
                .setPlantId("PLANT-01")
                .setLineId("LINE-01")
                .setOrderId(orderId)
                .setEffectiveFromMs(from.toEpochMilli())
                .setContextRevision(revision)
                .setActive(active);
        if (to != null) {
            builder.setEffectiveToMs(to.toEpochMilli());
        }
        return builder.build();
    }
}
