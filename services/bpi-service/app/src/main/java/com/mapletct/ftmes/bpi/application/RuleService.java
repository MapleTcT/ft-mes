package com.mapletct.ftmes.bpi.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.application.error.BpiConflictException;
import com.mapletct.ftmes.bpi.application.error.BpiForbiddenException;
import com.mapletct.ftmes.bpi.application.error.BpiPreconditionRequiredException;
import com.mapletct.ftmes.bpi.application.error.BpiValidationException;
import com.mapletct.ftmes.bpi.domain.GoldenBoundary;
import com.mapletct.ftmes.bpi.domain.RuleSimulationView;
import com.mapletct.ftmes.bpi.domain.RuleVersionView;
import com.mapletct.ftmes.bpi.domain.TelemetryObservation;
import com.mapletct.ftmes.bpi.domain.TopologyVersionView;
import com.mapletct.ftmes.bpi.infrastructure.postgres.BpiPostgresRepository;
import com.mapletct.ftmes.bpi.infrastructure.postgres.IdempotencyRecord;
import com.mapletct.ftmes.bpi.infrastructure.postgres.RulePostgresRepository;
import com.mapletct.ftmes.bpi.infrastructure.outbox.RulePublicationOutboxProperties;
import com.mapletct.ftmes.bpi.infrastructure.outbox.RulePublicationOutboxRepository;
import com.mapletct.ftmes.bpi.interfaces.rest.RulePublishCommand;
import com.mapletct.ftmes.bpi.interfaces.rest.RuleSimulationCommand;
import com.mapletct.ftmes.bpi.rules.BoundaryRuleDefinition;
import com.mapletct.ftmes.bpi.rules.BoundaryWindowEvaluator;
import com.mapletct.ftmes.bpi.rules.BoundaryWindowResult;
import com.mapletct.ftmes.bpi.rules.BoundaryWindowState;
import com.mapletct.ftmes.bpi.rules.ConditionStatus;
import com.mapletct.ftmes.bpi.rules.SignalObservation;
import com.mapletct.ftmes.bpi.rules.SignalQuality;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RuleService {
    private static final Pattern REVISION_HEADER = Pattern.compile("^(?:W/)?\\\"?(\\d+)\\\"?$");
    private static final int MAX_REPLAY_OBSERVATIONS = 100_000;

    private final RulePostgresRepository repository;
    private final BpiPostgresRepository sharedRepository;
    private final RuleDefinitionParser definitionParser;
    private final RulePublicationFactory publicationFactory;
    private final RulePublicationOutboxRepository outboxRepository;
    private final RulePublicationOutboxProperties outboxProperties;
    private final ObjectMapper objectMapper;

    public RuleService(
            RulePostgresRepository repository,
            BpiPostgresRepository sharedRepository,
            RuleDefinitionParser definitionParser,
            RulePublicationFactory publicationFactory,
            RulePublicationOutboxRepository outboxRepository,
            RulePublicationOutboxProperties outboxProperties,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.sharedRepository = sharedRepository;
        this.definitionParser = definitionParser;
        this.publicationFactory = publicationFactory;
        this.outboxRepository = outboxRepository;
        this.outboxProperties = outboxProperties;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<TopologyVersionView> listTopologies(
            ActorContext actor, String plantId, String lineId) {
        assertRequestedScope(actor, plantId, lineId);
        return repository.listTopologies(actor, plantId, lineId);
    }

    @Transactional(readOnly = true)
    public TopologyVersionView getTopology(ActorContext actor, UUID topologyId) {
        return repository.findTopology(actor, topologyId);
    }

    @Transactional(readOnly = true)
    public List<RuleVersionView> listRules(ActorContext actor, String plantId, String lineId) {
        assertRequestedScope(actor, plantId, lineId);
        return repository.listRules(actor, plantId, lineId);
    }

    @Transactional(readOnly = true)
    public RuleVersionView getRule(ActorContext actor, UUID ruleId) {
        return repository.findRule(actor, ruleId);
    }

    @Transactional(readOnly = true)
    public RuleSimulationView getSimulation(ActorContext actor, UUID simulationId) {
        return repository.findSimulation(actor, simulationId);
    }

    @Transactional(timeout = 30)
    public CommandResult<RuleSimulationView> simulate(
            ActorContext actor,
            UUID ruleId,
            String idempotencyKey,
            String ifMatch,
            RuleSimulationCommand command,
            String traceId) {
        validateHeaders(idempotencyKey, ifMatch);
        long expectedRevision = parseRevision(ifMatch);
        RuleVersionView visible = repository.findRule(actor, ruleId);
        validateSimulationInput(actor, visible, command);
        assertRuleManagementEnabled(actor, visible);
        String path = "/bpi/v1/rules/" + ruleId + "/simulate";
        String requestChecksum = Checksums.sha256(ruleId + "|" + expectedRevision + "|" + writeJson(command));
        CommandResult<RuleSimulationView> replay = replay(
                actor, idempotencyKey, path, requestChecksum, new TypeReference<RuleSimulationView>() {});
        if (replay != null) return replay;

        RuleVersionView rule = repository.lockRule(actor, ruleId);
        if (rule.revision() != expectedRevision) {
            throw new BpiConflictException("Rule revision is stale.", rule.revision());
        }
        if (!Set.of("DRAFT", "SIMULATION_PASSED").contains(rule.state())) {
            throw new BpiConflictException("Rule cannot be simulated from state " + rule.state() + ".", rule.revision());
        }
        BoundaryRuleDefinition definition = definitionParser.parse(rule);
        Set<String> signals = new LinkedHashSet<>();
        definition.conditions().forEach(item -> signals.add(item.signal()));
        List<TelemetryObservation> observations = repository.findObservations(
                actor, rule.plantId(), rule.lineId(), command.from(), command.to(),
                command.calibrationVersion(), signals);
        if (observations.isEmpty()) {
            throw new BpiValidationException("No calibrated telemetry samples exist in the requested window.");
        }
        if (observations.size() > MAX_REPLAY_OBSERVATIONS) {
            throw new BpiValidationException(
                    "Simulation exceeds the 100000-observation safety limit; narrow the replay window.");
        }
        List<GoldenBoundary> golden = repository.findGoldenBoundaries(
                actor, rule.plantId(), rule.lineId(), command.goldenSetId(),
                definition.boundaryKind().name(), command.from(), command.to());
        if (golden.isEmpty()) {
            throw new BpiValidationException("The requested golden set has no matching boundary labels.");
        }

        List<Instant> emitted = evaluate(definition, observations, command.to());
        SimulationMetrics metrics = compare(emitted, golden);
        boolean passed = metrics.matched > 0 && metrics.missed == 0 && metrics.falsePositive == 0;
        Map<String, Object> metricMap = metrics.asMap();
        Map<String, Object> manifest = manifest(rule, command, observations.size(), golden.size());
        String simulationChecksum = Checksums.sha256(
                rule.checksum() + "|" + writeJson(manifest) + "|" + writeJson(emitted) + "|" + writeJson(metricMap));
        UUID simulationId = UUID.randomUUID();
        RuleSimulationView simulation = new RuleSimulationView(
                simulationId, rule.id(), passed ? "PASSED" : "FAILED", simulationChecksum,
                metricMap, manifest, emitted, passed ? null : "Replay metrics did not match the golden set.");
        repository.insertSimulation(actor, simulation, rule);
        repository.recordSimulationResult(
                actor.tenantId(), rule.id(), rule.revision(), simulation.id(), passed, actor.userId());
        repository.insertRuleAudit(
                actor, rule, "RULE_SIMULATED", rule.revision(), rule.revision() + 1,
                "Historical replay against " + command.goldenSetId(), traceId,
                Map.of("simulationId", simulation.id(), "simulationChecksum", simulation.checksum(),
                        "result", simulation.state(), "metrics", metricMap));
        sharedRepository.completeIdempotency(actor.tenantId(), idempotencyKey, 202, writeJson(simulation));
        return new CommandResult<>(simulation, false);
    }

    @Transactional(timeout = 15)
    public CommandResult<RuleVersionView> publish(
            ActorContext actor,
            UUID ruleId,
            String idempotencyKey,
            String ifMatch,
            RulePublishCommand command,
            String traceId) {
        validateHeaders(idempotencyKey, ifMatch);
        long expectedRevision = parseRevision(ifMatch);
        RuleVersionView visible = repository.findRule(actor, ruleId);
        assertRuleManagementEnabled(actor, visible);
        String path = "/bpi/v1/rules/" + ruleId + "/publish";
        String requestChecksum = Checksums.sha256(ruleId + "|" + expectedRevision + "|" + writeJson(command));
        CommandResult<RuleVersionView> replay = replay(
                actor, idempotencyKey, path, requestChecksum, new TypeReference<RuleVersionView>() {});
        if (replay != null) return replay;

        RuleVersionView rule = repository.lockRule(actor, ruleId);
        if (rule.revision() != expectedRevision) {
            throw new BpiConflictException("Rule revision is stale.", rule.revision());
        }
        RuleSimulationView simulation = repository.findSimulation(actor, command.simulationId());
        if (!simulation.ruleId().equals(rule.id()) || !"PASSED".equals(simulation.state())
                || !simulation.checksum().equals(command.simulationChecksum())) {
            throw new BpiValidationException("A PASSED simulation with the matching checksum is required.");
        }
        BoundaryRuleDefinition definition = definitionParser.parse(rule);
        TopologyVersionView topology = repository.findTopologyForRule(actor, rule.id());
        UUID publicationEventId = UUID.randomUUID();
        var publication = publicationFactory.create(
                actor, rule, topology, definition, publicationEventId, Instant.now(),
                outboxProperties.topic(), traceId);
        repository.publishRule(
                actor.tenantId(), rule.id(), rule.revision(), simulation.id(), actor.userId());
        outboxRepository.insertPublication(actor, rule, publication);
        repository.insertRuleAudit(
                actor, rule, "RULE_PUBLISHED", rule.revision(), rule.revision() + 1,
                command.reason(), traceId,
                Map.of("simulationId", simulation.id(), "simulationChecksum", simulation.checksum(),
                        "publicationEventId", publicationEventId));
        RuleVersionView published = repository.findRule(actor, rule.id());
        sharedRepository.completeIdempotency(actor.tenantId(), idempotencyKey, 200, writeJson(published));
        return new CommandResult<>(published, false);
    }

    private List<Instant> evaluate(
            BoundaryRuleDefinition definition,
            List<TelemetryObservation> observations,
            Instant windowEnd) {
        BoundaryWindowState state = BoundaryWindowState.empty();
        List<Instant> emitted = new ArrayList<>();
        for (TelemetryObservation observation : observations) {
            state = advanceReplayClock(definition, state, observation.eventTime(), emitted);
            SignalObservation signal = observation.numericValue() != null
                    ? SignalObservation.numeric(
                            observation.eventId(), observation.signal(), observation.numericValue(),
                            SignalQuality.valueOf(observation.quality()), observation.eventTime())
                    : SignalObservation.bool(
                            observation.eventId(), observation.signal(), Boolean.TRUE.equals(observation.booleanValue()),
                            SignalQuality.valueOf(observation.quality()), observation.eventTime());
            BoundaryWindowResult result = BoundaryWindowEvaluator.onObservation(definition, state, signal);
            state = applyReplayResult(result, observation.eventTime(), emitted);
        }
        advanceReplayClock(definition, state, windowEnd, emitted);
        return emitted.stream().sorted().toList();
    }

    private BoundaryWindowState advanceReplayClock(
            BoundaryRuleDefinition definition,
            BoundaryWindowState initialState,
            Instant target,
            List<Instant> emitted) {
        BoundaryWindowState state = initialState;
        while (true) {
            Instant nextDeadline = null;
            for (var condition : definition.conditions()) {
                var signal = state.signals().get(condition.signal());
                if (signal == null || signal.status() != ConditionStatus.PENDING || signal.trueSince() == null) {
                    continue;
                }
                Instant deadline = signal.trueSince().plus(condition.holdFor());
                if (!deadline.isAfter(target) && (nextDeadline == null || deadline.isBefore(nextDeadline))) {
                    nextDeadline = deadline;
                }
            }
            if (nextDeadline == null) break;
            BoundaryWindowResult timerResult = BoundaryWindowEvaluator.advanceEventTime(
                    definition, state, nextDeadline, 0);
            state = applyReplayResult(timerResult, nextDeadline, emitted);
        }
        BoundaryWindowResult targetResult = BoundaryWindowEvaluator.advanceEventTime(
                definition, state, target, 0);
        return applyReplayResult(targetResult, target, emitted);
    }

    private BoundaryWindowState applyReplayResult(
            BoundaryWindowResult result,
            Instant eventTime,
            List<Instant> emitted) {
        BoundaryWindowState state = result.state();
        if (result.newlyEligible()) emitted.add(eventTime);
        if (!result.decision().eligible() && state.candidateEmitted()) {
            return BoundaryWindowEvaluator.resetCandidate(state);
        }
        return state;
    }

    private SimulationMetrics compare(List<Instant> emitted, List<GoldenBoundary> golden) {
        boolean[] used = new boolean[emitted.size()];
        int matched = 0;
        long totalErrorMillis = 0;
        for (GoldenBoundary expected : golden) {
            int closest = -1;
            long closestError = Long.MAX_VALUE;
            for (int index = 0; index < emitted.size(); index++) {
                if (used[index]) continue;
                long error = Math.abs(Duration.between(expected.boundaryTime(), emitted.get(index)).toMillis());
                if (error <= expected.toleranceSeconds() * 1_000L && error < closestError) {
                    closest = index;
                    closestError = error;
                }
            }
            if (closest >= 0) {
                used[closest] = true;
                matched++;
                totalErrorMillis += closestError;
            }
        }
        int falsePositive = 0;
        for (boolean item : used) if (!item) falsePositive++;
        int missed = golden.size() - matched;
        double meanErrorSeconds = matched == 0 ? 0 : totalErrorMillis / 1000.0 / matched;
        return new SimulationMetrics(matched, missed, falsePositive, meanErrorSeconds);
    }

    private Map<String, Object> manifest(
            RuleVersionView rule,
            RuleSimulationCommand command,
            int observationCount,
            int goldenBoundaryCount) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ruleChecksum", rule.checksum());
        result.put("lineId", command.lineId());
        result.put("from", command.from());
        result.put("to", command.to());
        result.put("topologyVersion", command.topologyVersion());
        result.put("calibrationVersion", command.calibrationVersion());
        result.put("goldenSetId", command.goldenSetId());
        result.put("observationCount", observationCount);
        result.put("goldenBoundaryCount", goldenBoundaryCount);
        return Collections.unmodifiableMap(result);
    }

    private void validateSimulationInput(
            ActorContext actor, RuleVersionView rule, RuleSimulationCommand command) {
        if (!actor.canAccess(rule.plantId(), command.lineId()) || !rule.lineId().equals(command.lineId())) {
            throw new BpiForbiddenException("Token scope does not allow this rule simulation.");
        }
        if (!rule.topologyVersion().equals(command.topologyVersion())) {
            throw new BpiValidationException("Simulation topologyVersion must match the rule version.");
        }
        if (!command.from().isBefore(command.to())) {
            throw new BpiValidationException("Simulation from must be before to.");
        }
        if (Duration.between(command.from(), command.to()).compareTo(Duration.ofDays(31)) > 0) {
            throw new BpiValidationException("Simulation window must not exceed 31 days.");
        }
    }

    private void assertRuleManagementEnabled(ActorContext actor, RuleVersionView rule) {
        if (!sharedRepository.featureEnabled(
                actor, rule.plantId(), rule.lineId(), "bpi.rule-management")) {
            throw new BpiForbiddenException("BPI rule management is disabled for this scope.");
        }
    }

    private void assertRequestedScope(ActorContext actor, String plantId, String lineId) {
        if (plantId != null && lineId != null && !actor.canAccess(plantId, lineId)) {
            throw new BpiForbiddenException("Token scope does not allow the requested rule scope.");
        }
    }

    private <T> CommandResult<T> replay(
            ActorContext actor,
            String idempotencyKey,
            String path,
            String checksum,
            TypeReference<T> type) {
        boolean owner = sharedRepository.reserveIdempotency(
                UUID.randomUUID(), actor.tenantId(), idempotencyKey, "POST", path, checksum);
        if (owner) return null;
        IdempotencyRecord previous = sharedRepository.lockIdempotency(actor.tenantId(), idempotencyKey);
        if (!"POST".equals(previous.method()) || !path.equals(previous.resourcePath())
                || !checksum.equals(previous.requestChecksum())) {
            throw new BpiConflictException("Idempotency-Key was reused with a different request.", null);
        }
        if ("COMPLETED".equals(previous.state()) && previous.responseBody() != null) {
            return new CommandResult<>(sharedRepository.readJson(previous.responseBody(), type), true);
        }
        throw new BpiConflictException("The command is still processing.", null);
    }

    private void validateHeaders(String idempotencyKey, String ifMatch) {
        if (idempotencyKey == null || idempotencyKey.length() < 8 || ifMatch == null) {
            throw new BpiPreconditionRequiredException("Idempotency-Key and If-Match are required.");
        }
        if (idempotencyKey.length() > 128) {
            throw new BpiValidationException("Idempotency-Key must not exceed 128 characters.");
        }
    }

    private long parseRevision(String header) {
        Matcher matcher = REVISION_HEADER.matcher(header);
        if (!matcher.matches()) {
            throw new BpiPreconditionRequiredException("If-Match must contain a numeric entity revision.");
        }
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException exception) {
            throw new BpiPreconditionRequiredException("If-Match revision is outside the supported range.");
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize BPI rule command", exception);
        }
    }

    private static final class SimulationMetrics {
        private final int matched;
        private final int missed;
        private final int falsePositive;
        private final double meanBoundaryErrorSeconds;

        private SimulationMetrics(
                int matched, int missed, int falsePositive, double meanBoundaryErrorSeconds) {
            this.matched = matched;
            this.missed = missed;
            this.falsePositive = falsePositive;
            this.meanBoundaryErrorSeconds = meanBoundaryErrorSeconds;
        }

        private Map<String, Object> asMap() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("matched", matched);
            result.put("missed", missed);
            result.put("falsePositive", falsePositive);
            result.put("meanBoundaryErrorSeconds", meanBoundaryErrorSeconds);
            return Collections.unmodifiableMap(result);
        }
    }
}
