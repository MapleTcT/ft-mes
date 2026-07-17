-- PATROL hidden-danger persistence and EAM risk-record compatibility.
--
-- PATROL 6.0.4.0 delegates abnormal findings to SESH. The recovered target does
-- not include SESH, while EAM already projects the same SES_HRM_RISKHANDLES
-- table. Restore that source-backed projection and the columns required by the
-- EAM model so PATROL can create an auditable pending record without pretending
-- that the full SESH governance workflow is installed.

\set ON_ERROR_STOP on

BEGIN;

ALTER TABLE public.ses_hrm_riskhandles
    ADD COLUMN IF NOT EXISTS modify_time timestamp without time zone,
    ADD COLUMN IF NOT EXISTS create_staff_id bigint,
    ADD COLUMN IF NOT EXISTS modify_staff_id bigint,
    ADD COLUMN IF NOT EXISTS create_department_id bigint,
    ADD COLUMN IF NOT EXISTS create_position_id bigint,
    ADD COLUMN IF NOT EXISTS find_time timestamp without time zone,
    ADD COLUMN IF NOT EXISTS numberparama numeric,
    ADD COLUMN IF NOT EXISTS numberparamb numeric,
    ADD COLUMN IF NOT EXISTS numberparamc numeric,
    ADD COLUMN IF NOT EXISTS numberparamd numeric,
    ADD COLUMN IF NOT EXISTS numberparame numeric,
    ADD COLUMN IF NOT EXISTS numberparamf numeric,
    ADD COLUMN IF NOT EXISTS numberparamg numeric,
    ADD COLUMN IF NOT EXISTS numberparamh numeric,
    ADD COLUMN IF NOT EXISTS numberparami numeric,
    ADD COLUMN IF NOT EXISTS numberparamj numeric,
    ADD COLUMN IF NOT EXISTS numberparamk numeric,
    ADD COLUMN IF NOT EXISTS numberparaml numeric,
    ADD COLUMN IF NOT EXISTS dateparama timestamp without time zone,
    ADD COLUMN IF NOT EXISTS dateparamb timestamp without time zone,
    ADD COLUMN IF NOT EXISTS dateparamc timestamp without time zone,
    ADD COLUMN IF NOT EXISTS dateparamd timestamp without time zone,
    ADD COLUMN IF NOT EXISTS dateparame timestamp without time zone,
    ADD COLUMN IF NOT EXISTS dateparamf timestamp without time zone,
    ADD COLUMN IF NOT EXISTS dateparamg timestamp without time zone,
    ADD COLUMN IF NOT EXISTS dateparamh timestamp without time zone,
    ADD COLUMN IF NOT EXISTS dateparami timestamp without time zone,
    ADD COLUMN IF NOT EXISTS dateparamj timestamp without time zone,
    ADD COLUMN IF NOT EXISTS dateparamk timestamp without time zone,
    ADD COLUMN IF NOT EXISTS dateparaml timestamp without time zone,
    ADD COLUMN IF NOT EXISTS dateparamm timestamp without time zone,
    ADD COLUMN IF NOT EXISTS dateparamn timestamp without time zone,
    ADD COLUMN IF NOT EXISTS dateparamo timestamp without time zone,
    ADD COLUMN IF NOT EXISTS dateparamp timestamp without time zone,
    ADD COLUMN IF NOT EXISTS scparama varchar(2000),
    ADD COLUMN IF NOT EXISTS scparamb varchar(2000),
    ADD COLUMN IF NOT EXISTS scparamc varchar(2000),
    ADD COLUMN IF NOT EXISTS scparamd varchar(2000),
    ADD COLUMN IF NOT EXISTS scparame varchar(2000),
    ADD COLUMN IF NOT EXISTS scparamf varchar(2000),
    ADD COLUMN IF NOT EXISTS scparamg varchar(2000),
    ADD COLUMN IF NOT EXISTS scparamh varchar(2000),
    ADD COLUMN IF NOT EXISTS objparama bigint,
    ADD COLUMN IF NOT EXISTS objparamb bigint,
    ADD COLUMN IF NOT EXISTS objparamc bigint,
    ADD COLUMN IF NOT EXISTS objparamd bigint,
    ADD COLUMN IF NOT EXISTS objparame bigint,
    ADD COLUMN IF NOT EXISTS objparamf bigint,
    ADD COLUMN IF NOT EXISTS objparamg bigint,
    ADD COLUMN IF NOT EXISTS objparamh bigint,
    ADD COLUMN IF NOT EXISTS charparama varchar(2000),
    ADD COLUMN IF NOT EXISTS charparamb varchar(2000),
    ADD COLUMN IF NOT EXISTS charparamc varchar(2000),
    ADD COLUMN IF NOT EXISTS charparamd varchar(2000),
    ADD COLUMN IF NOT EXISTS charparame varchar(2000),
    ADD COLUMN IF NOT EXISTS charparamf varchar(2000),
    ADD COLUMN IF NOT EXISTS charparamg varchar(2000),
    ADD COLUMN IF NOT EXISTS charparamh varchar(2000),
    ADD COLUMN IF NOT EXISTS charparami varchar(2000),
    ADD COLUMN IF NOT EXISTS charparamj varchar(2000);

ALTER TABLE public.ses_hrm_riskhandles
    ALTER COLUMN valid SET DEFAULT 1,
    ALTER COLUMN status SET DEFAULT 0,
    ALTER COLUMN version SET DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_ses_hrm_riskhandles_pending
    ON public.ses_hrm_riskhandles (cid, valid, status);
CREATE INDEX IF NOT EXISTS idx_ses_hrm_riskhandles_eam_info
    ON public.ses_hrm_riskhandles (eam_info);

-- The recovered i18n runtime maps I18nResourcePO.modifier to java.util.Date.
-- Older PostgreSQL bootstrap scripts created the column as varchar, which only
-- stayed hidden while every value was NULL. Convert that legacy shape before
-- adding SESH translations; non-date audit strings cannot be consumed by the
-- runtime and are therefore normalized to NULL during the one-time repair.
DO $$
DECLARE
    modifier_data_type text;
BEGIN
    SELECT data_type
      INTO modifier_data_type
      FROM information_schema.columns
     WHERE table_schema = 'public'
       AND table_name = 'supfusion_i18n_resource'
       AND column_name = 'modifier';

    IF modifier_data_type IN ('character varying', 'character', 'text') THEN
        EXECUTE $migration$
            ALTER TABLE public.supfusion_i18n_resource
                ALTER COLUMN modifier TYPE timestamp without time zone
                USING CASE
                    WHEN modifier IS NULL OR btrim(modifier) = '' THEN NULL
                    WHEN btrim(modifier) ~ '^\d{4}-\d{2}-\d{2}([ T]\d{2}:\d{2}(:\d{2}(\.\d{1,6})?)?)?$'
                        THEN btrim(modifier)::timestamp without time zone
                    ELSE NULL
                END
        $migration$;
    END IF;
END
$$;

-- Source evidence recovered from the SESH runtime i18n bundle:
--   SESHRM.systemEntityname.randon1570600798462 = 隐患来源
--   SESHRM.systemCodevalue.randon1589806103444 = 巡检
-- PATROL writes SESHRM_riskResource/005 for inspection-originated findings,
-- and EAM resolves that value through SystemCodeService while rendering the
-- risk ledger. Restore the dependency even when the full SESH module is not
-- installed so the shared PATROL/EAM path remains source-compatible.
WITH source(i18n_key, i18n_value, langu_code, id) AS (
    VALUES
        ('SESHRM.systemEntityname.randon1570600798462', '隐患来源', 'zh_CN', 9186000000000101::bigint),
        ('SESHRM.systemCodevalue.randon1589806103444', '巡检', 'zh_CN', 9186000000000102::bigint),
        ('SESHRM.systemEntityname.randon1570600798462', 'Source of hidden danger', 'en_US', 9186000000000103::bigint),
        ('SESHRM.systemCodevalue.randon1589806103444', 'On-Site Inspection', 'en_US', 9186000000000104::bigint)
)
UPDATE public.supfusion_i18n_resource target
   SET i18n_value = source.i18n_value,
       module_code = 'SESHRM',
       module_version_code = 'SESHRM202112161002',
       valid = '1',
       modifier = CURRENT_TIMESTAMP,
       modify_time = CURRENT_TIMESTAMP,
       modify_staff_id = 1
  FROM source
 WHERE target.i18n_key = source.i18n_key
   AND target.langu_code = source.langu_code
   AND COALESCE(target.tenant_id, 'dt') = 'dt';

WITH source(i18n_key, i18n_value, langu_code, id) AS (
    VALUES
        ('SESHRM.systemEntityname.randon1570600798462', '隐患来源', 'zh_CN', 9186000000000101::bigint),
        ('SESHRM.systemCodevalue.randon1589806103444', '巡检', 'zh_CN', 9186000000000102::bigint),
        ('SESHRM.systemEntityname.randon1570600798462', 'Source of hidden danger', 'en_US', 9186000000000103::bigint),
        ('SESHRM.systemCodevalue.randon1589806103444', 'On-Site Inspection', 'en_US', 9186000000000104::bigint)
)
INSERT INTO public.supfusion_i18n_resource
    (id, i18n_key, i18n_value, langu_code, module_code, module_version_code,
     valid, tenant_id, creator, create_time, create_staff_id, modifier,
     modify_time, modify_staff_id)
SELECT
    source.id,
    source.i18n_key,
    source.i18n_value,
    source.langu_code,
    'SESHRM',
    'SESHRM202112161002',
    '1',
    'dt',
    'system',
    CURRENT_TIMESTAMP,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    1
  FROM source
 WHERE NOT EXISTS (
     SELECT 1
       FROM public.supfusion_i18n_resource current_row
      WHERE current_row.i18n_key = source.i18n_key
        AND current_row.langu_code = source.langu_code
        AND COALESCE(current_row.tenant_id, 'dt') = 'dt'
 );

UPDATE public.sys_entity
   SET row_version = 0,
       type = 'list',
       name = 'SESHRM.systemEntityname.randon1570600798462',
       display_name = '隐患来源',
       module_id = 'SESHRM',
       cid = 1000,
       valid = 1,
       multi_flag = 0,
       sys_default = 0,
       memo = 'PATROL/EAM shared hidden-danger compatibility',
       source = 'legacy',
       modify_time = CURRENT_TIMESTAMP
 WHERE code = 'SESHRM_riskResource';

INSERT INTO public.sys_entity
    (id, row_version, type, code, name, display_name, module_id, cid, valid,
     multi_flag, sys_default, memo, source)
