CREATE TABLE IF NOT EXISTS public.auth_user_role (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  creator VARCHAR(32) NOT NULL DEFAULT 'system',
  modifier VARCHAR(32),
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modify_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_staff_id BIGINT,
  modify_staff_id BIGINT,
  role_type SMALLINT DEFAULT 1,
  company_id BIGINT,
  role_code VARCHAR(256),
  role_name VARCHAR(256)
);

CREATE INDEX IF NOT EXISTS idx_user_role ON public.auth_user_role(user_id);
CREATE INDEX IF NOT EXISTS idx_auth_user_role_role_id ON public.auth_user_role(role_id);
CREATE UNIQUE INDEX IF NOT EXISTS udx_auth_user_role_user_role
  ON public.auth_user_role(user_id, role_id, COALESCE(company_id, 0));

INSERT INTO public.auth_user_role (
  id,
  user_id,
  role_id,
  creator,
  modifier,
  create_time,
  modify_time,
  create_staff_id,
  modify_staff_id,
  role_type,
  company_id,
  role_code,
  role_name
)
SELECT
  ru.id,
  ru.user_id,
  ru.role_id,
  COALESCE(ru.creator, 'system'),
  ru.modifier,
  COALESCE(ru.create_time, CURRENT_TIMESTAMP),
  COALESCE(ru.modify_time, CURRENT_TIMESTAMP),
  ru.create_staff_id,
  ru.modify_staff_id,
  CASE WHEN COALESCE(ru.from_position, 0) = 1 THEN 2 ELSE 1 END,
  NULL,
  r.code,
  r.name
FROM public.rbac_roleuser ru
LEFT JOIN public.rbac_role r ON r.id = ru.role_id
WHERE ru.user_id IS NOT NULL
  AND ru.role_id IS NOT NULL
ON CONFLICT (id) DO UPDATE
SET user_id = EXCLUDED.user_id,
    role_id = EXCLUDED.role_id,
    modifier = COALESCE(EXCLUDED.modifier, public.auth_user_role.modifier),
    modify_time = CURRENT_TIMESTAMP,
    modify_staff_id = COALESCE(EXCLUDED.modify_staff_id, public.auth_user_role.modify_staff_id),
    role_type = COALESCE(EXCLUDED.role_type, public.auth_user_role.role_type),
    role_code = COALESCE(EXCLUDED.role_code, public.auth_user_role.role_code),
    role_name = COALESCE(EXCLUDED.role_name, public.auth_user_role.role_name);
