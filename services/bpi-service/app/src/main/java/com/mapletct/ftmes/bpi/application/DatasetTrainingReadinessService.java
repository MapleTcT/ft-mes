package com.mapletct.ftmes.bpi.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.application.error.BpiConflictException;
import com.mapletct.ftmes.bpi.application.error.BpiPreconditionRequiredException;
import com.mapletct.ftmes.bpi.application.error.BpiValidationException;
import com.mapletct.ftmes.bpi.domain.DatasetMlflowRegistrationView;
import com.mapletct.ftmes.bpi.domain.DatasetTrainingReadinessBuild;
import com.mapletct.ftmes.bpi.domain.DatasetTrainingReadinessEvidence;
import com.mapletct.ftmes.bpi.domain.DatasetTrainingReadinessView;
import com.mapletct.ftmes.bpi.infrastructure.postgres.BpiPostgresRepository;
import com.mapletct.ftmes.bpi.infrastructure.postgres.DatasetMlflowRegistrationPostgresRepository;
import com.mapletct.ftmes.bpi.infrastructure.postgres.DatasetPostgresRepository;
import com.mapletct.ftmes.bpi.infrastructure.postgres.DatasetTrainingReadinessPostgresRepository;
import com.mapletct.ftmes.bpi.infrastructure.postgres.IdempotencyRecord;
import com.mapletct.ftmes.bpi.interfaces.rest.DatasetTrainingReadinessCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DatasetTrainingReadinessService {
    private static final Pattern REVISION_HEADER = Pattern.compile("^(?:W/)?\\\"?(\\d+)\\\"?$");

    private final DatasetTrainingReadinessPostgresRepository repository;
    private final DatasetMlflowRegistrationPostgresRepository registrationRepository;
    private final DatasetPostgresRepository datasetRepository;
    private final DatasetTrainingReadinessBuilder builder;
    private final BpiPostgresRepository sharedRepository;
    private final CanonicalJson canonicalJson;
    private final ObjectMapper objectMapper;

    public DatasetTrainingReadinessService(
            DatasetTrainingReadinessPostgresRepository repository,
            DatasetMlflowRegistrationPostgresRepository registrationRepository,
            DatasetPostgresRepository datasetRepository,
            DatasetTrainingReadinessBuilder builder,
            BpiPostgresRepository sharedRepository,
            CanonicalJson canonicalJson,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.registrationRepository = registrationRepository;
        this.datasetRepository = datasetRepository;
        this.builder = builder;
        this.sharedRepository = sharedRepository;
        this.canonicalJson = canonicalJson;
        this.objectMapper = objectMapper;
    }

    @Transactional(timeout = 20)
    public CommandResult<DatasetTrainingReadinessView> assess(
            ActorContext actor,
            UUID registrationId,
            String idempotencyKey,
            String ifMatch,
            DatasetTrainingReadinessCommand command,
            String traceId) {
        validateHeaders(idempotencyKey, ifMatch);
        validateObjective(command.objectiveCode());
        long expectedRevision = parseRevision(ifMatch);
        DatasetTrainingReadinessEvidence evidence = repository.lockEvidence(actor, registrationId);
        if (evidence.registrationRevision() != expectedRevision) {
            throw new BpiConflictException(
                    "MLflow dataset registration revision is stale.",
                    evidence.registrationRevision());
        }
        if (!"REGISTERED".equals(evidence.registrationState())) {
            throw new BpiConflictException(
                    "Training readiness requires a REGISTERED MLflow Dataset Input.",
                    evidence.registrationRevision());
        }

        String path = "/bpi/v1/dataset-mlflow-registrations/" + registrationId
                + "/training-readiness-assessments";
        String requestChecksum = Checksums.sha256(
                registrationId + "|" + expectedRevision + "|"
                        + DatasetTrainingReadinessBuilder.POLICY_VERSION + "|"
                        + canonicalJson.write(command));
        CommandResult<DatasetTrainingReadinessView> replay = replay(
                actor, idempotencyKey, path, requestChecksum);
        if (replay != null) return replay;

        DatasetTrainingReadinessBuild build = builder.build(evidence);
        long sequence = repository.nextSequence(
                actor, registrationId,
                DatasetTrainingReadinessBuilder.OBJECTIVE_CODE,
                DatasetTrainingReadinessBuilder.POLICY_VERSION);
        UUID id = UUID.randomUUID();
        repository.insert(actor, id, evidence,
                DatasetTrainingReadinessBuilder.OBJECTIVE_CODE,
                DatasetTrainingReadinessBuilder.POLICY_VERSION,
                sequence, build, command.reason().trim());
        DatasetTrainingReadinessView created = repository.find(actor, id);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("mlflowRegistrationId", registrationId);
        details.put("snapshotId", evidence.snapshotId());
        details.put("objectiveCode", created.objectiveCode());
        details.put("policyVersion", created.policyVersion());
        details.put("assessmentSequence", sequence);
        details.put("state", created.state());
        details.put("assessmentChecksum", created.assessmentChecksum());
        details.put("blockerCodes", created.blockerCodes());
        details.put("assessmentOnly", true);
        details.put("trainingStarted", false);
        details.put("modelCreated", false);
        details.put("modelRegistered", false);
        details.put("onlineInferenceEnabled", false);
        details.put("productionActivationAllowed", false);
        datasetRepository.insertAudit(actor, evidence.plantId(), null,
                "DATASET_TRAINING_READINESS", id,
                "DATASET_TRAINING_READINESS_ASSESSED",
                0L, created.revision(), command.reason().trim(), traceId, details);
        complete(actor, idempotencyKey, created);
        return new CommandResult<>(created, false);
    }

    @Transactional(readOnly = true)
    public DatasetTrainingReadinessView get(ActorContext actor, UUID assessmentId) {
        return repository.find(actor, assessmentId);
    }

    @Transactional(readOnly = true)
    public DatasetTrainingReadinessView getLatest(
            ActorContext actor,
            UUID registrationId) {
        DatasetMlflowRegistrationView registration =
                registrationRepository.find(actor, registrationId);
        return repository.findLatest(actor, registration.id(),
                DatasetTrainingReadinessBuilder.OBJECTIVE_CODE,
                DatasetTrainingReadinessBuilder.POLICY_VERSION).orElse(null);
    }

    private CommandResult<DatasetTrainingReadinessView> replay(
            ActorContext actor,
            String idempotencyKey,
            String path,
            String checksum) {
        boolean owner = sharedRepository.reserveIdempotency(
                UUID.randomUUID(), actor.tenantId(), idempotencyKey,
                "POST", path, checksum);
        if (owner) return null;
        IdempotencyRecord previous = sharedRepository.lockIdempotency(
                actor.tenantId(), idempotencyKey);
        if (!"POST".equals(previous.method()) || !path.equals(previous.resourcePath())
                || !checksum.equals(previous.requestChecksum())) {
            throw new BpiConflictException(
                    "Idempotency-Key was reused with a different request.", null);
        }
        if ("COMPLETED".equals(previous.state()) && previous.responseBody() != null) {
            return new CommandResult<>(sharedRepository.readJson(
                    previous.responseBody(),
                    new TypeReference<DatasetTrainingReadinessView>() {}), true);
        }
        throw new BpiConflictException("The command is still processing.", null);
    }

    private void complete(
            ActorContext actor,
            String idempotencyKey,
            DatasetTrainingReadinessView response) {
        sharedRepository.completeIdempotency(
                actor.tenantId(), idempotencyKey, 200, writeJson(response));
    }

    private void validateObjective(String objectiveCode) {
        if (!DatasetTrainingReadinessBuilder.OBJECTIVE_CODE.equals(objectiveCode)) {
            throw new BpiValidationException(
                    "Phase 3C-B supports only BATCH_START_BOUNDARY_REVIEW_RISK.");
        }
    }

    private void validateHeaders(String idempotencyKey, String ifMatch) {
        if (idempotencyKey == null || idempotencyKey.length() < 8 || ifMatch == null) {
            throw new BpiPreconditionRequiredException(
                    "Idempotency-Key and If-Match are required.");
        }
        if (idempotencyKey.length() > 128) {
            throw new BpiValidationException(
                    "Idempotency-Key must not exceed 128 characters.");
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
            throw new IllegalStateException(
                    "Could not serialize BPI training readiness response", exception);
        }
    }
}
