\set ON_ERROR_STOP on

BEGIN;

CREATE TEMP TABLE target_shadow_runs ON COMMIT DROP AS
SELECT id FROM bpi.bpi_shadow_runs
 WHERE tenant_id = '1000' AND run_code = :'marker';

CREATE TEMP TABLE target_incidents ON COMMIT DROP AS
SELECT id FROM bpi.bpi_data_quality_incidents
 WHERE tenant_id = '1000' AND id = md5(:'marker' || ':incident')::uuid;

DELETE FROM bpi.bpi_shadow_run_batch_reviews
 WHERE tenant_id = '1000' AND shadow_run_id IN (SELECT id FROM target_shadow_runs);

DELETE FROM bpi.bpi_data_quality_incident_actions
 WHERE tenant_id = '1000' AND incident_id IN (SELECT id FROM target_incidents);

DELETE FROM bpi.bpi_data_quality_incident_events
 WHERE tenant_id = '1000' AND incident_id IN (SELECT id FROM target_incidents);

DELETE FROM bpi.bpi_audit_events
 WHERE tenant_id = '1000'
   AND object_id IN (
       SELECT id FROM target_shadow_runs
       UNION ALL SELECT id FROM target_incidents
   );

DELETE FROM bpi.bpi_api_idempotency
 WHERE tenant_id = '1000' AND idempotency_key LIKE :'marker' || '%';

DELETE FROM bpi.bpi_shadow_runs
 WHERE tenant_id = '1000' AND id IN (SELECT id FROM target_shadow_runs);

DELETE FROM bpi.bpi_data_quality_incidents
 WHERE tenant_id = '1000' AND id IN (SELECT id FROM target_incidents);

DELETE FROM bpi.bpi_batch_instances
 WHERE tenant_id = '1000' AND batch_no LIKE :'marker' || '_BATCH_%';

DELETE FROM bpi.bpi_outbox_events
 WHERE tenant_id = '1000' AND aggregate_id = md5(:'marker' || ':rule')::uuid;

DELETE FROM bpi.bpi_rule_approval_requests
 WHERE tenant_id = '1000' AND rule_version_id = md5(:'marker' || ':rule')::uuid;

DELETE FROM bpi.bpi_rule_simulations
 WHERE tenant_id = '1000' AND rule_version_id = md5(:'marker' || ':rule')::uuid;

DELETE FROM bpi.bpi_rule_versions
 WHERE tenant_id = '1000' AND id = md5(:'marker' || ':rule')::uuid;

DELETE FROM bpi.bpi_topology_versions
 WHERE tenant_id = '1000' AND id = md5(:'marker' || ':topology')::uuid;

DELETE FROM bpi.bpi_point_catalog_entries
 WHERE tenant_id = '1000' AND snapshot_id = md5(:'marker' || ':catalog')::uuid;

DELETE FROM bpi.bpi_point_calibrations
 WHERE tenant_id = '1000' AND id = md5(:'marker' || ':calibration')::uuid;

DELETE FROM bpi.bpi_point_catalog_snapshots
 WHERE tenant_id = '1000' AND id = md5(:'marker' || ':catalog')::uuid;

COMMIT;

SELECT jsonb_pretty(jsonb_build_object(
    'marker', :'marker',
    'remaining', jsonb_build_object(
        'shadowRuns', (SELECT count(*) FROM bpi.bpi_shadow_runs
                        WHERE tenant_id = '1000' AND run_code = :'marker'),
        'reviews', (SELECT count(*) FROM bpi.bpi_shadow_run_batch_reviews review
                     JOIN bpi.bpi_shadow_runs run ON run.id = review.shadow_run_id
                    WHERE run.tenant_id = '1000' AND run.run_code = :'marker'),
        'incidents', (SELECT count(*) FROM bpi.bpi_data_quality_incidents
                       WHERE tenant_id = '1000' AND id = md5(:'marker' || ':incident')::uuid),
        'batches', (SELECT count(*) FROM bpi.bpi_batch_instances
                     WHERE tenant_id = '1000' AND batch_no LIKE :'marker' || '_BATCH_%'),
        'outbox', (SELECT count(*) FROM bpi.bpi_outbox_events
                    WHERE tenant_id = '1000' AND aggregate_id = md5(:'marker' || ':rule')::uuid),
        'rules', (SELECT count(*) FROM bpi.bpi_rule_versions
                   WHERE tenant_id = '1000' AND id = md5(:'marker' || ':rule')::uuid),
        'topologies', (SELECT count(*) FROM bpi.bpi_topology_versions
                        WHERE tenant_id = '1000' AND id = md5(:'marker' || ':topology')::uuid),
        'catalogs', (SELECT count(*) FROM bpi.bpi_point_catalog_snapshots
                      WHERE tenant_id = '1000' AND id = md5(:'marker' || ':catalog')::uuid),
        'calibrations', (SELECT count(*) FROM bpi.bpi_point_calibrations
                          WHERE tenant_id = '1000' AND id = md5(:'marker' || ':calibration')::uuid),
        'idempotency', (SELECT count(*) FROM bpi.bpi_api_idempotency
                         WHERE tenant_id = '1000' AND idempotency_key LIKE :'marker' || '%')
    )
));
