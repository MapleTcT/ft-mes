-- PostgreSQL compatibility for the auth user lock/unlock workflow.
-- MyBatis Plus can include inherited error_count in UPDATE auth_user statements
-- when lock status is changed. Legacy ADP DDL variants do not always create it.

ALTER TABLE public.auth_user
  ADD COLUMN IF NOT EXISTS error_count INTEGER DEFAULT 0;

UPDATE public.auth_user
SET error_count = 0
WHERE error_count IS NULL;
