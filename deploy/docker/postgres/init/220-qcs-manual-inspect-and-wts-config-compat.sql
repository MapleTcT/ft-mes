-- Restore manual inspection entry points and PostgreSQL compatibility for the
-- recovered WTS configuration view.
--
-- Product inspections can still be created by WOM. These buttons provide an
-- explicit manual entry for commissioning, exception handling, and incoming
-- inspection scenarios without replacing the upstream WOM integration.

CREATE OR REPLACE FUNCTION public.adp_prepend_datagrid_button(
    target jsonb,
    grid_identity text,
    button jsonb
) RETURNS jsonb
LANGUAGE plpgsql
AS $$
DECLARE
    item_key text;
    item_value jsonb;
    patched jsonb;
    current_buttons jsonb;
BEGIN
    IF jsonb_typeof(target) = 'object' THEN
        IF target->>'DataGridCode' = grid_identity
           OR target->>'dataGridName' = grid_identity
           OR target->>'code' = grid_identity
           OR target->>'idPrefix' = grid_identity THEN
            current_buttons := CASE
                WHEN jsonb_typeof(target->'buttons') = 'array' THEN target->'buttons'
                ELSE '[]'::jsonb
            END;

            IF NOT EXISTS (
                SELECT 1
                FROM jsonb_array_elements(current_buttons) AS existing
                WHERE existing->>'id' = button->>'id'
            ) THEN
                target := jsonb_set(
                    target,
                    '{buttons}',
                    jsonb_build_array(button) || current_buttons,
                    true
                );
            END IF;
        END IF;

        patched := '{}'::jsonb;
        FOR item_key, item_value IN SELECT * FROM jsonb_each(target)
        LOOP
            patched := patched || jsonb_build_object(
                item_key,
                public.adp_prepend_datagrid_button(item_value, grid_identity, button)
            );
        END LOOP;
        RETURN patched;
    END IF;

    IF jsonb_typeof(target) = 'array' THEN
        SELECT jsonb_agg(
            public.adp_prepend_datagrid_button(value, grid_identity, button)
        )
        INTO patched
        FROM jsonb_array_elements(target);
        RETURN COALESCE(patched, '[]'::jsonb);
    END IF;

    RETURN target;
END $$;

DO $do$
DECLARE
    view_json_is_oid boolean;
    item record;
    current_payload text;
    patched_payload text;