SELECT
    9186000000000001,
    0,
    'list',
    'SESHRM_riskResource',
    'SESHRM.systemEntityname.randon1570600798462',
    '隐患来源',
    'SESHRM',
    1000,
    1,
    0,
    0,
    'PATROL/EAM shared hidden-danger compatibility',
    'legacy'
WHERE NOT EXISTS (
    SELECT 1
      FROM public.sys_entity
     WHERE code = 'SESHRM_riskResource'
)
ON CONFLICT (id) DO UPDATE SET
    row_version = EXCLUDED.row_version,
    type = EXCLUDED.type,
    code = EXCLUDED.code,
    name = EXCLUDED.name,
    display_name = EXCLUDED.display_name,
    module_id = EXCLUDED.module_id,
    cid = EXCLUDED.cid,
    valid = EXCLUDED.valid,
    multi_flag = EXCLUDED.multi_flag,
    sys_default = EXCLUDED.sys_default,
    memo = EXCLUDED.memo,
    source = EXCLUDED.source,
    modify_time = CURRENT_TIMESTAMP;

UPDATE public.sys_code
   SET row_version = 0,
       type = 'list',
       name = 'SESHRM.systemCodevalue.randon1589806103444',
       display_name = '巡检',
       cid = 1000,
       valid = 1,
       leaf = 1,
       default_flag = 0,
       full_path = 'SESHRM_riskResource/005',
       full_path_name = '巡检',
       parent_id = NULL,
       parent_name = 'SESHRM.systemEntityname.randon1570600798462',
       lay_no = 1,
       sort = 5,
       memo = 'PATROL inspection source',
       modify_time = CURRENT_TIMESTAMP
 WHERE entity_code = 'SESHRM_riskResource'
   AND code = '005';

INSERT INTO public.sys_code
    (id, row_version, type, code, entity_code, name, display_name, cid, valid,
     leaf, default_flag, full_path, full_path_name, parent_id, parent_name,
     lay_no, lay_rec, seq_id, sort, des_a, des_b, des_c, memo)
SELECT
    9186000000000002,
    0,
    'list',
    '005',
    'SESHRM_riskResource',
    'SESHRM.systemCodevalue.randon1589806103444',
    '巡检',
    1000,
    1,
    1,
    0,
    'SESHRM_riskResource/005',
    '巡检',
    NULL,
    'SESHRM.systemEntityname.randon1570600798462',
    1,
    '9186000000000002',
    NULL,
    5,
    '',
    '',
    '',
    'PATROL inspection source'
WHERE NOT EXISTS (
    SELECT 1
      FROM public.sys_code
     WHERE entity_code = 'SESHRM_riskResource'
       AND code = '005'
)
ON CONFLICT (id) DO UPDATE SET
    row_version = EXCLUDED.row_version,
    type = EXCLUDED.type,
    code = EXCLUDED.code,
    entity_code = EXCLUDED.entity_code,
    name = EXCLUDED.name,
    display_name = EXCLUDED.display_name,
    cid = EXCLUDED.cid,
    valid = EXCLUDED.valid,
    leaf = EXCLUDED.leaf,
    default_flag = EXCLUDED.default_flag,
    full_path = EXCLUDED.full_path,
    full_path_name = EXCLUDED.full_path_name,
    parent_id = EXCLUDED.parent_id,
    parent_name = EXCLUDED.parent_name,
    lay_no = EXCLUDED.lay_no,
    lay_rec = EXCLUDED.lay_rec,
    seq_id = EXCLUDED.seq_id,
    sort = EXCLUDED.sort,
    des_a = EXCLUDED.des_a,
    des_b = EXCLUDED.des_b,
    des_c = EXCLUDED.des_c,
    memo = EXCLUDED.memo,
    modify_time = CURRENT_TIMESTAMP;

INSERT INTO public.ec_sql
    (code, ec_env, version, proj_flag, data_grid_code, view_code, type, query_sql)
VALUES
    (
        'EAM_1.0.0_businessConfig_riskRecord_dg1578550214154_3',
        'product',
        0,
        0,
        'EAM_1.0.0_businessConfig_riskRecorddg1578550214154',
        'EAM_1.0.0_businessConfig_riskRecord',
        3,
        'SELECT COUNT(*) count FROM '
    ),
    (
        'EAM_1.0.0_businessConfig_riskRecord_dg1578550214154_6',
        'product',
        0,
        0,
        'EAM_1.0.0_businessConfig_riskRecorddg1578550214154',
        'EAM_1.0.0_businessConfig_riskRecord',
        6,
        'SELECT "riskHandle".ID AS "id","riskHandle".VERSION AS "version","riskHandle".CID AS "cid","riskHandle".TABLE_NO AS "riskTableNo","finder".NAME AS "finder.name","riskHandle".FIND_TIME AS "findTime","riskHandle".RISK_CONTENT AS "riskContent","riskHandle".RISK_SOURCE AS "riskSource","riskHandle".RISK_LEVEL AS "riskLevel","riskHandle".EAM_INFO AS "eamInfo","finder".ID AS "finder.id","riskHandle".VALID AS "valid" FROM SES_HRM_RISKHANDLES "riskHandle" LEFT OUTER JOIN base_staff "finder" ON "finder".ID = "riskHandle".FINDER'
    )
ON CONFLICT (code) DO UPDATE SET
    ec_env = EXCLUDED.ec_env,
    version = GREATEST(COALESCE(public.ec_sql.version, 0), EXCLUDED.version),
    proj_flag = EXCLUDED.proj_flag,
    data_grid_code = EXCLUDED.data_grid_code,
    view_code = EXCLUDED.view_code,
    type = EXCLUDED.type,
    query_sql = EXCLUDED.query_sql;

INSERT INTO public.runtime_sql
    (code, ec_env, version, proj_flag, data_grid_code, view_code, type, query_sql)
VALUES
    (
        'EAM_1.0.0_businessConfig_riskRecord_dg1578550214154_3',
        'product',
        0,
        false,
        'EAM_1.0.0_businessConfig_riskRecorddg1578550214154',
        'EAM_1.0.0_businessConfig_riskRecord',
        3,
        'SELECT COUNT(*) count FROM '
    ),
    (
        'EAM_1.0.0_businessConfig_riskRecord_dg1578550214154_6',
        'product',
        0,
        false,
        'EAM_1.0.0_businessConfig_riskRecorddg1578550214154',
        'EAM_1.0.0_businessConfig_riskRecord',
        6,
        'SELECT "riskHandle".ID AS "id","riskHandle".VERSION AS "version","riskHandle".CID AS "cid","riskHandle".TABLE_NO AS "riskTableNo","finder".NAME AS "finder.name","riskHandle".FIND_TIME AS "findTime","riskHandle".RISK_CONTENT AS "riskContent","riskHandle".RISK_SOURCE AS "riskSource","riskHandle".RISK_LEVEL AS "riskLevel","riskHandle".EAM_INFO AS "eamInfo","finder".ID AS "finder.id","riskHandle".VALID AS "valid" FROM SES_HRM_RISKHANDLES "riskHandle" LEFT OUTER JOIN base_staff "finder" ON "finder".ID = "riskHandle".FINDER'
    )
ON CONFLICT (code) DO UPDATE SET
    ec_env = EXCLUDED.ec_env,
    version = GREATEST(COALESCE(public.runtime_sql.version, 0), EXCLUDED.version),
    proj_flag = EXCLUDED.proj_flag,
    data_grid_code = EXCLUDED.data_grid_code,
    view_code = EXCLUDED.view_code,
    type = EXCLUDED.type,
    query_sql = EXCLUDED.query_sql;

DO $$
DECLARE
    ec_view_count integer;
    runtime_view_count integer;
BEGIN
    UPDATE public.ec_view
       SET extra_view = 'EAM_1.0.0_businessConfig_riskRecord'
     WHERE code = 'EAM_1.0.0_businessConfig_riskRecord';
    GET DIAGNOSTICS ec_view_count = ROW_COUNT;

    UPDATE public.runtime_view
       SET extra_view = 'EAM_1.0.0_businessConfig_riskRecord'
     WHERE code = 'EAM_1.0.0_businessConfig_riskRecord';
    GET DIAGNOSTICS runtime_view_count = ROW_COUNT;

    IF ec_view_count <> 1 OR runtime_view_count <> 1 THEN
        RAISE EXCEPTION
            'EAM risk-record view metadata is missing: ec=%, runtime=%',
            ec_view_count,
            runtime_view_count;
    END IF;
END $$;

INSERT INTO public.ec_extra_view
    (code, ec_env, version, view_json, proj_flag, view_code)
