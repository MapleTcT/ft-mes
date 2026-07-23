package com.mapletct.ftmes.bpi.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.application.error.BpiConflictException;
import com.mapletct.ftmes.bpi.application.error.BpiForbiddenException;
import com.mapletct.ftmes.bpi.application.error.BpiPreconditionRequiredException;
import com.mapletct.ftmes.bpi.application.error.BpiValidationException;
import com.mapletct.ftmes.bpi.domain.DatasetDefinitionView;
import com.mapletct.ftmes.bpi.domain.DatasetSnapshotView;
import com.mapletct.ftmes.bpi.domain.ProcessSignalWindowDefinition;
import com.mapletct.ftmes.bpi.infrastructure.postgres.BpiPostgresRepository;
import com.mapletct.ftmes.bpi.infrastructure.postgres.DatasetPostgresRepository;
import com.mapletct.ftmes.bpi.infrastructure.postgres.IdempotencyRecord;
import com.mapletct.ftmes.bpi.interfaces.rest.DatasetDefinitionCreateCommand;
import com.mapletct.ftmes.bpi.interfaces.rest.DatasetSnapshotCommand;
import com.mapletct.ftmes.bpi.interfaces.rest.ProcessSignalWindowDefinitionCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DatasetService {
    private static final Pattern REVISION_HEADER = Pattern.compile("^(?:W/)?\\\"?(\\d+)\\\"?$");
    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 200;

    private final DatasetPostgresRepository repository;
    private final BpiPostgresRepository sharedRepository;
    private final DatasetManifestBuilder manifestBuilder;
    private final CanonicalJson canonicalJson;
    private final ObjectMapper objectMapper;

    public DatasetService(
            DatasetPostgresRepository repository,
            BpiPostgresRepository sharedRepository,
            DatasetManifestBuilder manifestBuilder,
            CanonicalJson canonicalJson,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.sharedRepository = sharedRepository;
        this.manifestBuilder = manifestBuilder;
        this.canonicalJson = canonicalJson;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<DatasetDefinitionView> list(
            ActorContext actor,
            String plantId,
            Integer requestedLimit) {
        assertPlantScope(actor, plantId);
        int limit = requestedLimit == null ? DEFAULT_LIMIT : requestedLimit;
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new BpiValidationException("limit must be between 1 and 200.");
        }
        return repository.list(actor, plantId, limit);
    }

    @Transactional(readOnly = true)
    public DatasetSnapshotView getSnapshot(ActorContext actor, UUID snapshotId) {
        return repository.findSnapshot(actor, snapshotId);
    }

    @Transactional(timeout = 15)
    public CommandResult<DatasetDefinitionView> createDefinition(
            ActorContext actor,
            String idempotencyKey,
            String ifMatch,
            DatasetDefinitionCreateCommand command,
            String traceId) {
        validateHeaders(idempotencyKey, ifMatch);
        long expectedRevision = parseRevision(ifMatch);
        if (expectedRevision != 0) {
            throw new BpiConflictException("A new dataset definition must use If-Match 0.", 0L);
        }
        List<String> lineIds = normalizeStrings(command.lineIds(), "lineIds");
        List<String> featureRefs = normalizeStrings(command.featureRefs(), "featureRefs");
        List<ProcessSignalWindowDefinition> processSignalWindows =
                normalizeProcessSignalWindows(command.processSignalWindows());
        List<String> labelRefs = normalizeStrings(command.labelRefs(), "labelRefs");
        assertScopes(actor, command.plantId(), lineIds);
        manifestBuilder.validateDefinition(featureRefs, processSignalWindows, labelRefs,
                command.predictionTimePolicy(), command.featureCutoffPolicy(), command.splitPolicy());

        String path = "/bpi/v1/datasets";
        String requestChecksum = Checksums.sha256(canonicalJson.write(command));
        CommandResult<DatasetDefinitionView> replay = replay(
                actor, idempotencyKey, path, requestChecksum,
                new TypeReference<DatasetDefinitionView>() {});
        if (replay != null) return replay;

        Map<String, Object> controlled = new LinkedHashMap<>();
        controlled.put("datasetCode", command.datasetCode());
        controlled.put("version", command.version());
        controlled.put("name", command.name());
        controlled.put("plantId", command.plantId());
        controlled.put("lineIds", lineIds);
        controlled.put("predictionTimePolicy", command.predictionTimePolicy());
        controlled.put("featureCutoffPolicy", command.featureCutoffPolicy());
        controlled.put("featureRefs", featureRefs);
        controlled.put("processSignalWindows", processSignalWindows);
        controlled.put("labelRefs", labelRefs);
        controlled.put("maxLabelDelayHours", command.maxLabelDelayHours());
        controlled.put("minimumConfidence", command.minimumConfidence());
        controlled.put("splitPolicy", command.splitPolicy());
        String definitionChecksum = Checksums.sha256(canonicalJson.write(controlled));

        UUID id = UUID.randomUUID();
        repository.insertDefinition(actor, id, command, lineIds, featureRefs,
                processSignalWindows, labelRefs, definitionChecksum);
        DatasetDefinitionView created = repository.findDefinition(actor, id);
        repository.insertAudit(actor, created.plantId(), null, "DATASET_DEFINITION", id,
                "DATASET_DEFINITION_CREATED", 0L, created.revision(), command.reason(), traceId,
                Map.of(
                        "definitionChecksum", definitionChecksum,
                        "predictionTimePolicy", created.predictionTimePolicy(),
                        "featureCutoffPolicy", created.featureCutoffPolicy(),
                        "splitPolicy", created.splitPolicy(),
                        "processSignalWindowCount", created.processSignalWindows().size(),
                        "lineIds", created.lineIds()));
        complete(actor, idempotencyKey, 200, created);
        return new CommandResult<>(created, false);
    }

    @Transactional(timeout = 20)
    public CommandResult<DatasetSnapshotView> createSnapshot(
            ActorContext actor,
            UUID datasetId,
            String idempotencyKey,
            String ifMatch,
            DatasetSnapshotCommand command,
            String traceId) {
        validateHeaders(idempotencyKey, ifMatch);
        long expectedRevision = parseRevision(ifMatch);
        DatasetDefinitionView definition = repository.findDefinition(actor, datasetId);
        if (definition.revision() != expectedRevision) {
            throw new BpiConflictException("Dataset definition revision is stale.", definition.revision());
        }
        if (command.freezeAt().isAfter(Instant.now())) {
            throw new BpiValidationException("freezeAt must not be in the future.");
        }
        List<String> lineIds = normalizeStrings(command.lineIds(), "lineIds");
        List<UUID> ruleVersionIds = normalizeUuids(command.ruleVersionIds());
        assertScopes(actor, definition.plantId(), lineIds);
        if (!definition.lineIds().containsAll(lineIds)) {
            throw new BpiValidationException(
                    "Snapshot lines must be a subset of the immutable dataset definition.");
        }
        if (!definition.predictionTimePolicy().equals(command.predictionTimePolicy())) {
            throw new BpiValidationException(
                    "Snapshot predictionTimePolicy must match the immutable dataset definition.");
        }

        String path = "/bpi/v1/datasets/" + datasetId + "/snapshots";
        String requestChecksum = Checksums.sha256(
                datasetId + "|" + expectedRevision + "|" + canonicalJson.write(command));
        CommandResult<DatasetSnapshotView> replay = replay(
                actor, idempotencyKey, path, requestChecksum,
                new TypeReference<DatasetSnapshotView>() {});
        if (replay != null) return replay;

        Set<String> eligibleLines = repository.eligibleLines(
                actor.tenantId(), datasetId, command.freezeAt(), lineIds, ruleVersionIds);
        if (!eligibleLines.equals(new LinkedHashSet<>(lineIds))) {
            Set<String> missing = new LinkedHashSet<>(lineIds);
            missing.removeAll(eligibleLines);
            throw new BpiValidationException(
                    "Every selected line requires an APPROVED shadow run with reviews at freezeAt; missing: "
                            + String.join(",", missing));
        }
        if (!ruleVersionIds.isEmpty()) {
            Set<UUID> eligibleRules = repository.eligibleRuleVersions(
                    actor.tenantId(), datasetId, command.freezeAt(), lineIds, ruleVersionIds);
            if (!eligibleRules.equals(new LinkedHashSet<>(ruleVersionIds))) {
                throw new BpiValidationException(
                        "Every requested rule version must have eligible APPROVED shadow-run reviews.");
            }
        }

        long snapshotVersion = repository.nextSnapshotVersion(actor, datasetId, expectedRevision);
        UUID snapshotId = UUID.randomUUID();
        repository.insertSnapshot(actor, snapshotId, definition, snapshotVersion,
                command, lineIds, ruleVersionIds);
        DatasetSnapshotView created = repository.findSnapshot(actor, snapshotId);
        repository.insertAudit(actor, definition.plantId(), null, "DATASET_SNAPSHOT", snapshotId,
                "DATASET_SNAPSHOT_QUEUED", 0L, created.revision(), command.reason(), traceId,
                Map.of(
                        "datasetId", datasetId,
                        "snapshotVersion", snapshotVersion,
                        "freezeAt", command.freezeAt(),
                        "lineIds", lineIds,
                        "ruleVersionIds", ruleVersionIds,
                        "excludeLowConfidence", command.effectiveExcludeLowConfidence(),
                        "deliveryBoundary", "MANIFEST_ONLY"));
        complete(actor, idempotencyKey, 202, created);
        return new CommandResult<>(created, false);
    }

    private void assertPlantScope(ActorContext actor, String plantId) {
        if (plantId == null || plantId.isBlank()) {
            throw new BpiValidationException("plantId is required for dataset access.");
        }
        if (!actor.plantIds().contains("*") && !actor.plantIds().contains(plantId)) {
            throw new BpiForbiddenException("Token scope does not allow the requested dataset plant.");
        }
    }

    private void assertScopes(ActorContext actor, String plantId, List<String> lineIds) {
        assertPlantScope(actor, plantId);
        for (String lineId : lineIds) {
            if (!actor.canAccess(plantId, lineId)) {
                throw new BpiForbiddenException(
                        "Token scope does not allow dataset line " + lineId + ".");
            }
        }
    }

    private List<String> normalizeStrings(List<String> values, String field) {
        if (values == null || values.isEmpty()) {
            throw new BpiValidationException(field + " must not be empty.");
        }
        List<String> normalized = values.stream().map(String::trim).sorted().toList();
        if (normalized.stream().anyMatch(String::isBlank)) {
            throw new BpiValidationException(field + " must not contain blank values.");
        }
        if (new LinkedHashSet<>(normalized).size() != normalized.size()) {
            throw new BpiValidationException(field + " must not contain duplicates.");
        }
        return normalized;
    }

    private List<UUID> normalizeUuids(List<UUID> values) {
        if (values == null || values.isEmpty()) return List.of();
        List<UUID> normalized = values.stream().sorted().toList();
        if (new LinkedHashSet<>(normalized).size() != normalized.size()) {
            throw new BpiValidationException("ruleVersionIds must not contain duplicates.");
        }
        return normalized;
    }

    private List<ProcessSignalWindowDefinition> normalizeProcessSignalWindows(
            List<ProcessSignalWindowDefinitionCommand> commands) {
        if (commands == null || commands.isEmpty()) return List.of();
        if (commands.size() > 20) {
            throw new BpiValidationException(
                    "processSignalWindows must not contain more than 20 definitions.");
        }
        List<ProcessSignalWindowDefinition> normalized = commands.stream()
                .map(command -> {
                    List<String> qualityCodes = command.acceptedQualityCodes() == null
                            ? List.of()
                            : command.acceptedQualityCodes().stream()
                            .map(String::trim).sorted().toList();
                    Map<String, Object> controlled = new LinkedHashMap<>();
                    controlled.put("featureRef", command.featureRef().trim());
                    controlled.put("signal", command.signal().trim());
                    controlled.put("valueType", command.valueType().trim());
                    controlled.put("metric", command.metric().trim());
                    controlled.put("startOffsetSeconds", command.startOffsetSeconds());
                    controlled.put("endOffsetSeconds", command.endOffsetSeconds());
                    controlled.put("minimumSamples", command.minimumSamples());
                    controlled.put("maximumGapSeconds", command.maximumGapSeconds());
                    controlled.put("expectedUnit", command.expectedUnit().trim());
                    controlled.put("requireCalibration", command.requireCalibration());
                    controlled.put("acceptedQualityCodes", qualityCodes);
                    return new ProcessSignalWindowDefinition(
                            command.featureRef().trim(), command.signal().trim(),
                            command.valueType().trim(), command.metric().trim(),
                            command.startOffsetSeconds(), command.endOffsetSeconds(),
                            command.minimumSamples(), command.maximumGapSeconds(),
                            command.expectedUnit().trim(), command.requireCalibration(),
                            qualityCodes, Checksums.sha256(canonicalJson.write(controlled)));
                })
                .sorted(java.util.Comparator.comparing(
                        ProcessSignalWindowDefinition::featureRef))
                .toList();
        if (new LinkedHashSet<>(normalized.stream()
                .map(ProcessSignalWindowDefinition::featureRef).toList()).size()
                != normalized.size()) {
            throw new BpiValidationException(
                    "processSignalWindows must not contain duplicate featureRef values.");
        }
        return normalized;
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

    private void complete(ActorContext actor, String idempotencyKey, int status, Object response) {
        sharedRepository.completeIdempotency(
                actor.tenantId(), idempotencyKey, status, writeJson(response));
    }

    private void validateHeaders(String idempotencyKey, String ifMatch) {
        if (idempotencyKey == null || idempotencyKey.length() < 8 || ifMatch == null) {
            throw new BpiPreconditionRequiredException("Idempotency-Key and If-Match are required.");
        }
        if (idempotencyKey.length() > 128) {
            throw new BpiValidationException("Idempotency-Key must not exceed 128 characters.");
        }
        parseRevision(ifMatch);
    }

    private long parseRevision(String ifMatch) {
        Matcher matcher = REVISION_HEADER.matcher(ifMatch == null ? "" : ifMatch.trim());
        if (!matcher.matches()) {
            throw new BpiPreconditionRequiredException(
                    "If-Match must contain a numeric entity revision.");
        }
        return Long.parseLong(matcher.group(1));
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize BPI dataset command", exception);
        }
    }
}
