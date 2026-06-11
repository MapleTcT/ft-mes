-- Recovered menu rows can be left with valid/enable=false after the
-- PostgreSQL boolean conversion, which makes child menus lose their parents
-- during current-user menu tree construction.
UPDATE public.rbac_menuinfo
SET valid = TRUE,
    enable = TRUE,
    modify_time = CURRENT_TIMESTAMP
WHERE delete_time IS NULL
  AND (valid IS DISTINCT FROM TRUE OR enable IS DISTINCT FROM TRUE);

