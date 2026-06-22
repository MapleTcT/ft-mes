-- Register the recovered WOM manufacturing task workflow with the legacy JBPM
-- runtime.
--
-- Source evidence:
-- - WOMFormCreatServiceImpl.creatProduceTask resolves workflow config
--   'manufacture', then calls WOMProduceTaskServiceImpl.save.
-- - WOMProduceTaskServiceImpl.save starts a JBPM process through
--   InstanceServiceImpl.start(deploymentId, ...).
-- - PostgreSQL already contains wf_deployment/process_xml for makeTaskFlow,
--   but missing jbpm4_deployment/jbpm4_deployprop/jbpm4_lob rows causes
--   ec.workflow.noProcessDefinition during produceTaskCreated2.

WITH workflow_seed(process_key, deployment_dbid) AS (
    VALUES
        ('makeTaskFlow', 7401000000110000::bigint)
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
       publish_flag = 1,
       modify_time = CURRENT_TIMESTAMP
  FROM current_flow
 WHERE deployment.process_key = current_flow.process_key;

WITH workflow_seed(process_key, deployment_dbid) AS (
    VALUES
        ('makeTaskFlow', 7401000000110000::bigint)
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
        ('makeTaskFlow', 7401000000110000::bigint)
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
        ('makeTaskFlow', 7401000000110000::bigint)
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
     WHERE process_key = 'makeTaskFlow'
       AND coalesce(valid, 1) = 1
       AND coalesce(is_current_version, 0) = 1
     ORDER BY coalesce(process_version, 0) DESC, id DESC
     LIMIT 1
)
UPDATE public.wf_task task
   SET process_version = current_flow.process_version
  FROM current_flow
 WHERE task.deployment_id = current_flow.deployment_id
   AND task.process_key = 'makeTaskFlow';

-- The recovered XML assignment handler has all dynamic staff flags disabled
-- and an empty staffIds field. Without explicit flow permission rows, the
-- first user task starts and then fails with ec.common.workflow.noPowerUsers.
WITH current_flow AS (
    SELECT id AS deployment_id,
           coalesce(process_version, 1)::text AS flow_version,
           coalesce(name, 'WOM_1.0.0.workflowname.randon1575443181113.flag') AS flow_name
      FROM public.wf_deployment
     WHERE process_key = 'makeTaskFlow'
       AND coalesce(valid, 1) = 1
       AND coalesce(is_current_version, 0) = 1
     ORDER BY coalesce(process_version, 0) DESC, id DESC
     LIMIT 1
),
permission_seed AS (
    SELECT *
      FROM (
        VALUES
            (301, 'TaskEvent_0il1mab'),
            (302, 'TaskEvent_023irnk')
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
       'WOM_1.0.0_produceTask',
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
       'makeTaskFlow',
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

CREATE INDEX IF NOT EXISTS idx_rbac_flow_permission_wom_make_task_activity
    ON public.rbac_flow_permission(flow_key, activity_code, purview_state);
