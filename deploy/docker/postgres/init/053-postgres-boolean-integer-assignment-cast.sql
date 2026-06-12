-- Hibernate entities in the recovered ADP stack often bind Java Boolean values
-- for MySQL-style INT flag columns. PostgreSQL has an explicit boolean->integer
-- cast already; make it usable for assignment into INT columns as 1/0.
CREATE OR REPLACE FUNCTION public.adp_boolean_to_integer(value boolean)
RETURNS integer
LANGUAGE sql
IMMUTABLE
STRICT
AS $$
  SELECT CASE WHEN value THEN 1 ELSE 0 END
$$;

DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM pg_cast
    WHERE castsource = 'boolean'::regtype
      AND casttarget = 'integer'::regtype
  ) THEN
    UPDATE pg_cast
    SET castcontext = 'a'
    WHERE castsource = 'boolean'::regtype
      AND casttarget = 'integer'::regtype
      AND castcontext <> 'a';
  ELSE
    CREATE CAST (boolean AS integer)
      WITH FUNCTION public.adp_boolean_to_integer(boolean)
      AS ASSIGNMENT;
  END IF;
END $$;
