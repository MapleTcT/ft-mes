-- Legacy foundation services read base_menuoperate Boolean switches through a
-- numeric 0/1 Hibernate mapping. Keep rbac_menuoperate as PostgreSQL BOOLEAN
-- for newer RBAC APIs, but expose the compatibility view as smallint flags.

DO $$
BEGIN
  IF to_regclass('public.rbac_menuoperate') IS NOT NULL THEN
    ALTER TABLE public.rbac_menuoperate
      ADD COLUMN IF NOT EXISTS enable_assigndept boolean DEFAULT false,
      ADD COLUMN IF NOT EXISTS enable_deptrict boolean DEFAULT false;

    DROP VIEW IF EXISTS public.base_menuoperate;

    EXECUTE $view$
CREATE VIEW public.base_menuoperate AS
SELECT
    rbac_menuoperate.id,
    rbac_menuoperate.row_version AS version,
    rbac_menuoperate.create_staff_id,
    rbac_menuoperate.modify_staff_id,
    NULL::text AS delete_staff_id,
    rbac_menuoperate.create_time,
    rbac_menuoperate.modify_time,
    rbac_menuoperate.delete_time,
    CASE WHEN rbac_menuoperate.valid IS NULL THEN NULL WHEN lower(rbac_menuoperate.valid::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS valid,
    rbac_menuoperate.cid,
    CASE WHEN rbac_menuoperate.is_allow_proxy IS NULL THEN NULL WHEN lower(rbac_menuoperate.is_allow_proxy::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS is_allow_proxy,
    CASE WHEN rbac_menuoperate.is_hidden IS NULL THEN NULL WHEN lower(rbac_menuoperate.is_hidden::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS is_hidden,
    CASE WHEN rbac_menuoperate.three_role IS NULL THEN NULL WHEN lower(rbac_menuoperate.three_role::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS three_role,
    rbac_menuoperate.view_code,
    CASE WHEN rbac_menuoperate.is_query IS NULL THEN NULL WHEN lower(rbac_menuoperate.is_query::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS is_query,
    CASE WHEN rbac_menuoperate.is_orrelation IS NULL THEN NULL WHEN lower(rbac_menuoperate.is_orrelation::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS is_orrelation,
    CASE WHEN rbac_menuoperate.for_flow_permission IS NULL THEN NULL WHEN lower(rbac_menuoperate.for_flow_permission::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS for_data_permission,
    CASE WHEN rbac_menuoperate.enable_norestrict IS NULL THEN NULL WHEN lower(rbac_menuoperate.enable_norestrict::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS enable_norestrict,
    CASE WHEN rbac_menuoperate.enable_custompermission IS NULL THEN NULL WHEN lower(rbac_menuoperate.enable_custompermission::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS enable_otherrestrict,
    CASE WHEN rbac_menuoperate.enable_datapermission IS NULL THEN NULL WHEN lower(rbac_menuoperate.enable_datapermission::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS enable_specialpermission,
    CASE WHEN rbac_menuoperate.enable_dealerpermission IS NULL THEN NULL WHEN lower(rbac_menuoperate.enable_dealerpermission::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS enable_dealerpermission,
    CASE WHEN rbac_menuoperate.enable_assignstaff IS NULL THEN NULL WHEN lower(rbac_menuoperate.enable_assignstaff::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS enable_assignstaff,
    CASE WHEN rbac_menuoperate.enable_assignpos IS NULL THEN NULL WHEN lower(rbac_menuoperate.enable_assignpos::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS enable_assignpos,
    CASE WHEN rbac_menuoperate.enable_posrestrict IS NULL THEN NULL WHEN lower(rbac_menuoperate.enable_posrestrict::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS enable_posrestrict,
    CASE WHEN rbac_menuoperate.enable_grouprestrict IS NULL THEN NULL WHEN lower(rbac_menuoperate.enable_grouprestrict::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS enable_grouprestrict,
    rbac_menuoperate.entity_code,
    CASE WHEN rbac_menuoperate.ignore_permission IS NULL THEN NULL WHEN lower(rbac_menuoperate.ignore_permission::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS ignore_permission,
    CASE WHEN rbac_menuoperate.power_flag IS NULL THEN NULL WHEN lower(rbac_menuoperate.power_flag::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS power_flag,
    NULLIF(rbac_menuoperate.flow_version::text, '')::integer AS flow_version,
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
    rbac_menuoperate.flow_name_display,
    CASE WHEN rbac_menuoperate.enable_assigndept IS NULL THEN NULL WHEN lower(rbac_menuoperate.enable_assigndept::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS enable_assigndept,
    CASE WHEN rbac_menuoperate.enable_deptrict IS NULL THEN NULL WHEN lower(rbac_menuoperate.enable_deptrict::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS enable_deptrict
FROM public.rbac_menuoperate
$view$;
  END IF;
END $$;
