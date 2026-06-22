-- Register recovered QCS workflow XML with the legacy JBPM runtime.
--
-- Source evidence:
-- - WOMProduceTaskServiceImpl.createManuInspect delegates to
--   QCSInspectServiceImpl.submitInspectAndInspectStdListByWfCustomConfig.
-- - The QCS save path starts a JBPM process via InstanceServiceImpl.start.
-- - Recovered PostgreSQL already contains QCS wf_deployment.process_xml, but
--   jbpm4_deployment/jbpm4_deployprop/jbpm4_lob only contained preNeedFlow.
--   The runtime then returned ec.workflow.noProcessDefinition for
--   manuInspectWorkFlow.

WITH workflow_seed(process_key, deployment_dbid) AS (
    VALUES
        ('manuInspectWorkFlow', 7401000000000000::bigint),
        ('manuInspReleaseWorkflow', 7401000000010000::bigint),
        ('manuInspectReportWorkFlow', 7401000000020000::bigint),
        ('manuUnQlfDealWorkFlow', 7401000000030000::bigint)
),
current_flow AS (
    SELECT DISTINCT ON (deployment.process_key)
           deployment.id,
           deployment.process_key,
           deployment.name,
           workflow_seed.deployment_dbid,
           greatest(coalesce(deployment.process_version, 0), 1) AS process_version
      FROM public.wf_deployment deployment
      JOIN workflow_seed
        ON workflow_seed.process_key = deployment.process_key
     WHERE coalesce(deployment.valid, 1) = 1
       AND deployment.process_xml IS NOT NULL
     ORDER BY deployment.process_key,
              coalesce(deployment.is_current_version, 0) DESC,
              coalesce(deployment.process_version, 0) DESC,
              deployment.id DESC
)
UPDATE public.wf_deployment deployment
   SET process_version = current_flow.process_version,
       process_definition_id = current_flow.process_key || '-' || current_flow.process_version::text,
       deployment_id = current_flow.deployment_dbid::text,
       is_current_version = 1,
       valid = 1,
       modify_time = CURRENT_TIMESTAMP
  FROM current_flow
 WHERE deployment.id = current_flow.id;

WITH workflow_seed(process_key, deployment_dbid) AS (
    VALUES
        ('manuInspectWorkFlow', 7401000000000000::bigint),
        ('manuInspReleaseWorkflow', 7401000000010000::bigint),
        ('manuInspectReportWorkFlow', 7401000000020000::bigint),
        ('manuUnQlfDealWorkFlow', 7401000000030000::bigint)
),
current_flow AS (
    SELECT DISTINCT ON (deployment.process_key)
           deployment.process_key,
           workflow_seed.deployment_dbid
      FROM public.wf_deployment deployment
      JOIN workflow_seed
        ON workflow_seed.process_key = deployment.process_key
     WHERE coalesce(deployment.valid, 1) = 1
       AND coalesce(deployment.is_current_version, 0) = 1
       AND deployment.process_xml IS NOT NULL
     ORDER BY deployment.process_key,
              coalesce(deployment.process_version, 0) DESC,
              deployment.id DESC
)
INSERT INTO public.jbpm4_deployment (
    dbid_,
    name_,
    timestamp_,
    state_
)
SELECT current_flow.deployment_dbid,
       '',
       0,
       'active'
  FROM current_flow
ON CONFLICT (dbid_) DO UPDATE SET
    state_ = EXCLUDED.state_;

