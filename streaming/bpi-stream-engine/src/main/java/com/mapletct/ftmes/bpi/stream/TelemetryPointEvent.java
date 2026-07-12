package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.contract.v1.PointValue;
import com.mapletct.ftmes.bpi.contract.v1.TelemetryEnvelopeV1;

import java.time.Instant;
import java.util.Objects;

public record TelemetryPointEvent(TelemetryEnvelopeV1 envelope, int pointIndex) {

    public TelemetryPointEvent {
        Objects.requireNonNull(envelope, "envelope");
        if (pointIndex < 0 || pointIndex >= envelope.getPointsCount()) {
            throw new IllegalArgumentException("pointIndex is outside the telemetry envelope");
        }
    }

    public PointValue point() {
        return envelope.getPoints(pointIndex);
    }

    public Instant eventTime() {
        long millis = point().getSampleTimeMs() > 0
                ? point().getSampleTimeMs()
                : envelope.getEventTimeMs();
        return Instant.ofEpochMilli(millis);
    }

    public String identity() {
        return envelope.getEventId() + "|" + pointIndex;
    }

    public String scopeKey() {
        return scopeKey(envelope.getTenantId(), envelope.getPlantId(), envelope.getLineId());
    }

    public static String contextScopeKey(com.mapletct.ftmes.bpi.contract.v1.ProductionContextEventV1 context) {
        return scopeKey(context.getTenantId(), context.getPlantId(), context.getLineId());
    }

    private static String scopeKey(String tenantId, String plantId, String lineId) {
        return tenantId + "|" + plantId + "|" + lineId;
    }
}
