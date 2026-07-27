package com.mapletct.ftmes.bpi.infrastructure.postgres;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
public class ProcessEvidencePostgresRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public ProcessEvidencePostgresRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<EvidenceRow> list(
            String tenantId,
            String plantId,
            String lineId,
            Instant from,
            Instant to,
            List<String> propertyIds,
            int limit) {
        String propertyFilter = propertyIds.isEmpty()
                ? ""
                : " AND point.property_id IN (:propertyIds)\n";
        String sql = """
                SELECT event.device_id,
                       point.property_id,
                       point.unit,
                       point.sample_time,
                       point.numeric_value,
                       point.string_value,
                       point.boolean_value,
                       point.quality_code
                  FROM bpi.bpi_telemetry_events event
                  JOIN bpi.bpi_telemetry_points point
                    ON point.telemetry_event_id = event.id
                   AND point.tenant_id = event.tenant_id
                 WHERE event.tenant_id = :tenantId
                   AND event.plant_id = :plantId
                   AND event.line_id = :lineId
                   AND event.status IN ('ACCEPTED', 'PARTIAL')
                   AND point.sample_time >= :from
                   AND point.sample_time <= :to
                """ + propertyFilter + """
                 ORDER BY point.sample_time, point.property_id, event.device_id, point.id
                 LIMIT :limit
                """;
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("plantId", plantId)
                .addValue("lineId", lineId)
                .addValue("from", Timestamp.from(from))
                .addValue("to", Timestamp.from(to))
                .addValue("limit", limit);
        if (!propertyIds.isEmpty()) {
            parameters.addValue("propertyIds", propertyIds);
        }
        return jdbc.query(sql, parameters, (rs, rowNumber) -> {
            var numericValue = rs.getBigDecimal("numeric_value");
            return new EvidenceRow(
                    rs.getString("device_id"),
                    rs.getString("property_id"),
                    rs.getString("unit"),
                    rs.getTimestamp("sample_time").toInstant(),
                    numericValue == null ? null : numericValue.doubleValue(),
                    rs.getString("string_value"),
                    rs.getObject("boolean_value", Boolean.class),
                    rs.getString("quality_code"));
        });
    }

    public record EvidenceRow(
            String deviceId,
            String propertyId,
            String unit,
            Instant sampleTime,
            Double numericValue,
            String stringValue,
            Boolean booleanValue,
            String qualityCode) {
    }
}
