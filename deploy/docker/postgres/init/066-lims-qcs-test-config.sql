-- Test-env defaults for recovered LIMS/QCS modules.
--
-- The recovered module metadata does not always import system configuration
-- rows for Boolean @Value keys. When these rows are absent, the application
-- injects null and legacy custom controller code can throw NPE while opening
-- list pages. Keep data permission disabled by default in the Linux test env.

INSERT INTO public.systemconfig_config_info(
  id, catalog_id, sort, code, name, app_code, module_code, widget_type,
  default_value, widget_value, max_value, min_value, reg_format, reg_message,
  has_require, custom, description
)
VALUES
  (66001, 2, 1, 'dataPermission', 'LIMSSample.ocd.dataPermission', 'LIMSSample', 'LIMSSample', 2,
   'false', 'false', NULL, NULL, NULL, NULL, false, NULL, 'Whether to enable LIMSSample resource permissions'),
  (66002, 2, 1, 'dataPermission', 'QCS.ocd.dataPermission', 'QCS', 'QCS', 2,
   'false', 'false', NULL, NULL, NULL, NULL, false, NULL, 'Whether to enable QCS resource permissions'),
  (66003, 2, 1, 'dataPermission', 'LIMSBasic.ocd.dataPermission', 'LIMSBasic', 'LIMSBasic', 2,
   'false', 'false', NULL, NULL, NULL, NULL, false, NULL, 'Whether to enable LIMSBasic resource permissions')
ON CONFLICT (app_code, code) DO UPDATE
SET default_value = COALESCE(NULLIF(public.systemconfig_config_info.default_value, ''), EXCLUDED.default_value),
    widget_value = COALESCE(NULLIF(public.systemconfig_config_info.widget_value, ''), EXCLUDED.widget_value),
    name = EXCLUDED.name,
    module_code = EXCLUDED.module_code,
    widget_type = EXCLUDED.widget_type,
    has_require = EXCLUDED.has_require,
    description = EXCLUDED.description;
