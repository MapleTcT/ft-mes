-- Add workflow runtime columns required by the deployed configuration service.
--
-- The recovered source tree lacks Task.endCountersignFlag, but the currently
-- deployed configuration runtime queries wf_task.end_countersign_flag while
-- publishing JBPM workflows.  Without this column, /ec/workflow/flowPublish
-- fails before it can persist task metadata.
ALTER TABLE public.wf_task
    ADD COLUMN IF NOT EXISTS end_countersign_flag integer DEFAULT 0;
