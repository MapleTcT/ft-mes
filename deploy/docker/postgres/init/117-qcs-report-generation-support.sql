-- QCS manufacturing inspection report-generation prerequisites.
--
-- Source evidence:
-- - QCSInspectServiceImpl.afterSubmitInspect calls
--   QCSInspectReportServiceImpl.createQcsReportByInspet when a manufacturing
--   inspection becomes effective and needLab=false.
-- - createQcsReportByInspet only runs when QCS.autoReport contains the current
--   table type code ("manu"), loads report items from limsba_std_ver_coms when
--   QCS.reportShowIndexRange=qualityStd, and then starts the custom workflow
--   config code "manuReport".
-- - LIMSBasic init.xml defines the missing LIMSBA_TEST_COMPONENTS entity used
--   by LIMSBA_STD_VER_COMS.COM_ID. Without it, a report item can exist but
--   Hibernate cannot materialize stdVerCom.comId for QCS_REPORT_COMS creation.

CREATE TABLE IF NOT EXISTS public.limsba_test_components (
    id bigint PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS public.limsba_std_ver_grades (
    id bigint PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS public.limsba_spec_limits (
    id bigint PRIMARY KEY
);

ALTER TABLE public.limsba_test_components
    ADD COLUMN IF NOT EXISTS version integer DEFAULT 0,
    ADD COLUMN IF NOT EXISTS create_staff_id bigint,
    ADD COLUMN IF NOT EXISTS create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS modify_staff_id bigint,
    ADD COLUMN IF NOT EXISTS modify_time timestamp without time zone,
    ADD COLUMN IF NOT EXISTS delete_staff_id bigint,
    ADD COLUMN IF NOT EXISTS delete_time timestamp without time zone,
    ADD COLUMN IF NOT EXISTS valid boolean DEFAULT true,
    ADD COLUMN IF NOT EXISTS cid bigint,
    ADD COLUMN IF NOT EXISTS sort integer,
    ADD COLUMN IF NOT EXISTS table_info_id bigint,
    ADD COLUMN IF NOT EXISTS old_com_name text,
    ADD COLUMN IF NOT EXISTS option_names text,
    ADD COLUMN IF NOT EXISTS option_values text,
    ADD COLUMN IF NOT EXISTS origin_value text,
    ADD COLUMN IF NOT EXISTS parallel_times integer DEFAULT 1,
    ADD COLUMN IF NOT EXISTS report_name text,
    ADD COLUMN IF NOT EXISTS round_value text,
    ADD COLUMN IF NOT EXISTS test_id bigint,
    ADD COLUMN IF NOT EXISTS unit_name text,
    ADD COLUMN IF NOT EXISTS value_kind character varying,
    ADD COLUMN IF NOT EXISTS code character varying(200),
    ADD COLUMN IF NOT EXISTS com_type character varying,
    ADD COLUMN IF NOT EXISTS default_value text,
    ADD COLUMN IF NOT EXISTS digit_type character varying,
    ADD COLUMN IF NOT EXISTS disp_value text,
    ADD COLUMN IF NOT EXISTS executable_formula text,
    ADD COLUMN IF NOT EXISTS is_necessary boolean DEFAULT true,
    ADD COLUMN IF NOT EXISTS is_report boolean DEFAULT false,
    ADD COLUMN IF NOT EXISTS limit_type character varying,
    ADD COLUMN IF NOT EXISTS max_value text,
    ADD COLUMN IF NOT EXISTS memo_field text,
    ADD COLUMN IF NOT EXISTS min_value text,
    ADD COLUMN IF NOT EXISTS name text,
    ADD COLUMN IF NOT EXISTS name_eng text,
    ADD COLUMN IF NOT EXISTS calculate_param_names text,
    ADD COLUMN IF NOT EXISTS calculate_params text,
    ADD COLUMN IF NOT EXISTS calcul_formula text,
    ADD COLUMN IF NOT EXISTS carry_formula text,
    ADD COLUMN IF NOT EXISTS carry_rule text,
    ADD COLUMN IF NOT EXISTS carry_space text,
    ADD COLUMN IF NOT EXISTS carry_type character varying,
    ADD COLUMN IF NOT EXISTS bigintparama integer,
    ADD COLUMN IF NOT EXISTS bigintparamb integer,
    ADD COLUMN IF NOT EXISTS bigintparamc integer,
    ADD COLUMN IF NOT EXISTS bigintparamd integer,
    ADD COLUMN IF NOT EXISTS charparama text,
    ADD COLUMN IF NOT EXISTS charparamb text,
    ADD COLUMN IF NOT EXISTS charparamc text,
    ADD COLUMN IF NOT EXISTS charparamd text,
    ADD COLUMN IF NOT EXISTS dateparama timestamp without time zone,
    ADD COLUMN IF NOT EXISTS dateparamb timestamp without time zone,
    ADD COLUMN IF NOT EXISTS numberparama numeric(38, 6),
    ADD COLUMN IF NOT EXISTS numberparamb numeric(38, 6),
    ADD COLUMN IF NOT EXISTS numberparamc numeric(38, 6),
    ADD COLUMN IF NOT EXISTS numberparamd numeric(38, 6),
    ADD COLUMN IF NOT EXISTS objparama bigint,
    ADD COLUMN IF NOT EXISTS objparamb bigint,
    ADD COLUMN IF NOT EXISTS scparama text,
    ADD COLUMN IF NOT EXISTS scparamb text;

ALTER TABLE public.limsba_std_ver_grades
    ADD COLUMN IF NOT EXISTS version integer DEFAULT 0,
    ADD COLUMN IF NOT EXISTS create_staff_id bigint,
    ADD COLUMN IF NOT EXISTS create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS modify_staff_id bigint,
    ADD COLUMN IF NOT EXISTS modify_time timestamp without time zone,
    ADD COLUMN IF NOT EXISTS delete_staff_id bigint,
    ADD COLUMN IF NOT EXISTS delete_time timestamp without time zone,
    ADD COLUMN IF NOT EXISTS valid boolean DEFAULT true,
    ADD COLUMN IF NOT EXISTS cid bigint,
    ADD COLUMN IF NOT EXISTS sort integer,
    ADD COLUMN IF NOT EXISTS table_info_id bigint,
    ADD COLUMN IF NOT EXISTS product_id bigint,
    ADD COLUMN IF NOT EXISTS std_grade character varying,
    ADD COLUMN IF NOT EXISTS std_id bigint,
    ADD COLUMN IF NOT EXISTS std_ver_id bigint,
    ADD COLUMN IF NOT EXISTS code character varying(200),
    ADD COLUMN IF NOT EXISTS memo_field text,
    ADD COLUMN IF NOT EXISTS name character varying(256),
    ADD COLUMN IF NOT EXISTS doc_id bigint,
    ADD COLUMN IF NOT EXISTS bap_attachment_info text,
    ADD COLUMN IF NOT EXISTS company_id bigint,
    ADD COLUMN IF NOT EXISTS objparama bigint,
    ADD COLUMN IF NOT EXISTS objparamb bigint,
    ADD COLUMN IF NOT EXISTS scparama text,
    ADD COLUMN IF NOT EXISTS scparamb text,
    ADD COLUMN IF NOT EXISTS bigintparama integer,
    ADD COLUMN IF NOT EXISTS bigintparamb integer,
    ADD COLUMN IF NOT EXISTS charparama text,
    ADD COLUMN IF NOT EXISTS charparamb text,
    ADD COLUMN IF NOT EXISTS dateparama timestamp without time zone,
    ADD COLUMN IF NOT EXISTS dateparamb timestamp without time zone,
    ADD COLUMN IF NOT EXISTS numberparama numeric(38, 6),
    ADD COLUMN IF NOT EXISTS numberparamb numeric(38, 6);

ALTER TABLE public.limsba_spec_limits
    ADD COLUMN IF NOT EXISTS version integer DEFAULT 0,
    ADD COLUMN IF NOT EXISTS create_staff_id bigint,
    ADD COLUMN IF NOT EXISTS create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS modify_staff_id bigint,
    ADD COLUMN IF NOT EXISTS modify_time timestamp without time zone,
    ADD COLUMN IF NOT EXISTS delete_staff_id bigint,
    ADD COLUMN IF NOT EXISTS delete_time timestamp without time zone,
    ADD COLUMN IF NOT EXISTS valid boolean DEFAULT true,
    ADD COLUMN IF NOT EXISTS cid bigint,
    ADD COLUMN IF NOT EXISTS sort integer,
    ADD COLUMN IF NOT EXISTS table_info_id bigint,
    ADD COLUMN IF NOT EXISTS code character varying(200),
    ADD COLUMN IF NOT EXISTS disp_value text,
    ADD COLUMN IF NOT EXISTS judge_cond text,
    ADD COLUMN IF NOT EXISTS judge_names text,
    ADD COLUMN IF NOT EXISTS judge_option text,
    ADD COLUMN IF NOT EXISTS judge_values text,
    ADD COLUMN IF NOT EXISTS max_val_include boolean,
    ADD COLUMN IF NOT EXISTS max_value text,
    ADD COLUMN IF NOT EXISTS min_val_include boolean,
    ADD COLUMN IF NOT EXISTS min_value text,
    ADD COLUMN IF NOT EXISTS sampling_plan character varying,
    ADD COLUMN IF NOT EXISTS standard_grade character varying,
    ADD COLUMN IF NOT EXISTS std_grade_name text,
    ADD COLUMN IF NOT EXISTS std_id bigint,
    ADD COLUMN IF NOT EXISTS std_ver_com_id bigint,
    ADD COLUMN IF NOT EXISTS valuec integer,
    ADD COLUMN IF NOT EXISTS valuem text,
    ADD COLUMN IF NOT EXISTS valuemm text,
    ADD COLUMN IF NOT EXISTS valuen integer,
    ADD COLUMN IF NOT EXISTS doc_id bigint,
    ADD COLUMN IF NOT EXISTS bap_attachment_info text,
    ADD COLUMN IF NOT EXISTS company_id bigint,
    ADD COLUMN IF NOT EXISTS bigintparama integer,
    ADD COLUMN IF NOT EXISTS bigintparamb integer,
    ADD COLUMN IF NOT EXISTS charparama text,
    ADD COLUMN IF NOT EXISTS charparamb text,
    ADD COLUMN IF NOT EXISTS dateparama timestamp without time zone,
    ADD COLUMN IF NOT EXISTS dateparamb timestamp without time zone,
    ADD COLUMN IF NOT EXISTS numberparama numeric(38, 6),
    ADD COLUMN IF NOT EXISTS numberparamb numeric(38, 6),
    ADD COLUMN IF NOT EXISTS objparama bigint,
    ADD COLUMN IF NOT EXISTS objparamb bigint,
    ADD COLUMN IF NOT EXISTS scparama text,
    ADD COLUMN IF NOT EXISTS scparamb text,
    ADD COLUMN IF NOT EXISTS result_value text,
    ADD COLUMN IF NOT EXISTS result_key text,
    ADD COLUMN IF NOT EXISTS un_qualified_value text;

ALTER TABLE public.limsba_std_ver_coms
    ADD COLUMN IF NOT EXISTS com_id bigint,
    ADD COLUMN IF NOT EXISTS is_report boolean DEFAULT false,
    ADD COLUMN IF NOT EXISTS report_name text,
    ADD COLUMN IF NOT EXISTS report_sort integer,
    ADD COLUMN IF NOT EXISTS unit_name text,
    ADD COLUMN IF NOT EXISTS name_eng text,
    ADD COLUMN IF NOT EXISTS memo_field text,
    ADD COLUMN IF NOT EXISTS valuen integer,
    ADD COLUMN IF NOT EXISTS sampling_plan character varying,
    ADD COLUMN IF NOT EXISTS parallel_times integer DEFAULT 1;

CREATE INDEX IF NOT EXISTS idx_limsba_test_components_code
    ON public.limsba_test_components(code);
CREATE INDEX IF NOT EXISTS idx_limsba_test_components_name
    ON public.limsba_test_components(name);
CREATE INDEX IF NOT EXISTS idx_limsba_test_components_test_id
    ON public.limsba_test_components(test_id);
CREATE INDEX IF NOT EXISTS idx_limsba_test_components_valid
    ON public.limsba_test_components(valid);
CREATE INDEX IF NOT EXISTS idx_limsba_std_ver_grades_code
    ON public.limsba_std_ver_grades(code);
CREATE INDEX IF NOT EXISTS idx_limsba_std_ver_grades_std_ver_id
    ON public.limsba_std_ver_grades(std_ver_id);
CREATE INDEX IF NOT EXISTS idx_limsba_std_ver_grades_std_id
    ON public.limsba_std_ver_grades(std_id);
CREATE INDEX IF NOT EXISTS idx_limsba_std_ver_grades_product_id
    ON public.limsba_std_ver_grades(product_id);
CREATE INDEX IF NOT EXISTS idx_limsba_std_ver_grades_valid
    ON public.limsba_std_ver_grades(valid);
CREATE INDEX IF NOT EXISTS idx_limsba_spec_limits_code
    ON public.limsba_spec_limits(code);
CREATE INDEX IF NOT EXISTS idx_limsba_spec_limits_std_id
    ON public.limsba_spec_limits(std_id);
CREATE INDEX IF NOT EXISTS idx_limsba_spec_limits_std_ver_com_id
    ON public.limsba_spec_limits(std_ver_com_id);
CREATE INDEX IF NOT EXISTS idx_limsba_spec_limits_valid
    ON public.limsba_spec_limits(valid);
CREATE INDEX IF NOT EXISTS idx_limsba_std_ver_coms_com_id
    ON public.limsba_std_ver_coms(com_id);
CREATE INDEX IF NOT EXISTS idx_limsba_std_ver_coms_report
    ON public.limsba_std_ver_coms(std_ver_id, is_report, valid);

INSERT INTO public.systemconfig_config_catalog(
  id, parent_id, sort, code, name, has_hide, app_code, catalog_type,
  creator, create_time, tenant_id
)
VALUES (
  117000, 2, 117, 'QCS', 'QCS.ocd.QCS', false, 'QCS', 2,
  'postgres-init-117', CURRENT_TIMESTAMP, 'dt'
)
ON CONFLICT (id) DO UPDATE
SET parent_id = EXCLUDED.parent_id,
    sort = EXCLUDED.sort,
    code = EXCLUDED.code,
    name = EXCLUDED.name,
    has_hide = EXCLUDED.has_hide,
    app_code = EXCLUDED.app_code,
    catalog_type = EXCLUDED.catalog_type,
    tenant_id = EXCLUDED.tenant_id,
    modifier = 'postgres-init-117',
    modify_time = CURRENT_TIMESTAMP;

INSERT INTO public.systemconfig_config_info(
  id, catalog_id, sort, code, name, app_code, module_code, widget_type,
  default_value, widget_value, max_value, min_value, reg_format, reg_message,
  has_require, custom, description, creator, create_time, tenant_id
)
VALUES
  (117001, 117000, 10, 'reportShowIndexRange', 'QCS.ocd.reportShowIndexRange', 'QCS', 'QCS', 1,
   'qualityStd', 'qualityStd', NULL, NULL, NULL, NULL, true, NULL, 'QCS report items read from quality standard in the recovered PostgreSQL test runtime', 'postgres-init-117', CURRENT_TIMESTAMP, 'dt'),
  (117002, 117000, 11, 'autoReportStaff', 'QCS.ocd.autoReportStaff', 'QCS', 'QCS', 1,
   'currentUser', 'currentUser', NULL, NULL, NULL, NULL, true, NULL, 'Use current login staff when auto-generating QCS report documents', 'postgres-init-117', CURRENT_TIMESTAMP, 'dt'),
  (117003, 117000, 12, 'autoReport', 'QCS.ocd.autoReport', 'QCS', 'QCS', 1,
   'manuCheck', 'manuCheck', NULL, NULL, NULL, NULL, false, NULL, 'Enable automatic report generation for manufacturing inspection in test env', 'postgres-init-117', CURRENT_TIMESTAMP, 'dt')
ON CONFLICT (app_code, code) DO UPDATE
SET catalog_id = EXCLUDED.catalog_id,
    default_value = EXCLUDED.default_value,
    widget_value = EXCLUDED.widget_value,
    name = EXCLUDED.name,
    module_code = EXCLUDED.module_code,
    widget_type = EXCLUDED.widget_type,
    has_require = EXCLUDED.has_require,
    description = EXCLUDED.description,
    tenant_id = EXCLUDED.tenant_id,
    modifier = 'postgres-init-117',
    modify_time = CURRENT_TIMESTAMP;

UPDATE public.systemconfig_config_version
SET config_version = 'dt/QCS/QCS/postgres-init-117-' || extract(epoch from CURRENT_TIMESTAMP)::bigint,
    tenant_id = 'dt',
    modifier = 'postgres-init-117',
    modify_time = CURRENT_TIMESTAMP
WHERE tid_module_key = 'dt/QCS/QCS';

INSERT INTO public.systemconfig_config_version(
  id, config_version, tid_module_key, creator, create_time, tenant_id
)
SELECT
  117004,
  'dt/QCS/QCS/postgres-init-117',
  'dt/QCS/QCS',
  'postgres-init-117',
  CURRENT_TIMESTAMP,
  'dt'
WHERE NOT EXISTS (
  SELECT 1
  FROM public.systemconfig_config_version
  WHERE tid_module_key = 'dt/QCS/QCS'
);

WITH ranked_workflow AS (
    SELECT
        id,
        process_key,
        row_number() OVER (
            PARTITION BY process_key
            ORDER BY coalesce(process_version, 0) DESC, id DESC
        ) AS rn
    FROM public.wf_deployment
    WHERE process_key = 'manuInspectReportWorkFlow'
      AND coalesce(valid, 1) = 1
)
UPDATE public.wf_deployment deployment
SET is_current_version = CASE
    WHEN ranked_workflow.rn = 1 THEN 1
    ELSE 0
END
FROM ranked_workflow
WHERE deployment.id = ranked_workflow.id;

INSERT INTO public.baseset_wf_custom_configs (
    id,
    version,
    valid,
    cid,
    create_staff_id,
    create_time,
    create_department_id,
    create_position_id,
    group_id,
    owner_staff_id,
    owner_department_id,
    owner_position_id,
    position_lay_rec,
    status,
    table_no,
    code,
    name,
    deployment_key,
    transaction_key,
    flow_config_type,
    cus_create_staff_id,
    cus_create_dept_id,
    cus_create_pos_id,
    effective_state
)
SELECT
    1170000000000001::bigint,
    0,
    true,
    1000,
    1,
    CURRENT_TIMESTAMP,
    1,
    1,
    1000,
    1,
    1,
    1,
    '1',
    99,
    'QCS_MANU_REPORT_WORKFLOW_CONFIG',
    'manuReport',
    '产品检验报告',
    'manuInspectReportWorkFlow',
    'manuReport',
    'BaseSet_flowConfigType/save',
    1,
    1,
    1,
    0
WHERE NOT EXISTS (
    SELECT 1
    FROM public.baseset_wf_custom_configs existing
    WHERE existing.code = 'manuReport'
)
ON CONFLICT (id) DO NOTHING;

UPDATE public.baseset_wf_custom_configs
SET
    valid = true,
    name = '产品检验报告',
    deployment_key = 'manuInspectReportWorkFlow',
    transaction_key = 'manuReport',
    flow_config_type = 'BaseSet_flowConfigType/save',
    cus_create_staff_id = coalesce(cus_create_staff_id, 1),
    cus_create_dept_id = coalesce(cus_create_dept_id, 1),
    cus_create_pos_id = coalesce(cus_create_pos_id, 1),
    modify_time = CURRENT_TIMESTAMP
WHERE code = 'manuReport';
