\set ON_ERROR_STOP on

BEGIN;

INSERT INTO bpi.bpi_point_catalog_snapshots
    (id, tenant_id, source, source_instance, source_revision, plant_id, line_id,
     checksum, observed_at, point_count, source_claim_ready_point_count, imported_by)
VALUES
    (md5(:'marker' || ':catalog')::uuid, '1000', 'JETLINKS',
     :'marker' || '_SOURCE', :'marker', 'PLANT-01', 'LINE-S07-01', repeat('a', 64),
     now(), 1, 1, :'marker');

INSERT INTO bpi.bpi_point_catalog_entries
    (id, tenant_id, snapshot_id, plant_id, line_id, locality_group, product_id,
     device_id, property_id, source_property_id, point_name, unit, data_type,
     device_state, registered, property_present, calibration_version,
     calibration_status, source_sequence_enabled)
VALUES
    (md5(:'marker' || ':point')::uuid, '1000', md5(:'marker' || ':catalog')::uuid,
     'PLANT-01', 'LINE-S07-01', :'marker' || '_LOCALITY', 'PRODUCT-SUGAR',
     :'marker' || '_DEVICE', 'flow.instant', 'instantFlow', '影子验收瞬时流量',
     't/h', 'DECIMAL', 'ACTIVE', true, true, :'marker' || '_CAL_1', 'VERIFIED', true);

INSERT INTO bpi.bpi_point_calibrations
    (id, tenant_id, plant_id, line_id, product_id, device_id, property_id,
     calibration_version, certificate_reference, certificate_checksum,
     valid_from, valid_until, state, revision, submitted_by, submit_reason,
     decided_by, decided_at, decision_reason)
VALUES
    (md5(:'marker' || ':calibration')::uuid, '1000', 'PLANT-01', 'LINE-S07-01',
     'PRODUCT-SUGAR', :'marker' || '_DEVICE', 'flow.instant', :'marker' || '_CAL_1',
     'urn:adp:shadow-run-acceptance:' || :'marker', repeat('d', 64),
     now() - interval '1 day', now() + interval '1 year', 'APPROVED', 2,
     :'marker' || '_CAL_AUTHOR', '受控影子验收校准证据',
     :'marker' || '_CAL_REVIEWER', now(), '独立复核通过');

INSERT INTO bpi.bpi_topology_versions
    (id, tenant_id, topology_code, version, state, checksum, definition,
     created_by, plant_id, line_id, revision, updated_by, validation_status,
     validation_errors, validation_warnings, validated_checksum, validated_by,
     validated_at, published_by, published_at, validated_point_catalog_snapshot_id,
     validated_point_catalog_checksum)
VALUES
    (md5(:'marker' || ':topology')::uuid, '1000', :'marker' || '_TOPOLOGY', '1.0.0',
     'PUBLISHED', repeat('b', 64),
     jsonb_build_object(
         'localityGroup', :'marker' || '_LOCALITY',
         'nodes', jsonb_build_array(jsonb_build_object('code', 'FLOW', 'type', 'METER', 'name', '流量计')),
         'edges', '[]'::jsonb,
         'bindings', jsonb_build_array(jsonb_build_object(
             'signal', 'flow.instant', 'productId', 'PRODUCT-SUGAR',
             'deviceId', :'marker' || '_DEVICE', 'propertyId', 'flow.instant',
             'expectedUnit', 't/h', 'calibrationVersion', :'marker' || '_CAL_1')),
         'requiredSignals', jsonb_build_array('flow.instant')),
     :'marker' || '_ENGINEER', 'PLANT-01', 'LINE-S07-01', 3,
     :'marker' || '_ENGINEER', 'PASSED', '[]'::jsonb, '[]'::jsonb, repeat('b', 64),
     :'marker' || '_VALIDATOR', now(), :'marker' || '_TOPOLOGY_APPROVER', now(),
     md5(:'marker' || ':catalog')::uuid, repeat('a', 64));

INSERT INTO bpi.bpi_rule_versions
    (id, tenant_id, rule_code, version, topology_version_id, state, checksum,
     definition, revision, created_by, plant_id, line_id, updated_by)
