CREATE TABLE IF NOT EXISTS public.personal_theme (
  id BIGINT PRIMARY KEY,
  row_version BIGINT DEFAULT 0,
  user_id BIGINT,
  theme VARCHAR(500) NOT NULL,
  font SMALLINT DEFAULT 12,
  status SMALLINT DEFAULT 0,
  type VARCHAR(500) NOT NULL,
  creator VARCHAR(200),
  modifier VARCHAR(200),
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  modify_time TIMESTAMP,
  tenant_id VARCHAR(64)
);

DO $$
BEGIN
  IF to_regclass('public.ec_portlet') IS NOT NULL THEN
    ALTER TABLE public.ec_portlet
      ADD COLUMN IF NOT EXISTS tab_config TEXT,
      ADD COLUMN IF NOT EXISTS tab_flag BOOLEAN DEFAULT FALSE,
      ADD COLUMN IF NOT EXISTS more_func TEXT,
      ADD COLUMN IF NOT EXISTS status_flag BOOLEAN DEFAULT TRUE;

    IF EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = 'public' AND table_name = 'ec_portlet'
        AND column_name = 'iframe_flag' AND data_type <> 'boolean'
    ) THEN
      ALTER TABLE public.ec_portlet
        ALTER COLUMN iframe_flag TYPE BOOLEAN
        USING COALESCE(iframe_flag, 0) <> 0;
    END IF;

    IF EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = 'public' AND table_name = 'ec_portlet'
        AND column_name = 'power_flag' AND data_type <> 'boolean'
    ) THEN
      ALTER TABLE public.ec_portlet
        ALTER COLUMN power_flag TYPE BOOLEAN
        USING COALESCE(power_flag, 0) <> 0;
    END IF;

    IF EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = 'public' AND table_name = 'ec_portlet'
        AND column_name = 'is_default' AND data_type <> 'boolean'
    ) THEN
      ALTER TABLE public.ec_portlet
        ALTER COLUMN is_default TYPE BOOLEAN
        USING COALESCE(is_default, 0) <> 0;
    END IF;
  END IF;
END $$;

UPDATE public.ec_portlet
SET status_flag = TRUE
WHERE code IN ('myProcess', 'pendingNotice');

UPDATE public.ec_portlet
SET scope_num = 0
WHERE code IN ('myProcess', 'pendingNotice')
  AND scope_num IS NULL;

CREATE TABLE IF NOT EXISTS public.ec_portal_company (
  id BIGINT PRIMARY KEY,
  portlet_code VARCHAR(512),
  cid BIGINT
);

CREATE TABLE IF NOT EXISTS public.ec_portal_homepage (
  id BIGINT PRIMARY KEY,
  code VARCHAR(512),
  title_key VARCHAR(512),
  power_flag BOOLEAN DEFAULT FALSE,
  status_flag BOOLEAN DEFAULT TRUE,
  remark VARCHAR(4000),
  priority INTEGER,
  config TEXT
);

CREATE TABLE IF NOT EXISTS public.ec_portal_homepage_role (
  id BIGINT PRIMARY KEY,
  role_id BIGINT,
  homepage_id BIGINT
);

CREATE TABLE IF NOT EXISTS public.ec_portal_user_config (
  id BIGINT PRIMARY KEY,
  user_id BIGINT,
  portal_homepage_id BIGINT,
  current_portal BOOLEAN DEFAULT FALSE,
  custom_flag BOOLEAN DEFAULT FALSE,
  config TEXT
);

INSERT INTO public.ec_portal_homepage (
  id, code, title_key, power_flag, status_flag, config
)
VALUES (
  1,
  'portal.homepage.default',
  'portal.homepage.default',
  FALSE,
  TRUE,
  '[{"companyIds":[-1],"code":"myProcess","id":"myProcess","powerFlag":false,"statusFlag":true,"x":0,"y":0,"title":"我的流程","titleKey":"portal.homepage.myProcess","url":"/supplant/#/myProcess?__t__=1616033155820"},{"companyIds":[-1],"code":"pendingNotice","id":"pendingNotice","powerFlag":false,"statusFlag":true,"x":4,"y":0,"title":"待办提醒","titleKey":"portal.homepage.pendingNotice","url":"/supplant/#/pendingNotice?__t__=1616033156792"}]'
)
ON CONFLICT (id) DO UPDATE
SET code = EXCLUDED.code,
    title_key = EXCLUDED.title_key,
    power_flag = EXCLUDED.power_flag,
    status_flag = EXCLUDED.status_flag,
    config = EXCLUDED.config;

CREATE TABLE IF NOT EXISTS public.auth_login_log (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  user_name VARCHAR(256) NOT NULL,
  ticket VARCHAR(50) NOT NULL,
  login_ip VARCHAR(256),
  device_type VARCHAR(32) NOT NULL,
  login_type VARCHAR(32) NOT NULL,
  logout_type VARCHAR(32),
  login_time TIMESTAMP,
  logout_time TIMESTAMP,
  cid BIGINT,
  company_name VARCHAR(256),
  creator VARCHAR(32) NOT NULL DEFAULT 'system',
  modifier VARCHAR(32),
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modify_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_staff_id BIGINT,
  modify_staff_id BIGINT
);

CREATE INDEX IF NOT EXISTS idx_login_log_username
  ON public.auth_login_log (user_name);

CREATE INDEX IF NOT EXISTS auth_login_log_ticket
  ON public.auth_login_log (ticket);
