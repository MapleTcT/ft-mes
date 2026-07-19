\set ON_ERROR_STOP on

WITH target_run AS (
    SELECT * FROM bpi.bpi_shadow_runs
     WHERE tenant_id = '1000' AND run_code = :'marker'
), review_metrics AS (
    SELECT count(*) FILTER (WHERE review.state = 'ACTIVE') AS active_reviews,
           sum((review.start_boundary_accepted::integer)
               + (review.end_boundary_accepted::integer))
               FILTER (WHERE review.state = 'ACTIVE') AS accepted_boundaries,
           count(*) FILTER (WHERE review.state = 'ACTIVE') * 2 AS total_boundaries,
           abs(sum(review.automatic_quantity) FILTER (WHERE review.state = 'ACTIVE')
               - sum(review.reference_quantity) FILTER (WHERE review.state = 'ACTIVE'))
               / NULLIF(sum(review.reference_quantity) FILTER (WHERE review.state = 'ACTIVE'), 0)
               * 100 AS cumulative_quantity_deviation_percent
      FROM bpi.bpi_shadow_run_batch_reviews review
      JOIN target_run run ON run.id = review.shadow_run_id AND run.tenant_id = review.tenant_id
), marker_objects AS (
    SELECT id FROM target_run
    UNION ALL SELECT md5(:'marker' || ':incident')::uuid
), flyway AS (
    SELECT max(version::integer) AS version FROM bpi.flyway_schema_history WHERE success
)
SELECT jsonb_pretty(jsonb_build_object(
    'marker', :'marker',
    'flywayVersion', (SELECT version FROM flyway),
    'run', (
        SELECT jsonb_build_object(
            'id', id, 'state', state, 'revision', revision,
            'createdBy', created_by, 'startedBy', started_by,
            'completedBy', completed_by, 'decidedBy', decided_by,
            'minimumDurationDays', minimum_duration_days,
            'minimumReviewedBatches', minimum_reviewed_batches,
            'minimumBoundaryAgreement', minimum_boundary_agreement,
            'quantityTolerancePercent', quantity_tolerance_percent,
            'observedDurationSeconds', floor(extract(epoch FROM (completed_at - started_at)))::bigint
        ) FROM target_run
    ),
    'metrics', (
        SELECT jsonb_build_object(
            'activeReviews', active_reviews,
            'acceptedBoundaries', accepted_boundaries,
            'totalBoundaries', total_boundaries,
            'boundaryAgreement', accepted_boundaries::numeric / NULLIF(total_boundaries, 0),
            'cumulativeQuantityDeviationPercent', cumulative_quantity_deviation_percent
        ) FROM review_metrics
    ),
    'audit', jsonb_build_object(
        'rows', (SELECT count(*) FROM bpi.bpi_audit_events
                  WHERE tenant_id = '1000' AND object_id IN (SELECT id FROM marker_objects)),
        'shadowActions', (SELECT jsonb_agg(action ORDER BY created_at, id)
                          FROM bpi.bpi_audit_events
                          WHERE tenant_id = '1000' AND object_id IN (SELECT id FROM target_run))
    ),
    'idempotency', jsonb_build_object(
        'rows', (SELECT count(*) FROM bpi.bpi_api_idempotency
                  WHERE tenant_id = '1000' AND idempotency_key LIKE :'marker' || '%'),
        'completed200', (SELECT count(*) FROM bpi.bpi_api_idempotency
                          WHERE tenant_id = '1000' AND idempotency_key LIKE :'marker' || '%'
                            AND state = 'COMPLETED' AND response_status = 200)
    ),
    'criticalIncident', (
        SELECT jsonb_build_object('state', state, 'revision', revision,
                                  'acknowledgedBy', acknowledged_by, 'resolvedBy', resolved_by)
          FROM bpi.bpi_data_quality_incidents
         WHERE tenant_id = '1000' AND id = md5(:'marker' || ':incident')::uuid
    ),
    'shadowBatches', jsonb_build_object(
        'rows', (SELECT count(*) FROM bpi.bpi_batch_instances
                  WHERE tenant_id = '1000' AND batch_no LIKE :'marker' || '_BATCH_%'),
        'closedRaw', (SELECT count(*) FROM bpi.bpi_batch_instances
                       WHERE tenant_id = '1000' AND batch_no LIKE :'marker' || '_BATCH_%'
                         AND state = 'CLOSED_RAW'),
        'qualityNotApplicable', (SELECT count(*) FROM bpi.bpi_batch_instances
                                  WHERE tenant_id = '1000' AND batch_no LIKE :'marker' || '_BATCH_%'
                                    AND quality_gate = 'NOT_APPLICABLE'),
        'wmsNotRequested', (SELECT count(*) FROM bpi.bpi_batch_instances
                             WHERE tenant_id = '1000' AND batch_no LIKE :'marker' || '_BATCH_%'
                               AND wms_status = 'NOT_REQUESTED')
    )
));
