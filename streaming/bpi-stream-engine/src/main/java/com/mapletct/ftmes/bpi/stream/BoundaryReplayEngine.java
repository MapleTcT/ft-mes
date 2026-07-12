package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.contract.v1.BatchCandidateV1;
import com.mapletct.ftmes.bpi.rules.BoundaryRuleDefinition;
import com.mapletct.ftmes.bpi.rules.BoundaryWindowEvaluator;
import com.mapletct.ftmes.bpi.rules.BoundaryWindowResult;
import com.mapletct.ftmes.bpi.rules.BoundaryWindowState;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class BoundaryReplayEngine {

    private BoundaryReplayEngine() {
    }

    public static List<BatchCandidateV1> replay(
            BoundaryRuleDefinition rule,
            List<BoundaryEngineInput> inputs,
            Instant finalWatermark) {
        if (inputs.isEmpty()) {
            return List.of();
        }
        BoundaryExecutionContext context = inputs.get(0).context();
        List<BoundaryEngineInput> ordered = new ArrayList<>(inputs);
        ordered.sort(Comparator.comparing((BoundaryEngineInput item) -> item.observation().eventTime())
                .thenComparing(item -> item.observation().eventId()));
        Instant latestEventTime = ordered.get(ordered.size() - 1).observation().eventTime();
        if (finalWatermark.isBefore(latestEventTime)) {
            throw new IllegalArgumentException("finalWatermark cannot precede the latest event time");
        }
        BoundaryWindowState state = BoundaryWindowState.empty();
        List<BatchCandidateV1> candidates = new ArrayList<>();
        for (BoundaryEngineInput input : ordered) {
            if (!context.equals(input.context())) {
                throw new IllegalArgumentException("one replay call can cover only one execution context");
            }
            BoundaryWindowResult observed = BoundaryWindowEvaluator.onObservation(
                    rule, state, input.observation());
            state = observed.state();
            if (observed.newlyEligible()) {
                candidates.add(BoundaryCandidateProjector.project(
                        rule, context, observed, input.observation().eventTime()));
            }
            BoundaryWindowResult timed = BoundaryWindowEvaluator.advanceEventTime(
                    rule, state, input.observation().eventTime(), 0);
            state = timed.state();
            if (timed.newlyEligible()) {
                candidates.add(BoundaryCandidateProjector.project(
                        rule, context, timed, input.observation().eventTime()));
            }
        }
        BoundaryWindowResult completed = BoundaryWindowEvaluator.advanceEventTime(
                rule, state, finalWatermark, 0);
        if (completed.newlyEligible()) {
            candidates.add(BoundaryCandidateProjector.project(rule, context, completed, finalWatermark));
        }
        return List.copyOf(candidates);
    }
}
