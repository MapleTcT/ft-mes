package com.mapletct.ftmes.bpi.rules;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BoundaryWindowEvaluator {

    private BoundaryWindowEvaluator() {
    }

    public static BoundaryWindowResult onObservation(
            BoundaryRuleDefinition rule,
            BoundaryWindowState state,
            SignalObservation observation) {
        EvidenceCondition condition = condition(rule, observation.signal());
        if (condition == null) {
            return evaluate(rule, state, false, 0);
        }
        EvidenceSignalState previous = state.signals().get(observation.signal());
        if (previous != null && observation.eventTime().isBefore(previous.lastEventTime())) {
            return evaluate(rule, state, true, 0);
        }

        boolean predicate = observation.quality().factor() > 0 && predicate(condition, observation, previous);
        Instant trueSince = predicate
                ? previous != null && isContinuouslyTrue(previous.status()) ? previous.trueSince() : observation.eventTime()
                : null;
        String firstTrueEventId = predicate
                ? previous != null && isContinuouslyTrue(previous.status())
                    ? previous.firstTrueEventId() : observation.eventId()
                : null;
        ConditionStatus status = predicate
                ? matured(condition, trueSince, observation.eventTime()) ? ConditionStatus.TRUE : ConditionStatus.PENDING
                : observation.quality().factor() == 0 ? ConditionStatus.UNKNOWN : ConditionStatus.FALSE;
        EvidenceSignalState updated = new EvidenceSignalState(
                observation.signal(), status, trueSince, firstTrueEventId, observation.eventId(),
                observation.eventTime(), previous == null ? null : previous.currentNumericValue(),
                observation.numericValue(), observation.booleanValue(), observation.quality());
        Map<String, EvidenceSignalState> signals = new HashMap<>(state.signals());
        signals.put(observation.signal(), updated);
        return evaluate(rule, new BoundaryWindowState(
                signals, state.candidateEmitted(), state.firstQuorumEvidenceEventId()), false, 0);
    }

    public static BoundaryWindowResult advanceEventTime(
            BoundaryRuleDefinition rule,
            BoundaryWindowState state,
            Instant eventTime,
            double penaltyFactor) {
        Map<String, EvidenceSignalState> signals = new HashMap<>(state.signals());
        for (EvidenceCondition condition : rule.conditions()) {
            EvidenceSignalState current = signals.get(condition.signal());
            if (current == null || eventTime.isBefore(current.lastEventTime())) continue;
            ConditionStatus status = current.status();
            if (eventTime.isAfter(current.lastEventTime().plus(condition.maxSilence()))) {
                status = ConditionStatus.UNKNOWN;
            } else if (current.trueSince() != null && status == ConditionStatus.PENDING
                    && matured(condition, current.trueSince(), eventTime)) {
                status = ConditionStatus.TRUE;
            }
            if (status != current.status()) {
                signals.put(condition.signal(), new EvidenceSignalState(
                        current.signal(), status, current.trueSince(), current.firstTrueEventId(),
                        current.lastEventId(), current.lastEventTime(), current.previousNumericValue(),
                        current.currentNumericValue(), current.currentBooleanValue(), current.quality()));
            }
        }
        return evaluate(rule, new BoundaryWindowState(
                signals, state.candidateEmitted(), state.firstQuorumEvidenceEventId()), false, penaltyFactor);
    }

    public static BoundaryWindowState resetCandidate(BoundaryWindowState state) {
        return new BoundaryWindowState(state.signals(), false, null);
    }

    private static BoundaryWindowResult evaluate(
            BoundaryRuleDefinition rule, BoundaryWindowState state, boolean ignoredObservation, double penaltyFactor) {
        List<ScoredRuleEvidence> scored = new ArrayList<>();
        List<BoundaryEvidenceSnapshot> snapshots = new ArrayList<>();
        for (EvidenceCondition condition : rule.conditions()) {
            EvidenceSignalState signal = state.signals().get(condition.signal());
            boolean satisfied = signal != null && signal.status() == ConditionStatus.TRUE;
            scored.add(new ScoredRuleEvidence(
                    condition.signal(), condition.classification(), satisfied, satisfied ? 1 : 0,
                    signal == null ? 0 : signal.quality().factor(), condition.weight()));
            snapshots.add(snapshot(condition, signal));
        }
        BoundaryDecision rawDecision = BoundaryRuleRuntime.evaluateScored(
                scored, rule.quorumMinimum(), penaltyFactor, rule.maxCompositePenalty());
        boolean eligible = rawDecision.eligible() && rawDecision.confidence() >= rule.minimumConfidence();
        List<String> blockers = new ArrayList<>(rawDecision.blockers());
        if (rawDecision.eligible() && !eligible) blockers.add("confidence:" + rawDecision.confidence());
        BoundaryDecision decision = new BoundaryDecision(
                eligible, rawDecision.confidence(), rawDecision.quorumSatisfied(), List.copyOf(blockers));
        boolean newlyEligible = eligible && !state.candidateEmitted();
        String firstQuorumEventId = state.firstQuorumEvidenceEventId();
        if (newlyEligible) firstQuorumEventId = firstQuorumEvidenceEvent(rule, state);
        BoundaryWindowState updated = new BoundaryWindowState(
                state.signals(), state.candidateEmitted() || newlyEligible, firstQuorumEventId);
        return new BoundaryWindowResult(
                updated, decision, newlyEligible, ignoredObservation, firstQuorumEventId, snapshots);
    }

    private static String firstQuorumEvidenceEvent(BoundaryRuleDefinition rule, BoundaryWindowState state) {
        return rule.conditions().stream()
                .filter(item -> item.classification() == EvidenceClass.QUORUM)
                .map(item -> new QuorumCompletion(
                        state.signals().get(item.signal()), item.holdFor().toMillis()))
                .filter(item -> item.state() != null && item.state().status() == ConditionStatus.TRUE)
                .max(Comparator.comparing(QuorumCompletion::completedAt)
                        .thenComparing(item -> item.state().lastEventId()))
                .map(item -> item.state().lastEventId())
                .orElseThrow(() -> new IllegalStateException("eligible decision has no quorum evidence event"));
    }

    private static BoundaryEvidenceSnapshot snapshot(EvidenceCondition condition, EvidenceSignalState state) {
        if (state == null) {
            return new BoundaryEvidenceSnapshot(
                    condition.signal(), condition.classification(), ConditionStatus.UNKNOWN,
                    null, null, null, null);
        }
        String value = state.currentNumericValue() != null
                ? state.currentNumericValue().toPlainString() : String.valueOf(state.currentBooleanValue());
        return new BoundaryEvidenceSnapshot(
                condition.signal(), condition.classification(), state.status(), state.lastEventId(),
                state.lastEventTime(), state.quality(), value);
    }

    private static boolean predicate(
            EvidenceCondition condition, SignalObservation observation, EvidenceSignalState previous) {
        return switch (condition.operator()) {
            case GREATER_THAN -> numeric(observation).compareTo(condition.threshold()) > 0;
            case LESS_THAN -> numeric(observation).compareTo(condition.threshold()) < 0;
            case EQUALS_TRUE -> Boolean.TRUE.equals(observation.booleanValue());
            case EQUALS_FALSE -> Boolean.FALSE.equals(observation.booleanValue());
            case RISING -> previous != null && previous.currentNumericValue() != null
                    && numeric(observation).subtract(previous.currentNumericValue()).compareTo(condition.threshold()) >= 0;
        };
    }

    private static BigDecimal numeric(SignalObservation observation) {
        if (observation.numericValue() == null) {
            throw new IllegalArgumentException("numeric condition received a boolean observation: " + observation.signal());
        }
        return observation.numericValue();
    }

    private static boolean matured(EvidenceCondition condition, Instant trueSince, Instant now) {
        return !now.isBefore(trueSince.plus(condition.holdFor()));
    }

    private static boolean isContinuouslyTrue(ConditionStatus status) {
        return status == ConditionStatus.PENDING || status == ConditionStatus.TRUE;
    }

    private static EvidenceCondition condition(BoundaryRuleDefinition rule, String signal) {
        for (EvidenceCondition condition : rule.conditions()) {
            if (condition.signal().equals(signal)) return condition;
        }
        return null;
    }

    private record QuorumCompletion(EvidenceSignalState state, long holdMillis) {
        Instant completedAt() {
            return state.trueSince().plusMillis(holdMillis);
        }
    }
}
