-- Seed sysbase staff reference view required by business-module special permissions.
-- EQPTOperation uses sysbase_1.0_staff_ref as a refView for ownerStaff.

WITH view_rows (
  code,
  show_type,
  custom_flag_int,
  custom_flag_bool,
  open_type,
  view_type,
  title,
  display_name,
  name
) AS (
  VALUES
    (
      'sysbase_1.0_staff_ref',
      'SINGLE',
      1,
      true,
      'dialog',
      'REFERENCE',
      'foundation.ec.entity.view.reference',
      'foundation.ec.entity.view.reference',
      'staffRef'
    )
)
INSERT INTO public.ec_view (
  code,
  ec_env,
  version,
  valid,
  edit_view_type,
  has_custom_section,
  import_flag,
  mobile_enable_flag,
  mobile,
  only_for_query,
  module_code,
  control_print,
  is_permission,
  is_audit,
  is_print,
  is_hand_sign,
  is_sign,
  is_shadow,
  is_reference,
  include_children,
  used_for_tree,
  deal_info_show,
  has_attachment,
  is_control,
  close_page_after_save,
  show_type,
  custom_flag,
  url,
  ass_model_code,
  entity_code,
  main_ref,
  main_view,
  used_for_work_flow,
  open_type,
  type,
  title,
  display_name,
  name
)
SELECT
  code,
  'product',
  0,
  1,
  0,
  0,
  0,
  0,
  0,
  0,
  'sysbase_1.0',
  0,
  0,
  0,
  0,
  0,
  0,
  0,
  0,
  0,
  0,
  0,
  0,
  0,
  0,
  show_type,
  custom_flag_int,
  '/organization/#/reference?type=staff',
  'sysbase_1.0_staff_base_staff',
  'sysbase_1.0_staff',
  0,
  0,
  0,
  open_type,
  view_type,
  title,
  display_name,
  name
FROM view_rows
ON CONFLICT (code) DO NOTHING;

WITH view_rows (
  code,
  show_type,
  custom_flag_int,
  custom_flag_bool,
  open_type,
  view_type,
  title,
  display_name,
  name
) AS (
  VALUES
    (
      'sysbase_1.0_staff_ref',
      'SINGLE',
      1,
      true,
      'dialog',
      'REFERENCE',
      'foundation.ec.entity.view.reference',
      'foundation.ec.entity.view.reference',
      'staffRef'
    )
)
INSERT INTO public.runtime_view (
  code,
  ec_env,
  version,
  valid,
  edit_view_type,
  has_custom_section,
  import_flag,
  mobile_enable_flag,
  mobile,
  only_for_query,
  module_code,
  control_print,
  is_permission,
  is_audit,
  is_print,
  is_hand_sign,
  is_sign,
  is_shadow,
  is_reference,
  include_children,
  used_for_tree,
  deal_info_show,
  has_attachment,
  is_control,
  close_page_after_save,
  show_type,
  custom_flag,
  url,
  ass_model_code,
  entity_code,
  main_ref,
  main_view,
  used_for_work_flow,
  open_type,
  type,
  title,
  display_name,
  name
)
SELECT
  code,
  'product',
  0,
  true,
  0,
  false,
  false,
  false,
  false,
  false,
  'sysbase_1.0',
  false,
  false,
  false,
  false,
  false,
  false,
  false,
  false,
  false,
  false,
  false,
  false,
  false,
  false,
  show_type,
  custom_flag_bool,
  '/organization/#/reference?type=staff',
  'sysbase_1.0_staff_base_staff',
  'sysbase_1.0_staff',
  false,
  false,
  false,
  open_type,
  view_type,
  title,
  display_name,
  name
FROM view_rows
ON CONFLICT (code) DO NOTHING;

INSERT INTO public.ec_extra_view (
  code,
  ec_env,
  version,
  view_json,
  config,
  view_code
) VALUES
  ('sysbase_1.0_staff_ref', 'product', 0, NULL, NULL, 'sysbase_1.0_staff_ref')
ON CONFLICT (code) DO NOTHING;

INSERT INTO public.runtime_event (
  code,
  ec_env,
  version,
  modify_time,
  create_time,
  modify_staff_id,
  create_staff_id,
  valid,
  entity_code,
  module_code,
  name
) VALUES
  (
    'sysbase_1.0_staff_ref_ptPageInit',
    'product',
    1,
    TIMESTAMP '2020-07-24 09:00:29.192000',
    TIMESTAMP '2020-07-24 08:56:55.886000',
    1000,
    1000,
    true,
    'sysbase_1.0_staff',
    'sysbase_1.0',
    'sysbase_1.0_staff_ref_ptPageInit'
  ),
  (
    'sysbase_1.0_staff_ref_renderOver',
    'product',
    1,
    TIMESTAMP '2020-07-24 09:00:29.050000',
    TIMESTAMP '2020-07-24 08:56:55.751000',
    1000,
    1000,
    true,
    'sysbase_1.0_staff',
    'sysbase_1.0',
    'sysbase_1.0_staff_ref_renderOver'
  )
ON CONFLICT (code) DO NOTHING;

INSERT INTO public.runtime_extra_query_json (
  code,
  ec_env,
  version,
  view_code
) VALUES
  ('sysbase_1.0_staff_ref', 'product', 0, 'sysbase_1.0_staff_ref')
ON CONFLICT (code) DO NOTHING;

INSERT INTO public.runtime_extra_view (
  code,
  ec_env,
  version,
  view_code
) VALUES
  ('sysbase_1.0_staff_ref', 'product', 0, 'sysbase_1.0_staff_ref')
ON CONFLICT (code) DO NOTHING;

INSERT INTO public.runtime_fast_query_json (
  code,
  ec_env,
  version,
  view_code
) VALUES
  ('sysbase_1.0_staff_ref', 'product', 0, 'sysbase_1.0_staff_ref')
ON CONFLICT (code) DO NOTHING;
