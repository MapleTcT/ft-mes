-- PostgreSQL defaults for the recovered Qualify module.
--
-- QualifyConfigureUtil injects Qualify/Qualify.setDefaultLevel into a Boolean.
-- The packaged OCD declares this required setting with a default of false, but
-- the recovered runtime may not import the OCD row. A missing row injects null
-- and QualifyCertificateServiceImpl auto-unboxes it after saving a certificate,
-- causing the whole transaction to fail with NullPointerException.

INSERT INTO public.systemconfig_config_catalog(
  id, parent_id, sort, code, name, has_hide, app_code, catalog_type,
  creator, create_time, tenant_id
)
VALUES
  (238000, 2, 238, 'Qualify', 'qualify.configuration', false, 'Qualify', 2,
   'postgres-init-238', CURRENT_TIMESTAMP, 'dt')
ON CONFLICT (id) DO UPDATE
SET parent_id = EXCLUDED.parent_id,
    sort = EXCLUDED.sort,
    code = EXCLUDED.code,
    name = EXCLUDED.name,
    has_hide = EXCLUDED.has_hide,
    app_code = EXCLUDED.app_code,
    catalog_type = EXCLUDED.catalog_type,
    tenant_id = EXCLUDED.tenant_id,
    modifier = 'postgres-init-238',
    modify_time = CURRENT_TIMESTAMP;

INSERT INTO public.systemconfig_config_info(
  id, catalog_id, sort, code, name, app_code, module_code, widget_type,
  default_value, widget_value, max_value, min_value, reg_format, reg_message,
  has_require, custom, description, creator, create_time, tenant_id
)
VALUES
  (238001, 238000, 1, 'setDefaultLevel', 'qualify.setDefaultLevel',
   'Qualify', 'Qualify', 2, 'false', 'false',
   NULL, NULL, NULL, NULL, true, NULL,
   'Create no implicit certificate level unless explicitly enabled',
   'postgres-init-238', CURRENT_TIMESTAMP, 'dt')
ON CONFLICT (app_code, code) DO UPDATE
SET catalog_id = EXCLUDED.catalog_id,
    sort = EXCLUDED.sort,
    default_value = EXCLUDED.default_value,
    widget_value = COALESCE(
      NULLIF(public.systemconfig_config_info.widget_value, ''),
      EXCLUDED.widget_value
    ),
    name = EXCLUDED.name,
    module_code = EXCLUDED.module_code,
    widget_type = EXCLUDED.widget_type,
    has_require = EXCLUDED.has_require,
    description = EXCLUDED.description,
    tenant_id = EXCLUDED.tenant_id,
    modifier = 'postgres-init-238',
    modify_time = CURRENT_TIMESTAMP;

UPDATE public.systemconfig_config_version
SET config_version =
      'dt/Qualify/Qualify/postgres-init-238-'
      || extract(epoch from CURRENT_TIMESTAMP)::bigint,
    tenant_id = 'dt',
    modifier = 'postgres-init-238',
    modify_time = CURRENT_TIMESTAMP
WHERE tid_module_key = 'dt/Qualify/Qualify';

INSERT INTO public.systemconfig_config_version(
  id, config_version, tid_module_key, creator, create_time, tenant_id
)
SELECT
  238101,
  'dt/Qualify/Qualify/postgres-init-238',
  'dt/Qualify/Qualify',
  'postgres-init-238',
  CURRENT_TIMESTAMP,
  'dt'
WHERE NOT EXISTS (
  SELECT 1
  FROM public.systemconfig_config_version
  WHERE tid_module_key = 'dt/Qualify/Qualify'
);

DO $$
DECLARE
  configured_value text;
BEGIN
  SELECT COALESCE(NULLIF(widget_value, ''), default_value)
    INTO configured_value
    FROM public.systemconfig_config_info
   WHERE app_code = 'Qualify'
     AND module_code = 'Qualify'
     AND code = 'setDefaultLevel'
   ORDER BY CASE WHEN tenant_id = 'dt' THEN 0 ELSE 1 END
   LIMIT 1;

  IF configured_value IS DISTINCT FROM 'false' THEN
    RAISE EXCEPTION
      'Qualify setDefaultLevel compatibility failed: expected false, got %',
      configured_value;
  END IF;
END $$;
