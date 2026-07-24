-- Production list export field metadata compatibility.
--
-- 204-production-list-export-runtime-datagrid.sql rebuilds missing runtime
-- datagrid rows from recovered layoutJson so the production pages can expose a
-- target export action. Some recovered list layouts do not carry
-- propertyCode/fullPropertyCode on their grid fields, and a few WOM object
-- properties lost associated_property_code during the Oracle -> PostgreSQL
-- restore. The legacy ExportServiceImpl then falls back to association lookups
-- and can hit NullPointerException while building the Excel config.
--
-- Keep this patch scoped to PROD-023 export targets. It enriches generated
-- datagrid JSON with stable property codes derived from runtime_property and
-- fills only object associations verified from source DTOs/runtime metadata.
-- The legacy export service reads lower-case map keys such as name,
-- columntype and propshowformat, while the recovered Vue grid metadata mostly
-- uses key, columnType and showFormat. Keep both spellings in runtime JSON so
-- normal browser clicks and exportFlag=true API probes use the same contract.

DO $$
DECLARE
    item record;
BEGIN
    FOR item IN
        SELECT *
        FROM (
            VALUES
                ('WOM_1.0.0_produceTask_ProduceTask_productId', 'BaseSet_1.0.0_material_Material_id'),
                ('WOM_1.0.0_produceTask_ProduceTask_formulaId', 'RM_1.0.0_formula_Formula_id'),
                ('WOM_1.0.0_produceTask_ProduceTask_lineId', 'HierarchicalMod_1.0.0_factoryModel_FactoryModel_id'),
                ('WOM_1.0.0_produceTask_ProduceTask_workAreaId', 'HierarchicalMod_1.0.0_factoryModel_FactoryModel_id')
        ) AS associations(property_code, associated_property_code)
    LOOP
        IF EXISTS (
            SELECT 1
              FROM public.runtime_property
             WHERE code = item.property_code
        ) AND EXISTS (
            SELECT 1
              FROM public.runtime_property
             WHERE code = item.associated_property_code
        ) THEN
            UPDATE public.runtime_property
               SET associated_property_code = item.associated_property_code
             WHERE code = item.property_code
               AND COALESCE(associated_property_code, '') = '';
        END IF;

        IF EXISTS (
            SELECT 1
              FROM public.ec_property
             WHERE code = item.property_code
        ) AND EXISTS (
            SELECT 1
              FROM public.ec_property
             WHERE code = item.associated_property_code
        ) THEN
            UPDATE public.ec_property
               SET associated_property_code = item.associated_property_code
             WHERE code = item.property_code
               AND COALESCE(associated_property_code, '') = '';
        END IF;
    END LOOP;
END $$;

CREATE OR REPLACE FUNCTION public.adp_export_child_property_code(
    associated_id_code text,
    child_name text
) RETURNS text
LANGUAGE plpgsql
AS $$
DECLARE
    child_model_code text;
    resolved_code text;
BEGIN
    IF COALESCE(associated_id_code, '') = '' OR COALESCE(child_name, '') = '' THEN
        RETURN NULL;
    END IF;

    IF child_name = 'id' THEN
        RETURN associated_id_code;
    END IF;

    SELECT model_code
      INTO child_model_code
      FROM public.runtime_property
     WHERE code = associated_id_code
     LIMIT 1;

    IF COALESCE(child_model_code, '') = '' THEN
        child_model_code := regexp_replace(associated_id_code, '_id$', '');
    END IF;

    SELECT code
      INTO resolved_code
      FROM public.runtime_property
     WHERE model_code = child_model_code
       AND name = child_name
     ORDER BY valid DESC, version DESC
     LIMIT 1;

    IF resolved_code IS NULL THEN
        SELECT code
          INTO resolved_code
          FROM public.runtime_property
         WHERE code = child_model_code || '_' || child_name
         LIMIT 1;
    END IF;

    RETURN resolved_code;
END $$;

