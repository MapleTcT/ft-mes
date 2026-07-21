\set ON_ERROR_STOP on

BEGIN;

INSERT INTO bpi.bpi_point_catalog_snapshots
    (id, tenant_id, source, source_instance, source_revision, plant_id, line_id,
     checksum, observed_at, point_count, source_claim_ready_point_count, imported_by)
VALUES
    (md5(:'marker' || ':catalog')::uuid, '1000', 'JETLINKS',
     :'marker' || '_SOURCE', :'marker', 'PLANT-01', 'LINE-S07-01', repeat('a', 64),
     now() - interval '9 days', 1, 1, :'marker');

INSERT INTO bpi.bpi_topology_versions
    (id, tenant_id, topology_code, version, state, checksum, definition,
     plant_id, line_id, revision, created_by, updated_by,
     validation_status, validated_checksum, validated_by, validated_at,
     validated_point_catalog_snapshot_id, validated_point_catalog_checksum,
     published_by, published_at)
VALUES
    (md5(:'marker' || ':topology')::uuid, '1000', :'marker' || '_TOPOLOGY', '1.0.0',
     'PUBLISHED', repeat('b', 64), '{"nodes":[],"edges":[]}'::jsonb,
     'PLANT-01', 'LINE-S07-01', 2, :'marker', :'marker', 'PASSED', repeat('b', 64),
     :'marker' || '_REVIEWER', now() - interval '9 days',
     md5(:'marker' || ':catalog')::uuid, repeat('a', 64),
     :'marker' || '_REVIEWER', now() - interval '9 days');

INSERT INTO bpi.bpi_rule_versions
    (id, tenant_id, rule_code, version, topology_version_id, state,
     checksum, definition, revision, plant_id, line_id, created_by, updated_by)
VALUES
    (md5(:'marker' || ':rule')::uuid, '1000', :'marker' || '_RULE', '1.0.0',
     md5(:'marker' || ':topology')::uuid, 'PUBLISHED', repeat('c', 64),
     '{"logic":"dataset-target-fixture"}'::jsonb, 4, 'PLANT-01', 'LINE-S07-01',
     :'marker', :'marker');

INSERT INTO bpi.bpi_shadow_runs
    (id, tenant_id, run_code, name, plant_id, line_id, state, revision,
     rule_version_id, topology_version_id, point_catalog_snapshot_id,
     minimum_duration_days, minimum_reviewed_batches, boundary_tolerance_seconds,
     minimum_boundary_agreement, quantity_tolerance_percent,
     created_by, created_at, updated_by, updated_at,
     started_by, started_at, completed_by, completed_at,
     decided_by, decided_at, decision_reason)
VALUES
    (md5(:'marker' || ':shadow-primary')::uuid, '1000', :'marker' || '_SHADOW_PRIMARY',
     :'marker' || ' approved dataset source', 'PLANT-01', 'LINE-S07-01', 'APPROVED', 14,
     md5(:'marker' || ':rule')::uuid, md5(:'marker' || ':topology')::uuid,
     md5(:'marker' || ':catalog')::uuid, 7, 10, 60, 0.950000, 2.000000,
     :'marker', now() - interval '9 days', :'marker', now() - interval '1 hour',
     :'marker', now() - interval '8 days', :'marker', now() - interval '2 hours',
     :'marker' || '_REVIEWER', now() - interval '1 hour', 'Approved target dataset fixture'),
    (md5(:'marker' || ':shadow-cross')::uuid, '1000', :'marker' || '_SHADOW_CROSS',
     :'marker' || ' cross-plant collision', 'PLANT-02', 'LINE-S07-01', 'APPROVED', 14,
     md5(:'marker' || ':rule')::uuid, md5(:'marker' || ':topology')::uuid,
     md5(:'marker' || ':catalog')::uuid, 7, 10, 60, 0.950000, 2.000000,
     :'marker', now() - interval '9 days', :'marker', now() - interval '1 hour',
     :'marker', now() - interval '8 days', :'marker', now() - interval '2 hours',
     :'marker' || '_REVIEWER', now() - interval '1 hour', 'Cross-plant negative fixture');

INSERT INTO bpi.bpi_batch_instances
    (id, tenant_id, plant_id, batch_no, line_id, stage_code, order_id,
     material_code, state, revision, is_shadow, start_time, end_time,
     quantity, quantity_unit, quality_gate, wms_status,
     topology_version_id, rule_version_id, created_by)
