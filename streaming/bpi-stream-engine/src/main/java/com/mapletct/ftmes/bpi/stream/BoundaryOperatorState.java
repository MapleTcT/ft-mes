package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.rules.BoundaryWindowState;

import java.util.Objects;

public record BoundaryOperatorState(
        BoundaryExecutionContext context,
        BoundaryRuleRef ruleRef,
        BoundaryWindowState windowState,
        long nextTimerEpochMs) {

    public static final long NO_TIMER = Long.MIN_VALUE;

    public BoundaryOperatorState {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(ruleRef, "ruleRef");
        Objects.requireNonNull(windowState, "windowState");
    }

    public BoundaryOperatorState withWindow(BoundaryWindowState nextWindow, long nextTimer) {
        return new BoundaryOperatorState(context, ruleRef, nextWindow, nextTimer);
    }
}
