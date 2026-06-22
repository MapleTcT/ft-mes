-- Register recovered RM formula workflows with the legacy JBPM runtime.
--
-- Source evidence:
-- - RMBatchFormulaServiceImpl.addFormula uses the batch server code as the
--   workflow key, then RMFormulaServiceImpl.save starts that deployment.
-- - Recovered PostgreSQL has RM wf_deployment XML backups, but the JBPM
--   runtime tables were missing deployment/deployprop/lob rows for the RM
--   formula workflows, causing ec.workflow.noProcessDefinition.

WITH workflow_seed(process_key, deployment_dbid) AS (
    VALUES
        ('formulaEnableFlw', 7401000000040000::bigint),
        ('serviceUrl_001', 7401000000050000::bigint)
),
current_flow AS (
    SELECT DISTINCT ON (deployment.process_key)
           deployment.id,
           deployment.process_key,
           workflow_seed.deployment_dbid,
           greatest(coalesce(deployment.process_version, 0), 1) AS process_version
      FROM public.wf_deployment deployment
      JOIN workflow_seed
        ON workflow_seed.process_key = deployment.process_key
     WHERE coalesce(deployment.valid, 1) = 1
       AND (deployment.process_xml IS NOT NULL OR deployment.process_xml_text_backup IS NOT NULL)
     ORDER BY deployment.process_key,
              coalesce(deployment.is_current_version, 0) DESC,
              coalesce(deployment.process_version, 0) DESC,
              deployment.id DESC
)
UPDATE public.wf_deployment deployment
   SET process_version = current_flow.process_version,
       process_definition_id = current_flow.process_key || '-' || current_flow.process_version::text,
       deployment_id = current_flow.deployment_dbid::text,
       is_current_version = CASE WHEN deployment.id = current_flow.id THEN 1 ELSE 0 END,
       valid = 1,
       modify_time = CURRENT_TIMESTAMP
  FROM current_flow
 WHERE deployment.process_key = current_flow.process_key;

