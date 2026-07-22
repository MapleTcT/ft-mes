package com.mapletct.ftmes.bpi.infrastructure.postgres;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.application.ActorContext;
import com.mapletct.ftmes.bpi.application.error.BpiConflictException;
import com.mapletct.ftmes.bpi.application.error.BpiNotFoundException;
import com.mapletct.ftmes.bpi.domain.DatasetMlflowRegistrationView;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DatasetMlflowRegistrationPostgresRepository {
    private static final String SELECT = """
            SELECT registration.*, snapshot.dataset_id,
                   snapshot.line_ids::text AS line_ids,
                   definition.dataset_code, definition.version AS dataset_version,
                   definition.plant_id
              FROM bpi.bpi_dataset_mlflow_registrations registration
              JOIN bpi.bpi_dataset_snapshots snapshot
                ON snapshot.tenant_id = registration.tenant_id
               AND snapshot.id = registration.source_snapshot_id
              JOIN bpi.bpi_dataset_definitions definition
                ON definition.tenant_id = snapshot.tenant_id
               AND definition.id = snapshot.dataset_id
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public DatasetMlflowRegistrationPostgresRepository(
            NamedParameterJdbcTemplate jdbc,
            ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public void insert(
            ActorContext actor,
            UUID id,
            DatasetMlflowRegistrationSource source,
            String registrarVersion,
            String trackingProfile,
            String experimentName,
            String datasetName,
            String datasetDigest,
            String reason) {
        try {
            jdbc.update("""
                    INSERT INTO bpi.bpi_dataset_mlflow_registrations
                        (id, tenant_id, retention_archive_id, catalog_publication_id,
                         source_snapshot_id, source_materialization_id,
                         registrar_version, tracking_profile, state,
                         manifest_checksum, source_content_sha256,
                         source_object_version_id, source_byte_size, source_row_count,
                         source_schema_json, table_identifier, iceberg_snapshot_id,
                         catalog_semantic_checksum, archive_bucket,
                         source_archive_object_key, source_archive_version_id,
                         archive_manifest_object_key, archive_manifest_version_id,
                         archive_manifest_sha256, experiment_name, dataset_name,
                         dataset_digest, requested_by, request_reason)
                    VALUES (:id, :tenantId, :archiveId, :publicationId,
                            :snapshotId, :materializationId,
                            :registrarVersion, :trackingProfile, 'QUEUED',
                            :manifestChecksum, :sourceContentSha256,
                            :sourceObjectVersionId, :sourceByteSize, :sourceRowCount,
                            CAST(:sourceSchema AS jsonb), :tableIdentifier, :icebergSnapshotId,
                            :catalogSemanticChecksum, :archiveBucket,
                            :sourceArchiveObjectKey, :sourceArchiveVersionId,
                            :archiveManifestObjectKey, :archiveManifestVersionId,
                            :archiveManifestSha256, :experimentName, :datasetName,
                            :datasetDigest, :requestedBy, :requestReason)
                    """, new MapSqlParameterSource()
                    .addValue("id", id)
                    .addValue("tenantId", actor.tenantId())
                    .addValue("archiveId", source.archiveId())
                    .addValue("publicationId", source.publicationId())
                    .addValue("snapshotId", source.snapshotId())
                    .addValue("materializationId", source.materializationId())
                    .addValue("registrarVersion", registrarVersion)
                    .addValue("trackingProfile", trackingProfile)
                    .addValue("manifestChecksum", source.manifestChecksum())
                    .addValue("sourceContentSha256", source.sourceContentSha256())
                    .addValue("sourceObjectVersionId", source.sourceObjectVersionId())
                    .addValue("sourceByteSize", source.sourceByteSize())
                    .addValue("sourceRowCount", source.sourceRowCount())
                    .addValue("sourceSchema", writeJson(source.sourceSchema()))
                    .addValue("tableIdentifier", source.tableIdentifier())
                    .addValue("icebergSnapshotId", source.icebergSnapshotId())
                    .addValue("catalogSemanticChecksum", source.catalogSemanticChecksum())
                    .addValue("archiveBucket", source.archiveBucket())
                    .addValue("sourceArchiveObjectKey", source.sourceArchiveObjectKey())
                    .addValue("sourceArchiveVersionId", source.sourceArchiveVersionId())
                    .addValue("archiveManifestObjectKey", source.archiveManifestObjectKey())
                    .addValue("archiveManifestVersionId", source.archiveManifestVersionId())
                    .addValue("archiveManifestSha256", source.archiveManifestSha256())
                    .addValue("experimentName", experimentName)
                    .addValue("datasetName", datasetName)
                    .addValue("datasetDigest", datasetDigest)
                    .addValue("requestedBy", actor.userId())
                    .addValue("requestReason", reason));
        } catch (DataIntegrityViolationException exception) {
            throw new BpiConflictException(
                    "An MLflow registration already exists for this retention archive contract.",
                    null);
        }
    }

    public DatasetMlflowRegistrationView find(ActorContext actor, UUID id) {
        StringBuilder sql = new StringBuilder(SELECT)
                .append(" WHERE registration.tenant_id = :tenantId")
                .append(" AND registration.id = :id");
        MapSqlParameterSource parameters = scoped(actor, sql)
                .addValue("tenantId", actor.tenantId())
                .addValue("id", id);
        try {
            DatasetMlflowRegistrationView value = jdbc.queryForObject(
                    sql.toString(), parameters, (rs, rowIndex) -> map(rs));
            if (value == null) throw notFound();
            return value;
        } catch (EmptyResultDataAccessException exception) {
            throw notFound();
        }
    }

    public Optional<DatasetMlflowRegistrationView> findByArchive(
            ActorContext actor,
            UUID archiveId,
            String registrarVersion) {
        StringBuilder sql = new StringBuilder(SELECT)
                .append(" WHERE registration.tenant_id = :tenantId")
                .append(" AND registration.retention_archive_id = :archiveId")
                .append(" AND registration.registrar_version = :registrarVersion");
        MapSqlParameterSource parameters = scoped(actor, sql)
                .addValue("tenantId", actor.tenantId())
                .addValue("archiveId", archiveId)
                .addValue("registrarVersion", registrarVersion);
        try {
            return Optional.ofNullable(jdbc.queryForObject(
                    sql.toString(), parameters, (rs, rowIndex) -> map(rs)));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    @Transactional
    public DatasetMlflowRegistrationView retry(
            ActorContext actor,
            UUID id,
            long expectedRevision) {
        DatasetMlflowRegistrationView current = lock(actor, id);
        if (current.revision() != expectedRevision) {
            throw new BpiConflictException(
                    "MLflow dataset registration revision is stale.", current.revision());
        }
        if (!"FAILED".equals(current.state())) {
            throw new BpiConflictException(
                    "Only a FAILED MLflow dataset registration can be retried.",
                    current.revision());
        }
        int updated = jdbc.update("""
                UPDATE bpi.bpi_dataset_mlflow_registrations
                   SET state = 'QUEUED', revision = revision + 1,
                       started_at = NULL, completed_at = NULL,
                       claim_token = NULL, claimed_at = NULL,
                       failure_code = NULL, failure_detail = NULL
                 WHERE tenant_id = :tenantId AND id = :id
                   AND state = 'FAILED' AND revision = :expectedRevision
                """, new MapSqlParameterSource()
                .addValue("tenantId", actor.tenantId())
                .addValue("id", id)
                .addValue("expectedRevision", expectedRevision));
        if (updated != 1) {
            throw new BpiConflictException(
                    "MLflow dataset registration changed before retry.", current.revision());
        }
        return find(actor, id);
    }

    private DatasetMlflowRegistrationView lock(ActorContext actor, UUID id) {
        StringBuilder sql = new StringBuilder(SELECT)
                .append(" WHERE registration.tenant_id = :tenantId")
                .append(" AND registration.id = :id");
        MapSqlParameterSource parameters = scoped(actor, sql)
                .addValue("tenantId", actor.tenantId())
                .addValue("id", id);
        sql.append(" FOR UPDATE OF registration");
        try {
            DatasetMlflowRegistrationView value = jdbc.queryForObject(
                    sql.toString(), parameters, (rs, rowIndex) -> map(rs));
            if (value == null) throw notFound();
            return value;
        } catch (EmptyResultDataAccessException exception) {
            throw notFound();
        }
    }

    private MapSqlParameterSource scoped(ActorContext actor, StringBuilder sql) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        if (!actor.plantIds().contains("*")) {
            sql.append(" AND definition.plant_id IN (:allowedPlants)");
            parameters.addValue("allowedPlants", actor.plantIds().isEmpty()
                    ? List.of("__NO_PLANT_SCOPE__") : actor.plantIds());
        }
        if (!actor.lineIds().contains("*")) {
            sql.append(" AND CAST(:allowedLines AS jsonb) @> snapshot.line_ids");
            List<String> lines = actor.lineIds().isEmpty()
                    ? List.of("__NO_LINE_SCOPE__") : actor.lineIds().stream().sorted().toList();
            parameters.addValue("allowedLines", writeJson(lines));
        }
        return parameters;
    }

    private DatasetMlflowRegistrationView map(ResultSet rs) throws SQLException {
        return new DatasetMlflowRegistrationView(
                rs.getObject("id", UUID.class),
                rs.getObject("retention_archive_id", UUID.class),
                rs.getObject("catalog_publication_id", UUID.class),
                rs.getObject("source_materialization_id", UUID.class),
                rs.getObject("source_snapshot_id", UUID.class),
                rs.getObject("dataset_id", UUID.class),
                rs.getString("dataset_code"),
                rs.getString("dataset_version"),
                rs.getString("tenant_id"),
                rs.getString("plant_id"),
                readStrings(rs.getString("line_ids")),
                rs.getString("registrar_version"),
                rs.getString("tracking_profile"),
                rs.getString("state"),
                rs.getLong("revision"),
                rs.getString("manifest_checksum"),
                rs.getString("source_content_sha256"),
                rs.getString("source_object_version_id"),
                rs.getLong("source_byte_size"),
                rs.getLong("source_row_count"),
                readMap(rs.getString("source_schema_json")),
                rs.getString("table_identifier"),
                rs.getLong("iceberg_snapshot_id"),
                rs.getString("catalog_semantic_checksum"),
                rs.getString("archive_bucket"),
                rs.getString("source_archive_object_key"),
                rs.getString("source_archive_version_id"),
                rs.getString("archive_manifest_object_key"),
                rs.getString("archive_manifest_version_id"),
                rs.getString("archive_manifest_sha256"),
                rs.getString("experiment_name"),
                rs.getString("dataset_name"),
                rs.getString("dataset_digest"),
                rs.getString("requested_by"),
                rs.getString("request_reason"),
                instant(rs, "created_at"),
                instant(rs, "started_at"),
                instant(rs, "completed_at"),
                rs.getInt("attempt_count"),
                rs.getString("mlflow_experiment_id"),
                rs.getString("mlflow_run_id"),
                rs.getString("mlflow_artifact_uri"),
                rs.getString("mlflow_dataset_source"),
                readMap(rs.getString("registration_metadata")),
                rs.getString("failure_code"),
                rs.getString("failure_detail"));
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private BpiNotFoundException notFound() {
        return new BpiNotFoundException("MLflow dataset registration not found.");
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize BPI MLflow registration JSON", exception);
        }
    }

    private List<String> readStrings(String value) {
        if (value == null) return List.of();
        try {
            return objectMapper.readValue(value, new TypeReference<List<String>>() {});
        } catch (Exception exception) {
            throw new IllegalStateException("Could not read BPI dataset line scope", exception);
        }
    }

    private Map<String, Object> readMap(String value) {
        if (value == null) return null;
        try {
            return objectMapper.readValue(value, new TypeReference<Map<String, Object>>() {});
        } catch (Exception exception) {
            throw new IllegalStateException("Could not read BPI MLflow registration JSON", exception);
        }
    }
}
