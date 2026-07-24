-- Link LIMS quality-standard reference runtime view to its recovered layout JSON.
--
-- The generated reference HTML can load successfully, but baseService then calls
-- /baseService/view/layoutJson and dereferences runtime_view.extra_view. The
-- recovered PostgreSQL row had runtime_extra_view.view_json populated while
-- runtime_view.extra_view was blank, causing:
--   ViewServiceFoundationImpl.findLayoutJsonByViewCode -> NullPointerException

UPDATE public.runtime_view
SET ec_env = COALESCE(NULLIF(ec_env, ''), 'product'),
    extra_view = COALESCE(NULLIF(extra_view, ''), code),
    has_attachment = COALESCE(has_attachment, false),
    only_for_query = COALESCE(only_for_query, false),
    main_view = COALESCE(main_view, false),
    main_ref = COALESCE(main_ref, false),
    used_for_work_flow = COALESCE(used_for_work_flow, false),
    is_shadow = COALESCE(is_shadow, 0),
    data_grid_type = COALESCE(data_grid_type, 0),
    proj_flag = COALESCE(proj_flag, false),
    move_flag = COALESCE(move_flag, false)
WHERE code = 'LIMSBasic_1.0.0_qualityStd_qualityStdVerRef';

UPDATE public.runtime_extra_view
SET ec_env = COALESCE(NULLIF(ec_env, ''), 'product'),
    proj_flag = COALESCE(proj_flag, false),
    view_code = COALESCE(NULLIF(view_code, ''), code)
WHERE code = 'LIMSBasic_1.0.0_qualityStd_qualityStdVerRef';