WITH workflow_seed(process_key, deployment_dbid) AS (
    VALUES
        ('manuInspectWorkFlow', 7401000000000000::bigint),
        ('manuInspReleaseWorkflow', 7401000000010000::bigint),
        ('manuInspectReportWorkFlow', 7401000000020000::bigint),
        ('manuUnQlfDealWorkFlow', 7401000000030000::bigint)
),
current_flow AS (
    SELECT DISTINCT ON (deployment.process_key)
           deployment.process_key,
           workflow_seed.deployment_dbid,
           greatest(coalesce(deployment.process_version, 0), 1) AS process_version
      FROM public.wf_deployment deployment
      JOIN workflow_seed
        ON workflow_seed.process_key = deployment.process_key
     WHERE coalesce(deployment.valid, 1) = 1
       AND coalesce(deployment.is_current_version, 0) = 1
       AND deployment.process_xml IS NOT NULL
     ORDER BY deployment.process_key,
              coalesce(deployment.process_version, 0) DESC,
              deployment.id DESC
),
deployprop_seed AS (
    SELECT current_flow.deployment_dbid + 1 AS dbid_,
           current_flow.deployment_dbid AS deployment_,
           current_flow.process_key AS objname_,
           'langid' AS key_,
           'jpdl-4.4' AS stringval_,
           NULL::bigint AS longval_
      FROM current_flow
    UNION ALL
    SELECT current_flow.deployment_dbid + 2,
           current_flow.deployment_dbid,
           current_flow.process_key,
           'pdid',
           current_flow.process_key || '-' || current_flow.process_version::text,
           NULL::bigint
      FROM current_flow
    UNION ALL
    SELECT current_flow.deployment_dbid + 3,
           current_flow.deployment_dbid,
           current_flow.process_key,
           'pdkey',
           current_flow.process_key,
           NULL::bigint
      FROM current_flow
    UNION ALL
    SELECT current_flow.deployment_dbid + 4,
           current_flow.deployment_dbid,
           current_flow.process_key,
           'pdversion',
           NULL::text,
           current_flow.process_version::bigint
      FROM current_flow
)
INSERT INTO public.jbpm4_deployprop (
    dbid_,
    deployment_,
    objname_,
    key_,
    stringval_,
    longval_
)
SELECT dbid_,
       deployment_,
       objname_,
       key_,
       stringval_,
       longval_
  FROM deployprop_seed
ON CONFLICT (dbid_) DO UPDATE SET
    deployment_ = EXCLUDED.deployment_,
    objname_ = EXCLUDED.objname_,
    key_ = EXCLUDED.key_,
    stringval_ = EXCLUDED.stringval_,
    longval_ = EXCLUDED.longval_;

WITH workflow_seed(process_key, deployment_dbid) AS (
    VALUES
        ('manuInspectWorkFlow', 7401000000000000::bigint),
        ('manuInspReleaseWorkflow', 7401000000010000::bigint),
        ('manuInspectReportWorkFlow', 7401000000020000::bigint),
        ('manuUnQlfDealWorkFlow', 7401000000030000::bigint)
),
current_flow AS (
    SELECT DISTINCT ON (deployment.process_key)
           deployment.process_key,
           deployment.name,
           workflow_seed.deployment_dbid,
           convert_from(lo_get(deployment.process_xml), 'UTF8') AS process_xml_text
      FROM public.wf_deployment deployment
      JOIN workflow_seed
        ON workflow_seed.process_key = deployment.process_key
     WHERE coalesce(deployment.valid, 1) = 1
       AND coalesce(deployment.is_current_version, 0) = 1
       AND deployment.process_xml IS NOT NULL
     ORDER BY deployment.process_key,
              coalesce(deployment.process_version, 0) DESC,
              deployment.id DESC
)
INSERT INTO public.jbpm4_lob (
    dbid_,
    dbversion_,
    blob_value_,
    deployment_,
    name_
)
SELECT current_flow.deployment_dbid + 10,
       0,
       lo_from_bytea(0, convert_to(current_flow.process_xml_text, 'UTF8')),
       current_flow.deployment_dbid,
       coalesce(nullif(current_flow.name, ''), current_flow.process_key) || '.jpdl.xml'
  FROM current_flow
ON CONFLICT (dbid_) DO UPDATE SET
    dbversion_ = EXCLUDED.dbversion_,
    blob_value_ = EXCLUDED.blob_value_,
    deployment_ = EXCLUDED.deployment_,
    name_ = EXCLUDED.name_;

