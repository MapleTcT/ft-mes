-- Restore the runtime task/transition metadata for WOM preNeedFlow.
--
-- Source evidence:
-- - WOM_6.1.3.4 service META-INF/init/metadata.json contains wf_task and
--   wf_transition rows for preNeedFlow.
-- - Recovered PostgreSQL has wf_deployment/process_xml, but wf_task is empty,
--   so processService.findStartTransitions(deploymentId) returns no usable
--   start transition and generatePrepareNeed fails before persistence.

WITH current_flow AS (
    SELECT id AS deployment_id,
           coalesce(process_version, 0) AS process_version
      FROM public.wf_deployment
     WHERE process_key = 'preNeedFlow'
       AND coalesce(valid, 1) = 1
       AND coalesce(is_current_version, 0) = 1
     ORDER BY coalesce(process_version, 0) DESC, id DESC
     LIMIT 1
),
task_seed AS (
    SELECT *
      FROM (
        VALUES
            (101, 'start_f78cp11', 'WOM_1.0.0.workflow.randon1591094242492.flag', '开始活动', 1, 'WOM_1.0.0_prepareMaterialNeed_needOrdrCofirmEdit', NULL, 0, 0),
            (102, 'TaskEvent_0jr2cds', 'WOM_1.0.0.workflow.randon1591095054929.flag', '制定', 4, 'WOM_1.0.0_prepareMaterialNeed_needOrdrCofirmEdit', '_blank', 1, 1),
            (103, 'TaskEvent_08drpvj', 'WOM_1.0.0.workflow.randon1591095096388.flag', '审核', 4, 'WOM_1.0.0_prepareMaterialNeed_needCofirmEdit', '_blank', 1, 1),
            (104, 'end_1rchsd1', 'WOM_1.0.0.workflow.randon1591095190550.flag', '结束活动', 2, NULL, NULL, 0, 0),
            (105, 'EndCancelEvent_1u735y5', 'WOM_1.0.0.workflow.randon1591095190561.flag', '作废', 3, NULL, NULL, 0, 0)
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
       'preNeedFlow',
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
     WHERE process_key = 'preNeedFlow'
       AND coalesce(valid, 1) = 1
       AND coalesce(is_current_version, 0) = 1
     ORDER BY coalesce(process_version, 0) DESC, id DESC
     LIMIT 1
),
transition_seed AS (
    SELECT *
      FROM (
        VALUES
            (201, 'SequenceFlow_1qy4q8d', 'WOM_1.0.0.workflow.randon1591094405584.flag', '制定', 'start_f78cp11', 'TaskEvent_0jr2cds', 1),
            (202, 'SequenceFlow_0ctamvz', 'WOM_1.0.0.workflow.randon1591094421200.flag', '工单确认', 'TaskEvent_0jr2cds', 'TaskEvent_08drpvj', 1),
            (203, 'SequenceFlow_1gdbah0', 'WOM_1.0.0.workflow.randon1591094437560.flag', '作废', 'TaskEvent_0jr2cds', 'EndCancelEvent_1u735y5', 3),
            (204, 'SequenceFlow_1m3tfmj', 'WOM_1.0.0.workflow.randon1591094431384.flag', '需求确认', 'TaskEvent_08drpvj', 'end_1rchsd1', 1),
            (205, 'SequenceFlow_1pc1e0q', 'WOM_1.0.0.workflow.randon1591094499202.flag', '驳回', 'TaskEvent_08drpvj', 'TaskEvent_0jr2cds', 2)
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

CREATE INDEX IF NOT EXISTS idx_wf_task_deployment_code
    ON public.wf_task(deployment_id, code);

CREATE INDEX IF NOT EXISTS idx_wf_transition_deployment_from
    ON public.wf_transition(deployment_id, from_node_code);
