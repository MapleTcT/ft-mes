\set ON_ERROR_STOP on

BEGIN;

CREATE TEMP TABLE target_snapshot ON COMMIT DROP AS
SELECT snapshot.*
  FROM bpi.bpi_point_catalog_snapshots snapshot
 WHERE snapshot.tenant_id = '1000'
   AND snapshot.plant_id = :'plant_id'
   AND snapshot.line_id = :'line_id'
 ORDER BY snapshot.observed_at DESC, snapshot.imported_at DESC, snapshot.id
 LIMIT 1;

CREATE TEMP TABLE target_point ON COMMIT DROP AS
SELECT entry.*
  FROM bpi.bpi_point_catalog_entries entry
  JOIN target_snapshot snapshot
    ON snapshot.tenant_id = entry.tenant_id
   AND snapshot.id = entry.snapshot_id
 WHERE entry.tenant_id = '1000'
   AND entry.product_id = :'product_id'
   AND entry.device_id = :'device_id'
   AND entry.property_id = :'property_id'
   AND entry.calibration_version = :'calibration_version'
   AND entry.calibration_status = 'VERIFIED'
   AND entry.registered
   AND entry.property_present
   AND entry.device_state = 'ACTIVE'
   AND entry.source_sequence_required
   AND entry.source_sequence_origin IN ('DEVICE', 'GATEWAY');

SELECT (SELECT count(*) FROM target_snapshot) = 1
   AND (SELECT count(*) FROM target_point) = 1 AS fixture_point_ready
\gset
\if :fixture_point_ready
\else
    \echo 'ERROR: latest point catalog snapshot does not expose the controlled verified pilot point'
    \quit
\endif

INSERT INTO bpi.bpi_point_calibrations
    (id, tenant_id, plant_id, line_id, product_id, device_id, property_id,
     calibration_version, certificate_reference, certificate_checksum,
     valid_from, valid_until, state, revision, submitted_by, submit_reason,
     decided_by, decided_at, decision_reason)
SELECT md5(:'marker' || ':calibration')::uuid, '1000', :'plant_id', :'line_id',
       :'product_id', :'device_id', :'property_id', :'calibration_version',
       'urn:adp:controlled-telemetry-landing:' || :'marker',
       md5(:'marker' || ':certificate') || md5(:'marker' || ':certificate'),
       snapshot.observed_at - interval '1 hour', now() + interval '1 day',
       'APPROVED', 2, :'marker' || '_ENGINEER',
       'Controlled target fixture for Kafka telemetry landing acceptance',
       :'marker' || '_REVIEWER', now(),
       'Independent fixture approval; not a field metrology certificate'
  FROM target_snapshot snapshot;

INSERT INTO bpi.bpi_topology_versions
    (id, tenant_id, topology_code, version, state, checksum, definition,
     created_by, plant_id, line_id, revision, updated_by, validation_status,
     validation_errors, validation_warnings, validated_checksum, validated_by,
     validated_at, published_by, published_at, validated_point_catalog_snapshot_id,
     validated_point_catalog_checksum)
SELECT md5(:'marker' || ':topology')::uuid, '1000', :'marker' || '_TOPOLOGY',
       '1.0.0', 'PUBLISHED',
       md5(:'marker' || ':topology-checksum') || md5(:'marker' || ':topology-checksum'),
       jsonb_build_object(
           'localityGroup', point.locality_group,
           'nodes', jsonb_build_array(
               jsonb_build_object('code', 'FLOW', 'type', 'METER',
                                  'name', 'Controlled MQTT flow meter')),
           'edges', '[]'::jsonb,
           'bindings', jsonb_build_array(
               jsonb_build_object(
                   'signal', :'property_id',
                   'productId', point.product_id,
                   'deviceId', point.device_id,
                   'propertyId', point.property_id,
                   'expectedUnit', point.unit,
                   'calibrationVersion', point.calibration_version)),
           'requiredSignals', jsonb_build_array(:'property_id'),
           'controlledSimulator', true,
           'fieldDeviceClaimed', false),
       :'marker' || '_ENGINEER', :'plant_id', :'line_id', 3,
       :'marker' || '_ENGINEER', 'PASSED', '[]'::jsonb, '[]'::jsonb,
       md5(:'marker' || ':topology-checksum') || md5(:'marker' || ':topology-checksum'),
       :'marker' || '_VALIDATOR', now(), :'marker' || '_TOPOLOGY_APPROVER', now(),
       snapshot.id, snapshot.checksum
  FROM target_snapshot snapshot
  CROSS JOIN target_point point;

INSERT INTO bpi.bpi_rule_versions
    (id, tenant_id, rule_code, version, topology_version_id, state, checksum,
     definition, revision, created_by, plant_id, line_id, updated_by)
VALUES
    (md5(:'marker' || ':rule')::uuid, '1000', :'marker' || '_RULE', '1.0.0',
     md5(:'marker' || ':topology')::uuid, 'PUBLISHED',
     md5(:'marker' || ':rule-checksum') || md5(:'marker' || ':rule-checksum'),
     jsonb_build_object(
         'boundaryType', 'START',
         'logic', 'controlled-telemetry-landing-acceptance',
         'trainingAllowed', false,
         'productionActivationAllowed', false),
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
SELECT md5(:'marker' || ':outbox')::uuid, '1000', :'plant_id', :'line_id',
       'RULE_VERSION', md5(:'marker' || ':rule')::uuid,
       'BOUNDARY_RULE_PUBLISHED', 'bpi.boundary.rule-publication.v1',
       :'marker' || '_RULE_KEY', decode('010203', 'hex'),
       jsonb_build_object(
           'marker', :'marker',
           'lifecycle_action', 'ACTIVATE',
           'controlledSimulator', true,
           'fieldDeviceClaimed', false),
       'PUBLISHED', now(), 'APPLIED', :'marker' || '_APPLICATION',
       :'marker' || '_DEPLOYMENT', now(), now(), 'READY',
       :'marker' || '_READINESS', :'marker' || '_DEPLOYMENT', now(), now(),
       :'marker' || '_CATALOG_EVENT', snapshot.source_revision,
       'ACTIVATE', 1, true
  FROM target_snapshot snapshot;

COMMIT;

SELECT jsonb_pretty(jsonb_build_object(
    'marker', :'marker',
    'database', 'PostgreSQL',
    'controlledSimulator', true,
    'fieldDeviceClaimed', false,
    'productionReadyClaimed', false,
    'catalog', (
        SELECT jsonb_build_object(
            'id', snapshot.id,
            'source', snapshot.source,
            'sourceInstance', snapshot.source_instance,
            'sourceRevision', snapshot.source_revision,
            'observedAt', snapshot.observed_at,
            'checksum', snapshot.checksum)
          FROM bpi.bpi_point_catalog_snapshots snapshot
          JOIN bpi.bpi_topology_versions topology
            ON topology.tenant_id = snapshot.tenant_id
           AND topology.validated_point_catalog_snapshot_id = snapshot.id
         WHERE topology.id = md5(:'marker' || ':topology')::uuid
    ),
    'calibrationVersion', :'calibration_version',
    'topologyId', md5(:'marker' || ':topology')::uuid,
    'ruleId', md5(:'marker' || ':rule')::uuid
));
