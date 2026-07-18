\set ON_ERROR_STOP on

BEGIN;

INSERT INTO bpi.bpi_feature_flags
    (id, tenant_id, scope_type, scope_key, flag_key, enabled, revision, updated_by)
VALUES
    (md5(:'marker' || ':feature-flag')::uuid, '1000', 'LINE', 'LINE-S07-01',
     'bpi.rule-management', true, 1, :'marker')
ON CONFLICT (tenant_id, scope_type, scope_key, flag_key)
DO UPDATE SET enabled = true,
              revision = bpi.bpi_feature_flags.revision + 1,
              updated_by = EXCLUDED.updated_by,
              updated_at = now();

INSERT INTO bpi.bpi_point_catalog_snapshots
    (id, tenant_id, source, source_instance, source_revision, plant_id, line_id,
     checksum, observed_at, point_count, ready_point_count, imported_by)
VALUES
    (md5(:'marker' || ':catalog')::uuid, '1000', 'JETLINKS', 'BPI-LIFECYCLE-ACCEPTANCE',
     :'marker', 'PLANT-01', 'LINE-S07-01', repeat('c', 64), now(), 2, 2, :'marker');

INSERT INTO bpi.bpi_point_catalog_entries
    (id, tenant_id, snapshot_id, plant_id, line_id, locality_group, product_id,
     device_id, property_id, source_property_id, point_name, unit, data_type,
     device_state, registered, property_present, calibration_version,
     calibration_status, source_sequence_enabled)
VALUES
    (md5(:'marker' || ':catalog-flow')::uuid, '1000', md5(:'marker' || ':catalog')::uuid,
     'PLANT-01', 'LINE-S07-01', 'LOCALITY-S07-V2', 'PRODUCT-SUGAR', 'DEVICE-S07-01',
     'flow.instant', 'instantFlow', '进料瞬时流量', 't/h', 'double', 'ACTIVE', true, true,
     'CAL-1', 'VERIFIED', true),
    (md5(:'marker' || ':catalog-pump')::uuid, '1000', md5(:'marker' || ':catalog')::uuid,
     'PLANT-01', 'LINE-S07-01', 'LOCALITY-S07-V2', 'PRODUCT-SUGAR', 'DEVICE-S07-01',
     'pump.running', 'pumpRunning', '进料泵运行', 'bool', 'boolean', 'ACTIVE', true, true,
     'CAL-1', 'VERIFIED', true);

INSERT INTO bpi.bpi_topology_versions
    (id, tenant_id, topology_code, version, state, checksum, definition, created_by,
     created_at, plant_id, line_id, revision, updated_by, validation_status,
     validation_errors, validation_warnings, validated_checksum, validated_by,
     validated_at, published_by, published_at, validated_point_catalog_snapshot_id,
     validated_point_catalog_checksum)
