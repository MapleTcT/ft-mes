-- PostgreSQL operator-resolution guard for legacy varchar LIKE string patterns.
--
-- Runtime evidence:
-- - WOM checkoutBill/generate calls PublicUtils.judgeModule.
-- - The query EC_MODULE.code LIKE 'QCS_%' should be a string comparison.
-- - After adding varchar LIKE bigint compatibility for other legacy paths,
--   PostgreSQL can prefer casting the unknown literal 'QCS_%' to bigint unless
--   an exact varchar LIKE text operator is available in the search path.

CREATE OR REPLACE FUNCTION public.adp_varchar_like_text(left_value character varying, right_value text)
RETURNS boolean
LANGUAGE sql
IMMUTABLE
AS $function$
  SELECT CASE
    WHEN left_value IS NULL OR right_value IS NULL THEN NULL
    ELSE left_value::text LIKE right_value
  END
$function$;

CREATE OR REPLACE FUNCTION public.adp_varchar_not_like_text(left_value character varying, right_value text)
RETURNS boolean
LANGUAGE sql
IMMUTABLE
AS $function$
  SELECT CASE
    WHEN left_value IS NULL OR right_value IS NULL THEN NULL
    ELSE left_value::text NOT LIKE right_value
  END
$function$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_operator op
    JOIN pg_namespace ns ON ns.oid = op.oprnamespace
    WHERE ns.nspname = 'public'
      AND op.oprname = '~~'
      AND op.oprleft = 'character varying'::regtype
      AND op.oprright = 'text'::regtype
  ) THEN
    CREATE OPERATOR public.~~ (
      LEFTARG = character varying,
      RIGHTARG = text,
      PROCEDURE = public.adp_varchar_like_text
    );
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM pg_operator op
    JOIN pg_namespace ns ON ns.oid = op.oprnamespace
    WHERE ns.nspname = 'public'
      AND op.oprname = '!~~'
      AND op.oprleft = 'character varying'::regtype
      AND op.oprright = 'text'::regtype
  ) THEN
    CREATE OPERATOR public.!~~ (
      LEFTARG = character varying,
      RIGHTARG = text,
      PROCEDURE = public.adp_varchar_not_like_text
    );
  END IF;
END $$;
