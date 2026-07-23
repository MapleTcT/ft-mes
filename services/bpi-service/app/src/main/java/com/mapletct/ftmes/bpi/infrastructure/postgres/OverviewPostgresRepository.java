package com.mapletct.ftmes.bpi.infrastructure.postgres;

import com.mapletct.ftmes.bpi.application.ActorContext;
import com.mapletct.ftmes.bpi.domain.LineIncidentSnapshot;
import com.mapletct.ftmes.bpi.domain.LineTelemetrySample;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Repository
public class OverviewPostgresRepository {

    private static final String LINE_SELECT = """
            WITH catalog_scope AS (
                SELECT DISTINCT ON (snapshot.tenant_id, snapshot.plant_id, snapshot.line_id)
                       snapshot.tenant_id, snapshot.plant_id, snapshot.line_id
                  FROM bpi.bpi_point_catalog_snapshots snapshot
                 WHERE snapshot.tenant_id = :tenantId
                 ORDER BY snapshot.tenant_id, snapshot.plant_id, snapshot.line_id,
                          snapshot.observed_at DESC, snapshot.imported_at DESC, snapshot.id DESC
            ),
            current_topology AS (
                SELECT DISTINCT ON (topology.tenant_id, topology.plant_id, topology.line_id)
                       topology.tenant_id, topology.plant_id, topology.line_id,
                       topology.id, topology.definition
                  FROM bpi.bpi_topology_versions topology
                 WHERE topology.tenant_id = :tenantId
                   AND topology.state = 'PUBLISHED'
                   AND topology.plant_id IS NOT NULL
                   AND topology.line_id IS NOT NULL
                 ORDER BY topology.tenant_id, topology.plant_id, topology.line_id,
                          topology.published_at DESC NULLS LAST,
                          topology.updated_at DESC, topology.id DESC
            ),
            topology_bindings AS (
                SELECT topology.tenant_id, topology.plant_id, topology.line_id,
                       binding.item, binding.ordinal
                  FROM current_topology topology
                  CROSS JOIN LATERAL jsonb_array_elements(
                      CASE WHEN jsonb_typeof(topology.definition -> 'bindings') = 'array'
                           THEN topology.definition -> 'bindings'
                           ELSE '[]'::jsonb
                      END
                  ) WITH ORDINALITY AS binding(item, ordinal)
                 WHERE CASE
                           WHEN jsonb_typeof(topology.definition -> 'requiredSignals') = 'array'
                            AND jsonb_array_length(topology.definition -> 'requiredSignals') > 0
                           THEN EXISTS (
                               SELECT 1
                                 FROM jsonb_array_elements_text(
                                     topology.definition -> 'requiredSignals'
                                 ) AS required(signal)
                                WHERE required.signal = binding.item ->> 'signal'
                           )
                           ELSE true
                       END
            ),
            primary_binding AS (
                SELECT DISTINCT ON (binding.tenant_id, binding.plant_id, binding.line_id)
                       binding.tenant_id, binding.plant_id, binding.line_id,
                       binding.item ->> 'signal' AS signal,
                       binding.item ->> 'productId' AS product_id,
                       binding.item ->> 'deviceId' AS device_id,
                       binding.item ->> 'propertyId' AS property_id,
                       COALESCE(
                           NULLIF(binding.item ->> 'expectedUnit', ''),
                           binding.item ->> 'unit'
                       ) AS expected_unit
                  FROM topology_bindings binding
                 ORDER BY binding.tenant_id, binding.plant_id, binding.line_id, binding.ordinal
            ),
            topology_coverage AS (
                SELECT binding.tenant_id, binding.plant_id, binding.line_id,
                       count(binding.item)::integer AS expected_signal_count,
                       count(latest.property_id) FILTER (
                           WHERE latest.sample_time >= :freshSince
                       )::integer AS observed_signal_count,
                       count(latest.property_id) FILTER (
                           WHERE latest.sample_time >= :freshSince
                             AND latest.quality_code = 'GOOD'
                             AND latest.sequence_disposition NOT IN ('GAP', 'OUT_OF_ORDER')
                       )::integer AS good_signal_count
                  FROM topology_bindings binding
                  LEFT JOIN bpi.bpi_telemetry_point_latest latest
                    ON latest.tenant_id = binding.tenant_id
                   AND latest.plant_id = binding.plant_id
                   AND latest.line_id = binding.line_id
                   AND latest.product_id = binding.item ->> 'productId'
                   AND latest.device_id = binding.item ->> 'deviceId'
                   AND latest.property_id = binding.item ->> 'propertyId'
                 GROUP BY binding.tenant_id, binding.plant_id, binding.line_id
            ),
            line_scope AS (
                SELECT tenant_id, plant_id, line_id FROM catalog_scope
                UNION
                SELECT tenant_id, plant_id, line_id FROM current_topology
                UNION
                SELECT latest.tenant_id, latest.plant_id, latest.line_id
                  FROM bpi.bpi_telemetry_point_latest latest
                 WHERE latest.tenant_id = :tenantId
                UNION
                SELECT candidate.tenant_id, candidate.plant_id, candidate.line_id
                  FROM bpi.bpi_batch_candidates candidate
                 WHERE candidate.tenant_id = :tenantId
                UNION
                SELECT batch.tenant_id, batch.plant_id, batch.line_id
                  FROM bpi.bpi_batch_instances batch
                 WHERE batch.tenant_id = :tenantId
            )
            SELECT line.plant_id,
                   line.line_id,
                   active_batch.id AS current_batch_id,
                   active_batch.order_id AS batch_order_id,
                   active_batch.stage_code,
                   active_batch.state AS batch_state,
                   active_batch.quantity AS totalized_quantity,
                   active_batch.start_time AS batch_start_time,
                   latest_candidate.order_id AS candidate_order_id,
                   latest_candidate.confidence,
                   latest_candidate.boundary_time AS candidate_time,
                   COALESCE(candidate_summary.pending_candidates, 0) AS pending_candidates,
                   primary_binding.property_id IS NOT NULL AS topology_bound,
                   COALESCE(primary_binding.signal, primary_latest.property_id) AS primary_signal,
                   COALESCE(primary_binding.product_id, primary_latest.product_id)
                       AS primary_product_id,
                   COALESCE(primary_binding.device_id, primary_latest.device_id)
                       AS primary_device_id,
                   COALESCE(primary_binding.property_id, primary_latest.property_id)
                       AS primary_property_id,
                   COALESCE(primary_binding.expected_unit, primary_latest.unit) AS expected_unit,
                   primary_latest.value_type,
                   primary_latest.numeric_value,
                   primary_latest.string_value,
                   primary_latest.boolean_value,
                   primary_latest.unit AS actual_unit,
                   primary_latest.quality_code,
                   primary_latest.sequence_origin,
                   primary_latest.sequence_disposition,
                   primary_latest.sample_time,
                   primary_latest.calibration_version,
                   COALESCE(topology_coverage.expected_signal_count, 0) AS expected_signal_count,
                   COALESCE(topology_coverage.observed_signal_count, 0) AS observed_signal_count,
                   COALESCE(topology_coverage.good_signal_count, 0) AS good_signal_count,
                   COALESCE(incident_summary.open_incident_count, 0) AS open_incident_count,
                   COALESCE(incident_summary.error_incident_count, 0) AS error_incident_count,
                   COALESCE(incident_summary.critical_incident_count, 0) AS critical_incident_count
              FROM line_scope line
              LEFT JOIN primary_binding
                ON primary_binding.tenant_id = line.tenant_id
               AND primary_binding.plant_id = line.plant_id
               AND primary_binding.line_id = line.line_id
              LEFT JOIN topology_coverage
                ON topology_coverage.tenant_id = line.tenant_id
               AND topology_coverage.plant_id = line.plant_id
               AND topology_coverage.line_id = line.line_id
              LEFT JOIN LATERAL (
                  SELECT latest.*
                    FROM bpi.bpi_telemetry_point_latest latest
                   WHERE latest.tenant_id = line.tenant_id
                     AND latest.plant_id = line.plant_id
                     AND latest.line_id = line.line_id
                     AND (
                         (
                             primary_binding.property_id IS NOT NULL
                             AND latest.product_id = primary_binding.product_id
                             AND latest.device_id = primary_binding.device_id
                             AND latest.property_id = primary_binding.property_id
                         )
                         OR primary_binding.property_id IS NULL
                     )
                   ORDER BY CASE
                                WHEN primary_binding.property_id IS NOT NULL
                                 AND latest.product_id = primary_binding.product_id
                                 AND latest.device_id = primary_binding.device_id
                                 AND latest.property_id = primary_binding.property_id THEN 0
                                ELSE 1
                            END,
                            latest.sample_time DESC,
                            latest.source_epoch DESC,
                            latest.sequence DESC
                   LIMIT 1
              ) primary_latest ON true
              LEFT JOIN LATERAL (
                  SELECT batch.id, batch.order_id, batch.stage_code, batch.state,
                         batch.quantity, batch.start_time
                    FROM bpi.bpi_batch_instances batch
                   WHERE batch.tenant_id = line.tenant_id
                     AND batch.plant_id = line.plant_id
                     AND batch.line_id = line.line_id
                     AND batch.state IN ('ACTIVE', 'SUSPENDED')
                   ORDER BY batch.start_time DESC, batch.id DESC
                   LIMIT 1
              ) active_batch ON true
              LEFT JOIN LATERAL (
                  SELECT candidate.order_id, candidate.confidence, candidate.boundary_time
                    FROM bpi.bpi_batch_candidates candidate
                   WHERE candidate.tenant_id = line.tenant_id
                     AND candidate.plant_id = line.plant_id
                     AND candidate.line_id = line.line_id
                   ORDER BY candidate.boundary_time DESC, candidate.id DESC
                   LIMIT 1
              ) latest_candidate ON true
              LEFT JOIN LATERAL (
                  SELECT count(*) FILTER (WHERE candidate.state = 'PENDING')::integer
                             AS pending_candidates
                    FROM bpi.bpi_batch_candidates candidate
                   WHERE candidate.tenant_id = line.tenant_id
                     AND candidate.plant_id = line.plant_id
                     AND candidate.line_id = line.line_id
              ) candidate_summary ON true
              LEFT JOIN LATERAL (
                  SELECT count(*)::integer AS open_incident_count,
                         count(*) FILTER (
                             WHERE incident.severity IN ('ERROR', 'CRITICAL')
                         )::integer AS error_incident_count,
                         count(*) FILTER (
                             WHERE incident.severity = 'CRITICAL'
                         )::integer AS critical_incident_count
                    FROM bpi.bpi_data_quality_incidents incident
                   WHERE incident.tenant_id = line.tenant_id
                     AND incident.plant_id = line.plant_id
                     AND incident.line_id = line.line_id
                     AND incident.state <> 'RESOLVED'
              ) incident_summary ON true
             WHERE 1 = 1
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public OverviewPostgresRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<LineProjection> listLines(
            ActorContext actor,
            String plantId,
            String lineId,
            Instant freshSince) {
        StringBuilder sql = new StringBuilder(LINE_SELECT);
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("tenantId", actor.tenantId())
                .addValue("freshSince", Timestamp.from(freshSince));
        if (plantId != null && !plantId.isBlank()) {
            sql.append(" AND line.plant_id = :plantId");
            parameters.addValue("plantId", plantId);
        }
        if (lineId != null && !lineId.isBlank()) {
            sql.append(" AND line.line_id = :lineId");
            parameters.addValue("lineId", lineId);
        }
        appendActorScope(sql, parameters, actor);
        sql.append(" ORDER BY line.line_id, line.plant_id");
        return jdbc.query(sql.toString(), parameters, (rs, rowNum) -> mapLine(rs));
    }

    public List<LineTelemetrySample> listSamples(
            ActorContext actor,
            LineProjection line,
            Instant windowStart,
            Instant windowEnd,
            int limit) {
        if (line.primaryProductId() == null
                || line.primaryDeviceId() == null
                || line.primaryPropertyId() == null) {
            return List.of();
        }
        List<LineTelemetrySample> samples = jdbc.query("""
                SELECT event.event_id,
                       point.value_type,
                       point.numeric_value,
                       point.string_value,
                       point.boolean_value,
                       point.unit,
                       point.quality_code,
                       event.sequence_disposition,
                       point.sample_time,
                       point.calibration_version
                  FROM bpi.bpi_telemetry_points point
                  JOIN bpi.bpi_telemetry_events event
                    ON event.tenant_id = point.tenant_id
                   AND event.id = point.telemetry_event_id
                 WHERE point.tenant_id = :tenantId
                   AND event.plant_id = :plantId
                   AND event.line_id = :lineId
                   AND event.product_id = :productId
                   AND event.device_id = :deviceId
                   AND point.property_id = :propertyId
                   AND point.sample_time >= :windowStart
                   AND point.sample_time <= :windowEnd
                 ORDER BY point.sample_time DESC, event.source_epoch DESC, event.sequence DESC
                 LIMIT :limit
                """, new MapSqlParameterSource()
                .addValue("tenantId", actor.tenantId())
                .addValue("plantId", line.plantId())
                .addValue("lineId", line.lineId())
                .addValue("productId", line.primaryProductId())
                .addValue("deviceId", line.primaryDeviceId())
                .addValue("propertyId", line.primaryPropertyId())
                .addValue("windowStart", Timestamp.from(windowStart))
                .addValue("windowEnd", Timestamp.from(windowEnd))
                .addValue("limit", limit),
                (rs, rowNum) -> new LineTelemetrySample(
                        rs.getString("event_id"),
                        line.primarySignal() == null ? line.primaryPropertyId() : line.primarySignal(),
                        value(rs),
                        rs.getBigDecimal("numeric_value"),
                        rs.getString("unit"),
                        rs.getString("quality_code"),
                        rs.getString("sequence_disposition"),
                        rs.getTimestamp("sample_time").toInstant(),
                        rs.getString("calibration_version")));
        List<LineTelemetrySample> chronological = new ArrayList<>(samples);
        Collections.reverse(chronological);
        return List.copyOf(chronological);
    }

    public List<LineIncidentSnapshot> listOpenIncidents(
            ActorContext actor,
            String plantId,
            String lineId,
            int limit) {
        return jdbc.query("""
                SELECT issue_code, severity, state, event_count, last_seen, last_detail
                  FROM bpi.bpi_data_quality_incidents
                 WHERE tenant_id = :tenantId
                   AND plant_id = :plantId
                   AND line_id = :lineId
                   AND state <> 'RESOLVED'
                 ORDER BY CASE severity
                              WHEN 'CRITICAL' THEN 4
                              WHEN 'ERROR' THEN 3
                              WHEN 'WARNING' THEN 2
                              ELSE 1
                          END DESC,
                          last_seen DESC,
                          id DESC
                 LIMIT :limit
                """, new MapSqlParameterSource()
                .addValue("tenantId", actor.tenantId())
                .addValue("plantId", plantId)
                .addValue("lineId", lineId)
                .addValue("limit", limit),
                (rs, rowNum) -> new LineIncidentSnapshot(
                        rs.getString("issue_code"),
                        rs.getString("severity"),
                        rs.getString("state"),
                        rs.getLong("event_count"),
                        rs.getTimestamp("last_seen").toInstant(),
                        rs.getString("last_detail")));
    }

    private void appendActorScope(
            StringBuilder sql,
            MapSqlParameterSource parameters,
            ActorContext actor) {
        if (!actor.plantIds().contains("*")) {
            if (actor.plantIds().isEmpty()) {
                sql.append(" AND 1 = 0");
            } else {
                sql.append(" AND line.plant_id IN (:allowedPlants)");
                parameters.addValue("allowedPlants", actor.plantIds());
            }
        }
        if (!actor.lineIds().contains("*")) {
            if (actor.lineIds().isEmpty()) {
                sql.append(" AND 1 = 0");
            } else {
                sql.append(" AND line.line_id IN (:allowedLines)");
                parameters.addValue("allowedLines", actor.lineIds());
            }
        }
    }

    private LineProjection mapLine(ResultSet rs) throws SQLException {
        return new LineProjection(
                rs.getString("plant_id"),
                rs.getString("line_id"),
                rs.getObject("current_batch_id", UUID.class),
                rs.getString("batch_order_id"),
                rs.getString("stage_code"),
                rs.getString("batch_state"),
                rs.getBigDecimal("totalized_quantity"),
                instant(rs, "batch_start_time"),
                rs.getString("candidate_order_id"),
                rs.getBigDecimal("confidence"),
                instant(rs, "candidate_time"),
                rs.getInt("pending_candidates"),
                rs.getBoolean("topology_bound"),
                rs.getString("primary_signal"),
                rs.getString("primary_product_id"),
                rs.getString("primary_device_id"),
                rs.getString("primary_property_id"),
                rs.getString("expected_unit"),
                rs.getString("value_type"),
                rs.getBigDecimal("numeric_value"),
                rs.getString("string_value"),
                nullableBoolean(rs, "boolean_value"),
                rs.getString("actual_unit"),
                rs.getString("quality_code"),
                rs.getString("sequence_origin"),
                rs.getString("sequence_disposition"),
                instant(rs, "sample_time"),
                rs.getString("calibration_version"),
                rs.getInt("expected_signal_count"),
                rs.getInt("observed_signal_count"),
                rs.getInt("good_signal_count"),
                rs.getInt("open_incident_count"),
                rs.getInt("error_incident_count"),
                rs.getInt("critical_incident_count"));
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private static Boolean nullableBoolean(ResultSet rs, String column) throws SQLException {
        boolean value = rs.getBoolean(column);
        return rs.wasNull() ? null : value;
    }

    private static String value(ResultSet rs) throws SQLException {
        String valueType = rs.getString("value_type");
        if ("DOUBLE".equals(valueType) || "LONG".equals(valueType)) {
            BigDecimal value = rs.getBigDecimal("numeric_value");
            return value == null ? null : value.stripTrailingZeros().toPlainString();
        }
        if ("BOOLEAN".equals(valueType)) {
            Boolean value = nullableBoolean(rs, "boolean_value");
            return value == null ? null : value.toString();
        }
        return rs.getString("string_value");
    }

    public record LineProjection(
            String plantId,
            String lineId,
            UUID currentBatchId,
            String batchOrderId,
            String stageCode,
            String batchState,
            BigDecimal totalizedQuantity,
            Instant batchStartTime,
            String candidateOrderId,
            BigDecimal confidence,
            Instant candidateTime,
            int pendingCandidates,
            boolean topologyBound,
            String primarySignal,
            String primaryProductId,
            String primaryDeviceId,
            String primaryPropertyId,
            String expectedUnit,
            String valueType,
            BigDecimal numericValue,
            String stringValue,
            Boolean booleanValue,
            String actualUnit,
            String qualityCode,
            String sequenceOrigin,
            String sequenceDisposition,
            Instant sampleTime,
            String calibrationVersion,
            int expectedSignalCount,
            int observedSignalCount,
            int goodSignalCount,
            int openIncidentCount,
            int errorIncidentCount,
            int criticalIncidentCount) {

        public String value() {
            if ("DOUBLE".equals(valueType) || "LONG".equals(valueType)) {
                return numericValue == null ? null : numericValue.stripTrailingZeros().toPlainString();
            }
            if ("BOOLEAN".equals(valueType)) {
                return booleanValue == null ? null : booleanValue.toString();
            }
            return stringValue;
        }

    }
}
