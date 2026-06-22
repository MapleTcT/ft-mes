-- Minimal assignee policy for recovered RM formula workflows on PostgreSQL.
--
-- Source evidence:
-- - After registering RM JBPM definitions, RM batch sync reached workflow
--   start but failed with ec.common.workflow.noPowerUsers.
-- - QCS PostgreSQL compatibility uses rbac_flow_permission rows to provide
--   a test-runtime user-task assignee policy when recovered workflow metadata
--   lacks full RBAC distribution data.

WITH flow_seed(process_key, activity_code, offset_id) AS (
    VALUES
        ('formulaEnableFlw', 'TaskEvent_11ysb3i', 301::bigint),
        ('formulaEnableFlw', 'TaskEvent_0k763xk', 302::bigint),
        ('serviceUrl_001', 'TaskEvent_1ishah0_ref121', 301::bigint),
        ('serviceUrl_001', 'TaskEvent_03ddbq6_ref121', 302::bigint)
),
current_flow AS (
    SELECT DISTINCT ON (deployment.process_key)
           deployment.id AS deployment_id,
           deployment.process_key,
           coalesce(deployment.process_version, 1)::text AS flow_version,
           coalesce(deployment.name, deployment.process_key) AS flow_name
      FROM public.wf_deployment deployment
     WHERE deployment.process_key IN ('formulaEnableFlw', 'serviceUrl_001')
       AND coalesce(deployment.valid, 1) = 1
       AND coalesce(deployment.is_current_version, 0) = 1
     ORDER BY deployment.process_key,
              coalesce(deployment.process_version, 0) DESC,
              deployment.id DESC
)
INSERT INTO public.rbac_flow_permission (
    id,
    version,
    modify_time,
    create_time,
    create_staff_id,
    cid,
    entity_code,
    purview_distribution,
    purview_state,
    memo,
    unlimited_power,
    group_power_flag,
    assign_staff_flag,
    assign_pos_flag,
    position_power_flag,
    flow_permission_type,
    type_id,
    activity_code,
    flow_version,
    flow_key,
    flow_name
)
SELECT current_flow.deployment_id + flow_seed.offset_id,
       0,
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP,
       1,
       1000,
       'RM_1.0.0_formula',
       3,
       1,
       '',
       true,
       false,
       false,
       false,
       false,
       'USER',
       1,
       flow_seed.activity_code,
       current_flow.flow_version,
       current_flow.process_key,
       current_flow.flow_name
  FROM current_flow
  JOIN flow_seed
    ON flow_seed.process_key = current_flow.process_key
ON CONFLICT (id) DO UPDATE SET
    modify_time = CURRENT_TIMESTAMP,
    entity_code = EXCLUDED.entity_code,
    purview_distribution = EXCLUDED.purview_distribution,
    purview_state = EXCLUDED.purview_state,
    unlimited_power = EXCLUDED.unlimited_power,
    group_power_flag = EXCLUDED.group_power_flag,
    assign_staff_flag = EXCLUDED.assign_staff_flag,
    assign_pos_flag = EXCLUDED.assign_pos_flag,
    position_power_flag = EXCLUDED.position_power_flag,
    flow_permission_type = EXCLUDED.flow_permission_type,
    type_id = EXCLUDED.type_id,
    activity_code = EXCLUDED.activity_code,
    flow_version = EXCLUDED.flow_version,
    flow_key = EXCLUDED.flow_key,
    flow_name = EXCLUDED.flow_name;

CREATE INDEX IF NOT EXISTS idx_rbac_flow_permission_flow_activity
    ON public.rbac_flow_permission(flow_key, activity_code, purview_state);
