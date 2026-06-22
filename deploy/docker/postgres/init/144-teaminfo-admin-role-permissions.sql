-- Give the default admin role the recovered TeamInfo action permissions used
-- by the production module smoke tests. The UI hides permission-controlled
-- buttons when these rows are missing, even though the menu and view metadata
-- exist.

WITH target_operations AS (
    SELECT mo.id AS menuoperate_id
    FROM public.rbac_menuoperate mo
    WHERE mo.code IN (
        'TeamInfo_1.0.0_team_teamLayout_teamList_addCustom_add_TeamInfo_1.0.0_team_teamList',
        'TeamInfo_1.0.0_team_teamLayout_teamList_modify_modify_TeamInfo_1.0.0_team_teamList',
        'TeamInfo_1.0.0_team_teamLayout_teamList_delete_del_TeamInfo_1.0.0_team_teamList',
        'TeamInfo_1.0.0_team_teamLayout_teamList_import_import_TeamInfo_1.0.0_team_teamList',
        'TeamInfo_1.0.0_team_teamLayout_teamList_addCustom_teamAuthSet_TeamInfo_1.0.0_team_teamList',
        'schedulePlanList_add_add_TeamInfo_1.0.0_schedulePlan_schedulePlanList',
        'schedulePlanList_modify_modify_TeamInfo_1.0.0_schedulePlan_schedulePlanList',
        'schedulePlanList_delete_del_TeamInfo_1.0.0_schedulePlan_schedulePlanList',
        'schedulePlanList_import_import_TeamInfo_1.0.0_schedulePlan_schedulePlanList'
    )
),
missing_operations AS (
    SELECT
        toper.menuoperate_id,
        row_number() OVER (ORDER BY toper.menuoperate_id) AS rn
    FROM target_operations toper
    WHERE NOT EXISTS (
        SELECT 1
        FROM public.rbac_rolepermission rp
        WHERE rp.role_id = 1
          AND rp.menuoperate_id = toper.menuoperate_id
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
    'codex_teaminfo_perm_20260619',
    'codex_teaminfo_perm_20260619',
    1,
    1
FROM missing_operations
CROSS JOIN id_base;