VALUES
    (md5(:'marker' || ':topology-v1')::uuid, '1000', :'marker' || '_TOPOLOGY', '1.0.0',
     'PUBLISHED', repeat('a', 64),
     '{"localityGroup":"LOCALITY-S07-V1","nodes":[{"code":"PUMP","type":"PUMP","name":"进料泵"},{"code":"TANK","type":"TANK","name":"接收罐"}],"edges":[{"from":"PUMP","to":"TANK"}],"bindings":[{"signal":"flow.instant","productId":"PRODUCT-SUGAR","deviceId":"DEVICE-S07-01","propertyId":"flow.instant","expectedUnit":"t/h","calibrationVersion":"CAL-1"},{"signal":"pump.running","productId":"PRODUCT-SUGAR","deviceId":"DEVICE-S07-01","propertyId":"pump.running","expectedUnit":"bool","calibrationVersion":"CAL-1"}],"requiredSignals":["flow.instant","pump.running"]}'::jsonb,
     :'marker' || '_AUTHOR', now() - interval '3 minutes', 'PLANT-01', 'LINE-S07-01', 3,
     :'marker' || '_AUTHOR', 'PASSED', '[]'::jsonb, '[]'::jsonb, repeat('a', 64),
     :'marker' || '_VALIDATOR', now() - interval '3 minutes', :'marker' || '_TOPOLOGY_APPROVER',
     now() - interval '3 minutes', md5(:'marker' || ':catalog')::uuid, repeat('c', 64)),
    (md5(:'marker' || ':topology-v2')::uuid, '1000', :'marker' || '_TOPOLOGY', '2.0.0',
     'PUBLISHED', repeat('b', 64),
     '{"localityGroup":"LOCALITY-S07-V2","nodes":[{"code":"FEED","type":"TANK","name":"进料罐"},{"code":"PUMP","type":"PUMP","name":"进料泵"},{"code":"TANK","type":"TANK","name":"接收罐"}],"edges":[{"from":"FEED","to":"PUMP"},{"from":"PUMP","to":"TANK"}],"bindings":[{"signal":"flow.instant","productId":"PRODUCT-SUGAR","deviceId":"DEVICE-S07-01","propertyId":"flow.instant","expectedUnit":"t/h","calibrationVersion":"CAL-1"},{"signal":"pump.running","productId":"PRODUCT-SUGAR","deviceId":"DEVICE-S07-01","propertyId":"pump.running","expectedUnit":"bool","calibrationVersion":"CAL-1"}],"requiredSignals":["flow.instant","pump.running"]}'::jsonb,
     :'marker' || '_AUTHOR', now() - interval '2 minutes', 'PLANT-01', 'LINE-S07-01', 3,
     :'marker' || '_AUTHOR', 'PASSED', '[]'::jsonb, '[]'::jsonb, repeat('b', 64),
     :'marker' || '_VALIDATOR', now() - interval '2 minutes', :'marker' || '_TOPOLOGY_APPROVER',
     now() - interval '2 minutes', md5(:'marker' || ':catalog')::uuid, repeat('c', 64));

INSERT INTO bpi.bpi_rule_versions
    (id, tenant_id, rule_code, version, topology_version_id, state, checksum, definition,
     revision, created_by, created_at, plant_id, line_id, updated_by)
VALUES
    (md5(:'marker' || ':rule-base')::uuid, '1000', :'marker' || '_RULE', '1.0.0',
     md5(:'marker' || ':topology-v1')::uuid, 'PUBLISHED', repeat('d', 64),
     '{"boundaryType":"START","quorumMinimum":2,"minimumConfidence":0.8,"maxCompositePenalty":0.8,"timing":{"allowedLatenessSeconds":0,"watermarkDelaySeconds":0,"evaluationTimeoutSeconds":300},"conditions":[{"signal":"flow.instant","operator":"GREATER_THAN","threshold":8,"holdSeconds":0,"maxSilenceSeconds":60,"classification":"QUORUM","weight":50},{"signal":"pump.running","operator":"EQUALS_TRUE","holdSeconds":0,"maxSilenceSeconds":60,"classification":"QUORUM","weight":50}]}'::jsonb,
     4, :'marker' || '_AUTHOR', now() - interval '3 minutes', 'PLANT-01', 'LINE-S07-01',
     :'marker' || '_AUTHOR'),
    (md5(:'marker' || ':rule-publish')::uuid, '1000', :'marker' || '_RULE', '2.0.0',
     md5(:'marker' || ':topology-v2')::uuid, 'DRAFT', repeat('e', 64),
     '{"boundaryType":"START","quorumMinimum":2,"minimumConfidence":0.8,"maxCompositePenalty":0.8,"timing":{"allowedLatenessSeconds":0,"watermarkDelaySeconds":0,"evaluationTimeoutSeconds":300},"conditions":[{"signal":"flow.instant","operator":"GREATER_THAN","threshold":10,"holdSeconds":0,"maxSilenceSeconds":60,"classification":"QUORUM","weight":50},{"signal":"pump.running","operator":"EQUALS_TRUE","holdSeconds":0,"maxSilenceSeconds":60,"classification":"QUORUM","weight":50}]}'::jsonb,
     1, :'marker' || '_AUTHOR', now() - interval '2 minutes', 'PLANT-01', 'LINE-S07-01',
     :'marker' || '_AUTHOR'),
    (md5(:'marker' || ':rule-reject')::uuid, '1000', :'marker' || '_REJECT', '1.0.0',
     md5(:'marker' || ':topology-v2')::uuid, 'DRAFT', repeat('f', 64),
     '{"boundaryType":"START","quorumMinimum":2,"minimumConfidence":0.8,"maxCompositePenalty":0.8,"timing":{"allowedLatenessSeconds":0,"watermarkDelaySeconds":0,"evaluationTimeoutSeconds":300},"conditions":[{"signal":"flow.instant","operator":"GREATER_THAN","threshold":12,"holdSeconds":0,"maxSilenceSeconds":60,"classification":"QUORUM","weight":50},{"signal":"pump.running","operator":"EQUALS_TRUE","holdSeconds":0,"maxSilenceSeconds":60,"classification":"QUORUM","weight":50}]}'::jsonb,
     1, :'marker' || '_AUTHOR', now() - interval '1 minute', 'PLANT-01', 'LINE-S07-01',
     :'marker' || '_AUTHOR');

