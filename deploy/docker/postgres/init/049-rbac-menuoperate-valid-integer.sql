-- The configuration import path maps rbac_menuoperate.VALID as Integer and
-- writes 0/1 values. Convert only this column; other menu operation switches
-- are mapped as Boolean by the same Hibernate entity.
DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'rbac_menuoperate'
      AND column_name = 'valid'
      AND data_type = 'boolean'
  ) THEN
    DROP VIEW IF EXISTS public.base_menuoperate;

    ALTER TABLE public.rbac_menuoperate
      ALTER COLUMN valid DROP DEFAULT,
      ALTER COLUMN valid TYPE integer
        USING CASE WHEN valid THEN 1 ELSE 0 END,
      ALTER COLUMN valid SET DEFAULT 1;
  END IF;
END $$;

CREATE OR REPLACE VIEW public.base_menuoperate AS
SELECT rbac_menuoperate.id,
  rbac_menuoperate.row_version AS version,
  rbac_menuoperate.create_staff_id,
  rbac_menuoperate.modify_staff_id,
  NULL::text AS delete_staff_id,
  rbac_menuoperate.create_time,
  rbac_menuoperate.modify_time,
  rbac_menuoperate.delete_time,
  rbac_menuoperate.valid::smallint AS valid,
  rbac_menuoperate.cid,
  rbac_menuoperate.is_allow_proxy,
  rbac_menuoperate.is_hidden,
  rbac_menuoperate.three_role,
  rbac_menuoperate.view_code,
  rbac_menuoperate.is_query,
  rbac_menuoperate.is_orrelation,
  rbac_menuoperate.for_flow_permission AS for_data_permission,
  rbac_menuoperate.enable_norestrict,
  rbac_menuoperate.enable_custompermission AS enable_otherrestrict,
  rbac_menuoperate.enable_datapermission AS enable_specialpermission,
  rbac_menuoperate.enable_dealerpermission,
  rbac_menuoperate.enable_assignstaff,
  rbac_menuoperate.enable_assignpos,
  rbac_menuoperate.enable_posrestrict,
  rbac_menuoperate.enable_grouprestrict,
  rbac_menuoperate.entity_code,
  rbac_menuoperate.ignore_permission,
  rbac_menuoperate.power_flag,
  NULLIF(rbac_menuoperate.flow_version::text, ''::text)::integer AS flow_version,
  rbac_menuoperate.flow_key,
  rbac_menuoperate.msg_assembled,
  rbac_menuoperate.deployment_id,
  rbac_menuoperate.menuoperatetype AS type,
  rbac_menuoperate.menuinfo_id,
  rbac_menuoperate.icon_cls,
  rbac_menuoperate.module_code AS module,
  rbac_menuoperate.sort,
  rbac_menuoperate.memo,
  rbac_menuoperate.target,
  rbac_menuoperate.namespace,
  rbac_menuoperate.url,
  rbac_menuoperate.name_zh_cn,
  rbac_menuoperate.name,
  rbac_menuoperate.code,
  rbac_menuoperate.action_url AS action,
  0 AS st_flag,
  NULL::text AS st_type,
  NULL::text AS st_tablecode,
  NULL::text AS st_showstyle,
  NULL::text AS st_operatetype,
  NULL::text AS st_isview,
  NULL::text AS st_iss2flowoperate,
  NULL::text AS st_ismainquery,
  NULL::text AS st_isdefault,
  NULL::text AS st_flowkey,
  NULL::text AS st_digitalsignature,
  NULL::text AS st_defaultdisplay,
  NULL::text AS st_activityid,
  NULL::text AS menuoperate_mainoperatecode,
  NULL::text AS menuoperate_iscontainer,
  NULL::text AS menuoperate_entryoperatecode,
  rbac_menuoperate.menuoperatetype,
  rbac_menuoperate.flow_name,
  rbac_menuoperate.flow_name_display
FROM public.rbac_menuoperate;