-- Restore the runtime task/transition metadata used when saving a product
-- manufacturing inspection request.
WITH current_flow AS (
    SELECT id AS deployment_id,
           coalesce(process_version, 1) AS process_version
      FROM public.wf_deployment
     WHERE process_key = 'manuInspectWorkFlow'
       AND coalesce(valid, 1) = 1
       AND coalesce(is_current_version, 0) = 1
     ORDER BY coalesce(process_version, 0) DESC, id DESC
     LIMIT 1
),
task_seed AS (
    SELECT *
      FROM (
        VALUES
            (101, 'start_fwswhbf', 'QCS.workflow.randon1619600129175.flag', '开始活动', 1, 'QCS_5.0.0.0_inspect_manuInspectEdit', NULL, 0, 0),
            (102, 'TaskEvent_1b1kzjf', 'QCS_5.0.0.0.workflow.randon1592221382676.flag', '编辑', 4, 'QCS_5.0.0.0_inspect_manuInspectEdit', '_blank', 1, 1),
            (103, 'TaskEvent_1lwqfuu', 'QCS_5.0.0.0.workflow.randon1588122044745.flag', '审核', 4, 'QCS_5.0.0.0_inspect_manuInspectView', '_blank', 1, 1),
            (104, 'end_ssmuhvm', 'QCS.workflow.randon1619600258836.flag', '结束活动', 2, NULL, NULL, 0, 0),
            (105, 'EndCancelEvent_1lgfmb4', 'QCS.workflow.randon1619600258848.flag', '作废', 3, NULL, NULL, 0, 0)
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
       'manuInspectWorkFlow',
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
     WHERE process_key = 'manuInspectWorkFlow'
       AND coalesce(valid, 1) = 1
       AND coalesce(is_current_version, 0) = 1
     ORDER BY coalesce(process_version, 0) DESC, id DESC
     LIMIT 1
),
transition_seed AS (
    SELECT *
      FROM (
        VALUES
            (201, 'SequenceFlow_1e3h4qj', 'QCS_5.0.0.0.workflow.randon1592207549041.flag', '制定', 'start_fwswhbf', 'TaskEvent_1b1kzjf', 1),
            (202, 'SequenceFlow_0c6e6n3', 'QCS_5.0.0.0.workflow.randon1588122044745.flag', '审核', 'TaskEvent_1b1kzjf', 'TaskEvent_1lwqfuu', 1),
            (203, 'SequenceFlow_1n93si1', 'QCS_5.0.0.0.workflow.randon1588125272223.flag', '作废', 'TaskEvent_1b1kzjf', 'EndCancelEvent_1lgfmb4', 3),
            (204, 'SequenceFlow_00vxp5u', 'QCS_5.0.0.0.workflow.randon1596437204957.flag', '生效', 'TaskEvent_1lwqfuu', 'end_ssmuhvm', 1),
            (205, 'SequenceFlow_0ultem7', 'QCS_5.0.0.0.workflow.randon1592221901944.flag', '驳回', 'TaskEvent_1lwqfuu', 'TaskEvent_1b1kzjf', 2)
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

-- Give the recovered QCS user-task nodes a minimal runtime assignee policy.
--
-- The XML assignment handler has inputorFlag/leaderFlag/staffIds all empty,
-- so WorkflowTaskServiceImpl falls back to BASE_DATAPERMISSION.  Without
-- rbac_flow_permission rows, the first user task throws
-- ec.common.workflow.noPowerUsers before the QCS inspection can be persisted.
WITH current_flow AS (
    SELECT id AS deployment_id,
           coalesce(process_version, 1)::text AS flow_version,
           coalesce(name, 'QCS_5.0.0.0.workflowname.randon1591083128045.flag') AS flow_name
      FROM public.wf_deployment
     WHERE process_key = 'manuInspectWorkFlow'
       AND coalesce(valid, 1) = 1
       AND coalesce(is_current_version, 0) = 1
     ORDER BY coalesce(process_version, 0) DESC, id DESC
     LIMIT 1
),
permission_seed AS (
    SELECT *
      FROM (
        VALUES
            (301, 'TaskEvent_1b1kzjf'),
            (302, 'TaskEvent_1lwqfuu')
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
       'QCS_5.0.0.0_inspect',
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
       'manuInspectWorkFlow',
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

-- Restore the runtime task/transition metadata used by the auto-generated
-- product inspection report workflow. QCSInspectReportServiceImpl starts this
-- flow from createQcsReportByInspet; without the recovered task metadata and
-- flow permissions, TaskServiceImpl cannot resolve the next assignee and raises
-- "ec.common.workflow.noPowerUsers".
WITH current_flow AS (
    SELECT id AS deployment_id,
           coalesce(process_version, 1) AS process_version
      FROM public.wf_deployment
     WHERE process_key = 'manuInspectReportWorkFlow'
       AND coalesce(valid, 1) = 1
       AND coalesce(is_current_version, 0) = 1
     ORDER BY coalesce(process_version, 0) DESC, id DESC
     LIMIT 1
),
task_seed AS (
    SELECT *
      FROM (
        VALUES
            (101, 'start_a9pr4ij', 'QCS.workflow.randon1619601838846.flag', '开始活动', 1, 'QCS_5.0.0.0_inspectReport_manuInspReportEdit', NULL, 0, 0),
            (102, 'TaskEvent_0u4czzq', 'QCS_5.0.0.0.workflow.randon1592221382676.flag', '编辑', 4, 'QCS_5.0.0.0_inspectReport_manuInspReportEdit', '_blank', 1, 1),
            (103, 'TaskEvent_10f9q36', 'QCS_5.0.0.0.workflow.randon1592221629652.flag', '审核', 4, 'QCS_5.0.0.0_inspectReport_manuInspReportView', '_blank', 1, 1),
            (104, 'end_z9aqldn', 'QCS.workflow.randon1619601849955.flag', '结束活动', 2, NULL, NULL, 0, 0),
            (105, 'EndCancelEvent_1lrcfca', 'QCS.workflow.randon1619601849967.flag', '作废', 3, NULL, NULL, 0, 0)
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
       'manuInspectReportWorkFlow',
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
     WHERE process_key = 'manuInspectReportWorkFlow'
       AND coalesce(valid, 1) = 1
       AND coalesce(is_current_version, 0) = 1
     ORDER BY coalesce(process_version, 0) DESC, id DESC
     LIMIT 1
),
transition_seed AS (
    SELECT *
      FROM (
        VALUES
            (201, 'SequenceFlow_1atn6l1', 'QCS_5.0.0.0.workflow.randon1592207549041.flag', '制定', 'start_a9pr4ij', 'TaskEvent_0u4czzq', 1),
            (202, 'SequenceFlow_1c3ezkn', 'QCS_5.0.0.0.workflow.randon1588122044745.flag', '审核', 'TaskEvent_0u4czzq', 'TaskEvent_10f9q36', 1),
            (203, 'SequenceFlow_1kds1x1', 'QCS_5.0.0.0.workflow.randon1588125272223.flag', '作废', 'TaskEvent_0u4czzq', 'EndCancelEvent_1lrcfca', 3),
            (204, 'SequenceFlow_0wzrwm1', 'QCS_5.0.0.0.workflow.randon1596437204957.flag', '生效', 'TaskEvent_10f9q36', 'end_z9aqldn', 1),
            (205, 'SequenceFlow_1lk2g48', 'QCS_5.0.0.0.workflow.randon1592221901944.flag', '驳回', 'TaskEvent_10f9q36', 'TaskEvent_0u4czzq', 2)
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
           coalesce(name, 'QCS_5.0.0.0.workflowname.randon1591149227023.flag') AS flow_name
      FROM public.wf_deployment
     WHERE process_key = 'manuInspectReportWorkFlow'
       AND coalesce(valid, 1) = 1
       AND coalesce(is_current_version, 0) = 1
     ORDER BY coalesce(process_version, 0) DESC, id DESC
     LIMIT 1
),
permission_seed AS (
    SELECT *
      FROM (
        VALUES
            (301, 'TaskEvent_0u4czzq'),
            (302, 'TaskEvent_10f9q36')
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
       'QCS_5.0.0.0_inspectReport',
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
       'manuInspectReportWorkFlow',
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

CREATE INDEX IF NOT EXISTS idx_jbpm4_deployprop_key_stringval
    ON public.jbpm4_deployprop(key_, stringval_);

CREATE INDEX IF NOT EXISTS idx_wf_task_qcs_manu_deployment_code
    ON public.wf_task(deployment_id, code);

CREATE INDEX IF NOT EXISTS idx_wf_transition_qcs_manu_deployment_from
    ON public.wf_transition(deployment_id, from_node_code);
