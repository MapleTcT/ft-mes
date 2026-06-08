-- Runtime compatibility for legacy MyBatis-Plus predicates such as:
--   WHERE boolean_column = 0
-- The recovered binaries still contain those predicates even though the
-- corresponding PO fields are Boolean and PostgreSQL columns must be BOOLEAN.

CREATE OR REPLACE FUNCTION public.adp_boolean_eq_integer(left_value boolean, right_value integer)
RETURNS boolean
LANGUAGE sql
IMMUTABLE
AS $$
    SELECT CASE
        WHEN left_value IS NULL OR right_value IS NULL THEN NULL
        ELSE left_value = (right_value <> 0)
    END
$$;

CREATE OR REPLACE FUNCTION public.adp_integer_eq_boolean(left_value integer, right_value boolean)
RETURNS boolean
LANGUAGE sql
IMMUTABLE
AS $$
    SELECT CASE
        WHEN left_value IS NULL OR right_value IS NULL THEN NULL
        ELSE (left_value <> 0) = right_value
    END
$$;

CREATE OR REPLACE FUNCTION public.adp_boolean_ne_integer(left_value boolean, right_value integer)
RETURNS boolean
LANGUAGE sql
IMMUTABLE
AS $$
    SELECT CASE
        WHEN left_value IS NULL OR right_value IS NULL THEN NULL
        ELSE left_value <> (right_value <> 0)
    END
$$;

CREATE OR REPLACE FUNCTION public.adp_integer_ne_boolean(left_value integer, right_value boolean)
RETURNS boolean
LANGUAGE sql
IMMUTABLE
AS $$
    SELECT CASE
        WHEN left_value IS NULL OR right_value IS NULL THEN NULL
        ELSE (left_value <> 0) <> right_value
    END
$$;

DROP OPERATOR IF EXISTS public.= (boolean, integer);
DROP OPERATOR IF EXISTS public.= (integer, boolean);
DROP OPERATOR IF EXISTS public.<> (boolean, integer);
DROP OPERATOR IF EXISTS public.<> (integer, boolean);

CREATE OPERATOR public.= (
    LEFTARG = boolean,
    RIGHTARG = integer,
    FUNCTION = public.adp_boolean_eq_integer
);

CREATE OPERATOR public.= (
    LEFTARG = integer,
    RIGHTARG = boolean,
    FUNCTION = public.adp_integer_eq_boolean
);

CREATE OPERATOR public.<> (
    LEFTARG = boolean,
    RIGHTARG = integer,
    FUNCTION = public.adp_boolean_ne_integer
);

CREATE OPERATOR public.<> (
    LEFTARG = integer,
    RIGHTARG = boolean,
    FUNCTION = public.adp_integer_ne_boolean
);