INSERT INTO bpi.bpi_rule_golden_boundaries
    (id, tenant_id, plant_id, line_id, golden_set_id, boundary_type, boundary_time,
     tolerance_seconds, source_ref, created_by)
VALUES
    (md5(:'marker' || ':golden')::uuid, '1000', 'PLANT-01', 'LINE-S07-01',
     :'marker' || '_GOLDEN', 'START', :'boundary_time'::timestamptz, 5,
     :'marker' || '_OPERATOR_REVIEW', :'marker');

INSERT INTO bpi.bpi_telemetry_events
    (id, tenant_id, plant_id, line_id, gateway_id, product_id, device_id, event_id,
     message_id, event_time, ingest_time, source_epoch, sequence, sequence_origin,
     sequence_disposition, payload_checksum, headers, point_count, accepted_point_count,
     rejected_point_count, status)
VALUES
    (md5(:'marker' || ':telemetry')::uuid, '1000', 'PLANT-01', 'LINE-S07-01',
     :'marker' || '_GATEWAY', 'PRODUCT-SUGAR', 'DEVICE-S07-01', :'marker' || '_EVENT',
     :'marker' || '_MESSAGE', :'boundary_time'::timestamptz,
     :'boundary_time'::timestamptz + interval '10 milliseconds', 1, 1, 'EXPORTER', 'FIRST',
     repeat('1', 64), '{}'::jsonb, 2, 2, 0, 'ACCEPTED');

INSERT INTO bpi.bpi_telemetry_points
    (id, tenant_id, telemetry_event_id, event_id, property_id, value_type,
     numeric_value, boolean_value, unit, quality_code, sample_time, calibration_version)
VALUES
    (md5(:'marker' || ':point-flow')::uuid, '1000', md5(:'marker' || ':telemetry')::uuid,
     :'marker' || '_FLOW', 'flow.instant', 'DOUBLE', 18.6, NULL, 't/h', 'GOOD',
     :'boundary_time'::timestamptz, 'CAL-1'),
    (md5(:'marker' || ':point-pump')::uuid, '1000', md5(:'marker' || ':telemetry')::uuid,
     :'marker' || '_PUMP', 'pump.running', 'BOOLEAN', NULL, true, 'bool', 'GOOD',
     :'boundary_time'::timestamptz, 'CAL-1');

COMMIT;

SELECT jsonb_build_object(
    'marker', :'marker',
    'topologyBaseId', md5(:'marker' || ':topology-v1')::uuid,
    'topologyTargetId', md5(:'marker' || ':topology-v2')::uuid,
    'publishRuleBaseId', md5(:'marker' || ':rule-base')::uuid,
    'publishRuleId', md5(:'marker' || ':rule-publish')::uuid,
    'rejectRuleId', md5(:'marker' || ':rule-reject')::uuid,
    'boundaryTime', :'boundary_time'
);
