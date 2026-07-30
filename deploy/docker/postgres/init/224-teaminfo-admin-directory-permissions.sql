-- Complete the default admin role's TeamInfo directory permissions.
-- The menu entries rendered for admin, but query/default operations remained
-- unassigned and could hide controls or reject the corresponding page action.

WITH target_operations AS (
    SELECT mo.id AS menuoperate_id
    FROM public.rbac_menuoperate mo
    WHERE mo.code IN (
        'TeamInfo_1.0.0_team_teamDeptLayout_self',
        'TeamInfo_1.0.0_team_teamLayout_self',
        'TeamInfo_1.0.0_schedulePlan_schedulePlanList_default',
        'TeamInfo_1.0.0_schedulePlan_schedulePlanList_self'
    )
),
missing_operations AS (
    SELECT
        target.menuoperate_id,
        row_number() OVER (ORDER BY target.menuoperate_id) AS rn
    FROM target_operations target
    WHERE NOT EXISTS (
        SELECT 1
        FROM public.rbac_rolepermission current_permission
        WHERE current_permission.role_id = 1
          AND current_permission.menuoperate_id = target.menuoperate_id
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
    create_time,
    modify_time,
    creator,
    modifier,
    create_staff_id,
    modify_staff_id
)
SELECT
    id_base.base_id + missing_operations.rn,
    1000,
    0,
    1,
    missing_operations.menuoperate_id,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    1,
    0,
    0,
    now(),
    now(),
    'codex_admin_scan_20260730',
    'codex_admin_scan_20260730',
    1,
    1
FROM missing_operations
CROSS JOIN id_base;
