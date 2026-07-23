\set ON_ERROR_STOP on

BEGIN;

CREATE TEMP TABLE target_shadow_runs ON COMMIT DROP AS
SELECT id
  FROM bpi.bpi_shadow_runs
 WHERE tenant_id = '1000' AND run_code = :'marker';

CREATE TEMP TABLE target_events ON COMMIT DROP AS
SELECT id, event_id
  FROM bpi.bpi_telemetry_events
 WHERE tenant_id = '1000'
   AND (
       message_id LIKE :'preheat_marker' || ':%'
       OR message_id LIKE :'window_marker' || ':%'
   );

CREATE TEMP TABLE target_catalogs ON COMMIT DROP AS
SELECT DISTINCT snapshot.id
  FROM bpi.bpi_point_catalog_snapshots snapshot
  JOIN bpi.bpi_point_catalog_entries entry
    ON entry.tenant_id = snapshot.tenant_id
   AND entry.snapshot_id = snapshot.id
 WHERE snapshot.tenant_id = '1000'
   AND snapshot.plant_id = :'plant_id'
   AND snapshot.line_id = :'line_id'
   AND entry.product_id = :'product_id'
   AND entry.device_id = :'device_id'
   AND entry.property_id = :'property_id'
   AND entry.calibration_version = :'calibration_version';

DELETE FROM bpi.bpi_shadow_run_batch_reviews
 WHERE tenant_id = '1000'
   AND shadow_run_id IN (SELECT id FROM target_shadow_runs);

DELETE FROM bpi.bpi_audit_events
 WHERE tenant_id = '1000'
   AND (
       object_id IN (SELECT id FROM target_shadow_runs)
       OR object_id IN (
           md5(:'marker' || ':rule')::uuid,
           md5(:'marker' || ':topology')::uuid,
           md5(:'marker' || ':calibration')::uuid
       )
       OR detail::text LIKE '%' || :'marker' || '%'
   );

DELETE FROM bpi.bpi_api_idempotency
 WHERE tenant_id = '1000'
   AND response_body::text LIKE '%' || :'marker' || '%';

DELETE FROM bpi.bpi_shadow_runs
 WHERE tenant_id = '1000'
   AND id IN (SELECT id FROM target_shadow_runs);

DELETE FROM bpi.bpi_outbox_events
 WHERE tenant_id = '1000'
   AND aggregate_id = md5(:'marker' || ':rule')::uuid;

DELETE FROM bpi.bpi_rule_approval_requests
 WHERE tenant_id = '1000'
   AND rule_version_id = md5(:'marker' || ':rule')::uuid;

DELETE FROM bpi.bpi_rule_simulations
 WHERE tenant_id = '1000'
   AND rule_version_id = md5(:'marker' || ':rule')::uuid;

DELETE FROM bpi.bpi_rule_versions
 WHERE tenant_id = '1000'
   AND id = md5(:'marker' || ':rule')::uuid;

DELETE FROM bpi.bpi_topology_versions
 WHERE tenant_id = '1000'
   AND id = md5(:'marker' || ':topology')::uuid;

DELETE FROM bpi.bpi_point_calibrations
 WHERE tenant_id = '1000'
   AND id = md5(:'marker' || ':calibration')::uuid;

DELETE FROM bpi.bpi_telemetry_point_rejects
 WHERE tenant_id = '1000'
   AND telemetry_event_id IN (SELECT id FROM target_events);

DELETE FROM bpi.bpi_telemetry_points
 WHERE tenant_id = '1000'
   AND telemetry_event_id IN (SELECT id FROM target_events);

DELETE FROM bpi.bpi_telemetry_source_state state
 WHERE state.tenant_id = '1000'
   AND state.gateway_id = :'gateway_id'
   AND state.device_id = :'device_id'
   AND state.last_event_id IN (SELECT event_id FROM target_events);

DELETE FROM bpi.bpi_telemetry_events
 WHERE tenant_id = '1000'
   AND id IN (SELECT id FROM target_events);

DELETE FROM bpi.bpi_point_catalog_entries
 WHERE tenant_id = '1000'
   AND snapshot_id IN (SELECT id FROM target_catalogs);

DELETE FROM bpi.bpi_point_catalog_snapshots
 WHERE tenant_id = '1000'
   AND id IN (SELECT id FROM target_catalogs);

COMMIT;

SELECT jsonb_pretty(jsonb_build_object(
    'marker', :'marker',
    'remaining', jsonb_build_object(
        'shadowRuns', (
            SELECT count(*) FROM bpi.bpi_shadow_runs
             WHERE tenant_id = '1000' AND run_code = :'marker'
        ),
        'rules', (
            SELECT count(*) FROM bpi.bpi_rule_versions
             WHERE tenant_id = '1000'
               AND id = md5(:'marker' || ':rule')::uuid
        ),
        'topologies', (
            SELECT count(*) FROM bpi.bpi_topology_versions
             WHERE tenant_id = '1000'
               AND id = md5(:'marker' || ':topology')::uuid
        ),
        'calibrations', (
            SELECT count(*) FROM bpi.bpi_point_calibrations
             WHERE tenant_id = '1000'
               AND id = md5(:'marker' || ':calibration')::uuid
        ),
        'telemetryEvents', (
            SELECT count(*) FROM bpi.bpi_telemetry_events
             WHERE tenant_id = '1000'
               AND (
                   message_id LIKE :'preheat_marker' || ':%'
                   OR message_id LIKE :'window_marker' || ':%'
               )
        ),
        'telemetryLatest', (
            SELECT count(*)
              FROM bpi.bpi_telemetry_point_latest latest
             WHERE latest.tenant_id = '1000'
               AND latest.plant_id = :'plant_id'
               AND latest.line_id = :'line_id'
               AND latest.product_id = :'product_id'
               AND latest.device_id = :'device_id'
               AND latest.property_id = :'property_id'
               AND latest.calibration_version = :'calibration_version'
        ),
        'catalogs', (
            SELECT count(*)
              FROM bpi.bpi_point_catalog_snapshots snapshot
              JOIN bpi.bpi_point_catalog_entries entry
                ON entry.tenant_id = snapshot.tenant_id
               AND entry.snapshot_id = snapshot.id
             WHERE snapshot.tenant_id = '1000'
               AND entry.calibration_version = :'calibration_version'
        )
    )
));
