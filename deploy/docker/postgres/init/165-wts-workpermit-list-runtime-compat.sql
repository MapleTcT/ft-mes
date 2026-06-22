-- Runtime compatibility for WTS work permit LIST acceptance on PostgreSQL.
--
-- Recovered deployments can have runtime_extra_view.view_json restored while
-- config/full_config remain NULL. WTSWorkPermitServiceImpl.commonQuery still
-- reads ExtraView.configMap for LISTPT sections, so the list-query endpoint
-- fails before it can render rows. This file keeps the patch narrowly scoped
-- to the recovered work permit list view.
--
-- The recovered runtime package can also miss runtime_sql rows keyed by
-- data_grid_code, and runtime_data_grid.data_grid_json may be empty. The WTS
-- service uses those records during export/query.
--
-- The same recovered test data also contained numeric/non-JSON payload values
-- on wts_work_permits. WTSWorkPermit.payload is mapped as @Lob, so PostgreSQL
-- must store it as a large-object OID for Hibernate while WorkPermitViewEvent
-- needs to decode the JSON from that OID.

DO $$
DECLARE
    payload text := $xml$<config><layout><sections><list><list-item><cells><list><list-item><regionType><![CDATA[LISTPT]]></regionType><columnType><![CDATA[BAPCODE]]></columnType><propertyCode><![CDATA[WTS_1.0.0_workPermit_WorkPermit_ticketNo]]></propertyCode><key><![CDATA[ticketNo]]></key><name><![CDATA[ticketNo]]></name><cellCode><![CDATA[cell_compat_ticket_no]]></cellCode><isHidden><![CDATA[false]]></isHidden></list-item><list-item><regionType><![CDATA[LISTPT]]></regionType><columnType><![CDATA[TEXT]]></columnType><propertyCode><![CDATA[WTS_1.0.0_workPermit_WorkPermit_content]]></propertyCode><key><![CDATA[content]]></key><name><![CDATA[content]]></name><cellCode><![CDATA[cell_compat_content]]></cellCode><isHidden><![CDATA[false]]></isHidden></list-item></list></cells><listProperty><isExportExcel><![CDATA[true]]></isExportExcel></listProperty><regionType><![CDATA[LISTPT]]></regionType><sectionCode><![CDATA[section_compat_listpt]]></sectionCode></list-item></list></sections></layout></config>$xml$;
    config_is_oid boolean;
    cfg_oid oid;
    full_cfg_oid oid;
BEGIN
    SELECT udt_name = 'oid'
      INTO config_is_oid
      FROM information_schema.columns
     WHERE table_schema = 'public'
       AND table_name = 'runtime_extra_view'
       AND column_name = 'config';

    IF COALESCE(config_is_oid, false) THEN
        cfg_oid := lo_from_bytea(0, convert_to(payload, 'UTF8'));
        full_cfg_oid := lo_from_bytea(0, convert_to(payload, 'UTF8'));

        UPDATE public.runtime_extra_view
           SET config = COALESCE(config, cfg_oid),
               full_config = COALESCE(full_config, full_cfg_oid),
               ec_env = COALESCE(ec_env, 'product')
         WHERE code = 'WTS_1.0.0_workPermit_workPermitList'
            OR view_code = 'WTS_1.0.0_workPermit_workPermitList';
    ELSE
        UPDATE public.runtime_extra_view
           SET config = COALESCE(config, payload),
               full_config = COALESCE(full_config, payload),
               ec_env = COALESCE(ec_env, 'product')
         WHERE code = 'WTS_1.0.0_workPermit_workPermitList'
            OR view_code = 'WTS_1.0.0_workPermit_workPermitList';
    END IF;
END $$;

INSERT INTO public.runtime_sql (
    code, ec_env, version, proj_flag, data_grid_code, view_code, type, query_sql
)
SELECT 'WTS_1.0.0_workPermit_workPermitList_dg_' || type::text,
       ec_env,
       version,
       proj_flag,
       'WTS_1.0.0_workPermit_workPermitList',
       view_code,
       type,
       query_sql
  FROM public.runtime_sql
 WHERE view_code = 'WTS_1.0.0_workPermit_workPermitList'
   AND COALESCE(data_grid_code, '') = ''
   AND type IN (3, 6)
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
SELECT 'WTS_1.0.0_workPermit_workPermitList_dg_' || type::text,
       ec_env,
       version,
       proj_flag,
       'WTS_1.0.0_workPermit_workPermitList',
       view_code,
       type,
       query_sql
  FROM public.ec_sql
 WHERE view_code = 'WTS_1.0.0_workPermit_workPermitList'
   AND COALESCE(data_grid_code, '') = ''
   AND type IN (3, 6)
ON CONFLICT (code) DO UPDATE SET
    ec_env = EXCLUDED.ec_env,
    version = EXCLUDED.version,
    proj_flag = EXCLUDED.proj_flag,
    data_grid_code = EXCLUDED.data_grid_code,
    view_code = EXCLUDED.view_code,
    type = EXCLUDED.type,
    query_sql = EXCLUDED.query_sql;

