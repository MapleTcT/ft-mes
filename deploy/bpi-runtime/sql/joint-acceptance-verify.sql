\set ON_ERROR_STOP on

WITH scoped AS (
    SELECT r.id AS rule_id,
           r.state AS rule_state,
           r.revision AS rule_revision,
           r.latest_simulation_id,
           o.id AS outbox_id,
           o.status AS publication_status,
           o.application_status,
           o.application_event_id,
           o.application_deployment_id
      FROM bpi.bpi_rule_versions r
      LEFT JOIN bpi.bpi_outbox_events o
        ON o.tenant_id = r.tenant_id
       AND o.aggregate_id = r.id
     WHERE r.tenant_id = :'tenant_id'
       AND r.created_by = :'marker'
), candidate AS (
    SELECT c.id,
           c.candidate_key,
           c.state,
           c.revision,
           c.batch_id,
           c.order_id
      FROM bpi.bpi_batch_candidates c
     WHERE c.tenant_id = :'tenant_id'
       AND c.order_id = 'MO-' || :'marker'
), batch AS (
    SELECT b.id,
           b.batch_no,
           b.state,
           b.revision,
           b.is_shadow
      FROM bpi.bpi_batch_instances b
     WHERE b.tenant_id = :'tenant_id'
       AND b.order_id = 'MO-' || :'marker'
)
SELECT json_build_object(
    'marker', :'marker',
    'rule', (SELECT row_to_json(scoped) FROM scoped),
    'candidate', (SELECT row_to_json(candidate) FROM candidate),
    'batch', (SELECT row_to_json(batch) FROM batch),
    'counts', json_build_object(
        'topologies', (SELECT count(*) FROM bpi.bpi_topology_versions WHERE tenant_id = :'tenant_id' AND created_by = :'marker'),
        'rules', (SELECT count(*) FROM bpi.bpi_rule_versions WHERE tenant_id = :'tenant_id' AND created_by = :'marker'),
        'simulations', (SELECT count(*) FROM bpi.bpi_rule_simulations s JOIN bpi.bpi_rule_versions r ON r.id = s.rule_version_id WHERE r.tenant_id = :'tenant_id' AND r.created_by = :'marker'),
        'outbox', (SELECT count(*) FROM bpi.bpi_outbox_events o JOIN bpi.bpi_rule_versions r ON r.id = o.aggregate_id WHERE r.tenant_id = :'tenant_id' AND r.created_by = :'marker'),
        'candidates', (SELECT count(*) FROM candidate),
        'batches', (SELECT count(*) FROM batch),
        'boundaryEvidence', (SELECT count(*) FROM bpi.bpi_boundary_evidence e JOIN batch b ON b.id = e.batch_id),
        'stateEvents', (SELECT count(*) FROM bpi.bpi_batch_state_events e JOIN batch b ON b.id = e.batch_id),
        'auditEvents', (SELECT count(*) FROM bpi.bpi_audit_events a WHERE a.tenant_id = :'tenant_id' AND (a.object_id IN (SELECT rule_id FROM scoped) OR a.object_id IN (SELECT id FROM candidate) OR a.object_id IN (SELECT id FROM batch)))
    )
);