CREATE OR REPLACE FUNCTION public.adp_export_field_property_code(
    field_json jsonb,
    model_code_arg text
) RETURNS text
LANGUAGE plpgsql
AS $$
DECLARE
    field_key text;
    existing_code text;
    parent_name text;
    child_name text;
    parent_code text;
    associated_id_code text;
    child_code text;
BEGIN
    existing_code := COALESCE(
        NULLIF(field_json->>'propertyCode', ''),
        NULLIF(field_json->>'fullPropertyCode', ''),
        NULLIF(field_json->>'property_code', ''),
        NULLIF(field_json->>'full_property_code', '')
    );
    IF existing_code IS NOT NULL THEN
        RETURN existing_code;
    END IF;

    field_key := NULLIF(field_json->>'key', '');
    IF field_key IS NULL OR COALESCE(model_code_arg, '') = '' THEN
        RETURN NULL;
    END IF;

    IF position('.' IN field_key) = 0 THEN
        SELECT code
          INTO parent_code
          FROM public.runtime_property
         WHERE model_code = model_code_arg
           AND name = field_key
         ORDER BY valid DESC, version DESC
         LIMIT 1;

        IF parent_code IS NULL THEN
            SELECT code
              INTO parent_code
              FROM public.runtime_property
             WHERE code = model_code_arg || '_' || field_key
             LIMIT 1;
        END IF;

        RETURN parent_code;
    END IF;

    parent_name := split_part(field_key, '.', 1);
    child_name := substring(field_key FROM position('.' IN field_key) + 1);

    SELECT code, associated_property_code
      INTO parent_code, associated_id_code
      FROM public.runtime_property
     WHERE model_code = model_code_arg
       AND name = parent_name
     ORDER BY valid DESC, version DESC
     LIMIT 1;

    IF parent_code IS NULL THEN
        SELECT code, associated_property_code
          INTO parent_code, associated_id_code
          FROM public.runtime_property
         WHERE code = model_code_arg || '_' || parent_name
         LIMIT 1;
    END IF;

    child_code := public.adp_export_child_property_code(associated_id_code, child_name);
    IF parent_code IS NOT NULL AND child_code IS NOT NULL THEN
        RETURN parent_code || '||' || child_code;
    END IF;

    RETURN parent_code;
END $$;

CREATE OR REPLACE FUNCTION public.adp_export_terminal_property_code(
    full_property_code text
) RETURNS text
LANGUAGE plpgsql
AS $$
DECLARE
    parts text[];
BEGIN
    IF COALESCE(full_property_code, '') = '' THEN
        RETURN NULL;
    END IF;

    parts := string_to_array(full_property_code, '||');
    RETURN parts[array_length(parts, 1)];
END $$;

CREATE OR REPLACE FUNCTION public.adp_export_field_metadata(
    field_json jsonb,
    model_code_arg text
) RETURNS jsonb
LANGUAGE plpgsql
AS $$
DECLARE
    resolved_code text;
    terminal_code text;
    prop_name text;
    prop_type text;
    prop_display_name text;
    prop_nullable boolean;
    field_key text;
    fallback_name text;
    export_name text;
    export_namekey text;
    export_column_type text;
    export_show_format text;
