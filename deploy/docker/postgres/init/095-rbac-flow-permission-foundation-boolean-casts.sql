-- Foundation workflow permission services hydrate base_datapermission through
-- numeric Boolean mappings. Keep the RBAC tables PostgreSQL-native while
-- exposing legacy-compatible 0/1 flags from the base_* compatibility views.

DO $$
BEGIN
  IF to_regclass('public.rbac_flow_permission') IS NOT NULL THEN
    DROP VIEW IF EXISTS public.base_datapermission;

    EXECUTE $view$
CREATE VIEW public.base_datapermission AS
SELECT
    id,
    version,
    create_staff_id,
    modify_staff_id,
    NULL::bigint AS delete_staff_id,
    create_time,
    modify_time,
    delete_time,
    1::smallint AS valid,
    entity_code,
    purview_distribution,
    purview_state,
    memo,
    CASE WHEN unlimited_power IS NULL THEN NULL WHEN lower(unlimited_power::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS unlimited_power,
    CASE WHEN group_power_flag IS NULL THEN NULL WHEN lower(group_power_flag::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS group_power_flag,
    CASE WHEN assign_staff_flag IS NULL THEN NULL WHEN lower(assign_staff_flag::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS assign_staff_flag,
    CASE WHEN assign_pos_flag IS NULL THEN NULL WHEN lower(assign_pos_flag::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS assign_pos_flag,
    CASE WHEN position_power_flag IS NULL THEN NULL WHEN lower(position_power_flag::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS position_power_flag,
    flow_permission_type AS data_permission_type,
    type_id,
    activity_code,
    NULLIF(flow_version::text, '')::integer AS flow_version,
    flow_key,
    cid
FROM public.rbac_flow_permission
$view$;
  END IF;

  IF to_regclass('public.rbac_flow_permission_position') IS NOT NULL THEN
    DROP VIEW IF EXISTS public.base_datapmsposition;

    EXECUTE $view$
CREATE VIEW public.base_datapmsposition AS
SELECT
    id,
    version,
    position_id,
    CASE WHEN include_lower IS NULL THEN NULL WHEN lower(include_lower::text) IN ('1', 't', 'true', 'y', 'yes') THEN 1 ELSE 0 END::smallint AS include_lower,
    flowpermission_id AS datapermission_id
FROM public.rbac_flow_permission_position
$view$;
  END IF;
END $$;
