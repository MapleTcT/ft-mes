-- Runtime compatibility for the recovered WorkAppointment work-plan list.
--
-- The restored list renders with modern runtime_extra_view.view_json, but the
-- generated WAPS query service still reads legacy DataGrid/config metadata when
-- a saved row exists. Without these rows, workPlanList-query and
-- workPlanList-pending fail in WorkAppointmentWorkTicketPlanServiceImpl.commonQuery.

CREATE TABLE IF NOT EXISTS public.codex_runtime_extra_view_backup_151_waps AS
SELECT *
FROM public.runtime_extra_view
WHERE false;

INSERT INTO public.codex_runtime_extra_view_backup_151_waps
SELECT *
FROM public.runtime_extra_view source
WHERE source.code = 'workAppointment_6.1.6.1_workPlan_workPlanList'
  AND NOT EXISTS (
      SELECT 1
      FROM public.codex_runtime_extra_view_backup_151_waps backup
      WHERE backup.code = source.code
  );

CREATE TABLE IF NOT EXISTS public.codex_runtime_data_grid_backup_151_waps AS
SELECT *
FROM public.runtime_data_grid
WHERE false;

INSERT INTO public.codex_runtime_data_grid_backup_151_waps
SELECT *
FROM public.runtime_data_grid source
WHERE source.code = 'workAppointment_6.1.6.1_workPlan_workPlanList'
  AND NOT EXISTS (
      SELECT 1
      FROM public.codex_runtime_data_grid_backup_151_waps backup
      WHERE backup.code = source.code
  );

CREATE TABLE IF NOT EXISTS public.codex_ec_data_grid_backup_151_waps AS
SELECT *
FROM public.ec_data_grid
WHERE false;

INSERT INTO public.codex_ec_data_grid_backup_151_waps
SELECT *
FROM public.ec_data_grid source
WHERE source.code = 'workAppointment_6.1.6.1_workPlan_workPlanList'
  AND NOT EXISTS (
      SELECT 1
      FROM public.codex_ec_data_grid_backup_151_waps backup
      WHERE backup.code = source.code
      );

CREATE TABLE IF NOT EXISTS public.codex_runtime_sql_backup_151_waps AS
SELECT *
FROM public.runtime_sql
WHERE false;

INSERT INTO public.codex_runtime_sql_backup_151_waps
SELECT *
FROM public.runtime_sql source
WHERE source.data_grid_code = 'workAppointment_6.1.6.1_workPlan_workPlanList'
  AND NOT EXISTS (
      SELECT 1
      FROM public.codex_runtime_sql_backup_151_waps backup
      WHERE backup.code = source.code
  );

CREATE TABLE IF NOT EXISTS public.codex_ec_sql_backup_151_waps AS
SELECT *
FROM public.ec_sql
WHERE false;

INSERT INTO public.codex_ec_sql_backup_151_waps
SELECT *
FROM public.ec_sql source
WHERE source.data_grid_code = 'workAppointment_6.1.6.1_workPlan_workPlanList'
  AND NOT EXISTS (
      SELECT 1
      FROM public.codex_ec_sql_backup_151_waps backup
      WHERE backup.code = source.code
  );

DO $$
DECLARE
    extra_view_json_is_oid boolean;
    data_grid_json_is_oid boolean;
    extra_payload text;
    patched_extra_payload text;
    grid_payload text := '{
      "code": "workAppointment_6.1.6.1_workPlan_workPlanList",
      "DataGridCode": "workAppointment_6.1.6.1_workPlan_workPlanList",
      "dataGridName": "workAppointment_6.1.6.1_workPlan_workPlanList",
      "modelCode": "workAppointment_6.1.6.1_workPlan_WorkTicketPlan",
      "fields": [
        {"key":"tableNo","width":100,"namekey":"计划编码","isHidden":false,"showType":"TEXTFIELD","columnType":"TEXT","showFormat":"TEXT"},
        {"key":"applyDep.name","width":100,"namekey":"申报部门/车间","isHidden":false,"showType":"TEXTFIELD","columnType":"TEXT","showFormat":"TEXT"},
        {"key":"planDescription","width":100,"namekey":"计划描述","isHidden":false,"showType":"TEXTFIELD","columnType":"TEXT","showFormat":"TEXT"},
        {"key":"applyStaff.name","width":100,"namekey":"报送人","isHidden":false,"showType":"TEXTFIELD","columnType":"TEXT","showFormat":"TEXT"},
        {"key":"applyTime","width":150,"namekey":"报送时间","isHidden":false,"showType":"DATETIME","columnType":"DATETIME","showFormat":"YMD_HMS"},
        {"key":"workTime","width":150,"namekey":"作业时间","isHidden":false,"showType":"DATETIME","columnType":"DATETIME","showFormat":"YMD_HMS"},
        {"key":"applyDep.id","width":120,"namekey":"applyDep.id","isHidden":true,"showType":"TEXTFIELD","columnType":"TEXT","showFormat":"TEXT"},
        {"key":"applyStaff.id","width":120,"namekey":"applyStaff.id","isHidden":true,"showType":"TEXTFIELD","columnType":"TEXT","showFormat":"TEXT"}
      ],
      "buttons": []
    }';
    legacy_layout_json text := '{"sections":[]}';
    legacy_config_json text := '{"layout":{"sections":[]}}';
    legacy_config_xml text := '<config><layout><sections><list/></sections></layout></config>';
