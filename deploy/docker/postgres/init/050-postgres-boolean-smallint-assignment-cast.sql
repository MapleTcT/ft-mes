-- Some recovered ADP modules keep MySQL-style SMALLINT flag columns because
-- native SQL compares them with 0/1, while Hibernate entities still bind Java
-- Boolean values. Allow Boolean parameters to be assigned into SMALLINT flag
-- columns without changing query semantics.
CREATE OR REPLACE FUNCTION public.adp_boolean_to_smallint(value boolean)
RETURNS smallint
LANGUAGE sql
IMMUTABLE
STRICT
AS $$
  SELECT CASE WHEN value THEN 1::smallint ELSE 0::smallint END
$$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_cast
    WHERE castsource = 'boolean'::regtype
      AND casttarget = 'smallint'::regtype
  ) THEN
    CREATE CAST (boolean AS smallint)
      WITH FUNCTION public.adp_boolean_to_smallint(boolean)
      AS ASSIGNMENT;
  END IF;
END $$;
