\set ON_ERROR_STOP on

BEGIN;

CREATE TEMP TABLE target_dataset_definitions ON COMMIT DROP AS
SELECT id FROM bpi.bpi_dataset_definitions
 WHERE tenant_id = '1000' AND dataset_code = :'marker';

CREATE TEMP TABLE target_dataset_snapshots ON COMMIT DROP AS
SELECT id FROM bpi.bpi_dataset_snapshots
 WHERE tenant_id = '1000' AND dataset_id IN (SELECT id FROM target_dataset_definitions);

CREATE TEMP TABLE target_shadow_runs ON COMMIT DROP AS
SELECT id FROM bpi.bpi_shadow_runs
 WHERE tenant_id = '1000' AND run_code LIKE :'marker' || '_SHADOW_%';

DELETE FROM bpi.bpi_dataset_snapshot_samples
 WHERE tenant_id = '1000' AND snapshot_id IN (SELECT id FROM target_dataset_snapshots);

DELETE FROM bpi.bpi_audit_events
 WHERE tenant_id = '1000'
   AND object_id IN (
       SELECT id FROM target_dataset_definitions
       UNION ALL SELECT id FROM target_dataset_snapshots);

DELETE FROM bpi.bpi_api_idempotency
 WHERE tenant_id = '1000'
   AND (idempotency_key LIKE :'marker' || '%'
        OR response_body::text LIKE '%' || :'marker' || '%');

DELETE FROM bpi.bpi_dataset_snapshots
 WHERE tenant_id = '1000' AND id IN (SELECT id FROM target_dataset_snapshots);

DELETE FROM bpi.bpi_dataset_definitions
 WHERE tenant_id = '1000' AND id IN (SELECT id FROM target_dataset_definitions);

DELETE FROM bpi.bpi_shadow_run_batch_reviews
 WHERE tenant_id = '1000' AND shadow_run_id IN (SELECT id FROM target_shadow_runs);

DELETE FROM bpi.bpi_shadow_runs
 WHERE tenant_id = '1000' AND id IN (SELECT id FROM target_shadow_runs);

DELETE FROM bpi.bpi_batch_instances
 WHERE tenant_id = '1000' AND batch_no LIKE :'marker' || '_%';

DELETE FROM bpi.bpi_rule_versions
 WHERE tenant_id = '1000' AND rule_code = :'marker' || '_RULE';

DELETE FROM bpi.bpi_topology_versions
 WHERE tenant_id = '1000' AND topology_code = :'marker' || '_TOPOLOGY';

DELETE FROM bpi.bpi_point_catalog_snapshots
 WHERE tenant_id = '1000' AND source_revision = :'marker';

COMMIT;

SELECT jsonb_pretty(jsonb_build_object(
    'marker', :'marker',
    'remaining', jsonb_build_object(
        'definitions', (SELECT count(*) FROM bpi.bpi_dataset_definitions
                         WHERE tenant_id = '1000' AND dataset_code = :'marker'),
        'snapshots', (SELECT count(*) FROM bpi.bpi_dataset_snapshots snapshot
                       JOIN bpi.bpi_dataset_definitions definition
                         ON definition.tenant_id = snapshot.tenant_id
                        AND definition.id = snapshot.dataset_id
                      WHERE definition.tenant_id = '1000' AND definition.dataset_code = :'marker'),
        'samples', (SELECT count(*) FROM bpi.bpi_dataset_snapshot_samples sample
                     WHERE sample.tenant_id = '1000'
                       AND sample.batch_no LIKE :'marker' || '_%'),
        'shadowRuns', (SELECT count(*) FROM bpi.bpi_shadow_runs
                        WHERE tenant_id = '1000' AND run_code LIKE :'marker' || '_SHADOW_%'),
        'reviews', (SELECT count(*) FROM bpi.bpi_shadow_run_batch_reviews review
                     WHERE review.tenant_id = '1000'
                       AND review.review_reason LIKE :'marker' || '%'),
        'batches', (SELECT count(*) FROM bpi.bpi_batch_instances
                     WHERE tenant_id = '1000' AND batch_no LIKE :'marker' || '_%'),
        'rules', (SELECT count(*) FROM bpi.bpi_rule_versions
                   WHERE tenant_id = '1000' AND rule_code = :'marker' || '_RULE'),
        'topologies', (SELECT count(*) FROM bpi.bpi_topology_versions
                        WHERE tenant_id = '1000' AND topology_code = :'marker' || '_TOPOLOGY'),
        'catalogs', (SELECT count(*) FROM bpi.bpi_point_catalog_snapshots
                      WHERE tenant_id = '1000' AND source_revision = :'marker'),
        'idempotency', (SELECT count(*) FROM bpi.bpi_api_idempotency
                         WHERE tenant_id = '1000'
                           AND (idempotency_key LIKE :'marker' || '%'
                                OR response_body::text LIKE '%' || :'marker' || '%'))
    )
));
