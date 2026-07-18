\set ON_ERROR_STOP on

BEGIN;

CREATE TEMP TABLE bpi_acceptance_target_topologies ON COMMIT DROP AS
SELECT id
  FROM bpi.bpi_topology_versions
 WHERE tenant_id = '1000'
   AND topology_code LIKE :'marker' || '%';

CREATE TEMP TABLE bpi_acceptance_target_rules ON COMMIT DROP AS
SELECT id
  FROM bpi.bpi_rule_versions
 WHERE tenant_id = '1000'
   AND rule_code LIKE :'marker' || '%';

CREATE TEMP TABLE bpi_acceptance_target_catalogs ON COMMIT DROP AS
SELECT id, source_revision
  FROM bpi.bpi_point_catalog_snapshots
 WHERE tenant_id = '1000'
   AND (
       source_revision = :'marker'
       OR source_instance LIKE 'BPI-JOINT-' || :'marker' || '%'
   );

CREATE TEMP TABLE bpi_acceptance_target_inbox_events ON COMMIT DROP AS
SELECT application_event_id AS event_id
  FROM bpi.bpi_outbox_events
 WHERE tenant_id = '1000'
   AND aggregate_id IN (SELECT id FROM bpi_acceptance_target_rules)
   AND application_event_id IS NOT NULL
UNION
SELECT runtime_readiness_event_id
  FROM bpi.bpi_outbox_events
 WHERE tenant_id = '1000'
   AND aggregate_id IN (SELECT id FROM bpi_acceptance_target_rules)
   AND runtime_readiness_event_id IS NOT NULL
UNION
SELECT 'point-catalog-' || substring(source_revision FROM 8)
  FROM bpi_acceptance_target_catalogs
 WHERE source_revision LIKE 'sha256:%';

CREATE TEMP TABLE bpi_acceptance_target_batches ON COMMIT DROP AS
SELECT id
  FROM bpi.bpi_batch_instances
 WHERE tenant_id = '1000'
   AND (
       rule_version_id IN (SELECT id FROM bpi_acceptance_target_rules)
       OR topology_version_id IN (SELECT id FROM bpi_acceptance_target_topologies)
   );

CREATE TEMP TABLE bpi_acceptance_target_telemetry ON COMMIT DROP AS
SELECT id
  FROM bpi.bpi_telemetry_events
 WHERE tenant_id = '1000'
   AND (
       gateway_id LIKE :'marker' || '%'
       OR event_id LIKE :'marker' || '%'
       OR message_id LIKE :'marker' || '%'
   );

UPDATE bpi.bpi_rule_versions
   SET latest_simulation_id = NULL
 WHERE id IN (SELECT id FROM bpi_acceptance_target_rules);

DELETE FROM bpi.bpi_boundary_evidence
 WHERE tenant_id = '1000'
   AND batch_id IN (SELECT id FROM bpi_acceptance_target_batches);

DELETE FROM bpi.bpi_batch_state_events
 WHERE tenant_id = '1000'
   AND batch_id IN (SELECT id FROM bpi_acceptance_target_batches);

DELETE FROM bpi.bpi_batch_candidates
 WHERE tenant_id = '1000'
   AND (
       rule_version_id IN (SELECT id FROM bpi_acceptance_target_rules)
       OR topology_version_id IN (SELECT id FROM bpi_acceptance_target_topologies)
       OR batch_id IN (SELECT id FROM bpi_acceptance_target_batches)
   );

DELETE FROM bpi.bpi_batch_instances
 WHERE id IN (SELECT id FROM bpi_acceptance_target_batches);

DELETE FROM bpi.bpi_outbox_events
 WHERE tenant_id = '1000'
   AND aggregate_id IN (SELECT id FROM bpi_acceptance_target_rules);

DELETE FROM bpi.bpi_audit_events
 WHERE tenant_id = '1000'
   AND object_id IN (
       SELECT id FROM bpi_acceptance_target_rules
       UNION ALL
       SELECT id FROM bpi_acceptance_target_topologies
   );

DELETE FROM bpi.bpi_api_idempotency
 WHERE tenant_id = '1000'
   AND (
       idempotency_key LIKE :'marker' || '%'
       OR EXISTS (
           SELECT 1
             FROM bpi_acceptance_target_rules target
            WHERE resource_path LIKE '%' || target.id::text || '%'
       )
       OR EXISTS (
           SELECT 1
             FROM bpi_acceptance_target_topologies target
            WHERE resource_path LIKE '%' || target.id::text || '%'
       )
   );

