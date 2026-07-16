\set ON_ERROR_STOP on

-- Additive rollback for PATROL 6.0.4.0.
--
-- This intentionally keeps recovered business tables and metadata in place so
-- an operator can re-enable the module by rerunning migrations 179-182. The
-- runtime JAR must be restored separately from its pre-deployment backup.

UPDATE public.runtime_module
   SET valid = false
 WHERE code = 'PATROL_1.0.0';

UPDATE public.runtime_entity
   SET valid = false
 WHERE module_code = 'PATROL_1.0.0';

UPDATE public.runtime_model
   SET valid = false
 WHERE module_code = 'PATROL_1.0.0';

UPDATE public.runtime_property
   SET valid = false
 WHERE module_code = 'PATROL_1.0.0';

UPDATE public.runtime_view
   SET valid = false
 WHERE module_code = 'PATROL_1.0.0';

UPDATE public.runtime_customer_condition
   SET valid = false
 WHERE module_code = 'PATROL_1.0.0';

UPDATE public.ec_module
   SET valid = 0
 WHERE code = 'PATROL_1.0.0';

UPDATE public.ec_entity
   SET valid = 0
 WHERE module_code = 'PATROL_1.0.0';

UPDATE public.ec_model
   SET valid = 0
 WHERE module_code = 'PATROL_1.0.0';

UPDATE public.ec_property
   SET valid = 0
 WHERE module_code = 'PATROL_1.0.0';

UPDATE public.ec_view
   SET valid = 0
 WHERE module_code = 'PATROL_1.0.0';

UPDATE public.ec_customer_condition
   SET valid = 0
 WHERE module_code = 'PATROL_1.0.0';

UPDATE public.rbac_menuinfo
   SET valid = false,
       enable = false,
       modify_time = CURRENT_TIMESTAMP,
       modifier = 'codex_patrol_disable'
 WHERE module_code = 'PATROL_1.0.0';

UPDATE public.rbac_menuoperate
   SET valid = 0,
       modify_time = CURRENT_TIMESTAMP,
       modifier = 'codex_patrol_disable'
 WHERE module_code = 'PATROL_1.0.0';

UPDATE public.wf_deployment
   SET valid = 0,
       is_current_version = 0,
       publish_flag = 0,
       modify_time = CURRENT_TIMESTAMP
 WHERE process_key IN ('potrolTaskWF', 'tempTaskWF');

UPDATE public.jbpm4_deployment deployment
   SET state_ = 'suspended'
 WHERE EXISTS (
     SELECT 1
       FROM public.jbpm4_deployprop property
      WHERE property.deployment_ = deployment.dbid_
        AND property.key_ = 'pdkey'
        AND property.stringval_ IN ('potrolTaskWF', 'tempTaskWF')
 );

DO $app$
DECLARE
    current_modules text;
    current_menus text;
BEGIN
    SELECT coalesce(modules, ''), coalesce(menus, '')
      INTO current_modules, current_menus
      FROM public.supos_app
     WHERE code = 'eamms'
     FOR UPDATE;

    IF FOUND THEN
        SELECT coalesce(string_agg(item, ',' ORDER BY ordinal), '')
          INTO current_modules
          FROM unnest(string_to_array(current_modules, ',')) WITH ORDINALITY AS value(item, ordinal)
         WHERE item <> 'PATROL_1.0.0';

        SELECT coalesce(string_agg(item, ',' ORDER BY ordinal), '')
          INTO current_menus
          FROM unnest(string_to_array(current_menus, ',')) WITH ORDINALITY AS value(item, ordinal)
         WHERE left(item, length('PATROL_1.0.0_')) <> 'PATROL_1.0.0_';

        UPDATE public.supos_app
           SET modules = current_modules,
               menus = current_menus,
               modify_time = CURRENT_TIMESTAMP
         WHERE code = 'eamms';
    END IF;
END $app$;

SELECT
    'PATROL metadata disabled; restore the EamMs JAR backup before recreating the service'
    AS rollback_status;
