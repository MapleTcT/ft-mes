-- PostgreSQL compatibility for legacy module attachment controllers.
--
-- Several recovered module FileDownloadController implementations call
-- ViewServiceFoundation.findShadowCode(viewCode), then parse IS_SHADOW with
-- Integer.parseInt(...). The Oracle-era runtime tables store IS_SHADOW as
-- 0/1, while the PostgreSQL import converted runtime_view.is_shadow to boolean.
-- That makes attachment list requests with viewCode fail with:
--   NumberFormatException: For input string: "false"
--
-- Keep this scoped to runtime_view.is_shadow. ec_view.is_shadow is already an
-- integer in the recovered PostgreSQL schema, and the platform cache reader
-- accepts both Boolean and Number values for this field.

DO $$
DECLARE
    column_type text;
BEGIN
    SELECT data_type
      INTO column_type
      FROM information_schema.columns
     WHERE table_schema = 'public'
       AND table_name = 'runtime_view'
       AND column_name = 'is_shadow';

    IF column_type = 'boolean' THEN
        ALTER TABLE public.runtime_view
            ALTER COLUMN is_shadow TYPE integer
            USING CASE WHEN is_shadow THEN 1 ELSE 0 END;
    END IF;
END $$;
