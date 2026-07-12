package com.mapletct.ftmes.bpi.infrastructure.telemetry;

import com.mapletct.ftmes.bpi.domain.TelemetryValue;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class TelemetryPostgresRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public TelemetryPostgresRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public TelemetryEventSnapshot findEvent(String tenantId, String eventId) {
        List<TelemetryEventSnapshot> rows = jdbc.query("""
                SELECT id, event_id, payload_checksum, status, sequence_disposition,
                       accepted_point_count, rejected_point_count
                  FROM bpi.bpi_telemetry_events
                 WHERE tenant_id = :tenantId AND event_id = :eventId
                """, new MapSqlParameterSource().addValue("tenantId", tenantId).addValue("eventId", eventId),
                (rs, row) -> new TelemetryEventSnapshot(
                        rs.getObject("id", UUID.class), rs.getString("event_id"), rs.getString("payload_checksum"),
                        rs.getString("status"), rs.getString("sequence_disposition"),
                        rs.getInt("accepted_point_count"), rs.getInt("rejected_point_count")));
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void lockEventIdentity(String tenantId, String eventId) {
        jdbc.queryForObject("""
                SELECT pg_advisory_xact_lock(hashtextextended(CAST(:identity AS text), 0))::text
                """, new MapSqlParameterSource().addValue("identity", tenantId + "|" + eventId), String.class);
    }

    public TelemetryEventSnapshot findSourceIdentity(
            String tenantId, String gatewayId, String deviceId, BigInteger sourceEpoch, BigInteger sequence) {
        List<TelemetryEventSnapshot> rows = jdbc.query("""
                SELECT id, event_id, payload_checksum, status, sequence_disposition,
                       accepted_point_count, rejected_point_count
                  FROM bpi.bpi_telemetry_events
                 WHERE tenant_id = :tenantId AND gateway_id = :gatewayId AND device_id = :deviceId
                   AND source_epoch = :sourceEpoch AND sequence = :sequence
                """, new MapSqlParameterSource()
                .addValue("tenantId", tenantId).addValue("gatewayId", gatewayId).addValue("deviceId", deviceId)
                .addValue("sourceEpoch", decimal(sourceEpoch)).addValue("sequence", decimal(sequence)),
                (rs, row) -> new TelemetryEventSnapshot(
                        rs.getObject("id", UUID.class), rs.getString("event_id"), rs.getString("payload_checksum"),
                        rs.getString("status"), rs.getString("sequence_disposition"),
                        rs.getInt("accepted_point_count"), rs.getInt("rejected_point_count")));
        return rows.isEmpty() ? null : rows.get(0);
    }

    public SourceState lockSource(String tenantId, String gatewayId, String deviceId) {
        List<SourceState> rows = jdbc.query("""
                SELECT source_epoch, last_sequence, last_event_id, last_event_time, revision
                  FROM bpi.bpi_telemetry_source_state
                 WHERE tenant_id = :tenantId AND gateway_id = :gatewayId AND device_id = :deviceId
                 FOR UPDATE
                """, sourceParameters(tenantId, gatewayId, deviceId),
                (rs, row) -> new SourceState(
                        rs.getBigDecimal("source_epoch").toBigIntegerExact(),
                        rs.getBigDecimal("last_sequence").toBigIntegerExact(),
                        rs.getString("last_event_id"), rs.getTimestamp("last_event_time").toInstant(),
                        rs.getLong("revision")));
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void insertSourceIfAbsent(String tenantId, String gatewayId, String deviceId,
                                     BigInteger sourceEpoch, BigInteger sequence,
                                     String eventId, Instant eventTime) {
        jdbc.update("""
                INSERT INTO bpi.bpi_telemetry_source_state
                    (tenant_id, gateway_id, device_id, source_epoch, last_sequence, last_event_id, last_event_time)
                VALUES (:tenantId, :gatewayId, :deviceId, :sourceEpoch, :sequence, :eventId, :eventTime)
                ON CONFLICT (tenant_id, gateway_id, device_id) DO NOTHING
                """, sourceParameters(tenantId, gatewayId, deviceId)
                .addValue("sourceEpoch", decimal(sourceEpoch)).addValue("sequence", decimal(sequence))
                .addValue("eventId", eventId).addValue("eventTime", Timestamp.from(eventTime)));
    }

    public void advanceSource(String tenantId, String gatewayId, String deviceId,
                              BigInteger sourceEpoch, BigInteger sequence,
                              String eventId, Instant eventTime, long expectedRevision) {
        int updated = jdbc.update("""
                UPDATE bpi.bpi_telemetry_source_state
                   SET source_epoch = :sourceEpoch, last_sequence = :sequence,
                       last_event_id = :eventId, last_event_time = :eventTime,
                       revision = revision + 1, updated_at = now()
                 WHERE tenant_id = :tenantId AND gateway_id = :gatewayId AND device_id = :deviceId
                   AND revision = :expectedRevision
                """, sourceParameters(tenantId, gatewayId, deviceId)
                .addValue("sourceEpoch", decimal(sourceEpoch)).addValue("sequence", decimal(sequence))
                .addValue("eventId", eventId).addValue("eventTime", Timestamp.from(eventTime))
                .addValue("expectedRevision", expectedRevision));
        if (updated != 1) throw new IllegalStateException("Telemetry source state changed concurrently");
    }

    public void insertEvent(TelemetryEvent event) {
        jdbc.update("""
                INSERT INTO bpi.bpi_telemetry_events
                    (id, tenant_id, plant_id, line_id, gateway_id, product_id, device_id,
                     event_id, message_id, event_time, ingest_time, source_epoch, sequence,
                     sequence_origin, sequence_disposition, payload_checksum, headers,
                     point_count, accepted_point_count, rejected_point_count, status)
                VALUES (:id, :tenantId, :plantId, :lineId, :gatewayId, :productId, :deviceId,
                        :eventId, :messageId, :eventTime, :ingestTime, :sourceEpoch, :sequence,
                        :sequenceOrigin, :sequenceDisposition, :payloadChecksum, CAST(:headers AS jsonb),
                        :pointCount, :acceptedPointCount, :rejectedPointCount, :status)
                """, event.parameters());
    }

    public void insertPoint(UUID id, String tenantId, UUID telemetryEventId, String eventId, TelemetryValue point) {
        jdbc.update("""
                INSERT INTO bpi.bpi_telemetry_points
                    (id, tenant_id, telemetry_event_id, event_id, property_id, value_type,
                     numeric_value, string_value, boolean_value, unit, quality_code,
                     sample_time, calibration_version)
                VALUES (:id, :tenantId, :telemetryEventId, :eventId, :propertyId, :valueType,
                        :numericValue, :stringValue, :booleanValue, :unit, :qualityCode,
                        :sampleTime, :calibrationVersion)
                """, new MapSqlParameterSource()
                .addValue("id", id).addValue("tenantId", tenantId).addValue("telemetryEventId", telemetryEventId)
                .addValue("eventId", eventId).addValue("propertyId", point.propertyId())
                .addValue("valueType", point.valueType()).addValue("numericValue", point.numericValue())
                .addValue("stringValue", point.stringValue()).addValue("booleanValue", point.booleanValue())
                .addValue("unit", point.unit()).addValue("qualityCode", point.qualityCode())
                .addValue("sampleTime", Timestamp.from(point.sampleTime()))
                .addValue("calibrationVersion", point.calibrationVersion()));
    }

    public void insertPointReject(UUID id, String tenantId, UUID telemetryEventId, String eventId,
                                  int pointIndex, String propertyId, String reasonsJson, String rawPointJson) {
        jdbc.update("""
                INSERT INTO bpi.bpi_telemetry_point_rejects
                    (id, tenant_id, telemetry_event_id, event_id, point_index, property_id, reason_codes, raw_point)
                VALUES (:id, :tenantId, :telemetryEventId, :eventId, :pointIndex, :propertyId,
                        CAST(:reasons AS jsonb), CAST(:rawPoint AS jsonb))
                """, new MapSqlParameterSource()
                .addValue("id", id).addValue("tenantId", tenantId).addValue("telemetryEventId", telemetryEventId)
                .addValue("eventId", eventId).addValue("pointIndex", pointIndex).addValue("propertyId", propertyId)
                .addValue("reasons", reasonsJson).addValue("rawPoint", rawPointJson));
    }

    public void insertQuarantine(UUID id, String tenantId, String eventId, String payloadChecksum,
                                 String reasonsJson, String rawPayloadJson, String traceId) {
        jdbc.update("""
                INSERT INTO bpi.bpi_telemetry_quarantine
                    (id, tenant_id, event_id, payload_checksum, reason_codes, raw_payload, trace_id)
                VALUES (:id, :tenantId, :eventId, :payloadChecksum, CAST(:reasons AS jsonb),
                        CAST(:rawPayload AS jsonb), :traceId)
                ON CONFLICT (tenant_id, payload_checksum) DO NOTHING
                """, new MapSqlParameterSource().addValue("id", id).addValue("tenantId", tenantId)
                .addValue("eventId", eventId).addValue("payloadChecksum", payloadChecksum)
                .addValue("reasons", reasonsJson).addValue("rawPayload", rawPayloadJson).addValue("traceId", traceId));
    }

    private MapSqlParameterSource sourceParameters(String tenantId, String gatewayId, String deviceId) {
        return new MapSqlParameterSource().addValue("tenantId", tenantId)
                .addValue("gatewayId", gatewayId).addValue("deviceId", deviceId);
    }

    private BigDecimal decimal(BigInteger value) {
        return new BigDecimal(value);
    }

    public record TelemetryEventSnapshot(
            UUID id, String eventId, String payloadChecksum, String status, String sequenceDisposition,
            int acceptedPointCount, int rejectedPointCount) {
    }

    public record SourceState(
            BigInteger sourceEpoch, BigInteger lastSequence, String lastEventId, Instant lastEventTime, long revision) {
    }

    public record TelemetryEvent(
            UUID id, String tenantId, String plantId, String lineId, String gatewayId, String productId,
            String deviceId, String eventId, String messageId, Instant eventTime, Instant ingestTime,
            BigInteger sourceEpoch, BigInteger sequence, String sequenceOrigin, String sequenceDisposition,
            String payloadChecksum, String headersJson, int pointCount, int acceptedPointCount,
            int rejectedPointCount, String status) {

        MapSqlParameterSource parameters() {
            return new MapSqlParameterSource().addValue("id", id).addValue("tenantId", tenantId)
                    .addValue("plantId", plantId).addValue("lineId", lineId).addValue("gatewayId", gatewayId)
                    .addValue("productId", productId).addValue("deviceId", deviceId).addValue("eventId", eventId)
                    .addValue("messageId", messageId).addValue("eventTime", Timestamp.from(eventTime))
                    .addValue("ingestTime", Timestamp.from(ingestTime)).addValue("sourceEpoch", new BigDecimal(sourceEpoch))
                    .addValue("sequence", new BigDecimal(sequence)).addValue("sequenceOrigin", sequenceOrigin)
                    .addValue("sequenceDisposition", sequenceDisposition).addValue("payloadChecksum", payloadChecksum)
                    .addValue("headers", headersJson).addValue("pointCount", pointCount)
                    .addValue("acceptedPointCount", acceptedPointCount).addValue("rejectedPointCount", rejectedPointCount)
                    .addValue("status", status);
        }
    }
}