WITH workflow_seed(process_key, deployment_dbid) AS (
    VALUES
        ('formulaEnableFlw', 7401000000040000::bigint),
        ('serviceUrl_001', 7401000000050000::bigint)
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
       AND (deployment.process_xml IS NOT NULL OR deployment.process_xml_text_backup IS NOT NULL)
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
        ('formulaEnableFlw', 7401000000040000::bigint),
        ('serviceUrl_001', 7401000000050000::bigint)
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
       AND (deployment.process_xml IS NOT NULL OR deployment.process_xml_text_backup IS NOT NULL)
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
        ('formulaEnableFlw', 7401000000040000::bigint),
        ('serviceUrl_001', 7401000000050000::bigint)
),
current_flow AS (
    SELECT DISTINCT ON (deployment.process_key)
           deployment.process_key,
           deployment.name,
           workflow_seed.deployment_dbid,
           CASE
               WHEN deployment.process_xml_text_backup IS NOT NULL THEN deployment.process_xml_text_backup
               WHEN deployment.process_xml IS NOT NULL THEN convert_from(lo_get(deployment.process_xml), 'UTF8')
               ELSE NULL
           END AS process_xml_text
      FROM public.wf_deployment deployment
      JOIN workflow_seed
        ON workflow_seed.process_key = deployment.process_key
     WHERE coalesce(deployment.valid, 1) = 1
       AND coalesce(deployment.is_current_version, 0) = 1
       AND (deployment.process_xml IS NOT NULL OR deployment.process_xml_text_backup IS NOT NULL)
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
 WHERE current_flow.process_xml_text IS NOT NULL
ON CONFLICT (dbid_) DO UPDATE SET
    dbversion_ = EXCLUDED.dbversion_,
    blob_value_ = EXCLUDED.blob_value_,
    deployment_ = EXCLUDED.deployment_,
    name_ = EXCLUDED.name_;

WITH current_flow AS (
    SELECT id AS deployment_id,
           coalesce(process_version, 1) AS process_version
      FROM public.wf_deployment
     WHERE process_key = 'formulaEnableFlw'
       AND coalesce(valid, 1) = 1
       AND coalesce(is_current_version, 0) = 1
     ORDER BY coalesce(process_version, 0) DESC, id DESC
     LIMIT 1
),
task_seed AS (
    SELECT *
      FROM (
        VALUES
            (101, 'start_eahk56q', 'RM_1.0.0.workflow.randon1573716340983.flag', '开始活动', 1, 'RM_1.0.0_formula_commonFormulaEdit', NULL, 0, 0),
            (102, 'TaskEvent_11ysb3i', 'RM_1.0.0.workflow.randon1573716380222.flag', '编辑', 4, 'RM_1.0.0_formula_commonFormulaEdit', '_blank', 1, 1),
            (103, 'TaskEvent_0k763xk', 'RM_1.0.0.workflow.randon1575273353000.flag', '审核', 4, 'RM_1.0.0_formula_commonFormulaView', '_blank', 1, 1),
            (104, 'end_bllpwy8', 'RM_1.0.0.workflow.randon1573717257556.flag', '结束活动', 2, NULL, NULL, 0, 0),
            (105, 'EndCancelEvent_0zcr96s', 'RM_1.0.0.workflow.randon1575273404902.flag', '作废', 3, NULL, NULL, 0, 0)
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
       'formulaEnableFlw',
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
     WHERE process_key = 'formulaEnableFlw'
       AND coalesce(valid, 1) = 1
       AND coalesce(is_current_version, 0) = 1
     ORDER BY coalesce(process_version, 0) DESC, id DESC
     LIMIT 1
),
transition_seed AS (
    SELECT *
      FROM (
        VALUES
            (201, 'SequenceFlow_1dunjlg', 'RM_1.0.0.workflow.randon1573716309244.flag', '开始', 'start_eahk56q', 'TaskEvent_11ysb3i', 1),
            (202, 'SequenceFlow_0zdpiaf', 'RM_1.0.0.workflow.randon1575273319124.flag', '提交', 'TaskEvent_11ysb3i', 'TaskEvent_0k763xk', 1),
            (203, 'SequenceFlow_0huzc3e', 'RM_1.0.0.workflow.randon1575273303286.flag', '作废', 'TaskEvent_11ysb3i', 'EndCancelEvent_0zcr96s', 3),
            (204, 'SequenceFlow_0e0pbfv', 'RM_1.0.0.workflow.randon1575273325492.flag', '生效', 'TaskEvent_0k763xk', 'end_bllpwy8', 1),
            (205, 'SequenceFlow_08skd9s', 'RM_1.0.0.workflow.randon1575273282742.flag', '驳回', 'TaskEvent_0k763xk', 'TaskEvent_11ysb3i', 2)
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
           coalesce(process_version, 1) AS process_version
      FROM public.wf_deployment
     WHERE process_key = 'serviceUrl_001'
       AND coalesce(valid, 1) = 1
       AND coalesce(is_current_version, 0) = 1
     ORDER BY coalesce(process_version, 0) DESC, id DESC
     LIMIT 1
),
task_seed AS (
    SELECT *
      FROM (
        VALUES
            (101, 'start_rllcoxf_ref121', 'RM_1.0.0.workflow.randon1588213032064.flag', '开始活动', 1, 'RM_1.0.0_formula_batchFormulaEdit', NULL, 0, 0),
            (102, 'TaskEvent_1ishah0_ref121', 'RM_1.0.0.workflow.randon1588213032066.flag', '编辑', 4, 'RM_1.0.0_formula_batchFormulaEdit', '_blank', 1, 1),
            (103, 'TaskEvent_03ddbq6_ref121', 'RM_1.0.0.workflow.randon1588213032067.flag', '审核', 4, 'RM_1.0.0_formula_batchFormulaView', '_blank', 1, 1),
            (104, 'end_v27m1ly_ref121', 'RM_1.0.0.workflow.randon1588213032065.flag', '结束活动', 2, NULL, NULL, 0, 0),
            (105, 'EndCancelEvent_0ha0314_ref121', 'RM_1.0.0.workflow.randon1588213032068.flag', '作废', 3, NULL, NULL, 0, 0)
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
       'serviceUrl_001',
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
     WHERE process_key = 'serviceUrl_001'
       AND coalesce(valid, 1) = 1
       AND coalesce(is_current_version, 0) = 1
     ORDER BY coalesce(process_version, 0) DESC, id DESC
     LIMIT 1
),
transition_seed AS (
    SELECT *
      FROM (
        VALUES
            (201, 'SequenceFlow_0zr8c4s', 'RM_1.0.0.workflow.randon1588213083347.flag', '开始', 'start_rllcoxf_ref121', 'TaskEvent_1ishah0_ref121', 1),
            (202, 'SequenceFlow_1okplem', 'RM_1.0.0.workflow.randon1588213145991.flag', '提交', 'TaskEvent_1ishah0_ref121', 'TaskEvent_03ddbq6_ref121', 1),
            (203, 'SequenceFlow_1m4hrfj', 'RM_1.0.0.workflow.randon1588213141718.flag', '作废', 'TaskEvent_1ishah0_ref121', 'EndCancelEvent_0ha0314_ref121', 3),
            (204, 'SequenceFlow_03lfb9l', 'RM_1.0.0.workflow.randon1588213155284.flag', '生效', 'TaskEvent_1ishah0_ref121', 'end_v27m1ly_ref121', 1),
            (205, 'SequenceFlow_1orsgw7', 'RM_1.0.0.workflow.randon1588213149637.flag', '审核', 'TaskEvent_03ddbq6_ref121', 'end_v27m1ly_ref121', 1),
            (206, 'SequenceFlow_1bfn7i6', 'RM_1.0.0.workflow.randon1588213159704.flag', '驳回', 'TaskEvent_03ddbq6_ref121', 'TaskEvent_1ishah0_ref121', 2)
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

CREATE INDEX IF NOT EXISTS idx_jbpm4_deployprop_key_stringval
    ON public.jbpm4_deployprop(key_, stringval_);

CREATE INDEX IF NOT EXISTS idx_wf_task_deployment_code
    ON public.wf_task(deployment_id, code);

CREATE INDEX IF NOT EXISTS idx_wf_transition_deployment_from
    ON public.wf_transition(deployment_id, from_node_code);
