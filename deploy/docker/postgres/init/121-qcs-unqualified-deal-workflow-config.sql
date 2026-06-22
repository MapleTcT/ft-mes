-- Enable the recovered QCS product unqualified-deal workflow on PostgreSQL.
--
-- Source evidence:
-- - QCSInspectReportServiceImpl.createUnQlfDeal reads BaseSetWfCustomConfig
--   by code = 'manuUnqlf' for product inspection reports.
-- - QCSUnQlfDealServiceImpl.save starts the JBPM process and writes DealInfo,
--   so wf_task/wf_transition/rbac_flow_permission metadata is required even
--   when the custom config uses BaseSet_flowConfigType/save.
-- - The recovered QCS package defines manuUnQlfDealWorkFlow in module.xml.

WITH current_flow AS (
    SELECT id AS deployment_id,
           coalesce(process_version, 1) AS process_version
      FROM public.wf_deployment
     WHERE process_key = 'manuUnQlfDealWorkFlow'
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
       process_definition_id = 'manuUnQlfDealWorkFlow-' || current_flow.process_version::text,
       deployment_id = '7401000000030000',
       modify_time = CURRENT_TIMESTAMP
  FROM current_flow
 WHERE deployment.process_key = 'manuUnQlfDealWorkFlow';

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
    1210000000000001::bigint,
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
    'QCS_MANU_UNQLF_WORKFLOW_CONFIG',
    'manuUnqlf',
    '产品不合格品处理单',
    'manuUnQlfDealWorkFlow',
    'manuUnqlf',
    'BaseSet_flowConfigType/save',
    1,
    1,
    1,
    0
WHERE NOT EXISTS (
    SELECT 1
      FROM public.baseset_wf_custom_configs existing
     WHERE existing.code = 'manuUnqlf'
);

UPDATE public.baseset_wf_custom_configs
   SET valid = true,
       status = 99,
       name = '产品不合格品处理单',
       deployment_key = 'manuUnQlfDealWorkFlow',
       transaction_key = 'manuUnqlf',
       flow_config_type = 'BaseSet_flowConfigType/save',
       cus_create_staff_id = coalesce(cus_create_staff_id, 1),
       cus_create_dept_id = coalesce(cus_create_dept_id, 1),
       cus_create_pos_id = coalesce(cus_create_pos_id, 1),
       effective_state = coalesce(effective_state, 0),
       modify_time = CURRENT_TIMESTAMP
 WHERE code = 'manuUnqlf';

WITH current_flow AS (
    SELECT id AS deployment_id,
           coalesce(process_version, 1) AS process_version
      FROM public.wf_deployment
     WHERE process_key = 'manuUnQlfDealWorkFlow'
       AND coalesce(valid, 1) = 1
       AND coalesce(is_current_version, 0) = 1
     ORDER BY coalesce(process_version, 0) DESC, id DESC
     LIMIT 1
),
task_seed AS (
    SELECT *
      FROM (
        VALUES
            (101, 'start_dyjf8px', 'QCS.workflow.randon1619602706442.flag', '开始活动', 1, 'QCS_5.0.0.0_unQlfDeal_manuUnQlfDealEdit', NULL, 0, 0),
            (102, 'TaskEvent_18cp6xa', 'QCS_5.0.0.0.workflow.randon1592221382676.flag', '编辑', 4, 'QCS_5.0.0.0_unQlfDeal_manuUnQlfDealEdit', '_blank', 1, 1),
            (103, 'TaskEvent_0ixce5n', 'QCS_5.0.0.0.workflow.randon1592221629652.flag', '审核', 4, 'QCS_5.0.0.0_unQlfDeal_manuUnQlfDealView', '_blank', 1, 1),
            (104, 'end_wkd9on5', 'QCS.workflow.randon1619602743312.flag', '结束活动', 2, NULL, NULL, 0, 0),
            (105, 'EndCancelEvent_0sst61l', 'QCS.workflow.randon1619602743324.flag', '作废', 3, NULL, NULL, 0, 0)
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
       'manuUnQlfDealWorkFlow',
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
     WHERE process_key = 'manuUnQlfDealWorkFlow'
       AND coalesce(valid, 1) = 1
       AND coalesce(is_current_version, 0) = 1
     ORDER BY coalesce(process_version, 0) DESC, id DESC
     LIMIT 1
),
transition_seed AS (
    SELECT *
      FROM (
        VALUES
            (201, 'SequenceFlow_07z3vrn', 'QCS_5.0.0.0.workflow.randon1592207549041.flag', '制定', 'start_dyjf8px', 'TaskEvent_18cp6xa', 1),
            (202, 'SequenceFlow_1kkewv3', 'QCS_5.0.0.0.workflow.randon1588122044745.flag', '审核', 'TaskEvent_18cp6xa', 'TaskEvent_0ixce5n', 1),
            (203, 'SequenceFlow_1df7vqz', 'QCS_5.0.0.0.workflow.randon1588125272223.flag', '作废', 'TaskEvent_18cp6xa', 'EndCancelEvent_0sst61l', 3),
            (204, 'SequenceFlow_1rfxf3h', 'QCS_5.0.0.0.workflow.randon1596437204957.flag', '生效', 'TaskEvent_0ixce5n', 'end_wkd9on5', 1),
            (205, 'SequenceFlow_04k7g7d', 'QCS_5.0.0.0.workflow.randon1592221901944.flag', '驳回', 'TaskEvent_0ixce5n', 'TaskEvent_18cp6xa', 2)
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

WITH current_flow AS (
    SELECT id AS deployment_id,
           coalesce(process_version, 1)::text AS flow_version,
           coalesce(name, 'QCS_5.0.0.0.workflowname.randon1592221527333.flag') AS flow_name
      FROM public.wf_deployment
     WHERE process_key = 'manuUnQlfDealWorkFlow'
       AND coalesce(valid, 1) = 1
       AND coalesce(is_current_version, 0) = 1
     ORDER BY coalesce(process_version, 0) DESC, id DESC
     LIMIT 1
),
permission_seed AS (
    SELECT *
      FROM (
        VALUES
            (301, 'TaskEvent_18cp6xa'),
            (302, 'TaskEvent_0ixce5n')
      ) AS seed(offset_id, activity_code)
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
SELECT current_flow.deployment_id + permission_seed.offset_id,
       0,
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP,
       1,
       1000,
       'QCS_5.0.0.0_unQlfDeal',
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
       permission_seed.activity_code,
       current_flow.flow_version,
       'manuUnQlfDealWorkFlow',
       current_flow.flow_name
  FROM current_flow
 CROSS JOIN permission_seed
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

CREATE INDEX IF NOT EXISTS idx_wf_task_qcs_unqlf_deployment_code
    ON public.wf_task(deployment_id, code);

CREATE INDEX IF NOT EXISTS idx_wf_transition_qcs_unqlf_deployment_from
    ON public.wf_transition(deployment_id, from_node_code);
