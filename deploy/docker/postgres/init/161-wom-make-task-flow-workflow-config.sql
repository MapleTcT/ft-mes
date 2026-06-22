-- Enable the recovered WOM manufacturing task workflow on PostgreSQL.
--
-- Source evidence:
-- - WOMFormCreatServiceImpl.produceWfConfig is hard-coded to 'manufacture'
--   before it converts the daily-plan payload into WOMProduceTask rows.
-- - The recovered WOM SQL Server init seeds the manufacturing workflow config
--   as deployment_key/code 'makeTaskFlow' with name '生产指令单'.
-- - The recovered module.xml defines processKey 'makeTaskFlow', but the
--   PostgreSQL runtime currently has wf_deployment only; wf_task,
--   wf_transition, and the BaseSet workflow config rows are absent.

WITH current_flow AS (
    SELECT id AS deployment_id,
           coalesce(process_version, 0) AS process_version
      FROM public.wf_deployment
     WHERE process_key = 'makeTaskFlow'
       AND coalesce(valid, 1) = 1
     ORDER BY coalesce(is_current_version, 0) DESC,
              coalesce(process_version, 0) DESC,
              id DESC
     LIMIT 1
)
UPDATE public.wf_deployment deployment
   SET is_current_version = CASE
       WHEN deployment.id = current_flow.deployment_id THEN 1
       ELSE 0
   END,
       valid = 1,
       process_version = current_flow.process_version,
       modify_time = CURRENT_TIMESTAMP
  FROM current_flow
 WHERE deployment.process_key = 'makeTaskFlow';

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
    seed.id,
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
    seed.table_no,
    seed.code,
    seed.name,
    'makeTaskFlow',
    seed.code,
    'BaseSet_flowConfigType/save',
    1,
    1,
    1,
    0
FROM (
    VALUES
        (1610000000000001::bigint, 'manufacture', '生产指令单', 'WOM_MANUFACTURE_WORKFLOW_CONFIG'),
        (1610000000000002::bigint, 'makeTaskFlow', '生产指令单', 'WOM_MAKE_TASK_FLOW_CONFIG')
) AS seed(id, code, name, table_no)
WHERE NOT EXISTS (
    SELECT 1
      FROM public.baseset_wf_custom_configs existing
     WHERE existing.code = seed.code
);

UPDATE public.baseset_wf_custom_configs
   SET valid = true,
       status = 99,
       name = '生产指令单',
       deployment_key = 'makeTaskFlow',
       flow_config_type = 'BaseSet_flowConfigType/save',
       cus_create_staff_id = coalesce(cus_create_staff_id, 1),
       cus_create_dept_id = coalesce(cus_create_dept_id, 1),
       cus_create_pos_id = coalesce(cus_create_pos_id, 1),
       effective_state = coalesce(effective_state, 0),
       modify_time = CURRENT_TIMESTAMP
 WHERE code IN ('manufacture', 'makeTaskFlow');

WITH current_flow AS (
    SELECT id AS deployment_id,
           coalesce(process_version, 0) AS process_version
      FROM public.wf_deployment
     WHERE process_key = 'makeTaskFlow'
       AND coalesce(valid, 1) = 1
       AND coalesce(is_current_version, 0) = 1
     ORDER BY coalesce(process_version, 0) DESC, id DESC
     LIMIT 1
),
task_seed AS (
    SELECT *
      FROM (
        VALUES
            (101, 'start_dhbmq3g', 'WOM_1.0.0.workflow.randon1575443224771.flag', '开始活动', 1, 'WOM_1.0.0_produceTask_makeTaskEdit', NULL, 0, 0),
            (102, 'TaskEvent_0il1mab', 'WOM_1.0.0.workflow.randon1575443257288.flag', '编辑', 4, 'WOM_1.0.0_produceTask_makeTaskEdit', '_blank', 1, 1),
            (103, 'TaskEvent_023irnk', 'WOM_1.0.0.workflow.randon1575443317727.flag', '审批', 4, 'WOM_1.0.0_produceTask_makeTaskSubmitView', '_blank', 1, 1),
            (104, 'end_v7ufaij', 'WOM_1.0.0.workflow.randon1575443398527.flag', '结束活动', 2, NULL, NULL, 0, 0),
            (105, 'EndCancelEvent_08r52dp', 'WOM_1.0.0.workflow.randon1575443398538.flag', '作废', 3, NULL, NULL, 0, 0)
      ) AS seed(offset_id, code, name, name_zh_cn, type, view_code, open_mode, mobile_approve, recall_able)
)
INSERT INTO public.wf_task (
    id,
    version,
    valid,
    ignore_permission,
    web_signet_flag,
    deal_set,
    is_allow_proxy,
    show_in_simple_dealinfo,
    mobile_approve,
    recall_able,
    process_version,
    process_key,
    open_mode,
    view_code,
    type,
    deployment_id,
    code,
    name_zh_cn,
    name,
    cid
)
SELECT current_flow.deployment_id + task_seed.offset_id,
       0,
       1,
       0,
       0,
       0,
       1,
       1,
       task_seed.mobile_approve,
       task_seed.recall_able,
       current_flow.process_version,
       'makeTaskFlow',
       task_seed.open_mode,
       task_seed.view_code,
       task_seed.type,
       current_flow.deployment_id,
       task_seed.code,
       task_seed.name_zh_cn,
       task_seed.name,
       1000
  FROM current_flow
 CROSS JOIN task_seed