VALUES
    (
        'EAM_1.0.0_businessConfig_riskRecord',
        'product',
        14,
        $eam_risk_view${"components":[{"components":[{"components":[{"components":[{"tabViewIndex":0,"isborder":0,"components":[{"isborder":0,"components":[{"isSuperTable":false,"iscontrol":false,"nullable":false,"isOfficeHandSign":false,"isRevision":false,"isFirstLoad":false,"type":"layoutDatagrid","readonly":0,"ptPageInit_es5":"var eamId = window.parent.ReactAPI.getParamsInRequestUrl().id;\nReactAPI.getComponentAPI(\u0022SupDataGrid\u0022).APIs(\u0022EAM_1.0.0_businessConfig_riskRecorddg1578550214154\u0022).refreshDataByRequst({\n\ttype: \u0022post\u0022,\n\turl: \u0022/msService/EAM/businessConfig/riskHandle/data-dg1578550214154?datagridCode=EAM_1.0.0_businessConfig_riskRecorddg1578550214154\u0026id=-1\u0022,\n\tparam: {\n\t\tcustomCondition: { eamId: eamId }\n\t}\n});","isdbcustom":false,"convertPdfOnLine":true,"regionType":"LISTPT","getRequireData":"/baseService/excel/getRequireDataByModelcode","modelCode":"EAM_1.0.0_businessConfig_RiskHandle","complex":true,"rowspan":1,"downloadXls":"/msService/EAM/businessConfig/riskHandle/downloadXls","showType":"DATAGRID","isgroup":false,"isCreateNew":false,"renderOver":"showView(\u0027EAM_1.0.0_businessConfig_riskRecorddg1578550214154\u0027,\u0027riskTableNo\u0027,\u0027risk\u0027)","firstTd":1,"officeNotNull":false,"cellCode":"cell_1578550338819_4902","importMainXls":"/msService/EAM/businessConfig/riskHandle/importMainXls","isTransCondition":false,"nodeExpanded":false,"isOfficeSign":false,"columnType":"DATAGRID","isTitle":true,"dataGridName":"dg1578550214154","hasFastQuery":false,"fields":[{"precisionHasChanged":false,"onlyLeaf":false,"code":"EAM_1.0.0_businessConfig_riskRecorddg1578550214154_LISTPT_OTHER_EAM_1.0.0_businessConfig_RiskHandle_riskTableNo","isTotal":false,"isOrderBy":false,"seniorSystemcode":false,"displayName":"EAM.businessConfig.RiskHandle.table_no","showFormatHasChanged":false,"isCount":false,"dbColumnType":"TEXT","ecEnv":"product","propertyCode":"EAM_1.0.0_businessConfig_RiskHandle_riskTableNo","regionType":"LISTPT","layRec":"riskTableNo","modelCode":"EAM_1.0.0_businessConfig_RiskHandle","namekey":"EAM.businessConfig.RiskHandle.table_no","showType":"TEXTFIELD","key":"riskTableNo","showFormat":"TEXT","entityCode":"EAM_1.0.0_businessConfig","moduleCode":"EAM_1.0.0","cellCode":"cell_1578562700653_92","fullPropertyCode":"EAM_1.0.0_businessConfig_RiskHandle_riskTableNo","isHidden":false,"columnType":"TEXT","pc":"__pc__=RUFNXzEuMC4wX2J1c2luZXNzQ29uZmlnX3Jpc2tSZWNvcmRfc2VsZnw_","width":130,"openPending":false,"isCustom":false,"textalign":"left","multable":false,"maxLength":2000,"columnName":"TABLE_NO"},{"ass":{"tar":"base_staff_id","org":"EAM_1.0.0_businessConfig_RiskHandle_finder"},"assPropertyName":"id","precisionHasChanged":true,"onlyLeaf":null,"code":"EAM_1.0.0_businessConfig_riskRecorddg1578550214154_LISTPT_OTHER_EAM_1.0.0_businessConfig_RiskHandle_finder_base_staff_name","isTotal":false,"isOrderBy":false,"seniorSystemcode":false,"displayName":"EAM.businessConfig.RiskHandle.finder","showFormatHasChanged":false,"isCount":false,"dbColumnType":"TEXT","ecEnv":"product","propertyCode":"EAM_1.0.0_businessConfig_RiskHandle_finder||base_staff_name","regionType":"LISTPT","modelCode":"sysbase_1.0_staff_base_staff","layRec":"base_staff,ID,SES_HRM_RISKHANDLES,FINDER-name","namekey":"EAM.businessConfig.RiskHandle.finder","showType":"TEXTFIELD","key":"finder.name","isLink":false,"showFormat":"TEXT","entityCode":"EAM_1.0.0_businessConfig","moduleCode":"EAM_1.0.0","cellCode":"cell_1578550344066_8231","fullPropertyCode":"EAM_1.0.0_businessConfig_RiskHandle_finder||base_staff_name","isHidden":false,"columnType":"TEXT","pc":"__pc__=RUFNXzEuMC4wX2J1c2luZXNzQ29uZmlnX3Jpc2tSZWNvcmRfc2VsZnw_","textalign":"left","width":100,"openPending":false,"isCustom":false,"multable":false,"maxLength":80,"columnName":"NAME"},{"precisionHasChanged":false,"onlyLeaf":false,"code":"EAM_1.0.0_businessConfig_riskRecorddg1578550214154_LISTPT_OTHER_EAM_1.0.0_businessConfig_RiskHandle_findTime","isTotal":false,"isOrderBy":false,"seniorSystemcode":false,"displayName":"EAM.businessConfig.RiskHandle.findTime","showFormatHasChanged":false,"isCount":false,"dbColumnType":"DATETIME","ecEnv":"product","propertyCode":"EAM_1.0.0_businessConfig_RiskHandle_findTime","regionType":"LISTPT","layRec":"findTime","modelCode":"EAM_1.0.0_businessConfig_RiskHandle","namekey":"EAM.businessConfig.RiskHandle.findTime","showType":"DATETIME","key":"findTime","showFormat":"YMD_HMS","entityCode":"EAM_1.0.0_businessConfig","moduleCode":"EAM_1.0.0","cellCode":"cell_1578550344258_7655","fullPropertyCode":"EAM_1.0.0_businessConfig_RiskHandle_findTime","isHidden":false,"columnType":"DATETIME","pc":"__pc__=RUFNXzEuMC4wX2J1c2luZXNzQ29uZmlnX3Jpc2tSZWNvcmRfc2VsZnw_","width":150,"openPending":false,"isCustom":false,"textalign":"center","multable":false,"maxLength":null,"columnName":"FIND_TIME"},{"precisionHasChanged":false,"onlyLeaf":false,"code":"EAM_1.0.0_businessConfig_riskRecorddg1578550214154_LISTPT_OTHER_EAM_1.0.0_businessConfig_RiskHandle_riskContent","isTotal":false,"isOrderBy":false,"seniorSystemcode":false,"displayName":"EAM.businessConfig.RiskHandle.riskContent","showFormatHasChanged":false,"isCount":false,"dbColumnType":"TEXT","ecEnv":"product","propertyCode":"EAM_1.0.0_businessConfig_RiskHandle_riskContent","regionType":"LISTPT","layRec":"riskContent","modelCode":"EAM_1.0.0_businessConfig_RiskHandle","namekey":"EAM.businessConfig.RiskHandle.riskContent","showType":"TEXTFIELD","key":"riskContent","showFormat":"TEXT","entityCode":"EAM_1.0.0_businessConfig","moduleCode":"EAM_1.0.0","cellCode":"cell_1578550344451_491","fullPropertyCode":"EAM_1.0.0_businessConfig_RiskHandle_riskContent","isHidden":false,"columnType":"TEXT","pc":"__pc__=RUFNXzEuMC4wX2J1c2luZXNzQ29uZmlnX3Jpc2tSZWNvcmRfc2VsZnw_","width":300,"openPending":false,"isCustom":false,"textalign":"left","multable":false,"maxLength":255,"columnName":"RISK_CONTENT"},{"precisionHasChanged":false,"onlyLeaf":false,"code":"EAM_1.0.0_businessConfig_riskRecorddg1578550214154_LISTPT_OTHER_EAM_1.0.0_businessConfig_RiskHandle_riskSource","isTotal":false,"isOrderBy":false,"seniorSystemcode":false,"displayName":"EAM.businessConfig.RiskHandle.riskSource","showFormatHasChanged":false,"isCount":false,"dbColumnType":"TEXT","ecEnv":"product","propertyCode":"EAM_1.0.0_businessConfig_RiskHandle_riskSource","regionType":"LISTPT","layRec":"riskSource","modelCode":"EAM_1.0.0_businessConfig_RiskHandle","namekey":"EAM.businessConfig.RiskHandle.riskSource","showType":"TEXTFIELD","key":"riskSource","showFormat":"TEXT","entityCode":"EAM_1.0.0_businessConfig","moduleCode":"EAM_1.0.0","cellCode":"cell_1578550344642_781","fullPropertyCode":"EAM_1.0.0_businessConfig_RiskHandle_riskSource","isHidden":false,"columnType":"TEXT","pc":"__pc__=RUFNXzEuMC4wX2J1c2luZXNzQ29uZmlnX3Jpc2tSZWNvcmRfc2VsZnw_","width":80,"openPending":false,"isCustom":false,"textalign":"left","multable":false,"maxLength":2000,"columnName":"RISK_SOURCE"},{"precisionHasChanged":false,"onlyLeaf":false,"code":"EAM_1.0.0_businessConfig_riskRecorddg1578550214154_LISTPT_OTHER_EAM_1.0.0_businessConfig_RiskHandle_riskLevel","isTotal":false,"isOrderBy":false,"seniorSystemcode":false,"displayName":"EAM.businessConfig.RiskHandle.riskLevel","showFormatHasChanged":false,"isCount":false,"dbColumnType":"TEXT","ecEnv":"product","propertyCode":"EAM_1.0.0_businessConfig_RiskHandle_riskLevel","regionType":"LISTPT","layRec":"riskLevel","modelCode":"EAM_1.0.0_businessConfig_RiskHandle","namekey":"EAM.businessConfig.RiskHandle.riskLevel","showType":"TEXTFIELD","key":"riskLevel","showFormat":"TEXT","entityCode":"EAM_1.0.0_businessConfig","moduleCode":"EAM_1.0.0","cellCode":"cell_1578552601206_3112","fullPropertyCode":"EAM_1.0.0_businessConfig_RiskHandle_riskLevel","isHidden":false,"columnType":"TEXT","pc":"__pc__=RUFNXzEuMC4wX2J1c2luZXNzQ29uZmlnX3Jpc2tSZWNvcmRfc2VsZnw_","width":80,"openPending":false,"isCustom":false,"textalign":"left","multable":false,"maxLength":2000,"columnName":"RISK_LEVEL"},{"precisionHasChanged":false,"onlyLeaf":false,"code":"EAM_1.0.0_businessConfig_riskRecorddg1578550214154_LISTPT_OTHER_EAM_1.0.0_businessConfig_RiskHandle_eamInfo","isTotal":false,"isOrderBy":false,"seniorSystemcode":false,"displayName":"EAM.businessConfig.RiskHandle.eamInfo","showFormatHasChanged":false,"isCount":false,"dbColumnType":"LONG","ecEnv":"product","minValue":"-2147483648","propertyCode":"EAM_1.0.0_businessConfig_RiskHandle_eamInfo","regionType":"LISTPT","layRec":"eamInfo","modelCode":"EAM_1.0.0_businessConfig_RiskHandle","namekey":"EAM.businessConfig.RiskHandle.eamInfo","showType":"TEXTFIELD","key":"eamInfo","showFormat":"TEXT","entityCode":"EAM_1.0.0_businessConfig","moduleCode":"EAM_1.0.0","maxValue":"2147483647","cellCode":"cell_1578550344834_1134","fullPropertyCode":"EAM_1.0.0_businessConfig_RiskHandle_eamInfo","isHidden":true,"columnType":"INTEGER","pc":"__pc__=RUFNXzEuMC4wX2J1c2luZXNzQ29uZmlnX3Jpc2tSZWNvcmRfc2VsZnw_","width":100,"openPending":false,"isCustom":false,"textalign":"right","multable":false,"columnName":"EAM_INFO"},{"precisionHasChanged":true,"onlyLeaf":false,"code":"EAM_1.0.0_businessConfig_riskRecorddg1578550214154_LISTPT_OTHER_EAM_1.0.0_businessConfig_RiskHandle_id","isTotal":false,"isOrderBy":false,"seniorSystemcode":false,"displayName":"EAM.propertyshowName.randon1578550439513.flag","showFormatHasChanged":false,"isCount":false,"dbColumnType":"LONG","ecEnv":"product","minValue":"-9223372036854775808","propertyCode":"EAM_1.0.0_businessConfig_RiskHandle_id","regionType":"LISTPT","layRec":"id","modelCode":"EAM_1.0.0_businessConfig_RiskHandle","namekey":"EAM.propertyshowName.randon1578550439513.flag","showType":"TEXTFIELD","key":"id","showFormat":"TEXT","entityCode":"EAM_1.0.0_businessConfig","moduleCode":"EAM_1.0.0","maxValue":"9223372036854775807","cellCode":"cell_1578550420514_5406","fullPropertyCode":"EAM_1.0.0_businessConfig_RiskHandle_id","isHidden":true,"columnType":"LONG","pc":"__pc__=RUFNXzEuMC4wX2J1c2luZXNzQ29uZmlnX3Jpc2tSZWNvcmRfc2VsZnw_","width":100,"openPending":false,"isCustom":false,"textalign":"right","multable":false,"columnName":"ID"},{"assPropertyName":"id","code":"EAM_1.0.0_businessConfig_riskRecorddg1578550214154_LISTPT_OTHER_EAM_1.0.0_businessConfig_RiskHandle_finder_base_staff_id_none","entityCode":"EAM_1.0.0_businessConfig","moduleCode":"EAM_1.0.0","displayName":"finder.id","cellCode":"cell_1584336830045_59","none":"hide","fullPropertyCode":"EAM_1.0.0_businessConfig_RiskHandle_finder||base_staff_id","isHidden":true,"ecEnv":"product","propertyCode":"EAM_1.0.0_businessConfig_RiskHandle_finder||base_staff_id","pc":"__pc__=RUFNXzEuMC4wX2J1c2luZXNzQ29uZmlnX3Jpc2tSZWNvcmRfc2VsZnw_","regionType":"LISTPT","namekey":"finder.id","name":"finder.id","showType":"TEXTFIELD","key":"finder.id"}],"isNoCopy":false,"isCheckBox":false,"acceptRevisions":false,"code":"EAM_1.0.0_businessConfig_riskRecorddg1578550214154","idPrefix":"RiskHandle_dg1578550214154","hideRevision":false,"title":null,"getRevisions":false,"ptPageInit":"var eamId = window.parent.ReactAPI.getParamsInRequestUrl().id;\n\tReactAPI.getComponentAPI(\u0022SupDataGrid\u0022)\n\t\t\t\t  .APIs(\u0022EAM_1.0.0_businessConfig_riskRecorddg1578550214154\u0022)\n\t\t\t\t  .refreshDataByRequst({\n\t\t\t\t\ttype: \u0022post\u0022,\n\t\t\t\t\turl: \u0022/msService/EAM/businessConfig/riskHandle/data-dg1578550214154?datagridCode=EAM_1.0.0_businessConfig_riskRecorddg1578550214154\u0026id=-1\u0022,\n\t\t\t\t\tparam: {\n\t\t\t\t\t\tcustomCondition:{eamId:eamId}\n\t\t\t\t\t}\n\t\t\t\t  });","colspan":1,"ecEnv":"product","showRevision":false,"colNum":6,"hideKey":"\u0027id\u0027,\u0027version\u0027,\u0027finder.id\u0027","isreadonly":0,"selectFirstRow":false,"officePrint":false,"downloadDoc":true,"buttons":[],"entityCode":"EAM_1.0.0_businessConfig","moduleCode":"EAM_1.0.0","autoresize":false,"DataGridCode":"EAM_1.0.0_businessConfig_riskRecorddg1578550214154","listPT":true,"renderOver_es5":"showView(\u0027EAM_1.0.0_businessConfig_riskRecorddg1578550214154\u0027, \u0027riskTableNo\u0027, \u0027risk\u0027);","queryUrl":"/EAM/businessConfig/riskHandle/data-dg1578550214154","saveTemplate":false,"isExportExcel":true,"targetModelCode":"EAM_1.0.0_businessConfig_RiskHandle","isTreeView":false,"openEmptyDoc":true,"multable":false}],"regionType":"LISTPT","colwidth":"13,20,13,20,13,20","cssstyle":null,"customSection":null,"colNum":6,"name":null,"sectionCode":null,"refcol":"\u003ctr style=\u0027border:none;height:0px; \u0027\u003e\u003ctd style=\u0027height:0px;border:none;width:13%\u0027\u003e\u003c/td\u003e\u003ctd style=\u0027height:0px;border:none;width:20%\u0027\u003e\u003c/td\u003e\u003ctd style=\u0027height:0px;border:none;width:13%\u0027\u003e\u003c/td\u003e\u003ctd style=\u0027height:0px;border:none;width:20%\u0027\u003e\u003c/td\u003e\u003ctd style=\u0027height:0px;border:none;width:13%\u0027\u003e\u003c/td\u003e\u003ctd style=\u0027height:0px;border:none;width:20%\u0027\u003e\u003c/td\u003e\u003c/tr\u003e","type":"layoutSection"}],"ratio_w":100,"isrightbottomfixed":false,"layoutContent":"datagrid","type":"layout","ranksType":"panel","isShow":true,"isreadonlyBak":0,"parlayoutname":"tabs-1","layoutmethod":"container","ptRealTimeLoad":1,"isreadonly":0,"layno":2,"layoutname":"row-2","islefttopfixed":false,"nums":1}],"isborder":0,"tabViewIndex":0,"isrightbottomfixed":false,"type":"layout","isreadonlyBak":0,"ptRealTimeLoad":1,"layoutmethod":"row","namekey":"ec.view.commoninfo","name":"常规信息","isreadonly":0,"layno":1,"id":null,"tabCode":"tab_0","layoutname":"tabs-1","islefttopfixed":false,"nums":1}],"layoutmethod":"tab","type":"layout"}],"layoutmethod":"column","type":"layout"}],"enableSimpleDealInfo":false,"title":"EAM.viewtitle.randon1578549638421","url":"/msService/EAM/businessConfig/riskHandle/riskRecord","dealInfoShow":false,"hasAttachment":false,"pageType":"EDIT","dealInfoGroup":"byTime","refCopy":{"isReference":false},"colNum":6,"layoutType":"classic","selfAdaption":false,"events":[]}$eam_risk_view$,
        0,
        'EAM_1.0.0_businessConfig_riskRecord'
    )
