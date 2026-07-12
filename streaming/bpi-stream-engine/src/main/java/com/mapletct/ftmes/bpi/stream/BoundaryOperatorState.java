package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.rules.BoundaryWindowState;
import com.mapletct.ftmes.bpi.rules.SignalObservation;

import java.util.List;
import java.util.Objects;

public record BoundaryOperatorState(
        BoundaryExecutionContext context,
        BoundaryRuleRef ruleRef,
        BoundaryWindowState windowState,
        boolean observationHistoryComplete,
        List<SignalObservation> observations,
        long nextTimerEpochMs) {

    public static final long NO_TIMER = Long.MIN_VALUE;

    public BoundaryOperatorState(
            BoundaryExecutionContext context,
            BoundaryRuleRef ruleRef,
            BoundaryWindowState windowState,
            long nextTimerEpochMs) {
        this(context, ruleRef, windowState, true, List.of(), nextTimerEpochMs);
    }

    public BoundaryOperatorState(
            BoundaryExecutionContext context,
            BoundaryRuleRef ruleRef,
            BoundaryWindowState windowState,
            List<SignalObservation> observations,
            long nextTimerEpochMs) {
        this(context, ruleRef, windowState, true, observations, nextTimerEpochMs);
    }

    public BoundaryOperatorState {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(ruleRef, "ruleRef");
        Objects.requireNonNull(windowState, "windowState");
        Objects.requireNonNull(observations, "observations");
        observations = List.copyOf(observations);
    }

    public BoundaryOperatorState withWindow(BoundaryWindowState nextWindow, long nextTimer) {
        return new BoundaryOperatorState(
                context, ruleRef, nextWindow, observationHistoryComplete, observations, nextTimer);
    }

    public BoundaryOperatorState withWindowAndObservations(
            BoundaryWindowState nextWindow,
            List<SignalObservation> nextObservations,
            long nextTimer) {
        return new BoundaryOperatorState(
                context, ruleRef, nextWindow, observationHistoryComplete, nextObservations, nextTimer);
    }
}
