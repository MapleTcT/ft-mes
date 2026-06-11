CREATE TABLE IF NOT EXISTS public.auth_online_user (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    user_name VARCHAR(256) NOT NULL,
    person_id BIGINT,
    person_code VARCHAR(50),
    person_name VARCHAR(50),
    company_id BIGINT,
    ticket VARCHAR(50) NOT NULL,
    login_ip VARCHAR(256) NOT NULL,
    login_time TIMESTAMP NOT NULL,
    client VARCHAR(128),
    company_name VARCHAR(512),
    device_type VARCHAR(256),
    access_token VARCHAR(4000),
    creator VARCHAR(32) NOT NULL DEFAULT 'system',
    modifier VARCHAR(32),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modify_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_staff_id BIGINT,
    modify_staff_id BIGINT
);

ALTER TABLE public.auth_online_user ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE public.auth_online_user ADD COLUMN IF NOT EXISTS user_name VARCHAR(256);
ALTER TABLE public.auth_online_user ADD COLUMN IF NOT EXISTS person_id BIGINT;
ALTER TABLE public.auth_online_user ADD COLUMN IF NOT EXISTS person_code VARCHAR(50);
ALTER TABLE public.auth_online_user ADD COLUMN IF NOT EXISTS person_name VARCHAR(50);
ALTER TABLE public.auth_online_user ADD COLUMN IF NOT EXISTS company_id BIGINT;
ALTER TABLE public.auth_online_user ADD COLUMN IF NOT EXISTS ticket VARCHAR(50);
ALTER TABLE public.auth_online_user ADD COLUMN IF NOT EXISTS login_ip VARCHAR(256);
ALTER TABLE public.auth_online_user ADD COLUMN IF NOT EXISTS login_time TIMESTAMP;
ALTER TABLE public.auth_online_user ADD COLUMN IF NOT EXISTS client VARCHAR(128);
ALTER TABLE public.auth_online_user ADD COLUMN IF NOT EXISTS company_name VARCHAR(512);
ALTER TABLE public.auth_online_user ADD COLUMN IF NOT EXISTS device_type VARCHAR(256);
ALTER TABLE public.auth_online_user ADD COLUMN IF NOT EXISTS access_token VARCHAR(4000);
ALTER TABLE public.auth_online_user ADD COLUMN IF NOT EXISTS creator VARCHAR(32) DEFAULT 'system';
ALTER TABLE public.auth_online_user ADD COLUMN IF NOT EXISTS modifier VARCHAR(32);
ALTER TABLE public.auth_online_user ADD COLUMN IF NOT EXISTS create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE public.auth_online_user ADD COLUMN IF NOT EXISTS modify_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE public.auth_online_user ADD COLUMN IF NOT EXISTS create_staff_id BIGINT;
ALTER TABLE public.auth_online_user ADD COLUMN IF NOT EXISTS modify_staff_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_online_user_username ON public.auth_online_user(user_name);
CREATE INDEX IF NOT EXISTS idx_online_user_ticket ON public.auth_online_user(ticket);
CREATE INDEX IF NOT EXISTS idx_online_user_company ON public.auth_online_user(company_id);