VALUES
    (md5(:'marker' || ':rule')::uuid, '1000', :'marker' || '_RULE', '1.0.0',
     md5(:'marker' || ':topology')::uuid, 'PUBLISHED', repeat('c', 64),
     jsonb_build_object('boundaryType', 'START', 'logic', 'controlled-shadow-acceptance'),
     4, :'marker' || '_ENGINEER', 'PLANT-01', 'LINE-S07-01', :'marker' || '_ENGINEER');

INSERT INTO bpi.bpi_outbox_events
    (id, tenant_id, plant_id, line_id, aggregate_type, aggregate_id, event_type,
     topic, partition_key, payload, headers, status, published_at,
     application_status, application_event_id, application_deployment_id,
     application_observed_at, application_received_at,
     runtime_readiness_status, runtime_readiness_event_id,
     runtime_readiness_deployment_id, runtime_readiness_observed_at,
     runtime_readiness_received_at, runtime_point_catalog_event_id,
     runtime_point_catalog_source_revision, lifecycle_action,
     lifecycle_sequence, lifecycle_active)
VALUES
    (md5(:'marker' || ':outbox')::uuid, '1000', 'PLANT-01', 'LINE-S07-01',
     'RULE_VERSION', md5(:'marker' || ':rule')::uuid, 'BOUNDARY_RULE_PUBLISHED',
     'bpi.boundary.rule-publication.v1', :'marker' || '_RULE_KEY', decode('010203', 'hex'),
     jsonb_build_object('marker', :'marker', 'lifecycle_action', 'ACTIVATE'),
     'PUBLISHED', now(), 'APPLIED', :'marker' || '_APPLICATION', :'marker' || '_DEPLOYMENT',
     now(), now(), 'READY', :'marker' || '_READINESS', :'marker' || '_DEPLOYMENT',
     now(), now(), :'marker' || '_CATALOG_EVENT', :'marker', 'ACTIVATE', 1, true);

INSERT INTO bpi.bpi_batch_instances
    (id, tenant_id, plant_id, batch_no, line_id, stage_code, order_id,
     material_code, state, revision, is_shadow, start_time, end_time,
     quantity, quantity_unit, topology_version_id, rule_version_id, created_by)
SELECT md5(:'marker' || ':batch:' || item::text)::uuid,
       '1000', 'PLANT-01', :'marker' || '_BATCH_' || lpad(item::text, 2, '0'),
       'LINE-S07-01', 'SUGAR-STAGE', :'marker' || '_ORDER_' || lpad(item::text, 2, '0'),
       'SUGAR', 'CLOSED_RAW', 2, true,
       now() - interval '10 hours' + ((item - 1) * interval '45 minutes'),
       now() - interval '10 hours' + ((item - 1) * interval '45 minutes') + interval '20 minutes',
       100.000000, 't', md5(:'marker' || ':topology')::uuid,
       md5(:'marker' || ':rule')::uuid, :'marker' || '_FLINK'
  FROM generate_series(1, 10) AS item;

INSERT INTO bpi.bpi_data_quality_incidents
    (id, tenant_id, plant_id, line_id, source, device_id, property_id,
     issue_code, severity, state, revision, event_count, first_seen,
     last_seen, last_event_id, last_source_event_id, last_detail)
VALUES
    (md5(:'marker' || ':incident')::uuid, '1000', 'PLANT-01', 'LINE-S07-01',
     'FLINK', :'marker' || '_DEVICE', 'flow.instant', 'CLOCK_DRIFT', 'CRITICAL',
     'OPEN', 1, 1, now() - interval '1 hour', now(), :'marker' || '_DQ_EVENT',
     :'marker' || '_SOURCE_EVENT', :'marker' || ' controlled CRITICAL blocker');

COMMIT;

SELECT jsonb_pretty(jsonb_build_object(
    'marker', :'marker',
    'catalogId', md5(:'marker' || ':catalog')::uuid,
    'topologyId', md5(:'marker' || ':topology')::uuid,
    'ruleId', md5(:'marker' || ':rule')::uuid,
    'incidentId', md5(:'marker' || ':incident')::uuid,
    'closedShadowBatches', (
        SELECT count(*) FROM bpi.bpi_batch_instances
         WHERE tenant_id = '1000' AND batch_no LIKE :'marker' || '_BATCH_%'
    )
));