DELETE FROM bpi.bpi_inbox_events
 WHERE tenant_id = '1000'
   AND (
       idempotency_key LIKE :'marker' || '%'
       OR event_id LIKE :'marker' || '%'
       OR event_id IN (SELECT event_id FROM bpi_acceptance_target_inbox_events)
   );

DELETE FROM bpi.bpi_rule_approval_requests
 WHERE tenant_id = '1000'
   AND rule_version_id IN (SELECT id FROM bpi_acceptance_target_rules);

DELETE FROM bpi.bpi_rule_simulations
 WHERE tenant_id = '1000'
   AND rule_version_id IN (SELECT id FROM bpi_acceptance_target_rules);

DELETE FROM bpi.bpi_rule_golden_boundaries
 WHERE tenant_id = '1000'
   AND (
       golden_set_id LIKE :'marker' || '%'
       OR source_ref LIKE :'marker' || '%'
       OR created_by = :'marker'
   );

DELETE FROM bpi.bpi_telemetry_point_rejects
 WHERE tenant_id = '1000'
   AND telemetry_event_id IN (SELECT id FROM bpi_acceptance_target_telemetry);

DELETE FROM bpi.bpi_telemetry_quarantine
 WHERE tenant_id = '1000'
   AND event_id LIKE :'marker' || '%';

DELETE FROM bpi.bpi_telemetry_points
 WHERE tenant_id = '1000'
   AND telemetry_event_id IN (SELECT id FROM bpi_acceptance_target_telemetry);

DELETE FROM bpi.bpi_telemetry_events
 WHERE id IN (SELECT id FROM bpi_acceptance_target_telemetry);

DELETE FROM bpi.bpi_telemetry_source_state
 WHERE tenant_id = '1000'
   AND (
       gateway_id LIKE :'marker' || '%'
       OR last_event_id LIKE :'marker' || '%'
   );

DELETE FROM bpi.bpi_rule_versions
 WHERE id IN (SELECT id FROM bpi_acceptance_target_rules);

DELETE FROM bpi.bpi_topology_versions
 WHERE id IN (SELECT id FROM bpi_acceptance_target_topologies);

DELETE FROM bpi.bpi_point_catalog_entries
 WHERE tenant_id = '1000'
   AND snapshot_id IN (SELECT id FROM bpi_acceptance_target_catalogs);

DELETE FROM bpi.bpi_point_catalog_snapshots
 WHERE tenant_id = '1000'
   AND id IN (SELECT id FROM bpi_acceptance_target_catalogs);

UPDATE bpi.bpi_feature_flags
   SET updated_by = 'bpi-test-environment',
       updated_at = now()
 WHERE tenant_id = '1000'
   AND flag_key = 'bpi.rule-management'
   AND updated_by = :'marker';

COMMIT;

SELECT jsonb_pretty(jsonb_build_object(
    'marker', :'marker',
    'remaining', jsonb_build_object(
        'topologies', (
            SELECT count(*)
              FROM bpi.bpi_topology_versions
             WHERE tenant_id = '1000'
               AND topology_code LIKE :'marker' || '%'
        ),
        'rules', (
            SELECT count(*)
              FROM bpi.bpi_rule_versions
             WHERE tenant_id = '1000'
               AND rule_code LIKE :'marker' || '%'
        ),
        'goldenBoundaries', (
            SELECT count(*)
              FROM bpi.bpi_rule_golden_boundaries
             WHERE tenant_id = '1000'
               AND golden_set_id LIKE :'marker' || '%'
        ),
        'telemetryEvents', (
            SELECT count(*)
              FROM bpi.bpi_telemetry_events
             WHERE tenant_id = '1000'
               AND (
                   gateway_id LIKE :'marker' || '%'
                   OR event_id LIKE :'marker' || '%'
                   OR message_id LIKE :'marker' || '%'
               )
        ),
        'catalogSnapshots', (
            SELECT count(*)
              FROM bpi.bpi_point_catalog_snapshots
             WHERE tenant_id = '1000'
               AND (
                   source_revision = :'marker'
                   OR source_instance LIKE 'BPI-JOINT-' || :'marker' || '%'
               )
        ),
        'idempotency', (
            SELECT count(*)
              FROM bpi.bpi_api_idempotency
             WHERE tenant_id = '1000'
               AND idempotency_key LIKE :'marker' || '%'
        )
    )
));
