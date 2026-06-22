-- Test-env system configuration for recovered RM batch-control integration.
--
-- RMConfigure is annotated with @ClassSystemConfigAnno, so the Linux runtime
-- reads these values from systemconfig tables during AnnoFilter initialization.
-- Without them, the recovered RM service gets null for the batch simulated
-- login user and MQ broker host, causing batch formula sync to fail before
-- persistence can be validated.

INSERT INTO public.systemconfig_config_catalog(
  id, parent_id, sort, code, name, has_hide, app_code, catalog_type,
  creator, create_time, tenant_id
)
VALUES
  (129000, 2, 129, 'RM', 'RM.ocd.RM', false, 'RM', 2,
   'postgres-init-129', CURRENT_TIMESTAMP, 'dt'),
  (129010, 129000, 10, 'RM.Batch', 'RM.ocd.Batch', false, 'RM', 2,
   'postgres-init-129', CURRENT_TIMESTAMP, 'dt'),
  (129020, 129000, 20, 'RM.MQ', 'RM.ocd.MQ', false, 'RM', 2,
   'postgres-init-129', CURRENT_TIMESTAMP, 'dt'),
  (129030, 2, 130, 'BaseSet', 'BaseSet.ocd.BaseSet', false, 'BaseSet', 2,
   'postgres-init-129', CURRENT_TIMESTAMP, 'dt')
ON CONFLICT (id) DO UPDATE
SET parent_id = EXCLUDED.parent_id,
    sort = EXCLUDED.sort,
    code = EXCLUDED.code,
    name = EXCLUDED.name,
    has_hide = EXCLUDED.has_hide,
    app_code = EXCLUDED.app_code,
    catalog_type = EXCLUDED.catalog_type,
    tenant_id = EXCLUDED.tenant_id,
    modifier = 'postgres-init-129',
    modify_time = CURRENT_TIMESTAMP;

INSERT INTO public.systemconfig_config_info(
  id, catalog_id, sort, code, name, app_code, module_code, widget_type,
  default_value, widget_value, max_value, min_value, reg_format, reg_message,
  has_require, custom, description, creator, create_time, tenant_id
)
VALUES
  (129001, 129010, 10, 'username', 'RM.ocd.Batch.username', 'RM', 'RM.Batch', 1,
   'admin', 'admin', NULL, NULL, NULL, NULL, true, NULL,
   'Test-env simulated login user for RM batch formula sync', 'postgres-init-129', CURRENT_TIMESTAMP, 'dt'),
  (129002, 129010, 11, 'password', 'RM.ocd.Batch.password', 'RM', 'RM.Batch', 1,
   '123456', '123456', NULL, NULL, NULL, NULL, true, NULL,
   'Test-env simulated login password for RM batch formula sync', 'postgres-init-129', CURRENT_TIMESTAMP, 'dt'),
  (129003, 129020, 20, 'brokerUrl', 'RM.ocd.MQ.brokerUrl', 'RM', 'RM.MQ', 1,
   'localhost', 'localhost', NULL, NULL, NULL, NULL, false, NULL,
   'Test-env RM batch MQ broker host; no external batch-control broker is required for DB persistence acceptance', 'postgres-init-129', CURRENT_TIMESTAMP, 'dt'),
  (129004, 129030, 30, 'isEnable', 'BaseSet.ocd.isEnable', 'BaseSet', 'BaseSet', 2,
   'true', 'true', NULL, NULL, NULL, NULL, false, NULL,
   'Enable recovered batch-control integration defaults in the PostgreSQL test runtime', 'postgres-init-129', CURRENT_TIMESTAMP, 'dt')
ON CONFLICT (app_code, code) DO UPDATE
SET catalog_id = EXCLUDED.catalog_id,
    sort = EXCLUDED.sort,
    default_value = EXCLUDED.default_value,
    widget_value = EXCLUDED.widget_value,
    name = EXCLUDED.name,
    module_code = EXCLUDED.module_code,
    widget_type = EXCLUDED.widget_type,
    has_require = EXCLUDED.has_require,
    description = EXCLUDED.description,
    tenant_id = EXCLUDED.tenant_id,
    modifier = 'postgres-init-129',
    modify_time = CURRENT_TIMESTAMP;

UPDATE public.systemconfig_config_version
SET config_version = 'dt/RM/RM.Batch/postgres-init-129-' || extract(epoch from CURRENT_TIMESTAMP)::bigint,
    tenant_id = 'dt',
    modifier = 'postgres-init-129',
    modify_time = CURRENT_TIMESTAMP
WHERE tid_module_key = 'dt/RM/RM.Batch';

INSERT INTO public.systemconfig_config_version(
  id, config_version, tid_module_key, creator, create_time, tenant_id
)
SELECT
  129101,
  'dt/RM/RM.Batch/postgres-init-129',
  'dt/RM/RM.Batch',
  'postgres-init-129',
  CURRENT_TIMESTAMP,
  'dt'
WHERE NOT EXISTS (
  SELECT 1
  FROM public.systemconfig_config_version
  WHERE tid_module_key = 'dt/RM/RM.Batch'
);

UPDATE public.systemconfig_config_version
SET config_version = 'dt/RM/RM.MQ/postgres-init-129-' || extract(epoch from CURRENT_TIMESTAMP)::bigint,
    tenant_id = 'dt',
    modifier = 'postgres-init-129',
    modify_time = CURRENT_TIMESTAMP
WHERE tid_module_key = 'dt/RM/RM.MQ';

INSERT INTO public.systemconfig_config_version(
  id, config_version, tid_module_key, creator, create_time, tenant_id
)
SELECT
  129102,
  'dt/RM/RM.MQ/postgres-init-129',
  'dt/RM/RM.MQ',
  'postgres-init-129',
  CURRENT_TIMESTAMP,
  'dt'
WHERE NOT EXISTS (
  SELECT 1
  FROM public.systemconfig_config_version
  WHERE tid_module_key = 'dt/RM/RM.MQ'
);

UPDATE public.systemconfig_config_version
SET config_version = 'dt/BaseSet/BaseSet/postgres-init-129-' || extract(epoch from CURRENT_TIMESTAMP)::bigint,
    tenant_id = 'dt',
    modifier = 'postgres-init-129',
    modify_time = CURRENT_TIMESTAMP
WHERE tid_module_key = 'dt/BaseSet/BaseSet';

INSERT INTO public.systemconfig_config_version(
  id, config_version, tid_module_key, creator, create_time, tenant_id
)
SELECT
  129103,
  'dt/BaseSet/BaseSet/postgres-init-129',
  'dt/BaseSet/BaseSet',
  'postgres-init-129',
  CURRENT_TIMESTAMP,
  'dt'
WHERE NOT EXISTS (
  SELECT 1
  FROM public.systemconfig_config_version
  WHERE tid_module_key = 'dt/BaseSet/BaseSet'
);
