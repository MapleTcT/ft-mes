CREATE TABLE IF NOT EXISTS public.auth_user_directory (
  id BIGINT PRIMARY KEY,
  directory_name VARCHAR(256) NOT NULL,
  directory_type VARCHAR(256),
  sort DOUBLE PRECISION,
  enabled BOOLEAN DEFAULT FALSE,
  valid BOOLEAN DEFAULT TRUE,
  description VARCHAR(512),
  hostname VARCHAR(256),
  port INTEGER,
  enable_ssl BOOLEAN DEFAULT FALSE,
  user_name VARCHAR(256),
  password VARCHAR(256),
  base_dn VARCHAR(512),
  attach_user_dn VARCHAR(512),
  attach_group_dn VARCHAR(512),
  permission VARCHAR(256),
  default_roles VARCHAR(512),
  company_id BIGINT NOT NULL,
  sync_first BOOLEAN DEFAULT FALSE,
  creator VARCHAR(32) NOT NULL DEFAULT 'system',
  modifier VARCHAR(32),
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modify_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_staff_id BIGINT,
  modify_staff_id BIGINT
);

CREATE INDEX IF NOT EXISTS idx_user_directory_name
  ON public.auth_user_directory (directory_name);

CREATE INDEX IF NOT EXISTS idx_user_directory_enabled
  ON public.auth_user_directory (enabled);