BEGIN
    resolved_code := public.adp_export_field_property_code(field_json, model_code_arg);
    IF resolved_code IS NULL THEN
        RETURN '{}'::jsonb;
    END IF;

    terminal_code := public.adp_export_terminal_property_code(resolved_code);

    SELECT rp.name, rp.type, rp.display_name, rp.nullable
      INTO prop_name, prop_type, prop_display_name, prop_nullable
      FROM public.runtime_property rp
     WHERE rp.code = terminal_code
     ORDER BY rp.valid DESC, rp.version DESC
     LIMIT 1;

    field_key := COALESCE(NULLIF(field_json->>'key', ''), NULLIF(prop_name, ''), terminal_code);
    fallback_name := COALESCE(NULLIF(regexp_replace(field_key, '^.*\.', ''), ''), terminal_code);
    export_name := COALESCE(
        NULLIF(field_json->>'name', ''),
        NULLIF(field_json->>'dataIndex', ''),
        NULLIF(prop_name, ''),
        fallback_name
    );
    export_namekey := COALESCE(
        NULLIF(field_json->>'namekey', ''),
        NULLIF(field_json->>'displayName', ''),
        NULLIF(field_json->>'displayText', ''),
        NULLIF(field_json->>'title', ''),
        NULLIF(prop_display_name, ''),
        export_name
    );
    export_column_type := upper(COALESCE(
        NULLIF(field_json->>'columntype', ''),
        NULLIF(field_json->>'columnType', ''),
        NULLIF(field_json->>'type', ''),
        NULLIF(prop_type, ''),
        'TEXT'
    ));
    export_show_format := COALESCE(
        NULLIF(field_json->>'propshowformat', ''),
        NULLIF(field_json->>'showFormat', '')
    );

    IF export_column_type = 'DATE' THEN
        IF export_show_format IS NULL OR export_show_format NOT IN ('Y', 'YM', 'YMD', 'YMD_H', 'YMD_HM', 'YMD_HMS') THEN
            export_show_format := 'YMD';
        END IF;
    ELSIF export_column_type = 'DATETIME' THEN
        IF export_show_format IS NULL OR export_show_format NOT IN ('Y', 'YM', 'YMD', 'YMD_H', 'YMD_HM', 'YMD_HMS') THEN
            export_show_format := 'YMD_HMS';
        END IF;
    ELSE
        export_show_format := COALESCE(export_show_format, 'TEXT');
    END IF;

    RETURN jsonb_build_object(
        'propertyCode', resolved_code,
        'fullPropertyCode', resolved_code,
        'name', export_name,
        'namekey', export_namekey,
        'displayName', export_namekey,
        'displayText', export_namekey,
        'columntype', export_column_type,
        'columnType', export_column_type,
        'propshowformat', export_show_format,
        'showFormat', export_show_format,
        'nullable', COALESCE(prop_nullable, true)
    );
END $$;

CREATE OR REPLACE FUNCTION public.adp_embed_export_properties_in_buttons(
    target jsonb,
    export_properties text
) RETURNS jsonb
LANGUAGE plpgsql
AS $$
DECLARE
    item_key text;
    item_value jsonb;
    item_text text;
    patched jsonb;
