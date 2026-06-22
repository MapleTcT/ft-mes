-- PostgreSQL compatibility for legacy Hibernate native SQL that binds text
-- parameters as bytea.
--
-- Runtime evidence:
-- - QCSInspectReportServiceImpl.getResultOption executes native SQL:
--   stdGrade.NAME = ?
-- - The recovered PostgreSQL runtime stores LIMSBA_STD_VER_GRADES.NAME as
--   varchar, while the legacy Hibernate path binds the right-hand parameter as
--   bytea. PostgreSQL then raises:
--   operator does not exist: character varying = bytea
--
-- Keep this as a narrow comparison operator instead of changing business
-- columns to bytea/oid. It decodes the right-hand bytea as UTF-8 and compares
-- with the text value.

CREATE OR REPLACE FUNCTION public.adp_text_eq_bytea(left_value text, right_value bytea)
RETURNS boolean
LANGUAGE plpgsql
IMMUTABLE
AS $function$
BEGIN
  IF left_value IS NULL OR right_value IS NULL THEN
    RETURN NULL;
  END IF;

  BEGIN
    RETURN left_value = convert_from(right_value, 'UTF8');
  EXCEPTION WHEN others THEN
    RETURN false;
  END;
END;
$function$;

CREATE OR REPLACE FUNCTION public.adp_varchar_eq_bytea(left_value character varying, right_value bytea)
RETURNS boolean
LANGUAGE plpgsql
IMMUTABLE
AS $function$
BEGIN
  IF left_value IS NULL OR right_value IS NULL THEN
    RETURN NULL;
  END IF;

  BEGIN
    RETURN left_value::text = convert_from(right_value, 'UTF8');
  EXCEPTION WHEN others THEN
    RETURN false;
  END;
END;
$function$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_operator op
    JOIN pg_namespace ns ON ns.oid = op.oprnamespace
    WHERE ns.nspname = 'public'
      AND op.oprname = '='
      AND op.oprleft = 'text'::regtype
      AND op.oprright = 'bytea'::regtype
  ) THEN
    CREATE OPERATOR public.= (
      LEFTARG = text,
      RIGHTARG = bytea,
      PROCEDURE = public.adp_text_eq_bytea
    );
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM pg_operator op
    JOIN pg_namespace ns ON ns.oid = op.oprnamespace
    WHERE ns.nspname = 'public'
      AND op.oprname = '='
      AND op.oprleft = 'character varying'::regtype
      AND op.oprright = 'bytea'::regtype
  ) THEN
    CREATE OPERATOR public.= (
      LEFTARG = character varying,
      RIGHTARG = bytea,
      PROCEDURE = public.adp_varchar_eq_bytea
    );
  END IF;
END $$;
