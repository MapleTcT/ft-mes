\set ON_ERROR_STOP on

SELECT 1 / CASE WHEN EXISTS (
    SELECT 1
      FROM bpi.bpi_feature_flags
     WHERE tenant_id = :'tenant_id'
       AND scope_type = 'LINE'
       AND scope_key = :'line_id'
       AND flag_key IN ('bpi.rule-management', 'bpi.commands')
) THEN 0 ELSE 1 END AS existing_feature_flag_guard;

INSERT INTO bpi.bpi_topology_versions
    (id, tenant_id, topology_code, version, state, checksum, definition,
     plant_id, line_id, revision, created_by, updated_by)
VALUES (
    md5(:'marker' || ':topology')::uuid,
    :'tenant_id',
    :'topology_code',
    :'topology_version',
    'PUBLISHED',
    repeat('t', 64),
    jsonb_build_object(
        'localityGroup', 'LOCALITY-BPI-ACCEPTANCE',
        'nodes', jsonb_build_array(
            jsonb_build_object('code', :'line_id', 'type', 'LINE'),
            jsonb_build_object('code', :'device_id', 'type', 'DEVICE')
        ),
        'bindings', jsonb_build_array(
            jsonb_build_object(
                'signal', 'flow.instant',
                'deviceId', :'device_id',
                'propertyId', 'flow.instant',
                'expectedUnit', 't/h',
                'calibrationVersion', 'CAL-1'
            ),
            jsonb_build_object(
                'signal', 'pump.running',
                'deviceId', :'device_id',
                'propertyId', 'pump.running',
                'expectedUnit', 'bool',
                'calibrationVersion', 'CAL-1'
            )
        )
    ),
    :'plant_id',
    :'line_id',
    1,
    :'marker',
    :'marker'
);

INSERT INTO bpi.bpi_rule_versions
    (id, tenant_id, rule_code, version, topology_version_id, state, checksum, definition,
     revision, plant_id, line_id, created_by, updated_by)
VALUES (
    md5(:'marker' || ':rule')::uuid,
    :'tenant_id',
    :'rule_code',
    :'rule_version',
    md5(:'marker' || ':topology')::uuid,
    'DRAFT',
    repeat('r', 64),
    jsonb_build_object(
        'boundaryType', 'START',
        'quorumMinimum', 2,
        'minimumConfidence', 0.80,
        'maxCompositePenalty', 0.80,
        'timing', jsonb_build_object(
            'allowedLatenessSeconds', 0,
            'watermarkDelaySeconds', 0,
            'evaluationTimeoutSeconds', 300
        ),
        'conditions', jsonb_build_array(
            jsonb_build_object(
                'signal', 'flow.instant',
                'operator', 'GREATER_THAN',
                'threshold', 10,
                'holdSeconds', 0,
                'maxSilenceSeconds', 60,
                'classification', 'QUORUM',
                'weight', 50
            ),
            jsonb_build_object(
                'signal', 'pump.running',
                'operator', 'EQUALS_TRUE',
                'holdSeconds', 0,
                'maxSilenceSeconds', 60,
                'classification', 'QUORUM',
                'weight', 50
            )
        )
    ),
    1,
    :'plant_id',
    :'line_id',
    :'marker',
    :'marker'
);

INSERT INTO bpi.bpi_feature_flags
    (id, tenant_id, scope_type, scope_key, flag_key, enabled, revision, updated_by)
VALUES
    (md5(:'marker' || ':flag:rule')::uuid, :'tenant_id', 'LINE', :'line_id',
     'bpi.rule-management', true, 1, :'marker'),
    (md5(:'marker' || ':flag:commands')::uuid, :'tenant_id', 'LINE', :'line_id',
     'bpi.commands', true, 1, :'marker');

INSERT INTO bpi.bpi_rule_golden_boundaries
    (id, tenant_id, plant_id, line_id, golden_set_id, boundary_type,
     boundary_time, tolerance_seconds, source_ref, created_by)
VALUES (
    md5(:'marker' || ':golden')::uuid,
    :'tenant_id',
    :'plant_id',
    :'line_id',
    :'golden_set_id',
    'START',
    :'boundary_time'::timestamptz,
    5,
    'browser-joint-acceptance:' || :'marker',
    :'marker'
);

INSERT INTO bpi.bpi_telemetry_events
    (id, tenant_id, plant_id, line_id, gateway_id, product_id, device_id,
     event_id, message_id, event_time, ingest_time, source_epoch, sequence,
     sequence_origin, sequence_disposition, payload_checksum, headers,
     point_count, accepted_point_count, rejected_point_count, status)
VALUES (
    md5(:'marker' || ':telemetry-event')::uuid,
    :'tenant_id',
    :'plant_id',
    :'line_id',
    'GW-BPI-ACCEPTANCE',
    'PRODUCT-BPI-ACCEPTANCE',
    :'device_id',
    :'marker' || '-HISTORY-EVENT',
    :'marker' || '-HISTORY-MESSAGE',
    :'boundary_time'::timestamptz,
    :'boundary_time'::timestamptz + interval '10 milliseconds',
    1,
    1,
    'EXPORTER',
    'FIRST',
    repeat('a', 64),
    jsonb_build_object('acceptance_marker', :'marker'),
    2,
    2,
    0,
    'ACCEPTED'
);

INSERT INTO bpi.bpi_telemetry_points
    (id, tenant_id, telemetry_event_id, event_id, property_id, value_type,
     numeric_value, string_value, boolean_value, unit, quality_code,
     sample_time, calibration_version)
VALUES
    (md5(:'marker' || ':point:flow')::uuid, :'tenant_id',
     md5(:'marker' || ':telemetry-event')::uuid, :'marker' || '-HISTORY-FLOW',
     'flow.instant', 'DOUBLE', 18.6, NULL, NULL, 't/h', 'GOOD',
     :'boundary_time'::timestamptz, 'CAL-1'),
    (md5(:'marker' || ':point:pump')::uuid, :'tenant_id',
     md5(:'marker' || ':telemetry-event')::uuid, :'marker' || '-HISTORY-PUMP',
     'pump.running', 'BOOLEAN', NULL, NULL, true, 'bool', 'GOOD',
     :'boundary_time'::timestamptz, 'CAL-1');

SELECT json_build_object(
    'status', 'SEEDED',
    'marker', :'marker',
    'tenantId', :'tenant_id',
    'plantId', :'plant_id',
    'lineId', :'line_id',
    'topologyId', md5(:'marker' || ':topology')::uuid,
    'topology', :'topology_code' || '@' || :'topology_version',
    'ruleId', md5(:'marker' || ':rule')::uuid,
    'rule', :'rule_code' || '@' || :'rule_version',
    'goldenSetId', :'golden_set_id',
    'boundaryTime', :'boundary_time'
);
