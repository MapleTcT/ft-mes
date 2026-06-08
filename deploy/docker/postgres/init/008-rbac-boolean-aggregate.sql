-- Runtime compatibility for decompiled RBAC binaries.
--
-- Some compiled MyBatis-Plus query fragments still emit sum(boolean_column)
-- after boolean columns are converted for PostgreSQL. Source code has been
-- adjusted to use CASE expressions, but the Docker recovery path runs the
-- recovered Windows binaries, so provide a PostgreSQL aggregate shim.

CREATE OR REPLACE FUNCTION public.adp_boolean_sum_state(state integer, value boolean)
RETURNS integer
LANGUAGE sql
IMMUTABLE
AS $$
    SELECT COALESCE(state, 0) + CASE WHEN COALESCE(value, false) THEN 1 ELSE 0 END
$$;

DROP AGGREGATE IF EXISTS public.sum(boolean);

CREATE AGGREGATE public.sum(boolean) (
    SFUNC = public.adp_boolean_sum_state,
    STYPE = integer
);
