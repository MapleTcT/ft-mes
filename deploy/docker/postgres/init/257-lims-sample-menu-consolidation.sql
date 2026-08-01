-- Consolidate the restored LIMSSample navigation into business-stage folders.
--
-- This migration changes navigation only. Existing page menu IDs, routes,
-- operations, company references and user favourites remain untouched. Group
-- names use the platform's literal-name fallback because this legacy runtime
-- cannot hot-load new menu keys when its packaged counter bundle is missing.

BEGIN;

CREATE TEMP TABLE adp_lims_sample_group_seed (
    id bigint PRIMARY KEY,
    code text NOT NULL UNIQUE,
    name_key text NOT NULL UNIQUE,
    name_display text NOT NULL,
    sort double precision NOT NULL
) ON COMMIT DROP;

INSERT INTO adp_lims_sample_group_seed VALUES
    (9257000000000001, 'LIMSSample.group.registerCollect',
     'LIMSSample.menu.group.registerCollect', '登记与取样', 1.0),
    (9257000000000002, 'LIMSSample.group.receivePrepare',
     'LIMSSample.menu.group.receivePrepare', '收样与制备', 2.0),
    (9257000000000003, 'LIMSSample.group.resultReview',
     'LIMSSample.menu.group.resultReview', '结果录入与复核', 3.0),
    (9257000000000004, 'LIMSSample.group.auditDisposition',
     'LIMSSample.menu.group.auditDisposition', '审核与处置', 4.0),
    (9257000000000005, 'LIMSSample.group.ledgerReport',
     'LIMSSample.menu.group.ledgerReport', '台账与报告', 5.0);

DO $guard$
BEGIN
    IF (SELECT count(*) FROM public.rbac_menuinfo WHERE code = 'LIMSSample') <> 1 THEN
        RAISE EXCEPTION 'LIMSSample menu consolidation requires exactly one LIMSSample root';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM adp_lims_sample_group_seed seed
          JOIN public.rbac_menuinfo current_row ON current_row.id = seed.id
         WHERE current_row.code IS DISTINCT FROM seed.code
    ) THEN
        RAISE EXCEPTION 'LIMSSample menu consolidation deterministic menu IDs collide';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM adp_lims_sample_group_seed seed
          JOIN public.rbac_menuinfo current_row ON current_row.code = seed.code
         WHERE current_row.id IS DISTINCT FROM seed.id
    ) THEN
        RAISE EXCEPTION 'LIMSSample menu consolidation codes already use different IDs';
    END IF;
END $guard$;

INSERT INTO public.rbac_menuinfo (
    id, version, create_time, modify_time, leaf, valid, cid, show_type,
    menu_type, is_hide, module_code, system_default, css_class, sort,
    name, name_display, code, app, enable, lay_no, lay_rec, parent_id,
    full_path, full_path_name, source, edited, type, no_restrict, status
)
SELECT
    seed.id,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    false,
    true,
    1000,
    1,
    1,
    false,
    'LIMSSample_5.0.0.0',
    true,
    'icon-folder',
    seed.sort,
    seed.name_display,
    seed.name_display,
    seed.code,
    'LIMSSample',
    true,
    2,
    concat(root.lay_rec, '-', seed.id::text),
    root.id,
    concat(root.full_path, '/', seed.id::text),
    concat(root.full_path_name, '/', seed.name_display),
    'ADP_RECOVERY',
    false,
    0,
    false,
    0
  FROM adp_lims_sample_group_seed seed
 CROSS JOIN LATERAL (
     SELECT id, lay_rec, full_path, full_path_name
       FROM public.rbac_menuinfo
      WHERE code = 'LIMSSample'
      ORDER BY id
      LIMIT 1
 ) root
