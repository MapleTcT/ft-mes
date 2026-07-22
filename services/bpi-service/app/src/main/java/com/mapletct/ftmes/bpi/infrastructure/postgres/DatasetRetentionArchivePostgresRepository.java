package com.mapletct.ftmes.bpi.infrastructure.postgres;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.application.ActorContext;
import com.mapletct.ftmes.bpi.application.error.BpiConflictException;
import com.mapletct.ftmes.bpi.application.error.BpiNotFoundException;
import com.mapletct.ftmes.bpi.domain.DatasetRetentionArchiveView;
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
public class DatasetRetentionArchivePostgresRepository {
    private static final String SELECT = """
            SELECT archive.*, publication.materialization_id,
                   materialization.snapshot_id,
                   snapshot.dataset_id, snapshot.line_ids::text AS line_ids,
                   definition.dataset_code, definition.version AS dataset_version,
                   definition.plant_id
              FROM bpi.bpi_dataset_retention_archives archive
              JOIN bpi.bpi_dataset_catalog_publications publication
                ON publication.tenant_id = archive.tenant_id
               AND publication.id = archive.catalog_publication_id
              JOIN bpi.bpi_dataset_materializations materialization
                ON materialization.tenant_id = publication.tenant_id
               AND materialization.id = publication.materialization_id
              JOIN bpi.bpi_dataset_snapshots snapshot
                ON snapshot.tenant_id = materialization.tenant_id
               AND snapshot.id = materialization.snapshot_id
              JOIN bpi.bpi_dataset_definitions definition
                ON definition.tenant_id = snapshot.tenant_id
               AND definition.id = snapshot.dataset_id
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public DatasetRetentionArchivePostgresRepository(
            NamedParameterJdbcTemplate jdbc,
            ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public void insert(
            ActorContext actor,
            UUID id,
            DatasetRetentionArchiveSource source,
            String archiverVersion,
            String archiveProfile,
            String reason) {
        try {
            jdbc.update("""
                    INSERT INTO bpi.bpi_dataset_retention_archives
                        (id, tenant_id, catalog_publication_id, source_snapshot_id,
                         source_materialization_id, archiver_version, archive_profile,
                         state, manifest_checksum, source_content_sha256,
                         source_object_version_id, source_byte_size, source_row_count,
                         source_schema_json, table_identifier, iceberg_snapshot_id,
                         iceberg_metadata_location, iceberg_schema_id,
                         iceberg_partition_spec_id, catalog_verified_row_count,
                         catalog_semantic_checksum, requested_by, request_reason)
                    VALUES (:id, :tenantId, :publicationId, :snapshotId,
                            :materializationId, :archiverVersion, :archiveProfile,
                            'QUEUED', :manifestChecksum, :sourceContentSha256,
                            :sourceObjectVersionId, :sourceByteSize, :sourceRowCount,
                            CAST(:sourceSchema AS jsonb), :tableIdentifier, :icebergSnapshotId,
                            :icebergMetadataLocation, :icebergSchemaId,
                            :icebergPartitionSpecId, :catalogVerifiedRowCount,
                            :catalogSemanticChecksum, :requestedBy, :requestReason)
                    """, new MapSqlParameterSource()
                    .addValue("id", id)
                    .addValue("tenantId", actor.tenantId())
                    .addValue("publicationId", source.publicationId())
                    .addValue("snapshotId", source.snapshotId())
                    .addValue("materializationId", source.materializationId())
                    .addValue("archiverVersion", archiverVersion)
                    .addValue("archiveProfile", archiveProfile)
                    .addValue("manifestChecksum", source.manifestChecksum())
                    .addValue("sourceContentSha256", source.sourceContentSha256())
                    .addValue("sourceObjectVersionId", source.sourceObjectVersionId())
                    .addValue("sourceByteSize", source.sourceByteSize())
                    .addValue("sourceRowCount", source.sourceRowCount())
                    .addValue("sourceSchema", writeJson(source.sourceSchema()))
                    .addValue("tableIdentifier", source.tableIdentifier())
                    .addValue("icebergSnapshotId", source.icebergSnapshotId())
                    .addValue("icebergMetadataLocation", source.icebergMetadataLocation())
                    .addValue("icebergSchemaId", source.icebergSchemaId())
                    .addValue("icebergPartitionSpecId", source.icebergPartitionSpecId())
                    .addValue("catalogVerifiedRowCount", source.catalogVerifiedRowCount())
                    .addValue("catalogSemanticChecksum", source.catalogSemanticChecksum())
                    .addValue("requestedBy", actor.userId())
                    .addValue("requestReason", reason));
        } catch (DataIntegrityViolationException exception) {
            throw new BpiConflictException(
                    "A retention archive already exists for this catalog publication contract.",
                    null);
        }
    }

    public DatasetRetentionArchiveView find(ActorContext actor, UUID id) {
        StringBuilder sql = new StringBuilder(SELECT)
                .append(" WHERE archive.tenant_id = :tenantId")
                .append(" AND archive.id = :id");
        MapSqlParameterSource parameters = scoped(actor, sql)
                .addValue("tenantId", actor.tenantId())
                .addValue("id", id);
        try {
            DatasetRetentionArchiveView value = jdbc.queryForObject(
                    sql.toString(), parameters, (rs, rowIndex) -> map(rs));
            if (value == null) throw notFound();
            return value;
        } catch (EmptyResultDataAccessException exception) {
            throw notFound();
        }
    }

    public Optional<DatasetRetentionArchiveView> findByPublication(
            ActorContext actor,
            UUID publicationId,
            String archiverVersion) {
        StringBuilder sql = new StringBuilder(SELECT)
                .append(" WHERE archive.tenant_id = :tenantId")
                .append(" AND archive.catalog_publication_id = :publicationId")
                .append(" AND archive.archiver_version = :archiverVersion");
        MapSqlParameterSource parameters = scoped(actor, sql)
                .addValue("tenantId", actor.tenantId())
                .addValue("publicationId", publicationId)
                .addValue("archiverVersion", archiverVersion);
        try {
            return Optional.ofNullable(jdbc.queryForObject(
                    sql.toString(), parameters, (rs, rowIndex) -> map(rs)));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    @Transactional
    public DatasetRetentionArchiveView retry(
            ActorContext actor,
            UUID id,
            long expectedRevision) {
        DatasetRetentionArchiveView current = lock(actor, id);
        if (current.revision() != expectedRevision) {
            throw new BpiConflictException(
                    "Dataset retention archive revision is stale.", current.revision());
        }
        if (!"FAILED".equals(current.state())) {
            throw new BpiConflictException(
                    "Only a FAILED dataset retention archive can be retried.",
                    current.revision());
        }
        int updated = jdbc.update("""
                UPDATE bpi.bpi_dataset_retention_archives
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
                    "Dataset retention archive changed before retry.", current.revision());
        }
        return find(actor, id);
    }

    private DatasetRetentionArchiveView lock(ActorContext actor, UUID id) {
        StringBuilder sql = new StringBuilder(SELECT)
                .append(" WHERE archive.tenant_id = :tenantId")
                .append(" AND archive.id = :id");
        MapSqlParameterSource parameters = scoped(actor, sql)
                .addValue("tenantId", actor.tenantId())
                .addValue("id", id);
        sql.append(" FOR UPDATE OF archive");
        try {
            DatasetRetentionArchiveView value = jdbc.queryForObject(
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

    private DatasetRetentionArchiveView map(ResultSet rs) throws SQLException {
        return new DatasetRetentionArchiveView(
                rs.getObject("id", UUID.class),
                rs.getObject("catalog_publication_id", UUID.class),
                rs.getObject("materialization_id", UUID.class),
                rs.getObject("snapshot_id", UUID.class),
                rs.getObject("dataset_id", UUID.class),
                rs.getString("dataset_code"),
                rs.getString("dataset_version"),
                rs.getString("tenant_id"),
                rs.getString("plant_id"),
                readStrings(rs.getString("line_ids")),
                rs.getString("archiver_version"),
                rs.getString("archive_profile"),
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
                rs.getString("iceberg_metadata_location"),
                rs.getInt("iceberg_schema_id"),
                rs.getInt("iceberg_partition_spec_id"),
                rs.getLong("catalog_verified_row_count"),
                rs.getString("catalog_semantic_checksum"),
                rs.getString("requested_by"),
                rs.getString("request_reason"),
                instant(rs, "created_at"),
                instant(rs, "started_at"),
                instant(rs, "completed_at"),
                rs.getInt("attempt_count"),
                rs.getString("retention_mode"),
                instant(rs, "retain_until"),
                rs.getObject("legal_hold_enabled", Boolean.class),
                rs.getString("archive_bucket"),
                rs.getString("archive_prefix"),
                rs.getString("source_archive_object_key"),
                rs.getString("source_archive_version_id"),
                rs.getString("archive_manifest_object_key"),
                rs.getString("archive_manifest_version_id"),
                rs.getString("archive_manifest_sha256"),
                rs.getObject("archive_object_count", Integer.class),
                rs.getObject("archive_total_bytes", Long.class),
                rs.getObject("verified_row_count", Long.class),
                rs.getString("verified_semantic_checksum"),
                readMap(rs.getString("archive_metadata")),
                rs.getString("failure_code"),
                rs.getString("failure_detail"));
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private BpiNotFoundException notFound() {
        return new BpiNotFoundException("Dataset retention archive not found.");
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize BPI retention archive JSON", exception);
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
            throw new IllegalStateException("Could not read BPI retention archive JSON", exception);
        }
    }
}
