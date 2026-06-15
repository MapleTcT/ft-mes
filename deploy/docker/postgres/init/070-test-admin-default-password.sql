-- Keep the Docker/PostgreSQL test profile aligned with the documented smoke
-- account: admin / 123456. This patch is intentionally test-profile scoped and
-- must not be reused as a production credential strategy.

CREATE TABLE IF NOT EXISTS public.codex_auth_user_password_backup (
  backup_key text PRIMARY KEY,
  backup_time timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  source_table text NOT NULL,
  id bigint NOT NULL,
  user_name text NOT NULL,
  old_password text NOT NULL
);

INSERT INTO public.codex_auth_user_password_backup (
  backup_key,
  source_table,
  id,
  user_name,
  old_password
)
SELECT
  'restore-admin-default-password-20260615',
  'auth_user',
  id,
  user_name,
  password
FROM public.auth_user
WHERE user_name = 'admin'
ON CONFLICT (backup_key) DO NOTHING;

UPDATE public.auth_user
SET password = '$2a$10$QEd181jr.RNYME6hz/.xpONiMe3uGkI5sI8fjH5DQWwgwBKEs0/Cy',
    valid = 1,
    has_lock = 0,
    lock_reason = NULL,
    lock_time = NULL,
    login_first = 0,
    modify_time = CURRENT_TIMESTAMP
WHERE user_name = 'admin'
  AND (
    password IS DISTINCT FROM '$2a$10$QEd181jr.RNYME6hz/.xpONiMe3uGkI5sI8fjH5DQWwgwBKEs0/Cy'
    OR valid IS DISTINCT FROM 1
    OR has_lock IS DISTINCT FROM 0
    OR lock_reason IS NOT NULL
    OR lock_time IS NOT NULL
    OR login_first IS DISTINCT FROM 0
  );
