package com.mapletct.ftmes.bpi.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.application.error.BpiConflictException;
import com.mapletct.ftmes.bpi.application.error.BpiPreconditionRequiredException;
import com.mapletct.ftmes.bpi.application.error.BpiValidationException;
import com.mapletct.ftmes.bpi.domain.DatasetCatalogPublicationView;
import com.mapletct.ftmes.bpi.domain.DatasetRetentionArchiveView;
import com.mapletct.ftmes.bpi.infrastructure.postgres.BpiPostgresRepository;
import com.mapletct.ftmes.bpi.infrastructure.postgres.DatasetCatalogPublicationPostgresRepository;
import com.mapletct.ftmes.bpi.infrastructure.postgres.DatasetPostgresRepository;
import com.mapletct.ftmes.bpi.infrastructure.postgres.DatasetRetentionArchivePostgresRepository;
import com.mapletct.ftmes.bpi.infrastructure.postgres.DatasetRetentionArchiveSource;
import com.mapletct.ftmes.bpi.infrastructure.postgres.IdempotencyRecord;
import com.mapletct.ftmes.bpi.interfaces.rest.ReasonCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DatasetRetentionArchiveService {
    public static final String ARCHIVER_VERSION = "bpi-dataset-retention-archiver/0.1.0";
    public static final String ARCHIVE_PROFILE = "bpi-dataset-recovery-v1";

    private static final Pattern REVISION_HEADER = Pattern.compile("^(?:W/)?\\\"?(\\d+)\\\"?$");

    private final DatasetCatalogPublicationPostgresRepository publicationRepository;
    private final DatasetRetentionArchivePostgresRepository repository;
    private final DatasetPostgresRepository datasetRepository;
    private final BpiPostgresRepository sharedRepository;
    private final CanonicalJson canonicalJson;
    private final ObjectMapper objectMapper;

    public DatasetRetentionArchiveService(
            DatasetCatalogPublicationPostgresRepository publicationRepository,
            DatasetRetentionArchivePostgresRepository repository,
            DatasetPostgresRepository datasetRepository,
            BpiPostgresRepository sharedRepository,
            CanonicalJson canonicalJson,
            ObjectMapper objectMapper) {
        this.publicationRepository = publicationRepository;
        this.repository = repository;
        this.datasetRepository = datasetRepository;
        this.sharedRepository = sharedRepository;
        this.canonicalJson = canonicalJson;
        this.objectMapper = objectMapper;
    }

    @Transactional(timeout = 20)
    public CommandResult<DatasetRetentionArchiveView> request(
            ActorContext actor,
            UUID publicationId,
            String idempotencyKey,
            String ifMatch,
            ReasonCommand command,
            String traceId) {
        validateHeaders(idempotencyKey, ifMatch);
        long expectedRevision = parseRevision(ifMatch);
        DatasetCatalogPublicationView publication = publicationRepository.find(actor, publicationId);
        if (publication.revision() != expectedRevision) {
            throw new BpiConflictException(
                    "Dataset catalog publication revision is stale.", publication.revision());
        }
        DatasetRetentionArchiveSource source = source(publication);
        String path = "/bpi/v1/dataset-catalog-publications/" + publicationId
                + "/retention-archives";
        String requestChecksum = Checksums.sha256(
                publicationId + "|" + expectedRevision + "|" + canonicalJson.write(command));
        CommandResult<DatasetRetentionArchiveView> replay = replay(
                actor, idempotencyKey, path, requestChecksum);
        if (replay != null) return replay;

        UUID id = UUID.randomUUID();
        repository.insert(actor, id, source, ARCHIVER_VERSION, ARCHIVE_PROFILE,
                command.reason().trim());
        DatasetRetentionArchiveView created = repository.find(actor, id);
        datasetRepository.insertAudit(actor, created.plantId(), null,
                "DATASET_RETENTION_ARCHIVE", id, "DATASET_RETENTION_ARCHIVE_QUEUED",
                0L, created.revision(), command.reason().trim(), traceId,
                Map.ofEntries(
                        Map.entry("catalogPublicationId", publicationId),
                        Map.entry("materializationId", created.materializationId()),
                        Map.entry("snapshotId", created.snapshotId()),
                        Map.entry("tableIdentifier", created.tableIdentifier()),
                        Map.entry("icebergSnapshotId", String.valueOf(created.icebergSnapshotId())),
                        Map.entry("archiverVersion", created.archiverVersion()),
                        Map.entry("archiveProfile", created.archiveProfile()),
                        Map.entry("objectLockVerified", false),
                        Map.entry("recoveryVerified", false),
                        Map.entry("mlflowRegistered", false),
                        Map.entry("modelTrained", false)));
        complete(actor, idempotencyKey, created);
        return new CommandResult<>(created, false);
    }

    @Transactional(readOnly = true)
    public DatasetRetentionArchiveView get(ActorContext actor, UUID archiveId) {
        return repository.find(actor, archiveId);
    }

    @Transactional(readOnly = true)
    public DatasetRetentionArchiveView getForPublication(
            ActorContext actor,
            UUID publicationId) {
        publicationRepository.find(actor, publicationId);
        return repository.findByPublication(actor, publicationId, ARCHIVER_VERSION).orElse(null);
    }

    @Transactional(timeout = 20)
    public CommandResult<DatasetRetentionArchiveView> retry(
            ActorContext actor,
            UUID archiveId,
            String idempotencyKey,
            String ifMatch,
            ReasonCommand command,
            String traceId) {
        validateHeaders(idempotencyKey, ifMatch);
        long expectedRevision = parseRevision(ifMatch);
        String path = "/bpi/v1/dataset-retention-archives/" + archiveId + "/retry";
        String requestChecksum = Checksums.sha256(
                archiveId + "|" + expectedRevision + "|" + canonicalJson.write(command));
        CommandResult<DatasetRetentionArchiveView> replay = replay(
                actor, idempotencyKey, path, requestChecksum);
        if (replay != null) return replay;

        DatasetRetentionArchiveView before = repository.find(actor, archiveId);
        DatasetRetentionArchiveView retried = repository.retry(actor, archiveId, expectedRevision);
        datasetRepository.insertAudit(actor, retried.plantId(), null,
                "DATASET_RETENTION_ARCHIVE", archiveId,
                "DATASET_RETENTION_ARCHIVE_RETRIED",
                before.revision(), retried.revision(), command.reason().trim(), traceId,
                Map.of(
                        "catalogPublicationId", retried.catalogPublicationId(),
                        "previousFailureCode", before.failureCode(),
                        "attemptCount", retried.attemptCount(),
                        "retentionMode", value(before.retentionMode()),
                        "retainUntil", value(before.retainUntil())));
        complete(actor, idempotencyKey, retried);
        return new CommandResult<>(retried, false);
    }

    private DatasetRetentionArchiveSource source(DatasetCatalogPublicationView value) {
        if (!"READY".equals(value.state())) {
            throw new BpiConflictException(
                    "Retention archive requires a READY Iceberg catalog publication.",
                    value.revision());
        }
        boolean catalogVerified = value.catalogMetadata() != null
                && Boolean.TRUE.equals(value.catalogMetadata().get("catalogSnapshotVerified"));
        if (!catalogVerified || value.icebergSnapshotId() == null
                || value.icebergMetadataLocation() == null
                || value.icebergSchemaId() == null
                || value.icebergPartitionSpecId() == null
                || value.verifiedRowCount() == null
                || value.semanticChecksum() == null
                || value.sourceSchema() == null) {
            throw new BpiConflictException(
                    "Retention archive source is missing verified catalog facts.", value.revision());
        }
        if (value.verifiedRowCount() != value.sourceRowCount()) {
            throw new BpiConflictException(
                    "Retention archive source row counts do not reconcile.", value.revision());
        }
        return new DatasetRetentionArchiveSource(
                value.id(), value.materializationId(), value.snapshotId(),
                value.manifestChecksum(), value.sourceContentSha256(),
                value.sourceObjectVersionId(), value.sourceByteSize(),
                value.sourceRowCount(), value.sourceSchema(), value.tableIdentifier(),
                value.icebergSnapshotId(), value.icebergMetadataLocation(),
                value.icebergSchemaId(), value.icebergPartitionSpecId(),
                value.verifiedRowCount(), value.semanticChecksum());
    }

    private CommandResult<DatasetRetentionArchiveView> replay(
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
                    new TypeReference<DatasetRetentionArchiveView>() {}), true);
        }
        throw new BpiConflictException("The command is still processing.", null);
    }

    private void complete(
            ActorContext actor,
            String idempotencyKey,
            DatasetRetentionArchiveView response) {
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
                    "Could not serialize BPI dataset retention archive command", exception);
        }
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
