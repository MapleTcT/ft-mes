-- PostgreSQL compatibility for legacy native SQL that applies LIKE patterns to
-- numeric ID columns.
--
-- Runtime evidence:
-- - WOM checkoutBill/generate calls PublicUtils.judgeModule.
-- - The recovered legacy SQL compares a bigint column with the text pattern
--   'QCS_%', which PostgreSQL otherwise attempts to cast to bigint:
--     ERROR: invalid input syntax for type bigint: "QCS_%"
--
-- Keep this as a narrow bigint LIKE text/varchar compatibility operator so the
-- comparison evaluates as text matching without widening business table columns.

CREATE OR REPLACE FUNCTION public.adp_bigint_like_text(left_value bigint, right_value text)
RETURNS boolean
LANGUAGE sql
IMMUTABLE
AS $function$
  SELECT CASE
    WHEN left_value IS NULL OR right_value IS NULL THEN NULL
    ELSE left_value::text LIKE right_value
  END
$function$;

CREATE OR REPLACE FUNCTION public.adp_bigint_not_like_text(left_value bigint, right_value text)
RETURNS boolean
LANGUAGE sql
IMMUTABLE
AS $function$
  SELECT CASE
    WHEN left_value IS NULL OR right_value IS NULL THEN NULL
    ELSE left_value::text NOT LIKE right_value
  END
$function$;

CREATE OR REPLACE FUNCTION public.adp_bigint_like_varchar(left_value bigint, right_value character varying)
RETURNS boolean
LANGUAGE sql
IMMUTABLE
AS $function$
  SELECT public.adp_bigint_like_text(left_value, right_value::text)
$function$;

CREATE OR REPLACE FUNCTION public.adp_bigint_not_like_varchar(left_value bigint, right_value character varying)
RETURNS boolean
LANGUAGE sql
IMMUTABLE
AS $function$
  SELECT public.adp_bigint_not_like_text(left_value, right_value::text)
$function$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_operator op
    JOIN pg_namespace ns ON ns.oid = op.oprnamespace
    WHERE ns.nspname = 'public'
      AND op.oprname = '~~'
      AND op.oprleft = 'bigint'::regtype
      AND op.oprright = 'text'::regtype
  ) THEN
    CREATE OPERATOR public.~~ (
      LEFTARG = bigint,
      RIGHTARG = text,
      PROCEDURE = public.adp_bigint_like_text
    );
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM pg_operator op
    JOIN pg_namespace ns ON ns.oid = op.oprnamespace
    WHERE ns.nspname = 'public'
      AND op.oprname = '!~~'
      AND op.oprleft = 'bigint'::regtype
      AND op.oprright = 'text'::regtype
  ) THEN
    CREATE OPERATOR public.!~~ (
      LEFTARG = bigint,
      RIGHTARG = text,
      PROCEDURE = public.adp_bigint_not_like_text
    );
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM pg_operator op
    JOIN pg_namespace ns ON ns.oid = op.oprnamespace
    WHERE ns.nspname = 'public'
      AND op.oprname = '~~'
      AND op.oprleft = 'bigint'::regtype
      AND op.oprright = 'character varying'::regtype
  ) THEN
    CREATE OPERATOR public.~~ (
      LEFTARG = bigint,
      RIGHTARG = character varying,
      PROCEDURE = public.adp_bigint_like_varchar
    );
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM pg_operator op
    JOIN pg_namespace ns ON ns.oid = op.oprnamespace
    WHERE ns.nspname = 'public'
      AND op.oprname = '!~~'
      AND op.oprleft = 'bigint'::regtype
      AND op.oprright = 'character varying'::regtype
  ) THEN
    CREATE OPERATOR public.!~~ (
      LEFTARG = bigint,
      RIGHTARG = character varying,
      PROCEDURE = public.adp_bigint_not_like_varchar
    );
  END IF;
END $$;
