package com.mapletct.ftmes.bpi.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.application.error.BpiConflictException;
import com.mapletct.ftmes.bpi.application.error.BpiPreconditionRequiredException;
import com.mapletct.ftmes.bpi.application.error.BpiValidationException;
import com.mapletct.ftmes.bpi.domain.DatasetMlflowRegistrationView;
import com.mapletct.ftmes.bpi.domain.DatasetRetentionArchiveView;
import com.mapletct.ftmes.bpi.infrastructure.postgres.BpiPostgresRepository;
import com.mapletct.ftmes.bpi.infrastructure.postgres.DatasetMlflowRegistrationPostgresRepository;
import com.mapletct.ftmes.bpi.infrastructure.postgres.DatasetMlflowRegistrationSource;
import com.mapletct.ftmes.bpi.infrastructure.postgres.DatasetPostgresRepository;
import com.mapletct.ftmes.bpi.infrastructure.postgres.DatasetRetentionArchivePostgresRepository;
import com.mapletct.ftmes.bpi.infrastructure.postgres.IdempotencyRecord;
import com.mapletct.ftmes.bpi.interfaces.rest.ReasonCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DatasetMlflowRegistrationService {
    public static final String REGISTRAR_VERSION = "bpi-dataset-mlflow-registrar/0.1.0";
    public static final String TRACKING_PROFILE = "bpi-mlflow-dataset-v1";

    private static final Pattern REVISION_HEADER = Pattern.compile("^(?:W/)?\\\"?(\\d+)\\\"?$");
    private static final Pattern SAFE_NAME = Pattern.compile("[^A-Za-z0-9_.-]");

    private final DatasetRetentionArchivePostgresRepository archiveRepository;
    private final DatasetMlflowRegistrationPostgresRepository repository;
    private final DatasetPostgresRepository datasetRepository;
    private final BpiPostgresRepository sharedRepository;
    private final CanonicalJson canonicalJson;
    private final ObjectMapper objectMapper;

    public DatasetMlflowRegistrationService(
            DatasetRetentionArchivePostgresRepository archiveRepository,
            DatasetMlflowRegistrationPostgresRepository repository,
            DatasetPostgresRepository datasetRepository,
            BpiPostgresRepository sharedRepository,
            CanonicalJson canonicalJson,
            ObjectMapper objectMapper) {
        this.archiveRepository = archiveRepository;
        this.repository = repository;
        this.datasetRepository = datasetRepository;
        this.sharedRepository = sharedRepository;
        this.canonicalJson = canonicalJson;
        this.objectMapper = objectMapper;
    }

    @Transactional(timeout = 20)
    public CommandResult<DatasetMlflowRegistrationView> request(
            ActorContext actor,
            UUID archiveId,
            String idempotencyKey,
            String ifMatch,
            ReasonCommand command,
            String traceId) {
        validateHeaders(idempotencyKey, ifMatch);
        long expectedRevision = parseRevision(ifMatch);
        DatasetRetentionArchiveView archive = archiveRepository.find(actor, archiveId);
        if (archive.revision() != expectedRevision) {
            throw new BpiConflictException(
                    "Dataset retention archive revision is stale.", archive.revision());
        }
        DatasetMlflowRegistrationSource source = source(archive);
        String path = "/bpi/v1/dataset-retention-archives/" + archiveId
                + "/mlflow-registrations";
        String requestChecksum = Checksums.sha256(
                archiveId + "|" + expectedRevision + "|" + canonicalJson.write(command));
        CommandResult<DatasetMlflowRegistrationView> replay = replay(
                actor, idempotencyKey, path, requestChecksum);
        if (replay != null) return replay;

        UUID id = UUID.randomUUID();
        String experimentName = "ft-mes-bpi-training-candidates-"
                + SAFE_NAME.matcher(actor.tenantId()).replaceAll("_");
        String datasetDigest = source.catalogSemanticChecksum().substring(0, 16);
        repository.insert(actor, id, source, REGISTRAR_VERSION, TRACKING_PROFILE,
                experimentName, source.datasetCode(), datasetDigest,
                command.reason().trim());
        DatasetMlflowRegistrationView created = repository.find(actor, id);
        datasetRepository.insertAudit(actor, created.plantId(), null,
                "DATASET_MLFLOW_REGISTRATION", id,
                "DATASET_MLFLOW_REGISTRATION_QUEUED",
                0L, created.revision(), command.reason().trim(), traceId,
                Map.ofEntries(
                        Map.entry("retentionArchiveId", archiveId),
                        Map.entry("catalogPublicationId", created.catalogPublicationId()),
                        Map.entry("materializationId", created.materializationId()),
                        Map.entry("snapshotId", created.snapshotId()),
                        Map.entry("experimentName", created.experimentName()),
                        Map.entry("datasetName", created.datasetName()),
                        Map.entry("datasetDigest", created.datasetDigest()),
                        Map.entry("sourceArchiveVersionId", created.sourceArchiveVersionId()),
                        Map.entry("registrarVersion", created.registrarVersion()),
                        Map.entry("trackingProfile", created.trackingProfile()),
                        Map.entry("datasetInputVerified", false),
                        Map.entry("modelTrained", false),
                        Map.entry("modelRegistered", false),
                        Map.entry("productionActivationAllowed", false)));
        complete(actor, idempotencyKey, created);
        return new CommandResult<>(created, false);
    }

    @Transactional(readOnly = true)
    public DatasetMlflowRegistrationView get(ActorContext actor, UUID registrationId) {
        return repository.find(actor, registrationId);
    }

    @Transactional(readOnly = true)
    public DatasetMlflowRegistrationView getForArchive(
            ActorContext actor,
            UUID archiveId) {
        archiveRepository.find(actor, archiveId);
        return repository.findByArchive(actor, archiveId, REGISTRAR_VERSION).orElse(null);
    }

    @Transactional(timeout = 20)
    public CommandResult<DatasetMlflowRegistrationView> retry(
            ActorContext actor,
            UUID registrationId,
            String idempotencyKey,
            String ifMatch,
            ReasonCommand command,
            String traceId) {
        validateHeaders(idempotencyKey, ifMatch);
        long expectedRevision = parseRevision(ifMatch);
        String path = "/bpi/v1/dataset-mlflow-registrations/" + registrationId + "/retry";
        String requestChecksum = Checksums.sha256(
                registrationId + "|" + expectedRevision + "|" + canonicalJson.write(command));
        CommandResult<DatasetMlflowRegistrationView> replay = replay(
                actor, idempotencyKey, path, requestChecksum);
        if (replay != null) return replay;

        DatasetMlflowRegistrationView before = repository.find(actor, registrationId);
        DatasetMlflowRegistrationView retried = repository.retry(
                actor, registrationId, expectedRevision);
        datasetRepository.insertAudit(actor, retried.plantId(), null,
                "DATASET_MLFLOW_REGISTRATION", registrationId,
                "DATASET_MLFLOW_REGISTRATION_RETRIED",
                before.revision(), retried.revision(), command.reason().trim(), traceId,
                Map.of(
                        "retentionArchiveId", retried.retentionArchiveId(),
                        "previousFailureCode", value(before.failureCode()),
                        "attemptCount", retried.attemptCount(),
                        "datasetDigest", retried.datasetDigest(),
                        "modelTrained", false,
                        "productionActivationAllowed", false));
        complete(actor, idempotencyKey, retried);
        return new CommandResult<>(retried, false);
    }

    private DatasetMlflowRegistrationSource source(DatasetRetentionArchiveView value) {
        boolean recoveryVerified = value.archiveMetadata() != null
                && Boolean.TRUE.equals(value.archiveMetadata().get("objectLockVerified"))
                && Boolean.TRUE.equals(value.archiveMetadata().get("recoveryVerified"));
        if (!"LOCKED".equals(value.state()) || !recoveryVerified) {
            throw new BpiConflictException(
                    "MLflow registration requires a LOCKED and recovery-verified archive.",
                    value.revision());
        }
        if (value.archiveBucket() == null || value.sourceArchiveObjectKey() == null
                || value.sourceArchiveVersionId() == null
                || value.archiveManifestObjectKey() == null
                || value.archiveManifestVersionId() == null
                || value.archiveManifestSha256() == null
                || value.catalogSemanticChecksum() == null
                || value.sourceSchema() == null
                || value.verifiedRowCount() == null
                || value.verifiedSemanticChecksum() == null) {
            throw new BpiConflictException(
                    "MLflow registration source is missing immutable recovery facts.",
                    value.revision());
        }
        if (value.verifiedRowCount() != value.sourceRowCount()
                || !value.catalogSemanticChecksum().equals(value.verifiedSemanticChecksum())) {
            throw new BpiConflictException(
                    "MLflow registration source does not reconcile with the recovery archive.",
                    value.revision());
        }
        return new DatasetMlflowRegistrationSource(
                value.id(), value.catalogPublicationId(), value.materializationId(),
                value.snapshotId(), value.manifestChecksum(), value.sourceContentSha256(),
                value.sourceObjectVersionId(), value.sourceByteSize(), value.sourceRowCount(),
                value.sourceSchema(), value.tableIdentifier(), value.icebergSnapshotId(),
                value.catalogSemanticChecksum(), value.archiveBucket(),
                value.sourceArchiveObjectKey(), value.sourceArchiveVersionId(),
                value.archiveManifestObjectKey(), value.archiveManifestVersionId(),
                value.archiveManifestSha256(), value.datasetCode());
    }

    private CommandResult<DatasetMlflowRegistrationView> replay(
            ActorContext actor,
            String idempotencyKey,
            String path,
            String checksum) {
        boolean owner = sharedRepository.reserveIdempotency(
                UUID.randomUUID(), actor.tenantId(), idempotencyKey, "POST", path, checksum);
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
                    new TypeReference<DatasetMlflowRegistrationView>() {}), true);
        }
        throw new BpiConflictException("The command is still processing.", null);
    }

    private void complete(
            ActorContext actor,
            String idempotencyKey,
            DatasetMlflowRegistrationView response) {
        sharedRepository.completeIdempotency(
                actor.tenantId(), idempotencyKey, 202, writeJson(response));
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
            throw new IllegalStateException(
                    "Could not serialize BPI MLflow registration command", exception);
        }
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