ON CONFLICT (code) DO UPDATE SET
    ec_env = EXCLUDED.ec_env,
    version = GREATEST(COALESCE(public.ec_extra_view.version, 0), EXCLUDED.version),
    view_json = EXCLUDED.view_json,
    proj_flag = EXCLUDED.proj_flag,
    view_code = EXCLUDED.view_code;

DO $$
DECLARE
    payload text := $eam_risk_view${"components":[{"components":[{"components":[{"components":[{"tabViewIndex":0,"isborder":0,"components":[{"isborder":0,"components":[{"isSuperTable":false,"iscontrol":false,"nullable":false,"isOfficeHandSign":false,"isRevision":false,"isFirstLoad":false,"type":"layoutDatagrid","readonly":0,"ptPageInit_es5":"var eamId = window.parent.ReactAPI.getParamsInRequestUrl().id;\nReactAPI.getComponentAPI(\u0022SupDataGrid\u0022).APIs(\u0022EAM_1.0.0_businessConfig_riskRecorddg1578550214154\u0022).refreshDataByRequst({\n\ttype: \u0022post\u0022,\n\turl: \u0022/msService/EAM/businessConfig/riskHandle/data-dg1578550214154?datagridCode=EAM_1.0.0_businessConfig_riskRecorddg1578550214154\u0026id=-1\u0022,\n\tparam: {\n\t\tcustomCondition: { eamId: eamId }\n\t}\n});","isdbcustom":false,"convertPdfOnLine":true,"regionType":"LISTPT","getRequireData":"/baseService/excel/getRequireDataByModelcode","modelCode":"EAM_1.0.0_businessConfig_RiskHandle","complex":true,"rowspan":1,"downloadXls":"/msService/EAM/businessConfig/riskHandle/downloadXls","showType":"DATAGRID","isgroup":false,"isCreateNew":false,"renderOver":"showView(\u0027EAM_1.0.0_businessConfig_riskRecorddg1578550214154\u0027,\u0027riskTableNo\u0027,\u0027risk\u0027)","firstTd":1,"officeNotNull":false,"cellCode":"cell_1578550338819_4902","importMainXls":"/msService/EAM/businessConfig/riskHandle/importMainXls","isTransCondition":false,"nodeExpanded":false,"isOfficeSign":false,"columnType":"DATAGRID","isTitle":true,"dataGridName":"dg1578550214154","hasFastQuery":false,"fields":[{"precisionHasChanged":false,"onlyLeaf":false,"code":"EAM_1.0.0_businessConfig_riskRecorddg1578550214154_LISTPT_OTHER_EAM_1.0.0_businessConfig_RiskHandle_riskTableNo","isTotal":false,"isOrderBy":false,"seniorSystemcode":false,"displayName":"EAM.businessConfig.RiskHandle.table_no","showFormatHasChanged":false,"isCount":false,"dbColumnType":"TEXT","ecEnv":"product","propertyCode":"EAM_1.0.0_businessConfig_RiskHandle_riskTableNo","regionType":"LISTPT","layRec":"riskTableNo","modelCode":"EAM_1.0.0_businessConfig_RiskHandle","namekey":"EAM.businessConfig.RiskHandle.table_no","showType":"TEXTFIELD","key":"riskTableNo","showFormat":"TEXT","entityCode":"EAM_1.0.0_businessConfig","moduleCode":"EAM_1.0.0","cellCode":"cell_1578562700653_92","fullPropertyCode":"EAM_1.0.0_businessConfig_RiskHandle_riskTableNo","isHidden":false,"columnType":"TEXT","pc":"__pc__=RUFNXzEuMC4wX2J1c2luZXNzQ29uZmlnX3Jpc2tSZWNvcmRfc2VsZnw_","width":130,"openPending":false,"isCustom":false,"textalign":"left","multable":false,"maxLength":2000,"columnName":"TABLE_NO"},{"ass":{"tar":"base_staff_id","org":"EAM_1.0.0_businessConfig_RiskHandle_finder"},"assPropertyName":"id","precisionHasChanged":true,"onlyLeaf":null,"code":"EAM_1.0.0_businessConfig_riskRecorddg1578550214154_LISTPT_OTHER_EAM_1.0.0_businessConfig_RiskHandle_finder_base_staff_name","isTotal":false,"isOrderBy":false,"seniorSystemcode":false,"displayName":"EAM.businessConfig.RiskHandle.finder","showFormatHasChanged":false,"isCount":false,"dbColumnType":"TEXT","ecEnv":"product","propertyCode":"EAM_1.0.0_businessConfig_RiskHandle_finder||base_staff_name","regionType":"LISTPT","modelCode":"sysbase_1.0_staff_base_staff","layRec":"base_staff,ID,SES_HRM_RISKHANDLES,FINDER-name","namekey":"EAM.businessConfig.RiskHandle.finder","showType":"TEXTFIELD","key":"finder.name","isLink":false,"showFormat":"TEXT","entityCode":"EAM_1.0.0_businessConfig","moduleCode":"EAM_1.0.0","cellCode":"cell_1578550344066_8231","fullPropertyCode":"EAM_1.0.0_businessConfig_RiskHandle_finder||base_staff_name","isHidden":false,"columnType":"TEXT","pc":"__pc__=RUFNXzEuMC4wX2J1c2luZXNzQ29uZmlnX3Jpc2tSZWNvcmRfc2VsZnw_","textalign":"left","width":100,"openPending":false,"isCustom":false,"multable":false,"maxLength":80,"columnName":"NAME"},{"precisionHasChanged":false,"onlyLeaf":false,"code":"EAM_1.0.0_businessConfig_riskRecorddg1578550214154_LISTPT_OTHER_EAM_1.0.0_businessConfig_RiskHandle_findTime","isTotal":false,"isOrderBy":false,"seniorSystemcode":false,"displayName":"EAM.businessConfig.RiskHandle.findTime","showFormatHasChanged":false,"isCount":false,"dbColumnType":"DATETIME","ecEnv":"product","propertyCode":"EAM_1.0.0_businessConfig_RiskHandle_findTime","regionType":"LISTPT","layRec":"findTime","modelCode":"EAM_1.0.0_businessConfig_RiskHandle","namekey":"EAM.businessConfig.RiskHandle.findTime","showType":"DATETIME","key":"findTime","showFormat":"YMD_HMS","entityCode":"EAM_1.0.0_businessConfig","moduleCode":"EAM_1.0.0","cellCode":"cell_1578550344258_7655","fullPropertyCode":"EAM_1.0.0_businessConfig_RiskHandle_findTime","isHidden":false,"columnType":"DATETIME","pc":"__pc__=RUFNXzEuMC4wX2J1c2luZXNzQ29uZmlnX3Jpc2tSZWNvcmRfc2VsZnw_","width":150,"openPending":false,"isCustom":false,"textalign":"center","multable":false,"maxLength":null,"columnName":"FIND_TIME"},{"precisionHasChanged":false,"onlyLeaf":false,"code":"EAM_1.0.0_businessConfig_riskRecorddg1578550214154_LISTPT_OTHER_EAM_1.0.0_businessConfig_RiskHandle_riskContent","isTotal":false,"isOrderBy":false,"seniorSystemcode":false,"displayName":"EAM.businessConfig.RiskHandle.riskContent","showFormatHasChanged":false,"isCount":false,"dbColumnType":"TEXT","ecEnv":"product","propertyCode":"EAM_1.0.0_businessConfig_RiskHandle_riskContent","regionType":"LISTPT","layRec":"riskContent","modelCode":"EAM_1.0.0_businessConfig_RiskHandle","namekey":"EAM.businessConfig.RiskHandle.riskContent","showType":"TEXTFIELD","key":"riskContent","showFormat":"TEXT","entityCode":"EAM_1.0.0_businessConfig","moduleCode":"EAM_1.0.0","cellCode":"cell_1578550344451_491","fullPropertyCode":"EAM_1.0.0_businessConfig_RiskHandle_riskContent","isHidden":false,"columnType":"TEXT","pc":"__pc__=RUFNXzEuMC4wX2J1c2luZXNzQ29uZmlnX3Jpc2tSZWNvcmRfc2VsZnw_","width":300,"openPending":false,"isCustom":false,"textalign":"left","multable":false,"maxLength":255,"columnName":"RISK_CONTENT"},{"precisionHasChanged":false,"onlyLeaf":false,"code":"EAM_1.0.0_businessConfig_riskRecorddg1578550214154_LISTPT_OTHER_EAM_1.0.0_businessConfig_RiskHandle_riskSource","isTotal":false,"isOrderBy":false,"seniorSystemcode":false,"displayName":"EAM.businessConfig.RiskHandle.riskSource","showFormatHasChanged":false,"isCount":false,"dbColumnType":"TEXT","ecEnv":"product","propertyCode":"EAM_1.0.0_businessConfig_RiskHandle_riskSource","regionType":"LISTPT","layRec":"riskSource","modelCode":"EAM_1.0.0_businessConfig_RiskHandle","namekey":"EAM.businessConfig.RiskHandle.riskSource","showType":"TEXTFIELD","key":"riskSource","showFormat":"TEXT","entityCode":"EAM_1.0.0_businessConfig","moduleCode":"EAM_1.0.0","cellCode":"cell_1578550344642_781","fullPropertyCode":"EAM_1.0.0_businessConfig_RiskHandle_riskSource","isHidden":false,"columnType":"TEXT","pc":"__pc__=RUFNXzEuMC4wX2J1c2luZXNzQ29uZmlnX3Jpc2tSZWNvcmRfc2VsZnw_","width":80,"openPending":false,"isCustom":false,"textalign":"left","multable":false,"maxLength":2000,"columnName":"RISK_SOURCE"},{"precisionHasChanged":false,"onlyLeaf":false,"code":"EAM_1.0.0_businessConfig_riskRecorddg1578550214154_LISTPT_OTHER_EAM_1.0.0_businessConfig_RiskHandle_riskLevel","isTotal":false,"isOrderBy":false,"seniorSystemcode":false,"displayName":"EAM.businessConfig.RiskHandle.riskLevel","showFormatHasChanged":false,"isCount":false,"dbColumnType":"TEXT","ecEnv":"product","propertyCode":"EAM_1.0.0_businessConfig_RiskHandle_riskLevel","regionType":"LISTPT","layRec":"riskLevel","modelCode":"EAM_1.0.0_businessConfig_RiskHandle","namekey":"EAM.businessConfig.RiskHandle.riskLevel","showType":"TEXTFIELD","key":"riskLevel","showFormat":"TEXT","entityCode":"EAM_1.0.0_businessConfig","moduleCode":"EAM_1.0.0","cellCode":"cell_1578552601206_3112","fullPropertyCode":"EAM_1.0.0_businessConfig_RiskHandle_riskLevel","isHidden":false,"columnType":"TEXT","pc":"__pc__=RUFNXzEuMC4wX2J1c2luZXNzQ29uZmlnX3Jpc2tSZWNvcmRfc2VsZnw_","width":80,"openPending":false,"isCustom":false,"textalign":"left","multable":false,"maxLength":2000,"columnName":"RISK_LEVEL"},{"precisionHasChanged":false,"onlyLeaf":false,"code":"EAM_1.0.0_businessConfig_riskRecorddg1578550214154_LISTPT_OTHER_EAM_1.0.0_businessConfig_RiskHandle_eamInfo","isTotal":false,"isOrderBy":false,"seniorSystemcode":false,"displayName":"EAM.businessConfig.RiskHandle.eamInfo","showFormatHasChanged":false,"isCount":false,"dbColumnType":"LONG","ecEnv":"product","minValue":"-2147483648","propertyCode":"EAM_1.0.0_businessConfig_RiskHandle_eamInfo","regionType":"LISTPT","layRec":"eamInfo","modelCode":"EAM_1.0.0_businessConfig_RiskHandle","namekey":"EAM.businessConfig.RiskHandle.eamInfo","showType":"TEXTFIELD","key":"eamInfo","showFormat":"TEXT","entityCode":"EAM_1.0.0_businessConfig","moduleCode":"EAM_1.0.0","maxValue":"2147483647","cellCode":"cell_1578550344834_1134","fullPropertyCode":"EAM_1.0.0_businessConfig_RiskHandle_eamInfo","isHidden":true,"columnType":"INTEGER","pc":"__pc__=RUFNXzEuMC4wX2J1c2luZXNzQ29uZmlnX3Jpc2tSZWNvcmRfc2VsZnw_","width":100,"openPending":false,"isCustom":false,"textalign":"right","multable":false,"columnName":"EAM_INFO"},{"precisionHasChanged":true,"onlyLeaf":false,"code":"EAM_1.0.0_businessConfig_riskRecorddg1578550214154_LISTPT_OTHER_EAM_1.0.0_businessConfig_RiskHandle_id","isTotal":false,"isOrderBy":false,"seniorSystemcode":false,"displayName":"EAM.propertyshowName.randon1578550439513.flag","showFormatHasChanged":false,"isCount":false,"dbColumnType":"LONG","ecEnv":"product","minValue":"-9223372036854775808","propertyCode":"EAM_1.0.0_businessConfig_RiskHandle_id","regionType":"LISTPT","layRec":"id","modelCode":"EAM_1.0.0_businessConfig_RiskHandle","namekey":"EAM.propertyshowName.randon1578550439513.flag","showType":"TEXTFIELD","key":"id","showFormat":"TEXT","entityCode":"EAM_1.0.0_businessConfig","moduleCode":"EAM_1.0.0","maxValue":"9223372036854775807","cellCode":"cell_1578550420514_5406","fullPropertyCode":"EAM_1.0.0_businessConfig_RiskHandle_id","isHidden":true,"columnType":"LONG","pc":"__pc__=RUFNXzEuMC4wX2J1c2luZXNzQ29uZmlnX3Jpc2tSZWNvcmRfc2VsZnw_","width":100,"openPending":false,"isCustom":false,"textalign":"right","multable":false,"columnName":"ID"},{"assPropertyName":"id","code":"EAM_1.0.0_businessConfig_riskRecorddg1578550214154_LISTPT_OTHER_EAM_1.0.0_businessConfig_RiskHandle_finder_base_staff_id_none","entityCode":"EAM_1.0.0_businessConfig","moduleCode":"EAM_1.0.0","displayName":"finder.id","cellCode":"cell_1584336830045_59","none":"hide","fullPropertyCode":"EAM_1.0.0_businessConfig_RiskHandle_finder||base_staff_id","isHidden":true,"ecEnv":"product","propertyCode":"EAM_1.0.0_businessConfig_RiskHandle_finder||base_staff_id","pc":"__pc__=RUFNXzEuMC4wX2J1c2luZXNzQ29uZmlnX3Jpc2tSZWNvcmRfc2VsZnw_","regionType":"LISTPT","namekey":"finder.id","name":"finder.id","showType":"TEXTFIELD","key":"finder.id"}],"isNoCopy":false,"isCheckBox":false,"acceptRevisions":false,"code":"EAM_1.0.0_businessConfig_riskRecorddg1578550214154","idPrefix":"RiskHandle_dg1578550214154","hideRevision":false,"title":null,"getRevisions":false,"ptPageInit":"var eamId = window.parent.ReactAPI.getParamsInRequestUrl().id;\n\tReactAPI.getComponentAPI(\u0022SupDataGrid\u0022)\n\t\t\t\t  .APIs(\u0022EAM_1.0.0_businessConfig_riskRecorddg1578550214154\u0022)\n\t\t\t\t  .refreshDataByRequst({\n\t\t\t\t\ttype: \u0022post\u0022,\n\t\t\t\t\turl: \u0022/msService/EAM/businessConfig/riskHandle/data-dg1578550214154?datagridCode=EAM_1.0.0_businessConfig_riskRecorddg1578550214154\u0026id=-1\u0022,\n\t\t\t\t\tparam: {\n\t\t\t\t\t\tcustomCondition:{eamId:eamId}\n\t\t\t\t\t}\n\t\t\t\t  });","colspan":1,"ecEnv":"product","showRevision":false,"colNum":6,"hideKey":"\u0027id\u0027,\u0027version\u0027,\u0027finder.id\u0027","isreadonly":0,"selectFirstRow":false,"officePrint":false,"downloadDoc":true,"buttons":[],"entityCode":"EAM_1.0.0_businessConfig","moduleCode":"EAM_1.0.0","autoresize":false,"DataGridCode":"EAM_1.0.0_businessConfig_riskRecorddg1578550214154","listPT":true,"renderOver_es5":"showView(\u0027EAM_1.0.0_businessConfig_riskRecorddg1578550214154\u0027, \u0027riskTableNo\u0027, \u0027risk\u0027);","queryUrl":"/EAM/businessConfig/riskHandle/data-dg1578550214154","saveTemplate":false,"isExportExcel":true,"targetModelCode":"EAM_1.0.0_businessConfig_RiskHandle","isTreeView":false,"openEmptyDoc":true,"multable":false}],"regionType":"LISTPT","colwidth":"13,20,13,20,13,20","cssstyle":null,"customSection":null,"colNum":6,"name":null,"sectionCode":null,"refcol":"\u003ctr style=\u0027border:none;height:0px; \u0027\u003e\u003ctd style=\u0027height:0px;border:none;width:13%\u0027\u003e\u003c/td\u003e\u003ctd style=\u0027height:0px;border:none;width:20%\u0027\u003e\u003c/td\u003e\u003ctd style=\u0027height:0px;border:none;width:13%\u0027\u003e\u003c/td\u003e\u003ctd style=\u0027height:0px;border:none;width:20%\u0027\u003e\u003c/td\u003e\u003ctd style=\u0027height:0px;border:none;width:13%\u0027\u003e\u003c/td\u003e\u003ctd style=\u0027height:0px;border:none;width:20%\u0027\u003e\u003c/td\u003e\u003c/tr\u003e","type":"layoutSection"}],"ratio_w":100,"isrightbottomfixed":false,"layoutContent":"datagrid","type":"layout","ranksType":"panel","isShow":true,"isreadonlyBak":0,"parlayoutname":"tabs-1","layoutmethod":"container","ptRealTimeLoad":1,"isreadonly":0,"layno":2,"layoutname":"row-2","islefttopfixed":false,"nums":1}],"isborder":0,"tabViewIndex":0,"isrightbottomfixed":false,"type":"layout","isreadonlyBak":0,"ptRealTimeLoad":1,"layoutmethod":"row","namekey":"ec.view.commoninfo","name":"常规信息","isreadonly":0,"layno":1,"id":null,"tabCode":"tab_0","layoutname":"tabs-1","islefttopfixed":false,"nums":1}],"layoutmethod":"tab","type":"layout"}],"layoutmethod":"column","type":"layout"}],"enableSimpleDealInfo":false,"title":"EAM.viewtitle.randon1578549638421","url":"/msService/EAM/businessConfig/riskHandle/riskRecord","dealInfoShow":false,"hasAttachment":false,"pageType":"EDIT","dealInfoGroup":"byTime","refCopy":{"isReference":false},"colNum":6,"layoutType":"classic","selfAdaption":false,"events":[]}$eam_risk_view$;
    payload_bytes bytea := convert_to(payload, 'UTF8');
    runtime_json_is_oid boolean;
    existing_oid oid;
    payload_oid oid;
