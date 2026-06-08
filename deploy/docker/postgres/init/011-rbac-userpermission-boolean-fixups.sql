-- Convert user permission flags to PostgreSQL BOOLEAN for MyBatis Boolean
-- writes, while keeping the legacy base_userpermission view exposed as 0/1
-- integers for older Hibernate/configuration modules.

DROP VIEW IF EXISTS base_userpermission;

DO $$
DECLARE
    item RECORD;
    current_type TEXT;
BEGIN
    FOR item IN
        SELECT * FROM (VALUES
            ('position_flag'),
            ('department_flag'),
            ('assign_staff_flag'),
            ('assign_pos_flag'),
            ('assign_dept_flag'),
            ('dealer_permission_flag'),
            ('no_restrict_flag'),
            ('assign_datapermission_flag'),
            ('assign_custompermission_flag')
        ) AS columns(column_name)
    LOOP
        SELECT data_type
        INTO current_type
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'rbac_userpermission'
          AND column_name = item.column_name;

        IF current_type IS NOT NULL AND current_type <> 'boolean' THEN
            EXECUTE format('ALTER TABLE rbac_userpermission ALTER COLUMN %I DROP DEFAULT', item.column_name);
            EXECUTE format(
                'ALTER TABLE rbac_userpermission ALTER COLUMN %I TYPE BOOLEAN USING COALESCE(%I, 0) <> 0',
                item.column_name,
                item.column_name
            );
            EXECUTE format('ALTER TABLE rbac_userpermission ALTER COLUMN %I SET DEFAULT false', item.column_name);
        END IF;
    END LOOP;
END $$;

CREATE OR REPLACE VIEW base_userpermission (
    id,
    version,
    user_id,
    staff_id,
    cid,
    menuoperate_id,
    purview_type,
    position_flag,
    group_flag,
    assign_staff_flag,
    assign_pos_flag,
    dealer_permission_flag,
    no_restrict_flag,
    assign_otherrestrict_flag,
    assign_specialpermission_flag,
    url_pattern,
    create_staff_id,
    modify_staff_id,
    delete_staff_id,
    create_time,
    modify_time,
    delete_time,
    st_state,
    st_powerflag,
    st_powercode,
    st_operatestate,
    st_operateid,
    st_modulecode,
    st_menuid,
    st_memo,
    st_flag,
    deal_time
) AS
SELECT
    id,
    version,
    user_id,
    deal_staff AS staff_id,
    cid,
    menuoperate_id,
    purview_type,
    CASE WHEN position_flag IS NULL THEN NULL WHEN position_flag THEN 1 ELSE 0 END AS position_flag,
    group_flag,
    CASE WHEN assign_staff_flag IS NULL THEN NULL WHEN assign_staff_flag THEN 1 ELSE 0 END AS assign_staff_flag,
    CASE WHEN assign_pos_flag IS NULL THEN NULL WHEN assign_pos_flag THEN 1 ELSE 0 END AS assign_pos_flag,
    CASE WHEN dealer_permission_flag IS NULL THEN NULL WHEN dealer_permission_flag THEN 1 ELSE 0 END AS dealer_permission_flag,
    CASE WHEN no_restrict_flag IS NULL THEN NULL WHEN no_restrict_flag THEN 1 ELSE 0 END AS no_restrict_flag,
    CASE WHEN assign_custompermission_flag IS NULL THEN NULL WHEN assign_custompermission_flag THEN 1 ELSE 0 END AS assign_otherrestrict_flag,
    CASE WHEN assign_datapermission_flag IS NULL THEN NULL WHEN assign_datapermission_flag THEN 1 ELSE 0 END AS assign_specialpermission_flag,
    url_pattern,
    create_staff_id,
    modify_staff_id,
    NULL AS delete_staff_id,
    create_time,
    modify_time,
    delete_time,
    NULL AS st_state,
    NULL AS st_powerflag,
    NULL AS st_powercode,
    NULL AS st_operatestate,
    NULL AS st_operateid,
    NULL AS st_modulecode,
    NULL AS st_menuid,
    NULL AS st_memo,
    NULL AS st_flag,
    NULL AS deal_time
FROM rbac_userpermission;
