-- Seed the minimal QCS workflow configuration required by WOM manufacturing
-- inspection creation on PostgreSQL.
--
-- Source evidence:
-- - QCSInspectServiceImpl.submitInspectAndInspectStdListByWfCustomConfig
--   reads BaseSetWfCustomConfig by code = 'manuInspect' when tableType = 'manu'.
-- - It then calls taskService.getCurrentDeployment(deploymentKey), which
--   requires wf_deployment.is_current_version = 1.

WITH ranked_workflow AS (
    SELECT
        id,
        process_key,
        row_number() OVER (
            PARTITION BY process_key
            ORDER BY coalesce(process_version, 0) DESC, id DESC
        ) AS rn
    FROM public.wf_deployment
    WHERE process_key IN (
        'manuInspectWorkFlow',
        'manuInspReleaseWorkflow',
        'manuInspectReportWorkFlow',
        'manuUnQlfDealWorkFlow'
    )
      AND coalesce(valid, 1) = 1
)
UPDATE public.wf_deployment deployment
SET is_current_version = CASE
    WHEN ranked_workflow.rn = 1 THEN 1
    ELSE 0
END
FROM ranked_workflow
WHERE deployment.id = ranked_workflow.id;

CREATE INDEX IF NOT EXISTS idx_baseset_wf_custom_configs_code_valid
    ON public.baseset_wf_custom_configs(code, valid);

INSERT INTO public.baseset_wf_custom_configs (
    id,
    version,
    valid,
    cid,
    create_staff_id,
    create_time,
    create_department_id,
    create_position_id,
    group_id,
    owner_staff_id,
    owner_department_id,
    owner_position_id,
    position_lay_rec,
    status,
    table_no,
    code,
    name,
    deployment_key,
    transaction_key,
    flow_config_type,
    cus_create_staff_id,
    cus_create_dept_id,
    cus_create_pos_id,
    effective_state
)
SELECT
    1100000000000001::bigint,
    0,
    true,
    1000,
    1,
    CURRENT_TIMESTAMP,
    1,
    1,
    1000,
    1,
    1,
    1,
    '1',
    99,
    'QCS_MANU_INSPECT_WORKFLOW_CONFIG',
    'manuInspect',
    '制造请检保存配置',
    'manuInspectWorkFlow',
    'manuInspect',
    'BaseSet_flowConfigType/save',
    1,
    1,
    1,
    0
WHERE NOT EXISTS (
    SELECT 1
    FROM public.baseset_wf_custom_configs existing
    WHERE existing.code = 'manuInspect'
);

UPDATE public.baseset_wf_custom_configs
SET
    valid = true,
    deployment_key = 'manuInspectWorkFlow',
    transaction_key = coalesce(transaction_key, 'manuInspect'),
    flow_config_type = coalesce(flow_config_type, 'BaseSet_flowConfigType/save'),
    cus_create_staff_id = coalesce(cus_create_staff_id, 1),
    cus_create_dept_id = coalesce(cus_create_dept_id, 1),
    cus_create_pos_id = coalesce(cus_create_pos_id, 1),
    modify_time = CURRENT_TIMESTAMP
WHERE code = 'manuInspect';
