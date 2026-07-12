package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.contract.v1.ProductionContextEventV1;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ProductionContextTimeline {

    private static final int MAX_EVENTS = 10_000;
    private static final Comparator<ProductionContextEventV1> EVENT_ORDER = Comparator
            .comparingLong(ProductionContextEventV1::getEffectiveFromMs)
            .thenComparingLong(ProductionContextEventV1::getContextRevision)
            .thenComparing(ProductionContextEventV1::getEventId);

    private final List<ProductionContextEventV1> events;

    public ProductionContextTimeline() {
        this(List.of());
    }

    private ProductionContextTimeline(List<ProductionContextEventV1> events) {
        this.events = List.copyOf(events);
    }

    public ProductionContextTimeline apply(ProductionContextEventV1 incoming) {
        validate(incoming);
        for (ProductionContextEventV1 existing : events) {
            if (existing.getEventId().equals(incoming.getEventId())) {
                if (existing.equals(incoming)) {
                    return this;
                }
                throw new IllegalArgumentException("production context event_id was reused with different content");
            }
            if (sameScope(existing, incoming)
                    && existing.getContextRevision() == incoming.getContextRevision()) {
                throw new IllegalArgumentException("production context revision is not unique within its line scope");
            }
        }
        if (events.size() >= MAX_EVENTS) {
            throw new IllegalStateException("production context timeline exceeded " + MAX_EVENTS + " events");
        }
        List<ProductionContextEventV1> next = new ArrayList<>(events);
        next.add(incoming);
        next.sort(EVENT_ORDER);
        return new ProductionContextTimeline(next);
    }

    public Optional<ProductionContextEventV1> resolve(
            String tenantId,
            String plantId,
            String lineId,
            Instant eventTime) {
        Objects.requireNonNull(eventTime, "eventTime");
        long timestamp = eventTime.toEpochMilli();
        return events.stream()
                .filter(item -> item.getTenantId().equals(tenantId)
                        && item.getPlantId().equals(plantId)
                        && item.getLineId().equals(lineId))
                .filter(item -> item.getEffectiveFromMs() <= timestamp)
                .filter(item -> item.getEffectiveToMs() == 0 || timestamp < item.getEffectiveToMs())
                .max(Comparator.comparingLong(ProductionContextEventV1::getContextRevision)
                        .thenComparingLong(ProductionContextEventV1::getEffectiveFromMs)
                        .thenComparing(ProductionContextEventV1::getEventId))
                .filter(ProductionContextEventV1::getActive);
    }

    public ProductionContextTimeline pruneBefore(Instant cutoff) {
        Objects.requireNonNull(cutoff, "cutoff");
        long timestamp = cutoff.toEpochMilli();
        return new ProductionContextTimeline(events.stream()
                .filter(item -> item.getEffectiveToMs() == 0 || item.getEffectiveToMs() >= timestamp)
                .toList());
    }

    public List<ProductionContextEventV1> events() {
        return events;
    }

    private static void validate(ProductionContextEventV1 event) {
        require(event.getEventId(), "event_id");
        require(event.getTenantId(), "tenant_id");
        require(event.getPlantId(), "plant_id");
        require(event.getLineId(), "line_id");
        if (event.getEffectiveFromMs() <= 0) {
            throw new IllegalArgumentException("effective_from_ms must be positive");
        }
        if (event.getEffectiveToMs() != 0 && event.getEffectiveToMs() <= event.getEffectiveFromMs()) {
            throw new IllegalArgumentException("effective_to_ms must be zero or later than effective_from_ms");
        }
        if (event.getContextRevision() == 0) {
            throw new IllegalArgumentException("context_revision must be positive");
        }
        if (event.getActive() && event.getOrderId().isBlank() && event.getBatchId().isBlank()) {
            throw new IllegalArgumentException("active context requires an order_id or batch_id");
        }
    }

    private static boolean sameScope(
            ProductionContextEventV1 left,
            ProductionContextEventV1 right) {
        return left.getTenantId().equals(right.getTenantId())
                && left.getPlantId().equals(right.getPlantId())
                && left.getLineId().equals(right.getLineId());
    }

    private static void require(String value, String field) {
        if (value == null || value.isBlank() || value.indexOf('|') >= 0) {
            throw new IllegalArgumentException(field + " must be nonblank and cannot contain '|'");
        }
    }
}