ON CONFLICT (id) DO UPDATE SET
    modify_time = CURRENT_TIMESTAMP,
    leaf = false,
    valid = true,
    cid = EXCLUDED.cid,
    show_type = EXCLUDED.show_type,
    menu_type = EXCLUDED.menu_type,
    is_hide = false,
    module_code = EXCLUDED.module_code,
    system_default = true,
    css_class = EXCLUDED.css_class,
    sort = EXCLUDED.sort,
    name = EXCLUDED.name,
    name_display = EXCLUDED.name_display,
    code = EXCLUDED.code,
    app = EXCLUDED.app,
    enable = true,
    lay_no = EXCLUDED.lay_no,
    lay_rec = EXCLUDED.lay_rec,
    parent_id = EXCLUDED.parent_id,
    full_path = EXCLUDED.full_path,
    full_path_name = EXCLUDED.full_path_name,
    source = EXCLUDED.source,
    edited = false,
    type = 0,
    no_restrict = false,
    status = 0,
    namespace = NULL,
    url = NULL,
    route = NULL,
    action_url = NULL;

CREATE TEMP TABLE adp_lims_sample_leaf_seed (
    menu_code text PRIMARY KEY,
    group_code text NOT NULL,
    sort double precision NOT NULL
) ON COMMIT DROP;

INSERT INTO adp_lims_sample_leaf_seed VALUES
    ('LIMSSample_5.0.0.0_sample_sampleRegisterLayout',
     'LIMSSample.group.registerCollect', 1.0),
    ('LIMSBasic_1.0.0_testPlan_planSetSampleList',
     'LIMSSample.group.registerCollect', 2.0),
    ('LIMSSample_5.0.0.0_sample_batchSampleRegister',
     'LIMSSample.group.registerCollect', 3.0),
    ('LIMSSample_5.0.0.0_sample_sampTaskAlcatLayOutNew',
     'LIMSSample.group.registerCollect', 4.0),
    ('LIMSSample_5.0.0.0_sample_collectListLayoutNew',
     'LIMSSample.group.registerCollect', 5.0),

    ('LIMSSample_5.0.0.0_sample_receiveListLayoutNew',
     'LIMSSample.group.receivePrepare', 1.0),
    ('LIMSSample_5.0.0.0_sample_sampleSweepReceive',
     'LIMSSample.group.receivePrepare', 2.0),
    ('LIMSSample_5.0.0.0_sample_makeListLayoutNew',
     'LIMSSample.group.receivePrepare', 3.0),
    ('LIMSSample_5.0.0.0_sample_handoverListLayoutNew',
     'LIMSSample.group.receivePrepare', 4.0),

    ('LIMSSample_5.0.0.0_sample_batchRecordByTest',
     'LIMSSample.group.resultReview', 1.0),
    ('LIMSSample_5.0.0.0_sample_recordBySingleSample',
     'LIMSSample.group.resultReview', 2.0),
    ('LIMSSample_5.0.0.0_sample_recordByTest',
     'LIMSSample.group.resultReview', 3.0),
    ('LIMSSample_5.0.0.0_sample_recordBySample',
     'LIMSSample.group.resultReview', 4.0),
    ('LIMSSample_5.0.0.0_sample_recordCheckBySample',
     'LIMSSample.group.resultReview', 5.0),
    ('LIMSSample_5.0.0.0_sample_recordCheckByTest',
     'LIMSSample.group.resultReview', 6.0),

    ('LIMSSample_5.0.0.0_sample_sampleCheck',
     'LIMSSample.group.auditDisposition', 1.0),
    ('LIMSSample_5.0.0.0_sample_sampleRefuse',
     'LIMSSample.group.auditDisposition', 2.0),
    ('LIMSSample_5.0.0.0_sample_sampleAccept',
     'LIMSSample.group.auditDisposition', 3.0),
    ('LIMSSample_5.0.0.0_sample_sampleDealListLayout',
     'LIMSSample.group.auditDisposition', 4.0),
    ('LIMSSample_5.0.0.0_sample_remainSampleLayout',
     'LIMSSample.group.auditDisposition', 5.0),
    ('LIMSSample_5.0.0.0_sample_retainListLayout',
     'LIMSSample.group.auditDisposition', 6.0),

    ('LIMSSample_5.0.0.0_sample_sampleTestProgress',
     'LIMSSample.group.ledgerReport', 1.0),
    ('LIMSSample_5.0.0.0_sample_sampleInfoLayout',
     'LIMSSample.group.ledgerReport', 2.0),
    ('LIMSSample_5.0.0.0_sampleReport_sampleReportList',
     'LIMSSample.group.ledgerReport', 3.0);

