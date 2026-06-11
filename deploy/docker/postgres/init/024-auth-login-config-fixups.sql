CREATE TABLE IF NOT EXISTS public.auth_login_config (
  id BIGINT PRIMARY KEY,
  pc_unique SMALLINT DEFAULT 0,
  mobile_unique SMALLINT DEFAULT 0,
  pc_expire INTEGER DEFAULT 1800,
  mobile_expire INTEGER DEFAULT 2592000,
  pc_timeunit VARCHAR(12),
  mobile_timeunit VARCHAR(12),
  creator VARCHAR(32) NOT NULL DEFAULT 'system',
  modifier VARCHAR(32),
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modify_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_staff_id BIGINT,
  modify_staff_id BIGINT,
  valid SMALLINT DEFAULT 1
);

INSERT INTO public.auth_login_config (
  id,
  pc_unique,
  mobile_unique,
  pc_expire,
  mobile_expire,
  valid,
  pc_timeunit,
  mobile_timeunit
)
VALUES (1, 0, 0, 30, 30, 1, 'DAYS', 'DAYS')
ON CONFLICT (id) DO UPDATE
SET pc_unique = EXCLUDED.pc_unique,
    mobile_unique = EXCLUDED.mobile_unique,
    pc_expire = EXCLUDED.pc_expire,
    mobile_expire = EXCLUDED.mobile_expire,
    valid = EXCLUDED.valid,
    pc_timeunit = EXCLUDED.pc_timeunit,
    mobile_timeunit = EXCLUDED.mobile_timeunit,
    modify_time = CURRENT_TIMESTAMP;