BEGIN
    SELECT udt_name = 'oid'
      INTO runtime_json_is_oid
      FROM information_schema.columns
     WHERE table_schema = 'public'
       AND table_name = 'runtime_extra_view'
       AND column_name = 'view_json';

    IF COALESCE(runtime_json_is_oid, false) THEN
        SELECT view_json
          INTO existing_oid
          FROM public.runtime_extra_view
         WHERE code = 'EAM_1.0.0_businessConfig_riskRecord';

        IF existing_oid IS NOT NULL
           AND EXISTS (
               SELECT 1
                 FROM pg_largeobject_metadata
                WHERE oid = existing_oid
           )
           AND lo_get(existing_oid) = payload_bytes THEN
            payload_oid := existing_oid;
        ELSE
            payload_oid := lo_from_bytea(0, payload_bytes);
        END IF;

        INSERT INTO public.runtime_extra_view
            (code, ec_env, version, view_json, proj_flag, view_code)
        VALUES
            (
                'EAM_1.0.0_businessConfig_riskRecord',
                'product',
                14,
                payload_oid,
                false,
                'EAM_1.0.0_businessConfig_riskRecord'
            )
        ON CONFLICT (code) DO UPDATE SET
            ec_env = EXCLUDED.ec_env,
            version = GREATEST(COALESCE(public.runtime_extra_view.version, 0), EXCLUDED.version),
            view_json = EXCLUDED.view_json,
            proj_flag = EXCLUDED.proj_flag,
            view_code = EXCLUDED.view_code;
    ELSE
        INSERT INTO public.runtime_extra_view
            (code, ec_env, version, view_json, proj_flag, view_code)
        VALUES
            (
                'EAM_1.0.0_businessConfig_riskRecord',
                'product',
                14,
                payload,
                false,
                'EAM_1.0.0_businessConfig_riskRecord'
            )
        ON CONFLICT (code) DO UPDATE SET
            ec_env = EXCLUDED.ec_env,
            version = GREATEST(COALESCE(public.runtime_extra_view.version, 0), EXCLUDED.version),
            view_json = EXCLUDED.view_json,
            proj_flag = EXCLUDED.proj_flag,
            view_code = EXCLUDED.view_code;
    END IF;
