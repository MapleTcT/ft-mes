package com.mapletct.ftmes.bpi.infrastructure.postgres;

import com.mapletct.ftmes.bpi.domain.DatasetManifestClaim;
import com.mapletct.ftmes.bpi.domain.ProcessSignalWindowDefinition;
import com.mapletct.ftmes.bpi.domain.ProcessSignalWindowEvidence;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class ProcessSignalWindowPostgresRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public ProcessSignalWindowPostgresRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<ProcessSignalWindowEvidence> findEvidence(
            DatasetManifestClaim claim,
            int limit) {
        if (claim.processSignalWindows().isEmpty()) return List.of();
        Map<String, ProcessSignalWindowDefinition> definitions = new LinkedHashMap<>();
        claim.processSignalWindows().forEach(
                definition -> definitions.put(definition.featureRef(), definition));

        StringBuilder sql = new StringBuilder("""
                WITH selected_sources AS (
                    SELECT review.id AS review_id, review.shadow_run_id, review.batch_id,
                           batch.batch_no, batch.plant_id, batch.line_id,
                           batch.rule_version_id, batch.topology_version_id,
                           run.point_catalog_snapshot_id,
                           review.automatic_start_time
                      FROM bpi.bpi_shadow_runs run
                      JOIN bpi.bpi_shadow_run_batch_reviews review
                        ON review.tenant_id = run.tenant_id
                       AND review.shadow_run_id = run.id
                      JOIN bpi.bpi_batch_instances batch
                        ON batch.tenant_id = review.tenant_id
                       AND batch.id = review.batch_id
                       AND batch.plant_id = run.plant_id
                       AND batch.line_id = run.line_id
                     WHERE run.tenant_id = :tenantId
                       AND run.state = 'APPROVED'
                       AND run.decided_at <= :freezeAt
                       AND run.line_id IN (:lineIds)
                       AND review.reviewed_at <= :freezeAt
                       AND (review.state = 'ACTIVE' OR review.superseded_at > :freezeAt)
                       AND run.plant_id = (
                           SELECT plant_id
                             FROM bpi.bpi_dataset_definitions
                            WHERE tenant_id = :tenantId AND id = :datasetId)
                """);
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("tenantId", claim.tenantId())
                .addValue("datasetId", claim.datasetId())
                .addValue("freezeAt", Timestamp.from(claim.freezeAt()))
                .addValue("lineIds", claim.lineIds())
                .addValue("limit", limit);
        if (!claim.ruleVersionIds().isEmpty()) {
            sql.append(" AND run.rule_version_id IN (:ruleVersionIds)");
            parameters.addValue("ruleVersionIds", claim.ruleVersionIds());
        }
        sql.append("""
                     ORDER BY run.line_id, review.automatic_start_time, review.batch_id, review.id
                     LIMIT :limit
                ),
                window_specs AS (
                    SELECT spec ->> 'featureRef' AS feature_ref,
                           spec ->> 'signal' AS signal,
                           spec ->> 'valueType' AS value_type,
                           spec ->> 'metric' AS metric,
                           (spec ->> 'startOffsetSeconds')::integer AS start_offset_seconds,
                           (spec ->> 'endOffsetSeconds')::integer AS end_offset_seconds,
                           (spec ->> 'minimumSamples')::integer AS minimum_samples,
                           (spec ->> 'maximumGapSeconds')::integer AS maximum_gap_seconds,
                           spec ->> 'expectedUnit' AS expected_unit,
                           (spec ->> 'requireCalibration')::boolean AS require_calibration,
                           spec -> 'acceptedQualityCodes' AS accepted_quality_codes,
                           spec ->> 'checksum' AS window_definition_checksum
                      FROM bpi.bpi_dataset_definitions definition
                      CROSS JOIN LATERAL
                           jsonb_array_elements(definition.process_signal_windows) spec
                     WHERE definition.tenant_id = :tenantId
                       AND definition.id = :datasetId
                ),
                source_windows AS (
                    SELECT source.*, spec.*,
                           source.automatic_start_time AS prediction_time,
                           source.automatic_start_time
                               + spec.start_offset_seconds * interval '1 second'
                               AS window_start,
                           source.automatic_start_time
                               + spec.end_offset_seconds * interval '1 second'
                               AS window_end
                      FROM selected_sources source
                      CROSS JOIN window_specs spec
                ),
                bound_windows AS (
                    SELECT source_window.*,
                           COALESCE(binding.binding_count, 0)::integer AS binding_count,
                           binding.binding_expected_unit,
                           binding.binding_calibration_version,
                           binding.product_id, binding.device_id, binding.property_id,
                           catalog.unit AS point_catalog_unit,
                           catalog.calibration_version AS point_catalog_calibration_version,
                           catalog.device_state AS point_catalog_device_state,
                           catalog.registered AS point_catalog_registered,
                           catalog.property_present AS point_catalog_property_present,
                           catalog.calibration_status AS point_catalog_calibration_status
                      FROM source_windows source_window
                      JOIN bpi.bpi_topology_versions topology
                        ON topology.tenant_id = :tenantId
                       AND topology.id = source_window.topology_version_id
                      LEFT JOIN LATERAL (
                          SELECT count(*)::integer AS binding_count,
                                 max(COALESCE(NULLIF(item ->> 'expectedUnit', ''),
                                              item ->> 'unit')) AS binding_expected_unit,
                                 max(item ->> 'calibrationVersion')
                                     AS binding_calibration_version,
                                 max(item ->> 'productId') AS product_id,
                                 max(item ->> 'deviceId') AS device_id,
                                 max(item ->> 'propertyId') AS property_id
                            FROM jsonb_array_elements(
                                 COALESCE(topology.definition -> 'bindings', '[]'::jsonb)) item
                           WHERE item ->> 'signal' = source_window.signal
                      ) binding ON true
                      LEFT JOIN bpi.bpi_point_catalog_entries catalog
                        ON catalog.tenant_id = :tenantId
                       AND catalog.snapshot_id = source_window.point_catalog_snapshot_id
                       AND catalog.product_id = binding.product_id
                       AND catalog.device_id = binding.device_id
                       AND catalog.property_id = binding.property_id
                ),
                raw_points AS (
                    SELECT bound.*,
                           point.id AS point_row_id,
                           point.event_id AS point_event_id,
                           point.value_type AS observed_value_type,
                           point.numeric_value, point.boolean_value,
                           point.unit AS observed_unit,
                           point.quality_code,
                           point.sample_time,
                           point.calibration_version AS observed_calibration_version,
                           event.id AS event_row_id,
                           event.ingest_time,
                           event.event_id AS source_event_id
                      FROM bound_windows bound
                      LEFT JOIN bpi.bpi_telemetry_points point
                        ON point.tenant_id = :tenantId
                       AND point.property_id = bound.property_id
                       AND point.sample_time >= bound.window_start
                       AND point.sample_time <= bound.window_end
                      LEFT JOIN bpi.bpi_telemetry_events event
                        ON event.tenant_id = point.tenant_id
                       AND event.id = point.telemetry_event_id
                       AND event.plant_id = bound.plant_id
                       AND event.line_id = bound.line_id
                       AND event.product_id = bound.product_id
                       AND event.device_id = bound.device_id
                       AND event.ingest_time <= :freezeAt
                ),
                point_flags AS (
                    SELECT raw.*,
                           event_row_id IS NOT NULL AS physical_point_match,
                           COALESCE(
                               jsonb_exists(accepted_quality_codes, quality_code),
                               false)
                               AS quality_accepted,
                           COALESCE(observed_unit = expected_unit, false) AS unit_accepted,
                           COALESCE(
                               (value_type = 'NUMERIC'
                                   AND observed_value_type IN ('DOUBLE', 'LONG')
                                   AND numeric_value IS NOT NULL)
                               OR
                               (value_type = 'BOOLEAN'
                                   AND observed_value_type = 'BOOLEAN'
                                   AND boolean_value IS NOT NULL),
                               false) AS value_type_accepted,
                           COALESCE(
                               NOT require_calibration
                               OR (
                                   observed_calibration_version = binding_calibration_version
                                   AND point_catalog_calibration_version
                                       = binding_calibration_version
                                   AND EXISTS (
                                       SELECT 1
                                         FROM bpi.bpi_point_calibrations calibration
                                        WHERE calibration.tenant_id = :tenantId
                                          AND calibration.plant_id = raw.plant_id
                                          AND calibration.line_id = raw.line_id
                                          AND calibration.product_id = raw.product_id
                                          AND calibration.device_id = raw.device_id
                                          AND calibration.property_id = raw.property_id
                                          AND calibration.calibration_version
                                              = raw.binding_calibration_version
                                          AND calibration.state = 'APPROVED'
                                          AND raw.sample_time >= calibration.valid_from
                                          AND raw.sample_time < calibration.valid_until
                                   )
                               ),
                               false) AS calibration_accepted,
                           COALESCE(ingest_time <= prediction_time, false)
                               AS available_at_prediction
                      FROM raw_points raw
                ),
                raw_stats AS (
                    SELECT review_id, feature_ref,
                           count(*) FILTER (WHERE physical_point_match)::integer
                               AS source_point_count,
                           count(*) FILTER (
                               WHERE physical_point_match AND NOT quality_accepted)::integer
                               AS rejected_quality_count,
                           count(*) FILTER (
                               WHERE physical_point_match
                                 AND NOT available_at_prediction)::integer
                               AS late_availability_count,
                           count(*) FILTER (
                               WHERE physical_point_match AND NOT unit_accepted)::integer
                               AS unit_mismatch_count,
                           count(*) FILTER (
                               WHERE physical_point_match
                                 AND NOT value_type_accepted)::integer
                               AS value_type_mismatch_count,
                           count(*) FILTER (
                               WHERE physical_point_match
                                 AND NOT calibration_accepted)::integer
                               AS calibration_mismatch_count,
                           md5(COALESCE(string_agg(
                               concat_ws('|',
                                   source_event_id, point_row_id::text,
                                   sample_time::text, ingest_time::text,
                                   observed_value_type,
                                   numeric_value::text, boolean_value::text,
                                   observed_unit, quality_code,
                                   observed_calibration_version),
                               '||' ORDER BY sample_time, point_row_id)
                               FILTER (WHERE physical_point_match), ''))
                               AS source_fingerprint
                      FROM point_flags
                     GROUP BY review_id, feature_ref
                ),
                accepted_points AS (
                    SELECT *
                      FROM point_flags
                     WHERE physical_point_match
                       AND quality_accepted
                       AND unit_accepted
                       AND value_type_accepted
                       AND calibration_accepted
                       AND available_at_prediction
                       AND sample_time <= prediction_time
                ),
                accepted_ordered AS (
                    SELECT accepted.*,
                           lag(sample_time) OVER (
                               PARTITION BY review_id, feature_ref
                               ORDER BY sample_time, point_row_id) AS previous_sample_time
                      FROM accepted_points accepted
                ),
                accepted_stats AS (
                    SELECT review_id, feature_ref,
                           count(*)::integer AS accepted_sample_count,
                           min(sample_time) AS first_sample_time,
                           max(sample_time) AS last_sample_time,
                           max(ingest_time) AS latest_ingest_time,
                           max(EXTRACT(EPOCH FROM
                               (sample_time - previous_sample_time)))::numeric(18,6)
                               AS maximum_internal_gap_seconds,
                           avg(numeric_value) AS mean_value,
                           min(numeric_value) AS minimum_value,
                           max(numeric_value) AS maximum_value,
                           (array_agg(numeric_value
                               ORDER BY sample_time, point_row_id))[1] AS first_value,
                           (array_agg(numeric_value
                               ORDER BY sample_time DESC, point_row_id DESC))[1] AS last_value,
                           regr_slope(
                               numeric_value::double precision,
                               EXTRACT(EPOCH FROM sample_time)::double precision
                           )::numeric AS slope_value,
                           avg(CASE
                               WHEN boolean_value THEN 1::numeric
                               ELSE 0::numeric
                           END) FILTER (WHERE boolean_value IS NOT NULL)
                               AS true_ratio_value
                      FROM accepted_ordered
                     GROUP BY review_id, feature_ref
                )
                SELECT bound.*,
                       COALESCE(raw.source_point_count, 0) AS source_point_count,
                       COALESCE(accepted.accepted_sample_count, 0)
                           AS accepted_sample_count,
                       COALESCE(raw.rejected_quality_count, 0)
                           AS rejected_quality_count,
                       COALESCE(raw.late_availability_count, 0)
                           AS late_availability_count,
                       COALESCE(raw.unit_mismatch_count, 0)
                           AS unit_mismatch_count,
                       COALESCE(raw.value_type_mismatch_count, 0)
                           AS value_type_mismatch_count,
                       COALESCE(raw.calibration_mismatch_count, 0)
                           AS calibration_mismatch_count,
                       accepted.first_sample_time, accepted.last_sample_time,
                       accepted.latest_ingest_time,
                       CASE WHEN accepted.accepted_sample_count > 0 THEN
                           GREATEST(
                               EXTRACT(EPOCH FROM
                                   (accepted.first_sample_time - bound.window_start)),
                               COALESCE(accepted.maximum_internal_gap_seconds, 0),
                               EXTRACT(EPOCH FROM
                                   (bound.window_end - accepted.last_sample_time))
                           )::numeric(18,6)
                       END AS maximum_observed_gap_seconds,
                       accepted.mean_value, accepted.minimum_value,
                       accepted.maximum_value, accepted.first_value,
                       accepted.last_value, accepted.slope_value,
                       accepted.true_ratio_value,
                       COALESCE(raw.source_fingerprint, md5('')) AS source_fingerprint
                  FROM bound_windows bound
                  LEFT JOIN raw_stats raw
                    ON raw.review_id = bound.review_id
                   AND raw.feature_ref = bound.feature_ref
                  LEFT JOIN accepted_stats accepted
                    ON accepted.review_id = bound.review_id
                   AND accepted.feature_ref = bound.feature_ref
                 ORDER BY bound.line_id, bound.prediction_time,
                          bound.batch_id, bound.review_id, bound.feature_ref
                """);

        return jdbc.query(sql.toString(), parameters, (rs, rowNum) -> {
            String featureRef = rs.getString("feature_ref");
            ProcessSignalWindowDefinition definition = definitions.get(featureRef);
            if (definition == null) {
                throw new IllegalStateException(
                        "Window evidence refers to an unpinned feature: " + featureRef);
            }
            return mapEvidence(rs, definition);
        });
    }

    private ProcessSignalWindowEvidence mapEvidence(
            ResultSet rs,
            ProcessSignalWindowDefinition definition) throws SQLException {
        return new ProcessSignalWindowEvidence(
                rs.getObject("review_id", UUID.class),
                rs.getObject("shadow_run_id", UUID.class),
                rs.getObject("batch_id", UUID.class),
                rs.getString("batch_no"),
                rs.getString("plant_id"),
                rs.getString("line_id"),
                rs.getObject("rule_version_id", UUID.class),
                rs.getObject("topology_version_id", UUID.class),
                rs.getObject("point_catalog_snapshot_id", UUID.class),
                definition,
                instant(rs, "prediction_time"),
                instant(rs, "window_start"),
                instant(rs, "window_end"),
                rs.getInt("binding_count"),
                rs.getString("binding_expected_unit"),
                rs.getString("binding_calibration_version"),
                rs.getString("product_id"),
                rs.getString("device_id"),
                rs.getString("property_id"),
                rs.getString("point_catalog_unit"),
                rs.getString("point_catalog_calibration_version"),
                rs.getString("point_catalog_device_state"),
                rs.getObject("point_catalog_registered", Boolean.class),
                rs.getObject("point_catalog_property_present", Boolean.class),
                rs.getString("point_catalog_calibration_status"),
                rs.getInt("source_point_count"),
                rs.getInt("accepted_sample_count"),
                rs.getInt("rejected_quality_count"),
                rs.getInt("late_availability_count"),
                rs.getInt("unit_mismatch_count"),
                rs.getInt("value_type_mismatch_count"),
                rs.getInt("calibration_mismatch_count"),
                instant(rs, "first_sample_time"),
                instant(rs, "last_sample_time"),
                instant(rs, "latest_ingest_time"),
                rs.getBigDecimal("maximum_observed_gap_seconds"),
                rs.getBigDecimal("mean_value"),
                rs.getBigDecimal("minimum_value"),
                rs.getBigDecimal("maximum_value"),
                rs.getBigDecimal("first_value"),
                rs.getBigDecimal("last_value"),
                rs.getBigDecimal("slope_value"),
                rs.getBigDecimal("true_ratio_value"),
                rs.getString("source_fingerprint"));
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}
