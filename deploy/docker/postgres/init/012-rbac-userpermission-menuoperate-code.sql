-- Backfill operation codes used by RBAC URL refresh. The recovered seed data
-- can contain menuoperate_id without menuoperate_code, which makes the legacy
-- MyBatis IN clause render invalid SQL during first login.

DO $$
BEGIN
    IF to_regclass('public.rbac_userpermission') IS NOT NULL
       AND to_regclass('public.rbac_menuoperate') IS NOT NULL THEN
        UPDATE rbac_userpermission up
        SET menuoperate_code = mo.code
        FROM rbac_menuoperate mo
        WHERE up.menuoperate_id = mo.id
          AND (up.menuoperate_code IS NULL OR btrim(up.menuoperate_code) = '');
    END IF;
END $$;
