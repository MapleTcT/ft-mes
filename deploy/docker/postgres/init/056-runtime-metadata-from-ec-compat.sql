-- Runtime metadata compatibility for standalone MES module services.
--
-- Module package import stores design/runtime metadata in ec_* tables, while
-- standalone module services use BAPNamingStrategy("RUNTIME") and query the
-- matching runtime_* tables. PostgreSQL also needs Hibernate 5 @Lob String
-- columns to be represented as large object OIDs.

DO $$
DECLARE
  v_schema text := current_schema();
  pair record;
  col record;
  cols text;
  vals text;
  expr text;
  inserted_count bigint;
BEGIN
  FOR pair IN
    SELECT e.table_name AS source_table,
           replace(e.table_name, 'ec_', 'runtime_') AS target_table
    FROM information_schema.tables e
    JOIN information_schema.tables r
      ON r.table_schema = e.table_schema
     AND r.table_name = replace(e.table_name, 'ec_', 'runtime_')
    WHERE e.table_schema = v_schema
      AND e.table_type = 'BASE TABLE'
      AND e.table_name LIKE 'ec\_%' ESCAPE '\'
    ORDER BY e.table_name
  LOOP
    IF NOT EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = v_schema
        AND table_name = pair.source_table
        AND column_name = 'code'
    ) OR NOT EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = v_schema
        AND table_name = pair.target_table
        AND column_name = 'code'
    ) THEN
      CONTINUE;
    END IF;

    cols := NULL;
    vals := NULL;

    FOR col IN
      SELECT tc.column_name,
             tc.udt_name AS target_udt,
             sc.udt_name AS source_udt
      FROM information_schema.columns tc
      JOIN information_schema.columns sc
        ON sc.table_schema = tc.table_schema
       AND sc.table_name = pair.source_table
       AND sc.column_name = tc.column_name
      WHERE tc.table_schema = v_schema
        AND tc.table_name = pair.target_table
      ORDER BY tc.ordinal_position
    LOOP
      IF col.target_udt = 'bool' THEN
        expr := format(
          'CASE WHEN s.%1$I IS NULL THEN NULL WHEN lower(s.%1$I::text) IN (''1'', ''t'', ''true'', ''y'', ''yes'') THEN true WHEN lower(s.%1$I::text) IN (''0'', ''f'', ''false'', ''n'', ''no'') THEN false ELSE NULL END',
          col.column_name
        );
      ELSIF col.target_udt = 'oid' AND col.source_udt <> 'oid' THEN
        expr := format(
          'CASE WHEN s.%1$I IS NULL THEN NULL ELSE lo_from_bytea(0, convert_to(s.%1$I::text, ''UTF8'')) END',
          col.column_name
        );
      ELSE
        expr := format('s.%I', col.column_name);
      END IF;

      cols := concat_ws(', ', cols, format('%I', col.column_name));
      vals := concat_ws(', ', vals, expr);
    END LOOP;

    IF cols IS NULL THEN
      CONTINUE;
    END IF;

    EXECUTE format(
      'INSERT INTO %1$I.%2$I (%3$s)
       SELECT %4$s
       FROM (SELECT DISTINCT ON (code) * FROM %1$I.%5$I WHERE code IS NOT NULL ORDER BY code) s
       WHERE NOT EXISTS (SELECT 1 FROM %1$I.%2$I t WHERE t.code = s.code)',
      v_schema,
      pair.target_table,
      cols,
      vals,
      pair.source_table
    );
    GET DIAGNOSTICS inserted_count = ROW_COUNT;
    RAISE NOTICE 'copied % rows from % to %', inserted_count, pair.source_table, pair.target_table;
  END LOOP;
END $$;

ALTER TABLE IF EXISTS public.base_cp_model_mapping
  ADD COLUMN IF NOT EXISTS precision1 integer;

UPDATE public.base_cp_model_mapping
SET precision1 = precision
WHERE precision1 IS NULL
  AND precision IS NOT NULL;

ALTER TABLE IF EXISTS public.base_cp_view_mapping
  ADD COLUMN IF NOT EXISTS precision1 integer;

UPDATE public.base_cp_view_mapping
SET precision1 = precision
WHERE precision1 IS NULL
  AND precision IS NOT NULL;

CREATE OR REPLACE FUNCTION public.adp_convert_text_lob_column_to_oid(p_table text, p_column text)
RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
  current_udt text;
BEGIN
  SELECT udt_name
  INTO current_udt
  FROM information_schema.columns
  WHERE table_schema = current_schema()
    AND table_name = p_table
    AND column_name = p_column;

  IF current_udt IS NULL OR current_udt = 'oid' THEN
    RETURN;
  END IF;

  EXECUTE format(
    'ALTER TABLE %1$I.%2$I ALTER COLUMN %3$I TYPE oid USING CASE WHEN %3$I IS NULL THEN NULL ELSE lo_from_bytea(0, convert_to(%3$I::text, ''UTF8'')) END',
    current_schema(),
    p_table,
    p_column
  );
END $$;

SELECT public.adp_convert_text_lob_column_to_oid('runtime_property', 'fillcontent');
SELECT public.adp_convert_text_lob_column_to_oid('runtime_property', 'attributes');

SELECT public.adp_convert_text_lob_column_to_oid('runtime_field', 'config');
SELECT public.adp_convert_text_lob_column_to_oid('runtime_extra_view', 'config');
SELECT public.adp_convert_text_lob_column_to_oid('runtime_extra_view', 'full_config');
SELECT public.adp_convert_text_lob_column_to_oid('runtime_extra_view', 'view_json');
SELECT public.adp_convert_text_lob_column_to_oid('runtime_data_grid', 'config');
SELECT public.adp_convert_text_lob_column_to_oid('runtime_data_grid', 'full_config');
SELECT public.adp_convert_text_lob_column_to_oid('runtime_data_grid', 'data_grid_json');
SELECT public.adp_convert_text_lob_column_to_oid('runtime_adv_query_json', 'query_config');
SELECT public.adp_convert_text_lob_column_to_oid('runtime_fast_query_json', 'query_config');
SELECT public.adp_convert_text_lob_column_to_oid('runtime_extra_query_json', 'query_config');
SELECT public.adp_convert_text_lob_column_to_oid('runtime_backup_view', 'config');
SELECT public.adp_convert_text_lob_column_to_oid('runtime_backup_view', 'field_config');
SELECT public.adp_convert_text_lob_column_to_oid('runtime_backup_data_grid', 'config');
SELECT public.adp_convert_text_lob_column_to_oid('runtime_backup_data_grid', 'dg_field_config');
SELECT public.adp_convert_text_lob_column_to_oid('runtime_button', 'config');
SELECT public.adp_convert_text_lob_column_to_oid('runtime_event', 'event_function');
SELECT public.adp_convert_text_lob_column_to_oid('runtime_event', 'event_function_es5');
SELECT public.adp_convert_text_lob_column_to_oid('runtime_import_template', 'value');
SELECT public.adp_convert_text_lob_column_to_oid('runtime_model', 'specialper_template_sql');
SELECT public.adp_convert_text_lob_column_to_oid('runtime_model', 'model_sql');
SELECT public.adp_convert_text_lob_column_to_oid('runtime_model', 'view_sql');
SELECT public.adp_convert_text_lob_column_to_oid('runtime_print_template', 'template');
SELECT public.adp_convert_text_lob_column_to_oid('runtime_validate', 'params');

SELECT public.adp_convert_text_lob_column_to_oid('base_cp_view_mapping', 'custom_style');
SELECT public.adp_convert_text_lob_column_to_oid('base_cp_view_mapping', 'custom_script');
