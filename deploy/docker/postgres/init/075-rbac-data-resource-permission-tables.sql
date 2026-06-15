-- PostgreSQL compatibility for RBAC data resource permission persistence.
-- These tables are used by /inter-api/rbac/v1/{role|user}/{id}/data/resource/{groupCode}.

CREATE TABLE IF NOT EXISTS public.rbac_role_data_permission (
    id BIGINT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    cid BIGINT NOT NULL,
    resource_code VARCHAR(512) NOT NULL,
    resource_name VARCHAR(512) NOT NULL,
    resource_type VARCHAR(512),
    group_code VARCHAR(512) NOT NULL,
    valid BOOLEAN NOT NULL DEFAULT true,
    creator VARCHAR(32) DEFAULT 'system',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_staff_id BIGINT NOT NULL DEFAULT 0,
    modifier VARCHAR(32),
    modify_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modify_staff_id BIGINT
);

CREATE TABLE IF NOT EXISTS public.rbac_role_data_permission_ctrl (
    id BIGINT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    cid BIGINT NOT NULL,
    group_code VARCHAR(512) NOT NULL,
    controlled INTEGER NOT NULL DEFAULT 1,
    creator VARCHAR(32) DEFAULT 'system',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_staff_id BIGINT NOT NULL DEFAULT 0,
    modifier VARCHAR(32),
    modify_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modify_staff_id BIGINT
);

CREATE UNIQUE INDEX IF NOT EXISTS udx_rbac_role_data_ctrl
    ON public.rbac_role_data_permission_ctrl (role_id, cid, group_code);

CREATE INDEX IF NOT EXISTS idx_rbac_role_data_permission_role
    ON public.rbac_role_data_permission (role_id, cid, group_code);

CREATE TABLE IF NOT EXISTS public.rbac_user_data_permission (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    cid BIGINT NOT NULL,
    resource_code VARCHAR(512) NOT NULL,
    resource_name VARCHAR(512) NOT NULL,
    resource_type VARCHAR(512),
    group_code VARCHAR(512) NOT NULL,
    role_id BIGINT,
    purview_type INTEGER NOT NULL,
    valid BOOLEAN NOT NULL DEFAULT true,
    creator VARCHAR(32) DEFAULT 'system',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_staff_id BIGINT NOT NULL DEFAULT 0,
    modifier VARCHAR(32),
    modify_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modify_staff_id BIGINT
);

CREATE TABLE IF NOT EXISTS public.rbac_user_data_permission_ctrl (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    cid BIGINT NOT NULL,
    group_code VARCHAR(512) NOT NULL,
    controlled INTEGER NOT NULL DEFAULT 1,
    creator VARCHAR(32) DEFAULT 'system',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_staff_id BIGINT NOT NULL DEFAULT 0,
    modifier VARCHAR(32),
    modify_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modify_staff_id BIGINT
);

CREATE UNIQUE INDEX IF NOT EXISTS udx_rbac_user_data_ctrl
    ON public.rbac_user_data_permission_ctrl (user_id, cid, group_code);

CREATE INDEX IF NOT EXISTS idx_rbac_user_data_permission_user
    ON public.rbac_user_data_permission (user_id, cid, group_code, purview_type);

CREATE INDEX IF NOT EXISTS idx_rbac_user_data_permission_role
    ON public.rbac_user_data_permission (role_id, cid, group_code)
    WHERE role_id IS NOT NULL;

ALTER TABLE public.rbac_role_data_permission
    ALTER COLUMN valid SET DEFAULT true,
    ALTER COLUMN create_time SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN create_staff_id SET DEFAULT 0;

ALTER TABLE public.rbac_role_data_permission_ctrl
    ALTER COLUMN controlled SET DEFAULT 1,
    ALTER COLUMN create_time SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN create_staff_id SET DEFAULT 0;

ALTER TABLE public.rbac_user_data_permission
    ALTER COLUMN valid SET DEFAULT true,
    ALTER COLUMN create_time SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN create_staff_id SET DEFAULT 0;

ALTER TABLE public.rbac_user_data_permission_ctrl
    ALTER COLUMN controlled SET DEFAULT 1,
    ALTER COLUMN create_time SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN create_staff_id SET DEFAULT 0;

CREATE SEQUENCE IF NOT EXISTS public.adp_rbac_data_permission_id_seq
    START WITH 900000000000000000
    INCREMENT BY 1;

CREATE OR REPLACE FUNCTION public.adp_sync_rbac_role_data_permission_to_users()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        IF COALESCE(NEW.valid, true) THEN
            INSERT INTO public.rbac_user_data_permission (
                id,
                user_id,
                cid,
                resource_code,
                resource_name,
                resource_type,
                group_code,
                role_id,
                purview_type,
                valid,
                creator,
                create_staff_id
            )
            SELECT
                nextval('public.adp_rbac_data_permission_id_seq'),
                ru.user_id,
                NEW.cid,
                NEW.resource_code,
                NEW.resource_name,
                NEW.resource_type,
                NEW.group_code,
                NEW.role_id,
                0,
                true,
                COALESCE(NEW.creator, 'system'),
                COALESCE(NEW.create_staff_id, 0)
            FROM public.rbac_roleuser ru
            WHERE ru.role_id = NEW.role_id
              AND COALESCE(ru.valid, false) = true
              AND NOT EXISTS (
                  SELECT 1
                  FROM public.rbac_user_data_permission existing
                  WHERE existing.user_id = ru.user_id
                    AND existing.cid = NEW.cid
                    AND existing.group_code = NEW.group_code
                    AND existing.role_id = NEW.role_id
                    AND existing.purview_type = 0
                    AND existing.resource_code = NEW.resource_code
                    AND COALESCE(existing.valid, false) = true
              );
        END IF;
        RETURN NEW;
    END IF;

    IF TG_OP = 'UPDATE' THEN
        IF COALESCE(OLD.valid, true) = true AND COALESCE(NEW.valid, true) = false THEN
            UPDATE public.rbac_user_data_permission
            SET valid = false,
                modify_time = CURRENT_TIMESTAMP
            WHERE role_id = OLD.role_id
              AND cid = OLD.cid
              AND group_code = OLD.group_code
              AND purview_type = 0
              AND resource_code = OLD.resource_code
              AND COALESCE(valid, false) = true;
        END IF;
        RETURN NEW;
    END IF;

    IF TG_OP = 'DELETE' THEN
        UPDATE public.rbac_user_data_permission
        SET valid = false,
            modify_time = CURRENT_TIMESTAMP
        WHERE role_id = OLD.role_id
          AND cid = OLD.cid
          AND group_code = OLD.group_code
          AND purview_type = 0
          AND resource_code = OLD.resource_code
          AND COALESCE(valid, false) = true;
        RETURN OLD;
    END IF;

    RETURN NULL;
END $$;

DROP TRIGGER IF EXISTS trg_rbac_role_data_permission_sync_users
    ON public.rbac_role_data_permission;

CREATE TRIGGER trg_rbac_role_data_permission_sync_users
AFTER INSERT OR UPDATE OF valid OR DELETE
ON public.rbac_role_data_permission
FOR EACH ROW
EXECUTE FUNCTION public.adp_sync_rbac_role_data_permission_to_users();
