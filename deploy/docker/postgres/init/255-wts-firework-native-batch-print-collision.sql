-- The recovered firework list exposes a CUSTOM batch-print action because
-- greenDill filters its native BATCH_PRINT action from the visible toolbar.
-- Keeping the view-level injection enabled also overwrites the custom action's
-- label with the raw ec.print.batchPrint key.  Disable only that synthesized
-- action; the compatibility action still calls the original print endpoint and
-- operation code.

UPDATE public.ec_view
SET is_batch_control_print = 0,
    modify_time = CURRENT_TIMESTAMP,
    version = GREATEST(COALESCE(version, 0), 1)
WHERE code = 'WTS_1.0.0_workTicket_firework';

UPDATE public.runtime_view
SET is_batch_control_print = FALSE,
    modify_time = CURRENT_TIMESTAMP,
    version = GREATEST(COALESCE(version, 0), 1)
WHERE code = 'WTS_1.0.0_workTicket_firework';

UPDATE public.project_view
SET is_batch_control_print = 0,
    modify_time = CURRENT_TIMESTAMP,
    version = GREATEST(COALESCE(version, 0), 1)
WHERE code = 'WTS_1.0.0_workTicket_firework';