BEGIN
    SELECT udt_name = 'oid' INTO view_json_is_oid
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'runtime_extra_view'
      AND column_name = 'view_json';

    FOR item IN
        SELECT *
        FROM (
            VALUES
            (
                'QCS_5.0.0.0_inspect_manuInspectList',
                'ec_QCS_inspect_inspect_manuInspectList',
                jsonb_build_object(
                    'id', 'manualAdd',
                    'showname', '新增申请',
                    'namekey', '新增申请',
                    'i18nKey', '新增申请',
                    'buttonstyle', 'add',
                    'operatetype', 'ADD',
                    'operateType', 'ADD',
                    'isHide', false,
                    'ispermission', false,
                    'isPublished', true,
                    'iscallback', true,
                    'iscustomfunc', false,
                    'useInMore', false,
                    'isconfirm', false,
                    'regionType', 'BUTTON',
                    'cellCode', 'cell_adp_qcs_manu_inspect_manual_add',
                    'buttonoperationcode',
                        'manuInspectList_manualAdd_add_QCS_5.0.0.0_inspect_manuInspectList',
                    'viewselect',
                        public.adp_runtime_view_ref('QCS_5.0.0.0_inspect_manuInspectEdit'),
                    'CODE',
                        'manuInspectList_manualAdd_add_QCS_5.0.0.0_inspect_manuInspectList',
                    'NAME', '新增申请',
                    'ICONCLS', 'cui-btn-add',
                    'USEINMORE', 'false',
                    'SEPARATENUM', '0'
                )
            ),
            (
                'QCS_5.0.0.0_inspect_purchInspectList',
                'ec_QCS_inspect_inspect_purchInspectList',
                jsonb_build_object(
                    'id', 'manualAdd',
                    'showname', '新增申请',
                    'namekey', '新增申请',
                    'i18nKey', '新增申请',
                    'buttonstyle', 'add',
                    'operatetype', 'ADD',
                    'operateType', 'ADD',
                    'isHide', false,
                    'ispermission', false,
                    'isPublished', true,
                    'iscallback', true,
                    'iscustomfunc', false,
                    'useInMore', false,
                    'isconfirm', false,
                    'regionType', 'BUTTON',
                    'cellCode', 'cell_adp_qcs_purch_inspect_manual_add',
                    'buttonoperationcode',
                        'purchInspectList_manualAdd_add_QCS_5.0.0.0_inspect_purchInspectList',
                    'viewselect',
                        public.adp_runtime_view_ref('QCS_5.0.0.0_inspect_purchInspectEdit'),
                    'CODE',
                        'purchInspectList_manualAdd_add_QCS_5.0.0.0_inspect_purchInspectList',
                    'NAME', '新增申请',
                    'ICONCLS', 'cui-btn-add',
                    'USEINMORE', 'false',
                    'SEPARATENUM', '0'
                )
            )
        ) AS target(view_code, grid_identity, button)
    LOOP
        current_payload := NULL;

        IF COALESCE(view_json_is_oid, false) THEN
            SELECT convert_from(lo_get(view_json), 'UTF8')
            INTO current_payload
            FROM public.runtime_extra_view
            WHERE code = item.view_code;
        ELSE
            SELECT view_json::text
            INTO current_payload
            FROM public.runtime_extra_view
            WHERE code = item.view_code;
        END IF;

        IF current_payload IS NULL OR current_payload = '' THEN
            RAISE EXCEPTION 'runtime_extra_view % is missing', item.view_code;
        END IF;

        patched_payload := public.adp_prepend_datagrid_button(
            current_payload::jsonb,
            item.grid_identity,
            item.button
        )::text;

        IF patched_payload IS DISTINCT FROM current_payload THEN
            IF COALESCE(view_json_is_oid, false) THEN
                UPDATE public.runtime_extra_view
                SET view_json = lo_from_bytea(0, convert_to(patched_payload, 'UTF8'))
                WHERE code = item.view_code;
            ELSE
                UPDATE public.runtime_extra_view
                SET view_json = patched_payload
                WHERE code = item.view_code;
            END IF;
        END IF;

        UPDATE public.ec_extra_view
        SET view_json = patched_payload
        WHERE code = item.view_code
          AND view_json IS DISTINCT FROM patched_payload;
    END LOOP;
END $do$;

-- publishMenuFrame loads all design-time children for an entity. The recovered
-- ec_* tables intentionally remain text for migration work, while Hibernate
-- maps the payload columns as CLOB. Store OID references only for this WTS
-- entity's design metadata so the configuration page can hydrate the complete
-- view without changing unrelated modules.
CREATE OR REPLACE FUNCTION public.adp_convert_scoped_text_lob_to_oid_ref(
    target_table text,
    target_column text,
    target_predicate text
) RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
    current_udt text;
    item record;
    payload_oid oid;
BEGIN
    SELECT udt_name
    INTO current_udt
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = target_table
      AND column_name = target_column;

    IF current_udt IS NULL OR current_udt NOT IN ('text', 'varchar') THEN
        RETURN;
    END IF;

    FOR item IN EXECUTE format(
        'SELECT ctid, %1$I::text AS payload
           FROM public.%2$I
          WHERE (%3$s)
            AND %1$I IS NOT NULL',
        target_column,
        target_table,
        target_predicate
    )
    LOOP
        IF item.payload ~ '^[0-9]+$'
           AND EXISTS (
               SELECT 1
               FROM pg_largeobject_metadata
               WHERE oid = item.payload::oid
           ) THEN
            CONTINUE;
        END IF;

        payload_oid := lo_from_bytea(0, convert_to(item.payload, 'UTF8'));
        EXECUTE format(
            'UPDATE public.%1$I
                SET %2$I = $1::text
              WHERE ctid = $2',
            target_table,
            target_column
        )
        USING payload_oid, item.ctid;
    END LOOP;
END $$;

