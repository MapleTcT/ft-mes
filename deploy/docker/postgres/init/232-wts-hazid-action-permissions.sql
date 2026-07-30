-- Split the recovered WTS hazard-library editor permission into the distinct
-- add/modify operations used by the restored runtime list view.
--
-- The vendor metadata granted one legacy "编辑" operation. The recovered
-- browser layout exposes separate "新增" and "修改" buttons, so baseService
-- filtered both buttons until matching RBAC operations existed. Preserve every
-- existing role/user assignment from the legacy operation.

BEGIN;

WITH source_operation AS (
    SELECT *
    FROM public.rbac_menuoperate
    WHERE code = 'hazidLibList_add_modify_WTS_1.0.0_hazidLib_hazidLibList'
      AND valid = 1
    ORDER BY id
    LIMIT 1
),
desired_operations (
    code,
    name,
    name_display,
    icon_cls,
    sort
) AS (
    VALUES
        (
            'hazidLibList_add_add_WTS_1.0.0_hazidLib_hazidLibList',
            'WTS.buttonPropertyshowName.randon1573731918669.flag',
            '新增',
            'cui-btn-add',
            0::double precision
        ),
        (
            'hazidLibList_modify_modify_WTS_1.0.0_hazidLib_hazidLibList',
            'WTS.buttonPropertyshowName.randon1573731933752.flag',
            '修改',
            'cui-btn-modify',
            1::double precision
        )
),
missing_operations AS (
    SELECT
        desired.*,
        row_number() OVER (ORDER BY desired.sort, desired.code) AS rn
    FROM desired_operations desired
    WHERE NOT EXISTS (
        SELECT 1
        FROM public.rbac_menuoperate current_operation
        WHERE current_operation.code = desired.code
          AND current_operation.valid = 1
    )
),
id_base AS (
    SELECT COALESCE(max(id), 6579649077035249) AS base_id
    FROM public.rbac_menuoperate
)
INSERT INTO public.rbac_menuoperate (
    id,
    row_version,
    version,
    modify_time,
    create_time,
    modifier,
    creator,
    create_staff_id,
    modify_staff_id,
    valid,
    cid,
    is_allow_proxy,
    is_hidden,
    three_role,
    view_code,
    is_query,
    is_orrelation,
    enable_datapermission,
    enable_custompermission,
    for_flow_permission,
    enable_norestrict,
    enable_dealerpermission,
    enable_assignstaff,
    enable_assignpos,
    enable_posrestrict,
    enable_deptrict,
    enable_assigndept,
    enable_grouprestrict,
    entity_code,
    ignore_permission,
    power_flag,
    menuoperatetype,
    menuinfo_id,
    icon_cls,
    module_code,
    sort,
    memo,
    target,
    action_url,
    namespace,
    url,
    name_zh_cn,
    name,
    name_display,
    code,
    app,
    default_operate,
    edited
)
SELECT
    id_base.base_id + missing.rn,
    source.row_version,
    source.version,
    now(),
    now(),
    'codex_wts_basic_20260730',
    'codex_wts_basic_20260730',
    COALESCE(source.create_staff_id, 1),
    COALESCE(source.modify_staff_id, 1),
    source.valid,
    source.cid,
    source.is_allow_proxy,
    source.is_hidden,
    source.three_role,
    source.view_code,
    source.is_query,
    source.is_orrelation,
    source.enable_datapermission,
    source.enable_custompermission,
    source.for_flow_permission,
    source.enable_norestrict,
    source.enable_dealerpermission,
    source.enable_assignstaff,
    source.enable_assignpos,
    source.enable_posrestrict,
    source.enable_deptrict,
    source.enable_assigndept,
    source.enable_grouprestrict,
    source.entity_code,
    source.ignore_permission,
    source.power_flag,
    source.menuoperatetype,
    source.menuinfo_id,
    missing.icon_cls,
    source.module_code,
    missing.sort,
    source.memo,
    source.target,
    source.action_url,
    source.namespace,
    source.url,
    missing.name_display,
    missing.name,
    missing.name_display,
    missing.code,
    source.app,
    source.default_operate,
    source.edited
