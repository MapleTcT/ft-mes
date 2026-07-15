package com.mapletct.ftmes.bpi.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.application.error.BpiConflictException;
import com.mapletct.ftmes.bpi.application.error.BpiForbiddenException;
import com.mapletct.ftmes.bpi.application.error.BpiPreconditionRequiredException;
import com.mapletct.ftmes.bpi.application.error.BpiValidationException;
import com.mapletct.ftmes.bpi.domain.GoldenBoundary;
import com.mapletct.ftmes.bpi.domain.RulePublicationView;
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
import com.mapletct.ftmes.bpi.interfaces.rest.RuleDraftCommand;
import com.mapletct.ftmes.bpi.interfaces.rest.RuleSimulationCommand;
import com.mapletct.ftmes.bpi.interfaces.rest.ReasonCommand;
import com.mapletct.ftmes.bpi.interfaces.rest.TopologyDraftCommand;
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
    private final TopologyDefinitionValidator topologyValidator;
    private final PointCatalogService pointCatalogService;
    private final CanonicalJson canonicalJson;
    private final ObjectMapper objectMapper;

    public RuleService(
            RulePostgresRepository repository,
            BpiPostgresRepository sharedRepository,
            RuleDefinitionParser definitionParser,
            RulePublicationFactory publicationFactory,
            RulePublicationOutboxRepository outboxRepository,
            RulePublicationOutboxProperties outboxProperties,
            TopologyDefinitionValidator topologyValidator,
            PointCatalogService pointCatalogService,
            CanonicalJson canonicalJson,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.sharedRepository = sharedRepository;
        this.definitionParser = definitionParser;
        this.publicationFactory = publicationFactory;
        this.outboxRepository = outboxRepository;
        this.outboxProperties = outboxProperties;
        this.topologyValidator = topologyValidator;
        this.pointCatalogService = pointCatalogService;
        this.canonicalJson = canonicalJson;
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

    @Transactional(timeout = 15)
    public CommandResult<TopologyVersionView> createTopologyDraft(
            ActorContext actor,
            String idempotencyKey,
            String ifMatch,
            TopologyDraftCommand command,
            String traceId) {
        validateHeaders(idempotencyKey, ifMatch);
        long expectedRevision = parseRevision(ifMatch);
        assertRequestedScope(actor, command.plantId(), command.lineId());
        assertRuleManagementEnabled(actor, command.plantId(), command.lineId());
        String path = "/bpi/v1/topologies/drafts";
        String requestChecksum = Checksums.sha256(canonicalJson.write(command));
        CommandResult<TopologyVersionView> replay = replay(
                actor, idempotencyKey, path, requestChecksum, new TypeReference<TopologyVersionView>() {});
        if (replay != null) return replay;

        if (command.baseVersionId() == null) {
            if (expectedRevision != 0) {
                throw new BpiConflictException("A new topology must use If-Match 0.", 0L);
            }
        } else {
            TopologyVersionView base = repository.lockTopology(actor, command.baseVersionId());
            if (base.revision() != expectedRevision) {
                throw new BpiConflictException("Base topology revision is stale.", base.revision());
            }
            if (!"PUBLISHED".equals(base.state())) {
                throw new BpiConflictException("Only a published topology can be copied.", base.revision());
            }
            if (!base.code().equals(command.code()) || !base.plantId().equals(command.plantId())
                    || !base.lineId().equals(command.lineId())) {
                throw new BpiValidationException("A copied topology must keep its code and scope.");
            }
        }

        UUID topologyId = UUID.randomUUID();
        String checksum = Checksums.sha256(canonicalJson.write(command.definition()));
        repository.insertTopologyDraft(
                actor, topologyId, command.code(), command.version(), command.plantId(),
                command.lineId(), checksum, command.definition());
        TopologyVersionView draft = repository.findTopology(actor, topologyId);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("checksum", checksum);
        detail.put("baseVersionId", command.baseVersionId());
        repository.insertTopologyAudit(
                actor, draft, "TOPOLOGY_DRAFT_CREATED", 0, 1,
                command.reason(), traceId, detail);
        sharedRepository.completeIdempotency(actor.tenantId(), idempotencyKey, 200, writeJson(draft));
        return new CommandResult<>(draft, false);
    }

    @Transactional(timeout = 15)
    public CommandResult<TopologyVersionView> validateTopology(
            ActorContext actor,
            UUID topologyId,
            String idempotencyKey,
            String ifMatch,
            ReasonCommand command,
            String traceId) {
        validateHeaders(idempotencyKey, ifMatch);
        long expectedRevision = parseRevision(ifMatch);
        TopologyVersionView visible = repository.findTopology(actor, topologyId);
        assertRuleManagementEnabled(actor, visible.plantId(), visible.lineId());
        String path = "/bpi/v1/topologies/" + topologyId + "/validate";
        String requestChecksum = Checksums.sha256(
                topologyId + "|" + expectedRevision + "|" + canonicalJson.write(command));
        CommandResult<TopologyVersionView> replay = replay(
                actor, idempotencyKey, path, requestChecksum, new TypeReference<TopologyVersionView>() {});
        if (replay != null) return replay;

        TopologyVersionView topology = repository.lockTopology(actor, topologyId);
        if (topology.revision() != expectedRevision) {
            throw new BpiConflictException("Topology revision is stale.", topology.revision());
        }
        if (!"DRAFT".equals(topology.state())) {
            throw new BpiConflictException("Published topology versions are immutable.", topology.revision());
        }
        String checksum = Checksums.sha256(canonicalJson.write(topology.definition()));
        if (!checksum.equals(topology.checksum())) {
            throw new BpiConflictException("Topology definition checksum no longer matches the draft.", topology.revision());
        }
        TopologyDefinitionValidator.ValidationResult structural = topologyValidator.validate(topology.definition());
        PointCatalogService.BindingValidationResult catalog = pointCatalogService.validateBindings(
                actor, topology.plantId(), topology.lineId(), topology.definition());
        List<com.mapletct.ftmes.bpi.domain.TopologyValidationIssue> errors = new ArrayList<>(structural.errors());
        errors.addAll(catalog.errors());
        List<com.mapletct.ftmes.bpi.domain.TopologyValidationIssue> warnings = new ArrayList<>(structural.warnings());
        warnings.addAll(catalog.warnings());
        repository.recordTopologyValidation(
                actor, topology.id(), topology.revision(), checksum,
                catalog.snapshotId(), catalog.snapshotChecksum(), errors, warnings);
        repository.insertTopologyAudit(
                actor, topology,
                errors.isEmpty() ? "TOPOLOGY_VALIDATION_PASSED" : "TOPOLOGY_VALIDATION_FAILED",
                topology.revision(), topology.revision() + 1, command.reason(), traceId,
                Map.of("errorCount", errors.size(),
                        "warningCount", warnings.size(), "checksum", checksum,
                        "pointCatalogSnapshotId", catalog.snapshotId() == null ? "" : catalog.snapshotId().toString(),
                        "pointCatalogChecksum", catalog.snapshotChecksum() == null ? "" : catalog.snapshotChecksum()));
        TopologyVersionView validated = repository.findTopology(actor, topology.id());
        sharedRepository.completeIdempotency(actor.tenantId(), idempotencyKey, 200, writeJson(validated));
        return new CommandResult<>(validated, false);
    }

    @Transactional(timeout = 15)
    public CommandResult<TopologyVersionView> publishTopology(
            ActorContext actor,
            UUID topologyId,
            String idempotencyKey,
            String ifMatch,
            ReasonCommand command,
            String traceId) {
        validateHeaders(idempotencyKey, ifMatch);
        long expectedRevision = parseRevision(ifMatch);
        TopologyVersionView visible = repository.findTopology(actor, topologyId);
        assertRuleManagementEnabled(actor, visible.plantId(), visible.lineId());
        String path = "/bpi/v1/topologies/" + topologyId + "/publish";
        String requestChecksum = Checksums.sha256(
                topologyId + "|" + expectedRevision + "|" + canonicalJson.write(command));
        CommandResult<TopologyVersionView> replay = replay(
                actor, idempotencyKey, path, requestChecksum, new TypeReference<TopologyVersionView>() {});
        if (replay != null) return replay;

        TopologyVersionView topology = repository.lockTopology(actor, topologyId);
        if (topology.revision() != expectedRevision) {
            throw new BpiConflictException("Topology revision is stale.", topology.revision());
        }
        String creator = repository.findTopologyCreator(actor.tenantId(), topology.id());
        if (actor.userId().equals(creator)) {
            throw new BpiValidationException("Topology publication requires an administrator other than the creator.");
        }
        repository.publishTopology(actor, topology.id(), topology.revision(), topology.checksum());
        repository.insertTopologyAudit(
                actor, topology, "TOPOLOGY_PUBLISHED", topology.revision(), topology.revision() + 1,
                command.reason(), traceId, Map.of("checksum", topology.checksum(), "creator", creator));
        TopologyVersionView published = repository.findTopology(actor, topology.id());
        sharedRepository.completeIdempotency(actor.tenantId(), idempotencyKey, 200, writeJson(published));
        return new CommandResult<>(published, false);
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

    @Transactional(timeout = 15)
    public CommandResult<RuleVersionView> createRuleDraft(
            ActorContext actor,
            String idempotencyKey,
            String ifMatch,
            RuleDraftCommand command,
            String traceId) {
        validateHeaders(idempotencyKey, ifMatch);
        long expectedRevision = parseRevision(ifMatch);
        TopologyVersionView topology = repository.findPublishedTopologyByRef(
                actor, command.lineId(), command.topologyVersion());
        assertRuleManagementEnabled(actor, topology.plantId(), topology.lineId());
        String path = "/bpi/v1/rules/drafts";
        String requestChecksum = Checksums.sha256(canonicalJson.write(command));
        CommandResult<RuleVersionView> replay = replay(
                actor, idempotencyKey, path, requestChecksum, new TypeReference<RuleVersionView>() {});
        if (replay != null) return replay;

        if (command.baseVersionId() == null) {
            if (expectedRevision != 0) {
                throw new BpiConflictException("A new rule must use If-Match 0.", 0L);
            }
        } else {
            RuleVersionView base = repository.lockRule(actor, command.baseVersionId());
            if (base.revision() != expectedRevision) {
                throw new BpiConflictException("Base rule revision is stale.", base.revision());
            }
            if (!"PUBLISHED".equals(base.state())) {
                throw new BpiConflictException("Only a published rule can be copied.", base.revision());
            }
            if (!base.code().equals(command.code()) || !base.lineId().equals(command.lineId())) {
                throw new BpiValidationException("A copied rule must keep its code and line scope.");
            }
        }

        BoundaryRuleDefinition definition = definitionParser.parse(
                command.code(), command.version(), command.ast());
        assertRuleSignalsBound(definition, topology);
        UUID ruleId = UUID.randomUUID();
        String checksum = Checksums.sha256(canonicalJson.write(command.ast()));
        repository.insertRuleDraft(
                actor, ruleId, command.code(), command.version(), topology, checksum, command.ast());
        RuleVersionView draft = repository.findRule(actor, ruleId);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("checksum", checksum);
        detail.put("baseVersionId", command.baseVersionId());
        detail.put("topologyVersion", command.topologyVersion());
        repository.insertRuleAudit(
                actor, draft, "RULE_DRAFT_CREATED", 0, 1, command.reason(), traceId, detail);
        sharedRepository.completeIdempotency(actor.tenantId(), idempotencyKey, 200, writeJson(draft));
        return new CommandResult<>(draft, false);
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
        PointCatalogService.BindingValidationResult catalog = pointCatalogService.validateBindings(
                actor, topology.plantId(), topology.lineId(), topology.definition());
        if (!catalog.errors().isEmpty()) {
            String codes = catalog.errors().stream()
                    .map(com.mapletct.ftmes.bpi.domain.TopologyValidationIssue::code)
                    .distinct()
                    .sorted()
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("UNKNOWN");
            throw new BpiValidationException(
                    "Rule publication requires current READY point catalog bindings: " + codes + ".");
        }
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
                        "publicationEventId", publicationEventId,
                        "pointCatalogSnapshotId", catalog.snapshotId().toString(),
                        "pointCatalogChecksum", catalog.snapshotChecksum()));
        RuleVersionView published = repository.findRule(actor, rule.id());
        sharedRepository.completeIdempotency(actor.tenantId(), idempotencyKey, 200, writeJson(published));
        return new CommandResult<>(published, false);
    }

    @Transactional(timeout = 15)
    public CommandResult<RuleVersionView> retryPublication(
            ActorContext actor,
            UUID ruleId,
            String idempotencyKey,
            String ifMatch,
            ReasonCommand command,
            String traceId) {
        validateHeaders(idempotencyKey, ifMatch);
        long expectedRevision = parseRevision(ifMatch);
        RuleVersionView visible = repository.findRule(actor, ruleId);
        assertRuleManagementEnabled(actor, visible);
        String path = "/bpi/v1/rules/" + ruleId + "/publication/retry";
        String requestChecksum = Checksums.sha256(ruleId + "|" + expectedRevision + "|" + writeJson(command));
        CommandResult<RuleVersionView> replay = replay(
                actor, idempotencyKey, path, requestChecksum, new TypeReference<RuleVersionView>() {});
        if (replay != null) return replay;

        RuleVersionView rule = repository.lockRule(actor, ruleId);
        RulePublicationView before = outboxRepository.lockPublication(actor, ruleId);
        if (before.revision() != expectedRevision) {
            throw new BpiConflictException("Rule publication revision is stale.", before.revision());
        }
        if (!"FAILED".equals(before.status())) {
            throw new BpiConflictException(
                    "Only a FAILED rule publication can be retried.", before.revision());
        }
        RulePublicationView after = outboxRepository.requeueFailed(actor, ruleId, before.revision());
        outboxRepository.insertPublicationAudit(
                actor, rule, before, after, command.reason(), traceId);
        RuleVersionView requeued = repository.findRule(actor, ruleId);
        sharedRepository.completeIdempotency(actor.tenantId(), idempotencyKey, 200, writeJson(requeued));
        return new CommandResult<>(requeued, false);
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
        assertRuleManagementEnabled(actor, rule.plantId(), rule.lineId());
    }

    private void assertRuleManagementEnabled(ActorContext actor, String plantId, String lineId) {
        if (!sharedRepository.featureEnabled(
                actor, plantId, lineId, "bpi.rule-management")) {
            throw new BpiForbiddenException("BPI rule management is disabled for this scope.");
        }
    }

    private void assertRuleSignalsBound(
            BoundaryRuleDefinition definition, TopologyVersionView topology) {
        Object rawBindings = topology.definition().get("bindings");
        if (!(rawBindings instanceof List<?> bindings)) {
            throw new BpiValidationException("Published topology has no point bindings.");
        }
        Set<String> boundSignals = new LinkedHashSet<>();
        for (Object rawBinding : bindings) {
            if (rawBinding instanceof Map<?, ?> binding) {
                Object signal = binding.get("signal");
                if (signal instanceof String value && !value.isBlank()) boundSignals.add(value);
            }
        }
        List<String> missing = definition.conditions().stream()
                .map(item -> item.signal())
                .filter(signal -> !boundSignals.contains(signal))
                .distinct()
                .sorted()
                .toList();
        if (!missing.isEmpty()) {
            throw new BpiValidationException(
                    "Rule conditions reference signals not bound by the topology: " + String.join(", ", missing));
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