DO $leaf_guard$
DECLARE
    missing_codes text;
BEGIN
    SELECT string_agg(seed.menu_code, ', ' ORDER BY seed.menu_code)
      INTO missing_codes
      FROM adp_lims_sample_leaf_seed seed
 LEFT JOIN public.rbac_menuinfo menu ON menu.code = seed.menu_code
     WHERE menu.id IS NULL;

    IF missing_codes IS NOT NULL THEN
        RAISE EXCEPTION 'LIMSSample menu consolidation missing leaf menus: %', missing_codes;
    END IF;

    IF EXISTS (
        SELECT 1
          FROM adp_lims_sample_leaf_seed seed
          JOIN public.rbac_menuinfo menu ON menu.code = seed.menu_code
         WHERE nullif(menu.url, '') IS NULL
    ) THEN
        RAISE EXCEPTION 'LIMSSample menu consolidation expected every mapped page to have a URL';
    END IF;
END $leaf_guard$;

UPDATE public.rbac_menuinfo leaf_menu
   SET parent_id = group_menu.id,
       leaf = true,
       lay_no = 3,
       lay_rec = concat(root.lay_rec, '-', group_menu.id::text, '-', leaf_menu.id::text),
       full_path = concat(root.full_path, '/', group_menu.id::text, '/', leaf_menu.id::text),
       full_path_name = concat(
           root.full_path_name, '/', group_menu.name, '/', leaf_menu.name
       ),
       sort = seed.sort,
       modify_time = CURRENT_TIMESTAMP
  FROM adp_lims_sample_leaf_seed seed
  JOIN public.rbac_menuinfo group_menu ON group_menu.code = seed.group_code
 CROSS JOIN LATERAL (
     SELECT id, lay_rec, full_path, full_path_name
       FROM public.rbac_menuinfo
      WHERE code = 'LIMSSample'
      ORDER BY id
      LIMIT 1
 ) root
 WHERE leaf_menu.code = seed.menu_code;

CREATE TEMP TABLE adp_lims_sample_i18n_seed (
    id bigint PRIMARY KEY,
    i18n_key text NOT NULL,
    i18n_value text NOT NULL,
    langu_code text NOT NULL,
    UNIQUE (i18n_key, langu_code)
) ON COMMIT DROP;

INSERT INTO adp_lims_sample_i18n_seed VALUES
    (9257000000000201, 'LIMSSample.menu.group.registerCollect', '登记与取样', 'zh_CN'),
    (9257000000000202, 'LIMSSample.menu.group.receivePrepare', '收样与制备', 'zh_CN'),
    (9257000000000203, 'LIMSSample.menu.group.resultReview', '结果录入与复核', 'zh_CN'),
    (9257000000000204, 'LIMSSample.menu.group.auditDisposition', '审核与处置', 'zh_CN'),
    (9257000000000205, 'LIMSSample.menu.group.ledgerReport', '台账与报告', 'zh_CN'),
    (9257000000000206, 'LIMSSample.menu.group.registerCollect', 'Registration & Sampling', 'en_US'),
    (9257000000000207, 'LIMSSample.menu.group.receivePrepare', 'Receiving & Preparation', 'en_US'),
    (9257000000000208, 'LIMSSample.menu.group.resultReview', 'Result Entry & Review', 'en_US'),
    (9257000000000209, 'LIMSSample.menu.group.auditDisposition', 'Approval & Disposition', 'en_US'),
    (9257000000000210, 'LIMSSample.menu.group.ledgerReport', 'Ledger & Reports', 'en_US'),
    (9257000000000211, 'LIMSSample.menu.group.registerCollect', '登记与取样', 'zh_HK'),
    (9257000000000212, 'LIMSSample.menu.group.receivePrepare', '收樣與製備', 'zh_HK'),
    (9257000000000213, 'LIMSSample.menu.group.resultReview', '結果錄入與覆核', 'zh_HK'),
    (9257000000000214, 'LIMSSample.menu.group.auditDisposition', '審核與處置', 'zh_HK'),
    (9257000000000215, 'LIMSSample.menu.group.ledgerReport', '台賬與報告', 'zh_HK');

