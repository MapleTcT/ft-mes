package com.mapletct.ftmes.bpi.infrastructure.postgres;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.application.ActorContext;
import com.mapletct.ftmes.bpi.application.error.BpiConflictException;
import com.mapletct.ftmes.bpi.application.error.BpiNotFoundException;
import com.mapletct.ftmes.bpi.domain.DatasetCatalogPublicationView;
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
public class DatasetCatalogPublicationPostgresRepository {
    private static final String SELECT = """
            SELECT publication.*, materialization.snapshot_id,
                   snapshot.dataset_id, snapshot.line_ids::text AS line_ids,
                   definition.dataset_code, definition.version AS dataset_version,
                   definition.plant_id
              FROM bpi.bpi_dataset_catalog_publications publication
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

    public DatasetCatalogPublicationPostgresRepository(
            NamedParameterJdbcTemplate jdbc,
            ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public void insert(
            ActorContext actor,
            UUID id,
            DatasetCatalogPublicationSource source,
            String catalogName,
            String catalogNamespace,
            String tableName,
            String publisherVersion,
            String reason) {
        try {
            jdbc.update("""
                    INSERT INTO bpi.bpi_dataset_catalog_publications
                        (id, tenant_id, materialization_id, source_snapshot_id,
                         catalog_name, catalog_namespace, table_name, table_identifier,
                         publisher_version, state, manifest_checksum,
                         source_content_sha256, source_object_version_id,
                         source_byte_size, source_row_count, source_schema_json,
                         requested_by, request_reason)
                    VALUES (:id, :tenantId, :materializationId, :snapshotId,
                            :catalogName, :catalogNamespace, :tableName, :tableIdentifier,
                            :publisherVersion, 'QUEUED', :manifestChecksum,
                            :sourceContentSha256, :sourceObjectVersionId,
                            :sourceByteSize, :sourceRowCount, CAST(:sourceSchema AS jsonb),
                            :requestedBy, :requestReason)
                    """, new MapSqlParameterSource()
                    .addValue("id", id)
                    .addValue("tenantId", actor.tenantId())
                    .addValue("materializationId", source.materializationId())
                    .addValue("snapshotId", source.snapshotId())
                    .addValue("catalogName", catalogName)
                    .addValue("catalogNamespace", catalogNamespace)
                    .addValue("tableName", tableName)
                    .addValue("tableIdentifier",
                            catalogName + "." + catalogNamespace + "." + tableName)
                    .addValue("publisherVersion", publisherVersion)
                    .addValue("manifestChecksum", source.manifestChecksum())
                    .addValue("sourceContentSha256", source.contentSha256())
                    .addValue("sourceObjectVersionId", source.objectVersionId())
                    .addValue("sourceByteSize", source.byteSize())
                    .addValue("sourceRowCount", source.rowCount())
                    .addValue("sourceSchema", writeJson(source.schema()))
                    .addValue("requestedBy", actor.userId())
                    .addValue("requestReason", reason));
        } catch (DataIntegrityViolationException exception) {
            throw new BpiConflictException(
                    "A catalog publication already exists for this materialization and publisher contract.",
                    null);
        }
    }

    public DatasetCatalogPublicationView find(ActorContext actor, UUID id) {
        StringBuilder sql = new StringBuilder(SELECT)
                .append(" WHERE publication.tenant_id = :tenantId")
                .append(" AND publication.id = :id");
        MapSqlParameterSource parameters = scoped(actor, sql)
                .addValue("tenantId", actor.tenantId())
                .addValue("id", id);
        try {
            DatasetCatalogPublicationView value = jdbc.queryForObject(
                    sql.toString(), parameters, (rs, rowIndex) -> map(rs));
            if (value == null) {
                throw new BpiNotFoundException("Dataset catalog publication not found.");
            }
            return value;
        } catch (EmptyResultDataAccessException exception) {
            throw new BpiNotFoundException("Dataset catalog publication not found.");
        }
    }

    public Optional<DatasetCatalogPublicationView> findByMaterialization(
            ActorContext actor,
            UUID materializationId,
            String catalogName,
            String publisherVersion) {
        StringBuilder sql = new StringBuilder(SELECT)
                .append(" WHERE publication.tenant_id = :tenantId")
                .append(" AND publication.materialization_id = :materializationId")
                .append(" AND publication.catalog_name = :catalogName")
                .append(" AND publication.publisher_version = :publisherVersion");
        MapSqlParameterSource parameters = scoped(actor, sql)
                .addValue("tenantId", actor.tenantId())
                .addValue("materializationId", materializationId)
                .addValue("catalogName", catalogName)
                .addValue("publisherVersion", publisherVersion);
        try {
            DatasetCatalogPublicationView value = jdbc.queryForObject(
                    sql.toString(), parameters, (rs, rowIndex) -> map(rs));
            return Optional.ofNullable(value);
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    @Transactional
    public DatasetCatalogPublicationView retry(
            ActorContext actor,
            UUID id,
            long expectedRevision) {
        DatasetCatalogPublicationView current = lock(actor, id);
        if (current.revision() != expectedRevision) {
            throw new BpiConflictException(
                    "Dataset catalog publication revision is stale.", current.revision());
        }
        if (!"FAILED".equals(current.state())) {
            throw new BpiConflictException(
                    "Only a FAILED dataset catalog publication can be retried.",
                    current.revision());
        }
        int updated = jdbc.update("""
                UPDATE bpi.bpi_dataset_catalog_publications
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
                    "Dataset catalog publication changed before retry.", current.revision());
        }
        return find(actor, id);
    }

    private DatasetCatalogPublicationView lock(ActorContext actor, UUID id) {
        StringBuilder sql = new StringBuilder(SELECT)
                .append(" WHERE publication.tenant_id = :tenantId")
                .append(" AND publication.id = :id");
        MapSqlParameterSource parameters = scoped(actor, sql)
                .addValue("tenantId", actor.tenantId())
                .addValue("id", id);
        sql.append(" FOR UPDATE OF publication");
        try {
            DatasetCatalogPublicationView value = jdbc.queryForObject(
                    sql.toString(), parameters, (rs, rowIndex) -> map(rs));
            if (value == null) {
                throw new BpiNotFoundException("Dataset catalog publication not found.");
            }
            return value;
        } catch (EmptyResultDataAccessException exception) {
            throw new BpiNotFoundException("Dataset catalog publication not found.");
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

    private DatasetCatalogPublicationView map(ResultSet rs) throws SQLException {
        return new DatasetCatalogPublicationView(
                rs.getObject("id", UUID.class),
                rs.getObject("materialization_id", UUID.class),
                rs.getObject("snapshot_id", UUID.class),
                rs.getObject("dataset_id", UUID.class),
                rs.getString("dataset_code"),
                rs.getString("dataset_version"),
                rs.getString("tenant_id"),
                rs.getString("plant_id"),
                readStrings(rs.getString("line_ids")),
                rs.getString("catalog_name"),
                rs.getString("catalog_namespace"),
                rs.getString("table_name"),
                rs.getString("table_identifier"),
                rs.getString("publisher_version"),
                rs.getString("state"),
                rs.getLong("revision"),
                rs.getString("manifest_checksum"),
                rs.getString("source_content_sha256"),
                rs.getString("source_object_version_id"),
                rs.getLong("source_byte_size"),
                rs.getLong("source_row_count"),
                readMap(rs.getString("source_schema_json")),
                rs.getString("requested_by"),
                rs.getString("request_reason"),
                instant(rs, "created_at"),
                instant(rs, "started_at"),
                instant(rs, "completed_at"),
                rs.getInt("attempt_count"),
                rs.getObject("iceberg_snapshot_id", Long.class),
                rs.getString("iceberg_metadata_location"),
                rs.getObject("iceberg_schema_id", Integer.class),
                rs.getObject("iceberg_partition_spec_id", Integer.class),
                rs.getObject("verified_row_count", Long.class),
                rs.getString("semantic_checksum"),
                readMap(rs.getString("catalog_metadata")),
                rs.getString("failure_code"),
                rs.getString("failure_detail"));
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize BPI catalog publication JSON", exception);
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
            throw new IllegalStateException("Could not read BPI catalog publication JSON", exception);
        }
    }
}