SELECT public.adp_convert_scoped_text_lob_to_oid_ref(
    'ec_fast_query_json',
    'query_config',
    $$view_code LIKE 'WTS_1.0.0_workPermit%'$$
);
SELECT public.adp_convert_scoped_text_lob_to_oid_ref(
    'ec_adv_query_json',
    'query_config',
    $$view_code LIKE 'WTS_1.0.0_workPermit%'$$
);
SELECT public.adp_convert_scoped_text_lob_to_oid_ref(
    'ec_extra_query_json',
    'query_config',
    $$view_code LIKE 'WTS_1.0.0_workPermit%'$$
);
SELECT public.adp_convert_scoped_text_lob_to_oid_ref(
    'ec_extra_view',
    'config',
    $$code LIKE 'WTS_1.0.0_workPermit%'
      OR view_code LIKE 'WTS_1.0.0_workPermit%'$$
);
SELECT public.adp_convert_scoped_text_lob_to_oid_ref(
    'ec_extra_view',
    'full_config',
    $$code LIKE 'WTS_1.0.0_workPermit%'
      OR view_code LIKE 'WTS_1.0.0_workPermit%'$$
);
SELECT public.adp_convert_scoped_text_lob_to_oid_ref(
    'ec_extra_view',
    'view_json',
    $$code LIKE 'WTS_1.0.0_workPermit%'
      OR view_code LIKE 'WTS_1.0.0_workPermit%'$$
);

SELECT public.adp_convert_scoped_text_lob_to_oid_ref(
    'ec_data_grid',
    'config',
    $$entity_code = 'WTS_1.0.0_workPermit'
      OR view_code LIKE 'WTS_1.0.0_workPermit%'$$
);
SELECT public.adp_convert_scoped_text_lob_to_oid_ref(
    'ec_data_grid',
    'full_config',
    $$entity_code = 'WTS_1.0.0_workPermit'
      OR view_code LIKE 'WTS_1.0.0_workPermit%'$$
);
SELECT public.adp_convert_scoped_text_lob_to_oid_ref(
    'ec_data_grid',
    'data_grid_json',
    $$entity_code = 'WTS_1.0.0_workPermit'
      OR view_code LIKE 'WTS_1.0.0_workPermit%'$$
);
SELECT public.adp_convert_scoped_text_lob_to_oid_ref(
    'ec_field',
    'config',
    $$entity_code = 'WTS_1.0.0_workPermit'
      OR view_code LIKE 'WTS_1.0.0_workPermit%'$$
);
SELECT public.adp_convert_scoped_text_lob_to_oid_ref(
    'ec_button',
    'config',
    $$entity_code = 'WTS_1.0.0_workPermit'
      OR view_code LIKE 'WTS_1.0.0_workPermit%'$$
);
SELECT public.adp_convert_scoped_text_lob_to_oid_ref(
    'ec_event',
    'event_function',
    $$entity_code = 'WTS_1.0.0_workPermit'
      OR code LIKE 'WTS_1.0.0_workPermit%'$$
);
SELECT public.adp_convert_scoped_text_lob_to_oid_ref(
    'ec_event',
    'event_function_es5',
    $$entity_code = 'WTS_1.0.0_workPermit'
      OR code LIKE 'WTS_1.0.0_workPermit%'$$
);

SELECT public.adp_convert_scoped_text_lob_to_oid_ref(
    'ec_backup_view',
    'config',
    $$view_code LIKE 'WTS_1.0.0_workPermit%'$$
);
SELECT public.adp_convert_scoped_text_lob_to_oid_ref(
    'ec_backup_view',
    'field_config',
    $$view_code LIKE 'WTS_1.0.0_workPermit%'$$
);
SELECT public.adp_convert_scoped_text_lob_to_oid_ref(
    'ec_backup_data_grid',
    'config',
    $$view_code LIKE 'WTS_1.0.0_workPermit%'
      OR targetmodel_code LIKE 'WTS_1.0.0_workPermit%'$$
);
SELECT public.adp_convert_scoped_text_lob_to_oid_ref(
    'ec_backup_data_grid',
    'dg_field_config',
    $$view_code LIKE 'WTS_1.0.0_workPermit%'
      OR targetmodel_code LIKE 'WTS_1.0.0_workPermit%'$$
);

