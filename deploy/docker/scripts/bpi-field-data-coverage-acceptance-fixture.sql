\set ON_ERROR_STOP on

BEGIN;

INSERT INTO bpi.bpi_point_catalog_snapshots
    (id, tenant_id, source, source_instance, source_revision, plant_id, line_id,
     checksum, observed_at, point_count, source_claim_ready_point_count, imported_by)
VALUES
    (md5(:'marker' || ':catalog')::uuid, '1000', 'JETLINKS',
     :'marker' || '_SOURCE', :'marker', :'plant_id', :'line_id', repeat('a', 64),
     now(), 2, 2, :'marker');

INSERT INTO bpi.bpi_point_catalog_entries
    (id, tenant_id, snapshot_id, plant_id, line_id, locality_group, product_id,
     device_id, property_id, source_property_id, point_name, unit, data_type,
     device_state, registered, property_present, calibration_version,
     calibration_status, source_sequence_enabled, source_sequence_required,
     source_sequence_origin, source_sequence_binding_fingerprint)
VALUES
    (md5(:'marker' || ':flow-point')::uuid, '1000',
     md5(:'marker' || ':catalog')::uuid, :'plant_id', :'line_id',
     :'marker' || '_LOCALITY', 'PRODUCT-SUGAR', :'marker' || '_FLOW_DEVICE',
     'flow.instant', 'instantFlow', '现场覆盖验收瞬时流量', 't/h', 'DECIMAL',
     'ACTIVE', true, true, :'marker' || '_FLOW_CAL', 'VERIFIED',
     true, true, 'DEVICE', 'sha256:' || repeat('1', 64)),
    (md5(:'marker' || ':pump-point')::uuid, '1000',
     md5(:'marker' || ':catalog')::uuid, :'plant_id', :'line_id',
     :'marker' || '_LOCALITY', 'PRODUCT-SUGAR', :'marker' || '_PUMP_DEVICE',
     'pump.running', 'running', '现场覆盖验收泵运行状态', 'bool', 'BOOLEAN',
     'ACTIVE', true, true, :'marker' || '_PUMP_CAL', 'VERIFIED',
     true, true, 'GATEWAY', 'sha256:' || repeat('2', 64));

INSERT INTO bpi.bpi_source_sequence_evidence_current
    (id, tenant_id, source, source_instance, plant_id, line_id, product_id,
     device_id, binding_fingerprint, status, sequence_origin, source_epoch,
     first_sequence, last_sequence, observation_count, first_observed_at,
     last_observed_at, valid_until, source_event_id, observed_at,
     payload_checksum, revision)
SELECT md5(:'marker' || ':sequence:' || entry.device_id)::uuid,
       entry.tenant_id, snapshot.source, snapshot.source_instance,
       entry.plant_id, entry.line_id, entry.product_id, entry.device_id,
       entry.source_sequence_binding_fingerprint, 'QUALIFIED',
       entry.source_sequence_origin, 1, 1, 2, 2,
       snapshot.observed_at,
       snapshot.observed_at + interval '1 second',
       snapshot.observed_at + interval '1 day',
       :'marker' || '_SEQ_' || right(entry.device_id, 16),
       snapshot.observed_at + interval '1 second',
       CASE WHEN entry.property_id = 'flow.instant'
            THEN repeat('e', 64) ELSE repeat('f', 64) END,
       1
  FROM bpi.bpi_point_catalog_entries entry
  JOIN bpi.bpi_point_catalog_snapshots snapshot
    ON snapshot.tenant_id = entry.tenant_id
   AND snapshot.id = entry.snapshot_id
 WHERE entry.tenant_id = '1000'
   AND entry.snapshot_id = md5(:'marker' || ':catalog')::uuid;

INSERT INTO bpi.bpi_point_calibrations
    (id, tenant_id, plant_id, line_id, product_id, device_id, property_id,
     calibration_version, certificate_reference, certificate_checksum,
     valid_from, valid_until, state, revision, submitted_by, submit_reason,
     decided_by, decided_at, decision_reason)
VALUES
    (md5(:'marker' || ':flow-calibration')::uuid, '1000', :'plant_id', :'line_id',
     'PRODUCT-SUGAR', :'marker' || '_FLOW_DEVICE', 'flow.instant',
     :'marker' || '_FLOW_CAL', 'urn:adp:field-coverage:' || :'marker' || ':flow',
     repeat('c', 64), now() - interval '1 day', now() + interval '30 days',
     'APPROVED', 2, :'marker' || '_ENGINEER', '现场来源覆盖受控验收',
     :'marker' || '_REVIEWER', now(), '独立校准复核通过'),
    (md5(:'marker' || ':pump-calibration')::uuid, '1000', :'plant_id', :'line_id',
     'PRODUCT-SUGAR', :'marker' || '_PUMP_DEVICE', 'pump.running',
     :'marker' || '_PUMP_CAL', 'urn:adp:field-coverage:' || :'marker' || ':pump',
     repeat('d', 64), now() - interval '1 day', now() + interval '30 days',
     'APPROVED', 2, :'marker' || '_ENGINEER', '现场来源覆盖受控验收',
     :'marker' || '_REVIEWER', now(), '独立校准复核通过');