BEGIN
    SELECT udt_name = 'oid' INTO extra_view_json_is_oid
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'runtime_extra_view'
      AND column_name = 'view_json';

    SELECT udt_name = 'oid' INTO data_grid_json_is_oid
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'runtime_data_grid'
      AND column_name = 'data_grid_json';

    SELECT CASE
               WHEN COALESCE(extra_view_json_is_oid, false)
                   THEN convert_from(lo_get(view_json::oid), 'UTF8')
               ELSE view_json::text
           END
      INTO extra_payload
      FROM public.runtime_extra_view
     WHERE code = 'workAppointment_6.1.6.1_workPlan_workPlanList';

    IF extra_payload IS NOT NULL THEN
        patched_extra_payload := jsonb_set(
            jsonb_set(
                jsonb_set(extra_payload::jsonb, '{layout}', legacy_layout_json::jsonb, true),
                '{configMap}', legacy_config_json::jsonb, true
            ),
            '{config}', legacy_config_json::jsonb, true
        )::text;

        IF COALESCE(extra_view_json_is_oid, false) THEN
            UPDATE public.runtime_extra_view
               SET view_json = lo_from_bytea(0, convert_to(patched_extra_payload, 'UTF8')),
                   config = lo_from_bytea(0, convert_to(legacy_config_xml, 'UTF8')),
                   full_config = lo_from_bytea(0, convert_to(legacy_config_xml, 'UTF8'))
             WHERE code = 'workAppointment_6.1.6.1_workPlan_workPlanList';

            UPDATE public.ec_extra_view
               SET view_json = patched_extra_payload,
                   config = legacy_config_xml,
                   full_config = legacy_config_xml
             WHERE code = 'workAppointment_6.1.6.1_workPlan_workPlanList';
        ELSE
            UPDATE public.runtime_extra_view
               SET view_json = patched_extra_payload,
                   config = legacy_config_xml,
                   full_config = legacy_config_xml
             WHERE code = 'workAppointment_6.1.6.1_workPlan_workPlanList';

            UPDATE public.ec_extra_view
               SET view_json = patched_extra_payload,
                   config = legacy_config_xml,
                   full_config = legacy_config_xml
             WHERE code = 'workAppointment_6.1.6.1_workPlan_workPlanList';
        END IF;
    END IF;

    IF COALESCE(data_grid_json_is_oid, false) THEN
        INSERT INTO public.runtime_data_grid (
            code, ec_env, version, valid, entity_code, module_code, data_grid_json,
            proj_flag, operate_name, permission_code, is_permission, data_grid_type,
            full_config, data_grid_name, config, view_code, name
        )
        VALUES (
            'workAppointment_6.1.6.1_workPlan_workPlanList',
            'product',
            0,
            true,
            'workAppointment_6.1.6.1_workPlan',
            'workAppointment_6.1.6.1',
            lo_from_bytea(0, convert_to(grid_payload, 'UTF8')),
            false,
            NULL,
            'workAppointment_6.1.6.1_workPlan_workPlanList',
            false,
            0,
            lo_from_bytea(0, convert_to(grid_payload, 'UTF8')),
            'workAppointment_6.1.6.1_workPlan_workPlanList',
            lo_from_bytea(0, convert_to(grid_payload, 'UTF8')),
            'workAppointment_6.1.6.1_workPlan_workPlanList',
            'workAppointment_6.1.6.1_workPlan_workPlanList'
        )
        ON CONFLICT (code) DO UPDATE SET
            ec_env = EXCLUDED.ec_env,
            version = EXCLUDED.version,
            valid = EXCLUDED.valid,
            entity_code = EXCLUDED.entity_code,
            module_code = EXCLUDED.module_code,
            data_grid_json = EXCLUDED.data_grid_json,
            proj_flag = EXCLUDED.proj_flag,
            permission_code = EXCLUDED.permission_code,
            is_permission = EXCLUDED.is_permission,
            data_grid_type = EXCLUDED.data_grid_type,
            full_config = EXCLUDED.full_config,
            data_grid_name = EXCLUDED.data_grid_name,
            config = EXCLUDED.config,
            view_code = EXCLUDED.view_code,
            name = EXCLUDED.name;
    ELSE
        INSERT INTO public.runtime_data_grid (
            code, ec_env, version, valid, entity_code, module_code, data_grid_json,
            proj_flag, operate_name, permission_code, is_permission, data_grid_type,
            full_config, data_grid_name, config, view_code, name
        )
        VALUES (
            'workAppointment_6.1.6.1_workPlan_workPlanList',
            'product',
            0,
            true,
            'workAppointment_6.1.6.1_workPlan',
            'workAppointment_6.1.6.1',
            grid_payload,
            false,
            NULL,
            'workAppointment_6.1.6.1_workPlan_workPlanList',
            false,
            0,
            grid_payload,
            'workAppointment_6.1.6.1_workPlan_workPlanList',
            grid_payload,
            'workAppointment_6.1.6.1_workPlan_workPlanList',
            'workAppointment_6.1.6.1_workPlan_workPlanList'
        )
        ON CONFLICT (code) DO UPDATE SET
            ec_env = EXCLUDED.ec_env,
            version = EXCLUDED.version,
            valid = EXCLUDED.valid,
            entity_code = EXCLUDED.entity_code,
            module_code = EXCLUDED.module_code,
            data_grid_json = EXCLUDED.data_grid_json,
            proj_flag = EXCLUDED.proj_flag,
            permission_code = EXCLUDED.permission_code,
            is_permission = EXCLUDED.is_permission,
            data_grid_type = EXCLUDED.data_grid_type,
            full_config = EXCLUDED.full_config,
            data_grid_name = EXCLUDED.data_grid_name,
            config = EXCLUDED.config,
            view_code = EXCLUDED.view_code,
            name = EXCLUDED.name;
    END IF;

	    INSERT INTO public.ec_data_grid (
	        code, ec_env, version, valid, entity_code, module_code, data_grid_json,
	        proj_flag, operate_name, permission_code, is_permission, data_grid_type,
	        full_config, data_grid_name, config, view_code, name
	    )
	    VALUES (
	        'workAppointment_6.1.6.1_workPlan_workPlanList',
	        'product',
	        0,
	        1,
	        'workAppointment_6.1.6.1_workPlan',
	        'workAppointment_6.1.6.1',
	        grid_payload,
	        0,
	        NULL,
	        'workAppointment_6.1.6.1_workPlan_workPlanList',
	        0,
	        0,
	        grid_payload,
	        'workAppointment_6.1.6.1_workPlan_workPlanList',
	        grid_payload,
	        'workAppointment_6.1.6.1_workPlan_workPlanList',
	        'workAppointment_6.1.6.1_workPlan_workPlanList'
	    )
	    ON CONFLICT (code) DO UPDATE SET
	        ec_env = EXCLUDED.ec_env,
	        version = EXCLUDED.version,
	        valid = EXCLUDED.valid,
	        entity_code = EXCLUDED.entity_code,
	        module_code = EXCLUDED.module_code,
	        data_grid_json = EXCLUDED.data_grid_json,
	        proj_flag = EXCLUDED.proj_flag,
	        permission_code = EXCLUDED.permission_code,
	        is_permission = EXCLUDED.is_permission,
	        data_grid_type = EXCLUDED.data_grid_type,
	        full_config = EXCLUDED.full_config,
	        data_grid_name = EXCLUDED.data_grid_name,
	        config = EXCLUDED.config,
	        view_code = EXCLUDED.view_code,
	        name = EXCLUDED.name;
	END $$;

