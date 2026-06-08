DROP OPERATOR IF EXISTS public.= (varchar, smallint);
DROP OPERATOR IF EXISTS public.= (varchar, integer);
DROP OPERATOR IF EXISTS public.= (varchar, bigint);
DROP FUNCTION IF EXISTS public.varchar_eq_smallint(varchar, smallint);
DROP FUNCTION IF EXISTS public.varchar_eq_integer(varchar, integer);
DROP FUNCTION IF EXISTS public.varchar_eq_bigint(varchar, bigint);

CREATE OR REPLACE FUNCTION public.smallint_eq_varchar(left_value smallint, right_value varchar) RETURNS boolean
LANGUAGE sql IMMUTABLE STRICT AS 'select left_value = right_value::smallint';

CREATE OR REPLACE FUNCTION public.integer_eq_varchar(left_value integer, right_value varchar) RETURNS boolean
LANGUAGE sql IMMUTABLE STRICT AS 'select left_value = right_value::integer';

CREATE OR REPLACE FUNCTION public.bigint_eq_varchar(left_value bigint, right_value varchar) RETURNS boolean
LANGUAGE sql IMMUTABLE STRICT AS 'select left_value = right_value::bigint';

CREATE OR REPLACE FUNCTION public.varchar_to_smallint(value varchar) RETURNS smallint
LANGUAGE plpgsql IMMUTABLE STRICT AS $$
BEGIN
  IF btrim(value) = '' THEN
    RETURN 0;
  END IF;
  IF lower(value) IN ('true', 't', 'yes', 'y') THEN
    RETURN 1;
  END IF;
  IF lower(value) IN ('false', 'f', 'no', 'n') THEN
    RETURN 0;
  END IF;
  RETURN value::smallint;
EXCEPTION WHEN others THEN
  RETURN 0;
END;
$$;

CREATE OR REPLACE FUNCTION public.varchar_to_integer(value varchar) RETURNS integer
LANGUAGE plpgsql IMMUTABLE STRICT AS $$
BEGIN
  IF btrim(value) = '' THEN
    RETURN 0;
  END IF;
  IF lower(value) IN ('true', 't', 'yes', 'y') THEN
    RETURN 1;
  END IF;
  IF lower(value) IN ('false', 'f', 'no', 'n') THEN
    RETURN 0;
  END IF;
  RETURN value::integer;
EXCEPTION WHEN others THEN
  RETURN 0;
END;
$$;

CREATE OR REPLACE FUNCTION public.varchar_to_bigint(value varchar) RETURNS bigint
LANGUAGE plpgsql IMMUTABLE STRICT AS $$
BEGIN
  IF btrim(value) = '' THEN
    RETURN 0;
  END IF;
  IF lower(value) IN ('true', 't', 'yes', 'y') THEN
    RETURN 1;
  END IF;
  IF lower(value) IN ('false', 'f', 'no', 'n') THEN
    RETURN 0;
  END IF;
  RETURN value::bigint;
EXCEPTION WHEN others THEN
  RETURN 0;
END;
$$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_operator WHERE oprnamespace='public'::regnamespace AND oprname='=' AND oprleft='smallint'::regtype AND oprright='character varying'::regtype) THEN
    CREATE OPERATOR public.= (LEFTARG = smallint, RIGHTARG = varchar, PROCEDURE = public.smallint_eq_varchar);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_operator WHERE oprnamespace='public'::regnamespace AND oprname='=' AND oprleft='integer'::regtype AND oprright='character varying'::regtype) THEN
    CREATE OPERATOR public.= (LEFTARG = integer, RIGHTARG = varchar, PROCEDURE = public.integer_eq_varchar);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_operator WHERE oprnamespace='public'::regnamespace AND oprname='=' AND oprleft='bigint'::regtype AND oprright='character varying'::regtype) THEN
    CREATE OPERATOR public.= (LEFTARG = bigint, RIGHTARG = varchar, PROCEDURE = public.bigint_eq_varchar);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_cast WHERE castsource='character varying'::regtype AND casttarget='smallint'::regtype) THEN
    CREATE CAST (varchar AS smallint) WITH FUNCTION public.varchar_to_smallint(varchar) AS ASSIGNMENT;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_cast WHERE castsource='character varying'::regtype AND casttarget='integer'::regtype) THEN
    CREATE CAST (varchar AS integer) WITH FUNCTION public.varchar_to_integer(varchar) AS ASSIGNMENT;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_cast WHERE castsource='character varying'::regtype AND casttarget='bigint'::regtype) THEN
    CREATE CAST (varchar AS bigint) WITH FUNCTION public.varchar_to_bigint(varchar) AS ASSIGNMENT;
  END IF;
END $$;

CREATE TABLE IF NOT EXISTS public.org_company_mnecode (
  id bigint NOT NULL,
  row_version bigint DEFAULT 0 NOT NULL,
  language character varying(510),
  mne_code character varying(510),
  company_id bigint,
  company_short_name character varying(510),
  creator character varying(32) DEFAULT NULL::character varying,
  modifier character varying(32) DEFAULT NULL::character varying,
  create_time timestamp without time zone,
  modify_time timestamp without time zone,
  create_staff_id bigint,
  modify_staff_id bigint,
  tenant_id character varying(64) DEFAULT NULL::character varying,
  CONSTRAINT org_company_mnecode_pkey PRIMARY KEY (id)
);
