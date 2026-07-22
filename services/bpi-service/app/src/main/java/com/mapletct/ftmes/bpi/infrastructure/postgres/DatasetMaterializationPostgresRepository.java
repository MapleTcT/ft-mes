package com.mapletct.ftmes.bpi.infrastructure.postgres;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.application.ActorContext;
import com.mapletct.ftmes.bpi.application.error.BpiConflictException;
import com.mapletct.ftmes.bpi.application.error.BpiNotFoundException;
import com.mapletct.ftmes.bpi.domain.DatasetMaterializationView;
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
import java.util.UUID;

@Repository
public class DatasetMaterializationPostgresRepository {
    private static final String SELECT = """
            SELECT materialization.*, snapshot.dataset_id, snapshot.line_ids::text AS line_ids,
                   definition.dataset_code, definition.version AS dataset_version,
                   definition.plant_id
              FROM bpi.bpi_dataset_materializations materialization
              JOIN bpi.bpi_dataset_snapshots snapshot
                ON snapshot.tenant_id = materialization.tenant_id
               AND snapshot.id = materialization.snapshot_id
              JOIN bpi.bpi_dataset_definitions definition
                ON definition.tenant_id = snapshot.tenant_id
               AND definition.id = snapshot.dataset_id
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public DatasetMaterializationPostgresRepository(
            NamedParameterJdbcTemplate jdbc,
            ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public void insert(
            ActorContext actor,
            UUID id,
            UUID snapshotId,
            String artifactFormat,
            String artifactSchemaVersion,
            String materializerVersion,
            String manifestChecksum,
            String reason) {
        try {
            jdbc.update("""
                    INSERT INTO bpi.bpi_dataset_materializations
                        (id, tenant_id, snapshot_id, artifact_format,
                         artifact_schema_version, materializer_version, state,
                         manifest_checksum, requested_by, request_reason)
                    VALUES (:id, :tenantId, :snapshotId, :artifactFormat,
                            :artifactSchemaVersion, :materializerVersion, 'QUEUED',
                            :manifestChecksum, :requestedBy, :requestReason)
                    """, new MapSqlParameterSource()
                    .addValue("id", id)
                    .addValue("tenantId", actor.tenantId())
                    .addValue("snapshotId", snapshotId)
                    .addValue("artifactFormat", artifactFormat)
                    .addValue("artifactSchemaVersion", artifactSchemaVersion)
                    .addValue("materializerVersion", materializerVersion)
                    .addValue("manifestChecksum", manifestChecksum)
                    .addValue("requestedBy", actor.userId())
                    .addValue("requestReason", reason));
        } catch (DataIntegrityViolationException exception) {
            throw new BpiConflictException(
                    "A materialization already exists for this snapshot and materializer contract.", null);
        }
    }

    public DatasetMaterializationView find(ActorContext actor, UUID id) {
        StringBuilder sql = new StringBuilder(SELECT)
                .append(" WHERE materialization.tenant_id = :tenantId")
                .append(" AND materialization.id = :id");
        MapSqlParameterSource parameters = scoped(actor, sql)
                .addValue("tenantId", actor.tenantId())
                .addValue("id", id);
        try {
            DatasetMaterializationView value = jdbc.queryForObject(
                    sql.toString(), parameters, (rs, rowNum) -> map(rs));
            if (value == null) throw new BpiNotFoundException("Dataset materialization not found.");
            return value;
        } catch (EmptyResultDataAccessException exception) {
            throw new BpiNotFoundException("Dataset materialization not found.");
        }
    }

    @Transactional
    public DatasetMaterializationView retry(
            ActorContext actor,
            UUID id,
            long expectedRevision) {
        DatasetMaterializationView current = lock(actor, id);
        if (current.revision() != expectedRevision) {
            throw new BpiConflictException(
                    "Dataset materialization revision is stale.", current.revision());
        }
        if (!"FAILED".equals(current.state())) {
            throw new BpiConflictException(
                    "Only a FAILED dataset materialization can be retried.", current.revision());
        }
        int updated = jdbc.update("""
                UPDATE bpi.bpi_dataset_materializations
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
                    "Dataset materialization changed before retry.", current.revision());
        }
        return find(actor, id);
    }

    private DatasetMaterializationView lock(ActorContext actor, UUID id) {
        StringBuilder sql = new StringBuilder(SELECT)
                .append(" WHERE materialization.tenant_id = :tenantId")
                .append(" AND materialization.id = :id");
        MapSqlParameterSource parameters = scoped(actor, sql)
                .addValue("tenantId", actor.tenantId())
                .addValue("id", id);
        sql.append(" FOR UPDATE OF materialization");
        try {
            DatasetMaterializationView value = jdbc.queryForObject(
                    sql.toString(), parameters, (rs, rowNum) -> map(rs));
            if (value == null) throw new BpiNotFoundException("Dataset materialization not found.");
            return value;
        } catch (EmptyResultDataAccessException exception) {
            throw new BpiNotFoundException("Dataset materialization not found.");
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

    private DatasetMaterializationView map(ResultSet rs) throws SQLException {
        return new DatasetMaterializationView(
                rs.getObject("id", UUID.class),
                rs.getObject("snapshot_id", UUID.class),
                rs.getObject("dataset_id", UUID.class),
                rs.getString("dataset_code"),
                rs.getString("dataset_version"),
                rs.getString("tenant_id"),
                rs.getString("plant_id"),
                readStrings(rs.getString("line_ids")),
                rs.getString("artifact_format"),
                rs.getString("artifact_schema_version"),
                rs.getString("materializer_version"),
                rs.getString("state"),
                rs.getLong("revision"),
                rs.getString("manifest_checksum"),
                rs.getString("requested_by"),
                rs.getString("request_reason"),
                instant(rs, "created_at"),
                instant(rs, "started_at"),
                instant(rs, "completed_at"),
                rs.getInt("attempt_count"),
                rs.getString("artifact_uri"),
                rs.getString("object_bucket"),
                rs.getString("object_key"),
                rs.getString("content_sha256"),
                rs.getObject("byte_size", Long.class),
                rs.getObject("row_count", Long.class),
                readMap(rs.getString("schema_json")),
                readMap(rs.getString("artifact_metadata")),
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
            throw new IllegalStateException("Could not serialize BPI dataset scope", exception);
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
            throw new IllegalStateException("Could not read BPI materialization JSON", exception);
        }
    }
}