ON CONFLICT (id) DO UPDATE SET
    valid = EXCLUDED.valid,
    deployment_id = EXCLUDED.deployment_id,
    process_key = EXCLUDED.process_key,
    process_version = EXCLUDED.process_version,
    code = EXCLUDED.code,
    name = EXCLUDED.name,
    name_zh_cn = EXCLUDED.name_zh_cn,
    type = EXCLUDED.type,
    view_code = EXCLUDED.view_code,
    open_mode = EXCLUDED.open_mode,
    mobile_approve = EXCLUDED.mobile_approve,
    recall_able = EXCLUDED.recall_able;

WITH current_flow AS (
    SELECT id AS deployment_id
      FROM public.wf_deployment
     WHERE process_key = 'makeTaskFlow'
       AND coalesce(valid, 1) = 1
       AND coalesce(is_current_version, 0) = 1
     ORDER BY coalesce(process_version, 0) DESC, id DESC
     LIMIT 1
),
transition_seed AS (
    SELECT *
      FROM (
        VALUES
            (201, 'SequenceFlow_1b2c4zg', 'WOM_1.0.0.workflow.randon1575443252841.flag', '开始', 'start_dhbmq3g', 'TaskEvent_0il1mab', 1),
            (202, 'SequenceFlow_1p2u071', 'WOM_1.0.0.workflow.randon1575443294828.flag', '作废', 'TaskEvent_0il1mab', 'EndCancelEvent_08r52dp', 3),
            (203, 'SequenceFlow_00a9xaa', 'WOM_1.0.0.workflow.randon1575443398549.flag', '下推车间', 'TaskEvent_0il1mab', 'TaskEvent_023irnk', 1),
            (204, 'SequenceFlow_0vcn8hp', 'WOM_1.0.0.workflow.randon1575443361321.flag', '生效', 'TaskEvent_023irnk', 'end_v7ufaij', 1),
            (205, 'SequenceFlow_0libf0v', 'WOM_1.0.0.workflow.randon1575443374113.flag', '驳回2', 'TaskEvent_023irnk', 'TaskEvent_0il1mab', 2)
      ) AS seed(offset_id, code, name, name_zh_cn, from_node_code, to_node_code, type)
)
INSERT INTO public.wf_transition (
    id,
    version,
    valid,
    default_staff,
    required_staff,
    select_staff,
    deployment_id,
    to_node_code,
    from_node_code,
    type,
    code,
    name_zh_cn,
    name
)
SELECT current_flow.deployment_id + transition_seed.offset_id,
       0,
       1,
       0,
       0,
       '0',
       current_flow.deployment_id,
       transition_seed.to_node_code,
       transition_seed.from_node_code,
       transition_seed.type,
       transition_seed.code,
       transition_seed.name_zh_cn,
       transition_seed.name
  FROM current_flow
 CROSS JOIN transition_seed
ON CONFLICT (id) DO UPDATE SET
    valid = EXCLUDED.valid,
    deployment_id = EXCLUDED.deployment_id,
    to_node_code = EXCLUDED.to_node_code,
    from_node_code = EXCLUDED.from_node_code,
    type = EXCLUDED.type,
    code = EXCLUDED.code,
    name_zh_cn = EXCLUDED.name_zh_cn,
    name = EXCLUDED.name,
    default_staff = EXCLUDED.default_staff,
    required_staff = EXCLUDED.required_staff,
    select_staff = EXCLUDED.select_staff;

CREATE INDEX IF NOT EXISTS idx_wf_task_make_task_flow_deployment_code
    ON public.wf_task(deployment_id, code);

CREATE INDEX IF NOT EXISTS idx_wf_transition_make_task_flow_deployment_from
    ON public.wf_transition(deployment_id, from_node_code);
