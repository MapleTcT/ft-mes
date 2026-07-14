package com.mapletct.ftmes.bpi.infrastructure.postgres;

import com.mapletct.ftmes.bpi.application.ActorContext;
import com.mapletct.ftmes.bpi.application.error.BpiConflictException;
import com.mapletct.ftmes.bpi.application.error.BpiNotFoundException;
import com.mapletct.ftmes.bpi.domain.PointCatalogPointView;
import com.mapletct.ftmes.bpi.domain.PointCatalogSnapshotView;
import com.mapletct.ftmes.bpi.interfaces.rest.PointCatalogPointCommand;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PointCatalogPostgresRepository {
    private static final String SNAPSHOT_SELECT = """
            SELECT id, source, source_instance, source_revision, plant_id, line_id,
                   checksum, observed_at, point_count, ready_point_count, imported_by, imported_at
              FROM bpi.bpi_point_catalog_snapshots
            """;
    private static final String POINT_SELECT = """
            SELECT id, snapshot_id, plant_id, line_id, locality_group, product_id, device_id,
                   property_id, source_property_id, point_name, unit, data_type, device_state, registered,
                   property_present, calibration_version, calibration_status, source_sequence_enabled
              FROM bpi.bpi_point_catalog_entries
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public PointCatalogPostgresRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<PointCatalogSnapshotView> listSnapshots(
            ActorContext actor, String plantId, String lineId) {
        StringBuilder sql = new StringBuilder(SNAPSHOT_SELECT)
                .append(" WHERE tenant_id = :tenantId");
        MapSqlParameterSource parameters = new MapSqlParameterSource("tenantId", actor.tenantId());
        addScope(sql, parameters, actor, plantId, lineId);
        sql.append(" ORDER BY observed_at DESC, imported_at DESC, id");
        return jdbc.query(sql.toString(), parameters, (rs, rowNum) -> mapSnapshot(rs));
    }

    public Optional<PointCatalogSnapshotView> findCurrentSnapshot(
            ActorContext actor, String plantId, String lineId) {
        if (!actor.canAccess(plantId, lineId)) return Optional.empty();
        List<PointCatalogSnapshotView> snapshots = jdbc.query(
                SNAPSHOT_SELECT + """
                         WHERE tenant_id = :tenantId
                           AND plant_id = :plantId
                           AND line_id = :lineId
                         ORDER BY observed_at DESC, imported_at DESC, id
                         LIMIT 1
                        """,
                new MapSqlParameterSource("tenantId", actor.tenantId())
                        .addValue("plantId", plantId).addValue("lineId", lineId),
                (rs, rowNum) -> mapSnapshot(rs));
        return snapshots.stream().findFirst();
    }

    public List<PointCatalogPointView> listPoints(
            ActorContext actor, PointCatalogSnapshotView snapshot) {
        if (!actor.canAccess(snapshot.plantId(), snapshot.lineId())) {
            throw new BpiNotFoundException("Point catalog snapshot not found.");
        }
        return jdbc.query(
                POINT_SELECT + """
                         WHERE tenant_id = :tenantId AND snapshot_id = :snapshotId
                         ORDER BY locality_group NULLS LAST, product_id, device_id, property_id
                        """,
                new MapSqlParameterSource("tenantId", actor.tenantId())
                        .addValue("snapshotId", snapshot.id()),
                (rs, rowNum) -> mapPoint(rs));
    }

    public void insertSnapshot(
            ActorContext actor,
            UUID snapshotId,
            String source,
            String sourceInstance,
            String sourceRevision,
            String plantId,
            String lineId,
            String checksum,
            Instant observedAt,
            int pointCount,
            int readyPointCount,
            List<PointCatalogPointCommand> points) {
        try {
            jdbc.update("""
                    INSERT INTO bpi.bpi_point_catalog_snapshots
                        (id, tenant_id, source, source_instance, source_revision, plant_id, line_id,
                         checksum, observed_at, point_count, ready_point_count, imported_by)
                    VALUES (:id, :tenantId, :source, :sourceInstance, :sourceRevision, :plantId, :lineId,
                            :checksum, :observedAt, :pointCount, :readyPointCount, :actorId)
                    """, new MapSqlParameterSource("id", snapshotId)
                    .addValue("tenantId", actor.tenantId()).addValue("source", source)
                    .addValue("sourceInstance", sourceInstance).addValue("sourceRevision", sourceRevision)
                    .addValue("plantId", plantId).addValue("lineId", lineId).addValue("checksum", checksum)
                    .addValue("observedAt", Timestamp.from(observedAt)).addValue("pointCount", pointCount)
                    .addValue("readyPointCount", readyPointCount).addValue("actorId", actor.userId()));

            if (!points.isEmpty()) {
                MapSqlParameterSource[] batch = points.stream()
                        .map(point -> pointParameters(actor, snapshotId, plantId, lineId, point))
                        .toArray(MapSqlParameterSource[]::new);
                jdbc.batchUpdate("""
                        INSERT INTO bpi.bpi_point_catalog_entries
                            (id, tenant_id, snapshot_id, plant_id, line_id, locality_group,
                             product_id, device_id, property_id, point_name, unit, data_type,
                             source_property_id, device_state, registered, property_present, calibration_version,
                             calibration_status, source_sequence_enabled)
                        VALUES (:id, :tenantId, :snapshotId, :plantId, :lineId, :localityGroup,
                                :productId, :deviceId, :propertyId, :pointName, :unit, :dataType,
                                :sourcePropertyId, :deviceState, :registered, :propertyPresent, :calibrationVersion,
                                :calibrationStatus, :sourceSequenceEnabled)
                        """, batch);
            }
        } catch (DataIntegrityViolationException exception) {
            throw new BpiConflictException(
                    "Point catalog source revision or point identity already exists.", null);
        }
    }

    public PointCatalogSnapshotView findSnapshot(ActorContext actor, UUID snapshotId) {
        List<PointCatalogSnapshotView> snapshots = jdbc.query(
                SNAPSHOT_SELECT + " WHERE tenant_id = :tenantId AND id = :id",
                new MapSqlParameterSource("tenantId", actor.tenantId()).addValue("id", snapshotId),
                (rs, rowNum) -> mapSnapshot(rs));
        PointCatalogSnapshotView snapshot = snapshots.stream().findFirst()
                .orElseThrow(() -> new BpiNotFoundException("Point catalog snapshot not found."));
        if (!actor.canAccess(snapshot.plantId(), snapshot.lineId())) {
            throw new BpiNotFoundException("Point catalog snapshot not found.");
        }
        return snapshot;
    }

    public void insertAudit(
            ActorContext actor,
            PointCatalogSnapshotView snapshot,
            String reason,
            String traceId) {
        jdbc.update("""
                INSERT INTO bpi.bpi_audit_events
                    (id, tenant_id, plant_id, line_id, object_type, object_id, action, actor_id,
                     before_revision, after_revision, reason, trace_id, detail)
                VALUES (:id, :tenantId, :plantId, :lineId, 'POINT_CATALOG_SNAPSHOT', :objectId,
                        'POINT_CATALOG_SNAPSHOT_IMPORTED', :actorId, 0, 1, :reason, :traceId,
                        jsonb_build_object('checksum', :checksum, 'source', :source,
                                           'sourceRevision', :sourceRevision,
                                           'pointCount', :pointCount,
                                           'readyPointCount', :readyPointCount))
                """, new MapSqlParameterSource("id", UUID.randomUUID())
                .addValue("tenantId", actor.tenantId()).addValue("plantId", snapshot.plantId())
                .addValue("lineId", snapshot.lineId()).addValue("objectId", snapshot.id())
                .addValue("actorId", actor.userId()).addValue("reason", reason)
                .addValue("traceId", traceId).addValue("checksum", snapshot.checksum())
                .addValue("source", snapshot.source()).addValue("sourceRevision", snapshot.sourceRevision())
                .addValue("pointCount", snapshot.pointCount())
                .addValue("readyPointCount", snapshot.readyPointCount()));
    }

    private MapSqlParameterSource pointParameters(
            ActorContext actor,
            UUID snapshotId,
            String plantId,
            String lineId,
            PointCatalogPointCommand point) {
        return new MapSqlParameterSource("id", UUID.randomUUID())
                .addValue("tenantId", actor.tenantId()).addValue("snapshotId", snapshotId)
                .addValue("plantId", plantId).addValue("lineId", lineId)
                .addValue("localityGroup", blankToNull(point.localityGroup()))
                .addValue("productId", point.productId()).addValue("deviceId", point.deviceId())
                .addValue("propertyId", point.propertyId())
                .addValue("sourcePropertyId", blankToNull(point.sourcePropertyId()))
                .addValue("pointName", blankToNull(point.pointName()))
                .addValue("unit", blankToNull(point.unit())).addValue("dataType", blankToNull(point.dataType()))
                .addValue("deviceState", point.deviceState()).addValue("registered", point.registered())
                .addValue("propertyPresent", point.propertyPresent())
                .addValue("calibrationVersion", blankToNull(point.calibrationVersion()))
                .addValue("calibrationStatus", point.calibrationStatus())
                .addValue("sourceSequenceEnabled", point.sourceSequenceEnabled());
    }

    private PointCatalogSnapshotView mapSnapshot(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new PointCatalogSnapshotView(
                rs.getObject("id", UUID.class), rs.getString("source"), rs.getString("source_instance"),
                rs.getString("source_revision"), rs.getString("plant_id"), rs.getString("line_id"),
                rs.getString("checksum"), rs.getTimestamp("observed_at").toInstant(),
                rs.getInt("point_count"), rs.getInt("ready_point_count"), rs.getString("imported_by"),
                rs.getTimestamp("imported_at").toInstant());
    }

    private PointCatalogPointView mapPoint(java.sql.ResultSet rs) throws java.sql.SQLException {
        boolean registered = rs.getBoolean("registered");
        boolean propertyPresent = rs.getBoolean("property_present");
        String deviceState = rs.getString("device_state");
        String unit = rs.getString("unit");
        String calibrationVersion = rs.getString("calibration_version");
        String calibrationStatus = rs.getString("calibration_status");
        boolean sourceSequenceEnabled = rs.getBoolean("source_sequence_enabled");
        List<String> issues = readinessIssues(
                registered, propertyPresent, deviceState, unit, calibrationVersion, calibrationStatus,
                sourceSequenceEnabled);
        return new PointCatalogPointView(
                rs.getObject("id", UUID.class), rs.getObject("snapshot_id", UUID.class),
                rs.getString("plant_id"), rs.getString("line_id"), rs.getString("locality_group"),
                rs.getString("product_id"), rs.getString("device_id"), rs.getString("property_id"),
                rs.getString("source_property_id"), rs.getString("point_name"), unit,
                rs.getString("data_type"), deviceState,
                registered, propertyPresent, calibrationVersion, calibrationStatus,
                sourceSequenceEnabled, issues.isEmpty(), issues);
    }

    public static boolean isReady(PointCatalogPointCommand point) {
        return readinessIssues(
                point.registered(), point.propertyPresent(), point.deviceState(), point.unit(),
                point.calibrationVersion(), point.calibrationStatus(), point.sourceSequenceEnabled()).isEmpty();
    }

    private static List<String> readinessIssues(
            boolean registered,
            boolean propertyPresent,
            String deviceState,
            String unit,
            String calibrationVersion,
            String calibrationStatus,
            boolean sourceSequenceEnabled) {
        List<String> issues = new ArrayList<>();
        if (!registered) issues.add("DEVICE_NOT_REGISTERED");
        if (!"ACTIVE".equals(deviceState)) issues.add("DEVICE_NOT_ACTIVE");
        if (!propertyPresent) issues.add("PROPERTY_NOT_AVAILABLE");
        if (unit == null || unit.isBlank()) issues.add("UNIT_MISSING");
        if (calibrationVersion == null || calibrationVersion.isBlank()
                || !"VERIFIED".equals(calibrationStatus)) {
            issues.add("CALIBRATION_NOT_VERIFIED");
        }
        if (!sourceSequenceEnabled) issues.add("SOURCE_SEQUENCE_DISABLED");
        return List.copyOf(issues);
    }

    private void addScope(
            StringBuilder sql,
            MapSqlParameterSource parameters,
            ActorContext actor,
            String plantId,
            String lineId) {
        if (plantId != null) {
            sql.append(" AND plant_id = :plantId");
            parameters.addValue("plantId", plantId);
        }
        if (lineId != null) {
            sql.append(" AND line_id = :lineId");
            parameters.addValue("lineId", lineId);
        }
        if (!actor.plantIds().contains("*")) {
            if (actor.plantIds().isEmpty()) sql.append(" AND 1 = 0");
            else {
                sql.append(" AND plant_id IN (:actorPlantIds)");
                parameters.addValue("actorPlantIds", actor.plantIds());
            }
        }
        if (!actor.lineIds().contains("*")) {
            if (actor.lineIds().isEmpty()) sql.append(" AND 1 = 0");
            else {
                sql.append(" AND line_id IN (:actorLineIds)");
                parameters.addValue("actorLineIds", actor.lineIds());
            }
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