INSERT INTO public.runtime_sql (
    code, ec_env, version, proj_flag, data_grid_code, view_code, type, query_sql
)
SELECT
    'workAppointment_6.1.6.1_workPlan_workPlanList_dg_' || source.type::text,
    source.ec_env,
    source.version,
    source.proj_flag,
    'workAppointment_6.1.6.1_workPlan_workPlanList',
    source.view_code,
    source.type,
    source.query_sql
FROM public.runtime_sql source
WHERE source.view_code = 'workAppointment_6.1.6.1_workPlan_workPlanList'
  AND COALESCE(source.data_grid_code, '') = ''
ON CONFLICT (code) DO UPDATE SET
    ec_env = EXCLUDED.ec_env,
    version = EXCLUDED.version,
    proj_flag = EXCLUDED.proj_flag,
    data_grid_code = EXCLUDED.data_grid_code,
    view_code = EXCLUDED.view_code,
    type = EXCLUDED.type,
    query_sql = EXCLUDED.query_sql;

INSERT INTO public.ec_sql (
    code, ec_env, version, proj_flag, data_grid_code, view_code, type, query_sql
)
SELECT
    'workAppointment_6.1.6.1_workPlan_workPlanList_dg_' || source.type::text,
    source.ec_env,
    source.version,
    source.proj_flag,
    'workAppointment_6.1.6.1_workPlan_workPlanList',
    source.view_code,
    source.type,
    source.query_sql
FROM public.ec_sql source
WHERE source.view_code = 'workAppointment_6.1.6.1_workPlan_workPlanList'
  AND COALESCE(source.data_grid_code, '') = ''
ON CONFLICT (code) DO UPDATE SET
    ec_env = EXCLUDED.ec_env,
    version = EXCLUDED.version,
    proj_flag = EXCLUDED.proj_flag,
    data_grid_code = EXCLUDED.data_grid_code,
    view_code = EXCLUDED.view_code,
    type = EXCLUDED.type,
    query_sql = EXCLUDED.query_sql;
