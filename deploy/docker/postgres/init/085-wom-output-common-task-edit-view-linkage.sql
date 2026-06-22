-- PostgreSQL runtime metadata fix for WOM minimal finish/report-work dialog.
--
-- 084 restores runtime_extra_view.view_json for
-- WOM_1.0.0_procReport_outPutCommonTaskEdit.  This companion patch makes the
-- runtime_view row point at that extra view and normalizes env flags used by
-- baseService layoutJson lookup.
--
-- If this is applied to an already-running environment after the view was
-- requested once, restart baseService or clear its layout cache. Fresh docker
-- init applies this before service startup and does not need a restart.

UPDATE public.runtime_view
SET ec_env = COALESCE(NULLIF(ec_env, ''), 'product'),
    extra_view = COALESCE(NULLIF(extra_view, ''), code),
    has_attachment = COALESCE(has_attachment, false),
    only_for_query = COALESCE(only_for_query, false),
    main_view = COALESCE(main_view, false),
    main_ref = COALESCE(main_ref, false),
    used_for_work_flow = COALESCE(used_for_work_flow, false),
    is_shadow = COALESCE(is_shadow, false),
    data_grid_type = COALESCE(data_grid_type, 0),
    proj_flag = COALESCE(proj_flag, false),
    move_flag = COALESCE(move_flag, false)
WHERE code = 'WOM_1.0.0_procReport_outPutCommonTaskEdit';

UPDATE public.runtime_extra_view
SET ec_env = COALESCE(NULLIF(ec_env, ''), 'product'),
    view_code = COALESCE(NULLIF(view_code, ''), code),
    proj_flag = COALESCE(proj_flag, false)
WHERE code = 'WOM_1.0.0_procReport_outPutCommonTaskEdit'
   OR view_code = 'WOM_1.0.0_procReport_outPutCommonTaskEdit';

UPDATE public.runtime_model
SET ec_env = COALESCE(NULLIF(ec_env, ''), 'product')
WHERE code IN (
    'WOM_1.0.0_procReport_ProcReport',
    'WOM_1.0.0_procReport_OutputDetail'
);
