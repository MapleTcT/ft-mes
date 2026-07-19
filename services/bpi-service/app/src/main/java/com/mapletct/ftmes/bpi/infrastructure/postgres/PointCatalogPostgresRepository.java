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
            SELECT s.id, s.source, s.source_instance, s.source_revision, s.plant_id, s.line_id,
                   s.checksum, s.observed_at, s.point_count,
                   (SELECT count(*)::integer
                      FROM bpi.bpi_point_catalog_entries e
                     WHERE e.tenant_id = s.tenant_id
                       AND e.snapshot_id = s.id
                       AND e.registered
                       AND e.device_state = 'ACTIVE'
                       AND e.property_present
                       AND e.unit IS NOT NULL
                       AND btrim(e.unit) <> ''
                       AND e.source_sequence_enabled
                       AND e.calibration_version IS NOT NULL
                       AND btrim(e.calibration_version) <> ''
                       AND EXISTS (
                           SELECT 1
                             FROM bpi.bpi_point_calibrations c
                            WHERE c.tenant_id = e.tenant_id
                              AND c.plant_id = e.plant_id
                              AND c.line_id = e.line_id
                              AND c.product_id = e.product_id
                              AND c.device_id = e.device_id
                              AND c.property_id = e.property_id
                              AND c.calibration_version = e.calibration_version
                              AND c.state = 'APPROVED'
                              AND c.valid_from <= s.observed_at
                              AND c.valid_until > s.observed_at
                              AND c.valid_from <= CURRENT_TIMESTAMP
                              AND c.valid_until > CURRENT_TIMESTAMP
                       )) AS ready_point_count,
                   s.imported_by, s.imported_at
              FROM bpi.bpi_point_catalog_snapshots s
            """;
    private static final String POINT_SELECT = """
            SELECT e.id, e.snapshot_id, e.plant_id, e.line_id, e.locality_group, e.product_id, e.device_id,
                   e.property_id, e.source_property_id, e.point_name, e.unit, e.data_type, e.device_state,
                   e.registered, e.property_present, e.calibration_version,
                   e.calibration_status AS source_calibration_status,
                   CASE
                       WHEN c.id IS NOT NULL THEN 'VERIFIED'
                       WHEN e.calibration_version IS NULL OR btrim(e.calibration_version) = '' THEN 'MISSING'
                       ELSE 'UNVERIFIED'
                   END AS calibration_status,
                   c.id AS calibration_evidence_id,
                   c.valid_until AS calibration_valid_until,
                   e.source_sequence_enabled
              FROM bpi.bpi_point_catalog_entries e
              JOIN bpi.bpi_point_catalog_snapshots s
                ON s.tenant_id = e.tenant_id AND s.id = e.snapshot_id
              LEFT JOIN LATERAL (
                  SELECT calibration.id, calibration.valid_until
                    FROM bpi.bpi_point_calibrations calibration
                   WHERE calibration.tenant_id = e.tenant_id
                     AND calibration.plant_id = e.plant_id
                     AND calibration.line_id = e.line_id
                     AND calibration.product_id = e.product_id
                     AND calibration.device_id = e.device_id
                     AND calibration.property_id = e.property_id
                     AND calibration.calibration_version = e.calibration_version
                     AND calibration.state = 'APPROVED'
                     AND calibration.valid_from <= s.observed_at
                     AND calibration.valid_until > s.observed_at
                     AND calibration.valid_from <= CURRENT_TIMESTAMP
                     AND calibration.valid_until > CURRENT_TIMESTAMP
                   ORDER BY calibration.decided_at DESC, calibration.id
                   LIMIT 1
              ) c ON true
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public PointCatalogPostgresRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<PointCatalogSnapshotView> listSnapshots(
            ActorContext actor, String plantId, String lineId) {
        StringBuilder sql = new StringBuilder(SNAPSHOT_SELECT)
                .append(" WHERE s.tenant_id = :tenantId");
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
                         WHERE s.tenant_id = :tenantId
                           AND s.plant_id = :plantId
                           AND s.line_id = :lineId
                         ORDER BY s.observed_at DESC, s.imported_at DESC, s.id
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
                         WHERE e.tenant_id = :tenantId AND e.snapshot_id = :snapshotId
                         ORDER BY e.locality_group NULLS LAST, e.product_id, e.device_id, e.property_id
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
            int sourceClaimReadyPointCount,
            List<PointCatalogPointCommand> points) {
        try {
            jdbc.update("""
                    INSERT INTO bpi.bpi_point_catalog_snapshots
                        (id, tenant_id, source, source_instance, source_revision, plant_id, line_id,
                         checksum, observed_at, point_count, source_claim_ready_point_count, imported_by)
                    VALUES (:id, :tenantId, :source, :sourceInstance, :sourceRevision, :plantId, :lineId,
                            :checksum, :observedAt, :pointCount, :sourceClaimReadyPointCount, :actorId)
                    """, new MapSqlParameterSource("id", snapshotId)
                    .addValue("tenantId", actor.tenantId()).addValue("source", source)
                    .addValue("sourceInstance", sourceInstance).addValue("sourceRevision", sourceRevision)
                    .addValue("plantId", plantId).addValue("lineId", lineId).addValue("checksum", checksum)
                    .addValue("observedAt", Timestamp.from(observedAt)).addValue("pointCount", pointCount)
                    .addValue("sourceClaimReadyPointCount", sourceClaimReadyPointCount)
                    .addValue("actorId", actor.userId()));

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
                SNAPSHOT_SELECT + " WHERE s.tenant_id = :tenantId AND s.id = :id",
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
        String sourceCalibrationStatus = rs.getString("source_calibration_status");
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
                registered, propertyPresent, calibrationVersion, sourceCalibrationStatus,
                calibrationStatus, rs.getObject("calibration_evidence_id", UUID.class),
                instant(rs.getTimestamp("calibration_valid_until")),
                sourceSequenceEnabled, issues.isEmpty(), issues);
    }

    public static boolean isSourceClaimReady(PointCatalogPointCommand point) {
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

    private static Instant instant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }
}