DO $$
DECLARE
    grid_payload text := $json$
{
  "code": "WTS_1.0.0_workPermit_workPermitList",
  "DataGridCode": "WTS_1.0.0_workPermit_workPermitList",
  "dataGridName": "WTS_1.0.0_workPermit_workPermitList",
  "modelCode": "WTS_1.0.0_workPermit_WorkPermit",
  "fields": [
    {"key":"ticketNo","width":140,"namekey":"作业票号","isHidden":false,"showType":"TEXTFIELD","columnType":"TEXT","showFormat":"TEXT","propertyCode":"WTS_1.0.0_workPermit_WorkPermit_ticketNo"},
    {"key":"content","width":180,"namekey":"作业内容","isHidden":false,"showType":"TEXTFIELD","columnType":"TEXT","showFormat":"TEXT","propertyCode":"WTS_1.0.0_workPermit_WorkPermit_content"},
    {"key":"workType","width":120,"namekey":"作业类型","isHidden":false,"showType":"SELECTCOMP","columnType":"SYSTEMCODE","showFormat":"TEXT","propertyCode":"WTS_1.0.0_workPermit_WorkPermit_workType"},
    {"key":"finishStatus","width":120,"namekey":"完成状态","isHidden":false,"showType":"SELECTCOMP","columnType":"SYSTEMCODE","showFormat":"TEXT","propertyCode":"WTS_1.0.0_workPermit_WorkPermit_finishStatus"},
    {"key":"startTime","width":150,"namekey":"开始时间","isHidden":false,"showType":"DATETIME","columnType":"DATETIME","showFormat":"YMD_HMS","propertyCode":"WTS_1.0.0_workPermit_WorkPermit_startTime"},
    {"key":"endTime","width":150,"namekey":"结束时间","isHidden":false,"showType":"DATETIME","columnType":"DATETIME","showFormat":"YMD_HMS","propertyCode":"WTS_1.0.0_workPermit_WorkPermit_endTime"},
    {"key":"id","width":120,"namekey":"id","isHidden":true,"showType":"TEXTFIELD","columnType":"TEXT","showFormat":"TEXT"}
  ],
  "buttons": []
}
$json$;
    data_grid_json_is_oid boolean;
BEGIN
    SELECT udt_name = 'oid'
      INTO data_grid_json_is_oid
      FROM information_schema.columns
     WHERE table_schema = 'public'
       AND table_name = 'runtime_data_grid'
       AND column_name = 'data_grid_json';

    IF COALESCE(data_grid_json_is_oid, false) THEN
        INSERT INTO public.runtime_data_grid (
            code, ec_env, version, valid, entity_code, module_code, data_grid_json,
            proj_flag, operate_name, permission_code, is_permission, data_grid_type,
            full_config, data_grid_name, config, view_code, name
        )
        VALUES (
            'WTS_1.0.0_workPermit_workPermitList',
            'product',
            0,
            true,
            'WTS_1.0.0_workPermit',
            'WTS_1.0.0',
            lo_from_bytea(0, convert_to(grid_payload, 'UTF8')),
            false,
            NULL,
            'WTS_1.0.0_workPermit_workPermitList',
            false,
            0,
            lo_from_bytea(0, convert_to(grid_payload, 'UTF8')),
            'WTS_1.0.0_workPermit_workPermitList',
            lo_from_bytea(0, convert_to(grid_payload, 'UTF8')),
            'WTS_1.0.0_workPermit_workPermitList',
            'workPermitList'
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
            'WTS_1.0.0_workPermit_workPermitList',
            'product',
            0,
            true,
            'WTS_1.0.0_workPermit',
            'WTS_1.0.0',
            grid_payload,
            false,
            NULL,
            'WTS_1.0.0_workPermit_workPermitList',
            false,
            0,
            grid_payload,
            'WTS_1.0.0_workPermit_workPermitList',
            grid_payload,
            'WTS_1.0.0_workPermit_workPermitList',
            'workPermitList'
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
        'WTS_1.0.0_workPermit_workPermitList',
        'product',
        0,
        1,
        'WTS_1.0.0_workPermit',
        'WTS_1.0.0',
        grid_payload,
        0,
        NULL,
        'WTS_1.0.0_workPermit_workPermitList',
        0,
        0,
        grid_payload,
        'WTS_1.0.0_workPermit_workPermitList',
        grid_payload,
        'WTS_1.0.0_workPermit_workPermitList',
        'workPermitList'
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

DO $$
DECLARE
    payload_udt text;
BEGIN
    SELECT udt_name
      INTO payload_udt
      FROM information_schema.columns
     WHERE table_schema = 'public'
       AND table_name = 'wts_work_permits'
       AND column_name = 'payload';

    IF payload_udt IN ('text', 'varchar') THEN
        WITH target AS (
            SELECT p.id AS permit_id,
                   t.id AS ticket_id,
                   t.work_type
              FROM public.wts_work_permits p
              LEFT JOIN public.wts_work_tickets t
                ON t.ticket_no = p.ticket_no
               AND COALESCE(t.valid, true) = true
             WHERE p.payload IS NOT NULL
               AND p.payload !~ '^\s*\{'
        )
        UPDATE public.wts_work_permits p
           SET payload = CASE
               WHEN target.ticket_id IS NULL THEN '{"workTickets":{}}'
               ELSE json_build_object(
                        'workTickets',
                        json_build_object(
                            COALESCE(NULLIF(regexp_replace(target.work_type, '^.*/', ''), ''), 'ticket'),
                            target.ticket_id
                        )
                    )::text
               END
          FROM target
         WHERE p.id = target.permit_id;
    END IF;
END $$;

SELECT public.adp_convert_text_lob_column_to_oid('wts_work_permits', 'payload');
