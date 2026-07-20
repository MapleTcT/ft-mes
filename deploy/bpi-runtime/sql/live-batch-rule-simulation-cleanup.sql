\set ON_ERROR_STOP on

BEGIN;

SELECT 1 / CASE
    WHEN :'marker' ~ '^[A-Za-z0-9_-]{8,96}$' THEN 1
    ELSE 0
END AS marker_guard;

CREATE TEMP TABLE bpi_live_batch_fixture_events ON COMMIT DROP AS
SELECT id
  FROM bpi.bpi_telemetry_events
 WHERE tenant_id = :'tenant_id'
   AND headers ->> 'acceptance_marker' = :'marker'
   AND headers ->> 'fixture_kind' = 'TEST_ONLY_RULE_QUALIFICATION';

DELETE FROM bpi.bpi_telemetry_points
 WHERE tenant_id = :'tenant_id'
   AND telemetry_event_id IN (SELECT id FROM bpi_live_batch_fixture_events);

DELETE FROM bpi.bpi_telemetry_events
 WHERE id IN (SELECT id FROM bpi_live_batch_fixture_events);

DELETE FROM bpi.bpi_rule_golden_boundaries
 WHERE tenant_id = :'tenant_id'
   AND created_by = :'marker'
   AND source_ref LIKE 'test-only-live-batch-rule-qualification:%';

SELECT json_build_object(
    'status', 'CLEANED',
    'fixtureKind', 'TEST_ONLY_RULE_QUALIFICATION',
    'marker', :'marker',
    'remaining', json_build_object(
        'telemetryEvents', (
            SELECT count(*)
              FROM bpi.bpi_telemetry_events
             WHERE tenant_id = :'tenant_id'
               AND headers ->> 'acceptance_marker' = :'marker'
               AND headers ->> 'fixture_kind' = 'TEST_ONLY_RULE_QUALIFICATION'
        ),
        'goldenBoundaries', (
            SELECT count(*)
              FROM bpi.bpi_rule_golden_boundaries
             WHERE tenant_id = :'tenant_id'
               AND created_by = :'marker'
               AND source_ref LIKE 'test-only-live-batch-rule-qualification:%'
        )
    )
);

COMMIT;
