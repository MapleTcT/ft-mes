CREATE TABLE IF NOT EXISTS public.org_manager (
  id BIGINT PRIMARY KEY,
  row_version BIGINT DEFAULT 0,
  org_id BIGINT NOT NULL,
  manager_id BIGINT NOT NULL,
  manager_name VARCHAR(50) NOT NULL,
  manager_type VARCHAR(16) NOT NULL,
  creator VARCHAR(32) NOT NULL DEFAULT 'system',
  modifier VARCHAR(32) DEFAULT NULL,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modify_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_staff_id BIGINT DEFAULT NULL,
  modify_staff_id BIGINT DEFAULT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS udx_mgr_oid_mid_mtype
  ON public.org_manager(org_id, manager_id, manager_type);
CREATE INDEX IF NOT EXISTS idx_org_manager_org_id
  ON public.org_manager(org_id);
CREATE INDEX IF NOT EXISTS idx_org_manager_manager_id
  ON public.org_manager(manager_id);
