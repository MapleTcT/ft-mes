-- PostgreSQL compatibility for legacy Hibernate Criteria that compares text
-- columns with numeric IDs through LIKE.
--
-- Runtime evidence:
-- - WTS fireworkClose submit calls WorkTicketEvent.workTicketBeforeSave.
-- - WorkTicket2PermitService.getPermitByWorkTicketId looks up the parent permit
--   with Restrictions.like("payload", "%" + workTicketId + "%").
-- - The recovered runtime can still bind the right-hand LIKE parameter as
--   bigint, so PostgreSQL raises:
--     ERROR: operator does not exist: text ~~ bigint
--
-- The Java source intent is "payload contains the work ticket id", so keep this
-- as a narrow text/varchar LIKE bigint operator instead of widening WTS columns
-- or switching back to Oracle behavior.

CREATE OR REPLACE FUNCTION public.adp_text_like_bigint(left_value text, right_value bigint)
RETURNS boolean
LANGUAGE sql
IMMUTABLE
AS $function$
  SELECT CASE
    WHEN left_value IS NULL OR right_value IS NULL THEN NULL
    ELSE left_value LIKE ('%' || right_value::text || '%')
  END
$function$;

CREATE OR REPLACE FUNCTION public.adp_varchar_like_bigint(left_value character varying, right_value bigint)
RETURNS boolean
LANGUAGE sql
IMMUTABLE
AS $function$
  SELECT CASE
    WHEN left_value IS NULL OR right_value IS NULL THEN NULL
    ELSE left_value::text LIKE ('%' || right_value::text || '%')
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
      AND op.oprleft = 'text'::regtype
      AND op.oprright = 'bigint'::regtype
  ) THEN
    CREATE OPERATOR public.~~ (
      LEFTARG = text,
      RIGHTARG = bigint,
      PROCEDURE = public.adp_text_like_bigint
    );
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM pg_operator op
    JOIN pg_namespace ns ON ns.oid = op.oprnamespace
    WHERE ns.nspname = 'public'
      AND op.oprname = '~~'
      AND op.oprleft = 'character varying'::regtype
      AND op.oprright = 'bigint'::regtype
  ) THEN
    CREATE OPERATOR public.~~ (
      LEFTARG = character varying,
      RIGHTARG = bigint,
      PROCEDURE = public.adp_varchar_like_bigint
    );
  END IF;
END $$;