DO $i18n_guard$
BEGIN
    IF EXISTS (
        SELECT 1
          FROM adp_lims_sample_i18n_seed seed
          JOIN public.supfusion_i18n_resource current_row ON current_row.id = seed.id
         WHERE current_row.i18n_key IS DISTINCT FROM seed.i18n_key
            OR current_row.langu_code IS DISTINCT FROM seed.langu_code
    ) THEN
        RAISE EXCEPTION 'LIMSSample menu consolidation deterministic i18n IDs collide';
    END IF;
END $i18n_guard$;

UPDATE public.supfusion_i18n_resource current_row
   SET i18n_value = seed.i18n_value,
       module_code = 'LIMSSample',
       module_version_code = 'LIMSSample202606130026',
       valid = '1',
       modifier = CURRENT_TIMESTAMP,
       modify_time = CURRENT_TIMESTAMP,
       modify_staff_id = 1
  FROM adp_lims_sample_i18n_seed seed
 WHERE current_row.i18n_key = seed.i18n_key
   AND current_row.langu_code = seed.langu_code
   AND coalesce(current_row.tenant_id, 'dt') = 'dt';

INSERT INTO public.supfusion_i18n_resource (
    id, i18n_key, i18n_value, langu_code, module_code, module_version_code,
    valid, tenant_id, creator, create_time, create_staff_id, modifier,
    modify_time, modify_staff_id
)
SELECT
    seed.id,
    seed.i18n_key,
    seed.i18n_value,
    seed.langu_code,
    'LIMSSample',
    'LIMSSample202606130026',
    '1',
    'dt',
    'system',
    CURRENT_TIMESTAMP,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    1
  FROM adp_lims_sample_i18n_seed seed
 WHERE NOT EXISTS (
     SELECT 1
       FROM public.supfusion_i18n_resource current_row
      WHERE current_row.i18n_key = seed.i18n_key
        AND current_row.langu_code = seed.langu_code
        AND coalesce(current_row.tenant_id, 'dt') = 'dt'
 );

-- The legacy i18n service caches one dictionary per module index. Updating
-- resources without advancing the index leaves new keys invisible until a
-- later package import changes that index.
UPDATE public.supfusion_i18n_index
   SET module_index_code = 'LIMSSample25700000-0000-4000-8000-000000000001',
       modifier = 'codex_menu_consolidation',
       modify_time = CURRENT_TIMESTAMP,
       modify_staff_id = 1
 WHERE module_code = 'LIMSSample'
   AND coalesce(tenant_id, 'dt') = 'dt';

UPDATE public.supfusion_i18n_version
   SET modifier = 'codex_menu_consolidation',
       modify_time = CURRENT_TIMESTAMP,
       modify_staff_id = 1
 WHERE module_code = 'LIMSSample'
   AND module_version_code = 'LIMSSample202606130026';

CREATE TEMP TABLE adp_lims_sample_company_ref_seed (
    id bigint PRIMARY KEY,
    menuinfo_id bigint NOT NULL UNIQUE
) ON COMMIT DROP;

INSERT INTO adp_lims_sample_company_ref_seed
SELECT 9257000000000100 + row_number() OVER (ORDER BY seed.id), seed.id
  FROM adp_lims_sample_group_seed seed;

DO $company_ref_guard$
BEGIN
    IF EXISTS (
        SELECT 1
          FROM adp_lims_sample_company_ref_seed seed
          JOIN public.rbac_menuinfo_company_ref current_row ON current_row.id = seed.id
         WHERE current_row.menuinfo_id IS DISTINCT FROM seed.menuinfo_id
            OR current_row.company_id NOT IN (-1, 1000)
    ) THEN
        RAISE EXCEPTION 'LIMSSample menu consolidation deterministic company-ref IDs collide';
    END IF;
END $company_ref_guard$;

