package com.mapletct.ftmes.bpi.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.application.error.BpiConflictException;
import com.mapletct.ftmes.bpi.application.error.BpiPreconditionRequiredException;
import com.mapletct.ftmes.bpi.application.error.BpiValidationException;
import com.mapletct.ftmes.bpi.domain.DatasetCatalogPublicationView;
import com.mapletct.ftmes.bpi.domain.DatasetMaterializationView;
import com.mapletct.ftmes.bpi.infrastructure.postgres.BpiPostgresRepository;
import com.mapletct.ftmes.bpi.infrastructure.postgres.DatasetCatalogPublicationPostgresRepository;
import com.mapletct.ftmes.bpi.infrastructure.postgres.DatasetCatalogPublicationSource;
import com.mapletct.ftmes.bpi.infrastructure.postgres.DatasetMaterializationPostgresRepository;
import com.mapletct.ftmes.bpi.infrastructure.postgres.DatasetPostgresRepository;
import com.mapletct.ftmes.bpi.infrastructure.postgres.IdempotencyRecord;
import com.mapletct.ftmes.bpi.interfaces.rest.ReasonCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DatasetCatalogPublicationService {
    public static final String CATALOG_NAME = "ft_mes_bpi";
    public static final String PUBLISHER_VERSION = "bpi-dataset-catalog-publisher/0.1.0";

    private static final Pattern REVISION_HEADER = Pattern.compile("^(?:W/)?\\\"?(\\d+)\\\"?$");

    private final DatasetMaterializationPostgresRepository materializationRepository;
    private final DatasetCatalogPublicationPostgresRepository repository;
    private final DatasetPostgresRepository datasetRepository;
    private final BpiPostgresRepository sharedRepository;
    private final CanonicalJson canonicalJson;
    private final ObjectMapper objectMapper;

    public DatasetCatalogPublicationService(
            DatasetMaterializationPostgresRepository materializationRepository,
            DatasetCatalogPublicationPostgresRepository repository,
            DatasetPostgresRepository datasetRepository,
            BpiPostgresRepository sharedRepository,
            CanonicalJson canonicalJson,
            ObjectMapper objectMapper) {
        this.materializationRepository = materializationRepository;
        this.repository = repository;
        this.datasetRepository = datasetRepository;
        this.sharedRepository = sharedRepository;
        this.canonicalJson = canonicalJson;
        this.objectMapper = objectMapper;
    }

    @Transactional(timeout = 20)
    public CommandResult<DatasetCatalogPublicationView> request(
            ActorContext actor,
            UUID materializationId,
            String idempotencyKey,
            String ifMatch,
            ReasonCommand command,
            String traceId) {
        validateHeaders(idempotencyKey, ifMatch);
        long expectedRevision = parseRevision(ifMatch);
        DatasetMaterializationView materialization =
                materializationRepository.find(actor, materializationId);
        if (materialization.revision() != expectedRevision) {
            throw new BpiConflictException(
                    "Dataset materialization revision is stale.", materialization.revision());
        }
        DatasetCatalogPublicationSource source = source(materialization);

        String path = "/bpi/v1/dataset-materializations/" + materializationId
                + "/catalog-publications";
        String requestChecksum = Checksums.sha256(
                materializationId + "|" + expectedRevision + "|" + canonicalJson.write(command));
        CommandResult<DatasetCatalogPublicationView> replay = replay(
                actor, idempotencyKey, path, requestChecksum);
        if (replay != null) return replay;

        UUID id = UUID.randomUUID();
        String namespace = "bpi_training.tenant_"
                + Checksums.sha256(actor.tenantId()).substring(0, 16);
        String tableName = "dataset_" + materialization.datasetId().toString().replace("-", "");
        repository.insert(actor, id, source, CATALOG_NAME, namespace, tableName,
                PUBLISHER_VERSION, command.reason().trim());
        DatasetCatalogPublicationView created = repository.find(actor, id);
        datasetRepository.insertAudit(actor, created.plantId(), null,
                "DATASET_CATALOG_PUBLICATION", id, "DATASET_CATALOG_PUBLICATION_QUEUED",
                0L, created.revision(), command.reason().trim(), traceId,
                Map.of(
                        "materializationId", materializationId,
                        "snapshotId", created.snapshotId(),
                        "manifestChecksum", created.manifestChecksum(),
                        "sourceContentSha256", created.sourceContentSha256(),
                        "sourceObjectVersionId", created.sourceObjectVersionId(),
                        "tableIdentifier", created.tableIdentifier(),
                        "publisherVersion", created.publisherVersion(),
                        "icebergReady", false,
                        "mlflowRegistered", false,
                        "modelTrained", false));
        complete(actor, idempotencyKey, created);
        return new CommandResult<>(created, false);
    }

    @Transactional(readOnly = true)
    public DatasetCatalogPublicationView get(ActorContext actor, UUID publicationId) {
        return repository.find(actor, publicationId);
    }

    @Transactional(readOnly = true)
    public DatasetCatalogPublicationView getForMaterialization(
            ActorContext actor,
            UUID materializationId) {
        return repository.findByMaterialization(
                actor, materializationId, CATALOG_NAME, PUBLISHER_VERSION).orElse(null);
    }

    @Transactional(timeout = 20)
    public CommandResult<DatasetCatalogPublicationView> retry(
            ActorContext actor,
            UUID publicationId,
            String idempotencyKey,
            String ifMatch,
            ReasonCommand command,
            String traceId) {
        validateHeaders(idempotencyKey, ifMatch);
        long expectedRevision = parseRevision(ifMatch);
        String path = "/bpi/v1/dataset-catalog-publications/" + publicationId + "/retry";
        String requestChecksum = Checksums.sha256(
                publicationId + "|" + expectedRevision + "|" + canonicalJson.write(command));
        CommandResult<DatasetCatalogPublicationView> replay = replay(
                actor, idempotencyKey, path, requestChecksum);
        if (replay != null) return replay;

        DatasetCatalogPublicationView before = repository.find(actor, publicationId);
        DatasetCatalogPublicationView retried = repository.retry(
                actor, publicationId, expectedRevision);
        datasetRepository.insertAudit(actor, retried.plantId(), null,
                "DATASET_CATALOG_PUBLICATION", retried.id(),
                "DATASET_CATALOG_PUBLICATION_RETRIED",
                before.revision(), retried.revision(), command.reason().trim(), traceId,
                Map.of(
                        "materializationId", retried.materializationId(),
                        "snapshotId", retried.snapshotId(),
                        "tableIdentifier", retried.tableIdentifier(),
                        "previousFailureCode", before.failureCode(),
                        "attemptCount", retried.attemptCount()));
        complete(actor, idempotencyKey, retried);
        return new CommandResult<>(retried, false);
    }

    private DatasetCatalogPublicationSource source(DatasetMaterializationView value) {
        if (!"READY".equals(value.state())) {
            throw new BpiConflictException(
                    "Catalog publication requires a READY Parquet materialization.", value.revision());
        }
        Object versionId = value.artifactMetadata() == null
                ? null : value.artifactMetadata().get("objectVersionId");
        if (value.snapshotId() == null || value.manifestChecksum() == null
                || value.contentSha256() == null || value.byteSize() == null
                || value.rowCount() == null || value.schema() == null
                || versionId == null || versionId.toString().isBlank()) {
            throw new BpiConflictException(
                    "Catalog publication source is missing verified artifact facts.", value.revision());
        }
        if (value.rowCount() <= 0) {
            throw new BpiConflictException(
                    "Catalog publication requires at least one verified Parquet row.",
                    value.revision());
        }
        return new DatasetCatalogPublicationSource(
                value.id(), value.snapshotId(), value.manifestChecksum(),
                value.contentSha256(), versionId.toString(), value.byteSize(),
                value.rowCount(), value.schema());
    }

    private CommandResult<DatasetCatalogPublicationView> replay(
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
                    new TypeReference<DatasetCatalogPublicationView>() {}), true);
        }
        throw new BpiConflictException("The command is still processing.", null);
    }

    private void complete(
            ActorContext actor,
            String idempotencyKey,
            DatasetCatalogPublicationView response) {
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
                    "Could not serialize BPI dataset catalog publication command", exception);
        }
    }
}
