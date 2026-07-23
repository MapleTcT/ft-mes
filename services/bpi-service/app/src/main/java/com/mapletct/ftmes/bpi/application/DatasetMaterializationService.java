package com.mapletct.ftmes.bpi.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.application.error.BpiConflictException;
import com.mapletct.ftmes.bpi.application.error.BpiPreconditionRequiredException;
import com.mapletct.ftmes.bpi.application.error.BpiValidationException;
import com.mapletct.ftmes.bpi.domain.DatasetMaterializationView;
import com.mapletct.ftmes.bpi.domain.DatasetSnapshotView;
import com.mapletct.ftmes.bpi.infrastructure.postgres.BpiPostgresRepository;
import com.mapletct.ftmes.bpi.infrastructure.postgres.DatasetMaterializationPostgresRepository;
import com.mapletct.ftmes.bpi.infrastructure.postgres.DatasetPostgresRepository;
import com.mapletct.ftmes.bpi.infrastructure.postgres.IdempotencyRecord;
import com.mapletct.ftmes.bpi.interfaces.rest.DatasetMaterializationCommand;
import com.mapletct.ftmes.bpi.interfaces.rest.ReasonCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DatasetMaterializationService {
    public static final String ARTIFACT_SCHEMA_VERSION = "bpi.dataset-parquet.v2";
    public static final String MATERIALIZER_VERSION = "bpi-dataset-materializer/0.2.0";

    private static final Pattern REVISION_HEADER = Pattern.compile("^(?:W/)?\\\"?(\\d+)\\\"?$");

    private final DatasetPostgresRepository datasetRepository;
    private final DatasetMaterializationPostgresRepository repository;
    private final BpiPostgresRepository sharedRepository;
    private final CanonicalJson canonicalJson;
    private final ObjectMapper objectMapper;

    public DatasetMaterializationService(
            DatasetPostgresRepository datasetRepository,
            DatasetMaterializationPostgresRepository repository,
            BpiPostgresRepository sharedRepository,
            CanonicalJson canonicalJson,
            ObjectMapper objectMapper) {
        this.datasetRepository = datasetRepository;
        this.repository = repository;
        this.sharedRepository = sharedRepository;
        this.canonicalJson = canonicalJson;
        this.objectMapper = objectMapper;
    }

    @Transactional(timeout = 20)
    public CommandResult<DatasetMaterializationView> request(
            ActorContext actor,
            UUID snapshotId,
            String idempotencyKey,
            String ifMatch,
            DatasetMaterializationCommand command,
            String traceId) {
        validateHeaders(idempotencyKey, ifMatch);
        long expectedRevision = parseRevision(ifMatch);
        DatasetSnapshotView snapshot = datasetRepository.findSnapshot(actor, snapshotId);
        if (snapshot.revision() != expectedRevision) {
            throw new BpiConflictException(
                    "Dataset snapshot revision is stale.", snapshot.revision());
        }
        if (!"MANIFEST_READY".equals(snapshot.state()) || snapshot.manifestChecksum() == null) {
            throw new BpiConflictException(
                    "Dataset materialization requires a MANIFEST_READY snapshot.", snapshot.revision());
        }
        if (!"PARQUET".equals(command.artifactFormat())) {
            throw new BpiValidationException("Phase 3B-A supports only PARQUET artifacts.");
        }

        String path = "/bpi/v1/dataset-snapshots/" + snapshotId + "/materializations";
        String requestChecksum = Checksums.sha256(
                snapshotId + "|" + expectedRevision + "|" + canonicalJson.write(command));
        CommandResult<DatasetMaterializationView> replay = replay(
                actor, idempotencyKey, path, requestChecksum);
        if (replay != null) return replay;

        UUID id = UUID.randomUUID();
        repository.insert(actor, id, snapshotId, command.artifactFormat(),
                ARTIFACT_SCHEMA_VERSION, MATERIALIZER_VERSION,
                snapshot.manifestChecksum(), command.reason().trim());
        DatasetMaterializationView created = repository.find(actor, id);
        datasetRepository.insertAudit(actor, snapshot.plantId(), null,
                "DATASET_MATERIALIZATION", id, "DATASET_MATERIALIZATION_QUEUED",
                0L, created.revision(), command.reason().trim(), traceId,
                Map.of(
                        "snapshotId", snapshotId,
                        "manifestChecksum", snapshot.manifestChecksum(),
                        "artifactFormat", created.artifactFormat(),
                        "artifactSchemaVersion", created.artifactSchemaVersion(),
                        "materializerVersion", created.materializerVersion(),
                        "icebergReady", false,
                        "mlflowRegistered", false,
                        "modelTrained", false));
        complete(actor, idempotencyKey, created);
        return new CommandResult<>(created, false);
    }

    @Transactional(readOnly = true)
    public DatasetMaterializationView get(ActorContext actor, UUID materializationId) {
        return repository.find(actor, materializationId);
    }

    @Transactional(timeout = 20)
    public CommandResult<DatasetMaterializationView> retry(
            ActorContext actor,
            UUID materializationId,
            String idempotencyKey,
            String ifMatch,
            ReasonCommand command,
            String traceId) {
        validateHeaders(idempotencyKey, ifMatch);
        long expectedRevision = parseRevision(ifMatch);
        String path = "/bpi/v1/dataset-materializations/" + materializationId + "/retry";
        String requestChecksum = Checksums.sha256(
                materializationId + "|" + expectedRevision + "|" + canonicalJson.write(command));
        CommandResult<DatasetMaterializationView> replay = replay(
                actor, idempotencyKey, path, requestChecksum);
        if (replay != null) return replay;

        DatasetMaterializationView before = repository.find(actor, materializationId);
        DatasetMaterializationView retried = repository.retry(actor, materializationId, expectedRevision);
        datasetRepository.insertAudit(actor, retried.plantId(), null,
                "DATASET_MATERIALIZATION", retried.id(), "DATASET_MATERIALIZATION_RETRIED",
                before.revision(), retried.revision(), command.reason().trim(), traceId,
                Map.of(
                        "snapshotId", retried.snapshotId(),
                        "previousFailureCode", before.failureCode(),
                        "attemptCount", retried.attemptCount()));
        complete(actor, idempotencyKey, retried);
        return new CommandResult<>(retried, false);
    }

    private CommandResult<DatasetMaterializationView> replay(
            ActorContext actor,
            String idempotencyKey,
            String path,
            String checksum) {
        boolean owner = sharedRepository.reserveIdempotency(
                UUID.randomUUID(), actor.tenantId(), idempotencyKey, "POST", path, checksum);
        if (owner) return null;
        IdempotencyRecord previous = sharedRepository.lockIdempotency(actor.tenantId(), idempotencyKey);
        if (!"POST".equals(previous.method()) || !path.equals(previous.resourcePath())
                || !checksum.equals(previous.requestChecksum())) {
            throw new BpiConflictException(
                    "Idempotency-Key was reused with a different request.", null);
        }
        if ("COMPLETED".equals(previous.state()) && previous.responseBody() != null) {
            return new CommandResult<>(sharedRepository.readJson(
                    previous.responseBody(), new TypeReference<DatasetMaterializationView>() {}), true);
        }
        throw new BpiConflictException("The command is still processing.", null);
    }

    private void complete(
            ActorContext actor,
            String idempotencyKey,
            DatasetMaterializationView response) {
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
                    "Could not serialize BPI dataset materialization command", exception);
        }
    }
}
