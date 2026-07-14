\set ON_ERROR_STOP on

BEGIN;

CREATE TEMP TABLE bpi_joint_acceptance_ids ON COMMIT DROP AS
SELECT r.id AS rule_id,
       t.id AS topology_id,
       o.id AS outbox_id,
       o.application_event_id
  FROM bpi.bpi_rule_versions r
  JOIN bpi.bpi_topology_versions t ON t.id = r.topology_version_id
  LEFT JOIN bpi.bpi_outbox_events o
    ON o.tenant_id = r.tenant_id
   AND o.aggregate_id = r.id
 WHERE r.tenant_id = :'tenant_id'
   AND r.created_by = :'marker'
   AND t.created_by = :'marker';

CREATE TEMP TABLE bpi_joint_candidate_ids ON COMMIT DROP AS
SELECT id, candidate_key, batch_id
  FROM bpi.bpi_batch_candidates
 WHERE tenant_id = :'tenant_id'
   AND order_id = 'MO-' || :'marker';

CREATE TEMP TABLE bpi_joint_batch_ids ON COMMIT DROP AS
SELECT id
  FROM bpi.bpi_batch_instances
 WHERE tenant_id = :'tenant_id'
   AND order_id = 'MO-' || :'marker';

DELETE FROM bpi.bpi_audit_events
 WHERE tenant_id = :'tenant_id'
   AND (
        object_id IN (SELECT rule_id FROM bpi_joint_acceptance_ids)
     OR object_id IN (SELECT id FROM bpi_joint_candidate_ids)
     OR object_id IN (SELECT id FROM bpi_joint_batch_ids)
   );

DELETE FROM bpi.bpi_boundary_evidence
 WHERE tenant_id = :'tenant_id'
   AND batch_id IN (SELECT id FROM bpi_joint_batch_ids);

DELETE FROM bpi.bpi_batch_state_events
 WHERE tenant_id = :'tenant_id'
   AND batch_id IN (SELECT id FROM bpi_joint_batch_ids);

DELETE FROM bpi.bpi_api_idempotency
 WHERE tenant_id = :'tenant_id'
   AND (
        resource_path LIKE ANY (
            SELECT '%' || rule_id::text || '%' FROM bpi_joint_acceptance_ids
        )
     OR resource_path LIKE ANY (
            SELECT '%' || id::text || '%' FROM bpi_joint_candidate_ids
        )
     OR resource_path LIKE ANY (
            SELECT '%' || id::text || '%' FROM bpi_joint_batch_ids
        )
   );

DELETE FROM bpi.bpi_inbox_events
 WHERE tenant_id = :'tenant_id'
   AND (
        idempotency_key IN (SELECT candidate_key::text FROM bpi_joint_candidate_ids)
     OR event_id IN (
            SELECT application_event_id
              FROM bpi_joint_acceptance_ids
             WHERE application_event_id IS NOT NULL
        )
     OR (
            source = 'bpi.boundary.rule-application.v1'
        AND event_id LIKE ANY (
                SELECT outbox_id::text || '|%'
                  FROM bpi_joint_acceptance_ids
                 WHERE outbox_id IS NOT NULL
            )
        )
   );

DELETE FROM bpi.bpi_batch_candidates
 WHERE id IN (SELECT id FROM bpi_joint_candidate_ids);

DELETE FROM bpi.bpi_batch_instances
 WHERE id IN (SELECT id FROM bpi_joint_batch_ids);

DELETE FROM bpi.bpi_outbox_events
 WHERE id IN (SELECT outbox_id FROM bpi_joint_acceptance_ids WHERE outbox_id IS NOT NULL);

UPDATE bpi.bpi_rule_versions
   SET latest_simulation_id = NULL
 WHERE id IN (SELECT rule_id FROM bpi_joint_acceptance_ids);

DELETE FROM bpi.bpi_rule_simulations
 WHERE tenant_id = :'tenant_id'
   AND rule_version_id IN (SELECT rule_id FROM bpi_joint_acceptance_ids);

DELETE FROM bpi.bpi_rule_golden_boundaries
 WHERE tenant_id = :'tenant_id'
   AND created_by = :'marker';

DELETE FROM bpi.bpi_telemetry_points
 WHERE tenant_id = :'tenant_id'
   AND telemetry_event_id IN (
        SELECT id
          FROM bpi.bpi_telemetry_events
         WHERE tenant_id = :'tenant_id'
           AND headers ->> 'acceptance_marker' = :'marker'
   );

DELETE FROM bpi.bpi_telemetry_events
 WHERE tenant_id = :'tenant_id'
   AND headers ->> 'acceptance_marker' = :'marker';

DELETE FROM bpi.bpi_feature_flags
 WHERE tenant_id = :'tenant_id'
   AND updated_by = :'marker';

DELETE FROM bpi.bpi_rule_versions
 WHERE id IN (SELECT rule_id FROM bpi_joint_acceptance_ids);

DELETE FROM bpi.bpi_topology_versions
 WHERE id IN (SELECT topology_id FROM bpi_joint_acceptance_ids);

SELECT json_build_object(
    'status', 'CLEANED',
    'marker', :'marker',
    'remaining', json_build_object(
        'topologies', (SELECT count(*) FROM bpi.bpi_topology_versions WHERE tenant_id = :'tenant_id' AND created_by = :'marker'),
        'rules', (SELECT count(*) FROM bpi.bpi_rule_versions WHERE tenant_id = :'tenant_id' AND created_by = :'marker'),
        'candidates', (SELECT count(*) FROM bpi.bpi_batch_candidates WHERE tenant_id = :'tenant_id' AND order_id = 'MO-' || :'marker'),
        'batches', (SELECT count(*) FROM bpi.bpi_batch_instances WHERE tenant_id = :'tenant_id' AND order_id = 'MO-' || :'marker')
    )
);

COMMIT;
