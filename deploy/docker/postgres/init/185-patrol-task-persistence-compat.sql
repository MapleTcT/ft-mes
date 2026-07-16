-- PATROL task persistence compatibility for PostgreSQL.
--
-- The packaged task generator historically populated PATROL_PLAN_ID without
-- the association column used by generated list SQL. It also omitted the
-- lifecycle defaults from direct MP_TASK_DETAILS inserts. Keep existing task
-- data queryable and make future direct inserts deterministic.

\set ON_ERROR_STOP on

BEGIN;

UPDATE public.mp_potrol_tasks
   SET patrol_plan_id = patrol_plan
 WHERE patrol_plan IS NOT NULL
   AND patrol_plan_id IS NULL;

UPDATE public.mp_potrol_tasks
   SET patrol_plan = patrol_plan_id
 WHERE patrol_plan_id IS NOT NULL
   AND patrol_plan IS DISTINCT FROM patrol_plan_id;

UPDATE public.mp_task_details
   SET valid = true
 WHERE valid IS NULL;

UPDATE public.mp_task_details
   SET version = 0
 WHERE version IS NULL;

UPDATE public.mp_task_details detail
   SET task_detail_state = 'PATROL_taskDetailState/pending'
  FROM public.mp_potrol_tasks task
 WHERE detail.patrol_task = task.id
   AND detail.task_detail_state IS NULL
   AND (
       task.task_state IS NULL
       OR task.task_state = 'PATROL_taskState/notIssued'
   );

ALTER TABLE public.mp_task_details
    ALTER COLUMN valid SET DEFAULT true,
    ALTER COLUMN version SET DEFAULT 0,
    ALTER COLUMN task_detail_state SET DEFAULT 'PATROL_taskDetailState/pending';

DO $$
DECLARE
    inconsistent_task_count bigint;
    incomplete_detail_count bigint;
BEGIN
    SELECT count(*)
      INTO inconsistent_task_count
      FROM public.mp_potrol_tasks
     WHERE patrol_plan IS DISTINCT FROM patrol_plan_id
       AND (patrol_plan IS NOT NULL OR patrol_plan_id IS NOT NULL);

    IF inconsistent_task_count <> 0 THEN
        RAISE EXCEPTION
            'PATROL task plan references remain inconsistent: %',
            inconsistent_task_count;
    END IF;

    SELECT count(*)
      INTO incomplete_detail_count
      FROM public.mp_task_details detail
      JOIN public.mp_potrol_tasks task ON task.id = detail.patrol_task
     WHERE (
               task.task_state IS NULL
               OR task.task_state = 'PATROL_taskState/notIssued'
           )
       AND (
               detail.valid IS NULL
               OR detail.version IS NULL
               OR detail.task_detail_state IS NULL
           );

    IF incomplete_detail_count <> 0 THEN
        RAISE EXCEPTION
            'PATROL pending task details remain incomplete: %',
            incomplete_detail_count;
    END IF;
END $$;

COMMIT;