INSERT INTO public.rbac_menuinfo_company_ref (
    id, menuinfo_id, company_id, valid, creator, modifier,
    create_time, modify_time, create_staff_id, modify_staff_id, appid
)
SELECT
    seed.id,
    seed.menuinfo_id,
    -1,
    false,
    'codex_menu_consolidation',
    'codex_menu_consolidation',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    1,
    1,
    'LIMSSample'
  FROM adp_lims_sample_company_ref_seed seed
 WHERE NOT EXISTS (
     SELECT 1
       FROM public.rbac_menuinfo_company_ref current_row
      WHERE current_row.menuinfo_id = seed.menuinfo_id
        AND current_row.company_id IN (-1, 1000)
 );

UPDATE public.rbac_menuinfo_company_ref current_row
   SET appid = 'LIMSSample',
       modify_time = CURRENT_TIMESTAMP,
       modifier = 'codex_menu_consolidation'
 WHERE current_row.menuinfo_id IN (SELECT id FROM adp_lims_sample_group_seed)
   AND current_row.company_id IN (-1, 1000);

DO $assertions$
DECLARE
    root_id bigint;
    direct_group_count integer;
    mapped_leaf_count integer;
    invalid_path_count integer;
BEGIN
    SELECT id INTO root_id FROM public.rbac_menuinfo WHERE code = 'LIMSSample';

    SELECT count(*)
      INTO direct_group_count
      FROM public.rbac_menuinfo menu
      JOIN adp_lims_sample_group_seed seed ON seed.id = menu.id
     WHERE menu.parent_id = root_id
       AND menu.lay_no = 2
       AND NOT menu.leaf
       AND menu.valid
       AND menu.enable;

    SELECT count(*)
      INTO mapped_leaf_count
      FROM public.rbac_menuinfo menu
      JOIN adp_lims_sample_leaf_seed seed ON seed.menu_code = menu.code
      JOIN public.rbac_menuinfo parent_menu
        ON parent_menu.id = menu.parent_id
       AND parent_menu.code = seed.group_code
     WHERE menu.lay_no = 3
       AND menu.leaf
       AND menu.valid
       AND menu.enable;

    SELECT count(*)
      INTO invalid_path_count
      FROM public.rbac_menuinfo menu
      JOIN adp_lims_sample_leaf_seed seed ON seed.menu_code = menu.code
      JOIN public.rbac_menuinfo parent_menu ON parent_menu.id = menu.parent_id
     WHERE menu.full_path <> concat(
               (SELECT full_path FROM public.rbac_menuinfo WHERE id = root_id),
               '/', parent_menu.id::text, '/', menu.id::text
           )
        OR menu.lay_rec <> concat(
               (SELECT lay_rec FROM public.rbac_menuinfo WHERE id = root_id),
               '-', parent_menu.id::text, '-', menu.id::text
           );

    IF direct_group_count <> 5 THEN
        RAISE EXCEPTION 'LIMSSample menu consolidation expected 5 direct groups, got %',
            direct_group_count;
    END IF;

    IF mapped_leaf_count <> 24 THEN
        RAISE EXCEPTION 'LIMSSample menu consolidation expected 24 mapped pages, got %',
            mapped_leaf_count;
    END IF;

    IF invalid_path_count <> 0 THEN
        RAISE EXCEPTION 'LIMSSample menu consolidation found % invalid leaf paths',
            invalid_path_count;
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM public.supfusion_i18n_index
         WHERE module_code = 'LIMSSample'
           AND coalesce(tenant_id, 'dt') = 'dt'
           AND module_index_code = 'LIMSSample25700000-0000-4000-8000-000000000001'
    ) THEN
        RAISE EXCEPTION 'LIMSSample menu consolidation did not advance the i18n module index';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM public.rbac_menuinfo menu
         WHERE menu.parent_id = root_id
           AND menu.leaf
           AND menu.valid
           AND menu.code IN (SELECT menu_code FROM adp_lims_sample_leaf_seed)
    ) THEN
        RAISE EXCEPTION 'LIMSSample menu consolidation left mapped pages directly under root';
    END IF;
END $assertions$;

COMMIT;
