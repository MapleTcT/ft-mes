package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.rules.BoundaryKind;
import com.mapletct.ftmes.bpi.rules.SignalObservation;

import java.util.Objects;

public record BoundaryStreamInput(
        BoundaryExecutionContext context,
        BoundaryRuleRef ruleRef,
        BoundaryKind boundaryKind,
        SignalObservation observation) {

    public BoundaryStreamInput {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(ruleRef, "ruleRef");
        Objects.requireNonNull(boundaryKind, "boundaryKind");
        Objects.requireNonNull(observation, "observation");
    }

    public String keyedLocality() {
        return String.join(
                "|",
                context.tenantId(),
                context.plantId(),
                context.lineId(),
                context.localityGroup(),
                boundaryKind.name(),
                ruleRef.key());
    }
}