SELECT public.adp_convert_scoped_text_lob_to_oid_ref(
    'ec_property',
    'attributes',
    $$entity_code = 'WTS_1.0.0_workPermit'$$
);
SELECT public.adp_convert_scoped_text_lob_to_oid_ref(
    'ec_property',
    'fillcontent',
    $$entity_code = 'WTS_1.0.0_workPermit'$$
);
SELECT public.adp_convert_scoped_text_lob_to_oid_ref(
    'ec_model',
    'specialper_template_sql',
    $$entity_code = 'WTS_1.0.0_workPermit'$$
);
SELECT public.adp_convert_scoped_text_lob_to_oid_ref(
    'ec_model',
    'model_sql',
    $$entity_code = 'WTS_1.0.0_workPermit'$$
);
SELECT public.adp_convert_scoped_text_lob_to_oid_ref(
    'ec_model',
    'view_sql',
    $$entity_code = 'WTS_1.0.0_workPermit'$$
);
SELECT public.adp_convert_scoped_text_lob_to_oid_ref(
    'ec_validate',
    'params',
    $$entity_code = 'WTS_1.0.0_workPermit'$$
);
SELECT public.adp_convert_scoped_text_lob_to_oid_ref(
    'ec_print_template',
    'template',
    $$entity_code = 'WTS_1.0.0_workPermit'
      OR view_code LIKE 'WTS_1.0.0_workPermit%'$$
);

-- The recovered PostgreSQL environment had only project_module, while the
-- configuration service switches all entity metadata to project_* tables when
-- opening the menu designer. Recreate the missing project draft layer from the
-- design-time ec_* snapshot. These are physical draft tables, so later edits do
-- not mutate the published runtime_* metadata until the platform publishes them.
DO $do$
DECLARE
    item record;
    target_relkind "char";
BEGIN
    FOR item IN
        SELECT *
        FROM (
            VALUES
                ('project_module', 'ec_module'),
                ('project_entity', 'ec_entity'),
                ('project_model', 'ec_model'),
                ('project_property', 'ec_property'),
                ('project_field', 'ec_field'),
                ('project_data_grid', 'ec_data_grid'),
                ('project_view', 'ec_view'),
                ('project_extra_view', 'ec_extra_view'),
                ('project_button', 'ec_button'),
                ('project_event', 'ec_event'),
                ('project_extra_query_json', 'ec_extra_query_json'),
                ('project_fast_query_json', 'ec_fast_query_json'),
                ('project_module_relation', 'ec_module_relation'),
                ('project_module_reference', 'ec_module_reference'),
                ('project_backup_data_grid', 'ec_backup_data_grid'),
                ('project_data_classific', 'ec_data_classific'),
                ('project_data_group', 'ec_data_group'),
                ('project_validate', 'ec_validate'),
                ('project_backup_view', 'ec_backup_view'),
                ('project_customer_condition', 'ec_customer_condition'),
                ('project_print_template', 'ec_print_template'),
                ('project_sql', 'ec_sql'),
                ('project_adv_query_json', 'ec_adv_query_json'),
                ('project_echarts', 'ec_echarts'),
                ('project_echarts_model', 'ec_echarts_model')
        ) AS mappings(project_table, source_table)
    LOOP
        SELECT c.relkind
        INTO target_relkind
        FROM pg_class c
        JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE n.nspname = 'public'
          AND c.relname = item.project_table;

        IF target_relkind IS NULL THEN
            EXECUTE format(
                'CREATE TABLE IF NOT EXISTS public.%1$I
                   (LIKE public.%2$I
                    INCLUDING DEFAULTS
                    INCLUDING CONSTRAINTS
                    INCLUDING INDEXES
                    INCLUDING STORAGE
                    INCLUDING COMMENTS)',
                item.project_table,
                item.source_table
            );
            target_relkind := 'r';
        END IF;

        IF target_relkind IN ('r', 'p') THEN
            EXECUTE format(
                'INSERT INTO public.%1$I
                 SELECT source.*
                   FROM public.%2$I source
                  WHERE NOT EXISTS (
                        SELECT 1
                          FROM public.%1$I target
                         WHERE target.code = source.code
                  )',
                item.project_table,
                item.source_table
            );
        END IF;
    END LOOP;
END $do$;

DROP FUNCTION IF EXISTS public.adp_convert_scoped_text_lob_to_oid_ref(text, text, text);

DROP FUNCTION IF EXISTS public.adp_prepend_datagrid_button(jsonb, text, jsonb);