END $$;

DO $$
DECLARE
    payload text := $eam_risk_grid${"isSuperTable":false,"iscontrol":false,"nullable":false,"isOfficeHandSign":false,"isRevision":false,"isFirstLoad":false,"type":"layoutDatagrid","readonly":0,"ptPageInit_es5":"var eamId = window.parent.ReactAPI.getParamsInRequestUrl().id;\nReactAPI.getComponentAPI(\u0022SupDataGrid\u0022).APIs(\u0022EAM_1.0.0_businessConfig_riskRecorddg1578550214154\u0022).refreshDataByRequst({\n\ttype: \u0022post\u0022,\n\turl: \u0022/msService/EAM/businessConfig/riskHandle/data-dg1578550214154?datagridCode=EAM_1.0.0_businessConfig_riskRecorddg1578550214154\u0026id=-1\u0022,\n\tparam: {\n\t\tcustomCondition: { eamId: eamId }\n\t}\n});","isdbcustom":false,"convertPdfOnLine":true,"regionType":"LISTPT","getRequireData":"/baseService/excel/getRequireDataByModelcode","modelCode":"EAM_1.0.0_businessConfig_RiskHandle","complex":true,"rowspan":1,"downloadXls":"/msService/EAM/businessConfig/riskHandle/downloadXls","showType":"DATAGRID","isgroup":false,"isCreateNew":false,"renderOver":"showView(\u0027EAM_1.0.0_businessConfig_riskRecorddg1578550214154\u0027,\u0027riskTableNo\u0027,\u0027risk\u0027)","firstTd":1,"officeNotNull":false,"cellCode":"cell_1578550338819_4902","importMainXls":"/msService/EAM/businessConfig/riskHandle/importMainXls","isTransCondition":false,"nodeExpanded":false,"isOfficeSign":false,"columnType":"DATAGRID","isTitle":true,"dataGridName":"dg1578550214154","hasFastQuery":false,"fields":[{"precisionHasChanged":false,"onlyLeaf":false,"code":"EAM_1.0.0_businessConfig_riskRecorddg1578550214154_LISTPT_OTHER_EAM_1.0.0_businessConfig_RiskHandle_riskTableNo","isTotal":false,"isOrderBy":false,"seniorSystemcode":false,"displayName":"EAM.businessConfig.RiskHandle.table_no","showFormatHasChanged":false,"isCount":false,"dbColumnType":"TEXT","ecEnv":"product","propertyCode":"EAM_1.0.0_businessConfig_RiskHandle_riskTableNo","regionType":"LISTPT","layRec":"riskTableNo","modelCode":"EAM_1.0.0_businessConfig_RiskHandle","namekey":"EAM.businessConfig.RiskHandle.table_no","showType":"TEXTFIELD","key":"riskTableNo","showFormat":"TEXT","entityCode":"EAM_1.0.0_businessConfig","moduleCode":"EAM_1.0.0","cellCode":"cell_1578562700653_92","fullPropertyCode":"EAM_1.0.0_businessConfig_RiskHandle_riskTableNo","isHidden":false,"columnType":"TEXT","pc":"__pc__=RUFNXzEuMC4wX2J1c2luZXNzQ29uZmlnX3Jpc2tSZWNvcmRfc2VsZnw_","width":130,"openPending":false,"isCustom":false,"textalign":"left","multable":false,"maxLength":2000,"columnName":"TABLE_NO"},{"ass":{"tar":"base_staff_id","org":"EAM_1.0.0_businessConfig_RiskHandle_finder"},"assPropertyName":"id","precisionHasChanged":true,"onlyLeaf":null,"code":"EAM_1.0.0_businessConfig_riskRecorddg1578550214154_LISTPT_OTHER_EAM_1.0.0_businessConfig_RiskHandle_finder_base_staff_name","isTotal":false,"isOrderBy":false,"seniorSystemcode":false,"displayName":"EAM.businessConfig.RiskHandle.finder","showFormatHasChanged":false,"isCount":false,"dbColumnType":"TEXT","ecEnv":"product","propertyCode":"EAM_1.0.0_businessConfig_RiskHandle_finder||base_staff_name","regionType":"LISTPT","modelCode":"sysbase_1.0_staff_base_staff","layRec":"base_staff,ID,SES_HRM_RISKHANDLES,FINDER-name","namekey":"EAM.businessConfig.RiskHandle.finder","showType":"TEXTFIELD","key":"finder.name","isLink":false,"showFormat":"TEXT","entityCode":"EAM_1.0.0_businessConfig","moduleCode":"EAM_1.0.0","cellCode":"cell_1578550344066_8231","fullPropertyCode":"EAM_1.0.0_businessConfig_RiskHandle_finder||base_staff_name","isHidden":false,"columnType":"TEXT","pc":"__pc__=RUFNXzEuMC4wX2J1c2luZXNzQ29uZmlnX3Jpc2tSZWNvcmRfc2VsZnw_","textalign":"left","width":100,"openPending":false,"isCustom":false,"multable":false,"maxLength":80,"columnName":"NAME"},{"precisionHasChanged":false,"onlyLeaf":false,"code":"EAM_1.0.0_businessConfig_riskRecorddg1578550214154_LISTPT_OTHER_EAM_1.0.0_businessConfig_RiskHandle_findTime","isTotal":false,"isOrderBy":false,"seniorSystemcode":false,"displayName":"EAM.businessConfig.RiskHandle.findTime","showFormatHasChanged":false,"isCount":false,"dbColumnType":"DATETIME","ecEnv":"product","propertyCode":"EAM_1.0.0_businessConfig_RiskHandle_findTime","regionType":"LISTPT","layRec":"findTime","modelCode":"EAM_1.0.0_businessConfig_RiskHandle","namekey":"EAM.businessConfig.RiskHandle.findTime","showType":"DATETIME","key":"findTime","showFormat":"YMD_HMS","entityCode":"EAM_1.0.0_businessConfig","moduleCode":"EAM_1.0.0","cellCode":"cell_1578550344258_7655","fullPropertyCode":"EAM_1.0.0_businessConfig_RiskHandle_findTime","isHidden":false,"columnType":"DATETIME","pc":"__pc__=RUFNXzEuMC4wX2J1c2luZXNzQ29uZmlnX3Jpc2tSZWNvcmRfc2VsZnw_","width":150,"openPending":false,"isCustom":false,"textalign":"center","multable":false,"maxLength":null,"columnName":"FIND_TIME"},{"precisionHasChanged":false,"onlyLeaf":false,"code":"EAM_1.0.0_businessConfig_riskRecorddg1578550214154_LISTPT_OTHER_EAM_1.0.0_businessConfig_RiskHandle_riskContent","isTotal":false,"isOrderBy":false,"seniorSystemcode":false,"displayName":"EAM.businessConfig.RiskHandle.riskContent","showFormatHasChanged":false,"isCount":false,"dbColumnType":"TEXT","ecEnv":"product","propertyCode":"EAM_1.0.0_businessConfig_RiskHandle_riskContent","regionType":"LISTPT","layRec":"riskContent","modelCode":"EAM_1.0.0_businessConfig_RiskHandle","namekey":"EAM.businessConfig.RiskHandle.riskContent","showType":"TEXTFIELD","key":"riskContent","showFormat":"TEXT","entityCode":"EAM_1.0.0_businessConfig","moduleCode":"EAM_1.0.0","cellCode":"cell_1578550344451_491","fullPropertyCode":"EAM_1.0.0_businessConfig_RiskHandle_riskContent","isHidden":false,"columnType":"TEXT","pc":"__pc__=RUFNXzEuMC4wX2J1c2luZXNzQ29uZmlnX3Jpc2tSZWNvcmRfc2VsZnw_","width":300,"openPending":false,"isCustom":false,"textalign":"left","multable":false,"maxLength":255,"columnName":"RISK_CONTENT"},{"precisionHasChanged":false,"onlyLeaf":false,"code":"EAM_1.0.0_businessConfig_riskRecorddg1578550214154_LISTPT_OTHER_EAM_1.0.0_businessConfig_RiskHandle_riskSource","isTotal":false,"isOrderBy":false,"seniorSystemcode":false,"displayName":"EAM.businessConfig.RiskHandle.riskSource","showFormatHasChanged":false,"isCount":false,"dbColumnType":"TEXT","ecEnv":"product","propertyCode":"EAM_1.0.0_businessConfig_RiskHandle_riskSource","regionType":"LISTPT","layRec":"riskSource","modelCode":"EAM_1.0.0_businessConfig_RiskHandle","namekey":"EAM.businessConfig.RiskHandle.riskSource","showType":"TEXTFIELD","key":"riskSource","showFormat":"TEXT","entityCode":"EAM_1.0.0_businessConfig","moduleCode":"EAM_1.0.0","cellCode":"cell_1578550344642_781","fullPropertyCode":"EAM_1.0.0_businessConfig_RiskHandle_riskSource","isHidden":false,"columnType":"TEXT","pc":"__pc__=RUFNXzEuMC4wX2J1c2luZXNzQ29uZmlnX3Jpc2tSZWNvcmRfc2VsZnw_","width":80,"openPending":false,"isCustom":false,"textalign":"left","multable":false,"maxLength":2000,"columnName":"RISK_SOURCE"},{"precisionHasChanged":false,"onlyLeaf":false,"code":"EAM_1.0.0_businessConfig_riskRecorddg1578550214154_LISTPT_OTHER_EAM_1.0.0_businessConfig_RiskHandle_riskLevel","isTotal":false,"isOrderBy":false,"seniorSystemcode":false,"displayName":"EAM.businessConfig.RiskHandle.riskLevel","showFormatHasChanged":false,"isCount":false,"dbColumnType":"TEXT","ecEnv":"product","propertyCode":"EAM_1.0.0_businessConfig_RiskHandle_riskLevel","regionType":"LISTPT","layRec":"riskLevel","modelCode":"EAM_1.0.0_businessConfig_RiskHandle","namekey":"EAM.businessConfig.RiskHandle.riskLevel","showType":"TEXTFIELD","key":"riskLevel","showFormat":"TEXT","entityCode":"EAM_1.0.0_businessConfig","moduleCode":"EAM_1.0.0","cellCode":"cell_1578552601206_3112","fullPropertyCode":"EAM_1.0.0_businessConfig_RiskHandle_riskLevel","isHidden":false,"columnType":"TEXT","pc":"__pc__=RUFNXzEuMC4wX2J1c2luZXNzQ29uZmlnX3Jpc2tSZWNvcmRfc2VsZnw_","width":80,"openPending":false,"isCustom":false,"textalign":"left","multable":false,"maxLength":2000,"columnName":"RISK_LEVEL"},{"precisionHasChanged":false,"onlyLeaf":false,"code":"EAM_1.0.0_businessConfig_riskRecorddg1578550214154_LISTPT_OTHER_EAM_1.0.0_businessConfig_RiskHandle_eamInfo","isTotal":false,"isOrderBy":false,"seniorSystemcode":false,"displayName":"EAM.businessConfig.RiskHandle.eamInfo","showFormatHasChanged":false,"isCount":false,"dbColumnType":"LONG","ecEnv":"product","minValue":"-2147483648","propertyCode":"EAM_1.0.0_businessConfig_RiskHandle_eamInfo","regionType":"LISTPT","layRec":"eamInfo","modelCode":"EAM_1.0.0_businessConfig_RiskHandle","namekey":"EAM.businessConfig.RiskHandle.eamInfo","showType":"TEXTFIELD","key":"eamInfo","showFormat":"TEXT","entityCode":"EAM_1.0.0_businessConfig","moduleCode":"EAM_1.0.0","maxValue":"2147483647","cellCode":"cell_1578550344834_1134","fullPropertyCode":"EAM_1.0.0_businessConfig_RiskHandle_eamInfo","isHidden":true,"columnType":"INTEGER","pc":"__pc__=RUFNXzEuMC4wX2J1c2luZXNzQ29uZmlnX3Jpc2tSZWNvcmRfc2VsZnw_","width":100,"openPending":false,"isCustom":false,"textalign":"right","multable":false,"columnName":"EAM_INFO"},{"precisionHasChanged":true,"onlyLeaf":false,"code":"EAM_1.0.0_businessConfig_riskRecorddg1578550214154_LISTPT_OTHER_EAM_1.0.0_businessConfig_RiskHandle_id","isTotal":false,"isOrderBy":false,"seniorSystemcode":false,"displayName":"EAM.propertyshowName.randon1578550439513.flag","showFormatHasChanged":false,"isCount":false,"dbColumnType":"LONG","ecEnv":"product","minValue":"-9223372036854775808","propertyCode":"EAM_1.0.0_businessConfig_RiskHandle_id","regionType":"LISTPT","layRec":"id","modelCode":"EAM_1.0.0_businessConfig_RiskHandle","namekey":"EAM.propertyshowName.randon1578550439513.flag","showType":"TEXTFIELD","key":"id","showFormat":"TEXT","entityCode":"EAM_1.0.0_businessConfig","moduleCode":"EAM_1.0.0","maxValue":"9223372036854775807","cellCode":"cell_1578550420514_5406","fullPropertyCode":"EAM_1.0.0_businessConfig_RiskHandle_id","isHidden":true,"columnType":"LONG","pc":"__pc__=RUFNXzEuMC4wX2J1c2luZXNzQ29uZmlnX3Jpc2tSZWNvcmRfc2VsZnw_","width":100,"openPending":false,"isCustom":false,"textalign":"right","multable":false,"columnName":"ID"},{"assPropertyName":"id","code":"EAM_1.0.0_businessConfig_riskRecorddg1578550214154_LISTPT_OTHER_EAM_1.0.0_businessConfig_RiskHandle_finder_base_staff_id_none","entityCode":"EAM_1.0.0_businessConfig","moduleCode":"EAM_1.0.0","displayName":"finder.id","cellCode":"cell_1584336830045_59","none":"hide","fullPropertyCode":"EAM_1.0.0_businessConfig_RiskHandle_finder||base_staff_id","isHidden":true,"ecEnv":"product","propertyCode":"EAM_1.0.0_businessConfig_RiskHandle_finder||base_staff_id","pc":"__pc__=RUFNXzEuMC4wX2J1c2luZXNzQ29uZmlnX3Jpc2tSZWNvcmRfc2VsZnw_","regionType":"LISTPT","namekey":"finder.id","name":"finder.id","showType":"TEXTFIELD","key":"finder.id"}],"isNoCopy":false,"isCheckBox":false,"acceptRevisions":false,"code":"EAM_1.0.0_businessConfig_riskRecorddg1578550214154","idPrefix":"RiskHandle_dg1578550214154","hideRevision":false,"title":null,"getRevisions":false,"ptPageInit":"var eamId = window.parent.ReactAPI.getParamsInRequestUrl().id;\n\tReactAPI.getComponentAPI(\u0022SupDataGrid\u0022)\n\t\t\t\t  .APIs(\u0022EAM_1.0.0_businessConfig_riskRecorddg1578550214154\u0022)\n\t\t\t\t  .refreshDataByRequst({\n\t\t\t\t\ttype: \u0022post\u0022,\n\t\t\t\t\turl: \u0022/msService/EAM/businessConfig/riskHandle/data-dg1578550214154?datagridCode=EAM_1.0.0_businessConfig_riskRecorddg1578550214154\u0026id=-1\u0022,\n\t\t\t\t\tparam: {\n\t\t\t\t\t\tcustomCondition:{eamId:eamId}\n\t\t\t\t\t}\n\t\t\t\t  });","colspan":1,"ecEnv":"product","showRevision":false,"colNum":6,"hideKey":"\u0027id\u0027,\u0027version\u0027,\u0027finder.id\u0027","isreadonly":0,"selectFirstRow":false,"officePrint":false,"downloadDoc":true,"buttons":[],"entityCode":"EAM_1.0.0_businessConfig","moduleCode":"EAM_1.0.0","autoresize":false,"DataGridCode":"EAM_1.0.0_businessConfig_riskRecorddg1578550214154","listPT":true,"renderOver_es5":"showView(\u0027EAM_1.0.0_businessConfig_riskRecorddg1578550214154\u0027, \u0027riskTableNo\u0027, \u0027risk\u0027);","queryUrl":"/EAM/businessConfig/riskHandle/data-dg1578550214154","saveTemplate":false,"isExportExcel":true,"targetModelCode":"EAM_1.0.0_businessConfig_RiskHandle","isTreeView":false,"openEmptyDoc":true,"multable":false}$eam_risk_grid$;
    payload_bytes bytea := convert_to(payload, 'UTF8');
    runtime_json_is_oid boolean;
    existing_oid oid;
    payload_oid oid;
    ec_grid_count integer;
    runtime_grid_count integer;
