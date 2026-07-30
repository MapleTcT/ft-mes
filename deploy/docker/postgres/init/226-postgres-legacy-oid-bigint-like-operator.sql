-- PostgreSQL compatibility for legacy Hibernate Criteria that applies LIKE to
-- an @Lob String mapped as a PostgreSQL large-object OID.
--
-- WTS WorkTicket2PermitService searches wts_work_permits.payload for a work
-- ticket ID. With the recovered WTS package, Hibernate binds the ticket ID as
-- bigint while payload is oid, producing:
--   ERROR: operator does not exist: oid ~~ bigint
--
-- Keep the compatibility narrow: read the referenced large object as UTF-8
-- text and compare only the exact oid/bigint operator pair used by WTS.

CREATE OR REPLACE FUNCTION public.adp_oid_like_bigint(left_value oid, right_value bigint)
RETURNS boolean
LANGUAGE sql
AS $function$
  SELECT CASE
    WHEN left_value IS NULL OR right_value IS NULL THEN NULL
    ELSE convert_from(lo_get(left_value), 'UTF8') LIKE ('%' || right_value::text || '%')
  END
$function$;

CREATE OR REPLACE FUNCTION public.adp_oid_not_like_bigint(left_value oid, right_value bigint)
RETURNS boolean
LANGUAGE sql
AS $function$
  SELECT CASE
    WHEN left_value IS NULL OR right_value IS NULL THEN NULL
    ELSE convert_from(lo_get(left_value), 'UTF8') NOT LIKE ('%' || right_value::text || '%')
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
      AND op.oprleft = 'oid'::regtype
      AND op.oprright = 'bigint'::regtype
  ) THEN
    CREATE OPERATOR public.~~ (
      LEFTARG = oid,
      RIGHTARG = bigint,
      PROCEDURE = public.adp_oid_like_bigint
    );
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM pg_operator op
    JOIN pg_namespace ns ON ns.oid = op.oprnamespace
    WHERE ns.nspname = 'public'
      AND op.oprname = '!~~'
      AND op.oprleft = 'oid'::regtype
      AND op.oprright = 'bigint'::regtype
  ) THEN
    CREATE OPERATOR public.!~~ (
      LEFTARG = oid,
      RIGHTARG = bigint,
      PROCEDURE = public.adp_oid_not_like_bigint
    );
  END IF;
END $$;
