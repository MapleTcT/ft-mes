-- JBPM maps ScopeInstanceImpl.hasVariables as a Java boolean. The recovered
-- MySQL-style bigint(1) column rejects PostgreSQL boolean bind values during
-- process-instance creation.

DO $$
DECLARE
  current_type text;
BEGIN
  FOR current_type IN
    SELECT column_name
      FROM information_schema.columns
     WHERE table_schema = 'public'
       AND table_name = 'jbpm4_execution'
       AND column_name IN ('hasvars_', 'group_enabled_', 'cross_company_flag_')
       AND data_type <> 'boolean'
  LOOP
    EXECUTE format(
      'ALTER TABLE public.jbpm4_execution
         ALTER COLUMN %1$I DROP DEFAULT,
         ALTER COLUMN %1$I TYPE boolean
           USING CASE
             WHEN %1$I IS NULL THEN NULL
             WHEN lower(%1$I::text) IN (''1'', ''t'', ''true'', ''y'', ''yes'') THEN true
             ELSE false
           END',
      current_type
    );
  END LOOP;

  SELECT data_type
    INTO current_type
    FROM information_schema.columns
   WHERE table_schema = 'public'
     AND table_name = 'jbpm4_task'
     AND column_name = 'hasvars_';

  IF current_type IS NOT NULL AND current_type <> 'boolean' THEN
    ALTER TABLE public.jbpm4_task
      ALTER COLUMN hasvars_ DROP DEFAULT,
      ALTER COLUMN hasvars_ TYPE boolean
        USING CASE
          WHEN hasvars_ IS NULL THEN NULL
          WHEN lower(hasvars_::text) IN ('1', 't', 'true', 'y', 'yes') THEN true
          ELSE false
        END;
  END IF;
END $$;