BEGIN
    UPDATE public.ec_data_grid
       SET data_grid_json = payload,
           version = GREATEST(COALESCE(version, 0), 14),
           modify_time = now()
     WHERE code = 'EAM_1.0.0_businessConfig_riskRecorddg1578550214154';
    GET DIAGNOSTICS ec_grid_count = ROW_COUNT;

    SELECT udt_name = 'oid'
      INTO runtime_json_is_oid
      FROM information_schema.columns
     WHERE table_schema = 'public'
       AND table_name = 'runtime_data_grid'
       AND column_name = 'data_grid_json';

    IF COALESCE(runtime_json_is_oid, false) THEN
        SELECT data_grid_json
          INTO existing_oid
          FROM public.runtime_data_grid
         WHERE code = 'EAM_1.0.0_businessConfig_riskRecorddg1578550214154';

        IF existing_oid IS NOT NULL
           AND EXISTS (
               SELECT 1
                 FROM pg_largeobject_metadata
                WHERE oid = existing_oid
           )
           AND lo_get(existing_oid) = payload_bytes THEN
            payload_oid := existing_oid;
        ELSE
            payload_oid := lo_from_bytea(0, payload_bytes);
        END IF;

        UPDATE public.runtime_data_grid
           SET data_grid_json = payload_oid,
               version = GREATEST(COALESCE(version, 0), 14),
               modify_time = now()
         WHERE code = 'EAM_1.0.0_businessConfig_riskRecorddg1578550214154';
    ELSE
        UPDATE public.runtime_data_grid
           SET data_grid_json = payload,
               version = GREATEST(COALESCE(version, 0), 14),
               modify_time = now()
         WHERE code = 'EAM_1.0.0_businessConfig_riskRecorddg1578550214154';
    END IF;
    GET DIAGNOSTICS runtime_grid_count = ROW_COUNT;

    IF ec_grid_count <> 1 OR runtime_grid_count <> 1 THEN
        RAISE EXCEPTION
            'EAM risk-record datagrid metadata is missing: ec=%, runtime=%',
            ec_grid_count,
            runtime_grid_count;
    END IF;