FROM missing_operations missing
CROSS JOIN source_operation source
CROSS JOIN id_base;

WITH source_operation AS (
    SELECT id
    FROM public.rbac_menuoperate
    WHERE code = 'hazidLibList_add_modify_WTS_1.0.0_hazidLib_hazidLibList'
      AND valid = 1
    ORDER BY id
    LIMIT 1
),
target_operations AS (
    SELECT id, code
    FROM public.rbac_menuoperate
    WHERE code IN (
        'hazidLibList_add_add_WTS_1.0.0_hazidLib_hazidLibList',
        'hazidLibList_modify_modify_WTS_1.0.0_hazidLib_hazidLibList'
    )
      AND valid = 1
),
source_permissions AS (
    SELECT permission.*
    FROM public.rbac_rolepermission permission
    JOIN source_operation source
      ON source.id = permission.menuoperate_id
    WHERE permission.delete_time IS NULL
),
missing_permissions AS (
    SELECT
        source_permissions.*,
        target_operations.id AS target_menuoperate_id,
        row_number() OVER (
            ORDER BY source_permissions.role_id, target_operations.code
        ) AS rn
    FROM source_permissions
    CROSS JOIN target_operations
    WHERE NOT EXISTS (
        SELECT 1
        FROM public.rbac_rolepermission current_permission
        WHERE current_permission.role_id = source_permissions.role_id
          AND current_permission.menuoperate_id = target_operations.id
          AND current_permission.delete_time IS NULL
    )
),
id_base AS (
    SELECT COALESCE(max(id), 6576396574805000) AS base_id
    FROM public.rbac_rolepermission
)
INSERT INTO public.rbac_rolepermission (
    id,
    cid,
    version,
    role_id,
    menuoperate_id,
    position_flag,
    department_flag,
    group_flag,
    assign_staff_flag,
    assign_pos_flag,
    assign_dept_flag,
    dealer_permission_flag,
    no_restrict_flag,
    assign_datapermission_flag,
    assign_custompermission_flag,
    url_pattern,
    modify_time,
    create_time,
    modifier,
    creator,
    create_staff_id,
    modify_staff_id,
    assign_departments,
    assign_positions,
    assign_staffs
)
SELECT
    id_base.base_id + missing.rn,
    missing.cid,
    missing.version,
    missing.role_id,
    missing.target_menuoperate_id,
    missing.position_flag,
    missing.department_flag,
    missing.group_flag,
    missing.assign_staff_flag,
    missing.assign_pos_flag,
    missing.assign_dept_flag,
    missing.dealer_permission_flag,
    missing.no_restrict_flag,
    missing.assign_datapermission_flag,
    missing.assign_custompermission_flag,
    missing.url_pattern,
    now(),
    now(),
    'codex_wts_basic_20260730',
    'codex_wts_basic_20260730',
    COALESCE(missing.create_staff_id, 1),
    COALESCE(missing.modify_staff_id, 1),
    missing.assign_departments,
    missing.assign_positions,
    missing.assign_staffs
FROM missing_permissions missing
CROSS JOIN id_base;

