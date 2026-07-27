\set ON_ERROR_STOP on

BEGIN;

DELETE FROM bpi.bpi_rule_golden_boundaries
 WHERE tenant_id = '1000'
   AND golden_set_id = :'marker' || '_GOLDEN';

DELETE FROM bpi.bpi_telemetry_events
 WHERE tenant_id = '1000'
   AND gateway_id = :'marker' || '_SIM_GATEWAY';

COMMIT;

SELECT jsonb_build_object(
    'marker', :'marker',
    'remainingGolden', (
        SELECT count(*)
          FROM bpi.bpi_rule_golden_boundaries
         WHERE tenant_id = '1000'
           AND golden_set_id = :'marker' || '_GOLDEN'
    ),
    'remainingEvents', (
        SELECT count(*)
          FROM bpi.bpi_telemetry_events
         WHERE tenant_id = '1000'
           AND gateway_id = :'marker' || '_SIM_GATEWAY'
    )
);