BEGIN
    IF target IS NULL THEN
        RETURN target;
    END IF;

    IF jsonb_typeof(target) = 'object' THEN
        patched := '{}'::jsonb;
        FOR item_key, item_value IN SELECT * FROM jsonb_each(target)
        LOOP
            IF jsonb_typeof(item_value) = 'string'
               AND item_key IN ('funcbody', 'funcbody_es5', 'scriptCode', 'function', 'function_es5')
               AND item_value #>> '{}' LIKE '%properties: []%' THEN
                item_text := replace(item_value #>> '{}', 'properties: [],', 'properties: ' || export_properties || ',');
                patched := patched || jsonb_build_object(item_key, to_jsonb(item_text));
            ELSE
                patched := patched || jsonb_build_object(
                    item_key,
                    public.adp_embed_export_properties_in_buttons(item_value, export_properties)
                );
            END IF;
        END LOOP;
        RETURN patched;
    END IF;

    IF jsonb_typeof(target) = 'array' THEN
        SELECT jsonb_agg(public.adp_embed_export_properties_in_buttons(value, export_properties))
          INTO patched
          FROM jsonb_array_elements(target);
        RETURN COALESCE(patched, '[]'::jsonb);
    END IF;

    RETURN target;
END $$;

CREATE OR REPLACE FUNCTION public.adp_enrich_export_datagrid_fields(
    target jsonb,
    fallback_model_code text
) RETURNS jsonb
LANGUAGE plpgsql
AS $$
DECLARE
    item_key text;
    item_value jsonb;
    patched jsonb;
    model_code_value text;
    enriched_fields jsonb;
BEGIN
    IF target IS NULL THEN
        RETURN target;
    END IF;

    IF jsonb_typeof(target) = 'object' THEN
        model_code_value := COALESCE(
            NULLIF(target->>'modelCode', ''),
            NULLIF(target->>'targetModelCode', ''),
            NULLIF(target->>'targetmodelCode', ''),
            NULLIF(fallback_model_code, '')
        );

        IF jsonb_typeof(target->'fields') = 'array' THEN
            SELECT COALESCE(jsonb_agg(
                       CASE
                           WHEN public.adp_export_field_property_code(field_value, model_code_value) IS NULL THEN
                               field_value
                           ELSE
                               field_value || public.adp_export_field_metadata(field_value, model_code_value)
                       END
                       ORDER BY ordinality
                   ), '[]'::jsonb)
              INTO enriched_fields
              FROM jsonb_array_elements(target->'fields') WITH ORDINALITY AS fields(field_value, ordinality)
             WHERE NOT (
                 public.adp_export_field_property_code(field_value, model_code_value) IS NULL
                 AND COALESCE(field_value->>'key', '') LIKE '%_LISTPT_CUSTOM_%'
             );

            target := jsonb_set(target, '{fields}', enriched_fields, true);
        END IF;

        patched := '{}'::jsonb;
        FOR item_key, item_value IN SELECT * FROM jsonb_each(target)
        LOOP
            patched := patched || jsonb_build_object(
                item_key,
                public.adp_enrich_export_datagrid_fields(item_value, model_code_value)
            );
        END LOOP;
        RETURN patched;
    END IF;

    IF jsonb_typeof(target) = 'array' THEN
        SELECT jsonb_agg(public.adp_enrich_export_datagrid_fields(value, fallback_model_code))
          INTO patched
          FROM jsonb_array_elements(target);
        RETURN COALESCE(patched, '[]'::jsonb);
    END IF;

    RETURN target;
END $$;

DO $do$
DECLARE
    target record;
    item record;
    current_payload text;
    patched_payload text;
BEGIN
    FOR target IN
        SELECT *
          FROM (VALUES
              ('WOM_1.0.0_produceTask_makeTaskList', 'WOM_1.0.0_produceTask_ProduceTask'),
              ('RM_1.0.0_formula_batchFormulaList', 'RM_1.0.0_formula_Formula'),
              ('QCS_5.0.0.0_inspectReport_manuInspReportList', 'QCS_5.0.0.0_inspectReport_InspectReport'),
              ('QCS_5.0.0.0_unQlfDeal_manuUnQlfDealList', 'QCS_5.0.0.0_unQlfDeal_UnQlfDeal'),
              ('QCS_5.0.0.0_inspectRelease_manuInspReleaseList', 'QCS_5.0.0.0_inspectRelease_InspectRelease')
          ) AS t(view_code, model_code)
    LOOP
        FOR item IN
            SELECT column_name, udt_name
              FROM information_schema.columns
             WHERE table_schema = 'public'
               AND table_name = 'runtime_extra_view'
               AND column_name IN ('view_json', 'full_config', 'config')
        LOOP
            IF item.udt_name = 'oid' THEN
                EXECUTE format(
                    'SELECT convert_from(lo_get(%I), ''UTF8'') FROM public.runtime_extra_view WHERE code = $1 OR view_code = $1 LIMIT 1',
                    item.column_name
                )
                INTO current_payload
                USING target.view_code;
            ELSE
                EXECUTE format(
                    'SELECT %I::text FROM public.runtime_extra_view WHERE code = $1 OR view_code = $1 LIMIT 1',
                    item.column_name
                )
                INTO current_payload
                USING target.view_code;
            END IF;

            IF current_payload IS NOT NULL AND current_payload ~ '^\s*[\{\[]' THEN
                patched_payload := public.adp_enrich_export_datagrid_fields(
                    current_payload::jsonb,
                    target.model_code
                )::text;

                patched_payload := public.adp_embed_export_properties_in_buttons(
                    patched_payload::jsonb,
                    COALESCE(public.adp_find_layout_datagrid(patched_payload::jsonb, target.view_code)->'fields', '[]'::jsonb)::text
                )::text;

                IF item.udt_name = 'oid' THEN
                    EXECUTE format(
                        'UPDATE public.runtime_extra_view SET %I = lo_from_bytea(0, convert_to($1, ''UTF8'')) WHERE code = $2 OR view_code = $2',
                        item.column_name
                    )
                    USING patched_payload, target.view_code;
                ELSE
                    EXECUTE format(
                        'UPDATE public.runtime_extra_view SET %I = $1 WHERE code = $2 OR view_code = $2',
                        item.column_name
                    )
                    USING patched_payload, target.view_code;
                END IF;
            END IF;
        END LOOP;

        FOR item IN
            SELECT column_name, udt_name
              FROM information_schema.columns
             WHERE table_schema = 'public'
               AND table_name = 'runtime_data_grid'
               AND column_name IN ('data_grid_json', 'full_config', 'config')
        LOOP
            IF item.udt_name = 'oid' THEN
                EXECUTE format(
                    'SELECT convert_from(lo_get(%I), ''UTF8'') FROM public.runtime_data_grid WHERE code = $1 OR view_code = $1 LIMIT 1',
                    item.column_name
                )
                INTO current_payload
                USING target.view_code;
            ELSE
                EXECUTE format(
                    'SELECT %I::text FROM public.runtime_data_grid WHERE code = $1 OR view_code = $1 LIMIT 1',
                    item.column_name
                )
                INTO current_payload
                USING target.view_code;
            END IF;

            IF current_payload IS NOT NULL AND current_payload ~ '^\s*[\{\[]' THEN
                patched_payload := public.adp_enrich_export_datagrid_fields(
                    current_payload::jsonb,
                    target.model_code
                )::text;

                IF item.udt_name = 'oid' THEN
                    EXECUTE format(
                        'UPDATE public.runtime_data_grid SET %I = lo_from_bytea(0, convert_to($1, ''UTF8'')) WHERE code = $2 OR view_code = $2',
                        item.column_name
                    )
                    USING patched_payload, target.view_code;
                ELSE
                    EXECUTE format(
                        'UPDATE public.runtime_data_grid SET %I = $1 WHERE code = $2 OR view_code = $2',
                        item.column_name
                    )
                    USING patched_payload, target.view_code;
                END IF;
            END IF;
        END LOOP;

        IF EXISTS (
            SELECT 1
              FROM information_schema.tables
             WHERE table_schema = 'public'
               AND table_name = 'ec_data_grid'
        ) THEN
            FOR item IN
                SELECT column_name
                  FROM information_schema.columns
                 WHERE table_schema = 'public'
                   AND table_name = 'ec_data_grid'
                   AND column_name IN ('data_grid_json', 'full_config', 'config')
            LOOP
                EXECUTE format(
                    'SELECT %I::text FROM public.ec_data_grid WHERE code = $1 OR view_code = $1 LIMIT 1',
                    item.column_name
                )
                INTO current_payload
                USING target.view_code;

                IF current_payload IS NOT NULL AND current_payload ~ '^\s*[\{\[]' THEN
                    patched_payload := public.adp_enrich_export_datagrid_fields(
                        current_payload::jsonb,
                        target.model_code
                    )::text;
                    EXECUTE format(
                        'UPDATE public.ec_data_grid SET %I = $1 WHERE code = $2 OR view_code = $2',
                        item.column_name
                    )
                    USING patched_payload, target.view_code;
                END IF;
            END LOOP;
        END IF;
    END LOOP;
END $do$;
