-- Align recovered makeTaskFlow metadata with the workflow XML that is actually
-- executed by the current WOM runtime package.
--
-- Migration 161 restored an older SQL Server metadata snapshot containing an
-- approval node. The current wf_deployment XML has a direct "编辑 -> 生效 -> 结束"
-- transition instead. Apply this correction only when that exact XML signature
-- is present, so a future business package with a different workflow is not
-- overwritten.

DO $guard$
DECLARE
    deployment_id_value bigint;
    transition_id_value bigint;
BEGIN
    SELECT id
      INTO deployment_id_value
      FROM public.wf_deployment
     WHERE process_key = 'makeTaskFlow'
       AND coalesce(valid, 1) = 1
       AND coalesce(is_current_version, 0) = 1
       AND position(
           '<transition name="SequenceFlow_0vcn8hp" desc="生效" to="end_v7ufaij"'
           IN coalesce(process_xml_text_backup, '')
       ) > 0
     ORDER BY coalesce(process_version, 0) DESC, id DESC
     LIMIT 1;

    IF deployment_id_value IS NULL THEN
        RETURN;
    END IF;

    transition_id_value := deployment_id_value + 203;
    IF EXISTS (
        SELECT 1
          FROM public.wf_transition
         WHERE id = transition_id_value
           AND NOT (
               deployment_id = deployment_id_value
               AND code IN (
                   'SequenceFlow_00a9xaa',
                   'SequenceFlow_0vcn8hp',
                   'SequenceFlow_0libf0v'
               )
           )
    ) THEN
        RAISE EXCEPTION
            'makeTaskFlow transition id % is owned by unrelated workflow metadata',
            transition_id_value;
    END IF;
END
$guard$;

WITH current_flow AS (
    SELECT id AS deployment_id
      FROM public.wf_deployment
     WHERE process_key = 'makeTaskFlow'
       AND coalesce(valid, 1) = 1
       AND coalesce(is_current_version, 0) = 1
       AND position(
           '<transition name="SequenceFlow_0vcn8hp" desc="生效" to="end_v7ufaij"'
           IN coalesce(process_xml_text_backup, '')
       ) > 0
     ORDER BY coalesce(process_version, 0) DESC, id DESC
     LIMIT 1
)
DELETE FROM public.wf_transition transition
 USING current_flow
 WHERE transition.deployment_id = current_flow.deployment_id
   AND transition.code IN (
       'SequenceFlow_00a9xaa',
       'SequenceFlow_0vcn8hp',
       'SequenceFlow_0libf0v'
   );

WITH current_flow AS (
    SELECT id AS deployment_id
      FROM public.wf_deployment
     WHERE process_key = 'makeTaskFlow'
       AND coalesce(valid, 1) = 1
       AND coalesce(is_current_version, 0) = 1
       AND position(
           '<transition name="SequenceFlow_0vcn8hp" desc="生效" to="end_v7ufaij"'
           IN coalesce(process_xml_text_backup, '')
       ) > 0
     ORDER BY coalesce(process_version, 0) DESC, id DESC
     LIMIT 1
)
DELETE FROM public.wf_task task
 USING current_flow
 WHERE task.deployment_id = current_flow.deployment_id
   AND task.code = 'TaskEvent_023irnk';

WITH current_flow AS (
    SELECT id AS deployment_id
      FROM public.wf_deployment
     WHERE process_key = 'makeTaskFlow'
       AND coalesce(valid, 1) = 1
       AND coalesce(is_current_version, 0) = 1
       AND position(
           '<transition name="SequenceFlow_0vcn8hp" desc="生效" to="end_v7ufaij"'
           IN coalesce(process_xml_text_backup, '')
       ) > 0
     ORDER BY coalesce(process_version, 0) DESC, id DESC
     LIMIT 1
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
SELECT current_flow.deployment_id + 203,
       0,
       1,
       0,
       0,
       '0',
       current_flow.deployment_id,
       'end_v7ufaij',
       'TaskEvent_0il1mab',
       1,
       'SequenceFlow_0vcn8hp',
       '生效',
       'WOM_1.0.0.workflow.randon1575443361321.flag'
  FROM current_flow
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

DO $verify$
DECLARE
    deployment_id_value bigint;
    matching_transitions integer;
    stale_transitions integer;
    stale_tasks integer;
BEGIN
    SELECT id
      INTO deployment_id_value
      FROM public.wf_deployment
     WHERE process_key = 'makeTaskFlow'
       AND coalesce(valid, 1) = 1
       AND coalesce(is_current_version, 0) = 1
       AND position(
           '<transition name="SequenceFlow_0vcn8hp" desc="生效" to="end_v7ufaij"'
           IN coalesce(process_xml_text_backup, '')
       ) > 0
     ORDER BY coalesce(process_version, 0) DESC, id DESC
     LIMIT 1;

    IF deployment_id_value IS NULL THEN
        RETURN;
    END IF;

    SELECT count(*)
      INTO matching_transitions
      FROM public.wf_transition
     WHERE deployment_id = deployment_id_value
       AND code = 'SequenceFlow_0vcn8hp'
       AND from_node_code = 'TaskEvent_0il1mab'
       AND to_node_code = 'end_v7ufaij'
       AND name_zh_cn = '生效'
       AND coalesce(valid, 1) = 1;

    SELECT count(*)
      INTO stale_transitions
      FROM public.wf_transition
     WHERE deployment_id = deployment_id_value
       AND (
           code IN ('SequenceFlow_00a9xaa', 'SequenceFlow_0libf0v')
           OR (code = 'SequenceFlow_0vcn8hp' AND from_node_code <> 'TaskEvent_0il1mab')
       );

    SELECT count(*)
      INTO stale_tasks
      FROM public.wf_task
     WHERE deployment_id = deployment_id_value
       AND code = 'TaskEvent_023irnk';

    IF matching_transitions <> 1 OR stale_transitions <> 0 OR stale_tasks <> 0 THEN
        RAISE EXCEPTION
            'makeTaskFlow runtime metadata sync failed: matching=%, staleTransitions=%, staleTasks=%',
            matching_transitions,
            stale_transitions,
            stale_tasks;
    END IF;
END
$verify$;
