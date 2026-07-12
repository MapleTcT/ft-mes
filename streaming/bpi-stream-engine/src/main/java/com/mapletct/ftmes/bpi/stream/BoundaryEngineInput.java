package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.rules.SignalObservation;

import java.util.Objects;

public record BoundaryEngineInput(
        BoundaryExecutionContext context,
        SignalObservation observation) {

    public BoundaryEngineInput {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(observation, "observation");
    }
}
