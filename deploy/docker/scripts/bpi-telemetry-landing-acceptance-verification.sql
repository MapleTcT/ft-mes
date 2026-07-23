\set ON_ERROR_STOP on

WITH target_run AS (
    SELECT *
      FROM bpi.bpi_shadow_runs
     WHERE tenant_id = '1000' AND run_code = :'marker'
), target_events AS (
    SELECT event.*
      FROM bpi.bpi_telemetry_events event
     WHERE event.tenant_id = '1000'
       AND event.message_id LIKE :'window_marker' || ':%'
), target_points AS (
    SELECT point.*
      FROM bpi.bpi_telemetry_points point
      JOIN target_events event
        ON event.tenant_id = point.tenant_id
       AND event.id = point.telemetry_event_id
), preheat_events AS (
    SELECT event.*
      FROM bpi.bpi_telemetry_events event
     WHERE event.tenant_id = '1000'
       AND event.message_id LIKE :'preheat_marker' || ':%'
), target_snapshot AS (
    SELECT snapshot.*
      FROM bpi.bpi_point_catalog_snapshots snapshot
      JOIN bpi.bpi_topology_versions topology
        ON topology.tenant_id = snapshot.tenant_id
       AND topology.validated_point_catalog_snapshot_id = snapshot.id
     WHERE topology.tenant_id = '1000'
       AND topology.id = md5(:'marker' || ':topology')::uuid
), telemetry_projection AS (
    SELECT count(DISTINCT entry.id) FILTER (
               WHERE point.id IS NOT NULL)::integer AS observed_point_count,
           count(DISTINCT entry.id) FILTER (
               WHERE point.id IS NOT NULL
                 AND event.sequence_origin = entry.source_sequence_origin
                 AND event.source_epoch > 0
                 AND event.sequence > 0)::integer AS authoritative_sequence_point_count,
           count(DISTINCT entry.id) FILTER (
               WHERE point.id IS NOT NULL
                 AND point.calibration_version = entry.calibration_version)::integer
               AS calibrated_point_count,
           count(DISTINCT entry.id) FILTER (
               WHERE point.id IS NOT NULL
                 AND point.quality_code = 'GOOD')::integer AS good_quality_point_count,
           count(DISTINCT event.id) FILTER (
               WHERE point.id IS NOT NULL
                 AND event.sequence_disposition = 'GAP') AS gap_event_count,
           count(DISTINCT event.id) FILTER (
               WHERE point.id IS NOT NULL
                 AND event.sequence_disposition = 'OUT_OF_ORDER') AS out_of_order_event_count
      FROM target_run run
      JOIN target_snapshot snapshot ON true
      JOIN bpi.bpi_point_catalog_entries entry
        ON entry.tenant_id = snapshot.tenant_id
       AND entry.snapshot_id = snapshot.id
      LEFT JOIN target_events event
        ON event.tenant_id = entry.tenant_id
       AND event.plant_id = entry.plant_id
       AND event.line_id = entry.line_id
       AND event.product_id = entry.product_id
       AND event.device_id = entry.device_id
       AND event.event_time >= run.started_at
       AND event.created_at >= run.started_at
      LEFT JOIN target_points point
        ON point.tenant_id = event.tenant_id
       AND point.telemetry_event_id = event.id
       AND point.property_id = entry.property_id
       AND point.sample_time >= run.started_at
       AND point.created_at >= run.started_at
), flyway AS (
    SELECT max(version::integer) AS version
      FROM bpi.flyway_schema_history
     WHERE success
)
SELECT jsonb_pretty(jsonb_build_object(
    'marker', :'marker',
    'database', 'PostgreSQL',
    'flywayVersion', (SELECT version FROM flyway),
    'run', (
        SELECT jsonb_build_object(
            'id', run.id,
            'state', run.state,
            'revision', run.revision,
            'startedAt', run.started_at,
            'cancelledAt', run.cancelled_at,
            'pointCatalogSnapshotId', run.point_catalog_snapshot_id)
          FROM target_run run
    ),
    'windowIsolation', jsonb_build_object(
        'preheatEventRows', (SELECT count(*) FROM preheat_events),
        'preheatPersistedBeforeRun', (
            SELECT bool_and(event.created_at < run.started_at)
              FROM preheat_events event CROSS JOIN target_run run
        ),
        'windowEventRows', (SELECT count(*) FROM target_events),
        'windowPointRows', (SELECT count(*) FROM target_points),
        'windowPersistedAfterRunStart', (
            SELECT bool_and(event.created_at >= run.started_at)
              FROM target_events event CROSS JOIN target_run run
        )
    ),
    'telemetry', jsonb_build_object(
        'messageIds', (
            SELECT jsonb_agg(event.message_id ORDER BY event.sequence)
              FROM target_events event
        ),
        'sourceEpochs', (
            SELECT jsonb_agg(DISTINCT event.source_epoch ORDER BY event.source_epoch)
              FROM target_events event
        ),
        'sequences', (
            SELECT jsonb_agg(event.sequence ORDER BY event.sequence)
              FROM target_events event
        ),
        'sequenceDispositions', (
            SELECT jsonb_agg(event.sequence_disposition ORDER BY event.sequence)
              FROM target_events event
        ),
        'statuses', (
            SELECT jsonb_agg(event.status ORDER BY event.sequence)
              FROM target_events event
        ),
        'qualities', (
            SELECT jsonb_agg(point.quality_code ORDER BY event.sequence)
              FROM target_points point
              JOIN target_events event
                ON event.tenant_id = point.tenant_id
               AND event.id = point.telemetry_event_id
        ),
        'calibrationVersions', (
            SELECT jsonb_agg(point.calibration_version ORDER BY event.sequence)
              FROM target_points point
              JOIN target_events event
                ON event.tenant_id = point.tenant_id
               AND event.id = point.telemetry_event_id
        ),
        'rejectedPointRows', (
            SELECT count(*)
              FROM bpi.bpi_telemetry_point_rejects reject
              JOIN target_events event
                ON event.tenant_id = reject.tenant_id
               AND event.id = reject.telemetry_event_id
        )
    ),
    'coverageProjection', (
        SELECT to_jsonb(telemetry_projection) FROM telemetry_projection
    ),
    'safety', jsonb_build_object(
        'modelTrainingStarted', false,
        'modelRegistered', false,
        'onlineInferenceEnabled', false,
        'productionActivationAllowed', false,
        'externalWmsWrites', (
            SELECT count(*)
              FROM bpi.bpi_outbox_events outbox
             WHERE outbox.tenant_id = '1000'
               AND outbox.event_type = 'WMS_COMPLETION_INBOUND_COMMAND'
               AND outbox.headers::text LIKE '%' || :'marker' || '%'
        )
    )
));
