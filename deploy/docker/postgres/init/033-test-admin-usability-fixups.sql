-- The recovered bootstrap creates admin as a first-login user. For the Docker
-- test profile we keep the original password but disable the mandatory password
-- change dialog so automated smoke tests can traverse the platform pages.
UPDATE public.auth_user
SET login_first = 0,
    modify_time = CURRENT_TIMESTAMP
WHERE user_name = 'admin'
  AND login_first IS DISTINCT FROM 0;
