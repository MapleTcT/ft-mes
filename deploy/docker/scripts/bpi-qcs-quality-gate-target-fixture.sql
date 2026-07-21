\set ON_ERROR_STOP on

SELECT count(*) AS existing_override
  FROM bpi.bpi_feature_flags
 WHERE tenant_id = '1000'
   AND scope_type = 'LINE'
   AND scope_key = 'LINE-S07-01'
   AND flag_key = 'bpi.qcs-link'
\gset

\if :existing_override
    \echo 'LINE-S07-01 already has a bpi.qcs-link override; acceptance will not replace it'
    \quit 4
\endif

BEGIN;

INSERT INTO bpi.bpi_feature_flags
    (id, tenant_id, scope_type, scope_key, flag_key, enabled, revision,
     updated_by, active, last_reason)
VALUES
    (:'qcs_flag_id'::uuid, '1000', 'LINE', 'LINE-S07-01', 'bpi.qcs-link', true, 1,
     :'marker', true, :'marker' || ' controlled QCS quality-gate acceptance');

INSERT INTO bpi.bpi_batch_instances
    (id, tenant_id, plant_id, batch_no, line_id, stage_code, order_id,
     material_code, state, revision, is_shadow, start_time, end_time,
     quantity, quantity_unit, quality_gate, wms_status,
     topology_version_id, rule_version_id, created_by)
SELECT :'batch_id'::uuid, '1000', 'PLANT-01', :'marker' || '_BPI_BATCH', 'LINE-S07-01',
       'QCS_GATE_ACCEPTANCE', :'order_id', :'material_code',
       'CLOSED_RAW', 1, true, now() - interval '2 hours', now() - interval '1 hour',
       1.000000, 't', 'NOT_APPLICABLE', 'NOT_REQUESTED',
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
    \echo 'No existing topology/rule pair is available for the QCS acceptance fixture'
    \quit 5
\endif

COMMIT;

SELECT json_build_object(
    'marker', :'marker',
    'batchId', :'batch_id',
    'qcsFlagId', :'qcs_flag_id',
    'orderId', :'order_id',
    'materialCode', :'material_code',
    'batchRows', (SELECT count(*) FROM bpi.bpi_batch_instances
                   WHERE tenant_id = '1000' AND id = :'batch_id'::uuid),
    'qcsLinkEnabled', (SELECT enabled AND active FROM bpi.bpi_feature_flags
                       WHERE tenant_id = '1000' AND id = :'qcs_flag_id'::uuid),
    'initialState', (SELECT state FROM bpi.bpi_batch_instances
                     WHERE tenant_id = '1000' AND id = :'batch_id'::uuid),
    'initialRevision', (SELECT revision FROM bpi.bpi_batch_instances
                        WHERE tenant_id = '1000' AND id = :'batch_id'::uuid)
);