INSERT INTO bpi.bpi_topology_versions
    (id, tenant_id, topology_code, version, state, checksum, definition,
     created_by, plant_id, line_id, revision, updated_by, validation_status,
     validation_errors, validation_warnings, validated_checksum, validated_by,
     validated_at, published_by, published_at, validated_point_catalog_snapshot_id,
     validated_point_catalog_checksum)
VALUES
    (md5(:'marker' || ':topology')::uuid, '1000', :'marker' || '_TOPOLOGY',
     '1.0.0', 'PUBLISHED', repeat('b', 64),
     jsonb_build_object(
         'localityGroup', :'marker' || '_LOCALITY',
         'nodes', jsonb_build_array(
             jsonb_build_object('code', 'FLOW', 'type', 'METER', 'name', '流量计'),
             jsonb_build_object('code', 'PUMP', 'type', 'PUMP', 'name', '输送泵')),
         'edges', '[]'::jsonb,
         'bindings', jsonb_build_array(
             jsonb_build_object(
                 'signal', 'flow.instant', 'productId', 'PRODUCT-SUGAR',
                 'deviceId', :'marker' || '_FLOW_DEVICE',
                 'propertyId', 'flow.instant', 'expectedUnit', 't/h',
                 'calibrationVersion', :'marker' || '_FLOW_CAL'),
             jsonb_build_object(
                 'signal', 'pump.running', 'productId', 'PRODUCT-SUGAR',
                 'deviceId', :'marker' || '_PUMP_DEVICE',
                 'propertyId', 'pump.running', 'expectedUnit', 'bool',
                 'calibrationVersion', :'marker' || '_PUMP_CAL')),
         'requiredSignals', jsonb_build_array('flow.instant', 'pump.running')),
     :'marker' || '_ENGINEER', :'plant_id', :'line_id', 3,
     :'marker' || '_ENGINEER', 'PASSED', '[]'::jsonb, '[]'::jsonb,
     repeat('b', 64), :'marker' || '_VALIDATOR', now(),
     :'marker' || '_TOPOLOGY_APPROVER', now(),
     md5(:'marker' || ':catalog')::uuid, repeat('a', 64));

INSERT INTO bpi.bpi_rule_versions
    (id, tenant_id, rule_code, version, topology_version_id, state, checksum,
     definition, revision, created_by, plant_id, line_id, updated_by)
VALUES
    (md5(:'marker' || ':rule')::uuid, '1000', :'marker' || '_RULE', '1.0.0',
     md5(:'marker' || ':topology')::uuid, 'PUBLISHED', repeat('9', 64),
     jsonb_build_object(
         'boundaryType', 'START',
         'logic', 'field-data-coverage-acceptance',
         'trainingAllowed', false),
     4, :'marker' || '_ENGINEER', :'plant_id', :'line_id',
     :'marker' || '_ENGINEER');

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
    (md5(:'marker' || ':outbox')::uuid, '1000', :'plant_id', :'line_id',
     'RULE_VERSION', md5(:'marker' || ':rule')::uuid, 'BOUNDARY_RULE_PUBLISHED',
     'bpi.boundary.rule-publication.v1', :'marker' || '_RULE_KEY',
     decode('010203', 'hex'),
     jsonb_build_object('marker', :'marker', 'lifecycle_action', 'ACTIVATE'),
     'PUBLISHED', now(), 'APPLIED', :'marker' || '_APPLICATION',
     :'marker' || '_DEPLOYMENT', now(), now(), 'READY',
     :'marker' || '_READINESS', :'marker' || '_DEPLOYMENT', now(), now(),
     :'marker' || '_CATALOG_EVENT', :'marker', 'ACTIVATE', 1, true);

COMMIT;

SELECT jsonb_pretty(jsonb_build_object(
    'marker', :'marker',
    'plantId', :'plant_id',
    'lineId', :'line_id',
    'catalogId', md5(:'marker' || ':catalog')::uuid,
    'topologyId', md5(:'marker' || ':topology')::uuid,
    'ruleId', md5(:'marker' || ':rule')::uuid,
    'pointCount', (
        SELECT count(*) FROM bpi.bpi_point_catalog_entries
         WHERE tenant_id = '1000'
           AND snapshot_id = md5(:'marker' || ':catalog')::uuid
    ),
    'batchCount', (
        SELECT count(*) FROM bpi.bpi_batch_instances
         WHERE tenant_id = '1000' AND batch_no LIKE :'marker' || '%'
    )
));