VALUES
    (md5(:'marker' || ':batch-high')::uuid, '1000', 'PLANT-01', :'marker' || '_HIGH',
     'LINE-S07-01', 'EVAPORATION', :'marker' || '_ORDER_HIGH', 'SUGAR-JUICE',
     'CLOSED_RAW', 2, true, now() - interval '6 hours', now() - interval '5 hours 30 minutes',
     100.000000, 't', 'NOT_APPLICABLE', 'NOT_REQUESTED',
     md5(:'marker' || ':topology')::uuid, md5(:'marker' || ':rule')::uuid, :'marker'),
    (md5(:'marker' || ':batch-low')::uuid, '1000', 'PLANT-01', :'marker' || '_LOW',
     'LINE-S07-01', 'EVAPORATION', :'marker' || '_ORDER_LOW', 'SUGAR-JUICE',
     'CLOSED_RAW', 2, true, now() - interval '4 hours', now() - interval '3 hours 30 minutes',
     100.000000, 't', 'NOT_APPLICABLE', 'NOT_REQUESTED',
     md5(:'marker' || ':topology')::uuid, md5(:'marker' || ':rule')::uuid, :'marker'),
    (md5(:'marker' || ':batch-delayed')::uuid, '1000', 'PLANT-01', :'marker' || '_DELAYED',
     'LINE-S07-01', 'EVAPORATION', :'marker' || '_ORDER_DELAYED', 'SUGAR-JUICE',
     'CLOSED_RAW', 2, true, now() - interval '50 hours', now() - interval '49 hours 30 minutes',
     100.000000, 't', 'NOT_APPLICABLE', 'NOT_REQUESTED',
     md5(:'marker' || ':topology')::uuid, md5(:'marker' || ':rule')::uuid, :'marker'),
    (md5(:'marker' || ':batch-cross')::uuid, '1000', 'PLANT-02', :'marker' || '_CROSS_PLANT',
     'LINE-S07-01', 'EVAPORATION', :'marker' || '_ORDER_CROSS', 'SUGAR-JUICE',
     'CLOSED_RAW', 2, true, now() - interval '3 hours', now() - interval '2 hours 30 minutes',
     100.000000, 't', 'NOT_APPLICABLE', 'NOT_REQUESTED',
     md5(:'marker' || ':topology')::uuid, md5(:'marker' || ':rule')::uuid, :'marker');

INSERT INTO bpi.bpi_shadow_run_batch_reviews
    (id, tenant_id, shadow_run_id, batch_id, review_sequence, state,
     automatic_start_time, automatic_end_time, manual_start_time, manual_end_time,
     start_deviation_seconds, end_deviation_seconds,
     start_boundary_accepted, end_boundary_accepted,
     automatic_quantity, reference_quantity, quantity_unit,
     quantity_deviation_percent, quantity_within_tolerance,
     reviewed_by, review_reason, reviewed_at)
VALUES
    (md5(:'marker' || ':review-high')::uuid, '1000',
     md5(:'marker' || ':shadow-primary')::uuid, md5(:'marker' || ':batch-high')::uuid,
     1, 'ACTIVE', now() - interval '6 hours', now() - interval '5 hours 30 minutes',
     now() - interval '6 hours', now() - interval '5 hours 30 minutes', 0, 0, true, true,
     100.000000, 99.500000, 't', 0.500000000, true,
     :'marker' || '_REVIEWER', :'marker' || ' accepted label', now() - interval '5 hours'),
    (md5(:'marker' || ':review-low')::uuid, '1000',
     md5(:'marker' || ':shadow-primary')::uuid, md5(:'marker' || ':batch-low')::uuid,
     2, 'ACTIVE', now() - interval '4 hours', now() - interval '3 hours 30 minutes',
     now() - interval '4 hours' + interval '61 seconds', now() - interval '3 hours 30 minutes',
     61, 0, false, true, 100.000000, 99.500000, 't', 0.500000000, true,
     :'marker' || '_REVIEWER', :'marker' || ' low confidence label', now() - interval '3 hours'),
    (md5(:'marker' || ':review-delayed')::uuid, '1000',
     md5(:'marker' || ':shadow-primary')::uuid, md5(:'marker' || ':batch-delayed')::uuid,
     3, 'ACTIVE', now() - interval '50 hours', now() - interval '49 hours 30 minutes',
     now() - interval '50 hours', now() - interval '49 hours 30 minutes', 0, 0, true, true,
     100.000000, 99.500000, 't', 0.500000000, true,
     :'marker' || '_REVIEWER', :'marker' || ' delayed label', now() - interval '1 hour'),
    (md5(:'marker' || ':review-cross')::uuid, '1000',
     md5(:'marker' || ':shadow-cross')::uuid, md5(:'marker' || ':batch-cross')::uuid,
     1, 'ACTIVE', now() - interval '3 hours', now() - interval '2 hours 30 minutes',
     now() - interval '3 hours', now() - interval '2 hours 30 minutes', 0, 0, true, true,
     100.000000, 100.000000, 't', 0.000000000, true,
     :'marker' || '_REVIEWER', 'Must remain outside PLANT-01 manifest', now() - interval '30 minutes');

COMMIT;

SELECT jsonb_pretty(jsonb_build_object(
    'marker', :'marker',
    'tenantId', '1000',
    'plantId', 'PLANT-01',
    'lineId', 'LINE-S07-01',
    'ruleVersionId', md5(:'marker' || ':rule')::uuid,
    'primaryReviews', 3,
    'crossPlantReviews', 1
));
