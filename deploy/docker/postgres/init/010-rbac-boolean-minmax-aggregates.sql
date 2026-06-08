-- Runtime compatibility for recovered query providers that aggregate Boolean
-- permission flags with max()/min(). Rebuilt source uses CASE + bool_or(), but
-- the current Docker recovery path runs the decompiled Windows binaries.
--
-- Several @Results mappings read these aggregate columns as Integer, so these
-- shims return 0/1 integers rather than booleans.

CREATE OR REPLACE FUNCTION public.adp_boolean_max_state(state integer, value boolean)
RETURNS integer
LANGUAGE sql
IMMUTABLE
AS $$
    SELECT CASE
        WHEN state IS NULL AND value IS NULL THEN NULL
        WHEN state IS NULL THEN CASE WHEN value THEN 1 ELSE 0 END
        WHEN value IS NULL THEN state
        ELSE GREATEST(state, CASE WHEN value THEN 1 ELSE 0 END)
    END
$$;

CREATE OR REPLACE FUNCTION public.adp_boolean_min_state(state integer, value boolean)
RETURNS integer
LANGUAGE sql
IMMUTABLE
AS $$
    SELECT CASE
        WHEN state IS NULL AND value IS NULL THEN NULL
        WHEN state IS NULL THEN CASE WHEN value THEN 1 ELSE 0 END
        WHEN value IS NULL THEN state
        ELSE LEAST(state, CASE WHEN value THEN 1 ELSE 0 END)
    END
$$;

DROP AGGREGATE IF EXISTS public.max(boolean);
DROP AGGREGATE IF EXISTS public.min(boolean);

CREATE AGGREGATE public.max(boolean) (
    SFUNC = public.adp_boolean_max_state,
    STYPE = integer
);

CREATE AGGREGATE public.min(boolean) (
    SFUNC = public.adp_boolean_min_state,
    STYPE = integer
);
