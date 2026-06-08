-- MyBatis-Plus binds Java Boolean values as PostgreSQL booleans. Legacy MySQL
-- DDL represented those fields as tinyint(1), so convert known RBAC Boolean
-- columns to BOOLEAN after the RBAC tables exist.

DROP VIEW IF EXISTS base_rolepermission;
DROP VIEW IF EXISTS base_menuoperate;
DROP VIEW IF EXISTS base_menuinfo;

DO $$
DECLARE
    item RECORD;
    current_type TEXT;
BEGIN
    FOR item IN
        SELECT * FROM (VALUES
            ('rbac_menuinfo', 'absolute_hidden'),
            ('rbac_menuinfo', 'three_role'),
            ('rbac_menuinfo', 'is_hide'),
            ('rbac_menuinfo', 'group_only'),
            ('rbac_menuinfo', 'system_default'),
            ('rbac_menuinfo', 'enable'),
            ('rbac_menuinfo', 'leaf'),
            ('rbac_menuinfo', 'edited'),
            ('rbac_menuinfo', 'no_restrict'),
            ('rbac_menuoperate', 'is_allow_proxy'),
            ('rbac_menuoperate', 'is_hidden'),
            ('rbac_menuoperate', 'three_role'),
            ('rbac_menuoperate', 'is_query'),
            ('rbac_menuoperate', 'is_orrelation'),
            ('rbac_menuoperate', 'enable_datapermission'),
            ('rbac_menuoperate', 'enable_custompermission'),
            ('rbac_menuoperate', 'for_flow_permission'),
            ('rbac_menuoperate', 'enable_norestrict'),
            ('rbac_menuoperate', 'enable_dealerpermission'),
            ('rbac_menuoperate', 'enable_assignstaff'),
            ('rbac_menuoperate', 'enable_assignpos'),
            ('rbac_menuoperate', 'enable_posrestrict'),
            ('rbac_menuoperate', 'enable_assigndept'),
            ('rbac_menuoperate', 'enable_deptrict'),
            ('rbac_menuoperate', 'enable_grouprestrict'),
            ('rbac_menuoperate', 'ignore_permission'),
            ('rbac_menuoperate', 'power_flag'),
            ('rbac_menuoperate', 'default_operate'),
            ('rbac_menuoperate', 'edited'),
            ('rbac_menuoperatecode_url_ref', 'reg_match'),
            ('rbac_menuoperatecode_url_ref', 'is_custom'),
            ('rbac_rolepermission', 'position_flag'),
            ('rbac_rolepermission', 'department_flag'),
            ('rbac_rolepermission', 'assign_staff_flag'),
            ('rbac_rolepermission', 'assign_pos_flag'),
            ('rbac_rolepermission', 'assign_dept_flag'),
            ('rbac_rolepermission', 'dealer_permission_flag'),
            ('rbac_rolepermission', 'no_restrict_flag'),
            ('rbac_rolepermission', 'assign_datapermission_flag'),
            ('rbac_rolepermission', 'assign_custompermission_flag'),
            ('rbac_userpermission', 'position_flag'),
            ('rbac_userpermission', 'department_flag'),
            ('rbac_userpermission', 'assign_staff_flag'),
            ('rbac_userpermission', 'assign_pos_flag'),
            ('rbac_userpermission', 'assign_dept_flag'),
            ('rbac_userpermission', 'dealer_permission_flag'),
            ('rbac_userpermission', 'no_restrict_flag'),
            ('rbac_userpermission', 'assign_datapermission_flag'),
            ('rbac_userpermission', 'assign_custompermission_flag'),
            ('rbac_rolepposition', 'include_lower'),
            ('rbac_rolepdepartment', 'include_lower'),
            ('rbac_userpposition', 'include_lower'),
            ('rbac_userpdepartment', 'include_lower')
        ) AS columns(table_name, column_name)
    LOOP
        SELECT data_type
        INTO current_type
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = item.table_name
          AND column_name = item.column_name;

        IF current_type IS NOT NULL AND current_type <> 'boolean' THEN
            EXECUTE format('ALTER TABLE %I ALTER COLUMN %I DROP DEFAULT', item.table_name, item.column_name);
            EXECUTE format(
                'ALTER TABLE %I ALTER COLUMN %I TYPE BOOLEAN USING COALESCE(%I, 0) <> 0',
                item.table_name,
                item.column_name,
                item.column_name
            );
            EXECUTE format('ALTER TABLE %I ALTER COLUMN %I SET DEFAULT false', item.table_name, item.column_name);
        END IF;
    END LOOP;
END $$;