WITH source_operation AS (
    SELECT id
    FROM public.rbac_menuoperate
    WHERE code = 'hazidLibList_add_modify_WTS_1.0.0_hazidLib_hazidLibList'
      AND valid = 1
    ORDER BY id
    LIMIT 1
),
target_operations AS (
    SELECT id, code
    FROM public.rbac_menuoperate
    WHERE code IN (
        'hazidLibList_add_add_WTS_1.0.0_hazidLib_hazidLibList',
        'hazidLibList_modify_modify_WTS_1.0.0_hazidLib_hazidLibList'
    )
      AND valid = 1
),
source_permissions AS (
    SELECT permission.*
    FROM public.rbac_userpermission permission
    JOIN source_operation source
      ON source.id = permission.menuoperate_id
    WHERE permission.delete_time IS NULL
),
missing_permissions AS (
    SELECT
        source_permissions.*,
        target_operations.id AS target_menuoperate_id,
        target_operations.code AS target_menuoperate_code,
        row_number() OVER (
            ORDER BY source_permissions.user_id, target_operations.code
        ) AS rn
    FROM source_permissions
    CROSS JOIN target_operations
    WHERE NOT EXISTS (
        SELECT 1
        FROM public.rbac_userpermission current_permission
        WHERE current_permission.user_id = source_permissions.user_id
          AND current_permission.menuoperate_id = target_operations.id
          AND current_permission.purview_type = source_permissions.purview_type
          AND current_permission.delete_time IS NULL
    )
),
id_base AS (
    SELECT COALESCE(max(id), 6576396574806000) AS base_id
    FROM public.rbac_userpermission
)
INSERT INTO public.rbac_userpermission (
    id,
    version,
    user_id,
    deal_staff,
    cid,
    menuoperate_id,
    purview_type,
    position_flag,
    department_flag,
    group_flag,
    assign_staff_flag,
    assign_pos_flag,
    assign_dept_flag,
    dealer_permission_flag,
    no_restrict_flag,
    assign_datapermission_flag,
    assign_custompermission_flag,
    url_pattern,
    menuoperate_code,
    modify_time,
    create_time,
    modifier,
    creator,
    create_staff_id,
    modify_staff_id,
    assign_departments,
    assign_positions,
    assign_staffs,
    assign_custompermissions
)
SELECT
    id_base.base_id + missing.rn,
    missing.version,
    missing.user_id,
    missing.deal_staff,
    missing.cid,
    missing.target_menuoperate_id,
    missing.purview_type,
    missing.position_flag,
    missing.department_flag,
    missing.group_flag,
    missing.assign_staff_flag,
    missing.assign_pos_flag,
    missing.assign_dept_flag,
    missing.dealer_permission_flag,
    missing.no_restrict_flag,
    missing.assign_datapermission_flag,
    missing.assign_custompermission_flag,
    missing.url_pattern,
    missing.target_menuoperate_code,
    now(),
    now(),
    'codex_wts_basic_20260730',
    'codex_wts_basic_20260730',
    COALESCE(missing.create_staff_id, 1),
    COALESCE(missing.modify_staff_id, 1),
    missing.assign_departments,
    missing.assign_positions,
    missing.assign_staffs,
    missing.assign_custompermissions
FROM missing_permissions missing
CROSS JOIN id_base;

DO $validation$
DECLARE
    operation_count integer;
    admin_permission_count integer;
BEGIN
    SELECT count(*)
    INTO operation_count
    FROM public.rbac_menuoperate
    WHERE code IN (
        'hazidLibList_add_add_WTS_1.0.0_hazidLib_hazidLibList',
        'hazidLibList_modify_modify_WTS_1.0.0_hazidLib_hazidLibList'
    )
      AND valid = 1;

    SELECT count(*)
    INTO admin_permission_count
    FROM public.rbac_rolepermission permission
    JOIN public.rbac_menuoperate operation
      ON operation.id = permission.menuoperate_id
    WHERE permission.role_id = 1
      AND permission.delete_time IS NULL
      AND operation.code IN (
          'hazidLibList_add_add_WTS_1.0.0_hazidLib_hazidLibList',
          'hazidLibList_modify_modify_WTS_1.0.0_hazidLib_hazidLibList'
      )
      AND operation.valid = 1;

    IF operation_count <> 2 OR admin_permission_count <> 2 THEN
        RAISE EXCEPTION
            'WTS hazard-library RBAC split incomplete: operations %, admin permissions %',
            operation_count,
            admin_permission_count;
    END IF;
END $validation$;

COMMIT;
