-- Mark recovered WOM workflow definitions as the current effective version.
--
-- Source evidence:
-- - WOMProduceTaskServiceImpl.generatePrepareNeed calls
--   taskService.getCurrentDeployment("preNeedFlow") and then dereferences it.
-- - WorkflowTaskServiceImpl.getCurrentDeployment filters wf_deployment by
--   is_current_version = true.
-- - Recovered PostgreSQL data contains valid WOM workflow definitions, but
--   they were imported with is_current_version = 0, causing runtime NPEs.

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
        'makeTaskFlow',
        'outputFlow',
        'preNeedFlow',
        'putinFlow'
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

CREATE INDEX IF NOT EXISTS idx_wf_deployment_process_current
    ON public.wf_deployment(process_key, is_current_version);