END $$;

DO $$
DECLARE
    required_column text;
    missing_columns text[] := ARRAY[]::text[];
    sql_row_count integer;
    ec_json_length integer;
    runtime_json_length integer;
    ec_grid_json_length integer;
    runtime_grid_json_length integer;
    runtime_json_is_oid boolean;
    runtime_grid_json_is_oid boolean;
BEGIN
    FOREACH required_column IN ARRAY ARRAY[
        'find_time',
        'modify_time',
        'create_staff_id',
        'modify_staff_id',
        'create_department_id',
        'create_position_id',
        'numberparama',
        'numberparaml',
        'dateparama',
        'dateparamp',
        'scparama',
        'scparamh',
        'objparama',
        'objparamh',
        'charparama',
        'charparamj'
    ]
    LOOP
        IF NOT EXISTS (
            SELECT 1
              FROM information_schema.columns
             WHERE table_schema = 'public'
               AND table_name = 'ses_hrm_riskhandles'
               AND column_name = required_column
        ) THEN
            missing_columns := array_append(missing_columns, required_column);
        END IF;
    END LOOP;

    IF cardinality(missing_columns) <> 0 THEN
        RAISE EXCEPTION 'EAM risk columns are missing: %', missing_columns;
    END IF;

    SELECT count(*)
      INTO sql_row_count
      FROM public.runtime_sql
     WHERE code IN (
               'EAM_1.0.0_businessConfig_riskRecord_dg1578550214154_3',
               'EAM_1.0.0_businessConfig_riskRecord_dg1578550214154_6'
           )
       AND data_grid_code = 'EAM_1.0.0_businessConfig_riskRecorddg1578550214154';

    IF sql_row_count <> 2 THEN
        RAISE EXCEPTION 'EAM risk-record SQL metadata is incomplete: %', sql_row_count;
    END IF;

    SELECT octet_length(view_json)
      INTO ec_json_length
      FROM public.ec_extra_view
     WHERE code = 'EAM_1.0.0_businessConfig_riskRecord';

    SELECT udt_name = 'oid'
      INTO runtime_json_is_oid
      FROM information_schema.columns
     WHERE table_schema = 'public'
       AND table_name = 'runtime_extra_view'
       AND column_name = 'view_json';

    IF COALESCE(runtime_json_is_oid, false) THEN
        SELECT octet_length(lo_get(view_json))
          INTO runtime_json_length
          FROM public.runtime_extra_view
         WHERE code = 'EAM_1.0.0_businessConfig_riskRecord';
    ELSE
        SELECT octet_length(view_json)
          INTO runtime_json_length
          FROM public.runtime_extra_view
         WHERE code = 'EAM_1.0.0_businessConfig_riskRecord';
    END IF;

    IF COALESCE(ec_json_length, 0) < 10000
       OR COALESCE(runtime_json_length, 0) < 10000 THEN
        RAISE EXCEPTION
            'EAM risk-record layout JSON is incomplete: ec=%, runtime=%',
            ec_json_length,
            runtime_json_length;
    END IF;

    SELECT octet_length(data_grid_json)
      INTO ec_grid_json_length
      FROM public.ec_data_grid
     WHERE code = 'EAM_1.0.0_businessConfig_riskRecorddg1578550214154';

    SELECT udt_name = 'oid'
      INTO runtime_grid_json_is_oid
      FROM information_schema.columns
     WHERE table_schema = 'public'
       AND table_name = 'runtime_data_grid'
       AND column_name = 'data_grid_json';

    IF COALESCE(runtime_grid_json_is_oid, false) THEN
        SELECT octet_length(lo_get(data_grid_json))
          INTO runtime_grid_json_length
          FROM public.runtime_data_grid
         WHERE code = 'EAM_1.0.0_businessConfig_riskRecorddg1578550214154';
    ELSE
        SELECT octet_length(data_grid_json)
          INTO runtime_grid_json_length
          FROM public.runtime_data_grid
         WHERE code = 'EAM_1.0.0_businessConfig_riskRecorddg1578550214154';
    END IF;

    IF COALESCE(ec_grid_json_length, 0) < 10000
       OR COALESCE(runtime_grid_json_length, 0) < 10000 THEN
        RAISE EXCEPTION
            'EAM risk-record datagrid JSON is incomplete: ec=%, runtime=%',
            ec_grid_json_length,
            runtime_grid_json_length;
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM public.base_systementity
         WHERE code = 'SESHRM_riskResource'
           AND name = 'SESHRM.systemEntityname.randon1570600798462'
    ) THEN
        RAISE EXCEPTION 'SESHRM hidden-danger source entity is missing';
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM public.base_systemcode
         WHERE id = 'SESHRM_riskResource/005'
           AND value = 'SESHRM.systemCodevalue.randon1589806103444'
           AND value_zh_cn = '巡检'
    ) THEN
        RAISE EXCEPTION 'SESHRM inspection source system code is missing';
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM public.supfusion_i18n_resource
         WHERE i18n_key = 'SESHRM.systemCodevalue.randon1589806103444'
           AND i18n_value = '巡检'
           AND langu_code = 'zh_CN'
           AND COALESCE(tenant_id, 'dt') = 'dt'
           AND valid = '1'
    ) THEN
        RAISE EXCEPTION 'SESHRM inspection source translation is missing';
    END IF;

    PERFORM "riskHandle".id
      FROM public.ses_hrm_riskhandles "riskHandle"
      LEFT JOIN public.base_staff finder ON finder.id = "riskHandle".finder
     LIMIT 0;
END $$;

COMMIT;
