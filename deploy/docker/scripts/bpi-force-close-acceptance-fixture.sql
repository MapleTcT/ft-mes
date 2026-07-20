\set ON_ERROR_STOP on

SELECT count(*) AS existing_override
  FROM bpi.bpi_feature_flags
 WHERE tenant_id = '1000'
   AND scope_type = 'PLANT'
   AND scope_key = 'PLANT-01'
   AND flag_key = 'bpi.commands'
\gset

\if :existing_override
    \echo 'PLANT-01 already has a bpi.commands override; acceptance will not replace it'
    \quit 4
\endif

BEGIN;

INSERT INTO bpi.bpi_feature_flags
    (id, tenant_id, scope_type, scope_key, flag_key, enabled, revision,
     updated_by, active, last_reason)
VALUES
    (:'commands_flag_id'::uuid, '1000', 'PLANT', 'PLANT-01', 'bpi.commands', true, 1,
     :'marker', true, :'marker' || ' controlled force-close acceptance');

INSERT INTO bpi.bpi_batch_instances
    (id, tenant_id, plant_id, batch_no, line_id, stage_code, order_id,
     material_code, state, revision, is_shadow, start_time, end_time,
     quantity, quantity_unit, dry_matter, quality_gate, wms_status,
     topology_version_id, rule_version_id, created_by)
SELECT :'batch_id'::uuid, '1000', 'PLANT-01', :'marker', 'LINE-S07-01',
       'EVAPORATION', :'marker' || '_ORDER', :'marker' || '_MATERIAL',
       'ACTIVE', 1, true, now() - interval '2 hours', NULL,
       12.345000, 't', 8.641500, 'NOT_APPLICABLE', 'NOT_REQUESTED',
       topology_version_id, rule_version_id, :'marker'
  FROM bpi.bpi_batch_instances
 WHERE tenant_id = '1000'
   AND topology_version_id IS NOT NULL
   AND rule_version_id IS NOT NULL
 ORDER BY created_at DESC
 LIMIT 1;

SELECT count(*) AS inserted_batch_rows
  FROM bpi.bpi_batch_instances
 WHERE tenant_id = '1000'
   AND id = :'batch_id'::uuid
\gset

\if :inserted_batch_rows
\else
    \echo 'No existing topology/rule pair is available for the acceptance fixture'
    \quit 5
\endif

COMMIT;

SELECT json_build_object(
    'marker', :'marker',
    'batchId', :'batch_id',
    'commandsFlagId', :'commands_flag_id',
    'batchRows', (SELECT count(*) FROM bpi.bpi_batch_instances
                   WHERE tenant_id = '1000' AND id = :'batch_id'::uuid),
    'commandsEnabled', (SELECT enabled AND active FROM bpi.bpi_feature_flags
                         WHERE tenant_id = '1000' AND id = :'commands_flag_id'::uuid),
    'initialState', (SELECT state FROM bpi.bpi_batch_instances
                      WHERE tenant_id = '1000' AND id = :'batch_id'::uuid),
    'initialRevision', (SELECT revision FROM bpi.bpi_batch_instances
                         WHERE tenant_id = '1000' AND id = :'batch_id'::uuid)
);
