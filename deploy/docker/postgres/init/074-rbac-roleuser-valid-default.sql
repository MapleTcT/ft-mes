-- RoleUserServiceImpl.saveRoleUsers does not set valid explicitly. The legacy
-- schema default was valid=1, but the generic boolean conversion script set
-- boolean defaults to false. Keep new role-user assignments active by default.

DO $$
BEGIN
    IF to_regclass('public.rbac_roleuser') IS NOT NULL THEN
        ALTER TABLE public.rbac_roleuser
            ALTER COLUMN valid SET DEFAULT true;
    END IF;
END $$;
