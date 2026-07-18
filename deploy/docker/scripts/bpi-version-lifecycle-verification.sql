\set ON_ERROR_STOP on

SELECT jsonb_pretty(jsonb_build_object(
    'marker', :'marker',
    'database', current_database(),
    'flywayVersion', (
        SELECT max(installed_rank)
          FROM bpi.flyway_schema_history
         WHERE success
    ),
    'rules', COALESCE((
        SELECT jsonb_agg(jsonb_build_object(
            'id', id,
            'code', rule_code,
            'version', version,
            'state', state,
            'revision', revision,
            'latestSimulationId', latest_simulation_id,
            'createdBy', created_by,
            'updatedBy', updated_by
        ) ORDER BY rule_code, version)
          FROM bpi.bpi_rule_versions
         WHERE tenant_id = '1000'
           AND rule_code LIKE :'marker' || '%'
    ), '[]'::jsonb),
    'approvals', COALESCE((
        SELECT jsonb_agg(jsonb_build_object(
            'id', approval.id,
            'ruleId', approval.rule_version_id,
            'state', approval.state,
            'revision', approval.revision,
            'simulationId', approval.simulation_id,
            'submittedBy', approval.submitted_by,
            'decidedBy', approval.decided_by,
            'submitReason', approval.submit_reason,
            'decisionReason', approval.decision_reason
        ) ORDER BY approval.submitted_at)
          FROM bpi.bpi_rule_approval_requests approval
          JOIN bpi.bpi_rule_versions rule ON rule.id = approval.rule_version_id
         WHERE approval.tenant_id = '1000'
           AND rule.rule_code LIKE :'marker' || '%'
    ), '[]'::jsonb),
    'simulations', COALESCE((
        SELECT jsonb_agg(jsonb_build_object(
            'id', simulation.id,
            'ruleId', simulation.rule_version_id,
            'state', simulation.state,
            'checksum', simulation.checksum,
            'metrics', simulation.metrics,
            'inputManifest', simulation.input_manifest,
            'createdBy', simulation.created_by
        ) ORDER BY simulation.created_at)
          FROM bpi.bpi_rule_simulations simulation
          JOIN bpi.bpi_rule_versions rule ON rule.id = simulation.rule_version_id
         WHERE simulation.tenant_id = '1000'
           AND rule.rule_code LIKE :'marker' || '%'
    ), '[]'::jsonb),
    'audits', COALESCE((
        SELECT jsonb_agg(jsonb_build_object(
            'objectId', audit.object_id,
            'action', audit.action,
            'actorId', audit.actor_id,
            'beforeRevision', audit.before_revision,
            'afterRevision', audit.after_revision,
            'reason', audit.reason,
            'traceId', audit.trace_id
        ) ORDER BY audit.created_at, audit.id)
          FROM bpi.bpi_audit_events audit
          JOIN bpi.bpi_rule_versions rule ON rule.id = audit.object_id
         WHERE audit.tenant_id = '1000'
           AND audit.object_type = 'RULE_VERSION'
           AND rule.rule_code LIKE :'marker' || '%'
    ), '[]'::jsonb),
    'idempotency', COALESCE((
        SELECT jsonb_agg(jsonb_build_object(
            'key', idempotency_key,
            'method', method,
            'resourcePath', resource_path,
            'state', state,
            'responseStatus', response_status
        ) ORDER BY created_at, id)
         FROM bpi.bpi_api_idempotency
         WHERE tenant_id = '1000'
           AND (
               idempotency_key LIKE :'marker' || '%'
               OR resource_path LIKE '%' || md5(:'marker' || ':rule-publish')::uuid::text || '%'
               OR resource_path LIKE '%' || md5(:'marker' || ':rule-reject')::uuid::text || '%'
           )
    ), '[]'::jsonb),
    'candidates', COALESCE((
        SELECT jsonb_agg(jsonb_build_object(
            'id', candidate.id,
            'candidateKey', candidate.candidate_key,
            'state', candidate.state,
            'revision', candidate.revision,
            'boundaryType', candidate.boundary_type,
            'orderId', candidate.order_id,
            'ruleCode', rule.rule_code,
            'ruleVersion', rule.version,
            'ruleState', rule.state,
            'topologyCode', topology.topology_code,
            'topologyVersion', topology.version,
            'topologyState', topology.state
        ) ORDER BY candidate.created_at, candidate.id)
          FROM bpi.bpi_batch_candidates candidate
          JOIN bpi.bpi_rule_versions rule ON rule.id = candidate.rule_version_id
          JOIN bpi.bpi_topology_versions topology ON topology.id = candidate.topology_version_id
         WHERE candidate.tenant_id = '1000'
           AND (
               rule.rule_code LIKE :'marker' || '%'
               OR topology.topology_code LIKE :'marker' || '%'
               OR candidate.order_id LIKE 'MO-' || :'marker' || '%'
           )
    ), '[]'::jsonb),
    'candidateInbox', COALESCE((
        SELECT jsonb_agg(jsonb_build_object(
            'id', inbox.id,
            'eventId', inbox.event_id,
            'idempotencyKey', inbox.idempotency_key,
            'source', inbox.source,
            'receivedAt', inbox.received_at,
            'processedAt', inbox.processed_at
        ) ORDER BY inbox.received_at, inbox.id)
          FROM bpi.bpi_inbox_events inbox
         WHERE inbox.tenant_id = '1000'
           AND inbox.idempotency_key IN (
               SELECT candidate.candidate_key::text
                 FROM bpi.bpi_batch_candidates candidate
                 JOIN bpi.bpi_rule_versions rule ON rule.id = candidate.rule_version_id
                 JOIN bpi.bpi_topology_versions topology ON topology.id = candidate.topology_version_id
                WHERE candidate.tenant_id = '1000'
                  AND (
                      rule.rule_code LIKE :'marker' || '%'
                      OR topology.topology_code LIKE :'marker' || '%'
                      OR candidate.order_id LIKE 'MO-' || :'marker' || '%'
                  )
           )
    ), '[]'::jsonb),
    'outbox', COALESCE((
        SELECT jsonb_agg(jsonb_build_object(
            'id', outbox.id,
            'ruleId', outbox.aggregate_id,
            'eventType', outbox.event_type,
            'topic', outbox.topic,
            'status', outbox.status,
            'attemptCount', outbox.attempt_count,
            'totalAttemptCount', outbox.total_attempt_count
        ) ORDER BY outbox.created_at, outbox.id)
          FROM bpi.bpi_outbox_events outbox
          JOIN bpi.bpi_rule_versions rule ON rule.id = outbox.aggregate_id
         WHERE outbox.tenant_id = '1000'
           AND rule.rule_code LIKE :'marker' || '%'
    ), '[]'::jsonb)
));
