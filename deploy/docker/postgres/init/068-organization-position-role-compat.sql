CREATE TABLE IF NOT EXISTS public.org_position_role (
  id BIGINT PRIMARY KEY,
  position_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  creator VARCHAR(32) NOT NULL DEFAULT 'system',
  modifier VARCHAR(32) DEFAULT NULL,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modify_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_staff_id BIGINT DEFAULT NULL,
  modify_staff_id BIGINT DEFAULT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS udx_pos_role_ids
  ON public.org_position_role(position_id, role_id);
CREATE INDEX IF NOT EXISTS idx_org_position_role_position_id
  ON public.org_position_role(position_id);
CREATE INDEX IF NOT EXISTS idx_org_position_role_role_id
  ON public.org_position_role(role_id);

CREATE OR REPLACE VIEW public.base_roleposition (
  id,
  version,
  position_id,
  role_id,
  valid
) AS
SELECT
  id,
  0 AS version,
  position_id,
  role_id,
  1 AS valid
FROM public.org_position_role;
